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
import util.align.AlignCompute;
import util.align.AlignPairOrig;
import util.align.AlignData;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;

public class CoreAnno {
	final private int MIN_ALIGN_LEN = 20;
	final private int RECORDS_PER_SAVE = 5; 
		
	private boolean bFirstAnno = false;
	private boolean bIsAAstcwDB = false;
	
	CoreAnno () {}
	
	public void setMainObj(DoUniProt db, DoBlast b, CoreDB s, DBConn m) {
		uniObj = db;
		blastObj = b;
		sqlObj = s;
		mDB = m;
	}
	
	public boolean run(String path, DoORF orf) {
		try {		
			DoORF orfObj=orf;
			bFirstAnno=sqlObj.isFirstAnno();	
			bIsAAstcwDB=sqlObj.isAAtcw();
			boolean bReCalcORF = runSTCWMain.doRecalcORF;
			
			/*****************************************************************
			   Step 1: Annotate contigs    
			*******************************************************************/
			Out.PrtDateMsg("Start annotating sequences");
			long time = Out.getTime();
			if (!bReCalcORF && blastObj.numDB() > 0) 
			{
				TreeMap <String, Integer> ctgMap = sqlObj.loadContigMap(); // ctgName, ctgID
				Out.PrtSpMsg(1, ctgMap.size() + " Sequences loaded ");
				boolean b = uniObj.processAllDBhitFiles(bIsAAstcwDB, ctgMap);
				if (!b) { // terminate if errors 
					if (!setAnnotationDate()) return false;
					Out.PrtMsgTime("Pre-mature ending due to errors", time);
					return false;
				}
				ctgMap.clear();
			}
			if (!bIsAAstcwDB && (bReCalcORF || blastObj.numDB()>0)) {
				orfObj.calcORF(path, mDB); 
			}
			System.err.print("                                                                         \r");
			Out.PrtMsgTime("\nFinished annotating sequences", time);
			
			/*******************************************************************
			   Step 3. Run homology tests on contigs  (calls getORFcoding)
			********************************************************************/
			
			if (!doHomologyTests()) return false;
			if (!setAnnotationDate()) return false;
			
			return true;
		} catch (Throwable e) {
			ErrorReport.reportError(e, "Core Annotator");
			return false;
		}
	}

	private boolean setAnnotationDate() {
		try {
			sqlObj.setAnnotationDate();
		} catch (Exception e) {
			ErrorReport.reportError(e, "Failed to set annotation date.");
			return false;
		}
		return true;
	}
	
	/***************** HOMOLOGY TESTS *********************************/
	/**
	 * The UniProt blast file has been read, loaded and committed
	 * 1. uniProtPairs Get all UniProt hits from database that occur in two contigs
	 * 2. selfBlastPairs Read selfblast file for all megablast hits
	 * 3. tselfBlastPairs Read tselfblast file for all tblastx hits
	 * 4. pairsSpMx contains all pairs
	 * 5. Get all current pair_wise data and remove from pairsSpMx so not redone
	 * 7. DP all pairs within JPAVE_pairs_limit 
	 */
	private boolean doHomologyTests() throws Exception {
								
		if (maxDPpairs == 0 || !blastObj.doPairs()) {
			//Out.PrtSpMsg(0, "No sequence comparisons to be performed\n");
			return true;
		}
		if (bIsAAstcwDB) {
			//Out.PrtSpMsg(0, "This option is disabled for protein databases\n");
			return true;
		}
		Out.Print("");
		Out.PrtDateMsg("Start sequence comparisons");
		long time = Out.getTime();
		
		if (!blastObj.runSelfBlast("blastn")) return false;		
		if (!blastObj.runSelfBlast("tblastx")) return false;
		
		Out.PrtSpMsg(1, "Find pairs to align");
		// used for updating existing pairs where key is ctg1;ctg2
		HashMap <String, BlastHitData> pairsHash = 	new HashMap <String, BlastHitData> ();
		// pairs copied for sorting 
		ArrayList <BlastHitData> pairsList = new ArrayList <BlastHitData>(); 
		int cnt1, cnt2;
	
		/******************************************************************
		 * Load pairs from UniTrans selfblast and UniTrans tselfblast in  matrix
		 *********************************************************************/

		// tblastx: Load pairs from translated self-blast file
		cnt1=cnt2=0;
		String fileTransSelfBlast = blastObj.getTSelfBlastFile();
		if (fileTransSelfBlast != null && !fileTransSelfBlast.equals("-")) {
			cnt2 =  blastObj.getAllPairsFromBlastFile(pairsHash,
						fileTransSelfBlast, false /* !isSelf */);
			prtline("Pairs from tblastx self-blast ", cnt2);
		}
		
		// megablast: Load pairs from self-blast file
		String fileSelfBlast = blastObj.getSelfBlastFile();		
		if (fileSelfBlast != null && !fileSelfBlast.equals("-")) {
			cnt1  = blastObj.getAllPairsFromBlastFile(pairsHash, 
					fileSelfBlast, true /* isSelf */);
			if (cnt2==0) prtline("Pairs from self-blast ", cnt1);
			else prtline("Additional pairs from self-blast ", cnt1);
		}
	    
		if (cnt1==0 && cnt2==0) {
			Out.PrtSpMsg(0, "No pairs to align");
			return true;
		}
		if (!bFirstAnno) {
			cnt1 = sqlObj.removePairsInDB (pairsHash);
			if (cnt1>0) prtline("Total unique pairs after removing " + cnt1 + " existing pairs", pairsHash.size());
			else        prtline("Total unique pairs ", pairsHash.size());
		}
		else prtline("Total unique pairs ", pairsHash.size());
	
		/************************************************************   
		 *  Make final list
		 *************************************************************/
		
	    for ( String key : pairsHash.keySet() )
        {
	    		BlastHitData hit = pairsHash.get( key );
	    		pairsList.add(hit);
        }	
		Collections.sort(pairsList);
		pairsHash.clear();
		
		/************************************************************   
		 *  XXX  Align all of the sequences
		 *************************************************************/
		AlignPairOrig dpAlgo = new AlignPairOrig();
		Vector <AlignData> alignmentList = new Vector<AlignData>();
		HashMap <String, Integer> cntPairsCtgHash = new HashMap <String, Integer> ();
		
		int nPairs = pairsList.size();
		int failToAlign=0, tooShort=0;
		int successToAlign=0;
		int nCompareCount = 0;
		BlastHitData blastObj = null;
		
		if (maxDPpairs < nPairs) {
			Out.PrtSpMsg(1, "Aligning best " + maxDPpairs + " out of " + nPairs 
					+ " pairs, due to Pairs limit in Options");
		}
		else 
			Out.PrtSpMsg(1, "Aligning " + nPairs + " pairs");
		
		int noCtg=0;
		for (int i = 0; i < nPairs && successToAlign < maxDPpairs; ++i) {		

			blastObj = pairsList.get(i);
			if (blastObj.getAlignLen() < MIN_ALIGN_LEN) {
				tooShort++;
				continue; // heuristic
			}
			
			String ctgID1 = blastObj.getContigID();
			String ctgID2 = blastObj.getHitID();
			
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
						dpAlgo, contig1, contig2, blastObj,
						AlignCompute.bestResult);	
			
			if (theAlign  == null) { // fails on out of memory
				System.err.println("Cannot align " + 
						contig1.getContigID() + " (" + contig1.getSeqData().getLength() + "bp) " +
						contig2.getContigID() + " (" + contig2.getSeqData().getLength() + "bp)"  );
				failToAlign++;
				continue;
			}
			theAlign.releaseSequences();
			
			successToAlign++;
			sqlObj.loadSharedHit(blastObj);
			alignmentList.add(theAlign);
			
			if (alignmentList.size() >= RECORDS_PER_SAVE) {				
				sqlObj.savePairAlignments(alignmentList);
				alignmentList.removeAllElements();
			}

			updateHash(contig1.getContigID(), cntPairsCtgHash);
			updateHash(contig2.getContigID(), cntPairsCtgHash);
			
			Out.r("Compared pairs " + nCompareCount);
		}

		sqlObj.savePairAlignments(alignmentList);
		sqlObj.saveAllCtgPairwiseCnts(cntPairsCtgHash);
		System.err.print("                                                                                    \r");
		
		if (noCtg>0) prtline("Sequences in hit file but not in database ", noCtg);
		if (failToAlign > 0) prtline("Failed alignments", failToAlign);
		if (tooShort > 0)    prtline("Rejected alignments (<" + MIN_ALIGN_LEN + "bp)", tooShort);
		Out.PrtMsgTime("Finished " + successToAlign + " sequence comparisons", time);
		return true;
	}

	private void updateHash(String ctgName, HashMap <String, Integer> map) {
		if (map.containsKey(ctgName)) {
			int cnt = map.get(ctgName) + 1;		
			map.put(ctgName, cnt);
		}
		else map.put(ctgName, 1);
	}
	
	private void prtline(String msg, int cnt) {
		String t = String.format("%6d ", cnt);
		Out.PrtSpMsg(1, t + msg);
	}

	/*************************************************************/
	public void setMaxDPpairs(int i) {maxDPpairs = i;}		
	
	private int maxDPpairs = 0;
	private CoreDB sqlObj = null;
	private DoBlast blastObj = null;
	private DoUniProt uniObj = null;
	private DBConn mDB = null;
}
