package cmp.compile.panels;

/**********************************************************
 * Calls the various other panels for display, plus defines
 * the actions for: 
 * 		Species: Create Database and Add/Edit Keep
 * 		Method:  Add New Method and Add/Edit Keep
 * 		Blast:   just calls it
 * 
 * Routines to read HOSTS.cfg and other database things
 * Routines to read mTCW.cfg (previous CPAVE.cfg) in order to populate this panel
 * Routines to get values from the other panels to pass on to other methods
 * Routines to update database with input from Edit panels
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.TCWprops;
import util.methods.Out;
import util.ui.UserPrompt;
import cmp.compile.runMTCWMain;
import cmp.compile.Summary;
import cmp.database.Globals;
import cmp.database.Version;
import cmp.database.DBinfo;

public class CompilePanel extends JPanel {
	private static final long serialVersionUID = 8981060324149115578L;
    private HostsCfg hosts; // was static
    
	public CompilePanel(CompileFrame parentFrame, Vector<String> hostList, HostsCfg h) {		
		hosts = h;
		
		theViewerFrame = parentFrame;
		theCompileMain = parentFrame.getParentMain();
		
		pnlMain = new JPanel();
		pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.PAGE_AXIS));
		pnlMain.setBackground(Globals.BGCOLOR);
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		// three replacement panels
		pnlEditSpecies = new EditSpeciesPanel(this);
		pnlEditSpecies.setVisible(false);
		pnlMain.add(pnlEditSpecies);
		
		pnlEditMethod = new EditMethodPanel(this);
		pnlEditMethod.setVisible(false);	
		pnlMain.add(pnlEditMethod);
		
		pnlEditBlast = new EditBlastPanel(this);
		pnlEditBlast.setVisible(false);	
		pnlMain.add(pnlEditBlast);
		
		pnlEditStats = new EditStatsPanel(this);
		pnlEditStats.setVisible(false);	
		pnlMain.add(pnlEditStats);
				
		// build the main panel
		pnlProject = new ProjectPanel(this);
		pnlMain.add(pnlProject);
		
		pnlSpecies = new SpeciesPanel(this,pnlEditSpecies);
		pnlMain.add(pnlSpecies);
		
		pnlBlast = new BlastPanel(this, pnlEditBlast);
		pnlMain.add(pnlBlast);
		
		pnlMethod = new MethodPanel(this, pnlEditMethod);
		pnlMain.add(pnlMethod);
	
		pnlStats = new StatsPanel(this, pnlEditStats);
		pnlMain.add(pnlStats);
		
		btnRunViewer = new JButton("Launch viewMultiTCW");
		btnRunViewer.setBackground(Globals.LAUNCHCOLOR);
		btnRunViewer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new RunCmd().viewMultiTCW(dbName);
			}
		});
		pnlMain.add(btnRunViewer);
		btnRunViewer.setEnabled(false);
		pnlMain.add(Box.createVerticalStrut(5));
		
		pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlMain.setMaximumSize(pnlMain.getMinimumSize());
		pnlMain.setPreferredSize(pnlMain.getMaximumSize());
		
		mainPane = new JScrollPane(pnlMain);
		theViewerFrame.getContentPane().add(mainPane);

		updateClearInterface();
	}
	
    /***************************************************************
     * Database routines
     */
	static public Vector<String> dbListFromHost(String hostURL, String user, String pass, 
	        String prefix) {
		Vector<String> retVal = new Vector<String> ();
		try {
			DBConn conn = new DBConn(hostURL, "", user, pass);
			ResultSet rs = conn.executeQuery("show databases");
				
			String dbName;
			while (rs.next()) {
				dbName = rs.getString(1);
				if (dbName.startsWith(prefix)) {
					retVal.add(dbName);
				}
			}
			if (conn != null) conn.close();
		} 
		catch (Exception err) {ErrorReport.die(err, "Error accessing database on " + hostURL); }
		return retVal;
	}

	private boolean dbCheckExists() {
		dbName = getDBName();
		if (dbName==null || dbName.equals("")) return false; // happens on Add Project
		dbExists = DBConn.checkMysqlDB("runMulti ", hosts.host(), dbName, hosts.user(), hosts.pass());
		if (!dbExists) {
			theInfo = null;
			return dbExists;
		}
		
		try {
			DBConn conn = new DBConn(hosts.host(), dbName, hosts.user(), hosts.pass());
			if (conn!=null) {
				boolean b = new Version().run(conn); // CAS310 moved this from mTCWcfgRead to here
				if (b) theInfo = new DBinfo(conn); 
				conn.close();
				if (!b) { // CAS310 added this; pretends it does not exist if not updated
					dbExists=false;
					theInfo = null;
					return false;
				}
			}
		} 
		catch (Exception err) {ErrorReport.die(err, "Error accessing database on " + dbName); }
		
		return dbExists;
	}
	public boolean dbIsExist() {
		return dbExists;
	}
	
	public DBConn getDBconn() {
		try {
			DBConn conn = new DBConn(hosts.host(), dbName, hosts.user(), hosts.pass());
			return conn;
		}
		catch (Exception err) {ErrorReport.die(err, "Error accessing database on " + dbName); }
		return null;
	}
	/*************************************************************
	 * methods to immediately update database
	 */
    public void dbAddMethodRemark(String prefix, String remark) {
    		if (!dbIsExist()) return;    	
		try {
			DBConn conn =  hosts.getDBConn(dbName);
			conn.executeUpdate("UPDATE pog_method SET description='" + remark + "' WHERE prefix='" + prefix + "'");
			new Summary(conn).removeSummary();
			conn.close();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error updating method");}
    }
    
    public void dbAddSpeciesRemark(String assembly, String remark) {
    		if (!dbIsExist()) return;
		try {
			DBConn conn =  hosts.getDBConn(dbName);
			if(conn.tableColumnExists("assembly", "remark"))
				conn.executeUpdate("UPDATE assembly SET remark='" + remark + "' WHERE ASMstr='" + assembly + "'");
			new Summary(conn).removeSummary();
			conn.close();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error updating remark");}
    }
    
    /********************************************************
     * get all prefixes from database -- called by  MethodPanel
     */
	public Vector<String> dbLoadedMethodPrefixes(String dbName) {
		Vector<String> retVal = new Vector<String> ();
		try {
			if (theInfo==null) return retVal;
			String [] pref = theInfo.getMethodPrefix();
			for (int i=0; i<pref.length; i++) retVal.add(pref[i]);		
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error dbLoadMethodPrefix");}
		return retVal;
	}
	/***********************************************************
	 * XXX The Main Functions -- 
	 * RunBlast and AddPairs are in BlastPanel - AddPairs calls updateAll
	 * Run Stats is in StatsPanel and calls updateAll
	 */
	public void execBuildDatabase() {//Called from SpeciesPanel
		if (!pnlProject.checkDBName()) {
			UserPrompt.showWarn("Incorrect database name");
			return; 
		}
		theCompileMain.buildDatabase();
		updateAll();
		if (theInfo!=null) theInfo.updateSTCW(); 
	}
	public void execBuildGO() {
		theCompileMain.buildGO();
	}
	public void execAddMethods() { // Called from MethodPanel
		theCompileMain.addNewMethods();
		updateAll();
		if (theInfo!=null) theInfo.updateMethod();
	}
	
	public void updateDeleteBlastDir(String blastDir) { // project still remains, just cleared blastdir
		//new File(blastDir).mkdir();
		pnlBlast.removedBlastDir();
	}
	public void updateAll() {
		dbCheckExists(); // re-created dbInfo, hence, gets updated.
		
		pnlProject.update(dbExists);
		pnlSpecies.update(dbExists);
		pnlBlast.update(dbExists);
		pnlMethod.update(dbExists);
		pnlStats.update(dbExists);
	
		btnRunViewer.setEnabled(dbExists);
		
		updateDisplay();
	}

	public void updateClearInterface() { // remove dbfiles or Select Select
		dbName="";
		dbExists=false;
		btnRunViewer.setEnabled(false);
		
		pnlProject.updateClearInterface();
		pnlSpecies.updateClearInterface();
		pnlBlast.updateClearInterface();
		pnlMethod.updateClearInterface();
		pnlStats.updateClearInterface();
		
		updateDisplay();
	}
	private void updateDisplay() {
		updateUI();
		repaint();
		revalidate();
		setVisible(false);
		setVisible(true);
	}
	public void setMainPanelVisible(boolean visible) {
		pnlProject.setVisible(visible);
		pnlSpecies.setVisible(visible);
		pnlBlast.setVisible(visible);
		pnlMethod.setVisible(visible);
		pnlStats.setVisible(visible);
		btnRunViewer.setVisible(visible);
	}
	
	public void resizedEvent(ComponentEvent e) {
		pnlSpecies.resizeEvent(e);
		pnlMethod.resizeEvent(e);		
		pnlMethod.setSize(pnlSpecies.getSize());
	}
	
	/*************** XXX Save/Load *********************************/
	/********************************************************
     * Read on new project and selecting an existing project mTCW.cfg
     */
	public boolean mTCWcfgRead() {
		try{
			updateClearInterface();
			
			File cfg = mTCWcfgGetFile(true);
			if(cfg == null) return false;
			
			Out.Print("\nLoading " + Out.mkPathRelative(Globals.PROJECTDIR, cfg.getAbsolutePath()));
			
			TCWprops props = new TCWprops(TCWprops.PropType.Cmp); 
			mTCWcfgClear(props);
			props.loadMTCWcfg(cfg);	
			
			mTCWcfgReadFile(props);
			
			updateAll(); 
			
			if(dbIsExist()) { 
				Out.Print("Database " + getDBName() + " exists ");
				pnlSpecies.updateSTCWtype(getDBconn(), theCompileMain);
			}
			else {
				Out.Print("Database " + getDBName() + " does not exists ");
				pnlSpecies.updateSTCWtype(null, theCompileMain);
			}
		} 
		catch(Exception e) {ErrorReport.reportError(e); return false;}
		return true;
	}
	private void mTCWcfgReadFile(TCWprops props) {
		try {
			if(props.containsKey("MTCW_db")) 
				pnlProject.setDBName(props.getProperty("MTCW_db"));
			else Out.PrtError("NO mTCW_db");
			
	 // update species panel
			boolean doneRead = false;
			for(int x=1; !doneRead; x++) {
				if(props.containsKey("STCW_db"+x)) {
					String dbName = props.getProperty("STCW_db"+x);
					if(!dbName.equals("-")) {
						String prefix = getPropertyVal(props, "STCW_prefix"+x);
						String stcwid = getPropertyVal(props, "STCW_id"+x);
						if (stcwid.equals("-")) stcwid = getPropertyVal(props, "STCW_STCWid"+x); // 1/21/19 backward compatiable
						if (stcwid.equals("-")) Out.PrtError("No STCWid for " + dbName);
						
						String remark = getPropertyVal(props, "STCW_remark"+x);
						
						pnlSpecies.addRow(dbName, stcwid, prefix, remark, false); 
					}
					else doneRead = true;
				}
				else doneRead = true;
			}
			pnlSpecies.updateTable();
			
		// update blast panel
			EditBlastPanel editPanel = pnlBlast.getEditPanel();
			editPanel.cfgFileDefaults();
			
			// aa selfblast
			int tp = Globals.CompilePanel.AA;
			String aa = props.getProperty("MTCW_search_file").trim();
			if (!aa.equals("")) {
				editPanel.cfgSetBlast(tp, aa);
				
				aa = props.getProperty("MTCW_DBsearch_pgm").trim();
				if (!aa.equals("")) editPanel.cfgSetPgm(tp, aa);
				
				aa = props.getProperty("MTCW_search_params").trim(); // Must be after cfgSetPgm
				if (!aa.equals("")) editPanel.cfgSetParams(tp, aa);
			}
			
			// nt selfblast
			tp = Globals.CompilePanel.NT;
			String runBlastNT = props.getProperty("MTCW_run_blastn"); 
			if (runBlastNT.equals("0")) editPanel.cfgFileNoBlastN();
			else {
				String nt = props.getProperty("MTCW_blastn_file").trim();
				if (!nt.equals("")) {
					editPanel.cfgSetBlast(tp, nt);
						
					nt = props.getProperty("MTCW_blastn_params").trim();
					if (!nt.equals("")) editPanel.cfgSetParams(tp, nt);
				}
			}
		// updates Method panel
			String method = "";
			doneRead = false;
			for(int x=1; !doneRead; x++) {
				if(props.containsKey("CLST_method_type"+x)) {
					method = props.getProperty("CLST_method_type"+x);
					if(!method.equals("")) {
						String comment = getPropertyVal(props, "CLST_comment"+x);
						String settings = getPropertyVal(props, "CLST_settings"+x);
						String methodPrefix = getPropertyVal(props, "CLST_method_prefix"+x);
					
						pnlMethod.addRow(method, methodPrefix, comment, settings);
					}
					else doneRead = true;
				}
				else doneRead = true;
			}
			pnlMethod.updateTable();
		} catch(Exception e) {ErrorReport.reportError(e);}
	}
	
	//Needed when id values aren't there, simply return empty string instead of exception
	private String getPropertyVal(TCWprops props, String key) {
		try {
			return props.getProperty(key);
		}
		catch(Exception e) {return "";}
	}
	
	private void mTCWcfgClear(TCWprops props) {
		try {
			if(props.containsKey("MTCW_db")) props.setProperty("MTCW_db", "");
			if(props.containsKey("MTCW_host")) props.setProperty("MTCW_host", "");
			if(props.containsKey("MTCW_blast_file")) props.setProperty("MTCW_blast_file", "");
			if(props.containsKey("MTCW_blast_params")) props.setProperty("MTCW_blast_params", "");
			if(props.containsKey("MTCW_blast_filter")) props.setProperty("MTCW_blast_filter", "");
			if(props.containsKey("MTCW_blastn_file")) props.setProperty("MTCW_blastn_file", "");
			if(props.containsKey("MTCW_blastn_params")) props.setProperty("MTCW_blastn_params", "");
			if(props.containsKey("MTCW_blastn_filter")) props.setProperty("MTCW_blastn_filter", "");
			
			boolean doneRead = false;
			for(int x=1; !doneRead; x++) {
				if(props.containsKey("CLST_method_name"+x)) {
					props.setProperty("CLST_method_name"+x, "");
					props.setProperty("CLST_method_type"+x, "");
					props.setProperty("CLST_method_prefix"+x, "");
					props.setProperty("CLST_file"+x, "");
					props.setProperty("CLST_comment"+x, "");
					props.setProperty("CLST_settings"+x, "");
				}
				else
					doneRead = true;
			}
			
			doneRead = false;
			for(int x=1; !doneRead; x++) {
				if(props.containsKey("STCW_db"+x)) {
					props.setProperty("STCW_db"+x, "-");
					props.setProperty("STCW_assem"+x, "");
					props.setProperty("STCW_host"+x, "");
					props.setProperty("STCW_AAFile"+x, "");
					props.setProperty("STCW_remark"+x, "");
				}
				else
					doneRead = true;
			}
		} catch(Exception e) {ErrorReport.reportError(e);}
	}	
	public boolean mTCWcfgNew() {
		try {
			File cfg = mTCWcfgGetFile(false);
			if (cfg==null) return false;
			
			PrintWriter out = new PrintWriter(cfg.getAbsoluteFile());
			out.print("MTCW_db = " + pnlProject.getDBName() + "\n");
			
			//String file = pnlBlast.getDefaultBlastDirRelPath() + "/" + Globals.CompilePanel.BLAST_AA_TAB;
			out.print("MTCW_blast_file = \n");
			out.close();
			
			return true;
		} catch(Exception e) {ErrorReport.reportError("Saving new mTCW.cfg", e); return false;}
	}
	// called by runMultiTCW interface and at end of Create Database to create mTCW.cfg
	public boolean mTCWcfgSave() {
		try{
			File cfg = mTCWcfgGetFile(false);	
			if (cfg==null) return false;
			
			// I added the copy because I think I lose mTCW.cfg sometimes somehow.
			// FileHelpers.copyFile(cfg, new File (cfg.getAbsoluteFile()+"~"));
			
			PrintWriter out = new PrintWriter(cfg.getAbsoluteFile());
			out.print("MTCW_db = " + pnlProject.getDBName() + "\n");
			
			EditBlastPanel editPanel = pnlBlast.getEditPanel();
			int tp = Globals.CompilePanel.AA;
			if (editPanel.isBlast(tp)) { // always on
				out.print("\n");
				// Always print search program 
				out.print("MTCW_DBsearch_pgm = " + editPanel.getSearchPgm(tp) + "\n");
				String file = editPanel.getBlastFileToProcess(tp);
				out.print("MTCW_search_file = " + file + "\n");
				
				if(!editPanel.getBlastParams(tp).equals(BlastArgs.getBlastpOptions()))
					out.print("MTCW_search_params = " + editPanel.getBlastParams(tp) + "\n");	
			}
			
			tp = Globals.CompilePanel.NT;
			if (editPanel.isBlast(tp)) {
				out.print("\n");
				String file = editPanel.getBlastFileToProcess(tp);
				out.print("MTCW_blastn_file = " + file + "\n");
				
				if(!editPanel.getBlastParams(tp).equals(BlastArgs.getBlastnOptions()))
					out.print("MTCW_blastn_params = " + editPanel.getBlastParams(tp) + "\n");
			}
			else out.print("MTCW_run_blastn = 0\n");
			
			out.print("\n");
			out.flush();
			int id = 1;
			for(int x=0; x<pnlSpecies.getNumRows(); x++) {
				out.print("STCW_db" + id + " = " + pnlSpecies.getDBNameAt(x) + "\n");
				out.print("STCW_prefix" + id + " = " + pnlSpecies.getPrefixAt(x) + "\n");
				out.print("STCW_id" + id + " = " + pnlSpecies.getSTCWidAt(x) + "\n");
				out.print("STCW_remark" + id + " = " + pnlSpecies.getRemarkAt(x) + "\n");	
				out.print("\n");
				id++;
			}
			out.flush();
			
			id = 1;
			for(int x=0; x<pnlMethod.getNumRows(); x++) {
				out.print("CLST_method_type" + id + " = " + pnlMethod.getMethodTypeAt(x) + "\n");
				out.print("CLST_method_prefix" + id + " = " + pnlMethod.getMethodPrefixAt(x) + "\n");
				out.print("CLST_comment" + id + " = " + pnlMethod.getCommentAt(x) + "\n");
				out.print("CLST_settings" + id + " = " + pnlMethod.getSettingsAt(x) + "\n");
				out.print("\n");
				id++;
			}
			out.close();
		
		} catch(Exception e) {
			ErrorReport.reportError(e);
			return false;
		}
		return true;
	}
	
	private File mTCWcfgGetFile(boolean read) {
		String projName = pnlProject.getCurProjName();
		if (projName==null) return null;
		String cfgFile = Globals.PROJECTDIR + "/" + projName + "/" + Globals.CONFIG_FILE;	
		File cfgFileObj = new File(cfgFile);
		
		if (read && ! cfgFileObj.exists()) {
			Out.PrtError("Could not find configuration file " + cfgFile);
			return null;
		}
		return cfgFileObj;
	}
	/**********************************************************
	 * Instead of having other methods have direct access to the Species and Method panels,
	 * they go through these get routines
	 */
	/*************** Gets *********************/
	public runMTCWMain getCompileMain() { return theCompileMain;}
	public ProjectPanel getProjectPanel() { return pnlProject; }
	public SpeciesPanel getSpeciesPanel() { return pnlSpecies; }
	public BlastPanel getBlastPanel() { return pnlBlast; }
	public StatsPanel getStatsPanel() { return pnlStats; }
	public MethodPanel getMethodPanel() { return pnlMethod; }
	public CompileFrame getParentFrame() { return theViewerFrame; }
	
	// Project
	public String getProjectName() { return pnlProject.getCurProjName(); }
	public String getDBName() { return pnlProject.getDBName(); }
	public void updateDBInfo() {
		
	}
	public DBinfo getDBInfo() {
		if (theInfo==null && dbExists) {
			Out.PrtWarn("Cari - dbinfo is null when dbExists");
			dbCheckExists();
		}
		return theInfo;
	}
	public int getNumNTdb() { if (theInfo!=null) return theInfo.nNTdb(); else return -1;}

	// Species
	public int getSpeciesCount() { return pnlSpecies.getNumRows(); }
	public String getSpeciesDB(int index) { return pnlSpecies.getDBNameAt(index); }
	
	public String getSpeciesSTCWid(int index)   { return pnlSpecies.getSTCWidAt(index); } 
		
	// file
	public String getCurProjAbsDir() { 
		File temp = new File(Globals.PROJECTDIR + "/" + pnlProject.getCurProjName() + "/");
		return temp.getAbsolutePath();
	}
	public String getCurProjRelDir() { 
		return Globals.PROJECTDIR + "/" + pnlProject.getCurProjName() + "/";
	}
	public String getCurProjMethodDir() { 
		try { // its made on startup, but just to be sure it wasn't removed.
			String methodDir = Globals.PROJECTDIR + "/" + pnlProject.getCurProjName() + 
					"/" + Globals.Methods.METHODDIR + "/";
			File f = new File(methodDir);
			if (!f.exists() || !f.isDirectory()) {
				f.mkdir();
				System.out.println("Create directory " + methodDir);
			}
			return methodDir;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "making method dir");}
		return "./error";
	}
	public HostsCfg getHosts() {return hosts;}
	
	// CAS310 moved this method from DBinfo to here
	// This uses mysql databases, so do not need a current one.
	public boolean isReservedWords(String w) { 
		try {
			DBConn mDB = new DBConn(hosts.host(), hosts.user(), hosts.pass());
			String ww = w.toLowerCase();
			HashSet <String> words = new HashSet <String> ();
			ResultSet rs = mDB.executeQuery("SELECT name FROM mysql.help_keyword");
			while (rs.next()) words.add(rs.getString(1).toLowerCase());
			if (words.size()==0) {
				Out.PrtError("Cannot read mysql.help_keyword");
				return false;
			}
			return words.contains(ww);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting reserved words");}
		return false;
	}
	
	/*******************************************************/
	private JScrollPane mainPane = null;
	private JPanel pnlMain = null;	

	private ProjectPanel pnlProject = null;
	private SpeciesPanel pnlSpecies = null;
	private MethodPanel pnlMethod = null;
	public  BlastPanel pnlBlast = null;
	public  StatsPanel pnlStats = null;
	private JButton btnRunViewer = null;
	
	private EditSpeciesPanel pnlEditSpecies = null;
	private EditMethodPanel pnlEditMethod = null;
	private EditBlastPanel pnlEditBlast = null;
	private EditStatsPanel pnlEditStats = null;
	
	private String dbName="";
	private boolean dbExists=false;
	private CompileFrame theViewerFrame = null;
	private runMTCWMain theCompileMain = null;
	private DBinfo theInfo=null;
}