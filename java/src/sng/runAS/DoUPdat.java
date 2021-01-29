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
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;

/*************************************************
** Extract Fasta sequence from Dat 
 >sp|Q6V4H0|10HGO_CATRO 8-hydroxygeraniol dehydrogenase OS=Catharanthus roseus GN=10HGO PE=1 SV=1
MAKSPEVEHPVKAFGWAARDTSGHLSPFHFSRRATGEHDVQFKVLYCGICHSDLHMIKNE

ID   10HGO_CATRO             Reviewed;         360 AA.
AC   Q6V4H0;
DE   RecName: Full=8-hydroxygeraniol dehydrogenase;
OS   Catharanthus roseus (Madagascar periwinkle) (Vinca rosea).
OS   (Populus tomentosa x Populus bolleana) x Populus tomentosa
GN   Name=10HGO;			// not really used in TCW, but consistent with Perl/SWISS.tar parsing
SQ   SEQUENCE   360 AA;  38937 MW;  AB701A2D8921005E CRC64;
DR   GO; GO:0016491; F:oxidoreductase activity; IEA:UniProtKB-KW.
DR   GO; GO:0006886; P:intracellular protein transport; IC:PomBase.
DR   InterPro; IPR013149; ADH_C.
DR   Pfam; PF08240; ADH_N; 1.
DE   EC=2.4.1.- {ECO:0000305};
 MAKSPEVEHP VKAFGWAARD TSGHLSPFHF SRRATGEHDV QFKVLYCGIC HSDLHMIKNE
//
 */
public class DoUPdat {
	public DoUPdat(ASFrame asf) {frameObj=asf; ecHash= new MetaData().getEChash();}
	
	private String errFile = "parseErrors";
	private String errPath;
	private int cntErr=0;
	private HashSet <String> ecHash;
	
	private final Pattern patID = Pattern.compile("ID\\s+(\\S*)"); 
	private final Pattern patAC = Pattern.compile("AC\\s+(\\S*);");
	private final Pattern patDE = Pattern.compile("DE\\s+\\S+[:] Full=(.*);");
	private final Pattern patOS = Pattern.compile("OS\\s+([^(]*)");
	private final Pattern patGN = Pattern.compile("GN\\s+Name=([^;]*)");
	private final Pattern patGO = Pattern.compile("DR\\s+GO;");
	private final Pattern patGOec = Pattern.compile("DR\\s+GO;\\s*(GO:\\d*);[^;]*;\\s*(\\w\\w\\w)");
	private final Pattern patGOnoec = Pattern.compile("DR\\s+GO;\\s*(GO:\\d*);[^;]*;");
	private final Pattern patEC = Pattern.compile("DE\\s+EC=");
	private final Pattern patECfull = Pattern.compile("DE\\s+EC=([^;]*);");
	private final Pattern patKG = Pattern.compile("DR\\s+KEGG; ([^;]*);");
	private final Pattern patPF = Pattern.compile("DR\\s+Pfam; ([^;]*);");
	private final Pattern patIP = Pattern.compile("DR\\s+InterPro; ([^;]*);");
	
	private final DecimalFormat df = new DecimalFormat("#,###,###");
	
	public boolean fasta(String prefix, String upDir, String inFile, String outFile) {
		String datSize = FileHelpers.getFileSize(upDir+inFile); // CAS315 add size
		Out.PrtSpMsg(2, "Make FASTA from " + (upDir+inFile) + " " + datSize);
		errPath = upDir + errFile;
		Matcher x;
		String line="";
		try {
			long time=Out.getTime();
			BufferedReader in=FileHelpers.openGZIP(upDir+inFile);
			if (in==null) return false;
			BufferedWriter out = new BufferedWriter(new FileWriter(upDir+outFile, false));
			
			boolean inSeq=false;
			String id="",ac="", de="",os="", gn="";
			int cnt=0, cntSeq=0;
	        while ((line = in.readLine()) != null) {
        	 	if (line.startsWith("//")) {
        	 		inSeq=false;
        	 		ac=id=de=os=gn="";
        	 		if (cntSeq==0) {
        	 			prtParseErr("Missing sequence: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);
        	 		}
        	 	}
        	 	else if (inSeq) {
        	 		line = line.replace(" ", "");
        	 		out.write(line + "\n");
        	 		cntSeq++;
        	 	}
        	 	else if (line.startsWith("SQ")) {
        	 		checkOK(ac,id,de,os); // print even if error
        	 		if (id!="") {
        	 			String desc = ">" + prefix + "|" + ac + "|" + id + " " + de + " OS=" + os + " GN=" + gn;
        	 			out.write(desc+"\n");
        	 			inSeq=true;
        	 			cntSeq=0;
        	 		}
        	 	}
        	 	else if (id=="" && line.startsWith("ID")) {
        	 		if (inSeq) {
        	 			prtParseErr("Sequence not terminated: " + " ID=" + id + " AC=" + ac + " DE=" + de + " OS="+os);
        	 			inSeq=false;
        	 		}
        	 		x = patID.matcher(line);
        	 		if (x.find()) id = x.group(1);
    				else Out.PrtErr("Invalid line: " + line);
        	 		
        	 		cnt++;
        	 		if (cnt%10000==0) Out.r("Wrote " + cnt);
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
	         in.close(); out.close();
	         if (id!="")   Out.PrtWarn("Incomplete file. Entry not complete: " + id);
	         if (cntErr>0) Out.PrtWarn("Parse errors " + cntErr + " see " + errPath);
	         
	         String faSize = FileHelpers.getFileSize(upDir+outFile);
	         Out.PrtSpMsgTime(3, df.format(cnt) + " written to " + outFile + " " + faSize, time);
	         
	         return true;
		}
		catch (Exception e) {
			Out.PrtErr("Check your space, you may have run out.");
			ErrorReport.reportError("Failed on: " + line);
			ErrorReport.reportError(e, "Making FASTA " + inFile); 
			return false;
		}
	}
	private void prtParseErr(String err) {
		try {
			PrintWriter pWriter = new PrintWriter(new FileWriter(errPath, true));
			pWriter.println(err + "\n");
			pWriter.close();
			cntErr++;
		} catch (Exception e1) {
			Out.PrtErr("An error has occurred writing to file " + errPath);
		}
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
	/*************************************************
	** Read dat and put into godb
	 */
	public boolean dat2go(String upDir, String file, DBConn goDB) {
		Out.PrtSpMsg(2, "Processing " + file);
		Matcher x;
		String err="";
		try {
			long time=Out.getTime();
			BufferedReader in=FileHelpers.openGZIP(upDir+"/"+file);
			if (in==null) return false;
			
			goDB.openTransaction(); 
			
			PreparedStatement ps = goDB.prepareStatement(
					"insert ignore into PAVE_Uniprot (upid, acc, go, pfam, kegg, ec, interpro) " +
					"values (?,?,?,?,?,?,?)");
			String line, id="", acc="", go="", pfam="", kegg="", ec="", ip="";
			HashMap <String, Integer> badEC = new HashMap <String, Integer> ();
			
			boolean inSeq=false;
			int cnt=0;
			long cnt1=0;
	        while ((line = in.readLine()) != null) {
	            if (line.startsWith("//")) {
            	 	ps.setString(1, id);
            	 	ps.setString(2, acc);
            	 	ps.setString(3, go);
            		ps.setString(4, pfam);
            		ps.setString(5, kegg);
            		ps.setString(6, ec);
            		ps.setString(7, ip);
            		err = id + "\n" + go + "\n"+  pfam + "\n"+ kegg + "\n"+ ec + "\n"+ ip;
	 				ps.addBatch();
	 				cnt++;cnt1++;
	 	            if (cnt == 1000) {
	 	     	        Out.r("Load " + cnt1);
	 					ps.executeBatch();
	 					cnt=0;
	 				}
	 	            inSeq=false;
	 	            id=acc=go=pfam=kegg=ec=ip="";
	 	            continue;
	            }
	            if (inSeq) {continue;}
	            
        	 	if (line.startsWith("SQ")) {inSeq=true; continue;}
        	 	
        	 	if (id=="" && line.startsWith("ID")) {
        	 		x = patID.matcher(line);
        	 		if (x.find()) id = x.group(1);
    				else System.out.println("Invalid line: " + line);
        	 		continue;
        	 	}
        	 	if (acc =="" && line.startsWith("AC")) {
        	 		x = patAC.matcher(line);
        	 		if (x.find()) acc = x.group(1);
        	 		continue;
        	 	}
        	 	x = patEC.matcher(line);
        	 	if (x.find()) {
    	 			x = patECfull.matcher(line);
    	 			if (x.find()) {
    	 				ec = x.group(1);
    	 				cntEC++;
    	 			}
    	 			else Out.bug("EC: " + line);
    	 			continue;
        	 	}
        	 	x = patGO.matcher(line);
        	 	if (x.find()) { 
        	 		x = patGOec.matcher(line);
        	 		if (x.find()) {
        	 			String gonum = x.group(1);
        	 			String goec = x.group(2);
        	 			if (!ecHash.contains(goec)) { 
        	 				if (!badEC.containsKey(goec)) {
        	 					Out.PrtWarn("Unknown EC: " + line);
        	 					badEC.put(goec, 1);
        	 				}
        	 				else badEC.put(goec, badEC.get(goec)+1);
        	 				
        	 				if (!allbadEC.containsKey(goec)) allbadEC.put(goec, 1);
        	 				else allbadEC.put(goec, allbadEC.get(goec)+1);
        	 		
        	 				goec = "UNK";
        	 			}
        	 			
        	 			String gx = gonum + ":" + goec;
        	 			if (go=="") go=gx;
        	 			else go+= ";"+gx;
        	 			cntGO++;
        	 			continue;
        	 		}
        	 		x = patGOnoec.matcher(line);
        	 		if (x.find()) {
        	 			String gx = x.group(1) + ":" + "UNK";
        	 			if (go=="") go=gx;
        	 			else go+= ";"+gx;
        	 			cntGO++;
        	 		}
        	 		else Out.PrtErr("Fail: " + line);
        	 		continue;
        	 	}
        	 	if (line.startsWith("DR")) { 
        	 		x = patPF.matcher(line);
        	 		if (x.find()) {
        	 			if (pfam=="") pfam=x.group(1);
        	 			else pfam+= ";"+x.group(1);
        	 			cntPF++;
        	 			continue;
        	 		}
        	 		
        	 		x = patKG.matcher(line);
        	 		if (x.find()) {
        	 			if (kegg=="") kegg=x.group(1);
        	 			else kegg+= ";" + x.group(1);
        	 			cntKG++;
        	 			continue;
        	 		}
	        	 		
        	 		x = patIP.matcher(line);
        	 		if (x.find()) {
        	 			if (ip=="") ip=x.group(1);
        	 			else ip+= ";"+x.group(1);
        	 			cntIP++;
        	 			continue;
        	 		}
        	 	}
	         }
	         if (cnt>0) ps.executeBatch();
	         in.close(); ps.close();
	      
			goDB.closeTransaction();
	         
	        System.out.print("                                                     \r");
	        if (id!="") Out.PrtWarn("Incomplete -- did not add " + id);
	         
	        if (badEC.size()>0) { 
	        	String xx="";
		 		for (String ecx : badEC.keySet()) xx += " " + ecx + ":" + badEC.get(ecx);
		 		Out.PrtSpMsg(4, "Unknown ECs: " + xx);
		 	}
	       
	        Out.PrtSpMsgTime(4, df.format(cnt1) + " UniProts ", time);
	        return true;
		}
		catch (Exception e) {
			Out.PrtErr("Inserting:\n" + err);
			ErrorReport.reportError(e, "Processing " + file);}
		return false;
	}
	public boolean prtTotals() {
		Out.PrtSpMsg(2, "Totals:");
		if (allbadEC.size()>0) { 
			String x="";
			for (String ec : allbadEC.keySet()) x += " " + ec + ":" + allbadEC.get(ec);
			Out.PrtSpMsg(3, "Unknown ECs: " + x);
		}
		
		Out.PrtSpMsg(3, "GO: " + df.format(cntGO) +  "  Pfam: " + df.format(cntPF)  
				+ "  KEGG: " + df.format(cntKG)  + "  EC: " + df.format(cntEC) + "  InterPro: " 
				+ df.format(cntIP));	
		return (cntGO>0); 
	}
	private HashMap <String, Integer> allbadEC = new HashMap <String, Integer> ();
	private int cntGO=0, cntKG=0, cntEC=0, cntIP=0, cntPF=0;
	private ASFrame frameObj=null;
}
