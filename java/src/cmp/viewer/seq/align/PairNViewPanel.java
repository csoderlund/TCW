package cmp.viewer.seq.align;

/*************************************************************
 * For display multiple sets of pairs. It call PairAllAlignPanel for the graphics
 * Almost exactly like AlignPairView3Panel, but different buttons 
 * and it calls AlignPairNPanel as the display is a bit different.
 */
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import util.methods.ErrorReport;
import util.methods.Static;
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
	
	public PairNViewPanel(MTCWFrame parentFrame, String [] members, int alignType) {
		theParentFrame = parentFrame;
		numSeqs=members.length;
		this.isNT = (alignType==PairAlignData.AlignNT) ? true : false;
		this.alignType = alignType;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(theParentFrame.getSettings().getFrameSettings().getBGColor());
	
		buildAlignments(members);
	}
	// CAS305 for align consensus hit to all members
	public PairNViewPanel(MTCWFrame parentFrame, String [] members, int alignType, String hitStr) {
		theParentFrame = parentFrame;
		numSeqs=members.length;
		this.isNT = false;
		this.alignType = alignType;
		this.consensusHitID = hitStr;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(theParentFrame.getSettings().getFrameSettings().getBGColor());
	
		buildAlignments(members);
	}
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		JPanel theRow = Static.createRowPanel();
		
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
		theRow.add(new JLabel("View:")); theRow.add(Box.createHorizontalStrut(1));
		theRow.add(btnViewType);         theRow.add(Box.createHorizontalStrut(5));
		
		dotBox = Static.createCheckBox("Dot", true); // CAS312 new
		dotBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		dotBox.setEnabled(false);
		theRow.add(dotBox);
		theRow.add(Box.createHorizontalStrut(5));
		
		trimBox = Static.createCheckBox("Trim", false); // CAS313 new
		trimBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		theRow.add(trimBox);
		theRow.add(Box.createHorizontalStrut(5));
		
		menuZoom = Static.createZoom2();	// CAS312 Zoom2 allows increase size
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					refreshPanels();
			}
		});	
		theRow.add(menuZoom); theRow.add(Box.createHorizontalStrut(5));
		
		menuColor = Static.createCombo(BaseAlignPanel.colorSchemes); // CAS312 new
		menuColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		if (!isNT) {
			theRow.add(menuColor); theRow.add(Box.createHorizontalStrut(5));
		}
		JButton btnHelp1 = Static.createButton("Help1", true, Globals.HELPCOLOR); // CAS312
		btnHelp1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Alignment...", help1HTML);
			}
		});
		theRow.add(Box.createHorizontalStrut(10));
		theRow.add(btnHelp1);
		
	// pairwise specific
		btnShowAllPairs = Static.createButton("Show Only", false);
		btnShowAllPairs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				displaySelectedPairs(false);
				unselectAll();
				setButtonsEnabled();
			}
		});
		btnAlign = Static.createButton("Align...", false); 
		btnAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				showSelectedPair();
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
		JButton btnShowHelp = Static.createButton("Help2", true, Globals.HELPCOLOR);
		btnShowHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Selected:", help2HTML);
			}
		});
		buttonPanel.add(theRow); buttonPanel.add(Box.createVerticalStrut(5));
		
		JPanel theRow2 = Static.createRowPanel();
		if (numSeqs>3) { // CAS312 need second row
			theRow2.add(new JLabel("Selected:"));	theRow2.add(Box.createHorizontalStrut(2));
			theRow2.add(btnAlign); 					theRow2.add(Box.createHorizontalStrut(2)); 
			theRow2.add(btnShowHelp);				theRow2.add(Box.createHorizontalStrut(10));
			theRow2.add(btnShowAllPairs);			theRow2.add(Box.createHorizontalStrut(5));
			theRow2.add(btnShowAll);				
		}
		else  {
			if (numSeqs>2) {theRow2.add(new JLabel("Selected:"));theRow2.add(Box.createHorizontalStrut(2));}
			theRow2.add(btnAlign); 					theRow2.add(Box.createHorizontalStrut(2)); 
			theRow2.add(btnShowHelp);				
		}
		buttonPanel.add(theRow2);
		
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize()); 
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize()); 
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
	private void buildAlignments(String [] members) {
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
						if (rc) {
							createButtonPanel();
							createMainPanel();
						
							add(buttonPanel);
							add(scroller);
							setButtonsEnabled();
						}
						else {
							System.out.println("Error getting sequences to align");
						}
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
			DBConn mDB = theParentFrame.getDBConnection();
			for(int x=0; x<members.length && bRunThread; x++) {
				for(int y=x+1; y<members.length && bRunThread; y++) {
					seqIDs[0] = members[x];
					seqIDs[1] = members[y];
					cnt++;
					theParentFrame.setStatus(msg + " aligning " + cnt + " of " + max);
					
					align = new PairAlignData(mDB, 
							seqIDs,  
							alignType);
					
					if (!align.isGood()) 
						UserPrompt.showWarn(seqIDs[0] + " and " + seqIDs[1] + " may not be aligned correctly");
					
					results.add(align);
				}
			}
			theParentFrame.setStatus(msg + " alignment is done - now generate display.....");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Aligning pairs for Member table");}
		
		//Sort by 2nd sequence
		Collections.sort(results);
		theAlignData = results.toArray(new PairAlignData[results.size()]);
		
		theGraphicPanels = new PairNAlignPanel[theAlignData.length];
		for(int x=0; x<theGraphicPanels.length; x++) {
			theGraphicPanels[x] = new PairNAlignPanel(theParentFrame, theAlignData[x], isNT);
			theGraphicPanels[x].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		theParentFrame.setStatus("");
		return true;
	}
	private boolean loadSeqAndAlign(String [] members) { // CAS305 added for Seq->Hit
		Vector<PairAlignData> results = new Vector<PairAlignData> ();
		String [] seqIDs = new String [2];
		PairAlignData align;
		try {
			int cnt=0, max=members.length;
			DBConn mDB = theParentFrame.getDBConnection();
			for(int x=0; x<members.length && bRunThread; x++) {
				seqIDs[0] = members[x];
				seqIDs[1] = consensusHitID;
				cnt++;
				theParentFrame.setStatus("Seq-Hit aligning " + cnt + " of " + max);
				
				align = new PairAlignData(mDB, seqIDs,  alignType);
				if (!align.isGood()) {
					UserPrompt.showWarn(seqIDs[0] + " and " + seqIDs[1] + " may not be aligned correctly");
					return false;
				}
				results.add(align);
			}
			theParentFrame.setStatus("Seq-Hit alignment is done - now generate display.....");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Aligning seq-hits for Member table");}
		
		//Sort by 2nd seqName
		Collections.sort(results);
		theAlignData = results.toArray(new PairAlignData[results.size()]);
		
		theGraphicPanels = new PairNAlignPanel[theAlignData.length];
		for(int x=0; x<theGraphicPanels.length; x++) {
			theGraphicPanels[x] = new PairNAlignPanel(theParentFrame, theAlignData[x], isNT);
			theGraphicPanels[x].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		theParentFrame.setStatus("");
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
	
	private String consensusHitID=null;
	private int alignType=0;
	private boolean isNT=true;
	private boolean isShowGraphic=true;
	
	private MTCWFrame theParentFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null;
	private JPanel mainPanel = null;

	private JComboBox <String> menuColor = null;
	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnViewType = null;
	private JCheckBox dotBox = null, trimBox = null;
	
	private JButton btnShowAll = null;
	private JButton btnShowAllPairs = null;
	private JButton btnAlign=null;
	
	private Thread theThread = null;
	private boolean bRunThread = false;
	
	private PairAlignData [] theAlignData = null;
	private PairNAlignPanel [] theGraphicPanels = null;
	private int numSeqs=0;
}
