package cmp.compile;

/********************************************************
 * runMulti: pairwise - align pairs in clusters that have not been aligned.
 * 		calls ScoreCDS
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashSet;

import cmp.compile.panels.CompilePanel;
import cmp.database.Globals;
import cmp.align.*;

import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.FileHelpers;

public class PairStats {
	private final int MIN_ALIGN = 20; // not write to KaKs file
	private String kaksEx = "KaKs_Calulator"; // add prefix
	
	public int run(CompilePanel compilePanel, boolean bKaKs) {
		theCompilePanel = compilePanel;
		isKaKs = bKaKs;
		long startTime = Out.getTime();
		Out.PrtDateMsg("Compute DP alignment for all cluster pairs");
		
		if (isKaKs) {
			kaksEx = FileHelpers.getUserDir() + "/" + FileHelpers.getExtDir() + Globals.Ext.kaksExe + " ";
		}
		
		try {
			mDB = theCompilePanel.getDBconn(); 
			
			loadPairsFromDB();
			if (pairSet.size()==0) {
				if (cntAlreadyAligned==0) return -1; // no pairs, no aligned
				return cntAlreadyAligned; // no pairs, all aligned - force computation of summary
			}
		
			if (!initKaKs()) return -1; 
			scoreObj = new ScoreCDS();
			
			PairSumStats sumObj = new PairSumStats(mDB); // CAS310 was creating new obj each time through loop
			PairAlignData cdsAlnObj = new PairAlignData();
			PairAlignData utr5AlnObj = new PairAlignData();
			PairAlignData utr3AlnObj = new PairAlignData();
			
			for (Pair p : pairSet) {
				curPair = p;		
				
				// Aligns AA and fits to CDS
				cdsAlnObj.run(mDB, curPair.seqName, PairAlignData.AlignCDS_AA); 
				if (!cdsAlnObj.isGood()) {
					Out.PrtError("aligning " + curPair.seqName[0] + " " + curPair.seqName[1]);
					cntBadAlign++;
					continue;
				}
				curPair.alignCDS[0] = cdsAlnObj.getAlignCropSeq1();
				curPair.alignCDS[1] = cdsAlnObj.getAlignCropSeq2();
				
				scoreObj.scoreCDS(curPair.cdsLen, curPair.alignCDS);
				
				// Aligns NT
				utr5AlnObj.utrAlign(cdsAlnObj.get5UTR1(), cdsAlnObj.get5UTR2(), PairAlignData.AlignNT);
				utr3AlnObj.utrAlign(cdsAlnObj.get3UTR1(), cdsAlnObj.get3UTR2(), PairAlignData.AlignNT);
				
				sumObj.saveAlignToDB(curPair.pairID, cdsAlnObj, utr5AlnObj, utr3AlnObj);
				
				scoreObj.diffScoreAll(curPair.seqName, cdsAlnObj, utr5AlnObj, utr3AlnObj);	
				
				scoreObj.saveStatsToDB(mDB, curPair.seqIndex[0], curPair.seqIndex[1]);
				
				scoreObj.sumScore(); // clears current scores after summing
				
				if (isKaKs) kaksWrite(); 
				
				curPair.done(); p=null;
				cdsAlnObj.clear(); utr5AlnObj.clear(); utr3AlnObj.clear();
				
				cntAligned++;
				if (cntAligned%100==0) {
					 Out.r("aligned " + cntAligned);
					 Thread.sleep(1000);
				}
			}
			if (outKsKsCmd!=null) outKsKsCmd.close();  
			if (outKaKs!=null)    outKaKs.close();
			
			if (cntWrite>0)  {
				Out.PrtSpCntMsg(2, cntWrite,  "KaKs alignments written to files");
				Out.PrtSpMsg(2, "From the projects KaKs directory, execute 'sh " + Globals.KaKsCmd + "' ");
				Out.PrtSpMsg(3, "It uses path: " + kaksEx);
				if (cntBadAlign>0 || cntShortCDS>0) {
					Out.PrtSpMsg(3, "Not written to KaKs files: ");
					Out.PrtSpCntMsgNz(3, cntBadAlign, "Could not be aligned ");
					Out.PrtSpCntMsgNz(3, cntShortCDS, "CDS Alignment less than " + MIN_ALIGN);
				}
			}		
			Out.PrtSpMsgTimeMem(0, "Complete computing " + pairSet.size() + " pairs", startTime);
			mDB.close(); 
			return cntAlreadyAligned;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "computing pairs and writing...."); 
			return -1;}
	}
	public ScoreCDS getScoreObj() { return scoreObj;}
	
	
	/***************************************************************
	 * Writes for file.
	 * Called from Pairwise.saveStatsAndKaKsWrite
	 */
	private void kaksWrite() {
		try {
			if (!curPair.goodCDS || !curPair.goodKaKs) return;
			
			kaksNoGap();
			
			if (!curPair.goodCDS || !curPair.goodKaKs) return; // CAS305 
			
			incKaKs++; 
			if (outKaKs==null || incKaKs==NRECORDs) {
				incKaKs=0;
				
				String in = Globals.KaKsInPrefix + fileCnt + Globals.KaKsInSuffix;
				String ot = Globals.KaKsOutPrefix + fileCnt + Globals.KaKsOutSuffix;
				outKsKsCmd.write(kaksEx + " -i " + ot + " -o " + in + " -m YN &\n");
				outKsKsCmd.flush();
				fileCnt++;
				
				if (outKaKs!=null) outKaKs.close();
				String file = dirKaKs + "/" + ot;
				
				outKaKs = new BufferedWriter(new FileWriter(file)) ;
			}
			outKaKs.write(curPair.seqName[0] +  ":" + curPair.seqName[1] + "\n");
			outKaKs.write(curPair.alignCDSnoGap[0] + "\n");
			outKaKs.write(curPair.alignCDSnoGap[1] + "\n\n");
			outKaKs.flush();
			cntWrite++;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error writing KaKs files");}
	}
	/*******************************************************************
	 * create gapFree for KaKs: go through 3 at time, append codons if no gap in either codon
	 * so as to preserve the alignment
	 */
	private void kaksNoGap() { 
		int seqLen = curPair.alignCDS[0].length();
	    StringBuffer sb1 = new StringBuffer (seqLen);
	    StringBuffer sb2 = new StringBuffer (seqLen);
	    
		for (int i=0; i<seqLen; i+=3) {
			String codon1="", codon2="";
			if (i+3>seqLen) break;
			codon1 = curPair.alignCDS[0].substring(i, i+3);
			codon2 = curPair.alignCDS[1].substring(i, i+3);
			if (!codon1.contains(Share.gapStr) && !codon2.contains(Share.gapStr)) {
				sb1.append(codon1);
				sb2.append(codon2);
			}
		}
		curPair.alignCDSnoGap[0] = sb1.toString();
		curPair.alignCDSnoGap[1] = sb2.toString();
		
		if (curPair.alignCDSnoGap[0].length() < MIN_ALIGN) {
			cntShortCDS++;
			curPair.goodKaKs=curPair.goodCDS=false;
		}			
	}
	/*********************************************************
	 * Only load pairs that have not already been aligned
	 * The sequence gets loaded in PairAlignData.
	 */
	private void loadPairsFromDB() {
	try {
		ResultSet rs;
		
		HashSet <Integer> ntDB = new HashSet <Integer> ();
		rs = mDB.executeQuery("Select ASMid from assembly where isPep=0");
		while (rs.next()) ntDB.add(rs.getInt(1));
		
		rs = mDB.executeQuery(
			"Select UTid1, UTid2, UTstr1, UTstr2 , ASMid1, ASMid2, PAIRid, nAlign, kaks," +
			"cdsLen1, cdsLen2 from pairwise where hasGrp=1"); 
	
		while (rs.next()) {
			int asm1 = rs.getInt(5);
			int asm2 = rs.getInt(6);
			if (!ntDB.contains(asm1) || !ntDB.contains(asm2)) continue;
			
			boolean bAligned = (rs.getInt(8)>0);
			if (bAligned) cntAlreadyAligned++;
			
			boolean bKaKs = (rs.getInt(9)>=0);
			
			if (bAligned && bKaKs)   continue; // has all info
			if (bAligned && !isKaKs) continue; // not writing KaKs
			
			cntDoAlg++;
			Pair grp = new Pair();
			grp.seqIndex[0] = rs.getInt(1);
			grp.seqIndex[1] = rs.getInt(2);
			grp.seqName[0] = rs.getString(3);
			grp.seqName[1] = rs.getString(4);
			grp.asmID[0] = asm1;
			grp.asmID[1] = asm2;
			grp.pairID = rs.getInt(7);
			grp.cdsLen[0] = rs.getInt(10);
			grp.cdsLen[1] = rs.getInt(11);
			pairSet.add(grp);
		}
		rs.close();
		Out.PrtSpCntMsgNz(1, cntAlreadyAligned, "Previously aligned");
		Out.PrtSpCntMsg(1, pairSet.size(), "Pairs to align");
	}
	catch(Exception e) {ErrorReport.reportError(e, "Error loadBBHpairs"); }
	}
	
	/*************************************************************
	 * Flags: run stats, run KaKs.
	 */
	private boolean initKaKs() {
		try {
			if (isKaKs) {
				dirKaKs = theCompilePanel.getCurProjRelDir() + Globals.KaKsDIR;
				File temp = new File(dirKaKs);
				if (!temp.exists()) {
					temp.mkdir();
				}
				int nCPU = theCompilePanel.getBlastPanel().getCPUs();
				NRECORDs = (int) (((float)cntDoAlg/(float)nCPU)+5.0);
				Out.PrtSpCntMsg(2, NRECORDs, "Aligned pairs will be written per KaKs file " +
						"(" + Globals.KaKsOutPrefix + "n" + Globals.KaKsOutSuffix + ")");
				outKsKsCmd = new BufferedWriter(new FileWriter(dirKaKs + "/" + Globals.KaKsCmd)) ;
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e,  "Opening files for stats"); return false;}
	}
	
	/*********************************************************************/
	private class Pair {
		int [] seqIndex = new int [2];
		String [] seqName = new String [2];
		String [] alignCDS = new String [2]; 		// full Align CDS 
		String [] alignCDSnoGap = new String [2];	// full Align CDS without gaps for KaKs
		int [] cdsLen = new int [2];
		
		int [] asmID = new int [2];
		
		boolean goodCDS=true, goodKaKs=true;
		int pairID=0;
		
		private void done() {
			seqIndex=null;
			seqName=null;
			alignCDS=null;  
			alignCDSnoGap=null;		
			
			asmID=null;
		}
	}
	
	private int cntBadAlign=0, cntShortCDS=0,  cntAlreadyAligned=0, cntAligned=0, cntWrite=0;
	
	private BufferedWriter outKsKsCmd=null, outKaKs=null;
	private int NRECORDs, incKaKs=NRECORDs, fileCnt=1;
	private String dirKaKs="";
	
	// list
	private int cntDoAlg=0;
	private Vector <Pair> pairSet = new Vector <Pair>();
	
	// for current
	private Pair curPair;
	private ScoreCDS scoreObj;
	
	private DBConn mDB = null;
	private boolean isKaKs=false;
	private CompilePanel theCompilePanel;
}
