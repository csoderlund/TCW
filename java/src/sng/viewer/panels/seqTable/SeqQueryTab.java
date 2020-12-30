/**
 * Filter Contigs Query
 * One object is made in STCWFrame at startup
 * Filters are only on the contig and pja_db_unitrans_hits 
 * Filters on pja_db_unique_hits are in Basic Hit
 */
package sng.viewer.panels.seqTable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import sng.database.Globals;
import sng.database.MetaData;
import sng.util.RunQuery;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.Converters;
import util.methods.Static;
import util.ui.CollapsiblePanel;
import util.ui.ToggleTextComboField;
import util.ui.ToggleTextField;
import util.ui.UserPrompt;


public class SeqQueryTab extends Tab
{
	private static final long serialVersionUID = 4667335387617904461L;
	private final int nIntField = 6;
	private final static String HTML = "html/viewSingleTCW/SeqFilter.html";
	
	public SeqQueryTab ( STCWFrame inFrame, RunQuery inQuery ) throws Exception
	{
		super(inFrame, null); 
		metaData = inFrame.getMetaData();
		norm = metaData.getNorm();
		if (norm.contentEquals("TPM")) {
			LIBRARY_HEADER = "Counts and " + norm;
			LIBRARY_DESCRIPTION = "Filter sequences based on counts or " + norm;
		}
		JLabel test = new JLabel ( "99999999" );
		defaultLabelDims = test.getPreferredSize();
		test = new JLabel ("999999"); // size of number text box
		defaultNumDims = test.getPreferredSize();
		MIN_ROW_HEIGHT = (int)test.getPreferredSize().getHeight() + 5;
		
		theQuery = inQuery;
		tempContigData = (FieldSeqData) Converters.deepCopy( theQuery.getContigData() );
		
		createFilterPanel();
	}
	/**
	 * called from STCWFrame on openAssembly (startup). 
	 */
	public void createFilterPanel ( )
	{	
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout ( centerPanel, BoxLayout.Y_AXIS ));
		centerPanel.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		centerPanel.setBackground(Color.WHITE);
		centerPanel.setAlignmentY(LEFT_ALIGNMENT);
		
		JPanel buttonPanel = createButtonPanel();
		centerPanel.add( buttonPanel );
		centerPanel.add( Box.createVerticalStrut(10) );
		
		JLabel lblTitle = new JLabel("Filters for Sequences");
		Font f = lblTitle.getFont();
		lblTitle.setFont(new Font(f.getName(), f.getStyle(), f.getSize()+4));
		JLabel txtDesc = new JLabel("Only sequences that pass all filters will be shown.");
		txtDesc.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		txtDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		centerPanel.add( lblTitle );
		centerPanel.add( txtDesc );
		centerPanel.add( Box.createVerticalStrut(10) );
		
		contigPanel 	= createGeneralFilter ( ); 
		centerPanel.add( contigPanel);
		
		if(metaData.hasExpLevels()) {
			selLibraryPanel    = createSelectLibrary( );
			centerPanel.add( selLibraryPanel);
			
			nfoldPanel = createNfold();
			centerPanel.add( nfoldPanel);
		}
		if (metaData.hasDE()) {
			PValPanel = createSelectPVal();
			if(PValPanel != null) centerPanel.add( PValPanel );
		}
		bHasAnno=false;
		if (metaData.hasHits()) {
			uniprotPanel 	= createAnnoDBFilters (  );
			centerPanel.add( uniprotPanel);
	
			if (metaData.hasGOs()) {
				GOPanel 	= createGOFilters (  );
				centerPanel.add( GOPanel ); 
			}
			bHasAnno=true;
		}
		if (metaData.hasORFs()) {
			SNPandORFPanel 	= createSNPandORFFilters (  );
			if(SNPandORFPanel != null) {
				centerPanel.add( SNPandORFPanel );
			}
		}
		centerPanel.setMinimumSize(centerPanel.getPreferredSize());
		centerPanel.setMaximumSize(centerPanel.getPreferredSize());
		
		scroller = new JScrollPane ( centerPanel);
		scroller.setBorder( null );
		scroller.setPreferredSize(getParentFrame().getSize());
		scroller.getVerticalScrollBar().setUnitIncrement(15);
		
		lblError.setText( " " );
		lblError.setVisible(false);
		
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ));
		add( lblError );
		add( scroller );
	}
	
	// General
	private JComponent createGeneralFilter ( )
	{	
		JComponent thePanel = new CollapsiblePanel(GENERAL_HEADER, GENERAL_DESCRIPTION);
		
		if (metaData.hasAssembly()) {
		   chkNumAligns = new ToggleTextComboField(
				"", "1", " # Alignments", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		   addRowToPanel ( chkNumAligns, thePanel );
		}
		chkCtgLength = new ToggleTextComboField(
				"", "1", " Length", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		addRowToPanel ( chkCtgLength, thePanel );

		if(metaData.hasExpLevels()) {
			chkTotalExpLevel = new ToggleTextComboField(
					"", "1", " Total Read Count", BOOLEAN_LABELS, 1, defaultLabelDims, null);
			addRowToPanel ( chkTotalExpLevel, thePanel );
		}
		
		int width=100;
		if(metaData.hasNs()) {
			hasNQuery = new UIqueryIncEx("Has N's:", "Yes", "No", "Ignore",  2 , width);
			addRowToPanel( hasNQuery, thePanel );
		}
		hasRemarkQuery =  new UIqueryIncEx("Has User Remark:", "Yes", "No", "Ignore", 2, width);
		addRowToPanel( hasRemarkQuery, thePanel );
		
		if (metaData.hasLoc()) {
			hasLocQuery =    new UIqueryIncEx("Has a Location:", "Yes", "No", "Ignore", 2, width);
			addRowToPanel( hasLocQuery, thePanel );
			chkLocN = new ToggleTextField("N group", "0", "", nIntField, null);
			chkLocStart = new ToggleTextField("Start ", "0", "",  nIntField, null);
			chkLocEnd = new ToggleTextField("End ", "", "",  nIntField, null);
			JPanel row = Static.createRowPanel();
			
			row.add(chkLocN); 
			row.add(chkLocStart); 
			row.add(chkLocEnd);
			addRowToPanel(row, thePanel);
		}
		
		return thePanel;
	}
	// Library
	private JComponent createSelectLibrary( ) {
		libColumns = metaData.getLibNames();
		if (libColumns==null || libColumns.length==0) return null;
		libTitles = metaData.getLibTitles();
		
		JComponent thePanel = new CollapsiblePanel(LIBRARY_HEADER, LIBRARY_DESCRIPTION);
		String [] libLabels = {"EVERY", "ANY"};
		
		chkCounts = new JRadioButton("use counts");
		chkCounts.setBackground(Color.WHITE);
		
		String ppx = "use  " + norm + " ";
		chkRPKM = new JRadioButton(ppx);
		chkRPKM.setBackground(Color.WHITE);
		chkCounts.addActionListener(btnListener);
		chkRPKM.addActionListener(btnListener);
		
		ButtonGroup libModeGroup = new ButtonGroup();
		libModeGroup.add(chkCounts);
		libModeGroup.add(chkRPKM);
		chkRPKM.setSelected(true);
		
		JPanel libModePanel = Static.createRowPanel();
		libModePanel.add(chkCounts);
		libModePanel.add(Box.createHorizontalStrut(20));
		libModePanel.add(chkRPKM);
		libModePanel.setMaximumSize(libModePanel.getPreferredSize());
		addRowToPanel(libModePanel, thePanel);
		
		incLibQuery = new UIqueryIncExLib(
				"At least   ", 1, " from ", libLabels, " included condition",  
				0, defaultNumDims, filterLibListener);
		incLibQuery.setSelected(false); 
		
		exLibQuery = new UIqueryIncExLib(
				"At most   ", 0, " from ", libLabels, " excluded condition", 
				0, defaultNumDims, filterLibListener);
		exLibQuery.setSelected(false); 
		
		thePanel.add(Box.createVerticalStrut(5));
		addRowToPanel(incLibQuery, thePanel);
		thePanel.add(Box.createVerticalStrut(5));
		addRowToPanel(exLibQuery, thePanel);
		thePanel.add(Box.createVerticalStrut(10));
		
		addRowToPanel(new JLabel("Include Conditions"), thePanel);
		
		chkIncludeLibs = new JCheckBox[libColumns.length];
		labelIncLibs = new JLabel[libColumns.length];
		
		for(int x=0; x<chkIncludeLibs.length; x++) {
			chkIncludeLibs[x] = new JCheckBox(libColumns[x]);
			
			chkIncludeLibs[x].setSelected(false);
			chkIncludeLibs[x].setBackground(Color.WHITE);
			chkIncludeLibs[x].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					libSelAll.setSelected(areAllIncLibsSelected());
					updateUnCheckList();
				}
			});
			
			JPanel row = Static.createRowPanel();
			
			JPanel chk = Static.createRowPanel();
			chk.add(chkIncludeLibs[x]);
			chk.add(Box.createHorizontalGlue());
			chk.setMinimumSize(new Dimension(160,chk.getPreferredSize().height));
			row.add(chk);
			
			row.add(Box.createHorizontalStrut(20));
			labelIncLibs[x] = new JLabel(libTitles[x]);
			row.add(labelIncLibs[x]);
			
			addRowToPanel(row, thePanel);
		}

		thePanel.add(new JSeparator());
		libSelAll = new JCheckBox("Check/uncheck all");
		libSelAll.setSelected(true);
		libSelAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectAllIncLibsClicked();
			}
		});
		addRowToPanel(libSelAll, thePanel);
		thePanel.add(new JSeparator());
		
		thePanel.add(Box.createVerticalStrut(10));
		addRowToPanel(new JLabel("Exclude Conditions"), thePanel);

		chkExcludeLibs = new JCheckBox[libColumns.length];
		labelExLibs = new JLabel[libColumns.length];
		for(int x=0; x<chkExcludeLibs.length; x++) { 
			chkExcludeLibs[x] = new JCheckBox(libColumns[x]);
			chkExcludeLibs[x].setBackground(Color.WHITE);
			chkExcludeLibs[x].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					libCheckUnChecked.setSelected(areExCheckedForIncUnChecked());
				}
			});
			
			JPanel row = Static.createRowPanel();
		
			JPanel chk = new JPanel();
			chk.setBackground(Color.white);
			chk.setLayout(new BoxLayout(chk, BoxLayout.LINE_AXIS));
			chk.add(chkExcludeLibs[x]);
			chk.add(Box.createHorizontalGlue());
			chk.setMinimumSize(new Dimension(160,chk.getPreferredSize().height));
			row.add(chk);
			
			row.add(Box.createHorizontalStrut(20));
			labelExLibs[x] = new JLabel(libTitles[x]);
			row.add(labelExLibs[x]);
		
			addRowToPanel(row, thePanel);
		}
		thePanel.add(new JSeparator());
		libCheckUnChecked = new JCheckBox("check all not included above");
		libCheckUnChecked.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectUncheckedClicked();
			}
		});
		addRowToPanel(libCheckUnChecked, thePanel);
		thePanel.add(new JSeparator());
		thePanel.add(Box.createVerticalStrut(10));
		
		thePanel.add(Box.createVerticalStrut(5));

		updateUnCheckList();
		setLibEnabled(false);
		return thePanel;
	}
	private JComponent createNfold() {
		JComponent thePanel = new CollapsiblePanel(NFOLD_HEADER, NFOLD_DESCRIPTION);
		nFoldObj = new UIfieldNFold(chkIncludeLibs, true);
		thePanel.add(nFoldObj);
		return thePanel;
	}
	// Differential Expression
	private JComponent createSelectPVal(  ) {
		pValColumns = metaData.getDENames();
		if (pValColumns==null || pValColumns.length==0) return null;
		pValTitles = metaData.getDETitles();
		
		int pLen=pValColumns.length;
		boolean hasUpDown = true; // databases before 2013 did not distinquish up/down. Now we use Negative values
		
		JComponent thePanel = new CollapsiblePanel(PVAL_HEADER, PVAL_DESCRIPTION);
		chkFilterPVal = new JCheckBox();
		chkFilterPVal.addItemListener(filterPvalListener);
		
		cmbBoolPVal = new JComboBox <String> ();
		cmbBoolPVal.setBackground(Color.WHITE);
		cmbBoolPVal.addItem("EVERY");
		cmbBoolPVal.addItem("ANY");
		cmbBoolPVal.setSelectedIndex(0);
		cmbBoolPVal.setMaximumSize(cmbBoolPVal.getPreferredSize());
		cmbBoolPVal.setMinimumSize(cmbBoolPVal.getPreferredSize());
		
		JLabel lblPVal = new JLabel("p-value <");
		lblPVal.setBackground(Color.WHITE);
		txtPVal = new JTextField(4);
		txtPVal.setText("0.05");
		txtPVal.setMaximumSize(txtPVal.getPreferredSize());
		txtPVal.setMinimumSize(txtPVal.getPreferredSize());
		
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
		row.setBackground(Color.WHITE);
		
		row.add(chkFilterPVal);
		row.add(Box.createHorizontalStrut(5));
		row.add(lblPVal);
		row.add(txtPVal);
		row.add(Box.createHorizontalStrut(3));
		row.add(new JLabel("from"));
		row.add(Box.createHorizontalStrut(3));
		row.add(cmbBoolPVal);
		row.add(new JLabel("selected DE column"));
		
		addRowToPanel(row, thePanel);
		thePanel.add(Box.createVerticalStrut(10));
		
		row = new JPanel();
		row.setBackground(Color.white);
		row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
		row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("Select one or more DE columns"));
		addRowToPanel(row, thePanel);
		
		colSelectPVal = new JCheckBox[pLen];
		colUpDown = new JRadioButton[pLen];
		colUpOnly = new JRadioButton[pLen];
		colDownOnly = new JRadioButton[pLen];
		colpValTitle = new JLabel[pLen];
		for(int x=0; x<pLen; x++) {
			row = new JPanel();
			row.setBackground(Color.white);
			row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
			
			JPanel sr = new JPanel();
			sr.setBackground(Color.white);
			sr.setLayout(new BoxLayout(sr, BoxLayout.LINE_AXIS));
		
			colSelectPVal[x] = new JCheckBox(pValColumns[x]);
			
			sr.add(colSelectPVal[x]);
			sr.add(Box.createHorizontalGlue());
			sr.setMinimumSize(new Dimension(160,sr.getPreferredSize().height));
			row.add(sr);
			
			colUpDown[x] = new JRadioButton("Either"); colUpDown[x].setBackground(Color.white);
			colUpOnly[x] = new JRadioButton("Up"); colUpOnly[x].setBackground(Color.white);
			colDownOnly[x] = new JRadioButton("Down"); colDownOnly[x].setBackground(Color.white);
			ButtonGroup bg2 = new ButtonGroup();
			bg2.add(colUpDown[x]);
			bg2.add(colDownOnly[x]);
			bg2.add(colUpOnly[x]);
			colUpDown[x].setSelected(true);
			
			if (hasUpDown)
			{
				row.add(colUpOnly[x]);
				row.add(Box.createHorizontalStrut(5));
				row.add(colDownOnly[x]);
				row.add(Box.createHorizontalStrut(5));
				row.add(colUpDown[x]);
			}
			row.add(Box.createHorizontalStrut(30));
			colpValTitle[x] = new JLabel(pValTitles[x]);
			row.add(colpValTitle[x]); 
			addRowToPanel(row, thePanel);
		}
		
		row = new JPanel();
		row.setBackground(Color.white);
		row.setLayout(new BoxLayout(row, BoxLayout.LINE_AXIS));
		JPanel sr = new JPanel();
		sr.setBackground(Color.white);
		sr.setLayout(new BoxLayout(sr, BoxLayout.LINE_AXIS));
		chkAllPVal = new JCheckBox("Check/uncheck all"); 
		chkAllPVal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectPValClicked();
			}
		});
		
		sr.add(chkAllPVal);
		sr.add(Box.createHorizontalGlue());
		sr.setMinimumSize(new Dimension(160,sr.getPreferredSize().height));
		if (pLen>1) row.add(sr);
		
		chkUpDownAll = new JRadioButton("Either"); chkUpDownAll.setBackground(Color.white);
		chkUpDownAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectPValUpDown(false, false, true);
			}
		});
		chkUpOnlyAll = new JRadioButton("Up"); chkUpOnlyAll.setBackground(Color.white);
		chkUpOnlyAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectPValUpDown(true, false, false);
			}
		});
		chkDownOnlyAll = new JRadioButton("Down"); chkDownOnlyAll.setBackground(Color.white);
		chkDownOnlyAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectPValUpDown(false, true, false);
			}
		});
		
		ButtonGroup allbg = new ButtonGroup();
		allbg.add(chkUpDownAll); 
		allbg.add(chkUpOnlyAll); 
		allbg.add(chkDownOnlyAll);
		chkUpDownAll.setSelected(true);
	    
		if (hasUpDown && pLen>1) {
			row.add(chkUpOnlyAll);
			row.add(Box.createHorizontalStrut(5));
			row.add(chkDownOnlyAll);
			row.add(Box.createHorizontalStrut(5));
			row.add(chkUpDownAll);
			thePanel.add(Box.createVerticalStrut(5));
			addRowToPanel(row, thePanel);
		}
		
		setPvalEnabled(false);
		
		return thePanel;
	}
	// Annotation
	private JComponent createAnnoDBFilters ( )
	{
		// High confidence UniProt hit rows
		JComponent thePanel = new CollapsiblePanel(BEST_DB_HIT_HEADER, BEST_DB_HIT_DESCRIPTION); 

		// Contig has best hit
		JPanel annoModes = Static.createRowPanel();
		
		chkHasBestDBHit = Static.createRadioButton("Annotated", hasAnnotation());
		chkHasBestDBHit.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				setBestHitEnabled(true);
			}
		});
		ButtonGroup annoGroup = new ButtonGroup();
		annoGroup.add(chkHasBestDBHit);
		annoModes.add(chkHasBestDBHit);
		annoModes.add(Box.createHorizontalStrut(5));
		
		chkNoAnno = Static.createRadioButton("No annotation", false);
		chkNoAnno.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				setBestHitEnabled(false);
			}
		});
		annoGroup.add(chkNoAnno);
		annoModes.add(chkNoAnno);
		annoModes.add(Box.createHorizontalStrut(5));
		
		chkEitherAnno = Static.createRadioButton("Don't care", true);
		chkEitherAnno.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				setBestHitEnabled(false);
			}
		});
		annoGroup.add(chkEitherAnno);
		annoModes.add(chkEitherAnno);
		addRowToPanel(annoModes, thePanel);

		if (!metaData.hasGOs())
			cmbFirstBest = new UIqueryIncEx("  Search", "Best Eval", "Best Anno", null, 0, 0);
		else 
			cmbFirstBest = new UIqueryIncEx("  Search", "Best Eval", "Best Anno", "Best With GO",  0, 0);
		addRowToPanel(cmbFirstBest, thePanel);
		
		cmbDBHitAndOr = new UIqueryIncEx( "", "AND", "OR", null, FieldSeqData.FILTER_AND, 0 );
		JPanel bar5 = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), cmbDBHitAndOr );
		addRowToPanel( bar5, thePanel );
		
		JPanel bar;
		// EVal
		chkDBHitEVal = new ToggleTextComboField(
				"Has ", "1E-30"," E-value", BOOLEAN_LABELS, 0, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), 
				chkDBHitEVal );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
		
		chkDBHitIdent = new ToggleTextComboField(
				"Has ", "60", " % Similarity", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), chkDBHitIdent );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
				
		thePanel.add(Box.createVerticalStrut(5));
		chkDBHitCov = new ToggleTextComboField(
				"Has ", "95", " % Coverage of hit", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), chkDBHitCov );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
		
		chkDBSeqCov = new ToggleTextComboField(
				"Has ", "50"," % Coverage of sequence", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), chkDBSeqCov );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));

		chkDBtype = new ToggleTextComboField(
				"Has ", null, "DBtype", metaData.getTypeDBs(), 0, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), chkDBtype );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
		
		chkDBtaxo = new ToggleTextComboField(
				"Has ", null, "Taxonomy", metaData.getTaxoDBs(), 0, defaultLabelDims, null);
		bar = addTwoToRow ( Box.createHorizontalStrut(SUBGROUP_INSET_WIDTH), chkDBtaxo );
		addRowToPanel ( bar, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
		
		chkNumAnno = new ToggleTextComboField(
				"General: ", "1", " #annoDBs", BOOLEAN_LABELS, 1, defaultLabelDims, null);
		addRowToPanel ( chkNumAnno, thePanel );
		
		setBestHitEnabled(chkHasBestDBHit.isSelected());
		
		return thePanel;
	}
	// GO
	private JComponent createGOFilters (  ) {
		if(!metaData.hasGOs())
			return null;
		
		JComponent thePanel = new CollapsiblePanel(GO_HEADER, GO_DESCRIPTION); 

		chkGOID = new ToggleTextField("GO term number ", "", "", 10, null);
		
		addRowToPanel(chkGOID, thePanel);
		return thePanel;
	}
	// ORFs and SNPS
	private JComponent createSNPandORFFilters (  )
	{	
		JComponent thePanel = new CollapsiblePanel(SNP_ORF_HEADER, SNP_ORF_DESCRIPTION); 
		
		if(metaData.hasAssembly()) {
			// Has >= SNPs
			chkGreaterEqualSNPs = new ToggleTextField(
					"Has >= ", "1", " SNPs",  nIntField, null);
			addRowToPanel ( chkGreaterEqualSNPs, thePanel );
			
			// Has <= SNPs
			chkLessEqualSNPs = new ToggleTextField(
					"Has <= ", "1", " SNPs",  nIntField, null);	
			addRowToPanel ( chkLessEqualSNPs, thePanel );
		}
		
		chkHasORF = new ToggleTextField(
				"Has ORF  >= ", "300", " NT",  nIntField, null);
		addRowToPanel ( chkHasORF, thePanel );

		// Both UTRs
		chkHasProteinORF = new JCheckBox ( "Protein confirmation (Is PR Frame='Yes')" );
		chkHasProteinORF.setSelected( false);
		chkHasProteinORF.setBackground(Color.WHITE);
		addRowToPanel ( chkHasProteinORF, thePanel );	
		
		// Both UTRs
		chkHasBothUTRs = new JCheckBox ( "Has a start and stop codon" );
		chkHasBothUTRs.setSelected( false);
		chkHasBothUTRs.setBackground(Color.WHITE);
		addRowToPanel ( chkHasBothUTRs, thePanel );		
	
		chkFrameORF = new ToggleTextField(
				"Frame = ", "", " ",  nIntField, null);
		addRowToPanel ( chkFrameORF, thePanel );
		thePanel.add(Box.createVerticalStrut(5));
		return thePanel;
	}
	/*************************************
	 * Collapse buttons
	 */
	private JPanel createButtonPanel() {
		JButton btnExpandAll = new JButton("Expand All");
		btnExpandAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				expandAll(true);
			}
		});
		
		JButton btnCollapseAll = new JButton("Collapse All");
		btnCollapseAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				expandAll(false);
			}
		});
		
		JButton btnRestore = new JButton("Clear");
		btnRestore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				clear();
			}
		});
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), 
						"Sequence Filters", HTML);
			}
		});

		JPanel buttonPanel = Static.createRowPanel();
		
		buttonPanel.add(btnExpandAll); 	buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnCollapseAll); buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(btnRestore);		buttonPanel.add(Box.createHorizontalStrut(5));
		
		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add(btnHelp);
		
		buttonPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)buttonPanel.getPreferredSize ().getHeight() ) );
		return buttonPanel;
	}
	
	/*****************************************************************
	 * XXX Listeners
	 */
	private ActionListener btnListener = new ActionListener() {
		public void actionPerformed(ActionEvent e)
		{
		}
	};
	private ItemListener filterPvalListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			setPvalEnabled(chkFilterPVal.isSelected());
		}
	};
	private ItemListener filterLibListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			setLibEnabled(incLibQuery.isSelected() || exLibQuery.isSelected());
		}
	};
	
	private String [] getSelectedIncludeLibs() {
		if (chkIncludeLibs==null) return null;
		
		if (!incLibQuery.isSelected()) return null;
		
		int numResults = 0;
		for(int x=0; x<chkIncludeLibs.length; x++)
			if(chkIncludeLibs[x].isSelected())
				numResults++;
		
		String [] retVal = new String [numResults];
		if(numResults == 0) return retVal;
			
		int pos = 0;
		for(int x=0; x<chkIncludeLibs.length; x++)
			if(chkIncludeLibs[x].isSelected()) {
				retVal[pos++] = chkIncludeLibs[x].getText();
			}
		return retVal;
	}
	
	private String [] getSelectedExcludeLibs() {
		if (chkExcludeLibs==null) return null;
		if (!exLibQuery.isSelected()) return null;
		
		int numResults = 0;
		for(int x=0; x<chkExcludeLibs.length; x++)
			if(chkExcludeLibs[x].isSelected())
				numResults++;
		
		String [] retVal = new String [numResults];
		if(numResults == 0) return retVal;
		
		int pos = 0;
		for(int x=0; x<chkExcludeLibs.length; x++)
			if(chkExcludeLibs[x].isSelected())
				retVal[pos++] = chkExcludeLibs[x].getText();
		
		return retVal;
	}
	
	private String [] getSelectedPValColumns() {
		Vector<String> theColumns = new Vector<String> ();
		if(colSelectPVal != null) {
			for(int x=0; x<colSelectPVal.length; x++) {
				if(colSelectPVal[x].isSelected())
					theColumns.add(colSelectPVal[x].getText());
			}
		}
		return theColumns.toArray(new String[0]);
	}
	private boolean [] getSelectedUpColumns() {
		Vector<Boolean> theColumns = new Vector<Boolean> ();
		if(colSelectPVal != null) {
			for(int x=0; x<colSelectPVal.length; x++) {
				if(colSelectPVal[x].isSelected())
					theColumns.add(colUpOnly[x].isSelected());
			}
		}
		boolean[] ret = new boolean[theColumns.size()];
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = theColumns.get(i);
		}
		return ret;
	}	
	private boolean [] getSelectedDownColumns() {
		Vector<Boolean> theColumns = new Vector<Boolean> ();
		if(colSelectPVal != null) {
			for(int x=0; x<colSelectPVal.length; x++) {
				if(colSelectPVal[x].isSelected())
					theColumns.add(colDownOnly[x].isSelected());
			}
		}
		boolean[] ret = new boolean[theColumns.size()];
		for (int i = 0; i < ret.length; i++)
		{
			ret[i] = theColumns.get(i);
		}
		return ret;
	}	
	
	private void selectAllIncLibsClicked() {
		if (libSelAll==null || chkIncludeLibs==null) return;
		boolean selected = libSelAll.isSelected();
		for(int x=0; x<chkIncludeLibs.length; x++)
			chkIncludeLibs[x].setSelected(selected);
	}
	
	private boolean areAllIncLibsSelected() {
		if (chkIncludeLibs==null) return false;
		boolean retVal = true;
		for(int x=0; x<chkIncludeLibs.length && retVal; x++) 
			retVal &= chkIncludeLibs[x].isSelected();
		return retVal;
	}
	
	private void selectUncheckedClicked() {
		if (chkIncludeLibs==null) return;
		for(int x=0; x<chkIncludeLibs.length; x++) {
			if(libCheckUnChecked.isSelected())
				chkExcludeLibs[x].setSelected(!chkIncludeLibs[x].isSelected());
			else
				chkExcludeLibs[x].setSelected(false);
		}
	}
	private void selectPValClicked() {
		if (colSelectPVal==null) return;
		boolean selected = chkAllPVal.isSelected();
		for (int x=0; x<colSelectPVal.length; x++)
			colSelectPVal[x].setSelected(selected);
	}
	private void selectPValUpDown(boolean up, boolean down, boolean either) {
		if (colSelectPVal==null) return;
		
		for (int x=0; x<colSelectPVal.length; x++) {
			colUpDown[x].setSelected(either);
			colDownOnly[x].setSelected(down);
			colUpOnly[x].setSelected(up);
		}
	}
	private boolean areExCheckedForIncUnChecked() {
		if (chkIncludeLibs==null) return false;
		boolean retVal = true;
		
		for(int x=0; x<chkIncludeLibs.length && retVal; x++)
			retVal &= (chkIncludeLibs[x].isSelected() != chkExcludeLibs[x].isSelected());
		return retVal;
	}
	
	private void updateUnCheckList() {
		if (libCheckUnChecked==null) return;
		if(libCheckUnChecked.isSelected())
			for(int x=0; x<chkIncludeLibs.length; x++)
				chkExcludeLibs[x].setSelected(!chkIncludeLibs[x].isSelected());
	}
	
	private void setLibEnabled(boolean enabled) {
		if (chkIncludeLibs==null) return;
		for(int x=0; x<chkIncludeLibs.length; x++) {
			chkIncludeLibs[x].setEnabled(enabled);
			labelIncLibs[x].setEnabled(enabled);
		}
		for(int x=0; x<chkExcludeLibs.length; x++) {
			chkExcludeLibs[x].setEnabled(enabled);
			labelExLibs[x].setEnabled(enabled);
		}
		libSelAll.setEnabled(enabled);
		libCheckUnChecked.setEnabled(enabled);
	}
	private void setPvalEnabled(boolean enabled) {
		if (colSelectPVal==null) return;
		
		for (int x=0; x<colSelectPVal.length; x++) {
			colSelectPVal[x].setEnabled(enabled);
			colUpDown[x].setEnabled(enabled);
			colDownOnly[x].setEnabled(enabled);
			colUpOnly[x].setEnabled(enabled);
			colpValTitle[x].setEnabled(enabled);
		}
		chkAllPVal.setEnabled(enabled);
		chkUpDownAll.setEnabled(enabled);
		chkDownOnlyAll.setEnabled(enabled);
		chkUpOnlyAll.setEnabled(enabled);
		txtPVal.setEnabled(enabled);
	}
	private void setBestHitEnabled(boolean enabled) {
		if (cmbFirstBest==null) return;
		
		cmbFirstBest.setEnabled(enabled);
		cmbDBHitAndOr.setEnabled(enabled);
		chkDBHitCov.setEnabled(enabled);
		chkDBSeqCov.setEnabled(enabled);
		chkDBHitEVal.setEnabled(enabled);
		chkDBHitIdent.setEnabled(enabled);
		chkDBtype.setEnabled(enabled);
		chkDBtaxo.setEnabled(enabled);
		chkNumAnno.setEnabled(enabled);
	}
	
	/********************************************************
	 * XXX Shared misc
	 */
	private void expandAll(boolean expand) {	
		Component[] comps = centerPanel.getComponents();
		for (int i = 0;  i < centerPanel.getComponentCount();  i++) {		
			if (comps[i] instanceof CollapsiblePanel) {
				CollapsiblePanel c = (CollapsiblePanel)comps[i];
				if (expand) c.expand();
				else c.collapse();
			}
		}
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
	
	private double getAndValidateDouble ( ToggleTextComboField theField, String name ) throws DataValidationError
	{
		try
		{
			double n = Double.parseDouble( theField.getText() );
			if ( n >= 0.0d )
				return n;
		}
		catch ( Exception err ) { }
		throw new DataValidationError ( theField, "The " + name + " value entered is not a valid double." );
	}
	
	private int getAndValidateInteger ( ToggleTextField theField, boolean isPos, String name ) throws DataValidationError
	{
		try
		{
			int n = Integer.parseInt( theField.getText() );
			if (isPos &&  n >= 0 ) return n;
			if (!isPos) return n;
		}
		catch ( Exception err ) { }
		throw new DataValidationError ( theField, "The " + name + " value entered is not a valid positive integer." );
	}
	
	private int getAndValidateInteger ( ToggleTextComboField theField, String name ) throws DataValidationError
	{
		try
		{
			int n = Integer.parseInt( theField.getText() );
			if ( n >= 0 ) return n;
		}
		catch ( Exception err ) { }
		throw new DataValidationError ( theField, "The " + name + " value entered is not a valid positive integer." );
	}
	private String getAndValidateString ( ToggleTextField theField, String name ) throws DataValidationError
	{
		String x = theField.getText();
		if (!x.equals("")) return x;
		throw new DataValidationError ( theField, "The " + name + " string is empty." );
	}
	
	private JPanel addTwoToRow ( Component comp1, Component comp2 )
	{
		JPanel panel = new JPanel ();
		panel.setLayout( new BoxLayout ( panel, BoxLayout.X_AXIS ) );
		panel.setBackground(Color.WHITE);
		
		panel.add( comp1 );
		panel.add( Box.createHorizontalStrut(5) );
		
		panel.add( comp2 );
		panel.add( Box.createHorizontalStrut(5) );
		
		return panel;
	}
	private void addRowToPanel ( JComponent row, JComponent toPanel )
	{
		int nWidth = (int)row.getMinimumSize().getWidth();
		int nHeight = Math.max( (int)row.getPreferredSize().getHeight(), MIN_ROW_HEIGHT );
		row.setAlignmentX ( Component.LEFT_ALIGNMENT );
		row.setMinimumSize(   new Dimension ( nWidth, nHeight ) );
		row.setPreferredSize( new Dimension ( nWidth, nHeight ) );
		row.setMaximumSize(   new Dimension ( nWidth, nHeight ) );
		toPanel.add( row );
	}
	
	/***************** Public methods *****************/
	public String [] getAllLibraryNames() {return libColumns; }
	public String [] getAllLibraryTitles() {return libTitles;}
	public String [] getAllPValNames() {return pValColumns;}
	public String [] getAllPValTitles() {return pValTitles;}
	
	public void executeQuery ( String tag, String [] contigIDs, int viewMode ) 
	{	
		theQuery.setType(RunQuery.QUERY_CONTIGS);
		getParentFrame().addQueryResultsTab(theQuery, contigIDs, viewMode, tag);
	}
	public void executeQuery ( ) 
	{
		lblError.setVisible(false); 
		lblError.setText( " " ); // Clear out the existing error if any
		if ( failure != null ) {
			if ( failure.getBorder() instanceof CompoundBorder ) {
				CompoundBorder errBorder = (CompoundBorder)failure.getBorder();
				failure.setBorder( errBorder.getInsideBorder() );
			}
			failure.setForeground( Color.BLACK );
			failure = null;
		}
		String where="";
		try {
			where = getWHEREClause(); // does data validation as building where clause
		}
		catch ( DataValidationError err ) {
			handleDataValidationError ( err );
			UserPrompt.showWarn("See red warning on top of Filter page");
			return;
		}	
		// pre-June16 was passing each piece of filter. Now just passes where clause.
		tempContigData.setWhere(where);
		tempContigData.setSummary(summary);
		theQuery.setContigData(tempContigData);
			
		theQuery.setType(RunQuery.QUERY_CONTIGS);
				
		++nChildren;
		getParentFrame().addQueryResultsTab ( theQuery, "Filter" + nChildren ); 
	}
	// this merges both the Query Include/Exclude and the Columns Nfold
	public String [] getAllRequiredLibs(FieldSeqTab fTab) { 
		Vector<String> retVal = new Vector<String> ();
		if (!metaData.hasExpLevels()) return retVal.toArray(new String[0]);
		
		String [] selectedIncLibs = getSelectedIncludeLibs();
		if(selectedIncLibs != null) {
			for(int x=0; x<selectedIncLibs.length; x++)
				if(!retVal.contains(selectedIncLibs[x]))
					retVal.add(selectedIncLibs[x]);
		}
	
		String [] selectedExLibs = getSelectedExcludeLibs();
		if(selectedExLibs != null) {
			for(int x=0; x<selectedExLibs.length; x++)
				if(!retVal.contains(selectedExLibs[x]))
					retVal.add(selectedExLibs[x]);
		}
	
		String [] foldCols = fTab.getFoldColumns();
		for(int x=0; x<foldCols.length; x++) {
			String [] vals = foldCols[x].split("/");
			if (vals!=null && vals.length==2) {
				if(!retVal.contains(vals[0])) retVal.add(vals[0]);
				if(!retVal.contains(vals[1])) retVal.add(vals[1]);
			}
		}
	
		return retVal.toArray(new String[0]);
	}
	public boolean usesCounts() { return chkCounts!=null && chkCounts.isSelected();}
	public String getGOterm () {
		if (chkGOID!=null && chkGOID.isSelected()) return chkGOID.getText();
		else return null;
	}
	public boolean hasAnnotation() { 
		if (chkHasBestDBHit==null) return false;
		return chkHasBestDBHit.isSelected();
	}
	
	public void close()
	{
		defaultLabelDims = null;
		defaultNumDims = null;
		uniprotPanel = null;
		selLibraryPanel = null;
		nfoldPanel = null;
		contigPanel = null;
		SNPandORFPanel = null;
		GOPanel = null;
	}
	
	/*************************************************
	 * XXX Build where clause
	 */
	private String summary;
	private final int FILTER_OR = 0;
	private final int FILTER_AND = 1;
	private final int FILTER_INCLUDE = FILTER_AND; 
	private final int FILTER_EXCLUDE = FILTER_OR;
	
	public String getSummary () { return summary; }
	
	public String getWHEREClause ( ) throws DataValidationError 
	{	
    		summary="";
		String strSQL = "";	
		int inc;
	
		// General	
		int nCtgLen = (chkCtgLength.isSelected()) ? 
				getAndValidateInteger( chkCtgLength , "Length") : 0;
		if ( nCtgLen>1 ) {
			String boolOp;
			if(chkCtgLength.getOptionSelection() == 0) boolOp = " <= ";
			else boolOp = " >= ";
			strSQL = appendANDPredicate ( strSQL, "contig.consensus_bases" + boolOp + nCtgLen );
			joinSum("Len" + boolOp + nCtgLen);
		}
		
		int nNumAlign = (chkNumAligns!=null && chkNumAligns.isSelected()) ?
				getAndValidateInteger( chkNumAligns, "Num Align" ) : 0;
		if ( nNumAlign>1) {
			String boolOp;
			if(chkNumAligns.getOptionSelection() == 0) boolOp = " <= ";
			else boolOp = " >= ";
			strSQL = appendANDPredicate ( strSQL, "contig.numclones" + boolOp + nNumAlign );
			joinSum("#Align" + boolOp + nNumAlign); 
		}
		
		int nTotalExpLevel = (chkTotalExpLevel!=null && chkTotalExpLevel.isSelected()) ?
			getAndValidateInteger( chkTotalExpLevel, "Total Expression" ) : -1;
		if ( nTotalExpLevel!=-1 ) {
			String boolOp;
			if (chkTotalExpLevel.getOptionSelection() == 0) boolOp = " <= ";
			else boolOp = " >= ";
			strSQL = appendANDPredicate ( strSQL, "contig.totalexp" + boolOp + nTotalExpLevel );
			joinSum("Total Exp" + boolOp + nTotalExpLevel);
		}
		
		if (metaData.hasNs()) {
			inc = hasNQuery.getValue();
			if ( inc == FILTER_INCLUDE ) {
				strSQL = appendANDPredicate ( strSQL, "contig.cnt_ns>0" );
				joinSum("Ns>0");
			}
			else if ( inc == FILTER_EXCLUDE ) {
				strSQL = appendANDPredicate ( strSQL, "contig.cnt_ns=0" );
				joinSum("Ns=0");
			}
		}
		
		inc = hasRemarkQuery.getValue();
		if ( inc == FILTER_INCLUDE ) {
			strSQL = appendANDPredicate ( strSQL, 
					"(contig.user_notes IS NOT NULL AND contig.user_notes != \"\")" );
			joinSum("Has User Remarks");
		}
		else if ( inc == FILTER_EXCLUDE ) {
			strSQL = appendANDPredicate ( strSQL, 
					"(contig.user_notes IS NULL or contig.user_notes='')");
			joinSum("No User Remarks");
		}
		
		if (metaData.hasLoc()) {
			inc = hasLocQuery.getValue();
			if ( inc == FILTER_INCLUDE ) {
				strSQL = appendANDPredicate ( strSQL, "contig.seq_end>0" );	
				joinSum("Has Loc");
			}
			else if ( inc == FILTER_EXCLUDE ) {
				strSQL = appendANDPredicate ( strSQL, "contig.seq_end = 0" );
				joinSum("No Loc");
			}
			int ng = (chkLocN!=null && chkLocN.isSelected()) ?
					getAndValidateInteger( chkLocN, true, "N group" ) : -1;
			if (ng!=-1) {
				strSQL = appendANDPredicate ( strSQL, "contig.seq_ngroup = " +  ng );
				joinSum("N group=" + ng);
			}
			int st = (chkLocStart!=null && chkLocStart.isSelected()) ?
					getAndValidateInteger( chkLocStart, true, "Start" ) : -1;
			if (st!=-1) {
				strSQL = appendANDPredicate ( strSQL, "contig.seq_start >= " +  st );
				joinSum("Start>=" + st);
			}
			int end = (chkLocEnd!=null && chkLocEnd.isSelected()) ?
					getAndValidateInteger( chkLocEnd, true, "End" ) : -1;
			if (end!=-1) {
				strSQL = appendANDPredicate ( strSQL, "contig.seq_end <= " +  st );
				joinSum("End<=" + end);
			}
		}
		if (metaData.hasExpLevels()) {
			strSQL = getWhereLibrary(strSQL);
			strSQL = getWhereNfold(strSQL);
		}
		if (metaData.hasDE() && chkFilterPVal!=null) strSQL = getWherePval(strSQL);
		if (metaData.hasHits()) 						strSQL = getWhereHits(strSQL);
		if (SNPandORFPanel != null) 					strSQL = getWhereORFs(strSQL);
		
		if (chkGOID!=null && chkGOID.isSelected())  {
			String x = getAndValidateString(chkGOID, "GO");
			joinSum("GO# " + x); // Query added in FieldContigData.getSeqFilterSQL
		}
		
		if (summary.equals("")) summary = "No filters set";
		
		if (strSQL==null || strSQL.length()==0 ) return "1";
		else return "(" + strSQL + ")";
	}
	
	private String getWhereLibrary(String strSQL) throws DataValidationError
	{
		boolean bNumIncLibrary = incLibQuery.isSelected();
		boolean bNumExLibrary = exLibQuery.isSelected();
		double dNumIncLibs = incLibQuery.getNum();
		double dNumExLibs = exLibQuery.getNum();
		String [] selIncLibs = getSelectedIncludeLibs();
		String [] selExLibs = getSelectedExcludeLibs();
		boolean hasInc = (selIncLibs!=null && selIncLibs.length>0) ? true : false;
		boolean hasEx = (selExLibs!=null && selExLibs.length>0) ? true : false;
		
		if (bNumIncLibrary && !hasInc) 
			throw new DataValidationError ( incLibQuery, "At least: select at least one include condition" );
		if(bNumExLibrary && !hasEx) 
			throw new DataValidationError ( exLibQuery, "At most: select at least one exclude condition" );		
		
		if (bNumIncLibrary  && dNumIncLibs <= 0)
			throw new DataValidationError ( incLibQuery, "Please enter a valid value for included counts");	
		if (bNumExLibrary && dNumExLibs < 0)
			throw new DataValidationError ( exLibQuery, "Please enter a valid value for excluded counts");	
		
		if (!bNumIncLibrary && !bNumExLibrary) return strSQL;
		
		// filter set, create clause
		boolean bNormalizedMode = chkRPKM.isSelected();
		String sum =  bNormalizedMode ? norm + " " : "Counts ";
		String mode = bNormalizedMode ? "N" : "";
		String prefix = "contig.L" + mode + "__" ;
		
		if( bNumIncLibrary && selIncLibs.length > 0) {
			String libPred = "";
			int op = (incLibQuery.getOption() == 0) ? 1 : 0;
			String sign = (incLibQuery.getOption() == 0) ? "&" : "|";
			sum += "I:" + selIncLibs[0];
			libPred = prefix + selIncLibs[0] + " >= " + dNumIncLibs;
			
			for(int i=1; i<selIncLibs.length; i++) {
				libPred = appendPredicate( libPred, 
						op, prefix + selIncLibs[i] + " >= " + dNumIncLibs);
				sum += sign + selIncLibs[i];
			}
			sum += " >=" + dNumIncLibs;
			strSQL = appendANDPredicate(strSQL, libPred);
		}
		
		if( bNumExLibrary && selExLibs.length > 0 ) {
			String libPred = "";
			int op = (exLibQuery.getOption() == 0) ? 1 : 0;
			String sign = (exLibQuery.getOption() == 0) ? "&" : "|";
			if (sum!="") sum += ", ";
			
			sum += "E:" +  selExLibs[0];
			libPred = prefix + selExLibs[0] + " <= " + dNumExLibs;
			for(int i=1; i<selExLibs.length; i++) {
				libPred = appendPredicate( libPred, op, prefix + selExLibs[i] + " <= " + dNumExLibs);
				sum += sign + selExLibs[i];
			}
			sum += " <=" + dNumExLibs;
			
			strSQL = appendANDPredicate(strSQL, libPred);
		}
		joinSum(sum);
		
		return strSQL;
	}
	private String getWhereNfold(String strSQL) throws DataValidationError {
		if (!nFoldObj.isNfold()) return strSQL;
		String [] colPairs = nFoldObj.getColumnNames();
		if (colPairs.length==0) return strSQL;
		
		int n = nFoldObj.getNfold();   // n-fold, get from NfoldObj
		String op = nFoldObj.getAndOr();
		
		if (n<0) 
			throw new DataValidationError ( nFoldObj, "N-fold must be a positive number" );
		
		String sql="", sum= n +"-fold: ", dirStr="(";
		
		for (String p : colPairs) {
			String [] tok = p.split(" ");
			if (tok.length!=2) continue;
			
			String [] lib = tok[0].split("/");
			if (lib.length!=2) continue;
			
			int dir = 0;
			if (tok[1].equals("U")) dir = 1;
			else if (tok[1].equals("D")) dir = -1;
			dirStr += tok[1];
			
			if (sql!= "") { 
				sql += op;
				sum += op; 
			} 
			sum += lib[0] + "/" + lib[1];
			
			String lib1 = "contig.LN__" + lib[0];
			String lib2 = "contig.LN__" + lib[1];
			
			// create ( (lib1/lib2>=n or lib2/lib1>=0) and (lib1>0 or lib2>0))
			String clause = "";
			if (dir>=0) clause += lib1 + "/ greatest(" + lib2 + ", 0.1) >= " + n;
			if (dir==0) clause += " or "; 
			if (dir<=0) clause += lib2 + "/ greatest(" + lib1 + ", 0.1) >= " + n;
			sql += "((" + clause + ") and (" + lib1 + " > 0 or " + lib2 + " > 0))";
		}
		sum += " " + dirStr + ")";
		joinSum(sum);
		return appendANDPredicate(strSQL, sql);
	}
	private String getWherePval(String strSQL)throws DataValidationError
	{
		boolean bPValFilter = chkFilterPVal.isSelected();
		if (!bPValFilter) return strSQL;
		
		String [] pValCols = getSelectedPValColumns();
		if (bPValFilter && (pValCols==null || pValCols.length==0))
			throw new DataValidationError ( chkFilterPVal, "Please select at least on DE column." );
		
		double dPValLimit = 0;
		try { 
			dPValLimit = Double.parseDouble(txtPVal.getText());
		}
		catch(NumberFormatException e) {
			throw new DataValidationError ( chkFilterPVal, "Invalid value for DE p-value limit" );
		}
		
		String boolVal = "";
		if (cmbBoolPVal.getSelectedIndex() == 0) boolVal = "&";
		else boolVal = "|";
		
		String sum = "p-value <" + String.format("%.0E ", dPValLimit)  +  pValCols[0];
		String pValQuery = "(abs(contig.P_" + pValCols[0] + ") < " + dPValLimit + ")";
		for(int i=1; i<pValCols.length; i++) {
			pValQuery += boolVal + "(abs(contig.P_" + pValCols[i] + ") < " + dPValLimit + ")";
			sum += boolVal + pValCols[i];
		}		
		
		strSQL = appendANDPredicate(strSQL, pValQuery);
		
		Vector<String> terms = new Vector<String>();
		boolean [] colsUp = getSelectedUpColumns();
		boolean [] colsDown = getSelectedDownColumns();
		sum += " (";
		for (int i = 0; i < colsUp.length; i++)
		{
			if (colsUp[i]) {
				terms.add("contig.P_" + pValCols[i] + ">0 ");
				sum += "U";
			}
			else if (colsDown[i]) {
				terms.add("contig.P_" + pValCols[i] + "<0 ");
				sum += "D";
			}
			else sum += "E";
		}
		sum += ")";
		if (terms.size() >0)
		{
			String sql = Static.strVectorJoin(terms, " and ");
			strSQL = appendANDPredicate(strSQL, sql);				
		}
		joinSum(sum);
		return strSQL;
	}
	private String getWhereHits(String strSQL) throws DataValidationError
	{
		boolean hasAnnotation = chkHasBestDBHit.isSelected();
		boolean hasNoAnnotation = chkNoAnno.isSelected();
		if(hasNoAnnotation) {
			strSQL = appendANDPredicate ( strSQL,"contig.bestmatchid IS NULL ");
			joinSum("No annotation");
			return strSQL;
		}
		if (!hasAnnotation) return strSQL;
	
		strSQL = appendANDPredicate ( strSQL,"contig.bestmatchid IS NOT NULL");
		
		int nDBHitIdent = (chkDBHitIdent.isSelected()) ? 
				getAndValidateInteger ( chkDBHitIdent, "Hit Similarity" ) : 0;
		int nDBHitCov = (chkDBHitCov.isSelected()) ? 
				getAndValidateInteger ( chkDBHitCov, "Hit coverage" ) : 0;
		int nDBSeqCov = (chkDBSeqCov.isSelected()) ? 
				getAndValidateInteger ( chkDBSeqCov, "Sequence coverage" ) : 0;	
		double dDBHitEval = (chkDBHitEVal.isSelected()) ? 
				getAndValidateDouble ( chkDBHitEVal, "E-value" ) : -1;
		String taxo =   (chkDBtaxo.isSelected()) ? chkDBtaxo.getOptionText() : "";
		String dbType = (chkDBtype.isSelected()) ? chkDBtype.getOptionText() : "";
						
		String type="", sum="", strUniProt="";
		int nDBHitAndOr = cmbDBHitAndOr.getValue();
		String fieldType; // 1, 0, -1
		int bestSelect = cmbFirstBest.getValue();
		if (bestSelect==1)      {fieldType = "";   type="Best Eval:";}
		else if (bestSelect==0) {fieldType = "ov"; type="Best Anno:";}
		else {// if has annotation, there will be a bestmatchid not null, but not PIDgo
			fieldType="go"; type="Best with GO:";
  			if (nDBHitCov==0 && dDBHitEval==-1 && nDBHitIdent==0 && nDBSeqCov==0) {
  				String strQ = "(contig.PIDgo>0)";
  				strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );
  			}
		}
		if ( dDBHitEval!=-1 )
		{
			String strQ = 	"contig.PID" + fieldType +" = pja_db_unitrans_hits.PID AND " +
							"pja_db_unitrans_hits.e_value";
			String op = chkDBHitEVal.getOptionSelection()==0 ?" <= ":" >= ";
			strQ += op + dDBHitEval;
			strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );	
			sum += ", E-value " + op + String.format("%.0E", dDBHitEval);
		}
		if ( nDBHitIdent>0 )
		{
			String strQ = 	"contig.PID" + fieldType + " = pja_db_unitrans_hits.PID AND " +
							"pja_db_unitrans_hits.percent_id";
			String op = chkDBHitIdent.getOptionSelection()==0 ? " <= ":" >= ";
			strQ += op + nDBHitIdent;
			strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );	
			sum += ", %Sim " + op + nDBHitIdent;
		}
		if ( nDBHitCov >0 ) 
		{
			String strQ = "(contig.PID" + fieldType + " = pja_db_unitrans_hits.PID AND " +
							"pja_db_unitrans_hits.prot_cov";
			String op = chkDBHitCov.getOptionSelection()==0 ? " <= ":" >= ";
			strQ += op + nDBHitCov + ")";
			strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );
			sum += ", %HitCov " + op + nDBHitCov;
		}
		
		if ( nDBSeqCov >0)
		{
			String strQ = 	"contig.PID" + fieldType + " = pja_db_unitrans_hits.PID AND " +
							"pja_db_unitrans_hits.ctg_cov";
			String op = chkDBSeqCov.getOptionSelection()==0 ?" <= ":" >= ";
			strQ += op + nDBSeqCov;
			strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );	
			sum += ", %SeqCov " + op + nDBSeqCov;
		}
		
		if ( taxo!="" || dbType!="")
		{
			String strQ = 	"(contig.PID" + fieldType + " = pja_db_unitrans_hits.PID";
			if (taxo!="") {
				strQ += " AND pja_db_unitrans_hits.taxonomy='" + taxo+"'";
				sum += ", Taxo=" + taxo;
			}
			if ( dbType!="")
			{
				strQ += " AND pja_db_unitrans_hits.dbtype='" + dbType +"'";
				sum += ", DBtype=" + dbType;
			}
			strQ += ")";			
			strUniProt = appendPredicate ( strUniProt, nDBHitAndOr, strQ );	
		}
		if (sum=="") {
			if (bestSelect==-1) sum = "Has GOs";
			else sum="Has Anno"; 
		}
		else {
			sum = type + sum.substring(1);
		}
		
		strSQL = appendANDPredicate ( strSQL, strUniProt );	
		joinSum(sum);
	
		int nNumTax = (chkNumAnno.isSelected()) ? getAndValidateInteger( chkNumAnno , "#annoDB") : 0;
		if ( nNumTax>0 ) {
			String boolOp;
			if(chkNumAnno.getOptionSelection() == 0) boolOp = " <= ";
			else boolOp = " >= ";
			strSQL = appendANDPredicate ( strSQL, "contig.cnt_annodb" + boolOp + nNumTax );
			joinSum("#annoDBs" + boolOp + nNumTax);
		}
		
		return strSQL;
	}
	private String getWhereORFs(String strSQL) throws DataValidationError
	{
		String strORF="";
		int nORFMinNT = chkHasORF.isSelected() ? 
				getAndValidateInteger( chkHasORF , true, "ORF Length") : -1;
		if (nORFMinNT!=-1) { 
			strORF = "contig.o_frame IS NOT NULL " +
							" AND (o_coding_end - o_coding_start)+1 >= " + nORFMinNT;
			joinSum("ORF>=" + nORFMinNT);
		}
		int nORFframe = chkFrameORF.isSelected() ? 
				getAndValidateInteger( chkFrameORF , true, "ORF Frame") : -10;
		if (nORFframe!=-10) {
			strORF = appendANDPredicate ( strORF,"contig.o_frame = " + nORFframe);
			joinSum("Frame=" + nORFframe);
		}				
		if (chkHasProteinORF.isSelected()) {
			strORF = appendANDPredicate ( strORF, "p_eq_o_frame=TRUE"); // the field is mis-named
			joinSum("ORF uses Hit frame");
		}
		if ( chkHasBothUTRs.isSelected()) {
			strORF = appendANDPredicate ( strORF, " o_coding_has_begin = TRUE AND o_coding_has_end = TRUE " );	
			joinSum("ORF has Start/Stop codons"); 
		}
		if (strORF!="") strSQL = appendANDPredicate ( strSQL, strORF);
		
		if (metaData.hasAssembly()) {
			int nMinSNPs = chkGreaterEqualSNPs.isSelected() ? 
					getAndValidateInteger( chkGreaterEqualSNPs , true, "Minimum SNPs") : -1;
			int nMaxSNPs = chkLessEqualSNPs.isSelected() ? 
					getAndValidateInteger( chkLessEqualSNPs, true, "Maximum SNPs") : -1;	
		
			if ( nMinSNPs!=-1 ) {
				strSQL = appendANDPredicate ( strSQL, "contig.snp_count >= " + nMinSNPs );
				joinSum("#SNPs>=" + nMinSNPs);
			}
			if ( nMaxSNPs!=-1 ) {
				strSQL = appendANDPredicate ( strSQL, "contig.snp_count <= " + nMaxSNPs );
				joinSum("#SNPs<=" + nMaxSNPs);
			}
		}
		return strSQL;
	}
	
    private void joinSum(String one) {
    		if (one==null || one.length()==0) return;
    		if (summary==null || summary.length()==0) summary=one;
    		else summary += "; " + one;
    }
    private String appendPredicate ( String strPred1, int nType, String strPred2 )
	{
		if (strPred1==null || strPred1.length()==0 ) return strPred2;
		if (strPred2==null || strPred2.length()==0 ) return strPred1;
		if ( nType == FILTER_OR )
			return "( ( " + strPred1 + " ) OR ( " + strPred2 + " ) )";
		if ( nType == FILTER_AND )
			return "( ( " + strPred1 + " ) AND ( " + strPred2 + " ) )";	
		throw new RuntimeException ( "Invalid AND/OR value of " + nType );
	}
	
	private  String appendANDPredicate ( String strPred1, String strPred2 )
	{
		return appendPredicate ( strPred1, FILTER_AND, strPred2 );
	}

	private void clear() { 
		// general
		chkCtgLength.setSelected(false);

		if (chkNumAligns!=null) chkNumAligns.setSelected(false);
		if (chkTotalExpLevel!=null) chkTotalExpLevel.setSelected(false);
		if (hasRemarkQuery!=null) hasRemarkQuery.setValue(FieldSeqData.FILTER_NONE);
		if (hasLocQuery!=null) {
			hasLocQuery.setValue(FieldSeqData.FILTER_NONE);
			chkLocN.setSelected(false); chkLocStart.setSelected(false); chkLocEnd.setSelected(false);
		}
		if (hasNQuery!=null) hasNQuery.setValue(FieldSeqData.FILTER_NONE);
		
		// libraries
		if (metaData.hasExpLevels()) {
			chkRPKM.setSelected(true);
			
			incLibQuery.setSelected(false);
			incLibQuery.setOption(0);
			incLibQuery.setNum(1);
			
			exLibQuery.setSelected(false);
			exLibQuery.setOption(0);
			exLibQuery.setNum(0);
			
			nFoldObj.clear();
			
			libSelAll.setSelected(true);
			for(int x=0; x<chkIncludeLibs.length; x++) {
				chkIncludeLibs[x].setSelected(true);
			}
			
			libCheckUnChecked.setSelected(false);
			for(int x=0; x<chkExcludeLibs.length; x++) {
				chkExcludeLibs[x].setSelected(false);
			}
		}
		
		if (chkFilterPVal!=null) {
			chkFilterPVal.setSelected(false);
			txtPVal.setText("0.05");
			
			for (int x=0; x<colSelectPVal.length; x++) {
				colSelectPVal[x].setSelected(false);
				colUpOnly[x].setSelected(false);
				colDownOnly[x].setSelected(false);
				colUpDown[x].setSelected(true);
			}
			chkAllPVal.setSelected(false);
			chkUpDownAll.setSelected(true);	
		}
		
		if (bHasAnno) { 	
			chkHasBestDBHit.setSelected(false);
			chkEitherAnno.setSelected(true);
			
			setBestHitEnabled(chkHasBestDBHit.isSelected());
			
			chkDBHitCov.setSelected(false);
			chkDBHitCov.setText("1");
			chkDBSeqCov.setSelected(false);
			chkDBSeqCov.setText("1");
			chkDBHitEVal.setSelected(false);
			chkDBHitEVal.setText("1E-30");
			chkDBHitIdent.setSelected(false);
			chkDBHitIdent.setText("40");
			chkDBtype.setSelected(false);
			chkDBtaxo.setSelected(false);
			chkNumAnno.setSelected(false);
		}
		
		if (chkGOID!=null) {
			chkGOID.setSelected(false);
			chkGOID.setText("");
		}
		
		if (metaData.hasORFs()) {
			chkHasORF.setText("300");
			chkHasORF.setSelected(false);
			chkFrameORF.setText("");
			chkFrameORF.setSelected(false);
			
			chkHasBothUTRs.setSelected(false);
			chkHasProteinORF.setSelected(false);

			if (metaData.hasAssembly()) { // Can have SNPs and no ORFs -- not worth separating....
				chkLessEqualSNPs.setSelected(false);
				chkLessEqualSNPs.setText("1");
				chkGreaterEqualSNPs.setSelected(false);
				chkGreaterEqualSNPs.setText("1");
			}
		}
	}
	/**************** Private variable ************/
	private Dimension defaultLabelDims = null;
	private Dimension defaultNumDims = null;
	
	private JComponent uniprotPanel, selLibraryPanel, nfoldPanel,
		contigPanel, SNPandORFPanel, PValPanel, GOPanel;

	// Layout "constants"
	private static final int SUBGROUP_INSET_WIDTH = 50;
	private int MIN_ROW_HEIGHT = 50; 
	
	// Error handling
	private JLabel lblError = new JLabel ( " " );
	private JComponent failure = null;
	private JComponent centerPanel = null;
	private JScrollPane scroller = null;
	
	// general
	private ToggleTextComboField chkCtgLength = null;
	private ToggleTextComboField chkNumAnno = null;
	private ToggleTextComboField chkNumAligns = null;
	private ToggleTextComboField chkTotalExpLevel = null;
	private UIqueryIncEx hasRemarkQuery = null;
	private UIqueryIncEx hasLocQuery = null;
	private UIqueryIncEx hasNQuery = null;
	private ToggleTextField chkLocN = null, chkLocStart=null, chkLocEnd=null;
	
	// libs
	private JCheckBox [] chkIncludeLibs = null;
	private JLabel [] labelIncLibs = null;
	private JCheckBox [] chkExcludeLibs = null;
	private JLabel [] labelExLibs = null;
	private JCheckBox libSelAll = null;
	private JCheckBox libCheckUnChecked = null;
	
	private JRadioButton chkCounts = null;
	private JRadioButton chkRPKM = null;
	private UIqueryIncExLib incLibQuery = null;
	private UIqueryIncExLib exLibQuery = null;
	
	// Nfold
	private UIfieldNFold nFoldObj = null;
	
	//PVal controls
	private JTextField txtPVal = null;
	private JCheckBox chkFilterPVal = null;
	private JComboBox <String> cmbBoolPVal = null;
	private JCheckBox [] colSelectPVal = null;
	private JRadioButton[] colUpOnly = null;
	private JRadioButton[] colDownOnly = null;
	private JRadioButton[] colUpDown = null;
	private JLabel[] colpValTitle = null;

	private JRadioButton chkUpOnlyAll; 
	private JRadioButton chkDownOnlyAll; 
	private JRadioButton chkUpDownAll; 
	private JCheckBox chkAllPVal;
	
	// Good UTRs and SNPs widgets
	private ToggleTextField chkLessEqualSNPs = null;		
	private ToggleTextField chkGreaterEqualSNPs = null;	
	
	private ToggleTextField chkHasORF = null;
	private ToggleTextField chkFrameORF = null;
	private JCheckBox chkHasBothUTRs = null;
	private JCheckBox chkHasProteinORF = null;
	
	// Search 
	private ToggleTextField chkGOID = null;
	
	// Good UniProt hit widgets
	private JRadioButton chkHasBestDBHit = null, chkNoAnno = null, chkEitherAnno = null;
	private UIqueryIncEx cmbFirstBest = null, cmbDBHitAndOr = null;
	private ToggleTextComboField chkDBHitCov = null, chkDBSeqCov = null;
	private ToggleTextComboField chkDBHitEVal = null, chkDBHitIdent = null;
	private ToggleTextComboField chkDBtaxo = null, chkDBtype = null;
	private boolean bHasAnno=true;
			
	//Hold unitrans set names (name retrieval only, not filtered)
	private String [] pValColumns = null;
	private String [] pValTitles = null;
	private String [] libColumns = null;
	private String [] libTitles = null;
	
	private static final String [] BOOLEAN_LABELS = { "<=", ">=" };
	//Labels
	private static final String GENERAL_HEADER = "General";
	private static final String GENERAL_DESCRIPTION = "Find sequences with the specified attributes.";
	private static String LIBRARY_HEADER = "Counts and RPKM";
	private static String LIBRARY_DESCRIPTION = "Filter sequences based on counts or RPKM";
	private static final String NFOLD_HEADER = "N-fold";
	private static final String NFOLD_DESCRIPTION = "Filter on n-fold change between two conditions";
	private static final String PVAL_HEADER = "Differential Expression";
	private static final String PVAL_DESCRIPTION = "Filter on DE p-value columns";
	private static final String BEST_DB_HIT_HEADER = "Best Hit";
	private static final String BEST_DB_HIT_DESCRIPTION = "Search on Best Eval or Best Anno for a sequence (use 'AnnoDB Hits' to search on all hits).";
	private static final String SNP_ORF_HEADER = "SNPs and ORFs";
	private static final String SNP_ORF_DESCRIPTION = "Find sequences with SNPs (assembled contigs only) and/or ORF attributes.";
	private static final String GO_HEADER = "GO";
	private static final String GO_DESCRIPTION = "Search for a specific GO term (show all assigned and inherited)";

    private MetaData metaData = null;
	private int nChildren = 0;
	private RunQuery theQuery = null;
	private FieldSeqData tempContigData = null;
	private String norm="RPKM";
}
