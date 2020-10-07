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
	 */
	static public boolean descIsGood (String desc) {
		String des = desc.toLowerCase();
		des = des.replace("_", " ");
		if (des.contains("uncharacterized protein") || 
	    		des.contains("putative uncharacterized") || 
	    		des.contains("hypothetical protein") || 
	    		des.contains("expressed protein") || 
	    		des.contains("predicted protein") ||
	    		des.contains("whole genome shotgun") ||
	    		des.contains("scaffold") || 
	    		des.equals("unknown") || 
	    		des.equals("unk") || 
	    		des.equals("orf"))
	    	{
	    	        return false;
	    	}
		
		int ix = des.indexOf("{ec");  
		if (ix != -1) {
			String s = des.substring(0,ix).trim();
			if (s.length() > 3) des = s;
		}
		String [] words = des.split(" ");
		if (words.length > nWORDS) return true; 

		// many are just a 'name' or 'name protein' or 'name (fragment)' or 
		// If name has lots of numbers, seems to be meaningless
		// Names like MET_1 will pass, names like GK1599 will not
		// Rules: if over 3 digits in first word - name
		//		  if more digits than letters - name
		boolean isName = false;
		int cntDigit=0;
		for (int i=0; i < words[0].length(); i++) {
			char c = words[0].charAt(i);
			if (Character.isDigit(c)) cntDigit++;
		}
		if (cntDigit>3) isName=true;
		else {
			int wl = words[0].length();
			double x = (cntDigit > 0) ? ((double) wl/ (double) cntDigit) : 0;
			if (wl <= 5 && x > 1) isName = true;
		}
		if (words.length == 1 && isName) return false;
		if (isName &&  des.endsWith("protein")) return false;
		if (isName &&  des.endsWith("(fragment)")) return false;
		if (isName &&  des.endsWith("partial")) return false; 
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
