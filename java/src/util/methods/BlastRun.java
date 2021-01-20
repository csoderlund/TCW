package util.methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
/**********************************************
 * blastp - aa to aa
 * blastn - nt to nt
 * blastx - nt to aa
 * tblastx - translated nt to translated nt 
 * tblastn - tranlated nt to aa
 */
public class BlastRun {
	static private String[] ntFormatFiles = { ".nin", ".nhr",  ".nsq"};
	static private String[] aaFormatFiles = { ".phr", ".pin", ".psq"};
	static private String[] diaFormatFiles = {".dmnd"};
	
	static public boolean run(int ncpu, String pgm, String action, String args, 
			boolean isAAdb, String dbFile, boolean isAAseq, String inFile, String tabFile) {
		
		if (!runFormatDB(pgm, dbFile, isAAdb)) return false;
		
		String searchCmd="";
		if (pgm.equals("diamond")) {
			searchCmd = BlastArgs.getDiamondCmd(inFile, dbFile, tabFile, action, args, ncpu);
			if (!args.contains("--quiet")) searchCmd += " --quiet"; // CAS314 diamond is no longer quite by default
		}
		else if (pgm.equals("blast"))  
			searchCmd = BlastArgs.getBlastCmd(inFile, dbFile, tabFile, action, args, ncpu);
		else {
			Out.PrtError("Command '" + pgm + "'  not a valid option" );
			return false;
		}
			
		try {
			Out.PrtSpMsg(3, searchCmd);
			
			Process p = Runtime.getRuntime().exec(searchCmd);
			
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = input.readLine()) != null) System.out.println(line);
			input.close();
			
			p.waitFor();
			
			if (p.exitValue() != 0) {
				Out.PrtError(pgm + " failed with exit value = " + p.exitValue());
				message(pgm);
				return false;
			}
			
			if (!new File(tabFile).exists()) {
				if (!new File(tabFile + ".gz").exists()) 
					Out.PrtWarn("No output file created: " + tabFile);
				return false;
			}
			else return true;
		} catch (Exception e) {
			ErrorReport.reportError(e, "Executing search");
			return false;
		}
	}
	// used by both selfblasts and DB blasts
	static private boolean runFormatDB (String pgm, String dbFileName, boolean isAAdb) {
		String formatdbCmd;
		long dbFastaTime = (new File(dbFileName)).lastModified();
			
		try {
		// Check for existing
			String [] checkFiles;
			if (isAAdb) {
				if (pgm.equals("diamond")) 		checkFiles = diaFormatFiles;
				else 							checkFiles = aaFormatFiles;
			}
			else 								checkFiles = ntFormatFiles;			
			
			boolean exists = true;
			
			for (int i = 0; i < checkFiles.length && exists; i++) {
				String s1 = dbFileName + checkFiles[i];
				String s2 = dbFileName + ".00" + checkFiles[i];  // BLAST specific
				if (!FileHelpers.fileExists(s1) && !FileHelpers.fileExists(s2)) {
					exists=false;
					break;
				}
			
				if (!FileHelpers.fileExists(s1)) s1 = s2;
				long fdbTime = (new File(s1)).lastModified();
				
				if (fdbTime < dbFastaTime) {
					Out.PrtSpMsg(3,"Format files are out of date - reformatting");
					exists= false;
					break;
				}				
			}
			if (exists) {
				Out.PrtSpMsg(3,"Using existing formated files");
				return true;
			}
			
		// No existing, Format
			if (pgm.equals("diamond"))		formatdbCmd = BlastArgs.getDiamondFormat(dbFileName, dbFileName);
			else {
				if (isAAdb) 				formatdbCmd = BlastArgs.getFormatp(dbFileName);
				else 						formatdbCmd = BlastArgs.getFormatn(dbFileName);
			}
			
			Out.PrtSpMsg(3,"Format file for " + pgm);
			Out.PrtSpMsg(3, formatdbCmd);
			long time = Out.getTime();
			
			Process p = Runtime.getRuntime().exec(formatdbCmd);
			p.waitFor();
			if (p.exitValue() != 0) {
				Out.PrtError("failed with exit value = " + p.exitValue());
				messageFormat();
				return false;
			}
			Out.PrtSpMsgTime(3, "Complete formatting", time);
			return true;
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Running format database");
			messageFormat();
		}
		return false;
	}
	static private void message(String pgm) {
		Out.Print("Suggestion: if the error is not obvious, copy the command from above,");
		Out.PrtSpMsg(2,"and run it from the command line -- it should tell you the problem.");
		
		if (pgm.equals("diamond")) {
			if (BlastArgs.isDiamond()) {
				Out.Print("+++ TCW was tested with diamond 0.9.22 (Sept 2018)");
				Out.PrtSpMsg(2,"If this is out-of-date, please email tcw@agcol.arizona.edu and TCW will be updated");
			}
			else {
		         Out.Print("+++ Diamond was not found on your machine. See Touble.html");
		    }
		}
		else if (pgm.equals("blast")) {
			if (BlastArgs.isBlast()) {
				Out.Print("+++ TCW was tested on blast+ 2.7.1, NCBI (10/3/17)");
				Out.PrtSpMsg(2,"If this is out-of-date, please email tcw@agcol.arizona.edu and TCW will be updated");
		    }
			else {
		         Out.Print("+++ Blast+ was not on your machine. See Touble.html");
		    }
		}
	}
	static private void messageFormat() {
		Out.Print("\nSuggestion: copy the command from above,");
		Out.PrtSpMsg(2,"and run it from the command line -- it should tell you the problem.");
		Out.Print("Check that it has permissions set for execution");
	}
}
