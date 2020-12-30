package sng.viewer.panels.seqDetail;
/***************************************************
 * Show: Detail Contig SNP Frame GO... Align...
 * Detail: 	SeqDetailPanel
 * Contig: 	ContigViewPanel, ContigAlignPanel
 * SNP: 	SNPMultiPanel
 * Frame: 	SeqFramePanel
 * GO: 		SeqGOPanel
 * Align:   PairViewPanel, PairAlignPanel(shared with Pairs)
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.MultiCtgData;
import sng.util.CenteredMessageTab;
import sng.util.MenuTreeNode;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import sng.viewer.panels.align.AlignCompute;
import sng.viewer.panels.align.AlignData;
import sng.viewer.panels.align.PairViewPanel;
import sng.viewer.panels.align.ContigViewPanel;
import sng.viewer.panels.seqTable.SeqTableTab;
import util.methods.Static;
import util.ui.UserPrompt;
import util.database.DBConn;

public class SeqTopRowTab extends Tab {
	private static final long serialVersionUID = -6991503611757134213L;
	private Color buttonColor = Color.LIGHT_GRAY; // new Color(230, 230, 255); 
	private short parent;
	private short seqTab = 1;
	private short capTab = 2;
	
	static final public int SHOW_ASSIGNED_GO = 1;
	static final public int SHOW_ALL_GO = 2;
	static final public int SHOW_SEL_GO = 3;
	static final public int SHOW_SEL_ALL_GO = 4;
	
	static final private int ALIGN_BEST_SEQUENCES = 1; 
	static final private int ALIGN_SELECTED = 2;
	static final private int ALIGN_SELECTED_ALL = 3;
	
	public SeqTopRowTab ( STCWFrame theFrame,
                			MultiCtgData listData, Tab parentTab, int recordNum,
                			int [] prevSettings)
	{
		super(theFrame, parentTab);
		prevDisplaySettings = prevSettings;
		theMainFrame = theFrame;
		metaData = theFrame.getMetaData();
		nRecordNum = recordNum; 								
		ctgNameData = listData;
			
		if (parentTab == null) parent = capTab;
		else 	parent=seqTab;
	
		/********************************************
		 *  XXX Create dropdown menu of contig
		 */
		rbDetails = new JButton("Details"); 
		rbDetails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive();
				rbDetails.setBackground(buttonColor);
				rbDetails.setSelected(true);
				opDetails();
			}
		});
		
		rbFrame = new JButton("Frame"); 
		rbFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive();
				rbFrame.setBackground(buttonColor);
				rbFrame.setSelected(true);
				opFrame();
			}
		});
		// if assembled
		rbContig = new JButton("Contig"); 
		rbContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive();
				rbContig.setBackground(buttonColor);
				rbContig.setSelected(true);
				opContig();
			}
		});
		rbSNP = new JButton("SNPs"); 
		rbSNP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive();
				rbSNP.setBackground(buttonColor);
				rbSNP.setSelected(true);
				opSNPs();
			}
		});
		// GO options
		final JPopupMenu gopopup = new JPopupMenu();
		gopopup.add(new JMenuItem(new AbstractAction("Assigned GOs for all hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opGO(SHOW_ASSIGNED_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("All GOs for all hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opGO(SHOW_ALL_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("Assigned GOs for selected hit") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opGO(SHOW_SEL_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("All GOs for selected hit") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opGO(SHOW_SEL_ALL_GO);
			}
		}));
		
		rbGO = Static.createButton("GO...", true); 
		rbGO.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
        			setInActive();
        			rbGO.setBackground(buttonColor);
        			rbGO.setSelected(true);
        			gopopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
		/*  Align */
		final JPopupMenu alignpopup = new JPopupMenu();
		alignpopup.add(new JMenuItem(new AbstractAction("Best Hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opAlign(ALIGN_BEST_SEQUENCES);
			}
		}));
		
		alignpopup.add(new JMenuItem(new AbstractAction("DB hits: Selected hit(s)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				opAlign(ALIGN_SELECTED);
			}
		}));
		if (!metaData.isAAsTCW()) {
			alignpopup.add(new JMenuItem(new AbstractAction("DB hits: Selected hit(s) in all frames") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					opAlign(ALIGN_SELECTED_ALL);
				}
			}));
		}
		
		rbAlign = Static.createButton("Align Hits...", true);
		rbAlign.setBackground(Globals.FUNCTIONCOLOR);
		rbAlign.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {      		
        			setInActive();
        			rbAlign.setBackground(buttonColor);
        			rbAlign.setSelected(true);
        			alignpopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		
		// Create prev and next buttons
		JButton btnPrev = Static.createButton("<<Prev", true);
		btnPrev.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if (getParentTab() instanceof SeqTableTab) {
					addPrevNextTab( ((SeqTableTab)getParentTab()).getPrevRowNum( nRecordNum ) );
				}
				else System.err.println("<< Prev TCW error"); 
			}
		});

		JButton btnNext = Static.createButton("Next>>", true);
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//if(displayedJPanel != null) {
				//	setPrevDisplaySettings(((PairViewPanel)displayedJPanel).getDisplaySettings());
				//}
				if (getParentTab() instanceof SeqTableTab) { 
					addPrevNextTab( ((SeqTableTab) getParentTab()).getNextRowNum( nRecordNum ) );
				}
				else System.err.println("Next >> TCW error"); 
			}
		});
		if (recordNum < 0)
		{
			btnPrev.setEnabled(false);
			btnNext.setEnabled(false);
		}
		
		JButton btnHelp = Static.createButton("Help1", true, Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theMainFrame,"Sequence Options", 
						"html/viewSingleTCW/ContigTopRowPanel.html");
			}
		});
		
		// Top panel with buttons and drop-down		
		setInActive(); 
		JPanel topPanel = new JPanel ( );
		topPanel.setLayout( new BoxLayout ( topPanel, BoxLayout.X_AXIS ) );
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( rbDetails ); 
		topPanel.add( Box.createHorizontalStrut(5) );
		
		if (parent == capTab) {
			topPanel.add( rbContig ); 
		}
		else {	
			if (metaData.hasAssembly()) {
				topPanel.add( rbContig ); 
				topPanel.add( Box.createHorizontalStrut(5) );
				topPanel.add( rbSNP );
				topPanel.add( Box.createHorizontalStrut(5) );
			}		
			topPanel.add( rbFrame );
			topPanel.add( Box.createHorizontalStrut(5) );
			
			if (metaData.hasGOs()) {
				topPanel.add( rbGO );
				topPanel.add( Box.createHorizontalStrut(5) );
			}
			topPanel.add( rbAlign ); 
			
			topPanel.add( Box.createHorizontalGlue() );
			//topPanel.add( Box.createHorizontalStrut(5) );
			topPanel.add( btnPrev ); 	
			topPanel.add( Box.createHorizontalStrut(1) );
			topPanel.add( btnNext ); 	
			int x = (metaData.hasAssembly()) ? 5 : 20;
			topPanel.add( Box.createHorizontalStrut(x) );
			topPanel.add( btnHelp ); 	
			topPanel.add( Box.createHorizontalStrut(5) );
		}
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		topPanel.setBackground(Color.white);
		super.setBackground(Color.white);
			
		bottomPanel = new JPanel ();
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		add ( Box.createVerticalStrut(5) );
		add ( topPanel );
		add ( Box.createVerticalStrut(5) );
		add ( bottomPanel );	
		
		rbDetails.setBackground(buttonColor);
		rbDetails.setSelected(true);
		
		if (parent==capTab) opFirstCAP3(listData);
		else opFirst(); 
	}
	private void setInActive() {
		// this works on mac but not linux
		rbDetails.setSelected(false);
		rbFrame.setSelected(false);
		rbGO.setSelected(false);
		rbAlign.setSelected(false);
		rbContig.setSelected(false);
		rbSNP.setSelected(false);
		
		// this works on linux but not mac
		rbDetails.setBackground(Color.white);
		rbFrame.setBackground(Color.white);
		rbGO.setBackground(Color.white);
		rbAlign.setBackground(Color.white);
		rbContig.setBackground(Color.white);
		rbSNP.setBackground(Color.white);
	}
	private void opFirstCAP3(MultiCtgData listData) {
		ctgFullData = listData; 
		if (detailPanel==null)
			detailPanel = new SeqDetailPanel(theMainFrame,  ctgFullData);
				
		bottomPanel.removeAll();	
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		bottomPanel.add ( detailPanel );
		ctgAlignPanel = null;
		setVisible(false);
		setVisible(true);
	}
	private void opFirst() {
		String message = "Loading Sequence data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
		final STCWFrame theFrame = getParentFrame(); 
		
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				
				DBConn dbc = theMainFrame.getNewDBC();
				LoadFromDB dbLoadObj = new LoadFromDB(dbc, theFrame.getMetaData());
				ctgFullData = dbLoadObj.loadContig(ctgNameData);
				dbc.close();
				if (ctgFullData==null) return;
				
				opDetails();
				theFrame.removeTab(waitTab);
			}
		});
	}
// Detail panel
	private void opDetails() {
		String message = "Loading Detail data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
		final STCWFrame theFrame = getParentFrame(); 
		
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				if (detailPanel==null) {
					detailPanel = new SeqDetailPanel(theMainFrame,  ctgFullData);
					if (!detailPanel.hasHits()) {
						rbGO.setEnabled(false);
						rbAlign.setEnabled(false);
					}
				}		
				bottomPanel.removeAll();	
				bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
				bottomPanel.add ( detailPanel );
				ctgAlignPanel=null;
				pairAlignPanel= null;
				setVisible(false);
				setVisible(true);
				
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}
	
// Frame panel
	private void opFrame() {
		String message = "Loading frame data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
	
		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				if (framePanel==null)
					framePanel = new SeqFramePanel(theMainFrame, ctgFullData, detailPanel);
						
				bottomPanel.removeAll();	
				bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
				bottomPanel.add ( framePanel );
				ctgAlignPanel=null;
				pairAlignPanel= null;
				setVisible(false);
				setVisible(true);
				
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}
	
	// GO panel
	private void opGO(int dType) {
		final int displayType = dType;
		if (displayType>=SHOW_SEL_GO && detailPanel.cntSelectedHits() !=1) {
			String msg="";
			if (detailPanel.cntSelectedHits() != 1) 
				msg = "Select one hit from the Detail DB Hit table";
			else 
				msg = "Cannot show GOs for multiple selected. " +
						"Either select one or unselect all to see all GOs.";
			JOptionPane.showMessageDialog(getParentFrame(), msg, 
					"Selected GO", JOptionPane.PLAIN_MESSAGE);
			return;
		}
	
		if (lastDisplay != displayType) {
			goPanel=null;
			goHit = null;
		}
		if (displayType>=SHOW_SEL_GO) {
			String [] cur = detailPanel.getSelectedHits();
			if (cur[0] != goHit) goPanel = null;
			goHit = cur[0];
		}
		lastDisplay = displayType;
		
		String message = "Loading GO data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
	
		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				if (goPanel==null) {
					goPanel = new SeqGOPanel(theMainFrame,  displayType, goHit, 
								detailPanel, ctgFullData.getContig());
				}
				bottomPanel.removeAll();	
				bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
				bottomPanel.add ( goPanel );
				ctgAlignPanel=null;
				pairAlignPanel= null;
				setVisible(false);
				setVisible(true);
				
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}
// Align panel
	private void opAlign(int type) {
		if (type!=ALIGN_BEST_SEQUENCES && detailPanel.cntSelectedHits() == 0) {
			JOptionPane.showMessageDialog(getParentFrame(), 
					"Select one or more hits from the Detail DB Hits table", 
					"Selected Hits", JOptionPane.PLAIN_MESSAGE);
			return;
		}
		String message = "Loading alignment data ; please wait... ";
		final int alignType = type;
		final Tab waitTab = new CenteredMessageTab ( message );

		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				switch ( alignType)
				{						
				case ALIGN_BEST_SEQUENCES:
					String [] x = checkNewAlign(displayedHits1, true);
					if (x!=null) { // new list
						hitSelectedPanel=null;
						displayedHits1 = x;
					}
					if (hitBestPanel==null) {
						Vector<AlignData> hitbest =  
								AlignCompute.DBhitsAlignDisplay(displayedHits1, ctgFullData, 
										AlignCompute.frameResult, metaData.isAAsTCW(), detailPanel );
						hitBestPanel = PairViewPanel.createPairAlignPanel (true, false, hitbest );
					}
					installPairPanel(hitBestPanel);
					
					break;
				case ALIGN_SELECTED:
					x = checkNewAlign(displayedHits1, false);
					if (x!=null) { // new list
						hitSelectedPanel=null;
						displayedHits1 = x;
					}
					if ( hitSelectedPanel == null ) {
						Vector<AlignData> hitSL =  
							AlignCompute.DBhitsAlignDisplay(displayedHits1, ctgFullData, 
									AlignCompute.frameResult, metaData.isAAsTCW(), detailPanel);
						hitSelectedPanel = PairViewPanel.createPairAlignPanel (true,false, hitSL );
					}
					installPairPanel(hitSelectedPanel);
					
					break;
				case ALIGN_SELECTED_ALL:
					x = checkNewAlign(displayedHits2, false);
					if (x!=null) { // new list
						hitAllFramePanel=null;
						displayedHits2 = x;
					}
					if (hitAllFramePanel==null) {
						Vector<AlignData> hitSLA =  // selected hits in all frames
							AlignCompute.DBhitsAlignDisplay(displayedHits2, ctgFullData,  
									AlignCompute.allResult, metaData.isAAsTCW(), detailPanel );
						hitAllFramePanel = PairViewPanel.createPairAlignPanel (true, true, hitSLA );
					}
					installPairPanel(hitAllFramePanel);;
					break;		
				default:
					System.err.println("Internal error for alignment: " + alignType);
				} 
		
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}

	private void installPairPanel ( PairViewPanel thePanel )
	{
		pairAlignPanel = thePanel;		
		bottomPanel.removeAll();
		bottomPanel.setLayout( new BorderLayout () );
		bottomPanel.add( pairAlignPanel, BorderLayout.CENTER );
		
		setVisible(false);
		setVisible(true);
	}
	private void installContigPanel (ContigViewPanel thePanel )
	{
		ctgAlignPanel = thePanel;		
		bottomPanel.removeAll();
		bottomPanel.setLayout( new BorderLayout () );
		bottomPanel.add( ctgAlignPanel, BorderLayout.CENTER );
		
		setVisible(false);
		setVisible(true);
	}
	// SNP panel
	private void opSNPs() {
		String message = "Loading SNP data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
	
		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				if (snpPanel==null) 
					snpPanel = new SNPMultiPanel ( ctgFullData );
						
				bottomPanel.removeAll();	
				bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
				bottomPanel.add ( snpPanel );
				ctgAlignPanel=null;
				pairAlignPanel= null;
				setVisible(false);
				setVisible(true);
				
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}
// Contig panel
	private void opContig() {
		String id = ctgFullData.getCtgIDAt(0);
		if (id.equals(CAP3Tab.NO_CAP3)) return;
		
		String message = "Loading Contig data ; please wait... ";
		final Tab waitTab = new CenteredMessageTab ( message );
	
		final STCWFrame theFrame = getParentFrame(); 
		theFrame.addTabAndRun(SeqTopRowTab.this, null, waitTab, 
		  new STCWFrame.RunnableCanThrow() {
			public void run() throws Throwable {
				ContigViewPanel ctgPanel = 
					ContigViewPanel.createESTPanel( metaData.hasCAP3(),
							ctgFullData, nRecordNum, getID());
				
				installContigPanel ( ctgPanel );
				
				theFrame.swapInTab( waitTab, getTitle(), SeqTopRowTab.this );
			}
		});
	}
	
	/***************************************************
	 * Prev Next tabs
	 **************************************************/
	private void addPrevNextTab(int nNewRecordNum )
	{	
		Tab parentTab = getParentTab();
		String strTitle = ((SeqTableTab)parentTab).getContigIDAtRow(nNewRecordNum);
		if (strTitle==null) return;
		
		Tab newTab = getParentFrame().addNewContigTab( strTitle, parentTab, 
					nNewRecordNum, prevDisplaySettings );
		
		MenuTreeNode node = getMenuNode();// Re-use existing menu tree node
		getParentFrame().removeTabFromNode(node);
		
		newTab.setMenuNode(node);
		node.setText(strTitle);
		node.setUserObject(newTab);
	}
	
	private String [] checkNewAlign(String [] viewed, boolean isBest) {
		String [] cur;
		if (isBest) cur = detailPanel.getBestHits();
		else cur = detailPanel.getSelectedHits();
		if (viewed == null) return cur;
		if (viewed.length != cur.length) return cur;
		
		for (int i=0; i< cur.length; i++) {
			for (int j=0; j< viewed.length; j++) {
				if (!cur[i].equals(viewed[j])) return cur;
			}
		}
		return null;
	}
	
	public void close()
	{
		bottomPanel = null;
		ctgAlignPanel=null;
		pairAlignPanel=null;
		
		if(ctgNameData != null) ctgNameData.clear();
		if(ctgFullData != null) ctgFullData.clear();

		hitBestPanel = null;
		snpPanel = null;
	}
	
	/*****************************************************/
	
	private SeqDetailPanel detailPanel = null;
	private SeqFramePanel framePanel = null;
	private SeqGOPanel goPanel = null;
	private SNPMultiPanel snpPanel = null; 
	
	private PairViewPanel hitBestPanel = null;
	private PairViewPanel hitSelectedPanel = null;
	private PairViewPanel hitAllFramePanel = null;	
	
	/*******************************************************/
	private int nRecordNum;	

	private STCWFrame theMainFrame = null;
	private MetaData metaData = null;
	
	private JButton rbDetails = null, rbFrame = null;
	private JButton rbGO = null, rbAlign = null;
	private JButton rbContig = null, rbSNP = null;
	
	private JPanel bottomPanel = null;
	private PairViewPanel pairAlignPanel = null;
	private ContigViewPanel ctgAlignPanel=null;
	
	private MultiCtgData ctgNameData = null;
	private MultiCtgData ctgFullData = null;

	private String [] displayedHits1 = null;
	private String [] displayedHits2 = null;
	private String goHit = null;
	
	/**********************************************************/
	private int [] prevDisplaySettings = null; // not working right now
	private int lastDisplay=0;
}
