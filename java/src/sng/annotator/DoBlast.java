package sng.annotator;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

import sng.database.Globals;
import sng.dataholders.BlastHitData;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileHelpers;
import util.file.FileVerify;
import util.methods.BlastRun;
import util.methods.ErrorReport;
import util.methods.TimeHelpers;
import util.methods.BlastArgs;
import util.methods.Out;

/* 
 * There will only be one DoBlast object - all blast/diamond for runSTCW are done here.
 * Populated by CfgAnno from sTCW.cfg (after saved by ManagerData)
 */
public class DoBlast {
	private final String seqAAFile = 		"seqAA.fa"; 
	private final String seqNTFile = 		"seqNT.fa"; 
	private final String orfFile =   		"orfSeqAA.fa";
	private final String blastnSuffix =  	"self_blastn.tab";
	private final String tblastxSuffix = 	"self_tblastx.tab";
	private final String blastpDiaSuffix =  "self_blastp.dmnd.tab";
	private final String blastpSuffix =  	"self_blastp.tab";
	
	final int maxHitIDlen = 30; // hard coded in Schema.java
	final boolean DOPROMPT=true;
	private boolean noPrompt=false;
	private boolean checkForPrompt=false;
	
	public void setMainObj(DoUniProt db, CoreAnno a, CoreDB s, String dbID) {
		sqlObj = s;
		isAAstcw = sqlObj.isAAtcw();
	}
	
	// called from CoreAnno and runSTCWMain to get state
	public boolean doPairs() { 
		if (doSelfBlastn || doSelfTblastx || doSelfBlastp) return true;
		return false;
	}
	public int numDB() { return dbInfo.size(); }
	
	
	/*********************************************************
	 * runSTCWMain - called first
	 * Checks all supplied files and sets dbType, isProtein and add Blast program type to ARGs
	 * Checks for tab blast files and ask user whether to use the existing file
	 */
	public boolean testFilesAndcreateOutFiles() {
		Out.Print("\nChecking annoDB files");
		
		if (!testDatabaseTable()) return false; // check to see if the databases have been processed before
	
		// AnnoDB: the user may enter the tab file name. 
		// Pairs:  the user CANNOT enter the tab file name, but it may exists and will be reused
		int cntErr=0; 
		doDBblast = false;  
		boolean doDBload = false;
		
	// AnnoDB check user supplied tab files and dbfile. Also sets dbType. 
		for (int i=0; i < dbInfo.size(); i++) {
			if (dbInfo.get(i).doBlast) { // Blast options selected
				if (!Globals.hasVal(dbInfo.get(i).DBfastaFile))
					Out.die("FASTA file name must exist for DB#" + i);
				
				// fasta file
				boolean found=false;
			 	File f = new File(dbInfo.get(i).DBfastaFile);
			 	if (f.isFile() && f.exists()) {
			 		found=true; 
			 		dbInfo.get(i).fileSize = f.length();
			 	}
			 	else {
			 		f = new File(dbInfo.get(i).DBfastaFile + ".gz");
			 		if (f.isFile() && f.exists()) {
			 			found=true; 
				 		dbInfo.get(i).fileSize = f.length();
				 		dbInfo.get(i).DBfastaFile += ".gz";
			 		}
			 	}
				if (!found) {
					Out.PrtErr("File does not exist: " + dbInfo.get(i).DBfastaFile);
					cntErr++; 
				}
				else { 
					if (testDBfastaFileAndSetSearchPgm(i)) doDBblast = true; // Prints blast message
					else cntErr++;
				}
			}
			else { // load tab file 
				String label = "DB#"+(i+1);
				doDBload=true;
				
				boolean b = false;
				if (testExistFile(dbInfo.get(i).tabularFile)) {
					if (testTabularFile(label, dbInfo.get(i).tabularFile, i, DOPROMPT)) {
						Out.PrtSpMsg(1, label + " Load file " + FileHelpers.removeRootPath(dbInfo.get(i).tabularFile));
						b = true;
					}
				}
				if (b) { // optional DB file
					if (Globals.hasVal(dbInfo.get(i).DBfastaFile)) {
						if (FileHelpers.fileExists(dbInfo.get(i).DBfastaFile)) {
							if (!testDBfastaFileAndSetSearchPgm(i)) b = false;
							else if (!testFastaLtBlastDates(i, DOPROMPT)) b = false;
						}
						else {
							b = false;
							Out.PrtErr("DBfasta does not exist " + dbInfo.get(i).DBfastaFile);
						}
					}
				}
				if (!b) cntErr++;
			}
		}
	
	// Similarity (tab file is named here and used in runSelfBlast, fastaFile is name again in runSelfBlast)
		String fastaFile = FileHelpers.removeRootPath(allFilePrefix + seqNTFile);
		if (doSelfBlastn) Out.PrtSpMsg(1, "Pairs blastn: " + fastaFile);
		
		if (doSelfTblastx) Out.PrtSpMsg(1, "Pairs tblastx: " + fastaFile);
		
		if (doSelfBlastp) {
			fastaFile = (isAAstcw) ? allFilePrefix + seqAAFile : allFilePrefix + orfFile;
			fastaFile = FileHelpers.removeRootPath(fastaFile);
			
			Out.PrtSpMsg(1, "Pairs " + selfBlastpPgm + ": " + fastaFile);
		}
		
		if (cntErr>0) {
			Out.PrtError("Failed checking files and databases\n");
			return false;
		}
				
		if (!(doSelfBlastn || doSelfTblastx|| doSelfBlastp || doDBblast || doDBload)) {
			Out.PrtSpMsg(0,"No searches to be executed or loaded - complete check\n");
			return true;
		}
		/********************************************************************************/
		/**** create blast output file names and see if they already exist; prompt user if exists ****/
		Out.Print("Checking for existing tab files");
		doDBblast = false;
		int cntRun=0, cntUse=0;
		cntErr=0;
		
		for (int ix=0; ix < dbInfo.size(); ix++) {
			if (! dbInfo.get(ix).doBlast) continue; // already have blast file
			
			String tt =  dbInfo.get(ix).typeTaxo;   // create blast file name
			int k=0;
			for (int j=0; j< ix; j++) 
				if (dbInfo.get(j).typeTaxo.equals(tt)) k++;			
			if (k>0) tt = tt + "_" + k;
					
			String fName = allFilePrefix + tt;
			if (dbInfo.get(ix).searchPgm.equals("diamond")) fName += Globalx.diamondSuffix;
			dbInfo.get(ix).tabularFile  = fName  + ".tab";
			
			if (dbInfo.get(ix).doBlast) { // CAS314 was not checking
				if (testUseExistingFile(ix, dbInfo.get(ix).tabularFile))  {
					dbInfo.get(ix).doBlast = false; 
					cntUse++;
				}
				else {
					doDBblast = true; 	
					cntRun++;
				}
			}
			else {
				if (testExistFile(dbInfo.get(ix).tabularFile)) cntUse++;
				else cntErr++;
			}
		}
		
		if (doSelfBlastn) {
			selfBlastnFile = allFilePrefix + blastnSuffix;
			
			if (testUseExistingFile(-1, selfBlastnFile)) {
				bExecBlastn=false;
				cntUse++;
			}
			else {
				bExecBlastn=true;
				cntRun++;
			}
		}
		if (doSelfTblastx) {
			selfTblastxFile = allFilePrefix + tblastxSuffix;
			
			if (testUseExistingFile(-2, selfTblastxFile)) {
				bExecTblastx=false;
				cntUse++;
			}
			else {
				bExecTblastx=true;
				cntRun++;
			}
		}
		if (doSelfBlastp) {
			if (selfBlastpPgm.equals("blast")) selfBlastpFile = allFilePrefix + blastpSuffix;
			else                               selfBlastpFile = allFilePrefix + blastpDiaSuffix;
			
			if (testUseExistingFile(-3, selfBlastpFile)) {
				bExecBlastp=false;
				cntUse++;
			}
			else {
				bExecBlastp=true;
				cntRun++;
			}
		}

		if (cntErr>0) {
			Out.PrtError("Failed checking tab files\n");
			return false;
		}
		Out.PrtSpMsg(0, "Check complete:   Run Search " + cntRun + "   Use existing " + cntUse + "\n");
		return true;
	}
		
	private boolean testExistFile(String theFile) {				
		if (!FileHelpers.fileExists(theFile)) {
			Out.PrtError("Cannot locate the file '" + theFile + "'");
			return false;
		} 
		return true;
	}
	
	private boolean testUseExistingFile(int ix, String file) {
		if (! FileHelpers.fileExists(file)) {
			file = file + ".gz"; 
			if (! FileHelpers.fileExists(file)) {
				Out.debug("Does not exist: " + file);
				return false;
			}
			else if (ix>=0) dbInfo.get(ix).tabularFile = file; // ix<0 selfBlast, file already assigned 
		}
		
		if (!checkForPrompt) {
			String msg = "At least one hit tab file exists for selected set. ";
			String [] x = new String [4]; 
			x[0]="u"; x[1]="p"; x[3]="e";
			msg += "\n Use current tab files [u], prompt on each tab file [p], exit[e]: ";
			
			String ans = runSTCWMain.promptQuestion(msg, x, 0); // Has its own NoPrompt, so may just return 'u'
			if (ans.equals("e")) Out.die("User terminated");
			if (ans.equals("u")) noPrompt=true;
			else {
				noPrompt=false;
				Out.prtToErr("");
			}
			checkForPrompt=true;
		}
		
		String msg="", msgS="Pairs";
		
		if (ix == -1) 		{msg = "Pairs blastn";}
		else if (ix == -2)  {msg = "Pairs tblastx";}
		else if (ix == -3)  {msg = "Pairs blastp";}
		else {
			msg =  "DB#" + (ix+1) + " " + dbInfo.get(ix).DBfastaNoPath;
			msgS = "DB#" + (ix+1);
		}
		
		if (!testTabularFile(msg, file, -1, noPrompt)) return false;

		File f = new File(file);
		String s = TimeHelpers.longDate(f.lastModified());
		boolean ans=true;
		
		if (noPrompt) {
			Out.PrtSpMsg(1, msgS + " load:  " + FileHelpers.removeRootPath(file) + "; Date: " + s);
		}
		else {
			Out.PrtSpMsg(1, msg);
			Out.PrtSpMsg(2, "Output exists:  " + FileHelpers.removeRootPath(file) + "; Date: " + s);
			ans = runSTCWMain.yesNo("Load this existing file [y] or perform new search [n] ");
			if (ans && ix >= 0) ans = testFastaLtBlastDates(ix, true);
		}
		return ans;
	}
	
	private boolean testFastaLtBlastDates(int ix, boolean prompt) {
		String fasta = dbInfo.get(ix).DBfastaFile;
		String blast = dbInfo.get(ix).tabularFile;
		
		File ff = new File(fasta);
		long fastaDate = ff.lastModified();
		
		File bf = new File(blast);
		long blastDate = bf.lastModified();
		
		if (fastaDate <= blastDate) return true;
		
		String sf = TimeHelpers.longDate(fastaDate);
		String sb = TimeHelpers.longDate(blastDate);
		String msg = "Date for DBfasta file is greater than blast output file\n" +
				"DBfasta: " + sf + " " + FileHelpers.removeRootPath(fasta) + "\n" +
				"Output : " + sb + " " + FileHelpers.removeRootPath(blast);
	
		if (!prompt) return false;
		
		Out.PrtWarn(msg);
		boolean ans = runSTCWMain.yesNo("Use this file anyway (y) or do blast (n): ");
		if (ans) return true;
		return false;
	}
	
    private boolean testTabularFile (String msg, String blastFile, int ix, boolean prompt) {
		try {
			if (ix == -1) return true; // not selfblast file
			
			if (!verObj.verify(FileC.bNoPrt, blastFile, FileC.fTAB)) return false; // CAS316 use FileVerify
			
			String line = verObj.getLine();
			
			LineParser lp = new LineParser();
			if (!lp.parseLine(line)) {
				Out.PrtError("Cannot parse hit file: " + blastFile + "\n   Line:" + line);
				return false;
			}
			String type = lp.getDBtype();
			
			if (!dbInfo.get(ix).dbType.equals("")) {// already added by testDBfile
				if (!dbInfo.get(ix).dbType.equals(type)) {
					if (prompt) Out.PrtError("DB and hit file are different types:" +
								"\n   " + type + ":" + blastFile + 
								"\n   "	+ dbInfo.get(ix).dbType + ":" + dbInfo.get(ix).DBfastaFile);
					return false;
				}
			}
			setTypeTaxo(ix, type);
			dbInfo.get(ix).isAAdb = true;
	
			return true;
		} 
		catch ( Exception err ) {
			ErrorReport.reportError(err, "Annotator - loading Hit Tab file");
			return false;
		} 
	}
	
	public boolean testDBfastaFileAndSetSearchPgm (int ix) {
		boolean dosearch =	  	dbInfo.get(ix).doBlast;
		String fileName = 		dbInfo.get(ix).DBfastaFile;
		String prtFileName = FileHelpers.removeRootPath(fileName);
		boolean isZIP = (fileName.endsWith(".gz")) ? true : false;
		
		try {
		// verify file and type
			if (!verObj.verify(FileC.bNoPrt, fileName, FileC.fFASTA)) return false;
			
			String line1 = verObj.getLine();
			boolean isAA = verObj.isProtein();
			
			LineParser lp = new LineParser ();
			if (! lp.parseLine(line1)) {
				Out.PrtErr(prtFileName + ": cannot parse DBfasta file\n   Line:" + line1);
				return false;
			}
				
			String dbtype =  lp.getDBtype();
			String linetype = "pr";
			if (!dbtype.equals("sp") && !dbtype.equals("tr")) {
				if (!isAA) linetype="nt"; 
			}
			if (dbtype.equals("")) dbtype=linetype;
			
		// determine action
			String action="";
			if (dbtype.equals("sp") || dbtype.equals("tr") || linetype.equals("pr")) {
				dbInfo.get(ix).isAAdb = true;
				if (isAAstcw) 	action = "blastp"; // AA-AA
				else 			action = "blastx"; // tr-NT-AA
				
				if (!Globals.hasVal(dbInfo.get(ix).searchPgm)) 
					dbInfo.get(ix).searchPgm =  BlastArgs.defPgm;
				
				if (!Globals.hasVal(dbInfo.get(ix).blastArgs)) 
					dbInfo.get(ix).blastArgs = BlastArgs.getArgsDB(dbInfo.get(ix).searchPgm, action);
				
				if (dosearch) {
					String x = "DB#" + (ix+1) + " " + dbInfo.get(ix).searchPgm;
					if 		(dbtype.equals("gi")) 	Out.PrtSpMsg(1, x + " GB AA: " + prtFileName);
					else if (dbtype.equals("sp")) 	Out.PrtSpMsg(1, x + " SP AA: " + prtFileName);
					else if (dbtype.equals("tr"))	Out.PrtSpMsg(1, x + " TR AA: " + prtFileName);
					else 							Out.PrtSpMsg(1, x + " pr AA: " + prtFileName);
				}
			}
			else if (dbtype.equals("nt") || linetype.equals("nt")){
				if (isAAstcw) {
					Out.PrtErr(prtFileName + ": cannot have a nucleotide annoDB with a protein sTCW");
					return false;
				}
				dbInfo.get(ix).isAAdb = false;
				action =  "blastn";
				
				if (!Globals.hasVal(dbInfo.get(ix).searchPgm) || !dbInfo.get(ix).searchPgm.equals("blast")) {
					if (dbInfo.get(ix).searchPgm.equals("diamond"))
						Out.PrtWarn("Cannot use " + dbInfo.get(ix).searchPgm + " with nucleotide annoDB -- using blast");
					dbInfo.get(ix).blastArgs = "-";
					dbInfo.get(ix).searchPgm = "blast";
				}
				if (!Globals.hasVal(dbInfo.get(ix).blastArgs)) 
					dbInfo.get(ix).blastArgs = BlastArgs.getArgsDB(dbInfo.get(ix).searchPgm, action);
				
				if (dosearch) {
					String x = "DB#" + (ix+1) + " " + action;
					if (dbtype.equals("gi")) 	Out.PrtSpMsg(1, x + " GB NT: " + prtFileName);
					else 						Out.PrtSpMsg(1, x + " nt NT: " + prtFileName);
				}
			}
			else {
				Out.PrtErr("Could not determine type of file for " + prtFileName);
				return false;
			}
			if (isZIP && !dbInfo.get(ix).searchPgm.equals("diamond")) { 
				Out.PrtErr("The file " + prtFileName + " is compressed, which will fail with " + dbInfo.get(ix).searchPgm);
				return false;
			}
			if (Globals.hasVal(dbInfo.get(ix).dbType)) { // already added by testBlastFile
				if (!dbInfo.get(ix).dbType.equals(dbtype)) {
					Out.PrtErr("DB and hit file are different types:" +
						"\n   " + dbtype + ":" + dbInfo.get(ix).DBfastaFile + 
						"\n   "	+ dbInfo.get(ix).dbType + ":" + dbInfo.get(ix).tabularFile);
					return false;
				}
			}
			else setTypeTaxo(ix, dbtype); 
				
			dbInfo.get(ix).exec = action;
		
			return true;
		} 
		catch ( Exception err ) {
			ErrorReport.reportError(err, "Annotator - determining type of file " + prtFileName);
			return false;
		} 
	}
	
	private boolean testDatabaseTable () {
		// pja_database is not cleared until after all checks
		if (runSTCWMain.bDelAnno) return true;
		try {
			ArrayList <String> paths = sqlObj.loadDatabases();
			for (int i=0; i < paths.size(); i++) {
				for (int j=0; j < dbInfo.size(); j++) {
					if (paths.get(i).equals(dbInfo.get(j).tabularFile) ||
						paths.get(i).equals(dbInfo.get(j).DBfastaFile)) 
					{
						String str = "DB#" + dbInfo.get(j).dbNum + " The database " + 
								FileHelpers.removeRootPath(paths.get(i)) + 
							"\n      has been processed previously. Continue?";
						boolean ans = runSTCWMain.yesNo(str);
						if (ans==false) return false;
					}					
				}
			}
			return true;
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "checking existing annoDB files in the sTCW database");
			return false;
		}
	}
	/*************************************************
	 * Self blast
	 *******************************************/
	public boolean runSelfBlast(String action) 
	{
		boolean isORF=false;
		// true just means Blast did not fail. It doesn't mean the option was selected.
		if (action.equals("blastn")  && !bExecBlastn) {
			if (doSelfBlastn) Out.PrtSpMsg(2, "Use existing blastn: "  + selfBlastnFile);
			return true;
		}
		if (action.equals("tblastx") && !bExecTblastx) {
			if (doSelfTblastx) Out.PrtSpMsg(2, "Use existing tblastx: " + selfTblastxFile);
			return true; 
		}
		if (action.equals("blastp")  && !bExecBlastp) {
			if (doSelfBlastp) Out.PrtSpMsg(2, "Use existing blastp: " + selfBlastpFile);
			return true;
		}
		/********************** Run Blast *****************************/
		long time = Out.getTime();
		
		int ncpu = runSTCWMain.getCPUs();
	    boolean isAAtcw=sqlObj.isAAtcw();
	    
	    String inFile="", tabFile="", args="", pgm="blast";
	    boolean isAA = false;
		if (action.equals("tblastx")) {
			tabFile = selfTblastxFile;
			args    = selfTblastxArgs; 
		    
			Out.PrtSpMsg(1, "Running pairs tblastx");
		}
		else if (action.equals("blastn")) {
			tabFile = selfBlastnFile;
			args    = selfBlastnArgs;
			
			Out.PrtSpMsg(1, "Running pairs blastn ");
		}	
		else if (action.equals("blastp")) {
			tabFile = selfBlastpFile;
			args    = selfBlastpArgs;
			
	        pgm 	= selfBlastpPgm;
	        if (!isAAtcw) isORF=true;
	        isAA=true;
			
			Out.PrtSpMsg(1, "Running Pairs blastp with " + pgm);
		}
		else Out.die("No action " + action);
		
		if (!writeSeqFile(isORF)) return false;
		
		String inFilePath = (isORF) ? orfFilePath : seqFilePath;
		 
		try {
			inFile =  (new File(inFilePath)).getAbsolutePath();
			tabFile = (new File(tabFile)).getAbsolutePath();	
			
			boolean rc = BlastRun.run(ncpu, pgm, action, args, isAA, inFile, isAA, inFile, tabFile);
			
			Out.PrtSpMsgTime(1, "Complete " + action, time);

			sqlObj.reset(); // times out during blast
			return rc;
		} catch (Exception e) {
			ErrorReport.reportError(e, "Executing blast");
			return false;
		}
	}
	/*************************************************
	 * Execute all unitrans against DBs
	 *******************************************/
	public String getRunDbBlastFile(int i) {
		
		if (dbInfo.get(i).doBlast) {
			if (!runDBblast(i)) return null;
			try {
				sqlObj.reset(); // times out during blast
			}
			catch (Exception e) {} // dies if fails
		}
		return dbInfo.get(i).tabularFile;
	}
	private boolean runDBblast(int ix)
	{
		if (!BlastArgs.searchPgmExists()) {
			Out.PrtErr("No search program detected or identified in HOSTS.cfg");
			return false;
		}
		// creates file of sequences on the first call to this
		if (! writeSeqFile(false)) return false;
				
		String dbFile =  dbInfo.get(ix).DBfastaFile;
		String blastParams = dbInfo.get(ix).blastArgs;
		String tabFile = dbInfo.get(ix).tabularFile;
		String action =  dbInfo.get(ix).exec;		// blastn, blastp, blastx
		String pgm =     dbInfo.get(ix).searchPgm;   // blast, diamond
		boolean isAAdb = dbInfo.get(ix).isAAdb;
		boolean isAAtcw= sqlObj.isAAtcw();
		int ncpu = runSTCWMain.getCPUs();
    	long startTime = Out.getTime();
    	
    	String file = dbInfo.get(ix).DBfastaNoPath;
  
		Out.PrtSpMsg(2, "DB#" + dbInfo.get(ix).dbNum + " " + file  + " " +
				FileHelpers.getSize(dbInfo.get(ix).fileSize) + "       " + TimeHelpers.getDate());

		// Execute........
		dbFile = (new File(dbFile)).getAbsolutePath();
		String inFile = (new File(seqFilePath)).getAbsolutePath();	
		tabFile = (new File(tabFile)).getAbsolutePath();	
		
		boolean rc = BlastRun.run(ncpu, pgm, action, blastParams, isAAdb, dbFile, isAAtcw, inFile, tabFile);
		
		Out.PrtSpMsgTime(3, "Complete " + pgm, startTime);
		return rc;
	}
	
	// executed before first DB blast and then whenever there is a JPAVE_unitrans_subset_n parameter
	private boolean writeSeqFile(boolean isORF){	
		File blastDir = new File(seqBlastDir);
		if (!blastDir.exists()) {
			Out.PrtSpMsg(3,"Creating " + seqBlastDir + " directory");
			if (!blastDir.mkdir()) {
				Out.PrtErr("Failed to create " + seqBlastDir + " directory " + blastDir);
				return false;
			}
		}
		
		int cnt;
		if (isORF) { // CAS314
			orfFilePath = allFilePrefix + orfFile;
			if (FileHelpers.fileExists(orfFilePath)) return true;
			
			Out.PrtSpMsg(3, "Create translated ORF file: " + FileHelpers.removeRootPath(orfFilePath));
			
			cnt = sqlObj.writeOrfFile(new File(orfFilePath)); 
		}
		else {
			seqFilePath = (sqlObj.isAAtcw()) ? allFilePrefix + seqAAFile : allFilePrefix + seqNTFile;
			if (FileHelpers.fileExists(seqFilePath)) return true;
			
			Out.PrtSpMsg(3, "Create sequence file: " + FileHelpers.removeRootPath(seqFilePath));
		
			cnt = sqlObj.writeSeqFile(new File(seqFilePath)); 
		}
		Out.PrtSpMsg(4, "Wrote " + cnt + " sequence records");		
		return true;
	}
	
	/*
	 *  XXX read megablast or tblaxtx for the pairwise comparisons
	 *  If both AA and NT hits, only the AA scores are saved
	 */
	public int addAllPairsFromBlastFile(String delim, 
			 HashMap <String, BlastHitData> pairsHash,
			 String fileBlast, String type) throws Exception 
	{  	
		String strLine = null;
		String strSeq2, strSeq1, keyR, key;
		BlastHitData hitData;
		
		boolean isAA = (type.equals(Globalx.typeNT)) ? false : true;
		
		try {
			BufferedReader reader = FileHelpers.openGZIP(fileBlast);
			if (reader==null) return 0;

			int goodHits = 0, cntRead = 0, cntBad = 0;

			while ((strLine = reader.readLine()) != null) {
				if (strLine.length() == 0 || strLine.charAt(0) == '#') continue; 

				cntRead++;
				String strFields[] = strLine.split("\t");
				if (strFields.length<2) {
					Out.PrtErr("Bad Line at " + cntRead + " : " + strLine);
					cntBad++;
					if (cntBad>50) {
						Out.PrtErr("Too many bad lines -- exiting");
						return 0;
					}
					continue;
				}
				strSeq1 = cleanID(strFields[0].trim());
				strSeq2 = cleanID(strFields[1].trim());
				
				if (strSeq1.equals(strSeq2)) continue; // Ignore self-hit
		
				// key must be same order as blast query-hit
				key =  strSeq1 + delim + strSeq2;
				keyR = strSeq2 + delim + strSeq1;
						
      			if (!pairsHash.containsKey(keyR) && !pairsHash.containsKey(key)) {
      				hitData = new BlastHitData(isAA, strLine);
      				hitData.setPairHitType(type);
         			pairsHash.put(key, hitData);
         			goodHits++; 
      			}
      			else { // CAS314 - to update the pairHitType for all possible hits (gets called twice per typeX)
      				String k= (pairsHash.containsKey(key)) ? key : keyR;
      				hitData =  pairsHash.get(k);
      				hitData.setPairHitType(type);
      			}
 
				if (cntRead % 10000 == 0) 
					Out.r("Load " + goodHits + " hits " + cntRead + " read");
			} 
			reader.close();
		
			return goodHits;
			
		} catch (Exception e) {
			Out.PrtErr("Input Line = '" + strLine + "'");
			ErrorReport.reportError(e, "Failed on reading all pairs from hit file.");
			return 0;
		} 
	}
	
	static private String cleanID(String str) {
		if (str.startsWith("lcl|"))
			return str.substring(4);
		else
			return str;
	}
	
	/**************************************************************************/
	public boolean doAnyBlast () {
		if ((doSelfBlastn && bExecBlastn) || 
			(doSelfTblastx && bExecTblastx) || 
			(doSelfBlastp && bExecBlastp)) return true;
		
		for (int i=0; i< dbInfo.size(); i++) 
			if (dbInfo.get(i).doBlast) return true;
		return false;
	}

	public String getSelfBlastnFile() {return selfBlastnFile;}
	public String getSelfTblastxFile(){return selfTblastxFile;}
	public String getSelfBlastpFile() {return selfBlastpFile;}
	
	public String getDBfastaFile(int i) {return dbInfo.get(i).DBfastaFile;}
	public String getDBfastaNoPath(int i) {return dbInfo.get(i).DBfastaNoPath;}
	public String getDBtype(int i) {return dbInfo.get(i).dbType;}
	public String getDBtaxo(int i) {return dbInfo.get(i).dbTaxo;}
	public String getDBparams(int i) { // for overview
		if (dbInfo.get(i).doBlast) {
			String a = dbInfo.get(i).blastArgs;
			if (a.contains("--tmpDir")) a = a.substring(0, a.indexOf("--tmpDir"));
			return dbInfo.get(i).searchPgm + "  " + a;
		}
		else {
			String a = dbInfo.get(i).tabularFile;
			if (a.contains("/")) a = a.substring(a.lastIndexOf("/")+1);
			return "Load " + a;
		}
	}
	// also in BlastHitData
	
	private void setTypeTaxo(int i, String t) {
		dbInfo.get(i).dbType = t;
		String capType = 		dbInfo.get(i).dbType.toUpperCase();
		String taxo = 			dbInfo.get(i).dbTaxo; 
		if (taxo.length() < 3) 	dbInfo.get(i).typeTaxo = capType + taxo;
		else 					dbInfo.get(i).typeTaxo = capType + dbInfo.get(i).dbTaxo.substring(0, 3);
	}
	public String getTypeTaxo(int i) {
		return dbInfo.get(i).typeTaxo;
	}
	public int getDbNum(int i) 		{return dbInfo.get(i).dbNum;}
	public boolean isProtein(int i) {return dbInfo.get(i).isAAdb;}
	public String getDBdate(int i) 	{return dbInfo.get(i).dbDate;}
	
	public void setSeqBlastPrefix(String p) {allFilePrefix = p;}
	public void setSeqBlastDir(String p)    {seqBlastDir = p;}
	
	// the arg set may be defaults
	public void setSelfBlastnArgs(boolean b,String s)  {doSelfBlastn=b;  selfBlastnArgs=s;}	
	public void setSelfTblastxArgs(boolean b,String s) {doSelfTblastx=b; selfTblastxArgs=s;}
	public void setSelfBlastpArgs(boolean b,String s)  {doSelfBlastp=b;  selfBlastpArgs=s;}	
	public void setSelfBlastpPgm(String s)  		   {selfBlastpPgm=s;}	
	
	public void setCheckForPrompt() {checkForPrompt=true;} // Already said to prompt in CoreMain
	public void setNoPrompt() { noPrompt=true;}
	
	public void makeDB(int ix, String blast, String fasta, String args, String taxo, 
			String dbdate, String pgm) {
		DB d = new DB(ix, blast, fasta, args, taxo, dbdate, pgm);
		dbInfo.add(d);
	}
	private class DB {
		DB (int ix, String blast, String fasta, String args,  String taxo,  
				String dbdate,  String pgm) {
			dbNum = ix;
			tabularFile = blast; // if value, already tested for existance
			DBfastaFile = fasta; // should exist even if there is a blast file -- for description & sequence
			DBfastaNoPath = fasta.substring(fasta.lastIndexOf("/")+1);
			blastArgs = args;  // if "-", gets set when testing file
			dbTaxo = taxo;
			dbDate = dbdate;
			if (!Globals.hasVal(tabularFile)) doBlast=true;
			searchPgm=pgm;
		}
		int dbNum=0; 		// from sTCW.cfg '_n' 
		String tabularFile = "";
		String DBfastaFile = "";
		
		String dbType=""; // sp/tr/nt -- gets set on file check
		String dbTaxo="";   // plant, etc
		String typeTaxo="";
		String DBfastaNoPath="";
		String dbDate=null;
		boolean isAAdb = false, doBlast = false;
		
		String searchPgm=BlastArgs.defPgm;  // diamond
		String exec=""; 		// blastn for nt -- computed in testDBfastaFile
		String blastArgs="";	// if none, computed in testDBfastaFile
		
		long fileSize=0;
	}
	private CoreDB sqlObj = null;
	private boolean isAAstcw = false;
	
	private String allFilePrefix = null; // hitResults/<dbID> - check for complete blasts
	private String seqBlastDir = null, seqFilePath = null, orfFilePath = null;
	
	public boolean doSelfBlastn = false, bExecBlastn = false;
    private String selfBlastnFile = null, selfBlastnArgs = null;
	
	public boolean doSelfTblastx = false, bExecTblastx = false;
	private String selfTblastxArgs = null, selfTblastxFile = null; // blast output file
	
	public boolean doSelfBlastp = false, bExecBlastp = false;
	private String selfBlastpFile = null; // blast output file
	private String selfBlastpArgs = null, selfBlastpPgm = null;
			
	private boolean doDBblast = false;
	private ArrayList<DB> dbInfo = new ArrayList<DB>();
	
	private FileVerify verObj = new FileVerify();
}
