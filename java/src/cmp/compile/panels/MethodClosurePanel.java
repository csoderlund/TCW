package cmp.compile.panels;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.methods.Static;
import util.ui.ButtonComboBox;

import cmp.database.Globals;

public class MethodClosurePanel extends JPanel {
	private static final long serialVersionUID = 109004697047687473L;	
	private String [] abbrev = {"AA", "NT"}; // also in MethodTransitive.java
	private String [] covTypes = {"Either", "Both"};
	
	public MethodClosurePanel(CompilePanel parentPanel) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		int width = Globals.CompilePanel.WIDTH;
		add(Box.createVerticalStrut(10));
		
		JPanel row = Static.createRowPanel();
		JLabel type = new JLabel("Hit File Type");
		row.add(type);
		row.add(Box.createHorizontalStrut(width - type.getPreferredSize().width));
		seqMode = new ButtonComboBox();
		seqMode.addItem("Amino Acid");
		seqMode.addItem("Nucleotide");
		seqMode.finish();
		
		boolean doNT = true;
		if (parentPanel.dbIsExist() && parentPanel.getNumNTdb()<=0) doNT=false;
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
		
		JLabel req = new JLabel(EditMethodPanel.LBLPREFIX);
		row.add(req);
		add(row);
		add(Box.createVerticalStrut(10));
					
		// cutoff
		row = Static.createRowPanel();
		lblCovCutoff = new JLabel("%Coverage");
		row.add(lblCovCutoff);
		row.add(Box.createHorizontalStrut(width - lblCovCutoff.getPreferredSize().width));
		
		txtCovCutoff = Static.createTextField(Globals.Methods.Closure.COVERAGE_CUTOFF, 4);
		row.add(txtCovCutoff);	
		row.add(Box.createHorizontalStrut(3));
		row.add(new JLabel("For: "));
		row.add(Box.createHorizontalStrut(1));
		covLenMode = new ButtonComboBox();
		covLenMode.addItems(covTypes);
		row.add(covLenMode);
		add(row);
		add(Box.createVerticalStrut(10));
		
		add(row);
		add(Box.createVerticalStrut(10));
		
		// similarity
		row = Static.createRowPanel();
		lblSimCutoff = new JLabel("%Similarity");
		row.add(lblSimCutoff);
		row.add(Box.createHorizontalStrut(width - lblSimCutoff.getPreferredSize().width));
		
		txtSimCutoff =  Static.createTextField(Globals.Methods.Closure.SIMILARITY, 4);
		row.add(txtSimCutoff);	
		add(row);
		add(Box.createVerticalStrut(10));
				
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	
	public String getSettings() {
		return  "x:" + 
				txtCovCutoff.getText() + ":" + covLenMode.getSelectedIndex() + ":" +
				txtSimCutoff.getText() +":"
				+ seqMode.getSelectedIndex() + ":x";
	}
	
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
	}
	
	public void resetSettings() {
		covLenMode.setSelectedIndex(Globals.Methods.Closure.COV_TOGGLE);
		txtPrefix.setText(Globals.Methods.Closure.DEFAULT_PREFIX);
		txtCovCutoff.setText(Globals.Methods.Closure.COVERAGE_CUTOFF);
		txtSimCutoff.setText(Globals.Methods.Closure.SIMILARITY);
	}
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.Closure.TYPE_NAME, txtPrefix.getText()); 
	}
	public static String getMethodType() { return Globals.Methods.Closure.TYPE_NAME; }
	public String getSearchType() {return abbrev[seqMode.getSelectedIndex()];}
	
	public String getPrefix() { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public String getComment() { 
		String com = "Sim " + txtSimCutoff.getText() + "; Cov " + txtCovCutoff.getText(); 
		
		int forx = covLenMode.getSelectedIndex();
		int sim = Static.getInteger(txtSimCutoff.getText());
		int cov = Static.getInteger(txtCovCutoff.getText());
		if (sim!=0 || cov!=0) com += "(" + covTypes[forx] +")";
		
		String ab = abbrev[seqMode.getSelectedIndex()] ;
		if (ab.equals("NT")) com += ";" + ab;
		return com;
	}
	
	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblCovCutoff.setEnabled(!bLoaded);
		txtCovCutoff.setEnabled(!bLoaded);
		lblSimCutoff.setEnabled(!bLoaded);
		txtSimCutoff.setEnabled(!bLoaded);
	}
	private ButtonComboBox seqMode = null;
	
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private ButtonComboBox covLenMode = null;
	private JLabel lblCovCutoff = null;
	private JTextField txtCovCutoff = null;
	
	private JLabel lblSimCutoff = null;
	private JTextField txtSimCutoff = null;
}
