package sng.viewer.panels.seqTable;

/*****************************************
 * Create Column Panel for Sequence
 */
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;
import java.util.Vector;


import sng.database.Globals;
import sng.util.FieldMapper;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.Static;
import util.ui.CollapsiblePanel;
import util.ui.UserPrompt;
import util.ui.DisplayFloat;

public class FieldContigTab extends Tab implements ActionListener {
	private static final long serialVersionUID = -1208399171249754290L;
	
	private static final String HTML = "html/viewSingleTCW/SeqColumn.html";
	private static final String [] ALWAYS_SELECTED = { "Seq ID" };
	public static final int NAME_COL_WIDTH = 250;
	public static final int DESCR_COL_WIDTH = 400;
	
	private String sTCWdb = ""; // used for preference prefix 
	private String prefID() {return sTCWdb + "_ctgID";}
	private String prefLabel() {return sTCWdb + "_ctgLabel";}
	private String prefRound() {return "rounding";}
	
	public FieldContigTab ( STCWFrame inFrame) {
		super(inFrame, null);
		theParentFrame = inFrame;
		
		sTCWdb = theParentFrame.getdbName(); 
		prefsRoot = theParentFrame.getPreferencesRoot();
		theMapper = FieldContigData.createContigFieldMapper(theParentFrame);
		
		createColumnPanel();
		
		setMapperFromPrefs();
		setSelectedFromMapper(); 
	}
	private void createColumnPanel() {
		centerPanel = Static.createPagePanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel buttonPanel = createButtonPanel();
		centerPanel.add(buttonPanel);
		
		JLabel lblTitle = Static.createTitleLabel("Choose Columns");
		centerPanel.add(Box.createVerticalStrut(10));
		centerPanel.add(lblTitle);
		
		JTextArea txtDesc = Static.createTextArea(
			"To update a specific result table, select it followed by the 'Refresh Columns' button.");
		centerPanel.add(txtDesc);
		centerPanel.add(Box.createVerticalStrut(10));

		createUIFromMapper(0);
		
		JScrollPane scroller = new JScrollPane ( centerPanel );
		scroller.setBorder( null );
		scroller.setPreferredSize(getParentFrame().getSize());
		scroller.getVerticalScrollBar().setUnitIncrement(15);
		
		setLayout(new BoxLayout ( this, BoxLayout.Y_AXIS )); // needed for left-justification!
		add(scroller);
	}
	
	/***************************************
	 * when the Mapper was created, all columns were instantiate from FieldContigData.
	 * Use those for creating the UI. Not all are instantiated.
	 */
	private void createUIFromMapper(int tabLevel) {
		String[] grpNames = theMapper.getGroupNames();
		String[] grpDesc = theMapper.getGroupDescriptions();
		
		for (int i = 0;  i < grpNames.length;  i++) {
	        String[] fieldNames = theMapper.getFieldNamesByGroup(grpNames[i]);
	        String[] fieldDesc = theMapper.getFieldDescriptionsByGroup(grpNames[i]);
	        
	        if(grpNames[i].equals(FieldContigData.GROUP_NAME_OVER_BEST)) {
	        		bestObj.addNames(fieldNames, FieldContigData.GROUP_NAME_OVER_BEST);
				continue;
			}
			if(grpNames[i].equals(FieldContigData.GROUP_NAME_GO_BEST)) {
				bestObj.addNames(fieldNames, FieldContigData.GROUP_NAME_GO_BEST);
				continue;
			}
			
			String name = grpNames[i].equals(FieldContigData.GROUP_NAME_FIRST_BEST) ?
					"Best Hits" : grpNames[i];
			String desc = grpNames[i].equals(FieldContigData.GROUP_NAME_FIRST_BEST) ?
					"EV - Best E-value, AN - Best Annotation, WG - Best with GO" : grpDesc[i];
	        CollapsiblePanel subPanel = new CollapsiblePanel(name, desc);
	     
	        if(grpNames[i].equals(FieldContigData.GROUP_NAME_CONTIG)) {
	        		ctgGeneralSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDesc, grpNames[i]);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_LIB)) {
		        createLibUIFromFields(subPanel, fieldNames, grpNames[i], fieldDesc);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_SEQ_SET)) {
	        		ctgSetSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDesc, grpNames[i]);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_PVAL)) {
	        		createPvalUIFromFields(subPanel, fieldNames, fieldDesc, grpNames[i]);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_RSTAT)) {
 				nFoldObj = new UIfieldNFold(libListSelect, false); 
 				subPanel.add(nFoldObj);
 				
 				subPanel.add(new JSeparator());
	        		ctgRStatSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDesc, grpNames[i]);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_FIRST_BEST)) {
	        		createBestHit(subPanel, fieldNames, fieldDesc, name);
	        }
	        else if(grpNames[i].equals(FieldContigData.GROUP_NAME_SNPORF)) {
	        		ctgSNPORFSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDesc, grpNames[i]);
	        }
	        centerPanel.add(new ItemPanel(subPanel, tabLevel));	 // create a "tab" option	
		}
	}
	
	private JCheckBox [] createGroupUIFromFields(CollapsiblePanel subPanel, String [] fieldNames, 
			String[] fieldDesc, String grpName) {
		
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
		    	if (grpName.equals(FieldContigData.GROUP_NAME_CONTIG)) {
		    		if (fieldNames[j].equals("#Taxonomy") || fieldNames[j].equals("SeqGroup"))
		    			subPanel.add(new JSeparator());
		    	}
		    	subPanel.add(createRowWithCheck(retVal[j], fieldDesc[j]));
		}
		return retVal;
	}

	private JPanel createRowWithCheck(JCheckBox select, String description) {
   		JLabel lblDesc = new JLabel(description);
   		lblDesc.setFont(new Font(lblDesc.getFont().getName(),Font.PLAIN,lblDesc.getFont().getSize()));

   		JPanel row = Static.createRowPanel();
   		row.add(select);
   		Dimension d = select.getPreferredSize();
   		if(d.width < NAME_COL_WIDTH) row.add(Box.createHorizontalStrut(NAME_COL_WIDTH - d.width));
   		row.add(lblDesc);
   		d = lblDesc.getPreferredSize();
   		if(d.width < DESCR_COL_WIDTH) row.add(Box.createHorizontalStrut(DESCR_COL_WIDTH - d.width));
	
   		return row;
	}
	private void createBestHit(CollapsiblePanel subPanel, String [] fieldNames, 
			String[] fieldDesc, String grpName) {
	
		boolean hasGO = theParentFrame.getMetaData().hasGOs();
		
		bestObj = new UIfieldBestHit (fieldNames, fieldDesc, hasGO);
		
		subPanel.add(bestObj);
	}
	private void createLibUIFromFields(CollapsiblePanel subPanel, 
			String [] fieldNames, String grpName, String[] fieldDesc) {
		Integer[] fieldIDs = theMapper.getFieldIDsByGroup(grpName);
		int minID = FieldContigData.N_LIBRARY_COUNT_ALL;
		int maxID = FieldContigData.CONTIG_SET_COUNT;
		
		// Count RPKM Panel
		JPanel libCntPanel = Static.createRowPanel();
		
		chkLibExpLevel = Static.createCheckBox("Counts");
		chkLibExpLevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateLibSelectedGroupFields(FieldContigData.LIBRARY_COUNT_ALL, 
						FieldContigData.N_LIBRARY_COUNT_ALL);
			}
		});
		libCntPanel.add(chkLibExpLevel);
		
		String norm = theParentFrame.getMetaData().getNorm(); // CAS304
		chkLibNExpLevel = Static.createCheckBox(norm + " Normalized Counts");
		chkLibNExpLevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				updateLibSelectedGroupFields(FieldContigData.N_LIBRARY_COUNT_ALL, 
						FieldContigData.CONTIG_SET_COUNT);
			}
		});
		libCntPanel.add(chkLibNExpLevel);
		libCntPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		subPanel.add(libCntPanel);
		
		// List libraries
		Vector<JCheckBox> mainList = new Vector<JCheckBox> ();
		JCheckBox [] chkBox = new JCheckBox[fieldNames.length];
	    	for (int j = 0;  j < fieldNames.length;  j++) {
	    		chkBox[j] = new JCheckBox(fieldNames[j], false);
	
	        	//Only allow exp level fields to be shown
	        	if(fieldIDs[j] >= minID && fieldIDs[j] < maxID) {
	        		JCheckBox tempCheck = new  JCheckBox(fieldNames[j], false);
	        		tempCheck.setBackground(Color.WHITE);
	        		tempCheck.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							chkLibSelAll.setSelected(false);
							updateLibSelectedGroupFields(-1, -1);
						}
					});
	        		mainList.add(tempCheck);
	        		subPanel.add(createRowWithCheck(tempCheck, fieldDesc[j]));
	        	}
	   	}
		libListSelect = mainList.toArray(new JCheckBox[0]);
		
		// Check/Uncheck
		chkLibSelAll = Static.createCheckBox("Check/uncheck all");
		chkLibSelAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean bIsLibSelectAll = chkLibSelAll.isSelected();
				for(int x=0; x<libListSelect.length; x++)
					libListSelect[x].setSelected(bIsLibSelectAll);
			}
		});
		JPanel chkPanel = Static.createRowPanel();
		chkPanel.add(chkLibSelAll);
		subPanel.add(chkPanel);
	}
	private void createPvalUIFromFields(CollapsiblePanel subPanel, String [] fieldNames, 
			String[] fieldDesc, String grpName) {
		
		pValSelect = createGroupUIFromFields(subPanel, fieldNames, fieldDesc, grpName);
		
		JCheckBox chkAllPval = Static.createCheckBox("Check/uncheck all"); 
		chkAllPval.addItemListener(	
			new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e) 
				{
					JCheckBox chkall = (JCheckBox)e.getSource();
					for (JCheckBox chk : pValSelect) {
						chk.setSelected(chkall.isSelected());
					}
				}
			}
		);
		JPanel chkPanel = Static.createRowPanel();
		chkPanel.add(chkAllPval);
		subPanel.add(chkPanel);
	}
	private JPanel createButtonPanel() {
		JButton btnExpandAll = new JButton("Expand All");
		btnExpandAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				expandGroups(true);
			}
		});
		
		JButton btnCollapseAll = new JButton("Collapse All");
		btnCollapseAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				expandGroups(false);
			}
		});
		JButton btnRestore = new JButton("Clear");
		btnRestore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				clear();
			}
		});
		JButton btnRefreshAll = new JButton("Refresh Existing Tables");
		btnRefreshAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {	
				refreshAllTables();
			}
		});
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), 
						"Sequence Columns", HTML);
			}
		});
		
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.add(btnExpandAll);   buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnCollapseAll); buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnRefreshAll);  buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnRestore);     buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add(btnHelp);   
		buttonPanel.add(Box.createHorizontalStrut(40));
		buttonPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)buttonPanel.getPreferredSize ().getHeight() ) );
		return buttonPanel;
	}
	// end create UI
	/*********** Methods for communication between mapper and prefs ***********/
	// On startup
	private void setMapperFromPrefs() {
		if (prefsRoot==null) return;
		
		String prefList = prefsRoot.get(prefLabel(), "");
		if (!prefList.contains("Seq ID")) prefList += "\tSeq ID"; // Remove: It is required -- just while changing code, lost it.
		theMapper.setVisibleFieldNames(prefList.split("\t"));
		
		setNFoldFromPrefs();
	}
	private void setNFoldFromPrefs() {
		if (prefsRoot==null || nFoldObj==null) return;
		
        String [] names = prefsRoot.get(prefLabel(), "").split("\t");
        String [] ids =   prefsRoot.get(prefID(), "").split(",");
        if (names.length <= 1) return;

        	
        for(int x=0; x<ids.length; x++) {	// initiates columns in nFold object
			if(theMapper.isNFoldField(Integer.parseInt(ids[x])) && x<names.length) {
				nFoldObj.initFoldColumn(names[x]);
			}
		}
        theMapper.setVisibleNFold(getFoldColumns());  // assigns the names to mapper
        
        nFoldObj.addFoldColumns(theMapper); // assigns column numbers to mapper 
	}
	
	// On started and Restore Defaults
	private void setSelectedFromMapper() {
		setSelectedForGroup(ctgGeneralSelect, 	FieldContigData.GROUP_NAME_CONTIG);
		setSelectedForGroup(ctgSetSelect,  		FieldContigData.GROUP_NAME_SEQ_SET);
		setSelectedForGroup(pValSelect, 			FieldContigData.GROUP_NAME_PVAL);
		setSelectedForGroup(ctgRStatSelect, 		FieldContigData.GROUP_NAME_RSTAT);
		setSelectedForGroup(ctgDBHitSelect, 		FieldContigData.GROUP_NAME_CNTS);
		setSelectedForGroup(ctgSNPORFSelect, 	FieldContigData.GROUP_NAME_SNPORF);
		
		setSelectedForBest();
		setSelectedLibsFromMapper(libListSelect);
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
				chkFields[x].setSelected(selected);
			}
		}
	}
	private void setSelectedForBest() {
		if (bestObj==null) return;
		
		JCheckBox [] chk = bestObj.firstChk;
		String [] name = bestObj.firstLabel;
		for(int i=0; i<chk.length; i++) {
			boolean selected = theMapper.isFieldVisible(name[i]);
			chk[i].setSelected(selected);
		}
		chk = bestObj.overChk;
		name = bestObj.overLabel;
		for(int i=0; i<chk.length; i++) {
			boolean selected = theMapper.isFieldVisible(name[i]);
			chk[i].setSelected(selected);
		}
		chk = bestObj.goChk;
		name = bestObj.goLabel;
		for(int i=0; i<chk.length; i++) {
			boolean selected = theMapper.isFieldVisible(name[i]);
			chk[i].setSelected(selected);
		}
	}
	private void setSelectedLibsFromMapper(JCheckBox [] chkFields) {	
		if (prefsRoot==null || chkLibExpLevel==null) return;
		
		String [] temp = prefsRoot.get(prefLabel(), "").split("\t");
		Vector<String> colNames = new Vector<String> ();
		for(int x=0; x<temp.length; x++) colNames.add(temp[x]);
		
		String [] strIDList = theMapper.getVisibleFieldIDsStr();
		String [] strLabelList = theMapper.getVisibleFieldNames();
		boolean expLevel = chkLibExpLevel.isSelected();
		boolean expNLevel = chkLibNExpLevel.isSelected();
		
		for(int x=0; x<strIDList.length; x++) {
			int id = Integer.parseInt(strIDList[x]);
			if(id >= FieldContigData.LIBRARY_COUNT_ALL && id < FieldContigData.N_LIBRARY_COUNT_ALL && 
					colNames.contains(strLabelList[x])) {
				expLevel = true;
				chkFields[id - FieldContigData.LIBRARY_COUNT_ALL].setSelected(true);
			}
			else if(id >= FieldContigData.N_LIBRARY_COUNT_ALL && id < FieldContigData.CONTIG_SET_COUNT && 
					colNames.contains(strLabelList[x])) {
				expNLevel = true;
				chkFields[id - FieldContigData.N_LIBRARY_COUNT_ALL].setSelected(true);
			}
		}

		chkLibExpLevel.setSelected(expLevel);
		chkLibNExpLevel.setSelected(expNLevel);
		
		//Now check if nothing selected, if so, select RPKM option
		boolean found = false;
		for(int x=0; x<libListSelect.length && !found; x++)
			found = libListSelect[x].isSelected();
		if(!chkLibExpLevel.isSelected() && !chkLibNExpLevel.isSelected() && !found)
			chkLibNExpLevel.setSelected(true);
			
		// check if everything is selected, if so, select Check All
		found = true; 
		for(int x=0; x<libListSelect.length && found; x++)
			found = libListSelect[x].isSelected();
		chkLibSelAll.setSelected(found);
	}
	
	
	/************************************************
	 * Set Mapper fields from Selected so used in Query. 
	 */
	private void setMapperFromSelected() {
		Vector<String> selected = new Vector<String>();	
				
		selected.addAll(getSelecteForGroup(ctgGeneralSelect));
		selected.addAll(getSelectedFromLib(libListSelect));
		selected.addAll(getSelecteForGroup(ctgSetSelect));
		selected.addAll(getSelecteForGroup(pValSelect));
		selected.addAll(getSelecteForGroup(ctgRStatSelect));
		selected.addAll(getSelecteForGroup(ctgDBHitSelect));
		selected.addAll(getSelectedForBest());
		selected.addAll(getSelecteForGroup(ctgSNPORFSelect));
		selected.addAll(getSelectedFromNFold());
		
		theMapper.setVisibleField(selected.toArray());
		theMapper.setVisibleNFold(getFoldColumns());
		
		setPrefFromMapper();
	}
	private Vector<String> getSelecteForGroup(JCheckBox [] chkSelections) {
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
	private Vector<String> getSelectedForBest() {
		Vector<String> retVal = new Vector<String> ();
		if (bestObj==null) return retVal;
		
		for (int i=0; i<bestObj.firstChk.length; i++) {
			if (bestObj.firstChk[i].isSelected()) {
				String name = bestObj.firstLabel[i];
				if (theMapper.hasFieldName(name)) retVal.add(name);
			}
		}
		for (int i=0; i<bestObj.overChk.length; i++) {
			if (bestObj.overChk[i].isSelected()) {
				String name = bestObj.overLabel[i];
				if (theMapper.hasFieldName(name)) retVal.add(name);
			}
		}
		for (int i=0; i<bestObj.goChk.length; i++) {
			if (bestObj.goChk[i].isSelected()) {
				String name = bestObj.goLabel[i];
				if (theMapper.hasFieldName(name)) retVal.add(name);
			}
		}
		return retVal;
	}
	
	private Vector<String> getSelectedFromLib(JCheckBox[] chkSelections) {
		Vector<String> retVal = new Vector<String> ();
		
		if(chkSelections == null) return retVal;
		if (chkLibExpLevel == null) return retVal;
		
		for(int x=0; x<chkSelections.length; x++) {
			if(chkSelections[x].isSelected()) {
				String name = chkSelections[x].getText();
				if(chkLibExpLevel.isSelected()) retVal.add("#" + name);
				if(chkLibNExpLevel.isSelected()) retVal.add(name);
			}
		}		
		return retVal;		
	}
	
	private Vector<String> getSelectedFromNFold() {
		Vector<String> retVal = new Vector<String> ();
		if (nFoldObj==null) return retVal;
		
		String [] names = nFoldObj.getColumnNames();
		for(int x=0; x<names.length; x++) {
			if(!retVal.contains(names[x])) retVal.add(names[x]);
		}
		return retVal;		
	}
	private void updateLibSelectedGroupFields(int min, int max) {
		if(min >= 0 && max >= 0)
			theMapper.hideIDRange(min, max);
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
	        prefsRoot.put(prefRound(), DisplayFloat.getPrefString());
			prefsRoot.flush();
		}
		catch (Exception err) { 
			System.err.println("Could not get preferences -- continuing... ");
		}
	}
	
	/************** Utilities ************************/
	
	private void expandGroups(boolean expand) {
		Component[] comps = centerPanel.getComponents();
		
		for (int i = 0;  i < centerPanel.getComponentCount();  i++) {		
			if (comps[i] instanceof ItemPanel) {
				CollapsiblePanel c = ((ItemPanel)comps[i]).getPanel();
				if (expand) c.expand();
				else c.collapse();
			}
		}
	}
	
	private void refreshAllTables() {
		Tab tabs[] = getParentFrame().tabbedPane.getTabs();
		for ( int i = 0; i < tabs.length; ++i ) {
			// Update any open contig list tabs
			if ( tabs[i] instanceof ContigListTab ) {
				ContigListTab tab = (ContigListTab)tabs[i];
				if(tab.getQuery() != null && tab.getContigIDs() != null)
					getParentFrame().loadQueryContigs ( tab, tab.getQuery(), tab.getContigIDs(), tab.getViewMode(), null );
				else if(tab.getQuery() != null)
					getParentFrame().loadQueryFilter ( tab, tab.getQuery(), null );
			}
		}
	}
	
	/*************** Public *****************/
	// something is selected/deselected on column panel
	public void actionPerformed(ActionEvent e) {
		
	}
			
	public FieldMapper getMapper() { // called before query. mapObj is belongs to this query.
		setMapperFromSelected();
		FieldMapper mapObj = FieldContigData.createContigFieldMapper(theParentFrame);
		mapObj.setVisibleFieldNames(theMapper.getVisibleFieldNames());
		mapObj.setNFoldLibNames(theMapper.getNfoldLibNames());
		return mapObj;
	}
	
	public String [] getFoldColumns() { 
		if (nFoldObj==null) return null;
		return nFoldObj.getColumnNames(); 
	}
	public int getNFoldFieldID(String name) { 
		if (nFoldObj==null) return 0;
		return nFoldObj.getIDForName(name); 
	}
	
	private void clear() {
		if (nFoldObj!=null) nFoldObj.clear();
		
		theMapper.setVisibleFieldIDsList(null); // sets defaults
		setSelectedFromMapper();
		
		if(libListSelect != null) {
			for(int x=0; x<libListSelect.length; x++) {
				libListSelect[x].setSelected(false);
			}
		
			chkLibSelAll.setSelected(false);
			chkLibExpLevel.setSelected(false);
			chkLibNExpLevel.setSelected(true);
		}
	}
	public void close()
	{
		setMapperFromSelected();
		setPrefFromMapper();
		
		centerPanel = null;
	}
	/************ Classes *************/
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
	/****** Private *************/
	private JCheckBox [] ctgGeneralSelect = null;
	private JCheckBox [] ctgSetSelect = null;
	private JCheckBox [] pValSelect = null;
	private JCheckBox [] ctgRStatSelect = null;
	private JCheckBox [] ctgDBHitSelect = null;
	private JCheckBox [] ctgSNPORFSelect = null;

	//viewable column selection for libraries
	private JCheckBox [] libListSelect = null;
	
	//Library selection
	private JCheckBox chkLibSelAll = null;
	private JCheckBox chkLibExpLevel = null;
	private JCheckBox chkLibNExpLevel = null;
	
	private UIfieldNFold nFoldObj = null;
	private UIfieldBestHit bestObj = null;
	
	private JPanel centerPanel = null;
	
	private STCWFrame theParentFrame=null;
	private FieldMapper theMapper=null;
	private Preferences prefsRoot = null;
}
