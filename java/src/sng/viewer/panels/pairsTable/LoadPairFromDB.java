package sng.viewer.panels.pairsTable;

import java.sql.ResultSet;
import java.util.ArrayList;

import sng.database.MetaData;
import sng.dataholders.BlastHitData;
import sng.dataholders.CodingRegion;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.dataholders.SequenceData;
import sng.viewer.panels.seqDetail.LoadFromDB;
import util.database.DBConn;
import util.methods.ErrorReport;

public class LoadPairFromDB {
	public LoadPairFromDB(DBConn d, MetaData m) {mDB=d; metaData = m;}
	
	DBConn mDB = null;
	MetaData metaData = null;
	
	public MultiCtgData loadTwoContigs ( MultiCtgData pairCtgObj) throws Exception		
	{	
		String ctg1 = pairCtgObj.getContigAt(0).getContigID();
		String ctg2 = pairCtgObj.getContigAt(1).getContigID();
		
		ContigData ctg1Obj = loadSequence(ctg1);
		if ( ctg1Obj != null )  pairCtgObj.replaceContig ( ctg1, ctg1Obj );
		
		ContigData ctg2Obj = loadSequence(ctg2);
		if ( ctg2Obj != null )  pairCtgObj.replaceContig ( ctg2, ctg2Obj );
		
		String best1= ctg1Obj.getBestMatch();
		String best2= ctg1Obj.getBestMatch();
		if (best1!=null && best1!="" && best2!=null && best2!="") {
			LoadFromDB loadObj = new LoadFromDB(mDB, metaData);
			ArrayList <SequenceData> list1 = loadObj.loadSeqHitDataForCtg(ctg1Obj);
			ctg1Obj.setSeqDataHitList(list1);
			ArrayList <SequenceData> list2 = loadObj.loadSeqHitDataForCtg(ctg2Obj);
			ctg2Obj.setSeqDataHitList(list2);
		}
		return pairCtgObj;
	}
	private ContigData loadSequence(String strContigID) throws Exception {
		ResultSet rs = null;
        ContigData curContig = new ContigData ();
        curContig.setContigID( strContigID );
        try
        {          
            String strQuery = "SELECT CTGID, consensus, bestmatchid,  " +
            		"o_frame, o_coding_start, o_coding_end, o_coding_has_begin, o_coding_has_end " +
                 		"FROM contig " +
                 		"WHERE contig.contigid = '" + strContigID + "' ";

            rs = mDB.executeQuery( strQuery );
            if ( !rs.next() ) {
            		throw new RuntimeException ( 
        					"Loading data for sequence  " + strContigID );
            }
           
            curContig.setCTGID(rs.getInt(1));
            
            String seqString = rs.getString(2);
            SequenceData seqObj = new SequenceData ("consensus");
            seqObj.setName( strContigID );
            // CAS313 seqObj.setSequence ( SequenceData.normalizeBases( seqString, '*', Globals.gapCh ) );
            seqObj.setSequence(seqString);
            curContig.setSeqData( seqObj );
            
            curContig.setBestMatch(rs.getString(3));
            
            CodingRegion coding = new CodingRegion ( CodingRegion.TYPE_LARGEST_ORF );
			coding.setFrame( rs.getInt( 4 ) );
			coding.setBegin( rs.getInt( 5 ) );
			coding.setEnd( rs.getInt( 6) );
			coding.setHasBegin( rs.getBoolean(7) );
			coding.setHasEnd( rs.getBoolean(8) );        	
			curContig.setLargestCoding( coding ); 
			
            rs.close();
        }
        catch(Exception e) {
        		ErrorReport.reportError(e, "Error: reading database for pairs sequence");
        		throw e;
        }
        return curContig;
	}
	public BlastHitData loadPairHitData(String ctg1, String ctg2) throws Exception {
	        ResultSet rset = null;
	        String str="";
	        try
	        {
	            String strQ = "SELECT  e_value, percent_id, alignment_len, " +
	            		"coding_frame1, coding_frame2, " +
	            		"ctg1_start, ctg1_end, ctg2_start, ctg2_end, " +
	            		"shared_hitID, in_self_blast_set, in_translated_self_blast " +
	            		"FROM pja_pairwise ";
	            
	            rset = mDB.executeQuery ( strQ + "WHERE contig1 = '" + ctg1 + "' and contig2 = '" + ctg2 + "' ");
	            if (rset.next()) {
	            		str = 
	            			ctg1 + "\t" +
	            			ctg2 + "\t" +
	            			rset.getDouble("e_value") + "\t" + 
	            			rset.getDouble("percent_id") + "\t" + 
	            			rset.getInt("alignment_len") + "\t" + 
	            			
	            			rset.getInt("ctg1_start") + "\t" + 
	            			rset.getInt("ctg1_end") + "\t" + 
	            			rset.getInt("ctg2_start") + "\t" + 
	            			rset.getInt("ctg2_end") + "\t" + 
	            			rset.getInt("coding_frame1") + "\t" + 
	            			rset.getInt("coding_frame2") + "\t" +
	            			
	            			rset.getString("shared_hitID") + "\t" +
	            			rset.getInt("in_self_blast_set") + "\t" +
	            			rset.getInt("in_translated_self_blast");
	            		 return new BlastHitData(BlastHitData.DB_PAIRWISE, str);	 
	            }
	            // this never seems to happen. 
	            rset = mDB.executeQuery ( strQ + "WHERE contig1 = '" + ctg2 + "' and contig2 = '" + ctg1 + "' ");
	            if (rset.next()) {
	            		str = 
	            			ctg1 + "\t" +
	            			ctg2 + "\t" +
	            			rset.getDouble("e_value") + "\t" + 
	            			rset.getDouble("percent_id") + "\t" + 
	            			rset.getInt("alignment_len") + "\t" + 
	            			rset.getInt("ctg2_start") + "\t" + 
	            			rset.getInt("ctg2_end") + "\t" + 
	            			rset.getInt("ctg1_start") + "\t" + 
	            			rset.getInt("ctg1_end") + "\t" + 
	            		
						rset.getInt("coding_frame2") + "\t" +  // put these in opposite, so right order
	            			rset.getInt("coding_frame1") + "\t" + 
	            			
						rset.getString("shared_hitID") + "\t" +
	            			rset.getInt("in_self_blast_set") + "\t" +
	            			rset.getInt("in_translated_self_blast"); 
	            		 return new BlastHitData(BlastHitData.DB_PAIRWISE, str);	 
	            }
	            System.err.println("Internal error: no database pair for " + ctg1 + " " + ctg2);
	            return null;
	        }
	        catch ( Exception err ) {
		        	ErrorReport.reportError(err,"Error: reading database for getPairFrames");
		        	return null;
	        }
	        finally {
	        		if ( rset != null ) rset.close();
	        }
	}
}
