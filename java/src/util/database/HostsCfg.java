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

import javax.swing.JApplet;

import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.TCWprops;

public class HostsCfg 
{
	final String CAP3_DEF = TCWprops.getExtDir() + "/CAP3/cap3";
	final String HOSTS = Globalx.HOSTS;
	
	public HostsCfg() 
	{	
		try
		{
			if (theHost.equals(""))
			{
				File f = new File(HOSTS);
				if (!f.isFile())
				{
					System.err.println("Can't find " + HOSTS);
					System.exit(0);
				}
				theHost="localhost"; //default
				loadHOSTS(f);
				checkLocalHost();
				checkDBConnect();
				getBlastPath();	
			}
		}
		catch(Exception e){ErrorReport.die("Error reading HOSTS.cfg!!");}
	}
	// Used by the applets. It doesn't do localhost checks.
	public HostsCfg(String h, String u, String p)
	{
		theHost = h;
		theUser = u;
		thePass = p;
		
		String[] f2 = theHost.split(":");
		if (f2.length == 2)
		{
			theHost = f2[0];
			thePort = f2[1];
		}
		isLocalHost = false;
	}
	private void loadHOSTS(File f) throws Exception
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
			if (key.equalsIgnoreCase("db_host"))
			{
				theHost = val;
				String[] f2 = theHost.split(":");
				if (f2.length == 2)
				{
					theHost = f2[0];
					thePort = f2[1];
				}
			}
			else if (key.equalsIgnoreCase("db_port")) thePort = val;
			else if (key.equalsIgnoreCase("db_user")) theUser = val;
			else if (key.equalsIgnoreCase("db_password")) thePass = val;
			else otherParams.put(key, val);
		}
		br.close(); 
		if (theUser.equals("")) ErrorReport.die("DB_user has no value in " + HOSTS);
	}

	// Find all the aliases for the current machine, and see if
	// the specified host is one of them
	private void checkLocalHost()
	{
		try
		{
			localHostAliases.add("localhost");
			if (theHost.equalsIgnoreCase("localhost"))
			{
				theHost = "localhost";
				isLocalHost = true;
			}
			String hostName = getHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost))
			{
				isLocalHost = true;
			}
			hostName = java.net.InetAddress.getLocalHost().getHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost))
			{
				isLocalHost = true;
			}		
			hostName = java.net.InetAddress.getLocalHost().getCanonicalHostName();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost))
			{
				isLocalHost = true;
			}			
			hostName = java.net.InetAddress.getLocalHost().getHostAddress();
			localHostAliases.add(hostName);
			if (hostName.equalsIgnoreCase(theHost))
			{
				isLocalHost = true;
			}			
			if (localHostAliases.size() <= 1)
			{
				System.err.println("Unable to determine local host name or address");
			}
			if (isLocalHost)
			{
				if (!theHost.equals("localhost"))
				{
					System.err.println("Using host:" + theHost + " which is the local host");
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Error: reading HOSTs.cfg file");
			e.printStackTrace();
		}
	}
	private void checkDBConnect()
	{
		if (DBConn.checkMaxAllowedPacket(theHost, theUser, thePass)) {
			return;
		}
		
		System.err.println("Could not connect to MySQL server using the information in HOSTS.cfg");
		if (!isLocalHost)
		{
			System.err.println("The host (" + theHost + ") does not appear to be an alias of localhost");
			System.err.println("known aliases are:");
			for (String alias : localHostAliases)
			{
				System.err.println(alias);
			}
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
				{
					System.err.println(alias + ": fail");
				}
			}
		}
		System.exit(0);
	}
	
	private void getBlastPath() 
	{
	    String bpath = "", dpath="", upath="";
	    
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
		if (otherParams.containsKey("usearch_path"))
		{
			upath = otherParams.get("usearch_path").trim();
			if (upath.endsWith("/")) upath = upath.substring(0, upath.length()-1); 
		}
		BlastArgs.evalBlastPath(bpath, dpath, upath);
	}
	
	private String getHostName() throws Exception
	{
		String name = getEnv("HOSTNAME");
		
        Process p = Runtime.getRuntime().exec("uname -n");
        BufferedReader stdInput = new BufferedReader(new 
             InputStreamReader(p.getInputStream()));
        name = stdInput.readLine().trim();       
        p.destroy();			
		
		return name;
	}
	private String getEnv(String key)
	{
		if (System.getenv().containsKey(key))
		{
			return System.getenv(key);
		}
		return "";
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
	public String port() {return thePort;}
	
	private String theHost="";      
	private String theUser="";
	private String thePass="";
	private String thePort="";
	private boolean isLocalHost = false;
	
	private TreeSet<String> localHostAliases = new TreeSet<String>();
	private TreeMap<String,String> otherParams = new TreeMap<String,String>();
	
	private JApplet applet = null;
	public JApplet getApplet() { return applet; }
	public void setApplet(JApplet applet) { this.applet = applet; }
}
