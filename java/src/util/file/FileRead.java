package util.file;

import java.io.File;
import java.awt.Component;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

/****************************************************
 * Brings up a File Chooser window, which knows about different TCW paths, and uses last path
 * CAS316 this replaced sng.amanager.FileTextField, cmp.compile.panels.FileSelectTextField,
 * which both included the text field, which was instantiated before project was know.
 */
public class FileRead {

	private final String [] faExt = new String [] 
			{"fa","fa.gz", "fasta", "fasta.gz",  "fna", "ffn", "faa", "frn", "fna.gz", "ffn.gz", "faa.gz", "frn.gz"};
	
	public FileRead (String projName, boolean bVerify, boolean bPrt) {
		this.projName=projName;
		this.bVerify=bVerify;
		this.bPrt=bPrt;
	}
	/***********************************************
	 * File Reader
	 */
	public boolean run(Component c, String title, int pathType, int fileType) { // c can be button
		try {	
			this.pathType = pathType;
			
			FileVerify verObj = new FileVerify();
			
			JFileChooser fc = new JFileChooser();
			
			fc.setCurrentDirectory(FileC.getDirPath(projName, pathType));
			
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);//user must select a file not folder
			fc.setMultiSelectionEnabled(false);				//disabled selection of multiple files
			
			if (fileType==FileC.fR) { 
				FileFilter ff = new FileNameExtensionFilter("R script (.R)", "R");
				fc.addChoosableFileFilter(ff);	
				fc.setFileFilter(ff);			
			}
			else if (fileType==FileC.fCFG) { // CAS316
				String cfg = Globalx.CFG.substring(1);
				FileFilter ff = new FileNameExtensionFilter("Config (.cfg)", cfg);
				fc.addChoosableFileFilter(ff);	// ff added to filechooser
				fc.setFileFilter(ff);			// set ff as default selection
			}
			else if (fileType==FileC.fTAB) { // CAS314
				FileFilter ff = new FileNameExtensionFilter("Hit tab (.tab)", "tab");
				fc.addChoosableFileFilter(ff);	
				fc.setFileFilter(ff);			
			}
			else if (fileType==FileC.fTXT || fileType==FileC.fTSV) { // CAS316
				FileFilter ff = new FileNameExtensionFilter("Column (.txt, .tsv)", "txt", "tsv");
				fc.addChoosableFileFilter(ff);	
				fc.setFileFilter(ff);			
			}
			else if (fileType==FileC.fFASTA) { // CAS315 fa.gz does not work with the above, need custom.
				FileFilter ff = new FileFilter() {
				    public String getDescription() {
				        return "FASTA (.fa, .fa.gz, etc...)";
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
			
			fc.setDialogTitle("Open " + title);
			if(fc.showOpenDialog(c) == JFileChooser.APPROVE_OPTION) {
				String file = fc.getSelectedFile().getPath();
				
				if (bVerify) {
					verObj.verify(bPrt, file, fileType); 
					bIsProtein = verObj.isProtein();
				}
				FileC.setLastDirFile(file, pathType);
				fullName = file;
				
				return true;
			}
			return false;
		}
		catch(Exception e) {
			Out.prt("Project directory name: " + projName);
			ErrorReport.prtReport(e, "Error finding file"); 
			return false;
		}
	}
	
	/***************************************************************/
	// For File Readers
	public String getRelativeFile() {
		return FileC.removeRootPath(fullName);
	}
	public String getRemoveFixedPath() {
		return FileC.removeFixedPath(projName, fullName, pathType);
	}
	public String getFullPathFile() {
		return fullName;
	}
	public boolean isProtein() {return bIsProtein;}
	
	// return
	private boolean bIsProtein=false;
	private String fullName=null;
	
	// params
	private boolean bVerify=false, bPrt=false;
	private String projName=null;
	private int pathType=0;
	
}
