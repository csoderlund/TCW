package cmp.database;

import java.awt.Color;

import util.database.Globalx;

public final class Globals {
	public static final boolean isRelease=true;
	public static final String VERSION = Globalx.strTCWver;
	public static final String VDATE = Globalx.strRelDate;
	
	// Written to /logs directory from Out
	public final static String buildFile = "build.log";
	public final static String searchFile = "search.log";
	public final static String methodFile = "method.log";
	public final static String statsFile = "stats.log";
			
	public static final String CSV_DELIM = Globalx.CSV_DELIM;
	public static final String CSV_SUFFIX = Globalx.CSV_SUFFIX;
	public static final String FASTA_SUFFIX = Globalx.FASTA_SUFFIX;
	
	public static final String PROJECTDIR = Globalx.CMPDIR;
	
	public static final String StatsDIR =      "Stats";
	public static final String sumFile =       "summary.txt";
	
	public static final String KaKsDIR = "KaKs"; // See Ext below
	public static final String KaKsCmd = "runKaKs";
	public static final String KaKsOutPrefix = "oTCW";
	public static final String KaKsOutSuffix = ".awt"; // pairs of aligned for input to KaKs
	public static final String KaKsInPrefix = "iTCW";
	public static final String KaKsInSuffix = ".tsv";
	
	public static final String gapStr = Globalx.gapStr;
	public static final char   gapCh = Globalx.gapCh;
	
	public static final String MTCW =    "mTCW";
	public static final String CONFIG_FILE = "mTCW.cfg";
    public static final String HOST_FILE = Globalx.HOSTS; 
	public static final String CmpErrorLog = "mTCW.error.log";
	public static final String STCW = "sTCW";
	public static final String TypeAA = "AA (Protein)";
	public static final String TypeNT = "NT (DNA)";
	public static final String helpDir = "html/viewMultiTCW/";
	
	public static final int AA=0, NT=1, CDS=2;
	public static final int bSEQ=0, bPAIR=1, bGRP=2, bHIT=3;
	
	// Unique description and No shared description. The specialID makes them sort to bottom
	public static final String specialID = "*";
	public static final String uniqueID = specialID + "Novel"; 
	public static final String uniqueDesc = specialID + "Novel - No annotation for any sequence";
	public static final String noneID = specialID + "NoShare";
	public static final String noneDesc = specialID + "NoShare - No shared annotation for the pair";
	static public final String GO_FORMAT = Globalx.GO_FORMAT;
	
	// these are duplicated in jpave.ManagerFrame.java
	public static final Color BGCOLOR = Color.WHITE;
	public static final Color FUNCTIONCOLOR = Globalx.FUNCTIONCOLOR; 
	public static final Color MENUCOLOR = Globalx.MENUCOLOR;	// beige
	public static final Color HELPCOLOR = Globalx.HELPCOLOR;	// rose
	public static final Color PROMPTCOLOR = Globalx.PROMPTCOLOR;	// light beige
	public static final Color LAUNCHCOLOR = Globalx.LAUNCHCOLOR; // light purple
	public static final Color BUTTONCOLOR = Globalx.BUTTONCOLOR; // light gray CAS310
	
	// Compile
	public static final String PRE_ASM = "A__";  // mTCW asmID
	public static final String PRE_LIB = "L__"; //  mTCW RPKM 
	public static final String PRE_DE = "P__";	// mTCW DE 
	
	// Prefixes are slightly different between Single and Multi; these are singleTCW
	public static final String PRE_S_RPKM = Globalx.LIBRPKM; // sTCW RPKM
	public static final String PRE_S_CNT = Globalx.LIBCNT;   // sTCW count
	public static final String PRE_S_DE =  Globalx.PVALUE;
	
	// Cutoff for Pearson's Correlation Coefficient for perPCC
	public static final double PCCcutoff = 0.8;
	
	public static final int COLUMN_SELECT_WIDTH = 75, COLUMN_PANEL_WIDTH = 900;
	
	// Blast is also external, but taken care of in BlastArgs
	public static class Ext { 
		public final static int MUSCLE = 0;
		public final static int MAFFT = 1;
		// This can be changed to one of the other MstatX functions, however,
		// it would need to be changed in the summary.html and GrpTable.html documentation.
		public final static String score2="Trident";
		
		public final static String mstatxExe = "/mstatX/mstatx";
		public final static String muscleExe = "/muscle/muscle";
		public final static String mafftExe = "/mafft/mafft.bat";
		public final static String kaksExe  = "/KaKs_Calculator";
		public final static String orthoDir = "/OrthoMCL";
		public final static String orthoTryExe = "/OrthoMCL/bin/orthomclInstallSchema"; // try first one
	}
		
	public static class Compile {	
		//Method Classes
		public static final String GROUP_FILE_BEST_RECIP = "BBH";
		public static final String GROUP_FILE_CLOSURE = "Closure";
		public static final String GROUP_FILE_HIT = "BestHit";
		
		//Summary
		public static final String summaryDir = Globalx.HTMLDIR;
		public static final String summaryPath = PROJECTDIR + "/" + Globalx.HTMLDIR;
	}
	
	public static class CompilePanel {
		public static final String BLASTDIR = Globalx.BLASTDIR; // searchResults
		public static final String ALL_AA_FASTA = "combineAA.fasta";
		public static final String ALL_NT_FASTA = "combineNT.fasta";
		public static final String BLAST_AA_TAB = "hitsAA.tab";
		public static final String BLAST_NT_TAB = "hitsNT.tab";
		public static final int AA=0;
		public static final int NT=1;
		public static final String BLASTFILTSUF = ".filt"; // if change this, change EditBlastPanel.parseBlastFile
	
		public static final String BLASTFILTID = "99";
		public static final String BLASTFILTEX = "2";
		
		//Labels for host file
	    public static final String DATABASE_HOST_URL = "DB_host";
		public static final String DATABASE_ANNOTATE_USER = "DB_user";
		public static final String DATABASE_ANNOTATE_PASSWORD = "DB_password";

		//File/directory locations
	    public static final String TEMP_DIR = 	".mtcw";		
		public static final int WIDTH = 110;
	}
	
	public static class Methods {
		public static final String METHODDIR = "Methods";
		public final static String METHODS_DELIM = "x";
		
		public static class BestRecip {	// also AA/NT; Change both/either default to both
			public static final String TYPE_NAME = "BBH";
			public static final String DEFAULT_PREFIX = "BB";
			public static final int COV_TOGGLE=1;			 
			public static final String COVERAGE_CUTOFF = "40"; 
			public static final String SIMILARITY =      "60"; 
		}
		public static class Closure {	// also AA/NT;
			public static final String TYPE_NAME = "Closure";
			public static final String DEFAULT_PREFIX = "CL";
			public static final int COV_TOGGLE=1;			 
			public static final String COVERAGE_CUTOFF = "40";
			public static final String SIMILARITY =      "60";
		}
		public static class Hit {	
			public static final String TYPE_NAME = "BestHit";
			public static final String DEFAULT_PREFIX = "HT";
			public static final String COVERAGE_CUTOFF = "50"; 
			public static final String SIMILARITY =      "20"; 
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
	
	
	public static class Viewer {
	    public static final String DB_NAME_PREFIX = "sTCW"; // for compiling comparison database
	}
	/****************************************
	 * static methods
	 */
	public static String getName(String method, String prefix) {
		return prefix + "_" + method;
	}
}
