package sng.assem;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import sng.assem.enums.*;
import sng.assem.helpers.*;
import util.database.DBConn;


// Parse and store ace file output of cap3.
// Knows how to build Contig and Subcontig classes, and load them to the database. 

public class AceFile
{
	File mFile;
	File mSingFile;
	Vector<SubContig> mContigs;
	Pattern mSingPat;
	Pattern mCOPat;
	Pattern mAFPat;
	Pattern mRDPat;
	Str2Obj<Clone> mName2Clone;
	ID2Obj<Clone> mID2Clone;
	String mSource;
	OKStatus mOK = OKStatus.OK;
	String mOKInfo = "";
	static File mCapFailLog;
	static int mNumCapFails;
	int mThisID = 0; // either clique id or edge id being worked on
	int mAID;
	String mAssemName;
	DBConn mDB;
	
	public AceFile(int aid, String aname, File file,ID2Obj<Clone> id2clone,Str2Obj<Clone> name2clone,String source,DBConn db) throws Exception
	{
		mAID = aid;
		mAssemName = aname;
		mFile = file;
		mName2Clone = name2clone;
		mID2Clone = id2clone;
		mSource = source;
		mContigs = new Vector<SubContig>();
		mDB = db;
		
		initCapFailLog();
		
		if (!mFile.exists())
		{
			mOK = OKStatus.CapFailure;
			mOKInfo = "No ace file"; 
			Log.msg("Cap Fail: " + file.getAbsolutePath(),LogLevel.Detail);
			FileWriter fw = new FileWriter(mCapFailLog,true);
			fw.write(file.getAbsolutePath());
			fw.write("\n");
			fw.flush();
			fw.close();
			mNumCapFails++;
			return;
		}
		mCOPat = Pattern.compile("CO\\s+(\\S+)\\s+\\d+\\s+(\\d+).*");
		mAFPat = Pattern.compile("AF\\s+(\\S+)\\s+([UC])\\s+(-?\\d+).*");
		mRDPat = Pattern.compile("RD\\s+(\\S+).*");
		mSingPat = Pattern.compile(">(\\S+).*");
	}
	public void doParse(boolean immediateUpload, int TCID) throws Exception
	{
		parseAce(immediateUpload, TCID);
		parseSing();
		for (SubContig c : mContigs) 
		{
			c.setPairs();
			c.setRF();
			c.setUC();
			c.setCloneEnds();
		}	
	}

	private void initCapFailLog() throws Exception
	{
		if (mCapFailLog == null)
		{
			String path = Log.logFile.getAbsolutePath() + ".capfail";
			File f = new File(path);
			if (f.exists()) f.delete();
			mCapFailLog = f;
		}
	}
	// Parse the ace file into contigs
	// We need the CO sections with the contig consensus, and the AF sections with the read locations,
	// And the BQ sections with the qual values, and the RD sections which give the gaps
	private void parseAce(boolean immediateUpload, int TCID) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(mFile));
		SubContig ctg = null;
		while (br.ready())
		{
			String line = br.readLine();
			Matcher matchCO = mCOPat.matcher(line);
			Matcher matchAF = mAFPat.matcher(line);
			Matcher matchRD = mRDPat.matcher(line);
			if (matchCO.matches())
			{
				if (immediateUpload && ctg != null)
				{
					doImmediateUpload(ctg,TCID);
					ctg.clear();
					ctg = null;
				}
				String ctgname = matchCO.group(1);
				ctg = new SubContig(mID2Clone,mAID, mAssemName);
				ctg.mIDStr = ctgname;
				//Utils.singleLineMsg(ctgname);
				if (!immediateUpload)
				{
					mContigs.add(ctg);
				}
				// read the sequence block
				StringBuilder ccsB = new StringBuilder();
				while (br.ready())
				{
					line = br.readLine();
					line.trim();
					if (line.length() == 0) break;
					ccsB.append(line);
				}
				ctg.mCCS = ccsB.toString().toUpperCase();
				ctg.mCCSLen = ctg.mCCS.length();
				ccsB = null;
			}
			if (line.equals("BQ"))
			{
				// read the qual block
				StringBuilder qualB = new StringBuilder();
				while (br.ready())
				{
					line = br.readLine();
					line.trim();
					if (line.length() == 0) break;
					qualB.append(line);
				}
				ctg.mQual = qualB.toString();
				qualB = null;
			}			
			else if (matchAF.matches())
			{
				if (ctg == null)
				{
					Log.die("Error parsing .ace file: clone section found but no contig! file:" + mFile.getAbsolutePath() );
				}
				String name = matchAF.group(1);
				UC uc = UC.valueOf(matchAF.group(2));
				int start = Integer.parseInt(matchAF.group(3));
				if (!mName2Clone.containsKey(name))
				{
					Log.die("Error parsing ace file: found unknown clone " + name + " file:" + mFile.getAbsolutePath());
				}
				Clone clone = mName2Clone.get(name);
				clone.mStart = start;
				clone.mEnd = 0;
				clone.mUC = uc;
				ctg.addClone(clone);
			}
			else if (matchRD.matches())
			{
				String name = matchRD.group(1);
				Clone clone = mName2Clone.get(name);
				
				StringBuilder gapsB = new StringBuilder();
				StringBuilder readB = new StringBuilder();
				while (br.ready())
				{
					line = br.readLine();
					line.trim();
					if (line.length() == 0) break;
					readB.append(line);
				}		
				int ngaps = 0;
				String read = readB.toString().toUpperCase();
				for (int i = 0; i < read.length(); i++)
				{
					if (read.charAt(i) == '*')
					{
						// Gaps are stored as being after the nth character in the read,
						// with gaps removed. So there is an increasing offset as we 
						// count through the gapped read. 
						gapsB.append((i-ngaps) + " ");
						ngaps++;
					}
				}
				int nMis = 0;
				int nExtras = 0;
				int offset = clone.mStart;				
				for (int i = 0; i < read.length(); i++)
				{
					int tpos = i + offset - 1;
					if (tpos < 0 || tpos >= ctg.mCCS.length()) continue;
					if (read.charAt(i) != '*' && ctg.mCCS.charAt(tpos) == '*')
					{
						nExtras++;
					}
					if (read.charAt(i) == '*' || ctg.mCCS.charAt(tpos) == '*') continue;
					if (read.charAt(i) != ctg.mCCS.charAt(tpos) ) 
					{
						nMis++;
					}
				}
				clone.mNExtras = nExtras;
				clone.mNMis = nMis;
				clone.mNGaps = ngaps;
				clone.mGaps = gapsB.toString().trim();
//				if (read.length() < 20*nMis)
//				{
//					Utils.termOut(clone.mFullName + " " + nMis + " mismatches, length:" + read.length() + "\n");
//				}
				read = "";readB = null; gapsB = null;
			}			
		}

		br.close();
		
		for (SubContig sc : mContigs)
		{
			sc.mSource = mSource;
		}
	}
	// parse the singlet file, each to its own subcontig
	private void parseSing() throws Exception
	{
		mSingFile = new File(mFile.getParentFile(), mFile.getName().replace(".ace", ".singlets"));
		if (!mSingFile.isFile() || mSingFile.length() == 0)
		{
			return;
		}
		BufferedReader sr = new BufferedReader(new FileReader(mSingFile));
		SubContig sc = null;
		while (sr.ready())
		{
			String line = sr.readLine();
			Matcher m = mSingPat.matcher(line);
			if (m.matches())
			{
				String cname = m.group(1);
				Clone c = mName2Clone.get(cname);
				sc = new SubContig(mID2Clone,mAID,mAssemName,c);
				mContigs.add(sc);
				sc.mQual = c.getQualFromDB(mDB);
			}
			else
			{
				// should be part of sequence block
				if (sc == null)
				{
					Log.die("Cap3 singleton file parse error: " + mSingFile.getAbsolutePath());
				}
				line.trim();
				sc.mCCS += line;							
			}
		}
		sr.close();
	}

	boolean OK_CTG(int num_clones_in, boolean pairedEdge, boolean strictMerge, boolean bHeuristics,boolean bTwoBridge,boolean bNoTest4,MatePair[] matePairs) throws Exception
	{
		if (mOK == OKStatus.CapFailure)
		{
			return false;
		}

		// we want all the clones assembled, into one or two contigs (two only if paired to start)
		if (!pairedEdge && mContigs.size() > 1) 
		{
			mOK = OKStatus.UnpairedMultCtg;
			return false;
		}

		if (mContigs.size() > 2) 
		{
			mOK = OKStatus.TooManyCtg;
			mOKInfo = String.valueOf(mContigs.size());
			return false;
		}
		
		// we don't want any clones hanging way off the end, or the beginning
		// Note, duplicated into SubContig.hasOverhangs()
		int maxHang = 20;
		for (SubContig sc : mContigs)
		{			
			int ccsLen = sc.mCCS.length();
			for (int cid : sc.mAllClonesID)
			{
				Clone c = mID2Clone.get(cid);
//				if (c.mName.equalsIgnoreCase("dwg_02580"))
//				{
//					System.err.println("CLONE " + c.mName + " start: " + c.mStart + " len:" + c.mSeqLen + " conslen:" + ccsLen  + " nclones:" + sc.mNumClones );
//				}
				if (c.mStart + c.mSeqLen > ccsLen + maxHang)
				{
					mOK = OKStatus.Hang;
					mOKInfo = c.mFullName;
					return false;
				}
				if (c.mStart < -maxHang)
				{
					mOK = OKStatus.Hang;
					mOKInfo = c.mFullName;
					return false;
				}
			}			
		}
		
		if (mContigs.size() == 1)
		{

			mOK = OKStatus.OK;
			mOKInfo = "one contig";
			return true;
		}	

		// we don't want any mate pairs embedded in a single subcontig, with
		// inconsistent (matching) orientation
		
		for (SubContig sc : mContigs)
		{
			for (int idx : sc.mPairsIdx)
			{
				MatePair p = matePairs[idx];
				if (sc.mAllClonesID.contains(p.m3Prime.mID) && 
						sc.mAllClonesID.contains(p.m5Prime.mID))
				{
					UC uc1 = p.m3Prime.mUC;
					UC uc2 = p.m5Prime.mUC;
					if (uc1 == uc2)
					{
						mOK = OKStatus.PairSameOrient;
						mOKInfo = p.idStr();
						return false;
					}
				}
			}
		}
	
		SubContig sc1 = mContigs.get(0);
		SubContig sc2 = mContigs.get(1);
		
		// Require 1 (or 2) mate pair bridges between them

		int minMates = (bTwoBridge ? 2 : 1);
		int nMates = 0; // mates split across the subcontigs
		boolean good = false;
		for (int idx : sc1.mPairsIdx)
		{
			if (sc2.mPairsIdx.contains(idx))
			{
				nMates++;
				if (nMates >= minMates)
				{
					good = true;
				}
			}
		}
		if (!good)
		{
			mOK = OKStatus.TooFewBridge;
			mOKInfo = nMates + " found,require " + minMates;
			return false;
		}		
		
		
		// If the contigs are unmixed orientation, we're good to go!
		if (!sc1.mixedUC() && !sc2.mixedUC())
		{
			mOK = OKStatus.OK;
			mOKInfo = "unmixed u/c";
			return true;
		}
		
		if (!bHeuristics)
		{
			mOK = OKStatus.OK;
			mOKInfo = "heuristics disabled";
			return true;
		}
		
		// If it's not the last run, we will be strict and reject now.
		if (strictMerge)
		{
			mOK = OKStatus.MixedStrictMerge;
			mOKInfo = "mixed u/c";
			return false;
		}

		// Two contigs, at least one is mixed. Now we have several tests.
		
		int numEST = sc1.mAllClonesID.size() + sc2.mAllClonesID.size();
		
		// Test 0. 
		// Disallow a single bridge if at least 10 est.
		if (numEST >= 10)
		{
			if (nMates == 1)
			{
				mOK = OKStatus.MixedSingleBridge;
				mOKInfo = "";
				return false;
			}
		}
		
		// Test 1.
		// Minimum ratio of bridging to non-bridging mate pairs
		int nSameCtgPairs = sc1.mContainedPairsIdx.size() + sc2.mContainedPairsIdx.size();
		if (nSameCtgPairs > 0)
		{
			Float minRatio = (float)(nMates == 1 ? .33 : .1); 
			Float ratio =  ((float)nMates)/((float)nSameCtgPairs);
			if (ratio <= minRatio)
			{
				mOK = OKStatus.TooManyNonBridgeMates;
				mOKInfo = "ratio:" + ratio.toString();
				return false;
			}
		}

		// Test 2.
		// One contig unmixed, the other too mixed
		if (sc1.numEST() > 15 && sc1.mixedUC() && !sc2.mixedUC())
		{
			Float maxRatio = (float)(sc1.mixedRF() ? .1 : .5);
			Float ratio = sc1.ratioUC();
			if (ratio > maxRatio)
			{
				mOK = OKStatus.MixedUCRatio;
				mOKInfo = sc1.mRF.toString() + " ratio:" + ratio.toString() + " max:" + maxRatio.toString();
				return false;
			}
		}
		if (sc2.numEST() > 15 && sc2.mixedUC() && !sc1.mixedUC())
		{
			Float maxRatio = (float)(sc2.mixedRF() ? .1 : .5);
			Float ratio = sc2.ratioUC();
			if (ratio > maxRatio)
			{
				mOK = OKStatus.MixedUCRatio;
				mOKInfo = sc2.mRF.toString() + " ratio:" + ratio.toString() + " max:" + maxRatio.toString();
				return false;
			}
		}		
		// Test 3.
		// Unclean separation between F/R.
		if ( (sc1.mCountF > sc1.mCountR && sc1.mCountR > sc2.mCountR) ||
				(sc1.mCountR > sc1.mCountF && sc1.mCountF > sc2.mCountF) )
		{
			mOK = OKStatus.RFImbalance;	
			mOKInfo = sc1.mRF.toString();
			return false;
		}
		if ( (sc2.mCountF > sc2.mCountR && sc2.mCountR > sc1.mCountR) ||
				(sc2.mCountR > sc2.mCountF && sc2.mCountF > sc1.mCountF) )
		{
			mOK = OKStatus.RFImbalance;	
			mOKInfo = sc2.mRF.toString();
			return false;
		}
		
		// Test 4.
		// Too many uncomplemented clones of both R and F
		if (!bNoTest4)
		{	
			if (sc1.mCountUF > 10 && sc1.mCountUR > 10)
			{
				mOK = OKStatus.TooManyUncompRF;
				mOKInfo = sc1.mRF.toString();
				return false;				
			}
			if (sc2.mCountUF > 10 && sc2.mCountUR > 10)
			{
				mOK = OKStatus.TooManyUncompRF;
				mOKInfo = sc2.mRF.toString();
				return false;				
			}			
		}
		mOK = OKStatus.OK;
		return true;
	}	
	public void clear() throws Exception
	{
		// Don't fully clear if contigs were ok because they are being used elsewhere!
		// However, safe to clear the sequences since they're re-read from DB when used.
		if (mOK != OKStatus.OK)
		{
			for (SubContig sc : mContigs)
			{
				sc.clear();
			}
			mContigs.clear();
			mContigs = null;
		}
		else
		{
			for (SubContig sc : mContigs)
			{
				sc.clearSeqs();
			}
			
		}
	}
	private void doImmediateUpload(SubContig sctg1, int TCID) throws Exception
	{
		sctg1.mTCID = TCID;
		
		sctg1.doUpload(mDB,sctg1.mIDStr,false);
		sctg1.mCCS = "";
		sctg1.mQual = "";
			
		Contig ctg = new Contig(mAID,sctg1,null);
		ctg.upload(mDB,TCID);

	}
	public void dumpContigs()
	{
		System.err.println("contigs in .ace file:");
		for (SubContig ctg : mContigs)
		{
			int size = ctg.mAllClonesID.size();
			System.err.println(size + " EST");
		}
	}
}
