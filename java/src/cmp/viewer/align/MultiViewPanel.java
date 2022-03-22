package cmp.viewer.align;

/*************************************************************
 * Called from AlignButtons for MSA - used by GrpTablePanel and SeqTablePanel
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.MenuMapper;
import util.ui.UserPrompt;
import cmp.align.MultiAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class MultiViewPanel extends JPanel {
	private static final long serialVersionUID = -2090028995232770402L;
	private static final String help1HTML = Globals.helpDir + "BaseAlign.html";
	private static final String help2HTML = Globals.helpDir + "MSAScores.html";
	
	// runs MSA program and displays results
	public MultiViewPanel(MTCWFrame vFrame, AlignButtons buttons, String [] members, 
			String hitid, int align, int type, String sum, int row) {
		theViewerFrame = vFrame;
		theAlignButtons = buttons;
		
		isAA = (type==Globals.AA);
		alignType = type;
		isFromDB = false;
		nParentRow = row;
		alignPgm = align;
		
		sumLine1 = sum;
		if (sumLine1.contains("(")) 	sumLine1 = sumLine1.substring(0, sumLine1.indexOf("(")); // Remove score
		sumLine1 = Globals.trimSum(sumLine1);
		
		pgmType = (alignPgm==Globals.Ext.MUSCLE) ? "MUSCLE" : "MAFFT"; // CAS340 
		if (type==Globals.AA) 		pgmType += " AA";
		else if (type==Globals.NT) 	pgmType += " NT";
		else 						pgmType += " CDS";
		
		buildMSArun(members, hitid);
	}
	// displays existing MSA from database
	public MultiViewPanel(MTCWFrame vFrame, AlignButtons buttons, String [] members, 
			int grpid, String sum, int row) {
		theViewerFrame = vFrame;
		theAlignButtons = buttons;
		
		isAA = true;
		alignType = Globals.AA;
		isFromDB = true;
		nParentRow = row;
		
		sumLine1 = sum;
		if (sumLine1.contains("(")) {
			int idx =  sumLine1.indexOf("(");
			sumLine2 = "Scores: " + sumLine1.substring(idx); // put scores on second line
			sumLine1 = sumLine1.substring(0, idx);
		}
		sumLine1 = Globals.trimSum(sumLine1);
		
		buildMSAdb(members, grpid);
	}
	/*************************************************************/
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		
		JPanel theRow = Static.createRowPanel();
		
		btnViewType = Static.createButton("Line", bAlign); 
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
		theRow.add(Static.createLabel("View:")); theRow.add(Box.createHorizontalStrut(1));
		theRow.add(btnViewType);         		 theRow.add(Box.createHorizontalStrut(1));
		
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
		theRow.add(trimBox);					theRow.add(Box.createHorizontalStrut(1));
		
		menuZoom = Static.createZoom2(); // CAS312 Zoom2 allows increase size
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshPanels();
			}
		});	
		theRow.add(menuZoom);	theRow.add(Box.createHorizontalStrut(1));

		menuColor = Static.createCombo(BaseAlignPanel.colorSchemes); // CAS312 add
		menuColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		if (isAA) {
			theRow.add(menuColor);			theRow.add(Box.createHorizontalStrut(2));
		}
		else 								theRow.add(Box.createHorizontalStrut(50));
		
		btnScore = Static.createButtonPopup("Scores...", true); // CAS312
		btnScore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String s1 = theViewerFrame.getInfo().getMSA_Score1();
				String s2 = theViewerFrame.getInfo().getMSA_Score2();
				theAlignPanel.showScores(theViewerFrame, s1, s2, sumLine2);
			}
		});
		if (isAA) theRow.add(btnScore); 				
		
		theRow.add(Box.createHorizontalGlue());
		
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
		popup.add(new JMenuItem(new AbstractAction("MSA Scores") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theViewerFrame, "MSA Scores", help2HTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing help2"); }
			}
		}));
		JButton btnHelp = Static.createButtonHelp("Help...", true); // CAS312, CAS340...
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		theRow.add(btnHelp);							theRow.add(Box.createHorizontalStrut(1));
		
		if(nParentRow >= 0) { // if -1, then showing members from multiple clusters, and no Next/Prev
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
 	 	   theRow.add(btnPrevRow);
 	 	   theRow.add(btnNextRow); 
	 	}
		theRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,(int)theRow.getPreferredSize ().getHeight()));
		buttonPanel.add(theRow);
	}
	
	/*************************************************************************/
	private void createInfo() {
		textPanel = Static.createPagePanel();
		
		infoText1 = Static.createTextFieldNoEdit(Globals.trimSUM);
		textPanel.add(infoText1); 
		textPanel.add(Box.createVerticalStrut(5));
		
		infoText2 = Static.createTextFieldNoEdit(Globals.trimSUM);
		textPanel.add(infoText2);
	}
	public void updateInfo(String msg1, String msg2) { 
		String sp="   ";
		
		if (msg1!=null && msg1!="") infoText1.setText(sp + msg1);
		else 						infoText1.setText(sp + sumLine1);
		
		if (msg2!=null && msg2!="") infoText2.setText(sp + msg2);
		else 						infoText2.setText(sp + sumLine2);
	}
	/**************************************************************/
	private void createViewTop() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globalx.BGCOLOR);
		
		add(Box.createVerticalStrut(10));
		
		createButtonPanel();	
		add(buttonPanel);
		add(Box.createVerticalStrut(10));
		
		createInfo();			
		add(textPanel);
		add(Box.createVerticalStrut(5));
	}
	private void createViewAlign() {
		theAlignPanel = new MultiAlignPanel(getInstance(),isAA, theMultiAlignData);
		theAlignPanel.setAlignmentY(Component.LEFT_ALIGNMENT);
		
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
		add(scroller);
		
		refreshPanels();	
		updateInfo(sumLine1, sumLine2); 
	}
	/***********************************************************************
	 * MSA methods
	 */
	private void buildMSAdb(String [] members, int grpid) {
		try {
			createViewTop();
			
			String [] aaSeq = loadSequencesFromDB(members, isAA);
			
			theMultiAlignData = new MultiAlignData(null);
			for(int x=0; x<members.length; x++) 
				theMultiAlignData.addSequence(members[x], aaSeq[x]);
			
			DBConn mDB = theViewerFrame.getDBConnection();
			bAlign = theMultiAlignData.loadAlignFromDB(mDB, grpid); 
			mDB.close();
			
			createViewAlign();
		} 
		catch (Exception e) {ErrorReport.prtReport(e, "MSA from DB");}
	}
	
	private void buildMSArun(String [] members, String hitid) {
		final String [] theMembers = members;
		final String theHitid = hitid;
		
		createViewTop();
		updateInfo("Aligning sequences please wait.", "Results will be written to the /ResultAlign directory");
		
		if(theThread == null) {
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						theMultiAlignData = new MultiAlignData(theViewerFrame.getInfo());
						
						if (isAA && theHitid!=null && !Globals.hasSpecialID(theHitid)) {
							String hitSeq = loadHitSeqFromDB(theHitid);
							theMultiAlignData.addSequence(theHitid, hitSeq);
						}
						
						String [] memSeq = loadSequencesFromDB(theMembers, isAA);
						for(int x=0; x<theMembers.length; x++) 
							theMultiAlignData.addSequence(theMembers[x], memSeq[x]);
						
						int rc =  theMultiAlignData.runAlignPgm(alignPgm, isAA); 
						
						bAlign = (rc==0) ? true : false;
						String scores = (bAlign) ? theMultiAlignData.getGlScores() : "";
						if (scores=="") {
							sumLine2 = pgmType; 
							btnScore.setEnabled(false);							// CAS340 nt aligns have no scores
						}
						else  {
							sumLine2 = "Scores: " +  scores + "   " + pgmType; // CAS340
							btnScore.setEnabled(true);
						}
						
						createViewAlign();
						
						theViewerFrame.setStatus("Results in /ResultAlign");
						
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}
		else {Out.PrtError("Did not start thread");}
	}
	private MultiViewPanel getInstance() {return this;}
	
    private String [] loadSequencesFromDB(String [] IDs, boolean isAA) {
    	ResultSet rs = null;
    	
    	try {
	    	DBConn mDB = theViewerFrame.getDBConnection();
	  
	    	Vector<String> sequences = new Vector<String> ();
	    	
	    	if (alignType==Globals.AA) {
	    		for(int x=0; x<IDs.length; x++) {
	    			rs = mDB.executeQuery("SELECT aaSeq FROM unitrans WHERE UTstr= '" + IDs[x] + "'");
	    			rs.next();
	    			String seq = rs.getString(1);
	    			if (seq!=null && !seq.equals("")) sequences.add(seq);
	    		}
	    	}
	    	else {
	    		for(int x=0; x<IDs.length; x++) {
		    		rs = mDB.executeQuery("SELECT ntSeq, orf_start, orf_end FROM unitrans" +
    						" WHERE UTstr= '" + IDs[x] + "'");
		    		rs.next();
	    			String seq = rs.getString(1);
	    			if (seq!=null && !seq.equals("")) {
	    				if (alignType==Globals.CDS)
	    					seq = seq.substring(rs.getInt(2)-1, rs.getInt(3));
	    				sequences.add(seq);
	    			}
	    		}
	    	}	
    		if (rs!=null) rs.close();
    		mDB.close();
    		
    		return sequences.toArray(new String[sequences.size()]); 
    	}
    	catch(Exception e) {
    		Out.Print("Load sequences from DB");
    		ErrorReport.reportError(e);
    		return null;
    	}
    }	
    private String loadHitSeqFromDB(String hitID) {
    	try {
	    	DBConn mDB = theViewerFrame.getDBConnection();
	    	String seq = mDB.executeString("SELECT sequence FROM unique_hits WHERE HITstr= '" + hitID + "'");
	    	return seq;
    	}
    	catch(Exception e) {
    		ErrorReport.prtReport(e, "loading hit sequence");
    		return null;
    	}
    }
    
	/*****************************************************************************/
	private void refreshPanels() {
		try {
			refreshMultiPanels();
			
			mainPanel.revalidate();
			mainPanel.repaint();
			setVisible(false); // occasionally the panels do not show
			setVisible(true);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Refreshing panels");}
	}
	
	private void refreshMultiPanels() {
		mainPanel.removeAll();
		try {
			if(theAlignPanel == null) {
				mainPanel.add(new JLabel("No MSA Sequences"));
				return;
			}
			int mode = (isShowGraphic) ? BaseAlignPanel.GRAPHICMODE : BaseAlignPanel.TEXTMODE;
			int col = menuColor.getSelectedIndex();
			boolean bDot = dotBox.isSelected();
			boolean bTrim = trimBox.isSelected();
			
			MenuMapper ratioSelection = (MenuMapper) menuZoom.getSelectedItem();
			int nZoom = ratioSelection.asInt();
			
			theAlignPanel.setOpts(mode, col, bDot, bTrim, nZoom); // causes repaint
			
			mainPanel.add(theAlignPanel);

			mainPanel.add(Box.createVerticalStrut(40));
			
			LegendPanel lPanel = new LegendPanel(isAA, col);
			mainPanel.add(lPanel);
			
		} catch (Exception e) {ErrorReport.reportError(e);}
	}
	
	/********************************************************************/
	private void handleClick(MouseEvent e) {
		if(theAlignPanel == null) return;
		
		// Convert to view relative coordinates
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
		
		// Get the panel and convert to panel relative coordinates
		int nPanelX = viewX - theAlignPanel.getX();
		int nPanelY = viewY - theAlignPanel.getY();

		if (theAlignPanel.contains(nPanelX, nPanelY) )
			theAlignPanel.handleClick( e, new Point(nPanelX, nPanelY) );
		else {
			theAlignPanel.selectNoRows();
			theAlignPanel.selectNoColumns();
			updateInfo("", "");
		}
	}
	/*******************************************************************/
	private void getNextRow(int rowNum) { // only Clusters can have newRow
		removeAll(); // Container call to remove everything
		
		String [] strVals = theAlignButtons.getNextRow(rowNum); 
		if (strVals==null) return;
		
		tabName = 			strVals[0];
		sumLine1 = 			strVals[1];
		nParentRow = 		Static.getInteger(strVals[2], -1);	
		
		int grpID = 		Static.getInteger(strVals[3], -1);
		String hitID = 		strVals[4];
		
		if (sumLine1.contains("(")) {
			int idx =  sumLine1.indexOf("(");
			sumLine2 = "Scores: " + sumLine1.substring(idx);
			sumLine1 = sumLine1.substring(0, idx);
		}
		sumLine1 = Globals.trimSum(sumLine1);
		
		String [] members = theAlignButtons.getSeqIDrow(nParentRow);
		if (isFromDB) 	{
			tabName += theAlignButtons.makeTagMSAdb();
			buildMSAdb(members, grpID);
		}
		else {
			tabName += theAlignButtons.makeTagMSA(alignPgm, alignType);
			sumLine2 += pgmType;
			theThread = null;
			buildMSArun(members, hitID);
		}
	
        theViewerFrame.changePanelName(this, tabName, sumLine1);
	}
	
	/*****************************************************/	
	private JPanel mainPanel = null, buttonPanel = null,  textPanel = null;
	
	private JScrollPane scroller = null;
	
	private JButton btnNextRow = null, btnPrevRow = null;
	private JButton btnScore = null;

	private JComboBox <String> menuColor = null;
	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnViewType = null;
	private JCheckBox dotBox = null, trimBox=null;
	private JTextField infoText1 = null, infoText2 = null;
	
	private Thread theThread = null;
	
	private MTCWFrame theViewerFrame = null;
	private AlignButtons theAlignButtons = null;
	private MultiAlignData theMultiAlignData = null;
	private MultiAlignPanel theAlignPanel = null;
	
	private int alignType=0, alignPgm=0;
	private boolean isAA=true, bAlign=true, isFromDB=false;
	private boolean isShowGraphic=true;
	
	private String tabName="", sumLine1 = "", sumLine2="", pgmType="";
	private int nParentRow = -1;
}
