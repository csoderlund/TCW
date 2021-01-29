package sng.annotator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import sng.dataholders.BlastHitData;
import util.methods.ErrorReport;
import util.methods.Out;

/********************************************************
 * Static methods to parse the > fasta line, a blast tabular line,
 * and "|" names.
 */
public class LineParser {
	static final int maxHitNameLen = 30; // should correspond to schema for name lengths
	static final int maxSpeciesLen = 100; 
	static final int maxDescriptLen = 250;
	static int badHitNameLen=0;
	static int badSpeciesLen =0;
	static int badDescriptLen=0;
	static String badHitFile = BlastHitData.badHits;
	
	public LineParser() {}
	
	public boolean parseLine(String line) {
		strDBtype =strHitID =strOtherID =strDesc =strOS = "";
		if (line.startsWith(">")) return parseFasta(line);
		else return parseBlastTab(line);
	}
	
	/*********************************************
	 * Create species and geneRep for DoUniProt
	 */
	private Pattern u = Pattern.compile("\\s+");
	public String getSpecies(String spec) {
		String t = spec.trim();
		String [] r = u.split(t);
		if (r.length == 1) return r[0];
		return r[0] + " " + r[1];
	}
	public String getGeneRep(String fulldesc) {
		String desc = fulldesc.trim().toLowerCase();
		int ix;

		if (desc.contains("putative")) return null;
		if (desc.contains("predicted")) return null;
		if (desc.contains("hypothetical")) return null;
		if (desc.contains("uncharacterized")) return null;
		if (desc.contains("unknown")) return null;
		if (desc.contains("whole genome shotgun")) return null;
		if (desc.contains("scaffold")) return null;

		ix = desc.indexOf("{ec");  
		if (ix != -1) {
			String s = desc.substring(0,ix).trim();
			if (s.length() > 5) desc = s;
		}
		else {
			ix = desc.indexOf("(fragment)");
			if (ix != -1) {
				String s = desc.substring(0,ix);
				if (s.length() > 1) desc = s;
			}
		}
		// remove number
		ix = desc.lastIndexOf(" ");
		if (ix != -1) {
			if (ix == desc.length()-2) return desc.substring(0, ix);
			if (ix == desc.length()-3) return desc.substring(0, ix);
		}
		ix = desc.lastIndexOf("-");
		if (ix != -1) {
			if (ix == desc.length()-2) return desc.substring(0, ix);
			if (ix == desc.length()-3) return desc.substring(0, ix);
		}
		ix = desc.lastIndexOf("_");
		if (ix != -1) {
			if (ix == desc.length()-2) return desc.substring(0, ix);
			if (ix == desc.length()-3) return desc.substring(0, ix);
		}
		return desc;
	}

	/***********************************************
	 * BLAST tab file
	 * just parsing subject (second column of blast)
	 * the complete parsing is in BlastHitData
	 */
	public boolean parseBlastTab(String line) {
		String[] tokens = line.split("\t");
		if (tokens == null || tokens.length<2) return parseSubject(line);
		else 	return parseSubject(tokens[1]);
	}
	private boolean parseSubject(String line) {
		strDBtype = strHitID = strOtherID = "";	
		
		if (line.indexOf("|") == -1) {
			strHitID = line;
			if (strHitID.length()>maxHitNameLen) {
				Out.die("Hit Id > " + maxHitNameLen + " " + strHitID);
			}
			return true;
		}
		String [] str = line.split(" ");
		String [] e = str[0].split("\\|"); // Since usearch keeps desc
		if (e.length==2) {
			strDBtype=e[0];
			strOtherID = "";
			strHitID = e[1];
		}
		else if (e.length==3) {
			strDBtype=e[0];
			strOtherID = e[1];
			strHitID = e[2];
		}
		else if (e.length>=4){ // has a '|' at the end
			strDBtype=e[0];
			strOtherID = e[1];
			strHitID = e[3];
		}
		else {
			Out.die("Do not recognize format: " + str);
		}
		if (strHitID.length()>maxHitNameLen) {
			Out.die("Hit Id > " + maxHitNameLen + " " + strHitID);
		}
		return true;
	}
    /*********************************************************** 
     * FASTA parse > line
     */ 
	public boolean parseFasta(String line) {	
		boolean rc=true;
		strDBtype = strHitID = strOtherID = strOS = strDesc = "";	
		line = line.substring(1); // get rid of >
				
		if (line.startsWith("sp|") || line.startsWith("tr|")) rc = matchUniProt(line);
		else if (line.startsWith("gi|")) 					 rc = matchRefSeq(line); // old nr.gz
		else {
			if (line.indexOf("\u0001")!=-1) { 
				String [] tok = line.split("\u0001"); 	// This is for nr.gz, which has long lines 
				if (tok.length>1) line = tok[0];		// So only pattern match on first description
			}
			rc = matchGeneral(line);
		}
		
		if (strOS.length() > maxSpeciesLen ) {
			String s = strOS.substring(0,maxSpeciesLen-1);
			if (!badSpecies.contains(s)) { // can occur many times in blast file, just print once
				if (badSpeciesLen<100) {
					BlastHitData.printWarning(strHitID + ": Species name length (" + strOS.length() + ") >" + maxSpeciesLen 
							+ "-- truncating: " + s + "...");
					badSpecies.add(s);
				}
				else if (badSpeciesLen==100) {
					Out.PrtWarn("Over 100 species length >" + maxSpeciesLen + " -- stop saving to hitsWarning file");
					badSpecies.clear();
				}
				badSpeciesLen++;
			}
			strOS = s;
		}
		if (strDesc.length() > maxDescriptLen ) {
			String d = strDesc.substring(0,maxDescriptLen-1);
			if (!badDesc.contains(d)) {
				if (badDescriptLen<100) { 
					BlastHitData.printWarning(strHitID + ": Decription length (" + strDesc.length() + ") >" + maxDescriptLen + 
							"-- truncating :" + d + "...");
					badDesc.add(d);
				} else if (badDescriptLen==100) {
					Out.PrtWarn("Over 100 descriptions length >" + maxDescriptLen + " -- stop saving to hitsWarning file");
					badDesc.clear();
				}
				badDescriptLen++;
			}
			strDesc = d;
		}

		if (rc==false) return false;
		return true;
	}

	// uniprot line
    //>sp|Q9V2L2|1A1D_PYRAB Putative 1-ami OS=Pyrococcus abyssi GN=PYRAB00630 PE=3 SV=1v
    // 		GN is not always present

	private boolean matchUniProt(String line) {
		try {		
		    Pattern u1 = Pattern.compile("(\\S+)\\|(\\S+)\\|(\\S+)\\s+(.+)OS=(.+)GN="); 	
			Pattern u2 = Pattern.compile("(\\S+)\\|(\\S+)\\|(\\S+)\\s+(.+)OS=(.+)PE="); 
			Pattern u3 = Pattern.compile("(\\S+)\\|(\\S+)\\|(\\S+)\\s+(.+)"); 

			Matcher m = u1.matcher(line);
			if (m.find()) {
				strDBtype = m.group(1);
				strOtherID = m.group(2);
				strHitID = m.group(3);
				strDesc = m.group(4).trim();
				strOS = m.group(5).trim();
				// this check makes it backwards compatiable
				if (strOS.endsWith(".")) strOS = strOS.substring(0, strOS.length()-1);
				return true;
			}	
			m = u2.matcher(line);
			if (m.find()) {
				strDBtype = m.group(1);
				strOtherID = m.group(2);
				strHitID = m.group(3);
				strDesc = m.group(4).trim();
				strOS = m.group(5).trim(); // trim necessary for endsWith(".")
				// this check makes it backwards compatiable
				if (strOS.endsWith(".")) strOS = strOS.substring(0, strOS.length()-1);
				return true;
			}								
			m = u3.matcher(line);
			if (m.find()) {
				strDBtype = m.group(1);
				strOtherID = m.group(2);
				strHitID = m.group(3);
				strDesc = m.group(4);
				strOS = "unk";
				return true;
			}	
			if (strHitID.length() > 25) strHitID = line.substring(25);
			else strHitID = line;
			strDesc = "unk";
			strOS = "unk";
			return false;
		}
		catch (Exception err) {
			String s = "Annotator - parsing UniProt: " + line;
			ErrorReport.reportError(err, s);
			return false;
		}
	}
	// refset line
	//>gi|13591923|ref|NM_031017.1| Rattus norvegicus cAMP responsive element binding protein 1 (Creb1), transcript va
	//>gi|66818355|ref|XP_642837.1| hypothetical protein DDB_G0276911 [Dictyostelium discoideum AX4]^Agi|60470987|gb|EAL68957.1| hypothetical protein DDB_G0276911 [Dictyostelium discoideum AX4]
	//MKTKSSNNIKKIYYISSILVGIYLCWQIIIQIIFLMDNSIAILEAIGMVVFISVYSLAVAINGWILVGRMKKSSKKAQYE
	//DFYKKMILKSKILLSTIIIVIIVVVVQDIVINFILPQNPQPYVYMIISNFIVGIADSFQMIMVIFVMGELSFKNYFKFKR
	// NCBI changed the format for nr.gz, so this is obsolete
	private boolean matchRefSeq(String line) {
		try {
			//Pattern r = Pattern.compile("\\s*(\\S+\\s+\\S+)\\s+(.+)");
			Pattern r = Pattern.compile("(.*)\\[(.*)\\].*");
			String [] e = line.split("\\|");
			
			if (e.length < 5) return false;
			
			strDBtype="gi";
			strOtherID = e[1];
			strHitID = e[3];
			Matcher m = r.matcher(e[4]);
			if (m.matches()) {
				strDesc = m.group(1);
				strOS = m.group(2);
				return true;
			}
			else {
				strDesc = e[4];
				m = r.matcher(line);
				if (m.matches()) strOS = m.group(2);
				else strOS = "UNK";
				return true;
			}
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Annotator - parsing nr: " + line);
			return false;
		}
	}
	
	// NCBI nr
	//>name description [OS]			
	//>type|x|name description [OS]	-- does not work right now
	//>type||name description [OS] -- also does not work right now
	//General
	//>type|name description OS=
	//>name description OS=
	//>name description
	private boolean matchGeneral(String line) {
		try {
			Pattern gi =  Pattern.compile("(\\S+)\\s+(.+)\\[(.+)\\]"); // for NCBI nr
			Pattern g0 = Pattern.compile("(\\S+)\\|(\\S+)\\s+(.+)OS=(.+)");
			Pattern g1 = Pattern.compile("(\\S+)\\s+(.+)OS=(.+)");
			Pattern g2 = Pattern.compile("(\\S+)\\s+(.+)"); 	
			
			strDBtype = strDesc = strOS = "";
			Matcher m = gi.matcher(line);
			if (m.matches()) {
				strDBtype = "";
				strHitID = m.group(1);
				if (strHitID.contains("|")) { // NCBInr has some subjects with '|' -- brain dead design
					String [] e = strHitID.split("\\|");
					strHitID = e[e.length-1];
				}
				strDesc = m.group(2);
				strOS = m.group(3);
				return true;
			}
			
			m = g0.matcher(line);
			if (m.matches()) {
				strDBtype = m.group(1);
				strHitID = m.group(2);
				strDesc = m.group(3);
				strOS = m.group(4);
				return true;
			}
			m = g1.matcher(line);
			if (m.matches()) {
				strHitID = m.group(1);
				strDesc = m.group(2);
				strOS = m.group(3);
				return true;
			}
			m = g2.matcher(line);
			if (m.matches()) {
				strHitID = m.group(1);
				strDesc = m.group(2);
				strOS = "unk";
				return true;
			}
			// only a line
			strHitID = line;
			strDesc = "unk";
			strOS = "unk";
			return true;
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Annotator - parsing General: " + line);
			return false;
		}
	}
	public void PrtInfo(String msg) {
		System.err.println(msg);
		System.err.println("HitID:   " + strHitID);
		System.err.println("OtherID: " + strOtherID);
		System.err.println("Species: " + strOS);
		System.err.println("Descrip: " + strDesc);
	}
	
	public String getDBtype() {return strDBtype;}
	public String getHitID() {return strHitID;}
	public String getOtherID() {return strOtherID;}
	public String getDescription() {return strDesc;}
	public String getSpecies() { return strOS;}
	
	// These are used when reading fasta file. They will be restarted on every fasta file
	// read from DoUniProt, but there should not be overlap. The static count at the top is for all.
	private HashSet <String>badSpecies = new HashSet <String> ();
	private HashSet <String>badDesc = new HashSet <String> ();
	
	// from FASTA description lines 
	private String strDBtype = "";
	private String strHitID = "";
	private String strOtherID = "";
	private String strDesc = "";
	private String strOS = "";
}
