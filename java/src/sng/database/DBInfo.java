package sng.database;

/*************************************************
 * Contains information of all TCW databases on the host specified in HOSTs.cfg
 */
import java.sql.ResultSet;
import java.util.Vector;

import util.database.DBConn;
import util.database.HostsCfg;
import util.database.Globalx;
import util.methods.ErrorReport;

public class DBInfo {
	/*** Static *****/
	static public Vector <DBInfo> getDBlist(HostsCfg hostsObj, String dbstr) {
		try 
		{	
			Vector <String> list = new Vector <String> ();
			DBConn dbc = hostsObj.getDBConn("mysql");
			
			ResultSet rs = dbc.executeQuery("show databases");
			while (rs.next()) 
			{
				String dbName = rs.getString(1);
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
			for (String dbName : list) {
				DBConn mdb = hostsObj.getDBConn(dbName);
				DBInfo db = setDBParams(mdb, dbName, dbstr);
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
        		DBInfo db = new DBInfo();
	        db.dbName = dbName;
	        db.id = Globalx.error;
	        
        		if (!mdb.tableExists("assembly")) {
        			System.err.println("Error: corrupt database  '" + dbName);
        			return null;
        		}
            String strQ = "SELECT username, projectpath, assemblydate, annotationdate, assemblyid " +
            		"FROM assembly";
 
            rs = mdb.executeQuery ( strQ );
            if ( !rs.next() ) {
            		System.err.println(Globalx.error + "Database not instantiated  '" + dbName + "' -- ignoring");
            		rs.close();
        			return db;
            }
            
            db.id = rs.getString("assemblyid");
            db.username = rs.getString("username");
            db.assemblydate = rs.getString("assemblydate");
            
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
	
	/**
	 ******** Class  DBInfo method *******************************
	 ***/
	public DBInfo() {}
	
	public boolean checkDBver(HostsCfg hostsObj) {
		try {
			DBConn dbc = new DBConn(hostsObj.host(), dbName,hostsObj.user(), hostsObj.pass());
			
			Schema s = new Schema(dbc);
			if (!s.current()) dbVerStr = s.update();
			
			dbc.close();
			return true;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Unable to update schema");}
		
		return false;
	}
	
	public String toString() {
		String str = id + "    [" + dbName + " " + assemblydate + " " + username + "]";
		String v = dbVerStr;
		if (!v.equals(Schema.currentVerString())) str += " db" + v;
		return str;
	}
	public String getDescription() {
		return "viewSingleTCW v" + Globalx.strTCWver + "   " + dbName + "::" + id;
	}
	public String getID() {return id;}
	public String getdbName() {return dbName;}
	
	private String id="", username="",  assemblydate="", dbVerStr="";
	private String dbName;
}
