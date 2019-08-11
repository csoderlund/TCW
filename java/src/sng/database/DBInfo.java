package sng.database;

/*************************************************
 * Contains information of all TCW databases on the host specified in HOSTs.cfg
 */
import java.sql.ResultSet;
import java.util.Vector;

import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.ui.UIHelpers;

public class DBInfo {
	/*** Static *****/
	static public Vector <DBInfo> getDBlist(HostsCfg hostsObj, String dbstr) {
		try 
		{	
			Vector <String> list = new Vector <String> ();
			DBConn dbc = hostsObj.getDBConn("mysql");
			
			ResultSet rs = dbc.executeQuery("show databases");
			String dbName;
			while (rs.next()) 
			{
				dbName = rs.getString(1);
				if ((dbName.startsWith(Globals.STCW))) list.add(dbName);
			}
			rs.close();
			dbc.close();
			
			if (list.size() == 0)
			{
				System.err.println("No TCW databases were found on host: " + hostsObj.host());
				System.err.println("Does your database user (" + hostsObj.user() + ") have sufficient permissions to 'show databases'?");
				System.exit(0);
			}
			Vector <DBInfo> dbList = new Vector <DBInfo> ();
			for (String n : list) {
				DBConn mdb = hostsObj.getDBConn(n);
				DBInfo db = setDBParams(mdb, n, dbstr);
				if (db!=null) dbList.add(db);
				mdb.close();
			}
			return dbList;
		} 
		catch (Exception err) 
		{
			ErrorReport.reportError(err, "Error accessing database on " + hostsObj.host());
		}
		return null;
	}
	static public DBInfo setDBParams(DBConn mdb, String dbName, String dbstr) throws Exception
	{
        ResultSet rs = null;
        try {
        		if (!mdb.tableExists("assembly")) {
        			prtErr(dbName, dbstr);
        			return null;
        		}
            String strQ = "SELECT username, projectpath, assemblydate, annotationdate, assemblyid " +
            		"FROM assembly";
 
            rs = mdb.executeQuery ( strQ );
            if ( !rs.next() ) {
            		prtErr(dbName, dbstr);
            		rs.close();
	            	return null;
            }
            DBInfo db = new DBInfo();
            db.dbName = dbName;
            
            db.id = rs.getString("assemblyid");
            db.username = rs.getString("username");
            db.projectpath = rs.getString("projectpath");
            db.assemblydate = rs.getString("assemblydate");
            db.annotationdate = rs.getString("annotationdate"); 
            
            rs = mdb.executeQuery("SELECT schemver FROM schemver");
			if ( rs.next() ) {
				db.dbVerStr = rs.getString("schemver.schemver");
			}
			rs.close();
			return db;
        }
        catch ( Exception err ) {
        		if (dbstr=="" || dbName.equals(dbstr))
        			ErrorReport.reportError(err, "Query failed for database " + dbName);
        }
        return null;
	}
	static private void prtErr(String dbName, String dbstr) {
		if (dbstr!="" && dbName.equals(dbstr))
			System.err.println("Error: incomplete database '" + dbName + "'");
		else if (dbstr=="")
			System.err.println("Error: incomplete database  '" + dbName + "' -- ignoring");
	}
	/***** DBInfo method *********************************/
	public boolean checkDBver(HostsCfg hostsObj) {
		if (dbVerStr.equals(Version.strDBver)) return true;
		if (UIHelpers.isApplet()) return false;
		
		try {
			DBConn dbc = new DBConn(hostsObj.host(), dbName,hostsObj.user(), hostsObj.pass());
			Schema s = new Schema(dbc);
			if (!s.current()) s.update();
			dbVerStr = Version.strDBver;
			dbc.close();
			return true;
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "Unable to update schema");
		}
		
		return false;
	}
	/*** Class *****/
	public DBInfo() {}
	public String toString() {
		String str = id + "    [" + dbName + " " + assemblydate + " " + username + "]";
		String v = dbVerStr;
		if (!v.equals(Version.strDBver)) str += " db" + v;
		return str;
	}
	public String getDescription() {
		return Version.sTCWtitle + "   " + dbName + "::" + id;
	}
	public String getID() {return id;}
	public String getdbName() {return dbName;}
	String id;
	String dbName;
	String username;
	String projectpath;
	String assemblydate;
	String annotationdate;
	String dbVerStr;
}
