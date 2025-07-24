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
			mDB.executeUpdate("UPDATE info SET summary='', seqSQL='', pairSQL='', grpSQL='', hitSQL=''"); // CAS310 had hit
		}
		catch (Exception e) {ErrorReport.die(e, "create/get summary");}
	}
	
	public String getSummary() {
		String sum="";
		try {
			ResultSet rs = mDB.executeQuery("SELECT summary FROM info");
			if (rs.next()) sum  = rs.getString(1); // CAS405 was first
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
			makeMethodsVector();
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
			makeMethodsVector();
			lines.clear(); // just want scores, but need initialization from makeMethods
			makeMethodScoreTable();
			for (int i=0; i< lines.size(); i++) text += lines.get(i) + "\n";
		}
	 	catch (Exception e) {ErrorReport.prtReport(e, "create/get summary");}
	 	return text;
	}
	/*****************************************************
	 * Create Overview
	 */
	public String updateSummary() {
		Out.Print("\nUpdate overview");
		String text="";
		try {
			Out.PrtSpMsg(1, "Header....");
	        makeHeader();
			
	        Out.PrtSpMsg(1, "Datasets....");
	        makeDatasets(); 
	        
	        Out.PrtSpMsg(1, "Methods....");
        	makeMethods(); 
        	
        	Out.PrtSpMsg(1, "Pairs....");
        	makePairs(); 
        	 
        	Out.PrtSpMsg(1, "Sequences....");
        	makeSeqs();
       
        	makeProcessing();
	        	
	 		for (int i=0; i< lines.size(); i++)
	 			text += lines.get(i) + "\n";
	 	
	 		mDB.executeUpdate("UPDATE info SET summary = \"" + text + "\""); 
	     	writeHTML(text);
	     	Out.PrtSpMsg(0, "Complete overview");
		}
		catch (Exception e) {ErrorReport.die(e, "create/get summary");}
	    return text;
    }   
    private void makeHeader() { // CAS310 made this a separate method and date is a separate line, with last update
    	try {
    		info = new DBinfo(mDB); 
			ResultSet rs = mDB.executeQuery("SELECT DATABASE()");
			rs.next();
			dbName = rs.getString(1);
			dbName = dbName.substring(Globals.MTCW.length()+1);
			
			nGrp = info.getCntGrp();
			nPairs = info.getCntPair();
			nSeq = info.getCntSeq();
			int nHit = info.getCntHit();
			
			int nGO = info.getCntGO(); 
			String line = "Project: " + dbName + 
	        		"  Cluster: " + Out.kMText(nGrp) + "  Pairs: " + Out.kMText(nPairs) + 
	        		"  Seqs: " + Out.kMText(nSeq)    + "  Hits: " + Out.kMText(nHit);
			if (nGO>0) line += "  GOs: " + Out.kMText(nGO);
			line += "  ";
			if (info.hasPCC())     line += " PCC";
			if (info.hasStats())   line += " Stats";
			if (info.hasKaKs())    line += " KaKs";
			if (info.hasDBalign()) line += " Multi";
			
	        lines.add(line);
	        
	        String dateLine="";
			
			rs = mDB.executeQuery("SELECT compiledate, version, lastDate, lastVer FROM info");
			if (rs.next())  {
				String bDate = rs.getString(1);
				String date = TimeHelpers.convertDate(bDate);
				String bVer = rs.getString(2);
				
				dateLine = "Created: " + date + " v" + bVer;
				
				String uDate = rs.getString(3);
				String uVer = rs.getString(4);
				if ((uDate!=null && uDate!="" && !bDate.equals(uDate)) 
				 || (uVer!=null  && uVer!=""  && !bVer.equals(uVer))) 
				{
					date = TimeHelpers.convertDate(uDate);	
					dateLine += "     Last Update: " + date + " v" + uVer;
				}
				lines.add("");
				lines.add(dateLine);
			}
			else 
				Out.PrtError("Could not make 'Created date' line");
    	}
    	catch (Exception e) {ErrorReport.die(e, "create header");}
    }
    /**************************************************************
     * Information about Species in compare database
     */
    private void makeDatasets() {
		ResultSet rs;
		try {		
			nAsm = mDB.executeCount("SELECT COUNT(*) FROM assembly");
			
			lines.add("");
			lines.add("DATASETS: " + nAsm);
			
		    String[] fields = {"", "Type", "#Seq", "#annotated",  "#annoDB",  "Created", "Remark"};
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
	   			String date = rs.getString(6);
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
		 	makeMethodsVector();
			if (nMethod==0) return;
			
			makeMethodScoreTable();
			lines.add("");
			makeMethodSizeTable();
		}
	    catch (Exception err) {ErrorReport.prtReport(err, "reading database for Clusters");}
    }
    /********************************************************
     * CAS340 Create description to go at end of Score table
     */
    private void makeMethodsVector() 
    {
   		ResultSet rs;
		try { 			      
	        nMethod = mDB.executeCount("SELECT COUNT(*) FROM pog_method");
			lines.add("CLUSTER SETS: " + nMethod);
	        if (nMethod==0) return;
	        	    
	        String strQ = "SELECT PMid, prefix, PMtype,  description from pog_method order by PMid"; // CAS327 changed from PMtype
	   		rs = mDB.executeQuery(strQ);
	   
	   		while(rs.next()) {
	   			Method m = new Method ();
	   			m.pmid = rs.getInt(1);
	   			m.prefix = rs.getString(2);
	   			String type = rs.getString(3);
	   			String desc = rs.getString(4);
	   			
	   			String [] tok = desc.split(";");
	   			int n = tok.length;
	   			String x=type + " ";
	   			
	   			// BBH
	   			if (n>=3 && type.contentEquals(Globals.Methods.BestRecip.TYPE_NAME)) { // Sim 60; Cov 40(Both); AA; NnR,OlR
	   				if (n==4) x += tok[3].trim() + " "; // sTCWdbs
	   				if (tok[2].trim().contentEquals("NT")) x += "NT";
	   			} 
	   			// Closure
	   			else if (n>=3 && type.contentEquals(Globals.Methods.Closure.TYPE_NAME)) {// Sim 60; Cov 40(Both); AA
	   				if (tok[2].trim().contentEquals("NT")) x += "NT";
	   			}
	   			// Best Hit
	   			else if (n>=4 && type.contentEquals(Globals.Methods.Hit.TYPE_NAME)) { // Sim 20; Cov 20; All; Description
	   				x += tok[3].trim().substring(0,5) + " ";
	   				if (tok[2].trim().contentEquals("Any")) x += "Any";
	   			}
	   			// OrthoMCL
	   			else if (n>=1  && type.contentEquals(Globals.Methods.OrthoMCL.TYPE_NAME)) {// Inflation 4
	   				String [] t = tok[0].split(" ");
	   				x += t[1].trim();
	   			}
	   			// User defined
	   			else if (n>=1 && type.contentEquals(Globals.Methods.UserDef.TYPE_NAME)) { // orthoMCL.OM-40
	   				x = tok[0].trim(); // no method, just file name 
	   			}
	   			else {
	   				x += " ???";
	   				Out.PrtWarn("TCW error - cannot determine method '" + type + "'  '" + desc + "'  " + n);
	   			}
	   			m.method = x;
	            methods.add(m);
            } 
	   		rs.close();	   		
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
	    
		lines.add(padTitle + "Sizes");
    	for (int r = 0; r < methods.size(); r++) {
    		Method m = methods.get(r);
    		rows[r][0] = m.prefix;
    		int total=0;
    		for (int i = 0; i<nSiz; i++) {
    			int cnt =  mDB.executeCount("SELECT count(*) FROM pog_groups " +
	   	          " WHERE count > " + start[i] + " AND count <= " + end[i] + 
	   	          " AND PMid = " + m.pmid);
    			rows[r][i+1] = df.format(cnt);
    			total += cnt;
    		}
    		rows[r][nCol-2] = df.format(total);
    		
    		total = mDB.executeCount("SELECT SUM(count) from pog_groups " +
    				" WHERE PMid = " + m.pmid);
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
		int cnt = mDB.executeCount("select count(*) from pog_groups where score1>=0 limit 1");
		if (cnt==0) return; 
		
		String [] dfields = {"Prefix",  "Method", "conLen", "sdLen", "   Score1","SD  ", "Score2", "SD  "};
		int [] djust = 	    {1,      1,     0,        0,     0,     0,       0, 0};
		int nCol = djust.length;
		String [][] rows = new String[nMethod][nCol];
    
		lines.add(padTitle + "Statistics");
	    for (int r = 0; r < methods.size(); r++) {
    		Method m = methods.get(r);
    		
    		ResultSet rs = mDB.executeQuery(
    				"SELECT AVG(conLen), AVG(sdLen), " +
    				"AVG(score1), STDDEV(score1), AVG(score2), STDDEV(score2) FROM pog_groups " +
		   	        "WHERE PMid = " + m.pmid);
    		if (!rs.next()) {
    			Out.PrtError("Cannot get averages for overview");
    			return;
    		}
    		rows[r][0] = m.prefix;
    		rows[r][1] = m.method;
    		double conLen = rs.getDouble(1);
    		
    		if (conLen<=0) {
    			for (int i=2; i<nCol; i++) rows[r][i] = "-";
    		}
    		else {
    			for (int i=2, j=1; i<nCol; i++, j++) {
    				rows[r][i] = String.format("%.2f", rs.getDouble(j));
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
		lines.add("");
		if (nPairs==0) {
			lines.add("PAIRS: not computed yet");
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
  		if (text!=null && text.trim()!="") {
  			String [] pline = text.split("\n");
  			if (pline.length>1) { // CAS330 prevent extra blank lines
  				lines.add("");
  				for (String l:pline) lines.add(l);
  			}
  		}
  		
  		// kaksInfo created in Pairwise.java during compile
  		text = mDB.executeString("select kaksInfo from info");
	 	if (text!=null && text.trim()!="") {
	 		text = mDB.executeString("select kaksInfo from info");
	 		String [] kline = text.split("\n");
	 		if (kline.length>1) {
  				lines.add("");
  				for (String l:kline) lines.add(l);
	 		}
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
			
			lines.add("");
			lines.add(padTitle + "Differential Expression (p-value < " + DEcutoff + ") ");
			
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
						double d = Math.abs(rs.getDouble(i)); // CAS321 moved abs from if statement to here
						if (d < 2) { // CAS321 change from !=
							if (d < DEcutoff) deCnt[r][i]++;
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
		}
		catch (Exception e) {ErrorReport.die(e, "create dataset DE Table for summary");}
    }
    private void makeSeqExplevel() {
		ResultSet rs;
		try {
			int cnt = mDB.executeCount("select hasLib from info");// if no expression libraries
			if (cnt==0) return;
			
			lines.add("");
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
		
		String s1 = info.getMSA_Score1();
		String s2 = info.getMSA_Score2();
		
		if (aa=="" && nt=="" && s1=="" && s2=="") return;
		
		lines.add("----------------------------------------------------");
		lines.add("PROCESSING:");
		if (aa!="") lines.add("AA: " + aa);
		if (nt!="") lines.add("NT: " + nt);
		
		String score = "";  
		if (s1!=null && s1!="") score =  "Score1=" + info.getMSA_Score1() + "    ";
		if (s2!=null && s2!="") score += "Score2=" + info.getMSA_Score2();
		if (score != "") {
			lines.add("");
			lines.add("MSA: " + score);
		}
		lines.add("");
		
		// Methods
		if (nMethod==0) return;
		 
		lines.add("Cluster Sets "); // CAS340 move this table to processing
        	
        String[] fields = {"Prefix", "Method", "Parameters"};
        int [] just = {1,1,1};
        int nCol=fields.length;
		String [][] rows = new String[nMethod][nCol];

        String strQ = "SELECT PMtype, prefix, description from pog_method order by PMid"; // CAS327 changed from PMtype
        ResultSet rs = mDB.executeQuery(strQ);
   		int r=0;
   		while(rs.next()) {
   			int i=1;
   			String type = rs.getString(i++);
   			String prefix = rs.getString(i++);
   			String desc = rs.getString(i++);
   			rows[r][0] = prefix;
   			rows[r][1] = type;
            rows[r][2] = desc;
            r++;
        } 
   		rs.close();	   		
   		Out.makeTable(lines, nCol, r, fields, just, rows);
    }
	catch (Exception e) {ErrorReport.prtReport(e, "processing");}	
    }
   
    // HTML CAS404 make html pass BBEdit test
	public void writeHTML(String text) {
		String projDir = Globals.PROJECTDIR;
		String sumDir = Globals.summaryPath;
		String overFilePath = sumDir + "/" + dbName + ".html";
		try {
			if (!new File(projDir).exists()) return; // should exist
			
			String [] lines = text.split("\n");
			for (int i=0; i<lines.length; i++) {
				lines[i] = lines[i].replaceAll("&", "&amp;");
				lines[i] = lines[i].replaceAll(">", "&gt;");
				lines[i] = lines[i].replaceAll("<", "&lt;");
			}
			
			File h = new File(sumDir);
			if (!h.exists()) {
				System.out.println("Creating directory " + sumDir);
				h.mkdir();
			}
			FileOutputStream out = new FileOutputStream(overFilePath);
			PrintWriter fileObj = new PrintWriter(out); 
			
    		Out.prtSp(1, "Writing overview HTML file: " + overFilePath);
			
			fileObj.println("<!DOCTYPE html>");	// CAS404 html5
			fileObj.println("<html>");
			fileObj.println("<head><title>Overview mTCW_" + dbName + "</title></head>");
			fileObj.println("<body>");
			fileObj.println("<center>");
			fileObj.println("<h2>Overview for mTCW_" + dbName + " </h2>"); 
			fileObj.println("<table style=\"width: 800px; border: 2px solid #999999;\"><tr><td>");
			fileObj.println("<pre>");
			for (int i=0; i<lines.length; i++)  fileObj.println(lines[i]);
    		fileObj.println("</pre>");
    		
    		fileObj.println("</table></center></body>");
    		fileObj.println("</html>");
    		fileObj.close();
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error writing to " + overFilePath);}
	}
	private class Method {
		int pmid;
		String prefix;
		String method;
	}
	private String dbName="";
	private DBinfo info;
	private DBConn mDB;  

	private Vector <String> lines = new Vector <String> ();
	private DecimalFormat df = new DecimalFormat("#,###,###");
	private int nAsm=0, nMethod=0, nGrp=0, nPairs=0, nSeq=0;
	private Vector <String> assmStr = new  Vector <String> ();
	private Vector <Integer> assmID = new  Vector <Integer> ();
	private Vector <Method> methods = new Vector <Method> ();
	
}