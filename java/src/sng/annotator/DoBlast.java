package sng.annotator;
/* 
 * There will only be one DoBlast object 
 */
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;


import sng.database.Globals;
import sng.dataholders.BlastHitData;
import util.database.Globalx;
import util.methods.BlastRun;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.TimeHelpers;
import util.methods.BlastArgs;
import util.methods.Out;

public class DoBlast {
	final String seqFileSuffix = Globals.seqFile;
	final String selfSuffix = "blast_self.tab";
	final String tselfSuffix = "blast_tself.tab";
	
	final int maxHitIDlen = 30; // hard coded in Schema.java
	final boolean DOPROMPT=true;
	private boolean noPrompt=false;
	private boolean checkForPrompt=false;
	
	public void setMainObj(DoUniProt db, CoreAnno a, CoreDB s) {sqlObj = s;}
	
	// called from CoreMain to get state
	public boolean doPairs() { 
		if (selfBlastFile != null && selfBlastFile!="") return true;
		if (tselfBlastFile != null && selfBlastFile!="") return true;
		return false;
	}
	public int numDB() { return dbInfo.size(); }
	
	/*********************************************************
	 * Checks all supplied files and sets dbType, isProtein and add Blast program type to ARGs
	 * Checks for existing blast and ask user whether to use the existing file
	 * 		If ReUse, only print warnings
	 */
	public boolean testFilesAndcreateOutFiles() {
		Out.Print("\nChecking blast and annoDB fasta files");

		// check to see if the databases have been processed before
		if (!testDatabaseTable()) return false;
	
		// check user supplied blast files and dbfile. Also sets dbType
		int cntErr=0; 
		doDBblast = false;  
		
		for (int i=0; i < dbInfo.size(); i++) {
			if (dbInfo.get(i).doBlast) {
				if (!testExistString(dbInfo.get(i).DBfastaFile))
					Out.die("TCW error: fasta name must exist for DB#" + i);
				
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
					Out.PrtError("File does not exist: " + dbInfo.get(i).DBfastaFile);
					cntErr++; 
				}
				else {
					if (testDBfastaFileAndSetSearchPgm(i)) doDBblast = true; // Prints file name
					else cntErr++;
				}
			}
			else {
				if (!testExistString(dbInfo.get(i).tabularFile))
					Out.die("TCW error: hit file must exist for DB#" + i);
				
				String label = "DB#"+i;
				boolean b = false;
				if (testExistFile(dbInfo.get(i).tabularFile)) 
					if (testTabularFile(label, dbInfo.get(i).tabularFile, i, DOPROMPT)) {
						Out.PrtSpMsg(1, (i+1) + ". Load file " + 
								FileHelpers.removeCurrentPath(dbInfo.get(i).tabularFile));
						b = true;
					}
				if (b) { // optional DB file
					if (testExistString(dbInfo.get(i).DBfastaFile)) {
						if (FileHelpers.fileExists(dbInfo.get(i).DBfastaFile)) {
							if (!testDBfastaFileAndSetSearchPgm(i)) b = false;
							else if (!testFastaLtBlastDates(i, DOPROMPT)) b = false;
						}
						else {
							b = false;
							Out.PrtError("DBfasta does not exist " + dbInfo.get(i).DBfastaFile);
						}
					}
				}
				if (!b) cntErr++;
			}
		}
		
		if (!doSelfBlast && testExistString(selfBlastFile)) {
			if (!testExistFile(selfBlastFile)) cntErr++;
			else if (!testTabularFile("Selfblast", selfBlastFile, -1, DOPROMPT)) cntErr++;
			else Out.PrtSpMsg(2, "Load pair file " + FileHelpers.removeCurrentPath(selfBlastFile));
		}
		else if (doSelfBlast) Out.PrtSpMsg(1, "Create pairs self blast");
		if (!doTSelfBlast && testExistString(tselfBlastFile)) {
			if (!testExistFile(tselfBlastFile)) cntErr++; 
			else if (!testTabularFile("TselfBlast", tselfBlastFile, -1, DOPROMPT) ) cntErr++;
			else Out.PrtSpMsg(2, "Load pair file " + FileHelpers.removeCurrentPath(tselfBlastFile));
		}
		else if (doTSelfBlast) Out.PrtSpMsg(1, "Create pairs translated self blast");
		
		if (cntErr>0) {
			Out.PrtError("Failed checking files and databases\n");
			return false;
		}
				
		if (!(doSelfBlast || doTSelfBlast|| doDBblast)) {
			Out.PrtSpMsg(0,"No searches to be executed - complete check files\n");
			return true;
		}
		
		/**** create blast output file names and see if they already exist ****/
		
		doDBblast = false;
		for (int ix=0; ix < dbInfo.size(); ix++) {
			if (! dbInfo.get(ix).doBlast) continue; // already have blast file
			
			// create blast file name
			String tt =  dbInfo.get(ix).typeTaxo;
			int k=0;
			for (int j=0; j< ix; j++) 
				if (dbInfo.get(j).typeTaxo.equals(tt)) k++;			
			if (k>0) tt = tt + "_" + k;
					
			String fName = seqBlastPrefix + tt;
			
			if (dbInfo.get(ix).searchPgm.equals("diamond")) 		fName += Globalx.diamondSuffix;
			else if (dbInfo.get(ix).searchPgm.equals("usearch")) fName += Globalx.usearchSuffix;
			
			dbInfo.get(ix).tabularFile  = fName  + ".tab";
			
			if (testUseExistingFile(ix, dbInfo.get(ix).tabularFile))  // exist and reuse?
				dbInfo.get(ix).doBlast = false;
			else doDBblast = true; 
		}
		
		if (doSelfBlast) {
			selfBlastFile = seqBlastPrefix + selfSuffix;
			if (testUseExistingFile(-1, selfBlastFile))  doSelfBlast = false;
		}
		if (doTSelfBlast) {
			tselfBlastFile = seqBlastPrefix + tselfSuffix;
			if (testUseExistingFile(-2, tselfBlastFile))  doTSelfBlast = false;
		}
		if (!(doSelfBlast || doTSelfBlast|| doDBblast))  // using existing files
			Out.PrtSpMsg(0,"No blasts to be executed - complete check files\n");
		else
			Out.PrtSpMsg(0,"Complete check files\n");
		return true;
	}
		
	private boolean testExistString(String theFile) {		
		if (theFile == null || theFile.equals("") || theFile.equals("-")) 
			return false;
		return true;
	}
	
	private boolean testExistFile(String theFile) {				
		if (!FileHelpers.fileExists(theFile)) {
			Out.PrtError(" Cannot locate the file '" + theFile + "'");
			return false;
		} 
		return true;
	}
	
	private boolean testUseExistingFile(int ix, String file) {
		if (! FileHelpers.fileExists(file)) {
			file = file + ".gz"; 
			if (! FileHelpers.fileExists(file)) return false;
			else dbInfo.get(ix).tabularFile = file;
		}
		
		if (!checkForPrompt) {
			String msg = "At least one hit file exists for selected set.";
			String [] x = new String [4]; 
			x[0]="u"; x[1]="p"; x[3]="e";
			msg += ". Enter [u/p/e]\n";
			msg += "  Use current hit files [u], prompt on each hit file [p], exit[e]: ";
    			String ans = runSTCWMain.promptQuestion(msg, x, 0);
    			if (ans.equals("e")) { 
    				Out.die("User terminated");
    			}
			if (ans.equals("u")) noPrompt=true;
			else noPrompt=false;
			checkForPrompt=true;
		}
		
		String msg="";
		if (ix == -1) msg = "Self blast";
		else if (ix == -2) msg = "Tself blast";
		else msg = "DB#" + (ix+1) + " " + dbInfo.get(ix).DBfastaNoPath;
		if (!testTabularFile(msg, file, -1, noPrompt)) return false;

		File f = new File(file);
		String s = TimeHelpers.longDate(f.lastModified());
		boolean ans=true;
		
		Out.PrtSpMsg(1, msg);
		
		if (noPrompt) {
			if (ix >= 0) Out.PrtSpMsg(2, "Use tab file:  " + FileHelpers.removeCurrentPath(file) + "; Dated: " + s);
			else Out.PrtSpMsg(2,  "Use tab file:  " + FileHelpers.removeCurrentPath(file) + "; Dated: " + s);
		}
		else {
			Out.PrtSpMsg(2,     "Output exists:  " + FileHelpers.removeCurrentPath(file) + "; Dated: " + s);
			ans = runSTCWMain.yesNo("Use this existing file [y] or perform new search [n] ");
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
				"DBfasta: " + sf + " " + FileHelpers.removeCurrentPath(fasta) + "\n" +
				"Output : " + sb + " " + FileHelpers.removeCurrentPath(blast);
	
		if (!prompt) return false;
		
		Out.PrtWarn(msg);
		boolean ans = runSTCWMain.yesNo("Use this file anyway (y) or do blast (n): ");
		if (ans) return true;
		return false;
	}
	
    private boolean testTabularFile (String msg, String blastFile, int ix, boolean prompt) 
	{
		try {
			// Get the first non-empty line
			BufferedReader reader = FileHelpers.openGZIP(blastFile);
			if (reader==null) return false;
			
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine().trim();
			}
			reader.close();
			
			boolean ans;
			if ( line == null ) {
				if (!prompt) return false;
				ans = runSTCWMain.yesNo(msg + " file " + blastFile + " is empty.  Continue anyway?");
				if (ans) return true;
				else return false;
			}

	    		String[] tokens = line.split("\t");
	    		if (tokens == null || tokens.length < 11) {
				if (prompt) {
					ans = runSTCWMain.yesNo(" file " + blastFile + " is not -m 8 format. Continue anyway?");
					if (ans) return true;
				}
				return false;
	    		}
			
			if (ix == -1) return true; // not selfblast file
			
			// see if type is UniProt	
			LineParser lp = new LineParser();
			if (!lp.parseLine(line)) {
				Out.PrtError("Cannot parse hit file: " + blastFile + "\n   Line:" + line);
				return false;
			}
			String type = lp.getDBtype();
			
			if (!dbInfo.get(ix).dbType.equals("")) {// already added by testDBfile
				if (!dbInfo.get(ix).dbType.equals(type)) {
					if (prompt) 
						Out.PrtError("DB and hit file are different types:" +
								"\n   " + type + ":" + blastFile + 
								"\n   "	+ dbInfo.get(ix).dbType + ":" + dbInfo.get(ix).DBfastaFile);
					return false;
				}
			}
			setType(ix, type);
			dbInfo.get(ix).isProtein = true;
	
			return true;
		} 
		catch ( Exception err ) {
			ErrorReport.reportError(err, "Annotator - loading  Blast file");
			return false;
		} 
	}
	
	public boolean testDBfastaFileAndSetSearchPgm (int ix) {
		String fileName = dbInfo.get(ix).DBfastaFile;
		boolean dob = dbInfo.get(ix).doBlast;
		String prtFileName = FileHelpers.removeCurrentPath(fileName);
		
		try {
			BufferedReader reader = FileHelpers.openGZIP(fileName);
			if (reader==null) return false;
			
			String line1 = reader.readLine();
			LineParser lp = new LineParser ();
			
			if (line1 != null) {
				line1 = line1.trim();
				while ( line1 != null && (line1.length() == 0 || line1.charAt(0) == '#') )
					line1 = reader.readLine().trim();
			}
			if ( line1 == null ) { // must come after checking for a file of empty lines
				Out.PrtError(prtFileName + ": the file " + prtFileName + " is empty." );
				reader.close();
				return false;
			}	
			if (!line1.startsWith(">")) {
				Out.PrtError(prtFileName + ": fasta files should have '>' in column 0 of first non-blank line. ");
				reader.close();
				return false;
			}
			if (! lp.parseLine(line1)) {
				Out.PrtError(prtFileName + ": cannot parse DBfasta file\n   Line:" + line1);
				return false;
			}
				
			// XXX determine blast program to use
			// blastx nucleotide against protein; translated nucleotide in 6 frames
			// blastp protein against protein
			// blastn nucleotide against nucleotide; no translation
			// not-valid protein against nucleotide; could do with tblastn
			String line2 = reader.readLine().trim();
			reader.close();
			if (line2 == null) {
				Out.PrtError( "The file " + prtFileName + " is incorrect." );
				reader.close();
				return false;
			}
			String dbtype = lp.getDBtype();
			String seqtype = "pr";
			if (!dbtype.equals("sp") && !dbtype.equals("tr")) {
				if (BlastArgs.isNucleotide(line2)) seqtype="nt"; 
			}
			if (dbtype.equals("")) dbtype=seqtype;
			
			boolean PEPTIDE=sqlObj.isAAtcw();
			String action="", pgm="blast";
			String searchPgm=dbInfo.get(ix).searchPgm;
			
			boolean isZIP = (fileName.endsWith(".gz")) ? true : false;
			
			if (dbtype.equals("sp") || dbtype.equals("tr") || seqtype.equals("pr")) {
				dbInfo.get(ix).isProtein = true;
				if (PEPTIDE) action = "blastp";
				else action =  "blastx";
				
				if (searchPgm.equals("")) {
					pgm = BlastArgs.getSearch();
					dbInfo.get(ix).searchPgm = pgm;
					dbInfo.get(ix).blastArgs = BlastArgs.getParams(pgm);
				} 
				else pgm = searchPgm;
				
				if (dob) {
					String x = (ix+1) + ". " + pgm;
					if (dbtype.equals("gi")) Out.PrtSpMsg(1, x + " Genbank Protein Fasta File: " + prtFileName);
					else if (dbtype.equals("sp")) Out.PrtSpMsg(1, x + " SwissProt Fasta File: " + prtFileName);
					else if (dbtype.equals("tr")) Out.PrtSpMsg(1, x + " Trembl Fasta File: " + prtFileName);
					else Out.PrtSpMsg(1, x + " Protein Fasta File: " + prtFileName);
				}
			}
			else if (dbtype.equals("nt") || seqtype.equals("nt")){
				if (PEPTIDE) {
					Out.PrtError(prtFileName + ": cannot have a nucleotide annoDB with a protein sTCW");
					return false;
				}
				
				dbInfo.get(ix).isProtein = false;
				action =  "blastn";
				if (!searchPgm.equals("blast")) {
					dbInfo.get(ix).blastArgs="";
					if (searchPgm.equals("diamond") || searchPgm.equals("usearch"))
						Out.PrtWarn("Cannot use " + searchPgm + " with nucleotide annoDB -- using blast");
					pgm = "blast";
				}
				if (dob) {
					String x = (ix+1) + ". " + action;
					if (dbtype.equals("gi")) Out.PrtSpMsg(1, x + " Genbank Nucleotide Fasta File: " + prtFileName);
					else Out.PrtSpMsg(1, x + " Nucleotide Fasta File: " + prtFileName);
				}
			}
			else {
				Out.PrtError("Could not determine type of file for " + prtFileName + 
						" - you may need to run blast and provide the tabular file for input.");
				return false;
			}
			if (isZIP && !pgm.equals("diamond") && !pgm.trim().equals("-")) { 
				Out.PrtError("The file " + prtFileName + " is compressed, which will fail with " + pgm);
				return false;
			}
			if (!dbInfo.get(ix).dbType.equals("")) { // already added by testBlastFile
				if (!dbInfo.get(ix).dbType.equals(dbtype)) {
					Out.PrtError("DB and hit file are different types:" +
						"\n   " + dbtype + ":" + dbInfo.get(ix).DBfastaFile + 
						"\n   "	+ dbInfo.get(ix).dbType + ":" + dbInfo.get(ix).tabularFile);
					return false;
				}
			}
			else setType(ix, dbtype);
				
			dbInfo.get(ix).searchPgm=pgm;
			dbInfo.get(ix).exec = action;
			
			if (dbInfo.get(ix).blastArgs.equals("-")) 
				dbInfo.get(ix).blastArgs = BlastArgs.getParams(pgm);
		
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
								FileHelpers.removeCurrentPath(paths.get(i)) + 
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
	 * Only available for nt-sTCW
	 *******************************************/
	public boolean runSelfBlast(String action) 
	{
		if (action.equals("tblastx") && !doTSelfBlast) return true;
		if (action.equals("blastn") && !doSelfBlast) return true;
		if (!writeSeqFile()) return false;
		 
		long time = Out.getTime();
		
		int ncpu = runSTCWMain.getCPUs();
	    boolean isAAtcw=sqlObj.isAAtcw();
	    
	    String inFile, tabFile, args;
		if (action.equals("tblastx")) {
			tabFile = tselfBlastFile;
			args = tselfBlastArgs; 
			if (args==null || args=="") args = BlastArgs.getTblastxOptions(); 
		    
			Out.PrtSpMsg(1, "Running Sequence translated selfblast");
		}
		else {
			tabFile = selfBlastFile;
			args = selfBlastArgs;
	        if (args==null || args=="") args = BlastArgs.getBlastnOptions();
			
			Out.PrtSpMsg(1, "Running Sequence selfblast ");
		}				
		if (tabFile==null || tabFile=="") 
			return false;  
		
		try { 
			inFile = (new File(seqFilePath)).getAbsolutePath();
			tabFile = (new File(tabFile)).getAbsolutePath();	
			
			boolean rc = BlastRun.run(ncpu, "blast", action, args, isAAtcw, inFile, isAAtcw, inFile, tabFile);
			
			Out.PrtDateMsgTime("Complete " + action, time);

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
		if (!BlastArgs.blastExists()) {
			System.err.println("Error: No blast program detected or identified in HOSTS.cfg");
			return false;
		}
		// creates file of sequences on the first call to this
		if (! writeSeqFile()) return false;
				
		String dbFile =  dbInfo.get(ix).DBfastaFile;
		String blastParams = dbInfo.get(ix).blastArgs;
		String tabFile = dbInfo.get(ix).tabularFile;
		String action =  dbInfo.get(ix).exec;		// blastn, blastp, blastx
		String pgm =     dbInfo.get(ix).searchPgm;   // blast, diamond, usearch
		boolean isAAdb = dbInfo.get(ix).isProtein;
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
	private boolean writeSeqFile()
	{	
		File blastDir = new File(seqBlastDir);
		if (!blastDir.exists()) {
			Out.PrtSpMsg(3,"Creating " + seqBlastDir + " directory");
			if (!blastDir.mkdir()) {
				Out.PrtError("Failed to create " + seqBlastDir + " directory " + blastDir);
				return false;
			}
		}
		seqFilePath = seqBlastPrefix + seqFileSuffix;
		if (FileHelpers.fileExists(seqFilePath)) return true;
		
		Out.PrtSpMsg(3, "Create sequence file: " + FileHelpers.removeCurrentPath(seqFilePath));
		
		int cnt = sqlObj.writeSeqFile(seqSubType, new File(seqFilePath)); // false - do not write subset
		if (cnt < 0) return false;
		Out.PrtSpMsg(4, "Wrote " + cnt + " sequence records");		
		return true;
	}	
	
	/*
	 *  XXX read megablast or tblaxtx for the pairwise comparisons
	 *  TODO: these share the same pairsHash, so end up gathering HSPs for both
	 */
	public int getAllPairsFromBlastFile( 
			 HashMap <String, BlastHitData> pairsHash,
			 String fileBlast, boolean isSelf) throws Exception 
	{  	
		String strLine = null;
		String strHitContig, strQueryContig, keyR, key;
		BlastHitData hitData;
		
		try {
			BufferedReader reader = FileHelpers.openGZIP(fileBlast);
			if (reader==null) return 0;

			int goodHits = 0, ignore = 0, cntRead = 0, cntBad = 0;

			while ((strLine = reader.readLine()) != null) {
				if (strLine.length() == 0 || strLine.charAt(0) == '#') continue; 

				cntRead++;
				String strFields[] = strLine.split("\t");
				if (strFields.length<2) {
					System.err.println("Bad Line at " + cntRead + " : " + strLine);
					cntBad++;
					if (cntBad>50) {
						System.err.println("Too many bad lines -- exiting");
						return 0;
					}
					continue;
				}
				strQueryContig = cleanID(strFields[0].trim());
				strHitContig = cleanID(strFields[1].trim());
				
				if (strQueryContig.equals(strHitContig)) continue; // Ignore self-hit
		
				// key must be same order as blast query-hit
				key = strQueryContig + ";" + strHitContig;
				keyR = strHitContig + ";" + strQueryContig;
						
      			if (!pairsHash.containsKey(keyR) && !pairsHash.containsKey(key)) {
      				hitData = new BlastHitData(false, strLine);
      				if (isSelf) hitData.setIsSelf(true);
      				else	 hitData.setIsTself(true);
         			pairsHash.put(key, hitData);
         			goodHits++; 
      			}
 
				if (cntRead % 10000 == 0) 
					Out.r("Load " + goodHits + " hits " + cntRead + " read");
			} 
			reader.close();
			if (ignore > 0) Out.PrtWarn("Ignored " + ignore + " hits because e-value > 1e-20");
			return goodHits;
			
		} catch (Exception e) {
			Out.PrtError("Input Line = '" + strLine + "'");
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
		if (doSelfBlast) return true;
		if (doTSelfBlast) return true;
		for (int i=0; i< dbInfo.size(); i++) 
			if (dbInfo.get(i).doBlast) return true;
		return false;
	}

	public String getSelfBlastFile() {return selfBlastFile;}
	public String getTSelfBlastFile() {return tselfBlastFile;}
	
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
	
	private void setType(int i, String t) {
		dbInfo.get(i).dbType = t;
		String capType = dbInfo.get(i).dbType.toUpperCase();
		String taxo = dbInfo.get(i).dbTaxo; 
		if (taxo.length() < 3) dbInfo.get(i).typeTaxo = capType + taxo;
		else dbInfo.get(i).typeTaxo = capType + dbInfo.get(i).dbTaxo.substring(0, 3);
	}
	public String getTypeTaxo(int i) {
		return dbInfo.get(i).typeTaxo;
	}
	public int getDbNum(int i) { return dbInfo.get(i).dbNum;}
	public boolean isProtein(int i) {return dbInfo.get(i).isProtein;}
	public String getDBdate(int i) {return dbInfo.get(i).dbDate;}
	
	public void setSeqBlastPrefix(String p) {seqBlastPrefix = p;}
	public void setSeqBlastDir(String p) {seqBlastDir = p;}
	public void setSeqTransType(int t)   {seqSubType = t;}
	
	public void setSelfBlastFile(String s) {doSelfBlast=false; selfBlastFile=s;}
	public void setSelfBlastArgs(String s) {doSelfBlast=true; selfBlastArgs=s;}		
	public void setTSelfBlastFile(String s) {doTSelfBlast=false; tselfBlastFile=s;}
	public void setTSelfBlastArgs(String s) {doTSelfBlast=true;  tselfBlastArgs=s;}
	
	public void setCheckForPrompt() {checkForPrompt=true;} // Already said to prompt in CoreMain
	public void setNoPrompt() { noPrompt=true;}
	
	public void makeDB(int ix, String blast, String fasta, String args, String taxo, 
			String dbdate, String pgm) {
		DB d = new DB(ix, blast, fasta, args, taxo, dbdate, pgm);
		dbInfo.add(d);
	}
	private CoreDB sqlObj = null;
	
	private String seqBlastPrefix = null; // uniBlast/<assemblyID> - check for complete blasts
	private String seqBlastDir = null;
	private String seqFilePath = null;	
	private int    seqSubType = 0;      // 0 - all, 1 = ctg only, 2 = singles only
	
	public boolean doSelfBlast = false;
    private String selfBlastFile = null;
    private String selfBlastArgs = null;
	
	public boolean doTSelfBlast = false;
	private String tselfBlastFile = null; // blast output file
	private String tselfBlastArgs = null;
			
	private boolean doDBblast = false;
	private ArrayList<DB> dbInfo = new ArrayList<DB>();
	
	private class DB {
		DB (int ix, String blast, String fasta, String args,  String taxo,  
				String dbdate,  String pgm) {
			dbNum = ix;
			tabularFile = blast;
			DBfastaFile = fasta; // should exist even if there is a blast file -- for description & sequence
			DBfastaNoPath = fasta.substring(fasta.lastIndexOf("/")+1);
			blastArgs = args;  // if "-", gets set when testing file
			dbTaxo = taxo;
			dbDate = dbdate;
			if (!testExistString(tabularFile)) doBlast=true;
			searchPgm=pgm;
		}
		int dbNum=0; 		// from PAVE.cfg '_n' 
		String tabularFile = "", DBfastaFile = "", blastArgs="";
		String dbType=""; // sp/tr/nt -- gets set on file check
		String dbTaxo="";   // plant, etc
		String typeTaxo="";
		String DBfastaNoPath="";
		String dbDate=null;
		boolean isProtein = false, doBlast = false;
		String exec="blastx"; // blastn for nt -- computed in testDBfastaFile
		String searchPgm="";
		long fileSize=0;
	}
}
