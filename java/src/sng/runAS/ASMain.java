package sng.runAS;

/**********************************************
 * Main method for newUPver and newGOver rewritten in Java
 * Contains shared static methods:
 * -- method to download a file from the UniProt or GO URL
 * -- method to run a command
 * -- methods to print trace info
 */
import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;


import sng.database.Version;
import util.methods.ErrorReport;
import util.methods.Out;

public class ASMain {
	public static boolean test=false, isDemo=false;
	private static final String annoDir = "projects/DBfasta";
	private static final String logFile = "runAS.log";
	
	public static void main(String[] args) {
		Out.PrtDateMsg("Annotation Setup " + Version.TCWhead );
		Out.createLogFile(annoDir, logFile, true); // write to stdout
		
		if (hasArg(args, "-h") || hasArg(args, "-help") || hasArg(args, "?")) {
			System.err.println("Usage: runAS [id]");
			System.err.println("   -d: use for demo");
			return;
		}
		if (hasArg(args, "-d")) {
			isDemo=true;
			System.out.println("Running in demo mode");
		}
		if (hasArg(args, "-t")) {
			test=true;
			System.out.println("!!!!!!!!!!! Test mode -- read .gz files from DBfasta/dats");
		}
		
		new ASFrame();
	}
	static boolean hasArg(String [] args, String arg) {
		for (int i=0; i<args.length; i++)
			if (args[i].equals(arg)) return true;
		return false;
	}
	/**
	 *  read file with curl from UniProt or Gene Ontology
	 */
	public static boolean ioURL(String url, String inFile, String outPath) {
		Out.PrtSpMsg(2,"Downloading " + inFile);
		long time = Out.getTime();
		
	    try {
	    	if (new File(outPath).exists()) {
    			Out.PrtSpMsg(2, "File exists " + outPath);
    			return true;
    		}
	    	RunCmd rcd = new RunCmd();
// Does curl exist?
	    	int exitVal= rcd.run("curl -help", null);
	    	if (exitVal!=0) {
	    		Out.PrtWarn("curl does not exist on this system");
	    		int ret = JOptionPane.showOptionDialog(null, 
		    	"curl does not exist on your computer (see Help).\n" +
		    	"However, TCW can download manually, though its much slower.\n" +
		    	"\nContinue?",
		    	"Download from UniProt", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		    	if (ret == JOptionPane.NO_OPTION) 
		    		return false;
		    	else 
		    		return ioURLManual(url, inFile, outPath);
	    	}
		    	
// download
		    String cmd = "curl " + url + inFile + " -o " + outPath;
		    System.err.println(cmd);
		    exitVal = rcd.run(cmd, null); // current directory, print stderr
		    	 
	    	if (exitVal!=0) {
	    		System.err.println("\ncurl failed " + exitVal);
	    		if (exitVal==6)  System.err.println("6: Make sure you have internet access");
	    		if (exitVal==13) System.err.println("13: curl failed to get a sensible result back from the server as a response to either a PASV or a EPSV command. The server is flawed. ");
	    		return false;
	    	}
		    Out.PrtSpMsgTime(2, "curl complete " + outPath, time);
		    return true;
	    } 
	    catch (Exception e) { ErrorReport.prtReport(e, "Exception");}
		return false;
	}
	/**
	 *  read file manually from UniProt or Gene Ontology
	 */
	public static boolean ioURLManual(String url, String inFile, String outPath) {
		Out.PrtSpMsg(2,"Manual Downloading " + inFile);
		long time = Out.getTime();
		
	    try {
	    	//URLConnection con = url.openConnection();
	    	//BufferedInputStream in =     new BufferedInputStream(con.getInputStream());
		    URL u = new URL(url + inFile);
		    InputStream os = u.openStream();         
		    BufferedInputStream in = new BufferedInputStream(os);
		    FileOutputStream out = new FileOutputStream(outPath);
	         
		    int i = 0, cnt=0;
		    byte[] bytesIn = new byte[3000000];
		    while ((i = in.read(bytesIn)) >= 0) {
		        out.write(bytesIn, 0, i);
		        cnt++;
	    		if (cnt%1000==0) System.out.print("      Wrote " + cnt + "\r");
		    }
		    in.close(); out.close();
		    Out.PrtSpMsgTime(2, cnt + " gzipped lines written to " + outPath, time);
		    
		    return true;
	    } 
	    catch (IOException e) { ErrorReport.prtReport(e, "IO Exception"); }
	    catch (Exception e) { ErrorReport.prtReport(e, "Exception"); }
		return false;
	}
}
