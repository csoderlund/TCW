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

import cmp.compile.MethodLoad;
import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.MethodPanel;
import cmp.compile.panels.RunCmd;
import cmp.database.Globals;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.TCWprops;
import util.methods.FileHelpers;
import util.methods.Out;

public class MethodOrthoMCL {
	 String groupFile = "orthoMCL";  			// name of orthoMCL final output, usually orthoMCL
	
	 public boolean run(int idx, DBConn db, CompilePanel panel) {
		 Out.PrtDateMsg("\nStart execution of orthoMCL (downloaded from www.orthomcl.org)");	
		 if (!setParams(idx, db, panel)) return false;
		 
		 long startTime = Out.getTime();
		 long allTime = startTime;
			
		 doSteps(idx);
		 
		 Out.PrtSpMsgTime(1, "Finish computing orthoMCL ", startTime);
		 Out.PrtSpMsg(1, "");
		 
				// load from file that orthoMCL output
		 if (! new MethodLoad(cmpDBC).run(idx, groupFile, cmpPanel)) {
			 Out.PrtSpMsg(2, "Sometimes orthoMCL fails, but then successed on a rerun -- try 'Add new clusters' again");
			 return false;
		 }
		
		 Out.PrtDateMsgTime("Finish execution of orthoMCL", allTime);
		 return true;
	}
	
	private void doSteps(int idx) {
	try{		 
		Out.PrtSpMsg(1, "Start processing...");
		File OMCDir = new File(TCWprops.getExtDir() + "/OrthoMCL");
		File binDir = new File(OMCDir,"bin");
		String tempDB = "TMP_" + cmpPanel.getProjectName();
		ResultSet rs;
		
		if (!OMCDir.exists())
		{
			Out.PrtError("Can't find OrthoMCL directory " + OMCDir.getAbsolutePath());
			return;		
		}
		if (!binDir.exists())
		{
			Out.PrtError("Can't find OrthoMCL bin directory " + binDir.getAbsolutePath());
			return;		
		}
		
		File projDir = new File(cmpPanel.getCurProjAbsDir());
		
		// Directory for doing the work
		File tmpDir = new File(projDir,"OMCLTEMP");
		if (tmpDir.exists())
		{
			FileHelpers.clearDir(tmpDir);
		}
		else
		{
			tmpDir.mkdir();
		}
		File cfgFile = new File(tmpDir,"orthomcl.config");

		
		Out.PrtSpMsg(2, "Create OrthoMCL temporary database");
		rs = cmpDBC.executeQuery("show databases like '" + tempDB + "'");
		if (rs.first())
		{
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
		System.out.println(cmd);
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);

		// orthoMCL uses the original combined file
		File blastDir = new File(projDir, Globals.CompilePanel.BLASTDIR);
		File combFile = new File(blastDir, Globals.CompilePanel.ALL_AA_FASTA);
		if (!combFile.exists() || combFile.length() < 10)
			if (!runMTCWMain.generateFastaFromDB(cmpPanel)) return;
		
		String blastFileStr = cmpPanel.getBlastPanel().getBlastFileForMethods(0);
		if (blastFileStr == null)
		{
			Out.PrtError("No hit file! Generate hit file, then rerun." );
			return;
			
		}
		File blastFile = new File(blastFileStr); 
		if (!blastFile.exists())
		{
			Out.PrtError("Can't find hit file " + blastFile.getAbsolutePath());
			return;
		}
		File tempFasta = new File(tmpDir,"fasta_temp");
		if (tempFasta.exists()) FileHelpers.clearDir(tempFasta);
		else tempFasta.mkdir();
		Runtime.getRuntime().exec("ln -s " + combFile.getAbsolutePath() + " " + tempFasta.getAbsolutePath());

		long t = Out.getTime();
		File ssFile = new File(tmpDir,"simSeq.txt");
		cmd = binDir.getAbsolutePath() + "/orthomclBlastParser " + blastFile.getAbsolutePath() + " " + tempFasta.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclBlastParser");
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, ssFile,0);
		Out.PrtSpMsgTime(3, "Finish", t);

		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclLoadBlast " + cfgFile.getAbsolutePath() + " " + ssFile.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclLoadBlast");
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		Out.PrtSpMsgTime(3, "Finish", t);	
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclPairs " + cfgFile.getAbsolutePath() + " log-pairs cleanup=no";
		
		Out.PrtSpMsg(2, "Running orthomclPairs");
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		Out.PrtSpMsgTime(3, "Finish", t);
		
		t = Out.getTime();
		cmd = binDir.getAbsolutePath() + "/orthomclDumpPairsFiles " + cfgFile.getAbsolutePath();
		
		Out.PrtSpMsg(2, "Running orthomclDumpPairsFiles");
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, true, null,0);
		Out.PrtSpMsgTime(3, "Finish", t);
		
		t = Out.getTime();
		String inflVal = inflation;
		inflVal = cmpPanel.getMethodPanel().getSettingsAt(idx).split(":")[1];
		cmd = binDir.getAbsolutePath() + "/mcl mclInput --abc -I " + inflVal + " -o mclOutput";
		
		Out.PrtSpMsg(2, "Running mcl");
		RunCmd.runCommand(cmd.split("\\s+"), tmpDir, false, false, null,0);
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
	}catch (Exception e) {ErrorReport.reportError(e, "Error in OrthoMCL"); }
	}
	private boolean setParams(int idx, DBConn db, CompilePanel panel) {
		MethodPanel theMethod = panel.getMethodPanel();
		String comment = theMethod.getCommentAt(idx);
		String [] settings = theMethod.getSettingsAt(idx).split(":");

		prefix = theMethod.getMethodPrefixAt(idx);		// Groups should be prefixed with this
		inflation = settings[1];					// mcl parameter
		cmpDBC = db;
		cmpPanel = panel;
		blastFile = cmpPanel.getBlastPanel().getBlastFileForMethods(0);
		if (blastFile == null) return false;
		String x = inflation.replace(".", "");
		if (x.length()==1) x += "0";
		groupFile = cmpPanel.getCurProjMethodDir() 
				+ "/" + groupFile + "." + prefix + "-" + x;
		
		Out.PrtSpMsg(1, "Prefix:    " + prefix);
		Out.PrtSpMsg(1, "Remark:    " + comment);
		Out.PrtSpMsg(1, "Inflation: " + inflation );
		Out.PrtSpMsg(1, "Hit File:  " + blastFile);	
		Out.PrtSpMsg(1, "");
		return true;
	}
	private DBConn cmpDBC;			// database connection
	private CompilePanel cmpPanel;	// get all parameters from this
	
	static private String prefix;
	static private String inflation;
	static private String blastFile;
}
