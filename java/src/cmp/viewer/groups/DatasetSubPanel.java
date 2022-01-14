package cmp.viewer.groups;

/*******************************************************
 * Called from GrpQueryPanel for the Dataset section
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import util.database.Globalx;

import cmp.viewer.MTCWFrame;
import util.methods.Static;

public class DatasetSubPanel extends JPanel {
	private static final long serialVersionUID = 5011440646805837338L;
	
	private static final String [] ASSEMBLY_COUNT_OPTIONS = { "All Datasets", "Any Dataset" };
		
	public DatasetSubPanel(MTCWFrame parentFrame) {
		theViewerFrame = parentFrame;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globalx.BGCOLOR);
		
		btnSelAllInc = Static.createButton("Select All", true);
		btnSelAllInc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				incList.setAllSelected(true);
			}
		});
		btnSelNoneInc = Static.createButton("UnSelect All", true);
		btnSelNoneInc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				incList.setAllSelected(false);
			}
		});
		btnSelNotEx = Static.createButton("Check all not excluded", true);
		btnSelNotEx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				incList.setSelectedNot(exList.getSelected());
			}
		});
		txtIncVal = Static.createTextField("1", 3); 
		
		cmbInc = new ButtonComboBox();
		cmbInc.setBackground(Color.white);
		for(int x=0; x<ASSEMBLY_COUNT_OPTIONS.length; x++)
			cmbInc.addItem(ASSEMBLY_COUNT_OPTIONS[x]);
		cmbInc.setSelectedIndex(0);
		
		
		btnSelAllEx = Static.createButton("Select All", true);
		btnSelAllEx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				exList.setAllSelected(true);
			}
		});
		btnSelNoneEx = Static.createButton("UnSelect All", true);
		btnSelNoneEx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				exList.setAllSelected(false);
			}
		});
		btnSelNotInc = Static.createButton("Check all not included", true);
		btnSelNotInc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exList.setSelectedNot(incList.getSelected());
			}
		});
		
		txtExVal = Static.createTextField("0", 3);
		
		cmbEx = new ButtonComboBox();
		cmbEx.setBackground(Color.white);
		for(int x=0; x<ASSEMBLY_COUNT_OPTIONS.length; x++)
			cmbEx.addItem(ASSEMBLY_COUNT_OPTIONS[x]);
		cmbEx.setSelectedIndex(0);

		JPanel rangePanelInc = Static.createPagePanel();	
		JLabel incLabel = toolTipLabel("Include Dataset", "Cluster must have >=N  sequences from the selected datasets (see Help).", true);
		rangePanelInc.add(incLabel);
		rangePanelInc.add(Box.createVerticalStrut(20));
		
		JPanel tempPanel = Static.createRowPanel();
		tempPanel.add(Static.createLabel("At least "));
		tempPanel.add(txtIncVal);
		tempPanel.add(Static.createLabel(" sequence(s) "));
		tempPanel.setMaximumSize(tempPanel.getPreferredSize());	
		rangePanelInc.add(tempPanel);
		rangePanelInc.add(Box.createVerticalStrut(5));
		
		tempPanel = Static.createRowPanel();
		tempPanel.add(Static.createLabel("From "));
		tempPanel.add(cmbInc);
		tempPanel.add(Static.createLabel("   "));
		tempPanel.setMaximumSize(tempPanel.getPreferredSize());		
		rangePanelInc.add(tempPanel);
		
		JPanel rangePanelEx =  Static.createPagePanel();
		JLabel exLabel = toolTipLabel("Exclude Dataset", "Cluster may NOT have >N sequences from the selected datasets (see Help).", true);
		rangePanelEx.add(exLabel);
		rangePanelEx.add(Box.createVerticalStrut(20));
		
		tempPanel = Static.createRowPanel();
		tempPanel.add(Static.createLabel("At most "));
		tempPanel.add(txtExVal);
		tempPanel.add(Static.createLabel(" sequence(s) "));
		tempPanel.setMaximumSize(tempPanel.getPreferredSize());		
		rangePanelEx.add(tempPanel);
		rangePanelEx.add(Box.createVerticalStrut(5));
		
		tempPanel = Static.createRowPanel();
		tempPanel.add(Static.createLabel("From "));
		tempPanel.add(cmbEx);
		tempPanel.add(Static.createLabel("   "));
		tempPanel.setMaximumSize(tempPanel.getPreferredSize());		
		rangePanelEx.add(tempPanel);
		
		incList = new DataSetList(theViewerFrame.getAsmList());
		exList = new DataSetList(theViewerFrame.getAsmList());

		JPanel buttonPanelInc = Static.createPagePanel();
		buttonPanelInc.add(btnSelAllInc); buttonPanelInc.add(Box.createVerticalStrut(5));
		buttonPanelInc.add(btnSelNoneInc); buttonPanelInc.add(Box.createVerticalStrut(5));
		buttonPanelInc.add(btnSelNotEx);	  buttonPanelInc.setMaximumSize(buttonPanelInc.getPreferredSize());

		JPanel incRow = Static.createRowPanel();
		incRow.add(rangePanelInc); incRow.add(Box.createHorizontalStrut(10));
		incRow.add(incList); incRow.add(Box.createHorizontalStrut(10));
		incRow.add(buttonPanelInc);
		
		JPanel buttonPanelEx = Static.createPagePanel();
		buttonPanelEx.add(btnSelAllEx); buttonPanelEx.add(Box.createVerticalStrut(5));
		buttonPanelEx.add(btnSelNoneEx); buttonPanelEx.add(Box.createVerticalStrut(5));
		buttonPanelEx.add(btnSelNotInc);	 buttonPanelEx.setMaximumSize(buttonPanelEx.getPreferredSize());

		JPanel exRow = Static.createRowPanel();
		exRow.add(rangePanelEx); exRow.add(Box.createHorizontalStrut(10));
		exRow.add(exList); exRow.add(Box.createHorizontalStrut(10));
		exRow.add(buttonPanelEx);

		add(incRow);
		add(Box.createVerticalStrut(25));
		add(exRow);
	}
	private JLabel toolTipLabel(String label, String descript, boolean enable) {
		JLabel tmp = new JLabel(label);
		tmp.setBackground(Color.white);
		tmp.setEnabled(enable);
		final String desc = descript;
		tmp.addMouseListener(new MouseAdapter() 
		{
			public void mouseEntered(MouseEvent e) {
			    theViewerFrame.setStatus(desc);
			}
			public void mouseExited(MouseEvent e) {
			    theViewerFrame.setStatus("");
			}
		});
		return tmp;
	}
	public String [] getIncludedNames() { return incList.getSelectedLabels(); }
	public String [] getExcludedNames() { return exList.getSelectedLabels(); }
	public void setAllSelectedInc(boolean selected) { incList.setAllSelected(selected); }
	public void setAllSelectedEx(boolean selected) { exList.setAllSelected(selected); }
	public void setIncludeLimit(String value) { txtIncVal.setText(value); }
	public void setExcludeLimit(String value) { txtExVal.setText(value); }
	public int getIncludeLimit() {
		try {
			return Integer.parseInt(txtIncVal.getText());
		}
		catch(Exception e) {
			return 0;
		}
	}
	public int getExcludeLimit() {
		try {
			return Integer.parseInt(txtExVal.getText());
		}
		catch(Exception e) {
			return 0;
		}
	}
	public boolean getIncAnd() { return cmbInc.getSelectedIndex() == 0; }
	public boolean getExAnd() { return cmbEx.getSelectedIndex() == 0; }
	
	private DataSetList incList = null, exList = null;
	private JButton btnSelAllInc = null, btnSelNoneInc = null, btnSelNotEx = null;
	private JButton btnSelAllEx = null, btnSelNoneEx = null, btnSelNotInc = null;
	private JTextField txtIncVal = null, txtExVal = null;
	private ButtonComboBox cmbInc = null, cmbEx = null;
	private MTCWFrame theViewerFrame = null;
	
	/******************************************************************/
	private class DataSetList extends JPanel {
		private static final long serialVersionUID = 5586648551611595554L;

		public DataSetList(String [] assemblies) {
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setBackground(theViewerFrame.getSettings().getFrameSettings().getBGColor());

			DSetListItem [] theItems = new DSetListItem[assemblies.length];
			for(int x=0; x<theItems.length; x++)
				theItems[x] = new DSetListItem(assemblies[x], false);
			
			theList = new JList <DSetListItem> (theItems);
			theList.setCellRenderer(new TheListRenderer());
			theList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			theList.addMouseListener(new MouseAdapter() 
			{
				public void mouseClicked(MouseEvent e) {
					@SuppressWarnings("unchecked")
					JList <DSetListItem> list = (JList <DSetListItem> ) e.getSource();
					
					int index = list.locationToIndex(e.getPoint());
					DSetListItem item = list.getModel().getElementAt(index);
					item.setSelected(!item.isSelected());
					list.repaint(list.getCellBounds(index, index));
				}
			});
			sPane = new JScrollPane(theList);
			sPane.setSize(new Dimension(800, 200));
			sPane.setPreferredSize(new Dimension(200, 100));
			add(sPane);
		}
		
		public String [] getSelectedLabels() {
			int numResults = 0;
			for(int x=0; x<theList.getModel().getSize(); x++) {
				if((theList.getModel().getElementAt(x)).isSelected()) {
					numResults++;
				}
			}
			
			if(numResults > 0) {
				String [] retVal = new String[numResults];
				int pos = 0;
				for(int x=0; x<theList.getModel().getSize(); x++) {
					if((theList.getModel().getElementAt(x)).isSelected()) {
						retVal[pos] = (theList.getModel().getElementAt(x)).toString();
						pos++;
					}
				}
				return retVal;
			}
			
			return null;
		}
		
		public void setAllSelected(boolean selected) {
			for(int x=0; x<theList.getModel().getSize(); x++) {
				(theList.getModel().getElementAt(x)).setSelected(selected);
			}
			theList.repaint(100);
		}
		
		public boolean [] getSelected() {
			boolean [] retVal = new boolean[theList.getModel().getSize()];
			
			for(int x=0; x<retVal.length; x++) {
				retVal[x] = (theList.getModel().getElementAt(x)).isSelected();
			}
			
			return retVal;
		}
		
		public void setSelectedNot(boolean [] selections) {
			for(int x=0; x<selections.length; x++) {
				(theList.getModel().getElementAt(x)).setSelected(!selections[x]);
			}
			theList.repaint(100);
		}
		
		private JList <DSetListItem> theList = null;
		private JScrollPane sPane = null;
	}
	/********************************************************************/
	private class DSetListItem {
		public DSetListItem(String label, boolean selected) {
			strLabel = label;
			bIsSelected = selected;
		}
		
		public boolean isSelected() { return bIsSelected; }
		public void setSelected(boolean selected) { bIsSelected = selected; }
		public String toString() { return strLabel; }
		
		private String strLabel = "";
		private boolean bIsSelected = false;
	}
	
	private class TheListRenderer extends JCheckBox implements  ListCellRenderer <Object>
	{
		private static final long serialVersionUID = -2234133731431277696L;

		public Component getListCellRendererComponent(	JList <?> list, Object value, int index,
														boolean isSelected, boolean hasFocus)
		{
			setEnabled(list.isEnabled());
			setSelected(((DSetListItem)value).isSelected());
			setFont(list.getFont());
			setBackground(list.getBackground());
			setForeground(list.getForeground());
			setText(value.toString());
			setMaximumSize(getPreferredSize());
			return this;
		}
	}
}
