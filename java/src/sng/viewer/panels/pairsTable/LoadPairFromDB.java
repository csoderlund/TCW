package sng.viewer.panels.pairsTable;

import java.sql.ResultSet;
import java.util.ArrayList;

import sng.database.MetaData;
import sng.dataholders.BlastHitData;
import sng.dataholders.CodingRegion;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.dataholders.SequenceData;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

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
			ArrayList <SequenceData> list1 = loadSeqHitDataForCtg(ctg1Obj);
			ctg1Obj.setSeqDataHitList(list1);
			ArrayList <SequenceData> list2 = loadSeqHitDataForCtg(ctg2Obj);
			ctg2Obj.setSeqDataHitList(list2);
		}
		return pairCtgObj;
	}
	private ContigData loadSequence(String strContigID) {
		ResultSet rs = null;
        ContigData curContig = new ContigData ();
        curContig.setContigID( strContigID );
        try
        {          
            String strQuery = "SELECT CTGID, consensus, bestmatchid,  " +
            		"o_frame, o_coding_start, o_coding_end, o_coding_has_begin, o_coding_has_end " +
                 	"FROM contig WHERE contig.contigid = '" + strContigID + "' ";

            rs = mDB.executeQuery( strQuery );
            if ( !rs.next() ) Out.die("Loading data for sequence  " + strContigID );
           
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
        catch(Exception e) {ErrorReport.reportError(e, "Error: reading database for pairs sequence");}
        return curContig;
	}
	public BlastHitData loadPairHitData(String ctg1, String ctg2)  {
        ResultSet rset = null;
        String str="";
        try
        {
            String strQ = "SELECT  e_value, percent_id, alignment_len, " +
            	"align_frame1, align_frame2, ctg1_start, ctg1_end, ctg2_start, ctg2_end, " +
            	"shared_hitID, hit_type FROM pja_pairwise ";
            
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
    			rset.getInt("align_frame1") + "\t" + 
    			rset.getInt("align_frame2") + "\t" +
    			
    			rset.getString("shared_hitID") + "\t" +
    			rset.getString("hit_type");
        		rset.close();
        		return new BlastHitData(BlastHitData.DB_PAIRWISE, str);	 
            }
            // ctg2 before ctg1
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
        		
				rset.getInt("align_frame2") + "\t" +  // put these in opposite, so right order
        		rset.getInt("align_frame1") + "\t" + 
        			
				rset.getString("shared_hitID") + "\t" +
				rset.getString("hit_type");
        		rset.close();
            	return new BlastHitData(BlastHitData.DB_PAIRWISE, str);	 
            }
            Out.bug("Internal error: no database pair for " + ctg1 + " " + ctg2);
            return null;
        }
        catch ( Exception err ) {
	        ErrorReport.reportError(err,"Error: reading database for getPairFrames");
	        return null;
        }
	}
	// CAS314 moved from SeqDetail.loadFromDB
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
}
