package sng.runDE;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.HashSet;
import java.util.Map;
import java.lang.StringBuilder;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

import sng.database.Globals;
import sng.database.Schema;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

/********************************************************
 * seq DE
 * 	input counts of two groups (e.g. Root and Stem); set P-value of DE for seq
 * 	add and set contig.P_colName (e.g. P_RoSt)
 * 	enter libraryDE.P_colName, title (e.g. Root: Stem), method
 * 
 * go over-represented
 * 	input is boolean vector of contigs.P_colName<cutoff; set p-value of DE for GO
 * 	add and set go_info.P_colName (same name as in contig table, e.g. P_RoSt)
 * 	enter assem_msg.goDE all P_colName with cutoffs
 */
public class QRProcess {
	private final int NUM_PRT_FILTER=25;
	/********* R variables *********************
	 * These variable are set in R for the DE R-scripts methods
	 */
	private final String countDataR = "countData";
	private final String rowNamesR = "rowNames";
	private final String grpNamesR = "grpNames";
	private final String repNamesR = "repNames";
	private final String dispR = "disp"; 
	private final String nGroup1R = "nGroup1";
	private final String nGroup2R = "nGroup2";
	private final String resultR = "results";
	
	/********************************************
	 * Variables set in R for the GO enrichment R-script; also uses resultR
	 */
	private final String seqDEsR 	= "seqDEs";
	private final String seqNamesR 	= "seqNames";
	private final String seqLensR 	= "seqLens";
	private final String seqGOsR 	= "seqGOs";
	private final String gonumsR 	= "goNums";
	private final String nSeqsR		= "nSeqs";
	
	private final String RPKM = Globals.LIBRPKM;
	private final String pColPrefix = Globals.PVALUE; 

	public QRProcess(String name, DBConn m) {
		mDB = m;
		dbName = name;
	}
	public boolean rStart(boolean bDE) {
		try {
			String sql = "select schemver from schemver where schemver='" + Schema.currentVerString() + "'";
			String ver = mDB.executeString(sql);
			if (ver==null || ver.contentEquals("")) {
				System.err.println("Database is not current (" + Schema.currentVerString() + ") - viewSingleTCW project to update.");
				boolean c = Out.yesNo("Continue?"); // CAS321 was just returning false
				if (!c) return false;				
			}
			if (re == null) {
				initJRI();
				packages = new HashSet<String>();
				getPackages(packages);
			}
			
			if (!bDE) {
				nSeq = mDB.executeCount("select count(*) from contig where PIDgo > 0");
	        	if (nSeq==0) {
	        		Out.PrtWarn("The sequences have not been annotated yet."); 
	        		return false;
	        	}
	        	
				String tab = mDB.executeString("show tables like 'pja_unitrans_go'");
				if (tab==null || tab.contentEquals("")) {
					Out.PrtWarn("GOs have not been added to this database."); 
	        		return false;
				}
	    		if (!checkPackage("goseq")) {
	    			Out.PrtError("GOseq package does not exist");
	    			return false;
	    		}
	    		doCmd(true, "suppressPackageStartupMessages(library(goseq))");
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot start R"); return false;}
			
	}
	public void rFinish() {
		re.startMainLoop();
		Out.Print("The console is in R, you may run R commands -- q() or Cntl-C " +
				"when done, or perform another Execute.");
		System.err.print(">");
	}
	/***************************************************
	 * Loading p-values from file. All values have been checked in QRFrame.
	 */
	public boolean deReadPvalsFromFile(String pColName, TreeSet<String> g1,  
			TreeSet <String> g2, TreeMap <String, Double> scores, String pvalFile) 
	{
		try {
			dePrint(scores);
			String col = Globals.PVALUE + pColName;
			deSaveCols(scores, col, g1, g2);
			deSaveMethod(col, g1, g2, "", pvalFile, 0);
			Out.Print("\nFinished adding p-values from file " + pvalFile + " for " + dbName);
			return true;
		}
		catch(Exception e){ErrorReport.reportError(e, "Error in execute"); return false;}
	}
	/******************************************
	 * XXX DE methods - the loop through multiple grp1/grp2 occurs in QRframe
	 */
	public boolean deRun(boolean b, String rScriptFile, 
		int filCnt, int filCPM, int filCPMn, double disp, 
		String pColName, boolean addCol, TreeSet<String> g1,  TreeSet <String> g2) 
	{
		try {
			TreeSet<String> grp1 = g1;
			TreeSet<String> grp2 = g2;	

			long startTime = Out.getTime();
			
			if (re == null) {
				initJRI();
				packages = new HashSet<String>();
				getPackages(packages);
			}
			
			deLoadCountsToR(b, filCnt, filCPM, filCPMn, disp, grp1,grp2);
			
			TreeMap<String,Double> scores = new TreeMap<String,Double>();
			if (!rScriptFile.equals("")) {
				if (!deRunScript(scores, rScriptFile)) return false;
			}			
			else {
				Out.PrtError("No R-script");
				return false;
			}
			
			dePrint(scores);
			if (scores.size() > 0 && pColName.length() > 0 && addCol) {
				deSaveCols(scores, pColName, grp1, grp2);
				deSaveMethod(pColName, grp1, grp2, rScriptFile, "", disp);
				Out.r("                                             ");
			}  
			Out.PrtMsgTimeMem("\nFinished DE execution for " + prtName(pColName), startTime);
		
			return true;
		}
		catch(Exception e){ErrorReport.reportError(e, "Error in execute"); return false;}
	}
	
	
	/***
	 *  Put the counts into R objects for use by the DE methods
	 *  grp1 are the libraries to be compared with grp2
	 *  filCnt = filter on count; filCPM and filCPMn = filter on CPM
	 */
	private void deLoadCountsToR(boolean b, int filCnt, int filCPM, int filCPMn, double disp,
			TreeSet<String> grp1, TreeSet<String> grp2)
	{
		try {
			nGroup1=nGroup2=0; // in case run a second time, don't want accumlative
			ResultSet rs;
			String strLib="";
			Vector<String> allGrp = new Vector<String>();
			allGrp.addAll(grp1);
			allGrp.addAll(grp2);
			int nRepSamps = 0;
		  	Vector<String> allReps = new Vector<String>(); 
		  	TreeMap <Integer, Sample> samMap = new TreeMap <Integer, Sample> ();
		
	// Get Samples: e.g. root has root1, root2...
			mDB.executeUpdate("update library set reps='' where reps is null"); // in case...
			
			Out.PrtSpMsg(0, "Replicates:");
			for (String libName : allGrp) {
				rs = mDB.executeQuery("select reps,lid from library where libid='" + libName + "'");
				rs.first();
				String reps = rs.getString(1).trim(); // name of reps, e.g. root1, root2, etc
				int lid = rs.getInt(2);
				
				Sample sObj = new Sample(lid, libName);
				samMap.put(lid, sObj);
				if (strLib.equals("")) strLib = ""+ lid;
				else strLib += "," + lid;
				
				if (reps.equals("")) {
					sObj.addN(0, nRepSamps);
					
					nRepSamps++;
					if (grp1.contains(libName)) nGroup1++;
					else nGroup2++;
					
				 	allReps.add(libName + "1"); // used by EDAnorm, and can't be same as grpNames
				}
				else {
					String[] repnames = reps.split(",");
					int nReps = repnames.length;
					Out.PrtSpCntMsg(0, nReps, libName);
					for (int r = 1; r <= repnames.length; r++) {
						sObj.addN(r, nRepSamps);						
						
						nRepSamps++;	
						if (grp1.contains(libName)) nGroup1++; 	
						else nGroup2++;
						
					 	allReps.add(repnames[r-1].trim());
					}
				}
			}
		/* Create groups */
			// array to group conditions: if multiple conditions in group, need to use 'Grp'
			// else can use the condition name (e.g. root for reps root1, root2...)
			String group1="Grp1", group2="Grp2";
			if (grp1.size()==1 && grp2.size()==1) { 
				group1 = grp1.first();
				group2 = grp2.first();
			}
			
			String [] colNames = new String [nGroup1+nGroup2];
			int c=0;
			for (int i=0; i<nGroup1; i++) colNames[c++] = group1;
			for (int i=0; i<nGroup2; i++) colNames[c++] = group2;
			
			Out.Print("Collecting count data (may take several minutes)");
			
			rs = mDB.executeQuery("select count(*) from contig");
			rs.first();
			int nSeq = rs.getInt(1);
			String [] rowNames = new String[nSeq];
			double[] gc = new double[nSeq];
			TreeMap <Integer, Integer> seqMap = new TreeMap <Integer, Integer> ();
			
		/* Get sequences (don't change contig order) */
			rs = mDB.executeQuery("select contigid, ctgid, gc_ratio " +
					"from contig order by ctgid asc limit " + nSeq); // for testing
			int seqIndex = 0;
			while (rs.next()) {
				int seqID = rs.getInt(2);
				seqMap.put(seqID, seqIndex); // if assembled, not the same
				rowNames[seqIndex] = rs.getString(1);
				gc[seqIndex] = rs.getFloat(3);
				seqIndex++;
			}
			int[][] cntMatrix = new int[nSeq][nRepSamps];
			
		/* Get counts */
			// counts are in database by sequences assembled into contigs (super-sequence).
			// For non-assembled, sequence==super-sequences
			// For assembled, sum(count) for the same contig, lib and rep.
			rs = mDB.executeQuery("select ctgid, lid, rep, sum(count) from clone_exp " +
					" join contclone on contclone.cid=clone_exp.cid " +
					" where lid in (" + strLib + ") " +
					" group by contclone.ctgid, lid, rep");							
			while (rs.next()) {
				int seqID = rs.getInt(1);
				int seqX = seqMap.get(seqID);
				int lid = rs.getInt(2);
				int rep = rs.getInt(3);
				int count = rs.getInt(4);
				int repNum = 0;
				
				Sample sObj = samMap.get(lid);
				if (sObj.nRep.containsKey(0))        repNum = sObj.nRep.get(0);
				else if (sObj.nRep.containsKey(rep)) repNum = sObj.nRep.get(rep);
				else continue;// It could be the total count (rep=0) for a library that has reps	
				
				cntMatrix[seqX][repNum] = count;
			}
		/* Filter */
			int [] counts;
			if (filCnt>0 || filCPM>0) {
				boolean [] isKeep = deLoadFilter(filCnt, filCPM, filCPMn, 
						cntMatrix, colNames, rowNames, allReps);
				// Make counts without !keep, update seqNames, update nSeq
				int remove=0, mSeq=0;
				for (int i=0; i<nSeq; i++) {
					if (!isKeep[i]) remove++;
					else mSeq++;
				}
				Out.PrtSpCntMsg(0, remove, "filtered sequences");
			
				counts = new int[nRepSamps*mSeq];
				int k=0;
				for (int i=0; i<nRepSamps; i++) {
					for (int j=0; j<nSeq; j++) 
						if (isKeep[j]) counts[k++] = cntMatrix[j][i];
				}
				
				String [] newSeqNames = new String[mSeq];
				double [] newGC = new double[mSeq];
				k=0;
				for (int j=0; j<nSeq; j++) 
					if (isKeep[j]) {
						newSeqNames[k] = rowNames[j];
						newGC[k] = gc[j];
						k++;
					}
				rowNames = newSeqNames;
				gc = newGC;
				nSeq = mSeq;
			}
			else {
				counts = new int[nRepSamps*nSeq];
				int k=0;
				for (int i=0; i<nRepSamps; i++)
					for (int j=0; j<nSeq; j++) 
						counts[k++] = cntMatrix[j][i];
			}
			if (re == null) initJRI();
			
        	if (b) Out.Print("Assigning R variables");
        	else   Out.Print("Assigning R variables (see #1)");
        /********************************************
        * XXX these assignments are used by the methods
 		********************************************/
        	re.assign("gc",gc); 				if (b) Out.PrtSpMsg(1, "gc: GC values of sequences");
        	re.assign(rowNamesR,rowNames); 		if (b) Out.PrtSpMsg(1,rowNamesR + ": sequences (row) names");
	        re.assign(grpNamesR, colNames); 	if (b) Out.PrtSpMsg(1,grpNamesR + ": group (column) names");
	     	re.assign(repNamesR, allReps.toArray(new String[0])); 
	     										if (b) Out.PrtSpMsg(1,repNamesR + ": replicate names");
	     	
	        re.assign("counts", counts);  		if (b) Out.PrtSpMsg(1,"counts: counts of sequences");
	        doCmd(b, countDataR + " <- array(counts,dim=c(" + nSeq + "," + nRepSamps + "))");
	        doCmd(b, "rm(counts)");
	        doCmd(b, "rownames(" + countDataR + ") <- " + rowNamesR);
	        doCmd(b, nGroup1R + " <- " + nGroup1);
	        doCmd(b, nGroup2R + " <- " + nGroup2);
	        
	        if (disp>0) doCmd(b, dispR + " <- " + disp);
	     	else if (nGroup1==1 && nGroup2==1) doCmd(b, dispR + " <- 0.1");
		}
		catch(Exception e){ErrorReport.die(e, "performing DB counts");}
	}
	/**************************************************
	 * Assigns column number to Lib+Rep - used by deLoadCountsToR
	 */
	private class Sample {
		public Sample(int i, String n) {
			//lid = i;
			//libName=n;
		}
		public void addN(int rep, int totalRepN) {
			nRep.put(rep,totalRepN);
		}
		TreeMap <Integer, Integer> nRep = new TreeMap <Integer, Integer> ();
	}
	/****************************************************
	 * Produces same cpm as: edgeR.cpm(y, normalized.lib.sizes=FALSE, log=FALSE)
	 * 3Sept18 tested again on hind with latest edgeR. It does not give the exact same answer
	 * when edgeR samSize = lib.size*norm.factors (i.e. edgeR computed norm.factors are used)
	 */
	private boolean [] deLoadFilter(int filCnt, int filCPM, int filCPMn, 
			int [][]cntMatrix, String [] colNames, String [] rowNames, Vector <String> repNames) {
		
		int nSeq = rowNames.length;
		int nRepSamp = colNames.length;
		boolean [] isKeep = new boolean [nSeq];
		int cntPrt=0, cntZero=0;
		
		if (filCnt>0) { /** Count **/
			Out.Print("Using count filter >" + filCnt );
			for (int s=0; s<nSeq; s++) {
				int zero=0;
				isKeep[s]=false;
				for (int i=0; i<nRepSamp; i++) {
					if (cntMatrix[s][i]>filCnt) {
						isKeep[s]=true;
						break;
					}
					else if (cntMatrix[s][i]==0) zero++;
				}
				
				if (!isKeep[s] && QRMain.verbose) {
					if (zero==nRepSamp) cntZero++; 
					if (cntPrt<NUM_PRT_FILTER){
						cntPrt++;
						String line=String.format("%15s ", rowNames[s]);
						for (int i=0; i<nRepSamp; i++) 
							line += String.format("%4d ", cntMatrix[s][i]);
						Out.Print(line);
					}
				}
			}
			if (QRMain.verbose) 
				Out.PrtSpCntMsg(1,cntZero, "sequences with all zero counts");
				
			return isKeep;
		}	
		/** CPM **/
	
		Out.Print("Using CPM filter > " + filCPM  + " for >= " + filCPMn);
		double [] samSize = new double [nRepSamp];
		
		for (int i=0; i<nRepSamp; i++) {
			samSize[i]=0;
			for (int j=0; j<nSeq; j++) 
				samSize[i] += cntMatrix[j][i]; 
		}
		if (QRMain.verbose) {
			Out.Print(String.format("%-10s %s", "Condition", "Sum"));
			for (int i=0; i<samSize.length; i++) 
				Out.Print(String.format("%-10s %d", repNames.get(i), (int) samSize[i]));
			Out.Print(String.format("Show first " + NUM_PRT_FILTER + " filtered: %s     %s : %s\n", "SeqID", "Count", "CPM"));
		}

		for (int s=0; s<nSeq; s++) {
			int cntGoodCPM=0, zero=0;
			isKeep[s]=false;
			for (int i=0; i<nRepSamp; i++) {
				double cpm = 0;
				if (cntMatrix[s][i]>0) 
					cpm = (((double)cntMatrix[s][i]/samSize[i])*1000000.0); 
				else zero++;
				if (cpm>filCPM) cntGoodCPM++; 
			}
			if (cntGoodCPM>=filCPMn) isKeep[s]=true; 
			
			if (!isKeep[s] && QRMain.verbose) {
				if (zero==nRepSamp) cntZero++; 
				if (cntPrt<NUM_PRT_FILTER){
					cntPrt++;
					String line=String.format("               %-15s ", rowNames[s]);
					for (int i=0; i<nRepSamp; i++) 
						line += String.format("%4d ", cntMatrix[s][i]);
					line += " : ";
					for (int i=0; i<nRepSamp; i++) {
						double cpm = (((double)cntMatrix[s][i]/samSize[i])*1000000.0); 
						line += String.format("%4.1f ", cpm);
					}
					Out.Print(line);
				}
			}
		}
		if (QRMain.verbose) 
			Out.PrtSpCntMsg(1, cntZero, "sequences with all zero counts");
		return isKeep;
	}
	
	/*****************************************************
	 * All data is loaded for R. Run DE script
	 */
	private boolean deRunScript(TreeMap<String,Double> scores, String rScriptFile) 
	{
		boolean b=true; // print
     	Out.Print("\nStart R-script ");
     	
     	doCmd(b, "source('" + rScriptFile + "')");
		 
		REXP x;
		double[] pvals=null;
		try {
			x = doCmdx(resultR);
			pvals = x.asDoubleArray();
		}
		catch (Exception e) {Out.PrtError("No R variable 'results' exists"); return false;}
		
        if (pvals==null || pvals.length==0) {
    		Out.PrtError(resultR + " R variable does not contain an array of results (type double)");
    		return false;
    	}
    	x = doCmdx(rowNamesR);
    	String[] ctgs = x.asStringArray();
    	
    	if (ctgs.length==0) {
    		Out.PrtError(rowNamesR + " R variable does not contain an array of row names (type string)");
    		return false;
    	}
     	
    	for (int i = 0; i < ctgs.length; i++) {
    		scores.put(ctgs[i], pvals[i]);
    	}

    	Out.Print("R-script done");
    	return true;
	}
	/*************************************************
	 * save pvalues
	 */
	private void dePrint(TreeMap<String,Double> scores) {
		double [] cutoff =    {     1e-5,    1e-4,    0.001,   0.01,     0.05};
        String [] df = {     "<1e-5", "<1e-4", "<0.001", "<0.01", "<0.05"};
        int [] ct =      {     0,       0,      0,       0,        0};
        for (String x : scores.keySet()) {
    		double de = scores.get(x);
    		for (int i=0; i<ct.length; i++) {
    			if (Math.abs(de) < cutoff[i]) ct[i]++;
    		}
        }
        Out.Print("\nNumber of DE results:");
        Out.Print(String.format("   %5s %5s %5s %5s %5s", df[0], df[1],df[2],df[3],df[4]));
        Out.Print(String.format("   %5d %5d %5d %5d %5d", ct[0], ct[1],ct[2],ct[3],ct[4]));
	}
	/*********************************************************
	 * Create DE column if not exist, else clear
	 * Enter data frin 
	 */
	private void deSaveCols(TreeMap<String,Double> scores, String pColName, 
			TreeSet <String> grp1, TreeSet <String> grp2) 
	{
		try {
			String colName = pColName.substring(2); // so doesn't print P_
			Out.Print("\nSaving " + scores.size() + " scores for " + colName);
			
		/* add column or set to default 3 */
			ResultSet rs = mDB.executeQuery("show columns from contig where field= '" + pColName + "'");
			if (!rs.first()) {
				Out.PrtSpMsg(1, "Adding column to database...");
				mDB.executeUpdate("alter table contig add " + pColName + 
						" double default " + Globalx.dStrNoDE);
			}
			else {
				mDB.executeUpdate("update contig set " + pColName + "=" + Globalx.dStrNoDE);
			}
			mDB.executeUpdate("update assem_msg set pja_msg=NULL");
		
			PreparedStatement ps = mDB.prepareStatement("update contig set " + pColName 
					+ "=? where contigid=?");
			int cntSave = 0, cntNA=0, cnt=0;
			double nan=Globalx.dNaDE; 
			
			mDB.openTransaction(); 
			for (String ctg : scores.keySet()){
				double sc = scores.get(ctg);
				if (scores.get(ctg).isNaN()) {ps.setDouble(1,nan); cntNA++;}
				else ps.setDouble(1,sc); 
				ps.setString(2, ctg);
				ps.addBatch();
				cnt++; cntSave++; 
				if (cntSave==100) {
					cntSave=0;
					Out.r("add " +cnt);
					try{
						ps.executeBatch();
					}
					catch(Exception e){System.err.println("BAD VALUE " + ctg + " " + scores.get(ctg));}
				}
			}
			if (cntSave>0) ps.executeBatch();
			mDB.closeTransaction(); 
			if (cntNA>0) Out.PrtSpMsg(1, cntNA + " NA scores");
			
		/* Set negative if col1<col2 */
			// IF there is one library in each group set, and IF the corresponding LN__ columns exist, 
			// then convert the scores to +/- to encode the direction of DE.
			if (grp1.size() == 1 && grp2.size() == 1)
			{
				String col1 = RPKM + grp1.first();
				String col2 = RPKM + grp2.first();
				rs = mDB.executeQuery("show columns from contig where field='" + col1 + "'");
				boolean col1Exists = rs.first();
				rs = mDB.executeQuery("show columns from contig where field='" + col2 + "'");
				boolean col2Exists = rs.first();
				if (col1Exists && col2Exists) {
					mDB.executeUpdate("update contig set " + pColName + " =-" + pColName + 
							" where " + col1 + "<" + col2);						
				}
			}
			rs.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Adding DE columns");}
	}
	/*******************************
	 *  Metadata goes in libraryDE 
	 */
	private void deSaveMethod(String pColName, TreeSet <String> grp1, TreeSet <String> grp2,
			String rScriptFile, String pvalFile, double disp) {
		try {
			String title = "";
			for (String lib : grp1) title += lib + " ";
			title += ": ";
			for (String lib : grp2) title += lib + " ";
			if (title.length()>100) title = title.substring(0,99);
		
			String sMethod = "";
			if (!rScriptFile.equals("")) {
				String file = rScriptFile.substring(rScriptFile.lastIndexOf("/")+1);
				if (file.length()>20) file = file.substring(0,20);
				sMethod = file;
			}
			else if (!pvalFile.equals("")) {
				String file = pvalFile.substring(pvalFile.lastIndexOf("/")+1);
				if (file.length()>20) file = file.substring(0,20);
				sMethod = file;
			}
			else Out.die("TCW error in save method");
			
			ResultSet rs = mDB.executeQuery("Select title from libraryDE where pCol='" + pColName + "'");
			if (rs.first()) {
				Out.PrtSpMsg(1, "Overwrite existing " + pColName);
				if (mDB.tableExists("go_info")) { //CAS321 was not removing Go data
					String goMethod = mDB.executeString("select goMethod from libraryDE where pCol='" + pColName + "'");
					if (goMethod!=null && goMethod!="") {
						Out.PrtSpMsg(1, "Remove GO p-values for " + pColName);
						mDB.tableCheckDropColumn("go_info", pColName);
					}
				}
				mDB.executeUpdate(
					"update libraryDE set title='" + title + "', method='" + sMethod + "', goMethod='', goCutoff=-1.0" +
						" where pCol='" + pColName + "'");
			}
			else {
				mDB.executeUpdate("insert into libraryDE set pCol='" + 
						pColName + "', title='" + title +  "', method='" + sMethod +"'");
			}
			rs.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Adding DE metadata");}
	}
/****************************************************************************************/
	/***********************************************
	 * XXX GOSeq enrichment (over-represented)
	 * colNames: 	Names of all p-value columns 
	 * col: 		Name of the one selected column
	 * doAll:		use colNames (process all) or col (process one)
	 * usePercent:	true: use the top N%  false: use p-value<N
	 * pCutoff:		use this cutoff for N
	 */
	public void goRun(boolean b, String colName, boolean usePercent, double pCutoff, String rScriptFile)
	{
		String pColName = pColPrefix + colName;
		
		long startTime = Out.getTime();
		try {	    		
    		double cutoff = pCutoff; 
			if (usePercent) { // figure out cutoff for Top N%
				int nThresh = (int)((cutoff*nSeq)/100);
				double thresh2 = mDB.executeFloat("select abs(" + pColName +  ") from contig " +
						"where PIDgo > 0 order by " + pColName + " asc limit " + nThresh);
				String tx = String.format("%.4f", thresh2); // limit precision
				Out.PrtSpMsg(1, pCutoff + "% = seq p-value cutoff:" + tx);
				cutoff = Double.parseDouble(tx);
			}
			
    	/** load data **/
    		goLoadDEtoR(b, pColName, cutoff, nSeq);
				
		/** run GOseq **/
    		TreeMap<String,Double> scores = new TreeMap<String,Double> ();
    		goRunScript(scores, rScriptFile);
    		
	    /** save results **/
        	goSaveCols(pColName, scores, rScriptFile, cutoff);
	      
            Out.PrtMsgTime("Finished GO enrichment for " + colName, startTime);
		}
		catch(Exception e){
			Out.PrtError("q() out of R in terminal window.");
			ErrorReport.reportError(e, "running GO enrichment");
			return;
		}
	}
	/************************************************************8
	 * Write R values
	 */
	private boolean goLoadDEtoR(boolean b, String pColName, double cutoff, int nSeq) {
		try {
			if (b) Out.Print("Assigning R variables");
        	else   Out.Print("Assigning R variables (see #1)");
	        doCmd(false, "rm(list=ls())"); // remove all variables from previous run
	        
	 /* get names, lengths and pvalue */
			int[] lens = new int[nSeq]; 
        	String[] names = new String[nSeq]; 
        	int[] binaryDE = new int[nSeq]; 
 		    int cntDE = 0, n = 0;
	    	
        	ResultSet rs = mDB.executeQuery("select contigid, consensus_bases, " + pColName +
        			" from contig where PIDgo > 0 order by ctgid asc"); 
        	
        	if (rs==null) {
        		Out.Print("select contigid, consensus_bases, " + pColName  +
            			" from contig where PIDgo > 0 order by ctgid asc");
        		Out.die("null rs");
        	}
        	while (rs.next()) {
        		names[n] = rs.getString(1);
        		lens[n] =  rs.getInt(2);
        		double p = Math.abs(rs.getDouble(3));
        		int de = (p < cutoff ? 1 : 0); 
        		binaryDE[n] = de;
        		cntDE += de;
        		n++;
        	}
        	rs.close();
        	if (b) Out.PrtSpCntkMsg(0, n, "Sequences with GOs (from " + nSeq + ")");
        	
        	re.assign(seqNamesR, names);	if (b) Out.PrtSpMsg(1, seqNamesR + ": sequence names");
        	re.assign(seqLensR, lens);		if (b) Out.PrtSpMsg(1, seqLensR + ":  sequence lengths");
			doCmd(b, nSeqsR + " <- " + nSeq);		
			
        	re.assign(seqDEsR,binaryDE);	Out.PrtSpMsg(1, seqDEsR + ": DE binary vector (" + cntDE + " seq p-value < " + cutoff + ")" );
        	doCmd(b, "names(" + seqDEsR + ") <- " + seqNamesR);
        	
        	//doCmd(b, "names(seqDE) <- seqNames");
			//doCmd(b, "deSeqNames <- seqNames");       // deSeqNames assigned null if no p-value		
			doCmd(b, seqGOsR + " <- vector(\"list\"," + nSeqsR + ")");
			
  /* get GOs for all direct and indirect per contig and write to R */
			if (b) Out.PrtSpMsg(1, "For all n: seqGOs[[n]] <- c(gonum list)");
			
			Vector<Integer> seqGOs = new Vector<Integer>();
			boolean haveMore=true;
			int gonum;
			String curID = "", seqID="";
			n = 1; 
			int noGO=0;
			
			rs = mDB.executeQuery("select c.contigid, p.gonum " +
					" from contig as c  " +
					" left join pja_unitrans_go as p on p.ctgid=c.ctgid " +
					" where c.PIDgo > 0 order by c.ctgid asc");	
			
			while (haveMore) {
				haveMore = rs.next();
				if (haveMore) { 
					seqID = rs.getString(1);
					gonum = rs.getInt(2);
					if (curID.equals("")) curID = seqID;
				} 
				else { // last one
					gonum=0;
					curID="done";
				}
			
				if (!seqID.equals(curID)) {
					if (seqGOs.size() > 0) {
						StringBuilder str = new StringBuilder("");
						str.append("\"" + seqGOs.firstElement() + "\"");
						for (int i = 1; i < seqGOs.size(); i++) {
							str.append(",\"" + seqGOs.get(i)  + "\"");
						}
						String cmd = seqGOsR + "[[" + n + "]] <- c(" + str.toString() + ")";
						re.eval(cmd);  
						seqGOs.clear();
					}
					else {   // this replaces name with NA; if pid is PIDgo, this will not happen
						String cmd =  seqNamesR + "[" + n + "] <- NA";
						re.eval(cmd);
						noGO++;
					}
					curID = seqID;
					n++;
					if (n%1000 == 0) Out.r("Process sequence #" + n);
				}
				
				if (gonum > 0)	seqGOs.add(gonum);
			}
			Out.r("                                                  ");
			
			doCmd(b, "names( " + seqGOsR + ") =  " + seqNamesR); 
			//doCmd(b, "np <- nullp(seqDE,'','',seqLens,FALSE)"); 
			
			Out.PrtSpCntMsgNz(0, noGO, "No GOs for Sequences");
			return true;
		}
		catch (Exception e) {
			ErrorReport.die(e, "Assigning R values for GO enrichment for " + pColName);
			return false;
		}
	}
	/*****************************************************
	 * All data is loaded for R. Run DE script
	 */
	private boolean goRunScript(TreeMap<String,Double> scores, String rScriptFile) 
	{
		boolean b=true; // print
     	Out.Print("\nStart R-script ");
     	
     	doCmd(b, "source('" + rScriptFile + "')");
		 
		REXP x;
		double[] pvals=null;
		try {
			x = doCmdx(resultR);
			pvals = x.asDoubleArray();
		}
		catch (Exception e) {Out.PrtError("No R variable 'results' exists"); return false;}
		
        if (pvals==null || pvals.length==0) {
    		Out.PrtError(resultR + " R variable does not contain an array of results (type double)");
    		return false;
    	}
    	x = doCmdx(gonumsR);
    	String[] gos = x.asStringArray();
    	
    	if (gos.length==0) {
    		Out.PrtError(gonumsR + " R variable does not contain an array of row names (type string)");
    		return false;
    	}
     	
    	for (int i = 0; i < gos.length; i++) {
    		scores.put(gos[i], pvals[i]);
    	}

    	Out.Print("R-script done");
    	return true;
	}
	/***************************************************************
	 * Save GO
	 */
	private void goSaveCols(String pColName, TreeMap <String, Double> scores, String rScriptFile, double cutoff) {
		try {
			Out.Print("\nSaving " + scores.size() + " values to database");
			
			ResultSet rs = mDB.executeQuery("show columns from go_info where field='" + pColName + "'");
        	if (!rs.first()) {
        		Out.PrtSpMsg(1, "Adding column to go_info table");
        		mDB.executeUpdate("alter table go_info add " + pColName + " double default " + Globalx.dStrNoDE);             
        	}
        	else mDB.executeUpdate("update go_info set " + pColName + "=" + Globalx.dStrNoDE);
    
        	mDB.executeUpdate("update assem_msg set pja_msg=NULL");
        	int n = scores.size(); 
        	int cntSave=0, cnt=0;
        	
        	mDB.openTransaction(); 
			PreparedStatement ps = mDB.prepareStatement(
					"update go_info set " + pColName + "=? where gonum=?");
			int cnt05=0;
			for (String goStr : scores.keySet()) {
				int gonum = Integer.parseInt(goStr);
				double pval = scores.get(goStr);
				ps.setDouble(1, pval);
				ps.setInt(2,gonum);
				ps.addBatch();
				
				cnt++; cntSave++;
				if (cntSave==1000) {
					Out.rp("Save ", cnt, n);
					ps.executeBatch();
					cntSave=0;
				}
				if (pval<0.05) cnt05++;
			}	
			if (cntSave>0) ps.executeBatch();
			ps.close();
			mDB.closeTransaction(); 
			
			Out.PrtSpCntMsg(0, cnt05, "GO p-values < 0.05 ");
			
			String sMethod = "unknown";
        	if (!rScriptFile.equals("")) {
				String file = rScriptFile.substring(rScriptFile.lastIndexOf("/")+1);
				if (file.length()>20) file = file.substring(0,20);
				sMethod = file;
			}
        	mDB.executeUpdate("update libraryDE set goCutoff=" + cutoff + ",goMethod='" + sMethod + 
        			"' where pCol='" + pColName + "'");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Saving p-values for GO enrichment " + pColName);}
	}
	
	/***********************************************
	 * XXX JRI
	 */
	private void initJRI()
	{
		try {
			if (re == null) {
				Out.Print("\nStartup R");
				Map<String, String> env = System.getenv();
				boolean flag=false;
			    for (String envName : env.keySet()) {
		            if (envName.contains("R_HOME")) {
		            		System.err.format("%s=%s\n\n", envName,env.get(envName));
		            		flag=true;
		            }
			    }
			    if (!flag) Out.PrtWarn("$R_HOME not set\n");
			    Out.Print("Checking if runDE has the correct java.library.path....");
				if (!Rengine.versionCheck()) {
				    Out.PrtError("Version mismatch - Java files don't match library version.");
				    System.exit(1);
				}
				re= Rengine.getMainEngine();
				String[] args = {"--vanilla"};
				re = new Rengine(args, true, new TextConsole());
		        Out.Print("Rengine created, waiting for R");
		        
				// the engine creates R is a new thread, so we should wait until it's ready
		        if (!re.waitForR()) {
		            Out.PrtError("Cannot load R");
		            return;
		        }
			}
			else {
				Out.Print("Use existing R session");
				doCmd(true, "rm(list=ls())");
			}
		}
		catch(Exception e){ErrorReport.die(e, "starting JRI");}
	}
	private void doCmd (boolean b, String cmd) {
		if (b) Out.PrtSpMsg(1, cmd);
		re.eval(cmd);
	}
	private REXP doCmdx (String cmd) {
		Out.PrtSpMsg(1, cmd);
		return re.eval(cmd);
	}
	private void getPackages(HashSet<String> pkgs) {
    	REXP x = re.eval("rownames(installed.packages())");
    	for (String pkg : x.asStringArray()) {
    		pkgs.add(pkg);
    	}		
	}
	private boolean checkPackage(String pkg) {
		if (!packages.contains(pkg)) {
			Out.Print("*****R package " + pkg + " needs to be installed.******");
			return false;
		}
		return true;
	}
	private class TextConsole implements RMainLoopCallbacks
	{
	    public void rWriteConsole(Rengine re, String text, int oType) {
	        System.err.print(text);
	    }
	    
	    public void rBusy(Rengine re, int which) {
	        //System.err.println("rBusy("+which+")");
	    }
	    
	    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
	        System.err.print(prompt);
	        try {
	            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
	            String s=br.readLine();
	            return (s==null||s.length()==0)?s:s+"\n";
	        } catch (Exception e) { Out.PrtError("jriReadConsole exception: "+e.getMessage()); }
	        return null;
	    }
	   
	    public void rShowMessage(Rengine re, String message) {
	        System.err.println("rShowMessage \""+message+"\"");
	    }
		
	    public String rChooseFile(Rengine re, int newFile) {
			FileDialog fd = new FileDialog(new Frame(), (newFile==0)?"Select a file":"Select a new file", (newFile==0)?FileDialog.LOAD:FileDialog.SAVE);
			fd.setVisible(true);
			String res=null;
			if (fd.getDirectory()!=null) res=fd.getDirectory();
			if (fd.getFile()!=null) res=(res==null)?fd.getFile():(res+fd.getFile());
			return res;
	    }
	    
	    public void   rFlushConsole (Rengine re) {}
	    public void   rLoadHistory  (Rengine re, String filename) {}			
	    public void   rSaveHistory  (Rengine re, String filename) {}			
	}
	private String prtName(String pColName) {
		return pColName.substring(2);
	}
	/** private variables **/
	private DBConn mDB;
	private static Rengine re = null;
	private HashSet<String> packages;
	private int nGroup1, nGroup2;
	private String dbName="";
	private int nSeq=0;
}
