package scripts;
/*****************************************************************
* Compares TCW and TransDecoder ORFs.
* 
* To produce test set:
* 	viewSingleTCW pl_Osj created testsets
* To produce the TD results, the following commands were executed:
*   >./TransDecoder.LongOrfs -t osj1.fna
*   # transdecoder_dir/longest_orfs.pep was searched against SP_plant with results in osTD_SPpl.tab
*   #	by creating an sTCWdb with longest_orfs.pep, then using TCW defaults (DIAMOND) against SP_plants
*   >./TransDecoder.Predict --retain_blastp_hits odTD_SPpl.tab   -–single_best_only -t osj1.fna
*   >grep “>”osj1.fna.transdecoder.cds >TD.results
* 
* To produce TCW:
* 	Build sTCW_os1 with osj1 sequences, search with defaults against SP_plants.
* 	Use sTCW_pl_Osj of NCBI rice transcripts 
*   For both, select Format 1 columns and Export table 
*   
* Input format
* 1: SeqID   Length  TCWRemarks      ORFFrame        ORFStart        ORFEnd  
* 2: >NM_001048268.2.p1 GENE.NM_001048268.2~~NM_001048268.2.p1  ORF type:complete len:411 (+),score=44.70 NM_001048268.2:198-1430(+)
* File 1: format 1 output from sTCWdb: sTCW_osS1 against SP_plants
* File 2: format 1 output form sTCWdb: sTCW_pl_Osj
* File 3: format 2 output from TD grep 
*         
* Produces a OutDiff.txt file and OutRemark.txt, where the remark file can be
* added to the TCW database using runSingleTCW.
*/

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ORFcmp {
	static String chkFile = 	"paper/ORF/SeqTableColumnsFull.tsv";  // File2 - TCW TR annotated for correct
	
	static String sub = "os2";
	static String dir =     	"paper/ORF/" + sub + "/"; 		// directory of files and results
	static String tcwFile = 	dir + "SeqTableColumnsSP.tsv";  // File1 - TCW SP annotated
	static String tdfFile = 	dir + "TD.results";		  		// File3 - TD SP annotated
	
	static String rmkFile =     dir + "Remarks.txt";
	static String difFile =     dir + "Diff.txt";

	public static void main(String[] args) {
		prt("Start " + sub);
		readTCW(true);
		readTCW(false);
		readTD();
		process();
		prt("Finish " + sub);
	}
	
	private static void process() {
    	try {
    		int cntTCWeqTD=0, cntTDeqTR=0, cntTCWeqTR=0;
    		int cntTCWeqTDh=0, cntTDeqTRh=0, cntTCWeqTRh=0;
    		int cntFrTD=0, cntFrTCW=0, cntFrDiff=0; 
    		int cntStartTD=0, cntStartTCW=0, cntStartDiff=0;
    		int cntEndTD=0, cntEndTCW=0, cntEndDiff=0;
    		int tcwGTtd=0, tcwLTtd=0;
    		
    		String line;
    		TreeMap <String, String> rmkFrame = new TreeMap <String, String>();
    		TreeMap <String, String> rmkDiff = new TreeMap <String, String>();
    		
    		PrintWriter difFH = new PrintWriter(new FileOutputStream(difFile, false));
    		
    		for (String name : tcwORFmap.keySet()) {
    			ORF o = tcwORFmap.get(name);
    			
    			boolean tdSame=false, tcwSame=false;
    			if (o.tcwFrame==o.tdFrame  && o.tcwStart==o.tdStart  && o.tcwEnd==o.tdEnd)  {
    				cntTCWeqTD++;
    				if (o.tcwHasHit=="H" && o.tdHasHit=="H") cntTCWeqTDh++;
    			}
    			if (o.tcwFrame==o.chkFrame && o.tcwStart==o.chkStart && o.tcwEnd==o.chkEnd) {
    				cntTCWeqTR++;
    				if (o.tcwHasHit=="H") cntTCWeqTRh++;
    				tcwSame=true;
    			}
    			if (o.tdFrame==o.chkFrame  && o.tdStart==o.chkStart  && o.tdEnd==o.chkEnd)  {
    				cntTDeqTR++;
    				if (o.tdHasHit=="H") cntTDeqTRh++;
    				tdSame=true;
    			}
    			if (tdSame && tcwSame) continue;
    			
    			if (o.tcwFrame!=o.tdFrame) cntFrDiff++;
    			else {
    				if (o.tcwStart!=o.tdStart) cntStartDiff++;
    				if (o.tcwEnd!=o.tdEnd) 	   cntEndDiff++;
    			}
    			String difMsg="";
    			String frMsg="Frame " + o.chkFrame + " ";
    			String stMsg="Start " + o.chkStart + " ";
    			String enMsg="End "   + o.chkEnd + " ";    
    			boolean isDiffFr=false, isDiffSt=false, isDiffEn=false;
    			
    			if (o.tcwFrame!=o.chkFrame) {
					cntFrTCW++;
					frMsg +="!TCW";
    			}
    			else {
    				if (o.chkStart!=o.tcwStart)  {
    					cntStartTCW++;
    					stMsg += "!TCW";
    				}
    				if (o.chkEnd!=o.tcwEnd)  {
    					cntEndTCW++; 
    					enMsg += "!TCW";
    				}
    			}
    			if (o.tdFrame!=o.chkFrame) {
					cntFrTD++;
					frMsg +="!TD";
    			}
    			else {
    				if (o.chkStart!=o.tdStart)  {
    					cntStartTD++;
    					stMsg += "!TD";
    				}
    				if (o.chkEnd!=o.tdEnd)  {
    					cntEndTD++; 
    					enMsg += "!TD";
    				}
    			}
    			if (frMsg.contains("TCW") || frMsg.contains("TD")) {
    				line = String.format("%s (%d,%d,%s) (%d,%d,%s)", frMsg, 
    						o.tcwFrame,o.tcwLen, o.tcwHasHit, o.tdFrame, o.tdLen, o.tdHasHit);
					rmkFrame.put(name, line);
					difMsg = frMsg;
					isDiffFr=true;
    			}
				isDiffSt= stMsg.contains("TCW") || stMsg.contains("TD");
				isDiffEn= enMsg.contains("TCW") || enMsg.contains("TD");
				
	    		if (isDiffSt || isDiffEn) {	
    				String msg="";
    				if (isDiffSt) msg = stMsg + " ";
    				if (isDiffEn) msg += enMsg + " ";
    				difMsg += " " + msg;
	    			line = String.format("%s (%d,%d,%d,%s) (%d,%d,%d,%s)",
	    					msg, o.tcwStart, o.tcwEnd, o.tcwLen, o.tcwHasHit,
	    					     o.tdStart, o.tdEnd, o.tdLen, o.tdHasHit);
    				rmkDiff.put(name, line);
	    		}
    
    			if (isDiffFr || isDiffSt || isDiffEn) {
					if (o.tcwLen>o.tdLen) tcwGTtd++;
					if (o.tcwLen<o.tdLen) tcwLTtd++;
					
					String x = String.format("TCW=(%d,%d,%d,%d,%s)",o.tcwFrame, o.tcwStart, o.tcwEnd, o.tcwLen, o.tcwHasHit);
					String y = String.format("TD=(%d,%d,%d,%d,%s)", o.tdFrame, o.tdStart, o.tdEnd, o.tdLen, o.tdHasHit);
					line = String.format("%-15s %-20s %-30s %s",name, difMsg, x, y);
					difFH.println(line);
    			}
    		}
    		prt("");
    		prt(difFH, String.format("TCW=TR %4d  With Hit %4d  TCW>TD %4d ",cntTCWeqTR, cntTCWeqTRh, tcwGTtd)); 
    		prt(difFH, String.format("TD =TR %4d  With Hit %4d  TCW<TD %4d", cntTDeqTR, cntTDeqTRh,	tcwLTtd));
    		prt(difFH, String.format("TCW=TD %4d  With Hit %4d  ", cntTCWeqTD, cntTCWeqTDh));
    				
    		prt(difFH,"");
    		prt(difFH,String.format("          Frame   Start    End"));
    		prt(difFH,String.format("TCW       %5d     %3d    %3d", cntFrTCW,  cntStartTCW, cntEndTCW)); 						
    		prt(difFH,String.format("TD        %5d     %3d    %3d", cntFrTD,   cntStartTD,  cntEndTD)); 
    		prt(difFH,String.format("TCW!=TD   %5d     %3d    %3d", cntFrDiff, cntStartDiff,cntEndDiff)); 
    		difFH.close();
    		
    		// Remark file
    		PrintWriter out = new PrintWriter(new FileOutputStream(rmkFile, false));
    		for (String n : rmkFrame.keySet()) 	out.format("%-15s %s\n", n, rmkFrame.get(n));
    		for (String n : rmkDiff.keySet()) 	out.format("%-15s %s\n", n, rmkDiff.get(n));
    		out.close();
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
				name = m.group(1);
				start = Integer.parseInt(m.group(2));
				end = Integer.parseInt(m.group(3));
				sign = m.group(4);
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
				if (hf>0) cntHit++;
				
				tcwORFmap.put(name, o);
			}
			else {		// Full TR
				if (tcwORFmap.containsKey(name)) {
					ORF o = tcwORFmap.get(name);
					o.chkFrame=f;
					o.chkStart = s;
					o.chkEnd = e;
					o.seqLen = hf;
					
					if (hf>0) cntHit++;
				}
			}
			cnt++;
		}
		reader.close();
		String x = (isNotChk) ? "TCW " : "Full";
		prt(String.format("%s Read %5d  HasHit %4d  %s", x, cnt, cntHit, file));
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
		
		int  chkFrame=0, chkStart=0, chkEnd=0; // presumably the correct frame from the File2
		
		// from TransDecoder file
		private void tdAdd(int frame, int start, int end, int olen, String hit) {
			tdFrame=frame; tdLen=olen;  tdStart=start; tdEnd=end; tdHasHit=hit;
		}
		int tdFrame=0, tdLen, tdStart, tdEnd; // file3 TD
		String tdHasHit="?";
	}
}
