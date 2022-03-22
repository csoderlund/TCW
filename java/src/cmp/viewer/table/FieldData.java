package cmp.viewer.table;

/***********************************************************
 * Defines all columns
 * They are rendered in SortTable (e.g. if you want to change how a double is printed)
 */
import java.util.Iterator;
import java.util.Vector;

import util.methods.ErrorReport;
import util.methods.Out;
import cmp.database.DBinfo;
import cmp.database.Globals;

public class FieldData {
	// table columns accessed elsewhere
	public static final String SEQID = "SeqID";
	public static final String CLUSTERID = "ClusterID";
	public static final String CLUSTERCOUNT = "Count";
	
	public static final String HITID = "HitID"; // TableUtil uses the fact that all 3 tables have this column header
	public static final String HITDESC = "Descript";
	
	public static final String PAIRID = "Pair #";
	public static final String NTLEN = "NT Len";
	public static final String AALEN = "AA Len";
	public static final String SEQID1 = "SeqID1";
	public static final String SEQID2 = "SeqID2";
	public static final String ROWNUM = "Row #";
	public static final String KaKs = "KaKs";
	public static final String SCORE1 = "Score1";
	public static final String SCORE2 = "Score2";
	
	//Main table for the different query modes - MYSQL and column table names.
	public static final String GRP_TABLE =    "pog_groups";
	public static final String MEMBER_TABLE = "pog_members";
	public static final String SEQ_TABLE =    "unitrans";
	public static final String PAIR_TABLE =   "pairwise";
	public static final String HIT_TABLE =    "unique_hits";
	public static final String SEQ_HIT_TABLE = "unitrans_hits"; // hard-coded everywhere
		
	// the hidden column name (and real) to access the SQL index field
	public static final String SEQ_SQLID = "UTid";  // unitrans.UTid
	public static final String GRP_SQLID = "PGid"; // POG_groups.POGid
	public static final String ID1_SQLID = "UTid1"; // pairwise.UTid1
	public static final String ID2_SQLID = "UTid2"; // pairwise.UTid2
	public static final String HIT_SQLID = "HITid"; // unique_hits.HITid
	public static final String AASEQ_SQL = "aaSeq"; // unitrans.aaSeq not used in this file, but since all MySQL is here...
	public static final String NTSEQ_SQL = "ntSeq"; // unitrans.ntSeq not used in this file, but since all MySQL is here...
	
	private static String msaScore1="", msaScore2="";
	private static DBinfo theInfo;
	
	public static boolean hasNTdb=false, hasGO=false, hasPCC=false, hasKaKs=false;
	public static boolean hasStats=false, hasNTblast=false, hasMultiScore=false, hasOrig=false;
	
	public static void setState(DBinfo info) {
		theInfo = info;
		hasNTdb=theInfo.nNTdb()>1;
		hasGO=theInfo.hasGOs(); // this will be zero if 'Add GOs' has not been run from runMulti
		hasPCC=theInfo.hasPCC();
		hasStats = theInfo.hasStats();
		hasNTblast = theInfo.hasNTblast();
		hasMultiScore = theInfo.hasMultiScore();
		hasKaKs = theInfo.hasKaKs();
		msaScore1 = theInfo.getMSA_Score1();
		msaScore2 = theInfo.getMSA_Score2();
		hasOrig = theInfo.hasOrig();
		
		if (theInfo.nNTdb()==0) System.out.println("   This is an AA-mTCW.");
	}
	/*********************************************************************************
	 * XXX Build cluster columns
	 */
	private static String [] GRP_SECTIONS;
	private static  String [] GRP_COLUMNS, GRP_SQLCOL, GRP_SQLTAB, GRP_DESCRIP ; 
	public  static  boolean [] GRP_DEFAULT;
	private static  Class<?>  [] GRP_TYPE;
	private static int [] GRP_SECTION_IDX, GRP_SECTION_BREAK;
	
	private static void buildGrp() {
		int sec = (hasMultiScore) ? 4 : 3;
		
		GRP_SECTIONS = new String [sec];
		if (hasMultiScore) {
			GRP_SECTIONS[0] = "General";
			GRP_SECTIONS[1] = "Multi-align"; // CAS310
			GRP_SECTIONS[2] = "Majority Best Anno Hit";
			GRP_SECTIONS[3] = "Dataset counts";
		}
		else {
			GRP_SECTIONS[0] = "General";
			GRP_SECTIONS[1] = "Majority Best Anno Hit";
			GRP_SECTIONS[2] = "Dataset counts";
		}
		
		Vector <Integer> mkBreak = new Vector <Integer> ();
		Vector <Integer> mkSection = new Vector <Integer> ();
		int nCol = 12;
		if (hasPCC) nCol+=2;
		if (hasMultiScore) nCol+=4;
		if (hasGO) nCol+=2;
		
		GRP_COLUMNS = new String [nCol];
		GRP_SQLCOL =  new String [nCol];
		GRP_SQLTAB =  new String [nCol];
		GRP_DESCRIP = new String [nCol];
		GRP_TYPE =    new Class<?> [nCol];
		GRP_DEFAULT = new boolean [nCol];
			 
		int c=0;
		addGrp(c++, ROWNUM, Integer.class, null, null, "Row number", false);
		addGrp(c++, CLUSTERID, String.class,     GRP_TABLE, "PGstr", "Name of Cluster", true);
		addGrp(c++, CLUSTERCOUNT, Integer.class, GRP_TABLE, "count" , "Number of sequences in cluster", false);
		addGrp(c++, "Taxa", Integer.class,       GRP_TABLE, "taxa" , "Number of dataset x sequences (e.g. 2xN is two datasets where at least one has multiple sequences)", false);
		if (hasPCC) {
		   addGrp(c++, "%PCC", Integer.class,       GRP_TABLE,  "perPCC" , 
				   "Percent of the pairs with PCC>=" + Globals.PCCcutoff 
				+ " where PCC is Pearson correlation coefficient on the TPM values ",  false);
		   addGrp(c++, "minPCC", Integer.class,     GRP_TABLE,  "minPCC" , "The minimal PCC for any pair of the cluster ", false);
		}
		if (hasMultiScore) {
			 mkSection.add(c);
			 addGrp(c++, "conLen", Integer.class,       GRP_TABLE,  "conLen" , 
					 "Length of the consenus sequence for the amino acid alignment.",  false);
			 addGrp(c++, "sdLen", Double.class,       GRP_TABLE,  "sdLen" , 
					 "Standard deviation of the AA sequence lengths (AAlen) in the cluster ",  false);
			 addGrp(c++, SCORE1, Integer.class,       GRP_TABLE,  "score1" , msaScore1,  false);
			 addGrp(c++, SCORE2, Integer.class,       GRP_TABLE,  "score2" , msaScore2,  false);
		}
		mkSection.add(c);
		
		addGrp(c++, HITID, String.class,     GRP_TABLE,  "HITstr" , "Identifier of majority best anno hit", false);
		addGrp(c++, HITDESC, String.class,   HIT_TABLE, "description", "Description of majority best anno hit", false);
		addGrp(c++, "Species", String.class, HIT_TABLE, "species" , "Species of majority best anno hit", false);
		addGrp(c++, "Type", String.class,    HIT_TABLE, "dbtype" , "Type of majority best anno hit", false);
		addGrp(c++, "Tax", String.class,     HIT_TABLE, "taxonomy" , "Taxonomy of majority best anno hit", false);
		addGrp(c++, "HitLen", Integer.class, HIT_TABLE, "length" , "Length of the majority best anno hit", false);
		
		mkBreak.add(c);
		if (hasGO) {
			addGrp(c++, "nGOs", Integer.class, HIT_TABLE, "nGO", "Number of GOs assigned to the majority best anno hit", false);
			addGrp(c++, "GOs", String.class,   HIT_TABLE, "goList" , "List of GOs assigned to the majority best anno hit", false);
		}
		addGrp(c++, "%Hit", Integer.class,   GRP_TABLE, "perAnno" , "Percent of the sequences in the cluster with the majority hit", false);
		addGrp(c++, "E-value", Double.class, GRP_TABLE, 	"e_value" , "Best Blast E-value for the majority best anno hit", false);
		mkSection.add(c);
		
		if (c!=nCol) // only happens when change columns
			ErrorReport.die("Wrong columns " + c + " declared " + nCol);
		
		GRP_SECTION_IDX = new int [mkSection.size()];
		int i=0;
		for (int j : mkSection) GRP_SECTION_IDX[i++]=j;
		GRP_SECTION_BREAK = new int [mkBreak.size()];
		i=0;
		for (int j : mkBreak) GRP_SECTION_BREAK[i++]=j;
	}
	private static void addGrp(int c, String name,  Class <?> type, String sqlTab, String sqlCol, 
			String desc, boolean def) {
		if (c>=GRP_COLUMNS.length) // only happens when change columns
			ErrorReport.die("addGRP: Wrong columns " + c + " declared " + GRP_COLUMNS.length);
		
		GRP_COLUMNS[c] = name;
		GRP_SQLCOL[c] = sqlCol;
		GRP_SQLTAB[c] = sqlTab;
		GRP_TYPE[c] = type;
		GRP_DESCRIP[c] = desc;
		GRP_DEFAULT[c] = def;
	}
	// First method called, so do set up
	public static FieldData getGrpFields(String [] asm) {
		if (GRP_COLUMNS==null) {
			buildGrp();
		}
		
		FieldData fd = new FieldData();
		fd.addField(GRP_COLUMNS[0], GRP_TYPE[0], GRP_SQLTAB[0], GRP_SQLCOL[0],  "ROWNUM");
		for (int i=1; i<GRP_COLUMNS.length; i++)
			fd.addField(GRP_COLUMNS[i], GRP_TYPE[i], GRP_SQLTAB[i], GRP_SQLCOL[i],  "X"+i);
		
		for(int x=0; x<asm.length; x++) {
			fd.addField(asm[x], Integer.class, GRP_TABLE, Globals.PRE_ASM + asm[x], "A" + x);
		}
		fd.addJoin("unique_hits", "pog_groups.HITid = unique_hits.HITid", "");

		fd.addField(GRP_SQLID, Integer.class, GRP_TABLE, "PGid", "PGID");
		
		return fd;
	}
	public static String [] getGrpColumnSections() { return GRP_SECTIONS; }
	
	public static int [] getGrpColumnSectionIdx(int nAsm) {
		int [] retVal = new int[GRP_SECTION_IDX.length + 3];
		for(int x=0; x<GRP_SECTION_IDX.length; x++) retVal[x] = GRP_SECTION_IDX[x];
		retVal[GRP_SECTION_IDX.length] = 	GRP_SECTION_IDX[GRP_SECTION_IDX.length -1] + nAsm;		
		return retVal; 
	}
	public static int [] getGrpColumnSectionBreak() {
		int [] retVal = new int[GRP_SECTION_BREAK.length];
		for(int x=0; x<GRP_SECTION_BREAK.length; x++) retVal[x] = GRP_SECTION_BREAK[x];
		return retVal; 
	}
	public static String [] getGrpColumns(String [] asm) {
		String [] retVal = new String[GRP_COLUMNS.length + asm.length];
		for(int x=0; x<GRP_COLUMNS.length; x++) retVal[x] = GRP_COLUMNS[x];
		
		int offset = GRP_COLUMNS.length;
		for(int x=0; x<asm.length; x++) retVal[x + offset] = asm[x];
		offset += asm.length;
		
		return retVal; 
	}
	
	public static String [] getGrpDescript(String [] asm) {
		String [] retVal = new String[GRP_DESCRIP.length + asm.length ];
		for(int x=0; x<GRP_DESCRIP.length; x++) retVal[x] = GRP_DESCRIP[x];
		int offset = GRP_DESCRIP.length;
	
		for(int x=0; x<asm.length; x++)
			retVal[x + offset] = "Number of sequences in the cluster from dataset " + asm[x];
		offset += asm.length;
		
		return retVal; 
	}
		
	public static boolean [] getGrpSelections(int nAsm) {
		boolean [] retVal = new boolean[GRP_DEFAULT.length + nAsm];
		for(int x=0; x<GRP_DEFAULT.length; x++) retVal[x] = GRP_DEFAULT[x];
		
		int offset = GRP_DEFAULT.length;
		for(int x=0; x<nAsm; x++) retVal[x + offset] = true;
		
		return retVal; 
	}
	
	//****************************************************************************
	//* XXX Pairs section
	//****************************************************************************	
	private static String [] PAIR_SECTIONS;
	private static  String [] PAIR_COLUMNS, PAIR_SQLCOL, PAIR_SQLTAB, PAIR_DESCRIP ; 
	public static  boolean [] PAIR_DEFAULT;
	private static  Class<?>  [] PAIR_TYPE;
	private static int [] PAIR_SECTION_IDX, PAIR_SECTION_BREAK;
	
	public static void buildPairs() {
		int nCol=19, nNT=8, nStat=21, nKa=4; 
		if (hasPCC) nCol++;
		if (hasGO)  nCol++;
		if (hasNTblast) nCol += nNT;
		
		if (!hasStats) {
			PAIR_SECTIONS =	new String [4];		
			PAIR_SECTIONS[0] = "General";
			PAIR_SECTIONS[1] = "Shared Hit"; // CAS310
			PAIR_SECTIONS[2] = "Pair Hit";
			PAIR_SECTIONS[3] = "Cluster Sets";
		}
		else {
			nCol += nStat;
			if (hasKaKs) nCol += nKa;
			
			PAIR_SECTIONS =	new String [6];	
			PAIR_SECTIONS[0] = "General";
			PAIR_SECTIONS[1] = "Shared Hit";
			PAIR_SECTIONS[2] = "Pair Hit";
			PAIR_SECTIONS[3] = "Coding sequence";
			if (hasKaKs) {
				PAIR_SECTIONS[4] = "KaKs";
				PAIR_SECTIONS[5] = "Cluster Sets";
			}
			else PAIR_SECTIONS[4] = "Cluster Sets";
		}
		
		Vector <Integer> mkBreak = new Vector <Integer> ();
		Vector <Integer> mkSection = new Vector <Integer> ();
		
		PAIR_COLUMNS = new String [nCol];
		PAIR_SQLCOL = new String [nCol];
		PAIR_SQLTAB = new String [nCol];
		PAIR_DESCRIP = new String [nCol];
		PAIR_TYPE = new Class<?> [nCol];
		PAIR_DEFAULT = new boolean [nCol];
		
		int c=0;
		addPair(c++, ROWNUM, Integer.class, null, null, "Row number", false);
		addPair(c++, PAIRID, Integer.class, PAIR_TABLE, "PAIRid", "Pair number", false);
		addPair(c++, SEQID1, String.class, PAIR_TABLE, "UTstr1", "Name of 1st sequence of pair", true);
		addPair(c++, SEQID2, String.class, PAIR_TABLE, "UTstr2",  "Name of 2nd sequence of pair", true);
		String seqType = (hasNTdb) ? "CDS" : "AA";
		String desc = (hasNTdb) ? "Length of CDS in bases for" : "Length of translated ORF in amino acids"; // CAS342
		addPair(c++, seqType+"len1", Integer.class, PAIR_TABLE, "CDSlen1", desc + " SeqID1", false);
		addPair(c++, seqType+"len2", Integer.class, PAIR_TABLE,"CDSlen2",  desc + " SeqID2", false);
		if (hasPCC)
		   addPair(c++, "PCC",  Float.class, PAIR_TABLE,"PCC", "Pearson Correlation Coefficient of TPM values",false);
		
		mkSection.add(c);
		
		// Using Hit_Table.HITID instead of PAIR_TABLE.HITid because it has 'NoShare'
		addPair(c++, HITID,  String.class, PAIR_TABLE,"HITstr", "HitID of best shared hit", false);
		addPair(c++, HITDESC, String.class, HIT_TABLE, "description", "Description of best shared hit",false);
		addPair(c++, "Type", String.class,  HIT_TABLE,"dbtype", "Type of best shared hit",  false);
		addPair(c++, "Tax",  String.class,  HIT_TABLE,"taxonomy","Taxonomy of best shared hit", false);
		addPair(c++, "HitLen",     Integer.class, HIT_TABLE, "length",     "Length of hit for shared anno hit"	, false); // CAS310 added for consistency
		if (hasGO)
			addPair(c++, "nGOs", Integer.class, HIT_TABLE, "nGO", "Number GOs of best shared hit", false);

		mkSection.add(c);
		
		addPair(c++,"AAeval", Double.class, PAIR_TABLE, "aaEval",   "Hit E-value for protein alignment",  false);
		addPair(c++,"%AAsim", Float.class, PAIR_TABLE, "aaSim",     "Hit %Similarity (%Identity) for protein alignment", false);
		addPair(c++, "AAalign",Integer.class, PAIR_TABLE,"aaAlign", "Hit protein alignment length - includes gaps", false);
		
		desc = (hasNTdb) ? "(AAalign/(CDSlen1/3)" : "(AAalign/AAlen1)"; // CAS342
		addPair(c++, "%AAcov1",Float.class, PAIR_TABLE, "aaOlap1",  "Hit protein percent coverage for SeqID1 " + desc, false);
		desc = (hasNTdb) ? "(AAalign/(CDSlen2/3)" : "(AAalign/AAlen2)"; // CAS342
		addPair(c++, "%AAcov2", Float.class, PAIR_TABLE,"aaOlap2",  "Hit protein percent coverage for SeqID2 " + desc, false);
		addPair(c++, "AAgap", Integer.class, PAIR_TABLE, "aaGap",   "Hit #Gap Open for protein alignment", false);
		addPair(c++, "AAbit", Integer.class, PAIR_TABLE, "aaBit",   "Hit bit-score for protein alignment",  false);
		addPair(c++, "AAbest",  Integer.class, PAIR_TABLE,"aaBest", 
				"Pair is best AA hit for one (1) or both (2) sequences, where a 2 is BBH)",false);
		
		if (theInfo.hasNTblast()) {
			mkBreak.add(c);
			addPair(c++, "NTeval", Double.class, PAIR_TABLE, "ntEval",   "Hit E-value for DNA alignment",  false);
			addPair(c++, "%NTsim", Float.class, PAIR_TABLE, "ntSim",     "Hit %Similarity (%Identity) for DNA alignment",   false);
			addPair(c++, "NTalign", Integer.class, PAIR_TABLE, "ntAlign","Hit DNA alignment length - includes gaps",  false);
			addPair(c++, "%NTcov1", Float.class, PAIR_TABLE, "ntOlap1",  "Hit DNA percent coverage for SeqID1 (ntAlign/CDSlen1)", false);
			addPair(c++, "%NTcov2", Float.class, PAIR_TABLE, "ntOlap2",  "Hit DNA percent coverage for SeqID2 (ntAlign/CDSlen2)",  false);
			addPair(c++, "NTgap", Integer.class, PAIR_TABLE, "ntGap",    "Hit #Gap Open for DNA alignment",  false);
			addPair(c++, "NTbit",  Integer.class, PAIR_TABLE, "ntBit",   "Hit bit-score for DNA alignment",  false);
			addPair(c++, "NTbest",  Integer.class, PAIR_TABLE,"ntBest", 
					"Pair is best NT hit for one (1) or both (2) sequences, where a 2 is BBH)",false);
		}
		mkSection.add(c);
		
		if (hasStats) {
			addPair(c++, "Align",  Integer.class, PAIR_TABLE, "nAlign",  "Number of aligned bases in the CDS alignment; includes gaps, does not include overhang",  false);
			addPair(c++, "%Cov1", Float.class, PAIR_TABLE,     "pOlap1",  "Percent CDS coverage of alignment for SeqID1 (Align/CDSlen1)%; >100% can occur due to gaps", false);
			addPair(c++, "%Cov2", Float.class, PAIR_TABLE,     "pOlap2",  "Percent CDS coverage of alignment for SeqID2 (Align/CDSlen2)%; >100% can occur due to gaps",  false);
			addPair(c++, "SNPs", Integer.class, PAIR_TABLE, "nSNPs",      "Number of SNPs in the alignment", false);
			addPair(c++, "Gopen", Integer.class, PAIR_TABLE, "nOpen",     "Number of gap opens in the alignment", false);
			addPair(c++, "Gap", Integer.class, PAIR_TABLE, "nGap",        "Number of gaps in the alignment", false);
			addPair(c++, "GC-JI",Integer.class, PAIR_TABLE,  "GC",       "Jaccard Index (#G + #C in both sequence)/(#G + #C in either sequence);; 1 if the numerator is 0",   false);
		
			mkBreak.add(c);
			addPair(c++, "Calign", Float.class, PAIR_TABLE, "Calign",   "Number of aligned codons - excludes codons with gaps",   false);
			addPair(c++, "%Cexact", Float.class, PAIR_TABLE, "pCmatch",   "Percent of codons (Calign) that are exact matches",   false);		
			addPair(c++, "%Csyn",Float.class, PAIR_TABLE, "pCsyn",        "Percent of codons (Calign) that are synonymous (different codon, same amino acid)",   false);
			addPair(c++, "%CnonSyn", Float.class, PAIR_TABLE, "pCnonSyn", "Percent of codons (Calign) that are nonsynonymous (different codon, different amino acid)",  false);
			addPair(c++, "%C4d", Float.class, PAIR_TABLE, "pC4d",         "Percent of codons (Calign) that are 4-fold degenerate (synonymous: four possible bases in ith position)",  false);
			addPair(c++, "%C2d", Float.class, PAIR_TABLE, "pC2d",         "Percent of codons (Calign) that are 2-fold degenerate (synonymous: two possible bases in ith position)",  false);
			
			addPair(c++, "CpG-JI", Float.class, PAIR_TABLE,  "CpGc",         "Jaccard Index (#CpG in both codons)/(#CpG in either codon); 1 if the numerator is 0;  CpG do not cross codon boundaries",   false);
			
			mkBreak.add(c);
			addPair(c++, "%Aexact", Float.class, PAIR_TABLE,  "pAmatch",  "Percent of amino acids (Calign) that are matches",   false);
			addPair(c++, "%Apos", Float.class, PAIR_TABLE,  "pAsub",      "Percent of amino acid (Calign) that have positive Blosum62 substitution score, i.e. more likely substitution",  false);		
			addPair(c++, "%Aneg", Float.class, PAIR_TABLE,  "pAmis",      "Percent of amino acid (Calign) that have negative or zero Blosum62 substitution score, i.e. less likely substitution",   false);
			addPair(c++, "%Cdiff", Float.class, PAIR_TABLE, "pDiffCDS",   "CDS %Difference   (#bases different including gaps/Align)",  false);
			addPair(c++, "%5diff", Float.class, PAIR_TABLE,  "pDiffUTR5", "5'UTR %Difference (#bases different including gaps/Align); a '-' indicates one or both do not have UTR; overhangs excluded",  false);
			addPair(c++, "%3diff", Float.class, PAIR_TABLE,  "pDiffUTR3", "3'UTR %Difference (#bases different including gaps/Align); a '-' indicates one or both do not have UTR; overhangs excluded",  false);
			addPair(c++, "ts/tv",Float.class, PAIR_TABLE,  "tstv",        "ts/tv of CDS, where ts=Transistion, tv=Transversion",	  false);
			
			if (hasKaKs) {
				mkSection.add(c);
				addPair(c++, "Ka",Float.class, PAIR_TABLE, "ka","Nonsynonymous substitution rate",	   false);
				addPair(c++, "Ks", Float.class, PAIR_TABLE, "ks","Synonymous substitution rate", 	   false);
				addPair(c++, KaKs, Float.class, PAIR_TABLE, "kaks","Selective strength",	   false);
				addPair(c++, "p-value",	Double.class, PAIR_TABLE, "pVal", "Fisher exact test of KaKs value",   false);
			}
			mkSection.add(c);
		}
		if (c!=nCol) // only happens when change columns
			ErrorReport.die("Pairs: Wrong columns " + c + " declared " + nCol);
		
		PAIR_SECTION_IDX = new int [mkSection.size()];
		int i=0;
		for (int j : mkSection) PAIR_SECTION_IDX[i++]=j;
		PAIR_SECTION_BREAK = new int [mkBreak.size()];
		i=0;
		for (int j : mkBreak) PAIR_SECTION_BREAK[i++]=j;
	}
	
	private static void addPair(int c, String name,  Class <?> type, String sqlTab, String sqlCol, 
			String desc, boolean def) {
		if (c>=PAIR_COLUMNS.length) // only happens when change columns
			ErrorReport.die("addPairs: Wrong columns " + c + " declared " + PAIR_COLUMNS.length);
		
		PAIR_COLUMNS[c] = name;
		PAIR_SQLCOL[c] = sqlCol;
		PAIR_SQLTAB[c] = sqlTab;
		PAIR_TYPE[c] = type;
		PAIR_DESCRIP[c] = desc;
		PAIR_DEFAULT[c] = def;
	}
	// this is the first routine called so do setup from it 
	public static FieldData getPairFields(String [] methods) {
		if (PAIR_COLUMNS==null) {
			buildPairs();
		}
		
		FieldData fd = new FieldData();
		fd.addField(PAIR_COLUMNS[0], PAIR_TYPE[0], PAIR_SQLTAB[0], PAIR_SQLCOL[0],  "ROWNUM");
		for (int i=1; i<PAIR_COLUMNS.length; i++)
			fd.addField(PAIR_COLUMNS[i], PAIR_TYPE[i], PAIR_SQLTAB[i], PAIR_SQLCOL[i],  "X"+i);
		
		for(int x=0; x<methods.length; x++) {
			fd.addField(methods[x], String.class, PAIR_TABLE, methods[x], "MET" + x);
		}
		fd.addJoin("unique_hits", "pairwise.HITid = unique_hits.HITid", "");
		fd.addField(ID1_SQLID, Integer.class, PAIR_TABLE, "UTid1", "UT1");
		fd.addField(ID2_SQLID, Integer.class, PAIR_TABLE, "UTid2", "UT2");
		return fd;
	}
	public static String [] getPairColumnSections() { return PAIR_SECTIONS; }
	
	public static int [] getPairColumnSectionIdx(int nMet) {
		int [] retVal = new int[PAIR_SECTION_IDX.length+nMet];
		for(int x=0; x<PAIR_SECTION_IDX.length; x++) retVal[x] = PAIR_SECTION_IDX[x];
		return retVal; 
	}
	public static int [] getPairColumnSectionBreak() {
		int [] retVal = new int[PAIR_SECTION_BREAK.length];
		for(int x=0; x<PAIR_SECTION_BREAK.length; x++) retVal[x] = PAIR_SECTION_BREAK[x];
		return retVal; 
	}
	public static String [] getPairColumns(String [] methods) {
		int offset = PAIR_COLUMNS.length;
		String [] retVal = new String[PAIR_COLUMNS.length + methods.length];
		for(int x=0; x<PAIR_COLUMNS.length; x++) retVal[x] = PAIR_COLUMNS[x];
		for(int x=0; x<methods.length; x++) retVal[x + offset] = methods[x];	
		return retVal; 
	}
	public static int getPairMethodStart() {
		return PAIR_COLUMNS.length;
	}
	public static String [] getPairDescript(String [] methods) {
		int offset = PAIR_DESCRIP.length;
		String [] retVal = new String[PAIR_DESCRIP.length+methods.length];
		for(int x=0; x<PAIR_DESCRIP.length; x++) retVal[x] = PAIR_DESCRIP[x];
		for (int x=0; x<methods.length; x++) retVal[x+offset] = "Cluster ID for " + methods[x];
		return retVal; 
	}
	
	public static boolean [] getPairSelections(int nMet) {
		int offset = PAIR_DEFAULT.length;
		boolean [] retVal = new boolean[PAIR_DEFAULT.length + nMet];
		for(int x=0; x<PAIR_DEFAULT.length; x++) retVal[x] = PAIR_DEFAULT[x];
		for(int x=0; x<nMet; x++) retVal[x + offset] = false;
		return retVal; 
	}
	           
	/**********************************************
	 * XXX Sequence columns 
	 **********************************************/
	private static String [] SEQ_SECTION;
	private static  String [] SEQ_COLUMNS, SEQ_SQLCOL, SEQ_SQLTAB, SEQ_DESCRIP ; 
	public static  boolean [] SEQ_DEFAULTS;
	private static  Class<?>  [] SEQ_TYPE;
	private static int [] SEQ_SECTION_IDX, SEQ_SECTION_BREAK;
	
	private static void buildSeq() {
		int nCol=20, nNT=9;
		if (hasGO) nCol++;
		if (hasOrig) nCol++;
		
		if (!hasNTdb) {
			nCol -= nNT;
			SEQ_SECTION =	new String [5];		
			SEQ_SECTION[0] = "General";
			SEQ_SECTION[1] = "Best Hit"; 
			SEQ_SECTION[2] = "Cluster Sets";
			SEQ_SECTION[3] = "TPM";
			SEQ_SECTION[4] ="Differential Expression";
		}
		else {
			SEQ_SECTION =	new String [6];		
			SEQ_SECTION[0] = "General";
			SEQ_SECTION[1] = "Regions";
			SEQ_SECTION[2] = "Best Hit"; 
			SEQ_SECTION[3] = "Cluster Sets";
			SEQ_SECTION[4] = "TPM";
			SEQ_SECTION[5] ="Differential Expression";
		}
		
		Vector <Integer> mkBreak = new Vector <Integer> ();
		Vector <Integer> mkSection = new Vector <Integer> ();
		
		SEQ_COLUMNS = new String [nCol];
		SEQ_SQLCOL = new String [nCol];
		SEQ_SQLTAB = new String [nCol];
		SEQ_DESCRIP = new String [nCol];
		SEQ_TYPE = new Class<?> [nCol];
		SEQ_DEFAULTS = new boolean [nCol];
		
		int c=0;
		addSeq(c++, ROWNUM, Integer.class, null, null, "Row number", false);
		addSeq(c++, SEQID, String.class,  SEQ_TABLE, "UTstr", "Name of sequence", false);
		if (hasOrig) 
			addSeq(c++, "OrigID", String.class,  SEQ_TABLE, "origStr", "Name of original sequence", false);
		addSeq(c++,  AALEN, Integer.class, SEQ_TABLE, "aaLen", "Amino acid length", false);
		//addSeq(c++, "Total Count", Integer.class, SEQ_TABLE, "totExp", "Sum of the counts for all conditions", false);
		if (hasNTdb) {
			addSeq(c++, NTLEN, Integer.class, SEQ_TABLE, "ntLen", "Nucleotide length",false);
			addSeq(c++, "%GC", Double.class,  SEQ_TABLE, "GC", "Percent GC nucleotides for the entire sequence", false);
			addSeq(c++, "%CpG", Double.class,  SEQ_TABLE, "CpG", "Percent CpG sites for the entire sequence",false);
		}
		addSeq(c++, "nPairs", Integer.class,  SEQ_TABLE, "nPairs", "Number of Hit Pairs from different sets",false);
		mkSection.add(c);
		
		if (hasNTdb) {
			addSeq(c++,  "5UTR Len",Integer.class, SEQ_TABLE, "utr5Len","Length of 5'UTR",false);
			addSeq(c++, "CDS Len", Integer.class, SEQ_TABLE, "cdsLen","Length of CDS",false);
			addSeq(c++, "3UTR Len", Integer.class, SEQ_TABLE, "utr3Len", "Length of 3'UTR",false);
			addSeq(c++, "5UTR CpG", Double.class,  SEQ_TABLE, "utr5Ratio",  "5'UTR CpG O/E  ((#CpG / #G*#C)*Len)",false);
			addSeq(c++, "CDS CpG", Double.class,  SEQ_TABLE, "cdsRatio",    "CDS CpG O/E    ((#CpG / #G*#C)*Len)", false);
			addSeq(c++,  "3UTR CpG", Double.class,  SEQ_TABLE, "utr3Ratio", "3'UTR CpG O/E  ((#CpG / #G*#C)*Len)",false);
			mkSection.add(c);
		}
	
		addSeq(c++, HITID,     String.class,  HIT_TABLE,  "HITstr",     "Name of best anno hit", false);
		addSeq(c++, HITDESC,   String.class,  HIT_TABLE,  "description","Description of best anno hit",false);
		addSeq(c++, "Species", String.class,  HIT_TABLE,  "species",    "Species of best anno hit", false);
		addSeq(c++, "Type",    String.class,  HIT_TABLE,  "dbtype",     "DB Type for best anno hit",false);
		addSeq(c++, "Tax",     String.class,  HIT_TABLE,  "taxonomy",   "Taxonomy for best anno hit",false);
		addSeq(c++, "HitLen",     Integer.class, HIT_TABLE, "length",     "Length of hit for best anno hit"	, false);
		if (hasGO)
			addSeq(c++, "nGO", Integer.class, HIT_TABLE, "nGO","Number of GOs for best anno hit",false);
		addSeq(c++, "E-value", Double.class,  SEQ_TABLE,  "e_value",    "E-value to best anno hit",false);
		mkSection.add(c);
	
		SEQ_SECTION_IDX = new int [mkSection.size()];
		int i=0;
		for (int j : mkSection) SEQ_SECTION_IDX[i++]=j;
		SEQ_SECTION_BREAK = new int [mkBreak.size()];
		i=0;
		for (int j : mkBreak) SEQ_SECTION_BREAK[i++]=j;
	}
	private static void addSeq(int c, String name,  Class <?> type, String sqlTab, String sqlCol, 
			String desc, boolean def) {
		if (c>=SEQ_COLUMNS.length) // only happens when change columns
			ErrorReport.die("addSeq: Wrong columns " + c + " declared " + SEQ_COLUMNS.length);
		
		SEQ_COLUMNS[c] = name;
		SEQ_SQLCOL[c] = sqlCol;
		SEQ_SQLTAB[c] = sqlTab;
		SEQ_TYPE[c] = type;
		SEQ_DESCRIP[c] = desc;
		SEQ_DEFAULTS[c] = def;
	}
	public static FieldData getSeqFields(String [] lib, String [] de, String [] methods) {
		if (SEQ_COLUMNS==null) {
			buildSeq();
		}
		
		FieldData fd = new FieldData();
		fd.addField(SEQ_COLUMNS[0], SEQ_TYPE[0], SEQ_SQLTAB[0], SEQ_SQLCOL[0],  "ROWNUM");
		for (int i=1; i<SEQ_COLUMNS.length; i++)
			fd.addField(SEQ_COLUMNS[i], SEQ_TYPE[i], SEQ_SQLTAB[i], SEQ_SQLCOL[i],  "X"+i);
		
		for(int x=0; x<methods.length; x++) {
			fd.addField(methods[x], String.class, SEQ_TABLE, methods[x], "MET" + x);
		}	
		for(int x=0; x<lib.length; x++) {
			fd.addField(lib[x], Double.class, SEQ_TABLE, Globals.PRE_LIB + lib[x], "LIB" + x);
		}
		for(int x=0; x<de.length; x++) {
			fd.addField(de[x], Double.class, SEQ_TABLE, Globals.PRE_DE + de[x], "DE" + x);
		}

		fd.addField(SEQ_SQLID, String.class, SEQ_TABLE, "UTid", "UTRANSID");
		
		fd.addJoin("unique_hits", "unitrans.HITid = unique_hits.HITid", "");

		return fd;
	}
	public static String [] getSeqColumnSections() { return SEQ_SECTION; }
	
	public static int [] getSeqColumnSectionIdx(int nLib, int nDE, int nMet) {
		int nSection = SEQ_SECTION_IDX.length;
		int nStart =   SEQ_SECTION_IDX[nSection -1];
		
		int [] retVal = new int[nSection + 3];
		
		for(int x=0; x<nSection; x++) retVal[x] = SEQ_SECTION_IDX[x];
		retVal[nSection] = 		nStart + nMet;
		retVal[nSection+1] = 	nStart + nLib + nMet;
		retVal[nSection + 2] = 	nStart + nLib + nDE + nMet;
	
		return retVal; 
	}

	public static String [] getSeqColumns(String [] lib, String [] de, String [] methods) { 
		int offset = SEQ_COLUMNS.length;
		String [] retVal = new String[offset + lib.length + de.length + methods.length];
		for(int x=0; x<offset; x++) retVal[x] = SEQ_COLUMNS[x];
	
		for(int x=0; x<methods.length; x++) retVal[x + offset] = methods[x];		
		offset += methods.length;

		for(int x=0; x<lib.length; x++) retVal[x + offset] = lib[x];
		offset += lib.length;
		
		for(int x=0; x<de.length; x++) retVal[x + offset] = de[x];
		
		return retVal; 
	}
	
	public static String [] getSeqDescrip(String [] lib, String [] de, String [] methods) { 
		int offset = SEQ_DESCRIP.length;
		String [] retVal = new String[offset + lib.length + de.length + methods.length];
		for(int x=0; x<offset; x++) retVal[x] = SEQ_DESCRIP[x];
	
		for(int x=0; x<methods.length; x++) retVal[x + offset] = "Name of Cluster is " + methods[x];		
		offset += methods.length;
		
		for(int x=0; x<lib.length; x++) retVal[x + offset] = "TPM (normalized count) for " + lib[x];
		offset += lib.length;
		
		for(int x=0; x<de.length; x++) retVal[x + offset] = "Differential Expression for " + de[x];
		return retVal; 
	}
	
	public static boolean [] getSeqSelections(int nLib, int nDE, int nMet) {
		int offset = SEQ_DEFAULTS.length;
		boolean [] retVal = new boolean[offset + nLib + nDE + nMet];
		for(int x=0; x<offset; x++) retVal[x] = SEQ_DEFAULTS[x];
		
		for(int x=0; x<nMet; x++) retVal[x + offset] = false;		
		offset += nMet;
		
		for(int x=0; x<nLib; x++) retVal[x + offset] = true;
		offset += nLib;
		
		for(int x=0; x<nDE; x++) retVal[x + offset] = true;
		return retVal; 
	}
	
	/**********************************************
	 * XXX Hit columns CAS310
	 **********************************************/
	
	private static String [] HIT_SECTION;
	private static  String [] HIT_COLUMNS, HIT_SQLCOL, HIT_SQLTAB, HIT_DESCRIP ; 
	public static  boolean [] HIT_DEFAULTS;
	private static  Class<?>  [] HIT_TYPE;
	private static int [] HIT_SECTION_IDX, HIT_SECTION_BREAK;
	
	private static void buildHit() {
		int nCol=10;
		if (hasGO) nCol++;
		
		HIT_SECTION =	new String [2];		
		HIT_SECTION[0] = "General";
		HIT_SECTION[1] = "Hit Info";
		
		Vector <Integer> mkBreak = new Vector <Integer> ();
		Vector <Integer> mkSection = new Vector <Integer> ();
		
		HIT_COLUMNS = new String [nCol];
		HIT_SQLCOL = new String [nCol];
		HIT_SQLTAB = new String [nCol];
		HIT_DESCRIP = new String [nCol];
		HIT_TYPE = new Class<?> [nCol];
		HIT_DEFAULTS = new boolean [nCol];
		
		int c=0;
		addHit(c++, ROWNUM, Integer.class, null, null, "Row number", false);
		addHit(c++,  "nSeq", Integer.class, HIT_TABLE, "nSeq", "Number of sequences with hits", true);
		addHit(c++,  "nBest", Integer.class, HIT_TABLE, "nBest", "Number of best hits", true);
		addHit(c++, "E-value", Double.class,  HIT_TABLE,  "e_value",    "Best E-value",false);
		mkSection.add(c);
		
		addHit(c++, HITID,     String.class,  HIT_TABLE,  "HITstr",     "Hit name", true);
		addHit(c++, HITDESC,   String.class,  HIT_TABLE,  "description","Hit Description",true);
		addHit(c++, "Species", String.class,  HIT_TABLE,  "species",    "Hit Species", true);
		addHit(c++, "Type", String.class,  HIT_TABLE, "dbtype", "DB Type", false);
		addHit(c++,  "Tax", String.class, HIT_TABLE, "taxonomy", "Taxonomy", false);
		addHit(c++, "HitLen",     Integer.class, HIT_TABLE, "length",     "Length of hit"	, false);
		if (hasGO)
			addHit(c++, "nGO", Integer.class, HIT_TABLE, "nGO","Number of GOs for hit",false);
		mkSection.add(c);
		
		if (c!=nCol) Out.PrtWarn("Hit table: " + c + " " + nCol);
	
		HIT_SECTION_IDX = new int [mkSection.size()];
		int i=0;
		for (int j : mkSection) HIT_SECTION_IDX[i++]=j;
		
		HIT_SECTION_BREAK = new int [mkBreak.size()];
		i=0;
		for (int j : mkBreak) HIT_SECTION_BREAK[i++]=j;
	}
	private static void addHit(int c, String name,  Class <?> type, String sqlTab, String sqlCol, 
			String desc, boolean def) {
		if (c>=HIT_COLUMNS.length) // only happens when change columns
			ErrorReport.die("addHit: Wrong columns " + c + " declared " + HIT_COLUMNS.length);
		
		HIT_COLUMNS[c] = name;
		HIT_SQLCOL[c] = sqlCol;
		HIT_SQLTAB[c] = sqlTab;
		HIT_TYPE[c] = type;
		HIT_DESCRIP[c] = desc;
		HIT_DEFAULTS[c] = def;
	}
	public static FieldData getHitFields() {
		if (HIT_COLUMNS==null) {
			buildHit();
		}
		
		FieldData fd = new FieldData();
		fd.addField(HIT_COLUMNS[0], HIT_TYPE[0], HIT_SQLTAB[0], HIT_SQLCOL[0],  "ROWNUM");
		
		for (int i=1; i<HIT_COLUMNS.length; i++)
			fd.addField(HIT_COLUMNS[i], HIT_TYPE[i], HIT_SQLTAB[i], HIT_SQLCOL[i],  "X"+i);

		fd.addField(HIT_SQLID, String.class, HIT_TABLE, "HITid", "HITID");
		
		return fd;
	}
	public static String [] getHitColumnSections() { return HIT_SECTION; }
	
	public static int [] getHitColumnSectionIdx() {
		int nSection = HIT_SECTION_IDX.length;
		
		int [] retVal = new int[nSection];
		
		for(int x=0; x<nSection; x++) retVal[x] = HIT_SECTION_IDX[x];
		
		return retVal; 
	}
	public static int [] getHitColumnSectionBreak() {
		int [] retVal = new int[HIT_SECTION_BREAK.length];
		for(int x=0; x<HIT_SECTION_BREAK.length; x++) retVal[x] = HIT_SECTION_BREAK[x];
		return retVal; 
	}
	public static String [] getHitColumns() { 
		int offset = HIT_COLUMNS.length;
		String [] retVal = new String[offset];
		for(int x=0; x<offset; x++) retVal[x] = HIT_COLUMNS[x];
		
		return retVal; 
	}
	
	public static String [] getHitDescript() { 
		int offset = HIT_DESCRIP.length;
		String [] retVal = new String[offset];
		for(int x=0; x<offset; x++) retVal[x] = HIT_DESCRIP[x];
		return retVal; 
	}
	
	public static boolean [] getHitSelections() {
		int offset = HIT_DEFAULTS.length;
		boolean [] retVal = new boolean[offset];
		for(int x=0; x<offset; x++) retVal[x] = HIT_DEFAULTS[x];
		return retVal; 
	}                   
	//****************************************************************************
	//* XXX Methods for FieldData
	//****************************************************************************
	public FieldData() {
		theViewFields = new Vector<FieldItem> ();
		theJoins = new Vector<JoinItem> ();
	}
	
	private void addJoin(String table, String condition, String strSymbol) { 
		theJoins.add(new JoinItem(table, condition, strSymbol)); 
	}
	private String getFieldReference(FieldItem item) {
		String retVal;
		if(item.getDBFieldName() == null) 
			retVal = "NULL";
		else
			retVal = item.getDBTable() + "." + item.getDBFieldName();
		
		if(item.getDBSymbolName().length() > 0)
			retVal += " AS " + item.getDBSymbolName();
		
		return retVal;
	}

	private void addField(	String tabCol, Class<?> type, String dbTable, String dbCol, String dbSymbol) {
		FieldItem fd = new FieldItem(tabCol, type);
		fd.setQuery(dbTable, dbCol, dbSymbol);
		theViewFields.add(fd);
	}
	//****************************************************************************
	//* Methods  called by tables
	//****************************************************************************
	public String [] getDisplayNames() {
		if(theViewFields.size() == 0) return null;

		Vector<String> retVal = new Vector<String> ();
		Iterator<FieldItem> iter = theViewFields.iterator();
		FieldItem item = null;

		while(iter.hasNext()) {
			item = iter.next();
			String field = item.getFieldName();
			retVal.addElement(field);
		}
		return retVal.toArray(new String[retVal.size()]);
	}
	public Class<?> [] getDisplayTypes() {
		if(theViewFields.size() == 0) return null;

		Vector<Class<?>> retVal = new Vector<Class<?>> ();
		Iterator<FieldItem> iter = theViewFields.iterator();
		FieldItem item = null;

		while(iter.hasNext()) {
			item = iter.next();
			Class<?> type = item.getFieldType();
			retVal.addElement(type);
		}
		return retVal.toArray(new Class<?>[retVal.size()]);
	}
	
	public String getDBFieldQueryList() {
		Iterator<FieldItem> iter = theViewFields.iterator();
		FieldItem item = null;
		if(iter.hasNext()) {
			item = iter.next();
		}
		String retVal = getFieldReference(item);
		
		while(iter.hasNext()) {
			item = iter.next();
			retVal += ", " + getFieldReference(item);
		}
		return retVal;
	}
	public boolean hasJoins() { return !theJoins.isEmpty(); }
	public String getJoins() { 
		Iterator<JoinItem> iter = theJoins.iterator();
		String retVal = "";
		while(iter.hasNext()) {
			if(retVal.length() == 0)
				retVal = iter.next().getJoin();
			else
				retVal += " " + iter.next().getJoin();
		}
		return retVal;
	}
	/***********************************************
	 * Methods called by TableData - addRowsWithProgress 
	 */
	public String [] getDisplaySymbols() {
		if(theViewFields.size() == 0) return null;

		Vector<String> retVal = new Vector<String> ();
		Iterator<FieldItem> iter = theViewFields.iterator();
		FieldItem item = null;

		while(iter.hasNext()) {
			item = iter.next();
			String symbol = item.getDBSymbolName();
			retVal.addElement(symbol);
		}
		return retVal.toArray(new String[retVal.size()]);
	}

	/************************************************
	 * JoinItem class - used by FieldData
	 */
	private class JoinItem {
		public JoinItem(String table, String condition, String symbol) {
			strTable = table;
			strCondition = condition;
			strSymbol = symbol;
		}
		public String getJoin() {
			String retVal = "LEFT JOIN " + strTable;
			if(strSymbol.length() > 0) retVal += " AS " + strSymbol;
			retVal += " ON " + strCondition;
			return  retVal;
		}
		private String strTable = "";
		private String strCondition = "";
		private String strSymbol = "";
	}

	/***************************************************
	 * FieldItem class - used by FieldData
	 */
	private class FieldItem {
		public FieldItem(String fieldName, Class<?> type) {
			strFieldName = fieldName;
			cType = type;
 		}
		public void setQuery(String table, String field, String symbol) {
			strDBTable = table;
			strDBField = field;
			strDBSymbol = symbol;
		}
		public String getFieldName() { return strFieldName; }
		public Class<?> getFieldType() { return cType; }
		public String getDBTable() { return strDBTable; }
		public String getDBFieldName() { return strDBField; }
		public String getDBSymbolName() { return strDBSymbol; }

		private String strFieldName; 		//Display name for a table
		private Class<?> cType = null;		//Data type for the column
		private String strDBTable;			//Source table for the value; False=value calculated
		private String strDBField;			//Source field in the named DB table
		private String strDBSymbol = "";		//Symbolic name for query value 
	}
	private Vector<FieldItem> theViewFields = null;
	private Vector<JoinItem> theJoins = null;
}
