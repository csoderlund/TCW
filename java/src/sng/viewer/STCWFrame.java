package sng.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.util.Iterator;
import java.util.Vector;

import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import sng.database.DBInfo;
import sng.database.Globals;
import sng.database.MetaData;
import sng.database.Overview;
import sng.dataholders.*;
import sng.util.FieldMapper;
import sng.util.HiddenTabbedPane;
import sng.util.InterThreadProgress;
import sng.util.MenuTree;
import sng.util.MenuTreeNode;
import sng.util.RunQuery;
import sng.util.StyleTextTab;
import sng.util.Tab;
import sng.util.TextTab;
import sng.util.MenuTreeNode.MenuTreeNodeEvent;
import sng.util.MenuTreeNode.MenuTreeNodeListener;
import sng.viewer.panels.*;
import sng.viewer.panels.Basic.BasicGOQueryTab;
import sng.viewer.panels.Basic.BasicHitQueryTab;
import sng.viewer.panels.Basic.BasicSeqQueryTab;
import sng.viewer.panels.pairsTable.FieldPairsTab;
import sng.viewer.panels.pairsTable.PairListTab;
import sng.viewer.panels.pairsTable.PairTopRowTab;
import sng.viewer.panels.pairsTable.QueryPairsTab;
import sng.viewer.panels.seqDetail.SeqTopRowTab;
import sng.viewer.panels.seqTable.ContigListTab;
import sng.viewer.panels.seqTable.FieldContigTab;
import sng.viewer.panels.seqTable.QueryContigTab;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.Converters;
import util.methods.Out;
import util.methods.Static;
import util.methods.TimeHelpers;
import util.ui.DisplayFloat;
import util.ui.UIHelpers;

/**
 *     Single TCW window
 *   	  Execute the query...
 */
public class STCWFrame extends JFrame {
	private static final long serialVersionUID = 1436659031225723421L;

	public static int nFrames = 0; // Very important -- used by STCWChooser and here to determine when to System.exit
	
	private static final boolean debug=false;
	// if change here, also change in util.ui.MenuTreeNode
	private static final Color bgColor = Color.white; 
	private static final Color bgColorLeft = Color.white; 
	private static final String title = "viewSingleTCW";
	private static final String overviewHelp = "html/viewSingleTCW/Overview.html";
	
	// Always uses the same preference file. So if in conflict with earlier versions, change name.
	String prefRootName = "viewSingleTCW"; 
	
	public STCWFrame(HostsCfg hosts, DBInfo dbInfo, boolean isApplet) {
		initialize();
		hostsObj = hosts;
		dbObj = dbInfo;
		bIsApplet = isApplet;
		
		buildInterface();

		System.err.println("Viewing "  +  dbInfo.getdbName() + " Project: " + dbInfo.getID());
		setVisible(true);
	}
	
	private void initialize() {
		Out.prt("Initialize...");
		
		lastSaveFilePath = System.getProperty("user.dir") + "/" + Globalx.EXPORTDIR;
		File nDir = new File(lastSaveFilePath);
		if (!nDir.exists()) {
			if (nDir.mkdir()) Out.prt("Create " + lastSaveFilePath);
			else lastSaveFilePath = System.getProperty("user.dir");
		}
		
		// Add shutdown handler to remove cache files
		MyShutdown sh = new MyShutdown();
		Runtime.getRuntime().addShutdownHook(sh);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Load window dimensions from preferences
		Preferences userPrefs = openPreferencesRoot();
		if (userPrefs!=null) {
			int nX = userPrefs.getInt("frame_win_x", Integer.MAX_VALUE);
			int nY = userPrefs.getInt("frame_win_y", Integer.MAX_VALUE);
			int nWidth = userPrefs.getInt("frame_win_width", Integer.MAX_VALUE);
			int nHeight = userPrefs.getInt("frame_win_height", Integer.MAX_VALUE);
			if (nX == Integer.MAX_VALUE) {
				UIHelpers.centerScreen(this);
			} else
				setBounds(nX, nY, nWidth, nHeight);
			DisplayFloat.setRoundingPrefs(userPrefs.get("rounding", "").trim());
		}
		else UIHelpers.centerScreen(this);
		
		// Setup to save window dimensions when it is closed
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				shutdown(); 

				try {
					// Save the window dimensions to the preferences
					if (prefsRoot!=null) {
						prefsRoot.putInt("frame_win_x", getX());
						prefsRoot.putInt("frame_win_y", getY());
						prefsRoot.putInt("frame_win_width", getWidth());
						prefsRoot.putInt("frame_win_height", getHeight());
						prefsRoot.flush();
					}
					cleanupMemory();
					--nFrames;
					if (nFrames == 0 && !UIHelpers.isApplet()) {
						System.exit(0);
					} 
				} 
				catch (Exception err) {ErrorReport.reportError(err, "Error during initialization");}
			}
		});

		try {
			setTitle(title + " on " + InetAddress.getLocalHost().getHostName());
		} catch (Exception e) {
			setTitle(title + " " + STCWMain.TCW_VERSION );
		}
		tabbedPane = new HiddenTabbedPane();
		tabbedPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		//contentPane = getContentPane();
		//contentPane.add(tabbedPane);

		++nFrames;
	}
	// all viewSingle panels call this to set the selected path, so can be used on next file chooser
	public void setLastPath(String path) {
		try {
			int last = path.lastIndexOf("/");
			lastSaveFilePath = path.substring(0, last);
			if (!new File(lastSaveFilePath).isDirectory())
				lastSaveFilePath=System.getProperty("user.dir"); 
		}
		catch (Exception e) {ErrorReport.prtReport(e, "setting last path");}
	}
	
	/********************************************************************
	 * XXX  Single single dataset window
	 * Creates the left panel of menu items and the outer left panel
	 * Reads the assembly and displays the overview
	 */
	private void buildInterface() {
		try {
			setSize(1024, 768);
			setTitle(dbObj.getDescription());
			System.err.println("   Open database");
			
			if (!dbObj.checkDBver(hostsObj)) System.exit(-1);
			dbName = dbObj.getdbName();
			metaData = new MetaData(this, hostsObj.getCapPath());
			
			System.out.println("   Build interface");
			
			// it is threaded so can't depend on flags from MetaData for initial left panel
			MenuTreeNode root = new MenuTreeNode();

			Tab instructionsTab = new StyleTextTab(this, null,HTML);
			tabbedPane.addTab(Instructions, instructionsTab);

			Tab overviewTab = addOverviewTab(); 
			tabbedPane.addTab(Overview, overviewTab);

			decimalTab = new DecimalNumbersTab(this);
			tabbedPane.addTab(DecimalNumbers, decimalTab);
			
			// Contig (Sequence) query (Filter) window
			filterContigTab = new QueryContigTab(this, new RunQuery(RunQuery.QUERY_CONTIGS));
			tabbedPane.addTab(ContigsFilters, filterContigTab); 
			
			// Pairs query (Filter) window
			filterPairsTab = new QueryPairsTab(this, new RunQuery(RunQuery.QUERY_PAIRS));
			tabbedPane.addTab(PairsFilters, filterPairsTab); 
				
			//Setup the columns for Contigs and Pairs
			fieldContigTab = new FieldContigTab(this);
			tabbedPane.addTab(ContigsColumns, fieldContigTab);
			
			fieldPairsTab = new FieldPairsTab(this); 
			tabbedPane.addTab(PairsColumns, fieldPairsTab);
			
			// Basic windows
			basicSeqQueryTab = new BasicSeqQueryTab(this);
			tabbedPane.addTab(BasicSeqQuery, basicSeqQueryTab);
			MenuTreeNode basicContigQueryNode = new MenuTreeNode(BasicSeqQuery, basicSeqQueryTab);
			
			basicHitQueryTab = new BasicHitQueryTab(this);
			tabbedPane.addTab(BasicHitQuery, basicHitQueryTab);
			MenuTreeNode basicHitQueryNode = new MenuTreeNode(BasicHitQuery, basicHitQueryTab);
			
			MenuTreeNode basicGOQueryNode = null;
			if(metaData.hasGOs()) {
				basicGOQueryTab = new BasicGOQueryTab(this);
				tabbedPane.addTab(BasicGOQuery, basicGOQueryTab);
				basicGOQueryNode = new MenuTreeNode(BasicGOQuery, basicGOQueryTab);
			}

			blastTab = new BlastTab(this,metaData);
			tabbedPane.addTab(Blast, blastTab);
			MenuTreeNode blastNode = new MenuTreeNode(Blast,blastTab);
			
			//Columns for displaying query criteria on the "Sequence Results" 
			String [] colNames = new String [] {"Result", "Filter"}; 
			resultsContigTab = new ResultsSummaryTab(this, null, colNames);
			tabbedPane.addTab(ContigsResults, resultsContigTab);
			
			//Columns for displaying queries on the "Pair Results"
			resultsPairTab = new ResultsSummaryTab(this, null, colNames);
			tabbedPane.addTab(PairResults, resultsPairTab);

			/*
			 *  Display on left panel
			 */
			MenuTreeNode general = new MenuTreeNode(GeneralSection);
			general.addChild(new MenuTreeNode(Instructions, instructionsTab));
			general.addChild(new MenuTreeNode(Overview, overviewTab));
			general.addChild(new MenuTreeNode(DecimalNumbers, decimalTab));
			root.addChild(general);

			MenuTreeNode contigHeader = new MenuTreeNode(SequenceSection);
			contigHeader.addChild(new MenuTreeNode(ShowAllContigs));
			contigHeader.addChild(new MenuTreeNode(ContigsColumns, fieldContigTab));
			contigHeader.addChild(new MenuTreeNode(ContigsFilters, filterContigTab));
			contigHeader.addChild(new MenuTreeNode(ContigsResults, resultsContigTab));
			root.addChild(contigHeader);	

			MenuTreeNode basicHeader = new MenuTreeNode(BasicSection);
			basicHeader.addChild(basicContigQueryNode);
			basicHeader.addChild(basicHitQueryNode);
			if(metaData.hasGOs())
				basicHeader.addChild(basicGOQueryNode);
			basicHeader.addChild(blastNode);
			root.addChild(basicHeader);
			
			MenuTreeNode pairHeader = new MenuTreeNode(PairsSection);
			pairHeader.addChild(new MenuTreeNode(ShowAllPairs));
			pairHeader.addChild(new MenuTreeNode(PairsColumns, fieldPairsTab));
			pairHeader.addChild(new MenuTreeNode(PairsFilters, filterPairsTab));
			pairHeader.addChild(new MenuTreeNode(PairsResults, resultsPairTab));
			root.addChild(pairHeader);			

			menuTree = new MenuTree(root);
			menuTree.addMenuTreeNodeListener(menuTreeListener);
			
			// the Sequences and Pairs get loaded on startup, 
			// but its threaded so interface can appear before its done 
			// loadQueryFilter creates the tab, which is associated with its name 
			loadQueryFilter(null, RunQuery.createAllContigQuery(), null);
			menuTree.getNodeWithName(ShowAllContigs).setUserObject(tabbedPane.getTabWithTitle(ShowAllContigs));
			
			loadQueryFilter(null, RunQuery.createAllPairsQuery(), null);
			menuTree.getNodeWithName(ShowAllPairs).setUserObject(tabbedPane.getTabWithTitle(ShowAllPairs));
			
			if (!metaData.hasPairWise()) { // hide 
				menuTree.getNodeWithName(PairsSection).setVisible(false);
				menuTree.getNodeWithName(ShowAllPairs).setVisible(false);
				menuTree.getNodeWithName(PairsFilters).setVisible(false);
				menuTree.getNodeWithName(PairsFilters).hideChildren();
				menuTree.getNodeWithName(PairsColumns).setVisible(false);
				menuTree.getNodeWithName(PairsResults).setVisible(false);
			}				
			
			JScrollPane menuScrollPane = new JScrollPane(menuTree);
			menuScrollPane.setPreferredSize(new Dimension(400, 400));
			menuScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			menuScrollPane.setBackground(bgColorLeft);
			
			/*
			 * Buttons in top panel for different options from left panel
			 * Other buttons are in ContigListTab.java and ContigPairListTab.java
			 */
			JPanel buttonPanel = new JPanel();
					// next statement doesn't seem to effect anything
			buttonPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE)); 
			buttonPanel.setBackground(bgColor);

			if (lblStatus == null) {
				lblStatus = new JLabel("Working...");
				lblStatus.setForeground(bgColor);
			}

			// XXX View Filtered Contig
			btnFiltered = new JButton(ViewFilteredSequences);
			btnFiltered.setBackground(Globals.FUNCTIONCOLOR);
			btnFiltered.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {

					if(btnFiltered.getText().equals(ViewFilteredSequences)){
						filterContigTab.executeQuery();
					}
					else if(btnFiltered.getText().equals(ViewFilteredPairs)) {
						filterPairsTab.executeQuery();
					}
				}
			});

			JPanel leftPanel = new JPanel();
			leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
			leftPanel.setBackground(bgColorLeft); // only effects upper part
			leftPanel.add(menuScrollPane);

			buttonPanel.add(btnFiltered);
			buttonPanel.add(lblStatus);

			viewPane = Static.createPagePanel();
			viewPane.add(buttonPanel);
			viewPane.add(tabbedPane);

			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			splitPane.setContinuousLayout(true);
		    splitPane.setDividerLocation(215);
		    splitPane.setOneTouchExpandable(true); 
		    
		    splitPane.setBorder(null);
		    splitPane.setRightComponent(viewPane);
		    splitPane.setLeftComponent(leftPanel);
		       
		    Dimension minSize = new Dimension(215,50);
		    leftPanel.setMinimumSize(minSize);
		    
		    setLayout(new BorderLayout());
	        add(splitPane, BorderLayout.CENTER);
			
			tabbedPane.setSelectedTab(overviewTab);
			menuTree.setSelectedNode("Overview");
		} catch (Exception err) {
			ErrorReport.reportError(err, "Internal error: creating interface");
			swapToExceptionTab(null, err);
		}
	}

	/********************************************************
	 * Selecting an item in the menu tree, or adding an item to the menu tree
	 * evokes this listener. However, next and prev do not evoke this.
	 */
	private MenuTreeNodeListener menuTreeListener = new MenuTreeNodeListener() {
		public void eventOccurred(MenuTreeNodeEvent e) {
			MenuTreeNode node = e.getNode();
			if (node == null)return;

			Tab tab = (Tab) node.getUserObject();
			if (tab == null) return;

			String nodeName = node.getText();
			if(nodeName.equals(ContigsFilters) || nodeName.equals(ContigsColumns))
			{
				btnFiltered.getParent().setVisible(true);
				btnFiltered.setText(ViewFilteredSequences);
			}
			else if(nodeName.equals(PairsFilters) || nodeName.equals(PairsColumns)) {
				btnFiltered.getParent().setVisible(true);
				btnFiltered.setText(ViewFilteredPairs);
			}
			else
			{
				btnFiltered.getParent().setVisible(false);
			}
			
			// closing a tab 
			if (e.getType() == MenuTreeNodeEvent.TYPE_CLOSED) {
				MenuTreeNode parentNode = node.getParentNode();
				removeNode(node); 
				
				resultsContigTab.removeResultSummary(tab);
				resultsPairTab.removeResultSummary(tab);
				menuTree.setSelected(parentNode);
				tab = (Tab) parentNode.getUserObject();
				tabbedPane.setSelectedTab(tab);
			} else if (e.getType() == MenuTreeNodeEvent.TYPE_SELECTED)
				tabbedPane.setSelectedTab(tab);
		}
	};
	
	public void setButtonsVisible(boolean show) {
		if(btnFiltered != null) btnFiltered.getParent().setVisible(show);
	}
	
	/****************************************
	 * Display methods
	 */
	//Load individual contigs for a table: from Basic
	public void loadContigs(String strTabID, String [] contigs, int basicType) {
		filterContigTab.executeQuery(strTabID, contigs, basicType);
	}
	// filterContigTab.executeQuery -> queryContigTab.executeQuery -> calls this method 
	public void addQueryResultsTab(RunQuery theQuery, String [] contigIDs, int viewMode, String strTabID) {
		loadQueryContigs(null, theQuery, contigIDs, viewMode, strTabID);
	}
	
	// Used for displaying individual sequence from the basic query (Could be combined with the filter handleLoadQuery
	public void loadQueryContigs(Tab oldTab, RunQuery inQuery, final String [] contigIDs, 
	            final int basicType, final String strTabID) { // strTabID shows on left followed by ':' # seqs
		try {
			long time = TimeHelpers.getTime();
			final RunQuery theQuery = (RunQuery) Converters.deepCopy(inQuery);
		
			String strTabTitle = strTabID;
			if (oldTab != null) strTabTitle = oldTab.getTitle();

			final Vector <String> tableRows = new Vector <String> ();
			final InterThreadProgress progress = new InterThreadProgress(this);
			
			progress.swapInTabAndStartThread(oldTab, strTabTitle,
					new InterThreadProgress.WorkerThread() 
			{
				public void run() throws Throwable 
				{
					progress.setProgressLabel("Loading ...");

					String nodeID = strTabID;
					String summary="";
					MenuTreeNode newNode = new MenuTreeNode(nodeID);
					
					if (strTabID != null) { 
						newNode.setUserObject(progress.getProgressPanel());
						
						if (basicType == BASIC_QUERY_MODE_SEQ)     {
							menuTree.addNode(BasicSeqQuery, newNode);
							summary = "Basic Seq";
						}
						else if(basicType == BASIC_QUERY_MODE_HIT) {
							menuTree.addNode(BasicHitQuery, newNode);
							summary = "Basic Hit";
						}
						else   if(basicType == BASIC_QUERY_MODE_GO) {
							menuTree.addNode(BasicGOQuery, newNode);
							summary = "Basic GO";
						}
						else   if(basicType == PAIRS_QUERY) {
							menuTree.addNode(ContigsFilters, newNode);
							summary = "Pairs view sequence";
						}
						else System.err.println("TCW internal error -- no such type " + basicType);
						
						// must come after addNode
						newNode.setType(MenuTreeNode.TYPE_CLOSEABLE); 
						menuTree.setSelected(newNode);
					}
					
					// XXX Execute the query...
					FieldMapper fields = fieldContigTab.getMapper();
					
					int count = theQuery.loadTableRowsForContigs(
							getInstance(), fieldContigTab, fields, contigIDs, tableRows);
					
					Tab newTab = new ContigListTab(STCWFrame.this,
								fields, theQuery, contigIDs, tableRows, 
								metaData.nContigs(), summary);
					
					progress.swapOutProgress(newTab);
					if (strTabID != null) { 
						// Update new node with new tab of query results
						if (!progress.wasCanceled()) {
							nodeID = strTabID + ": " + count + " seqs";
							newNode.setText(nodeID);
							newNode.setUserObject(newTab);
							
							if(theQuery.getType() == RunQuery.QUERY_CONTIGS)
								resultsContigTab.addResultSummary(nodeID,newTab, newNode, summary);
							else
								resultsPairTab.addResultSummary(nodeID,newTab, newNode, summary);
						} else {
							// this thread was canceled but hasn't been killed yet
							newNode.setUserObject(progress.getCancelPanel());
						}
						menuTree.setSelected(newNode);
					}
				}
			});
			if (STCWMain.doTIME) {
				Out.prt("> " + strTabTitle + 
						" query time: " + TimeHelpers.getElapsedTimeStr(time) + 
						" total memory: " + TimeHelpers.getMemoryUsed());
			}
		} catch (Exception err) {
			ErrorReport.reportError(err, "Internal error: loading query");
		}
	}
	private STCWFrame getInstance() {return this;}
	/***********************************
	 * handle Query for Contig or Pairs data
	 */
	public void addQueryResultsTab(RunQuery theQuery, String strTabID) 
	{
		loadQueryFilter(null, theQuery, strTabID);
	}
	/**************************************************
	 * View Sequences and View Pairs table.
	 * oldTab = not null if Refresh Columns
	 * strTabID = not blank if Filter
	 */
	public void loadQueryFilter(Tab oldTab, RunQuery inQuery, final String strTabID) {
		try {
			long time = TimeHelpers.getTime();
			final RunQuery theQuery = (RunQuery) Converters.deepCopy(inQuery);
		
			String strTabTitle = strTabID;
			if (oldTab != null) {
				strTabTitle = oldTab.getTitle();
			}
			else if (strTabID == null) {
				if (theQuery.isAllContigs()) 			strTabTitle = ShowAllContigs;
				else if (theQuery.isAllContigPairs()) 	strTabTitle = ShowAllPairs;
				else {Out.PrtError("Load Query Filter"); return;}
			
				oldTab = tabbedPane.getTabWithTitle(strTabTitle);
			} 
			
			final Vector <String> tableRows = new Vector <String> ();
			final InterThreadProgress progress = new InterThreadProgress(this);
			
			progress.swapInTabAndStartThread(oldTab, strTabTitle,
					new InterThreadProgress.WorkerThread() 
			{
				public void run() throws Throwable 
				{
					progress.setProgressLabel("Loading ...");

					String nodeID = strTabID;
					MenuTreeNode newNode = new MenuTreeNode(nodeID);
					
					if (strTabID != null) { 
						newNode.setUserObject(progress.getProgressPanel());
						
						if(theQuery.getType() == RunQuery.QUERY_CONTIGS)
							menuTree.addNode(ContigsFilters, newNode);
						else
							menuTree.addNode(PairsFilters, newNode);
						
						newNode.setType(MenuTreeNode.TYPE_CLOSEABLE); // must come after addNode
						menuTree.setSelected(newNode);
					}

		// XXX Execute the query...
					String summary="";
					FieldMapper fields = null;
					if (theQuery.getType() == RunQuery.QUERY_CONTIGS) {
						fields = fieldContigTab.getMapper();
					}
					else {
						fields = fieldPairsTab.getMapper();
					}
						
					int count=0;
					Tab newTab = null;
					if (theQuery.getType() == RunQuery.QUERY_CONTIGS) {// Setup the output tab
						
						count = theQuery.loadTableRowsForFilterSeq( 
								getInstance(), fieldContigTab, fields, filterContigTab, tableRows);
						if (count==-1) 
							JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
						progress.setProgressLabel("Loading " + count + " sequences...");

						summary = theQuery.getContigSummary();
						newTab = new ContigListTab(STCWFrame.this,
								fields, theQuery, null, tableRows, metaData.nContigs(), summary);
					}
					else {
						count = theQuery.loadTableRowsForFilterPairs(
								getInstance(), fieldPairsTab, fields, tableRows);
						if (count==-1) 
							JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
						progress.setProgressLabel("Loading " + count + " pairs...");

						summary = theQuery.getPairsSummary();
						newTab = new PairListTab(STCWFrame.this,
								fields, theQuery, tableRows, summary);
					}
					progress.swapOutProgress(newTab);
					if (strTabID != null) { 
						if (!progress.wasCanceled()) {
							nodeID = strTabID + ": " + count + " seqs";
							newNode.setText(nodeID);
							newNode.setUserObject(newTab);
							// Add new entry to results summary table
							if(theQuery.getType() == RunQuery.QUERY_CONTIGS)
								resultsContigTab.addResultSummary(nodeID,newTab, newNode, summary);
							else
								resultsPairTab.addResultSummary(nodeID,newTab, newNode, summary);
						} else {
							newNode.setUserObject(progress.getCancelPanel());
						}
						menuTree.setSelected(newNode);
					}
				}
			});
			if (STCWMain.doTIME) {
				Out.prt("> " + strTabTitle + 
						" query time: " + TimeHelpers.getElapsedTimeStr(time) + 
						" total memory: " + TimeHelpers.getMemoryUsed());
			}
		} catch (Exception err) {
			ErrorReport.reportError(err, "Internal error: loading query");
		}
	}
	
	private static int nThreads = 0;
	public void updateStatus(int action) 
	{
		if (action == 1)      nThreads++; // thread started
		else if (action == 0) nThreads--; // thread ended
			
		if (lblStatus == null) lblStatus = new JLabel("Working...");

		if (nThreads > 0) lblStatus.setForeground(Color.BLACK); // show
		else lblStatus.setForeground(bgColor); // hide
	}

	public void swapInCAP3Tab(Tab oldTab, MultiCtgData theCluster,
		int nRecordNum, String strTabTitle) 
	{
		Tab tab = new SeqTopRowTab(this, theCluster, null, nRecordNum, null);
		tabbedPane.addTab(strTabTitle, tab);
		tabbedPane.setSelectedTab(tab);
		
		// Reposition the cluster tab to the position of the "wait" tab
		MenuTreeNode node = menuTree.getNodeWithUserObject(oldTab);
		if (node != null) node.setUserObject(tab);
		tabbedPane.swapInTab(oldTab, strTabTitle, tab);
		tabbedPane.setSelectedTab(tab);		
	}

	// adds a tab on the left under Show Sequences
	public Tab addContigPage(String strContigName, Tab parentTab, int nRecordNum) 
	{
		Tab tab = addNewContigTab(strContigName, parentTab, nRecordNum, null);
		
		MenuTreeNode newNode = new MenuTreeNode(strContigName);
		tab.setMenuNode(newNode);
		newNode.setUserObject(tab);
		menuTree.addNode(parentTab, newNode);
		newNode.setType(MenuTreeNode.TYPE_CLOSEABLE);
		menuTree.setSelected(newNode);
		
		return tab;
	}
	// called from addContigPage and seqDetail.addNewTab for prev/next
	public Tab addNewContigTab(String strContigName, 
			Tab parentTab, int nRecordNum, int [] prevDisplayOptions) 
	{
		ContigData theContig = new ContigData();
		theContig.setContigID(strContigName);

		MultiCtgData mData = new MultiCtgData();// Make a psuedo cluster with the one contig
		mData.addContig(theContig);

		Tab tab = new SeqTopRowTab(this, mData, parentTab, nRecordNum, prevDisplayOptions);
		tabbedPane.addTab(strContigName, tab);
		tabbedPane.setSelectedTab(tab);
		return tab;
	}
	
	// called from Pairs Table for selected row
	public void addPairAlignTab(MultiCtgData pairObj, String strTitle,
			Tab parentTab, int nRecordNum, int nPairNum) 
	{
		Tab newTab = new PairTopRowTab(this, pairObj, parentTab, nRecordNum, nPairNum, null);
		tabbedPane.addTab(strTitle, newTab);
		tabbedPane.setSelectedTab(newTab);
		
		MenuTreeNode newNode = new MenuTreeNode(strTitle, newTab);
		newTab.setMenuNode(newNode);
		
		menuTree.addNode(parentTab, newNode);
		newNode.setType(MenuTreeNode.TYPE_CLOSEABLE);
		menuTree.setSelected(newNode);
	}
	// Called from PairTopRowTab: next/prev
	public void addPairAlignNextPrev(MultiCtgData theCluster, 
			Tab parentTab, int nRecordNum, int nPairNum,
			String strTabTitle, int [] prevDisplayOptions, MenuTreeNode node) 
	{
		Tab newTab = new PairTopRowTab(this, theCluster, parentTab, nRecordNum, nPairNum, prevDisplayOptions);
		tabbedPane.addTab(strTabTitle, newTab);
		tabbedPane.setSelectedTab(newTab);
		
		// Re-use existing menu tree node		
		removeTabFromNode(node); 
		newTab.setMenuNode(node);
		node.setText(strTabTitle);
		node.setUserObject(newTab);
	}
	
	public MenuTreeNode addNewMenuTreeNode(String parentTitle,
			String nodeTitle, Tab tab, String toolTipText) 
	{
		MenuTreeNode node = menuTree.getNodeWithUserObject(tab);
		if (node == null) {
			node = new MenuTreeNode(nodeTitle);
			menuTree.addNode(parentTitle, node);
			node.setType(MenuTreeNode.TYPE_CLOSEABLE);
		}
		node.setUserObject(tab);
		node.setToolTipText(toolTipText);
		menuTree.setSelected(node); // causes action event

		return node;
	}
	
	private Tab addOverviewTab() {
		Vector<String> tempMsg = new Vector<String>();
		tempMsg.add("Generating statistics ...\n" +
				"If the overview has to be computed, this can take a few minutes.\n" +
				"You can go to another tab if you like.");
		final TextTab newTab = new TextTab(this, null, tempMsg, overviewHelp);

		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Vector<String> lines = new Vector<String>();
					DBConn mDB = getNewDBC();
					new Overview(mDB).overview(lines); 
					mDB.close();
					newTab.setContent(lines);
				} catch (Exception err) {
					newTab.setContent("Failed generating stats.\n" + err.toString());
					ErrorReport.reportError(err);
				}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();

		return newTab;
	}

	
	// Modification of the Runnable interface so that it can throw to
	// prevent redundant exception handling code.
	public interface RunnableCanThrow {
		public void run() throws Throwable;
	}
	// MainToolAlignPanel.alignSelectedESTs
	public void addTab(String strTabName, Tab newTab) {
		tabbedPane.addTab(strTabName, newTab);
		tabbedPane.setSelectedTab(newTab);
	}
	// SeqTopRowTab, PairTopRow -> displayDraw, loadContigDataAndDraw
	public void addTabAndRun(Tab oldTab, String strTabName, Tab newTab,
			RunnableCanThrow toBeRun) {
		
		/**************** Nested Class ****************/
		class WaitAndRun implements Runnable {
			private RunnableCanThrow holdToBeRun = null;
			private Tab waitTab = null;

			public WaitAndRun(RunnableCanThrow inWaitAndRun, Tab inWaitTab) {
				waitTab = inWaitTab;
				holdToBeRun = inWaitAndRun;
			}

			public void run() {
				try {
					UIHelpers.setFrameWaitCursor(STCWFrame.this);
					holdToBeRun.run();   // run the actual process
				} catch (Throwable err) {
					ErrorReport.reportError(err, "Internal error: wait and run");
					swapToExceptionTab(waitTab, err);// Swap the wait tab for an exception tab
				}
				UIHelpers.clearFrameCursor(STCWFrame.this);
			}
		}
		;
		/************* End Nested Class *******************/
		swapInTab(oldTab, strTabName, newTab);
		validate(); 
		repaint(); 

		// Add an event to actually run the process after all of the events are processed to display the new tab.
		EventQueue.invokeLater(new WaitAndRun(toBeRun, newTab));
	}

	public void swapInTab(Tab oldTab, String strNewTitle, Tab newTab) {
		if (tabbedPane==null || menuTree==null) return;  // can happen on closing
		if (oldTab==null || newTab==null) return; 		// don't think this happens 
		
		tabbedPane.swapInTab(oldTab, strNewTitle, newTab);

		if (menuTree != null) {
			MenuTreeNode n = menuTree.getNodeWithUserObject(oldTab);
			if (n != null) 
				n.setUserObject(newTab);
		}
	}
	
	public void removeTabFromNode(MenuTreeNode node) {
		Tab tab = (Tab) node.getUserObject();
		tabbedPane.remove(tab);
	}
	public void removeTab(Tab t) {
		tabbedPane.remove(t);
	}
	public void removeResult(String name) {
		MenuTreeNode node = menuTree.getNodeWithName(name);
		if (node!=null) removeNode(node); 
	}
	public void removeNode(MenuTreeNode node) {
		Vector <MenuTreeNode> children = node.getChildNodes();
		
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext()) {
			MenuTreeNode child = iter.next();
			Tab tab = (Tab) child.getUserObject();
			tabbedPane.remove(tab);
			menuTree.removeNodeFromPanel(child);
		}
		Tab tab = (Tab) node.getUserObject();
		tabbedPane.remove(tab);
		menuTree.removeNode(node);
	}
	public void swapToExceptionTab(Tab oldTab, Throwable err) {
		if (debug)
			System.err.println("STCWFrame.swapToExceptionTab: oldTab=" + oldTab);

		// Build up a list of error information
		Vector<String> errorStrings = new Vector<String>();
		if (err instanceof OutOfMemoryError) {
			errorStrings.add("viewSingleTCW ran out of memory.");
			if (bIsApplet) {
				STCWChooser.addAppletMemory(errorStrings);
			} else {
				errorStrings.add("Increase the Java memory size for your system using the instructions below:");
				errorStrings.add("- Edit the viewSingleTCW file.");
				errorStrings.add("- Changed the text '-Xmx512m' to a larger number, e.g '-Xmx768'.");
				errorStrings.add("- Save the file and restartSingleTCW.");
			}
		} else {
			errorStrings.add("The viewSingleTCW program encountered an error, we apologize for the inconvenience.");
			errorStrings.add("Please send the error description below to tcw@agcol.arizona.edu.");
			errorStrings.add("To copy the error description, highlight it with the mouse and press CTRL-C.");
			errorStrings.add("");
			errorStrings.addAll(Converters.getStackTraceAndMessageLines(err));
		}
		err.printStackTrace();

		swapInTab(oldTab, null, new TextTab(null, null, errorStrings, overviewHelp));
	}
	
	private Preferences openPreferencesRoot() {
		Preferences root = null;
		try {
			root = Preferences.userRoot();
		} catch (Throwable err) {
			System.err.println("Cannot open the user's preferences. Will use defaults.");
			return null;
		}	
		prefsRoot = root.node(prefRootName);
		return prefsRoot;
	}

	private void cleanupMemory()
	{
		//contentPane = null;
		tabbedPane = null;
		viewPane = null;
		splitPane = null;
		menuTree = null;
		lblStatus = null;
		btnFiltered = null;
		if(filterContigTab != null) filterContigTab.close();
		if(filterPairsTab != null) filterPairsTab.close();
		if(fieldPairsTab != null) fieldPairsTab.close();
		if(fieldContigTab != null) fieldContigTab.close();
		if(basicSeqQueryTab != null) basicSeqQueryTab.close();
		if(basicHitQueryTab != null) basicHitQueryTab.close();
		if(resultsContigTab != null) resultsContigTab.close();	
		if(resultsPairTab != null) resultsPairTab.close();
		hostsObj = null;
	}
	/**
	 * Shutdown methods
	 */
	private class MyShutdown extends Thread { // runs at program exit for each JFrame
		public void run() {
			shutdown();
		}
	}

	private boolean isShutdown = false;

	private synchronized void shutdown() {
		try {
			if (!isShutdown) {
				// used to be necessary to remove cached files
				for (Tab t : tabbedPane.getTabs()) t.close();
				
				ErrorReport.notifyUserOnClose("tcw@agcol.arizona.edu");
				
				try {
					prefsRoot.flush();
				}
				catch (Exception e){ErrorReport.prtReport(e, "Saving preferences");}
				
				isShutdown = true;
			}
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error shuting down");}
	}
	public QueryPairsTab getQueryPairsTab() { return filterPairsTab;}
	public QueryContigTab getQueryContigTab() { return filterContigTab;}
	public FieldPairsTab getFieldPairsTab() { return fieldPairsTab;}
	public FieldContigTab getFieldContigTab() { return fieldContigTab;}
	
	public MetaData getMetaData() { return metaData;}
	public String getdbName() {return dbName;}
	public String getdbID() {return dbObj.getID();}
	public HostsCfg getHosts() {return hostsObj;}
	public DBConn getNewDBC() { return hostsObj.getDBConn(dbName);}
	public Connection getNewConn() throws Exception {return hostsObj.getDBConn(dbName).getDBconn();}
	public boolean isApplet() {return bIsApplet;}
	public Preferences getPreferencesRoot() {return prefsRoot;}
	
	/******************************
	 * Constants
	 */
	public static final int BASIC_QUERY_MODE_SEQ = 0;
	public static final int BASIC_QUERY_MODE_HIT = 1;
	public static final int BASIC_QUERY_MODE_GO = 2;
	public static final int PAIRS_QUERY = 3;
	
	// These string are labels on the left. And used in MenuTree as a way to put items under them.
	private static final String GeneralSection = "General ";
	private static final String Instructions = "Instructions";
	private static final String HTML = "/html/viewSingleTCW/instructions.html";
	private static final String Overview = "Overview";
	private static final String DecimalNumbers = "Decimal Numbers";
	private static final String Blast = "Find Hits"; 
	
	private static final String BasicSection =  "Basic Queries ";
	private static final String BasicSeqQuery = "Sequences";
	private static final String BasicHitQuery = "AnnoDB Hits";
	public static final String BasicGOQuery =   "GO Annotation"; 
	
	private static final String SequenceSection = "Sequence ";
	public static final String ShowAllContigs = "Show All";
	private static final String ContigsFilters = "Filters";
	private static final String ContigsColumns = "Columns";
	private static final String ContigsResults = "Results";
	private static final String ViewFilteredSequences = "View Filtered Sequences";
	
	private static final String PairsSection = "Similar Pairs ";
	private static final String ShowAllPairs = "Show Pairs";
	private static final String PairsFilters = "Pairs Filters";
	private static final String PairsResults = "Pairs Results";
	private static final String PairsColumns = "Pairs Columns";
	private static final String PairResults = "Pair Results";
	private static final String ViewFilteredPairs = "View Filtered Pairs";
	
	public JSplitPane splitPane;
	public MenuTree menuTree;
	public HiddenTabbedPane tabbedPane;
	
	//private Container contentPane;
	private JPanel viewPane;
	private JLabel lblStatus = null;
	private JButton btnFiltered = null;

	// TABs
	private DecimalNumbersTab decimalTab = null;
	private QueryContigTab filterContigTab = null;
	private FieldContigTab fieldContigTab = null;
	
	private QueryPairsTab  filterPairsTab = null;
	private FieldPairsTab  fieldPairsTab = null;
	
	private BasicSeqQueryTab basicSeqQueryTab = null;
	private BasicHitQueryTab basicHitQueryTab = null;
	private BasicGOQueryTab basicGOQueryTab = null;
	
	private ResultsSummaryTab resultsContigTab = null;
	private ResultsSummaryTab resultsPairTab = null;
	private BlastTab blastTab  = null;
	
	public String lastSaveFilePath = ""; // All panels use this as the path, and set it if changed

	private boolean bIsApplet=false;
	
	private Preferences prefsRoot=null; // set once and pass to anyone who needs it
	private MetaData metaData = null;
	private HostsCfg hostsObj=null;
	private DBInfo dbObj = null;
	private String dbName=null;
}
