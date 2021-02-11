package util.file;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.methods.ErrorReport;
import util.methods.Out;

/**********************************************************
 * All export - added CAS316
 */
public class FileWrite {

	public final String [] faExt = new String [] {"fa","fasta",  "fna", "ffn", "faa", "frn"};
	
	private final int pathType = 	FileC.dRESULTEXP;
	private final String faSuffix = FileC.FASTA_SUFFIX;
	private final String txtSuffix = FileC.TEXT_SUFFIX;
	private final String tsvSuffix = FileC.TSV_SUFFIX;
	private final String htmlSuffix = FileC.HTML_SUFFIX;
	
	public FileWrite (boolean bVerify, boolean bPrt) {
		this.bVerify=bVerify;
		this.bPrt=bPrt;
	}
	/***********************************************
	 * File Reader - always write into ResultExport as default
	 */
	public File run(Component btnC, String defFile, int fileType, int wrType) { // c can be button
		return run(btnC, "", defFile, fileType, wrType);
	}
	public File run(Component btnC, String title, String defFile, int fileType, int wrType) { // c can be button
		try {	
			if (defFile==null || defFile=="") defFile="Default";
			if (fileType==FileC.fTXT   && !defFile.endsWith(txtSuffix))   	defFile += txtSuffix;
			if (fileType==FileC.fTSV   && !defFile.endsWith(tsvSuffix))   	defFile += tsvSuffix;
			if (fileType==FileC.fHTML  && !defFile.endsWith(htmlSuffix)) 	defFile += htmlSuffix;
			if (fileType==FileC.fFASTA && 
				(!defFile.endsWith(faSuffix) && !defFile.endsWith(".fasta"))) defFile += faSuffix;
			
			FileVerify verObj = new FileVerify();
			
			JFileChooser fc = new JFileChooser();

			fc.setCurrentDirectory(FileC.getDirPath(pathType));
			fc.setSelectedFile(new File(defFile));
			
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);//user must select a file not folder
			fc.setMultiSelectionEnabled(false);				//disabled selection of multiple files
			
			if (fileType==FileC.fTXT) { 
				FileFilter ff = new FileNameExtensionFilter("Text (.txt)", "txt");
				fc.addChoosableFileFilter(ff);	
				fc.setFileFilter(ff);			
			}
			else if (fileType==FileC.fTSV) { 
				FileFilter ff = new FileNameExtensionFilter("Column (.tsv)", "tsv");
				fc.addChoosableFileFilter(ff);	
				fc.setFileFilter(ff);			
			}
			else if (fileType==FileC.fFASTA) { 
				FileFilter ff = new FileFilter() {
				    public String getDescription() {
				        return "FASTA (.fa, .fasta, etc...)";
				    }
				    public boolean accept(File f) {
				        if (f.isDirectory()) {
				            return true;
				        } else {
				        	String fName = f.getName().toLowerCase();
				        	for (String x : faExt) {
					            if (fName.endsWith(x)) return true;
				        	}
				            return false;
				        }
				    }
				};
				fc.addChoosableFileFilter(ff);	// ff added to filechooser
				fc.setFileFilter(ff);			// set ff as default selection
			}
			fc.setAcceptAllFileFilterUsed(true);
		
			// On MacOS, the ShowSaveDialog grays out all files except directories, regardless of filters
			fc.setDialogTitle("Save " + title);
			if(fc.showDialog(btnC, "Save") != JFileChooser.APPROVE_OPTION) return null;
			
			final File f = fc.getSelectedFile();
			final File d = f.getParentFile();
			if (!d.canWrite()) { 
				JOptionPane.showMessageDialog(null, 
						"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
				return null;
			}
			if (f.exists()) {
				if (wrType==FileC.wONLY) {
					Object[] options = {"Cancel", "Overwrite"};
					int n = JOptionPane.showOptionDialog(btnC,"File exists: " + f.getName(), "File exists",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (n==0) return null; // cancel	
				}
				else {
					Object[] options = {"Cancel", "Overwrite", "Append"};
					if (wrType==FileC.wMERGE) options[2] = "Merge";
					
					int n = JOptionPane.showOptionDialog(btnC, "File exists: " + f.getName(), "File exists",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
						
					if (n==0) return null; // cancel	
					bAppend = (n==2) ? true : false;  // append or merge, according to 'fn'
				}
				if (bVerify) verObj.verify(bPrt, f.getName(), fileType); 
			}
			
			String file = fc.getSelectedFile().getPath();
			
			FileC.setLastDirFile(file, pathType);
			
			return new File(file);
		}
		catch(Exception e) {
			Out.prt("Default file name: " + defFile);
			ErrorReport.prtReport(e, "Error finding file"); 
			return null;
		}
	}
	/*************************************************************************
	 * Provides nl. Can do print, println. No exception.
	 */
	public PrintWriter getWriter(Component c, String title, String defFile, int fileType, int wrType) {
		try {
			File f = run(c, title, defFile, fileType, wrType);
			if (f==null) return null;
			return new PrintWriter(new BufferedWriter(new FileWriter(f, bAppend)));
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting print writer"); 
			return null;
		}
	}
	/*************************************************************************
	 * Needs user to provide nl. Can only do write. Throws exception.
	 */
	public BufferedWriter getBWriter(Component c, String title, String defFile, int fileType, int wrType) {
		try {
			File f = run(c, title, defFile, fileType, wrType);
			if (f==null) return null;
			return new BufferedWriter(new FileWriter(f, bAppend));
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting print writer"); 
			return null;
		}
	}
	/*************************************************************************/
	public void writeText(Component c, String title, String defFile, int fileType, int wrType, String lines) {
		try {
			File f = run(c, title, defFile, fileType, wrType);
			if (f==null) return;
			Out.prt("Write to " + f.getPath());
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
    		pw.print(lines);
    		pw.close();
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting print writer"); 
			return;
		}
	}
	/*************************************************************************/
	public void writeTextThread(Component c, String defFile, int fileType, int wrType, String lines) {
		try {
			File f = run(c, defFile, wrType, fileType);
			if (f==null) return;
			
			final String theLines = lines;
			final File fh = f;
			
			Thread theThread = new Thread(new Runnable() {
				public void run() {
			    	PrintWriter pw = null;
			    	try {
			    		Out.prt("Write to " + fh.getPath());
			    		pw = new PrintWriter(new BufferedWriter(new FileWriter(fh)));
			    		pw.print(theLines);
			    		pw.close();
			    	}
			    	catch(Exception e) {ErrorReport.prtReport(e,  "Could not write to " + fh.getPath());}
				}
			});
			theThread.start();			
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error getting print writer"); 
			return;
		}
	}
	/********************************************************************************/
	public boolean isAppend() {return bAppend;}
	private boolean bAppend=false;
	
	private boolean bVerify=false, bPrt=false;
}
