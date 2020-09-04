package sng.assem;

/*********************************************
 * Called from AssemMain when there is no assembly
 * 	both the clone and contclone tables are not necessary, but need to remove from viewSingleTCW before removing here
 * CAS304 this code was in AssemMain. There was Loc stuff that has not been tested in a long time, so removed.
 * 		Added code for TPM
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashMap;

import sng.assem.enums.LogLevel;
import sng.database.Schema;
import util.database.DBConn;
import util.database.Globalx;

public class AssemSkip {
	private DBConn mDB;
	private String mIDStr;
	private int mAID;
	private Vector<Library> mSeqLibs;
	private boolean bUseTransName;
	private boolean bUseRPKM;
	
	public AssemSkip(DBConn mDB, String mIDStr, int mAID, Vector<Library> mLibs, boolean bUseTransName, boolean bUseRPKM) {
		this.mDB = mDB;
		this.mIDStr = mIDStr;
		this.mAID = mAID;
		this.mSeqLibs = mLibs;
		this.bUseTransName = bUseTransName;
		this.bUseRPKM = bUseRPKM;
		
		skipAssembly();
	}
	
	private void skipAssembly()
	{
	try {
		Log.head("Performing instantiate", LogLevel.Basic);
		
		int nclones = mDB.executeCount("select count(*) as nclones from clone");
		
		Log.indentMsg("Instantiating " + String.format("%,d", nclones) + " sequences", LogLevel.Basic);
		
		if (bUseRPKM)      Log.indentMsg("\tCompute RPKM instead of TPM", LogLevel.Basic);
		if (bUseTransName) Log.indentMsg("\tUse names from sequence file", LogLevel.Basic);
		
		mDB.executeQuery("set foreign_key_checks = 0");
		
	// add assemlib
		for (Library lib : mSeqLibs) {
			String libid = lib.mIDStr;
			mDB.executeUpdate("insert ignore into assemlib "
				+ "(AID, LID, assemblyid, libid) values"
				+ "(1,(select lid from library where libid='" + libid + "'),'" + mIDStr + "','" + libid + "')");
		}
	
	// add contig
		mDB.tableCheckAddColumn("contig", "tmpCID", "bigint", null); // Need this to get CID from clone to contclone via contig
		
		Vector <Integer> seqID = new Vector <Integer> ();
		ResultSet rs = mDB.executeQuery("select CID from clone"); // Probably 1-#nClone, but this is safer
		while (rs.next()) seqID.add(rs.getInt(1));
		
		for (int cid : seqID) {
			mDB.executeUpdate("insert into contig " +
	     "(AID, contigid, assemblyid,     numclones,consensus,quality,consensus_bases,frpairs,finalized, tmpCID, longest_clone) " +
    "(select 1, cloneid, '" + mIDStr + "',1,        sequence, quality,length(sequence),0,     1,         CID,    cloneid from clone " +
				" where clone.CID=" + cid + ")");
		}
		
		if (!bUseTransName){ // Replace contigid with ID_<cnt>
			int ctgPad  = mDB.executeCount("select count(*) as count from contig");
			int minID  =  mDB.executeCount("select min(CTGID) as min from contig");
			String num = Integer.toString(ctgPad);
			int len = num.length();
			
			mDB.executeUpdate("update contig set contigid="
					+ "concat('" + mIDStr + "','_',lpad((CTGID-" + minID + "+1)," + len + ",0))");
		}
	
	// add contclone
		mDB.executeUpdate("insert into contclone "
		+       " (CTGID, CID,    contigid,cloneid,orient,leftpos,gaps,extras,ngaps, mismatch,numex,buried,prev_parent,pct_aligned) "
		+ "(select CTGID, tmpCID, contigid,longest_clone,'U',   1,      '',  '',    0,     0,       0,    0,     0,100 from contig)");

		mDB.tableCheckDropColumn("contig", "tmpCID");
		
		mDB.executeQuery("set foreign_key_checks = 1");
		
	// Normalize
		normalize();
	
	// Finalize
		mDB.executeUpdate("update assembly set completed=1, assemblydate=NOW() where AID=" + mAID);
		
		summarizeSkipAssembly(); 	
	}
	catch (Exception e) {Log.error(e, "Running skip assembly");}
	}
	
	/*********************************************
	 * contig has CTGid, contig_exp has CID, contclone has CTGid and CID.
	 * CTGid should equal CID, but when the contig table is deleted, the indexes were not reset
	 * Hence, the mapping. I reset the contig indexes, but am leaving the mapping anyway.
	 */
	private void normalize() 
	{
	try {
		
		Log.indentMsg("\tNormalize ", LogLevel.Basic);
		
		Schema.addCtgLibFields(mDB);  // Add L_ and LN_ for all libs
		
		Utils.singleLineMsg("Initialize");
		int cntSeq = mDB.executeCount("select count(*) from contig");
		int cntLib = mDB.executeCount("select count(*) from library"); 
		
		int [] libSize = new int[cntLib];   // For Rstat
		Lib [] libInfo = new Lib[cntLib];   // LID not same as index into libInfo
		Ctg [] ctgInfo = new Ctg[cntSeq];   // CTGid is not the index into ctgInfo
		HashMap <Integer, Integer> id2idx = new HashMap <Integer, Integer> (); // map CTGid to idx
		
		int n=0;
		ResultSet rs = mDB.executeQuery("select LID, libid, libsize from library");
		while (rs.next()) {
			libSize[n] = rs.getInt(3);
			libInfo[n++] = new Lib(rs.getInt(1), rs.getString(2), rs.getInt(3));	
		}
		
		n=0;
		rs = mDB.executeQuery("select CTGid, consensus_bases from contig");
		while (rs.next()) {
			int CTGid = rs.getInt(1);
			ctgInfo[n] = new Ctg(cntLib, CTGid, rs.getInt(2));
			id2idx.put(CTGid, n);
			n++;
		}
		
		HashMap <Integer, Integer> exp2ctg = new HashMap <Integer, Integer> (); 
		rs = mDB.executeQuery("select CTGid, CID from contclone");
		while (rs.next()) {
			exp2ctg.put(rs.getInt(2), rs.getInt(1));
		}
		for (int i=0; i<cntLib; i++) {
			rs = mDB.executeQuery("select CID, count from clone_exp where rep=0 and LID=" + libInfo[i].LID);
			while (rs.next()) {
				int cid = rs.getInt(1);
				int cnt = rs.getInt(2);
				int ctgid = exp2ctg.get(cid);
				int idx = id2idx.get(ctgid);
				
				ctgInfo[idx].cntForLib(i, cnt);
			}
		}
		exp2ctg.clear();
		
		// Rstat
		Utils.singleLineMsg("Rstat");
		RStat rsObj = new RStat(libSize);
		for (int idx=0; idx<cntSeq; idx++) {
			if (ctgInfo[idx]==null) continue;
			
			ctgInfo[idx].rstat = rsObj.calcRstat(ctgInfo[idx].libCnt);
		}
		/***************************************************************
		 * Normalize (RPKM is computed in SubContig for assembled contigs)
		 * 
		 * libInfo.libsize   = total reads for library
		 * ctgInfo.libCnt[i] = read count for contig for library
		 * ctgInfo.lenkb     = length of contig in kilobases
		 * RPKM:
			PM =   Count up the total reads in a sample and divide that number by 1,000,000; "per million" scaling factor
			RPM =  Divide the read counts by the PM scaling factor; normalizes for sequencing depth
			RPKM = Divide the RPM values by the length of the gene, in kilobases. 
		 * TPM:
		 	RPK = Divide the read counts by the length of each gene in kilobases. 
			PM =  Count up all the RPK values in a sample and divide this number by 1,000,000; “per million” scaling factor.
			TPM = Divide the RPK/PM. 
		 *****************************************************************/
		
		String type = (bUseRPKM) ? "RPKM" : "TPM";
		
		Utils.singleLineMsg("Compute " + type);
		// LN__
		if (bUseRPKM) {
			for (int i=0; i<cntLib; i++) 
				libInfo[i].PMr = libInfo[i].libSize / 1000000.0;
			
			for (int idx=0; idx<cntSeq; idx++) {
				if (ctgInfo[idx]==null) continue;
				
				for (int i=0; i<cntLib; i++) {
					double cnt=ctgInfo[idx].libCnt[i];
					if (cnt>0.0) {
						double rpm = (cnt / libInfo[i].PMr);
						ctgInfo[idx].libNorm[i] = (rpm*1000.0)/ctgInfo[idx].len; // rpm/lenKB
					}
				}
			}
		}
		else { // sum of TPM for a sample always adds up to 1M (with round-off)
			for (int i=0; i<cntLib; i++) {
				double pm=0.0;
				
				for (int idx=0; idx<cntSeq; idx++) {
					double cnt = ctgInfo[idx].libCnt[i];
					if (cnt>0) {
						ctgInfo[idx].RPKt[i] = (cnt*1000.0)/ctgInfo[idx].len; // libCnt/lenKB
						pm +=  ctgInfo[idx].RPKt[i];
					}
				}
				libInfo[i].PMt = pm/1000000.0; 
			}
			for (int idx=0; idx<cntSeq; idx++) {
				if (ctgInfo[idx]==null) continue;
				
				for (int i=0; i<cntLib; i++) {
					double rpk=ctgInfo[idx].RPKt[i];
					if (rpk>0.0) 
						ctgInfo[idx].libNorm[i] = rpk/libInfo[i].PMt; 
				}
			}
		}
		
		// Database
		Utils.singleLineMsg("Save to database");
		String L  = ", " + Globalx.LIBCNT;
		String LN = ", " + Globalx.LIBRPKM;
		String sql = "update contig set rstat=?, totalexp=?, totalexpN=? ";
		for (int i=0; i<cntLib; i++) sql +=  L + libInfo[i].libid + "=?"    + LN + libInfo[i].libid + "=?";
		sql += " where CTGID=? ";

		PreparedStatement ps = mDB.prepareStatement(sql);
		
		for (int idx=0; idx<cntSeq; idx++) {
			if (ctgInfo[idx]==null) continue;
			int total =0, totalN=0;
			for (int i=0; i<cntLib; i++) {
				total +=ctgInfo[idx].libCnt[i];
				totalN+=ctgInfo[idx].libNorm[i];
			}
			
			n=1;
			ps.setFloat(n++,(float)ctgInfo[idx].rstat);
			ps.setInt(n++, total);
			ps.setInt(n++, totalN);
			
			for (int i=0; i<cntLib; i++) {
				ps.setInt(n++, ctgInfo[idx].libCnt[i]);
				ps.setFloat(n++, (float) ctgInfo[idx].libNorm[i]);
			}
			ps.setInt(n++, ctgInfo[idx].cid);
			ps.execute();
		}
		
		ps.close();
		rs.close();
		
		mDB.executeUpdate("update assem_msg set norm='" + type + "' where aid=" + mAID);
		System.err.print("                                      \r");
	}
	catch (Exception e) {Log.error(e, "Compute lib counts");}
	}	
	/********************************************************/
	private class Ctg {
		int cid;
		double rstat;
		double len;
		
		public Ctg(int nLibs, int cid, int len) {
			libCnt = new int [nLibs];
			libNorm = new double[nLibs];
			RPKt    = new double[nLibs];
			
			for (int i=0; i<nLibs; i++) {
				libCnt[i]=0; 
				libNorm[i]=RPKt[i]=0.0;
			}
			
			this.cid = cid;
			this.len = len;
		}
		public void cntForLib(int n, int cnt) {
			libCnt[n]  = cnt;
		}
		int [] libCnt;
		double [] RPKt;
		double [] libNorm;
	}
	/********************************************************/
	private class Lib {
		int LID;
		String libid;
		int libSize=0;

		double PMr = 0; // Count up the total reads in a sample and divide that number by 1,000,000;
		double PMt = 0; // Count up the RPK values  in a sample and divide this number by 1,000,000;
		
		public Lib(int LID, String libid, int libSize) {
			this.LID = LID;
			this.libid = libid;
			this.libSize = libSize;
		}
	}
   /************************************************************************8
    * Summarize
    */
	private void summarizeSkipAssembly() 
	{		
	try {
		ResultSet rs = null;

		StringBuilder statsMsg = new StringBuilder();
		
		statsMsg.append(Log.head("Statistics",LogLevel.Basic));
		
		int[] rangeMin = new int[8];
		int[] rangeMax = new int[8];
		int[] counts = new int[8];
		
		rangeMin[0] = 1; rangeMax[0] = 100;
		rangeMin[1] = 101; rangeMax[1] = 500;
		rangeMin[2] = 501; rangeMax[2] = 1000;
		rangeMin[3] = 1001; rangeMax[3] = 2000;
		rangeMin[4] = 2001; rangeMax[4] = 3000;
		rangeMin[5] = 3001; rangeMax[5] = 4000;
		rangeMin[6] = 4001; rangeMax[6] = 5000;
		rangeMin[7] = 5001; rangeMax[7] = 1000000;
		for (int i = 0; i < counts.length; i++) counts[i] = 0;
		
		rs = mDB.executeQuery("select length(consensus) as ccslen from contig");
		while (rs.next())
		{
			int length = rs.getInt(1);
			for (int i = 0; i < rangeMin.length; i++) {
				if (rangeMin[i] <= length && rangeMax[i] >= length) counts[i]++;	
			}
		}
		statsMsg.append(Log.newLine(LogLevel.Basic));
		statsMsg.append(Log.indentMsg("Sequence lengths (bp)",LogLevel.Basic));		
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"Length","1-100","101-500","501-1000","1001-2000","2001-3000","3001-4000","4001-5000",">5000"));
		statsMsg.append(Log.columnsInd(11,LogLevel.Basic,"#Sequences",counts[0],counts[1],counts[2],counts[3],counts[4],counts[5],counts[6],counts[7]));
	}
	catch (Exception e) {Log.error(e, "Summarize");}
	}
}
