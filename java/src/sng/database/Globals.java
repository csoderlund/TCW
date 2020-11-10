package sng.database;

// This is just for sTCW; mTCW has its own Globals.java
// The shared Globals are being merged into Globals.java

import java.awt.Color;
import java.awt.Font;

import util.database.Globalx;

public class Globals {
	public final static String annoFile = "anno.log";
	public final static String loadFile = "load.log";
	public final static String assmFile = "inst.log";
	
	public static final String OLDLIBDIR = 	"libraries/";
	public final static String seqFile = "sequences.fa"; // write sequences for blasting
	
	public static final String STCWCFG = "sTCW.cfg";
	public static final String LIBCFG = 	"LIB.cfg";
	public static final String STCW = 	Globalx.STCW; // database prefix
	
	public static final String helpDir = "html/viewSingleTCW/";
	
	public static final String 	gapStr = Globalx.gapStr;
	public static final char 	gapCh = Globalx.gapCh;
	
	public static final String def5p = ".f";
	public static final String def3p = ".r";
	
	public static final int SHORT_UTR = 10;
	
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
	
	public static final int numDB = 40; // number of annoDBs
	
	public static final String LIBCNT =  Globalx.LIBCNT;
	public static final String LIBRPKM = Globalx.LIBRPKM;
	public static final String PVALUE =  Globalx.PVALUE; 
	
	static public final String GO_FORMAT = "GO:%07d";
	
	static public final String tcwDelim = ";"; // If change, change in AddRemarkPanel
	public static final String ROW_DELIMITER = "\t";
	
	// For sequence remark column - all assigned in DoORF except MultiFrame, which is assigned in DoUniProt
	static public final String RMK_MultiFrame = 		"Multi-frame";
	static public final String RMK_HIT_hitSTOP = 	"StopsIn";
	static public final String RMK_ORF_exact =     	"ORF=Hit";
	static public final String RMK_ORF_gtHit =  		"ORF>Hit";
	static public final String RMK_ORF_ltHit = 		"ORF<Hit"; 
	static public final String RMK_ORF_appxHit =  	"ORF~Hit";
	static public final String RMK_ORF_NOTLONG = 	"ORF!LG";
	static public final String RMK_ORF_NOTMarkovBest3 = 	"ORFmk!b!g"; // !best !good
	static public final String RMK_ORF_NOTMarkovBest =   "ORFmk!b";  // !b is good
	static public final String RMK_ORF_NOTMarkov3 =      "ORFmk!g";  // is best !good
	static public final String RMK_ORF_NOTHit = 		"ORF!Hit";
	static public final String RMK_ORF_Ns = 			"ORF>=9Ns";
	static public final String RMK_ORF_ANNO = 		"ORF:AN";
	static public final String ORF_MARK = "*";
	static public final String ORF_NoMARK = "-";
	
	// Anno Option parameters - put in one place
	static public final String pHIT_EVAL = "1E-10";
	static public final String pHIT_SIM = "20";
	static public final String pDIFF_LEN = "0.5";
	static public final String pTRAIN_MIN = "50";
	static public final String pSP_PREF = "0";
	static public final String pRM_ECO = "1";
}
