package util.file;

import java.io.File;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

/********************************************************
 * Represents all possible files used in TCW.
 * FASTA: 	runSingle, viewSingle, runMulti, viewMulti
 * TAB: 	runSingle Add AnnoDB - use existing tab
 * CFG: 	runSingle Import
 * WSV: 	runSingle Add Count file;  Add loc or remarks; 
 * 			runDE Define columns or pvals
 * QUAL: 	runSingle add qual file
 * OBO:  	runSingle use GO slims
 * 
 * RULE: if a file is in the default directory, is it not written to LIB.cfg or sTCW.cfg
 */
public class  FileC {
	// default directory
	public static final int dPROJTOP 	= 1; // no project name appended
	public static final int dPROJ 		= 2; // project name appended
	public static final int dPROJHIT 	= 3; 
	public static final int dANNO		= 4;
	public static final int dRSCRIPTS	= 5;	
	public static final int dCMP 		= 6;
	public static final int dCMPHIT 	= 7;
	public static final int dRESULTEXP	= 8;
	public static final int dUSER		= 9;
	
	// file types
	public static final int fFASTA = 1;	// .fa Sequence file
	public static final int fQUAL = 2;	// .qual Quality file
	public static final int fOBO = 3;	// GO Slim formated file
	public static final int fTAB = 4; 	// .tab TSV tab separated values -- blast hit file
	public static final int fCFG = 5;	// .cfg file
	public static final int fTXT = 6;	// .txt White-space separated values (anything with columns).
	public static final int fTSV = 7;
	public static final int fHTML = 8;
	public static final int fR = 9;		// .R for r-scripts
	public static final int fANY = 10;	// e.g. orthoMCL files
	
	// Flags
	public final static boolean bDoVer=true, bNoVer=false;
	public final static boolean bDoPrt=true, bNoPrt=false;
	
	// Write flags
	public final static int wONLY=1;
	public final static int wAPPEND=2;
	public final static int wMERGE=3;
	
	public static final String TSV_DELIM = "\t";
	public static final String TSV_SUFFIX = ".tsv"; 
	public static final String FASTA_SUFFIX = ".fa";
	public static final String TEXT_SUFFIX = ".txt";
	public static final String HTML_SUFFIX = ".html";
	
	// Removed from path for output
	private final static String PROJDIR = Globalx.PROJDIR + "/";  	// projects
	private final static String ANNODIR = Globalx.ANNODIR + "/";  	// projects/DBfasta
	private final static String CMPDIR 	= Globalx.CMPDIR + "/";		// projcmp
	/******************************************************************/
	private static String lastDir=null;
	private static int lastDirType=0;
	
	public static String getLastDir() {
		if (lastDir==null) return System.getProperty("user.dir");
		else return lastDir;
	}
	public static void setLastDir(String lastUsedDir, int lastUsed) {
		lastDir = lastUsedDir;
		lastDirType = lastUsed;
	}
	public static void setLastDirFile(String lastUsedDir, int lastUsed) {
		lastDir = lastUsedDir.substring(0, lastUsedDir.lastIndexOf("/"));
		lastDirType = lastUsed;
	}
	public static void resetLastDir() {
		lastDir=null;
		lastDirType=0;
	}
	public static int getLastType() { return lastDirType;}
	
	/***********************************************************************/
	// Add relative directory for usage and when reading from .cfg for verification
	public static String addFixedPath(String projDirName, String filePath, int pathType) {
		// full path
		if (filePath.startsWith("/"))
			if (new File(filePath).exists()) return filePath;
		
		// may have "/" before projects
		if (filePath.startsWith("/")) filePath = filePath.substring(1);
		if (new File(filePath).exists()) return filePath;
		
		// canonical TCW file name
		String fp=filePath;
		if (pathType==dPROJ)      fp = PROJDIR + projDirName + "/" + filePath;
		else if (pathType==dANNO) fp = ANNODIR + filePath;
		if (new File(fp).exists()) return fp;
	
		// relative to current directory
		fp = System.getProperty("user.dir") + "/" + filePath;
		if (new File(fp).exists()) return fp;
		
		Out.PrtError("File does not exist: " + filePath); 
		return null;
	}
	// Remove for display; anything under project/<dir> and project/annodb remains
	static public String removeFixedPath(String projDir, String filePath, int pathType) {
		String cur = System.getProperty("user.dir") + "/";
		String absPath, fullPath;
		
		// check for full path 
		if (pathType==dPROJ)      fullPath = cur + PROJDIR + projDir;
		else if (pathType==dCMP)  fullPath = cur + CMPDIR + projDir;
		else if (pathType==dANNO) fullPath = cur + ANNODIR.substring(0, ANNODIR.length()-1); // '/' at end
		else return filePath;

		if (filePath.startsWith(fullPath)) 
			return filePath.substring(fullPath.length()+1);
	
		// check for path starting with project 
		String path = filePath;
		if (path.startsWith("./")) path = path.substring(2);
		
		if (pathType==dPROJ)  absPath = PROJDIR + projDir;
		else 				  absPath = ANNODIR.substring(0, ANNODIR.length()-1);
	
		if (path.startsWith(absPath)) 
			return path.substring(absPath.length()+1);
		
		// return original
		return filePath;
	}
	// Remove up to TCW directory
	public static String removeRootPath(String fullPath) { // also in FileHelpers
		try {
			if (fullPath==null || fullPath=="") return "";
			if (!fullPath.startsWith("/")) return fullPath;
			
			String rPath = fullPath;
			String cur = System.getProperty("user.dir");
			int index = rPath.indexOf(cur);
			
			if(index == 0) {
				rPath = rPath.substring(cur.length());
				if (rPath.startsWith("/")) rPath = rPath.substring(1);
			}
			return rPath;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "removing current path from " + fullPath); }
		return fullPath;
	}
	public static String removePath(String file) {
		return file.substring(file.lastIndexOf("/"));
	}
	/**********************************************************************/
	// Directory for File Chooser
	public static File getDirPath(String projName, int pathType) {
		String d="";
	
		if (pathType==lastDirType)  	d = getLastDir();
		else if (pathType==dPROJTOP)   	d = PROJDIR; 
		else if (pathType==dPROJ)   	d = PROJDIR + projName; 
		else if (pathType==dPROJHIT)  	d = PROJDIR + projName + "/" + Globalx.pHITDIR + "/"; // CAS314
		
		else if (pathType==dUSER)   	d = PROJDIR + Globalx.USERDIR; // CAS317 remove extra "/"
		else if (pathType==dANNO)   	d = ANNODIR;
		
		else if (pathType==dCMP)   		d = CMPDIR + projName;
		else if (pathType==dCMPHIT)   	d = CMPDIR + projName + "/" + Globalx.pHITDIR + "/";
		
		else if (pathType==dRSCRIPTS)   d = Globalx.RSCRIPTSDIR + "/";
		else 							d = System.getProperty("user.dir");
		
		File f = new File(d);
		if (!f.exists() || !f.isDirectory()) {
			Out.PrtErr("Directory does not exist: " + d);
			return new File(System.getProperty("user.dir"));
		}
		return f;
	}
	// Directory for File Write
	public static File getDirPath(int pathType) {
		String d="";
	
		if (pathType==lastDirType)  	d = getLastDir();
		else if (pathType==dRESULTEXP)  d = Globalx.rEXPORTDIR + "/";
		else 							d = System.getProperty("user.dir");
		
		File f = new File(d);
		if (!f.exists()  || !f.isDirectory()) {
			if (f.mkdir()) Out.prt("Create " + d);
			else {
				Out.PrtWarn("Cannot create " + d);
				f = new File(System.getProperty("user.dir"));
			}
		}
		return f;
	}
}
