package scripts;

import java.io.*;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***********************************************
 * This has been written specifically for the D.citri project.
 * 
 * Input: 	
 * 1. GFF file with Genbank (gi) identifiers
 * e.g: NW_007377440.1  Gnomon  CDS     61136   62158   .       +       0       ID=cds0;Parent=rna0;Dbxref=GeneID:103504879,Genbank:XP_008467433.1;Name=XP_008467433.1;gbkey=CDS;
 *
 * 2. Scaffold names -- mapping from the genome site of 'scaffoldxxx' to name used in GFF
 * e.g: Diaci psyllid genome assembly version 1.1       scaffold1       NW_007377440.1  KI472552.1      GPS_004273110.1
 *
 * 3. DBHitTable exported from Basic annoDB: 
 * Format: row#, seqID, evalue, sim, alignLen, hitID
 * 
 * Output: 
 * Location file: TW seqID can only have one location, but multiple sequences can have the same location
 * Remark file: hitID, evalue, sim, align
 * 
 * http://www.ncbi.nlm.nih.gov/genome/annotation_euk/Diaphorina_citri/100/
 * This annotation should be referred to as NCBI Diaphorina citri Annotation Release 100
 */
public class MakeLocs {
	static private final boolean logAll=false; 
	// input
	static private String dbName = "DcGS";
	static private String dir = "dcfiles";
	static private String hitFile=dir +"/input/" + dbName + ".HitTable.tsv";
	static private String gffFile=dir +"/input/diaci.gff3";
	static private String scfFile=dir +"/input/scaffold_names";
	
	// output
	static private String locFile= dir + "/Locations." + dbName + ".txt";
	static private String rmkFile= dir +"/Remarks."+ dbName + ".txt";
	static private String scfCntFile= dir +"/Scaffold_cnt."+ dbName + ".txt";
	static private String multSeqFile= dir +"/seqMultHit."+  dbName + ".txt";
	static private String multHitFile= dir +"/hitMultSeq."+  dbName + ".txt";
	static private String logFile= dir +"/suspect."+  dbName + ".txt";
	static private PrintWriter logOut;
	public static void main(String[] args) {
		try {
			logOut = new PrintWriter(new FileWriter(logFile));
			
			readGFFforGenes(); // Get list of gene names in GFF file
			readTCWHit();   // seqMap(seqID, Seq) where Seq hitID with besteval
						    // hitMap(hitID, Hit) where Hit is assigned values in readGFF
			readScaffold(); // scfNameMap(NW_, scaffoldN) 
			                // scfCntMap(N, Scf) - Scf has gffCnt (readGFF) and tcwCnt (writeLocs)
			readGFF();      // hitMap(hitID, Hit) set Hit has scaffoldN, N, start, end; SC.gffCnt++
			
			findBestSeqsForHit();
			//findBestHitForSeqs();   // reassign best hit where possible
			
			writeLocs();    // for each seq-hit, write scaffold, start, end; Scf.twCnt++
			writeMult();
			logOut.close();
			System.out.println("+++ Done");
		}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*********************************
	 *  Read all possible hits as the TCW may have some that are not in the GFF file
	 */
	static private void readGFFforGenes() {
		System.out.println("+++Read " + gffFile);
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( gffFile ) );
	    		String line;
	    		Pattern namePat = Pattern.compile("Name=([^;]*)");
	    		
	    		while ((line = reader.readLine()) != null) {
	    			String[] tok = line.split("\t");
		    		if (tok == null || tok.length < 9) continue;
		    		if (!tok[2].equals("CDS")) continue;
		    		String desc = tok[8];
		    		
		    		Matcher m = namePat.matcher(desc);
		    		if (!m.find()) continue;
		  
		    		String hitID=m.group(1);
		    		if (!geneSet.contains(hitID)) geneSet.add(hitID);
	    		}
	    		reader.close(); 
	    		System.out.println("    " + geneSet.size() + " genes in GFF file");
	    	}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*********************************************
	 *  Read the DBhitsTable from TCW and creates TC and DC objects
	 *  #Row    SeqID  Length  E-value+        %Sim+   SeqStart+       Align+  HitID
	 */
	static private void readTCWHit() {
		System.out.println("+++Read " + hitFile);
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( hitFile ) );
	    		String line;
	    		int cntDupSeq=0, cntBadGene=0;
	    		
	    		while ((line = reader.readLine()) != null) {
	    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue;  
	    			if (line.startsWith("Row")) continue;
	    			
		    		String[] tok = line.split("\\t");
		    		if (tok == null || tok.length < 8) continue;
		    		
		    		String seqID = tok[1];
		    		int len = Integer.parseInt(tok[2]);
		    		double eval = Double.parseDouble(tok[3]);
		    		int sim = Integer.parseInt(tok[4]);
		    		int start = Integer.parseInt(tok[5]);
		    		int align = Integer.parseInt(tok[6]);
		    		String hitID = tok[7];
		    		if (!geneSet.contains(hitID)) {
		    			cntBadGene++;
		    			continue;
		    		}
		    		
		    		if (seqMap.containsKey(seqID)) {
		    			Seq seq = seqMap.get(seqID);
		    			seq.updateIfBest(hitID, eval, sim, len, start, align);
		    			cntDupSeq++;
		    		}
		    		else {
		    			Seq seq = new Seq(hitID, eval, sim, len, start, align);
		    			seqMap.put(seqID, seq);
		    		}
		    		if (!hitMap.containsKey(hitID)) { // creates list of all hits in DBhitable
		    			Hit hit = new Hit();
		    			hitMap.put(hitID, hit);
		    		}
	    		}
	    		reader.close();
	    		System.out.println("    " +seqMap.size() + " unique seqIDs; " + cntDupSeq + " multiple hits to seqID");
	    		System.out.println("    " +hitMap.size() + " unique hitIDs; " + cntBadGene + " not in GFF file");
	    	}
	    	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	/***************************************************
	 * Diaci psyllid genome assembly version 1.1       scaffold1       NW_007377440.1  KI472552.1      GPS_004273110.1
	 */
	static private void readScaffold() {
		System.out.println("+++Read " + scfFile);
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( scfFile ) );
	    		String line;
	    		Pattern scaf1Pat = Pattern.compile("(scaffold\\d+)");
	    		Pattern name2Pat = Pattern.compile("(NW_\\d+.\\d+)");
	    		String scaffold="";
	    		while ((line = reader.readLine()) != null) {
	    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue; 
	    			
	    			Matcher m = scaf1Pat.matcher(line);
		    		if (!m.find()) {
		    			System.err.println("Scaffold " + line);
		    			System.exit(0);
		    		}
		    		scaffold = m.group(1);
		    		
		    		m = name2Pat.matcher(line);
		    		if (!m.find()) {
		    			System.err.println("nrName " + line);
		    			System.exit(0);
		    		}
		    		String nrName = m.group(1);
		    		scfNameMap.put(nrName, scaffold);
	    		}
	    		reader.close();
	    		System.out.println(scfNameMap.size() + " scaffolds; last one " + scaffold);
	    	}
    	   	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}

	/**********************************************
	 * NW_007377440.1  Gnomon  CDS     70765   70945   .       -       0       
	 * ID=cds1;Parent=rna1;Dbxref=GeneID:103504972,Genbank:XP_008467534.1;
	 * Name=XP_008467534.1;gbkey=CDS;gene=LOC103504972;product=spondin-1-like;
	 * protein_id=XP_008467534.1
	 */
	static private void readGFF() {
		System.out.println("+++Read " + gffFile);
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( gffFile ) );
	    		String line;
	    		int cntFound=0;
	    		Pattern namePat = Pattern.compile("Name=([^;]*)");
	    		
	    		while ((line = reader.readLine()) != null) {
	    			String[] tok = line.split("\t");
		    		if (tok == null || tok.length < 9) continue;
		    		if (!tok[2].equals("CDS")) continue;
		    		
		    		String nrScf = tok[0];
		    		int start = Integer.parseInt(tok[3]);
		    		int end = Integer.parseInt(tok[4]);
		    		String strand = tok[6];
		    		String desc = tok[8];
		    		
		    		Matcher m = namePat.matcher(desc);
		    		if (!m.find()) continue;
		    		
		    		String hitID=m.group(1);
		    		if (!hitMap.containsKey(hitID)) continue; // not in HitTable
		    		
		    		Hit hit = hitMap.get(hitID);
		    		if (hit.scaffold=="") { // use first entry start
		    			String scfName="";
		    			if (scfNameMap.containsKey(nrScf)) scfName=scfNameMap.get(nrScf);
		    			else {
		    				System.err.println("No " + nrScf + " scaffold in Scaffold file");
		    				System.exit(0);
		    			}
			    		hit.scaffold = scfName;
			    		hit.gffScf = nrScf;
			    		hit.scfStart = start;
			    		hit.scfEnd = end;
			    		hit.scfStrand = strand;
			    		cntFound++;
			    		
					int n = getNum(hit.scaffold);	
					hit.scfN = n;
					
			    		if (!scfCntMap.containsKey(n)) {
			    			Scf scf = new Scf();
			    			scfCntMap.put(n, scf);
			    		}
			    		else {
			    			Scf scf = scfCntMap.get(n);
			    			scf.gffCnt++;
			    		}
		    		}
		    		else { // keep updating end until the last CDS
		    			if (strand.equals("-"))  hit.scfStart=start;
		    			else hit.scfEnd = end;
		    		}
	    		}
	    		reader.close(); 
	    		System.out.println("    " + cntFound + " hits found in GFF file");
	    		System.out.println("    " + scfCntMap.size() + " scaffolds");
	    		scfNameMap.clear();
	    	}
	    	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*******************************************************
	 * The best hit for each seq has been determined; assign them to the hit
	 * Also, determine what seqs have their final location and set DONE
	 */
	static private void findBestSeqsForHit() {
		for (Hit hitObj : hitMap.values()) hitObj.bestSeqs.clear();
		int cntEnd=0, cntDone=0, cntOffEnd=0;
		// add each hit for the seqID to either bestMap
		for (String seqID : seqMap.keySet()) {
			Seq seqObj = seqMap.get(seqID);
			if (seqObj.done) continue;
			
			Hit bestHit = hitMap.get(seqObj.hitID);
    			bestHit.addBest(seqID, seqObj);			// add to hit
    			
    			if (seqObj.allHits.size()==1) seqObj.done=true;
    			if (bestHit.bestSeqs.size()==1) seqObj.done=true;
    			
    			seqObj.scfStart = bestHit.scfStart + seqObj.start;
    			int extend = (seqObj.align>seqObj.len) ? seqObj.len : seqObj.align;
    			seqObj.scfEnd = seqObj.scfStart + extend;
    			if (seqObj.scfEnd>bestHit.scfEnd) {
    				if (seqObj.allHits.size()==1) seqObj.bad=true;
    				log(seqID, seqObj, bestHit);
    				
    				cntEnd++;
    				seqObj.done = false;
    			}
    			if (seqObj.done) cntDone++;
		}
		System.err.println("    " +cntDone + " seq has been hit; " + cntEnd + " end extend passed scaffold end");
	}
	static void log(String seqID, Seq seqObj, Hit bestHit) {
		logOut.format("Hit %20s  start %5d  end %5d  diff %5d\n", seqObj.hitID, bestHit.scfStart,
				bestHit.scfEnd, (bestHit.scfEnd-bestHit.scfStart));
		logOut.format("Seq %20s  start %5d  end %5d  diff %5d\n", seqID, seqObj.scfStart,
				seqObj.scfEnd, (seqObj.scfEnd-seqObj.scfStart));
		logOut.format("Ali %20s  start %d align %d   SeqLen %d   Extend %d\n\n", "",
				seqObj.start, seqObj.align, seqObj.len,  (seqObj.scfEnd-bestHit.scfEnd));
	}
	/**************************************************
	 * >DcGS_26072                                      1E-144 _____
    		XP_008485064.1 ( 5837     8202     9560 -)     0E0  99 **9 (9 = #seqs with this best hit)
    		XP_008481051.1 ( 1958     7511     9274 -)   4E-158 53
    		XP_008485065.1 ( 5837    18026    19384 -)     0E0  99 * 0  replace
    		XP_008483142.1 ( 2963     2510      447 -)   1E-72  90
	 */
	private static void findBestHitForSeqs() {
		System.out.println("+++ smooth locs");
		for (String seqID : seqMap.keySet()) {
			Seq seqObj = seqMap.get(seqID);
			
			for (String hitID : seqObj.allHits.keySet()) {
				Hit hitObj = hitMap.get(hitID);
				seqObj.addToMsg(hitID,  hitObj.scfN, hitObj.scfStart, hitObj.scfEnd, hitObj.scfStrand);
			}
		}
		int cntReplace=0;
		// 1. replace only with perfect match 2. replace with near best
		for (String seqID : seqMap.keySet()) {
			Seq seqObj = seqMap.get(seqID); 
			if (seqObj.nextBestHitID!="") continue; // replaced already
			
			Hit bestHitObj = hitMap.get(seqObj.hitID);
			if (bestHitObj.bestSeqs.size()==1) continue; // unique hit
			
			String info = seqObj.allHits.get(seqObj.hitID);
			for (String hitID : seqObj.allHits.keySet()) {
				if (seqObj.hitID.equals(hitID)) continue; // equal
				
				Hit hitObj = hitMap.get(hitID);
				if (hitObj.bestSeqs.size()>0) continue;    // already has multi-map
				
				String info2 = seqObj.allHits.get(hitID);
				if (info.equals(info2)) continue;    // some genes are the same in D.citri
				
				String [] tok = info2.split("\\s+"); 
				double eval = Double.parseDouble(tok[0]);
		
				if (eval==seqObj.eval) {
					seqObj.addBestToMsg(hitID);
					seqObj.replace(info2);
					
					//bestHitObj.bestSeqs.remove(seqObj.best2HitID);
					//hitObj.addBest(seqObj.bestHitID, seqObj.bestEval, seqObj.bestSim);
					
					cntReplace++;
					break;
				}
			}
		}
		System.out.println("    " +"Replace " + cntReplace);
		findBestSeqsForHit();
	}
	/*******************************************************
	 * Output: seqID supercontig_name:start-end(strand)
	 * where supercontig is 'scaffoldn'
	 */
	private static void writeLocs() {
		try {
			System.out.println("+++ Write " + locFile + " and " + rmkFile);
			PrintWriter locOut = new PrintWriter(new FileWriter(locFile));
			PrintWriter rmkOut = new PrintWriter(new FileWriter(rmkFile));
			int cntNoScf=0, cntWrite=0;
			for (String seqID : seqMap.keySet()) {
				Seq seqObj = seqMap.get(seqID);
				Hit hitObj = hitMap.get(seqObj.hitID);
				if (hitObj.scaffold=="") {
					cntNoScf++; // from UniProt probably
					continue;
				}
				cntWrite++;
				locOut.format("%-10s  %s:%d-%d(%s)\n", 
						seqID, hitObj.scaffold, seqObj.scfStart, seqObj.scfEnd, hitObj.scfStrand);
				rmkOut.format("%s Diaci=%s %.1e %d%s\n", 
						seqID, seqObj.hitID, seqObj.eval, seqObj.sim, "%");
				
	    			Scf scf = scfCntMap.get(hitObj.scfN);
	    			scf.tcwCnt++;
			}
			locOut.close(); rmkOut.close();
			System.out.println("    " + cntWrite + " Write; " + cntNoScf + " ignore"); 
			
			System.out.println("+++ Write " + scfCntFile);
			int cnt=1;
			PrintWriter writer = new PrintWriter(new FileWriter(scfCntFile));
			writer.format("%5s %4s %4s %4s\n", "Cnt", "Scaf", "GFF", "TCW");
			for (int key : scfCntMap.keySet()) {
				Scf scf = scfCntMap.get(key);
				writer.format("%4d. %4d %4d %4d\n", cnt++, key, scf.gffCnt, scf.tcwCnt);
			}
			writer.close();
		}
	  	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	private static void writeMult() {
		try {
			// Seq: list their hits
			System.out.println("+++ Write " + multSeqFile);
			PrintWriter writer = new PrintWriter(new FileWriter(multSeqFile));
			int cntOne=0, cntMore=0;
			for (String seqID : seqMap.keySet()) {
				Seq seqObj = seqMap.get(seqID);
				if (seqObj.bad) continue;
				if (seqObj.allHits.size()<=1) cntOne++; else cntMore++;
				if (!logAll && seqObj.done) continue;
				
				writer.format("\n>%s\t\t\t\t\t %.1e\n", seqID, seqObj.bestLimit);
				for (String hitID : seqObj.allHits.keySet()) {
					Hit hitObj = hitMap.get(hitID);
					String best = "    ";
					if (seqObj.hitID.equals(hitID)) {
						best= String.format("#%d (%d %d)", hitObj.bestSeqs.size(), seqObj.scfStart, seqObj.scfEnd);
						if (seqObj.scfEnd>hitObj.scfEnd) best += "**";
					}
					else best = "#" + hitObj.bestSeqs.size();
					writer.format("   %15s (Scaf%6d %8d %8d %s)   %-25s %s\n", 
						hitID, hitObj.scfN, hitObj.scfStart, hitObj.scfEnd, 
						hitObj.scfStrand, seqObj.allHits.get(hitID), best);
				}
			}
			writer.close();
			System.out.println("    " +cntOne + " one hit; " + cntMore + " >1 hit"); 
			
	// same hit assigned to multiple seqs
			System.out.println("+++ Write " + multHitFile);
			cntOne=cntMore=0;
			writer = new PrintWriter(new FileWriter(multHitFile));
			for (String hitID : hitMap.keySet()) {
				Hit hit = hitMap.get(hitID);
				if (hit.bestSeqs.size()<=1) cntOne++; else cntMore++;;
				if (!logAll && hit.bestSeqs.size()<=1) continue;
				
				writer.format(">%15s                               (scf%d %d, %d, %s) %s\n", 
						hitID, hit.scfN, hit.scfStart, hit.scfEnd, hit.scfStrand, hit.gffScf);
				for (String seqID : hit.bestSeqs.keySet()) {
					Seq seq = seqMap.get(seqID);
					String best2="";
					if (seq.nextBestHitID!="") {
						Hit h2 = hitMap.get(seq.nextBestHitID);
						best2 = String.format("%15s %15s scf=%d (%d, %d) %s)", 
								seq.nextBestHitID, seq.allHits.get(seq.nextBestHitID),
								h2.scfN, h2.scfStart, h2.scfEnd, h2.scfStrand);
					}
					writer.format("   %15s %15s #%d   %s\n", 
							seqID, seq.allHits.get(hitID), seq.allHits.size(), best2);
				}
			}
			writer.close();	
			System.out.println("    " +cntOne + " one seq; " + cntMore + " >1 seq"); 
		}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/************************** Utilities *************************/
	
	static private int getNum(String scaf) {
		String x = scaf.replace("scaffold", "");
		int n = Integer.parseInt(x);
		return n;
	}
	
	// used by classes below
	static private String getMsg(double eval, int sim, int len, int start, int align) {
		String x = (new DecimalFormat("0E0")).format((Double)eval);
		if (eval==0.0) x = "0.0";
		return String.format("%s %d L=%d S=%d A=%d", x, sim, len, start, align);
	}
	
	/***************************************************/
	static private class Hit {
		String scaffold="", scfStrand="", gffScf="";
		int scfN=0, scfStart, scfEnd;
		
		public void addBest(String seqID, Seq seq) {
			bestSeqs.put(seqID, seq);
		}
		HashMap <String, Seq> bestSeqs = new HashMap <String, Seq> ();
	}
	/****************************************************/
	static private class Seq {
		public Seq (String hitID, double eval, int sim, int len, int start, int align) {
			updateIfBest(hitID, eval, sim, len, start, align);
		}
		public void updateIfBest( String hitID, double eval, int sim, int len, int start, int align) {
			allHits.put(hitID, getMsg(eval, sim, len, start, align));
			
			boolean replace=false;
			if (eval<this.eval) replace=true;
			else if (eval==this.eval && sim>this.sim) replace=true;
			else if (eval==this.eval && sim==this.sim && this.start==start && this.align==align) {
				Hit hitObj1=hitMap.get(this.hitID);
				Hit hitObj2=hitMap.get(hitID);
				if (hitObj1.scfStrand.equals("-") && hitObj2.scfStrand.equals("+")) replace = true;
			}
			if (replace) {
				this.hitID=hitID;  
				this.eval=eval;
				this.sim=sim;
				this.len=len;
				this.start=start;
				this.align=align;
				this.bestLimit = getBestLimt(eval);
			}
		}
		public void replace(String info) {
			String [] tok = info.split(" ");
			hitID=tok[0];  
			eval= Double.parseDouble(tok[1]);
			sim= Integer.parseInt(tok[2]);
			len=Integer.parseInt(tok[3]);
			start=Integer.parseInt(tok[4]);
			align=Integer.parseInt(tok[5]);
			bestLimit = getBestLimt(eval);
		}
		public void addToMsg(String hitID, int n, int start, int end, String strand) {
			String msg = String.format("%s %4d %8d %8d %s", 
					allHits.get(hitID), n, start, end, strand);
			allHits.put(hitID, msg);
		}
		public void addBestToMsg(String hitID) {
			nextBestHitID = hitID;
			String msg = allHits.get(hitID) + "*";
			allHits.put(hitID, msg);
		}
		private double getBestLimt(double eval) {
			if (eval == 0.0) eval = 1e-180;
			double exp = Math.log(eval) * 0.80;
			return Math.exp(exp);
		}
		boolean done=false, bad=false; // selected the best of allHits
		String hitID, nextBestHitID="";
		double eval=100.0, bestLimit;
		int sim=0, len, start, align;
		int scfStart=0, scfEnd=0;
		HashMap <String, String> allHits = new HashMap <String, String> ();
	}
	/********************************************************/
	static private class Scf {
		int gffCnt=0;
		int tcwCnt=0;
	}
	static HashSet <String> geneSet = new HashSet <String> ();
	static HashMap <String, Hit> hitMap = new  HashMap <String, Hit> ();
	static HashMap <String, Seq> seqMap = new  HashMap <String, Seq> ();
	static TreeMap <Integer, Scf> scfCntMap = new TreeMap<Integer, Scf> ();
	static HashMap <String, String> scfNameMap = new HashMap<String, String> ();
}
