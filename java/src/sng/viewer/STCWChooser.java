package sng.viewer;

/*************************************************
 * Single TCW chooser
 * 	also put a few other things in here from STCWFrame to reduce content in it.
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
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
import sng.util.HiddenTabbedPane;
import sng.util.MenuTree;
import util.database.HostsCfg;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.UserPrompt;
import util.database.Globalx;

public class STCWChooser extends JFrame {
	private static final long serialVersionUID = 6434887265792656100L;
	private static final String title = "viewSingleTCW";
	
	/**
	 *  Open the Single TCW Database List Window
	 */
	public STCWChooser(HostsCfg hosts, Vector <DBInfo> dbList) {
		STCWFrame.nFrames=1;
		hostsObj = hosts;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				STCWFrame.nFrames--;
				if (STCWFrame.nFrames == 0) System.exit(0);
			}
		});
				
		JPanel selectPanel = createTCWdbSelectionPanel(dbList);
		
		try {setTitle(title + " on " + InetAddress.getLocalHost().getHostName());}
		catch (Exception e){ setTitle(title);}
		
		setResizable(true);
		add(selectPanel);
		Static.centerScreen(this);
		pack();
		setVisible(true);
	}
	/******************************************************
	 * TCWdb chooser methods
	 * HOSTs.cfg only allows one host specified
	 * this could be expanded to allow multiple, as it use to
	 */
	private JPanel createTCWdbSelectionPanel(Vector <DBInfo> dbList) 
	{
	       JPanel selectTCWdbPanel = new JPanel();
	       selectTCWdbPanel.setBackground(Color.WHITE);

	       // Create a tree of the hosts
	       DefaultMutableTreeNode hostTree = new DefaultMutableTreeNode("");
	       DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(hostsObj.host());
		   hostTree.add(newNode);
		   DefaultMutableTreeNode start = newNode;

	       for (int i = 0; i < hostTree.getChildCount(); i++) {
	           DefaultMutableTreeNode hostNode = (DefaultMutableTreeNode) hostTree.getChildAt(i);
	           
               for (DBInfo dbObj : dbList) {
            	   	  if (!dbObj.getID().equals(Globalx.error))
            	   		  hostNode.add(new DefaultMutableTreeNode(dbObj));
               } 
	       }

	       // Create display tree
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
		                       DBInfo dbObj = (DBInfo) node.getUserObject();
				               new STCWFrame(hostsObj, dbObj);
		                   }
		               }
	        	   }
	        	   catch(Exception ex) {ErrorReport.prtReport(ex, "Error launching singleTCW window");}
	           }
	       });

	       final JScrollPane dbScrollPane = new JScrollPane(dbTree);
	       dbScrollPane.setPreferredSize(new Dimension(400, 400));
	       Dimension dim = dbScrollPane.getMaximumSize();
	       dbScrollPane.setMaximumSize(new Dimension(Math.max(400,
	               (int) dim.getWidth()), Math.max(400, (int) dim.getHeight())));
	       dbScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

	       final JButton btnViewdb = new JButton("Launch");
	       btnViewdb.setAlignmentX(Component.CENTER_ALIGNMENT);
	       btnViewdb.setBackground(Globalx.LAUNCHCOLOR);
	       btnViewdb.setEnabled(false);

	       btnViewdb.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
	               DefaultMutableTreeNode node = (DefaultMutableTreeNode)
	                   dbTree.getLastSelectedPathComponent();
	               DBInfo dbObj = (DBInfo) node.getUserObject();
	               new STCWFrame(hostsObj, dbObj);
	        	   }
	        	   catch(Exception e) {ErrorReport.prtReport(e, "Error launching viewSingleTCW window");}
	           }
	       });
	       
			final JButton btnGetState = new JButton("Overview");
			btnGetState.setBackground(Globalx.LAUNCHCOLOR);
			btnGetState.setAlignmentX(Component.LEFT_ALIGNMENT);
			btnGetState.setEnabled(false);
			btnGetState.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						String val = "Error getting overview";
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) dbTree.getLastSelectedPathComponent();
						DBInfo dbObj = (DBInfo) node.getUserObject();
	                    String dbName = dbObj.getdbName();
						
						setConnection(dbName);
						ResultSet rset = mDB.executeQuery("SELECT pja_msg, meta_msg FROM assem_msg");
						if(rset.first()) {
							val = rset.getString(1);
							if (val==null || (val != null && val.length() <= 10)) {
								Overview ov = new Overview(mDB);
								Vector <String> lines = new Vector <String> ();
								val = ov.createOverview(lines);
							}
							else val +=  rset.getString(2);
						}
						rset.close();
						
						UserPrompt.displayInfoMonoSpace(getInstance(), "Overview for " + dbName, 
								val.split("\n"), false, false); 
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Error getting project state");}
				}
			});

			final JButton btnClose = new JButton("Exit");
		       btnClose.setBackground(Color.WHITE);
		       btnClose.addActionListener(new ActionListener() {
		    	   public void actionPerformed(ActionEvent arg0) {
		    		   STCWFrame.nFrames--;
		    		   dispose();
		    	   }
		       });
		       
	       final JButton btnCloseAll = new JButton("Exit All");
	       btnCloseAll.setBackground(Color.WHITE);
	       btnCloseAll.addActionListener(new ActionListener() {
		    	   public void actionPerformed(ActionEvent arg0) {
		    		   dispose();
		    		   System.exit(1);
		    	   }
	       });      
	       	       
	       dbTree.addTreeSelectionListener(new TreeSelectionListener() {
	           public void valueChanged(TreeSelectionEvent e) {
	               int depth = dbTree.getSelectionPath().getPathCount();
	               btnViewdb.setEnabled(depth >= 3);
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

	       JPanel buttonPanel = new JPanel();
	       buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
	       buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	       buttonPanel.setBackground(Color.WHITE);
	       buttonPanel.add(btnViewdb); buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnGetState);     buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnClose);        buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnCloseAll);
	       
	       buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
	       buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
	       
	       selectTCWdbPanel.setLayout(new BoxLayout(selectTCWdbPanel,BoxLayout.Y_AXIS));
	       selectTCWdbPanel.add(Box.createVerticalStrut(20));
	       
	       selectTCWdbPanel.add(Static.createCenteredLabel("singleTCW Databases"));
	       selectTCWdbPanel.add(dbScrollPane);
	       selectTCWdbPanel.add(Box.createVerticalStrut(5));
	       selectTCWdbPanel.add(buttonPanel);
	       selectTCWdbPanel.add(Box.createVerticalStrut(20));
	       selectTCWdbPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	       return selectTCWdbPanel;
	}
	
	private void setConnection(String dbName) {
		setTitle(title + " " + STCWMain.TCW_VERSION + ":   " + dbName);
		System.err.println(title + " " + dbName);

		try {
			mDB = hostsObj.getDBConn(dbName);		
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting databse information for " + dbName);
		}
	}
	private STCWChooser getInstance() { return this; }
	
	
	/*************************************************************
	 * XXX set UI Defaults 
	 */
	static private final Font boldUIFont = Globalx.boldUIFont;
	static private final Font textFont = Globalx.textFont;
	static private final Color componentBGColor = Globalx.componentBGColor;
	static private final Color selectColor = Globalx.selectColor;
	
	static public void setupUIDefaults() {
		UIManager.put("CheckBox.font", boldUIFont);
		UIManager.put("Panel.font", boldUIFont);
		UIManager.put("Label.font", boldUIFont);
		UIManager.put("Button.font", boldUIFont);
		UIManager.put("Menu.font", boldUIFont);
		UIManager.put("MenuItem.font", boldUIFont);
		UIManager.put("RadioButtonMenuItem.font", boldUIFont);
		UIManager.put("ComboBox.font", boldUIFont);
		UIManager.put("RadioButton.font", boldUIFont);
		UIManager.put("TabbedPane.font", boldUIFont);

		UIManager.put("TextField.font", textFont);
		UIManager.put("TextArea.font", textFont);
		UIManager.put("List.font", textFont);

		UIManager.put("ScrollBar.background", componentBGColor);
		UIManager.put("ScrollBar.track", componentBGColor);
		UIManager.put("CheckBox.background", componentBGColor);
		UIManager.put("Panel.background", componentBGColor);
		UIManager.put("TabbedPane.background", componentBGColor);
		UIManager.put("TabbedPane.tabAreaBackground", componentBGColor);
		UIManager.put("TabbedPane.selected", componentBGColor);
		UIManager.put("TabbedPane.highlight", componentBGColor);
		UIManager.put("Label.background", componentBGColor);
		UIManager.put("Button.background", componentBGColor);
		UIManager.put("Menu.background", componentBGColor);
		UIManager.put("MenuItem.background", componentBGColor);
		UIManager.put("RadioButtonMenuItem.background", componentBGColor);
		UIManager.put("ComboMenuItem.background", componentBGColor);
		UIManager.put("ComboBox.background", componentBGColor);
		UIManager.put("ComboBox.disabledBackground", componentBGColor);
		UIManager.put("ComboBox.listBackground", componentBGColor);
		UIManager.put("RadioButton.background", componentBGColor);
		UIManager.put("Viewport.background", componentBGColor);
		UIManager
				.put("InternalFrame.inactiveTitleBackground", componentBGColor);
		UIManager.put("ScrollPane.background", componentBGColor);
		UIManager.put("TableHeader.background", componentBGColor);

		UIManager.put("control", componentBGColor);
		UIManager.put("inactiveCaption", componentBGColor);
		UIManager.put("menu", componentBGColor);
		UIManager.put("scrollbar", componentBGColor);
		UIManager.put("windowBorder", componentBGColor);

		UIManager.put("List.selectionBackground", selectColor);
		UIManager.put("TextField.selectionBackground", selectColor);
		UIManager.put("TextArea.selectionBackground", selectColor);
		UIManager.put("ScrollBar.thumb", selectColor);

		UIManager.put("Button.focus", selectColor);
		UIManager.put("RadioButton.focus", selectColor);
		UIManager.put("RadioButton.focus", selectColor);
		UIManager.put("ComboBox.selectionBackground", selectColor);
		UIManager.put("CheckBoxMenuItem.selectionBackground", selectColor);
		UIManager.put("Menu.selectionBackground", selectColor);
		UIManager.put("MenuItem.selectionBackground", selectColor);
		UIManager.put("RadioButtonMenuItem.selectionBackground", selectColor);
		UIManager.put("RadioButton.focus", selectColor);
	}
	
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	public JSplitPane splitPane;
	public MenuTree menuTree;
	public HiddenTabbedPane tabbedPane;

	private DBConn mDB;	
	private HostsCfg hostsObj = null;
}
