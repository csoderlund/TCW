package cmp.viewer.align;

/*************************************************************
 * For display multiple sets of pairs. It call PairAllAlignPanel for the graphics
 * Almost exactly like AlignPairView3Panel, but different buttons 
 * and it calls AlignPairNPanel as the display is a bit different.
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.ui.MenuMapper;
import util.ui.UserPrompt;
import util.database.DBConn;
import cmp.align.PairAlignText;
import cmp.align.PairAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class PairNViewPanel extends JPanel {
	private static final long serialVersionUID = -2090028995232770402L;
	private static final String help2HTML = Globals.helpDir + "PairAlign.html";
	private static final String help1HTML = Globals.helpDir + "BaseAlign.html";
	
	public PairNViewPanel(MTCWFrame vFrame, AlignButtons buttons, 
			String [] members, int aType, String sum, int row) {
		theViewerFrame = vFrame;
		theAlignButtons = buttons;
		
		numSeqs=members.length;
		alignType = aType;
		conHitID = null;
		strSummary = Globals.trimSum(sum);
		nParentRow = row;
		isNT = (aType==PairAlignData.AlignNT) ? true : false;
		
		buildAlignments(members);
	}
	// CAS305 for align consensus hit to all members
	public PairNViewPanel(MTCWFrame parentFrame, AlignButtons buttons, 
			String [] members, int aType, String sum, String hitStr, int row) {
		theViewerFrame = parentFrame;
		theAlignButtons = buttons;
		
		numSeqs=members.length;
		alignType = aType;
		conHitID = hitStr;
		strSummary = Globals.trimSum(sum);
		nParentRow = row;
		isNT = false;
		
		buildAlignments(members);
	}
	/***********************************************************************/
	private void createTopPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		add(Box.createVerticalStrut(10));
		
		JPanel topRow = createTopButton();
		add(topRow);
		add(Box.createVerticalStrut(5));
		
		JPanel lowRow = createAlignButton();
		add(lowRow);
		add(Box.createVerticalStrut(5));
		
		createHeaderPanel();
		add(headerPanel);
		add(Box.createVerticalStrut(5));
	}
	private void createPairPanel() {
		createMainPanel();
		
		add(scroller);
		setButtonsEnabled();
		alignHeader.setText(strSummary);
	}
	
	private JPanel createTopButton() {
		JPanel theTopRow = Static.createRowPanel();
		
		btnViewType = Static.createButton("Line", true);
		btnViewType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(isShowGraphic) {
					isShowGraphic = false;
					btnViewType.setText("Seq");
					menuZoom.setEnabled(false);
					dotBox.setEnabled(true);
				}
				else {
					isShowGraphic = true;
					
					btnViewType.setText("Line");
					menuZoom.setEnabled(true);
					dotBox.setEnabled(false);
				}
				refreshPanels();
			}
		});
		theTopRow.add(Static.createLabel("View: ")); 	theTopRow.add(Box.createHorizontalStrut(1));
		theTopRow.add(btnViewType);         			theTopRow.add(Box.createHorizontalStrut(5));
		
		dotBox = Static.createCheckBox("Dot", false); // CAS312 new
		dotBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		dotBox.setEnabled(false);
		theTopRow.add(dotBox);							theTopRow.add(Box.createHorizontalStrut(5));
		
		trimBox = Static.createCheckBox("Trim", false); // CAS313 new
		trimBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		theTopRow.add(trimBox);						theTopRow.add(Box.createHorizontalStrut(5));
		
		menuZoom = Static.createZoom2();	// CAS312 Zoom2 allows increase size
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					refreshPanels();
			}
		});	
		theTopRow.add(menuZoom); 						
		
		menuColor = Static.createCombo(BaseAlignPanel.colorSchemes); // CAS312 new
		menuColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		if (!isNT) {
			theTopRow.add(Box.createHorizontalStrut(5));
			theTopRow.add(menuColor); 
		}
		theTopRow.add(Box.createHorizontalGlue()); 
		
		// Help
		final JPopupMenu popup = new JPopupMenu();
		
		popup.add(new JMenuItem(new AbstractAction("View: graphical align") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theViewerFrame, "View: graphical align", help1HTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing help1"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Selected: multi-line") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theViewerFrame, "Selected: multi-align", help2HTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing help2"); }
			}
		}));
		JButton btnHelp = Static.createButtonHelp("Help...", true); // CAS312, CAS340...
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		theTopRow.add(btnHelp); theTopRow.add(Box.createHorizontalStrut(1));
		
		if(nParentRow >= 0) { // CAS341 if -1, then not from a single row
 	 	   btnPrevRow = Static.createButton(Globals.prev, true);
 	 	   btnPrevRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow-1);
 	 		   }
 	 	   }); 
 	 	   btnNextRow = Static.createButton(Globals.next, true);
 	 	   btnNextRow.addActionListener(new ActionListener() {
 	 		   public void actionPerformed(ActionEvent arg0) {
 	 			   getNextRow(nParentRow+1);
 	 		   }
 	 	   });
 	 	   theTopRow.add(btnPrevRow);
 	 	   theTopRow.add(btnNextRow);
		}
		theTopRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,(int)theTopRow.getPreferredSize ().getHeight()));
		return theTopRow;
	}
	/************************ row2 pairwise specific *********************************/
	private JPanel createAlignButton() {
		JPanel theRow2 = Static.createRowPanel();
		btnAlign = Static.createButtonPopup("Align...", false); 
		btnAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showSelectedPair();
			}
		});
		btnShowAllPairs = Static.createButton("Show Only", false);
		btnShowAllPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				displaySelectedPairs(false);
				unselectAll();
				setButtonsEnabled();
			}
		});
		
		btnShowAll = Static.createButton("Show All", false);
		btnShowAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				displayAllPairwise();
				unselectAll();
				setButtonsEnabled();
			}
		});
		
		theRow2.add(Static.createLabel(Globals.select)); theRow2.add(Box.createHorizontalStrut(2));
		theRow2.add(btnAlign); 							 theRow2.add(Box.createHorizontalStrut(2)); 
		
		if (numSeqs>3) { // CAS312 need second row
			theRow2.add(btnShowAllPairs);					 theRow2.add(Box.createHorizontalStrut(5));
			theRow2.add(btnShowAll);				
		}
		theRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE,(int)theRow2.getPreferredSize ().getHeight()));
		return theRow2;
	}
	private void createMainPanel() {
		scroller = new JScrollPane ( );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );		
		scroller.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				handleClick(e);
			}
		});	
		
		mainPanel = Static.createPagePanel();
		scroller.setViewportView( mainPanel );
		
		refreshPanels();
	}
	private void createHeaderPanel() {
		headerPanel = Static.createRowPanel();
		
		alignHeader =  Static.createTextFieldNoEdit(100);
		alignHeader.setText(strSummary);  headerPanel.add(Box.createHorizontalStrut(5));
		
		headerPanel.add(alignHeader);
		headerPanel.setMaximumSize(headerPanel.getPreferredSize()); 
		headerPanel.setMinimumSize(headerPanel.getPreferredSize()); 
	}
	/*********************************************************************/
	private void buildAlignments(String [] members) {
		createTopPanel();
		int cnt = members.length;
		int num = (alignType>=PairAlignData.AlignHIT0_AA) ? cnt : (cnt*(cnt-1))/2;
		alignHeader.setText("Computing " + num + " pairwise alignments. Please wait.");
		
		final String [] theMembers = members;
		if(theThread == null)
		{
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						bRunThread = true;
						
						boolean rc;
						if (alignType>=PairAlignData.AlignHIT0_AA)
							 rc = loadSeqAndAlign(theMembers);
						else rc = loadPairAndAlign(theMembers);
						
						if (rc) 
							createPairPanel();
						else 
							Out.PrtWarn("TCW error: building pairwise alignments");
						
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}
	private boolean loadPairAndAlign(String [] members) {
		Vector<PairAlignData> results = new Vector<PairAlignData> ();
		String [] seqIDs = new String [2];
		PairAlignData align;
		String msg = (isNT) ? "NT" : "AA";
		try {
			int cnt=0, max=(members.length*(members.length-1))/2;
			DBConn mDB = theViewerFrame.getDBConnection();
			for(int x=0; x<members.length && bRunThread; x++) {
				for(int y=x+1; y<members.length && bRunThread; y++) {
					seqIDs[0] = members[x];
					seqIDs[1] = members[y];
					cnt++;
					theViewerFrame.setStatus(msg + " aligning " + cnt + " of " + max);
					
					align = new PairAlignData(mDB, seqIDs,  alignType);
					
					if (!align.isGood()) 
						UserPrompt.showWarn(seqIDs[0] + " and " + seqIDs[1] + " may not be aligned correctly");
					
					results.add(align);
				}
			}
			theViewerFrame.setStatus(msg + " alignment is done - now generate display.....");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Aligning pairs for Member table");}
		
		//Sort by 2nd sequence
		Collections.sort(results);
		theAlignData = results.toArray(new PairAlignData[results.size()]);
		
		theGraphicPanels = new PairNAlignPanel[theAlignData.length];
		for(int x=0; x<theGraphicPanels.length; x++) {
			theGraphicPanels[x] = new PairNAlignPanel(theViewerFrame, theAlignData[x], isNT);
			theGraphicPanels[x].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		theViewerFrame.setStatus("");
		return true;
	}
	private boolean loadSeqAndAlign(String [] members) { // CAS305 added for Seq->Hit
		Vector<PairAlignData> results = new Vector<PairAlignData> ();
		String [] seqIDs = new String [2];
		PairAlignData align;
		try {
			int cnt=0, max=members.length;
			DBConn mDB = theViewerFrame.getDBConnection();
			for(int x=0; x<members.length && bRunThread; x++) {
				seqIDs[0] = members[x];
				seqIDs[1] = conHitID;
				cnt++;
				theViewerFrame.setStatus("Seq-Hit aligning " + cnt + " of " + max);
				
				align = new PairAlignData(mDB, seqIDs,  alignType);
				if (!align.isGood()) {
					UserPrompt.showWarn(seqIDs[0] + " and " + seqIDs[1] + " may not be aligned correctly");
					return false;
				}
				results.add(align);
			}
			theViewerFrame.setStatus("Seq-Hit alignment is done - now generate display.....");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Aligning seq-hits for Member table");}
		
		//Sort by 2nd seqName
		Collections.sort(results);
		theAlignData = results.toArray(new PairAlignData[results.size()]);
		
		theGraphicPanels = new PairNAlignPanel[theAlignData.length];
		for(int x=0; x<theGraphicPanels.length; x++) {
			theGraphicPanels[x] = new PairNAlignPanel(theViewerFrame, theAlignData[x], isNT);
			theGraphicPanels[x].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		theViewerFrame.setStatus("");
		return true;
	}
	private void unselectAll() {
		for(int x=0; x<theGraphicPanels.length; x++) {
			theGraphicPanels[x].selectNone();
		}
	}
	private void showSelectedPair() {
		int idx = getSelectedAlign();
		final PairAlignText at = new PairAlignText(false, isNT, false); // not CDS, not UTR
		at.setVisible(true);
		final int mode = at.getSelection();
		
		if(mode != PairAlignText.Align_cancel) {
			Vector <String> lines = at.alignPopup(theGraphicPanels[idx].getAlignment()); 
			if (lines!=null) 
			{
				String [] alines = new String [lines.size()];
				lines.toArray(alines);
				String x = (isNT) ? "Nucleotide" : "Amino acid"; 
				UserPrompt.displayInfoMonoSpace(null, x + " align", alines);
			}
		}	
	}
	private void displaySelectedPairs(boolean pairsOnly) {
		String [] selectedIDs = getSelectedIDsPairwise();
		for(int x=0; x<theGraphicPanels.length; x++) 
			theGraphicPanels[x].setVisible(theGraphicPanels[x].hasSeqIDs(selectedIDs, pairsOnly));
	}	
	private void displayAllPairwise() {
		for(int x=0; x<theGraphicPanels.length; x++) 
			theGraphicPanels[x].setVisible(true);
	}
	private void setButtonsEnabled() {
		boolean allVisible = true;
		
		for(int x=0; x<theGraphicPanels.length && allVisible; x++) {
			allVisible = theGraphicPanels[x].isVisible();
		}
		if (allVisible) btnShowAll.setEnabled(false);
		else	            btnShowAll.setEnabled(true);
		
		int numSel = getNumSelectedIDsPairwise();
		if(numSel > 0) btnShowAllPairs.setEnabled(true);
		else           btnShowAllPairs.setEnabled(false);
		
		if(numSel == 1 || numSeqs<=2) btnAlign.setEnabled(true);
		else           btnAlign.setEnabled(false);
	}
	
	private int getSelectedAlign() {
		for(int x=0; x<theGraphicPanels.length; x++) {
			Vector<String> temp = theGraphicPanels[x].getSelectedSeqIDs();
			if(temp.size() > 0) return x;		
		}
		return 0;
	}
	private String [] getSelectedIDsPairwise() {
		Vector<String> theIDs = new Vector<String> ();
		for(int x=0; x<theGraphicPanels.length; x++) {
			Vector<String> temp = theGraphicPanels[x].getSelectedSeqIDs();
			if(temp.size() > 0) {
				Iterator<String> iter = temp.iterator();
				while(iter.hasNext()) {
					String ID = iter.next();
					if(!theIDs.contains(ID)) theIDs.add(ID);
				}
			}
		}
		return theIDs.toArray(new String[theIDs.size()]);
	}
	private int getNumSelectedIDsPairwise() {
		Vector<String> theIDs = new Vector<String> ();
		for(int x=0; x<theGraphicPanels.length; x++) {
			Vector<String> temp = theGraphicPanels[x].getSelectedSeqIDs();
			if(temp.size() > 0) {
				Iterator<String> iter = temp.iterator();
				while(iter.hasNext()) {
					String ID = iter.next();
					if(!theIDs.contains(ID))
						theIDs.add(ID);
				}
			}
		}
		return theIDs.size();
	}
	private void refreshPanels() {
		try {
			refreshPairPanels();
			
			mainPanel.revalidate();
			mainPanel.repaint();
			setVisible(false); // occasionally the panels do not show (FIXME still doesn't on new display sometime)
			setVisible(true);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Refreshing panels");}
	}
	private void refreshPairPanels() {
		mainPanel.removeAll();
		try {
			MenuMapper ratioSelection = (MenuMapper) menuZoom.getSelectedItem();
			int ratio = ratioSelection.asInt();
			int view = (isShowGraphic) ? PairNAlignPanel.GRAPHICMODE : PairNAlignPanel.TEXTMODE;
			int col = menuColor.getSelectedIndex();
			boolean bDot = dotBox.isSelected();
			boolean bTrim = trimBox.isSelected();
			
			for(int x=0; x<theGraphicPanels.length; x++) {				
				theGraphicPanels[x].setOpts(view, col, bDot, bTrim, ratio);
				
				mainPanel.add(theGraphicPanels[x]);
			}
			mainPanel.add(Box.createVerticalStrut(40));
			LegendPanel lPanel = new LegendPanel(!isNT, col);
			mainPanel.add(lPanel);
		
		} catch (Exception e) {ErrorReport.reportError(e);}
	}
	
	private void handleClick(MouseEvent e) {
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
	
		for(int x=0; x<theGraphicPanels.length; x++)
		{
			int nPanelX = viewX - theGraphicPanels[x].getX();
			int nPanelY = viewY - theGraphicPanels[x].getY();
		
			if ( theGraphicPanels[x].contains( nPanelX, nPanelY ) ) {
				theGraphicPanels[x].handleClick( e, new Point( nPanelX, nPanelY ) );
			}
			else { // if ( !e.isShiftDown() && !e.isControlDown() ) {
				theGraphicPanels[x].selectNone();
			}
		}
		setButtonsEnabled();
	}
	private void getNextRow(int rowNum) { // only Pairs or Hits can have newRow
		removeAll(); // Container call to remove everything
		theThread=null;
		
		String [] strVals = theAlignButtons.getNextRow(rowNum); 
		if (strVals==null) return;
		
		tabName = 			strVals[0];
		strSummary = 		Globals.trimSum(strVals[1]);
		nParentRow = 		Static.getInteger(strVals[2], -1);	
		
		switch (alignType) { // this is done in AlignButton on the original call
		case PairAlignData.AlignAA:
			tabName += ": AA";
			break;
		case PairAlignData.AlignNT:
			tabName += ": NT";
			break;
		case PairAlignData.AlignHIT0_AA:// to each seq's best hit
			tabName += ": Hit";
			break;
		case PairAlignData.AlignHIT1_AA:// the same hit against each seq
			conHitID = strVals[3];
			tabName += ": Best";
			break;
		}
			
		if (strSummary.length()>180) strSummary = strSummary.substring(0, 180) + "...";
		
		String [] seqIDs = theAlignButtons.getSeqIDrow(nParentRow);
		buildAlignments(seqIDs);
		
		theViewerFrame.changePanelName(this, tabName, strSummary);
	}
	
	/********************************************************************/
	private JPanel mainPanel = null, headerPanel = null;
	
	private JScrollPane scroller = null;
	private JTextField alignHeader = null;
	 
	private JComboBox <String> menuColor = null;
	private JComboBox <MenuMapper> menuZoom = null;
	private JCheckBox dotBox = null, trimBox = null;
	
	private JButton btnShowAll = null, btnShowAllPairs = null, btnAlign=null, btnViewType = null;
	private JButton btnNextRow = null, btnPrevRow = null;
	
	private Thread theThread = null;
	private boolean bRunThread = false;
	
	private MTCWFrame theViewerFrame = null;
	private PairAlignData [] theAlignData = null;
	private PairNAlignPanel [] theGraphicPanels = null;
	private AlignButtons theAlignButtons = null;
	
	private String conHitID=null, strSummary="", tabName="";
	private int alignType=0, numSeqs=0, nParentRow = -1;
	private boolean isNT=true, isShowGraphic=true;
}
