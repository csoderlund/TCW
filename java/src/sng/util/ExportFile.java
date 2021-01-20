package sng.util;

/********************************************************
 * Export for all Single stuff
 * (mTCW uses cmp.viewer.table.TableUtil)
 * CAS314 consolidated all Export file choosers
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import sng.viewer.STCWFrame;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class ExportFile {
	
	/*******************************************************
	 * Returns file handle for GOtree
	 * there was a comment here saying it would not run within thread, but it is called from thread....
	 */
	static public File getFileHandle(String fileName, STCWFrame theMainFrame) {
		setLastPath();
		
		final JFileChooser fc = new JFileChooser(lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
		
		if (fc.showSaveDialog(theMainFrame) != JFileChooser.APPROVE_OPTION) return null;
		if (fc.getSelectedFile() == null) return null;
		
		final File f = fc.getSelectedFile();
		final File d = f.getParentFile();
		if (!d.canWrite()) { 
			JOptionPane.showMessageDialog(null, 
					"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
			return null;
		}
		
		if (f.exists()) {
			int writeOption = JOptionPane.showConfirmDialog(theMainFrame," The file '" + f.getName() 
				+ "' already exists. \nOverwrite it?", "Save to File", JOptionPane.YES_NO_OPTION);
			
			if (writeOption != JOptionPane.YES_OPTION) return null;
		}
		setThisPath(d.getAbsolutePath());
		return f;
	}
	/***************************************************
	 * Return file handle for BasicGoTablePanel
	 * where append/merge or overwrite are options
	 */
	static public File getFileHandle(String fn /*merge or append*/, String fileName, STCWFrame theMainFrame) {
		setLastPath();
		
		final JFileChooser fc = new JFileChooser(lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
		
		if (fc.showSaveDialog(theMainFrame) != JFileChooser.APPROVE_OPTION) return null;
		if (fc.getSelectedFile() == null) {
			Out.prt("No file selected.  Default file: " + fileName + " Default path:" + lastSaveFilePath);
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
			Object[] options = {"Cancel", "Overwrite", fn};
			int n = JOptionPane.showOptionDialog(theMainFrame,
			    "File exists: " + f.getName(), "File exists",
			    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
			    null, options, options[2]);
			
			if (n==0) return null; // cancel
			
			bAppend = (n==2) ? true : false;  // append or merge, according to 'fn'
		} 
		else bAppend = false;
		
		setThisPath(d.getAbsolutePath());
		return f;	
	}
	static boolean bAppend=false;
	static public boolean isAppend() {return bAppend;}
	/**********************************************************
	 * File selector and return PrintWriter (was in MainTable)
	 * Called by sng.util.MainTable and BasicTablePanel
	 * Run within a thread 
	 */
	public static PrintWriter getWriter(String label, String fileName, STCWFrame frame) {
 		try {
 			setLastPath();
 			final String path = lastSaveFilePath; 
 			
			final JFileChooser fc = new JFileChooser(path);	
			fc.setSelectedFile(new File(fileName));
			
			if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return null;
			if (fc.getSelectedFile() == null) {
				Out.prt("No file selected.  Default file: " + fileName + " Default path:" + lastSaveFilePath);
				return null;
			}
			
			final File f = fc.getSelectedFile();
			final File d = f.getParentFile();
			if (!d.canWrite()) { 
				JOptionPane.showMessageDialog(null, 
						"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
				return null;
			}
	
			boolean append=false;
			if (f.exists()) {
				Object[] options = {"Cancel", "Overwrite", "Append"};
				int n = JOptionPane.showOptionDialog(frame,
				    "File exists: " + f.getPath(), "File exists",
				    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				    null, options, options[2]);
				if (n==0) return null;
			
				if (n==2) append=true;
			}
			
			setThisPath(d.getAbsolutePath());
 			return new PrintWriter(new BufferedWriter(new FileWriter(f, append)));
 		} 
 		catch (Exception e) {ErrorReport.prtReport(e, "Error: cannot write file");}
 		return null;
    
	}
	/***********************************************************
	 * File Selector and writes to file
	 * Called from SeqDetail (not threaded)
	 */
	public static void saveTextToFile(String lines, String fileName, STCWFrame frame) {
		setLastPath();
		
		final JFileChooser fc = new JFileChooser(lastSaveFilePath);
		fc.setSelectedFile(new File(fileName));
		
		if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
		if (fc.getSelectedFile() == null) {
			Out.prt("No file selected.  Default file: " + fileName + " Default path:" + lastSaveFilePath);
			return;
		}	
		
		final File f = fc.getSelectedFile();
		final File d = f.getParentFile();
		if (!d.canWrite()) { 
			JOptionPane.showMessageDialog(null, 
					"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
			return;
		}
		
		int writeOption = JOptionPane.YES_OPTION;
		if (f.exists()) {
			writeOption = JOptionPane.showConfirmDialog(
				    frame,"The file already exists, overwrite it?",
				    "Save to File", JOptionPane.YES_NO_OPTION);
		}
		if (writeOption == JOptionPane.YES_OPTION) {
			setThisPath(d.getAbsolutePath());
			final String theLines = lines;
			
			Thread theThread = new Thread(new Runnable() {
				public void run() {
			    	PrintWriter pw = null;
			    	try {
			    		Out.prt("Write to " + f.getPath());
			    		pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
			    		pw.print(theLines);
			    		pw.close();
			    	}
			    	catch(Exception e) {ErrorReport.prtReport(e,  "Could not write to " + f.getPath());}
				}
			});
			theThread.start();
		}
	}
	// does not work if path is "user.dir" - hangs 2nd time of use
	// but works with everything else i tried.
	static void setThisPath(String dir) {
		lastSaveFilePath=dir;
		if (lastSaveFilePath.equals(System.getProperty("user.dir"))) 
			lastSaveFilePath=null;
	}
	static void setLastPath() {
		if (lastSaveFilePath==null) {
			lastSaveFilePath = System.getProperty("user.dir")+ "/" + Globalx.EXPORTDIR;
		
			File nDir = new File(lastSaveFilePath);
			if (!nDir.exists()) {
				if (nDir.mkdir()) Out.prt("Create " + lastSaveFilePath);
				else lastSaveFilePath = System.getProperty("user.dir");
			}
		}
	}
	private static String lastSaveFilePath=null;
	
}
