package cmp.viewer.panels;

/****************************************************
 * Blast tab - runs blast and displays results
 * This is copied and adapted from sng.viewer.panel.BlastTab
 * 
 * Diamond must have a AA database
 * Blast can have AA or NT
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import cmp.viewer.MTCWFrame;
import cmp.database.Globals;

import util.methods.BlastArgs;
import util.ui.UserPrompt;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.database.DBConn;
import util.database.Globalx;

public class BlastTab extends JPanel
{
	private static final long serialVersionUID = 653192706293635582L;
	private static final String helpHTML = "html/viewMultiTCW/BlastTab.html";
	
	private final String RESULTS0 = ".results" + Globalx.TEXT_SUFFIX;
	private final String RESULTS6 = ".results" + Globalx.CSV_SUFFIX;;
	private final String dbSELECT = "Select protein database";
	private final int nCOL=47;
	private final int nWIDTH=520;
	private final int nLEN=350;
	
	public BlastTab(MTCWFrame parentFrame) 
	{
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		theParentFrame = parentFrame;
		isAAonly = (theParentFrame.getnNTdb()==0);
		
		// blastn default is megablast, so do can use same defaults 
		int cpu=1;
		blastDefaults = BlastArgs.getBlastxOptions()  + " -num_threads " + cpu;
		dmndDefaults =  BlastArgs.getDiamondOpDefaults() + " --threads " + cpu;
		
		// added after Blast is performed; not used right now
		btnViewContig = Static.createButton("Copy Selected Subject", false, Globals.FUNCTIONCOLOR);
		btnViewContig.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (resultTable.getSelectedRowCount() == 1)
					showSeqDetailTab();
			}
		});
		
		pnlRealMain = Static.createPagePanel();
		pnlRealMain.setBorder(null);
		pnlRealMain.add(Box.createVerticalStrut(20));
	
	// Above input box
		JPanel row = Static.createRowPanel();
		JLabel lblBlast = new JLabel("Search database sequences");
		lblBlast.setFont(new Font("Verdana",Font.PLAIN,18));
		row.add(lblBlast);
		row.add(Box.createHorizontalStrut(150));
		
		row.add(Box.createHorizontalGlue());
        JButton btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
        btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Find Hits", helpHTML);
			}
		});	
		row.add(btnHelp);
		pnlRealMain.add(row);
		pnlRealMain.add(Box.createVerticalStrut(15));
		
		JPanel pnlMain = Static.createPagePanel();
		pnlMain.setBorder(null);
				
		row = Static.createRowPanel();
		row.add(new JLabel("Query: Amino acid or nucleotide sequence(s) - FASTA format"));
		row.add(Box.createHorizontalStrut(30));
		
		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					inputSeq.setText("");
				}
		});
		row.add(btnClear); row.add(Box.createHorizontalStrut(5));
		
		JButton btnPaste = new JButton("Paste");
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
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(2));

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
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(10));
	
		// First row below input box
		
		row = Static.createRowPanel();
		row.add(new JLabel("Target: ")); row.add(Box.createHorizontalStrut(2));	
		ButtonGroup dbType = new ButtonGroup();
		
		ntSeqCheck = Static.createRadioButton("NT-Seqs", true);
		ntSeqCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(true, false, false);		
		}});
		if (!isAAonly) {
			dbType.add(ntSeqCheck);
			row.add(ntSeqCheck); row.add(Box.createHorizontalStrut(2));	
		}
		aaSeqCheck = Static.createRadioButton("AA-Seqs", false);
		aaSeqCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(false, true, false);		
		}});
		dbType.add(aaSeqCheck);
		row.add(aaSeqCheck); row.add(Box.createHorizontalStrut(2));	
		
		dbCheck = Static.createRadioButton("AA-DB", false);
		dbCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(false, false, true);		
		}});
		dbType.add(dbCheck);
		row.add(dbCheck); row.add(Box.createHorizontalStrut(2));	
		
		btnDBfind = Static.createButton("...", false);
		btnDBfind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setSelectedDBfile();		
		}});
		row.add(btnDBfind); row.add(Box.createHorizontalStrut(2));	
		
		txtDBname = Static.createLabel(dbSELECT, false);
		row.add(txtDBname); 
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(10));
			
	// Second row below input box	
		row = Static.createRowPanel();
		blastCheck = Static.createRadioButton("Blast", true);
		blastCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableSearch(true, false);		
		}});
		dmndCheck = Static.createRadioButton("Diamond", false);
		dmndCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableSearch(false, true);		
		}});
		
		if (!isAAonly) dmndCheck.setEnabled(false);
		ButtonGroup pgmType = new ButtonGroup();
		pgmType.add(dmndCheck);
		pgmType.add(blastCheck);
		blastCheck.setSelected(true);
		
		row.add(blastCheck);
		row.add(dmndCheck);
		row.add(Box.createHorizontalStrut(5));
		
		row.add(new JLabel("Parameters: "));
		txtParams = Static.createTextField(blastDefaults, 35);
		row.add(txtParams);
		row.add(Box.createHorizontalStrut(10));
		
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(10));
		
	// Third row below input box	
		row = Static.createRowPanel();
		row.add(new JLabel("Output format: "));
	
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
	
		JButton btnBlast = Static.createButton("RUN SEARCH", true, Globals.FUNCTIONCOLOR);
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
		JButton btnReset = new JButton("Reset");
		btnReset.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					reset();
				}
		});
		row.add(btnReset);
		
		row.add(Box.createHorizontalGlue());
		pnlMain.add(row);
		pnlMain.add(Box.createVerticalStrut(10));
		
		inputBlastPath = new FileTextField(30);
		if (!BlastArgs.foundABlast())
		{
			blastPathPanel = Static.createPagePanel();
			row = Static.createRowPanel();
			row.add(new JLabel("BLAST program directory: "));
			row.add(inputBlastPath);
			blastPathPanel.add(row);
			blastPathPanel.add(Box.createVerticalStrut(10));
			pnlMain.add(blastPathPanel);
		}
		row = Static.createRowPanel();
		createStatusBar();
		row.add(txtStatus);
		pnlMain.add(row);
		
		pnlRealMain.add(pnlMain);
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
	 * NT query -> 
	 * 		if hasNTdb, NTseqs  blastn
	 * 		AAseqs, AAdb		diamond or blast
	 * AA query
	 * 		AAseqs, AAdb		diamond or blast
	 */
	private void runSearch() throws Exception
	{		
		String pgm = (dmndCheck.isSelected()) ? "Diamond" : "Blast";
		if (blastPathPanel != null) blastPathPanel.removeAll();
		resultSection.removeAll();
		pnlRealMain.revalidate();
		resultTable = null;

		baseDir = new File(Globalx.HITDIR); 
		if(!baseDir.exists()) baseDir.mkdir();
		
	// Create database
		String ID = theParentFrame.getDBName();
		createDB(ID);
		if (dbFileName==null || dbFileName.equals("")) {
			showErr("Cannot create search database file");
			return;
		}
		
	// Determine if the input sequence is nucleotide or protein
		isQueryPR = true;
		String wholeSeq = inputSeq.getText().trim();
		if (!wholeSeq.startsWith(">")) wholeSeq = ">Input\n" + wholeSeq;
		
		String[] seqs = wholeSeq.split("\\n");
		if (BlastArgs.isNucleotide(seqs[1])) {
			isQueryPR = false;
			showStatus("Query is nucleotide sequence - starting " + pgm + "...");
		}
		else if (BlastArgs.isProtein(seqs[1])) 
				showStatus("Query is protein sequence - starting " + pgm + "...");		
		else showStatus("Sequence type is ambiguous -- try protein search - starting " + pgm + "...");
		
	// Write query to file	
		File queryFile = new File(baseDir, ID+".input.fa");
		if (queryFile.exists()) queryFile.delete();
		queryFile.createNewFile();
		BufferedWriter w = new BufferedWriter(new FileWriter(queryFile));
		w.write(wholeSeq); w.newLine();
		w.close();

	// Setting for blast
		boolean bIsTabOutput = tabCheck.isSelected();
		String suffix = (bIsTabOutput) ? RESULTS6 : RESULTS0;
		String resultFile = baseDir + "/" + ID + "." + pgm.substring(0,2) + suffix;
		outFile = new File(resultFile);
		
		String seqPath = 	new File(dbFileName).getAbsolutePath();
		String queryPath = 	queryFile.getAbsolutePath();
		String outPath = 	outFile.getAbsolutePath();
		
		isTargetPR = (ntSeqCheck.isSelected()) ? isAAonly : true;
	
		String params = txtParams.getText();
		
	// Run blast
		String action;
		if (isTargetPR) 	action = (isQueryPR) ? "blastp" : "blastx";
		else				action = (isQueryPR) ? "tblastn" : "blastn";
		
		String blastCmd = "";
		if (blastCheck.isSelected()) {
			blastCmd = runSetupBlast(action, seqPath, queryPath, outPath, params, bIsTabOutput);
		}
		else {
			blastCmd = runSetupDmnd(action, seqPath, queryPath, outPath, params, bIsTabOutput);
		}
		if (blastCmd==null || blastCmd.equals("")) return;
		
	// Execution
		
		long startTime = Out.getTime();
		if (traceCheck.isSelected()) Out.prt("Executing: " + blastCmd);
		
		Process p = Runtime.getRuntime().exec(blastCmd);
		p.waitFor();
		
		if (!outFile.isFile()) {
			showErr("Output file not found (see Help)");
			return;
		}
		if (traceCheck.isSelected())  Out.PrtMsgTime("Complete search", startTime);
		
		int nrows = 0;
		BufferedReader br = new BufferedReader(new FileReader(outFile));
		while (br.ready())
		{
			if (!br.readLine().trim().equals(""))
			{
				nrows++;
				break;
			}
		}
		br.close();
		if (nrows == 0)
		{
			showErr("No results found (see Help).");
			return;
		}	

	// Create table of output
		runResultRow(bIsTabOutput);
		if (bIsTabOutput) runReadTab(pgm, resultFile);
		else runReadLong(pgm, resultFile);
		
		pnlRealMain.revalidate();
	}
	/*********************************************************
	 * createDB
	 */
	private boolean createDB(String ID) {
	try {
		boolean bHaveDB=false;
		int cnt=0;
		
		if (ntSeqCheck.isSelected()) {
			isTargetPR=false; 
			dbFileName = baseDir + "/" + ID + ".ntSeq.fa";
			
			File seqFile = new File(dbFileName);
			bHaveDB = (seqFile.isFile()) ? true : false;
			if (bHaveDB) return true;
			
			// Write file of sequences
			DBConn mDB = theParentFrame.getDBConnection();
			int nclones = mDB.executeCount("select count(*) from unitrans");		
			showStatus("Writing " + nclones + "  sequences to file ....");
			
			seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			ResultSet rs1 = mDB.executeQuery("select UTstr, ntSeq from unitrans");
			while (rs1.next())
			{
				String name = rs1.getString(1);
				String seq =  rs1.getString(2);
				w.write(">" + name + "\n");
				w.write(seq+ "\n");
				cnt++;
			}			
			w.close(); rs1.close(); mDB.close();
			
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			return true;
		}
		if (aaSeqCheck.isSelected()) {
			isTargetPR=true;
			dbFileName = baseDir + "/" + ID + ".aaSeq.fa";
			File seqFile = new File(dbFileName);
			bHaveDB = (seqFile.isFile()) ? true : false;
			if (bHaveDB) return true;
			
			// Write file of ORF sequences
			DBConn mDB = theParentFrame.getDBConnection();
			int nclones = mDB.executeCount("select count(*) from unitrans");		
			showStatus("Writing " + nclones + "  AA sequences to file ....");
			
			seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			ResultSet rs2 = mDB.executeQuery("SELECT UTstr, aaSeq from unitrans");

			while (rs2.next()) {
				String name = rs2.getString(1);
				String seq =  rs2.getString(2);
				w.write(">" + name + "\n");
				w.write(seq + "\n");
			    cnt++;
			}
			w.close(); rs2.close(); mDB.close();
			
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			return true;
		}
		if (dbCheck.isSelected()) {
			isTargetPR=true; 
			
			dbFileName = dbSelectName;
			if (dbFileName.trim().equals("")) {
				showStatus("Select ... to select the database");
				return false;
			}
			File dbFile = new File(dbFileName);
			bHaveDB = (dbFile.isFile()) ? true : false;
			if (!bHaveDB) {
				showStatus("Select ... to select the database");
				return false;
			}
			return true;
		}
	} catch (Exception e) {ErrorReport.prtReport(e, "Create database to search against");}	
	  return false;
	}
	/**********************************************************
	 * Read results and format for display
	 */
	private void runReadTab(String pgm, String resultFile) {
	try {
		int nrows = 0, ncols = 0;
		BufferedReader br = new BufferedReader(new FileReader(outFile));
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
		br = new BufferedReader(new FileReader(outFile));
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
		BufferedReader br = new BufferedReader(new FileReader(outFile));
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
			String seqPath, String queryPath, String outPath, String params, boolean isTab) {
		try {
			if (!action.equals("blastp") && !action.equals("blastx")) {
				showErr("To use diamond, the target must be a protein (AA-ORF or AA-DB).");
				return null;
			}
			
			if (seqPath.endsWith(".dmnd")) seqPath = seqPath.substring(0, seqPath.indexOf(".dmnd"));
			
			String formatFile = seqPath + ".dmnd"; 
			if (new File(formatFile).exists()) {
				if (traceCheck.isSelected()) Out.prt("Use formated file: " + formatFile);
			}
			else {
				String cmd = BlastArgs.getDiamondFormat(seqPath, seqPath);
				if (traceCheck.isSelected()) Out.prt("Executing: " + cmd);
				Process pFormatDB = Runtime.getRuntime().exec(cmd);
				pFormatDB.waitFor();
			}
			String diamondPath = BlastArgs.getDiamondPath() + " " + action + " ";
    		String args = " -q " + queryPath + " -d " + seqPath + " -o " + outPath;
    		String fmt =  (isTab) ? " --outfmt 6 " : " --outfmt 0 ";
    				
    		return diamondPath + args + fmt + params;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Format for diamond"); return null;}
	}
	/*****************************************************************
	 * Blast is very slow if ONLY the query is formatted, so its done opposite of diamond
	 */
	private String runSetupBlast(String action, String seqPath, String queryPath, String outPath, 
			String params, boolean isTab) {
		try {
			boolean doFormat=true;
			if (seqPath.endsWith(".gz")) { 
				String file = seqPath.substring(seqPath.lastIndexOf("/"));
				showErr("For blast, the file may not be gzipped (" + file + ")");
				return null;
			}
			if (queryPath.endsWith(".gz")) {
				String file = seqPath.substring(seqPath.lastIndexOf("/"));
				showErr("For blast, the file may not be gzipped (" + file + ")");
				return null;
			}
			if (isTargetPR)
			{
				File phr = (new File(seqPath + ".phr"));
				File pin = (new File(seqPath + ".pin"));
				File psq = (new File(seqPath + ".psq"));
								
				if(phr.exists() && pin.exists() && psq.exists()) doFormat=false;
			}
			else
			{
				File nhr = (new File(seqPath + ".nhr"));
				File nin = (new File(seqPath + ".nin"));
				File nsq = (new File(seqPath + ".nsq"));
								
				if(nhr.exists() && nin.exists() && nsq.exists()) doFormat=false;		
			}
			if (doFormat) {
				String cmd = (isTargetPR ? BlastArgs.getFormatp(seqPath) : BlastArgs.getFormatn(seqPath));
				if (traceCheck.isSelected()) Out.prt("Executing: " + cmd);
				Process pFormatDB = Runtime.getRuntime().exec(cmd);
				pFormatDB.waitFor();
			}
			String blastPath = BlastArgs.getBlastPath() + action; // CAS303
			
			String args =  " -query " + queryPath + " -db " + seqPath + " -out " + outPath +  " ";
			  
		    String fmt = (isTab) ? " -outfmt 6 " : " -outfmt 0 ";
		   
		    return blastPath + args + fmt + params;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Format for blast"); return null;}
	}
	
	private void setEnableDB(boolean seq, boolean orf, boolean db) {
		ntSeqCheck.setSelected(seq);
		aaSeqCheck.setSelected(orf);
		dbCheck.setSelected(db);
		txtDBname.setEnabled(db);
		btnDBfind.setEnabled(db);
		
		if (seq && !isAAonly) {
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
		blastDefaults = BlastArgs.getBlastxOptions()  + " -num_threads 1";
		dmndDefaults =  BlastArgs.getDiamondOpDefaults() + " --threads 1";
		if (dmndCheck.isSelected()) txtParams.setText(dmndDefaults); // else, the above replace what was in the txtParams
		
		blastCheck.setSelected(true); setEnableSearch(true, false);
		ntSeqCheck.setSelected(true); setEnableDB(true, false, false);
	}
	private void setSelectedDBfile() {
		try {
			JFileChooser fc = new JFileChooser();
			fc.setCurrentDirectory(new File(Globalx.ANNODIR));

			if(fc.showOpenDialog(theParentFrame) == JFileChooser.APPROVE_OPTION) {
				String fname = fc.getSelectedFile().getCanonicalPath();
				dbSelectName = fname;
				String cur = System.getProperty("user.dir");
				if(fname.contains(cur)) fname = fname.replace(cur, "");
				if (fname.length()>65) fname = fname.substring(fname.lastIndexOf("/"));
				txtDBname.setText(fname);
			}
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error finding file");
		}		
	}
	
	private void deleteFiles(File dir)
	{
		int cnt=0, cntResults=0;
		String [] suffix = {".phr", ".pin", ".psq", ".nhr", ".nhr", ".nin", ".nsq", 
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
		Out.prt("Delete files: " + cnt + " Existing Result files: " + cntResults );
	}
	// For sTCW, this brings up the Detail; for mTCW, its just a copy
	private void showSeqDetailTab ( )
	{
		if (resultTable != null)
		{
			int nRow = resultTable.getSelectedRow();
			
			if (nRow < 0)return;
			
			String tName = resultTable.getValueAt(nRow, 1).toString();
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			cb.setContents(new StringSelection(tName), null);
		}
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
	private class FileTextField extends JPanel {
		private static final long serialVersionUID = 2509867678940815100L;
		public FileTextField(int size) {
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setBackground(Globals.BGCOLOR);
			
			txtValue = new JTextField(size);
			Dimension dTxt = txtValue.getPreferredSize(); 
			txtValue.setMaximumSize(dTxt);
			txtValue.setMinimumSize(dTxt);
			
			btnFindFile = new JButton("...");
			btnFindFile.setBackground(Globals.BGCOLOR);
			Dimension dBtn = new Dimension();
			dBtn.width = btnFindFile.getPreferredSize().width;
			dBtn.height = dTxt.height;
			
			btnFindFile.setMaximumSize(dBtn);
			btnFindFile.setMinimumSize(dBtn);
			btnFindFile.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						JFileChooser fc = new JFileChooser();
						fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

						if(fc.showOpenDialog(theParentFrame) == JFileChooser.APPROVE_OPTION) {
							txtValue.setText(fc.getSelectedFile().getPath());
						}
					}
					catch(Exception e) {
						ErrorReport.prtReport(e, "Error finding file");
					}
					
				}});
			
			add(txtValue);
			add(btnFindFile);
		}
		
		public void setEnabled(boolean enabled) { 
			txtValue.setEnabled(enabled);
			btnFindFile.setEnabled(enabled);
		}
		
		private JTextField txtValue = null;
		private JButton btnFindFile = null;
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
	
	private JRadioButton ntSeqCheck=null, aaSeqCheck=null, dbCheck=null;
	private JLabel txtDBname = null;
	private JButton btnDBfind = null;
	
	private JRadioButton blastCheck = null, dmndCheck = null;
	private JTextField txtParams = null;
	private JRadioButton tabCheck = null, longCheck = null;
	private JCheckBox traceCheck = null;
	
	private FileTextField inputBlastPath = null;
	private JPanel blastPathPanel = null;
	
	private JPanel resultSection = null;
	private JPanel resultRow = null;
	
	private JTable resultTable = null;
	
	private final JButton btnViewContig; // have to do this to use in action handler
	private static File baseDir = null;
	
	private File outFile = null;
	private MTCWFrame theParentFrame = null;
	private String blastDefaults="", dmndDefaults="", dbFileName="", dbSelectName="";
	private boolean isAAonly=false, isTargetPR=false, isQueryPR=false;
}
