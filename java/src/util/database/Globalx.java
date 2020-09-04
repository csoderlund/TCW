package util.database;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;

// shared by sTCW and mTCW
// They both have their own database/Globals.java
public class Globalx {
	public static final String strRelDate = "(4-Sept-20)"; 
	public static final String strTCWver = "3.0.4";
	public static final String URL = "http://www.agcol.arizona.edu ";
	public static final String TCWhead = "TCW v" + strTCWver + " " + strRelDate;
	public static final String error="***";
	
	public final static String extDir = "Ext"; // CAS303 changed from external and external_osx
	public final static String lintelDir = extDir + "/linux";
	public final static String macDir = extDir + "/mac";
	
	public final static String diamondSuffix = ".dmnd";
	
	public static final String HOSTS = 		"HOSTS.cfg";
	public static final String STCW 	= 	"sTCW_"; 
	public static final String MTCW 	= 	"mTCW_"; 
	
	// on MAC, directory names are case in-sensitive
	public static final String LIBDIR 	= "projects"; // 11/18/18 libraries directory merged with projects
	public static final String PROJDIR 	= "projects";
	public static final String CMPDIR 	= "projcmp";
	public static final String RDIR 		= "R-scripts";
	
	// Under projects directory
	public static final String ANNOSUBDIR =  "DBfasta";
	public static final String ANNODIR 	= 	 PROJDIR + "/DBfasta";
	public static final String USERDIR = 	"Userfiles";   // under projects directory for user
	public static final String HTMLDIR = 	"OverviewHTML";
	
	// for specific project
	public final static String logDir  	=   "logs";
	public static final String BLASTDIR = 	"hitResults";  // runSingle: results from annotation; runMulti: self-blast
	public static final String ORFDIR = 	"orfFiles";
	
	// TCW directory for viewSingle and viewMulti to put results the user can view
	public static final String HITDIR =   	"ResultHits"; // viewSingle: sequences are written and blasts occur
	public static final String ALIGNDIR = 	"ResultAlign"; // viewMulti: results of multi align 
	public static final String EXPORTDIR= 	"ResultExport";
	
	public static final String gapStr = "-";
	public static final char gapCh = '-';
	
	public static final String CSV_DELIM = "\t";
	public static final String CSV_SUFFIX = ".tsv"; 
	public static final String FASTA_SUFFIX = ".fa";
	public static final String TEXT_SUFFIX = ".txt";
	
	public static final int numDB = 40; // number of annoDBs
	
	public static final String LIBCNT =  "L__";
	public static final String LIBRPKM = "LN__";
	public static final String PVALUE =  "P_"; // note just one dash
	
	// Its messy about what the values of 'no value' where
	// See Stats.average - 
	public static final String sNoVal = "-";
	public static final String sNullVal = "--";
	public static final double dNullVal =        -1.5; 
	
	public static final String iStrNoVal = 		"-2";
	public static final int    iNoVal = 			-2; 
	
	public static final String dStrNoVal = 		"-2.0"; 
	public static final double dNoVal = 			-2.0; 
	
	public static final String dStrNoDE = 		"3.0"; // set in QRprocess
	public static final double dNoDE =  			3.0;   // may also be a -3
	public static final double dNaDE		=		3.0;   // DE computation returns NA
	
	public static final String frameField = "Frame";
	public static final String scoreField = "Score"; //
	public static final float  dNoScore = -100000; // Float.MAX_VALUE does not work
	
	static public final String GO_FORMAT = "GO:%07d";
	static public final String GO_FORMAT_FNAME = "GO_%07d";
	// the ECs are in MetaData. MetaData checks database for GO_TERMS, but not used
	// multiple places use substring(0,4) to get abbr
	static public final String [] GO_TERM_LIST = {"biological_process","cellular_component","molecular_function"};
	static public final String [] GO_TERM_ABBR = {"biol", "cell", "mole"};
	
	static public final String tcwDelim = ";";
	public static final String ROW_DELIMITER = "\t";
	
	// ISSUB
	public static final String blosumPos = "Substitution >0";
	public static final String blosumNeg = "Substitution<=0";
	public static final String blosumPosLegend = "Substitution >0 (i.e. BLOSUM62)";
	public static final String AA_POS ="+"; // aa pos sub
	public static final String AA_NEG =" "; // aa neg sub
	public static final char cAA_POS ='+'; // aa pos sub
	public static final char cAA_NEG =' '; // aa neg sub
	
	public static final Color BGCOLOR = Color.WHITE;
	public static final Color FUNCTIONCOLOR = new Color(215, 229, 243); // replace with no color
	public static final Color MENUCOLOR = new Color(229, 245, 237);	// beige
	public static final Color HELPCOLOR = new Color(245, 213, 234);	// rose
	public static final Color PROMPTCOLOR = new Color(243, 235, 227);	// light beige
	public static final Color LAUNCHCOLOR = new Color(200, 200, 240); // light purple
	
	public static final Color altRowColor = new Color(0xEEF2FF);
	public static final Color selectColor = new Color(0xB0C4DE);
	public static final Font boldUIFont = new Font("Sans", Font.BOLD, 12);
	public static final Font textFont = new Font("Sans", Font.PLAIN, 11);
	public static final Color componentBGColor = Color.white;
	
	// For drawing
	public static final Color mismatchRed = new Color(255, 173, 190); // Color.red;
	public static final Color lowQualityBlue = Color.blue;	
	public static final Color gapGreen = new Color(0,200,0);
	public static final Color darkGreen = Color.green; //new Color(0,102,0);
	public static final Color lightGray =  new Color(230, 230, 230);
	public static final Color mediumGray =  new Color(180, 180, 180);
	public static final Color purple = new Color(138, 0, 184); 
	
	public static final Color colorORF = Color.YELLOW;
	public static final Color colorHIT = new Color(157, 189, 242);
	
	public DecimalFormat df = new DecimalFormat("#,###,###");
}
