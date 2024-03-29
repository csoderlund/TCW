package sng.runAS;
/****************************************
 * Input: .dat.gz or .dat
 * Output: fasta file for TCW 
 * Output: load UniProt dat into goDB
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;


import sng.database.MetaData;
import util.database.*;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;

/*************************************************
** Extract Fasta sequence from Dat 
 >sp|Q6V4H0|10HGO_CATRO 8-hydroxygeraniol dehydrogenase OS=Catharanthus roseus GN=10HGO PE=1 SV=1
MAKSPEVEHPVKAFGWAARDTSGHLSPFHFSRRATGEHDVQFKVLYCGICHSDLHMIKNE

** Extract GO etc for GOdb - enter upid, acc, go, pfam, kegg, ec, interpro

Example lines (edited to show variations): 
ID   10HGO_CATRO             Reviewed;         360 AA.
AC   Q6V4H0;
DE   RecName: Full=Aldehyde dehydrogenase 1 {ECO:0000303|PubMed:29122986};
DE            Short=TcALDH1 {ECO:0000303|PubMed:29122986};
DE            EC=1.2.1.5 {ECO:0000269|PubMed:29122986};
DE			  EC=1.2.1.6;
DE   AltName: Full=Trans-chrysanthemic acid synthase {ECO:0000305};
DE            EC=1.2.1.- {ECO:0000269|PubMed:29122986};				// don't want this one
OS   Catharanthus roseus (Madagascar periwinkle) (Vinca rosea).
OS   (Populus tomentosa x Populus bolleana) x Populus tomentosa
GN   Name=10HGO;												// not used in TCW but is entered anyway
DR   GO; GO:0016491; F:oxidoreductase activity; IEA:UniProtKB-KW.
DR   GO; GO:0006886; P:intracellular protein transport; IC:PomBase.
DR   InterPro; IPR013149; ADH_C.
DR   Pfam; PF08240; ADH_N; 1.
SQ   SEQUENCE   336 AA;  37370 MW;  DECEB0ECA469A9E3 CRC64;
	MAKSPEVEHP VKAFGWAARD TSGHLSPFHF SRRATGEHDV QFKVLYCGIC HSDLHMIKNE
	MKRKGVGFSL PVTVVMLVIG FIYFASVFTF IDRWFSLTSS PGIANAAAFT ALALMCIYNY
//
 */
public class DoUPdat {
	public DoUPdat(ASFrame asf) {
		frameObj=asf; 
		evcHash= new MetaData().getEvChash();
	}
	
	private String errFile = "parseErrors";
	private String errPath;
	private int cntErr=0;
	private HashSet <String> evcHash;
	
	// dat2godb and dat2fasta
	private final Pattern patID = Pattern.compile("ID\\s+(\\S*)"); 
	
	// dat2godb
	private final Pattern patAC = Pattern.compile("AC\\s+(\\S*);");
	private final Pattern patGO = Pattern.compile("DR\\s+GO;");
	private final Pattern patGOev = Pattern.compile("DR\\s+GO;\\s*(GO:\\d*);[^;]*;\\s*(\\w{2,3})"); // CAS320 included IC, ND
	private final Pattern patGOnoev = Pattern.compile("DR\\s+GO;\\s*(GO:\\d*);[^;]*;");
	private final Pattern patEC = Pattern.compile("DE\\s+EC=");
	private final Pattern patECfull = Pattern.compile("DE\\s+EC=(\\S+)[ ;]"); // CAS320 was matching enter line
	private final Pattern patKG = Pattern.compile("DR\\s+KEGG; ([^;]*);");
	private final Pattern patPF = Pattern.compile("DR\\s+Pfam; ([^;]*);");
	private final Pattern patIP = Pattern.compile("DR\\s+InterPro; ([^;]*);");
	
	// dat2 fasta
	private final Pattern patDE = Pattern.compile("DE\\s+\\S+[:] Full=(.*);");
	private final Pattern patOS = Pattern.compile("OS\\s+([^(]*)");
	private final Pattern patGN = Pattern.compile("GN\\s+Name=([^;]*)");
	
	private final DecimalFormat df = new DecimalFormat("#,###,###");
	
	/*************************************************
	** Read dat and put into godb
	*  Process one dat file -- loop through files in in DoOBO
	*  Enters GO, EC, PFAM, KEGG, InterPro
	*  Does NOT enter description as that is from the UniProt fasta file
	 */
	public boolean dat2godb(String upDir, String file, DBConn goDB, HashSet <String> goSet, HashSet <String> obsSet) {
		Out.PrtSpMsg(2, "Processing " + file);
		Matcher x;
		String err="";
		try {
			long time=Out.getTime();
			BufferedReader in=FileHelpers.openGZIP(upDir+"/"+file);
			if (in==null) return false;
			
			String line, prID="", acc="", goList="", pfamList="", keggList="", ec="", ipList="";
			HashSet <String> goNoSet = new HashSet <String> ();
			HashSet <String> goFdObsSet = new HashSet <String> ();
			HashMap <String, Integer> badEC = new HashMap <String, Integer> ();
			
			boolean inSeq=false, inRec=false;
			int cnt=0, cntAdd=0;
			
			// e.g. file=sp_full/uniprot_sprot.dat
			String subDir = (file.contains("/")) ? file.substring(0, file.indexOf("/")) : file;
        	subDir = (subDir.length()>=15) ? subDir.substring(0,15) : subDir;
        	
			goDB.openTransaction(); 
			
			// CAS339 will have duplicates from full SwissProt and TrEMBL, but will be ignored
			PreparedStatement ps = goDB.prepareStatement(
					"insert ignore into " + Globalx.goUpTable + " (upid, acc, go, pfam, kegg, ec, interpro) " +
					"values (?,?,?,?,?,?,?)");
			
	        while ((line = in.readLine()) != null) {
	        /* Save last protein which ends with // */
	            if (line.startsWith("//")) { 
            	 	ps.setString(1, prID);
            	 	ps.setString(2, subDir); // CAS339 change from acc to subDir because acc not used.
            	 	ps.setString(3, goList);
            		ps.setString(4, pfamList);
            		ps.setString(5, keggList);
            		ps.setString(6, ec);
            		ps.setString(7, ipList);
            		err = prID + "\n" + goList + "\n"+  pfamList + "\n"+ keggList + "\n"+ ec + "\n"+ ipList;
	 				ps.addBatch();
	 				cnt++;cntAdd++;
	 	            if (cnt == 10000) { // CAS339 was 1000
	 	     	        Out.r("Load " + cntAdd);
	 					ps.executeBatch();
	 					cnt=0;
	 				}
	 	            inSeq=false;
	 	            prID=goList=pfamList=keggList=ec=ipList="";
	 	            continue;
	            }
	            
	        /* In seq */
	            if (inSeq) {continue;}
        	 	if (line.startsWith("SQ")) {inSeq=true; continue;}
        	 	if (line.startsWith("DE") ) {
        	 		if (line.contains("RecName:")) inRec=true;
        	 		else if (line.contains("AltName:")) inRec=false;
        	 	}
        	 	else inRec=false;
			
        	/* Parse relevant keywords */
        	 	// ID   10HGO_CATRO             Reviewed;         360 AA.
        	 	if (prID=="" && line.startsWith("ID")) {
        	 		x = patID.matcher(line);
        	 		if (x.find()) 
        	 			prID = x.group(1);
    				else 
    					Out.PrtWarn("Invalid ID line: " + line);
        	 		continue;
        	 	}
        	 	// AC   Q6V4H0;
        	 	if (acc =="" && line.startsWith("AC")) {
        	 		x = patAC.matcher(line);
        	 		if (x.find()) 
        	 			acc = x.group(1);
        	 		else 
    					Out.PrtWarn("Invalid AC line: " + line);
        	 		continue;
        	 	}
        	 	// DE            EC=4.2.1.3;
        	 	if (inRec && line.startsWith("DE")) { // CAS320 could get AltName EC if inRec not checked
	        	 	x = patEC.matcher(line);
	        	 	if (x.find()) {
	    	 			x = patECfull.matcher(line);
	    	 			if (x.find()) {
	    	 				if (ec=="") ec = x.group(1);
	    	 				else ec +=  "; " + x.group(1) ; // CAS320 was only saving the last
	    	 				cntEC++;
	    	 			}
	    	 			else Out.PrtWarn("Invalid EC line: " + line);
	    	 			continue;
	        	 	}
        	 	}
        	 	
        	 	// DR   GO; GO:0016491; F:oxidoreductase activity; IEA:UniProtKB-KW.
        	 	x = patGO.matcher(line);
        	 	if (x.find()) { 
        	 		x = patGOev.matcher(line);
        	 		if (x.find()) {
        	 			String goStr = x.group(1);
        	 			String goec = x.group(2);
        	 			if (!evcHash.contains(goec)) { 
        	 				if (!badEC.containsKey(goec)) {
        	 					Out.PrtWarn("Unknown EC: " + line);
        	 					badEC.put(goec, 1);
        	 				}
        	 				else badEC.put(goec, badEC.get(goec)+1);
        	 				
        	 				if (!allbadEvC.containsKey(goec)) allbadEvC.put(goec, 1);
        	 				else allbadEvC.put(goec, allbadEvC.get(goec)+1);
        	 		
        	 				goec = "UNK";
        	 			}
        	 			if (!goSet.contains(goStr) && !goNoSet.contains(goStr)) goNoSet.add(goStr);
        	 			if (obsSet.contains(goStr) && !goFdObsSet.contains(goStr))  goFdObsSet.add(goStr);
        	 			
        	 			String gx = goStr + ":" + goec;
        	 			if (goList=="") goList=gx;
        	 			else goList+= ";"+gx;
        	 			cntGO++;
        	 			continue;
        	 		}
        	 		x = patGOnoev.matcher(line);
        	 		if (x.find()) {
        	 			String gx = x.group(1) + ":" + "UNK";
        	 			if (goList=="") goList=gx;
        	 			else goList+= ";"+gx;
        	 			cntGO++;
        	 			
        	 			Out.prt(line);
        	 		}
        	 		else Out.PrtErr("Fail: " + line);
        	 		continue;
        	 	} // end GO 
        	 	
        	 	// DR   InterPro; IPR013149; ADH_C.
        	 	// DR   Pfam; PF08240; ADH_N; 1.
        	 	if (line.startsWith("DR")) { 
        	 		x = patPF.matcher(line);
        	 		if (x.find()) {
        	 			if (pfamList=="") pfamList=x.group(1);
        	 			else pfamList+= ";"+x.group(1);
        	 			cntPF++;
        	 			continue;
        	 		}
        	 		
        	 		x = patKG.matcher(line);
        	 		if (x.find()) {
        	 			if (keggList=="") keggList=x.group(1);
        	 			else keggList+= ";" + x.group(1);
        	 			cntKG++;
        	 			continue;
        	 		}
	        	 		
        	 		x = patIP.matcher(line);
        	 		if (x.find()) {
        	 			if (ipList=="") ipList=x.group(1);
        	 			else ipList+= ";"+x.group(1);
        	 			cntIP++;
        	 			continue;
        	 		}
        	 	}
	         }	// End read file loop
	         if (cnt>0) ps.executeBatch();
	         in.close(); ps.close();
	         goDB.closeTransaction();
	         Out.rClear();
	         if (prID!="") Out.PrtWarn("Incomplete -- did not add " + prID);
	         
	        if (goNoSet.size()>0) {
	        	Out.PrtSpMsg(4, "Unknown GOs: " + goNoSet.size());
	        	String xx="For example: ";
	        	int i=0;
	        	for (String go : goNoSet) {
	        		xx += go + " ";
	        		i++;
	        		if (i>5) break;
	        	}
	        	Out.PrtSpMsg(4, xx);
	        }
	        if (badEC.size()>0) { 
	        	String xx="";
		 		for (String ecx : badEC.keySet()) xx += " " + ecx + ":" + badEC.get(ecx);
		 		Out.PrtSpMsg(4, "Unknown ECs: " + xx);
		 	}
	        int cntDB = goDB.executeCount("select count(*) from " + Globalx.goUpTable + " where acc='" + subDir + "'");
	        String igMsg = (cntDB==cntAdd) ? "" : Out.df(cntAdd-cntDB) + " Already in GOdb";
	        String obMsg = (goFdObsSet.size()==0) ? "" : Out.df(goFdObsSet.size()) + " Obsolete GOs   ";
	        String msg =  "UniProts   " + obMsg + igMsg;
	        		
	        Out.PrtSpCntMsgTimeMemM(2, cntDB, msg, time);
	        return true;
		}
		catch (Exception e) {
			Out.PrtErr("Inserting:\n" + err);
			ErrorReport.reportError(e, "Processing " + file);}
		return false;
	}
	public boolean prtTotals() {
		Out.PrtSpMsg(2, "Totals:");
		if (allbadEvC.size()>0) { 
			String x="";
			for (String ec : allbadEvC.keySet()) x += " " + ec + ":" + allbadEvC.get(ec);
			Out.PrtSpMsg(3, "Unknown ECs: " + x);
		}
		
		Out.PrtSpMsg(3, "GO: " + df.format(cntGO) +  "  Pfam: " + df.format(cntPF)  
				+ "  KEGG: " + df.format(cntKG)  + "  EC: " + df.format(cntEC) + "  InterPro: " 
				+ df.format(cntIP));	
		return (cntGO>0); 
	}
	/********************************************************************
	 * UniProt dat -> fasta file
	 * skipIDs is for the subset file. It is null for taxo files.
	 */
	public boolean dat2fasta(String prefix, String upDir, String fullDatFile, String outFile, HashSet <String> skipIDs) {
		String datSize = FileHelpers.getFileSize(fullDatFile); // CAS315 add size
		Out.PrtSpMsg(2, "Create " + prefix.toUpperCase() + " FASTA from " + fullDatFile + " (Size " + datSize + ")");
		errPath = upDir + errFile;
		Matcher x;
		String line="";
		
		try {
			long time=Out.getTime();
			BufferedReader inFA=FileHelpers.openGZIP(fullDatFile);
			if (inFA==null) return false;
			
			BufferedWriter outFA = new BufferedWriter(new FileWriter(upDir+outFile, false));
			
			boolean bInSeq=false, bPrtSeq=false;
			String hitID="",ac="", de="",os="", gn="";
			int cntRead=0, cntWrite=0;
			
	        while ((line = inFA.readLine()) != null) {
        	 	if (line.startsWith("//")) { // end last sequence
        	 		bInSeq=false;
        	 		ac=hitID=de=os=gn="";
        	 	}
        	 	else if (bInSeq && bPrtSeq) {  // print sequence
        	 		line = line.replace(" ", "");
        	 		outFA.write(line + "\n");
        	 	}
        	 	else if (line.startsWith("SQ")) { // start sequence on next line
        	 		if (hitID!="" && bPrtSeq) {    // print header line 
        	 			checkOK(ac,hitID,de,os); 
        	 			
        	 			String desc = ">" + prefix + "|" + ac + "|" + hitID + " " + de + " OS=" + os + " GN=" + gn;
        	 			outFA.write(desc+"\n");
        	 			
        	 			bInSeq=true;
        	 			cntWrite++;
        	 		}
        	 	}
        	 	else if (hitID=="" && line.startsWith("ID")) { // start of new entry
        	 		if (bInSeq) {
        	 			prtParseErr("Sequence not terminated: " + " ID=" + hitID + " AC=" + ac + " DE=" + de + " OS="+os);
        	 			bInSeq=false;
        	 		}
        	 		x = patID.matcher(line);
        	 		if (x.find()) 	hitID = x.group(1);
    				else 			Out.PrtErr("Invalid line: " + line);
        	 		
        	 		bPrtSeq =  (skipIDs!=null && skipIDs.contains(hitID)) ? false : true; // CAS339
        	
        	 		cntRead++;
        	 		if (cntRead%10000==0) Out.r("Wrote " + cntRead);
        	 	}
        	 	else if (ac =="" && line.startsWith("AC")) {
        	 		x = patAC.matcher(line);
        	 		if (x.find()) ac = x.group(1);
        	 	}
        	 	else if (de == "" && line.startsWith("DE")) {
        	 		x = patDE.matcher(line);
        	 		if (x.find()) de = x.group(1);
        	 	}
        	 	else if (os=="" && line.startsWith("OS")) {
        	 		x = patOS.matcher(line);
        	 		if (x.find()) os = x.group(1); // can match successfully to nothing
        	 		if (os.equals("")) {
        	 			os = line.substring(3).trim();
        	 			if (os.endsWith(".")) os = os.substring(0, os.length()-1);
        	 		}
        	 	}  
        	 	else if (gn=="" && line.startsWith("GN")) {
        	 		x = patGN.matcher(line);
        	 		if (x.find()) gn = x.group(1);
        	 	} 
	         }
	         inFA.close(); outFA.close();
	         if (hitID!="")  Out.PrtWarn("Incomplete file. Entry not complete: " + hitID);
	         if (cntErr>0) 	 Out.PrtWarn("Parse errors " + cntErr + " see " + errPath);
	         
	         Out.PrtSpCntMsg(3, cntWrite, "of " + df.format(cntRead) + " written to " + outFile);
	         Out.PrtSpMsgTimeMem(2, "Complete " + prefix.toUpperCase() + " FASTA file", time);
	         
	         return true;
		}
		catch (Exception e) {
			Out.PrtErr("Check your space, you may have run out.");
			ErrorReport.reportError("Failed on: " + line);
			ErrorReport.reportError(e, "Making FASTA from " + fullDatFile); 
			return false;
		}
	}
	private void prtParseErr(String err) {
		try {
			PrintWriter pWriter = new PrintWriter(new FileWriter(errPath, true));
			pWriter.println(err);
			pWriter.close();
			cntErr++;
		} catch (Exception e1) {Out.PrtErr("An error has occurred writing to file " + errPath);}
	}
	
	private boolean checkOK(String ac, String id, String de, String os) {
		if (!ac.equals("") && !id.equals("") && !de.equals("") && !os.equals("")) return true;
		if (id.equals("")) {
			prtParseErr("Missing ID: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);	
		}
		else if (ac.equals("")) {
			prtParseErr("Missing AC: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);
		}
		else if (de.equals("")) {
			prtParseErr("Missing DE: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);
		}
		else if (os.equals("")) {
			prtParseErr("Missing OS: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);
		}
		return false;
	}
	
	private HashMap <String, Integer> allbadEvC = new HashMap <String, Integer> ();
	private int cntGO=0, cntKG=0, cntEC=0, cntIP=0, cntPF=0;
	private ASFrame frameObj=null;
}
