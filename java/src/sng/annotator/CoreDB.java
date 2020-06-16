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

import sng.database.Globals;
import sng.database.Schema;
import sng.database.Version;
import sng.dataholders.BlastHitData;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import util.align.AlignData;
import util.database.DBConn;
import util.methods.*;

public class CoreDB {
	private boolean bAnnoExists=false;
	private boolean bGOtree = false;
	private String annoVer="v1.0", annoDate="";
	public boolean isFirstAnno() { return !bAnnoExists;}
	public boolean existsGOtree() { return bGOtree;}
	public String getAnnoVer() { return annoVer;}
	public String getAnnoDate() { return annoDate;}
	
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
			    	Out.PrtWarn("singleTCW ID=" + stcw + " internal ID=" + strAssemblyID);
		    	}
		   
		    	username = rs.getString("username");
		    	projectpath = rs.getString("projectpath");
		    	assemblydate = rs.getString("assemblydate");
		    	try {
		    		annotationdate = rs.getString("annotationdate");
		    	}
		    	catch(Exception e) {
		    		annotationdate = "";
		    	} 
		   
		    	if (annotationdate==null && existsAnno()) { 
		    		System.out.println("Annotation date is missing but annotation exists - may be incomplete");
		    		setAnnotationDate();
		    		annotationdate="unknown";
		    	}
		    	String anno = "Annotation:   " + annotationdate;
			if (annotationdate == null || annotationdate.equals("") || annotationdate.equals("2000-11-09")) {
				anno = "Database has no annotation.";
				bAnnoExists = false;
			} 
			else {
				bAnnoExists=true;
				anno = "Previous annotation was completed on " + annotationdate + ".";
			
		    		if (!existsAnno()) {
		    			bAnnoExists=false;
		    			anno = "Previous annotation started but did not finish.";
		    		}
			}
			if (mDB.tableColumnExists("schemver", "annoVer")) {
				rs = mDB.executeQuery("Select annoVer, annoDate from schemver");
				if (rs.next()) {
					annoVer = rs.getString(1);
					annoDate = rs.getString(2);
				}		
			}
		    	Out.PrtSpMsg(1, "sTCW ID:      " + strAssemblyID);
		    	Out.PrtSpMsg(1, "Create:       " + assemblydate);
		    	Out.PrtSpMsg(1, "User Name:    " + username);
		    	Out.PrtSpMsg(1, "Project Path: " + projectpath);
		    	isAAtcw = mDB.tableColumnExists("assem_msg", "peptide");
		    	if (isAAtcw) Out.PrtSpMsg(1,"Protein sequence sTCW database");
		    	Out.PrtSpMsg(1,anno);
		    
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
		    	bGOtree = mDB.tableExists("pja_gotree");
		    	
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
	   deleteOverview();
       try
       { 	
    	   		Out.PrtSpMsg(1, "Remove annotation from sequences...");
    	   		
    	   		Schema s = new Schema(mDB);
    			if (!s.current()) s.update();
    			
    			mDB.executeUpdate("UPDATE assem_msg set pja_msg=null, meta_msg=null," +
    					"spAnno=false, orf_msg=null, gc_msg=null, go_msg=null, go_ec=null,go_slim=null");
    			
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
       	   
       	   Out.PrtSpMsg(1, "Remove annotation tables...");
       	   mDB.tableDelete("pja_pairwise");
       	   mDB.tableDelete("pja_db_unitrans_hits");
       	   mDB.tableDelete("pja_db_unique_hits");
       	   mDB.tableDelete("pja_databases");
       	   mDB.tableDelete("pja_db_species");
         
       	   Out.PrtSpMsg(1, "Remove GO tables...");
       	   mDB.tableDrop("go_info");
       	   mDB.tableDrop("pja_gotree");
       	   mDB.tableDrop("pja_unitrans_go");
       	   mDB.tableDrop("pja_uniprot_go");
       	   mDB.tableDrop("go_term2term");
       	   mDB.tableDrop("go_graph_path");
          
           if (prt) Out.PrtSpMsg(0, "Complete deleting annotation");
           return true;
       }
       catch(Exception e) {ErrorReport.die(e,"Error deleting annotation data - try again...");}
       return false;
   }
   public void deleteOverview() {
	   try {
		   mDB.executeUpdate("update assem_msg set " +
	       	   		"pja_msg = NULL, go_msg = null, orf_msg = null where AID = 1");
	   }
	   catch(Exception e) {ErrorReport.die(e,"Error deleting overview");}  	   	
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
    public int writeSeqFile(int uniBlastType,  File f) 
 	{
 		try {
 			BufferedWriter file = new BufferedWriter(new FileWriter(f));
 		
 	        ResultSet rs = mDB.executeQuery("select contigid,consensus,numclones from contig" );

 	        int cnt=0;
 			while (rs.next())
 			{
 				String ctgid = rs.getString("contigid");
 				String CCS = rs.getString("consensus");
 				int numClones = rs.getInt("numclones");
 				
 				if ( uniBlastType==0 ||
 					(uniBlastType==1 && numClones > 1 ) ||
 					(uniBlastType==2 && numClones ==1 )) 
 				{
 					CCS = CCS.toUpperCase();
 					file.append(">" + ctgid + "\n" + CCS.replace("*","") + "\n"); 
 					cnt++;
 				}				
 			}
 			file.close();
 			return cnt;
 		}
 		catch (Exception e)
         {
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
 
   
    public void saveAllCtgPairwiseCnts(HashMap <String, Integer> map) 
    {
        String strQ = null;
 
        try {
		    for ( String ctgName : map.keySet() )
	        {
				int cnt = map.get(ctgName);
				strQ = "UPDATE contig SET cnt_pairwise = "   + String.valueOf(cnt) +
	        		" WHERE contigid = '" + ctgName + "' ";
	       
				mDB.executeUpdate( strQ );
			}
        }
		catch (Exception e) {ErrorReport.prtReport(e, "cannot save contig pairwise counts to database");}
    }
    // CoreAnno - compute pairs
    public void savePairAlignments ( Vector<AlignData> alignList ) throws Exception
    {
        if ( alignList.isEmpty() )return;
      
        try
        {
        		PreparedStatement ps = mDB.prepareStatement("INSERT into pja_pairwise set " +
                "AID=?, contig1=?, contig2=?, coding_frame1=?, coding_frame2=?, " +
                "NT_olp_ratio=?, NT_olp_score=?, NT_olp_len=?, " +
                "AA_olp_ratio=?, AA_olp_score=?, AA_olp_len=?, " +
                "in_self_blast_set=?, in_uniprot_set=?, in_translated_self_blast=?," +
                "shared_hitID=?, e_value=?, percent_id=?, alignment_len=?, " +
                "ctg1_start=?, ctg1_end=?, ctg2_start=?, ctg2_end=? ");
            mDB.openTransaction();
            for (  AlignData alignObj : alignList )
            {
            		ps.setInt(1, 1);
                ps.setString(2, alignObj.getName1());					// contig1
                ps.setString(3, alignObj.getName2());
               
                ps.setInt(4, alignObj.getFrame1());
                ps.setInt(5, alignObj.getFrame2());
                ps.setDouble(6, alignObj.getOLPratio());
                ps.setInt(7, alignObj.getOLPmatch());
                ps.setInt(8, alignObj.getOLPlen());				
                
                AlignData aaObj = alignObj.getAApwData();
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
                ps.setBoolean(12, hitData.getIsSelf());
                ps.setBoolean(13, hitData.getIsShared());
                ps.setBoolean(14, hitData.getIsTself());
               
                ps.setString(15, hitData.getSharedHitID());
                ps.setDouble(16, hitData.getEVal() ); 
                ps.setInt(17, (int) hitData.getPercentID());
                ps.setInt(18, hitData.getAlignLen());
 
                ps.setInt(19, hitData.getCtgStart());
                ps.setInt(20, hitData.getCtgEnd());
                ps.setInt(21, hitData.getHitStart());
                ps.setInt(22, hitData.getHitEnd());
                ps.execute();
            }
            mDB.closeTransaction();
        }
        catch (Exception e) {ErrorReport.prtReport(e, "cannot save pairwise data");} 	
    }
 
    public boolean setAnnotationDate() 
	{
		try {
	    		if (!existsAnno()) 
	    			mDB.executeUpdate("UPDATE assembly SET annotationdate=null WHERE AID=1");
	    		else 
	    			mDB.executeUpdate("UPDATE assembly SET annotationdate=(CAST(NOW() as DATE)) WHERE AID=1");	
			
			return true;
		}catch (Exception e) {ErrorReport.prtReport(e, "cannot set annotation date");} 
		return false;
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
	
	// called from DoORF and DoUniProt.processAllDBblastFiles
	static public void updateAnnoVer(DBConn mDB) {
		try {
			mDB.executeUpdate("update schemver set annoVer='" +  Version.strTCWver + "'");
			mDB.executeUpdate("update schemver set annoDate='" + Version.strRelDate + "'");
		}
		catch (Exception e) {ErrorReport.reportError(e, "Updating annotation verion");}
	}
	// called by DoUniProt (why does it need sequence?) and doHomology 
	static public ContigData loadContigData (DBConn mDB, String ctgName ) 
   {
       ResultSet rset = null;
       
       try
       {
           ContigData curContig = new ContigData ();
           
           String strQuery = "SELECT consensus, consensus_bases, numclones, bestmatchid, o_frame " +
       			"FROM contig " +
       			"WHERE contig.contigid = '"  + ctgName + "'"; 
           
           rset = mDB.executeQuery( strQuery );
           if ( !rset.next() ) return null; 

           curContig.setContigID( ctgName);
           curContig.setCTGID( rset.getInt(5) );
           
           String seqString = ( getStringFromReader (rset.getCharacterStream( 1 ) ) ).trim();       
           SequenceData consensus = new SequenceData ("consensus");
           
           consensus.setName( ctgName );
           consensus.setSequence ( SequenceData.normalizeBases( seqString, '*', Globals.gapCh ) );
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
			// Only place this is computed. Used in Basic Sequence
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
	public void setIsAAtcw() {
	    try {
	        if (mDB.tableColumnExists("assem_msg", "peptide")) {
	            isAAtcw=true;
	            System.err.println("Protein sequence sTCW database");
	        }
	        else {
	            isAAtcw=false;
	        }
	    }
	    catch (Exception e) {ErrorReport.reportError(e, "Checking to see if proteing database");}
	}
	public DBConn getConn () {return mDB;}
	/****************** instance variables *****************************/
   
   public String strAssemblyID = null;
   public String STCWdb = null;
  
   private String username, projectpath, assemblydate, annotationdate;
   private boolean isAAtcw = false;
   
   private DBConn mDB;
}

