package util.methods;

import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.text.SimpleDateFormat;

import java.io.*;

public class FileHelpers 
{
	static public String getSize(long fileSize) {
		String size="";
		
		if (fileSize>=1000000000) {
			size = String.format("%.0fGb", (float) ((double)fileSize)/1000000000.0);
		}
		else if (fileSize>=1000000) {
			size = String.format("%.0fMb", (float) ((double)fileSize)/1000000.0);
		}
		else if (fileSize>=1000) {
			size = String.format("%.0fkb", (float) ((double)fileSize)/1000.0);
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
	     FileChannel sourceChannel = new
	          FileInputStream(in).getChannel();
	     FileChannel destinationChannel = new
	          FileOutputStream(out).getChannel();
	     sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);

	     sourceChannel.close();
	     destinationChannel.close();
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
	static void clearDir(String dir)
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
    
    public static boolean fileExists(String filepath)
    {
	    	if (filepath == null) return false;
	    	File f = new File(filepath);
	    	return f.isFile() && f.exists();
    }
    
    public static boolean isAbsolutePath(String filepath)
    {
	    	if (filepath == null) return false;
	    	File f = new File(filepath);
	    	return f.isAbsolute();
    }
    
    public static String addAbsolutePath(String absPath, String filepath)
    {
    		if (filepath == null) return null;
    		File f = new File(filepath);
    		if (f.isAbsolute() || filepath.equals(""))
    			return filepath;
    	
    		return absPath + "/" + filepath;
    }
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
	/*******************************************************
	 * @param src - copy contents of this directory
	 * @param dest - to this directory
	 * @param ignore - but ignore these directories directly under src
	 */
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
		    	}else{
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
	
	public static String removeCurrentPath(String file) {
		String path="";
		try {
			if (file==null || file=="") return "";
			String cur = System.getProperty("user.dir");
			path = file.replace(cur, "");
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
			String f = removeCurrentPath(file);
	    		Out.PrtError("Cannot open file " + f); 
	    }
		return null;
	}
	// Written to merge /libraries with /projects
	static public boolean mergeDir(String src, String dest, boolean trace) { 
		try {
			// check top directories
			if (!src.endsWith("/")) src += "/";
			if (!dest.endsWith("/")) dest += "/";
			
			File srcDir = new File(src);
			if (!srcDir.exists()) return true;
	
			if (!srcDir.isDirectory()) {
				Out.PrtWarn(src + " is not a directory");
				return true;
			}
			Out.prt("");
			Out.prt("TCW v2.10: The " + src + " directory is now merged with " + dest);
			Out.prt("All files in " + src + " projects should be in their corresponding directory of " + dest); 
			boolean rc = Out.yesNo("Can TCW merge the directories for you");
			if (!rc) {
				Out.prt("You must merge " + src + " with " + dest);
				return false;
			}
			
			File destDir = new File(dest);
			if (!destDir.isDirectory()) {
				Out.PrtWarn(dest + " is a file - cannot move " + src + " to it");
				return true;
			}
			if (!destDir.exists()) {
				Out.prt(src + " renamed to " + dest);
				srcDir.renameTo(destDir);
				return true;
			}
			
			// move src/* to dest/*
			int cntCantMove=0, cntMove=0;
			for (File srcSub : srcDir.listFiles())
			{
				String srcSubName = srcSub.getName();
				if (srcSubName.equals(".") || srcSubName.equals("..")) continue;
				if (!srcSub.isDirectory()) {
					Out.PrtWarn(srcSub.getAbsoluteFile() + " is not a directory -- cannot move");
					cntCantMove++;
					continue;
				}
				File destSub = new File(dest + srcSubName);
				if (destSub.exists() && !destSub.isDirectory()) {
					Out.PrtWarn(destSub.getAbsoluteFile() + " is not a directory -- cannot move file to it");
					cntCantMove++;
					continue;
				}
				
				if (!destSub.exists()) {
					if (trace) Out.prt(srcSub.getAbsoluteFile() + " move whole directory ");
					srcSub.renameTo(destSub);
					cntMove++;
					continue;
				}
				
				// copy all src/x/* to dest/x 
				if (trace) Out.prt(srcSub.getAbsoluteFile() + " move contents ");
				
				int cntX=0;
				for (File srcFile : srcSub.listFiles())
				{
					String fname = srcFile.getName();
					if (fname.equals(".") || fname.equals("..")) continue;
					
					File destFile = new File(dest + "/" + srcSubName + "/" + fname);
					
					if (!destFile.exists()) {
						//if (trace) Out.prt("       move  " + fname);
						srcFile.renameTo(destFile);
						cntMove++;
					}
					else {
						Out.PrtWarn(destFile.getAbsoluteFile() + " exists -- cannot move");
						cntX++;
					}
				}
				if (cntX==0) Out.prt(srcSub.getAbsoluteFile() + " is empty -- can delete");
				else cntCantMove++;
			}
			
			File dir = new File(srcDir.getName() + "OLD");
			Out.prt(src + " renamed to " + dir.getName());
			srcDir.renameTo(dir);
			Out.PrtCntMsg(cntCantMove, " could not be moved");
			Out.PrtCntMsg(cntMove, " moved");
			Out.PrtSpMsg(0, "Complete move");
			
			return true;
		}
		catch (Exception e){e.printStackTrace(); return false;}
	}
    public FileHelpers () { }; // Don't instantiate
}
