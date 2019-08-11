package sng.util;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import sng.viewer.STCWFrame;
import util.methods.Out;
import util.ui.UIHelpers;
import util.ui.UserPrompt;


public class ExportFile {
// uses lastSaveFilePath
	
	// this will not work from inside a thread
	static public File getFileHandle(String fileName, STCWFrame theMainFrame) {
		final JFileChooser fc = new JFileChooser(theMainFrame.lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
		
		if (fc.showSaveDialog(theMainFrame) != JFileChooser.APPROVE_OPTION) return null;
		if (fc.getSelectedFile() == null) return null;
		
		final File f = fc.getSelectedFile();
		final File d = f.getParentFile();
		if (!d.canWrite()) { 
			JOptionPane.showMessageDialog(null, 
					"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
		}
		else {
			int writeOption = JOptionPane.YES_OPTION;
			if (f.exists()) {
				writeOption = JOptionPane.showConfirmDialog(theMainFrame,
					    " The file '" + f.getName() + "' already exists. \nOverwrite it?", "Save to File",
					    JOptionPane.YES_NO_OPTION);
			}
			if (writeOption == JOptionPane.YES_OPTION) {
				theMainFrame.setLastPath(f.getPath());
				return f;
			}
		}
		return null;
	}
	// for append or merge
	static public File getFileHandle(String msg, String fileName, STCWFrame theMainFrame) {
		final JFileChooser fc = new JFileChooser(theMainFrame.lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
		
		if (fc.showSaveDialog(theMainFrame) != JFileChooser.APPROVE_OPTION) {
			Out.prt("Cancel Export");
			return null;
		}
		if (fc.getSelectedFile() == null) {
			Out.prt("No file selected.  Default file: " + fileName + " Default path:" + theMainFrame.lastSaveFilePath);
			return null;
		}
		
		final File f = fc.getSelectedFile();
		final File d = f.getParentFile();
		if (!d.canWrite()) { 
			JOptionPane.showMessageDialog(null, 
					"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
			return null;
		}
		
		if (f.exists()) {
			Object[] options = {"Cancel", "Overwrite", msg};
			int n = JOptionPane.showOptionDialog(theMainFrame,
			    "File exists: " + f.getName(), "File exists",
			    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
			    null, options, options[2]);
			if (n==0) return null; // cancel
			
			bAppend = (n==2) ? true : false;
			
			theMainFrame.setLastPath(f.getPath());
			
			boolean isApplet = UIHelpers.isApplet();
			if(isApplet) {
			    UserPrompt.showMsg("Exporting a file across the network is very slow.\n" +
			    		"A message box will popup when done.");
			}
		} else bAppend = false;
		
		return f;	
	}
	static boolean bAppend=false;
	static public boolean isAppend() {return bAppend;}
}
