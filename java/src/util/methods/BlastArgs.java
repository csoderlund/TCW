package util.methods;

/***********************************************************
 * Typically, it should only be necessary to call the following for the full command string
 *      getBlastxCmd(String query, String database, String outfile, int nCPU)
 *      getBlastnCmd(String query, String database, String outfile, int nCPU)
 *      getBlastpCmd(String query, String database, String outfile, int nCPU)
 *      getTlastxCmd(String query, String database, String outfile, int nCPU)
 * as there are three methods to set the options if the user changes the defaults.
 * 
 * However, there are public methods to get the three parts;
 *      name of executable, fixed arguments, options
 * For formating, use:
 *      protein: getFormatp(String database)
 *      nucleotide: getFormatn(String database)
 *      
 * blastn: used by assembler, computing pairs, nucleotide against nucleotide database
 * tblastx: used for computing pairs (6 translation nt against nt)
 * blastx: used for nucleotide against protein database
 * blastp: used for protein sequence against protein database
 */
import java.io.File;
import java.util.Vector;

import util.align.AAStatistics;
import util.database.Globalx;
import util.ui.UIHelpers;

public class BlastArgs {  
	public static final String [] validSearchPgms = {"blast", "diamond", "usearch"};
	
	// This is the only place that default arguments need to be changed for sTCW.cfg
	// NOTE: Older blasts do not have 'max_hsps 1'.
    private static final String plusBlastxOptions =  "-evalue 1e-05 ";
    private static final String plusBlastnOptions =  "-evalue 1e-20 "; // uses megablast by default
    private static final String plusTblastxOptions = "-evalue 1e-10 ";
    
    private static final String plusBlastpOptions =  "-evalue 1e-05 -max_target_seqs 25"; // default is 500
    private static final String plusBlastpOptionsMTCW = plusBlastpOptions + " -soft_masking true -use_sw_tback"; 
    
    // default diamond is --evalue 1e-03; which is ~equivalent with blast 1e-10
    // don't compress as the other search programs don't
    private static final String diamondOptions =     "--masking 0 --max-hsps 1 --top 20";
    private static final String diamondOptionsMTCW = "--masking 0 --max-hsps 1 --sensitive --query-cover 25 --subject-cover 25";
    
    // comparing exBar dataset, 1e-08 gets closer to blast and diamond output
    private static final String usearchOptions = "-evalue 1e-07 -maxhits 25 -trunclabels";
    private static final String usearchSuffix = ".udb";
    
    // kludge because mTCW calls HOSTCfg 4x on startup; stops it printing over and over
    private static boolean hasChecked=false; 
    
    public static void availSearch() {
    		System.err.println("Available search program: ");
    		if (isDiamond) System.err.println("  diamond - for blastx and blastp, annotation, interactive searching");
        if (isUsearch) System.err.println("  usearch - for ORF blastx and blastp, annotation only");
        System.err.println("  blast - assembly, finding pairs, annotation, interactive searching");
    }
   
    /***********************************************
     * User defines the blast or diamond path in HOSTS.cfg
     * The new blast+ is the default if they do not define one.
     * It should find a blast path, and optionally a diamond path
     */
    
    public static void evalBlastPath(String bp, String dp, String up) {
    		searchPgms.clear(); // This gets called multiple times by runMultiTCW, and keeps adding...
     	boolean bNewBlast=false, bExtDmnd=false, bUsearch=false;
     	bNewBlast = newBlast(bp);
     	bExtDmnd = diamond(dp);
     	if (!up.trim().equals(""))		bUsearch = usearch(up);
     	
     	if (!bNewBlast && !bExtDmnd && !bUsearch) {
     		if (bp.equals("") && dp.equals("") && up.equals("")) {
     			Out.PrtWarn("Cannot find a blast program to use");
     			Out.prt("   Try setting the blast path and optional diamond or usearch path in HOSTS.cfg");
     		}
            if (!bp.equals("")) Out.PrtWarn("Incorrect blast_path in HOSTS.cfg");
            if (!dp.equals("")) Out.PrtWarn("Incorrect diamond_path in HOSTS.cfg");
            if (!up.equals("")) Out.PrtWarn("Incorrect usearch_path in HOSTS.cfg");
         
            if (!UIHelpers.isApplet()) System.err.println("");
     	}
     	else {
	     	if (!bNewBlast) {
	     		Out.PrtError("The Blast search programs is not found - it is required. ");
	     		Out.die("       TCW expects to find Blast in your path or in the HOSTS.cfg file");
	     	}
	     	// do not change constants diamond, usearch and blast as used elsewhere
	     	if (isDiamond) searchPgms.add("diamond"); 
	     	if (isBlast)   searchPgms.add("blast");
	     	if (isUsearch) searchPgms.add("usearch");
     	}
     	hasChecked=true;
        return;
    }
  
    /* 3/3/19 added diamond to external directory */
    private static boolean diamond(String userPath) {
    		String extPath = TCWprops.getExtDir() + "/diamond";
		if (pathExists("diamond_path", extPath, false)) {
			extPath += "/diamond";
			if (fileExec("diamond", extPath, true)) {
				diamondPath = extPath;
				isDiamond = true;
				return true;
			}	
		}
		else if (!hasChecked) Out.PrtWarn("Could not find the TCW supplied Diamond executable");
		
    		if (userPath.equals("")) return false;
    		
    		if (!fileExec("diamond", userPath, hasChecked)) {
			if (!userPath.endsWith("diamond")) {
				if (!pathExists("diamond_path", userPath, true)) return false;
				userPath = userPath + "/diamond";
			}
		 	if (!fileExists("diamond", userPath, true)) return false;
    		}
	 	diamondPath = userPath;
	 	isDiamond = true;
	 	return true;
	}
    private static boolean usearch(String p) {
    		if (!fileExec("usearch", p, true)) {
			if (!p.endsWith("usearch")) {
				if (!pathExists("usearch_path", p, true)) return false;
				p = p + "/usearch";
			}
		 	if (!fileExists("usearch", p, true)) return false;
    		}
  	 	usearchPath = p;
  	 	isUsearch = true;
  	 	return true;
  	}
    
    // if path is blank, then makeblastdb should be in the users path
    private static boolean newBlast(String p) {
    		if (!p.equals("")) {
    			if (!pathExists("blast_path", p, false)) p = "";
    			else {
    				p = p.trim() + "/";
    				if (!fileExists("blast+", p + "makeblastdb", false)) p="";
    			}
    		}
    		isBlast = true;
        formatp = p + "makeblastdb -dbtype prot -in ";
        formatn = p + "makeblastdb -dbtype nucl -in ";
        format = p + "makeblastdb";
        blastxEx = p + "blastx"; 
        blastpEx = p + "blastp"; 
        blastnEx = p + "blastn"; 
        tblastxEx = p + "tblastx";
        tblastnEx = p + "tblastn";
         
    		return true;
    }
    /***************************************************************
     * File routines specific to searching
     */
    private static boolean pathExists(String pgm, String path, boolean prt) {
	    	File f = new File(path);
	    	if (f.exists() && f.isDirectory()) return true;
	    
	    	String err = "   The " + pgm + " "+ path + " does not exist";
		if (!f.isDirectory()) err = "   The " + pgm + " " + path + " is not a valid directory";
		
		if (!UIHelpers.isApplet() && prt && !hasChecked) Out.PrtError(err);
    	    return false;
    }
    private static boolean fileExists(String pgm, String file, boolean prt) {
	    	File f = new File(file);
	    	if (f.exists() && !f.isDirectory()) return true;
	    
	    	String err = "   The " + pgm + " "+ file + " does not exist";
		if (f.isDirectory()) err = "   The " + pgm + " " + file + " is a directory";
		
		if (!UIHelpers.isApplet() && prt && !hasChecked) Out.PrtError(err);
		return false;
	}
    private static boolean fileExec(String pgm, String file, boolean prt) {
	    	File f = new File(file);
	    	if (f.exists() && !f.isDirectory() && f.canExecute()) return true;
	    
	    	if (f.isDirectory()) return false;
	    	
	    	String err = "   The " + pgm + " "+ file + " is not executable";
		
		if (!UIHelpers.isApplet() && prt && !hasChecked) Out.PrtError(err);
		return false;
	}
    /*******************************************************
     * General queries
     */
    public static boolean isBlast() {return isBlast;}
    public static boolean foundABlast() 
    {
    		String pgm = getBlastnExec();
	    	try
	    	{
	    		Process p = Runtime.getRuntime().exec(pgm);
	    		p.waitFor();
	    		return true;
	    	}
	    	catch(Exception e){
	    		Out.PrtError("Cannot find " + pgm + " in user's path");
	    		return false;
	    	}
    }

    /****************************************************************
     *  execAssm - methods for assembler
     * **************************************************************/
    public static String getFormatCmd() {
        return format;
    }
    // WMN Megablast translator added for doing TCW assembly steps.
    // It's not fully complete because "-i", "-o" are not included, due to the way the assembler's blast threading is set up (they
    // are added later). Also it assumes all arguments are present and it adds "-FF" always. 
    public static String getAsmMegablastArgs(String database, String eval, float pctID, int score, String moreParams) {
	    return blastnEx +  " -db " +  database + " -evalue " + eval + " -dust no " + " -perc_identity " + pctID +
	    		" -min_raw_gapped_score " + score + " " + moreParams +
	    		" -outfmt 6 ";
    }
    // Add in/out file to complete megablast command line (used in assembler)
    public static String getAsmInOutParams(String inFile, String outFile) {
	    	return " -query " + inFile + " -out " + outFile;
    }
    // WN for some of the assembler exec calls, need to have a tokenized command line. This function
    // should be pretty general, in particular it handles quoted parameters correctly. 
    // Note, there must be a space between each flag and its argument, even if quoted, e.g. -X "Y Z".
    // (Actually this algorithm collapses multiple spaces within the quoted parameter, but it's good enough for blast)
    public static String[] cmdLineToArray(String in) {
	    	String[] array1 = in.trim().split("\\s+"); // step 1, split on spaces
	    	Vector<String> array2 = new Vector<String>(); // step2, put back together the ones starting/ending with "
	    	boolean inQuotedParam = false;
	    	for (String s : array1)
	    	{
	    		if (!inQuotedParam)
	    		{
	    			if (s.startsWith("\""))
	    			{
	    				inQuotedParam = true;
	    				s = s.replace("\"", "");
	    			}
	    			if (s.contains("\"") || s.contains("'"))
	    			{
	    				Out.PrtWarn("Unable to process command line argument " + s + " from:\n" + in);
	    			}
	    			array2.add(s);
	    		}
	    		else
	    		{
	    			int curIdx = array2.size()-1;
	    			String curParam = array2.get(curIdx);
	    			if (s.endsWith("\""))
	    			{
	    				s = s.replace("\"", "");
	    				inQuotedParam = false;
	    			}
	    			if (s.contains("\"") || s.contains("'"))
	    			{
	    				Out.PrtWarn("Unable to process command line argument " + s + " from:\n" + in);
	    			}
	    			curParam += " " + s;
	    			array2.set(curIdx, curParam);
	    		}
	    	}
	    	return array2.toArray(new String[0]);
    }
    
    /**********************************************************************
     *  execAnno - methods for annotater
     * ********************************************************************/
    public static boolean blastExists() {return (isBlast || isDiamond);}
    
    /** diamond **/
    
    public static boolean isDiamond() {return isDiamond;}
    public static String getDiamondPath() { return diamondPath;}
    public static String getDiamondOpDefaults() { return diamondOptions;}
    public static String getDiamondOpDefaultsMTCW() { return diamondOptionsMTCW;}
    public static String getDiamondFormat(String database, String outfile) {
		return diamondPath + " makedb --in " + database + " -d " + outfile;
    }
    public static String getDiamondCmd(String query, String database, String outfile, 
    			String action, String blastOp, int nCPU) 
    {
    		String pgmPath = diamondPath + " blastx ";
		if (action.equals("blastp")) pgmPath = diamondPath + " blastp ";
		return pgmPath  + " -q " + query + " -d " + database + " -o " + outfile + 
			" " + blastOp + " -p " + nCPU;
    }
    
    /** usearch **/
    public static boolean isUsearch() {return isUsearch;}
    public static String getUsearchOpDefaults() {return usearchOptions;}
    public static String getUsearchFormat(String database, String outfile) {
    		outfile += usearchSuffix;
		return usearchPath + " -makeudb_ublast " + database + " -output " + outfile;
    }
    public static String getUsearchCmd(String query, String database, String outfile, String op) {
    		database += usearchSuffix; 
    		// requires an evalue
    		String uOp = (op.contains("evalue") && !op.contains("max_")) ? op : usearchOptions; 
    		return usearchPath + " -ublast " + query + " " + uOp + " -db " + database + " -blast6out " + outfile;
    }
 
    /** blast **/
    // Called from DoBlast for blastp, blastn, blastx
    public static String getBlastCmd(String query, String database, String outfile, String action, String blastOp, int nCPU) {
	 	return action + " -query " + query + " -db " + database + " -out " + outfile + 
	            " -outfmt 6 " + blastOp + " -num_threads " + nCPU + " ";
    }
    
    public static String getFormatp(String database) { return formatp + database;}   
    public static String getFormatn(String database) { return formatn + database;}
    // arguments the same for blastx, blastn and tblastx 
    private static String getBlastArgs(String query, String database, String outfile, int nCPU) {
        return " -outfmt 6 -query " + query + " -db " + database + " -out " + outfile + " -num_threads " + nCPU + " ";
    }
    
    /** blastx nt to aa **/
    public static String getBlastxOpDefaults() {return plusBlastxOptions; }
   
    public static String getBlastxCmd(String query, String database, String outfile, int nCPU) {
        return  blastxEx  + " " + plusBlastxOptions + " " + getBlastArgs(query, database, outfile, nCPU);  
    }
    
    /** blastp aa to aa **/
    public static String getBlastpOpDefaults() {return plusBlastpOptions; }
  
    public static String getBlastpOpDefaultsMTCW() {
	    return plusBlastpOptionsMTCW; 
	}
    
    /** blastn nt to nt **/
    public static String getBlastnExec() {return blastnEx;}
    public static String getBlastnOptions() {return plusBlastnOptions;}
   
    public static String getBlastnOpDefaults() {return plusBlastnOptions; }
    
    /** tblastx nt to nt with 6 frame translation - used in runSTCW homology test**/
    public static String getTblastxOptions() {return plusTblastxOptions;}
    
  
    /*********************************************************************
     * mTCW no CPU, no diamond
     */ 
    private static String getBlastArgs(String query, String database, String outfile, boolean tabular) {
        String arg = "-query " + query + " -db " + database + " -out " + outfile +  " ";
    		if (tabular) arg += " -outfmt 6 "; 
    		return arg;
    } 
  
    public static String getBlastpCmdMTCW(String query, String database, String outfile, boolean tabular) {
        return blastpEx  + " " + getBlastpOpDefaultsMTCW() + " " + 
        			getBlastArgs(query, database, outfile, tabular);  
    }
    public static String getSearch() {
    		if (isDiamond) return "diamond";
    		if (isBlast) return "blast";
    		if (isUsearch) return "usearch";
    		Out.die("No valid search program");
    		return "";
    }
    public static String getParams(String pgm) {
    		if (pgm.equals("diamond")) return diamondOptions;
    		if (pgm.equals("usearch")) return usearchOptions;
		if (pgm.equals("blastx")) return plusBlastxOptions;
		if (pgm.equals("blastn")) return plusBlastnOptions;	
		return plusBlastpOptions;
    }
    public static boolean isNucleotide(String line) {
    		int nt=0, bad=0, len = line.length(); 
		for (int i = 0; i<len; i++) {
			char c = line.charAt(i);
			if (AAStatistics.isDNALetter(c) || c=='n' || c=='N') nt++; 
			else if (c==Globalx.gapCh || c=='*' || c==' ') nt++;
			else bad++;
		}
		if (nt==len) return true;
		else if (bad<= 5&& len>30) return true;
		else return false;
    }
    public static boolean isProtein(String line) {
    		int bad=0, pr=0, nt=0, len=line.length(); 
		for (int i = 0; i<len; i++) {
			char c = line.charAt(i);
			if (AAStatistics.isDNALetter(c) || c=='n' || c=='N') nt++;
			if (AAStatistics.isAcidLetter(c)) pr++;
			else if (c==Globalx.gapCh || c=='*' ||  c==' ') pr++;
			else bad++;
		}
		if (nt==len) return false; 
		if (pr==len) return true;
		else if (bad<=5 && len>30) return true;
		else return false;
    }
    public static Vector <String> getSearchPgms() {return searchPgms;}
    public static String [] getValidSearchPgms() {return validSearchPgms;}
    public static Vector <String> getValidSearchPgmVec() {
    		Vector <String> arr = new Vector <String>();
    		for (int i=0; i<validSearchPgms.length; i++) arr.add(validSearchPgms[i]);
    		return arr;
    }
  
    private static boolean isBlast=false, isUsearch=false, isDiamond=false;
    private static String usearchPath="";
    private static String diamondPath="";
    private static String formatp, formatn, format;
    private static String blastxEx, blastpEx, blastnEx, tblastxEx, tblastnEx;
    private static Vector <String> searchPgms = new Vector <String> ();
}
