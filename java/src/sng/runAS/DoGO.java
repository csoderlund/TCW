package sng.runAS;

/******************************************************
 * CAS318 OBSOLETE this has been replaced with DoOBO -- just saving case I need to reference....
 * Downloads the GO database and creates a local one with the 
 * UniProt records from the UniProt directory.
 * URL http://release.geneontology.org/daily/go_daily-termdb-tables.tar.gz
 * or, e.g. http://release.geneontology.org/full/2016-03-01/go_201603-termdb-tables.tar.gz
 * Add to schema:
 * 		3 TCW_ tables
 * 		term       add gonum int unsigned default 0
 *		           add level smallint default 0
 *		graph_path add child     int unsigned default 0
 *		           add ancestor  int unsigned default 0
 *		term2term  add child  int unsigned default 0
 *				   add parent int unsigned default 0
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;
import util.database.*;
import util.methods.ErrorReport;
import util.methods.Out;

public class DoGO {
	// test stuff -- so read file on disk instead of over network for testing
	private boolean test=ASMain.test;
	private String demoIn = "./projects/DBfasta/dats/";
		
	private final String subset = DoUP.subset; // file name of full subset
	private final String goURL = "http://release.geneontology.org/"; // CAS318 changed from archive to release
	public static String goDailyFile = "go_daily-termdb-tables.tar.gz";
	
	private final DecimalFormat df = new DecimalFormat("#,###,###");
	
	static private final String goTreeTable = Globalx.goTreeTable; // CAS318 were prefixed with PAVE_
	static private final String goMetaTable = Globalx.goMetaTable;
	static private final String goUpTable =   Globalx.goUpTable;
	
	public DoGO(ASFrame asf) { 
		frameObj = asf;
		hostObj = new HostsCfg(); // Gets mysql user/pass and checks that mySQL can be accessed
	}
	// hasGOtmp, goTemp, hasGOdb, goDB, 
	public void run(String upPath, String goPath, String godb, boolean hasGOfile) {
		Out.PrtDateMsg("\nStart GO processing " + godb);
		long startTime = Out.getTime();
		
		runObj = new RunCmd();
		upDir = upPath;
		goDBname = godb;
		goDir =   goPath;
		goFile = goDailyFile;
		goFullDir = goDir + "/" + goFile.replace(".tar.gz", "/");
		
		String fullURL = goURL;
		Out.PrtSpMsg(1, "UniProt directory: " + upPath);
		Out.PrtSpMsg(1, "GO temporary directory: " + goDir);
		if (!hasGOfile) Out.PrtSpMsg(1, "URL: " + fullURL);
		else Out.PrtSpMsg(1, "Use existing GO file");
		
	// Start processing
		if (!hasGOfile) {
			if (!checkGOdir()) return;
			if (!downloadGO(fullURL)) return;
		}
		else {
			if (!checkUnZipped()) return;
		}
		if (!checkDBdelete()) return;
		
		try {
			if (!loadGOschema()) return;
			
			goDB = connectToGODB(goDBname);
			
			if (!loadGOdbfromUniProt(upDir)) {
				goDB.close();
				return;
			}
			modifyGOdb(goDBname);
			deleteTablesGOdb();
			goDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Creating GO database");}
		
		Out.PrtSpMsgTimeMem(0, "Complete creating GO database " + goDBname, startTime);
	}
	private boolean downloadGO(String fullURL) {
		String goPath = goDir+ "/" + goFile;
		if (!new File(goPath).exists()) {
			if (test) {
				String cmd = "cp " + (demoIn+ "/" + goFile) + " " + goDir;
				if (!doCmd(cmd, null, "copy tar.gz", true)) return false;
			}
			else {
				if (!ASMain.ioURL(fullURL, goFile, goPath)) return false;
			}
		}
	    	String cmd = "tar xf " + goFile;
	    	if (!doCmd(cmd, goDir, "untar GO file", true)) return false;
		return true;
	}
	private boolean checkUnZipped() {
		try {
			String goPath = goDir + "/" + goFile.substring(0, goFile.indexOf(".tar"));
			Out.PrtSpMsg(1, "Check for directory " + goPath);
			if (!new File(goPath).exists()) {
			 	String cmd = "tar xf " + goFile;
			 	if (!doCmd(cmd, goDir, "untar GO file", true)) {
			 		Out.PrtSpMsg(0, "+++ If the tar file is corrupted - may be due to updates -- try again in 24 hours");
			 		return false;
			 	}
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Trying to unzipped existing GO tables file");}
		return false;
	}
	/********************************************
	 * Load the downloaded mysql to the database goDB
	 * goFile: go_daily-termdb-tables.tar.gz
	 * goDir: ./projects/DBfasta/GO_tmpMar2016 
	 * goFullDir: ./projects/DBfasta/GO_tmpMar2016/go_daily-termdb-tables
	 */
	private boolean loadGOschema() {
		String action = "Loading " + goFile + " to " + goDBname;
		Out.PrtSpMsg(1, action);
		try  {
			String cmd;
			String params = "-h " + hostObj.host() + " -u " + hostObj.user() + " -p" + hostObj.pass();
			cmd = "mysqladmin " + params + " create " + goDBname;
    		if (!doCmd(cmd, null, "create mySQL database", true)) return false;
    			
			File [] files = new File(goFullDir).listFiles();
			Vector <String> txt = new Vector <String> ();
			for (int i=0; i<files.length; i++) {
				String fname = files[i].getName();
				if (fname.endsWith(".txt")) txt.add(fname);
			}
			
			String shFile = "load.sh";
			BufferedWriter out = new BufferedWriter(new FileWriter(goFullDir+shFile, false));
			out.write("cat *.sql | mysql " + params + " " + goDBname + "\n");
			for (String f : txt) 
				out.write("mysqlimport " + params + " -L " + goDBname + " " + f + "\n");
			out.close();
			
			Out.PrtSpMsg(2, "The load takes a while - with no output to terminal - please be patient....");
			cmd = "sh " + shFile;
			// CAS303 the mysqlimport failed on Mac 10.15 MySQL v8; the local_infile fixed the problem
			if (!doCmd(cmd, goFullDir, "load GO into database", false)) {
				System.out.println("Try: ");
				System.out.println("mysql> set global local_infile = 1;");
				return false;
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e,  action);}
		return false;
	}
	private boolean doCmd(String cmd, String dir, String msg, boolean prtErr) {
		int ret = runObj.runP(cmd, dir, prtErr);
		if (ret!=0) {
			JOptionPane.showMessageDialog(frameObj, "Error - " + msg + ": " + ret + "\n"); 
			return false;
		}
		return true;
	}
	
	/********************************************
	 * Calls DoUPdat to load
	 */
	private boolean loadGOdbfromUniProt(String upDir) {
		String action = "Loading " + upDir + " to " + goDBname;
		Out.PrtSpMsg(1, action);
		try {
			goDB.executeUpdate("CREATE TABLE " + goMetaTable + " (filename text null) ENGINE=MyISAM;");
			
			String file = goFile;
			if (file.contains("daily")) { // CAS318 this is date GOdb created, but may not be date downloaded
				SimpleDateFormat sdf=new SimpleDateFormat("yyyyMM"); 
				String date = sdf.format(new Date());
				if (goDBname.contains("demo")) file = file.replace("daily", "demo"); // CAS318
				else file = file.replace("daily", date);
			}
			goDB.executeUpdate("insert into " + goMetaTable + " set filename='" + file + "'");
			goDB.executeUpdate("CREATE TABLE " + goUpTable + " (" +
                    "UPindex bigint unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,"+
                    "UPid varchar(25), acc varchar(15)," +
                    "go text, pfam text, kegg text, ec text, interpro text,"+
                    "unique (UPid)) ENGINE=MyISAM;");
			
			DoUPdat datObj = new DoUPdat(frameObj);
			File [] dirs = (new File(upDir)).listFiles();
			for (File d : dirs) {
				if (!d.isDirectory()) continue;
				String dname = d.getName() + "/";
				
				File [] xfiles = d.listFiles();
				for (File f : xfiles) {
					String fname = f.getName();
					if (fname.startsWith(".")) continue; 
					if (!fname.endsWith(".dat") && !fname.endsWith(".dat.gz")) continue;
					
					// The fullSubset have original and new reduced -- used reduced
					if (dname.contains(subset) && !fname.contains(subset)) continue;
						
					if (!datObj.dat2go(upDir, dname+f.getName(),  goDB)) return false; 
					break;
				}	
			}
			return datObj.prtTotals();
		}
		catch (Exception e) {ErrorReport.prtReport(e,  action);}
		return false;
	}
	/***********************************************************
	 * 1. gonum is the number part of the GO identifier. Add the following columns.
	 * 	GO table		id column: gonum column
	 *  --------    ------------------------
	 * 	term			id: gonum
	 * 	term2term 	term1_id: parent,   term2_id: child
	 * 	graph_path	term1_id: ancestor, term2_id: child
	 * 2. create levels in go_tree
	 */
	public  void modifyGOdb(String goname) {
		try
		{	
			Out.PrtSpMsg(1, "Modify TCW GO database " + goname);
			long time = Out.getTime();	
			ResultSet rs = null;
			
		/** term - Add go number column to the term table = integer without "GO:" prefix. **/
			Out.PrtSpMsg(2,"Add columns gonum, level to term");
			if (!goDB.tableColumnExists("term", "gonum")) {
				goDB.executeUpdate("alter table term add gonum int unsigned default 0");
				goDB.executeUpdate("alter table term add level smallint default 0");
			}
			goDB.executeUpdate("update term set " +
					" gonum=cast(substring(acc,4) as unsigned integer) " + 
					" where acc like 'GO:%'");
			goDB.executeUpdate("alter table term add index(gonum)");
			
		/** graph_path - Add gonums to the graph_path table so we can find ancestor GOs easily **/
			Out.PrtSpMsg(2,"Add columns gonum of term_id1 (ancestor) and term_id2 (child) to graph_path");
			if (!goDB.tableColumnExists("graph_path", "child")) {
				goDB.executeUpdate("alter table graph_path add ancestor int unsigned default 0"); // CAS318 put first 
				goDB.executeUpdate("alter table graph_path add child int unsigned default 0");
				goDB.executeUpdate("alter table graph_path add index(child)");
			}
			else {
				goDB.executeUpdate("update graph_path set child=0,ancestor=0");
			}
			// get 'id' for is_a and part_of from the term table
			int is_a_Num = 0;
			int part_of_Num = 0;
			rs = goDB.executeQuery("select id,acc from term where acc in ('is_a','part_of')");
			while (rs.next())
			{
				int id = rs.getInt(1);
				String acc = rs.getString(2);
				if (acc.equals("is_a")) is_a_Num = id;
				else part_of_Num = id;
			}
			rs.close();
			
			// Add the gonums IF 
			// A) both entries are actual GO terms 
			// B) both terms have the same major categories (i.e., we are not going to track the cross-listings between the 3 trees)
			// C) the relationship is "is_a", or "part_of".
			// Then gonum searches automatically ignore the other relationships stored in the table.  
			
			Out.PrtSpMsg(3,"Bulk update of graph_path...");
			goDB.executeUpdate("update graph_path, term as tchild, term as tanc " +
			" set graph_path.child=tchild.gonum, graph_path.ancestor=tanc.gonum " +
			" where tchild.id=graph_path.term2_id " +
			" and tchild.gonum != 0 " + 
			" and tanc.id=graph_path.term1_id " +
			" and tanc.gonum != 0 " +
			" and tchild.term_type=tanc.term_type " +  
			" and relationship_type_id in (" + is_a_Num + "," + part_of_Num + ")");
			
		/** term2term - add gonum for term_id1 (term1_id=parent, term2_id=child)**/
			Out.PrtSpMsg(2,"Add columns gonum of term_id1 (parent) and term_id2 (child) to term2term");
			// Add gonums to the term2term table so we can do the depth-first search
			if (!goDB.tableColumnExists("term2term", "child")) {
				goDB.executeUpdate("alter table term2term add parent int unsigned default 0"); // CAS318 put first 
				goDB.executeUpdate("alter table term2term add child int unsigned default 0");
				goDB.executeUpdate("alter table term2term add index(child)");
				goDB.executeUpdate("alter table term2term add index(parent)");
			}
			// this has to run twice, once for child and once for parent
			Out.PrtSpMsg(3,"Bulk update of term2term for id1...");
			goDB.executeUpdate("update term2term, term " +
					" set   term2term.child = term.gonum " +
					" where term.id = term2term.term2_id " +
					" and term.gonum != 0 " +
					" and relationship_type_id in (" + is_a_Num + "," + part_of_Num + ")");
			Out.PrtSpMsg(3,"Bulk update of term2term for id2...");
			goDB.executeUpdate("update term2term, term " +
					" set term2term.parent = term.gonum " +
					" where term.id = term2term.term1_id " +
					" and term.gonum != 0 " + 
					" and relationship_type_id in (" + is_a_Num + "," + part_of_Num + ")");
		
		/** TCW_gotree **/ 
			Out.PrtSpMsg(2,"Create a table of levels");
			if (!goDB.tableExists(goTreeTable))  
	    			goDB.executeUpdate("create table " + goTreeTable + " (" +
	    				" idx 	int unsigned auto_increment primary key, " +
	    				" gonum int unsigned, " +
	    				" level smallint unsigned," + 
	    				" index(gonum) ) ENGINE=MyISAM;");			
			TreeMap<Integer,TreeSet<Integer>> parent2child = new TreeMap<Integer,TreeSet<Integer>>();
			String sql = "select child,parent from term2term " +
					" where child != 0 and parent != 0 " +
					" and relationship_type_id in (" + is_a_Num + "," + part_of_Num + ")";
			rs = goDB.executeQuery(sql);
			while (rs.next())
			{
				int child = rs.getInt("child");
				int parent = rs.getInt("parent");
				if (!parent2child.containsKey(parent))
				{
					parent2child.put(parent,new TreeSet<Integer>());
				}
				parent2child.get(parent).add(child);
			}
			rs.close();
			Out.PrtSpMsg(3, df.format(parent2child.size()) + " Parent-child");
			
			// Now get the top-level nodes and do a depth-first traversal, adding as we go.
			// Note, some gonums appear in multiple places in the "tree", up to 300+ times.
			// E.g., 433 has 55 immediate parents and 384 locations in the tree.
			// The biggest duplications are in the biological_process tree.
			// (Note, since we ignored cross-relationships above, each go is only in one of the three major trees, e.g. biological process). 
			
			String[] types = Globalx.GO_TERM_LIST; 
			int [] topNum = new int [types.length];
			int ii=0;
			for (String type : types) {
				rs = goDB.executeQuery("select gonum from term where name='" + type + "'");
				rs.first();
				topNum[ii] = rs.getInt(1);
				ii++;
			}
			rs.close();
			
			int cntSave=0;
			
			PreparedStatement ps = goDB.prepareStatement(
					"insert into " + goTreeTable + " (gonum, level) values(?,?)");
			
			for (int i=0; i<types.length; i++) // loop 3 times...
			{
				goDB.openTransaction(); 
				
				int nOrdered = 0, level = 0, maxLevel=0;
				
				Stack<Integer> stack = new Stack<Integer>();
				Stack<Integer> curPath = new Stack<Integer>();
				stack.push(topNum[i]);
				while (!stack.isEmpty())
				{
					int num = stack.pop();				
					int curSubtreeRoot = (curPath.empty() ? 0 : curPath.lastElement());
					if (curSubtreeRoot == num)
					{
						// We've now traversed this whole subtree. 
						curPath.pop();
						level--;
					}
					else
					{
						// We're still working through the children of the
						// most recent branching node (or we're still at the root node)
						ps.setInt(1, num);
						ps.setInt(2, level);
						ps.addBatch();
						nOrdered++; cntSave++;
						if (cntSave == 5000) { 
							cntSave=0;
							ps.executeBatch();
							Out.r(types[i] + " " + nOrdered);
						}

						if (parent2child.containsKey(num))
						{
							curPath.push(num);
							stack.push(num);
							stack.addAll(parent2child.get(num));
							level++;
							maxLevel = Math.max(maxLevel, level);
						}
					}
				}
				if (cntSave>0) ps.executeBatch();
				goDB.closeTransaction(); 
				
				Out.PrtSpMsg(3, df.format(nOrdered) + " tree entries for " + types[i]);
			}
			ps.close(); 
			
			// Get the GO levels from the tree and add them to the term table for query access. 
			// For GOs at multiple levels (e.g., cell_part) we take the highest level (e.g. prefer 3 over 2). 
			// This fixes the "cell_part" type problem but is otherwise basically arbitrary.
			Out.PrtSpMsg(2,"Add GO level numbers to term");
			goDB.executeUpdate("update term set level = (select max(level)+1 from " +
								goTreeTable + " where gonum=term.gonum)" );
			Out.PrtSpMsgTimeMem(1, "Complete TCW GO database modifications", time);
		}
		catch(Exception e){ErrorReport.reportError(e, "Updating go database");}
	}
	/***************************************************/
	private boolean checkGOdir() {
		try {
			File d = new File(goDir);
			if (!d.exists() ) {
				Out.PrtSpMsg(1, "Create " + goDir);
				if (d.mkdir()) return true;
		    		
				JOptionPane.showMessageDialog(frameObj,"Could not create directory: " + goDir + "\n"); 
		    		Out.PrtError("Could not create directory: " + goDir);
		    		return false;
			}
			return true;
		}
    		catch(Exception e){ErrorReport.reportError(e, "Creating GO directory");}
		return false;
	}
	
	private boolean checkDBdelete() {
		try {
			int rc = checkGODB(goDBname);
			if (rc==0) return true;
			
			Out.PrtSpMsg(1, "Delete mySQL database " + goDBname);
			DBConn.deleteMysqlDB(hostObj.host(), goDBname, hostObj.user(), hostObj.pass());
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "GO check params");}
		return true;
	}
	private void deleteTablesGOdb() { // CAS318 no functional reason for this, but easier to look at GOdb
		try {
			Out.PrtSpMsg(1, "Final step - remove empty tables");
			int n=0;
			Vector <String> tabs = new Vector <String> ();
			ResultSet rs = goDB.executeQuery("SHOW TABLES");
			while (rs.next()) tabs.add(rs.getString(1));
			rs.close();
			
			for (String name : tabs) {
			    int cnt = goDB.executeCount("select count(*) from " + name);
			    if (cnt==0) {
			        goDB.tableDrop(name);
			        n++;
			    }
			}
			Out.PrtSpMsg(2,"Remove " + n + " empty tables");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Remove empty tables from GOdb");}
	}
	private DBConn connectToGODB(String goname)
	{		
		DBConn goDB = null;
		try {
			goDB = new DBConn(hostObj.host(), goname, hostObj.user(), hostObj.pass());
		}
		catch(Exception e) {Out.PrtWarn("Could not connect to GO database " + goDBname);}
		return goDB;
	}
	
	/**********************************
	 * Return:
	 * 0 - does not exist
	 * 1 - exists but not TCW modified
	 * 2 - exists and complete
	 */
	public int checkGODB(String goname)
	{	int x = 0;
		try {
			boolean rc = DBConn.checkMysqlDB("", hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!rc) return 0; 
			
			DBConn goDB = new DBConn(hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!goDB.tableExists(goUpTable)) x=1; // gets added in modfiyGOdb
			else x=2; 
			
			goDB.close();
			return x;
		}
		catch(Exception e) {Out.PrtWarn("Could not check GO database " + goDBname);}
		return 0;
	}
	
	/**************************************************/
	private String upDir;
	private String goDBname;  // e.g. go_Apr2016
	private String goDir;     // e.g. ./projects/DBfasta/GO_tmpApr2016 -- tar.gz downloaded here
	private String goFile;    // e.g. go_daily-termdb-tables.tar.gz
	private String goFullDir; // e.g. ./projects/DBfasta/GO_tmpApr2016/go_daily-termdb-tables
	
	private ASFrame frameObj=null;
	private RunCmd runObj=null;
	private DBConn goDB=null;
	private HostsCfg hostObj=null;
}
