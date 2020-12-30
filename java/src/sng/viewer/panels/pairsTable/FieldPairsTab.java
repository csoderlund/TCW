/**
 * Creates right panel for Select Pairs Columns
 */
package sng.viewer.panels.pairsTable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import java.util.Vector;

import sng.database.Globals;
import sng.util.FieldMapper;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.Static;
import util.ui.CollapsiblePanel;
import util.ui.UserPrompt;

public class FieldPairsTab extends Tab implements ActionListener 
{
	private static final String HTML = "html/viewSingleTCW/PairColumn.html";
	private static final String [] ALWAYS_SELECTED = { "Seq ID" };
	
	private static final long serialVersionUID = 7539920711023954756L;
	private static final int NAME_COL_WIDTH = 250;
	private static final int DESCR_COL_WIDTH = 400;

	private String prefID() {return sTCWdb + "_pairID";}
	private String prefLabel() {return sTCWdb + "_pairLabel";}
	
	public FieldPairsTab ( STCWFrame inFrame) throws Exception
	{
		super(inFrame, null);
		theParentFrame = inFrame;
		
		prefsRoot = theParentFrame.getPreferencesRoot();
		sTCWdb = getParentFrame().getdbName(); 
		
		theMapper = FieldPairsData.createPairFieldMapper();
		
		createColumnPanel1();
		
		setMapperFromPrefs();
		setSelectedFromMapper();
	}
	
	private void createColumnPanel1() {
		
		JPanel buttonPanel = createButtonPanel();
		JLabel lblTitle = new JLabel("Choose Columns");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		
		JTextArea txtDesc = new JTextArea(
				"To update a specific result table, select it followed by the 'Refresh Columns' button.");
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		txtDesc.setMaximumSize(txtDesc.getPreferredSize()); // needed to prevent vertical stretching
		
		centerPanel = new JPanel();
		
		BoxLayout tempLayout = new BoxLayout ( centerPanel, BoxLayout.Y_AXIS );
		centerPanel.setLayout(tempLayout);
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.setBackground(Color.WHITE);
		centerPanel.setAlignmentY(LEFT_ALIGNMENT);
		centerPanel.add(buttonPanel);
		centerPanel.add(Box.createVerticalStrut(20));
		centerPanel.add(lblTitle);
		centerPanel.add(txtDesc);
		centerPanel.add(Box.createVerticalStrut(20));

		createUIFromFields(0);
		
		JScrollPane scroller = new JScrollPane ( centerPanel );
		scroller.setBorder( null );
		scroller.setPreferredSize(getParentFrame().getSize());
		scroller.getVerticalScrollBar().setUnitIncrement(15);
		
		setLayout(new BoxLayout ( this, BoxLayout.Y_AXIS )); // needed for left-justification!
		add(scroller);
	}
	
	private void createUIFromFields(int tabLevel) {
		String[] groupNames = theMapper.getGroupNames();
		String[] groupDescriptions = theMapper.getGroupDescriptions();
		
		for (int i = 0;  i < groupNames.length;  i++) {
	        String[] fieldNames = theMapper.getFieldNamesByGroup(groupNames[i]);
	        String[] fieldDescriptions = theMapper.getFieldDescriptionsByGroup(groupNames[i]);
	        CollapsiblePanel subPanel = new CollapsiblePanel(groupNames[i], groupDescriptions[i]);
	     
	       if(groupNames[i].equals(FieldPairsData.GROUP_NAME_PAIR))
	        		ctgPairGeneralSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDescriptions);
	        else if(groupNames[i].equals(FieldPairsData.GROUP_NAME_BLAST))
	        		ctgPairBlastSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDescriptions);
	        else if(groupNames[i].equals(FieldPairsData.GROUP_NAME_OLAP))
	        		ctgPairOLPSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDescriptions);
	        	
	        centerPanel.add(new ItemPanel(subPanel, tabLevel));	
		}
	}
	private JCheckBox [] createGroupUIFromFields(CollapsiblePanel subPanel, String [] fieldNames, 
			String[] fieldDescriptions) {
		JCheckBox [] retVal = new JCheckBox[fieldNames.length];
	
		for (int j = 0;  j < fieldNames.length;  j++) {
		    	retVal[j] = new JCheckBox(fieldNames[j], false);
		    	retVal[j].setBackground(Color.WHITE);
		    	retVal[j].addActionListener(this);
		    	
		    	boolean done = false;
		    	for(int x=0; x<ALWAYS_SELECTED.length && !done; x++) {
		    		if(fieldNames[j].equals(ALWAYS_SELECTED[x])) {
		    			done = true;
		    			retVal[j].setSelected(true);
		    			retVal[j].setEnabled(false);
		    		}
		    	}
		    	subPanel.add(createFieldSelection(retVal[j], fieldDescriptions[j]));
		}
		return retVal;
	}
	private JPanel createFieldSelection(JCheckBox select, String description) {
   		JLabel lblDesc = new JLabel(description);
   		lblDesc.setFont(new Font(lblDesc.getFont().getName(),Font.PLAIN,lblDesc.getFont().getSize()));

   		JPanel subPanel = Static.createRowPanel();
   		subPanel.add(select);
   		Dimension d = select.getPreferredSize();
   		if(d.width < NAME_COL_WIDTH) subPanel.add(Box.createHorizontalStrut(NAME_COL_WIDTH - d.width));
   		subPanel.add(lblDesc);
   		d = lblDesc.getPreferredSize();
   		if(d.width < DESCR_COL_WIDTH) subPanel.add(Box.createHorizontalStrut(DESCR_COL_WIDTH - d.width));
	
   		return subPanel;
	}
	
	
	private JPanel createButtonPanel() {
		JButton btnExpandAll = new JButton("Expand All");
		btnExpandAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				expandSections(true);
			}
		});
		
		JButton btnCollapseAll = new JButton("Collapse All");
		btnCollapseAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				expandSections(false);
			}
		});
		
		JButton btnRefreshAll = new JButton("Refresh All Existing Filter Tables");
		btnRefreshAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				refreshAll();
			}
		});
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), 
						"Pairs Columns", HTML);
			}
		});
		
		JPanel buttonPanel = Static.createRowPanel();
		
		buttonPanel.add(btnExpandAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnCollapseAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnRefreshAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add(btnHelp);
		buttonPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)buttonPanel.getPreferredSize ().getHeight() ) );
		
		return buttonPanel;
	}
	/******************************************************************************
	 * XXX Preferences -- prefixed by database name
	 */
	// On startup
	private void setMapperFromPrefs() {
		if (prefsRoot==null) return;
		
		String strList = prefsRoot.get(prefID(), "");
		theMapper.setVisibleFieldIDsList(strList.split(","));
	}

	private void setPrefFromMapper() {
		try {
			if (prefsRoot==null) return;
	        String strLabelList = Static.join(theMapper.getVisibleFieldNames(), "\t");
	        String strIDList = Static.join( theMapper.getVisibleFieldIDsStr(), "," );
	       
	        prefsRoot.put ( prefID(), strIDList );
	        prefsRoot.flush();
	        prefsRoot.put ( prefLabel(), strLabelList );
	        prefsRoot.flush();
		}
		catch (Exception err) {
			System.err.println("Could not get preferences -- continue");
		}
	}
	
	private void setMapperFromSelected() {
		Vector<String> selected = new Vector<String>();	
				
		selected.addAll(getSelectedForGroup(ctgPairGeneralSelect));
		selected.addAll(getSelectedForGroup(ctgPairBlastSelect));
		selected.addAll(getSelectedForGroup(ctgPairHSRSelect));
		selected.addAll(getSelectedForGroup(ctgPairOLPSelect));
	
		theMapper.setVisibleFieldNames(selected.toArray());
		setPrefFromMapper();
	}
	private Vector<String> getSelectedForGroup(JCheckBox [] chkSelections) {
		
		Vector<String> retVal = new Vector<String> ();
		if(chkSelections == null) return retVal;
		
		for(int x=0; x<chkSelections.length; x++) {
			String name = chkSelections[x].getText();
			if (chkSelections[x].isSelected()) {
				if (theMapper.hasFieldName(name)) retVal.add(name);
			}			
		}	
		return retVal;
	}
	

	private void setSelectedFromMapper() {		
		setSelectedForGroup(ctgPairGeneralSelect, FieldPairsData.GROUP_NAME_PAIR);
		setSelectedForGroup(ctgPairBlastSelect, FieldPairsData.GROUP_NAME_BLAST);
		setSelectedForGroup(ctgPairOLPSelect, FieldPairsData.GROUP_NAME_OLAP);
	}
	private void setSelectedForGroup(JCheckBox [] chkFields, String groupName) {
		if(chkFields == null) return;
		
		for(int x=0; x<chkFields.length; x++) {
			String name = chkFields[x].getText();
		
			if (theMapper.getFieldRequiredByName(name)) {
				chkFields[x].setEnabled(false);
				chkFields[x].setSelected(true);
			}
			else {
				boolean selected = theMapper.isFieldVisible(name);
				chkFields[x].setEnabled(true);
				chkFields[x].setSelected(selected);
			}
		}
		
	}
	private void expandSections(boolean expand) {
		Component[] comps = centerPanel.getComponents();
		
		for (int i = 0;  i < centerPanel.getComponentCount();  i++) {		
			if (comps[i] instanceof ItemPanel) {
				CollapsiblePanel c = ((ItemPanel)comps[i]).getPanel();
				if (expand) c.expand();
				else c.collapse();
			}
		}
	}
	
	private void refreshAll() {
		Tab tabs[] = getParentFrame().tabbedPane.getTabs();
		for ( int i = 0; i < tabs.length; ++i ) {
			if ( tabs[i] instanceof PairTableTab ) {
				PairTableTab tab = (PairTableTab)tabs[i];
				getParentFrame().loadQueryFilter ( tab, tab.getQuery(), null );
			}
		}
	}
	
	/************* Public **********************/
	public FieldMapper getMapper() { // called before query
		setMapperFromSelected();
		FieldMapper newMapper = FieldPairsData.createPairFieldMapper();
		newMapper.setVisibleFieldNames(theMapper.getVisibleFieldNames());
		return newMapper;
	}
	public void close()
	{
		centerPanel = null;
	}
	public void actionPerformed(ActionEvent e) {
		//updateFields();
	}
	
	private class ItemPanel extends JPanel
	{
		private static final long serialVersionUID = 7344260472768859642L;

		public ItemPanel(CollapsiblePanel cp, int tabLevel)
		{
	    		setLayout(new BoxLayout ( this, BoxLayout.X_AXIS ));
	    		setAlignmentX(Component.LEFT_ALIGNMENT);
	    		setBackground(Color.WHITE);
	    		if(tabLevel > 0)
	    			add(Box.createRigidArea(new Dimension(tabLevel, 0)));
	    		add(cPanel = cp);
		}
		
		public CollapsiblePanel getPanel() { return cPanel; }
		
		private CollapsiblePanel cPanel = null;
	}
	// Private
	private JCheckBox [] ctgPairGeneralSelect = null;
	private JCheckBox [] ctgPairBlastSelect = null;
	private JCheckBox [] ctgPairHSRSelect = null;
	private JCheckBox [] ctgPairOLPSelect = null;
	
	private JPanel centerPanel = null;
	
	private String sTCWdb = ""; 
	private STCWFrame theParentFrame=null;
	private FieldMapper theMapper=null;
	private Preferences prefsRoot = null;
	
	
}