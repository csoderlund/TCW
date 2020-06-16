package sng.runAS;
/*****************************************************
 * Run a command - copied from ManagerFrame
 */
import java.io.*;
import java.net.MalformedURLException;
import java.util.Vector;

import util.methods.ErrorReport;
import util.methods.Out;

public class RunCmd {
	
	public void runCmd() {}
	
	/**************************************************************
	 * Run a command that does not use redirection or piples
	 *last arg is dir - the working directory of the subprocess, 
	 *or null if the subprocess should inherit the working directory of the current process.
	 * ****************************************/
	public int run(String cmd, String dir) { return runCmd(cmd, dir, true);}
	
	public int runP(String cmd, String dir, boolean prtStdErr) {
		try {
			Out.PrtSpMsg(2, "Exec: " + cmd);
			if (dir!=null) Out.PrtSpMsg(2, "From: " + dir);
			
			int rc = runCmd(cmd, dir, prtStdErr);
			if (rc!=0) {
				Out.PrtError("Command failed " + rc);
				return rc;
			}
			return rc;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "run command"); return -1;}
	}
	public int runCmd(String cmd, String dir, boolean prtStdErr) {
	 	int exitVal=0;
	    	try {
	    		String[] args = cmd.split("\\s+");
	    		File f = (dir==null) ? null : new File(dir);
	    		
	    		Process p = Runtime.getRuntime().exec(args, null, f);
	    		p.getOutputStream().flush();
				OutputHandler oh = new OutputHandler(p.getErrorStream());
				InputHandler ih = new InputHandler(p.getOutputStream());
				
				if (prtStdErr) oh.start();
				ih.start();
				
				p.waitFor();
				
				oh.Stop();
				ih.Stop();
		    	exitVal = p.exitValue();
	    	}
	    	catch (MalformedURLException e) {
	    		e.printStackTrace();
	    		Out.PrtWarn("MalformedURLException: command failed");
	    		exitVal=-1;
	    	}
	    catch (IOException e) {
	    		e.printStackTrace();
	    		Out.PrtWarn("IOException: command failed");
	    		exitVal=-2;
	    	}
	    	catch (Exception e) { 
	    		e.printStackTrace();
	    		Out.PrtWarn("Exception: command failed");
	    		exitVal=-3;
	    	}
	    	return exitVal;
	}
	private class OutputHandler extends Thread
	{
	    InputStream is;
	    public OutputHandler(InputStream is){this.is = is;}
	    
	    public void Stop() {
		    	try {
		    		if (is != null) {
		    			for (int i=0; i<100000; i++);
		    			is.close();
		    		}
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
	private class InputHandler extends Thread
	{
	    boolean keepRunning = true;
	    BufferedReader inRead;
	    InputStreamReader isr;
	    BufferedWriter osw;
	    
	    public InputHandler(OutputStream os) {
	        isr = new InputStreamReader(System.in);
	        osw = new BufferedWriter(new OutputStreamWriter(os));
	    }
	    
	    public void Stop() {keepRunning = false;}
	    
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
	
	public int runCmdStdin(String cmd, String dir, Vector <File> stdin) {
	 	int exitVal=0;
	    	try {
	    		String[] args = cmd.split("\\s+");
	    		File d = (dir==null) ? null : new File(dir);
	    		Process p = Runtime.getRuntime().exec(args, null, d);
	    		//p.getOutputStream().flush();
	    		//OutputHandler oh = new OutputHandler(p.getErrorStream());
			//oh.start();
	    		// Write into the standard input of the subprocess
	        PrintStream pin = new PrintStream(new BufferedOutputStream(p.getOutputStream()));
	       
	        for (File f : stdin) {
    				BufferedReader br = new BufferedReader(new FileReader (f));
    				String line;
    				while ((line = br.readLine()) !=null) {
    					pin.print(line);
    				}
    				br.close();
    			}
	        pin.close();
	        
	        p.waitFor();
	        //oh.Stop();
			exitVal = p.exitValue();
	    	}
	    catch (IOException e) {
	    		e.printStackTrace();
	    		Out.PrtWarn("IOException: command failed");
	    		exitVal=-2;
	    	}
	    	catch (Exception e) { 
	    		e.printStackTrace();
	    		Out.PrtWarn("Exception: command failed");
	    		exitVal=-3;
	    	}
	    	return exitVal;
	}
}
