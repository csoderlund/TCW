package util.methods;

/*******************************************************
 * CAS305 had duplicate in sng.DoUniprot and cmp.LoadMethod
 * The method for finding the best anno per sequence is UniProt.java
 * The Class for finding the best shared cluster hit is in MethodLoad
 */
public class BestAnno {
	final static int nWORDS=2;  // use first N words for short desc
	final static int nCHARS=20; // or first 20 chars - not breaking at blank
	
	/*********************************************
	 *  sng&cmp: bunch of heuristic to figure out non-informative names
	 *  CAS317 made a few changes
	 */
	static public boolean descIsGood (String desc) {
		String des = desc.toLowerCase().trim();
		
		String dedash = des.replace("_", " ");
		if (dedash.contains("uncharacterized protein") || 
	    		dedash.contains("putative uncharacterized") || 
	    		dedash.contains("hypothetical protein") || 
	    		dedash.equals("expressed protein") || 
	    		dedash.contains("predicted protein") ||
	    		dedash.contains("whole genome shotgun") ||
	    		dedash.startsWith("low quality protein") ||  // CAS317 may be particular to dcitri
	    		des.equals("unknown") || 
	    		des.equals("unk") || 
	    		des.equals("orf") ||
				dedash.contains("unnamed protein product")) // CAS317 added for NR
	    {
			return false;
	    }
		
		int ix = des.indexOf("{ec");  // Remove {ec from UniProt
		if (ix != -1) {
			String s = des.substring(0,ix).trim();
			if (s.length() > 3) des = s;
		}
		
		// Bunch of heuristics for un-informative names 
		String [] words = des.split(" ");
		int wLen=words.length;
		if (wLen > nWORDS) return true; 
		
		int cntDigit=0, cntDash=0;
		for (int i=0; i < words[0].length(); i++) {
			char c = words[0].charAt(i);
			if (Character.isDigit(c)) cntDigit++;
			else if (c=='-') cntDash++;
		}
		if (cntDash>2) return true;
		
		int  w0Len = words[0].length();
		if (wLen==1 && w0Len<=3) return false;
		if (wLen==1 && w0Len<=5 && cntDigit>0) return false;
		
		double mostlyN = (cntDigit > 0) ? ((double) cntDigit/ (double) w0Len) : 0;
		
		boolean isName = (mostlyN>0.4);
		
		if (isName) {
			if (wLen==1) return false;
			if (des.endsWith("protein")) return false;
			if (des.endsWith("(fragment)")) return false;
			if (des.endsWith("partial")) return false; 
			if (des.endsWith("scaffold")) return false;
		}
		return true;
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
