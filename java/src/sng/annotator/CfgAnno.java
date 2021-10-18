package sng.annotator;

import java.io.File;

import sng.database.Globals;
import util.file.FileC;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.TCWprops;

/***********************************************
 * runSingleTCW creates sTCW.cfg
 * execAnno uses this file to read the sTCW.cfg 
 * It sends the appropriate parameters to the class executing the function.
 */
public class CfgAnno {

	private static final String sTCWcfg = Globals.STCWCFG;
	private static final String defaultTaxo = "Unk";
	
	public CfgAnno() {}
	
	public boolean load(boolean bdoAnno, boolean bdoORF, boolean bdoGO, boolean bdoPrune,
			DoBlast blastObj, CoreAnno annoObj, DoUniProt uniObj, DoORF orfObj) {
		
		this.blastObj = blastObj; // sets annoDBs
		this.annoObj = annoObj;   // sets npairs
		this.uniObj = uniObj;	  // sets bUseSP, flank, min_score
		this.orfObj = orfObj;	  // sets ORf params
		
		return cfgTCWload(bdoAnno, bdoORF, bdoGO, bdoPrune);
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
	private boolean cfgTCWload(boolean bdoAnno, boolean bdoORF, boolean bdoGO, boolean bdoPrune) 
	{			
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
			if (bdoAnno || bdoPrune) {
				if (!cfgDBParams(bdoAnno)) return false;
				if (!cfgGO()) return false;
			}
			else if (bdoGO) {
				if (!cfgGO()) return false;
			}
		} catch (Exception err) {
			ErrorReport.reportError(err, "reading configuration file " + cfgFile);
			return false;
		}		
		return true;
	}
	private boolean cfgGO() {
		try {
			String godb = 			mProps.getAnnoProperty("Anno_GO_DB").trim(); 
			String noGO = 			mProps.getAnnoProperty("Anno_No_GO").trim(); // CAS331 add
			String goSlimSubset = 	mProps.getAnnoProperty("Anno_SLIM_SUBSET").trim(); 
			String goSlimOBOFile = 	mProps.getAnnoProperty("Anno_SLIM_OBOFile").trim(); 
			runSTCWMain.setGOparameters(godb, goSlimSubset, goSlimOBOFile, noGO);
			
			if (Globals.hasVal(godb)) 			{
				Out.PrtSpMsg(1,"GO_DB = " + godb); // CAS316 added prints
				
				if (Globals.hasVal(noGO) && noGO!=Globals.pNO_GO) //CAS331
					Out.PrtSpMsg(1,"Do not add GOs on 'Annotate'");
			}
			if (Globals.hasVal(goSlimSubset)) 	Out.PrtSpMsg(1,"GO SLIM = " + goSlimSubset);
			if (Globals.hasVal(goSlimOBOFile)) 	Out.PrtSpMsg(1,"GO Slim OBO file = " + goSlimOBOFile);
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
		try {		// changed in v314 - not backward compatible	
			// Default args are all set here.
			int pairs = 			Integer.parseInt(mProps.getAnnoProperty("Anno_pairs_limit"));
			String blastnRun =  	mProps.getAnnoProperty("Anno_pairs_blastn");
			String blastnArgs = 	mProps.getAnnoProperty("Anno_pairs_blastn_args");
			String tblastxRun = 	mProps.getAnnoProperty("Anno_pairs_tblastx");
			String tblastxArgs = 	mProps.getAnnoProperty("Anno_pairs_tblastx_args");
			String blastpRun = 		mProps.getAnnoProperty("Anno_pairs_blastp");
			String blastpArgs = 	mProps.getAnnoProperty("Anno_pairs_blastp_args");
			String blastpPgm = 		mProps.getAnnoProperty("Anno_pairs_blastp_pgm");
			
			boolean p=false, n=false, x=false;
			
			if (blastnRun.equals("1")) {
				n=true;
				Out.PrtSpMsg(1,"Run blastn");
				
				if (!Globals.hasVal(blastnArgs)) blastnArgs = BlastArgs.getBlastnArgs();
				else Out.PrtSpMsg(1,"Blastn parameters = " + blastnArgs);
				
				blastObj.setSelfBlastnArgs(n, blastnArgs);
			}
			
			if (tblastxRun.equals("1")) {
				x=true;
				Out.PrtSpMsg(1,"Run tblastx");
				
				if (!Globals.hasVal(tblastxArgs)) tblastxArgs = BlastArgs.getTblastxArgs();
				else Out.PrtSpMsg(1,"Tblastx parameters = " + tblastxArgs);	
				
				blastObj.setSelfTblastxArgs(x,tblastxArgs);
			}
		
			if (blastpRun.equals("1")) {
				p=true;
				Out.PrtSpMsg(1,"Run blastp ");
				if (Globals.hasVal(blastpPgm)) {
					Out.PrtSpMsg(1,"Blastp program = " + blastpPgm);
					blastObj.setSelfBlastpPgm(blastpPgm);
				}
				if (!Globals.hasVal(blastpArgs)) {
					blastpArgs = (blastpPgm.equalsIgnoreCase("blast")) ?
							BlastArgs.getBlastArgsORF() :  
							BlastArgs.getDiamondArgsORF();
				}
				else Out.PrtSpMsg(1,"Blastp parameters = " + blastpArgs);
				blastObj.setSelfBlastpArgs(p, blastpArgs);
			}
			
			if (p || n || x) {
				annoObj.setMaxDPpairs(pairs);
				Out.PrtSpMsg(1,"Pairs limit = " + pairs);
				if (pairs==0) Out.PrtWarn("Search will be run, but no pairs added");
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
			
			// prints in argDouble and argInt if not key
			double hitEval =    argDouble("Anno_ORF_hit_evalue", "ORF Hit E-value");
			int hitSim 	=		argInt("Anno_ORF_hit_sim", "ORF %Sim of Hit");
			
			double diffLen = 	argDouble("Anno_ORF_len_diff", "ORF Log Len Ratio");
			
			double diffMk = 	argDouble("Anno_ORF_mk_diff", "ORF Log Markov Ratio"); // CAS334
		
			int trSet	 =		argInt("Anno_ORF_train_min_set", "ORF minimal training set");
			
			String cdsFile = mProps.getAnnoProperty("Anno_ORF_train_CDS_file"); 
			if (Globals.hasVal(cdsFile)) {
				Out.PrtSpMsg(1,"ORF Training CDS file = " + cdsFile);
			}
			orfObj.setParams(bAlt, hitEval, hitSim, diffLen, diffMk, trSet, cdsFile);
		}
		catch (Exception e) {ErrorReport.reportError(e, "getting annoDB parameters");}
	}
	public boolean cfgDBParams(boolean bAnno) {
		try {	
			int flank =  argInt("Anno_flanking_region", "Flanking region");	
			int spPref = argInt("Anno_SwissProt_pref", "SwissProt preference");
			int rmPref = argInt("Anno_Remove_ECO", "Remove {ECO...} string"); // CAS305
			int bit =    argInt("Anno_min_bitscore", "Minimum bitscore");
			int prune =  argInt("Anno_Prune_type", "Prune type");
	
			String godbName = mProps.getAnnoProperty("Anno_GO_DB").trim(); // So DoUniProt can pass to DoUniPrune
			uniObj.setCfgAnnoParams(rmPref==1, spPref==1, flank, bit, prune, godbName); // CAS331 change from 4 to 1 method call
			if (!bAnno) return true; // is bPrune
			
			for (int i=1; i < Globals.numDB; i++) {
				String n = Integer.toString(i);
				String b = "Anno_DBtab_" + n;
				String f = "Anno_DBfasta_" + n;
				String a = "Anno_DBargs_" + n;
				String t = "Anno_DBtaxo_" + n;
				String d = "Anno_DBdate_" + n;
				String p = "Anno_DBsearch_pgm_" + n;

				boolean rc = cfgAddDB(i, b, f, a, t, d, "Anno_DBdate", p);
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
	
	private boolean cfgAddDB (int index, String kTabFile, String kdbFile, String kargs, 
			String ktaxo, String kdate,  String kdefdate, String kpgm) {
		try {
			String projDirName=runSTCWMain.getProjName();
			
			String vtabFile =   mProps.getAnnoProperty(kTabFile);	
			String vdbFile =    mProps.getAnnoProperty(kdbFile); 
			if (!Globals.hasVal(vtabFile) && !Globals.hasVal(vdbFile)) return true;
				
			String vargs =      mProps.getAnnoProperty(kargs);
			String vtaxo =	   	mProps.getAnnoProperty(ktaxo);
			String vdbDate =    mProps.getAnnoProperty(kdate);
			String vpgm = 		mProps.getAnnoProperty(kpgm);
			
			if (kTabFile!=null) kTabFile = kTabFile.replace("Anno_", "");
			if (kdbFile!=null)  kdbFile = kdbFile.replace("Anno_", "");
			if (kargs!=null) 	kargs = kargs.replace("Anno_", "");
			if (ktaxo!=null) 	ktaxo = ktaxo.replace("Anno_", "");
			if (kdate!=null) 	kdate = kdate.replace("Anno_", "");
			if (kpgm!=null) 	kpgm = kpgm.replace("Anno_", "");
			
			Out.PrtSpMsg(1,"DB#" + index);
			
			// hits tabular file provided
			if (Globals.hasVal(vtabFile)) { // supplied file
				Out.PrtSpMsg(1,"  " + kTabFile + " = " + vtabFile);
				vtabFile = FileC.addFixedPath(projDirName, vtabFile, FileC.dPROJ);
				if (vtabFile==null) return false;
				
				if (!Globals.hasVal(vdbFile)) 
					Out.PrtWarn("No database fasta file supplied; DB entries will not have descriptions or sequences"); 
			}
			
			// DBfasta file
			if (Globals.hasVal(vdbFile)) { 
				Out.PrtSpMsg(2,kdbFile + " = " + vdbFile);
				vdbFile = FileC.addFixedPath(projDirName, vdbFile, FileC.dANNO);
				
				if (vdbFile==null) return false;	// ignore any other keywords
			}
			if (Globals.hasVal(vpgm))   Out.PrtSpMsg(2, kpgm  + " = " + vpgm);
			
			// Default args are set in DoBlast, because need to know type of DB
			if (Globals.hasVal(vargs))  Out.PrtSpMsg(2, kargs + " = " + vargs);
			
			// taxo
			if (Globals.hasVal(vtaxo)) 	Out.PrtSpMsg(2, ktaxo + " = " + vtaxo);
			else vtaxo = defaultTaxo;
			
			// DBfasta date
			if (Globals.hasVal(vdbDate)) Out.PrtSpMsg(2, kdate + " = " + vdbDate);
			else {
				if (kdefdate != null) vdbDate =  mProps.getAnnoProperty(kdefdate);
				if (!Globals.hasVal(vdbDate)) vdbDate = null;
			}
			if (vdbDate != null) {
				if (!goodDate(vdbDate)) Out.die("The date " + vdbDate + " is not formated yyyy-mm-dd (e.g. 2011-12-31)");
			}
			
			blastObj.makeDB(index, vtabFile,vdbFile, vargs, vtaxo, vdbDate, vpgm);
			return true;
		}
		catch (Exception e) {
			String s = "Getting parameter: #" + index + " " + kTabFile + " " + kdbFile + " " + kargs;
			ErrorReport.reportError(e, s);
			return false;
		}
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
	
	private TCWprops mProps;
	private DoBlast blastObj = null;
	private DoUniProt uniObj = null;
	private CoreAnno annoObj = null;
	private DoORF orfObj = null;
}
