package cmp.compile.panels;

import java.util.HashSet;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;

import cmp.database.DBinfo;
import cmp.database.Globals;
import util.methods.Static;
import util.ui.ButtonComboBox;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class MethodBBHPanel extends JPanel {
	private static final long serialVersionUID = 109004697047687473L;	
	private final static String xDELIM = Globals.Methods.METHODS_DELIM;
	
	private String [] abbrev = {"AA", "NT"}; // also in MethodBBH.java
	private String [] covTypes = {"Either", "Both"}; 
	
	public MethodBBHPanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		
		int width = Globals.CompilePanel.WIDTH;
		add(Box.createVerticalStrut(20));
		add(new JLabel("Bi-directional Best Hit"));
		add(Box.createVerticalStrut(10));
		
		JPanel row = Static.createRowPanel();
		JLabel type = new JLabel("Hit File Type");
		row.add(type);
		row.add(Box.createHorizontalStrut(width - type.getPreferredSize().width));
		String labels [] = {"Amino Acid","Nucleotide"}; 
		seqMode = new ButtonComboBox();
		seqMode.addItems(labels);
		
		boolean doNT = true;
		if (theCompilePanel.dbIsExist() && theCompilePanel.getNumNTdb()<=1) doNT=false;
		if (doNT) {
			row.add(seqMode);
			add(row);
			add(Box.createVerticalStrut(10));
		}
		
		// prefix
		row = Static.createRowPanel();
		lblPrefix = new JLabel("Prefix");
		row.add(lblPrefix);
		row.add(Box.createHorizontalStrut(width - lblPrefix.getPreferredSize().width));
		
		txtPrefix = Static.createTextField("", 3);
		row.add(txtPrefix);
		row.add(Box.createHorizontalStrut(5));
		
		row.add(new JLabel(EditMethodPanel.LBLPREFIX));
		add(row);
		add(Box.createVerticalStrut(20));
		
		// cutoff
		row = Static.createRowPanel();
		lblCovCutoff = new JLabel("%Coverage");
		row.add(lblCovCutoff);
		row.add(Box.createHorizontalStrut(width - lblCovCutoff.getPreferredSize().width));
		
		txtCovCutoff = Static.createTextField(Globals.Methods.BestRecip.COVERAGE_CUTOFF, 4);
		row.add(txtCovCutoff);	
		
		row.add(Box.createHorizontalStrut(3));
		row.add(new JLabel("for "));
		row.add(Box.createHorizontalStrut(1));
		covLenMode = new ButtonComboBox();
		covLenMode.addItems(covTypes);
		
		row.add(covLenMode);
		add(row);
		add(Box.createVerticalStrut(10));
		
		// similarity
		row = Static.createRowPanel();
		lblSimCutoff = new JLabel("%Similarity");
		row.add(lblSimCutoff);
		row.add(Box.createHorizontalStrut(width - lblSimCutoff.getPreferredSize().width));
		
		txtSimCutoff = Static.createTextField(Globals.Methods.BestRecip.SIMILARITY, 4);
		row.add(txtSimCutoff);	
		add(row);
		add(Box.createVerticalStrut(10));
		
		row = Static.createRowPanel();
		btnSelect = Static.createButton("Select sTCWdbs", false);
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showSelect();
			}
		});
		row.add(btnSelect);
		row.add(Box.createHorizontalStrut(3));
		
		lblSelect = new JLabel("");
		row.add(lblSelect);
		updateSTCWbutton();
		
		add(row);
		add(Box.createVerticalStrut(10));
		
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	public boolean checkSet() {
		if (nSets==2) return true;
		if (selectSTCW.size()>=2) return true;
		return false;
	}
	private void showSelect() {
		final SelectSTCW select = new SelectSTCW();
		select.setVisible(true);	
		select.doOp();
		updateList();
	}
	// Called by setSettings
	public void updateSTCWbutton() {
		boolean bDB = theCompilePanel.dbIsExist();
		if (!bDB) {
			btnSelect.setEnabled(false);
			lblSelect.setText("Database does not exist");
			return;
		}
		DBinfo info = theCompilePanel.getDBInfo();
		if (info==null) {
			btnSelect.setEnabled(false);
			lblSelect.setText("No database information"); // shouldn't happen
			return;
		}
		nSets = info.getASM().length;
		if (nSets==2) {
			btnSelect.setEnabled(false);
			lblSelect.setText("No selection needed");
			return;
		}
		btnSelect.setEnabled(true);
		updateList();
		return;
	}
	
	private void updateList() {
		listSTCW = "";
		if (selectSTCW.size()==0) {
			lblSelect.setText("sTCWdb: select 2 or more");
			return;
		}
		for (String x : selectSTCW) {
			if (listSTCW=="") listSTCW=x;
			else listSTCW += "," + x;
		}
		lblSelect.setText("sTCWdb: " + listSTCW);
	}
	public void setLoaded(boolean loaded) { // true - method already in database
		lblPrefix.setEnabled(!loaded);
		txtPrefix.setEnabled(!loaded);
		lblCovCutoff.setEnabled(!loaded);
		txtCovCutoff.setEnabled(!loaded);
		lblSimCutoff.setEnabled(!loaded);
		txtSimCutoff.setEnabled(!loaded);
		btnSelect.setEnabled(!loaded);
		if (nSets==2) btnSelect.setEnabled(false);
	}
	// Order here is important. Written to mTCW.cfg - sync with setSettings
	// x:olap:lenMode:sim:seqMode:stcwList:x
	// Called to write to mTCW and to use by MethodBBH
	public String getSettings() {
		return  
			xDELIM + ":" + 
				txtCovCutoff.getText() 		+ ":" + 
				covLenMode.getSelectedIndex() 	+ ":" +
				txtSimCutoff.getText() 			+ ":" +
				seqMode.getSelectedIndex() 		+ ":" +
				listSTCW							+ ":" + 
			 xDELIM;
	}
	// Old: x:olap:lenMode:sim:seqMode:x
	// New: x:olap:lenMode:sim:seqMode:stcwList:x  created in getSettings
	// Called from Method table to set parameters
	public void setSettings(String settings) {
		String [] theSettings = settings.split(":");
		if (theSettings.length<3) return; // earlier versions
		
		txtCovCutoff.setText(theSettings[1]);
		try {
			int i = Integer.parseInt(theSettings[2]);
			covLenMode.setSelectedIndex(i);
		} catch (Exception e) {}
		
		txtSimCutoff.setText(theSettings[3]);
		try {
			int i = Integer.parseInt(theSettings[4]);
			seqMode.setSelectedIndex(i);
		} catch (Exception e) {}
		
		selectSTCW.clear();
		if (!theSettings[5].equals(xDELIM)) {
			String [] list = theSettings[5].split(",");
			for (int i=0; i<list.length; i++)
				selectSTCW.add(list[i]);
		}
		updateSTCWbutton();
	}
	// Called on Add
	public void resetSettings() {
		setLoaded(false); // enable all buttons
		covLenMode.setSelectedIndex(Globals.Methods.BestRecip.COV_TOGGLE);
		txtPrefix.setText(Globals.Methods.BestRecip.DEFAULT_PREFIX);
		txtCovCutoff.setText(Globals.Methods.BestRecip.COVERAGE_CUTOFF);
		txtSimCutoff.setText(Globals.Methods.BestRecip.SIMILARITY);
		
		listSTCW = "";
		selectSTCW.clear();
		updateSTCWbutton();
	}
	public String getSearchType() {return abbrev[seqMode.getSelectedIndex()];}
	public static String getMethodType() { return Globals.Methods.BestRecip.TYPE_NAME; }
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.BestRecip.TYPE_NAME, txtPrefix.getText()); 
	}
	public String 	getPrefix() { return txtPrefix.getText(); }
	public String getComment() {  
		String com = "Sim " + txtSimCutoff.getText() + "; Cov " + txtCovCutoff.getText(); 
		
		int forx = covLenMode.getSelectedIndex();
		int sim = Static.getInteger(txtSimCutoff.getText());
		int cov = Static.getInteger(txtCovCutoff.getText());
		if (sim!=0 || cov!=0) com += "(" + covTypes[forx] + ")"; 
		
		String ab = abbrev[seqMode.getSelectedIndex()] ;
		if (ab.equals("NT")) com += ";" + ab;
		
		if (selectSTCW.size()>0 && !listSTCW.equals("")) com += "; " + listSTCW;
		else if (nSets>2) com += "; All pairs of sTCWs";
		return com;
	}
	
	public void 	setPrefix(String prefix) { txtPrefix.setText(prefix); }

	private class SelectSTCW extends JDialog {
		private static final long serialVersionUID = 1L;
		public static final int OK = 1;
    		public static final int CANCEL = 2;
    		
	 	public SelectSTCW() {
	    		setModal(true);
	    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	    		setTitle("Select.... ");
			
	    		DBinfo info = theCompilePanel.getDBInfo();
	    		setNames = info.getASM();
	    		
	    		JPanel selectPanel = Static.createPagePanel();
	    		
	    		selectPanel.add(new JLabel("Select sTCWdb:"));
	    		asmCheckBox = new JCheckBox [nSets];
	    		for (int i=0; i<nSets; i++) {
	    			boolean b = (selectSTCW.contains(setNames[i])) ? true: false;
	    			asmCheckBox[i] = Static.createCheckBox(setNames[i], b);
	    			selectPanel.add(asmCheckBox[i]); 
	    			selectPanel.add(Box.createVerticalStrut(3));
	    		}
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
	 	public void doOp() {
	 		if (nMode==CANCEL) return;
	 		
	 		selectSTCW.clear();
	 		for (int i=0; i<nSets; i++) 
				if (asmCheckBox[i].isSelected()) 
					selectSTCW.add(setNames[i]);
	 		if (selectSTCW.size()==1) {
	 			UserPrompt.showWarn("Only selected one database. Must select 0 or >1.");
	 			selectSTCW.clear();
	 		}
	 	}
	 	
	 	JButton btnOK, btnCancel;
	 	int nMode = -1;
	 	
		private String [] setNames = null;
		private JCheckBox [] asmCheckBox = null;
	}

	private ButtonComboBox seqMode=null;
	private JLabel     lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private ButtonComboBox covLenMode = null;
	private JLabel     lblCovCutoff = null, lblSimCutoff = null;
	private JTextField txtCovCutoff = null, txtSimCutoff = null;

	private JButton btnSelect = null;
	private JLabel lblSelect = null;
	
	private String listSTCW = "";
	private int nSets=2;
	private HashSet <String> selectSTCW = new HashSet <String> ();
	private CompilePanel theCompilePanel = null;
}
