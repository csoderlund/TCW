package sng.assem;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Vector;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.BufferedWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import sng.assem.enums.*;
import sng.assem.helpers.*;
import util.database.DBConn;
import util.database.Globalx;

// Single contig; direct result of a cap assembly.
// Corresponds to the 'contig' database table.
// L/R pairs of SubContigs are tracked by the Contig object.

public class SubContig
{
	String mCCS = "";
	int mCCSLen = 0;  // This is set in AssemMain.loadContigs, when CCS itself is not loaded.
	String mQual = "";
	int mStart;
	int mEnd;
	TreeSet<Integer> mLonersID;		// unpaired
	TreeSet<Integer> mAllClonesID;
	TreeSet<Integer> mBuriedClonesID;
	TreeSet<Integer> mPairsIdx; 	// pairs which have at least one clone in the contig!!
	TreeSet<Integer> mContainedPairsIdx; // pairs which have both clones in the contig!!
	String mSource = "";
	SubContig mMate;
	int mMateID;
	int mID = 0;
	String mAIDStr = "";
	String mIDStr = "";
	int mCTGID = 0; 	// its containing supercontig (i.e., the ASM_scontig table from the database)
	Contig mContig = null; 
	RF mRF = RF.Unk;
	UC mUC;
	int mNCount = 0;
	int mNStart = 0;
	int mMinGapDepth;
	ID2Obj<Clone> mID2Clone;
	int mAID;
	
	int mNumClones = 0;
	int mTCID = 0;
	int mCountU = 0;
	int mCountC = 0;
	int mCountR = 0;
	int mCountF = 0;
	int mCountUR = 0;
	int mCountUF = 0;
	boolean mDoUpload = false;
	int mMateNum = -1;
	
	int[] mDepthMap;
	Clone[] mBuryCloneList;
	int[] mMaxR;
	TreeMap<Integer,Integer> mID2Idx;
	public SubContig(ID2Obj<Clone> id2clone, int aid, String astr)
	{
		mID2Clone = id2clone;
		mAID = aid;
		mAIDStr = astr;
		mLonersID = new TreeSet<Integer>();
		mAllClonesID = new TreeSet<Integer>();
		mBuriedClonesID = new TreeSet<Integer>();
		mPairsIdx = new TreeSet<Integer>();
		mContainedPairsIdx = new TreeSet<Integer>();
	}
	// Called when making a contig from a single clone
	public SubContig(ID2Obj<Clone> id2clone, int aid, String astr, Clone c)
	{
		this(id2clone,aid,astr);
		mSource = "Single clone ctg from:" + c.mFullName;
		Log.msg(mSource,LogLevel.Dbg);
		mAllClonesID.add(c.mID);	
		c.mUC = UC.U; // it's the only clone, hence it is U
	}
	// The depth map is used for deciding which clones can be buried.
	public int buildDepthMap() throws Exception
	{
		int maxDepth = 0;
		mDepthMap = null;
		int size = 0;
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			size = Math.max(size, c.mEnd);
		}
		mDepthMap = new int[size+1];
		for (int i = 0; i <= size; i++) 
		{
			mDepthMap[i] = 0;	
		}
		for (int cid : mAllClonesID)
		{
			if (mBuriedClonesID.contains(cid)) continue;
			Clone c = mID2Clone.get(cid);
			for (int i = Math.max(0,c.mStart); i <= c.mEnd; i++)
			{
				mDepthMap[i]++;
			}
		}
		for (int i = 0; i <= size; i++) 
		{
			maxDepth = Math.max(maxDepth,mDepthMap[i]);	
		}
		return maxDepth;
	}
	public void destroyDepthMap()
	{
		mDepthMap = null;
	}
	
	// Create a clone list ordered by left end, and secondarily by right end. 
	// A clone can only be buried in a parent which is higher on this list, i.e., which
	// starts further to the left. 
	// The buried clone MAY extend to the right farther than the parent, subject to maxHang parameter.
	public void leftOrderClones() throws Exception
	{
		int size = mAllClonesID.size() - mBuriedClonesID.size();
		mBuryCloneList = new Clone[size];
		int i = 0;
		for (int cid : mAllClonesID)
		{
			if (mBuriedClonesID.contains(cid)) continue;
			mBuryCloneList[i] = mID2Clone.get(cid);
			i++;
		}
		Arrays.sort(mBuryCloneList,new Clone.CtgPosCmp());
		mID2Idx = new TreeMap<Integer,Integer>();
		mMaxR = new int[mBuryCloneList.length];
		int maxR = 0;
		for (i = 0; i < mBuryCloneList.length; i++)
		{
			mID2Idx.put(mBuryCloneList[i].mID,i);
			maxR = Math.max(maxR, mBuryCloneList[i].mEnd);
			mMaxR[i] = maxR;
		}
	}
	public void clearLeftOrder()
	{
		mBuryCloneList = null;
		mMaxR = null;
		mID2Idx.clear();
		mID2Idx = null;
	}
	public void buryClone(int cid) throws Exception
	{
		if (mBuriedClonesID.contains(cid)) 
		{
			throw(new Exception(idStr() + ":Burying already buried clone " + cid));
		}
		if (!mAllClonesID.contains(cid)) 
		{
			throw(new Exception(idStr() + ":Burying noncontained clone " + cid));
		}		
		Clone c = mID2Clone.get(cid);
		for (int i = Math.max(0,c.mStart); i <= c.mEnd; i++)
		{
			mDepthMap[i]--;
		}
		mBuriedClonesID.add(cid);
	}
	public void clearBuries() throws Exception
	{
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			c.mParent = null;
			//c.mParentID = 0;
		}
		mBuriedClonesID.clear();
	}
	// Clone is buryable if the depth is greater than minDepth across its whole length,
	// and if it doesn't overhang the consensus end by more than maxHang. 
	public boolean buryable(int cid, int minDepth, int maxHang) throws Exception
	{
		boolean ret = true;
		if (!mAllClonesID.contains(cid)) 
		{
			throw(new Exception(idStr() + ":Bury check noncontained clone " + cid));
		}		
		Clone c = mID2Clone.get(cid);
		int start = Math.max(0, c.mStart);
		int end = start + c.mSeqLen + c.mNGaps;
		if (end <= mCCS.length() + maxHang) 
		{
			int min = mDepthMap[start];		
			for (int i = start; i <= c.mEnd; i++)
			{
				min = Math.min(min,mDepthMap[i]);
				if (mDepthMap[i] <= minDepth)
				{
					ret = false;
					break;
				}
			}	
		}
		else
		{
			// don't bury any clone that goes too far beyond consensus end
			ret = false;	
		}
		return ret;
	}
	// Given a clone is buryable according to depth of coverage (see previous function), now
	// scan up the bury list to try to actually find a clone to bury it in. (This may fail).
	// If we find one, require that the mates bury also.
	//
	// If return is true, the parent is set in the clone object, and
	// same for the mate of this clone, if any
	//
	public boolean findBury(int cid, int hang) throws Exception
	{
		boolean ret = false;
		int idx = mID2Idx.get(cid);
		Clone child = mID2Clone.get(cid);
		UC childUC = child.mUC;
		for (int i = idx - 1; i > 0; i--)
		{
			Clone parent = mBuryCloneList[i];
			if (parent.mID == child.mMateID) continue; 	// We aren't going to bury a clone in its mate; plus, this gives rise to loops
			if (parent.mUC != childUC) continue; 		// We may want to relax this condition for 454.
			if (parent.mParent != null) continue; 		// We aren't going to bury more than 1 deep because loops can arise when mate pairs appear in varying orders
			if (child.mEnd - mMaxR[i] > hang) break; 	// Don't bury in a parent that is too much shorter
			if (child.isBuryable(parent,hang))
			{
				if (child.mMate != null)
				{
					if (parent.mMate == null) continue;
					if (parent.mMate.mParent != null) continue;
					if (child.mMate.mSCID != parent.mMate.mSCID) continue;
					if (!child.mMate.isBuryable(parent.mMate, hang)) continue;
				}
				ret = true;
				child.mParent = parent;
				//child.mParentID = parent.mID;
				if (child.mMate != null)
				{
					if (parent.mMate == null)
					{
						throw(new Exception("paired child " + child.mFullName + " buried in unpaired parent " + parent.mFullName));
					}
					child.mMate.mParent = parent.mMate;
					//child.mMate.mParentID = parent.mMateID;
				}
			}
		}
		if (ret)
		{
			//Log.msg(child.mFullName + " (" + child.mStart + "," + child.mEnd + ") buried in " + child.mParent.mFullName + " (" + child.mParent.mStart + "," + child.mParent.mEnd + ")",LogLevel.Dbg);
			if (child.mMate != null)
			{
				//Log.msg(child.mMate.mFullName + " (" + child.mMate.mStart + "," + child.mMate.mEnd + ") buried in " + child.mMate.mParent.mFullName + " (" + child.mMate.mParent.mStart + "," + child.mMate.mParent.mEnd + ")",LogLevel.Dbg);				
			}
		}
		return ret;
	}


	public void addClone(Clone c) throws Exception
	{
		if (mAllClonesID.contains(c.mID))
		{
			Log.msg("duplicate clone " + c.mFullName + " being added to subctg " + idStr(),LogLevel.Detail);
		}
		else
		{
			mAllClonesID.add(c.mID);
		}
	}
	public int numClones() 
	{
		// the AllClones object isn't always filled out, e.g. out beginning of doSNP. 
		return (mAllClonesID.size() > 0 ? mAllClonesID.size() : mNumClones);
	}
	public int countPaired(SubContig ctg2) throws Exception
	{
		int count = 0;
		for (Integer id : mAllClonesID)
		{
			Clone c = mID2Clone.get(id);
			if (c.mMate == null)
			{
				throw(new Exception("Unmated clone " + c.mFullName + " in mated contig"));
			}
			int id2 = c.mMate.mID;
			if (ctg2.mAllClonesID.contains(id2))
			{
				count++;
			}
		}
		return count;
	}
	public boolean checkFixPaired(SubContig ctg2) throws Exception
	{
		// Check that this contig forms a mated pair with the given one	
		// and throw out clones which don't match.
		// This is used during the clique formation when we demand exact pairing
		TreeSet<Integer> discardID = new TreeSet<Integer>();
		for (Integer id : mAllClonesID)
		{
			Clone c = mID2Clone.get(id);
			if (c.mMate == null)
			{
				throw(new Exception("Unmated clone " + c.mFullName + " in mated contig"));
			}
			int id2 = c.mMate.mID;
			if (!ctg2.mAllClonesID.contains(id2))
			{
				Log.msg("Discard " + c.mFullName,LogLevel.Detail);
				discardID.add(id);
			}
		}
		mAllClonesID.removeAll(discardID);
		TreeSet<Integer> discardID2 = new TreeSet<Integer>();
		for (Integer id2 : ctg2.mAllClonesID)
		{
			Clone c2 = mID2Clone.get(id2);
			if (c2.mMate == null)
			{
				throw(new Exception("Unmated clone " + c2.mFullName + " in mated contig"));
			}
			int id = c2.mMate.mID;
			if (!mAllClonesID.contains(id))
			{
				Log.msg("Discard " + c2.mFullName,LogLevel.Detail);
				discardID2.add(id2);
			}			
		}		
		ctg2.mAllClonesID.removeAll(discardID2);		
		
		setPairs();
		ctg2.setPairs();

		// No discards ==> an ok mated pair
		if (discardID.size() == 0 && discardID2.size() == 0)
		{
			setRF();
			ctg2.setRF();
			if (mRF == ctg2.mRF || mRF == RF.Unk || ctg2.mRF == RF.Unk)
			{
				Log.msg("!! contig pair has mismatched directions " + mRF.toString() + "," + ctg2.mRF.toString(),LogLevel.Detail);
			}
			mMate = ctg2;
			ctg2.mMate = this;
			return true;
		}
		return false;
	}
	public boolean checkFixSelfPaired() throws Exception
	{
		// Check that this contig is built from mate pairs	
		// and throw out ones whose mate was not included.
		// Used during the clique formation
		TreeSet<Integer> discardID = new TreeSet<Integer>();
		for (Integer id : mAllClonesID)
		{
			Clone c = mID2Clone.get(id);
			if (c.mMate == null)
			{
				throw(new Exception("Unmated clone " + c.mFullName + " in mated contig"));
			}
			int id2 = c.mMate.mID;
			if (!mAllClonesID.contains(id2))
			{
				discardID.add(id);
			}
		}
		mAllClonesID.removeAll(discardID);

		setPairs();

		// No discards ==> an ok mated pair
		if (discardID.size() == 0 )
		{
			return true;
		}
		return false;
	}	
	public boolean checkSelfSinglePaired() throws Exception
	{
		// The contig should contain exactly a clone and its mate.
		// used during clique formation
		if (mAllClonesID.size() == 2)
		{
			Integer id1 = mAllClonesID.first();
			Integer id2 = mID2Clone.get(id1).mMateID;
			if (mAllClonesID.contains(id2))
			{
				return true;
			}
		}
		return false;
	}
	public void validate() throws Exception
	{
		int nqvals = mQual.split("\\s+").length;
		int slen = mCCS.replace(Globalx.assmGap, "").length();
		if (nqvals != slen)
		{
			throw(new Exception("Invalid contig, seq len " + slen + " qual len " + nqvals));
		}
	}
	public void doUpload(DBConn db, String ctgName, boolean includeBuried) throws Exception
	{		
		setRF();
		validate();

		int nclones = mAllClonesID.size();
		
		if (nclones == 0)
		{
			throw(new Exception("Result contig has 0 clones!"));	
		}

		PreparedStatement ps = null; 
		ps = db.prepareStatement("insert into contig (notes, AID,contigid,assemblyid,numclones," 
				+ "consensus,quality,consensus_bases,frpairs,est_5_prime,"
				+ "est_3_prime,est_loners,orient,rftype,TCID,has_ns,nstart)" +
				" values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		if (Utils.isDebug())
		{
			ps.setString(1, mSource);
		}
		else
		{
			ps.setString(1,""); 
		}
		ps.setInt(2, mAID);
		ps.setString(3, "temp");
		ps.setString(4, mAIDStr);
		ps.setInt(5, mAllClonesID.size());
		ps.setString(6, mCCS);
		ps.setString(7, mQual);
		ps.setInt(8, mCCS.length());
		ps.setInt(9, mPairsIdx.size());
		ps.setInt(10, mCountF);
		ps.setInt(11, mCountR);
		ps.setInt(12, mLonersID.size());
		ps.setString(13, "");
		ps.setString(14, mRF.toString());
		ps.setInt(15,mTCID);
		ps.setInt(16,(mNCount > 0 ? 1 : 0));
		ps.setInt(17,mNStart);
		
		ps.execute();
		ps.close();
		mID = db.lastID();

		mIDStr = (ctgName.equals("") ? "CTG" + mID : ctgName);

		db.executeUpdate("update contig set contigid='" + mIDStr + "' where CTGID=" + mID  );
		
		ps = db.prepareStatement("insert into contclone (CTGID,contigid,extras,CID,cloneid,orient,leftpos, gaps, ngaps,buried,mismatch,numex) " +
				" values(" + mID + ",'" + mIDStr + "','',?,?,?,?,?,?,?,?,?)");
		for (int id : mAllClonesID)
		{
			Clone c = mID2Clone.get(id);
			c.mSCID = mID;
			c.mSC = this;
			if (includeBuried || !c.mBuried)
			{
				ps.setInt(1,c.mID);
				ps.setString(2,c.mFullName);
				ps.setString(3, c.mUC.toString());
				ps.setInt(4, c.mStart);
				String gaps = c.mGaps;
				int ngaps = c.mNGaps;
				ps.setString(5,gaps);
				ps.setInt(6,ngaps);
				ps.setInt(7,c.mBuried ? 1 : 0);
				ps.setInt(8,c.mNMis);
				ps.setInt(9,c.mNExtras);
				ps.addBatch();
			
			}
		}
		ps.executeBatch();
		ps.close();
	}
	public void setCloneEnds() throws Exception
	{
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			c.mEnd = Math.min(c.mStart + c.mSeqLen + c.mNGaps - 1,mCCS.length());
		}
	}
	// For one-clone subcontigs, set the seq and qual from the clone
	// needed because at the point where we create these, the clone seq/qual
	// entries are blank to conserve memory
	public void seqQualFromClone(DBConn db) throws Exception
	{
		if (mAllClonesID.size() != 1) throw(new Exception("not a one-clone subcontig : " + mID));
		int cid = mAllClonesID.first();
		ResultSet rs = db.executeQuery("select sequence, quality from clone where cid=" + cid);
		rs.next();
		mCCS = rs.getString("sequence");
		mQual = rs.getString("quality");
		rs.close();
	}

	public int countUC(UC uc) throws Exception
	{
		int count = 0;
		for (int id : mAllClonesID)
		{
			if (mID2Clone.get(id).mUC == uc)
			{
				count++;
			}
		}
		return count;
	}	
	// set the contigs R/F status by majority rule of its clones
	public void setRF()  throws Exception
	{
		for (int id : mAllClonesID)
		{
			if (mID2Clone.get(id).mRF == RF.F)
			{
				mCountF++;
				if (mID2Clone.get(id).mUC == UC.U)
				{
					mCountUF++;
				}
			}
			else if (mID2Clone.get(id).mRF == RF.R)
			{
				mCountR++;
				if (mID2Clone.get(id).mUC == UC.U)
				{
					mCountUR++;
				}				
			}
		}
		
		if (mCountF > 0 && mCountR > 0) mRF = RF.Mix;
		if (mCountF > mCountR) mRF = RF.F;
		else if (mCountR > mCountF) mRF = RF.R;
	}
	public boolean mixedRF()
	{
		if (mCountF > 0 && mCountR > 0) return true;
		return false;
	}
	// see if the contig is complemented
	public void setUC() throws Exception
	{
		mCountU = countUC(UC.U);
		mCountC = countUC(UC.C);
		
		if (mCountU >= mCountC) mUC = UC.U;
		else mUC = UC.C;
	}	
	public boolean mixedUC() throws Exception
	{
		setUC();
		if (mCountU > 0 && mCountC > 0) return true;
		return false;
	}
	public float ratioUC()
	{
		if (mCountU == 0 || mCountC == 0) return 0;
		return (mCountU < mCountC ? mCountU/mCountC : mCountC/mCountU);
	}
	public int numEST()
	{
		return mAllClonesID.size();
	}
	public void setMate(SubContig sc, DBConn db) throws Exception
	{
		mMate = sc;
		if (sc != null) 
		{	
			mMateID = sc.mID;
			setMateID(db);
		}
	}
	public void setMateID(DBConn db) throws Exception
	{
		db.executeUpdate("update contig set mate_CTGID='" + mMate.mID + "' where CTGID='" + mID + "'");
	}
	public int writeClones(BufferedWriter w, BufferedWriter qw, DBConn db, 
				HashSet<SubContig> ctgWritten ) throws Exception
	{
		int numWritten = 0;
		if (ctgWritten.contains(this)) return 0;
		ctgWritten.add(this);
		
		
		ResultSet rs = db.executeQuery("select clone.cid, clone.cloneid, clone.sequence, clone.quality " +
				" from clone join contclone on clone.CID=contclone.CID where contclone.CTGID='" + mID + "' and buried=0" );	
		while (rs.next())
		{
			w.write(">" + rs.getString("clone.cloneid") + "\n");
			w.write(rs.getString("clone.sequence").replace(Globalx.assmGap,"") + "\n");
			qw.write(">" + rs.getString("clone.cloneid") + "\n");
			qw.write(rs.getString("clone.quality") + "\n");
			numWritten++;
		}
		rs.close();
		w.flush();
		qw.flush();
		return numWritten;
	}
	public String idStr()
	{		
		return "" + mIDStr + "(" + mRF.toString() + ")";
	}
	
	// Load the clone sequences and some other fields needed for finalizing the contigs. 
	public void loadClones(DBConn db) throws Exception
	{
		mAllClonesID.clear();
		mBuriedClonesID.clear();
		ResultSet rs = db.executeQuery("select contclone.CID,orient,leftpos,gaps,ngaps,buried,clone.sequence from contclone join clone on clone.cid=contclone.cid where CTGID=" + mID );
		while (rs.next())
		{
			int CID = rs.getInt("CID");
			UC uc = UC.valueOf(rs.getString("orient"));
			int lpos = rs.getInt("leftpos");
			int ngaps = rs.getInt("ngaps");
			String gaps = rs.getString("gaps");
			String sequence = rs.getString("sequence");
			boolean buried = (rs.getInt("buried") == 1 ? true : false);
			
			mAllClonesID.add(CID);
			if (buried)
			{
				mBuriedClonesID.add(CID);
			}
			Clone c = mID2Clone.get(CID);
			c.mUC = uc;
			c.mSeq = sequence;
			c.mSeqLen = sequence.length();
			c.mBuried = buried;
			c.mStart = lpos;
			c.mEnd = lpos + c.mSeqLen + ngaps - 1;
			c.mGaps = gaps.trim();
			c.mNGaps = ngaps;
			c.mUC = uc;
			
			// EST are always assumed to start from their first base, so some are recorded as
			// having negative start points on the consensus. The alignment functions can't
			// handle negative start points, so we will change these to instead
			// have start points > 1 in their own sequence (and we'll have to change it back later).
			c.mQStart = (c.mStart >= 1 ? 1 : -(c.mStart-2));
		}
		rs.close();
	}
	// locate the mated clones and fill out the pairs array 
	// with pairs, at least one clone of which is in the contig
	public void setPairs()  throws Exception
	{
		mPairsIdx.clear();
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			if (c.mPair != null)
			{
				mPairsIdx.add(c.mPair.mIdx);
				if (mAllClonesID.contains(c.mMate.mID))
				{
					mContainedPairsIdx.add(c.mPair.mIdx);
				}
			}
			else
			{
				mLonersID.add(cid);
			}
		}
	}
	// For new subcontigs, merged from earlier ones.
	// Collect all the clones from the bury table, whose top clone is now in this
	// contig, and make a contclone entry for them.
	// TOPPARENTS MUST BE UPDATED FOR THIS TO WORK (call AssemMain.setTopParents)
	public void collectBuries(DBConn db) throws Exception
	{
		ResultSet rs = null;
		PreparedStatement ps = db.prepareStatement("insert into contclone (CTGID,contigid,buried,orient,gaps,ngaps," + 
		"CID,cloneid,leftpos) values(" + mID + ",'" + mIDStr + "',1,?,'',0,?,?,?) on duplicate key update cid=cid");

		rs = db.executeQuery("select CID_child,CID_parent,contclone.leftpos,clone.cloneid,clone.sequence,contclone.orient,contclone.cloneid as topname from buryclone " + 
					" join contclone on contclone.CID=buryclone.CID_topparent " +		
					" join clone on clone.CID=CID_child where contclone.CTGID=" + mID + 
					" and buryclone.AID=" + mAID );
		while (rs.next())
		{
			int childID = rs.getInt("CID_child");
			int parentID = rs.getInt("CID_parent");
			String childname = rs.getString("clone.cloneid");
			int leftpos = rs.getInt("contclone.leftpos");
			ps.setInt(2,childID);
			ps.setString(3,childname);
			ps.setInt(4, leftpos);
			mAllClonesID.add(childID);
			mBuriedClonesID.add(childID);
			Clone c = mID2Clone.get(childID);
			c.mParent = mID2Clone.get(parentID);
			c.mSeq = rs.getString("sequence");
			c.mSeqLen = rs.getString("sequence").length();
			UC uc = UC.valueOf(rs.getString("orient"));
			ps.setString(1, uc.toString());
			c.mUC = uc;
			ps.addBatch();
		}
		rs.close();
		ps.executeBatch();
		ps.close();
	}
	
	// We're looking for suspiciously low-coverage sections in the middle of a contig
	// We will build the depth map over both bases and the spaces between bases.
	// If we find one, we'll update the notes section

	public boolean chimeric(DBConn db) throws Exception
	{		
		boolean ret = false;
		int nStack = 5;
		int nGap = 1;
		int iMax = 0;

		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			iMax = Math.max(iMax,c.mEnd);
		}
		
		int[] depth = new int[2*iMax + 1];
		for (int i = 0; i < depth.length; i++) depth[i] = 0;
		
		SparseMatrix bp2clone = new SparseMatrix(2*iMax+1,20);
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
						
			int iStart = Math.max(2, 2*c.mStart); // the starts are 1-indexed but can be negative due to clipping			
			int iEnd = 2*c.mEnd; 
			
			for (int i = iStart; i <= iEnd; i++ )
			{
				depth[i]++;
				bp2clone.set(i, c.mID);
			}
		}
		int state = 0; 
		mMinGapDepth = nStack;
		int iChimLoc = 0;
	
		setRF();
		for (int i = 0; i < depth.length; i++ )
		{
			int d = depth[i];
			if (state == 0)
			{
				// we haven't found a stack yet
				if (d >= nStack)
				{
					state = 1;
				}
			}
			else if (state == 1)
			{
				// we found the first stack and we're looking for the gap
				mMinGapDepth = Math.min(d,mMinGapDepth);
				if (d <= nGap)
				{
					state = 2;
					iChimLoc = i;
					
					if (d == 0)
					{
						if (!mixedRF())
						{
							// this shouldn't be able to happen
							Log.msg(idStr() + " empty gap at " + iChimLoc + "!!", LogLevel.Detail);
						}
						else
						{
							//we're probably just between the matepair sides so reset the search
							state = 0;
						}
					}
					
				}
			}
			else if (state == 2)
			{
				// we found a stack, then a gap
				mMinGapDepth = Math.min(d,mMinGapDepth);
				if (d >= nStack)
				{
					// now we found the second stack --> suspicious!!
					ret = true;
					
					break;
				}
				else if (d == 0)
				{
					// we hit a break in coverage instead - it must be the middle of a joined mate pair contig
					// start over
					state = 0;
				}
			}
		}
		if (ret) 
		{
			Integer[] chimClones = bp2clone.getRowAsIntegers(iChimLoc);
			if (ret)
			{
				String note = "";
				if (chimClones.length > 0) 
				{
					note = "suspect @";
					for (int cid : bp2clone.getRowAsIntegers(iChimLoc))
					{
						Clone c = mID2Clone.get(cid);
						Log.msg("Chimeric:" + c.mFullName + " at " + iChimLoc,LogLevel.Detail);
						note += c.mFullName + " ";
					}
				}
				else
				{
					note = "suspect @" + iChimLoc;
				}
				db.executeUpdate("update contig set notes = concat(notes,'" + note + "') where ctgid=" + mID);
				chimClones = null;
			}
		}
		bp2clone.clear(); bp2clone = null;
		return ret;
	}
	public void doRstat(RStat rstatObj, DBConn db) throws Exception
	{
		TreeMap<Integer,Integer> libCounts = new TreeMap<Integer,Integer>();
		ResultSet rs = db.executeQuery("select library.lid,contig_counts.count from library join contig_counts " +
				" on contig_counts.libid=library.libid where contig_counts.contigid='" + mIDStr + "' and library.ctglib=0");
		while (rs.next())
		{
			int lid = rs.getInt("library.lid");
			int count = rs.getInt("contig_counts.count");
			libCounts.put(lid,count);
		}
		double rstat = rstatObj.calcRstat(libCounts);
		Log.msg("Ctg" + mIDStr + " rstat:" + rstat,LogLevel.Detail);
		db.executeUpdate("update contig set rstat='" + rstat + "' where ctgid=" + mID);
		
	}

	public boolean finalized(DBConn db) throws Exception
	{
		ResultSet rs = db.executeQuery("select finalized from contig where ctgid=" + mID);
		rs.next();
		boolean finalized = rs.getBoolean("finalized");
		return finalized;
	}
	public void setFinalized(DBConn db, boolean finalized) throws Exception
	{
		int val = (finalized ? 1 : 0);
		db.executeUpdate("update contig set finalized=" + val + " where ctgid=" + mID);
	}
	/***********************************************************************
	 * RPKM, Rstat, totalExp, totalExpN for current contig.
	 * These are computed in AssemSkip for no assembly
	 * For assembled contigs, only RPKM is currently computed
	 */
	public void libCounts(DBConn db) throws Exception
	{
		// First initialized to zero for all libs, including the ones from expression files
		db.executeUpdate("delete from contig_counts where contigid='" + mIDStr + "'");
		db.executeUpdate("insert into contig_counts (select '" + mIDStr + "', libid, 0 from assemlib where aid=" + mAID + ")");
		// Now add the counts due to membership in the contig 
		db.executeUpdate("update contig_counts set count=count+(select count(*) from contclone  " + 
				" join clone on clone.cid=contclone.cid where contclone.contigid='" + mIDStr + "' " +
				" and clone.libid=contig_counts.libid) where contig_counts.contigid='" + mIDStr + "'");
		
		// Now add the counts that came from loading the expression files *clone_exp table)
		db.executeUpdate("update contig_counts set count=count+"
				+ "(select IFNULL(sum(count),0) from clone_exp  " + 
				" join clone on clone.cid=clone_exp.cid " + 
				" join contclone on contclone.cid=clone.cid " + 
				" join library on library.lid=clone_exp.lid " +
				" where contclone.contigid='" + mIDStr + "' " +
				" and library.libid=contig_counts.libid and clone_exp.rep=0) "
				+ " where contig_counts.contigid='" + mIDStr + "' ");
		
		// transfer the counts to the contig table
		ResultSet rs = db.executeQuery(
				"select library.libid, count, libsize, library.ctglib from contig_counts "
				+ " join library on library.libid=contig_counts.libid " +
				" where contigid='" + mIDStr + "' ");
		TreeMap<String,Integer> counts = new TreeMap<String,Integer>();
		TreeMap<String,Integer> libsizes = new TreeMap<String,Integer>();
		TreeSet<String> ctgLibs = new TreeSet<String>();
		while (rs.next()) // CAS304 change to numeral gets
		{
			String libid = rs.getString(1);
			int count = rs.getInt(2);
			int size = rs.getInt(3);
			counts.put(libid,count);
			libsizes.put(libid,size);
			
			boolean ctglib = rs.getBoolean(4);
			if (ctglib) ctgLibs.add(libid);
		}
		rs.close();

		Vector<String> Ls = new Vector<String>();
		Vector<String> LNs = new Vector<String>();
		
		for (String libid : counts.keySet())
		{
			double count = (double)counts.get(libid);
			//double libsize = (double)libsizes.get(libid);
			//int normalized = (int)Math.floor(denom*(count/libsize));
			//int rpkm = (int)Math.floor((1000000.0D/libsize)*(count/lenkb));
			String col = Globalx.LIBCNT + libid;
			String colN = Globalx.LIBRPKM + libid;
			db.executeUpdate("update contig set " + col  + "=" + count + " where contigid='" + mIDStr + "'");
			db.executeUpdate("update contig,library set " + colN + 
					" =  (1000000/library.libsize)*(" + col + "*1000/consensus_bases) " +
					" where library.libid='" + libid + "' and contigid='" + mIDStr + "'");
			if (ctgLibs.contains(libid)) continue;
			Ls.add(col);
			LNs.add(colN);
		}
		if (Ls.size() > 0)
		{
			String LSum = Utils.join(Ls,"+");
			String LNSum = Utils.join(LNs,"+");
			db.executeUpdate("update contig set totalexp=(" +LSum + "), totalexpN=(" + LNSum + ") "
					+ " where contigid='" + mIDStr + "'");
		}
		db.executeUpdate("update assem_msg set norm='RPKM' where aid=" + mAID);
	}
	public void snpCounts(DBConn db) throws Exception
	{
		db.executeUpdate("update contig set snp_count=(select count(*) from snp  where snp.ctgid=" + mID + 
				" and snp.snptype !='" + SNPType.Indel + "') where ctgid=" + mID);	
	}
	// Align the clones to the consensus
	public void placeClones(DBConn db, int minIndelConfirm, int minSNPConfirm, int poorAlignPct, int minExtraConfirm, int ignoreHpoly, 
					boolean unburySNP, double eRate, double exRate, double minSNPScore,double minExScore,int nThread) throws Exception
	{
		loadClones(db);
		loadCCS_orig(db); // note, get the original, cap-created consensus, which was saved
		
		boolean useOpt = false; // this optimization didn't turn out to be a big gain
		
		Utils.intTimerStart(-nThread-1);

		int nClones = mAllClonesID.size();
		
		if (nClones == 1)
		{
			db.executeUpdate("update contig set buried_placed = 1,avg_pct_aligned=100 where ctgid=" + mID);
			db.executeUpdate("update contclone set pct_aligned=100 where ctgid=" +  mID);
			return;
		}
		
		db.executeUpdate("delete snp_clone.* from snp,snp_clone where snp.ctgid=" + mID + " and snp_clone.snpid=snp.snpid");
		db.executeUpdate("delete from snp where snp.ctgid=" + mID);
		
		PreparedStatement ps = db.prepareStatement("update contclone set leftpos=?,gaps=?,ngaps=?,extras=?,orient=?,pct_aligned=? where cid=? and ctgid=" + mID);
		
		String ccs = mCCS.replace(Globalx.assmGap, "").toUpperCase(); // get rid of the pads added by cap3, but keep the n's, which can be from joining l/r contigs!!
		
		if (mCCS.length() > 10000)
		{
			//Log.msg("Attempting to align long consensus (" + mCCS.length() + "," + mIDStr + ")",LogLevel.Basic);
		}
		Aligner AlignForwards = new Aligner(ccs);
		Aligner AlignReverse = new Aligner(Utils.reverseComplement(ccs).toUpperCase());
		
		int avgPctAlign = 0;
		int nAlign = 0;
		
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			AlignForwards.clearQuery();
			AlignReverse.clearQuery();
			
			String queryseq =  c.mSeq.replaceAll("[^agctAGCTnN]","n").toUpperCase();
			
			// Do the indicated alignment first; if it's bad, then try the reverse
			Aligner fwAlign = (c.mUC == UC.U ? AlignForwards : AlignReverse);
			Aligner revAlign = (c.mUC == UC.U ? AlignReverse : AlignForwards);	
			Utils.intTimerStart(30*nThread);
		
			boolean memError = false;
			try
			{
			
				// If we still have a crummy alignment (or optimization is turned off), do it without optimization			
				if (!useOpt || (fwAlign.pctQueryAligned() < poorAlignPct && revAlign.pctQueryAligned() < poorAlignPct))
				{
					fwAlign.clearQuery();
					revAlign.clearQuery();
	
					fwAlign.SWAlign(queryseq,c.mFullName, false, nThread);
	
					if (fwAlign.pctQueryAligned() < poorAlignPct)
					{
						revAlign.SWAlign(queryseq,c.mFullName, false, nThread);
						if (revAlign.pctQueryAligned() > fwAlign.pctQueryAligned() )//&& revAlign.mNGaps < 2*fwAlign.mNGaps)
						{
							c.mUC = c.mUC.invert();
						}	
					}
				}	
			}
			catch(OutOfMemoryError e)
			{
				memError = true;
				int memNeededGB = (int)(10.0*((float)c.mSeqLen)*((float)mCCS.length())/1000000000);
				System.err.println("Out of memory while aligning read " + c.mFullName + " to contig " + mIDStr + ".\n" + 
						"Lengths: " + c.mFullName + "=" + c.mSeqLen + "," + mIDStr + "=" + mCCS.length() + "\n" +
						"The read will not be aligned.\n " +
						"Estimated memory required:" + memNeededGB + "GB");
			}

			int pctAligned = 0;
			if (!memError)
			{
				if (c.mUC == UC.U)
				{
				    c.mGaps = AlignForwards.mGaps.trim();
				    c.mTGaps = AlignForwards.mTGaps.trim();
				    c.mStart = AlignForwards.mTStart;
				    c.mNTgaps = AlignForwards.mNTGaps;
				    c.mQStart = AlignForwards.mQStart;
				    c.mQEnd = AlignForwards.mQEnd;
				    c.mEnd = AlignForwards.mTEnd;
				    c.mQueryLine = AlignForwards.mQueryOut.toUpperCase();
				    c.mTargetLine = AlignForwards.mTargetOut.toUpperCase();
					pctAligned = AlignForwards.pctQueryAligned();
					if (c.mStart <= 0 || c.mQStart <= 0 || c.mQueryLine.length() != c.mTargetLine.length())
					{			    
						//Log.msg(c.mFullName + " alignment, QStart:" + c.mQStart + ", TStart:" + c.mStart + ", QLen:" + c.mQueryLine.length() + ", TLen:" + c.mTargetLine.length());
						//AlignForwards.printAlignment();
					}
				}
				else
				{
					// The consensus was reverse-comp'd in the alignment, 
					// We need to reverse-comp each of the numbers
					c.mStart = ccs.length() - AlignReverse.mTEnd + 1;
					c.mQStart = c.mSeqLen - AlignReverse.mQEnd + 1; 
					c.mEnd = ccs.length() - AlignReverse.mTStart + 1;
					c.mQEnd = c.mSeqLen - AlignReverse.mQStart + 1; 
				    c.mNTgaps = AlignReverse.mNTGaps;
				    c.mGaps = AlignReverse.mGaps.trim();
				    c.mTGaps = AlignReverse.mTGaps.trim();
	
					c.mQueryLine = Utils.reverseComplement(AlignReverse.mQueryOut).toUpperCase();
				    c.mTargetLine = Utils.reverseComplement(AlignReverse.mTargetOut).toUpperCase();
				    
					pctAligned = AlignReverse.pctQueryAligned();
					if (c.mStart <= 0 || c.mQStart <= 0 || c.mQueryLine.length() != c.mTargetLine.length())
					{		
						//Log.msg(c.mFullName + " alignment, QStart:" + c.mQStart + ", TStart:" + c.mStart + ", QLen:" + c.mQueryLine.length() + ", TLen:" + c.mTargetLine.length());
						//AlignReverse.printAlignment();
					}
					
				}
				
				if (c.mQueryLine.length() == 0)
				{
					throw(new Exception(c.mFullName + " no query line"));	
				}
				
				avgPctAlign += pctAligned;
				nAlign++;
				
				Utils.intTimerEnd(30*nThread,"DoAlign");
			}
			else
			{
				c.mStart = 0;
				c.mQStart = 0;
				c.mUC = UC.U;	
				c.mNoAlign = true;
			}
			
		    ps.setInt(1, c.mStart - (c.mQStart-1));
		    ps.setString(2,"");			//c.mGaps);
		    ps.setInt(3,0);				//c.mNGaps);
		    ps.setString(4,"");			//c.mTGaps);
		    ps.setString(5,c.mUC.toString());
		    ps.setInt(6, pctAligned);
		    ps.setInt(7, cid);
		    ps.addBatch();
		    
			fwAlign.clearQuery();
			revAlign.clearQuery();
		}
		ps.executeBatch();		
		ps.close();

		Utils.intTimerEnd(-nThread-1,"AlignClones");

		addPads(db, minIndelConfirm, minSNPConfirm,poorAlignPct,ignoreHpoly, minExtraConfirm, unburySNP, eRate,exRate, minSNPScore,minExScore,nThread);

		AlignForwards.clear();
		AlignReverse.clear();
		
		for (int cid : mAllClonesID)
		{
			// this is to free memory
			Clone c = mID2Clone.get(cid);
			c.mGaps = ""; c.mGaps = null;
			c.mTGaps = ""; c.mTGaps = null;
			c.mQueryLine = ""; c.mQueryLine = null;
			c.mTargetLine = ""; c.mTargetLine = null;
			c.mSeq = ""; c.mSeq = null;
			c.mQual = ""; c.mQual = null;
		}
		if (nAlign > 0)
		{
			avgPctAlign /= nAlign;
			db.executeUpdate("update contig set avg_pct_aligned=" + avgPctAlign + " where ctgid=" +mID);	
		}
	}
	// The most complicated routine in the assembler...
	// 
	// Take the alignments generated for each EST, and where there is a gap on the consensus
	// side (i.e., an apparent "extra base" in the EST), add a pad ('*') IF the extra is confirmed 
	// by minExtraConfirm identical bases in other ESTs. 
	// If ignoreHpoly is set, then ignore any putative extras that are part of a homopolymer stretch of this length. 
	// 
	// For each pad that is added, a corresponding gap has to be added at each aligning EST which has no extra.
	//
	// In the case of multiple extras at one location, they are ordered, and the confirmation must be with
	// an identically placed extra in another EST. In other words, the 'a' in extras 'tag' 
	// can not be confirmed by a single extra 'a' in another EST, but would be by 'ta'
	//
	// Lastly, SNPs are located and saved to the DB, based on the minSNPConfirm and ignoreHpoly parameters.
	// 
	// The idea is to build a multiple alignment matrix with the consensus at the top, and
	// then go column by column adding pads and inserting new columns. Then, the final
	// alignments can be read off row by row. 
	// The actual data structures are a bit more involved in order to use sparseness and cut down on memory. 
	// 
	public int addPads(DBConn db, int minIndelConfirm, int minSNPConfirm, int poorAlignPct, int ignoreHpoly, int minExtraConfirm, boolean unburySNP, 
			double eRate, double exRate, double minSNPScore, double minExScore, int nThread) throws Exception
	{
		int padsAdded = 0;
		int gapsAdded = 0;
		int nSNP = 0;

		Utils.intTimerStart(100*nThread);
		
		// How many pads will we be adding at maximum?
		int maxPads = 0;

		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			maxPads += c.mNTgaps;
		}		
		
		// We are going to start the MA matrix at base 1 of the consensus, although some clone 
		// alignments start before the beginning.
		// For convenience, column 0 stores the clone ID (offset by the min ID).
		
		int nClones = mAllClonesID.size();
		Astats.inc("ClonesAnalyzed",nClones);
		int maxCols = mCCS.length() + maxPads;
		Astats.inc("CCSBases",mCCS.length());
		
		char[] ccsArray = new char[maxCols];
		
		// At each position of a clone, there is either a match, a gap, or an extra base.
		// We can save memory by only tracking the latter two, since the first is redundant with the consensus. 
		// We also need to track the start points of the clones.
		// For SNP finding, we will also track mismatches
		
		SparseMatrix cloneGaps = new SparseMatrix(maxCols, 20);				// (col, cid) <--> clone cid has gap against col
		SparseMatrix cloneStarts = new SparseMatrix(maxCols,20);		// (col, cid) <--> clone cid starts here
		SparseMatrix cloneEnds = new SparseMatrix(maxCols,20);		// (col, cid) <--> clone cid ends here
		
		int exIdxInc = 20;
		int[] extraCount = new int[maxCols];
		int[][] extraIdx = new int[maxCols][exIdxInc];					// stores auxiliary clone index used to access the extraBases array
																		// which is needed since the database indexes cid are A)sparse and B)don't start from 0
		char[][][] extraBases = new char[maxCols][exIdxInc][];			// the actual extra bases

		int misIdxInc = 20;
		int[] misCount = new int[maxCols];  			// how many mismatches in the column
		int[][] misIdx = new int[maxCols][misIdxInc];   // clone id's of the mismatches
		char[][] misBases = new char[maxCols][misIdxInc];
				
		for (int i = 0; i < maxCols; i++)
		{
			ccsArray[i] = 0;
			extraCount[i] = 0;
			misCount[i] = 0;
			
			for (int j = 0; j < exIdxInc; j++)
			{
				extraIdx[i][j] = 0;	
				extraBases[i][j] = null;
				misIdx[i][j] = 0;
				misBases[i][j] = 0;
			}
		}
	
		String ccsUnStarred = mCCS.replace(Globalx.assmGap,"").toUpperCase(); // keep the n's though
		for (int i = 0; i < ccsUnStarred.length(); i++)
		{
			ccsArray[i] = ccsUnStarred.charAt(i);
		}
				
		// Fill out the initial alignment matrix and extras matrix, and the mismatches
		int maxExtras = 0;

		int cid_prev = 0;
		for (int cid : mAllClonesID)
		{
			if (cid <= cid_prev)
			{
				throw(new Exception("clones not enumerated in ascending order!!"));	
			}
			Clone c = mID2Clone.get(cid);

			if (c.mNoAlign) continue;
			
			if (c.mQStart <= 0)
			{
				throw(new Exception(c.mFullName + " starts at " + c.mQStart));	
			}
			if (c.mStart <= 0)
			{
				throw(new Exception(c.mFullName + " starts at " + c.mStart));	
			}
			
			cloneStarts.set(c.mStart - 1,cid);			
			cloneEnds.set(c.mEnd - 1,cid);
						
			int col = c.mStart - 1; // cannot be < 0 because it came from our own DP alignment rather than cap3
			
			for (int i = 0; i < c.mQueryLine.length() && col < maxCols; i++)
		    {
		    	if (c.mTargetLine.charAt(i) == '-')
		    	{
					// Extra base.
		    		if (c.mQueryLine.charAt(i) == '-')
		    		{
		    			throw(new Exception(c.mFullName + " extra and gap both at " + i));	
		    		}
		    		// Look ahead - how many will there be? 
		    		int nExtras = 0;
		    		for (int j = i; j < c.mTargetLine.length() && c.mTargetLine.charAt(j) == '-'; j++)
		    		{
						nExtras++;	
		    		}

		    		// the current count of clones with extra bases at this column
		    		// is also the next array position at which to put them
		    		int curCount = extraCount[col];
		    		
		    		if (curCount == extraIdx[col].length)
		    		{
		    			// have to grow this row
		    			int newRowSize = extraIdx[col].length + exIdxInc;
		    			int[] newIdxRow = new int[newRowSize];
		    			char[][] newExtrasRow = new char[newRowSize][];
		    			for (int j = 0; j < extraIdx[col].length; j++)
		    			{
		    				newIdxRow[j] = extraIdx[col][j];
		    				newExtrasRow[j] = extraBases[col][j];
		    			}
		    			extraBases[col] = null;
		    			extraBases[col] = newExtrasRow;
		    			extraIdx[col] = newIdxRow;
		    		}

		    		extraIdx[col][curCount] = cid;		    				    	
					extraBases[col][curCount] = new char[nExtras];
		    		extraCount[col]++;
					
				// Fill them all in and advance the alignment pointer
				for (int j = 0; j < nExtras; j++)
				{
					char base = c.mQueryLine.charAt(i + j);
					extraBases[col][curCount][j] = base; // note that n's have to be added too
				}
				
				// Now col doesn't advance at all, because we're still on the same consensus base location
				// But the query line position advances to the last extra base, and then the loop will advance it one more
				i += (nExtras - 1);
				
				maxExtras = Math.max(maxExtras,nExtras);
		    	}		    	
		    	else 
		    	{
		    		if (c.mQueryLine.charAt(i) == '-')
		    		{
		    			// record location of this gap so we can rebuild the gap list later
		    			cloneGaps.set(col,cid);	
		    		}
		    		else
		    		{
		    			char qbase = c.mQueryLine.charAt(i);
		    			char tbase = c.mTargetLine.charAt(i);
		    			if (qbase != tbase)
		    			{
		    				// mismatch! Store it for the snp computation
				    		int curCount = misCount[col];
				    		
				    		if (curCount == misIdx[col].length)
				    		{
				    			// have to grow this row
				    			int newRowSize = misIdx[col].length + misIdxInc;
				    			int[] newIdxRow = new int[newRowSize];
				    			char[] newRow = new char[newRowSize];
				    			for (int j = 0; j < misIdx[col].length; j++)
				    			{
				    				newIdxRow[j] = misIdx[col][j];
				    				newRow[j] = misBases[col][j];
				    			}
				    			misBases[col] = null;
				    			misBases[col] = newRow;
				    			misIdx[col] = newIdxRow;
				    		}	
				    		
				    		misIdx[col][curCount] = cid;
				    		misBases[col][curCount] = qbase;
				    		misCount[col]++;
		    			}
		    		}
					col++;
		    	  }
		    }
		}
		
		cloneStarts.sortRows();
		cloneEnds.sortRows();
		cloneGaps.sortRows();
		
		// blank out the clone starts/ends, which we will reset
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			
			if (c.mNoAlign) continue;
			
			c.mStart = -1;
			c.mEnd = -1;
			
			c.mGapArray = new Vector<Integer>();
			c.mTGapArray = new Vector<Integer>();	
		}
		
		// Now, go through and update the matrices column by column adding pads
		// For each pad we add a snp (indel) as well.
		// Each confirmed base variation at that position is tagged as a snp variant. 
		
		// This array stores the counts of each extra base at each position. 
		// Needed so we can know which extras are confirmed.
		int[][] exBaseCounts = new int[maxExtras][5];
		for (int i = 0; i < maxExtras; i++)
		{
			for (int j = 0; j < 5; j++)
			{
				exBaseCounts[i][j] = 0;	
			}
		}
		TreeMap<Character,Integer> base2int = new TreeMap<Character,Integer>();
		char[] int2base = new char[6];
		
		base2int.put('A',0); int2base[0] = 'A';
		base2int.put('G',1); int2base[1] = 'G';
		base2int.put('C',2); int2base[2] = 'C';
		base2int.put('T',3); int2base[3] = 'T';
		base2int.put('N',4); int2base[4] = 'N';
		base2int.put('-',5); int2base[5] = '-'; // Don't change w/o changing bGap too
		
		PreparedStatement psSNPclone = db.prepareStatement("insert into snp_clone (snpid,cid,snptype) values(?,?,?)");
		
		// This array stores the snps for each clone so we can later unbury some of the clones with snps
		TreeMap<Integer,TreeMap<Character,HashSet<Clone>>> snp2clone = new TreeMap<Integer,TreeMap<Character,HashSet<Clone>>>();
		
		//int gapWatch = 378;
		int hpolyIgnored = 0;
		int depth = 0;
		for (int col = 0; col < maxCols; col++)
		{
			// How many new pads at this location?
			int curCount = extraCount[col];
			int nPads = 0;
			for (int j = 0; j < curCount; j++)
			{
				if (extraBases[col][j] != null)
				{
					nPads = Math.max(nPads,extraBases[col][j].length);
				}				
			}
			
			// update the clone starts/ends as we go				
			for (int cid : cloneStarts.getRowAsIntegers(col))
			{
				Clone c = mID2Clone.get(cid);
				c.mStart = col + 1;	
				depth++;
			}

			if (nPads > 0)
			{
				// Uh oh - now we have to work
				// First, we need to know the exact counts of each base at each pad position
				// And we're only counting real bases, agct. N's will just become extras.  
				for (int i = 0; i < maxExtras; i++)
				{
					for (int j = 0; j < 5; j++)
					{
						exBaseCounts[i][j] = 0;	
					}
				}
				
				int maxPos = 0;
	
				for (int r = 0; r < curCount; r++)
				{
					int cid2 = extraIdx[col][r];
					Clone c2 = mID2Clone.get(cid2);
					for (int expos = 0; expos < extraBases[col][r].length; expos++)
					{
						char ibase = extraBases[col][r][expos];
						if (base2int.containsKey(ibase)) 
						{
							exBaseCounts[expos][base2int.get(ibase)]++;	
							maxPos = Math.max(maxPos,expos);
						}
						else
						{
							throw(new Exception(c2.mFullName + " Unrecognized base " + ibase));	
						}
					}
				}
				
				// Now for every position that meets the threshold, add a pad and delete the Extras entry.
				// Entries that didn't meet the threshold are retained to record to the database

				// Go through them backwards since their order will be reversed by adding pads
				for (int expos = maxPos; expos >= 0; expos--)
				{
					int maxCount = 0;
					int maxB = -1;
					for (int b = 0; b <= 4; b++)
					{
						if (exBaseCounts[expos][b] > maxCount)
						{
							maxB = b;
							maxCount = exBaseCounts[expos][b];
						}
					}
					if (maxCount < minExtraConfirm)
					{
						continue;
					}	
					double score = Utils.cumulBinom(depth, exRate, maxCount);
					if (score <= minExScore)
					{
						// We have a  winner. Add one pad and delete the Extras entries for this position.
						
						if (ignoreHpoly > 0)
						{
							// ignore this extra if its part of a homopolymer run of the given length
							// note that the extra could come out at any place in the alignment
							char base = int2base[maxB];
							int runLength = 1;
							for (int check = col - 1; check >= 0; check--)
							{
								if (ccsArray[check] == '*')
								{
									// skip pads that we may have already added
									continue;	
								}
								if (ccsArray[check] != base)
								{
									break;
								}
								runLength++;
							}	
							for (int check = col + 1; check < maxCols; check++)
							{
								if (ccsArray[check] == '*') // should not really be needed as we add pads left to right
								{
									continue;	
								}
								if (ccsArray[check] != base)
								{
									break;
								}
								
								runLength++;
							}	
							
							if (runLength >= ignoreHpoly) 
							{
								hpolyIgnored++;
								continue;
							}
						}
						
						// Shift everything to the right, starting one column over
						
						for (int col2 = maxCols-1; col2 > col + 1; col2--)
						{
							extraBases[col2] = extraBases[col2 - 1];
							misBases[col2] = misBases[col2 - 1];
							extraIdx[col2] = extraIdx[col2 - 1];
							misIdx[col2] = misIdx[col2 - 1];
							extraCount[col2] = extraCount[col2 - 1];
							misCount[col2] = misCount[col2 - 1];
							ccsArray[col2] = ccsArray[col2 - 1];
							//if (col2 - 1 == gapWatch) gapWatch++;
						}
												
						// Initialize the immediate next column. Note that it doesn't have any extras,
						// since they stay with the new pad column.
						// The mismatches do move over, though.
						
						ccsArray[col + 1] = ccsArray[col];
						extraBases[col + 1] 	= null;
						extraIdx[col + 1]		= null;
						extraCount[col + 1] 	= 0;
						misBases[col + 1] 	= misBases[col];
						misIdx[col + 1] 	= misIdx[col];
						misCount[col + 1] 	= misCount[col];
						misBases[col] 	= null;
						misIdx[col] 	= null;
						misCount[col] 	= 0;

						// add the pad
						ccsArray[col] = '*';
						
						// shift over the quantities in the sparse matrices also
						cloneGaps.insertRow(col);
						cloneStarts.insertRow(col);
						cloneEnds.insertRow(col);

						// add a gap to each clone covering this position (we'll then delete them from the clones with the extras)
						// this add-then-delete procedure could be avoided by tracking every column covered by a clone 
						// with an array like coneGaps, instead of just tracking the start/end columns for each clone. 
						
						for (int cid : mAllClonesID)
						{
							Clone c = mID2Clone.get(cid);
	
							if (c.mStart > 0 && c.mStart < col + 1 && c.mEnd == -1)
							{
								cloneGaps.set(col,cid);
								gapsAdded++;
							}
						
						}
	
						padsAdded++;
						if (padsAdded > maxPads)
						{
							throw(new Exception("Added " + padsAdded + " but max pads only " + maxPads));	
						}
						
						// Delete the extras entry for this position, since it now "aligns" to the pad
						// Delete the gap we just added as well
						// And, add the SNP to these clones if their extra base was confirmed
						for (int r = 0; r < curCount; r++)
						{
							if (extraBases[col][r] != null)
							{
								if (extraBases[col][r].length > expos)
								{
									extraBases[col][r][expos] = '+';	
									int cid = extraIdx[col][r];
									if (cloneGaps.isSet(col,cid))
									{
										cloneGaps.delete(col,cid);
										gapsAdded--;
									}
								}
							}
						}
						if (maxCount >= minSNPConfirm)
						{
							psSNPclone.executeBatch();
						}

					}
					else
					{
						// Presumed spurious.
						// Do nothing - just go on to the next column. 
						// The Extras entries will be saved to the database later. 
					}
				}
			}
			for (int cid : cloneEnds.getRowAsIntegers(col))
			{
				Clone c = mID2Clone.get(cid);			
				c.mEnd = col + 1;	
				depth--;
				if (depth < 0)
				{
					throw(new Exception("depth below zero!!"));	
				}
			}			
		}
		
		// Now the fun part: read off the new alignments from the various arrays
		// And the snps
		// Note that we will count a confirmed gap as a snp, unless it is aligning to a pad
		
		StringBuilder newCCSBuild = new StringBuilder("");
		for (int i = 0; i < maxCols; i++)
		{
			if (ccsArray[i] == 0) break;
			newCCSBuild.append(ccsArray[i]);		// CAS304 removed (char) before ccs...
		}
		String newCCS = newCCSBuild.toString();
		if (newCCS.length() < ccsUnStarred.length())
		{
			throw(new Exception("Padded ccs is shorter!! " + newCCS + ":" + ccsUnStarred));	
		}

		mCCS = newCCS;
		
		// compile list of gaps/extra bases, indexed by consensus location
				
		depth = 0;
		int nIndels = 0;
		for (int col = 0; col < maxCols; col++)
		{
			if (ccsArray[col] == 0) break; // end of consensus
							
			Integer[] cgaps = cloneGaps.getRowAsIntegers(col);
			if (cgaps.length >= minIndelConfirm)
			{
				nIndels++;	
				Log.msg("Indel col " + col, LogLevel.Dbg);
			}
			for (int cid : cgaps)
			{
				Clone c = mID2Clone.get(cid);	
				
				if (c.mStart > col)
				{
					throw(new Exception(c.mFullName + " gap at " + col + " before align start at " + c.mStart + "!!"));	
				}
				if (c.mEnd < col)
				{
					throw(new Exception(c.mFullName + " gap after align end!!"));	
				}	

				c.mGapArray.add(col);						
			}		
			for (int k = 0; k < extraCount[col]; k++)
			{				
				int cid = extraIdx[col][k];				
				Clone c = mID2Clone.get(cid);
				if (c.mStart > col)
				{
					throw(new Exception(c.mFullName + " extra base before align start!!"));	
				}
				if (c.mEnd < col)
				{
					throw(new Exception(c.mFullName + " extra base at " + col + " after align end at " + c.mEnd + "!!"));	
				}	
				int nPads = 0;
				for (int j = 0; j < extraBases[col][k].length; j++)
				{
					if (extraBases[col][k][j] != '+') // else this one was added as a pad
					{
						c.mTGapArray.add(col);
						c.mTGapArray.add(nPads);  // Very confusing. The translation back to clone coords below needs this
													// in order to properly offset the consensus coordinate for multiple extras, some of which became pads.
					} 
					else
					{
						nPads++;	
					}
				}
			}	
			char tbase = mCCS.charAt(col);
			depth += cloneStarts.rowCount(col);
			int[] baseCounts = new int[6];
			for (int j = 0; j < baseCounts.length; j++) baseCounts[j] = 0;
			
			int maxCount = 0;
			int maxBaseIdx = -1;
			// Get it right for * and N as well. Don't go off consensus, just count all the bases. 
			if (tbase != '*' && tbase != 'N')
			{
				// Count the SNPs
				// Note that if the consensus has '*', then we already did its snps
				for (int j = 0; j < misCount[col]; j++)
				{
					int cid = misIdx[col][j];
					char base = misBases[col][j];
					if (base == tbase)
					{
						Clone c = mID2Clone.get(cid);
						throw(new Exception("faulty mismatch! base=" + base + ", tbase=" + tbase + " clone=" + c.mFullName));	
					}
					baseCounts[base2int.get(base)]++;
					if (base != 'N') 
					{
						if (baseCounts[base2int.get(base)] > maxCount)
						{
							maxCount = baseCounts[base2int.get(base)];
							maxBaseIdx = base2int.get(base);
						}
					}
				}

				if (maxCount >= minSNPConfirm && maxBaseIdx < 5) //|| (maxCount >= minIndelConfirm && maxBaseIdx == 5) ) // WMN use score now
				{
					int nMatch = depth - misCount[col] - cgaps.length;
					StringBuilder baseVars = new StringBuilder();
					baseVars.append(Character.toUpperCase(tbase) + ":" + nMatch);
					
					int numVariants = 0;
					for (int b = 0; b < 6; b++)
					{
						int count = baseCounts[b];
						if (count < minSNPConfirm) continue;
						numVariants++;
													
						baseVars.append(", ");
						baseVars.append(int2base[b]);
						baseVars.append(":");
						baseVars.append(count);
					}
					SNPType stype = SNPType.Mis;
					boolean keep = false;
					double score = 0;
					if (maxBaseIdx == 5)
					{
					}
					else
					{
						score = Utils.cumulBinom(depth, eRate/3, maxCount);
						if (score <= minSNPScore)
						{
							keep = true;	
						}
						else
						{
							Log.msg("rejected snp depth:" + depth + " count:" + maxCount + " score:" + score + " col:" + col,LogLevel.Dbg);	
						}
					}
		
					if (keep)
					{
						db.executeUpdate("insert into snp (ctgid,pos,basevars,numvars,snptype,score) values(" + mID + "," + (col+1) + ",'" + 
									baseVars.toString() + "'," + numVariants + ",'" + stype.toString() + "','" + score + "')");
						int snpid = db.lastID();
						psSNPclone.setInt(1,snpid);
						nSNP++;
						snp2clone.put(snpid,new TreeMap<Character, HashSet<Clone>>());
						for (int b = 0; b < int2base.length; b++)
						{
							snp2clone.get(snpid).put(int2base[b],new HashSet<Clone>());
						}
						
						for (int j = 0; j < misCount[col]; j++)
						{
							Character base = misBases[col][j];
							int count = baseCounts[base2int.get(base)];
							if (count < minSNPConfirm) continue;						
							int cid = misIdx[col][j];
							psSNPclone.setInt(2,cid);
							psSNPclone.setString(3,SNPType.Mis.toString());
							psSNPclone.addBatch();	
							snp2clone.get(snpid).get(base).add(mID2Clone.get(cid));
						}	
						psSNPclone.executeBatch();
					}
				}	
			}

			depth -= cloneEnds.rowCount(col); // do this last as clones ending here still cover this position
		}
		
		psSNPclone.close();

		// Now, for each clone go through and convert the coordinates of gaps/extras into 
		// clone sequence coordinates (from consensus coords)
		
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			
			if (c.mNoAlign) continue;
			
			c.mNTgaps = c.mTGapArray.size()/2;
			c.mNGaps = c.mGapArray.size();
			
			// First we have to extrapolate the start position to the FIRST base of the clone sequence
			// E.g., if the alignment starts at consensus base 1 and clone base 5, then it
			// will be stored as starting at consensus base 1-5+1 = -3
			
			c.mStart -= (c.mQStart - 1); 
			
			int ig = 0; int it = 0;
			int gap_count = 0;
			int tgap_count = 0;
			int skip_gaps = 0;
			StringBuilder gapBuild = new StringBuilder();
			StringBuilder tgapBuild = new StringBuilder();
			
			while (ig < c.mNGaps || it < c.mNTgaps)
			{
				boolean validGap = (c.mNGaps > 0 && ig < c.mNGaps);
				boolean validTGap = (c.mNTgaps > 0 && it < c.mNTgaps);
				
				int gap = (validGap ? c.mGapArray.get(ig) : -1);
				int tgapCol = (validTGap ? c.mTGapArray.get(2*it) : -1);
				int tgapPos = (validTGap ? c.mTGapArray.get(2*it + 1) : -1);
															
				// The current offset between the consensus pos and the clone pos depends on prior gaps as follows
				int cur_offset = -(c.mStart - 1) - gap_count + tgap_count;
				
				if (validGap && validTGap)
				{
					if (gap == tgapCol)
					{
						// This can happen if, e.g., this clone had 1 extra base here, but
						// other clones had 2 extras, and only the 2nd position was confirmed. 
						// The code adds a pad for the 2nd extra, and a gap to all clones,
						// and then removes that gap from those which had the 2nd extra. 
						// 
						// All we have to do is ignore both the tgap and the gap. 
						//
						ig++;
						it++;
						skip_gaps++;
					}
					else if (gap < tgapCol)
					{
						// Note that consecutive gaps will come out having the same value in clone coords
						// because "gap" grows by one, but also "gap_count" grows by one
						gap += cur_offset;
						gap_count++;
						gapBuild.append(" " + gap);
						ig++;
					}
					else // tgapCol < gap
					{
						// Conversely, here we can have multiple extras at the SAME consensus location, 
						// and this is converted into CONSECUTIVE extras in the clone coords
						int tgap = tgapCol + tgapPos + cur_offset;
						tgap_count++;
						tgapBuild.append(" " + tgap);
						it++;
					}
				}
				else if (validGap)
				{
					gap += cur_offset;
					gap_count++;
					gapBuild.append(" " + gap);
					ig++;					
				}
				else if (validTGap)
				{
					int tgap = tgapCol + tgapPos + cur_offset;
					tgap_count++;
					tgapBuild.append(" " + tgap);
					it++;					
				}
				else
				{
					break; // of course should not get here	
				}
			}
			c.mNGaps -= skip_gaps;
			c.mNTgaps -= skip_gaps;
			if (c.mNGaps != gap_count)
			{
				throw(new Exception(c.mFullName + " only used " + gap_count + " of " + c.mNGaps + " gaps"));	
			}
			if (c.mNTgaps != tgap_count)
			{
				throw(new Exception(c.mFullName + " only used " + tgap_count + " of " + c.mNTgaps + " extra bases"));	
			}			
			c.mGaps = gapBuild.toString().trim();
			c.mTGaps = tgapBuild.toString().trim();
			
			c.mTargetLine = "";
			c.mQueryLine = "";
			c.mGapArray.clear(); c.mGapArray = null; 
			c.mTGapArray.clear(); c.mTGapArray = null;
			
		}
		Astats.inc("Pads Added",padsAdded);
		Astats.inc("Gaps Added",gapsAdded);
		Astats.inc("Homopoly Ignored",hpolyIgnored);

		PreparedStatement ps = db.prepareStatement("update contclone set leftpos=?,gaps=?,ngaps=?,extras=?,orient=? where cid=? and ctgid=" + mID);
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);	
						
			ps.setInt(1, c.mStart);
			ps.setString(2,c.mGaps.trim());
			ps.setInt(3,c.mNGaps);
			ps.setString(4,c.mTGaps);
			ps.setString(5,c.mUC.toString());
			ps.setInt(6,c.mID);
			ps.addBatch();
			
			c.mEnd = c.mStart + c.mSeqLen + c.mNGaps - 1; // we need this for the chimerics computation
			
			c.mGaps = "";
			c.mTGaps = "";
			c.mSeq = "";
			c.mQual = "";
		}
		ps.executeBatch();
		ps.close();
		// the consensus_bases is different from the length sometimes
		db.executeUpdate("update contig set consensus='" + mCCS + "',consensus_bases=" + mCCS.length() + ", buried_placed=1 where ctgid=" + mID);
		
		cloneGaps.clear();
		cloneStarts.clear();
		cloneEnds.clear();
		extraCount = null;
		extraIdx = null;
		extraBases = null;

		int newUnburies = 0;

		if (unburySNP)
		{
			// Finally, unbury at least two clones per snp, per base variant
			int bury_thresh = 2;
			
			PreparedStatement ps2 = db.prepareStatement("update contclone set buried=0,prev_parent=? where cid=? and ctgid=" + mID);
			PreparedStatement ps3 = db.prepareStatement("delete from buryclone where cid_child=? and aid=" + mAID);
			for (int snpid : snp2clone.keySet())
			{
				for (Character base : snp2clone.get(snpid).keySet())
				{
					int nUnburied = 0;
					for (Clone c : snp2clone.get(snpid).get(base))
					{
						if (!c.mBuried) nUnburied++;
						if (nUnburied >= bury_thresh)
						{
							break;	
						}
					}
			
					if (nUnburied < bury_thresh)
					{
						for (Clone c : snp2clone.get(snpid).get(base))
						{
							if (c.mBuried)
							{
								nUnburied++;
								ps2.setInt(1,c.mParent.mID);
								ps2.setInt(2,c.mID);
								c.mBuried = false;
								c.mParent = null;
								ps2.addBatch();
								ps3.setInt(1,c.mID);
								ps3.addBatch();
								newUnburies++;
								
								// If it's paired, we have to unbury the other one too
								if (c.mMate != null)
								{
									c.mMate.mBuried = false;
									ps2.setInt(1,c.mMate.mParent.mID);
									ps2.setInt(2,c.mMate.mID);
									ps2.addBatch();
									ps3.setInt(1,c.mMate.mID);
									ps3.addBatch();
									newUnburies++;
									
								}
							}
							if (nUnburied >= bury_thresh)
							{
								break;	
							}
				
						}
							
					}	
				}
			}
			ps2.executeBatch();
			ps2.close();
			ps3.executeBatch();
			ps3.close();
		}
		Log.msg(mIDStr + " " + mAllClonesID.size() + " clones; " + padsAdded + " pads added; " + gapsAdded + " gaps added; " + nSNP + " snps; " + newUnburies + " snp-clones unburied; " + hpolyIgnored + " homopolymer extras ignored",LogLevel.Detail);
		db.executeUpdate("update contig set indel_count=" + nIndels + " where ctgid=" + mID);
		snp2clone.clear();
		Utils.intTimerEnd(100*nThread,"addPads");
		return padsAdded;
	}	
	public int countAligningBases(String target, String query, int tstart, String qgapStr, String extraStr)
	{
		int nalign = 0; // if we start before begin, this many bases can't align
		int ig = 0;
		int ie = 0;
		int tpos = tstart; 
		int qpos = 0;
		// note qpos is 0-indexed, rest are 1-indexed
		
		// if the strings are empty these arrays will get one element
		String[] qgaps = qgapStr.split("\\s+");
		String[] extras = extraStr.split("\\s+");
		

		while (qpos < query.length())
		{
			if (tpos < 1)
			{
				// before the start - increment till we get to real bases
				qpos++;
				tpos++;
				continue;
			}
			
			if (!qgapStr.equals("") && ig < qgaps.length) 
			{	
				int qgap = Integer.parseInt(qgaps[ig]);
			
				if (qpos == qgap)
				{
					// there is a gap before this base
					// this has no effect on the alignment count but it does affect which subsequent target bases we are aligning to
					ig++;
					tpos++;
					continue;
				}
			}
			
			if (!extraStr.equals("") && ie < extras.length)
			{
				int extra = Integer.parseInt(extras[ie]);
				if (extra == qpos + 1)
				{
					// now we're going to skip this base, so it doesn't get aligned and detracts from the alignment count
					ie++;
					qpos++;
					continue;
				}
			}
			
			char qbase = query.charAt(qpos);
			char tbase = target.charAt(tpos-1);
			
			if (qbase == tbase)
			{
				nalign++;	
			}
			qpos++;
			tpos++;
					
		}
		return nalign;	
	}

	void dumpExtras(int[][][] Extras, int[][] MA, int row, int cidoff) throws Exception
	{
		Clone c = mID2Clone.get(MA[row][0] + cidoff);
		String msg = "Extras " + c.mFullName + ":";
		for (int col = 1; col < Extras[row].length; col++)
		{
			if (Extras[row][col] != null)
			{
				int num = Extras[row][col].length;
				msg += col + ":" + num + ",";
			}
		}
		Log.msg(msg);
	}


	public void clear() throws Exception
	{
		clearClones();
		if (mLonersID != null) mLonersID.clear(); mLonersID = null;
		if (mAllClonesID != null) mAllClonesID.clear(); mAllClonesID = null;
		if (mBuriedClonesID != null) mBuriedClonesID.clear(); mBuriedClonesID = null;
		if (mPairsIdx != null) mPairsIdx.clear(); mPairsIdx = null;
		if (mContainedPairsIdx != null) mContainedPairsIdx.clear(); mContainedPairsIdx = null;
		if (mID2Idx != null) mID2Idx.clear(); mID2Idx = null;
		mDepthMap = null;
		mBuryCloneList = null;
		mMaxR = null;
		mCCS = "";
		mQual = "";
		
	}
	public void clearSeqs()
	{
		mCCS = "";
		mQual = "";
	}
	public void loadCCS(DBConn db) throws Exception
	{
		ResultSet rs = db.executeQuery("select consensus,quality from contig where ctgid=" + mID);
		rs.next();
		mCCS = rs.getString("consensus");
		mQual = rs.getString("quality");
		rs.close();
	}
	public void loadCCS_orig(DBConn db) throws Exception
	{
		ResultSet rs = db.executeQuery("select orig_ccs from contig where ctgid=" + mID);
		rs.next();
		mCCS = rs.getString("orig_ccs");
		if (mCCS == null)
		{
			// We must have been interrupted during finalizing. Set the orig_ccs.
			db.executeUpdate("update contig set orig_ccs=consensus where ctgid=" + mID);
			rs = db.executeQuery("select orig_ccs from contig where ctgid=" + mID);
			rs.next();
			mCCS = rs.getString("orig_ccs");			
		}
		rs.close();
	}	
	public void clearClones()  throws Exception
	{
		if (mAllClonesID == null) return;
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			c.mSeq = "";
			c.mQual = "";
			c.mGaps = "";
			c.mTGaps = "";
		}
	}
	// Figure out whether the contig has backwards orientation.
	// In the correct orientation, .f should be uncomplemented.
	// Hence, we'll calculate a simple correlation between the RF and UC values for each clone.
	public boolean reversed() throws Exception
	{
		int score = 0;
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			if ( (c.mUC == UC.U && c.mRF == RF.F) || (c.mUC == UC.C && c.mRF == RF.R))
			{
				score++;
			}
			else if ( (c.mUC == UC.C && c.mRF == RF.F) || (c.mUC == UC.U && c.mRF == RF.R))
			{
				score--;
			}	
		}
		
		return (score < 0);
	}
	public String qualReverse()
	{
		String[] vals = mQual.split("\\s+");
		StringBuilder out = new StringBuilder();
		for (int i = vals.length - 1; i >= 0; i--)
		{
			out.append( vals[i]);
			out.append(" ");
		}
		return out.toString();
	}	
	// Flip the contig and reverse everything
	// Although actually only the consensus really matters because we later
	// will re-align everything to it. 
	public void flip() throws Exception
	{
		mCCS = Utils.reverseComplement(mCCS);
		mQual = qualReverse();
		for (int cid : mAllClonesID)
		{
			Clone c = mID2Clone.get(cid);
			if (c.mGaps != "")
			{
				c.flipGaps();
			}
			c.mUC = c.mUC.invert();

			// Flip around the start coord. Note that c.mEnd already includes the gaps (set in loadContigs)
			c.mStart = (mCCS.length()  - c.mEnd + 1);

		}	
	}
	public static class CapContigSizeCmp implements Comparator<SubContig>
	{
		public int compare(SubContig a, SubContig b)
		{
			int sizea = a.mAllClonesID.size();
			int sizeb = b.mAllClonesID.size();
			if (sizea > sizeb) return -1;
			else if (sizea < sizeb) return 1;
			return 0;
		}
	}

	// check that the contig consensus actually spans the clones instead
	// of being cut off, e.g. because cap3 stopped at an N block.
	// Copied from OK_CTG
	public boolean hasOverhangs()
	{
		// we don't want any clones hanging way off the end, or the beginning
		int maxHang = 20;
		
		int ccsLen = mCCS.length();
		for (int cid : mAllClonesID)
		{
			Clone c =  null;
			try
			{
				c = mID2Clone.get(cid);
			}
			catch (Exception e)
			{
				System.err.println("Unknown clone in contig:" + c.mCloneName);
				e.printStackTrace();
				System.exit(0);
			}
			if (c.mStart + c.mSeqLen > ccsLen + maxHang)
			{
				return true;
			}
			if (c.mStart < -maxHang)
			{
				return true;
			}
		}			
		return false;		
	}
	static class SizeCmp implements Comparator<SubContig>
	{
		public int compare(SubContig a, SubContig b) 
		{				
			if (a == null)
			{
				if (b == null) return 0;
				else return +1;
			}
			else if (b == null) return -1;
			int scorea = a.numClones();
			int scoreb = b.numClones();
			if (scorea > scoreb) return -1;
			else if (scorea < scoreb) return 1;
			return 0;
		}
	}		
}
