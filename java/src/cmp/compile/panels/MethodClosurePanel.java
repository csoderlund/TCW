package cmp.compile.panels;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import util.methods.Out;
import util.methods.Static;
import util.ui.ButtonComboBox;

import cmp.database.Globals;

public class MethodClosurePanel extends JPanel {
	private static final long serialVersionUID = 109004697047687473L;	
	private final static String xDELIM = Globals.Methods.outDELIM;
	private final static String iDELIM = Globals.Methods.inDELIM;
	private String [] abbrev = {"AA", "NT"}; // also in MethodClosure.java and BBH 
	private String [] covTypes = {"Either", "Both"};
	
	public MethodClosurePanel(CompilePanel parentPanel) {
		theCompilePanel = parentPanel;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Static.BGCOLOR);
		add(Box.createVerticalStrut(20));
		
		int width = Globals.Methods.WIDTH;
		
		JPanel row = Static.createRowPanel();
		lblPrefix = new JLabel("Prefix");
		row.add(lblPrefix);
		row.add(Box.createHorizontalStrut(width - lblPrefix.getPreferredSize().width));
		txtPrefix = Static.createTextField("", 4);
		row.add(txtPrefix);
		row.add(Box.createHorizontalStrut(5));
		JLabel req = new JLabel(EditMethodPanel.LBLPREFIX);
		row.add(req);
		add(row);
		add(Box.createVerticalStrut(20));
				
		row = Static.createRowPanel();
		lblHitType = new JLabel("Hit File Type");
		row.add(lblHitType);
		row.add(Box.createHorizontalStrut(width - lblHitType.getPreferredSize().width));
		aaButton = Static.createRadioButton("Amino Acid",true); 
		row.add(aaButton); row.add(Box.createHorizontalStrut(5));
		
		ntButton = Static.createRadioButton("Nucleotide",false); 
		row.add(ntButton); row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup();
		group.add(aaButton); group.add(ntButton); 
		add(row);
		add(Box.createVerticalStrut(20));
		
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
		int isNT = (aaButton.isSelected()) ? 0 : 1;
		return  xDELIM 						+ iDELIM + 
				txtCovCutoff.getText() 		+ iDELIM + 
				covLenMode.getSelectedIndex() + iDELIM +
				txtSimCutoff.getText() 		+ iDELIM +
				isNT 						+ iDELIM + 
				xDELIM;
	}
	
	public void setSettings(String settings) {
		String [] theSettings = settings.split(iDELIM);
		if (theSettings.length<3) return; // earlier versions
	
		txtCovCutoff.setText(theSettings[1]);
		try {
			int i = Integer.parseInt(theSettings[2]);
			covLenMode.setSelectedIndex(i);
		} catch (Exception e) {}
		
		txtSimCutoff.setText(theSettings[3]);
		try {
			int i = Integer.parseInt(theSettings[4]);
			if (i==0) aaButton.setSelected(true);
			else ntButton.setSelected(true);
		} catch (Exception e) {}
		
		updateHitType();
	}
	
	public void resetSettings() {
		covLenMode.setSelectedIndex(Globals.Methods.Closure.COV_TOGGLE);
		txtPrefix.setText(Globals.Methods.Closure.DEFAULT_PREFIX);
		txtCovCutoff.setText(Globals.Methods.Closure.COVERAGE_CUTOFF);
		txtSimCutoff.setText(Globals.Methods.Closure.SIMILARITY);
		updateHitType();
	}
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.Closure.TYPE_NAME, txtPrefix.getText()); 
	}
	public static String getMethodType() { return Globals.Methods.Closure.TYPE_NAME; }
	public String getSearchType() {if (aaButton.isSelected()) return abbrev[0]; else return abbrev[1];}
	
	public String getPrefix() { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public String getComment() { 
		int forx = covLenMode.getSelectedIndex();
		int sim = Static.getInteger(txtSimCutoff.getText());
		int cov = Static.getInteger(txtCovCutoff.getText());
		
		String com = "Sim " + sim + "; Cov " + cov; 
		com += "(" + covTypes[forx] +")";
		
		com += "; " + getSearchType(); // CAS330 always put AA or NT
		
		return com;
	}
	
	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblCovCutoff.setEnabled(!bLoaded);
		txtCovCutoff.setEnabled(!bLoaded);
		lblSimCutoff.setEnabled(!bLoaded);
		txtSimCutoff.setEnabled(!bLoaded);
		updateHitType();
	}
	private void updateHitType() {
		boolean hasNT=(theCompilePanel.getNumNTdb()>1);
		aaButton.setEnabled(hasNT);
		ntButton.setEnabled(hasNT);
		lblHitType.setEnabled(hasNT);
	}
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private JLabel lblHitType = null;
	private JRadioButton aaButton, ntButton;
	
	private ButtonComboBox covLenMode = null;
	private JLabel lblCovCutoff = null;
	private JTextField txtCovCutoff = null;
	
	private JLabel lblSimCutoff = null;
	private JTextField txtSimCutoff = null;
	
	private CompilePanel theCompilePanel = null;
}
