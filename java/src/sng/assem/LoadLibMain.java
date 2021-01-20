package sng.assem;

import java.io.File;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Vector;
import java.sql.ResultSet;

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
import util.methods.Out;
import util.methods.ErrorReport;
/***************************************************
 * Main library loading class. called by execLoadLib and runSingleTCW
 * CAS304
	replaced execLoadLib.pl with execLoadLib, so args parsed here
 	broke this into separate function and rearranged to be logical
 	the 'update' no longer works, so removed it
**/
public class LoadLibMain
{
	static private boolean mNoPrompts = false;
	static private TCWprops mProps;
	static private File mLibDir = null;
	static private String mProj;
	static private DBConn mDB;
	static private TreeMap<String, Library> mLibs;
	static private Vector <String> keepOrder;
	
	static private int mDefQual = 20;
	static private String dbName=null;

	public LoadLibMain(){}
	
	public static void main(String[] args)
	{
		Version.printTCWversion();
		try {
			new LoadLibMain();
			LoadLibMain.run(true, args);
		} 
		catch (Exception e) {   
			Log.die("Load Library main method");
		}
	}

	private static void run(boolean exitWhenComplete, String[] args)
	{
		if (args.length == 0 || args.length > 3 || 
				Utils.hasOption(args,"-h") || Utils.hasOption(args, "--help") || Utils.hasOption(args, "-help")) printUsage();
			
		if (Utils.hasOption(args, "-n")) mNoPrompts = true;
		
		mProj = args[0];

		long startTime = Out.getTime();
										// everything dies if there is a problem
		new Utils(); 					// initialize the class static members
		checkDirectories(); 			// sets mLibDir
		setLog();						// open log file
		loadCfg();  					// sets mProps
		checkDB();						// set mDB, dbName, DBalreadyExists, DBnewlyCreated
		
		getLibs();						// set mLibs, keepOrder, mExpFiles
		loadLibs();
		
		finishDB();						// set database library sizes
		
		Log.msg("\n" + Out.getMsgTimeMem(">>>End Load Data for " + dbName, startTime),LogLevel.Basic);
		Log.finish();
	}
	
	private static void checkDirectories() {
		// FileHelpers.mergeDir(Globals.OLDLIBDIR, Globalx.PROJDIR, true); CAS314 don't need anymore
		String libDir = Globalx.PROJDIR;
		String[] libdirs = { libDir, "../" + libDir, "../../" + libDir };
		boolean found = false;
		File f = null;
		for (String lib : libdirs){
			f = new File(lib);
			if (f.isDirectory()) {
				found = true;
				break;
			}
		}
		if (!found) Log.die("Unable to find the '" + libDir + " directory.");

		File p = new File(f.getAbsolutePath() + "/" + mProj);
		if (p.isDirectory()) mLibDir = p;
		else Log.die("Unable to find the directory '" + mProj + "' under the " + libDir + " directory");
	}
	
	private static void setLog() {
		try {
			File logDir =  new File(mLibDir, Globalx.logDir);
			if (!logDir.exists()) {
				System.err.println("Creating project log directory ...");
				if (!logDir.mkdir())  {
					System.err.println("*** Failed to create project log directory '" +  logDir.getAbsolutePath() + "'.");
					return;
				}
			};
			
			File logFile = new File(logDir, Globals.loadFile);

			new Log(logFile);
			Log.setDebugLog(logFile);

			Log.addLogAction(LogLevel.Basic, LogAction.Terminal);
			Log.addLogAction(LogLevel.Basic, LogAction.Log);
			Log.addLogAction(LogLevel.Basic, LogAction.DebugLog);
			Log.addLogAction(LogLevel.Detail, LogAction.DebugLog);	
			
			ErrorReport.setErrorReportFileName(Log.errFile);
		}
		catch (Exception e) {e.printStackTrace(); Log.die("Cannot create log file");}
	}
	
	private static void loadCfg() {
		String logFile = mLibDir.getAbsolutePath() + "/" + Globals.LIBCFG;
		try {
			mProps = new TCWprops(TCWprops.PropType.Lib);
			Utils.mProps = mProps;
			if (Utils.isDebug()) Log.addLogAction(LogLevel.Detail, LogAction.Terminal);	
	
			File c = new File(logFile);
			if (!c.isFile()) 
				Log.die("Unable to find LIB.cfg under the '" + mProj + "' directory");
			
			mProps.load(c);
		} 
		catch (Exception e) {
			Log.die(e, "Problem loading config file " + logFile);				
		} 
	}
	private static void checkDB() {
		try {
			Log.head("Check database",LogLevel.Basic);
			
			dbName = mProps.getProperty("STCW_db");
			if (dbName==null) Log.die("Missing STCW_db in LIB.cfg");
				
			HostsCfg hosts = new HostsCfg();
		
			boolean exists = DBConn.checkMysqlDB("Load Data ", hosts.host(), dbName, hosts.user(),hosts.pass());	
			if (exists)
			{
				if (DBConn.oldDBisEmpty(hosts.host(),dbName,hosts.user(),hosts.pass()))
				{
					Log.indentMsg("Delete empty database " + dbName + " on host " + hosts.host(),LogLevel.Basic);
					DBConn.deleteDB(hosts.host(), dbName, hosts.user(), hosts.pass());				
				}
				else if (mNoPrompts) { // CAS304 add check
					Log.indentMsg("Delete existing database " + dbName + " on host " + hosts.host(),LogLevel.Basic);
					DBConn.deleteDB(hosts.host(), dbName, hosts.user(), hosts.pass());	
				}
				else if (Utils.yesNo("Database " + dbName + " already exists. Do you want to delete it ")) {
					if (Utils.yesNo("Database " + dbName + " will be deleted. Are you sure ")){
						Log.indentMsg("Delete empty database " + dbName + " on host " + hosts.host(),LogLevel.Basic);
						DBConn.deleteDB(hosts.host(), dbName, hosts.user(), hosts.pass());
					}
					else Log.die("Abort - database exists");
				}
			}
				
			Log.indentMsg("Creating database " + dbName + " on host " + hosts.host(),LogLevel.Basic);
			DBConn.createMysqlDB(hosts.host(),dbName,hosts.user(),hosts.pass());
				
			mDB = hosts.getDBConn(dbName);
			Schema schObj = new Schema(mDB);
			schObj.loadSchema();
		}
		catch (Exception e) {Log.die(e, "Checking/creating database " + dbName);}
	}
	/**************************************************
	 * Get Libs from LIB.cfg
	 */
	private static void getLibs() {
		try {
			Log.head("Read libraries from LIB.cfg",LogLevel.Basic);

			mLibs = new TreeMap<String, Library>();
			keepOrder = new Vector<String> ();
			TreeSet<String> dupNameList = new TreeSet<String>();
			for (Properties prop : mProps.mLibProps)
			{
				Library lib = new Library(mDB, prop, mLibDir); // dies if any problem
				
				String name = lib.mIDStr.toLowerCase(); // CAS304 database is case-insensitive
				if (dupNameList.contains(name))
					Log.die("SeqID or Condition '" + name + "' is not unique -- it must be unique");
				dupNameList.add(name);
				
				mLibs.put(lib.mIDStr, lib);
				keepOrder.add(lib.mIDStr);
			}
			if (mLibs.size()==0) Log.die("No libraries in LIB.cfg");
			
			// Dup rep?
			dupNameList.clear();
			for (Library lib : mLibs.values()) {
				for (String libName : lib.getAllReps()) {
					if (dupNameList.contains(libName))
						Log.die("Dataset name " + libName + " is not unique -- it must be unique");
					else 
						dupNameList.add(libName); // CAS304 was not being added
				}
			}
			dupNameList.clear();
			
		// get Quality files
			boolean anyQuals = false;
			for (Library lib : mLibs.values()) {
				if (lib.mQualFile != null) {
					anyQuals = true;
					break;
				}
			}
			mDefQual = (anyQuals ? 18 : 20);
			
			if (anyQuals) Log.indentMsg("Some quality files found: using default 18 for missing qual values", LogLevel.Basic);
			else          Log.indentMsg("No quality files supplied: using default 20 for all qual values", LogLevel.Basic);
			
			for (Library lib : mLibs.values()) lib.mDefQual = mDefQual;
		}
		catch (Exception e) {Log.die(e, "Getting libraries ");}
	}
	/**************************************************
	 * Verify sequence and quals, Load libs into database, then lad sequence and quals
	 */
	private static void loadLibs() {
		try {		
			Log.head("Verify files"); // dies if fatal error
				
			// Load library into DB before verify
			for (String name : keepOrder) {
				Library lib = mLibs.get(name);
				lib.loadLibIntoDB();
			}
						
			HashSet<String> seqNames = new HashSet<String>();
			for (String name : keepOrder) {
				Library lib = mLibs.get(name);
				if (lib.hasSeqFile()) 
					lib.scanAllFiles(seqNames);
			}
						
			Log.head2("Load all files");
			for (String name : keepOrder) {
				Library lib = mLibs.get(name);
				if (lib.hasSeqFile()) 
					lib.loadAllFiles();
			}
		}
		catch (Exception e) {Log.die(e, "Load new libraries ");}
	}
	
	private static void finishDB() {
		try {
			ResultSet rs;
			for (String libid : mLibs.keySet())
			{
				int lid = mLibs.get(libid).mLID;
				if (mLibs.get(libid).mNGoodSeq>0) { // seq lib
					int nSeq = mDB.executeCount("select count(*) from clone where LID=" + lid);
					mDB.executeUpdate("update library set libsize=" + nSeq + " where LID=" + lid);
				}
				else { // exp lib
					int nCnt = mDB.executeCount("select sum(count) from clone_exp where rep=0 and LID=" + lid);	
					mDB.executeUpdate("update library set libsize=" + nCnt + " where LID=" + lid);
				}
			}
				
			// sanity check
			rs = mDB.executeQuery("select libid from library where ctglib=0 and libsize=0");
			while(rs.next())
				Log.warn(rs.getString(1) + " has no expression data!");
		
			rs = mDB.executeQuery("select msg from assem_msg where AID=1");
			if (!rs.next()) mDB.executeUpdate("insert assem_msg set msg='LibLoad', AID=1"); 
		}
		catch (Exception e) {Log.die(e, "Could not update database status");}
	}
	
	private static void printUsage()
	{
		System.err.println("Usage:  LoadLibMain <project> [optional flags]");
		System.err.println("    The <project> directory must be under the 'project' directory");
		System.err.println("    A configuration file LIB.cfg must be located in this directory.");
		System.err.println("    Using the values in LIB.cfg, datasets will be loaded to the MySQL database.");
		System.err.println("Optional flags:");
		System.err.println("    -n no prompts");
		System.exit(-1);
	}
}
