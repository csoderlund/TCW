package sng.viewer.panels.pairsTable;
/**
 * Called when a row is selected from pairs table
 * Displays buttons at top
 */
import java.util.Vector;
import java.util.TreeSet;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;

import javax.swing.*;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.BlastHitData;
import sng.dataholders.MultiCtgData;
import sng.util.CenteredMessageTab;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import sng.viewer.panels.MainToolAlignPanel;
import sng.viewer.panels.seqTable.ContigListTab;
import util.methods.ErrorReport;
import util.ui.MenuMapper;
import util.align.AlignCompute;
import util.align.AlignData;
import util.database.DBConn;

public class PairTopRowTab extends Tab  implements ClipboardOwner
{
	private static final long serialVersionUID = -4644940934101056207L;
	private Color buttonColor = new Color(230, 230, 255); 
	
	public PairTopRowTab ( STCWFrame theFrame,
                			MultiCtgData listData, Tab parentTab,					
                			int recordNum, int pairNum, int [] prevCtgDisplaySettings)
	{
		super(theFrame, parentTab);
		theMainFrame = theFrame;
		prevContigDisplaySettings = prevCtgDisplaySettings;
		nRecordNum = recordNum; 
		nPairNum = pairNum;
		pairCtgObj = listData;
		metaData = theFrame.getMetaData();	
		
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadPairFromDB dbObj = new LoadPairFromDB(dbc, theFrame.getMetaData());
			loadedPairObj = dbObj.loadTwoContigs(pairCtgObj);
			BlastHitData hitData =  dbObj.loadPairHitData(listData.getCtgIDAt(0), listData.getCtgIDAt(1));
			loadedPairObj.setPairHit(hitData);
			dbc.close();
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Internal error: creating Pair alignment object");
		}
	
		/********************************************
		 *  XXX Create dropdown menu of pairwise display options
		 */
		displayDropDown = new JComboBox ();
		displayDropDown.setBackground(buttonColor);
		Dimension dim = new Dimension ( 250, (int)displayDropDown.getPreferredSize().getHeight() );
		displayDropDown.setPreferredSize( dim );
		displayDropDown.setMaximumSize ( dim );

		menuBestalign = new MenuMapper(ShowPWbestFrame, SHOW_BEST_FRAME);
		displayDropDown.addItem(menuBestalign);
		
		MenuMapper menuOrientalign = new MenuMapper ( ShowPWorientFrames, SHOW_ORIENT_FRAMES );
		displayDropDown.addItem( menuOrientalign );	
		
		MenuMapper menuAllalign = new MenuMapper ( ShowPWallFrames, SHOW_ALL_FRAMES );
		displayDropDown.addItem( menuAllalign );	
		
		MenuMapper menuSharedHits = new MenuMapper ( ShowPWsharedHits, SHOW_SHARED_HITS);
		displayDropDown.addItem( menuSharedHits);		
			
		displayDropDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayTypeInit();
			}
		});
		JButton jbView = new JButton("View Sequences");
		jbView.setBackground(Globals.PROMPTCOLOR);
		jbView.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            		String ctg1 = pairCtgObj.getContigAt(0).getContigID();
				String ctg2 = pairCtgObj.getContigAt(1).getContigID();
				String [] contigNames = {ctg1, ctg2};
				String label = "Pair #" + nPairNum;
				getParentFrame().loadContigs(label, contigNames, STCWFrame.PAIRS_QUERY);
            }
        });
		// copy 
		final JPopupMenu copypopup = new JPopupMenu();
		copypopup.add(new JMenuItem(new AbstractAction("First Seq ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg1 = pairCtgObj.getContigAt(0).getContigID();
				saveTextToClipboard(ctg1);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Second Seq ID") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg2 = pairCtgObj.getContigAt(1).getContigID();
				saveTextToClipboard(ctg2);
			}
		}));
		JButton jbCopy = new JButton("Copy ID...");
		jbCopy.setBackground(Globals.PROMPTCOLOR);
		jbCopy.addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                copypopup.show(e.getComponent(), e.getX(), e.getY());
	            }
	        });
		// Create prev and next buttons
		JButton btnPrev = new JButton("<<Prev");
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if(displayedJPanel != null)
					setPrevDisplaySettings(((MainToolAlignPanel)displayedJPanel).getDisplaySettings());
				
				addPrevNextTab( ((PairListTab)getParentTab()).getPrevRowNum( nRecordNum ) );
			}
		});
		btnPrev.setBackground(buttonColor);

		JButton btnNext = new JButton("Next>>");
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(displayedJPanel != null) {
					setPrevDisplaySettings(((MainToolAlignPanel)displayedJPanel).getDisplaySettings());
				}
				addPrevNextTab( ((PairListTab) getParentTab()).getNextRowNum( nRecordNum ) );
			}
		});
		btnNext.setBackground(buttonColor);
		
		if (recordNum < 0)
		{
			btnPrev.setEnabled(false);
			btnNext.setEnabled(false);
		}

		// Top panel with buttons and drop-down		
		JPanel topPanel = new JPanel ( );
		topPanel.setLayout( new BoxLayout ( topPanel, BoxLayout.X_AXIS ) );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( displayDropDown ); 
		topPanel.add( Box.createHorizontalStrut(5) );	
		topPanel.add(jbView);
		topPanel.add( Box.createHorizontalStrut(5) );	
		topPanel.add(jbCopy);
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.add( Box.createHorizontalGlue() );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( btnPrev ); 	
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( btnNext ); 	
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		topPanel.setBackground(Color.white);
		super.setBackground(Color.white);
			
		bottomPanel = new JPanel ();
		
		// Add to the called object's panel
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		add ( Box.createVerticalStrut(5) );
		add ( topPanel );
		add ( Box.createVerticalStrut(5) );
		add ( bottomPanel );

		displayDropDown.setSelectedItem( menuBestalign );	
	}
	
	private void saveTextToClipboard(String lines) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents( new StringSelection(lines), (ClipboardOwner) this );
	}
	public void lostOwnership(Clipboard arg0, Transferable arg1) {} // needed for clipboard
	
	// called on ActionPerformed for displaydropdown
	private void displayTypeInit()
	{
		// So this a bit crazy, but otherwise the event for closing the
		// drop down menu never seems to come, and all of the other 
		// UI changes don't happen...
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MenuMapper selection = (MenuMapper) displayDropDown.getSelectedItem();
				int displayType = selection.asInt();
				displayTypeLoad(displayType);
			}
		});
	}

	// called on initialization of display (above) and on ActionaListener (below)
	private void displayTypeLoad ( int nDisplayType)
	{
		try
		{
			if ( displayedJPanel != null  ) 
				selectedContigs = getSelectedContigIDs ( );

			loadContigDataAndDraw ( nDisplayType);
		} 		
		catch ( Exception err )
		{
			ErrorReport.reportError(err, "Internal error in displayTypeLoad " + nDisplayType);
		}			
	}
	// XXX display for contig overview panel
	private void loadContigDataAndDraw ( int nDisplayAfterLoad) throws Exception
	{	
		// Swap in a wait tab while we hit the database
		final int saveDisplayAfterLoad = nDisplayAfterLoad;
		final Tab waitTab = new CenteredMessageTab ( "Loading please wait... " );
		
		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(PairTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				if(loadedPairObj == null) {
					DBConn dbc = theMainFrame.getNewDBC();
					LoadPairFromDB dbObj = new LoadPairFromDB(dbc, theFrame.getMetaData());
					loadedPairObj = dbObj.loadTwoContigs(pairCtgObj);
					BlastHitData hitData =  dbObj.loadPairHitData(loadedPairObj.getCtgIDAt(0), loadedPairObj.getCtgIDAt(1));
					loadedPairObj.setPairHit(hitData);
					dbc.close();
					if (loadedPairObj==null) return;
				}
				displayDraw(loadedPairObj, saveDisplayAfterLoad,  waitTab);
			}		
		});
	}
	
	private void  displayDraw ( MultiCtgData pairCtgObj,
								int displayType, Tab waitTab ) throws Exception
	{	
		boolean bHaveData = false;
		String strWaitMsg = "Please wait...";
		final STCWFrame theFrame = getParentFrame(); 
		
		switch ( displayType )
		{
		case SHOW_BEST_FRAME:
            strWaitMsg = "Aligning pair in best frame, please wait...";
			if ( bestFramePanel != null ) {
				installMainPanel ( bestFramePanel );
				bHaveData = true;
			}
			break;
			
		case SHOW_ORIENT_FRAMES:
            strWaitMsg = "Aligning pair in all 18 frames, please wait (this can be slow)...";
			if ( orientFramePanel != null ) {
				installMainPanel ( orientFramePanel );
				bHaveData = true;
			}
			break;
			
		case SHOW_ALL_FRAMES:
            strWaitMsg = "Aligning pair in all 36 frames, please wait (this can be slow)...";
			if ( allFramePanel != null ) {
				installMainPanel ( allFramePanel );
				bHaveData = true;
			}
			break;
			
		case SHOW_SHARED_HITS:
			strWaitMsg = "Aligning first 3 shared DB hit sequences; please wait...";
			if ( sharedHitPanel != null ) {
				installMainPanel ( sharedHitPanel );
				bHaveData = true;
			}
			break;
		default:
			System.err.println("Internal error MainTopRow: " + displayType);
		} // end displayType

		if ( bHaveData ) {
			if ( waitTab != null ) { 
				theFrame.swapInTab( waitTab, getTitle(), PairTopRowTab.this ); 
			}
			return; /******************** Early Exit ************************/
		}
		
		Tab oldTab = PairTopRowTab.this;
		if ( waitTab != null) oldTab = waitTab;

		final MultiCtgData pairObj = pairCtgObj;
		final int saveDisplay = displayType;
		final Tab tempTab = new CenteredMessageTab ( strWaitMsg );
		
		theFrame.addTabAndRun ( oldTab, null, tempTab, 
			new STCWFrame.RunnableCanThrow ()
			{
				public void run () throws Throwable
				{
					Vector<AlignData> pairs;
					
					switch ( saveDisplay )
					{						
					case SHOW_BEST_FRAME:
						pairs = AlignCompute.pairAlignDisplay( pairObj, AlignCompute.frameResult);
						bestFramePanel = installPairAlignPanel( pairs );
						break;
					case SHOW_ORIENT_FRAMES:
						pairs = AlignCompute.pairAlignDisplay( pairObj, AlignCompute.orientResult);
						orientFramePanel = installPairAlignPanel( pairs );
						break;
					case SHOW_ALL_FRAMES:
						pairs = AlignCompute.pairAlignDisplay( pairObj, AlignCompute.allResult);
						allFramePanel = installPairAlignPanel( pairs );
						break;
					case SHOW_SHARED_HITS: 
						pairs = AlignCompute.pairAlignSharedHits(pairObj, metaData.isProteinDB()  );
						sharedHitPanel = installPairAlignPanel( pairs );
						break;
					default:
						System.err.println("Internal error in Pair Top Row: " + saveDisplay);
					} 	
					theFrame.swapInTab( tempTab, null, PairTopRowTab.this );
				}
			}
		);
	}

	/***************************************************
	 * Prev Next tabs
	 **************************************************/
	
	private void addPrevNextTab(int nNewRecordNum) {
		Tab parentTab = (Tab)getParentTab();
		int pairNum = ((PairListTab)parentTab).getPairNumAtRow(nNewRecordNum);
		String strTitle = "Pair # " + 	pairNum;

		String[] contigIDs = ((PairListTab)parentTab).getContigIDsAtRow( nNewRecordNum );
		MultiCtgData theCluster = new MultiCtgData(contigIDs[0], contigIDs[1]);
		
		getParentFrame().addPairAlignNextPrev( 
				theCluster, parentTab, nNewRecordNum, pairNum,
				strTitle, prevContigDisplaySettings, getMenuNode() );
	}
	/***************************************************************
	 * Install panels
	 ***************************************************************/

	private MainToolAlignPanel installPairAlignPanel ( 
			Vector<AlignData> theAlignmentsToView )
	{
		MainToolAlignPanel alignmentPanel = 
			MainToolAlignPanel.createPairAlignPanel (false, false, theAlignmentsToView );
		installMainPanel ( alignmentPanel );
		alignmentPanel.applyDisplaySettings(prevContigDisplaySettings);
		return alignmentPanel;
	}
	
	private void installMainPanel ( MainToolAlignPanel thePanel )
	{
		displayedJPanel = thePanel;
		thePanel.setSelectedContigs ( selectedContigs );// Pass in the select contigs from the
														// last one that we displayed		
		bottomPanel.removeAll();
		bottomPanel.setLayout( new BorderLayout () );
		bottomPanel.add( displayedJPanel, BorderLayout.CENTER );
		setVisible(false);
		setVisible(true);
	}
	
	public TreeSet<String> getSelectedContigIDs ( )
	{
		MainToolAlignPanel panel = (MainToolAlignPanel)displayedJPanel;
		return panel.getSelectedContigIDs();
	}

	public void setPrevDisplaySettings(int [] prevContigDisplaySettings)
	{
		this.prevContigDisplaySettings = prevContigDisplaySettings;
	}
	
	public void close()
	{
		displayDropDown = null;
		bottomPanel = null;
		displayedJPanel = null;
		if(selectedContigs != null) selectedContigs.clear();
		if(pairCtgObj != null) pairCtgObj.clear();
		if(loadedPairObj != null) loadedPairObj.clear();

		allFramePanel = orientFramePanel = bestFramePanel = sharedHitPanel  = null;
	}
	
	/***************************************************
	 * XXX Pairwise/unitran views
	 */
	static final private String ShowPWsharedHits = 	"Align shared hits to pair (max 3)";
	static final private String ShowPWbestFrame = 	"Align in Best Frame";
	static final private String ShowPWorientFrames = "Align in Best Orient Frames";
	static final private String ShowPWallFrames = 	"Align in All Frames";
	
	static final private int SHOW_BEST_FRAME = 1;
	static final private int SHOW_ORIENT_FRAMES = 2;
	static final private int SHOW_ALL_FRAMES = 3;
	static final private int SHOW_SHARED_HITS = 4;
	
	private MenuMapper menuBestalign = null; 
	
	private MainToolAlignPanel bestFramePanel = null;
	private MainToolAlignPanel orientFramePanel = null;
	private MainToolAlignPanel allFramePanel = null;
	private MainToolAlignPanel sharedHitPanel = null;

	/*******************************************************/
	private int nRecordNum, nPairNum;	
	private MetaData metaData = null;
	private JComboBox displayDropDown = null;

	private JPanel bottomPanel = null;
	private JPanel displayedJPanel = null;
	
	private TreeSet<String> selectedContigs = new TreeSet<String> ();
	private MultiCtgData pairCtgObj = null;
	private MultiCtgData loadedPairObj = null;
	private STCWFrame theMainFrame = null;

	private int [] prevContigDisplaySettings = null;
}
