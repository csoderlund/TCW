package cmp.compile.panels;
/*****************************************************
 * Top panel  of multiTCW window
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import util.database.DBConn;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.ui.ButtonComboBox;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import cmp.compile.runMTCWMain;
import cmp.compile.Summary;
import cmp.database.DBinfo;
import cmp.database.Globals;

public class ProjectPanel extends JPanel {
	private static final long serialVersionUID = -5440876354212703010L;
	private final String helpHTML = Globals.helpRunDir + "ManagerPanel.html";
	
	public ProjectPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		mainPanel = Static.createPagePanel();
		JPanel row1 = Static.createRowPanel();
		
		btnAdd = Static.createButtonMenu("Add Project", true);
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theCompilePanel.updateClearInterface();
				addProject();
				
				theCompilePanel.mTCWcfgNew(); 
				theCompilePanel.mTCWcfgRead();
			}
		});
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theCompilePanel.getParentFrame(), "runMultiTCW", helpHTML);
			}
		});
		row1.add(btnAdd);
		row1.add(Box.createHorizontalStrut(10));
		row1.add(btnHelp);
		row1.setMaximumSize(row1.getPreferredSize());
		row1.setMinimumSize(row1.getPreferredSize());
		
		mainPanel.add(row1);
		mainPanel.add(Box.createVerticalStrut(10));

	// row2 project
		JPanel row2 = Static.createRowPanel();
		row2.add(new JLabel("Project"));
		row2.add(Box.createHorizontalStrut(5));
		
		Out.prt("Reading comparison projects from directory /" + Globals.PROJECTDIR);
		String [] projNames = findProjcmpDirs();
		cmbProject = new ButtonComboBox();
		cmbProject.addItems(projNames);
		cmbProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(cmbProject.getSelectedIndex() > 0) 
					theCompilePanel.mTCWcfgRead();	  
			}
		});
		row2.add(cmbProject);
		row2.add(Box.createHorizontalStrut(10));
		
		btnSave = Static.createButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(cmbProject.getSelectedIndex() > 0)
					theCompilePanel.mTCWcfgSave();
			}
		});
		row2.add(btnSave);
		row2.add(Box.createHorizontalStrut(10));
		
		btnRemove = Static.createButtonMenu("Remove...", true);
		btnRemove.setAlignmentX(Component.RIGHT_ALIGNMENT);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				remove();
			}
		});
		
		row2.add(btnRemove);
		row2.add(Box.createHorizontalStrut(10));
		
		btnOverview = Static.createButtonPopup("Overview", true);
		btnOverview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dbShowOverview();
			}
		});
		row2.add(btnOverview);
		row2.setMaximumSize(row2.getPreferredSize());
		row2.setMinimumSize(row2.getPreferredSize());
		mainPanel.add(row2);
		mainPanel.add(Box.createVerticalStrut(10));
		
	// Row3 database	
		JPanel row3 = Static.createRowPanel();
		JLabel lblDBName = new JLabel("mTCW database"); 
		row3.add(lblDBName);
		row3.add(Box.createHorizontalStrut(5));
		
		txtDBName = new JTextField(10);
		txtDBName.setMaximumSize(txtDBName.getPreferredSize());
		txtDBName.setMinimumSize(txtDBName.getPreferredSize());
		row3.add(txtDBName);
		row3.add(Box.createHorizontalStrut(15));
		row3.add(new JLabel("Host: " + theCompilePanel.getHosts().host()));
		
		row3.setMaximumSize(row3.getPreferredSize());
		row3.setMinimumSize(row3.getPreferredSize());
		mainPanel.add(row3);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(mainPanel);
	}
	private static String [] findProjcmpDirs() { // existing projects on disk
	    	File dir = new File(Globals.PROJECTDIR + "/");
	    	if(!dir.exists()) dir.mkdir();
	    	
	    	Vector<String> results = new Vector<String> ();
	    	results.add("Select...");
	    	
	    	String [] allFiles = dir.list();
	    	for(int x=0; x<allFiles.length; x++) {
	    		if (allFiles[x].equals(Globals.summaryDir)) continue;
	    		
	    		File temp = new File(Globals.PROJECTDIR + "/" + allFiles[x]);
	    		
	    		if(temp.isDirectory()) results.add(allFiles[x]);
	    	}    
	    	Collections.sort(results); // CAS305
	    	return results.toArray(new String[results.size()]);
	}
	private boolean addProject() {
		try {
			String newProjectName = null;
			boolean valid = true;
			do {
				newProjectName = JOptionPane.showInputDialog(theCompilePanel.getParentFrame(), 
				        "Enter new project name", Globals.MTCW, JOptionPane.PLAIN_MESSAGE);
				if(newProjectName == null) continue;
				
				if (newProjectName.startsWith(Globals.MTCW)) { // CAS304
					newProjectName = newProjectName.substring(Globals.MTCW.length()+1);
				}
					
				valid = true;
				if (newProjectName.equals(Globals.summaryDir)) {
					JOptionPane.showMessageDialog(theCompilePanel, 
							Globals.summaryDir +
							" is a reserved directory name, please select another",
							"Invalid name", JOptionPane.PLAIN_MESSAGE);
					Out.PrtError("reserved directory name");
					valid = false;
				}
				if (!newProjectName.matches("[\\w\\d]+"))
				{
					JOptionPane.showMessageDialog(theCompilePanel, "Name contains invalid characters.\nUse letters,numbers,underscores.",
							"Invalid name", JOptionPane.PLAIN_MESSAGE);
					System.err.println("Error: name contains invalid characters");
					valid = false;
				}
				else
				{
					String projDir = Globals.PROJECTDIR + "/" + newProjectName + "/";// + CONFIGFILE;
					File temp = new File(projDir);
					
					if(temp.exists()) { 
						Out.PrtSpMsg(1, "Project exists: " + projDir);
						projDir += Globals.CONFIG_FILE;
						temp = new File(projDir);
						if (!temp.exists())
							(new File(projDir)).createNewFile();
					}
					else {
						Out.PrtSpMsg(1, "New project: " + projDir);
						temp.mkdir();
						projDir += Globals.CONFIG_FILE;
						(new File(projDir)).createNewFile();
						
						String methodDir = Globals.PROJECTDIR + "/" + newProjectName + "/" + Globals.Methods.METHODDIR;
						(new File(methodDir)).mkdir();
					}
				}
			} while(!valid && newProjectName != null);
			
			if(newProjectName != null) {
				updateProjectList(newProjectName);
				return true;
			}
			return false;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error creating new project"); return false;}
	}
	private void dbShowOverview() {
		try {
			String sum=null;
			if(txtDBName.getText().length() == 0) {
				System.out.println("No database name set");
				return;
			}
			String dbName = txtDBName.getText();
				
			if(!theCompilePanel.getHosts().checkDBConnect(dbName)) {
				sum = "No database available";
				UserPrompt.displayInfoMonoSpace(null, "Overview of " + dbName, sum.split("\n"), false, true);
				return;
			}
			DBConn conn = theCompilePanel.getHosts().getDBConn(dbName);
			sum = new Summary(conn).getSummary();
			conn.close();
			
			UserPrompt.displayInfoMonoSpace(null, "Overview of " + dbName, 
					sum.split("\n"), false, true);
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error getting summary");}
	}
	
	private void updateProjectList(String projectName) {
		String [] projNames = findProjcmpDirs();

		cmbProject.removeAllItems();
		for(int x=0; x<projNames.length; x++)
			cmbProject.addItem(projNames[x]);
		
		if (projectName == null) cmbProject.setSelectedIndex(0);
		else {
			cmbProject.setSelectedItem(projectName);
			Out.Print("Project: " + projectName);
		}
		
		cmbProject.setBackground(Globals.BGCOLOR);
		cmbProject.setMaximumSize(cmbProject.getPreferredSize());
		cmbProject.setMinimumSize(cmbProject.getPreferredSize());
		
		if (cmbProject.getSelectedIndex() > 0)
			txtDBName.setText(Globals.MTCW + "_" + projectName);
	}
	/*****************************************************/
	public void update(boolean exists) {
		btnSave.setEnabled(true);
		btnRemove.setEnabled(true);
		btnOverview.setEnabled(exists);
		txtDBName.setEnabled(!exists);
	}
	public void updateClearInterface() {
		txtDBName.setText("");
		
		btnSave.setEnabled(false);
		btnRemove.setEnabled(false);
		btnOverview.setEnabled(false);
		txtDBName.setEnabled(false);
	}
	
	public String getCurProjName() { 
		if (cmbProject.getSelectedIndex() == 0) return null;
		else  return  cmbProject.getSelectedItem();
	}
	
	public boolean checkDBName() {
		String name = txtDBName.getText();
		String [] tok = name.split("_");
		if (tok.length<2) return false;
		if (!tok[0].equals("mTCW")) return false;
		if (tok[1].trim().equals("")) return false;
		return true;
	}
	public String getDBName() { return txtDBName.getText(); }
	public void setDBName(String dbName) { txtDBName.setText(dbName); }
	
	/****************************************************************
	 * All remove methods
	 */
	private void remove() {
		final RemoveType remove = new RemoveType();
		remove.setVisible(true);	
		if (remove.isCancel()) return;
		remove.doOp();
		
		theCompilePanel.updateAll(); // update interface as it checks status of db
	}
	private class RemoveType extends JDialog {
		private static final long serialVersionUID = 1L;
		public static final int OK = 1;
    	public static final int CANCEL = 2;
    	private static final String blastDir = Globals.Search.BLASTDIR;
    	private static final String kaksDir = Globals.KaKsDIR;
    	private static final String methodDir = Globals.Methods.METHODDIR;
    	private static final String statsDir = Globals.StatsDIR;
    	   
    	public RemoveType() {
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Remove.... ");
		
    		// Database
    		boolean bDB = theCompilePanel.dbIsExist();
    		DBinfo info = theCompilePanel.getDBInfo();
    		boolean bPairs = (info!=null) ? (info.getCntPair()>0) : false;
    		
    		JPanel selectPanel = Static.createPagePanel();
    		
    		btnClust = Static.createCheckBox("Clusters from database", false, bPairs);
    		selectPanel.add(btnClust);
    		selectPanel.add(Box.createVerticalStrut(5));
    		
    		btnClustAndPair = Static.createCheckBox("Clusters and Pairs from database", false, bPairs);
    		selectPanel.add(btnClustAndPair);
    		selectPanel.add(Box.createVerticalStrut(5));
    		
    		btnDB = Static.createCheckBox("mTCW database", false, bDB);
	        selectPanel.add(btnDB);
	        selectPanel.add(Box.createVerticalStrut(5));
	        selectPanel.add(new JSeparator());
         		
	        // Files 
	        boolean b1=true, b2=true;
	        try {
        		String projDir = theCompilePanel.getCurProjRelDir();
        		
        		b1 = (FileHelpers.existDir(projDir + blastDir)) ? true : false;
        		
        		b2 = (FileHelpers.existDir(projDir + methodDir)) ? true : false;
        		if (!b2) {
        			b2 = (FileHelpers.existDir(projDir + statsDir)) ? true : false;
        			if (!b2)
        				b2 = (FileHelpers.existDir(projDir + kaksDir)) ? true : false;
        		}
	        }
	        catch (Exception e) {ErrorReport.reportError(e, "Project: could not determine if project directories exist");}
	        
    		btnPairDir = Static.createCheckBox("Pair and cluster files from disk (KaKs, Stats, Methods)", false, b2);
    		selectPanel.add(btnPairDir);
	        selectPanel.add(Box.createVerticalStrut(5));
	               
	        btnHitDir = Static.createCheckBox("Hit files from disk (" + blastDir + ")", false, b1);
            selectPanel.add(btnHitDir);
    	    selectPanel.add(Box.createVerticalStrut(5));
            	
        	btnAllDir = Static.createCheckBox("All files from disk for this mTCW project", false, true);
        	selectPanel.add(btnAllDir);		
         	selectPanel.add(Box.createVerticalStrut(5));
                    		
         	JPanel buttonPanel = Static.createRowPanel();
    		btnOK = Static.createButton("OK", true);
    		btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode = OK;
					setVisible(false);
				}
			});
    		buttonPanel.add(btnOK);
    		buttonPanel.add(Box.createHorizontalStrut(20));
    		
    		btnCancel = Static.createButton("Cancel", true);
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
    	} 
    	/*********************************************************/
    	private void removeDB() {
    		try
    		{
    			boolean valid = runMTCWMain.hosts.checkDBConnect(getDBName());
    			if (!valid) {
    				Out.PrtWarn("Database '" + getDBName() + "' does not exist on " + runMTCWMain.hosts.host());
    				return;
    			}
    			boolean ret = UserPrompt.showConfirm("Remove...", 
    					"Remove mTCW database '" + getDBName() + "'");
    			if (!ret) return;
    			
    			DBConn conn = runMTCWMain.hosts.getDBConn(getDBName());
    			conn.executeUpdate("drop database " + getDBName());
    			conn.close();
    			
    			Out.PrtSpMsg(2, "Successfully removed " + getDBName());
    			theCompilePanel.updateAll(); 
    		} 
    		catch (Exception e){ErrorReport.reportError(e, "Cannot delete database " + getDBName());}
    	}
      
    	public void removeClust() { // CAS326 added
    		try {
    			DBinfo info = theCompilePanel.getDBInfo();
    			if (info==null) {
    				Out.PrtWarn("Database does not exist");
    				return;
    			}
    			boolean ret = UserPrompt.showConfirm("Remove...", "Remove all clusters from database");
    			if (!ret) return;
    			
    			Out.PrtSpMsg(0, "Remove clusters from database");
    			info.clearCntKeys();
    			
    			DBConn mDB = runMTCWMain.hosts.getDBConn(getDBName());
    			
    			removeClust(mDB, info.getMethodPrefix(), true /* delete method from pairwise */);
 
    			mDB.executeUpdate("update pairwise set hasGrp=0, hasBBH=0");
    			
    			info.updateCntKeys(mDB); // removing clusters still leaves PCC and stats
    			
    			mDB.close();
    			
    			Out.PrtSpMsg(0, "Finish cluster removal");
    			theCompilePanel.updateAll(); 
    		}
    		catch (Exception e){ErrorReport.reportError(e, "Cannot remove clusters " + getDBName());}
    	}
    	public void removeClustAndPair() {
    		try {
    			DBinfo info = theCompilePanel.getDBInfo();
    			if (info==null) {
    				Out.PrtWarn("Database does not exist");
    				return;
    			}
    			boolean ret = UserPrompt.showConfirm("Remove...", "Remove clusters and pairs from database");
    			if (!ret) return;
    			
    			Out.PrtSpMsg(0,"Remove clusters and pairs from database ");
    			DBConn mDB = runMTCWMain.hosts.getDBConn(getDBName());
    			
    			// Clusters
    			Out.PrtSpMsg(1, "Remove clusters....");
    			removeClust(mDB, info.getMethodPrefix(), false /* do not delete method from pairs*/);
    			
    			// Pairs
    			Out.PrtSpMsg(1, "Remove pairs....");
    			mDB.tableDelete("pairwise"); 
    			mDB.tableDelete("pairMap");
    			mDB.executeUpdate("update unitrans set nPairs=0");  
    			     
    			mDB.executeUpdate("update info set kaksInfo='', pairInfo='', aaInfo='', ntInfo='', "
    					+ "hasMA=0, hasPCC=0"); // CAS310 add these two
    			info.clearCntKeys(); // CAS340
    			
    			Out.PrtSpMsg(0, "Finish clusters and pairs removal");
    			theCompilePanel.updateAll(); 
    		}
    		catch(Exception e) {ErrorReport.prtReport(e, "Error removing pairs and methods from database");}
    	}
    	// Cluster removal is also in MethodPanel.removeFromDB
    	private void removeClust(DBConn mDB, String [] prefixes, boolean bPairCol) {
    		try {
    			mDB.tableDelete("pog_method"); // CAS342 use tableDelete
    			mDB.tableDelete("pog_groups");
    			mDB.tableDelete("pog_members");
    			mDB.tableDelete("pog_scores");		
    			
    			int nSeq = mDB.executeCount("select count(*) from unitrans");
    			Out.PrtSpMsg(1, "Remove method columns from " + nSeq + " sequence table rows...");
    			for (String methodPrefix: prefixes) { 
    				mDB.tableCheckDropColumn("unitrans", methodPrefix);
    			}
    			mDB.executeUpdate("update info set hasMA=0, allMethods='', allTaxa=''");// CAS330 hasMA; CAS342 allMethods..
    			new Summary(mDB).removeSummary(); 
    			
    			if (bPairCol) {
	    			int nPair = mDB.executeCount("select count(*) from pairwise");
	    			Out.PrtSpMsg(1, "Remove method columns from " + nPair + " pair table rows...");
	    			
	    			for (String methodPrefix: prefixes) { // truncate pairwise before remove prefix
	    				mDB.tableCheckDropColumn("pairwise", methodPrefix);
	    			}
    			}
    			
    		}
    		catch(Exception e) {ErrorReport.prtReport(e, "Error removing pairs and methods from database");}
    	}
    	private void removePairDir() {
    		try {
    			boolean ret = UserPrompt.showConfirm("Remove...", 
    					"Remove project directories Methods, Stats and KaKs");
    			if (!ret) return;
    			
    			Out.PrtSpMsg(0,"Remove pairs and clusters from disk");
    			String pdir = theCompilePanel.getCurProjAbsDir() +  "/";
    			
    			Out.PrtSpMsg(1,"Remove directory " + pdir + Globals.Methods.METHODDIR);
    			FileHelpers.deleteDir(new File(pdir + Globals.Methods.METHODDIR ));
    			
    			Out.PrtSpMsg(1,"Remove directory " + pdir + Globals.StatsDIR );
    			FileHelpers.deleteDir(new File(pdir + Globals.StatsDIR ));
    			
    			Out.PrtSpMsg(1,"Remove directory " + pdir + Globals.KaKsDIR);
    			FileHelpers.deleteDir(new File(pdir + Globals.KaKsDIR));
    			
    			Out.PrtSpMsg(0,"Finish removal from disk");
    			theCompilePanel.updateAll(); 
    		}
    		catch(Exception e) {ErrorReport.prtReport(e, "Error removing directories");}
    	}
    	private void removeHitDir() {
    		try {
    			boolean ret = UserPrompt.showConfirm("Remove...", 
    					"Remove hit files\n");
    			if (!ret) return;
    			
    			String blastDir = theCompilePanel.getCurProjAbsDir() +  "/" + Globals.Search.BLASTDIR; 
    			Out.PrtSpMsg(0, "Remove directory " +  blastDir);		
    			FileHelpers.deleteDir(new File(blastDir));
				theCompilePanel.updateDeleteBlastDir(blastDir);
    		} 
    		catch (Exception e){ErrorReport.reportError(e, "Cannot delete hit directory ");}
    	}
    	private void removeProject() {
    		try {
    			boolean ret = UserPrompt.showConfirm("Remove...", 
    					"Remove mTCW project '" + getDBName() + "' from disk");
    			if (!ret) return;
    			
    			File fLib = new File(theCompilePanel.getCurProjAbsDir());
    			
    			Out.PrtSpMsg(0, "Delete directory " +  theCompilePanel.getCurProjRelDir());
    			FileHelpers.deleteDirTrace(fLib);
    			Out.PrtSpMsg(0, "Complete delete");
    			updateProjectList(null);
    			theCompilePanel.updateClearInterface(); 
    			updateUI();
    		}
    		catch(Exception e) {ErrorReport.prtReport(e, "Error removing project");}
    	}
    	
    	public boolean isCancel() {
    		if (nMode==CANCEL) return true; 
    		else return false;
    	}
    	public void doOp() {
    		if (btnDB.isSelected()) {
    			removeDB();
    			btnClustAndPair.setSelected(false);
    		}
    		if (btnAllDir.isSelected()) {
    			removeProject();
    			btnPairDir.setEnabled(false);
    			btnHitDir.setEnabled(false);
    		}
    		if (btnClust.isSelected()) {
    			removeClust();
    		}
    		if (btnClustAndPair.isSelected()) {
    			removeClustAndPair();
    		}
    		if (btnPairDir.isSelected()) {
    			removePairDir();
    		}
    		if (btnHitDir.isSelected()) {
    			removeHitDir();
    		}		
    	}
    	JCheckBox btnClust = null, btnClustAndPair = null, btnPairDir = null;
    	JCheckBox btnDB = null, btnHitDir = null, btnAllDir = null;
    	JButton btnOK = null, btnCancel = null;
    	int nMode = -1;
    } // end RemoveType
	    
	private CompilePanel theCompilePanel = null;
	private JPanel mainPanel = null;
	private ButtonComboBox cmbProject = null;
	private JButton btnSave = null, btnAdd = null, btnHelp = null, btnRemove = null, btnOverview = null;
	private JTextField txtDBName = null;
}
