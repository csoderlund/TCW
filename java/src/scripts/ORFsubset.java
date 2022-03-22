package scripts;

/*************************************************
 * sTCW_pl_Osj and sTCW_pl_NnR annotated with UniProt Dec2021
 * 
 *  Set1
 *  viewSingleTCW:
 *  	Basic Hit Filter: Rank=1, SP-plants, 1E-30, %Sim>50, %Hit>50 (make sure have SP-plant hits)
 *  	Create Seq Table and output columns
 *  This Filter:  Not Multi; not ORF=Hit+; Not TR-plants; Sim>=100 HitCov>=100
 *  
 *  Set2 (Osj) & Set3 (NnR)
 *  viewSingleTCW:
 *  	Seq Filter: BestBits, TR-plants, %Sim>=100, %Hit>=100%  (no check for SP-plants)
 *  	Create Seq Table and output columns
 *  This Filter: Not Multi; not ORF=Hit+; 
 *  
 *  Seq Table Columns:
 #		Seq-ID TCW-Remarks     #SwissProt  BS-Hit-ID   BS-DB-type   BS-Taxo BS-%Sim BS-%HitCov
 */
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

public class ORFsubset {

public static void main(String[] args) {new ORFsubset();}
	int SET=3; 
	
	String dir = 	  "paper/ORF/Subset/set";
	String fastaFile = "SeqTableSeqs.fa";
	String tabFile =   "SeqTableColumns.tsv";
	
	String outFaFile, outRmkFile;
	int maxSeqs=5000,  maxNoSP=-1;
	String sp0Rmk = "NoSP";
	double simCutoff=100.0, hitCutoff=100.0;
	
	HashMap <String, String>  seqHitMap =  new HashMap <String, String> (); // initial set with removal except unique
	HashMap <String, String>  seqRmkMap =  new HashMap <String, String> (); // non-unique based on hit & pairs
	
	HashSet <String> uniqueSet = new HashSet <String> ();
	HashSet <String> finalSet = new HashSet <String> ();
	
	private void init() {
		dir += SET + "/";
		outFaFile = dir + "set" + SET + ".fa";
		outRmkFile = dir + "names" + SET + ".txt";
		
		if (SET!=1) maxNoSP=2000; 
			
		prt(dir + ": " + maxSeqs + " seqs; " + maxNoSP + " sp=0");
	}
	public ORFsubset() {
		try {	
			init();
			readColumns();			// seqHitMap, seqRmkMap, hitCntMap
			createUniqueHit();		// uniqueSeq - keep only one for each hit
			finalSet();				// finalSet  - keep only those that pass max parameters
			writeFasta();
		}
		catch (Exception e) {e.printStackTrace(); System.exit(0);}
	}
	/*****************************************************/
	private void finalSet() {
		try {
			int cntSp0=0;
			
			for (String seqid : seqRmkMap.keySet()) { // set2 only
				
				if (!uniqueSet.contains(seqid)) continue;
				
				String remark = seqRmkMap.get(seqid);
				
				if (remark.contains(sp0Rmk)) cntSp0++;
				else continue;
				
				finalSet.add(seqid);
				if (cntSp0>=maxNoSP) break;	
			}
			
			for (String seqid : seqRmkMap.keySet()) { 
				if (uniqueSet.contains(seqid)) {
					finalSet.add(seqid);
					if (finalSet.size()>=maxSeqs) break;
				}
			}
			prt("Finals:");
			prt(finalSet.size(), "Final Set ");
			prt(cntSp0, "SP=0");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(0);}
	}
	/********************************************************
	 * All have ORf
	 *******************************************************/
	private void readColumns() {
		try {
			String line, name=null;
			int nSP;
			String  remark, hit, type, taxo;
			double sim, hitcov;
			int cntRead=0, cntNoTR=0, cntNoSim=0, cntNoHitCov=0, cntNoPlus=0, cntIsMulti=0, cntNoSP=0;
					
			BufferedReader in = new BufferedReader ( new FileReader (dir + "/" + tabFile)); 
			line = in.readLine();
			prt(line);
			
			// #Seq-ID TCW-Remarks     #SwissProt      BS-Hit-ID       BS-DB-type      BS-Taxo BS-%Sim BS-%HitCov
			while ((line = in.readLine()) != null) {
				cntRead++;
				String [] tok =  line.split("\\t");
				int i=0;
				name = tok[i++];
				remark = tok[i++];
				nSP = Integer.parseInt(tok[i++]);
				hit = tok[i++];		
				type = tok[i++];
				taxo = tok[i++];
				sim = Double.parseDouble(tok[i++]);
				hitcov = Double.parseDouble(tok[i++]);
	
				if (!remark.contains("ORF=Hit+")) 	 {cntNoPlus++; continue;}
				if (remark.startsWith("Multi")) 	 {cntIsMulti++; continue;}
				
				// should always pass for set2 and set3
				if (!type.contentEquals("tr") || !taxo.contentEquals("plants")) {cntNoTR++; continue;}
				if (sim<simCutoff)    			 {cntNoSim++; continue;}
				if (hitcov<hitCutoff) 			 {cntNoHitCov++; continue;}
				
				if (SET!=1) { 
					if (nSP==0) 			{cntNoSP++; 	 remark += "; " + sp0Rmk;} 
				}
				seqHitMap.put(name, hit); 	
				seqRmkMap.put(name, remark);	
			}
			in.close();
			prt("Totals:");
			prt(seqHitMap.size(), tabFile);
			prt(cntRead, 	  "Read");
			prt(cntNoSP, 	  "is SP=0");
			prt(cntNoTR, 	  "not TR");
			prt(cntNoSim, 	  "not Sim");
			prt(cntNoHitCov,  "not HitCov");
			prt(cntIsMulti,   "Multi");
			prt(cntNoPlus,   "not ORF=Hit+");
			prt("");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(0);}
	}
	
	/*******************************************************
	 * This is done after removing similar pairs, so there are not many dup hits
	 */
	private void createUniqueHit() {
		try {
			HashSet <String> hitSet = new HashSet <String> ();
			for (String seqid : seqHitMap.keySet()) {
				String hit = seqHitMap.get(seqid);
				if (!hitSet.contains(hit)) {
					uniqueSet.add(seqid);
					hitSet.add(hit);
				}
			}
			prt(uniqueSet.size(), "remove Duplicate hit seqs ");	
		}
		catch (Exception e) {e.printStackTrace(); System.exit(0);}
	}
	private void writeFasta() {
		try {
			int cntLg=0, cntMk=0;
			String line, seq="", name=null;
			boolean bPrt=false;
			
			PrintWriter fhFa = new PrintWriter(new FileOutputStream(outFaFile, false));
			PrintWriter fhRm = new PrintWriter(new FileOutputStream(outRmkFile, false));
			BufferedReader inFa = new BufferedReader ( new FileReader (dir + "/" + fastaFile)); 
			
			while ((line = inFa.readLine()) != null) {
				line = line.trim();
				if (line.startsWith(">")) {
					if (bPrt) {
						String remark = seqRmkMap.get(name);
						if (remark.contains("!Lg")) cntLg++;
						if (remark.contains("!Mk")) cntMk++;
						fhRm.println(name + " " + remark);
						fhFa.println(">" + name + "\n" + seq);
						seq="";
					}
					line = line.substring(1);
					name = line.split(" ")[0];
					bPrt = finalSet.contains(name);
				}
				else if (bPrt) seq += line;
			}
			
			if (bPrt) {
				String remark = seqRmkMap.get(name);
				if (remark.contains("!Lg")) cntLg++;
				if (remark.contains("!Mk")) cntMk++;
				fhRm.println(name);
				fhFa.println(">" + name + "\n" + seq);
				seq="";
			}
			inFa.close(); fhFa.close(); fhRm.close();
			
			prt(cntLg, "!Lg");
			prt(cntMk, "!Mk");
			prt(SET + " Done " + finalSet.size());
		}
		catch (Exception e) {e.printStackTrace(); System.exit(0);}
	}
	private void prt(String msg) {System.out.println(msg);}
	private void prt(int x, String msg) {if (x>0) System.out.format("%8d %s\n", x, msg);}
}
