package cmp.compile;
/*****************************************************
 * Run blast
 * Note: pairwise is created from blast files in Pairwise.java
 */
import java.io.*;

import util.methods.BlastRun;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;
import cmp.compile.panels.CompilePanel;
import cmp.database.Globals;

public class DoBlast {
	
	public static String runBlastp(CompilePanel cmpPanel, String pgm, String tabFile, String blastParams) {
		try {
			long startTime = Out.getTime();
					
			int n = cmpPanel.getBlastPanel().getCPUs();
			if (n==0) n=1;
			
			String combinedFile = combinedFile(cmpPanel, Globals.CompilePanel.ALL_AA_FASTA); 
			File f = (combinedFile!=null) ? new File(combinedFile) : null;
			if (f==null || !f.exists()) {
				Out.PrtError("Could not create combined file");
				return null;
			}
			long fileSize = f.length();
			
			// Execute........
			Out.PrtDateMsg("\nRunning " + pgm + " on " + FileHelpers.getSize(fileSize) + " combined file");
			Out.PrtSpMsg(1, "Parameters: " + blastParams);
			
			boolean rc = BlastRun.run(n, pgm, "blastp", blastParams, true, combinedFile, true, combinedFile, tabFile);
			
			Out.PrtMsgTime("Complete " + pgm, startTime);
			if (!rc) pgm=null;
			return pgm + " " + blastParams;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error running " + pgm);
			return null;
		}
	}
	public static String runBlastn(CompilePanel cmpPanel, String tabFile, String blastParams) {
		try {
			long startTime = Out.getTime();
							
			int n = cmpPanel.getBlastPanel().getCPUs();
			if (n==0) n=1;
			
			String combinedFile = combinedFile(cmpPanel, Globals.CompilePanel.ALL_NT_FASTA);
			File f = (combinedFile!=null) ? new File(combinedFile) : null;
			if (f==null || !f.exists()) {
				Out.PrtError("Could not create combined file");
				return null;
			}
			long fileSize = f.length();
			
			// Execute........
			Out.PrtDateMsg("\nRunning blastn on " + FileHelpers.getSize(fileSize) + " combined file");
			
			boolean rc = BlastRun.run(n, "blast", "blastn", blastParams, false, combinedFile, false, combinedFile, tabFile);
			
			Out.PrtMsgTime("\nComplete blastn", startTime);
			
			String pgm = (rc) ? "blastn " + blastParams: null;
			return pgm;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error running blastn");
			return null;
		}
	}
	private static String combinedFile(CompilePanel cmpPanel, String file) {
		String blastDir = cmpPanel.getCurProjAbsDir() + "/" + Globals.CompilePanel.BLASTDIR;
		String combinedFile = blastDir + "/" + file; 		
		File testDir = new File(blastDir);
		if(!testDir.exists()) { // should have been made during Create database
			if (!runMTCWMain.generateFastaFromDB(cmpPanel)) return null;
		}
		else {
			File combFile = new File(blastDir, file);
			if (!combFile.exists() || combFile.length() < 10)
				runMTCWMain.generateFastaFromDB(cmpPanel);
		}
		return combinedFile;
	}
}
