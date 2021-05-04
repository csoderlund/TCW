package sng.viewer.panels.seqTable;
/******************************************************
 * The main sequence table
 */
import java.awt.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.Vector;

import sng.util.FieldMapper;
import sng.util.MainTable;
import sng.util.RunQuery;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import sng.database.Globals;
import util.ui.ButtonComboBox;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.database.Globalx;
import util.methods.*;


public class SeqTableTab extends Tab
{		
	private final String helpDir = Globals.helpDir + "SeqTable.html";
	
	public SeqTableTab (STCWFrame inFrame, FieldMapper inContigFields, 
						RunQuery inQuery, String [] contigs, 
						Vector <String> tableRows, 
						int nTotalContigs, String summary)
	throws Exception
	{
		super(inFrame, null);
		super.setBackground(Color.WHITE);
		super.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		hasGOs = inFrame.getMetaData().hasGOs();
		isAAdb = inFrame.getMetaData().isAAsTCW(); 
		hasReps = inFrame.getMetaData().hasReps();
		
		theContigIDs = contigs;
		theQuery = inQuery;
        theFields = inContigFields;
       
        contigTable = new MainTable(theFields, tableRows, 
        		theFields.getVisibleFieldIDs(), refreshListener);

		contigTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
		contigTable.getTableHeader().setBackground(Color.WHITE);
		
		contigTable.addSelectionChangeListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) { // responds to clicking header
				if ( btnViewContig != null )
					btnViewContig.setEnabled( contigTable.getSelectedRowCount() > 0 );
			}
		});

		contigTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				btnViewContig.setEnabled(true);
			}
		});
		contigTable.addDoubleClickListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				addContigTab();
			}
		});
		
		JPanel scrollPane = new JPanel();

		scrollPane.setAlignmentY(LEFT_ALIGNMENT);
		scrollPane.add(contigTable);
		JScrollPane scroller = new JScrollPane ( contigTable );
		scroller.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		scroller.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		scroller.getViewport().setBackground(Color.WHITE);
		UIHelpers.setScrollIncrements( scroller );
		
		JPanel toolBar = createToolbar ( );
		toolBar.setBackground(Color.WHITE);

		JPanel sumRow = Static.createRowPanel();
		if (summary==null || summary=="") summary = "; All sequences";
		else summary = ";  " + summary;
		
		double cnt = contigTable.getDataRowCount();
		double tot = nTotalContigs;
		double per = (cnt/tot)*100.0;
		String sper = String.format(" (%.1f%s)", per, "%");
		tableSummary = summary;
		lblSummary = new JLabel ("     " + contigTable.getDataRowCount() + sper +
				" of " + nTotalContigs +  summary);
		lblSummary.setAlignmentY(LEFT_ALIGNMENT);
		sumRow.add(lblSummary);
		
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		add ( Box.createVerticalStrut(5) );
		add ( sumRow );
		add ( Box.createVerticalStrut(5) );
		add ( toolBar );
		add ( Box.createVerticalStrut(5) );
		add ( scroller );
	}
	
	private ActionListener refreshListener = new ActionListener() {
		public void actionPerformed(ActionEvent event) {
			doRefreshColumns();
		}
	};
	
	public RunQuery getQuery ( ) { return theQuery; }
	public String [] getContigIDs() { return theContigIDs; }
	public int getViewMode() { return nViewMode; }
	
	public FieldMapper getFieldMapper ( ) { return theFields; }
	
	// Setup the Button panel.  (Everything above the table.)	
	private JPanel createToolbar ( )
	{
		final JPanel toolbarPanel = Static.createRowPanel();
		
		btnViewContig = new JButton("View Selected Sequence");
		btnViewContig.setBackground(Globalx.FUNCTIONCOLOR);
		btnViewContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				addContigTab();
			}
		});
		btnViewContig.setEnabled( false ); 
		
		JButton btnRefresh = new JButton ("Refresh Columns");
		btnRefresh.addActionListener(refreshListener);	
		
		createToolTable();
		
		btnHelp = new JButton("Help");
		btnHelp.setBackground(Globalx.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getParentFrame(), "Sequence table", helpDir );
			}
		});

		toolbarPanel.add( Box.createHorizontalStrut(5) );
		toolbarPanel.add( btnViewContig );
		toolbarPanel.add( Box.createHorizontalStrut(10) );
		toolbarPanel.add(btnRefresh);
		toolbarPanel.add( Box.createHorizontalStrut(10) );
		toolbarPanel.add(btnTable);
		toolbarPanel.add( Box.createHorizontalStrut(5) );
		
		toolbarPanel.add( Box.createHorizontalGlue() );
		toolbarPanel.add( Box.createHorizontalStrut(5) );
		toolbarPanel.add( btnHelp );
		toolbarPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)toolbarPanel.getPreferredSize ().getHeight() ) );		
		return toolbarPanel;
	}
	
	private void createToolTable() {
		final JPopupMenu tablepopup = new JPopupMenu();
		
		tablepopup.add(new JMenuItem(new AbstractAction("Show Column Stats") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				try {
					String title = "Main table" + tableSummary;
					String info = contigTable.statsPopUpCompute(title);
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
					StringSelection stuff = new StringSelection(contigTable.copyTableToString());
					cb.setContents(stuff, null);
				} catch (Exception er) {ErrorReport.reportError(er, "Error copy table");}
			}
		}));
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("Export table of columns (" + Globalx.CSV_SUFFIX + ")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				runExport(EXPORT_TABLE);
			}
		}));
		tablepopup.add(new JMenuItem(new AbstractAction("Export sequences from table (" + Globalx.FASTA_SUFFIX + ")") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				runExport(EXPORT_SEQS);
			}
		}));
		if (!isAAdb) { 
			tablepopup.add(new JMenuItem(new AbstractAction("Export translated ORFs from table ("+ Globalx.FASTA_SUFFIX + ")") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					runExport(EXPORT_ORF);
				}
			}));
		}
		if (hasReps) {
			tablepopup.add(new JMenuItem(new AbstractAction("Export library replicates from table (" + Globalx.CSV_SUFFIX + ", very slow)") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					runExport(EXPORT_COUNTS);
				}
			}));
		}
		
		tablepopup.addSeparator();
		tablepopup.add(new JMenuItem(new AbstractAction("Export hit sequences from table...") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				runExportHit();
			}
		}));
		if (hasGOs) {
			tablepopup.add(new JMenuItem(new AbstractAction("Export sequences with Best GOs (.txt)") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					runExport(EXPORT_SeqGO);
				}
			}));
			
			tablepopup.addSeparator();
			tablepopup.add(new JMenuItem(new AbstractAction("Export GOs from table...") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					runExportGO();
				}
			}));
		}
		btnTable = new JButton("Table...");
		btnTable.setBackground(Color.WHITE);
		btnTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                tablepopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnTable.setEnabled(true);
	}
	
	private void doRefreshColumns() {
		if (contigTable!=null) contigTable.clearTable();
		if(theContigIDs != null) // from Basic Query, where query is list of contigs
			getParentFrame().loadQueryContigs( SeqTableTab.this, theQuery, theContigIDs, nViewMode,  null );
		else if(theQuery != null)
			getParentFrame().loadQueryFilter( SeqTableTab.this, theQuery, null );
		else {
			System.out.println("TCW error in Refresh Columns");
		}
	}
	// one contig selected
	private void addContigTab ( )
	{
		int nRow = contigTable.getSelectedRow();
		if (nRow < 0) return;
		
		String strName = (String)theFields.extractFieldByID( contigTable.getRowAt(nRow), 
				FieldSeqData.SEQ_ID_FIELD);
		getParentFrame().addContigPage(strName, this, nRow);
	}
	
	public String getContigIDAtRow(int nRow) {
		if (nRow >= 0 && nRow < contigTable.getDataRowCount())
			return (String)theFields.extractFieldByID( contigTable.getRowAt(nRow), 
					FieldSeqData.SEQ_ID_FIELD );		
		return null;
	}
	
	public int getNextRowNum ( int nRow )
	{
		int nNextRow = nRow + 1;
		if (nNextRow >= contigTable.getDataRowCount()) 
			nNextRow = 0;
		return nNextRow;
	}
	
	public int getPrevRowNum ( int nRow )
	{
		int nPrevRow = nRow - 1;
		if (nPrevRow < 0)
			nPrevRow = contigTable.getDataRowCount() - 1; // get last row
		return nPrevRow;
	}

	/**
	 * Called on closing a sequence table tab
	 */
	public void close() {
		if (contigTable!=null) contigTable.clearTable();
		contigTable = null;
		theQuery=null;
		theFields = null;
		btnViewContig = null;
		lblSummary = null;
	}
	/******************************************************************
	 * XXX Export table functions
	 */
	private static final int EXPORT_CANCEL = 0;
	private static final int EXPORT_OK = 1;
	private static final int EXPORT_TABLE = 2;
	private static final int EXPORT_COUNTS = 3;
	private static final int EXPORT_SEQS = 4;
	private static final int EXPORT_ORF = 5;
	private static final int EXPORT_SeqGO = 6;
	private static final String filePrefix = "SeqTable";

	private void runExport(int mode) {
		final int saveMode = mode;
		//Thread theThread = new Thread(new Runnable() { // CAS314 maybe this is why the file popup sometimes doens't work
		//	public void run() {
				try {
					btnTable.setEnabled(false);
					if(saveMode == EXPORT_TABLE) {
						contigTable.saveToFileTabDelim(btnTable, filePrefix+"Columns", getParentFrame());
					}
					else if(saveMode == EXPORT_SEQS) {
                        contigTable.saveToFasta(btnTable, filePrefix+"Seqs", getParentFrame());
                    }
                    else if(saveMode == EXPORT_ORF) {
                        contigTable.saveORFToFasta(btnTable, filePrefix+"ORFs", getParentFrame());
                    }
                    else if(saveMode == EXPORT_COUNTS) {
						contigTable.saveToFileCounts(btnTable, filePrefix+"Reps", getParentFrame(),
						   getParentFrame().getQueryContigTab().getAllLibraryNames()); 
					}
                    else if(saveMode == EXPORT_SeqGO) { 
						contigTable.saveGOFromBest(btnTable, filePrefix+"BestGO", getParentFrame()); 
					}
					btnTable.setEnabled(true);
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error creating export file");}
			//}
		//});
		//theThread.start();
	}
	private void runExportHit() {
		final ExportHit eh = new ExportHit();
		eh.setVisible(true);
		final int saveMode = eh.getSelection();
		if(saveMode == EXPORT_CANCEL) return;
	
		Thread theThread = new Thread(new Runnable() {
			public void run() {
				try {
					btnTable.setEnabled(false);
					
                    contigTable.saveHitsToFasta(btnTable, eh.getHitFile(), getParentFrame(), eh.getHitSQL()); 
					
                    btnTable.setEnabled(true);
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error creating export file");}
			}
		});
		theThread.start();
	}
	private void runExportGO() {
		final ExportGO et = new ExportGO();
		et.setVisible(true);
		final int saveMode = et.getSelection();
		if(saveMode == EXPORT_CANCEL) return;
	
		Thread theThread = new Thread(new Runnable() {
			public void run() {
				try {
					btnTable.setEnabled(false);
					
                    contigTable.saveGOtoFile(btnTable, et.getGOfile(), getParentFrame(),
                    		et.getGOLevel(), et.getGOEval(), et.getTermType()); 
					
                    btnTable.setEnabled(true);
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error creating export file");}
			}
		});
		theThread.start();
	}
	private class ExportHit extends JDialog {
		private static final long serialVersionUID = 1L;
		public ExportHit() {
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Export.... ");
       
    		JPanel selectPanel = Static.createPagePanel();
	        JLabel label = Static.createLabel("Hit Sequences from table (" + Globalx.FASTA_SUFFIX + ")");
	       
	        btnHitEval = Static.createRadioButton("Best Bits", false);
	        btnHitAnno = Static.createRadioButton("Best Anno", false); 
	        btnHitBoth = Static.createRadioButton("Both", true); 
	        ButtonGroup hitgrp = new ButtonGroup();
	        hitgrp.add(btnHitEval); hitgrp.add(btnHitAnno);hitgrp.add(btnHitBoth);

	        JPanel row = Static.createRowPanel();
	        row.add(Box.createHorizontalStrut(15));
	        row.add(btnHitEval); row.add(Box.createHorizontalStrut(3));
	        row.add(btnHitAnno); row.add(Box.createHorizontalStrut(3));
	        row.add(btnHitBoth); 
	        
    		selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(label);
    		selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(row);
    		
    		 // bottom buttons
    		btnOK = new JButton("OK");
    		btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode = EXPORT_OK;
					setVisible(false);
				}
			});
    		btnCancel = new JButton("Cancel");
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode = EXPORT_CANCEL;
					setVisible(false);
				}
			});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		
    		JPanel buttonPanel = Static.createRowPanel();
    		buttonPanel.add(btnOK);
    		buttonPanel.add(Box.createHorizontalStrut(20));
    		buttonPanel.add(btnCancel);
    		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
  
           	JPanel mainPanel = Static.createPagePanel();
    		mainPanel.add(selectPanel);
    		mainPanel.add(Box.createVerticalStrut(15));
    		mainPanel.add(buttonPanel);
    		
    		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    		add(mainPanel);
    		
    		pack();
    		this.setResizable(false);
    		UIHelpers.centerScreen(this);
		}
		public String getHitSQL() {
    		if (btnHitEval.isSelected()) return "filter_best=1";
    		if (btnHitAnno.isSelected()) return "filter_ovbest=1";
    		return "(filter_best=1 or filter_ovbest=1)";
    	}
	    public String getHitFile() {
    		String prefix = filePrefix+"Hits";
    		if (btnHitEval.isSelected()) return prefix + "Bits" + Globalx.FASTA_SUFFIX;
    		if (btnHitAnno.isSelected()) return prefix + "Anno" + Globalx.FASTA_SUFFIX;
    		return prefix + "Both"+ Globalx.FASTA_SUFFIX;
    	}
		public int getSelection() { return nMode; }
		
	    public int nMode;
	    JRadioButton btnHitEval = null, btnHitAnno=null, btnHitBoth=null;
		JButton btnOK = null, btnCancel = null;
	}
    private class ExportGO extends JDialog {
		private static final long serialVersionUID = 6152973237315914324L;
    	
	    /***********************************************
	     * the methods to output the files are in SortableTable in util.ui
	     * Called above (search for Export button)
	     */
    	public ExportGO() {
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Export GOs.... ");
            
            JLabel label = Static.createLabel("GOs from table (" + Globalx.CSV_SUFFIX + ")");
           
            JLabel goLabel = new JLabel("GO Level: ");
			txtGOlevel  = new JTextField(3);
			txtGOlevel.setMaximumSize(txtGOlevel.getPreferredSize());
			txtGOlevel.setText("");
			
			cmbTermTypes = new ButtonComboBox();
    		cmbTermTypes.addItem("Any " + Globalx.goFullOnt);
    		for(int x=0; x<Globalx.GO_TERM_LIST.length; x++)
    				cmbTermTypes.addItem(Globalx.GO_TERM_LIST[x]);
    		
    		cmbTermTypes.setSelectedIndex(0);
    		cmbTermTypes.setMaximumSize(cmbTermTypes.getPreferredSize());
    		cmbTermTypes.setMinimumSize(cmbTermTypes.getPreferredSize());
    		cmbTermTypes.setBackground(Globalx.BGCOLOR);
    		
			JLabel goEvalLabel = new JLabel("E-value: ");
            txtGOEval  = new JTextField(4);
            txtGOEval.setMaximumSize(txtGOEval.getPreferredSize());
            txtGOEval.setText("");
    		
            // bottom buttons
    		btnOK = new JButton("OK");
    		btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode=EXPORT_OK;
					setVisible(false);
				}
			});
    		btnCancel = new JButton("Cancel");
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode=EXPORT_CANCEL;
					setVisible(false);
				}
			});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		
    		JPanel selectPanel = Static.createPagePanel();
	        JPanel goPanel = Static.createPagePanel();
	        JPanel row = Static.createRowPanel();
	        
	        row = Static.createRowPanel();
            row.add(Box.createHorizontalStrut(15));
            
            row.add(goLabel);  row.add(Box.createHorizontalStrut(2));
            row.add(txtGOlevel);
            row.add(Box.createHorizontalStrut(5));
            
            row.add(cmbTermTypes);
            goPanel.add(row);
            goPanel.add(Box.createVerticalStrut(5));
            
            row = Static.createRowPanel();
            row.add(Box.createHorizontalStrut(15));
            row.add(goEvalLabel); row.add(Box.createHorizontalStrut(2));
            row.add(txtGOEval);
            goPanel.add(row);
            	        
	        selectPanel.add(label);
	        selectPanel.add(Box.createVerticalStrut(5));
	        selectPanel.add(goPanel);
	    
	        selectPanel.add(new JSeparator());
	        
    		JPanel buttonPanel = Static.createRowPanel();
    		buttonPanel.add(btnOK);
    		buttonPanel.add(Box.createHorizontalStrut(20));
    		buttonPanel.add(btnCancel);
    		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
  
           	JPanel mainPanel = Static.createPagePanel();
    		mainPanel.add(selectPanel);
    		mainPanel.add(Box.createVerticalStrut(15));
    		mainPanel.add(buttonPanel);
    		
    		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    		add(mainPanel);
    		
    		pack();
    		this.setResizable(false);
    		UIHelpers.centerScreen(this);
    	}
        
    	public String getGOLevel() { 
    		String x = txtGOlevel.getText().trim();
    		if (x.equals("-")) return x;
    		if (x.equals("")) return "-";
    		if (x.equals("0")) return "-";
			try {Integer.parseInt(x);}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, 
					"Incorrect level " + x + "\nUsing default 2", "Error", JOptionPane.PLAIN_MESSAGE);
				x="2";
				txtGOlevel.setText(x);
			}
    		return x; 
    	}
        public String getGOEval() { 
        	String x = txtGOEval.getText().trim();
			if (x.equals("") || x.equals("-")) return "";
			
			try {Double.parseDouble(x);}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, 
					"Incorrect evalue " + x + "\nDefault to no cutoff", "Error", JOptionPane.PLAIN_MESSAGE);
				x="";
				txtGOEval.setText(x);
			}
        	return x; 
        }
        public String getTermType() {
    			String type = (cmbTermTypes.getSelectedIndex() > 0) ? cmbTermTypes.getSelectedItem() : "";
    			return type;
        }
   
    	public int getSelection() { return nMode; }
    	public String getGOfile() {
    		String file=filePrefix+"GO";
    		
    		String level = getGOLevel();
    		if (!level.equals("-")) file += "_" + level;
    		if (cmbTermTypes.getSelectedIndex() > 0) {
    			int idx = cmbTermTypes.getSelectedIndex();
    			file += "_" + Globalx.GO_TERM_ABBR[idx-1];
    		}
    		
    		String eval = getGOEval();
    		if (!eval.equals("")) file += "_" + eval;
    		return file + Globalx.CSV_SUFFIX;
    	}
    	 	
    	ButtonComboBox cmbTermTypes = null;
    	JTextField txtGOlevel = null, txtGOEval = null;
    	
    	JButton btnOK = null, btnCancel = null;
    	
    	int nMode = EXPORT_OK;
    } // end ExportType
    
    private FieldMapper theFields;
	private RunQuery theQuery;
	private JButton btnViewContig;
	private JButton btnTable;
	private JButton btnHelp = null;
	private MainTable contigTable;
	private JLabel lblSummary;
	private String [] theContigIDs = null; 
	private int nViewMode = -1;
	private boolean hasGOs=false, isAAdb=false, hasReps=false;
	private String tableSummary="";
    
    private static final long serialVersionUID = 1;
}