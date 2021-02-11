package util.database;
/***************************************************
 * Reads HOSTs.cfg for Mysql host, user and password
 * Almost all methods get the hostsObj from STCWFrame and use hostObj.getDBConn to 
 * get access to the database
 */
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.TreeSet;

import util.file.FileHelpers;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.TCWprops;

public class HostsCfg 
{
	final String CAP3_DEF = TCWprops.getExtDir() + "/CAP3/cap3";
	final String HOSTS = Globalx.HOSTS;
	final String localHost = "localhost";
	
	public HostsCfg() 
	{	
		if (!theHost.contentEquals("")) return; // already run
		try
		{
			File f = new File(HOSTS);
			if (!f.isFile())
			{
				System.err.println("Cannot find " + HOSTS);
				System.exit(0);
			}
			
			theHost=localHost; //default
			loadHOSTFile(f);
			checkDBConnect();
			
			getSearchPath(false);	
		}
		catch(Exception e){ErrorReport.die("Error reading HOSTS.cfg!!");}
	}
	
	// -v Only - Used by runSingleTCW to check database variables
	public HostsCfg(boolean chkVar) { // value doesn't matter
		if (!theHost.contentEquals("")) return; // already run
		
		try {
			String version = System.getProperty("java.version");
			Out.PrtSpMsg(0, "Java version " + version);
			Out.PrtSpMsg(0,"");
			
			File f = new File(HOSTS);
			if (!f.isFile()) {
				System.err.println("Cannot find " + HOSTS);
				System.exit(0);
			}
			theHost="localhost"; //default
			loadHOSTFile(f);
			
			DBConn.checkVariables(theHost, theUser, thePass, true);
			
			Out.PrtSpMsg(0,"");
			getSearchPath(true);
			
			Out.PrtSpMsg(0, "Check complete");
		}
		catch(Exception e){ErrorReport.die("Error reading HOSTS.cfg!!");}
	}
	/*******************************************************
	 * Reads HOSTS.cfg
	 */
	private void loadHOSTFile(File f) throws Exception
	{
		theUser="";
		BufferedReader br = new BufferedReader(new FileReader(f));
		TreeSet<String> seen = new TreeSet<String>();
		while (br.ready())
		{	
			String line = br.readLine();
			line = line.replaceAll("#.*","");
			line += " "; // so the split works right 
			String[] fields = line.split("=");
			if (fields.length != 2) continue;
			
			String key = fields[0].trim();
			String val = fields[1].trim();
			
			key = key.replaceFirst("PAVE_", "DB_");

			if (seen.contains(key.toLowerCase()))
			{
				System.err.println("Duplicated parameter " + key);
				System.err.println("HOSTS.cfg can only have one set of parameters");
				System.exit(0);
			}
			seen.add(key.toLowerCase());
			
			if (key.equalsIgnoreCase("db_host")) theHost = val;
			else if (key.equalsIgnoreCase("db_user")) theUser = val;
			else if (key.equalsIgnoreCase("db_password")) thePass = val;
			// else if (key.equalsIgnoreCase("db_port")) thePort = val; CAS303 was for applet
			else otherParams.put(key, val);
		}
		br.close(); 
		if (theUser.equals("")) ErrorReport.die("DB_user has no value in " + HOSTS);
	}

	
	private void checkDBConnect()
	{
		if (DBConn.checkMysqlServer(theHost, theUser, thePass)) {
			return;
		}
		
		System.err.println("Could not connect to MySQL server using the information in HOSTS.cfg");
		
		// CAS303 moved here as only used when cannot connect
		// this all may not be necessary
		checkLocalHost(); 
		if (!isLocalHost)
		{
			System.err.println("The host (" + theHost + ") does not connect to MySQL server");
			System.err.println("Known aliases are of the localhost:");
			for (String alias : localHostAliases)
				System.err.println(alias);
			System.exit(0);
		}
		else
		{
			// Try all the aliases
			System.err.println("Trying other aliases for localhost");
			for (String alias : localHostAliases)
			{
				if (DBConn.checkMysqlServer(alias, theUser, thePass))
				{
					System.err.println(alias + ": success!!");
					System.err.println("Using hostname:" + alias);
					theHost = alias;
					return;
				}
				else
					System.err.println(alias + ": fail");
			}
		}
		System.exit(0);
	}
	// Find all the aliases for the current machine, and see if the specified host is one of them
	private void checkLocalHost()
	{
		try
		{
			localHostAliases.add(localHost);
			if (theHost.equalsIgnoreCase(localHost)) {
				theHost = localHost;
				isLocalHost = true;
			}
			String hostName = getHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost)) {
				isLocalHost = true;
			}
			
			hostName = java.net.InetAddress.getLocalHost().getHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost)){
				isLocalHost = true;
			}
			
			hostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost)) {
				isLocalHost = true;
			}	
			
			hostName = java.net.InetAddress.getLocalHost().getHostAddress();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost)) {
				isLocalHost = true;
			}			
			
			if (isLocalHost) {
				if (!theHost.equals(localHost)) {
					System.err.println("Using host:" + theHost + " which is the local host");
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Error: reading HOSTs.cfg file");
			ErrorReport.prtReport(e, "Reading HOSTs.cfg file");
		}
	}
	private String getHostName() throws Exception
	{
		String name="";
		if (System.getenv().containsKey("HOSTNAME")) name = System.getenv("HOSTNAME");
		if (!name.contentEquals("")) return name;
		
        Process p = Runtime.getRuntime().exec("uname -n");
        BufferedReader stdInput = new BufferedReader(new 
             InputStreamReader(p.getInputStream()));
        name = stdInput.readLine().trim();       
        p.destroy();			
		
		return name;
	}
	
	/********************************************
	 * Public
	 */
	public boolean checkDBConnect(String db)
	{
		return DBConn.checkMysqlDB("Check DB connection ", theHost, db, theUser, thePass);
	}
	public DBConn getDBConn(String db)
	{
		try
		{
			if (!db.trim().equals("") && !db.trim().equals("mysql"))
			{
				return new DBConn(theHost,db,theUser,thePass);
			}
			else
			{
				return new DBConn(theHost, theUser, thePass);
			}
		}
		catch(Exception e)
		{ 
			ErrorReport.reportError(e, "Unable to connect to database!!");
		}
		return null;
	}
	
	/*************************************************
	 * External directory
	 */
	
	private void getSearchPath(boolean prt) // the paths can be hard-coded in HOSTS.cfg
	{
		FileHelpers.makeExt(); // CAS303 one time only, move external->ext/lintel
		
	    String bpath = "", dpath="";
	    
		if (otherParams.containsKey("blast_path"))
		{
			bpath = otherParams.get("blast_path").trim();
			if (bpath.endsWith("/")) bpath = bpath.substring(0, bpath.length()-1);
		}
		if (otherParams.containsKey("diamond_path"))
		{
			dpath = otherParams.get("diamond_path").trim();
			if (dpath.endsWith("/")) dpath = dpath.substring(0, dpath.length()-1); 
		}
		BlastArgs.evalBlastPath(bpath, dpath, prt);
	}
	
	public String getCapPath() 
	{
		String capPath="";
		if (otherParams.containsKey("cap3_path")) capPath = otherParams.get("cap3_path").trim();
		if (capPath.equals("")) capPath = CAP3_DEF; 
		
	 	File filePath = new File ( capPath );
	 	if ( !filePath.isFile() || !filePath.exists() ) return "";
	 	return capPath;
	}
	public String host() {return theHost;}
	public String user() {return theUser;}
	public String pass() {return thePass;}
	
	private String theHost="", theUser="", thePass="";
	private boolean isLocalHost = false;
	
	private TreeSet<String> localHostAliases = new TreeSet<String>();
	private TreeMap<String,String> otherParams = new TreeMap<String,String>();
}
