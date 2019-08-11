package cmp.compile;
/*****************************************************
 * Creates Overview (summary) that is display on viewMulti
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;

import java.util.Vector;
import java.text.DecimalFormat; 

import cmp.database.DBinfo;
import cmp.database.Globals;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.TimeHelpers;
import util.methods.Out;

public class Summary {
	public static String padTitle = "   ";
	public static String pad =      "      ";
	private static final double DEcutoff = 0.001; 
	
	public Summary (DBConn dbc) {
		mDB = dbc;
	}
	
	public void removeSummary() { // can track through this where db is altered
		try {
			mDB.executeUpdate("UPDATE info SET summary='', seqSQL='', pairSQL='', grpSQL=''");
		}
		catch (Exception e) {ErrorReport.die(e, "create/get summary");}
	}
	
	public String getSummary() {
		String sum="";
		try {
			ResultSet rs = mDB.executeQuery("SELECT summary FROM info");
			if(rs.first()) sum  = rs.getString(1);
			rs.close();
			if (sum==null || sum.trim().equals("")) sum = updateSummary();
		}
		catch (Exception e) {ErrorReport.die(e, "create/get summary");}
		return sum;
	}
	/******* CompileMain - after "Add Methods" *******************/
	public String getMethodSizeTable() {
		String text="";
		try {
			makeMethodsTable();
			lines.clear(); // just want sizes, but need initialization from makeMethods
			makeMethodSizeTable();
			for (int i=0; i< lines.size(); i++) text += lines.get(i) + "\n";
		}
	 	catch (Exception e) {ErrorReport.prtReport(e, "create/get summary");}
	 	return text;
	}
	public String getMethodScoreTable() {
		String text="";
		try {
			makeMethodsTable();
			lines.clear(); // just want sizes, but need initialization from makeMethods
			makeMethodScoreTable();
			for (int i=0; i< lines.size(); i++) text += lines.get(i) + "\n";
		}
	 	catch (Exception e) {ErrorReport.prtReport(e, "create/get summary");}
	 	return text;
	}
	
	public String updateSummary() {
		Out.Print("\nUpdate overview");
		String text="";
		try {
			info = new DBinfo(mDB); 
			ResultSet rs = mDB.executeQuery("SELECT DATABASE()");
			rs.next();
			dbName = rs.getString(1);
			dbName = dbName.substring(Globals.MTCW.length()+1);
			
			nGrp = info.getCntGrp();
			nPairs = info.getCntPair();
			nSeq = info.getCntSeq();
			int nGO = info.getCntGO(); 
			String line = "Project: " + dbName + "  multiTCW " + Globals.VERSION + 
	        		"  Cluster: " + df.format(nGrp) + "  Pairs: " + df.format(nPairs) +
	        		"  Sequences: " + df.format(nSeq);
			if (nGO>0) line += "  GOs: " + df.format(nGO);
	        lines.add(line);
	        
	        Out.PrtSpMsg(1, "Datasets....");
	        makeDatasets();
	        
	        Out.PrtSpMsg(1, "Methods....");
	        	makeMethods();
	        	
	        	Out.PrtSpMsg(1, "Pairs....");
	        	makePairs();
	        
	        	Out.PrtSpMsg(1, "Sequences....");
	        	makeSeqs();
	       
	        	makeProcessing();
	        	makeLegend();
	        	
	 		for (int i=0; i< lines.size(); i++)
	 			text += lines.get(i) + "\n";
	 	
	 		mDB.executeUpdate("UPDATE info SET summary = \"" + text + "\""); 
	     	writeOverview(text);
	     	Out.PrtSpMsg(0, "Complete overview");
		}
		catch (Exception e) {ErrorReport.die(e, "create/get summary");}
	    return text;
    }   
   
    /**************************************************************
     * Information about Species in compare database
     */
    private void makeDatasets() {
    		ResultSet rs;
    		try {
    			String date = "";
				
    			rs = mDB.executeQuery("SELECT compiledate FROM info");
    			if (rs.next()) 
    				date = TimeHelpers.convertDate(rs.getString(1));
    			else 
    				Out.die("Fatal error reading info from database");
    			   			
    			nAsm = mDB.executeCount("SELECT COUNT(*) FROM assembly");
    			
    			lines.add("");
    			lines.add("DATASETS: " + nAsm + "  Created " + date);
    			
    		    String[] fields = {"", "Type", "#Seq", "#annotated",  
    		    		 "#annoDB",  "Created", "Remark"};
    	        int [] just = {1,1,0, 0,0,1,1};
    	        int nCol=fields.length;
    			String [][] rows = new String[nAsm+1][nCol];
    	
    	        rs = mDB.executeQuery("SELECT ASMid, ASMstr,  nUT, nAnnoUT,   " +
    	        		"nAnnoDB, assemblydate, remark, isPep from assembly");
    	   		int r=0;
    	   		while(rs.next()) {
    	   			assmID.add(rs.getInt(1));
    	   			String asmstr = rs.getString(2);
    	   			int nSeq = rs.getInt(3);
    	   			int nAnno = rs.getInt(4);
    	   			int nDB = rs.getInt(5);
    	   			date = rs.getString(6);
    	   			String rmk = rs.getString(7);
    	   			boolean isPep = rs.getBoolean(8);
    	   			String type = (isPep) ? "AA" : "NT";
    	   			assmStr.add(asmstr);
    	   			
    	   			rows[r][0] = asmstr;
    	   			rows[r][1] = type;
    	   			rows[r][2] = df.format(nSeq);
    	   			
    	   			rows[r][3] = df.format(nAnno); // annoSeq
    	   			rows[r][4] = df.format(nDB); // #annoDB
    	   			rows[r][5] = TimeHelpers.convertDate(date);
    	   			rows[r][6] = rmk; 
    	   			r++;
            } 
    	   		
    	   		rs.close();
    	   		Out.makeTable(lines, nCol, r+1, fields, just, rows);
    		}
    		catch (Exception e) {ErrorReport.die(e, "create dataset Table for summary");}
    }
    
    /************************************************************************
     * XXX Methods and Clusters
     *******************************************************************/
    private void makeMethods()
    {
		try {
		 	makeMethodsTable();
			if (nMethod==0) return;
			
			makeMethodSizeTable();
			lines.add("");
			makeMethodScoreTable();
		}
	    catch (Exception err) {ErrorReport.prtReport(err, "reading database for Clusters");}
    }
    /********************************************************
     * Table of Methods in compare database
     */
    private void makeMethodsTable() 
    {
   		ResultSet rs;
		try { 			      
	        nMethod = mDB.executeCount("SELECT COUNT(*) FROM pog_method");
	 		
			lines.add("CLUSTER SETS: " + nMethod);
	        if (nMethod==0) return;
	        	
	        String[] fields = {"Prefix", "Method", "Parameters"};
	        int [] just = {1,1,1};
	        int nCol=fields.length;
			String [][] rows = new String[nMethod][nCol];
	
	        String strQ = "SELECT PMid, PMtype, prefix, description from pog_method order by PMtype";
	   		rs = mDB.executeQuery(strQ);
	   		int r=0;
	   		while(rs.next()) {
	   			int pmid = rs.getInt(1);
	   			String type = rs.getString(2);
	   			String prefix = rs.getString(3);
	   			String desc = rs.getString(4);
	   			rows[r][0] = prefix;
	   			rows[r][1] = type;
                rows[r][2] = desc;
	            r++;
	            methods.add(pmid + " " + prefix);
            } 
	   		rs.close();	   		
	   		Out.makeTable(lines, nCol, r, fields, just, rows);
	   		lines.add("");
		}
		catch (Exception e) {ErrorReport.die(e, "create Method Table for summary");}
    }
    private void makeMethodSizeTable() 
	{	
    	try {
    		// info may not be set, do directly get counts
    		int nSet = mDB.executeCount("select count(*) from assembly");
    		int maxSize = mDB.executeCount("select max(count) from pog_groups");
    		int nSeq = mDB.executeCount("select count(*) from unitrans");
    		 
	    String [] dfields = {"Prefix", "=2", "3-5", "6-10", "11-20", "21-30", "31-40", "41-50", ">50", "Total", "#Seqs"};
	    int [] djust = 	    {1,         0,     0,     0,       0,      0,        0,        0,     0,    0,  0};
	    int[] start=                   {1,     2,     5,      10,      20,      30,       40,     50};
	    int[] end=                     {2,     5,     10,     20,      30,      40,       50,    100000000};

	    // if there are N input sets, then want number of exactly N to be one of dfields
	    if (nSet<8) {
		    int c=1;
		    for (int i=2, j=0; j<nSet-1; i++, j++, c++) {
		    		dfields[c] = 	"=" + i;
		    		start[c-1] = 	i-1;
		    		end[c-1] = 		i;
		    }
		    // inc of 5 up to the > 
		    int low=nSet+1;
		    int high= (nSet<4) ? 5 : 10;
		    while (c<dfields.length-3) {
		    		dfields[c] = low + "-" + high;
		    		start[c-1] = low-1;
		    		end[c-1] = high;
		    		c++;
		    		low = high+1;
		    		high += 5;
		    }
		    	dfields[c] = ">" + (low-1);
		    	start[c-1] = low-1;
		    	end[c-1] = maxSize+10;
	    }
	    
	    int nSiz = start.length;
	    int nCol=dfields.length;
	    String [][] rows = new String[nMethod][nCol+1];
	    
    		lines.add(padTitle + "Sizes:");
	    	for (int r = 0; r < methods.size(); r++) {
	    		String [] s = methods.get(r).split(" ");
	    		rows[r][0] = s[1];
	    		int total=0;
	    		for (int i = 0; i<nSiz; i++) {
	    			int cnt =  mDB.executeCount("SELECT count(*) FROM pog_groups " +
		   	          " WHERE count > " + start[i] + " AND count <= " + end[i] + 
		   	          " AND PMid = " + s[0]);
	    			rows[r][i+1] = df.format(cnt);
	    			total += cnt;
	    		}
	    		rows[r][nCol-2] = df.format(total);
	    		
	    		total = mDB.executeCount("SELECT SUM(count) from pog_groups " +
	    				" WHERE PMid = " + s[0]);
	    		rows[r][nCol-1] = Out.perFtxt(total, nSeq);
	    		if (total==0) {
	    		    System.err.println("+++ No clusters found for " + rows[r][0]);  		    
	    		}
	    }
	    Out.makeTable(lines, nCol, nMethod, dfields, djust, rows);
	}
	catch (Exception err) {ErrorReport.prtReport(err, "reading database for Group count");}	
	}

    private void makeMethodScoreTable() {
    try {
    		int cnt = mDB.executeCount("select count(*) from pog_groups where score1>0 limit 1");
    		if (cnt==0) return; 
    		
    		String [] dfields = {"Prefix", "conLen", "sdLen", "   Score","SD  ", Globals.MultiAlign.score2, "SD  "};
    		int [] djust = 	    {1,      0,     0,        0,     0,     0,       0};
    		int nCol = djust.length;
    		String [][] rows = new String[nMethod][nCol];
   	    
    		lines.add(padTitle + "Statistics:");
	    for (int r = 0; r < methods.size(); r++) {
	    		String [] s = methods.get(r).split(" ");
	    		rows[r][0] = s[1];
	    		
	    		ResultSet rs = mDB.executeQuery(
	    				"SELECT AVG(conLen), AVG(sdLen), " +
	    				"AVG(score1), STDDEV(score1), AVG(score2), STDDEV(score2) FROM pog_groups " +
			   	          " WHERE PMid = " + s[0]);
	    		if (!rs.next()) {
	    			Out.PrtError("Cannot get averages for overview");
	    			return;
	    		}
	    		double conLen = rs.getDouble(1);
	    		if (conLen<=0) {
	    			for (int i=1; i<nCol; i++) rows[r][i] = "-";
	    		}
	    		else {
	    			for (int i=1; i<nCol; i++) {
	    				rows[r][i] = String.format("%.2f", rs.getDouble(i));
	    			}
	    		}
	    	}
	    Out.makeTable(lines, nCol, nMethod, dfields, djust, rows);
    }
    catch (Exception e) {ErrorReport.prtReport(e, "reading database for cluster scores");}	
    }
    // XXX Pairs
    private void makePairs() {
    	try {
    		lines.add(" ");
    		
    		if (nPairs==0) {
    			lines.add("Pairs not computed yet");
    			return;
    		}
    		lines.add("PAIRS: " + String.format("%,d", nPairs)); 
    		lines.add(padTitle + "Hits");
    		String msg=""; // The following align with stats from ScoreCDS
    		
    		if (mDB.tableColumnExists("info", "aaInfo")) { 
    			msg = mDB.executeString("select aaInfo from info");
    			lines.add(pad + msg);
    		}	
    		if (mDB.tableColumnExists("info", "ntInfo")) {
    			msg = mDB.executeString("select ntInfo from info");
    			if (msg!=null && !msg.equals("")) lines.add(pad + msg);
    		}
  		
 		// pairInfo created in PairStats during compile
  		String text = mDB.executeString("select pairInfo from info");
  		if (text!=null) {
  			lines.add("");
  			String [] pline = text.split("\n");
  			for (String l:pline) lines.add(l);
  		}
  		
  		// kaksInfo created in Pairwise.java during compile
	 	int ks  = mDB.executeCount("SELECT count(*) FROM pairwise where pVal!=-2.0");
	 	if (ks>0) {
	 		lines.add("");
	 		text = mDB.executeString("select kaksInfo from info");
	 		String [] kline = text.split("\n");
		 	for (String l:kline) lines.add(l);
	 	}
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "reading database for Pairs");}	
    }
    // XXX sequences
    private void makeSeqs() {
    	try {
    		lines.add("");
    		lines.add("SEQUENCES: " + String.format("%,d", nSeq)); 
    		
    		String msg = mDB.executeString("select seqInfo from info");
   		if (msg!=null && !msg.equals("")) {
   			String [] x = msg.split("\n");
   			for (String l : x) lines.add(l);
   			lines.add("");
   		}
   	 	makeSeqExplevel();
	    makeSeqDE();
    	}
    	catch (Exception e) {ErrorReport.prtReport(e, "reading database for Seqs");}	
    }
    private void makeSeqDE() {
		ResultSet rs;
		try {
			String [] deCol = info.getSeqDE();
			int nDE = deCol.length;
			if (nDE == 0) return;
			
			lines.add(padTitle + "Differential Expression (p-value < " + DEcutoff + "): ");
			
			// make Column names for table; indexes are 1 to nLib (0 is assembly name)
			int nCol = nDE+1;
			String [][] rows = 		new String[nAsm][nCol];
			int [][]    deCnt = 		new int[nAsm][nCol];
		    String[]    deFields = 	new String[nCol];
		    int []      deJust = 	new int[nCol];
		    int [][]    allCnt =		new int[nAsm][nCol];
		    
	    		deFields[0] = "";
	    		deJust[0] = 1;
		    for (int i=0; i< nDE; i++) {
		    		deFields[i+1] = deCol[i];
		    		deJust[i+1] = 0;
		    }

			for (int i=0; i< nAsm; i++)
				for (int j=0; j < nCol; j++) {
					deCnt[i][j]=0;
					allCnt[i][j]=0;
				}
			String deSQL = info.getSeqDESQL(); // same order as columns
			// for each assembly, sum values in expList (using unnormalized)
	        for (int r=0; r< assmID.size(); r++) 
	        {
		        	rs = mDB.executeQuery("SELECT " + deSQL + " FROM unitrans " +
		        			"WHERE ASMid = " + assmID.get(r) + " and aaLen>0");
		        	// loop through all sequences
		        	while (rs.next()) {
					for(int i=1; i<=deCol.length; i++) {
						double d = rs.getDouble(i);
						if (d != Globalx.dNaDE) { 
							if (Math.abs(d) < DEcutoff) deCnt[r][i]++;
							allCnt[r][i]++; // distinguish no DE for this column vs no <0.5 DE
						}
	       	 		}
		        	} 
		     	rs.close();
	        }
	   		
	   		// create table
	        for (int r=0; r< assmID.size(); r++) 
	        {
		        	rows[r][0] = assmStr.get(r);
		        	for (int c=1; c < nCol; c++) {
		        		if (allCnt[r][c] > 0)  
		        			 rows[r][c] = Out.kbText(deCnt[r][c]); 
		        		else rows[r][c] = "--";
		        	}
	        }
	        Out.makeTable(lines, nCol, nAsm, deFields, deJust, rows);
	        lines.add("");
		}
		catch (Exception e) {ErrorReport.die(e, "create dataset DE Table for summary");}
    }
    private void makeSeqExplevel() {
		ResultSet rs;
		try {
			int cnt = mDB.executeCount("select hasLib from info");// if no expression libraries
			if (cnt==0) return;
			
			String [] libCol = info.getSeqLib();
			int nLib = libCol.length;
			lines.add(padTitle + "Counts  ");
			
			// make Column names for table; indexes are 1 to nLib (0 is assembly name)
			int nCol = nLib+1;
			String [][] rows = 		new String[nAsm][nCol];
			int [][] 	expCnt = 	new int[nAsm][nCol];
		    String[] 	libFields = 	new String[nCol];
		    int [] 		libJust = 	new int[nCol];
		    
	    		libFields[0] = "";
	    		libJust[0] = 1;
		    for (int i=0; i< nLib; i++) {
		    		libFields[i+1] = libCol[i];
		    		libJust[i+1] = 0;
		    }

			for (int i=0; i< nAsm; i++)
				for (int j=0; j < nCol; j++) expCnt[i][j]=0;
	
			// for each assembly, sum values in expList (using unnormalized)
	        for (int r=0; r< assmID.size(); r++) 
	        {
		        	rs = mDB.executeQuery("SELECT  expList FROM unitrans " +
		        			"WHERE ASMid = " + assmID.get(r) + " and aaLen>0");
		        	// loop through all unitrans
		        	while (rs.next()) {
		        		String libCounts = rs.getString(1);
		    			String [] groups = libCounts.split("\\s+");
					for(int i=0; i<groups.length; i++) {
						String [] pair = groups[i].split("=");
						if (pair.length==2) { 
							int c=-1;
							for (int j=0; j<nLib && c== -1; j++) 
								if (libCol[j].equals(pair[0])) c = j;
	       	 				int v = Integer.parseInt(pair[1]);
	       	 				if (v>0) expCnt[r][c+1] += v;
						}
	       	 		}
		        	} 
		     	rs.close();
	        }
	   		
	   		// create table
	        for (int r=0; r< assmID.size(); r++) 
	        {
		        	rows[r][0] = assmStr.get(r);
		        	for (int c=1; c < nCol; c++) {
		        		int x=expCnt[r][c];
		        		if (x==0) rows[r][c] = "--";
		        		else      rows[r][c] = df.format(x);
		        	}
	        }
	        Out.makeTable(lines, nCol, nAsm, libFields, libJust, rows);
	        lines.add("");
		}
		catch (Exception e) {ErrorReport.die(e, "create dataset Table for summary");}
    }
    
    // XXX Processing
    private void makeProcessing() {
    	try {
    		String aa = ""; 
    		if (mDB.tableColumnExists("info", "aaPgm"))
    			aa = mDB.executeString("select aaPgm from info");
    		if (aa==null) aa="";
    		
    		String nt = ""; 
		if (mDB.tableColumnExists("info", "ntPgm"))
			nt = mDB.executeString("select ntPgm from info");
		if (nt==null) nt="";
		
		if (aa=="" && nt=="") return;
		
		lines.add("----------------------------------------------------");
		lines.add("PROCESSING:");
		lines.add("AA " + aa);
		lines.add("NT " + nt);
    	}
	catch (Exception e) {ErrorReport.prtReport(e, "processing");}	
    }
    private void makeLegend() {
    	
    }
	public void writeOverview(String text) {
		String projDir = Globals.PROJECTDIR;
		String sumDir = Globals.Compile.summaryPath;
		String overFilePath = sumDir + "/" + dbName + ".html";
		try {
			if (!new File(projDir).exists()) return; // should exist
			
			File h = new File(sumDir);
			if (!h.exists()) {
				System.out.println("Creating directory " + sumDir);
				h.mkdir();
			}
			FileOutputStream out = new FileOutputStream(overFilePath);
			PrintWriter fileObj = new PrintWriter(out); 
    			fileObj.println("<html><pre>\n" + text + "\n</pre></html>");
    			fileObj.close();
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error writing to " + overFilePath);}
	}
	private String dbName="";
	private DBinfo info;
	private DBConn mDB;  

	private Vector <String> lines = new Vector <String> ();
	private DecimalFormat df = new DecimalFormat("#,###,###");
	private int nAsm=0, nMethod=0, nGrp=0, nPairs=0, nSeq=0;
	private Vector <String> assmStr = new  Vector <String> ();
	private Vector <Integer> assmID = new  Vector <Integer> ();
	private Vector <String> methods = new Vector <String> ();
	
}