package sng.viewer.panels;

/****************************************************
 * Blast tab - runs blast and displays results
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.SequenceData;
import sng.util.Tab;
import sng.viewer.STCWFrame;
import util.methods.BlastArgs;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Static;
import util.methods.Out;
import util.database.DBConn;
import util.database.Globalx;
import util.ui.TableColumnSizer;

public class BlastTab extends Tab
{
	private static final long serialVersionUID = 653192706293635582L;
	private final String RESULTS0 = ".results" + Globalx.TEXT_SUFFIX;
	private final String RESULTS6 = ".results" + Globalx.CSV_SUFFIX;;
	private final String dbSELECT = "Select protein database";
	private final int nCOL=47;
	private final int nWIDTH=520;
	private final int nLEN=350;
	
	public BlastTab(STCWFrame parentFrame, MetaData md) 
	{
		super(parentFrame, null);
		setBackground(Color.white);
		metaData = md;
		theParentFrame = parentFrame;
		isApplet = UIHelpers.isApplet();
		isSTCWdbPR = metaData.isProteinDB();
		
		// blastn default is megablast, so do can use same defaults 
		int cpu=1;
		blastDefaults = BlastArgs.getBlastxOptions()  + " -num_threads " + cpu;
		dmndDefaults =  BlastArgs.getDiamondOpDefaults() + " --threads " + cpu;
		
		// added after Blast is performed
		btnViewContig = Static.createButton("View Selected Sequence", false, Globals.FUNCTIONCOLOR);
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
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Search Help", "html/viewSingleTCW/BlastTab.html");
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
		String label = (isSTCWdbPR) ? " AA-Seqs" : "NT-Seqs";
		seqCheck = Static.createRadioButton(label, true);
		seqCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(true, false, false);		
		}});
		dbType.add(seqCheck);
		row.add(seqCheck); row.add(Box.createHorizontalStrut(2));	
	
		orfCheck = Static.createRadioButton("AA-ORFs", false);
		orfCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					setEnableDB(false, true, false);		
		}});
		if (!isSTCWdbPR) {
			dbType.add(orfCheck);
			row.add(orfCheck); row.add(Box.createHorizontalStrut(2));	
		}
		
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
		
		if (!isSTCWdbPR) dmndCheck.setEnabled(false);
		ButtonGroup pgmType = new ButtonGroup();
		pgmType.add(dmndCheck);
		pgmType.add(blastCheck);
		blastCheck.setSelected(true);
		
		boolean useDmnd = (!isApplet && BlastArgs.isDiamond());
		if (useDmnd) {
			row.add(blastCheck);
			row.add(dmndCheck);
			row.add(Box.createHorizontalStrut(5));
		}
		
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
		if (!isApplet) row.add(traceCheck);
		
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
		String ID = theParentFrame.getdbID();
		runCreateDB(ID);
		if (dbName==null || dbName.equals("")) {
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
		
		String seqPath = 	new File(dbName).getAbsolutePath();
		String queryPath = 	queryFile.getAbsolutePath();
		String outPath = 	outFile.getAbsolutePath();
		
		isTargetPR = (seqCheck.isSelected()) ? isSTCWdbPR : true;
	
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
	private boolean runCreateDB(String ID) {
	try {
		boolean bHaveDB=false;
		ResultSet rs = null;
		int cnt=0;
		
		if (seqSet.size()==0) {
			DBConn mDB = theParentFrame.getNewDBC();
			rs = mDB.executeQuery("select contigID from contig");
			while (rs.next()) seqSet.add(rs.getString(1)); // For View Sequence
		}
		if (seqCheck.isSelected()) {
			if (isSTCWdbPR) {
				isTargetPR=true; 
				dbName = baseDir + "/" + ID + ".aaSeq.fa";
			}
			else {
				isTargetPR=false; 
				dbName = baseDir + "/" + ID + ".ntSeq.fa";
			}
			
			File seqFile = new File(dbName);
			bHaveDB = (seqFile.isFile()) ? true : false;
			if (bHaveDB) return true;
			
			DBConn mDB = theParentFrame.getNewDBC();
			rs = mDB.executeQuery("select count(*) from contig");
			rs.first();
			int nclones = rs.getInt(1);			
			showStatus("Writing " + nclones + "  sequences to file ....");
			
			seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			rs = mDB.executeQuery("select contigid, consensus from contig");
			while (rs.next())
			{
				String name = rs.getString(1);
				String seq = rs.getString(2);
				w.write(">" + name);
				w.newLine();
				w.write(seq);
				w.newLine();
				cnt++;
			}			
			w.close();
			if (rs!=null) rs.close();
			mDB.close();
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			return true;
		}
		if (orfCheck.isSelected()) {
			isTargetPR=true;
			dbName = baseDir + "/" + ID + ".aaOrf.fa";
			File seqFile = new File(dbName);
			bHaveDB = (seqFile.isFile()) ? true : false;
			if (bHaveDB) return true;
			
			DBConn mDB = theParentFrame.getNewDBC();
			rs = mDB.executeQuery("select count(*) from contig");
			rs.first();
			int nclones = rs.getInt(1);			
			showStatus("Writing " + nclones + "  translated ORFs to file ....");
			
			seqFile.createNewFile();
			BufferedWriter w = new BufferedWriter(new FileWriter(seqFile));
			
			rs = mDB.executeQuery("SELECT contigid, o_frame, o_coding_start, o_coding_end, consensus " +
 		           " FROM contig where o_frame!=0");

			while (rs.next()) {
				String name = rs.getString(1);
				int fr = rs.getInt(2);
			    int start = rs.getInt(3);
			    int end = rs.getInt(4);
			
			    String strSeq = rs.getString(5);
			    String orf = SequenceData.getORFtrans(name, strSeq, fr, start, end);
			     	
			    w.write(">" + name + " AAlen=" + orf.length() + " frame=" + fr);
			    w.newLine();
			    w.write(orf);
			    w.newLine(); 
			    cnt++;
			}
			w.close();
			if (rs!=null) rs.close();
			mDB.close();
			if (traceCheck.isSelected()) Out.prt("Wrote " + cnt + " to file ");
			return true;
		}
		if (dbCheck.isSelected()) {
			isTargetPR=true; 
			
			dbName = dbSelectName;
			if (dbName.trim().equals("")) {
				showStatus("Select ... to select the database");
				return false;
			}
			File dbFile = new File(dbName);
			bHaveDB = (dbFile.isFile()) ? true : false;
			if (!bHaveDB)
			{
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
		TableColumnSizer.resizeColumns(resultTable);
		
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
		if (!isApplet) row.add(btnClean);
		resultRow = row;
	}
	/*****************************************************
	Diamond can ONLY format protein database
	blastp - Align protein query sequences against a protein reference database.
	blastx - Align translated DNA query sequences against a protein reference database.
	********************************************************/
	private String runSetupDmnd(String action,
			String seqPath, String queryPath, String outPath, 
			String params, boolean isTab) {
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
	/** CAS303
	private boolean getBlastPath() {
		try {
			if (!BlastArgs.foundABlast())
			{
				String bpath = inputBlastPath.getText().trim();
				if (bpath.equals(""))
				{
					JOptionPane.showMessageDialog(theParentFrame, 
							"Please enter the location of the BLAST programs", "", 
							JOptionPane.OK_OPTION);
					return false;
				}
				BlastArgs.evalBlastPath(bpath, "");
				if (!BlastArgs.foundABlast())
				{
					if (theParentFrame.isApplet()) {
						JOptionPane.showMessageDialog(theParentFrame, 
								"The BLAST programs were not found at:\n" + bpath + 
								"\nYour browser probably does not allow the execution of local programs", 
								"", JOptionPane.OK_OPTION);
					}
					else {
						JOptionPane.showMessageDialog(theParentFrame, 
							"The BLAST programs were not found at:\n" + bpath, "", 
							JOptionPane.OK_OPTION);
					}
					return false;
				}
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting blast path"); return false;}
	}
	**/
	private void setEnableDB(boolean seq, boolean orf, boolean db) {
		seqCheck.setSelected(seq);
		orfCheck.setSelected(orf);
		dbCheck.setSelected(db);
		txtDBname.setEnabled(db);
		btnDBfind.setEnabled(db);
		
		if (seq && !isSTCWdbPR) {
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
		seqCheck.setSelected(true); setEnableDB(true, false, false);
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
	private void showSeqDetailTab ( )
	{
		if (resultTable != null)
		{
			int nRow = resultTable.getSelectedRow();
			
			if (nRow < 0)return;
			
			String qName = resultTable.getValueAt(nRow, 0).toString();
			String tName = resultTable.getValueAt(nRow, 1).toString();
			String strName="";
			if (seqSet.contains(qName)) strName = qName;
			else if (seqSet.contains(tName)) strName = tName;
			else {
				showStatus("Neither query or target is a STCW seqID");
				return;
			}
			theParentFrame.addContigPage(strName, this, nRow);
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
		public String getText() { return txtValue.getText(); }
		
		private JTextField txtValue = null;
		private JButton btnFindFile = null;
	}
	
	private JPanel pnlRealMain = null;
	private JTextField txtStatus = null;
	private JTextArea inputSeq = null;
	
	private JRadioButton seqCheck=null, orfCheck=null, dbCheck=null;
	private JLabel txtDBname = null;
	private JButton btnDBfind = null;
	
	private JRadioButton blastCheck = null, dmndCheck = null;
	private JTextField txtParams = null;
	private JRadioButton tabCheck = null, longCheck = null;
	private JCheckBox traceCheck = null;
	
	private FileTextField inputBlastPath = null;
	private JPanel blastPathPanel = null;
	
	private JPanel resultSection = null;
	private JTable resultTable = null;
	private JPanel resultRow = null;
	
	private final JButton btnViewContig; // have to do this to use in action handler
	private static File baseDir = null;
	
	private File outFile = null;
	private MetaData metaData = null;
	private STCWFrame theParentFrame = null;
	private String blastDefaults="", dmndDefaults="", dbName="", dbSelectName="";
	private boolean isApplet=false, isSTCWdbPR=false, isTargetPR=false, isQueryPR=false;
	private HashSet <String> seqSet = new HashSet <String> ();
}
