package sng.viewer.panels.align;
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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.viewer.STCWFrame;
import sng.viewer.panels.seqDetail.CAP3Tab;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.MenuMapper;
import util.ui.UIHelpers;

public class ContigViewPanel extends JPanel implements ClipboardOwner 
{		
	public static int HIDE_BURIED_EST = 0;
	public static int SHOW_BURIED_EST_LOCATION = 1;
	public static int SHOW_BURIED_EST_DETAIL = 2;
	
	public void lostOwnership(Clipboard clipboard, Transferable contents) { }
	
	static public ContigViewPanel createESTPanel (boolean hasCAP3,
					MultiCtgData inCluster, int inRecordNum, int parentID) throws Exception 
	{
		Vector<ContigAlignPanel> subPanels = new Vector<ContigAlignPanel> ();
		Iterator<ContigData> i = inCluster.getContigIterator();
		int nTotalESTs = 0;		
		int nTotalBuried = 0; 	

		while ( i.hasNext() ) {
			ContigData curData = i.next ();
			ContigAlignPanel curPanel = new ContigAlignPanel (curData, 													
								nTopGap, nBottomGap, nSideGaps, nSideGaps );
			subPanels.add( curPanel );	
			
			nTotalESTs += curData.getNumSequences();	
			nTotalBuried += curData.getNumBuried(); 		
		}
		ContigViewPanel panel = new ContigViewPanel (inRecordNum, parentID);
		
		panel.createMainPanelFromSubPanels( subPanels);
		
		panel.add ( panel.createToolEST(hasCAP3, nTotalESTs, nTotalBuried) );
		panel.add ( Box.createVerticalStrut(5) );
		panel.add ( panel.scroller );
		
		panel.menuContigSort.setSelectedIndex( 2 );
		
		return panel;
	}
	
	/*******************************************
	 * XXX Class starts here
	 */
	private ContigViewPanel (int inRecordNum, int parentID)  {
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
	
	private void createMainPanelFromSubPanels (Vector<ContigAlignPanel> ctgPanels ){
		JPanel mainPanel = Static.createPagePanel();
		mainPanel.setVisible( false );
		
		// Add each panel to the parent panel
		baseAlignPanelsVec = new Vector <BaseAlignPanel> ();
		
		Iterator <ContigAlignPanel> iter = ctgPanels.iterator();
		while ( iter.hasNext() )
		{
			BaseAlignPanel curPanel = (BaseAlignPanel) iter.next ();
			mainPanel.add( curPanel );
			baseAlignPanelsVec.add( curPanel );
		}

		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( new LegendPanel(false /*isPair*/) );
		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( Box.createVerticalGlue() ); 	
		
		scroller.setViewportView( mainPanel );
	}
	
	/****************************************************************
	 * XXX Contig alignment
	 */
	private JPanel createToolEST (final boolean hasCAP3, final int nESTs, final int nBuried){	
		btnViewSNPs = new JButton ("SNPs Off");
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
							btnViewSNPs.setText("SNPs On");
							btnViewSNPs.setEnabled(true);
						}
					});
					theThread.setPriority(Thread.MIN_PRIORITY);
					theThread.start();
				}
				else {
					setShowSNPs(bShowSNPS);
					btnViewSNPs.setText("SNPs Off");
				}
			}
		});	// end view SNPs

		menuShowBuried = new  JComboBox <String> ();
		menuShowBuried.addItem("Buried Hidden");
		menuShowBuried.addItem("Buried Location");
		menuShowBuried.addItem("Buried Detail");
		menuShowBuried.setBackground(Color.WHITE);
		menuShowBuried.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setShowBuriedAllPanels();
				revalidate(); 
			}
		});
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
		menuContigSort.addItem( new MenuMapper ( "Sort by Left Pos", ContigData.SORT_BY_LEFT_POS ) );
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
		btnViewType = new JButton ("Line");
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
		
		// third row of buttons
		JPanel topPanel3 = Static.createRowPanel();
		topPanel3.add( Box.createHorizontalStrut(5) );	
		topPanel3.add(new JLabel("View:"));
		topPanel3.add(btnViewType);		topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add(menuZoom);		topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( menuShowBuried ); topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( menuContigSort ); topPanel3.add( Box.createHorizontalStrut(5) ); 
		topPanel3.add( btnViewSNPs );	topPanel3.add( Box.createHorizontalStrut(5) );
		topPanel3.add( Box.createHorizontalGlue() );
				
		JPanel topPanel = Static.createPagePanel();
		topPanel.add(topPanel1);		topPanel.add(Box.createVerticalStrut(5));
		topPanel.add(topPanel3);
		
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		
		return topPanel;
	}
	
	private Vector<String> getSeqIDs ( ) {
		Vector<String> out = new Vector<String>();
		
		for (BaseAlignPanel curPanel : baseAlignPanelsVec) {	
			out.addAll(curPanel.getSeqIDs());
		}	
		return out;
	}
	public void addShowHideListener(ActionListener al) {
		if(menuShowBuried != null)
			menuShowBuried.addActionListener(al);
	}
	
	private void setSelectedClones(String strNamePattern)  {
		for (BaseAlignPanel curPanel : baseAlignPanelsVec)
			curPanel.selectMatchSeqs(strNamePattern);
	}
	
	private void alignSelectedESTs ( String strProgramToUse ) {
		try
		{
			STCWFrame theFrame = (STCWFrame)UIHelpers.findParentFrame( this );
			
			// Have each contig panel add its selected ESTs to the set
			TreeSet<String> selectedESTs = new TreeSet<String> ();
			Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();		
			while ( iter.hasNext() ) {
				BaseAlignPanel curPanel = iter.next();
				curPanel.addSelectedSeqsToSet ( selectedESTs );			
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
			CAP3Tab tab = new CAP3Tab ( theFrame, 
					nRecordNum, strProgramToUse, 
					selectedESTs, getSeqIDs());
	
			theFrame.addTab(tab.getAlignName(), tab);
			
			// Add new CAP3 node to menu tree 
			String nodeName = tab.getAlignName() + " " + selectedESTs.size() + " ESTs";
			String toolTipText = selectedESTs.size() + " ESTs";
			theFrame.addNewMenuTreeNode(STCWFrame.ShowAllContigs, nodeName, tab, toolTipText);
		}
		catch ( Throwable err ) { ErrorReport.reportError(err); }
	}
	
	private void setShowSNPs ( boolean bShowSNPS ){		
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			ContigAlignPanel curPanel = (ContigAlignPanel)iter.next ();
			curPanel.setShowSNPs ( bShowSNPS );
		}
	}
	public void setShowBuriedAllPanels () {	
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			ContigAlignPanel curPanel = (ContigAlignPanel)iter.next ();
			curPanel.setShowBuried ( menuShowBuried.getSelectedIndex() );
		}
	}
	private void selectAll ( ) {
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			BaseAlignPanel curPanel = iter.next();
			curPanel.selectAll();
		}		
	}
	private void selectNone ( ) {
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			BaseAlignPanel curPanel = iter.next();
			curPanel.selectNone();
		}	
	}
	private void changeSortOrder ( int nNewOrder ) {
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			ContigAlignPanel curPanel = (ContigAlignPanel)iter.next ();
			curPanel.changeSortOrder ( nNewOrder );
			curPanel.refreshPanels();
		}		
	}
	/*********************************************************/
	private void handleClick (MouseEvent e) {
		// Convert to view relative coordinates
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
		
		// Go through  all the panels and see which one was clicked on:
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() )
		{
			// Get the panel and convert to panel relative coordinates
			BaseAlignPanel curPanel = iter.next();
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
	private void fitBasesPerPixel ( ){
		int nViewPortWidth = (int)scroller.getViewport().getBounds().getWidth();
		int nOptBasesPer = 1;
		int nCurAvailable;
		int nCurBasesPer;
		
		// Notify all sub-panels
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() )
		{
			BaseAlignPanel curPanel = iter.next ();
			nCurAvailable = nViewPortWidth - (int)curPanel.getGraphicalDeadWidth();
			nCurBasesPer = (int) Math.ceil( curPanel.getTotalBases () / (double)nCurAvailable );
			nOptBasesPer = Math.max( nOptBasesPer, nCurBasesPer );
		}		
		
		// Set the optimum value
		nOptBasesPer = Math.min( nOptBasesPer, menuZoom.getItemCount() );
		menuZoom.setSelectedIndex ( nOptBasesPer - 1 );
	}
	
	private void changeBasesPerPixel ( int n ) throws Exception {
		// Notify all sub-panels
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			BaseAlignPanel curPanel = iter.next ();
			curPanel.setZoom ( n );
		}		
		scroller.getViewport().getView().setVisible( true );
	}
	
	private void toggleViewType ( ) {
		if ( nViewType == BaseAlignPanel.GRAPHICMODE )
			nViewType = BaseAlignPanel.TEXTMODE;
		else
			nViewType = BaseAlignPanel.GRAPHICMODE;
		
		setViewType();
	}
	
	private void setViewType (){
		// Notify all sub-panels
		Iterator<BaseAlignPanel> iter = baseAlignPanelsVec.iterator();

		if ( nViewType == BaseAlignPanel.GRAPHICMODE )
		{
			btnViewType.setText( "Line" );
			menuZoom.setEnabled ( true );
			while ( iter.hasNext() )
			{
				BaseAlignPanel curPanel = iter.next ();
				curPanel.setDrawMode (BaseAlignPanel.GRAPHICMODE);	
			}
		}
		else
		{
			btnViewType.setText( "Seq" );		
			menuZoom.setEnabled ( false );
			while ( iter.hasNext() )
			{
				BaseAlignPanel curPanel = iter.next ();
				curPanel.setDrawMode (BaseAlignPanel.TEXTMODE);	
			}
		}		
		scroller.setViewportView( scroller.getViewport().getView() );		
	}
	
	private int nRecordNum;
	
	// ESTs
	private JButton btnViewSNPs = null;
	private JButton btnCAP3 = null;
	private JComboBox <String> menuShowBuried = null;
	private JComboBox <MenuMapper> menuContigSort = null;
	
	// All (bases vs graphics)
    private JButton btnViewType = null;
    private int nViewType = BaseAlignPanel.GRAPHICMODE;
	private JComboBox <MenuMapper> menuZoom = null;
	
	private JScrollPane scroller = null;
	private Vector<BaseAlignPanel> baseAlignPanelsVec = null;
    
	static final private int GAP_WIDTH = 10, nTopGap = GAP_WIDTH, nBottomGap = GAP_WIDTH / 2, nSideGaps = GAP_WIDTH; 
    private static final long serialVersionUID = 1;	
}
