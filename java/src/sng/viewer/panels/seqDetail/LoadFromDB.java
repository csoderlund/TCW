package sng.viewer.panels.seqDetail;

/**************************************************
 * All database calls for the seqDetail and SeqAlign panels.
 * And one method for LoadPairFromDB to share the method to load the contig sequences and hits.
 */

import java.io.IOException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.BlastHitData;
import sng.dataholders.CodingRegion;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.dataholders.SNPData;
import sng.dataholders.SequenceData;
import util.methods.Converters;
import util.methods.ErrorReport;
import util.methods.Static;

import util.database.DBConn;

public class LoadFromDB {
	private boolean debug=false;
	private void prt(String msg) {if (debug) System.err.println("LoadFromDB: " + msg);}
	
	public LoadFromDB(DBConn d, MetaData m) {mDB=d; metaData = m;}
	
	// Called from SeqTopRowTab when sequence is selected from Sequence table
	public MultiCtgData loadContig ( MultiCtgData inCluster) throws Exception		
	{	
		prt("public loadContig");
		String ctgid = inCluster.getContig().getContigID();
		
		ContigData fullContig;
		
		if (metaData.hasAssembly()) {
			fullContig = loadDetailForContig ( ctgid);
			loadESTs(fullContig);
		}
		else {
			fullContig  = loadDetail( ctgid );
		}
		if (fullContig==null) return null;
		
		inCluster.replaceContig ( ctgid, fullContig ); // replacing contigData stub with contigData with data
		
		return inCluster;
	}
	
	public ContigData loadDetail ( String strContigID ) 
    		throws Exception
    {	
        ContigData curContig = new ContigData ();
        try
        {          
            String strQuery = "SELECT " +
            			"CTGID, contigid, consensus, consensus_bases, " +
            			"notes,   gc_ratio, cnt_pairwise,  " +
            			"o_frame, o_coding_start, o_coding_end, o_coding_has_begin, o_coding_has_end, " +
                 	"bestmatchid, cnt_swiss, cnt_trembl, cnt_nt, cnt_gi," +
                 	"cnt_gene, cnt_species,  cnt_overlap, cnt_annodb, user_notes  " +                                                      
                 		"FROM contig " +
                 		"WHERE contig.contigid = '" + strContigID + "' ";

            ResultSet rset = mDB.executeQuery( strQuery );
            if ( !rset.next() ) {
            		throw new RuntimeException ( 
        					"Loading data for sequence  " + strContigID );
            }
           
            int CTGID = rset.getInt(1);
            curContig.setCTGID(CTGID);
            curContig.setContigID( rset.getString( 2 ) );
 
            String seqString = rset.getString(3).trim();
            SequenceData consensus = new SequenceData ("consensus");
            consensus.setName( strContigID );
            consensus.setSequence ( SequenceData.normalizeBases( seqString, '*', Globals.gapCh ) );
            curContig.setSeqData( consensus );
            
            curContig.setConsensusBases( rset.getInt( 4 ) );
 
            curContig.setTCWNotes(rset.getString(5));
          
            curContig.setGCratio(rset.getFloat(6)); 
            curContig.setPairwiseCnt(rset.getInt(7));
         
            CodingRegion coding = new CodingRegion ( CodingRegion.TYPE_LARGEST_ORF );
			coding.setFrame( rset.getInt( 8 ) );
			coding.setBegin( rset.getInt( 9 ) );
			coding.setEnd( rset.getInt( 10 ) );
			coding.setHasBegin( rset.getBoolean(11) );
			coding.setHasEnd( rset.getBoolean(12) );        	
			curContig.setLargestCoding( coding ); 
        
            curContig.setBestMatch(rset.getString(13));
            curContig.setSwissTremblNTCnt(rset.getInt(14), 
            		rset.getInt(15), rset.getInt(16), rset.getInt(17)); 
            curContig.setGeneCntEtc(rset.getInt(18),
              	rset.getInt(19), rset.getInt(20), rset.getInt(21));
            curContig.setUserNotes( rset.getString(22));
            
            boolean hasGO=false;
         	if (mDB.tableExist("go_info") && mDB.tableExist("pja_gotree"))
         		hasGO=true;
         	rset.close();
             
            if (hasGO) {
                ResultSet rset2 = mDB.executeQuery("select count(*) from pja_unitrans_go as ug " +
                		" where ug.direct=1 and ug.ctgid=" + CTGID);
                rset2.next();
                curContig.setCntGO(rset2.getInt(1)); 
                rset2.close();
            }
        }
        catch(Exception e) {
        		ErrorReport.reportError(e, "Error: reading database newLoadConsensusAndData");
        		throw e;
        }
        return curContig;
    }
	// Called by pairsTable.LoadPairFromDB, which only needs sequence and hitID
	public ArrayList <SequenceData>loadSeqHitDataForCtg ( ContigData ctgData) throws Exception
	{
      ResultSet rs = null;
      ArrayList <SequenceData> hitList= new ArrayList <SequenceData> ();
      try
      {
          String ctgID = ctgData.getContigID();
  
          String strQ = 	"SELECT q.DUHID, " +
          		"q.hitID, q.description, q.sequence, q.species, q.dbtype, q.taxonomy, " +
          		"q.kegg, q.pfam, q.ec, q.goBrief, q.interpro, " +  // goBrief, not goList because just want #n from front
          		
          		"t.uniprot_id, t.percent_id, t.alignment_len," +
          		"t.ctg_start, t.ctg_end, t.prot_start, t.prot_end, " +
          		"t.e_value, t.bit_score,  t.blast_rank, t.isProtein, t.filtered, t.best_rank " +
          		
          		"FROM pja_db_unique_hits as q " +
          		"JOIN pja_db_unitrans_hits as t " +
          		"WHERE t.DUHID = q.DUHID " + 
          		"AND   t.contigid = '" + ctgID + "'";
       
          rs = mDB.executeQuery( strQ );
	    	  while( rs.next() )
	    	  {	
	    		  	int duhid = rs.getInt(1);
	    			String hitid = rs.getString(2);
	    			String desc  = rs.getString(3);
	    			String sequence   = rs.getString(4);
	    			String species = rs.getString(5);
	    			String type = rs.getString(6);
	    			String tax = rs.getString(7);
	    			String kegg = rs.getString(8); if (kegg==null) kegg="";
	    			String pfam = rs.getString(9); if (pfam==null) pfam="";
	    			String ec = rs.getString(10);  if (ec==null) ec="";
	    			String go = rs.getString(11); if (go==null) go="";
	    			String interpro = rs.getString(12); if (interpro==null) interpro="";
	    			
	    			String id = rs.getString(13);
	    			String perc = rs.getString(14);
	    			String align = rs.getString(15);
	    			String cstart = rs.getString(16);
	    			String cend = rs.getString(17);
	    			String pstart = rs.getString(18);
	    			String pend = rs.getString(19);
	    			String eval = rs.getString(20);
	    			String bit = rs.getString(21);
	    			String brank = rs.getString(22);
	    			String isProtein = rs.getString(23);
	    			String filter = rs.getString(24);
	    			String rank = rs.getString(25);
	    			
	    			SequenceData seq = new SequenceData ("DB");
	    			seq.setID(duhid);
	    			seq.setName(hitid);
	    			seq.setSequence(sequence);
	    			seq.setDBdesc(desc);
	    			seq.setDBspecies(species);
	    			seq.setDBtype(type);
	    			seq.setDBfiltered(Integer.parseInt(filter));
	    			seq.setGOstuff(go, kegg, pfam, ec, interpro);
	    			
	    			StringBuffer line = new StringBuffer();
	            	line.append(ctgID); 		line.append("\t");            
	            	line.append(id);			line.append("\t");        
	            	line.append(perc);		line.append("\t");        
	            	line.append(align);		line.append("\t");        
	            	line.append(cstart);		line.append("\t");        
	            	line.append(cend);		line.append("\t");        
	            	line.append(pstart);		line.append("\t");        
	            	line.append(pend);		line.append("\t");        
	            	line.append(eval);		line.append("\t");        
	            	line.append(bit); 		line.append("\t");        
	            	line.append("1\t");
	            	line.append(isProtein); line.append("\t");        
	            	line.append(brank); 		line.append("\t");        
	            	line.append(filter);		line.append("\t");        
	            	line.append(type);		line.append("\t");        
	            	line.append(tax);		line.append("\t");        
	            	line.append(species);	line.append("\t");        
	            	line.append(desc);		line.append("\t"); 
	            	line.append(rank);		line.append("\t"); 
	            	BlastHitData hitData = new BlastHitData(BlastHitData.DB_UNITRANS, line.toString());
	            	hitData.setCTGID(ctgData.getCTGID());
	    			seq.setBlastHitData(hitData);
	    			hitList.add(seq);
	    		}
	    	  return hitList;
      }
      catch(Exception e) {
	        	ErrorReport.reportError(e, "Error: reading database loadHitsSeqforContig");
	        	throw e;
      }
      finally 
      {
          if ( rs != null ) rs.close();
      }		
	}
	// used by BasicHitFilter for sequence hit
	public SequenceData loadHitData(String hitID) {
		try {
			String strQ = 	"SELECT  " +
          		"q.DUHID, q.description, q.sequence, q.species, q.dbtype " +
          		"FROM pja_db_unique_hits as q " +     		
          		"WHERE hitID= '" + hitID + "'"; 
          		
			ResultSet rs = mDB.executeQuery( strQ );
			if (!rs.next()) return null;
			
			SequenceData seq = new SequenceData ("DB");
			seq.setID(rs.getInt(1));
			seq.setName(hitID);
			seq.setDBdesc(rs.getString(2));
			seq.setSequence(rs.getString(3));
			seq.setDBspecies(rs.getString(4));
			seq.setDBtype(rs.getString(5));
			return seq;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Reading database for Hit data " + hitID);}		
		return null;
	}
	public BlastHitData loadBlastHitData(String seqID, String hitID) {
		try {
			 String strQ = 	"SELECT t.ctg_start, t.ctg_end, t.isProtein, " +
			 	"t.e_value, t.percent_id, t.alignment_len" +	
		         " FROM  pja_db_unitrans_hits as t " +
		         " WHERE t.uniprot_id = '" + hitID + "'" +
		         " AND   t.contigid = '" + seqID + "'";
			 
          		
			ResultSet rs = mDB.executeQuery( strQ );
			if (!rs.next()) return null;
			
			int start = rs.getInt(1);
			int end = rs.getInt(2);
			boolean isP = rs.getBoolean(3);
			double eval = rs.getDouble(4);
			int pid = rs.getInt(5);
			int align = rs.getInt(6);
			 
			String e = "0.0";
			if (eval!=0) e =  String.format("%.0E", eval); 
			String msg = String.format("Hit: E-value %s, Sim=%d%s, Align=%d", 
					e, pid, "%", align);
			BlastHitData blastData = new BlastHitData(hitID, isP, 
					start, end, msg);
			
			return blastData;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Reading database for Hit data " + hitID);}		
		return null;
	}
	private ContigData loadDetailForContig ( String strContigID ) 
    		throws Exception
    {	
        ContigData curContig = new ContigData ();
        try
        {       
            String strQuery = "SELECT CTGID, contigid, consensus, quality, " +
            			"notes, has_ns, consensus_bases, " + 
            			"numclones, frpairs, est_5_prime, est_3_prime, est_loners, " +
            			"snp_count, indel_count, gc_ratio, cnt_pairwise," +
            			"o_frame, o_coding_start, o_coding_end, o_coding_has_begin, o_coding_has_end,  " +
            			"bestmatchid,  cnt_swiss, cnt_trembl, cnt_nt, cnt_gi," +
            			"cnt_gene, cnt_species,  cnt_overlap, cnt_annodb, user_notes  " +                                                      
                 		"FROM contig " +
                 		"WHERE contig.contigid = '" + strContigID + "' ";

            ResultSet rset = mDB.executeQuery( strQuery );
            if ( !rset.next() ) {
            		throw new RuntimeException ( 
        					"Loading data for sequence  " + strContigID );
            }
           
            int CTGID = rset.getInt(1);
            curContig.setCTGID(CTGID);
            curContig.setContigID( rset.getString( 2 ) );
 
            String seqString = ( getStringFromReader (rset.getCharacterStream( 3 ) ) ).trim();
            Vector<Integer> qualVector = getIntVectorFromReader (rset.getCharacterStream( 4 ) );
            SequenceData consensus = new SequenceData ("consensus");
            consensus.setName( strContigID );
            consensus.setSequence ( SequenceData.normalizeBases( seqString, '*', Globals.gapCh ) );
            consensus.padAndSetQualities ( qualVector );
            curContig.setSeqData( consensus );
 
            curContig.setTCWNotes( rset.getString(5));
            curContig.setHasNs( asBool ( rset.getInt( 6 ) ) );
            curContig.setConsensusBases( rset.getInt( 7 ) );
            curContig.setNumSequences( rset.getInt( 8 ) );          
            
            curContig.setESTMatePairs( rset.getInt( 9 ) );
            curContig.setEST5Prime( rset.getInt( 10 ) );
            curContig.setEST3Prime( rset.getInt( 11 ) );
            curContig.setESTLoners( rset.getInt( 12 ) );
            
            curContig.setSNPCount( rset.getInt( 13 ) );
            curContig.setSNPInDels(rset.getInt(14)); 
            curContig.setGCratio(rset.getFloat(15)); 
            curContig.setPairwiseCnt(rset.getInt(16));
         
            CodingRegion coding = new CodingRegion ( CodingRegion.TYPE_LARGEST_ORF );
			coding.setFrame( rset.getInt( 17 ) );
			coding.setBegin( rset.getInt( 18) );
			coding.setEnd( rset.getInt( 19 ) );
			coding.setHasBegin( rset.getBoolean(20) );
			coding.setHasEnd( rset.getBoolean(21) );        	
			curContig.setLargestCoding( coding ); 
			    
            curContig.setBestMatch(rset.getString(22));
            curContig.setSwissTremblNTCnt(rset.getInt(23), 
            				rset.getInt(24), rset.getInt(25), rset.getInt(26)); 
            curContig.setGeneCntEtc(rset.getInt(27),
              			rset.getInt(28), rset.getInt(29), rset.getInt(30));
            curContig.setUserNotes( rset.getString(31));
                   
            rset.close();
            
            boolean hasGO=false;
         	if (mDB.tableExist("go_info") && mDB.tableExist("pja_gotree")) hasGO=true;
        	
            if (hasGO) {
                ResultSet rset2 = mDB.executeQuery("select count(*) from pja_unitrans_go as ug " +
                		" where ug.direct=1 and ug.ctgid=" + CTGID);
                rset2.next();
                curContig.setCntGO(rset2.getInt(1));
                rset2.close();
            }
        }
        catch(Exception e) {
        		ErrorReport.reportError(e, "Error: reading database newLoadConsensusAndData");
        		throw e;
        }
        return curContig;
    }
	private void loadESTs( ContigData theContig ) throws Exception {
		boolean tGapsExist=mDB.tableColumnExists("contclone", "tgaps");
		boolean extrasExist=mDB.tableColumnExists("contclone", "extras");
		prt("loadESTs");
        String tGapField = "";
        if(tGapsExist) tGapField = ", contclone.tgaps ";
        else if(extrasExist) tGapField = ", contclone.extras ";
        
		String strQuery = "SELECT clone.cloneid, " +
				"clone.Sequence, clone.Quality, clone.Length" + 
				", contclone.LeftPos, contclone.Orient, contclone.Gaps" +
				", buryclone.parentid, contclone.buried " + tGapField +
				"FROM clone JOIN contclone ON contclone.cloneid = clone.cloneid " +
				"LEFT JOIN buryclone ON buryclone.childid = clone.cloneid " + 		
				"WHERE contclone.contigid = '" + theContig.getContigID() + "' ";
		
		ResultSet rset  = mDB.executeQuery(strQuery);
		while( rset.next() ) {	
			SequenceData curClone = new SequenceData ("EST");
			
			curClone.setName ( rset.getString( 1 ) ); // cloneid
			
			String seqString = ( getStringFromReader (rset.getCharacterStream( 2 ) ) ).trim();
			seqString = SequenceData.normalizeBases( seqString, '*', Globals.gapCh );
			Vector<Integer> qualVect = getIntVectorFromReader (rset.getCharacterStream( 3 ) );
			int nSeqLen = rset.getInt( 4 );
			
			int leftPos = rset.getInt( 5 );
			
			boolean bComplement = rset.getString( 6 ).equals ("C");
			
			int[] gapArray = getIntArrayFromReader ( rset.getCharacterStream(7) );
			curClone.setParentName( rset.getString( 8 ) );
			curClone.setBuried( rset.getInt( 9 ) == 1 );
			
			if(tGapsExist) {
				curClone.setTGaps(getIntArrayFromString(rset.getString("contclone.tgaps")));
			}
			else if(extrasExist) {
				curClone.setTGaps(getIntArrayFromString(rset.getString("contclone.extras")));
			}
			else curClone.setTGaps(new int[0]);
			
			if ( nSeqLen != qualVect.size() && qualVect.size()==1)
			{	
				int q = qualVect.get(0);
				for (int i=1; i<nSeqLen; i++) {
					if (seqString.charAt(i) == Globals.gapCh) qualVect.add(i, 0);
					else qualVect.add(i, q);
				}	
			}
			curClone.compAndSet ( seqString, qualVect, bComplement );
			
			// Gaps (gaps that were inserted to match to consensus)
			Arrays.sort ( gapArray );
			for( int i = gapArray.length - 1; i >= 0; --i )
				curClone.insertGapAt ( gapArray[i] + 1 );
			
			// Left position (aligns clone to consensus). This should always be done 
			// after inserting the gaps, since it will change the indexes to be relative to the reference sequence.
			curClone.setLeftPos ( leftPos );
			curClone.setSequenceFromTGaps();
          		
			theContig.addSequence( curClone );		
		}
		rset.close();

		// count buried children under each top-level parent clone
		for (SequenceData sd : theContig.getAllSequences()) {
			if ( sd.getParentName() == null ) // restrict to top-level parents
				sd.setBuriedChildCount( countBuried( sd.getName(), 
						theContig.getAllSequences() ) );
		}	
		//Set the SNP list, if available in the database
		Vector<SNPData> snp = loadSNPData(theContig.getContigID());
		theContig.setSNPs(snp);
	}
	
	private Vector<SNPData> loadSNPData ( String contigID)
	{
        ResultSet rset = null;
        
        Vector<SNPData> retVal = new Vector<SNPData>();
        try
        {
            // Do a test to see if we have data in the SNP table
            String strQ = "SELECT snp.pos, snp.basevars " +                
            				"FROM contig, snp " +
            				"WHERE contig.contigid='" +contigID+"'" +
            				" AND contig.CTGID = snp.CTGID" +
            				" AND snp.snptype = 'Mis'";
            
            rset = mDB.executeQuery ( strQ );
            while(rset.next()) {
	            	SNPData temp = new SNPData(SNPData.TYPE_AMINO_ACIDS);
	            	temp.setMaybeSNP(true);
	            	temp.setPosition(rset.getInt(1));
	            	temp.setValuesFromDBBaseVarsField(rset.getString(2));
	            	retVal.add(temp);
            }
            
            if(retVal.isEmpty())
            		return null;
            return retVal;
        }
        catch ( Exception err ) {
            return null;
        }	
	}
	// addBuriedESTsForContig, addBuriedESTsLocationOnly, loadUnBuriedESTsForContig
	static private int countBuried( String strParentID, 
			Vector<SequenceData> clones ) {
		int count = 0;
		for ( SequenceData sd : clones ) {
			if ( strParentID.equals( sd.getParentName() ) ) {
				count += countBuried( sd.getName(), clones ) + 1;
			}
		}
		
		return count;
	}
	
	 /*******************************************************
     * Database routine
     */
   
 	public void crossIndexCAP3Output(Vector<ContigData> contigList)
 			throws Exception {
		
 		for (int i = 0; i < contigList.size(); ++i) {
 			ContigData contig = (ContigData) contigList.get(i);
 			crossIndexCAP3Output(contig);
 		}
 	}
 	private void crossIndexCAP3Output ( ContigData theContig) throws Exception
	{
		final int CLONE_ID_IDX = 1;	
		final int QUALITY_IDX = 2;		
		final int CONTIG_ID_IDX = 3;		
		
		// Build up a string of the original contig names and ESTs
		String strESTs = theContig.getListOfSequenceNames ("' , '" );
		
		String strQuery = "SELECT clone.cloneid, clone.quality, " +
				"contclone.contigid  " +
				"FROM clone JOIN (contclone, contig) " +
				"ON (contclone.cloneid = clone.cloneid " +
				"AND contclone.contigid = contig.contigid) " +
				"WHERE clone.cloneid IN ( '" + strESTs + "' ) ";

		ResultSet rset  = mDB.executeQuery(strQuery);		
		
		while( rset.next() )
		{	
			String cloneid = rset.getString( CLONE_ID_IDX );
			
			Vector<Integer> qualVect = getIntVectorFromReader (rset.getCharacterStream( QUALITY_IDX ) );
			String contigID = rset.getString( CONTIG_ID_IDX );
						
			// Cross index back to the clone's object in the contig
			SequenceData curClone = theContig.getSequenceByName( cloneid );
			
			if ( curClone.isReverseComplement() )
				Collections.reverse ( qualVect );

			curClone.padAndSetQualities ( qualVect );
			
			curClone.setOldContig( contigID );
		}
		rset.close();
	}
 	
 	// Loads the ests for the input IDs for CAPorPhrap
 	public Vector<SequenceData> loadClones(TreeSet<String> theIDs)
 			throws Exception {
 		TreeSet<String> missingESTs = new TreeSet<String>();
 		missingESTs.addAll(theIDs);
 		Vector<SequenceData> out = new Vector<SequenceData>();

 		
 		Vector<SequenceData> curClones = loadClones(missingESTs.iterator());
 		out.addAll(curClones);

 		// See what clones are left to find
 		for (int i = 0; i < curClones.size(); ++i) {
 			SequenceData curSeq = (SequenceData) curClones.elementAt(i);
 			missingESTs.remove(curSeq.getName());
 		}
 	
 		return out;
 	}
 	 private Vector<SequenceData> loadClones ( Iterator<String> theCloneIDs) 
     		throws Exception
 	{
         ResultSet rset = null;
         try
         {
     		Vector<SequenceData> out = new Vector<SequenceData> ();
             String strCloneIDs = Static.join( theCloneIDs, "', '" );
            
             String strQ = "SELECT clone.cloneid, clone.sequence, clone.quality, " +
 						  "clone.length, clone.libid " +
 						  "FROM clone " +
 						  "WHERE clone.cloneid IN ( '" + strCloneIDs + "' )";
             rset = mDB.executeQuery( strQ );
             
 			while( rset.next() )
 			{		
 				SequenceData curClone = new SequenceData ("EST");
 	
 				String cloneid = rset.getString( "clone.cloneid" );
 				curClone.setName ( cloneid );
 			
 				int nSeqLen = rset.getInt( "clone.length" );
 			
 				Vector<Integer> qualVect = getIntVectorFromReader ( 
 						rset.getCharacterStream( "clone.quality" ) );

 				if ( nSeqLen != qualVect.size() )
 					throw new RuntimeException ( 
 					"Error: Failed to parse the correct number of quality values.");	
 			
 				String seqString = ( getStringFromReader (
 					rset.getCharacterStream( "clone.sequence" ) ) ).trim();
 				seqString = SequenceData.normalizeBases( seqString, '*', Globals.gapCh );
  
     				if ( nSeqLen != seqString.length () )
     					throw new RuntimeException ( 
     						"Error: Failed to parse the correct number of bases." );	
 			
 				curClone.compAndSet ( seqString, qualVect, false );
 			                
 				out.add( curClone );		
 			}
 			return out;
         }
         catch(Exception e) {
         		ErrorReport.reportError(e,  "Error: reading database loadClones");
         		throw e;
         }
         finally 
         {
             if ( rset != null ) rset.close();
         }
 	}

	public String getCAPparams() throws Exception 
	{
        ResultSet rs = null;
        try {
        		
            // ASM_params are (pname, pvalue) pairs
            String strQ = "SELECT pvalue FROM ASM_params " + 
            		" WHERE pname=\"RECAP_ARGS\"";
            rs = mDB.executeQuery ( strQ );
            if ( !rs.next() ) {
            		System.err.println("Error: could not load CAP params");
            		return null;
            }
            String cap =  rs.getString("pvalue");
      		if (rs != null) rs.close();
      		return cap;
        }
        catch ( SQLException err ) {ErrorReport.reportError(err, "Error: reading CAPparms");}    
        return null;
	}
	

	//	----------------CONVERTERS for CLOB fields --------------- //
	static private Vector<Integer> getIntVectorFromReader ( Reader reader ) 
		throws IOException, SQLException 
	{
		Vector<Integer> theVect = new Vector<Integer> ();
		Converters.addIntArray( getIntArrayFromReader ( reader), theVect );
		return theVect;
	}
	
	static private int [] getIntArrayFromString ( String str )
	{
		if(str.equals("")) return null;
		String [] vals = str.split(" ");
		int [] retval = new int[vals.length];
		
		for(int x=0; x<vals.length; x++)
			retval[x] = Integer.parseInt(vals[x]);
		
		return retval;
	}
	
	static private int [] getIntArrayFromReader ( Reader reader ) 
		throws IOException, SQLException 
	{	
		if ( reader == null )
			return new int [0];

		String strString = getStringFromReader ( reader );

		StringTokenizer toker = new StringTokenizer( strString, " \t" );
		int nCount = toker.countTokens();

		int[] IntArray = new int [nCount]; 
		String strInt;
		for ( int i = 0; i < nCount; ++i  )
		{
			strInt = toker.nextToken();
			IntArray[i] = Integer.parseInt( strInt );
		} 
			
		return IntArray;
	}
	
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
			if ( nNumRead <= 0 )
				break;
			
			// Append to the output string
			strReturn += new String ( chChars, 0, nNumRead );
		} while ( true );
			
		return strReturn;
	}
	 private boolean asBool ( int n )
    {
        if ( n == 0 ) return false;
        else return true;
    }
	DBConn mDB=null;
	MetaData metaData = null;
}
