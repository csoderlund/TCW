/**
 * Filter Pairs 
 */
package sng.viewer.panels.pairsTable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import sng.database.Globals;
import sng.util.RunQuery;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.Converters;
import util.methods.Static;
import util.ui.CollapsiblePanel;
import util.ui.UserPrompt;

public class PairQueryTab extends Tab 
{	
	private static final long serialVersionUID = 1127876634478104544L;
	private static final String HTML = "html/viewSingleTCW/PairFilter.html";
			
	public PairQueryTab ( STCWFrame inFrame, RunQuery inQuery ) throws Exception
	{
		super(inFrame, null); 
		
		theQuery = inQuery;
		tempPairsData = (FieldPairsData) Converters.deepCopy( theQuery.getPairsData() );
		createFilterPanel();
		enableFilters(false);
	}
	
	private void createFilterPanel() 
	{
		JPanel buttonPanel = createButtonPanel();
		JLabel lblTitle = new JLabel("Filters for Pairs");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		JTextArea txtDesc = new JTextArea("Only pairs that pass selected filters will be shown.");
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		txtDesc.setEditable(false);
		txtDesc.setMaximumSize(txtDesc.getPreferredSize()); // needed to prevent vertical stretching
		
		centerPanel = Static.createPagePanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		centerPanel.setAlignmentY(LEFT_ALIGNMENT);
		
		centerPanel.add(buttonPanel);
		centerPanel.add(Box.createVerticalStrut(20));
		centerPanel.add(lblTitle);
		centerPanel.add(txtDesc);
		centerPanel.add(Box.createVerticalStrut(20));

		searchPanel 	= createSearchFilter( );
		centerPanel.add( searchPanel );
		centerPanel.add(Box.createVerticalStrut(10));
		
		similarityPanel 	= createPairsFilters( );
		centerPanel.add( similarityPanel );
		
		lblError.setText( " " );
		lblError.setVisible(false);
		
		JScrollPane scroller = new JScrollPane ( centerPanel );
		scroller.setBorder( null );
		scroller.setPreferredSize(getParentFrame().getSize());
		scroller.getVerticalScrollBar().setUnitIncrement(15);
		
		setLayout(new BoxLayout ( this, BoxLayout.Y_AXIS )); // needed for left-justification!
		add( lblError );
		add(scroller);
	}
	private JComponent createSearchFilter () {
		JComponent thePanel = new CollapsiblePanel("Search", "Find pairs based on substring");
		JPanel row = Static.createRowPanel();
		chkFindSeqName = new JCheckBox ( "Seq ID   (substring)" );
		chkFindSeqName.setBackground(Color.WHITE);
		chkFindSeqName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableFilters(chkApplyFilters.isSelected());
				txtSeqName.setEnabled(chkFindSeqName.isSelected());
			}
		});
		txtSeqName = Static.createTextField("", 15); 
		chkFindSeqName.setSelected(false);
		txtSeqName.setEnabled(false);
		row.add(chkFindSeqName);
		row.add(txtSeqName);
		thePanel.add(row);
		
		row = Static.createRowPanel();
		chkFindType = new JCheckBox ( "Hit Type (substring)" );
		chkFindType.setBackground(Color.WHITE);
		chkFindType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableFilters(chkApplyFilters.isSelected());
				txtAlignType.setEnabled(chkFindType.isSelected());
			}
		});
		txtAlignType = Static.createTextField("", 10); 
		chkFindType.setSelected(false);
		txtAlignType.setEnabled(false);
		row.add(chkFindType);
		row.add(txtAlignType);
		thePanel.add(row);
		
		return thePanel;
	}
	private JComponent createPairsFilters ( )
	{	
		JComponent thePanel = new CollapsiblePanel("Similarity", "Based on dynamic programming");
		
		JPanel bar = null;
		chkApplyFilters = new JCheckBox ( "Sequence Pairs (check this box when setting filters - all filter must pass)" );
		chkApplyFilters.setBackground(Color.WHITE);
		chkApplyFilters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableFilters(chkApplyFilters.isSelected());
			}
		});
		thePanel.add( chkApplyFilters); thePanel.add(Box.createVerticalStrut(5));
		
		int fieldSize=4;
		txtNTPercentSim = Static.createTextField("40", fieldSize); 
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtNTPercentSim, 
				new JLabel ( "NT %Sim" ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(3));

		txtNTLen = Static.createTextField("100", fieldSize);
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtNTLen, 
				new JLabel ( "NT Align" ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(3));

		txtNTPercentLen = Static.createTextField("50", fieldSize); 
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtNTPercentLen, 
				new JLabel ( "NT %Len " ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(6));

		txtAAPercentSim = Static.createTextField("40", fieldSize);
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtAAPercentSim, 
				new JLabel ( "AA %Sim " ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(3));

		txtAALen = Static.createTextField("100", fieldSize);
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtAALen, 
				new JLabel ( "AA Align" ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(3));

		txtAAPercentLen = Static.createTextField("50", fieldSize); 
		bar = createFilterRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				new JLabel ( "Has >= " ), txtAAPercentLen, 
				new JLabel ( "AA %Len " ) );
		thePanel.add(bar); thePanel.add(Box.createVerticalStrut(5));

		return thePanel;
	}
	private JPanel createFilterRow ( Component comp1, Component comp2, Component comp3, Component comp4 )
	{
		JPanel panel = Static.createRowPanel();

		panel.add(comp1); panel.add( Box.createHorizontalStrut(5) );
		panel.add(comp2); panel.add( Box.createHorizontalStrut(5) );
		panel.add(comp3); panel.add( Box.createHorizontalStrut(5) );
		panel.add(comp4); panel.add( Box.createHorizontalStrut(5) );
		
		return panel;
	}
	
	private JPanel createButtonPanel() {
		JButton btnExpandAll = Static.createButton("Expand All");
		btnExpandAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				expandSections(true);
			}
		});
		
		JButton btnCollapseAll = Static.createButton("Collapse All");
		btnCollapseAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				expandSections(false);
			}
		});
		
		JButton btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), "Pairs Filters", HTML);
			}
		});

		JPanel buttonPanel = Static.createRowPanel();
		
		buttonPanel.add(btnExpandAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnCollapseAll);
		buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add(btnHelp);
		buttonPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)buttonPanel.getPreferredSize ().getHeight() ) );
		return buttonPanel;
	}
	// End panel setup
		
	private void expandSections(boolean expand) {	
		Component[] comps = centerPanel.getComponents();
		for (int i = 0;  i < centerPanel.getComponentCount();  i++) {		
			if (comps[i] instanceof CollapsiblePanel) {
				CollapsiblePanel c = (CollapsiblePanel)comps[i];
				if (expand) c.expand();
				else c.collapse();
			}
		}
	}

	/**********************************************
	 * Transfers filter information to FieldPairData for creating mySQL query.
	 */
	private void checkPairsInputFromUI ( ) throws DataValidationError 
	{		
		if(!chkApplyFilters.isSelected()) return;
		
		getAndValidateInteger ( txtNTLen );
		getAndValidatePercent ( txtNTPercentLen ); 
		getAndValidatePercent ( txtNTPercentSim ); 
		getAndValidateInteger( txtAALen );
		getAndValidatePercent ( txtAAPercentLen ); 
		getAndValidatePercent ( txtAAPercentSim ); 
	}
	
	/***************************************
	 * Query - called from STCWFrame
	 */
	public void executeQuery ( ) 
	{	
		lblError.setVisible(false); 
		lblError.setText( " " ); 
		if ( failure != null ) {
			if ( failure.getBorder() instanceof CompoundBorder ) {
				CompoundBorder errBorder = (CompoundBorder)failure.getBorder();
				failure.setBorder( errBorder.getInsideBorder() );
			}
			failure.setForeground( Color.BLACK );
			failure = null;
		}
		
		try {
			checkPairsInputFromUI ( ); 
		}
		catch ( DataValidationError err ) {
			handleDataValidationError ( err );
			return;
		}
		tempPairsData.setWhereClause(getSQLWhere());
		tempPairsData.setSummary(getSummary());
		theQuery.setPairsData(tempPairsData);
		theQuery.setType(RunQuery.QUERY_PAIRS);
				
		++nChildren;
		getParentFrame().addQueryResultsTab ( theQuery, "Filter" + nChildren ); 
	}
	private String getSQLWhere() {
		if (!chkApplyFilters.isSelected() && !chkFindSeqName.isSelected()  && !chkFindType.isSelected()) return "1";
		
		String where="";
		
		if (chkFindSeqName.isSelected()) {
			String id = txtSeqName.getText();
			where = "(contig1 like '%" + id + "%' or contig2 like '%" + id + "%')";
		}	
		if (chkFindType.isSelected()) {
			String id = txtAlignType.getText();
			where = "(hit_type like '%" + id + "%')";
		}
		if (chkApplyFilters.isSelected()) {
			if (where!="") where += " AND ";
			double ntRatio = Double.parseDouble(txtNTPercentLen.getText())/100.0;
			double aaRatio = Double.parseDouble(txtAAPercentLen.getText())/100.0;
			double ntPid  =  Double.parseDouble(txtNTPercentSim.getText())/100.0;
			double aaPid  =  Double.parseDouble(txtAAPercentSim.getText())/100.0;
			
			where =  "(NT_olp_len >= " + txtNTLen.getText();
			where += " AND NT_olp_ratio >= " + ntRatio;
			where += " AND (NT_olp_score/NT_olp_len) >= " + ntPid;	
			
			where += " AND AA_olp_len >= " + txtAALen.getText();
			where += " AND AA_olp_ratio >= " + aaRatio;
			where += " AND (AA_olp_score/AA_olp_len) >= " + aaPid + ")";
		}
		return where;
	}
	private String getSummary() {
		if (!chkApplyFilters.isSelected() && !chkFindSeqName.isSelected()  && !chkFindType.isSelected()) return "All pairs";
		
		String sum = "";
		if (chkFindSeqName.isSelected()) 
			sum = "SeqID1 or SeqID2 contains '" +  txtSeqName.getText() + "' ";
		if (chkFindType.isSelected()) 
			sum += "Hit Type contains '" +  txtAlignType.getText() + "' ";
		if (chkApplyFilters.isSelected()) 
		    sum +=  "NT %Sim >= " + txtNTPercentSim.getText() + 
				", NT Len >= " + txtNTLen.getText() +
				", NT %Len >= " + txtNTPercentLen.getText() +
				", AA %Sim >= " + txtAAPercentSim.getText() +
				", AA Len >= " + txtAALen.getText() +
				", AA %Len >= " + txtAAPercentLen.getText(); 
		return sum;
	}
	
	private void enableFilters(boolean b) {
		txtNTLen.setEnabled(b);
		txtNTPercentLen.setEnabled(b);
		txtNTPercentSim.setEnabled(b);
		
		txtAALen.setEnabled(b);
		txtAAPercentLen.setEnabled(b);
		txtAAPercentSim.setEnabled(b);
		
		tempPairsData.setWhereClause("1");
		tempPairsData.setSummary("All pairs");
	}
	private void handleDataValidationError ( DataValidationError err )
	{
		failure = err.getComponent();
		if (failure == null) return; 
		if ( failure instanceof JTextField )
		{
			Border in = failure.getBorder();
			Border out = BorderFactory.createLineBorder( Color.RED );
			failure.setBorder( BorderFactory.createCompoundBorder( out, in ) );
		}
		else
			failure.setForeground( Color.RED );
		failure.requestFocus();
		lblError.setText( err.getMessage() );
		lblError.setForeground( Color.RED );
		lblError.setVisible(true); 
	}
	
	private class DataValidationError extends Exception
	{
		DataValidationError ( JComponent inComponent, String inDescription )
		{
			super ( inDescription );
			theComponent = inComponent;
		}
		
		public JComponent getComponent () { return theComponent; }
		
		private JComponent theComponent = null; // The component that failed validation
	    private static final long serialVersionUID = 1;
	};
	
	private double getAndValidatePercent ( JTextField theField ) throws DataValidationError
	{
		try
		{
			double percent = Double.parseDouble( theField.getText() );
			if ( percent >= 0.0d && percent <= 100.0d )
				return percent /= 100.0d;
		}
		catch ( Exception err ) { }
		throw new DataValidationError ( theField, "The value entered is not a valid percent." );
	}
	
	private int getAndValidateInteger ( JTextField theField ) throws DataValidationError
	{
		try
		{
			int n = Integer.parseInt( theField.getText() );
			if ( n >= 0 ) return n;
		}
		catch ( Exception err ) { 
		}
		throw new DataValidationError ( theField, "The value entered is not a valid positive integer." );
	}

	public void close()
	{
		tempPairsData = null;
		similarityPanel = null;
		searchPanel = null;
	}
	public void restoreDefaults() {
		chkApplyFilters.setSelected(false);
	}

	private static final int SUBGROUP_INSET_WIDTH = 50;
	
	private JLabel lblError = new JLabel ( " " );
	private JComponent failure = null;
	private JComponent centerPanel = null;
	private JComponent similarityPanel = null;
	private JComponent searchPanel = null;
	
	// Similarity widgets
	private JCheckBox chkApplyFilters = null;
	
	private JTextField txtNTLen = null;
	private JTextField txtNTPercentLen = null;
	private JTextField txtNTPercentSim = null;
	
	private JTextField txtAALen = null;
	private JTextField txtAAPercentLen = null;
	private JTextField txtAAPercentSim = null;

	private JCheckBox chkFindSeqName = null;
	private JTextField txtSeqName = null;
	
	private JCheckBox chkFindType = null; // CAS314 add
	private JTextField txtAlignType = null;
	
	private int nChildren = 0;
	private RunQuery theQuery = null;
	private FieldPairsData tempPairsData = null;
}
