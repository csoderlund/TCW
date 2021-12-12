package sng.annotator;

/*******************************************************
 * Compute Best Anno, Best Bits and assign flags. 
 * 
 * All seq-hits (pja_db_unitrans_hits) and unique hits (pja_db_unique_hits) have been assigned
 * 
 * Called from DoUniProt, but may also be called from DoUniRm after remove 
 * 
 * Best GO is computed in DoGO since after all Hit files processed,
 * but may be computed again here if DoUniRmDups.
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import sng.database.Globals;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.BestAnno;
import util.methods.ErrorReport;
import util.methods.Out;

public class DoUniAssign {
	private final int COMMIT = 10000; 
	private final double useSP = 0.7;
	private final String SP = Globalx.SP, TR=Globalx.TR, NT=Globalx.NT;
	
	public DoUniAssign(DBConn m) {
		mDB = m;
	}
	/*******************************************************
     * After all hit files are processed and all DBfasta info entered
     * For each contig: 
     * 	set pja_db_unitrans_hits.filtered for each hit
     * 	Set filter counts, PIDov (best annotation), PID (best evalue)
     */
    public boolean Step2_processAllHitsPerSeq(boolean isAAstcwDB, boolean bUseSP, int flank, HashSet<Integer> annoSeqSet) {  
    	this.isAAstcwDB = isAAstcwDB;
    	this.flank = flank;
    	this.annoSeqSet = annoSeqSet;
    	
		int cntPrt=0, cntAll=0, cntTotalDiffFrame=0, cntTotalDiffOrient=0,  countSP=0;
		long startTime = Out.getTime();
    	Out.PrtSpMsg(2,"Process all hits for " + annoSeqSet.size() + " sequences ");
    	
    	LineParser lp = new LineParser();
    	SeqData ctgData = new SeqData ();
    	ArrayList <HitData> hitList;
    	ArrayList <Olap> olapList = new ArrayList <Olap> ();
        ArrayList <String> geneSet = new ArrayList <String> ();
        ArrayList <String> specSet = new ArrayList <String> ();
        ArrayList <String> annoSet = new ArrayList <String> (); 
    	
		try {
			mDB.renew();

		/** go through all contigs that have annoDB hits */
			for (int seqID : annoSeqSet) { // seqs with DB hits   
    			ctgData.seqID=seqID;	
    			cntPrt++; cntAll++;
    			if(cntPrt==COMMIT) {
    				Out.r("Processed sequences " + cntAll); 
    				cntPrt=0;
    			}
    			
        		int saveDBID=-1;
				int cntTop=0, cntSwiss=0, cntTrembl=0, cntNT=0, cntAA=0;
				int cntDiffFrame=0, cntDiffOrient=0;
			
				HitData bestAnno = null, bestBit = null, bestSP = null, firstAA = null;
  			    
  			    hitList = s2_loadHitDataForSeq(seqID);
  			    if (hitList==null || hitList.size()==0) continue; 
  			    Collections.sort(hitList);
				  				
		/** XXX go through all hits for the current seq -- sorted on bitscore, goodDesc, and rank */
    			for (int h=0; h < hitList.size(); h++) {
    				HitData hitData = hitList.get(h);
    			    olapList.add(new Olap(hitData));
    			    
    			/* get best description and best hit */
    			    String hitType = hitData.dbtype;
    		       
		    		if (bestBit == null) { 
            			bestBit = hitData;
        			}  
		    		else {
		    			if (bestBit.isAA && hitData.isAA && hitData.frame!=bestBit.frame) cntDiffFrame++;
		    			
		    			if (hitData.frame<0 && bestBit.frame>0) 	 cntDiffOrient++; // CAS331 add for NT hits
		    			else if (hitData.frame>0 && bestBit.frame<0) cntDiffOrient++; 
		    		}
		    		
		    		if (hitData.isGood) {
        		    	if (bestAnno==null && hitData.isAA) {
        		    		bestAnno=hitData;
    		        	}  
        		    	if (bestSP==null && hitData.dbtype.equals(SP)) {// keep bestSP - decide below which to keep
        		    		bestSP=hitData;
    		        	}   
		    		}
		    		if (firstAA==null && hitData.isAA) firstAA=hitData; // special case if Best Eval is NT, and no good AA
		    		
    		     /* compute flags */
    				int dbid = hitData.DBid;
    				if (dbid != saveDBID) {
    					saveDBID = dbid;
    					cntTop=0;
    				}
    				// filter 2 top annoDB - not used. best per annodb uses rank
    				if (cntTop < 3) { 
    					cntTop++;
    					hitData.filter |= 2;
    				}
    				// filter 4 is top hit for species
    				String spec = lp.getSpecies(hitData.species);
    				if (spec != null && !specSet.contains(spec)) {
    					specSet.add(spec);
    					hitData.filter |= 4;
    				}
    				// filter 8 is top hit for unique description
    				String gene = lp.getUniqueDesc(hitData.desc);
    				if (gene != null && !geneSet.contains(gene)) {
    					geneSet.add(gene);
    					hitData.filter |= 8;
    				}
        			   			
    				if (hitType.equals(SP)) 		cntSwiss++;
    				else if (hitType.equals(TR)) 	cntTrembl++;
    				else if (hitData.isAA) 			cntAA++; 
    				else 							cntNT++;
    				String anno = hitData.dbtaxo + hitData.dbtype;
    				if (!annoSet.contains(anno)) annoSet.add(anno);
    			} 
        	/* end loop through hits for this contig */
        		
        	// XXX 
    			if (bestBit!=null) {
        			ctgData.pFrame=bestBit.frame;
        			
        			if (bestAnno==null) {// there is no isGood
        				if (!bestBit.isAA && firstAA!=null) bestAnno=firstAA; // regardless of E-value; need for ORF finding
        				else bestAnno=bestBit; 
        			}
        			else { 
        				/** CAS317 any anno could be interesting
	        			if (bestAnno!=bestBit && bestBit.isAA) {
		    				double bDiff = bestBit.bitScore * useAN;
			    			if (bestAnno.bitScore < bDiff) bestAnno=bestBit; 
	        			}
	        			**/
	        			if (bUseSP) { // use SP if not much worse than BestAnno
	    					if (bestSP!=null && bestAnno!=bestSP) {
	    		    			double aDiff = bestAnno.bitScore * useSP; 
	    		    			
	    		    			if (bestSP.bitScore >= aDiff) {
	    		    				bestAnno=bestSP; 
	    		    				countSP++;
	    		    			}
	        				}
	    				}
        			}	
    			}
    		       	      			
    			// rank is based on the order of input from the hit file
    			// the bestAnno and bestEval are set to rank 1 so show in the view contigs
    			bestAnno.filter = bestAnno.filter | 32;
    			bestAnno.rank = 1; 
    			
    			bestBit.filter = bestBit.filter | 16;
    			bestBit.rank = 1; 
    			
		       	ctgData.annoPID = bestAnno.Pid;
		       	ctgData.evalPID = bestBit.Pid;
		       	ctgData.bestmatchid = bestBit.hitName;
    			
    			ctgData.cnt_gene = 		geneSet.size();
    			ctgData.cnt_species = 	specSet.size();
    			ctgData.cnt_overlap = 	s2_setFilterOlap(olapList);
    			ctgData.cnt_annodb = 	annoSet.size();
    			ctgData.cnt_swiss = 	cntSwiss;
    			ctgData.cnt_trembl = 	cntTrembl;
    			ctgData.cnt_nt = 		cntNT;
    			ctgData.cnt_pr = 		cntAA;    
    			
    			if (cntDiffFrame>0) {
    				ctgData.remark = Globals.RMK_MultiFrame;
    				cntTotalDiffFrame++;
    			} 
    			else ctgData.remark="";
    			if (cntDiffOrient>0) cntTotalDiffOrient++;
    			
    			// save annoDB filters as to whether it is a best hit, etc
    			s2_saveDBhitFilterForSeqHits(hitList);
    			
    			// saves seq filters, PID, PIDov, and compute protein ORF    	       	
	    		 s2_saveDBHitCntsForSeq(ctgData);
    			
    			geneSet.clear(); specSet.clear(); olapList.clear(); 
    			annoSet.clear(); hitList.clear();
    		}// end loop through contig list
        		
    		// finished setting all filters   CAS303 changed rank to best_rank for mySQL v8
			// CAS317 added +gobest - add anno and GOs, then other DBs - GOs still good, but not this
    		mDB.executeUpdate("update pja_db_unitrans_hits set best_rank=(filter_best+filter_ovbest+filter_gobest)");
    		
    		System.err.print("                                                           \r");
    		Out.PrtSpCntMsgZero(2, countSP, "replaced bestAnno with best SwissProt");
    		Out.PrtSpCntMsgZero(2, cntTotalDiffFrame, "Sequences with hits to multiple frames ");
    		Out.PrtSpCntMsgZero(2, cntTotalDiffOrient, "Sequences with hits to different orientations ");
    		Out.PrtSpMsgTimeMem(2, "Finish filter", startTime);
    		return true;
		}
        catch ( Exception e ) {
			ErrorReport.reportError(e, "Annotator - computing filters for DB hits");
			return false;
        }
    }

    private int s2_setFilterOlap(ArrayList <Olap> olapList) {
		for (int h=0; h < olapList.size(); h++) {
		   Olap o1 = olapList.get(h);
		   if (o1.isOlap==false) continue;
		   
		   int comp1 = (o1.start > o1.end) ? 1 : 0;
		
		   for (int j=h+1; j< olapList.size(); j++) {
			   Olap o2 = olapList.get(j);
			   if (o2.isOlap==false) continue;
			   
			   int comp2 = (o2.start > o2.end) ? 1 : 0;
			   if (comp1!=comp2) continue;
			   if (o1.frame!=o2.frame) continue; 
			   
			   // o1 is better eval than o2 so if same coords, make o2 contained
			   if      (s2_isContained(comp1, o2, o1)) o2.isOlap=false;
			   else if (s2_isContained(comp1, o1, o2)) {o1.isOlap=false; break;}
		   }
		}
		// change filter
		int cnt=0;
	   	for (int h=0; h < olapList.size(); h++) {
		   Olap o = olapList.get(h);
		   if (o.isOlap) {
			   int f = o.hitData.filter;
			   f |= 1;
			   o.hitData.filter = f;
			   cnt++;
		   }
	   	}
	   	return cnt;
    }
 // is o1 contained in o2 
 	private boolean s2_isContained(int comp, Olap o1, Olap o2) {
 		if (comp==0 && o1.start >= o2.start-flank && o1.end <= o2.end+flank) 
 			return true;
 		if (comp==1 && o1.end   >= o2.end-flank   && o1.start <= o2.start+flank) 
 			return true;
 		return false;
 	}
    private void s2_saveDBhitFilterForSeqHits(	ArrayList <HitData> hitList) {
		try {  
			PreparedStatement ps = mDB.prepareStatement(
    				"UPDATE pja_db_unitrans_hits SET " +
					" filtered = ?,filter_best = ?, filter_ovbest = ?," +
					" filter_olap = ?, filter_top3 = ?, filter_species = ?," +
					" filter_gene = ? WHERE PID = ?");
			mDB.openTransaction(); 
    		for (int h=0; h < hitList.size(); h++) {
    			HitData hitData = hitList.get(h);
    			
    			int best=0, bestov=0, olap = 0, top3=0, species=0, gene=0;
        		int filtered = hitData.filter;
        		if ((filtered & 16) != 0) best  = 1;
        		if ((filtered & 32) != 0) bestov  = 1;
        		if ((filtered & 1) != 0)  olap  = 1;
        		if ((filtered & 2) != 0)  top3  = 1;
        		if ((filtered & 4) != 0)  species  = 1;
        		if ((filtered & 8) != 0)  gene  = 1;
    	
        		ps.setInt(1, filtered);
        		ps.setInt(2, best);
        		ps.setInt(3, bestov);
        		ps.setInt(4, olap);
        		ps.setInt(5, top3);
        		ps.setInt(6, species);
        		ps.setInt(7, gene);
        		ps.setInt(8, hitData.Pid);
        		ps.execute();
    		}
    		ps.close();
    		mDB.closeTransaction(); 
       	}
    	catch (Exception e) {ErrorReport.die(e, "Error on sequence hits ");}
    }
    private void s2_saveDBHitCntsForSeq(SeqData ctg) {
		try {   
			PreparedStatement ps = mDB.prepareStatement("UPDATE contig SET " +
    				"  PIDov = ?, PID =?, bestmatchid =?, cnt_overlap = ?, cnt_gene =? " +
    				" , cnt_species =?, cnt_annodb =?, cnt_swiss =?, cnt_trembl = ?" +
    				", cnt_nt = ?, cnt_gi =?, p_frame =?, notes =? WHERE CTGid = ? ");
			
			ps.setInt(1, ctg.annoPID);
			ps.setInt(2, ctg.evalPID);
			ps.setString(3, ctg.bestmatchid);
			ps.setInt(4, ctg.cnt_overlap);
			ps.setInt(5, ctg.cnt_gene);
			ps.setInt(6, ctg.cnt_species);
			ps.setInt(7, ctg.cnt_annodb);
			ps.setInt(8, ctg.cnt_swiss);
			ps.setInt(9, ctg.cnt_trembl);
			ps.setInt(10, ctg.cnt_nt);
			ps.setInt(11, ctg.cnt_pr);
			ps.setInt(12, ctg.pFrame);
			ps.setString(13, ctg.remark);
			ps.setInt(14, ctg.seqID);
	        ps.execute();
	        ps.close();
       	}
		catch (Exception e) {ErrorReport.die(e, "Error on save DB hit cnts for sequence ");} 
    }
    
    /******************************************************
     * Called at end of adding all Hits.
     */
    public boolean Step3_saveSpeciesSQLTable() {	
		try {
			if (annoSeqSet.size() == 0) return true;
			
			System.err.println();
	 		Out.PrtSpMsg(2, "Creating species table");
			long t = Out.getTime();
			
			int nSpe = mDB.executeCount("SELECT COUNT(*) FROM pja_db_species");
			if (nSpe>0) { 
				 mDB.executeUpdate("update assem_msg set pja_msg = NULL where AID = 1");
		         mDB.tableDelete("pja_db_species");
			}
			int nSeqs = mDB.executeCount("SELECT COUNT(*) FROM contig")+1;
			int minSeq = mDB.executeCount("SELECT min(CTGID) FROM contig");
			int maxSeq = mDB.executeCount("SELECT max(CTGID) FROM contig");
			
    	    HashMap <String, Species> speciesMap = new HashMap <String, Species> ();
    	    
    	    int maxDB = mDB.executeCount("SELECT COUNT(*) FROM pja_databases")+1;
    	    int dbEval[] = new int [maxDB];
    	    int dbAnno[] = new int [maxDB];
    	    int dbOnly[] = new int [maxDB];
    	    for (int i=0; i<maxDB; i++) 
    	    		dbEval[i]=dbAnno[i]=dbOnly[i]=0;
    	    
	    	LineParser lp = new LineParser();
	    	long cnt=0;
	    	int  cntPrt=0, lastDB=-1, cntCtgDB=0;
    	
	    	Out.PrtSpMsg(3, "Read species per sequence from database");
	   
	    	ResultSet rs=null;
	    	String sql = "select " +
	    			" uh.DBID,  uh.species,  sh.filter_best, sh.filter_ovbest " +
	    			" from pja_db_unitrans_hits as sh" +
	    			" join pja_db_unique_hits   as uh " +
	    			" WHERE sh.DUHID = uh.DUHID ";			
	    	    
	    	for (int ctgid=minSeq, cntid=0; ctgid<=maxSeq; ctgid++, cntid++) {
    	    	rs = mDB.executeQuery(sql + " and CTGID=" + ctgid + " order by uh.DBID"); 
	    		cntPrt++; 
	    		if (cntPrt == COMMIT) {
	    			Out.rp(cnt + " hits, processed sequences ", cntid, nSeqs);
	    			cntPrt=0;
	    		}
	    		lastDB = -1; 
    	    		
	    		while (rs.next()) {
	    			cnt++;
	    			int dbid = rs.getInt(1);     // database index
	    			String spec = lp.getSpecies(rs.getString(2));
	    			int bestEval = rs.getInt(3);
	    			int bestAnno = rs.getInt(4);	
	    			
	    			if (dbid > maxDB) {
	    				System.err.println("More than " + maxDB + " databases (" + dbid + ")");
	    				ErrorReport.die("Email tcw@agcol.arizona.edu");
	    			}
	    	
	    			if (lastDB!=dbid) cntCtgDB++;
	    			lastDB = dbid; 
	    		
	    			Species spObj;
	    			if (speciesMap.containsKey(spec)) spObj = speciesMap.get(spec);
	    			else {
	    				spObj = new Species();
	    				speciesMap.put(spec, spObj);
	    			}
	    			spObj.total++;
	    				
	    			if (bestEval==1) {
	    				spObj.bestEval++;
	    				dbEval[dbid]++;
	    			}
	    			if (bestAnno==1) {
	    				spObj.bestAnno++;
	    				dbAnno[dbid]++;
	    			}
	    		} // complete loop for this sequence
	    		
	    		if (cntCtgDB==1 && lastDB != -1) dbOnly[lastDB]++;
	    		cntCtgDB = 0;
	    		lastDB = -1;
    	    } // complete loop through sequences
    		if (rs != null)  rs.close();
    		
    		nSpe = speciesMap.size();
    		Out.PrtSpCntMsg(3, cnt, "total seq-hits                                        ");
    		Out.PrtSpCntMsg(3, nSpe, "total species");
    		
    		Out.PrtSpMsg(3, "Insert species counts into database");
    		mDB.openTransaction();
    		PreparedStatement ps = mDB.prepareStatement("insert pja_db_species set " +
					"AID = 1, species = ?, count = ?, nBestHits = ?,nOVBestHits = ?");
    		cnt=0; cntPrt=0;
    		
    		for ( String spec : speciesMap.keySet() ) {
    			Species spObj = speciesMap.get(spec);
    			cnt++; cntPrt++;
    			if (cntPrt  == COMMIT) {
    				Out.rp("species", cnt, nSpe);
    				cntPrt=0;
    			}
    			ps.setString(1, spec);
    			ps.setInt(2, spObj.total);
    			ps.setInt(3, spObj.bestEval);
    			ps.setInt(4, spObj.bestAnno);
    			ps.execute();
    		}
    		ps.close();
    		mDB.closeTransaction();
    		
    		// These are only used in Overview 
    		Out.PrtSpMsg(3, "Insert species totals per database");
    		mDB.executeUpdate("update pja_databases set nBestHits=0, nOVBestHits=0, nOnlyDB=0");
    		for (int i=0; i< maxDB; i++) {
    			mDB.executeUpdate("update pja_databases set " +
    					" nBestHits = " +   dbEval[i] + "," +
    					" nOVBestHits = " + dbAnno[i] + "," +
    					" nOnlyDB = " +     dbOnly[i] + 
    					" where DBID = " + i);
    		}
    		Out.PrtSpMsgTimeMem(2, "Finish creating species table", t);
    		return true;
		}
       	catch (Exception e) {
    		ErrorReport.reportError(e, "Error making species tables");
    		return false;
       	}
    }

	/******************************************************
	 * Load data
	 */
    private ArrayList <HitData>s2_loadHitDataForSeq (int CTGid)
	{
        ArrayList <HitData> hitList= new ArrayList <HitData> ();
        try {
        	int seqLen = mDB.executeCount("select consensus_bases from contig where CTGid=" + CTGid);
        	
        	// CAS331 creates list, which is then sorted by HitData.compareTo, so 'order' just helps out...
            String strQ = 	"SELECT " +
            		"uh.DBID, uh.hitID,uh.description, uh.species, uh.dbtype, uh.taxonomy, uh.isProtein," +
            		"sh.PID, sh.e_value, sh.bit_score, sh.ctg_start, sh.ctg_end, sh.blast_rank " +
		    		"FROM pja_db_unique_hits   as uh " +
		    		"JOIN pja_db_unitrans_hits as sh " +
		    		"ON sh.DUHID = uh.DUHID " +
		    		"WHERE sh.CTGID = " + CTGid + " " +
		    		"order by sh.bit_score DESC, sh.e_value ASC, sh.blast_rank ASC"; 
		 
            ResultSet rset = mDB.executeQuery( strQ );
            while( rset.next() ) {	
            	int i=1;
            	HitData hit = new HitData ();
                hit.DBid = 		rset.getInt(i++);
                hit.hitName = 	rset.getString(i++);
                hit.desc = 		rset.getString(i++).trim().toLowerCase();
                hit.species = 	rset.getString(i++).trim().toLowerCase();
                hit.dbtype =  	rset.getString(i++);
                hit.dbtaxo = 	rset.getString(i++);
                hit.isAA = 		rset.getBoolean(i++); // CAS313
                
                hit.Pid = 		rset.getInt(i++);
                hit.eVal = 		rset.getDouble(i++); 
                hit.bitScore = 	rset.getDouble(i++);
		        hit.ctgStart = 	rset.getInt(i++);
		        hit.ctgEnd = 	rset.getInt(i++); 
		        hit.rank = 		rset.getInt(i++);  
		        
		        hit.isGood = BestAnno.descIsGood(hit.desc); // CAS305 moved to BestAnno
		        if (hit.isGood && hit.dbtype.equals(NT)) hit.isGood=false; 
		        /******************************************************
		         * RCOORDS
		         */
		        int orient = 1;
				if (hit.ctgEnd <= hit.ctgStart) { // sequence will be reverse complemented before ORF finding
					hit.ctgStart = seqLen - hit.ctgStart +1;
					hit.ctgEnd = seqLen - hit.ctgEnd + 1;
					orient = -1;
				}
				if (!isAAstcwDB && hit.isAA) { // CAS313 check isAA
					hit.frame = hit.ctgStart % 3;
					if (hit.frame==0) hit.frame = 3;
					if (orient<0) hit.frame = -hit.frame;
				}
				else hit.frame=0;
				
				if (hit.dbtype.equals(SP)) hit.isSP=true; // CAS317
				else if (hit.dbtype.equals(TR)) hit.isTR=true; 
				
		        hitList.add(hit);
	    	}
            if ( rset != null ) rset.close();
            if (hitList.size()==0) return null; // happens with mismatch of .fasta and .tab hit file
	    	return hitList;
        }
        catch(Exception e) {
        	ErrorReport.prtReport(e, "Reading database for hit data");
        	return null;
        }	
	}
    private class HitData implements Comparable <HitData> {
		 String desc, species, dbtype, dbtaxo;
		 double eVal, bitScore;
	     int DBid, Pid;
	     String hitName;
	     int ctgStart, ctgEnd, frame;
	     int filter=0, rank=0;
	     boolean isAA=true;
	     boolean isGood=true;
	 	 boolean isSP=false, isTR=false;
	     
	     public int compareTo(HitData b) { // CAS317 changed
	    	if (this.bitScore > b.bitScore) return -1; 
	    	if (this.bitScore < b.bitScore) return  1;
	    	
	 		if ( this.isSP && !b.isSP) return -1;
	 		if (!this.isSP &&  b.isSP) return  1;
	 		
	 		if ( this.isTR && !b.isTR) return -1;
	 		if (!this.isTR &&  b.isTR) return  1;
	 		
	 		if (this.eVal < b.eVal) return -1;
	 		if (this.eVal > b.eVal) return  1;
	 		
			if ( this.isGood && !b.isGood) return -1;
			if (!this.isGood &&  b.isGood) return  1;	
			
			if (this.rank < b.rank) return -1; // CAS331 had the check backwards
	    	if (this.rank > b.rank) return  1;
   		
   		return 0;
		}
	 }
    private class Species {
		int total = 0;
	    int bestEval = 0;
	    int bestAnno = 0;
	}
	private class Olap {
		Olap (HitData h) {
			start = h.ctgStart;
			end = h.ctgEnd;
			frame = h.frame;
			hitData = h;
		}
		int start, end, frame;
		HitData hitData = null;
		boolean isOlap = true;
	}
	
	private class SeqData {
  		int seqID, annoPID, evalPID;

  		String bestmatchid, remark="";
  		int cnt_overlap, cnt_gene, cnt_species, cnt_annodb; 
  		int cnt_swiss, cnt_trembl, cnt_nt, cnt_pr;
  		int pFrame=0;
    }
	private int flank;
	private HashSet<Integer> annoSeqSet; // sequences with annotation
    private boolean isAAstcwDB=false;
	private DBConn mDB;
}
