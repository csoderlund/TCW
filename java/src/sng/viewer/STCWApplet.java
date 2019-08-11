/**
 * This is not referenced from any PAVE file, but reference from the HTML
 */
package sng.viewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JApplet;
import javax.swing.JOptionPane;


import sng.database.DBInfo;
import sng.database.Version;
import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.TimeHelpers;
import util.ui.UIHelpers;

public class STCWApplet extends JApplet 
{
	public void start ()
	{
		if (!Version.checkJavaSupported()) System.exit(-1);
		
		System.err.println("Starting viewSingleTCW " + Version.strTCWver + " " + Version.strRelDate);
		UIHelpers.setIsApplet(true);
		
		try {
			dbName =  getParameter("ASSEMBLY_DB");
			String host  = getParameter("DB_URL");
			String user = getParameter("DB_USER");
			String pass = getParameter("DB_PASS");
			
			if (!setDBobj(host, user, pass)) return;
			openAssembly ( );	
		}
		catch ( Exception err )
		{
			JOptionPane.showMessageDialog(null, "Error starting applet for " + dbName);
			System.err.println("Error starting TCW applet");
			err.printStackTrace();
		}
	}
	/********* check database **********************/
	private boolean setDBobj(String host, String user, String pass) {
		try {
			hostsObj = new HostsCfg(host, user, pass);
			hostsObj.setApplet( this );
			
			if (!DBConn.checkMysqlServer(host, user, pass))
			{
				showErr("Unable to connect to MySQL database with URL " + host);
				return false;
			}
			DBConn.checkMaxAllowedPacket(host, user, pass);
			
			DBConn mDB = getConn();
			if (mDB==null) return false;
			dbObj = DBInfo.setDBParams(mDB, dbName, dbName);	
			mDB.close();
	        
	        if (dbObj==null) {
	        		showErr("Error reading " + dbName + " on " + host);
	        		return false;
	        }
	        if (!dbObj.checkDBver(hostsObj)) {
		        	showErr("Schema needs updating for " + dbName + "\nRun from desktop to update.");
	        		return false;
	        }
	        // set blast path
	        BlastArgs.evalBlastPath("", "", ""); // 2nd arg is diamond, which is not used for interactive blast
			if (!BlastArgs.foundABlast())
				BlastArgs.evalBlastPath("/usr/local/ncbi/blast/bin", "", "");	
			
	        return true;
		}
        catch ( Exception err )
		{
			showErr("Error starting applet for " + dbName);
			err.printStackTrace();
		}
        return false;
	}
	private DBConn getConn() {
		try {
	        DBConn mdb= hostsObj.getDBConn(dbName);
	        if (mdb==null) {
	        		showErr("The connection to database is null for " + dbName);
	        		return null;
	        }
	        return mdb;
	    }
	    catch (Exception e) { 
	    		showErr("Error getting mySQl connection for " + dbName);
	    		ErrorReport.reportError(e, "Getting mySQL connection for " + dbName);
	    		return null;
	    	}
	}
	private void showErr(String msg) {
		JOptionPane.showMessageDialog(null, msg);
		System.err.println("Error: " + msg);
	}
	/********* openAssembly **********************/
	private void openAssembly ( )
	{
		if ( theFrame != null ) theFrame.toFront();
		else 
		{
			System.err.println("Opening TCW database '" + dbName + "' ..."); 
			
			theFrame = new STCWFrame (hostsObj, dbObj, true);
			
			theFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					theFrame = null;
				}

				public void windowClosed(WindowEvent e) {
					theFrame = null;
				}
			});	
		}
	}
	/***
	private void oldCode() {
		String url = getParameter("START_PAGE_URL") ;
		String startURL = Helpers.normalizeURL(startURL);
		new URL(startURL);
		
		String str = getAppletContext().getClass().toString();
		hostsObj.setAppletDebug( str.equals( "class sun.applet.AppletViewer" ) );				
	}
	**/
	public void stop() 
	{
		System.err.println("Closing TCW session '" +  dbName + "'...");
		theFrame = null;
		hostsObj = null;
		dbObj = null;
	}
	
	private String dbName;
	private HostsCfg hostsObj=null;
	private DBInfo dbObj=null;
	private STCWFrame theFrame = null;
    private static final long serialVersionUID = 1;
}
