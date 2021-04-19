package sng.runDE;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import sng.database.DBInfo;
import sng.database.Globals;
import sng.database.Overview;
import util.database.Globalx;
import util.database.HostsCfg;
import util.database.DBConn;
import util.file.FileC;
import util.file.FileRead;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.ButtonComboBox;
import util.ui.UserPrompt;

/****************************************
 * Graphical interface for computing DE (runDE)
 * v2.11 - remove built-in DE methods, make them scripts
 * v3.0.3 - change all showOptionDialog(null,.. to showOptionDialog(getInstance(); the null hide window on Java 14.
 * v3.2.1 - changed GOseq to be script. Rearrange interface. Made Remove and GO separate
 */
public class QRFrame extends JDialog implements WindowListener {
	private static final long serialVersionUID = 6227242983434722745L;
	
	public static double dispersion = 0.1;
	
	public static final String allCols = "All P-value";
	public static final String noCol = "No P-values";
	
	private final String RSCRIPT1= Globalx.RSCRIPTSDIR + "/edgeRglm.R";
	private final String RSCRIPT2= Globalx.RSCRIPTSDIR + "/goSeq.R";
	
	private final String [] SELECTIONS = { "Conditions", "Group 1", "Group 2", "Exclude" };
	private final int DEFAULT_SELECTION = 2; 
	
	private final String pValColPrefix = Globals.PVALUE; 
	private final String defPercent = "10";
	private final String defPVal = ".05";
	
	private final String BADCOLNAME = "Invalid column name, use up to 15 letters/numbers/underscores";
	private final int COLUMN_LABEL_WIDTH = 80;
	private final int COLUMN_WIDTH = 140;
	private final int NUMBER_WIDTH = 2;
	private final int FILE_WIDTH=25;
	
	private static Vector<QRFrame> openWindows = null;
	private static boolean hasChooser=false;
	/************************************************
	 * Called from QRmain to open TCW selection panel
	 */
	public QRFrame(HostsCfg h, Vector <DBInfo> list) 
	{		
		hostsObj = h;
		hasChooser=true;
		
		openWindows = new Vector<QRFrame> ();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
		
		JPanel selectPanel = createTCWdbSelectionPanel(list);
		
		try {setTitle("runDE Database Chooser ");}
		catch (Exception e){ setTitle("runDE");}
		
		setResizable(true);
		add(selectPanel);
		setWindowSettings("deframechooser");
		setVisible(true);		
	}
	
	/*********************************************************
	 * Called from QRmain or from the TCW selection panel 
	 * with the name of the TCWdb. 
	 * The DE window will open with the libraries of the specified TCWdb
	 */
	public QRFrame(HostsCfg h, DBInfo d)  throws Exception
	{
		hostsObj = h;
		dbObj = d;
		
		// if window is launched from viewSingleTCW, this has not been initialized
		if(openWindows == null) openWindows = new Vector<QRFrame> ();
		openWindows.add(this);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); 
		
		addWindowListener(this);
		
		if (!dbObj.checkDBver(hostsObj)) System.exit(-1); // CAS321
		dbSetConnection(dbObj.getdbName()); // opens mDB for this database
		
		hasGO = mDB.tableExists("go_info");
		qrProcess = new QRProcess(dbObj.getID(), mDB);
		
		createMainPanel();

		updateColumnList();

		setResizable(true);
		add(mainPanel);
		setWindowSettings("QRframemainview");
		setVisible(true);
	}
	private void setWindowSettings(final String prefix) {
		Static.centerScreen(this);
		pack();
	}
	/********************************************************
	 * XXX Grp1 against Grp2 - one execute
	 */
	private void exDeGrp1Grp2(){
		// user selected groups of libraries
		TreeSet<String> grp1 = new TreeSet <String> ();
		TreeSet<String> grp2 = new TreeSet <String> ();		
		for(int x=0; x<theLibraryNames.length; x++) {
			if (isLibSelectedAt(x, 0)) 		grp1.add(theLibraryNames[x]);
			else if (isLibSelectedAt(x, 1))	grp2.add(theLibraryNames[x]);
		}
		if (grp1.size() == 0) {
			JOptionPane.showMessageDialog(null,"Please select at least one Condition for Group 1.");
			return;
		}
		if (grp2.size() == 0) {
			JOptionPane.showMessageDialog(null,"Please select at least one Condition for Group 2.");
			return;
		}
		String colName = txtColName.getText();
		boolean addCol = true;
		if (!chkSaveCol.isSelected()) addCol=false;
		else {
			if (!colNameOK(colName, 1)) return; 
			if (!colNameUnique(colName, 1)) return;
		}

		if (addCol) {
			int ret = JOptionPane.showOptionDialog(getInstance(), // CAS303 change null to getInstance()
					"Compute " + colName + " \nContinue?",
					"Save Result in p-value column", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (ret == JOptionPane.NO_OPTION) return;
			Out.Print("\nStart DE execution - add results to column " + colName);
		}
		else {
			int ret = JOptionPane.showOptionDialog(getInstance(), 
					"You have not selected to Save results in TCW database. \nContinue?",
					"Save Result in p-value column", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (ret == JOptionPane.NO_OPTION) return;
			Out.Print("\nStart DE execution - results not added to database");
		}
	
		String pColName = pValColPrefix + colName;
		
		if (qrProcess.rStart(true)) {
			Out.Print("****************************************************************");
			Out.Print("******** Start DE execution for column: " + colName + " *********");
			Out.Print("****************************************************************");
			
			qrProcess.deRun(true, rScriptFile, filCnt, filCPM, filCPMn, disp,  pColName, addCol,  grp1, grp2);
			
			Out.Print("Complete all Group1-Group2 for " + dbObj.getID());
			qrProcess.rFinish();
		}
	}
	/***********************************************
	 * Grp1 entries against each other
	 */
	private void exDeGrp1All(){
		TreeSet<String> grp1 = new TreeSet <String> ();
		TreeSet<String> grp2 = new TreeSet <String> ();	
		
		Vector<String> libNames = new Vector<String>();
		for(int x=0; x<theLibraryNames.length; x++) {
			if (isLibSelectedAt(x, 0))
				libNames.add(theLibraryNames[x]);
		}
		if (libNames.size() == 0) {
			JOptionPane.showMessageDialog(null,"Please select at least two conditions from Group 1.");
			return;
		}
		String libs = libNames.get(0);
		for (int i= 1; i<libNames.size(); i++) libs += "," + libNames.get(i);
		String msg = "\nDo all pairs for " + libs + "\n and save to auto-named columns?";	
		int ret = JOptionPane.showOptionDialog(getInstance(), msg,
				"Confirm All Pairs", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (ret != JOptionPane.YES_OPTION) {
			Out.Print("Abort All pairs from Group 1");
			return;
		}
		chkSaveCol.setSelected(true);
		
		if (!qrProcess.rStart(true)) return;
		int ex=0;
		for(int x=0; x<libNames.size(); x++) 
		{
			grp1.clear();
			String lib1 = libNames.get(x);
			grp1.add(lib1);
			for(int x2=x+1; x2<libNames.size(); x2++) 
			{
				grp2.clear();
				String lib2 = libNames.get(x2);
				grp2.add(lib2);
				
				boolean addCol = true;			
				String pColName = colNameCreate(lib1,lib2);
		
				Out.Print("****************************************************************");
				Out.Print("******** " + (ex+1) + ". Start DE execution for column: " + pColName + " *********");
				Out.Print("****************************************************************");
				
				pColName = pValColPrefix + pColName;
				boolean rc = qrProcess.deRun((ex==0), rScriptFile, filCnt, filCPM, filCPMn, 
						disp,  pColName, addCol,  grp1, grp2);
				if (!rc) {
					Out.Print("*** Abort All pairs from Group 1");
					return;
				}
				ex++;
			}
		}
		Out.Print("Complete all pairs for " + dbObj.getID());
		qrProcess.rFinish();
	}
	/*********************************************************
	 * XXX read file of group1, group2, columnname
	 ********************************************************/
	private void exDePairFile(String fname){
		try {	
			Out.Print("\nReading " + fname);
			
			Vector <String> addLines = new Vector <String> ();
			
			chkSaveCol.setSelected(true);
			BufferedReader file = new BufferedReader(new FileReader(fname));
			String line, colSaveStr="", pColName;
			
			// read file and check
			TreeSet<String> libs = new TreeSet <String> ();
			int cnt=0, err=0, warn=0;
			while((line = file.readLine()) != null) {
				line = line.trim();
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				
				libs.clear();
				boolean good=true;
				Out.Print("Input: " + line);
				
				String [] col = line.split("\\s+");
				if (col.length != 3) {
					Out.PrtWarn("improper line - ignore: " + line);
					err++; good=false;
					continue;
				}
				
				// columns can be multiple groups delimited by :
				if (col[0].contains(":")) {
					String [] set = col[0].split(":");
					for (int i=0; i< set.length; i++) {
						libs.add(set[i]);
					}
				}
				else libs.add(col[0]); 
				
				if (col[1].contains(":")) {
					String [] set = col[1].split(":");
					for (int i=0; i< set.length; i++) {
						libs.add(set[i]);
					}
				}
				else libs.add(col[1]); 
				
				// libraries exist?
				for (String lib : libs) {
					boolean found=false;
					for(int x=0; x<theLibraryNames.length && !found; x++) 
						if (lib.equals(theLibraryNames[x])) found = true;
					if (!found) {
						Out.PrtWarn("Condition not found - ignore: " + lib);
						err++; good=false;
					}
				}
				
				// check for proper name and uniqueness
				if (!colNameOK(col[2], 2)) {
					err++; good=false;
				}
				else if (colSaveStr.contains(" " + col[2] + " ")) {
					Out.PrtWarn("Duplicate column - ignore: " + col[2]);
					err++; good=false;
				}
				else if (!colNameUnique(col[2], 2)) {
					warn++; 
				}
				
				if (good) {
					colSaveStr += " " + col[2] + " ";
					addLines.add(line);
					cnt++;
				}
			}
			file.close();
			// prompt the user
			String msg = "Found " + cnt + " good entries";
			if (warn>0) msg += " with " + warn + " overwrite (see terminal)";
			if (err>0)  msg += " and " + err + " ignored entries (see terminal)";

			int ret = JOptionPane.showOptionDialog(getInstance(), msg,
					"runDE", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		
			if (ret != JOptionPane.YES_OPTION) {
				Out.Print("Abort all pairs from file");
				return;
			}
			
			// read and compute
			TreeSet<String> grp1 = new TreeSet <String> ();
			TreeSet<String> grp2 = new TreeSet <String> ();
			
			if (!qrProcess.rStart(true)) return;
			
			for (int j=0; j<addLines.size(); j++) {
				line = addLines.get(j);
				String [] col = line.split("\\s+");
				grp1.clear(); grp2.clear();
				if (col[0].contains(":")) {
					String [] set = col[0].split(":");
					for (int i=0; i< set.length; i++) grp1.add(set[i]);
				}
				else grp1.add(col[0]); 
				
				if (col[1].contains(":")) {
					String [] set = col[1].split(":");
					for (int i=0; i< set.length; i++) grp2.add(set[i]);
				}
				else grp2.add(col[1]); 
				
				pColName = pValColPrefix + col[2];
			
				Out.Print("****************************************************************");
				Out.Print("******** " + (j+1) + ". Start DE execution for column: " + col[2] + " *********");
				Out.Print("****************************************************************");
			
				boolean rc = qrProcess.deRun((j==0), rScriptFile, filCnt, filCPM, filCPMn, 
						                      disp, pColName, true,  grp1, grp2);
				if (!rc) {
					Out.Print("*** Abort All pairs from File");
					return;
				}
			}
			Out.Print("Complete adding from file for " + dbObj.getID());
			qrProcess.rFinish();
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Cannot process file " + fname);
		}
	}
	// lib1_name lib2_nane  new_pvalue_column_name
	// seqID value
	// ...
	// read file, then call 
	private void exDeLoadFile(String fname) {
		try {
			Out.Print("\nLoad p-values from file " + fname);
			
			chkSaveCol.setSelected(true);
			TreeSet <String> grp1 = new TreeSet <String> ();
			TreeSet <String> grp2 = new TreeSet <String> ();
			String line, pColName="";
			TreeMap <String, Double> scores = new TreeMap <String, Double> ();
			
			BufferedReader file = new BufferedReader(new FileReader(fname));
			
			boolean readCol=false;
			int cnt=0;
			while((line = file.readLine()) != null) {
				line = line.trim();
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				String [] tok = line.split("\\s+");
				if (!readCol) { // first line defining columns and p-value column
					readCol=true;
					if (tok.length!=3) {
						JOptionPane.showMessageDialog(getInstance(), 
								"First line must be of format: group1 group2 column_name\nSee Help for more information", "Error", JOptionPane.PLAIN_MESSAGE);
						file.close();
						return;
					}
					String [] g1 = tok[0].split(":");
					for (String g : g1) grp1.add(g);
					String [] g2 = tok[1].split(":");
					for (String g : g2) grp2.add(g);
					pColName = tok[2];
					Out.Print("Group1=" + tok[0] + " Group2=" + tok[1] + "; P-value column=" + tok[2]);
				}
				else {
					if (tok.length!=2) {
						JOptionPane.showMessageDialog(getInstance(), 
								"Incorrect line: " + line + 
								"\nScore lines must have two values: seqID score" +
								"\nSee Help for more information", "Error", JOptionPane.PLAIN_MESSAGE);
						file.close();
						return;
					}
					String name = tok[0];
					try {
						Double d = Math.abs(Double.parseDouble(tok[1]));
						scores.put(name, d);
						cnt++;
						if (cnt%1000==0) Out.r("read " + cnt);
					}
					catch (Exception e){
						JOptionPane.showMessageDialog(getInstance(), 
								"Incorrect line: " + line + 
								"\nThe second value must be the p-value.", 
								"Error", JOptionPane.PLAIN_MESSAGE);
						file.close();
						return;
					}
				}
			}
			file.close();
			// check that these correspond to conditions (i.e. library names)
			for (String g : grp1) {
				boolean found=false;
				for (String l : theLibraryNames) {
					if (l.equals(g)) {
						found=true;
						break;
					}
				}
				if (!found) {
					JOptionPane.showMessageDialog(getInstance(), 
							"Incorrect Condition name: " + g + 
							"\nThe condition names must match those listed under 'Condition'.", 
							"Error", JOptionPane.PLAIN_MESSAGE);
					return;
				}
			}
			// P-value column ok?
			if (!colNameOK(pColName, 1)) return; 
			if (!colNameUnique(pColName, 1)) return;
			
			// check seqIDs are in database
			int cntHave=0, cntNo=0, cntAll=0;
			ResultSet rs = mDB.executeQuery("select contigid from contig");
			while (rs.next()) {
				String seqid = rs.getString(1);
				if (scores.containsKey(seqid)) cntHave++;
				else cntNo++;
				cntAll++;
			}
			if (cntHave!=cntAll) {
				String msg = "# seqIDs in file: " + scores.size() + "\n" +
						"# seqIDs in file and database: " + cntHave + "\n" +
						"# seqIDs in database but not in file: " + cntNo;
				if (!UserPrompt.showContinue("seqID inconsistencies", msg)) return;
			}
			boolean rc = qrProcess.deReadPvalsFromFile(pColName, grp1, grp2, scores, fname);
			if (!rc) {
				Out.Print("*** Abort Load p-values from file");
				return;
			}
			Out.Print("Complete adding p-values from file for " + dbObj.getID());
		}
		catch (Exception e) {ErrorReport.reportError(e, "Cannot process file " + fname);}
	}
	
	/********************************************************
	 * Creates a default column name for the libraries selected
	 * It tries to 'guess' what kind of naming scheme these are using
	 */
	private String colNameCreate(TreeSet<String> grp1, TreeSet<String> grp2, String colName) {
		String col="";
		String a="", b="";
		if (grp1.size() ==1 && grp2.size() == 1) {
			boolean isLt4 = true;
			
			for(int x=0; x<theLibraryNames.length; x++) // tries to keep them all the same length
				if (theLibraryNames[x].length() > 3) isLt4 = false;
			
			for(int x=0; x<theLibraryNames.length; x++) 
			{
				if (isLibSelectedAt(x, 0)) a = theLibraryNames[x];
				else if (isLibSelectedAt(x, 1)) b = theLibraryNames[x];
			}
			
			if (isLt4)  col = a + b;
			else {
				String aa =  (a.length()>=2) ? a.substring(0,2) : a; 
				String bb =  (b.length()>=2) ? b.substring(0,2) : b; 
				if (!aa.equals(bb) && (aa.length() + bb.length() < 6)) col = aa+bb;			
				else if (a.length() + b.length() < 6) col =  a + b;
				else {
					String aaa =  (aa.length()>=3) ? aa.substring(0,3) : aa; 
					String bbb =  (bb.length()>=3) ? bb.substring(0,3) : bb; 
					col = aaa+bbb;
				}
			}
		}
		else {
			for(int x=0; x<theLibraryNames.length; x++)  {
				if (isLibSelectedAt(x, 0)) a += theLibraryNames[x].charAt(0);
				else if (isLibSelectedAt(x, 1)) b += theLibraryNames[x].charAt(0);
			}
			col =   a + "x" + b; // can't use '_" because of L_ and LN_ columns in sTCW
		}
		if (!colNameOK(col, 2)) {
			if (colNameOK(col+"2", 2)) col += "2";
			else if (colNameOK(col+"3", 2)) col += "3";
			else return "";
		}
		return col;
	}
	// For the looping mode, since it can't use the interface selection
	private String colNameCreate(String a, String b) {
		String col="";
		boolean isLt4 = (a.length() < 4 && b.length() < 4);
		
		if (isLt4)  col = a + b;
		else {
			String aa =  (a.length()>=2) ? a.substring(0,2) : a; 
			String bb =  (b.length()>=2) ? b.substring(0,2) : b; 
			if (!aa.equals(bb) && (aa.length() + bb.length() < 6)) col = aa+bb;
			else if (a.length() + b.length() < 6) col = a + b;
			else {
				String aaa =  (aa.length()>=3) ? aa.substring(0,3) : aa; 
				String bbb =  (bb.length()>=3) ? bb.substring(0,3) : bb; 
				col = aaa+bbb;
			}
		}
		if (!colNameOK(col, 2)) {
			if (colNameOK(col+"2", 2)) col += "2";
			else if (colNameOK(col+"3", 2)) col += "3";
			else return "";
		}
		return col;
	}
	/************************************************************
	 * Column checking
	 * L__ cannot have the same name as the table crashes
	 */
	private boolean colNameOK(String colName, int msg) {// msg=0 none, 1 dialog, 2 stderr
		try {
			if (colName.length() <= 0) {
				if (msg==1) JOptionPane.showMessageDialog(null, "No column name.\nUncheck-check box to auto-create a name, enter enter in test box.");
				else if (msg==2) 
					Out.PrtWarn("No column name - ignore: " + colName);
				return false;
			}
			
			if (colName.length() > 15 || !colName.matches("[\\w]+"))
			{
				if (msg==1) JOptionPane.showMessageDialog(null, colName + " " + BADCOLNAME);
				else if (msg==2) 
					Out.PrtWarn(BADCOLNAME + " - ignore: " + colName);			
				return false;
			}
			// but P_ would be added as prefix??
			String [] L_Col = dbLoadPrefixedColumns("L__");
			for (int i=0; i<L_Col.length; i++) {
				if (L_Col[i].equals(colName)) {
					if (msg==1)
						JOptionPane.showMessageDialog(null,"Invalid: " + colName + 
						"\nPlease do not prefix a column name with L__, as that is used internally/\n");
					else if (msg==2) 
						Out.PrtWarn("Condition name exists - ignore: " + colName);
					return false;				
				}
			}
			return true;
		}
		catch (Exception e) {
			ErrorReport.reportError("checking column" + colName, e);
			return false;
		}
	}

	private boolean colNameUnique(String colName, int msg) {
		try { 
			String [] P_Col = dbLoadPrefixedColumns(Globals.PVALUE);
		
			for (int i=0; i<P_Col.length; i++) {
				if (P_Col[i].equals(colName)) {			
					if (msg==1) {
						int ret = JOptionPane.showOptionDialog(getInstance(), "Column " + colName + 
				" is already used. Do you want to overwrite the previous scores?\n" +
				"(see Help about generating column names)",
				"Existing column", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		
						if (ret != JOptionPane.YES_OPTION)
						{
							Out.Print("*** Abort Execute");
							return false;
						}
					}
					else if (msg==2)  {
						Out.Print("Exists in database - overwrite: " + colName);
						return false; 
					}
					return true;
				}
			}
			return true;
		}
		catch (Exception e) {
			ErrorReport.reportError("checking column exists" + colName, e);
			return false;
		}
	}
	/*********************************************************
	 * exGoSeq
	 */
	private void exGoSeq() {
		boolean usePercent = btnPercent.isSelected();
		String strThresh = (usePercent ? txtPercent.getText() : txtPVal.getText());
		double thresh = Double.parseDouble(strThresh);
		
		String rScriptFile = txtRfile2.getText().trim();
		if (rScriptFile.equals("")) {
			JOptionPane.showMessageDialog(null,"Enter R-script file name.");
			return;
		}
		if (!(new File(rScriptFile).exists())) {
			JOptionPane.showMessageDialog(null,"R-script file does not exist.\nFile name: " + rScriptFile);
			return;
		}
		
		if (usePercent) {
			if (thresh >= 100 || thresh <0) {
				UserPrompt.showError("Invalid percentage for 'Top' " + thresh);
				Out.PrtError("Invalid percentage " + thresh);
				return;
			}
		}
		else {
			if (thresh >= 1.0 || thresh <0.0) {
				UserPrompt.showError("Invalid cutoff for 'p-value' " + thresh);
				Out.PrtError("Invalid cutoff for 'p-value' " + thresh);
				return;
			}
		}
		String selected = cmbColGO.getSelectedItem();
		boolean doAll = selected.equals(allCols);
		
		String [] colNames = cmbColGO.getColumns(); 
		String colString="";
		for (int i=0; i<colNames.length; i++) {
			if (!colNames[i].equals(allCols)) {
				
				if (colString=="") colString=colNames[i];
				else colString += "," + colNames[i];
			}
		}
		String msg = (doAll) ? "Compute GOseq for all DE columns" : "Compute GOseq for " + selected;
		int ret = JOptionPane.showOptionDialog(getInstance(), msg + "\nContinue?",
				"GOseq", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (ret == JOptionPane.NO_OPTION) return;
		
		String col = cmbColGO.getSelectedItem();
		String[] cols2do = (doAll ? colNames : new String[]{col});
		
		if (!qrProcess.rStart(false)) return;
		int ex=1;
		for (String colName : cols2do) {
			if (colName.equals(allCols)) continue;
				
			Out.Print("****************************************************************");
			Out.Print("******** " + ex + ". Start GO enrichment for column: " + colName + " *********");
			Out.Print("****************************************************************");
			
			qrProcess.goRun((ex==1), colName, usePercent, thresh, rScriptFile);
			ex++;
		}
		Out.Print("Complete GO enrichment for " + dbObj.getID());
		qrProcess.rFinish();
	}
	/******************************************************
	 * XXX QR panel methods	
	 */
	private void createMainPanel() {
		mainPanel = Static.createPagePanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		createLibSelections(); 
		
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		createDEmethodPanel();
		
		mainPanel.add(Box.createVerticalStrut(10));
		createDEexecPanel();
		
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		createGOPanel();
		
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		createRemovePanel();
		
		mainPanel.add(Box.createVerticalStrut(10));
		
		mainPanel.add(new JSeparator());
		mainPanel.add(Box.createVerticalStrut(10));
		createExitPanel();
		
		mainPanel.setMaximumSize(mainPanel.getPreferredSize());
		mainPanel.setMinimumSize(mainPanel.getPreferredSize());
	}
	// Library Selections
	private void createLibSelections() {		
		JPanel row = Static.createRowPanel();

		colHeaders = new JLabel[SELECTIONS.length];
		for(int x=0; x<SELECTIONS.length; x++) {
			colHeaders[x] = new JLabel(SELECTIONS[x]);
			colHeaders[x].setAlignmentX(Component.CENTER_ALIGNMENT);
			
			JPanel temp = Static.createRowPanel();
			temp.add(colHeaders[x]);
			Dimension d = temp.getPreferredSize();
			d.width = COLUMN_WIDTH;
			temp.setPreferredSize(d);
			temp.setMaximumSize(d);
			temp.setMinimumSize(d);
			
			row.add(temp);
		}		
		mainPanel.add(row);
		
		int maxWidth = 0;
		rowHeaders = new JLabel[theLibraryNames.length];
		for(int x=0; x<theLibraryNames.length; x++) {
			rowHeaders[x] = new JLabel(theLibraryNames[x]);
			if(rowHeaders[x].getPreferredSize().width > maxWidth)
				maxWidth = rowHeaders[x].getPreferredSize().width;
		}
		
		for(int x=0; x<rowHeaders.length; x++) {
			Dimension d = rowHeaders[x].getPreferredSize();
			d.width = maxWidth;
			rowHeaders[x].setPreferredSize(d);
			rowHeaders[x].setMaximumSize(d);
			rowHeaders[x].setMinimumSize(d);
		}
		
		libRadio = new JRadioButton[theLibraryNames.length][];
		for(int x=0; x<theLibraryNames.length; x++) {
			libRadio[x] = new JRadioButton[SELECTIONS.length-1];
			ButtonGroup grp = new ButtonGroup();
			for(int y=0; y<libRadio[x].length; y++) {
				libRadio[x][y] = new JRadioButton();
				libRadio[x][y].setBackground(Color.WHITE);
				libRadio[x][y].setAlignmentX(Component.CENTER_ALIGNMENT);
				libRadio[x][y].setMaximumSize(libRadio[x][y].getPreferredSize());
				libRadio[x][y].setMinimumSize(libRadio[x][y].getPreferredSize());
				
				grp.add(libRadio[x][y]);
			}
		}
		for(int x=0; x<theLibraryNames.length; x++) {
			mainPanel.add(createLibRow(x));
		}
	}
	private JPanel createLibRow(int rowNum) {
		JPanel retVal = Static.createRowPanel();
		
		JPanel temp = Static.createRowPanel();
		temp.add(rowHeaders[rowNum]);
		Dimension d = temp.getPreferredSize();
		d.width = COLUMN_WIDTH;
		temp.setPreferredSize(d);
		temp.setMaximumSize(d);
		temp.setMinimumSize(d);
		retVal.add(temp);
		
		for(int x=0; x<libRadio[rowNum].length; x++) {
			temp = Static.createRowPanel();
			if (x == DEFAULT_SELECTION) libRadio[rowNum][x].setSelected(true);
			else libRadio[rowNum][x].setSelected(false);
			
			temp.add(libRadio[rowNum][x]);
			d = temp.getPreferredSize();
			d.width = COLUMN_WIDTH;
			temp.setPreferredSize(d);
			temp.setMaximumSize(d);
			temp.setMinimumSize(d);
			retVal.add(temp);
		}
		return retVal;
	}
	private boolean isLibSelectedAt(int x, int y) { return libRadio[x][y].isSelected(); }
	
	// Methods
	private void createDEmethodPanel() {
		JPanel page = Static.createPagePanel();
		page.add(new JLabel("Differential Expression") );
		page.add(Box.createVerticalStrut(3));
		
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("R-script"));
		row.add(Box.createHorizontalStrut(1));
		
		txtRfile1 = new JTextField(FILE_WIDTH);
		txtRfile1.setText(RSCRIPT1);
		txtRfile1.setMaximumSize(txtRfile1.getPreferredSize());
		row.add(txtRfile1);
		row.add(Box.createHorizontalStrut(1));
		
		btnRfile1 = new JButton("...");
		btnRfile1.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnRfile1, "R Script File", FileC.dRSCRIPTS, FileC.fR)) { // CAS316 was in-file chooser
					txtRfile1.setText(fc.getRelativeFile());
				}
			}
		});
		row.add(btnRfile1);
		page.add(row);
		page.add(Box.createVerticalStrut(3));

		// Dispersion
		row = Static.createRowPanel();
		
		filterRadio = new JRadioButton[3];
		ButtonGroup fg = new ButtonGroup();
		
		row.add(Box.createHorizontalStrut(5));
		JLabel lb = new JLabel("Pre-filter:");
		row.add(lb);
		if(lb.getPreferredSize().width < COLUMN_LABEL_WIDTH)
			row.add(Box.createHorizontalStrut(COLUMN_LABEL_WIDTH - lb.getPreferredSize().width));
		
		filterRadio[0] = getRadioPanel("Count >");	
		filterRadio[0].setBackground(Color.WHITE);
		filterRadio[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtFilCnt.setEnabled(true);
				txtFilCPM.setEnabled(false);
				txtFilCPMn.setEnabled(false);
			}
		});
		row.add(filterRadio[0]);
		fg.add(filterRadio[0]);
			
		txtFilCnt = Static.createTextField("1", NUMBER_WIDTH, false);
		row.add(txtFilCnt);
		row.add(Box.createHorizontalStrut(20));
		
		filterRadio[1] = getRadioPanel("CPM >");	
		filterRadio[1].setBackground(Color.WHITE);
		filterRadio[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtFilCnt.setEnabled(false);
				txtFilCPM.setEnabled(true);
				txtFilCPMn.setEnabled(true);
			}
		});
		row.add(filterRadio[1]);			
		fg.add(filterRadio[1]);	
		row.add(Box.createHorizontalStrut(1));
		
		txtFilCPM = Static.createTextField("1", NUMBER_WIDTH, true);
		row.add(txtFilCPM); 				
		row.add(new JLabel("for >=")); 	
		txtFilCPMn = Static.createTextField("2", NUMBER_WIDTH, true);
		row.add(txtFilCPMn);
		row.add(Box.createHorizontalStrut(20));
		
		filterRadio[2] = getRadioPanel("None");	
		filterRadio[2].setBackground(Color.WHITE);
		filterRadio[2].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtFilCnt.setEnabled(false);
				txtFilCPM.setEnabled(false);
				txtFilCPMn.setEnabled(false);
			}
		});
		row.add(filterRadio[2]);
		fg.add(filterRadio[2]);
		
		filterRadio[1].setSelected(true); 
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		// Dispersion
		row = Static.createRowPanel();
		chkDisp = new JCheckBox("Fixed dispersion");
		chkDisp.setBackground(Color.WHITE);
		chkDisp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (chkDisp.isSelected()) txtDisp.setEnabled(true);
				else  txtDisp.setEnabled(false);
			}
		});
		row.add(chkDisp);	row.add(Box.createHorizontalStrut(1));
		txtDisp = new JTextField(3);
		txtDisp.setText(String.valueOf(dispersion));
		txtDisp.setMaximumSize(txtDisp.getPreferredSize());
		row.add(txtDisp);
		chkDisp.setSelected(false);
		txtDisp.setEnabled(false);
		
		page.add(row);
		page.add(Box.createVerticalStrut(5));
		
		page.setMaximumSize(page.getPreferredSize());
		page.setMinimumSize(page.getPreferredSize());
		mainPanel.add(page);
	}

	/*************************************************
	 * XXX row starting with Execute
	 */
	private void createDEexecPanel() {
		JPanel page = Static.createPagePanel();
		page.add(new JLabel("Execute") );
		page.add(Box.createVerticalStrut(3));
		
		String [] labels = new String []{
			"All Pairs for Group 1",
			"Group 1 - Group 2",
			"All Pairs from File",
			"P-values from File"
		};
		JPanel row = Static.createRowPanel();
		btnGrp1All 	= 		new JButton(labels[0]);
		btnGrp1Grp2 = 		new JButton(labels[1]);
		btnPairFile  = 		new JButton(labels[2]);
		btnLoadPvalFile = 	new JButton(labels[3]);
		int width =  btnGrp1All.getPreferredSize().width;
		int height = btnGrp1All.getPreferredSize().height;
		Dimension dim = new Dimension(width, height);
		btnGrp1Grp2.setPreferredSize(dim);
		btnPairFile.setPreferredSize(dim);
		btnLoadPvalFile.setPreferredSize(dim);
		
	// All Pairs for Group 1
		row = Static.createRowPanel();
		btnGrp1All.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				if (getMethodsFromPanel()) {
					exDeGrp1All();
					updateColumnList();
				}
			}
		});
		row.add(btnGrp1All);
		page.add(row);
		page.add(Box.createVerticalStrut(3));
				
	// Group 1 - Group 2
		row = Static.createRowPanel();
		btnGrp1Grp2.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				if (getMethodsFromPanel()) {
					exDeGrp1Grp2();
					updateColumnList();
				}
			}
		});		
		row.add(btnGrp1Grp2);
		int w = btnGrp1Grp2.getPreferredSize().width;
		if (w < width) row.add(Box.createHorizontalStrut(width - w));
		row.add(Box.createHorizontalStrut(5));

		chkSaveCol = new JCheckBox("Save results in p-value column");
		chkSaveCol.setBackground(Color.white);
		chkSaveCol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (chkSaveCol.isSelected()) txtColName.setEnabled(true);
				else  txtColName.setEnabled(false);
			}
		});
		row.add(chkSaveCol);
		row.add(Box.createHorizontalStrut(1));
		txtColName = new JTextField(8);
		txtColName.setMaximumSize(txtColName.getPreferredSize());
		row.add(Box.createHorizontalStrut(5));
		row.add(txtColName);
		
		chkSaveCol.setSelected(false);
		txtColName.setEnabled(false);
		
		chkSaveCol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (chkSaveCol.isSelected()){
					txtColName.setEnabled(true);
					TreeSet<String> grp1 = new TreeSet <String> ();
					TreeSet<String> grp2 = new TreeSet <String> ();		
					for(int x=0; x<theLibraryNames.length; x++) {
						if (isLibSelectedAt(x, 0))      grp1.add(theLibraryNames[x]);
						else if (isLibSelectedAt(x, 1)) grp2.add(theLibraryNames[x]);
					}
					if (grp1.size() != 0 && grp2.size() != 0) {
						String cname="";
						cname = colNameCreate(grp1, grp2, cname);
						if (cname != null) txtColName.setText(cname);
					}	
				}
				else txtColName.setEnabled(false);
			}
		});
		page.add(row);
		page.add(Box.createVerticalStrut(3));
		
	// All pairs from file
		row = Static.createRowPanel();
		btnPairFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String fname = txtPairsFile.getText().trim();
				if (fname != null && !fname.equals("")) {
					if (getMethodsFromPanel()) {
						exDePairFile( fname);
						updateColumnList();
					}
				}
			}
		});
		row.add(btnPairFile);
		w = btnPairFile.getPreferredSize().width;
		if (w < width) row.add(Box.createHorizontalStrut(width - w));
		row.add(Box.createHorizontalStrut(5));
		
		txtPairsFile = new JTextField(FILE_WIDTH);
		txtPairsFile.setMaximumSize(txtPairsFile.getPreferredSize());
		row.add(txtPairsFile);
		row.add(Box.createHorizontalStrut(1));
		
		btnPairsFile = new JButton("...");
		btnPairsFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnPairsFile, "Pair File",  FileC.dPROJ, FileC.fTXT)) { // CAS316 was in-file choooser
					txtPairsFile.setText(fc.getRelativeFile());
				}
			}
		});
		row.add(btnPairsFile);
		page.add(row);
		page.add(Box.createVerticalStrut(3));
	    
	// All pvalues from file
	    row = Static.createRowPanel();
		btnLoadPvalFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String fname = txtPvalFile.getText().trim();
				if (fname != null && !fname.equals("")) {
					exDeLoadFile( fname);
					updateColumnList();
				}
			}
		});
		row.add(btnLoadPvalFile);
		w = btnLoadPvalFile.getPreferredSize().width;
		if (w < width) row.add(Box.createHorizontalStrut(width - w));
		row.add(Box.createHorizontalStrut(5));
		
		txtPvalFile = new JTextField(FILE_WIDTH);
		txtPvalFile.setMaximumSize(txtPvalFile.getPreferredSize());
		row.add(txtPvalFile);
		row.add(Box.createHorizontalStrut(1));
		
		btnPvalFile = new JButton("...");
		btnPvalFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnPvalFile, "P-value File", FileC.dPROJ, FileC.fTXT)) { // CAS316 was in-file choooser
					txtPvalFile.setText(fc.getRelativeFile());
				}
			}
		});
		row.add(btnPvalFile);
		page.add(row);
	    
		mainPanel.add(page);
	}
	// Select Column and Remove button
	private void createRemovePanel() {
		JPanel row = Static.createRowPanel();
		row.add(new JLabel("Remove p-value column(s)"));
		mainPanel.add(row); mainPanel.add(Box.createVerticalStrut(5));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(5));
		btnRemoveColumns = Static.createButton("Remove", false, Globals.BGCOLOR);
		btnRemoveColumns.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {		
					String column = cmbColRm.getSelectedItem(); // CAS304 remove (String)
					if (!column.equals(allCols)) {
						int ret = JOptionPane.showOptionDialog(getInstance(), "Remove " + column + "?",
								"Remove column", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (ret == JOptionPane.YES_OPTION) 
							dbRemoveColumn(column);
					}
					else {
						int ret = JOptionPane.showOptionDialog(getInstance(), "Remove all p-value columns?",
								"Remove column", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
						if (ret == JOptionPane.YES_OPTION)
							dbRemoveColumnAll();
					}
					Out.Print("Completed removal ");
				}
				catch(Exception e) {ErrorReport.prtReport(e, "Error removing method column");}
			}
		});
		row.add(btnRemoveColumns);row.add(Box.createHorizontalStrut(5));
		
		cmbColRm = new ButtonComboBox();
		cmbColRm.setEnabled(false);
		cmbColRm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int n = cmbColRm.getItemCount()	;
				if (n == 1 ) btnRemoveColumns.setEnabled(false);
				else btnRemoveColumns.setEnabled(true);
			}
		});
		row.add(cmbColRm);	
			
		mainPanel.add(row);
	}
	private void updateColumnList() {
		cmbColRm.removeAllItems();
		cmbColGO.removeAllItems();
		
		String [] vals = dbLoadPvalColumns();
		boolean b = (vals.length>0);
		
		if (b) {
			cmbColRm.addItem(allCols);
			cmbColGO.addItem(allCols);
			
			for(int x=0; x<vals.length; x++) {
				cmbColRm.addItem(vals[x]);
				cmbColGO.addItem(vals[x]);
			}
		}
		else {
			cmbColRm.addItem(noCol);
			cmbColGO.addItem(noCol);
		}
		cmbColRm.setSelectedIndex(0);
		cmbColGO.setSelectedIndex(0);
		
		cmbColRm.setMaximumSize(cmbColRm.getPreferredSize());
		cmbColRm.setMinimumSize(cmbColRm.getPreferredSize());
		cmbColRm.setEnabled(b);
		
		cmbColGO.setMaximumSize(cmbColGO.getPreferredSize());
		cmbColGO.setMinimumSize(cmbColGO.getPreferredSize());
		cmbColGO.setEnabled(b);
		
		btnRemoveColumns.setEnabled(b);
		btnGOSeq.setEnabled(b);
		repaint();
	}
	// GO Panel
	private void createGOPanel() {
		JPanel row = Static.createRowPanel();
		String msg = hasGO ? "GO enrichment" : "GO enrichment - no GOs";
		row.add(new JLabel(msg));
		mainPanel.add(row); mainPanel.add(Box.createVerticalStrut(3));
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("R-script")); row.add(Box.createHorizontalStrut(1));
		
		txtRfile2 = new JTextField(FILE_WIDTH);
		txtRfile2.setText(RSCRIPT2);
		txtRfile2.setMaximumSize(txtRfile2.getPreferredSize());
		row.add(txtRfile2); row.add(Box.createHorizontalStrut(1));
		
		btnRfile2 = new JButton("...");
		btnRfile2.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnRfile2, "R Script File", FileC.dRSCRIPTS, FileC.fR)) { // CAS316 was in-file chooser
					txtRfile2.setText(fc.getRelativeFile());
				}
			}
		});
		row.add(btnRfile2);
		if (hasGO) {mainPanel.add(row); mainPanel.add(Box.createVerticalStrut(3));}
		
		row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(5));
		btnGOSeq = Static.createButton("Execute", false);
		btnGOSeq.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				exGoSeq();
			}
		});
		row.add(btnGOSeq); 	row.add(Box.createHorizontalStrut(5));
		
		cmbColGO = new ButtonComboBox();
		cmbColGO.setEnabled(false);
		cmbColGO.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int n = cmbColGO.getItemCount();	
				if (n == 1 ) btnGOSeq.setEnabled(false);
				else btnGOSeq.setEnabled(true);
			}
		});
		row.add(cmbColGO);	row.add(Box.createHorizontalStrut(10));
		
		ButtonGroup group = new ButtonGroup();
		btnPercent = new JRadioButton("Top");
		btnPercent.setBackground(Color.WHITE);
		row.add(btnPercent);		row.add(Box.createHorizontalStrut(3));
		
		txtPercent = new JTextField(3);
		txtPercent.setMaximumSize(txtPercent.getPreferredSize());
		txtPercent.setMinimumSize(txtPercent.getPreferredSize());
		txtPercent.setText(defPercent);
		row.add(txtPercent);
		row.add(new JLabel("%"));	row.add(Box.createHorizontalStrut(5));
		row.add(new JLabel("or"));	row.add(Box.createHorizontalStrut(5));
		
		btnPVal = new JRadioButton("p-value");
		btnPVal.setBackground(Color.WHITE);
		btnPVal.setSelected(true);
		row.add(btnPVal);			row.add(Box.createHorizontalStrut(3));
		
		txtPVal = new JTextField(5);
		txtPVal.setMaximumSize(txtPercent.getPreferredSize());
		txtPVal.setMinimumSize(txtPercent.getPreferredSize());
		txtPVal.setText(defPVal);
		row.add(txtPVal);			row.add(Box.createHorizontalStrut(35));
		
		group.add(btnPercent);
		group.add(btnPVal);
		
		if (hasGO) mainPanel.add(row);
	}	

	private void createExitPanel() {
		JPanel row = Static.createRowPanel();
		JButton btnFinishExit = Static.createButton("Update Overview", true);
		btnFinishExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dbOverview(dbObj.getdbName(), mDB);
			}
		});
		row.add(btnFinishExit);
		
		row.add(Box.createHorizontalStrut(10));
		JButton btnClose = Static.createButton("Close", true);
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openWindows.remove(getInstance()); // CAS316 was 'this'
				dispose();
			}
		});
		row.add(btnClose);
		row.add(Box.createHorizontalStrut(10));
		
		JButton btnExit = Static.createButton("Exit", true);
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				openWindows.remove(getInstance()); // CAS316 was 'this'
				dispose();
			
				if(!hasChooser && (openWindows == null || openWindows.size()<=1)) {
					Out.Print("Exiting R and runDE for " + dbObj.getdbName());
					System.exit(0);
				}
				else Out.Print("Exiting runDE for " + dbObj.getdbName());
			}
		});
		row.add(btnExit);
		row.add(Box.createHorizontalStrut(10));
		row.add(Box.createHorizontalGlue());
		
		final JButton btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
	       btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(getInstance(), "RunDE Help", "html/runDE/QRFrame.html");
			}
	       });
	    row.add(btnHelp);
	    
		mainPanel.add(row);
	}
	// this is used by the three types of execute
	private boolean getMethodsFromPanel() {
		rScriptFile = txtRfile1.getText().trim();
		if (rScriptFile.equals("")) {
			JOptionPane.showMessageDialog(null,"Enter R-script file name.");
			return false;
		}
		if (!(new File(rScriptFile).exists())) {
			JOptionPane.showMessageDialog(null,"R-script file does not exist.\nFile name: " + rScriptFile);
			return false;
		}
		
		disp = -1.0;
		if (chkDisp.isSelected()){
			try {disp = Double.parseDouble(txtDisp.getText());}
			catch(Exception e){
				JOptionPane.showMessageDialog(null,"Invalid dispersion:" + txtDisp.getText());
				return false;
			}
		}
		
		filCnt = -1; filCPM = -1;
		if (filterRadio[0].isSelected()){
			try {filCnt = Integer.parseInt(txtFilCnt.getText());}
			catch(Exception e){
				JOptionPane.showMessageDialog(null,"Invalid Count value:" + txtFilCnt.getText());
				return false;
			}
		}
		else if (filterRadio[1].isSelected()) {
			try { filCPM = Integer.parseInt(txtFilCPM.getText()); }
			catch(Exception e){
				JOptionPane.showMessageDialog(null,"Invalid CPM value:" + txtFilCPM.getText());
				return false;
			}
			try { filCPMn = Integer.parseInt(txtFilCPMn.getText()); }
			catch(Exception e){
				JOptionPane.showMessageDialog(null,"Invalid CPM for :" + txtFilCPMn.getText());
				return false;
			}
		}
		return true;
	}
	
	private JRadioButton getRadioPanel(String name) {
		JRadioButton rb = new JRadioButton(name);
		rb.setBackground(Color.WHITE);
		rb.setAlignmentX(Component.CENTER_ALIGNMENT);
		rb.setMaximumSize(rb.getPreferredSize());
		rb.setMinimumSize(rb.getPreferredSize());
		return rb;
	}
	
	/******************************************************
	 * TCWdb chooser methods
	 */
	private JPanel createTCWdbSelectionPanel(Vector<DBInfo> dbList) 
	{
	       JPanel selectTCWdbPanel = new JPanel();
	       selectTCWdbPanel.setBackground(Color.WHITE);

	       // Create a tree of the hosts
	       DefaultMutableTreeNode hostTree = new DefaultMutableTreeNode("");
	       DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(hostsObj.host());
		   hostTree.add(newNode);
		   DefaultMutableTreeNode start = newNode;

	       for (int i = 0; i < hostTree.getChildCount(); i++) {
	           DefaultMutableTreeNode hostNode = (DefaultMutableTreeNode) hostTree.getChildAt(i);
	           
               for (DBInfo dbi : dbList) {
                   hostNode.add(new DefaultMutableTreeNode(dbi));
               } 
	       }

	       // Create display tree
	       DefaultTreeModel dbModel = new DefaultTreeModel(hostTree);
	       final JTree dbTree = new JTree(dbModel);
	       dbTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	       dbTree.setRootVisible(false);
	       dbTree.setToggleClickCount(1);
	       ((DefaultTreeCellRenderer) dbTree.getCellRenderer()).setLeafIcon(null);
	       if (dbTree.getRowCount() == 1) dbTree.expandRow(0);

	       dbTree.addMouseListener(new MouseAdapter() {
	           public void mouseClicked(MouseEvent e) {
	        	   try {
		               TreePath path = dbTree.getSelectionPath();
		               if (path != null) {
		                   int depth = path.getPathCount();
		                   if (e.getClickCount() == 2 && depth >= 3) {
		                       DefaultMutableTreeNode node =
		                           (DefaultMutableTreeNode) dbTree.getLastSelectedPathComponent();
		                       DBInfo dbi = (DBInfo) node.getUserObject();
				               new QRFrame(hostsObj, dbi);
		                   }
		               }
	        	   }
	        	   catch(Exception ex) {ErrorReport.prtReport(ex, "Error launching DE window");}
	           }
	       });

	       final JScrollPane assemblyScrollPane = new JScrollPane(dbTree);
	       assemblyScrollPane.setPreferredSize(new Dimension(400, 400));
	       Dimension dim = assemblyScrollPane.getMaximumSize();
	       assemblyScrollPane.setMaximumSize(new Dimension(Math.max(400,
	               (int) dim.getWidth()), Math.max(400, (int) dim.getHeight())));
	       assemblyScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

	       final JButton btnViewAssembly = new JButton("Launch");
	       btnViewAssembly.setAlignmentX(Component.CENTER_ALIGNMENT);
	       btnViewAssembly.setBackground(Globals.LAUNCHCOLOR);
	       btnViewAssembly.setEnabled(false);

	       btnViewAssembly.addActionListener(new ActionListener() {
	           public void actionPerformed(ActionEvent event) {
	        	   try {
		               DefaultMutableTreeNode node = (DefaultMutableTreeNode)
		                   dbTree.getLastSelectedPathComponent();
		               DBInfo dbi = (DBInfo) node.getUserObject();
		               new QRFrame(hostsObj, dbi);
	        	   }
	        	   catch(Exception e) {ErrorReport.prtReport(e, "Error launching DE window");}
	           }
	       });
	       
			final JButton btnGetState = new JButton("Overview");
			btnGetState.setBackground(Globals.LAUNCHCOLOR);
			btnGetState.setAlignmentX(Component.LEFT_ALIGNMENT);
			btnGetState.setEnabled(false);
			btnGetState.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					try {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) dbTree.getLastSelectedPathComponent();
						DBInfo dbi = (DBInfo) node.getUserObject();
						String dbName = dbi.getdbName();
						DBConn dbc = hostsObj.getDBConn(dbName);
						dbOverview(dbName, dbc);
						dbc.close();
					}
					catch(Exception e) {ErrorReport.prtReport(e, "Error getting overview");}
				}
			});

			final JButton btnClose = new JButton("Close");
			btnClose.setBackground(Color.WHITE);
			btnClose.addActionListener(new ActionListener() {
		    	   public void actionPerformed(ActionEvent arg0) {
		    		   hasChooser=false;
		    		   dispose();
		    	   }
			});
		       
	       final JButton btnCloseAll = new JButton("Exit All");
	       btnCloseAll.setBackground(Color.WHITE);
	       btnCloseAll.addActionListener(new ActionListener() {
		    	   public void actionPerformed(ActionEvent arg0) {
		    		   if(openWindows != null) {
		    			   for(int x=0; x<openWindows.size(); x++) {
		    				   openWindows.get(x).dispose();
		    			   }
		    			   openWindows.removeAllElements();
		    		   }
		    		   dispose();
		    		   Out.Print("Exiting R and runDE");
		    		   System.exit(0); // cause exit out of R too.
		    	   }
	       });      
	       	       
	       dbTree.addTreeSelectionListener(new TreeSelectionListener() {
	           public void valueChanged(TreeSelectionEvent e) {
	               int depth = dbTree.getSelectionPath().getPathCount();
	               btnViewAssembly.setEnabled(depth >= 3);
	               btnGetState.setEnabled(depth >= 3);
	           }
	       });

	       // Select current host in list
	       if (start != null) {
	           TreeNode[] nodes = dbModel.getPathToRoot(start.getFirstLeaf());
	           TreePath path = new TreePath(nodes);
	           dbTree.scrollPathToVisible(path);
	           dbTree.setSelectionPath(path);
	       }

	       JPanel buttonPanel = new JPanel();
	       buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
	       buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
	       buttonPanel.setBackground(Color.WHITE);
	       buttonPanel.add(btnViewAssembly);
	       buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnGetState);
	       buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnClose);
	       buttonPanel.add(Box.createHorizontalStrut(10));
	       buttonPanel.add(btnCloseAll);
	       
	       buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
	       buttonPanel.setMinimumSize(buttonPanel.getPreferredSize());
	       
	       selectTCWdbPanel.setLayout(new BoxLayout(selectTCWdbPanel,
	               BoxLayout.Y_AXIS));
	       selectTCWdbPanel.add(Box.createVerticalStrut(20));
	       selectTCWdbPanel.add(Static.createCenteredLabel("singleTCW Databases"));
	       selectTCWdbPanel.add(assemblyScrollPane);
	       selectTCWdbPanel.add(Box.createVerticalStrut(5));
	       selectTCWdbPanel.add(buttonPanel);
	       selectTCWdbPanel.add(Box.createVerticalStrut(20));
	       selectTCWdbPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	       return selectTCWdbPanel;
	}
	
	private QRFrame getInstance() { return this; }

	/************************************************************
	 * XXX Database methods
	 */
	private String dbSetConnection(String db) {
		setTitle("runDE " + Globalx.strTCWver + ":   " + db);
		Out.Print("Opening " + db);

		try {
			mDB = hostsObj.getDBConn(db);		
			theLibraryNames = dbLoadLibraryNames();
			
			String fullPath = mDB.executeString("select projectpath from assembly");
			if (fullPath==null)
				Out.bug("Project path is not set in database");
			else {
				projDirName = fullPath.substring(fullPath.lastIndexOf('/')+1);
				Out.PrtSpMsg(1, "Project directory name: " + projDirName);
			}
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting databse information");
			return null;
		}
		return db;
	}
	private String [] dbLoadPrefixedColumns(String prefix) {
	    Vector<String> retval = new Vector<String>();
	    try {
			ResultSet rs = mDB.executeQuery("SELECT * FROM contig LIMIT 1");
			
			ResultSetMetaData rsmd = rs.getMetaData();
			int numCols = rsmd.getColumnCount();
			for(int x=1; x<=numCols; x++) {
				if(rsmd.getColumnName(x).startsWith(prefix)) {
					retval.add(rsmd.getColumnName(x).substring(prefix.length()));
				}
			}
			
			return retval.toArray(new String [0]);
	    }
		catch ( Exception err ) {
			ErrorReport.reportError(err, "Error: reading database for getLibs");
            return null;
        }
	}

	private String [] dbLoadLibraryNames() {
		try {
			Vector<String> names = new Vector<String> ();
			ResultSet rset = mDB.executeQuery("SELECT libid FROM library where ctglib=0");
			while(rset.next()) {
				names.add(rset.getString(1));
			}			
			rset.close();
			return names.toArray(new String[0]);
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error loading data");
		}		
		return null;
	}
	private void dbRemoveColumnAll() {
		try {
			int nItems = cmbColRm.getItemCount();
			Vector<String> columns = new Vector<String>();
			for (int i = 0; i < nItems; i++)
			{
				String column = cmbColRm.getItemAt(i);
				if (!column.equals(allCols)) {
					columns.add(column);
				}
			}
			for (String name :columns) dbRemoveColumn(name);
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error removing column");}
	}
	private void dbRemoveColumn(String column) { // CAS321 changed for new columns in libraryDE
		try {
			Out.Print("Removing column " + column + "...");
			column =  pValColPrefix + column;
			mDB.executeUpdate("ALTER TABLE contig DROP COLUMN " + column);
			
			if (mDB.tableExists("go_info")) {
				String goMethod = mDB.executeString("select goMethod from libraryDE where pCol='" + column + "'");
				if (goMethod!=null && !goMethod.trim().contentEquals("")) {
					Out.PrtSpMsg(1, "from GO....");
					mDB.tableCheckDropColumn("go_info", column);
				}
			}
			mDB.executeUpdate("delete from libraryDE WHERE pCol='" + column + "'");
			
			mDB.executeUpdate("update assem_msg set pja_msg=NULL"); 
			
			updateColumnList();	
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error removing column");}
	}
	
	private String [] dbLoadPvalColumns() {
		Vector<String> names = new Vector<String> ();
		try {
			ResultSet rset = mDB.executeQuery("SHOW COLUMNS FROM contig");
			while(rset.next()) {
				String row = rset.getString(1);
				if(row.startsWith(Globals.PVALUE))
					names.add(row.substring(Globals.PVALUE.length()));
			}
			rset.close();
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error getting method columns");}
		
		return names.toArray(new String[0]);
	}
	private void dbOverview(String dbName, DBConn dbc) {
		try {
			String val = null;
			ResultSet rset = dbc.executeQuery("SELECT pja_msg, meta_msg FROM assem_msg");
			
			if(rset.first()) {
				val = rset.getString(1);
				if (val==null || (val != null && val.length() <= 10)) {
					Overview ov = new Overview(dbc);
					Vector <String> lines = new Vector <String> ();
					val = ov.createOverview(lines);
				}
				else val +=  rset.getString(2);
			}
			rset.close(); // don't close dbc
			
			UserPrompt.displayInfoMonoSpace(getInstance(), "Overview for " + dbName, 
					val.split("\n"), false, false); 
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting overview for " + dbName);}
	}
	//Window event code
	public void windowClosed(WindowEvent arg0) {
		if(openWindows != null)
			openWindows.remove(this); 
	}
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosing(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	/***************************************************************
	 * Object variables
	 */
	private JPanel mainPanel = null;
	private JLabel [] rowHeaders = null;
	private JLabel [] colHeaders = null;
	
	private JRadioButton [][] libRadio = null;
	
	private JButton btnRfile1 = null;
	private JTextField txtRfile1 = null;
	
	private JRadioButton [] filterRadio = null;
	private JCheckBox chkSaveCol, chkDisp;
	private JTextField txtColName, txtDisp, txtFilCPM, txtFilCPMn, txtFilCnt;
	
	private JButton btnGrp1Grp2 = null, btnGrp1All = null;
	private JButton btnPairFile = null, btnPairsFile = null;
	private JTextField txtPairsFile = null;
	
	private JButton btnLoadPvalFile = null, btnPvalFile = null;
	private JTextField txtPvalFile = null;
	
	
	private ButtonComboBox cmbColRm = null;
	private ButtonComboBox cmbColGO = null;
	
	private JButton btnRfile2 = null;
	private JTextField txtRfile2 = null;
	private JButton btnRemoveColumns = null, btnGOSeq = null;
	private JRadioButton btnPercent = null, btnPVal = null;
	
	private JTextField txtPercent = null, txtPVal = null;
	
	private QRProcess qrProcess = null;

	private String [] theLibraryNames = null;

	private DBConn mDB=null;
	private HostsCfg hostsObj=null;
	private DBInfo dbObj=null;

	private double disp = -1;
	private int filCPM = -1, filCPMn=-1, filCnt = -1;
	private String rScriptFile="";
	private String projDirName=null; // CAS316
	private boolean hasGO=false;
}
