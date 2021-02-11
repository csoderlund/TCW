package util.file;

import java.io.BufferedReader;
import java.io.File;

import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;

/************************************************************
 * Verifies all file types used in TCW
 */
public class FileVerify {
	
	public FileVerify() {}
	
	public boolean verify(boolean bPrt, String file, int fileType) {
		this.bPrt = bPrt;
		if (bPrt) Out.prt("Verifying file " + FileC.removeRootPath(file));
		
		try {
			File f = new File(file);
			if (!f.exists()) 
				return prtWarn("", "File does not exist", file);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot open file " + file); return false;}
		
		if (fileType==FileC.fFASTA)    return verifyFasta(file);
		else if (fileType==FileC.fQUAL)return verifyQFasta(file);
		else if (fileType==FileC.fTXT) return verifyWSV(file); // white-space separated values
		else if (fileType==FileC.fTAB) return verifyBlastTab(file);
		else if (fileType==FileC.fOBO) return verifyOBO(file);
		else if (fileType==FileC.fCFG) return verifyCFG(file);
		return false;
	}
	private boolean verifyFasta(String seqfile) {
		String type="Sequence file";
		try {
			BufferedReader br = FileHelpers.openGZIP(seqfile);
			if (br==null) 
				return prtWarn(type, "Cannot open file", seqfile);
	
			// Read header line
			String line="";
			while(line.equals("") || line.startsWith("#") || line.startsWith("/")) {
				line = br.readLine();
				if (line==null) {
					br.close();
					return prtWarn(type, "Empty file -- no valid lines", seqfile);
				}
				else line = line.trim();
			}
			if (!line.startsWith(">")) { 
				br.close();
				Out.PrtWarn("First non-blank line does not start with >\n" + "Line: " + line);
				return prtWarn(type, "Wrong format", seqfile);
			}
			this.line = line;
			
			// read sequence line
			line = br.readLine().trim(); 
			if (!line.matches("^[a-zA-Z*]+$")) {
				br.close();
				Out.PrtWarn("First line after > is not all characters.\n" + "Line: " + line);
				return prtWarn(type, "Wrong format", seqfile);
			}
			// is good, determine type
			int cntP=0, cntN=0;
			if (BlastArgs.isProtein(line)) cntP++;
			if (BlastArgs.isNucleotide(line)) cntN++;
			
			if 		(cntP==1 && cntN==0) bIsProtein=true;
			else if (cntP==0 && cntN==1) bIsProtein=false;
			else {
				do {
					line = br.readLine().trim();
				} while (line.startsWith(">"));
				
				if (BlastArgs.isProtein(line)) cntP++;
				if (BlastArgs.isNucleotide(line)) cntN++;
				if (cntP>cntN)  bIsProtein=true;
				else if (cntP<cntN) bIsProtein=false;
				else 
					UserPrompt.showWarn("Cannot determine if file is protein or nucleotide -- " + seqfile);
			}
			br.close();
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Verifying sequence file"); 
			return prtWarn(type, "Could not verify", seqfile);
		}
	}
	private boolean verifyQFasta(String qualfile) {
		String type="Quality file";
		try {
			BufferedReader br = FileHelpers.openGZIP(qualfile);
			if (br==null) 
				return prtWarn(type,"Could not open", qualfile);
			
			// Read header line
			String line="";
			while(line.equals("") || line.startsWith("#") || line.startsWith("/")) {
				line = br.readLine();
				if (line==null) {
					br.close();
					return prtWarn(type, "Empty file -- no valid lines", qualfile);
				}
				else line = line.trim();
			}
			if (!line.startsWith(">")) {
				br.close();
				Out.PrtWarn("First non-blank line does not start with >\n" + "Line: " + line);
				return prtWarn(type, "Incorrect format", qualfile);
			}
			
			// Read first quality line
			line = br.readLine().trim();
			if (!line.matches("^[0-9 ]+")) {
				br.close();
				Out.PrtWarn("First line after > has characters other than digits and blanks.\n" + "Line: " + line);
				return prtWarn(type, "Incorrect format", qualfile);
			} 
			br.close();
			return true;
		}
		catch (Exception e) {
			prtWarn(type, "Could not verify" , qualfile);
			ErrorReport.prtReport(e, "Verifying quality file"); 
			return false;
		}
	}
	private boolean verifyWSV(String file) {
		String type = "Space or tab delimited file";
		try {
			BufferedReader reader = FileHelpers.openGZIP(file);
			if (reader==null) 
				return prtWarn(type, "Could not open", file);
			
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.trim().length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine();
			}
			reader.close();
			
			if ( line == null ) 
				return prtWarn(type, "File is empty", file);
				
    		String[] tokens = line.split("\\s");
    		if (tokens == null || tokens.length < 2) 
    			return prtWarn(type, "File not space or tab delimited", file);
			
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "error reading " + file); 
			return prtWarn(type, "Could not verify", file);
		}
	}
	private boolean verifyBlastTab(String file) {
		String type="Hit Tabular file";
		try {
			BufferedReader reader = FileHelpers.openGZIP(file);
			if (reader==null) 
				return prtWarn(type, "Could not open file", file);
			
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.trim().length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine();
			}
			reader.close();
			
			if ( line == null ) 
				return prtWarn(type, "File is empty", file);
			
	    	String[] tokens = line.split("\t");
	    	if (tokens == null || tokens.length < 11) 
	    		return prtWarn(type, "File is not -m 8 format", file);
	    	
	    	this.line=line;
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "error reading " + file); 
			return prtWarn(type, "Could not verify", file);
		}
	}
	
	//[Term]
	//id:
	//name:
	private boolean verifyOBO(String file) {
		String type = "OBO slims file";
		try {
			Out.PrtWarn("TCW does not check consistency between GO database and OBO file\n");
			BufferedReader in = FileHelpers.openGZIP(file);
			if (in==null) 
				return prtWarn(type, "Cannot open file", file);
				
			String line, name="";
			int gonum=0;
			boolean first=false;
			
			while ((line = in.readLine()) != null) {
				if (line.equals("[Term]")) {
					if (first) {
						if (gonum>0 && !name.equals("")) {
							in.close();
							return true;
						}
						else {	
							in.close();
							return prtWarn(type, "Format wrong", file);
						}
					}
					first=true;
				}
				else if (line.startsWith("id:") && line.contains("GO:")) {
					String tmp = line.substring(3).trim(); // remove id:
					tmp = tmp.substring(3).trim(); // remove GO:
					gonum = Integer.parseInt(tmp);
				}
				else if (line.startsWith("name:")) {
					name = line.substring(5).trim();
				}
			}
			in.close();
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Checking file " + file);  
			return prtWarn(type, "Could not verify", file);
		}
	}
	// must have at least one key=value pair
	private boolean verifyCFG(String fileName) {
		String type="Configuation file";
		try {
			BufferedReader reader = FileHelpers.openGZIP(fileName);
			if (reader==null) 
				return prtWarn(type,"Cannot open", fileName);
			
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.trim().length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine();
			}
			reader.close();
			
			if ( line == null ) 
				return prtWarn(type, "File is empty", fileName);
			
    		String[] tokens = line.split("=");
    		if (tokens == null || tokens.length < 2) 
    			return prtWarn(type, "Must have at least one key=value pair", fileName);
			
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "error reading " + fileName); 
			return prtWarn(type, "Could not verify", fileName);
		}
	}
	
	private boolean prtWarn(String fileType, String err, String file) {
		String msg = "Incorrect " + fileType + ": " + err + "\n   FileName: " + file;
		if (bPrt) {
			Out.PrtErr(msg);
			UserPrompt.showWarn(msg);
		}
		else {
			Out.PrtErr(msg);
		}
		return false;
	}
	public boolean isProtein() {return bIsProtein;}
	public String getLine() {return line;}
	
	private boolean bPrt=false, bIsProtein=false;
	private String line=null;
}
