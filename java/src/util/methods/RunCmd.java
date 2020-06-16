package util.methods;
/*****************************************************
 * Run a command - copied from ManagerFrame
 */
import java.io.*;
import java.net.MalformedURLException;
import java.util.Vector;

import util.methods.Out;

public class RunCmd {
	
	public void runCmd() {}
	
	/*************************************************
	 * Does NOT work with redirections - see runP below for redirection
	 */
	public int runP(String cmd, boolean prt) {
		if (prt) {
			Out.prt("");
			Out.PrtSpMsg(0, "Exec: " + cmd);
		}
		return runCmd(cmd, prt);
	}
	private int runCmd(String cmd, boolean prt) {
	 	int exitVal=0;
	    	try {
	    		String[] args = cmd.split("\\s+");
	    		
	    		Process p = Runtime.getRuntime().exec(args, null, null); // directory is last arg
	    		p.getOutputStream().flush();
			OutputHandler oh = new OutputHandler(p.getErrorStream());
			InputHandler ih = new InputHandler(p.getOutputStream());
			
			if (prt) {
				oh.start();
				ih.start();
			}
			p.waitFor();
			
			oh.Stop();
			ih.Stop();
	    		exitVal = p.exitValue();
	    		
	    		if (prt) Out.prt("");
	    	}
	    	catch (MalformedURLException e) { 
	    		ErrorReport.reportError(e, "Run cmd: MalformedURLException");
	    		if (!prt) Out.prt("Cmd: " + cmd);
	    		exitVal=-3;
	    	}
	    catch (IOException e) {
	    		ErrorReport.reportError(e, "Run cmd: IOException - check permissions");
	    		if (!prt) Out.prt("Cmd: " + cmd);
	    		exitVal=-2;
	    	}
	    	catch (Exception e) {
	    		ErrorReport.reportError(e, "Run cmd: command failed");
	    		if (!prt) Out.prt("Cmd: " +cmd);
	    		exitVal=-1;
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
	/******************************************************
	// XXX Execute a program that uses ">" redirection
	 ******************************************************/
	public int runP(String cmd, String outFile, String errFile, boolean prt) {
		if (prt) {
			Out.prt("");
			Out.PrtSpMsg(0, "Exec: " + cmd);
		}
		return runStdOut(cmd, outFile, errFile, prt);
	}
	
	private int runStdOut(String cmd, String outFile, String errFile, boolean prt) {
		int exitVal=0;
    	try {
    		String[] args = cmd.split("\\s+");
    		ProcessBuilder pb = new ProcessBuilder(args);
    		pb.redirectOutput(new File(outFile));
    		pb.redirectError(new File(errFile));
    		Process p = pb.start(); 
    		
    		exitVal = p.waitFor();
    		
    		return exitVal;
    	}
    	catch (MalformedURLException e) {
    		ErrorReport.reportError(e, "Run Cmd: MalformedURLException");
    		if (!prt) Out.prt("Cmd: " + cmd);
    		exitVal=-3;
    	}
	    catch (IOException e) {
	    		ErrorReport.reportError(e, "Run Cmd: IOException - check permissions");
	    		if (!prt) Out.prt("Cmd: " + cmd);
	    		exitVal=-2;
    	}
    	catch (Exception e) { 
    		ErrorReport.reportError(e, "Run Cmd: command failed");
    		if (!prt) Out.prt("Cmd: " + cmd);
    		exitVal=-1;
    	}
    	return exitVal;
	}
	/*******************************************************************/
	public int runCmdStdin(String cmd, String dir, Vector <File> stdin) {
	 	int exitVal=0;
	    	try {
	    		String[] args = cmd.split("\\s+");
	    		File d = (dir==null) ? null : new File(dir);
	    		Process p = Runtime.getRuntime().exec(args, null, d);
	    		
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
