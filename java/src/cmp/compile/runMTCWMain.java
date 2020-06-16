package cmp.compile;

/*************************************************
 * 2/6/19 renamed CompileMain to runMTCWMain
 * Starts up runMultiTCW
 * - buildDatabase
 * - addNewMethod
 * - generate FastaFromDB
 * -- some routines to get DBConn
 */
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.Out;
import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.compile.panels.MethodBBHPanel;
import cmp.compile.panels.MethodOrthoMCLPanel;
import cmp.compile.panels.MethodClosurePanel;
import cmp.compile.panels.MethodLoadPanel;
import cmp.compile.panels.CompileFrame;
import cmp.database.Globals;
import cmp.database.Schema;

/** 
 * Starts up the Panel
 */
public class runMTCWMain {
	static public runMTCWMain cmpMain;
	public static String strGODB = "";
	public static boolean test=false, bWithPivot=false, bWoPivot=false;
	
	public static void main(String[] args) {
		try {
			System.out.println("runMultiTCW v" + Globalx.strTCWver + " " + Globalx.strRelDate);
			if (args.length>0 && (args[0].equals("-h") || args[0].contains("help"))) {
				System.out.println("runMultiTCW is typically run with no arguments.");
				System.out.println("However, the 'Closure' seeding with BBH algorithm can be replaced with:");
				System.out.println("  runMultiTCW -BHwop   #Use Bron_Kerbosch Without Pivot");
				System.out.println("  runMultiTCW -BHwp    #Use Bron_Kerbosch With Pivot");
				System.exit(0);
			}
			if (args.length>0) {
				if (args[0].equals("-test")) {
					test=true;
					System.out.println("In test mode");
				}
				if (args[0].equals("-BHwop")) {
					bWoPivot=true;
					System.out.println("Bron_Kerbosch without pivot");
				}
				else if (args[0].equals("-BHwp")) {
					bWithPivot=true;
					System.out.println("Bron_Kerbosch with pivot");
				}
			}
			ErrorReport.setErrorReportFileName(Globals.CmpErrorLog);
			runMTCWMain.run(args); // creates CompilePanel, which is the main panel
		} catch(Exception e) {
			ErrorReport.reportError(e);
		}
	}
	
	/**************************************************
	 * Select sTCW databases from interface
	 */
	public static void run(String [] args) throws Exception {
		cmpMain = new runMTCWMain();	
		hosts = new HostsCfg();
	}
	
	//Creates Compile Frame 
	public runMTCWMain() 
	{ 
		theFrame = new CompileFrame(this);
		theFrame.addWindowListener(new WindowListener() {

			public void windowActivated(WindowEvent arg0) {}
			public void windowClosed(WindowEvent arg0) {System.exit(0);}
			public void windowClosing(WindowEvent arg0) {}
			public void windowDeactivated(WindowEvent arg0) {}
			public void windowDeiconified(WindowEvent arg0) {}
			public void windowIconified(WindowEvent arg0) {}
			public void windowOpened(WindowEvent arg0) {}
		});
		theCompilePanel = theFrame.getCompilePanel();
		
		theFrame.pack();
		theFrame.setVisible(true);			
	}
	/***************************************************************
	 * Create Database
	 * Called by species panel when user clicks to create database
	 ***************************************************************/
	public boolean buildDatabase() {
		try {
			Out.PrtDateMsg("\nStart Build Database");
			Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.buildFile);
			
	   		long startTime = Out.getTime();
	   		
	   	// Create schema and enter Info table
			if (!validateMTCWdb()) return false;
				
			DBConn mDB = theCompilePanel.getDBconn();
		 	if (mDB==null) return false;
		 	
	   	// Assembly, hits and unitrans tables
			if (!new LoadSingleTCW(mDB, theCompilePanel).run()) return false;
			
		// Make combined file for blast
			generateFastaFromDB(theCompilePanel);
			
		// Add summary to info table
			Summary sumObj = new Summary(mDB);
			String text = sumObj.updateSummary();
			System.out.println(text);
			
    			mDB.executeUpdate("update info set annoState = " + quote("FINISHED"));
	   		mDB.close();
    			
	   		Out.PrtDateMsgTime("Complete build of " + theCompilePanel.getDBName(), startTime);
	   		Out.close();
	   		return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Failed build database");}
		Out.close();
		return false;
	}		
	/***************************************************************
	 * Create Database
	 * Called by species panel when user clicks to create database
	 ***************************************************************/
	public boolean buildGO() {
		try {
			Out.PrtDateMsg("\nStart Adding GOs");
			Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.buildFile);
			
	   		long startTime = Out.getTime();
	   		
	   		DBConn mDB = theCompilePanel.getDBconn();
		 	if (mDB==null) return false;
		 	
	   		if (!new LoadSingleGO(mDB, theCompilePanel).run()) return false;
	   		mDB.close();
			
	   		Out.PrtDateMsgTime("Complete adding GOs " + theCompilePanel.getDBName(), startTime);
	   		Out.close();
	   		return true;
		}
	   	catch (Exception e) {ErrorReport.prtReport(e, "Failed adding GOs");}
		Out.close();
		return false;
	}
	/***************************************************************
	 * Add new groups
	 ***************************************************************/
	public void addNewMethods () {		
		try {
			DBConn mDB = theCompilePanel.getDBconn();
		 	if (mDB==null) return;
			if (! checkProjectPath()) return;
			
			int cnt=0, fail=0;
			boolean rc=true;
							
			long startTime = Out.getTime();
			
			Out.createLogFile(theCompilePanel.getCurProjAbsDir(), Globals.methodFile);
			MethodPanel methodPanel = theCompilePanel.getMethodPanel();
			
			int num = methodPanel.getNumRows();
			String failed="";
			for(int x=0; x<num; x++) {
				String POGtype = methodPanel.getMethodTypeAt(x);
				
				String prefix = methodPanel.getMethodPrefixAt(x);
				if (theCompilePanel.getDBInfo().isReservedWords(prefix)) {
					String msg = "Prefix '" + prefix + "' is a MySQL resevered word. Please select a different prefix.";
					JOptionPane.showMessageDialog(theCompilePanel, msg,
							"Invalid prefix", JOptionPane.PLAIN_MESSAGE);
					Out.PrtError(msg);
					continue;
				}
				if(!methodPanel.isMethodLoadedAt(x)) {		
					if(POGtype.equals(MethodBBHPanel.getMethodType())) {
						rc = new MethodBBH().run(x, mDB, theCompilePanel);
						if (rc) cnt++; 
						else {fail++; failed += methodPanel.getMethodPrefixAt(x) + " ";}
					}					
					else if(POGtype.equals(MethodOrthoMCLPanel.getMethodType())) {
						rc = new MethodOrthoMCL().run(x, mDB, theCompilePanel);
						if (rc) cnt++; 
						else {fail++; failed += methodPanel.getMethodPrefixAt(x) + " ";}
					}
					else if(POGtype.equals(MethodClosurePanel.getMethodType())) {
						if (bWithPivot || bWoPivot) 
							 rc = new MethodClique().run(x, mDB, theCompilePanel, bWithPivot);
						else rc = new MethodClosure().run(x, mDB, theCompilePanel);
					
						if (rc) cnt++; else fail++;
					}
					else if(POGtype.equals(MethodBBHPanel.getMethodType())){
						rc = new MethodBBH().run(x, mDB, theCompilePanel);
						if (rc) cnt++; 
						else {fail++; failed += methodPanel.getMethodPrefixAt(x) + " ";}
					}
					else if(POGtype.equals(MethodLoadPanel.getMethodType())){
						rc = new MethodLoad(mDB).run(x, theCompilePanel);
						if (rc) cnt++; 
						else {fail++; failed += methodPanel.getMethodPrefixAt(x) + " ";}
					}
					else {
						System.err.println("Error: cannot load unidentified type '" + POGtype + "'");
					}
				}
			}
			if (cnt>0) {
				Summary sumObj = new Summary(mDB);
				sumObj.removeSummary();
				String text = sumObj.getMethodSizeTable();
				System.out.println("\nSummary\n" + text);
			}
			mDB.close();
			if (fail > 0) Out.PrtWarn(fail + " failed methods (" + failed + ")");
			Out.PrtDateMsgTime("Complete adding " + cnt + " methods for " + theCompilePanel.getDBName() + " at ", startTime);
			Out.close();
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Cannot add Cluster from main");
			Out.close();
			return;
		}
	}
	
	/***************************************************************
	 * Database routines
	 ***************************************************************/
	private boolean validateMTCWdb() {		
		try {
			Out.PrtSpMsg(0, "Checking database");
       		String dbName = theCompilePanel.getDBName();
       		Out.PrtSpMsg(1, "Database: " + dbName);
    			
			boolean doesDBExist = DBConn.checkMysqlDB("Validate mTCW ", hosts.host(), dbName, 
					hosts.user(), hosts.pass());
				
			if (!doesDBExist) 
				return (createMTCWdb(hosts.host(), dbName, hosts.user(), hosts.pass()));	
			
			if (Out.yesNo("Delete database and restart?")) {
				DBConn.deleteMysqlDB(hosts.host(), dbName, hosts.user(), hosts.pass());
				return (createMTCWdb(hosts.host(), dbName, hosts.user(), hosts.pass()));
			}
			else {
				Out.PrtWarn("User abort");
				return false;
			}
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Cannot open new multiTCW database");
			return false;
		}
	}
	
	private boolean createMTCWdb(String url, String dbName, String user, String pass) {
		try {
			DBConn.createMysqlDB(url, dbName, user, pass);
	
			DBConn mDB = new DBConn(url, dbName, user, pass);
	
			Out.PrtSpMsg(1, "Creating database");
			new Schema(mDB, Globals.VERSION, theCompilePanel.getCurProjAbsDir());
			mDB.close();
			
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Cannot create database");
			return false;
		}
	}
	
	// get a specific database connection
	static public DBConn getDBCstcw(CompilePanel pnl, int assemIndex) throws Exception {
		String dbName = pnl.getSpeciesDB(assemIndex);
		if (hosts.checkDBConnect(dbName))
			return hosts.getDBConn(dbName);
		else {
			String msg = "Database " + dbName + " does not exist";
			JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.PLAIN_MESSAGE);
			Out.PrtError(msg);
			System.err.println("Remove database, correct error, and then run again");
			return null;
		}
	}
	// called from SpeciesPanel
	public DBConn getDBCstcw(String dbName) throws Exception {
		if (hosts.checkDBConnect(dbName))return hosts.getDBConn(dbName);
		else return null;	
	}
	// get the mTCW connections -- should change everything to use theCompilePanel.getDBConn
	static public DBConn getDBConnection(CompilePanel pnl) throws Exception {
		String dbName = pnl.getDBName();		
		return hosts.getDBConn(dbName);
	}

	static public DBConn getDBConnection(String dbName) {
		return hosts.getDBConn(dbName);
	}
	// Database select panel -- should move this to it or hosts, or somewhere else
	static public DBConn getDBConnection(String host, String dbName) throws Exception {
		return hosts.getDBConn(dbName);
	}
	
	// May not be necessary anymore... 
	private boolean checkProjectPath() {
		String projPath = theCompilePanel.getCurProjAbsDir();
		if (projPath == null) {
			Out.PrtError("No current project " + projPath);
			return false;
		}
			
		File f = new File(projPath);
		if (!f.exists()) {
			Out.PrtError("Cannot find project directory " + projPath);
			return false;
		}
		return true;
	}
	
	
	/*****************************************
	 * generateFastaFromDB
	 */
	public static boolean generateFastaFromDB(CompilePanel cmpPanel) {
		try {
			Out.Print("\nCreate combined files");
			String blastDir = cmpPanel.getCurProjAbsDir() + "/" + Globals.CompilePanel.BLASTDIR;
			String aaFastaFile = blastDir + "/" + Globals.CompilePanel.ALL_AA_FASTA;  		
			Out.PrtSpMsg(1, "Create " + aaFastaFile);
			
			String ntFastaFile = blastDir + "/" + Globals.CompilePanel.ALL_NT_FASTA;  		
			Out.PrtSpMsg(1,"Create " + ntFastaFile);
			
			File testDir = new File(blastDir);
			if(!testDir.exists()) testDir.mkdir();
			
			// get datasets indexes
			DBConn mDBC = getDBConnection(cmpPanel);
			int cntDS = mDBC.executeCount("SELECT COUNT(*) FROM assembly");
			String [] asmPfx = new String [cntDS+1]; 
			ResultSet rs = mDBC.executeQuery("SELECT ASMid, prefix FROM assembly");		
			while(rs.next()) asmPfx[rs.getInt(1)] = rs.getString(2);
			
			int cntAllAA=0, cntAllNT=0;
			String strAA="", strNT="";
	        BufferedWriter pwAA = new BufferedWriter(new FileWriter (aaFastaFile));
	        BufferedWriter pwNT= new BufferedWriter(new FileWriter (ntFastaFile));
	        
	        for (int i=1; i<= cntDS;  i++) {
	        		int cntaa=0, cntnt=0;
	        		rs = mDBC.executeQuery("SELECT UTstr, aaSeq, ntSeq FROM unitrans WHERE ASMid=" + i);
				while(rs.next()) {
					String id = rs.getString(1);
					String aaseq = rs.getString(2);
					String ntseq = rs.getString(3);
					if (aaseq != null) {
						pwAA.write(">" + asmPfx[i] + "|" + id + "\n");
						pwAA.write(aaseq + "\n");
						cntAllAA++; cntaa++;
					}
					if (ntseq != null && !ntseq.trim().equals("")) { 
						pwNT.write(">" + asmPfx[i] + "|" + id + "\n");
						pwNT.write(ntseq + "\n");
						cntAllNT++; cntnt++;
					}
				}
				strAA += String.format(i + ":" + asmPfx[i] + ":" + cntaa + " ");
				strNT += String.format(i + ":" + asmPfx[i] + ":" + cntnt + " ");
	        }
	        pwAA.flush(); pwAA.close(); pwNT.flush(); pwNT.close();
	        Out.PrtSpMsg(2, "Wrote " + cntAllAA + " AA sequences (" + strAA + ")");
	        Out.PrtSpMsg(2, "Wrote " + cntAllNT + " NT sequences (" + strNT + ")");
			rs.close();  mDBC.close();	
			
			return true;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error generating fasta from database");
			return false;
		}
	}

	private String quote(String word) {
		return "\"" + word + "\""; 
	}
	
	/**************************************************************
	 *  private
	 ************************************************************/
	
	private CompileFrame theFrame = null;
	private CompilePanel theCompilePanel = null;
	public static HostsCfg hosts;
}