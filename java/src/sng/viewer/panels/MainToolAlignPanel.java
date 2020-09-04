package sng.viewer.panels;
/**
 * Shared by contig (EST contig and seq-hit pairs) and pairs (sequence pairs) for drawing
 * Shows 2nd row of buttons for both EST contig aligment and pair alignment
 * 
 * For pairs:
 * SeqTopRow (seq-hit) and PairTopRow (seq-seq) initiates drawing with createPairAlignPanel
 * 	supplying vector of alignments, where AlignmentData - contains alignment
 * MainToolAlignPanel -- main panel
 * MainPairAlignPanel extends MainAlignPanel to draws subpanel of pair alignments
 * 
 * For contig:
 * SeqTopRow initiates drawing with createESTPanel
 * MainToolAlignPanel -- main panel
 * ContigAlignPanel extends MainAlignPanel to draws subpanels of contig alignment (max 1 panel)
 */
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import sng.database.Globals;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.viewer.STCWFrame;
import sng.viewer.panels.seqDetail.CAP3Tab;
import sng.viewer.panels.seqDetail.DrawContigPanel;
import util.align.AlignPairAA;
import util.align.AlignData;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.MenuMapper;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class MainToolAlignPanel extends JPanel implements ClipboardOwner 
{		
	public static int HIDE_BURIED_EST = 0;
	public static int SHOW_BURIED_EST_LOCATION = 1;
	public static int SHOW_BURIED_EST_DETAIL = 2;
	public static int GAP = 12;
	public static int EXTEND = 2;
	private boolean isHit;
	private boolean isBeta=true;
	
	public void lostOwnership(Clipboard clipboard, Transferable contents) { }
	
	// this can be two sequences or sequence and hit. AlignData contains the alignments.
	static public MainToolAlignPanel createPairAlignPanel (
			boolean isHit, 		// alignment against Hit
			boolean isAllFrame, // seqDetailAlign option to show all frames
			Vector<AlignData> inAlignmentLists) 
	{
		boolean alt = true, isAAdb=false;
		int cnt=1;
		String lastHit="";
		Vector<MainPairAlignPanel> subPanels = new Vector<MainPairAlignPanel> ();
		
		Iterator<AlignData> i = inAlignmentLists.iterator();
		while ( i.hasNext() )
		{
			AlignData alignData = i.next ();
			isAAdb = alignData.isAAdb();
			String hitID = alignData.getSequence2().getName(); // Only change if hitID is different
			if (!hitID.equals(lastHit)) {
				alt = !alt;
				lastHit=hitID;
			}
			MainPairAlignPanel curPanel = new MainPairAlignPanel ( 
					cnt, alignData, alt, nTopGap, nBottomGap, nSideGaps, nSideGaps );
			subPanels.add( curPanel );	
			cnt++;
		}
		
		MainToolAlignPanel panel = new MainToolAlignPanel (isHit, isAAdb,  -1, -1 );
		panel.createMainPanelFromSubPanels(null, subPanels, true, true );
		
		panel.add ( panel.createToolPair (isAllFrame ) );
		panel.add ( Box.createVerticalStrut(5) );
		panel.add ( panel.scroller );	
		return panel;
	}	
	// panel for EST alignment
	static public MainToolAlignPanel createESTPanel (boolean hasCAP3,
					MultiCtgData inCluster, int inRecordNum, int parentID,
					boolean hasNoAssembly) throws Exception 
	{
		Vector<DrawContigPanel> subPanels = new Vector<DrawContigPanel> ();
		Iterator<ContigData> i = inCluster.getContigIterator();
		int nTotalESTs = 0;		
		int nTotalBuried = 0; 	

		while ( i.hasNext() )
		{
			ContigData curData = i.next ();
			DrawContigPanel curPanel = new DrawContigPanel (curData, 													
								nTopGap, nBottomGap, nSideGaps, nSideGaps );
			subPanels.add( curPanel );	
			
			nTotalESTs += curData.getNumSequences();	
			nTotalBuried += curData.getNumBuried(); 		
		}
		MainToolAlignPanel panel = new MainToolAlignPanel (false, false, inRecordNum, parentID);
		
		panel.createMainPanelFromSubPanels( subPanels, null, false, !hasNoAssembly );
		
		if (hasNoAssembly) { 
			panel.add ( panel.createToolSeq() );
		}
		else {
			panel.add ( panel.createToolEST(hasCAP3, nTotalESTs, nTotalBuried) );
		}
		panel.add ( Box.createVerticalStrut(5) );
		panel.add ( panel.scroller );
		
		if (!hasNoAssembly) {
			panel.menuContigSort.setSelectedIndex( 2 );
		}
		return panel;
	}
	
	/*******************************************
	 * XXX Class starts here
	 */
	private MainToolAlignPanel (boolean b,  boolean a, int inRecordNum, int parentID) 
	{
		isHit = b;
		isAAdb = a;
		nRecordNum = inRecordNum; 
				
		scroller = new JScrollPane ( );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		scroller.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				handleClick(e);
			}
		});	
        UIHelpers.setScrollIncrements( scroller );
        
		scroller.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				fitBasesPerPixel();
				scroller.removeComponentListener(this);
			}
		});
        
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		super.setBackground(Color.WHITE);
	}
	// Could be Vector of SeqAlignPanel or MainPairAlignPanel, both of type MainAlignPanel
	// CAS304 just made separate arguments for -lint
	private void createMainPanelFromSubPanels (Vector<DrawContigPanel> ctgPanels,
			Vector <MainPairAlignPanel>  pairPanels, boolean isPair, boolean hasAssembly )
	{
		JPanel mainPanel = Static.createPagePanel();
		mainPanel.setVisible( false );
		
		// Add each panel to the parent panel
		listOfMainAlignPanels = new Vector <MainAlignPanel> ();
		
		if (ctgPanels!=null) {
			Iterator <DrawContigPanel> iter = ctgPanels.iterator();
			while ( iter.hasNext() )
			{
				MainAlignPanel curPanel = (MainAlignPanel) iter.next ();
				mainPanel.add( curPanel );
				listOfMainAlignPanels.add( curPanel );
			}
		}
		else {
			Iterator <MainPairAlignPanel>iter = pairPanels.iterator();
			while ( iter.hasNext() )
			{
				MainAlignPanel curPanel = (MainAlignPanel) iter.next ();
				mainPanel.add( curPanel );
				listOfMainAlignPanels.add( curPanel );
			}
		}

		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( createLegendPanel(isPair, hasAssembly) );
		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( Box.createVerticalGlue() ); 	
		
		scroller.setViewportView( mainPanel );
	}

	private JPanel createLegendPanel(boolean isPair, boolean hasAssembly)
	{
		JPanel panel = new JPanel();
		panel.setBackground( Color.WHITE );
		panel.setLayout( new BoxLayout ( panel, BoxLayout.X_AXIS ) );
		panel.add( Box.createHorizontalStrut(10) );
		
		// the legend is different for pairwise vs contig
		LegendPanel lp;
		if (!isPair && !hasAssembly) lp = new LegendPanel(3);
		else if (isPair) lp = new LegendPanel(2);
		else lp = new LegendPanel(1);		
		lp.setIsPair(isPair);
		lp.setHasNoAssembly(!hasAssembly);
		
		panel.add( lp );
		panel.add( Box.createHorizontalGlue() );
		return panel;
	}
	
	
	/*******************************************************
	 * Pairwise - either NTAA, NTNT or AAAA
	 */
	private JPanel createToolPair (boolean isAllFrames)
	{
		btnViewType = new JButton ("View Bases");
		btnViewType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleViewType();
			}
		});
		
		menuZoom = Static.createZoom();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					MenuMapper selection = (MenuMapper) menuZoom.getSelectedItem();
					changeBasesPerPixel(selection.asInt());
				} catch (Exception err) {
					ErrorReport.reportError(err);
				}
				revalidate(); 
			}
		});		
		
		btnAlign = new JButton("Alignment..."); 
		btnAlign.setBackground(Globals.PROMPTCOLOR);
		btnAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				TreeSet <String> selectedCtg = getSelectedContigIDs();
				
				if (selectedCtg==null || selectedCtg.size()==0) {
					JOptionPane.showMessageDialog( 	null, 
							"Select a hit in one of the alignments.","No hit selected", JOptionPane.PLAIN_MESSAGE );
				}
				else {
					final String name = selectedCtg.first();
					final AlignType at = new AlignType(name);
					at.setVisible(true);
					final int mode = at.getSelection();
					
					if(mode != AlignType.Align_cancel) {
						Vector <String> lines = alignPopup(at.getGap(), at.getExtend(), mode);
						if (lines!=null) 
						{
							String [] alines = new String [lines.size()];
							lines.toArray(alines);
							UserPrompt.displayInfoMonoSpace(null, "Hit Alignment", alines);
						}
					}
				}
			}
		});
		
		chkUTR = Static.createCheckBox("UTRs", false);
		chkUTR.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pairHighlight(0, chkUTR.isSelected());
				revalidate(); 
			}
		});
		
		chkHit = Static.createCheckBox("Hit", false);
		chkHit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pairHighlight(1, chkHit.isSelected());
				revalidate(); 
			}
		});
		
		JButton btnHelp = new JButton("Help2");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(),"Sequence-Hit Alignment", 
						"html/viewSingleTCW/ContigHitPanel.html");
			}
		});
		// Top panel with buttons and drop-down
		JPanel topPanel = Static.createRowPanel();
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( btnViewType );			topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add( menuZoom );			topPanel.add( Box.createHorizontalStrut(5) );
		
        if (isHit) {
        		if (isBeta && !isAllFrames) {
        			if (!(isAAdb)) {
        				topPanel.add( chkUTR ); 
        				topPanel.add( Box.createHorizontalStrut(5) );
        			}
        			topPanel.add( chkHit ); 
        			topPanel.add( Box.createHorizontalStrut(5) ); 
        		}
        	 	topPanel.add( Box.createHorizontalStrut(5) ); 
	        topPanel.add(new JLabel("For selected hit:"));	topPanel.add( Box.createHorizontalStrut(3) ); 
	        topPanel.add(btnAlign);							topPanel.add( Box.createHorizontalStrut(15) );
	        topPanel.add( Box.createHorizontalGlue() );
	        topPanel.add(btnHelp);							topPanel.add( Box.createHorizontalStrut(5) );
        }
	    topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)topPanel.getPreferredSize().getHeight() ) );
	    topPanel.setBackground(Color.WHITE);
	    
	    return topPanel;
	}
	/****************************************************************
	 * XXX Contig alignment
	 */
	private JPanel createToolEST (final boolean hasCAP3, final int nESTs, final int nBuried)
	{	
		btnViewSNPs = new JButton ("Show SNPs");
		btnViewSNPs.addActionListener(new ActionListener() {
			boolean bShowSNPS = false;
			boolean bDoneCalc = false;

			// on-the-fly calculation thread 
			public void actionPerformed(ActionEvent e) {
				bShowSNPS = !bShowSNPS;
				
				if (bShowSNPS) {
					if (!bDoneCalc && nESTs >= 100) {
						btnViewSNPs.setText("Calculating SNPs ...");
						btnViewSNPs.setEnabled(false);
					}
					Thread theThread = new Thread(new Runnable() {
						public void run() {	
							setShowSNPs(bShowSNPS);
							bDoneCalc = true;
							btnViewSNPs.setText("Hide SNPs");
							btnViewSNPs.setEnabled(true);
						}
					});
					theThread.setPriority(Thread.MIN_PRIORITY);
					theThread.start();
				}
				else {
					setShowSNPs(bShowSNPS);
					btnViewSNPs.setText("Show SNPs");
				}
			}
		});	// end view SNPs

		menuShowBuried = new  JComboBox <String> ();
		menuShowBuried.addItem("Hide Buried");
		menuShowBuried.addItem("Show Buried (Location)");
		menuShowBuried.addItem("Show Buried (Detail)");
		menuShowBuried.setBackground(Color.WHITE);
		menuShowBuried.setMinimumSize(menuShowBuried.getPreferredSize());
		menuShowBuried.setMaximumSize(menuShowBuried.getPreferredSize());

		JLabel lblFind = new JLabel("Find EST: ");
		final JTextField txtFind  = new JTextField(15);
		txtFind.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				setSelectedClones(txtFind.getText());
			}
		});
		txtFind.setMaximumSize(txtFind.getPreferredSize());

		// Sort menu
		menuContigSort = new JComboBox <MenuMapper> ();
		menuContigSort.addItem( new MenuMapper ( "Sort by Name", ContigData.SORT_BY_NAME ) );
		menuContigSort.addItem( new MenuMapper ( "Sort by Left Position", ContigData.SORT_BY_LEFT_POS ) );
		menuContigSort.addItem( new MenuMapper ( "Sort by F/R Pairs", ContigData.SORT_BY_GROUPED_LEFT_POS ) );
		menuContigSort.setBackground(Color.WHITE);
		menuContigSort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeSortOrder(menuContigSort.getSelectedIndex());
				revalidate(); 
			}
		});
		menuContigSort.setMinimumSize(menuContigSort.getPreferredSize());
		menuContigSort.setMaximumSize(menuContigSort.getPreferredSize());
		
		boolean doSelect=false;
		if (hasCAP3) { // Cap
			doSelect = true;
			btnCAP3 = new JButton("Run CAP3");
			btnCAP3.setToolTipText("Execute CAP3 on selected ESTs");
			btnCAP3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					alignSelectedESTs("cap3"); 
				}
			});
		}

        // Button panel
		btnViewType = new JButton ("View Bases");
		btnViewType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleViewType();
			}
		});
		
		menuZoom = Static.createZoom();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					MenuMapper selection = (MenuMapper) menuZoom.getSelectedItem();
					changeBasesPerPixel(selection.asInt());
				} catch (Exception err) {
					ErrorReport.reportError(err);
				}
				revalidate(); 
			}
		});		
		
		JButton btnClusterSelectNone = null;
		JButton btnClusterSelectAll = null;
			
		// Add panel with Select All/Select None buttons:
		if (doSelect) {
			btnClusterSelectAll = new JButton ( "Select All" );
			btnClusterSelectAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectAll();
				}
			});
			
			btnClusterSelectNone = new JButton ( "Unselect All" ); 
			btnClusterSelectNone.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					selectNone();
				}
			});
		}
		// 2nd row of buttons
		JPanel topPanel1 = Static.createRowPanel();
		topPanel1.add( Box.createHorizontalStrut(5) );
		topPanel1.add( lblFind ); 						
		topPanel1.add( txtFind ); 						
		topPanel1.add( Box.createHorizontalStrut(5) ); 
		if (btnClusterSelectAll!=null) {
			topPanel1.add( btnClusterSelectAll ); 	topPanel1.add( Box.createHorizontalStrut(5) ); 
			topPanel1.add( btnClusterSelectNone );	topPanel1.add( Box.createHorizontalStrut(5) ); 
		}

		if ( btnCAP3 != null ) {
			topPanel1.add( btnCAP3 );	topPanel1.add( Box.createHorizontalStrut(5) );
		}
		topPanel1.add( Box.createHorizontalGlue() );
		topPanel1.add( Box.createHorizontalStrut(5) );	
		
		// third row of button
		JPanel topPanel2 = Static.createRowPanel();
		topPanel2.add( Box.createHorizontalStrut(5) );
		topPanel2.add( menuContigSort );	topPanel2.add( Box.createHorizontalStrut(5) ); 
		topPanel2.add( btnViewSNPs );	topPanel2.add( Box.createHorizontalStrut(5) );
		topPanel2.add( Box.createHorizontalGlue() );
		
		// fourth row of buttons
		JPanel topPanel3 = Static.createRowPanel();
		topPanel3.add( Box.createHorizontalStrut(5) );			
		topPanel3.add(btnViewType);		topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add(menuZoom);			topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( menuShowBuried ); topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( Box.createHorizontalGlue() );
				
		JPanel topPanel = Static.createPagePanel();
		topPanel.add(topPanel1);		topPanel.add(Box.createVerticalStrut(5));
		topPanel.add(topPanel2);		topPanel.add(Box.createVerticalStrut(5));
		topPanel.add(topPanel3);
		
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		
		return topPanel;
	}
	
	/************************************************
	 * replica of createESTToolbar but to show sequences only
	 */
	private JPanel createToolSeq ( )
	{	
        // Button panel
		btnViewType = new JButton ("View Bases");
		btnViewType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				toggleViewType();
			}
		});
		
		menuZoom = Static.createZoom();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					MenuMapper selection = (MenuMapper) menuZoom.getSelectedItem();
					changeBasesPerPixel(selection.asInt());
				} catch (Exception err) {
					ErrorReport.reportError(err);
				}
				revalidate(); 
			}
		});		
					
		JPanel topPanel3 = Static.createRowPanel();
		topPanel3.add( Box.createHorizontalStrut(5) );			
		topPanel3.add(btnViewType);		topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add(menuZoom);			topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( Box.createHorizontalGlue() );
				
		JPanel topPanel = Static.createPagePanel();
		topPanel.add(Box.createVerticalStrut(5));
		topPanel.add(topPanel3);
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		
		return topPanel;
	}
	
	private MainToolAlignPanel getInstance() {return this;}
	
	private Vector<String> getContigIDs ( )
	{
		Vector<String> out = new Vector<String>();
		
		for (MainAlignPanel curPanel : listOfMainAlignPanels) {	
			out.addAll(curPanel.getContigIDs());
		}	
		return out;
	}
	public void addShowHideListener(ActionListener al)
	{
		if(menuShowBuried != null)
			menuShowBuried.addActionListener(al);
	}
	public TreeSet<String> getSelectedContigIDs ( )
	{
		TreeSet<String> selection = new TreeSet<String> ();
		
		for (MainAlignPanel curPanel : listOfMainAlignPanels) {	
			curPanel.getSelectedContigIDs( selection );
		}		
		if (selection.size()>0) return selection;
		else return getFirstContigID(); // CAS304
	}
	private TreeSet<String> getFirstContigID() { // CAS304 so do not have to select an alignment
		TreeSet<String> selection = new TreeSet<String> ();
		
		for (MainAlignPanel curPanel : listOfMainAlignPanels) {	
			Vector <String> ids = curPanel.getContigIDs( );
			selection.add(ids.get(0));
			return selection;
		}			
		return selection;
	}
	public void setSelectedContigs ( TreeSet<String> selectedIDs )
	{
		for (MainAlignPanel curPanel : listOfMainAlignPanels) {	
			curPanel.setSelectedContigIDs( selectedIDs );
		}		
	}
	public void setSelectedClones(String strNamePattern) 
	{
		for (MainAlignPanel curPanel : listOfMainAlignPanels)
			curPanel.selectMatchingSequences(strNamePattern);
	}
	
	private void alignSelectedESTs ( String strProgramToUse ) 
	{
		try
		{
			STCWFrame theFrame = (STCWFrame)UIHelpers.findParentFrame( this );
			
			// Have each contig panel add its selected ESTs to the set
			TreeSet<String> selectedESTs = new TreeSet<String> ();
			Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();		
			while ( iter.hasNext() ) {
				MainAlignPanel curPanel = iter.next();
				curPanel.addSelectedSequencesToSet ( selectedESTs );			
			}
			
			// Make sure the user has selected something 
			if ( selectedESTs.size() == 0 ) {
				JOptionPane.showMessageDialog( 	theFrame, 
							"Please select the ESTs you wish to align first.",
							"No ESTs Selected",
							JOptionPane.PLAIN_MESSAGE );
				return;
			}
			
			// Open a new tab with the CAP options
			CAP3Tab tab = new CAP3Tab ( 
					theFrame, 
					nRecordNum, 
					strProgramToUse, 
					selectedESTs,
					getContigIDs());
	
			theFrame.addTab(tab.getAlignName(), tab);
			
			// Add new CAP3 node to menu tree 
			String nodeName = tab.getAlignName() + " " + selectedESTs.size() + " ESTs";
			String toolTipText = selectedESTs.size() + " ESTs";
			theFrame.addNewMenuTreeNode(STCWFrame.ShowAllContigs, nodeName, tab, toolTipText);
		}
		catch ( Throwable err ) { ErrorReport.reportError(err); }
	}
	
	private void setShowSNPs ( boolean bShowSNPS )
	{		
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			DrawContigPanel curPanel = (DrawContigPanel)iter.next ();
			curPanel.setShowSNPs ( bShowSNPS );
		}
	}
	public void setShowBuriedAllPanels (boolean hasNoAssembly )
	{	
		if (hasNoAssembly) return;
		
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			DrawContigPanel curPanel = (DrawContigPanel)iter.next ();
			curPanel.setShowBuried ( menuShowBuried.getSelectedIndex() );
		}
	}
	private void selectAll ( )
	{
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			MainAlignPanel curPanel = iter.next();
			curPanel.selectAll();
		}		
	}
	private void selectNone ( )
	{
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			MainAlignPanel curPanel = iter.next();
			curPanel.selectNone();
		}	
	}
	private void changeSortOrder ( int nNewOrder )
	{
		// Notify all sub-panels
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			DrawContigPanel curPanel = (DrawContigPanel)iter.next ();
			curPanel.changeSortOrder ( nNewOrder );
			curPanel.refreshPanels();
		}		
	}
	/*********************************************************/
	private void handleClick (MouseEvent e)
	{
		// Convert to view relative coordinates
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
		
		// Go through  all the panels and see which one was clicked on:
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			// Get the panel and convert to panel relative coordinates
			MainAlignPanel curPanel = iter.next();
			int nPanelX = viewX - curPanel.getX();
			int nPanelY = viewY - curPanel.getY();
			
			if ( curPanel.contains( nPanelX, nPanelY ) ) {
				// Click is in current panel, let the object handle it
				curPanel.handleClick( e, new Point( nPanelX, nPanelY ) );
			}
			else
				// Clear all selections in the panel unless shift or control are down
				if ( !e.isShiftDown() && !e.isControlDown() )
					curPanel.selectNone();
		}
	}
	private void fitBasesPerPixel ( )
	{
		int nViewPortWidth = (int)scroller.getViewport().getBounds().getWidth();
		int nOptBasesPer = 1;
		int nCurAvailable;
		int nCurBasesPer;
		
		// Notify all sub-panels
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			MainAlignPanel curPanel = iter.next ();
			nCurAvailable = nViewPortWidth - (int)curPanel.getGraphicalDeadWidth();
			nCurBasesPer = (int) Math.ceil( curPanel.getTotalBases () / (double)nCurAvailable );
			nOptBasesPer = Math.max( nOptBasesPer, nCurBasesPer );
		}		
		
		// Set the optimum value
		nOptBasesPer = Math.min( nOptBasesPer, menuZoom.getItemCount() );
		menuZoom.setSelectedIndex ( nOptBasesPer - 1 );
	}
	
	private void changeBasesPerPixel ( int n ) throws Exception
	{
		// Notify all sub-panels
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			MainAlignPanel curPanel = iter.next ();
			curPanel.setBasesPerPixel ( n );
		}		
		scroller.getViewport().getView().setVisible( true );
	}
	
	private void toggleViewType ( )
	{
		if ( nViewType == MainAlignPanel.GRAPHICMODE )
			nViewType = MainAlignPanel.TEXTMODE;
		else
			nViewType = MainAlignPanel.GRAPHICMODE;
		
		setViewType();
	}
	
	private void setViewType ()
	{
		// Notify all sub-panels
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();

		if ( nViewType == MainAlignPanel.GRAPHICMODE )
		{
			btnViewType.setText( "View Bases" );
			menuZoom.setVisible ( true );
			while ( iter.hasNext() )
			{
				MainAlignPanel curPanel = iter.next ();
				curPanel.setDrawMode (MainAlignPanel.GRAPHICMODE);	
			}
		}
		else
		{
			btnViewType.setText( "View Graphic" );		
			menuZoom.setVisible ( false );
			while ( iter.hasNext() )
			{
				MainAlignPanel curPanel = iter.next ();
				curPanel.setDrawMode (MainAlignPanel.TEXTMODE);	
			}
		}		
		scroller.setViewportView( scroller.getViewport().getView() );		
	}
	
    //Gets called when user clicks Next>> or <<Prev
    public void setShowBuriedMenuSelection(int selection, boolean hasAssembly) 
    { 
    		if (hasAssembly)
    			menuShowBuried.setSelectedIndex(selection); 
    }
	public int getShowBuriedMenuSelection() {
		if(menuShowBuried == null) return 0;
		return menuShowBuried.getSelectedIndex(); 
	}
	
	/**
	 * Called before new contig loaded from either a next>> or <<prev click
	 */
	public int [] getDisplaySettings() {
		int [] retVal = new int[6];
		//Value used for both contig and contig pair listings
		if(menuContigSort != null) retVal[1] = menuContigSort.getSelectedIndex();
		if(menuShowBuried != null) retVal[2] = menuShowBuried.getSelectedIndex();
		if(menuZoom != null)  retVal[3] = menuZoom.getSelectedIndex();
		retVal[4] = nViewType;
		if(btnViewSNPs != null && btnViewSNPs.getText().equals("Hide SNPs")) retVal[5] = 1;
		else retVal[5] = 0;
		return retVal;
	}

	/**
	 * Called after new contig loaded on a next, prev click
	 * @param settings previous view settings
	 */
	public void applyDisplaySettings(int [] settings) {
		if(settings == null) {
			return;
		}
		if(menuContigSort != null) menuContigSort.setSelectedIndex(settings[1]);
		if(menuShowBuried != null) menuShowBuried.setSelectedIndex(0);
		if(menuZoom != null && settings[3]<menuZoom.getItemCount()) 
				menuZoom.setSelectedIndex(settings[3]);
		nViewType = settings[4];
		setViewType();
		if(btnViewSNPs != null && btnViewSNPs.getText().equals("Show SNPs") && settings[5] == 1)
			btnViewSNPs.doClick();
	}
	
	/****************************************************************
	 * XXX Align Pair for Pairwise
	 */
	private void pairHighlight(int type, boolean set) {
		Iterator<MainAlignPanel> iter = listOfMainAlignPanels.iterator();
		while ( iter.hasNext() )
		{
			MainAlignPanel curPanel = iter.next ();
			if (type==0) curPanel.setShowORF(set);
			else if (type==1) curPanel.setShowHit(set);
		}
	}
	private Vector <String> alignPopup(int gap, int extend, int type) {
			int inc = 60;
			TreeSet <String> selectedCtg = getSelectedContigIDs();
			String name = selectedCtg.first();
			
			AlignData aDataObj=null;
			for (MainAlignPanel ap : listOfMainAlignPanels) {
				AlignData ad = ap.getAlignData();
				if (name.equals(ad.getName1()) || name.equals(ad.getName2())) {
					aDataObj=ad;
					break;
				}
			}
			String aSeq1, aSeq2, aSeqM, method;
			int nStart1, nEnd1, nStart2, nEnd2, nStart,  nEnd, score=-1;
			if (type!=0) { // new alignment with PairAlign. Local. 
				AlignPairAA aExecObj = new AlignPairAA(gap, extend, 
						aDataObj.getOrigSeq1(), aDataObj.getOrigSeq2(), type);
				aSeq1 = aExecObj.getAlignOne();
				aSeq2 = aExecObj.getAlignTwo();
				aSeqM = aExecObj.getAlignMatch();
				
				nStart=0; 
				nEnd=aSeqM.length();
				int [] ends = aExecObj.getEnds();
				nStart1=ends[0]+1; 
				nEnd1=  ends[1];
				nStart2=ends[2]+1; 
				nEnd2=  ends[3];
				score = aExecObj.getScore();
				method = aExecObj.getMethod();
			}
			else { // current alignment, already done. Global
				aDataObj.computeMatch();
				aSeq1 = aDataObj.getAlignSeq1();
				aSeq2 = aDataObj.getAlignSeq2();
				aSeqM = aDataObj.getMatcheSeq();
				nStart =  aDataObj.getStartAlign();
				nEnd = aDataObj.getEndAlign();
				int [] ends = aDataObj.getEnds();
				nStart1=ends[0]; 
				nEnd1=  ends[1];
				nStart2=ends[2]; 
				nEnd2=  ends[3];
				score = aDataObj.getScore();
				method = "Original semi-global with affine gap";
			}
			// create info for left label
			String name1 = aDataObj.getName1();
			String name2 = aDataObj.getName2();
			int max = Math.max(name1.length(), name2.length());
			int max2 = Math.max(nEnd1, nEnd2);
			int y=5;
			if (max2<999) y=3;
			else if (max2<9999) y=4;
			String format = "%" + max + "s " + "%" + y + "d  ";
			String format1 = "%" + max + "s " + "%" + y + "s  ";
			
			// header score
			boolean inGap=false;
			int cntPos=0, cntNeg=0, cntGap=0, cntOpen=0, cntMat=0;
			for (int i=nStart;i<nEnd && i<aSeqM.length(); i++) {
				char c1 = aSeq1.charAt(i);
				char c2 = aSeq2.charAt(i);
				char cM = aSeqM.charAt(i);
				if (c1=='-' || c2=='-') {
					if (!inGap) {
						cntOpen++; 
						inGap=true;
					} 
					else cntGap++;
				}
				else {
					if (cM==Globalx.cAA_NEG) cntNeg++;
					else if (cM==Globalx.cAA_POS) cntPos++;
					else cntMat++;
					inGap=false;
				}
			}
			// header 
			Vector <String> lines = new Vector <String> ();
			String msg = "Method: " + method;
			if (type>0) msg += "    Penalties: Gap " + gap;
			if (type>1) msg += " Extend " + extend;
			lines.add(msg);
						
			msg = String.format("Score: %4d   %s: %3d  Gap open:   %3d", 
					score, Globalx.blosumPos, cntPos,  cntOpen,cntGap);
			lines.add(msg);
			
			msg = String.format("Match: %4d   %s: %3d  Gap extend: %3d",
					cntMat, Globalx.blosumNeg, cntNeg, cntGap);
			lines.add(msg); 
			lines.add("");
			
			// alignment
			int x;
			StringBuffer sb = new StringBuffer (inc);
			
			for (int offset=nStart; offset<nEnd; offset+=inc) {
				sb.append(String.format(format, name1, nStart1)); 
				for (x=0; x<inc && (x+offset)<nEnd; x++) sb.append(aSeq1.charAt(x+offset));
				sb.append("  " + (nStart1+x));
				lines.add(sb.toString());
				sb.delete(0, sb.length());
				
				sb.append(String.format(format1, "",""));
				for (int i=0; i<inc && (i+offset)<nEnd; i++) sb.append(aSeqM.charAt(i+offset));
				lines.add(sb.toString());
				sb.delete(0, sb.length());
				
				sb.append(String.format(format, name2, nStart2));
				for (x=0; x<inc && (x+offset)<nEnd; x++) sb.append(aSeq2.charAt(x+offset));
				sb.append("  " + (nStart2+x));
				lines.add(sb.toString());
				sb.delete(0, sb.length());
				
				lines.add("");
				nStart1+=inc;
				nStart2+=inc;
			}
			return lines;
	}
	private class AlignType extends JDialog {
		private static final long serialVersionUID = 6152973237315914324L;

		public static final int Align_orig= 0;
		public static final int Align_local = 1;
    	    public static final int Align_local_affine= 2;
    	    public static final int Align_cancel= 3;
    	   
        	public AlignType(String name) {
        		setModal(true);
        		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        		setTitle("Align " + name);
        		// globals not used as need to fix ends
        		JRadioButton btnOrig = new JRadioButton("Original semi-global with affine gaps");
        		btnOrig.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent arg0) {
    					nMode = Align_orig;
    				}
    			});
        		
        		JRadioButton btnLocal =  new JRadioButton("Local");
        		btnLocal.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent arg0) {
    					nMode = Align_local;
    				}
    			});
     
        		JRadioButton btnLocalAffine = new JRadioButton("Local with affine gaps");
            btnLocalAffine.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    nMode = Align_local_affine;
                }
            });
	   	      
             JLabel gapLabel = new JLabel("Gap ");
    			txtGap  = new JTextField(3);
    			txtGap.setMaximumSize(txtGap.getPreferredSize());
    			txtGap.setText("12");
    			
    			JLabel extendLabel = new JLabel("Extend ");
            txtExtend  = new JTextField(3);
            txtExtend.setMaximumSize(txtExtend.getPreferredSize());
            txtExtend.setText("2");
    			       		
        		JButton btnOK = new JButton("OK");
        		btnOK.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					setVisible(false);
    				}
    			});
        		JButton btnCancel = new JButton("Cancel");
        		btnCancel.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					nMode = Align_cancel;
    					setVisible(false);
    				}
    			});
        		
        		btnOK.setPreferredSize(btnCancel.getPreferredSize());
        		btnOK.setMaximumSize(btnCancel.getPreferredSize());
        		btnOK.setMinimumSize(btnCancel.getPreferredSize());
        		
        		ButtonGroup grp = new ButtonGroup();
        		grp.add(btnOrig);
        		grp.add(btnLocal);
	        grp.add(btnLocalAffine); 
	          
	    		btnLocalAffine.setSelected(true);
	    		nMode = Align_local_affine;
	    		
            JPanel affinePanel = new JPanel();
            affinePanel.setLayout(new BoxLayout(affinePanel, BoxLayout.LINE_AXIS));
            affinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            affinePanel.add(Box.createHorizontalStrut(15));
            affinePanel.add(new JLabel("Penalties:"));
            affinePanel.add(Box.createHorizontalStrut(5));
            affinePanel.add(gapLabel);
            affinePanel.add(txtGap);
            affinePanel.add(Box.createHorizontalStrut(5));
            affinePanel.add(extendLabel);
            affinePanel.add(txtExtend);
            affinePanel.add(Box.createHorizontalGlue());
	
	    		JPanel selectPanel = new JPanel();
	    		selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.PAGE_AXIS));
	    		selectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    		selectPanel.add(btnOrig);
	    		selectPanel.add(new JSeparator());
	    		selectPanel.add(Box.createVerticalStrut(5));
	    		selectPanel.add(btnLocal);
	    		selectPanel.add(Box.createVerticalStrut(5));
	    		selectPanel.add(btnLocalAffine);
	        	selectPanel.add(Box.createVerticalStrut(5));
	        
	        selectPanel.add(affinePanel);
	        selectPanel.add(new JSeparator());
        		JPanel buttonPanel = new JPanel();
        		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        		buttonPanel.add(btnOK);
        		buttonPanel.add(Box.createHorizontalStrut(20));
        		buttonPanel.add(btnCancel);
        		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
  
           	JPanel mainPanel = new JPanel();
        		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        		mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        		mainPanel.add(selectPanel);
        		mainPanel.add(Box.createVerticalStrut(15));
        		mainPanel.add(buttonPanel);
        		
        		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        		add(mainPanel);
        		
        		pack();
        		this.setResizable(false);
        		UIHelpers.centerScreen(this);
        	}
        	public int getGap() { return getInt(txtGap.getText().trim(), GAP); }
        public int getExtend() { return getInt(txtExtend.getText().trim(), EXTEND); }
        	public int getSelection() { return nMode; }
        	
        	JTextField txtGap = null;
        JTextField txtExtend = null;        	
        	int nMode = -1;
	}
	private int getInt(String x, int def) {
		try {
			return Integer.parseInt(x);
		}
		catch (Exception e) {}
		return def;
	}
	
    private boolean isAAdb=false;
	private int nRecordNum;
	
	// ESTs
	private JButton btnViewSNPs = null;
	private JButton btnCAP3 = null;
	private JComboBox <String> menuShowBuried = null;
	private JComboBox <MenuMapper> menuContigSort = null;
	
	// Pair/Hit align
	private JButton btnAlign = null;
	private JCheckBox chkUTR = null;
	private JCheckBox chkHit = null;
	
	// All (bases vs graphics)
    private JButton btnViewType = null;
    private int nViewType = MainAlignPanel.GRAPHICMODE;
	private JComboBox <MenuMapper> menuZoom = null;
	
	private JScrollPane scroller = null;
	private Vector<MainAlignPanel> listOfMainAlignPanels = null;
    
	static final private int GAP_WIDTH = 10, nTopGap = GAP_WIDTH, nBottomGap = GAP_WIDTH / 2, nSideGaps = GAP_WIDTH; 
    private static final long serialVersionUID = 1;	
}
