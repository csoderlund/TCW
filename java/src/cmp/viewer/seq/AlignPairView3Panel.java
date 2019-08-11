package cmp.viewer.seq;
/************************************************************
 * When the members table is displayed for pairs, this is called 
 * to align with PairAlignPanel
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.MenuMapper;
import util.ui.UserPrompt;
import util.database.DBConn;
import cmp.align.PairAlignText;
import cmp.align.PairAlignData;
import cmp.align.Share;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class AlignPairView3Panel extends JPanel {
	private static final long serialVersionUID = -2090028995232770402L;
	private static final String helpHTML = "PairAlign.html";
	private int viewType=1; // 0=AA,CDS,NT  1=5'UTR,CDS, 3'UTR
	
	public AlignPairView3Panel(MTCWFrame parentFrame, String [] members, int [] lens, int type) { 
		theParentFrame = parentFrame;
		thePair = members;
		theLens = lens;
		viewType=type;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
	
		buildAlignments();
	}

	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		JPanel theRow = Static.createRowPanel();
		
		btnShowType = Static.createButton("View Sequence", true);
		btnShowType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(isShowGraphic) {
					isShowGraphic = false;
					btnShowType.setText("View Graphic");
					menuZoom.setEnabled(false);
				}
				else {
					isShowGraphic = true;
					btnShowType.setText("View Sequence");
					menuZoom.setEnabled(true);
				}
				refreshPanels();
			}
		});
		//theRow.add(btnShowType);
		//theRow.add(Box.createHorizontalStrut(3));
		
		menuZoom = Static.createZoom();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshPanels();
			}
		});	
		
		theRow.add(menuZoom);
		theRow.add(Box.createHorizontalStrut(10));
		
		// Align functions
		theRow.add(Static.createLabel("Align: ", true));
		theRow.add(Box.createHorizontalStrut(2));
		
		String type = (viewType==0) ? "AA..." : "5UTR...";
		btnAAalign = Static.createButton(type, true); 
		btnAAalign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				alignPopUp(0); 
			}
		});
	
		if (viewType==1) {
			int utr5_1 = theAlignData[0].getSeq1().length();
			int utr5_2 = theAlignData[0].getSeq2().length();
			if (utr5_1<=Share.minAlignLen || utr5_2<=Share.minAlignLen)
				btnAAalign.setEnabled(false);
		}
		theRow.add(btnAAalign);
		theRow.add(Box.createHorizontalStrut(2));
		
		btnCDSalign = Static.createButton("CDS...", true); 
		btnCDSalign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				alignPopUp(1);
			}
		});
		theRow.add(btnCDSalign);
		theRow.add(Box.createHorizontalStrut(4));
		
		type = (viewType==0) ? "NT..." : "3UTR...";
		btnNTalign = Static.createButton(type, true); 
		btnNTalign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				alignPopUp(2);
			}
		});
		theRow.add(btnNTalign);
		if (viewType==1) {
			int utr3_1 = theAlignData[2].getSeq1().length();
			int utr3_2 = theAlignData[2].getSeq2().length();
			if (utr3_1<=Share.minAlignLen || utr3_2<=Share.minAlignLen)
				btnNTalign.setEnabled(false);
		}
		
		theRow.add(Box.createHorizontalStrut(20));
	    theRow.add(Box.createHorizontalGlue()); 
	    
		JButton btnShowHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
		btnShowHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Pair Align", "html/viewMultiTCW/" + helpHTML);
			}
		});
		theRow.add(btnShowHelp);
		theRow.setMaximumSize(theRow.getPreferredSize()); 
		theRow.setMinimumSize(theRow.getPreferredSize()); 
		add(theRow);
		
		buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize()); 
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize()); 
	}

	private void buildAlignments() {
		if(theThread == null)
		{
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						if (viewType==0) createAA_CDS_NTPanel();		
						else createUTR_CDSPanel();
						
						createButtonPanel();
						add(buttonPanel);
					
						createMainPanel();
						add(scroller);
	
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}

	private void createAA_CDS_NTPanel() {
	try {
		DBConn mDB = theParentFrame.getDBConnection();
		theGraphicPanels = new AlignPair3Panel[theAlignData.length];
		
		if (theLens[0]>0 && theLens[1]>0) { // has AA for both sequences
			theParentFrame.setStatus("Aligning AA " + thePair[0] + " and " + thePair[1]);
			theAlignData[0] = new PairAlignData(mDB, thePair, PairAlignData.AlignAA); //  !isNT
			
			if (theAlignData[0].getAlignFullSeq1().length()>0 && theAlignData[0].getAlignFullSeq2().length()>0) {
				theGraphicPanels[0] = new AlignPair3Panel(theParentFrame, theAlignData[0]); 
				theGraphicPanels[0].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else System.err.println("No AA alignment for pair");
		}	
		else theGraphicPanels[0]=null;
		
		if (theLens[2]>0 && theLens[3]>0) { // has NT for both sequences; NT and CDS alignment
			theParentFrame.setStatus("Aligning CDS " + thePair[0] + " and " + thePair[1]);
			theAlignData[1] = new PairAlignData(mDB, thePair,  PairAlignData.AlignCDS_AA); // isCDS
			
			if (theAlignData[1].getAlignFullSeq1().length()>0 && theAlignData[1].getAlignFullSeq2().length()>0) {
				theGraphicPanels[1] = new AlignPair3Panel(theParentFrame, theAlignData[1]); 
				theGraphicPanels[1].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else System.err.println("No CDS alignment for pair");
			
			theParentFrame.setStatus("Aligning NT " + thePair[0] + " and " + thePair[1]);
			theAlignData[2] = new PairAlignData(mDB, thePair,  PairAlignData.AlignNT); //  isNT
			
			if (theAlignData[2].getAlignFullSeq1().length()>0 && theAlignData[2].getAlignFullSeq2().length()>0) {
				theGraphicPanels[2] = new AlignPair3Panel(theParentFrame, theAlignData[2]); 
				theGraphicPanels[2].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else System.err.println("No NT alignment for pair");
		}
		else theGraphicPanels[1]=theGraphicPanels[2]=null;
		theParentFrame.setStatus("");
		mDB.close();
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Getting alignment for pair");}
	}
	private void createUTR_CDSPanel() {
		try {
			theGraphicPanels = new AlignPair3Panel[theAlignData.length];
			
			if (theLens[2]<=0 && theLens[3]<=0) {
				Out.PrtError("Should not happen");
				theGraphicPanels[1]=theGraphicPanels[2]=null;
				theParentFrame.setStatus("Major TCW error");
				return;
			}
				
			DBConn mDB = theParentFrame.getDBConnection();
			
			theParentFrame.setStatus("Aligning CDS " + thePair[0] + " and " + thePair[1]);
			theAlignData[1] = new PairAlignData(mDB, thePair,  PairAlignData.AlignCDS_AA); // isCDS
			if (theAlignData[1].getAlignFullSeq1().length()>0 && theAlignData[1].getAlignFullSeq2().length()>0) {
				theGraphicPanels[1] = new AlignPair3Panel(theParentFrame, theAlignData[1]); 
				theGraphicPanels[1].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else Out.PrtWarn("No CDS alignment for pair");
				
			// This always creates a panel, even if there is zero aligned as it produces '---'
			String name1 = theAlignData[1].getSeqID1(), name2 = theAlignData[1].getSeqID2();
			theParentFrame.setStatus("Aligning 5UTR " + thePair[0] + " and " + thePair[1]);
			theAlignData[0] = new PairAlignData(name1, name2, theAlignData[1].get5UTR1(), theAlignData[1].get5UTR2(), true);
			if (theAlignData[0].getAlignFullSeq1().length()>0 && theAlignData[0].getAlignFullSeq2().length()>0) {
				theGraphicPanels[0] = new AlignPair3Panel(theParentFrame, theAlignData[0]); 
				theGraphicPanels[0].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else Out.PrtWarn("No 5'UTR alignment for pair");
			
			theParentFrame.setStatus("Aligning 3UTR " + thePair[0] + " and " + thePair[1]);
			theAlignData[2] = new PairAlignData(name1, name2, theAlignData[1].get3UTR1(), theAlignData[1].get3UTR2(), false);
			if (theAlignData[2].getAlignFullSeq1().length()>0 && theAlignData[2].getAlignFullSeq2().length()>0) {
				theGraphicPanels[2] = new AlignPair3Panel(theParentFrame, theAlignData[2]); 
				theGraphicPanels[2].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else Out.PrtWarn("No 3'UTR alignment for pair");
				
			theParentFrame.setStatus("");
			mDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting alignment for pair");}
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
		/** does not work the first time and causes blank display
		scroller.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				setZoom();
				scroller.removeComponentListener(this);
			}
		});
		**/
	}
	private void refreshPanels() {
		try {
			refreshPairwisePanels();
			
			mainPanel.revalidate();
			mainPanel.repaint();
			setVisible(false); // occasionally the panels do not show
			setVisible(true);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Refreshing panels");}
	}
	private void refreshPairwisePanels() {
		mainPanel.removeAll();
		try {
			MenuMapper ratioSelection = (MenuMapper) menuZoom.getSelectedItem();
			int ratio = ratioSelection.asInt();
			int view = AlignPair3Panel.GRAPHICMODE;
			//int view = (isShowGraphic) ? AlignPair3Panel.GRAPHICMODE : AlignPair3Panel.TEXTMODE;
			
			for(int x=0; x<theGraphicPanels.length; x++) {	
				if (theGraphicPanels[x]==null) continue;
				
				theGraphicPanels[x].setBorderColor(Color.BLACK);
			
				theGraphicPanels[x].setBasesPerPixel(ratio);
				theGraphicPanels[x].setDrawMode(view);
				
				mainPanel.add(theGraphicPanels[x]);
			}
			mainPanel.add(Box.createVerticalStrut(20));
			//scroller.getViewport().getView().setVisible( true );
		} catch (Exception e) {ErrorReport.reportError(e);}
	}
	// not working
	private void setZoom ( )
	{
		int nViewPortWidth = (int)scroller.getViewport().getBounds().getWidth();
		int nOptBasesPer = 1;
		int nCurAvailable;
		int nCurBasesPer;
		
		// Notify all sub-panels
		for (AlignPair3Panel curPanel : theGraphicPanels) {
			nCurAvailable = nViewPortWidth - (int)curPanel.getGraphicalDeadWidth();
			nCurBasesPer = (int) Math.ceil( curPanel.getTotalBases () / (double)nCurAvailable );
			nOptBasesPer = Math.max( nOptBasesPer, nCurBasesPer );
		}		
		
		// Set the optimum value
		nOptBasesPer = Math.min( nOptBasesPer, menuZoom.getItemCount() );
		menuZoom.setSelectedIndex ( nOptBasesPer - 1 );
	}
	// don't think I need
	private void handleClick(MouseEvent e) {
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
	
		for(int x=0; x<theGraphicPanels.length; x++)
		{
			if (theGraphicPanels[x]==null) continue;
			
			int nPanelX = viewX - theGraphicPanels[x].getX();
			int nPanelY = viewY - theGraphicPanels[x].getY();
		
			if ( theGraphicPanels[x].contains( nPanelX, nPanelY ) ) {
				theGraphicPanels[x].handleClick( e, new Point( nPanelX, nPanelY ) );
			}
			else if ( !e.isShiftDown() && !e.isControlDown() ) {
				theGraphicPanels[x].selectNone();
			}
		}
		boolean allVisible = true;
		for(int x=0; x<theGraphicPanels.length && allVisible; x++) {
			allVisible = theGraphicPanels[x].isVisible();
		}
	}
	
    private void alignPopUp(int index) {
    		String msg = "CDS codon align";
    		boolean isNT=true, isCDS=true, isUTR=false;
    		if (viewType==0) {
    			if (index==0) {
    				isNT=isCDS=false;
    				msg="Amino acid align";
    			}
    			else if (index==2) {
    				isCDS=false;
    				msg="Nucleotide align";
    			}
    		}
    		else { 
    			if (index!=1) {
    				isUTR=true; isCDS=false;
    				if (index==0)  msg="5'UTR align";
    				else if (index==2) msg="3'UTR align";
    			}
    		}
    		final PairAlignText at = new PairAlignText(isCDS, isNT, isUTR); // Pop up menu of options
		at.setVisible(true);
		final int mode = at.getSelection();
		
		if(mode != PairAlignText.Align_cancel) {
			Vector <String> lines;
			if (isCDS)
				lines = at.alignPopupCDS(theGraphicPanels[index].getAlignData());
			else 
				lines = at.alignPopup(theGraphicPanels[index].getAlignData());
			if (lines!=null) 
			{
				String [] alines = new String [lines.size()];
				lines.toArray(alines);
				UserPrompt.displayInfoMonoSpace(null, msg, alines);
			}
		}
    }

	private boolean isNT=true;
	private boolean isShowGraphic=true;
	
	private MTCWFrame theParentFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null;
	private JPanel mainPanel = null;

	private JComboBox menuZoom = null;
	private JButton btnShowType = null;
	private JButton btnAAalign=null;
	private JButton btnNTalign=null;
	private JButton btnCDSalign=null;

	private Thread theThread = null;
	
	private PairAlignData [] theAlignData = new PairAlignData [3];
	private AlignPair3Panel [] theGraphicPanels = null;
	private String [] thePair;
	private int [] theLens; // 1: ntlen, aaLen; 2: ntLen, aaLen
}
