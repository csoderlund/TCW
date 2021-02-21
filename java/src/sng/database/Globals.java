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
	
	public static final String STCWCFG = "sTCW.cfg";
	public static final String LIBCFG =  "LIB.cfg";
	public static final String STCW = 	Globalx.STCW; // database prefix
	
	public static final String helpDir = "html/viewSingleTCW/";
	public static final String helpRunDir = "html/runSingleTCW/";
	
	public static final String 	gapStr = Globalx.gapStr;
	public static final char 	gapCh = Globalx.gapCh;
	
	public static final String def5p = ".f";
	public static final String def3p = ".r";
	
	public static final Color BGCOLOR = Color.WHITE;
	public static final Color FUNCTIONCOLOR = Globalx.FUNCTIONCOLOR; // replace with no color
	public static final Color MENUCOLOR = Globalx.MENUCOLOR;	// beige
	public static final Color HELPCOLOR = Globalx.HELPCOLOR;	// rose
	public static final Color PROMPTCOLOR = Globalx.PROMPTCOLOR;	// light beige
	public static final Color LAUNCHCOLOR = Globalx.LAUNCHCOLOR; // light purple
	
	public static final Color altRowColor = Globalx.altRowColor;
	public static final Color selectColor = Globalx.selectColor;
	public static final Font boldUIFont = Globalx.boldUIFont;
	public static final Font textFont = Globalx.textFont;
	
	public static final int numDB = 40; // number of annoDBs
	
	public static final String LIBCNT =  Globalx.LIBCNT;
	public static final String LIBRPKM = Globalx.LIBRPKM;
	public static final String PVALUE =  Globalx.PVALUE; 
	
	static public final String GO_FORMAT = "GO:%07d";
	
	static public final String tcwDelim = ";"; // If change, change in AddRemarkPanel
	public static final String ROW_DELIMITER = "\t";
	
	// For sequence remark column - all assigned in DoORF except MultiFrame, which is assigned in DoUniProt
	static public final String RMK_MultiFrame = 	"Multi-frame";
	static public final String RMK_HIT_hitSTOP = 	"StopsIn";
	static public final String RMK_ORF_exact =     	"ORF=Hit";
	static public final String RMK_ORF_gtHit =  	"ORF>Hit";
	static public final String RMK_ORF_ltHit = 		"ORF<Hit"; 
	static public final String RMK_ORF_appxHit =  	"ORF~Hit";
	static public final String RMK_ORF_NOTLONG = 	"ORF!LG";
	static public final String RMK_ORF_NOTMarkovBest3 = "ORFmk!b!g"; // !best !good
	static public final String RMK_ORF_NOTMarkovBest =   "ORFmk!b";  // !b is good
	static public final String RMK_ORF_NOTMarkov3 =      "ORFmk!g";  // is best !good
	static public final String RMK_ORF_NOTHit = 		"ORF!Hit";
	static public final String RMK_ORF_Ns = 			"ORF>=9Ns";
	static public final String RMK_ORF_ANNO = 		"ORF:AN";
	static public final String ORF_MARK = Globalx.stopStr;
	static public final String ORF_NoMARK = "-";
	
	// Anno Option parameters - put in one place
	static public final String pHIT_EVAL = "1E-10";
	static public final String pHIT_SIM = "20";
	static public final String pDIFF_LEN = "0.5";
	static public final String pTRAIN_MIN = "50";
	static public final String pSP_PREF = "0";
	static public final String pRM_ECO = "1";
	
	public static boolean hasVal(String val) {
		if (val==null || val.trim().equals("")|| val.trim().equals("-")  ) return false;
		return true;
	}
}
