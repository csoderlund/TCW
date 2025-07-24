package cmp.compile.panels;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import util.methods.ErrorReport;

public class RunCmd {
	static Map<String,Long> mCmdTimes = null;
	static Map<String,Integer> mCmdCounts = null;
	
	static int runCommand(String cmd, boolean showStdOut, boolean showStdErr, int nThread)
			throws Exception
	{
		String[] args = cmd.split("\\s+");
		return runCommand(args,null,showStdOut,showStdErr,null,nThread);
	}
	
	public static int runCommand(String[] args, File dir, boolean showStdOut, boolean showStdErr, File outFile, int nThread)
	throws Exception
	{
		String cmd = args[0];
		cmd = cmd.replaceAll(".*/", "");
		if (mCmdCounts != null)
		{
			if (!mCmdCounts.containsKey(cmd)) 
			{
				mCmdCounts.put(cmd, 0);
				mCmdTimes.put(cmd, 0L); // CAS303 new Long -> 0L
			}
			mCmdCounts.put(cmd,1 + mCmdCounts.get(cmd));
		}
		//Utils.intTimerStart(nThread);
		
		Process p = Runtime.getRuntime().exec(args,null,dir);
		
		BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
		
		BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		
		BufferedWriter outWriter =  null;
		if (outFile != null)
		{
			outWriter = new BufferedWriter(new FileWriter(outFile));
		}
		StringBuilder errStr = new StringBuilder();
		while (true)
		{
			if (showStdOut)
			{
				while (stdOut.ready())
					System.err.append((char) stdOut.read());
			}
			else if (outWriter != null)
			{
				while (stdOut.ready())
					outWriter.write(stdOut.readLine() + "\n");
			}
			else
			{
				while (stdOut.ready()) stdOut.readLine();
			}
			if (showStdErr)
			{
				while (stdError.ready())
				{
					errStr.append((char)stdError.read());
					//System.err.append((char) stdError.read());
				}
			}
			else
			{
				while (stdError.ready()) stdError.readLine();
			}
			try
			{
				p.exitValue();
				break;
			} 
			catch (Exception e)
			{
				Thread.sleep(100);
				if (outWriter != null) outWriter.flush();
			}
		}
		if (showStdOut)
		{
			while (stdOut.ready())
				System.err.append((char) stdOut.read());
		}
		else if (outWriter != null)
		{
			while (stdOut.ready())
				outWriter.write(stdOut.readLine() + "\n");
		}
		else
		{
			while (stdOut.ready()) stdOut.readLine();
		}
		if (showStdErr)
		{
			while (stdError.ready())
				System.err.append((char) stdError.read());
		}
		else
		{
			while (stdError.ready()) stdError.readLine();
		}		
		stdOut.close();
		stdError.close();
		if (outWriter != null) 
		{
			outWriter.flush();
			outWriter.close();
		}
		if (errStr.length() > 0)
		{
			ErrorReport.reportError(errStr.toString());	
		}
		int ev = p.exitValue();
		p.getInputStream().close();
		p.getOutputStream().close();
		p.getErrorStream().close();
		p.destroy();
		return ev;
	}
	/****************************************************************************/
	public RunCmd() {}
	
	public void viewMultiTCW(String db) {
		final String dbName = db;
		Thread buildThread = new Thread(new Runnable() {
			public void run() {
				try {
					final String JAR_DIR = "java/jars";
					final String JAR = JAR_DIR + "/mtcw.jar";
					final String MAXMEM = "4096";
					final String classPath = JAR + ":" + JAR_DIR + "/mysql-connector-java-5.0.5-bin.jar";
					final String cmd = "java -Xmx" + MAXMEM + "m -cp " + classPath + " cmp.viewer.MTCWMain " + dbName;

					System.err.println("Launching viewMultiTCW for " + dbName);
					String [] x = cmd.split("\\s+");
					Process pr = Runtime.getRuntime().exec(x); // CAS405 add split
					pr.getOutputStream().flush();
					OutputHandler oh = new OutputHandler(pr.getErrorStream());
					InputHandler ih = new InputHandler(pr.getOutputStream());
					
					oh.start();
					ih.start();
					
					pr.waitFor();
					
					oh.Stop();
					ih.Stop();
				} catch (Exception e) {
					ErrorReport.prtReport(e, "Error launching viewMultiTCW");
				}
			}
		});
		buildThread.setPriority(Thread.MIN_PRIORITY);
		buildThread.start();
	}
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
	        } catch (Exception e) {
	        	
	        }
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
