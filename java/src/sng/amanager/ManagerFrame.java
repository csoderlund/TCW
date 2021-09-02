package sng.amanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import sng.annotator.CoreDB;
import sng.annotator.runSTCWMain;
import sng.database.Globals;
import sng.database.Overview;
import sng.database.Version;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.file.FileC;
import util.file.FileRead;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.TCWprops;
import util.ui.ButtonComboBox;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

/**************************************************
 * This file contains the following (in this order)
 * 1 Creates the main window
 * 2 Panels that make up main window
 * 3 routine for main window
 * 4 Panels that replace main window (Assembly Options, Annotation Options, Annotation Add/Edit)
 * 5 routines to transfer information to the Manager data
 *  		other panels are in their own files
 * 6 Database routines - each routine creates its own connection
 * 7 other assorted routines and classes
 */

public class ManagerFrame extends JFrame {
	private static final long serialVersionUID = 7879962219972222820L;
	private static final String HTML = Globals.helpRunDir + "ManagerHelp.html";
	private static final String COPYPROJ = "Cp"; // suffix
	
	public static final String PROJDIR = Globalx.PROJDIR  + "/";
	public static final String ANNODIR = Globalx.ANNODIR  + "/";
	public static final String STCW = Globalx.STCW;
	
	public static final int FRAME_MODE_MAIN = 0;
	public static final int FRAME_MODE_ASSEM = 1;
	public static final int FRAME_MODE_TRANS_LIB_EDIT = 2;
	public static final int FRAME_MODE_COUNT_LIB_EDIT = 3;
	public static final int FRAME_MODE_ANNO_DB_EDIT = 4;
	public static final int FRAME_MODE_ANNO_DB_OPTIONS = 5;
	public static final int FRAME_MODE_BUILD_REPS = 6;
	public static final int FRAME_MODE_REMARKS = 7;
	
	private static final int TWIDTH = 400;
	private static final int THEIGHT = 10;
	private static final int COLUMN_LABEL_WIDTH = 120; 
	
	public ManagerFrame() {
		hostsObj = new HostsCfg();
		host = hostsObj.host();
		
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				try {
					if(nFrameMode == FRAME_MODE_MAIN) {
						//Need to test if application is running
						if (rCmd.isBusy()) {
							if (!UserPrompt.showConfirm("Process running", 
							"There is a subprocess currently running. \n" +
							"Exiting will terminate the process."))
									return;
						}
						Preferences userPrefs = Preferences.userRoot();
						userPrefs.putInt("manager_pave_frame_win_x", getX());
						userPrefs.putInt("manager_pave_frame_win_y", getY());
						userPrefs.putInt("manager_pave_frame_win_width", getWidth());
						userPrefs.putInt("manager_pave_frame_win_height", getHeight());
						userPrefs.flush();
	
						System.exit(0);
					}
					else if(nFrameMode == FRAME_MODE_ASSEM) {
						pnlAssemblyOptions.setVisible(false);
						mainPanel.setVisible(true);
					}
					else if(nFrameMode == FRAME_MODE_TRANS_LIB_EDIT) {
						pnlTransLibEdit.setVisible(false); 
						mainPanel.setVisible(true);
					}
					else if(nFrameMode == FRAME_MODE_BUILD_REPS) {
						if (pnlTransLibEdit!=null)
							pnlTransLibEdit.setEditTransVisible();
						nFrameMode = FRAME_MODE_TRANS_LIB_EDIT;
					}
					else if(nFrameMode == FRAME_MODE_COUNT_LIB_EDIT) {
						if(bRemoveExpLibOnDiscard) {
							curManData.removeCountLib(nCurCountLibIndex);
							updateUI();
						}
						pnlCountLibEdit.setVisible(false);
						mainPanel.setVisible(true);
					}
					else if(nFrameMode == FRAME_MODE_ANNO_DB_EDIT) {
						if(bAddAnno) {
							curManData.setAnnoDBAt(nCurAnnoIndex, null);
							updateUI();
						}		
						pnlAnnoDBEdit.setVisible(false);
						mainPanel.setVisible(true);
					}
					else if(nFrameMode == FRAME_MODE_ANNO_DB_OPTIONS) {
						pnlAnnoDBOptions.setVisible(false);
						mainPanel.setVisible(true);
					}
					else if (nFrameMode == FRAME_MODE_REMARKS) {
						pnlRemark.setVisible(true);
						mainPanel.setVisible(true);
					}
					if(nFrameMode != FRAME_MODE_BUILD_REPS)
						nFrameMode = FRAME_MODE_MAIN;
				}
				catch(Exception e) {System.err.println("Error saving preferences");}
			}
		});
		try {
			setTitle("runSingleTCW " + Version.strTCWver); 
		}
		catch(Exception e) {setTitle("runSingleTCW");}
		
		rCmd = new RunCmd(this, hostsObj);
		createMainPanel();
		setWindowSettings();
	}
	
	private void setWindowSettings() {
		Preferences userPrefs = Preferences.userRoot();
		int nX = userPrefs.getInt("manager_pave_frame_win_x", Integer.MAX_VALUE);
		int nY = userPrefs.getInt("manager_pave_frame_win_y", Integer.MAX_VALUE);
		int nWidth = userPrefs.getInt("manager_pave_frame_win_width", Integer.MAX_VALUE);
		int nHeight = userPrefs.getInt("manager_pave_frame_win_height", Integer.MAX_VALUE);
		if (nX == Integer.MAX_VALUE) {
			UIHelpers.centerScreen(this);
		} else {
			setBounds(nX, nY, nWidth, nHeight);
		}
	}
		
	/**************************************************
	 * XXX Creates the main window
	 */
	private void createMainPanel() {
		createFirstTwoRowPanel(getLibraryList(true));
		createSecondTwoRowPanel();		
		
		createTransLibPanel();		 
		createCountLibPanel();
		createAnnoDBPanel();
		
		pnlAnnoDBEdit = new EditAnnoPanel(this); 
		pnlTransLibEdit = new EditTransLibPanel(this);
		pnlCountLibEdit = new EditCountPanel(this);
		pnlGenReps = new GenRepsPanel(this);
		pnlAssemblyOptions = new AssmOptionsPanel(this);
		pnlRemark = new AddRemarkPanel(this);
		pnlAnnoDBOptions = new AnnoOptionsPanel(this);
		
		mainPanel = Static.createPagePanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				
		mainPanel.add(pnlProject);

		mainPanel.add(Box.createVerticalStrut(15));
		mainPanel.add(pnlGeneral);

		// 1. ////////////////////////////////////////////////
		mainPanel.add(new JSeparator());
		mainPanel.add(new JLabel("Sequence Datasets"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(pnlTransLib);

		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(new JLabel("Associated Counts"));
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(pnlCountLib);
				
		// this is place holder for Transcript read count table
		// which gets populated in the updateUI method
		JPanel tempRow = Static.createRowPanel();
		
		btnExecLoadLib = Static.createButton("Step 1. Build Database", true, null);
		btnExecLoadLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread buildThread = new Thread(new Runnable() {
					public void run() {
						rCmd.actionLoadLibrary();
						dbExists=tcwDBexists();
						updateUI();
						tcwDBselectLoadStatus(); // CAS314 was not getting isAA
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();
			}
		});
		mainPanel.add(Box.createVerticalStrut(10));
		tempRow.add(btnExecLoadLib);
		mainPanel.add(tempRow);
		
		//2. ///////////////////////////////////////////
		mainPanel.add(new JSeparator());
		
		chkSkipAssembly = Static.createCheckBox("Skip Assembly", true);
		chkSkipAssembly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {	
				boolean bSkip = chkSkipAssembly.isSelected(); 
				curManData.setSkipAssembly(bSkip);				// and added !
				if (!bSkip) curManData.setUseTransNames(false); // CAS304 CAS311 was (bSkip) BUG
				updateUI();
			}
		});
		
		chkUseTransName = Static.createCheckBox("Use Sequence Names from File", false);
		chkUseTransName.setBackground(Globals.BGCOLOR);
		chkUseTransName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				boolean bUse = chkUseTransName.isSelected();
				curManData.setUseTransNames(bUse);
				if (bUse) curManData.setSkipAssembly(true); // CAS304 
				updateUI();
			}
		});
		
		btnAssemOptions = Static.createButton("Options", true, Globals.MENUCOLOR);
		btnAssemOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nFrameMode = FRAME_MODE_ASSEM;
				pnlAssemblyOptions.actionOptionsTrans();
				mainPanel.setVisible(false);
				pnlAssemblyOptions.setVisible(true);
			}
		});
		
		btnExecAssem = Static.createButton("Step 2. Instantiate", true, null);
		btnExecAssem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread buildThread = new Thread(new Runnable() {
					public void run() {
						rCmd.actionCreateTrans();
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();					
			}
		});
		
		tempRow = Static.createRowPanel();
		tempRow.add(btnExecAssem);
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(chkUseTransName);
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(chkSkipAssembly);
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(btnAssemOptions);
		
		mainPanel.add(tempRow);
		
		// 3. ///////////////////////////////////////////////////////
		mainPanel.add(new JSeparator());

		tempRow = Static.createRowPanel();
		tempRow.add(new JLabel("AnnoDBs (e.g. UniProt)"));
		btnCheck = Static.createButton("Check All", true);
		btnCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				checkAllAnnos();
			}
		});
		btnCheck.setMargin(new Insets(0, 0, 0, 0));
		btnCheck.setFont(new Font(btnCheck.getFont().getName(),Font.PLAIN,10));
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(btnCheck);
		
		btnRemove = Static.createButton("Remove All", true);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				removeAllAnnos();
			}
		});
		btnRemove.setMargin(new Insets(0, 0, 0, 0));
		btnRemove.setFont(new Font(btnRemove.getFont().getName(),Font.PLAIN,10));
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(btnRemove);
		
		btnImportAnnot = Static.createButton("Import AnnoDBs", true, Globals.PROMPTCOLOR);
		btnImportAnnot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					FileRead fc = new FileRead(projDirName, FileC.bDoVer, FileC.bDoPrt);
					if (fc.run(btnImportAnnot, "Import AnnoDBs", FileC.dPROJTOP, FileC.fCFG)) { 
						curManData.importAnnoDBs(fc.getRemoveFixedPath());
					}
					updateUI();
				}
				catch(Exception err) {ErrorReport.reportError(err, "Error Import AnnoDBs");}
			}
		});
		btnImportAnnot.setMargin(new Insets(0, 0, 0, 0));
		btnImportAnnot.setFont(new Font(btnImportAnnot.getFont().getName(),Font.PLAIN,10));
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(btnImportAnnot);
		
		mainPanel.add(tempRow);
		mainPanel.add(Box.createVerticalStrut(5));
		
		mainPanel.add(pnlAnnoDB);
		Dimension d = mainPanel.getPreferredSize();
		d.height = pnlProject.getPreferredSize().height;
		pnlProject.setMinimumSize(d);
		pnlProject.setPreferredSize(d);
		pnlProject.setMaximumSize(d);
		
		tempRow = Static.createRowPanel();
		btnExecAnno = Static.createButton("Step 3. Annotate", true, null);
		btnExecAnno.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread buildThread = new Thread(new Runnable() {
					public void run() {
						Object [] options = {"Answer", "Use Defaults", "Cancel"}; 
						String msg = "Answer - answer prompts at command line\n" +
								"Use Defaults - run with defaults (see Help)";
						int n = JOptionPane.showOptionDialog(getInstance(),
						    msg, "Annotation",
						    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
						    null, options, options[0]);
						if (n==2) return;
						boolean prompt = (n==0) ? true : false;
						
						rCmd.actionAnno(prompt);
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();
			}
		});
		tempRow.add(btnExecAnno);
		tempRow.add(Box.createHorizontalStrut(5));
		
		btnUpdateGO = Static.createButton("GO only", true, null);
		btnUpdateGO.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				Thread buildThread = new Thread(new Runnable() 
				{
					public void run() 
					{
						if (UserPrompt.showConfirm("GO only", "Add GO annotations"))
							rCmd.actionRedo(runSTCWMain.optGO);
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();
			}
		});
		tempRow.add(btnUpdateGO);
		tempRow.add(Box.createHorizontalStrut(5));
		
		btnUpdateORF = Static.createButton("ORF only", true, null);
		btnUpdateORF.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				Thread buildThread = new Thread(new Runnable() 
				{
					public void run() 
					{
						if (UserPrompt.showConfirm("ORF only", "Compute and add ORFs"))
							rCmd.actionRedo(runSTCWMain.optORF);
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();
			}
		});
		tempRow.add(btnUpdateORF);
		tempRow.add(Box.createHorizontalStrut(5));
		
		btnEditAnnoOptions = Static.createButton("Options", true, Globals.MENUCOLOR);
		btnEditAnnoOptions.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nFrameMode = FRAME_MODE_ANNO_DB_OPTIONS;
				pnlAnnoDBOptions.updateAnnoOptions(curManData, hostsObj,isAAtcw);
				mainPanel.setVisible(false);
				pnlAnnoDBOptions.setVisible(true);	
			}
		});
		tempRow.add(btnEditAnnoOptions);
		tempRow.add(Box.createHorizontalStrut(5));
		
		lblAnno = new JLabel("");
		tempRow.add(lblAnno); // CAS331 merged lblGO and lblSim
		mainPanel.add(tempRow);
		
		mainPanel.add(new JSeparator());
		////////////////////////////////////////////////////////
		tempRow = Static.createRowPanel();
		btnRunViewTCW = Static.createButton("Launch viewSingleTCW",true, Globals.LAUNCHCOLOR);
		btnRunViewTCW.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Thread buildThread = new Thread(new Runnable() {
					public void run() {
						rCmd.actionView();	
					}
				});
				buildThread.setPriority(Thread.MIN_PRIORITY);
				buildThread.start();
			}
		});
		tempRow.add(btnRunViewTCW);
		tempRow.add(Box.createHorizontalStrut(250));
		
		btnAddRemark = Static.createButton("Add Remarks or Locations", true, Globals.MENUCOLOR);
		btnAddRemark.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pnlRemark.reset();
				nFrameMode = FRAME_MODE_REMARKS;
				pnlRemark.setVisible(true);
				mainPanel.setVisible(false);
			}
		});
		tempRow.add(btnAddRemark);
		mainPanel.add(tempRow);
		
		JPanel outsidePanel = Static.createPagePanel();
		outsidePanel.add(mainPanel);
		outsidePanel.add(pnlAnnoDBEdit);
		outsidePanel.add(pnlTransLibEdit);
		outsidePanel.add(pnlCountLibEdit);
		outsidePanel.add(pnlAssemblyOptions);
		outsidePanel.add(pnlAnnoDBOptions);
		outsidePanel.add(pnlGenReps);
		outsidePanel.add(pnlRemark);
		sPane = new JScrollPane(outsidePanel);
		getContentPane().add(sPane);
		
		TCWprops theProps = new TCWprops(TCWprops.PropType.Assem);
		chkSkipAssembly.setSelected(theProps.getProperty("SKIP_ASSEMBLY").equals("1"));
		chkUseTransName.setSelected(theProps.getProperty("USE_TRANS_NAME").equals("1"));
		
		pack();
		updateUI();
	}
	/**************************************************************
	 * XXX Panels that make up main window
	 */
	private void createFirstTwoRowPanel(String [] projectNames) {
		pnlProject = Static.createPagePanel();
		
		btnAddProject = Static.createButton("Add Project", true, Globals.PROMPTCOLOR);
		btnAddProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addProject();
			}
		});

		btnHelp = Static.createButton("Help", true, Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(), "runSingleTCW", HTML);
			}
		});
		// second line
		cmbProjects = new ButtonComboBox();
		cmbProjects.setBackground(Globals.BGCOLOR);
		refreshProjectList();
		cmbProjects.setMaximumSize(cmbProjects.getPreferredSize());
		
		cmbProjects.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(cmbProjects.getSelectedIndex() > 0) {
					updateCurManData(cmbProjects.getSelectedIndex());
					FileC.resetLastDir();
				}
				else {
					curManData = null;
				}
				updateUI();
			}
		});
		cmbProjects.setMaximumSize(cmbProjects.getPreferredSize());
		
		btnSaveProject = Static.createButton("Save", true, Globals.BGCOLOR);
		btnSaveProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveProject();
			}
		});
		
		btnCopyProject = Static.createButton("Copy", true, Globals.BGCOLOR);
		btnCopyProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				copyProject();
			}
		});
		
		btnRemoveProject = new JButton("Remove...");
		btnRemoveProject.setBackground(Globals.PROMPTCOLOR);
		btnRemoveProject.setAlignmentX(Component.RIGHT_ALIGNMENT);
		btnRemoveProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final RemoveType remove = new RemoveType();
				remove.setVisible(true);	
				if (!remove.isCancel()) {
					remove.doOp();
					updateUI();
					repaint();
				}
			}
		});
		
		btnGetState = Static.createButton("Overview", true, Globals.LAUNCHCOLOR);
		btnGetState.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tcwDBselectShowOverview();
			}
		});
		
		JPanel tempRow = Static.createRowPanel();
		tempRow.add(btnAddProject);
		tempRow.add(Box.createHorizontalStrut(10));
		tempRow.add(btnHelp);
		
		pnlProject.add(tempRow);
		pnlProject.add(Box.createVerticalStrut(10));
		
		tempRow = Static.createRowPanel();
		tempRow.add(new JLabel("Project"));
		tempRow.add(Box.createHorizontalStrut(5));
		tempRow.add(cmbProjects);
		tempRow.add(Box.createHorizontalStrut(10));
		tempRow.add(btnSaveProject);
		tempRow.add(Box.createHorizontalStrut(10));
		tempRow.add(btnCopyProject);
		tempRow.add(Box.createHorizontalStrut(10));
		tempRow.add(btnRemoveProject);
		tempRow.add(Box.createHorizontalStrut(10));
		tempRow.add(btnGetState);
		
		pnlProject.add(tempRow);
	}
	/****************************************************************
	 * Two two rows with Host/CPU/singleTCW /Database
	 */
	private void createSecondTwoRowPanel() {
		theHosts = new Vector<String>(); 
		theHosts.add(host);
		
		pnlGeneral = Static.createRowPanel();
		
		JPanel leftPanel = Static.createPagePanel();
		leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		
		txtdbID = Static.createTextField("", 5);
		txtdbID.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent arg0) {
				if(curManData != null)
					curManData.setProjID(txtdbID.getText());
			}
		});
		leftPanel.add(addRow("singleTCW ID", txtdbID));
		leftPanel.add(Box.createVerticalStrut(5));
		
		txtdbName = Static.createTextField("", 15);
		txtdbName.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				if(curManData != null)
					curManData.setTCWdb(txtdbName.getText());
			}
		});
		leftPanel.add(addRow("sTCW Database", txtdbName));
		leftPanel.add(Box.createVerticalStrut(5));
		
		JPanel rightPanel = Static.createPagePanel();
		rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		
		txtNumCPUs = Static.createTextField("", 3);
		txtNumCPUs.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				try {
					curManData.setNumCPUs(Integer.parseInt(txtNumCPUs.getText()));
				} catch(Exception err) {}
			}
		});
		JPanel row = Static.createRowPanel();
		row.add(new JLabel("# CPUs"));
		row.add(Box.createHorizontalStrut(2));
		row.add(txtNumCPUs);
		
		rightPanel.add(row);
		rightPanel.add(Box.createVerticalStrut(5));
		
		rightPanel.add(new JLabel("Host: " + host));
		rightPanel.add(Box.createVerticalStrut(5));
		
		pnlGeneral.add(leftPanel);
		pnlGeneral.add(Box.createHorizontalStrut(30));
		pnlGeneral.add(rightPanel);		
		pnlGeneral.setMaximumSize(pnlGeneral.getPreferredSize());
	}
	/*******************************************************************
	 * XXX Utilities
	 */
	public void setMainPanelVisible(boolean visible) {
		mainPanel.setVisible(visible);
	}
	public String getProjDir() { return projDirName;}
	
	public void setFrameMode(int mode) { nFrameMode = mode; }
	
	private static JPanel addRow(String fieldName, JComponent theField) {
		JPanel retVal = Static.createRowPanel();
		
		JLabel theLabel = new JLabel(fieldName);
		int diff = COLUMN_LABEL_WIDTH - theLabel.getPreferredSize().width;
		
		retVal.add(theLabel);
		retVal.add(Box.createHorizontalStrut(diff));
		retVal.add(theField);
		
		retVal.setMaximumSize(retVal.getPreferredSize());
		
		return retVal;
	}
	
	/***********************************************************
	 * transLibPanel - table and Add/Edit/Remove on Main window
	 */
	private void createTransLibPanel() {
		pnlTransLib = new JPanel();
		pnlTransLib.setLayout(new BoxLayout(pnlTransLib, BoxLayout.LINE_AXIS));
		pnlTransLib.setBackground(Globals.BGCOLOR);
		pnlTransLib.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		transTable = new ManagerTable(ManagerTable.TRANS_LIB_MODE);
		transTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				updateUI();
			}
		});
		
		sPaneTransLibrary = new JScrollPane();
		sPaneTransLibrary.setViewportView(transTable);
		transTable.getTableHeader().setBackground(Color.WHITE);
		sPaneTransLibrary.setColumnHeaderView(transTable.getTableHeader());
		sPaneTransLibrary.setAlignmentX(Component.LEFT_ALIGNMENT);
    	
		sPaneTransLibrary.getViewport().setBackground(Color.WHITE);
		sPaneTransLibrary.getHorizontalScrollBar().setBackground(Color.WHITE);
		sPaneTransLibrary.getVerticalScrollBar().setBackground(Color.WHITE);
		sPaneTransLibrary.getHorizontalScrollBar().setForeground(Color.WHITE);
		sPaneTransLibrary.getVerticalScrollBar().setForeground(Color.WHITE);
		
		sPaneTransLibrary.getViewport().setMaximumSize(new Dimension(TWIDTH, THEIGHT));
		sPaneTransLibrary.getViewport().setPreferredSize(new Dimension(TWIDTH, THEIGHT));
		sPaneTransLibrary.getViewport().setMinimumSize(new Dimension(TWIDTH, THEIGHT));
		pnlTransLib.add(sPaneTransLibrary);
		pnlTransLib.add(Box.createHorizontalStrut(5));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonPanel.setBackground(Globals.BGCOLOR);
		
		btnAddTransLib = new JButton("Add");
		btnAddTransLib.setBackground(Globals.MENUCOLOR);
		btnAddTransLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nFrameMode = FRAME_MODE_TRANS_LIB_EDIT;
				bAddTransLib = true;
				nCurTransLibIndex = curManData.addNewTransLib();
				pnlTransLibEdit.updateTransLibEditUI(nCurTransLibIndex, true, bAddTransLib);
				mainPanel.setVisible(false);
				pnlTransLibEdit.setVisible(true);				
			}
		});
		
		btnEditTransLib = new JButton("Edit");
		btnEditTransLib.setBackground(Globals.MENUCOLOR);
		btnEditTransLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isTransSelected()) {
					nFrameMode = FRAME_MODE_TRANS_LIB_EDIT;
					bAddTransLib = false;
					
					nCurTransLibIndex  = transTable.getSelectedRow();
					pnlTransLibEdit.updateTransLibEditUI(nCurTransLibIndex, !dbExists, bAddTransLib);
	
					mainPanel.setVisible(false);
					pnlTransLibEdit.setVisible(true);
				}
			}
		});
		btnEditTransLib.setEnabled(false);
		
		btnRemoveTransLib = new JButton("Remove");
		btnRemoveTransLib.setBackground(Globals.BGCOLOR);
		btnRemoveTransLib.setEnabled(false);
		btnRemoveTransLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isTransSelected()) {
					if (UserPrompt.showConfirm("Remove table entry", 
							"Remove selected sequence dataset" )) {
						curManData.removeTransLib(transTable.getSelectedRow());
						saveProject();
						transTable.getSelectionModel().clearSelection();
						updateUI();
					}
				}
			}
		});
		
		btnAddTransLib.setMinimumSize(btnRemoveTransLib.getPreferredSize());
		btnAddTransLib.setMaximumSize(btnRemoveTransLib.getPreferredSize());
		btnEditTransLib.setMinimumSize(btnRemoveTransLib.getPreferredSize());
		btnEditTransLib.setMaximumSize(btnRemoveTransLib.getPreferredSize());
		
		buttonPanel.add(btnAddTransLib);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnEditTransLib);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnRemoveTransLib);
		
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		
		pnlTransLib.add(buttonPanel);
	}
	/**************************************************************
	 * Table of TransLibs with Edit/Define Reps buttons
	 */
	private void createCountLibPanel() {
		pnlCountLib = new JPanel();
		pnlCountLib.setLayout(new BoxLayout(pnlCountLib, BoxLayout.LINE_AXIS));
		pnlCountLib.setBackground(Globals.BGCOLOR);
		pnlCountLib.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		countTable = new ManagerTable(ManagerTable.EXP_LIB_MODE);
		countTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				updateUI();
			}
		});
			
		sPaneCountLib = new JScrollPane();
		sPaneCountLib.setViewportView(countTable);
		countTable.getTableHeader().setBackground(Color.WHITE);
		sPaneCountLib.setColumnHeaderView(countTable.getTableHeader());
		sPaneCountLib.setAlignmentX(Component.LEFT_ALIGNMENT);
    	
		sPaneCountLib.getViewport().setBackground(Color.WHITE);
		sPaneCountLib.getHorizontalScrollBar().setBackground(Color.WHITE);
		sPaneCountLib.getVerticalScrollBar().setBackground(Color.WHITE);
		sPaneCountLib.getHorizontalScrollBar().setForeground(Color.WHITE);
		sPaneCountLib.getVerticalScrollBar().setForeground(Color.WHITE);
		
		sPaneCountLib.getViewport().setMaximumSize(new Dimension(TWIDTH, THEIGHT));
		sPaneCountLib.getViewport().setPreferredSize(new Dimension(TWIDTH, THEIGHT));
		sPaneCountLib.getViewport().setMinimumSize(new Dimension(TWIDTH, THEIGHT));
    		pnlCountLib.add(sPaneCountLib);
    		pnlCountLib.add(Box.createHorizontalStrut(5));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonPanel.setBackground(Globals.BGCOLOR);
		
		btnDefRep = new JButton("Define Replicates");
		btnDefRep.setBackground(Globals.MENUCOLOR);
		btnDefRep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				pnlGenReps.setRepList(curManData);
				mainPanel.setVisible(false);
				pnlGenReps.setVisible(true);
			}
		});
		btnEditExpLib = new JButton("Edit Attributes");
		btnEditExpLib.setBackground(Globals.MENUCOLOR);
		btnEditExpLib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isCountSelected()) {
					nFrameMode = FRAME_MODE_COUNT_LIB_EDIT;
					nCurCountLibIndex = countTable.getSelectedRow();
					bRemoveExpLibOnDiscard = false;
					pnlCountLibEdit.updateExpLibEditUI(nCurCountLibIndex, false);
					mainPanel.setVisible(false);
					pnlCountLibEdit.setVisible(true);
				}
			}
		});
		btnEditExpLib.setEnabled(false);
		
		btnEditExpLib.setMinimumSize(btnDefRep.getPreferredSize());
		btnEditExpLib.setMaximumSize(btnDefRep.getPreferredSize());
		btnDefRep.setMaximumSize(btnDefRep.getPreferredSize());
		btnDefRep.setMinimumSize(btnDefRep.getPreferredSize());
		
		buttonPanel.add(Box.createVerticalStrut(40));
		buttonPanel.add(btnDefRep);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnEditExpLib);
		buttonPanel.add(Box.createVerticalStrut(40));
		pnlCountLib.add(buttonPanel);
	}
	/********************************************************
	 * annoDB panel on main window with Add/Edit/Remove/MoveUp/MoveDown
	 */
	private void createAnnoDBPanel() {
		pnlAnnoDB = new JPanel();
		pnlAnnoDB.setLayout(new BoxLayout(pnlAnnoDB, BoxLayout.LINE_AXIS));
		pnlAnnoDB.setBackground(Globals.BGCOLOR);
		pnlAnnoDB.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		annoDBTable = new ManagerTable(ManagerTable.ANNODB_MODE);
		annoDBTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				updateUI();
			}
		});	
		annoDBTable.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent ev) {
				for(int x=0; x<annoDBTable.getRowCount(); x++) {
					curManData.getAnnoDBAt(x).setSelected(
							(Boolean)annoDBTable.getModel().getValueAt(x, 0));
				}
			}
		});
	
		sPaneAnnoDB = new JScrollPane();
		sPaneAnnoDB.setViewportView(annoDBTable);
    	annoDBTable.getTableHeader().setBackground(Color.WHITE);
    	sPaneAnnoDB.setColumnHeaderView(annoDBTable.getTableHeader());
    	sPaneAnnoDB.setAlignmentX(Component.LEFT_ALIGNMENT);
    	
    	sPaneAnnoDB.getViewport().setBackground(Color.WHITE);
    	sPaneAnnoDB.getHorizontalScrollBar().setBackground(Color.WHITE);
    	sPaneAnnoDB.getVerticalScrollBar().setBackground(Color.WHITE);
    	sPaneAnnoDB.getHorizontalScrollBar().setForeground(Color.WHITE);
    	sPaneAnnoDB.getVerticalScrollBar().setForeground(Color.WHITE);
		
    	sPaneAnnoDB.getViewport().setMaximumSize(new Dimension(TWIDTH, THEIGHT));
    	sPaneAnnoDB.getViewport().setPreferredSize(new Dimension(TWIDTH, THEIGHT));
    	sPaneAnnoDB.getViewport().setMinimumSize(new Dimension(TWIDTH, THEIGHT));
    	pnlAnnoDB.add(sPaneAnnoDB);
    	pnlAnnoDB.add(Box.createHorizontalStrut(5));
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonPanel.setBackground(Globals.BGCOLOR);
		
		btnAddAnnoDB = new JButton("Add");
		btnAddAnnoDB.setBackground(Globals.MENUCOLOR);
		btnAddAnnoDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nFrameMode = FRAME_MODE_ANNO_DB_EDIT;
				nCurAnnoIndex = curManData.addNewAnnoDB();
				bAddAnno = true;
				pnlAnnoDBEdit.updateAnnoDBEditUI(nCurAnnoIndex, bAddAnno);
				
				mainPanel.setVisible(false);
				pnlAnnoDBEdit.setVisible(true);				
			}
		});
		
		btnEditAnnoDB = new JButton("Edit");
		btnEditAnnoDB.setBackground(Globals.MENUCOLOR);
		btnEditAnnoDB.setEnabled(false);
		btnEditAnnoDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isAnnoDBSelected()) {
					nFrameMode = FRAME_MODE_ANNO_DB_EDIT;
					nCurAnnoIndex = annoDBTable.getSelectedRow();
					bAddAnno = false;
					pnlAnnoDBEdit.updateAnnoDBEditUI(nCurAnnoIndex, bAddAnno);
					
					mainPanel.setVisible(false);
					pnlAnnoDBEdit.setVisible(true);	
				}
			}
		});
		
		btnRemoveAnnoDB = new JButton("Remove");
		btnRemoveAnnoDB.setBackground(Globals.BGCOLOR);
		btnRemoveAnnoDB.setEnabled(false);
		btnRemoveAnnoDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isAnnoDBSelected()) {
					int row = annoDBTable.getSelectedRow();
					if (!curManData.getAnnoDBAt(row).isLoaded()) {
						if (UserPrompt.showConfirm("Remove table entry", 
								"Remove selected annoDB")) {
							curManData.removeAnnoDB(row);
							annoDBTable.getSelectionModel().clearSelection();
							curManData.saveSTCWcfg(); 
							updateUI();
						}
					}
					else {
						UserPrompt.showWarn("Cannot remove an annoDB because it is in the database");
					}
				}
			}
		});
		
		btnMoveUpAnnoDB = new JButton("Move Up");
		btnMoveUpAnnoDB.setBackground(Globals.BGCOLOR);
		btnMoveUpAnnoDB.setEnabled(false);
		btnMoveUpAnnoDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isAnnoDBSelected()) {
					int row = annoDBTable.getSelectedRow();
					curManData.moveAnnoDB(row, row-1);
					updateUI();
					annoDBTable.getSelectionModel().setSelectionInterval(row-1, row-1);
				}
			}
		});
		
		btnMoveDownAnnoDB = new JButton("Move Down");
		btnMoveDownAnnoDB.setBackground(Globals.BGCOLOR);
		btnMoveDownAnnoDB.setEnabled(false);
		btnMoveDownAnnoDB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (isAnnoDBSelected()) {
					int row = annoDBTable.getSelectedRow();
					curManData.moveAnnoDB(row, row+1);
					updateUI();
					annoDBTable.getSelectionModel().setSelectionInterval(row+1, row+1);
				}
			}
		});
		
		btnAddAnnoDB.setMinimumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnAddAnnoDB.setMaximumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnEditAnnoDB.setMinimumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnEditAnnoDB.setMaximumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnRemoveAnnoDB.setMinimumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnRemoveAnnoDB.setMaximumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnMoveUpAnnoDB.setMinimumSize(btnMoveDownAnnoDB.getPreferredSize());
		btnMoveUpAnnoDB.setMaximumSize(btnMoveDownAnnoDB.getPreferredSize());
		
		buttonPanel.add(btnAddAnnoDB);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnEditAnnoDB);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnRemoveAnnoDB);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnMoveUpAnnoDB);
		buttonPanel.add(Box.createVerticalStrut(5));
		buttonPanel.add(btnMoveDownAnnoDB);
		
		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
		
		pnlAnnoDB.add(buttonPanel);
	}
	
	/*****************************************************************
	 * XXX routine for main window
	 */
	public boolean saveProject() {
		if (curManData == null) return false;
		
		if(!isValidID(txtdbID.getText())) { // may have been changed since checked
			UserPrompt.showError("singleTCW ID is invalid. \n   " +
			"It must start with a letter, and only contain letters, '_' or digits");
			return false;
		}

		try {
			curManData.setProjID(txtdbID.getText());
			curManData.setTCWdb(txtdbName.getText());
			String cpu = txtNumCPUs.getText();
			if(cpu.length() > 0) curManData.setNumCPUs(Integer.parseInt(cpu));
			else curManData.setNumCPUs(1);
			
			curManData.setSkipAssembly(chkSkipAssembly.isSelected());
			curManData.setUseTransNames(chkUseTransName.isEnabled() && chkUseTransName.isSelected());
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error updating project data");}
		
		return curManData.saveLIBcfg_sTCWcfg();
	}
	private boolean copyProject() {
		if (curManData == null) {
			UserPrompt.showWarn("No current project to copy");
			return false;
		}
		try {
			String oProj = txtdbName.getText().substring(STCW.length());
			String nProj = oProj + COPYPROJ;
			String nfullDB = txtdbName.getText() + COPYPROJ;
			String nID = txtdbID.getText() + COPYPROJ;
			System.out.println("Create " + nfullDB + " with ID " + nID);
			
			if (new File(PROJDIR + nProj).exists()) {
				UserPrompt.showWarn("Directory exists '" + PROJDIR + nProj + "' -- abort copy");
				return false;
			}
			String [] ignore = {Globalx.pORFDIR, Globalx.pLOGDIR, Globalx.pHITDIR};
			if (!FileHelpers.copyDir(new File(PROJDIR + oProj), new File(PROJDIR + nProj), ignore)) {
				UserPrompt.showError("Could not create copied '" + PROJDIR + nProj + "'");
				return false;
			}
		
			curManData.saveCopyCfg(nID, nfullDB, nProj);
			
			txtdbID.setText(nID);
			txtdbName.setText(nfullDB);
			cmbProjects.addItem(nProj);
			cmbProjects.setSelectedItem(nProj);
			projDirName = nProj;
			updateCurManData(cmbProjects.getSelectedIndex());
			
			updateUI();
			//saveProject();
		
			repaint();
			System.out.println("Complete creating copy of project");
			return true;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error updating project data");}
		
		return curManData.saveLIBcfg_sTCWcfg();
	}
	private boolean addProject () {
		try {
			String result = JOptionPane.showInputDialog(getInstance(), 
					"Enter name for new project", "", JOptionPane.PLAIN_MESSAGE);
			if(result != null && isValidID(result)) {
				if (result.startsWith(STCW)) 
					result = result.substring(STCW.length()); 
				cmbProjects.addItem(result);
				cmbProjects.setSelectedItem(result);
				
				projDirName = result;
				updateCurManData(cmbProjects.getSelectedIndex());
				
				chkSkipAssembly.setSelected(true);
				updateUI();
				saveProject();
			
				repaint();
				return true;
			}
			else {
				if(result != null)
					UserPrompt.showError("Invalid Project Name '" + result + "'. \n " +
						"   It must start with a letter, and contain only letters, '_' or digits");
				return false;
			}
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error adding project"); return false;}
	}
	private void refreshProjectList() {
		String [] projs = getLibraryList(false);
		
		cmbProjects.removeAllItems();
		
		cmbProjects.addItem("Select...     ");
		
		for(int x=0; x<projs.length; x++)
			cmbProjects.addItem(projs[x]);
		
		cmbProjects.setSelectedIndex(0);
	}
	
	
	/*****************************************************************
	 * XXX Database routines
	 */
	public DBConn tcwDBConn() {
		try {
			if(! hostsObj.checkDBConnect(curManData.getTCWdb())) {
				System.err.println("Problem with mySQL");
				return null;
			}
			return hostsObj.getDBConn(curManData.getTCWdb());
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Cannot open database connection");
			return null;
		}
	}
	
	private boolean tcwDBexists() {
		if (curManData == null) {
			System.err.println("Error: current project is null");
			return false;
		}
		dbExists = hostsObj.checkDBConnect(curManData.getTCWdb());
		return dbExists;
	}
	
	public void tcwDBselectShowOverview() {
		try {
			String val = null;
			
			if(!dbExists) {
				val = "Database does not exist";
				UserPrompt.displayInfoMonoSpace(getInstance(), "Overview for " + 
						curManData.getProjDir(), val.split("\n"), false, true);
				return;
			}
			
			DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());
			ResultSet rs = mDB.executeQuery("SELECT pja_msg, meta_msg FROM assem_msg");
				
			if(rs.first()) {
				val = rs.getString(1);
				if (val==null || (val != null && val.length() <= 10)) {
					Overview ov = new Overview(mDB);
					Vector <String> lines = new Vector <String> ();
					val = ov.createOverview(lines);
				}
				else val = rs.getString(1) + rs.getString(2);
			}
			rs.close();
			mDB.close();
			
			if (val==null && mDB.executeCount("select count(*) from library")>0) {
				val = "Datasets:\n";
				val += String.format("%10s %8s %10s   %s\n", "ID", "Size", "SeqID", "Info");
				rs = mDB.executeQuery("Select libid, libsize, parent, fastafile, reps from library");
				while (rs.next()) {
					String trans = rs.getString(3);
					if (trans.equals("")) trans = "N/A";
					String file = rs.getString(4);
					String reps = rs.getString(5);
					String info = (file.equals("")) ? reps : file;
					val += String.format("%10s %8d %10s   %s\n", 
							rs.getString(1), rs.getInt(2), trans, info);
				}
			}
			if (val==null) val = "Empty database -- delete and restart.";
			
			UserPrompt.displayInfoMonoSpace(getInstance(), "Overview for " + 
					curManData.getProjDir(), val.split("\n"), false, true);
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting project overview");
		}
	}
	public void tcwDBselectLoadStatus() {
		try {
			curManData.setLibraryLoaded(false);
			curManData.setInstantiated(false);
			if(!tcwDBexists()) return;
			
			DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());
			
			isAAtcw=false;
			if (!mDB.tableExists("assem_msg")) {
				UserPrompt.showError("The database has missing tables.");
				return;
			}
			if (mDB.tableColumnExists("assem_msg", "peptide")) isAAtcw=true;
			curManData.setProteinDB(isAAtcw);
			
			int count = mDB.executeCount("select count(*) from clone");
			if (count>0) curManData.setLibraryLoaded(true);
			count = mDB.executeCount("select count(*) from contig");
			if (count>0) curManData.setInstantiated(true);
			
			Vector<String> fastaDBs = new Vector<String> ();
			for(int x=0; x<curManData.getNumAnnoDBs(); x++) {
				if (curManData.getAnnoDBAt(x)!=null && curManData.getAnnoDBAt(x).getFastaDB()!=null) {
					fastaDBs.add(curManData.getAnnoDBAt(x).getFastaDB()); 
					curManData.getAnnoDBAt(x).setLoaded(false);
				}
			}
			// once the setLoaded is set true, italic are used 
			ResultSet rs = mDB.executeQuery("SELECT path FROM pja_databases");
			while(rs.next()) {
				String path = rs.getString(1);
				for (int i=0; i<fastaDBs.size(); i++) {
					String db = fastaDBs.get(i);
					if(db.equals(path)) curManData.getAnnoDBAt(i).setLoaded(true);
					else if (db.endsWith(path))  curManData.getAnnoDBAt(i).setLoaded(true);
					else if (path.endsWith(db))  curManData.getAnnoDBAt(i).setLoaded(true);
				}
			}
			rs.close();	mDB.close();	
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting load status");
		}
	}
	
	
	public void tcwDBupdateLibAttributes(int type) { //0 - trans, 1 - count
		if(curManData == null || !curManData.isLibraryLoaded()) return;
		
		try {
			DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());
			String theStatement;
			if (type==0) theStatement = curManData.getTransLib(nCurTransLibIndex);
			else         theStatement = curManData.getCountLib(nCurCountLibIndex);
			mDB.executeUpdate(theStatement);
			
			mDB.executeUpdate("UPDATE assem_msg SET pja_msg = NULL where AID=1");
			mDB.close();
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error updating attributes");
		}
	}
	public void tcwDBupdateAnnoDBTaxonomy() {
		if(curManData == null || !curManData.isLibraryLoaded()) return;
			
		try {
			DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());

			String theStatement = "UPDATE pja_databases SET ";
			theStatement += "taxonomy = '" + curManData.getAnnoDBAt(nCurAnnoIndex).getTaxo() + "' ";
			theStatement += "WHERE path = '" + curManData.getAnnoDBAt(nCurAnnoIndex).getFastaDB() + "'";
			mDB.executeUpdate(theStatement);
			
			mDB.executeUpdate("UPDATE assem_msg SET pja_msg = NULL where AID=1");
			mDB.close();
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error updating taxonomy for annoDB");
		}
	}
	
	// XXX
	public boolean tcwDBupdateTaxonomyForLoadedAnnoDB(String newTax, String oldTax) {
		if(curManData == null) return false;
			
		try {
			DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());
			String annoFile = curManData.getAnnoDBAt(nCurAnnoIndex).getFastaDB();
			Out.Print("Change " + oldTax + " to " + newTax + " for annoDB " + annoFile);
			
			if (!annoFile.startsWith("/")) 
				annoFile = Globalx.ANNODIR + "/" + annoFile;

			int dbid = mDB.executeInteger("select DBID from pja_databases where path='" + annoFile + "'");
			if (dbid<=0) {
				UserPrompt.showWarn("Inconsistency with annoDB FASTA file -- cannot change taxonomy");
				Out.prtToErr("AnnoDB: " + annoFile);
				Out.prtToErr("AnnoDB in database:");
				ResultSet rs = mDB.executeQuery("select dbtype, taxonomy, path from pja_databases");
				while(rs.next()) {
					Out.prtToErr(String.format("%5s %15s %s", rs.getString(1), rs.getString(2), rs.getString(3)));
				}
				mDB.close();
				return false;
			}
			
			Out.PrtSpMsg(1, "Updating database table...");
			mDB.executeUpdate("UPDATE pja_databases SET " +
					"taxonomy = '" + newTax + "' WHERE DBID=" + dbid);
			
			Out.PrtSpMsg(1, "Updating hit table...");
			mDB.executeUpdate("UPDATE pja_db_unique_hits set " +
					"taxonomy = '" + newTax + "' WHERE DBID=" + dbid);
			
			Out.PrtSpMsg(1, "Updating sequence table...");
			mDB.executeUpdate("UPDATE pja_db_unitrans_hits t, pja_db_unique_hits h " +
					"SET t.taxonomy = h.taxonomy " +
					"WHERE h.DBID=" + dbid + " and h.DUHID = t.DUHID");
			
			mDB.executeUpdate("UPDATE assem_msg SET pja_msg = NULL where AID=1");
			mDB.close();
			Out.Print("Change complete");
			
			return true;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error updating taxonomy for annoDB");
			return false;
		}
	}
	/***************************************************************
	 * XXX other assorted routines and classes
	 */
	private String [] getLibraryList(boolean first) {
		try {
			File dir = new File(PROJDIR);
			if (!dir.exists()) {
			    System.err.println("Creating directory " + PROJDIR);
			    boolean result = dir.mkdir();  
			    if(!result) {
			    	System.err.println("Error: could not create /" + PROJDIR + " Check permissions"); 
			    	return new String [0];
			    }
			    // CAS316 was under creating new project
				File userFile = new File(PROJDIR + Globalx.USERDIR);
				Out.PrtSpMsg(1,"Creating: " + userFile.getAbsolutePath());
				Out.PrtSpMsg(2, "This is not a project directory - it is for the user's miscellaneous files");
				userFile.mkdir();
			}
			if (first) System.err.println("Reading projects from directory '" + PROJDIR + "'");
		
			Vector<String> dirs = new Vector<String> ();
			String [] files = dir.list();
			for(int x=0; x<files.length; x++) {
				if (files[x].equals(Globalx.ANNOSUBDIR)) continue;
				if (files[x].equals(Globalx.HTMLDIR)) continue;
				if (files[x].equals(Globalx.USERDIR)) continue;
				if (files[x].equals(Globalx.DEDIR)) continue;
				
				File temp = new File(PROJDIR + files[x]);
				if(temp.exists() && temp.isDirectory() && !temp.isHidden())
					dirs.add(files[x]);
			}
			
			Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
			String [] retVal = new String[dirs.size()];
			for(int x=0; x<retVal.length; x++)
				retVal[x] = dirs.get(x);
			
			dirs.clear();
			return retVal;
		}
		catch(Exception e) {return null;}
	}
	/***************************************************
	 * Set new curManData
	 */
	private void updateCurManData(int index) {
		if (curManData!=null) curManData.clear();
		pnlTransLibEdit.clearCountFileList();
		
		projDirName = cmbProjects.getSelectedItem();
		
		curManData = new ManagerData(this, projDirName, hostsObj);
		if (curManData==null) ErrorReport.die("TCW error: remove LIB.cfg and start again");
		
		boolean b = tcwDBexists();
		if (!b) {
			curManData.setLibraryLoaded(false);
			curManData.setInstantiated(false);
		}
		else tcwDBselectLoadStatus();
		
		int cnt=0, num=curManData.getNumAnnoDBs();
		for(int x=0; x<num; x++) {
			ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(x);
			if (annoObj.isSelected()) cnt++;
		}
		if (cnt==num && num!=0) btnCheck.setText("UnCheck All");
		else btnCheck.setText("Check All");
	}
	/*******************************************
	 * updates
	 */
	public void updateUI() {
		try {
			boolean isProjectSelected = (curManData != null);
			if(!isProjectSelected) {
				lblAnno.setText("");
				txtdbID.setText("");
				txtdbName.setText("");
				updateEnable(false);
				return;
			}
		
			updateMain(false);
			updateTrans(false);
			updateCount(false);
			updateAnnoDB(false);
				
			updateEnable(isProjectSelected);
		}
		catch (Exception e) {ErrorReport.prtReport(e,"Updating interface");}
	}
	public void updateMain(boolean bEnable) {
		txtdbID.setText(curManData.getProjID());
		txtdbName.setText(curManData.getTCWdb());
		
		int cpus = curManData.getNumCPUs();
		if (cpus<=0) cpus = Runtime.getRuntime().availableProcessors(); 
		if (cpus<=0) cpus = 1;
		txtNumCPUs.setText(cpus + "");
		
		chkSkipAssembly.setSelected(curManData.getSkipAssembly());
		chkUseTransName.setSelected(curManData.getUseTransNames());
		
		String lblMsg="";
		boolean hasGO = (curManData.getGODB().length()>0);
		if (hasGO) {
			lblMsg = "GO: " + curManData.getGODB();
			
			String noGO = curManData.getNoGO();
			if (noGO.contentEquals("1")) {
				lblMsg += " (Ign)";
			}
			else {
				String slim = curManData.getSlimSubset();
				if (!slim.equals(""))  lblMsg += " (" + slim + ")";
				else {
					String file = curManData.getSlimFile();
					if (!file.equals("")) lblMsg += " (Slim File)";
				}
			}
		}
		else lblMsg = "GO: none";
			
		String prune = curManData.getAnnoObj().getPruneType();
		if (prune.contentEquals("0"))		lblMsg += "  Prune: none";
		else if (prune.contentEquals("1")) 	lblMsg += "  Prune: align";
		else if (prune.contentEquals("2")) 	lblMsg += "  Prune: desc";
		
		int npairs = curManData.getAnnoObj().getDoPairs();
		if (npairs>0) 						lblMsg += "  Pairs: " + npairs;
		
		lblAnno.setText(lblMsg);
		if (bEnable) updateEnable(true);
	}
	public void updateTrans(boolean bEnable) {
		transTable.resetData();
		for(int x=0; x<curManData.getNumSeqLibs() && curManData.getTransLibraryAt(x) != null; x++) {
			String [] temp = new String[2];
			temp[0] = curManData.getTransLibraryAt(x).getSeqID();
			temp[1] = curManData.getTransLibraryAt(x).getAttr().getTitle();
			
			if(curManData.getTransLibraryAt(x).getCountFile().length() > 0)
				transTable.addRow(true, true, temp);
			else
				transTable.addRow(false, false, temp);
		}
		transTable.setVisible(false);
		transTable.setVisible(true);
		if (bEnable) updateEnable(true);
	}
	public void updateCount(boolean bEnable) {
		countTable.resetData();
		// adds content to the Transcrip Read Count table
		for(int x=0; x<curManData.getNumCountLibs(); x++) {
			String [] temp = new String[4]; 
			temp[0] = curManData.getCountLibAt(x).getSeqID();
			temp[1] = curManData.getCountLibAt(x).getCondID();
			temp[2] = curManData.getCountLibAt(x).getAttr().getTitle(); 
			temp[3] = curManData.getCountLibAt(x).getNumReps() + ""; 
			countTable.addRow(true, false, temp);
		}
		countTable.setVisible(false);
		countTable.setVisible(true);
		if (bEnable) updateEnable(true);
	}
	private void checkAllAnnos() {
		if (annoDBTable==null) return;
		
		boolean check;
		if (btnCheck.getText().equals("Check All")) {
			btnCheck.setText("UnCheck All");
			check = true;
		}
		else {
			btnCheck.setText("Check All");
			check = false;
		}	
		
		if (curManData!=null) { 
			for(int x=0; x<curManData.getNumAnnoDBs(); x++) {
				ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(x);
				annoObj.setSelected(check);
			}
		}
		updateAnnoDB(true);
		curManData.saveSTCWcfg(); 
	}
	private void removeAllAnnos() { 
		if (annoDBTable==null) return;
		if (curManData==null) return;
		
		if (!UserPrompt.showConfirm("Remove table entries", 
				"Remove all annoDBs from the table that are not already loaded in sTCW." +
				"\n\nNote: to remove all annotation from the sTCW database, " +
				"\n      use 'Remove...' at the top of this panel.")) return;
		
		// Each remove changes the indexes, hence, restart going through the table after each remove
		boolean rm=true;
		while (rm) {
			rm= false;
			for (int row=0; row<annoDBTable.getRowCount(); row++) {
				if (!curManData.getAnnoDBAt(row).isLoaded()) {
					curManData.removeAnnoDB(row);
					annoDBTable.getSelectionModel().clearSelection();
					updateAnnoDB(true);
					rm = true;
					break;
				}
			}
		}	
		curManData.saveSTCWcfg(); 
	}
	public void updateAnnoDB(boolean bEnable) {
		annoDBTable.resetData();
		for(int x=0; x<curManData.getNumAnnoDBs(); x++) {
			ManagerData.AnnodbData annoObj = curManData.getAnnoDBAt(x);
			if (annoObj == null) {
				Out.debug("ManagerFrame: TCW error in annoDB table " + x);
				continue;
			}
			String [] temp = new String[3];
			temp[0] = annoObj.getTaxo();
			temp[1] = annoObj.getTabularFile(); 
			temp[2] = annoObj.getFastaDB();
			if (!temp[1].equals("")) 
				 temp[1] = temp[1].substring(temp[1].lastIndexOf("/"));
			else temp[1] = annoObj.getSearchPgm();
			boolean isSelected = annoObj.isSelected();
			boolean isLoaded = annoObj.isLoaded();
			
			annoDBTable.addRow(isSelected, isLoaded, temp);
		}
		annoDBTable.setVisible(false);
		annoDBTable.setVisible(true);
		if (bEnable) updateEnable(true);
	}
	// enable/disable button on main window
	public void updateEnable(boolean isProjectSelected) {
		boolean bBusy = rCmd.isBusy();	
		if (bBusy) System.err.println("Command is executing....");
		
		boolean isProteinDB=false, islibLoaded=false, isInstantiate=false, hasGO=false;
		if (isProjectSelected) {
			isProteinDB = curManData.isProteinDB();
			islibLoaded = curManData.isLibraryLoaded();
			isInstantiate = curManData.isInstantiated();
			hasGO = (curManData.getGODB().length()>0);
		}
		boolean bNotLoadLib = (!bBusy && !islibLoaded && isProjectSelected);
		boolean bIsAssm =     (!bBusy && isInstantiate && isProjectSelected );
		boolean bHasProj =    (!bBusy &&  isProjectSelected);
		
		txtdbID.setEnabled(bNotLoadLib);
		txtdbName.setEnabled(bNotLoadLib);
		txtNumCPUs.setEnabled(!bBusy);
		btnGetState.setEnabled(bHasProj);
		btnSaveProject.setEnabled(bHasProj);
		btnCopyProject.setEnabled(bHasProj);
		btnRemoveProject.setEnabled(bHasProj);
		
		transTable.setEnabled(bHasProj);
		btnAddTransLib.setEnabled(bNotLoadLib);
		btnEditTransLib.setEnabled(bHasProj);
		btnRemoveTransLib.setEnabled(bNotLoadLib);
		
		countTable.setEnabled(bHasProj);
		btnEditExpLib.setEnabled(bHasProj);
		btnDefRep.setEnabled(bNotLoadLib);
		btnExecLoadLib.setEnabled(bNotLoadLib);
		
		boolean selected = chkSkipAssembly.isSelected();
		if (isProteinDB) chkSkipAssembly.setEnabled(false);
		else chkSkipAssembly.setEnabled(bHasProj);
		btnAssemOptions.setEnabled(bHasProj && !selected);
		chkUseTransName.setEnabled(bHasProj);
		btnExecAssem.setEnabled(bHasProj && islibLoaded && !isInstantiate);
		
		annoDBTable.setEnabled(bHasProj);
		btnAddAnnoDB.setEnabled(bHasProj);
		btnEditAnnoDB.setEnabled(bHasProj);
		btnRemoveAnnoDB.setEnabled(bHasProj);	
		btnMoveUpAnnoDB.setEnabled(bHasProj);	
		btnMoveDownAnnoDB.setEnabled(bHasProj);	
		
		sPaneAnnoDB.setEnabled(bHasProj);
		btnEditAnnoOptions.setEnabled(bHasProj);
		
		btnExecAnno.setEnabled(bIsAssm);
		btnImportAnnot.setEnabled(bHasProj);
		btnUpdateGO.setEnabled(bIsAssm && hasGO);
		btnUpdateORF.setEnabled(bIsAssm && !isProteinDB);
		btnAddRemark.setEnabled(bIsAssm);
		
		btnRunViewTCW.setEnabled(bIsAssm);
	}
	private boolean isCountSelected() {
		if (countTable.getRowCount() > 0 && countTable.getSelectedRow()>=0) return true;
		else {
			UserPrompt.showMsg("Select an row in the Count table to perform this operation");
			return false;
		}
	}
	private boolean isTransSelected() {
		if (transTable.getRowCount() > 0 && transTable.getSelectedRow()>=0) return true;
		else {
			UserPrompt.showMsg("Select an row in the Sequence table to perform this operation");
			return false;
		}
	}
	private boolean isAnnoDBSelected() {
		if (annoDBTable.getRowCount() > 0 && annoDBTable.getSelectedRow()>=0) return true;
		else {
			UserPrompt.showMsg("Select an row in the annoDB table to perform this operation");
			return false;
		}
	}
	public boolean isLibraryLoaded() { 
		if(curManData == null) return false;
		return curManData.isLibraryLoaded();
	}
	public  boolean isValidID(String id) {
		return (id.matches("[A-Za-z]+[A-Za-z0-9_]*")); 
	}
	public boolean isRemoveAnnoOnDiscard() { return bAddAnno; }
	public void clearCurrentAnnoDB() { 
		curManData.removeAnnoDB(nCurAnnoIndex); 
	}
	public boolean isAddTransLib() { return bAddTransLib;}
	public void clearCurrentTransLib() { curManData.removeTransLib(nCurTransLibIndex); }
	
	public boolean isRemoveExpLibOnDiscard() { return bRemoveExpLibOnDiscard; }
	public void clearCurrentExpLib() { curManData.removeCountLib(nCurCountLibIndex); }
		
	private ManagerFrame getInstance() { return this; }
	public boolean isAAtcw() {return isAAtcw;}
	public ManagerData getCurManData() { return curManData;}
	
	/****************************************************
	 * Remove pop-up
	 */
	public class RemoveType extends JDialog {
		private static final long serialVersionUID = 1L;
		public static final int OK = 1;
	    	public static final int CANCEL = 2;
	    	   
        	public RemoveType() {
        		setModal(true);
        		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        		setTitle("Remove.... ");
    		
        		boolean b1 = tcwDBexists();
        		String blastDir = PROJDIR + curManData.getProjDir() +  "/" + Globalx.pHITDIR; 
        		boolean b2 = (new File(blastDir)).exists() ? true : false;
     			
        		JPanel selectPanel = Static.createPagePanel();
        		
        		btnAnno = Static.createCheckBox("Annotation from sTCW database", false, b1);
        		selectPanel.add(btnAnno);
	    		selectPanel.add(Box.createVerticalStrut(5));
	    		
	    		btnDB = Static.createCheckBox("sTCW database", false, dbExists);
		        selectPanel.add(btnDB);
		        selectPanel.add(Box.createVerticalStrut(5));
		        selectPanel.add(new JSeparator());
	        		
	            btnBlast = Static.createCheckBox("Hit files from disk (" + Globalx.pHITDIR + ")", 
	            		false, b2);
	            selectPanel.add(btnBlast);
		        selectPanel.add(Box.createVerticalStrut(5));
        	
        		btnAll = Static.createCheckBox("All files from disk for this sTCW project", false, true);
        		selectPanel.add(btnAll);		
        		selectPanel.add(Box.createVerticalStrut(5));
                    		
        		JPanel buttonPanel = Static.createRowPanel();
        		btnOK = new JButton("OK");
        		btnOK.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					nMode = OK;
    					setVisible(false);
    				}
    			});
        		buttonPanel.add(btnOK);
        		buttonPanel.add(Box.createHorizontalStrut(20));
        		
        		btnCancel = new JButton("Cancel");
        		btnCancel.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent e) {
    					nMode = CANCEL;
    					setVisible(false);
    				}
    			});
        		buttonPanel.add(btnCancel);
        		
        		btnOK.setPreferredSize(btnCancel.getPreferredSize());
        		btnOK.setMaximumSize(btnCancel.getPreferredSize());
        		btnOK.setMinimumSize(btnCancel.getPreferredSize());
        		buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
	    		nMode = CANCEL;

	    		JPanel mainPanel = Static.createPagePanel();
        		mainPanel.add(selectPanel);
        		mainPanel.add(Box.createVerticalStrut(15));
        		mainPanel.add(buttonPanel);
        		
        		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        		add(mainPanel);
        		
        		pack();
        		this.setResizable(false);
        		UIHelpers.centerScreen(this);
        		setAlwaysOnTop(true);
        	} 
        	private void removeAnno() {
        		try {
        			if(!dbExists) {
        				System.err.println("Database " + curManData.getTCWdb() + " does not exist");
        				return;
        			}
        			if (!UserPrompt.showConfirm("runSingleTCW", "Remove annotation from " + curManData.getTCWdb())) return;
        			
    				DBConn mDB = hostsObj.getDBConn(curManData.getTCWdb());
    				CoreDB sqlObj = new CoreDB(mDB);
    				sqlObj.deleteAnnotation(true);
    				mDB.close();
    				
    				curManData.clearAnnoLoaded();
    				updateUI();
        		}
        		catch(Exception e) {ErrorReport.prtReport(e, "Deleting annotation");}
        	}
        	private void removeDB() {
        		try {
        			if(!dbExists) {
        				Out.Print("Database " + curManData.getTCWdb() + " does not exist");
        				return;
        			}
        			if (!UserPrompt.showConfirm("runSingleTCW", "Remove database " + curManData.getTCWdb())) return;
        			
        			DBConn.deleteMysqlDB(hostsObj.host(), curManData.getTCWdb(), 
        					hostsObj.user(), hostsObj.pass());
        			
        			curManData.setLibraryLoaded(false);
        			curManData.setInstantiated(false);
        			for(int x=0; x<curManData.getNumAnnoDBs(); x++)
        				curManData.getAnnoDBAt(x).setLoaded(false);
        			dbExists=false;
        			
        			Out.Print("Removed database " + curManData.getTCWdb() + " successfully");
        		} 
        		catch (Exception e){ErrorReport.reportError(e, "Cannot delete database " + curManData.getTCWdb());}
        	}
 
        	private void removeBlast() {
        		try
        		{
    				String blastDir = PROJDIR + curManData.getProjDir() +  "/" + Globalx.pHITDIR; 
    				if(!(new File(blastDir)).exists()) {
    					System.out.println("Hit directory does not exist for this project");
        				return;
    				}
    				if (!UserPrompt.showConfirm("runSingleTCW", "Remove hit files")) return;
    	
        			boolean rc = FileHelpers.deleteDir(new File(blastDir));
        			
        			if (rc) System.out.println("Hit files removed successfully");
    				else System.out.println("Hit file remove failed");
        		} 
        		catch (Exception e){ErrorReport.reportError(e, "Cannot delete hit files for " + projDirName);}
        	}
        	private void removeProject() {
        		String msg = "Remove sTCW project '" + curManData.getProjDir() + "' from disk";
        		if (!UserPrompt.showConfirm("runSingleTCW", msg)) return;
    			
				try {
					File fProj = new File(PROJDIR + curManData.getProjDir());
					 
					boolean rc = FileHelpers.deleteDirTrace(fProj);
					if (rc) System.out.println("Project removed successfully");
					else System.out.println("Project remove failed");
	
					refreshProjectList();
					
					if (curManData!=null) {
						curManData.clear(); 
						curManData = null;
					}
					transTable.resetData();
					countTable.resetData();
					annoDBTable.resetData();
					
					updateUI();
				}
			catch(Exception e) {ErrorReport.prtReport(e, "Error removing project");}
        	}
        	 	
        	public boolean isCancel() {
        		if (nMode==CANCEL) return true; 
        		else return false;
        	}
        	public void doOp() {
        		if (btnAnno.isSelected()) {
        			removeAnno();
        			btnAnno.setSelected(false);
        		}
        		if (btnDB.isSelected()) {
        			removeDB();
        			btnAnno.setSelected(false);
        		}
        		if (btnBlast.isSelected()) {
        			removeBlast();
        			btnBlast.setEnabled(false);
        		}
        		if (btnAll.isSelected()) {
        			removeProject();
        			btnBlast.setEnabled(false);
        		}			
        	}
        	JCheckBox btnAnno = null, btnDB = null, btnBlast = null, btnAll = null;
        	JButton btnOK = null, btnCancel = null;
        	int nMode = -1;
	} // end RemoveType class
	/********************************************************
	 * XXX Private variables
	 */
	private JPanel pnlProject = null;
	private JPanel pnlCountLib = null;
	private JPanel pnlTransLib = null;
	private JPanel pnlAnnoDB = null;
	private JPanel pnlGeneral = null;
	
	private AssmOptionsPanel pnlAssemblyOptions = null;
	private EditAnnoPanel pnlAnnoDBEdit = null;
	private EditTransLibPanel pnlTransLibEdit = null;
	private EditCountPanel pnlCountLibEdit = null;
	private GenRepsPanel pnlGenReps = null;
	private AddRemarkPanel pnlRemark = null;
	private AnnoOptionsPanel pnlAnnoDBOptions = null;
	
	private JPanel mainPanel = null;
	private JScrollPane sPane = null;
	
	//Project controls
	private ButtonComboBox cmbProjects = null;
	private JButton btnAddProject = null;
	private JButton btnSaveProject = null;
	private JButton btnCopyProject = null;
	private JButton btnRemoveProject = null;
	private JButton btnHelp = null;
	private JButton btnGetState = null;
	
	//Exp Library controls
	private ManagerTable countTable = null;
	private JScrollPane sPaneCountLib = null;
	private JButton btnEditExpLib = null;
	private JButton btnDefRep = null;
	
	//Trans Library controls
	private ManagerTable transTable = null;
	private JScrollPane sPaneTransLibrary = null;
	private JButton btnAddTransLib = null;
	private JButton btnEditTransLib = null;
	private JButton btnRemoveTransLib = null;
	
	private JButton btnAssemOptions = null;
	
	private JButton btnExecLoadLib = null;
	private JButton btnExecAssem = null;
	private JButton btnExecAnno = null;
	private JButton btnRunViewTCW = null;
	
	private JButton btnImportAnnot = null;
	private JButton btnAddRemark = null;
	private JButton btnUpdateGO = null;
	private JButton btnUpdateORF = null;

	private JLabel lblAnno = null;
	private JLabel lblSim = null;
	
	//AnnoDB controls
	private ManagerTable annoDBTable = null;
	private JScrollPane sPaneAnnoDB = null;
	private JButton btnAddAnnoDB = null;
	private JButton btnEditAnnoDB = null;
	private JButton btnRemoveAnnoDB = null;	
	private JButton btnMoveUpAnnoDB = null;
	private JButton btnMoveDownAnnoDB = null;
	private JButton btnEditAnnoOptions = null;
	private JButton btnCheck = null, btnRemove;
	
	//General controls
	private JTextField txtdbID = null;
	private JTextField txtdbName = null;
	private JTextField txtNumCPUs = null;

	private JCheckBox chkSkipAssembly = null;
	private JCheckBox chkUseTransName = null;
	
	//AnnoDB Edit
	private int nCurAnnoIndex = -1;
	private boolean bAddAnno = false; 
	
	//Exp Library controls
	private int nCurCountLibIndex = -1;
	private boolean bRemoveExpLibOnDiscard = false;
	
	//Read Library controls
	private int nCurTransLibIndex = -1;
	private boolean bAddTransLib = false;
	
	private Vector<String> theHosts = null;
	
	// state
	private boolean isAAtcw=false;
	private boolean dbExists=false;	
	private int nFrameMode = FRAME_MODE_MAIN;
	
	private String projDirName=null; // may not be the same as the singleTCW ID; name only
	private String host="";
	private ManagerData curManData = null;
	private HostsCfg hostsObj = null;
	private RunCmd rCmd = null;
}
