package cmp.compile;

/**********************************************
 * Add Pairs: 
 * 	 	savePairsFromHitFile creates pairs from all entries in the blast file(s)
 * Stats Panel: Compute Pearson correlation coefficient
 * MethodLoad: saveMethodPairwise which updates PCC
 * 			   fixFlagsPairwise to remove method
 * Stats Panel: saveStatsAndKaKsWrite
 * 		calls PairStats to compute the statistics
 * 		calls SumStats to compute the summary
 * Stats Panel: saveKaKsRead
 */
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import cmp.align.SumStats;
import cmp.database.*;
import cmp.compile.panels.CompilePanel;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class Pairwise {
	boolean dolog2 = false; 
	private final double GOOD_PCC = Globals.PCCcutoff; // 0.8
	
	public Pairwise(CompilePanel panel) { 
		theCompilePanel = panel;
	} 
	/******************************************************
	 * XXX Called from StatsPanel
	 * 1. doWriteKaKs says to write alignments and perform all alignments
	 * 2. !doWriteKaKs says to only do alignments from pair that have not been done before.
	 */
	public void saveStatsAndKaKsWrite(boolean doWriteKaKs) {
		try {
			PairStats statsObj = new PairStats();
			int cntPreviouslyAligned = statsObj.run(theCompilePanel, doWriteKaKs);
			
			DBConn mDB = theCompilePanel.getDBconn();
			String summary="";
			if (cntPreviouslyAligned>0) { // redo stats from scratch
				Out.PrtSpMsg(0, "Compute summary on all pairs");
			    summary = new SumStats(mDB).allCodingStats();
			    Out.prt("                                                        ");
			}
			else { // have everything necessary in ScoreCDS object
				String hangSum = statsObj.getScoreObj().createOverhang(); // only written when all are done
				Out.Print("\n"+hangSum);
				summary = statsObj.getScoreObj().createSummary(); 
			}
			mDB.executeUpdate("update info set pairInfo='" + summary + "'");
			mDB.close();
		}
		catch  (Exception e) {
			ErrorReport.prtReport(e, "Cannot add Cluster from main");
			return;
		}
	}
	
	/******************************************************
	 * XXX Read all KaKs files and enter data into database. Called from StatsPanel.
	 * The files were written with seqName1:seqName2, where seqName1<seqName2; 
	 * KaKs_calculator writes them the same way.
	 */
	public void loadKaKsRead() {
		String line="";
		try {
			String dirKaKs = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
			Out.PrtDateMsg("Read KaKs " + Globals.KaKsInSuffix + " files from " + dirKaKs);
			long time = Out.getTime();
			
			String method=null;
			HashMap <String, Integer> seqMap = new HashMap <String, Integer> ();
	
			DBConn mDB = theCompilePanel.getDBconn();
			ResultSet rs = mDB.executeQuery("select UTstr, UTid from unitrans");
			while (rs.next()) seqMap.put(rs.getString(1), rs.getInt(2));
			rs.close();
			
			PreparedStatement ps = mDB.prepareStatement("update pairwise set " +
				"ka=?, ks=?, kaks=?, pVal=?  where UTid1=? and UTid2=?"	);
			
			int cnt=0, cntSave=0, bad=0, cntNull=0, cntGt1000=0;
			double nullVal = Globalx.dNullVal;
			
		 	File dObj = new File(dirKaKs);
		 	String [] allFiles = dObj.list();
		 	mDB.openTransaction(); 
		 	for(String file : allFiles) {
		 		if (!file.endsWith(Globals.KaKsInSuffix)) continue;
		 		String path = dirKaKs + "/" + file;
		 		Out.PrtSpMsg(1, "Read " + file + "           ");
		 		BufferedReader reader = new BufferedReader ( new FileReader ( path ) );
		 		
		 		int cntLine=0;
		 		line = reader.readLine(); // header
				while ((line = reader.readLine()) != null) 
				{
					if (line.trim().equals("")) continue;
					
					cntLine++;
					String [] tok = line.split("\\s+");
					if (tok.length<9) {
						Out.PrtWarn("Cannot parse line #" + cnt + " : " + line);
						if (bad>10) {
							Out.PrtError("Too many errors - abort");
							reader.close();
							return;
						}
						bad++;
						continue;
					}
					String [] name = tok[0].split(":");
					if (name.length<2) { // CAS305
						Out.PrtError(cntLine + ". Missing names:  " + tok[0] + " (" + line + ")");
						continue;
					}
					String UTstr1=name[0], UTstr2=name[1];
					if (UTstr1.compareToIgnoreCase(UTstr2)>0) {
						Out.PrtError("seqID1 must be less than seqID2: " + tok[0]);
						Out.PrtError("Abort load!!!!");
						reader.close();
						return;
					}
					if (!seqMap.containsKey(UTstr1) || !seqMap.containsKey(UTstr2)) {
						Out.PrtError("Both seqIDs not in database: " + tok[0]);
						Out.PrtError("Abort load!!!!");
						reader.close();
						return;
					}
					if (method==null) method = tok[1];
					double ka=nullVal, ks=nullVal, kaks=nullVal, pVal=nullVal;
					try {
						if (!tok[2].equals("NA")) ka = 	Double.parseDouble(tok[2]);
						if (!tok[3].equals("NA")) ks = 	Double.parseDouble(tok[3]);
						if (!tok[4].equals("NA")) kaks = 	Double.parseDouble(tok[4]);
						else cntNull++;
						if (!tok[5].equals("NA")) pVal = 	Double.parseDouble(tok[5]);		
					}
					catch (Exception e) {
						cntNull++;
						continue;
					}
			
					if (UTstr1.compareToIgnoreCase(UTstr2) > 0) {
						UTstr1=name[2];
						UTstr2=name[1];
					}
					int id1 = seqMap.get(UTstr1);
					int id2 = seqMap.get(UTstr2);
					
					ps.setDouble(1, ka);
					ps.setDouble(2, ks);
					ps.setDouble(3, kaks);
					ps.setDouble(4, pVal);
					ps.setInt(5, id1);
					ps.setInt(6, id2);
					ps.addBatch();
					
					cnt++; cntSave++;
				    if (cntSave==1000) {
						ps.executeBatch();
						cntSave=0;
						Out.r("added " + cnt);
					}   
				}
				reader.close();
		 	}
		 	if (cntSave>0) ps.executeBatch();
		 	Out.r("                                                   ");
		 	mDB.closeTransaction(); 
		 	
			// output to terminal
			Out.PrtSpCntMsg(2, cnt, "loaded                 ");
			if (cntNull>0) Out.PrtSpCntMsg(2, cntNull, " with null entries               ");
			if (cntGt1000>0)Out.PrtSpCntMsg(2, cntGt1000, " KaKs>1000               ");
			
			new SumStats(mDB).allKaKsStats(method);
			mDB.close();
			
			Out.PrtSpMsgTime(0, "Complete KaKs ", time);
		}
		catch  (Exception e) {
			ErrorReport.prtReport(e, "Reading iTCW KaKs file line\n" + line);
			return;
		}
	}

	/*****************************************************************
	 * The following two methods create and update the pairwise table.
	 * There is also a method in ScoreCDS which adds CDS score.
	 */
	public boolean savePairsFromHitFile(String hitFile, boolean isAA) {
		try {
			long time = Out.getTime();
			String delim = "|";
			String delimPat = "\\|";
			
			DBConn mDB = theCompilePanel.getDBconn();
			Out.PrtSpMsg(1, "Add pairs to database from " + hitFile);
		
			HashMap <String, Seq> seqNameMap = new HashMap <String, Seq> (); //  sequence hit info
			HashMap <String, Boolean> pairMap = new HashMap <String, Boolean> (); // name1|name2
			int nAsm = mDB.executeCount("select max(ASMid) from assembly");
			
	// create map of sequences
			String sql = "Select ASMid, UTid, UTstr, ntLen, aaLen, nPairs, orf_start, orf_end from unitrans";
			if (isAA) sql += " where aaLen>0";
			else sql += " where ntLen>0"; 
			ResultSet rs = mDB.executeQuery(sql);
			while (rs.next()) {
				int asmID = rs.getInt(1);
				int seqID = rs.getInt(2);
				String seqName = rs.getString(3);
				int len = (isAA) ?  rs.getInt(5) :  rs.getInt(4);
				
				int nPairs=rs.getInt(6);
				int s = rs.getInt(7);
				int e = rs.getInt(8);
				
				int cdsLen = (e>0) ? Math.abs(s-e)+1 : 0;  // CAS305 if NT and AA, the AA cdsLen was 1
				
				Seq seq = new Seq(seqName, seqID, asmID, len, nPairs, cdsLen, nAsm);
				seqNameMap.put(seqName, seq);
			}
			
	// create map of pairs  already in database
			rs = mDB.executeQuery("select UTstr1, UTstr2 from pairwise");
			while (rs.next()) {
				String key = rs.getString(1) + delim + rs.getString(2);
				pairMap.put(key, false);
			}
			Out.PrtSpCntMsgZero(3, pairMap.size(), "existing pairs in database");
			
			
			
	// Create sql 
			String pre = (isAA) ? "aa" : "nt";
			String  evalCol =  pre+ "Eval";
			String 	simCol =   pre+ "Sim"; 
			String 	alignCol = pre+ "Align";
			String 	gapCol =   pre+ "Gap";
			String 	olap1Col = pre+ "Olap1";
			String 	olap2Col = pre+ "Olap2";
			String 	bitCol =   pre+ "Bit";
		
			MethodLoad load = new MethodLoad(mDB);
			PreparedStatement psI = mDB.prepareStatement("insert into pairwise set " +
					"ASMid1=?, ASMid2=?, UTid1=?, UTid2=?, UTstr1=?, UTstr2=?, " 
					+ evalCol + "=?," + simCol + "=?," + alignCol + "=?," + gapCol + "=?," 
					+ olap1Col+ "=?," + olap2Col + "=?," + bitCol +"=?, HITid=?, HITstr=?," +
					"CDSlen1=?, CDSlen2=? " + 
					", aaBest=?, PCC = -2"); 
					
			PreparedStatement psU = mDB.prepareStatement("update pairwise set " 
					+ evalCol + "=?," + simCol + "=?," + alignCol + "=?," 
					+ gapCol + "=?," + olap1Col + "=?," + olap2Col + "=?," + bitCol + "=? "
					+ " where UTid1=? and UTid2=?");
		
			int [] ids = new int [2];
			
		/** Main Loop -- read file and save **/
			BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(hitFile))));
			String curLine = "";
			int cntRead=0, cntAddPair=0, cntUpdate=0,  cntMiss=0, cntAAbest2=0;
			int cntBatchI=0, cntBatchU=0,  cntNoShare=0, cntUnique=0, cntPrt=0;
			
			double avgSim=0.0, avgOlap=0.0, sumID=0, sumAlign=0, sumLen=0;
			int cntSame=0, cntDiff=0, cntTotal=0;
			
			mDB.openTransaction(); 
			
			// combinedFile has dataset|seqname, so blast file should be like this
			while((curLine = br.readLine()) != null) {
				if (curLine.length() == 0 || curLine.charAt(0) == '#') continue; 
				cntRead++;
			
				String tokens[] = curLine.split("\t");
				String name1 = tokens[0].trim();
				String name2 = tokens[1].trim();
				if(name1.equals(name2)) continue;  // *** don't process self-hit
				
				if (name1.contains(delim)) name1 = name1.split(delimPat)[1];
				if (name2.contains(delim)) name2 = name2.split(delimPat)[1];
				
				if (!seqNameMap.containsKey(name1) || !seqNameMap.containsKey(name2)) {
					cntMiss++;
					if (cntMiss<4) {
						if (!seqNameMap.containsKey(name1)) Out.PrtWarn("No sequence for '" + name1 + "' in database");
						if (!seqNameMap.containsKey(name2)) Out.PrtWarn("No sequence for '" + name2 + "' in database");
					}
					continue;
				}
				
				// keep track of which is first in hit file
				Seq seqX1 = seqNameMap.get(name1);
				Seq seqX2 = seqNameMap.get(name2);
				
				/** Important: all code expects name1<name2!! Only in pairwise once in this order **/
				/** when SQL sorts, it ignores case!! **/
				if (name1.compareToIgnoreCase(name2)>0) {
					String tmp = name1;
					name1 = name2;
					name2 = tmp;
				}
				String key = name1 + delim + name2;
				
				// if in pairMap with value of true, already added for the hitfile			  
				if (pairMap.containsKey(key) && pairMap.get(key)) {
					if (seqX1.asmID!=seqX2.asmID) {
						if (!seqX1.hasMate(seqX2.asmID))  
							 seqX1.setMate(seqX2);	
					}
					continue; 	
				}
				
				int aaBest=0;
				if (seqX1.asmID!=seqX2.asmID) {
					if (!pairMap.containsKey(key)) {
						seqX1.nPairs++;
						seqX2.nPairs++;
					}
					if (!seqX1.hasMate(seqX2.asmID)) {
						 seqX1.setMate(seqX2);
						 aaBest=2; // enter as bi-directional so only correct the ones that aren't
						 cntAAbest2++;
					}
				} 
				
				Seq seq1 =  seqNameMap.get(name1);
				Seq seq2 =  seqNameMap.get(name2);
				
				double dsim = 	Double.parseDouble(tokens[2]);
				int align = 	Integer.parseInt(tokens[3]);
				int gap = 		Integer.parseInt(tokens[5]);
				double eval = 	Double.parseDouble(tokens[10]);
				int bit = (int) (Double.parseDouble(tokens[11])+0.5);
	
				double olap1= ((double)align/(double) seq1.seqLen)*100.0;
				double olap2= ((double)align/(double) seq2.seqLen)*100.0;
				//if (olap1>100.0) olap1=100.0; Let go over 100, happens with gaps
				//if (olap2>100.0) olap2=100.0;
				
				if (!pairMap.containsKey(key)) {
					ids[0] = seq1.seqID;
					ids[1] = seq2.seqID;
					String [] rt = load.descriptComputeBest(cntAddPair, ids);
					cntAddPair++; 
					
					String hitID = rt[0];
					String hitStr = rt[1];
		
					if (hitStr.equals(Globals.uniqueID)) cntUnique++;
					if (hitStr.equals(Globals.noneID)) cntNoShare++;
					
					if (!isAA) aaBest=0;
					
					psI.setInt(1, seq1.asmID);
					psI.setInt(2, seq2.asmID);
					psI.setInt(3, seq1.seqID);
					psI.setInt(4, seq2.seqID);
					psI.setString(5, name1);
					psI.setString(6, name2);
					psI.setDouble(7, eval);
					psI.setDouble(8, dsim);
					psI.setDouble(9, align);
					psI.setInt(10, gap);
					psI.setDouble(11, olap1);
					psI.setDouble(12, olap2);
					psI.setInt(13, bit);
					psI.setString(14, hitID);
					psI.setString(15, hitStr);
					psI.setInt(16, seq1.cdsLen);
					psI.setInt(17, seq2.cdsLen);
					psI.setInt(18, aaBest);
					psI.addBatch(); 
					cntBatchI++;
					if (cntBatchI==1000) {
						psI.executeBatch();
						cntBatchI=0;
					}
					pairMap.put(key, true);
				}
				else { // previously added with AA hit file; update with NT
					psU.setDouble(1, eval);
					psU.setDouble(2, dsim);
					psU.setInt(3, 	align);
					psU.setInt(4, 	gap);
					psU.setDouble(5, olap1);
					psU.setDouble(6, olap2);
					psU.setInt(7, 	bit);
					psU.setInt(8, 	seq1.seqID);
					psU.setInt(9, 	seq2.seqID);
					psU.addBatch();
				 
				   cntUpdate++;  cntBatchU++;
				   if (cntBatchU==1000) {
						psU.executeBatch();
						cntBatchU=0;
				   }
				   pairMap.put(key, true); // set false to true indicating its in the file
				}
				if (seq1.asmID!=seq2.asmID) cntDiff++;
				else cntSame++;
				
				// CAS310 compute %sim over all aligned and average sim; same for olap
				sumLen += (seq1.seqLen + seq2.seqLen);
				double id = align*(dsim/100.0);
				sumID += (int) (id+0.5);
				sumAlign += align;
				
				avgOlap += (olap1+olap2);
				avgSim += dsim;
				cntTotal++; cntPrt++;
				
				if (cntPrt==1000) {
					Out.r("pairs " + cntTotal + " read " + cntRead);
					cntPrt=0;
				}
			}
			if (cntBatchI>0) psI.executeBatch();
			if (cntBatchU>0) psU.executeBatch();
			psI.close(); psU.close();
			System.err.print("                                                        \r");
			br.close();
			rs.close();
			mDB.closeTransaction(); 
	
			Out.PrtSpCntMsg(3, cntRead, "Read");
			Out.PrtSpCntMsg(3, cntAddPair, "Add pair");
			Out.PrtSpCntMsgZero(3, cntNoShare,  "Pairs with no shared hit description (NoShare)");
			Out.PrtSpCntMsgZero(3, cntUnique,   "Pairs with no hits (Novel)");
			
			int nShare = cntAddPair-cntNoShare;
			String x = Out.perFtxtP(nShare, cntAddPair);
			Out.PrtSpCntMsgZero(3, nShare, "Pairs with shared hit description, including novel " + x);
			Out.PrtSpCntMsgZero(3, cntUpdate,  "Update pair");
			Out.PrtSpCntMsgZero(3, cntMiss,    "SeqID NOT in database");
			
			if (cntMiss>0 && (cntAddPair+cntUpdate)==0)
				ErrorReport.die("Abort -- hit file has the wrong seqIDs");
	
			Out.PrtSpCntMsgTimeMem(3, (cntAddPair+cntUpdate), "total " + pre.toUpperCase() + " pairs", time);
			
	/** find non-bidirectional  **/
			if (isAA) { // actually, marking one-sided bi-directional, but that is a confusing message
				time = Out.getTime();
				
				Out.PrtSpMsg(2, "Processing bi-directional AA pairs");
				mDB.openTransaction();
				psU = mDB.prepareStatement("update pairwise set aaBest=1 " +
						"where UTid1=? and UTid2=?"); 
				cntUpdate=cntBatchU=0;
				
				for (String key : pairMap.keySet()) {
					if (!pairMap.get(key)) continue; 
					
					String [] seqID = key.split(delimPat);
					Seq seq1 =  seqNameMap.get(seqID[0]);
					Seq seq2 =  seqNameMap.get(seqID[1]);
					
					if (seq1.asmID==seq2.asmID) continue;
					
					if (( seq1.isMate(seq2) && !seq2.isMate(seq1)) ||
					    (!seq1.isMate(seq2) &&  seq2.isMate(seq1))) {
				
						psU.setInt(1, seq1.seqID);
						psU.setInt(2, seq2.seqID);
						psU.addBatch();
						 
						cntUpdate++;  cntBatchU++;
					    if (cntBatchU==1000) {
							psU.executeBatch();
							cntBatchU=0;
							Out.r("processed " + cntUpdate);
						}
					}
				}
				if (cntBatchU>0) psU.executeBatch();
				psU.close();
				mDB.closeTransaction();
				x = Out.perFtxtP(cntAAbest2, cntTotal);
				Out.PrtSpCntMsg(3, cntAAbest2, "candidate bi-directional AA best hit " + x);
				x = Out.perFtxtP(cntUpdate, cntTotal);
				Out.PrtSpCntMsgTimeMem(3, cntUpdate, "one-sided-directional AA best hit " + x, time);
			}
		
		/*********** Update unitrans ******************/
			Out.PrtSpMsg(2, "Add pair count to sequence table");
			time = Out.getTime();
			
			mDB.openTransaction();
			psU = mDB.prepareStatement("update unitrans set nPairs=? where UTid=?"); 
			cntUpdate=cntBatchU=0;	
			int cntPairs=0;
			for (Seq seq : seqNameMap.values()) {
				cntPairs+=seq.nPairs;
				psU.setInt(1, seq.nPairs);
				psU.setInt(2, seq.seqID);
				psU.addBatch();
				 
				cntUpdate++;  cntBatchU++;
			    if (cntBatchU==1000) {
					psU.executeBatch();
					cntBatchU=0;
					Out.r("update " + cntUpdate);
				}
			}
			if (cntBatchU>0) psU.executeBatch();
			psU.close();
			mDB.closeTransaction();
			System.err.print("                                                        \r");
			Out.PrtSpMsgTimeMem(3, Out.avg(cntPairs, cntUpdate) + " average number pairs per sequence", time);
		
			/*************************************** 
			* OVERVIEW-INFO
			*********/
			if (isAA) Out.PrtSpMsg(2, "AA overview info for Hit Pairs");
			else      Out.PrtSpMsg(2, "NT overview info for Hit Pairs");
			new Summary(mDB).removeSummary();
			
			avgSim  /= cntTotal;
			avgOlap /= (cntTotal+cntTotal);
			
			// CAS310 
			double sumOlap = ((sumAlign*2.0)/sumLen)*100.0;
			double sumSim =  (sumID  / sumAlign)    *100.0;
			
			sql = String.format("Diff %-7s Same %-7s  Similarity %3.1f%s (%3.1f%s)  Coverage %3.1f%s (%3.1f%s)", 
					Out.kMText(cntDiff), Out.kMText(cntSame), sumSim, "%", avgSim, "%", sumOlap, "%", avgOlap, "%");
			
			if (isAA) mDB.executeUpdate("update info set aaInfo='" + "AA   " + sql + "'");
			else      mDB.executeUpdate("update info set ntInfo='" + "NT   " + sql + "'");

			Out.PrtSpMsg(3, sql);
				
			mDB.close();
			return true;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error loading pairs");}
		return false;
	}
	
	/********************************************************
	 * Called from MethodPanel when a method is removed -- it has already been removed from pairwise
	 * remove cluster method from pairwise
	 * update hasGrp and hasBBH
	 */
	public void fixFlagsPairwise(DBConn mDB) {
		try {
		// create methodsBBHMap -- method name if whether it is BBH
			//  the removed method has been removed
			HashMap <String, Boolean> methodsBBHMap = new HashMap<String, Boolean> ();
			ResultSet rs = mDB.executeQuery("select prefix, PMtype from pog_method");
			while (rs.next()) {
				String type = rs.getString(2);
				boolean b = (type.equals(Globals.Methods.BestRecip.TYPE_NAME)) ? true : false;
				methodsBBHMap.put(rs.getString(1), b);
			}
			if (methodsBBHMap.size()==0) {
				Out.PrtWarn("All methods have been removed"); // should not happen
				return;
			}
			int cnt=0;
		
		// create pairGrpMap -- group name and whether it is BBH 
			HashMap <Integer, Boolean> pairGrpMap  = new HashMap <Integer, Boolean> ();
					
			String sql ="select PAIRid ";
			for (String m : methodsBBHMap.keySet()) sql += "," + m;
			sql += " from pairwise where hasGrp=1";
			rs = mDB.executeQuery(sql);
			
			while (rs.next()) {
				int pairid = rs.getInt(1);
				
				for (String m : methodsBBHMap.keySet()) {
					String grpName = rs.getString(m);
					if (grpName==null || grpName.equals("")) continue;
					
					boolean isBBH = methodsBBHMap.get(m);
					pairGrpMap.put(pairid, isBBH);
				}
				cnt++;
				if (cnt%100==0) Out.r("checked " + cnt);
			}
			System.err.print("                                                      \r");
			rs.close();
			
		// set hasGrp, hasBBH
			int total = pairGrpMap.size();
			Out.PrtSpMsg(2, "Correct flags for " + total + " pairs");
			mDB.executeUpdate("update pairwise set hasGrp=0, hasBBH=0");
			
			mDB.openTransaction(); 
			int cntSave=0;
			cnt=0;
			PreparedStatement ps = mDB.prepareStatement(
					"update pairwise set hasBBH=?, hasGrp=1 where PAIRid=? ");
			for (int id : pairGrpMap.keySet()) { 
				boolean isBBH = pairGrpMap.get(id);
				int x = (isBBH) ? 1 : 0;
				ps.setInt(1, x);
				ps.setInt(2, id);
				ps.addBatch();
				
				cnt++; cntSave++;
				if (cntSave==1000) { 
					cntSave=0;
					ps.executeBatch();
					Out.rp("update ", cnt, total);
				}
			}
			if (cntSave>0) ps.executeBatch();
			mDB.closeTransaction();
			Out.PrtSpCntMsg(2, cnt, "Pairs updated");
		}
		catch (Exception e) {ErrorReport.prtReport(e,  "Removing and Updating Pairwise with methods");}
	}
	/**********************************************************
	 * Add cluster name to pairwise and set hasGrp=1 for each pair (will compute statistics for hasGrp==1)
	 * If BBH, also set hasBBH=1 (will compute overall statistics for hasBBH=1)
	 */
	public void saveMethodPairwise(DBConn mDB, int PMid, String prefix) {
		try {
			Out.PrtSpMsg(1,"Adding cluster pairs to pairwise for " + prefix);
		
			String type = mDB.executeString("select PMtype from pog_method where PMid=" + PMid);
			boolean isBBH = (type.equals(Globals.Methods.BestRecip.TYPE_NAME)) ? true : false;
			
			HashMap <Integer, Grp>  grpMap = new HashMap <Integer, Grp> ();
			ResultSet rs = mDB.executeQuery(
					"select PGid, PGstr, HITid, count from pog_groups where PMid=" + PMid);
			long numPairs=0;
			while (rs.next()) {
				int grpID = rs.getInt(1);
				int count = rs.getInt(4);
				Grp g = new Grp(grpID, rs.getString(2), rs.getInt(3), count);
				grpMap.put(grpID, g);
				numPairs += (count*count-1)/2;
			}
			Out.PrtSpCntMsg(2, numPairs, " approximate # pairs to update");
			
			PreparedStatement ps;
			if (isBBH) ps = mDB.prepareStatement("update pairwise set hasBBH=1, hasGrp=1,  " +
								 prefix + "=? where UTid1=? and UTid2=? ");
			else       ps = mDB.prepareStatement("update pairwise set hasGrp=1, " +
								 prefix + "=? where UTid1=? and UTid2=? ");
			mDB.openTransaction(); 
			
			int cntUp=0, cntNotPair=0, cntAllPairs=0;
			for (int gid : grpMap.keySet()) {
				Grp grp = grpMap.get(gid);
				int [] seqID = new int [grp.cnt];
				String [] seqName = new String [grp.cnt];
				
			/** order by UTstr is important since the UTstr1<UTstr2 in pairwise **/
				rs = mDB.executeQuery("select UTid, UTstr from pog_members where PGid=" + gid + 
						" order by UTstr"); 
				int ns=0;
				while (rs.next()) {
					seqID[ns] = rs.getInt(1);
					seqName[ns] = rs.getString(2);
					ns++;
				}
				for (int i=0; i<grp.cnt-1; i++) {
					for (int j=i+1; j<grp.cnt; j++) {
						int exists = mDB.executeCount("select count(*) from pairwise " +
								" where UTid1=" + seqID[i] + " and UTid2=" + seqID[j] + " limit 1");
						if (exists>0) {
							ps.setString(1, grp.grpName);
							ps.setInt(2, seqID[i]);
							ps.setInt(3, seqID[j]);
							ps.execute();
							cntUp++; 
						}
						else {  
							// reverse check is just to make sure....
							exists = mDB.executeCount("select count(*) from pairwise " +
									" where UTid1=" + seqID[j] + " and UTid2=" + seqID[i] + " limit 1");
							if (exists>0) {
								ps.setString(1, grp.grpName);
								ps.setInt(2, seqID[j]);
								ps.setInt(3, seqID[i]);
								ps.execute();
								cntUp++; 
							}
							else {
								cntNotPair++;
							}
						}
						cntAllPairs++; 
						if (cntAllPairs%10000==0) Out.r("processed " + cntAllPairs);
					}
				}
			}
			System.err.print("                                                            \r");
			rs.close();
			mDB.closeTransaction(); 
			
			if (cntUp==0) {
				Out.PrtError("The update should not be zero. Please email tcw@agcol.arizona.edu with this problem");
				Out.PrtSpMsg(1, "This is NOT your error. But due to the many types of input, some may not work.");
				Out.PrtSpMsg(1, "TCW will be fixed to work for your dataset.");
				return;
			}
			Out.PrtSpCntMsg(2, cntUp, " updated pairs with method");
			Out.PrtSpCntMsgZero(2, cntNotPair, " pairs in clusters together, but not a hit pair");
			
			if (theCompilePanel.getDBInfo().hasPCC())
				new PCC().addPCCforNewMethod(PMid); // if PCC computed, not added to new Methods
		}
		catch (Exception e) {ErrorReport.prtReport(e, "write clusters to file"); }
	}
	public void computeAndSavePCC() {
		new PCC().savePCC();
	}
	
	/********************************************************
	 * XXX Peason Correlation Coefficient comparing RPKM of pairs
	 */
	private class PCC {
		public void savePCC () {
			try {
				DBConn mDB = theCompilePanel.getDBconn();
				mDB.executeUpdate("update pog_groups SET perPCC = -1" ); 
				
				long time = Out.getTime();
				Out.PrtDateMsg("Computing PCC on TPM values");
				ResultSet rs;
			
		// get library column headers and make SQL and list
				DBinfo info = theCompilePanel.getDBInfo();
				String [] libList = info.getSeqLib();
				String libSQL =     info.getSeqLibSQL();
				int nLib = libList.length;
		
		// get existing pairs
				Out.PrtSpMsg(1, "Compute pairwise PCC");
				
				HashSet <Pair>           pairSet    = new HashSet <Pair> ();
				HashMap <Integer, Seq>   seqLibMap  = new HashMap <Integer, Seq> ();
				
				rs = mDB.executeQuery("select UTid1, UTid2 from pairwise");
				while (rs.next()) {
					Pair p = new Pair();
					p.id1 = rs.getInt(1);
					p.id2 = rs.getInt(2);
					pairSet.add(p);
				
					if (!seqLibMap.containsKey(p.id1)) seqLibMap.put(p.id1, new Seq(nLib));
					if (!seqLibMap.containsKey(p.id2)) seqLibMap.put(p.id2, new Seq(nLib));
				}
				Out.PrtSpCntMsg(2, pairSet.size(), "Pairs");
				
		// get RPKM
				double log2 = Math.log(2);
				rs = mDB.executeQuery("select UTid, " + libSQL + " from unitrans");
				while (rs.next()) {
					int seqid = rs.getInt(1);
					if (!seqLibMap.containsKey(seqid)) continue;
					
					Seq seqObj = seqLibMap.get(seqid);
					for (int j=0; j< libList.length; j++)  {
						double r = rs.getDouble(Globals.PRE_LIB + libList[j]);
						if (dolog2 && r != Globalx.dNoVal) { 
							double l = Math.log(r)/log2;
			    	   			if (l < 0) l = 0;
			    	   			seqObj.pkm[j] = l;
						}
						else seqObj.pkm[j] = r;
					}
				}
		// compute PCC per pair
				mDB.openTransaction(); 
				PreparedStatement psU = mDB.prepareStatement("update pairwise set" +
						" PCC=? where UTid1=? and UTid2=?");
				int cntPCC=0, cnt=0, cntSave=0;
				for (Pair pair : pairSet) {
					Seq s1 = seqLibMap.get(pair.id1);
					Seq s2 = seqLibMap.get(pair.id2);
					pair.pcc = getCorrelation(pair.id1, pair.id2, s1.pkm, s2.pkm);
					
					psU.setDouble(1, pair.pcc);
					psU.setInt(2, 	pair.id1);
					psU.setInt(3, 	pair.id2);
					psU.addBatch();
				 
				   cnt++;  cntSave++;
				   if (cntSave==1000) {
						psU.executeBatch();
						cntSave=0;
						Out.r("computed " + cnt);
					}
				   
					if (pair.pcc>=GOOD_PCC) cntPCC++;
					String key = pair.id1 + ":" + pair.id2;
					pairPccMap.put(key, pair.pcc);
				}
				if (cntSave>0) psU.executeBatch();
				psU.close();
				mDB.closeTransaction(); 
				
				System.err.print("                                                    \r");
				Out.PrtSpCntMsg(2, cntPCC, "Pairs with PPC >= " + GOOD_PCC + "                           ");
				pairSet.clear();
				seqLibMap.clear();
			
				computePCCforGrps(mDB, 0); // Compute the Cluster PCC scores using pairPccMap
				
				mDB.executeUpdate("update info set hasPCC=1"); // CAS301 add
				mDB.close();
		
				Out.PrtMsgTimeMem("Finish computing PCC", time);
			}
			catch (Exception e) {ErrorReport.die(e, "Error add PCC to Method");}
		}
		
		  // Pearson Correlation Coefficient: verified with on-line http://www.endmemo.com/statistics/cc.php
	    private double getCorrelation(int ix, int iy, Double [] xVect, Double [] yVect) {
	    		if (xVect==null || yVect==null) {
	    			Out.debug(ix + "," + iy + " null xVect or yVect ");
	    			return 0;
	    		}
	        double sumX=0.0, sumY=0.0, sumXY = 0.0, powX=0.0, powY=0.0;
	        int sz = 0;
	        try {
		        for(int i = 0; i < xVect.length; i++)
		        {
		            double x = xVect[i];
		            double y = yVect[i];
		            if (x == Globalx.dNoVal || y == Globalx.dNoVal) continue;
	        			sz++;
		            sumX  += x;
		            sumY  += y;
		            powX  += (x*x);
		            powY  += (y*y);
		            sumXY += (x*y);
		        }	
		        if (sz==0) return 0.0;
		        double dsz = (double) sz;
		        double num = sumXY - ((sumX*sumY)/dsz);	        
		        double demX = powX - ((sumX * sumX)/dsz);
		        double demY = powY - ((sumY * sumY)/dsz);
		        double dem = Math.sqrt(demX * demY);
		        double r = (num!=0 && dem!=0) ? (num/dem) : 0.0;
		        return r;
	        }
	        catch (Exception e) {
	        		ErrorReport.reportError(e, "Internal error: computing correlation");
	        		return 0.0;
	        }
	    }//end: GetCorrelation(X,Y)
	    
	    // for updating new methods after PCC is computed
	    private void addPCCforNewMethod(int PMid) {
		    	try {
				DBConn mDB = theCompilePanel.getDBconn();
				ResultSet rs = mDB.executeQuery("select UTid1, UTid2, PCC from pairwise");
				while (rs.next()) {
					String key = rs.getInt(1) + ":" + rs.getInt(2);
					pairPccMap.put(key, rs.getDouble(3));
				}
				computePCCforGrps(mDB, PMid);
		    	}
		    	catch (Exception e) {ErrorReport.die(e, "Read and compute cluster PCC");}
	    }
	 /***************************************************
	  * Compute cluster stats of %PCC and minPCC
	  ***************************************************/
 		private void computePCCforGrps(DBConn mDB, int PMid) {
 			try {
 				int cntGrp = mDB.executeCount("select count(*) from pog_groups");
 				if (cntGrp==0) return; 
 				
 				Out.PrtSpMsg(1, "Compute cluster PCC values");
 				
 				String sql = "select mem.PGid, mem.UTid from pog_members as mem";
 				if (PMid>0) sql += " join pog_groups as grp on grp.PGid=mem.PGid where grp.PMid=" + PMid;
 				else sql += " order by PGid";
 				ResultSet rs = mDB.executeQuery(sql);
 				
 				while (rs.next()) {
 					grpid = rs.getInt(1);
 					memid = rs.getInt(2);
 					if (savegrpid==-1) savegrpid=grpid;
 					
 					if (grpid==savegrpid) {  // gather members
 						mems.add(memid);
 						continue;
 					}

 					computeGrpPCC();		 // compute for current group
 					if (cntAll%100==0) Out.r("compute group PCC " + cntAll);
 					cntAll++;
 				}
 				computeGrpPCC();
 				System.err.print("                                                       \r");
 				
 				int cnt=0, cntSave=0;
 				mDB.openTransaction(); 
 				PreparedStatement psG = mDB.prepareStatement("update pog_groups set" +
 							" perPCC=?, minPCC=? where PGid=?");
 				for (int id : grpCntMap.keySet()) {
 					goodPCC = grpCntMap.get(id);
 					int pcc = (int) (goodPCC+0.5);
 					psG.setInt(1, pcc);
 					psG.setDouble(2, grpMinMap.get(id));
 					psG.setInt(3, id);
 					psG.addBatch(); 
 					
 					 cnt++;  cntSave++;
 					 if (cntSave==1000) {
 						psG.executeBatch();
 						cntSave=0;
 						Out.r("save " + cnt);
 					}
 				}
 				if (cntSave>0) psG.executeBatch();
 				psG.close();
 				mDB.closeTransaction(); 
 				rs.close();
 				
 				Out.PrtSpCntMsg(2, cnt, "Updated clusters");
 				Out.PrtSpCntMsg(2, goodclusters, "Clusters with 100% PCC >= " + GOOD_PCC);
 			}
 			catch (Exception e) {ErrorReport.die(e, "Error add PCC to Clusters");}
 		}
 		private void computeGrpPCC() {
 			cntGood=cntPairs=0;		// compute %good
 			for (int i=0; i<mems.size()-1; i++) {
 				int id1 = mems.get(i);
 				
 				for (int j=i+1; j<mems.size(); j++) {
 					int id2 = mems.get(j);
 					
 					String key = id1 + ":" + id2;
 					if (!pairPccMap.containsKey(key)) {
 						key = id2 + ":" + id1; 
 						if (!pairPccMap.containsKey(key)) continue; // shouldn't happen...
 					}
 					
 					double pcc = pairPccMap.get(key);
 					if (pcc>=GOOD_PCC) cntGood++;
 					if (minPCC>pcc) minPCC=pcc;
 					cntPairs++;
 				}
 			}
 			goodPCC = 0.0;
 			if (cntGood>0 && cntPairs>0) 
 				goodPCC = ((double)cntGood/(double)cntPairs) * 100.0; 
 			grpCntMap.put(savegrpid, goodPCC);
 			grpMinMap.put(savegrpid, minPCC);
 			
 			if (goodPCC>=100) goodclusters++;
 			
 			// start new group 
 			savegrpid = grpid;
 			mems.clear();
 			mems.add(memid);
 			minPCC=1000.0;
 		}
	   
		private int goodclusters=0;
		private int savegrpid=-1, grpid=0, memid=0, cntGood=0, cntPairs=0, cntAll=0;
		private double goodPCC=0.0, minPCC=1000.0;
		
		private HashMap <String, Double> pairPccMap = new HashMap <String, Double> ();
		private ArrayList <Integer> mems = new ArrayList <Integer> ();
		private HashMap <Integer, Double> grpCntMap = new HashMap <Integer, Double> ();
		private HashMap <Integer, Double> grpMinMap = new HashMap <Integer, Double> ();
	}
	private class Pair {
		int id1, id2;
		double pcc;
	}
	private class Seq {
		public Seq (int n) {
			pkm = new Double [n];
		}
		Double [] pkm;
		
		public Seq(String name, int seqID, int asmID, int len, int nPairs, int cdsLen, int n) {
			this.seqID = seqID;
			this.seqLen = len;
			this.asmID = asmID;
			this.nPairs = nPairs;
			this.cdsLen = cdsLen;
			
			mateID = new int[n+1];
			for (int i=0; i<=n; i++) mateID[i]=0;
		}
		public boolean hasMate(int asmID) {
			if (mateID[asmID]>0) return true;
			else return false;
		}
		public boolean isMate(Seq mate) {
			if (mateID[mate.asmID]==mate.seqID) return true;
			else return false;
		}
		public void setMate(Seq mate) {
			mateID[mate.asmID] = mate.seqID;
		}
		
		int seqID, asmID, seqLen=0, nPairs=0, cdsLen=0;
		int [] mateID; // asmID is index
	}
	private class Grp {
		public Grp(int grpID, String grpName, int hitID, int cnt) {
			this.grpName = grpName;
			this.cnt = cnt;
		}
		int cnt;
		String grpName="";
	}
	private CompilePanel theCompilePanel;
}
