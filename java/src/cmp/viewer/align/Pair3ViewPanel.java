package cmp.viewer.align;
/************************************************************
 * When the members table is displayed for pairs, this is called 
 * to align with PairAlignPanel
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

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

public class Pair3ViewPanel extends JPanel {
	private static final long serialVersionUID = -2090028995232770402L;
	private static final String help1HTML = Globals.helpDir + "PairAlign.html";
	private static final String help2HTML = Globals.helpDir + "BaseAlign.html";
	private int viewType=1; // 0=AA,CDS,NT  1=5'UTR,CDS, 3'UTR
	
	public Pair3ViewPanel(MTCWFrame parentFrame, String [] members,  int type, String sum) { 
		theParentFrame = parentFrame;
		thePair = members;
		viewType=type;
		summary=sum;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Static.BGCOLOR);
	
		buildAlignments();
	}

	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		JPanel theRow = Static.createRowPanel();
		
		btnShowType = Static.createButton("Line", true);
		btnShowType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(isShowGraphic) {
					isShowGraphic = false;
					btnShowType.setText("Seq");
					menuZoom.setEnabled(false);
					dotBox.setEnabled(true);
				}
				else {
					isShowGraphic = true;
					btnShowType.setText("Line");
					menuZoom.setEnabled(true);
					dotBox.setEnabled(false);
				}
				refreshPanels();
			}
		});
		theRow.add(Static.createLabel("View: ")); 	theRow.add(Box.createHorizontalStrut(1));
		theRow.add(btnShowType);         		theRow.add(Box.createHorizontalStrut(5));
		
		dotBox = Static.createCheckBox("Dot", true);
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
		
		menuZoom = Static.createZoom2();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshPanels();
			}
		});	
		
		theRow.add(menuZoom);
		theRow.add(Box.createHorizontalStrut(5));
		
		JButton btnHelp1 = Static.createButtonHelp("Help1", true);
		btnHelp1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Alignment...",  help2HTML);
			}
		});
		theRow.add(btnHelp1);
		theRow.add(Box.createHorizontalStrut(20));
		
		// Align functions
		JPanel theRow2 = Static.createRowPanel();
		theRow2.add(Static.createLabel("Align: ", true));
		theRow2.add(Box.createHorizontalStrut(2));
		
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
		theRow2.add(btnAAalign);
		theRow2.add(Box.createHorizontalStrut(2));
		
		btnCDSalign = Static.createButton("CDS...", true); 
		btnCDSalign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				alignPopUp(1);
			}
		});
		theRow2.add(btnCDSalign);
		theRow2.add(Box.createHorizontalStrut(2));
		
		type = (viewType==0) ? "NT..." : "3UTR...";
		btnNTalign = Static.createButton(type, true); 
		btnNTalign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				alignPopUp(2);
			}
		});
		theRow2.add(btnNTalign);
		if (viewType==1) {
			int utr3_1 = theAlignData[2].getSeq1().length();
			int utr3_2 = theAlignData[2].getSeq2().length();
			if (utr3_1<=Share.minAlignLen || utr3_2<=Share.minAlignLen)
				btnNTalign.setEnabled(false);
		}
		theRow2.add(Box.createHorizontalStrut(5));
	    
		JButton btnHelp = Static.createButtonHelp("Help2", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Align: ",  help1HTML);
			}
		});
		theRow2.add(btnHelp);
		
		theRow2.add(Box.createHorizontalGlue()); 
	
		buttonPanel.add(theRow);
		buttonPanel.add(theRow2);
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize()); 
		buttonPanel.setMinimumSize(buttonPanel.getPreferredSize()); 
	}
	private void createHeaderPanel() {
		headerPanel = Static.createRowPanel();
		alignHeader =  Static.createTextFieldNoEdit(100);
		alignHeader.setText(summary);
		headerPanel.add(Box.createHorizontalStrut(5));
		headerPanel.add(alignHeader);
		
		headerPanel.setMaximumSize(headerPanel.getPreferredSize()); 
		headerPanel.setMinimumSize(headerPanel.getPreferredSize()); 
	}
	private void buildAlignments() {
		if(theThread == null)
		{
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						if (viewType==0) createAA_CDS_NTPanel();		
						else createUTR_CDSPanel();
						
						add(Box.createVerticalStrut(10));
						
						createButtonPanel();
						add(buttonPanel);
						add(Box.createVerticalStrut(5));
						
						createHeaderPanel();
						add(headerPanel);
						add(Box.createVerticalStrut(5));
						
						createMainPanel();
						add(scroller);
	
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}
	// CAS340 removed len checks (I think they were for ESTscan that did not always produce AA seqs)
	private void createAA_CDS_NTPanel() {
	try {
		DBConn mDB = theParentFrame.getDBConnection();
		theGraphicPanels = new Pair3AlignPanel[theAlignData.length];
		
		theParentFrame.setStatus("Aligning AA " + thePair[0] + " and " + thePair[1]);
		theAlignData[0] = new PairAlignData(mDB, thePair, PairAlignData.AlignAA); //  !isNT
		
		if (theAlignData[0].getAlignFullSeq1().length()>0 && theAlignData[0].getAlignFullSeq2().length()>0) {
			theGraphicPanels[0] = new Pair3AlignPanel(theParentFrame, theAlignData[0]); 
			theGraphicPanels[0].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		else {
			Out.PrtWarn("No AA alignment for pair");
			theGraphicPanels[0]=null;
		}	
		
		theParentFrame.setStatus("Aligning CDS " + thePair[0] + " and " + thePair[1]);
		theAlignData[1] = new PairAlignData(mDB, thePair,  PairAlignData.AlignCDS_AA); // isCDS
		
		if (theAlignData[1].getAlignFullSeq1().length()>0 && theAlignData[1].getAlignFullSeq2().length()>0) {
			theGraphicPanels[1] = new Pair3AlignPanel(theParentFrame, theAlignData[1]); 
			theGraphicPanels[1].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		else System.err.println("No CDS alignment for pair");
		
		theParentFrame.setStatus("Aligning NT " + thePair[0] + " and " + thePair[1]);
		theAlignData[2] = new PairAlignData(mDB, thePair,  PairAlignData.AlignNT); //  isNT
		
		if (theAlignData[2].getAlignFullSeq1().length()>0 && theAlignData[2].getAlignFullSeq2().length()>0) {
			theGraphicPanels[2] = new Pair3AlignPanel(theParentFrame, theAlignData[2]); 
			theGraphicPanels[2].setAlignmentY(Component.LEFT_ALIGNMENT);
		}
		else {
			Out.PrtWarn("No NT alignment for pair");
			theGraphicPanels[1]=theGraphicPanels[2]=null;
		}
		
		theParentFrame.setStatus("");
		mDB.close();
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Getting alignment for pair");}
	}
	private void createUTR_CDSPanel() {
		try {
			theGraphicPanels = new Pair3AlignPanel[theAlignData.length];
			
				
			DBConn mDB = theParentFrame.getDBConnection();
			
			theParentFrame.setStatus("Aligning CDS " + thePair[0] + " and " + thePair[1]);
			theAlignData[1] = new PairAlignData(mDB, thePair,  PairAlignData.AlignCDS_AA); // isCDS
			if (theAlignData[1].getAlignFullSeq1().length()>0 && theAlignData[1].getAlignFullSeq2().length()>0) {
				theGraphicPanels[1] = new Pair3AlignPanel(theParentFrame, theAlignData[1]); 
				theGraphicPanels[1].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else Out.PrtWarn("No CDS alignment for pair");
				
			// This always creates a panel, even if there is zero aligned as it produces '---'
			String name1 = theAlignData[1].getSeqID1(), name2 = theAlignData[1].getSeqID2();
			theParentFrame.setStatus("Aligning 5UTR " + thePair[0] + " and " + thePair[1]);
			theAlignData[0] = new PairAlignData(name1, name2, theAlignData[1].get5UTR1(), theAlignData[1].get5UTR2(), true);
			if (theAlignData[0].getAlignFullSeq1().length()>0 && theAlignData[0].getAlignFullSeq2().length()>0) {
				theGraphicPanels[0] = new Pair3AlignPanel(theParentFrame, theAlignData[0]); 
				theGraphicPanels[0].setAlignmentY(Component.LEFT_ALIGNMENT);
			}
			else Out.PrtWarn("No 5'UTR alignment for pair");
			
			theParentFrame.setStatus("Aligning 3UTR " + thePair[0] + " and " + thePair[1]);
			theAlignData[2] = new PairAlignData(name1, name2, theAlignData[1].get3UTR1(), theAlignData[1].get3UTR2(), false);
			if (theAlignData[2].getAlignFullSeq1().length()>0 && theAlignData[2].getAlignFullSeq2().length()>0) {
				theGraphicPanels[2] = new Pair3AlignPanel(theParentFrame, theAlignData[2]); 
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
			int nZoom = ratioSelection.asInt();
			int view = (isShowGraphic) ? BaseAlignPanel.GRAPHICMODE : BaseAlignPanel.TEXTMODE;
			boolean bDot = dotBox.isSelected();
			boolean bTrim = trimBox.isSelected();
			int colMode = 0;
			
			for(int x=0; x<theGraphicPanels.length; x++) {	
				if (theGraphicPanels[x]==null) continue;
				
				theGraphicPanels[x].setOpts(view, colMode, bDot, bTrim, nZoom);
				
				theGraphicPanels[x].addDescLines(); // CAS313 was called in setZoom
			
				mainPanel.add(theGraphicPanels[x]);
			}
			mainPanel.add(Box.createVerticalStrut(20));
		} catch (Exception e) {ErrorReport.reportError(e);}
	}
	
	private void handleClick(MouseEvent e) { }// CAS312 removed unnecessary code - nothing is selectable
	
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
/********************************************************************/
	private boolean isShowGraphic=true;
	
	private MTCWFrame theParentFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null, mainPanel = null, headerPanel = null;
	private JTextField alignHeader = null;

	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnShowType = null;
	private JCheckBox dotBox = null , trimBox = null;
	
	private JButton btnAAalign=null, btnNTalign=null, btnCDSalign=null;

	private Thread theThread = null;
	
	private PairAlignData [] theAlignData = new PairAlignData [3];
	private Pair3AlignPanel [] theGraphicPanels = null;
	private String [] thePair;
	private String summary;
}
