package sng.assem;

import java.io.BufferedReader;
import java.io.File;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import sng.assem.enums.LogLevel;
import sng.assem.enums.RF;
import sng.database.Globals;
import util.database.DBConn;
import util.file.FileHelpers;

/*******************************************************
 * There is an Library object for each library in LIB.cfg (libid or transLib)
 * libid ctglib=0 sequence file
 * libid ctglib=0 reps
 * libid ctglib=1 sequence file and expression file
 * 
 * if Sanger ESTs, the suffix is replaced with .f and .r
 * 
 * CAS304 - code removed for obsolete 
 * 		adding or updating libraries
 * 		allowing multiple sequence files for one library
 * 		there use to be a 'prefix' to disambiguate lib names, but that was discontinued.
 * CAS315 made all file readers read gzipped
 */
public class Library
{
	// for LoadLibMain
	public String mIDStr;
	public File mSeqFile = null, mQualFile = null, mExpFile = null;
	public int mDefQual;
	public int mNGoodSeq=0;
	public int mLID = 0;
	
	// For AssemMain
	public String mLoadDate=null;
	
	private int MAX_NAME_LEN=30, MIN_SEQ_LEN=3; 	
	private final int MIN_NT_LEN=9,  MIN_PEP_LEN=3;
	private final int LONGEST_LEN=100000;
	
	private Pattern mFastaPat = 	Pattern.compile(">(\\S+).*"); // matches non-white
	
	private boolean PEPTIDE=false, hasPaired=false, bIgnExpFile=false, bIgnQualFile=false;
	private Properties mProp;
	private DBConn mDB;
	private String mPfx = ""; // obsolete? prefix for disambiguating expr lib names

	private String mReps="";
	private boolean mCtgLib = false;
	private File mDir;	

	private HashMap<String, Clone> mSeqMap = new HashMap<String, Clone>();
	private HashMap <String, String> condRepMap = new HashMap <String, String> (); // cond rep list
	
	private HashSet<String> mIgnOrigSet = new HashSet <String> ();
	private HashMap<String, String> mRenameMap = new HashMap<String, String>();
	private String mFSuffix = "", mRSuffix = "";
	
	private Pattern mBadCharPat = null; // note, can't be static as changed for peptides
	private String mUnknownSymbol = "N"; // =X for peptides
	
	private int mAvgLen = 0, mMedLen = 0;

	/*******************************************************************************/
	public Library(DBConn db, Properties libProp, File dir){
	try {
		if (!libProp.containsKey("libid")) Log.die("Missing libid");
		mIDStr = 	libProp.getProperty("libid");
		if (mIDStr.trim().equals("")) Log.die("Libid is blank - it must exist");
		if (!Utils.validName(mIDStr)) Log.die("Invalid ID " + mIDStr + ": may only contain letters, numbers, underscores");	
		
		mDB = db;
		mDir = dir;
		mProp = new Properties();
		
		mReps = 	libProp.getProperty("reps").trim(); 
		mCtgLib = 	libProp.getProperty("ctglib").equals("1"); // TCWprops changes translib to libid with ctglib=1
		
		Enumeration <?> keys = libProp.propertyNames();
		while (keys.hasMoreElements()){
			String key = keys.nextElement().toString();
			String val = libProp.getProperty(key);
			mProp.put(key, val);
		}			
		
	// sequence file
		String path = mProp.getProperty("seqfile");	
		
		if (path==null || path.equals("")) {
			if (mCtgLib) Log.die("Translib must have sequence file: " + mIDStr);
			else { // is expression lib
				if (mReps.contentEquals("")) mReps = mIDStr; // demoAsm
				return;
			}
		}
		mSeqFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
		if (!mSeqFile.isFile()) Log.die("Cannot find sequence file "	+ mSeqFile.getAbsolutePath());	
		
		
		path = mProp.getProperty("qualfile");
		if (path!=null && !path.equals("")){
			mQualFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
			if (!mQualFile.isFile())
				Log.die("Cannot find qual file " + mQualFile.getAbsolutePath());
		} 
		
		path = mProp.getProperty("expfile");		
		if (!path.equals("")) {
			mExpFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
			if (!mExpFile.isFile())
				Log.die("Cannot find Count file "	+ mExpFile.getAbsolutePath());
		}
		
		mFSuffix = mProp.getProperty("fiveprimesuf");
		mRSuffix = mProp.getProperty("threeprimesuf");
		if (!mFSuffix.equals("")&& !mFSuffix.equals(Globals.def5p)) 
			Log.indentMsg("5' suffix: " + mFSuffix + "    3' suffix: " + mRSuffix, LogLevel.Basic);

	} catch (Exception e) {Log.die("Error reading libraries from LIB.cfg");}
	}
	
	
	/**************************************************************
	 * Load properties into BD
	 */
	public void loadLibIntoDB()  {
		try {
			PreparedStatement ps = mDB.prepareStatement("insert into library "
					+ "(libid,fastafile,fastqfile,fiveprimesuf,threeprimesuf,"
					+ "title,organism,cultivar,tissue,stage,treatment,year,"
					+ "source,sourcelink,libsize,loaddate,defqual,avglen,medlen,ctglib," 
					+ "strain, prefix, orig_libid,parent,reps) "
					+ " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),?,?,?,?,?,?,?,?,?)");
			ps.setString(1, mIDStr);
			ps.setString(2, (mSeqFile != null ? mSeqFile.getAbsolutePath() : ""));
			ps.setString(3, (mQualFile != null ? mQualFile.getAbsolutePath() : ""));
			ps.setString(4, mProp.getProperty("fiveprimesuf"));
			ps.setString(5, mProp.getProperty("threeprimesuf"));
			ps.setString(6, mProp.getProperty("title"));
			ps.setString(7, mProp.getProperty("organism"));
			ps.setString(8, mProp.getProperty("cultivar"));
			ps.setString(9, mProp.getProperty("tissue"));
			ps.setString(10, mProp.getProperty("stage"));
			ps.setString(11, mProp.getProperty("treatment"));
			ps.setString(12, mProp.getProperty("year"));
			ps.setString(13, mProp.getProperty("source"));
			ps.setString(14, mProp.getProperty("sourcelink"));
			ps.setInt(15, mSeqMap.size());
			ps.setInt(16, mDefQual);
			ps.setInt(17, mAvgLen); // zero, not computed yet
			ps.setInt(18, mMedLen);
			ps.setBoolean(19, mProp.getProperty("ctglib").equals("1"));
			ps.setString(20, mProp.getProperty("strain"));
			ps.setString(21, mProp.getProperty("prefix"));
			ps.setString(22, mProp.getProperty("orig_libid"));
			ps.setString(23, ""); // set later (search 'parent')
			ps.setString(24, mReps);
			ps.execute();
			ps.close();
			mLID = mDB.lastID();
		}
		catch (Exception e) {Log.die(e, "Saving sequence properities to databases");}
	}
	/*****************************************************
	 * SCAN ALL FILES
	 ****************************************************/
	public void scanAllFiles(HashSet<String> seqNames) {
		try {
			scanSeqFile(seqNames);
			
			scanQualFile();
			
			scanExpFile();
			
			Log.newLine(LogLevel.Basic);
		}
		catch (Exception e) {Log.die(e, "Loading sequence file");}
	}
	
	/*********************************************************
	 * Load sequence file
	 */
	public void loadAllFiles() {
	try {	
		loadSeqFile();
		
		if (hasPaired) matePairs();
		
		if (!bIgnQualFile) loadQualFile();
		
		if (!bIgnExpFile) loadExpFile();
		
		Log.indentMsg("", LogLevel.Basic);
	}
	catch (Exception e) {Log.die(e, "Loading sequence file");}
	}
	
	/************************************************************
	// Scan seq files and check for errors. 
	 * 		Also create dbName, seqMap<dbMap, Clone>, mIgnoredSet and mRenameMap to be used in loads
	 * Errors: 
	// 	sequences too short - ignored
	//  seqname too long - ignored
	// 	clone names duplicated (in lib, fail; between libs, warn)
	// 	non-standard sequence chars - fixed
	 * 
	 * Origname may be changed - a mapping is created here of original to dbName, used by other methods
	 *  incorrect name - fixed
	 *  sanger est have suffixes replaced with .f and .r
	***************************************************************/
	private void scanSeqFile(HashSet<String> allSeqNames){
	try {
		if (mSeqFile == null) return;
		
		Log.indentMsg("Verify sequence data from " + mSeqFile.getName(), LogLevel.Basic);
		
		Pattern mNamePat = 		Pattern.compile("^[\\w\\.]+$"); // word char and . from start to finish
		Pattern mNameFixPat = 	Pattern.compile("[^\\w\\.]"); // does NOT match word char or .
		
		setTypeAAorNT(); // read file for type
		
		BufferedReader reader = FileHelpers.openGZIP(mSeqFile.getAbsolutePath());
		
		int wNSeqTooShort = 0,  wNNonStand = 0, wNBadName = 0;
		int eNDups = 0, eNBadHeader = 0,  eNLongName = 0, eNBad=0;
		
		Clone curSeq = null;
		String line, origName="", dbName, cloneName, msg;
		int n = 0;
		RF mRF= RF.Unk;
		
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("/")) continue;

			if (line.startsWith(">")) {
				n++;
				if (n % 10000 == 0) Utils.singleLineMsg("Scanned " + n); 
				
				if (curSeq != null) { // finish last sequence
					if (curSeq.mSeqLen < MIN_SEQ_LEN) {
						msg = curSeq.mFullName + ": sequence too short (< " + MIN_SEQ_LEN + "), will not be loaded";
						if (wNSeqTooShort==0) Log.warn(msg); else Log.warnFile(msg);
						wNSeqTooShort++;
						
						mIgnOrigSet.add(curSeq.mOrigName);
					}
				}
				
				curSeq = null;
				Matcher m = mFastaPat.matcher(line);
				if (!m.matches()) {
					msg = "Bad fasta header: " + line;
					if (eNBadHeader==0) Log.warn(msg); else Log.warnFile(msg);
					eNBadHeader++;
					continue;
				}
				
				origName = m.group(1);
				if (origName.length() > MAX_NAME_LEN) {
					msg = origName + ": Sequence name too long (>" + MAX_NAME_LEN + "), will not be loaded";
					if (eNLongName==0) Log.error(msg); else Log.errFile(msg);
					eNLongName++; 
					mIgnOrigSet.add(origName);
					continue;
				} 
				
				// create dbName
				m = mNamePat.matcher(origName);
				if (!m.matches()) {
					Matcher m2 = mNameFixPat.matcher(origName);
					dbName = m2.replaceAll("_");
					
					msg = origName + "Sequence name contain unusual characters which will be replaced with '_',  e.g. renamed to " + dbName;
					if (wNBadName==0) Log.warn(msg); else Log.warnFile(msg);;
					wNBadName++;
					
					mRenameMap.put(origName, dbName);
				}
				else dbName=origName;
				
				CloneName cName = new CloneName(dbName, mFSuffix,	mRSuffix);
				if (cName.isEST()) {
					cloneName = cName.mClone;
					mRF =       cName.mRF;
					dbName =  	cName.replaceRF2();
					
					if (!origName.contentEquals(dbName)) mRenameMap.put(origName, dbName);
				}
				else cloneName = origName;
					
			/** add to sequence lists **/
				
				curSeq = new Clone(mProp, origName, cloneName, dbName, mRF);
				
				String lcName = dbName.toLowerCase();
				if (allSeqNames.contains(lcName)) {
					if (origName.equals(dbName)) msg = origName + ": duplicate sequence name";
					else msg = origName + ": renamed (" + dbName + ") causes duplicate sequence name";
					if (eNDups==0) Log.error(msg); else Log.errFile(msg);
					eNDups++; 
				}
				else {
					mSeqMap.put(dbName, curSeq);
					allSeqNames.add(lcName);
				}
			} 
			else // sequence
			{
				if (curSeq != null) {
					Matcher m = mBadCharPat.matcher(line);
					if (m.find()) {
						if (!PEPTIDE) msg = origName + ": Characters other than AGCTagct found in sequence (will be replaced by N)";
						else          msg = origName + ": Characters other than valid amino acids found (will be replaced by X)";
						
						if (wNNonStand==0) Log.warn(msg); else Log.warnFile(msg);
						wNNonStand++;
					}
					curSeq.mSeqLen += line.length();
				}
			}
		}
		reader.close();
		
		Utils.singleLineMsg("Complete scanning file"); 
		
	// Wrap-up stats
		int[] lengthCounts 	= new int[LONGEST_LEN];;
		for (int i = 0; i < LONGEST_LEN; i++) lengthCounts[i] = 0;	
		int nR=0, nF=0;
		
		for (String dbn : mSeqMap.keySet())
		{
			Clone sObj = mSeqMap.get(dbn);
			int len = sObj.mSeqLen;
			mNGoodSeq++;
			mAvgLen += len;
			lengthCounts[Math.min(lengthCounts.length-1,len)]++;
			
			if (sObj.mRF == RF.R) nR++;
			else if (sObj.mRF == RF.F) nF++;
		}
	
		if (mNGoodSeq!=0) mAvgLen /= mNGoodSeq;

		int runningCount = 0;
		for (int i = 0; i < lengthCounts.length; i++) {
			runningCount += lengthCounts[i];
			if (runningCount >= mNGoodSeq/2) {
				mMedLen = i;
				break;
			}
		}
		// Libraries already loaded, so update
		mDB.executeUpdate("Update library set avglen=" + mAvgLen + ", medlen=" + mMedLen + " where libid='" + mIDStr + "'");
		
		String x = mNGoodSeq + " good sequences, " + mAvgLen +     " avg length, " +  mMedLen +     " median length";
		if ((nR+nF)>0) {
			x += ", " +  (nR+nF) +    " ESTs ";
			hasPaired=true;
		}
		Log.indentMsg(x + "\n",LogLevel.Basic);
		
	// Errors and warnings
		if (wNNonStand>0 || wNSeqTooShort>0 || wNBadName>0) {
			Log.indentMsg("WARN: " + mIDStr + " - warnings in sequence file:", LogLevel.Basic);
			if (wNNonStand > 0) {
				if (!PEPTIDE) msg = "fasta lines contained characters other than AGCTN (will be replaced by N's)";
				else          msg = "fasta lines contained characters other than amino acids (will be replaced by X's)";
				prtCount(wNNonStand, msg);
			}
			prtCount(wNBadName, "sequence names had bad characters which were replaced by '_'");
			prtCount(wNSeqTooShort, "ignored, sequences too short (under " + MIN_SEQ_LEN + " chars)");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
		}
		if (eNDups > 0 || eNBadHeader > 0 || eNLongName > 0 || eNBad> 0) {
			Log.indentMsg("ERROR: " + mIDStr + " - fatal errors in sequence file:", LogLevel.Basic);
			prtCount(eNDups, "duplicate");
			prtCount(eNBadHeader, "bad header");
			prtCount(eNLongName, "too long name");
			prtCount(eNBad, "bad entry");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
			Log.die("Errors in seqfile for " + mSeqFile);
		}
	}
	catch (Exception e) {Log.die(e, "Scanning sequence file");}
	}
	/******* Determine sequence file type ***************/
	private void setTypeAAorNT() {
		try {
			BufferedReader seqFH = FileHelpers.openGZIP(mSeqFile.getAbsolutePath() );
			String line;
			int cntAAseq=0, cntNTseq=0; 
			boolean found=false;
			
			while ((line = seqFH.readLine())!=null) {
				if (line.startsWith(">")) continue;
				
				String tmpLine = line.toUpperCase();
				
				int numAA=0, numNT=0;
				for (int i = 0; i< line.length(); i++) {
					char n = tmpLine.charAt(i);
					if (n=='A' || n=='C' || n=='T' || n=='G') numNT++;
					else numAA++;							
				}
				if (numNT > numAA) cntNTseq++;
				else cntAAseq++;
				
				if (cntNTseq > cntAAseq+3) { 
					PEPTIDE=false; found=true;
					break;
				}
				else if (cntAAseq > cntNTseq+3) {
					PEPTIDE=true; found=true;
					break;
				}
				else if (cntAAseq+cntNTseq > 10) {
					break;
				}
			}
			seqFH.close();
			
			if (!found) {
				if      (cntAAseq>0 && cntNTseq==0) PEPTIDE=true;
				else if (cntAAseq==0 && cntNTseq>0) PEPTIDE=false;
				else {
					Log.warn("Having trouble determing sequence types" + "(" + cntNTseq + "nt," + cntAAseq + "aa)");
					if (cntAAseq>0) PEPTIDE=true;
					else            PEPTIDE=false;
				}
			}
		
			if (PEPTIDE) 	{
				MIN_SEQ_LEN = MIN_PEP_LEN;
				mDB.tableCheckAddColumn("assem_msg", "peptide", "tinyint", "");
				Log.indentMsg("   Reading Peptides sequences; using minimal length of " + MIN_PEP_LEN, LogLevel.Basic);
				
				mBadCharPat = Pattern.compile("[^FIMLVPSTAY*QKEWGHNDCRX]"); 
				mUnknownSymbol = "X";
			}
			else {
				MIN_SEQ_LEN = MIN_NT_LEN;
				mDB.tableCheckDropColumn("assem_msg", "peptide");
				Log.indentMsg("   Reading nucleotide sequences; using minimal length of " + MIN_NT_LEN, LogLevel.Basic);
				
				mBadCharPat = Pattern.compile("[^acgtnACGTN]");
				mUnknownSymbol = "N";
			}	
		}
		catch (Exception e) {Log.die(e, "Finding sequence file type");}
		}
	/******************************************************************
	 * Load sequence
	 */
	private void loadSeqFile() {
	try {
		if (mSeqFile == null) return;
		
		Log.indentMsg("Loading " + mSeqFile.getName(), LogLevel.Basic);

		PreparedStatement ps = mDB.prepareStatement("insert into clone (cloneid, origid, libid, LID,"
						+ "sequence,quality,sense,length,mate_CID,source)"
						+ " values(?,?,?,?,?,?,?,?,0," + mProp.getProperty("ctglib") + ")"); // WN not sure why setting source like this

		BufferedReader reader = FileHelpers.openGZIP(mSeqFile.getAbsolutePath()); // CAS315

		Clone curSeq = null;
		
		String line;
		int nAdd = 0, nPrt=0;
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("/")) continue;

			if (line.startsWith(">")){
				if (curSeq != null){
					curSeq.mSeqLen = curSeq.mSeq.length();
					
					nAdd++;
					batchAddSeq(curSeq, ps);
					if (nAdd==100) {
						ps.executeBatch();
						nAdd=0;
					}
					
					curSeq.mSeq = ""; 
					if (nPrt % 10000 == 0) Utils.singleLineMsg(nPrt + "/" + mNGoodSeq);
					nPrt++;
				}
				
				curSeq = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches()) {
					String origname = m.group(1);
					if (mIgnOrigSet.contains(origname)) continue;
					
					String dbName = (mRenameMap.containsKey(origname)) ? mRenameMap.get(origname) : origname;
					
					if (!mSeqMap.containsKey(dbName)) continue; 
					
					curSeq = mSeqMap.get(dbName);
				} 
				else continue;
			} 
			else {
				if (curSeq == null) continue;
				
				Matcher m = mBadCharPat.matcher(line);
				line = m.replaceAll(mUnknownSymbol);
				curSeq.mSeq += line;
			}
		} // end while
		reader.close();
		Utils.singleLineMsg("       ");
		
		if (curSeq != null){
			curSeq.mSeqLen = curSeq.mSeq.length();
			nAdd++;
			batchAddSeq(curSeq, ps);
		}
		if (nAdd > 0) ps.executeBatch(); // CAS304 was n%100=0
		ps.close();
		
		// Get clone IDs
		ResultSet rs = mDB.executeQuery("select CID,cloneid from clone where LID='" + mLID + "'");
		while (rs.next()){
			String name = rs.getString("cloneid");
			if (mSeqMap.containsKey(name))
				mSeqMap.get(name).mID = rs.getInt("CID");
		}
		rs.close();
	}
	catch (Exception e) {Log.die(e, "Loading sequence file");}
	}
	private void batchAddSeq(Clone c, PreparedStatement ps) {
	try {
		int sense = (c.mRF == RF.F) ? 1 : -1;
		
		ps.setString(1, c.mFullName);
		ps.setString(2, c.mOrigName);
		ps.setString(3, mIDStr);
		ps.setInt(4, mLID);
		ps.setString(5, c.mSeq);
		
		ps.setString(6, defQualStr(c.mSeq.length())); // add now with default qual values; may be updated after
		ps.setInt(7, sense);
		ps.setInt(8, c.mSeq.length());

		ps.addBatch();
	}
	catch (Exception e) {Log.die(e, "Add sequence to DB");}
	}
	
	/**************************************************
	 * Compute mate pairs
	 */
	private void matePairs() throws Exception
	{
		HashMap<String, HashMap<RF, Clone>> mClones = new HashMap<String, HashMap<RF, Clone>> ();
		for (String dbName : mSeqMap.keySet())
		{
			Clone sObj = mSeqMap.get(dbName);
			if (sObj.mRF != RF.Unk) {
				String clone = sObj.mCloneName;
				RF rf = sObj.mRF;
				
				if (!mClones.containsKey(clone)) mClones.put(clone, new HashMap<RF, Clone>());
			
				if (!mClones.get(clone).containsKey(rf)) mClones.get(clone).put(rf, sObj);

			}
		}
		PreparedStatement ps = mDB.prepareStatement("update clone set mate_CID=? where CID=? ");

		for (String cname : mClones.keySet())
		{
			if (mClones.get(cname).containsKey(RF.F) && mClones.get(cname).containsKey(RF.R))
			{
				int cid5 = mClones.get(cname).get(RF.F).mID;
				int cid3 = mClones.get(cname).get(RF.R).mID;
				
				ps.setInt(1, cid3);
				ps.setInt(2, cid5);
				ps.addBatch();
				
				ps.setInt(2, cid3);
				ps.setInt(1, cid5);
				ps.addBatch();
			}
		}
		ps.executeBatch();
		ps.close();
	}
	
	/******************************************************************
	 * Scan quality file
	 */
	private void scanQualFile() {
	try {
		if (mQualFile == null) {
			bIgnQualFile=true;
			return;
		}
		
		Log.indentMsg("Verify qual data from " + mQualFile.getName(), LogLevel.Basic);
		
		int  wNMissingQual = 0, wNMissingSeq = 0;
		int  eNQualDups = 0, eNBadHeader = 0, eNBadLength = 0;
		
		HashSet<String> qualsFound = new HashSet<String>();
		Clone curSeq = null;
		String line, msg;
		int n = 0;
		
		BufferedReader reader = FileHelpers.openGZIP(mQualFile.getAbsolutePath());
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0) continue;

			if (line.startsWith(">"))
			{
				n++;
				if (n % 10000 == 0) Utils.singleLineMsg("Scanned " + n + " of " + mNGoodSeq); 
				curSeq = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches())
				{
					String origname = m.group(1);
					if (mIgnOrigSet.contains(origname)) continue;
					
					if (qualsFound.contains(origname)) {
						msg = origname + ": duplicate qual found for sequence";
						if (eNQualDups==0) Log.error(msg); else Log.errFile(msg);
						eNQualDups++;
						continue;
					}
					qualsFound.add(origname);
					
					String dbName = (mRenameMap.containsKey(origname)) ? mRenameMap.get(origname) : origname;
					
					if (mSeqMap.containsKey(dbName)) {
						curSeq = mSeqMap.get(dbName);
					} 
					else {
						msg = origname + ": qual found for unknown sequence";
						if (wNMissingSeq==0) Log.warn(msg); else Log.warnFile(msg);
						wNMissingSeq++;
						continue;
					}
				} 
				else {
					msg = "bad qual header: " + line;
					if (eNBadHeader==0) Log.error(msg); else Log.errFile(msg);
					eNBadHeader++;
				}
			}
			else {
				if (curSeq != null) {
					String[] vals = line.trim().split("\\s+");
					curSeq.mQualLen += vals.length;
				}
			}
		}
		reader.close();
		Utils.singleLineMsg("                                                       "); 
		
		for (Clone c : mSeqMap.values())
		{
			if (c.mSeqLen < MIN_SEQ_LEN) continue;
			
			if (c.mQualLen == 0){
				msg = c.mFullName + ": missing qual for sequence ";
				if (wNMissingQual==0) Log.warn(msg); else Log.warnFile(msg);
				wNMissingQual++;
			}
			else if (c.mQualLen != c.mSeqLen)	{
				msg = c.mFullName + ": " + c.mQualLen + " qual values; " + c.mSeqLen + " sequence characters";
				if (eNBadLength==0) Log.error(msg); else Log.errFile(msg);
				eNBadLength++;
			}
		}
		
		if (wNMissingQual > 0 || wNMissingSeq > 0) {
			System.err.println();
			Log.indentMsg("WARN: SeqID " + mIDStr + " - warnings in quality file:", LogLevel.Basic);
			prtCount(wNMissingQual, "missing quals");
			prtCount(wNMissingSeq, "missing sequence");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
		}
		if (eNQualDups > 0 || eNBadHeader > 0 || eNBadLength > 0) {
			System.err.println();
			Log.indentMsg("ERROR: SeqID " + mIDStr + " - errors in quality file:", LogLevel.Basic);
			prtCount(eNQualDups, "duplicate quals");
			prtCount(eNBadHeader, "bad header");
			prtCount(eNBadLength, "quals length not equal sequence length");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
			Log.die("Errors in quality file for " + mQualFile);
		}
		Log.indentMsg("Complete scan", LogLevel.Basic);
	}
	catch (Exception e) {Log.die(e, "Scanning quality file");}
	}
	/***********************************************************
	 * loadQualFile
	 */
	private void loadQualFile() {
	try {
		if (bIgnQualFile) return;
		
		Log.indentMsg("Loading " + mQualFile.getName(), LogLevel.Basic);
		
		BufferedReader reader = FileHelpers.openGZIP(mQualFile.getAbsolutePath());

		PreparedStatement ps = mDB.prepareStatement("update clone set quality=? where CID=?");
		
		Clone curSeq = null;
		int nAdd=0, nPrt=0;
		String line;
		
		while (reader.ready()){
			line = reader.readLine();
			line = line.trim();
			if (line.length() == 0) continue;

			if (line.startsWith(">")) {
				if (curSeq != null) {
					curSeq.checkQual(true);
					ps.setString(1, curSeq.mQual);
					ps.setInt(2, curSeq.mID);
					ps.addBatch();
					nAdd++;
					if (nAdd == 100) {
						ps.executeBatch();
						nAdd=0;
					} 
					
					curSeq.mQual = ""; 
					
					nPrt++;
					if (nPrt % 10000 == 0) Utils.singleLineMsg(nPrt + " of " + mNGoodSeq);
				}
				curSeq = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches()) {
					String origname = m.group(1);
					if (mIgnOrigSet.contains(origname)) continue;
					
					String dbName = (mRenameMap.containsKey(origname)) ? mRenameMap.get(origname) : origname;
					
					if (!mSeqMap.containsKey(dbName)) continue;
											
					curSeq = mSeqMap.get(dbName);
				} 
				else continue;
			}
			else {
				if (curSeq == null) continue;
					
				curSeq.mQual += line.trim();
				curSeq.mQual += " ";
			}
		}
		if (curSeq != null) {
			curSeq.checkQual(true);
			ps.setString(1, curSeq.mQual);
			ps.setInt(2, curSeq.mID);
			ps.addBatch();
			nAdd++;
		}
		if (nAdd>0) ps.executeBatch();
		ps.close();
		reader.close();
	}
	catch (Exception e) {Log.die(e, "Loading quality file");}
	}
			
	/********************************************************
	 * Check expression data
	 * There will be a libid with repids that match the header line of the expression file
	 * e.g.
	 * libid  = C
	 * reps   = C1,C2,C3
	 * libid = D			
	 * reps = D1, D2, D3
	 * 
	 * translib     = ds1
	 * expfile      = /Users/cari/Workspace/github/TCW/projects/exBar/Bar_counts.txt
	 * SeqID   C1      C2      C3	D1	D2	D3
	 * 
	 * This will not work for two translib
	 * 
	 * Duplicate libids are checked in the interface. I think duplicates reps are okay
	 */
	private void scanExpFile() { 
	try {
		if (mExpFile==null) {
			bIgnExpFile=true;
			return;
		}
		Log.indentMsg("Verify count data from " + mExpFile.getName(), LogLevel.Basic);

	// Get all expression libs with reps
		HashMap <String, String> allCondRep = new HashMap <String, String> ();
		ResultSet rs = mDB.executeQuery("select libid, reps, fastafile from library where ctglib=0");
		while (rs.next()){
			String fafile = rs.getString("fastafile");
			if (!fafile.contentEquals("")) continue; // ESTs for assemble
			
			String lib = rs.getString("libid");
			String reps = rs.getString("reps").trim();
			
			allCondRep.put(lib,  reps);
		}
		
	// Header line	of reps	
		BufferedReader bwExp = FileHelpers.openGZIP(mExpFile.getAbsolutePath()); // CAS315
		String line = bwExp.readLine();
		String[] libTok = line.split("\\s+");
		
		if (libTok.length < 2) {
			bIgnExpFile=true;
			Log.errLog("No replicate/condition names found on first line of - ignoring file " + mExpFile.getAbsolutePath());
			bwExp.close();
			return;
		}
		
		TreeSet <String> fileRepList = new TreeSet <String>  ();
		for (int i=1; i<libTok.length; i++) fileRepList.add(libTok[i]);
		
	// Find the expression libid that matches these reps	
		String cond = "Conditions: ";
		int cntFound=0;
		for (String key : allCondRep.keySet()) {
			String [] libReps = allCondRep.get(key).split(",");
			
			int nRep=0;
			boolean found=true;
			for (String lib : libReps) {
				if (!fileRepList.contains(lib)) {
					found=false;
					break;
				}
				else {cntFound++; nRep++;}
			}
			if (found) {
				condRepMap.put(key, allCondRep.get(key));
				cond += key + "(" + nRep + ") ";
			}
		}
		Log.indentMsg(cond, LogLevel.Basic);
		
		if (condRepMap.size()==0) {
			bIgnExpFile=true;
			Log.error("No replicate/conditions match a libid - ignoring file " + mExpFile.getAbsolutePath());
			bwExp.close();
			return;
		}
		if (cntFound!=fileRepList.size()) {
			bwExp.close();
			bIgnExpFile=true;
			Log.error("The reps defined ( " + cntFound + ") in LIB.cfg do not correspond with the expression file " + mExpFile.getAbsolutePath()
				+	"\nHeader line: " + line);
			return;
		}
		
	// Check sequence names the same as in the seqFile
		int cntUnk=0, cntCnt=0, cntErr=0;
		while (bwExp.ready()) {
			line = bwExp.readLine().trim();
			String[] tok = line.split("\\s+");
			String origname = tok[0];
			
			if (mIgnOrigSet.contains(origname)) continue;
			
			String dbName = (mRenameMap.containsKey(origname)) ? mRenameMap.get(origname) : origname;
			
			if (!mSeqMap.containsKey(dbName)) {
				String msg = "Unknown sequence " + tok[0] + " in count file";
				if (cntUnk==0) Log.warn(msg); else Log.warnFile(msg);
				cntUnk++;
			}
			if (tok.length != libTok.length) {
				String msg = "Wrong number of columns (" + libTok.length + "): " + line;
				if (cntCnt==0) Log.error(msg); else Log.errFile(msg);
				cntCnt++;
			}
			try { 
				Double.parseDouble(tok[1]);
			}
			catch (Exception e){
				String msg = "2nd column must be number: " + line;
				if (cntErr==0) Log.error(msg); else Log.errFile(msg);
				cntErr++;
			}
		}
		// prints to stdout and to the project load file
		if (cntUnk>0) {
			System.err.println();
			Log.indentMsg("WARN: SeqID " + mIDStr + " - warnings in expression file:", LogLevel.Basic);
			prtCount(cntUnk, "Unknown sequences in count file");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
		}
		if (cntCnt>0 || cntErr>0) {
			System.err.println();
			Log.indentMsg("ERROR: SeqID " + mIDStr + " - errors in expression file:", LogLevel.Basic);
			prtCount(cntErr, "Number not in second column");
			prtCount(cntCnt, "Inconsistent number of columns");
			Log.indentMsg("See " + Log.errFile + " for all occurances", LogLevel.Basic);
			Log.die("Errors in expression file for " + mExpFile);
		}
		bwExp.close();
	}
	catch (Exception e) {Log.error(e, "Checking expression file"); }
	}

	/*********************************************
	 * Load the expression data
	 * TransLib: 
	 * 		mID and mIDstr
	 * Libid: 
	 * 		expLibIDStr is the expression library ID 
	 * 		TreeSet<String> mExpLibList is the list of reps
	 */
	private void loadExpFile() {
	try {
		if (bIgnExpFile) return;
		
		Log.indentMsg("Loading " + mExpFile.getName(), LogLevel.Basic);
	
		HashMap<String, Integer> condMap =    new HashMap <String, Integer> (); // LID of conds
		HashMap<String,Integer> rep2lib = new HashMap<String,Integer>();
		HashMap<String,Integer> rep2num = new HashMap<String,Integer>();
		
		ResultSet rs = mDB.executeQuery("select libid, LID, reps, fastafile from library where ctglib=0");
		while (rs.next())
		{
			String fafile = rs.getString("fastafile");
			if (!fafile.contentEquals("")) continue; // ESTs for assemble
			
			String lib = rs.getString("libid");
			if (!condRepMap.containsKey(lib)) continue;
			
			int LID = rs.getInt("LID");
			condMap.put(lib,LID);
			
			String repList = rs.getString("reps").trim();
			
			int rnum=1;
			
			for (String rep : repList.split(",")) {
				if (!rep.trim().equals("")){
					rep2num.put(rep.trim(), rnum);
					rep2lib.put(rep.trim(), LID);
				}
				rnum++;
			}
		}
		
	// update parent in library
		for (String lib : condMap.keySet()) {
			mDB.executeUpdate("update library set parent='" + mIDStr + "' where LID=" + condMap.get(lib));
		}
		
		
	// Parse first line
		BufferedReader bwExp = FileHelpers.openGZIP(mExpFile.getAbsolutePath());
		String line = bwExp.readLine(); // do not trim, may start with \t that would get removed
		String[] repTok = line.split("\\s+");
		
		Vector<String> libOrder = new Vector<String>();
		for (int i = 1; i < repTok.length; i++) {
			String rep = repTok[i].trim();
			libOrder.add(rep);			
		}
		
	// clone_exp
		String xLine="",  repName;
		int LID=0, rep=0;
		HashMap<Integer,Integer> lid2cnt = new HashMap<Integer,Integer>();
		
		PreparedStatement ps = mDB.prepareStatement("insert into clone_exp (LID, count,CID,rep) values(?,?,?,?)");
		int nAdd = 0, nPrt=0;
		while (bwExp.ready()) {
			xLine = bwExp.readLine();
			String[] tok = xLine.trim().split("\\s+");
			String origname = tok[0];
			
			if (mIgnOrigSet.contains(origname)) continue;
			
			String dbName = (mRenameMap.containsKey(origname)) ? mRenameMap.get(origname) : origname;
			
			if (!mSeqMap.containsKey(dbName)) continue; 
			
			lid2cnt.clear();
			int seqID = mSeqMap.get(dbName).mID;
			
			for (int i = 1; i < tok.length && i <= libOrder.size(); i++)
			{
				repName =  libOrder.get(i-1);
				
				LID = rep2lib.containsKey(repName) ? rep2lib.get(repName) : 0;
				rep = rep2num.containsKey(repName) ? rep2num.get(repName) : 0;
				
				double dcount = Double.parseDouble(tok[i]); // a mapper called 'Salmon' outputs real numbers
				int count = (int) (dcount+0.5);
				
				if (lid2cnt.containsKey(LID)) lid2cnt.put(LID, lid2cnt.get(LID)+count);
				else lid2cnt.put(LID, count);
				
				ps.setInt(1, LID);
				ps.setInt(2, count);
				ps.setInt(3, seqID);
				ps.setInt(4, rep);
				ps.addBatch();
				
				nAdd++;
				if (nAdd==100) {
					ps.executeBatch();	
					nAdd=0;
				}
				nPrt++;
				if (nPrt%10000==0) Utils.singleLineMsg(nPrt + " loaded");
			}
			for (int id : lid2cnt.keySet()) {
				ps.setInt(1, id);
				ps.setInt(2, lid2cnt.get(id));
				ps.setInt(3, seqID);
				ps.setInt(4, 0);
				ps.addBatch();
			}
		}
		if (nAdd>0) ps.executeBatch();
		bwExp.close();
		
		mDB.executeUpdate("update library set expdate=now(),exploaded=1 where lid=" + mLID);
	}
	catch (Exception e) {Log.die(e, "Read expression file");}
	}
	
	/***************************************************** 
	// These methods called from the assembler
	*****************************************************/
	public Library(DBConn db, String libid) throws Exception
	{
		mDB = db;
		mIDStr = libid;
		
		ResultSet rs = mDB.executeQuery("select LID,libsize,loaddate,fastafile,defqual,avglen,medlen,ctglib,reps "
				+ " from library where libid='" + mIDStr + "'");
		if (rs.next())
		{
			mLID = rs.getInt("LID");
			mAvgLen = rs.getInt("avglen");
			mMedLen = rs.getInt("medlen");
			mDefQual = rs.getInt("defqual");
			mSeqFile = new File(rs.getString("fastafile"));
			mCtgLib = rs.getBoolean("ctglib");
			mReps = rs.getString("reps").trim();
			mNGoodSeq = rs.getInt("libsize");
			mLoadDate = rs.getString("loaddate");
		} 
		rs.close();
	}
	/****************************************************************
	 * XXX Private utilities
	 */
	
	private class CloneName
	{
		String mFull, mClone;
		RF mRF = RF.Unk;
		String mRSuffix, mFSuffix; 

		public CloneName(String fullname, String fsuf, String rsuf) {
			mFull = fullname;
			mClone = fullname;
			mFSuffix = fsuf;
			mRSuffix = rsuf;
		}
		public boolean isEST() { // returns true if has correct suffix 
			if (!mRSuffix.equals("") && mFull.endsWith(mRSuffix)){
				mRF = RF.R;
				mClone = mFull.substring(0, mFull.length() - mRSuffix.length());
				return true;
			} 
			else if (!mFSuffix.equals("") && mFull.endsWith(mFSuffix)) {
				mRF = RF.F;
				mClone = mFull.substring(0, mFull.length() - mRSuffix.length());
				return true;
			}
			return false;
		}
		public String replaceRF2()
		{
			String out = mFull;
			if (!mFSuffix.equals("") && mFull.endsWith(mFSuffix)) {
				out = mFull.substring(0, mFull.length() - mFSuffix.length());
				out += Globals.def5p;
			} 
			else if (!mRSuffix.equals("") && mFull.endsWith(mRSuffix)) {
				out = mFull.substring(0, mFull.length() - mRSuffix.length());
				out += Globals.def3p;
			} 
			return out;
		}
	}
	private void prtCount(int count, String msg) {
		if (count>0) {
			String x = String.format("%-6d %s", count, msg);
			Log.indentMsg(x, LogLevel.Basic);
		}
	}
	
	/****************************************************************
	 * XXX Public utilities
	 */
	public String defQualStr(int size)
	{
		StringBuffer dqstr = new StringBuffer();
		String defQual = String.valueOf(mDefQual);
		for (int i = 1; i <= size; i++)
		{
			dqstr.append(defQual);
			if (i < size) dqstr.append(" ");
		}
		
		return dqstr.toString();
	}
	public int mNumSingle=0, mNumBuried=0;
	public void collectStats(int tcid) throws Exception {
		// singles (including mated with 2 clones)
		mNumSingle = mDB.executeCount("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mLID + " and contig.tcid=" + tcid + " and contig.numclones=1");
			
		mNumSingle += mDB.executeCount("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mLID + " and contig.tcid=" + tcid + " and (contig.numclones=2 and clone.mate_CID > 0)");
		mNumSingle /= 2;
		
		mNumBuried = mDB.executeCount("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mLID + " and contig.tcid=" + tcid + " and contclone.buried=1");		
	}
	public TreeSet<String> getAllReps()
	{
		TreeSet<String> ret = new TreeSet<String>();
		ret.add(mPfx + mIDStr);
		for (String rep : mReps.split(","))
		{
			if (!rep.trim().equals(""))
				ret.add(mPfx + rep.trim());
		}
		return ret;
	}
	public boolean hasExp()
	{
		return (mExpFile != null );
	}
	public String idPlusReps()
	{
		String ret = mIDStr;
		if (!mReps.equals(""))
		{
			int nReps = mReps.split(",").length;
			ret += "(" + nReps + ")";
		}
		return ret;
	}
	public boolean hasSeqFile() {return (mSeqFile!=null);}	
}
