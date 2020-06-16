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

import sng.database.Globals;
import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;
import util.methods.Static;

public class DoUniProt 
{
	static final boolean debug =  false;
	static boolean bUseSP = false; // Best Anno only replaces 'uncharcterized'unless true
	static final int COMMIT = 10000; 
	static final int maxHitSeq = 32000;
	static final String badHitFile = BlastHitData.badHits;
	static final int maxHitsPerAnno = 25;
	static final double useSP = 0.80;
	static final double useEV = 0.10;
	static double DLOW = 1E-308; // 1E-180 for 32-bit - not checked
	
	// bunch of heuristic to figure out non-informative names
	// des is lowercase
	private boolean goodDescript (String des) {
		des = des.replace("_", " ");
		if (des.contains("uncharacterized protein") || 
	    		des.contains("putative uncharacterized") || 
	    		des.contains("hypothetical protein") || 
	    		des.contains("expressed protein") || 
	    		des.contains("predicted protein") ||
	    		des.contains("whole genome shotgun") ||
	    		des.contains("scaffold") || 
	    		des.equals("unknown") || 
	    		des.equals("unk") || 
	    		des.equals("orf"))
	    	{
	    	        return false;
	    	}
		
		int ix = des.indexOf("{ec");  
		if (ix != -1) {
			String s = des.substring(0,ix).trim();
			if (s.length() > 3) des = s;
		}
		String [] words = des.split(" ");
		if (words.length > 2) return true; 

		// many are just a 'name' or 'name protein' or 'name (fragment)' or 
		// If name has lots of numbers, seems to be meaningless
		// Names like MET_1 will pass, names like GK1599 will not
		// Rules: if over 3 digits in first word - name
		//		  if more digits than letters - name
		boolean isName = false;
		int cntDigit=0;
		for (int i=0; i < words[0].length(); i++) {
			char c = words[0].charAt(i);
			if (Character.isDigit(c)) cntDigit++;
		}
		if (cntDigit>3) isName=true;
		else {
			int wl = words[0].length();
			double x = (cntDigit > 0) ? ((double) wl/ (double) cntDigit) : 0;
			if (wl <= 5 && x > 1) isName = true;
		}
		if (words.length == 1 && isName) return false;
		if (isName &&  des.endsWith("protein")) return false;
		if (isName &&  des.endsWith("(fragment)")) return false;
		if (isName &&  des.endsWith("partial")) return false; 
		return true;
	}
	
	public DoUniProt () {}

	public void setMainObj(CoreAnno a, DoBlast b, DBConn d) {
		hitObj = b;
		mDB = d;
	}
	
	 /**
     * called from CoreAnno.run to annotate all contigs with protein or DNA
     * 1. updates pja_db_unitrans_hits with hits that pass sTCW.cfg filters
     * 2. updates pja_db_unique_hits with new hits and/or new annotation
     * 		that is, even if no hits are found in the hit file, 
     * 		the fasta file will still be read for annotation
     * 		this is for the case where it was not done to start with
     * 3. update pja_database by adding the given DB
     * 
     **/
	public boolean processAllDBhitFiles(boolean isAA, TreeMap<String, Integer> seqs) 
	{
	 	long totalTime = Out.getTime();
	 	seqMap  = seqs;
	 	isAAstcwDB = isAA;
	 	
	 	// Annotated sequences are added to annoSeq set and entered into database
	 	Step1_processAllHitFiles();
	 	if (!pRC) return false;  
	 	
		// go through annoSeq set and calculate the filters and enter into database
	 	Step2_processAllHitsPerSeq();
	 	if (!pRC) return false;  
	 	
	 	// final things
	 	if (annoSeqMap.size() > 0) {
	 		CoreDB.updateAnnoVer(mDB);
	 		Step3_saveSpeciesSQLTable();
	 	}
	 	if (!pRC) return false;  
	 	
	 	Out.Print("");
	 	if (nTotalBadHits > 0) 
	 		Out.PrtSpMsg(2, nTotalBadHits + " hits ignored -- see " + badHitFile);
	 	if (notFoundSeq.size()>0)
	 		Out.PrtSpMsg(2, notFoundSeq.size() + " not found in database  -- see file " + badHitFile);
	 	if (nSeqTooLong>0)
	 		Out.PrtSpMsg(2, nSeqTooLong + " sequence > " + 
	 				maxHitSeq + ": truncated -- see file " + badHitFile);
	 	if (LineParser.badSpeciesLen>0)
	 		Out.PrtSpMsg(2, LineParser.badSpeciesLen + " hits with species length> " + 
	 				LineParser.maxSpeciesLen + ": truncated -- see file " + badHitFile);
	 	if (LineParser.badDescriptLen>0)
	 		Out.PrtSpMsg(2, LineParser.badDescriptLen + " hits with description length> " + 
	 				LineParser.maxDescriptLen + ": truncated -- see file " + badHitFile);
	 	
	 	Out.PrtSpMsgTime(1, 
	 			"Finished " + annoSeqMap.size() + " annotated  " + (seqMap.size()-annoSeqMap.size()) + " unannotated",
	 			totalTime);
	 	Out.PrtSpMsg(1," ");
	 	cleanup();
	 	
	 	return pRC; 
	}
	
	/**********************************************************
	 * Read all files, add hits, annoDB and unique hit information to db
	 */
    private int Step1_processAllHitFiles() 
    {	   
	    	int total = 0;
	    	int nFiles = hitObj.numDB();
    	
		try {
			Out.PrtSpMsg(1, "Annotate sequences with sequence hits from " + nFiles + " DB file(s)");
			hitsInDB = loadUniqueHitSet();
	    	
		    	for (int i=0; i< nFiles; i++) {  
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
		    		if (isAAannoDB) dbLabelType = "protein"; 
		    		else dbLabelType = "DNA";
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
				if (db.equals("-")) Step1c_addUniqueNoFastaFile(i);
				else Step1c_addUniqueFromFastaFile(i);
				Step1d_updateHitCov();
				
			    	hitsAddToDB.clear();
			    	
			    	Out.Print("");
			    	total += nAnnoSeq;
			    	nAnnoSeq = 0;
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
       	int nHitNum=0, cntUniqueExists=0, noSeq=0;
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
	    		
	    		LineParser lp = new LineParser();
	    		int cntHits=0, cntPrt=0;
	    		
	    		while ((line = reader.readLine()) != null) {
	    			if ( line.length() == 0 || line.charAt(0) == '#' ) continue;  			
			    	nHitNum++; cntPrt++;
		        if (cntPrt == COMMIT) {     
		        		cntPrt=0;
		          	Out.r("      Annotated sequences " + nAnnoSeq + ", total hits   " + 
		          		nTotalHits + ", HSPs   " + nHitNum);
		        }	   
	    			String[] tokens = line.split("\t");
	    			if (tokens == null || tokens.length < 11) continue;
	    			
		    		String newSeqName = tokens[0];
				if (!curSeqName.equals(newSeqName)) { // new sequence
					
					if (! curSeqName.equals("")) { // process previous
						if (curHitDataForSeq.size() > 0) {// may have had bad hit
							addHitDataForCtgToDB(); 
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
				
				if (!lp.parseLine(line)) {
					Out.PrtError("Error parsing: " + line);
					continue;
				}
				String newHitName = lp.getHitID();
				if (curHitName.equals(newHitName)) continue;
				curHitName = newHitName;
				
				cntHits++;
				if (cntHits>maxHitsPerAnno) { 
					continue;
				}
			    	// create list of hits for current contig
			    	hitData = new BlastHitData (isAAannoDB, line);
			    	
			    	// if HIT id already in database, then this is being run again
			    	if (hitsInDB.contains(hitData.getHitID())) {
			    		cntUniqueExists++;
			    	}
			    	// hit is past the 32k limit so no use saving
			    	else if (hitData.badHitData(maxHitSeq, minBitScore)) {
			    		nTotalBadHits++;
			    	}
			    	else {
			    		hitData.setCTGID(curSeqData.getCTGID());	   
			    		curHitDataForSeq.add(hitData);	
			    	}
		    	}  // end loop through hit tab file
	    		//-----------------------------------------------------------//
	    		if (! curSeqName.equals("")) { // process last contig
	    			if (curHitDataForSeq.size() > 0) {// may have had bad hit
					addHitDataForCtgToDB(); 
					curHitDataForSeq.clear();
				}
	    		}
	    		if ( reader != null ) reader.close();
		
			System.err.print("                                                                         \r");
			if (noSeq>0) // this never occurs - founds and printed elsewhere
				Out.PrtWarn(noSeq + " Sequences in hit file but not in database ");
			if (cntUniqueExists > 1) 
				Out.PrtWarn(cntUniqueExists + " DB ids already existed in sTCW -- ignored ");
	
			if (nHitNum==0) 
				Out.PrtSpMsgTime(3, "NO HIT RESULTS", time);
			else {
				Out.PrtSpMsgTimeMem(3,nAnnoSeq + " annotated sequences                 ", time); 
			
				if (nTotalHits > 0) 
					Out.PrtSpMsg(3, nTotalHits + " " + dbLabelType +  "-sequence additional pairs ");
			}
	    	}
        catch ( Throwable err ) {
        		pRC=false;
			ErrorReport.reportError(err, "Annotator - reading DB hit file\nLine: " + line);
			return false;
        }       
        return true;
    }
 
    // gathered all hits in hitDataForCtg. Save each hit. 
    private void addHitDataForCtgToDB()
    {
	    try {
	    		int seqLen = curSeqData.getConsensusBases();
				
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
		    		String b = (hitData.hitIsProtein()) ? "1" : "0";
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
       		String name = curSeqData.getContigID();
		    if (!annoSeqMap.containsKey(name)) {
		    		int id = seqMap.get(name);
		    		annoSeqMap.put(id, "");
		    }
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
		    		String hitID = (String)hitsAddToDB.iterator().next();
		    		
				saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, hitID, "", "", "", "", 0);
				hitsInDB.add(hitID); // so do not get divide by zero error
		    		hitsAddToDB.remove(hitID); 
		    	}
			System.err.print("                                                                      \r");
			Out.PrtSpMsgTimeMem(3,cnt_add + 	" unique hits added",time);
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
					saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, hitID, 
						lp.getOtherID(), lp.getDescription(), lp.getSpecies(), hitSeq, 0);
					cnt_add++;
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
				read++; cntPrt++;
				if (cntPrt == COMMIT) {	                
			         Out.rp("Unique " + dbLabelType + " added ", cnt_add, total); 
			         cntPrt=0;
				}
			}
			// add last
			if (addHit) { 
				saveDBhitUnique(DBID, isAAannoDB, dbType, dbTaxo, lp.getHitID(), lp.getOtherID(), 
						lp.getDescription(), lp.getSpecies(), hitSeq, 0);
				cnt_add++;
			}
			if ( reader != null ) reader.close();

			System.err.print("                                                                      \r");
			Out.PrtSpMsgTimeMem(3,cnt_add + 	" unique hits descriptions added from " + read + "    ",time);
			if (cnt_add==0) {
				Out.PrtError("The annoDB fasta file does not correspond to the hit tab file -- or the headers lines are weird");
			}
			else if (!hitsAddToDB.isEmpty()) {
				Out.PrtWarn(hitsAddToDB.size() + 
					" HitIds in tab file not found in DB fasta file (happens with nr.gz due to irregular naming)");
				if (hitsAddToDB.size()>10) Out.PrtSpMsg(1, "Writing first 10 to annotator.log");
				int i=0;
				for (String name : hitsAddToDB) {
					Out.PrtSpMsg(1, "HitID: '" + name + "'");
					i++;
					if (i>10) break;
				}
			}
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
			Out.PrtSpMsg(3, "Update hit coverage ");
			mDB.executeUpdate("update pja_db_unitrans_hits as seq " +
			"inner join pja_db_unique_hits as hit on seq.DUHID = hit.DUHID " +
			"set seq.prot_cov = round( ((abs(seq.prot_end - seq.prot_start)+1)/ hit.length)* 100.0, 0) " +
			"where hit.length>0 and seq.prot_cov=0 and seq.prot_end>seq.prot_start");
			
			// X-Y doesn't work if X<Y, so to both ways; NT-NT can have this happen
			mDB.executeUpdate("update pja_db_unitrans_hits as seq " +
					"inner join pja_db_unique_hits as hit on seq.DUHID = hit.DUHID " +
					"set seq.prot_cov = round( ((abs(seq.prot_start - seq.prot_end)+1)/ hit.length)* 100.0, 0) " +
					"where hit.length>0 and seq.prot_cov=0 and seq.prot_start>seq.prot_end");
			return true;
    		}
    		catch (Exception e) {
			pRC = false;
			ErrorReport.prtReport(e, "updateHitCov");
			return false;
		}
    }
    private void saveDBhitUnique(int DBID, boolean isProtein, String dbtype, String dbtaxo, 
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
    /*******************************************************
     * After all hit files are processed and all DBfasta info entered
     * For each contig: 
     * 	set pja_db_unitrans_hits.filtered for each hit
     * 	Set filter counts, PIDov (best annotation), PID (best evalue)
     */
    private void Step2_processAllHitsPerSeq() {  
    		int cntPrt=0, cntAll=0, cntTotalDiffFrame=0, countSP=0;
    		long startTime = Out.getTime();
        	Out.PrtSpMsg(2,"Process all hits for " + annoSeqMap.size() + " sequences ");
        	LineParser lp = new LineParser();
        	CtgData ctgData = new CtgData ();
        	ArrayList <HitData> hitList;
        	ArrayList <Olap> olapList = new ArrayList <Olap> ();
        ArrayList <String> geneSet = new ArrayList <String> ();
        ArrayList <String> specSet = new ArrayList <String> ();
        ArrayList <String> annoSet = new ArrayList <String> (); 
    	
		try {
			mDB.renew();

		/** go through all contigs that have annoDB hits */
        		for (int CTGid : annoSeqMap.keySet()) { // contigs with DB hits   
        			ctgData.CTGid=CTGid;	
        			cntPrt++; cntAll++;
        			if(cntPrt==COMMIT) {
        				Out.r("Processed sequences " + cntAll); 
        				cntPrt=0;
        			}
    			
        			int saveDBID=-1;
				int cntTop=0, cntSwiss=0, cntTrembl=0, cntNT=0, cntAA=0;
				int cntDiffFrame=0;
			
				HitData bestAnno = null, bestEval = null, bestSP = null;
  			    
  			    hitList = loadHitDataForCtg(CTGid);
  			    if (hitList==null || hitList.size()==0) continue; 
  			    Collections.sort(hitList);
				  				
		/** XXX go through all hits for the current contig -- sorted on bitscore, goodDesc, and rank */
        			for (int h=0; h < hitList.size(); h++) {
        				HitData hitData = hitList.get(h);
        			    olapList.add(new Olap(hitData));
        			    
        			/* get best description and best hit */
        			    String hitType = hitData.dbtype;
        		       
        		    		if (bestEval == null) { 
    	            			bestEval = hitData;
    	        			}  
        		    		else if (hitData.frame!=bestEval.frame) cntDiffFrame++;
        		    		
        		    		if (hitData.isGood) {
		        		    	if (bestAnno==null) {
		        		    		bestAnno=hitData;
		    		        	}   	
		        		    	if (bestSP==null && hitData.dbtype.equals("sp")) {// keep bestSP - decide below which to keep
		        		    		bestSP=hitData;
		    		        	}   
        		    		}
        		     /* compute flags */
        				int dbid = hitData.DBid;
        				if (dbid != saveDBID) {
        					saveDBID = dbid;
        					cntTop=0;
        				}
        				// filter 2 is top three hits for annoDB - not used
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
        				// filter 8 is top hit for gene annotation
        				String gene = lp.getGeneRep(hitData.desc);
        				if (gene != null && !geneSet.contains(gene)) {
        					geneSet.add(gene);
        					hitData.filter |= 8;
        				}
            			   			
        				if (hitType.equals("sp")) cntSwiss++;
        				else if (hitType.equals("tr")) cntTrembl++;
        				else if (hitData.isAAhit==1) cntAA++; 
        				else cntNT++;
        				String anno = hitData.dbtaxo + hitData.dbtype;
        				if (!annoSet.contains(anno)) annoSet.add(anno);
        			} 
        	/* end loop through hits for this contig */
        		
        	// XXX finish setting reading frame for ORF finding
        			if (bestEval!=null) {// This format is used in CoreAnno, see Tag ORFTAG
	        			if (bestEval.isAAhit==1) annoSeqMap.put(CTGid, bestEval.frame + ":" + 
	        					bestEval.ctgStart + ":" + bestEval.ctgEnd + ":" + bestEval.eVal);
	        			ctgData.pFrame=bestEval.frame;
	        			
	        			if (bestAnno==null) bestAnno=bestEval; // there is no isGood
        			}
        			
        			if (bestAnno!=null) { 
        				double ev = (bestEval.eVal==0.0) ? DLOW : bestEval.eVal;
        				double an = (bestAnno.eVal==0.0) ? DLOW : bestAnno.eVal;
        				double eexp = Math.exp(Math.log(ev) * useEV);
        				
		    			if (an>eexp) {
		    				bestAnno=bestEval; 
		    			}
		    			else if (bUseSP) { // use SP if close
        					if (bestSP!=null && bestAnno!=bestSP) {
            		    			double sp = (bestSP.eVal==0.0)   ? DLOW : bestSP.eVal;
            		    			eexp = Math.exp(Math.log(an) * useSP);
            		    			
            		    			if (sp<=eexp) {
            		    				bestAnno=bestSP; 
            		    				countSP++;
            		    			}
            				}
        				}
        			}
    		       	      			
        			// rank is based on the order of input from the hit file
        			// the bestAnno and bestEval are set to rank 1 so show in the view contigs
        			bestAnno.filter = bestAnno.filter | 32;
        			bestAnno.rank = 1; 
        			bestEval.filter = bestEval.filter | 16;
        			bestEval.rank = 1; 
    		       	ctgData.annoPID = bestAnno.Pid;
    		       	ctgData.evalPID = bestEval.Pid;
    		       	ctgData.bestmatchid = bestEval.hitName;
        			
        			ctgData.cnt_gene = geneSet.size();
        			ctgData.cnt_species = specSet.size();
        			ctgData.cnt_overlap = setFilterOlap(olapList);
        			ctgData.cnt_annodb = annoSet.size();
        			ctgData.cnt_swiss = cntSwiss;
        			ctgData.cnt_trembl = cntTrembl;
        			ctgData.cnt_nt = cntNT;
        			ctgData.cnt_pr = cntAA;    
        			
        			if (cntDiffFrame>0) {
        				ctgData.remark = Globals.RMK_MultiFrame;
        				cntTotalDiffFrame++;
        			} 
        			else ctgData.remark="";
        			
        			// save annoDB filters as to whether it is a best hit, etc
        			saveDBhitFilterForCtgHits(hitList);
        			
	    			// saves contig filters, PID, PIDov, and compute protein ORF    	       	
    	    		    saveDBHitCntsForCtg(ctgData);
        			
        			geneSet.clear(); specSet.clear(); olapList.clear(); 
        			annoSet.clear(); hitList.clear();
        		}// end loop through contig list
        		
        		// finished setting all filters   CAS303 changed rank to best_rank for mySQL v8
        		mDB.executeUpdate("update pja_db_unitrans_hits set best_rank=(filter_best+filter_ovbest)");
        		
        		System.err.print("                                                           \r");
        		Out.PrtSpCntMsgZero(2, countSP, "replaced bestAnno with best SwissProt");
        		Out.PrtSpCntMsgZero(2, cntTotalDiffFrame, "Sequences with hits to multiple frames ");
        		Out.PrtSpMsgTimeMem(2, "Finish filter", startTime);
		}
        catch ( Throwable err ) {
        		pRC=false;
			ErrorReport.reportError(err, "Annotator - computing filters for DB hits");
			return;
        }
    }

    private int setFilterOlap(ArrayList <Olap> olapList) {
    			
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
    			   if      (isContained(comp1, o2, o1)) o2.isOlap=false;
    			   else if (isContained(comp1, o1, o2)) {o1.isOlap=false; break;}
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
 	private boolean isContained(int comp, Olap o1, Olap o2) {
 		if (comp==0 && o1.start >= o2.start-flank && o1.end <= o2.end+flank) 
 			return true;
 		if (comp==1 && o1.end   >= o2.end-flank   && o1.start <= o2.start+flank) 
 			return true;
 		return false;
 	}
    private void saveDBhitFilterForCtgHits(	ArrayList <HitData> hitList) 
    {
    		try
    		{  
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
	        		if ((filtered & 1) != 0) olap  = 1;
	        		if ((filtered & 2) != 0) top3  = 1;
	        		if ((filtered & 4) != 0) species  = 1;
	        		if ((filtered & 8) != 0) gene  = 1;
	    	
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
    private void saveDBHitCntsForCtg(CtgData ctg) 
    {
    		try
    		{   
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
    			ps.setInt(14, ctg.CTGid);
    	        ps.execute();
    	        ps.close();
       	}
    		catch (Exception e) {
    			pRC = false;
    			ErrorReport.die(e, "Error on save DB hit cnts for sequence ");
    		} 
    }
    
    
    /******************************************************
     * Called at end of adding all Hits.
     */
    private boolean Step3_saveSpeciesSQLTable() {	
		try {
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
    	    	
	    	    	Out.PrtSpMsg(3, "Read species per sequence from database (" + minSeq + "," + maxSeq + ")");
	    	   
	    	    	ResultSet rs=null;
	    	    	String sql = "select " +
	    	    		" pja_db_unique_hits.DBID,  pja_db_unique_hits.species, " +
    			    " pja_db_unitrans_hits.filter_best, pja_db_unitrans_hits.filter_ovbest " +
    				" from pja_db_unitrans_hits " +
    				" join pja_db_unique_hits  " +
    				" WHERE pja_db_unitrans_hits.DUHID = pja_db_unique_hits.DUHID ";			
	    	    
	    	    for (int ctgid=minSeq, cntid=0; ctgid<=maxSeq; ctgid++, cntid++) {
	    	    		rs = mDB.executeQuery(sql + " and CTGID=" + ctgid + 
	    	    				" order by pja_db_unique_hits.DBID"); 
	    	    		
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
	    		Out.PrtSpCntkMsg(4, cnt, " total hits                                        ");
	    		Out.PrtSpCntkMsg(4, nSpe, " total species");
	    		
	    		mDB.openTransaction();
	    		PreparedStatement ps = mDB.prepareStatement("insert pja_db_species set " +
    					"AID = 1, species = ?, count = ?, nBestHits = ?,nOVBestHits = ?");
	    		cnt=0; cntPrt=0;
	    		Out.PrtSpMsg(3, "Insert species counts into database");
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
	    		Out.PrtSpMsg(3, "Insert species totals per database");
	    		
	    		// These are only used in Overview 
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
       		pRC = false;
    			ErrorReport.reportError(e, "Error making species tables");
    			return false;
       	}
    }

	/******************************************************
	 * Load data
	 */
    private ArrayList <HitData>loadHitDataForCtg (int CTGid) throws Exception
	{
        ArrayList <HitData> hitList= new ArrayList <HitData> ();
        try
        {
        		int seqLen = mDB.executeCount("select consensus_bases from contig where CTGid=" + CTGid);
            String strQ = 	"SELECT " +
            		"pja_db_unique_hits.DBID, pja_db_unique_hits.hitID," +
            		"pja_db_unique_hits.description, pja_db_unique_hits.species, " +
            		"pja_db_unique_hits.dbtype, pja_db_unique_hits.taxonomy, " +
            		"pja_db_unitrans_hits.PID, " +
            		"pja_db_unitrans_hits.e_value, pja_db_unitrans_hits.bit_score," +
            		"pja_db_unitrans_hits.ctg_start, pja_db_unitrans_hits.ctg_end, " +
            		"pja_db_unitrans_hits.blast_rank, pja_db_unique_hits.isProtein " +
		    		"FROM pja_db_unique_hits " +
		    		"JOIN pja_db_unitrans_hits " +
		    		"ON pja_db_unitrans_hits.DUHID = pja_db_unique_hits.DUHID " +
		    		"WHERE   pja_db_unitrans_hits.CTGID = " + CTGid + " " +
		    		"order by pja_db_unitrans_hits.PID";
		 
            ResultSet rset = mDB.executeQuery( strQ );
            while( rset.next() )
            {	
            		HitData hit = new HitData ();
                hit.DBid = rset.getInt(1);
                hit.hitName = rset.getString(2);
                hit.desc = rset.getString(3).trim().toLowerCase();
                hit.species = rset.getString(4).trim().toLowerCase();
                hit.dbtype =  rset.getString(5);
                hit.dbtaxo = rset.getString(6);
                
                hit.Pid = rset.getInt(7);
                hit.eVal = rset.getDouble(8); 
                hit.bitScore = rset.getDouble(9);
		        hit.ctgStart = rset.getInt(10);
		        hit.ctgEnd = rset.getInt(11); 
		        hit.rank = rset.getInt(12);
		        hit.isAAhit = rset.getInt(13);  
		        
		        hit.isGood = goodDescript(hit.desc);
		        if (hit.isGood && hit.dbtype.equals("nt")) hit.isGood=false; 
		        /******************************************************
		         * RCOORDS
		         */
		        int orient = 1;
    				if (hit.ctgEnd <= hit.ctgStart) { // sequence will be reverse complemented before ORF finding
					hit.ctgStart = seqLen - hit.ctgStart +1;
					hit.ctgEnd = seqLen - hit.ctgEnd + 1;
					orient = -1;
    				}
    				if (!isAAstcwDB) { 
    					hit.frame = hit.ctgStart % 3;
    					if (hit.frame==0) hit.frame = 3;
    					if (orient<0) hit.frame = -hit.frame;
    				}
		        hitList.add(hit);
	    		}
            if ( rset != null ) rset.close();
            if (hitList.size()==0) return null; // happens with mismatch of .fasta and .tab hit file
	    		return hitList;
        }
        catch(Exception e) {
        		pRC = false;
	        	ErrorReport.prtReport(e, "Reading database for hit data");
	        	throw e;
        }	
	}
    private HashSet<String> loadUniqueHitSet()
    {
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
	public void setFlankingRegion(int f) {flank = f; System.out.println("flanking " + flank);} // default 30
	public void setSwissProtPref(int b) {if (b==0) bUseSP=false; else bUseSP=true;}
	public void setMinBitScore(int m) {minBitScore = m; } // default 0
	
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
	
	private class CtgData {
  		int CTGid, annoPID, evalPID;

  		String bestmatchid, remark="";
  		int cnt_overlap, cnt_gene, cnt_species, cnt_annodb; 
  		int cnt_swiss, cnt_trembl, cnt_nt, cnt_pr;
  		int pFrame=0;
    }
	private class HitData implements Comparable <HitData> {
 		 String desc, species, dbtype, dbtaxo;
 		 double eVal, bitScore;
	     int DBid, Pid;
	     String hitName;
	     int ctgStart, ctgEnd, isAAhit, frame;
	     int filter=0, rank=0;
	     boolean isGood=true;
	     
	     public int compareTo(HitData b) {
	    	 	if (this.eVal < b.eVal) return -1;
	    		if (this.eVal > b.eVal) return 1;
	    	 	if (this.bitScore>b.bitScore) return -1;
 			if (this.bitScore<b.bitScore) return 1;
 			if (this.isGood && !b.isGood) return -1;
 			if (!this.isGood && b.isGood) return 1;
	    		
	    		if (this.dbtype.equals(b.dbtype)) {	
	    			if (this.rank<b.rank) return -1;
	    			if (this.rank>b.rank) return 1;
	    			return 0; 
	    		}
	    		return 0;
 		}
	 }
	private void cleanup() {
		if (hitsAddToDB!=null) hitsAddToDB.clear();
		if (hitsInDB!=null) hitsInDB.clear();
		if (curHitDataForSeq!=null) curHitDataForSeq.clear();
		// if (ctgMap!=null) ctgMap.clear(); - not this one, was passed from caller
		if (annoSeqMap!=null) annoSeqMap.clear();
		if (duidMap!=null) duidMap.clear();
		if (notFoundSeq!=null) notFoundSeq.clear();
	}
	// current file
	private int DBID = 0;
	private int dbNum = 0;
	private String dbLabelType = "";
	private String dbTaxo = "";
	private String dbType = "";
	private boolean isAAannoDB = true, isAAstcwDB=false;
    
	private HashSet<String> hitsAddToDB = new HashSet<String> (); // unique set in dbFasta
	private HashSet<String> hitsInDB = new HashSet<String> (); // already in sTCW
	
	private ContigData curSeqData = new ContigData ();
   	private ArrayList <BlastHitData> curHitDataForSeq = new ArrayList <BlastHitData> ();
   	
    private TreeMap<String, Integer> seqMap = new TreeMap<String, Integer> (); // sequences in database 
    private HashMap<Integer, String> annoSeqMap = new HashMap<Integer, String> (); // sequences with annotation
    
	private int flank = 0, minBitScore = 0; // neither of these are ever set, but can be
	private HashMap <String, Integer> duidMap = new HashMap <String, Integer> ();
	
	// counts
	private int nAnnoSeq = 0, nTotalHits = 0, nTotalBadHits = 0, nSeqTooLong=0;
	private HashSet <String> notFoundSeq = new HashSet <String> ();
  		
    private DoBlast hitObj = null;				// created in runSingleMain	
    private DBConn mDB = null;
    private boolean pRC = true;
}
