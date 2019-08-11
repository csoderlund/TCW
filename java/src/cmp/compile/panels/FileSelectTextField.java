package cmp.compile.panels;

/************************************************
 * Used by all methods that need to select a file
 */
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretListener;

import cmp.database.Globals;

public class FileSelectTextField extends JPanel {
	private static final long serialVersionUID = 2799665158668961271L;
	private static final int TEXTFIELD_WIDTH = 20;
	
	public static final int BLAST = 1;   // EditBlastPanel to get user supplied blast
	public static final int ORTHO = 2;   // MethodUserDefPanel for predefined clusters
	
	private int pathType=0, lastPathType=0;
	private String lastDir="";
	
	public FileSelectTextField(CompilePanel parentPanel, int type) { 
		theParentPanel = parentPanel;
		pathType = type;
		txtField = new JTextField(TEXTFIELD_WIDTH);
		txtField.setMaximumSize(txtField.getPreferredSize());
		txtField.setMinimumSize(txtField.getPreferredSize());
		
		btnSelectFile = new JButton("...");
		btnSelectFile.setBackground(Globals.BGCOLOR);
		btnSelectFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				String startingDir = getPath();
				fc.setCurrentDirectory(new File(startingDir));
				
				if(fc.showOpenDialog(theParentPanel) == JFileChooser.APPROVE_OPTION) {
					String fullPath = fc.getSelectedFile().getAbsolutePath();
					setDirLast(fullPath, pathType);
					txtField.setText(pathMakeRelative(fullPath));
				}
			}
		});
		btnSelectFile.setPreferredSize(new Dimension(btnSelectFile.getPreferredSize().width, txtField.getPreferredSize().height));
		btnSelectFile.setMaximumSize(btnSelectFile.getPreferredSize());
		btnSelectFile.setMinimumSize(btnSelectFile.getPreferredSize());
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		add(txtField);
		add(btnSelectFile);
		
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}
	
	public void addActionListener(ActionListener l) { btnSelectFile.addActionListener(l); }
	public void addFocusListener(FocusListener l) { 
		txtField.addFocusListener(l);
		btnSelectFile.addFocusListener(l);
	}
	public void addCaretListener(CaretListener l) { txtField.addCaretListener(l); }
	
	public void setText(String text) { 
		txtField.setText(text); 
	}
	public String getText() { return txtField.getText(); }
	
	public void setEnabled(boolean enabled) {
		txtField.setEnabled(enabled);
		btnSelectFile.setEnabled(enabled);
	}
	private String getPath() {
		if (pathType==lastPathType && lastDir!="") return lastDir;
		if (pathType==BLAST) return theParentPanel.pnlBlast.getDefaultBlastDirFullPath();
		if (pathType==ORTHO) return theParentPanel.getCurProjAbsDir();
		return "";
	}
	private String pathMakeRelative(String filePath) {
		if (pathType==BLAST) {
			String blastRelPath = theParentPanel.pnlBlast.getDefaultBlastDirRelPath();
			if (filePath.contains(blastRelPath))
			{
				int index = filePath.indexOf(blastRelPath);
				return filePath.substring(index);
			}
			else return filePath;
		}
		else {
			String cur = System.getProperty("user.dir");
			if(filePath.contains(cur)) filePath = filePath.replace(cur, ".");
			return filePath;
		}
	}
	private void setDirLast(String path, int t) {
		lastDir = path.substring(0, path.lastIndexOf("/"));
		lastPathType = t;
	}
	
	private CompilePanel theParentPanel = null;
	private JTextField txtField = null;
	private JButton btnSelectFile = null;
}
