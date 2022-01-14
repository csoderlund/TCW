package cmp.viewer.align;

/*************************************************************
 * Called from SeqsTopRowPanel.opMulti to show a Muscle or Mafft multiple alignment
 */
import java.awt.Component;
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
	private static final int INFOSZ = 200;
	
	// runs MSA program and displays results
	public MultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int alignPgm, int type, String sum) {
		theMainFrame = parentFrame;
		isAA = (type==Globals.AA);
		alignType = type;
		
		sumLine1 = sum;
		if (sumLine1.contains("(")) sumLine1 = sumLine1.substring(0, sumLine1.indexOf("(")); // Remove score
		if (sumLine1.length()>INFOSZ-4)  sumLine1 = sumLine1.substring(0,INFOSZ-4) + "...";
		
		pgmType = (alignPgm==Globals.Ext.MUSCLE) ? "MUSCLE" : "MAFFT"; // CAS340 
		if (type==Globals.AA) 		pgmType += " AA";
		else if (type==Globals.NT) 	pgmType += " NT";
		else 						pgmType += " CDS";
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globalx.BGCOLOR);
		
		buildMSArun(alignPgm, POGMembers);
	}
	// displays existing MSA from database
	public MultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int grpid, String sum) {
		theMainFrame = parentFrame;
		isAA = true;
		alignType = Globals.AA;
		
		sumLine1 = sum;
		if (sumLine1.contains("(")) {
			int idx =  sumLine1.indexOf("(");
			sumLine2 = "Scores: " + sumLine1.substring(idx);
			sumLine1 = sumLine1.substring(0, idx);
		}
		if (sumLine1.length()>INFOSZ-4)  sumLine1 = sumLine1.substring(0,INFOSZ-4) + "...";
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globalx.BGCOLOR);
		
		buildMSAfromDB(POGMembers, grpid);
	}
	
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
		
		menuZoom = Static.createZoom2(); // CAS312 Zoom2 allows increase size
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshPanels();
			}
		});	
		theRow.add(menuZoom);
		theRow.add(Box.createHorizontalStrut(5));

		menuColor = Static.createCombo(BaseAlignPanel.colorSchemes); // CAS312 add
		menuColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				refreshPanels();
			}
		});
		if (isAA) {
			theRow.add(menuColor);
			theRow.add(Box.createHorizontalStrut(5));
		}
		
		
		btnScore = Static.createButtonMenu("Scores...", true); // CAS312
		btnScore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String s1 = theMainFrame.getInfo().getMSA_Score1();
				String s2 = theMainFrame.getInfo().getMSA_Score2();
				theAlignPanel.showScores(theMainFrame, s1, s2, sumLine2);
			}
		});
		theRow.add(btnScore); theRow.add(Box.createHorizontalStrut(30));
		theRow.add(Box.createHorizontalGlue());
		
		final JPopupMenu popup = new JPopupMenu();
		
		popup.add(new JMenuItem(new AbstractAction("Alignment") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Alignment", help1HTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing help1"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Scores") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theMainFrame, "Scores", help2HTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error showing help2"); }
			}
		}));
		JButton btnHelp = Static.createButtonHelp("Help...", true); // CAS312, CAS340...
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		theRow.add(btnHelp);
		
		theRow.setMaximumSize(theRow.getPreferredSize());
		
		buttonPanel.add(theRow);
		buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
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
	/***********************************************************************
	 * MSA methods
	 */
	private void buildMSAfromDB(String [] POGMembers, int grpid) {
		try {
			add(Box.createVerticalStrut(10));
			
			createButtonPanel();	
			add(buttonPanel);
			add(Box.createVerticalStrut(10));
			
			createInfo();			
			add(textPanel);
			add(Box.createVerticalStrut(5));
			
			loadMultiDB(POGMembers, grpid);
			
			createMultiAlignPanels();	
			
			createMainPanel();		
			add(scroller);
			
			updateInfo("", "");
		} 
		catch (Exception e) {ErrorReport.prtReport(e, "MSA from DB");}
	}
	private void buildMSArun(int alignPgm, String [] POGMembers) {
		final String [] theMembers = POGMembers;
		final int pgm = alignPgm;
		if(theThread == null)
		{
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						add(Box.createVerticalStrut(10));
						createButtonPanel();	
						add(buttonPanel);
						add(Box.createVerticalStrut(10));
						
						createInfo();			
						add(textPanel);
						add(Box.createVerticalStrut(5));
						
						String scores = loadMultiAndRun(pgm, theMembers);
						
						createMainPanel();		
						add(scroller);
						
						if (scores=="") btnScore.setEnabled(false);// CAS340 nt aligns have no scores
						else 			btnScore.setEnabled(true);
						
						if (scores=="") sumLine2 = pgmType; // CAS340
						else sumLine2 = "Scores: " +  scores + "   " + pgmType; // CAS340
						
						updateInfo("", ""); 			 // sets summary as info
						theMainFrame.setStatus("Results in /ResultAlign");
	
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}
	private void createMultiAlignPanels() {
		theAlignPanel = new MultiAlignPanel(getInstance(),isAA, theMultiAlignData);
		theAlignPanel.setAlignmentY(Component.LEFT_ALIGNMENT);
	}	
	private MultiViewPanel getInstance() {return this;}
	
    private String [] loadSequencesFromDB(String [] IDs, boolean isAA) {
    	ResultSet rs = null;
    	Vector<String> sequences = null;
    	try {
	    	DBConn mDB = theMainFrame.getDBConnection();
	  
	    	sequences = new Vector<String> ();
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
    
    private String loadMultiAndRun(int alignPgm, String [] POGMembers) {
		String [] POGSeq = loadSequencesFromDB(POGMembers, isAA);
		
		theMultiAlignData = new MultiAlignData(theMainFrame.getInfo());
		for(int x=0; x<POGMembers.length; x++) {
			theMultiAlignData.addSequence(POGMembers[x], POGSeq[x]);
		}
		updateInfo("Aligning sequences please wait.", "Results will be written to the /ResultAlign directory");
		
		int rc = theMultiAlignData.runAlignPgm(alignPgm, isAA); 
		String scores = theMultiAlignData.getGlScores();
		
		bAlign = (rc==0) ? true : false;
		
		createMultiAlignPanels();
		return scores;
	}
	private void loadMultiDB(String [] POGMembers, int grpid) {
	try {	
		String [] POGSeq = loadSequencesFromDB(POGMembers, isAA);
		
		theMultiAlignData = new MultiAlignData(null);
		for(int x=0; x<POGMembers.length; x++) 
			theMultiAlignData.addSequence(POGMembers[x], POGSeq[x]);
		
		DBConn mDB = theMainFrame.getDBConnection();
		
		bAlign = theMultiAlignData.loadAlignFromDB(mDB, grpid); 
		
		mDB.close();
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Load Multi DB");}
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
	/*************************************************************************/
	private void createInfo() {
		textPanel = Static.createPagePanel();
		
		infoText1 = Static.createTextFieldNoEdit(INFOSZ);
		textPanel.add(infoText1); 
		textPanel.add(Box.createVerticalStrut(5));
		
		infoText2 = Static.createTextFieldNoEdit(INFOSZ);
		textPanel.add(infoText2);
	}
	public void updateInfo(String msg1, String msg2) { 
		String sp="   ";
		
		if (msg1!=null && msg1!="") infoText1.setText(sp + msg1);
		else 						infoText1.setText(sp + sumLine1);
		
		if (msg2!=null && msg2!="") infoText2.setText(sp + msg2);
		else 						infoText2.setText(sp + sumLine2);
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
	
	private int alignType=0;
	private boolean isAA=true, bAlign=true;
	private boolean isShowGraphic=true;
	
	private MTCWFrame theMainFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null, mainPanel = null, textPanel = null;
	private JButton btnScore = null;

	private JComboBox <String> menuColor = null;
	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnViewType = null;
	private JCheckBox dotBox = null, trimBox=null;
	
	private JTextField infoText1 = null, infoText2 = null;
	private String sumLine1 = "", sumLine2="", pgmType="";
	
	private Thread theThread = null;
	
	private MultiAlignData theMultiAlignData = null;
	private MultiAlignPanel theAlignPanel = null;
}
