/**
 * Initiates queries for Contig and Pairs tables
 * Was dataholders/QueryData
 * 
 * A 'deepCopy' is made of this in STCWFrame, hence,
 * it has to be Serializable, and didn't like STCWframe variable being private.
 */
package sng.util;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Vector;

import sng.viewer.STCWFrame;
import sng.viewer.panels.pairsTable.FieldPairsData;
import sng.viewer.panels.pairsTable.FieldPairsTab;
import sng.viewer.panels.seqTable.FieldContigData;
import sng.viewer.panels.seqTable.FieldContigTab;
import sng.viewer.panels.seqTable.QueryContigTab;
import util.database.DBConn;

import util.methods.ErrorReport;

public class RunQuery implements Serializable
{		
	private static final long serialVersionUID = -8697940118170126357L;
	private static final int QUERY_INCREMENT = 50000;
	public final static int QUERY_CONTIGS	 	= 0;
	public final static int QUERY_PAIRS 			= 1;
	
	public final static int QUERY_AND = FieldContigData.FILTER_AND;
	public final static int QUERY_OR = FieldContigData.FILTER_OR;

	public RunQuery (int inType ) 
	{
		if (inType==QUERY_PAIRS) pairData = new FieldPairsData();
		else contigData = new FieldContigData();
		
		nType = inType;
	}
	static public RunQuery createAllPairsQuery (  ) 
	{
		RunQuery data = new RunQuery (QUERY_PAIRS);			
		return data;
	}
	static public RunQuery createAllContigQuery (  ) 
	{
		RunQuery data = new RunQuery (QUERY_CONTIGS);			
		return data;
	}
	 /*************************
  	 *  Pairs with filter
  	 */
    public int loadTableRowsForFilterPairs ( STCWFrame f, 
    			FieldPairsTab fTab, FieldMapper mapper, 
    							Vector <String> tableRows) throws Exception
    {	
	    String strSQL = pairData.getPairListSQL(  );
	    if (strSQL==null) return 0;
	    
	    	return runDBQuery(f, strSQL, mapper, null, tableRows);	 
    }

	/*************************
	 *  Contigs with contig list
	 */
    public int loadTableRowsForContigs (STCWFrame f, 
    		FieldContigTab fieldObj,  FieldMapper mapObj , 
    		String [] contigIDs, Vector <String> tableRows )	throws Exception
    {	
		String strSQL = contigData.getSeqListSQL(mapObj, fieldObj, contigIDs);
		if (strSQL==null) return 0;
		 
		return runDBQuery(f, strSQL, mapObj, fieldObj, tableRows);
    }
   
    /*************************
	 *  Contigs with filter
	 */
    public int loadTableRowsForFilterSeq (STCWFrame f,
    			FieldContigTab fieldObj, FieldMapper mapObj, 
    			QueryContigTab queryObj, Vector <String> tableRows) throws Exception
    {			
    		if (contigData==null) {
    			System.err.println("ERROR: contigData not defined in RunQuery");
    			return 0;
    		}
    		
		String strSQL = contigData.getSeqFilterSQL(queryObj, mapObj, fieldObj);
		if (strSQL==null) return 0;
		
		return runDBQuery(f, strSQL, mapObj, fieldObj, tableRows);
    }
    public String getContigSummary() { return contigData.getSummary();}
    public String getPairsSummary() { return pairData.getSummary();}
   
    /**
     * Runs query. --fieldObj is currently null for pairs query
     */
    private int runDBQuery (STCWFrame f, String strQuery,
					FieldMapper mapper, FieldContigTab fieldObj, Vector <String> tableRows) 
    throws Exception
	{    	
    		int count = 0; 
    		ResultSet rs=null;
    		DBConn mdb = null;
    		
		if (strQuery==null || strQuery.length()==0) {
			System.err.println("Error: the SQL query string is empty");
			return 0; 
		}

		// Load the filter list of contigs
		try {
			
			mdb = f.getNewDBC();
			int pos = 0;
			boolean readMoreData = true;
			
			while(readMoreData) {
				readMoreData = false;
				// XXX Executes Query here!
				rs = mdb.executeQuery(strQuery + " LIMIT " + pos + ", " + QUERY_INCREMENT);

				pos += QUERY_INCREMENT;

				while (rs.next()) {
					readMoreData = true;
					String row = null;
	
					if (nType == QUERY_CONTIGS) 
						row = mapper.getObjFromSeqResultSet( rs, count+1, fieldObj);
					else
					 	row = mapper.getObjFromPairResultSet( rs, count+1);
							
					if (row == null) continue;
					count++; 
					tableRows.add(row);
				}
			}
		} 
		catch (Exception e) {
			ErrorReport.prtReport(e, "sTCW Error running query\n" + strQuery);
			count=-1;
		}  finally {
			if (rs != null) rs.close();
			if (mdb != null) mdb.close();
		}
		return count; 
	}
	
	public FieldPairsData getPairsData ( ) { return pairData; }	
 	public void setPairsData(FieldPairsData pd) {pairData=pd;}
 	
 	public FieldContigData getContigData ( ) { return contigData; }	
 	public void setContigData(FieldContigData pd) {contigData=pd;}
 	
 	public int getType() { return nType; }
	public void setType(int type) { nType = type; }
	public boolean isAllContigs ( ){return nType == QUERY_CONTIGS;} // && contigFields.isAllContigs();
	public boolean isAllContigPairs ( ){return nType == QUERY_PAIRS;}
	
	/************ variables ******************/
	private int nType = QUERY_CONTIGS; 
	private FieldContigData contigData = null;
	private FieldPairsData pairData = null;
}
