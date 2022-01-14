package sng.viewer.panels.pairsTable;

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
import sng.viewer.panels.align.AlignCompute;
import sng.viewer.panels.align.AlignData;
import sng.viewer.panels.align.PairViewPanel;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.MenuMapper;
import util.database.DBConn;

/**
 * PairTopRow of Alignment
 * Displays buttons at top the alignment
 */

public class PairTopRowTab extends Tab  implements ClipboardOwner
{
	private static final long serialVersionUID = -4644940934101056207L;
	
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
		isAAdb = metaData.isAAsTCW();
		
		try {
			DBConn dbc = theMainFrame.getNewDBC();
			LoadPairFromDB dbObj =  new LoadPairFromDB(dbc, theFrame.getMetaData());
			loadedPairObj = 		dbObj.loadTwoContigs(pairCtgObj);
			BlastHitData hitData =  dbObj.loadPairHitData(listData.getCtgIDAt(0), listData.getCtgIDAt(1));
			loadedPairObj.setPairHit(hitData);
			dbc.close();
		}
		catch (Exception e) {ErrorReport.reportError(e, "Pair Table alignment"); }
	
		/********************************************
		 *  XXX Create dropdown menu of pairwise display options
		 */
		displayDropDown = new JComboBox <MenuMapper>();
		displayDropDown.setBackground(Color.white);
		Dimension dim = new Dimension ( 250, (int)displayDropDown.getPreferredSize().getHeight() );
		displayDropDown.setPreferredSize( dim );
		displayDropDown.setMaximumSize ( dim );

		if (isAAdb) menuBestalign = new MenuMapper(ShowPWpair, SHOW_PAIR);
		else        menuBestalign = new MenuMapper(ShowPWbestFrame, SHOW_BEST_FRAME);
		displayDropDown.addItem(menuBestalign);
		
		if (!isAAdb) {
			MenuMapper menuAllalign = new MenuMapper ( ShowPWallFrames, SHOW_ALL_FRAMES );
			displayDropDown.addItem( menuAllalign );	
		}
		MenuMapper menuSharedHits = new MenuMapper ( ShowPWsharedHits, SHOW_SHARED_HITS);
		displayDropDown.addItem( menuSharedHits);		
			
		displayDropDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				displayPair();
			}
		});
		JButton jbView = Static.createButtonTab(Globals.seqTableLabel, true);
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
		copypopup.add(new JMenuItem(new AbstractAction("Seq ID 1") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg1 = pairCtgObj.getContigAt(0).getContigID();
				saveTextToClipboard(ctg1);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Seq ID 2") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg2 = pairCtgObj.getContigAt(1).getContigID();
				saveTextToClipboard(ctg2);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Sequence 1") { // CAS314
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg1 = pairCtgObj.getContigAt(0).getContigID();
				String seq =  pairCtgObj.getContigAt(0).getSeqData().getSequence();
				saveTextToClipboard(">" + ctg1 + "\n" + seq);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Sequence 2") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg2 = pairCtgObj.getContigAt(1).getContigID();
				String seq =  pairCtgObj.getContigAt(1).getSeqData().getSequence();
				saveTextToClipboard(">" + ctg2 + "\n" + seq);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Reverse Complement 1") { // CAS314
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg1 = pairCtgObj.getContigAt(0).getContigID();
				String seq =  pairCtgObj.getContigAt(0).getSeqData().getSeqRevComp();
				saveTextToClipboard(">" + ctg1 + "\n" + seq);
			}
		}));
		copypopup.add(new JMenuItem(new AbstractAction("Reverse Complement 2") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String ctg2 = pairCtgObj.getContigAt(1).getContigID();
				String seq =  pairCtgObj.getContigAt(1).getSeqData().getSeqRevComp();
				saveTextToClipboard(">" + ctg2 + "\n" + seq);
			}
		}));
		JButton jbCopy = Static.createButtonMenu("Copy...", true);
		jbCopy.addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                copypopup.show(e.getComponent(), e.getX(), e.getY());
	            }
	        });
		// Create prev and next buttons
		JButton btnPrev = Static.createButton("<<Prev");
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if(alignPanel != null)
					setPrevDisplaySettings(alignPanel.getDisplaySettings());
				
				addPrevNextTab( ((PairTableTab)getParentTab()).getPrevRowNum( nRecordNum ) );
			}
		});

		JButton btnNext = Static.createButton("Next>>");
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(alignPanel != null) {
					setPrevDisplaySettings(alignPanel.getDisplaySettings());
				}
				addPrevNextTab( ((PairTableTab) getParentTab()).getNextRowNum( nRecordNum ) );
			}
		});
		
		if (recordNum < 0){
			btnPrev.setEnabled(false);
			btnNext.setEnabled(false);
		}

		// Top panel with buttons and drop-down		
		JPanel topPanel = new JPanel ( );
		topPanel.setLayout( new BoxLayout ( topPanel, BoxLayout.X_AXIS ) );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( displayDropDown ); topPanel.add( Box.createHorizontalStrut(10) );	
			
		topPanel.add(jbView); topPanel.add( Box.createHorizontalStrut(2) );	
		topPanel.add(jbCopy); topPanel.add( Box.createHorizontalStrut(2) );
		
		topPanel.add( Box.createHorizontalGlue() );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( btnPrev ); 	
		topPanel.add( Box.createHorizontalStrut(2) );
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
	private void displayPair()
	{
		// So this a bit crazy, but otherwise the event for closing the
		// drop down menu never seems to come, and all of the other 
		// UI changes don't happen...
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				MenuMapper selection = (MenuMapper) displayDropDown.getSelectedItem();
				int displayType = selection.asInt();
				
				try { 
					loadPairAndDraw ( displayType);
				} 		
				catch ( Exception err ) { ErrorReport.reportError(err, "Load Pair " +displayType);
				}	
			}
		});
	}
	// XXX display for contig overview panel
	private void loadPairAndDraw ( int nDisplayAfterLoad) throws Exception
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
		case SHOW_PAIR:
            strWaitMsg = "Aligning pair, please wait...";
			if ( bestFramePanel != null ) {
				installMainPanel ( bestFramePanel );
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
					case SHOW_ORIENT_FRAMES: // not currently used
						pairs = AlignCompute.pairAlignDisplay( pairObj, AlignCompute.orientResult);
						orientFramePanel = installPairAlignPanel( pairs );
						break;
					case SHOW_ALL_FRAMES:
						pairs = AlignCompute.pairAlignDisplay( pairObj, AlignCompute.allResult);
						allFramePanel = installPairAlignPanel( pairs );
						break;
					case SHOW_SHARED_HITS: 
						pairs = AlignCompute.pairAlignSharedHits(pairObj, metaData.isAAsTCW()  );
						sharedHitPanel = installPairAlignPanel( pairs );
						break;
					case SHOW_PAIR: 
						pairs = AlignCompute.pairAlignDisplayAA(pairObj);
						bestFramePanel = installPairAlignPanel( pairs );
						break;
					default:
						Out.bug("Internal error in Pair Top Row: " + saveDisplay);
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
		Tab parentTab = getParentTab();
		int pairNum = ((PairTableTab)parentTab).getPairNumAtRow(nNewRecordNum);
		String strTitle = "Pair # " + 	pairNum;

		String[] contigIDs = ((PairTableTab)parentTab).getContigIDsAtRow( nNewRecordNum );
		MultiCtgData theCluster = new MultiCtgData(contigIDs[0], contigIDs[1]);
		
		getParentFrame().addPairAlignNextPrev( 
				theCluster, parentTab, nNewRecordNum, pairNum,
				strTitle, prevContigDisplaySettings, getMenuNode() );
	}
	/***************************************************************
	 * Install panels
	 ***************************************************************/

	private PairViewPanel installPairAlignPanel ( 
			Vector<AlignData> theAlignmentsToView )
	{
		PairViewPanel alignmentPanel = 
			PairViewPanel.createPairAlignPanel (false, false, theAlignmentsToView );
		installMainPanel ( alignmentPanel );
		alignmentPanel.applyDisplaySettings(prevContigDisplaySettings);
		return alignmentPanel;
	}
	
	private void installMainPanel ( PairViewPanel thePanel )
	{
		alignPanel = thePanel;
			
		bottomPanel.removeAll();
		bottomPanel.setLayout( new BorderLayout () );
		bottomPanel.add( alignPanel, BorderLayout.CENTER );
		setVisible(false);
		setVisible(true);
	}

	private void setPrevDisplaySettings(int [] prevContigDisplaySettings)
	{
		this.prevContigDisplaySettings = prevContigDisplaySettings;
	}
	
	public void close()
	{
		displayDropDown = null;
		bottomPanel = null;
		alignPanel = null;
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
	static final private String ShowPWpair = 			"Align pair";
	//static final private String ShowPWorientFrames = "Align in Best Orient Frames";
	static final private String ShowPWallFrames = 	"Align in All Frames";
	
	static final private int SHOW_BEST_FRAME = 1;
	static final private int SHOW_ORIENT_FRAMES = 2;
	static final private int SHOW_ALL_FRAMES = 3;
	static final private int SHOW_SHARED_HITS = 4;
	static final private int SHOW_PAIR = 5; // CAS314
	
	private MenuMapper menuBestalign = null; 
	
	private PairViewPanel bestFramePanel = null;
	private PairViewPanel orientFramePanel = null;
	private PairViewPanel allFramePanel = null;
	private PairViewPanel sharedHitPanel = null;

	/*******************************************************/
	private int nRecordNum, nPairNum;	
	private MetaData metaData = null;
	private JComboBox <MenuMapper> displayDropDown = null;

	private JPanel bottomPanel = null;
	private PairViewPanel alignPanel = null;
	
	private TreeSet<String> selectedContigs = new TreeSet<String> ();
	private MultiCtgData pairCtgObj = null;
	private MultiCtgData loadedPairObj = null;
	private STCWFrame theMainFrame = null;

	private int [] prevContigDisplaySettings = null;
	private boolean isAAdb=false;
}
