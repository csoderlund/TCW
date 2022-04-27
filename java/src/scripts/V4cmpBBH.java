package scripts;

/************************************************
 * V4 Compare Galaxy blast_RBH output to mTCW BBH output
 * It provides a listing of the differences, and makes a guess at some of the algorithm differences.
 * Note: The differences are usually from: A best B best C bidirectional D
 * 
 * 1. Two FASTA files were created: NNU.fa and OlR.fa that were part of Closure clusters
 * 
 * 2. Galaxy directory
 * remove last line of blast_rbh.py so that the tmp directory is not removed
 * ./blast_rbh.py  --nr -a prot -t blastp -o galaxy.tsv NNU.fa OlR.fa
 * copy galaxy.tsv to paper/BBH
 * the output tells where the tmp directory is.
 * from tmp directory, copy A_vs_B.tabular and B_vs_A.tabular to paper/BBH
 * 
 * 3. TCW Put fasta files in in projects/bbh_nnu and projects/bbh_olr
 * Create sTCW_bbn_nnu and sTCW_bbh_olr
 * Create mTCW_bbn from them. 
 * 		For the search: use blast with no parameters
 * 		For computing BBH: set Coverage 50 and Similarity 70 (same as galaxy)
 * 		viewMultiTCW: 
 * 			View Pairs from different datasets. Set Columns. SORT on Bit-score. Export Table.
 * 	
 * 4. Run this script. 
 * 		
 * PairTable.tsv - all pairs read from blast file, plus column indicating if BBH
 *   #SeqID1  SeqID2  AAeval %AAcov1 %AAcov2 %AAsim  AAbit BBH
 * 
 * Galaxy.tsv results
 *   #A_id    B_id    A_length  B_length A_qcov  B_qcov  align    sim    bitscore
 * A_vs_B.tabular 
 *   #Aid1  id2 bitscore sim qcov qlen align  (qcov=A_qcov and qlen=B_qcov)
 * B_vs_A.tabular 
 *   #Bid1  id2 bitscore sim qcov qlen align  (qcov=B_qcov and qlen=A_qcov)
 */
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;
import java.util.Arrays;

public class V4cmpBBH {
	String dir =     "paper/BBH/";
	String idir = 	 dir + "input/";
	
	String inTCW =   idir+"PairTable.tsv";   // TCW all blast pairs with BBH column
	
	String inGx =    idir+"galaxy.tsv";    // galaxy output
	String inGx1 =   idir+"A_vs_B.tabular";  // Search output from Galaxy
	String inGx2 =   idir+"B_vs_A.tabular";
	
	String outFile = dir+"diffv4.txt";
	PrintWriter outFH;
	double simCutoff=70, covCutoff=50, noVal=-2.0;
	
	int TCW=0, GX=1;
	
	String state="!G";
	
	public static void main(String[] args) {
		new V4cmpBBH();
	}
	private V4cmpBBH ()  {
		try {
			outFH = new PrintWriter(new FileOutputStream(outFile, false)); 
		
			readTCW(inTCW); // PairTable.tsv all pairs from blast and BBH
			
			readGxHits(0, inGx1); 	// add to pairMap; A_vs_B.tabular
			readGxHits(1, inGx2); 	// add to pairMap; B_vs_A.tabular
			readGx(inGx); // update pairMap and seqMap with BBH for Galaxy; galaxy.tsv
			
			writeFinal();
			outFH.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	/********************************************************************/
	private void writeFinal() {
		try { 
			prtBoth("\nFinal results:");
			
			int cnt=0;
			for (String key : pairMap.keySet()) {
				Pair p = pairMap.get(key);
				if (p.isTCW && p.isGX) cnt++;
			}
			prtFile("------------- TCW && !Gx ------------------ ");
			prtBoth("");
			prtCnt(cnt, "Total shared in TCW and Gx");
			
			prtFile(String.format("xx %10s %10s  %6s   %6s %6s %6s", 
					"Name1", "Name2",  "Bit", "Sim", "Cov1", "Cov2"));
		
			cnt=0;
			for (String key : pairMap.keySet()) {
				Pair pObj = pairMap.get(key);
				if (pObj.isTCW && !pObj.isGX) {
					writeDiffForPair(key);
					cnt++;
				}
			}
			prtCnt(cnt, "Total in TCW and not Gx");
			writeSummary();
			outFH.println();
			
			state = "!T";
			prtFile("\n\n------------- !TCW && Gx ------------------ ");
			prtBoth("");
			prtFile(String.format("xx %10s %10s  %6s   %6s %6s %6s", 
					"Name1", "Name2",  "Bit", "Sim", "Cov1", "Cov2"));
			
			cnt=0;
			for (String key : pairMap.keySet()) {
				Pair p = pairMap.get(key);
				if (!p.isTCW && p.isGX) {
					
					writeDiffForPair(key);
					cnt++;
				}
			}
			prtCnt(cnt, "Total in Gx not in TCW");
			writeSummary();
			outFH.println();	
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	
	// This checking is not exhaustive
	private int errMult=0, errSim=0, errCov=0,  errTie=0, errBla=0, errUnk=0;
	private void writeSummary() {
		if (errMult>0) prtCnt(errMult,"Probably A->B->C<->D");
		if (errSim>0) prtCnt(errSim, "Sim (rounds up)");
		if (errCov>0) prtCnt(errCov, "Cov (rounds up)");
		if (errTie>0) prtCnt(errTie, "Tie (same evalue and bitscore)");
		if (errBla>0) prtCnt(errBla, "Bla (Not in both blast files)");
		if (errUnk>0) prtCnt(errUnk, "Unk");
		errMult= errSim= errCov= errTie= errUnk= 0;
	}
	/*****************************************************************
	 * Tests could be better, but allowed me to look at results in viewMultiTCW
	 */
	private void writeDiffForPair(String key) {
		String [] n = key.split(":");
		Pair pObj = pairMap.get(key);
		
		Seq s1obj = seqMap.get(n[0]);
		Seq s2obj = seqMap.get(n[1]);
		
		if (pObj.tsim!=-2 && pObj.gsim==-2) {
			errBla++;
			prtSeqs("*in TCW not in Gx blast", s1obj, s2obj);
			return;
		}
		if (pObj.tsim==-2 && pObj.gsim!=-2) {
			errBla++;
			prtSeqs("*in Gx not in TCW blast", s1obj, s2obj);
			return;
		}
		
		if ((pObj.tsim<simCutoff && (pObj.tsim+0.5)>=simCutoff)) {
			errSim++;
			prtPair(String.format("*Sim rounds to %4.2f", simCutoff), pObj);
			return;
		}
		if ((pObj.tcov1<covCutoff && (pObj.tcov1+0.5)>=covCutoff) ||
			(pObj.tcov2<covCutoff && (pObj.tcov2+0.5)>=covCutoff)) {
			errCov++;
			prtPair(String.format("*Cov round to  %4.2f", covCutoff), pObj);
		}
	
		// TCW tie
		for (Seq seq : Arrays.asList(s1obj, s2obj)) {
			for (Pair px : seq.tPairSet) {
				if (pObj==px) continue;
				if (pObj.teval==px.teval && pObj.tbit==px.tbit) {
					errTie++;
					String msg;
					if (state.contentEquals("!T")) msg = (seq.tBBH=="") ? "No BBH" : seq.tBBH;
					else msg = (seq.gBBH=="") ? "No BBH" : seq.gBBH;
					prtList("*Tie (" + msg + ")", seq.getTseq());
					return;
				}
			}
		}
		if (state.contentEquals("!T")) {
			String msg="";
			if (s1obj.tBBH=="" && s2obj.tBBH=="") msg = "*No TCW BBH for either seq of pair";
			else if (s1obj.tBBH!="" || s2obj.tBBH!="") msg = "*No TCW BBH for one seq of pair";
			if (msg!="") {
				errMult++;
				prtSeqs(msg, s1obj, s2obj);
				return;
			}
		}
		else {
			String msg="";
			if (s1obj.gBBH=="" && s2obj.gBBH=="") msg = "*No GX BBH for either seq of pair";
			else if (s1obj.gBBH!="" || s2obj.gBBH!="") msg = "*No GX BBH for one seq of pair";
			if (msg!="") {
				errMult++;
				prtSeqs(msg, s1obj, s2obj);
				return;
			}
		}
		errUnk++;
		prtSeqs("*Unknown", s1obj, s2obj);
	}
	
	private void prtSeqs(String msg, Seq s1obj, Seq s2obj) {
		prtFile(state + " ********Seq \n" + msg);
		if (s1obj.tBBH!="") prtFile(s1obj.tBBH);
		if (s2obj.tBBH!="") prtFile(s2obj.tBBH);
		if (s1obj.gBBH!="") prtFile(s1obj.gBBH);
		if (s2obj.gBBH!="") prtFile(s2obj.gBBH);
		prtFile("");
	}
	private void prtPair(String msg, Pair pObj) {
		prtFile(state + " ******** Pair\n" + msg);
		prtFile(pObj.tinfoStr());
		prtFile(pObj.ginfoStr());
		prtFile("");
	}
	private void prtList(String msg, String list) {
		prtFile(state + " ******** List\n" + msg);
		prtFile(list);
		prtFile("");
	}
	
	/*********************************************************************/
	
	/*********************************************************
	 * Read PairsTable.tsv of all pairs
	 *  #SeqID1  SeqID2  AAeval %AAcov1 %AAcov2 %AAsim  AAbit BBH
	 */
	private void readTCW(String file) {
		try {
			prtBoth("\nRead " + file);
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String key;
			Pair pObj;
			
			String line = in.readLine();
			prtBoth("   Columns: " + line);
			
			int cntBBH=0;
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s");
				if (tok.length<2) continue;
				
				key = tok[0] + ":" + tok[1];
				
				if (!pairMap.containsKey(key)) {
					pObj = new Pair();
					pairMap.put(key, pObj);
				
					pObj.key = key;
				}
				else {
					pObj = pairMap.get(key); 
					prtBoth("Duplicate " + key ); // should not happen
				}
				
				pObj.teval = Double.parseDouble(tok[2]);
				pObj.tsim  = Double.parseDouble(tok[3]);
				pObj.tcov1 = Double.parseDouble(tok[4]);
				pObj.tcov2 = Double.parseDouble(tok[5]);
				pObj.tbit  = Double.parseDouble(tok[6]);
				String bbh = tok[7];
				if (!bbh.contentEquals("-")) {
					pObj.isTCW=true;
					cntBBH++;
				}
				for (String name : Arrays.asList(tok[0], tok[1])) {
					Seq sObj;
					if (seqMap.containsKey(name)) sObj = seqMap.get(name);
					else {
						sObj = new Seq(name);
						seqMap.put(name, sObj);
					}
					sObj.tadd(pObj);
				}
			}
			in.close();
			prtCnt(seqMap.size(), " Seqs");
			prtCnt(pairMap.size(), " Pairs");
			prtCnt(cntBBH, " BBH pairs");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	/***************************************************
	 * Galaxy Hit files
	 * qseqid sseqid bitscore sim qcov qlen align
	***************************************************/
	private void readGxHits(int idx, String file) {
		try {
			prtBoth("\nRead " + file);
			int cnt=0, cntNew=0, cnt2nd=0;
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String name="", mate="", line;
			
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s+");
				if (tok.length<5 ) continue;
				if (tok[0].contains(";")) {
					prtBoth(line);
					continue;
				}

				if (name.equals(tok[0]) && mate.equals(tok[1])) continue;
				name = tok[0]; 
				mate = tok[1];
				
				String key;
				if (name.compareTo(mate)>0) key = mate + ":" + name;
				else  						key = name + ":" + mate;
				
				Pair pObj;
				if (!pairMap.containsKey(key)) {
					pObj = new Pair();
					pairMap.put(key, pObj);
				
					pObj.key = key;
					cntNew++;
				}
				else pObj = pairMap.get(key);
				
				if (pObj.gbit > noVal) { // already added for GX
					double bit = Double.parseDouble(tok[2]);
					if (bit>pObj.gbit) {
						pObj.gbit = bit;
						pObj.gsim = Double.parseDouble(tok[3]);
					}
					pObj.gcov2 = Double.parseDouble(tok[4]);
					cnt2nd++;
					continue;
				}

				pObj.gbit =  Double.parseDouble(tok[2]);
				pObj.gsim =  Double.parseDouble(tok[3]);
				pObj.gcov1 = Double.parseDouble(tok[4]);
				
				for (String n : Arrays.asList(tok[0], tok[1])) {
					Seq sObj;
					if (seqMap.containsKey(n)) sObj = seqMap.get(n);
					else {
						sObj = new Seq(n);
						seqMap.put(n, sObj);
					}
					sObj.gadd(pObj);
				}
				cnt++;
			}
			in.close();
			
			prtCnt(seqMap.size(), " Seqs");
			prtCnt(pairMap.size(), " Pairs");
			prtCnt(cnt, " unique pairs");
			prtCnt(cntNew, " new pairs");
			if (cnt2nd>0) prtCnt(cnt2nd, " 2nd pairs");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	/***************************************************
	 * Galaxy BBH tsv file
	 *   #A_id    B_id    A_length  B_length A_qcov  B_qcov  align    sim    bitscore
	***************************************************/
	private void readGx(String file) { 
		try {
			prtBoth("\n Read " + file);
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String line = in.readLine();
			prtBoth("   Columns: " + line);
			int cnt=0;
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s");
				if (tok.length<2) continue;
				
				String key;
				if (tok[0].compareTo(tok[1])>0) key = tok[1] + ":" + tok[0];
				else                            key = tok[0] + ":" + tok[1];
				
				if (!pairMap.containsKey(key)) {
					prtBoth(key + " not in blast tab file");
					continue;
				}
				Pair pObj = pairMap.get(key);
				pObj.isGX = true;
				pObj.gcov1 = Double.parseDouble(tok[4]); // from one of the hit files
				pObj.gcov2 = Double.parseDouble(tok[5]);
				pObj.gsim =  Double.parseDouble(tok[6]);
				pObj.gbit =  Double.parseDouble(tok[7]);
				
				for (String name : Arrays.asList(tok[0], tok[1])) {
					Seq sObj= seqMap.get(name);
					sObj.gBBH = pObj.ginfoStr();
				}
				cnt++;
			}
			in.close();
			prtCnt(cnt, "BBH pairs ");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	private void prtCnt(int num, String msg) {
		String x = String.format("%5d %s", num, msg);
		System.out.println(x);
		outFH.println(x);
	}
	private void prtBoth(String msg) {
		System.out.println(msg);
		outFH.println(msg);
	}
	private void prtFile(String msg) {
		outFH.println(msg);
	}
	private class Seq {
		String name, tBBH="", gBBH="";
	
		Vector <Pair> tPairSet = new Vector <Pair> ();
		Vector <Pair> gPairSet = new Vector <Pair> ();
		
		public Seq (String name) {this.name=name;}
		
		void tadd(Pair pObj) {
			if (tPairSet.contains(pObj) || tPairSet.size()>2) return;
			
			if (pObj.isTCW) tBBH=pObj.tinfoStr();
			tPairSet.add(pObj);
		}
		void gadd(Pair pObj) {
			if (gPairSet.contains(pObj) || gPairSet.size()>2) return;
			
			if (pObj.isGX) gBBH=pObj.ginfoStr();
			gPairSet.add(pObj);
		}
		String getTseq() {
			String list="";
			for (int i=0; i<tPairSet.size(); i++) {
				Pair pObj = tPairSet.get(i);
				if (list!="") list += "\n";
				list += pObj.tinfoStr();
			}
			return list;
		}
 	}
	private class Pair {
		String key;
		
		// Index 0:1 for scores for X:Y and Y:X
		// TCW has all in one file
		double teval =  noVal, tbit = noVal, tsim = noVal;
		double tcov1 =  noVal, tcov2 = noVal;
		boolean isTCW=false;
		
		// Gx has in A_vs_B and B_vs_A
		double gbit = noVal, gsim = noVal, gcov1 =  noVal, gcov2 = noVal;
		boolean isGX=false;
		
		String tinfoStr() {
			String bbh = (isTCW) ? "BBH" : "   ";
			String e = String.format("%7.2E", teval); 
			return String.format("tc %20s  %6.1f   %6.1f %6.2f %6.2f  %s %9s ", 
						              key,  tbit,   tsim, tcov1, tcov2, bbh, e);
		}
		String ginfoStr() { 
			String bbh = (isGX) ? "BBH" : "   ";
			return String.format("gx %20s  %6.1f   %6.1f %6.2f %6.2f  %s", 
						             key,  gbit,   gsim, gcov1, gcov2, bbh);
		}
	}
	private HashMap <String, Seq> seqMap = new HashMap <String, Seq>  ();
	private HashMap <String, Pair> pairMap = new HashMap <String, Pair>  ();
}
