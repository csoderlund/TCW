package cmp.compile.panels;
/***************************************************
 * User defined file to load
 */
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.methods.Static;

import cmp.database.Globals;

public class MethodLoadPanel extends JPanel {
	private static final long serialVersionUID = 2272315161653860174L;

	public MethodLoadPanel(CompilePanel parentPanel) {
		theParentPanel = parentPanel;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globals.BGCOLOR);
		int width = Globals.CompilePanel.WIDTH;
		add(Box.createVerticalStrut(20));
	
		// prefix
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
		add(Box.createVerticalStrut(10));
				
		// file
		row = Static.createRowPanel();
		lblFile = new JLabel("File");
		row.add(lblFile);
		row.add(Box.createHorizontalStrut(width - lblFile.getPreferredSize().width));
		
		txtFile = new FileSelectTextField(theParentPanel, FileSelectTextField.ORTHO);
		row.add(txtFile);
		add(row);
		
		// remark
		row = Static.createRowPanel();
		lblRemark = new JLabel("Parameters");
		row.add(lblRemark);
		row.add(Box.createHorizontalStrut(width - lblRemark.getPreferredSize().width));
		
		txtRemark = Static.createTextField("", 20);
		row.add(txtRemark);
		row.add(Box.createHorizontalStrut(5));
		
		add(row);
		add(Box.createVerticalStrut(10));
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	
	public String getSettings() {
		return  "x:" + txtFile.getText() + ":x";
	}
	
	public void setSettings(String settings) {
		String [] vals = settings.split(":");
		txtFile.setText(vals[1]);
	}
	
	public void resetSettings() {
		txtPrefix.setText(Globals.Methods.UserDef.DEFAULT_PREFIX);
		txtFile.setText("");
		txtRemark.setText("");
	}
	
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.UserDef.TYPE_NAME, txtPrefix.getText()); 
	}
	
	public String getPrefix() { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public static String getMethodType() { return Globals.Methods.UserDef.TYPE_NAME; }
	
	public String getComment() { 
		String x = txtRemark.getText();
		if (x.equals("")) x = "File " + txtFile.getText();
		return x; 
		
	}
	public void setComment(String comment) { txtRemark.setText(comment); }
	
	private CompilePanel theParentPanel = null;
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	private JLabel lblRemark = null;
	private JTextField txtRemark = null;
	private JLabel lblFile = null;
	private FileSelectTextField txtFile = null;

	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblFile.setEnabled(!bLoaded);
		txtFile.setEnabled(!bLoaded);
	}
}
