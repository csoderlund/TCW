package sng.runAS;
/*********************************************************
 * DoUP perform file handling for downloading taxonomic and full uniprot
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import util.database.Globalx;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.TimeHelpers;

public class DoUP {
	private final String upTaxURL = 
"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/";
	private String upFullURL = 
"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/";
	
	// test stuff -- so read file on disk instead of over network for testing
	private boolean test=ASMain.test;
	private String demoIn = "./projects/DBfasta/dats/";
	
	// naming
	public static final String subset = "fullSubset";
	private final String SP = Globalx.SP, TR=Globalx.TR;
	private final String spPre = "uniprot_sprot", trPre = "uniprot_trembl";
	
	private final Pattern fasta =  Pattern.compile(">(\\S+)");
	private final Pattern patID = Pattern.compile("ID\\s+(\\S*)"); 
	private final DecimalFormat df = new DecimalFormat("#,###,###");
	
	public DoUP (ASFrame asf) {frameObj=asf;} 
	
	/**************************************************************
	 * XXX Taxonomic
	 */
	public void xTaxo(Vector <String> spFiles, Vector <String> trFiles, 
			Vector <Boolean> spDats, Vector <Boolean> trDats, String dir) {
		try {
			Out.PrtDateMsg("\nStart Taxonomic UniProt");
			targetUpDir = dir;
			if (!mkDirTaxo(spFiles, trFiles)) return;
			
			long startTime = Out.getTime();
			if (spFiles.size()>0) {
				Out.PrtSpDateMsg(1, "Download SwissProt files");
				for (int i=0; i<spFiles.size(); i++) {
					if (!spDats.get(i)) {
						String f = spFiles.get(i);
						if (!runDownload(SP, spPre, f, f)) return;	
					}
				}
			}
			if (trFiles.size()>0) {
				Out.PrtSpMsg(1,"");
				Out.PrtSpDateMsg(1, "Download TrEMBL files");
				for (int i=0; i<trFiles.size(); i++) {
					if (!trDats.get(i)) {
						String f = trFiles.get(i);
						if (!runDownload(TR, trPre, f, f)) return;	
					}
				}
			}
			Out.PrtSpMsg(1,"");
			Out.PrtSpDateMsg(1, "Create FASTA files");
			DoUPdat datObj = new DoUPdat(frameObj);
			for (String f : spFiles) {
				if (!datObj.fasta(SP, mkNameDir(SP,f), 
						mkNameDat(spPre,f), mkNameFasta(spPre,f))) return;
			}
			for (String f : trFiles) {
				if (!datObj.fasta(TR, mkNameDir(TR,f), mkNameDat(trPre,f), 
						mkNameFasta(trPre,f))) return;
			}
			Out.PrtMsgTime("Complete Taxonomic UniProt", startTime);
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems getting file");}
	}
	
	/*******************************************************
	 * XXX Full
	 */
	public void xFull(boolean isSP, boolean isTR, boolean hasSPdat, boolean hasTRdat, String dir) {
		Out.PrtDateMsg("\nStart Full UniProt");
		targetUpDir = dir;
		long startTime = Out.getTime();
		
		if (!mkDirFull(isSP, isTR)) return;
		
		if (isSP && !hasSPdat) {
			Out.PrtSpDateMsg(1, "Download SwissProt");
			if (!runDownload(SP, spPre, "", subset)) return;	
		}
		if (isTR && !hasTRdat) {
			Out.PrtSpDateMsg(1, "Download TrEMBL");
			if (!runDownload(TR, trPre, "", subset)) return;	
		}
		Out.PrtSpDateMsg(1, "Create subset files");
		DoUPdat datObj = new DoUPdat(frameObj);
		if (isSP) {
			if (!fullCreateSubset(SP,spPre)) return;
			
			if (!datObj.fasta(SP, mkNameDir(SP, subset), 
					mkNameDat(spPre, subset), mkNameFasta(spPre, subset))) return;
		}
		if (isTR) {
			if (!fullCreateSubset(TR,trPre)) return;
			
			if (!datObj.fasta( TR,mkNameDir(TR, subset), 
					mkNameDat(trPre, subset), mkNameFasta(trPre, subset))) return;
		}
		Out.PrtSpMsgTime(0, "Complete Full UniProt", startTime);
	}
	
	/*********************************************
	 * XXX Make subset: Read uniprot_Px.data.gz, write uniprot_Px_subset.data, then gzip 
	 */
	
	private boolean fullCreateSubset(String type, String Px) {
		long startTime = Out.getTime();
		String path = mkNameDir(type, subset);
		String inPath = path + mkNameDat(Px, "");
		String outPath = path + mkNameDat(Px, subset);
		Out.PrtSpMsg(2, "Make .dat subset " + outPath);
		
		try {	
			if (!fullReadFasta(type)) return false;
			
			Out.PrtSpMsg(3, "Sort list");
			if (taxoIDs==null) { 
				Out.PrtError("Error createing full subset (taxoIDs is null)");
				Out.die("Cannot continue - may have run out of memory");
			}
			Arrays.sort(taxoIDs);
			
			if (!fullWriteDat(inPath, outPath)) return false;
			Out.PrtSpMsgTime(2, "Complete creating .dat subset file ", startTime);
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "creating full subset");}
		catch (OutOfMemoryError e) {prtMemError(e, "creating full subset");}
		return false;
	}
	/***************************************************
	 * First count entries in taxonmic fasta file
	 * Allocate string array
	 * Enter entries from taxonomic fasta files
	 */
	private boolean fullReadFasta(String type) {
		long startTime = Out.getTime();
		try {
			File dir = new File(targetUpDir);
			File [] dirs = dir.listFiles();
			
			for (int x=0; x<2; x++) {
				if (x==0) Out.PrtSpMsg(3, "Count taxonomic entries");
				else Out.PrtSpMsg(3, "Record taxonomic entries");
				
				for (File d : dirs) {
					if (!d.isDirectory()) continue;
					
					String fname = d.getName();
					if (!fname.startsWith(type) || fname.contains(subset)) continue;
					
					File [] xfiles = d.listFiles();
					for (File f : xfiles) {
						String fName = f.getName();
						if (fName.endsWith(".fasta") || fName.endsWith(".fasta.gz")) {
							if (fullReadTaxo(f, x)) break;
							else return false;
						}
					}
				}
				if (x==0) {
					Out.PrtSpMsg(3, "Allocate memory for " + df.format(index) + " total entries");
					taxoIDs = new String [index];
					index=0;
				}
			}
			Out.PrtSpCntMsg(4, index, "IDs will not be written to subset");
			Out.PrtSpMsgTime(3, "Complete making list", startTime);
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error reading file");}
		catch (OutOfMemoryError e) {ErrorReport.prtReport(e, "Memory error reading file ");}
		return false;
	}
	// " >sp|Q6V4H0|10HGO_CATRO 8-hyxxxxxxxxxx xxxxxxxx xxxxxx";
	private boolean fullReadTaxo(File f, int action) { 
		String line="";
		int cnt=0, cntErr=0;
		Matcher x;
		String fn = f.getName();
		
		try {		
			BufferedReader in = FileHelpers.openGZIP(f.getAbsolutePath()); // CAS315
			if (in==null) Out.die("Cannot continue");
		
			while ((line = in.readLine()) != null) {
				if (!line.startsWith(">")) continue;
				line = line.trim();
				cnt++;
				x = fasta.matcher(line);
				if (!x.find()) System.err.println("Parse error: " + line);
				else {
					String [] tok = x.group(1).split("\\|");
					if (tok.length!=3) {
						Out.PrtErr("Invalid identifier: " +  x.group(1));
						cntErr++;
						if (cntErr>3) Out.die("A problem with file " + fn); // CAS315
						continue;
					}
					if (tok[2].length()>30) {
						Out.PrtErr("Identifier too long: " +  tok[2]);
						Out.prt("Line: " +  line);
						continue;
					}
					if (action==1) taxoIDs[index] = tok[2];
					tok = null; 
					index++;
					if (cnt%100000==0) Out.r("read " + cnt + " in " + fn);
				}
			}
			in.close();
			System.err.print("                                                     \r");
			if (action==0) Out.PrtSpCntMsg(4, cnt, "IDs in " + fn);
		
			return true;
		}
		catch (Exception e) {
			String msg = "Reading #" + cnt + " " + fn + "                 \n" + 
					"Error parsing: " + line;
			ErrorReport.prtReport(e, msg);
		}
		catch (OutOfMemoryError e) { prtMemError(e, "reading taxonomic databases");}
		return false;
	}
	/**********************************************
	 * Read full sp/tr .dat UniProt
	 * Write entries not in taxonomic fasta files
	 */
	private boolean fullWriteDat(String inPath, String outPath) {
		long startTime = Out.getTime();
		try {		
			Out.PrtSpMsg(3, "Write .dat.gz subset file ");
			
			BufferedReader in = FileHelpers.openGZIP(inPath);
			if (in==null) return false;
			BufferedWriter out = FileHelpers.createGZIP(outPath);
			if (out==null) return false;
			
			String line, id="";
			boolean bwrite=false;
			long cnt=0, cnt1=0;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith("ID")) {
					if (bwrite) out.write(line + "\n");
					continue;
				}
				cnt++;
				Matcher x = patID.matcher(line);
				if (x.find()) {
					id = x.group(1);
					int index = Arrays.binarySearch(taxoIDs,id);
					if (index>=0) bwrite=false;
					else {
						cnt1++;
						bwrite=true;
						out.write(line + "\n");
					}
				}
				else System.out.println("Invalid line: " + line);
				
				if (cnt%5000==0) System.err.print(cnt1 + " wrote from " + cnt + "\r");
			}
			String p = Static.perText(cnt1, cnt);
			Out.PrtSpMsgTime(4,  df.format(cnt1) + " wrote from " +  df.format(cnt) + " " + p, startTime);
			in.close(); out.close();
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error writing full fasta");}
		catch (OutOfMemoryError e) {prtMemError(e, "Writing subset fasta");}
		return false;
	}
	/*********************************************************
	 * XXX Utilities
	 */
	private boolean runDownload(String type, String prefix, String tax, String dir) {
		try {
			boolean rc=false;
			String inFile =  mkNameDat(prefix, tax);
			String outPath = mkNameDir(type, dir) + mkNameDat(prefix, tax);
			if (test) {
				String cmd = "cp " + (demoIn+inFile) + " " + outPath;
				if (new File(outPath).exists()) {
	    				Out.PrtSpMsg(2, "File exists " + outPath);
	    				return true;
	    			}
				int ret = new RunCmd().runP(cmd, null, true);
				rc = (ret==0) ? true: false;
			}
			else {
				if (tax=="") rc = ASMain.ioURL(upFullURL, inFile, outPath);
				else rc = ASMain.ioURL(upTaxURL, inFile, outPath);
			}
			return rc;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems getting file");}
		return false;
	}
	private String mkNameDir(String type, String tax) { 
		if (!targetUpDir.endsWith("/")) targetUpDir += "/";
		return targetUpDir + type + "_" + tax  + "/";
	}
	private String mkNameFasta(String ptx, String tax) {
		return ptx + "_" + tax + ".fasta";
	}
	private String mkNameDat(String ptx, String tax) {
		if (tax=="") return ptx + ".dat.gz";
		return ptx + "_" + tax + ".dat.gz";
	}
	
	private boolean mkDirFull(boolean bsp, boolean btr) {
		try {
			Out.PrtSpMsg(1, "Check Directories ");
			if (!mkCheckDir(targetUpDir)) return false;
			
			if (bsp && !mkCheckDir(mkNameDir(SP, subset))) return false;
			if (btr && !mkCheckDir(mkNameDir(TR, subset))) return false;
			return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems creating directories");}
		return false;
	}
	private boolean mkDirTaxo(Vector <String> spFiles, Vector <String> trFiles) {
		try {
			Out.PrtSpMsg(1, "Check Directories");
	        
			if (!mkCheckDir(targetUpDir)) return false;
			
			for (String f : spFiles) {
				if (!mkCheckDir(mkNameDir(SP, f))) return false;
			}
			for (String f : trFiles) {
				if (!mkCheckDir(mkNameDir(TR, f))) return false;
			}
			Out.PrtSpMsg(1, "Check complete");
			return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems creating directories");}
		return false;
	}
	
	private boolean mkCheckDir(String name) {
		try {
			File d = new File(name);
	        if (!d.exists()) {
	        		Out.PrtSpMsg(2, "Creating directory: " + name);
	        		boolean b = d.mkdir();
	        		if (!b) {
	        			JOptionPane.showMessageDialog(frameObj,"Could not create directory: " + name + "\n"); 
	        			Out.PrtError("Count not make directory: " + name);
	        			return false;
	        		}
	        }
	        return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems creating directories");}
		return false;
	}
	private void prtMemError(OutOfMemoryError e, String msg) {
		TimeHelpers.printMemoryUsed("*** Memory error - ");
		System.err.println("--- Increase memory in the 'runAS' script and re-run");
		ErrorReport.die(e, msg);
	}
	private String targetUpDir=null;
	private ASFrame frameObj=null;
	
	private String [] taxoIDs;
	private int index=0;
}
