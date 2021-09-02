package sng.annotator;
/**
* Processes the results of blasting a transcript set against annoDBs
* 1. Go through all blast files. Add all hits to database in unitrans_hits table
* 2. For each newly annotated contig, extract only its hits from database and set filters and best hits
**/

import java.io.BufferedReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;

import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import util.database.DBConn;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.BestAnno;

public class DoUniProt 
{
	private boolean bUseSP = false; // Best Anno only replaces 'uncharcterized'unless true
	private boolean bRmECO = true;  // Remove {ECO... string
	
	private final int COMMIT = 10000; 
	private final int maxHitSeq = 32000;
	private final String badHitFile = BlastHitData.badHits;
	
	public DoUniProt () {}

	public void setMainObj(CoreAnno cObj, DoBlast bObj, DBConn d) {
		hitObj = bObj;
		mDB = d;
	}
	
	 /**
     * called from CoreAnno.run to annotate all sequences with hits
     * 1. updates pja_db_unitrans_hits with hits that pass sTCW.cfg filters
     * 2. updates pja_db_unique_hits with new hits and/or new annotation
     * 		that is, even if no hits are found in the hit file, 
     * 		the fasta file will still be read for annotation
     * 		this is for the case where it was not done to start with
     * 3. update pja_database by adding the given DB
     **/
	public boolean processAllDBhitFiles(boolean isAA, TreeMap<String, Integer> seqs) {
		if (bUseSP) Out.PrtSpMsg(1, "Best Anno - SwissProt preference");
		if (bRmECO) Out.PrtSpMsg(1, "Remove {ECO...} from UniProt descripts");
		else Out.PrtSpMsg(1, "Do NOT remove {ECO...} from UniProt descripts");
		Out.PrtSpMsg(1,"");
		
	 	long totalTime = Out.getTime();
	 	seqMap  = seqs;
	 	isAAstcwDB = isAA;
	 	
	 	init();
		if (!pRC) return false;  
		
	 	// Annotated sequences are added to annoSeq set and entered into database
	 	Step1_processAllHitFiles();
	 	if (!pRC) return false;  
	 	
	 	Out.PrtSpCntMsgZero(1, nTotalBadHits, "hits ignored -- see " + badHitFile);
	 	Out.PrtSpCntMsgZero(1, notFoundSeq.size(), "not found in database  -- see file " + badHitFile);
	 	Out.PrtSpCntMsgZero(1, nSeqTooLong, "sequence > " + maxHitSeq + ": truncated -- see file " + badHitFile);
	 	Out.PrtSpCntMsgZero(1, LineParser.badSpeciesLen, "hits with species length> " + 
	 				LineParser.maxSpeciesLen + ": truncated -- see file " + badHitFile);
	 	Out.PrtSpCntMsgZero(1, LineParser.badDescriptLen, "hits with description length> " + 
	 				LineParser.maxDescriptLen + ": truncated -- see file " + badHitFile);
	 	
	 	
	     // CAS331 - UniPrune calls Step2&Step3 so it can run stand-alone from command line
	 	if (pruneType==1 || pruneType==2) {
	 		new DoUniPrune(mDB, godbName, isAAstcwDB, bUseSP, flank, pruneType, false, 0); 
	 	}
	 	else {
	 		DoUniAssign assignObj = new DoUniAssign(mDB);
	 		 
			// calculate filters
		 	pRC = assignObj.Step2_processAllHitsPerSeq(isAAstcwDB, bUseSP, flank, annoSeqSet);
		 	if (!pRC) return false;  
		 	
		 	// final things
		 	pRC = assignObj.Step3_saveSpeciesSQLTable();
		 	if (!pRC) return false;  
	 	}
	 	
	 	Out.PrtSpMsgTime(1, 
	 			"Finished " + annoSeqSet.size() + " annotated  " + (seqMap.size()-annoSeqSet.size()) + " unannotated",
	 			totalTime);
	 	Out.PrtSpMsg(1," ");
	 	cleanup();
	 	
	 	return pRC; 
	}
	private void init() { // CAS326
		try {
			if (mDB.tableColumnExists("assem_msg", "anno_msg"))
		 		mDB.executeUpdate("update assem_msg set anno_msg=''");
		}
		catch (Exception e) {
			pRC = false;
			ErrorReport.prtReport(e, "Processing annoDB hit files");
		}
	}
	/**********************************************************
	 * Read all files, add hits, annoDB and unique hit information to db
	 */
    private int Step1_processAllHitFiles() {	   
	    int total = 0;
	    int nFiles = hitObj.numDB();
    	
		try {
			Out.PrtSpMsg(1, "Annotate sequences with sequence hits from " + nFiles + " DB file(s)");
			hitsInDB = s1_loadUniqueHitSet();
	    	
	    	for (int i=0; i< nFiles; i++) { 
	    		long totalTime = Out.getTime();
	    		
	    		// Run search if necessary or get existing hit file
	    		String hitFile = hitObj.getRunDbBlastFile(i); 
	    		if (hitFile==null) { // don't continue if something goes wrong
	    			pRC = false;
	    			return total; 
	    		}
	    		if (hitFile.equals("-"))  continue;
	    		
	    		dbType = hitObj.getDBtype(i);
	    		dbNum = hitObj.getDbNum(i);
	    		isAAannoDB = hitObj.isProtein(i);
	    		dbTaxo = hitObj.getDBtaxo(i); 
			    		
		/** 1. Add to pja_unitran_hits and contig SQL tables from blast file **/
				Step1a_processHitFile(i, hitFile);   				
		    	
				if (nAnnoSeq==0 && nTotalHits==0) continue;
				
		/** 2. Add to pja_database SQL table **/
				// 	must come before adding unique hits so the DBID is entered into mysql
				String db = hitObj.getDBfastaFile(i);
				if (db.equals("-")) db = hitFile;
				String date = hitObj.getDBdate(i);
				DBID = Step1b_saveAnnoDB(db, isAAannoDB, dbType, dbTaxo, nAnnoSeq, hitsAddToDB.size(), 
						nTotalHits, date, hitObj.getDBparams(i));
				
		/** 3.  Add to pja_unique_hits the sequence and header info from DB fasta file **/
				if (db.equals("-")) 
						Step1c_addUniqueNoFastaFile(i);
				else 	
						Step1c_addUniqueFromFastaFile(i);
				
		    	hitsAddToDB.clear();
		    	
		    	total += nAnnoSeq;
		    	nAnnoSeq = 0;
		    	Out.PrtSpMsgTime(2, "Complete adding DB#" + dbNum, totalTime);
		    	Out.PrtSpMsg(0,"");
	    	}
	    	return total;
		}
		catch (Exception e) {
			pRC = false;
			ErrorReport.prtReport(e, "Processing annoDB hit files");
			return 0;
		}
    }
   
    /*************************************************
  	* Read hit File 
  	* 	call processHitDataForContig to update pja_unitrans_hits  
	*************************************************************/
    private boolean Step1a_processHitFile(int ix, String hitFile ) 
    {     	
       	BlastHitData hitData = null;
       	int nHitNum=0, cntUniqueExists=0, noSeq=0, nTotalDups=0;
       	String curSeqName="", curHitName="";
       	nAnnoSeq = nTotalHits = 0;
    	long time = Out.getTime();
			
    	String file = hitFile.toString();
    	file = file.substring(file.lastIndexOf("/")+1);
    	Out.PrtSpMsg(2,"DB#" + dbNum + " hits: " + file);
		
    	BufferedReader reader = null;
    	String line="";
    	try {
    		mDB.renew(); // Creates a new database connection -- it was timing out...
    		reader = FileHelpers.openGZIP(hitFile);
    		if (reader==null) return false;
    		
    		LineParser lpObj = new LineParser(); // Gets seqID
    		int cntHits=0, cntPrt=0;
    		
    		while ((line = reader.readLine()) != null) {
    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue;  			
		    	nHitNum++; cntPrt++;
		        if (cntPrt == COMMIT) {     
		        	cntPrt=0;
		          	Out.r("      Annotated sequences " + nAnnoSeq + ", total hits " + 
		          		nTotalHits + ", HSPs " + nHitNum);
		        }	   
    			String[] tokens = line.split("\t");
    			if (tokens == null || tokens.length < 11) continue;
    			
	    		String newSeqName = tokens[0];
				if (!curSeqName.equals(newSeqName)) { // new sequence
					
					if (! curSeqName.equals("")) { // process previous
						if (curHitDataForSeq.size() > 0) {// may have had bad hit
							s1_saveHitDataForSeq(); 
							curHitDataForSeq.clear();
						}
					}
					
					if (!seqMap.containsKey(newSeqName)) {
						if (!notFoundSeq.contains(newSeqName)) {
							BlastHitData.printWarning("Sequence " + newSeqName + " in hit file but not in database");
			    				notFoundSeq.add(newSeqName); 
						}
		        			continue;
					}
					curSeqName = newSeqName;					
			        curSeqData.clearAnno();
			        curSeqData = CoreDB.loadContigData(mDB, curSeqName); 
			        if (curSeqData!=null) { 
			        	curSeqData.setContigID(curSeqName); 
			        	curSeqData.setCTGID(seqMap.get(curSeqName));
			        }
			        else {
			        	if (noSeq==0) Out.PrtError("No sequence '" + curSeqName + "' in database; no further such error messages will be printed");
						noSeq++;
			        }
			        cntHits=0;
			        curHitName="";
				}
				
				if (!lpObj.parseLine(line)) {
					Out.PrtError("Error parsing: " + line);
					continue;
				}
				String newHitName = lpObj.getHitID();
				if (curHitName.equals(newHitName)) continue;
				curHitName = newHitName;
				
				cntHits++;
				//if (cntHits>maxHitsPerAnno) { // CAS331 allow user to control
				//	continue;
				//}
		    	// create list of hits for current contig
		    	hitData = new BlastHitData (isAAannoDB, line);
		    	
		    	// if HIT id already in database, then this is being run again
		    	if (hitsInDB.contains(hitData.getHitID())) {
		    		cntUniqueExists++;
		    		continue;
		    	}
		    	// hit is past the 32k limit so no use saving
		    	if (hitData.badHitData(maxHitSeq, minBitScore)) {
		    		nTotalBadHits++;
		    		continue;
		    	}
		    	
		    	hitData.setCTGID(curSeqData.getCTGID());	   
		    	curHitDataForSeq.add(hitData);	
	    	}  // end loop through hit tab file
    		
	    	//-----------------------------------------------------------//
	    	if (! curSeqName.equals("")) { // process last contig
	    		if (curHitDataForSeq.size() > 0) {// may have had bad hit
					s1_saveHitDataForSeq(); 
					curHitDataForSeq.clear();
				}
	    	}
	    	if ( reader != null ) reader.close();
		
			System.err.print("                                                                         \r");
			
			if (cntUniqueExists > 0) 
				Out.PrtWarn(cntUniqueExists + " DB ids already existed in sTCW -- ignored ");
	
			if (nHitNum==0) 
				Out.PrtSpMsgTime(3, "NO HIT RESULTS", time);
			else  {
				Out.PrtSpCntMsgZero(3, nTotalDups, "duplicate alignment ");
				Out.PrtSpCntMsgZero(3, nTotalHits, "seq-hit pairs");
				Out.PrtSpCntMsgTimeMem(3,nAnnoSeq, "annotated sequences", time); 	
			}
			 return true;
	    }
        catch ( Exception e ) {
        	pRC=false;
			ErrorReport.reportError(e, "Annotator - reading DB hit file\nLine: " + line);
			return false;
        }       
    }
 
    // gathered all hits in hitDataForCtg per sequence per annoDB. Save each hit. 
    private void s1_saveHitDataForSeq() {
	    try {
	    	int seqLen = curSeqData.getConsensusBases();
				
	    	Collections.sort(curHitDataForSeq); // CAS317 was blast (eval/bitsore) order, make TCW (bitscore/eval) order
	    	
	    	PreparedStatement ps = mDB.prepareStatement("INSERT INTO pja_db_unitrans_hits " + 
			  "(CTGID, AID, DUHID, contigid, uniprot_id, percent_id, alignment_len," +
			  "mismatches, gap_open, ctg_start, ctg_end, prot_start, prot_end, " +
			  "bit_score , e_value, dbtype, taxonomy, blast_rank, isProtein, ctg_cov, prot_cov) " +
			  "VALUES (?,1,0,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, ?,?)");
	    		mDB.openTransaction(); 
	    		
       		for (int i=0; i<curHitDataForSeq.size(); i++) {
       			BlastHitData hitData = curHitDataForSeq.get(i);
       			hitData.setBlastRank ( i+1 );
       			hitData.setDBtaxo(dbTaxo);
    			
       			String type = hitData.getDBtype();
	    		if (type.equals("")) type = dbType; 
	    		String b = (hitData.isAAhit()) ? "1" : "0";
	    		int sStart = hitData.getCtgStart(), sEnd=hitData.getCtgEnd();
	    		int seqCov = Static.percent(Math.abs(sStart-sEnd)+1, seqLen);
	    		int hStart = hitData.getHitStart(), hEnd=hitData.getHitEnd();
	    		int hitCov = 0; // do not have hit length until loaded
		    		
			    ps.setInt(1, hitData.getCTGID());
			    ps.setString(2,hitData.getContigID());
			    ps.setString(3,hitData.getHitID());
			    ps.setDouble(4,hitData.getPercentID());
			    ps.setInt(5, hitData.getAlignLen());
			    ps.setInt(6, hitData.getMisMatches());
			    ps.setInt(7, hitData.getGapOpen());
			    ps.setInt(8, sStart);
			    ps.setInt(9, sEnd);
			    ps.setInt(10, hStart);
			    ps.setInt(11, hEnd);
			    ps.setDouble(12, hitData.getBitScore());
			    ps.setDouble(13,hitData.getEVal());
			    ps.setString(14, type);
			    ps.setString(15, hitData.getDBtaxo());
			    ps.setInt(16, hitData.getBlastRank());
			    ps.setString(17, b);
			    ps.setInt(18, seqCov);
			    ps.setInt(19, hitCov);
			    ps.execute();
	       
		       // the pid gets updated in the contig record in the calling routine
		       int pid = mDB.lastID();
		       hitData.setPID(pid);
	       
       			String p = hitData.getHitID();
       			if (!hitsAddToDB.contains(p)) hitsAddToDB.add(p);
       			
				nTotalHits++;
       		}
       		ps.close();
       		mDB.closeTransaction();
       		
       		nAnnoSeq++;
       		// CAS304
       		String name = curSeqData.getContigID();
       		int id = seqMap.get(name);
		    if (!annoSeqSet.contains(id)) annoSeqSet.add(id);
	    }
        catch ( Exception err ) {
        	pRC=false;
			ErrorReport.reportError(err, "Annotator - processing hit data");
        }
    }
    /************************************************************
     * pja_database update
     */
    private int Step1b_saveAnnoDB(String fileName, boolean isProtein, String dbtype, String dbtaxo, 
    		int bestHits, int uniqueHits, int totalHits,  String dbdate,
    		String parameters) throws Exception
    {
	    	String strQ = null;
	    	try {
    		String fdate;
    		
    		if (dbdate==null) fdate = FileHelpers.getFileDate(fileName);
    		else fdate = dbdate;
		
    		if (fdate==null) {
    			Out.PrtError("Not valid file " + fileName);
    			return -1;
    		}
           	int isP = (isProtein) ? 1 : 0;
           	
	    	strQ = "INSERT INTO pja_databases " + 
	           "(AID,  path, isProtein, dbtype, taxonomy, " +
	           "nBestHits, nUniqueHits, nTotalHits, dbDate, addDate, subset, parameters ) " +
	           "VALUES (1, '" + fileName + "', " +
	           		isP + ", '" + dbtype + "', '" + dbtaxo + "', " +
	           		bestHits + ", " + 
	           		uniqueHits + ", " + 
	           		totalHits + ",'" + fdate + "', NOW()," + bUseSP + 
	           		",'" + parameters + "' )";
	        mDB.executeUpdate( strQ );  
	       
	        mDB.executeUpdate("update assem_msg set spAnno=" + bUseSP);
	        return mDB.lastID();
    	}
    	catch (SQLException e) {
    		pRC = false;
    		ErrorReport.reportError(e,"Error on query " + strQ);
			throw e;
    	}   	
    }
   
    /*********************************************************************
     * Populate pja_unique_hits
     ************************************************************/
    private void Step1c_addUniqueNoFastaFile(int ix) 
    {
        int cnt_add=0;
       	long time = Out.getTime();
       
		Out.PrtSpMsg(2,"DB#" + dbNum + " descriptions: no Database Fasta file provided");
    	try {
    		mDB.renew();
	    	while (!hitsAddToDB.isEmpty()) {
	    		String hitID = hitsAddToDB.iterator().next();
	    		
			s1_saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, hitID, "", "", "", "", 0);
			hitsInDB.add(hitID); // so do not get divide by zero error
	    		hitsAddToDB.remove(hitID); 
	    	}
	    	System.err.print("                                                                      \r");
	    	Step1d_updateHitCov();
	    	Out.PrtSpCntMsgTimeMem(3,cnt_add, "unique hits added",time);
    	}
        catch ( Exception err ) {
        		pRC=false;
			ErrorReport.reportError(err, "Annotator - creating unique hit records for hit file w/o description file");
			return;
        }
    }
        
    private void Step1c_addUniqueFromFastaFile(int ix)  
    {
        int cnt_add=0, read=0, failParse=0, cntPrt=0;
        String hitID="", hitSeq = "", line=null;
        LineParser lp = new LineParser();
       	BufferedReader reader = null;
       	long time = Out.getTime();
       	int total = hitsAddToDB.size();
		Out.PrtSpMsg(2,"DB#" + dbNum + " descriptions: " + hitObj.getDBfastaNoPath(ix) );
        try {
    		mDB.renew();
    		boolean addHit = false;
    	
    		reader = FileHelpers.openGZIP(hitObj.getDBfastaFile(ix));
    		if (reader==null) return;
        		
			while((line = reader.readLine()) != null) {	
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#') continue;
				
		// build up sequence 
				if (line.charAt(0) != '>') {
					if (addHit) {
						if (hitSeq.length()+line.length() > maxHitSeq) {
							if (hitSeq.length() == 0) hitSeq = line.substring(0,maxHitSeq-1);  
							BlastHitData.printWarning(lp.getHitID() + " > " + maxHitSeq + "; truncated to " 
											+ hitSeq.length() + " to put in database");
							nSeqTooLong++;
						}
						else hitSeq += line;
					}
					continue;
				}
		// > description line 
					
				// Add previous 
				if (addHit) {
					String desc = (bRmECO) ? BestAnno.rmECO(lp.getDescription()) : lp.getDescription();
					s1_saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, hitID, 
						lp.getOtherID(), desc, lp.getSpecies(), hitSeq, 0);
					cnt_add++;
					
					cntPrt++; // CAS330 moved from end of loop
					if (cntPrt == COMMIT) {	                
				         Out.rp("Read " + read + ", added ", cnt_add, total); 
				         cntPrt=0;
					}
				}
					
				// start the next
				hitSeq = "";	
				if (!lp.parseFasta(line)) {
		        	Out.PrtWarn("Cannot parse line: " + line);
		        	failParse++;
		        	if (failParse>20) Out.die("Too many parse errors");
					addHit = false;
					continue;
				}
				
				// new hit from fast file
				hitID = lp.getHitID();
				
				if (hitsAddToDB.contains(hitID)) {
					addHit = true;
					hitsInDB.add(hitID); 
					hitsAddToDB.remove(hitID); 
				}
				else addHit = false;
				read++; 
			}
			// add last
			if (addHit) { 
				String desc = (bRmECO) ? BestAnno.rmECO(lp.getDescription()) : lp.getDescription(); // CAS305
				s1_saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, lp.getHitID(), lp.getOtherID(), 
					desc, lp.getSpecies(), hitSeq, 0);
				cnt_add++;
			}
			if ( reader != null ) reader.close();

			System.err.print("                                                                      \r");
			
			Step1d_updateHitCov();
			
			if (cnt_add==0) {
				Out.PrtError("The annoDB fasta file does not correspond to the hit tab file -- or the headers lines are weird");
			}
			else if (!hitsAddToDB.isEmpty()) {
				Out.PrtWarn(hitsAddToDB.size() + " HitIds in tab file not found in DB fasta file");
				if (hitsAddToDB.size()>10) Out.PrtSpMsg(1, "Writing first 10....");
				int i=0;
				for (String name : hitsAddToDB) {
					Out.PrtSpMsg(1, "HitID: '" + name + "'");
					i++;
					if (i>10) break;
				}
			}
			String x = String.format("%,d", read);
			Out.PrtSpCntMsgTimeMem(3,cnt_add, "unique hits descriptions added from " + x + "    ",time);
	    }
        catch ( Exception err ) {
        	pRC=false;
			ErrorReport.reportError(err, "Annotator - reading DB Fasta file " + ix);
			return;
        }
    }
    // do not have lengths until after add annoDBs sequences
    private boolean Step1d_updateHitCov() {
    	try {
			Out.r("update hit coverage.....");
			mDB.executeUpdate("update pja_db_unitrans_hits as seq " +
			"inner join pja_db_unique_hits as hit on seq.DUHID = hit.DUHID " +
			"set seq.prot_cov = round( ((abs(seq.prot_end - seq.prot_start)+1)/ hit.length)* 100.0, 0) " +
			"where hit.length>0 and seq.prot_cov=0 and seq.prot_end>seq.prot_start");
			
			// X-Y doesn't work if X<Y, so to both ways; NT-NT can have this happen
			mDB.executeUpdate("update pja_db_unitrans_hits as seq " +
					"inner join pja_db_unique_hits as hit on seq.DUHID = hit.DUHID " +
					"set seq.prot_cov = round( ((abs(seq.prot_start - seq.prot_end)+1)/ hit.length)* 100.0, 0) " +
					"where hit.length>0 and seq.prot_cov=0 and seq.prot_start>seq.prot_end");
			Out.r("                                    ");
			return true;
    	}
    	catch (Exception e) {
			pRC = false;
			ErrorReport.prtReport(e, "updateHitCov");
			return false;
		}
    }
    private void s1_saveDBhitUnique(int DBID, boolean isProtein, String dbtype, String dbtaxo, 
    		String hitID, String otherID, String description, String species, String sequence, int cntUni) 
    			throws Exception
    {    
		try {
    		int DUHID=0;
    		PreparedStatement ps = mDB.prepareStatement("INSERT INTO pja_db_unique_hits set " + 
    			           "AID=1, DBID=?,  hitID=?, repID=?, dbtype=?, taxonomy=?, isProtein=?, " +
    			           "description=?, species=?, length=?, sequence=?");
			// The following situation can make this happen:
    			// 	-v 25 does not necessarily give 25 hits, so one may not be hit in species
    			// 	but hit in all; will get the the species DUHID, but have the ALL taxo.
			if (duidMap.containsKey(hitID)) DUHID = duidMap.get(hitID);
	        else {
	        	// a " occurred in UniProt description which causes an SQL error
	        	if (description.indexOf("\"") != -1) {
	        		String x = description.replace("\"", "");
	        		description = x;
	        	}

		        ps.setInt(1, DBID);
		        ps.setString(2,hitID);
		        ps.setString(3,otherID);
		        ps.setString(4,dbtype);
		        ps.setString(5,dbtaxo);
		        ps.setBoolean(6,isProtein);
		        ps.setString(7,description);
		        ps.setString(8,species);
		        ps.setInt(9,sequence.length());
		        ps.setString(10, sequence);
		        ps.execute();
		       
		        DUHID = mDB.lastID();          
	        }
	        if (DUHID == 0 ) {
	        	Out.PrtError("Internal: could not get DUHID for " + hitID);
	        	return;
	        }
	        // update all unitran records with DUHID
            mDB.executeUpdate("UPDATE pja_db_unitrans_hits SET DUHID=" + DUHID +
            		" WHERE uniprot_id = '" + hitID + "' ");
            duidMap.put(hitID, DUHID);
        }
        catch (Exception e) {
        		ErrorReport.die(e, "Error saving hit description to database");
        }   
    }
    
    private HashSet<String> s1_loadUniqueHitSet() {
    	HashSet <String> hit = new HashSet<String>();
        try {
            ResultSet rset = mDB.executeQuery( "SELECT hitID FROM pja_db_unique_hits " );
 
            while(rset.next()) {
            	hit.add(rset.getString(1));
            }
            rset.close();
            return hit;
        }
        catch (Exception e) {
    		pRC = false;
    		ErrorReport.prtReport(e, "Loading unique hits"); 
    		return null;
    	}   		
    }

    /**************************************************************************
     * Only called from CoreMain
     */
	public void updateAnnoDBParams() {
		try {
           	ResultSet rs;
           	
		 	int nFiles = hitObj.numDB();
			for (int ix=0; ix < nFiles; ix++) {
				hitObj.testDBfastaFileAndSetSearchPgm(ix);
				String pgm = hitObj.getDBparams(ix);
				String tax = hitObj.getDBtaxo(ix);
				String type = hitObj.getDBtype(ix);
				String file = hitObj.getDBfastaFile(ix);
				System.out.println("Updating " + tax + " " + type + " " + file);
				rs = mDB.executeQuery("Select DBID from pja_databases where taxonomy='" + tax +
						"' and dbtype='" + type + "'");
				int DBID=0;
				if (rs.next()) {
					DBID = rs.getInt(1);
					mDB.executeUpdate("Update pja_databases set parameters='" + pgm + "' where DBID=" + DBID);
				}
				else System.out.println("Cannot find DBID");
			}
		}
		catch (Exception e) {
			pRC = false;
			ErrorReport.prtReport(e, "Updating annoDB params");
		}
	}
    /******************* Instance variables ****************************/
	public void setCfgAnnoParams(boolean bRmECO, boolean bUseSP,  
			int flank, int minBitScore, int pruneType, String godbName) {
		this.bRmECO = bRmECO;
		this.bUseSP = bUseSP;
		this.flank = flank;
		this.minBitScore = minBitScore;
		this.pruneType = pruneType;
		this.godbName = godbName; // only necessary to pass on to UniPrune
	}
	public boolean getUseSP() {return bUseSP;} // CAS331 one method for setting, three get's for command line Prune
	public int     getFlank() {return flank;}
	public int     getPrune() {return pruneType;}
	
	private void cleanup() {
		if (hitsAddToDB!=null) hitsAddToDB.clear();
		if (hitsInDB!=null) hitsInDB.clear();
		if (curHitDataForSeq!=null) curHitDataForSeq.clear();
		// if (ctgMap!=null) ctgMap.clear(); - not this one, was passed from caller
		if (annoSeqSet!=null) annoSeqSet.clear();
		if (duidMap!=null) duidMap.clear();
		if (notFoundSeq!=null) notFoundSeq.clear();
	}
	// current file
	private int DBID = 0;
	private int dbNum = 0;
	
	private String dbTaxo = "";
	private String dbType = "";
	private boolean isAAannoDB = true, isAAstcwDB=false;
    
	private HashSet<String> hitsAddToDB = new HashSet<String> (); // unique set in dbFasta
	private HashSet<String> hitsInDB = new HashSet<String> (); // already in sTCW
	
	private ContigData curSeqData = new ContigData ();
   	private ArrayList <BlastHitData> curHitDataForSeq = new ArrayList <BlastHitData> ();
   	
    private TreeMap<String, Integer> seqMap = new TreeMap<String, Integer> (); // sequences in database 
    private HashSet<Integer> annoSeqSet = new HashSet<Integer> (); // sequences with annotation (CAS317 was Map)
    
	private int flank = 0, minBitScore = 0; // neither of these are ever set, but can be
	private HashMap <String, Integer> duidMap = new HashMap <String, Integer> ();
	private int pruneType=0;
	private String godbName=null; // for DoUniPrune
	
	// counts
	private int nAnnoSeq = 0, nTotalHits = 0, nTotalBadHits = 0, nSeqTooLong=0;
	private HashSet <String> notFoundSeq = new HashSet <String> ();
  		
    private DoBlast hitObj = null;				// created in runSingleMain	
    private DBConn mDB = null;
    private boolean pRC = true;
}
