package util.database;

/***********************************************************
 * Most database operations are performed via this method for TCW
 */
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.regex.Pattern;
import java.util.Vector;

import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;

// WARNING: Does not work for nested queries (because it uses one Statement for all queries). 
// To do a query within the read loop of a previous
// query, use a second DBConn object.

public class DBConn 
{
	static Pattern mpQUOT = null;
	final int maxTries = 10;
	public String mBadStrMsg = "Quotes not permitted";
	public String mUser;
	public String mPass;
	public String mDBName;
	public String mHost;
    public String jdbcStr = "";
    static private String chrSQL = "characterEncoding=utf8"; // utf8mb4, CAS303
    static public String driver = "com.mysql.jdbc.Driver";

	Connection mConn = null;
	Statement mStmt = null;

    public DBConn(String host, String dbname,  String user, String pass) throws Exception
    {
            mHost = host;
            mDBName = dbname;
            mUser = user;
            mPass = pass;
            jdbcStr = createDBstr(host, dbname);  
            jdbcStr += "&useServerPrepStmts=false&rewriteBatchedStatements=true"; 
            renew();
    }
    // For connection w/o specific database, for doing e.g. "show databases".
    // User needs sufficient permissions to do this. 
    public DBConn(String host, String user, String pass) throws Exception
    {
            mHost = host;
            mUser = user;
            mPass = pass;
            jdbcStr = createDBstr(host, null);
            renew();
    }	
	public Connection getDBconn() { return mConn;}
	
	public void renew() throws Exception
	{
		Class.forName(driver);
		mStmt = null;
		if (mConn != null && !mConn.isClosed()) mConn.close();
		for (int i = 0; i <= maxTries; i++)
		{
			try {
				mConn = DriverManager.getConnection(jdbcStr, mUser,mPass);
				break;
			} 
			catch (SQLException e)
			{
				if (i == maxTries) {
					ErrorReport.die(e, "Unable to connect to " 	+ jdbcStr + "\nJava Exception: " + e.getMessage());
				}
			}
			Thread.sleep(100);
		}
	}
	/******************************************
	 * Main query code
	 */
	private Statement getStatement() throws Exception
	{
		if (mStmt == null)
		{
			mStmt = mConn.createStatement();
			//mStmt.setQueryTimeout(10000); this causes a problem with applet if one db closed and another immediately opened
		}
		return mStmt;
	}
	
	public ResultSet executeQuery(String sql) throws Exception
	{
		if (mConn == null || mConn.isClosed()) renew();
		Statement stmt = getStatement();
		ResultSet rs = null;
		try
		{
			rs = stmt.executeQuery(sql);
		}
		catch (Exception e)
		{
			try {
				mStmt.close();
				mStmt = null;
				renew();
				stmt = getStatement();
				rs = stmt.executeQuery(sql);
			}
			catch (SQLException ee) {
				System.err.println("Query failed: " + sql);
				System.err.println(ee.getMessage());
			}
		}
		return rs;
	}

	public int executeUpdate(String sql) throws Exception
	{
		if (mConn == null || mConn.isClosed()) renew();
		Statement stmt = getStatement();
		int ret = 0;
		try
		{
			ret = stmt.executeUpdate(sql);
		}
		catch (Exception e)
		{
			System.err.println("Query failed, retrying:" + sql);
			mStmt.close();
			mStmt = null;
			renew();
			stmt = getStatement();
			ret = stmt.executeUpdate(sql);
		}
		return ret;
	}
	public int executeCount(String sql) throws Exception
    {
        try {
            ResultSet rs = executeQuery(sql);
            int n = (rs.next()) ? rs.getInt(1) : -1; 
            rs.close();
            return n;
        }
        catch (Exception e) {
    			ErrorReport.prtReport(e, "Getting counts");
            return 0;
        }
	}
	public int executeInteger(String sql) throws Exception
    {
        try {
            ResultSet rs = executeQuery(sql);
            int n = (rs.next()) ? rs.getInt(1) : -1;
            rs.close();
            return n;
        }
        catch (Exception e) {
    			ErrorReport.prtReport(e, "Getting counts");
            return -1;
        }
	}
	public long executeLong(String sql) throws Exception
    {
        try {
            ResultSet rs = executeQuery(sql);
            rs.next();
            long n = rs.getLong(1);
            rs.close();
            return n;
        }
        catch (Exception e) {
    			ErrorReport.prtReport(e, "Getting long");
            return 0;
        }
	}
	public float executeFloat(String sql) throws Exception
    {
        try {
            ResultSet rs = executeQuery(sql);
            rs.next();
            float n = rs.getFloat(1);
            rs.close();
            return n;
        }
        catch (Exception e) {
    			ErrorReport.prtReport(e, "Getting float");
            return 0;
        }
	}
	public String executeString(String sql) throws Exception
    {
        try {
            ResultSet rs = executeQuery(sql);
            if (!rs.next()) return null;
            String n = rs.getString(1);
            rs.close();
            return n;
        }
        catch (Exception e) {
    			ErrorReport.prtReport(e, "Getting string");
            return null;
        }
	}
	public boolean executeBoolean(String sql) throws Exception
    {
        try {
           int cnt = executeCount(sql); // CAS310 was (sql + "limit 1")
           if (cnt==0) return false;
           else return true;
        }
        catch (Exception e) {
    		ErrorReport.prtReport(e, "Getting boolean");
            return false;
        }
	}
	public void close()
	{
		try {
			if (mStmt!=null) mStmt.close(); 
			mStmt = null;
			
			if (mConn!=null) mConn.close();
			mConn = null;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Closing connection");}
	}
	/************ 
	 * table operations 
	 *******************************************************/
	public boolean tablesExist() throws Exception
	{
		boolean ret = false;
		ResultSet rs = executeQuery("show tables");
		if (rs!=null && rs.first()) ret = true;
		if (rs!=null) rs.close();
		return ret;
	}
	public boolean tableExist(String name) throws Exception
	{
		return tableExists(name);
	}
	public boolean tableExists(String name) throws Exception
	{
		ResultSet rs = executeQuery("show tables");
		while (rs.next()) {
			if (rs.getString(1).equals(name)) {
				rs.close();
				return true;
			}
		}
		if (rs!=null) rs.close();
		return false;
	}
	public boolean tableColumnExists(String table, String column) throws Exception
	{
		ResultSet rs = executeQuery("show columns from " + table);
		while (rs.next()) {
			if (rs.getString(1).equals(column)) {
				rs.close();
				return true;
			}
		}
		if (rs!=null) rs.close();
		return false;
	}
	public void tableDrop(String table) {
		try {
			if (tableExists(table))
				executeUpdate ("DROP TABLE " + table);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot drop table " + table);}
	}
	public void tableRename(String otab, String ntab) { // CAS318
		try {
			if (tableExists(otab))
				executeUpdate ("RENAME TABLE " + otab + " to " + ntab);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot rename table " + otab);}
	}
	public void tableDelete(String table) {
	   	try { // finding 'status' fails when 'show tables' succeeds (incorrectly)
	   	   ResultSet rs = executeQuery("show table status like '" + table + "'");
	   	   if (!rs.next()) {
	   		   rs.close();
	   		   return;
	   	   }
	   	   executeUpdate ("TRUNCATE TABLE " + table); // CAS342 this resets auto-increment
	   	   /**
	       executeUpdate ("DELETE FROM " + table);
	       
	       rs = executeQuery("SELECT COUNT(*) FROM " + table);
	       rs.next();
	       int cnt = rs.getInt(1);
	       rs.close();
	    
	       executeUpdate ("ALTER TABLE " + table + " AUTO_INCREMENT = " + cnt+1); 
	       **/  
	    }
	    catch(Exception e) {
	    	System.err.println("*** Database is probably corrupted");
	     	System.err.println("*** MySQL finds table but then cannot delete from it.");
		    ErrorReport.die(e,"Fatal error deleting table " + table);
		    System.exit(-1);
	     }
   }
	public boolean tableCheckAddColumn(String table, String col, String type, String aft) throws Exception
	{
		String cmd = "alter table " + table + " add " + col + " " + type ;
		try {
			if (!tableColumnExists(table,col))
			{
				if (aft!=null && !aft.trim().equals(""))
				{
					cmd += " after " + aft;
				}
				executeUpdate(cmd);
				return true;
			}
			return false;
		}
		catch(Exception e){ErrorReport.prtReport(e, "MySQL error: " + cmd);}
		return false;
	}
	public void tableCheckDropColumn(String table, String col) throws Exception
	{
		if (tableColumnExists(table,col))
		{
			String cmd = "alter table " + table + " drop " + col;
			executeUpdate(cmd);
		}
	}
	// CAS303 added ` around oldCol so the new reserved keyword 'rank' can be rename.
	public void tableCheckRenameColumn(String table, String oldCol, String newCol, String type) throws Exception
	{
		if (tableColumnExists(table,oldCol))
		{
			String cmd = "alter table " + table + " change column `" + oldCol + "` " + newCol + " " +  type ;
			executeUpdate(cmd);
		}
	}
	
	public void tableCheckModifyColumn(String table, String col, String type) throws Exception
	{
		if (tableColumnExists(table,col)){
			String curDesc = tableGetColDesc(table,col);
			if (!curDesc.equalsIgnoreCase(type)){
				String cmd = "alter table " + table + " modify " + col + " " + type ;
				executeUpdate(cmd);
			}
		}
		else {Out.PrtWarn("Tried to change column " + table + "." + col + ", which does not exist");}
	}
	
	// Change column to new definition, if it doesn't already match.
	// Note, the definition must be given exactly as seen in the "show table" listing,
	// e.g. mediumint(8) and not just mediumint.
	// If defs don't match, it will re-change the column, wasting time. 
	public void tableCheckChangeColumn(String table, String col, String type) throws Exception
	{
		if (tableColumnExists(table,col)){
			String curDesc = tableGetColDesc(table,col);
			if (!curDesc.equalsIgnoreCase(type)){
				String cmd = "alter table " + table + " change " + col + " " + col + " " + type ;
				executeUpdate(cmd);
			}
		}
		else{Out.PrtWarn("Tried to change column " + table + "." + col + ", which does not exist");}
	}
	public String tableGetColDesc(String tbl, String col)
	{
		String ret = "";
		try {
			ResultSet rs = executeQuery("describe " + tbl);
			while (rs.next())
			{
				String fld = rs.getString("Field");
				String desc = rs.getString("Type");
				if (fld.equalsIgnoreCase(col)){
					ret = desc;
					break;
				}
			}
		}
		catch(Exception e){ErrorReport.prtReport(e, "checking column description for " + tbl + "." + col);
		}
		return ret;
	}
	
	/************************************
	 * Used by assembly and some cmp
	 */
	public Statement createStatement() throws Exception
	{
		return mConn.createStatement();
	}

	// NOT THREAD SAFE unless each thread is using its own DB connection.
	// Each thread should call Assem.getDBConnection() to get one. 
	public Integer lastID() throws Exception
	{
		String st = "select last_insert_id() as id";
		ResultSet rs = executeQuery(st);
		int i = 0;
		if (rs.next())
		{
			i = rs.getInt("id");
		}
		rs.close();
		return i;
	}	
	public void openTransaction() throws Exception
	{
		executeQuery("BEGIN");
	}
	public void closeTransaction() throws Exception
	{
		executeQuery("COMMIT");
	}	
	public void rollbackTransaction() throws Exception
	{
		executeQuery("ROLLBACK");
	}	

	public  PreparedStatement prepareStatement(String st) throws SQLException
	{
		return mConn.prepareStatement(st);
	}
	public void foreignKeysOff() throws Exception
	{
		executeQuery("set foreign_key_checks=0");	
	}
	public void foreignKeysOn() throws Exception
	{
		executeQuery("set foreign_key_checks=1");	
	}	
	public void writeLockTable(String table) throws Exception
	{
		executeQuery("lock tables " + table + " write");	
	}
	public void lockAllTables() throws Exception
	{
		// get the tables to a vector first since we can't
		// do nested queries
		Vector<String> tbls = new Vector<String>();
		ResultSet rs = executeQuery("show tables");	
		while (rs.next()) {
			String table = rs.getString(1);
			tbls.add(table);
		}
		
		for (String table : tbls) {
			executeQuery("lock tables " + table + " write");
		}
	}
	public void unlockTables() throws Exception
	{
		executeQuery("unlock tables");	
	}
	public void checkInnodbBufPool() throws Exception
	{
		ResultSet rs = executeQuery("show variables like 'innodb_buffer_pool_size'");
		if (rs.first())
		{
			long size = rs.getLong(2);
			if (size < 100000000L)
			{
				System.err.println("\nYour database variable 'innodb_buffer_pool_size' is set to a small number (" + size + ")");
				System.err.println("It is strongly recommended to raise this and related settings (see documentation).");
				if (!FileHelpers.yesNo("The assembly may be slow with these settings. Continue"))
				{
					System.exit(0);	
				}
			}
		}
		else
		{
			System.err.println("Attempted to check database variable 'innodb_buffer_pool_size' but was not successful.\nEnsure that this setting, and others, are large enough for your assembly (see documentation)."); 	
		}
	}
	public boolean hasInnodb() throws Exception
	{
		boolean has = false;
		ResultSet rs = executeQuery("show engines");
		while (rs.next())
		{
			String engine = rs.getString(1);
			String status = rs.getString(2);
			if (engine.equalsIgnoreCase("innodb") && (status.equalsIgnoreCase("yes")
					|| status.equalsIgnoreCase("default")) )
			{
				has = true;
				break;
			}
		}
		return has;
	}
    public ResultSet executeQueryNoCatch(String sql) throws Exception
    {
        if (mConn == null || mConn.isClosed()) renew();
        Statement stmt = getStatement();
        ResultSet rs = null;
        rs = stmt.executeQuery(sql);
        return rs;
    }
    public DBConn createDBAndNewConnection(String dbName) throws Exception
    {
        try {
            executeUpdate("create database " + dbName);
            return  new DBConn(mHost, dbName, mUser, mPass);

        }
        catch (Exception e)
        {
            System.err.println( "Cannot create MySQL databse " + dbName);
            System.exit(0);
        }
        return null;
    }
    public String getDbiStr()
    {
    		return "dbi:mysql:" + mDBName + ":" + mHost;
    }
    public String getDBname() { // CAS340
    	return mDBName;
    }
    /************************************************************** 
     * Static methods 
     * ******************************************/
    public static void deleteMysqlDB(String host, String db, String user, String pass) throws Exception
    {
        Class.forName(driver);
        String dbstr = createDBstr(host, null);
        Connection con = null;

        try {
            con = DriverManager.getConnection(dbstr, user, pass);
            Statement st = con.createStatement();
            st.execute("drop database " + db);
            con.close();
        }
        catch (Exception e) {ErrorReport.prtReport(e, "Cannot delete mySQL database " + db); }
    }
    public static boolean oldDBisEmpty(String host, String db, String user, String pass) 
	{
		String dbstr = createDBstr(host, db);
		Connection con = null; 

		try
		{
			Class.forName(driver);
			con = DriverManager.getConnection(dbstr, user, pass);
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("show tables like 'clone'");
			if (!rs.first()) 
			{
				con.close();
				return true;
			}
			rs = s.executeQuery("select count(*) from clone");
			rs.first();
			int nClones = rs.getInt(1);
			rs.close();
			con.close();
			return nClones == 0;			
		} 
		catch (Exception e) {} // already checked
		
		return false;
	}	
	public static boolean deleteDB(String host, String db, String user, String pass) 
	{
		String dbstr = createDBstr(host, "mysql"); // "jdbc:mysql://" + host + "/mysql";
		Connection con = null; 

		try {
			Class.forName(driver);
			con = DriverManager.getConnection(dbstr, user, pass);
			Statement s = con.createStatement();
			s.executeUpdate("drop database " + db);
			//System.err.println("Deleted database " + db); CAS304 let calling program 
			return true;
		} 
		catch (Exception e)
		{
			ErrorReport.die(e, "Database " + db + " could not be deleted");	
		}
		return false;
	}	
	public static void createMysqlDB(String host, String db, String user, String pass) throws Exception
	{
		Class.forName(driver);

		String dbstr = createDBstr(host, null);
		Connection con = null; 

		try
		{
			con = DriverManager.getConnection(dbstr, user, pass);
			Statement st = con.createStatement();
			st.execute("create database " + db);
			con.close();
		} 
		catch (Exception e)
		{
			if (user.equals("") || pass.equals("")) 
				System.err.println("Warning: User and password may not be defined in HOSTS.cfg");
			ErrorReport.die(e, "Database " + db + " could not be created on host " + host);	
		}
	}
	public static boolean connectionValid(String host, String db, String user, String pass) {
        try {
            Class.forName(driver);
            String dbstr = createDBstr(host, db);
            
            Connection conn = DriverManager.getConnection(dbstr, user, pass);

            SQLWarning warn = conn.getWarnings();
            while (warn != null) {
                    System.out.println("SQLState: " + warn.getSQLState());
                    System.out.println("Message:  " + warn.getMessage());
                    System.out.println("Error:   " + warn.getErrorCode());
                    System.out.println("");
                    warn = warn.getNextWarning();
            }
            return true;
        }
        catch(Exception e) {
            System.err.println("Error: Unable to load database");
            return false;
        }
    }
	public static boolean checkMysqlDB(String msg, String host, String db, String user, String pass) 
	{
		String dbstr = createDBstr(host, db);
		Connection con = null; 

		try {
			Class.forName(driver);
			con = DriverManager.getConnection(dbstr, user, pass);
			con.close();
			return true;
		} 
		catch (Exception e){}
		return false;
	}	
	public static boolean checkMysqlServer(String host, String user, String pass)
    {
        boolean ret = false;
        try {
            Class.forName(driver);
        }
        catch(Exception e) {
            ErrorReport.die("Unable to find MySQL driver: " + driver);
        }
        
        String dbstr = createDBstr(host, null); // CAS303;
        try {
            Connection con = DriverManager.getConnection(dbstr, user, pass);
            con.close();
            ret = true;
        }
        catch (Exception e) { } // CAS303 prints in calling routine	
            
        return ret;
    }
    public static boolean checkVariables(String host, String user, String pass, boolean bAll)
    {
        boolean ret = false;
        
        try {
            Class.forName(driver);
        }
        catch(Exception e) {
        	ErrorReport.die("Unable to find MySQL driver: " + driver);
            return false;
        }
      
        String dbstr = createDBstr(host, null);
       
        try {
        	Connection con;
        	Statement st;
        	
        	try {
        		con = DriverManager.getConnection(dbstr, user, pass);
        		st = con.createStatement();
        	}
        	catch (SQLException e)  {	
            	ErrorReport.prtReport(e, "Check MySQL database");
                System.err.println("Cannot connect to MySQL on host=" + host + " user=" + user);
                System.err.println(dbstr);
                System.err.println(e.getMessage()); 
                return false;
            }
        	// CAS316 made better output based on recent tests
        	
        	ResultSet rs = st.executeQuery("SHOW VARIABLES LIKE 'version'");
        	if (rs.next()) {
				String s = rs.getString(2);
				Out.PrtSpMsg(0,"MySQL Version=" + s);
        	}
			
            rs = st.executeQuery("show variables like 'max_allowed_packet'");
			if (rs.next()) {
				long packet = rs.getLong(2);
				
				Out.PrtSpMsg(1, "max_allowed_packet=" + packet);
				if (packet<=1048576) 
					Out.PrtSpMsg(1, "Suggest: SET GLOBAL max_allowed_packet=1073741824;");
			}
			
			Out.PrtSpMsg(1, "For runSingle assembly on MariaDB:");
			rs = st.executeQuery("show variables like 'innodb_buffer_pool_size'");
			if (rs.next()) {
				long packet = rs.getLong(2);
				
				Out.PrtSpMsg(2, "innodb_buffer_pool_size=" + packet);
				if (packet< 134217728) 
					Out.PrtSpMsg(2,"Suggest: SET GLOBAL innodb_buffer_pool_size=1073741824;");
			}
			
			rs = st.executeQuery("show variables like 'innodb_flush_log_at_trx_commit'");
			if (rs.next()) {
				int b = rs.getInt(2);
				Out.PrtSpMsg(2,"innodb_flush_log_at_trx_commit=" + b);
				if (b==1) 
					Out.PrtSpMsg(2,"Suggest: SET GLOBAL innodb_flush_log_at_trx_commit=0");
			}
            con.close();
            ret = true;
        }
        catch (SQLException e) {	
        	ErrorReport.prtReport(e, "Check MySQL variables");
            System.err.println(e.getMessage()); 
        }
       
        return ret;
    }
    // CAS303 - add chrSQL
    static public String createDBstr(String host, String db) {
    	if (db==null) {
    		String h = host.replace(";", ""); // in case a ';' is at end of localhost in HOSTs.cfg
    		return "jdbc:mysql://" + h + "?" + chrSQL; 
    	}
    	else {
    		String h = host.replace(";", "");
    		return "jdbc:mysql://" + h + "/" + db + "?" + chrSQL;
    	}
    }
}
