package sng.runDE;

/*********************************************************
 * CAS403 made DB chooser a separate method and fixed exits
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import sng.database.DBInfo;
import sng.database.Overview;
import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

public class QRChooser  extends JDialog implements WindowListener {
	private static final long serialVersionUID = 1L;
	
	private static String helpHTML = "html/runDE/QRChooser.html";
	private static Vector<QRFrame> openWindows = null;
	
	private HostsCfg hostsObj=null;
	
	/************************************************
	 * Called from QRmain to open TCW selection panel
	 */
	public QRChooser(HostsCfg h, Vector <DBInfo> list) {		
		hostsObj = h;
		
		openWindows = new Vector<QRFrame> ();
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
		
		JPanel selectPanel = createTCWdbSelectionPanel(list);
		
		try {setTitle("runDE Database Chooser ");}
		catch (Exception e){ setTitle("runDE");}
		
		setResizable(true);
		add(selectPanel);
		setWindowSettings("deframechooser");
		setVisible(true);		
	}
	/******************************************************
	 * TCWdb chooser methods
	 */
	private JPanel createTCWdbSelectionPanel(Vector<DBInfo> dbList) 
	{   
       // Create a tree of the hosts
       DefaultMutableTreeNode hostTree = new DefaultMutableTreeNode("");
       DefaultMutableTreeNode newNode =  new DefaultMutableTreeNode(hostsObj.host());
	   hostTree.add(newNode);
	   DefaultMutableTreeNode start = newNode;

       for (int i = 0; i < hostTree.getChildCount(); i++) {
           DefaultMutableTreeNode hostNode = (DefaultMutableTreeNode) hostTree.getChildAt(i);
           
           for (DBInfo dbi : dbList) {
               hostNode.add(new DefaultMutableTreeNode(dbi));
           } 
       }

       // Create display tree with hostTree
       DefaultTreeModel dbModel = new DefaultTreeModel(hostTree);
       final JTree dbTree = new JTree(dbModel);
       dbTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
       dbTree.setRootVisible(false);
       dbTree.setToggleClickCount(1);
       ((DefaultTreeCellRenderer) dbTree.getCellRenderer()).setLeafIcon(null);
       if (dbTree.getRowCount() == 1) dbTree.expandRow(0);

       dbTree.addMouseListener(new MouseAdapter() {
           public void mouseClicked(MouseEvent e) {
        	   try {
	               TreePath path = dbTree.getSelectionPath();
	               if (path != null) {
	                   int depth = path.getPathCount();
	                   if (e.getClickCount() == 2 && depth >= 3) {
	                       DefaultMutableTreeNode node =
	                           (DefaultMutableTreeNode) dbTree.getLastSelectedPathComponent();
	                       DBInfo dbi = (DBInfo) node.getUserObject();
	                       QRFrame x = new QRFrame(hostsObj, dbi, getInstance());
	                       openWindows.add(x);
	                   }
	               }
        	   }
        	   catch(Exception ex) {ErrorReport.prtReport(ex, "Error launching DE window");}
           }
       });

       // create scroll pane with dbTree
       final JScrollPane dbScrollPane = new JScrollPane(dbTree);
       dbScrollPane.setPreferredSize(new Dimension(400, 400));
       Dimension dim = dbScrollPane.getMaximumSize();
       dbScrollPane.setMaximumSize(new Dimension(Math.max(400,
               (int) dim.getWidth()), Math.max(400, (int) dim.getHeight())));
       dbScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

       // create buttons
       final JButton btnViewDB = Static.createButtonPopup("Launch", false);
       btnViewDB.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent event) {
        	   try {
	               DefaultMutableTreeNode node = (DefaultMutableTreeNode)
	                   dbTree.getLastSelectedPathComponent();
	               DBInfo dbi = (DBInfo) node.getUserObject();
	               QRFrame x = new QRFrame(hostsObj, dbi, getInstance());
	               openWindows.add(x);
        	   }
        	   catch(Exception e) {ErrorReport.prtReport(e, "Error launching DE window");}
           }
       });
       
		final JButton btnGetState = Static.createButtonPopup("Overview", false);
		btnGetState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) dbTree.getLastSelectedPathComponent();
					DBInfo dbi = (DBInfo) node.getUserObject();
					String dbName = dbi.getdbName();
					DBConn dbc = hostsObj.getDBConn(dbName);
					dbOverview(dbName, dbc);
					dbc.close();
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error getting overview");}
			}
		});

		final JButton btnClose = Static.createButton("Close");
		btnClose.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent arg0) {
	    		   Out.Print("Closing runDE chooser");
	    		   dispose();
	    	   }
		});
	       
       final JButton btnCloseAll = Static.createButton("Exit All");
       btnCloseAll.addActionListener(new ActionListener() {
	    	   public void actionPerformed(ActionEvent arg0) {
	    		   Out.Print("Exiting runDE chooser");
	    		   if(openWindows != null) {
	    			   boolean b=true;
	    			   for(int x=0; x<openWindows.size(); x++) {
	    				   if (b) {
	    					   openWindows.get(x).rQuit(); // this closes them all
	    					   b = false;
	    				   }
	    				   openWindows.get(x).dispose();
	    			   }
	    			   openWindows.removeAllElements();
	    		   }
	    		   
	    		   dispose();
	    		  
	    		   System.exit(0); 
	    	   }
       });      
       	    
       final JButton btnHelp = Static.createButtonHelp("Help", true);
       btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(getInstance(), "RunDE Help", helpHTML);
			}
       });
       JPanel buttonPanel = Static.createRowPanel();
       buttonPanel.add(btnViewDB);		buttonPanel.add(Box.createHorizontalStrut(5));
       buttonPanel.add(btnGetState);	buttonPanel.add(Box.createHorizontalStrut(5));
       buttonPanel.add(btnClose);		buttonPanel.add(Box.createHorizontalStrut(5));
       buttonPanel.add(btnCloseAll);	buttonPanel.add(Box.createHorizontalStrut(10));
       buttonPanel.add(btnHelp);
       
       buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
       buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
    
       
       dbTree.addTreeSelectionListener(new TreeSelectionListener() {
           public void valueChanged(TreeSelectionEvent e) {
               int depth = dbTree.getSelectionPath().getPathCount();
               btnViewDB.setEnabled(depth >= 3);
               btnGetState.setEnabled(depth >= 3);
           }
       });

       // Select current host in list
       if (start != null) {
           TreeNode[] nodes = dbModel.getPathToRoot(start.getFirstLeaf());
           TreePath path = new TreePath(nodes);
           dbTree.scrollPathToVisible(path);
           dbTree.setSelectionPath(path);
       }

       // Put it all together
       JPanel selectTCWdbPanel = Static.createPagePanel();
       selectTCWdbPanel.add(Box.createVerticalStrut(20));
       selectTCWdbPanel.add(Static.createLabel("singleTCW Databases"));
       
       selectTCWdbPanel.add(dbScrollPane);
       
       selectTCWdbPanel.add(Box.createVerticalStrut(5));
       selectTCWdbPanel.add(buttonPanel);
       
       selectTCWdbPanel.add(Box.createVerticalStrut(20));
       selectTCWdbPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
       selectTCWdbPanel.setPreferredSize(new Dimension(500, 500));
       
       return selectTCWdbPanel;
	}
	/************************************************************/
	private void dbOverview(String dbName, DBConn dbc) {
		try {
			String val = null;
			ResultSet rset = dbc.executeQuery("SELECT pja_msg, meta_msg FROM assem_msg");
			
			if(rset.first()) {
				val = rset.getString(1);
				if (val==null || (val != null && val.length() <= 10)) {
					Overview ov = new Overview(dbc);
					Vector <String> lines = new Vector <String> ();
					val = ov.createOverview(lines);
				}
				else val +=  rset.getString(2);
			}
			rset.close(); // do not close dbc
			
			UserPrompt.displayInfoMonoSpace(getInstance(), "Overview for " + dbName, 
					val.split("\n"), false, false); 
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting overview for " + dbName);}
	}
	private QRChooser getInstance() { return this; }
	
	private void setWindowSettings(final String prefix) {
		Static.centerScreen(this);
		pack();
	}
	
	public void windowClosed(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {}
	public void windowActivated(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	public void removeWindow(QRFrame q) {
		openWindows.remove(q);
	}
}
