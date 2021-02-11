package util.methods;

/***********************************************************
 * Typically, it should only be necessary to call the following for the full command string
 *      getBlastxCmd(String query, String database, String outfile, int nCPU)
 *      getBlastnCmd(String query, String database, String outfile, int nCPU)
 *      getBlastpCmd(String query, String database, String outfile, int nCPU)
 *      getTlastxCmd(String query, String database, String outfile, int nCPU)
 * 
 * However, there are public methods to get the three parts;
 *      name of executable, fixed arguments, options
 *      
 * For formating, use:
 *      protein:    getFormatp(String database)
 *      nucleotide: getFormatn(String database)
 *      
 * diamond: used for nucleotide against protein database
 * 
 * CAS303 any support for old blast is gone. Only blast+ works
 * Three ways of accessing blast and diamond:
 * 1. /external
 * 2. their path
 * 3. HOSTS.cfg diamond_path and blast_path
 */
import java.io.File;
import java.util.Vector;

import util.align.AAStatistics;
import util.database.Globalx;
import util.file.FileHelpers;

public class BlastArgs {  
	public static final String [] validSearchPgms = {"blast", "diamond"};
	static public final String defPgm = "diamond";
	
	// This is the only place that default arguments need to be changed for sTCW.cfg
	// CAS303 add max_hsps 1. CAS314 make all have evalue 1e05, too confusing otherwise
    private static final String blastnOpt =  
    		"-evalue 1e-05 -max_hsps 1  -max_target_seqs 25"; // nt->nt (Pairs)
    private static final String tblastxOpt = 
    		"-evalue 1e-05 -max_hsps 1  -max_target_seqs 25"; // trans nt->nt (Pairs)
    private static final String blastOpt =  
    		"-evalue 1e-05 -max_hsps 1 -max_target_seqs 25"; //  (Same args for all AnnoDB)
    // left off -max_target_seqs as doesn't seem necessary, and so it all fits in textbox 
    private static final String blastpOptORF = "-evalue 1e-05 -max_hsps 1 -soft_masking true -use_sw_tback"; 
    
    // default diamond is --evalue 1e-03; which is ~equivalent with blast 1e-10
    // don't compress as the other search programs don't
    private static final String diamondOpt = "--max-hsps 1";
    // CAS314 private static final String diamondOpt =     "--masking 0 --max-hsps 1 --top 20";
    // used for MTCW comparing ORFs and STCW self comparing ORFS
    private static final String diamondOptORF = "--max-hsps 1 --sensitive";
   
    // kludge because mTCW calls HOSTCfg 4x on startup; stops it printing over and over
    private static boolean hasChecked=false, bBlastExt=true; 
   
    public static void evalBlastPath(String bp, String dp, boolean prt) {
    	if (hasChecked) return; // Called multiple times from mTCW; everything is static for section
    	
     	boolean bBlast = blastSetup(bp);
     	boolean bDmnd =  diamondSetup(dp);
     	
     	if (!bBlast && !bDmnd) {
     		if (bp.equals("") && dp.equals("")) {
     			Out.PrtWarn("Cannot find a search program to use");
     		}
            if (!bp.equals("")) Out.PrtWarn("Incorrect blast_path in HOSTS.cfg");
            if (!dp.equals("")) Out.PrtWarn("Incorrect diamond_path in HOSTS.cfg");
     	}
     	else {
	     	if (!bBlast) {
	     		Out.PrtError("The Blast search programs is not found - it is required except for simply viewing. ");
	     		Out.Print("       TCW expects to find Blast in your path or in the HOSTS.cfg file");
	     	}
	     	// do not change constants diamond and blast as used elsewhere
	     	if (isDiamond) {
	     		searchPgms.add("diamond"); 
	     		if (prt) Out.PrtSpMsg(0, "Diamond path: " + diamondPath);
	     	}
	     	if (isBlast)   {
	     		searchPgms.add("blast");
	     		if (prt) Out.PrtSpMsg(0, "Blast path:  " + blastPath);
	     		bBlastExt = (blastPath.startsWith(Globalx.extDir)) ? true : false;
	     	}
     	}
     	hasChecked=true;
        return;
    }
  
    /**********************************************
     * Diamond
     * 3/3/19 added diamond to external directory 
     ************************************************/
    private static boolean diamondSetup(String userPath) 
    {	
    	if (!userPath.contentEquals("")) {
    		if (pathExists("diamond", userPath, hasChecked)) {
    			userPath += "/diamond";
    			if (fileExec("diamond", userPath, hasChecked)) {
    				diamondPath = userPath;
    				isDiamond = true;
    				return true;
    			}
        	}
    	}
    	
    	String extPath = TCWprops.getExtDir() + "/diamond";
		if (pathExists("diamond_path", extPath, false)) {
			extPath += "/diamond";
			if (fileExec("diamond", extPath, true)) {
				diamondPath = extPath;
				isDiamond = true;
				return true;
			}	
		}
		
		if (FileHelpers.tryExec("diamond", hasChecked)) {
			diamondPath = "diamond";
			isDiamond = true;
			return true;
		}
		
		if (!hasChecked) Out.PrtWarn("Cannot find DIAMOND executable. Read SystemGuide.html");
	 	return false;
	}
    public static boolean isDiamond() {return isDiamond;}
    public static String getDiamondPath() { return diamondPath;}
    public static String getDiamondArgsDB() { return diamondOpt;}
    public static String getDiamondArgsORF() { return diamondOptORF;}
    public static String getDiamondFormat(String database, String outfile) {
		return diamondPath + " makedb --in " + database + " -d " + outfile;
    }
    public static String getDiamondCmd(String query, String database, String outfile, 
    			String action, String blastOp, int nCPU) 
    {
    	String pgmPath = diamondPath + " " + action;
		
		return pgmPath  + " -q " + query + " -d " + database + " -o " + outfile + 
			" " + blastOp + " -p " + nCPU;
    }
    /*****************************************************************
     * Blast
     */
    private static boolean blastSetup(String userPath) 
    {
    	String p="";
		if (!userPath.equals("")) {
			if (pathExists("blast+", userPath, true)) {
				userPath = userPath.trim() + "/";
				if (fileExec("blast+", userPath + "makeblastdb", true)) {
					p = userPath;
					isBlast=true;
				}
			}
		}
		if (!isBlast) {
			String extPath = TCWprops.getExtDir() + "/blast";
			if (pathExists("blast+", extPath, false)) {
				extPath += "/";
				if (fileExec("blast+", extPath + "makeblastdb", true)) {
					p = extPath;
					isBlast = true;
				}	
			}
		}
		if (!isBlast) {
			if (FileHelpers.tryExec("makeblastdb", hasChecked)) {
				isBlast = true;
			}
		}
		if (!isBlast) {
			if (!hasChecked) Out.PrtError("Cannot find Blast directory. Read SystemGuide.html");
		}
		formatp =   p + "makeblastdb -dbtype prot -in ";
		formatn =   p + "makeblastdb -dbtype nucl -in ";
		format =    p + "makeblastdb";
		
		blastxEx =  p + "blastx"; 
        blastpEx =  p + "blastp"; 
        blastnEx =  p + "blastn"; 
        tblastxEx = p + "tblastx";
        blastPath = p;
        
        return true;
    }
    // Called from DoBlast for blastp, blastn, blastx; CAS303 use blastPath
    public static String getBlastCmd(String query, String database, String outfile, String action, String blastOp, int nCPU) {
	 	String pgm = blastPath + action;
	 	
    	return pgm + " -query " + query + " -db " + database + " -out " + outfile + 
	            " -outfmt 6 " + blastOp + " -num_threads " + nCPU + " ";
    }
    
    public static String getBlastPath() { return blastPath; }
    public static String getFormatp(String database) { return formatp + database;}   
    public static String getFormatn(String database) { return formatn + database;}
    
    public static String getBlastArgsDB() 	{return blastOpt; } 
    public static String getBlastArgsORF() {return blastpOptORF; } 
    public static String getBlastnArgs()    {return blastnOpt;}  // blast nt->nt
    public static String getTblastxArgs() 	{return tblastxOpt;} // translated blast nt->nt
    
    public static String getBlastxExec()  {return blastxEx;}
    public static String getBlastnExec()  {return blastnEx;}
    public static String getBlastpExec()  {return blastpEx;}
    public static String getTblastxExec() {return tblastxEx;}
    public static boolean isBlastExt() {return bBlastExt;}
    /*********************************************************************
     * mTCW no CPU, no diamond
     */ 
    private static String getBlastArgs(String query, String database, String outfile, boolean tabular) {
        String arg = "-query " + query + " -db " + database + " -out " + outfile +  " ";
    	if (tabular) arg += " -outfmt 6 "; 
    	return arg;
    } 
  
    public static String getBlastpCmdMTCW(String query, String database, String outfile, boolean tabular) {
        return blastpEx  + " " + getBlastArgsORF() + " " + 
        			getBlastArgs(query, database, outfile, tabular);  
    }
    /****************************************************************
     *  execAssm - blast methods for assembler
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
	    		if (!inQuotedParam) {
	    			if (s.startsWith("\"")) {
	    				inQuotedParam = true;
	    				s = s.replace("\"", "");
	    			}
	    			if (s.contains("\"") || s.contains("'"))
	    				Out.PrtWarn("Unable to process command line argument " + s + " from:\n" + in);
	    			array2.add(s);
	    		}
	    		else
	    		{
	    			int curIdx = array2.size()-1;
	    			String curParam = array2.get(curIdx);
	    			if (s.endsWith("\"")) {
	    				s = s.replace("\"", "");
	    				inQuotedParam = false;
	    			}
	    			if (s.contains("\"") || s.contains("'"))
	    				Out.PrtWarn("Unable to process command line argument " + s + " from:\n" + in);
	    			curParam += " " + s;
	    			array2.set(curIdx, curParam);
	    		}
	    	}
	    	return array2.toArray(new String[0]);
    }
    
    /*****************************************************
     * General
     */
    public static boolean searchPgmExists() {return (isBlast || isDiamond);}
    
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
   
    public static String getArgsDB(String pgm, String action) { // CAS314 was just checking pgm, needed action for blast
    	if (pgm.equals("diamond")) return diamondOpt;
		if (action.equals("blastn")) return blastnOpt;	
		if (action.equals("tblastx")) return tblastxOpt;
		return blastOpt;
    }
    public static String getArgsORF(String pgm) { 
    	if (pgm.equals("diamond")) return diamondOptORF;
		return blastpOptORF;
    }
    public static boolean isNucleotide(String line) {
    	int nt=0, bad=0, len = line.length(); 
		for (int i = 0; i<len; i++) {
			char c = line.charAt(i);
			if (AAStatistics.isDNALetter(c) || c=='n' || c=='N') nt++; 
			else bad++;
		}
		if (nt==len) return true;
		else if (bad <= 5 && len>30) return true;
		else return false;
    }
    public static boolean isProtein(String line) {
    	int bad=0, pr=0, nt=0, len=line.length(); 
		for (int i = 0; i<len; i++) {
			char c = line.charAt(i);
			// CAS314 a little rewrite
			boolean bn = (AAStatistics.isDNALetter(c) || c=='n' || c=='N');
			boolean bx = (AAStatistics.isAcidLetter(c));
			if (bn) nt++;
			if (bx) pr++;
			if (!bn || !bx) bad++;
		}
		if (nt==len) return false; 
		if (pr==len) return true;
		else if (bad<=5 && len>30) return true;
		else return false;
    }
    public static Vector <String> getSearchPgms() {return searchPgms;}
  
    /***************************************************************
     * File routines specific to searching
     */
    private static boolean pathExists(String pgm, String path, boolean prt) {
	    File f = new File(path);
	    if (f.exists() && f.isDirectory()) return true;
	    
	    String                err = "   " +  pgm + ": "+ path + " does not exist";
		if (!f.isDirectory()) err = "   " +  pgm + ": " + path + " is not a valid directory";
		
		if (prt && !hasChecked) Out.PrtWarn(err);
    	return false;
    }
    private static boolean fileExec(String pgm, String file, boolean prt) {
    	String err;
    	File f = new File(file);
    	if (f.exists() && !f.isDirectory()) {
    		if (f.canExecute()) return true;
    		err = "   " + pgm + ": "+ file + " is not executable";
    	}
    	else {
    		err = "   " + pgm + ": "+ file + " does not exist";
    		if (f.isDirectory()) err = "   " + pgm + ": " + file + " is a directory";
    	}
    
		if (prt && !hasChecked) Out.PrtError(err);
		return false;
	} 

    private static boolean isBlast=false, isDiamond=false;
    private static String diamondPath="", blastPath="";
    private static String formatp, formatn, format;
    private static String blastxEx, blastpEx, blastnEx, tblastxEx;
    private static Vector <String> searchPgms = new Vector <String> ();
}
