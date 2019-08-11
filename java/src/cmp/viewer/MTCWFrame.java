package cmp.viewer;

/*****************************************************
 * viewMultiTCW frames
 */
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import cmp.compile.Summary;
import cmp.compile.panels.CompilePanel;
import cmp.database.DBinfo;
import cmp.database.Globals;
import cmp.viewer.groups.GrpQueryPanel;
import cmp.viewer.groups.GrpTablePanel;
import cmp.viewer.pairs.PairQueryPanel;
import cmp.viewer.pairs.PairTablePanel;
import cmp.viewer.panels.DatabaseSelectPanel;
import cmp.viewer.panels.MenuPanel;
import cmp.viewer.panels.ResultPanel;
import cmp.viewer.panels.TextPanel;
import cmp.viewer.seq.SeqsQueryPanel;
import cmp.viewer.seq.SeqsTopRowPanel;
import cmp.viewer.table.FieldData;

import util.database.DBConn;
import util.database.HostsCfg;
import cmp.database.Version;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.TCWEditorPane;
import util.ui.UIHelpers;

public class MTCWFrame extends JFrame {
	private static final long serialVersionUID = -4137848844345115643L;
	public static final int PAIR_MODE = 0;
	public static final int MULTI_MODE = 1;
	
	// this are searched on as name of left hand menu
	public static final String MENU_PREFIX = ">";
	public static final String SEQ_PREFIX = "Seq";
	public static final String GRP_PREFIX = "Clus";
	public static final String PAIR_PREFIX = "Pair";
	public static final String FILTER = "Filter: ";   // On each table with summary of filter
	   
	// buttons at top to other pages
	public static final String GRP_TABLE = "Clusters";
	public static final String SEQ_TABLE = "Seqs";
	public static final String SEQ_DETAIL = "Details";
	public static final String PAIR_TABLE = "Pairs";
	
	public static final String [] MAIN_SECTIONS = {"General", "Samples", "Filters", "Results"};
	private static final String [] MAIN_MENU = { 
		">Instructions", ">Overview", ">Clusters", ">Pairs", ">Sequences", 
		 ">Cluster", ">Pair", ">Sequence", ">List" };
	private static final String [] MENU_DESCRIP = { 
		"Basic instructions for using multiTCW", "Information about the database", 
		"Sample set of clusters", "Sample set of pairs", "Sample set of sequences", 
		 "Filter clusters", "Filter Pairs", "Filter sequences", "List all results" };

	private final String instructionHelp = "html/viewMultiTCW/Instructions.html";
	private final String overviewHelp = "html/viewMultiTCW/summary.html";
	
	private static Vector<MTCWFrame> openFrames = null;
	
	/***************************************************
	 * viewMultiTCW frame for database chooser
	 */
	public MTCWFrame() {
		openFrames = new Vector<MTCWFrame> ();
		openFrames.add(this);
		
		ErrorReport.setErrorReportFileName(Globals.CmpErrorLog);
		setBlast();
		hostsObj = new HostsCfg();
		
		theSettings = new ViewerSettings(this);
		setWindowSettings("chooseMultiTCW");
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		try {
			setTitle("viewMultiTCW " + InetAddress.getLocalHost().getHostName());

			mainPanel = Static.createPagePanel();
			DatabaseSelectPanel thePanel = new DatabaseSelectPanel(Globals.MTCW, null);
			
			mainPanel.add(thePanel);
			mainPanel.setMaximumSize(mainPanel.getPreferredSize());
			
			add(mainPanel);
			setBackground(theSettings.getFrameSettings().getBGColor());
			pack();	
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error building frame");}
	}
	// called by DatabaseSelectPanel on Exit and Exit All
	public static int getFrameCount () { return openFrames.size();}
	public static void closeAllFrames() {
		if(openFrames != null) {
			for(int x=0; x<openFrames.size(); x++)
				openFrames.get(x).dispose();
		}
	}
	
	public static void closeFrame(int frame) {
		if(openFrames != null) {
			openFrames.get(frame).dispose();
			openFrames.remove(frame);
		}
	}
	
	/***********************************************
	 * XXX viewMultiTCW <dbname>
	 */
	public MTCWFrame(String dbName, String [] args) {
		try {
			MyShutdown sh = new MyShutdown();
			Runtime.getRuntime().addShutdownHook(sh);
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			ErrorReport.setErrorReportFileName(Globals.CmpErrorLog);
			hostsObj = new HostsCfg();
			setBlast();
			strHostName = hostsObj.host();//java.net.InetAddress.getLocalHost().getHostName();
			strDBUser = hostsObj.user();//userData[0];
			strDBPass = hostsObj.pass();//userData[1];			
			
			if(!dbName.startsWith(Globals.MTCW + "_")) 
				dbName = Globals.MTCW + "_" + dbName;
			strDBName = dbName;
			
			if (!Globals.isRelease) System.err.println("Production version - not for release");
			
			if(!hostsObj.checkDBConnect(strDBName)) 
			{
				System.err.println("Could not connect to database " + strDBName + " on host " + hostsObj.host() + ", exiting. ");
				Vector<String> names = CompilePanel.dbListFromHost(hostsObj.host(), 
				        hostsObj.user(), hostsObj.pass(), Globals.MTCW);
				System.err.println("Available databases: ");
				for (String n : names) System.err.println("   " + n);
				System.exit(0);
			} 
			DBConn db = getDBConnection();
			if (db==null) {
				System.err.println("Fatal error: Could not open database");
				return;
			}
			new Version(db, false);
			
			if (args.length>1 && args[1].equals("-o")) {
				new Summary(db).removeSummary();
			}

			theSettings = new ViewerSettings(this);
			setWindowSettings("viewMultiTCW");
			
			setTitle("viewMultiTCW " + Globals.VERSION + " : " + dbName);
			buildFrame();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error finding server name");}
		catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error finding server name", null);}
	}
	/*********************************************************
	 * Called from DatabaseSelect and MTCWapplet()
	 */
	public MTCWFrame(String host, String dbName, boolean isApplet, String dbUser, String dbPass) {
	try {
		if(openFrames != null) openFrames.add(this);
		
		ErrorReport.setErrorReportFileName(Globals.CmpErrorLog);
		
		bIsApplet = isApplet;
		UIHelpers.setIsApplet(isApplet);
		if (!isApplet) 
		{
			hostsObj = new HostsCfg();
			dbUser = hostsObj.user();
			dbPass = hostsObj.pass();
		}		
		if(!DBConn.connectionValid(host, dbName,dbUser, dbPass)) {
			System.out.println("Error: Database cannot be found, exiting. ");
			System.out.println(host + " " + dbName + " " + dbUser);
			if (isApplet) return;
			else System.exit(-1);
		}
		strDBName = dbName;
		strHostName = host;
		strDBUser = dbUser;
		strDBPass = dbPass;
		if (!isApplet) {
			new Version(getDBConnection(), false);
		}
		
		//Loads the profile, if none selected creates a default profile
		theSettings = new ViewerSettings(this);
		setWindowSettings("viewMultiTCW");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setTitle("viewMultiTCW " + Globals.VERSION + ": " + dbName);
		buildFrame();
	} catch(Exception e) {ErrorReport.prtReport(e, "Error building frame");}
	}
	/*******************************************************************
	 * BuildFrame
	 */
	private void buildFrame() {
		try {
			Out.PrtSpMsg(0, "Initialize...");
			
			theSettings.setProfileToDefault();	
			setMinimumSize(new Dimension(theSettings.getFrameSettings().getFrameWidth(), 
					theSettings.getFrameSettings().getFrameHeight()));
			setSize(new Dimension(theSettings.getFrameSettings().getFrameWidth(), 
					theSettings.getFrameSettings().getFrameHeight()));
		
			Out.PrtSpMsg(1, "Get metadata from database");
			theInfo = new DBinfo(getDBConnection()); 
			
			Out.PrtSpMsg(1, "Build interface");
			createViewerFrame();
			Out.PrtSpMsg(0, "Viewing "  +  strDBName);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error building frame");}
		catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error building frame", this);}
	}
	
	private void createViewerFrame() {
		createMainPanel();

		txtStatus = new JTextField();
		txtStatus.setEditable(false);
		txtStatus.setBorder(BorderFactory.createEmptyBorder());
		txtStatus.setBackground(theSettings.getFrameSettings().getBGColor());
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(theSettings.getFrameSettings().getFrameWidth()/5);

        splitPane.setBorder(null);
        splitPane.setRightComponent(mainPanel);
        splitPane.setLeftComponent(sPane);
        
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        add(txtStatus, BorderLayout.SOUTH);
        
        setVisible(false);
        setVisible(true);
	}
	private void createMainPanel() {
		mainPanel = Static.createPagePanel();
		
		menuSelectListener = new ActionListener() {//don't remove, needs non-null value
			public void actionPerformed(ActionEvent arg0) {
			}
		};
		menuCloseListener = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(arg0.getSource() instanceof JButton) {
					JPanel curPanel = menuPanel.getSelectedPanel();
					JPanel remPanel = menuPanel.getMenuItem((JButton)arg0.getSource());
					
					removePanel(remPanel);
					
					if(curPanel == remPanel)menuPanel.setSelected(resultPanel);					
				}
			}
		};
		menuPanel = new MenuPanel(this, menuSelectListener, menuCloseListener);
		
		instructionsPanel = new StyleTextPanel(instructionHelp);
		overviewPanel = new TextPanel(getOverviewText(), false, overviewHelp);
		
		FieldData.setState(theInfo);
		grpTablePanel = new GrpTablePanel(this, MAIN_MENU[2]);
		pairTablePanel = new PairTablePanel(this, MAIN_MENU[3]);
		seqTablePanel = new SeqsTopRowPanel(this, MAIN_MENU[4]);
		grpQueryPanel = new GrpQueryPanel(this);
		pairQueryPanel = new PairQueryPanel(this);
		seqQueryPanel = new SeqsQueryPanel(this);
		resultPanel = new ResultPanel(this);

		JPanel general = Static.createRowPanel();
		menuPanel.addTopItem(general, MAIN_SECTIONS[0], "General: Select a > item");
		menuPanel.addMenuItem(general, instructionsPanel, MAIN_MENU[0], MENU_DESCRIP[0]);
		menuPanel.addMenuItem(general, overviewPanel, MAIN_MENU[1], MENU_DESCRIP[1]);
		
		JPanel list = Static.createRowPanel();
		menuPanel.addTopItem(list, MAIN_SECTIONS[1], "Sample: Select a > item");
		menuPanel.addMenuItem(list, grpTablePanel, MAIN_MENU[2], MENU_DESCRIP[2]);
		menuPanel.addMenuItem(list, pairTablePanel, MAIN_MENU[3], MENU_DESCRIP[3]);
		menuPanel.addMenuItem(list, seqTablePanel, MAIN_MENU[4], MENU_DESCRIP[4]);
		
		JPanel filter = Static.createRowPanel();
		menuPanel.addTopItem(filter, MAIN_SECTIONS[2], "Filter: Select a > item");
		menuPanel.addMenuItem(filter, grpQueryPanel, MAIN_MENU[5], MENU_DESCRIP[5]);
		menuPanel.addMenuItem(filter, pairQueryPanel, MAIN_MENU[6], MENU_DESCRIP[6]);
		menuPanel.addMenuItem(filter, seqQueryPanel, MAIN_MENU[7], MENU_DESCRIP[7]);
		
		JPanel result = Static.createRowPanel();
		menuPanel.addTopItem(result, MAIN_SECTIONS[3], "Result: Select a > item");
		menuPanel.addMenuItem(result, resultPanel, MAIN_MENU[8], MENU_DESCRIP[8]);
		
		//Add all panels to the main
		mainPanel.add(instructionsPanel);
		mainPanel.add(overviewPanel);
		mainPanel.add(grpTablePanel);
		mainPanel.add(pairTablePanel);
		mainPanel.add(seqTablePanel);
		mainPanel.add(grpQueryPanel);
		mainPanel.add(pairQueryPanel);
		mainPanel.add(seqQueryPanel);
		mainPanel.add(resultPanel);
		menuPanel.setSelected(overviewPanel);
		
		sPane = new JScrollPane(menuPanel);
	}

	/*****************************************************
	 * Database methods 
	 */
	public DBConn getDBConnection() throws Exception
	{
		return new DBConn(strHostName, strDBName, strDBUser, strDBPass);
	}
	// Used for the big queries from filters so only one can run at a time
	static public ResultSet executeQuery(DBConn conn, String query, JTextField updateField) throws Exception {
		double x = 0;
		while(curResult != null) {
			Thread.sleep(100);
			x += .1;
			if(updateField != null) 
				updateField.setText("Waiting for DB to be available (" + x + ") seconds lapsed");
		}
		curResult = conn.executeQuery(query);
		return curResult;
	}
	static public void closeResultSet(ResultSet rset) throws Exception {
		rset.close();
		curResult = null;
	}
	private String getOverviewText() {
		try {
			DBConn conn = getDBConnection(); 
			String text= new Summary(conn).getSummary();
			conn.close();
			return text;
		} catch (Exception e) {ErrorReport.reportError(e, "Error loading overview");}
		return null;
	}
	
	/**********************************************************************/
	public ViewerSettings getSettings() { return theSettings; }
	
	public void setSelection(JPanel panel) {menuPanel.setSelected(panel);}
	
	public GrpQueryPanel getGrpQueryPanel() {return grpQueryPanel;}
	public PairQueryPanel getPairQueryPanel() {return pairQueryPanel;}
	public SeqsQueryPanel getSeqsQueryPanel() {return seqQueryPanel;}
	
	public String [] getMethodPrefixes() { return theInfo.getMethodPrefix(); }
	public int getMethodIDForName(String name) { return theInfo.getMethodID(name);}
	public String [] getAsmList() { return theInfo.getASM(); }
	public String [] getTaxaList() { return theInfo.getTaxa(); }
	public String [] getSeqLibList() { return theInfo.getSeqLib(); }
	public String [] getSeqDEList() { return theInfo.getSeqDE(); }
	public boolean 	hasNegDE() {return theInfo.hasNegDE();}
	public int 		getnAAdb() {return theInfo.nAAdb();}
	public int 		getnNTdb() {return theInfo.nNTdb();}
	public DBinfo getInfo() {
		if (theInfo==null) Out.PrtError("The metadata is not initialized");
		return theInfo;
	}
	
	public int getNextLabelNum(String prefix) {
		for(int x=1; true; x++) {
			if(!menuPanel.doesNodeLabelBeginWith(prefix + x))
				return x;
		}
	}

	public void setStatus(String status) {txtStatus.setText(status);}

	// Called from Tables to add tab beneath current tab
	public void addResultPanel(JPanel parentPanel, JPanel newPanel, String name, String summary) {
		mainPanel.add(newPanel);
		menuPanel.addChildItem(parentPanel, newPanel, name, summary);
		resultPanel.addResult(parentPanel, newPanel, name, summary);
	}
	// Called from Queries to add tab beneath results
	public void addResultPanel(String type, JPanel newPanel, String name, String summary) {
		JPanel parent = resultPanel;
		if (type.equals(GRP_PREFIX)) parent = grpQueryPanel;
		else if (type.equals(PAIR_PREFIX)) parent = pairQueryPanel;
		else if (type.equals(SEQ_PREFIX)) parent = seqQueryPanel;
		mainPanel.add(newPanel);
		menuPanel.addChildItem(parent, newPanel, name, summary);
		resultPanel.addResult(null, newPanel, name, summary);
	}
	public void changePanelName(JPanel sourcePanel, String newName, String summary) {
		menuPanel.renameMenuItem(sourcePanel, newName);
		resultPanel.renamePanel(sourcePanel, newName, summary);
	}
	
	public void removePanel(JPanel panel) {
		mainPanel.remove(panel);
		menuPanel.removeMenuItem(panel);
		resultPanel.removePanel(panel);
	}
	
	public void removePanel(JButton closeButton) {
		removePanel(menuPanel.getMenuItem(closeButton));
	}
	
	public void removePanelFromMenuOnly(JPanel panel) {
		menuPanel.removeMenuItem(panel);
	}
	
	
	/******************************************************************/
	private class StyleTextPanel extends JPanel
	{
		private static final long serialVersionUID = 1043240071484012170L;

		public StyleTextPanel ( String strURL )
		{
			if (!strURL.startsWith("/")) strURL = "/" + strURL;
			URL url = null;
			TCWEditorPane editorPane = null;
			try {
				url = StyleTextPanel.class.getResource(strURL);
	
				editorPane = new TCWEditorPane(url);
			}
			catch(Exception e) {
				e.printStackTrace();
		    	url = null;
			}
			if (url == null)
				editorPane.setText("Failed to load the instructions file ");

			JScrollPane scrollPane = new JScrollPane(editorPane);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
			add(scrollPane);
		}
	}
	private void setBlast() {
		if (blastIsSet) return;
		blastIsSet = true;
		try {
			new File("HOSTS.cfg");
			new HostsCfg();
		}
		catch(Exception e) {System.err.println("Error reading HOSTS.cfg"); }

	}
	private void setWindowSettings(final String prefix) {
		// Load window dimensions from preferences
		Preferences userPrefs = Preferences.userRoot();
		int nX = userPrefs.getInt(prefix + "_frame_win_x", Integer.MAX_VALUE);
		int nY = userPrefs.getInt(prefix + "_frame_win_y", Integer.MAX_VALUE);
		int nWidth = userPrefs.getInt(prefix + "_frame_win_width", Integer.MAX_VALUE);
		int nHeight = userPrefs.getInt(prefix + "_frame_win_height", Integer.MAX_VALUE);
		if (nX == Integer.MAX_VALUE) {
			UIHelpers.centerScreen(this);
		} else
			setBounds(nX, nY, nWidth, nHeight);

		// Setup to save window dimensions when it is closed
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					// Save the window dimensions to the preferences
					Preferences userPrefs = Preferences.userRoot();
					userPrefs.putInt(prefix + "_frame_win_x", getX());
					userPrefs.putInt(prefix + "_frame_win_y", getY());
					userPrefs.putInt(prefix + "_frame_win_width", getWidth());
					userPrefs.putInt(prefix + "_frame_win_height", getHeight());
					userPrefs.flush();
					
					if ((openFrames==null || openFrames.size() == 1) && !UIHelpers.isApplet()) {
						System.exit(0);
					} 

				} catch (Exception err) {ErrorReport.reportError(err, "Error: initializing");}
			}
		});
	}
	public String getDBName() { return strDBName; }	
	public boolean isApplet() { return bIsApplet; }
	
	private class MyShutdown extends Thread { // runs at program exit for each JFrame
		public void run() {
			shutdown();
		}
	}
	private synchronized void shutdown() {
		try {
			if(curResult != null) {
				try {
					curResult.close();
					curResult = null;
				}
				catch(Exception e) {}
			}
			if(openFrames != null && openFrames.size()==1) openFrames.get(0).dispose();
			if (!strDBName.equals("")) System.out.println("Terminating viewMultiTCW for " + strDBName);
			else System.out.println("Terminating viewMultiTCW");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error shuting down");}
	}
	static public void shutDownFromApplet() {
		if(curResult != null) {
			try {
				curResult.close();
				curResult = null;
			}
			catch(Exception e) {}
		}
	}
	private JSplitPane splitPane = null;
	private MenuPanel menuPanel = null;
	private JPanel mainPanel = null;
    private JScrollPane sPane = null;

	private JTextField txtStatus = null;
	
	private ActionListener menuSelectListener = null;
	private ActionListener menuCloseListener = null;
		
	private ViewerSettings theSettings = null;
	private DBinfo theInfo = null;
	
	private static ResultSet curResult = null;
	
	private boolean blastIsSet = false, bIsApplet = false;
	
	private HostsCfg hostsObj=null;
	private String strHostName = "", strDBName = "", strDBUser = "", strDBPass = "";
	
	//Individual content panels
	private StyleTextPanel instructionsPanel = null;
	private TextPanel overviewPanel = null;
	private GrpTablePanel grpTablePanel = null;
	private PairTablePanel pairTablePanel = null;
	private SeqsTopRowPanel seqTablePanel = null;
	private GrpQueryPanel grpQueryPanel = null;
	private SeqsQueryPanel seqQueryPanel = null;
	private PairQueryPanel pairQueryPanel = null;
	private ResultPanel resultPanel = null;
}
