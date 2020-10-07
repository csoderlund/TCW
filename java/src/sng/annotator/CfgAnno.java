package sng.annotator;

import java.io.File;


import sng.amanager.FileTextField;
import sng.database.Globals;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;
import util.methods.TCWprops;

public class CfgAnno {

	private static final String sTCWcfg = Globals.STCWCFG;
	private static final String defaultTaxo = "Unk";
	
	public CfgAnno() {}
	
	public boolean load(boolean bdoAnno, boolean bdoORF, boolean bdoGO, 
			DoBlast blastObj, CoreAnno annoObj, DoUniProt uniObj, DoORF orfObj) {
		
		this.blastObj = blastObj;
		this.annoObj = annoObj;
		this.uniObj = uniObj;
		this.orfObj = orfObj;
		
		return cfgTCWload(bdoAnno, bdoORF, bdoGO);
	}
	/**********************************************************************/
	/***
	 * load sTCW.cfg for annotator, calls TCWprops
	 * 
	 * if change parameter, change:
	 *  1. TCWprops sets defaults
	 *  2. ManagerData.readSTCWkeyVal
	 *  3. MangerData.saveLIBcfg_sTCWcfg and create get/set methods for parameter.
	 *     Manager saves any changes before calling runSTCWMain to then load sTCW.cfg again
	 *  4. This file
	 */
	private boolean cfgTCWload(boolean bdoAnno, boolean bdoORF, boolean bdoGO) 
	{			
		fileObj = new FileTextField(null, runSTCWMain.getProjName());
		Out.Print("\nReading " + sTCWcfg);
		String cfgFile = runSTCWMain.getCurProjPath() + "/" + sTCWcfg;
		File cfgFileObj = new File(cfgFile);
		if (! cfgFileObj.exists()) {
			Out.PrtError("Could not find configuration file " + cfgFile);
			return false;
		}
		// read sTCW.cfg
		mProps = new TCWprops(TCWprops.PropType.Annotate);
		try {
			mProps.loadAnnotate(cfgFileObj);
			
			cfgNonFileParams();
			if (bdoAnno) cfgSelfblastParams();
			if (bdoAnno||bdoORF) cfgORFParams();
			if (bdoAnno) {
				if (!cfgDBParams()) return false;
				if (!cfgGO()) return false;
			}
			else if (bdoGO)
				if (!cfgGO()) return false;
			
		} catch (Exception err) {
			ErrorReport.reportError(err, "reading configuration file " + cfgFile);
			return false;
		}		
		return true;
	}
	private boolean cfgGO() {
		try {
			String godb = mProps.getAnnoProperty("Anno_GO_DB").trim(); 
			String goSlimSubset = mProps.getAnnoProperty("Anno_SLIM_SUBSET").trim(); 
			String goSlimOBOFile = mProps.getAnnoProperty("Anno_SLIM_OBOFILE").trim(); 
			runSTCWMain.setGOparameters(godb, goSlimSubset, goSlimOBOFile);
			return true;
		} catch (Exception err) {
			ErrorReport.reportError(err, "reading GO parameters ");
			return false;
		}
	}
	private void cfgNonFileParams() {
		try {
			String stcwDB = mProps.getAnnoNotDefault("STCW_db");
			if (stcwDB==null) Out.die("sTCW.ctg does not contain a valid STCW_db (database name)");
			
			String stcwID = mProps.getAnnoProperty("SingleID");
			int nCPUs = Integer.parseInt(mProps.getAnnoProperty("CPUs"));	
			Out.PrtSpMsg(1,"CPUs/Threads = " + nCPUs);
				
			runSTCWMain.setParameters(stcwDB, stcwID, nCPUs);
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error getting parameters");}
	}
	
	private void cfgSelfblastParams() {		
		try {			
			int pairs = 			Integer.parseInt(mProps.getAnnoProperty("Anno_pairs_limit"));
			String selfBlastFile =  mProps.getAnnoProperty("Anno_unitrans_selfblast");
			String tselfBlastFile = mProps.getAnnoProperty("Anno_unitrans_tselfblast");
			String selfBlastArgs = 	mProps.getAnnoProperty("Anno_selfblast_args");
			String tselfBlastArgs = mProps.getAnnoProperty("Anno_tselfblast_args");
			
			// a '-' or -1 indicates that the keyword was not present
			annoObj.setMaxDPpairs(pairs);
			if (selfBlastFile.equals("-") && tselfBlastFile.equals("-")) return;
			
			if (mProps.getAnnoIsNotDefault("Anno_pairs_limit")) 
				Out.PrtSpMsg(1,"Pairs limit = " + pairs); 
		
			// SELFBLAST 
			// - indicates no keyword, no keyword, no self blast
			if (!selfBlastFile.equals("-")) { 
				if (!selfBlastFile.equals("")) {
					Out.PrtSpMsg(1,"Use selfblast file = " + selfBlastFile);
					String file = getValidPathForFile(selfBlastFile);
					if (file==null) Out.PrtWarn("Cannot find file '" + selfBlastFile + "' - ignoring");
					else blastObj.setSelfBlastFile(file);
				}
				else if (!selfBlastArgs.equals("")) { 
					Out.PrtSpMsg(1,"Run selfblast with parameters = " + selfBlastArgs);
					blastObj.setSelfBlastArgs(selfBlastArgs);
				}
				else {
					Out.PrtSpMsg(1,"Run selfblast with default parameters");
					blastObj.setSelfBlastArgs(BlastArgs.getBlastnOptions());
				}
			}
			
			if (!tselfBlastFile.equals("-")) { 
				if (!tselfBlastFile.equals("")) {
					Out.PrtSpMsg(1,"Use translated selfblast file = " + tselfBlastFile);
					String file = getValidPathForFile(tselfBlastFile);
					if (file==null) Out.PrtWarn("Cannot find file '" + tselfBlastFile + "' - ignoring");
					else blastObj.setTSelfBlastFile(file);
				}
				else if (!tselfBlastArgs.equals("")) { 
					Out.PrtSpMsg(1,"Run translated selfblast with parameters = " + tselfBlastArgs);
					blastObj.setTSelfBlastArgs(tselfBlastArgs);
				}
				else {
					Out.PrtSpMsg(1,"Run translated selfblast with default parameters");
					blastObj.setTSelfBlastArgs(BlastArgs.getBlastpOptions());
				}
			}
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error getting Selfblast parameters");}		
	}
	private void cfgORFParams() {
		try {
			boolean bAlt=false;
			
			int type = Integer.parseInt(mProps.getAnnoProperty("Anno_ORF_alt_start"));
			if (type==1) {
				bAlt=true;
				Out.PrtSpMsg(1,"ORF use alternative starts");
			}
			
			double hitEval =    argDouble("Anno_ORF_hit_evalue", "ORF Hit E-value");
			int hitSim 	=		argInt("Anno_ORF_hit_sim", "ORF %Sim of Hit");
			
			double diffLen = 	argDouble("Anno_ORF_len_diff", "ORF Log Len Ratio");
		
			int trSet	 =		argInt("Anno_ORF_train_min_set", "ORF minimal training set");
			String cdsFile = mProps.getAnnoProperty("Anno_ORF_train_CDS_file");
			if (!cdsFile.equals("-1")&&!cdsFile.equals("")) {
				Out.PrtSpMsg(1,"ORF Training CDS file = " + cdsFile);
			}
			orfObj.setParams(bAlt, hitEval, hitSim, diffLen, trSet, cdsFile);
		}
		catch (Exception e) {ErrorReport.reportError(e, "getting annoDB parameters");}
	}
	public boolean cfgDBParams() {
		try {	
			int flank = argInt("Anno_flanking_region", "Flanking region");			
			uniObj.setFlankingRegion(flank);
			
			int spPref = argInt("Anno_SwissProt_pref", "SwissProt preference");
			uniObj.setSwissProtPref(spPref);
			
			int rmPref = argInt("Anno_Remove_ECO", "Remove {ECO...} string"); // CAS305
			uniObj.setRemoveECO(rmPref);
			
			int bit = Integer.parseInt(mProps.getAnnoProperty("Anno_min_bitscore"));
			if (bit != -1) {
				uniObj.setMinBitScore(bit);
				Out.PrtSpMsg(1,"min_bitscore = " + bit);
			}
			
			// if no number at end
			boolean rc = cfgAddDB(0, "Anno_uniprot_blast", "Anno_uniprot_fasta", 
					"Anno_uniprot_args", "Anno_uniprot_taxo", "Anno_DBdate", "Anno_search_pgm", null);
			if (!rc) return false;
			
			for (int i=1; i < Globals.numDB; i++) {
				String n = Integer.toString(i);
				String b = "Anno_unitrans_DBblast_" + n;
				String f = "Anno_DBfasta_" + n;
				String a = "Anno_DBargs_" + n;
				String t = "Anno_DBtaxo_" + n;
				String d = "Anno_DBdate_" + n;
				String p = "Anno_DBsearch_pgm_" + n;

				rc = cfgAddDB(i, b, f, a, t, d, "Anno_DBdate", p);
				if (!rc) return false;
			}
			return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "getting annoDB parameters"); return false;}
	}
	private double argDouble(String key, String msg) {
		String str = mProps.getAnnoProperty(key);
		double d=0.0; 
		try {
			d= Double.parseDouble(str);
		}
		catch (Exception e) {Out.die(key + " must be a float: " + str);}
	
		if (mProps.getAnnoNotDefault(key)!=null) 
			Out.PrtSpMsg(1,msg + " = " + str);
		return d;
	}
	private int argInt(String key, String msg) {
		String str = mProps.getAnnoProperty(key);
		int d=0; 
		try {
			d= Integer.parseInt(str);
		}
		catch (Exception e) {Out.die(key + " must be an integer: " + str);}
		if (mProps.getAnnoNotDefault(key)!=null) 
			Out.PrtSpMsg(1,msg + " = " + str);
		return d;
	}
	
	private boolean cfgAddDB (int index, String mblast, String mfasta, String margs, 
			String mtaxo, String mdate,  String mdefdate, String mpgm) {
		try {
			String tabFile =   mProps.getAnnoProperty(mblast);	
			String dbFile = 		mProps.getAnnoProperty(mfasta); 
			if (!keyExist(tabFile) && !keyExist(dbFile)) return true;
				
			String args =    mProps.getAnnoProperty(margs);
			String taxo =	 mProps.getAnnoProperty(mtaxo);
			String DBdate =  mProps.getAnnoProperty(mdate);
			
			String searchPgm = mProps.getAnnoProperty(mpgm);
			if (mblast!=null) mblast = mblast.replace("Anno_unitrans_", "");
			if (mfasta!=null) mfasta = mfasta.replace("Anno_", "");
			if (margs!=null) margs = margs.replace("Anno_", "");
			if (mtaxo!=null) mtaxo = mtaxo.replace("Anno_", "");
			if (mdate!=null) mdate = mdate.replace("Anno_", "");
			if (mpgm!=null) mpgm = mpgm.replace("Anno_", "");
			
			Out.PrtSpMsg(1,"DB#" + index);
			
			// hits tabular file provided
			if (keyExist(tabFile)) { // supplied file
				Out.PrtSpMsg(1,"  " + mblast + " = " + tabFile);
				tabFile = fileObj.pathToOpen(tabFile, FileTextField.PROJ);
				if (tabFile==null) return false;
				
				if (!keyExist(dbFile)) 
					Out.PrtWarn("No database fasta file supplied; DB entries will not have descriptions or sequences"); 
			}
			
			// DBfasta file
			if (keyExist(dbFile)) { 
				Out.PrtSpMsg(2,mfasta + " = " + dbFile);
				dbFile = fileObj.pathToOpen(dbFile, FileTextField.ANNO);
				if (dbFile==null) return false;
				
				if (!args.equals("-")) 
				    Out.PrtSpMsg(2, margs + " = " + args);
			}
			
			// taxo
			if (!taxo.equals("-")) Out.PrtSpMsg(2, mtaxo + " = " + taxo);
			else taxo = defaultTaxo;
			
			// DBfasta date
			if (keyExist(DBdate)) Out.PrtSpMsg(2, mdate + "=" + DBdate);
			else {
				if (mdefdate != null) DBdate =  mProps.getAnnoProperty(mdefdate);
				if (!keyExist(DBdate)) DBdate = null;
			}
			if (DBdate != null) {
				if (!goodDate(DBdate)) Out.die("The date " + DBdate + " is not formated yyyy-mm-dd (e.g. 2011-12-31)");
			}
			
			blastObj.makeDB(index, tabFile,dbFile, args, taxo, DBdate, searchPgm);
			return true;
		}
		catch (Exception e) {
			String s = "Getting parameter: #" + index + " " + mblast + " " + mfasta + " " + margs;
			ErrorReport.reportError(e, s);
			return false;
		}
	}
	
	private boolean keyExist(String x) {
		if (x != null && !x.equals("-") && !x.equals("")) return true;
		else return false;
	}
	private boolean goodDate (String dt) { // yyyy-mm-dd
		int f = dt.indexOf("-");
		int l = dt.lastIndexOf("-");
		if (f== -1 || l == -1 || f == l) return false;
		String year = dt.substring(0,f);
		String month = dt.substring(f+1,l);
		String day = dt.substring(l+1, dt.length());
		if (year.length() != 4 || month.length() != 2 || day.length() != 2) return false;

		int y = Integer.parseInt(year);
		if (y<1000 || y > 3000) return false;
		int m = Integer.parseInt(month);
		if (m < 1 || m > 12) return false;
		int d = Integer.parseInt(day);
		if (d < 1 || d > 31) return false;
	
		return true;
	}
	private String getValidPathForFile(String file) {
		String absFile = file;
		if (FileHelpers.fileExists(absFile)) return absFile;
		
		if (!absFile.startsWith("/")) absFile = "/" + absFile;
		absFile = runSTCWMain.getCurProjPath() + absFile;

		if (FileHelpers.fileExists(absFile)) return absFile;
		
		return null;
	}	
	private TCWprops mProps;
	private FileTextField fileObj = null;
	private DoBlast blastObj = null;
	private DoUniProt uniObj = null;
	private CoreAnno annoObj = null;
	private DoORF orfObj = null;
}
