
package sng.assem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Stack;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import sng.annotator.CoreDB;
import sng.assem.enums.*;
import sng.assem.helpers.*;
import sng.database.Globals;
import sng.database.Schema;
import sng.database.Version;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.methods.*;

// The breakup between constructor, main() and run() is so
// that it can be used either as a standalone Java application (which uses main() )
// or be called from within a controller app, using run().
// CASZ 1Sept19 got rid of a lot of dead code. Made private methods private (were all public)
//      changed rs.getInt("name") to rs.getInt(num) in most places
//      there is still lots of dead code and the DEBUG option crashes

public class AssemMain
{
	private boolean debugTmp=false;
	public ID2Obj<Integer> mMergesDone;
	public TCWprops mProps;
	public int mAID;
	public String mIDStr;
	public ID2Obj<Clone> mID2Clone;
	public Str2Obj<Clone> mName2Clone;
	public MatePair[] mPairs;
	public ID2Obj<Contig> mID2Contig;
	
	private static boolean mNoPrompts = false;
	private final String assmTmpDir = "assem";
	private DBConn mDB;
	private DBConn mDB2; // For nested queries which are used a couple places (better solution: no nesting)
	
	private File mProjDir,  mTopDir, mInitialBuryDir, mCliqueDir, mLogDir;
	private File mSBFile = null;
	
	private Vector<Library> mLibs;
	private RStat mRstat;
	
	private ID2Obj<SubContig> mID2SubContig;
	private Clone[] mClones;
	
	private SparseMatrix mPairOlaps;
	private Stack<TreeSet<Integer>> mPairCliques;
	private Stack<TreeSet<Integer>> mSingleCliques;
	private SparseMatrix mSingleOlaps;
	private HashSet<MatePair> mSelfOlaps;
	
	private AutoArray<Cluster> mClusters;
	private int mThreadWorkItem;
	private int[] mThreadWorkList;
	private int mThreadsFinished;
	
	private AutoArray<Edge> mEdges;
	private BlastType mBlastType;
	private Contig[] mCtgsBySize ;
	private TreeMap<Integer,Integer> mLibCounts;
	private float mERate, mExRate; 
	
	private boolean bSkipAssembly=false;
	
	public static void main(String[] args)
	{
		Version.printTCWversion();
		
		if (args.length == 0 || args[0].equals("-h"))
			printUsage();

		int argStart = 0;
		if (args[0].equals("-q"))
		{
			argStart = 1;
			mNoPrompts = true;
		}
		String dir = args[argStart];
		String cfg = (args.length > 1 + argStart ? args[argStart + 1] : "sTCW.cfg");

		try
		{
			AssemMain assembler = new AssemMain(dir, cfg);
			assembler.run(true);
		} 
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			System.err.println("Fatal error - exiting");
			Log.exception(e);

			System.exit(-1);
		}
		System.exit(0);
	}

	private AssemMain(String dir, String cfg) throws Exception
	{
		mName2Clone = new Str2Obj<Clone>();
		mID2Clone = new ID2Obj<Clone>();
		mSingleCliques = new Stack<TreeSet<Integer>>();
		mPairCliques = new Stack<TreeSet<Integer>>();
		mSelfOlaps = new HashSet<MatePair>();
		mID2SubContig = new ID2Obj<SubContig>();
		mID2Contig = new ID2Obj<Contig>();
		mClusters = new AutoArray<Cluster>();
		
		mEdges = new AutoArray<Edge>();
		mMergesDone = new ID2Obj<Integer>();
		
		new Utils();
		Astats.init();
		
		// Try to find the projects directory. Hopefully we are not more than two levels from it.
		String[] pdirs = { "./projects", "../projects", "../../projects" };
		boolean found = false;
		File f = null;
		for (String d : pdirs)
		{
			f = new File(d);
			if (f.isDirectory())
			{
				found = true;
				break;
			}
		}
		if (!found)
		{
			Log.die("Unable to find the 'projects' directory.");
		} 	
		mProjDir = new File(f, dir);
		if (!mProjDir.isDirectory())
		{
			System.err.println("Unable to find project directory " + dir	+ " in " + f.getAbsolutePath());
			System.exit(0);
		}
		
		mProps = new TCWprops(TCWprops.PropType.Assem);
		mProps.load(new File(mProjDir, cfg));

		new Utils(); // This initializes the class static members
		Utils.mProps = mProps;
		
		mIDStr = mProps.getProperty("SingleID");
		if (mIDStr==null)  mIDStr = mProps.getProperty("AssemblyID");
		if (mIDStr==null) {
			Log.die("Missing SingleID in sTCW.cfg ");	
		}
		if (!Utils.validName(mIDStr)){
			Log.die("Invalid SingleID " + mIDStr + ": may contain letters, numbers, underscores");	
		}
		
		mLogDir = new File(mProjDir,Globalx.logDir);
		Utils.checkCreateDir(mLogDir);
		
		File logfile = new File(mLogDir, Globals.assmFile); 
		new Log(logfile); // sets log file
		Log.addLogAction(LogLevel.Basic, LogAction.Terminal);
		Log.addLogAction(LogLevel.Basic, LogAction.Log);

		Log.head("Begin instantiate " + dir,LogLevel.Basic);
		
		long maxMem = Runtime.getRuntime().maxMemory()/(1024*1024);
		String maxMemStr = Utils.getEnv("PAVE_MEM");
		if (maxMemStr.equals(""))
		{
			maxMemStr = "" + maxMem;
		}
		
		Log.indentMsg("Using " + maxMem + "M memory", LogLevel.Basic);
		if (mNoPrompts)
		{
			Log.indentMsg("No-prompt mode", LogLevel.Basic);	
		}
		
		if (!mProps.getProperty("User_EST_selfblast").equals("")) // obsolete
		{
			mSBFile = new File(mProps.getProperty("User_EST_selfblast"));
			if (!mSBFile.exists())
			{
				mSBFile = new File(mProjDir,mProps.getProperty("User_EST_selfblast"));
				if (!mSBFile.exists())
				{
					Log.die("Can't find User_EST_selfblast file " + mProps.getProperty("User_EST_selfblast"));
				}
			}
		}		
		
		HostsCfg hosts = new HostsCfg();

		String cap_cfg = hosts.getCapPath();
		if (cap_cfg!=null && !cap_cfg.equals(""))
		{
			mProps.setProperty("CAP_CMD", cap_cfg);
		}
		String path = mProps.getProperty("CAP_CMD");
		if (!path.equals("cap3") && !path.startsWith("/"))
		{
			File f2 = new File(path);
			if (f2.exists())
			{
				path = f2.getAbsolutePath();
				mProps.setProperty("CAP_CMD",path);
			}
			else
			{
				System.err.println("Error: cannot find CAP3 at path " + path);
				mProps.setProperty("CAP_CMD", "cap3");
			}
		}
		String b = mProps.getProperty("SKIP_ASSEMBLY");
		if (b.equals("0")) 
			Log.indentMsg("Using CAP3 command:" + mProps.getProperty("CAP_CMD"),LogLevel.Basic);
		
		String db = mProps.getProperty("STCW_db");
		if (db==null || db.equals("")) {
			System.err.println("Fatal error: STCW_db is not defined in sTCW.cfg");
			return;
		}
		mDB = hosts.getDBConn(db);
		if (mDB==null) {
			System.err.println("Fatal error: Cannot open database '" + db + "'");
			return;
		}
		Log.indentMsg("Open database " + db, LogLevel.Basic);
		
		mDB2 = hosts.getDBConn(db);
		
		Schema schObj = new Schema(mDB);
		if (!schObj.current())
		{
			schObj.update();				
		}
		
		AceFile.mCapFailLog = null;
		AceFile.mNumCapFails = 0;
	}
	private void run(boolean exitOnComplete) throws Exception
	{
		long startTime = Out.getTime();
		ResultSet rs;
		Schema schObj = new Schema(mDB);
		
	/** Determine state of database **/
		
		String msg="Instantiate";
		bSkipAssembly=true;
		if (mProps.getProperty("SKIP_ASSEMBLY").equals("0"))
		{
			msg = "Assembly";
			bSkipAssembly=false;
			
			rs = mDB.executeQuery("show columns from assem_msg like 'peptide'");
			if (rs.first())
			{
				Utils.termOut("");
				Utils.termOut("This is a protein project and cannot be assembled.");
				Utils.termOut("Set 'Skip Assembly' to instantiate proteins.");
				System.exit(0);
			}
		}
		
		rs = mDB.executeQuery("select AID from assembly where assemblyid != '" + mIDStr + "'");
		if (rs.first())
		{
			Utils.termOut("\n\nA different assembly/instantiation already exists in this database. " +
					"Multiple assemblies per database are no longer supported.");
			System.exit(0);
		}
		rs= mDB.executeQuery("select AID,completed from assembly where assemblyid='" + mIDStr + "'");
		boolean resume = false;
		boolean deleteAssembly = false;
		int maxTCnum = 0;
		String dbmsg =  mProps.getProperty("STCW_db") + " on host "+ mProps.getProperty("DB_host");
		if (rs.next())
		{
			boolean completed = rs.getBoolean("completed");
			mAID = rs.getInt("AID");
			if (completed || bSkipAssembly)
			{
				Utils.termOut("\nA completed instantiation for \"" + mIDStr
								+ "\" already exists in the database " + dbmsg);
				if (!mNoPrompts)
				{
					if (!Utils.yesNo("Delete this instantiation and re-start it"))
					{
						System.err.println("Exit instantiation");
						return;
					}
				}
				else return;
				
				deleteAssembly = true;			
			} 
			else
			{
				Log.msg("\nA partially-completed assembly with the same id \""+ mIDStr
								+ "\" already exists in the database " + dbmsg, LogLevel.Basic);
				if (!mNoPrompts && !Utils.yesNo("Resume this instantiation"))
				{
					if (!Utils.yesNo("Delete this assembly and re-start it"))
						return;
					deleteAssembly = true;
				}
				else
				{
					resume = true;
					Log.msg("Previous assembly is being resumed",LogLevel.Basic);
				}
			}
			if (!resume)
				deleteSavedLogFiles();
		}
		mBlastType = BlastType.valueOf(mProps.getProperty("BLAST_TYPE"));

		if (resume)
		{
			rs = mDB.executeQuery("select max(tcnum) as maxt from ASM_tc_iter where aid=" + mAID);
			if (rs.first())
			{
				maxTCnum = rs.getInt("maxt");
				if (maxTCnum > 0)
					Log.msg("Resuming TC" + maxTCnum, LogLevel.Basic);
			}
		}
		// to do: turn off
		(new memCheckThread()).start();
		
		rs.close();
		mLibs = new Vector<Library>();
		int nLibClones = 0;
		if (!mProps.getProperty("libraries").equals(""))
		{
			for (String libid : mProps.getProperty("libraries").split("\\s+"))
			{
				Library lib = new Library(mDB, libid);
				if (!lib.loadFromDB())
					Log.die("Can't find library " + libid + " in database");
				mLibs.add(lib);
			}
		}
		else
		{
			rs = mDB.executeQuery("select library.libid, count(*) as nclones from library join clone on " +
					" clone.LID=library.LID group by library.LID");
			while (rs.next())
			{
				String libid = rs.getString("libid");
				int nclones = rs.getInt("nclones");
				if (nclones > 0)
				{
					Library lib = new Library(mDB2, libid);
					if (!lib.loadFromDB())
						Log.die("Can't find dataset " + libid + " in database");
					mLibs.add(lib);
				}
			}
		}
		Log.head("Dataset",LogLevel.Basic);
		Log.columnsInd(25, LogLevel.Basic, "Dataset", "#Seqs", "Load Date", "Seq File");
		for (Library lib : mLibs)
		{
			nLibClones += lib.mNumESTLoaded;
			Log.columnsInd(25, LogLevel.Basic,lib.mIDStr, lib.mNumESTLoaded, lib.mLoadDate,
					lib.mSeqFile.getName());
		}
		if (nLibClones > 100000)
		{
			mDB.checkInnodbBufPool();	
		}
			
		int maxLen = Integer.parseInt(mProps.getProperty("MAX_MERGE_LEN"));
		if (maxLen > 0)
		{
			Vector<String> longLibs = new Vector<String>();
			int longClones = 0;
			for (Library lib : mLibs)
			{
				rs = mDB.executeQuery("select count(*) as cnt from clone where length > " + maxLen);
				rs.first();
				int cnt = rs.getInt("cnt");
				if (cnt > 0)
				{
					longLibs.add(lib.mIDStr);
					longClones += cnt;
				}
			}
			if (longLibs.size() > 0)
			{
				String clist = Utils.join(longLibs, ",");
				Log.msg(longClones + " reads have length greater than " + maxLen,LogLevel.Basic);
				Log.msg("These reads will not be assembled (to change, set property MAX_MERGE_LEN)",LogLevel.Basic);
				Log.msg("Dataset containing long clones:" + clist,LogLevel.Basic);
			}
		}
		checkCommands(); 

		// Find out if we need to ask them about using previous megablast files, before
		// longer operations commence
		boolean useOldBlastFiles = false;

		clearFastaFiles();
		
		if (deleteAssembly) 
			deleteAssembly(mAID);
		renewConnections();
		schObj.addASMTables(); // in case 
		
		if (!resume)
		{
			createAssemblyInDB();
			recordTime("Begin " + msg);	
		}
		else recordTime("Resume " + msg);	
		
		/***************************************************
		 * XXX SKIP Assembly
		 */
		if (bSkipAssembly)
		{
			skipAssembly();	
			schObj.dropInnoDBTables();
			Log.msg("\n" + Out.getMsgTimeMem(">>>End Instantiate", startTime),LogLevel.Basic);
			Log.finish();
			System.exit(0);
		}
		/*********************************************
		 * XXX ASSEMBLE
		 */
		if (!mDB.hasInnodb())
			Log.die("Unable to proceed because MySQL database does not support InnoDB tables.");
		
		int tc = 1;

		/****** Open files and directories for work and logs ***/
		//if (Utils.isDebug()) // CASZ 31aug19; moved from AssemMain
		if (debugTmp)          // creates large output; the isDebug doesn't work anymore.
		{
			System.err.println("Debug is on - large output to log file");
		    File detailfile = new File(mLogDir, mIDStr + ".detail");
			detailfile = new File(mLogDir, mIDStr + ".detail"); 
			detailfile.createNewFile();
			Log.setDebugLog(detailfile);
			Log.addLogAction(LogLevel.Basic, LogAction.DebugLog);
			Log.addLogAction(LogLevel.Detail, LogAction.DebugLog);
			Log.addLogAction(LogLevel.Dbg, LogAction.DebugLog);
		}
					
		File assemDir = new File(mProjDir,assmTmpDir);
		Utils.checkCreateDir(assemDir);
		
		mTopDir = new File(assemDir,mIDStr);
		Utils.checkCreateDir(mTopDir);
		if (!mTopDir.isDirectory())
			Log.die("Could not create " + mIDStr + " directory");

		mInitialBuryDir = new File(mTopDir, "initial_bury");
		Utils.checkCreateDir(mInitialBuryDir);		

		// Status dir holds the status tracking tags. this isn't very clean.		
		File statdir = new File(mTopDir, "status");
		Utils.checkCreateDir(statdir);
		Utils.setStatusDir(statdir);
		if (!resume)
			Utils.clearDir(statdir);

		mCliqueDir = new File(mTopDir, "cliques");
		Utils.checkCreateDir(mCliqueDir);

		if (!resume)
			clearProjectDir();
		
		fix_assemlib();
		Schema.addCtgLibFields(mDB); // do it now, while the table is empty
		loadClones();	
	
	/*** Initial buries ***/
		if (mProps.getProperty("DO_INITIAL_BURY").equals("1"))
		{
			if (!Utils.checkUpToDate("FIRST_BURY","",""))
			{
				Log.head("Initial bury alignment",LogLevel.Basic);
				doBuryBlast(useOldBlastFiles);
				renewConnections();
				processBuries();
			}
		}
			
		if (maxTCnum == 0 && doCliques()) // maxTCnum>0 means its being resumed
		{	
	/** Cliques **/
			Log.head("Compute cliques", LogLevel.Basic);
			if (!pairsOnly()) // mClones.length != 2*mPairs.length
			{
				singleCliqueBlast();
				renewConnections();
				
				buildSingleCliques();
				renewConnections();
			}
			else Log.indentMsg("Pairs only",LogLevel.Basic);
			
			if (!singlesOnly())  // (mPairs.length > 0)
			{
				pairCliqueBlast();
				renewConnections();
				
				buildPairCliques();
				renewConnections();
			}
			else Log.indentMsg("Singles only",LogLevel.Basic);
			
			int cliqueTCID = initCliques();
			if (doCliques()) 
			{
				assembleCliques(cliqueTCID);
			}
			renewConnections();
			
			makeSingletons(cliqueTCID);
			renewConnections();
			
			if (mProps.getProperty("DO_CAP_BURY").equals("1")) // on by default
			{
				loadContigs(true, false, false); 
				capBuryCliques(cliqueTCID);
				renewConnections();
			}
			else Log.msg("CAP Bury turned off", LogLevel.Basic);
			
			if (!Utils.isDebug())
			{
				Utils.deleteDir(new File(mCliqueDir,"cap"));
				Utils.deleteDir(new File(mCliqueDir,"burycap"));
				Utils.deleteDir(mCliqueDir);	
			}
			
			renewConnections();
			checkContigIntegrity(true);
			
	/******** TC contig merge *************/
			renewConnections();
			loadContigs(true, false, false); 
				
			// Figure out the maximum TC the user has specified to run.
			// We need this partly b/c there is one heuristic that applies to all but the final TC.
			int maxtc = 1;
			while (true)
			{
				String tcstr = "TC" + (maxtc + 1);
				if (!mProps.containsKey(tcstr) || mProps.getProperty(tcstr).trim().equals(""))
					break;
				maxtc++;
			}
			
			for (tc = 1; tc <= maxtc; tc++)
			{
				renewConnections();
				String tcstr = "TC" + tc;
				if (!mProps.containsKey(tcstr)) break;
				String tcprop = mProps.getProperty(tcstr).trim();
				if (tcprop.equals(""))	break;
				doTCByCluster(tc,maxtc);			
			}
			renewConnections();
		} // end maxTCnum == 0 && doCliques())
		else
		{
			mDB.executeUpdate("delete from ASM_tc_iter where aid=" + mAID);
			mDB.executeUpdate("delete from contig where aid=" + mAID);
			mDB.executeUpdate("insert into ASM_tc_iter (AID,tcnum, tctype) values(" + mAID + ",0,'" + IterType.Clique.toString() + "')");
			int tcid = mDB.lastID();
			makeSingletons(tcid);
			tc = 1;
		}
		
		finalizeContigs(tc);
		renewConnections();
		if (Utils.isDebug())
		{
			Log.msg("Debug is on", LogLevel.Basic);
			checkContigIntegrity(false);
		}
		else
		{
			cleanUpAssembly();
			Utils.deleteDir(assemDir);
		}
		fixAssemLibCounts();
		mDB.executeUpdate("update assembly set completed=1, assemblydate=NOW() where aid=" + mAID);
		Utils.recordTime("AssemblyDone",mAID,mDB);
		long maxMem = Utils.mMaxMemMB;
		Log.indentMsg("\nMax memory usage:" + maxMem + "M", LogLevel.Basic);
		Utils.timeSummary(LogLevel.Basic);
		summarizeAssembly();
		if (!Utils.isDebug()) 
			schObj.dropInnoDBTables();
		
		Log.msg("\n" + Out.getMsgTimeMem(">>>End Assembly", startTime),LogLevel.Basic);
		Log.finish();
		
		if(exitOnComplete)
			System.exit(0);
	}	

	private void clearFastaFiles()
	{
		// delete the fasta files on disk in case the new assembly results in different ones,
		// e.g. if user changed their "use trans names" selection
		File ubdir = new File(mProjDir,"uniblasts");
		if (ubdir.isDirectory())
		{
			for (File f : ubdir.listFiles())
			{
				if (f.getName().contains(".fasta"))
				{
					f.delete();
				}
			}
		}
	}
	private void deleteAssembly(int aid) throws Exception
	{
		Log.head("Delete previous instantiation", LogLevel.Basic);
		ResultSet rs = null;
		rs = mDB.executeQuery("select count(*) as count from contig where aid=" + aid);
		rs.first();
		int nctg = rs.getInt("count");
		if (nctg > 100000)
		{
			Utils.termOut("\tlarge instantiation...delete may be slow");	
		}
		
		// This could be done with one big cascading delete, but it's too slow.
		// So clear out some of the tables by hand first. 
		mDB.executeUpdate("delete from buryclone"); // WN one assembly per db!!
		mDB.executeUpdate("delete from contclone");
		mDB.executeUpdate("delete from contig_counts");
		mDB.executeUpdate("delete from snp");
		mDB.executeUpdate("delete from snp_clone");
		mDB.executeUpdate("delete from contig");
		
			
		mDB.executeUpdate("delete from ASM_params");
		mDB.executeUpdate("delete from ASM_assemtime");
		mDB.executeUpdate("delete from ASM_cmdlist");
		mDB.executeUpdate("delete from assem_msg");
		mDB.executeUpdate("delete from assemlib");
		mDB.executeUpdate("delete from assembly");
		
		CoreDB cdb = new CoreDB(mDB);
		if (cdb.existsAnno()) {
			Utils.termOut("Delete annotation");	
			cdb.deleteAnnotation(false);
		}
		Utils.termOut("Finish delete");	
	}
	private void cleanUpAssembly() throws Exception
	{
		Utils.singleLineMsg("Cleaning up database tables");
		mDB.executeUpdate("delete from ASM_cliques where aid=" + mAID);
		mDB.executeUpdate("delete from ASM_scontig where aid=" + mAID);
		mDB.executeUpdate("delete ASM_tc_edge.* from ASM_tc_edge join ASM_tc_iter on ASM_tc_edge.tcid=ASM_tc_iter.tcid where ASM_tc_iter.aid=" + mAID);

		if (mInitialBuryDir.isDirectory())
		{
			Utils.clearDir(mInitialBuryDir);
		}
	}	
	// As clones are buried, they are dropped from further consideration. 
	// At the end, their correct contigs have to be identified, which means
	// we have to find the top parent in their bury chain, which may be multiple levels deep. 
	private void setTopParents() throws Exception
	{
		String thisTag = "SET_TOP_PARENTS";	

		if (Utils.checkUpToDate(thisTag, "",""))
		{
			return;
		}
		Utils.termOut("Updating buried reads");
		Utils.deleteStatusFile(thisTag);
		recordTime(thisTag + " start");
		
		// First re-set each topparent to the immediate parent.
		
		int numReset = mDB.executeUpdate("update buryclone set cid_topparent=cid_parent where aid=" + mAID);
		Log.msg("reset " + numReset + " top parents",LogLevel.Detail);
		
		// Now iteratively update the child's topparents to equal the parent's topparents, until no more records change.
		while (true)
		{
			int nRows = mDB.executeUpdate("update buryclone as bc1, buryclone as bc2 set bc1.CID_topparent=bc2.CID_topparent " +
						"where  bc1.CID_parent=bc2.CID_child and bc1.CID_topparent != bc2.CID_topparent and bc1.aid=" + mAID + " and bc2.aid=" + mAID);
			if (nRows == 0)
			{
				break;
			}
			Log.msg("adjust " + nRows + " top parents",LogLevel.Detail);
		}
		if (Utils.isDebug())
		{
			validateBuried();
		}
		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
	}
	
	// The logic of log saving and deletion (same for both XX.log and XX.detail).
	// It is slightly complicated because we want to open a log file as early
	// as possible, before we know whether it's a restart or not. 
	//
	// A. Copy XX.log to XX.log.N, where N is the smallest available
	//		(purpose: to avoid blowing away the previous log file in case of restart)
	// B. Make the new log file XX.log
	// C. If it is a fresh build (NOT a restart), remove all of the XX.log.N files
	// 
	private void deleteSavedLogFiles() throws Exception
	{
		for (File f : mLogDir.listFiles())
		{
			// WMN last minute fixes here since debug file may not exist anymore. 
			// Result is that debug file will not be preserved on restart.
			if (f.getName().startsWith(Log.logFile.getName()) ) //|| f.getName().startsWith(mLog.dbgFile.getName()))
			{
				if (!f.getName().equals(Log.logFile.getName()) ) //&& !f.getName().equals(mLog.dbgFile.getName()))
				{
					// the file is one of the log files with an extra prefix, i.e., it must be one of the ".N" 
					// saved files, and we want to delete it
					f.delete();
				}
			}
		}

	}
	// Every thread should call this to get its own DB connection. 
	// Otherwise, there will be conflicts in getting the last autoincrement id's from inserts. 
	private DBConn getDBConnection() throws Exception
	{
		return new HostsCfg().getDBConn(mProps.getProperty("STCW_db"));
	}
	private void renewConnections() throws Exception
	{
		mDB.renew();
		mDB2.renew();
	}
	
	private void clearProjectDir()
	{
		Utils.singleLineMsg("Clearing project directory");
		for (File file : mTopDir.listFiles())
		{
			if (file.isDirectory() )
			{
				if (!file.getName().equals("logs") ) 
				{
					Log.msg("clear directory " + file.getAbsolutePath(),LogLevel.Detail);
					Utils.clearDir(file);
				}
			}
			else
			{
				Log.msg("delete file " + file.getAbsolutePath(),LogLevel.Detail);
				file.delete();
			}
		}
		System.err.println("                                            "); 
	}
	private void createAssemblyInDB() throws Exception
	{
		String user = System.getProperty("user.name"); 
		mDB.executeUpdate("insert into assembly (AID, assemblyid,assemblydate,projectpath,completed,annotationdate,username,descr) "
						+ " values(1, '" 
						+ mIDStr
						+ "',NOW(),'"
						+ mProjDir.getAbsolutePath() + "',0,NULL,'" + user + "','') ");
		mAID = 1;
		for (Library lib : mLibs)
		{
			mDB.executeUpdate("insert into assemlib (AID,LID,assemblyid,libid)"
					+ " values(" + mAID + "," + lib.mID + ",'" + mIDStr + "','"
					+ lib.mIDStr + "')");
		}
		PreparedStatement ps = mDB.prepareStatement("insert into ASM_params (aid,pname,pvalue) values(" + mAID + ",?,?)");
		for (Object obj : mProps.keySet())
		{
			String name = (String)obj;
			String val = mProps.getProperty(name);
			ps.setString(1,name);
			ps.setString(2,val);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}

	private void doBuryBlast(boolean useOldFiles) throws Exception
	{
		String thisTag = "FIRST_BURY_BLAST";

		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			Log.msg("bury alignment has been done",LogLevel.Basic);
		}

		File buryFile = new File(mInitialBuryDir, "blast_result0");

		if (!useOldFiles)
		{
			Log.indentMsg("Begin bury alignment using Megablast",LogLevel.Basic);
		}
		else
		{
			if (buryFile.exists())
			{
				Log.indentMsg("Begin bury alignment using previous Megablast output files",LogLevel.Basic);
			}
			else
			{
				Log.indentMsg("Bury file " + buryFile.getName() + " not found, must rerun", LogLevel.Basic);
			}
		}
			
		long tstart = TimeHelpers.getTime();

		if (buryFile.exists())
		{
			if (useOldFiles)
			{
				Utils.writeStatusFile(thisTag);
				return;
			}
		}
		
		if (mSBFile != null)
		{
			Log.indentMsg("Using provided file " + mSBFile.getAbsolutePath(),LogLevel.Basic) ;
			Utils.writeStatusFile(thisTag);
			return;
		}
		
		Utils.deleteStatusFile(thisTag);
		recordTime(thisTag + " start");

		// may as well clear everything out at this point
		for (File f : mInitialBuryDir.listFiles())
		{
			if (f.isFile())
				f.delete();
		}
		
		// We have to compile a single file of the sequences for the megablast.
		// Get them from the DB, where we already have them renamed. 
		
		File f = new File(mInitialBuryDir,"ests.fasta");
		if (f.exists()) f.delete();

		Utils.termOut("Writing sequences for alignment");
		
		int lastID = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		int maxLen = Integer.parseInt(mProps.getProperty("MAX_MERGE_LEN"));
		String lenClause = "";
		if (maxLen > 0)
		{
			lenClause = " and clone.length <= " + maxLen + " ";
		}
		while (true)
		{
			ResultSet rs = mDB.executeQuery("select clone.cid,cloneid,sequence from clone join assemlib on clone.LID=assemlib.LID " + 
						" where assemlib.AID=" + mAID + " and clone.cid > " + lastID + lenClause + " order by clone.cid asc limit 10000 ");
			int thisCount = 0;
			while (rs.next())
			{
				String name = rs.getString("cloneid");
				String seq = rs.getString("sequence");
				bw.append(">" + name + "\n");
				bw.append(seq + "\n");
				lastID = rs.getInt("cid");
				thisCount++;
			}
			rs.close();
			bw.flush();
			if (thisCount == 0) break;			
		}
		bw.close();
		
		String cmd = BlastArgs.getFormatn("ests.fasta");
		Utils.termOut("Running " + cmd); 

		Log.msg(cmd,LogLevel.Detail);
		Utils.recordCmd(mAID,"InitBuryFormatdb",cmd,mDB);
		Utils.appendToFile(cmd,mInitialBuryDir,"cmd.list");
		Utils.runCommand(BlastArgs.cmdLineToArray(cmd), mInitialBuryDir, false, true, null,0);

		int minLen = mClones[0].mSeqLen;
		for (Clone c : mClones)
		{
			minLen = Math.min(c.mSeqLen, minLen);
		}
		int maxMis = Integer.parseInt(mProps.getProperty("BLAST_BURY_MISMATCH"));
		Integer minScore = calcBlastScore(minLen - maxMis); // note, HIT score not BIT score
		
		String blastCmd = BlastArgs.getBlastnExec();
		Utils.termOut("Running " + blastCmd);

		Integer pctID = Math.max(100 - maxMis, (100*(minLen - maxMis))/minLen);
		
		if (!mProps.getProperty("BURY_BLAST_IDENTITY").equals(""))
		{
			pctID = Integer.parseInt(mProps.getProperty("BURY_BLAST_IDENTITY"));	
		}
		Log.msg("Min sequence length:" + minLen + " max mismatch:" + maxMis + " min %identity:" + pctID + " min bit score:" + minScore,LogLevel.Detail);
		
		String cmd2 = BlastArgs.getAsmMegablastArgs("ests.fasta", mProps.getProperty("BURY_BLAST_EVAL").trim(), 
				pctID, minScore, mProps.getProperty("BURY_BLAST_PARAMS").trim());
					
		String cmd3 = cmd2 + BlastArgs.getAsmInOutParams("ests.fasta", buryFile.getAbsolutePath()); // just for recording in log
		
		Log.msg(cmd3,LogLevel.Detail);
		Utils.recordCmd(mAID,"InitBuryBlast",cmd3,mDB);
		Utils.appendToFile(cmd3,mInitialBuryDir,"cmd.list");
		
		threadedBlast(cmd2,"ests.fasta","blast_result",mInitialBuryDir);

		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
		String timeStr = TimeHelpers.getElapsedTimeStr(tstart);
		Log.indentMsg("Bury alignment finished: elapsed time " + timeStr,LogLevel.Basic);
	}
	// megablast -a option is not trustworthy; hence, we multithread it by hand, just
	// by splitting the query into pieces.
	private void threadedBlast(String cmd, String queryFile, String outFile, File dir) throws Exception
	{		
			boolean nativeThreading = mProps.getProperty("BLAST_NATIVE_THREADING").equals("1");
			int nThreads = (nativeThreading ? 1 : Integer.parseInt(mProps.getProperty("CPUs"))); // threads we will start
			int nCPUs = Integer.parseInt(mProps.getProperty("CPUs")); 
			
			if (nativeThreading && nCPUs > 1)
			{
				cmd += " -a" + nCPUs + " ";	
			}
			Utils.appendToFile(cmd, dir, "cmd.list");
			BlastThread[] threadList = new BlastThread[nThreads];
		
			nThreads = splitFastaFile(dir,queryFile,nThreads);
			
			// get rid of old outfiles
			for (int i = 0; i < 1000; i++)
			{
				String name = outFile + i;
				File f = new File(dir,name);
				f.delete();
			}
			
			for (int i = 0; i < nThreads; i++)
			{
				threadList[i] = new BlastThread(this, dir,cmd,queryFile + "." + i,outFile + i,i);
			}

			mThreadsFinished = 0;
	
			for (int i = 0; i < nThreads; i++)
			{
				threadList[i].start();
			}
			
			Date timeStart = new Date();
			while (mThreadsFinished < nThreads)
			{
				Date timeNow = new Date();
				long elapsed = timeNow.getTime() - timeStart.getTime();
				timeNow = null;
				if (elapsed/1000 > 60)
				{
					timeStart = null;
					timeStart = new Date();
					int nRemaining = nThreads - mThreadsFinished;
					Utils.singleLineMsg(nRemaining + " blast processes running");
				}

				Thread.sleep(1000);
			}
			renewConnections();
	}
	// Split the query fasta file into parts so we can do the blast in multiple threads.
	private int splitFastaFile(File dir, String name, int nParts) throws Exception
	{
		// first count the fasta entries
		File qFile = new File(dir,name);
		BufferedReader reader = new BufferedReader(new FileReader(qFile));
		String line;
		int nEntries = 0;
		while ((line = reader.readLine()) != null)
		{
			line.trim();
			if (line.startsWith(">"))
			{
				nEntries++;	
			}
		}
		reader.close();
		int seqsPerPart = nEntries/nParts + 1;
		if (seqsPerPart < 1000)
		{
			nParts = 1;	
			seqsPerPart = nEntries;
		}
		
		// get rid of any previous leftover parts
		for (int p = 0; p < 1000; p++)
		{
			String pName = name + "." + p;
			File pFile = new File(dir,pName);
			pFile.delete();			
		}
		
		Log.msg("Splitting " + nEntries  + " sequences from " + qFile.getAbsolutePath() + " into " + nParts + " parts",LogLevel.Detail);
		String prevLine = "";
		reader = new BufferedReader(new FileReader(qFile));
		for (int p = 0; p < nParts; p++)
		{
			String pName = name + "." + p;
			File pFile = new File(dir,pName);
			pFile.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(pFile));
			int entryNum = 0;
			if (!prevLine.equals(""))
			{
				entryNum = 1;
				writer.write(line);writer.newLine();
				prevLine = "";
			}
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith(">"))
				{
					entryNum++;
					if (entryNum > seqsPerPart)
					{
						prevLine = line; // can't rewind the reader so must remember this line
						break;
					}	
					writer.write(line);writer.newLine();
				}
				else
				{
					writer.write(line);	writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		}
		reader.close();			
		return nParts;
	}

	// The blast of paired sequences for creating the paired cliques.
	private void pairCliqueBlast() throws Exception
	{
		if (singlesOnly()) return;
		String thisTag = "CLIQUE_ALIGN_PAIR";	
		
		long tstart = TimeHelpers.getTime();
		
		if (Utils.checkUpToDate(thisTag, null, null))
		{
			return;
		}
		Utils.deleteStatusFile(thisTag);
		recordTime(thisTag + " start");

		writeUnburiedPair();

		File outFile2 = new File(mCliqueDir, "unburied_pair.out");

		Log.indentMsg("Begin mate pair clique alignment",LogLevel.Basic);
				
		if (outFile2.exists())
			outFile2.delete();
				
		String cmd2 = BlastArgs.getFormatn("unburied_pair.fasta");
		Utils.termOut("Running " + cmd2); 

		Log.msg(cmd2,LogLevel.Detail);
		Utils.recordCmd(mAID,"PairCliqueFormatdb",cmd2,mDB);
		Utils.appendToFile(cmd2,mCliqueDir,"cmd.list");
		Utils.runCommand(BlastArgs.cmdLineToArray(cmd2), mCliqueDir, false, true, null,0);
		Utils.termOut("Running " + BlastArgs.getBlastnExec());
		
		int minScore = calcBlastScore(Integer.parseInt(mProps.getProperty("MIN_MATCH_CLIQUE"))); // note HIT score not BIT score
																								// whereas it is bit score in the output file
		String cmd = BlastArgs.getAsmMegablastArgs("unburied_pair.fasta", mProps.getProperty("CLIQUE_BLAST_EVAL"), 
				Integer.parseInt(mProps.getProperty("MIN_ID_CLIQUE")), minScore, mProps.getProperty("CLIQUE_BLAST_PARAMS").trim());
		
		Log.msg(cmd,LogLevel.Detail);
		Utils.recordCmd(mAID,"PairCliqueBlast",cmd,mDB);
		Utils.appendToFile(cmd,mCliqueDir,"cmd.list");
		
		threadedBlast(cmd,"unburied_pair.fasta","pair_blast_result",mCliqueDir);

		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
		
		String timeMsg = TimeHelpers.getElapsedTimeStr(tstart);
		Log.indentMsg("Mate pair clique alignment finished: elapsed time " + timeMsg,LogLevel.Basic);
	}

	// the blast for the unpaired clone cliques
	private void singleCliqueBlast() throws Exception
	{
		if (pairsOnly()) return;
		String thisTag = "CLIQUE_ALIGN_SINGLE";	
		if (Utils.checkUpToDate(thisTag, null,null)) return;
		
		long tstart = TimeHelpers.getTime();
		
		writeUnburiedSingle();
		File outFile1 = new File(mCliqueDir, "unburied_single.out");

		Log.indentMsg("Begin unmated clique alignment",LogLevel.Basic);		
		
		Utils.deleteStatusFile(thisTag);
		recordTime(thisTag + " start");

		if (outFile1.exists())
			outFile1.delete();
				
		String cmd2 = BlastArgs.getFormatn("unburied_single.fasta" );
		Utils.termOut("Running " + cmd2);
		Log.msg(cmd2,LogLevel.Detail);
		Utils.recordCmd(mAID,"SingleCliqueFormatdb",cmd2,mDB);
		Utils.appendToFile(cmd2,mCliqueDir,"cmd.list");
		Utils.runCommand(BlastArgs.cmdLineToArray(cmd2), mCliqueDir, false, true, null,0);
		int minScore = calcBlastScore(Integer.parseInt(mProps.getProperty("MIN_MATCH_CLIQUE"))); // note HIT score not BIT score
		Utils.termOut("Running " + BlastArgs.getBlastnExec() + "...");

		String cmd = BlastArgs.getAsmMegablastArgs("unburied_single.fasta", mProps.getProperty("CLIQUE_BLAST_EVAL").trim(), 
				Integer.parseInt(mProps.getProperty("MIN_ID_CLIQUE")), minScore, mProps.getProperty("CLIQUE_BLAST_PARAMS").trim());

		Log.msg(cmd,LogLevel.Detail);
		Utils.recordCmd(mAID,"SingleCliqueBlast",cmd,mDB);
		Utils.appendToFile(cmd,mCliqueDir,"cmd.list");
		
		threadedBlast(cmd,"unburied_single.fasta","single_blast_result",mCliqueDir);

		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
		String timeMsg = TimeHelpers.getElapsedTimeStr(tstart);
		Log.indentMsg("Unmated clique alignment finished: elapsed time " + timeMsg,LogLevel.Basic);
	}

	// Read in the bury pairs and compute the unmated clone buries. 
	// Each unmated clone is buried in the shortest length candidate parent.
	// At least two possible parents are required; this ensures that remaining coverage is
	// at least 2x so cap3 will not drop the segment from the consensus. 
	// 
	// For mated, we just save the possible buries, and then buildMatedBuryGraph finishes the job. 
	private void readBlastBuryData() throws Exception
	{
		int min2keep = Integer.parseInt(mProps.getProperty("MIN_UNBURIED"));
		int max2bury = Math.max(0,mClones.length - min2keep);
		Log.msg("burying max of " + max2bury + " reads (retaining at least " + min2keep + ")",LogLevel.Detail);
		File[] blastFiles = new File[1000]; 
		for (int i = 0; i < blastFiles.length; i++)
		{
			blastFiles[i] = null;	
		}
		if (mSBFile == null)
		{
			int nFound = 0;
			for (int p = 0; p < 1000; p++) // since they may have changed the thread number in the config file
			{	
				String fname = "blast_result" + p;
				File f = new File(mInitialBuryDir,fname);
				if (!f.exists())
				{
					break;	
				}
				blastFiles[nFound] = f;
				nFound++;
			}
		}
		else
		{
			blastFiles[0] = mSBFile;
		}

		for (Clone c : mClones)
		{
			c.mParent = null;
			c.mKids.clear();
		}
		
		TreeSet<String> unkClone = new TreeSet<String>();
		String line;
		int maxMismatch = Integer.parseInt(mProps.getProperty("BLAST_BURY_MISMATCH"));
		
		// Struct to gather stats about the buries
		TreeMap<String,Integer> rejects = new TreeMap<String,Integer>();
		rejects.put("mismatch",0);
		rejects.put("reversed",0);
		rejects.put("nocontain",0);
		rejects.put("alpha",0);
		rejects.put("child_toolong",0);
		rejects.put("same",0);
		
		int nRead = 0;
		int nSingBuried = 0;
		double highestEval = 0;
		int lowestScore = Integer.MAX_VALUE;
		boolean allowRevBuries = (Integer.parseInt(mProps.getProperty("ALLOW_REV_BURIES")) == 1);

		Log.msg("read bury alignment output from output files in " + mInitialBuryDir.getAbsolutePath(),LogLevel.Detail);
		for (int i = 0; i < blastFiles.length; i++)
		{
			File f = blastFiles[i];
			if (f == null) continue;
			Log.msg("reading " + f.getAbsolutePath(),LogLevel.Detail);
		
			BufferedReader r = new BufferedReader(new FileReader(f));
	
			while (null != (line = r.readLine()))
			{
				if (line.startsWith("#")) continue;
				if (nSingBuried >= max2bury) 
				{
					Log.msg("Bury limit reached",LogLevel.Detail);	
					break;
				}
				nRead++;
				if (nRead % 100000 == 0) Utils.singleLineMsg(nRead + " lines");
				String[] fields = line.trim().split("\\s+");
				if (fields.length != 12)
				{
					Log.msg("Bad line in " + f.getAbsolutePath() + " ("
							+ fields.length + " fields)",LogLevel.Basic);
					continue;
				}
				
				String name1 = fields[0];
				String name2 = fields[1].replace("lcl|",""); // megablast adds this annoying prefix sometimes
	
				if (name1.equals(name2)) 
				{
					rejects.put("same", 1 + rejects.get("same"));
					continue;
				}
				int nMis = Integer.parseInt(fields[4]);
				int qStart = Integer.parseInt(fields[6]);
				int qEnd = Integer.parseInt(fields[7]);
				int tStart = Integer.parseInt(fields[8]);
				int tEnd = Integer.parseInt(fields[9]);
				
				double eval = Double.parseDouble(fields[10]);
				float fScore = Float.parseFloat(fields[11]);
				int score = Math.round(fScore);
				
				if (nMis > maxMismatch) 
				{
					rejects.put("mismatch", 1 + rejects.get("mismatch"));
					continue;
				}
				
				boolean bRev = false;
				if (qStart > qEnd || tStart > tEnd) 
				{
					bRev = true;
					if (!allowRevBuries)
					{
						rejects.put("reversed", 1 + rejects.get("reversed"));
						continue; 
					}
					if (qStart > qEnd)
					{
						int tmp = qStart;
						qStart = qEnd;
						qEnd = tmp;
					}
					if (tStart > tEnd)
					{
						int tmp = tStart;
						tStart = tEnd;
						tEnd = tmp;
					}						
				}
				// it might be a user supplied blast file that doesn't exactly match what's loaded
				if (!mName2Clone.containsKey(name1))
				{
					unkClone.add(name1);
					continue;
				}
				if (!mName2Clone.containsKey(name2))
				{
					unkClone.add(name2);
					continue;
				}		
				
				Clone c1 = mName2Clone.get(name1);
				Clone c2 = mName2Clone.get(name2);
				
				if (c1.mSeqLen > c2.mSeqLen)
				{
					// switch everything up so the first is the potential child
					Clone c = c1;
					c1 = c2;
					c2 = c;
					int tmp = qEnd;
					qEnd = tEnd;
					tEnd = tmp;
					tmp = qStart;
					qStart = tStart;
					tStart = tmp;
				}
				int qMatchLen = qEnd - qStart + 1;
				
				// impose alphabetic order on sequences of identical length - to prevent looping
				if (c1.mSeqLen == c2.mSeqLen)
				{
					if (c1.mFullName.compareTo(c2.mFullName) > 0)
					{
						rejects.put("alpha", 1 + rejects.get("alpha"));
						continue;  				
					}
				}
				// Finally let's see if the query actually buries in the target!
				if (c1.mSeqLen - qMatchLen > maxMismatch)
				{
					rejects.put("nocontain", 1 + rejects.get("nocontain"));
					continue;
	
				}			
						
				if (eval > highestEval) highestEval = eval;
				if (score < lowestScore) lowestScore = score;
	
				c1.mFlipped = bRev; // This doesn't always work but it doesn't matter anymore
									// b/c we are re-computing the alignment direction at the end anyway. 
				
				if (c1.mMateID == 0)
				{				
					c1.mNParents++;
					if (c1.mParent1 == null)
					{
						// This is the first possible parent
						// Just save it for later, because we want to make sure there are at least two.
						// --do this in such a way that we can require N rather than 2.
						c1.mParent1 = c2;
					}
					else
					{
						if (c1.mParent == null)
						{
							// We've hit a second possible parent
							// Make the *first* one the parent temporarily, and then we'll compare lengths below
							nSingBuried++;
							c1.mParent = c1.mParent1;
							c1.mParent.mKids.add(c1.mID);
						}
						if (c1.mParent != null && c2.mSeq.length() < c1.mParent.mSeq.length())
						{
							c1.mParent.mKids.remove(c1.mID);
							c1.mParent = c2;
							c1.mParent.mKids.add(c1.mID);
						}
					}		
				} 
				else
				{
					// mated: keep a list of all possible parents so we can compute
					// the pair buries later
					c1.mParents.add(c2.mID);
				}
				fields = null;
			}
			r.close();
		}
		int nUnk = unkClone.size();
		if (nUnk > 0) 
		{
			Log.indentMsg(nUnk + " unknown reads in blast file",LogLevel.Basic);
			Log.indentMsg("Example: " + unkClone.first(), LogLevel.Basic);
		}
		Log.indentMsg(nRead + " bury alignment lines read",LogLevel.Basic);
		Log.indentMsg(nSingBuried + " reads buried",LogLevel.Basic);
		for (String reason : rejects.keySet())
		{
			int count = rejects.get(reason);
			Log.msg("Bury Rejected:" + reason + ":" + count, LogLevel.Detail);
		}
		Log.msg("Highest eval accepted " + highestEval + " lowest score " + lowestScore,LogLevel.Detail);
		int avgPar = 0;
		int maxPar = 0;
		Clone cMax  = null;
		for (Clone c : mClones)
		{
			avgPar += c.mNParents;
			if (c.mNParents > maxPar)
			{
				maxPar = c.mNParents;
				cMax = c;
			}
		}
		avgPar /= mClones.length;
		if (cMax != null)
		{
			Log.msg("Avg number of bury parents:" + avgPar + " max:" + maxPar + "(" + cMax.mFullName + ")",LogLevel.Detail);
		}
	}

	private void readPairCliqueBlast() throws Exception
	{
		File[] blastFiles = new File[1000]; 
		for (int i = 0; i < blastFiles.length; i++)
		{
			blastFiles[i] = null;	
		}
		
		int nFound = 0;
		for (int p = 0; p < 1000; p++) // since they may have changed the thread number in the config file
		{	
			String fname = "pair_blast_result" + p;
			File f = new File(mCliqueDir,fname);
			if (!f.exists())
			{
				break;	
			}
			blastFiles[nFound] = f;
			nFound++;
		}
		
		int nOlap = 0;
		int minmatch = Integer.parseInt(mProps.getProperty("MIN_MATCH_CLIQUE"));
		int maxhang = Integer.parseInt(mProps.getProperty("MAX_HANG_CLIQUE"));
		Float minid = Float.parseFloat(mProps.getProperty("MIN_ID_CLIQUE"));

		int minmatchself = Integer.parseInt(mProps.getProperty("MIN_MATCH_SELF"));
		int maxhangself = Integer.parseInt(mProps.getProperty("MAX_HANG_SELF"));
		Float minidself = Float.parseFloat(mProps.getProperty("MIN_ID_SELF"));

		mPairOlaps = new SparseMatrix(mClones.length,10); // overkill but simplest...
		
		TreeSet<String> unkClone = new TreeSet<String>();

		int linenum = 0;
		for (int i = 0; i < blastFiles.length; i++)
		{
			File f = blastFiles[i];
			if (f == null) continue;
			
			Utils.singleLineMsg("Reading pair alignment results");
			Log.msg("reading " + f.getAbsolutePath(),LogLevel.Detail);
	
			BufferedReader r = new BufferedReader(new FileReader(f));
			String line;
			
			while (null != (line = r.readLine()))
			{
				linenum++;
				if (line.startsWith("#")) continue;
				
				String[] fields = line.trim().split("\\s+");
				if (fields.length != 12)
				{
					Log.msg("Bad line in " + f.getAbsolutePath() + " ( line # "	+ linenum + "," + fields.length + " fields)",LogLevel.Basic);
					continue;
				}
				
				String name1 = fields[0];
				String name2 = fields[1].replace("lcl|",""); // megablast adds this stupid prefix 
		
				// user may provide their own blast file which may not match what was loaded exactly
				if (!mName2Clone.containsKey(name1))
				{
					unkClone.add(name1);
					continue;
				}
				
				if (!mName2Clone.containsKey(name2))
				{
					unkClone.add(name2);
					continue;
				}		
	
				Clone c1 = mName2Clone.get(name1);
				Clone c2 = mName2Clone.get(name2);
	
				if (c1.mPair == null || c1.mParent != null)
				{
					continue;
				}
				if (c2.mPair == null || c2.mParent != null)
				{
					continue;
				}
				
				Float pctID = Float.parseFloat(fields[2]);		
				int alignLen = Integer.parseInt(fields[3]);
				//int nMis = Integer.parseInt(fields[4]);
				int qStart = Integer.parseInt(fields[6]);
				int qEnd = Integer.parseInt(fields[7]);
				int tStart = Integer.parseInt(fields[8]);
				int tEnd = Integer.parseInt(fields[9]);
	
				boolean flipped = ((qEnd - qStart)*(tEnd - tStart) < 0);
					
				int len1 = c1.mSeqLen;
				int len2 = c2.mSeqLen;
				
				int tmp;
				if (qEnd  < qStart)
				{
					tmp = qEnd;
					qEnd = qStart;
					qStart = tmp;
				}
				if (tEnd  < tStart)
				{
					tmp = tEnd;
					tEnd = tStart;
					tStart = tmp;
				}
				
				int hang1;
				int hang2;
				if (!flipped)
				{
					hang1 = Math.min(qStart, tStart);
					hang2 = Math.min(len1 - qEnd, len2 - tEnd);
				}
				else
				{
					hang1 = Math.min(qStart, len2 - tEnd);
					hang2 = Math.min(len1 - qEnd, tStart);
				}
				assert(hang1 > 0 && hang2 > 0);
				int hang = Math.max(hang1,hang2);
				
				MatePair p1 = c1.mPair;
				MatePair p2 = c2.mPair;
				int pidx1 = p1.mIdx;
				int pidx2 = p2.mIdx;
				if (p1 != p2 && pidx1 == pidx2)
				{
					throw(new Exception("pair index problem"));
				}
				if (pidx1 == pidx2)
				{
					// same pair, i.e., self-overlap of pair
	
					if (pctID < minidself) continue;
					if (alignLen < minmatchself) continue;
					if (hang > maxhangself) continue;
	
					mSelfOlaps.add(p1);
					p1.mSelfOlap = true;
				} 
				else
				{
					if (pctID < minid) continue;
					if (alignLen < minmatch) continue;
					if (hang > maxhang) continue;
	
					// for paired we have to save off the hits and go back through again
					// 
					// for each hit we save the r/f values of query, target so
					// we can verify allowed combinations
	
					RF rf1 = c1.mRF;
					RF rf2 = c2.mRF;
					if (rf1 == RF.Unk)
					{
						Log.die("paired clone with unknown rf! " + c1.mFullName);
					}
					if (rf2 == RF.Unk)
					{
						Log.die("paired clone with unknown rf! " + c2.mFullName);
					}
					if (pidx1 > pidx2)
					{
						tmp = pidx1;
						pidx1 = pidx2;
						pidx2 = tmp;
						RF rf = rf1;
						rf1 = rf2;
						rf2 = rf;
					}
					PairOlap po = (rf1 == RF.R ? (rf2 == RF.R ? PairOlap.RR : PairOlap.RF)
							: (rf2 == RF.R ? PairOlap.FR : PairOlap.FF));
					MatePair p = mPairs[pidx1];
					if (!p.mOlapCandidates.containsKey(pidx2))
					{
						p.mOlapCandidates.put(pidx2, new HashSet<PairOlap>());
					}
					p.mOlapCandidates.get(pidx2).add(po);
				}
				
				nOlap++;
				line = null;
				fields = null;			
			}
		}
		for (MatePair p : mPairs)
		{
			for (int pidx : p.mOlapCandidates.keySet())
			{
				if (pidx <= p.mIdx)	Log.die("pair overlap tables corrupted!!");
				HashSet<PairOlap> combos = p.mOlapCandidates.get(pidx);
				if ((combos.contains(PairOlap.RR) && combos.contains(PairOlap.FF))
						|| (combos.contains(PairOlap.RF) && combos.contains(PairOlap.FR)))
				{
					mPairOlaps.set(p.mIdx,pidx);
					mPairOlaps.set(pidx,p.mIdx);
				}
			}
		}
		Log.indentMsg("Loaded " + nOlap 	+ " paired clone overlaps for clique creation",LogLevel.Basic);
		int nUnk = unkClone.size();
		if (nUnk > 0) 
		{
			Log.indentMsg(nUnk + " unknown reads in blast files",LogLevel.Basic);
			Log.indentMsg("Example: " + unkClone.first(), LogLevel.Basic);
		}
	}
	private void readSingleCliqueBlast() throws Exception
	{	
		File[] blastFiles = new File[1000]; 
		for (int i = 0; i < blastFiles.length; i++)
		{
			blastFiles[i] = null;	
		}
		
		int nFound = 0;
		for (int p = 0; p < 1000; p++) // since they may have changed the thread number in the config file
		{	
			String fname = "single_blast_result" + p;
			File f = new File(mCliqueDir,fname);
			if (!f.exists())
			{
				break;	
			}
			blastFiles[nFound] = f;
			nFound++;
		}

		int minmatch = Integer.parseInt(mProps.getProperty("MIN_MATCH_CLIQUE"));
		int maxhang = Integer.parseInt(mProps.getProperty("MAX_HANG_CLIQUE"));
		Float minid = Float.parseFloat(mProps.getProperty("MIN_ID_CLIQUE"));

		int nOlap = 0;

		mSingleOlaps = new SparseMatrix(mID2Clone.numSpaces(),10); 

		TreeSet<String> unkClone = new TreeSet<String>();
		
		Utils.singleLineMsg("Reading alignment results");
		Log.msg("read alignments results from " + mCliqueDir.getAbsolutePath(),LogLevel.Detail);

		int linenum = 0;
		boolean msgPrinted = false;
		for (int i = 0; i < blastFiles.length; i++)
		{
			File f = blastFiles[i];
			if (f == null) continue;
			Log.msg("reading " + f.getAbsolutePath(), LogLevel.Detail);
			BufferedReader r = new BufferedReader(new FileReader(f));
			String line;
	
			while (null != (line = r.readLine()))
			{
				linenum++;
				if (line.startsWith("#")) continue;
				
				String[] fields = line.trim().split("\\s+");
				if (fields.length != 12)
				{
					Log.msg("Bad line in " + f.getAbsolutePath() + " ( line # "	+ linenum + "," + fields.length + " fields)",LogLevel.Basic);
					continue;
				}
				
				String name1 = fields[0];
				String name2 = fields[1].replace("lcl|",""); // megablast adds this prefix
	
				// user may provide their own blast file which may not match what was loaded exactly
				if (!mName2Clone.containsKey(name1))
				{
					unkClone.add(name1);
					continue;
				}
				
				if (!mName2Clone.containsKey(name2))
				{
					unkClone.add(name2);
					continue;
				}		
	
				Clone c1 = mName2Clone.get(name1);
				Clone c2 = mName2Clone.get(name2);
				
				if (name1.compareTo(name2) >= 0) continue;
	
				if (c1.mPair != null || c1.mParent != null)
				{
					continue;
				}
				if (c2.mPair != null || c2.mParent != null)
				{
					continue;
				}
				
				Float pctID = Float.parseFloat(fields[2]);		
				int alignLen = Integer.parseInt(fields[3]);
				int qStart = Integer.parseInt(fields[6]);
				int qEnd = Integer.parseInt(fields[7]);
				int tStart = Integer.parseInt(fields[8]);
				int tEnd = Integer.parseInt(fields[9]);
	
				boolean flipped = ((qEnd - qStart)*(tEnd - tStart) < 0);
					
				int len1 = c1.mSeqLen;
				int len2 = c2.mSeqLen;
				
				int tmp;
				if (qEnd  < qStart)
				{
					tmp = qEnd;
					qEnd = qStart;
					qStart = tmp;
				}
				if (tEnd  < tStart)
				{
					tmp = tEnd;
					tEnd = tStart;
					tStart = tmp;
				}
				
				int hang1;
				int hang2;
				if (!flipped)
				{
					hang1 = Math.min(qStart, tStart);
					hang2 = Math.min(len1 - qEnd, len2 - tEnd);
				}
				else
				{
					hang1 = Math.min(qStart, len2 - tEnd);
					hang2 = Math.min(len1 - qEnd, tStart);
				}
				assert(hang1 >= 0 && hang2 >= 0);
				int hang = Math.max(hang1,hang2);
				if (pctID < minid) continue;
				if (alignLen < minmatch) continue;
				if (hang > maxhang) continue;
							
				int i1 = c1.mID - mID2Clone.minKey();
				int i2 = c2.mID - mID2Clone.minKey();
	
				mSingleOlaps.set(i1, i2);
				mSingleOlaps.set(i2, i1);
	
				nOlap++;
				
				if (nOlap % 1000000 == 0) 
				{
					Utils.singleLineMsg(nOlap + " valid overlaps read");
					if (nOlap > 300000000 && msgPrinted == false)
					{
						msgPrinted = true;
					}
				}
				line = null;
				fields = null;
			}
		}				

		Log.indentMsg("Loaded " + nOlap + " unpaired overlaps for clique creation",LogLevel.Basic);
		int nUnk = unkClone.size();
		if (nUnk > 0) 
		{
			Log.msg(nUnk + " unknown reads in blast file",LogLevel.Basic);
			Log.indentMsg("Example: " + unkClone.first(), LogLevel.Basic);
		}

	}
	// Look through the previously-saved possible parents for mated ESTs, and
	// find those that have an acceptable mated bury (i.e., into another mate pair).
	private void buildMatedBuryGraph() throws Exception
	{
		if (singlesOnly())
		{
			return;	
		}
		
		int min2keep = Integer.parseInt(mProps.getProperty("MIN_UNBURIED"));
		int max2bury = mClones.length - min2keep;		
		int nburied = 0;
		int pairsBuried = 0;
		for (Clone c : mClones)
		{
			if (c.mParent != null) nburied++;	
		}

		for (MatePair m : mPairs)
		{
			if (m.m3Prime.mParents.size() < 2 || m.m5Prime.mParents.size() < 2) continue;
			
			int bestLen = 0;
			MatePair bestPair = null;
			for (Integer p3id : m.m3Prime.mParents)
			{
				Clone p3 = mID2Clone.get(p3id);
				if (p3.mPair != null) 
				{
					MatePair mparent = p3.mPair; 
					if (checkPairBury(m, mparent))
					{
						if (bestPair == null)
						{
							bestLen = mparent.length();
							bestPair = mparent;
							pairsBuried++;						
						}
						else if (mparent.length() < bestLen)
						{
							bestLen = mparent.length();
							bestPair = mparent;
						}
					}
				}
			}
			if (bestPair != null)
			{
				m.mParent = bestPair;
				bestPair.mKids.add(m.mIdx);
				nburied += 2;
				if (nburied >= max2bury) break;
			}
		}
		if (pairsBuried > 0)
		{
			Log.indentMsg(pairsBuried + " pairs buried",LogLevel.Basic);
			Log.indentMsg(nburied + " total reads buried",LogLevel.Basic);
		}
	}
	// If pairs are contained backwards (.r in .f and vice versa) alert the user.
	private boolean checkPairBury(MatePair c, MatePair p)
	{
		if (c.m3Prime.mParents.contains(p.m3Prime.mID)
				&& c.m5Prime.mParents.contains(p.m5Prime.mID))
		{

			return true;
		} 
		else if (c.m3Prime.mParents.contains(p.m5Prime.mID)
				&& c.m5Prime.mParents.contains(p.m3Prime.mID))
		{
			Log.msg("Backwards pair containment (" + c.m3Prime.mFullName + ","
					+ c.m5Prime.mFullName + ") < " + " (" + p.m5Prime.mFullName
					+ "," + p.m3Prime.mFullName + ")",LogLevel.Detail);
			return true;
		}
		return false;
	}

	private void buryDataToDB() throws Exception
	{
		Log.msg("load bury data to database",LogLevel.Detail);
		renewConnections();
		mDB.executeUpdate("delete from buryclone where AID=" + mAID);
		PreparedStatement ps = mDB.prepareStatement("insert into buryclone "
						+ "(AID,assemblyid,CID_child,CID_parent,childid,parentid,flipped,bcode) "
						+ " values(" + mAID + ",'" + mIDStr + "',?,?,?,?,?,'" + BuryType.Blast.toString() + "') ");

		int thresh = 0; 
		int i = 0;
		for (Clone c : mClones)
		{
			if (c.mMate != null) continue;
			if (c.mParent != null)
			{
				if(c.mBuryLevel >= thresh)
				{
					ps.setInt(1, c.mID);
					ps.setInt(2, c.mParent.mID);
					ps.setString(3, c.mFullName);
					ps.setString(4, c.mParent.mFullName);
					ps.setInt(5, (c.mFlipped ? 1 : 0));
					ps.addBatch();
					i++;
					if (i % 1000 == 0)
					{
						ps.executeBatch();
					}
					if (i % 100000 == 0)
					{
						Log.msg(i + " loaded", LogLevel.Detail);
					}
				}
				else
				{
					c.mParent = null;
				}
			}
		}
		for (MatePair m : mPairs)
		{
			if (m.mParent != null)
			{
				if (m.mBuryLevel >= thresh)
				{
					ps.setInt(1, m.m3Prime.mID);
					ps.setInt(2, m.mParent.m3Prime.mID);
					ps.setString(3, m.m3Prime.mFullName);
					ps.setString(4, m.mParent.m3Prime.mFullName);
					ps.setInt(5, (m.m3Prime.mFlipped ? 1 : 0));
					ps.addBatch();
					i++;
					if (i % 1000 == 0)
					{
						ps.executeBatch();
					}
					if (i % 100000 == 0)
					{
						Log.msg(i + " loaded", LogLevel.Detail);
					}
	
					ps.setInt(1, m.m5Prime.mID);
					ps.setInt(2, m.mParent.m5Prime.mID);
					ps.setString(3, m.m5Prime.mFullName);
					ps.setString(4, m.mParent.m5Prime.mFullName);
					ps.setInt(5, (m.m5Prime.mFlipped ? 1 : 0));
					ps.addBatch();
					i++;
					if (i % 1000 == 0)
					{
						ps.executeBatch();
					}
					if (i % 100000 == 0)
					{
						Log.msg(i + " loaded", LogLevel.Detail);
					}
				}
				else
				{
					m.mParent = null;
				}
			}
		}
		if (i % 1000 != 0) 
		{
			ps.executeBatch();
		}
		ps.close();
		Log.msg("Loaded " + i + " buries to database",LogLevel.Detail);
	}

	// These two routines read the bury blast data, load the buries to
	// the database, and write the unburied ESTs for clique calculation.
	// these steps could be better organized.
	private void processBuries() throws Exception
	{
		String thisTag = "FIRST_BURY";
		if (Utils.checkUpToDate(thisTag, null,null))
		{
			return;
		}
		
		Utils.termOut("Reading bury alignment results");
		readBlastBuryData();
		
		buildMatedBuryGraph();
		buryDataToDB();
		Utils.writeStatusFile(thisTag);
	}
		
	private void writeUnburiedSingle() throws Exception
	{
		Utils.termOut("Write non-buried to file for clique alignment");
		File fSing = new File(mCliqueDir, "unburied_single.fasta");
		if (fSing.exists())	fSing.delete();
		if (!fSing.createNewFile())
		{
			Log.die("Unable to create file " + fSing.getAbsolutePath());
		}
		BufferedWriter bwSing = new BufferedWriter(new FileWriter(fSing));
		int i = 0;
		int maxLen = Integer.parseInt(mProps.getProperty("MAX_MERGE_LEN"));

		for (Clone c : mClones)
		{
			if (c.mMate != null) continue;
			if (maxLen > 0 && c.mSeqLen > maxLen) continue;
			if (c.mParent == null)
			{
				c.loadSequences(mDB);
				bwSing.write(">" + c.mFullName + "\n");
				bwSing.write(c.mSeq + "\n");
				c.clearSequences();
				i++;
			}
		}
		if (i == 0)
			fSing.delete();

		bwSing.flush();		
		bwSing.close();


	}
	private void writeUnburiedPair() throws Exception
	{	
		Utils.termOut("Write non-buried paired reads to file for clique alignment");

		File fMate = new File(mCliqueDir, "unburied_pair.fasta");
		if (fMate.exists()) fMate.delete();
		if (!fMate.createNewFile())
		{
			Log.die("Unable to create file " + fMate.getAbsolutePath());
		}
		BufferedWriter bwMate = new BufferedWriter(new FileWriter(fMate));

		int i = 0;
		int maxLen = Integer.parseInt(mProps.getProperty("MAX_MERGE_LEN"));

		for (MatePair m : mPairs)
		{
			if (m.mParent == null)
			{
				if (maxLen > 0 && (m.m3Prime.mSeqLen > maxLen || m.m5Prime.mSeqLen > maxLen)) continue;
				
				m.m3Prime.loadSequences(mDB);
				m.m5Prime.loadSequences(mDB);
				bwMate.write(">" + m.m3Prime.mFullName + "\n");
				bwMate.write(m.m3Prime.mSeq + "\n");
				bwMate.write(">" + m.m5Prime.mFullName + "\n");
				bwMate.write(m.m5Prime.mSeq + "\n");
				m.m3Prime.clearSequences();
				m.m5Prime.clearSequences();
				
				i += 2;
			}
		}
		if (i == 0)
			fMate.delete();

		bwMate.flush();
		bwMate.close();
	}
	private void loadCloneParents() throws Exception
	{
		for (Clone c : mClones)
		{
			c.mParent = null;
			c.mKids.clear();
		}

		ResultSet rs = mDB.executeQuery("select CID_child,CID_parent " + " from buryclone where AID=" + mAID );
		while (rs.next())
		{
			int cid = rs.getInt("CID_child");
			int pid = rs.getInt("CID_parent");
			
			mID2Clone.get(cid).mParent = mID2Clone.get(pid);
		}

	}
	// Does not load the sequence or quality, to save memory
	private void loadClones() throws Exception
	{
		Log.head("Load reads from database",LogLevel.Basic);
		ResultSet rs = null;

		mName2Clone.clear(); 
		mID2Clone.clear(); 
		mClones = null;
		mPairs = null;

		ID2Obj<MatePair> id2pair = new ID2Obj<MatePair>();
		
		int numpairs = 0;
		for (Library lib : mLibs)
		{
			rs = mDB.executeQuery("select count(*) as count from clone where LID=" + lib.mID + " and mate_CID > 0");
			rs.first();
			int count = rs.getInt("count");
			if (count % 2 != 0)
			{
				Log.die("Odd number of mated reads in " + lib.mIDStr + " ; reload the dataset before proceeding");
			}
			numpairs += count/2;
			rs.close();
		}
		
		int minID = -1;
		int maxID = 0;
		int numclones = 0;
		
		for (Library lib : mLibs)
		{
			rs = mDB.executeQuery("select min(CID) as minc, max(CID) as maxc, count(*) as count from clone where LID=" + lib.mID + "");
			rs.first();
			int min = rs.getInt("minc");
			int max = rs.getInt("maxc");
			if (minID == -1) minID = min;
			minID = Math.min(min, minID);
			maxID = Math.max(max,maxID);
			numclones += rs.getInt("count");
			rs.close();
		}
		
		mClones = new Clone[numclones];
		mPairs = new MatePair[numpairs];
		int cloneIdx = 0;
		int pairIdx = 0;
		
		mID2Clone.setMinMax(minID,maxID);
		id2pair.setMinMax(minID,maxID);
		mName2Clone.setSize(numclones);

		Log.msg("Load bury table",LogLevel.Detail);
		ID2Obj<Integer> buries = new ID2Obj<Integer>();
		buries.setMinMax(minID, maxID);
		rs = mDB.executeQuery("select CID_child, CID_parent from buryclone where aid=" + mAID);
		while (rs.next())
		{
			int child = rs.getInt("CID_child");
			int parent = rs.getInt("CID_parent");
			buries.put(child,parent);
		}
		rs.close();
		Log.msg(buries.numKeys() + " buried reads",LogLevel.Detail);
		
		for (Library lib : mLibs)
		{
			String query = "select CID,cloneid,sense,mate_CID,length from clone where LID='" 
							+ lib.mID + "' order by CID asc";
			rs = mDB.executeQuery(query);
			while (rs.next())
			{
				int sense = rs.getInt("sense");
				RF rf;
				if (sense == -1)
				{
					rf = RF.R;
				} 
				else if (sense == +1)
				{
					rf = RF.F;
				} 
				else
				{
					rf = RF.Unk;
				}

				Clone cl = new Clone(rs.getString("cloneid"), rs.getString("cloneid"), rf, rs.getInt("CID"),
					    rs.getInt("mate_CID"), "",rs.getInt("length"),  lib, lib.mID,mDB);
				if (lib.mLongestClone == null || cl.mSeqLen > lib.mLongestClone.mSeqLen)
				{
					lib.mLongestClone = cl;	
				}
				if (lib.mShortestClone == null || cl.mSeqLen < lib.mLongestClone.mSeqLen)
				{
					lib.mShortestClone = cl;	
				}				
				
				mName2Clone.put(cl.mFullName, cl);
				mClones[cloneIdx] = cl;
				cloneIdx++;
				mID2Clone.put(cl.mID, cl);
				if (cl.mMateID != 0)
				{
					if (!id2pair.containsKey(cl.mMateID))
					{
						MatePair mp = new MatePair();
						mp.set(cl);
						id2pair.put(cl.mID, mp);
						id2pair.put(cl.mMateID, mp);
						mPairs[pairIdx] = mp;
						mp.mIdx = pairIdx;
						pairIdx++;
					} 
					else
					{
						id2pair.get(cl.mMateID).set(cl);
					}
				}
			}
			rs.close();
		}
		for (int i = 0; i < mClones.length; i++)
		{
			Clone cl = mClones[i];
			if (buries.containsKey(cl.mID))
			{
				cl.mParent = mID2Clone.get(buries.get(cl.mID));
			}
		}				
		mName2Clone.sort();
		boolean ok = true;
		for (MatePair mp : mPairs)
		{
			if (mp.m3Prime == null && mp.m5Prime == null)
			{
				throw (new Exception("Empty MatePair!!"));
			}
			if (mp.m3Prime == null)
			{
				Log.msg("Missing partner for " + mp.m5Prime.mFullName,LogLevel.Basic);
				ok = false;
			} 
			else
			{
				mp.m3Prime.mPair = mp;
				mp.m3Prime.mMate = mp.m5Prime;
			}
			if (mp.m5Prime == null)
			{
				Log.msg("Missing partner for " + mp.m3Prime.mFullName,LogLevel.Basic);
				ok = false;
			} 
			else
			{
				mp.m5Prime.mPair = mp;
				mp.m5Prime.mMate = mp.m3Prime;
			}
		}
		if (!ok)
		{
			Log.die("Mate pairs did not load properly; reload libraries to proceed");
		}

		int numBuried = buries.numKeys();
		Log.indentMsg(numclones + " Reads loaded", LogLevel.Basic);
		if (numpairs > 0)
		{
			Log.indentMsg("There are " + numpairs + " mate pairs", LogLevel.Basic);
		}	
		if (numBuried > 0)
		{
			Log.indentMsg("There are " + numBuried + " buried reads", LogLevel.Basic);
		}	
		
		buries.clear();buries=null;
		id2pair.clear(); id2pair = null;
	}
	private boolean pairsOnly()
	{
		return (mClones.length == 2*mPairs.length);
	}
	private boolean singlesOnly()
	{
		return (mPairs.length == 0);
	}
	
	private static void printUsage()
	{
		System.err.println("Usage:  AssemMain <directory>");
		System.err.println("    <directory> must be under the 'projects' directory");
		System.exit(-1);

	}

	// Make sure we can find the programs we're going to need
	private void checkCommands() throws Exception
	{
		checkCommand(BlastArgs.getFormatCmd());
		checkCommand(BlastArgs.getBlastnExec());
	}

	private static void checkCommand(String cmd)
	{
		if (cmd.contains(" "))
		{
			Log.die("Command path contains a space:" + cmd);
		}
		try
		{
			Utils.runCommand(cmd, false, false,0);
		} 
		catch (Exception e)
		{
			Log.msg(e.toString() + "," + e.getMessage(),LogLevel.Detail);
			Log.die("Could not run program " + cmd + " : make sure it is in your path or specify the path in HOSTS.cfg");			
		}
	}

	private void buildSingleCliques() throws Exception
	{
		String thisTag = "CLIQUE_BUILD_SINGLE";
		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			return;
		}
		Utils.deleteStatusFile(thisTag);
		recordTime(thisTag + " start");
		readSingleCliqueBlast();
		
		buildCliques(mSingleOlaps, mSingleCliques, Integer.parseInt(mProps.getProperty("CLIQUE_SIZE_SING")),Integer.parseInt(mProps.getProperty("MAX_CLIQUE_SIZE_SING")));
		Log.indentMsg(mSingleCliques.size() + " unpaired cliques created",LogLevel.Basic);
		Utils.termOut("Uploading results to database");
		PreparedStatement st = mDB.prepareStatement("insert into ASM_clique_clone (CQID,CID) values(?,?)");
		int ic = 0;
		for (TreeSet<Integer> cl : mSingleCliques)
		{
			mDB.executeUpdate("insert into ASM_cliques (AID,type) values(" + mAID + ",'" + CliqueType.Single.toString() + "')");
			int CQID = mDB.lastID();
			for (Integer j : cl)
			{
				Clone c =  mID2Clone.get(j + mID2Clone.minKey());
				st.setInt(1, CQID);
				st.setInt(2, c.mID);
				st.addBatch();
				ic++;
				if (ic % 1000 == 0)	st.executeBatch();
			}
			mDB.executeUpdate("update ASM_cliques set nclone='" + cl.size()	+ "' where CQID='" + CQID + "'");
		}
		if (ic % 1000 != 0) st.executeBatch();
		st.close();
		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
	}

	private void buildPairCliques() throws Exception
	{
		String thisTag = "CLIQUE_BUILD_PAIR";
		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			return;
		}
		Utils.deleteStatusFile(thisTag);
		
		recordTime(thisTag + " start");
		readPairCliqueBlast();
		
		buildCliques(mPairOlaps, mPairCliques, Integer.parseInt(mProps.getProperty("CLIQUE_SIZE_PAIR")),Integer.parseInt(mProps.getProperty("MAX_CLIQUE_SIZE_PAIR")));
		Log.indentMsg(mPairCliques.size() + " mate pair cliques created",LogLevel.Basic);
		Utils.termOut("Uploading results to database");
		PreparedStatement st = mDB.prepareStatement("insert into ASM_clique_clone (CQID,CID) values(?,?)");
		TreeSet<Integer> pairsUsed = new TreeSet<Integer>();
		for (TreeSet<Integer> cl : mPairCliques)
		{
			mDB.executeUpdate("insert into ASM_cliques (AID,type) values("	+ mAID + ",'" + CliqueType.Paired.toString() + "')");
			int CQID = mDB.lastID();
			int ic = 0;
			for (Integer j : cl)
			{
				MatePair m = mPairs[j];
				pairsUsed.add(j);
				st.setInt(1, CQID);
				st.setInt(2, m.m3Prime.mID);
				st.addBatch();
				st.setInt(2, m.m5Prime.mID);
				st.addBatch();

				ic += 2;
				if (ic % 1000 == 0) st.executeBatch();
			}
			if (ic % 1000 != 0) st.executeBatch();
			mDB.executeUpdate("update ASM_cliques set nclone=" + ic	+ " where CQID=" + CQID);
		}
		st.close();
		
		// each self overlap of a pair becomes a clique itself, if 
		// not already used
		if (mSelfOlaps.size() > 0)
		{
			Log.indentMsg(mSelfOlaps.size() + " mate pair self-overlaps",LogLevel.Basic);
			Utils.termOut("Uploading results to database");
			st = mDB.prepareStatement("insert into ASM_clique_clone (CQID,CID) values(?,?)");
			int ic = 0;
			for (MatePair m : mSelfOlaps)
			{
				if (pairsUsed.contains(m.mIdx))
				{
					continue;
				}
				mDB.executeUpdate("insert into ASM_cliques (AID,type,nclone) values("
								+ mAID
								+ ",'"
								+ CliqueType.Self.toString()
								+ "',2)");
				int CQID = mDB.lastID();
				st.setInt(1, CQID);
				st.setInt(2, m.m3Prime.mID);
				st.addBatch();
				st.setInt(2, m.m5Prime.mID);
				st.addBatch();

				ic += 2;
				if (ic % 100 == 0) 	st.executeBatch();

			}
			if (ic % 100 != 0)	st.executeBatch();
			st.close();
		}
		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
	}
	private int initCliques() throws Exception
	{
		ResultSet rs = null;
		int TCID = 0;
		File capTopDir = new File(mCliqueDir,"cap");
		Utils.checkCreateDir(capTopDir);
		
		// Did we start the cliques?
		rs = mDB.executeQuery("select TCID from ASM_tc_iter where AID=" + mAID + " and tcnum=0");
		if (rs.first())
		{
			TCID = rs.getInt("TCID");
		}
		else
		{
			// This stage will be called "TC0"
			mDB.executeUpdate("insert into ASM_tc_iter (AID, tcnum, tctype) " +
					" values(" + mAID + ",0,'" + IterType.Clique.toString() + "')");
			TCID = mDB.lastID();

			Utils.clearDir(capTopDir);			
		}
		rs.close();
		
		return TCID;
	}
	private void assembleCliques(int TCID) throws Exception
	{
		String thisTag = "CLIQUE_ASSEMBLE";
		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			return;
		}
		Log.head("Clique assembly",LogLevel.Basic);
		Utils.deleteStatusFile(thisTag);
		File capTopDir = new File(mCliqueDir,"cap");
		recordTime(thisTag + " start");
		
		// Get the unassembled cliques 
		
		int numLeft = mDB.executeCount("select count(*) from ASM_cliques " +
				"where AID=" + mAID + " and assembled=0 order by nclone desc");
		
		Log.indentMsg(numLeft + " cliques to assemble",LogLevel.Basic);
		if (numLeft > 0)
		{
			int nThreads = Integer.parseInt(mProps.getProperty("CPUs"));
			CliqueThread[] threadList = new CliqueThread[nThreads];
			for (int i = 0; i < nThreads; i++)
			{
				threadList[i] = new CliqueThread(this, capTopDir,TCID,i);
			}
			mThreadWorkList = new int[numLeft];
			int i = 0;
			
			ResultSet rs = mDB.executeQuery("select CQID from ASM_cliques where AID=" + mAID + " and assembled=0 order by nclone desc");
			i = 0;
			while (rs.next())
			{
				int CQID = rs.getInt("CQID");
				mThreadWorkList[i] = CQID;
				i++;
			}
			rs.close();
			
			mThreadWorkItem = 0;
			mThreadsFinished = 0;
				
			Log.columns(15, LogLevel.Detail,"ThreadNum","Type","#Read","#Ctgs","ID","Error");
	
			for (i = 0; i < nThreads; i++)
			{
				threadList[i].start();
			}
			
			Date timeStart = new Date();
			while (mThreadsFinished < nThreads)
			{
				Date timeNow = new Date();
				long elapsed = timeNow.getTime() - timeStart.getTime();
				timeNow = null;
				if (elapsed/1000 > 60)
				{
					timeStart = null;
					timeStart = new Date();
					int nRemaining = mThreadWorkList.length - mThreadWorkItem;
					Utils.singleLineMsg(nRemaining + " cliques to assemble");
				}

				Thread.sleep(1000);
			}
			mThreadWorkList = null;
		}
		renewConnections();
		int count = mDB.executeCount("select count(*) as count from contig where aid=" + mAID);
		
		Log.indentMsg(count + " contigs created", LogLevel.Basic);
		mDB.executeUpdate("update ASM_tc_iter set finished=1,ctgs_end=" + count + " where TCID=" + TCID);

		Utils.writeStatusFile(thisTag);	
		recordTime(thisTag + " end");
	}
	private void doUnpairedCliqueUpload(AceFile af, int TCID, DBConn db,CliqueType ct) throws Exception
	{
		// Use a transaction to guarantee the contig, ASM_scontig, contclone tables are updated consistently
		//db.openTransaction();
		//db.executeQuery("lock tables contig write,ASM_scontig write,ASM_cliques write,contclone write");
		try
		{				
			for (SubContig sctg : af.mContigs)
			{
				sctg.mTCID = TCID;
				if (sctg.hasOverhangs())
				{
					continue;
				}
				sctg.doUpload(db,"", false);
				Contig ctg = new Contig(mAID,sctg,null);
				ctg.upload(db,TCID);			
				ctg.clearSeqs();
			}
			db.executeUpdate("update ASM_cliques set assembled=1,cap_success=1 where CQID=" + af.mThisID);
			
			//db.closeTransaction();
			//db.executeQuery("unlock tables");
		}
		catch (Exception e)
		{
			db.rollbackTransaction();
			db.executeQuery("unlock tables");
			throw(e);
		}
	}	
	private void doPairedCliqueUpload(int CQID, SubContig[] ctgs, int TCID, DBConn db,CliqueType ct) throws Exception
	{
		TreeSet<Integer> uploaded = new TreeSet<Integer>();

		try
		{
			// Use a transaction to guarantee the contig, ASM_scontig, contclone tables are updated consistently
			// db.openTransaction
			for (int i = 0; i < ctgs.length; i++)
			{
				if (uploaded.contains(i)) continue;
				SubContig sctg1 = ctgs[i];
				if (sctg1.mDoUpload)
				{
					uploaded.add(i);			
					sctg1.mTCID = TCID;		
					sctg1.doUpload(db,"",false);
					Contig ctg = null;
					if (sctg1.mMateNum == -1)
					{
						throw(new Exception("clique " + CQID + " idx " + i + " no mate!!"));
					}
					
					if (sctg1.mMateNum != i ) 
					{						
						uploaded.add(sctg1.mMateNum);
						SubContig sctg2 = ctgs[sctg1.mMateNum];			
						sctg2.mTCID = TCID;
						sctg2.doUpload(db,"",false);
						sctg2.setMate(sctg1,db);
						sctg1.setMate(sctg2,db);
						ctg = new Contig(mAID,sctg1,sctg2);
						Log.msg("Uploaded " + i + " " + sctg1.mMateNum,LogLevel.Dbg);
					}
					else
					{
						ctg = new Contig(mAID,sctg1,null);
						Log.msg("Uploaded " + i + " self paired",LogLevel.Dbg);
					}
					ctg.upload(db,TCID);
				}			
			}
			// db.closeTransaction
			db.executeUpdate("update ASM_cliques set assembled=1,cap_success=1 where CQID=" + CQID);
		}
		catch (Exception e)
		{
			db.rollbackTransaction();
			throw(e);
		}
	}
	private void doSelfPairedCliqueUpload(AceFile af, int TCID, DBConn db,CliqueType ct) throws Exception
	{
		SubContig sctg1 = af.mContigs.get(0);
		if (af.mContigs.size() > 1)
		{
			throw(new Exception("Not a self paired contig"));	
		}
		SubContig sctg2 = null; 
		
		sctg1.mTCID = TCID;
		
		// Use a transaction to guarantee the contig, ASM_scontig, contclone tables are updated consistently
		// db.openTransaction
		try
		{				
			sctg1.doUpload(db,"",false);
			sctg1.mCCS = "";
			sctg1.mQual = "";
			sctg1.setMate(sctg2, db);
			
			Contig ctg = new Contig(mAID,sctg1,sctg2);
			ctg.upload(db,TCID);
				
			db.executeUpdate("update ASM_cliques set assembled=1,cap_success=1 where CQID=" + af.mThisID);
		}
		catch (Exception e)
		{
			db.rollbackTransaction();
			throw(e);
		}
	}	

	// Helper functions used by a few of the threaded operations
	
	private synchronized void threadDone(int num)
	{
		Log.msg("Thread " + num + " finished!",LogLevel.Detail);
		mThreadsFinished++;
	}
	private synchronized int getNextThreadWorkItem()
	{
		int ret = 0;
		if (mThreadWorkItem < mThreadWorkList.length)
		{
			ret = mThreadWorkList[mThreadWorkItem];
			mThreadWorkItem++;
		}
		return ret;
	}
	private int threadItemsLeft()
	{
		return mThreadWorkList.length - mThreadWorkItem;
	}

	// Make initial "contigs" of the (unburied) single pairs and loners which didn't go
	// into contigs during clique assembly
	private void makeSingletons(int TCID) throws Exception
	{
		String thisTag = "SINGLES2CTGS";
		if (Utils.checkUpToDate(thisTag, "", "")) return;
		Utils.deleteStatusFile(thisTag);
		
		// First thing to do is get the error rate estimates using the cliques
		estimateErrorRates();
		
		Utils.singleLineMsg("Find remaining singletons");
		
		TreeSet<Integer> cliqueClones = new TreeSet<Integer>();
		ResultSet rs = mDB.executeQuery("select CID from contclone join contig on contig.CTGID=contclone.CTGID where contig.AID= " + mAID);
		while (rs.next())
		{
			int cid = rs.getInt(1);
			cliqueClones.add(cid);
		}
		rs.close();
		for (Clone c : mClones) c.mBuried = false;
		
		rs = mDB.executeQuery("select CID_child from buryclone where AID=" + mAID );
		while (rs.next())
		{
			int cid = rs.getInt(1);
			Clone c = mID2Clone.get(cid);
			c.mBuried = true;
		}
		rs.close();
		int cntNewCtgs = 0, cntPrt=0;
		for (MatePair p : mPairs)
		{
			if (cliqueClones.contains(p.m3Prime.mID)) continue;
			if (p.m3Prime.mBuried || p.m5Prime.mBuried) continue;
			
			Contig c = contigFromPair(p);
			c.mSC1.mTCID = TCID;
			c.mSC1.seqQualFromClone(mDB);
			c.mSC2.mTCID = TCID;
			c.mSC2.seqQualFromClone(mDB);
			c.mSC1.doUpload(mDB,"",false);
			c.mSC2.doUpload(mDB,"",false);
			c.upload(mDB,TCID);
			cntNewCtgs += 2; cntPrt++;
			if (cntPrt==1000) {
				Utils.singleLineMsg(cntNewCtgs + " contigs with mate");
				cntPrt=0;
			}
			c.clearSeqs();
			c.mSC1 = null;
			c.mSC2 = null;				
			c = null;
		}
		SubContig sc = null;
		Contig ctg = null;
		
		cntPrt=0;
		for (Clone c : mClones)
		{
			if (c.mMate != null) continue;
			if (cliqueClones.contains(c.mID)) continue;
			if (c.mBuried) continue;
			
			sc = new SubContig(mID2Clone,mAID,mIDStr,c);
			sc.mTCID = TCID;
			sc.seqQualFromClone(mDB);
			sc.doUpload(mDB,"",false);
			ctg = new Contig(mAID,sc,null);
			ctg.upload(mDB,TCID);
			sc.clear();
			ctg.clearSeqs();
			sc = null;
			ctg = null;
			cntNewCtgs++; cntPrt++;
			if (cntPrt==1000) {
				Utils.singleLineMsg(cntNewCtgs + " contigs without mate");
				cntPrt=0;
			}
		}
		Log.indentMsg(cntNewCtgs + " singleton contigs created",LogLevel.Basic); 
		mDB.executeUpdate("update ASM_tc_iter set ctgs_end = (select count(*) from ASM_scontig where aid=" + mAID + ") where tcid=" + TCID);
		Utils.writeStatusFile(thisTag);
	}

	private void estimateErrorRates() throws Exception
	{
		Utils.singleLineMsg("estimating sequencing error rates");
		ResultSet rs = mDB.executeQuery("select sum(mismatch) as mis, sum(numex) as ex from contclone " + 
				" join contig on contig.ctgid=contclone.ctgid where contig.aid=" + mAID);
		ResultSet rs2 = mDB2.executeQuery("select sum(length) as nbases from clone join contclone on contclone.cid=clone.cid " + 
						" join contig on contig.ctgid=contclone.ctgid where contig.aid=" + mAID);
		if (rs.first() && rs2.first())
		{
			float mis = rs.getFloat("mis");
			float ex = rs.getFloat("ex");
			long nbases = rs2.getLong("nbases");
			rs.close();rs2.close();
			if (nbases == 0)
			{
				Log.msg("Unable to estimate error rates; using default values.", LogLevel.Basic);
			}
			else
			{
				mis /= ((float)nbases);
				ex /= ((float)nbases);
				Log.indentMsg("Estimated error rates:" + mis*1000 + " miscalls/kb, " + ex*1000 + " extras/kb",LogLevel.Basic);
				
				mDB.executeUpdate("update assembly set erate='" + mis + "',exrate='" + ex + "' where aid=" + mAID);
			}
		}
		else
		{
			Log.msg("Unable to estimate error rates since no contigs!", LogLevel.Basic);	
		}
	}
	private Contig contigFromPair(MatePair p) throws Exception
	{
		SubContig sc1 = new SubContig(mID2Clone,mAID,mIDStr,p.m3Prime);
		SubContig sc2 = new SubContig(mID2Clone,mAID,mIDStr,p.m5Prime);
		
		Contig c = new Contig(mAID,sc1,sc2);
		
		return c;
	}

	// Assemble a collection of Reads. Used by all the operations that use cap. 
	private void assembleClones(File capDir, Integer[] cids, String capArgParam, int threadNum, DBConn db) throws Exception
	{
		int maxSize = Integer.parseInt(mProps.getProperty("MAX_CAP_SIZE"));
		if (cids.length > maxSize) 
		{
			Log.msg("Skipping large cap assembly of size " + maxSize, LogLevel.Detail);
			return;
		}
		Utils.clearDir(capDir);
		BufferedWriter seqFile = new BufferedWriter(new FileWriter(new File(capDir, "seqs.fasta")));
		BufferedWriter qualFile = new BufferedWriter(new FileWriter(new File(capDir, "seqs.fasta.qual")));
		String join = "(" + Utils.join(cids, ",") + ")";
		ResultSet rs = db.executeQuery("select CID,cloneid,sequence,quality from clone where CID in "
						+ join);

		while (rs.next())
		{
			int CID = rs.getInt("CID");
			String name = rs.getString("cloneid");
			String seq = rs.getString("sequence");
			String qual = rs.getString("quality");
			seqFile.write(">" + name + "\n" + seq + "\n");
			if (qual.equals(""))
			{
				qual = mID2Clone.get(CID).mLib.defQualStr(seq.length());
			} 
			else
			{
				int qlen = qual.split("\\s+").length;
				int slen = seq.length();
				if (qlen != slen)
				{
					Log.die("Bad quality values in database: qual length " + qlen
							+ ", seq length " + slen + " for clone " + name);
				}
			}
			qualFile.write(">" + name + "\n" + qual + "\n");
		}
		rs.close();
		seqFile.flush();
		qualFile.flush();

		String cmd = mProps.getProperty("CAP_CMD") + " seqs.fasta "
				+ mProps.getProperty(capArgParam);
		Utils.appendToFile(cmd,capDir,"cmd.list");
		
		int exitValue = Utils.runCommand(cmd.split("\\s+"), capDir, false,true, null,threadNum);
		Utils.checkExitValue(exitValue,cmd);
		seqFile.close();
		qualFile.close();		
	}

	private void buildCliques(SparseMatrix links, Stack<TreeSet<Integer>> cliques, int minSize, int maxSize) throws Exception
	{
		Utils.singleLineMsg("Building cliques");
		links.sortRows();
		
		Vector<Integer> sizes = new Vector<Integer>();

		Integer[] sortedLinks = new Integer[links.numRows()];
		for (int i = 0; i < links.numRows(); i++)
		{
			sortedLinks[i] = i;
			sizes.add(i, links.rowCount(i));
		}
		NbhdCmp cmp = new NbhdCmp(sizes);

		Arrays.sort(sortedLinks, cmp);

		TreeSet<Integer> used = new TreeSet<Integer>();
		for (int iu = 0; iu < links.numRows(); iu++)
		{
			int u = sortedLinks[iu];
			if (used.contains(u)) continue;
			int nlinks = links.rowCount(u);
			if (nlinks <= minSize)
				continue;
			Integer[] nbhd = links.getRowAsIntegers(u);
			Arrays.sort(nbhd, cmp);

			for (int iv = 0; iv < nbhd.length; iv++)
			{
				Integer v = nbhd[iv];

				if (used.contains(v) || used.contains(u)) continue; // have to re-check u as used may have changed

				TreeSet<Integer> cur = new TreeSet<Integer>();
				cur.add(u);
				cur.add(v);

				for (int ix = iv + 1; ix < nbhd.length; ix++)
				{
					Integer x = nbhd[ix];

					if (used.contains(x)) continue;

					boolean in = true;
					for (Integer y : cur)
					{
						if (!links.isSet(x,y))
						{
							in = false;
							break;
						}
					}
					if (in)
					{
						cur.add(x);
					}
				}
				if (cur.size() >= minSize)
				{
					for (Integer c : cur)
					{
						links.delete(c,u);
						sizes.set(c, -1 + sizes.get(c));
					}
					
					used.addAll(cur);
					if (cur.size() <= maxSize)
					{
						cliques.push(cur);
					}
					else
					{
						// break it up	
						Log.msg("Breaking up large clique of size " + cur.size(),LogLevel.Detail);
						TreeSet<Integer> cur2 = new TreeSet<Integer>();
						for (int zz : cur)
						{
							cur2.add(zz);
							if (cur2.size() >= maxSize)
							{
								cliques.push(cur2);
								cur2 = new TreeSet<Integer>();
							}
						}
						cliques.push(cur2);
					}
				}
				else
				{
					cur.clear();
					cur = null;
				}
			}
			nbhd = null;
		}
	}

	// Execute a TC step, using clustering to create non-overlapping chunks for different
	// threads to work on. 
	// Steps are taken to prevent any one cluster from being too big, which would negate the 
	// effects of threading.
	
	private void doTCByCluster(int tc, int maxtc) throws Exception
	{
		loadContigs(true,false, false); 

		int TCID;
		int finished = 0;
		boolean started = false;
		ResultSet rs = mDB.executeQuery("select TCID, finished from ASM_tc_iter where AID='"
						+ mAID + "' and tcnum='" + tc + "'");
		if (rs.first())
		{
			started = true;
			TCID = rs.getInt("TCID");
			finished = rs.getInt("finished");
		} 
		else
		{
			mDB2.executeUpdate("insert into ASM_tc_iter (AID,tcnum,tctype,ctgs_start) values(" + mAID
					+ "," + tc + ",'" + IterType.TC.toString() + "'," + mID2Contig.numKeys() + ")");
			TCID = mDB2.lastID();
		}
		rs.close();
		String tcstr = "TC" + tc;
		if (finished == 1)
		{
			return;
		}
		Log.head("Contig merge round #" + tc + " (" + tcstr + ")",LogLevel.Basic);
		
		File tcdir = new File(mTopDir, tcstr);
		Utils.checkCreateDir(tcdir);
		if (!started)
		{
			Utils.clearDir(tcdir);
		}

		// Next write out the contig sequence list and do the self alignment, if
		// not already done
		File outFile = 	doTCSelfAlign(tcdir, tc);
		
		// load the edges we've already tried and failed so we don't retry them
		Utils.termOut("Loading previously-tried edges");
				
		SparseMatrix triedEdges = new SparseMatrix(mID2Contig.numKeys(),10);
		
		rs = mDB.executeQuery("select scid1,scid2,errstr from ASM_tc_edge join ASM_tc_iter on ASM_tc_edge.tcid=ASM_tc_iter.tcid where aid=" + mAID + " and attempted=1 and succeeded=0 ");
		int count = 0;
		while (rs.next())
		{
			int scid1 = rs.getInt("scid1");
			int scid2 = rs.getInt("scid2");
			String errStr = rs.getString("errstr");
			
			if (tc == maxtc && errStr.equals(OKStatus.MixedStrictMerge.toString())) continue; // last TC, we will allow retry of edges that failed on this condition

			if (!mID2Contig.containsKey(scid1) || !mID2Contig.containsKey(scid2)) continue;
			
			int idx1 = mID2Contig.get(scid1).mIdx;
			int idx2 = mID2Contig.get(scid2).mIdx;
			
			if (idx1 < 0 || idx2 < 0)
			{
				throw(new Exception("Un-indexed contig!"));
			}
			
			triedEdges.set(idx1, idx2);
			count++;
		}
		triedEdges.sortRows();
		if (count > 0)
		{
			Log.msg(count + " previously-tried edges loaded", LogLevel.Detail);
		}
		
		LoadTCEdges(TCID, tc, tcstr, outFile,tcdir, triedEdges);

		File capTopDir = new File(tcdir,"cap");
		Utils.checkCreateDir(capTopDir);

		initMerges(mMergesDone);
		
		int iteration = 0;
		rs = mDB.executeQuery("select clustiter_done from ASM_tc_iter where tcid=" + TCID);
		if (rs.first())
		{
			iteration = rs.getInt("clustiter_done");
		}
				
		Utils.recordTime("TC" + tc + " begin assembly",mAID,mDB);
		
		rs = mDB.executeQuery("select count(*) as count from ASM_tc_edge where attempted=0 and tcid=" + TCID);
		rs.first();
		
		Log.indentMsg("Begin " + tcstr + " merges",LogLevel.Basic);
		long tstart = TimeHelpers.getTime();
		
		// In order to restrict the size of individual clusters, some edges have to be passed over and left for later,
		// so we just keep doing iterations until there are no more untried edges left in the DB.
		while (true)
		{
			renewConnections();
			loadContigs(true,false, false); 
			renewConnections();
			getTCEdgesFromDB(TCID, triedEdges, iteration,(tc == maxtc));
			Utils.memCheck();
			if (mEdges.size() == 0) break;
			Log.msg("cluster " + mEdges.size() + " edges", LogLevel.Detail);
			
			mClusters.clear();
			mClusters.setSize(1000);

			buildClusters(TCID,mEdges, null);
			iteration++;
			Log.msg("TC" + tc + " iteration " + iteration + " : " + mClusters.size() + " clusters", LogLevel.Detail);

			Utils.singleLineMsg("assembling " + mClusters.size() + " clusters");
			assembleClusters(capTopDir, TCID, tc, tcstr, maxtc);
			mDB.executeUpdate("update ASM_tc_iter set clustiter_done=" + iteration + " where tcid=" + TCID);
		}
		triedEdges.clear();
		triedEdges = null;
		
		if (!Utils.isDebug())
		{
			// If not debug, get rid of merged contigs.
			// This will automatically delete the edges for them also, by cascading.
			Log.msg("Removing merged contigs",LogLevel.Detail);
			
			rs = mDB.executeQuery("select contig.ctgid from ASM_scontig join contig on contig.sctgid=ASM_scontig.scid " +
					" where ASM_scontig.merged_to != ASM_scontig.scid and ASM_scontig.aid=" + mAID);
			PreparedStatement ps1 = mDB.prepareStatement("delete from contig where ctgid=?");
			int numRemoved = 0;
			while (rs.next())
			{
				int ctgid = rs.getInt("contig.ctgid");
				ps1.setInt(1,ctgid);
				ps1.addBatch();
				numRemoved++;
			}
			rs.close();
			ps1.executeBatch(); ps1.close();

			// do a separate query for the ASM_scontigs since there can be two contigs for each and we don't want to remove it twice
			rs = mDB.executeQuery("select ASM_scontig.scid from ASM_scontig where ASM_scontig.merged_to != ASM_scontig.scid and ASM_scontig.aid=" + mAID);
			PreparedStatement ps2 = mDB.prepareStatement("delete from ASM_scontig where scid=?");
			while (rs.next())
			{
				int scid = rs.getInt("ASM_scontig.scid");
				ps2.setInt(1,scid);
				ps2.addBatch();
			}
			rs.close();
			ps2.executeBatch(); ps2.close();
			
			Log.msg(numRemoved + " removed",LogLevel.Detail);
		}
				
		if  (Utils.isDebug())
		{
			checkContigIntegrity(false);
		}
		
		mDB.executeUpdate("update ASM_tc_iter set finished=1,ctgs_end=(select count(*) from ASM_scontig where scid=merged_to and aid=" + mAID + ") where TCID=" + TCID);

		mClusters.clear();
		
		if (!Utils.isDebug())
		{
			Utils.deleteDir(capTopDir);
			Utils.deleteDir(tcdir);	
		}
		Utils.recordTime("TC" + tc + " finished",mAID,mDB);
		String timeMsg = TimeHelpers.getElapsedTimeStr(tstart);
		Log.indentMsg(tcstr + " merges finished: elapsed time " + timeMsg,LogLevel.Basic);
		rs = mDB.executeQuery("select merges_tried,merges_ok,ctgs_end from ASM_tc_iter where tcid=" + TCID);
		rs.first();
		int tried = rs.getInt("merges_tried");
		int ok = rs.getInt("merges_ok");
		int ctgsend = rs.getInt("ctgs_end");
		Log.indentMsg(tried + " merges tried, " + ok + " successful, " + ctgsend + " final contigs",LogLevel.Basic);
		
	}	

	void initMerges(ID2Obj<Integer> mergesDone) throws Exception
	{
		ResultSet rs = null;
		rs = mDB.executeQuery("select min(ASM_scontig.SCID) as mins, max(ASM_scontig.SCID) as maxs from ASM_scontig where AID=" + mAID );
		rs.first();
		mergesDone.clear();
		mergesDone.setMinMax(rs.getInt("mins"),rs.getInt("maxs"));
		
		rs = mDB.executeQuery("select ASM_scontig.SCID, ASM_scontig.merged_to from ASM_scontig where AID='" + mAID + "' and ASM_scontig.merged_to != ASM_scontig.SCID ");
		while (rs.next())
		{
			mergesDone.put(rs.getInt("SCID"),rs.getInt("merged_to"));
		}
		rs.close();
		if (mergesDone.numKeys() > 0) 
			Log.msg( mergesDone.numKeys() + " previous contig merges loaded",LogLevel.Detail);
	}	

	// Get the next block of up to maxEdges untried edges from the database (limited, to cap memory use)
	// Update them for previous contig merges and see if they are still valid edges. 	
	private boolean getTCEdgesFromDB(int TCID, SparseMatrix triedEdges, int iteration, boolean lastTC ) throws Exception
	{
		boolean started = false;
		int maxEdges = 100000;
		
		Log.msg("Load edges from database",LogLevel.Detail);
		
		mEdges.clear();

		initMerges(mMergesDone);

		ResultSet rs = null; 

		rs = mDB.executeQuery("select count(*) as count from ASM_tc_edge where TCID=" + TCID + " and attempted=1" );
		rs.first();
		int nDone = rs.getInt("count");
		if (nDone > 0) 
		{
			Log.msg( nDone + " edges already analyzed ",LogLevel.Detail);
			started = true;
		}
		rs.close();
		
		rs = mDB.executeQuery("select count(*) as count from ASM_tc_edge where TCID=" + TCID + " and attempted=0" );
		rs.first();
		int nToLoad = rs.getInt("count");
		Log.msg("Scanning " + nToLoad + " edges, and updating for prior merges",LogLevel.Detail);
		
		mEdges.setSize(Math.min(maxEdges,nToLoad));
		
		// Note, get more than maxEdges since we'll reject some that are redundant
		rs = mDB.executeQuery("select EID,SCID1,SCID2,score,attempted from ASM_tc_edge where TCID=" + TCID + " and attempted=0 order by score desc limit " + 5*maxEdges );
		
		int nLoaded = 0;
		int alreadyTried = 0;
		int nScanned = 0;
		while (rs.next())
		{
			nScanned++;
			if (nScanned % 10000 == 0)
			{
				Log.msg(nScanned + " edges scanned, " + alreadyTried + " redundant, " + nLoaded + " loaded",LogLevel.Detail);
			}
			
			int eid = rs.getInt("EID");
			int score = rs.getInt("score");
			int scid1_orig = rs.getInt("SCID1");
			int scid2_orig = rs.getInt("SCID2");
			
			// follow the merge chain for these contigs and get the current contigs
			int scid1 = Edge.getMergedID(scid1_orig,mMergesDone);
			int scid2 = Edge.getMergedID(scid2_orig,mMergesDone);
			
			if (scid1 == scid2)
			{
				// The contigs have merged to the same contig, hence now redundant
				mDB2.executeUpdate("update ASM_tc_edge set attempted=1,errstr='redundant',errinfo='self' where eid=" + eid);
				continue;
			}
			
			boolean changed = (scid1 != scid1_orig || scid2 != scid2_orig);
			
			if (scid1 > scid2)
			{
				int tmp = scid1;
				scid1 = scid2;
				scid2 = tmp;
			}
			
			if (!mID2Contig.containsKey(scid1))
			{
				throw(new Exception("Merged contig " + scid1 + " not current??"));
			}	
			if (!mID2Contig.containsKey(scid2))
			{
				throw(new Exception("Merged contig " + scid2 + " not current??"));				
			}	

			// Is this edge already in the edge list? If so, it has already been tried, or it is already
			// in the queue. In either case, it is redundant. 
			// The one exception is if we're on the last TC, and the prior occurrence of the edge had a MixedStrictMerge failure. Then,
			// we will retry it. 

			ResultSet rs2 = mDB2.executeQuery("select eid, errstr from ASM_tc_edge join ASM_tc_iter on ASM_tc_edge.tcid=ASM_tc_iter.tcid where ASM_tc_iter.aid=" + mAID + " and scid1=" + scid1 + " and scid2=" + scid2 + " and eid !=" + eid);
			if (rs2.first())
			{
				int oldEID = rs2.getInt("eid");
				String errStr = rs2.getString("errstr");
				rs2.close();
				if (oldEID != eid)
				{
					if (!lastTC ||  !errStr.equals(OKStatus.MixedStrictMerge.toString()))
					{
						// it's redundant, and the last TC/MixedStrictMerge exception does not apply
						alreadyTried++;
						mDB2.executeUpdate("update ASM_tc_edge set errstr='made redundant',attempted=1 where eid=" + eid);
						continue;
					}
				}
			}
			rs2.close();
			
			Contig c1 = mID2Contig.get(scid1);
			Contig c2 = mID2Contig.get(scid2);
			
			Edge edge = new Edge(c1,c2,score,this, TCID);
			if (changed)
			{
				mDB2.executeUpdate("update ASM_tc_edge set attempted=1,succeeded=0,errstr='merged' where eid=" + eid);
				continue;
			}
			edge.mID = eid;
			
			if (edge.mC1.mID > edge.mC2.mID)
			{
				throw(new Exception("edge in wrong order!!"));
			}
			
			mEdges.add(edge);
			if (mEdges.size() == maxEdges)
			{
				Log.msg("Loaded " + maxEdges + " edges; going to assembly",LogLevel.Detail);	
				break;
			}
			nLoaded++;
		}

		rs.close();
		
		Log.msg("iteration " + iteration + ": " + mEdges.size() + " non-redundant edges loaded from database",LogLevel.Detail);
		Log.msg(alreadyTried + " had already been tried",LogLevel.Detail);
		return started;
	}

	// Get the threads going to assemble the current batch of clusters
	private void assembleClusters(File capTopDir, int TCID, int tcnum, String tcstr, int maxtc) throws Exception
	{
		int nThreads = Integer.parseInt(mProps.getProperty("CPUs"));

		ClusterThread[] threadList = new ClusterThread[nThreads];
		for (int i = 0; i < nThreads; i++)
		{
			threadList[i] = new ClusterThread(this, capTopDir,TCID,tcnum, i, tcstr, maxtc);
		}

		mThreadWorkList = new int[mClusters.size()];
		int i = 0;
		
		i = 0;
		for (int ic = 0; ic < mClusters.size(); ic++)
		{
			Cluster clust = mClusters.get(ic);
			if (clust == null) 
			{
				throw(new Exception("null cluster!!"));	
			}
			mThreadWorkList[i] = ic + 1; // b/c of how getNextThreadWorkItem is set up
			i++;			
		}
		
		mThreadWorkItem = 0;
		mThreadsFinished = 0;

		int[] ws = {7,12,50,30,20,10,10,10};
		Log.columns(ws, LogLevel.Detail,"ThreadNum","ClustID","Edge","Result","New Ctg","#CapBury","EdgeID","moreInfo");

		for (i = 0; i < nThreads; i++)
		{
			threadList[i].start();
		}
				
		int j = 0;
		while (mThreadsFinished < nThreads)
		{
			j++;
			if (j % 6 == 0)
			{
				int left = threadItemsLeft();
				for (ClusterThread th : threadList)
				{
					if (th == null || th.mClust == null) continue;
					int numLeft = th.mClust.assemblyProgress();
					if (j % 60 == 0)
					{
						Log.msg("Thread " + th.mNum + " Cluster" + th.mCLUSTID + " " + numLeft + " left",LogLevel.Detail);
					}
				}
				Log.msg(left + " clusters still on queue",LogLevel.Detail);
				
				int count = mDB.executeCount("select count(*) as count from ASM_tc_edge where attempted=0 and tcid=" + TCID);
				Utils.singleLineMsg("trying merges: " + count + " remaining");
			}
			Thread.sleep(10000);			
		}
		mThreadWorkList = null;

		for (i = 0; i < nThreads; i++)
		{
			threadList[i] = null;
		}
		threadList = null;
	}

	// A thread for cluster assembly.
	class ClusterThread extends Thread
	{
		AssemMain mAssem;
		File mCapTopDir;
		int mNum;
		int mTCID;
		int mTCNUM;
		String mTCSTR;
		boolean mStrict;
		int mCLUSTID;
		Cluster mClust = null;
		DBConn mLocalDB = null;
		private ClusterThread(AssemMain assem, File capTopDir, int TCID, int tcnum, int num, String tcstr, int maxtc) throws Exception
		{
			mAssem = assem;
			mCapTopDir = capTopDir;
			mNum = num;
			mTCID = TCID;
			mTCNUM = tcnum;
			mTCSTR = tcstr;
			mStrict = (tcnum < maxtc);
			mLocalDB = mAssem.getDBConnection();
		}
		public void run()
		{
			// Loop, getting the next cluster and starting it
			try
			{
				int clustIdx = 0;
				while ( (clustIdx = mAssem.getNextThreadWorkItem()) > 0)
				{					
					mLocalDB.renew();
					mClust = mAssem.mClusters.get(clustIdx - 1);					
					mClust.setDB(mLocalDB);
					mClust.mThreadNum = mNum;
					mClust.doAssembly(mTCID, mCapTopDir,mTCSTR,mTCNUM,mStrict);
					mClust.clear();	
					mClust = null;
				}
				mAssem.threadDone(mNum);
				mLocalDB.close();
			}
			catch (Exception e)
			{
				Log.msg("THREAD EXCEPTION: " + mNum + " edge:" + e.toString(),LogLevel.Detail);
				Log.exception(e);
				Log.msg("A fatal error has occurred; more information may be available in the detail log.",LogLevel.Basic);
				Log.msg(e.getMessage(),LogLevel.Basic);
				mAssem.threadDone(mNum);
				try
				{
					mLocalDB.close();
				}
				catch(Exception e1)
				{
					System.exit(0);
				}
				System.exit(0);
			}			
		}
	}
	// Thread for clique assembly.
	class CliqueThread extends Thread 
	{
		AssemMain mAssem;
		File mCapTopDir;
		int mNum;
		int mTCID;
		DBConn mLocalDB;
		private CliqueThread(AssemMain assem, File capTopDir, int TCID, int num) throws Exception
		{
			mAssem = assem;
			mCapTopDir = capTopDir;
			mNum = num;
			mTCID = TCID;
			mLocalDB = assem.getDBConnection();
		}
		public void run() 
		{
			Log.msg("Start clique assembly thread " + mNum,LogLevel.Detail);
			try
			{
				int CQID;
				while ( (CQID = mAssem.getNextThreadWorkItem()) > 0)
				{			
					mLocalDB.renew();
					ResultSet rs = null;
					
					rs = mLocalDB.executeQuery("select nclone,type, assembled from ASM_cliques where CQID=" + CQID );
					rs.next();
					String errstr = "";
	
					int nclone = rs.getInt("nclone");
					int assembled = rs.getInt("assembled");
					if (assembled != 0)
					{
						throw(new Exception("clique " + CQID + " already assembled!"));
					}
					CliqueType ct = CliqueType.valueOf(rs.getString("type"));
					int capMidNum = CQID/1000;
					File capMidDir = new File(mCapTopDir,"" + capMidNum);
					File capDir = new File(capMidDir,"" + CQID);
					rs.close();
					
					Utils.checkCreateDir(capMidDir);
					Utils.checkCreateDir(capDir);
					Utils.clearDir(capDir);
					
					Vector<Integer> cids = new Vector<Integer>();
					ResultSet rs2 = mLocalDB.executeQuery("select CID from ASM_clique_clone where CQID='"
									+ CQID + "'");
					while (rs2.next())
					{
						cids.add(rs2.getInt("CID"));
					}
					rs2.close();
					if (cids.size() != nclone)
					{
						throw (new Exception("clique " + CQID + " has " + cids.size()
								+ " reads but should have " + nclone));
					}
					mAssem.assembleClones(capDir,cids.toArray(new Integer[cids.size()]),"CAP_ARGS",mNum,mLocalDB);
										
					File recapDir = new File(capDir,"recap");
					Utils.checkCreateDir(recapDir);
					Utils.clearDir(recapDir);
					
					String src = "clique " + CQID;
					Log.msg(src,LogLevel.Dbg);
					AceFile aceFile = new AceFile(mAID,mIDStr, new File(capDir,"seqs.fasta.cap.ace"),  mID2Clone,mName2Clone, src,mLocalDB);
					aceFile.doParse(false,0);
					aceFile.mThisID = CQID;
					if (aceFile.mOK == OKStatus.CapFailure || aceFile.mContigs.size() == 0)
					{
						Log.msg("No contigs formed from clique " + CQID,LogLevel.Detail);
						mLocalDB.executeUpdate("update ASM_cliques set assembled=1,cap_success=0 where CQID=" + CQID);
						continue;
					}
					SubContig[] ctgs = new SubContig[aceFile.mContigs.size()];
					for (int i = 0; i < ctgs.length; i++)
					{
						ctgs[i] = aceFile.mContigs.get(i);
					}
					Arrays.sort(ctgs, new SubContig.CapContigSizeCmp());
					int nGood = 0;
					
					if (ct == CliqueType.Paired)
					{
						// For each contig we will find its best match, which may be
						// itself, and try to make it a perfect match by discarding non-matching clones.
						// If we succeed, we will upload it. 
						// We'll only use pairs that are mutually best matches.
						//Log.msg("Clique " + CQID + " " + ctgs.length + " contigs, start pairings",LogLevel.Dbg);
						for (SubContig ctg : ctgs) 
						{
							ctg.mMateNum = -1;
							ctg.mDoUpload = false;
						}
						for (int i = 0; i < ctgs.length; i++)
						{
							SubContig ctg1 = ctgs[i];
							int maxScore = 0;
							int bestNum = -1;
							for (int j = 0 ; j < ctgs.length; j++)
							{
								SubContig ctg2 = ctgs[j]; 
								int score = ctg1.countPaired(ctg2);
								if (score > maxScore)
								{
									maxScore = score;
									bestNum = j;
								}
							}
							if (maxScore > 0)
							{
								ctg1.mMateNum = bestNum;
							}
							else
							{
								throw(new Exception("No matching contig in " + CQID + "!"));	
							}
						}
						
						TreeSet<Integer> done = new TreeSet<Integer>();
						for (int i = 0; i < ctgs.length; i++)
						{
							if (done.contains(i)) continue;
							done.add(i);
							SubContig ctg = ctgs[i];
							if (ctg.mMateNum == i) // mated to itself
							{
								if (ctg.checkFixSelfPaired())
								{
									ctg.mDoUpload = true;
								} 
								else
								{
									// There were some discards. Is there enough left to
									// bother rebuilding?
									if (ctg.mPairsIdx.size() < Integer.parseInt(mProps.getProperty("CLIQUE_SIZE_PAIR")))
									{
										errstr = "Pairing";
									}
									else
									{
										ReassembleContigs(recapDir, ctg);
										AceFile aceFile2 = new AceFile(mAID,mIDStr, new File(recapDir,"seqs.fasta.cap.ace"), mID2Clone,mName2Clone, src,mLocalDB);
										aceFile2.doParse(false,0);
										// Now we're demanding one, exactly matched contigs
										if (aceFile2.mContigs.size() == 1)
										{
											ctg = aceFile2.mContigs.get(0);
											ctg.mMateNum = i;
											ctgs[i] = ctg;
											if (ctg.checkFixSelfPaired())
											{
												ctg.mDoUpload = true;
											}
										}
										aceFile2 = null;
									}
		
								}							
							}
							else
							{
								SubContig ctg2 = ctgs[ctg.mMateNum];
								done.add(ctg.mMateNum);
								if (ctg2.mMateNum == i)
								{
									// only use pairs which are mutually best
									if (ctg.checkFixPaired(ctgs[ctg.mMateNum]))
									{
										ctg.mDoUpload = true;								
									} 
									else
									{
										// There were some discards. Is there enough left to
										// bother rebuilding?
										if (ctg.mPairsIdx.size() < Integer.parseInt(mProps.getProperty("CLIQUE_SIZE_PAIR")))
										{
											errstr="Pairing";
										}
										else
										{
											ReassembleContigs(recapDir, ctg, ctg2);
											AceFile aceFile2 = new AceFile(mAID,mIDStr, new File(recapDir,"seqs.fasta.cap.ace"), mID2Clone,mName2Clone, src,mLocalDB);
											aceFile2.doParse(false,0);
											// Now we're demanding two, exactly matched contigs
											if (aceFile2.mContigs.size() == 2)
											{
												int mateNum = ctg.mMateNum;
												if (mateNum == -1)
												{
													throw(new Exception("clique " + CQID + " idx " + i + " no mate!!"));
												}
												ctgs[i] = aceFile2.mContigs.get(0);
												ctgs[i].mMateNum = mateNum;
												ctgs[mateNum] = aceFile2.mContigs.get(1);
												ctgs[mateNum].mMateNum = i;
												if (ctgs[i].checkFixPaired(ctgs[mateNum]))
												{
													// Contigs match without change - good
													ctgs[i].mDoUpload = true;
												}
											}
											aceFile2 = null;
										}
									}
								}
							}
						}
						
						mAssem.doPairedCliqueUpload(CQID, ctgs, mTCID,mLocalDB,ct);
						for (int i = 0; i < ctgs.length; i++)
						{
							ctgs[i].clear();
						}
						aceFile.mOK = OKStatus.CapFailure; // makes it clear completely
						aceFile.clear();
						aceFile = null;
					} 		
					else if (ct == CliqueType.Self)
					{
						// If the overlapping mate pairs went together, good,
						// otherwise reject
						if (aceFile.mContigs.size() == 1)
						{
							SubContig ctg1 = ctgs[0];							
							if (ctg1.checkSelfSinglePaired())
							{
								mAssem.doSelfPairedCliqueUpload(aceFile, mTCID,mLocalDB,ct);
								nGood = 1;
							}
						}
						if (nGood != 1)
						{
							errstr = "Self olap fail";
						}
						aceFile.mOK = OKStatus.CapFailure;
						aceFile.clear();
						aceFile = null;
					} 
					else if (ct == CliqueType.Single)
					{
						// Here we may as well just keep all the contigs.
						
						mAssem.doUnpairedCliqueUpload(aceFile, mTCID,mLocalDB,ct);
						nGood = aceFile.mContigs.size();
						aceFile.mOK = OKStatus.CapFailure;
						aceFile.clear();
						aceFile = null;
					}
						
					if (nGood > 0)
					{
						mLocalDB.executeUpdate("update ASM_cliques set cap_success=1 where CQID=" + CQID);
					}
					else
					{
						mLocalDB.executeUpdate("update ASM_cliques set assembled=1 where CQID=" + CQID);
					}
					Log.columns(15, LogLevel.Detail, mNum, ct.toString(),nclone,nGood,CQID,errstr);
					aceFile =  null;
	
					rs.close();
					
					if (!Utils.isDebug())
					{
						Utils.deleteDir(capDir);
					}
				}
				
				mAssem.threadDone(mNum);
				mLocalDB.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.msg("THREAD EXCEPTION: " + mNum + " edge:" + e.toString(),LogLevel.Detail);
				Log.exception(e);
				Log.msg("A fatal error has occurred",LogLevel.Basic);
				Log.msg(e.getMessage(),LogLevel.Basic);
				mAssem.threadDone(mNum);
				try
				{
					mLocalDB.close();
				}
				catch(Exception e1)
				{
					System.exit(0);
				}
				System.exit(0);
			}
		}
		private void ReassembleContigs(File capDir,SubContig... ctgs) throws Exception
		{
			TreeSet<Integer> IDs = new TreeSet<Integer>();
			for (SubContig ctg : ctgs)
			{
				IDs.addAll(ctg.mAllClonesID);
			}
			assembleClones(capDir,IDs.toArray(new Integer[0]),"CAP_ARGS",mNum,mLocalDB);
		}
		
	}
	// Thread to periodically print memory use to log.
	class memCheckThread extends Thread
	{
		private memCheckThread()
		{
			
		}
		public void run() 
		{
			while (true)
			{
				Utils.memCheck();
				try
				{
					Thread.sleep(600000);
				}
				catch (Exception e)
				{
					
				}
			}
		}
	}
	
	// Thread for doing the clique cap buries.
	class CapBuryThread extends Thread 
	{
		AssemMain mAssem;
		File mCapTopDir;
		int mNum;
		int mTCID;
		DBConn mLocalDB;
		private CapBuryThread(AssemMain assem, File capTopDir, int num,int TCID) throws Exception
		{
			mAssem = assem;
			mCapTopDir = capTopDir;
			mNum = num;
			mLocalDB = assem.getDBConnection();
			mTCID = TCID;
		}
		public void run() 
		{
			Log.msg("Start cap bury thread " + mNum,LogLevel.Detail);
			try
			{
				int CTGID;
				while ( (CTGID = mAssem.getNextThreadWorkItem()) > 0)
				{		
					mLocalDB.renew();
					Contig ctg = mID2Contig.get(CTGID);
					if (!ctg.mCapBuryDone)
					{
						mAssem.capBuryContig(mCapTopDir,ctg,"CAP_ARGS",mLocalDB,mNum,mTCID);
						ctg.setCapBuryDone(true,mLocalDB);
					}
				}
				mAssem.threadDone(mNum);
				mLocalDB.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.msg("THREAD EXCEPTION: " + mNum + " edge:" + e.toString(),LogLevel.Detail);
				Log.exception(e);
				Log.msg("A fatal error has occurred",LogLevel.Basic);
				Log.msg(e.getMessage(),LogLevel.Basic);
				mAssem.threadDone(mNum);
				try
				{
					mLocalDB.close();
				}
				catch(Exception e1)
				{
					System.exit(0);
				}
				System.exit(0);
			}
		}
	}
	
	// Thread for doing the parallelized megablasts.
	class BlastThread extends Thread 
	{
		AssemMain mAssem;
		File mBlastDir;
		int mNum;
		String mCmd;
		String mQueryFileName;
		String mOutFileName;
		private BlastThread(AssemMain assem, File blastDir, String cmdLine, String queryFileName, String outFileName, int num) throws Exception
		{
			mAssem = assem;
			mBlastDir = blastDir;
			mNum = num;
			mCmd = cmdLine;
			mQueryFileName = queryFileName;
			mOutFileName = outFileName;
		}
		public void run() 
		{
			Log.msg("Start blast thread " + mNum,LogLevel.Detail);

			File qFile = new File(mBlastDir,mQueryFileName);
			if (!qFile.exists() || qFile.length() == 0) return;
			File outFile = new File(mBlastDir,mOutFileName);
			outFile.delete();
			String cmd3 = mCmd + BlastArgs.getAsmInOutParams(qFile.getAbsolutePath(),outFile.getAbsolutePath());
			try
			{
				int ev = Utils.runCommand(BlastArgs.cmdLineToArray(cmd3), mBlastDir, false, true, null,0);
				if (ev != 0)
				{
					Log.msg("WARNING: Blast thread " + mNum + " exited with value " + ev, LogLevel.Basic);
				}
			}
			catch (Exception e)
			{
				Log.die("Failed to run blast:" + cmd3);	
			}
			mAssem.threadDone(mNum);
		}
	}	
	
	// Thread for finalizing the contigs.
	// May be ineffective since much of the time goes to DB operations (collecting buried).
	class FinalizeThread extends Thread 
	{
		AssemMain mAssem;
		int mNum;
		int mTCID;
		int mStart;
		DBConn mLocalDB;
		private FinalizeThread(AssemMain assem, int num, int TCID, int start) throws Exception
		{
			mAssem = assem;
			mNum = num;
			mLocalDB = assem.getDBConnection();
			mTCID = TCID;
			mStart = start;
		}
		public void run() 
		{
			Log.msg("Start finalize thread " + mNum,LogLevel.Detail);
			try
			{
				int idx;
				while ( (idx = mAssem.getNextThreadWorkItem()) > 0)
				{		
					mLocalDB.renew();
					Contig ctg = mCtgsBySize[idx-1];	
					if (ctg == null) continue;
					finalizeContig(ctg,idx + mStart - 1,mTCID,mNum,mLocalDB);
				}
				mAssem.threadDone(mNum);
				mLocalDB.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.msg("THREAD EXCEPTION: " + mNum + " edge:" + e.toString(),LogLevel.Detail);
				Log.exception(e);
				Log.msg("A fatal error has occurred",LogLevel.Basic);
				mAssem.threadDone(mNum);
				try
				{
					mLocalDB.close();
				}
				catch(Exception e1){}
				System.exit(0);
			}
		}
	}		

	// Clear out and re-download the current contigs and subcontigs (i.e., those which have not been merged to others).
	// Also get all the clone positions.
	// This is called a lot to initialize various stages, and protect against a restart.
	// It is getting and setting more fields than necessary.
	// WMN: added the noFinal at the last minute to keep it from re-doing finalized contigs
	// 		on a restart. The code it controls could really be executed every time but I
	//		put it within if blocks to make sure it can only affect the finalizeContigs function.
	void loadContigs(boolean unmergedOnly, boolean loadSeqQual, boolean noFinal) throws Exception
	{
		Log.msg("Load Contigs:  unmergedOnly " + unmergedOnly + "  loadSeqQual " + loadSeqQual +
				"  noFinal " + noFinal, LogLevel.Detail);
		
		// clear out all the old refs that hold contig pointers
		mID2SubContig.clear();
		mID2Contig.clear();
		
		for (int i = mID2Clone.minKey(); i <= mID2Clone.maxKey(); i++)
		{
			if (!mID2Clone.containsKey(i)) continue;
			Clone c = mID2Clone.get(i);
			c.mSCID = 0;
			c.mSC = null;
		}
		for (int i = 0; i < mEdges.size(); i++)
		{
			mEdges.get(i).mC1 = null;	
			mEdges.get(i).mC2 = null;	
		}
		mEdges.clear();
				
		ResultSet rs = null;
		String query = "select min(CTGID) as minid, max(CTGID) as maxid from contig " +
				" join ASM_scontig on ASM_scontig.scid=contig.sctgid " +
				" where contig.AID=" + mAID + " ";
		if (unmergedOnly) query += 	" and ASM_scontig.merged_to = ASM_scontig.scid ";
		rs = mDB.executeQuery(query);
		rs.first();

		mID2SubContig.setMinMax(rs.getInt("minid"),rs.getInt("maxid"));
		rs.close();
		
		int total_ctg = 0, total_subctg = 0;
		
		query = "select CTGID, contigid, mate_CTGID, numclones, rftype, SCTGID, consensus_bases  ";
		if (loadSeqQual) query += ",consensus,quality ";
		query +=		" from contig join ASM_scontig on ASM_scontig.scid=contig.sctgid where contig.AID=" + mAID + " " ;
		if (unmergedOnly) query += " and ASM_scontig.merged_to = ASM_scontig.scid ";
		if (noFinal) query += " and contig.finalized = 0 "; // WMN last-minute fix; it was re-doing already finalized contigs on restart
		query +=  " order by CTGID asc";
		
		rs = mDB.executeQuery(query);
		while (rs.next())
		{
			SubContig ctg = new SubContig(mID2Clone,mAID, mIDStr);
			int CTGID = rs.getInt(1);
			int SCTGID = rs.getInt(6);
			mID2SubContig.put(CTGID, ctg);
			ctg.mID = CTGID;
			ctg.mCTGID = SCTGID;
			ctg.mCCSLen = rs.getInt(7);
			if (SCTGID == 0) 
				throw(new Exception("subcontig " + CTGID + " has no containing contig"));				
			ctg.mMateID = rs.getInt(3);
			if (loadSeqQual)
			{
				ctg.mCCS = rs.getString("consensus");
				ctg.mQual = rs.getString("quality");
			}
			ctg.mRF = RF.valueOf(rs.getString(5));
			ctg.mIDStr = rs.getString(2);
			total_subctg++;
		}
		rs.close();
		for (int id = mID2SubContig.minKey(); id <= mID2SubContig.maxKey(); id++)
		{
			if (!mID2SubContig.containsKey(id)) continue;
			SubContig ctg1 = mID2SubContig.get(id);
			if (ctg1.mMateID > 0)
			{
				ctg1.mMate = mID2SubContig.get(ctg1.mMateID);
			}
		}
		if (mID2SubContig.numKeys() == 0)
		{
			Log.msg("There are no contigs yet");
			return;
		}
		rs = mDB.executeQuery("select min(SCID) as mins, max(SCID) as maxs " +
				" from ASM_scontig  where ASM_scontig.AID=" + mAID );
		if (unmergedOnly) query +=  " and merged_to = SCID " ;
		rs.first();
		mID2Contig.setMinMax(rs.getInt("mins"), rs.getInt("maxs"));
		rs.close();
		
		query = "select SCID,CTID1,CTID2,merged_to,capbury_done,TCID from ASM_scontig  " +
				"where ASM_scontig.AID=" + mAID ;
		if (unmergedOnly) query +=  " and merged_to = SCID ";
		query += " order by SCID asc" ;
		rs = mDB.executeQuery(query);
		while (rs.next())
		{
			int id = rs.getInt(1);
			int cid1 = rs.getInt(2);
			int cid2 = rs.getInt(3);
			int tcid = rs.getInt(6);
			int merged_to = rs.getInt(4);
			if (noFinal) // WMN last-minute fix; minimize impact
			{
				if (!mID2SubContig.containsKey(cid1) || (cid2 > 0 && !mID2SubContig.containsKey(cid2)))
				{
					continue;	
				}
			}
			if (!mID2SubContig.containsKey(cid1)) { // FIXME: with debug on, crashes here
				System.err.println("Internal error1 in Assembly: you need to delete database and restart.");
				System.exit(-1);
			}
			if (cid2>0 && !mID2SubContig.containsKey(cid2)){
				System.err.println("Internal error2 in Assembly: you need to delete database and restart.");
				System.exit(-1);
			}
			
			Contig ctg = new Contig(mAID,mID2SubContig.get(cid1),(cid2 > 0 ? mID2SubContig.get(cid2) : null));
			ctg.mID = id;
			ctg.mTCID = tcid;
			ctg.mMergedTo = merged_to;
			ctg.mCapBuryDone = rs.getBoolean("capbury_done");
			mID2Contig.put(id,ctg);
			total_ctg++;
		}
		rs.close();

		rs = mDB.executeQuery("select CID, contclone.CTGID, leftpos, ngaps, contclone.orient from contclone " +
				" join contig on contclone.CTGID=contig.CTGID " +
				" join ASM_scontig on ASM_scontig.SCID=contig.SCTGID " +
				" where contig.AID=" + mAID + " and buried=0 and ASM_scontig.merged_to=ASM_scontig.SCID");
		while (rs.next())
		{
			int cid = rs.getInt(1);			
			int ngaps = rs.getInt(4);
			int ctgid = rs.getInt(2);
			if (noFinal) // WMN last-minute fix; minimize impact
			{
				if (!mID2SubContig.containsKey(ctgid) )
				{
					continue;	
				}
			}
			
			Clone c = mID2Clone.get(cid);
			c.mStart = rs.getInt(3);
			c.mEnd = c.mStart + c.mSeqLen + ngaps;
			c.mNGaps = ngaps;
			c.mSCID = ctgid;
			c.mSC = mID2SubContig.get(ctgid);
			c.mUC = UC.valueOf(rs.getString(5));
			mID2SubContig.get(ctgid).mAllClonesID.add(c.mID);
		}
		rs.close();
		
		// Index the contigs for uses (e.g. arrays) where we need an index
		// that goes consecutively from 0, unlike the database ID. 
		
		int idx = 0;
		for (int ctgid = mID2Contig.minKey(); ctgid <= mID2Contig.maxKey(); ctgid++)
		{
			if (!mID2Contig.containsKey(ctgid)) continue;
			mID2Contig.get(ctgid).mIdx = idx;
			idx++;
		}
		
		int npaired = total_subctg - total_ctg;
		Log.msg("Loaded " + total_subctg + " contigs, " + npaired + " paired",LogLevel.Detail);
	}
	
	
	// Given a set of edges, build the transitive closure contig sets. 
	private void transitiveClose(int TCID, AutoArray<Edge> edges, Contig[] ctg2use) throws Exception
	{
		// we first have to map the contig ids to start from 0
		TreeMap<Integer,Integer> id2idx = new TreeMap<Integer,Integer>();
				
		int numCtgs = 0;
		for (int ic = 0; ic < ctg2use.length; ic++)
		{
			Contig ctg = ctg2use[ic];
			if (ctg == null) break;
			numCtgs++;
			id2idx.put(ctg.mID,ic);
		}
		SparseMatrix tcGraph = new SparseMatrix(numCtgs,10);
		int nNodes = 0;
		
		// We need to know which contigs go to which edges
		SparseMatrix ctg2edges = new SparseMatrix(numCtgs,10);
		for (int ie = 0; ie < edges.size(); ie++)
		{
			Edge edge = edges.get(ie);
			if (!id2idx.containsKey(edge.mC1.mID) || !id2idx.containsKey(edge.mC2.mID)) 
			{
				continue;
			}
						
			tcGraph.setNode(id2idx.get(edge.mC1.mID), id2idx.get(edge.mC2.mID));
			
			// Add the edge to the list for the smaller idx, so we ultimately only add it once to the cluster
			int minIdx = Math.min(id2idx.get(edge.mC1.mID),id2idx.get(edge.mC2.mID));
			ctg2edges.set(minIdx,ie);
			
			nNodes++;
		}
		
		Log.msg("Start trans closure of " + nNodes + "...",LogLevel.Dbg);
		Vector<int[]> tcRes = tcGraph.transitiveClosure();

		// build the cluster objects with the subsets of the full edge set
		Cluster[] clusters = new Cluster[tcRes.size()];
		Log.msg("closure done, " + tcRes.size() + " clusters found",LogLevel.Dbg);
		int nClust = 0;
		TreeSet<Integer> edgesUsed = new TreeSet<Integer>();
		for (int[] idclust : tcRes)
		{
			if (idclust.length == 1) continue;
			Cluster cluster = new Cluster(TCID, mDB, this);
			for (int idx2 : idclust)
			{
				Contig c = ctg2use[idx2];
				cluster.mContigs.add(c);
				// Since it is a full transitive closure, this cluster should contain all the edges emanating
				// from this contig.
				int[] edgeRow = ctg2edges.getRow(idx2);
				for (int ie : edgeRow)
				{
					if (ie >= edges.size()) break;
					if (edgesUsed.contains(edges.get(ie).mID))
					{
						Utils.noop();	
					}
					edgesUsed.add(edges.get(ie).mID);
					cluster.mEdges.add(edges.get(ie));
				}	
			}
			Log.msg("Cluster " + nClust + ":" + cluster.mEdges.size() + " edges " + cluster.mContigs.size() + " contigs",LogLevel.Dbg);
			clusters[nClust] = cluster;
			nClust++;
		}
		tcRes.clear();
		tcRes = null;
		
		int nonTriv = 0;
		int k = mClusters.size() + 1;
		for (int i = 0; i < nClust; i++)
		{
			if (clusters[i].mEdges.size() == 0) continue;
			nonTriv++;
			clusters[i].mID = k;
			mClusters.add(clusters[i]);
			k++;
		}
		Log.msg("transitive closure: " + nonTriv + " non-trivial clusters",LogLevel.Dbg);
	}
	
	// Create chunks of N contigs to feed to the transitive closure routine. 
	// The idea is to go down through the edge list, in order of overlap strength, 
	// and put the contigs from each N edges into a chunk, if that edge doesn't already
	// intersect a previous chunk. If it does, it is skipped for now.
	// This way we can keep clusters from getting too big, while still
	// mostly doing the strongest edges first. 
	private void buildClusters(int TCID, AutoArray<Edge> edges, Set<Integer> ctg2use) throws Exception
	{
		int ctgsPerCluster = 1000;
		
		// Get the biggest contig idx so we can size arrays 
		int maxIdx = 0;
		for (int ie = 0; ie < mEdges.size(); ie++)
		{
			Edge e = mEdges.get(ie);
			maxIdx = Math.max(maxIdx,e.mC1.mIdx);
			maxIdx = Math.max(maxIdx,e.mC2.mIdx);
		}
		
		boolean[] usedCtgs = new boolean[maxIdx + 1];
		for (int idx = 0; idx <= maxIdx; idx++)
		{
			usedCtgs[idx] = false;	
		}
		Contig[] curCtgs = new Contig[ctgsPerCluster + 2];
		for (int ic = 0; ic < curCtgs.length; ic++)
		{
			curCtgs[ic] = null;	
		}
		
		int curIdx = 0;
		for (int ie = 0; ie < mEdges.size(); ie++)
		{
			Edge e = mEdges.get(ie);
			
			// See if it's in another set already.
			// But don't set this value until after b/c it will keep other edges with the same contigs from being added!
			if (e.mC1.mIdx == -1)
			{
				throw(new Exception("Unindexed edge contig " + e.mC1.idStr()));	
			}
			if (e.mC2.mIdx == -1)
			{
				throw(new Exception("Unindexed edge contig " + e.mC2.idStr()));	
			}
			
			if (usedCtgs[e.mC1.mIdx] || usedCtgs[e.mC2.mIdx]) continue; 
			curCtgs[curIdx] = e.mC1; curIdx++;
			curCtgs[curIdx] = e.mC2; curIdx++;
			if (curIdx >= ctgsPerCluster)
			{
				Log.msg("Clump: " + curIdx + " contigs",LogLevel.Dbg);
				transitiveClose(TCID,edges,curCtgs);
				for (int ic = 0; ic < curCtgs.length; ic++)
				{
					if (curCtgs[ic] != null)
					{
						usedCtgs[curCtgs[ic].mIdx] = true;
						curCtgs[ic] = null;
					}
				}
				
				curIdx = 0;
			}
		}
		if (curIdx > 0)
		{
			Log.msg("Clump: " + curIdx + " contigs",LogLevel.Dbg);
			transitiveClose(TCID,edges,curCtgs);

		}
		curCtgs = null;
		usedCtgs = null;
	}	

	// Set up the blast or vmatch for the TC
	private File doTCSelfAlign(File tcdir, int tc) throws Exception
	{
		String tcstr = "TC" + tc;
		String thisTag = tcstr + "_SELF";
		
		File outFile;
		if (mBlastType == BlastType.Vmatch)
		{
			outFile = new File(tcdir,"vmatch.out");
		}
		else
		{
			outFile = null;
		}
		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			//Log.msg(tcstr + " alignment done",LogLevel.Basic);
			return outFile;
		}
		Log.indentMsg("Begin " + tcstr + " self alignment",LogLevel.Basic);
		long tstart = TimeHelpers.getTime();
		
		recordTime(thisTag + " start");
		File ctgFile = new File(tcdir, "contigs.fasta");
		writeCurrentContigs(ctgFile);
		doTCBlast(tcdir, ctgFile, outFile, tc);
		
		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
		
		String timeMsg = TimeHelpers.getElapsedTimeStr(tstart);
		Log.indentMsg(tcstr + " alignment finished: elapsed time " + timeMsg,LogLevel.Basic);
		
		return outFile;
	}
	
	// Write all current (unmerged) contig consensus to file for blast
	private void writeCurrentContigs(File ctgFile) throws Exception
	{
		BufferedWriter ctgFileW = new BufferedWriter(new FileWriter(ctgFile));
		int maxID = 0;
		int numWritten = 0;
		int maxLen = Integer.parseInt(mProps.getProperty("MAX_MERGE_LEN"));
		String lenClause = "";
		if (maxLen > 0)
		{
			lenClause = " and consensus_bases <= " + maxLen + " ";
		}
		while (true)
		{
			ResultSet rs = mDB.executeQuery("select ctgid,consensus from contig join ASM_scontig on ASM_scontig.scid=contig.sctgid " +
					" where ASM_scontig.merged_to=ASM_scontig.scid and contig.aid=" + mAID + " and ctgid > " + maxID +  lenClause + " order by ctgid asc limit 1000");			
			int numThisTime = 0;
			while (rs.next())
			{
				int id = rs.getInt("ctgid");
				String seq = rs.getString("consensus");
				numWritten++;
				numThisTime++;
				ctgFileW.write(">CTG" + id + "\n" + seq.replace("*", "") + "\n");
				ctgFileW.flush();
				maxID = id;
			}
			if (numThisTime == 0) break;
		}
		Log.indentMsg(numWritten + " contigs written for self alignment",LogLevel.Basic);

		ctgFileW.close();
	}
	
	// Load the TC overlaps from the blast or vmatch output file, and into the DB
	private void LoadTCEdges(int TCID, int tc, String tcstr, File outFile,  File tcdir, SparseMatrix triedEdges ) throws Exception
	{
		String thisTag = tcstr + "_EDGES";
		if (Utils.checkUpToDate(thisTag, "", ""))
		{
			return;
		}
		Utils.termOut( "Loading edges from blast file"); 
		
		// load the overlap graph edges from the vmatch output
		mEdges.clear();
		mEdges.setSize(100000); // reasonable start
		readTCBlastEdges(tc, mEdges, TCID, tcdir, triedEdges);
		Utils.writeStatusFile(thisTag);
	}

	// read the contig overlap edges from the blast output file
	private void readTCBlastEdges(int tc, AutoArray<Edge> edges, int TCID, File tcdir,SparseMatrix triedEdges) throws Exception
	{		
		File[] blastFiles = new File[1000]; 
		for (int i = 0; i < blastFiles.length; i++)
		{
			blastFiles[i] = null;	
		}

		int nFound = 0;
		for (int p = 0; p < 1000; p++) // since they may have changed the thread number in the config file
		{	
			String fname = "tc_blast_result" + p;
			File f = new File(tcdir,fname);
			if (!f.exists())
			{
				break;	
			}
			blastFiles[nFound] = f;
			nFound++;
		}
		
		String tcstr = "TC" + tc;		
		String tcprop = mProps.getProperty(tcstr).trim();
		Pattern tcpat = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(\\d+)");
		Matcher m = tcpat.matcher(tcprop);
		if (!m.matches())
		{
			throw (new Exception("bad TC value:" + tcstr + "=" + tcprop));
		}
		
		int minmatch = Integer.parseInt(m.group(1));
		Float minid = Float.parseFloat(m.group(2));
		int maxhang = Integer.parseInt(m.group(3));
		
		String line;
		TreeMap<String,Integer> rejectCounts = new TreeMap<String,Integer>();
		rejectCounts.put("%id", 0);
		rejectCounts.put("olap", 0);
		rejectCounts.put("hang", 0);
		int nRead = 0;

		int nBad = 0;
		
		SparseMatrix pairsSeen = new SparseMatrix(mID2Contig.numKeys(),10); // needed to know if we're adding a new edge or augmenting the score of one
		pairsSeen.sortRows();

		int iNew = 0;
		int iMod = 0;
		PreparedStatement ps = mDB.prepareStatement("insert into ASM_tc_edge (TCID,SCID1,SCID2,score,errstr,errinfo) " +
				" values(" + TCID + ",?,?,?,'','')");
		PreparedStatement ps2 = mDB.prepareStatement("update ASM_tc_edge set score = score + ? where TCID=" + TCID + 
								" and SCID1=? and SCID2=? " );

		for (int i = 0; i < blastFiles.length; i++)
		{
			File f = blastFiles[i];
			if (f == null) continue;
			
			BufferedReader reader = new BufferedReader(new FileReader(f));
			
			while (reader.ready())
			{
				line = reader.readLine();
				if (line.startsWith("#")) continue;
				nRead++;
				if (nRead % 10000 == 0) { 
					Utils.singleLineMsg(	nRead + " lines");
				}
				String[] fields = line.trim().split("\\s+");
				if (fields.length != 12)
				{
					Log.msg("bad line in " + f.getAbsolutePath(),LogLevel.Basic);
					Log.msg("bad line in " + f.getAbsolutePath() + ":\n" + line,LogLevel.Detail);
					nBad++;
					if (nBad == 5)
					{
						Log.die(nBad + " bad lines seen: the blast output is unusable");	
					}
				}
				
				String name1 = fields[0];
				String name2 = fields[1].replace("lcl|",""); // megablast adds this annoying prefix to genbank seqs
							
				Float pctID = Float.parseFloat(fields[2]);		
				int alignLen = Integer.parseInt(fields[3]);
				int qStart = Integer.parseInt(fields[6]);
				int qEnd = Integer.parseInt(fields[7]);
				int tStart = Integer.parseInt(fields[8]);
				int tEnd = Integer.parseInt(fields[9]);
				Float score = Float.parseFloat(fields[11]);
									
				int id1 = Integer.parseInt(name1.replace("CTG", ""));
				int id2 = Integer.parseInt(name2.replace("CTG", ""));
	
				if (id1 >= id2) continue;			
	
				if (!mID2SubContig.containsKey(id1)) throw(new Exception("Subcontig not found " + id1));
				if (!mID2SubContig.containsKey(id2)) throw(new Exception("Subcontig not found " + id2));
	
				SubContig sc1 = mID2SubContig.get(id1);
				SubContig sc2 = mID2SubContig.get(id2);
	
				if (!mID2Contig.containsKey(sc1.mCTGID)) throw(new Exception("Subcontig has no contig " + id1));
				if (!mID2Contig.containsKey(sc2.mCTGID)) throw(new Exception("Subcontig has no contig " + id2));
	
				Contig c1 = mID2Contig.get(sc1.mCTGID);
				Contig c2 = mID2Contig.get(sc2.mCTGID);
				
				int len1 = sc1.mCCSLen;
				int len2 = sc2.mCCSLen;
				
				assert(len1 > 0 && len2 > 0);
				
				// If it's flipped, megablast swaps start/end.
				boolean flipped = ((qEnd - qStart)*(tEnd - tStart) < 0);
		
				// Now put the start/end are in increasing order.
				int tmp;
				if (qEnd  < qStart)
				{
					tmp = qEnd;
					qEnd = qStart;
					qStart = tmp;
				}
				if (tEnd  < tStart)
				{
					tmp = tEnd;
					tEnd = tStart;
					tStart = tmp;
				}
				
				// Overhangs: a valid overlap has to both start and end at or near 
				// the start/end of a sequence. It should not have a termination point
				// which is in the middle of both sequences, because this means
				// the alignment cannot be extended. 
				int hang1;
				int hang2;
				if (!flipped)
				{
					hang1 = Math.min(qStart-1, tStart-1);
					hang2 = Math.min(len1 - qEnd, len2 - tEnd);
				}
				else
				{
					// If it's flipped, this means the end of one aligns to the start of the other. 
					hang1 = Math.min(qStart-1, len2 - tEnd);
					hang2 = Math.min(len1 - qEnd, tStart-1);
				}
				assert(hang1 >= 0 && hang2 >= 0);
				int hang = Math.max(hang1,hang2);
				
				if (pctID < minid)
				{
					rejectCounts.put("%id", 1 + rejectCounts.get("%id"));
					continue;
				}
				if (alignLen < minmatch) 
				{
					rejectCounts.put("olap", 1 + rejectCounts.get("olap"));
					continue;
				}				
				if (hang > maxhang)
				{
					rejectCounts.put("hang", 1 + rejectCounts.get("hang"));
					continue;
				}
					
				int ctid1 = (c1.mID < c2.mID ? c1.mID : c2.mID);
				int ctid2 = (c1.mID < c2.mID ? c2.mID : c1.mID);

				int ctidx1 = mID2Contig.get(ctid1).mIdx;
				int ctidx2 = mID2Contig.get(ctid2).mIdx;
			
				if (ctidx1 < 0 || ctidx2 < 0)
				{
					throw(new Exception("Un-indexed contig!"));
				}
						
				if (triedEdges.isSet(ctidx1, ctidx2))
				{
					continue;
				}
							
				int iscore = Math.round(score);
				if (!pairsSeen.isSet(ctidx1, ctidx2))
				{
					pairsSeen.set(ctidx1, ctidx2);
					
					ps.setInt(1,ctid1);			
					ps.setInt(2,ctid2);			
					
					ps.setInt(3, iscore);	
					ps.addBatch();
					iNew++;
					if (iNew > 0 && iNew % 10000 == 0) ps.executeBatch();
					if (iNew > 0 && iNew % 100000 == 0)
					{
						Log.msg(iNew + " uploaded",LogLevel.Detail);
					}
				}
				else
				{
					ps2.setInt(2,ctid1);			
					ps2.setInt(3,ctid2);			
					
					ps2.setInt(1, iscore);	
					ps2.addBatch();
					iMod++;
					if (iMod > 0 && iMod % 10000 == 0) ps2.executeBatch();
				}
			}
		}
		ps.executeBatch(); ps.close();
		ps2.executeBatch(); ps2.close();
		
		pairsSeen.clear(); pairsSeen = null;
		
		Log.indentMsg(nRead + " blast output lines read",LogLevel.Basic);
		Log.indentMsg(iNew + " contig overlaps loaded",LogLevel.Basic);

		for (String reason : rejectCounts.keySet())
		{
			int count = rejectCounts.get(reason);
			Log.msg("TC Overlap Rejected:" + reason + ":" + count,LogLevel.Detail);
		}
	}	

	private void doTCBlast(File tcdir, File ctgFile, File outFile, int tc) 	throws Exception
	{
		String tcstr = "TC" + tc;
		String tcprop = mProps.getProperty(tcstr).trim();
		Pattern tcpat = Pattern.compile("(\\d+)\\s+(\\d+)\\s+(\\d+)");
		Matcher m = tcpat.matcher(tcprop);
		if (!m.matches())
		{
			Log.die("bad TC parameters:" + tcstr + "=" + tcprop);
		}
		
		Float pctID = Float.parseFloat(m.group(2));
		int minScore = calcBlastScore(Integer.parseInt(m.group(1))); // note, HIT score, not BIT score
				
		Log.msg("doing self-alignment of " + ctgFile.getName() + " in " + tcdir.getAbsolutePath(),LogLevel.Detail);
				
		String cmd2 = BlastArgs.getFormatn("contigs.fasta");
		Utils.termOut("Running " + cmd2); 

		Log.msg(cmd2,LogLevel.Detail);
		Utils.recordCmd(mAID,"TC" + tc + "Formatdb",cmd2,mDB);
		Utils.appendToFile(cmd2,tcdir,"cmd.list");
		Utils.runCommand(BlastArgs.cmdLineToArray(cmd2), tcdir, false, true, null,0);
		Utils.termOut("Running " + BlastArgs.getBlastnExec());

		String cmd = BlastArgs.getAsmMegablastArgs("contigs.fasta", mProps.getProperty("TC_BLAST_EVAL").trim(),
				pctID, minScore, mProps.getProperty("TC_BLAST_PARAMS").trim());

		Utils.recordCmd(mAID,"TC" + tc + "Blast",cmd,mDB);
		Log.msg(cmd,LogLevel.Detail);
		Utils.appendToFile(cmd,tcdir,"cmd.list");
		
		threadedBlast(cmd,"contigs.fasta","tc_blast_result",tcdir);
	}
	private int calcBlastScore(int in)
	{
		return (9*in)/10;	
	}
	// make the final, N-joined contigs, and do the clone alignments, calculate rstats
	private void finalizeContigs(int finalTC) throws Exception
	{
		int tcid = 0;
		ResultSet rs = mDB.executeQuery("select TCID, finished from ASM_tc_iter where AID='"
				+ mAID + "' and tcnum='" + finalTC + "'");
		if (rs.first())
		{
			int finished = rs.getInt("finished");
			tcid = rs.getInt("TCID");
			if (finished == 1)
			{
				Log.msg("already done",LogLevel.Basic);
				return;
			}
		} 
		rs.close();
		
		/*****************************************************
		 * 
		 */
		Log.head("Finalize contigs",LogLevel.Basic);
		Utils.recordTime("FinalizeStart",mAID,mDB);
		if (tcid == 0)
		{
			mDB.executeUpdate("insert into ASM_tc_iter (tcnum,finished,AID,tctype) values(" + finalTC + ",0," + mAID + ",'" + IterType.Final + "')");
			tcid = mDB.lastID();
		}

		rs = mDB.executeQuery("select erate, exrate from assembly where aid=" + mAID);
		rs.first();
		mERate = rs.getFloat("erate");
		mExRate = rs.getFloat("exrate");
		if (mProps.mUserKeys.contains("BASECALL_ERROR_RATE") || mERate == 0)
		{
			mERate = Float.parseFloat(mProps.getProperty("BASECALL_ERROR_RATE"));			
		}
		if (mERate <= 0 || mERate >= 1)
		{
			Log.indentMsg("Invalid basecall error rate:" + mERate, LogLevel.Basic);
			mERate = 0.001F;
		}
		Log.indentMsg("Using basecall error rate " + mERate + " for SNP evaluation", LogLevel.Basic);
		if (mProps.mUserKeys.contains("EXTRA_RATE") || mExRate == 0)
		{
			mExRate = Float.parseFloat(mProps.getProperty("EXTRA_RATE"));			
		}
		Log.indentMsg("Using insertion error rate " + mExRate, LogLevel.Basic);
		
		Utils.snpThresholds(mERate,Double.parseDouble(mProps.getProperty("SNP_SCORE")),"SNP thresholds");
		Utils.snpThresholds(mExRate,Double.parseDouble(mProps.getProperty("EXTRA_SCORE")),"Pad thresholds");
		
		setTopParents();
		loadCloneParents();
		init_rstat();

		// in case we already started this process, get the first number to start with
		rs = mDB.executeQuery("select count(*) count from contig where tcid=" + tcid);
		rs.first();
		int startingNum = 1 + rs.getInt("count");
		
		loadContigs(true,false,true);
		mID2SubContig.clear(); // this will get filled out with the final subcontig id's
		
		mCtgsBySize = new Contig[mID2Contig.numKeys()];
		
		for (int i = 0; i < mCtgsBySize.length; i++) mCtgsBySize[i] = null;
		
		int ic = 0;
		for (int id = mID2Contig.minKey(); id <= mID2Contig.maxKey(); id++)
		{		
			if (!mID2Contig.containsKey(id)) continue;
			
			mCtgsBySize[ic] = mID2Contig.get(id);
			ic++;
		}
		Arrays.sort(mCtgsBySize,new Contig.SizeCmp());
			
		int nThreads = Integer.parseInt(mProps.getProperty("CPUs"));
		FinalizeThread[] threadList = new FinalizeThread[nThreads];
		for (int i = 0; i < nThreads; i++)
		{
			threadList[i] = new FinalizeThread(this,i,tcid,startingNum);
		}

		int nToDo = mCtgsBySize.length;
		mThreadWorkList = new int[nToDo+1];

		for (int i = 1; i <= nToDo; i++)
		{
			mThreadWorkList[i] = i;
		}

		mThreadWorkItem = 1;
		mThreadsFinished = 0;

		for (int i = 0; i < nThreads; i++)
		{
			threadList[i].start();
		}
		
		int d = 0;
		while (mThreadsFinished < nThreads)
		{
			int nRemaining = mThreadWorkList.length - mThreadWorkItem;
			d++;
			if (d % 600 == 0)
			{
				Utils.singleLineMsg("finalize: " + nRemaining + " contigs remaining");
			}
			
			Thread.sleep(100);
		}
		mThreadWorkList = null;

		// Remove zombie entries from contclone, which point to intermediate contigs that have been removed.
		// Probably should be done elsewhere,but here is good enough.
		// Note that the buried zombie entries need to be retained so they can be collected into their
		// final contigs in SubContig.collectBuries.
		Vector<Integer> ids = new Vector<Integer>();
		rs = mDB.executeQuery("select cc.ctgid from contclone as cc left join contig as c" +
						" on c.ctgid=cc.ctgid where c.ctgid is null");
		while (rs.next())
		{
			ids.add(rs.getInt(1));
		}
		rs.close();
		if (ids.size()>0) // CASZ 8/22/19
			mDB.executeUpdate("delete from contclone where ctgid in (" + Utils.strIntVectorJoin(ids, ",") + ")");
		
		Log.indentMsg("Complete finalizing ", LogLevel.Basic); 
		

		mDB.executeUpdate("update ASM_tc_iter set finished=1 where TCID=" + tcid);

		Log.indentMsg("Write final contigs ", LogLevel.Basic);
		Utils.recordTime("FinalizeEnd",mAID,mDB);
	}
	private void fix_assemlib() throws Exception
	{
		TreeSet<String> curLibs = new TreeSet<String>();
		for (Library lib : mLibs)
		{
			curLibs.add(lib.mIDStr);	
		}
		TreeSet<String> newLibs = new TreeSet<String>();
		
		// Find the expression libs for this assembly 
		ResultSet rs = mDB.executeQuery("select distinct library.libid from library join clone_exp on clone_exp.lid=library.lid " +
				" join clone on clone.cid=clone_exp.cid join assemlib on assemlib.lid=clone.lid " + 
				" where assemlib.aid=" + mAID);
		while (rs.next())
		{
			String libid = rs.getString("library.libid");
			if (!curLibs.contains(libid))
			{
				newLibs.add(libid);	
			}
		}
		
		// Make the assemlib entry, unless we already did
		for (String libid : newLibs)
		{
			mDB.executeUpdate("insert ignore into assemlib (AID,LID,assemblyid,libid) values(" + mAID + 
					",(select lid from library where libid='" + libid + "'),'" + mIDStr + "','" + libid + "')");
	
		}
		
	}
	private void init_rstat() throws Exception
	{
		// Build the library counts needed for the rstat computation

		mLibCounts = new TreeMap<Integer,Integer>();
		ResultSet rs = mDB.executeQuery("select library.lid, library.libsize from library join assemlib " + 
						" on assemlib.lid=library.lid where assemlib.aid=" + mAID + " and library.ctglib=0");
		while (rs.next())
		{
			int lid = rs.getInt("library.lid");
			int libsize = rs.getInt("library.libsize");
			mLibCounts.put(lid,libsize);
		}

		mRstat = new RStat(mLibCounts);	
	}
	
	private void finalizeContig(Contig c, int ctgNum, int tcid, int nThread, DBConn db) throws Exception
	{		
		SubContig newSC = null;
		if (c.mTCID != tcid)
		{
			int padLen = String.valueOf(mID2Contig.numKeys()).length();
			String numStr = "" + ctgNum;
			while (numStr.length() < padLen)
			{
				numStr = "0" +numStr;	
			}
			String newName = mIDStr + "_" + numStr;
			
			Log.msg("Finalize " + newName + " from " + c.idStr(),LogLevel.Dbg);
			int oldID = c.mID;
			c.loadSequence(db);
			// combine its subcontigs, upload, and update the various id's
			c.mSC1.collectBuries(db);
			if (c.mSC2 != null) 
			{
				c.mSC2.collectBuries(db);
			}
			
			newSC = combineSubContigs(c.mSC1,c.mSC2, db);
			newSC.mTCID = tcid;
			newSC.doUpload(db,newName,true);
			
			int oldsc1 = c.mSC1.mID;
			int oldsc2 = (c.mSC2 != null ? c.mSC2.mID : 0);
			SubContig oldSC1 = c.mSC1;
			SubContig oldSC2 = c.mSC2;

			c.mSC1 = newSC;
			c.mSC2 = null;
			c.upload(db, tcid);
			
			db.executeUpdate("update ASM_scontig set merged_to=" + c.mID + " where scid=" + oldID);

			if (!Utils.isDebug())
			{
				db.executeUpdate("delete from contig where ctgid=" + oldsc1);
				db.executeUpdate("delete from contig where ctgid=" + oldsc2);
				db.executeUpdate("delete from ASM_scontig where scid=" + oldID);
			}	
			ResultSet rs2 = db.executeQuery("select count(*) as numclones from contclone where ctgid=" + newSC.mID);
			rs2.first();
			int numClones = rs2.getInt("numclones");
			rs2.close();
			
			// WMN orig_ccs stores the original, cap-created consensus, as a fail-safe in case placeClones messes up
			// and we need to supply a fix that someone can use without re-doing their assembly

			db.executeUpdate("update contig set numclones=" + numClones + ", orig_ccs=consensus where ctgid=" + newSC.mID);
			
			oldSC1.clear();
			if (oldSC2 != null) oldSC2.clear();		
		}
		else
		{
			// We got interrupted in the middle of finalizing this
			newSC = c.mSC1;
		}	
		
		if (!newSC.finalized(db))
		{
		
			int minIndelConfirm = Integer.parseInt(mProps.getProperty("INDEL_CONFIRM"));
			int minSNPConfirm = Integer.parseInt(mProps.getProperty("SNP_CONFIRM"));
			int minExtraConfirm = Integer.parseInt(mProps.getProperty("EXTRA_CONFIRM"));
			int poorAlign = Integer.parseInt(mProps.getProperty("POOR_ALIGN_PCT"));
			int ignoreHpoly = Integer.parseInt(mProps.getProperty("IGNORE_HPOLY"));
			double minSNPScore = Double.parseDouble(mProps.getProperty("SNP_SCORE"));
			double minExScore = Double.parseDouble(mProps.getProperty("EXTRA_SCORE"));
			boolean unburySNP = mProps.getProperty("UNBURY_SNP").equals("1");
			newSC.placeClones(db,minIndelConfirm,minSNPConfirm,poorAlign, minExtraConfirm, ignoreHpoly, unburySNP, mERate,mExRate,
					minSNPScore,minExScore,nThread);	
	
			int longestLen = 0;
			Clone longestClone = null;
			for (int cid : newSC.mAllClonesID)
			{
				Clone c2 = mID2Clone.get(cid);
				if (c2.mSeqLen > longestLen)
				{
					longestClone = c2;
					longestLen = c2.mSeqLen;
				}
			}
			if (longestClone == null)
			{
				Log.msg("WARNING could not find longest clone for ctg " + newSC.mID,LogLevel.Basic);
			}
			else
			{
				db.executeUpdate("update contig set longest_clone='" + longestClone.mFullName + "'where ctgid=" + newSC.mID);
			}
			
			newSC.libCounts(db);
			newSC.doRstat(mRstat, db);
			newSC.snpCounts(db);
			newSC.setFinalized(db, true);
			newSC.clear();
		}
	}
	// Only called for debug
	private void validateBuried() throws Exception
	{		
		Log.msg("Check buried reads for consistency",LogLevel.Detail);
		// Check that every topparent is in a current contig, and is not buried itself
		ResultSet rs = null;
		int nBuried = 0;
		for (int ic = mID2Clone.minKey(); ic <= mID2Clone.maxKey(); ic++)
		{
			if (!mID2Clone.containsKey(ic)) continue;
			Clone c = mID2Clone.get(ic);
			c.mBuried = false;
			c.mIsParent = false;
		}
		// First set which Reads are parent/children
		rs = mDB.executeQuery("select cid_child,cid_parent,cid_topparent from buryclone where aid=" + mAID); 
		while (rs.next()) 
		{
			int child = rs.getInt("cid_child");
			int parent = rs.getInt("cid_parent");
			Clone childC = mID2Clone.get(child);
			Clone parentC = mID2Clone.get(parent);
			
			if (childC.mBuried)
			{
				throw(new Exception("clone " + childC.mFullName + " buried twice!"));
			}
			childC.mBuried = true;
			nBuried++;
			
			parentC.mIsParent = true;
		}
		rs.close();
		// Now check that top parents aren't buried
		Log.msg("Check that top parents are not buried", LogLevel.Detail);		
		rs = mDB.executeQuery("select distinct cid_topparent from buryclone where aid=" + mAID); 
		while (rs.next()) 
		{
			int parent = rs.getInt("cid_topparent");
			Clone parentC = mID2Clone.get(parent);
			
			if (parentC.mBuried)
			{
				throw(new Exception("top parent" + parentC.mFullName + " is buried!"));
			}
			if (!parentC.mIsParent)
			{
				throw(new Exception("top parent" + parentC.mFullName + " is not a parent of any clone!"));
			}			
		}
		rs.close();
		
		// check that every non-buried clone is in a current contig
		int nonBuried = mID2Clone.numKeys() - nBuried;
		Log.msg("Checking that " + nonBuried + " non-buried reads are in contigs...",LogLevel.Detail);
		int i = 0;
		for (int cid = mID2Clone.minKey(); cid <= mID2Clone.maxKey(); cid++)
		{
			if (!mID2Clone.containsKey(cid)) continue;			
			Clone c = mID2Clone.get(cid);

			if (c.mBuried) continue;
			rs = mDB.executeQuery("select count(*) as count from contclone " +
					" join contig on contig.ctgid=contclone.ctgid " +
					" join ASM_scontig on ASM_scontig.scid=contig.sctgid " +
					" where contclone.cid=" + cid + 
					" and ASM_scontig.merged_to=ASM_scontig.scid and contig.aid=" + mAID);
			rs.first();
			int count = rs.getInt("count");
			rs.close();
			if (count == 0) 
			{
				throw(new Exception("non-buried clone " + cid + " is not in a current contig"));				
			}
			else if (count > 1)
			{
				throw(new Exception("non-buried clone " + cid + " is in " + count + " current contigs"));				
			}	
			i++;
			if (i % 10000 == 0)
			{
				Log.msg(i + " checked",LogLevel.Detail);
			}
		}
	}
	
	private void checkContigIntegrity(boolean force) throws Exception
	{
		Log.msg("Check contig integrity",LogLevel.Detail);
		Utils.termOut("Checking contig integrity");
		synchronized(this)
		{
			ResultSet rs = null;
			
			// force is true after Cliques are created
			if (!force && !mProps.getProperty("DEBUG").equals("1")) return;
			
			rs = mDB.executeQuery("select count(*) as count from contig where AID=" + mAID);
			rs.first();
			if (rs.getInt("count") == 0) return;
			rs.close();
			
			ID2Obj<Integer> child2parent = new ID2Obj<Integer>();
			ID2Obj<Integer> clone2topparent = new ID2Obj<Integer>();
			TreeSet<Integer> top = new TreeSet<Integer>();
			ID2Obj<Integer> clone2sctg = new ID2Obj<Integer>();
			ID2Obj<Integer> clone2ctg = new ID2Obj<Integer>();
			
			child2parent.setMinMax(mID2Clone.minKey(), mID2Clone.maxKey());
			clone2topparent.setMinMax(mID2Clone.minKey(), mID2Clone.maxKey());
			clone2sctg.setMinMax(mID2Clone.minKey(), mID2Clone.maxKey());
			clone2ctg.setMinMax(mID2Clone.minKey(), mID2Clone.maxKey());
			
			rs = mDB.executeQuery("select cid_child, cid_parent, cid_topparent " +
					" from buryclone where aid=" + mAID); 
			while (rs.next()) 
			{
				int child = rs.getInt(1);
				int parent = rs.getInt(2);
				int topParent = rs.getInt(3);
				
				if (parent == 0) 
					throw(new Exception("No parent"));
				
				child2parent.put(child, parent);
				clone2topparent.put(child, topParent);
				top.add(topParent);
			}
			rs.close();
					
			// 3. every non-buried clone is in exactly one current, un-merged contig
			rs = mDB.executeQuery("select cid, ASM_scontig.scid, contig.ctgid " +
					" from contclone " +
					" join contig on contclone.ctgid=contig.ctgid " +
					" join ASM_scontig on ASM_scontig.scid=contig.sctgid " +
					" where ASM_scontig.merged_to=ASM_scontig.scid and contig.aid=" + mAID);
			while (rs.next())
			{
				int cid = rs.getInt(1);
				int ctgid = rs.getInt(3);
				int scid = rs.getInt(2);
				
				if (clone2ctg.containsKey(cid)) 
					throw(new Exception("Clone " + cid + " is in more than one current contig"));
				if (clone2sctg.containsKey(cid)) 
					throw(new Exception("Clone " + cid + " is in more than one current supercontig"));
				
				clone2ctg.put(cid,ctgid);	
				clone2sctg.put(cid,scid);
			}
			rs.close();
			
			for (int cid = mID2Clone.minKey(); cid <= mID2Clone.maxKey(); cid++)
			{
				if (!mID2Clone.containsKey(cid)) continue;
				
				if (!child2parent.containsKey(cid) && !clone2sctg.containsKey(cid))
				{// FIXME crash on Mariadb 10.4
					throw(new Exception("Non-buried clone " + cid + " is not in a current contig"));
				}
			}
			
			// 5. Mate pairs are in the same supercontig (if they are not buried)
			
			for (MatePair p : mPairs)
			{
				if (!child2parent.containsKey(p.m3Prime.mID))
				{
					int sctg1 = clone2sctg.get(p.m3Prime.mID); 
					int sctg2 = clone2sctg.get(p.m5Prime.mID); 
					if (sctg1 != sctg2)
					{
						throw(new Exception("Mate pair " + p.m3Prime.mID + "," + p.m5Prime.mID + " are in different supercontigs " + sctg1 + "," + sctg2));
					}
				}
			}
			
			// 6. Mate pairs are buried together if at all
			
			for (MatePair p : mPairs)
			{
				if (child2parent.containsKey(p.m3Prime.mID) || child2parent.containsKey(p.m5Prime.mID))
				{
					if (child2parent.containsKey(p.m3Prime.mID) && !child2parent.containsKey(p.m5Prime.mID))
					{
						throw(new Exception("Clone " + p.m3Prime.mID + " is buried but mate " + p.m5Prime.mID + " isn't"));				
					}
					if (child2parent.containsKey(p.m5Prime.mID) && !child2parent.containsKey(p.m3Prime.mID))
					{
						throw(new Exception("Clone " + p.m5Prime.mID + " is buried but mate " + p.m3Prime.mID + " isn't"));				
					}
				}
			}
			
			// 7. No more than two supercontigs can merge to any other
			
			rs = mDB.executeQuery("select merged_to,count(*) as count from ASM_scontig join ASM_tc_iter on ASM_tc_iter.tcid=ASM_scontig.tcid where ASM_tc_iter.aid=" + 
					mAID + " and ASM_scontig.scid != ASM_scontig.merged_to group by merged_to");
			while (rs.next())
			{
				int scid = rs.getInt("merged_to");
				int count = rs.getInt("count");
				if (count > 2)
				{
					throw(new Exception(count + " ASM_scontigs merged to ASM_scontig " + scid));
				}
			}
			rs.close();
			
			// 8. All contigs have nonempty consensus
			
			rs = mDB.executeQuery("select count(*) as count from contig where consensus=''");
			rs.first();
			int count = rs.getInt("count");
			if (count > 0)
			{
				throw(new Exception(count + " contigs have empty consensus!"));
			}
			rs.close();

			// 9. No failed edges gave rise to contigs
			
			rs = mDB.executeQuery("select count(*) as count from ASM_tc_edge join ASM_tc_iter on ASM_tc_edge.tcid=ASM_tc_iter.tcid where ASM_tc_iter.aid=" + mAID + 
					" and ASM_tc_edge.errstr != '' and ASM_tc_edge.errstr != 'OK' and ASM_tc_edge.scid_result != 0" );
			rs.first();
			count = rs.getInt("count");
			if (count > 0)
			{
				rs = mDB.executeQuery("select eid from ASM_tc_edge join ASM_tc_iter on ASM_tc_edge.tcid=ASM_tc_iter.tcid where ASM_tc_iter.aid=" + mAID + 
				" and ASM_tc_edge.errstr != '' and ASM_tc_edge.errstr != 'OK' and ASM_tc_edge.scid_result != 0" );
				String eids = "";
				while (rs.next())
				{
					eids += String.valueOf(rs.getInt("eid")) + " ";
				}
				throw(new Exception(count + " failed edges made contigs:" + eids));
			}
			rs.close();
			
			child2parent.clear();
			child2parent = null;
			
			clone2topparent.clear();
			clone2topparent = null;
			
			top.clear();
			top = null;
			
			clone2sctg.clear();
			clone2sctg = null;
			
			clone2ctg.clear();
			clone2ctg = null;			
		}
		System.gc();
	}
	// save a time milepost to the DB
	private void recordTime(String stage) throws Exception
	{
		Utils.recordTime(stage,mAID,mDB);
	}

	// Put to parts of a L/R contig pair into one, joined by N's
	private SubContig combineSubContigs(SubContig sc1, SubContig sc2,DBConn db) throws Exception
	{
		sc1.loadClones(db);
		sc1.setRF();
		if (sc2 != null) 
		{
			sc2.loadClones(db);
			sc2.setRF();
			if (sc1.mRF != RF.F && sc1.mRF != RF.R &&
					sc2.mRF != RF.F && sc2.mRF != RF.R)
			{
				Log.msg("No 5'/3' in contig pair " + sc1.idStr() + ":" + sc2.idStr(),LogLevel.Detail);
			}
			else
			{
				if (sc1.mRF == RF.F || sc2.mRF == RF.F)
				{
					// We have a 5', make it the first one
					if (sc2.mRF == RF.F && sc1.mRF != RF.F)
					{
						SubContig tmp = sc1;
						sc1 = sc2;
						sc2 = tmp;
					}
				}
				else
				{
					Log.msg("No 5' in contig pair " + sc1.idStr() + ":" + sc2.idStr(),LogLevel.Detail);
					if (sc1.mRF == RF.R || sc2.mRF == RF.R)
					{
						// We have a 3', make it the second one
						if (sc1.mRF == RF.R && sc2.mRF != RF.R)
						{
							SubContig tmp = sc1;
							sc1 = sc2;
							sc2 = tmp;
						}
					}
				}
			}
		}
		else if (sc1.reversed())
		{
			sc1.flip();	
		}
		SubContig newSC = new SubContig(mID2Clone,mAID,mIDStr);
		if (Utils.isDebug())
		{
			newSC.mSource = "Final:" + sc1.idStr();
		}
		
		if (sc2 != null) newSC.mSource += "," + sc2.idStr();
		// add the two sets of clones
		newSC.mAllClonesID.addAll(sc1.mAllClonesID);
		if (sc2 != null) 
		{
			newSC.mAllClonesID.addAll(sc2.mAllClonesID);
			for (int cid : sc2.mAllClonesID)
			{
				Clone c = mID2Clone.get(cid);
				if (c.mGaps != "")
				{
					c.flipGaps();
				}
			}
		}
		for (int cid : newSC.mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			if (c.mPair != null)
			{
				newSC.mPairsIdx.add(c.mPair.mIdx);
			}
		}
		
		// combine the sequences, with n's in between, and reverse-complementing the 3'
		sc1.mStart = 1;
		sc1.mEnd = sc1.mCCS.length();
		newSC.mStart = 1;
		newSC.mCCS = sc1.mCCS.toLowerCase();
		newSC.mEnd = newSC.mCCS.length();
		newSC.mQual = sc1.mQual;
		if (sc2 != null)
		{
			sc2.flip();
			newSC.mEnd += 50 + sc2.mCCS.length();
			newSC.mNCount = 50;
			newSC.mNStart = newSC.mCCS.length();
			for(int i = 1; i <= 5; i++) 
			{
				newSC.mCCS += "nnnnnnnnnn";
				newSC.mQual += " 1 1 1 1 1 1 1 1 1 1 ";
			}

			newSC.mCCS += sc2.mCCS; //Utils.reverseComplement(sc2.mCCS);
			newSC.mQual += sc2.mQual; // qualReverse(sc2.mQual);		
			// update all the clone coords and UC
			for (int cid : sc2.mAllClonesID)
			{
				Clone c = mID2Clone.get(cid); 
				c.mStart += 50 + sc1.mCCS.length();
			}
		}
		return newSC;
	}

	// fire off the cap bury threads for cliques
	private void capBuryCliques(int TCID) throws Exception
	{
		String thisTag = "CLIQUE_BURY";
		if (Utils.checkUpToDate(thisTag, "", "")) return;
		
		Log.head("Clique cap buries",LogLevel.Basic);
		
		recordTime(thisTag + " start");
		int nToDo = 0;
		for (int id = mID2Contig.minKey(); id <= mID2Contig.maxKey(); id++)
		{	
			if (!mID2Contig.containsKey(id)) continue;
			Contig ctg = mID2Contig.get(id);
			if (ctg.mCapBuryDone) continue;
			if (ctg.totalSize() < 10) continue;
			nToDo++;
		}
		Log.indentMsg(nToDo + " cliques check for bury", LogLevel.Basic);
		
		if (nToDo > 0)
		{
			File capdir = new File(mCliqueDir,"burycap");
			Utils.checkCreateDir(capdir);

			if (nToDo == mID2Contig.numKeys())
			{
				Utils.clearDir(capdir);
			}
		
			int nThreads = Integer.parseInt(mProps.getProperty("CPUs"));
			CapBuryThread[] threadList = new CapBuryThread[nThreads];
			for (int i = 0; i < nThreads; i++)
			{
				threadList[i] = new CapBuryThread(this, capdir,i,TCID);
			}

			mThreadWorkList = new int[nToDo];
	
			nToDo = 0;
			for (int id = mID2Contig.minKey(); id <= mID2Contig.maxKey(); id++)
			{
				if (!mID2Contig.containsKey(id)) continue;
				Contig ctg = mID2Contig.get(id);
				ctg.mCapBuries = 0;
				if (ctg.mCapBuryDone) continue;
				if (ctg.totalSize() < 10) continue;
				mThreadWorkList[nToDo] = ctg.mID;
				nToDo++;
			}
	
			mThreadWorkItem = 0;
			mThreadsFinished = 0;
	
			for (int i = 0; i < nThreads; i++)
			{
				threadList[i].start();
			}
			
			int d = 0;
			while (mThreadsFinished < nThreads)
			{
				int nWait = nThreads-mThreadsFinished;
				int nRemaining = mThreadWorkList.length - mThreadWorkItem;
				d++;
				if (d % 6 == 0)
				{
					int nBuried = 0;
					for (int id = mID2Contig.minKey(); id <= mID2Contig.maxKey(); id++)
					{
						if (!mID2Contig.containsKey(id)) continue;
						nBuried += mID2Contig.get(id).mCapBuries;
					}
					
					Log.msg(nWait + " threads working: " + nRemaining + " contigs remaining; " + nBuried + " buries so far", LogLevel.Detail);
				}
				
				Thread.sleep(10000);
			}
			mThreadWorkList = null;
			int nBuried = 0;
			for (int id = mID2Contig.minKey(); id <= mID2Contig.maxKey(); id++)
			{
				if (!mID2Contig.containsKey(id)) continue;
				nBuried += mID2Contig.get(id).mCapBuries;
			}
			
			Log.indentMsg(nBuried + " reads newly buried",LogLevel.Basic);
		}
		Utils.writeStatusFile(thisTag);
		recordTime(thisTag + " end");
	}

	// Do cap buries for a contig. Used for cliques and TC's. 
	// 
	// We'll try to bury any clone lying in a region where there is at least minDepth coverage across its whole length. 
	// For efficiency, and to prevent loops, the reads are sorted in increasing order of start point and 
	// a clone can only be buried in a higher clone, i.e., one starting further to the left. 
	// We don't require full containment for burying! This is too restrictive. 
	// The buried clone may extend further to the right than the parent, up to the maxHang parameter. 
	public int capBuryContig(File topdir, Contig ctg, String capArgParam, DBConn db, int threadNum, int TCID) throws Exception
	{
		int minDepth = Integer.parseInt(mProps.getProperty("CAP_BURY_MIN_DEPTH"));
		int maxHang = Integer.parseInt(mProps.getProperty("CAP_BURY_MAX_HANG"));
		
		if (ctg.totalSize() <= minDepth) return 0;
		
		ctg.loadSequence(db);
		
		int depth1 = ctg.mSC1.buildDepthMap();
		int depth2 = 0;
		if (depth1 < minDepth) 
		{
			ctg.mSC1.destroyDepthMap();
			return 0;
		}
		if (ctg.mSC2 != null)
		{
			depth2 = ctg.mSC2.buildDepthMap();
			if (depth2 < minDepth)
			{
				ctg.mSC1.destroyDepthMap();
				ctg.mSC2.destroyDepthMap();
				return 0;
			}
		}
		TreeSet<Integer> toBeBuried = new TreeSet<Integer>();
		ctg.mSC1.leftOrderClones();
		if (ctg.mSC2 != null) ctg.mSC2.leftOrderClones();
		
		// Build a list of reads from both subcontigs, ordered by gaps and length. 
		int nclones = ctg.mSC1.mBuryCloneList.length;
		if (ctg.mSC2 != null)
		{
			nclones += ctg.mSC2.mBuryCloneList.length;
		}
		Clone[] bigBuryList = new Clone[nclones];
		int ic = 0;
		for (Clone cl : ctg.mSC1.mBuryCloneList)
		{
			cl.mSC = ctg.mSC1;
			bigBuryList[ic] = cl;
			ic++;
		}
		if (ctg.mSC2 != null)
		{
			for (Clone cl : ctg.mSC2.mBuryCloneList)
			{
				cl.mSC = ctg.mSC2;
				bigBuryList[ic] = cl;
				ic++;
			}			
		}
		Arrays.sort(bigBuryList,new Clone.MismatchLengthCmp());
		
		for (Clone c : bigBuryList)
		{
			if (c.mParent != null) 
			{
				continue; // can happen if this is the mate of something earlier in the list
			}
			SubContig sc = c.mSC;
			if (sc == null) 
			{
				throw(new Exception(ctg.idStr() + "Can't find subcontig for clone " + c.mFullName));
			}
			if (!sc.buryable(c.mID, minDepth, maxHang)) continue;
			if (c.mMate != null && !c.mMate.mSC.buryable(c.mMateID, minDepth,maxHang)) continue;
			
			// ok, now we know the clone and its mate have sufficient depth in their contigs to be buried
			// is there something to bury them in?
			if (sc.findBury(c.mID,maxHang))
			{
				sc.buryClone(c.mID);
				toBeBuried.add(c.mID);
				if (c.mMate != null)
				{
					c.mMate.mSC.buryClone(c.mMate.mID);
					toBeBuried.add(c.mMateID);
				}
			}
			
		}
		ctg.mSC1.clearLeftOrder();
		if (ctg.mSC2 != null) ctg.mSC2.clearLeftOrder();
		
		// Collect the ones we aren't burying so we can re-cap them
		Integer[] notToBeBuried = new Integer[ctg.totalSize() - toBeBuried.size()];
		int i = 0;
		for (int cid : ctg.mSC1.mAllClonesID)
		{
			if (!toBeBuried.contains(cid)) 
			{
				notToBeBuried[i] = cid;
				i++;
			}
		}
		if (ctg.mSC2 != null)
		{
			for (int cid : ctg.mSC2.mAllClonesID)
			{
				if (!toBeBuried.contains(cid)) 
				{
					notToBeBuried[i] = cid;
					i++;
				}
			}			
		}
		File capdir = new File(topdir,"bury" + ctg.mID);
		if (!buryRecapOK(ctg,capdir,capArgParam,notToBeBuried,threadNum, db))
		{
			//ctg.setCapBuryDone(true, db);
			Log.msg("Cap bury recap failed ctg:" + ctg.mID,LogLevel.Detail);
			ctg.clearBuries();
			return 0;			
		}
		Log.msg("Cap bury recap succeeded ctg:" + ctg.mID,LogLevel.Dbg);
		if (ctg.totalSize() - toBeBuried.size() > 100)
		{
			Log.msg(ctg.mID + " has " + ctg.totalSize() + " reads and " + toBeBuried.size() + " cap buries!!",LogLevel.Detail);
		}
		else
		{
			Log.msg(ctg.mID + " " + ctg.totalSize() + " reads;" + toBeBuried.size() + " cap buries",LogLevel.Detail);
		}
		
		// Use a transaction so that the contclone table stays in sync with the buryclone table
		db.openTransaction();
		PreparedStatement ps1 = db.prepareStatement("update contclone set buried=1 where CID=? and CTGID=?");
		PreparedStatement ps2 = db.prepareStatement("insert into buryclone (AID,assemblyid,bcode,TCID,CID_child,CID_parent,CID_topparent,childid,parentid,flipped) " + 
					" values(" + mAID + ",'" + mIDStr + "','" + BuryType.CAP.toString() + "','" + TCID + "',?,?,?,?,?,?)");
		
		for (int cid : ctg.mSC1.mAllClonesID)
		{
			if (toBeBuried.contains(cid))
			{
				Clone c = mID2Clone.get(cid);
				UC ucchild = c.mUC;
				UC ucpar = c.mParent.mUC;
				Boolean flipped = (ucchild != ucpar);
				
				ps1.setInt(1,cid);
				ps1.setInt(2,ctg.mSC1.mID);
				ps1.addBatch();
				
				ps2.setInt(1,cid);
				ps2.setInt(2,c.mParent.mID);
				ps2.setInt(3,c.mParent.mID);
				ps2.setString(4,c.mFullName);
				ps2.setString(5,c.mParent.mFullName);
				ps2.setInt(6,(flipped ? 1 : 0));
				ps2.addBatch();	
				ctg.mSC1.mBuriedClonesID.add(cid);
				c.mBuried = true;
			}

		}
		if (ctg.mSC2 != null)
		{
			for (int cid : ctg.mSC2.mAllClonesID)
			{
				if (toBeBuried.contains(cid))
				{
					Clone c = mID2Clone.get(cid);

					UC ucchild = c.mUC;
					UC ucpar = c.mParent.mUC;
					Boolean flipped = (ucchild != ucpar);
					
					ps1.setInt(1,cid);
					ps1.setInt(2,ctg.mSC2.mID);
					ps1.addBatch();
					
					ps2.setInt(1,cid);
					ps2.setInt(2,c.mParent.mID);
					ps2.setInt(3,c.mParent.mID);
					ps2.setString(4,c.mFullName);
					ps2.setString(5,c.mParent.mFullName);
					ps2.setInt(6,(flipped ? 1 : 0));
					ps2.addBatch();	
					ctg.mSC2.mBuriedClonesID.add(cid);
					c.mBuried = true;
				}

			}
		}

		ps1.executeBatch();
		ps2.executeBatch();
		db.closeTransaction();
		
		ps1.close();
		ps2.close();
	
		if (!Utils.isDebug())
		{
			Utils.deleteDir(capdir);
		}
		ctg.mCapBuries = toBeBuried.size();
		toBeBuried.clear(); toBeBuried = null;
		
		ctg.clearSeqs();
		
		return ctg.mCapBuries;
	}

	// Make sure the contig still hangs together after the burying. 
	private boolean buryRecapOK(Contig ctg, File capdir, String capArgParam, Integer[] cids, int threadNum, DBConn db) throws Exception
	{
		if (mProps.getProperty("DO_RECAP").equals("0")) return true;
		
		Utils.checkCreateDir(capdir);
		Utils.clearDir(capdir);		
		assembleClones(capdir,cids,capArgParam, threadNum,db);

		File af = new File(capdir,"seqs.fasta.cap.ace");
		AceFile aceFile = new AceFile(mAID,mIDStr, af,mID2Clone, mName2Clone,"",db);
		aceFile.doParse(false,0);
		// we're looking for the same number of contigs
		boolean ret = true;
		if (aceFile.mOK != OKStatus.OK)
		{
			ret = false;
		}
		else if (ctg.mSC2 == null)
		{
			if (aceFile.mContigs.size() > 1) return false;
		}
		else 
		{
			if (aceFile.mContigs.size() != 2) return false;
		}
		aceFile.clear();
		return ret;
	}
    /// skip assembly
	private void summarizeSkipAssembly() throws Exception
	{		
		ResultSet rs = null;

		StringBuilder statsMsg = new StringBuilder();
		
		statsMsg.append(Log.head("Statistics",LogLevel.Basic));
		
		// Unitran lengths
		int[] rangeMin = new int[8];
		int[] rangeMax = new int[8];
		int[] counts = new int[8];
		
		rangeMin[0] = 1; rangeMax[0] = 100;
		rangeMin[1] = 101; rangeMax[1] = 500;
		rangeMin[2] = 501; rangeMax[2] = 1000;
		rangeMin[3] = 1001; rangeMax[3] = 2000;
		rangeMin[4] = 2001; rangeMax[4] = 3000;
		rangeMin[5] = 3001; rangeMax[5] = 4000;
		rangeMin[6] = 4001; rangeMax[6] = 5000;
		rangeMin[7] = 5001; rangeMax[7] = 1000000;
		for (int i = 0; i < counts.length; i++) counts[i] = 0;
		
		rs = mDB.executeQuery("select length(consensus) as ccslen from contig");
		while (rs.next())
		{
			int length = rs.getInt("ccslen");
			for (int i = 0; i < rangeMin.length; i++)
			{
				if (rangeMin[i] <= length && rangeMax[i] >= length)
				{
					counts[i]++;	
				}
			}
		}
		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Sequence lengths (bp)",LogLevel.Basic));		
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"Length","1-100","101-500","501-1000","1001-2000","2001-3000","3001-4000","4001-5000",">5000"));
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"#Sequences",counts[0],counts[1],counts[2],counts[3],counts[4],counts[5],counts[6],counts[7]));

		rangeMin = new int[6];
		rangeMax = new int[6];
		counts = new int[6];
		
		rangeMin[0] = 0; rangeMax[0] = 50;
		rangeMin[1] = 51; rangeMax[1] = 60;
		rangeMin[2] = 61; rangeMax[2] = 70;
		rangeMin[3] = 71; rangeMax[3] = 80;
		rangeMin[4] = 81; rangeMax[4] = 90;
		rangeMin[5] = 91; rangeMax[5] = 100;

		for (int i = 0; i < counts.length; i++) counts[i] = 0;

		mDB.executeUpdate("replace into assem_msg (aid,msg) values(" + mAID + ",'" + statsMsg.toString() + "')");
	}
	private void summarizeAssembly() throws Exception
	{
		ResultSet rs = null;
		StringBuilder statsMsg = new StringBuilder();
		statsMsg.append(Log.head("Assembly Statistics",LogLevel.Basic));
		statsMsg.append(Log.columnsInd(25, LogLevel.Basic, "DATASET", "#SEQS", "#SINGLETONS", "#BURIED"));
		
		int finalTCID = Utils.finalTC(mDB,mAID);
		for (Library lib : mLibs)
		{
			lib.collectStats(finalTCID);
			int numEST = lib.mNumESTLoaded;
			int numBuried = lib.mNumBuried;
			int numSingle = lib.mNumSingle;
			int buryPct = Utils.getPercent(numBuried,numEST);
			int singlePct = Utils.getPercent(numSingle,numEST);
			statsMsg.append(Log.columnsInd(25, LogLevel.Basic,lib.mIDStr, lib.mNumESTLoaded, numSingle + " (" + singlePct + "%)", numBuried + " (" + buryPct + "%)"));
		}	
		
		// Reads
		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Total reads:  " + mClones.length, LogLevel.Basic));
		
		rs = mDB.executeQuery("select count(*) as count from buryclone where aid=" + mAID);
		rs.first();
		int nBuried = rs.getInt("count");
		rs = mDB.executeQuery("select count(*) as count from buryclone where aid=" + mAID + " and bcode='cap'");
		rs.first();
		int nCapBuried = rs.getInt("count");
		statsMsg.append(Log.indentMsg("Total buried: " + nBuried + "  Initial buries: " + (nBuried-nCapBuried) + "   Buried during assembly: " + nCapBuried,LogLevel.Basic));
		
		// Unitran EST counts
		rs = mDB.executeQuery("select max(tcid) as max from ASM_tc_iter where aid=" + mAID);
		rs.first();
		int maxTC = rs.getInt("max");
		
		rs = mDB.executeQuery("select count(*) as count from contig where tcid=" + maxTC);
		rs.first();
		int nContigs = rs.getInt("count");
		rs = mDB.executeQuery("select count(*) as count from contig where tcid=" + maxTC + " and numclones=1");
		rs.first();
		int nSingletons = rs.getInt("count");
		
		// Get the contigs which are just one paired EST 
		rs = mDB.executeQuery("select count(*) as count from contclone join clone on clone.cid=contclone.cid join contig on contig.ctgid=contclone.ctgid " +
						" where contig.tcid=" + maxTC + " and contig.numclones=2 and clone.mate_CID > 0");
		rs.first();
		int nSinglePair = rs.getInt("count")/2;	
		
		rs = mDB.executeQuery("select count(*) as count from contig where tcid=" + maxTC + " and notes like '%suspect%'");
		rs.first();
		int nSuspect = rs.getInt("count");		
		
		statsMsg.append(Log.newLine(LogLevel.Basic));
		//statsMsg.append(Log.msg("   Summary",LogLevel.Basic));

		int[] rangeMin = new int[9];
		int[] rangeMax = new int[9];
		int[] counts = new int[9];
		
		rangeMin[0] = 1; rangeMax[0] = 1;
		rangeMin[1] = 2; rangeMax[1] = 2;
		rangeMin[2] = 3; rangeMax[2] = 5;
		rangeMin[3] = 6; rangeMax[3] = 10;
		rangeMin[4] = 11; rangeMax[4] = 20;
		rangeMin[5] = 21; rangeMax[5] = 50;
		rangeMin[6] = 51; rangeMax[6] = 100;
		rangeMin[7] = 101; rangeMax[7] = 1000;
		rangeMin[8] = 1001; rangeMax[8] = 1000000;
		
		for (int i = 0; i < rangeMin.length; i++)
		{
			rs = mDB.executeQuery("select count(*) as count from contig where numclones >= " + rangeMin[i] + " and numclones <= " + rangeMax[i] + " and tcid=" + maxTC);
			rs.first();
			counts[i] = rs.getInt("count");
		}
		
		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Contig aligned sequence counts",LogLevel.Basic));
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"Counts","=2","3-5","6-10","11=20","21-50","51-100","101-1000",">1000"));
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"#Contigs",counts[1],counts[2],counts[3],counts[4],counts[5],counts[6],counts[7],counts[8]));
		
		// Unitran lengths
		rangeMin = new int[8];
		rangeMax = new int[8];
		counts = new int[8];
		
		rangeMin[0] = 1; rangeMax[0] = 100;
		rangeMin[1] = 101; rangeMax[1] = 500;
		rangeMin[2] = 501; rangeMax[2] = 1000;
		rangeMin[3] = 1001; rangeMax[3] = 2000;
		rangeMin[4] = 2001; rangeMax[4] = 3000;
		rangeMin[5] = 3001; rangeMax[5] = 4000;
		rangeMin[6] = 4001; rangeMax[6] = 5000;
		rangeMin[7] = 5001; rangeMax[7] = 1000000;
		for (int i = 0; i < counts.length; i++) counts[i] = 0;
		
		rs = mDB.executeQuery("select length(consensus) as ccslen from contig where tcid=" + maxTC);
		while (rs.next())
		{
			int length = rs.getInt("ccslen");
			for (int i = 0; i < rangeMin.length; i++)
			{
				if (rangeMin[i] <= length && rangeMax[i] >= length)
				{
					counts[i]++;	
				}
			}
		}
		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Contig lengths (bp)",LogLevel.Basic));		
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"Length","1-100","101-500","501-1000","1001-2000","2001-3000","3001-4000","4001-5000",">5000"));
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"#Contigs",counts[0],counts[1],counts[2],counts[3],counts[4],counts[5],counts[6],counts[7]));

		rangeMin = new int[6];
		rangeMax = new int[6];
		counts = new int[6];
		
		rangeMin[0] = 0; rangeMax[0] = 50;
		rangeMin[1] = 51; rangeMax[1] = 60;
		rangeMin[2] = 61; rangeMax[2] = 70;
		rangeMin[3] = 71; rangeMax[3] = 80;
		rangeMin[4] = 81; rangeMax[4] = 90;
		rangeMin[5] = 91; rangeMax[5] = 100;

		for (int i = 0; i < counts.length; i++) counts[i] = 0;

		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Total contigs:     " + nContigs, LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Contigs(>1 seq):    " + (nContigs-nSingletons), LogLevel.Basic));
		if (nSinglePair > 0)
		{
			statsMsg.append(Log.indentMsg("  Single mate-pair: " + nSinglePair, LogLevel.Basic));
		}
		statsMsg.append(Log.indentMsg("Singletons:         " + nSingletons, LogLevel.Basic));
		if (nSuspect > 0)
		{
			statsMsg.append(Log.indentMsg("Suspect contigs:    " + nSuspect, LogLevel.Basic));
		}

		mDB.executeUpdate("replace into assem_msg (aid,msg) values(" + mAID + ",'" + statsMsg.toString() + "')");
	}
	class NbhdCmp implements Comparator<Integer> //, intCmp
	{
		Vector<Integer> mSizes;

		private NbhdCmp(Vector<Integer> sizes)
		{
			mSizes = sizes;
		}

		public int compare(Integer a, Integer b)
		{
			int sizea = mSizes.get(a);
			int sizeb = mSizes.get(b);
			if (sizea > sizeb)
				return -1;
			else if (sizea < sizeb)
				return 1;
			return 0;
		}
	}
	

	

	private boolean doCliques() throws Exception
	{
		return mProps.getProperty("DO_CLIQUES").equals("1");
	}
	private  void fixAssemLibCounts() throws Exception
	{
		mDB.executeUpdate("update assemlib set singletons=( select count(*) from contig_counts " + 
    						" join contig on contig.contigid=contig_counts.contigid " + 
                           "  where contig_counts.libid=assemlib.libid and count > 0 and numclones = 1 " +
                           " and contig.aid=assemlib.aid and contig.aid=" + mAID + ")");
		
		mDB.executeUpdate("update assemlib set contigs=( select count(*) from contig_counts " + 
    				" join contig on contig.contigid=contig_counts.contigid " +
                	" where contig_counts.libid=assemlib.libid and count > 0 and numclones > 1 " + 
                		" and contig.aid=assemlib.aid and contig.aid=" + mAID + ")");
		mDB.executeUpdate("update assemlib,library set assemlib.uniqueContigs= " +
    			" (select  count(*)  from contig_counts as ctc " + 
    				" join contig on contig.contigid=ctc.contigid " +
                	" where ctc.count= " + 
                	"	(select sum(count) from contig_counts as ctc2 join library as lib2 on " +
                    "	lib2.libid=ctc2.libid where ctc2.contigid=ctc.contigid and lib2.ctglib=library.ctglib) " +  
               		" and ctc.libid=assemlib.libid and contig.aid=assemlib.aid) " +
              	 " where library.libid=assemlib.libid and assemlib.aid=" + mAID + "");
	}	
	/***************************************************************
	 * SKIP ASSEMBLY
	 * Much of this code is copied in loadLibMain
	 */
	private void skipAssembly() throws Exception
	{
		// Just make the contigs from the clones
		ResultSet rs = mDB.executeQuery("select count(*) as nclones from clone");
		rs.first();
		int nclones = rs.getInt("nclones");
		Log.head("Performing instantiate", LogLevel.Basic);
		Log.indentMsg("Instantiating " + nclones + " sequences, may take a while", LogLevel.Basic);
		mDB.executeQuery("set foreign_key_checks = 0");
		fix_assemlib();
		
		mDB.executeUpdate("alter table contig add tmpCID bigint default 0");
		mDB.executeUpdate("insert into contig " +
				"(AID,contigid,tmpCID,assemblyid,numclones,consensus,quality," +
				"consensus_bases,frpairs,finalized) " +
				"(select " + mAID + ",cloneid,CID,'" + mIDStr + "',1," +
				"sequence,quality, length(sequence),0,1 from clone " +
				"join assemlib on clone.LID=assemlib.LID " +
				" where assemlib.AID=" + mAID + " order by CID asc)");
		rs = mDB.executeQuery("select count(*) as count from contig where AID=" + mAID);
		rs.first();
		int ctgPad = rs.getString("count").length();
		rs = mDB.executeQuery("select min(CTGID) as min from contig where AID=" + mAID);
		rs.first();
		int minID = rs.getInt("min");
		
		boolean hasLoc=false;
		rs = mDB.executeQuery("show columns from assem_msg like 'hasLoc'");
		if (rs.first()) hasLoc=true;
		
		if (mProps.getProperty("USE_TRANS_NAME").equals("0") && !hasLoc)
		{
			mDB.executeUpdate("update contig set contigid=concat('" + mIDStr + 
				"','_',lpad((CTGID-" + minID + "+1)," + ctgPad + ",0))");
		}
		mDB.executeUpdate("insert into contclone (CTGID,CID,contigid,cloneid,orient,leftpos,gaps,extras,ngaps, " +
				"mismatch,numex,buried,prev_parent,pct_aligned) (select CTGID,tmpCID,contigid,cloneid,'U',1," +
				"'','',0,0,0,0,0,100 from contig join clone on clone.CID=contig.tmpCID where contig.AID=" + 
				mAID + " order by CTGID asc)");
		mDB.executeUpdate("alter table contig drop tmpCID");
		mDB.executeQuery("set foreign_key_checks = 1");
		
		Schema.addCtgLibFields(mDB);
		allLibCounts(mDB);
		mDB.executeUpdate("update contig set finalized=1 where AID=" + mAID);
		mDB.executeUpdate("update assembly set completed=1, assemblydate=NOW() where AID=" + mAID);
		
		if (!hasLoc) {
			summarizeSkipAssembly(); 
			return;
		}
		/******** For Gene Location inputs ************/
		Log.indentMsg("Setting coordiantes", LogLevel.Basic);
	
		// Pattern is something like: LG_1:1-50(+)_1 where last _n is for duplicates
		Pattern mBEDPat = Pattern.compile("(\\S+):(\\d+)-(\\d+)"); 
		HashMap <String, String> origMap = new HashMap <String, String> ();
		rs = mDB.executeQuery("select cloneid, origid from clone");
		while (rs.next()) origMap.put(rs.getString(1), rs.getString(2));
		
		HashMap <Integer, String> ctgMap = new HashMap <Integer, String> ();
		rs = mDB.executeQuery("select CTGID, contigid from contig");
		while (rs.next()) ctgMap.put(rs.getInt(1), rs.getString(2));
		
		int cntBad=0;
		for (int ctgid : ctgMap.keySet()) {
			String contigid = ctgMap.get(ctgid);
			String origid = origMap.get(contigid);
			Matcher m = mBEDPat.matcher(origid);
			if (m.find()) 
			{ 
				String group = m.group(1);
				int start = Integer.parseInt(m.group(2));
				int end = Integer.parseInt(m.group(3));
				String sense="+";
				if (origid.contains("(-)")) sense = "-";
				mDB.executeUpdate("update contig set seq_group='" + group + 
			"', seq_start=" + start + ",seq_end=" + end +  ",seq_strand='" + sense + "'" +
					" where CTGID=" + ctgid); 
			}
			else cntBad++;
		}
		if (cntBad>0)
			Log.indentMsg("Warn: could not set for " + cntBad, LogLevel.Detail);
		summarizeSkipAssembly(); 
	}
	// Some of this code is replicated in several places; search LN__ to find them. 
	private void allLibCounts(DBConn db) throws Exception
	{
		// Step 1: fill the contig_counts table, and the contig.L__ and contig.LN__ fields
		// First initialize counts to zero for all libs, including the ones from expression files
		Utils.singleLineMsg("Update counts");
		db.executeUpdate("delete contig_counts.* from contig_counts, contig " +
				"where contig_counts.contigid=contig.contigid and contig.AID=" + mAID);
		db.executeUpdate("insert into contig_counts (select contigid, libid, 0 from contig,assemlib  where contig.aid=" + mAID + 
				" and assemlib.AID=" + mAID + " order by contig.CTGID asc, assemlib.LID asc)");
		// Now add the counts due to membership in the contig 
		db.executeUpdate("update contig_counts set count=count+(select count(*) from contclone  " + 
				" join clone on clone.cid=contclone.cid where contclone.contigid=contig_counts.contigid " +
				" and clone.libid=contig_counts.libid) ");
		
		// Now add the counts that came from loading the expression files *clone_exp table)
		db.executeUpdate("update contig_counts set count=count+(select IFNULL(sum(count),0) from clone_exp  " + 
				" join clone on clone.cid=clone_exp.cid " + 
				" join contclone on contclone.cid=clone.cid " + 
				" join library on library.lid=clone_exp.lid " +
				" where contclone.contigid=contig_counts.contigid " +
				" and library.libid=contig_counts.libid and clone_exp.rep=0) ");

		ResultSet rs;
		Vector<String> Ls = new Vector<String>();
		Vector<String> LNs = new Vector<String>();

		Vector<String> libs = new Vector<String>();
		rs = db.executeQuery("select libid from assemlib where AID=" + mAID);
		while (rs.next())
		{
			libs.add(rs.getString("libid"));
		}
		
		for (String libid : libs)
		{
			String col = "L__" + libid;
			String sql = "update contig,contig_counts " +
					"set contig." + col + "=contig_counts.count " +
					" where contig_counts.contigid=contig.contigid " +
					" and contig_counts.libid='" + libid + "'";
			mDB.executeUpdate(sql);
			Utils.setRPKM(mDB, libid, 1);
		}
		mDB.executeUpdate("update assembly set ppx=0");

		// Step 2: totalexp, totalexpN, and rstat
		
		// get just the expression libs
		Utils.singleLineMsg("Rstat...");
		rs = db.executeQuery("select library.libid,library.libsize from library " +
				"join assemlib on assemlib.LID=library.LID where ctglib=0");
		libs.clear();
		Vector<Integer> libSizes = new Vector<Integer>();
		while (rs.next())
		{
			String libid = rs.getString("libid");
			int size = rs.getInt("libsize");
			libs.add(libid);
			String col = "L__" + libid;
			String colN = "LN__" + libid;
			
			Ls.add(col);
			LNs.add(colN);
			libSizes.add(size);
		}
		if (Ls.size() > 0)
		{
			String LSum = Utils.join(Ls,"+");
			String LNSum = Utils.join(LNs,"+");
			db.executeUpdate("update contig " +
					"set totalexp=(" +LSum + "), totalexpN=(" + LNSum + ") " +
					"where AID=" + mAID);
	
			RStat rsObj = new RStat(libSizes.toArray(new Integer[1]));
			String lSel =  Utils.join(Ls, ",");
			rs = db.executeQuery("select " + lSel + ",CTGID from contig");
			int[] ctgCounts = new int[libSizes.size()];
			PreparedStatement ps = db.prepareStatement("update contig set rstat=? where CTGID=?");
			while (rs.next())
			{			
				int CTGID = rs.getInt("CTGID");
				for (int i = 0; i < ctgCounts.length; i++)
				{
					ctgCounts[i] = rs.getInt(Ls.get(i));
				}
				double rstat = rsObj.calcRstat(ctgCounts);
				ps.setInt(2, CTGID);
				ps.setFloat(1,(float)rstat);
				ps.execute();
			}
			ps.close();
			rs.close();
		}
	}	
}
