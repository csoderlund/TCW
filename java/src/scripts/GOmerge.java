package scripts;
/*****************************************
* See set methods for details of input and output (uses TCW directory /ResultExport):
* 
* Format one:
* Basic GO annotation export: GO-term Domain Description #Seqs 
* 
* Format two:
* Main Table export GO: GO      Level    Term_type       #Seqs  Description
* 
* The output will be: Domain		Description		N1		N2...	Nn	
* Read with Excel and use Chart, Column (Clustered columns works well)
* Chart Layout: Axis titles
 */
/*********************************************
 * TreeMap <String, Count> bioMap
 * 	        descrip, Count
 * 				int [] count = new int [nSuf]
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.util.TreeMap;

import util.methods.Out;

public class GOmerge {
	private String desc=""; 				// Description at top of file
	private boolean bIsFormat1=true;
	private int nTCW;					// number of  input files
	private String [] prefix; 			// STCWid, prefix on file names, order output
	private boolean bIsCnt=false; 		// read integer; divide by nGenes; set nGenes and cutoff
	private int [] nGenes=null; 			// '#Seqs with GOs' from overview
	private double dCutoff=0.01;			// output if ratio is > cutoff
	private int iCutoff;
	private String dirName = "./ResultExport";	// directory of input/output
	private String tcwFile;				// prefix + this is input file name
	private String outputFile;			// output file name
	private String suffix = ".tsv";
	
	private PrintWriter outF;
	private TreeMap <String, Count> biolMap = new TreeMap <String, Count>();
	private TreeMap <String, Count> cellMap = new TreeMap <String, Count>();
	private TreeMap <String, Count> moleMap = new TreeMap <String, Count>();
	private int cntOut=0, cntFound;
	
	public static void main(String[] args) {
		new GOmerge().build();
	}
	private void build() {
		try {
			setLevel2one();
			prt(desc);
			findFiles();
			
			for (int i=0; i<nTCW; i++) parseFile(i);
			prt("Biol " + biolMap.size());
			prt("Cell " + cellMap.size());
			prt("Mole " + moleMap.size());
			output();
			prt("Finish " + outputFile);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	private void findFiles() {
		try {
			File dir = new File(dirName);
			if (!dir.exists()) die(dirName + " does not exist");
			if (!dir.isDirectory()) die(dirName + " is not a directory");
			String [] fileN = dir.list();
			int cnt=0;
			for (String f : fileN) {
				if (f.endsWith(tcwFile)) cnt++;
			}
			if (cnt<nTCW) die("cannot find all files");
		}
		catch (Exception e) {e.printStackTrace(); die("File files");}
	}
	private void parseFile(int index) {
		try {
			String line;
			String path = dirName +  prefix[index] + tcwFile;
			prt("Read " + path);
			BufferedReader reader = new BufferedReader ( new FileReader(path));
			
			// read leading comment and column headers
			line = reader.readLine();
			if (!bIsFormat1) line = reader.readLine();
			String [] tok = line.split("\t");
			if (bIsFormat1 && !tok[1].equals("Domain")) die(tok.length + " " + line);
			if (!bIsFormat1 && !tok[1].equals("Level")) die(tok.length + " " + line);
			
			int nGO=0;
			while ((line = reader.readLine()) != null) {
	    			String domain="", desc="";
	    			double score=0.0;
	    			int count=0;
	    			
	    			tok = line.split("\t");
	    			if (bIsFormat1) {
	    				domain = tok[1];
	    				desc = tok[2];	
	    			}
	    			else {
	    				if (tok[2].equals("biological_process")) domain="biol";
	    				else if (tok[2].equals("molecular_function")) domain="mole";
	    				else if (tok[2].equals("cellular_component")) domain="cell";
	    				else die("Wrong format: " + line);
	    				desc = tok[4];
	    			}
	    			if (bIsCnt && nGenes==null) {
	    				count = Integer.parseInt(tok[3].trim());
	    			}
	    			else if (bIsCnt) {
	    				count = Integer.parseInt(tok[3].trim());
	    				score = ((double)count/(double)nGenes[index]);
	    			}
	    			else {
	    				score = Double.parseDouble(tok[3].trim());
	    			}
	    			
	    			if (domain.equals("biol")) 		addScore(biolMap, desc, score, count, index);
	    			else if (domain.equals("cell")) 	addScore(cellMap, desc, score, count, index);
	    			else if (domain.equals("mole")) 	addScore(moleMap, desc, score, count, index);
	    			nGO++;
			}
			reader.close();
			System.out.println(prefix[index] + " Total: " + nGO);
		}
		catch (Exception e) {e.printStackTrace(); die("reading " + prefix[index]);}
	}
	private void addScore(TreeMap <String, Count> map, String desc, double score, int count, int index) {
		if (map.containsKey(desc)) {
			map.get(desc).score[index]=score;
			map.get(desc).count[index]=count;
		}
		else {
			Count c = new Count();
			for (int i=0; i<nTCW; i++) c.score[i] = 0.0;
			c.score[index]=score;
			c.count[index]=count;
			map.put(desc, c);
		}
	}
	
	private void output() {
		try {
			outF = new PrintWriter(new FileWriter(outputFile));
			String o= "Domain\tDescription";
			for (String s : prefix) o += "\t" + s;
			outF.println(o);
			outSet(biolMap, "biological_process");
			outSet(cellMap, "cellular_component");
			outSet(moleMap, "molecular_function");
			outF.close();
			prt("Output " + cntOut);
			prt("Found " + cntFound);
		}
		catch (Exception e) {e.printStackTrace();}
	}
	private void outSet(TreeMap <String, Count> map, String title) {
		String o;
		int cnt=0;
		for (String desc : map.keySet()) {
			if (cnt==0) o = title + "\t" + desc;
			else o = "\t"+ desc;
			Count c = map.get(desc);
			boolean prt=true;
			for (int i=0; i<nTCW; i++) {
				if (bIsCnt && nGenes==null) {
					if (c.count[i]>iCutoff) o += String.format("\t%d", c.count[i]);
					else prt=false;
				}
				else if (bIsCnt) {
					if (c.score[i]<dCutoff) prt=false;
					o += String.format("\t%.3f",c.score[i]);
				}
				else {
					int ex = Out.getExponent(c.score[i]);
					o += String.format("\t%d",ex);
				}
			}
			cntFound++;
			if (prt) {
				outF.println(o);
				cntOut++;
			}
			cnt++;
		}
	}
	private void die(String msg) {
		System.out.println(msg);
		System.exit(-1);
	}
	private  void prt(String msg) {
		System.out.println(msg);
	}
	
	private class Count {
		int [] count = new int [nTCW];
		double [] score = new double [nTCW];
	}
	/*************************************************
	 * Experimental figure for comparing GO DE 
	 */
	private void setDE() {
		desc="Top N RhRo DE where N<40";
		nTCW=2;
		prefix = new String[nTCW];
		prefix[0]="PaR"; prefix[1]="ShR";
		
		bIsCnt=false; // do not need nGenes and cutoff
		
		dirName += "/GOnDE/";
		String pair = "RhRo";
		tcwFile = "_" + pair + "GOtable" + suffix;
		outputFile = dirName + "/GOn" + pair + suffix;
	}
	/******************************************
	 * Output from Main Table using Export GO
	 * No good: gives same of results as Basic GO Level 2
	 */
	private void setMain() {
		desc="RhRo 1E-05 (D) Level 2";
		bIsFormat1=false;
		
		nTCW=2;
		prefix = new String[nTCW];
		prefix[0]="PaR"; prefix[1]="ShR";
		
		bIsCnt=true; // output count, no nGenes
		//nGenes =  new int [nTCW]; 
		//nGenes[0]=21790; nGenes[1]= 19493; 
		iCutoff=2;
		
		dirName += "/GOnMain/";
		String pair = "RhRoD";
		tcwFile = "_" + pair + "_GOseqs_2_1e-05_perSeq" + suffix;
		outputFile = dirName + "/GOn" + pair + suffix;
	}
	/********************************************
	 * Figure in supplement of percent sequences in level 2 GOs categories
	 */
	private void setLevel2() {
		desc="Level 2, no other filters";
		nTCW=4;
		prefix = new String[nTCW];
		prefix[0]="PaR"; prefix[1]="ShR"; prefix[2]="Sb"; prefix[3]="Dc";
		
		bIsCnt=true; // divide by nGenes; set nGenes and cutoff
		nGenes =  new int [nTCW]; 
		nGenes[0]=21790; nGenes[1]= 19493; nGenes[2]= 37561; nGenes[3]= 21448; // Seqs with GOs
		dCutoff=0.01;
		
		dirName += "/GOnSeqs/";
		tcwFile = "_GOtable" + suffix;
		outputFile = dirName + "GOnSeqsLevel2" + suffix;
	}
	private void setLevel2one() {
		desc="Level 2, no other filters - for one file only";
		nTCW=1;
		prefix = new String[nTCW];
		prefix[0]="Sb"; 
		
		bIsCnt=true; // divide by nGenes; set nGenes and cutoff
		nGenes =  new int [nTCW]; 
		nGenes[0]= 37561;
		dCutoff=0.01;
		
		dirName += "/GOnSeqs/";
		tcwFile = "_GOtable" + suffix;
		outputFile = dirName + "GO1SeqsLevel2" + suffix;
	}
}
