package scripts;

/*****************************************************************
* V4 Compares TCW and TransDecoder ORFs.
* 
* To produce test set: see V4subsetORF.java (N=1,2,3)
* trFullOsj.tsv and trFullNnR.tsv are references as trFull.tsv below.
* 
* To produce TCW: Create with ORIGINAL names
* 	Build sTCW_setN with setN sequences, search with defaults against SP_plants.
* 	Produce:
*     tcwSet.tsv: #Seq-ID #SwissProt      ORF-Frame       ORF-Start       ORF-End
*	  trFull.tsv: #Seq-ID Length          ORF-Frame       ORF-Start       ORF-End
*   
* To produce the TD results, the following commands were executed:
*   >./TransDecoder.LongOrfs -t set1.fa
*   # transdecoder_dir/longest_orfs.pep was searched against SP_plant with results in std1_SPpl.tab
*   #	by creating an sTCWdb with longest_orfs.pep, then using TCW defaults (DIAMOND) against SP_plants
*   >./TransDecoder.Predict --retain_blastp_hits std1_SPpl.tab   -–single_best_only -t std1.fa
*   >grep “>”set1.fna.transdecoder.cds >TD.results
* 	TD.results: >NM_001048268.2.p1 GENE.NM_001048268.2~~NM_001048268.2.p1  ORF type:complete len:411 (+),score=44.70 NM_001048268.2:198-1430(+)
*
* Input: tcwSet.tsv, trFull.tsv, TD.results         
* Produces a OutDiff.txt file and OutRemark.txt, where the remark file can be
* added to the TCW database using runSingleTCW.
*/

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V4cmpORF {
	static int SET = 3;

	static String chkFile = 	"paper/ORF/trFullOsj.tsv";  // File2 - TCW TR annotated for correct
	static String chkFile3 = 	"paper/ORF/trFullNnR.tsv";  // ditto
	
	static String sub = 		"set" + SET;
	static String dir =     	"paper/ORF/" + sub + "/"; 		// directory of files and results
	static String tcwFile = 	dir + "tcwSet.tsv";  			// File1 - TCW SP annotated
	static String tdfFile = 	dir + "TD.results";		  		// File3 - TD SP annotated
	
	static String rmkFile =     dir + "Remarks";
	static String difFile =     dir + "Summary";

	public static void main(String[] args) {
		if (SET==3) chkFile = chkFile3;
		
		prt("Start " + sub);
		readTCW(true);  // create tcwORFmap
		readTCW(false); // add to tcwORFmap 
		readTD();		// add to tcwORFmap
		
		process(true, "NoORF.txt"); // ignore  NoORF
		process(false, ".txt");     // include NoORF
		prt("Finish " + sub);
	}
	
	private static void process(boolean bNoORF, String suffix) {
    	try {
    		int cntTCWeqTD=0,  cntTDeqTR=0,  cntTCWeqTR=0;
    		int cntTCWeqTDh=0, cntTDeqTRh=0, cntTCWeqTRh=0;
    		
    		int cntFrDiff=0, cntStartDiff=0, cntEndDiff=0, tcwGTtd=0, tcwLTtd=0;
    		
    		int cntFrTD=0,    cntFrTCW=0,    cntFrTDh=0,    cntFrTCWh=0;
    		int cntStartTD=0, cntStartTCW=0, cntStartTDh=0, cntStartTCWh=0;
    		int cntEndTD=0,   cntEndTCW=0,   cntEndTDh=0,   cntEndTCWh=0;
    		int cntOlapTD=0,  cntOlapTCW=0,  cntOlapTDh=0,  cntOlapTCWh=0;
    		int cntNoTD=0,    cntNoTCW=0,    cntNoTDh=0,    cntNoTCWh=0;
    		
    		String line;
    		PrintWriter rmkFH = new PrintWriter(new FileOutputStream(rmkFile+suffix, false));
    		
    /** Loop through file **/
    		for (String name : tcwORFmap.keySet()) {
    			ORF o = tcwORFmap.get(name);
    			
    	// Count if TCW=TD, TCW=TR, TD=TR
    			boolean tdSame=false, tcwSame=false;
    			if (o.tcwFrame==o.tdFrame  && o.tcwStart==o.tdStart  && o.tcwEnd==o.tdEnd)  { // TCW=TD
    				cntTCWeqTD++;
    				if (o.tcwHasHit.equals("H") && o.tdHasHit.equals("H")) cntTCWeqTDh++;
    			}
    			if (o.tcwFrame==o.chkFrame && o.tcwStart==o.chkStart && o.tcwEnd==o.chkEnd) { // TCW=tr
    				cntTCWeqTR++;
    				if (o.tcwHasHit.equals("H")) cntTCWeqTRh++;
    				tcwSame=true;
    			}
    			if (o.tdFrame==o.chkFrame  && o.tdStart==o.chkStart  && o.tdEnd==o.chkEnd)  { // TD=tr
    				cntTDeqTR++;
    				if (o.tdHasHit.equals("H")) cntTDeqTRh++;
    				tdSame=true;
    			}
    			if (tdSame && tcwSame) continue;
    	
    	// TD/TCW counts for ignore NoORF only
    			if (o.tcwFrame!=0 && o.tdFrame!=0) {
	    			if (o.tcwFrame!=o.tdFrame) 	   cntFrDiff++;
	    			else {
	    				if (o.tcwStart!=o.tdStart) cntStartDiff++;
	    				if (o.tcwEnd!=o.tdEnd) 	   cntEndDiff++;
	    			}
	    			if (o.tcwLen>o.tdLen) tcwGTtd++;
					if (o.tcwLen<o.tdLen) tcwLTtd++;
    			}
    	// check coords and remark
    			String frMsg= o.chkFrame + " Frame";
    			String stMsg= o.chkStart + " Start";
    			String enMsg= o.chkEnd   + " End";  
    			String olMsg= o.chkStart + "," + o.chkEnd + " Olap";
    			String noMsg= "NoORF";
    			String difMsg="";

    			boolean isFr=false, isSt=false, isEn=false, isOl=false, isNo=false;
    			
    			// TCW
    			if (bNoORF && o.tdFrame==0)  {// no counts
    				difMsg="XX ";
				}
    			else if (o.tcwFrame==0)  { 
					cntNoTCW++; 
					noMsg += "!TCW";
					if (!o.tcwHasHit.equals("H")) cntNoTCWh++;
				}
    			else if (o.tcwFrame!=o.chkFrame) { 
					cntFrTCW++;
					frMsg +="!TCW";
					if (o.tcwHasHit!="H") cntFrTCWh++;
    			}
				else if (o.tcwStart>o.chkEnd || o.tcwEnd<o.chkStart)  {
					cntOlapTCW++; 
					olMsg += "!TCW";
					if (!o.tcwHasHit.equals("H")) cntOlapTCWh++;
				}
				else {
    				if (o.chkStart!=o.tcwStart)  {
    					cntStartTCW++;
    					stMsg += "!TCW";
    					if (!o.tcwHasHit.equals("H")) cntStartTCWh++;
    				}
    				if (o.chkEnd!=o.tcwEnd)  {
    					cntEndTCW++; 
    					enMsg += "!TCW";
    					if (!o.tcwHasHit.equals("H")) cntEndTCWh++;
    				}
				}
    			// TD
    			if (o.tdFrame==0)  {
					cntNoTD++; 
					noMsg += "!TD";
					if (!o.tdHasHit.equals("H")) cntNoTDh++;
				}
    			else if (o.tdFrame!=o.chkFrame) { 
					cntFrTD++;
					frMsg +="!TD";
					if (!o.tdHasHit.equals("H")) cntFrTDh++;
    			}
				else if (o.tdStart>o.chkEnd || o.tdEnd<o.chkStart)  {
					cntOlapTD++; 
					olMsg += "!TD";
					if (!o.tdHasHit.equals("H")) cntOlapTDh++;
				}
				else {
    				if (o.chkStart!=o.tdStart)  {
    					cntStartTD++;
    					stMsg += "!TD";
    					if (!o.tdHasHit.equals("H")) cntStartTDh++;
    				}
    				if (o.chkEnd!=o.tdEnd)  {
    					cntEndTD++; 
    					enMsg += "!TD";
    					if (!o.tdHasHit.equals("H")) cntEndTDh++;
    				}
				}
    			
    			// compose remarks
    			
    			isFr= frMsg.contains("TCW") || frMsg.contains("TD");
				isSt= stMsg.contains("TCW") || stMsg.contains("TD");
				isEn= enMsg.contains("TCW") || enMsg.contains("TD");
				isOl= olMsg.contains("TCW") || olMsg.contains("TD");
				isNo= noMsg.contains("TCW") || noMsg.contains("TD");
				
	    		if (isFr || isSt || isEn || isOl || isNo) {	
    				if (isFr) {
    					frMsg = frMsg.replace("!TCW!TD", "!Both"); // Can be TCW!=TD!=Chk
    					difMsg += frMsg + " ";
    				}
    				if (isSt) {
    					stMsg = stMsg.replace("!TCW!TD", "!Both");
    					difMsg += stMsg + " ";
    				}
    				if (isEn) {
    					enMsg = enMsg.replace("!TCW!TD", "!Both");
    					difMsg += enMsg + " ";
    				}
    				if (isOl) {
    					olMsg = olMsg.replace("!TCW!TD", "!Both");
    					difMsg += olMsg + " ";
    				}
    				if (isNo) {
    					noMsg = noMsg.replace("!TCW!TD", "!Both");
    					difMsg += noMsg + " ";
    				}
    	
    				line = String.format("%s\t%s (%d,%d,%d,%d,%s) (%d,%d,%d,%d,%s)",name, difMsg,
	    					o.tcwFrame, o.tcwStart, o.tcwEnd, o.tcwLen, o.tcwHasHit, 
	    					o.tdFrame,  o.tdStart,  o.tdEnd,  o.tdLen,  o.tdHasHit);
	    			rmkFH.println(line);
    			}
    		} /** End Loop **/
    		rmkFH.close();
    		
    	/** final stats **/
    		PrintWriter difFH = new PrintWriter(new FileOutputStream(difFile+suffix, false));
    		String label;
    		if (bNoORF) { // write first time only
	    		prt("");
	    		prt(difFH, String.format("TCW=TR %4d  With Hit %4d", cntTCWeqTR, cntTCWeqTRh)); 
	    		prt(difFH, String.format("TR =TD %4d  With Hit %4d", cntTDeqTR,  cntTDeqTRh));
	    		prt(difFH, String.format("TCW=TD %4d  With Hit %4d", cntTCWeqTD, cntTCWeqTDh));
	    		prt(difFH,"");
    			prt(difFH,String.format("Ignore TCW!=TD  Frame %d  Start %d  End %d    TCW>TD %4d   TCW<TD %4d", 
        				cntFrDiff, cntStartDiff,cntEndDiff, tcwGTtd,tcwLTtd));
    			label="Ignore";
    		}
    		else label="Include";
    		 
    		prt(difFH,"");
    		prt(difFH,String.format("%6s  %4s (%3s)   %4s (%3s)   %4s (%3s)  %4s (%3s)  %4s (%3s)",
    				label, "None", "Hit", "Frame","Hit", "Start","Hit", "End","Hit", "Olap","Hit"));
    		prt(difFH,String.format("%6s  %4d (%3d)   %4d (%3d)   %4d (%3d)   %4d (%3d)   %4d (%3d)", 
    				"TCW", cntNoTCW, cntNoTCWh, cntFrTCW, cntFrTCWh, cntStartTCW, cntStartTCWh, cntEndTCW, cntEndTCWh, 
    				cntOlapTCW, cntOlapTCWh )); 						
    		prt(difFH,String.format("%6s  %4d (%3d)   %4d (%3d)   %4d (%3d)   %4d (%3d)   %4d (%3d)", 
    				"TD", cntNoTD, cntNoTDh, cntFrTD,  cntFrTDh, cntStartTD, cntStartTDh,  cntEndTD, cntEndTDh, 
    				cntOlapTD, cntOlapTDh)); 
    	
    		difFH.close();
		}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	private static void readTD() {
	try {
		// >NM_001048275.2.p1 GENE.NM_001048275.2~~NM_001048275.2.p1  ORF type:complete len:540 (+),score=69.23,sp|Q52RG7|SGPL_ORYSJ|100.0|1.1e-312 NM_001048275.2:1-1620(+)
		Pattern pat =   Pattern.compile("(\\S+):(\\d+)-(\\d+)(.+$)"); // NM_001048275.2:1-1620(+)
		
		BufferedReader reader = new BufferedReader ( new FileReader ( tdfFile ) );
		String line="", name="", sign="";
		int cnt=0, start=0, end=0, cntHit=0;
		
		while ((line = reader.readLine()) != null) {
			String [] tok = line.split("\\s");
	
			String lastTok = tok[tok.length-1];
			Matcher m = pat.matcher(lastTok);
			if (m.matches()) {
				name =  m.group(1);
				start = Integer.parseInt(m.group(2));
				end =   Integer.parseInt(m.group(3));
				sign =  m.group(4);
			}
			else die("fail " +lastTok);
			
			if (!tcwORFmap.containsKey(name)) {
				prt("Not in TCW " + name);
				continue; 
			}
			
			ORF o = tcwORFmap.get(name);
		
			int olen =   Math.abs(end-start)+1;
			
			int frame;
			if (sign.equals("(+)")) {
				frame = start%3;
				if (frame==0) frame=3;
			}
			else {
				int e = end;
				end = o.seqLen-start+1;
				start = o.seqLen-e+1;
				frame = start%3;
				if (frame==0) frame=3;
				frame = -frame;
			}
			String hit = (line.contains("sp|")) ? "H" : "-";
			o.tdAdd(frame, start, end, olen, hit);
			if (o.tcwHasHit=="H") cntHit++;
			cnt++;
		}
		reader.close(); 
		
		prt(String.format("TD   Read %5d  HasHit %4d  %s", cnt, cntHit, tdfFile));
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	// File-SP: #Seq-ID #SwissProt      ORF-Frame       ORF-Start       ORF-End
	// File-TR: #Seq-ID Length          ORF-Frame       ORF-Start       ORF-End
	private static void readTCW(boolean isNotChk) {
    try {
    	String file=chkFile;
    	if (isNotChk) file=tcwFile; 
    	
		int cnt=0, cntHit=0;
		BufferedReader reader = new BufferedReader ( new FileReader ( file ) );
		String line = reader.readLine();
		String [] tok = line.split("\\t");
		if (tok.length!=5) die("Wrong format:" + line);
			
		while ((line = reader.readLine()) != null) {
			tok = line.split("\\t");
			if (tok.length<4) {
				prt("Ignore: " + line);
				continue;
			}
			String name = tok[0];
			int hf= Integer.parseInt(tok[1]);
			int f = Integer.parseInt(tok[2]);
			int s = Integer.parseInt(tok[3]);
			int e = Integer.parseInt(tok[4]);
			int l = Math.abs(e-s)+1;
			
			if (isNotChk) { // Subset SP
				ORF o = new ORF();
				o.tcwFrame = f;
				o.tcwStart = s;
				o.tcwEnd =   e;
				o.tcwLen =   l;
				o.tcwHasHit = (hf!=0) ? "H" : "-";
				if (hf!=0) cntHit++;
				
				tcwORFmap.put(name, o);
			}
			else {		// Full TR
				if (tcwORFmap.containsKey(name)) {
					ORF o = tcwORFmap.get(name);
					o.chkFrame=f;
					o.chkStart = s;
					o.chkEnd = e;
					o.chkLen = l;
					o.seqLen = hf;
				}
			}
			cnt++;
		}
		reader.close();
		if (!isNotChk) {
			for (String name : tcwORFmap.keySet()) {
				ORF o = tcwORFmap.get(name);
				if (o.chkFrame==0) prt("Missing " + name);
			}
		}
		String x = (isNotChk) ? "TCW " : "Full";
		
		prt(String.format("%4s Read %5d  HasHit %4d  %s", x, cnt, cntHit, tdfFile));
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	/*********************************************************************************/
	private static void prt(String msg) {
		System.out.println(msg);
	}
	private static void prt(PrintWriter fd, String msg) {
		fd.println(msg);
		System.out.println(msg);
	}
	private static void die(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}
	private static HashMap <String, ORF> tcwORFmap = new HashMap <String, ORF> ();
	private static class ORF {
		int seqLen;
		int tcwFrame, tcwLen, tcwStart, tcwEnd; // file1 coords
		String tcwHasHit="?";
		
		int  chkFrame=0, chkStart=0, chkEnd=0, chkLen=0; // presumably the correct frame from the File2
		
		// from TransDecoder file
		private void tdAdd(int frame, int start, int end, int olen, String hit) {
			tdFrame=frame; tdLen=olen;  tdStart=start; tdEnd=end; tdHasHit=hit;
		}
		int tdFrame=0, tdLen, tdStart, tdEnd; // file3 TD
		String tdHasHit="-";
	}
}
