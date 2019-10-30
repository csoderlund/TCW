package sng.assem;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Vector;
import java.sql.ResultSet;
import java.sql.PreparedStatement;


import sng.assem.enums.LibAction;
import sng.assem.enums.LibStatus;
import sng.assem.enums.LogAction;
import sng.assem.enums.LogLevel;
import sng.database.Globals;
import sng.database.Schema;
import sng.database.Version;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.methods.FileHelpers;
import util.methods.TCWprops;
import util.methods.Stats;
import util.methods.Out;


// Main library loading class. Called by loadLib script.
public class LoadLibMain
{
	static TCWprops mProps;
	static Log mLog;
	static File mLibDir = null;
	static String mProj;
	static DBConn mDB;
	static TreeMap<String, Library> mLibs;
	static Vector <String> keepOrder;
	static boolean mNoPrompts = false;
	static TreeSet<File> mExpFiles = null;
	static int mDefQual = 20;
	static boolean debug=true;

	public LoadLibMain()
	{
	}
	public static void main(String[] args)
	{
		Version.printTCWversion();
		try
		{
			new LoadLibMain();
			LoadLibMain.run(true, args);
		} 
		catch (Exception e)
		{   
			System.err.println("Fatal error - exiting (see sTCW.error1.log)");
			Log.exception(e);
			try
			{
				if (Utils.isDebug()) e.printStackTrace();
				FileWriter fw = new FileWriter(new File("sTCW.error1.log"));
				for (StackTraceElement elt : e.getStackTrace())
				{
					fw.write(elt.toString() + "\n");
				}
				fw.flush();
				fw.close();
				System.exit(-1);
			}
			catch (Exception q){System.exit(-1);}
			System.exit(-1);
		}
	}

	public static void run(boolean exitWhenComplete, String[] args) throws Exception
	{
		if (args.length == 0 || args.length > 2) printUsage();
		
		long startTime = Out.getTime();
		
		new Utils(); // initialize the class static members
		Utils.mProps = mProps;

		int argStart = 0;
		if (args[0].equals("-q"))
		{
			argStart = 1;
			mNoPrompts = true;
		}
		mProj = args[argStart];

		FileHelpers.mergeDir(Globals.OLDLIBDIR, Globalx.PROJDIR, true); 
		String libDir = Globalx.PROJDIR;
		String[] libdirs = { libDir, "../" + libDir, "../../" + libDir };
		boolean found = false;
		File f = null;
		for (String lib : libdirs)
		{
			f = new File(lib);
			if (f.isDirectory())
			{
				found = true;
				break;
			}
		}
		if (!found)
		{
			Log.die("Unable to find the '" + libDir + " directory.");
			return;
		}

		File p = new File(f.getAbsolutePath() + "/" + mProj);
		if (p.isDirectory())
		{
			mLibDir = p;
		} else
		{
			Log.msg("Unable to find the directory '" + mProj + "' under the " + libDir + " directory",LogLevel.Basic);
			return;
		}

		// Assm uses its own logging, but writes into the same log directory as Annotation
		File logDir =  new File(mLibDir, Globalx.logDir);
		if (!logDir.exists()) {
			System.err.println("Creating project log directory ...");
			if (!logDir.mkdir())  {
				System.err.println("*** Failed to create project log directory '" + 
					logDir.getAbsolutePath() + "'.");
				return;
			}
		};
		
		File logFile = new File(logDir, Globals.loadFile);

		mLog = new Log(logFile);
		Log.setDebugLog(logFile);

		Log.addLogAction(LogLevel.Basic, LogAction.Terminal);
		Log.addLogAction(LogLevel.Basic, LogAction.Log);
		Log.addLogAction(LogLevel.Basic, LogAction.DebugLog);
		Log.addLogAction(LogLevel.Detail, LogAction.DebugLog);
		
		mProps = new TCWprops(TCWprops.PropType.Lib);
		Utils.mProps = mProps;

		if (Utils.isDebug())
		{
			Log.addLogAction(LogLevel.Detail, LogAction.Terminal);		
		}

		File c = new File(mLibDir.getAbsolutePath() + "/" + Globals.LIBCFG);
		if (c.isFile())
		{
			try
			{
				mProps.load(c);
			} 
			catch (Exception e)
			{
				Log.exception(e);
				Log.die("Problem loading config file " + c.getAbsolutePath());				
			}
		} 
		else
		{
			Log.die("Unable to find LIB.cfg under the '" + mProj + "' directory");
		}
		Log.head("Check database",LogLevel.Basic);
				
		String db = mProps.getProperty("STCW_db");
		if (db==null) mProps.getProperty("PAVE_db");
		if (db==null) {
			System.err.println("Fatal Error: Missing STCW_db in LIB.cfg");
			return;
		}
		
		HostsCfg hosts = new HostsCfg();
	
		// Give the user an option to delete an existing database, in case it is left over from an earlier
		// failed install. If the DB is empty, we will require that it be deleted, since it could have some other problem. 
		
		boolean DBAlreadyExists = DBConn.checkMysqlDB("Load Data ", hosts.host(), db, hosts.user(),hosts.pass());	
		boolean DBNewlyCreated = false;       // If it's newly created, then we'll delete it if the load fails
		if (DBAlreadyExists)
		{
			if (DBConn.oldDBisEmpty(hosts.host(),db,hosts.user(),hosts.pass()))
			{
				if (Utils.yesNo("Database " + db + " exists but is empty. It will be deleted and re-created. Continue "))
				{
					DBConn.deleteDB(hosts.host(), db, hosts.user(), hosts.pass());
					DBAlreadyExists = false;					
				}
				else
				{
					Log.indentMsg("Exiting; select a different database, or delete the existing one, in order to continue.", LogLevel.Basic);
					return;
				}
			}
			else if (Utils.yesNo("Database " + db + " already exists. Do you want to delete it "))
			{
				if (Utils.yesNo("Database " + db + " will be deleted. Are you sure "))
				{
					DBConn.deleteDB(hosts.host(), db, hosts.user(), hosts.pass());
					DBAlreadyExists = false;
				}				
			}
		}
		if (!DBAlreadyExists)
		{		
			Log.indentMsg("Creating database " + db + " on host " + hosts.host(),LogLevel.Basic);
			DBConn.createMysqlDB(hosts.host(),db,hosts.user(),hosts.pass());
			DBNewlyCreated = true;
		}
		
		mDB = hosts.getDBConn(db);
			
		Schema schObj = new Schema(mDB);
		
		if (DBNewlyCreated)
		{
			schObj.loadSchema();
		}
		else
		{
			schObj.update(); // how far back do we support
		}

		ResultSet rs = mDB.executeQuery("select count(*) as count from assembly");
		rs.first();
		int nCurrentAssemblies = rs.getInt("count");
		
		// DB and config file seem to be ok
		// Let's see what state the libs are in

		Log.head("Check library status",LogLevel.Basic);

		mLibs = new TreeMap<String, Library>();
		keepOrder = new Vector<String> ();
		for (Properties prop : mProps.mLibProps)
		{
			Library lib = new Library(mDB, prop, mLibDir);
			if (lib.mIDStr.trim().equals("")) {
				System.err.println("Empty ID - remove and restart");
			}
			if (!Utils.validName(lib.mIDStr))
			{
				Log.die("Invalid ID " + lib.mIDStr + ": may only contain letters, numbers, underscores");	
			}
			mLibs.put(lib.mIDStr, lib);
			keepOrder.add(lib.mIDStr);
		}
		
		// Make sure no duplicate clone names
		// Collect the expression files
		mExpFiles = new TreeSet<File>();
		for (Library lib : mLibs.values())
		{
			if (lib.hasExp())
			{
				mExpFiles.add(lib.mExpFile);
			}
		}

		Log.newLine(LogLevel.Basic);
		dumpLibraryStatus();
		Log.newLine(LogLevel.Basic);

		int n2Load = 0, n2Update = 0;
		for (Library lib : mLibs.values())
		{			
			if (lib.toBeLoaded()) n2Load++;
			else if (lib.mAction == LibAction.UpdateProperties) n2Update++;
		}

		if (n2Load == 0 && n2Update == 0 )
		{
			Log.indentMsg("Datasets are all up to date",LogLevel.Basic);
			if(exitWhenComplete)
				System.exit(0);
			return;
		}
		if (n2Load > 0 && nCurrentAssemblies >= 1)
		{
			Log.die("Not able to add new dataset to existing database.");
		}
		Log.indentMsg(n2Load + " datasets to load, " + n2Update + " datasets to update",LogLevel.Basic);
		if (!mNoPrompts && !DBNewlyCreated && !Utils.yesNo("\n\tContinue"))
		{
			System.err.println("execLoadLib aborted by user");
			return;
		}		
		
		// See if any of the libs have qual files.
		// If not, we will use default=20 on all of them, otherwise default=18 
		boolean anyQuals = false;
		for (Library lib : mLibs.values())
		{
			if (lib.mQualFile != null)
			{
				anyQuals = true;
				break;
			}
		}
		
		mDefQual = (anyQuals ? 18 : 20);
		if (anyQuals)
		{
			Log.indentMsg("Some quality files found: using default 18 for missing qual values", LogLevel.Basic);
		}
		else if (!DBAlreadyExists)
		{
			Log.indentMsg("No quality files supplied: using default 20 for all qual values", LogLevel.Basic);
		}
		
		for (Library lib : mLibs.values())
		{
			lib.mDefQual = mDefQual;
		}
		
		// XXX start the load
		if (n2Load > 0)
		{
			TreeSet<String> libNames = new TreeSet<String>();
			for (Library lib : mLibs.values())
			{
				for (String libName : lib.getAllNames())
				{
					if (libNames.contains(libName))
					{
						Log.die("Dataset name " + libName + " is not unique. Change name or prefix to fix.");
					}
				}
			}
			Log.head("Verifying sequence and quality files",LogLevel.Basic);
			HashSet<String> cloneNames = new HashSet<String>();
			try {
				for (Library lib : mLibs.values())
				{
					if (lib.toBeLoaded())
					{
						lib.scanSeqFiles(cloneNames);
					}
				}
				for (Library lib : mLibs.values())
				{
					if (lib.toBeLoaded() && lib.mQualFile != null)
					{
						lib.scanQualFiles();
					}
				}
			}
			catch (Exception e) 
			{
				Log.exception(e);
				Log.msg("Fatal error - exiting",LogLevel.Basic);
				Log.die(e.getMessage());
			}
		
			//dumpStatusTable();
			int nSeqErr = 0;
			int nQualErr = 0;
			int totalClones = 0;
			for (Library lib : mLibs.values())
			{
				totalClones += lib.mSize;
				nSeqErr += lib.readErr();
				nQualErr += lib.qualErr();
			}
			
			if (nSeqErr + nQualErr > 0)
			{
				if (nSeqErr > 0)
					Log.indentMsg(nSeqErr + " sequence file problems found, correct problem and reload",LogLevel.Basic);
				if (nQualErr > 0)
					Log.indentMsg(nQualErr + " sequence file problems found, correct problem and reload",LogLevel.Basic);
				if (DBNewlyCreated)
				{
					Log.indentMsg("Removing database... ",LogLevel.Basic);
					mDB.executeUpdate("drop database "+ db);
				}
				System.exit(-1);
			}
			if (totalClones > 100000)
			{
				mDB.checkInnodbBufPool();	
			}
			
			Log.head("Load Data",LogLevel.Basic);
			for (String name : keepOrder) 
			//for (Library lib : mLibs.values())
			{
				Library lib = mLibs.get(name);
				if (lib.mAction == LibAction.ReLoad)
				{
					lib.delete();
				}
				if (lib.mStatus != LibStatus.UpToDate)
				{
					lib.doLoad();
					lib.matePairs();
				}
			}
			Log.newLine(LogLevel.Basic);
		} // if n2Load>0
		
		if (n2Update > 0)
		{
			Log.head("Update dataset properties",LogLevel.Basic);
			mDB.executeUpdate("update assem_msg set pja_msg='' where AID=1"); 
			for (Library lib : mLibs.values())
			{
				if (lib.mAction == LibAction.UpdateProperties)
				{
					lib.updateProperties();
				}
			}
			System.exit(0);
		}
		if (DBAlreadyExists) System.exit(0); 
		
		/* everything after here is new loads only...*/
		
		// Update the expression files and also the various expression counts for the expression libs in them. 
		// Obsolete: We don't just do them all because there might be multiple projects in the database, and it might take forever. 
		TreeSet<String> expLibs = new TreeSet<String>();
		for (Library lib : mLibs.values())
		{			
			if (lib.hasExp() && (lib.mExpOutOfDate || lib.mAction == LibAction.NewLoad 
					|| lib.mAction == LibAction.ReLoad))
			{
				lib.checkExp();
				lib.loadExp();
				expLibs.addAll(lib.mExpLibList);
			}
		}
		if (n2Load > 0 || expLibs.size() > 0)
		{
			updateCloneExp();
		}
		
		TreeSet<String> allLibs = new TreeSet<String>();
		allLibs.addAll(mLibs.keySet());
		allLibs.addAll(expLibs);
		fixLibSizes(allLibs);
		
		// Check the assemblies again as we may have deleted some
		int nAssems;
		rs = mDB.executeQuery("select count(*) as num from assembly");
		rs.first();
		nAssems = rs.getInt("num");
		if (nAssems > 0)
		{
			// order matters here!!
			// assemlibs must be updated before fixing assembly.ppx and the LN__ fields
			fixCtgCounts(allLibs);
			fixAssemLibCounts(allLibs);
			Schema.addCtgLibFields(mDB);
			fixRStatEtc();
		}
		
		Log.head("Finish Build Database for " + db + " at ",LogLevel.Basic);
		
		int nLoaded = 0;
		int nPairs = 0;
		
		for (Library lib : mLibs.values())
		{
			rs = mDB.executeQuery("select count(*) as count from clone where lid=" + lib.mID);
			rs.first();
			nLoaded += rs.getInt("count");
			rs = mDB.executeQuery("select count(*) as count from clone where lid=" + lib.mID + " and mate_CID > 0");
			rs.first();
			nPairs += rs.getInt("count")/2;			
		}
		
		Log.indentMsg("Loaded " + mLibs.size() + " datasets, " + nLoaded + " sequences", LogLevel.Basic);
		if (nPairs > 0)
		{
			Log.indentMsg(2*nPairs + " ESTs have mates",LogLevel.Basic);	
		}
		try {
			rs = mDB.executeQuery("select msg from assem_msg where AID=1");
			if (!rs.next())
				mDB.executeUpdate("insert assem_msg set msg='LibLoad', AID=1"); 
		}
		catch (Exception e) {System.out.println("Could not update database status");}
		
		Log.msg("\n" + Out.getMsgTimeMem(">>>End Load Data", startTime),LogLevel.Basic);
		Log.finish();
	}

	public static void printUsage()
	{
		System.err.println("Usage:  LoadLibMain <project> ");
		System.err.println("    The <project> directory must be under the 'libraries' directory");
		System.err.println("    A configuration file LIB.cfg must be located in this directory.");
		System.err.println("    Using the values in LIB.cfg, datasets will be loaded to the MySQL database.");
		System.exit(-1);
	}

	public static void loadLibrary(Properties p) throws Exception
	{
		String libid = p.getProperty("libid");
		Log.msg("Loading dataset:" + libid,LogLevel.Basic);

		File seqFile = new File(mLibDir, p.getProperty("seqfile"));
		File qualFile = null;

		if (!seqFile.isFile())
		{
			throw (new Exception("can't find sequence file "
					+ seqFile.getAbsolutePath()));
		} 
		Log.msg("found seq file " + seqFile.getAbsolutePath(),LogLevel.Basic);
		
		if (p.containsKey("qualfile"))
		{
			qualFile = new File(mLibDir, p.getProperty("qualfile"));
			if (!qualFile.isFile())
			{
				throw (new Exception("can't find qual file "
						+ qualFile.getAbsolutePath()));
			} 	
			Log.msg("found qual file " + qualFile.getAbsolutePath(),LogLevel.Basic);
		} 
		else
		{
			Log.msg("Qual file not specified; using default qual value "
					+ p.getProperty("default_qual"),LogLevel.Basic);
		}
	}
	
	static void dumpStatusTable()
	{
		Log.msg("--------- Summary of Datasets to be Loaded ---------",LogLevel.Basic);
		Log.columns(20, LogLevel.Basic,"Dataset", "Status", "Good Reads", "Avg Read Len",
				"Paired", "Short Reads", "Read Problem", "Qual Problem");
		for (Library lib : mLibs.values())
		{
			if (lib.mStatus != LibStatus.UpToDate)
			{
				Log.columns(20, LogLevel.Basic,lib.mIDStr, lib.mStatus.toString(),
						lib.mNGoodClone, lib.mAvgLen, lib.paired(),
						lib.mNShortSeq, lib.readErr(), lib.qualErr());
			}
		}
	}
	static void dumpLibraryStatus() throws Exception
	{		
		Log.columnsInd(20, LogLevel.Basic,"Datasets(#Reps)", "Action");
		Log.indentMsg("-------------------------------------------------",LogLevel.Basic);
		
		for (Library lib : mLibs.values())
		{
			if (lib.mStatus == LibStatus.UpToDate && lib.mAction == LibAction.NoAction) continue;
			Log.columnsInd(20, LogLevel.Basic,lib.idPlusReps(), lib.mAction.toString());
		}
	}
	// Fix up contig_counts and libsize entries.
	// Note, we just do it for all libraries, rather than trying to be clever about which ones were
	// actually modified.
	private static void fixCtgCounts(TreeSet<String> libs) throws Exception
	{
		ResultSet rs;

		for (String lib : libs)
		{
			// First make contig_counts entries if they aren't there already 
			rs = mDB.executeQuery("select count(*) as cnt from contig_counts where libid='" + lib + "'");
			rs.first();
			int count = rs.getInt("cnt");
			if (count == 0)
			{
				mDB.executeUpdate("insert into contig_counts select contigid, '" + lib + "',0 from contig");				
			}
			// First, set the counts from real clones in contigs
			mDB.executeUpdate("update contig_counts set count=(select count(*) from contclone " + 
							" join clone on clone.cid=contclone.cid where clone.libid='" + lib + "'" +
							" and contclone.contigid=contig_counts.contigid ) where contig_counts.libid='" + lib + "'");
			
			// Then, the clone_exp
			mDB.executeUpdate("update contig_counts,library set contig_counts.count=contig_counts.count+(select IFNULL(sum(count),0) from clone_exp " +
					" join contclone on contclone.cid=clone_exp.cid where contclone.contigid=contig_counts.contigid and clone_exp.lid=library.lid and clone_exp.rep=0)" +
					 " where contig_counts.libid='" + lib + "' and library.libid='" + lib + "' ");
		}	
	}
	private static void fixLibSizes(TreeSet<String> libs) throws Exception
	{
		ResultSet rs;
		for (String lib : libs)
		{
			mDB.executeUpdate("update library set libsize=(select count(*) from clone where clone.lid=library.lid) where library.libid='" + lib + "'");
			mDB.executeUpdate("update library set libsize=libsize+(select ifnull(sum(count),0) from clone_exp where clone_exp.lid=library.lid and clone_exp.rep=0) " + 
					" where library.libid='" + lib + "' ");
			

		}	
		rs = mDB.executeQuery("select libid from library where ctglib=0 and libsize=0");
		while(rs.next())
		{
			System.err.println("***WARNING:" + rs.getString("libid") + " has no loaded reads or expression data!");
		}
	}	
	// Make sure all these libs are in the assemlib table, for all the assemblies in the DB.
	// This obviously could be problematic for multi-assembly DB's, but we don't really
	// support that. 
	// Also, set some assemlib counts which we may not actually be using.
	private static void fixAssemLibCounts(TreeSet<String> libList) throws Exception
	{
		for (String libid : libList)
		{
			mDB.executeUpdate("insert ignore into assemlib (AID,LID,assemblyid,libid) " + 
					" (select AID, LID, assemblyid, libid from assembly, library " +
					" where library.libid='" + libid + "')");
			mDB.executeUpdate("update assemlib set singletons=( select count(*) from contig_counts " + 
    						" join contig on contig.contigid=contig_counts.contigid " + 
                           "  where contig_counts.libid='" + libid + "' and count > 0 and numclones = 1 " +
                           " and contig.aid=assemlib.aid and assemlib.libid='" + libid + "')");
		
			mDB.executeUpdate("update assemlib set contigs=( select count(*) from contig_counts " + 
    				" join contig on contig.contigid=contig_counts.contigid " +
                	" where contig_counts.libid='" + libid + "' and count > 0 and numclones > 1 " + 
                		" and contig.aid=assemlib.aid)");
			mDB.executeUpdate("update assemlib,library set assemlib.uniqueContigs= " +
				" (select  count(*)  from contig_counts as ctc " + 
    				" join contig on contig.contigid=ctc.contigid " +
                	" where ctc.count= " + 
                	"	(select sum(count) from contig_counts as ctc2 join library as lib2 on " +
                    "	lib2.libid=ctc2.libid where ctc2.contigid=ctc.contigid and lib2.ctglib=library.ctglib) " +  
               		" and ctc.libid='" + libid + "' ) " +
              	 " where library.libid='" + libid + "' and assemlib.libid='" + libid + "' ");
		}
	}	
	// Some of this code replicated in several places; search LN__ to find
	private static void fixRStatEtc() throws Exception
	{
		ResultSet rs;

		// Step 1. Make sure we have all the L__ columns for the libs
		Vector<String> Ls = new Vector<String>();
		Vector<String> LNs = new Vector<String>();

		Vector<String> libs = new Vector<String>();
		rs = mDB.executeQuery("select libid from library ");
		while (rs.next())
		{
			libs.add(rs.getString("libid"));
		}
		for (String libid : libs)
		{
			String col = "L__" + libid;
			String colN = "LN__" + libid;
			
			if (!mDB.tableColumnExists("contig", col))
			{
				mDB.executeUpdate("alter table contig add " + col + " bigint default 0");
				mDB.executeUpdate("alter table contig add " + colN + " float default 0.0");
			}
			String sql = "update contig,contig_counts set contig." + col + "=contig_counts.count where contig_counts.contigid=contig.contigid " +
			" and contig_counts.libid='" + libid + "'";
			
			mDB.executeUpdate(sql);
			Utils.setRPKM(mDB, libid, 2);
		}
		mDB.executeUpdate("update assembly set ppx=0");
		// Step 2: totalexp, totalexpN, and rstat
		
		// get just the expression libs
		Utils.singleLineMsg("Rstat...");
		rs = mDB.executeQuery("select library.libid,library.libsize from library " + 
				" join assemlib on assemlib.LID=library.LID where ctglib=0");
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
			mDB.executeUpdate("update contig set totalexp=(" +LSum + "), totalexpN=(" + LNSum + ")");
	
			// RStat. This will not be accurate for multi-assembly DBs, because there will
			// be extra L__ columns in the contig table, with spurious 0 values. 
			
			Stats rsObj = new Stats(libSizes.toArray(new Integer[1]));
			String lSel =  Utils.join(Ls, ",");
			rs = mDB.executeQuery("select " + lSel + ",CTGID from contig");
			int[] ctgCounts = new int[libSizes.size()];
			PreparedStatement ps = mDB.prepareStatement("update contig set rstat=? where CTGID=?");
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
	public static void updateCloneExp() throws Exception
	{
		// Replicate counts were loaded to clone_exp, but the overall counts (rep=0) need to 
		// be added and/or updated. 
		// Step 1: add the ones that aren't there already
		mDB.executeUpdate("insert ignore into clone_exp (select distinct CID, LID, 0, 0 " +
				" from clone_exp where rep > 0 )");

		// Step 2: update the counts
		mDB.executeUpdate("update clone_exp, " +
				" (select CID, LID, sum(count) as cnt " +
				" from clone_exp where rep > 0 group by CID,LID) as cntTbl " +
				" set clone_exp.count=cntTbl.cnt " +
				" where clone_exp.CID=cntTbl.CID and clone_exp.LID=cntTbl.LID and rep=0");
	}
}
