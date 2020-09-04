package cmp.viewer.seq;

/*************************************************************
 * Called from SeqsTopRowPanel.opMulti to show a Muscle or Mafft multiple alignment
 */
import java.awt.Color;
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

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.MenuMapper;
import cmp.align.MultiAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class AlignMultiViewPanel extends JPanel {
	private static final long serialVersionUID = -2090028995232770402L;
	
	// runs MSA program and displays results
	public AlignMultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int alignPgm, int type) {
		theParentFrame = parentFrame;
		isAA = (type==Globals.AA);
		alignType = type;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(theParentFrame.getSettings().getFrameSettings().getBGColor());
		
		buildMultiAlignments(alignPgm, POGMembers);
	}
	// displays existing MSA from database
	public AlignMultiViewPanel(MTCWFrame parentFrame, String [] POGMembers, int grpid) {
		theParentFrame = parentFrame;
		isAA = true;
		alignType = Globals.AA;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(theParentFrame.getSettings().getFrameSettings().getBGColor());
		
		buildMultiAlignments(POGMembers, grpid);
	}
	private void createButtonPanel() {
		buttonPanel = Static.createPagePanel();
		
		add(createButtonRow());
		
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
		
	private JPanel createButtonRow() {
		JPanel theRow = Static.createRowPanel();
		
		String msg = (isAA) ? "View Amino Acids" : "View Nucleotides";
		btnShowType = Static.createButton(msg, bAlign); 
		btnShowType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(isShowGraphic) {
					isShowGraphic = false;
					btnShowType.setText("View Graphic");
					menuZoom.setEnabled(false);
				}
				else {
					isShowGraphic = true;
					String msg = (isAA) ? "View Amino Acid" : "View Nucleotides";
					btnShowType.setText(msg);
					menuZoom.setEnabled(true);
				}
				refreshPanels();
			}
		});
			
		theRow.add(btnShowType);
		theRow.add(Box.createHorizontalStrut(10));
		
		menuZoom = Static.createZoom();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					refreshPanels();
			}
		});	
		theRow.add(menuZoom);
		theRow.add(Box.createHorizontalStrut(10));

		theRow.setMaximumSize(theRow.getPreferredSize());
		return theRow;
	}
	
	private void createMultiAlignPanels() {
		theMultiPanel = new AlignMultiPanel(theParentFrame, isAA, theMultiAlignData, 10, 10, 10, 10);
		theMultiPanel.setAlignmentY(Component.LEFT_ALIGNMENT);
	}
	private void buildMultiAlignments(String [] POGMembers, int grpid) {
		try {
			setStatus();
			
			loadMultiDB(POGMembers, grpid);
			
			createButtonPanel();
			createMainPanel();
			showStatus(false);
		
			add(buttonPanel);
			add(scroller);
			
		} catch (Exception e) {e.printStackTrace();}
	}
	private void buildMultiAlignments(int alignPgm, String [] POGMembers) {
		final String [] theMembers = POGMembers;
		final int pgm = alignPgm;
		if(theThread == null)
		{
			theThread = new Thread(new Runnable() {
				public void run() {
					try {
						setStatus();
						
						loadMultiAndRun(pgm, theMembers);
						
						createButtonPanel();
						add(buttonPanel);
						
						createMainPanel();
						add(scroller);
						
						showStatus(false);
						
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			theThread.setPriority(Thread.MIN_PRIORITY);
			theThread.start();
		}		
	}
		
	private void setStatus() {
		progressField = new JTextField(100);
		progressField.setEditable(false);
		progressField.setMaximumSize(progressField.getPreferredSize());
		progressField.setBackground(theParentFrame.getSettings().getFrameSettings().getBGColor());
		progressField.setBorder(BorderFactory.createEmptyBorder());
		
		add(progressField);
		add(Box.createVerticalStrut(10));
	}
	private void showStatus(boolean show) {
		//btnCancel.setVisible(show); //  doesn't works
		progressField.setVisible(show);
	}
	private void updateStatus(String status) {
		progressField.setText(status);
	}
	
    private String [] loadSequencesFromDB(String [] IDs, boolean isAA) {
	    	ResultSet rs = null;
	    	Vector<String> sequences = null;
	    	try {
		    	DBConn mDB = theParentFrame.getDBConnection();
		  
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
	    		ErrorReport.reportError(e);
	    		return null;
	    	}
    }	
	private void loadMultiAndRun(int alignPgm, String [] POGMembers) {
		String [] POGSeq = loadSequencesFromDB(POGMembers, isAA);
		
		theMultiAlignData = new MultiAlignData(alignPgm);
		for(int x=0; x<POGMembers.length; x++) {
			if(POGSeq[x] != null)
				theMultiAlignData.addSequence(POGMembers[x], POGSeq[x]);
			else
				System.out.println("Error: no sequence in database for " + POGMembers[x]);
		}
		updateStatus("Aligning sequences please wait. Results will be written to the ResultAlign directory");
		int rc = theMultiAlignData.runAlignPgm(false, true, isAA); // false - program output, true - write scores to file
		bAlign = (rc==0) ? true : false;
		createMultiAlignPanels();
	}
	private void loadMultiDB(String [] POGMembers, int grpid) {
		String [] POGSeq = loadSequencesFromDB(POGMembers, isAA);
		
		theMultiAlignData = new MultiAlignData();
		for(int x=0; x<POGMembers.length; x++) {
			if(POGSeq[x] != null)
				theMultiAlignData.addSequence(POGMembers[x], POGSeq[x]);
			else
				System.out.println("Error: no sequence in database for " + POGMembers[x]);
		}
		bAlign = theMultiAlignData.getAlign(theParentFrame, grpid); 
		createMultiAlignPanels();
	}
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
			MenuMapper ratioSelection = (MenuMapper) menuZoom.getSelectedItem();
			int ratio = ratioSelection.asInt();
			
			if(theMultiPanel != null) {
				theMultiPanel.setBorderColor(Color.BLACK);
				theMultiPanel.setBasesPerPixel(ratio);
				if(isShowGraphic)
					theMultiPanel.setDrawMode(AlignBasePanel.GRAPHICMODE);
				else
					theMultiPanel.setDrawMode(AlignBasePanel.TEXTMODE);
				
				mainPanel.add(theMultiPanel);

				mainPanel.add(Box.createVerticalStrut(40));
				LegendPanel lPanel = new LegendPanel((isAA) ? Globals.AA : Globals.NT);
				mainPanel.add(lPanel);
			} else {
				mainPanel.add(new JLabel("No Sequences"));
			}
		} catch (Exception e) {ErrorReport.reportError(e);}
	}

	private void handleClick(MouseEvent e) {
		if(theMultiPanel != null) {
			// Convert to view relative coordinates
			int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
			int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
			
			// Get the panel and convert to panel relative coordinates
			int nPanelX = viewX - theMultiPanel.getX();
			int nPanelY = viewY - theMultiPanel.getY();
	
			if ( theMultiPanel.contains( nPanelX, nPanelY ) )
				// Click is in current panel, let the object handle it
				theMultiPanel.handleClick( e, new Point( nPanelX, nPanelY ) );
			else
				// Clear all selections in the panel unless shift or control are down
				if ( !e.isShiftDown() && !e.isControlDown() ) {
					theMultiPanel.selectNoRows();
					theMultiPanel.selectNoColumns();
				}
		}
	}
	private int alignType=0;
	private boolean isAA=true, bAlign=true;
	private boolean isShowGraphic=true;
	
	private MTCWFrame theParentFrame = null;
	private JScrollPane scroller = null;
	
	private JPanel buttonPanel = null;
	private JPanel mainPanel = null;

	private JComboBox <MenuMapper> menuZoom = null;
	private JButton btnShowType = null;
	
	private JTextField progressField = null;
	
	private Thread theThread = null;
	
	private MultiAlignData theMultiAlignData = null;
	private AlignMultiPanel theMultiPanel = null;
}
