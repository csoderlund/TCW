package cmp.compile;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;

import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.database.Globals;

public class MethodClique {
	 private boolean withPivot=false, debug=false;
	 
	 /****************************************************************
	  * Everything is copied from MethodClosure 
	  * and then modified to replace the BestHit class with Clique
	  * and additional data structures are added to the Seq class.
	  */
	 private String groupFile = Globals.Compile.GROUP_FILE_CLOSURE;
	 private String [] covTypes = {"Either", "Both"};
	 private String [] strType = {"Amino acid", "Nucleotide"};
	 
	 public boolean run(int idx, DBConn db, CompilePanel panel, boolean withPivot) {
		 
		Out.PrtDateMsg("\nStart execution of " + Globals.Methods.Closure.TYPE_NAME);
		this.withPivot = withPivot;
		
		if (withPivot) Out.PrtSpMsg(0, "Bron_Kerbosch with pivot");
	    else Out.PrtSpMsg(0, "Bron_Kerbosch without pivot");
		 
		if (!setParams(idx, db, panel)) return false;
		
		long startTime = Out.getTime();
		Out.PrtSpMsg(1, "Start processing...");
		
		loadFromDB();
		if (bSuccess) new Clique().computeGroups();
		if (bSuccess) writeGroups();
		if (bSuccess) 
			if (! new MethodLoad(cmpDBC).run(idx, groupFile, cmpPanel)) // loads the file just written
				bSuccess=false;
						
		Out.PrtDateMsgTime("Finish execution of " + Globals.Methods.Closure.TYPE_NAME, startTime);
		return bSuccess; 
	}
	
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		MethodPanel theMethod = panel.getMethodPanel();
		String comment = theMethod.getCommentAt(idx);
		String [] settings = theMethod.getSettingsAt(idx).split(":");

		prefix = theMethod.getMethodPrefixAt(idx);						// Groups should be prefixed with this
		covCutoff = Static.getInteger(settings[1].trim());
		if (covCutoff<0) covCutoff=0;
		covMode = Static.getInteger(settings[2].trim());
		if (covMode>covTypes.length) {
			Out.PrtError("Coverage length types " + covMode + " must be <" + covTypes.length);
			return false;
		}
		simCutoff = Static.getInteger(settings[3].trim());
		if (simCutoff<0) simCutoff=0;
		int x = Static.getInteger(settings[4].trim());
		type = x;
		if (x!=0 && x!=1) {
			Out.PrtError("Bad type " + x + " must be 0 or 1");
			return false;
		}
		cmpDBC = db;
		cmpPanel = panel;
		
		String root = cmpPanel.getCurProjMethodDir() + "/" + groupFile + 
				"." + prefix + "_" + covCutoff + "-" + simCutoff;
		groupFile = root;
		
		Out.PrtSpMsg(1, "Prefix:     " + prefix);
		Out.PrtSpMsg(1, "Coverage:   " + covCutoff + " (" + covTypes[covMode] + ")");
		Out.PrtSpMsg(1, "Similarity: " + simCutoff);
		Out.PrtSpMsg(1, "Remark:     " + comment);
		Out.PrtSpMsg(1, "");
		
		if (type == 1 && panel.getNumNTdb()<=0) {
			Out.PrtWarn("Must have at least one nucleotide singleTCWs to use nucleotide blast - abort Transitive");
			return false;
		}
		return true;
	}
	
	/**********************************************************
	 * Read sequence lengths from database and enter into seqLenMap
	 */
	private void loadFromDB() {
	try {
		Out.PrtSpMsg(2, "Load " + strType[type] + " sequences from database");
				
		String msq = "SELECT UTstr, asmID FROM unitrans";
		ResultSet rs = cmpDBC.executeQuery(msq);
		while (rs.next()) {
			Seq seq = new Seq(rs.getInt(2));
			seqMap.put(rs.getString(1), seq);
		}
		rs.close();		
		Out.PrtCntMsg( seqMap.size(), " Sequences loaded from database");
		
		int cntPass=0, cntMinSim=0, cntMinOlap=0, cntPair=0;
		Out.PrtSpMsg(2, "Load " + strType[type] + " blast pairs from database");
		if (type==0) msq = "select UTstr1, UTstr2, aaEval, aaSim, aaOlap1, aaOlap2, aaBit, aaBest from pairwise where aaSim>0";
		else         msq = "select UTstr1, UTstr2, ntEval, ntSim, ntOlap1, ntOlap2, ntBit, aaBest from pairwise where ntSim>0";
		rs = cmpDBC.executeQuery(msq);
		while (rs.next()) {
			cntPair++;
			String seq1=rs.getString(1);
			String seq2=rs.getString(2);
			double eval = rs.getDouble(3);
			int sim = rs.getInt(4);
			int olap1 = rs.getInt(5);
			int olap2 = rs.getInt(6);
			int bit   = rs.getInt(7);
			int aaBest = (type==0) ? rs.getInt(8) : 0;
			
			if (sim < simCutoff) {
				cntMinSim++;
				continue;
			}
			else {
				if (covTypes[covMode].startsWith("Either")) {// ok if one is greater
					if (olap1 < covCutoff && olap2 < covCutoff) {
						cntMinOlap++;		
						continue;
					}
				}
				else if (covTypes[covMode].startsWith("Both")) { // both must be greater
					if (olap1 < covCutoff || olap2 < covCutoff) {
						cntMinOlap++;		
						continue;
					}
				}
			}
			cntPass++;
			seqMap.get(seq1).add(seq2);
			seqMap.get(seq2).add(seq1);
			
			Pair pair = new Pair(seq1, seq2, eval, sim, olap1, olap2, bit, aaBest);
			pairList.add(pair);
		}
		rs.close();		
		Out.PrtCntMsg(cntPair, "Loaded pairs");
		Out.PrtCntMsg(cntMinSim, "Failed similarity ");
		Out.PrtCntMsg(cntMinOlap, "Failed coverage");
		Out.PrtCntMsg(cntPass, "Passed Pairs");
		
		rs = cmpDBC.executeQuery("select asmID, prefix from assembly");
		while (rs.next()) asmMap.put(rs.getInt(1), rs.getString(2));
		rs.close();
	}
	catch (Exception e) {ErrorReport.die(e, "readLengths"); bSuccess=false;}
	}
	
	/***********************************************************
	 * Write grpSeqs into a file with one group per line.
	 * The blast file has the taxo with each name, e.g. Pa|PaRi_0001
	 * So it does not need writing here. 
	 */
	private void writeGroups() {
	try {
		Out.PrtSpMsg(2, "Write clusters to file " + groupFile);			
		PrintWriter outFH = new PrintWriter(new FileOutputStream(groupFile)); 
		int group=1;
		for (int idx : grpMap.keySet()) {
			Vector <String> seqs = grpMap.get(idx).seqNames;
		
			String grp = group+"";
			//String grp = idx+"";
			for (String seq : seqs) {
				if (seq!=null && !seq.equals("")) {
					Seq seqObj = seqMap.get(seq);
					grp += " " + asmMap.get(seqObj.asmID) + "|" + seq;
				}
			}
			outFH.println(grp);
			group++;
		}
		outFH.close();
		Out.PrtSpMsg(3, "Wrote " + (group-1) + " clusters");
	}
	catch (Exception e) {ErrorReport.prtReport(e, "write clusters to file"); bSuccess=false;}
	}
	
	private class Clique {
		private HashMap <Integer, Grp>  initGrpMap = new HashMap <Integer, Grp> (); 
		
		private ArrayList <Seq>         graph =      new ArrayList <Seq> (); // per initGrpMap(i)
		private ArrayList <TmpGrp>      tmpList = new ArrayList <TmpGrp> (); // groups for initGrpMap(i)
		private int grpCount=1, cliqueCount=0;
		
		// seqMap with neighbors, and pairMap are populated
		private void computeGroups() {
			createTransitiveGroups();  				// initGrpMap 
			if (bSuccess) Bron_KerboschExecute();	// tmpGrpList
		}
		private void createTransitiveGroups() {
			try {
				Out.PrtSpMsg(2, "Compute clusters");
				
				int grpMax=1, cnt=0, cntNoJoin=0;
				
				for (Pair pair : pairList) 
				{
					int grp1 = seqMap.get(pair.seq1).grpID;
					int grp2 = seqMap.get(pair.seq2).grpID;
					if (grp1 != 0 && grp1 == grp2) 	continue;      // both in same group
					
					if (grp1!=0 && grp2!=0) { // merge
						int mergeID  = (grp1 < grp2) ? grp1 : grp2;
						int deleteID = (grp1 > grp2) ? grp1 : grp2;
						Vector <String> mergeSeqs = initGrpMap.get(deleteID).seqNames;
						initGrpMap.get(mergeID).addSeqs(mergeSeqs);
						initGrpMap.remove(deleteID);
					}
					else if (grp1==0 && grp2==0) {	
						Grp grp = new Grp(grpMax);  // new
						grp.add(pair.seq1);
						grp.add(pair.seq2);
						initGrpMap.put(grpMax, grp);
						grpMax++;
					}
					else if (grp2==0) {
						initGrpMap.get(grp1).add(pair.seq2);
					}
					else if (grp1==0) {
						initGrpMap.get(grp2).add(pair.seq1);
					}
					else cntNoJoin++;
					cnt++;
					if (cnt%10==0) Out.r("processed pairs " + cnt);
				}
				System.err.print("                                                             \r");
				
				Out.PrtSpCntMsg(2, initGrpMap.size(), "Total transitive clusters");
				Out.PrtSpCntMsgZero(2, cntNoJoin, "Good pairs not joined in cluster");
				if (initGrpMap.size()==0) bSuccess=false;
			}
			catch (Exception e) {
				writeGroups();
				ErrorReport.prtReport(e, "compute groups");
				bSuccess=false;
			}
		}
		/****************************************************************
		 * initGrpMap has transitive closure distinct graphs
		 * for each initGrpMap(i), 1 or more cliques are created in tmpList
		 * tmpList is pruned to remove sequences from more than one clique
		 * the results are added to grpMap 
		 */
		void Bron_KerboschExecute() { 
			int cntProcessed=0;
			for (Grp g : initGrpMap.values()) {
				bSuccess=true;
				tmpList.clear();
				createGraph(g);							// create graph
				
				 ArrayList<Seq> X = new ArrayList<Seq>(); 
			     ArrayList<Seq> R = new ArrayList<Seq>(); 
			     ArrayList<Seq> P = new ArrayList<Seq>(graph); 
			     
			     Bron_Kerbosch(R, P, X);    				// create tmpList
			     pruneClique();			// add to grpMap
			     cntProcessed++;
			     if (cntProcessed % 100 == 0) Out.r("Processed " + cntProcessed);
			}
			Out.PrtSpCntMsg(2, cliqueCount, "Total closure clusters with overlap");
			Out.PrtSpCntMsg(2, grpMap.size(), "Total closure clusters without overlap");
			if (grpMap.size()==0) bSuccess=false;
		} 
		void createGraph(Grp g) {
	        try { 
	        		graph.clear();
	               
	            for (String name  : g.seqNames) {
	            		Seq vertU = seqMap.get(name);
	            		vertU.setName(name);
	            		graph.add(vertU);
	            		
	            		for (String seqName : vertU.seqPairs) {
	                		Seq vertV = seqMap.get(seqName); 
	                		vertU.add(vertV); 
	                }	
	            }
	            Collections.sort(graph);
	            int x=0;
	            for (Seq v : graph) v.setX(x++);
	         
	        } catch (Exception e) { e.printStackTrace(); } 
	    } 

	     /* Bron_Kerbosch with or without pivot */
	    void Bron_Kerbosch(ArrayList<Seq> R, ArrayList<Seq> P, ArrayList<Seq> X) 
	    { 
	        if ((P.size() == 0) && (X.size() == 0)) {
	            createTmpGroup(R); 
	            return; 
	        } 
	        ArrayList<Seq> P1 = new ArrayList<Seq>(P); 
	
	        if (withPivot) {
	        		Seq u = getMaxDegreeVertex(union(P, X)); 
		        P = removeNbrs(P, u); // P = P / Nbrs(u) 
	        }
	        for (Seq v : P) { 
	            R.add(v); 
	            Bron_Kerbosch(R, intersect(P1, getNbrs(v)), intersect(X, getNbrs(v))); 
	            R.remove(v); 
	            P1.remove(v); 
	            X.add(v); 
	        } 
	    } 
	    /***********************************
	     * Finalize: A sequence can only be in one cluster. This prunes sequences from the smaller clusters.
	     */
	    void pruneClique() {
	    		int next=0;
	    		
	    		while (next >= 0) {
	    			Collections.sort(tmpList);
	    			next = -1;
	    			for (int i=0; i<tmpList.size(); i++) {
	    				if (tmpList.get(i).nSeqs <= 1) 
	    					break;
	    				else if (tmpList.get(i).id==0) {
	    					next = i;
	    					break;
	    				}
	    			}
	    			if (next==-1) break;
	    			
	    			TmpGrp bGrp = tmpList.get(next);
	    			for (int i=next+1; i<tmpList.size(); i++) {
	    				TmpGrp iGrp = tmpList.get(i);
	    			    if (iGrp.nSeqs<=1) continue;
	    			    
	    				for (int idx : bGrp.seqInGrp.keySet()) {
	    					if (iGrp.seqInGrp.containsKey(idx)) iGrp.remove(idx);
	    				}
	    				
	    				iGrp.nSeqs = iGrp.seqInGrp.size();
	    			}
	    			
	    			bGrp.id = grpCount++;
	    			createGroup(bGrp);
	    			tmpList.remove(next);
	    		}
	    }
	    
	    /******************************************************************/
	   
	    void createTmpGroup(ArrayList<Seq> R) {
	    		if (debug) {
		    		System.out.print("  ---- Maximal Clique : "); 
		        for (Seq v : R) System.out.print(" " + (v.name)); 
		        System.out.println(); 
	    		}
	    		cliqueCount++;
		    	TmpGrp grp = new TmpGrp();
	    	 	for (Seq v : R) grp.add(v.x, v.name); 
	    	 	grp.nSeqs = R.size();
	    	 	tmpList.add(grp);
	    }
	    void createGroup(TmpGrp bGrp) {
	    		Grp grp = new Grp(grpCount);
	    		for (String n : bGrp.seqInGrp.values()) grp.add(n);
	    		grpMap.put(grpCount, grp);
	    }
	    /**************************************************************************/
	    Seq getMaxDegreeVertex(ArrayList<Seq> g) { 
	        Collections.sort(g); 
	        return g.get(g.size() - 1);
	    } 
	    // Union of two sets 
	    ArrayList<Seq> union(ArrayList<Seq> arlFirst, ArrayList<Seq> arlSecond) { 
	        ArrayList<Seq> arlHold = new ArrayList<Seq>(arlFirst); 
	        arlHold.addAll(arlSecond); 
	        return arlHold; 
	    } 
	    // Intersect
	    ArrayList<Seq> intersect(ArrayList<Seq> arlFirst, ArrayList<Seq> arlSecond) { 
	        ArrayList<Seq> arlHold = new ArrayList<Seq>(arlFirst); 
	        arlHold.retainAll(arlSecond); 
	        return arlHold; 
	    } 
	    // Removes the neigbours 
	    ArrayList<Seq> removeNbrs(ArrayList<Seq> arlFirst, Seq v) { 
	        ArrayList<Seq> arlHold = new ArrayList<Seq>(arlFirst); 
	        arlHold.removeAll(v.getNbrs()); 
	        return arlHold; 
	    } 
	    // Finds nbr of Seq i 
	    ArrayList<Seq> getNbrs(Seq v) { 
	        int i = v.getX(); 
	        return graph.get(i).nbrs; 
	    } 
	   
	    // The algorithm uses numbers, so seqInGrp maps a number to a name
	    private class TmpGrp  implements Comparable <TmpGrp> {
			public TmpGrp () {}
			
			public void add(int i, String s) {
				seqInGrp.put(i, s);
			}
			public void remove(int i) {
				seqInGrp.remove(i);
			}
			public int compareTo(TmpGrp g) {
				if (nSeqs > g.nSeqs) return -1;
				if (nSeqs < g.nSeqs) return 1;
				return 0;
			}
			int id, nSeqs=0;
			HashMap <Integer, String> seqInGrp = new HashMap <Integer, String> ();
		}
	}
	
	/*****************************************************
	 * Data structures:
	 * Use seqName instead of integer seqID because write seqName to file to be
	 * consistent with orthoMCL
	 */
	private class Grp {
		public Grp (int id) {
			this.id = id;
		}
		public void add(String s) {
			seqNames.add(s);
			seqMap.get(s).grpID=id;
		}
		public void addSeqs(Vector <String> list) {
			for (String s : list) {
				seqNames.add(s);
				seqMap.get(s).grpID=id;
			}
		}
		int id;
		Vector <String> seqNames = new Vector <String> ();
	}
	
	private class Seq implements Comparable <Seq> {
		Seq(int asmID) {this.asmID = asmID;}
		void add(String seq) {seqPairs.add(seq);}
		int grpID=0, asmID=0;
		HashSet <String> seqPairs = new HashSet <String> ();
		
		// For Bron_Kerbosche
		String name;
		int x, degree=0;
		ArrayList<Seq> nbrs = new ArrayList<Seq>(); 
		
		int getX() {return x; }
		void setX(int x) {this.x = x;}
		void setName(String name) {this.name=name;}
		
		ArrayList<Seq> getNbrs() { return nbrs; } 	

		void add(Seq y) {nbrs.add(y); degree++;}
		
       public int compareTo(Seq o) {
           if (this.degree < o.degree) return 1; 
           if (this.degree > o.degree) return -1;
           return 0; 
       } 
	}
	private class Pair implements Comparable <Pair>{
		public Pair (String seq1, String seq2, double eval, int sim, int olap1, int olap2, int bit, int aaBest) {
			this.seq1=seq1;
			this.seq2=seq2;
			this.eval=eval;
			this.bit = bit;
			this.aaBest=aaBest;
		}
		public int compareTo(Pair p) {
			if (aaBest>p.aaBest) return -1;
			if (aaBest<p.aaBest) return  1;
			if (eval<p.eval)     return -1;
			if (eval>p.eval)     return  1;
			if (bit>p.bit)       return -1;
			if (bit<p.bit)       return  1;
			return 0;
		}
		String seq1, seq2;
		double eval;
		double bit;
		int aaBest;
	}
	 private DBConn cmpDBC;			// database connection
	 private CompilePanel cmpPanel;	// get all parameters from this
	
	 private boolean bSuccess = true;
	 private int type, covMode;
	 private String prefix;
	 private int covCutoff, simCutoff;
	
	 private HashMap <Integer, String> asmMap = new HashMap <Integer, String> ();
	 private HashMap <String, Seq>     seqMap = new HashMap <String, Seq> ();
	 private HashMap <Integer, Grp>    grpMap = new HashMap <Integer, Grp> (); // grpID, list of SeqNames;
	 private Vector <Pair>  pairList =  new Vector <Pair> ();
}
