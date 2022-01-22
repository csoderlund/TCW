package cmp.compile.panels;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cmp.database.Globals;
import util.methods.Static;

public class MethodOrthoMCLPanel extends JPanel {
	private static final long serialVersionUID = -721309318079790889L;
	private final static String xDELIM = Globals.Methods.outDELIM;
	private final static String iDELIM = Globals.Methods.inDELIM;

	public MethodOrthoMCLPanel(CompilePanel parentPanel) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Static.BGCOLOR);
		int width = Globals.Methods.WIDTH;
		add(Box.createVerticalStrut(20));

		// Set defaults in resetSettings()
		
		// Prefix
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
		
		
		// parameters
		row =  Static.createRowPanel();
		lblInflation = new JLabel("Inflation");
		row.add(lblInflation);
		row.add(Box.createHorizontalStrut(width - lblInflation.getPreferredSize().width));
		
		txtInflation = Static.createTextField(Globals.Methods.OrthoMCL.INFLATION, 4);	
		row.add(txtInflation);
		add(row);
		add(Box.createVerticalStrut(30));
	
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
		
	public String getSettings() {
		return  xDELIM + iDELIM + txtInflation.getText() + iDELIM + xDELIM;
	}
		
	public void setSettings(String settings) {
		String [] vals = settings.split(":");
		if (vals.length>=2) txtInflation.setText(vals[1]);
	}
	
	public void resetSettings() {
		txtPrefix.setText(Globals.Methods.OrthoMCL.DEFAULT_PREFIX);
		txtInflation.setText(Globals.Methods.OrthoMCL.INFLATION);
	}
	
	public String getMethodName() { 
		return Globals.makeName(Globals.Methods.OrthoMCL.TYPE_NAME, txtPrefix.getText()); 
	}
	
	public String getPrefix() { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public static String getMethodType() { return Globals.Methods.OrthoMCL.TYPE_NAME; }
	
	public String getComment() { 
		return "Inflation " + txtInflation.getText();
	}
		
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private JLabel lblInflation = null;
	private JTextField txtInflation = null;

	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblInflation.setEnabled(!bLoaded);
		txtInflation.setEnabled(!bLoaded);
	}
}
