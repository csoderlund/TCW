package sng.annotator;

/** 
 * This class provides the database routines for the Core Annotator.
 */

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.*;

import java.util.Vector;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;

import sng.database.Schema;
import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import sng.viewer.panels.align.AlignData;
import util.database.DBConn;
import util.methods.*;

public class CoreDB {
	private boolean bAnnoExists=false;
	private boolean bGO = false;
	public boolean isFirstAnno() { return !bAnnoExists;}
	public boolean existsGO() { return bGO;}
	
	
	// Used only by ManagerFrame to deleteAnnotation
	public CoreDB (DBConn db) 
	{
		mDB = db;
	}
	public void reset () {
		try {
			mDB.renew();
		} 
		catch (Exception e) {}
	}
	public CoreDB (DBConn dbc, boolean doAnno, String tcwDB, String tcwID   ) 
		throws Exception
	{
		mDB = dbc;
		STCWdb = tcwDB;
		strAssemblyID = tcwID;
		
		try {
			if (!mDB.tableExist("schemver")) {
				System.err.println("Incomplete database -- cannot get schema version " + tcwDB);
				return;
			}
		} catch (Exception e) {
			System.err.println("Problem with database " + tcwDB);
			return;
		}
		
	    ResultSet rs = null;
	    try {           
	        String strQ = "SELECT AID, username, projectpath, assemblydate, annotationdate " +
	    			"FROM assembly WHERE assemblyid='" + strAssemblyID + "'";
	  
	    	rs = mDB.executeQuery ( strQ );
	    	if ( !rs.next() ) { 
	    		String stcw = strAssemblyID;    	
	    		strQ = "SELECT AID, username, projectpath, assemblydate, annotationdate, assemblyid FROM assembly";
	    		rs = mDB.executeQuery ( strQ );
		    	if ( !rs.next() ) Out.die("Has Instantiate been executed?");
		    	strAssemblyID = rs.getString("assemblyid");
		    	Out.PrtWarn("Database: " + stcw + " singleTCW ID: " + strAssemblyID);
	    	}
	   
	    	username = rs.getString("username");
	    	projectpath = rs.getString("projectpath");
	    	if (projectpath.contains("/./")) projectpath = projectpath.replace("/./", "/"); // CAS314
	    	assemblydate = rs.getString("assemblydate");
	    	try {
	    		annotationdate = rs.getString("annotationdate");
	    	}
	    	catch(Exception e) {annotationdate = "";} 
	    	
	    	String anno = "Annotation:   " + annotationdate;
	    	if (annotationdate == null || annotationdate.equals("")) {
	    		if (existsAnno()) {
	    			System.out.println("Annotation date is missing but annotation exists - may be incomplete");
	    			anno = "Annotation:  incomplete ";
	    			bAnnoExists = true;
	    		}
	    		else {
	    			anno = "Database has no annotation.";	
	    			bAnnoExists = false;
	    		}
	    		annotationdate="unknown";
			} 
			else {
				bAnnoExists=true;
				anno = "Previous annotation was completed on " + annotationdate + ".";
			
	    		if (!existsAnno()) {
	    			bAnnoExists=false;
	    			anno = "Previous annotation started but did not finish.";
	    		}
			}
			
			isAAtcw = mDB.tableColumnExists("assem_msg", "peptide");
	    	String type = (isAAtcw) ? "AA-sTCW" : "NT-sTCW";
	    	
	    	Out.PrtSpMsg(1, "sTCW ID:  " + strAssemblyID);
	    	Out.PrtSpMsg(1, "Database: " + type);
	    	Out.PrtSpMsg(1, "Create:   " + assemblydate);
	    	Out.PrtSpMsg(1, "User:     " + username);
	    	Out.PrtSpMsg(1, "Path:     " + projectpath);
	    	Out.PrtSpMsg(1, anno);
	    
	    	if (doAnno && bAnnoExists) { 
	    		Out.PrtSpMsg(1,"");
	    		String [] x = new String [3]; 
	    		x[0]="a"; x[1]="d"; x[2]="e";
	    		String ans = 
	    		  runSTCWMain.promptQuestion("Annotation exists in database. Enter [a/d/e]\n" +
	    		  "  Add to existing annotation [a], Delete annotation [d], Exit [e]: ", x, 0);
	    		if (ans.equals("e")) Out.die(" User request");
	    		else if (ans.equals("d")) {
	    			Out.PrtSpMsg(1, "Will delete existing annotation");
	    			runSTCWMain.bDelAnno = true; // deletes after all checks
	    			bAnnoExists = false;
	    		}
	    		else System.err.println("Add to existing annotation");
	    	}	
	    	// CAs319 was looking for gotree
	    	bGO = mDB.tableExists("go_info");
	    	if (bGO)
	    		bGO = (mDB.executeCount("select count(*) from go_info limit 1")>0);
	    	
	    	if (rs!=null) rs.close();  
	    }
		catch (Exception e){ErrorReport.die(e, "Unable to query " 	+ tcwDB);}
	}
	public boolean hasORFs() {
		boolean rc=true;
	    ResultSet rs = null;
	    try {           
			rs = mDB.executeQuery("Select count(*) from contig where o_frame!=0");
			if (rs.next()) { 
				int cnt = rs.getInt(1);
				if (cnt==0) rc=false;
			}
		    	if (rs!=null) rs.close();  
	    }
		catch (SQLException e){ErrorReport.die(e, "mySQL error checking for ORFs ");}
		catch (Exception e){ErrorReport.die(e, "failed checking for ORFs");}
	    return rc;
	}	
	
	/**
    * Reannotate
    */
   public boolean deleteAnnotation (boolean prt) 
   {
	   if (prt) Out.PrtSpMsg(0, "Start deleting all previous annotation data");
       try { 	
	   		Out.PrtSpMsg(1, "Remove annotation from sequences...");
	   		
	   		Schema s = new Schema(mDB);
			if (!s.current()) s.update();
			
			mDB.executeUpdate("UPDATE assem_msg set pja_msg=null, meta_msg=null, anno_msg=null, " +
					"spAnno=false, orf_msg=null, gc_msg=null, go_msg=null,go_slim=null");
			
       	    mDB.executeUpdate("UPDATE contig " +
       	   		"SET PID = NULL, bestmatchid = NULL, PIDov = NULL, PIDgo = NULL, " +
           		"cnt_overlap = 0, cnt_species = 0, cnt_gene = 0, " +
           		"cnt_swiss = 0, cnt_trembl = 0, cnt_nt = 0, cnt_pairwise = 0, cnt_annodb = 0, " +
				"o_coding_start = NULL, o_coding_has_begin = 0, " +  
				"o_coding_end = NULL, o_coding_has_end = 0, o_frame = NULL, " +
				"o_len=0, p_eq_o_frame = 0, p_frame = NULL," +
				"gc_ratio=0, notes = NULL ");
       	  
       	   if (mDB.tableColumnExists("contig", "cnt_gi")) 
       		   mDB.executeUpdate("update contig set cnt_gi=0");
       	   else 
       		   mDB.executeUpdate("alter table contig add cnt_gi int unsigned default 0");
       		   
       	   mDB.executeUpdate("UPDATE assembly SET annotationdate=null WHERE AID=1");
       	   
       	   Out.PrtSpMsg(1, "Remove annotation tables..."); // CAS338 add Out.r
       	   Out.r("   Remove pairwise"); 	mDB.tableDelete("pja_pairwise");			
       	   Out.r("   Remove seq hits");	mDB.tableDelete("pja_db_unitrans_hits");
       	   Out.r("   Remove unique hits");	mDB.tableDelete("pja_db_unique_hits");
       	   Out.r("   Remove annoDBs");		mDB.tableDelete("pja_databases");
       	   Out.r("   Remove species");		mDB.tableDelete("pja_db_species");
       	   Out.r("   Remove ORF tuples");	mDB.tableDelete("tuple_orfs"); // CAS305
       	   Out.r("   Remove tuple usage");	mDB.tableDelete("tuple_usage");// CAS305
       	   Out.rClear();
        
       	   Schema.dropGOtables(mDB); // CAS332 was dropping from here, and not complete
       	   
       	   // CAS331 Created during prune from command-line
       	   mDB.tableDrop(DoUniPrune.tmp_hit);
    	   mDB.tableDrop(DoUniPrune.tmp_seq);
    	   if (mDB.tableColumnExists("assem_msg", "prune"))
    		   mDB.executeUpdate("update assem_msg set prune = -1");
    	   
           if (prt) Out.PrtSpMsg(0, "Complete deleting annotation");
           return true;
       }
       catch(Exception e) {ErrorReport.die(e,"Error deleting annotation data - try again...");}
       return false;
   }
  
   public void deletePairwise() {
	   Out.PrtSpMsg(0, "Start deleting all previous annotation data");
       try
       {
       	   mDB.executeUpdate("update assem_msg set pja_msg = NULL where AID = 1");
       	   mDB.tableDelete("pja_pairwise");
       	   mDB.executeUpdate("UPDATE contig SET cnt_pairwise = 0");
       }
       catch(Exception e) {ErrorReport.die(e,"Error deleting pairwise data ...");}  	 
   }
   /***************************************************
    * DoBlast
    */
	// check at beginning of annotation whether databases have already been used
    public ArrayList <String> loadDatabases (  ) throws Exception
    {
       ResultSet rset = null;

       try {
           ArrayList <String> paths = new ArrayList <String> ();
           String strQuery = "SELECT path FROM pja_databases ";
 
           rset = mDB.executeQuery( strQuery );
           
           while(rset.next())
               paths.add ( rset.getString(1) );
               
           return paths;
       }
		catch (SQLException e) {
			String err = "cannot load Blast databases from TCW database";
			ErrorReport.die(e, err);
		} 
       finally {
           if ( rset != null ) rset.close();
       }
       return null;
    }   
    public int writeSeqFile(File f) 
 	{
 		try {
 			BufferedWriter file = new BufferedWriter(new FileWriter(f));
 		
 	        ResultSet rs = mDB.executeQuery("select contigid,consensus from contig" );

 	        int cnt=0;
 			while (rs.next())
 			{
 				String ctgid = rs.getString(1);
 				
 				String CCS = rs.getString(2);
 				
 				file.append(">" + ctgid + "\n" + CCS + "\n"); 
 				cnt++;				
 			}
 			rs.close();
 			file.close();
 			return cnt;
 		}
 		catch (Exception e){
             ErrorReport.prtReport(e,"Error writing to " + f.getAbsoluteFile());
             return -1;
         }
 	}
    /***********************************************************
     * Write ORFs for DoBlast for Similar Pairs CAS314
     * sng.util.MainTable also writes ORF file, but from a table of sequences
     */
    public int writeOrfFile(File f) 
 	{
 		try {
 			BufferedWriter file = new BufferedWriter(new FileWriter(f));
 		
 	        ResultSet rs = mDB.executeQuery("SELECT contigid, o_frame, o_coding_start, o_coding_end, consensus FROM contig" );

 	        int cnt=0;
 			while (rs.next())
 			{
 				String name = rs.getString(1);
 				int fr = rs.getInt(2);
                if (fr == 0) continue;
                
                int start = rs.getInt(3);
                int end = rs.getInt(4);
  
                String strSeq = rs.getString(5);
                String orf = SequenceData.getORFtrans(name, strSeq, fr, start, end);
             	
                file.append(">" + name + " AAlen=" + orf.length() + " frame=" + fr + "\n" + orf + "\n");	
                cnt++;
 			}
 			rs.close();
 			file.close();
 			return cnt;
 		}
 		catch (Exception e){
             ErrorReport.prtReport(e,"Error writing to " + f.getAbsoluteFile());
             return -1;
         }
 	}
   /******************* Pairwise table **********************
    * CoreAnnotator: doHomologyTest
    * *********************************************************/
   public TreeMap <String, Integer> loadContigMap (  )
   {
       ResultSet rset = null;

       try {
           TreeMap<String, Integer> theIDs = new TreeMap<String, Integer> (); 

           String strQuery = "SELECT contigid, CTGID FROM contig ";
           rset = mDB.executeQuery( strQuery );
           
           while(rset.next()) 
               theIDs.put( rset.getString(1), rset.getInt(2) );
               
           rset.close();
           return theIDs;
       }
       catch (SQLException e) {ErrorReport.prtReport(e, "cannot load Contigs IDs from database");}
       catch (Exception e) {ErrorReport.prtReport(e, "cannot load Contigs IDs from database");} 
       return null;
   }    
   /*
 	* A shared hit between two pairs
    */    
   public void loadSharedHit (BlastHitData hitData ) throws Exception
   {
       ResultSet rset = null;
       String strQ;
  	
       try { 
           	String ctgID1 = hitData.getContigID();
           	String ctgID2 = hitData.getHitID();
 
	        strQ = "SELECT p1.uniprot_id " +
	        		" FROM pja_db_unitrans_hits as p1 " +
	        		" JOIN pja_db_unitrans_hits as p2 " +
	        		" WHERE p1.contigid = '" + ctgID1 + "'" +
	        		" AND   p2.contigid = '" + ctgID2 + "'" +
	        		" AND p1.uniprot_id = p2.uniprot_id limit 1";

	        rset = mDB.executeQuery( strQ );
	        String nBest="";
	        if (rset.next ()) nBest = rset.getString(1);
	    		hitData.setSharedHitID(nBest);
        }
		catch (SQLException e) {
			ErrorReport.die(e, "cannot load DB hit contig pairs from database");
		} 
       finally {
       		if ( rset != null ) rset.close();
       }
    }
    //all pairs have been added to pairsHash - remove those already in database
     public int removePairsInDB (HashMap <String, BlastHitData> pairsHash) throws Exception
     {
         ResultSet rset = null;
         int count = 0;
         String key, contig1, contig2;
     	
         try {
 	        String strQuery = "SELECT contig1, contig2 FROM pja_pairwise "; 
             rset = mDB.executeQuery( strQuery );
             while(rset.next()) {
 				contig1 = rset.getString("contig1");
 				contig2 = rset.getString("contig2");
 				
 				key = contig1 + ";" + contig2;
       			if (pairsHash.containsKey(key)) {
       				pairsHash.remove(key);
       				count++;
       			}
       			key = contig2 + ";" + contig1;
       			if (pairsHash.containsKey(key)) {
       				pairsHash.remove(key);
       				count++;
       			}
             }
         }
 		catch (SQLException e) {
 			String err = "cannot load pairs from database";
 			ErrorReport.die(e, err);
 			count = -1;
 		} 
         finally {
             if ( rset != null ) rset.close();
         }         
         return count;
     }
     /************************************************************************
      * Update hit_type CAS314
      */
    public int loadPairsFromDB(String delim, HashMap <String, String> pairTypeMap) {
 		try {
 			ResultSet rs = mDB.executeQuery("SELECT contig1, contig2, hit_type FROM pja_pairwise ");
 			while (rs.next()) {
 				pairTypeMap.put(rs.getString(1) + delim + rs.getString(2), rs.getString(3));
 			}
 			return pairTypeMap.size();
 		}
 		catch (Exception e) {ErrorReport.prtReport(e, "loading pairs from DB"); return -1;}
 	}
    public void savePairsType(String delim, HashMap <String, String> pairTypeMap) {
 		try {
 			for (String key : pairTypeMap.keySet()) {
 				String [] tok = key.split(delim);
 				String type = pairTypeMap.get(key);
 				mDB.executeUpdate("update pja_pairwise set hit_type='" + type + "' " +
 				" where contig1='" + tok[0] + "' and contig2='" + tok[1] + "'");
 			}
 		}
 		catch (Exception e) {ErrorReport.prtReport(e, "saving pairs from DB"); return;}
 	}
    public void saveAllCtgPairwiseCnts(HashMap <String, Integer> map) 
    {
        String strQ = null;
 
        try {
		    for ( String ctgName : map.keySet() ) {
				int cnt = map.get(ctgName);
				strQ = "UPDATE contig SET cnt_pairwise = "   + String.valueOf(cnt) +
	        		" WHERE contigid = '" + ctgName + "' ";
	       
				mDB.executeUpdate( strQ );
			}
        }
		catch (Exception e) {ErrorReport.prtReport(e, "cannot save contig pairwise counts to database");}
    }
    // CoreAnno - compute pairs
    public void savePairAlignments(Vector<AlignData> alignList, boolean isAAstcw) {
    	if (isAAstcw) savePairAlignAA(alignList);
    	else savePairAlignNT(alignList);
    }
    private void savePairAlignNT ( Vector<AlignData> alignList ) {
        if ( alignList.isEmpty() )return;
      
        try {
        	PreparedStatement ps = mDB.prepareStatement("INSERT into pja_pairwise set " +
                "AID=?, contig1=?, contig2=?, align_frame1=?, align_frame2=?, " +  
                "NT_olp_ratio=?, NT_olp_score=?, NT_olp_len=?, " +
                "AA_olp_ratio=?, AA_olp_score=?, AA_olp_len=?, " +
                "shared_hitID=?, hit_type=?, e_value=?, percent_id=?, alignment_len=?, " +
                "ctg1_start=?, ctg1_end=?, ctg2_start=?, ctg2_end=? ");
            mDB.openTransaction();
            for (AlignData alignObj : alignList)
            {
            	ps.setInt(1, 1);
                ps.setString(2, alignObj.getName1());					// contig1
                ps.setString(3, alignObj.getName2());
               
                ps.setInt(4, alignObj.getFrame1());
                ps.setInt(5, alignObj.getFrame2());
                ps.setDouble(6, alignObj.getOLPratio());
                ps.setInt(7, alignObj.getOLPmatch());
                ps.setInt(8, alignObj.getOLPlen());				
                
                AlignData aaObj = alignObj.getAApwData(); // DP results, always has AA results?
                if (aaObj==null) {
                	ps.setDouble(9, 0.0);
	                ps.setInt(10, 0);
	                ps.setInt(11, 0);
                }
                else {
	                ps.setDouble(9, aaObj.getOLPratio());
	                ps.setInt(10, aaObj.getOLPmatch());
	                ps.setInt(11, aaObj.getOLPlen());	
                }
                BlastHitData hitData = alignObj.getHitData();
                ps.setString(12, hitData.getSharedHitID());
                String type = hitData.getPairHitType();
                if (type.length()>=40) {
                	Out.PrtWarn("Hit Type too long: '" + type + "' for " + alignObj.getName1() + ", " + alignObj.getName2());
                	type = type.substring(0, 39);
                }
                ps.setString(13, type); // CAS314 was 3 tinyints
               
                ps.setDouble(14, hitData.getEVal() ); 
                ps.setInt(15, (int) hitData.getPercentID());
                ps.setInt(16, hitData.getAlignLen());
 
                ps.setInt(17, hitData.getCtgStart());
                ps.setInt(18, hitData.getCtgEnd());
                ps.setInt(19, hitData.getHitStart());
                ps.setInt(20, hitData.getHitEnd());
                ps.execute();
            }
            mDB.closeTransaction();
        }
        catch (Exception e) {ErrorReport.prtReport(e, "cannot save pairwise data");} 	
    }
    private void savePairAlignAA ( Vector<AlignData> alignList ) { // CAS314
        if ( alignList.isEmpty() )return;
      
        try {
        	PreparedStatement ps = mDB.prepareStatement("INSERT into pja_pairwise set " +
                "AID=?, contig1=?, contig2=?," +
                "AA_olp_ratio=?, AA_olp_score=?, AA_olp_len=?, " +
                "shared_hitID=?, hit_type=?, e_value=?, percent_id=?, alignment_len=?, " +
                "ctg1_start=?, ctg1_end=?, ctg2_start=?, ctg2_end=? ");
            mDB.openTransaction();
            for (AlignData aaObj : alignList) {
            	int i=1;
            	ps.setInt(i++, 1);
                ps.setString(i++, aaObj.getName1());					// contig1
                ps.setString(i++, aaObj.getName2());
               
                ps.setDouble(i++, aaObj.getOLPratio());
                ps.setInt(i++, aaObj.getOLPmatch());
                ps.setInt(i++, aaObj.getOLPlen());				
                	
                BlastHitData hitData = aaObj.getHitData();
                ps.setString(i++, hitData.getSharedHitID());
                ps.setString(i++, hitData.getPairHitType()); // CAS314 was 3 tinyints
               
                ps.setDouble(i++, hitData.getEVal() ); 
                ps.setInt(i++, (int) hitData.getPercentID());
                ps.setInt(i++, hitData.getAlignLen());
 
                ps.setInt(i++, hitData.getCtgStart());
                ps.setInt(i++, hitData.getCtgEnd());
                ps.setInt(i++, hitData.getHitStart());
                ps.setInt(i++, hitData.getHitEnd());
                ps.execute();
            }
            mDB.closeTransaction();
        }
        catch (Exception e) {ErrorReport.prtReport(e, "cannot save pairwise data");} 	
    }
    public void savePairMsg (String msg) { // CAS314
    	try {
    		if (mDB.tableColumnExists("assem_msg", "pair_msg"))
    			mDB.executeUpdate("update assem_msg set pair_msg='" + msg + "'");
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "Saving pair overview results");} 	
    }
  
    // used in ManagerFrame for Remove
    public boolean existsAnno() {
    	try {
			boolean bgc=true, bsim=true;
			ResultSet rs = mDB.executeQuery ("SELECT gc_ratio from contig where gc_ratio>0 limit 1");
    		if ( !rs.next() ) bgc=false;
    		
    		rs = mDB.executeQuery ("SELECT PWID FROM pja_pairwise WHERE PWID>0 limit 1");
    		if ( !rs.next() ) bsim=false;
    		
    		int nAnno = mDB.executeCount ("SELECT count(*) FROM pja_databases");
    		
    		return (bgc || bsim || nAnno>0);
		}catch (Exception e) {ErrorReport.prtReport(e, "cannot determine if annotation exists");} 
		return false;
    }
    /***************************************************
     * Static methods
     */
	
	// called for doHomology 
	static public ContigData loadContigData (DBConn mDB, String ctgName ) {
       ResultSet rset = null;
       
       try{
           ContigData curContig = new ContigData ();
           curContig.setContigID( ctgName);
           
           String strQuery = "SELECT consensus, consensus_bases, numclones, bestmatchid, o_frame " +
       			"FROM contig " +
       			"WHERE contig.contigid = '"  + ctgName + "'"; 
           
           rset = mDB.executeQuery( strQuery );
           if ( !rset.next() ) return null; 
           
           String seqString = (getStringFromReader(rset.getCharacterStream(1))).trim();       
           SequenceData consensus = new SequenceData ("consensus");
           consensus.setName( ctgName );
           // CAS313 consensus.setSequence ( SequenceData.normalizeBases( seqString, '*', Globals.gapCh ) ); 
           consensus.setSequence (seqString );
           
           curContig.setSeqData( consensus );
           curContig.setConsensusBases(consensus.getLength());
           curContig.setNumSequences( Integer.parseInt(rset.getString(3)) ); 
           curContig.setBestMatch(rset.getString(4));
           curContig.setFrame(rset.getInt(5));
           
           if ( rset != null ) rset.close();
           return curContig;
       }
       catch (Exception e) {
    	   ErrorReport.die(e, "Loading Consensus and Data");
    	   return null;
       }
   }
	public boolean setLongestSeq() { 
		try {
			// Needed for Assembled sequences. Used in Basic Sequence
			ResultSet rs = mDB.executeQuery( "select count(*) from contig " +
					" where longest_clone is not NULL" );
			rs.next ();
			int x = rs.getInt(1);
			if (x>0) return true;
			
			mDB.executeUpdate("update contig, contclone " +
					" set contig.longest_clone=contclone.cloneid " +
					" where contig.CTGID=contclone.CTGID");
			return true;
		}
		catch(Exception e) {ErrorReport.reportError(e,"Setting longest read"); return false;}
	}
	//	----------------CONVERTERS for CLOB fields --------------- //
	
	static private String getStringFromReader ( Reader reader ) 
		throws IOException, SQLException 
	{		
		String strReturn = "";
		char[] chChars = new char [ 2000 ]; 
		int nNumRead = 0;
	
		do 
		{
			// Keep filling the buffer until we exhaust the stream
			nNumRead = reader.read( chChars );
			if ( nNumRead <= 0 ) break;		
			// Append to the output string
			strReturn += new String ( chChars, 0, nNumRead );
		} while ( true );
			
		return strReturn;
	}
  
	
	public int getAID() { return 1; }
	public boolean isAAtcw() { return isAAtcw;}
	public boolean setIsAAtcw() {
	    try {
	        if (mDB.tableColumnExists("assem_msg", "peptide")) isAAtcw=true;
	        else isAAtcw=false;
	        return isAAtcw;
	    }
	    catch (Exception e) {ErrorReport.reportError(e, "Checking to see if proteing database"); return false;}
	}
	public DBConn getConn () {return mDB;}
	/****************** instance variables *****************************/
   
   public String strAssemblyID = null;
   public String STCWdb = null;
  
   private String username, projectpath, assemblydate, annotationdate;
   private boolean isAAtcw = false;
   
   private DBConn mDB;
}

