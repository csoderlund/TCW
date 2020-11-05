package cmp.compile.panels;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import cmp.database.Globals;
import util.methods.Static;

public class MethodHitPanel  extends JPanel {
	private static final long serialVersionUID = 1L;
	private final static String xDELIM = Globals.Methods.METHODS_DELIM;
	private String [] hitTypes = {"HitID", "Description"}; 
	
	public MethodHitPanel(CompilePanel parentPanel) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		add(Box.createVerticalStrut(20));

		int width = Globals.CompilePanel.WIDTH;
		
		JPanel row = Static.createRowPanel();	
		lblPrefix = new JLabel("Prefix");
		row.add(lblPrefix);
		row.add(Box.createHorizontalStrut(width - lblPrefix.getPreferredSize().width));
		
		txtPrefix = Static.createTextField("", 3);
		row.add(txtPrefix);
		row.add(Box.createHorizontalStrut(5));
		
		JLabel req = new JLabel(EditMethodPanel.LBLPREFIX);
		row.add(req);
		add(row);
		add(Box.createVerticalStrut(20));
		
		row = Static.createRowPanel();
		JLabel type = new JLabel("Cluster on ");
		row.add(type);
		row.add(Box.createHorizontalStrut(width - type.getPreferredSize().width));
		
		idButton = Static.createRadioButton(hitTypes[0],false); 
		row.add(idButton); row.add(Box.createHorizontalStrut(5));
		
		descButton = Static.createRadioButton(hitTypes[1],true); 
		row.add(descButton); row.add(Box.createHorizontalStrut(5));
		
		ButtonGroup group = new ButtonGroup();
		group.add(idButton); group.add(descButton); 
		
		add(row);  add(Box.createVerticalStrut(25));
		
		// cutoff
		row = Static.createRowPanel();
		lblCovCutoff = new JLabel("%Coverage");
		row.add(lblCovCutoff);
		row.add(Box.createHorizontalStrut(width - lblCovCutoff.getPreferredSize().width));

		txtCovCutoff = Static.createTextField(Globals.Methods.Hit.COVERAGE_CUTOFF, 4);
		row.add(txtCovCutoff);	
		row.add(Box.createHorizontalStrut(3));
		add(row);
		add(Box.createVerticalStrut(10));
		
		// similarity
		row = Static.createRowPanel();
		lblSimCutoff = new JLabel("%Similarity");
		row.add(lblSimCutoff);
		row.add(Box.createHorizontalStrut(width - lblSimCutoff.getPreferredSize().width));
		
		txtSimCutoff =  Static.createTextField(Globals.Methods.Hit.SIMILARITY, 4);
		row.add(txtSimCutoff);	
		add(row);
		add(Box.createVerticalStrut(10));
		
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblCovCutoff.setEnabled(!bLoaded);
		txtCovCutoff.setEnabled(!bLoaded);
		lblSimCutoff.setEnabled(!bLoaded);
		txtSimCutoff.setEnabled(!bLoaded);
	}
	// Called on Add
	public void resetSettings() {
		setLoaded(false); // enable all buttons
		descButton.setSelected(true);
		txtPrefix.setText(Globals.Methods.Hit.DEFAULT_PREFIX);
	
		txtCovCutoff.setText(Globals.Methods.Hit.COVERAGE_CUTOFF);
		txtSimCutoff.setText(Globals.Methods.Hit.SIMILARITY);
	}
	
	public static String getMethodType() { 
		return Globals.Methods.Hit.TYPE_NAME; 
	}
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.Hit.TYPE_NAME, txtPrefix.getText()); 
	}
	
	public String getPrefix() 		 { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public String getComment() {  
		int x = (descButton.isSelected()) ? 1 : 0;
		String com = "Sim " + txtSimCutoff.getText() + "; Cov " + txtCovCutoff.getText(); 
		return  com + "; " + hitTypes[x];
	}
	public String getSettings() {
		int x = (descButton.isSelected()) ? 1 : 0;
		return  xDELIM + ":" + x + ":" + txtCovCutoff.getText() + ":" + txtSimCutoff.getText() + ":" + xDELIM ;
	}
		
	public void setSettings(String settings) {
		String [] vals = settings.split(":");
		if (vals.length<=3) return;
		
		if (vals[1].contentEquals("0")) idButton.setSelected(true);
		else descButton.setSelected(true);
		
		txtCovCutoff.setText(vals[2]);
		txtSimCutoff.setText(vals[3]);
	}
	
	private JRadioButton idButton, descButton;
	private JLabel     lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private JLabel lblCovCutoff = null;
	private JTextField txtCovCutoff = null;
	
	private JLabel lblSimCutoff = null;
	private JTextField txtSimCutoff = null;
}
