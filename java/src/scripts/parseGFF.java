package scripts;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***********************************************
 * Input: 	
 * 1. GFF file with Genbank (GB) identifiers
 * 2. DBHitTable exported from Basic annoDB: species D. citri, nr, rank <=25
 * 	  TW sequence can only have one location, but multiple sequences can have the same location
 * 3. Scaffold names -- mapping from the genome site of 'scaffoldxxx' to name used in GFF
 *  
 * Output: Location file
 * 
 * http://www.ncbi.nlm.nih.gov/genome/annotation_euk/Diaphorina_citri/100/
 * This annotation should be referred to as NCBI Diaphorina citri Annotation Release 100
 */
public class parseGFF {
	// input
	static private String gffFile="blast/Dc_top_level.gff3";
	static private String hitFile="blast/DBhitTable25.csv";
	static private String scafFile="blast/scaffold_names";
	// output
	static private String locFile="blast/Locations.txt";
	static private String scafCntFile="blast/Scaffold_counts.txt";
	static private String multFile="blast/multiMap.txt";
	static private PrintWriter multiWriter;
	
	public static void main(String[] args) {
		try {
			multiWriter = new PrintWriter(new FileWriter(multFile));
			readHit();
			readScaffold();
			readGFF();
			findBest();
			writeLocs();
			System.out.println("+++ Done");
			multiWriter.close();
		}
		catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*************************************************************
	 * Multiple twName can have the same gbName (transcripts of the same gene?)
	 * but twName can only have on gbName.
	 * For each twName, go through hits and find the one with the best evalue.
	 */
	private static void findBest() {
		try {
			System.out.println("+++ Find best mapping");
			
		}
	  	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*******************************************************
	 * name supercontig_name:start-end(strand)
	 */
	private static void writeLocs() {
		try {
			System.out.println("+++ Write " + locFile);
			PrintWriter writer = new PrintWriter(new FileWriter(locFile));
			scafCntMap.clear();
			for (String twName : twMap.keySet()) {
				TW tw = twMap.get(twName);
				GB gb = gbMap.get(tw.bestGB);
				writer.format("%-10s  %s:%d-%d(%s)\n", twName, gb.scaffold, gb.start, gb.end, gb.strand);
				if (!scafCntMap.containsKey(gb.scaffold)) scafCntMap.put(gb.scaffold,1);
	    			else scafCntMap.put(gb.scaffold, scafCntMap.get(gb.scaffold)+1);
			}
			writer.close();
			
			System.out.println("+++ Write " + scafCntFile);
			int cnt=1;
			writer = new PrintWriter(new FileWriter(scafCntFile));
			for (String key : scafCntMap.keySet())
				writer.format("%4d. %15s %4d\n", cnt++, key, scafCntMap.get(key));
			writer.close();
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
		File path = new File(gffFile);
 	    if ( !(path.exists()) ) {
 	    		System.err.println(gffFile + " does not exist");
 	    		System.exit(-1);
 	    }
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( gffFile ) );
	    		String line;
	    		int cntRead=0, cntProcess=0, cntNoGB=0, cntGB=0;
	    		Pattern namePat = Pattern.compile("Genbank:(XP_.*);Name");
	    		
	    		while ((line = reader.readLine()) != null) {
	    			cntRead++;
	    			String[] tok = line.split("\t");
		    		if (tok == null || tok.length < 9) continue;
		    		if (!tok[2].equals("CDS")) continue;
		    		
		    		Matcher m = namePat.matcher(tok[8]);
		    		if (!m.find()) continue;
		    		cntProcess++;  // have XP_
		  
		    		String gbName=m.group(1);
		    		if (!gbMap.containsKey(gbName)) {
		    			cntNoGB++;
		    			continue;
		    		}
		    		GB gb = gbMap.get(gbName);
		    		if (gb.scaffold=="") {
		    			String sc="";
		    			if (scafNameMap.containsKey(tok[0])) sc=scafNameMap.get(tok[0]);
		    			else {
		    				System.err.println("No " + tok[0] + " scaffold");
		    				System.exit(0);
		    			}
			    		gb.scaffold = sc;
			    		gb.start = Integer.parseInt(tok[3]);
			    		gb.end = Integer.parseInt(tok[4]);
			    		gb.strand = tok[6];
			    		cntGB++;
			    		if (!scafCntMap.containsKey(gb.scaffold)) scafCntMap.put(gb.scaffold,1);
			    		else scafCntMap.put(gb.scaffold, scafCntMap.get(gb.scaffold)+1);
		    		}
	    		}
	    		reader.close();
	    		System.out.println(cntRead + " reads");
	    		System.out.println(cntProcess + " processed");
	    		System.out.println(cntNoGB + " no GB");
	    		System.out.println(cntGB + " GB");
	    		System.out.println(scafCntMap.size() + " scaffolds");
	    		scafNameMap.clear();
	    	}
	    	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/*********************************************
	 * 1,DcAN_07268,NP_001284621.1,6.0E-120,Diaphorinacitri,1
	 * 2,DcAN_00699,XP_008472795.1,8.0E-55,Diaphorina citri,2
	 */
	static private void readHit() {
		System.out.println("+++Read " + hitFile);
		File path = new File(hitFile);
 	    if ( !(path.exists()) ) {
 	    		System.err.println(hitFile + " does not exist");
 	    		System.exit(-1);
 	    }
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( hitFile ) );
	    		String line;
	    		int cntRead=0, cntProcess=0, cntDupGB=0, cntDupTW=0;
	    		
	    		while ((line = reader.readLine()) != null) {
	    			cntRead++;
	    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue;  			
		    		String[] tok = line.split(",");
		    		if (tok == null || tok.length < 6) continue;
		    		if (!tok[2].startsWith("XP_")) continue;
		    		if (!tok[4].contains("Diaphorina citri")) continue;
		    		cntProcess++;
		    		
		    		String twName = tok[1];
		    		String gbName = tok[2];
		    		double eval = Double.parseDouble(tok[3]);
		    		int rank = Integer.parseInt(tok[5]);
		    		
		    		TW tw=null;
		    		if (twMap.containsKey(twName)) {
		    			tw = twMap.get(twName);
		    			tw.add(gbName, eval, rank);
		    			cntDupTW++;
		    		}
		    		else {
		    			tw = new TW(twName, gbName, eval, rank);
		    			twMap.put(twName, tw);
		    		}
		    		if (gbMap.containsKey(gbName)) {
		    			GB gb = gbMap.get(gbName);
		    			gb.add(tw);
		    			cntDupGB++;
		    		}
		    		else {
		    			GB gb = new GB(gbName, tw);
		    			gbMap.put(gbName, gb);
		    		}
	    		}
	    		reader.close();
	    		System.out.println(cntRead + " reads");
	    		System.out.println(cntProcess + " processed");
	    		System.out.println(cntDupGB + " duplicate GB identifiers");
	    		System.out.println(cntDupTW + " duplicate TW identifiers");
	    		System.out.println(gbMap.size() + " unique GB identifiers");
	    		System.out.println(twMap.size() + " unique TW identifiers");
	    	}
	    	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	/***************************************************
	 * Diaci psyllid genome assembly version 1.1       scaffold1       NW_007377440.1  KI472552.1      GPS_004273110.1
	 */
	static private void readScaffold() {
		System.out.println("+++Read " + scafFile);
		File path = new File(scafFile);
 	    if ( !(path.exists()) ) {
 	    		System.err.println(scafFile + " does not exist");
 	    		System.exit(-1);
 	    }
	    	try {
	    		BufferedReader reader = new BufferedReader ( new FileReader ( scafFile ) );
	    		String line;
	    		int cntRead=0, cntProcess=0;
	    		Pattern name1Pat = Pattern.compile("(scaffold\\d+)");
	    		Pattern name2Pat = Pattern.compile("(NW_\\d+.\\d+)");
	    		
	    		while ((line = reader.readLine()) != null) {
	    			cntRead++;
	    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue; 
	    			
	    			Matcher m = name1Pat.matcher(line);
		    		if (!m.find()) {
		    			System.err.println(line);
		    			System.exit(0);
		    		}
		    		String scaffold = m.group(1);
		    		
		    		m = name2Pat.matcher(line);
		    		if (!m.find()) {
		    			System.err.println(line);
		    			System.exit(0);
		    		}
		    		String gbScaff = m.group(1);
		    		scafNameMap.put(gbScaff, scaffold);
		    		cntProcess++;
	    		}
	    		reader.close();
	    		System.out.println(cntRead + " reads");
	    		System.out.println(cntProcess + " processed");
	    	}
    	   	catch ( Throwable err ) {err.printStackTrace();System.exit(-1);}
	}
	
	static private class GB {
		public GB (String n, TW t) {
			name = n;
			map.add(t);
		}
		public void add(TW t) {
			map.add(t);
		}
		String name="", scaffold="", strand="";
		int start, end;
		ArrayList <TW> best = new ArrayList <TW> ();
		ArrayList <TW> map = new ArrayList <TW> ();
	}
	static private class TW {
		public TW (String n, String g, double d, int r) {
			name = n;
			gb.add(g);
			rank.add(r);
			eval.add(d);
			bestGB=g;  // may be changed if duplicates
			bestEval=d;
		}
		public void add( String g, double d, int r) {
			gb.add(g);
			rank.add(r);
			eval.add(d);
			if (d<bestEval) {
				multiWriter.print(name + " " + bestGB + " " + bestEval + " " + g + " " + d + "\n");
				bestGB=g;
				bestEval=d;
			}
		}
		String name;
		String bestGB;
		double bestEval;
		ArrayList <String> gb = new ArrayList <String> ();
		ArrayList <Integer> rank = new ArrayList <Integer> ();
		ArrayList <Double> eval = new ArrayList <Double> ();
	}
	static HashMap <String, GB> gbMap = new  HashMap <String, GB> ();
	static HashMap <String, TW> twMap = new  HashMap <String, TW> ();
	static TreeMap <String, Integer> scafCntMap = new TreeMap<String, Integer> ();
	static HashMap <String, String> scafNameMap = new HashMap<String, String> ();
}
