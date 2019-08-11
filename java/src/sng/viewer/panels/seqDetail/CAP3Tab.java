package sng.viewer.panels.seqDetail;

/************************************************
 * Run CAP3 for the Contig display of Sequence Detail
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.Preferences;

import sng.dataholders.MultiCtgData;
import sng.util.HiddenTabbedPane;
import sng.util.InterThreadProgress;
import sng.util.MenuTreeNode;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;

/*
 * Called from MainToolAlignPanel, which contains the "Run CAP3" button.
 */
public class CAP3Tab extends Tab 
{		
	private static final long serialVersionUID = -8527294478705980476L;
	public static final String NO_CAP3= "No CAP3 results";
	
	public CAP3Tab ( STCWFrame inFrame, int inRecordNum, 				
			final String strCAP, TreeSet<String> inESTs, final Vector<String> contigIDs)
	{		
		super(inFrame, null); 				
		
		theESTs = inESTs;
		nRecordNum = inRecordNum; 

		try {
			DBConn dbc = inFrame.getNewDBC();
			LoadFromDB dbLoadObj = new LoadFromDB(dbc, inFrame.getMetaData());
			execObj = new CAP3exec(dbLoadObj);
			theParams = dbLoadObj.getCAPparams();
			dbc.close();
		} catch (Exception e){}
		
	 	okButton = new JButton ( "Run" );
	 	okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String strOptions = optionEditPanel.getOptionString();
				if (strOptions != null) {
					try {
						optionEditPanel.getUserPrefs(userPrefs);// Save all of the settings to the user preferences
					} catch (Exception err) {ErrorReport.reportError(err, "Running CAP");}
					
					alignESTs(strCAP, contigIDs);
				}
			}
		});
	 	
		resetButton = new JButton ( "CAP3 Defaults" );
	 	resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				optionEditPanel.restoreDefaults();
				optionEditPanel.getOptionString(); // force validation to clear errors
			}
		});
	 	
		cancelButton = new JButton ( "Cancel" );
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HiddenTabbedPane parentTabPane = (HiddenTabbedPane) getParent(); 
				parentTabPane.remove(CAP3Tab.this);
				
				MenuTreeNode node = getParentFrame().menuTree.getNodeWithUserObject(CAP3Tab.this);
				MenuTreeNode parentNode = node.getParentNode();
				getParentFrame().menuTree.removeNode(node);
				getParentFrame().menuTree.setSelected(parentNode);
				parentTabPane.setSelectedTab( (Tab)parentNode.getUserObject() );
			}
		});
		
		lblTitle = new JLabel(); 
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc = new JTextArea();
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);

		doCAPSetup ( getParentFrame() );
		
		bottomPanel = Static.createRowPanel();
		bottomPanel.add ( Box.createHorizontalGlue( ) );
		bottomPanel.add ( cancelButton );
		bottomPanel.add ( Box.createHorizontalStrut ( 10 ) );
		bottomPanel.add ( resetButton );
		bottomPanel.add ( Box.createHorizontalStrut ( 10 ) );	

		if (assemblyButton != null) {
			bottomPanel.add ( assemblyButton );						
			bottomPanel.add ( Box.createHorizontalStrut ( 10 ) );
		}

		bottomPanel.add ( okButton );
		bottomPanel.add ( Box.createHorizontalGlue ( ) );
		bottomPanel.setMaximumSize( bottomPanel.getPreferredSize() );
		
		// Install everything into the main panel
		setLayout ( new BoxLayout ( this, BoxLayout.Y_AXIS ) );

		setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		optionEditPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setMaximumSize(txtDesc.getPreferredSize());
		
		add ( lblTitle );
		add ( txtDesc );
		add ( Box.createVerticalStrut ( 20 ) );	
		add ( optionEditPanel );
		add ( Box.createVerticalStrut ( 30 ) );	
		add ( bottomPanel );
		add ( Box.createVerticalGlue() );
	}
		
	private void alignESTs ( final String strCommand, final Vector<String> contigIDs ) 
	{	
        final InterThreadProgress progress = new InterThreadProgress ( getParentFrame() );
        progress.swapInTabAndStartThread( this, "Running " + strName, 
			new InterThreadProgress.WorkerThread ()
			{
				public void run () throws Throwable
				{
                    progress.setProgressLabel( "Waiting for " + strName + " to finish." );

					MultiCtgData cluster = execObj.executeCAP( strCommand,
							theESTs, 
							optionEditPanel.getOptionString (),
							nRecordNum);
					
					if ( cluster == null || cluster.getContig()==null) 
						cluster.addContigStub(NO_CAP3);
					
                    progress.swapOutProgressWithCAP3Tab ( cluster, 
                    		nRecordNum, 
                    		strName);                    
				}
			}
		);
	}
	public String getAlignName ( ) { return strName; }
	
	private void doCAPSetup ( STCWFrame theFrame )
	{
		strName = "CAP3";
		
		lblTitle.setText("Run CAP3");
		txtDesc.setText("Either change the CAP3 parameters to use.\n" +
						"Or select the 'CAP3 Defaults' button to use CAP3 default parameters.\n" +
						"Or select the 'Assembly (ReCAP)' button to use the assembly CAP3 parameters used for ReCAPing.\n" + 
        				"Finally, click the 'Run' button to execute CAP3, or 'Cancel' to abort.");
		
		// Setup the option panel:
		optionEditPanel = new CAP3LineInputPanel ( "_param_user_pref" );	
		optionEditPanel.addIntOptionRow ( "a", "(-a) Band expansion size", 10, Integer.MAX_VALUE, 20 );
		optionEditPanel.addIntOptionRow ( "b", "(-b) Base quality cutoff for differences", 15, Integer.MAX_VALUE, 20 );
		optionEditPanel.addIntOptionRow ( "c", "(-c) Base quality cutoff for clipping", 5, Integer.MAX_VALUE, 12 );
		optionEditPanel.addIntOptionRow ( "d", "(-d) Max qscore sum at differences", 20, Integer.MAX_VALUE, 200 );
		optionEditPanel.addIntOptionRow ( "e", "(-e) Clearance between num. of diff", 10, Integer.MAX_VALUE, 30 );
		optionEditPanel.addIntOptionRow ( "f", "(-f) Max gap length in any overlap", 1, Integer.MAX_VALUE, 20 );
		optionEditPanel.addIntOptionRow ( "g", "(-g) Gap penalty factor", 0, Integer.MAX_VALUE, 6 );
		optionEditPanel.addIntOptionRow ( "h", "(-h) Max overhang percent length", 2, Integer.MAX_VALUE, 20 );
		optionEditPanel.addIntOptionRow ( "m", "(-m) Match score factor", 0, Integer.MAX_VALUE, 2 );
		optionEditPanel.addIntOptionRow ( "n", "(-n) Mismatch score factor", Integer.MIN_VALUE, 0, -5 );
		optionEditPanel.addIntOptionRow ( "o", "(-o) Overlap length cutoff", 20, Integer.MAX_VALUE, 40 );
		optionEditPanel.addIntOptionRow ( "p", "(-p) Overlap percent identity cutoff", 65, Integer.MAX_VALUE, 80 );
		optionEditPanel.addIntOptionRow ( "r", "(-r) Reverse orientation value", -1, Integer.MAX_VALUE, 1 );
		optionEditPanel.addIntOptionRow ( "s", "(-s) Overlap similarity score cutoff", 400, Integer.MAX_VALUE, 900 );
		optionEditPanel.addIntOptionRow ( "t", "(-t) Max number of word matches", 30, Integer.MAX_VALUE, 300 );
		optionEditPanel.addIntOptionRow ( "u", "(-u) Min number of constraints for correction", 0, Integer.MAX_VALUE, 3 );
		optionEditPanel.addIntOptionRow ( "v", "(-v) Min number of constraints for linking", 0, Integer.MAX_VALUE, 2 );
		optionEditPanel.addIntOptionRow ( "y", "(-y) Clipping range", 5, Integer.MAX_VALUE, 250 );
		optionEditPanel.addIntOptionRow ( "z", "(-z) Min num. of good reads at clip pos", 0, Integer.MAX_VALUE, 3 );
		optionEditPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)optionEditPanel.getPreferredSize().getHeight() ) );
    
        // Setup and load preferences	
		userPrefs = theFrame.getPreferencesRoot();
		if (userPrefs!=null) {
			userPrefs = theFrame.getPreferencesRoot().node( "CAP3" );
        		optionEditPanel.setUserPrefs( userPrefs );  
		}
		assemblyButton = new JButton ( "Assembly (ReCAP)" );
		assemblyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				optionEditPanel.restoreDefaults();
				optionEditPanel.setOptions(theParams);
				optionEditPanel.getOptionString(); 
			}
		});
	    if (theParams == null || theParams.equals(""))
	    		assemblyButton.setEnabled(false);
	    else optionEditPanel.setOptions(theParams);
	}

	private String strName = null;
	private int nRecordNum = -1; 
	
	private CAP3exec execObj=null;
	private Preferences userPrefs = null;
	
	private CAP3LineInputPanel optionEditPanel;
	private JLabel lblTitle; 	
	private JTextArea txtDesc; 	

	private TreeSet<String> theESTs = null;
	private JPanel bottomPanel;
	private JButton okButton;
	private JButton resetButton;
	private JButton assemblyButton; 	
	private JButton cancelButton;
	private String theParams = null; 
}