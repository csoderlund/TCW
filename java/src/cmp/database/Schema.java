package cmp.database;

/*********************************************************
 * Contains the complete schema except:
 * 1. Method prefixes are added to the 
 * 		unitrans table in MethodLoad.step1_loadMethod
 *    	pairwise table in Pairwise.saveMethodPairwise. 
 * 2. The RPKM and DE are added in addDynamicColumns
 * Version additions are in Version, but also added to schema.
 */
/***
 * Largest known protein is 33k; so transcripts could be 100k
* CHAR: tinytext 1 byte, text 2 byte, mediumtext 2 byte
* max	256 char		     65k char	 16M char
* 		use VARCHAR for fields that need to be searched
* 
* INT		tinyint(1), smallint(2),  mediumint(3), int(4), bigint(8)		
* Unsigned	255               65k        16M		   4394M		
* Signed		127				  32k		 8M		   2147M	
* float 4 byte,  double 8 byte
* 
* 32-bit machine int 2145M. Java has no unsigned class. 
*****/
/**************************************************
 * 9jar17 - changed primary keys from bigints to ints or smallint
 */
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import cmp.compile.runMTCWMain;
import cmp.compile.panels.CompilePanel;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class Schema {
	DBConn mDB;

	public Schema (DBConn targetConn, String Sver, String path) { // new database
		mDB = targetConn;
		try {
			loadSchema();
			
			String user = System.getProperty("user.name");
			mDB.executeUpdate(
			"insert into info (schemver, version, annoState, compiledate, username, path, summary) "
					+ " values(" 
					+ quote(Version.DBver)	+ ","
					+ quote(Sver) 			+ "," 
					+ quote("start")			+ ","
					+ "NOW()" 				+ "," 
					+ quote(user) 			+ ", " 
					+ quote(path) 	+ "," 
					+ quote("START") + ")");
		} catch (Exception e) {ErrorReport.die(e, "entering schema version");}
	}
	public static void updateVersion (DBConn mDB) { // CAS301
		try {
			mDB.executeUpdate("update info set lastDate=NOW(), lastVer='" + Globalx.strTCWver + "'");
		} catch (Exception e) {ErrorReport.die(e, "update version");}
	}
	public void loadSchema() {
		try {
			String sqlU;
			
			// Information added during runMulti
			sqlU = "CREATE TABLE info ( " +
					"schemver 	tinytext, " +
					"version  	tinytext, " +	// build
					"compiledate date, " +
					"lastVer    tinytext, " +   // CAS310 db62 update
					"lastDate 	date, " +			// CAS310 db62
					"annoState 	tinytext, " +
					"username 	tinytext, " +
					"path 		text, " + 
					"summary 	text," +  // for viewer
					"seqInfo	text, " + // loadSingleTCW
					"pairInfo   text, " + // PairStats
					"kaksInfo   text, " + // Pairwise
					"aaPgm		tinytext, " + // BlastPanel
					"ntPgm		tinytext, " + // BlastPanel - always the same, but in case that changes
					"aaInfo		tinytext, " + // Pairwise 
					"ntInfo		tinytext, " + // Pairwise 
					"hasLib 	tinyint default 0," +
					"hasDE 		tinyint default 0," +
					"hasPCC 	tinyint default 0," + // CAS310 db62
					"hasMA 		tinyint default 0," + // CAS310 db62
					"grpSQL  text, " +		// DBinfo for sample table
					"pairSQL  text, " +
					"seqSQL  text, " +
					"hitSQL  text, " +	// CAS310 db62
					// dynamic columns
					"allSeqLib 	text," +
					"allSeqDE 	text," +
					"allASM 		text," +
					"allMethods text," +
					"allTaxa 	text" +
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
		   		
			// all sTCW databases in mTCW database; the term 'assembly' is outdated 
			// -- should be dataset
			sqlU =  "CREATE TABLE assembly ( " +
					"ASMid 		smallint NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
					"ASMstr 		VARCHAR(30), " +
					"prefix		VARCHAR(10), " +
					"username 	tinytext, " +
					"nPep 		int default 0, " +		// # AA sequences
					"nUT 		int default 0, " +		// # NT sequences
					"nAnnoUT 	int default 0, " +		// # annotated sequences
					"nAlign 		int default 0, " +		// # aligned sequences for all contigs
					"nExp  		bigint default 0, " +	// expression level count for all contigs
					"nLib 		smallint default 0, " +
					"nAnnoDB 	smallint default 0, " +
					"isPep		tinyint default 0," +
					"annoDBdate date, " +		// first one
					"annotationdate date, " +
					"assemblydate date, " +
					"remark 		text " +	
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
				
			// all sequences from all sTCW assemblies - the term unitrans is outdated.
			sqlU =  "CREATE TABLE unitrans ( " +
					"UTid 		int NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
					"UTstr 		VARCHAR(30), " +		// seqeunce name
					"ASMid 		smallint, " +       	// sTCW index into assembly data
					"numAlign 	smallint default 0, " +	// num_clones if assembled
					"ntLen  		int default 0, " +	// nucleotide sequence length
					"aaLen		int default 0, " +	// protein sequence length
					"orf_frame	tinyint default 0," +
					"orf_start	int default 0," +
					"orf_end		int default 0," +
					"cdsLen		int default 0," +
					"utr5Len		int default 0," +	
					"utr3Len		int default 0," +	
					"CpG			float default 0," +
					"GC			float default 0," +
					"utr5Ratio	float default 0," + // CpG ratio for UTR5
					"cdsRatio	float default 0," + // CpG ratio for CDS
					"utr3Ratio	float default 0," + // CpG ratio for UTR3
					"HITid 		int default 0, " + // this is best anno hit
					"HITstr 		VARCHAR(30), " +
					"e_value 	double, "     +		// blast evalue to hit
					"totExp 		int, " +
					"totExpN 	int, " +
					"expList 	text, " +			// list of libraires
					"expListN 	text, " +			// list of normalized libraires
					"ntSeq 		mediumTEXT, " +  	// sTCW consensus
					"aaSeq 		mediumTEXT, " +		// from file
					"nPairs		int default 0, " +  
					"index 		idx1 (UTstr)," +
					"index 		idx2 (HITid), " +	
		   			"index 		idx3 (HITstr) " +
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
			
			// The top N hits for each annoDB for each sequence. The term unitrans is outdated
			sqlU =  "CREATE TABLE unitrans_hits ( " +
					"HITid 		int, "	 +		// Primary in unique_hits
					"HITstr 		VARCHAR(30), " +
					"UTid 		int, " +        // Primary for unitrans
					"UTstr 		varchar(30), " + 
					"percent_id smallint, " +
					"alignment_len int, " +
					"seq_start 	int, " +
					"seq_end 	int, " +
					"hit_start  int, " +
					"hit_end 	int, " +
					"e_value 	double, " +
					"bit_score 	float, " +
					"type		VARCHAR(20), " +
					"bestEval	tinyint default 0," +		
					"bestAnno	tinyint default 0,"	+	
					"bestGO		tinyint default 0,"	+ 
					"unique(HITid, UTid), " +
					"index idx1 (HITid), " + 
					"index idx2 (HITstr), " +
					"index idx3 (UTstr), " +
					"index idx4 (UTid)" +
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
				
			// DB hits from all sequences 
		 	sqlU =	"CREATE TABLE unique_hits ( " +
		   			"HITid 		INT NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
		   			"HITstr 		VARCHAR(30), " +		// HitID in viewMulti
		   			"dbtype 		VARCHAR(10), " +
		   			"taxonomy 	VARCHAR(20), " +
		   			"isProtein 	tinyint, " +
		   			"description VARCHAR(250), " +
		   			"species 	VARCHAR(100), " +
		   			"length 		int, " +
		   			"sequence 	mediumtext, " +
		   			"goList 	mediumtext, " +
		   			"nGO			int default 0, " + 	 // CAS310 db62
		   			"nSeq			int default 0, " +   // CAS310 db62
		   			"nBest			int default 0, " +   // CAS310 db62
		   			"e_value		double default 0, "	+// CAS310 db62
		   			"unique (HITstr), " +	
		   			"index idx1 (HITstr) " +
		   			") ENGINE=MyISAM;";
		 	mDB.executeUpdate(sqlU);
		
			// Cluster_method -- undated term POG (Pave orthologous Groups) 
			sqlU =  "CREATE TABLE pog_method ( " +
					"PMid 			smallint NOT NULL PRIMARY KEY AUTO_INCREMENT, " + // should only be a few methods in db
					"PMstr 			VARCHAR(30), " +
					"PMtype			VARCHAR(30), " +
					"adddate date, " +
					"prefix			VARCHAR(10), " + // (allow 5 from interface)
					"description 	text, " +
					"parameters		text " + 		// not used yet, but should save
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
				
			// XXX each method has one or more pogs (clusters)
			sqlU =	"CREATE TABLE pog_groups ( " +
					"PGid 		int NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
					"PGstr 		VARCHAR(30), " +			// name of cluster
					"PMid 		smallint, " + 			// primary key in pog_method
					"count 		smallint, " +			// number of unitrans in cluster
					"perAnno 	tinyint default 0, " +	// percent with best hit
					"HITid 		int default 0, " +		// index into unique_hit, best hit across all members
					"HITstr 		VARCHAR(30), " +
					"e_value 	double, "     +	 		// best evalue
					"taxa		tinytext, " +
					"perPCC 		tinyint default -1, " + 
					"minPCC		float default 0,"  +	
					
					"conSeq		mediumTEXT, " +    // consensus sequence
					"conLen		int default 0, " + // consensus length
					"sdLen		float default " +   Globalx.dNoVal + ", " +// stddev from conLen
					// SortTable checks for Globalx.scoreField (prefix "score")
					"score1		float default " +   Globalx.dNoScore + ", " + // sum of sum of pairs - any value
					"score2		float default " +   Globalx.dNoVal +  ", " +  // trident -1 to 1
					
					// dynamic summed counts for each sTCWdb is added 
					"index idx1(PGstr)," +
					"index idx2(HITid)," +
					"index idx3(HITstr)" +
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
		
			// each cluster (pog_group) has two or more members
			sqlU = 	"CREATE TABLE pog_members ( " +
					"MEMid 	int NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
					"PGid  	int, " +   		// Primary in pog_groups
					"PGstr 	VARCHAR(30), " +		// name of cluster
					"UTid  	int, " +			// Primary in unitrans
					"UTstr 	VARCHAR(30), " + 	// name of sequence
					"alignMap text, "		+ //align to pog_group.conSeq
					"index 	idx1(PGid)," +		
					"index 	idx2(PGstr)," +
					"index 	idx3(UTid)," + 		
					"index 	idx4(UTstr)" +
					") ENGINE=MyISAM;"; 
			mDB.executeUpdate(sqlU);
			
			// XXXAll pairwise statistics, initiated with the search hits
			sqlU = "create table pairwise (" +
					"PAIRid 		int NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
					"ASMid1 		smallint not null, " +     // primary in assembly
					"ASMid2 		smallint not null, " +	   // primary in assembly
					"UTid1 		int not null, " +		   // primary in unitrans
					"UTid2 		int not null, " +          // primary in unitrans
					"UTstr1 		VARCHAR(30) not null, " +	// name of first sequence
					"UTstr2 		VARCHAR(30) not null, " +	// name of second sequence
					"CDSlen1		int default 0," +
					"CDSlen2		int default 0," +
					"PCC			float default  " + Globalx.dNoVal + "," + // PCC of RPKM, between -1 and 1
					"HITid 		int default 0, " +		//  primary in unique_hit
					"HITstr		VARCHAR(30), " +
					
					// blast - nt and aa
					"ntEval		double default -2.0,  " + 	// blast e-value of pair
					"ntSim		float default -2.0, "		+ 
					"ntAlign		int default -2, "		+
					"ntGap		smallint default -2, " +
					"ntOlap1		float default -2.0, " +		
					"ntOlap2		float default -2.0, " +		
					"ntBit		int default -2, " +
					"aaEval		double default -2.0,  " + 	// blast e-value of pair
					"aaSim		float default -2.0, " +		
					"aaAlign		int default -2, "		+
					"aaGap		smallint default -2, " +
					"aaOlap1		float default -2.0, " +		
					"aaOlap2		float default -2.0, " +		
					"aaBit		int default -2, " +
					"aaBest		tinyint default -2, " +				
	
					// dp align 
					"nAlign		int default -2, "	  +		// number of aligned nt include gaps except in overhang
					"pOlap1		float default -2.0, " +		// Cov1
					"pOlap2		float default -2.0, " +		// Cov2
					"nSNPs		int default -2.0, "	  +
					"nGap		smallint default -2, " +		// #of dashes not counting indel.
					"nOpen		smallint default -2, " +		// gap open
					
					"cAlign		int default -2, "	  +		// number of aligned codons minus codons with gaps
					"pCmatch		float default -2.0, " +		// exact match
					"pCsyn		float default -2.0, " +		// synonymous
					"pC4d		float default -2.0, " +		// codon pair in 4-fold  
					"pC2d		float default -2.0, " +		// codon pair in 2-fold
					"pCnonsyn	float default -2.0, " +		// non-synonymous
					
					"pAmatch		float default -2.0, " +		// exact match
					"pAsub		float default -2.0, " +		// %Apos - likely substitution
					"pAmis		float default -2.0, " +		// %Aneg - unlikely substitution
					
					"pDiffCDS    float default -2.0, " +		
					"pDiffUTR5   float default -2.0, " +		
					"pDiffUTR3   float default -2.0, " +		
					
					"GC			float default -2.0, " +	 // Jaccard Index #shared/#total
					"CpGn		float default -2.0, " +   // nt Jaccard Index #shared/#total
					"CpGc		float default -2.0, " +   // codon Jaccard Index #shared/#total
					"tstv		float default -2.0, "	+
					
					// KaKs  (BBH only)
					"ka			float default -2.0, " +
					"ks			float default -2.0, " +
					"kaks		float default -2.0, " +	
					"pVal		float default -2.0, " +		// pvalue for kaKs
					
					"hasBBH		tinyint default 0, " +      // if 1 -- pair is in BBH cluster
					"hasGrp		tinyint default 0, " +      // if 1 -- pair is in a cluster together
		
					"unique (UTid1, UTid2), " +
					"index idx1(UTid1), " +
					"index idx2(UTid2), " +
					"index idx3(UTstr1), " +
					"index idx4(UTstr2), " +
					"index idx5(ASMid1), " +
					"index idx6(ASMid2), " +
					"index idx7(HITid)" + 
					")ENGINE=MyISAM; ";
			mDB.executeUpdate(sqlU);
			
			// contains NT alignment used for statistics. 
			// currently not used for viewMulti
			sqlU = "create table pairMap (" +
					"PAIRid 		int, " +
					"cds			text," +   // n-m:n-M###n-m   '###' delinates cds1 from cds2
					"utr5		text," +
					"utr3		text," +
					"index idx1(PAIRid) " +
				")ENGINE=MyISAM; ";
			mDB.executeUpdate(sqlU);
			
			// XXX GO 
			// all assigned and inherited GO-seq pairs
			sqlU = "create table go_seq (" +
					" UTid 	int, " +		// Primary in unitrans
					" gonum	int, " +
					// the following 5 go togehter
					" bestHITstr varchar(30), "	+	// index into unique_hits.HITid
					" bestEval double, " +        
					" bestEV boolean default 0, " + 
					" bestAN boolean default 0, " +
					" EC varchar(5) default ''," + // if in parenthesis, inherited
					" direct boolean default 0, " + // any hit_GO could be direct whereas EC is inherited
					" unique(UTid,gonum), " +
					" index idx1(UTid), " +
					" index idx2(gonum) " +
					") ENGINE=MyISAM;";
			mDB.executeUpdate(sqlU);
			
			sqlU = "create table go_info (" +
					" gonum int, " +
					" descr text, " +
					" term_type enum('biological_process','cellular_component','molecular_function'), " +
					" level smallint default 0, " + 
					" bestEval double, " + 
					" nSeqs int default 0, " + 
					" unique(gonum), " +
					" index(gonum) ) ENGINE=MyISAM;";
			if (!mDB.tableExist("go_info")) mDB.executeUpdate(sqlU);
			
			mDB.executeUpdate("create table go_graph_path (" +
					" relationship_type_id tinyint unsigned, " + // e.g. 1 is_a, 27 part_of; can have duplicates because relation can be both
					" distance smallint unsigned, " + 			//e.g. if A part_of B is_a C part_of D, then distance=3 for A part_of D 
					" relation_distance smallint unsigned, " + // e.g. if A part_of B is_a C part_of D, then relation_distance=2 for A part_of D 
	    				" child int unsigned, " +
	    				" ancestor int unsigned, " +
	    				" index(child), " +
	    				" index(ancestor)) ENGINE=MyISAM;");
		} 
		catch(Exception e) {ErrorReport.die(e, "Entering schema");} 
	} 
	
	/**********************************************************
	 * XXX add dynamic columns
	 */
	static public boolean addDynamicCountColumns(DBConn mDB, CompilePanel cmpPanel) 
	{
		Out.PrtSpMsg(1, "Add TPM and DE columns");
		HashMap<String,HashSet<String>> asmLibs = new HashMap<String,HashSet<String>>();
		HashMap<String,HashSet<String>> asmDEs = new HashMap<String,HashSet<String>>();	
		Vector <String> allLibs = new Vector <String>();
		Vector <String> allDEs = new Vector <String>();
		boolean hasDE=true, hasTPM=true;
		
		ResultSet rs;
		try
		{
	    		for(int x=0; x<cmpPanel.getSpeciesCount(); x++) 
	    		{
	    			String asm = cmpPanel.getSpeciesSTCWid(x);
				
				asmLibs.put(asm, new HashSet<String>());
				asmDEs.put(asm, new HashSet<String>());
				
				DBConn sDBC = runMTCWMain.getDBCstcw(cmpPanel, x);	
					
				int cntR=0, cntP=0;
				String r="", p="";
				// ctglib=1 means there are expression libraries.
				int cnt = sDBC.executeCount("select count(*) from library where ctglib=1");
				if (cnt>0) {
					rs = sDBC.executeQuery("select libid from library where ctglib=0");
					while (rs.next())
					{
						String libName = rs.getString(1);
						asmLibs.get(asm).add(libName);
						addCheckCase(allLibs,libName);
						cntR++;  r += " " + libName;	
					}
				
					rs = sDBC.executeQuery("show columns from contig");
					while (rs.next()) {
						String colName = rs.getString(1);
						if (colName.startsWith(Globals.PRE_S_DE)) {
							String col = colName.substring(Globals.PRE_S_DE.length());
							asmDEs.get(asm).add(col);
							addCheckCase(allDEs,col);
							cntP++; p += " " + col;
						}
					}
					if (cntR>0) {
						String msg = asm + " " + cntR + " TPM (" + r + ") ";
						if (cntP>0) msg += + cntP + " DE (" + p + ")";
						Out.PrtSpMsg(2, msg);
					}
				}
				sDBC.close();
	    		}
	    		if (allLibs.size() > 0) {
	    			Out.PrtSpCntMsg(2,allLibs.size(), "Total unique conditions ");    			
	    		}
	    		else hasTPM = false;
	    		
	    		if (allDEs.size() > 0) {
	    			Out.PrtSpCntMsg(2, allDEs.size(), "Total unique DE names");
	    		}
	    		else hasDE = false;
	    		
	    		if (!hasDE && !hasTPM) {
	    			Out.PrtSpMsg(2, "No TPM or DE columns in any of the databases");
	    			return true;
	    		}
	    		
	    		// Add TPM (RPKM for pre-305)
	    		for (String lib : allLibs)
	    		{
	    			mDB.executeUpdate("alter table unitrans add " + Globals.PRE_LIB + lib + " double default " + Globalx.dStrNoVal);
	    		}
	    		// Add DE 
	    		for (String de : allDEs)
	    		{	// DEs can be between -1 and 1; generally -2 is not displayed (see sortTable.java)
	    			mDB.executeUpdate("alter table unitrans add " + Globals.PRE_DE + de + " double default " + Globalx.dStrNoDE);
	    		}

	    		return true;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error adding count columns");
			return false;
		}
		catch(Error e) {
			ErrorReport.reportFatalError(e, "Error adding count columns", null);
			return false;
		}
	}
	static public boolean addDynamicSTCW(DBConn mDB, String prefix) {
		String field = Globals.PRE_ASM + prefix;
	    try {
	    		mDB.executeUpdate("ALTER TABLE pog_groups ADD " + field + " integer default 0");
	    		return true;
	    }
	    catch (Exception e) {
	    		Out.PrtError("Your selected sTCW databases have duplicate singleTCW IDs: "  + prefix);
	    		Out.PrtSpMsg(2, "The only way to change an ID is to rebuild one of the databases");
	    }
	    return false;
	}
	static public void addDynamicMethod(DBConn mDB, String prefix) {
		try {
			Out.PrtSpMsg(2,"Adding column " + prefix + " to sequence and pair tables .... ");
			
			mDB.tableCheckAddColumn("pairwise", prefix, "VARCHAR(30)", "hasGrp"); // add group to pairwise
			mDB.tableCheckAddColumn("unitrans", prefix, "VARCHAR(30)", "");	
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error adding pairwise column " + prefix);}
	}
	static private void addCheckCase(Vector <String> set, String name)
	{
		boolean found = false;
		for (String s : set)
		{
			if (s.equalsIgnoreCase(name)) 
			{
				found = true;
				break;
			}
		}
		if (!found) set.add(name);
	}
	
	private String quote(String word) {
		return "'" + word + "'"; 
	}	
}
