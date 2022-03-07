/**
 * Defines Contig table columns
 * Links columns to their corresponding database fields
 * The field mapper for Pairs is in QueryData
 * A query is done with the filters, just obtaining the contigid of included set
 */
package sng.viewer.panels.seqTable;

import java.io.Serializable;

import sng.database.Globals;
import sng.database.MetaData;
import sng.util.FieldMapper;
import sng.viewer.STCWFrame;
import util.database.Globalx;

public class FieldSeqData implements Serializable
{	
	private static final long serialVersionUID = 930670111826862169L;

	public FieldSeqData(){}
	
	public static final int FILTER_NONE = -1;
	public static final int FILTER_OR = 0;
	public static final int FILTER_AND = 1;
	
	public static final int FILTER_INCLUDE = FILTER_AND; 
	public static final int FILTER_EXCLUDE = FILTER_OR;
	
	private static final String L = Globals.LIBCNT;
	private static final String LN = Globals.LIBRPKM;
	private static final String PVAL = Globals.PVALUE;
	
	// Number in same order as listed on Select columns; columns will display in this order
    public static final int SEQ_IDX_FIELD 		= 900;   
    public static final int SEQ_ID_FIELD 			= 901; 
    public static final int TOTAL_EXP_FIELD			= 1001;
    public static final int SEQ_LENGTH_FIELD 		= 1002;
 
    public static final int NUMBER_EST_FIELD 		= 1003;
    public static final int NUMBER_BURIED_FIELD 	= 1004;	
    public static final int NUMBER_UNBURIED_FIELD 	= 1005;	
    
    public static final int HAS_NS_ID_FIELD 		= 1006;
    public static final int EST_MATE_PAIRS_FIELD 	= 1007;  
    public static final int EST_LONERS_FIELD 		= 1008;  
    public static final int EST_5_PRIME_FIELD 		= 1009;  
    public static final int EST_3_PRIME_FIELD		= 1010;  
    
    public static final int LONGEST_EST_FIELD 		= 1011;
    public static final int CNT_NS_FIELD			= 1012;
    public static final int GC_RATIO_FIELD 			= 1013;	
    public static final int NOTES_FIELD				= 1014;
    public static final int USER_NOTES_FIELD		= 1015;
    
    public static final int PAIRWISE_FIELD			= 1016;
    
    public static final int SEQ_GROUP				= 1020;
    public static final int SEQ_NGROUP				= 1021;
    public static final int SEQ_START				= 1022;
    public static final int SEQ_END					= 1023;
    public static final int SEQ_STRAND				= 1024;
   
    public static final int CNT_TAXO				= 1030;
    public static final int CNT_SPECIES				= 1031;
    public static final int CNT_GENE				= 1032;
    public static final int CNT_OVERLAP				= 1033;
    public static final int CNT_SWISS				= 1034;
    public static final int CNT_TREMBL				= 1035;
    public static final int CNT_NCBI				= 1036;
    public static final int CNT_NT					= 1037;
    
    //since # of libraries is dynamic, can only label the start of the range
    public static final int LIBRARY_COUNT_ALL		= 1100; // Can have 400 libs
    public static final int LIBRARY_TPM_ALL			= 1500; // Can have 400 tpm
    
    public static final int CONTIG_SET_COUNT 		= 1900;
    
    public static final int N_FOLD_LIB				= 2700;
    public static final int N_FOLD_LIB_LIMIT		= 2899; // allows 200 N-FOLD fields
    
    public static final int RSTAT_FIELD				= 2900;
    public static final int P_VALUES				= 2901;
    
    public static final int SNP_COUNT_FIELD 		= 3002;  
    public static final int SNP_INDEL_FIELD			= 3003;
    
    public static final int FIRST_BEST_UNIPROT_FIELD 	= 3004;
    public static final int FIRST_BEST_DESCRIPTION		= 3005;
    public static final int FIRST_BEST_SPECIES			= 3006;
    public static final int FIRST_BEST_DBTYPE_FIELD		= 3007;
    public static final int FIRST_BEST_TAXO_FIELD		= 3008;
    public static final int FIRST_BEST_EVALUE_FIELD 	= 3009;
    public static final int FIRST_BEST_BIT_SCORE_FIELD 	= 3010;
    public static final int FIRST_BEST_PERCENT_IDENT	= 3011;
    public static final int FIRST_BEST_ALIGN_LEN_FIELD 	= 3012;
    public static final int FIRST_BEST_CTG_START_FIELD	= 3013;
    public static final int FIRST_BEST_CTG_END_FIELD	= 3014;
    public static final int FIRST_BEST_CTG_COV_FIELD	= 3015;
    public static final int FIRST_BEST_PRO_START_FIELD	= 3016;
    public static final int FIRST_BEST_PRO_END_FIELD	= 3017;
    public static final int FIRST_BEST_PRO_COV_FIELD	= 3018;
    public static final int FIRST_BEST_KEGG 			= 3019;
    public static final int FIRST_BEST_PFAM 			= 3020;
    public static final int FIRST_BEST_EC 				= 3021;
    public static final int FIRST_BEST_GO 				= 3022;
    public static final int FIRST_BEST_INTERPRO 		= 3023;
    
    public static final int OVER_BEST_UNIPROT_FIELD 	= 3030;
    public static final int OVER_BEST_DESCRIPTION		= 3031;
    public static final int OVER_BEST_SPECIES			= 3032;
    public static final int OVER_BEST_DBTYPE_FIELD		= 3033;
    public static final int OVER_BEST_TAXO_FIELD		= 3034;
    public static final int OVER_BEST_EVALUE_FIELD 		= 3035;
    public static final int OVER_BEST_BIT_SCORE_FIELD 	= 3036;
    public static final int OVER_BEST_PERCENT_IDENT		= 3037;
    public static final int OVER_BEST_ALIGN_LEN_FIELD 	= 3038;
    public static final int OVER_BEST_CTG_START_FIELD	= 3039;
    public static final int OVER_BEST_CTG_END_FIELD		= 3040;
    public static final int OVER_BEST_CTG_COV_FIELD		= 3041;
    public static final int OVER_BEST_PRO_START_FIELD	= 3042;
    public static final int OVER_BEST_PRO_END_FIELD		= 3043;
    public static final int OVER_BEST_PRO_COV_FIELD		= 3044;
    public static final int OVER_BEST_KEGG 				= 3045;
    public static final int OVER_BEST_PFAM 				= 3046;
    public static final int OVER_BEST_EC 				= 3047;
    public static final int OVER_BEST_GO 				= 3048;
    public static final int OVER_BEST_INTERPRO 			= 3049;
    
    public static final int GO_BEST_UNIPROT_FIELD 		= 3050;
    public static final int GO_BEST_DESCRIPTION			= 3051;
    public static final int GO_BEST_SPECIES				= 3052;
    public static final int GO_BEST_DBTYPE_FIELD		= 3053;
    public static final int GO_BEST_TAXO_FIELD			= 3054;
    public static final int GO_BEST_EVALUE_FIELD 		= 3055;
    public static final int GO_BEST_BIT_SCORE_FIELD 	= 3056;
    public static final int GO_BEST_PERCENT_IDENT		= 3057;
    public static final int GO_BEST_ALIGN_LEN_FIELD 	= 3058;
    public static final int GO_BEST_CTG_START_FIELD		= 3059;
    public static final int GO_BEST_CTG_END_FIELD		= 3060;
    public static final int GO_BEST_CTG_COV_FIELD		= 3061;
    public static final int GO_BEST_PRO_START_FIELD		= 3062;
    public static final int GO_BEST_PRO_END_FIELD		= 3063;
    public static final int GO_BEST_PRO_COV_FIELD		= 3064;
    public static final int GO_BEST_KEGG 				= 3065;
    public static final int GO_BEST_PFAM 				= 3066;
    public static final int GO_BEST_EC 					= 3067;
    public static final int GO_BEST_GO 					= 3068;
    public static final int GO_BEST_INTERPRO 			= 3069;
    
    public static final int ORF_CODING_FRAME 		= 4002;
    public static final int ORF_CODING_LENGTH 		= 4004; 
    public static final int ORF_HAS_START 			= 4007; 
    public static final int ORF_HAS_END 			= 4008; 
    public static final int ORF_START 				= 4009; 
    public static final int ORF_END 				= 4010; 
    public static final int ORF_MARKOV				= 4011;
    
    public static final int PROTEIN_FRAME 			= 5002; 
    public static final int USED_PROTEIN_FRAME 		= 5004; 
 
    public static final String GROUP_NAME_CONTIG 	= "General";
    public static final String GROUP_DESC_CONTIG 	= "General attributes";

    public static final String GROUP_NAME_SEQ_SET	= "Sequence Set";
    public static final String GROUP_DESC_SEQ_SET	= "Sequences with counts";
    
    public static String GROUP_NAME_LIB		= "Counts and TPM"; // TPM may be replaced with RPKM
    public static String GROUP_DESC_LIB		
    		= "Counts and/or TPM per condition (or assembled sequences).";
    
    public static final String GROUP_NAME_RSTAT		= "Fold-change and Rstat";
    public static final String GROUP_DESC_RSTAT 	
    		= "Log2FC or FC between two conditions and R-statistic over all conditions";

    public static final String GROUP_NAME_PVAL		= "Differential Expression";
    public static final String GROUP_DESC_PVAL 		= "Columns of DE p-values";
    
    public static final String GROUP_NAME_CNTS		= "Counts of annoDB hits";
    public static final String GROUP_DESC_CNTS 		= "Counts of species, minimal coverage, SwissProt, TrEMBL, NCBI and NT";
    
    // The three BEST are grouped together. So these names are only used for grouping
    public static final String GROUP_NAME_FIRST_BEST = "Annotation";
    public static final String GROUP_DESC_FIRST_BEST = "Best Bit-score, Best Annotation, Best with GO";
    
    public static final String GROUP_NAME_OVER_BEST 	= "Best Annotation";
    public static final String GROUP_DESC_OVER_BEST 	= "annoDB hit with the best annotation (i.e. excluding phrases such as \'uncharacterized protein\', see Help)";
    
    public static final String GROUP_NAME_GO_BEST 	= "Best Hit with GO";
    public static final String GROUP_DESC_GO_BEST 	= "annoDB hit with best e-value and assigned GO ID";
    
    public static final String GROUP_NAME_SNPORF 	= "SNPs and ORFs";
    public static final String GROUP_DESC_SNPORF 	= "Information on SNPs (if assembled) and best ORF";

    public static final String TABLE_FIRST_BEST 		= "firstbest";
    public static final String TABLE_OVER_BEST			= "overbest";
    public static final String TABLE_GO_BEST			= "gobest";
    
    public static final String TABLE_FIRST_BEST_DETAIL 	= "firstbestdetail";
    public static final String TABLE_OVER_BEST_DETAIL	= "overbestdetail";
    public static final String TABLE_GO_BEST_DETAIL		= "gobestdetail";
    
    /**
     * Defines the columns in the Contig Table
     * the return Mapper has all the columns defined, so used hence-forth
     */
    static public FieldMapper createSeqFieldMapper (STCWFrame parentFrame)
    {
    	SeqQueryTab qTab = parentFrame.getQueryContigTab();
    	MetaData metaData = parentFrame.getMetaData();
    	
    	String norm = metaData.getNorm();
    	if (norm.contentEquals("TPM")) { // CAS304
    		GROUP_NAME_LIB		= "Counts and TPM";
    		GROUP_DESC_LIB		= "Counts and/or TPM per condition (or assembled sequences)";
    	}
		String [] allLibraryNames = qTab.getAllLibraryNames();
		String [] allLibraryTitles = qTab.getAllLibraryTitles(); 
		String [] pLabels = qTab.getAllPValNames();	
		String [] pTitles = qTab.getAllPValTitles(); 
			
        FieldMapper mapper = new FieldMapper ("ContigData", allLibraryNames,  parentFrame);

     // General
        mapper.addIntField ( SEQ_IDX_FIELD, "Seq #", "contig", "CTGID",
        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        		"Index of the query result.   " );
   
        // The heading "Seq ID" is used in the Main Table for export
        mapper.addStringField( SEQ_ID_FIELD, "Seq ID", "contig", "contigid", 
        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        		"Sequence identifier." );
        
        if(allLibraryNames != null && allLibraryNames.length > 0)
	        	mapper.addIntField( TOTAL_EXP_FIELD, "Total Counts", "contig", "totalexp", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Sum of the counts for all conditions." );
  
        mapper.addIntField( SEQ_LENGTH_FIELD, "Length", "contig", "consensus_bases", 
        			GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        			"Length of the sequence." );
        
        if(!metaData.isAAsTCW()) {
        		mapper.addIntField( CNT_NS_FIELD, "Ns", "contig", "cnt_ns", 
        			GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        			"Number of ns" );
        }
        if(metaData.hasAssembly()) {
	        mapper.addIntField( NUMBER_EST_FIELD, "#Align", "contig", "numclones", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Number of aligned reads in the contig." );
        }
        if (metaData.hasBuried()) {
			mapper.addIntField( NUMBER_BURIED_FIELD, "#Buried", null, null, 
				GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
				"Number of buried sequences in the contig." ); 
			String strNumBuriedQ = "SELECT COUNT(*) FROM contclone WHERE contig.contigid=contclone.contigid and buried=1 ";
			mapper.setFieldSubQuery ( NUMBER_BURIED_FIELD, "numburied", strNumBuriedQ );  
    
			mapper.addIntField( NUMBER_UNBURIED_FIELD, "#Unburied", null, null, 
				GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
    			"Number of sequences in the contig that are not buried." ); 
			String strNumUnburiedQ = "SELECT COUNT(*) FROM contclone WHERE contig.contigid=contclone.contigid and buried!=1 ";  
			mapper.setFieldSubQuery ( NUMBER_UNBURIED_FIELD, "numunburied", strNumUnburiedQ );
        }

        if (metaData.hasMatePairs()) {
        	 	mapper.addBoolField( HAS_NS_ID_FIELD, "Has Ns", "contig", "has_ns", 
             		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
             		"Has n's joining mate-pairs" );
	        mapper.addIntField( EST_MATE_PAIRS_FIELD, "#EST Mate Pairs", "contig", "frpairs", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Number of EST mate-pairs in the contig." );
	        mapper.addIntField( EST_5_PRIME_FIELD, "#EST 5'", "contig", "est_5_prime", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Number of 5' ESTs in the contig." );
	        mapper.addIntField( EST_3_PRIME_FIELD, "#EST 3'", "contig", "est_3_prime", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Number of 3' ESTs in the contig." );
        }
        String longLabel = metaData.getLongLabel(); // CAS311
        String desc = metaData.hasAssembly() ? "Identifier for the longest sequence in the contig."  
        		: "Original sequence name.";
	    mapper.addStringField( LONGEST_EST_FIELD, longLabel, "contig", "longest_clone", 
	        	GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, desc );
        
        if (metaData.hasORFs()) {
	        mapper.addPercentField( GC_RATIO_FIELD, "%GC", "contig", "gc_ratio", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Percent GC content of the sequence." );
        }
        mapper.addStringField( NOTES_FIELD, "TCW Remarks", "contig", "notes", 
        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        		"TCW remarks for a sequence, added when ORFs were computed." );
        
        mapper.addStringField( USER_NOTES_FIELD, "User Remarks", "contig", "user_notes", 
        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
        		"User remarks for a sequence, added with runSingleTCW.." );
        
        if (metaData.hasLoc()) {
    	 		mapper.addStringField( SEQ_GROUP, "LocGroup", "contig", "seq_group", 
	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
	        		"Name of group (e.g. scaffold, chromosome)." );
    	 		if (metaData.hasNgroup()) {
    	 			mapper.addIntField( SEQ_NGROUP, "LocNGroup", "contig", "seq_ngroup", 
    		        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
    		        		"Number of group (e.g. scaffold, chromosome)." );
    	 		}
    	 		mapper.addIntField( SEQ_START, "LocStart", "contig", "seq_start", 
 	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
 	        		"Start position of the predicted gene on the group." );
    	 		mapper.addIntField( SEQ_END, "LocEnd", "contig", "seq_end", 
 	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
 	        		"End position of the predicted gene on the group." );
    	 		mapper.addStringField( SEQ_STRAND, "LocStrand", "contig", "seq_strand", 
     	        		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
     	        		"Strand of the predicted gene." );
        }
        if (metaData.hasPairWise()) {
            mapper.addStringField( PAIRWISE_FIELD, "Pairs Count", "contig", "cnt_pairwise", 
            		GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of Similar Sequence Pairs its contain in." );
        }
        if (metaData.hasHits()) {	// Counts for UniProt and Nucleotide hits   
            mapper.addIntField( CNT_TAXO, "#annoDBs", "contig", 
            		"cnt_annodb", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of different annoDBs with a hit for the sequence." ); 
            
            mapper.addIntField( CNT_SPECIES, "#Species", "contig", 
            		"cnt_species", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of different species aligned to the sequence." ); 
            
            mapper.addIntField( CNT_GENE, "#Desc", "contig", 
            		"cnt_gene", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of different descriptions of hit alignments." ); 
            
            mapper.addIntField( CNT_OVERLAP, "#Regions", "contig", 
            		"cnt_overlap", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of different regions of the sequence with hit alignments." );  
            
            mapper.addIntField( CNT_SWISS, "#SwissProt", "contig", 
            		"cnt_swiss", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of hits from Swissprot." );  
            
            mapper.addIntField( CNT_TREMBL, "#TrEMBL", "contig", 
            		"cnt_trembl", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of hits from TrEMBL." );
            
            mapper.addIntField( CNT_NCBI, "#Protein", "contig", 
            		"cnt_gi", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of hits from other protein database(s)." );
            
            mapper.addIntField( CNT_NT, "#Nucleotide", "contig", 
            		"cnt_nt", GROUP_NAME_CONTIG, GROUP_DESC_CONTIG, 
            		"Number of hits from nucleotide database(s)." ); 
        }
    // Library Columns
        if(allLibraryNames != null) {
        	for(int x=0; x<allLibraryNames.length; x++)
        		mapper.addIntField(LIBRARY_COUNT_ALL + x, "#" + allLibraryNames[x], "contig", 
        				L + allLibraryNames[x], GROUP_NAME_LIB, GROUP_DESC_LIB, 
        				"Library " + allLibraryNames[x]);
        	for(int x=0; x<allLibraryNames.length; x++)
        		mapper.addFloatField(LIBRARY_TPM_ALL + x, allLibraryNames[x], "contig", 
        				LN + allLibraryNames[x], GROUP_NAME_LIB, GROUP_DESC_LIB, 
        				allLibraryTitles[x]);    
	        	
        	// Nfold and Rstat
	        mapper.addFloatField( RSTAT_FIELD, "Rstat", "contig", "rstat", 
	        		GROUP_NAME_RSTAT, GROUP_DESC_RSTAT, 
	            	"(Stekel et al. 2000) Over all conditions for the sequence." );
	        
	        // DE/pvals
	        if(pLabels != null) {
	        	for(int x=0; x<pLabels.length; x++) {
	        		mapper.addFloatField(P_VALUES + x, pLabels[x], "contig", 
	        			PVAL + pLabels[x], GROUP_NAME_PVAL, GROUP_DESC_PVAL, 
	        			pTitles[x]);
	        	}
	        } 
        }  
       
  // Best match. 
        if (metaData.hasHits()) addHits(mapper, metaData);
        
  // SNP
        if (metaData.hasAssembly()) {
    		mapper.addIntField( SNP_COUNT_FIELD, "SNP Count", "contig", "snp_count", 
        		GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
        		"Number of SNPs detected in the contig." );
    		mapper.addIntField( SNP_INDEL_FIELD, "SNP InDel", "contig", "indel_count", 
          		GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
          		"Number of SNP insertions/deletions." );        	
        }
  // Open Reading Frames
        if(metaData.hasORFs()) {
	        mapper.addIntField( ORF_CODING_FRAME, "ORF " + Globalx.frameField, "contig", 
	        		"o_frame", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"Frame position of the best ORF." );
	 
	        mapper.addIntField( ORF_CODING_LENGTH, "ORF Len", "contig", 
	        		"o_len", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"Length of the best ORF in bases." );
	     
	        mapper.addIntField( ORF_START, "ORF Start", "contig", 
	        		"o_coding_start", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"ORF start (may not have start codon, see Has Start)." ); 
	        
	        mapper.addIntField( ORF_END, "ORF End", "contig", 
	        		"o_coding_end",  GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"ORF end (may not have stop codon, see Has Stop)." );
	        
	        mapper.addBoolField( ORF_HAS_START, "Has Start", "contig", 
	        		"o_coding_has_begin", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"ORF has start codon." );
	        
	        mapper.addBoolField( ORF_HAS_END, "Has Stop", "contig", 
	        		"o_coding_has_end", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"ORF has stop codon." );
	        
	        mapper.addFloatField( ORF_MARKOV, "ORF Markov", "contig", 
	        		"o_markov", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"Markov score for the ORF." );
	        
	        mapper.addBoolField( USED_PROTEIN_FRAME, "Is PR " + Globalx.frameField, "contig", 
	        		"p_eq_o_frame", GROUP_NAME_SNPORF, GROUP_DESC_SNPORF, 
	        		"ORF frame is same as protein hit frame." );
        }
      
        mapper.setDefaultFieldIDs ( new int [] { SEQ_ID_FIELD} );
        return mapper;
    }
    /** 
     * Add the Best Hit columns 
     * The descriptions are not used. They are hardcoded in UIfieldBestHit.
     * UIfieldBestHit column names must be the same as here. 
     ***/
    private static void addHits(FieldMapper mapper, MetaData metaData) {
        mapper.addStringField( FIRST_BEST_UNIPROT_FIELD, "BS Hit ID", TABLE_FIRST_BEST, 
        		"uniprot_id", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"AnnoDB ID of hit with the best bit-score to the sequence." ); 
        mapper.setFieldSubQuery(FIRST_BEST_UNIPROT_FIELD, "firstbestid", TABLE_FIRST_BEST + ".uniprot_id");
        
        mapper.addStringField( FIRST_BEST_DESCRIPTION, "BS Description", TABLE_FIRST_BEST_DETAIL, 
        		"description", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Description of the best bit-score." ); 
        mapper.setFieldSubQuery(FIRST_BEST_DESCRIPTION, "firstbestdescrip", TABLE_FIRST_BEST_DETAIL + ".description");
         
        mapper.addStringField( FIRST_BEST_SPECIES, "BS Species", TABLE_FIRST_BEST_DETAIL, 
        		"species", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Species of the best bit-score." ); 
        mapper.setFieldSubQuery(FIRST_BEST_SPECIES, "firstbestspecies", TABLE_FIRST_BEST_DETAIL + ".species");
        
        mapper.addStringField( FIRST_BEST_DBTYPE_FIELD, "BS DB type", TABLE_FIRST_BEST, 
        		"dbtype", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Type (e.g. SP (SwissProt) or TR (TrEMBL)) of the best bit-score." ); 
        mapper.setFieldSubQuery(FIRST_BEST_DBTYPE_FIELD, "firstbestdbtype", TABLE_FIRST_BEST + ".dbtype");
        
        mapper.addStringField( FIRST_BEST_TAXO_FIELD, "BS Taxo", TABLE_FIRST_BEST, 
        		"taxonomy", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Taxonomy (or sequence type) of the database for the best bit-score." ); 
        mapper.setFieldSubQuery(FIRST_BEST_TAXO_FIELD, "firstbesttaxo", TABLE_FIRST_BEST + ".taxonomy");
       
        mapper.addFloatField( FIRST_BEST_EVALUE_FIELD, "BS E-val", TABLE_FIRST_BEST, 
        		"e_value", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"E-value of the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_EVALUE_FIELD, "firstbesteval", TABLE_FIRST_BEST + ".e_value");
        
        mapper.addFloatField( FIRST_BEST_BIT_SCORE_FIELD, "BS Bitscore", TABLE_FIRST_BEST, //CAS331 change from Int
        		"bit_score", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Bit-score of the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_BIT_SCORE_FIELD, "firstbestbitscore", TABLE_FIRST_BEST + ".bit_score");
        
        mapper.addFloatField( FIRST_BEST_PERCENT_IDENT, "BS %Sim", TABLE_FIRST_BEST, //CAS331 change from Int
        		"percent_id", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST,
        		"Percent similarity of the best bit-score");
        mapper.setFieldSubQuery(FIRST_BEST_PERCENT_IDENT, "firstbestpercentid", TABLE_FIRST_BEST + ".percent_id");
 
        mapper.addIntField( FIRST_BEST_ALIGN_LEN_FIELD, "BS Align", TABLE_FIRST_BEST, 
        		"alignment_len", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Alignment length of the best bit-score." ); 
        mapper.setFieldSubQuery(FIRST_BEST_ALIGN_LEN_FIELD, "firstbestalignlen", TABLE_FIRST_BEST + ".alignment_len");
        
        mapper.addIntField( FIRST_BEST_CTG_START_FIELD, "BS SeqStart", TABLE_FIRST_BEST, 
        		"ctg_start", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Start of the match of the sequence for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_CTG_START_FIELD, "firstbestctgstart", TABLE_FIRST_BEST + ".ctg_start");
        
        mapper.addIntField( FIRST_BEST_CTG_END_FIELD, "BS SeqEnd", TABLE_FIRST_BEST, 
        		"ctg_end", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"End of the match of the sequence for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_CTG_END_FIELD, "firstbestctgend", TABLE_FIRST_BEST + ".ctg_end");
        
        mapper.addIntField( FIRST_BEST_CTG_COV_FIELD, "BS %SeqCov", TABLE_FIRST_BEST, 
        		"ctg_cov", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Percent coverage of sequence for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_CTG_COV_FIELD, "firstbestctgcov", TABLE_FIRST_BEST + ".ctg_cov");
        
        mapper.addIntField( FIRST_BEST_PRO_START_FIELD, "BS HitStart", TABLE_FIRST_BEST, 
        		"prot_start", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Start of match of the HSP for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_PRO_START_FIELD, "firstbestprotstart", TABLE_FIRST_BEST + ".prot_start");
        
        mapper.addIntField( FIRST_BEST_PRO_END_FIELD, "BS HitEnd", TABLE_FIRST_BEST, 
        		"prot_end", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"End of match of the HSP for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_PRO_END_FIELD, "firstbestprotend", TABLE_FIRST_BEST + ".prot_end");
        
        mapper.addIntField( FIRST_BEST_PRO_COV_FIELD, "BS %HitCov", TABLE_FIRST_BEST, 
        		"prot_cov", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"Percent coverage of hit for the best bit-score." );
        mapper.setFieldSubQuery(FIRST_BEST_PRO_COV_FIELD, "firstbestprotcov", TABLE_FIRST_BEST + ".prot_cov");
        
        if(metaData.hasGOs()) {
    	 	mapper.addStringField( FIRST_BEST_GO, "BS GO", TABLE_FIRST_BEST_DETAIL, 
	        		"goList", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
	        		"GO IDs for the overall best bit-score." );
    	 	mapper.setFieldSubQuery(FIRST_BEST_GO, "firstbestgo", TABLE_FIRST_BEST_DETAIL + ".goList");
    	 	
	        mapper.addStringField( FIRST_BEST_INTERPRO, "BS Interpro", TABLE_FIRST_BEST_DETAIL, 
	        		"interpro", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
	        		"Interpro ID for the overall best bit-score." );
	        mapper.setFieldSubQuery(FIRST_BEST_INTERPRO, "firstbestinterpro", TABLE_FIRST_BEST_DETAIL + ".interpro");
    	
        	mapper.addStringField( FIRST_BEST_KEGG, "BS KEGG", TABLE_FIRST_BEST_DETAIL, 
        		"kegg", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"KEGG ID for the overall best bit-score." );
        	mapper.setFieldSubQuery(FIRST_BEST_KEGG, "firstbestkegg", TABLE_FIRST_BEST_DETAIL + ".kegg");
        	
        	mapper.addStringField( FIRST_BEST_PFAM, "BS PFam", TABLE_FIRST_BEST_DETAIL, 
        		"pfam", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"PFam ID for the overall best bit-score." );
	         mapper.setFieldSubQuery(FIRST_BEST_PFAM, "firstbestpfam", TABLE_FIRST_BEST_DETAIL + ".pfam");
	         	
        	mapper.addStringField( FIRST_BEST_EC, "BS EC", TABLE_FIRST_BEST_DETAIL, 
        		"ec", GROUP_NAME_FIRST_BEST, GROUP_DESC_FIRST_BEST, 
        		"EC (enzyme) ID for the overall best bit-score." );
        	mapper.setFieldSubQuery(FIRST_BEST_EC, "firstbestec", TABLE_FIRST_BEST_DETAIL + ".ec");
        }
        //////////////////////////////////////////////
        // Overall Best match
        mapper.addStringField( OVER_BEST_UNIPROT_FIELD, "AN Hit ID", TABLE_OVER_BEST, 
        		"uniprot_id", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"AnnoDB Id of hit with good e-value and annotation." ); 
        mapper.setFieldSubQuery(OVER_BEST_UNIPROT_FIELD, "overbestid", TABLE_OVER_BEST + ".uniprot_id");
       
        mapper.addStringField( OVER_BEST_DESCRIPTION, "AN Description", TABLE_OVER_BEST_DETAIL, 
        		"description", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Description of the overall BestAnno." ); 
        mapper.setFieldSubQuery(OVER_BEST_DESCRIPTION, "overdescrip", TABLE_OVER_BEST_DETAIL + ".description");
        
        mapper.addStringField( OVER_BEST_SPECIES, "AN Species", TABLE_OVER_BEST_DETAIL, 
        		"species", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Species of the overall BestAnno." ); 
        mapper.setFieldSubQuery(OVER_BEST_SPECIES, "overspecies", TABLE_OVER_BEST_DETAIL + ".species");
        
        mapper.addStringField( OVER_BEST_DBTYPE_FIELD, "AN DB type", TABLE_OVER_BEST, 
        		"dbtype", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Type (e.g. SP (SwissProt) or TR (TrEMBL)) of the overall BestAnno." ); 
        mapper.setFieldSubQuery(OVER_BEST_DBTYPE_FIELD, "overtype", TABLE_OVER_BEST + ".dbtype");
        
        mapper.addStringField( OVER_BEST_TAXO_FIELD, "AN Taxo", TABLE_OVER_BEST, 
        		"taxonomy", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Taxonomy (or sequence type) of the database for the overall BestAnno." ); 
        mapper.setFieldSubQuery(OVER_BEST_TAXO_FIELD, "overtax", TABLE_OVER_BEST + ".taxonomy");
        
        mapper.addFloatField( OVER_BEST_EVALUE_FIELD, "AN E-val", TABLE_OVER_BEST, 
        		"e_value", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"E-value of the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_EVALUE_FIELD, "overeval", TABLE_OVER_BEST + ".e_value");
        
        mapper.addFloatField( OVER_BEST_BIT_SCORE_FIELD, "AN Bitscore", TABLE_OVER_BEST, // CAS331 change from int
        		"bit_score", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Bit-score of the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_BIT_SCORE_FIELD, "overbitscore", TABLE_OVER_BEST + ".bit_score");
        
        mapper.addFloatField( OVER_BEST_PERCENT_IDENT, "AN %Sim", TABLE_OVER_BEST, // CAS331 change from int
        		"percent_id", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST,
        		"Percent similarity of the overall BestAnno");
        mapper.setFieldSubQuery(OVER_BEST_PERCENT_IDENT, "overperid", TABLE_OVER_BEST + ".percent_id");
        
        mapper.addIntField( OVER_BEST_ALIGN_LEN_FIELD, "AN Align", TABLE_OVER_BEST, 
        		"alignment_len", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Alignment length of the overall best HSP of the overall BestAnno." ); 
        mapper.setFieldSubQuery(OVER_BEST_ALIGN_LEN_FIELD, "overalignlen", TABLE_OVER_BEST + ".alignment_len");
        
        mapper.addIntField( OVER_BEST_CTG_START_FIELD, "AN SeqStart", TABLE_OVER_BEST, 
        		"ctg_start", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Start of the match to the sequence for the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_CTG_START_FIELD, "overctgstart", TABLE_OVER_BEST + ".ctg_start");
        
        mapper.addIntField( OVER_BEST_CTG_END_FIELD, "AN SeqEnd", TABLE_OVER_BEST, 
        		"ctg_end", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"End of the match to the sequence for the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_CTG_END_FIELD, "overctgend", TABLE_OVER_BEST + ".ctg_end");
        
        mapper.addIntField( OVER_BEST_CTG_COV_FIELD, "AN %SeqCov", TABLE_OVER_BEST, 
        		"ctg_cov", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Percent coverage of sequence for the BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_CTG_COV_FIELD, "overctgcov", TABLE_OVER_BEST + ".ctg_cov");
        
        
        mapper.addIntField( OVER_BEST_PRO_START_FIELD, "AN HitStart", TABLE_OVER_BEST, 
        		"prot_start", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Start of match to the HSP for the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_PRO_START_FIELD, "overprot_start", TABLE_OVER_BEST + ".prot_start");
        
        mapper.addIntField( OVER_BEST_PRO_END_FIELD, "AN HitEnd", TABLE_OVER_BEST, 
        		"prot_end", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"End of match of the HSP for the overall BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_PRO_END_FIELD, "overprotend", TABLE_OVER_BEST + ".prot_end");
        
        mapper.addIntField( OVER_BEST_PRO_COV_FIELD, "AN %HitCov", TABLE_OVER_BEST, 
        		"prot_cov", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"Percent coverage of hit for the BestAnno." );
        mapper.setFieldSubQuery(OVER_BEST_PRO_COV_FIELD, "overhitcov", TABLE_OVER_BEST + ".prot_cov");
  
        
        if(metaData.hasGOs()) {
    		mapper.addStringField( OVER_BEST_GO, "AN GO", TABLE_OVER_BEST_DETAIL, 
	        		"goList", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
	        		"GO IDs for the overall BestAnno." );
    		mapper.setFieldSubQuery(OVER_BEST_GO, "overbestgo", TABLE_OVER_BEST_DETAIL + ".goList");
    		
    		mapper.addStringField( OVER_BEST_INTERPRO, "AN Interpro",   TABLE_OVER_BEST_DETAIL, 
	        		"interpro", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
	        		"Interpro IDs for the overall BestAnno." );
    		mapper.setFieldSubQuery(OVER_BEST_INTERPRO, "overinterpro", TABLE_OVER_BEST_DETAIL + ".interpro");
        	
        	mapper.addStringField( OVER_BEST_KEGG, "AN KEGG",    TABLE_OVER_BEST_DETAIL, 
        		"kegg", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"KEGG ID for the overall BestAnno." );
        	mapper.setFieldSubQuery(OVER_BEST_KEGG, "overkegg", TABLE_OVER_BEST_DETAIL + ".kegg");
        	
        	mapper.addStringField( OVER_BEST_PFAM, "AN PFam",   TABLE_OVER_BEST_DETAIL, 
        		"pfam", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"PFam ID for the overall BestAnno." );  
        	mapper.setFieldSubQuery(OVER_BEST_PFAM, "overpfam", TABLE_OVER_BEST_DETAIL + ".pfam");
        	
        	mapper.addStringField( OVER_BEST_EC, "AN EC",       TABLE_OVER_BEST_DETAIL, 
        		"ec", GROUP_NAME_OVER_BEST, GROUP_DESC_OVER_BEST, 
        		"EC (Enzyme) ID for the overall BestAnno." );
        	mapper.setFieldSubQuery(OVER_BEST_EC, "overenzyme", TABLE_OVER_BEST_DETAIL + ".ec");
        }
// Best GO
        if (!metaData.hasGOs()) return;
          
        ///////////////////////////////////////////////////////
        mapper.addStringField( GO_BEST_UNIPROT_FIELD, "WG Hit ID", TABLE_GO_BEST, 
        		"uniprot_id", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"AnnoDB Id of sequence hit with best Bitscore and with assigned GOs." ); 
        mapper.setFieldSubQuery(GO_BEST_UNIPROT_FIELD, "gobestid", TABLE_GO_BEST + ".uniprot_id");
       
        mapper.addStringField( GO_BEST_DESCRIPTION, "WG Description", TABLE_GO_BEST_DETAIL, 
        		"description", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Description of the BestHitWithGO." ); 
        mapper.setFieldSubQuery(GO_BEST_DESCRIPTION, "godescript", TABLE_GO_BEST_DETAIL + ".description");
        
        mapper.addStringField( GO_BEST_SPECIES, "WG Species", TABLE_GO_BEST_DETAIL, 
        		"species", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Species of the BestHitWithGO." ); 
        mapper.setFieldSubQuery(GO_BEST_SPECIES, "gospecies", TABLE_GO_BEST_DETAIL + ".species");
        
        mapper.addStringField( GO_BEST_DBTYPE_FIELD, "WG DB type", TABLE_GO_BEST, 
        		"dbtype", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Type (e.g. SP (SwissProt) or TR (TrEMBL)) of the BestHitWithGO." ); 
        mapper.setFieldSubQuery(GO_BEST_DBTYPE_FIELD, "godbtype", TABLE_GO_BEST + ".dbtype");
        
        mapper.addStringField( GO_BEST_TAXO_FIELD, "WG Taxo", TABLE_GO_BEST, 
        		"taxonomy", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Taxonomy (or sequence type) of the database for the BestHitWithGO." ); 
        mapper.setFieldSubQuery(GO_BEST_TAXO_FIELD, "gotaxonomy", TABLE_GO_BEST + ".taxonomy");
        
        mapper.addFloatField( GO_BEST_EVALUE_FIELD, "WG E-val", TABLE_GO_BEST, 
        		"e_value", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"E-value of the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_EVALUE_FIELD, "goevalue", TABLE_GO_BEST + ".e_value");
        
        mapper.addFloatField( GO_BEST_BIT_SCORE_FIELD, "WG Bitscore", TABLE_GO_BEST, //CAS331 change from hit
        		"bit_score", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Bit-score of the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_BIT_SCORE_FIELD, "gobitscore", TABLE_GO_BEST + ".bit_score");
        
        mapper.addFloatField( GO_BEST_PERCENT_IDENT, "WG %Sim", TABLE_GO_BEST, //CAS331 change from hit
        		"percent_id", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST,
        		"Percent similarity of the BestHitWithGO");
        mapper.setFieldSubQuery(GO_BEST_PERCENT_IDENT, "goperid", TABLE_GO_BEST + ".percent_id");
        
        mapper.addIntField( GO_BEST_ALIGN_LEN_FIELD, "WG Align", TABLE_GO_BEST, 
        		"alignment_len", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Alignment length of the best HSP of the BestHitWithGO." ); 
        mapper.setFieldSubQuery(GO_BEST_ALIGN_LEN_FIELD, "goalignlen", TABLE_GO_BEST + ".alignment_len");
        
        
        mapper.addIntField( GO_BEST_CTG_START_FIELD, "WG SeqStart", TABLE_GO_BEST, 
        		"ctg_start", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Start of the match to the sequence for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_CTG_START_FIELD, "goctgstart", TABLE_GO_BEST + ".ctg_start");
        
        mapper.addIntField( GO_BEST_CTG_END_FIELD, "WG SeqEnd", TABLE_GO_BEST, 
        		"ctg_end", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"End of the match to the sequence for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_CTG_END_FIELD, "goctgend", TABLE_GO_BEST + ".ctg_end");
        
        mapper.addIntField( GO_BEST_CTG_COV_FIELD, "WG %SeqCov", TABLE_GO_BEST, 
        		"ctg_cov", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Percent coverage of the sequence for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_CTG_COV_FIELD, "goctgcov", TABLE_GO_BEST + ".ctg_cov");
        
        
        mapper.addIntField( GO_BEST_PRO_START_FIELD, "WG HitStart", TABLE_GO_BEST, 
        		"prot_start", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Start of match to the HSP for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_PRO_START_FIELD, "goprotstart", TABLE_GO_BEST + ".prot_start");
        
        mapper.addIntField( GO_BEST_PRO_END_FIELD, "WG HitEnd", TABLE_GO_BEST, 
        		"prot_end", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"End of match of the HSP for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_PRO_END_FIELD, "goprotend", TABLE_GO_BEST + ".prot_end");
   
        mapper.addIntField( GO_BEST_PRO_COV_FIELD, "WG %HitCov", TABLE_GO_BEST, 
        		"prot_cov", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Percent coverage of hit for the BestHitWithGO." );
        mapper.setFieldSubQuery(GO_BEST_PRO_COV_FIELD, "goprotcov", TABLE_GO_BEST + ".prot_cov");
   
		mapper.addStringField( GO_BEST_GO, "WG GO", TABLE_GO_BEST_DETAIL, 
        		"goList", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"GO IDs for the BestHitWithGO." );
		mapper.setFieldSubQuery(GO_BEST_GO, "gobestgo", TABLE_GO_BEST_DETAIL + ".goList");
		
		mapper.addStringField( GO_BEST_INTERPRO, "WG Interpro", TABLE_GO_BEST_DETAIL, 
        		"interpro", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
        		"Interpro IDs for the BestHitWithGO." );
		mapper.setFieldSubQuery(GO_BEST_INTERPRO, "gointerpro", TABLE_GO_BEST_DETAIL + ".interpro");
		 
    	mapper.addStringField( GO_BEST_KEGG, "WG KEGG", TABLE_GO_BEST_DETAIL, 
    		"kegg", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
    		"KEGG ID for the BestHitWithGO." );
    	mapper.setFieldSubQuery(GO_BEST_KEGG, "gokegg", TABLE_GO_BEST_DETAIL + ".kegg");
    	  
    	mapper.addStringField( GO_BEST_PFAM, "WG PFam", TABLE_GO_BEST_DETAIL, 
    		"pfam", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
    		"PFam ID for the BestHitWithGO." ); 
    	mapper.setFieldSubQuery(GO_BEST_PFAM, "gopfam", TABLE_GO_BEST_DETAIL + ".pfam");
    	
    	mapper.addStringField( GO_BEST_EC, "WG EC", TABLE_GO_BEST_DETAIL, 
    		"ec", GROUP_NAME_GO_BEST, GROUP_DESC_GO_BEST, 
    		"EC (Enzyme) ID for the BestHitWithGO." );
    	mapper.setFieldSubQuery(GO_BEST_EC, "goenzyme", TABLE_GO_BEST_DETAIL + ".ec");
    }
    /*
     *    There is no reason for creating the mySQL statement here, except for using 
     *    some of the constants.
     */
    /*************************************
     * Builds the mySQL statement with a where clause of contig names
     */
    public String getSeqListSQL (FieldMapper fields, FieldSeqTab fTab, String [] seqIDs)
    {
    	String strQuery = "", strNFold="";
    	summary= "Basic search";
    	
   		strQuery = "SELECT " + fields.getDBFieldList(fTab.getNFoldObj()) + strNFold +
   				   " FROM contig " + getJoinForColumns(fields);

		String contigList = "('" + seqIDs[0] + "'";
		for(int x=1; x<seqIDs.length; x++) contigList += ", '" + seqIDs[x] + "'";
		contigList += ")";
		
		strQuery +=	" WHERE contig.contigid IN " + contigList  + " ORDER BY contig.contigid";
		return strQuery;
    }

    /**
     * XXX Builds the mySQL statement with a where clause of the filters
     */
    public String getSeqFilterSQL (SeqQueryTab queryObj, FieldMapper fMapObj, FieldSeqTab fTabObj)
    {
	    String strQuery = "SELECT " + fMapObj.getDBFieldList(fTabObj.getNFoldObj()) + " FROM contig";
   
   		String queryJoin =  "";
   		String strGOID = queryObj.getGOterm();
   		if(strGOID!=null) {
			if (strGOID.contains("GO:")) strGOID = strGOID.substring(3);
			queryJoin += " JOIN pja_unitrans_go ON contig.CTGID = pja_unitrans_go.CTGID " +
					" AND pja_unitrans_go.gonum = " + strGOID;
		}
   		// joins for queries
   		if (whereClause!=null && !whereClause.equals("1")) {
   			if (whereClause.indexOf("contclone.") >= 0)
   				queryJoin += " JOIN contclone ON contig.contigid = contclone.contigid";
   			
			if (whereClause.indexOf("pja_db_unitrans_hits.") >= 0)
			   queryJoin += " JOIN pja_db_unitrans_hits ON contig.CTGID = pja_db_unitrans_hits.CTGID";
   		} 
   		else whereClause="1";
   		
   		String fieldJoin = getJoinForColumns(fMapObj);
   		
   		strQuery += queryJoin + fieldJoin + " where " + whereClause + " ORDER by contig.contigid";
 
		return strQuery;
    }
    private String getJoinForColumns(FieldMapper fields) {
    		String strQuery="";
    		
		if ( fields.haveDBFieldWithTable(TABLE_FIRST_BEST) || fields.haveDBFieldWithTable(TABLE_FIRST_BEST_DETAIL) )	
			strQuery += " LEFT OUTER JOIN pja_db_unitrans_hits AS " + TABLE_FIRST_BEST + " ON " +
					"contig.PID = " + TABLE_FIRST_BEST + ".PID ";
		if ( fields.haveDBFieldWithTable(TABLE_OVER_BEST) || fields.haveDBFieldWithTable(TABLE_OVER_BEST_DETAIL) )	
			strQuery += " LEFT OUTER JOIN pja_db_unitrans_hits AS " + TABLE_OVER_BEST + " ON " +
					"contig.PIDov = " + TABLE_OVER_BEST + ".PID ";
		if ( fields.haveDBFieldWithTable(TABLE_GO_BEST) || fields.haveDBFieldWithTable(TABLE_GO_BEST_DETAIL) )	
			strQuery += " LEFT OUTER JOIN pja_db_unitrans_hits AS " + TABLE_GO_BEST + " ON " +
					"contig.PIDgo = " + TABLE_GO_BEST + ".PID ";

		if( fields.haveDBFieldWithTable(TABLE_FIRST_BEST_DETAIL) )
			strQuery += " LEFT OUTER JOIN pja_db_unique_hits AS " + TABLE_FIRST_BEST_DETAIL + " ON " +
					TABLE_FIRST_BEST + ".DUHID = " + TABLE_FIRST_BEST_DETAIL + ".DUHID ";
		if( fields.haveDBFieldWithTable(TABLE_OVER_BEST_DETAIL) )
			strQuery += " LEFT OUTER JOIN pja_db_unique_hits AS " + TABLE_OVER_BEST_DETAIL + " ON " +
					TABLE_OVER_BEST + ".DUHID = " + TABLE_OVER_BEST_DETAIL + ".DUHID ";
		if( fields.haveDBFieldWithTable(TABLE_GO_BEST_DETAIL) )
			strQuery += " LEFT OUTER JOIN pja_db_unique_hits AS " + TABLE_GO_BEST_DETAIL + " ON " +
					TABLE_GO_BEST + ".DUHID = " + TABLE_GO_BEST_DETAIL + ".DUHID ";
		return strQuery;
    }
    public void setWhere(String w) {whereClause=w;}
    public void setSummary(String w) {summary=w;}
    public String getSummary() {return summary;};
    
	private String summary=null;
	private String whereClause=null;
}
