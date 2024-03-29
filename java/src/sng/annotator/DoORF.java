package sng.annotator;

/******************************************************
 * An ORF for every frame is computed and entered into database even if less than minimal length
 * as they are displayed in SeqFramePanel
 * 
 * CAS334 - (1) made IsUniqueSeq more efficient. Limited number of sequences to use for training.
 * 			(2) Add cutoff for Markov log ratio. Added Rule 4 on ends.
 * 			(3) Add Stop-Stop ORFs, but then remove ones that are too close to an ATG ORF. 
 * 			(4) If a hit region ends in a ATG-Stop, automatically make the selected ORF. Otherwise,
 * 				find all upstream starts paired with first downstream Stop, and use finishThisFrame.
 * 			(5) fromHitWithStops: uses fromAllPossibleForFrame, then marks overlapping ORFs with Hits.
 * 
 * NCBI ORF finder: includes Stop codon in nt ORF, but not in translated ORF, e.g. tra_011 is 282nt and 93aa
 * TransDecoder: leaves Stop on ends of nt ORF and translated ORF
 * TCW: leaves Stop on end of both
 * 
 * v3 TESTING CODON USAGE computation: 
 * 1. Made a file of the one sequence with a hit. Set ORF training for one hit.
 * 2. Made a TCW database from the file of sequences. 
 * 3. Entered HitRegion.txt into http://www.bioinformatics.org/sms2/codon_usage.html
 * 4. StatsCodon.txt was the same results.
 * 	
 * v4: TEST 5th ORDER MARKOV MODEL with TransDecoder-TransDecoder-v5.1.0/util perl scripts
 * Test1: First test set (sTCW_os1)
 *   - execAnno os1 -r -f
 *     - use longest_orfs.cds.top_500_longest as training set
 *     -f the difference from significant digits -  Java has two extra significant digits
 *     		outputs the base frequencies, which match TD base_freq.dat
 *     The Markov scores were the same within +/-0.01 
 * v3 only Test2:    
 *   - seq_n_baseprobs_to_loglikelihood_vals.pl HitRegion.fa base_freqs.dat >hex_scores 
 *     hex_scores same output TCW ScoreMarkov.txt when run with the -t option.
 *   - ./score_CDS_likelihood_all_6_frames.pl os500_ntORFs.fa hex_scores >orf_scores
 *     orf_scores the same scores as in BestFrames.txt with a small roundoff
 *  
 * TCW and TD automatically removes non-unique sequences
 *    Their algorithm is different for detecting them
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;
import java.util.HashMap;

import sng.database.Overview;
import sng.database.Globals;
import sng.database.Version;
import sng.dataholders.SequenceData;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Markov;
import util.methods.Out;
import util.methods.Static;

public class DoORF {
	private final String orfDir = Globalx.pORFDIR;
	private boolean debug = Globalx.debug; // command line (./execAnno project -r -d)
	
	// Interface parameter defaults, set in Globals (I put values here to remind me)
	private boolean pbAltStart=false;  	// Use alternative start sites	
	private double  pdHitEval=1e-10;   	// Automatically use this frame
	private int 	piHitSim=20;	   	// or if the alignment to the hit has >= this similarity
	private double  pdDiffLen=0.4;	   	// Use longest ORF if Log Len Ratio > n
	private String  pCdsFileName = "-1";// Used file of CDS sequences for Markov instead of hits
	private int     piMinSet=500;		// must have more than this many hits
	private double  pdDiffMk=0.3;  		// Use highest Markov if Log difference > n 
	
	// command line parameters with -r -sc N -hc N, where -r is only ORFs
	private int     piHitOlap=0;     		// %hit coverage
	private int 	piSeqOlap=0;	  		// %seq coverage
	private int 	pbTestType=0; 			// n= 0-none, 1=longest, 2=Markov
	private boolean	pbTransDecoder=false;   // -f Make Markov Scores = Transcoder (hidden)
	private int 	piTrainNseq = 2000;		// -t number to train from	(CAS334)
	private int 	piTrainNbases = 1000000;
	private boolean pbFindDups=true;	
	private String  filterStr="";
	
	// Internal parameters
	private final int START_ATG = 10; 		// > N codons from last ATG to end of sequence
	private final int FROM_ATG =  30; 		// > N codons from last ATG to use non-ATG
	private final int FROM_ATG_STOP = 120; 	// > N Codons from last ATG to non-ATG abutting STOP
	private final int EX_hitIn =   10;  	// < N codons internal hit ATG (CAS342 changed from 5)
	private final int SORT_LEN = 100;		// used in OrfData sort (Rule 2A)
	private final double SORT_MK = 10.0;	// used in OrfData sort (Rule 2B)
	
	private final int KMER_wordSize = 5, KMER_perSame=99;// findSimilarKMER
	private final int MULTI_hitOlap=40, MULTI_seqOlap=40, MULTI_sim=40; // multiframe hit - if fail all, don't use hit		
	
	// ORF additional rules
	private int minLenForSeq=0;
	private final int     ORF_MIN_LEN =  9; 	 // for very short sequences
	private final int	  ORF_SHORT_LEN = 30;    // nt; for all other sequences
	private final int 	  ORF_WRITE_LEN_MK = 90; // nt; Write to AllORFs if good Markov and len>=ORF_WRITE_LEN_MK
	private final int     ORF_WRITE_LEN = 900;   // nt; Write to AllORFs.fa if len>ORF_WRITE_LEN
	private final String  NUM_N_CODONS = "nnnnnnnnn";   // remarks if > than this many
	
	// Only used on log output for last column of Great hit
	private final int GT_hitOlap = 95, GT_hitSim =  60;	
		
	// flags
	private final int    NO_RF = 0;
	private final double CODON_STOP = -20.0;
	private final int 	 BADORFS = 10;         // has stop codon in frame
	
	private boolean      bUseTrain=true;       // have training data
	private boolean      bTrainFromFile=false; // false - compute; true - use  cds file.
	
	private String orfPath="";
	// written in PrtFile.writeFastaFiles and writeAllFramesForOrf
	private final String orfBestAAfname =  "bestORFs.pep.fa"; 
	private final String orfBestNTfname =  "bestORFs.cds.fa";
	private final String bestFrameFname =  "bestORFs.scores.txt";
	private final String orfAllAAfname =   "allGoodORFs.pep.fa"; 
	private final String orfFrameFname =   "allGoodORFs.scores.txt";
	
	// written in Train 
	private final String codonScoreFname = 	"scoreCodon.txt";
	private final String markovScoreFname = "scoreMarkov.txt";
	
	DoORF () {}
	
	// Set in interface
	public void setParams(boolean bAlt, double hitEval, int hitSim, double diffLen, double mkLen, int trSet, String cdsFile) {
		pbAltStart = bAlt;
		pdHitEval = hitEval;
		piHitSim = hitSim;
		
		pdDiffLen = diffLen; 
		pdDiffMk = mkLen;
		piMinSet = trSet;
		if (isFileName(cdsFile))   pCdsFileName = cdsFile;	
		if (debug) Out.prt(String.format("Diff len %.3f   Diff Mk %.3f", pdDiffLen, pdDiffMk));
	}
	// Run from command line only
	public void setCmdParam(int type, int trans, int filter, int seqCov, int hitCov) { 
		pbTestType=type;
		pbTransDecoder= (trans==1) ? true : false;
		if (filter>0) {
			if (filter!=piTrainNseq) filterStr= "(Train " + filter + ")";
			piTrainNseq = filter;
			pbFindDups = true;
		}
		else if (filter == 0) {
			pbFindDups=false;
			filterStr= "(No Non-Unique Removal)";
		}
		piSeqOlap=seqCov;
		piHitOlap=hitCov;
	}
	/*********************************************************************/
	public void calcORF (String path, DBConn d) {
		projPath = path;
		mDB = d;
		try { 
			long time = Out.getTime();
			
			Out.PrtSpTimeMsg(1,"Annotate with GC and ORF " + filterStr);
		
			if (!train.setMode()) return;
			
			initializations();
			
			loadSeqData();
			
			train.computeCodonMarkov(); // will return if no protein hits loaded in loadSeqData
			
			prtObj.parameters(); // CAS334 moved from beginning
			
			find.forAllSeq();
			
			saveSeqORFData();
			prtObj.statsORFprint();
			
           	Out.PrtSpMsgTime(1,  "Complete annotation with ORF and GC ", time);
		}
		catch (Exception err) {ErrorReport.reportError(err, "CalcORF");}
    }
	
	/*****************************************************************/
	private void loadSeqData ( ) {	
		Out.PrtSpMsg(2, "Load all sequence from database");
		ResultSet rs = null;  
		String [] type = {"BS", "AN"}; // could also use "WG"; CAS327 changed EV to BS
			
		try{	
	   /* Load seqData - Get all seqIDs along with their sequences and best hit */
	   		int cntSeq = mDB.executeCount("select count(*) from contig");
	   		seqData = new SeqData[cntSeq];
	
	   		int idx=0, cntHasAnno=0;
	   		rs = mDB.executeQuery("SELECT CTGID, contigid, consensus, notes, PID, PIDov, PIDgo "
	   				+ " FROM contig order by contigid"); // CAS334 order by contigid so files are ordered 
	   		while (rs.next()) {
	   			int pid = rs.getInt(5);
	   			if (pid>0) cntHasAnno++;
	   			seqData[idx++] = new SeqData(rs.getInt(1), rs.getString(2), 
	   					rs.getString(3), rs.getString(4), pid, rs.getInt(6), rs.getInt(7));
	   		}
	   		rs.close();
	   		Out.PrtSpCntMsg(3, idx, "Sequences to process");
	   		
	   		if (cntHasAnno==0) {
	   			Out.PrtSpMsg(3, "No hits in database");
	   			return; 
	   		}
			
			int hasPr = mDB.executeCount("select count(*) from pja_db_unitrans_hits " +
	   				" where isProtein=1 limit 1");
	   		if (hasPr==0) {
	   			Out.PrtSpMsg(2, "No protein hits in database");
	   			return;
	   		}
    		   	
    	   /** Get the best hit for each sequence **/
	   		String sql = "SELECT isProtein, e_value, ctg_start, ctg_end,  " +
	   			" prot_cov, ctg_cov, percent_id FROM pja_db_unitrans_hits ";
	   		int cntHit=0, cntNoHit=0,  cntAN=0, cntStop=0, cntNT=0, cntBadMulti=0;
	   		int cntGoodHit=0, cntGreatHit=0, cntScov=0, cntHcov=0;
	   		
	   		int [] pid = new int [type.length];
	   		
	   		// Loop through sequences
	   		for (idx=0; idx<cntSeq; idx++) {
	   			if (seqData[idx].pid==0) {
	   				cntNoHit++;
	   				continue;
	   			}
				cntHit++;
				
	   			// Checking all three to avoid one with stops or NT hit. Set to zero if same as others.
	   			pid[0] =  seqData[idx].pid;
	   			pid[1] =  seqData[idx].pidov;
	   			
	   			boolean found=false;
	   			
	   			String seqSql = sql + " WHERE CTGID = " + seqData[idx].seqID;
	   			
	   			// First loop is for EV; if it is NT, then loop again for AN
	   			for (int i=0; i<type.length && !found; i++) {
	   				if (pid[i]==0) continue;
	   				
	   				rs = mDB.executeQuery(seqSql + " and PID= " + pid[i]);  
	   				if (!rs.next()) {
	   					Out.PrtError("Could not read " + seqData[idx].name + " " + type[i] + "=" + pid[i]);
	   					continue;
	   				}
	   				boolean isProtein = rs.getBoolean(1);
	   				if (!isProtein) {
	   					cntNT++;
	   					continue;
	   				}	
	   				double eval = rs.getDouble(2);
	   				int start = rs.getInt(3);
    	 			int end =   rs.getInt(4);
    	 			int hitCov = rs.getInt(5); 
    	 			int seqCov = rs.getInt(6);
	   				int sim = rs.getInt(7);
	   				
	   				// Use the hit for the ORF frame and coordiantes
	   				boolean isGoodHit = (eval<= pdHitEval || sim >= piHitSim) ? true : false; 
					
					if (isGoodHit) { // set Olap>0 with execAnno -r 
						if (seqCov<piSeqOlap) { 
    						isGoodHit=false;
    						cntScov++;
    					}
						else if (hitCov<piHitOlap) {
    						isGoodHit=false;
    						cntHcov++;
    					}
					}
					
				// Stops in hit
    	 			if (start<0 || end> seqData[idx].seqLen || end < 0) { 
    	 				Out.bug("Bad coordinates: " + seqData[idx].name + " Length: " +  seqData[idx].seqLen + " Start: " + start + " End: " + end);
    	 				continue;
    	 			}
    	 			int orient = 1;
    				String orientedSeq;
    				
    				if (end <= start) { // RCOORDS: sequence will be reverse complemented before ORF finding
    					start = seqData[idx].seqLen - start + 1;
    					end =   seqData[idx].seqLen - end + 1;
    					orient = -1;
    					orientedSeq = seqData[idx].getSeqRev();
    				}
    				else orientedSeq = seqData[idx].seq;
    				
    				int frame = start % 3;
    				if (frame==0) frame = 3;
    				if (orient == -1) frame = -frame;
					
    				boolean hasStop=false;
    				int nStop=0;
					for (int j=start-1; j<end-3; j+=3) {
						String codon = orientedSeq.substring(j, j+3);
						if (isCodonStop(codon)) nStop++;
					}
					if (nStop>0) {
						seqData[idx].addRemark(Globals.RMK_HIT_hitSTOP + nStop); // CAS334 remove type 
						hasStop=true;
					}
							
    	    	 	// XXX Internal Heuristic for sorting for Training set
    	    	 	boolean isGreatHit =  (hitCov >= GT_hitOlap && sim >= GT_hitSim) ? true : false;
    	    	 		  	    	 		
					seqData[idx].addHit(eval, sim, hitCov, seqCov, start, end, frame, isGoodHit, isGreatHit, hasStop);
    				if (i==1) {
    					cntAN++;
    					seqData[idx].addRemark(Globals.RMK_ORF_ANNO);
    				}
    				found=true;
	    	 	} // End loop through EV, AN
	   			
	   			if (seqData[idx].isGoodHit)  cntGoodHit++; // just counting if Eval and Sim are good
	   			if (seqData[idx].isGreatHit) cntGreatHit++;
	   			if (seqData[idx].hasStops)   cntStop++; // this can still be good hit so give frame precedence, just don't use coords
	   			if (seqData[idx].remark.contains(Globals.RMK_MultiFrame) // it has to be a really bad hit
	   					&& seqData[idx].hitCov<MULTI_hitOlap 
	   					&& seqData[idx].seqCov<MULTI_seqOlap
	   					&& seqData[idx].sim < MULTI_sim) {
	   				seqData[idx].isGoodHit=false;
					cntBadMulti++;
				}
	   		} // end loop through sequenses
	   		if ( rs != null ) rs.close();
	   		
	   		Out.PrtSpCntMsg2(3, cntHit,    "With hits", cntNoHit,  "With no hit"); // CAS314
	   		Out.PrtSpCntMsgZero(3, cntNT,   "Ignored NT hit");
	   		Out.PrtSpCntMsgZero(4, cntAN,   "Used Best Anno (vs Bits) ");
	   		
	   		String x = String.format("Good hit  (%sSim>=%d || E-value>=%.0E)", "%",piHitSim,  pdHitEval);
	   		Out.PrtSpCntMsg(3, cntGoodHit,  x); 
	   		Out.PrtSpCntMsg(3, cntGreatHit, "Great hit (%Sim>=" + GT_hitSim + " && %Hit>=" + GT_hitOlap + ")");
 
    	   	Out.PrtSpCntMsgZero(5, cntScov, "Failed seq coverage " + piSeqOlap);
	   		Out.PrtSpCntMsgZero(5, cntHcov, "Failed hit coverage " + piHitOlap);
	   		Out.PrtSpCntMsgZero(3, cntStop, "Hits with stops (find longest non-stop hit region)");
	   		Out.PrtSpCntMsgZero(3, cntBadMulti, "Poor hit with multi-frames (ignore) ");
	   	 	Out.PrtSpMsg(2, "Complete load");
       }
       catch (Exception e) {
	   		String x = "This error occurs when the hitResults are not consistent with sequences.";
	   		Out.PrtWarn(x);
	   		ErrorReport.prtToErrFile(x);
	   		ErrorReport.die(e, "Loading Sequence Data");
	   	}
	 }
	
	/*****************************************************/
	 private void saveSeqORFData () 
     {  
		long startTime = Out.getTime();
		Out.PrtSpMsg(2,"Save all best ORFs to the database");
        try {  
    	    PreparedStatement ps = mDB.prepareStatement(
					"update contig SET gc_ratio=?, o_coding_start=?, o_coding_end=?, o_frame=?, " +
					"o_coding_has_begin=?, o_coding_has_end=?, p_eq_o_frame=?, o_len=?, o_markov=?," +
					"notes=?, cnt_ns=? where CTGID=?"); 
    		
    	    mDB.openTransaction();
    	    int cntLoop=0, cntSave=0;
    	 	for (int i=0; i<seqData.length; i++) {
    	 		ps.setDouble(1, seqData[i].gcRatio);
    	 		ps.setInt(11, seqData[i].cntNs);
    	 		ps.setInt(12, seqData[i].seqID);
    	 		
    	 		if (seqData[i].orfStart==0 && seqData[i].orfEnd==0) {
    	 			ps.setInt(2, 0);
    	 			ps.setInt(3, 0);
    	 			ps.setInt(4, 0);
    	 			ps.setInt(5, 0);
    	 			ps.setInt(6, 0);
    	 			ps.setInt(7, 0);
    	 			ps.setInt(8, 0);
    	 			ps.setDouble(9, 0.0);
    	 			ps.setString(10, seqData[i].remark);
    	 		}
    	 		else {
    	 			if (seqData[i].orfFrame <-3 || seqData[i].orfFrame>3 || seqData[i].orfStart<0 || seqData[i].orfEnd<0)
    	 				ErrorReport.die("Internal error: " + seqData[i].seqID + " f:" + 
    	 						seqData[i].orfFrame + " s:" + seqData[i].orfStart  + " e:" + seqData[i].orfEnd);
    	 			
    	 			int hb = (seqData[i].hasStart) ? 1 : 0;
      	            int he = (seqData[i].hasEnd) ? 1 : 0; 
      	            int anno = (seqData[i].orfHasHit) ? 1 : 0;
  	        		ps.setInt(2, seqData[i].orfStart);
    	 			ps.setInt(3, seqData[i].orfEnd);
    	 			ps.setInt(4, seqData[i].orfFrame);
    	 			ps.setInt(5, hb);
    	 			ps.setInt(6, he);
    	 			ps.setInt(7, anno);
    	 			ps.setInt(8, seqData[i].orfEnd-seqData[i].orfStart+1);
    	 			ps.setDouble(9, seqData[i].dMKscore);
    	 			ps.setString(10, seqData[i].remark);
    	 		}
    	 		ps.addBatch(); 
    	 		if (cntSave ==  1000) {
    	 			cntSave=0;
    	 			Out.r("Save " + cntLoop);
    	 			ps.executeBatch();
    	 		}
    	 		cntLoop++; cntSave++;
    	 	}
    	 	if (cntSave>0) ps.executeBatch();
    	 	ps.close();
    	    mDB.closeTransaction();
    	    
    	 	// saved for seqFramePanel display
    	 	Out.PrtSpMsg(3,"Save " + saveORFsForDB.size() + " all frame ORFs to the database");
    	    PreparedStatement ps2 = mDB.prepareStatement(
 					"insert tuple_orfs SET CTGid=?, value=?");
    	    mDB.openTransaction();
    		cntLoop=cntSave=0;
        		
     		for (String key : saveORFsForDB) {
     			String [] tok = key.split("X");
     			int seqID = 0;
     			try { 
     				seqID = Integer.parseInt(tok[0]);
     			} catch (Exception e){ErrorReport.die("Parse " + tok[0] + " from " + key);}
     			
     			ps2.setInt(1, seqID);
     			ps2.setString(2, tok[1]);
     			ps2.addBatch();
     			
     			if (cntSave == 1000) {
     				cntSave=0;
     				Out.r("Save " + cntLoop);
     				ps2.executeBatch(); 
     			}
    	 		cntLoop++; cntSave++;
     		}
     		if (cntSave>0) ps2.executeBatch();
     		ps2.close();
     		mDB.closeTransaction();
     		Out.PrtSpMsgTimeMem(2, "Finish saving ORF data", startTime);
        }
 		catch (SQLException e) {ErrorReport.die(e, "cannot save new annotations to database");}
        catch(Exception e){ErrorReport.die(e, "Saving ORFs to database");}
     }
	
	/**********************************************************
	 * All initialization for this class
	 */
	private boolean initializations()  {
		try {
			gcObj.init();
			train.init();
			prtObj.init();
			
			orfPath = projPath + "/" + orfDir;
			File path = new File(orfPath);
			if (!path.exists()) {
				if (!path.mkdir()) {
					System.err.println("*** Failed to create project ORF directory '" + 
							path.getAbsolutePath() + "'.");
					return false;
				}
			}
			mDB.executeUpdate("update contig set gc_ratio=0.0," +
				"o_frame=0, o_coding_start=0, o_coding_end=0, o_len=0,  " +
				"o_coding_has_begin=0, o_coding_has_end=0, o_markov=0, " +
				"p_eq_o_frame=0"); // p_frame (hit frame); notes is cleared when read except for Multi-frame
			
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Initialize for ORFs"); return false;}
	}
	
	private boolean isCodonStop(String codon) {
		if (codon.equals("taa")) return true;
		if (codon.equals("tag")) return true;
		if (codon.equals("tga")) return true;
		return false;
	}
	private boolean isCodonATG(String codon) {
		if (codon.equals("atg")) return true;
		if (pbAltStart) {
			if (codon.equals("ctg")) return true;
			if (codon.equals("ttg")) return true;
		}
		return false;
	}
	/*****************************************************
	 * Check all reading frames for the best
	 */
	private class FindORF {
		/** Loop through all contigs computing GC and ORF **/
		private void forAllSeq () 
	    {
	        try {  
	        	long startTime = Out.getTime();
	        	Out.PrtSpMsg(2, "Start ORF computation");
				if (!prtObj.openFiles()) return;
				int numSeq = seqData.length, cntLoop=0;
				
			/** start loop **/
				for (int nn=0; nn<numSeq; nn++) { 
	                curSeqObj = seqData[nn];
	                if (curSeqObj.seqLen==0) continue;
	                
	                minLenForSeq = (curSeqObj.seqLen<=ORF_SHORT_LEN) ? ORF_MIN_LEN : ORF_SHORT_LEN;
					
	        		OrfData bestOrf = forThisSeq(); 
	        		if (bestOrf==null) continue; // happens if there is an error 
	        			
			        curSeqObj.orfFrame = bestOrf.oFrame;
			        curSeqObj.orfStart = bestOrf.oStart;
			        curSeqObj.hasStart = bestOrf.hasATG;
			        curSeqObj.orfEnd =   bestOrf.oEnd;
			        curSeqObj.hasEnd =   bestOrf.hasStop;
			        curSeqObj.dMKscore = bestOrf.dMKscore;
			        curSeqObj.orfHasHit = (curSeqObj.hitFrame==NO_RF) ? false : 
        		 			                 (bestOrf.oFrame==curSeqObj.hitFrame); 
			
			        if (bestOrf.oFrame!=NO_RF && bestOrf.oEnd>0) { 
			         	curSeqObj.addRemark(bestOrf.remark);
			        	
			        	prtObj.statsORFcompute(bestOrf); // Add Globals.RMK_ORF_NOTLONG - keep this order
			        	prtObj.writeFilesForBestORF(bestOrf);
			        	prtObj.writeAllGoodFramesForORF(); // uses bestPerFrameORFs
				        	
				        	// Keep in sorted Order
						for (OrfData o : bestPerFrameORFs) {
							String s = (o.hasATG)  ?  Globals.ORF_MARK : Globals.ORF_NoMARK; 
			     			String e = (o.hasStop) ? 	Globals.ORF_MARK : Globals.ORF_NoMARK; 
			     			String y = (o.isGoodMarkov) ? 	Globals.ORF_MARK : Globals.ORF_NoMARK; 
			     		
			     			String val = String.format("%d:%d:%d:%.2f:%.2f:%s:%s:%s",
			     					o.oFrame,o.oStart,o.oEnd,o.dMKscore, o.dCDscore, s,e,y);
			     			
							saveORFsForDB.add(o.seqID + "X" + val); // after sort - to be written to database and file
						}
			        }     
			        gcObj.seqGC(); 
			        
	                if (cntLoop % 100 == 0) Out.r("Processed " + cntLoop);
	                cntLoop++;
	            }
				Out.PrtSpMsgTimeMem(2, "Complete ORF computation", startTime);
	        } 
	        catch (Exception err) {ErrorReport.reportError(err, "Annotator - process remaining");}
	    }
		/************************************************************/
		private OrfData forThisSeq() {
			try {
				bestPerFrameORFs.clear();
				
				for (int i = -3; i <= 3; ++i) {
					if (i == 0) continue;
					
					OrfData o = null;
					String seq = (i>0) ? curSeqObj.seq : curSeqObj.getSeqRev();
					
					if (i==curSeqObj.hitFrame && curSeqObj.isGoodHit) {
						if (curSeqObj.hasStops) o = fromHitWithStops(i, seq);
						else                    o = fromHit(i, seq);
					}
					if (o==null) o = fromAllPossibleForFrame(i, seq, false); 
					if (o==null) o = new OrfData(i, 0, 0, false, false, false);
					
					bestPerFrameORFs.add(o);
				}
			
				OrfData best=null;
				try {
					Collections.sort(bestPerFrameORFs);
					best = bestPerFrameORFs.get(0);
					
					if (debug) {
						Out.prt(curSeqObj.name);
						for (OrfData o : bestPerFrameORFs) Out.prt(o.prtLine());
					}
				}
				catch (Exception e) {best = getBest("Final", e);	}
				
				return best;
			}
			catch (Exception err) {ErrorReport.reportError(err, "Annotator - process remaining");}
			return null;
		}
		
		/***************************************************************
		 * CAS334 
		 * 	If start/stop on boundaries of hit - use
		 *  Else, find all ORFs covering hit, and take best
		 */
		private OrfData fromHit(int frame, String orientedSeq) {
			if (pbTestType>0) return null;
			if (curSeqObj.seqHitStart == -1) return null;
			
			try {
				thisFrameORFs.clear();
				int hStart = curSeqObj.seqHitStart;
				int hEnd =   curSeqObj.seqHitEnd;
				int seqLen = orientedSeq.length();
				
			/** 1. use start/stop on boundary of hit (if Hit goes through n's, ok) **/
				String atgStr = orientedSeq.substring(hStart-1, hStart+2);
				String stopStr = (hEnd+3<=seqLen) ? orientedSeq.substring(hEnd, hEnd+3) : "";
				if (isCodonATG(atgStr) && isCodonStop(stopStr)) {
					OrfData o = new OrfData (frame, hStart, hEnd+3, true, true, false);
					return  o; 
				}
				
			/** 2. Find all possible ORFs around hit region, then sort for best **/
			// find first downstream stop 
				boolean hasStop=false, hasATG=false;
				int nAtg= Math.abs(frame), nStop=seqLen; 
				String codon;
				
				for (int i=hEnd-3; i<=orientedSeq.length()-3; i+=3) {// only go right
					codon = orientedSeq.substring(i, i+3);
					if (isCodonStop(codon)) {
						nStop = i+3;
						hasStop=true;
						break;
					}
				}
				
				if (isCodonATG(atgStr)) { // CAS342 hit has ATG but not stop
					addORF(frame, hStart, nStop, true, hasStop, false);
					if (thisFrameORFs.size()==1) return thisFrameORFs.get(0);
				}
				
			// find ATG - ORFs from stop to upstream hit ends
				int lastStop=-1;
				for (int i=hStart-1; i>=0; i-=3) { // only go to left
					codon = orientedSeq.substring(i, i+3);
					if (isCodonATG(codon)) {
						nAtg = i+1;
						hasATG=true;
						addORF(frame, nAtg, nStop, true, hasStop, false); 
					}
					else if (isCodonStop(codon)) {
						lastStop = i+4;
						addORF(frame, lastStop, nStop, false, hasStop, true); 
						break;
					}
				}
			// No ATG - look downstream a wee bit
				if (!hasATG) {
					for (int i=hStart-1, j=0; i>=0 && j<EX_hitIn; i+=3, j++) {
						codon = orientedSeq.substring(i, i+3);
						if (isCodonATG(codon)) {
							nAtg = i+1;
							hasATG=true;
							addORF(frame, nAtg, nStop, true, hasStop, false);
							break;
						}
					}
				}
			// Finish
				if (!hasATG && !hasStop) {
					hasATG = isCodonATG(atgStr);
					if (isCodonStop(stopStr)) addORF(frame, hStart, hEnd+3, hasATG, true, false); 
					else					  addORF(frame, hStart, hEnd,   hasATG, false, false);
				}
				if (lastStop==-1) 
					addORF(frame, Math.abs(frame), nStop, false, hasStop, false);
			
				return finishThisFrame(frame);
			}
			catch (Exception e) {return getBest("For hit", e);	}	
		}
		/*************************************8
		 * just look between stops
		 * CAS334 changed to use allPossibleForFrame with much better results
		 */
		private OrfData fromHitWithStops(int frame, String seq) {
			try {
				fromAllPossibleForFrame(frame, seq, true); // true=hasStop so does no call finishThsFrame
				
				int hStart = curSeqObj.seqHitStart;
				int hEnd =   curSeqObj.seqHitEnd;
				for (OrfData oObj : thisFrameORFs) {
					if (Static.isOlap(oObj.oStart, oObj.oEnd, hStart, hEnd)) oObj.hasAnno=true;
				}
				return finishThisFrame(frame);
			}
			catch (Exception e) {return getBest("HitWithStop", e);	}
		}
		/*************************************************
		 * Find and compare all ORFs for frame
		 * This is used for hit frame with Stop, and all non-hit (or bad hit) frame.
		 */
		private OrfData fromAllPossibleForFrame(int frame, String seq, boolean hasStops) {
			try {
				final int fStart = 	Math.abs(frame);
				final int fEnd = 	seq.length();
				int nATG=-1, nStop=-1, firstATG=-1,  firstStop=-1, lastStop=-1;
				
				thisFrameORFs.clear();
			
				// Scan sequence finding possible internal ORFs
				for (int i=fStart-1; i<=fEnd-3; i+=3) {
					String codon = seq.substring(i, i+3);
					
					if (isCodonStop(codon)) {
						nStop = i+3;
						if (nATG != -1) 
							addORF(frame, nATG, nStop, true, true, false); 	   // atg to stop
						if (lastStop != -1 && (lastStop+1)!=nATG) 		   		// atg be right after stop
							addORF(frame, lastStop+1, nStop, false, true, true); // stop to stop
				
						if (firstStop == -1) firstStop=nStop;
						lastStop = nStop;
						nATG = -1;
					}	
					else if (isCodonATG(codon)) { 
						if (nATG == -1) {
							nATG=i+1;
							if (firstATG == -1) firstATG=nATG;
						}
					}
				}
				// end cases
				boolean hasATG = (firstATG!=-1), hasStop = (firstStop!=-1);
				
				if (hasStop) {
					if (fStart!=firstATG) 
						addORF(frame, fStart, firstStop, false, true, false); 	// start to 1st Stop 
					if ((lastStop+1)!=nATG)
						addORF(frame, lastStop+1, fEnd,  false, false, true); // last Stop to end
				}
				if (!hasStop) {
					if (!hasATG || firstATG>START_ATG)
						addORF(frame, fStart, fEnd, false, false, false);	// start to end - no start or stop
					if (hasATG)
						addORF(frame, firstATG, fEnd, true, false, false);	// 1st ATG to end - no  stop
				}
				if (hasATG && hasStop) {  
					if (nATG>nStop)    
						addORF(frame, nATG, fEnd,  true, false, false); 	 // last ATG to end (if no later stop) 
				}
				
				if (hasStops) return null; // it calls finishThisFrame
				
				return finishThisFrame(frame);
			}
			catch (Exception e) {return getBest("All Possible ", e);	}	
		}
		private void addORF(int frame, int start, int end, boolean hasStart, boolean hasEnd, boolean is5Stop) {
			if (start>curSeqObj.seqLen || end>curSeqObj.seqLen) return;
			if (start<=0 || end<=0) return;
			if (start>end) return;
			
			int len = end-start+1;
			if (len>minLenForSeq) 
				thisFrameORFs.add(new OrfData(frame, start, end, hasStart, hasEnd, is5Stop));
		}
		/*******************************************************
		 * Remove non-ATGs that are close to ATGs (only one ORF if exact hit or hit has ATG)
		 * This does regular sort and return best
		 * CAS342 change checks
		 */
		private OrfData finishThisFrame(int frame) {
			try {
				if (thisFrameORFs.size()==0) return new OrfData(frame, 0, 0, false, false, false); 
				if (thisFrameORFs.size()==1) return thisFrameORFs.get(0);
				
			// Create temporary list to find close starts to remove 
				ArrayList <OrfTmp> startsObj = new ArrayList <OrfTmp> ();
				for (OrfData o : thisFrameORFs) 
					startsObj.add(new OrfTmp(o.oStart, o.hasATG, o.has5Stop, o));
				Collections.sort(startsObj);
				
				OrfTmp last=null;
				for (OrfTmp ot : startsObj) {
					if (last==null) {
						last=ot;
						continue;
					}
					int diff = (ot.oStart-last.oStart);
					if (diff<FROM_ATG_STOP && last.hasATG && ot.has5Stop) { 
						ot.bRm=true;
						thisFrameORFs.remove(ot.od);
					}
					else if (diff<FROM_ATG_STOP && last.has5Stop && ot.hasATG) {
						last.bRm=true;
						thisFrameORFs.remove(last.od);
					}
					else if (diff<FROM_ATG) { // FROM_ATG=30 
						if (last.hasATG && !ot.hasATG) {
							ot.bRm=true;
							thisFrameORFs.remove(ot.od);
						}
						else if (!last.hasATG && ot.hasATG) {
							last.bRm=true;
							thisFrameORFs.remove(last.od);
						}
						else if (last.oStart<ot.oStart) {
							ot.bRm=true;
							thisFrameORFs.remove(ot.od);
						}
						else {
							last.bRm=true;
							thisFrameORFs.remove(last.od);
						}
					}
					if (!ot.bRm) last=ot;
				}
				startsObj.clear();
				
			// Main sort
				Collections.sort(thisFrameORFs);
				
				return thisFrameORFs.get(0);
			}
			catch (Exception e) {return getBest("Finish", e);}
		}
		/***********************************************
		 * this is called when an exception happens (when no hit): 
		 * 		Comparison method violates its general contract
		 */
		private OrfData getBest(String msg, Exception e) {
			if (thisFrameORFs.size()==0) return null;
			
			prtObj.cntExcept++;
			
			OrfData best = thisFrameORFs.get(0);
			for (OrfData o : thisFrameORFs) 
			{	
				if (o.hasAnno && !best.hasAnno) best=o; //Rule1
				
				else if (Math.abs(best.lnlen-o.lnlen)> pdDiffLen && o.oLen>best.oLen) best=o; //Rule2
				
				else if (Math.abs(best.lnMK-o.lnMK)  > pdDiffMk && o.dMKscore>best.dMKscore) best=o; //Rule3
				
				else if (!best.hasATG && !best.hasStop && o.hasATG && o.hasStop) best=o; //Rule4a
				
				else if (o.oLen>best.oLen) best=o;	
			}
			return best;
		}
		
	} // end FindORF
	
	/************** ORFs to file - is class to collect all the methods **************************/
	class PrtFile {
		private BufferedWriter fh_allFrame = null, fh_bestFrame;
		private BufferedWriter fh_orfAA = null, fh_orfNT = null , fh_orfAll = null, fh_utr = null;
		private String fileHeader="", orfOverviewLegend="";
		private int writeGoodAA=0;
		
		private int cntExcept=0;
		private int cntHasORF=0; // they all do now
		// compute in prtFinal
		private int cntFrameHasAnno=0, cntBestMK=0, cntBestAll=0;
		private int cntLongest=0, cntHas300=0, cntHasBothEnds=0, cntHasAtLeastOneEnd=0;
		private int cntHasNs=0, cntMultiFrame=0, cntStopInHit=0;
		
		// compute in prtFinal and printed if isTest
		private int cntORFeq=0;
		private int cntTestORFappr=0, cntTestORFgt=0, cntTestORFeqEnds=0, cntTestORFgtEnds=0, cntTestGoodMK; 
		private int cntTestHasHit=0, cntTestHitBestMK=0, cntTestHitGoodMK=0, cntTestHitLongest=0, cntTestHitLongBestMK=0, cntTestHitHasEnds=0;
		private int cntTestGTHit=0, cntTestGTHitBestMK=0, cntTestGTHitGoodMK=0, cntTestGTHitLongest=0, cntTestGTHitLongBestMK=0, 
					cntTestGTHit300=0, cntTestGTHitSim=0;
		private int cntTestHitneORF=0;
		
		private int [] cntFrame = new int[7];
		
		private void init() {
			for (int i=0; i<7; i++) cntFrame[i]=0;
		}
		/***************************************************
		* Create table for Overview and anno.log
		* Add Remarks (a few are added elsewhere...)
		 */
		private void statsORFcompute(OrfData bestORF) {
			cntFrame[bestORF.oFrame+3]++;
			cntHasORF++;
			totalLength+=bestORF.oLen;
			
			// Longest and Markov
			boolean bNotMk=false, bNotLg=false;
			
			OrfData bestLen=bestORF, bestMarkov=bestORF;
			for (int i=1; i<bestPerFrameORFs.size(); i++) {
				OrfData o = bestPerFrameORFs.get(i);
				if (o.oLen > bestLen.oLen)            bestLen=o;
				if ((Math.abs(o.dMKscore-bestMarkov.dMKscore)> pdDiffMk 
						   && o.dMKscore > bestMarkov.dMKscore)) bestMarkov=o;
			}	
			if (bestORF==bestLen) cntLongest++;
			else bNotLg=true;
			
			if (bUseTrain) { 
				if (bestORF.nMKscore==3) cntTestGoodMK++; // >0 && best for selected frame
				if (bestORF==bestMarkov)  cntBestMK++;
				else bNotMk=true; // CAS326 add this one
			}
			
			// hit remarks
			boolean isExact=false;
			if (curSeqObj.orfHasHit) {
				cntFrameHasAnno++; // ORF frame == hit frame
				
				int hitStart = curSeqObj.seqHitStart, hitEnd = curSeqObj.seqHitEnd;
				int oStart = bestORF.oStart, oEnd=bestORF.oEnd;
				
				boolean x = (hitEnd == oEnd-3 && curSeqObj.hasEnd); // CAS403 wasn't checking hasEnd
				if (hitStart==oStart && (hitEnd == oEnd || x)) {
					curSeqObj.addRemark(Globals.RMK_ORF_exact);
					if (bestORF.hasATG && bestORF.hasStop) {
						curSeqObj.appendRemark(Globals.RMK_ORF_exact_END);
						isExact=true;
					}
				}
				else if (hitStart>=oStart && hitEnd <= oEnd) { 
					curSeqObj.addRemark(Globals.RMK_ORF_gtHit);
				}
				else if (oStart>hitStart && oEnd<hitEnd) { 
					curSeqObj.addRemark(Globals.RMK_ORF_ltHit);
				}
				else {
					curSeqObj.addRemark(Globals.RMK_ORF_appxHit);
				}
				if (!bNotLg && !bNotMk) {
					curSeqObj.appendRemark(Globals.RMK_ORF_ALL); // CAS334 new
					cntBestAll++;
				}
			}
			else if (curSeqObj.hitFrame!=NO_RF) {     // ORF frame != hit frame
				curSeqObj.addRemark(Globals.RMK_ORF_NOTHit);
			}
			if (bNotLg) curSeqObj.addRemark(Globals.RMK_ORF_NOTLONG);
			if (bNotMk) curSeqObj.addRemark(Globals.RMK_ORF_MarkovNotBest); // CAS326 new
			if (!bestORF.hasATG && !bestORF.hasStop) curSeqObj.addRemark(Globals.RMK_ORF_no5+Globals.RMK_ORF_no3); // CAS334 new
			else if (!bestORF.hasATG) curSeqObj.addRemark(Globals.RMK_ORF_no5);
			else if (!bestORF.hasStop) curSeqObj.addRemark(Globals.RMK_ORF_no3);
			
		/** ORF and problems (remarks have all been added) **/
			if (curSeqObj.remark.contains(Globals.RMK_ORF_Ns)) 		cntHasNs++;
			if (curSeqObj.remark.contains(Globals.RMK_MultiFrame)) 	cntMultiFrame++;  
			if (curSeqObj.remark.contains(Globals.RMK_HIT_hitSTOP)) cntStopInHit++;  
			
			if (bestORF.oLen>=300) cntHas300++; 
			if (bestORF.hasATG || bestORF.hasStop) cntHasAtLeastOneEnd++; 
			if (bestORF.hasATG && bestORF.hasStop) cntHasBothEnds++;	
			
			if (curSeqObj.remark.contains(Globals.RMK_ORF_exact)) 		 cntORFeq++; 
			else if (curSeqObj.remark.contains(Globals.RMK_ORF_gtHit)) 	 cntTestORFgt++; 
			else if (curSeqObj.remark.contains(Globals.RMK_ORF_appxHit)) cntTestORFappr++; 
			
			if (bestORF.hasATG && bestORF.hasStop) {
				if (curSeqObj.remark.contains(Globals.RMK_ORF_exact)) cntTestORFeqEnds++; 
				if (curSeqObj.remark.contains(Globals.RMK_ORF_gtHit)) cntTestORFgtEnds++; 
			}
			
			if (curSeqObj.hitFrame==NO_RF) return; // no hit
			
		// Has Hit even if not in ORF frame
			cntTestHasHit++;
			OrfData hitORF=bestORF;
			if (!curSeqObj.orfHasHit) { // Best ORF no hit; find ORF with hit
				for (int i=1; i<bestPerFrameORFs.size(); i++) {
					OrfData o = bestPerFrameORFs.get(i);
					if (o.hasAnno) {
						hitORF = o;
						break;
					}
				}
				cntTestHitneORF++;
			}
			if (hitORF==bestLen)    cntTestHitLongest++;
			
			if (bUseTrain) {
				if (hitORF==bestMarkov) cntTestHitBestMK++; 
				if (hitORF.nMKscore==3) cntTestHitGoodMK++;
				if (hitORF==bestLen && hitORF==bestMarkov) cntTestHitLongBestMK++;
			}
			if (hitORF.hasATG && hitORF.hasStop) cntTestHitHasEnds++;
			
		// Great hit computed earlier
			if (isExact) {
				cntTestGTHit++;
				if (hitORF==bestLen)    cntTestGTHitLongest++;
				if (bestORF.oLen>=300)  cntTestGTHit300++;
				if (curSeqObj.sim>=90)	cntTestGTHitSim++;
				if (bUseTrain) {
					if (!bNotMk) 			cntTestGTHitBestMK++; 
					if (hitORF.nMKscore==3) cntTestGTHitGoodMK++;
					if (!bNotLg && !bNotMk) cntTestGTHitLongBestMK++;
				}
			}
		}
		/*************************************************
		 * Output stats
		 * CAS335 tidied up some output
		 */
		private void statsORFprint() {
			Out.prt("                                                             "); 
			if (cntHasORF==0) {
				Out.PrtError("NO ORFs!!!!!!!!");
				return;
			}
			// CAS314 there was too much output - reduced it
			Out.logOnly(2, "");
			 
			int nCtg = seqData.length;
		
			int avg =  (int) (((double) totalLength/(double)cntHasORF)+0.5);
			String overview = "ORF stats: " +  "  Average length " +  avg;
			Out.PrtSpMsg(2, overview);												// term
			
			// table for overview; 3 column, text followed by number
			int [] justify =   {1,  0,  1,  0,  1,  0};
			int nRow = 8;
		    int nCol=  justify.length;
		    String [][] rows = new String[nRow][nCol];
			   
		    int nnn = NUM_N_CODONS.length();
		    
		    int r=0, c=0; 
		    rows[r][c] = "Has Hit";				rows[r++][c+1] = perCntText(cntFrameHasAnno, nCtg);
			rows[r][c] = "Is Longest ORF"; 		rows[r++][c+1] = perCntText(cntLongest, nCtg);
			rows[r][c] = "Markov Best Score";  	rows[r++][c+1] = perCntText(cntBestMK, nCtg);
			rows[r][c] = "All of the above";    rows[r++][c+1] = perCntText(cntBestAll, nCtg);
			
			r=0; c=2;
			rows[r][c] = "  Both Ends"; 		rows[r++][c+1] = perCntText(cntHasBothEnds, nCtg);
			rows[r][c] = "  ORF>=300"; 			rows[r++][c+1] = perCntText(cntHas300, nCtg);
			rows[r][c] = "  ORF=Hit"; 			rows[r++][c+1] = perCntText(cntORFeq,nCtg);
			rows[r][c] = "    with Ends";      	rows[r++][c+1] = perCntText(cntTestORFeqEnds,nCtg);
			
			r=0; c=4;
			rows[r][c] = "  Multi-frame"; 		rows[r++][c+1] = perCntText(cntMultiFrame, nCtg);
			rows[r][c] = "  Stops in Hit"; 		rows[r++][c+1] = perCntText(cntStopInHit, nCtg);
			rows[r][c] = "  >=" + nnn + " Ns in ORF";  rows[r++][c+1] = perCntText(cntHasNs, nCtg);
			if (cntHasORF!=nCtg) {
				rows[r][c] = "  No ORF"; 		rows[r++][c+1] = perCntText((nCtg-cntHasORF), nCtg);
			}
			else {
				rows[r][c] = "  "; 				rows[r++][c+1] = "";
			}
				
	        overview = Overview.subSpace + overview + "\n";
	        String msg = Out.makeTable(nCol, r+1, null, justify, rows);
	        String [] lines = msg.split("\n");
			for (int i=0; i<lines.length; i++) {
				overview += lines[i] + "\n";
				
				if (debug) 	Out.PrtSpMsg(2, lines[i]); // term & log
				else 		Out.logOnly(2, lines[i]); 		// CAS334 put in log	
			}
	        	
	        // Not part of overview, just print anno.log
        	int h=nCtg;
	        	
	        r=0; c=0;
	        rows[r][c] = "Additional ORF info";  rows[r++][c+1] = "";
	        rows[r][c] = " One End";		 		 rows[r++][c+1] = perCntText(cntHasAtLeastOneEnd, nCtg);
	        rows[r][c] = " Markov Good Frame";    rows[r++][c+1] = perCntText(cntTestGoodMK,h);
	        rows[r][c] = " ORF=Hit";  			 rows[r++][c+1] = perCntText(cntORFeq, h);
	        rows[r][c] = " ORF~Hit";  			 rows[r++][c+1] = perCntText(cntTestORFappr,h);  
	        rows[r][c] = " ORF>Hit";  			 rows[r++][c+1] = perCntText(cntTestORFgt,h);
	        rows[r][c] = "   with Ends";    		 rows[r++][c+1] = perCntText(cntTestORFgtEnds,h);
	              
	        r=0; c=2;
	        h=cntTestHasHit;
	        rows[r][c] = "   For seqs with hit";  	rows[r++][c+1] = perCntText(h,nCtg);
	        rows[r][c] = "    Both Ends";  			rows[r++][c+1] = perCntText(cntTestHitHasEnds,h);
	        rows[r][c] = "    Markov Good Frame";  	rows[r++][c+1] = perCntText(cntTestHitGoodMK,h);
	        rows[r][c] = "    Markov Best Score";  	rows[r++][c+1] = perCntText(cntTestHitBestMK,h);
	        rows[r][c] = "    Is Longest ORF";  	rows[r++][c+1] = perCntText(cntTestHitLongest,h);
	        rows[r][c] = "    Longest & Markov";	rows[r++][c+1] = perCntText(cntTestHitLongBestMK,h);
	        rows[r][c] = "    Not hit frame";  	    rows[r++][c+1] = Out.df(cntTestHitneORF);
	       	        
	        r=0; c=4;
	        h=cntTestGTHit;
	        rows[r][c] = "   ORF=Hit with Ends";  	rows[r++][c+1] = perCntText(h,nCtg);
	        rows[r][c] = "    ORF>=300";	  		rows[r++][c+1] = perCntText(cntTestGTHit300,h);
	        rows[r][c] = "    Markov Good Frame";  	rows[r++][c+1] = perCntText(cntTestGTHitGoodMK,h);
	        rows[r][c] = "    Markov Best Score";  	rows[r++][c+1] = perCntText(cntTestGTHitBestMK,h);
	        rows[r][c] = "    Is Longest ORF";  	rows[r++][c+1] = perCntText(cntTestGTHitLongest,h);
	        rows[r][c] = "    Longest & Markov";	rows[r++][c+1] = perCntText(cntTestGTHitLongBestMK,h);	       
        	rows[r][c] = "    Sim>=90";  			rows[r++][c+1] = perCntText(cntTestGTHitSim,h);
        		        	 
        	//CAS326 removed, CAS327 put back in fixed
	        msg = Out.makeTable(nCol, r+1, null, justify, rows);
	        lines = msg.split("\n");
	       
			for (int i=0; i<lines.length; i++) {
				if (debug) Out.PrtSpMsg(1, lines[i]); // term & log
				else 	   Out.logOnly(1, lines[i]);  // log only - not in overview
			}
	       
			String x="";
			for (int i=6; i>=0; i--) {
				if (i!=3 && cntFrame[i]>0) {
					double pp =  ( ( ((double) cntFrame[i]/(double)nCtg) * 100.0));
					String xx = String.format("(%4.1f%s) ",pp, "%");
					x+=String.format(" %d%s",  (i-3), xx);
				}
			}
			Out.PrtSpMsg(2,"   Frame:" + x);			//term
			Out.logOnly(2, "");
			Out.logOnly(2, "Both Ends:           Has Start and Stop codon");
			Out.logOnly(2, "ORF=Hit with ends:   ORF coordinates=Hit coordinates with ends");
		    Out.logOnly(2, "Markov Best Score:   Best score from best ORF for each of 6 frames");
		    Out.logOnly(2, "Markov Good Frame:   Score>0 and best score from 6 RFs of selected ORF");
		   
		    Out.logOnly(2, "");
		    
			// Save to database
			try {
				String gcOverview = gcObj.prtFinalGC();
				String ov = overview + gcOverview; // CAS330 removed extra line 
	 			mDB.executeUpdate("update assem_msg" +
	 					" set orf_msg='" + prtObj.orfOverviewLegend + "'" + ", gc_msg='" + ov + "'");
	 		}
	 		catch (Exception e) {ErrorReport.prtReport(e, "Adding ORF information to db");}
			
			
			if (debug) 	{
				Out.PrtSpMsg(1, "----------------------------------------------------------------------"); //xxx
				Out.PrtSpMsg(1, "Exceptions: " + cntExcept); 
			}
			else Out.logOnly(1, "Exceptions: " + cntExcept);
			
			Out.PrtSpMsg(2, "Wrote " + writeGoodAA + " ORFs to " + orfAllAAfname + " and " + orfFrameFname);	//prt
			
			if (!debug) Out.prtSp(2, "Additional information in log file " + Globals.annoFile);
			
			try {	
				fh_allFrame.close(); 
				fh_bestFrame.close();
				fh_orfAll.close(); 
				fh_orfAA.close(); 
				fh_orfNT.close();
				if (fh_utr!=null) fh_utr.close();
			}
			catch (Exception e) {ErrorReport.prtReport(e, "closing files");}
		}
		private boolean openFiles() {
			try {
				Out.PrtSpMsg(3, "Writing ORF information to database and files in " + 
							FileHelpers.removeRootPath(orfPath));
				String dbname = runSTCWMain.getProjName();
				if (dbname.startsWith(Globals.STCW)) dbname = dbname.substring(Globals.STCW.length());
				
				String framePath = orfPath + "/" + orfFrameFname;
				fh_allFrame = new BufferedWriter(new FileWriter(framePath));
				String head = "### " + Version.sTCWhead + "\n" + fileHeader;
				head += "### Output all ORFs that are selected, have a good Markov score, or length>=" +ORF_WRITE_LEN + "\n";
				head += String.format("%-15s %11s %11s %5s %12s   %14s \n", 
			"### Name",  "ntLen aaLen", "Type", "Frame", "Start..Stop", "Markov Codon"); // CAS334 remove Remark
				fh_allFrame.write(head);
				fh_allFrame.flush();
				
				framePath = orfPath + "/" + bestFrameFname;
				fh_bestFrame = new BufferedWriter(new FileWriter(framePath));
				fh_bestFrame.write("# Markov 6-frame scores; first is selected frame\n");
				fh_bestFrame.flush();
				
				String fastaPath = orfPath + "/" + orfAllAAfname;
				fh_orfAll = new BufferedWriter(new FileWriter(fastaPath));
				
				String fastaBPath = orfPath + "/" + orfBestAAfname;
				fh_orfAA = new BufferedWriter(new FileWriter(fastaBPath));
				
				String fastaDPath = orfPath + "/" + orfBestNTfname;
				fh_orfNT = new BufferedWriter(new FileWriter(fastaDPath));
				
				return true;
			}
			catch (Exception err) {ErrorReport.reportError(err, "ORF - open files");}
			return false;
		}
		/***********************************************
		 * 
		 */
		private int badORF=0;
		private void writeFilesForBestORF(OrfData best) {
		try {
	 // Write best for protein and CDS
			String aaORF = SequenceData.getTranslatedORF(curSeqObj.name, 
					curSeqObj.seq, curSeqObj.orfFrame, curSeqObj.orfStart, curSeqObj.orfEnd);
			if (aaORF==null) return; 
		
		// AA
			fh_orfAA.write(Fastaline(curSeqObj, best, true, ""));
			fh_orfAA.write(aaORF + "\n");
		
		// NT
			String head = Fastaline(curSeqObj, best, false, "");
			String ntORF = curSeqObj.getCDS();
			fh_orfNT.write(head);
			fh_orfNT.write(ntORF + "\n");
			fh_orfNT.flush();
		
			// Markov of best frame using ntORF; 
			fh_bestFrame.write(head);
			if (bUseTrain) fh_bestFrame.write(best.strMKscore);
			fh_bestFrame.flush();
		}
		catch (Exception e) {ErrorReport.reportError("Error in ORF file writer", e);}
		}
		
		/***********************************************************
		 * allGoodORFs.pep.fa
		 * allGoodORFs.scores.txt
		 */
		private void writeAllGoodFramesForORF() {
			try {
				String prtHit="";
				if (curSeqObj.hitFrame!=NO_RF) {
					String e = "0.0";
					if (curSeqObj.eval!=0.0) e = String.format("%.0E", curSeqObj.eval);
					prtHit =  String.format("Hit: %6s %3d%s %3d%s (%d..%d)", 
						e, curSeqObj.sim, "%", 
						curSeqObj.hitCov, "%", curSeqObj.seqHitStart, curSeqObj.seqHitEnd);
				}
				
			// Write All 'good' ORFs for curSeqObj
				String [] suf = {"_a","_b","_c","_d","_e", "_f", "_g", "_h", "_i", "_j"};
				int s=0;
				
				for (OrfData o : bestPerFrameORFs) {
					boolean bad=true; // Good: length, isGoodMK, hasHit
					if (o.oFrame!=curSeqObj.orfFrame) {
						if (o.oLen>=ORF_WRITE_LEN) bad=false;
						if (o.oLen>=ORF_WRITE_LEN_MK) {
							if (o.isGoodMarkov) bad=false;
							if (o.hasAnno && o.oFrame==curSeqObj.hitFrame)   bad=false;
						}
					}
					else bad=false; // CAS334 was not writing selected ORF
					
					if (bad) continue;
					
					if (o.oStart<=0 || o.oEnd>curSeqObj.seqLen || o.oEnd<=0) {
						System.err.print("Bad coord: " + Fastaline(curSeqObj, o, false, suf[s]));
						continue;
					}
					String orfAASeq = SequenceData.getTranslatedORF(curSeqObj.name, curSeqObj.seq, o.oFrame, o.oStart, o.oEnd);
					
					// sanity check - prints error to stdout in SequenceData during writeFilesForBestORF
					String orfnoStop = orfAASeq.substring(0, orfAASeq.length()-1);
					if (orfnoStop.contains(Globalx.stopStr)) {
						badORF++;
						if (badORF>BADORFS) ErrorReport.die("Too many ORFs with stop codons (" + badORF + ") - something is wrong");
					}
					writeGoodAA++;
					
					fh_orfAll.write(Fastaline(curSeqObj, o, true, suf[s]));
					fh_orfAll.write(orfAASeq + "\n");
					
					// goodFrames.txt
					String coord = String.format("%d..%d", o.oStart, o.oEnd );
					if (o.oFrame <0) coord = String.format("%d..%d", 
							(curSeqObj.seqLen-o.oStart+1), (curSeqObj.seqLen-o.oEnd+1) );
					int pLen = o.oLen/3;
			
					String x = (curSeqObj.hitFrame==o.oFrame) ? prtHit : "";
					String y = (!o.isGoodMarkov) ? "'" : " ";
					String n = curSeqObj.name+ suf[s];
					String str = String.format("%-15s %5d %5d %11s %5d %12s   %6.2f%s %6.2f   %s\n", 
						n, o.oLen, pLen,   ORFtype(o), o.oFrame, coord,  
						o.dMKscore, y, o.dCDscore,  x);
					
					fh_allFrame.write(str);
					
					s++;
				}
			}
			catch (Exception e) {ErrorReport.reportError("Write top three ORFs for " + curSeqObj.name, e);}
		}
		/**************************************
		 *  Fasta ">" line CAS334 few format changes
		 */
		private String Fastaline(SeqData seqOrf, OrfData o, boolean isAA, String suf) {
			String coord;
			if (o.oFrame>=0) coord = String.format("%d-%d", o.oStart, o.oEnd );
			else             coord = String.format("%d-%d", (seqOrf.seqLen-o.oStart+1), (seqOrf.seqLen-o.oEnd+1) );
			coord = "ORF:" + coord + "(" + o.oFrame + ")";
			
			int len = (isAA) ? o.oLen/3 : o.oLen;
			
			String hitInfo = (seqOrf.hitFrame!=NO_RF && seqOrf.hitFrame==o.oFrame) 
					? ("Hit:" + seqOrf.eval + "," + seqOrf.sim + "%" +"," + seqOrf.hitCov + "%")
					: "";
			
			String retStr;
			if (isAA) {
				String sLen = "aaLen:" + len;
				retStr = String.format(">%-15s type:%s %-15s %-20s %s\n", 
						seqOrf.name+suf, ORFtype(o), sLen, coord, hitInfo);
			}
			else {
				String seqLen = "seqLen:" +  seqOrf.seqLen;
				String oLen = "orfLen:" + len;
				retStr = String.format(">%-15s type:%s %-15s %-15s %-20s %s\n", 
						seqOrf.name, ORFtype(o), seqLen, oLen, coord, hitInfo);
			}
			return retStr;
		}
		private String ORFtype(OrfData o) { 
			if      ( o.hasATG && !o.hasStop) 	return "3p-partial";
			else if (!o.hasATG &&  o.hasStop) 	return "5p-partial";
			else if (!o.hasATG && !o.hasStop) 	return "partial   ";
			else 								return "complete  ";
		}
		
		private String perCntText(int cnt, int nCtg)  {
		 	String px;
		 	
		 	if (cnt==0)    px = String.format("(0%s)", "%");
		 	else {
		 		double p = ( ( ( (double) cnt/ (double) nCtg) ) *100.0);
		 		if (p<1.0) px = String.format("(<1%s)", "%");
		 		else if (p>99.9 && p<100.0) px = String.format("(99.9%s)", "%"); // rounds to 100
			 	else       px = String.format("(%.1f%s)", p, "%");
		 	}
		 	return String.format("%,d %8s", cnt, px);
		}
		/**********************************************
		 * Write parameters to Stdout, to file and some of them to database 
		 */
		private void parameters() {
			String [] msg = new String [20];
			for (int i=0; i<msg.length; i++) msg[i]="";
					
			int idx=0;
			orfOverviewLegend="";
			if (pbAltStart) msg[idx++] += "Use alternative start sites";
			else msg[idx++] += "Use ATG only for start site";
			
			String e = String.format("%.0E", pdHitEval);
			msg[idx++] += "Rule 1: Use Good hit: E-value <=" + e + " or Sim >= " + piHitSim + "%";
			if (piHitOlap>0 || piSeqOlap>0)
				msg[idx++] += "        and Hit coverage>=" + piHitOlap + "% and Seq coverage>=" + piSeqOlap + "% (internal params)";
			msg[idx++] += "Rule 2: Use longest ORF if Log Ratio > " + pdDiffLen;
			msg[idx++] += "Rule 3: Use best Markov score if Log Ratio > " + pdDiffMk;
			
			if (!pCdsFileName.equals("-1")) {
				String file = FileHelpers.removeRootPath(pCdsFileName);
			    msg[idx++] += "        Train using CDS file " + file + filterStr;
				bTrainFromFile=true;
			}
			else msg[idx++] +="        Train using best hits " + filterStr;
		
			for (int i=0; i<msg.length && msg[i]!=""; i++) {
				fileHeader += "### " + msg[i] + "\n";
				orfOverviewLegend += msg[i] + Globals.tcwDelim;
			}
		}
	}
	/**************** Data structure ***********************/
	 /****************************************************
	  * SeqData
	  */
	private class SeqData  implements Comparable<SeqData> {
		// from contig table
		int seqID=0;
		String name="", remark="", seq="";
		int seqLen, pid=0, pidov=0; // pidgo could be used to check for non-NT, but isn't right now
		String seqRev=""; // compute when needed.
		
		// from hits table
		int hitFrame=NO_RF, seqHitStart=-1, seqHitEnd=-1, hitCov=-2, sim=-2, seqCov=-2; 
		double eval=-1.0;
		boolean isGoodHit=false, isGreatHit=false, hasStops=false, hasAnno=false; 
		
		// compute for db
		double gcRatio=0.0;
		int cntNs=0, orfStart=0, orfEnd=0, orfFrame=0;
		boolean hasStart=false, hasEnd=false, orfHasHit=false;	
		double dMKscore=0.0;
				
		public SeqData(String seqname, String consensus) {
			name = seqname;
			seq = consensus.toLowerCase();
			seqLen = seq.length();
		}
		public SeqData(String seqname, String consensus, boolean isGreatHit, double eval) {
			name = seqname;
			seq = consensus.toLowerCase();
			seqLen = seq.length();
			this.isGreatHit=isGreatHit;
			this.eval = eval;
		}
		public SeqData(int seqid, String seqname, String consensus, String notes, int p, int po, int pg) {
			seqID = seqid;
			name = seqname;
			seq = consensus.toLowerCase();
			seqLen = seq.length();
			
			if (p>0) {
				pid = p;
				if (p!=po) pidov = po;
				//if (pg!=p && pg!=po) pidgo = pg;
				hasAnno=true;
			}
		
			if (notes!=null && notes.trim()!="")  {	
				if (notes.contains(Globals.RMK_MultiFrame)) // Multi is assigned in DoUniProt, all other remarks are assigned in DoORF.
					remark = Globals.RMK_MultiFrame;
			}
		}	
		public void addHit(double eval, int sim, int hitOlap, int seqOlap, int seqStart, 
				int seqEnd, int hitFrame, boolean good, boolean great, boolean stop) {
			this.eval = eval;
			this.sim = sim;
			this.hitCov = hitOlap;
			this.seqCov = seqOlap;
			this.seqHitStart = seqStart;
			this.seqHitEnd = seqEnd;
			this.hitFrame = hitFrame;
			this.isGoodHit = good;
			this.isGreatHit = great;
			this.hasStops = stop;
		}
		public void addRemark(String rmk) {
			if (rmk.equals("")) return; 
			if (remark.equals("")) remark=rmk;
			else remark += Globals.tcwDelim + " " + rmk;
		}
		public void appendRemark(String rmk) {
			remark += rmk;
		}
		private String getSeqRev() {
			if (!seqRev.equals("")) return seqRev;
			 StringBuilder builder = new StringBuilder ();
	         for (int i = seq.length() - 1; i >= 0; --i) {
	   		    builder.append(getBaseComplement(seq.charAt(i)));
	   		 }
	         seqRev = builder.toString();
	         return seqRev;
		}
		private char getBaseComplement(char chBase) {
			switch (chBase) {
			case 'a': return 't'; 
			case 't': return 'a';
			case 'c': return 'g';
			case 'g': return 'c'; 
			default: return chBase;
			}
		}
		private String getCDS() {
			if (orfEnd==0) return "";
			String s = (orfFrame<0) ? getSeqRev() : seq;
			//int end = (hasEnd) ? orfEnd-3 : orfEnd; // include stop codon
			return s.substring(orfStart-1, orfEnd);
		}
		public int compareTo(SeqData x) {
			if (isGreatHit && !x.isGreatHit) return -1;
			if (x.isGreatHit && !isGreatHit) return 1;
			
			if (seqLen>x.seqLen) return -1; // CAS308 put this before eval cmp
			if (seqLen<x.seqLen) return 1;
			
			if (eval<x.eval) return -1;
			if (eval>x.eval) return 1;
			
			return 0;
		}
	}
	
	/****************************************************
	  * OrfData
	  */
	private class OrfData implements Comparable<OrfData>{
		int seqID;
		int oFrame=NO_RF, oStart=0, oEnd=0, oLen=0;
		double  lnlen=0.0;
		int cntNs=0;
		
		double dMKscore=-100.0, dCDscore=-100.0, lnMK=0.0; // Need Codon score for SeqFrame display
		int nMKscore=0; // 3 = >0 & best; 2 = <0 & best; 1 = >0 & !best; 0 = <0 and !best
		String strMKscore="";
		
		boolean hasATG=false, hasStop=false, hasAnno=false, isFullLen=false, isGoodMarkov=false;
		boolean has5Stop=false; // CAS342; 5' abutts a Stop codon
		String remark="";
		 
		// coordinates are relative to the strand, i.e. to reversed sequence if frame<0
		public OrfData (int f, int start, int end, boolean hs, boolean he, boolean atStop) {
			seqID = curSeqObj.seqID;
			oFrame=f;
			oStart=start;
	        oEnd=end;
	        hasATG=hs;
	    	hasStop=he;
	    	has5Stop=atStop;
    		
	    	if (end==0) return; // dummy ORF
	    		
	    	// fix last coord if end
    		if (end > 0 && (end-start+1)%3!=0) {
    			if (he) Out.PrtErr("ORF coords !div/3: " + curSeqObj.name + " frame: " + f + " oStart: " 
    				+ start + " oEnd: " + end + " hasEnd: " + he + " orfLen: " + (end-start+1)  + " seqLen: " + curSeqObj.seqLen);
    			for (int i=0; i<4 && ((oEnd-oStart+1)%3!=0); i--, oEnd--); // or could be at the end
			}
    			
			String seq = (f>0) ? curSeqObj.seq : curSeqObj.seqRev;
			String orfSeq = seq.substring(oStart-1, oEnd); // If the coords != getCDS(), mkScore is diff
			
			oLen=oEnd-oStart+1;
			if (orfSeq.contains(NUM_N_CODONS)) {
				addRemark(Globals.RMK_ORF_Ns);
				for (int i=0; i<oLen; i++) 
					if (orfSeq.charAt(i)=='n') cntNs++;
			}
			lnlen = (oLen-cntNs)>0 ? Math.log(oLen-cntNs) : 0; // CAS334 remove n's from length
			if (Math.abs(oLen-curSeqObj.seqLen)<=3) isFullLen=true;
			
			if (bUseTrain) 
				train.scoreORF(this, orfSeq);
			
			if (curSeqObj.hitFrame == oFrame && curSeqObj.hasAnno) {//CAS334 was setting remarks here
				int hitStart = curSeqObj.seqHitStart, hitEnd = curSeqObj.seqHitEnd;
				if (hitStart > oEnd || hitEnd < oStart) hasAnno=false;
				else hasAnno=true;
			}
		}
		
		public void addRemark(String rmk) {
			if (remark.equals("")) remark=rmk;
			else remark += Globals.tcwDelim + " " + rmk;
		}
		// XXX heuristic - this gets exception in rare cases. Its caught in FindORF.getBest
		// 
		public int compareTo(OrfData x) {
			if (oLen<ORF_MIN_LEN && x.oLen<ORF_MIN_LEN) return 0; 
			if (oLen  < ORF_MIN_LEN) return 1;
			if (x.oLen< ORF_MIN_LEN) return -1;
			
			int diffLen = (x.oLen-oLen);
			
			if (pbTestType>0) { // command line test of longest ORF only or best MK only
				if (pbTestType==1) return diffLen;
				if (pbTestType==2) {
					if (dMKscore>x.dMKscore) return -1;
					if (dMKscore<x.dMKscore) return  1;
					return 0;
				}
			}
			
		// Rule 0: For gene datasets that do not have UTRs
			if (isFullLen && x.isFullLen && oFrame==1) {
				boolean x1 = (  hasAnno &&   hasATG);
				boolean x2 = (x.hasAnno && x.hasATG);
				if (x1 && !x2) return -1;
				if (!x1 && x2) return 1;
				
				if (!hasAnno && !x.hasAnno) {
					if ( hasATG &&  !x.hasATG) return -1;
					if (!hasATG &&   x.hasATG) return 1;
				}
			}
			
		// Rule 1: Good hit
			if (curSeqObj.isGoodHit) { // only no good if multi-frame and no good frame
				if ( hasAnno && !x.hasAnno) return -1;
				if (!hasAnno &&  x.hasAnno) return 1;
			}
			
		// Rule 2-3: Good length && Good Markov; CAS342 add first two checks with Len and Mk lower bounds
			double lnDiff = Math.abs(x.lnlen-lnlen);  // lnlen is log of orf length
			double mkDiff = Math.abs(x.lnMK-lnMK);
			int diffMk = (int) (x.dMKscore-dMKscore); // CAS334 changed to using log so not so stringent
			
			if ((oLen>SORT_LEN || x.oLen>SORT_LEN) && lnDiff>pdDiffLen) return diffLen;
			if ((dMKscore>SORT_MK || x.dMKscore>SORT_MK) && mkDiff>pdDiffMk) return diffMk;
				
			if (lnDiff>pdDiffLen) return diffLen;
			if (mkDiff>pdDiffMk)  return diffMk; 
			
		// Rule 4: CAS334 added  CAS342 quit checking for one end; add is5endAtStop
			boolean both =     hasATG  &&   hasStop;
			boolean xboth =  x.hasATG  && x.hasStop;
			if ( both  && !xboth) return -1;
			if (!both &&   xboth) return 1;
			
			if (hasATG && x.has5Stop) return -1;
			if (has5Stop && x.hasATG) return 1;
			
		// Default rules: if all else fails
			if ( hasAnno && !x.hasAnno) return -1; // even if not good, its something
			if (!hasAnno &&  x.hasAnno) return 1;
			
			if (lnDiff<mkDiff) return diffMk;
				
			return diffLen; // default to using longest
		}
		/************************************************************************/
		public String prtLine() {
			String start = (hasATG) ? 	"ATG" : "-";
			String stop = (hasStop) ? 	"Stop" : "-";
			String hit = (hasAnno) ? 	"Hit" : "-";
			String full = (isFullLen) ? "Full" : "-";
			return String.format("%2d %4d..%4d, Len %5d (%6.3f) Mk %6.2f (%6.3f) %d   %4s %4s %4s %4s ", 
					oFrame, oStart, oEnd, oLen, lnlen, dMKscore, lnMK, nMKscore, start, stop, hit, full);
		}
	} // end OrfData
	
	// Sort for close starts
	private class OrfTmp implements Comparable<OrfTmp>{
		private OrfTmp(int oStart, boolean hasATG, boolean has5Stop, OrfData od) {
			this.oStart=oStart;
			this.hasATG=hasATG;
			this.has5Stop=has5Stop;
			this.od = od;
		}
		int oStart;
		boolean hasATG, has5Stop, bRm=false;
		OrfData od;
		
		public int compareTo(OrfTmp x) {
			if (oStart<x.oStart) return -1;
			if (oStart>x.oStart) return 1;
			if (hasATG && !x.hasATG) return -1;
			if (!hasATG && x.hasATG) return 1;
			return 0;
		}
	}
	/***********************************************************/
	private class GcData {
		private void init() {
			for (int i=0; i<3; i++) regionLen[i]=regionCnt[i]=regionC[i]=regionG[i]=regionCpG[i]=0;
			for (int i=0; i<3; i++) cdsPos[i]=0; 
		}
		
		// for each sequence
		private void seqGC() {
            String seq = (curSeqObj.orfFrame<0) ? curSeqObj.getSeqRev() : curSeqObj.seq;
           
            int nGCcount=0, cntNs=0;
            for (int i = 0; i < seq.length(); i++) {
    				char c = seq.charAt(i);
    				if (c == 'g' || c == 'c') nGCcount++;
    				else if (c == 'n') cntNs++;
            }
			curSeqObj.gcRatio = (double) nGCcount/ (double) seq.length();
			curSeqObj.cntNs = cntNs;
			
			allGC += (double)nGCcount/(double)seq.length();
			
			// Compute overall GC and CpG per UTR/CDS/UTR, and GC for each codon position
			if (curSeqObj.orfFrame==0 || curSeqObj.orfStart<=0) return; 
			
			String utr5 = seq.substring(0, curSeqObj.orfStart-1);
			String cds =  seq.substring(curSeqObj.orfStart-1, curSeqObj.orfEnd);
			String utr3 = seq.substring(curSeqObj.orfEnd);
			stat(idx5, utr5);
			stat(idxC, cds);
			stat(idx3, utr3);
			
			for (int i=0; i<cds.length()-3; i+=3) {
				String codon;
				try { codon = cds.substring(i, i+3);
				} catch (Exception e) {Out.PrtError("GC " + i + " " + (i+3) + cds.length()); break;}
				
				for (int j=0; j<3; j++) {
					char c = codon.charAt(j);
					if (c=='c' || c=='g') cdsPos[j]++;
				}
			}
		}
		private void stat(int x, String seq) {
			int len = seq.length();
			
			regionLen[x]+= len;
			regionCnt[x]++;
			boolean lastC=false;
			for (int j=0; j<len; j++) {
				char c = seq.charAt(j);	
				if (c=='g') {
					regionG[x]++;
					if (lastC) regionCpG[x]++;
				}
				if (c=='c') {
					regionC[x]++;
					lastC=true;
				}
				else lastC=false;
			}
		}
	
		public String prtFinalGC() {
			// table for overview; 3 column, text followed by number
			int [] justify =   {0,  0,  0, 0,  0, 0, 0, 0, 0, 0};
			int nRow = 5;
			int nCol=  justify.length;
			String [][] rows = new String[nRow][nCol];
			
			// Overview.java searches for "content:" to determine overview formating
			double total = (allGC / (double) seqData.length)*100.0; 
			double [] perPos = new double [3];
			for (int i=0; i<3; i++) 
				perPos[i] = ((double) cdsPos[i] / (double) regionLen[idxC])*100.0; 
			
			String utr5Len = Out.kbText(regionLen[idx5]);
			String cdsLen =  Out.kbText(regionLen[idxC]);
			String utr3Len = Out.kbText(regionLen[idx3]);
			String utr5Avg = String.format("%6.1f", (double) regionLen[idx5]/(double) regionCnt[idx5]);
			String cdsAvg =  String.format("%6.1f", (double) regionLen[idxC]/(double) regionCnt[idxC]);
			String utr3Avg = String.format("%6.1f", (double) regionLen[idx3]/(double) regionCnt[idx3]);
			
			String utr5GC = txtPer(idx5);
			String cdsGC  = txtPer(idxC);
			String utr3GC = txtPer(idx3);
			String utr5CpG = txtOE(idx5);
			String cdsCpG  = txtOE(idxC);
			String utr3CpG = txtOE(idx3);
			
			int r=0, c=0;
		
			rows[r][c] = "Pos1"; rows[r++][c+1] = String.format("%.1f%s", perPos[0], "%");
			rows[r][c] = "Pos2"; rows[r++][c+1] = String.format("%.1f%s", perPos[1], "%");
			rows[r][c] = "Pos3"; rows[r++][c+1] = String.format("%.1f%s", perPos[2], "%");
			
			r=0; c=2;
			rows[r++][c] = ""; 	   rows[r++][c] = "  %GC    "; 	rows[r++][c] = "  CpG-O/E"; 
			r=0; c=3;
			rows[r++][c] = "5UTR"; rows[r++][c] = utr5GC; 	rows[r++][c] = utr5CpG; 
			r=0; c=4;
			rows[r++][c] = "CDS"; 	rows[r++][c] = cdsGC; 	rows[r++][c] = cdsCpG; 
			r=0; c=5;
			rows[r++][c] = "3UTR"; 	rows[r++][c] = utr3GC; 	rows[r++][c] = utr3CpG; 
			
			r=0; c=6;
			rows[r++][c] = ""; 		rows[r++][c] = "  Length"; 	rows[r++][c] = "  AvgLen"; 
			r=0; c=7;
			rows[r++][c] = "5UTR"; rows[r++][c] = utr5Len; 		rows[r++][c] = utr5Avg; 
			r=0; c=8;
			rows[r++][c] = "CDS"; 	rows[r++][c] = cdsLen; 		rows[r++][c] = cdsAvg; 
			r=0; c=9;
			rows[r++][c] = "3UTR"; rows[r++][c] = utr3Len; 		rows[r++][c] = utr3Avg; 
			
			String overview = String.format("GC content: %.2f%s", total, "%");
			Out.PrtSpMsg(2, overview);
			overview = Overview.subSpace + overview + "\n";
			
			String msg = Out.makeTable(nCol, r+1, null, justify, rows);
	        
			String [] lines = msg.split("\n");
			for (int i=0; i<lines.length; i++) {
				if (lines[i].trim()!="") 
					overview += lines[i] + "\n";
			}
			
			return overview;
		}
		private String txtPer(int n) {
			double p = 0;
			long cnt = regionG[n]+regionC[n];
		 	if (cnt>0) p = ( ( (double) cnt/ (double) regionLen[n]) *100.0);
		 	if (p<1.0) return String.format("<1%s", "%");
		 	return String.format("%5.2f",p);
		}
		private String txtOE(int n) {
			double p = 0;
			if (regionG[n]>0 && regionC[n]>0 && regionCpG[n]>0 && regionLen[n]>0) {
				p = ((double)regionCpG[n]/(double)(regionG[n]*regionC[n])) * (double)regionLen[n];
			}
			else return String.format("%5s", "N/A");
		 	return String.format("%5.2f",p);
		}
		private int idx5 = 0, idxC = 1, idx3 = 2;
		private long [] cdsPos		= new long[3]; // position 1,2,3 of codon 
		private long [] regionG  	= new long[3]; // G UTR'5, CDS, UTR'3
		private long [] regionC  	= new long[3]; // C UTR'5, CDS, UTR'3
		private long [] regionCpG	= new long[3]; // CpG UTR'5, CDS, UTR'3
		private long [] regionLen	= new long[3]; // total bases
		private long [] regionCnt 	= new long[3]; // number
		private double allGC=0.0; 
	} 
	/********************************************************/
	private class Train {
		final String FRAME = "FRAME-";

		private void init() {
			for (int i=0; i<codons.length; i++) {
				trCodonCntMap.put(codons[i], 0);
			}
			baseCntMap.put("a", 0); baseCntMap.put("t", 0);
			baseCntMap.put("c", 0); baseCntMap.put("g", 0);
			try {
				ResultSet rs = mDB.executeQuery("show table status like 'tuple_usage'");
				if (!rs.first()) {
					mDB.executeUpdate("create table tuple_usage " +
						"(tuple varchar(10),  freq  double default 0.0, INDEX(tuple)) ENGINE=MyISAM;");		
				}
				else mDB.tableDelete("tuple_usage");
				
				rs = mDB.executeQuery("show table status like 'tuple_orfs'");
				if (!rs.first()) {
					mDB.executeUpdate("create table tuple_orfs " +
						"(CTGid bigint,  value tinytext, INDEX(CTGid)) ENGINE=MyISAM;");		
				}
				else mDB.tableDelete("tuple_orfs");
				if (rs!=null) rs.close();
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Creating tuple MySQL tables");}
		}
		/******************************************************
		 * Calculates the codon and hex score for this ORF
		 */
		private void scoreORF(OrfData o, String orfSeq) {
			try {
				o.strMKscore = scoreObj.scoreSeq("Markov", scoreObj.fnMarkov, orfSeq);
				o.dMKscore = scoreObj.dScore();
				o.nMKscore = scoreObj.nScore(); // 0 not good, 1  >0 and not best, 2 <0 and best, 3 (Pos & best)
				o.isGoodMarkov = scoreObj.isGood();
			
				// any number between 0 and <1 will be 0.01, as compare two small #'s can result in 'no relevant difference'
				boolean isNeg = (o.dMKscore<0);
				double absMK = Math.abs(o.dMKscore);
				o.lnMK = (absMK<1) ? 0.01 : Math.log(absMK); // CAS334
				if (isNeg) o.lnMK = -o.lnMK;
				
				// just for display
				scoreObj.scoreSeq("Codon", scoreObj.fnCodon, orfSeq);
				o.dCDscore = scoreObj.dScore();
			}
			catch (Exception e) {
				Out.prtToErr(curSeqObj.name + " f:" + o.oFrame + " os:" +  o.oStart + " oe:" + o.oEnd  
						+ " ol:" + o.oLen + " sl:" + curSeqObj.seq.length());
				ErrorReport.prtReport(e, "Compute ORF scores"); 
				System.exit(-1);
			}
			return;
		}
		/****************************************************
		 * Count codons from hit regions or from file
		 */
		private void countCodons(String seq) {
			int seqLen = seq.length();
			for (int i=0; i<seqLen-3; i+=3) {
				String c = seq.substring(i, i+3);
				if (!c.contains("n")) {
					if (!trCodonCntMap.containsKey(c)) Out.prtToErr("No codon  '" + c + "'");
					else {
						trCodonCntMap.put(c,  trCodonCntMap.get(c)+1);
						trainCodons++;
					}
				}
			}
		}
		/*****************************************************************
		 * Count 1-base offset hexamer from hit regions or from file
		 * 5th Order Markov Chain copied from Transcoder Perl code. 
		 */
		private void countMarkov(String seq) {
			try {
				int seqlen = seq.length();
				trainHex+=seqlen;
				
				for (int mk=0; mk<6; mk++) {
					for (int i=mk; i<seqlen; i++) {
						int frame = i%3;
						if (frame==0 && i==seqlen-2-1) {
							String codon = seq.substring(i, i+3);
							if (isCodonStop(codon)) break;
						}
						if (mk==0) {
							String key = FRAME + frame;
							addOne(key, trHexCntMap);
						}
						int s = (i-mk);
						String kmer = seq.substring(s, s+mk+1);
						if (kmer.contains("n")) continue;
						
						String key = kmer + "-" + frame;
						addOne(key, trHexCntMap);
					}
				}
			}
			catch (Exception e) {e.printStackTrace(); System.exit(0);}
		}
		/***************************************************
		 * Main routine for training. The codons and hexs have been counted
		 */
		private void computeCodonMarkov() {
			long startTime = Out.getTime();
			Out.PrtSpMsg(2, "Start computation of coding potential");
			if (bTrainFromFile) {
				if (!pCdsFileName.equals("-")) {   // Build cntMap, freqMap and lnMap for codon and hex
					trainFromFasta(pCdsFileName);
				}
				else Out.die("TCW error getting training set");
			}
			else {
				trainFromHits();
			}
			
			Out.PrtSpCntmMsg(3, trainHex, "Bases used for training"); // CAS314
			
			if (totalIgnoreSTOPs>0) {
				String msg = (bTrainFromFile) ? "Sequences with in-frame Stops" : "Sequences with Stops within hits";
				Out.PrtSpCntMsg(3, totalIgnoreSTOPs, msg + " - ignored");
			}
	 		
			computeCodonLNwriteFile();
			computeMarkovLLRwriteFile();
			saveTuplesToDB();
			scoreObj = new Markov(trCodonLNmap, trHexLRRmap);
			
			// only use codonLnMap and hexLnMap for ORFs. 
			trCodonCntMap.clear(); trHexCntMap.clear();
			
			Out.PrtSpMsgTimeMem(2, "Complete training", startTime);
			
			// CAS334 move to end so compute/store anyway
			if (!bTrainFromFile && totalSeqForTrain<piMinSet) {
				bUseTrain=false;
				filterStr = "     Rule 3 not used - Training set too small (" + totalSeqForTrain + ")";
				Out.PrtWarn(filterStr);
				return;
			}
		}
		
		/**************************************************************
		 * Counts are complete. Compute frequencies and log. 
		 * 
		 * log-likelihood = Log(product1..N(freq(codon))/product1..N(expected)))
		 * = Sum1..N(log(freq(codon))-Sum1..N(log(expected)
		 *   and log(expected ** N) = N * log(expected)
		 * may have 'n', which are ignored; zero score for them
		 */
		private void computeCodonLNwriteFile() {
			try {
				String path = orfPath + "/" + codonScoreFname;
				Out.PrtSpMsg(3, "Compute Codon frequency and write to " + 
						FileHelpers.removeRootPath(path));
				
				BufferedWriter statsFile = new BufferedWriter(new FileWriter(path));
				statsFile.write("## TCW Codon Usage\n");
				statsFile.write("## Trained on " + train.totalSeqForTrain + " sequences and " + train.trainCodons + " codons\n");
				
				String x = String.format("%5s %2s  %9s  %7s\n", "Codon", "AA", "/1000", "Number");
				statsFile.write(x);
				
				double codonZero = 0.0, totalFreq=0.0;  
				long totalCodon=0;
				
			// default value in case codon with no count - though would be strange if it happened
				for (int i=0; i<codons.length; i++) {
					int count = (int) trCodonCntMap.get(codons[i]);
					double fq = ((double) count/ (double) trainCodons);
					if (fq>0.0) {
						double ll = Math.log(fq);  
						if (ll<codonZero) codonZero = ll;
					}
				}
				codonZero-=1.0;
				
			// compute freq and write to file
				for (int i=0; i<codons.length; i++) {
					int count = (int) trCodonCntMap.get(codons[i]);
					double fq = ((double) count/ (double) trainCodons);
					
					double ll = codonZero;
					if (isCodonStop(codons[i])) ll = CODON_STOP;
					else if (fq>0.0) ll = Math.log(fq);  // computes log here
					
					trCodonLNmap.put(codons[i], ll); 
					
					totalCodon+=count;
					double freq = fq*1000; // EMBOSS cusp uses 1000
					totalFreq += freq;
					x = String.format("%5s %2s  %9.2f  %7d\n", codons[i], aa[i], freq, count);
					statsFile.write(x);
				}
				x = String.format("%5s %2s  %9.2f  %7d\n", "Total", "-", totalFreq, totalCodon);
				statsFile.write(x);
				statsFile.close();
			}
			catch (Exception e) {ErrorReport.die(e, "Writing training set"); }
		}	
		/*******************************************************************
		 * 5th Order Markov model - totally duplicated from Transcoder Perl source
		 * report_logliklihood_ratios
		 */
		private void computeMarkovLLRwriteFile() {
			try {
				String path = orfPath + "/" + markovScoreFname;
				Out.PrtSpMsg(3, "Compute Markov loglikelihood and write to " + 
						FileHelpers.removeRootPath(path));
				
				BufferedWriter hexFile = new BufferedWriter(new FileWriter(path));
				hexFile.write("#TCW generated\n");
				hexFile.write("#framed_kmer    kmer_count      kminus1_prefix_count    loglikelihood\n");
				
				computeMarkovBaseFreq();
				
				for (String key : trHexCntMap.keySet()) {
					if (key.startsWith(FRAME)) continue;
					
					int kmerCnt = trHexCntMap.get(key);
					
					String [] tok = key.split("-");
					if (tok.length!=2) {
						Out.prtToErr(key);
						continue;
					}
					String kmer = tok[0];
					int kmerLen = kmer.length();
					
					int frame = Integer.parseInt(tok[1])-1;
					if (frame<0) frame=2;
					
					String key1 = "-" + frame;
					int kmer1Cnt = 0;
					String lastBase = kmer;
					
					if (kmerLen>1) {
						key1 = kmer.substring(0, kmerLen-1) + key1;
						kmer1Cnt = (trHexCntMap.containsKey(key1)) ? trHexCntMap.get(key1) : 0;
						lastBase = kmer.substring(kmerLen-1, kmerLen);
					}
					else {
						String x = FRAME + frame;
						kmer1Cnt = (trHexCntMap.containsKey(x)) ? trHexCntMap.get(x) : 0;
					}
				
					double prob = (double) (kmerCnt+1)/(double)(kmer1Cnt+4);
					
					double lprob = baseFreqMap.get(lastBase);
					double logliklihood = Math.log(prob / lprob);
					if (pbTransDecoder) {
						String s = String.format("%5.3f", logliklihood); // reduce precision to simulate Perl precision
						double sf = Double.parseDouble(s);
						trHexLRRmap.put(key, sf);
					}
					else trHexLRRmap.put(key, logliklihood);
					
					String out = String.format("%-8s %7d   %7d   %f\n", key, kmerCnt, kmer1Cnt, logliklihood);
					hexFile.write(out);
				}
				hexFile.close();
			}
			catch (Exception e) {ErrorReport.die(e, "Computing Markov Log"); }
		}
		// Base frequencies is on the full sequences (not the training set), 
		// so the same CDS input to different databases results in different log-liklihood scores
		private void computeMarkovBaseFreq() {
			// TransDecoder scores both strands, which makes G=C and A=T
			for (int i=0; i<seqData.length; i++) {
				String seq = seqData[i].seq;
				for (int j=0; j<seq.length(); j++) {
					String b = seq.substring(j, j+1);
					if (!b.equals("n")) addOne(b, baseCntMap);
				}
				seq = seqData[i].getSeqRev();
				for (int j=0; j<seq.length(); j++) {
					String b = seq.substring(j, j+1);
					if (!b.equals("n")) addOne(b, baseCntMap);
				}
			}
			int sum=0;
			for (String key : baseCntMap.keySet()) 
				sum +=  baseCntMap.get(key);
			
			for (String key : baseCntMap.keySet()) {
				int bcnt = baseCntMap.get(key);
				double freq = (double)bcnt/(double)sum;
				
				if (pbTransDecoder) {
					String s = String.format("%5.3f", freq); // reduce precision to simulate Perl precision
					double sf = Double.parseDouble(s);
					baseFreqMap.put(key, sf);
				}
				else baseFreqMap.put(key, freq);
			}	
			String x=""; // CAS334 always write frequencies to anno.log
			for (String key: baseFreqMap.keySet()) 
				x += String.format("%s:%.3f  ", key,baseFreqMap.get(key));
			if (pbTransDecoder) Out.PrtSpMsg(4, "Base Frequencies: " + x);
			Out.logOnly(4, "Base Frequencies: " + x);
		}
		private void saveTuplesToDB() {
			try {
				Out.PrtSpMsg(3, "Save training results to database");
				
				// saved for seqFramePanel display
	        	    PreparedStatement ps2 = mDB.prepareStatement(
	     					"insert tuple_usage SET tuple=?, freq=?");
	        	    mDB.openTransaction();
	        		int cnt=0, total=0;
	     		for (String codon : trCodonLNmap.keySet()) {
	     			double llr = trCodonLNmap.get(codon);
	     			ps2.setString(1, codon);
	     			ps2.setDouble(2, llr);
	     			ps2.addBatch();
	     			cnt++; total++;
	     			if (cnt==1000) { 
	     				cnt=0;
	     				ps2.executeBatch();
	     				Out.r("Saved " + total);
	     			}
	     		}
	     		for (String hex : trHexLRRmap.keySet()) {
	     			double llr = trHexLRRmap.get(hex);
	     			ps2.setString(1, hex);
	     			ps2.setDouble(2, llr);
	     			ps2.addBatch();
	     			cnt++; total++;
	     			if (cnt==1000) { 
	     				cnt=0;
	     				ps2.executeBatch();
	     				Out.r("Saved " + total);
	     			}
	     		}
	     		if (cnt>0) ps2.executeBatch();
	     		ps2.close();
	     		mDB.closeTransaction();
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Save tuples");}
		}
		/***************************************************************
		 * Train during seqLoadData
		 */
		 private void trainFromHits() {
			 int idx=0, cntIgn=0, cntAll=0;
			 try {
			/* get initial set, which just weeds out bad hits */
				 ArrayList <SeqData> firstSet = new ArrayList <SeqData> ();
				 
				 for (idx=0; idx<seqData.length; idx++) {
					SeqData seqObj = seqData[idx];
	 	   			if (seqObj.pid==0 || !seqObj.isGoodHit || seqObj.hasStops || seqObj.seqLen<=ORF_WRITE_LEN_MK) {
	 	   				cntIgn++; continue;
	 	   			} 
		 	   		if (seqObj.seqHitStart<=0 || seqObj.seqHitEnd<=0 || seqObj.seqHitEnd>seqObj.seqLen) {
						Out.PrtWarn(String.format("Bad coords in trainFromHits: %s (%d..%d) Pid %d %b",
								seqObj.name, seqObj.seqHitStart, seqObj.seqHitEnd, seqObj.pid, seqObj.hasAnno));
						cntIgn++;
						continue;
					}
		 	   		cntAll++;
		 	   		
		 	   		String seq = seqObj.seq;
		 	   		if (seqObj.hitFrame < 0) seq = seqObj.getSeqRev();
				
		 	   		String hitSeq = seq.substring(seqObj.seqHitStart-1, seqObj.seqHitEnd-1); // coords already reversed
	
		 	   		SeqData hitObj = new SeqData(seqObj.name, hitSeq, seqObj.isGreatHit, seqObj.eval);
		 	   		firstSet.add(hitObj);
				 }
				 Out.PrtSpCntMsg2(3, cntAll, "hit sequences", cntIgn, "Ignored");
				 
		/* Get unique N set (2000) */
				 ArrayList <SeqData> uniqueSet = findUniqueSet(firstSet);
				
		/* Calculate training tuples */
				int cnt=0;
			 	for (SeqData hitObj : uniqueSet) {
			 		countCodons(hitObj.seq); // trCodonCntMap
					countMarkov(hitObj.seq); // trHexCntMap
					
					cnt++;
					if (cnt==100) {
						Out.r("Train " + cnt);
						cnt=0;
					}
			 	}
				kmerIdList.clear();
				kmerCntList.clear();
			 }
			 catch (Exception e) {
				 Out.prtToErr(seqData[idx].name + " start: " + seqData[idx].seqHitStart + " end: "  + seqData[idx].seqHitEnd);
				 ErrorReport.die(e, "Training Sequence Data");
			 }
		 }
		
		/****************************************************************
		 * Read a file of CDS to train from or a file of codon frequencies (then, no hexamers)
		 */
		private void trainFromFasta(String file) {
			try {		
				Out.PrtSpMsg(3, "Reading " + file);
				File f = FileHelpers.getFile(projPath, file);
				if (f==null || !f.exists()) Out.die("Cannot open " +  file);
				
				String line, seq="", name="";
				int cntAll=0;
		
			/* get initial set */
				ArrayList <SeqData> firstSet = new ArrayList <SeqData> ();
				
				BufferedReader reader = FileHelpers.openGZIP(f.getAbsolutePath());
				
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.length()==0 || line.startsWith("#")) continue;
			
					if (line.startsWith(">")) {
						if (!seq.equals("")) {
							SeqData hitObj = new SeqData (name, seq);
							firstSet.add(hitObj);
						}
						name = (line.contains(" ")) ? line.substring(0, line.indexOf(" ")) : line;
						seq = "";
						cntAll++;
						if (cntAll%1000==0) Out.r("Read " + cntAll);
					}
					else seq += line.toLowerCase(); 
				}
				reader.close();
				if (!seq.equals("")) {
					SeqData hitObj = new SeqData (name, seq);
					firstSet.add(hitObj);
				}
				Out.PrtSpCntMsg(3, firstSet.size(), "CDS sequences");
				
			/* Get unique N set */		
				ArrayList <SeqData> uniqueSet = findUniqueSet(firstSet);
			
			/* Calculate training tuples */
				int cnt=0;
				for (SeqData hitObj : uniqueSet) {
			 		countCodons(hitObj.seq);
					countMarkov(hitObj.seq);
					cnt++;
					if (cnt%1000==0)Out.r("Train " + cnt);
					
			 	}
				kmerIdList.clear();
				kmerCntList.clear();
			}
			catch (Exception e) {ErrorReport.die(e, "Training data from file"); }
		}
		/*************************************************************
		 * Find Unique N set
		 */
		private ArrayList <SeqData> findUniqueSet(ArrayList <SeqData> firstSet) {
			if (!pbFindDups) {
				totalSeqForTrain = firstSet.size();
				filterStr += " (Used " + totalSeqForTrain + " seqs)";
				Out.PrtSpMsg(3, "Train with " + totalSeqForTrain + " sequences");
				return firstSet;
			}
			try {
				// Create kmerMap<String, Integer> where each string has an index
				int arr[] = new int[KMER_wordSize];
			    ntMap.put(1,'a'); ntMap.put(2,'c'); ntMap.put(3,'t'); ntMap.put(4,'g');
			    
			    createKmerIndex(arr, 4, KMER_wordSize, 0);
				
				Out.PrtSpMsg(3, "Find longest unique sequences with best hits");
				
				Collections.sort(firstSet);
				
				ArrayList <SeqData> uniqueSet = new ArrayList <SeqData> ();
				int cntPass=0, cntAll=0, cntDup=0, cntGreat=0;
				long totBases=0;
				
				for (SeqData seqObj : firstSet) {
					if (isUniqueSeq(seqObj.name, seqObj.seq)) {
						uniqueSet.add(seqObj);
						cntPass++;
						totBases += seqObj.seqLen;
						if (seqObj.isGreatHit) cntGreat++;
						if (totBases>=piTrainNbases && cntPass>=piTrainNseq) break;
					}
					else cntDup++;
					
					cntAll++;
					if (cntAll%100 == 0) Out.r("Pass " + cntPass + " from " + cntAll);
				}
				Out.PrtSpCntMsg(3, cntDup, "Non-unique from longest " + cntAll + " sequences ");
				kmerIdxMap.clear();
				totalSeqForTrain = uniqueSet.size();
				filterStr += "(" + totalSeqForTrain + " seqs, " + Out.kMText(totBases) + " bases)";
				Out.PrtSpMsg(3, "Train with " + uniqueSet.size() + " unique longest sequences (" + cntGreat + ")");
				
				return uniqueSet;
			}
			catch (Exception e) {ErrorReport.die(e, "Unique set");  return firstSet;}
		}
		/*********************************************************************
		 *  input is CDS or Hit region, so should be in-frame and correct coords
		 *  heuristic to catch sequences that are highly similar based on similar kmers
		 *  CAS334 speedup: changed HashMap of results to HashMap of indices for 5-tuples
		 */
		private boolean isUniqueSeq(String seqID, String seq) {
			if (!pbFindDups) return true;
			
			try {
				int nKmer = kmerIdxMap.size();
				
			// count kmers for seqID
				int [] kmerCnt = new int [nKmer]; 
				for (int i=0; i<kmerIdxMap.size(); i++) kmerCnt[i]=0;
				
				int tot=0;
				for (int i=0; i<seq.length()-KMER_wordSize; i++) {
				   	String word = seq.substring(i, i+KMER_wordSize);
				   	if (kmerIdxMap.containsKey(word)) {
				   		int idx = kmerIdxMap.get(word);
				   		kmerCnt[idx]++;
				   		tot++;
				   	}
				}
				
			// compare with others
				String dupSeqID=null;
				int total=0, cutoffDiff=tot/2, cnt1, cnt2;
				for (int idx=0; idx<kmerIdList.size(); idx++) {
				   	int [] cmpList = kmerCntList.get(idx);
		
		 			int cntDiff=0, cntSame=0;
				   	for (int j=0; j<nKmer; j++) {
				   		cnt1 = kmerCnt[j];
				   		
				   		if (cnt1>0) {
				   			cnt2 = cmpList[j];
				   			if (cnt2>0) {
				   				double dcnt1 = Math.log(cnt1);
								double dcnt2 = Math.log(cnt2);
								if (Math.abs(dcnt1-dcnt2) < 0.5) cntSame++;
				   			}
				   			else cntDiff++;
				   		}
				   	}
					total=cntSame+cntDiff;
		
					if (cntDiff < cutoffDiff && cntSame>0) {
						double x = Out.perF(cntSame, total);
						if (x >= KMER_perSame) {
							dupSeqID=kmerIdList.get(idx);
							break;
						}
					}
				}
				if (dupSeqID==null) {
					kmerIdList.add(seqID);
					kmerCntList.add(kmerCnt);
					return true;
				}
				else {
					cntDup++;
					if (cntDup<5) 
						Out.PrtSpMsg(4, "skipping non-unique training candidate " + seqID + " (" + dupSeqID + " " + total + ")");
					else if (cntDup==5) Out.PrtSpMsg(4, "suppress further non-unique messages");
					return false;
				}
			}
			catch (Exception e) {ErrorReport.die(e, "Non-unique sequences"); return false;}
		}
		// Index of 5-tuples so sequences have array of size index that hold counts
		private void createKmerIndex(int arr[], int n, int k, int index) {
		 	int i;
		 	if (k == 0) {
		 		 String kmer="";
			     for (int x = 0; x < index; x++) kmer += ntMap.get(arr[x]);
			     kmerIdxMap.put(kmer, kmerIdx++);
			     return;
		 	}
	 		for(i = 1; i<=n; ++i) {
	 			arr[index] = i;
	 			createKmerIndex(arr, n, k-1, index+1);
	 		}
		}
		
		 /******************************************************
			 * Either read file of frequencies, mRNA sequences or compute from hits
			 */
		private boolean setMode() {	
			bTrainFromFile=false;
			if (isFileName(pCdsFileName)) {
				File f = FileHelpers.getFile(projPath, pCdsFileName);
				if (f==null || !f.exists()) {
					Out.PrtError("Cannot open CDS file '" +  pCdsFileName + "'");
					return false;
				}
				bTrainFromFile=true;
			}
			if (bTrainFromFile) Out.PrtSpMsg(1, "Use file for training");
			return true;
		}
		private void addOne(String key, TreeMap <String, Integer> x) {
			if (x.containsKey(key)) x.put(key, x.get(key)+1);
			else x.put(key, 1);
		}
		
		private TreeMap <String, Integer> baseCntMap =  new TreeMap <String, Integer> ();
		private HashMap <String, Double>  baseFreqMap = new HashMap <String, Double> ();
		private HashMap <String, Double>  trHexLRRmap =   new HashMap <String, Double> ();
		private TreeMap <String, Integer> trHexCntMap =  new TreeMap <String, Integer> ();

		private HashMap <String, Double>  trCodonLNmap = new HashMap <String, Double> ();
		private HashMap <String, Integer> trCodonCntMap = new HashMap <String, Integer> ();
		private long trainCodons=0, trainHex=0;
		private int totalSeqForTrain=0, totalIgnoreSTOPs=0,  cntDup=0;
		
		// CAS334 changed the data structures from a big Hashmap 
		private int kmerIdx=0;
		private HashMap <Integer, Character> ntMap = new HashMap <Integer, Character> ();
		private HashMap <String, Integer> kmerIdxMap = new HashMap <String, Integer> (); // maps Kmer to index
		private ArrayList <String> kmerIdList = new ArrayList <String> (); // seqIDs with kmers
		private ArrayList <int []> kmerCntList = new ArrayList <int []> (); // dim=4**5 of indices
	} // end Train
	/**********************************************************************/
	private boolean isFileName(String file) {
		file = file.trim();
		if (file!=null && !file.equals("") && !file.equals("-1") && !file.equals("-")) return true;
		else return false;
	}
	private PrtFile prtObj = new PrtFile();
	private GcData  gcObj = new GcData();
	private Train   train = new Train();
	private FindORF find = new FindORF();
	private Markov scoreObj;
	
	private SeqData [] seqData;
	private SeqData curSeqObj;
	private ArrayList <OrfData> thisFrameORFs = new ArrayList <OrfData>(); // ORFs for current frame
	
	private ArrayList <OrfData> bestPerFrameORFs = new ArrayList <OrfData>();   // 6 frame ORF for curSeqObj
	private ArrayList <String> saveORFsForDB = new ArrayList <String> (); // 6xN ORFs for N sequences
	
	private DBConn mDB = null;
	private String projPath=null;
	private int totalLength=0;
	
	// the indexes of these two go together for printing the Codon Usage table
	final private String [] codons = 
		{"ttt", "ttc","tta","ttg",   "ctt","ctc","cta","ctg",   "att", "atc", "ata","atg",
		"gtt", "gtc","gta","gtg",    "tct", "tcc","tca","tcg",  "agt", "agc",
		"cct", "ccc","cca","ccg",    "act", "acc","aca","acg",
		"gct", "gcc","gca","gcg",    "tat", "tac",   "taa", "tag", "tga",
		"cat", "cac",   "caa", "cag",    "aat", "aac", "aaa", "aag",  
		"gat", "gac", "gaa", "gag",  "tgt", "tgc", "tgg",   	
		"cgt", "cgc","cga","cgg",   "aga", "agg",   "ggt", "ggc","gga","ggg"};	
	final private String [] aa = {
		 "F", "F", "L", "L",  "L", "L","L", "L",   "I", "I", "I",  "M",
		 "V", "V", "V", "V",  "S", "S", "S", "S",  "S", "S",
		 "P", "P", "P", "P",  "T", "T", "T", "T",   
		 "A", "A", "A", "A",  "Y", "Y",             "*", "*", "*",  
		 "H", "H",     "Q", "Q",   "N", "N",  "K", "K",     
		 "D", "D", "E", "E",      "C", "C","W",  		 
		 "R", "R", "R", "R",       "R", "R",     "G", "G", "G", "G"
	};
}
