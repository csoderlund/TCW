package sng.annotator;

import java.io.*;
import java.util.Vector;
import java.util.HashSet;

import sng.database.Globals;
import sng.database.Overview;
import sng.database.Schema;
import sng.database.Version;
import sng.dataholders.BlastHitData;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;

/**
 * Main routine - called from execAnno or from runSingleTCW on annotate 
 * 	annotate a specified dataset using its sTCW.cfg parameters.
 * 	2/6/19 rename CoreMain to runSTCWMain
*/
public class runSTCWMain 
{	
	private static String blastDir = Globalx.pHITDIR;  // seqs are written and search is run
	private static final String ProjDirName = Globalx.PROJDIR + "/";
	
	// command line parameters
	public static boolean doRecalcORF=false;
	public static boolean bDelAnno = false; // set to delete annotation
	
	private static boolean bdoAnno=true; // don't print parameters 
	private static boolean doGO = false, doPrune=false;
	private static boolean doDeleteAnnoOnly=false, doAnnoDBParams=false, doDeletePairwise=false;
	
	public static final String optGO =  "-g -n";		// used by the manager for GO only
	public static final String optORF = "-r -n";		// used by the manager for ORF only
	public static final String optRM  = "-s -n";		// used by the manager for Hit Rm only
	
	// if -r, then the following 5 can be set
	private static int ORF_Type_Param=0, ORF_Transcoder_Param=0, ORF_Filter= -1;
	private static int ORF_Seq_Cov= -1, ORF_Hit_Cov= -1;
	
	// if -o overview, then -o1 and -o2 can be set.
	private static int cover1=0, cover2=0; 
	
	// if -p, then the following can be set
	private static int pruneType=-1, nPrtPrune=0;
	private static boolean bRestore=false;
	
	public static void main(String[] args)
	{   
		if (!Version.checkJavaSupported())
			System.exit(-1);
			
		/**  Parameter processing */
		if (hasOption(args, "-h") 
				|| hasOption(args, "-help") 
				|| hasOption(args, "help")
				|| args.length == 0) {
			usage_exit();
		}
		projName = args[0]; 
		projPath = getCurProjPath();
		String path = getCurProjPath();
		if (path==null || !FileHelpers.existDir(path)) {
			Out.die("Project does not exist: " +projName);
		}
		System.out.println("Project: " + projName);
		long startTime = Out.getTime();
		
		if (args.length>1) {
			usage_setArgs(args);
			if (contMsg!=null) 
				if (!yesNo(contMsg)) Out.die("Terminate");
		}
		
		hostsObj = new HostsCfg(); // Read HOSTS.cfg
		
	// 1. create objects used for annotation
		uniObj =  new DoUniProt();
		annoObj = new CoreAnno();	
		orfObj =  new DoORF();
		blastObj = new DoBlast(); 
		if (noPrompt) blastObj.setNoPrompt();

	// 2. read LIB.cfg and sTCW.cfg and set params in respective object
		cfgObj = new CfgAnno(); 
		if (! cfgObj.load(bdoAnno, doRecalcORF, doGO, doPrune, blastObj, annoObj, uniObj, orfObj)) { 
			Out.close();		  // don't die, just return
			return;
		}
		
		// 3. create log file
		if (bdoAnno || doGO || doRecalcORF || doPrune) {
			Out.prtHeader("Annotate Sequences");
			Out.createLogFile(getCurProjPath(), Globals.annoFile);
		}
		
		// 4. create database connection
		try {
			Out.Print("\nCheck database " + stcwDB);
			mDB = hostsObj.getDBConn(stcwDB);
			if (mDB==null) ErrorReport.die("Error creating database connection for " + stcwDB);
			
			Schema s = new Schema(mDB);
			s.update(); // CAS314 was checking ifCurrent(), but then cannot 'force' update for testing
						
			sqlObj = new CoreDB(mDB, bdoAnno, stcwDB, stcwID);
			isAAstcwDB = sqlObj.setIsAAtcw();
			sqlObj.setLongestSeq();
		}
		catch (Exception e) {ErrorReport.die(e, "Error creating database connection for " + stcwDB);}
		
		annoObj.setMainObj(uniObj, blastObj, sqlObj, mDB);
		uniObj.setMainObj(annoObj, blastObj, mDB);
		blastObj.setMainObj(uniObj, annoObj, sqlObj, stcwID);		
		
		/** 5. all checks **/
		try {
			if (doDeleteAnnoOnly) {
				sqlObj.deleteAnnotation(true);
				Out.PrtMsgTime("\nEnd annotation for " + projName, startTime);
				finish();
				return;
			}
			if (doDeletePairwise) {
				sqlObj.deletePairwise();
				Out.PrtMsgTime("\nEnd annotation for " + projName, startTime);
				finish();
				return;
			}
			
			if (doGO) {
				checkGODB(true, true);
			}
			else if (doPrune) {
				if (!checkGODB(false, false))
					if (!yesNo("GO database not defined -- cannot use to determine best -- continue?"))
						System.exit(0);
			}
			else if (bdoAnno) { // not just doing GO or summary
			
				if (!blastObj.testFilesAndcreateOutFiles()) {
					finish();
					Out.die(" failed testing files");
				}
				boolean firstAnno = sqlObj.isFirstAnno();
				boolean doAnnoDB = 	(blastObj.numDB() > 0) ? true : false;
				boolean doSelf =    blastObj.doPairs();
				boolean hasGO = 	sqlObj.existsGO();
				
				if (!firstAnno && !doAnnoDB && !doSelf) { 
					Out.Print("No annoDB or pairs annotation to be done\n");
					doRecalcORF = yesNo("Perform GC and ORF computations?"); // CAS334 changed from Out.yesNO 
				}
				if (!sqlObj.isAAtcw()) { 
					if (blastObj.numDB()==0 && !doRecalcORF) {
						if (!sqlObj.hasORFs()) 
							doRecalcORF = yesNo("Perform GC and ORF computations?"); // CAS334 added check
					}
				}
				
				doGO = false;
				if (doAnnoDB || !hasGO) { 
					if (!goNoAdd) { // CAS331
						Out.Print("Check GO database ");
						if (checkGODB(false, true)) doGO=true;
					}
				}
		
				Boolean ans = yesNo("Please confirm above parameters. Continue with annotation? ");
				if (!ans)  {
					finish();
					Out.die("by user request");
				}
			}
		}
		catch (Exception e) {}

	/*** Finally do executions ***/
			
		if (bDelAnno) 
			if (!sqlObj.deleteAnnotation(true)) {
				finish();
				return;	
			}
		
		if (doAnnoDBParams) {
			if (!cfgObj.cfgDBParams(true)) {
				finish();
				return; // weren't read earlier because bdoAnno=false
			}
			uniObj.updateAnnoDBParams();
		}
		
		boolean successAnno=true; 
		if (bdoAnno || doRecalcORF || doGO || doPrune) updateState(false); // set null in case interrupt
		
		if (bdoAnno || doRecalcORF) {
			BlastHitData.startHitWarnings("Warnings for " + projName);
			if (!bdoAnno && doRecalcORF) 
				orfObj.setCmdParam(ORF_Type_Param, ORF_Transcoder_Param, ORF_Filter, ORF_Seq_Cov, ORF_Hit_Cov);
		
			successAnno = annoObj.run(getCurProjPath(), orfObj);
		}
		if (successAnno) {
			String msg=null;
			if (doPrune) {
				int pt = (pruneType!=-1) ? pruneType : uniObj.getPrune();
				new DoUniPrune(mDB, godbName, isAAstcwDB, uniObj.getUseSP(), 
						uniObj.getFlank(), pt, bRestore, nPrtPrune); // CAS331
			}
			else if (doGO) {
				new DoGOs(sqlObj,godbName, goSlimSubset, goSlimOBOFile); // dies if anything fails
			}
			else if (bdoAnno && sqlObj.existsGO()) 
				msg ="GO annotations exist. Update with 'Exec GO only' if new UniProt annoDBs were added.";	
			
			// 8. Create Overview -- file is written in overview.java 
			try {
				if (bdoAnno || doRecalcORF || doGO || doPrune) updateState(true); // CAS319 add here
				
				Overview viewObj;
				if (cover1>0 || cover2>0) viewObj = new Overview(mDB, cover1, cover2);
				else viewObj = new Overview(mDB);
				
				viewObj.createOverview(new Vector <String> () );
			}
			catch (Exception e) {}
			if (msg!=null) Out.PrtWarn(msg); // so is seen at the end

			Out.PrtMsgTime("\nEnd annotation for " + projName, startTime);
		}
		else { 
			if (doGO) Out.PrtWarn("Skip GO due to annotation errors");
			Out.PrtMsgTime("\nIncomplete annotation for " + projName, startTime);
		}
		
		Out.closeSize();
		finish();
	}
	static private boolean finish() {
		if (mDB!=null) {  
			try {mDB.close(); } catch (Exception e) { return false;}
		}
		Out.close();
		return true;
	}
	/*********************************************
	 * CAS319 These were all over the place. Used in Overview only
	 */
	static private void updateState(boolean bSuccess) {
		try {
			mDB.executeUpdate("update assem_msg set pja_msg = NULL where AID = 1");
			
			mDB.executeUpdate("update schemver set annoVer='" +  Globalx.strTCWver + "'");
			mDB.executeUpdate("update schemver set annoDate='" + Globalx.strRelDate + "'");
			
			if (bSuccess) 
				 mDB.executeUpdate("UPDATE assembly SET annotationdate=(CAST(NOW() as DATE)) WHERE AID=1");	
			else mDB.executeUpdate("UPDATE assembly SET annotationdate=null WHERE AID=1");	
		}
		catch (Exception e) {ErrorReport.reportError(e, "Updating annotation verion");}
	}
	/******************************************************
	 * set options/actions
	 */
	private static void usage_setArgs(String [] args) {		
		HashSet <String> flags = new HashSet <String> (); 
		flags.add("-n"); flags.add("-d");  
		flags.add("-p"); flags.add("-pt"); flags.add("-pr"); flags.add("-pp");
		flags.add("-g");
		flags.add("-o"); flags.add("-o1"); flags.add("-o2");
		flags.add("-r"); flags.add("-t");  flags.add("-f");  flags.add("-ff"); flags.add("-hc");flags.add("-sc"); 
		flags.add("-s"); flags.add("-x");  flags.add("-b");
		
		for (int i=0; i<args.length; i++) {
			if (args[i].startsWith("-")) {
				if (!flags.contains(args[i])) {
					if (!yesNo("Illegal argument '" + args[i] + "' - continue?")) 
						Out.die("Terminate");
				}
			}
		}
		
		if (hasOption(args, "-n")) {
			Out.prt("There will be no prompts");
			noPrompt=true;
		}
		if(hasOption(args, "-d")) {
			Out.prt("Set global debug on");
			Globalx.debug = true;
		}
		// only one of the following
		if(hasOption(args, "-o")) { 
			contMsg = "Update overview -- continue?";
			if (hasOption(args, "-o1")) {
				cover1 = getOptionNum(args, "-o1");
				if (cover1<0) Out.die("-o1 requires an integer following this flag");
				else          Out.prt("   Cover1: " + cover1);
			}
			if (hasOption(args, "-o2")) {
				cover2 = getOptionNum(args, "-o2");
				if (cover2<0) Out.die("-o2 requires an integer following this flag");
				else          Out.prt("   Cover2: " + cover2);
			}
			bdoAnno=false; 
		}
		else if (hasOption(args, "-b")) {
			contMsg = "Enter the search program params (for after a hit file reload)";
			bdoAnno=false; 
			doAnnoDBParams = true;
		}
		else if(hasOption(args, "-x")) { 
		    contMsg = "Delete annotation -- continue?";
			bdoAnno=false;
		    doDeleteAnnoOnly = true;
		}	
		else if (hasOption(args, "-s")) {
			contMsg = "Delete pairwise - continue?";
			bdoAnno=false;
			doDeletePairwise = true;
		}
		else if(hasOption(args, "-g")) { // used from runSingleTCW also
		    contMsg = "Compute GOs only - continue?";
			bdoAnno=false;
			doGO = true;
		}
		else if(hasOption(args, "-p")) { // used from runSingleTCW also
		    contMsg = "Prune redundant hits - continue?";
		    if (hasOption(args, "-pt")) {
				pruneType = getOptionNum(args, "-pt");
				
				if (pruneType==1)      Out.prt("   Prune hits based on same alignment values and description");
				else if (pruneType==2) Out.prt("   Prune hits based on same description");
				else if (pruneType==0) Out.prt("   No pruning - only restore if -pr is set"); // only restore
				else Out.die("-pt requires an integer between 0 and 2");
			}
			if (hasOption(args, "-pr")) {
				Out.prt("   Save/restore hit tables");
				bRestore=true;
			}
			if (hasOption(args, "-pp")) {
				nPrtPrune = getOptionNum(args, "-pp");
				Out.prt("   Print first " + nPrtPrune + " pruned hits per annoDB");
			}
			Out.prt("   This is just for experimentation. Remove annotation and Re-annotate when done experimenting.");
			bdoAnno=false;
			doPrune = true;
		}
		else if (hasOption(args, "-r")) { // used from runSingleTCW also
			contMsg = "Recalculate ORFs - continue?";
			bdoAnno=false;
			doRecalcORF = true; 
			
			if (hasOption(args, "-t")) { // CAS334 added
				ORF_Filter = getOptionNum(args,"-t");
				if (ORF_Filter == 0) Out.prt("   Use all sequences for training");
				else Out.prt("   Use N longest ORFs to train Markov Model: N=" + ORF_Filter);
			}
			// the following are not printed on usage_exit
			ORF_Type_Param = getOptionNum(args,"-r");
			if (ORF_Type_Param>0) {
				if (ORF_Type_Param==1)      Out.prt("   Use longest ORF ");
				else if (ORF_Type_Param==2) Out.prt("   Use Best Markov Score ");
				else {
					Out.prt("Invalid ORF type " + ORF_Type_Param);
					ORF_Type_Param=0;
				}
			}
			if (hasOption(args, "-f")) {
				ORF_Transcoder_Param=1;
				Out.prt("   Use TransDecoder Base Frequency calculation");
			}	
			if (hasOption(args, "-sc")) {
				ORF_Seq_Cov = getOptionNum(args,"-sc");
				if (ORF_Seq_Cov<0) Out.die("-sc requires an integer following this flag");
				else               Out.prt("   Minimum sequence coverage: " + ORF_Seq_Cov);
			}
			if (hasOption(args, "-hc")) {
				ORF_Hit_Cov = getOptionNum(args,"-hc");
				if (ORF_Hit_Cov<0) Out.die("-hc requires an integer following this flag");
				else               Out.prt("   Minimum hit coverage: " + ORF_Hit_Cov);
			}
		}	
	}
	/**************************************************************/
	private static void usage_exit () {
		System.err.println(
				"\nUsage:  execAnno <project_directory>  [optional flag]\n"
				+ "  A project directory must be supplied and exist under /projects.\n" 
				+ "  The project directory must contain the sTCW.cfg.\n"
				+ "  -n no prompt (this can be used with any of the below, or with no other parameter)\n" +
				"		It re-uses any existing files, does not delete anything\n" +
				"		This is good for batch processing\n"
				+ "Only one of the following may be selected:\n"
				+ "  -g Add GO data only      (annotation must already be done)\n"
				+ "  -p Prune redundant hits  (annotation must already be done)\n"
				+ "        -pt <integer> 1 Alignment  2 Descriptions  (this overrides what is set in Options)\n"
				+ "        -pp <integer> Print first n pruned seq-hits per annoDB\n"	
				+ "        -pr Save/restore hit tables before processing    \n"
				+ "  -r Recalculate ORFs\n"
				//+ "     optionally followed by 1=Use Longest ORF, 2=Use Best Markov Score\n" 
				// + "     -f Use TransDecoder Base Frequency calculation\n"
				+ "        -t <integer> Use N longest ORFs to train Markov Model \n"
				+ "           If N = 0, all good hit sequences will be used for training\n"
				+ "           with no duplicate removal (Only works with Best Hits)\n"
				//+ "        -sc <integer> minimum seq coverage\n"
				//+ "        -hc <integer> minimum hit coverage\n"
				+ "  -o Regenerate Overview\n" 
				+ "     -o1 <integer> set the 1st cover cutoff (default 50)\n"
				+ "     -o2 <integer> set the 2nd cover cutoff (default 90)\n"
				+ "  -x Delete annotation\n"
				+ "  -b Enter search program parameters into database based on defaults (for Hit Reload)\n"
				+ "  -s Delete similar pairs\n"
				
				);
			System.exit(-1);
	}
	private static boolean hasOption(String[] args, String name)
	{
		for (int i = 0;  i < args.length;  i++)
			if (args[i].equals(name)) 
				return true;
		return false;
	}
	private static int getOptionNum(String [] args, String flag) {
		int noInt= -1;
		for (int i = 0;  i < args.length;  i++) {
			if (!args[i].equals(flag)) continue;
		
			if (args.length>(i+1)) {
				if (args[i+1].startsWith("-")) return noInt;
				
				String n = args[i+1];
				try {
					return Integer.parseInt(n);
				}
				catch (Exception e) {return noInt;}
			}	
		}
		return noInt;
	}
	
	private static boolean checkGODB(boolean dieflag, boolean isGOflag) // true die, false live
	{
		boolean rc = true;
		
		if (godbName == null || godbName.equals("")) {
			rc = false;
			Out.PrtSpMsg(1, "No GO database");
			if (dieflag)  Out.die("Set GO database in runSingleTCW before working with GOs (see runSingleTCW annoDB Options)");
		}
		else {
			try{
				Out.PrtSpMsg(1, "GO database = " + godbName);
				
				if (!DBConn.checkMysqlDB("runSingle ", hostsObj.host(),godbName,hostsObj.user(),hostsObj.pass())){
					if (!dieflag) 	Out.PrtWarn("GO database " + godbName + " is missing; ignoring GO step\n");
					else 			Out.die("GO database " + godbName + " not found");
					return false;
				}
				
				DBConn goDB = DoGOs.connectToGODB(hostsObj.host(),hostsObj.user(),hostsObj.pass(), godbName);
				if (goDB == null) {
					if (!dieflag) 	Out.PrtWarn("GO database " + godbName + " is not current; ignoring GO step\n");
					else 			Out.die("GO database " + godbName + " is not current");
					return false;
				}
				
				// Verify GOdb
				String upTab = Globalx.goUpTable;
				boolean b = goDB.tableExist(upTab);
				if (b) {
					int cnt = goDB.executeCount("select count(*) from " + upTab);
					b = (cnt>0);
				}
				if (!b) {
					rc = false;
					if (!dieflag) 	Out.PrtWarn("GO database " + godbName + " is not current; ignoring GO step\n");
					else 			Out.die("GO database " + godbName + " is not current");
					godbName = null;
				} 
				else Out.PrtSpMsg(2,"Valid goDB exists '" + godbName + "'");
				goDB.close();
				
				if (isGOflag) {// GO Slims		
					if (goSlimSubset!=null && !goSlimSubset.equals(""))
						Out.PrtSpMsg(2,"Add GO_slim_subset " + goSlimSubset);
					else if (goSlimOBOFile!=null && !goSlimOBOFile.equals(""))
						Out.PrtSpMsg(2,"Add GO_slim_OBO_file " + goSlimOBOFile);
					Out.PrtSpMsg(1,"");
				}
			}
			catch(Exception e){ErrorReport.die(e, "Fatal error reading GO database " + godbName);}		
		}
		return rc;
	}
	
	/***************************************************************************
	 * Returns paths to various directories and files used by the annotator
	 ***************************************************************************/

	// Find where /projects is so can execute from TCW directory or one up
	static private String getTopLevelPath(){
		try {
			String s = ProjDirName;
			File f = new File(s);
			if (f.exists()) {
				return new File(".").getCanonicalPath();
			}
			s = "../" + ProjDirName;
			f = new File(s);
			if (f.exists()) {
				return new File("../").getCanonicalPath();
			}
			Out.PrtError("Cannot find project directory " + ProjDirName);
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}
			
	static public String getCurProjPath() {
		return getTopLevelPath() + "/" + ProjDirName + projName;
	}
	
	static public void setParameters(String db, String id, int n) {
		stcwDB = db;
		stcwID = id;
		nCPUs = n;
		
		String path = runSTCWMain.getCurProjPath();		
		blastPath = path + "/" + blastDir;
		blastObj.setSeqBlastDir(blastPath);
		String prefix = blastPath + "/" + stcwID + "_";
		blastObj.setSeqBlastPrefix(prefix);
	}
	 
	static public void setGOparameters(String xgodb, String xgoSlimSubset, String xgoSlimOBOFile, String xnoGO) {
		godbName = xgodb;
		goSlimSubset = xgoSlimSubset;
		goSlimOBOFile  = xgoSlimOBOFile;
		goNoAdd = (xnoGO.contentEquals("0")) ? false : true;
	}
	 /************************************************************************/
	public static boolean yesNo(String question) {
		if (noPrompt) return true;
		BufferedReader inLine = new BufferedReader(new InputStreamReader(System.in));

		System.err.print("?--" + question + " [y/n]: "); 
		try
		{
			String resp = inLine.readLine();
			if (resp.equalsIgnoreCase("y")) return true;
			else if (resp.equalsIgnoreCase("n")) return false;
			else {
				System.err.println("Could not understand response '" + resp + "' -- please enter again");
				System.err.print("?--" + question + " [y/n]: "); 
				resp = inLine.readLine();
				if (resp.equalsIgnoreCase("y")) return true;
				else return false;
			}
		} catch (Exception e)
		{
			ErrorReport.prtReport(e, "Could not read from keyboard");
			return false;
		}
	}
	// Leave this one probably
	public static String promptQuestion(String question, String [] ans, int def)
	{
		if (noPrompt) {
			return ans[def]; 
		}
		BufferedReader inLine = new BufferedReader(new InputStreamReader(System.in));
		System.err.print("?--" + question + " "); 
		try
		{
			String resp = inLine.readLine().trim();
			for (int i=0; i<ans.length; i++) {
				if (resp.equals(ans[i])) return ans[i];
			}
			System.err.println("Could not understand response '" + resp + "' -- please enter again");
			System.err.print("?--" + question); 
			resp = inLine.readLine();
			for (int i=0; i<ans.length; i++) 
				if (resp.equals(ans[i])) return ans[i];
				
		} catch (Exception e){ErrorReport.prtReport(e, "Could not read from keyboard");}
		return ans[0];
	}
	
	
	/*************************************************************
	 * Get and put methods
	 *************************************************************/
	
	static public String getProjPath() {return projPath;}
	static public String getProjName() {return projName;}
	static public String getAssemblyID() {return stcwID;}
	static public String getBlastDir() { return blastPath;}
	static public int getCPUs() {return nCPUs;}
	
	// Annotation settings
	static private HostsCfg hostsObj = null;
	static private DBConn mDB = null;
	static private String stcwDB = null;
	static private String stcwID = null;
	static private String godbName, goSlimSubset, goSlimOBOFile;
	static private boolean isAAstcwDB=true, goNoAdd;
	
	static private DoBlast blastObj = null;
	static private DoUniProt uniObj = null;
	static private CoreAnno annoObj = null;
	static private CoreDB sqlObj = null;
	static private DoORF  orfObj = null;
	
	static public PrintWriter logFileObj;
	
	static private int  nCPUs = 1;
	static private CfgAnno cfgObj;
	static private String projPath = null;
	static private String projName = null;
	static private String blastPath = null;
	
	static private String contMsg = null;
	static private boolean noPrompt = false;
}
