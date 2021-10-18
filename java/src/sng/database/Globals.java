package sng.database;

/***************************************************
 *  This is just for sTCW; mTCW has its own Globals.java
 *  The shared Globals are being merged in util.databasel.Globalx.java
 */

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
	
	static public final String seqTableLabel = "Seq Table"; // CAS334 Changed label everywhere
	static public final String seqDetailLabel = "Seq Detail"; // CAS334 ditto
	static public final String pairTableLabel = "Pair Table"; // CAS334 ditto
	
	// For sequence remark column - all assigned in DoORF except MultiFrame, which is assigned in DoUniProt
	
	static public final String RMK_MultiFrame = 		"Multi-frame";
	static public final String RMK_HIT_hitSTOP = 		"StopHit"; // appends #stop
	static public final String RMK_ORF_exact =     		"ORF=Hit";
	static public final String RMK_ORF_exact_END	=	"+";
	static public final String RMK_ORF_no5 =     		"!5";   // CAS334 new, no Start
	static public final String RMK_ORF_no3 =     		"!3";   // CAS334 new, no Stop
	static public final String RMK_ORF_gtHit =  		"ORF>Hit";
	static public final String RMK_ORF_ltHit = 			"ORF<Hit"; 
	static public final String RMK_ORF_appxHit =  		"ORF~Hit";
	static public final String RMK_ORF_NOTLONG = 		"ORF!Lg";
	// GOOD: A good Markov score is positive and the best score for all frames of the given ORF (see the Scores/AA display). 
	// BEST: This is in contrast to the best Markov score, which is the best over all ORFs shown in the ORFs/NT display.
	// CAS326  just report if !best
	static public final String RMK_ORF_MarkovNotBest =  "ORF!Mk"; 
	static public final String RMK_ORF_NOTHit = 		"ORF!Hit";
	static public final String RMK_ORF_ALL	=			"$";
	
	static public final String RMK_ORF_Ns = 			"ORF>=9Ns";
	static public final String RMK_ORF_ANNO = 			"ORF:AN";
	static public final String ORF_MARK = 				Globalx.stopStr;
	static public final String ORF_NoMARK = 			"-";
	
	// Anno Option parameters - put in one place
	static public final String pHIT_EVAL = "1E-10";
	static public final String pHIT_SIM = "20";
	static public final String pDIFF_LEN = "0.5"; 
	static public final String pDIFF_MK = "0.4"; // CAS334 new
	static public final String pTRAIN_MIN = "500"; // CAS334 changed from 50
	static public final String pSP_PREF = "0";
	static public final String pRM_ECO = "1";
	static public final String pPRUNE = "0"; // Also change in AnnoOption Defaults
	static public final String pNO_GO = "0";
	
	public static boolean hasVal(String val) {
		if (val==null || val.trim().equals("")|| val.trim().equals("-")  ) return false;
		return true;
	}
}
