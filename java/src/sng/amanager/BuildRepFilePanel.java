package sng.amanager;
/***********************************************
 * Build combined count file
 * called from EditTransLibPanel (Add/Edit Sequence dataset)
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import util.database.Globalx;
import util.file.FileC;
import util.file.FileHelpers;
import util.methods.Out;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.UserPrompt;

public class BuildRepFilePanel extends JPanel {
	private static final long serialVersionUID = 7368072364188049095L;
	private static final String DEFAULT_COMBINED_FILE = "Combined_read_counts.csv";
	private final String LIBDIR = Globalx.PROJDIR + "/";
	private final String HTML = "html/runSingleTCW/EditExpLibPanelGenCounts.html";
	
	public BuildRepFilePanel(ManagerFrame mf, EditTransLibPanel ef) {
		theManagerFrame = mf;
		theParentFrame = ef;
		fileList = new Vector<String> ();
		repNameList = new Vector<String> ();
		
		buildGenerateFilePanel();
	}
	public void setSeqFile(String name) {
		seqFilePath = name;
		currentDir = new File(LIBDIR + theManagerFrame.getProjDir()); 
	}
	
	private void buildGenerateFilePanel() { 
		JLabel title = new JLabel("Build combined count file");
		title.setFont(getFont().deriveFont(Font.BOLD, 18));
		JPanel titleRow = new JPanel();
		titleRow.setBackground(Globalx.BGCOLOR);
		titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
		titleRow.add(Box.createHorizontalGlue());
		titleRow.add(title);
		titleRow.add(Box.createHorizontalGlue());
		
		pnlTable = Static.createPageCenterPanel();

		JPanel tmpRow = Static.createRowPanel();
		JLabel tabTitle = Static.createLabel("Table of replicate count files", true);
		tmpRow.add(tabTitle);
		pnlTable.add(tmpRow); pnlTable.add(Box.createVerticalStrut(5));
		
		theModel = new TheTableModel();
		theTable = new JTable(theModel);
		theTable.setBackground(Globalx.BGCOLOR);
		theTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				btnRemoveFile.setEnabled(theTable.getSelectedRowCount() > 0);
			}
		});
		TableColumn column = theTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(120); // Rep name column
   		column.setMaxWidth(column.getPreferredWidth());
   		column.setMinWidth(column.getPreferredWidth());
		
		JScrollPane sPane = new JScrollPane(theTable, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sPane.setBackground(Globalx.BGCOLOR);
		
		pnlTable.add(sPane);
		pnlTable.setPreferredSize(new Dimension(700, 400));

		// first row of buttons
		pnlButton1 = Static.createRowCenterPanel();
		
		JLabel editTable = Static.createLabel("Edit table: ", true);
		btnAddDir = Static.createButton("Add Directory of Files", true);
		btnAddDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String lastDir = FileC.getLastDir();
				if (lastDir!=null) currentDir= new File(lastDir);
				else if(currentDir == null)
					currentDir = new File(LIBDIR + theManagerFrame.getProjDir());
				
				JFileChooser fc = new JFileChooser(currentDir);
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int retVal = fc.showDialog(theParentFrame, "Add Files from Directory");
				
				if(retVal == JFileChooser.APPROVE_OPTION) {
					String dir = fc.getSelectedFile().getPath(); 
					if (!new File(dir).isDirectory()) { // happens on mac
						JOptionPane.showMessageDialog(null,  
								"Please select directory name to open", "Error reading directory", 
								JOptionPane.PLAIN_MESSAGE);
					}
					else {
						addFilesFromDir(dir);
						currentDir = fc.getCurrentDirectory();
						theModel.fireTableDataChanged();
					}
				}
			}
		});
		
		btnAddFile = Static.createButton("Add Rep File", true);
		btnAddFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(currentDir == null)
					currentDir = new File(LIBDIR + theManagerFrame.getProjDir());
				
				JFileChooser fc = new JFileChooser(currentDir);
				int retVal = fc.showOpenDialog(theParentFrame);
				
				if(retVal == JFileChooser.APPROVE_OPTION) {
					String fullname = fc.getSelectedFile().getPath();
					String name = fullname.substring(fullname.lastIndexOf("/")+1);
					addNameFile(name, fullname);
					
					currentDir = fc.getCurrentDirectory();
					theModel.fireTableDataChanged();
				}
			}
		});
		btnRemoveFile = Static.createButton("Remove Rep File", false);
		btnRemoveFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				fileList.remove(theTable.getSelectedRow());
				repNameList.remove(theTable.getSelectedRow());
				theModel.fireTableDataChanged();
			}
		});
		btnClear = Static.createButton("Clear", true);
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clearCountFileList();
				theModel.fireTableDataChanged();
			}
		});
		pnlButton1.add(editTable); pnlButton1.add(Box.createHorizontalStrut(5));
		pnlButton1.add(btnAddDir); pnlButton1.add(Box.createHorizontalStrut(5));
		pnlButton1.add(btnAddFile);pnlButton1.add(Box.createHorizontalStrut(5));
		pnlButton1.add(btnRemoveFile); pnlButton1.add(Box.createHorizontalStrut(5));
		pnlButton1.add(btnClear);
		pnlButton1.setMaximumSize(pnlButton1.getPreferredSize());
		pnlButton1.setMinimumSize(pnlButton1.getPreferredSize());
		
		// Second row of buttons
		pnlButton2 = Static.createRowCenterPanel();
		
		btnBuildFile = Static.createButton("Generate File", true, Globalx.FUNCTIONCOLOR);
		btnBuildFile.setOpaque(true);
		btnBuildFile.setEnabled(true);
		btnBuildFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				generateFile();
				theParentFrame.setEditTransVisible();
			}
		});
		
		btnCancel = Static.createButton("Close", true);
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theParentFrame.setEditTransVisible();
			}
		});
		
		btnGenCountHelp = Static.createButton("Help", true, Globalx.HELPCOLOR);
		btnGenCountHelp.setBackground(Globalx.HELPCOLOR);
		btnGenCountHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, "Build Count File Help", HTML);
			}
		});
	
		pnlButton2.add(btnBuildFile);
		pnlButton2.add(Box.createHorizontalStrut(5));
		pnlButton2.add(btnCancel);
		pnlButton2.add(Box.createHorizontalStrut(5));
		pnlButton2.add(btnGenCountHelp);
		pnlButton2.setMaximumSize(pnlButton2.getPreferredSize());
		pnlButton2.setMinimumSize(pnlButton2.getPreferredSize());
		
		JPanel mainPanel = Static.createPageCenterPanel();
		mainPanel.add(titleRow);
		mainPanel.add(Box.createVerticalStrut(20));
		mainPanel.add(pnlTable);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(pnlButton1);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(pnlButton2);
		
		// int top width,int left width,int bottom,int right
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		mainPanel.setMaximumSize(mainPanel.getPreferredSize());
		mainPanel.setMinimumSize(mainPanel.getPreferredSize());
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globalx.BGCOLOR);
		
		add(mainPanel);
		setVisible(false);
	}
	public void addFile(String file, String header) {
		fileList.add(file);
		repNameList.add(header);
		theModel.fireTableDataChanged();
	}
	
	public void clearCountFileList() {
		fileList.clear();
		repNameList.clear();
	}
	// read directory of files and enter into table for Generate Counts
	private void addFilesFromDir(String dirStr) {
		try {
			Out.PrtCntMsg(0, "Loading files from " +dirStr);
			File dir = new File(dirStr);
			File[] files = dir.listFiles();
			
			for (int i=0; i<files.length; i++) {
				String name = files[i].getName();
				if (name.startsWith(".")) continue;
				
				String fullname = dirStr + "/" + name;
				
				if (!checkCountFile(fullname)) {
					Out.PrtWarn("Ignoring " + name);
					continue;
				}
				addNameFile(name, fullname);
			}
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Could not read folder");
		}
	}
	private void addNameFile(String name, String fullname) {
		int index = name.indexOf(".");
		String prefix;
		if (index == -1) prefix = name;
		else prefix = name.substring(0,index);
		
		String fname = removeLib(fullname);
		if (fileList.contains(fullname)) { 
			JOptionPane.showMessageDialog(this, 
					"Duplicate file name - " + fullname, 
					"Warning", JOptionPane.PLAIN_MESSAGE);
		}
		else {
			fileList.add(fname);
			repNameList.add(prefix);
		}
	}
	private boolean checkCountFile(String file) {
		try {
			File f = new File (file);
			if (f.isDirectory()) return false;
			if (!f.canRead()) {
				Out.PrtErr("Unreadeable " + file);
				return false;
			}
			
			BufferedReader br = FileHelpers.openGZIP(f.getAbsolutePath()); // CAS315
			String line;
			while((line = br.readLine())!= null) {
				if (line.startsWith("#")) continue;
				if (line.trim().equals("")) continue;	
				String [] vals = line.split("[\\s]+");
				try {
					Long.parseLong(vals[1]);
					return true;
				}
				catch (Exception e) {
					br.close();
					return false;
				}
			}
			br.close();
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Error reading " + file);
		}
		return false;
	}
	
	/*************************************************************
	 *XXX Generate File
	 */
	private void generateFile() {
		if(fileList.size() == 0) {
			System.out.println("Error: no files selected");
			return;
		}			
		try {
	         Out.PrtSpMsg(0, "Generating combined the count file - this takes a while");
	         
	  // get all seqIDs from sequence file to make sure they correspond to read count files		
	         ArrayList <String> seqIDs = new ArrayList<String> ();
			BufferedReader br = FileHelpers.openGZIP(seqFilePath);
			String line = br.readLine();
			while(line != null) {
				if(line.startsWith(">")) {
					seqIDs.add(line.substring(1).split("\\s+")[0]);
				}
				line = br.readLine();
			}
			br.close();
			
			Collections.sort(seqIDs);		
			Out.prtSpCnt(1, seqIDs.size(), "Sequences in file" );
			
			Vector<long []> theCounts = new Vector<long []> ();
			String projDir = theManagerFrame.getProjDir();
			
	// read through files
			for(int x=0; x<fileList.size(); x++) {
				String filename = fileList.get(x);				
				Out.prt("   Reading " + removeLib(filename));
				
				long [] theDECounts = new long[seqIDs.size()];
				for(int y=0; y<theDECounts.length; y++)
					theDECounts[y] = 0;
				
				String fname = FileC.addFixedPath(projDir, filename, FileC.dPROJ);
				br = FileHelpers.openGZIP(fname);
				line = br.readLine();
				int lineCount = 0;
				while(line != null) {
					if (line.startsWith("#")) continue;
					if (line.trim().equals("")) continue;
					
					lineCount++;
					if (lineCount % 1000 == 0) 
						System.out.print("      Reading line " + lineCount + "\r");
					String [] vals = line.split("[\\s]+");
					
					String seqID = vals[0].trim();
					long count;
					try {
						count = Long.parseLong(vals[1]);
					}
					catch (Exception e) {
						Out.PrtErr("Incorrect line: " + line);
						UserPrompt.showError("Invalid file (see terminal)\n   " + filename);
						br.close();
						return;
					}
					
					int index = Collections.binarySearch(seqIDs, seqID);						
					if (index < 0) {
						Out.PrtErr(seqID + " not in sequence file ");
						UserPrompt.showError("Invalid file (see terminal)\n   " + filename);
						br.close();
						return;
					}
					else theDECounts[index] = count;
					
					line = br.readLine();
				}
				br.close();
				Out.prtSpCnt(1, lineCount, "Lines read        ");
				theCounts.add(theDECounts);
			}
			String outFileName = LIBDIR + theManagerFrame.getProjDir() + "/" + DEFAULT_COMBINED_FILE;
			Out.PrtSpMsg(1, "Writing file " + outFileName);
			PrintWriter out = new PrintWriter(new FileWriter(new File(outFileName)));
			
			out.print("Sequence  ");
			for(int x=0; x<repNameList.size(); x++) {
				out.print(repNameList.get(x) + "\t");
			}
			out.println();
			
			for(int x=0; x<seqIDs.size(); x++) {
				out.print(seqIDs.get(x));
				for(int y=0; y<theCounts.size(); y++) {
					out.print("\t" + theCounts.get(y)[x]);
				}
				out.println();
				out.flush();
			}
			
			out.close();
			theParentFrame.setCountFile(DEFAULT_COMBINED_FILE); 
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error generating count file");
		}
	}
	public String removeLib(String path) {
		String libPath = LIBDIR + theManagerFrame.getProjDir();
		int index = path.indexOf(libPath);
		if(index >= 0) return path.substring(index + libPath.length() + 1);
		else return path;
	}
	
	private class TheTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 7134027422140841320L;

		public String getColumnName(int column) {
			if(column == 0) return "Rep name (editable)";
			return "File Name";
		}
		public int getColumnCount() {return 2;}
		public int getRowCount() {return fileList.size();}
		public Object getValueAt(int row, int col) {
			if(col == 0) return repNameList.get(row);
			return fileList.get(row);
		}
		public boolean isCellEditable (int row, int column) { return column == 0; }
	}
	
	private JTable theTable = null;
	private TheTableModel theModel = null;
	
	private JButton btnAddDir = null;
	private JButton btnAddFile = null;
	private JButton btnRemoveFile = null;
	private JButton btnCancel = null;
	private JButton btnClear = null;
	private JButton btnBuildFile = null;
	private JButton btnGenCountHelp = null;
	
	private Vector<String> fileList = null;
	private Vector<String> repNameList = null;
	
	private JPanel pnlTable = null;
	private JPanel pnlButton1 = null;
	private JPanel pnlButton2 = null;
	
	private EditTransLibPanel theParentFrame=null;
	private ManagerFrame theManagerFrame=null;
	private File currentDir=null;
	private String seqFilePath=null;
}
