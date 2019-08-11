package cmp.viewer.panels;

/****************************************************
 * Create tabs on left of main viewMultiTCW panel
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import cmp.viewer.MTCWFrame;
import util.methods.Static;

public class MenuPanel extends JPanel {
	private static final long serialVersionUID = 7029184547060500871L;
	private static final int TABSIZE = 10;
	private static final String [] mainSections = MTCWFrame.MAIN_SECTIONS;
	
	public MenuPanel(MTCWFrame parentFrame, ActionListener select, ActionListener close) {
		theViewerFrame = parentFrame;
		theSelectionListener = select;
		theCloseListener = close;
		
		theClickListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(e.getSource() instanceof JButton) {
					setSelectedByClick((JButton)e.getSource());
				}
			}
		};
		root = new MenuItem(null, null, "", "", null, null);
	}
	public void addTopItem(JPanel rowPanel, String name, String description) {
		root.child = addChild(root.child, 
				new MenuItem(null, rowPanel, name, description, theSelectionListener, null));
	}
	public void addMenuItem(JPanel rowPanel, JPanel refPanel, String name, String description) {
		root.child = addChild(root.child, 
				new MenuItem(rowPanel, refPanel, name, description, theSelectionListener, null));
	}
	public void addChildItem(JPanel refParentPanel, JPanel refPanel, String name, String description) {
		root.child = addChild(root.child, 
				new MenuItem(refParentPanel, refPanel, name, description, theSelectionListener, theCloseListener));
		setSelected(refPanel);
	}
	public void setSelected(JPanel refPanel) {
		setSelectedChild(root.child, refPanel);
		refresh();
	}
	public JPanel getSelectedPanel() {
		return getSelectedChild(root.child);
	}
	public JPanel getMenuItem(JButton closeButton) {
		return getMenuItem(root.child, closeButton);
	}
	public void removeMenuItem(JPanel refPanel) {
		root.child = removeMenuItem(root.child, refPanel);
		refresh();
	}
	public boolean doesNodeLabelBeginWith(String prefix) {
		return doesNodeLabelBeginWith(root.child, prefix);
	}

	public void renameMenuItem(JPanel refPanel, String newString) {
		 renameMenuItem(root.child, refPanel, newString);
	}
	/*****************************************************/
	private void setSelectedByClick(JButton refClick) {
		if (sectionNode(refClick.getText())) return;
		
		setSelectedChildByClick(root.child, refClick);
	}
	private boolean sectionNode(String name) {
		for (int i=0; i<mainSections.length; i++)
			if (mainSections[i].equals(name)) return true;
		return false;
	}
	// makes all panels invisible except the selected one 
	private static void setSelectedChildByClick(MenuItem rootNode, JButton refButton) {
		if (rootNode == null) return;
		if (refButton==null) return;
		
		if (refButton.equals(rootNode.btnItem)) {	
			rootNode.tabPanel.setVisible(true);
			rootNode.btnItem.setFont(
					new Font(rootNode.btnItem.getFont().getName(), Font.BOLD, rootNode.btnItem.getFont().getSize()));
		}
		else if (rootNode.btnItem != null) {
			rootNode.tabPanel.setVisible(false);
			rootNode.btnItem.setFont(
					new Font(rootNode.btnItem.getFont().getName(), Font.PLAIN, rootNode.btnItem.getFont().getSize()));
		}
		setSelectedChildByClick(rootNode.child, refButton);
		setSelectedChildByClick(rootNode.sibling, refButton);
	}

	private static void setSelectedChild(MenuItem rootNode, JPanel refPanel) {
		if (rootNode == null) return;
		if (refPanel==null) return;
		
		if(refPanel.equals(rootNode.tabPanel)) {
			rootNode.tabPanel.setVisible(true);
			rootNode.btnItem.setFont(new Font(rootNode.btnItem.getFont().getName(), Font.BOLD, rootNode.getFont().getSize()));
		}
		else if(rootNode.tabPanel != null) {
			rootNode.tabPanel.setVisible(false);
			rootNode.btnItem.setFont(new Font(rootNode.btnItem.getFont().getName(), Font.PLAIN, rootNode.btnItem.getFont().getSize()));
		}

		setSelectedChild(rootNode.child, refPanel);
		setSelectedChild(rootNode.sibling, refPanel);
	}
	
	private static JPanel getSelectedChild(MenuItem rootNode) {
		if(rootNode.btnItem.getFont().getStyle() == Font.BOLD)
			return rootNode.tabPanel;
		
		JPanel retVal = null;
		
		if(rootNode.child != null) retVal = getSelectedChild(rootNode.child);
		
		if(retVal == null && rootNode.sibling != null) retVal = getSelectedChild(rootNode.sibling);
		
		return retVal;
	}
	
	private static MenuItem addChild(MenuItem rootNode, MenuItem newItem) {
		if(rootNode == null) return newItem;
		
		// on startup
		if(newItem.topPanel == null && rootNode.topPanel == null) {
			rootNode.sibling = addChild(rootNode.sibling, newItem);
			return rootNode;
		}
		if(newItem.topPanel != null && newItem.topPanel.equals(rootNode.topPanel)) {// top
			rootNode.sibling = addChild(rootNode.sibling, newItem);
			return rootNode;
		}
		if (newItem.topPanel != null && newItem.topPanel.equals(rootNode.tabPanel)) { // tab
				rootNode.child = addChild(rootNode.child, newItem);
				return rootNode;
		}
		// adding filtered results, etc
		if(rootNode.child != null)
			rootNode.child = addChild(rootNode.child, newItem);
		if(rootNode.sibling != null)
			rootNode.sibling = addChild(rootNode.sibling, newItem);
		return rootNode;
	}
	
	private static JPanel getMenuItem(MenuItem rootNode, JButton closeButton) {
		if(closeButton.equals(rootNode.btnClose))
			return rootNode.tabPanel;
		
		JPanel retVal = null;
		if(rootNode.child != null)
			retVal = getMenuItem(rootNode.child, closeButton);
		if(retVal == null && rootNode.sibling != null)
			retVal = getMenuItem(rootNode.sibling, closeButton);
		
		return retVal;
	}
	
	private static MenuItem removeMenuItem(MenuItem rootNode, JPanel refPanel) {
		if(rootNode == null) return null;
		
		if(refPanel.equals(rootNode.tabPanel))
			return rootNode.sibling;
		
		rootNode.child = removeMenuItem(rootNode.child, refPanel);
		rootNode.sibling = removeMenuItem(rootNode.sibling, refPanel);
		
		return rootNode;
	}
		
	private static boolean doesNodeLabelBeginWith(MenuItem rootNode, String prefix) {
		if(rootNode == null) return false;
		if(rootNode.btnItem.getText().startsWith(prefix))
			return true;
		
		boolean bChildFound = doesNodeLabelBeginWith(rootNode.child, prefix);
		boolean bSibFound = doesNodeLabelBeginWith(rootNode.sibling, prefix);
		
		return bChildFound || bSibFound;
	}
	
	private static void renameMenuItem(MenuItem rootNode, JPanel refPanel, String newString) {
		if(rootNode == null) return;
		if(refPanel.equals(rootNode.tabPanel)) {
			rootNode.btnItem.setText(newString);
			return;
		}
		renameMenuItem(rootNode.child, refPanel, newString);
		renameMenuItem(rootNode.sibling, refPanel, newString);
	}
	
	private void refresh() {
		removeAll();
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());

		buildPanel(root.child, 1);
		setMaximumSize(getPreferredSize());

		setVisible(false);
		setVisible(true);
	}
	
	private void buildPanel(MenuItem rootNode, int tabLevel) {
		if(rootNode == null) return;
		
		JPanel temp = createSubPanel(rootNode, tabLevel);
		temp.setMaximumSize(new Dimension(Math.max(300, temp.getPreferredSize().width), 
				temp.getPreferredSize().height));
		
		add(temp);
		buildPanel(rootNode.child, tabLevel + 1);
		buildPanel(rootNode.sibling, tabLevel);
	}
	
	private JPanel createSubPanel(MenuItem item, int tabLevel) {
		JPanel temp = Static.createRowPanel();
		
		if(item.isLeaf()) item.btnClose.setVisible(true);
		else if(item.btnClose != null) item.btnClose.setVisible(false);
		
		temp.add(Box.createHorizontalStrut(TABSIZE * tabLevel));
		temp.add(item.displayPanel);

		return temp;
	}
	
	/**********************************************************/
	private class MenuItem extends JPanel {
		private static final long serialVersionUID = 1L;
		
		public MenuItem(JPanel refParentPanel, JPanel refPanel, String name, 
				String description, ActionListener selectListener, ActionListener closeListener) {
			topPanel = refParentPanel;
			tabPanel = refPanel;
			final String descrip = description;
		
			displayPanel = Static.createRowPanel();
			if(selectListener != null) {
				btnItem = new JButton(name);
				btnItem.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
				btnItem.setBorderPainted(false);
				btnItem.setFocusPainted(false);
				btnItem.setContentAreaFilled(false);
				btnItem.setMargin(new Insets(0, 0, 0, 0));
				btnItem.setVerticalAlignment(AbstractButton.TOP);
				btnItem.setHorizontalAlignment(AbstractButton.LEFT);
				
				btnItem.addActionListener(theClickListener);
				btnItem.addActionListener(selectListener);
				btnItem.addMouseListener(new MouseAdapter() 
				{
					public void mouseEntered(MouseEvent e) {
						theViewerFrame.setStatus(descrip);
					}
					public void mouseExited(MouseEvent e) {
						theViewerFrame.setStatus("");
					}
				});
				displayPanel.add(btnItem);
			}
			
			if(closeListener != null) {
				btnClose = new JButton("x");
				btnClose.setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());
				btnClose.setBorderPainted(false);
				btnClose.setFocusPainted(false);
				btnClose.setContentAreaFilled(false);
				btnClose.setMargin(new Insets(0, 0, 0, 0));
				btnClose.setVerticalAlignment(AbstractButton.TOP);
				btnClose.setHorizontalAlignment(AbstractButton.LEFT);
				btnClose.setFont(theViewerFrame.getSettings().getFrameSettings().getDefaultFont());
				btnClose.addActionListener(closeListener);
				displayPanel.add(Box.createHorizontalStrut(3));
				displayPanel.add(btnClose);
			}
		}

		public boolean isLeaf() {
			return btnClose != null && child == null;
		}
		private MenuItem sibling = null;
		private MenuItem child = null;
		
		private JButton btnItem = null;
		private JButton btnClose = null;

		private JPanel displayPanel = null;
		private JPanel topPanel = null;
		private JPanel tabPanel = null;
	}
	private ActionListener theSelectionListener = null;
	private ActionListener theCloseListener = null;
	private ActionListener theClickListener = null;
	private MTCWFrame theViewerFrame = null;
	private MenuItem root = null;
}
