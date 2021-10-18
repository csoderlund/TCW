package sng.util;

/*********************************************************
 * Controls selectable labels on left panel
 */
import java.awt.LayoutManager;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.Component;
import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import java.util.Vector;
import java.util.Iterator;

import sng.util.MenuTreeNode.MenuTreeNodeEvent;
import sng.util.MenuTreeNode.MenuTreeNodeListener;

public class MenuTree extends JPanel implements MenuTreeNodeListener {
	private static final long serialVersionUID = 1L;
	private static boolean DEBUG = false; 
	
	private static int DEFAULT_LEVEL_INDENT = 20;
	private static int DEFAULT_RIGHT_MARGIN = 5;
	private static Color bgColorLeft = Color.white; 

	private MenuTreeNode rootNode = null;
	private MenuTreeNodeListener nodeListener = null;
	private MenuTreeNode lastSelectedNode = null;
	
	private ImageIcon arrowIcon = null, dotIcon = null, closeIcon = null, plusminusIcon = null;
	
	// called by STCWFrame.openAssembly to set root node
	public MenuTree(MenuTreeNode node) {
		rootNode = node;
		setLayout(new MenuTreeLayout(this));
		rootNode.setVisible(false);
		setOpaque(true);
		setBackground(bgColorLeft);

		// Get icons for various node types
		java.net.URL imgURL;
		
		imgURL = MenuTreeNode.class.getResource("/images/rarrow.gif");
		if (imgURL != null) arrowIcon = new ImageIcon(imgURL);
		
		imgURL = MenuTreeNode.class.getResource("/images/dot.gif");
		if (imgURL != null) dotIcon = new ImageIcon(imgURL);
		
		imgURL = MenuTreeNode.class.getResource("/images/close.gif");
		if (imgURL != null) closeIcon = new ImageIcon(imgURL);
		
		imgURL = MenuTreeNode.class.getResource("/images/plusminus.gif");
		if (imgURL != null) plusminusIcon = new ImageIcon(imgURL);
		
		installNodes();
	}
	
	private void installNodes() {
		removeAll();
		
		Vector<MenuTreeNode> v = new Vector<MenuTreeNode>();
		depthFirstList(v, rootNode);
		for (MenuTreeNode n : v) {
			n.setIcons(arrowIcon, dotIcon, closeIcon, plusminusIcon);
			super.add(n); 
			n.addNodeListener(this);
		}
	}
	
	public void addMenuTreeNodeListener(MenuTreeNodeListener l) {
		nodeListener = l;
	}
	public void addNode(Tab parentTab, MenuTreeNode childNode) {
		MenuTreeNode parentNode = getNodeWithUserObject(parentTab);
		addNode(parentNode, childNode);
	}
	public void addNode(String parentName, MenuTreeNode childNode) {
		MenuTreeNode parentNode = getNodeWithName(parentName);
		addNode(parentNode, childNode);
	}
	public void addNode(MenuTreeNode parentNode, MenuTreeNode childNode) {
		int index = parentNode.getChildCount();
		prtMsg("addNode1", "parent="+parentNode.getText()+" child="+childNode.getText() + " Tree=" + getComponentCount());
		
		boolean foundParent = false;
		for (int i = 0;  i < getComponentCount();  i++) {
			if (getComponent(i) == parentNode) {
				childNode.addNodeListener(this);
				parentNode.addChild(childNode, index);
							
				super.add(childNode, index); // add to panel
				revalidate(); 
				foundParent = true;
				break;
			}
		}
		if (!foundParent) {
			System.err.println("MenuTree.addNode: parent node not found, child=" + childNode.getText());
		}
		if (DEBUG) printTree();
	}
	
	public void removeNode(MenuTreeNode node) {
		if (node == null) return;
		
		MenuTreeNode parent = node.getParentNode();
		if (parent != null) {
			super.remove(node); // remove from panel
			parent.removeChild(node);
			
			revalidate();
			repaint();
		}
	}
	public void removeNodeFromPanel(MenuTreeNode node) {
		if (node == null) return;
		
		MenuTreeNode parent = node.getParentNode();
		if (parent != null) {
			super.remove(node); // remove from panel
			
			revalidate();
			repaint();
		}
	}
	
	public void setSelected(MenuTreeNode node) {
		if (lastSelectedNode != null)
			lastSelectedNode.setSelected(false);
		
		if (node != null && node.isSelectable()) {
			prtMsg("setSelected", "node="+node+" lastSelectedNode="+lastSelectedNode);
			lastSelectedNode = node;
			node.setSelected();
		}
	}
	
	public MenuTreeNode getLastSelected() { return lastSelectedNode; }
	
	public void setSelectedNode(String nodeName) {
		setSelected(getNodeWithName(nodeName));
	}
	
	public void eventOccurred(MenuTreeNodeEvent e) {
		prtMsg("eventOccurred", "type="+e.getType()+" node="+e.getNode());
		
		MenuTreeNode n = e.getNode();
		if (e.getType() == MenuTreeNodeEvent.TYPE_SELECTED && n.isSelectable()) 
		{
			if (lastSelectedNode != null)
				lastSelectedNode.setSelected(false);
			
			lastSelectedNode = n;
			lastSelectedNode.setSelected(true);
		}
		
		if (nodeListener != null)
			nodeListener.eventOccurred(e);
	}
	
	public MenuTreeNode getRootNode() {
		return rootNode;
	}
	
	public MenuTreeNode getNodeWithName(String nodeName) {
		Vector<MenuTreeNode> v = new Vector<MenuTreeNode>();
		depthFirstList(v, rootNode);
		Iterator<MenuTreeNode> iter = v.iterator();
		while (iter.hasNext()) {
			MenuTreeNode n = iter.next();
			if (n.toString().equals(nodeName)) return n;
		}
		return null;
	}

	public MenuTreeNode getNodeWithUserObject(Object obj) {
		Vector<MenuTreeNode> v = new Vector<MenuTreeNode>();
		depthFirstList(v, rootNode);
		Iterator<MenuTreeNode> iter = v.iterator();
		while (iter.hasNext()) {
			MenuTreeNode n = iter.next();
			if (n.getUserObject() == obj) return n;
		}
		prtMsg("no NodeWithUserObject ", obj.getClass().getName());
		return null;
	}
	private void depthFirstList(Vector<MenuTreeNode> v, MenuTreeNode n) {
		v.add(n);
		
		for (MenuTreeNode child : n.getChildNodes())
			depthFirstList(v, child);
	}
	private void prtMsg(String method, String msg) {
		if (DEBUG) 
			System.err.format("MenuTree %-20s %s\n", method, msg);
	}
	
	private void printTree() {
		Vector<MenuTreeNode> v = new Vector<MenuTreeNode>();
		depthFirstList(v, rootNode);
		Iterator<MenuTreeNode> iter = v.iterator();
		int i=1;
		while (iter.hasNext()) {
			MenuTreeNode n = iter.next();
			System.err.println(i + " " + n.toString() + " " + n.getText());
			i++;
		}
	}
	
	/*
	 * MenuTreeLayout
	 */
	private class MenuTreeLayout implements LayoutManager {
		private MenuTree menuTree;
		
		public MenuTreeLayout(MenuTree menuTree) {
			this.menuTree = menuTree;
		}
		public void addLayoutComponent(String name, Component comp) { }
		
		public void removeLayoutComponent(Component comp) { }
		
		private Dimension doLayout(MenuTreeNode n, int x, int y) {	
			int x2 = x;
			int y2 = y;
			
			if (n.isVisible()) { 
				Dimension dim = n.getPreferredSize();
				int height = (int)dim.getHeight();
				
				switch (n.getLevel()) {
				case 1:
					n.setType(MenuTreeNode.TYPE_COLLAPSIBLE);
					n.setStyle(MenuTreeNode.STYLE_LEVEL1);
					break;					
				case 2:
					n.setType(MenuTreeNode.TYPE_COLLAPSIBLE);
					n.setStyle(MenuTreeNode.STYLE_LEVEL2);
					break;
				case 3:
					n.setStyle(MenuTreeNode.STYLE_LEVEL3);
					break;
				default:
					n.setStyle(MenuTreeNode.STYLE_LEVEL4);
				}
					
				n.setSize(dim);
				x2 += DEFAULT_RIGHT_MARGIN;
				n.setLocation(x2,y2);
				
				y2 += height;
				x2 += DEFAULT_LEVEL_INDENT;
			}
			for (MenuTreeNode child : n.getChildNodes()) {
				Dimension dim = doLayout(child, x2, y2);
				y2 += dim.getHeight();
			}	
			return new Dimension(x2-x, y2-y);
		}
		public void layoutContainer(Container parent) {
			doLayout(menuTree.rootNode, 0, 0);
		}
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}
		public Dimension preferredLayoutSize(Container parent) {
			return doLayout(menuTree.rootNode, 0, 0);
		}
	}
}
