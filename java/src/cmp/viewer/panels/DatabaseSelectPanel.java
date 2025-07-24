package cmp.viewer.panels;

/************************************************
 * Displays the sTCW database tree. It's also used for browsing the mTCW databases, depending whether has a parent or not. 
 */
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.UserPrompt;
import cmp.compile.runMTCWMain;
import cmp.compile.Summary;
import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.EditSpeciesPanel;
import cmp.viewer.MTCWFrame;

public class DatabaseSelectPanel extends JPanel {
	private static final long serialVersionUID = 3004816148155062955L;
	
	private static final int TREE_PANEL_WIDTH = 350;
	private static final int TREE_PANEL_HEIGHT = 300;
    
	public DatabaseSelectPanel(String prefix, EditSpeciesPanel parentPanel) {
		HostsCfg hostcfg = new HostsCfg();
		Vector<String> names = CompilePanel.dbListFromHost(hostcfg.host(), 
		        hostcfg.user(), hostcfg.pass(), prefix);
		allDBs = new Vector<DatabaseData> ();
		for (String db : names)
		{
			allDBs.add(new DatabaseData(hostcfg.host(), db));
		}
		
		theEditSpeciesPanel = parentPanel;
	
		setBackground(Static.BGCOLOR);
		add(createTreePanel());
	}
	
	//Tree that contains all available assemblies
	private JPanel createTreePanel() {
		JPanel thePanel = Static.createPagePanel();
		thePanel.setMinimumSize(new Dimension(TREE_PANEL_WIDTH, TREE_PANEL_HEIGHT));
		if(allDBs.size() == 0) return thePanel;
		
		//Create the tree
		Iterator<DatabaseData> iter = allDBs.iterator();
		DatabaseData temp = iter.next();

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Available Databases");
		DefaultMutableTreeNode header = new DefaultMutableTreeNode(temp.getHost());

		boolean done = false;
		while(!done) {
			if(temp.getHost().equals(header.getUserObject())) {
				header.add(new DefaultMutableTreeNode(temp.getDBName()));
				if(iter.hasNext()) temp = iter.next();
				else done = true;
			}
			else {
				rootNode.add(header);
				header = new DefaultMutableTreeNode(temp.getHost());
			}
		}
		rootNode.add(header);
		theTree = new JTree(rootNode);

		theTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		theTree.setToggleClickCount(1);
		((DefaultTreeCellRenderer) theTree.getCellRenderer()).setLeafIcon(null);
		
		if (theTree.getRowCount() == 2) theTree.expandRow(1);

		theTree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TreePath path = theTree.getSelectionPath();
				if (path != null) {
					int depth = path.getPathCount();
					if (e.getClickCount() == 2 && depth > 2) {
						keepClicked();
						return;
					}
					btnKeep.setEnabled(depth > 2);
					btnGetState.setEnabled(depth > 2);
				}
                            //update add and remove buttons
			}
		});

		JScrollPane treeView = new JScrollPane(theTree);
		treeView.setPreferredSize(thePanel.getMinimumSize());
		thePanel.add(treeView);
		
		//Notify user of the number of available assemblies
		txtNumAssemblies = new JTextField(1);
		Dimension d = treeView.getPreferredSize();
		Dimension d2 = txtNumAssemblies.getPreferredSize();
		txtNumAssemblies.setPreferredSize(new Dimension(d.width, d2.height));
		txtNumAssemblies.setEditable(false);
		txtNumAssemblies.setHorizontalAlignment(JTextField.CENTER);
		txtNumAssemblies.setBorder(BorderFactory.createEmptyBorder());
		txtNumAssemblies.setBackground(Static.BGCOLOR);
		thePanel.add(Box.createVerticalStrut(5));
		thePanel.add(txtNumAssemblies);
		thePanel.add(Box.createVerticalStrut(10));
		txtNumAssemblies.setText("Databases available: " + allDBs.size());
                
		
		
		JPanel buttonPanel = Static.createPagePanel();
		JPanel row = Static.createRowCenterPanel();
		
		
		if (theEditSpeciesPanel == null) btnKeep = Static.createButtonPopup("Launch", false);
		else 							 btnKeep = Static.createButton("Keep", false);;
		btnKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				keepClicked();
			}
		});
		row.add(btnKeep);
		
		btnGetState = Static.createButtonPopup("Overview", false);
		btnGetState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(theEditSpeciesPanel == null)
					showMultiTCW();
				else 
					showSingleTCW();
			}
		});
		row.add(Box.createHorizontalStrut(10));
		row.add(btnGetState);
		row.setMaximumSize(row.getPreferredSize());
		row.setMinimumSize(row.getPreferredSize());

		if(theEditSpeciesPanel != null) {
			btnDiscard = Static.createButton("Discard");
			btnDiscard.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					theEditSpeciesPanel.setMainPanelVisible(true);
					setVisible(false);
				}
			});
			row.add(Box.createHorizontalStrut(10));
			row.add(btnDiscard);
		}			
		else {
			// TODO If a Launch and Overview have occurred, and the
			// Launch was exited, then it does not go back to prompt
			// because it thinks the Overview is still open
			btnClose = Static.createButton("Exit");
			btnClose.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					//Position 0 is the select panel
					MTCWFrame.closeFrame(0);		
					if (MTCWFrame.getFrameCount() == 0)
						System.exit(0);
				}
			});
			
			row.add(Box.createHorizontalStrut(10));
			row.add(btnClose);
			
			btnCloseAll = Static.createButton("Exit all");
			btnCloseAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					MTCWFrame.closeAllFrames();
					System.exit(0); 
				}
			});
			row.add(Box.createHorizontalStrut(10));
			row.add(btnCloseAll);
		}
					
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(row);
		buttonPanel.add(Box.createHorizontalGlue());
		
		thePanel.add(buttonPanel);
		return thePanel;
	}
	private void showMultiTCW() {
		try {
			String val = null;
			HostsCfg hosts = new HostsCfg();
			DatabaseData data = getSelection();
			
			if(!DBConn.checkMysqlDB("Show MultiTCW", hosts.host(), data.getDBName(), hosts.user(), hosts.pass()))
				val = "No database available";
			else {
				DBConn conn = new DBConn(hosts.host(), data.getDBName(), hosts.user(), hosts.pass());
				val = new Summary(conn).getSummary();
			}
			UserPrompt.displayInfoMonoSpace(null, "Overview for " + data.getDBName(), val.split("\n"), false, true);
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting project state");
		}
	}
	private void showSingleTCW() {
		try {
			HostsCfg hosts = new HostsCfg();
			DatabaseData data = getSelection();
			if (data==null || data.getDBName()==null) {
				System.out.println("Error - no selected database");
				return;
			}
			String dbname = data.getDBName();
			String val = "No Overview for " + dbname + "\nRun viewSingleTCW to update it\n";
			
			if(!DBConn.checkMysqlDB("Show State DBs ", hosts.host(), dbname, 
					hosts.user(), hosts.pass())) {
				val = "No database available\n";
			}
			else {
				DBConn conn = new DBConn(hosts.host(), dbname, hosts.user(), hosts.pass());
				ResultSet rset = conn.executeQuery("SELECT pja_msg FROM assem_msg where AID=1");
				
				if(rset.next()) { // CAS405 was first
					val = rset.getString(1);
					if (val != null && val.length() <= 5) {
						val = "Bad Overview for " + dbname + "\nRun viewSingleTCW with -o to update it\n";
					}
				}
				rset.close();
				conn.close();
			}
			if (val==null || val.equals("")) val = "No Overview for " + dbname + "\nRun viewSingleTCW to update it\n";
			UserPrompt.displayInfoMonoSpace(null, "Overview for " + dbname, val.split("\n"), false, true);
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting project state");}
	}

	private void keepClicked() {
		if(theEditSpeciesPanel == null)
			launchSelection();
		else {
			DatabaseData data = getSelection();
			String dbName = data.getDBName();
			String prefix = "", stcwid="";
			try {
				DBConn sDB = runMTCWMain.getDBConnection(data.getHost(), data.getDBName());
				
				boolean bIsAA = false;
				if (sDB.tableColumnExists("assem_msg", "peptide")) bIsAA=true;

				ResultSet rs = sDB.executeQuery("SELECT assemblyid FROM assembly");
				if(rs.next()) {
					stcwid = rs.getString(1);
					prefix = stcwid;
					if (stcwid.length()>3) prefix = stcwid.substring(0,3);
					else if (stcwid.length()==0) {
						System.err.println("WARN: no ID from database " + dbName);
						JOptionPane.showMessageDialog(null, "No ID from database " + dbName 
								+ "It may not be complete. " +
								"\nYou must beable to view it in viewSingleTCW to use it here.", 
								"Warn", JOptionPane.PLAIN_MESSAGE);
						if (dbName.length()>3) prefix = dbName.substring(0,3);
						else prefix = dbName;
					}
				}
				else {
					System.err.println("ERROR: no ID from database " + dbName);
					System.err.println("   Did you Instantiate? That is necessary.");
					JOptionPane.showMessageDialog(null, "No ID from database " + dbName 
							+ "\nYou must 'Instantiate' in runSingleTCW. ", 
							"Error", JOptionPane.PLAIN_MESSAGE);
				}
				if (!bIsAA) {
					String orfs = sDB.executeString("select orf_msg from assem_msg");
					if (orfs==null || orfs=="" || orfs.length()<10) {
						String msg = "The sTCW database has not been annotated or is incorrectly annotated" +
								"\nYou must at least execute 'ORF only'";
						JOptionPane.showMessageDialog(null, "No annotation in database " + dbName + "\n"
								+ msg, "Error", JOptionPane.PLAIN_MESSAGE);
						System.err.println(msg);
					}
				}
				theEditSpeciesPanel.setFromDBselect(dbName, stcwid, prefix, bIsAA);
			}
			catch(Exception e) {
				ErrorReport.prtReport(e, "Error validating database");
				JOptionPane.showMessageDialog(null, "Error validating" + dbName 
						+ "\nYou must be able to view it in viewSingleTCW to use it here", 
						"Error", JOptionPane.PLAIN_MESSAGE);
			}
			theEditSpeciesPanel.setMainPanelVisible(true);
			setVisible(false);
		}
	}

	private DatabaseData getSelection() {
		DatabaseData retVal = null;
        TreePath path = theTree.getSelectionPath();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)node.getParent();
        
        Iterator<DatabaseData> iter = allDBs.iterator();
        while(iter.hasNext() && retVal == null) {
                DatabaseData temp = iter.next();
                if(temp.getDBName().equals(node.getUserObject()) && temp.getHost().equals(parentNode.getUserObject()))
                        retVal = temp;
        }
        return retVal;
	}
	private void launchSelection() {
        DatabaseData data = getSelection();
        MTCWFrame theFrame = new MTCWFrame(data.getHost(), data.getDBName(), null, null);
        theFrame.setVisible(true);
	}
	
	private class DatabaseData {
		public DatabaseData(String host, String name) {
			strHost = host;
			strDBName = name;
		}
		public String getHost() { return strHost; }
		public String getDBName() { return strDBName; }
		
		private String strHost = "";
		private String strDBName = "";
	}

	//Data vars
	private Vector<DatabaseData> allDBs = null;
	private EditSpeciesPanel theEditSpeciesPanel = null;
	private JTree theTree = null;
	private JTextField txtNumAssemblies = null;
	private JButton btnKeep = null;
	private JButton btnDiscard = null;
	private JButton btnGetState = null;
	private JButton btnClose = null;
	private JButton btnCloseAll = null;
}