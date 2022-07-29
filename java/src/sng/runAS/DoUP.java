package sng.runAS;
/*********************************************************
 * DoUP perform file handling for downloading taxonomic and full uniprot
 */
import java.io.BufferedReader;
import java.io.File;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import util.database.Globalx;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.TimeHelpers;

public class DoUP {
	private final String upTaxURL = GlobalAS.upTaxURL;
	private final String upFullURL = GlobalAS.upFullURL;
	
	private final String fullTaxo = GlobalAS.fullTaxo;					// full
	private final String SP = Globalx.SP, TR=Globalx.TR;				// sp, tr
	
	private final Pattern fasta =  Pattern.compile(">(\\S+)");
	
	public DoUP (ASFrame asf) {frameObj=asf;} 
	
	/**************************************************************
	 * XXX Taxonomic
	 */
	public void xTaxo(Vector <String> spTaxo, Vector <String> trTaxo, 
			Vector <Boolean> spHasDat, Vector <Boolean> trHasDat, String dir) {
		try {
			Out.PrtDateMsg("\nStart Taxonomic UniProt");
			targetUpDir = dir;
			if (!mkDirTaxo(spTaxo, trTaxo)) return;
			
			long startTime = Out.getTime();
			
			if (spTaxo.size()>0) {
				long downTime = Out.getTime(); //CAS331 add sub-times
				Out.PrtSpTimeMsg(1, "Download SwissProt files");
				for (int i=0; i<spTaxo.size(); i++) {
					if (!spHasDat.get(i)) {
						String f = spTaxo.get(i);
						if (!runDownload(SP, f, f)) return;	
					}
				}
				Out.PrtSpMsgTime(1,"Complete SwissProt download", downTime);
			}
			
			if (trTaxo.size()>0) {
				long downTime = Out.getTime();
				Out.PrtSpMsg(1,"");
				Out.PrtSpTimeMsg(1, "Download TrEMBL files");
				for (int i=0; i<trTaxo.size(); i++) {
					if (!trHasDat.get(i)) {
						String f = trTaxo.get(i);
						if (!runDownload(TR, f, f)) return;	
					}
				}
				Out.PrtSpMsgTime(1, "Complete TrEMBL download", downTime);
			}
			
			long createTime=Out.getTime();
			Out.PrtSpMsg(1,"");
			Out.PrtSpTimeMsg(1, "Create FASTA files");
			DoUPdat datObj = new DoUPdat(frameObj);
			GlobalAS fileObj = new GlobalAS(targetUpDir);
			
			for (String taxo : spTaxo) {
				String spDir =    fileObj.mkNameDir(SP,taxo);	  	// targetDir/sp_taxo
				String spOutFa =  fileObj.mkNameFa(SP,taxo); 		// uniprot_sprot + "_" + tax + faSuffix;
				
				String spFullDat = fileObj.mkFullDatExists(SP, taxo, taxo);
				
				if (!datObj.dat2fasta(SP, spDir, spFullDat, spOutFa, null)) return;
			}
			for (String taxo : trTaxo) {
				String trDir = fileObj.mkNameDir(TR,taxo);	  	// targetDir/tr_taxo
				String trOutFa =  fileObj.mkNameFa(TR,taxo); 	// uniprot_trembl + "_" + tax + faSuffix;
				String trFullDat = fileObj.mkFullDatExists(TR, taxo, taxo);
				
				if (!datObj.dat2fasta(TR, trDir, trFullDat, trOutFa, null)) return;
			}
			
			Out.PrtSpMsgTime(1, "Complete create files", createTime);
			Out.PrtMsgTimeMem("Complete Taxonomic UniProt", startTime);
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems getting file");}
	}
	
	/*******************************************************
	 * XXX Full
	 */
	public boolean xFullDown( boolean noSPdat, boolean noTRdat, String dir) {
		Out.PrtDateMsg("\nDownload Full UniProt");
		long startTime = Out.getTime();
		
		targetUpDir = dir;
		Out.PrtSpMsg(1, "Check Directories ");
		if (!mkCheckDir(targetUpDir)) return false;
		
		GlobalAS fileObj = new GlobalAS(targetUpDir);
		
		if (noSPdat) {
			Out.PrtSpTimeMsg(1, "Download SwissProt");
			if (!mkCheckDir(fileObj.mkNameDir(SP, fullTaxo))) return false;
			if (!runDownload(SP, "", fullTaxo)) return false;	
		}
		if (noTRdat) {
			Out.PrtSpTimeMsg(1, "Download TrEMBL");
			if (!mkCheckDir(fileObj.mkNameDir(TR, fullTaxo))) return false;
			if (!runDownload(TR,  "", fullTaxo)) return false;	
		}
		Out.PrtSpMsgTime(0, "Complete Full Download", startTime);
		return true;
	}
	public void xFullFasta(	String spSuffix, HashSet <String> spTaxoSet, 
							String trSuffix, HashSet <String> trTaxoSet, String upDir) {
		Out.PrtDateMsg("\nCreate Full UniProt Fasta");
		targetUpDir = upDir;
		GlobalAS fileObj = new GlobalAS(targetUpDir);
		
		long startTime = Out.getTime();
		
		if (spTaxoSet!=null) {
			HashSet <String> spHitIDs = fullReadFastas(SP, spTaxoSet); // get all hitIDs from selected .fasta
			
			String fullDir = 	 fileObj.mkNameDir(SP, fullTaxo);		// sp_full
			String outFaFile =	 fileObj.mkNameFa(SP, spSuffix);	    // uniprot_sprot_spSuffix.fasta
			String fullDatFile = fileObj.mkFullDatExists(SP, fullTaxo, "");
			
			DoUPdat spObj = new DoUPdat(frameObj);
			if (!spObj.dat2fasta(SP, fullDir, fullDatFile, outFaFile, spHitIDs)) return; 
		}
		if (trTaxoSet!=null) {
			HashSet <String> trHitIDs = fullReadFastas(TR, trTaxoSet);
			
			String fullDir = fileObj.mkNameDir(TR, fullTaxo);  		// tr_full
			String outFaFile=fileObj.mkNameFa(TR, trSuffix);	// uniprot_trembl_trSuffix.fasta
			
			String fullDatFile = fileObj.mkFullDatExists(TR, fullTaxo, "");			// uniprot_trembl.dat.gz
			
			DoUPdat trObj = new DoUPdat(frameObj);
			if (!trObj.dat2fasta(TR, fullDir, fullDatFile, outFaFile, trHitIDs)) return;
		}
		Out.PrtSpMsgTimeMem(0, "Complete Full UniProt fasta", startTime);
	}
	
	/***************************************************
	 * action=0; count hitIDs in taxonomic fasta file
	 * Allocate string array
	 * action=1; set hitIDs from taxonomic fast
	 */
	private HashSet <String> fullReadFastas(String type, HashSet <String> taxoSet) {
		long startTime = Out.getTime();
		try {
			Out.PrtSpMsg(2, "Create " + type.toUpperCase() + " hitID list from selected fasta files");
			HashSet <String> hitIDs = new HashSet <String> ();
			
			GlobalAS fileObj = new GlobalAS(targetUpDir);
			
			for (String taxo : taxoSet) {
				String fullname = fileObj.mkFullFaExists(type, taxo);	
				
				if (fullname==null) 
					Out.PrtErr("No valid .fasta or fasta.fz file for " + type + "_" + taxo);
				else 
					fullReadFastaFile(new File(fullname), hitIDs);
			}
			
			Out.PrtSpCntMsg(3, hitIDs.size(), "sequences will not be written to subset fasta file");
			Out.PrtSpMsgTime(2, "Complete " + type.toUpperCase() + " hitID list", startTime);
			return hitIDs;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error reading file");}
		catch (OutOfMemoryError e) {ErrorReport.prtReport(e, "Memory error reading file ");}
		return null;
	}
	// read hitIDs into HashSet
	// " >sp|Q6V4H0|10HGO_CATRO 8-hyxxxxxxxxxx xxxxxxxx xxxxxx";
	private boolean fullReadFastaFile(File faFH, HashSet <String> hitIDs) { 
		String line="";
		int cnt=0, cntErr=0, cntFound=0;
		Matcher x;
		String fn = faFH.getName();
		
		try {		
			BufferedReader inFH = FileHelpers.openGZIP(faFH.getAbsolutePath()); // CAS315
			if (inFH==null) Out.die("Cannot continue");
		
			while ((line = inFH.readLine()) != null) {
				if (!line.startsWith(">")) continue;
				line = line.trim();
				x = fasta.matcher(line);
				if (!x.find()) Out.PrtErr("Parse error: " + line);
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
					hitIDs.add(tok[2]);
					cntFound++;
					
					if (cnt%100000==0) Out.r("read " + cntFound + " in " + fn);
				}
			}
			inFH.close();
			Out.rClear();
			Out.PrtSpCntMsg(3, cntFound, "IDs in " + fn);
		
			return true;
		}
		catch (Exception e) {
			String msg = "Reading #" + cnt + " " + fn + "                 \n" + "Error parsing: " + line;
			ErrorReport.prtReport(e, msg);
		}
		catch (OutOfMemoryError e) { prtMemError(e, "reading taxonomic databases");}
		return false;
	}
	
	/*********************************************************
	 * XXX Utilities
	 */
	private boolean runDownload(String type,  String tax, String dir) {
		try {
			boolean rc=false;
			GlobalAS fileObj = new GlobalAS(targetUpDir);
			
			String inFile = 	fileObj.mkNameDatGz(type, tax);
			String outPath = 	fileObj.mkNameDir(type, dir) + inFile;
			
			if (tax=="") 	rc = ASMain.ioURL(upFullURL, inFile, outPath);
			else 			rc = ASMain.ioURL(upTaxURL, inFile, outPath);
			
			return rc;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Problems getting file");}
		return false;
	}
	/* Make taxo directories */
	private boolean mkDirTaxo(Vector <String> spFiles, Vector <String> trFiles) {
		try {
			Out.PrtSpMsg(1, "Check Directories");
	        
			GlobalAS fileObj = new GlobalAS(targetUpDir);
			
			if (!mkCheckDir(targetUpDir)) return false;
			
			for (String f : spFiles) {
				if (!mkCheckDir(fileObj.mkNameDir(SP, f))) return false;
			}
			for (String f : trFiles) {
				if (!mkCheckDir(fileObj.mkNameDir(TR, f))) return false;
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
}
