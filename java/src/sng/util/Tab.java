package sng.util;

/**********************************************
 * Every JPanel that can be viewed by the left panel via MenuTree is a 'Tab'.
 * HiddenTabbedPanel keeps track of them.
 * They are objects for a MenuTreeNode that have an associate name that is shown on the left.
 */
import javax.swing.JPanel;

import sng.viewer.STCWFrame;

public class Tab extends JPanel {
	private static final long serialVersionUID = 1744893675782003263L;

	private static final boolean DEBUG = false;
	
	private static int nNextID = 0;
	
	private int nID; // unique ID, used as name in CardLayout
	private String strTitle; // for debug
	private STCWFrame parentFrame = null;
	private Tab parentTab = null; // tab that created this tab (but not necessarily the container getParent())
	private MenuTreeNode menuNode = null;
	
	public Tab(STCWFrame parentFrame, Tab parentTab) {
		nID = ++nNextID;
		if (DEBUG) System.err.println("Tab: "+nID+" "+ getClass().getName());
		this.parentFrame = parentFrame;
		this.parentTab = parentTab;
	}
	
	public int getID() { return nID; }
	public STCWFrame getParentFrame() { return parentFrame; }
	public Tab getParentTab() { return parentTab; }
	public String getTitle() { return strTitle; }
	public void setTitle(String strTitle) { this.strTitle = strTitle; }
	public MenuTreeNode getMenuNode() { return menuNode; }
	public void setMenuNode(MenuTreeNode newNode) { this.menuNode = newNode; }
	public void close() { } // implemented by subclass
}
