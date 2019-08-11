package sng.util;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class MenuTreeNode extends JPanel implements ActionListener {
	private static final long serialVersionUID = -8445121683849128470L;

	private static final boolean DEBUG = false;
	
	public final static int TYPE_DEFAULT 	 = 0;
	public final static int TYPE_COLLAPSIBLE = 1;
	public final static int TYPE_CLOSEABLE 	 = 2;
	
	public final static int STYLE_LEVEL1 = 1;
	public final static int STYLE_LEVEL2 = 2;
	public final static int STYLE_LEVEL3 = 3;
	public final static int STYLE_LEVEL4 = 4;
	
	public final static Color bgColorLeft = Color.white;
	
	private MenuTreeNode parent;
	private Vector<MenuTreeNode> children;
	private Object userObject = null;
	private JButton nodeButton = null;
	private JButton actionButton = null;
	private int type = TYPE_COLLAPSIBLE;
	private MenuTreeNodeListener nodeListener = null;
	private boolean visible = true;
	private boolean collapsed = false;
	private boolean selectable = true;
	
	private ImageIcon arrowIcon = null;
	private ImageIcon dotIcon = null;
	private ImageIcon closeIcon = null;
	private ImageIcon plusminusIcon = null;
	
	// called by STCWFrame.openAssembly to create root node that is added to MenuTree
	public MenuTreeNode() {
		parent = null;
		children = new Vector<MenuTreeNode>();
		
		setBackground(bgColorLeft);
		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		
		nodeButton = new JButton(" ");
		nodeButton.setBorderPainted(false);
		nodeButton.setFocusPainted(false);
		nodeButton.setContentAreaFilled(false);
		nodeButton.setMargin(new Insets(0, 0, 0, 0));
		nodeButton.setVerticalAlignment(AbstractButton.TOP);
		nodeButton.setHorizontalAlignment(AbstractButton.LEFT);
		nodeButton.addActionListener(this);

		actionButton = new JButton();
		actionButton.setBorderPainted(false);
		actionButton.setFocusPainted(false);
		actionButton.setContentAreaFilled(false);
		
		actionButton.setVerticalAlignment(AbstractButton.TOP);
		actionButton.setHorizontalAlignment(AbstractButton.LEFT);
		actionButton.setMargin(new Insets(0, 0, 0, 0));
		actionButton.addActionListener(this);
		actionButton.setVisible(false);
		actionButton.setBorder(BorderFactory.createLineBorder(Color.green));
		
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		
		add(nodeButton);
		add(actionButton);
	}
	
	public MenuTreeNode(String name) {
		this();
		nodeButton.setText(name);
		prtMsg("Create ", name);
	}
	
	public MenuTreeNode(String name, Object userObj) {
		this(name);
		this.userObject = userObj;
		prtMsg("Create ", name + " " + userObj.getClass().getName());
	}
	
	public void setIcons(	ImageIcon arrowIcon, ImageIcon dotIcon, 
							ImageIcon closeIcon, ImageIcon plusminusIcon)
	{
		this.arrowIcon = arrowIcon;
		this.dotIcon = dotIcon;
		this.closeIcon = closeIcon;
		this.plusminusIcon = plusminusIcon;
	}
	
	public void actionPerformed(ActionEvent e) {
		prtMsg("actionPerformed ", getText());

		int eventType = MenuTreeNodeEvent.TYPE_SELECTED;
		
		if (e.getSource() == actionButton) {
			if (type == TYPE_COLLAPSIBLE) {
				if (isChildCollapsed()) expandChildren();
				else collapseChildren();
				return;
			}
			else if (type == TYPE_CLOSEABLE)
				eventType = MenuTreeNodeEvent.TYPE_CLOSED;
		}
		
		if (nodeListener != null)
			nodeListener.eventOccurred(new MenuTreeNodeEvent(eventType, MenuTreeNode.this));
	}
	
	public void addChild(MenuTreeNode newChild, int index) {
		if (newChild != null) {
			prtMsg("addChild ", index + " child="+newChild);
			newChild.setParent(this);
			newChild.setIcons(arrowIcon, dotIcon, closeIcon, plusminusIcon);
			children.add(index, newChild);
		}
	}
	
	public void addChild(MenuTreeNode newChild) { 
		addChild(newChild, getChildCount()); 
	}
	public void addNodeListener(MenuTreeNodeListener l) { nodeListener = l; }
	public void removeChild(MenuTreeNode theChild) { children.remove(theChild); }
	
	public void setUserObject(Object obj) { 
		prtMsg("setUserObject ", obj.getClass().getName());
		userObject = obj; 
	}
	public Object getUserObject() { return userObject; }
	public int getChildCount() { return children.size(); }
	public boolean hasChildren() { return (getChildCount() > 0); }
	public MenuTreeNode getChildAt(int index) { return children.get(index); }
	
	public void showNode() {
		setVisible(true);
		showChildren();
	}
	
	public void hideNode() {
		setVisible(false);
		hideChildren();
	}
	
	public void hideChildren() {
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext())
			iter.next().hideNode();
	}
	
	private void showChildren() {
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext())
			iter.next().showNode();
	}
	
	private void collapseNode() {
		collapsed = true;
		setVisible(visible);
		collapseChildren();
	}
	
	private void expandNode(boolean expandChildren) {
		collapsed = false;
		setVisible(visible);
		if(expandChildren) expandChildren();
	}
	
	public void collapseChildren() {
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext())
			iter.next().collapseNode();
	}
	
	public void expandChildren() {
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext())
		{
			MenuTreeNode temp = iter.next();
			if(type == TYPE_COLLAPSIBLE)
				if(getLevel()==1)
					temp.expandNode(false);
				else
					temp.expandNode(true);					
		}
	}
	
	private boolean isChildCollapsed() {
		boolean childCollapsed = false;
		
		Iterator<MenuTreeNode> iter = children.iterator();
		while (iter.hasNext())
			childCollapsed |= iter.next().collapsed;
		
		return childCollapsed;
	}
	
	public boolean isSelectable() { return selectable; }
	private void setParent(MenuTreeNode newParent) { parent = newParent; }
	
	public void setSelected(boolean selected) {
		prtMsg("setSelected: ", getText());
		
		if (selected && selectable)		
			setBorder(BorderFactory.createLineBorder(Color.GRAY));
		else
			setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	}
	
	public void setSelected() {
		if (selectable) {
			setVisible(true);
			nodeButton.doClick();
		}
	}
	
	public MenuTreeNode getParentNode() { return parent; }
	public Vector<MenuTreeNode> getChildNodes() { return children; }
	
	public void setVisible(boolean aFlag) {
		visible = aFlag;
		super.setVisible(visible && !collapsed);
	}
	
	public String getText() { return nodeButton.getText(); }
	public void setText(String text) { nodeButton.setText(text); }
	
	public void setToolTipText(String text) {
		nodeButton.setToolTipText(text);
	}
	
	public void setType(int type) { // must be called AFTER node added to parent in order to pick up icon values from pare
		this.type = type;
		switch (type) {
		case TYPE_COLLAPSIBLE : 
			if (children.size() > 0) {
				actionButton.setIcon(plusminusIcon);
				actionButton.setVisible(true);
			}
			else 
				actionButton.setVisible(false);
			actionButton.setToolTipText("Collapse/Expand");
			break;
		case TYPE_CLOSEABLE :
			actionButton.setIcon(closeIcon);
			actionButton.setVisible(true);
			actionButton.setToolTipText("Close");
			break;
		default :
			actionButton.setVisible(false);
		}
	}
	
	public void setStyle(int style) {
		Font font = nodeButton.getFont();
		switch (style) {
		case STYLE_LEVEL1:
			selectable = false;
			break;
		case STYLE_LEVEL2:
			nodeButton.setIcon(arrowIcon);
			selectable = true;
			break;
		case STYLE_LEVEL3:
			nodeButton.setIcon(dotIcon);
			nodeButton.setFont(new Font(font.getName(),Font.PLAIN,font.getSize()));
			selectable = true;
			break;
		case STYLE_LEVEL4:
			nodeButton.setIcon(dotIcon);
			nodeButton.setFont(new Font(font.getName(),Font.PLAIN,font.getSize()));
			selectable = true;
			//nodeButton.setFont(new Font(font.getName(),Font.PLAIN,10));
			//selectable = false;
			break;
		}
	}
	
	protected int getLevel() {
		MenuTreeNode nextParent = parent;
		int level = 0;
				
		while (nextParent != null) {
			nextParent = nextParent.parent;
			level++;
		}
		return level;
	}
	
	public String toString() {
		return nodeButton.getText();
	}
	
	private void prtMsg(String method, String msg) {
		if (DEBUG)
			System.err.format("MenuTreeNode %-20s %s\n", method, msg);
	}
	public class MenuTreeNodeEvent {
		public static final int TYPE_NONE					= 0;
		public static final int TYPE_SELECTED 				= 1;
		public static final int TYPE_CLOSED   				= 2;
		public static final int TYPE_EXPANDED 				= 3;
		public static final int TYPE_COLLAPSED 				= 4;
		public static final int TYPE_EXPANDED_OR_COLLAPSED  = 5;
		
		private int eventType;
		private MenuTreeNode sourceNode;
		
		public MenuTreeNodeEvent(int type, MenuTreeNode node) {
			this.eventType = type;
			this.sourceNode = node;
		}
		public int getType() { return eventType; }
		public MenuTreeNode getNode() { return sourceNode; }
	}
	
	public interface MenuTreeNodeListener extends EventListener {
		void eventOccurred(MenuTreeNodeEvent e);
	}
}
