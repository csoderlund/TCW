package sng.viewer.panels.pairsTable;

import java.io.Serializable;

import sng.util.FieldMapper;


public class FieldPairsData implements Serializable {
	private static final long serialVersionUID = 6308189781762719498L;
	
	final static public int RECORD_NUM_FIELD = 900;
	final static public int FIELD_ID_CONTIG_1 	= 1000;
	final static public int FIELD_ID_CONTIG_2 	= 1001;
	final static public int FIELD_ID_SHAREDHIT 	= 1006;
	
    final static public int FIELD_BLAST_E_VALUE = 1051;
	final static public int FIELD_BLAST_SIM =  1052;
	final static public int FIELD_BLAST_LEN =  1053;
	
	final static public int FIELD_ID_NT_OLAP_PID 	= 2006;
	final static public int FIELD_ID_NT_OLAP_MM	 	= 2007;
	final static public int FIELD_ID_NT_OLAP_LEN 	= 2008;
	final static public int FIELD_ID_NT_OLAP_RATIO 	= 2009;	
	final static public int FIELD_ID_AA_OLAP_PID 	= 2010;
	final static public int FIELD_ID_AA_OLAP_MM	 	= 2011;
	final static public int FIELD_ID_AA_OLAP_LEN 	= 2012;
	final static public int FIELD_ID_AA_OLAP_RATIO 	= 2013;
	
    // Don't use a group names already used by Contig Columns (e.g. General)
    // if you change, also change in FieldPairTab.createUIFromFields
    final static public String GROUP_NAME_PAIR = "Sequence information";
    final static public String GROUP_DESC_PAIR = "Information about the sequences in a pair.";
    
    final static public String GROUP_NAME_BLAST =  "Blast results for the pair";
    final static public String GROUP_DESC_BLAST =  "Blast results for the HSR (highest scoring region) of the pair.";
   
    final static public String GROUP_NAME_OLAP = "Total coverage";
    final static public String GROUP_DESC_OLAP = "The total coverage between the pair from dynamic programming.";
 
    static public FieldMapper createPairFieldMapper ( )
    {
        FieldMapper mapper = new FieldMapper ("Pairs");
        
        // General
        mapper.addIntField ( RECORD_NUM_FIELD, "Pair #", null, "PWID", 
        		GROUP_NAME_PAIR, GROUP_DESC_PAIR, 
        		"The index of the query result." );
        
        mapper.addStringField( FIELD_ID_CONTIG_1, "Seq ID 1", null, "contig1", 
        		GROUP_NAME_PAIR, GROUP_DESC_PAIR, 
        		"Identifier for the first sequence in the comparison." );
        mapper.addStringField( FIELD_ID_CONTIG_2, "Seq ID 2", null, "contig2", 
        		GROUP_NAME_PAIR, GROUP_DESC_PAIR, 
        		"Identifier for the second sequence in the comparison." );
        
        mapper.addStringField( FIELD_ID_SHAREDHIT , "Shared Hit", null, "shared_hitID", 
        		GROUP_NAME_PAIR, GROUP_DESC_PAIR, 
        		"The best shared hit between the two sequences (blank if none)." ); 
        
        // BLAST
        mapper.addFloatField( FIELD_BLAST_E_VALUE, "Bt E-value", null, "e_value", 
        		GROUP_NAME_BLAST, GROUP_DESC_BLAST, 
        		"Blast E-value of best HSP." ); 
       
        mapper.addIntField( FIELD_BLAST_SIM, "Bt %Sim", null, "percent_id", 
        		GROUP_NAME_BLAST, GROUP_DESC_BLAST, 
        		"Blast %similarity of best HSP." );
        mapper.addIntField( FIELD_BLAST_LEN, "Bt Align", null, "alignment_len", 
        		GROUP_NAME_BLAST, GROUP_DESC_BLAST, 
        		"Blast alignment length of best HSP." );
        
        // DP NT OLAP 
        // I don't think these equation do anything here, as their explicit in getPairListSQL
        String nt_olp_pid = "(NT_olp_score/NT_olp_len)"; 
        mapper.addPercentField( FIELD_ID_NT_OLAP_PID, "NT %Sim", null, null, 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Percent similarity in the NT coverage ((Match/Align)*100.0)." );  
        mapper.setFieldSubQuery( FIELD_ID_NT_OLAP_PID, "NT_olp_pid", nt_olp_pid);
        
        String nt_olp_mm = "(NT_olp_len-NT_olp_score)";
        mapper.addIntField( FIELD_ID_NT_OLAP_MM, "NT MisMatch", null, null, 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Number of nucleotide mis-matches." );  
        mapper.setFieldSubQuery( FIELD_ID_NT_OLAP_MM, "NT_olp_mm", nt_olp_mm);
        
        mapper.addIntField( FIELD_ID_NT_OLAP_LEN, "NT Align", null, "NT_olp_len", 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Alignment length of NT coverage (overhangs at end ignored)." );   
        
        mapper.addPercentField( FIELD_ID_NT_OLAP_RATIO, "NT %Len", null, "NT_olp_ratio", 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Nucleotide alignment divided by the longest of the two sequences." ); 
        
        // DP AA OLAP 
        String aa_olp_pid = "(AA_olp_score/AA_olp_len)";
        mapper.addPercentField( FIELD_ID_AA_OLAP_PID, "AA %Sim", null, null, 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Percent similarity in the AA alignment ((Match/Align)*100.0)." );
        mapper.setFieldSubQuery( FIELD_ID_AA_OLAP_PID, "AA_olp_pid", aa_olp_pid);      

        String aa_olp_mm = "(AA_olp_len-AA_olp_score)";
        mapper.addIntField( FIELD_ID_AA_OLAP_MM, "AA MisMatch", null, null, 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Number of Amino acid mis-matches (based on Blosum matrix scores < 0)." );  
        mapper.setFieldSubQuery( FIELD_ID_AA_OLAP_MM, "AA_olp_mm", aa_olp_mm);
        
        mapper.addIntField( FIELD_ID_AA_OLAP_LEN, "AA Align", null, "AA_olp_len", 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Alignment length of amino acids coverage (overhangs at end ignored)." ); 
        
        mapper.addPercentField( FIELD_ID_AA_OLAP_RATIO, "AA %Len", null, "aa_olp_ratio", 
        		GROUP_NAME_OLAP, GROUP_DESC_OLAP, 
        		"Amino acid coverage divided by the longest of the two sequences." );
        
        mapper.setDefaultFieldIDs ( new int [] { 	RECORD_NUM_FIELD,
        			FIELD_ID_CONTIG_1, FIELD_ID_CONTIG_2, FIELD_BLAST_E_VALUE ,
        			FIELD_ID_NT_OLAP_PID, FIELD_ID_AA_OLAP_PID } );
        
        mapper.setFieldRequired( RECORD_NUM_FIELD );
        mapper.setFieldRequired( FIELD_ID_CONTIG_1 );
        mapper.setFieldRequired( FIELD_ID_CONTIG_2 );
        return mapper;
    }
    /*****************************************************
     * Create mysql query string
     */
    public void setWhereClause(String w) { whereClause = w;}
    public String getPairListSQL ( )         
	{	
    		// the order does not matter because FieldMapper reads using column names.
    		// am getting all columns, only need selected, but need the FieldMapper object....
        String sql = "SELECT " +
        				"PWID, contig1, contig2, " +
	      			"(NT_olp_score/NT_olp_len) as NT_olp_pid, " +
	      			"(NT_olp_len-NT_olp_score) as NT_olp_mm, " +
	      			"NT_olp_ratio, NT_olp_len, " +
					"(AA_olp_score/AA_olp_len) as AA_olp_pid, " +
					"(AA_olp_len-AA_olp_score) as AA_olp_mm, " +
					"AA_olp_ratio, AA_olp_len, " +
					" e_value, percent_id, alignment_len, shared_hitID " + 
        				" FROM pja_pairwise where " + whereClause;
         return sql;
	}
    public String getSummary() {return summary;}
    public void setSummary(String s) {summary=s;}
    private String summary = "All pairs";
	private String whereClause = "1";
}
