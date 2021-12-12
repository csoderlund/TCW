package util.methods;

/*******************************************************
 * CAS305 had duplicate in sng.DoUniprot and cmp.LoadMethod
 * The method for finding the best anno per sequence is UniProt.java
 * The Class for finding the best shared cluster hit is in MethodLoad
 */
public class BestAnno {
	final static int nWORDS=2;  // use first N words for short desc
	final static int nCHARS=20; // or first 20 chars - not breaking at blank
	
	// is generated name, Os03g0850800 protein 
	static public boolean isName(String word) {
		int len = word.length();
		if (len<=2) return false;
		
		int cntDigit=0;
		for (int i=0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (Character.isDigit(c)) cntDigit++;
			if (c=='/') return false; //  e.g. Emp24/gp25L/p24-like
		}
		if (cntDigit<=1) return false;
		if (cntDigit>=5) return true;
		
		double x = (double)cntDigit/(double)word.length();
		if (x<0.5) return false;	
		
		return true; // one word with many digits 
	}
		
	/*********************************************
	 *  sng&cmp: bunch of heuristic to figure out non-informative names
	 *  CAS338 International Protein Nomenclature Guidelines
	 *  	where no domain or motif is observed:
	 *  		"uncharacterized protein", "hypothetical protein", "protein XYZ1", "XYZ1 protein"
	 */
	static public boolean descIsGood (String desc) {
		String descLow = desc.toLowerCase().trim();
		
		int ix = descLow.lastIndexOf("{");  // CAS338 was removing "{ec" after descNotDesc check
		if (ix != -1) descLow = descLow.substring(0,ix).trim();
		
		if (descNotDesc(descLow)) return false;
		
		String [] words = descLow.split(" ");
		int wLen=words.length;
		if (wLen > nWORDS) return true; 
		
		boolean isW1 = isName(words[0]);
		
		if (wLen==2) {// 2 words - starts or ends with "protein" and is a name (digits and letters)
			boolean isW2 = isName(words[1]);
			if (isW1 && words[1].toLowerCase().contentEquals("protein")) return false;
			if (isW2 && words[0].toLowerCase().contentEquals("protein")) return false;
			return true;
		}
		
		if (descLow.startsWith("orf")) return false; // 1 word - starts with orf
		
		return !isW1; // 1 word; not good if name
	}
	static private boolean descNotDesc(String desc) { // desc is lower case
		String dedash = desc.replace("_", " ");
		
		if (dedash.contains("uncharacterized protein")) return true;
		if (dedash.contains("hypothetical protein")) return true;  
		
		if (dedash.contains("unnamed protein product")) return true; // CAS317 added for NR
		
		// CAS338 (Dec21) verified the following found in Oryza longistaminata annotation
		if (dedash.contains("putative uncharacterized")) return true; 
		if (dedash.contains("uncharacterized conserved")) return true;
		if (dedash.contains("predicted protein")) return true;    
		if (dedash.contains("whole genome shotgun")) return true; 
		if (dedash.contains("scaffold")) return true;
		
		if (dedash.startsWith("cdna clone")) return true;  		// CAS338 may be particular to oryza
		if (dedash.startsWith("genome assembly")) return true;  // CAS338 may be particular to triticum
		
		if (dedash.equals("low quality protein")) return true;  // CAS317 may be particular to dcitri
		if (dedash.equals("expressed protein")) return true; 
	    
		return false;
	}
	
	
	static public String rmECO(String des) {
		int ix = des.indexOf("{ECO");  
		if (ix != -1) {
			String s = des.substring(0,ix).trim();
			if (s.length() > 0) des = s;
		}
		return des;
	}
	/*********************************************
	 *  cmp: Create string initial words for the %Hit of clusters
	 */
	static public String descBrief(String fullDesc) {
		String desc = fullDesc.toLowerCase().trim();
		if (desc.contains("{"))            	desc =  desc.substring(0,desc.indexOf("{"));
		if (desc.startsWith("probable"))   	desc =  desc.substring(9).trim();
		if (desc.startsWith("putative"))   	desc =  desc.substring(9).trim();
		if (desc.startsWith("predicted:")) 	desc =  desc.substring(desc.indexOf(":")+1).trim();
		
		for (int cc=0, wc=0; cc<desc.length(); cc++) {
			char c1 = desc.charAt(cc);
			if (c1==' ') { // CAS305 
				wc++;			
				if (cc>nCHARS || wc>=nWORDS) {
					desc = desc.substring(0, cc);
					break;
				}
			}
		}
		// Remove trailing numbers
		int j=0;
		for (int i=desc.length()-1; i>desc.length(); i--) {
			char c1 = desc.charAt(i);
			if (Character.isDigit(c1)) j=i;
			else break;
		}
		if (j>0) desc = desc.substring(0,j);
		return desc.trim();
	}
	/**********************************************
	 * cmp.BestHit 
	 */
	static public String getBestDesc(String desc) {
		String des = desc.toLowerCase();
		int ix = des.indexOf("{ec");  
		if (ix != -1) {
			String s = des.substring(0,ix).trim();
			if (s.length() > 0) des = s;
		}
		return des;
	}
}
