package sng.annotator;
/**
 * Runs the annotation pipeline
 * Contains Pairs code
 */

import java.util.Collections;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Vector;
import java.util.ArrayList;

import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import sng.viewer.panels.align.AlignCompute;
import sng.viewer.panels.align.AlignData;
import util.align.AlignPairOrig;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class CoreAnno {
	final private int MIN_ALIGN_LEN = 20;
	final private int RECORDS_PER_SAVE = 5; 
		
	private boolean bFirstAnno = false;
	private boolean isAAstcw = false;
	
	CoreAnno () {}
	
	public void setMainObj(DoUniProt db, DoBlast b, CoreDB s, DBConn m) {
		uniObj = db;
		doBlastObj = b;
		sqlObj = s;
		mDB = m;
	}
	
	public boolean run(String path, DoORF orf) {
		try {		
			DoORF orfObj=orf;
			bFirstAnno=sqlObj.isFirstAnno();	
			isAAstcw=sqlObj.isAAtcw();
			boolean bReCalcORF = runSTCWMain.doRecalcORF;
			
			/*****************************************************************
			   Step 1: Annotate contigs    
			*******************************************************************/
			Out.PrtDateMsg("Start annotating sequences");
			long time = Out.getTime();
			if (!bReCalcORF && doBlastObj.numDB() > 0) 
			{
				TreeMap <String, Integer> ctgMap = sqlObj.loadContigMap(); // ctgName, ctgID
				Out.PrtSpMsg(1, ctgMap.size() + " Sequences loaded ");
				boolean b = uniObj.processAllDBhitFiles(isAAstcw, ctgMap);
				if (!b) { // terminate if errors 
					Out.PrtMsgTime("Pre-mature ending due to errors", time);
					return false;
				}
				ctgMap.clear();
			}
			if (!isAAstcw && (bReCalcORF || doBlastObj.numDB()>0)) {
				orfObj.calcORF(path, mDB); 
			}
			System.err.print("                                                                         \r");
			Out.PrtMsgTime("\nFinished annotating sequences", time);
			
			/*******************************************************************
			   Step 3. Run homology tests on contigs  (calls getORFcoding)
			********************************************************************/
			
			if (!doHomologyTests()) return false;
			
			return true;
		} catch (Throwable e) {
			ErrorReport.reportError(e, "Core Annotator");
			return false;
		}
	}
	
	/***************** HOMOLOGY TESTS *********************************/
	/**
	 * The UniProt blast file has been read, loaded and committed
	 * 1. uniProtPairs Get all UniProt hits from database that occur in two contigs
	 * 2. selfBlastPairs Read selfblast file for all megablast hits
	 * 3. tselfBlastPairs Read tselfblast file for all tblastx hits
	 * 4. pairsSpMx contains all pairs
	 * 5. Get all current pair_wise data and remove from pairsSpMx so not redone
	 * 7. DP all pairs within pairs_limit 
	 */
	private boolean doHomologyTests() throws Exception {						
		if (maxDPpairs == 0 || !doBlastObj.doPairs()) return true; // no comparisons to be performed
		
		String delim = ";";
		
		Out.Print("");
		Out.PrtTimeMsg("Start creating Pairs");
		long time = Out.getTime();
		
		// Only false if blast fails. Otherwise, (1) Not selected (2) blast succeed (3) tab file exists
		if (!doBlastObj.runSelfBlast("blastn")) return false;		
		if (!doBlastObj.runSelfBlast("tblastx"))return false;
		if (!doBlastObj.runSelfBlast("blastp")) return false;
		
		Out.PrtSpMsg(1, "Find pairs to align");
		// used for updating existing pairs where key is ctg1;ctg2
		HashMap <String, BlastHitData> pairsHash = 	new HashMap <String, BlastHitData> ();
		// pairs copied for sorting 
		ArrayList <BlastHitData> pairsList = new ArrayList <BlastHitData>(); 
	
		/******************************************************************
		 * Load pairs - the Hit values of the first occurrence is saved to DB
		 *********************************************************************/
		int cntn=0, cntx=0, cntp=0;
		
		// blastn: Load pairs from self-blast nucleotide hit file
		String fileSelfBlast = doBlastObj.getSelfBlastnFile();		
		if (fileSelfBlast != null && !fileSelfBlast.equals("-")) { // CAS330 moved first to be consistent in order
			cntn  = doBlastObj.addAllPairsFromBlastFile(delim, pairsHash, fileSelfBlast, Globalx.typeNT);
			
			if (cntp==0 && cntx==0) Out.PrtSpCntMsg(1, cntn,"Pairs from blastn                ");
			else                    Out.PrtSpCntMsg(1, cntn,"Additional pairs from blastn ");
		}
				
		// tblastx: Load pairs from translated self-blast file
		String fileTransSelfBlast = doBlastObj.getSelfTblastxFile();
		if (fileTransSelfBlast != null && !fileTransSelfBlast.equals("-")) {
			cntx =  doBlastObj.addAllPairsFromBlastFile(delim, pairsHash, fileTransSelfBlast, Globalx.typeAA);
			
			Out.PrtSpCntMsg(1, cntx,"Pairs from tblastx                      ");
		}
				
		// blastp: Load pairs from translated ORF file or AAsTCW
		String fileSelfBlastp = doBlastObj.getSelfBlastpFile(); // null if not selected
		if (fileSelfBlastp != null && !fileSelfBlastp.equals("-")) {
			cntp =  doBlastObj.addAllPairsFromBlastFile(delim, pairsHash, fileSelfBlastp, Globalx.typeORF);
			
			if (cntx==0) Out.PrtSpCntMsg(1, cntp, "Pairs from blastp             ");
			else         Out.PrtSpCntMsg(1, cntp, "Additional pairs from blastp ");
		}
	    
		if (cntn==0 && cntx==0 && cntp==0) {
			Out.PrtSpMsg(0, "No pairs to align");
			return true;
		}
		/**********************************************
		 * Remove existing pairs and start count
		 **********************************************/
		if (!bFirstAnno) updatePairs(delim, pairsHash);
	
		/************************************************************   
		 *  Make final list
		 *************************************************************/
		
	    for (String key : pairsHash.keySet()) {
	    	BlastHitData hit = pairsHash.get( key );
	    	pairsList.add(hit);
        }	
		Collections.sort(pairsList); // CAS314 sort was changed from e-val to bitscore
		pairsHash.clear();
		
		/************************************************************   
		 *  XXX  Align all of the sequences
		 *  If both AA and NT hits, only the AA scores are saved
		 *************************************************************/
		AlignPairOrig dpAlgo = new AlignPairOrig();
		Vector <AlignData> alignmentList = new Vector<AlignData>();
		HashMap <String, Integer> cntPairsHash = new HashMap <String, Integer> ();
		
		int nPairs = pairsList.size();
		int failToAlign=0, tooShort=0, successToAlign=0, nCompareCount = 0;
		
		BlastHitData pairHitObj = null;
		long alignTime = Out.getTime(); // CAS330
		
		if (maxDPpairs < nPairs) 
			Out.PrtSpMsg(1, "Aligning best " + maxDPpairs + " out of " + nPairs + " pairs, due to Pairs limit in Options");
		else 
			Out.PrtSpMsg(1, "Aligning " + nPairs + " pairs");
		
		AlignCompute.setCnt6();
		int noCtg=0, nDiffFr=0;
		for (int i = 0; i < nPairs && successToAlign < maxDPpairs; ++i) {		

			pairHitObj = pairsList.get(i);
			if (pairHitObj.getAlignLen() < MIN_ALIGN_LEN) { // Hit align length; being applied to NT and AA
				tooShort++;
				continue; // heuristic
			}
			
			String ctgID1 = pairHitObj.getContigID();
			String ctgID2 = pairHitObj.getHitID();
			
			// a sequence may be loaded many times... possibly save in hashMap
			ContigData contig1 = CoreDB.loadContigData(mDB, ctgID1);
			if (contig1==null) { 
				if (noCtg==0) Out.PrtWarn("No sequence '" + ctgID1 + "' in database; no further such error messages will be printed");
				noCtg++;
				continue;
			}
			ContigData contig2 = CoreDB.loadContigData(mDB, ctgID2);
			if (contig2==null) {
				if (noCtg==0) Out.PrtWarn("No sequence '" + ctgID2 + "' in database; no further such error messages will be printed");
				noCtg++;
				continue;
			}
			
			++nCompareCount;	
			// XXX Do the Alignment		
			AlignData theAlign = AlignCompute.pairAlignCore(
						dpAlgo, 
						contig1, contig2, 
						pairHitObj, 
						isAAstcw);	
			
			if (theAlign  == null) { // fails on out of memory
				Out.PrtErr("Cannot align " + 
						contig1.getContigID() + " (" + contig1.getSeqData().getLength() + "bp) " +
						contig2.getContigID() + " (" + contig2.getSeqData().getLength() + "bp)"  );
				failToAlign++;
				continue;
			}
			theAlign.releaseSequences();
			
			successToAlign++;
			
			if (contig1.getFrame()!=theAlign.getFrame1() || contig2.getFrame()!=theAlign.getFrame2()) {
				nDiffFr++;
				String msg="DP (" + contig1.getFrame() + "," + contig2.getFrame() + ")->(" +
							       theAlign.getFrame1() + "," + theAlign.getFrame2() + ")";
				theAlign.getHitData().setPairHitType(msg);
			}
			sqlObj.loadSharedHit(pairHitObj);
			alignmentList.add(theAlign);
			
			if (alignmentList.size() >= RECORDS_PER_SAVE) {				
				sqlObj.savePairAlignments(alignmentList, isAAstcw);
				alignmentList.removeAllElements();
			}

			updateHash(contig1.getContigID(), cntPairsHash);
			updateHash(contig2.getContigID(), cntPairsHash);
			
			String type=pairHitObj.getPairHitType();
			if (type.contains(Globalx.typeAA))  cntAA++;
			if (type.contains(Globalx.typeNT))  cntNT++;
			if (type.contains(Globalx.typeORF)) cntORF++;
			
			if (nCompareCount%10==0)
				Out.r("Dynamic programming pairs " + nCompareCount);
		}
		sqlObj.savePairAlignments(alignmentList, isAAstcw);
		
		String msg = "AA " + cntAA + "::ORF " + cntORF + "::NT " + cntNT ;
		sqlObj.savePairMsg(msg);
		sqlObj.saveAllCtgPairwiseCnts(cntPairsHash);
		System.err.print("                                                                                    \r");
		
		Out.PrtSpCntMsgZero(1, nDiffFr, "Aligned Frame is not the same as ORF frame");
		Out.PrtSpCntMsgNz(1, noCtg, "Sequences in hit file but not in database ");
		Out.PrtSpCntMsgNz(1, failToAlign, "Failed alignments");
		Out.PrtSpCntMsgNz(1,tooShort, "Rejected alignments (<" + MIN_ALIGN_LEN + "bp)");
		Out.PrtSpCntMsgNz(1, AlignCompute.getCnt6(), "Checked 6 frames");
		Out.PrtSpMsgTime(1, "Finished " + successToAlign + " alignments", alignTime);
		Out.PrtMsgTime("Finished pairwise comparison", time);
		return true;
	}
	/*********************************************************************
	 * Get Pairs and hit_type from DB; remove from pairsHash; update types and count
	 */
	private boolean updatePairs(String delim, HashMap<String, BlastHitData> fPairsHash) {
	try {
		 int cntRm=0;
			
		HashMap <String, String> dbPairsType = new HashMap <String, String> ();
		int cnt = sqlObj.loadPairsFromDB(delim, dbPairsType);
		if (cnt==0) {
			Out.PrtSpCntMsg(1, fPairsHash.size(),"Total unique pairs ");
			return true;
		}
		HashMap <String, String> xPairsType = new HashMap <String, String> ();
		Out.PrtSpCntMsg2(1, cnt, "Existing pairs ", fPairsHash.size(), "Hit file pairs");
		
		for (String key : dbPairsType.keySet()) {
			String dbType=dbPairsType.get(key);
			
			BlastHitData fBlastObj=null;
			String fKey = key, fType=null;
			
			// does the file have the pair?
			if (fPairsHash.containsKey(fKey)) {
				fBlastObj = fPairsHash.get(fKey);
			}
			else {
				String [] tok = key.split(delim);
				fKey = tok[1] + delim + tok[0];
				if (fPairsHash.containsKey(fKey)) 
					fBlastObj = fPairsHash.get(fKey);
			}
			if (fBlastObj!=null) { // the file has the pair
				fType =  fBlastObj.getPairHitType();
				fPairsHash.remove(fKey);
				cntRm++;
			}
			// Merge types (one of 3 blasts was not done on earlier run)
			String mType = dbType, aa=Globalx.typeAA, nt=Globalx.typeNT, orf=Globalx.typeORF;
			if (fType!=null && !fType.equals(dbType)) {
				if (fType.contains(aa)  && !mType.contains(aa)) 	mType += "::" + aa;
				if (fType.contains(orf) && !mType.contains(orf)) 	mType += "::" + orf;
				if (fType.contains(nt)  && !mType.contains(nt)) 	mType += "::" + nt;
			}
			if (!mType.equals(dbType)) xPairsType.put(key, mType);
			
			if (mType.contains(aa))  cntAA++;
			if (mType.contains(nt))  cntNT++;
			if (mType.contains(orf)) cntORF++;
		}
		if (xPairsType.size()>0) {
			Out.PrtSpCntMsg(1, xPairsType.size(), "Update pair type for existing pairs");
			sqlObj.savePairsType(delim, xPairsType);
		}
		
		if (cntRm>0) Out.PrtSpCntMsg(1, fPairsHash.size(),"Total unique pairs after removing " + cntRm + " existing pairs from set");
		else         Out.PrtSpCntMsg(1, fPairsHash.size(),"Total unique pairs ");
		return true;
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Update Pairs"); return false;}
	}
	
	private void updateHash(String ctgName, HashMap <String, Integer> map) {
		if (map.containsKey(ctgName)) {
			int cnt = map.get(ctgName) + 1;		
			map.put(ctgName, cnt);
		}
		else map.put(ctgName, 1);
	}
	
	/*************************************************************/
	public void setMaxDPpairs(int i) {maxDPpairs = i;}		
	
	// for doHomologyTests
	private int cntAA=0, cntNT=0, cntORF=0;
	
	
	private int maxDPpairs = 0;
	private CoreDB sqlObj = null;
	private DoBlast doBlastObj = null;
	private DoUniProt uniObj = null;
	private DBConn mDB = null;
}
