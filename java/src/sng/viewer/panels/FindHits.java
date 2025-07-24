package sng.viewer.panels;

/****************************************************
 * Blast tab - runs blast/diamond and displays results
 * Diamond can run blastx or blastp mode
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.*;

import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.SequenceData;
import sng.util.Tab;
import sng.viewer.STCWFrame;

import util.file.FileRead;
import util.file.FileC;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.database.DBConn;
import util.database.Globalx;
import util.ui.UserPrompt;

public class FindHits extends Tab
{
	private static final long serialVersionUID = 653192706293635582L;
	private static final String helpHTML = Globals.helpDir + "FindHits.html"; // CAS330 rename file and help
	private static final String searchHelpHTML = Globalx.searchHelp;
	
	private final String RESULTS0 = ".results" + FileC.TEXT_SUFFIX;
	private final String RESULTS6 = ".results" + FileC.TSV_SUFFIX;;
	private final String dbSELECT = "Select protein database";
	private final int nCOL=47;
	private final int nWIDTH=520;
	private final int nLEN=350;
	
	public FindHits(STCWFrame parentFrame, MetaData md) 
	{
		super(parentFrame, null);
		setBackground(Color.white);
		metaData = md;
		theViewFrame = parentFrame;
		isNTsTCW = metaData.isNTsTCW();
		
		// blastn default is megablast, so do can use same defaults 
		int cpu=1;
		blastDefaults = BlastArgs.getBlastArgsDB()  + " -num_threads " + cpu;
		dmndDefaults =  BlastArgs.getDiamondArgsHit() + " --threads " + cpu; // CAS330 special for Find Hits
		
		// added after Blast is performed
		btnViewContig = Static.createButtonTab(Globals.seqDetailLabel, false);
		btnViewContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (resultTable.getSelectedRowCount() == 1)
					showSeqDetailTab();
			}
		});
		
		pnlRealMain = Static.createPagePanel();
		pnlRealMain.setBorder(null);
		pnlRealMain.add(Box.createVerticalStrut(20));
	
	// Header: Above input box
		JPanel row = Static.createRowPanel();
		JLabel lblBlast = Static.createLabel("Search database sequences");
		lblBlast.setFont(new Font("Verdana",Font.PLAIN,18));
		row.add(lblBlast);
		row.add(Box.createHorizontalStrut(150));
		
		row.add(Box.createHorizontalGlue());
		// CAS339 add two level help
		final JPopupMenu popup = new JPopupMenu();
		popup.add(new JMenuItem(new AbstractAction("Find Hits") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theViewFrame, "Find Hits", helpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on Find hits help"); }
			}
		}));
		popup.add(new JMenuItem(new AbstractAction("Search Parameters") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				try {
					UserPrompt.displayHTMLResourceHelp(theViewFrame, "Search Parameters", searchHelpHTML);
				} catch (Exception er) {ErrorReport.reportError(er, "Error on Find hits help"); }
			}
		}));
		JButton btnHelp = Static.createButtonHelp("Help...", true);
		btnHelp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
		btnHelp.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
		row.add(btnHelp);
		pnlRealMain.add(row);
		pnlRealMain.add(Box.createVerticalStrut(15));
	
	// Query
		JPanel cntlPanel = Static.createPagePanel();
		cntlPanel.setBorder(null);
				
		row = Static.createRowPanel();
		row.add(new JLabel("Query: Amino acid or nucleotide sequence(s) - FASTA format"));
		row.add(Box.createHorizontalStrut(30));
		
		JButton btnClear = Static.createButton("Clear");
		btnClear.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					inputSeq.setText("");
				}
		});
		row.add(btnClear); row.add(Box.createHorizontalStrut(5));
		
		JButton btnPaste = Static.createButton("Paste");
		btnPaste.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					try {
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					    String p = (String) clipboard.getData(DataFlavor.stringFlavor);
					    inputSeq.setText(p);
					} 
					catch (Exception e) {ErrorReport.prtReport(e, "Getting data from clipboard");}
				}
		});
		row.add(btnPaste);
		cntlPanel.add(row);
		cntlPanel.add(Box.createVerticalStrut(2));

	// Input box
		row = Static.createRowPanel();
		inputSeq = new JTextArea(5,nCOL); // #row, #col
		inputSeq.setMinimumSize(inputSeq.getPreferredSize());
		JScrollPane sp = new JScrollPane(inputSeq);
		Border border = BorderFactory.createLineBorder(Color.BLACK);
		inputSeq.setBorder(border);
		inputSeq.setEditable(true);
		inputSeq.setFont(new Font("Verdana", Font.PLAIN, 14));
		inputSeq.setLineWrap(true);
		
		row.add(sp);
		row.add(Box.createHorizontalGlue());
		cntlPanel.add(row);
		cntlPanel.add(Box.createVerticalStrut(10));
	
	// Subject: First row below input box
		row = Static.createRowPanel();
		row.add(Static.createLabel("Subject: ")); row.add(Box.createHorizontalStrut(2));	
		ButtonGroup dbType = new ButtonGroup();
	
		ntSeqCheck = Static.createRadioButton("NT-Seqs", false);
		ntSeqCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(true, false, false);		
		}});
		if (isNTsTCW) {
			dbType.add(ntSeqCheck);
			row.add(ntSeqCheck); row.add(Box.createHorizontalStrut(2));	
		}	

		String label = (isNTsTCW) ? "AA-ORFs" : "AA-Seqs";  // CAS313 
		aaSeqCheck = Static.createRadioButton(label, true); // CAS313 make this default
		aaSeqCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(false, true, false);		
		}});
		dbType.add(aaSeqCheck);
		row.add(aaSeqCheck); row.add(Box.createHorizontalStrut(2));	
		
		aaDbCheck = Static.createRadioButton("AA-DB", false);
		aaDbCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(false, false, true);		
		}});
		dbType.add(aaDbCheck);
		row.add(aaDbCheck); row.add(Box.createHorizontalStrut(2));	
	
		btnDBfind = Static.createButtonFile("...", false);
		btnDBfind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				FileRead fc = new FileRead("FindHit", FileC.bDoVer, FileC.bNoPrt); // CAS316
				if (fc.run(btnDBfind, "AA-DB",  FileC.dANNO, FileC.fFASTA)) { 
					dbSelectName = fc.getRelativeFile();
					txtDBname.setText(FileC.removePath(dbSelectName));
				}		
		}});
		row.add(btnDBfind); row.add(Box.createHorizontalStrut(2));	
		
		txtDBname = Static.createLabel(dbSELECT, false);
		row.add(txtDBname); 
		cntlPanel.add(row);
		cntlPanel.add(Box.createVerticalStrut(10));
			
	// Search: Second row below input box	
		row = Static.createRowPanel();
		blastCheck = Static.createRadioButton("Blast", false);
		blastCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableSearch(true, false);		
		}});
		row.add(blastCheck); row.add(Box.createHorizontalStrut(2));
		
		dmndCheck = Static.createRadioButton("Diamond", true);
		dmndCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableSearch(false, true);		
		}});
		row.add(dmndCheck); row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup pgmType = new ButtonGroup();
		pgmType.add(dmndCheck);
		pgmType.add(blastCheck);
		
		row.add(Static.createLabel("Parameters: "));
		txtParams = Static.createTextField(dmndDefaults, 35);
		row.add(txtParams);
		
		cntlPanel.add(row);
		cntlPanel.add(Box.createVerticalStrut(10));
		
	// Output format: Third row below input box	
		row = Static.createRowPanel();
		row.add(Static.createLabel("Output format: "));
	
		tabCheck = Static.createRadioButton("Tabular", true);
		row.add(tabCheck);
	
		longCheck = Static.createRadioButton("Long", true);
		row.add(longCheck);
		
		ButtonGroup outType = null;
		outType = new ButtonGroup();
		outType.add(tabCheck);
		outType.add(longCheck);
		tabCheck.setSelected(true);
		
		row.add(Box.createHorizontalStrut(30));
	
		JButton btnBlast = Static.createButtonRun("RUN SEARCH", true);
		btnBlast.addActionListener(
			new ActionListener() 
			{
				public void actionPerformed(ActionEvent event) 
				{
					Thread theThread = new Thread(new Runnable() {
						public void run() {
						    try {
						    	runSearch();
						    } catch(Exception e) {ErrorReport.prtReport(e, "Running blast");}
						}
					});
					theThread.start();
				}
			}
		);
		row.add(btnBlast);
		row.add(Box.createHorizontalStrut(3));
		
		traceCheck = Static.createCheckBox("Trace", false);
		row.add(traceCheck);
		
		row.add(Box.createHorizontalStrut(30));
		JButton btnReset = Static.createButton("Reset");
		btnReset.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					reset();
				}
		});
		row.add(btnReset);
		
		row.add(Box.createHorizontalGlue());
		cntlPanel.add(row);
		cntlPanel.add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		createStatusBar();
		row.add(txtStatus);
		cntlPanel.add(row);
		
		pnlRealMain.add(cntlPanel);
		pnlRealMain.add(Box.createVerticalStrut(10));
		
		resultSection = Static.createPagePanel();		
		resultSection.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		pnlRealMain.add(resultSection);
		pnlRealMain.add(Box.createVerticalGlue());
		add(pnlRealMain);
	}
	private void createStatusBar() {
		txtStatus = new JTextField(40);
		txtStatus.setAlignmentX(CENTER_ALIGNMENT);
		txtStatus.setHorizontalAlignment(JTextField.LEFT);
		txtStatus.setEditable(false);
		txtStatus.setBackground(Globals.BGCOLOR);
		txtStatus.setBorder(BorderFactory.createEmptyBorder());
		Dimension dimStat = txtStatus.getPreferredSize();
		dimStat.width = 500;
		txtStatus.setPreferredSize(dimStat);
		txtStatus.setMaximumSize(txtStatus.getPreferredSize());
		txtStatus.setText("The results will be displayed below.");
	}
	/****************************************************************
	 * runs the search and displays the results
	 */
	private void runSearch() {		
		if (dmndCheck.isSelected() && !BlastArgs.isDiamond()) { // CAS316
			showErr("Diamond executable not available");
			return;
		}
		if (blastCheck.isSelected() && !BlastArgs.isBlast()) {
			showErr("Blast executable not available");
			return;
		}
		String pgm = (dmndCheck.isSelected()) ? "Diamond" : "Blast";
		
		if (blastPathPanel != null) blastPathPanel.removeAll();
		resultSection.removeAll();
		pnlRealMain.revalidate();
		resultTable = null;

		baseDir = new File(Globalx.rHITDIR); 
		if(!baseDir.exists()) baseDir.mkdir();
		
	// Create the database to search against
		String ID = theViewFrame.getdbID();
		if (!createFastaDB(ID)) return; // Problem shown in calling routine
		
	// Determine if the input sequence is nucleotide or protein
		String wholeSeq = inputSeq.getText().trim();
		
		if (!wholeSeq.startsWith(">")) wholeSeq = ">Input\n" + wholeSeq;
		
		String[] seqs = wholeSeq.split("\\n");
		
		if (seqs.length<=1 || seqs[1].trim().length()==0) { // CAS313
			showErr("No Query sequence");
			return;
		}
		
		if (BlastArgs.isNucleotide(seqs[1])) {
			isQueryAA = false;
			showStatus("Query is nucleotide sequence - starting " + pgm + "...");
		}
		else if (BlastArgs.isProtein(seqs[1])) {
			isQueryAA = true;
			showStatus("Query is protein sequence - starting " + pgm + "...");		
		}
		else {
			isQueryAA = true;
			showStatus("Sequence type is ambiguous -- try protein search - starting " + pgm + "...");
		}
		
	// Write query to file	
		File queryFH=null;
		try {
			queryFH = new File(baseDir, ID+".input.fa");
			if (queryFH.exists()) queryFH.delete();
			// CAS330 queryFile.createNewFile();
			
			if (traceCheck.isSelected()) Out.prt("Writing file: " + queryFH.getCanonicalPath()); // CAS405
			BufferedWriter w = new BufferedWriter(new FileWriter(queryFH));
			w.write(wholeSeq); w.newLine();
			w.close();
		} catch (Exception e) {ErrorReport.prtReport(e, "Writing input.fa"); showErr("Failed writing input"); return;}

	// Setting for search
		boolean bIsTabOutput = tabCheck.isSelected();
		String suffix = (bIsTabOutput) ? RESULTS6 : RESULTS0;
		String resultFile = baseDir + "/" + ID + "." + pgm.substring(0,2) + suffix;
		outFH = new File(resultFile);
		if (outFH.exists()) outFH.delete(); // CAS313
		
		String dbPath = 	new File(dbFileName).getAbsolutePath();
		String queryPath = 	queryFH.getAbsolutePath();
		String outPath = 	outFH.getAbsolutePath();
		
		dbPath = 	FileC.removeRootPath(dbPath); // CAS316
		queryPath = FileC.removeRootPath(queryPath);
		outPath = 	FileC.removeRootPath(outPath);
		
		isSubjAA = (ntSeqCheck.isSelected()) ? false : true;
	
		String params = txtParams.getText();
		
	// Create command
		String action;
		if (isSubjAA) 	action = (isQueryAA) ? "blastp" : "blastx";
		else			action = (isQueryAA) ? "tblastn" : "blastn";
		
		String blastCmd = "";
		if (blastCheck.isSelected()) {
			blastCmd = runSetupBlast(action, dbPath, queryPath, outPath, params, bIsTabOutput);
		}
		else {
			blastCmd = runSetupDmnd(action, dbPath, queryPath, outPath, params, bIsTabOutput);
		}
		if (blastCmd==null || blastCmd.equals("")) return;
		
	// Execution	
		try {
			long startTime = Out.getTime();
			if (traceCheck.isSelected()) Out.prt("Executing: " + blastCmd);
			
			String [] x = blastCmd.split("\\s+");
			Process p = Runtime.getRuntime().exec(x); // CAS405 Java20
			p.waitFor();
			
			if (!outFH.isFile() || outFH.length()==0) { // CAS330 add check on length
				showErr("No resulting output file (see Help)");
				if (traceCheck.isSelected())  Out.prt("No resulting file");
				return;
			}
			if (traceCheck.isSelected())  Out.PrtMsgTime("Complete search", startTime);
			
			int nrows = 0;
			BufferedReader br = new BufferedReader(new FileReader(outFH));
			while (br.ready()) {
				if (!br.readLine().trim().equals("")) {
					nrows++;
					break;
				}
			}
			br.close();
			if (nrows == 0) {
				showErr("No results found (see Help).");
				if (traceCheck.isSelected())  Out.prt("Search failed to produce results");
				return;
			}	
		} catch (Exception e) {ErrorReport.prtReport(e, "Execution"); showErr("Failed execution"); return;}
		
	// Create table of output
		runResultRow(bIsTabOutput);
		if (bIsTabOutput) runReadTab(pgm, resultFile);
		else runReadLong(pgm, resultFile);
		
		pnlRealMain.revalidate();
	}
	
	private boolean createFastaDB(String ID) {
	try {
		boolean bHaveDB=false;
		int cnt=0;
		
		if (seqSet.size()==0) { // get all seqIDs for showSeqDetail (need for AAdb)
			DBConn mDB = theViewFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("select contigID from contig");
			while (rs.next()) seqSet.add(rs.getString(1)); // For View Sequence
			rs.close(); mDB.close();
		}
		
		if (ntSeqCheck.isSelected()) {
			isSubjAA=false; 
			dbFileName = baseDir + "/" + ID + ".ntSeq.fa";
			
			File seqFH = new File(dbFileName);
			bHaveDB = (seqFH.isFile() && seqFH.length()>0) ? true : false;
			if (bHaveDB) return true;
			
			// Write file of sequences
			Out.prt("Create " + dbFileName);
			DBConn mDB = theViewFrame.getNewDBC();
			int nclones = mDB.executeCount("select count(*) from contig");		
			showStatus("Writing " + nclones + " sequences to file ....");
			
			// seqFH.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFH));
			
			ResultSet rs1 = mDB.executeQuery("select contigid, consensus from contig");
			while (rs1.next()) {
				String name = rs1.getString(1);
				String seq =  rs1.getString(2);
				w.write(">" + name);
				w.newLine();
				w.write(seq);
				w.newLine();
				cnt++;
			}			
			w.close(); rs1.close(); mDB.close();
			
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			if (cnt==0) { // CAS330
				showErr("No sequences written to database file");
				return false;
			}
			return true;
		}
		if (aaSeqCheck.isSelected() && isNTsTCW) {
			isSubjAA=true;
			dbFileName = baseDir + "/" + ID + ".aaSeq.fa";
			File seqFile = new File(dbFileName);
			bHaveDB = (seqFile.isFile() && seqFile.length()>0) ? true : false;
			if (bHaveDB) return true;
			
			// Write file of ORF sequences
			Out.prt("Create " + dbFileName);
			DBConn mDB = theViewFrame.getNewDBC();
			int nclones = mDB.executeCount("select count(*) from contig");		
			showStatus("Writing " + nclones + " translated ORFs to file ....");
			
			// seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			ResultSet rs2 = mDB.executeQuery("SELECT contigid, o_frame, o_coding_start, o_coding_end, consensus " +
 		           " FROM contig where o_frame!=0");

			while (rs2.next()) {
				String name = rs2.getString(1);
				int fr =      rs2.getInt(2);
			    int start =   rs2.getInt(3);
			    int end =     rs2.getInt(4);
			
			    String strSeq = rs2.getString(5);
			    String orf = SequenceData.getORFtrans(name, strSeq, fr, start, end);
			     	
			    w.write(">" + name + " AAlen=" + orf.length() + " frame=" + fr);
			    w.newLine();
			    w.write(orf);
			    w.newLine(); 
			    cnt++;
			}
			w.close(); rs2.close(); mDB.close();
			
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			if (cnt==0) { // CAS330
				showErr("No translated ORFs");
				return false;
			}
			return true;
		}
		if (aaSeqCheck.isSelected() && !isNTsTCW) {
			isSubjAA=true;
			dbFileName = baseDir + "/" + ID + ".aaSeq.fa";
			File seqFile = new File(dbFileName);
			bHaveDB = (seqFile.isFile() && seqFile.length()>0) ? true : false;
			if (bHaveDB) return true;
			
			// Write file of ORF sequences
			Out.prt("Create " + dbFileName);
			DBConn mDB = theViewFrame.getNewDBC();
			int nclones = mDB.executeCount("select count(*) from contig");		
			showStatus("Writing " + nclones + "  AA sequences to file ....");
			
			seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			ResultSet rs2 = mDB.executeQuery("SELECT contigid, consensus FROM contig");

			while (rs2.next()) {
				String name = rs2.getString(1);
			    String strSeq = rs2.getString(2);
			     	
			    w.write(">" + name + " AAlen=" + strSeq.length());
			    w.newLine();
			    w.write(strSeq);
			    w.newLine(); 
			    cnt++;
			}
			w.close(); rs2.close(); mDB.close();
			
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			if (cnt==0) { // CAS330
				showErr("No sequences written");
				return false;
			}
			return true;
		}
		if (aaDbCheck.isSelected()) {
			isSubjAA=true; 
			
			dbFileName = dbSelectName;
			if (dbFileName.trim().equals("")) {
				showStatus("Select ... to select the database");
				return false;
			}
			File dbFile = new File(dbFileName);
			bHaveDB = (dbFile.isFile()) ? true : false;
			if (!bHaveDB){
				showStatus("Select ... to select the database");
				return false;
			}
			return true;
		}
		Out.PrtError("TCW error: no valid option for creating database");
	} catch (Exception e) {ErrorReport.prtReport(e, "Create database to search against");}	
	  return false;
	}
	/**********************************************************
	 * Read results and format for display
	 */
	private void runReadTab(String pgm, String resultFile) {
	try {
		int nrows = 0, ncols = 0;
		BufferedReader br = new BufferedReader(new FileReader(outFH));
		while (br.ready()) // Count number of rows and cols for data array
		{
			String line = br.readLine();
			if (ncols == 0)
				ncols = line.split("\\s+").length;
			nrows++;
		}
		br.close(); 
		if (ncols != 12) {
			showErr("Count not read tabular result file");
			return;
		}
		showStatus(nrows + " " + pgm + " results - output in " + resultFile);
		
		String[] colNames = new String[ncols];
		for (int i = 0; i < ncols; i++) colNames[i]="";
		
		colNames[0] = "Query"; 
		colNames[1] = "Subject";  
		colNames[2] = "%Sim";
		colNames[3] = "Align len";
		colNames[4] = "Mismatch";
		colNames[5] = "Gaps";
		colNames[6] = "Q.start";
		colNames[7] = "Q.end";
		colNames[8] = "S.start";
		colNames[9] = "S.end";
		colNames[10] = "E-value";
		colNames[11] = "Bit";
	
	// read into data array
		String[][] data = new String[nrows][ncols];
		nrows = 0;
		br = new BufferedReader(new FileReader(outFH));
		while (br.ready()) 
		{
			data[nrows] = br.readLine().split("\\s+");
			nrows++;
		}
		br.close(); 
		resultTable = new JTable(data,colNames);
		resultTable.setBorder(BorderFactory.createLineBorder(Color.black));
		resultTable.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		resultTable.setAutoCreateRowSorter(true);
		resizeColumns(resultTable);
		
		resultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				btnViewContig.setEnabled( resultTable.getSelectedRowCount() == 1 );
			}
		});
		
		JScrollPane sPane = new JScrollPane(resultTable); 
		int tHeight = resultTable.getPreferredSize().height;
		int tWidth = resultTable.getPreferredSize().width;
		
		Dimension scrollDim = new Dimension(Math.min(tWidth,nWIDTH),Math.min(tHeight+50, nLEN));
		sPane.setPreferredSize(scrollDim);
		
		resultSection.add(sPane);
		resultSection.add(Box.createVerticalStrut(5));
		resultSection.add(resultRow);
		resultSection.add(Box.createVerticalGlue());
		
	} catch (Exception e) {ErrorReport.prtReport(e, "Reading search tab file"); }
	}
	
	private void runReadLong(String pgm, String resultFile) {
	try {
		int nrows=0;
		StringBuffer sb = new StringBuffer();
		BufferedReader br = new BufferedReader(new FileReader(outFH));
		while (br.ready())
		{
			String line = br.readLine();
			sb.append(line + "\n");
			if (line.startsWith(">")) nrows++;
		}
		br.close();
		showStatus(nrows +  " " + pgm + " results");
		
		JTextArea messageArea = new JTextArea(25,nCOL); // row, col

		JScrollPane sPane = new JScrollPane(messageArea); 
		Dimension scrollDim = new Dimension(nWIDTH, nLEN);  
		sPane.setPreferredSize(scrollDim);

		messageArea.setFont(new Font("monospaced", Font.BOLD, 12));
		messageArea.setEditable(false);
		messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		messageArea.setMaximumSize(messageArea.getPreferredSize());
		messageArea.setText(sb.toString());
		
		resultSection.add(sPane);
		resultSection.add(Box.createVerticalStrut(5));
		
		resultSection.add(resultRow);
		resultSection.add(Box.createVerticalGlue());
	} catch (Exception e) {ErrorReport.prtReport(e, "Reading search long file"); }
	}		
	
	private void runResultRow(boolean bIsTabOutput) {
		JPanel row = Static.createRowPanel();
		if (bIsTabOutput)
		{
			row.add(btnViewContig);
			row.add(Box.createHorizontalStrut(50));
		}
		
		JButton btnClean = Static.createButton("Delete search files", true);
		btnClean.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					deleteFiles(baseDir);
		}});	
		row.add(btnClean);
		resultRow = row;
	}
	/*****************************************************
	Diamond can ONLY format protein database
	blastp - Align protein query sequences against a protein reference database.
	blastx - Align translated DNA query sequences against a protein reference database.
	********************************************************/
	private String runSetupDmnd(String action,
			String dbPath, String queryPath, String outPath, String params, boolean isTab) {
		try {
			if (!action.equals("blastp") && !action.equals("blastx")) {
				showErr("To use diamond, the subject must be a protein (AA-ORF or AA-DB).");
				return null;
			}
			
			// CAS330 if (seqPath.endsWith(".dmnd")) seqPath = seqPath.substring(0, seqPath.indexOf(".dmnd"));
			
			String formatFile = dbPath + ".dmnd"; 
			if (new File(formatFile).exists()) {
				if (traceCheck.isSelected()) Out.prt("Use formated file: " + formatFile);
			}
			else {
				String cmd = BlastArgs.getDiamondFormat(dbPath, dbPath);
				if (traceCheck.isSelected()) Out.prt("Executing: " + cmd);
				String [] x = cmd.split("\\s+");
				Process pFormatDB = Runtime.getRuntime().exec(x);
				pFormatDB.waitFor();
			}
			String diamondPath = BlastArgs.getDiamondPath() + " " + action + " ";
    		String args = " -q " + queryPath + " -d " + dbPath + ".dmnd -o " + outPath; // CAS330 add .dmnd for 2.0.11
    		String fmt =  (isTab) ? " --outfmt 6 " : " --outfmt 0 ";
    				
    		return diamondPath + args + fmt + params;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Format for diamond"); return null;}
	}
	/*****************************************************************
	 * Blast is very slow if ONLY the query is formatted, so its done opposite of diamond
	 */
	private String runSetupBlast(String action, String dbPath, String queryPath, String outPath, 
			String params, boolean isTab) {
		try {
			boolean doFormat=true;
			if (dbPath.endsWith(".gz")) { 
				String file = dbPath.substring(dbPath.lastIndexOf("/"));
				showErr("For blast, the file may not be gzipped (" + file + ")");
				return null;
			}
			if (queryPath.endsWith(".gz")) {
				String file = dbPath.substring(dbPath.lastIndexOf("/"));
				showErr("For blast, the file may not be gzipped (" + file + ")");
				return null;
			}
			if (isSubjAA)
			{
				File phr = (new File(dbPath + ".phr"));
				File pin = (new File(dbPath + ".pin"));
				File psq = (new File(dbPath + ".psq"));
								
				if(phr.exists() && pin.exists() && psq.exists()) doFormat=false;
			}
			else
			{
				File nhr = (new File(dbPath + ".nhr"));
				File nin = (new File(dbPath + ".nin"));
				File nsq = (new File(dbPath + ".nsq"));
								
				if(nhr.exists() && nin.exists() && nsq.exists()) doFormat=false;		
			}
			if (doFormat) {
				String cmd = (isSubjAA ? BlastArgs.getFormatp(dbPath) : BlastArgs.getFormatn(dbPath));
				if (traceCheck.isSelected()) Out.prt("Executing: " + cmd);
				String [] x = cmd.split("\\s+");
				Process pFormatDB = Runtime.getRuntime().exec(x);
				pFormatDB.waitFor();
			}
			String blastPath = BlastArgs.getBlastPath() + action; // CAS303
			
			String args =  " -query " + queryPath + " -db " + dbPath + " -out " + outPath +  " ";
			  
		    String fmt = (isTab) ? " -outfmt 6 " : " -outfmt 0 ";
		   
		    return blastPath + args + fmt + params;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Format for blast"); return null;}
	}
	
	private void setEnableDB(boolean seq, boolean orf, boolean db) {
		ntSeqCheck.setSelected(seq);
		aaSeqCheck.setSelected(orf);
		aaDbCheck.setSelected(db);
		
		txtDBname.setEnabled(db);
		btnDBfind.setEnabled(db);
		
		if (seq && isNTsTCW) {
			if (dmndCheck.isSelected()) setEnableSearch(true, false);
			blastCheck.setSelected(true);
			dmndCheck.setEnabled(false);	
		}
		else {
			dmndCheck.setEnabled(true);	
		}
	}
	private void setEnableSearch(boolean blast, boolean dmnd) {
		if (dmnd) {
			blastDefaults = txtParams.getText();
			txtParams.setText(dmndDefaults);
		}
		else {
			dmndDefaults = txtParams.getText();
			txtParams.setText(blastDefaults);
		}
	}
	private void reset() {
		dmndCheck.setSelected(true);  setEnableSearch(false, true);
		aaSeqCheck.setSelected(true); setEnableDB(false, true, false);
		
		blastDefaults = BlastArgs.getBlastArgsDB()  + " -num_threads 1";
		dmndDefaults =  BlastArgs.getDiamondArgsHit() + " --threads 1"; 
		
		txtParams.setText(dmndDefaults); 
	}
	
	private void deleteFiles(File dir){
		int cnt=0, cntResults=0;
		String [] suffix = {
				".phr", ".pin", ".psq", ".nhr", ".nhr", ".nin", ".nsq", 
				".pdb", ".pot", ".ptf", ".pto", ".ndb", ".not", ".ntf", ".nto", // CAS313
				".dmnd", ".fa"};
		for (File f : dir.listFiles()) {
			if (f.getName().endsWith(RESULTS6) || f.getName().endsWith(RESULTS0))  {
				cntResults++;
			}
			else {
				for (int i=0; i<suffix.length; i++) {
					if (f.getName().endsWith(suffix[i])) {
						f.delete();
						cnt++;
						break;
					}
				}
			}
		}
		showStatus("Search files deleted: " + cnt + "     Result files not deleted: " + cntResults);
	}
	private void showSeqDetailTab ( ) {
		if (resultTable == null) return;
		
		int nRow = resultTable.getSelectedRow();
		
		if (nRow < 0)return;
		
		String qName = resultTable.getValueAt(nRow, 0).toString();
		String sName = resultTable.getValueAt(nRow, 1).toString();
		String strName="";
		if (seqSet.contains(sName)) strName = sName; // CAS305 switched s and q
		else if (seqSet.contains(qName)) strName = qName;
		else {
			showStatus("Neither query or subject is a STCW seqID");
			return;
		}
		theViewFrame.addSeqDetailPage(strName, this, nRow);
	}
	private void showErr(String err)
	{
		txtStatus.setText("");
		resultSection.add(new JLabel(err));
		resultSection.revalidate();
	}
	private void showStatus(String msg) {
		if (traceCheck.isSelected()) Out.prt(msg);
		txtStatus.setText(msg);
	}
	
	/**
     * resizes the columns in a JTable based on the data in that table. data
     * scanned is limited to the first 25 rows.
     */
    private void resizeColumns(JTable table) 
    {

        final TableModel model = table.getModel();
        final int columnCount = model.getColumnCount();
        final ArrayList <Integer> charactersPerColumn = new ArrayList <Integer>();

        for (int col = 0; col < columnCount; col++) {
            charactersPerColumn.add(0);
        }

        // scan first 25 rows
        final int rowsToScan = model.getRowCount();
        for (int row = 0; row < rowsToScan; row++) {
            for (int col = 0; col < columnCount; col++) {

                // character counts for comparison
                final int existingCharacterCount = (charactersPerColumn.get(col)).intValue();
                final Object cellValue = model.getValueAt(row, col);
                if (cellValue != null) {
                    final Integer newCharacterCount = Integer.valueOf(cellValue.toString().length());

                    // do we need to increase the character count?
                    if (newCharacterCount.intValue() > existingCharacterCount) {
                        charactersPerColumn.set(col, newCharacterCount);
                    }
                }

            }
        }

        // prepare the table for column resizing
        final TableColumnModel columnModel = table.getColumnModel();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // set maximum character counts
        final Integer maximumCharacterCount = 30; // CAS304 Integer.valueOf(24);
        for (int col = 0; col < columnCount; col++) {
            final int existingCharacterCount = (charactersPerColumn.get(col)).intValue();
            if (existingCharacterCount > maximumCharacterCount.intValue()) {
                charactersPerColumn.set(col, maximumCharacterCount);
            }
        }

        // set column widths
        for (int col = 0; col < columnCount; col++) {
            final int existingCharacterCount = (charactersPerColumn.get(col)).intValue();
            final int columnWidth = 18 + (existingCharacterCount * 7);
            columnModel.getColumn(col).setPreferredWidth(columnWidth);
        }        
        
    }
	private JPanel pnlRealMain = null;
	private JTextField txtStatus = null;
	private JTextArea inputSeq = null;
	
	private JRadioButton ntSeqCheck=null, aaSeqCheck=null, aaDbCheck=null;
	private JLabel txtDBname = null;
	private JButton btnDBfind = null;
	
	private JRadioButton blastCheck = null, dmndCheck = null;
	private JTextField txtParams = null;
	private JRadioButton tabCheck = null, longCheck = null;
	private JCheckBox traceCheck = null;
	
	private JPanel blastPathPanel = null;
	
	private JPanel resultSection = null, resultRow = null;
	
	private JTable resultTable = null;
	
	private final JButton btnViewContig; // have to do this to use in action handler
	private static File baseDir = null;
	
	private File outFH = null;
	private MetaData metaData = null;
	private STCWFrame theViewFrame = null;
	private String blastDefaults="", dmndDefaults="", dbFileName="", dbSelectName="";
	private boolean isNTsTCW=false, isSubjAA=false, isQueryAA=false;
	private HashSet <String> seqSet = new HashSet <String> ();
}
