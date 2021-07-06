package sng.database;

/************************************************
 * Read in all metadata, e.g. number of sequences, etc for all methods to call
 */
import java.sql.ResultSet;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;

import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;

public class MetaData {
	private  final String L = Globals.LIBCNT;
	private  final String LN = Globals.LIBRPKM;
	private  final String P = Globals.PVALUE;
	
	public MetaData () {}// For DoGO.java so can get evidence code list, do not need mDB
	
	public MetaData (STCWFrame frame, String cap3) { 
		if (cap3!=null && cap3!="") {
			pathCAP3=cap3;
			bHasCAP3=true;
		}
		theMainFrame = frame;
		DBConn mDB = frame.getNewDBC();
		setMetaData(mDB);
		mDB.close();
	}
	public boolean setMetaData(DBConn mDB) {
		try {
			ResultSet rs; 
			int cnt;
			
			nSeqs = mDB.executeCount("Select COUNT(*) from contig");
			if (nSeqs==0) return true;
			
			if (mDB.tableColumnExists("contig", "cnt_ns")) {
				cnt = mDB.executeCount("SELECT COUNT(*) FROM contig WHERE cnt_ns > 1 LIMIT 1");
				if (cnt>0) bHasNs=true;
			}
			cnt = mDB.executeCount("SELECT COUNT(*) FROM pja_pairwise LIMIT 1");
			if (cnt>0) {
				bHasPairwise = true;
				nPairs = cnt;
			}
			
			cnt = mDB.executeCount("SELECT COUNT(*) FROM contig WHERE numclones > 1 LIMIT 1"); 	
			if (cnt>0) bHasAssembly = true;
			
			if (bHasAssembly) {
				cnt= mDB.executeCount("SELECT COUNT(*) FROM contig WHERE frpairs > 1 LIMIT 1"); 	
				if (cnt>0) bHasMatePairs = true;
				
				cnt = mDB.executeCount("SELECT COUNT(*) FROM buryclone LIMIT 1");	
				if (cnt > 0) bHasBuried = true;
			}
			else { // CAS311
				String b = mDB.executeString("SELECT pvalue FROM ASM_params  WHERE pname=\"USE_TRANS_NAME\"");
				if (b.contentEquals("1")) bUseOrigName=true;
			}
			bIsAAsTCW = mDB.tableColumnExists("assem_msg", "peptide") ;
			bHasLoc = mDB.tableColumnExists("assem_msg", "hasLoc");
			bHasNgroup = mDB.tableColumnExists("contig", "seq_ngroup");
			
			if (mDB.tableColumnExists("assem_msg", "norm")) // CAS304
				strNorm = mDB.executeString("select norm from assem_msg");
			
			cnt = mDB.executeCount("SELECT COUNT(*) FROM contig where o_frame!=0 LIMIT 1");	
			if (cnt > 0) bHasORFs = true;
			
			// annotation
			cnt = mDB.executeCount("SELECT count(*) FROM pja_databases");
			if (cnt>0) {
				bHasHits = true;
				totalSeqHitPairs = mDB.executeCount("select count(*) from pja_db_unitrans_hits");
				annoDBs = new String [cnt*2];
				
				TreeSet <String> type = new TreeSet <String> ();
				TreeSet <String> taxo = new TreeSet <String> ();
				rs = mDB.executeQuery("select dbtype, taxonomy from pja_databases");
		 		int i=0;
		 		while(rs.next()) {
		 			String tp = rs.getString(1), tx = rs.getString(2);
		 			annoDBs[i++] = tp; type.add(tp); 
		 			annoDBs[i++] = tx; taxo.add(tx);
		 		}
		 		i=0;
		 		typeDBs = new String [type.size()];
		 		for (String tp : type) typeDBs[i++] = tp;
		 		i=0;
		 		taxoDBs = new String [taxo.size()];
		 		for (String tx : taxo) taxoDBs[i++] = tx;
		 		
		 		if (mDB.tableColumnExists("pja_db_unique_hits", "kegg")) {
					cnt = mDB.executeCount("Select count(*) from pja_db_unique_hits " +
							"where kegg is not null and kegg !='' LIMIT 1");
					if (cnt>0) bHasKEGG=true;
				
					if (mDB.tableColumnExists("pja_db_unique_hits", "interpro")) {
						cnt = mDB.executeCount("Select count(*) from pja_db_unique_hits " +
								"where interpro is not null and interpro !='' LIMIT 1");
						if (cnt>0) bHasInterpro=true;
					}
				 	if (mDB.tableExists("go_info")) {
				 		cnt = mDB.executeCount("select count(*) from go_info LIMIT 1");
						if (cnt>0) bHasGOs=true;
				 	}
				 	if (mDB.tableColumnExists("assem_msg", "go_slim")) {
				 		rs = mDB.executeQuery("select go_slim from assem_msg");
				 		if (rs.next()) {
				 			String slim = rs.getString(1);
				 			if (slim!=null && !slim.equals("")) bHasSlims=true;
				 		}
				 	}
				}
			}
		
		 	// Sequence Sets
		 	deLibColList = null;
		 	cnt = mDB.executeCount("select count(*) from library where ctglib=1");
		 	if (cnt>0) {
		 		bHasSeqSets = true;
		 		seqLibNames = new String [cnt];
		 		seqLibTitles = new String [cnt];
		 		rs = mDB.executeQuery("select libid, title from library where ctglib=1");
		 		int i=0;
		 		while(rs.next()) {
		 			seqLibNames[i] = rs.getString(1);
		 			seqLibTitles[i++] = rs.getString(2);
		 		}
		 	}
		 	/***********************************************
		 	 * 					assmOnly	seqOnly		assm+exp	exp
		 	 * ctglib=1			0			0			1			1		has count file
		 	 * clone_exp		0			0			1			1		counts to trans
		 	 * contig_counts	1			0			1			0		assembled counts
		 	 * Can only have DE if there are count files with conditions
		 	 */ 
		 	cnt = mDB.executeCount("select count(*) from library where ctglib=1"); // CAS305 was ctglib=0, always true
		 	if (cnt>0) {
		 		bHasExpLevels = true;
		 		
		 		cnt = mDB.executeCount("select count(*) from library where ctglib=0");
		 		expLibNames = new String [cnt];
		 		expLibTitles = new String [cnt];
		 		rs = mDB.executeQuery("select libid, title from library where ctglib=0");
		 		int i=0;
		 		while(rs.next()) {
		 			expLibNames[i] = rs.getString(1);
		 			expLibTitles[i++] = rs.getString(2);
		 		}
		 	
			 	if (mDB.tableExists("libraryDE")) {
				 	cnt = mDB.executeCount("select count(*) from libraryDE");
				 	if (cnt>0) {
				 		bHasDE = true;
				 		deNames = new String [cnt];
				 		deTitles = new String [cnt];
				 		rs = mDB.executeQuery("select Pcol, title from libraryDE");
				 		i=0;
				 		while(rs.next()) {
				 			deNames[i] = rs.getString(1).substring(2); // remove P_
				 			deTitles[i++] = rs.getString(2);
				 		}
				 	}
			 	}	
		 	}
		 	loadGoPvalMap(mDB); // could be done above but needed for schema; needs to be initialized
		 	loadLibrary(mDB);
		 	checkVer(mDB);
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Getting metadata -- possible corrupted database"); return false;}
	}
	// CAS317 
	private void checkVer(DBConn mDB) {
		try {
			String annoVer = mDB.executeString("select annoVer from schemver");
			if (annoVer==null || annoVer=="") return; // CAS318 occurs if only Build Database
				
			String n = annoVer.replace(".","");
			int v = Static.getInteger(n);
			if (v<317) {
				Out.prt("+++Database was annotated before v3.1.7 - It was built with 'Best Eval', not 'Best Bits'.");
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e,"Checking version");}
		
	}
	/********************************************************
	* In order to show replicas, the lib index have to correspond with their LID
	* The deLibColList includes the ctglib=1 because needs the count of the assembled from the set.
	*/
	 private void loadLibrary(DBConn mDB) {
		try {
			ResultSet rs; 
 			int nLib = mDB.executeCount("SELECT count(*) from library") + 1;
 			
 			libNames2 = new String [nLib]; 	        
			for (int i=0; i<nLib; i++) libNames2[i]=null; 
			String fields=null;
			
			rs = mDB.executeQuery("SELECT LID, libid, ctgLib FROM library");
			while(rs.next ())
			{
				int id = rs.getInt(1);
				String name = rs.getString(2);
				int ctglib = rs.getInt(3);
				
				if (ctglib==0) libNames2[id] = name;
				
				if (fields==null) fields = L + name;
				else fields += "," + L + name;
				if (ctglib==0) fields += "," + LN + name;
			}
			if (deNames!=null)
				for (int i=0; i<deNames.length; i++) fields += "," +  P + deNames[i];
			
			deLibColList = fields;
			
			int cnt =  mDB.executeCount("select max(rep) from clone_exp");
			if (cnt>0) bHasReps=true;
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for Sequence Details");} 
	}
	/*******************************************************
	 * Load on demand
	 */
	/******************************************************
	 * BasicQueryTab -- this loads at beginning anyway.
	 */
	 public Vector <String> getSpecies() {
		if (species==null) loadSpecies();
		return species;
	 }
	 private void loadSpecies() {
		try {
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("SELECT DISTINCT species FROM pja_db_unique_hits order by species ASC");
            species = new Vector <String>();
			while(rs.next()) {
            		species.add(rs.getString(1).trim());
            }
            rs.close();
            mDB.close();
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for Species");} 
	}
	
	 public String getDeLibColList() { 
		return deLibColList;
	}
	 public String [] getLibNamesbyLID() {
		return libNames2;
	}
	 public boolean hasReps() {
		return bHasReps;
	}
	 /*************************************************
	 * used by BasicGOQueryTab upSeq, dnSeq; and for columns by BasicGOFilter - can change to read on startup
	 * RunDE and Overview query libraryDE in their own method
	 *************************************************/
	 public TreeMap <String, Double> loadGoPvalMap(DBConn mDB) { // called on schema.undateTo57
		 if (goPvalMap!=null) return goPvalMap;
		 goPvalMap = new TreeMap <String, Double> ();
		 
		 Vector <String> goCol = new Vector <String> ();
		 try {
			if (mDB.tableColumnExists("libraryDE", "goCutoff")) { // CAS321 (17-Apr-21)
				ResultSet rs = mDB.executeQuery("select pCol, goCutoff from libraryDE");
				while (rs.next()) {
					double cutoff =  rs.getDouble(2);
					if (cutoff>0) {
						goPvalMap.put(rs.getString(1), cutoff);
						goCol.add((rs.getString(1)));
					}
				}
				goPvalCols = new String[goCol.size()]; // CAS326
				int i=0;
				for (String c : goCol) goPvalCols[i++] = c.substring(2);
 				if (goPvalMap.size()>0) return goPvalMap;
			}
			
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for GO DE Pvals");}
		return goPvalMap; // CAS322 was return null
	 }
	/**********************************************************************
	 * ContigFramePanel: load on first use
	 */
	 public HashMap <String, Double> getTupleMap() { 
		if (tupleMap.size()==0) loadTuples();
		return tupleMap;
	}
	 
	 private void loadTuples() {
		try {
			ResultSet rs; 
			DBConn mDB = theMainFrame.getNewDBC();
			if (!mDB.tableExists("tuple_usage")) {
				mDB.close();
				return;
			}
			
		 	rs = mDB.executeQuery("SELECT tuple, freq from tuple_usage");
			while(rs.next()) tupleMap.put(rs.getString(1), rs.getDouble(2));
			
			if (rs!=null) rs.close(); 
			mDB.close();
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for Tuples");}
	}
	
	 /*********************************************************
	  * Basic GO Evidence - loads on first use
	  */
	 // runAS.DoUPdat only to ensure all Known EvCs 
	 public HashSet <String> getEvChash() {
		 createGoEvC();
		 HashSet <String> allEv = new HashSet <String> ();
		 for (String cat : evcCatMap.keySet()) {
			 String [] tok = evcCatMap.get(cat).split(",");
			 for (String t : tok) allEv.add(t);
		 }
		 return allEv;
	 }
	 // runSingleTCW.DoGO
	 public HashMap <String, String> getEvCatMap() {
		 createGoEvC();
		 return evcCatMap;
	 }
	 private void createGoEvC() { // no spaces between EvC
		 evcCatMap.put("EXP", "EXP,IDA,IPI,IMP,IGI,IEP");
		 evcCatMap.put("HTP", "HTP,HDA,HMP,HGI,HEP");
		 evcCatMap.put("PHY", "IBA,IBD,IKR,IRD");
		 evcCatMap.put("CA", "ISS,ISO,ISA,ISM,IGC,RCA");
		 evcCatMap.put("AS", "TAS,NAS");
		 evcCatMap.put("CS", "IC,ND");
		 evcCatMap.put("IEA", "IEA");
	 }
	private HashMap <String, String> evcCatMap = new HashMap <String, String> ();
	
	// BasicGOFilter (need cats) and BasicGOTable (need cats)
	public String [] getEvClist() {return evcCatList;}
	public String [] getEvCdesc() {return evcDescList;}
	
	private String [] evcCatList = {"EXP", "HTP", "PHY", "CA", "AS", "CS", "IEA"}; // Schema checks for "PHY" column
	private String [] evcDescList = 
		{"Experimental (EXP, IDA, IPI, IMP, IGI, IEP)",
		 "High throughput experimental (HTP, HDA, HMP, HGI, HEP)",
		 "Phylogenetically-inferred (IBA, IBD, IKR, IRD)",
		 "Computational analysis (ISS, ISO, ISA, ISM, IGC, RCA)",
		 "Author Statement (TAS, NAS)",
		 "Curator statement (IC, ND)",
		 "Electronic annotation (IEA)"
		};
	 /*********************************************************
	  * Basic GO - CAS319 overview needs this to be static
	  * alt_id is not really a relation, but is treated as one.
	  * 	It is referred to as "Alternate ID" and "Replaced_by" in AmiGO.
	  * 	GOtree.java replaced 'alt_id' accordingly
	  */
	 public static TreeMap <String, Integer> getGoRelTypes(DBConn mDB) {// CAS319 add
		 try {
			TreeMap <String, Integer> goRelTypeMap = new TreeMap <String, Integer> ();
			if (mDB.tableColumnExists("assem_msg", "go_rtypes")) {
				String rtypes = mDB.executeString("select go_rtypes from assem_msg");
				String [] tok = rtypes.split(";");
				for (String t : tok) {
					String [] r = t.split(":");
					int x = Integer.parseInt(r[0]);
					goRelTypeMap.put(r[1], x);
				}
			}
			if (goRelTypeMap.size()==0) { // for pre-319
				goRelTypeMap.clear();
				if (mDB.tableColumnExists("assem_msg", "is_a")) {
					goRelTypeMap.put("is_a", mDB.executeInteger("select is_a from assem_msg"));
					goRelTypeMap.put("part_of", mDB.executeInteger("select part_of from assem_msg"));
				}
				else Out.PrtWarn("The GO relation types are not in this sTCWdb");
			} 
			return goRelTypeMap;
		 }
		 catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for GO relations");}
		 return null;
	 }
	 /**********************************************************************/
		
	 public String getNorm() {return strNorm;}
	 public int nContigs() { return nSeqs;}
	 public int nContigSets() {if (seqLibNames==null) return 0; else return seqLibNames.length;}
	 public boolean hasContigSets() { return bHasSeqSets; }
	 public boolean hasNs() { return bHasNs; }

	 public String getLongLabel() {if (bHasAssembly) return "Longest"; else return "Orig ID";} // CAS311
	 public boolean bUseOrigName() {return bUseOrigName;} // CAS311
	 public boolean hasAssembly() { return bHasAssembly; }
	 public boolean hasNoAssembly() { return !bHasAssembly;}
	 public boolean hasBuried() {return bHasBuried; }
	 public boolean hasMatePairs() {return bHasMatePairs;}
	
	 public boolean hasPairWise() {return bHasPairwise;}
	 public int getnPairs() {return nPairs;}
	 public boolean isAAsTCW () {return bIsAAsTCW;}
	 public boolean isNTsTCW() {return !bIsAAsTCW;}

	 public int getNumSeqSets() { return seqLibNames.length; }
	 public String [] getSeqNames() { return seqLibNames; }
	 public String [] getSeqTitles() { return seqLibTitles; }
	 public String [] getDENames() { return deNames; }
	 public String [] getDETitles() { return deTitles; }
	 public String [] getLibNames() { return expLibNames; }
	 public String [] getLibTitles() { return expLibTitles; }
	 public boolean hasDE() { return bHasDE;}
	 public boolean hasExpLevels() { return bHasExpLevels; }
	 public String [] getGoPvalCols() { return goPvalCols;} // CAS326 no P__
	 public TreeMap <String, Double> getGoPvalPcolMap() {return  goPvalMap;} // has P_
	
	 public String [] getAnnoDBs() { return annoDBs;}
	 public String [] getTypeDBs() { return typeDBs;}
	 public String [] getTaxoDBs() { return taxoDBs;}
	 public boolean hasHits() { return bHasHits; }
	 public int totalSeqHitPairs() {return totalSeqHitPairs;}
	 public boolean hasKEGG() { return bHasKEGG; }
	 public boolean hasInterpro() { return bHasInterpro; }
	 public boolean hasGOs() { return bHasGOs; }
	 public boolean hasSlims() { return bHasSlims; }
	
	 public boolean hasLoc() { return bHasLoc;}
	 public boolean hasNgroup() { return bHasNgroup;}
	 public boolean hasORFs() {return bHasORFs;}
	 
	 // To assemble from SeqDetails
	 public boolean hasCAP3() {return bHasCAP3;}
	 public String pathCAP3() { return pathCAP3;}

	/*******************************************************************/
	 private int nSeqs = 0;
	 private boolean bHasAssembly = false;
	 private boolean bUseOrigName = false; // CAS311
	 private boolean bHasSeqSets = false;
	 private String [] seqLibNames = null;
	 private String [] seqLibTitles = null;
	 private String strNorm="RPKM";
	
	 private boolean bHasNs = false;
	 private boolean bHasBuried = false;
	 private boolean bHasMatePairs = false;
	
	 private boolean bHasExpLevels = false;
	 private String [] expLibNames = null;
	 private String [] expLibTitles = null;
	 private boolean bHasDE = false;
	 private String [] deNames = null;
	 private String [] deTitles = null;
	 private TreeMap <String, Double> goPvalMap = null;
	 private String [] goPvalCols = null;
	
	 private String [] annoDBs = null, typeDBs = null, taxoDBs=null;
	 private Vector <String> species = null;
	 private int totalSeqHitPairs=0;
	 private boolean bHasHits = false;
	 private boolean bHasGOs = false;
	 private boolean bHasKEGG = false;
	 private boolean bHasInterpro = false;
	 private boolean bHasSlims = false;
	
	 private boolean bHasPairwise = false;
	 private boolean bIsAAsTCW = false;
	 private boolean bHasLoc = false;
	 private boolean bHasNgroup = false;
	 private boolean bHasORFs = false;
	 
	 private boolean bHasCAP3 = false;
	 private String pathCAP3 = null;

	// load on demand
	 private String deLibColList = null;
	 private boolean bHasReps = false;
	 private String [] libNames2 = null;
	 private HashMap <String, Double> tupleMap = new HashMap <String, Double> ();
	 private int nPairs;
	 
	 private STCWFrame theMainFrame;
}
