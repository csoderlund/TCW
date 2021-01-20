/**
 * Table of Pairs
 * Object created for Table from sng.viewer.STCWFrame.loadQueryFilter
 */
package sng.viewer.panels.pairsTable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sng.database.Globals;
import sng.dataholders.MultiCtgData;
import sng.util.FieldMapper;
import sng.util.MainTable;
import sng.util.RunQuery;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class PairTableTab extends Tab
{	
	private String helpDir = Globals.helpDir + "PairTable.html";
	
	public PairTableTab ( STCWFrame parentFrame, 
						FieldMapper inIndexer, 
						RunQuery inQuery,	
						Vector <String> tableRows, String summary ) 
	throws Exception
	{
		super(parentFrame, null);
		super.setBackground(Color.WHITE);
		super.setAlignmentX(Component.LEFT_ALIGNMENT);
		nPairs = parentFrame.getMetaData().getnPairs();
		tableSummary = summary;
		
		theQuery = inQuery;
		theFields = inIndexer;
		
		pairTable = new MainTable ( 
			theFields, 
			tableRows, 
			theFields.getVisibleFieldIDs(),
			refreshListener
		);
		pairTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		pairTable.getTableHeader().setBackground(Color.WHITE);
		
		pairTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				btnViewAlign.setEnabled(true);
				btnViewSeqs.setEnabled(true);
			}
		});
		pairTable.addSelectionChangeListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if ( btnViewAlign != null )
					btnViewAlign.setEnabled( pairTable.getSelectedRowCount() > 0 );
				if ( btnViewSeqs != null )
					btnViewSeqs.setEnabled( pairTable.getSelectedRowCount() > 0 );
			}
		});
		pairTable.addDoubleClickListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				addPairAlignTab();
			}
		});
		
		// Scroll panel
		JPanel scrollPane = new JPanel();

		scrollPane.setAlignmentY(LEFT_ALIGNMENT);
		scrollPane.add(pairTable);
		JScrollPane scroller = new JScrollPane ( pairTable );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		scroller.getViewport().setBackground(Color.WHITE);
		UIHelpers.setScrollIncrements( scroller );
		
		JPanel toolPanel = createToolbar ( );
		
		JPanel sumRow = Static.createRowPanel();
		if (summary==null || summary=="") summary = "";
		else summary = ";  " + summary;
		
		lblSummary = new JLabel ("     " + pairTable.getDataRowCount() + " of " + nPairs +  summary);
		lblSummary.setAlignmentY(LEFT_ALIGNMENT);
		sumRow.add(lblSummary);
		
		// Add to the called object's panel
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
        add ( Box.createVerticalStrut(5) );
        add ( sumRow );
        add ( Box.createVerticalStrut(5) );
		
        add ( toolPanel );
		add ( Box.createVerticalStrut(5) );
		add ( scroller );
	}
	private JPanel createToolbar ( )
	{
		final JPanel topPanel = Static.createRowPanel();
		
		// Button panel	
		btnViewAlign = new JButton ("Align Selected Pair");
		btnViewAlign.setBackground(Globals.FUNCTIONCOLOR);
		btnViewAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addPairAlignTab();
			}
		});
		btnViewAlign.setEnabled(false);
		
		btnViewSeqs = new JButton("View Sequences");
		btnViewSeqs.setBackground(Globals.PROMPTCOLOR);
		btnViewSeqs.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            		addPairViewTab();
            }
        });
		
		JButton btnRefresh = new JButton ("Refresh Columns");
		btnRefresh.addActionListener(refreshListener);

		createToolTable(); // defines btnTable
		
		JButton btnHelp = new JButton("Help");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), "Pairs Table", helpDir); 
			}
		});
	
		topPanel.setBackground(Color.WHITE);
		topPanel.add( Box.createHorizontalStrut(5) );
	
		topPanel.add( btnViewAlign );
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.add( btnViewSeqs );
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.add(btnRefresh);
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.add( btnTable );
		topPanel.add( Box.createHorizontalStrut(5) );
		
		topPanel.add( Box.createHorizontalGlue() );
		
		topPanel.add(btnHelp);
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)topPanel.getPreferredSize ().getHeight() ) );
		return topPanel;
	}
	private void createToolTable() { //CAS314 added
		final JPopupMenu tablepopup = new JPopupMenu();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Show Column Stats") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					String title = "Main table: " + tableSummary;
					String info = pairTable.statsPopUpCompute(title);
					UserPrompt.displayInfoMonoSpace(null, title, info);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copy table");}
			}
		}));
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("Copy Table") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection stuff = new StringSelection(pairTable.copyTableToString());
					cb.setContents(stuff, null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copy table");}
			}
		}));
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("Export table of columns (" + Globalx.CSV_SUFFIX + ")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					btnTable.setEnabled(false);
					
					pairTable.saveToFileTabDelim("PairTableColumns" + Globalx.CSV_SUFFIX, getParentFrame());
	       
					btnTable.setEnabled(true);
				}
				catch (Exception ex) {ErrorReport.prtReport(ex, "Error creating export file");}
			}
		}));
		
		
		btnTable = new JButton("Table...");
		btnTable.setBackground(Color.WHITE);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnTable.setEnabled(true);
	}
	
	private ActionListener refreshListener = new ActionListener() {
		public void actionPerformed(ActionEvent event) {
			doRefreshColumns();
		}
	};
	
	public RunQuery getQuery () { return theQuery; }
	
	private void doRefreshColumns() {
		close(); // XXX
		getParentFrame().loadQueryFilter( PairTableTab.this, theQuery, null );
	}
	
	// a row in the Pairs table has been selected for viewing
	// add the tab on the left
	private void addPairAlignTab ()
	{
		int nRow = pairTable.getSelectedRow();
		if ( nRow == -1 ) return;
		
		String[] strContigIDs = getContigIDsAtRow( nRow );
		MultiCtgData pairObj = new MultiCtgData (strContigIDs[0], strContigIDs[1]);	
		
		int pairNum = getPairNumAtRow( nRow );
		String strTitle = "Pair #" + pairNum;

		getParentFrame().addPairAlignTab(pairObj, strTitle, PairTableTab.this, nRow, pairNum);
	}
	
	private void addPairViewTab ()
	{
		int nRow = pairTable.getSelectedRow();
		if ( nRow == -1 ) return;
		
		String[] strContigIDs = getContigIDsAtRow( nRow );
		
		int pairNum = getPairNumAtRow( nRow );
		String strTitle = "Pair #" + pairNum;

		getParentFrame().loadContigs(strTitle, strContigIDs, STCWFrame.PAIRS_QUERY);
	}

	public String[] getContigIDsAtRow( int nRow )
	{
		String[] contigIDs = new String[2];
		
		if ( nRow < 0 || nRow >= pairTable.getDataRowCount() )
			return null;
		
		Object[] row = (Object[])pairTable.getRowAt(nRow);
		contigIDs[0] = (String)theFields.extractFieldByID( row, FieldPairsData.FIELD_ID_CONTIG_1 );
		contigIDs[1] = (String)theFields.extractFieldByID( row, FieldPairsData.FIELD_ID_CONTIG_2 );
		
		return contigIDs;
	}
	
	public int getPairNumAtRow( int nRow )
	{
		if ( nRow < 0 || nRow >= pairTable.getDataRowCount() ) return -1;
		
		int num = nRow;
		Object[] row = (Object[])pairTable.getRowAt( nRow );
		String n = (String)theFields.extractFieldByID( row, FieldPairsData.RECORD_NUM_FIELD );
		try {
			num = Integer.valueOf(n);
		}
		catch (NumberFormatException e) { 
			// if Pairs# columns not shown in table, get this error
		}
		return num;
	}
	
	public FieldMapper getFieldMapper()
	{
		return theFields;
	}
	
	public int getNextRowNum ( int nRow )
	{
		int nNextRow = nRow + 1;
		if (nNextRow >= pairTable.getDataRowCount()) 
			nNextRow = 0; // wrap-around
		return nNextRow;
	}
	
	public int getPrevRowNum ( int nRow )
	{
		int nPrevRow = nRow - 1;
		if (nPrevRow < 0)
			nPrevRow = pairTable.getDataRowCount() - 1; // get last row
		return nPrevRow;
	}

	/**
	 * Remove table and free memory
	 */
	public void close() {
		if (pairTable!=null) pairTable.clearTable();
		pairTable = null;
		theFields = null;
		lblSummary = null;
	}
	
	private RunQuery theQuery = null;
	private MainTable pairTable;
	private FieldMapper theFields;
	private JLabel lblSummary = null;
	private JButton btnViewAlign, btnViewSeqs;
	private JButton btnTable;
	private int nPairs=0;
	private String tableSummary="";
	
    private static final long serialVersionUID = 1;
}
