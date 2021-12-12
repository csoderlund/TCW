package sng.annotator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import sng.dataholders.BlastHitData;
import util.methods.BestAnno;
import util.methods.ErrorReport;
import util.methods.Out;

/********************************************************
 * Static methods to parse the > fasta line, a blast tabular line and "|" names.
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
		if (line.startsWith(">")) 	return parseFasta(line);
		else 						return parseTab(line);
	}
	
	/*********************************************
	 * Create species and unique description for DoUniProt
	 */
	private Pattern u = Pattern.compile("\\s+");
	public String getSpecies(String spec) {
		String t = spec.trim();
		String [] r = u.split(t);
		if (r.length == 1) return r[0];
		return r[0] + " " + r[1];
	}
	// return description to be compared with other "similar" descriptions
	// CAS338 quite removing uncharacterized descriptions, and a few other changed
	public String getUniqueDesc(String fulldesc) {
		String desc = fulldesc.trim().toLowerCase();
	
		try {
			int ix= desc.indexOf("{");
			if (ix != -1) 	
				desc = desc.substring(0,ix).trim();
			
			if (desc.endsWith("-like")) 
				desc = desc.replace("-like", "").trim(); // CAS338
			
			String [] words = desc.split(" "); 
			if (words.length>2) {
				String lastWord = words[words.length-1];
				if (BestAnno.isName(lastWord)) 
					desc = desc.substring(0, desc.lastIndexOf(" ")); // CAS338
			}
			
			String [] prefix = {"putative", "probable", "uncharacterized"}; // after removing isName
			for (String pre : prefix) {
				if (desc.startsWith(pre)) {
					desc = desc.replace(pre, "").trim();
					break;
				}
			}
			
			int len = desc.length();
			if (len<4) return desc;
			
			boolean trim=false;
			String [] suffix = {" ", "-", "_"}; // remove suffix followed by number
			for (String x : suffix) {
				ix = desc.lastIndexOf(x);
				if (ix==-1) continue;
				
				String end = desc.substring(ix+1, desc.length());
				if (end.length()>=3) continue;
				
				try {
					Integer.parseInt(end);
					desc = desc.substring(0, ix);
					trim=true;
					break;
				}
				catch (Exception e) {};
			}
			if (!trim) { 						// remove trailing digit
				String end = desc.substring(len-1, len);
				try {
					Integer.parseInt(end);
					desc = desc.substring(0, len-1);
				}
				catch (Exception e) {};
			}
		} catch (Exception e) {}; 				// so continues if any problem
	
		return desc;
	}

	/***********************************************
	 * BLAST tab file
	 * just parsing subject (second column of blast)
	 * the complete parsing is in BlastHitData
	 */
	public boolean parseTab(String line) {
		String[] tokens = line.split("\t");
		if (tokens == null || tokens.length<2) 	return parseSubject(line);
		else 									return parseSubject(tokens[1]);
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
		try {
			boolean rc=true;
			strDBtype = strHitID = strOtherID = strOS = strDesc = "";	
			line = line.substring(1); // get rid of >
					
			if (line.startsWith("sp|") || line.startsWith("tr|")) rc = matchUniProt(line);
			else if (line.startsWith("gi|")) 					  rc = matchRefSeq(line); // old nr.gz
			else {
				if (line.indexOf("\u0001")!=-1) { 
					String [] tok = line.split("\u0001"); 	// This is for nr.gz, which has long lines 
					if (tok.length>1) line = tok[0];		// So only pattern match on first description
				}
				rc = matchGeneral(line);
			}
			// CAS317 was checking description length and #species here
				
			if (rc==false) return false;
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, line); return false;}
	}
	// uniprot line
    //>sp|Q9V2L2|1A1D_PYRAB Putative 1-ami OS=Pyrococcus abyssi GN=PYRAB00630 PE=3 SV=1v
	// CAS338 rewrote removing dependancy on Java RE, but didn't make much difference
	private boolean matchUniProt(String line) {
		try {	
			strDesc = strOS = "unk";
			String name=null, rest=null;
			
		// identifier - up too first blank
			int idx = line.indexOf(" ");
			if (idx==-1) name=line;
			else {
				name = line.substring(0,idx);
				rest = line.substring(idx+1);
			}
			String [] tok = name.split("\\|");
			if (tok.length!=3) {
				Out.PrtErr("Bad identifier: " + name);
				return false;
			}
			strDBtype =  tok[0];
			strOtherID = tok[1];
			strHitID =   tok[2];
			
			if (rest==null || !rest.contains("OS=")) {
				strDesc = rest;
				return true;
			}
		
		// Description - before OS=
			tok = rest.split("OS=");
			if (tok.length!=2) {
				Out.PrtErr("Strange line: " + line);
				return false;
			}
			strDesc = tok[0].trim();
			rest =    tok[1].trim();
		
		// Species - everything up to XX= where XX is two letters (OX, GN, PE)
			idx = rest.indexOf("=");
			if (idx == -1 || rest.length()<3) strOS=rest;
			else strOS = rest.substring(0,idx-2).trim();
			
			if (strOS.endsWith(".")) strOS = strOS.substring(0, strOS.length()-1);// backwards compatible
			return true;
			
		}
		catch (Exception err) {
			ErrorReport.reportError(err, "Annotator - parsing UniProt: " + line);
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
	//General
	//>type|name description OS=
	//>name description OS=
	//>name description
	private boolean matchGeneral(String line) {
		try {
			Pattern gi = Pattern.compile("(\\S+)\\s+(.+)\\[(.+)\\]"); // for NCBI nr
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
	
	public String getDBtype() {return strDBtype;}
	public String getHitID() {return strHitID;}
	public String getOtherID() {return strOtherID;}
	public String getDescription() {
		if (strDesc.length() > maxDescriptLen ) { // CAS317 only check ones that are to be saved
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
		return strDesc;
	}
	public String getSpecies() { 
		if (strOS.length() > maxSpeciesLen ) { // CAS317 moved from parseLine
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
		return strOS;
	}
	
	// These are used when reading fasta file. They will be restarted on every fasta file
	// read from DoUniProt, but there should not be overlap. The static count at the top is for all.
	private HashSet <String>badSpecies = new HashSet <String> ();
	private HashSet <String>badDesc = new HashSet <String> ();
	
	// from FASTA description lines 
	private String strDBtype = "", strHitID = "", strOtherID = "";
	private String strDesc = "", strOS = "";
}
