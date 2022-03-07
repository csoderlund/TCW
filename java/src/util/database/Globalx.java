package util.database;

import java.awt.Color;
import java.awt.Font;

import util.methods.Static;

// shared by sTCW and mTCW
// They both have their own database/Globals.java
public class Globalx {
	public static boolean debug = false; // changed in STCWMain or MTCWMain from command line 
	
	public static final String strRelDate = "(8-Mar-22)"; 
	public static final String strTCWver = "3.4.2";  //  3.4.2 must be 3 digits
	public static final String URL =     "http://www.agcol.arizona.edu ";
	public static final String TCWhead = "TCW v" + strTCWver + " " + strRelDate;
	public static final String sTCWver = "sTCW v" + strTCWver;
	public static final String mTCWver = "mTCW v" + strTCWver;
	public static final String error="***";
	
	public final static String extDir =   "Ext"; // CAS303 changed from external and external_osx
	public final static String lintelDir = extDir + "/linux";
	public final static String macDir =    extDir + "/mac";
	
	public final static String diamondSuffix = ".dmnd";
	
	public static final String HOSTS = 		"HOSTS.cfg";
	public static final String STCW 	= 	"sTCW_"; 
	public static final String MTCW 	= 	"mTCW_"; 
	public static final String CFG		= 	".cfg";
	public static final String goPreDB = "go_";

	// on MAC, directory names are case in-sensitive
	public static final String PROJDIR 	= "projects";// 11/18/18 libraries directory merged with projects
	public static final String CMPDIR 	= "projcmp";
	public static final String RSCRIPTSDIR 	= "R-scripts";
	
	// Under projects directory
	public static final String ANNOSUBDIR =  "DBfasta";
	public static final String ANNODIR 	= 	 PROJDIR + "/DBfasta";
	public static final String USERDIR = 	"Userfiles";   
	public static final String HTMLDIR = 	"OverviewHTML";
	public static final String DEDIR = 		"RunDE"; // CAS326
	public static final String searchHelp = "html/SearchParam.html"; // CAS339 shared
	
	// for specific project
	public final static String pLOGDIR =   "logs";
	public static final String pHITDIR = 	"hitResults";  // runSingle: results from annotation; runMulti: self-blast
	public static final String pORFDIR = 	"orfFiles";
	
	// TCW directory for viewSingle and viewMulti to put results the user can view
	public static final String rHITDIR =   	"ResultHits"; // viewSingle: sequences are written and blasts occur
	public static final String rALIGNDIR = 	"ResultAlign"; // viewMulti: results of multi align 
	public static final String rEXPORTDIR= 	"ResultExport";
	
	public static final String [] fastaFile = new String [] 
			{"fa","fa.gz", "fasta", "fasta.gz",  "fna", "ffn", "faa", "frn", "fna.gz", "ffn.gz", "faa.gz", "frn.gz"};
	
	public static String SP="sp", TR="tr"; // UniProt   CAS317 these were hardcoded everywhere
	public static String PR="pr", NT="nt"; // Other		ditto (NT and AA is still hardcoded...)
	
	public static final String gapStr  = "-";
	public static final char   gapCh   = '-';
	public static final String hangStr = "=";
	public static final char   hangCh  = '=';
	public static final String stopStr = "*";
	public static final char   stopCh  = '*';
	public static final String noNTstr = "n";
	public static final char   noNTch  = 'n';
	public static final String noAAstr = "X";
	public static final char   noAAch  = 'X';
	public static final char   assmGapCh = '*'; // CAS314 separate from stopCh as its CAP gap ch
	public static final String assmGap = "*";   // CAS314 - this was being replaced with '', causing problems
	public static final String typeAA = "AA";	// CAS314 add 3 types
	public static final String typeNT = "NT";
	public static final String typeORF = "ORF";
	
	// for writing files - repeated in FileC
	public static final String CSV_DELIM = "\t";
	public static final String CSV_SUFFIX = ".tsv"; 
	public static final String TSV_DELIM = "\t";
	public static final String TSV_SUFFIX = ".tsv"; 
	public static final String FASTA_SUFFIX = ".fa";
	public static final String TEXT_SUFFIX = ".txt";
	public static final String HTML_SUFFIX = ".html";
	
	public static final int numDB = 40; // number of annoDBs
	
	public static final String LIBCNT =  "L__";
	public static final String LIBRPKM = "LN__";
	public static final String PVALUE =  "P_"; // note just one dash
	public static final String GO_EvC = "EV__"; // CAS323 - necessary because AS is reserved word
	
	// Its messy about what the values of 'no value' where
	// See Stats.average - 
	public static final String sBlank =    "";
	public static final String sNoVal =    "-";
	public static final String sNullVal =  "NA"; // CAS327 used for KaKs=NA; NoVal is used for not computed
	public static final double dNullVal =   -1.5; 
	
	public static final String iStrNoVal =  "-2";
	public static final int    iNoVal = 	-2; 
	
	public static final String dStrNoVal = 	"-2.0"; 
	public static final double dNoVal = 	-2.0; 
	
	public static final String dStrNoDE = 	"3.0"; // set in QRprocess 
	public static final double dNoDE =  	3.0;   // +/- 
	public static final double dNaDE =		2.0;   // +/- DE computation returns NA - CAS321 changed from 3
	
	public static final String frameField = "Frame";
	public static final float  dNoScore = -100000; // Float.MAX_VALUE does not work
	
	static public final String GO_FORMAT = "GO:%07d";
	// the ECs are in MetaData. MetaData checks database for GO_TERMS, but not used
	// Static.getGOtermMap creates a hashmap; Keep the order, some methods expect it! 
	static public final String [] GO_TERM_LIST = {"biological_process","cellular_component","molecular_function"};
	static public final String [] GO_TERM_ABBR = {"BP", "CC", "MF"};
	static public final String goFullOnt = "Ontology";
	static public final String goOnt =  "Ont"; // CAS322 there is no consensus for what to call this
	static public final String goID =   "GO ID";
	static public final String goTerm = "GO Name";
	static public final String evCode = "EvC";
	
	// runAS.DoGO builds; 
	static public final String goTreeTable = "TCW_gotree"; // CAS318 were prefixed with PAVE_
	static public final String goMetaTable = "TCW_metadata";
	static public final String goUpTable =   "TCW_UniProt";
	
	static public final String tcwDelim = ";";
	public static final String ROW_DELIMITER = "\t";
	
	// Substitutions and alignments
	public static final String blosumGt =   "Substitution >0";
	public static final String blosumLtEq = "Substitution<=0";
	public static final String blosumGtLegend = "Substitution>0 (i.e. BLOSUM62)";
	public static final String blosumEq = "Substitution=0"; // CAS313
	public static final String blosumLt = "Substitution<0";
	public static final String AA_POS ="+"; // aa pos sub
	public static final String AA_NEG =" "; // aa neg sub
	public static final char   cAA_POS ='+'; // aa pos sub
	public static final char   cAA_NEG =' '; // aa neg sub
	
	public static final Color anyHang 	= new Color(180, 180, 180); // mediumGray
	public static final Color anyUnk	= Color.cyan;
	public static final Color anyGap 	= new Color(0,200,0);		// light green; shared with aaZappo
	public static final Color aaStop	= new Color(138, 0, 184); 	// purple; shared with aaZappo
	
	public static final Color ntMisMatch = Color.red;
	
	public static final Color aaLtZero 	= Color.red; 		
	public static final Color aaEqZero	= new Color(255, 173, 190); // light red
	public static final Color aaGtZero 	= Color.blue; 	
	
	static public final int OVERHANG=5; // overhang for trim
		
	// General - button colors are in Static.java
	public static final Color BGCOLOR = Color.WHITE;
	public static final Color HIGHCOLOR = Static.HIGHCOLOR; 	// CAS336
	public static final Color altRowColor = new Color(0xEEF2FF);
	public static final Color selectColor = new Color(0xB0C4DE);
	public static final Font  boldUIFont = new Font("Sans", Font.BOLD, 12);
	public static final Font  textFont = new Font("Sans", Font.PLAIN, 11);
	public static final Color componentBGColor = Color.white;
	
	// common button names
	public static String keepBtn    = "Keep";
	public static String cancelBtn  = "Cancel";
	public static String defaultBtn = "Defaults";
}
