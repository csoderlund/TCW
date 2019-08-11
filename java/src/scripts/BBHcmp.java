package scripts;

/************************************************
 * Compare Galaxy blast_RBH output to mTCW BBH output
 * It provides a listing of the differences, and makes a guess at some of the 
 * algorithm differences.
 */
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;

public class BBHcmp {
	String dir =     "paper/BBH/main/";
	String inEx1 =   dir+"PairTable.tsv"; // viewMultiTCW: show BBH pairs; export name1, name2
	String inEx2 =   dir+"galaxy.tsv";    // galaxy output
	
	String inTab =   dir+"hitsAA.tab";    // Search output from mTCW
	String inLen =   dir+"SeqTableLen.tsv"; // viewMultiTCW: show sequences; Export name and length
	String inGx1 =   dir+"A_vs_B.tabular";// comment out last line of blast_rbh.py and copy from tmp directory
	String inGx2 =   dir+"B_vs_A.tabular";
	String outFile = dir+"diff.tsv";
	PrintWriter outFH;
	double simCutoff=70, covCutoff=50;
	
	public static void main(String[] args) {
		new BBHcmp();
	}
	private BBHcmp ()  {
		try {
			outFH = new PrintWriter(new FileOutputStream(outFile, false)); 
		
			readSeqLen(inLen); // create seqMap with lengths
			readMapHits(inTab);// create pairMap of all possible pairs
			
			readGxHits(0, inGx1); // add to pairMap
			readGxHits(1, inGx2);
			prtBlast();
			
			readMap(0, inEx1); // update pairMap and seqMap with BBH for TCW
			readMap(1, inEx2); // update pairMap and seqMap with BBH for Galaxy
			
			writeCmp();
			outFH.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void writeCmp() {
		try { 
			prt("\n");
			int cnt=0;
			for (String key : pairMap.keySet()) {
				Pair p = pairMap.get(key);
				if (p.hasBBH[0] && p.hasBBH[1]) cnt++;
			}
			prt(cnt, "Total in TCW and Gx");
			
			cnt=0;
			for (String key : pairMap.keySet()) {
				Pair p = pairMap.get(key);
				if (p.hasBBH[0] && !p.hasBBH[1]) {
					out("1------------------------------------------ ");
					prtDiffAll(1, key);
					cnt++;
				}
			}
			prt("\n");
			prt(cnt, "Total in TCW and not Gx");
			prtDiffSum();
			outFH.println();
			
			cnt=0;
			for (String key : pairMap.keySet()) {
				Pair p = pairMap.get(key);
				if (!p.hasBBH[0] && p.hasBBH[1]) {
					out("2----------------------------------------- ");
					prtDiffAll(2, key);
					cnt++;
				}
			}
			prt("\n");
			prt(cnt, "Total in Gx not in TCW");
			prtDiffSum();
			outFH.println();	
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	// This checking is not exhaustive
	private int errMult=0, errSim=0, errCov=0,  errTie=0, errBla=0, errUnk=0;
	private void prtDiffSum() {
		prt(errMult,"Mul (X in BBH1 for TCW and BBH2 for Gx");
		prt(errSim, "Sim (rounds up)");
		prt(errCov, "Cov (rounds up)");
		prt(errTie, "Tie (same evalue and bitscore)");
		prt(errBla, "Bla (Not in both blast files)");
		prt(errUnk, "Unk");
		errMult= errSim= errCov= errTie= errUnk= 0;
	}
	private void prtDiffAll(int order, String key) {
		String [] n = key.split(":");
		Pair p = pairMap.get(key);
		
		out(p.infoStr(0));
		out(p.infoStr(1));
		Seq s0 = seqMap.get(n[0]);
		Seq s1 = seqMap.get(n[1]);
		
		if (p.sim[0]!=-2 && p.gsim[0]==-2) {
			errBla++;
			out("*in TCW not in Gx blast");
			out(s0.list);
			out(s1.list);
			return;
		}
		if (p.sim[0]==-2 && p.gsim[0]!=-2) {
			errBla++;
			out("*in Gx not in TCW blast");
			out(s0.list);
			out(s1.list);
			return;
		}
		if (s0.bbhSet.size()>1 || s1.bbhSet.size()>1) {
			int idx = (s0.bbhSet.size()>1) ? 0 : 1;
			Seq s = (s0.bbhSet.size()>1) ? s0 : s1;
			out("*" + n[idx] + " in two BBH pairs");
			for (Pair px : s.bbhSet) 
				if (p!=px) {
					out(px.infoStr(idx));
					
					out(s0.list);
					out(s1.list);
					out(s0.glist);
					out(s1.glist);
					
					errMult++; 
					return;
				}
		}
		if ((p.sim[0]<simCutoff && (p.sim[0]+0.5)>=simCutoff)) {
			errSim++;
			out(String.format("*Sim rounds to %4.2f", simCutoff));
			return;
		}
		if ((p.cov1[0]<covCutoff && (p.cov1[0]+0.5)>=covCutoff) ||
			(p.cov2[0]<covCutoff && (p.cov2[0]+0.5)>=covCutoff)) {
			errCov++;
			out(String.format("*Cov round to  %4.2f", covCutoff));
			return;
		}
	
		// tie
		for (Pair px : s0.pairSet) {
			if (p==px) continue;
			if (p.eval[0]==px.eval[0] && p.bit[0]==px.bit[0]) {
				errTie++;
				out("*Tie");
				out(s0.list);
				return;
			}
		}
		// 
		errUnk++;
		out("*Unknown " + p.key);

		out(s0.list);
		out(s1.list);
		out(s0.glist);
		out(s1.glist);
	}
	
	private void readMap(int idx, String file) {
		try {
			prt("\n" + idx + " Read " + file);
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String line = in.readLine();
			prt("   Columns: " + line);
			int cnt=0;
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s");
				if (tok.length<2) continue;
				
				String key;
				if (tok[0].compareTo(tok[1])>0) key = tok[1] + ":" + tok[0];
				else                            key = tok[0] + ":" + tok[1];
				
				if (!pairMap.containsKey(key)) {
					prt(key + " not in blast tab file");
					continue;
				}
				Pair p = pairMap.get(key);
				p.hasBBH[idx]=true;
					
				Seq s0 = seqMap.get(tok[0]);
				Seq s1 = seqMap.get(tok[1]);
				
				s0.bbhSet.add(p);
				s1.bbhSet.add(p);
				
				cnt++;
			}
			prt(cnt, " BBH pairs ");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	private void readSeqLen(String file) {
		try {
			prt("\nRead " + file);
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String line = in.readLine();
			prt("    Columns:" + line);
			
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s+");
				int len = Integer.parseInt(tok[1]);
				Seq s = new Seq();
				s.len = len;
				seqMap.put(tok[0], s);
			}
			prt(seqMap.size(), " sequences");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	
	// 0               1      sim     algn    mm      d       s1      e1      s2      e3       10
	// 0               1      2       3       4       5       6       7       8       9       10
	//x|x_001     y|y_038     96.330  218     8       0       1       218     1       218     6.62e-156       426
	//read first 3 for each
	private void readMapHits(String file) {
		try {
			prt("\nRead " + file);
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String line, name="", mate="", key;
			Pair p;
			
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\t");
				if (tok.length<2) continue;
				if (tok.length!=12) { 
					for (int i=0; i<tok.length; i++) prt(i + " " + tok[i]);
					System.exit(0);
				}
				String p0 = tok[0].split("\\|")[0];
				String p1 = tok[1].split("\\|")[0];
				if (p0.equals(p1)) continue;					// same db
				
				String n0 = tok[0].split("\\|")[1];
				String n1 = tok[1].split("\\|")[1];
				
				if (name.equals(n0) && name.equals(n1)) continue; // self hit
				
				if (name.equals(n0) && mate.equals(n1)) continue; // multiple HSPs
				mate = n1;
				
				if (!name.equals(n0)) name=n0;
				
				int idx;
				if (n0.compareTo(n1)>0) key = n1 + ":" + n0;
				else                    key = n0 + ":" + n1;
				
				if (pairMap.containsKey(key)) {
					p = pairMap.get(key);
					idx = 1;
				}
				else {
					p = new Pair();
					pairMap.put(key, p);
					
					idx=0;
					p.key = key;
					p.name[0]=n0;
					p.name[1]=n1;
				}
				p.sim[idx]= Double.parseDouble(tok[2]);
				
				double align = Double.parseDouble(tok[3]);
				p.cov1[idx] = ((double)align/seqMap.get(n0).len)*100.0;
				p.cov2[idx] = ((double)align/seqMap.get(n1).len)*100.0;
				
				p.eval[idx]=Double.parseDouble(tok[10]);
				p.bit[idx]= Double.parseDouble(tok[11]);
				
				Seq s0=seqMap.get(n0);
				s0.add(idx, p);
			}
			prt(pairMap.size(), " unique pairs");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	//qseqid sseqid bitscore pident qcovhsp qlen length
	private void readGxHits(int idx, String file) {
		try {
			prt("\n" + idx + " Read " + file);
			int cntNotFound=0, cntFound=0, cntHsp=0;
			BufferedReader in = new BufferedReader ( new FileReader (file));
			String line = in.readLine();
			String name="", mate="";
			
			while ((line = in.readLine()) != null) {
				String [] tok = line.split("\\s+");
				
				if (name.equals(tok[0]) && mate.equals(tok[1])) {cntHsp++; continue;}
				name = tok[0]; mate=tok[1];
				
				String key;
				if (tok[0].compareTo(tok[1])>0) key = tok[1] + ":" + tok[0];
				else                            key = tok[0] + ":" + tok[1];
				
				Pair p;
				if (!pairMap.containsKey(key)) {
					if (cntNotFound<5)
						out(key + " not found in blast tab file " + tok[2] + " " + tok[3]);
					cntNotFound++;
					
					p = new Pair();
					pairMap.put(key, p);
				
					p.key = key;
					p.name[0]=tok[0];
					p.name[1]=tok[1];
				}
				else p = pairMap.get(key);
				
				p.gbit[idx] = Double.parseDouble(tok[2]);
				p.gsim[idx] = Double.parseDouble(tok[3]);
				p.gcov[idx] = Double.parseDouble(tok[4]);
				
				Seq s0=seqMap.get(tok[0]);
				s0.gadd(idx, p);
				cntFound++;
			}
			prt(cntFound, " unique pairs");
			prt(cntNotFound, " new pairs");
			prt(cntHsp, " HSPs");
		}
		catch (Exception e) {e.printStackTrace(); System.exit(1);}
	}
	private void prtBlast() {
		prt("\nCompare TCW and Gx blast results");
		int cntNoTCW=0, cntOneTCW=0, cntNoGx=0, cntOneGx=0;
		int cntBothGood=0, cntBothTCW=0, cntBothGx=0;
		for (String key : pairMap.keySet()) {
			Pair p = pairMap.get(key);
			if (p.sim[0]!= -2.0  && p.sim[1] == -2.0) cntOneTCW++;
			if (p.gsim[0]!= -2.0 && p.gsim[1] == -2.0) cntOneGx++;
			
			if (p.sim[0]!= -2.0  && p.sim[1] != -2.0) cntBothTCW++;
			if (p.gsim[0]!= -2.0 && p.gsim[1] != -2.0) cntBothGx++;
			
			if (p.sim[0]!= -2.0 && p.gsim[0] == -2.0) cntNoGx++;
			if (p.sim[0]== -2.0 && p.gsim[0] != -2.0) cntNoTCW++;
			if (p.sim[0]!= -2.0 && p.gsim[0] != -2.0) cntBothGood++;
		}
		
		prt(cntBothTCW," Both TCW");
		prt(cntOneTCW," One TCW");
		
		prt(cntBothGx," Both Gx");
		prt(cntOneGx," One Gx");
		
		prt(cntBothGood," Both good");
		prt(cntNoTCW," NO TCW");
		prt(cntNoGx," NO Gx");
	}
	private void prt(int num, String msg) {
		String x = String.format("%5d %s", num, msg);
		System.out.println(x);
		outFH.println(x);
	}
	private void prt(String msg) {
		System.out.println(msg);
		outFH.println(msg);
	}
	private void out(String msg) {
		outFH.println(msg);
	}
	private class Seq {
		int len;
		String list="", glist="";
	
		Vector <Pair> bbhSet  = new Vector <Pair> ();
		Vector <Pair> pairSet = new Vector <Pair> ();
		
		void add(int i, Pair p) {
			if (pairSet.size()<3) {
				pairSet.add(p);
				list += p.infoStr(i) + "\n";
			}
		}
		int gcnt=0;
		void gadd(int i, Pair p) {
			if (gcnt<3) {
				gcnt++;
				glist += p.ginfoStr(i) + "\n";
			}
		}
 	}
	private class Pair {
		String key;
		// names and alias for X and Y
		String [] name =  new String [2];
		// scores for X:Y and Y:X
		double [] eval =  new double [2];
		double [] sim =   {-2.0, -2.0};
		double [] bit =   new double [2];
		double [] cov1 =  new double [2];
		double [] cov2 =  new double [2];
		boolean [] hasBBH = {false, false};
		double [] gbit = {-2.0, -2.0};   // galaxy bit-score
		double [] gsim = {-2.0, -2.0};  // galaxy similarity
		double [] gcov = {-2.0, -2.0};  // galaxy similarity
		
		String infoStr(int i) {
			int j = (i==0) ? 1: 0;
			String e = String.format("%7.2E", eval[i]); 
			return String.format("%s %s %9s %6.1f   %6.1f %6.2f %6.2f ", 
						name[i], name[j], e,  bit[i], sim[i], cov1[i], cov2[i]);
		}
		String ginfoStr(int i) {
			int j = (i==0) ? 1: 0; 
			return String.format("gx %s %s  %6.1f   %6.1f %6.2f ", 
						name[i], name[j],  gbit[i], gsim[i], gcov[i]);
		}
	}
	private HashMap <String, Seq> seqMap = new HashMap <String, Seq>  ();
	private HashMap <String, Pair> pairMap = new HashMap <String, Pair>  ();
}
