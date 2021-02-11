package sng.amanager;

/**************************************************
 * Runs commands for ManagerFrame
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;

import javax.swing.JOptionPane;

import util.database.DBConn;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.TCWprops;

public class RunCmd {
	private static final String MAX_MEM = "6144"; // used to launch all programs except assembly
	private ManagerFrame theParentFrame=null;
	private HostsCfg hostsObj=null;
	private boolean bBusy=false;
	
	public RunCmd(ManagerFrame mf, HostsCfg cf) {
		theParentFrame = mf;
		hostsObj = cf;
	}
	public boolean isBusy() { return bBusy;}
	/******************************************************
	 * XXX ACTIONS - call external programs
	 */
	public void actionLoadLibrary() {
		try {
			if (!theParentFrame.saveProject()) return;

			bBusy = true;
			theParentFrame.updateEnable(true);
			
			ManagerData curManData = theParentFrame.getCurManData();
			
			final String JAR_DIR = "java/jars";
			final String JAR = JAR_DIR + "/stcw.jar";
			final String MAXMEM = MAX_MEM;
			
			final String classPath = JAR + ":" + JAR_DIR + "/mysql-connector-java-5.0.5-bin.jar";
			final String cmd = "java -Xmx" + MAXMEM + "m -cp " + classPath + " sng.assem.LoadLibMain " + curManData.getProjDir();

			Process pr = Runtime.getRuntime().exec(cmd);
			
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();
			
			int c = 0;
			String stcwdb = curManData.getTCWdb();
			if (hostsObj.checkDBConnect(stcwdb)) { 
				DBConn conn = hostsObj.getDBConn(stcwdb);
				c = conn.executeCount("select count(*) from assem_msg");
			}
			if (c==0) { 
				JOptionPane.showMessageDialog(null, 
						"Load Data failed - remove database", "Error", 
						JOptionPane.PLAIN_MESSAGE);
			}
			else {
				curManData.setProteinDB(theParentFrame.isAAtcw());
				curManData.setLibraryLoaded(true);
			}
			
			bBusy = false;
			theParentFrame.updateEnable(true);
				
		} catch (Exception e) {ErrorReport.prtReport(e, "Error launching Load Data");}
	}
	public void actionCreateTrans() {
		try {
			if (!theParentFrame.saveProject()) return;
		
			bBusy = true;
			theParentFrame.updateEnable(true);
			ManagerData curManData = theParentFrame.getCurManData();

			int maxMem = 0;
			int numClones = 0;
			int avgLen = 0;

			try
			{
				DBConn conn = hostsObj.getDBConn(curManData.getTCWdb());
				ResultSet rset = conn.executeQuery("select count(*) from clone");
				rset.next();
				numClones = rset.getInt(1);
				rset.close();
				
				rset = conn.executeQuery("select sum(libsize*avglen)/sum(libsize) from library"); 
				rset.next();
				avgLen = rset.getInt(1);
				rset.close();
				conn.close();
				
				maxMem = numClones/200;
				if(avgLen > 300) {
					maxMem *= (avgLen/300);
				}
				
				maxMem += 1000;
				if(numClones > 3000000) {
					System.err.println("*Warning: " + numClones + " sequences found in database. Estimated memory requirement: " + maxMem + "M");
					System.err.println("*See System Guide for more information on memory usage.");
				}
				
			} 
			catch (Exception e)
			{
				System.err.println("Can't locate database " + curManData.getTCWdb());
				System.exit(0);
			}

			final String JAR_DIR = "java/jars";
			final String JAR = JAR_DIR + "/stcw.jar";
			
			final String classPath = JAR;
			final String cmd = "java -Xmx" + maxMem + "m -cp " + classPath + " sng.assem.AssemMain " + curManData.getProjDir();

			Process pr = Runtime.getRuntime().exec(cmd);
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();
					
			DBConn conn = hostsObj.getDBConn(curManData.getTCWdb());
			int c = conn.executeCount("select count(*) from contig");
			if (c==0) { 
				JOptionPane.showMessageDialog(null, "Instantiate failed", "Error", 
						JOptionPane.PLAIN_MESSAGE);
			}
			else {
				curManData.setInstantiated(true);
			}
			conn.close();
			
			bBusy = false;
			theParentFrame.updateEnable(true);
		} catch (Exception e) {
			ErrorReport.prtReport(e, "Error generating list");
		}
	}
	
	public void actionAnno(boolean prompt) {
		try {
			if (!theParentFrame.saveProject()) return;
			bBusy = true;
			theParentFrame.updateEnable(true);
			ManagerData curManData = theParentFrame.getCurManData();
			
			final String classPath = "java/jars/stcw.jar";
			final String MAXMEM = MAX_MEM;
			final String cmd = "java -Xmx" + MAXMEM + "m -cp " + classPath + 
					" sng.annotator.runSTCWMain " + curManData.getProjDir();
			final String cmdn = "java -Xmx" + MAXMEM + "m -cp " + classPath + 
					" sng.annotator.runSTCWMain " + curManData.getProjDir() + " -n";
			Process pr;
			
			if (prompt) pr = Runtime.getRuntime().exec(cmd);
			else pr = Runtime.getRuntime().exec(cmdn);
			
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();

			if (pr.exitValue() != 255) // not cancelled
			{
				theParentFrame.tcwDBselectLoadStatus();
			}
			bBusy = false;
			theParentFrame.updateUI();
		} catch (Exception e) {ErrorReport.prtReport(e, "Error launching annotater");}
	}
	public void actionRedo(String opt) {
		try 
		{
			if (!theParentFrame.saveProject()) return;
			bBusy = true;
			theParentFrame.updateEnable(true);
			ManagerData curManData = theParentFrame.getCurManData();
			
			final String JAR_DIR = "java/jars";
			final String JAR = JAR_DIR + "/stcw.jar";
			final String MAXMEM = MAX_MEM;
			final String classPath = JAR;
			final String cmd = "java -Xmx" + MAXMEM + "m -cp " + classPath + " sng.annotator.runSTCWMain " + 
					curManData.getProjDir() + " " + opt;
			Process pr = Runtime.getRuntime().exec(cmd);
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();

			if (pr.exitValue() != 255) // not cancelled
			{
				theParentFrame.tcwDBselectLoadStatus();
			}
			bBusy = false;
			theParentFrame.updateEnable(true);
		} 
		catch (Exception e) {ErrorReport.prtReport(e, "Error updating GO");}
	}
	public void actionView() {
		try {
			ManagerData curManData = theParentFrame.getCurManData();
			String target = curManData.getTCWdb() + ":" + curManData.getProjID();
			String host = hostsObj.host();
			if(!host.equals("localhost")) target = host + ":" + target;
			
			final String JAR_DIR = "java/jars";
			final String JAR = JAR_DIR + "/stcw.jar";
			final String MAXMEM = MAX_MEM;
			final String classPath = JAR ;
			final String cmd = "java -Xmx" + MAXMEM + "m -cp " + classPath + " sng.viewer.STCWMain " 
				+ target;

			System.err.println("Launching viewSingleTCW for " + 
					host + ":" + curManData.getTCWdb() + ":" + curManData.getProjID());
			Process pr = Runtime.getRuntime().exec(cmd);
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();
		} catch (Exception e) {ErrorReport.prtReport(e, "Error launching viewSingleTCW");}
	}
	// this is not being called anymore because the user may need to change the R path
	public void actionDE() {
		try {
			ManagerData curManData = theParentFrame.getCurManData();
			String host = hostsObj.host();
			final String JAR_DIR = "java/jars";
			final String JAR = JAR_DIR + "/stcw.jar";
			final String MAXMEM = MAX_MEM;
			final String classPath = JAR;
			String jriPath = " -Djava.library.path=" + TCWprops.getExtDir() + "/jri ";
			final String cmd = "java " + jriPath + " -Xmx" + MAXMEM + "m -cp " + classPath + " sng.runDE.QRMain " 
					+ curManData.getTCWdb();
			System.err.println("Launching DE for " + 
					host + ":" + curManData.getTCWdb() + ":" + curManData.getProjID());
			String rHome = System.getenv("R_HOME");
			if (rHome == null)
			{
				System.err.println("You must set the R_HOME environment variable to the location of the R installation.");
				return;
			}
			Process pr = Runtime.getRuntime().exec(cmd,null);
			pr.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(pr.getErrorStream());
			InputHandler ih = new InputHandler(pr.getOutputStream());
			
			oh.start();
			ih.start();
			pr.waitFor();
			oh.Stop();
			ih.Stop();
		} catch (Exception e) {ErrorReport.prtReport(e, "Error launching DE");}
	}
	/**************************************************************/
	public class OutputHandler extends Thread
	{
	    InputStream is;
	    
	    public OutputHandler(InputStream is)
	    {
	        this.is = is;
	    }
	    
	    public void Stop() {
		    	try {
			    	if(is != null) is.close();
		    	}
		    	catch(Exception e) {	}
		}
	    
	    public void run()
	    {
	        try
	        {
				int c;
				while((c = is.read()) != -1) {
					System.err.print((char)c);
					System.err.flush();
				}
	        } catch (Exception e) {}
	    }
	}
	
	public class InputHandler extends Thread
	{
	    boolean keepRunning = true;
	    BufferedReader inRead;
	    InputStreamReader isr;
	    BufferedWriter osw;
	    
	    public InputHandler(OutputStream os) {
	        isr = new InputStreamReader(System.in);
	        osw = new BufferedWriter(new OutputStreamWriter(os));
	    }
	    
	    public void Stop() {
	    		keepRunning = false;
	    }
	    
	    public void run()
	    {
	        try {
		        	while (keepRunning)
		        	{
		        		if (isr.ready())
		        		{
		        			int c = isr.read();
		        			osw.write(c);
		        			osw.flush();
		        		}
		        		Thread.sleep(100);
		        	}
		        	osw.close();
		        	inRead.close();
	        } 
	        catch (Exception e) {}
	    }
	}
}
