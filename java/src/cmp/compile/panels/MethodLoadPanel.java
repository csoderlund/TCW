package cmp.compile.panels;
/***************************************************
 * User defined file to load
 */
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cmp.database.Globals;
import util.methods.Static;
import util.file.FileC;
import util.file.FileRead;

public class MethodLoadPanel extends JPanel {
	private static final long serialVersionUID = 2272315161653860174L;
	private final static String xDELIM = Globals.Methods.outDELIM;
	private final static String iDELIM = Globals.Methods.inDELIM;

	public MethodLoadPanel(CompilePanel parentPanel) {
		theParentPanel = parentPanel;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Static.BGCOLOR);
		int width = 70;
		add(Box.createVerticalStrut(20));
	
		// prefix
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
				
		// file
		row = Static.createRowPanel();
		lblFile = Static.createLabel("File");
		row.add(lblFile);
		row.add(Box.createHorizontalStrut(width - lblFile.getPreferredSize().width));
		
		txtFile = Static.createTextField("", 25);
		row.add(txtFile);		
		row.add(Box.createHorizontalStrut(5));
		
		btnFile = Static.createButtonFile("...", true);
		btnFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				String projName = theParentPanel.getProjectName();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnFile, "User Defined File", FileC.dCMP, FileC.fANY)) { 
					txtFile.setText(fc.getRemoveFixedPath());
				}
			}
		});
		row.add(btnFile);	
		add(row);
		
		add(Box.createVerticalStrut(10));
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	
	public String getSettings() {
		return   xDELIM + iDELIM + txtFile.getText() + iDELIM + xDELIM;
	}
	
	public void setSettings(String settings) {
		String [] vals = settings.split(":");
		if (vals.length>=2) txtFile.setText(vals[1]);
	}
	
	public void resetSettings() {
		txtPrefix.setText(Globals.Methods.UserDef.DEFAULT_PREFIX);
		txtFile.setText("");
	}
	
	public String getMethodName() { 
		return Globals.getName(Globals.Methods.UserDef.TYPE_NAME, txtPrefix.getText()); 
	}
	
	public String getPrefix() { return txtPrefix.getText(); }
	public void setPrefix(String prefix) { txtPrefix.setText(prefix); }
	
	public static String getMethodType() { return Globals.Methods.UserDef.TYPE_NAME; }
	
	public String getComment() { 
		return txtFile.getText();
	}
	public boolean hasValidFile() {
		String f = txtFile.getText().trim();
		if (f.contentEquals("")) return false;
		return true;
	}
	private CompilePanel theParentPanel = null;
	private JLabel lblPrefix = null;
	private JTextField txtPrefix = null;
	
	private JLabel lblFile = null;
	private JTextField txtFile = null;
	private JButton btnFile = null;

	public void setLoaded(boolean bLoaded) {
		lblPrefix.setEnabled(!bLoaded);
		txtPrefix.setEnabled(!bLoaded);
		lblFile.setEnabled(!bLoaded);
		txtFile.setEnabled(!bLoaded);
	}
}
