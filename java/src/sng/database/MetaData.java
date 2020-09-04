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
import java.util.Arrays;

import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.methods.ErrorReport;

public class MetaData {
	private  final String L = Globals.LIBCNT;
	private  final String LN = Globals.LIBRPKM;
	private  final String P = Globals.PVALUE;
	
	public MetaData () {} // For GO.java so can get evidence code list, which is pre-built
	
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
	public boolean setMetaData(DBConn mDB) 
	{
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
			bIsProteinDB = mDB.tableColumnExists("assem_msg", "peptide") ;
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
		 	// Count Sets
		 	cnt = mDB.executeCount("select count(*) from library where ctglib=0");
		 	if (cnt>0) {
		 		bHasExpLevels = true;
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
		 	loadLibrary(mDB);
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Getting metadata -- possible corrupted database"); return false;}
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
	 * used by BasicGOQueryTab upSeq, dnSeq
	 * created in QRProcess: mDB is passed in because this is not part of viewSingleTCW, 
	 * 						 so no STCWframe.newDBConn.
	 *************************************************/
	 public TreeMap <String, Double> getDegPvalMap(DBConn mDB) {
		 if (degPvalMap!=null) return degPvalMap;
		 try {
			degPvalMap = new TreeMap <String, Double> ();
			if (!mDB.tableColumnExists("assem_msg", "goDE")) {
				mDB.close();
				return null;
			}
			String goDE = mDB.executeString("select goDE from assem_msg");
			mDB.close();
			
			String [] tok = goDE.split(",");
			for (String x : tok) {
				String [] tok2 = x.split(":");
				if (tok2.length==2)
					degPvalMap.put(tok2[0], Double.parseDouble(tok2[1]));
			}
			return degPvalMap;
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for GO DE Pvals");}
		return null;
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
	  * Basic GO - loads on first use
	  */
	 public HashSet <String> getECinDB() {
		if (ecInDB==null) loadGoEC();
		return ecInDB;
	 }
	 
	 public String [] getTermTypes() {
		 if (termTypes!=null) return termTypes;
		 try {
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rset = mDB.executeQuery("SHOW COLUMNS FROM go_info LIKE 'term_type'");
			if(rset.next()) {
				String val = rset.getString(2);
				val = val.substring(val.indexOf('(') + 1, val.length() - 1);
				String [] valArr = val.split(",");
				termTypes = new String[valArr.length];
				for(int x=0; x<valArr.length; x++) {
					termTypes[x] = valArr[x].substring(1, valArr[x].length()-1);
				}
			}
			rset.close(); 
		 }
		 catch (Exception e) {ErrorReport.prtReport(e, "Getting term types");}
		return termTypes;
	 }
	 
	 public HashSet <String> getEChash() {return new HashSet<String> (Arrays.asList(ecList));}
	 public String [] getEClist() {
		 return ecList;
	 }
	 public String [] getECdesc() {
		 return ecDesc;
	 }
	 public int getECnum() {
		return ecList.length;
	 }
	 private void loadGoEC() {
		try {
			ecInDB = new HashSet <String>();
			DBConn mDB = theMainFrame.getNewDBC();
			
			if (!mDB.tableColumnExists("go_info", "IEA")) return;
			
			String ecStr = mDB.executeString("SELECT go_ec from assem_msg");
			if (ecStr!=null) {
				String [] ec = ecStr.split(",");
				for (String x : ec) ecInDB.add(x);
				return;
			}
			
			// first time its run after annotation, create list
			ResultSet rs = mDB.executeQuery("SELECT DISTINCT EC FROM pja_uniprot_go");
			while(rs.next()) {
				String ec = rs.getString(1).trim();
            		ecInDB.add(ec);
            		if (ecStr==null) ecStr=ec;
            		else ecStr += ","+ec;
            }
            rs.close();
            mDB.executeUpdate("UPDATE assem_msg set go_ec='" + ecStr + "'");
            
            mDB.close();
              
	    		for (String ec : ecInDB) {
	    			boolean found=false;
	    			for (String ect : ecList) 
	    				if (ec.equals(ect)) {
	    					found=true;
	    					break;
	    				}
	    			if (!found) {
	    				System.err.println("Error: no known evidence code " + ec);
	    				if (!ecInDB.contains("UNK")) ecInDB.add("UNK");
	    			}
	    		}
		} 
		catch (Exception e) {ErrorReport.reportError(e, "Error: reading database for GO EC");} 
	}

	private HashSet <String> ecInDB = null;
	
	private String [] ecList = {
			"EXP", "IDA","IMP","IGI","IPI","IEP", // experimental
			"HTP", "HDA", "HMP", "HGI", "HEP",   // high throughput
     		"ISS","ISO","ISA","ISM","IGC","IBA","IBD","IKR","IRD","RCA", // computational
     		"TAS","NAS",  // author
     		"IC", "ND",   // curator statement
     		"IEA", 		  // electronic
     		"UNK"};
	private String [] ecDesc = {
			"Inferred from Experimental",
			"Inferred from Direct Assay",
      		"Inferred from Mutant Phenotype",
      		"Inferred from Genetic Interaction",
      		"Inferred from Physical Interaction",
      		"Inferred from Expression Pattern",
      		
      		"Inferred from High Throughput Experiment",
      		"Inferred from High Throughput Direct Assay",
      		"Inferred from High Throughput Mutant Phenotype",
      		"Inferred from High Throughput Genetic Interaction",
      		"Inferred from High Throughput Expression Pattern",
      		
      		"Inferred from Sequence or structural Similarity",
      		"Inferred from Sequence Orthology",
      		"Inferred from Sequence Alignment",
      		"Inferred from Sequence Model",
      		"Inferred from Genomic Context",
      		"Inferred from Biological aspect of Ancestor",
      		"Inferred from Biological aspect of Descendant",
      		"Inferred from Key Residues",
      		"Inferred from Rapid Divergence", 
      		"Inferred from Reviewed Computational Analysis",
      		
      		"Inferred from Traceable Author Statement",
      		"Non-traceable Author Statement",
      		"Inferred by Curator",
      		"No Biological Data Available", 
      		
      		"Inferred from Electronic Annotation",
      		"Unknown"};
	/**********************************************************************/
	
	 public String getNorm() {return strNorm;}
	 public int nContigs() { return nSeqs;}
	 public int nContigSets() {if (seqLibNames==null) return 0; else return seqLibNames.length;}
	 public boolean hasContigSets() { return bHasSeqSets; }
	 public boolean hasNs() { return bHasNs; }

	 public boolean hasAssembly() { return bHasAssembly; }
	 public boolean hasNoAssembly() { return !bHasAssembly;}
	 public boolean hasBuried() {return bHasBuried; }
	 public boolean hasMatePairs() {return bHasMatePairs;}
	
	 public boolean hasPairWise() {return bHasPairwise;}
	 public int getnPairs() {return nPairs;}
	 public boolean isProteinDB () {return bIsProteinDB;}
	 public boolean isNucleoDB() {return !bIsProteinDB;}

	 public int getNumSeqSets() { return seqLibNames.length; }
	 public String [] getSeqNames() { return seqLibNames; }
	 public String [] getSeqTitles() { return seqLibTitles; }
	 public String [] getDENames() { return deNames; }
	 public String [] getDETitles() { return deTitles; }
	 public String [] getLibNames() { return expLibNames; }
	 public String [] getLibTitles() { return expLibTitles; }
	 public boolean hasDE() { return bHasDE;}
	 public boolean hasExpLevels() { return bHasExpLevels; }
	
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
	
	 private String [] annoDBs = null, typeDBs = null, taxoDBs=null;
	 private Vector <String> species = null;
	 private int totalSeqHitPairs=0;
	 private boolean bHasHits = false;
	 private boolean bHasGOs = false;
	 private boolean bHasKEGG = false;
	 private boolean bHasInterpro = false;
	 private boolean bHasSlims = false;
	 private String [] termTypes = null;
	
	 private boolean bHasPairwise = false;
	 private boolean bIsProteinDB = false;
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
	 
	 private TreeMap <String, Double> degPvalMap = null;
	 
	 private STCWFrame theMainFrame;
}
