package cmp.viewer.seq.align;

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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	public MultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int alignPgm, int type, String sum) {
		theMainFrame = parentFrame;
		isAA = (type==Globals.AA);
		alignType = type;
		headerLine = sum;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globalx.BGCOLOR);
		
		buildMSArun(alignPgm, POGMembers);
	}
	// displays existing MSA from database
	public MultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int grpid, String sum) {
		theMainFrame = parentFrame;
		isAA = true;
		alignType = Globals.AA;
		headerLine = sum;
		
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
		
		JButton btnHelp1 = Static.createButton("Help1", true, Globals.HELPCOLOR); // CAS312
		btnHelp1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theMainFrame, "Alignment...", help1HTML);
			}
		});
		theRow.add(btnHelp1);
		theRow.add(Box.createHorizontalStrut(10));
		
		JButton btnScore = Static.createButton("Scores...", true, Globals.PROMPTCOLOR); // CAS312
		btnScore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String s1 = theMainFrame.getInfo().getMSA_Score1();
				String s2 = theMainFrame.getInfo().getMSA_Score2();
				theAlignPanel.showScores(theMainFrame, s1, s2, headerLine);
			}
		});
		theRow.add(btnScore); theRow.add(Box.createHorizontalStrut(2));
		JButton btnHelp2 = Static.createButton("Help2", true, Globals.HELPCOLOR); // CAS312
		btnHelp2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theMainFrame, "MSA Scores...", help2HTML);
			}
		});
		theRow.add(btnHelp2);
		
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
			createButtonPanel();	add(buttonPanel);
			createInfo();			add(infoText);
			add(Box.createVerticalStrut(10));
			
			loadMultiDB(POGMembers, grpid);
			
			createMultiAlignPanels();	
			
			createMainPanel();		add(scroller);
			
			updateInfo("");
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
						createButtonPanel();	add(buttonPanel);
						createInfo();			add(infoText);
						add(Box.createVerticalStrut(10));
						
						String scores = loadMultiAndRun(pgm, theMembers);
						
						createMainPanel();		add(scroller);
	
						updateInfo("Results also in /ResultAlign " + scores);
						
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}
	private void createMultiAlignPanels() {
		theAlignPanel = new MultiAlignPanel(getInstance(),
						isAA, 
						theMultiAlignData);
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
		updateInfo("Aligning sequences please wait. Results will be written to the ResultAlign directory");
		
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
	private void createInfo() {
		infoText = new JTextField(100);
		infoText.setEditable(false);
		infoText.setMaximumSize(infoText.getPreferredSize());
		infoText.setBackground(Globalx.BGCOLOR);
		infoText.setBorder(BorderFactory.createEmptyBorder());
	}
	public void updateInfo(String status) { // Called in MultiAlign for column click
		String sp="   ";
		if (status==null || status=="") infoText.setText(sp + headerLine);
		else                            infoText.setText(sp + status);
	}
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
			updateInfo("");
		}
	}
	private int alignType=0;
	private boolean isAA=true, bAlign=true;
	private boolean isShowGraphic=true;
	
	private MTCWFrame theMainFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null;
	private JPanel mainPanel = null;

	private JComboBox <String> menuColor = null;
	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnViewType = null;
	private JCheckBox dotBox = null, trimBox=null;
	
	private JTextField infoText = null;
	private String headerLine = "";
	
	private Thread theThread = null;
	
	private MultiAlignData theMultiAlignData = null;
	private MultiAlignPanel theAlignPanel = null;
}
