package cmp.compile;
/*************************************************************
 * I compared the results from running orthoMCL with the online results.
 * http://orthomcl.org/orthomcl/proteomeUpload.do
 * The results were almost the same as setting Inflation=5, but a little tighter
 * Inflation=6 gave same results as Inflation=5. That is, I could not get the
 * exact same results, but they were close. 
 */
import java.io.*;
import java.sql.ResultSet;

import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.compile.panels.RunCmd;
import cmp.database.Globals;
import util.database.DBConn;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.TCWprops;
import util.methods.Out;

public class MethodOrthoMCL {
	private final static String iDELIM = Globals.Methods.inDELIM;
	 String groupFile = "orthoMCL";  			// name of orthoMCL final output, usually orthoMCL
	
	 public boolean run(int idx, DBConn db, CompilePanel panel) {
		 Out.PrtDateMsg("\nStart execution of orthoMCL (downloaded from www.orthomcl.org)");	
		 if (!setParams(idx, db, panel)) return false;
		 
		 long startTime = Out.getTime();
		 long allTime = startTime;
			
		 boolean rc = doSteps(idx);
		 if (!rc) return false;
		 
		 Out.PrtSpMsgTime(1, "Finish computing orthoMCL ", startTime);
		 Out.PrtSpMsg(1, "");
		 
		// load from file that orthoMCL output
		 if (new MethodLoad(cmpDBC).run(idx, groupFile, cmpPanel)==-1) {
			 Out.PrtSpMsg(2, "Sometimes orthoMCL fails, but then successed on a rerun -- try 'Add new clusters' again");
			 return false;
		 }
		
		 Out.PrtMsgTimeMem("Finish execution of orthoMCL", allTime);
		 return true;
	}
	
	private boolean doSteps(int idx) {
	try{		 
		Out.PrtSpMsg(1, "Start processing...");
		File OMCDir = new File(TCWprops.getExtDir() + Globals.Ext.orthoDir);
		File binDir = new File(OMCDir,"bin");
		String tempDB = "TMP_" + cmpPanel.getProjectName();
		ResultSet rs;
		int rc = -1; 
		
		if (!OMCDir.exists()){
			Out.PrtError("Cannot find OrthoMCL directory " + OMCDir.getAbsolutePath());
			return false;		
		}
		if (!binDir.exists()){
			Out.PrtError("Cannot find OrthoMCL bin directory " + binDir.getAbsolutePath());
			return false;		
		}
		
		File projDir = new File(cmpPanel.getCurProjAbsDir());
		
		// Directory for doing the work
		File tmpDir = new File(projDir,"OMCLTEMP");
		if (tmpDir.exists()) {
			FileHelpers.clearDir(tmpDir);
		}
		else {
			tmpDir.mkdir();
		}
		File cfgFile = new File(tmpDir,"orthomcl.config");

		
		Out.PrtSpMsg(2, "Create OrthoMCL temporary database");
		rs = cmpDBC.executeQuery("show databases like '" + tempDB + "'");
		if (rs.first()) {
			cmpDBC.executeUpdate("drop database " + tempDB);
		}
		DBConn odb = cmpDBC.createDBAndNewConnection(tempDB);
		
		// Write the cfg file used by OrthoMCL
		if (cfgFile.exists()) cfgFile.delete();
		BufferedWriter bw = new BufferedWriter(new FileWriter(cfgFile));
		bw.write("dbVendor=mysql\n");
		bw.write("dbConnectString=" + odb.getDbiStr() + "\n");
		bw.write("dbLogin=" + odb.mUser + "\n");
		bw.write("dbPassword=" + odb.mPass + "\n");
		bw.write("similarSequencesTable=SimilarSequences\n");
		bw.write("orthologTable=Ortholog\n");
		bw.write("inParalogTable=InParalog\n");
		bw.write("coOrthologTable=CoOrtholog\n");
		bw.write("interTaxonMatchView=InterTaxonMatch\n");
		bw.write("percentMatchCutoff=50\n");
		bw.write("evalueExponentCutoff=-5\n");
		bw.write("oracleIndexTblSpc=NONE\n");
		bw.close();

		// Schema install
		File schemInst = new File(binDir,"orthomclInstallSchema");
		String cmd = schemInst.getAbsolutePath() + " " + cfgFile.getAbsolutePath();
		
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		if (rc!=0) { // CAS303 added check on rc for all commands
			System.out.println(cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}

		// orthoMCL uses the original combined file
		File blastDir = new File(projDir, Globals.Search.BLASTDIR);
		File combFile = new File(blastDir, Globals.Search.ALL_AA_FASTA);
		if (!combFile.exists() || combFile.length() < 10)
			if (!runMTCWMain.generateFastaFromDB(cmpPanel)) return false;
		
		String blastFileStr = cmpPanel.getBlastPanel().getBlastFileForMethods(0);
		if (blastFileStr == null) {
			Out.PrtErr("No hit file! Generate hit file, then rerun." );
			return false;
			
		}
		File blastFile = new File(blastFileStr); 
		if (!blastFile.exists()) {
			Out.PrtErr("Cannot find hit file " + blastFile.getAbsolutePath());
			return false;
		}
		
		File tempFasta = new File(tmpDir,"fasta_temp");
		if (tempFasta.exists()) FileHelpers.clearDir(tempFasta);
		else tempFasta.mkdir();
		Runtime.getRuntime().exec("ln -s " + combFile.getAbsolutePath() + " " + tempFasta.getAbsolutePath());

		long t = Out.getTime();
		File ssFile = new File(tmpDir,"simSeq.txt");
		cmd = binDir.getAbsolutePath() + "/orthomclBlastParser " + blastFile.getAbsolutePath() + " " + tempFasta.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclBlastParser");
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, ssFile,0);
		if (rc!=0) {
			Out.prt("Cmd: " + cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}
		Out.PrtSpMsgTime(3, "Finish", t);
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclLoadBlast " + cfgFile.getAbsolutePath() + " " + ssFile.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclLoadBlast");
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		if (rc!=0) {
			Out.prt("Cmd: " + cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}
		
		Out.PrtSpMsgTime(3, "Finish", t);	
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclPairs " + cfgFile.getAbsolutePath() + " log-pairs cleanup=no";
		
		Out.PrtSpMsg(2, "Running orthomclPairs");
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		if (rc!=0) {
			Out.prt("Cmd: " + cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}
		Out.PrtSpMsgTime(3, "Finish", t);
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclDumpPairsFiles " + cfgFile.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclDumpPairsFiles");
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		if (rc!=0) {
			Out.prt("Cmd: " + cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}
		Out.PrtSpMsgTime(3, "Finish", t);
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/mcl mclInput --abc -I " + inflation + " -o mclOutput";
		
		Out.PrtSpMsg(2, "Running mcl");
		rc = RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, false, null,0);
		if (rc!=0) {
			Out.prt("Cmd: " + cmd);
			Out.PrtErr("Cannot continue OrthoMCL due to non-zero return code: " + rc);
			return false;
		}
		Out.PrtSpMsgTime(3, "Finish", t);

		File finalOut = new File(groupFile);
		if (finalOut.exists()) finalOut.delete();
		
		File cshFile = new File(tmpDir,"final.csh");
		BufferedWriter cshW = new BufferedWriter(new FileWriter(cshFile));
		cmd = binDir.getAbsolutePath() + "/orthomclMclToGroups D 1 < mclOutput >" + finalOut.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclMclToGroups");
		cshW.write(cmd + "\n");
		cshW.flush();cshW.close();
		
		cmd = "csh final.csh";
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, false, null,0);
		
		Out.PrtSpMsg(2, "Groups written to " + groupFile);
		odb.close();
	
		cmpDBC.executeUpdate("drop database " + tempDB);
		FileHelpers.deleteDir(tmpDir);
		return true;
	}catch (Exception e) {ErrorReport.reportError(e, "Error in OrthoMCL"); return false; }
	}
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		cmpDBC = db;
		cmpPanel = panel;
		
		blastFile = cmpPanel.getBlastPanel().getBlastFileForMethods(0);
		if (blastFile == null) return false;
		
		MethodPanel theMethod = cmpPanel.getMethodPanel();
		
		prefix = theMethod.getMethodPrefixAt(idx);		// Groups should be prefixed with this
		
		String [] settings = theMethod.getSettingsAt(idx).split(iDELIM);
		if (settings.length<2) 	{
			Out.PrtError("Incorrect parameters: '" + theMethod.getSettingsAt(idx) + "' - using defaults");
			inflation = Globals.Methods.OrthoMCL.INFLATION;
		}
		else 	inflation = settings[1];					// mcl parameter
		
		String x = inflation.replace(".", "_");
		groupFile = cmpPanel.getCurProjMethodDir() +  groupFile + "." + prefix + "-" + x;
		
		Out.PrtSpMsg(1, "Prefix:    " + prefix);
		Out.PrtSpMsg(1, "Inflation: " + inflation );
		Out.PrtSpMsg(1, "Hit File:  " + blastFile);	
		Out.PrtSpMsg(1, "");
		return true;
	}
	private DBConn cmpDBC;			// database connection
	private CompilePanel cmpPanel;	// get all parameters from this
	
	private String prefix;
	private String inflation;
	private String blastFile;
}
