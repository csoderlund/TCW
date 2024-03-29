package util.file;

import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

import java.text.SimpleDateFormat;

import java.io.*;

public class FileHelpers 
{
	// CAS303
	public static String getUserDir() {
		return System.getProperty("user.dir");
	}
	public static String getOsArch() {
		return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
	}
	public static boolean isMac() {
		return System.getProperty("os.name").toLowerCase().contains("mac");
	}
	public static String getExtDir() {
		if (isMac()) return Globalx.macDir;
		return Globalx.lintelDir;
	}
	static public String getFileSize(String fileName) {// CAS315
		try {
			File logFile = new File (fileName);
			long fileSize = logFile.length();
			return getSize(fileSize);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Get file size"); return "";}
	}
	static public String getSize(long fileSize) { // CAS315 change %.0f to %.1f
		String size="";
		
		if (fileSize>=1000000000) {
			size = String.format("%.1fGb", (float) ((double)fileSize)/1000000000.0);
		}
		else if (fileSize>=1000000) {
			size = String.format("%.1fMb", (float) ((double)fileSize)/1000000.0);
		}
		else if (fileSize>=1000) {
			size = String.format("%.1fkb", (float) ((double)fileSize)/1000.0);
		}
		else size = String.format("%db", (int) fileSize);
		
		return size;
	}
	static public boolean yesNo(String question)
	{
		BufferedReader inLine = new BufferedReader(new InputStreamReader(System.in));

		System.err.print(question + " (y/n)? "); 
		try
		{
			String resp = inLine.readLine();
			if (resp.equals("y"))
				return true;
			else if (resp.equals("n"))
				return false; 
			else
			{
				System.err.println("Sorry, could not understand the response, please try again:");
				System.err.print(question + " (y/n)? "); 
				resp = inLine.readLine();
				if (resp.equals("y"))
					return true;
				else if (resp.equals("n"))
					return false; 
				else
				{
					System.err.println("Sorry, could not understand the response, exiting.");
					System.exit(0);
					// Have to just exit since returning "n" won't necessarily cause an exit
					return false;
				}
			}

		} catch (Exception e){
			return false;
		}
	}

	/***********************************************************
	 * Directory
	 */
	static public boolean createDir(String dir) {
		File nDir = new File(dir);
		if (nDir.exists()) {
			System.err.println("+++ Directory exists '" +  nDir.getAbsolutePath() + "'.");
			return false;
		}
		else {
			if (!nDir.mkdir()) {
				System.err.println("*** Failed to create directory '" + nDir.getAbsolutePath() + "'.");
				return false;
			}
		}	
		return true;
	}
	public static boolean existDir(String strPath) {
		if (strPath == null) return false;
	   	File path = new File(strPath);
	   	if (!path.exists()) return false;
    	    if (!path.isDirectory() ) return false;
	    return true;
	}
	
	public static boolean deleteDir (String strPath) {
		if (strPath == null) return false;
	   	File path = new File(strPath);
	   	return deleteDir(path);
	}
	// The recurse on this does not work. Use deleteDirTrace(File)
	public static boolean deleteDir (File path) {
		if ( !(path.exists()) && path.isDirectory() ) return false;
	     
		try {
    	    List <File> files = getFileListing(path.toString());
    	    
    	    for(int i=0; i<files.size(); i++) {
		    	   File f = files.get(i);
		    	   if(f.isDirectory()) deleteDir(f.getName());
		    	   else f.delete();
		    }
		    path.delete();
		}
		catch (Exception e) { return false;}
	    return true;
	}
	public static boolean deleteDirTrace(String strPath) {
		if (strPath == null) return false;
	   	
	   	return deleteDirTrace(new File(strPath));
	}
	public static boolean deleteDirTrace(File f) {
		boolean retVal = true;
		if (f.isDirectory()) {
			System.out.println("   Deleting  " + f.getAbsolutePath());
			for (File subFile : f.listFiles())
				retVal = retVal && deleteDirTrace(subFile);
		}
		boolean delResult = f.delete();
		if (!delResult) System.out.println("***Failed to delete " + f.getAbsolutePath());
		
		return retVal && delResult;
	}

	public static void clearDir(File d)
	{
		if (d.isDirectory())
		{
			for (File f : d.listFiles())
			{
				if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) 
				{
					clearDir(f);
				}
				f.delete();
			}
		}
	}
	public static void clearDir(String dir)
	{
		File d = new File(dir);
		clearDir(d);
	}
    public static List<File> getFileListing(String aStartingDir) throws FileNotFoundException 
    {
    		File startingDir = new File(aStartingDir);
    	
		if (!startingDir.exists()) 
			throw new FileNotFoundException("Directory does not exist: " + startingDir);
		
		if (!startingDir.isDirectory()) 
			throw new IllegalArgumentException("Is not a directory: " + startingDir);
		
		if (!startingDir.canRead()) 
			throw new IllegalArgumentException("Directory cannot be read: " + startingDir);

		List<File> result = new ArrayList<File>();
		
		File[] filesAndDirs = startingDir.listFiles();
		List<File> filesDirs = Arrays.asList(filesAndDirs);
		for(File file : filesDirs) {
			result.add(file); 
		}
		Collections.sort(result);
		return result;
    }
    
	public static boolean copyDir(File src, File dest, String [] ignore) {
		try {
	    	if (src.isDirectory()) {
	    		if(!dest.exists()) {//if directory not exists, create it
	    		   dest.mkdir();
	    		   System.out.println("Directory copied from " + src + "  to " + dest);
	    		}

	    		String files[] = src.list();
	    		for (String file : files) {
	    		   File srcFile = new File(src, file);
	    		   
	    		   boolean copy=true;
	    		   if (ignore!=null) {
	    			   if (srcFile.isDirectory()) {
		    			for (int i=0; i<ignore.length && copy; i++) 
		    				if (file.equals(ignore[i])) {
		    					Out.prt(">>> Ignoring TCW directory " + ignore[i]);
		    					copy=false;
		    				}
	    			   }
		    	   }
	    		   if (copy) {
	    			   File destFile = new File(dest, file);
	    			   copyDir(srcFile,destFile, null);//recursive copy
	    		   }
	    		}
	    		return true;
	    	} 
	    	else {
	    		//if file, then copy it
	    		InputStream in = new FileInputStream(src);
    	        OutputStream out = new FileOutputStream(dest);

    	        byte[] buffer = new byte[1024];

    	        int length;
    	        //copy the file content in bytesto support all file types
    	        while ((length = in.read(buffer)) > 0){
    	        		out.write(buffer, 0, length);
    	        }

    	        in.close();
    	        out.close();
    	        System.out.println("File copied from " + src + " to " + dest);
    	        return true;
	    	}
		}
	    catch (Exception e) {ErrorReport.prtReport(e, "Copy " + src + " to " + dest); return false;}
	}
	
	/************************************************************
	 * File
	 */
	static public File getFile(String projPath, String file) {
		try {
			File f = new File(file);
			if (f.exists()) return f;
			
			String xfile = projPath + "/" + file;
			f = new File(xfile);
			if (f.exists()) return f;

			return null;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking file " + file);}
		return null;
	}
	static public File getFile(String file) {
		try {
			File f = new File(file);
			if (!f.exists()) return null;
			return f;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking file " + file);}
		return null;
	}
	public static String getFileDate(String fileName) {
		File f = new File(fileName);
	   	if  (!(f.isFile() && f.exists())) return null;
	   	
		long d = f.lastModified();
	   	Date date = new Date(d);
	   	SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
	   	return fmt.format(date);
	}
	
	public static String getFileDateTime(String fileName) {
		File f = new File(fileName);
	   	if  (!(f.isFile() && f.exists())) return null;
	   	
		long d = f.lastModified();
	   	Date date = new Date(d);
	   	return date.toString();
	}
	
	public static void copyFile( File in, File out ) throws Exception 
	{
		 FileInputStream inFile = new FileInputStream(in);
	     FileChannel sourceChannel =   inFile.getChannel();
	     
	     FileOutputStream outFile = new FileOutputStream(out);
	     FileChannel destinationChannel = outFile.getChannel();
	     sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);

	     inFile.close(); sourceChannel.close();
	     outFile.close(); destinationChannel.close();
	}
	
    public static boolean fileExists(String filepath)
    {
	    	if (filepath == null) return false;
	    	File f = new File(filepath);
	    	return f.exists() && f.isFile();
    }
    public static boolean fileExec(String filepath)
    {
	    	if (filepath == null) return false;
	    	File f = new File(filepath);
	    	return f.exists() && f.isFile() && f.canExecute();
    }
    public static boolean tryExec(String pgm, boolean prt) 
    {
    	try {
    		Process p = Runtime.getRuntime().exec(pgm);
    		p.waitFor();
    		return true;
    	}
    	catch(Exception e){
    		if (prt) Out.PrtError("Cannot find " + pgm + " in user's path");
    		return false;
    	}
    }
    /************************************************************
     * String ops on paths
     */
    public static boolean isAbsolutePath(String filepath){
    	if (filepath == null) return false;
    	File f = new File(filepath);
    	return f.isAbsolute();
    }
    
    public static String addAbsolutePath(String absPath, String filepath){
		if (filepath == null) return null;
		File f = new File(filepath);
		if (f.isAbsolute() || filepath.equals(""))
			return filepath;
	
		return absPath + "/" + filepath;
    }
    /******************************************************
     * Remove part/all of prefix path
     */ 
	public static String removeRootPath(String file) { // Uses in many annotater routines
		String path="";
		try {
			if (file==null || file=="") return "";
			String cur = System.getProperty("user.dir");
			path = file.replace(cur, "");
			if (path.startsWith("/")) path = path.substring(1); // CAS316 
		}
		catch (Exception e) {ErrorReport.prtReport(e, "removing current path from " + file); }
		return path;
	}
	public static String removePath(String file) {
		return file.substring(file.lastIndexOf("/"));
	}
	/***************************************************************
	 * Open a file to write as zipped
	 */
	public static BufferedWriter createGZIP(String file) {
		try {
			return new BufferedWriter(
                    new OutputStreamWriter(
                            new GZIPOutputStream(new FileOutputStream(file))));
		}
		catch (Exception e) {ErrorReport.die(e, "Cannot write file " + file);}
		return null;
	}
	
	public static BufferedReader openGZIP(String file) {
		try {
			if (!file.endsWith(".gz")) {
				File f = new File (file);
				if (f.exists())
	    				return new BufferedReader ( new FileReader (f));
				else {
					f = new File (file + ".gz");
					if (f.exists()) file = file + ".gz";
					else ErrorReport.die("Cannot open file " + file);
				}
			}
			if (file.endsWith(".gz")) {
				FileInputStream fin = new FileInputStream(file);
				GZIPInputStream gzis = new GZIPInputStream(fin);
				InputStreamReader xover = new InputStreamReader(gzis);
				return new BufferedReader(xover);
			}
			else ErrorReport.die("Do not recognize file suffix: " + file);
		}
		catch (Exception e) {
			String f = removeRootPath(file);
	    		Out.PrtError("Cannot open file " + f); 
	    }
		return null;
	}
	
	// CAS303 Move external_osx to ext/mac and external to ext/lintel
	// CAS338 changed this to untar Ext/linux.tar.gz or mac.tar.gz; however, mac loses its executable permissions
	static public boolean makeExt() {
		try {
			String newDir = getExtDir(); // ext/mac or ext/lintel
			if (existDir(newDir)) return true;
			
			Out.PrtWarn("Directory does not exist: " + newDir);
			
			String tarFile = newDir + ".tar.gz";
			if (!fileExists(tarFile)) {
				Out.PrtWarn("External tar file does not exist: " + tarFile);
				return false;
			}
			
			Out.PrtSpMsg(1, "Untarring external programs in " + tarFile);
			if (systemCall("tar xf " + tarFile)) {
				String sub = newDir.replace(Globalx.extDir + "/", "");
				if (systemCall("mv " + sub + " " + Globalx.extDir)) return true;
				else Out.PrtWarn("Cannot move " + sub + " to " + Globalx.extDir);
			}
			
			Out.PrtWarn("Cannot access external programs (e.g. Blast, Diamond, MAFFT, etc");
			return false;
		}
		catch (Exception e){
			ErrorReport.prtReport(e, "Could not untar external programs in /Ext");
			return false;
		}
	}
	static public boolean systemCall(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			
			String line;
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = input.readLine()) != null) System.out.println(line);
			input.close();
			
			p.waitFor();
			
			if (p.exitValue() != 0) {
				Out.PrtError(cmd + " failed with exit value = " + p.exitValue());
				return false;
			}
			return true;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Could execute '" + cmd + "'");
			return false;
		}
	}
    public FileHelpers () { }; // Don't instantiate
}
