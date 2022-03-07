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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
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
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;
import util.database.DBConn;

public class SeqTopRowTab extends Tab {
	private static final long serialVersionUID = -6991503611757134213L;
	
	private static final String tophelpHTML =  Globals.helpDir + "DetailTopRow.html";
	private static final String detailhelpHTML = Globals.helpDir + "DetailPanel.html";
	private static final String framehelpHTML = Globals.helpDir + "DetailFramePanel.html";
	private static final String alignhelpHTML = Globals.helpDir + "Align.html";
	private static final String gohelpHTML =  	Globals.helpDir + "DetailGoPanel.html";
	private static final String go2helpHTML =  	Globals.helpDir + "goHelp/index.html";

	private Color panelColor = Static.PANELCOLOR; 
	private Color activeColor = Static.ACTIVECOLOR;  
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
           MultiCtgData listData, // One ContigData with name only; except if CAP3 was run
           Tab parentTab, 
           int recordNum)
	{
		super(theFrame, parentTab);
		super.setBackground(Color.white);
		
		theMainFrame = theFrame;
		metaData = theFrame.getMetaData();
		nParentRow = recordNum; 								
		ctgNameData = listData;
			
		setBackground(Color.white);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		if (parentTab == null) 	{ // assembled contig with cap
			parent = capTab;
			createCapButton();
		}
		else {
			parent = seqTab;
			createTopButtons();
		}
		
		add ( Box.createVerticalStrut(5) );
		add ( topRowPanel );
		
		bottomPanel = Static.createPagePanel();
		add ( Box.createVerticalStrut(5) );
		add ( bottomPanel );	
		
		setInActive(rbDetails);
		
		if (parent==capTab) opFirstCAP3(listData);
		else 				opFirst(); 
	}
	private void createCapButton() {
		topRowPanel = Static.createRowPanel();
		rbContig = Static.createButtonPanel("Contig", true); 
		rbContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive(rbContig);
				opContig();
			}
		});
		topRowPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)topRowPanel.getPreferredSize ().getHeight() ) );
	}
	private void createTopButtons() {
		topRowPanel = Static.createRowPanel();		topRowPanel.add(Box.createHorizontalStrut(5));
		
		rbDetails = Static.createButtonPanel("Details", true); 
		rbDetails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive(rbDetails);
				opDetails();
			}
		});
		topRowPanel.add(rbDetails); 				topRowPanel.add( Box.createHorizontalStrut(5) );
		
		rbFrame = Static.createButtonPanel("Frame", true); 
		rbFrame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive(rbFrame);
				opFrame();
			}
		});
		topRowPanel.add( rbFrame );				topRowPanel.add( Box.createHorizontalStrut(5) );
		
		// if assembled
		rbContig = Static.createButtonPanel("Contig", true); 
		rbContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive(rbContig);
				opContig();
			}
		});
		rbSNP = Static.createButtonPanel("SNPs", true); 
		rbSNP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setInActive(rbSNP);
				opSNPs();
			}
		});
		if (metaData.hasAssembly()) {
			topRowPanel.add( rbContig ); 		topRowPanel.add( Box.createHorizontalStrut(5) );
			topRowPanel.add( rbSNP );			topRowPanel.add( Box.createHorizontalStrut(5) );
		}	
		
		createAlignButton();
		topRowPanel.add( rbAlign ); 			
		
		createGObutton();
		if (metaData.hasGOs()) {
			topRowPanel.add( Box.createHorizontalStrut(5) );
			topRowPanel.add( rbGO );	
		}
		
		topRowPanel.add( Box.createHorizontalGlue() );
		createHelp();
		topRowPanel.add( btnHelp ); 			topRowPanel.add( Box.createHorizontalStrut(5) );
		
		// Create prev and next buttons
		if (nParentRow>=0) {
			JButton btnPrev = Static.createButton("<<", true);
			btnPrev.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {				
					if (getParentTab() instanceof SeqTableTab) {
						addPrevNextTab( ((SeqTableTab)getParentTab()).getPrevRowNum( nParentRow ) );
					}
					else Out.PrtErr("<< Prev TCW error"); 
				}
			});
			JButton btnNext = Static.createButton(">>", true);
			btnNext.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (getParentTab() instanceof SeqTableTab) { 
						addPrevNextTab( ((SeqTableTab) getParentTab()).getNextRowNum( nParentRow ) );
					}
					else Out.PrtErr("Next >> TCW error"); 
				}
			});
			topRowPanel.add( btnPrev ); 			topRowPanel.add( Box.createHorizontalStrut(1) );
			topRowPanel.add( btnNext ); 			topRowPanel.add( Box.createHorizontalStrut(5) );	
		}
		topRowPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)topRowPanel.getPreferredSize ().getHeight() ) );
		
	}
	private void createGObutton() {
		final JPopupMenu gopopup = new JPopupMenu();
		gopopup.add(new JMenuItem(new AbstractAction("Assigned GOs for all hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbGO);
				opGO(SHOW_ASSIGNED_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("All GOs for all hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbGO);
				opGO(SHOW_ALL_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("Assigned GOs for selected hit") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbGO);
				opGO(SHOW_SEL_GO);
			}
		}));
		gopopup.add(new JMenuItem(new AbstractAction("All GOs for selected hit") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbGO);
				opGO(SHOW_SEL_ALL_GO);
			}
		}));
		
		rbGO = Static.createButtonPanel("GO...", true); 
		rbGO.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { // CAS337 moved setInActive to Items	
        		gopopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
	}
	private void createAlignButton() {
		final JPopupMenu alignpopup = new JPopupMenu();
		alignpopup.add(new JMenuItem(new AbstractAction("Best Hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbAlign);
				opAlign(ALIGN_BEST_SEQUENCES);
			}
		}));
		alignpopup.add(new JMenuItem(new AbstractAction("DB hits: Selected hit(s)") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				setInActive(rbAlign);
				opAlign(ALIGN_SELECTED);
			}
		}));
		if (!metaData.isAAsTCW()) {
			alignpopup.add(new JMenuItem(new AbstractAction("DB hits: Selected hit(s) in all frames") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					setInActive(rbAlign);
					opAlign(ALIGN_SELECTED_ALL);
				}
			}));
		}
		rbAlign = Static.createButtonPanel("Align Hits...", true);
		rbAlign.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {// CAS337 moved setInActive to Items      		
        		alignpopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });	
	}
	private void createHelp() {
		final JPopupMenu popup = new JPopupMenu();
		
		popup.add(new JMenuItem(new AbstractAction("Top buttons") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Top Buttons", tophelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing buttons"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Details") { // CAS342 left this off
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Details", detailhelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing detail"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Frame") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Frame", framehelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing frame"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Align Hits...") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Align Hits...", alignhelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing remark"); }
			}
		}));
		if (metaData.hasGOs()) {
			popup.add(new JMenuItem(new AbstractAction("GO...") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, "GO...", gohelpHTML);
					} catch (Exception er) {ErrorReport.reportError(er, "Error showing go"); }
				}
			}));
			popup.add(new JMenuItem(new AbstractAction("GO Info") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					try {
						UserPrompt.displayHTMLResourceHelp(theMainFrame, "GO Info", go2helpHTML);
					} catch (Exception er) {ErrorReport.reportError(er, "Error showing go info"); }
				}
			}));
		}
		
		btnHelp = Static.createButtonHelp("Help...", true);
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnHelp.setAlignmentX(Component.RIGHT_ALIGNMENT);
	}
	private void setInActive(JButton button) {
		// this works on mac but not linux
		rbDetails.setSelected(false);
		rbFrame.setSelected(false);
		rbGO.setSelected(false);
		rbAlign.setSelected(false);
		rbContig.setSelected(false);
		rbSNP.setSelected(false);
		
		// this works on linux but not mac
		rbDetails.setBackground(panelColor);
		rbFrame.setBackground(panelColor);
		rbGO.setBackground(panelColor);
		rbAlign.setBackground(panelColor);
		rbContig.setBackground(panelColor);
		rbSNP.setBackground(panelColor);
		
		button.setBackground(activeColor); // CAS337 put here instead of on every button
		button.setSelected(true);
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
				theFrame.removeTabWait(waitTab);
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
					String [] x = checkNewAlign(alignedHits1, true);
					if (x!=null) { // new list
						hitBestPanel=null; // CAS314 said hitSelectedPanel
						alignedHits1 = x;
					}
					if (hitBestPanel==null) {
						Vector<AlignData> hitbest =  
								AlignCompute.DBhitsAlignDisplay(alignedHits1, ctgFullData.getContig(), 
										AlignCompute.frameResult, metaData.isAAsTCW(), detailPanel );
						hitBestPanel = PairViewPanel.createPairAlignPanel (true, false, hitbest );
					}
					installPairPanel(hitBestPanel);
					
					break;
				case ALIGN_SELECTED:
					x = checkNewAlign(alignedHits1, false);
					if (x!=null) { // new list
						hitSelectedPanel=null;
						alignedHits1 = x;
					}
					if ( hitSelectedPanel == null ) {
						Vector<AlignData> hitSL =  
							AlignCompute.DBhitsAlignDisplay(alignedHits1, ctgFullData.getContig(), 
									AlignCompute.frameResult, metaData.isAAsTCW(), detailPanel);
						hitSelectedPanel = PairViewPanel.createPairAlignPanel (true,false, hitSL );
					}
					installPairPanel(hitSelectedPanel);
					
					break;
				case ALIGN_SELECTED_ALL:
					x = checkNewAlign(alignedHits2, false);
					if (x!=null) { // new list
						hitAllFramePanel=null;
						alignedHits2 = x;
					}
					if (hitAllFramePanel==null) {
						Vector<AlignData> hitSLA =  // selected hits in all frames
							AlignCompute.DBhitsAlignDisplay(alignedHits2, ctgFullData.getContig(),  
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
							ctgFullData, nParentRow, getID());
				
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
		
		MenuTreeNode node = getMenuNode();// Re-use existing menu tree node
		getParentFrame().removeTabPrevNext(node); // Remove from left pane
		
		Tab newTab = getParentFrame().addNextSeqDetailTab( strTitle, node, parentTab, nNewRecordNum);
		
		newTab.setMenuNode(node);
		node.setText(strTitle);
		node.setUserObject(newTab);
	}
	
	private String [] checkNewAlign(String [] viewed, boolean isBest) {
		String [] cur;
		if (isBest) cur = detailPanel.getBestHits();
		else        cur = detailPanel.getSelectedHits();
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
	private int nParentRow;	

	private STCWFrame theMainFrame = null;
	private MetaData metaData = null;
	
	private JButton rbDetails = null, rbFrame = null;
	private JButton rbGO = null, rbAlign = null;
	private JButton rbContig = null, rbSNP = null;
	private JButton btnHelp = null;
	
	private JPanel topRowPanel, bottomPanel = null;
	private PairViewPanel pairAlignPanel = null;
	private ContigViewPanel ctgAlignPanel=null;
	
	private MultiCtgData ctgNameData = null;
	private MultiCtgData ctgFullData = null;

	private String [] alignedHits1 = null; // Best or Selected Aligned Hits - used to determine whether its changed
	private String [] alignedHits2 = null; // Selected Hits for all frames -  ditto
	private String goHit = null;
	
	/**********************************************************/
	private int lastDisplay=0;
}
