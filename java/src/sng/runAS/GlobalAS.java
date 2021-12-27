package sng.runAS;

import java.io.File;
import util.database.Globalx;
import util.methods.Out;

/**************************************************************
 * CAS339 new file for all statics
 */
public class GlobalAS {

	public static final String upTaxURL = 
			"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/";
	public static final String upFullURL = 
			"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/";
				
	public static final String goURL = "http://current.geneontology.org/ontology/";
	public static final String goFile = "go-basic.obo";
	
	public static final String rootDir =  Globalx.ANNODIR; // projects/DBfasta
	public static final String upDir =    rootDir + "/UniProt_";
	public static final String goDirOBO = rootDir + "/GO_obo";
	public static final String goPreDB =  Globalx.goPreDB;
	
	public static final String cfgPrefix = Globalx.PROJDIR +  "/AnnoDBs_"; // UniProt_date is added; CAS316 remove ./
	public static final String cfgSuffix = Globalx.CFG; // CAS315 added
	
	public static final String spPre = "uniprot_sprot", trPre = "uniprot_trembl";
	public static final String fullTaxo = "full";
	
	public static final String faSuffix = ".fasta", datSuffix = ".dat";
	public static final String faGzSuffix = ".fasta.gz", datGzSuffix = ".dat.gz";
	
	public static final String demoSuffix = "demo";
	
	private final String SP = Globalx.SP, TR=Globalx.TR;
	
	/********************************************************************* 
	 * Class for making various directory and file names 
	 * mkNameDir+mkNameFasta or  mkNameDir+mkNameDat creates full path name
	 *******************************************************************/
	public GlobalAS(String targetDir) { 
		this.targetUpDir=targetDir;
	}
	public String mkNameDir(String type, String tax) { 
		if (!targetUpDir.endsWith("/")) targetUpDir += "/";
		
		return 	targetUpDir + type + "_" + tax  + "/";
	}
	public String mkNameFa(String type, String tax) {
		String ptx = (type.contentEquals(SP)) ? spPre : trPre;
		
		return ptx+ "_" + tax + faSuffix;
	}
	public String mkNameDat(String type, String tax) {
		String ptx = (type.contentEquals(SP)) ? spPre : trPre;
		
		if (tax=="") return ptx + datSuffix;
		
		return ptx + "_" + tax + datSuffix;
	}
	public String mkNameDatGz(String type, String tax) {
		String ptx = (type.contentEquals(SP)) ? spPre : trPre;
		
		if (tax=="") return ptx + datGzSuffix;
		
		return ptx + "_" + tax + datGzSuffix;
	}
	public String mkFullDatExists(String type, String taxDir, String taxFile) {
		String dir =  mkNameDir(type, taxDir);
		String file = mkNameDat(type, taxFile);
		
		if (new File(dir+file).exists()) return dir+file;
		
		file+=".gz";
		if (new File(dir+file).exists()) return dir+file;
		
		Out.PrtWarn("No .dat file for " + dir+file);
		return null;
	}
	public String mkFullFaExists(String type, String tax) {
		String dir = mkNameDir(type, tax);
		String file = mkNameFa(type, tax);
		
		if (new File(dir+file).exists()) return dir+file;
		
		file+=".gz";
		if (new File(dir+file).exists()) return dir+file;
		
		Out.PrtWarn("No .fasta file for " + dir+file);
		return null;
	}
	
	private String targetUpDir=null;
}
