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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ORFcmp {
	static String sub = "os2";
	static String dir =     	"paper/ORF/" + sub + "/"; 			 // directory of files and results
	static String tcwFile = 	dir + "/SeqTableColumnsSP.tsv";  	// File1 - TCW SP annotated 
	static String chkFile = 	dir + "/SeqTableColumnsFull.tsv";  	// File2 - TCW TR annotated for correct
	static String tdfFile = 	dir + "/TD.results";		  		// File3 - TD SP annotated
	static String outFile = 	dir + "/OutDiff.txt";
	static String rmkFile =     dir + "/OutRemarks.txt";

	public static void main(String[] args) {
		readTCW(true);
		readTCW(false);
		readTD();
		process();
	}
	
	private static void process() {
    	try {
    		prt("+++Process " + sub);
    		int cntSame=0, cntTDsame=0, cntTCWsame=0;
    		int cntFr=0, cntFrTD=0, cntFrTCW=0, cntFrDiff=0; 
    		int cntStart=0, cntStartTD=0, cntStartTCW=0, cntStartDiff=0;
    		int cntEnd=0, cntEndTD=0, cntEndTCW=0, cntEndDiff=0;
    		int tcwGTtd=0, tcwLTtd=0;
    		
    		String line;
    		TreeMap <String, String> rmkFrame = new TreeMap <String, String>();
    		TreeMap <String, String> rmkDiff = new TreeMap <String, String>();
    		ArrayList <String> mkSame = new ArrayList <String> ();
    		
    		PrintWriter out = new PrintWriter(new FileOutputStream(outFile, false));
    		
    		for (String name : tcwORFmap.keySet()) {
    			ORF o = tcwORFmap.get(name);
    			String msg="?";
    			
    			if (o.tcwFrame==o.tdFrame  && o.tcwStart==o.tdStart  && o.tcwEnd==o.tdEnd)  {
    				cntSame++;
    				mkSame.add(name);
    			}
    			if (o.tcwFrame==o.chkFrame && o.tcwStart==o.chkStart && o.tcwEnd==o.chkEnd) cntTCWsame++;
    			if (o.tdFrame==o.chkFrame  && o.tdStart==o.chkStart  && o.tdEnd==o.chkEnd)  cntTDsame++;			
    			
    			boolean isDiffFr=false, isDiffCoords=false;
    			if (o.tdFrame!=o.chkFrame || o.tcwFrame!=o.chkFrame) {
    				msg="Frame " + o.chkFrame;
    				cntFr++;
					if (o.tcwFrame!=o.chkFrame) {
						cntFrTCW++;
						msg+=" !TCW";
					}
					if (o.tdFrame!=o.chkFrame) {
						cntFrTD++;
						msg+=" !TD";
					} 
					line = String.format("%s (%d) (%d)", msg, o.tcwFrame, o.tdFrame);
					rmkFrame.put(name, line);
					isDiffFr=true;
					if (o.tcwFrame!=o.tdFrame) cntFrDiff++;
    			}
    			else {
					if (o.tdStart!=o.chkStart || o.tcwStart!=o.chkStart){
	    				msg="Start " + o.chkStart;
	    				cntStart++;
	    				if (o.chkStart!=o.tcwStart)     {
	    					cntStartTCW++; 
	    					msg += " !TCW";
	    				}
	    				if (o.chkStart!=o.tdStart)  {
	    					cntStartTD++;
	    					msg += " !TD";
	    				}
	    				isDiffCoords=true;
	    				if (o.tcwStart!=o.tdStart) cntStartDiff++;
	    			}
	    			if (o.tdEnd!=o.chkEnd || o.tcwEnd!=o.chkEnd) {
	    				if (msg=="?") msg = "End " + o.chkEnd;
	    				else 		  msg += " End " + o.chkEnd;
	    				cntEnd++;
	    				if (o.chkEnd!=o.tcwEnd)  	 {
	    					cntEndTCW++; 
	    					msg += " !TCW";
	    				}
	    				if (o.chkEnd!=o.tdEnd)  {
	    					cntEndTD++; 
	    					msg += " !TD";
	    				}
	    				isDiffCoords=true;
	    				if (o.tcwEnd!=o.tdEnd) 	   cntEndDiff++;
	    			}
		    		if (isDiffCoords) {	
	    				line = String.format("%s (%d,%d,%d) (%d,%d,%d)",
		    					msg, o.tcwStart, o.tcwEnd, o.tcwLen, o.tdStart, o.tdEnd, o.tdLen);
	    				rmkDiff.put(name, line);
		    		}
    			}
    			
    			if (isDiffFr || isDiffCoords) {
					if (o.tcwLen>o.tdLen) tcwGTtd++;
					if (o.tcwLen<o.tdLen) tcwLTtd++;
					
					line = String.format("%-20s %-15s TCW=(%2d, %4d, %4d, %4d%s) TD=(%2d, %4d, %4d, %4d%s)  %5d  %2d %s\n", 
	    					msg, name, o.tcwFrame, o.tcwStart, o.tcwEnd, o.tcwLen, o.tcwHasHit,
	    					o.tdFrame, o.tdStart, o.tdEnd, o.tdLen, o.tdHasHit,
	    					(o.tcwLen-o.tdLen), o.chkFrame, o.chkRemark);
					out.format(line);
    			}
    		}	
    		out.println();
    		line = String.format("%-10s %d", "TCW=TD", 		cntSame); 		out.println(line); prt(line);
    		
    		line = String.format("%-10s %d", "TCW good", 	cntTCWsame); 	out.println(line); prt(line);
    		line = String.format("%-10s %d", "TD  good", 	cntTDsame); 	out.println(line); prt(line);
    		
    		line = String.format("%-10s %d", "TCW>TD", 		tcwGTtd); 		out.println(line); prt(line);
    		line = String.format("%-10s %d", "TCW<TD", 		tcwLTtd); 		out.println(line); prt(line);
    		
    		line = String.format("%-10s: ", "Incorrect");					out.println(line); prt(line);
    		line = String.format("  %-10s %d", "Frame", 	cntFr); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW", 	cntFrTCW); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TD", 		cntFrTD); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW!=TD", 	cntFrDiff); 	out.println(line); prt(line);
    		
    		line = String.format("  %-10s %d", "Start", 	cntStart); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW", 	cntStartTCW); 	out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TD", 		cntStartTD); 	out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW!=TD", 	cntStartDiff); out.println(line); prt(line);
    		
    		line = String.format("  %-10s %d", "End", 		cntEnd); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW", 	cntEndTCW); 	out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TD", 		cntEndTD); 		out.println(line); prt(line);
    		line = String.format("  %-10s %d", "  TCW!=TD", 	cntEndDiff); 	out.println(line); prt(line);
    		
    		out.close();
    		
    		// Remark file
    		out = new PrintWriter(new FileOutputStream(rmkFile, false));
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
		prt("+++Read " + tdfFile);
		
		BufferedReader reader = new BufferedReader ( new FileReader ( tdfFile ) );
		String line="", name="", sign="";
		int cnt=0, start=0, end=0;
		
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
			String hit = (line.contains("sp|")) ? " H" : "";
			o.tdAdd(frame, start, end, olen, hit);
			cnt++;
		}
		reader.close(); 
		
		prt("   Read " + cnt);
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	// File1: SeqID   Length  TCWRemarks      ORFFrame        ORFStart        ORFEnd 
	private static void readTCW(boolean isNotChk) {
    try {
    	String file=chkFile;
    	if (isNotChk) file=tcwFile; 
    	prt("+++Read " + file);
		int cnt=0;
		BufferedReader reader = new BufferedReader ( new FileReader ( file ) );
		String line = reader.readLine();
		String [] tok = line.split("\\t");
		if (tok.length!=6) die("Wrong format:" + line);
			
		while ((line = reader.readLine()) != null) {
			tok = line.split("\\t");
			if (tok.length<4) {
				prt("Ignore: " + line);
				continue;
			}
			String name = tok[0];
			int seqLen = Integer.parseInt(tok[1]);
			String remark = tok[2];
			int f = Integer.parseInt(tok[3]);
			int s = Integer.parseInt(tok[4]);
			int e = Integer.parseInt(tok[5]);
			int l = Math.abs(e-s)+1;
			int hf=0;
			if (tok.length>=7) 
				hf = Integer.parseInt(tok[6]);
			
			if (isNotChk) {
				ORF o = new ORF();
				o.seqLen = seqLen;
				o.tcwFrame = f;
				o.tcwStart = s;
				o.tcwEnd =   e;
				o.tcwLen =   l;
				o.tcwHasHit = (hf==1) ? " H" : "";
				
				tcwORFmap.put(name, o);
			}
			else {
				if (tcwORFmap.containsKey(name)) {
					ORF o = tcwORFmap.get(name);
					o.chkFrame=f;
					o.chkStart = s;
					o.chkEnd = e;
					o.chkRemark= remark;
				}
			}
			cnt++;
		}
		reader.close();
		prt("   Good: " + cnt);
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	/*********************************************************************************/
	private static void prt(String msg) {
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
		String tcwHasHit="";
		
		int  chkFrame=0, chkStart=0, chkEnd=0; // presumably the correct frame from the File2
		String chkRemark;      				   // remark from File2, with full annotation
		
		// from TransDecoder file
		private void tdAdd(int frame, int start, int end, int olen, String hit) {
			tdFrame=frame; tdLen=olen;  tdStart=start; tdEnd=end; tdHasHit=hit;
		}
		int tdFrame=0, tdLen, tdStart, tdEnd; // file3 TD
		String tdHasHit="";
	}
}
