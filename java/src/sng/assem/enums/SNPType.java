package sng.assem.enums;

// Don't change these symbols b/c they get written into the snp_clone table
public enum SNPType 
{
	Mis, 	// Mismatch (vs. the consensus): a regular snp 
	Indel, 	// Indel : the variant is a gap
	Mix;	// Mixed case: some EST have mismatch, others have gap
			// This seemed quite uncommon so these types are actually labeled as regular snps now
}
