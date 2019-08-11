package sng.assem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import sng.assem.enums.LibAction;
import sng.assem.enums.LibStatus;
import sng.assem.enums.LogLevel;
import sng.assem.enums.RF;
import util.database.DBConn;



public class Library
{
	int MIN_SEQ_LEN=3; 
	final int MIN_NT_LEN=9;
	final int MIN_PEP_LEN=3;
	int MAX_NAME_LEN=30; 
	final int LONGEST_LEN=100000;
	
	boolean PEPTIDE=false, FIRST=true;
	boolean hasLoc=false;			// is BED produced file with locations
	Properties mProp;
	DBConn mDB;
	String mIDStr;
	String mPfx = ""; // prefix for disambiguating expr lib names
	int mNumESTLoaded = 0;
	int mNumMissingQual = 0;
	long mTimeLoaded = 0;
	int mNumAssemblies = 0;
	int mID = 0;
	int mAID  = 0;
	int mSize = 0;
	LibStatus mStatus = LibStatus.New;
	LibAction mAction = LibAction.NewLoad;
	boolean m454;
	String mLoadDate;
	int mDefQual;
	int mDQSize = 0;
	int mPropsChanged;
	boolean mCtgLib = false;
	String mReps;
	
	File mDir;
	File mSeqFile = null;
	File mQualFile = null;
	File mExpFile = null;

	TreeMap<String, TreeMap<RF, Clone>> mClones;
	TreeMap<String, Clone> mClonesFullName;
	Pattern mClonePat = null;
	String mFSuffix = "";
	String mRSuffix = "";
	static Pattern mFastaPat = null;
	Pattern mBadCharPat = null; // note, can't be static as different for peptides
	String mUnknownSymbol = "N"; // =X for peptides
	
	static Pattern mNamePat = null, mNameFixPat = null;
	static Pattern mBEDPat = null;
	
	// properties that can change after initial load
	String[] mPropsToCheck = {"title","organism","cultivar","strain","tissue","stage","treatment","year","source","sourcelink","ctglib"};
	Clone mLongestClone = null;
	Clone mShortestClone = null;
	
	int mNShortSeq = 0;
	int mNGoodClone = 0;
	int mAvgLen = 0;
	int mMedLen = 0;
	int[] mLengthCounts;

	boolean foundPaired = false;
	int mNPaired = 0;

	int mNDups = 0;
	boolean mDupWarningShown = false;

	int mNLongName = 0;
	boolean mLongNameWarningShown = false;

	int mNNoSuffix = 0;
	boolean mNoSuffixWarningShown = false;

	int mNNonStand = 0;
	boolean mNonStandWarningShown = false;

	int mNBadQual = 0;
	boolean mBadQualWarningShown = false;

	int mNQualDups = 0;
	boolean mQualDupWarningShown = false;

	int mNBadLengthQual = 0;
	boolean mBadLengthQualWarningShown = false;

	int mNValidQual = 0;
	int mNMissingQual = 0;
	boolean mMissingQualWarningShown = false;

	int mNMissingClone = 0;
	boolean mMissingCloneWarningShown = false;
	
	int mNBadNames = 0;
	boolean mBadNameWarningShown = false;
	
	// these are filled out at the end of assembly
	int mNumBuried = 0;
	int mNumSingle = 0;
		
	TreeMap<String,Integer> mClone2ID = null;
	
	long mExpLoadDate;
	boolean mExpLoaded = false;
	boolean mExpOutOfDate = false;
	TreeSet<String> mExpLibList;  // holds the expression libs found in the expression file
									// filled out by checkExp, which must be called first

	public Library(DBConn db, Properties libProp, File dir)
			throws Exception
	{
		mDB = db;
		mProp = new Properties();
		mExpLibList = new TreeSet<String>();
		mDir = dir;
		mClones = new TreeMap<String, TreeMap<RF, Clone>>();
		mClonesFullName = new TreeMap<String, Clone>();
		mLengthCounts = new int[LONGEST_LEN];
		for (int i = 0; i < LONGEST_LEN; i++) mLengthCounts[i] = 0;	

		if (!libProp.containsKey("libid")) Log.die("Missing libid");
		
		// Note, prefix has been added to libid already by PaveProps
		mIDStr = libProp.getProperty("libid");
		mReps = libProp.getProperty("reps").trim();
		mPfx = libProp.getProperty("prefix").trim();
		mCtgLib = libProp.getProperty("ctglib").equals("1");

		Enumeration keys = libProp.propertyNames();
		while (keys.hasMoreElements())
		{
			String key = keys.nextElement().toString();
			String val = libProp.getProperty(key);
			mProp.put(key, val);
		}		
		getStatus();
		if (mStatus != LibStatus.New) 
		{
			// No further init is necessary unless it's a new load
			return;
		}
		
		String path = mProp.getProperty("seqfile");		
		mSeqFile = null;
		if (!path.equals(""))
		{
			mSeqFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
			mQualFile = null;
	
			if (!mSeqFile.isFile())
			{
				Log.die(msgStr("can't find sequence file "	+ mSeqFile.getAbsolutePath()));
			} 
			else if (FIRST) {  // check type
				Log.indentMsg("Checking file type of " + mSeqFile.getName() + "....", LogLevel.Basic);
				
				BufferedReader seqFH = new BufferedReader ( new FileReader ( mSeqFile) );
				String line;
				int cntAAseq=0, cntNTseq=0;
					
				while ((line = seqFH.readLine())!=null) {
					if (!line.startsWith(">")) {
						String tmp = line.toUpperCase();
						int numAA=0, numNT=0;
						for (int i = 0; i< line.length(); i++) {
							char n = tmp.charAt(i);
							if (n=='A' || n=='C' || n=='T' || n=='G') numNT++;
							else numAA++;							
						}
						if (numNT > numAA) cntNTseq++;
						else cntAAseq++;
						
						if (cntNTseq > cntAAseq+3) {
							Log.indentMsg("   Reading nucleotide sequences; using minimal length of " + MIN_NT_LEN, LogLevel.Basic);
							PEPTIDE=false;
							FIRST=false;
							db.tableCheckDropColumn("assem_msg", "peptide");
							break;
						}
						else if (cntAAseq > cntNTseq+3) {
							Log.indentMsg("   Reading Peptides sequences; using minimal length of " + MIN_PEP_LEN, LogLevel.Basic);
							PEPTIDE=true;
							FIRST=false;
							db.tableCheckAddColumn("assem_msg", "peptide", "tinyint", "");
							break;
						}
						else if (cntAAseq+cntNTseq > 10) {
							break;
						}
					}
				 }
				 if (FIRST==true) {
					Log.indentMsg("Having trouble determing sequence types: Assuming nucleotide sequence." +
							"(" + cntNTseq + "," + cntAAseq + ")",
							LogLevel.Basic);
					FIRST=false;
				 }
			
				if (PEPTIDE) MIN_SEQ_LEN=MIN_PEP_LEN;
				else MIN_SEQ_LEN=MIN_NT_LEN;
			}
			
			if (mProp.containsKey("qualfile") && !mProp.getProperty("qualfile").equals(""))
			{
				path = mProp.getProperty("qualfile");
				mQualFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
				if (!mQualFile.isFile())
				{
					Log.die(msgStr("can't find qual file " + mQualFile.getAbsolutePath()));
				} 	
			} 
		}
		
		path = mProp.getProperty("expfile");		
		if (!path.equals(""))
		{
			mExpFile = (path.startsWith("/") ? new File(path) : new File(mDir, path));
			if (!mExpFile.isFile())
			{
				Log.die(msgStr("can't find Count file "	+ mExpFile.getAbsolutePath()));
			} 
		}
		
		mRSuffix = mProp.getProperty("threeprimesuf");
		mFSuffix = mProp.getProperty("fiveprimesuf");	

		if (mFastaPat == null)
		{
			mFastaPat = Pattern.compile(">(\\S+).*");
			mNamePat = Pattern.compile("^[\\w\\.]+$");
			mNameFixPat = Pattern.compile("[^\\w\\.]");
			mBEDPat = Pattern.compile(">\\s*(\\S+):(\\d+)-(\\d+)");  // XXX
		}
		if (PEPTIDE) {
			mBadCharPat = Pattern.compile("[^FIMLVPSTAY*QKEWGHNDCRX]"); 
			mUnknownSymbol = "X";
		}
		else {
			mBadCharPat = Pattern.compile("[^acgtnACGTN]");
			mUnknownSymbol = "N";
		}
	}
	static public String nameFix(String in)
	{
		Matcher m = mNameFixPat.matcher(in);
		return m.replaceAll("_");
	}
	public TreeSet<String> getAllNames()
	{
		TreeSet<String> ret = new TreeSet<String>();
		ret.add(mPfx + mIDStr);
		for (String rep : mReps.split(","))
		{
			if (!rep.trim().equals(""))
			{
				ret.add(mPfx + rep.trim());
			}
		}
		return ret;
	}
	public boolean hasExp()
	{
		return (mExpFile != null );
	}
	
	// see what's in the DB for this lib
	public void getStatus() throws Exception
	{
		Statement st = mDB.createStatement();
		ResultSet rs;

		rs = st.executeQuery("select LID, libsize, fastqfile, UNIX_TIMESTAMP(loaddate) as date," +
				" UNIX_TIMESTAMP(expdate) as expdate, exploaded "
						+ " from library where libid='" + mIDStr + "'");
		if (rs.first())
		{
			mID = rs.getInt("LID");
			mSize = rs.getInt("libsize");
			mTimeLoaded = rs.getLong("date");
			mExpLoaded = rs.getBoolean("exploaded");
			mExpLoadDate = (mExpLoaded ? rs.getLong("expdate") : 0);
			
			rs = st.executeQuery("select count(*) as count from clone where LID='"
							+ mID + "'");
			rs.first();
			mNumESTLoaded = rs.getInt("count");

			rs = st.executeQuery("select count(*) as count from assemlib where LID='"
							+ mID + "'");
			rs.first();
			mNumAssemblies = rs.getInt("count");

			if (mQualFile != null)
			{
				rs = st.executeQuery("select count(*) as count from clone where LID='" + mID + "' and quality=''");
				rs.first();
				mNumMissingQual = rs.getInt("count");
			}
			
			mStatus = LibStatus.UpToDate;
				
			if (mStatus == LibStatus.UpToDate)
			{
				mAction = LibAction.NoAction;
			} 
			else if (mStatus == LibStatus.New)
			{
				mAction = LibAction.NewLoad;
			} 
			
			if (mAction == LibAction.NoAction)
			{
				// Even if the lib seems up to date, it still may have failed to fully load right
				// Can't really do this anymore with expression libraries in the mix
				if (mStatus == LibStatus.Incomplete)
				{
					mAction = LibAction.ReLoad;
				}
			}
			
			propsChanged();
			
			if (mAction == LibAction.NoAction && (mPropsChanged > 0 || mExpOutOfDate))
			{
				mAction = LibAction.UpdateProperties;
			}
		}
		rs.close();
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

	public void checkRequiredProps() throws Exception
	{
		String[] fields =
		{ "seqfile", "fiveprimesuf", "threeprimesuf", "MIN_SEQ_LEN", };
		for (String field : fields)
		{
			if (!mProp.containsKey(field))
			{
				throw (new Exception(
						msgStr("Missing required dataset property " + field)));
			}
		}
	}

	// Scan seq files and check for:
	// sequences too short
	// clone names duplicated (in lib, fail; between libs, warn)
	// clone name too long
	// non-standard sequence chars
	// note, to reduce memory usage the seqs are not stored
	public void scanSeqFiles(HashSet<String> cloneNames) throws Exception
	{
		if (mSeqFile == null) return;
		Utils.termOut("Checking " + mSeqFile.getName() + "..."); 
		if (scanGeneFile(cloneNames)) return;
		
		BufferedReader reader = new BufferedReader(new FileReader(mSeqFile));
		boolean emptySeqWarnShown = false;
		
		Clone cur_clone = null;
		String line;
		int n = 0;
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("/")) continue;

			// Fasta header line
			if (line.startsWith(">"))
			{
				n++;
				if (n % 10000 == 0) Utils.singleLineMsg("Scanned " + n); 
				if (cur_clone != null)
				{
					if (cur_clone.mSeqLen < MIN_SEQ_LEN)
					{
						if (!emptySeqWarnShown)
						{
							Log.indentMsg("Sequences with length < " + MIN_SEQ_LEN + 
									" will not be loaded, e.g. " + cur_clone.mFullName, LogLevel.Basic);
							emptySeqWarnShown = true;
						}
					}
				}
				cur_clone = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches())
				{
					String fullname = m.group(1);
					if (fullname.length() > MAX_NAME_LEN)
					{
						mNLongName++;
						if (!mLongNameWarningShown)
						{
							printError(msgStr("Sequence name too long: " + fullname ));
						}
						mLongNameWarningShown = true;
					} 
					else
					{
						m = mNamePat.matcher(fullname);
						if (!m.matches())
						{
							if (!mBadNameWarningShown)
							{
								String new_fullname = nameFix(fullname);
								Log.indentMsg("Sequence name contain unusual characters which will be replaced with '_'.\n" +
									"               Example:" + fullname + " renamed to " + new_fullname, LogLevel.Basic);
								mBadNameWarningShown = true;
							}
							fullname = nameFix(fullname);
							mNBadNames++;
						}
						CloneName cName = new CloneName(fullname, mRSuffix,	mFSuffix);
						if (!cName.parse())
						{
							if (mRSuffix.length() > 0 || mFSuffix.length() > 0)
							{
								mNNoSuffix++;
								if (!mNoSuffixWarningShown)
								{
									//Log.indentMsg(msgStr(" No suffix found on clone : " + fullname
									//				+ " (message only shown once per library)"),LogLevel.Detail);
								}
								mNoSuffixWarningShown = true;
							}
						}
						cur_clone = new Clone(mProp, cName.replaceRF(), cName.mClone, cName.mRF, cName.mFull);
						if (!addClone(cur_clone.mFullName, cName.mClone, cName.mRF, cur_clone, cloneNames))
						{
							mNDups++;
							if (!mDupWarningShown)
							{
								printError(msgStr(" duplicate sequence name " + fullname ));
							}
							mDupWarningShown = true;
						}
					}
				} 
				else
				{
					printError(msgStr("bad fasta header in " + mSeqFile.getAbsolutePath() + "\n" + line));
				}
			} 
			else
			{
				// sequence
				if (cur_clone == null)
				{
					printError(msgStr("found improper formated entry: \n" + line));
				}
				Matcher m = mBadCharPat.matcher(line);
				if (m.find())
				{
					mNNonStand++;
					if (!mNonStandWarningShown)
					{
						if (!PEPTIDE)
							Log.indentMsg("Characters other than agct found in sequence (will be replaced by N), e.g."
										+ cur_clone.mFullName,LogLevel.Basic);
						else {
							Log.indentMsg("Characters other than valid amino acids found (will be replaced by X), e.g. "
									+ cur_clone.mFullName ,LogLevel.Basic);
						}
					}
					mNonStandWarningShown = true;
				}
				cur_clone.mSeqLen += line.length();
			}
		}
		Utils.singleLineMsg("Complete scanning file"); 
		// Count the clones having r,f,unk suffixes and also with sequences too short
		
		TreeMap<RF, Integer> rfCounts = new TreeMap<RF, Integer>();
		for (RF rf : RF.values())
		{
			rfCounts.put(rf, 0);
		}
		for (String clone : mClones.keySet())
		{
			for (RF rf : mClones.get(clone).keySet())
			{
				int len = mClones.get(clone).get(rf).mSeqLen;
				if (len < MIN_SEQ_LEN) {mNShortSeq++; continue;} // we're not going to load the empty ones
				rfCounts.put(rf, 1 + rfCounts.get(rf));
			
				mNGoodClone++;
				mAvgLen += len;
				mLengthCounts[Math.min(mLengthCounts.length-1,len)]++;
			}
		}
		if (mNBadNames > 0)
		{
			Log.indentMsg(msgStr(mNBadNames	+ " sequence names had bad characters which were replaced by '_'"),LogLevel.Basic);
		}
		if (mNNonStand > 0)
		{
			if (!PEPTIDE)
				Log.indentMsg(msgStr(mNNonStand
							+ " fasta lines contained characters other than AGCTN (will be replaced by N's)"),LogLevel.Basic);
			else 
				Log.indentMsg(msgStr(mNNonStand
						+ " fasta lines contained characters other than amino acids (will be replaced by X's)"),LogLevel.Basic);
		}
		if (mNShortSeq > 0)
		{
			Log.indentMsg(msgStr(mNShortSeq
					+ " ignored, sequences too short (under " + MIN_SEQ_LEN + " chars)"),LogLevel.Basic);
		}
		if (mNGoodClone!=0) mAvgLen /= mNGoodClone;

		int runningCount = 0;
		for (int i = 0; i < mLengthCounts.length; i++)
		{
			runningCount += mLengthCounts[i];
			if (runningCount >= mNGoodClone/2)
			{
				mMedLen = i;
				break;
			}
		}
		mSize = mNGoodClone;
		Log.indentMsg(msgStr(
				mNGoodClone + " good sequences, " + 
				mAvgLen +     " avg length, " +  
				mMedLen +     " median length, " + 
				paired() +    " paired sequences " +   
				"\n"),LogLevel.Basic);
	}
	/*****************************************************************
	 * XXX bedtools getfasta -fi x.fasta -bed x.gff3 -s -split -fo mrna.fasta
	 * produces fasta files with header lines, e.g. >LG_1:7829-14077(-)
	 * It produces duplicates with JGI files
	 */
	public boolean scanGeneFile(HashSet<String> cloneNames) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(mSeqFile));
		boolean emptySeqWarnShown = false;
		boolean foundBed=false, dupName=false;
		
		String cur_name = null;
		int cur_len=0, n=0;
		long total_len=0;
		String line;
		
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("/")) continue;

			if (line.startsWith(">"))
			{
				n++;
				if (n % 10000 == 0) Utils.singleLineMsg("Scanned " + n); 
				if (cur_name != null)
				{
					if (cur_len < MIN_SEQ_LEN)
					{
						if (!emptySeqWarnShown)
						{
							Log.indentMsg("Sequences with length < " + MIN_SEQ_LEN + 
									" will not be loaded, e.g. " + cur_name, LogLevel.Basic);
							emptySeqWarnShown = true;
						}
						mNShortSeq++;
					}
					else {
						mNGoodClone++;
						total_len += cur_len;
						mLengthCounts[Math.min(LONGEST_LEN,cur_len)]++;
					}
				}
				cur_name = null;
				cur_len=0;
				dupName = false;
				Matcher m = mBEDPat.matcher(line);
				if (m.find())
				{
					foundBed=true;
					cur_name=line;
					if (!cloneNames.contains(line)) cloneNames.add(line);
					else dupName=true;
				} 
				else 
				{
					if (!foundBed) return false; // not bed output
					else printError(msgStr("bad fasta header in " + 
								mSeqFile.getAbsolutePath() + "\n" + line));
				}
			} 
			else
			{
				// sequence
				if (cur_name == null)
				{
					printError(msgStr("found improper formated entry: \n" + line));
				}
				else {
					Matcher m = mBadCharPat.matcher(line);
					if (m.find())
					{
						mNNonStand++;
						if (!mNonStandWarningShown)
						{
							if (!PEPTIDE)
								Log.indentMsg("Characters other than agct found in sequence (will be replaced by N), e.g."
											+ cur_name,LogLevel.Basic);
							else 
								Log.indentMsg("Characters other than valid amino acids found (will be replaced by X), e.g. "
										+ cur_name ,LogLevel.Basic);
						}
						mNonStandWarningShown = true;
					}
					cur_len += line.length();
				}
			}
		}
		Log.indentMsg("Complete scanning file with locations", LogLevel.Basic); 
		
		if (mNNonStand > 0)
		{
			if (!PEPTIDE)
				Log.indentMsg(msgStr(mNNonStand
							+ " fasta lines contained characters other than AGCTN (will be replaced by N's)"),LogLevel.Basic);
			else 
				Log.indentMsg(msgStr(mNNonStand
						+ " fasta lines contained characters other than amino acids (will be replaced by X's)"),LogLevel.Basic);
		}
		if (mNShortSeq > 0)
		{
			Log.indentMsg(msgStr(mNShortSeq
					+ " ignored, sequences too short (under " + MIN_SEQ_LEN + " chars)"),LogLevel.Basic);
		}
		mAvgLen = (int) (total_len/ (long) mNGoodClone);

		int runningCount = 0;
		for (int i = 0; i < mLengthCounts.length; i++)
		{
			runningCount += mLengthCounts[i];
			if (runningCount >= mNGoodClone/2)
			{
				mMedLen = i;
				break;
			}
		}
		mSize = mNGoodClone;
		Log.indentMsg(msgStr(mNGoodClone + "  total good sequences, avg length "
						+ mAvgLen + ", median length " + mMedLen + "\n"),LogLevel.Basic);
		hasLoc=true;
		return true;
	}
	// check qual files, but don't store quality strings, to conserve memory
	public void scanQualFiles() throws Exception
	{
		if (mQualFile == null)
			return;
		Utils.termOut("Verifying " + mQualFile.getName() + "..."); 
		BufferedReader reader = new BufferedReader(new FileReader(mQualFile));

		TreeSet<String> qualsFound = new TreeSet<String>();

		Clone cur_clone = null;
		String line;
		int n = 0;
		int lineNum = 0;
		while ((line = reader.readLine()) != null)
		{
			lineNum++;
			line = line.trim();
			if (line.length() == 0)
				continue;

			// Header line
			if (line.startsWith(">"))
			{
				n++;
				if (n % 10000 == 0) Utils.singleLineMsg("Scanned " + n + "/" + mNGoodClone); 
				cur_clone = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches())
				{
					String fullname = m.group(1);
					if (qualsFound.contains(fullname))
					{
						mNQualDups++;
						if (!mQualDupWarningShown)
						{
							printError(msgStr(" duplicate qual found for sequence: " 	+ fullname ));
							mQualDupWarningShown = true;
						}
					}
					qualsFound.add(fullname);
					String repName = CloneName.replaceRF2(fullname,mFSuffix, mRSuffix);
					if (mClonesFullName.containsKey(repName))
					{
						cur_clone = mClonesFullName.get(repName);
					} 
					else
					{
						mNMissingClone++;
						if (!mMissingCloneWarningShown)
						{
							printError(msgStr(" qual found for unknown sequence: "
								+ fullname + " (message only shown once)"));
							mMissingCloneWarningShown = true;
						}
					}
				} 
				else
				{
					printError(msgStr("bad qual header in " + mQualFile.getAbsolutePath() + "\n" + line));
				}
				if (cur_clone == null)
				{
					printError("missing qual header before line: " + line);
				}				
			}
			else
			{
				// it should be a line of qual values
				if (cur_clone == null)
				{
					throw(new Exception("missing qual header before line: " + lineNum));
				}
				String[] vals = line.trim().split("\\s+");
				cur_clone.mQualLen += vals.length;
			}
		}
		Utils.termOut("Complete scanning file..."); 

		for (Clone c : mClonesFullName.values())
		{
			if (c.mSeqLen < MIN_SEQ_LEN) continue;
			if (c.mQualLen == 0)
			{
				mNMissingQual++;
				if (!mMissingQualWarningShown)
				{
					printError(msgStr(" missing qual for sequence: " + c.mFullName ));
					mMissingQualWarningShown = true;
				}
			}
			else 
			{
				if (c.mQualLen != c.mSeqLen)			
				{
					mNBadLengthQual++;
					if (!mBadLengthQualWarningShown)
					{
						printError(msgStr(c.mQualLen + " qual values found for sequence: "
							+ c.mFullName
							+ " which has sequence length " + c.mSeqLen));
						mBadLengthQualWarningShown = true;
					}				
				}
				else
				{
					mNValidQual++;
				}
			}
		}
		Log.newLine(LogLevel.Basic);
	}

	// return false for duplicate
	public boolean addClone(String fullname, String name, RF rf, Clone clone,HashSet<String> cloneNames)
	{
		if (cloneNames.contains(fullname.toLowerCase()))
		{
			return false;
		}
		cloneNames.add(fullname.toLowerCase());
		if (!mClonesFullName.containsKey(fullname))
		{
			mClonesFullName.put(fullname, clone);
		}
		if (!mClones.containsKey(name))
		{
			mClones.put(name, new TreeMap<RF, Clone>());
		}
		if (!mClones.get(name).containsKey(rf))
		{
			mClones.get(name).put(rf, clone);
		} 
		else
		{
			return false;
		}
		return true;
	}

	public String msgStr(String str)
	{
		return mIDStr + ": " + str;
	}

	public int readErr()
	{
		return (mNLongName + mNDups + mNMissingQual);
	}

	public int qualErr()
	{
		return (mNMissingClone + mNQualDups + mNBadQual);
	}

	public int paired()
	{
		if (!foundPaired)
		{
			mNPaired = 0;
			for (String clone : mClones.keySet())
			{
				if (mClones.get(clone).containsKey(RF.F)
						&& mClones.get(clone).containsKey(RF.R))
				{
					mNPaired += 2;
				}
			}
			foundPaired = true;
		}
		return mNPaired;
	}

	public int propsChanged() throws Exception
	{
		mPropsChanged = 0;
		for (String prop : mPropsToCheck)
		{
			if (!propertyUpToDate(prop))
			{
				Log.indentMsg(mIDStr + ": new value: " + prop + "=" + mProp.getProperty(prop) + "",LogLevel.Basic);
				mPropsChanged++;
			}
		}
		
		return mPropsChanged;
	}
	public boolean propertyUpToDate(String propName) throws Exception
	{
		boolean ret = false;
		ResultSet rs = mDB.executeQuery("select " + propName + " from library where libid='" + mIDStr + "'");
		String curVal =  mProp.getProperty(propName);
		if (rs.first())
		{
			String dbVal = rs.getString(propName);
			if (curVal.equals(dbVal))
			{
				ret = true;
			}
		}

		return ret;
	}
	public void updateProperties() throws Exception
	{
		if (mPropsChanged > 0)
		{
			Log.indentMsg("Update " + mPropsChanged + " properties for " + mIDStr,LogLevel.Basic);
			int numUpdated = 0;
			for (String prop : mPropsToCheck)
			{
				if (!propertyUpToDate(prop))
				{
					updateProperty(prop);
					numUpdated++;
				}
			}
			if (numUpdated != mPropsChanged)
			{
				Log.msg("WARNING: " + numUpdated + " properties updated for " + mIDStr + ", expecting " + mPropsChanged, LogLevel.Basic);
			}
		}
	}

	public void updateProperty(String prop) throws Exception
	{
		String newVal = mProp.getProperty(prop);
		mDB.executeUpdate("update library set " + prop + "='" + newVal + "' where libid='" + mIDStr + "'");
	}
	// XXX Scan the files again, this time uploading the sequences.
	public void doLoadGene()
	{
		if (mSeqFile == null) return;
		Utils.singleLineMsg("loading " + mSeqFile.getName() + "...");
		try {
			mDB.tableCheckAddColumn("assem_msg", "hasLoc", "tinyint", "");
			PreparedStatement ps = mDB.prepareStatement("insert into clone "
						+ "(cloneid, origid, libid, LID,"
						+ "sequence,quality,sense,length,mate_CID,source)"
						+ " values(?,?,?,?,?,?,?,?,0," + mProp.getProperty("ctglib") + ")"); 
			BufferedReader reader = new BufferedReader(new FileReader(mSeqFile));

			HashSet <String> addedSet = new HashSet <String> ();
			String cloneid = null, mSeq = "", origid=null;
			String line;
			int n = 0, nSkipped=0, nadd=0, sense=0;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0) continue;
	
				if (line.startsWith(">"))
				{
					if (origid != null)
					{
						if (mSeq.length() < MIN_SEQ_LEN) nSkipped++;
						else 
						{
							n++; nadd++;
							addedSet.add(origid);
							ps.setString(1, cloneid);
							ps.setString(2, origid);
							ps.setString(3, mIDStr);
							ps.setInt(4, mID);
							ps.setString(5, mSeq);
							ps.setString(6, defQualStr(mSeq.length()));
							ps.setInt(7, sense);
							ps.setInt(8, mSeq.length());
	
							ps.addBatch();
							if (nadd == 100) {ps.executeBatch(); nadd=0;}
						}
						mSeq = ""; 
						if (n % 10000 == 0) Utils.singleLineMsg(n + "/" + mNGoodClone);
					}
					origid = null;
					Matcher m = mBEDPat.matcher(line);
					// >LG_1:7829-14077(-)
					//cloneid is <libid><seqid#>_gene#
					if (m.find()) 
					{
						cloneid =  mIDStr + String.format("_%05d", (n+1));
						
						origid=line.substring(1,line.length());
						// origid cannot be duplicate.
						// this is only used by AssemMain to extract location
						String tmp=origid;
						int cnt=1;
						while (addedSet.contains(tmp)) {
							tmp = origid + "_" + cnt;
							cnt++;
						}
						origid = tmp;
					} 
					else
					{
						throw (new Exception(msgStr("bad fasta header in "
								+ mSeqFile.getAbsolutePath() + "\n" + line)));
					}
				} 
				else
				{
					// sequence
					if (origid == null)
						throw (new Exception(msgStr(" found sequence with no header line\n" + line)));
					Matcher m = mBadCharPat.matcher(line);
					line = m.replaceAll(mUnknownSymbol);
					mSeq += line;
				}
			}
			
			if (origid != null)
			{
				if (mSeq.length() < MIN_SEQ_LEN) nSkipped++;
				else 
				{
					n++; nadd++;
					ps.setString(1, cloneid);
					ps.setString(2, origid);
					ps.setString(3, mIDStr);
					ps.setInt(4, mID);
					ps.setString(5, mSeq);
					ps.setString(6, defQualStr(mSeq.length()));
					ps.setInt(7, sense);
					ps.setInt(8, mSeq.length());
					ps.addBatch();
				}
			}
			if (nadd > 0) ps.executeBatch();
			ps.close();
			if (nSkipped > 0)
				Log.indentMsg("Skipped " + nSkipped + 
						" sequences due to length < " + MIN_SEQ_LEN,LogLevel.Basic); 
		}
		catch (Exception e) {
			Log.msg("Error loading dataset: " + e.toString(),LogLevel.Detail);
			Log.exception(e);
			System.err.println("Error loading dataset: " + e.toString()); 
		}
	}
	
	// Scan the files again, this time uploading the sequences.
	public void doLoad() throws Exception
	{
		doLoadLib();
		if (hasLoc) {
			doLoadGene();
			return;
		}
		
		mID = mDB.lastID();
		
		if (mSeqFile == null) return;
		Utils.singleLineMsg("loading " + mSeqFile.getName() + "...");

		PreparedStatement ps = mDB.prepareStatement("insert into clone (cloneid, origid, libid, LID,"
						+ "sequence,quality,sense,length,mate_CID,source)"
						+ " values(?,?,?,?,?,?,?,?,0," + mProp.getProperty("ctglib") + ")"); // WN not sure why setting source like this

		BufferedReader reader = new BufferedReader(new FileReader(mSeqFile));

		Clone cur_clone = null;
		TreeSet<String> empties = new TreeSet<String>();
		String line;
		int n = 0;
		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("/"))
				continue;

			// Fasta header line
			if (line.startsWith(">"))
			{
				if (cur_clone != null)
				{
					cur_clone.mSeqLen = cur_clone.mSeq.length();
					if (cur_clone.mSeqLen < MIN_SEQ_LEN) 
					{
						empties.add(cur_clone.mFullName);
					}
					else
					{
						n++;
						batchAddClone(cur_clone, ps,n);
					}
					cur_clone.mSeq = ""; // save space
					if (n % 10000 == 0) Utils.singleLineMsg(n + "/" + mNGoodClone);
				}
				cur_clone = null;
				Matcher m = mFastaPat.matcher(line);
				if (m.matches())
				{
					String fullname = nameFix(m.group(1));
					String repName = CloneName.replaceRF2(fullname,mFSuffix, mRSuffix);
					if (!mClonesFullName.containsKey(repName))
					{
						throw(new Exception("Unknown sequence on second pass: " + repName));
					}
					cur_clone = mClonesFullName.get(repName);
				} 
				else
				{
					throw (new Exception(msgStr("bad fasta header in "
							+ mSeqFile.getAbsolutePath() + "\n" + line)));
				}
			} 
			else
			{
				// sequence
				if (cur_clone == null)
				{
					throw (new Exception(msgStr(" found sequence with no clone\n" + line)));
				}
				Matcher m = mBadCharPat.matcher(line);
				line = m.replaceAll(mUnknownSymbol);
				cur_clone.mSeq += line;
			}
		}
		
		if (cur_clone != null)
		{
			cur_clone.mSeqLen = cur_clone.mSeq.length();
			if (cur_clone.mSeqLen < MIN_SEQ_LEN) 
			{
				empties.add(cur_clone.mFullName);
			}
			else
			{
				n++;
				batchAddClone(cur_clone, ps,n);
			}
		}
		
		if (n % 100 != 0) ps.executeBatch();
		ps.close();
		
		if (empties.size() > 0)
		{
			Log.indentMsg("Skipped " + empties.size() + " sequences due to length < " + MIN_SEQ_LEN,LogLevel.Basic); 
		}
		
		getCloneIDs();
		
		if (mQualFile != null)
		{
			Utils.termOut("Loading " + mQualFile.getName() + "...");
			reader = new BufferedReader(new FileReader(mQualFile));
	
			ps = mDB.prepareStatement("update clone set quality=? where CID=?");
			
			cur_clone = null;
			n = 0;
			while (reader.ready())
			{
				line = reader.readLine();
				line = line.trim();
				if (line.length() == 0)
					continue;
	
				// Header line
				if (line.startsWith(">"))
				{
					if (cur_clone != null)
					{
						if (!empties.contains(cur_clone.mFullName))
						{
							n++;
							if (n % 10000 == 0) Utils.singleLineMsg(n + " of " + mNGoodClone);
							batchUpdateCloneQual(cur_clone, ps,n);						
							cur_clone.mQual = ""; // save space
						}
					}
					cur_clone = null;
					Matcher m = mFastaPat.matcher(line);
					if (m.matches())
					{
						String fullname = m.group(1);
						String repName = CloneName.replaceRF2(fullname,mFSuffix, mRSuffix);
						if (mClonesFullName.containsKey(repName))
						{
							cur_clone = mClonesFullName.get(repName);
						} 
					} 
					else
					{
						throw (new Exception(msgStr("bad qual header in "
								+ mQualFile.getAbsolutePath() + "\n" + line)));
					}
				}
				else
				{
					// it should be a line of qual values
					if (cur_clone == null)
					{
						throw(new Exception("missing qual header before line: " + line));
					}
					cur_clone.mQual += line.trim();
					cur_clone.mQual += " ";
				}
			}
			if (cur_clone != null)
			{
				if (!empties.contains(cur_clone.mFullName))
				{
					n++;
					batchUpdateCloneQual(cur_clone, ps,n);
				}
			}
			
			if (n % 100 != 0) ps.executeBatch();
			ps.close();
		}		
	}
	private void doLoadLib() throws Exception 
	{
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
		ps.setInt(15, mClonesFullName.size());
		ps.setInt(16, mDefQual);
		ps.setInt(17, mAvgLen);
		ps.setInt(18, mMedLen);
		ps.setBoolean(19, mProp.getProperty("ctglib").equals("1"));
		ps.setString(20, mProp.getProperty("strain"));
		ps.setString(21, mProp.getProperty("prefix"));
		ps.setString(22, mProp.getProperty("orig_libid"));
		ps.setString(23, ""); // set later (search 'parent')
		ps.setString(24, mReps);
		ps.execute();
		ps.close();
		mID = mDB.lastID();
	}
	void batchUpdateCloneQual(Clone c, PreparedStatement ps, int n) throws Exception
	{
		c.checkQual(true);
		ps.setString(1, c.mQual);
		ps.setInt(2, c.mID);

		ps.addBatch();
		if (n % 100 == 0) ps.executeBatch();
	}	
	void batchAddClone(Clone c, PreparedStatement ps, int n) throws Exception
	{
		int sense = 0;
		if (c.mRF == RF.F)
			sense = 1;
		else if (c.mRF == RF.R)
			sense = -1;
		String trimmed = c.mSeq.trim();
		if (c.mSeq != trimmed)
		{
			throw(new Exception(""));
		}
		ps.setString(1, c.mFullName);
		ps.setString(2, c.mOrigName);
		ps.setString(3, mIDStr);
		ps.setInt(4, mID);
		ps.setString(5, c.mSeq);
		// we add it now with default qual values; will be updated after
		ps.setString(6, defQualStr(c.mSeq.length()));
		ps.setInt(7, sense);
		ps.setInt(8, c.mSeq.length());

		ps.addBatch();
		if (n % 100 == 0) ps.executeBatch();
	}
	
	// Once the lib is loaded, go back and get all the autoincrement clone id's,
	// and fill out the mate pair field
	public void matePairs() throws Exception
	{
		if (paired() == 0) return;
		Utils.singleLineMsg(mIDStr + ":assigning mate pairs....");		
		PreparedStatement ps = mDB.prepareStatement("update clone set mate_CID=? where CID=? ");

		for (String cname : mClones.keySet())
		{
			if (mClones.get(cname).containsKey(RF.F)
					&& mClones.get(cname).containsKey(RF.R))
			{
				int cid5 = mClones.get(cname).get(RF.F).mID;
				int cid3 = mClones.get(cname).get(RF.R).mID;
				if (cid5 == 0)
				{
					Log.die(cname + " five-prime not found in database!");
				}
				if (cid3 == 0)
				{
					Log.die(cname + " three-prime not found in database!");
				}
				mClones.get(cname).get(RF.F).mMateID = cid3;
				mClones.get(cname).get(RF.R).mMateID = cid5;
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

	public void delete() throws Exception
	{
		assert (mID > 0);
		Log.indentMsg("removing " + mIDStr,LogLevel.Basic);
		String stmt = "delete from library where LID='" + mID + "'";
		mDB.executeUpdate(stmt);
		initDBFields();
	}
	public Set<String> getAssemblies() throws Exception
	{
		TreeSet<String> ret = new TreeSet<String>();
		ResultSet rs = mDB.executeQuery("select distinct assemblyid from assemlib where lid=" + mID);
		while (rs.next())
		{
			ret.add(rs.getString("assemblyid"));
		}
		return ret;
	}
	public void initDBFields()
	{
		mID = 0;
		mStatus = LibStatus.New;
		mNumESTLoaded = 0;
		mNumAssemblies = 0;
		mTimeLoaded = 0;
		mSize = 0;
	}

	// 
	// These methods called from the assembler
	//
	public Library(DBConn db, String libid) throws Exception
	{
		mDB = db;
		mIDStr = libid;
	}

	public boolean loadFromDB() throws Exception
	{
		ResultSet rs = mDB.executeQuery("select LID,libsize,loaddate,fastafile,defqual,avglen,medlen,ctglib,reps from library where libid='"
						+ mIDStr + "'");
		if (rs.next())
		{
			mID = rs.getInt("LID");
			mNumESTLoaded = rs.getInt("libsize");
			mAvgLen = rs.getInt("avglen");
			mMedLen = rs.getInt("medlen");
			mDefQual = rs.getInt("defqual");
			mLoadDate = rs.getString("loaddate");
			mSeqFile = new File(rs.getString("fastafile"));
			mCtgLib = rs.getBoolean("ctglib");
			mReps = rs.getString("reps").trim();
			if (!mSeqFile.isFile())
			{
				Log.msg(msgStr("Can't find dataset sequence file "	+ mSeqFile.getAbsolutePath()));
			}
			return true;
		}
		rs.close();
		return false;
	}
	// This wasteful because we fill out both the mClone2ID, and the 
	// mClonesFullName objects. This is done because if ONLY the expression
	// data is reloaded, then the second map is not present.
	private void getCloneIDs() throws Exception
	{
		mClone2ID = new TreeMap<String,Integer>();
		ResultSet rs = mDB.executeQuery("select CID,cloneid from clone where LID='" + mID + "'");
		while (rs.next())
		{
			mClone2ID.put(rs.getString("cloneid"),rs.getInt("CID"));
			String name = rs.getString("cloneid");
			if (mClonesFullName.containsKey(name))
			{		
				// If we're only loading expr data, we won't get here
				mClonesFullName.get(name).mID = rs.getInt("CID");
			}
		}
		rs.close();
	}
	public String defQualStr(int size)
	{
		StringBuffer dqstr = new StringBuffer();
		String defQual = String.valueOf(mDefQual);
		for (int i = 1; i <= size; i++)
		{
			dqstr.append(defQual);
			if (i < size)
			{
				dqstr.append(" ");
			}
		}
		
		return dqstr.toString();
	}
	public boolean toBeLoaded()
	{
		return (mAction == LibAction.NewLoad || mAction == LibAction.ReLoad);
	}
	public void collectStats(int tcid) throws Exception
	{
		ResultSet rs = null;
		// singles (including mated with 2 clones)
		rs = mDB.executeQuery("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mID + " and contig.tcid=" + tcid + " and contig.numclones=1");
		rs.first();
		mNumSingle = rs.getInt("count");	
		
		rs = mDB.executeQuery("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mID + " and contig.tcid=" + tcid + " and (contig.numclones=2 and clone.mate_CID > 0)");
		rs.first();
		mNumSingle += rs.getInt("count")/2;
		
		// buried
		rs = mDB.executeQuery("select count(*) as count from contclone join contig on contig.ctgid=contclone.ctgid join clone on clone.cid=contclone.cid " +
												" where clone.lid=" + mID + " and contig.tcid=" + tcid + " and contclone.buried=1");
		rs.first();
		mNumBuried = rs.getInt("count");			
	}

	private void printError(String msg)
	{
		Log.indentMsg("Error -- " + msg, LogLevel.Basic);
	}
	private void printWarn(String msg)
	{
		Log.indentMsg("Warning -- " + msg, LogLevel.Basic);
	}
	// Make sure the expression file refers only to contigs which actually exist in 
	// this library, and to expression libraries or replicates which have been loaded.
	public void checkExp() throws Exception
	{
		TreeSet<String> libs = new TreeSet<String>();
		TreeSet<String> libsWithReps = new TreeSet<String>();
		TreeSet<String> reps = new TreeSet<String>();
		
		// If the library has been prefixed we will prefix the replicate names too and
		// then also add the prefix when we scan the expr file and make sure they match.
		ResultSet rs = mDB.executeQuery("select libid, reps, prefix from library order by libid asc");
		while (rs.next())
		{
			String lib = rs.getString("libid");
			String pfx = rs.getString("prefix");
			libs.add(lib);
			String repList = rs.getString("reps").trim();
			if (!repList.equals(""))
			{
				libsWithReps.add(lib);
				for (String rep : repList.split(","))
				{
					if (!rep.trim().equals(""))
					{
						reps.add(pfx + rep.trim());
					}
					else
					{
						printError("Empty replicate name in replicate list for dataset " + lib + " (list:" + repList + ")");
					}
				}
			}
		}
		
		for (String rep : reps)
		{
			if (libs.contains(rep))
			{   
				printWarn("Counts file contains " + rep + ", which may be both a dataset and replicate name.");
			}
		}
		BufferedReader bwExp = new BufferedReader(new FileReader(mExpFile));
		Log.indentMsg("Check Count data from " + mExpFile.getName(), LogLevel.Basic);

		// Get the library list from the first line, and create new shell libraries if needed. 
		String[] line1 = bwExp.readLine().trim().split("\\s+");
		if (line1.length < 2)
		{
			printError("No datasets found on first line of " + mExpFile.getAbsolutePath());
		}
		
		Vector<String> libOrder = new Vector<String>();
		for (int i = 1; i < line1.length; i++)
		{
			String lib = line1[i].trim();
			if (lib.equals("")) 
			{
				printError("Empty dataset name found on first line of " + mExpFile.getAbsolutePath() + "\nThe file may not be properly space-delimited\n");	
			}
			lib = mPfx + lib;
			if (!libs.contains(lib) && !reps.contains(lib))
			{
				printError("Unknown dataset or replicate " + lib + " found in " + mExpFile.getAbsolutePath());
			}
			if (libsWithReps.contains(lib) )
			{
				Log.msg("Counts file contains " + lib + ", which may be both a dataset and condition name.");
			}
			if (libOrder.contains(lib)) { 
				printError("Duplicate sample ID " + lib + " found in " + mExpFile.getAbsolutePath());
			}
			libOrder.add(lib);					
		}

		mExpLibList.addAll(libOrder);	
		if (mExpLibList.isEmpty())
		{
			printError("Count file has no counts dataset in it (" + mIDStr + ")!!");
		}				
		
		// Libs are fine. Now check the clones.
		getCloneIDs();
		int cnt=0;
		while (bwExp.ready())
		{
			String[] line = bwExp.readLine().trim().split("\\s+");
			if (!mClone2ID.containsKey(nameFix(line[0])))
			{
				if (cnt<3) Log.msg("Unknown sequence " + line[0] + " in count file",LogLevel.Basic);
				if (cnt==3) Log.msg("Unknown sequence " + line[0] + " in count file -- further message will be surpressed",LogLevel.Basic);
				cnt++;
			}
		}
		if (cnt>0) Log.msg(cnt + " unknown sequences in count file",LogLevel.Basic);
		bwExp.close();
	}

	// Load the expression data. It should have already been checked.	
	public void loadExp() throws Exception
	{
		if (mClone2ID == null)
		{
			Log.die("Loading Count file that was not checked (" + mIDStr + ")!!");
		}
		if (mExpLibList.isEmpty())
		{
			Log.die("Loading Count file that has no data (" + mIDStr + ")!!");
		}	
	
		// Delete the expression level entries for the contig clones in this library
		mDB.executeUpdate("delete clone_exp.* from clone_exp, clone where clone.lid='" + 
							mID + "' and clone_exp.cid=clone.cid");
		
		TreeMap<String,Integer> lib2num = new TreeMap<String,Integer>();
		TreeMap<String,Integer> lib2rep = new TreeMap<String,Integer>();
		TreeMap<String,String> rep2lib = new TreeMap<String,String>();
		
		ResultSet rs = mDB.executeQuery("select libid, LID,reps,prefix from library order by libid asc");
		while (rs.next())
		{
			String lib = rs.getString("libid");
			String pfx = rs.getString("prefix");
			int LID = rs.getInt("LID");
			lib2num.put(lib,LID);
			int rnum=1;
			String repList = rs.getString("reps").trim();
			if (!repList.equals(""))
			{
				for (String rep : repList.split(","))
				{
					if (!rep.trim().equals(""))
					{
						lib2rep.put(pfx + rep.trim(), rnum);
						rep2lib.put(pfx + rep.trim(), lib);
					}
					rnum++;
				}
			}
		}
		BufferedReader bwExp = new BufferedReader(new FileReader(mExpFile));
		Log.indentMsg("Load Count data from " + mExpFile.getName(), LogLevel.Basic);

		// Get the library list from the first line, and create new shell libraries if needed. 
		String[] line1 = bwExp.readLine().trim().split("\\s+");
		if (line1.length < 2)
		{
			Log.die("No dataset found on first line of " + mExpFile.getAbsolutePath());
		}
		
		Vector<String> libOrder = new Vector<String>();
		for (int i = 1; i < line1.length; i++)
		{
			String lib = line1[i].trim();
			if (lib.equals("")) 
			{
				Log.die("Empty dataset name found on first line of " + mExpFile.getAbsolutePath() + "\nThe file may not be properly space-delimited\n");	
			}
			lib = mPfx + lib;
			if (!lib2num.containsKey(lib) && !rep2lib.containsKey(lib))
			{
				Log.die("Unknown dataset " + lib + " found in " + mExpFile.getAbsolutePath());
			}
			libOrder.add(lib);					
			// Set this expression lib's parent to this lib
			if (lib2num.containsKey(lib))
			{
				mDB.executeUpdate("update library set parent='" + mIDStr + "' where libid='" + lib + "'");
			}
			else if (rep2lib.containsKey(lib))
			{
				// This will be re-done for each replicate but should be no problem
				String mainlib = rep2lib.get(lib);
				mDB.executeUpdate("update library set parent='" + mIDStr + "' where libid='" + mainlib + "'");
			}			
		}

		// Read the rest of the lines and fill out clone_exp
		String libid="", xLine="", name="";
		int LID=0, rep=0;
		try {
			PreparedStatement ps = mDB.prepareStatement("insert into clone_exp (LID, count,CID,rep) values(?,?,?,?)");
			int nAdd = 1;
			while (bwExp.ready())
			{
				xLine = bwExp.readLine();
				String[] line = xLine.trim().split("\\s+");
				name = nameFix(line[0]);
				if (!mClone2ID.containsKey(name)) continue; // wrote error already in check
				
				int seqID = mClone2ID.get(name);
				
				for (int i = 1; i < line.length && i <= libOrder.size(); i++)
				{
					libid =  libOrder.get(i-1);
					LID = (lib2num.containsKey(libid) ? lib2num.get(libid) : lib2num.get(rep2lib.get(libid)));
					rep = (lib2rep.containsKey(libid) ? lib2rep.get(libid) : 0);
						
					// a mapper called 'Salmon' outputs real numbers
					double dcount = Double.parseDouble(line[i]);
					int count = (int) (dcount+0.5);
					
					ps.setInt(1, LID);
					ps.setInt(2, count);
					ps.setInt(3, seqID);
					ps.setInt(4, rep);
					ps.addBatch();
					
					nAdd++;
					if (nAdd%10000==0) 
					{
						Utils.singleLineMsg(nAdd + " loaded          ");
						ps.executeBatch();		
					}
				}
			}
			if (nAdd>0) ps.executeBatch();
			bwExp.close();
			
			mDB.executeUpdate("update library set expdate=now(),exploaded=1 where lid=" + mID);
		}
		catch (Exception e) {
			System.err.println();
			Log.exception(e);
			Log.msg("Fatal error reading file - exiting",LogLevel.Basic);
			Log.die(e.getMessage());
		}
	}
}
