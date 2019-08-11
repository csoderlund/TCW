package scripts;

/*****************************************************************
* Compares TCW and TransDecoder ORFs.
* 
* Input format
* 1: SeqID   Length  TCWRemarks      ORFFrame        ORFStart        ORFEnd   ProteinFrame
* 2: >NM_001048268.2.p1 GENE.NM_001048268.2~~NM_001048268.2.p1  ORF type:complete len:411 (+),score=44.70 NM_001048268.2:198-1430(+)
* File 1: format 1 output from sTCWdb: os500 against SP_plants
* File 2: format 1 output from sTCWdb: OS against TR_plants (and other annoDBs)
* File 3: grep ">" <prefix>.transdecoder.cds >TD.sp.results
* 		  format 2 output from TD where longest_orf.pep searched against SP_plants
*         and --single_best_only --retain_blastp_hits td_SPpla.dmnd.tab
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
	static String db="os2"; 
	
 	static private String dir, tcwFile, tdfFile, outFile, rmkFile,
		chkFile, tcwMkFile, tdMkFile;
	
	public static void main(String[] args) {
		cmp();
	}
	
	private static void cmp() {			
		dir =     	"paper/ORF/" + db; // directory of files and results
		tcwFile = 	dir + "/TCW.results";     // File1 - TCW SP annotated
		chkFile = 	dir + "/TCW.Os.results";  // File2 - TCW full annotated
		tdfFile = 	dir + "/TD.results";		  // File3 - TD SP annotated
		tcwMkFile = dir + "/bestORFs.scores.txt";		// from TCW orfFiles
		tdMkFile = 	dir + "/longest_orfs.cds.scores";	// from TD  files
		outFile = 	dir + "/OutDiff.txt";
		rmkFile =   dir + "/OutRemarks.txt";
		
		readTCW(true);
		readTCW(false);
		readTD();
		readMarkov();
		process();
	}
	private static void process() {
	    	try {
	    		prt("+++Process ");
	    		
	    		int cntSame=0, cntF=0, cntFw=0, cntS=0, cntE=0, cntO=0, cntD=0, cntFm=0, cntFt=0, cntDm=0, cntDt=0;
	    		int cntNF=0, tcwGTtd=0, tcwLTtd=0;
	    		
	    		String allFrame="", saveDiff="", saveNotFound="";
	    		TreeMap <String, String> sFrame = new TreeMap <String, String>();
	    		TreeMap <String, String> sDiff = new TreeMap <String, String>();
	    		
	    		PrintWriter out = new PrintWriter(new FileOutputStream(outFile, false));
	    		
	    		for (String name : tcwORFmap.keySet()) {
	    			ORF o = tcwORFmap.get(name);
	    			if (o.tdFrame==0) {
	    				cntNF++;
	    				saveNotFound += String.format("%-15s NO-TD\n", name);
	    				continue;		
	    			}
	    			
		    		if (o.frame==o.tdFrame && o.start==o.tdStart && o.end==o.tdEnd) {
		    			cntSame++;
		    			if (o.frame!=o.chkFrame) cntFw++;
		    			continue;
	    			}
	    			
	    			String msg="?";
	    			if (o.frame!=o.tdFrame) {
	    				msg="F";
	    				cntF++;
	    			}
	    			else if (isNotOlap(o.start, o.end, o.tdStart, o.tdEnd)) {
	    				msg="Dif";
	    				cntD++;
	    			}
	    			else if (o.tdStart!=o.start && o.tdEnd!=o.end) {
	    				msg="O";
	    				cntO++;
	    			}
	    			else if (o.frame > 0   && o.tdStart!=o.start && o.tdEnd==o.end) {
	    				msg="S";
	    				cntS++;
	    			}
	    			else if (o.tdFrame < 0 && o.tdStart==o.start && o.tdEnd!=o.end) {
	    				msg="S";
	    				cntS++;
	    			}
	    			else if (o.frame > 0 && o.tdStart==o.start && o.tdEnd!=o.end) {
	    				msg="E";
	    				cntE++;
	    			}
	    			else if (o.tdFrame < 0 && o.tdStart!=o.start && o.tdEnd==o.end) {
	    				msg="E";
	    				cntE++;
	    			}
	    			else {
	    				cntD++;
	    				msg="???";
	    			}
	    	
    				if (o.olen>o.tdOlen) tcwGTtd++;
    				if (o.olen<o.tdOlen) tcwLTtd++;
    				
    				// output
    				String line = String.format("%3s %s (%2d, %4d, %4d, %4d%s) (%2d, %4d, %4d, %4d%s)  %5d  %2d %s\n", 
        					msg, name, o.frame, o.start, o.end, o.olen, o.tcwHasHit,
        					o.tdFrame, o.tdStart, o.tdEnd, o.tdOlen, o.tdHasHit,
        					(o.olen-o.tdOlen), o.chkFrame, o.remark);
    				
    				if (msg.equals("F")) {
    					String x="F---";
    					if (o.frame==o.chkFrame) {
    						cntFm++;
    						x="fTCW";
    					}
    					else if (o.tdFrame==o.chkFrame) {
    						cntFt++;
    						x="fTD ";
    					} 
    					
    					sFrame.put(name, x + line);
    					allFrame += String.format("%-15s %4s %2d (%2d,%4d,%4d,%4d,%s) (%2d,%4d,%4d,%4d%s)\n",
    						name,  x, o.chkFrame, o.frame, o.start, o.end, (o.end-o.start), o.tcwHasHit,
    						o.tdFrame, o.tdStart, o.tdEnd, (o.tdEnd-o.tdStart), o.tdHasHit);
    				}
    				else if (msg.equals("Dif")) {
    					String x="D---";
    					if (!isNotOlap(o.tdStart, o.tdEnd, o.chkStart, o.chkEnd)) {
    						x="dTCW";
    						cntDm++;
    					}
    					else if (!isNotOlap(o.start, o.end, o.chkStart, o.chkEnd)) {
    						x="dTD ";
    						cntDt++;
    					}
    					sDiff.put(name, x+line);
    					saveDiff  += String.format("%-15s %4s (%4d, %4d,%4d%s) (%4d,%4d,%4d%s)\n",
        					name,  x, o.start, o.end,(o.end-o.start), o.tcwHasHit, 
        					o.tdStart, o.tdEnd, (o.tdEnd-o.tdStart), o.tdHasHit);
    				}
    				else out.format(line);
	    		}
	    		out.println("\nDiff frame:\n");
	    		String [] order = {"---", "TD", "TCW"};
	    		for (int i=0; i<order.length; i++) {
		    		for (String n : sFrame.keySet()) {
		    			String l = sFrame.get(n);
		    			if (l.contains(order[i])) {
		    				out.print("\n" + sFrame.get(n));
		    				ORF o = tcwORFmap.get(n);
		    				out.println(o.tcwMK);
		    				out.println(o.tdMK);
		    			}
		    		}
	    		}
	    		out.println("\nDiff coords:");
	    		for (String n : sDiff.keySet()) out.print(sDiff.get(n));
	    		
	    		out.println();
	    		out.println("   NF:     " + cntNF);		prt("   NF:     " + cntNF);
	    		out.println("   Same :  " + cntSame);	prt("   Same :  " + cntSame);
	    		out.println("     Wrong:" + cntFw);		prt("     Wrong:" + cntFw);
	    		out.println("   Start:  " + cntS);		prt("   Start:  " + cntS);
	    		out.println("   End:    " + cntE);		prt("   End:    " + cntE);
	    		out.println("   Olap:   " + cntO);		prt("   Olap:   " + cntO);
	    		out.println("   Diff:   " + cntD);		prt("   Diff:   " + cntD);
	    		out.println("     TCW:  " + cntDm);		prt("     TCW:  " + cntDm);
	    		out.println("     TD :  " + cntDt);		prt("     TD :  " + cntDt);
	    		out.println("   Frame:  " + cntF);		prt("   Frame:  " + cntF);
	    		out.println("     TCW:  " + cntFm);		prt("     TCW:  " + cntFm);
	    		out.println("     TD :  " + cntFt);		prt("     TD :  " + cntFt);
	    		out.println("   TCW>TD: " + tcwGTtd);	prt("   TCW>TD: " + tcwGTtd);
	    		out.println("   TCW<TD: " + tcwLTtd);	prt("   TCW<TD: " + tcwLTtd);
	    		out.close();
	    		
	    		out = new PrintWriter(new FileOutputStream(rmkFile, false));
	    		out.println(allFrame);
	    		out.println(saveDiff);
	    		out.println(saveNotFound);
	    		out.close();
		}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	private static boolean isNotOlap(int s1, int e1, int s2, int e2) {
		if (s1>e2 || s2>e1 || e1<s2 || e2<s1) return true;
		return false;
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
				int s = o.len-end+1;
				frame = s%3;
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
	
	// File1: SeqID   Length  TCWRemarks      ORFFrame        ORFStart        ORFEnd  ProteinFrame
	private static void readTCW(boolean isNotChk) {
    	try {
    		String file=chkFile;
    		if (isNotChk) file=tcwFile; 
    		prt("+++Read " + file);
		int cnt=0;
		BufferedReader reader = new BufferedReader ( new FileReader ( file ) );
		String line = reader.readLine();
		String [] tok = line.split("\\t");
		if (tok.length!=7) die("Wrong format:" + line);
			
		while ((line = reader.readLine()) != null) {
			tok = line.split("\\t");
			if (tok.length<4) {
				prt("Ignore: " + line);
				continue;
			}
			String name = tok[0];
			ORF o = new ORF();
			o.len = Integer.parseInt(tok[1]);
			o.remark = tok[2];
			o.frame = Integer.parseInt(tok[3]);
			
			o.start =   Integer.parseInt(tok[4]);
			o.end =     Integer.parseInt(tok[5]);
			o.olen =    Math.abs(o.end-o.start)+1;
			if (tok.length>=7) {
				int hf = Integer.parseInt(tok[6]);
				if (hf!=0) o.tcwHasHit=" H";
				else o.tcwHasHit="";
			}
			if (o.frame<0) {
				int x = o.end;
				o.end =   o.len-o.start+1;
				o.start = o.len-x+1;
			}
			
			if (isNotChk) tcwORFmap.put(name, o);
			else {
				ORF ox = tcwORFmap.get(name);
				ox.chkFrame=o.frame;
				ox.chkStart=o.start;
				ox.chkEnd=o.end;
				ox.remark=o.remark;
			}
			cnt++;
		}
		reader.close();
		prt("   Good: " + cnt);
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/**************************************************
	 * BestORFscores.txt from TCW
	 * >NM_001048268.2 type:Complete seqLen:4025 orfLen:1233 frame:3 coords:198..1430
 		Markov   16.06  -13.55  -84.62  -28.13  -33.46  -63.84    3
 	 * longest_orf_cds.scores
 	 * NM_001048268.2.p1       5       1233    44.70   -22.87  -51.38  -11.85  -31.54  -46.58
	 */
	private static void readMarkov() {
		try {
    		String file = tcwMkFile;
    		prt("+++Read " + file);
		int cnt=0;
		String line, name="";
		BufferedReader reader = new BufferedReader ( new FileReader ( file ) );
		
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) continue;
			if (line.startsWith(">")) name = line.substring(1, line.indexOf(" "));
			else {
				ORF o = tcwORFmap.get(name);
				o.tcwMK = "   " + line;
				cnt++;
			}
		}
		reader.close();
		prt("   Good: " + cnt);
		
		// longest_orfs
		file = tdMkFile;
		prt("+++Read " + file);
		name="";
		cnt=0;
		reader = new BufferedReader ( new FileReader ( file ) );
		
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#")) continue;
			String [] tok = line.split("\\s+");
			name = tok[0].substring(0, tok[0].lastIndexOf("."));
			int len = Integer.parseInt(tok[2]);
			
			ORF o = tcwORFmap.get(name);
			if (len==o.tdOlen) {
				o.tdMK = "              ";
				for (int i=3; i<tok.length; i++) o.tdMK+= tok[i] + "  ";
				cnt++;
			}
		}
		reader.close();
		prt("   Good: " + cnt);		
	}
	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	private static void prt(String msg) {
		System.out.println(msg);
	}
	private static void die(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}
	private static HashMap <String, ORF> tcwORFmap = new HashMap <String, ORF> ();
	private static class ORF {
		int len, frame, olen, start, end;
		String tcwHasHit="";
		
		int  chkFrame=0, chkStart=0, chkEnd=0; // presumably the correct frame from the File2
		String remark;      // remark from File2, with full annotation
		
		// from TransDecoder file
		private void tdAdd(int frame, int start, int end, int olen, String hit) {
			tdFrame=frame; tdOlen=olen;  tdStart=start; tdEnd=end; tdHasHit=hit;
		}
		int tdFrame=0, tdOlen, tdStart, tdEnd;
		String tdHasHit="";
		
		String  tdMK="", tcwMK=""; // from score files
	}
}
