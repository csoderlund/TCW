/**
 * Can hold multiple contigs
 * The ContigData never has anything in it except the name.
 */
package sng.dataholders;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.Serializable;

public class MultiCtgData implements Serializable
{
	public MultiCtgData ()
	{
		contigMap = new TreeMap<String, ContigData> ();
	}

	public MultiCtgData (String ctg)
	{
		contigMap = new TreeMap<String, ContigData> ();
		ContigData stub = new ContigData ();
	    stub.setContigID( ctg);
	    addContig ( stub );
	}
	/**
	 * PairTopRowTab and PairListTab
	 */
	public MultiCtgData (String ctg1, String ctg2) 
	{
		contigMap = new TreeMap<String, ContigData> ();
		ContigData stub = new ContigData ();
	    stub.setContigID( ctg1);
	    addContig ( stub );
	        
	    ContigData stub2 = new ContigData ();
	    stub2.setContigID( ctg2);
	    addContig ( stub2 );
	}
	/********************************************
	 * CAP3
	 */
    public void addContigStub ( String strContigID )
    {
        ContigData stub = new ContigData ();
        stub.setContigID( strContigID);
        addContig ( stub );
    }
    
    /**
     * MainData.addContigStub
     * STCWFrame.addNewContig
     * CAP3AceFileReader.readClusterFrom
     */
	public void addContig ( ContigData theContig )
	{
		if (theContig==null) {
			System.err.println("Internal Error: MultCtgData getting sequence");
			return;
		}
		String ctgID = theContig.getContigID();
		if (ctgID==null || ctgID.equals("")) {
			System.err.println("Internal Error: MultCtgData getting sequence name");
			return;
		}
		
		if ( theContig.getNumSequences() == 1 )++nNumSingletons;
		else ++nNumContigs;
		
		if ( contigMap.put( ctgID, theContig ) != null )
			throw new RuntimeException ( "Found two contigs named " + theContig.getContigID() + "." );
	}
	// PairwiseAlignmentData
	public ContigData findContig ( String strID )
	{
		return contigMap.get( strID );
	}
	
	public void replaceContig ( String strID, ContigData newContig )
	{
		contigMap.put(strID, newContig);
	}
	
	public TreeSet<String> getContigIDSet ()
	{
		TreeSet<String> returnSet = new TreeSet<String> ();
		
		Iterator<Map.Entry<String, ContigData>> iter = contigMap.entrySet().iterator();
		while ( iter.hasNext() )
		{
			Map.Entry<String, ContigData> entry = iter.next ();
			ContigData curContig = entry.getValue();
			returnSet.add( curContig.getContigID() );
		}			
		return returnSet;
	}
	// MultiAlignmentPanel.createESTToolbar
	public ContigData getContigAt ( int i )
	{
		if ( i < 0 || i > getTotalContigs () ) return null;
		
		Iterator<ContigData> iter = getContigIterator ();
		ContigData returnContig = null;
		for ( int j = 0; j <= i; ++j ) returnContig = iter.next();	
		
		return returnContig;
	}
	
	public String getCtgIDAt ( int i )
	{
		if ( i < 0 || i > getTotalContigs () )
			return null;
		
		Iterator<ContigData> iter = getContigIterator ();
		ContigData returnContig = null;
		for ( int j = 0; j <= i; ++j )
			returnContig = iter.next();	
		
		return returnContig.getContigID();
	}
	
	public ContigData getContig() {
		if (contigMap==null || contigMap.size()==0) return null;

		String key = contigMap.firstKey();
		return contigMap.get(key);
	}
	public Iterator<ContigData> getContigIterator ( ) { 
		return contigMap.values().iterator(); 
	}		
	public int getTotalContigs ( ) { 
		return nNumSingletons + nNumContigs; 
	};	
	public void clear()
	{
		if(contigMap != null) contigMap.clear();
	}
	
	// The blast hit for a pair is stored in the database, and read into BlastHitData.
	// This makes it available to AlignmentData.
	public BlastHitData getPairHit() { return pairHit;}
	public void setPairHit(BlastHitData b) {pairHit = b;}
	private BlastHitData pairHit = null;
	
	private int nNumSingletons = 0;
	private int nNumContigs = 0;
	
	private TreeMap<String, ContigData> contigMap = null;
	
    private static final long serialVersionUID = 1;
}