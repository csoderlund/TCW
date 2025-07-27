package cmp.database;

import java.awt.Color;

import cmp.viewer.MTCWFrame;
import util.database.Globalx;
import util.file.FileHelpers;

public final class Globals {
	public static final String VERSION = Globalx.strTCWver;
	public static final String VDATE = Globalx.strRelDate;
	
	public static final String HOST_FILE =   Globalx.HOSTS; 
	public static final String PROJECTDIR =  Globalx.CMPDIR;
	public static final String MTCW =        "mTCW";
	public static final String CONFIG_FILE = "mTCW.cfg";
	public static final String CmpErrorLog = "mTCW.error.log";
	public static final String helpDir =     "html/viewMultiTCW/";
	public static final String helpRunDir =  "html/runMultiTCW/";
	public static final String summaryDir =  Globalx.HTMLDIR;
	public static final String summaryPath = PROJECTDIR + "/" + Globalx.HTMLDIR;
	
	public static final String CSV_DELIM =    Globalx.CSV_DELIM;
	public static final String CSV_SUFFIX =   Globalx.CSV_SUFFIX;
	public static final String FASTA_SUFFIX = Globalx.FASTA_SUFFIX;
	
	public static final String gapStr = Globalx.gapStr;
	public static final char   gapCh =  Globalx.gapCh;
	
	public static final String STCW = "sTCW";
	public static final String TypeAA = "AA (Protein)";
	public static final String TypeNT = "NT (DNA)";
	
	public static final int AA=0, NT=1, CDS=2;
	public static final int bSEQ=0, bPAIR=1, bGRP=2, bHIT=3, bDETAIL=4;
	
	// Compile and View
	public static final String PRE_ASM = "A__";  // mTCW asmID
	public static final String PRE_LIB = "L__";  //  mTCW RPKM/TPM
	public static final String PRE_DE = "P__";	 // mTCW DE 
	
	// Written to /logs directory from Out
	public final static String buildFile = "build.log";
	public final static String searchFile = "search.log";
	public final static String methodFile = "method.log";
	public final static String statsFile = "stats.log";
	
	// Load sTCW: Prefixes are slightly different between Single and Multi; these are singleTCW
	public static final String PRE_S_RPKM = Globalx.LIBRPKM; // sTCW RPKM/TPM
	public static final String PRE_S_CNT =  Globalx.LIBCNT;  // sTCW count
	public static final String PRE_S_DE =   Globalx.PVALUE;
			
	// produces during Run Stats
	public static final String StatsDIR =  "Stats";
	public static final String sumFile =   "summary.txt";
	
	// For KaKs processing
	public static final String KaKsDIR = "KaKs"; // See Ext below
	public static final String KaKsCmd = "runKaKs";
	public static final String KaKsOutPrefix = "oTCW";
	public static final String KaKsOutSuffix = ".awt"; // pairs of aligned for input to KaKs
	public static final String KaKsInPrefix = "iTCW";
	public static final String KaKsInSuffix = ".tsv";
			
	// Load: Unique description and No shared description. The specialID makes them sort to bottom
	public static final String specialID = "*";
	public static final String uniqueID =   specialID + "Novel"; 
	public static final String uniqueDesc = specialID + "Novel - No annotation for any sequence";
	public static final String noneID =     specialID + "NoShare";
	public static final String noneDesc =   specialID + "NoShare - No shared annotation for the pair";
	public static final String GO_FORMAT = Globalx.GO_FORMAT;		
	
	// Compile and viw: Cutoff for Pearson's Correlation Coefficient for perPCC
	public static final double PCCcutoff = 0.8;

	public static class Ext { 
		public final static int MUSCLE = 0;
		public final static int MAFFT = 1;
		
		public final static String mstatxExe = "/mstatX/mstatx";
		public final static String muscleExe = "/muscle/muscle";
		public final static String mafftExe = "/mafft/mafft.bat";
		public final static String kaksExe  = "/KaKs_Calculator";
		public final static String orthoDir = "/OrthoMCL";
		public final static String orthoTryExe = "/OrthoMCL/bin/orthomclInstallSchema"; // try first one
		public final static String orthoDisable = "/OrthoMCL/disable"; // CAS405b
	}
	
	public static class MSA {
		public final static String consName = "Consensus";
		public final static int BUILTIN_SCORE = 0; // built-in msa score
		public final static int MSTATX_SCORE = 1;  // executed by external MstatX
		public final static String SoP = "Sum-of-Pairs";
		public final static String Wep = "Wentropy";
		public final static String filePrefix = "MSA_";
	}
	
	public static class Search {
		public static final String BLASTDIR 	= Globalx.pHITDIR; // hitResults
		public static final String ALL_AA_FASTA = "combineAA.fa";
		public static final String ALL_NT_FASTA = "combineNT.fa";
		public static final String BLAST_AA_TAB = "hitsAA.tab";
		public static final String BLAST_NT_TAB = "hitsNT.tab";
	}
	
	public static class Methods {
		public static final String METHODDIR = "Methods";
		public final static String outDELIM = "x";
		public final static String inDELIM = ":";
		public static final int WIDTH = 110;
		
		public static class BestRecip {	// also AA/NT; Change both/either default to both
			public static final String 	TYPE_NAME = "BBH";
			public static final String 	DEFAULT_PREFIX = "BB";
			public static final int 	COV_TOGGLE=1;			 
			public static final String 	COVERAGE_CUTOFF = "40"; 
			public static final String 	SIMILARITY =      "60"; 
		}
		public static class Closure {	// also AA/NT;
			public static final String 	TYPE_NAME = "Closure";
			public static final String 	DEFAULT_PREFIX = "CL";
			public static final int 	COV_TOGGLE=1;			 
			public static final String 	COVERAGE_CUTOFF = "40";
			public static final String 	SIMILARITY =      "60";
		}
		public static class Hit {	
			public static final String 	TYPE_NAME = "BestHit";
			public static final String 	DEFAULT_PREFIX = "HT";
			public static final String 	COVERAGE_CUTOFF = "20"; 
			public static final String 	SIMILARITY =      "20"; 
			public static final int 	TYPE_TOGGLE=1; // Description
			public static final int 	HIT_TOGGLE=0;  // All hits - closure - reason for low cov and sim
		}
		public static class OrthoMCL {	
			public static final String TYPE_NAME = "OrthoMCL";
			public static final String DEFAULT_PREFIX = "OM";
			public static final String INFLATION = "4";
		}
		public static class UserDef {			
			public static final String TYPE_NAME = "User Defined";
			public static final String DEFAULT_PREFIX = "UD";
		}
	}
	public static final Color BGCOLOR = Color.WHITE;
	
	/**********************************************
	 * viewMulti
	 */
	public static final String next = ">>";
	public static final String prev = "<<";
	public static final String select = "Selected:";
	
	// buttons at top of tables
	public static final String FILTER = "Filter: ";   // On each table with summary of filter
	public static final String GRP_TABLE = 	"Clusters";
	public static final String SEQ_TABLE = 	"Seqs";
	public static final String PAIR_TABLE = "Pairs";
	public static final String SEQ_DETAIL = "Details";
		
	// tags on left and summary prefix: do not use the "s" ones ont tags, only summary
	public static final String tagGRP = 	MTCWFrame.GRP_PREFIX + ": ";
	public static final String tagGRPs = 	MTCWFrame.GRP_PREFIX + ": "; // no s 
	public static final String tagPAIR = 	MTCWFrame.PAIR_PREFIX + ": ";
	public static final String tagPAIRs = 	MTCWFrame.PAIR_PREFIX + "s: ";
	public static final String tagSEQ = 	MTCWFrame.SEQ_PREFIX + ": ";
	public static final String tagSEQs = 	MTCWFrame.SEQ_PREFIX + "s: ";
	public static final String tagHIT =  	MTCWFrame.HIT_PREFIX + ": ";
	public static final String tagHITs =  	MTCWFrame.HIT_PREFIX + "s: ";
	public final static String tagPW = 		"PW: ";
	public final static String tagMxx = 	"MSA: ";
	public final static String tagDETAIL = 	"DT: ";
	public final static int trimSUM = 115;
	
	/****************************************
	 * static methods
	 */
	public static String makeName(String method, String prefix) { // for runMulti methods
		return prefix + "_" + method;
	}
	
	public static boolean hasSpecialID(String hitID) {
		if (hitID.contentEquals(uniqueID) || hitID.contentEquals(noneID)) return true;
		return false;
	}
	public static String trimSum(String sum) {
    	int x = Globals.trimSUM;
    	if (sum.length()>x) sum = sum.substring(0, x) + "...";
    	return sum;
	}
	
	public static boolean isOrtho() {// CAS405b disabled if this file exist
		String path = FileHelpers.getExtDir();
		if (!FileHelpers.existDir(path + Globals.Ext.orthoDir)) return false;
		return !FileHelpers.fileExists(path + Globals.Ext.orthoDisable);
	}
	public static boolean isMuscle() {// CAS405b not available for macM4
		String path = FileHelpers.getExtDir();
		return FileHelpers.fileExists(path + Globals.Ext.muscleExe);
	}
}
