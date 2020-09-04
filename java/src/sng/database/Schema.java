package sng.database;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Vector;

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;

/** 
 * to change schema number: 
 * (1) change in util.methods.Version
 * (2) change in this file: add to DBVer emum, add to Schema constructor,
 * change method current(), and change in the schema.
 ***/

/**
 * Max length of protein or RNA identifier to 30
 * its hardcoded in the tables. If it gets changed, change in DoUniProt.java
**/

/***
* CHAR: tinytext 1 byte, text 2 byte, mediumtext 2 byte
* max	256 char			65k char		 16M char
* 		use VARCHAR for fields that need to be searched
* INT	tinyint 1 byte, smallint 2 byte,  mediumint 3 byte, int 4 byte, bigint 8 byte
* max	256               65k                16M				4394M		
* 		float 4 byte,  double 8 byte
*****/

enum DBVer 
{
	Ver35, // 3.5 - a few columns 
	Ver40, // 4.0 - few columns for reps
	Ver41, // 4.1 - few columns for annoDB
	Ver42, // 4.2 - go columns (135, 137 and 140 updates)
	Ver50, // 5.0 - more go (1.5)
	Ver51, // 5.1 - change cnt_taxo to cnt_annodb
	Ver52, // 5.2 - add user_remark
	Ver53, // 5.3 add assem_msg.spAnno and assem_msg.go_ec
	Ver54, // 5.4 add o_markov and rename p_coding fields; the Engine was not set for all tables.
	Ver55  // MySQL V8 changed rank to best_rank
}

/********************************************
 * DYNAMIC FIELDS:
 * The following are not in the below schema, but added later.
 * 	L__<lib> for counts, LN__<lib> for RPKM  - based on lib name	(this file)
 * 	P_<libLib> for pvalues -- add if computed						(QRProcess.saveDEcols)
 * 	assem_msg.peptide -- add if protein databases 					(Library)
 * 	assem_msg.hasLoc --  add if has location information 			(Library and AddRemarkPanel)
 *  assem_msg.goDE -- DE:Pval list for goseq p-values				(QRProcess)
 * 	contig.seq_ngroup -- add if group has number, e.g. scaffold123 	(AddRemarkPanel)
 * 	ORF tables -- tuple_orf, tuple_usage							(DoORF) 
 * 	GO tables -- go_info, pja_gotree, pja_uniprot_go, pja_unitrans_go (createGOtables)
 */
public class Schema 
{
	DBConn mDB;
	DBVer  dbVer =  DBVer.Ver55; // default to this, as in other cases we get it from the schemver table
	String dbVerStr = null;      // read from database
	
	DBVer  curVer = DBVer.Ver55; 
	String curVerStr = "5.5";
	public static String currentVerString() {return "5.5";}
	public static DBVer  currentVer() 		{return DBVer.Ver55;}
	
	public Schema(DBConn db)
	{
		mDB = db;		
		try
		{
			if (!db.tablesExist()) return; 
			
			ResultSet rs = mDB.executeQuery("select schemver from schemver");
			rs.first();
			dbVerStr = rs.getString("schemver");
			
			if (dbVerStr.equals("3.5"))			dbVer = DBVer.Ver35;
			else if (dbVerStr.equals("4.0"))	dbVer = DBVer.Ver40;
			else if (dbVerStr.equals("4.1"))	dbVer = DBVer.Ver41;
			else if (dbVerStr.equals("4.2"))	dbVer = DBVer.Ver42;
			else if (dbVerStr.equals("5.0"))	dbVer = DBVer.Ver50;
			else if (dbVerStr.equals("5.1"))	dbVer = DBVer.Ver51;
			else if (dbVerStr.equals("5.2"))	dbVer = DBVer.Ver52;
			else if (dbVerStr.equals("5.3"))	dbVer = DBVer.Ver53;
			else if (dbVerStr.equals("5.4"))	dbVer = DBVer.Ver54;
			else if (dbVerStr.equals("5.5"))	dbVer = DBVer.Ver55;
			else System.err.println("Unknown version string " + dbVerStr + " in schemver table");
		}
		catch (Exception e){}
	}
	/*******************************************************************
	 * SCHEMA -- contains all columns except dynamic
	 */
	
	public void loadSchema() throws Exception
	{
		try {
		ResultSet rs = mDB.executeQuery("show tables");
		if (rs.first())
		{
			ErrorReport.die("Cannot create database: tables already exist!");	
		}
		String sql = 
			"create table schemver ( " +
			"	schemver tinytext, " +
			"	annoVer tinytext, "	+
			"   annoDate tinytext " + 
			") " +
			"ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);	
		sql = "insert into schemver (schemver) values('" + currentVerString() + "'); ";
		mDB.executeUpdate(sql);
		
		 // assem_msg.peptide is added if protein databases
		 // assem_msg.hasLoc  if has location information (Library.java) or AddRemarkPanel
		 // assem_msg.goDE    if goseq has been run (QRProcess)
		sql = 
			"create table assem_msg ( " +
			"	AID integer NOT NULL PRIMARY KEY, " +	// obsolete, but everywhere (always 1)
			"	msg text, " +
			"   pja_msg text default null," +  // contains OVERVIEW
			"   meta_msg text default null," +     // contains test at bottom of overview
			"	spAnno boolean default false,"  +  // if SP takes precedence for Best Anno
			"	orf_msg text default null,"  +  // added in DoORF
			"	gc_msg text default null,"  +  //  added in DoORF
			"   go_msg text default null, "	+  //  name of goterm file, which contains date
			"   go_ec  text default null, " +  //  evidence codes in db
			"	go_slim tinytext default null,"  +  // either goDB subset 
			"   norm tinytext default null" +	// CAS304 RPKM or TPM
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
			
		sql = 
			"create table assembly  ( " +
			"	AID integer default 1, " + // obsolete
			"	assemblyid varchar(30) NOT NULL, " +
			"	 username varchar(30),  " +
			"	 descr tinytext, " +
			"	 assemblydate date NOT NULL, " +
			"	 projectpath tinytext, " +
			"	 annotationdate date, " +
			"	completed tinyint NOT NULL, " +
			"	completedate date, " +
			"	erate float default 0, " +
			"	exrate float default 0, " +
			"	ppx tinyint default 0, " + // Obsolete: used to be N value for 1EN for normalizing expression levels
			"	UNIQUE INDEX unq1(assemblyid) " +
			"	) ENGINE=MyISAM; " ;
		mDB.executeUpdate(sql);
		
		sql = 
			"create table library ( " +
			"	LID integer PRIMARY KEY AUTO_INCREMENT, " +
			"	libid varchar(30) NOT NULL, " +
			"   ctglib boolean default 0, " +
			"   parent varchar(30), " +
			"   reps TEXT," +
			"	libsize int NOT NULL, " +
			"   fastafile varchar(250) NOT NULL, " +
			
			"	 title tinytext,  " +
			"	 organism tinytext, " +
			"	 cultivar tinytext, " +
			"	 strain tinytext, " +
			"	 tissue tinytext, " +
			"	 stage tinytext,  " +
			"	 treatment tinytext,  " +
			"	 year tinytext,  " +
			"	 source tinytext, " +
			"	 sourcelink tinytext, " +
			"	 loaddate timestamp NOT NULL, " +
			"	 expdate datetime, " +
			"	 exploaded boolean default 0, " + /* these are also added in LoadLibMain.fixExpCols */
			"	 defqual tinytext, " +
			
			"	 avglen int default 0, " +
			"    medlen int default 0, " +
			"    prefix tinytext, " +				/* CAS304 obsolete (I think) */
			"	 orig_libid varchar(30), " +
			"	 fastqfile varchar(250) NOT NULL, " +
			"	 fiveprimesuf varchar(10) NOT NULL,  " +
			"	 threeprimesuf varchar(10) NOT NULL,  " +
			"	UNIQUE INDEX unq1(libid) " +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
		
		// Not a clone, its a read or EST
		sql = 
			"create table clone ( " +
			"	CID bigint NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
			"	cloneid varchar(30) NOT NULL, " +
			"	 origid varchar(30) NOT NULL, UNIQUE INDEX (origid),  " +
			"	 libid varchar(30) NOT NULL, " +
			"	 sequence MEDIUMTEXT NOT NULL, " + 
			"	 quality MEDIUMTEXT NOT NULL, " +  
			"	 source smallint UNSIGNED default 0, " +
			"	 sense tinyint NOT NULL, " +
			"	 length int NOT NULL, " +
			"     mate_CID bigint NOT NULL, " +
			"	LID integer NOT NULL, " +
			"	chimeric boolean not null default 0, " +
			" " +
			"	UNIQUE INDEX unq1(cloneid), /* we should remove this restriction!! needs jpave fix */ " +
			"	INDEX lid1(LID, cloneid), " +
			"	INDEX lid2(LID,CID) " +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);	
		
		sql = 
			"create table clone_exp ( " +
			"	CID bigint NOT NULL , " +
			"	LID integer NOT NULL, " +
			"	count integer NOT NULL, " +
			"	rep smallint unsigned default 0, " +
			"	UNIQUE INDEX unq1(CID,LID,rep) " +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);	
	
		sql = 
			"create table contclone ( " +
			"	CTGID bigint NOT NULL,  " +
			"	CID bigint NOT NULL, " +
			"	contigid varchar(30) NOT NULL, " +
			"	cloneid varchar(30) NOT NULL, " +
			"	orient char(1) NOT NULL, " +
			"	leftpos integer NOT NULL, " +
			"	gaps MEDIUMTEXT, " +
			"	extras MEDIUMTEXT, " +
			"	ngaps integer default 0, " +
			"   mismatch mediumint default 0, " + 
			"   numex mediumint default 0, " + 
			"	buried TINYINT default 0, " +
			"	prev_parent bigint default 0, " +
			"	pct_aligned smallint default 0,  " +
			"	 INDEX idx1(contigid,cloneid), " +
			"	 PRIMARY KEY prim(CTGID,CID) " +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
		
		// sequence table (contig is the same as unitran below).
		sql = 
			"create table contig ( " +
			"	CTGID bigint NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
			"	contigid varchar(30) NOT NULL, " +
			"	consensus_bases INT, " +
			"	numclones 	INT NOT NULL, " +
			"   totalexp bigint unsigned default 0," +
			"	longest_clone VARCHAR(30), " +			// instantiate when Skip; else annotate
		
			/* UniProt hits   */
			"	bestmatchid VARCHAR(30), " +
			"   PID bigint default 0, " +  		// best eval
			"   PIDov bigint default 0, " +  	// best anno
			"	PIDgo bigint default 0, " + 		// best with GO
			"   cnt_swiss	  smallint  unsigned default 0, " +
			"   cnt_trembl    smallint  unsigned default 0, " +
			"   cnt_nt        smallint unsigned  default 0, " +
			"   cnt_gi        smallint unsigned  default 0, " + 
			"   cnt_overlap   SMALLINT  unsigned default 0, " +
			"   cnt_species   SMALLINT  unsigned default 0, " +
			"   cnt_gene	  SMALLINT  unsigned default 0, " +
			"   cnt_annodb	  SMALLINT  unsigned default 0, " + 
			"   cnt_pairwise  smallint  unsigned default 0, " +
			
			/* Largest ORF coding information  */
			"	o_frame 				TINYINT default null, " +
			"   o_markov				float, "    + 	            // db5.4
			"	o_coding_start 		MEDIUMINT unsigned default null, " +
			"	o_coding_end   		MEDIUMINT unsigned default null, " +
			"	o_len 				MEDIUMINT unsigned default null, " + 
			"	o_coding_has_begin 	BOOLEAN, " +
			"	o_coding_has_end 	BOOLEAN, " +
			"	p_frame 				TINYINT   default null, " +  // hit (protein) frame
			"	p_eq_o_frame 		BOOLEAN, " +   				 // ORF frame equals hit frame
			
			"	notes VARCHAR(255), " +
			"	user_notes VARCHAR(255), " +	
			
			/* Sequence locations on chromosomes*/
			"   seq_group VARCHAR(30), " +   // e.g. scaffold_1
			"   seq_start bigint default 0, " + 
			"   seq_end  bigint  default 0, " +
			"   seq_strand tinytext, " +
			
			/* SNP statistics  */
			"	snp_count SMALLINT, " +
			"	indel_count SMALLINT, " +
			
			//other
			"	rstat 		FLOAT default 0, " +		// instantiate
			"	gc_ratio FLOAT, " +						// annotate
			"	cnt_ns smallint default 0," +
			"   totalexpN bigint unsigned default 0," +	// CAS304 never used; transferred to Multi, but not used there either
			
			// sequence info. Need to get rid of assemblid and orig_ccs, and single quality if all the same
			"	consensus MEDIUMTEXT NOT NULL, " + // holds 2**24
			"	quality MEDIUMTEXT NOT NULL, " +
			"	orig_ccs MEDIUMtext, " +  /* this gives a way to fix the contigs, in case doSNP has a bug and messes them up */
			"	assemblyid varchar(30) NOT NULL, " +
			
			// assembled
			"	 orient 		char(1), " +
			"	 frpairs 	int NOT NULL, " +
			"	 has_ns 	BOOLEAN default 0, " +
			"	 nstart 	integer default 0, " +
			"	 est_5_prime SMALLINT, " +
			"	 est_3_prime SMALLINT, " +
			"	 est_loners  SMALLINT, " +
			
			"	 recap BOOLEAN default 0,  " +
			"	 mate_CTGID bigint default 0, " +
			"	 rftype tinytext, " +
			"	 buried_placed boolean default 0, " +
			
			// for assembly
			"	AID integer NOT NULL, " +		// obsolete, but everywhere
			"	avg_pct_aligned smallint default 0, " +
			"	finalized boolean default 0, " +
			"	TCID integer default 0, " +  // for assembly
			"	SCTGID bigint default 0, " + // for assembly

			"	INDEX idx1(contigid), " +
			"	INDEX idx2(assemblyid), " +
			"	INDEX idx6(bestmatchid), " +   // for updating description
			"	INDEX idx4(tcid), " +
			"	INDEX idx5(sctgid) " +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
	
		sql = 
			"create table contig_counts (" +
			"	contigid varchar(30) NOT NULL, " +
			"	libid varchar(30) NOT NULL, " +
			"	 count int NOT NULL, " +
			"	 PRIMARY KEY (contigid, libid) " +
			" ) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
		
		sql = 
			"create table pja_databases (" +
			"DBID 			integer not null PRIMARY KEY AUTO_INCREMENT, " +
			"AID 			integer not null, " +
			"path 			varchar(250), " +
			"dbtype 		varchar(10), " + // tr, sp, nt, Ref90....
			"taxonomy 		varchar(20), " + // plants, invert, vert,...
			"isProtein 		boolean, " +
			"nOnlyDB		integer  unsigned default 0,"  +   // #contigs only hits
			"nBestHits 		integer unsigned default 0, " +     // #contigs best Eval
			"nOVBestHits 	integer  unsigned default 0, " +  // #contigs best Anno
			"nTotalHits 	integer  unsigned default 0, " +   // #contigs with hit
			"nUniqueHits 	integer  unsigned default 0, " +  // #proteins/nt unique
			"dbDate 		date, " +
			"addDate 		date, " +
			"subset			boolean default false, " + // v2.10 not used 
			"parameters 	text,  " +							// db4.1
			"INDEX(DBID) " +
			") ENGINE=MyISAM; ";
			mDB.executeUpdate(sql);
		
		// unitrans=contig=sequence; should be seq_hits	
		sql = 
		"create table pja_db_unitrans_hits " +
		"	(PID 			bigint not null PRIMARY KEY AUTO_INCREMENT, " +
		"	 CTGID 			bigint not null, " +
		"	 AID 			integer not null, " +     // db3.0
		"	 DUHID 			integer not null, " +	  // db3.0 unique hit index
		"	 contigid 		varchar(30) NOT NULL, " +
		"	 uniprot_id 	varchar(30) NOT NULL, " +  // hitID  ...
		"	 dbtype			varchar(10), " + 
		"	 taxonomy 		varchar(20)," +			 
		// search (e.g. blast) fields
		" 	 percent_id		SMALLINT  unsigned, " +
		"	 alignment_len	INTEGER  unsigned, " +		
		"	 mismatches		SMALLINT  unsigned, " +
		"	 gap_open		SMALLINT  unsigned, " +
		"	 ctg_start		MEDIUMINT unsigned, " +			// seq_start
		"	 ctg_end		MEDIUMINT unsigned, " +			// seq_end
		"    ctg_cov        smallint unsigned default 0, " +  // db5.4 percent sequence coverage
		"	 prot_start 	MEDIUMINT unsigned, " +			// hit_start
		"	 prot_end   	MEDIUMINT unsigned, " +			// hit_end
		"    prot_cov       smallint unsigned default 0, " +  // db5.4 percent hit coverage
		"	 e_value 		DOUBLE PRECISION, " +			
		"	 bit_score 		float, " +
		// computed fields
		"	 blast_rank 	smallint unsigned default 0, " +
															     // CAS303 rank failed in v8; changed to best_rank
		"	 best_rank 			smallint unsigned default 0, " + // filter_best + filter_ovbest + filter_gobest
		"	 isProtein 		boolean default 0, " +			
		"    filtered		tinyint  unsigned default 0, " +	// bit sting of following filters	
		"	 filter_best  	boolean default 0, " +           // bestEval
		"	 filter_ovbest	boolean default 0, " +           // bestAnno
		"	 filter_gobest	boolean default 0, " +           // best with GO
		"	 filter_olap	boolean default 0, " +  
		"	 filter_top3	boolean default 0, " +
		"	 filter_species	boolean default 0, " +
		"	 filter_gene	boolean default 0, " +
		"    INDEX(filter_best), " +
		"    INDEX(filter_ovbest), " +
		"    INDEX(uniprot_id), " +
		"    INDEX(contigid), " +
		"    INDEX(DUHID), " +  
		"    INDEX(CTGID) " +
		"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
	
		sql = 
		"create table pja_db_unique_hits (" +
		"DUHID 			integer not null PRIMARY KEY AUTO_INCREMENT, " +
		"AID 			integer not null, " +
		"DBID 			integer not null, " +  	// database this came from
		"hitID 			varchar (30) UNIQUE, " + // UniProtID or ntID 
		"repID			varchar (30), " +        // second id in Uniprot 			
		"dbtype 			varchar (10), " +
		"taxonomy 		varchar(20), " + 		
		"isProtein 		boolean default 0, " +
		"nUnitranHit    smallint unsigned default 0, " +  // nSeqHit		
		"nBestHit       smallint unsigned  default 0, " +  	
		"description 	varchar(250), " +
		"species 		varchar (100), " +
		"length			integer default 0, " +  
		"sequence 		MEDIUMTEXT, " +
		"goBrief			text, " + // #GOs followed by the first 4
		"goList			text, " + // this can be quite long
		"interpro		text, " +
		"kegg	 		text, " +
		"pfam 			text, " +
		"ec 				text, " +
		"INDEX(hitID), " +
		// loading - the following caused virtually no slow down on Linux, and tried a small set on Mac with no slow
		// generally speeds up Basic Hit search, but not always
		"INDEX(dbtype)," + 		
		"INDEX(taxonomy)," + 	
		"INDEX(description)," + 
		"INDEX(species)" +      
		"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
	
		sql = 
		"create table pja_db_species (" +
		"AID 			bigint not null, " +
		"species 		varchar (100), " +     		
		"count    		int unsigned default 0, " +  // total count
		"nBestHits     int unsigned default 0, " +   // best eval  
		"nOVBestHits     int unsigned default 0, " + // best anno
		"index(species) ) ENGINE=MyISAM;";
		mDB.executeUpdate(sql);
		
		// results for Pairs table
		sql = 
		"create table pja_pairwise (" +
			"PWID 		integer not null PRIMARY KEY AUTO_INCREMENT, " +
			"AID 		integer not null, " +				
			"contig1 	varchar(30) NOT NULL, " +
			"contig2 	varchar(30) NOT NULL, " +
			" " +
			"coding_frame1 TINYINT, " +
			"coding_frame2 TINYINT, " +
			" " +	
			"NT_olp_ratio	Float default 0, " +				// len_of_olap/longest_ctgLen
			"NT_olp_score	int  unsigned default 0, " +		// #match 	sim=(score/len)*100.0
			"NT_olp_len		int  unsigned default 0, " +		// length of overlap
			" " +				
			"AA_olp_ratio	Float default 0, " +
			"AA_olp_score	int  unsigned default 0, " +
			"AA_olp_len		int  unsigned default 0, " +
			" " +		
			"in_self_blast_set 		BOOLEAN default 0, " +
			"in_uniprot_set 		BOOLEAN default 0, " +
			"in_translated_self_blast BOOLEAN default 0, " +
			// if have protein results
			" shared_hitID			varchar(30), " +
			// selfblast result
			" e_value 				DOUBLE PRECISION, " +			
			" percent_id			SMALLINT  unsigned default 0, " +
			" alignment_len			INTEGER  unsigned default 0, " +
			" ctg1_start			SMALLINT default 0, " +
			" ctg1_end				SMALLINT default 0, " +
			" ctg2_start 			SMALLINT default 0, " +
			" ctg2_end				SMALLINT default 0  " +
			" ) ENGINE=MyISAM; ";
			mDB.executeUpdate(sql);

		// The following tables are used for assembling reads
		sql = 
			"create table snp ( " +
				"	SNPID bigint not null PRIMARY KEY AUTO_INCREMENT, " +
				"	CTGID bigint not null, " +
				"	pos integer not null, " +
				"	basevars tinytext, " +
				"	annot text, " +
				"	numvars smallint, " +
				"	snptype tinytext, " +
				"	score float default 0, " +
				"	INDEX(CTGID) " +
				") ENGINE=MyISAM; " ;
			mDB.executeUpdate(sql);
			
		sql = 
			"create table snp_clone ( " +
			"	SNPID bigint not null, " +
			"	CID bigint not null, " +
			"	snptype tinytext, " +
			"	UNIQUE INDEX(SNPID, CID), " +
			"	INDEX(CID) " +
			") ENGINE=MyISAM; " ;
		mDB.executeUpdate(sql);
				
		sql = 
			"create table assemlib ( " +
			"	AID integer NOT NULL, " +
			"	LID integer NOT NULL, " +
			"	assemblyid varchar(30) NOT NULL, " +
			"	 libid varchar(30) NOT NULL, " +
			"	singletons int default 0, " + 
			"   contigs int default 0,  " +
			"   uniqueContigs int default 0, " +
			"	goreads bigint default 0, " +
			"   UNIQUE INDEX (assemblyid,LID)," +
			"   UNIQUE INDEX (AID,LID)" +
			"	) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
			
		sql = 
			"create table ASM_assemtime ( " +
			"	AID integer not null, " +
			"	stage tinytext, " +
			"	time_start datetime " +
			") ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
			
		sql = 
			"create table ASM_params ( " +
			"	AID integer not null, " +
			"	pname tinytext, " +
			"	pvalue text " +
			") ENGINE=MyISAM; " ;
		mDB.executeUpdate(sql);
		sql = 
			"create table ASM_cmdlist ( " +
			"	AID integer not null, " +
			"	descr VARCHAR(100), " +
			"	cmdstr text, " +
			"	UNIQUE INDEX unq(AID,descr) " +
			") ENGINE=MyISAM; " ;
		mDB.executeUpdate(sql);
		
		// the rest of these tables are removed after assembly
		sql =
			"create table ASM_tc_iter ( " +
			"	TCID integer PRIMARY KEY AUTO_INCREMENT, " +
			"	AID integer NOT NULL, " +
			"	tctype tinytext NOT NULL, " +
			"	tcnum integer, " +
			"	finished tinyint default 0, " +
			"	clustiter_done smallint default 0, " +
			"	ctgs_start integer default 0, " +
			"	ctgs_end integer default 0, " +
			"	merges_tried integer default 0, " +
			"	merges_ok integer default 0 " +
			") ENGINE=INNODB; " ;
		mDB.executeUpdate(sql);
		
		// WN if you change buryclone you need to change reBuryClones in SNPMain.
		sql =
			"create table buryclone ( " +
			"	AID integer NOT NULL, " +
			"	CID_child bigint NOT NULL, INDEX(CID_child), " +
			"	CID_parent bigint NOT NULL, INDEX(CID_parent), " +
			"	CID_topparent bigint default 0, INDEX(CID_topparent), " +
			"	assemblyid varchar(30) NOT NULL,   " +
			"	 childid varchar(30) NOT NULL, INDEX(childid),   " +
			"	 parentid varchar(30) NOT NULL, INDEX(parentid),  " +
			"	bcode tinytext,	" +		/* -- either cap or blast */
			"	flipped tinyint(0), " +
			"	 PRIMARY KEY prim(AID, CID_child), " +
			"	 INDEX idx1(assemblyid, childid), " +
			"	 TCID integer default 0 " + /*	-- to permit rollback of TC stages  */
			" ) ENGINE=MyISAM; ";
		mDB.executeUpdate(sql);
		
		sql = 
			"create table ASM_cliques ( " +
			"	AID integer NOT NULL, " +
			"	CQID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	type tinytext, " +
			"	nclone integer default 0, " +
			"	assembled tinyint default 0, " +
			"	cap_success tinyint default 0 " +
			") ENGINE=INNODB; ";
		mDB.executeUpdate(sql);
		
		sql = 
			"create table ASM_clique_clone ( " +
			"	CQID bigint, " +
			"	CID bigint, " +
			"	PRIMARY KEY(CQID,CID), " +
			"	FOREIGN KEY (CQID) REFERENCES ASM_cliques(CQID) ON DELETE CASCADE  " +
			") ENGINE=INNODB; ";
		mDB.executeUpdate(sql);
			
		sql = 
			"create table ASM_scontig ( " +
			"	SCID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	AID integer NOT NULL, " +
			"	CTID1 bigint not null, " +
			"	CTID2 bigint default 0, " +
			"	TCID integer not null, " +
			"	merged_to integer default 0, " +
			"	capbury_done tinyint default 0, " +
			"	UNIQUE INDEX unq1(CTID1,CTID2), " +
			"	INDEX idx1(TCID), " +
			"	FOREIGN KEY (TCID) REFERENCES ASM_tc_iter(TCID) ON DELETE CASCADE " +
			") ENGINE=INNODB; ";
		mDB.executeUpdate(sql);
		
		sql = 
			"create table ASM_tc_edge ( " +
			"	EID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	TCID integer, " +
			"	SCID1 bigint, " +
			"	SCID2 bigint, " +
			"	score integer, " +
			"	CLUSTID bigint default 0, " +
			"	attempted tinyint default 0, " +
			"	succeeded tinyint default 0, " +
			"	errstr tinytext, " +
			"	errinfo tinytext, " +
			"	SCID_result bigint default 0, " +
			"	EID_from bigint default 0, " +
			"	olap smallint default 0, " +
			"	identity smallint default 0, " +
			"	hang smallint default 0, " +
			" " +
			"	UNIQUE INDEX unq1(TCID,SCID1,SCID2), " +
			"	INDEX scodx(TCID,score), " +
			"	INDEX att(TCID,attempted), " +
			"	 FOREIGN KEY (TCID) REFERENCES ASM_tc_iter(TCID) ON DELETE CASCADE , " +
			"	 FOREIGN KEY (SCID1) REFERENCES ASM_scontig(SCID) ON DELETE CASCADE , " +
			"	 FOREIGN KEY (SCID2) REFERENCES ASM_scontig(SCID) ON DELETE CASCADE  " +
			") ENGINE=INNODB; ";
		mDB.executeUpdate(sql);

		addRstat();
		}
		catch (Exception e) {ErrorReport.die(e, "Creating schema");}
	}
	
	private void addRstat() throws Exception
	{
		ResultSet rs = mDB.executeQuery("show tables like 'rstat_thresh'");
		if (!rs.first())
		{
			String sql = 
				"create table rstat_thresh ( " +
				"	assemblyid varchar(30), " +
				"	libid1 varchar(30), " +
				"	libid2 varchar(30), " +
				"	rstat int not null, " +
				"	rstype tinytext, " +
				"	simcount float not null, " +
				"	obscount int not null, " +
				"	conf float not null, " +
				"	INDEX(assemblyid, libid1, libid2) " +
				") ENGINE=MyISAM; " ;
			mDB.executeUpdate(sql);		
		}
	}
	/********************************************************************
	 * GO sql tables -- may be rerunning, so drop and readd
	 */
	public static void createGOtables (DBConn db) {
		try {
			if (db.tableExists("go_info")) {
				db.executeUpdate("drop table go_info");
				Out.PrtSpMsg(1, "Clearing database GO tables");
				Out.PrtWarn("Any GOseq values will be removed. You will need to re-execute GOseq");
			}
			else Out.PrtSpMsg(1, "Create database GO tables");
			db.tableCheckAddColumn("assem_msg", "go_slim", "tinytext", "");
			db.tableCheckAddColumn("assem_msg", "isa", "tinyint default 1", "");
			db.tableCheckAddColumn("assem_msg", "partof", "tinyint default 25", "");
			db.tableCheckAddColumn("pja_db_unitrans_hits", "filter_gobest", "boolean default 0", "filter_ovbest");
			
			// entry for every direct and ancestor
			String [] ecList = new MetaData().getEClist();
			String ecStr = "";
			for (int i=0; i<ecList.length; i++)  ecStr += ecList[i] + " boolean default 0, ";
			
			db.executeUpdate(
			"create table go_info (" +
					" gonum int, " +
					" descr tinytext, " +
					" term_type enum('biological_process','cellular_component','molecular_function'), " +
					" level smallint default 0, " + 
					" bestEval double, " + // new 4.0
					" nUnitranHit int unsigned default 0, " +
					" slim boolean default 0, " +   
					ecStr +
					" unique(gonum), " +
					" index(gonum) " +
					") ENGINE=MyISAM;");
			
			// Same info as in pja_db_unique_hits.goList, i.e. direct assignments
			if (db.tableExists("pja_uniprot_go")) db.executeUpdate("drop table pja_uniprot_go");
			db.executeUpdate(
				"create table pja_uniprot_go (" +
					" DUHID bigint, " +
					" gonum bigint, " +
					" EC varchar(3) default ''," +
					" index(gonum), " +
					" index(DUHID)" +
				    ") ENGINE=MyISAM;");
			
			// entry for each sequence-GO entry for all GOs found in set of hits for sequence
			if (db.tableExists("pja_unitrans_go")) db.executeUpdate("drop table pja_unitrans_go");
			db.executeUpdate(
				"create table pja_unitrans_go (" +
					" CTGID bigint, " +
					" gonum int, " +
					// the next 5 go together
					" bestDUH int, " +
					" bestEval double, " +         
					" bestEV boolean default 0, " + 
					" bestAN boolean default 0, " +
					" EC varchar(5) default ''," +   // if in parenthesis, inherited
					
					" direct boolean default 0, " + // any hit_GO could be direct whereas EC is inherited
					" slim boolean default 0, " +   
					" unique(CTGID,gonum), " +
					" index(CTGID)," +             
					" index(gonum) " +
					" ) ENGINE=MyISAM;");
			
			// used in BasicGO to build trimmed set
			if (db.tableExists("pja_gotree")) db.executeUpdate("drop table pja_gotree");
			db.executeUpdate(
					"create table pja_gotree (" +
					" tblidx int unsigned primary key auto_increment, " +
	    				" gonum int unsigned, " +
	    				" level smallint unsigned, " +
	    				" name tinytext, " +
	    				" nTotalReads bigint unsigned default 0," + 
	    				" term_type enum('biological_process','cellular_component','molecular_function')," +
	    				" index(gonum) " +
	    				" ) ENGINE=MyISAM;");
			
			// this is used in GOtree.java
			// if distance=1, then immediate parent so in term2term
			// GO database: graph_path table except use child/ancestor instead of their term1_id and term2_id
			if (db.tableExists("go_graph_path")) db.executeUpdate("drop table go_graph_path");
			db.executeUpdate(
					"create table go_graph_path (" +
					" relationship_type_id tinyint unsigned, " +
					" distance smallint unsigned, " + //e.g. if A part_of B is_a C part_of D, then distance=3 for A part_of D 
					" relation_distance smallint unsigned, " + // e.g. if A part_of B is_a C part_of D, then relation_distance=2 for A part_of D 
	    				" child int unsigned, " +
	    				" ancestor int unsigned, " +
	    				" index(child), " +
	    				" index(ancestor)" +
	    				" ) ENGINE=MyISAM;");
			
			// this is used in GOtree.java 
			if (db.tableExists("go_term2term")) db.executeUpdate("drop table go_term2term");
			db.executeUpdate(
				"create table go_term2term (" +
					" relationship_type_id tinyint unsigned, " +
					" child int unsigned, " +
	    			" parent int unsigned, " +
	    			" index(child), " +
	    			" index(parent)" +
	    			" ) ENGINE=MyISAM;");
		}
		catch(Exception e){ErrorReport.die(e, "Error adding GO tables to schema");}
	}
	/***************************************************************
	 * XXX Version methods
	 */
	public boolean current()
	{
		return (dbVer == currentVer());	
	}
	
	public String update() throws Exception
	{
		if (current()) return dbVerStr;
		
		System.err.println("Database schema is " + dbVerStr + "; current schema is " + curVerStr);
		
		if (!FileHelpers.yesNo("The database needs to be updated; continue?"))
		{
			if (FileHelpers.yesNo("Terminate (y) or try to continue (n)?")) {
				System.err.println("Terminated");
				System.exit(0);
			}
			else {
				System.err.println("Schema is version " + dbVerStr + ", expecting version " + curVerStr );
				System.err.println("Run TCW anyway -- warning, may not work");
				return dbVerStr;
			}
		}
		if (dbVer==DBVer.Ver35) updateTo40();
		if (dbVer==DBVer.Ver40) updateTo41();
		if (dbVer==DBVer.Ver41) updateTo42();
		if (dbVer==DBVer.Ver42) updateTo50();
		if (dbVer==DBVer.Ver50) updateTo51();
		if (dbVer==DBVer.Ver51) updateTo52();
		if (dbVer==DBVer.Ver52) updateTo53();
		if (dbVer==DBVer.Ver53) updateTo54();
		if (dbVer==DBVer.Ver54) updateTo55();
		return dbVerStr;
	}
	/*************************************************
	 * May2020 v3.0.3 change for MySQL v8
	 */
	private void updateTo55() {
		try {
			Out.Print("Update to sTCW schema db5.5 for " + Version.sTCWhead);
			
			mDB.tableCheckRenameColumn("pja_db_unitrans_hits", "rank",  " best_rank", "smallint unsigned");
			
			mDB.executeUpdate("update schemver set schemver='5.5'");
			dbVer = DBVer.Ver55;
			dbVerStr = curVerStr;
			Out.Print("Finish update for sTCW Schema db5.5");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update schema to v5.5");}	
	}
	/*************************************************
	 * 4/11/19 V2.14
	 */
	private void updateTo54() {
		try {
			Out.Print("Update to sTCW schema db5.4");
			Out.r("update 1 of 5");
			mDB.tableCheckAddColumn("contig", "o_markov", "float default 0", "o_coding_end");
			Out.r("update 2 of 5");
			mDB.tableCheckRenameColumn("contig", "p_coding_end",       "o_len", "mediumint");
			Out.r("update 3 of 5");
			mDB.tableCheckRenameColumn("contig", "p_coding_has_begin", "p_eq_o_frame", "boolean");
			Out.r("update 4 of 5");
			mDB.tableCheckAddColumn("pja_db_unitrans_hits", "ctg_cov", "smallint unsigned default 0", "ctg_end");
			Out.r("update 5 of 5");
			mDB.tableCheckAddColumn("pja_db_unitrans_hits", "prot_cov","smallint unsigned default 0", "prot_end");
			
			mDB.executeUpdate("update schemver set schemver='5.4'");
			dbVer = DBVer.Ver54;
			dbVerStr = "5.4";
			Out.Print("Finish update for sTCW Schema db5.4");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update schema to v5.4");}	
	}
	/**************************************************
	 * 10/15/18 
	 */
	private void updateTo53() {
		try {
			Out.Print("Update to sTCW schema db5.3");
			mDB.tableCheckAddColumn("contig", "user_notes", "VARCHAR(255)", "notes");
			mDB.tableCheckAddColumn("assem_msg", "spAnno", "boolean default false", null);
			mDB.tableCheckAddColumn("assem_msg", "go_ec", "text default null", null);
			mDB.executeUpdate("update schemver set schemver='5.3'");
			dbVer = DBVer.Ver53;
			dbVerStr = "5.3";
			Out.Print("Finish update for sTCW Schema db5.3");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update schema to v5.3");}	
	}
	/**************************************************
	 * 10/15/18 
	 */
	private void updateTo52() {
		try {
			Out.Print("Update to schema db5.2");
			mDB.tableCheckAddColumn("contig", "user_notes", "VARCHAR(255)", "notes");
			mDB.executeUpdate("update schemver set schemver='5.2'");
			dbVer = DBVer.Ver52;
			dbVerStr = "5.2";
			Out.Print("Finish update for sTCW Schema db5.2");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update schema to v5.2");}	
	}
	/**************************************************
	 * 10/9/18 
	 */
	private void updateTo51() {
		try {
			Out.Print("Update to schema db5.1");
			Out.Print("   Rename column....");
			mDB.tableCheckRenameColumn("contig", "cnt_taxo", "cnt_annodb", "smallint");
			Out.Print("   Compute number of annoDBs ....");
			
			int maxSeq = mDB.executeCount("SELECT COUNT(*) FROM contig")+1;
	     	int maxIdx = mDB.executeCount("SELECT max(CTGID) FROM contig")+1;
	     	if (maxSeq!=maxIdx) 
	     		Out.PrtError("TCW error: #seqs != highest index -- could cause incorrect results ");
	     	
	     	for (int i=1; i< maxSeq; i++) {
	     		int cnt = mDB.executeCount("SELECT COUNT(DISTINCT dbtype, taxonomy) " +
	     				"from pja_db_unitrans_hits where CTGID=" + i);
	     		mDB.executeUpdate("UPDATE contig set cnt_annodb=" + cnt + " where CTGID=" + i);
	     		if (i%1000==0) Out.r(" Updated # " + i);
	     	}
	     	
			mDB.executeUpdate("update schemver set schemver='5.1'");
			dbVer = DBVer.Ver51;
			dbVerStr = "5.1";
			Out.Print("Finish update for sTCW Schema db5.1");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update schema to v5.1");}	
	}
	/**************************************************
	 * March 2016 update without schema version change
	 * 8 March 16 release, was updateV141
	 */
	private void updateTo50() {
		try {
			System.out.println("Update to schema db5.0");
			// release 8 March 16
			System.out.println("   Adding columns....");
			mDB.tableCheckAddColumn("contig", "PIDgo", "integer", "");
			mDB.tableCheckAddColumn("assem_msg", "isa", "tinyint default 1", "");
			mDB.tableCheckAddColumn("assem_msg", "partof", "tinyint default 25", "");
			
			// release N March 16
			boolean add = mDB.tableCheckAddColumn("pja_db_unitrans_hits", "filter_gobest", 
									"boolean default 0", "filter_ovbest");
			if (add) { 
				int cnt = mDB.executeCount("select count(*) from contig where PIDgo>0");
				if (cnt>0) {
					System.err.println("   Update for sequence best hit with GO... ");
					mDB.executeUpdate("update pja_db_unitrans_hits, contig" +
						" set pja_db_unitrans_hits.filter_gobest=1 " +
						" where pja_db_unitrans_hits.PID=contig.PIDgo and contig.PIDgo>0");
				}
			}	
			// added rank to schema in 2015, but never added to  updateToVxx
			// CAS303 rank changed to best_rank in v303
			mDB.tableCheckAddColumn("pja_db_unitrans_hits", "best_rank", "tinyint default 0", "blast_rank");
			int cnt = mDB.executeCount("select count(*) from pja_db_unitrans_hits where best_rank>0");
			if (cnt==0) {
				System.err.println("Update for filter hits ");
				mDB.executeUpdate("update pja_db_unitrans_hits " +
						"set best_rank=(filter_best+filter_ovbest+filter_gobest)");
			}
			mDB.executeUpdate("update schemver set schemver='5.0'");
			dbVer = DBVer.Ver50;
			dbVerStr = "5.0";
			System.err.println("Finish update for V1.5 (Schema db5.0)");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update v1.5");}
	}
	
	/******************************************************
	 * update schema for database v42 and TCW v1.4
	 * this covers a number of releases that did not have schema version# updated
	 */
	private void updateTo42() {	
		System.err.println("Updating database for sTCW db1.4");
		updateTo42_V135();
		updateTo42_V137(); 
		updateTo42_V140(); 
		try {
			mDB.executeUpdate("update schemver set schemver='4.2'");
			dbVer = DBVer.Ver42;
			System.err.println("Complete schema update");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Updating schema version");}
	}
	// This does not add columns but just does some updates.
	// Want to use p_coding_end for the ORF_len; now does this for v135
	// Some consensus_bases are wrong if assembled. This always needs to be done as
	//    its easier than trying to fix it in the assembly code
	private void updateTo42_V135() { 
		try {
			ResultSet rs;
			
			// enter ORF lengths
			int orfRun = mDB.executeCount("Select count(*) from contig where o_coding_end>0");
			int orfUpdated = mDB.executeCount("Select count(*) from contig where p_coding_end>1");
			if (orfRun !=0 && orfUpdated == 0) {
				System.err.println("+++ Updating orf lengths");
				
				HashMap <Integer, Integer> orfMap = new HashMap <Integer, Integer> ();
				rs = mDB.executeQuery("Select CTGid, o_coding_start, o_coding_end from contig");
				while (rs.next()) {
					int ctgid = rs.getInt(1);
					int start = rs.getInt(2);
					int end = rs.getInt(3);
					if (start>0 && end>0) orfMap.put(ctgid, end-start+1);
				}
				if (orfMap.size()>0) {
					System.err.println("Updating database with ORF lengths: " + orfMap.size());
					for (int ctgid : orfMap.keySet()) {
						mDB.executeUpdate("update contig set p_coding_end=" + orfMap.get(ctgid) 
								+ " where CTGid=" + ctgid);
					}
				}
			}
			
			// correct contig lengths
			rs = mDB.executeQuery("Select erate from assembly");
			if (!rs.next()) return;
			double erate=rs.getDouble(1);
			if (erate==0.0) return;
			
			HashMap <Integer, Integer> seqLen = new HashMap <Integer, Integer> ();
			rs = mDB.executeQuery("Select ctgid, consensus, consensus_bases from contig");
			while (rs.next()) {
				int ctgid = rs.getInt(1);
				String seq = rs.getString(2);
				int len = rs.getInt(3);
				if (seq.length() != len) seqLen.put(ctgid, seq.length()); 
			}
			if (seqLen.size()==0) return;
			
			System.err.println("Update contig lengths: " + seqLen.size());
			for (int ctgid : seqLen.keySet())
				mDB.executeUpdate("Update contig set consensus_bases=" + seqLen.get(ctgid) + 
						" where CTGid=" + ctgid);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Fix contig lengths");}
	}
	private void updateTo42_V137() {
		try {

			if (mDB.tableColumnExists("assem_msg", "peptide")) return;
			if (!mDB.tableCheckAddColumn("contig", "cnt_ns", "smallint default 0", "")) return;
			System.err.println("+++ Adding count N's column");
			
			int cnt = mDB.executeCount("SELECT MAX(CTGID) FROM contig");
			int [] nCnt = new int [cnt+1];
			for (int i=0; i<=cnt; i++) nCnt[i]=0;
			ResultSet rs = mDB.executeQuery("select CTGID, consensus from contig");
			while (rs.next()) {
				int ctgid = rs.getInt(1);
				String seq = rs.getString(2).toLowerCase();
				int cntn=0;
				for (int i=0; i<seq.length(); i++)
					if (seq.charAt(i)=='n') cntn++;
				nCnt[ctgid] = cntn;
			}
			System.err.println(" Updating database...");
			for (int i=1; i<=cnt; i++) {
				if (nCnt[i]>0)
					mDB.executeUpdate("Update contig set cnt_ns=" + nCnt[i] + " where CTGID=" + i);
			}
			rs.close();
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update v1.37");}
	}
	private void updateTo42_V140() {
		try {
			System.err.println("Update message....");
			mDB.tableCheckAddColumn("assem_msg", "meta_msg", "text", "");
			mDB.tableCheckAddColumn("assem_msg", "go_msg", "text", "");

			if (!mDB.tableColumnExists("pja_db_unique_hits", "goList")) {
				System.err.println("Adding four columns for TCW v1.4 -- may take a minute");
				mDB.tableCheckAddColumn("pja_db_unique_hits", "goList", "text", "");
				System.err.println("Adding second one....");
				mDB.tableCheckAddColumn("pja_db_unique_hits", "goBrief", "text", "");
				System.err.println("Adding third one....");
				mDB.tableCheckAddColumn("pja_db_unique_hits", "interpro", "text", "");
				System.err.println("Adding fourth one....");
				mDB.tableCheckAddColumn("contig", "PIDgo", "bigint", "");
			}
			if (mDB.tableExists("go_info")) {
				System.err.println("The GO features will not work unless you do the following:");
				System.err.println("   execute newGOver.pl to update the local GO database");
				System.err.println("   execute 'Update GO' in runSingleTCW, or 'execAnno <db> -G");
				System.err.println("Removing GO tables...");
				mDB.executeUpdate("drop table go_info");
				mDB.executeUpdate("drop table pja_uniprot_go");
				mDB.executeUpdate("drop table pja_unitrans_go");
				mDB.executeUpdate("drop table pja_gotree");
			}
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error on update v1.40");}
	}
	
	
	private void updateTo40() throws Exception
	{
		if (!mDB.tableColumnExists("library", "reps"))
		{
			mDB.executeUpdate("alter table library add reps TEXT after parent");
		}
		if (!mDB.tableColumnExists("clone_exp", "rep"))
		{
			mDB.executeUpdate("alter table clone_exp add rep smallint unsigned default 0 after count");
			// Must add the new index before dropping old due to foreign keys
			mDB.executeUpdate("alter table clone_exp add index unq2(CID,LID,rep)");
			mDB.executeUpdate("alter table clone_exp drop index unq1");
		}
		mDB.executeUpdate("update library set reps='' where reps is null");
		
		if (!mDB.tableColumnExists("schemver", "annoVer")) {
			mDB.executeUpdate("alter table schemver add annoVer tinytext");
			mDB.executeUpdate("alter table schemver add annoDate tinytext");
		}	
		if (mDB.tableExists("pja_db_unique_hits"))
		{
			if (!mDB.tableColumnExists("pja_db_unique_hits", "kegg"))
			{
				mDB.executeUpdate("alter table pja_db_unique_hits add kegg mediumtext");
			}
			if (!mDB.tableColumnExists("pja_db_unique_hits", "pfam"))
			{
				mDB.executeUpdate("alter table pja_db_unique_hits add pfam mediumtext");
			}
			if (!mDB.tableColumnExists("pja_db_unique_hits", "ec"))
			{
				mDB.executeUpdate("alter table pja_db_unique_hits add ec mediumtext");
			}
		}
			
		mDB.executeUpdate("update schemver set schemver='4.0'");
	}
	/***********************************************************
	 * Feb 2015 
	 */
	private void updateTo41() throws Exception
	{
		System.err.println("\nUpdating columns for Feb15 4.1 release"); 
     	mDB.executeUpdate("update assem_msg set pja_msg = NULL where AID = 1");
     	System.err.println("   Delete species table to regenerate");
        mDB.executeUpdate("delete from pja_db_species");
        updateTo41giColumns();
        
        ResultSet rs = mDB.executeQuery("show columns from pja_databases where field='parameters'");
		if (!rs.next()) 
			mDB.executeUpdate("alter table pja_databases add parameters text");
		
		rs = mDB.executeQuery("show columns from pja_databases where field='nHighPercent'");
		if (!rs.next()) {
			mDB.executeUpdate("alter table pja_databases add nHighPercent int unsigned default 0");
			mDB.executeUpdate("alter table pja_databases add nLowPercent  int unsigned default 0");
		}

		rs = mDB.executeQuery("show columns from pja_databases where field='nOnlyDB'");
		if (!rs.first()) {
			mDB.executeUpdate("alter table pja_databases add nOnlyDB int unsigned default 0");
			mDB.executeUpdate("alter table pja_db_species add nBestHits  int unsigned default 0");
		}
        mDB.executeUpdate("update schemver set schemver='4.1'");
         
		Overview ov = new Overview(mDB);
		ov.createOverview(new Vector <String> ());
		
		return;
	}
	
	// This has nothing to do with Overview, but added in here for Feb15 release
	private void updateTo41giColumns() {
		try {
			ResultSet rs = mDB.executeQuery("show columns from contig where field='cnt_gi'");
			if (rs.next()) return;
			
			System.err.println("   Add GI count column and populate");
			mDB.executeUpdate("alter table contig add cnt_gi int unsigned default 0");
			rs = mDB.executeQuery("select count(*) from pja_db_unitrans_hits where dbtype='gi'");
			rs.next();
			int cnt = rs.getInt(1);
			if (cnt==0) return;
			
			rs = mDB.executeQuery("select count(*) from contig where cnt_gi>0");
			rs.next();
			cnt = rs.getInt(1);
			if (cnt==0) return; // updated already
			
			System.err.println("   Database has NCBI hits, updating #NCBI column for sequence.");
			HashMap <Integer, Integer> map = new HashMap <Integer, Integer> ();
			rs = mDB.executeQuery("select CTGID from pja_db_unitrans_hits where dbtype='gi'");
			while (rs.next()) {
				int ctg = rs.getInt(1);
				if (map.containsKey(ctg)) map.put(ctg, map.get(ctg)+1);
				else map.put(ctg, 1);
			}
			cnt=0;
			for (int ctgid : map.keySet()) {
				mDB.executeUpdate("update contig set cnt_gi=" + map.get(ctgid) + " where CTGID=" +  ctgid);
				cnt++;
			}
			System.err.println("   Update " + cnt + " sequences");
			
			rs = mDB.executeQuery("select count(*) from pja_db_unitrans_hits " +
					"where dbtype!='gi' and dbtype!='sp' and dbtype!='tr'");
			rs.next();
			cnt = rs.getInt(1);
			if (cnt==0) return;
			
			System.err.println("   Database has nt or other hits, updating #Nucleotide column for sequence.");
			mDB.executeUpdate("update contig set cnt_nt=0");
			map.clear();
			rs = mDB.executeQuery("select CTGID from pja_db_unitrans_hits " +
					"where dbtype!='gi' and dbtype!='sp' and dbtype!='tr'");
			while (rs.next()) {
				int ctg = rs.getInt(1);
				if (map.containsKey(ctg)) map.put(ctg, map.get(ctg)+1);
				else map.put(ctg, 1);
			}
			cnt=0;
			for (int ctgid : map.keySet()) {
				mDB.executeUpdate("update contig set cnt_nt=" + map.get(ctgid) + " where CTGID=" +  ctgid);
				cnt++;
			}
			System.err.println("   Update " + cnt + " sequences");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot add GI column");}
	}
	
	
	/********************************************************
	 * XXX Dynamic columns
	 * CAS304 removed assemlib, and reading libsize
	 */
	public static void addCtgLibFields(DBConn db) throws Exception
	{
		Vector <String> libName = new Vector <String> ();
		
		ResultSet rs = db.executeQuery("select libid from library"); 
				
		while (rs.next())
			libName.add(rs.getString(1));

		
		for (String col : libName) {
			Thread.sleep(1000);
			
			String column = Globals.LIBCNT + col;
			if (!db.tableColumnExists("contig", column))
				db.executeUpdate("alter table contig add " + column + " bigint unsigned default 0");
		
			column = Globals.LIBRPKM + col;
			if (!db.tableColumnExists("contig", column))
				db.executeUpdate("alter table contig add " + column + " float default 0.0");
			
			if (!db.tableColumnExists("contig", "totalexp")) {
				db.executeUpdate("alter table contig add totalexp bigint unsigned default 0");
				db.executeUpdate("alter table contig add totalexpN bigint unsigned default 0");
			}
		}
	}	
	
	/**********************************************************************
	 * 		XXX Assembly stuff
	 **********************************************************************/
	public void dropInnoDBTables() throws Exception
	{
		try
		{
			mDB.executeUpdate("drop table ASM_clique_clone");
			mDB.executeUpdate("drop table ASM_cliques");
			mDB.executeUpdate("drop table ASM_tc_edge");
			mDB.executeUpdate("drop table ASM_scontig");
			mDB.executeUpdate("drop table ASM_tc_iter");
		}
		catch(Exception e)
		{
			// If it's an upgrade we might not be able to drop them due to foreign keys - no big deal
		}
	}
	// Since we clean up the INNODB tables, this is needed to add them back if
	// an assembly is re-done.
	public void addASMTables() throws Exception
	{
		String sql;
		if (!mDB.tableExist("ASM_tc_iter"))
		{
			sql = 
			"create table ASM_tc_iter ( " +
			"	TCID integer PRIMARY KEY AUTO_INCREMENT, " +
			"	AID integer NOT NULL, " +
			"	tctype tinytext NOT NULL, " +
			"	tcnum integer, " +
			"	finished tinyint default 0, " +
			"	clustiter_done smallint default 0, " +
			"	ctgs_start integer default 0, " +
			"	ctgs_end integer default 0, " +
			"	merges_tried integer default 0, " +
			"	merges_ok integer default 0 " +
			") ENGINE=INNODB; " ;
			mDB.executeUpdate(sql);
		}
		if (!mDB.tableExist("ASM_cliques"))
		{
			sql = "" +
			"create table ASM_cliques " +
			"( " +
			"	AID integer NOT NULL, " +
			"	CQID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	type tinytext, " +
			"	nclone integer default 0, " +
			"	assembled tinyint default 0, " +
			"	cap_success tinyint default 0 " +
			" " +
			") " +
			"ENGINE=INNODB; ";
			mDB.executeUpdate(sql);
		}
		if (!mDB.tableExist("ASM_clique_clone"))
		{
			sql = "" +
			"create table ASM_clique_clone " +
			"( " +
			"	CQID bigint, " +
			"	CID bigint, " +
			"	PRIMARY KEY(CQID,CID), " +
			" " +
			"	FOREIGN KEY (CQID) REFERENCES ASM_cliques(CQID) ON DELETE CASCADE  " +
			") " +
			"ENGINE=INNODB; ";
			mDB.executeUpdate(sql);
		}
		/* ASM_scontig stands for "supercontig" 
		it holds either one or two actual assembled contigs 
		purpose is to track contig pairs through the assembly 
		it is these which are merged during the TC rounds  */
		if (!mDB.tableExist("ASM_scontig"))
		{
			sql = "" +
			"create table ASM_scontig " +
			"( " +
			"	SCID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	AID integer NOT NULL, " +
			"	CTID1 bigint not null, " +
			"	CTID2 bigint default 0, " +
			"	TCID integer not null, " +
			"	merged_to integer default 0, " +
			"	capbury_done tinyint default 0, " +
			" " +
			"	UNIQUE INDEX unq1(CTID1,CTID2), " +
			"	INDEX idx1(TCID), " +
			"	FOREIGN KEY (TCID) REFERENCES ASM_tc_iter(TCID) ON DELETE CASCADE " +
			") " +
			"ENGINE=INNODB; ";
			mDB.executeUpdate(sql);
		}
		if (!mDB.tableExist("ASM_tc_edge"))
		{
			sql = "" +
			"create table ASM_tc_edge " +
			"( " +
			"	EID bigint PRIMARY KEY AUTO_INCREMENT, " +
			"	TCID integer, " +
			"	SCID1 bigint, " +
			"	SCID2 bigint, " +
			"	score integer, " +
			"	CLUSTID bigint default 0, " +
			"	attempted tinyint default 0, " +
			"	succeeded tinyint default 0, " +
			"	errstr tinytext, " +
			"	errinfo tinytext, " +
			"	SCID_result bigint default 0, " +
			"	EID_from bigint default 0, " +
			"	olap smallint default 0, " +
			"	identity smallint default 0, " +
			"	hang smallint default 0, " +
			" " +
			"	UNIQUE INDEX unq1(TCID,SCID1,SCID2), " +
			"	INDEX scodx(TCID,score), " +
			"	INDEX att(TCID,attempted), " +
			"	 FOREIGN KEY (TCID) REFERENCES ASM_tc_iter(TCID) ON DELETE CASCADE , " +
			"	 FOREIGN KEY (SCID1) REFERENCES ASM_scontig(SCID) ON DELETE CASCADE , " +
			"	 FOREIGN KEY (SCID2) REFERENCES ASM_scontig(SCID) ON DELETE CASCADE  " +
			") " +
			"ENGINE=INNODB; ";
			mDB.executeUpdate(sql);
		}
	}
}
