package sng.assem;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.TreeMap;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.StringBuilder;

import sng.assem.enums.*;
import util.database.DBConn;
import util.methods.TCWprops;

// Container for various static helper routines. 
// Needs to be initialized once by calling constructor. 

public class Utils
{
	static File mStatusDir;
	static Date mTimeStart;
	static Date mIntTimeStart;
	static Map<Integer,Date> mIntTimeStartThread;
	static Map<String,Long> mCmdTimes = null;
	static Map<String,Integer> mCmdCounts = null;
	static TCWprops mProps = null;
	static long mMaxMemMB = 0;
	
	public Utils()
	{
		if (mCmdTimes == null)
		{
			mCmdTimes = Collections.synchronizedMap(new TreeMap<String,Long>());
			mCmdCounts = Collections.synchronizedMap(new TreeMap<String,Integer>());
			mIntTimeStartThread = Collections.synchronizedMap(new TreeMap<Integer,Date>());
		}
	}
	static void timeSummary(LogLevel lvl)
	{
		Log.head("Timer summary", lvl);
		Log.columns(20,lvl,"label","# usages","total time (msec)","time per");
		for (String cmd : mCmdTimes.keySet())
		{
			int nuse = mCmdCounts.get(cmd);
			Long time = mCmdTimes.get(cmd);
			Long timeper = time/nuse;
			Log.columns(20,lvl,cmd,nuse,time,timeper);
		}
	}
	
	static public String strVectorJoin(java.util.Vector<String> sa, String delim)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < sa.size(); i++)
		{
			out.append(sa.get(i));
			if (i < sa.size() - 1)
				out.append(delim);
		}
		return out.toString();
	}
	static String strIntVectorJoin(Vector<Integer> sa, String delim)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < sa.size(); i++)
		{
			out.append(sa.get(i).toString());
			if (i < sa.size() - 1)
				out.append(delim);
		}
		return out.toString();
	}

	static String join(Collection <String> s, String delimiter)
	{
		StringBuilder buffer = new StringBuilder();
		Iterator <String> iter = s.iterator();
		while (iter.hasNext())
		{
			buffer.append(iter.next().toString());
			if (iter.hasNext())
				buffer.append(delimiter);
		}
		return buffer.toString();
	}
	static public String join(Object[] s, String delimiter)
	{
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < s.length; i++)
		{
			buffer.append(s[i].toString());
			if (i < s.length - 1)
			{
				buffer.append(delimiter);
			}
		}

		return buffer.toString();
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

		} catch (Exception e) {return false;}
	}

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
				mCmdTimes.put(cmd, 0L); // CAS303 new Long(0) replaced with 0L
			}
			mCmdCounts.put(cmd,1 + mCmdCounts.get(cmd));
		}
	
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
			Log.msg(errStr.toString(), LogLevel.Detail);	
		}
		
		int ev = p.exitValue();
		p.getInputStream().close();
		p.getOutputStream().close();
		p.getErrorStream().close();
		p.destroy();
		return ev;
	}

	// Note problem, Java doesn't see unix links as existing, when viewed over NFS
	 static void checkCreateDir(File dir)
	{
		if (dir.exists() && !dir.isDirectory())
		{
			Log.die("Please remove file " + dir.getAbsolutePath()
					+ " as TCW needs to create a directory at this path");
		}
		if (!dir.exists())
		{
			// do this in a thread safe manner
			dir.mkdir(); 
			if (!dir.exists())
			{
				Log.die("Unable to create directory " + dir.getAbsolutePath());
			}
		}
	}

	// Return true if the check file is at least as new as the ref file.
	// Currently, I have removed all uses of this and rely
	// on the status marker files in the 'status' directory instead.
	static boolean firstMoreRecent(String checkPath, String refPath)
			throws Exception
	{
		File r = new File(refPath);
		if (!r.exists())
		{
			// if no ref file, return true as we assume that for this instance
			// the check is not applicable (e.g., it's checking one of the paired files when
			// there are only singles)
			return true;
		}
		
		File c = new File(checkPath);
		if (!c.isFile())
		{
			Log.msg(checkPath + " does not exist",LogLevel.Detail);
			return false;
		}
		
		if (c.lastModified() >= r.lastModified())
		{
			return true;
		}
		Log.msg(refPath + " is more recent than " + checkPath,LogLevel.Detail);
		return false;
	}
	static void setStatusDir(File dir)
	{
		mStatusDir = dir;
		Log.msg("Status dir:" + mStatusDir.getAbsolutePath(), LogLevel.Detail);
	}
	static File statusFile(String tag)
	{
		 String name = tag + "_DONE";
		 return new File(mStatusDir,name);
	}
	static void noop()
	{
		// no-op for inserting debug breakpoints
	}
	static void recordTime(String stage, int aid, DBConn db) throws Exception
	{
		db.executeUpdate("insert into ASM_assemtime (aid,stage,time_start) values(" + aid + ",'" +stage + "',NOW())");	
	}
	static void writeStatusFile(String tag) throws Exception
	{
		File f = statusFile(tag);
		if (f.exists()) f.delete();
		f.createNewFile();
	}
	static void deleteStatusFile(String tag) throws Exception
	{
		File f = statusFile(tag);
		if (f.exists()) f.delete();
	}
	// check both the status marker file and the expected output file date
	static boolean checkUpToDate(String tag, String checkPath, String refPath) throws Exception
	{
		File f = statusFile(tag);
		if (f.exists())
		{
			if (checkPath != null && !checkPath.equals("") && !firstMoreRecent(checkPath,refPath))
			{
				f.delete();
				return false;
			}
			return true;
		}
		return false;
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
	public static void deleteDir(File d)
	{
		clearDir(d);
		d.delete();
	}
	static void checkExitValue(int val, String cmd)
	{
		if (val != 0)
		{
			Log.newLine(LogLevel.Basic);
			Log.msg("Command failed with exit value " + val + ":\n" + cmd,LogLevel.Basic);
		}
	}
	// Thread safe timer functions, with results summed at the end
	// A thread can use two at once by using nThread, and -nThread-1 (if nThread can be 0)
	static public void intTimerStart(int nThread)
	{
		mIntTimeStartThread.put(nThread, new Date());
	}	

	static synchronized public Long intTimerEnd(int nThread, String cmd) throws Exception
	{
		Date now = new Date();
		if (!mIntTimeStartThread.containsKey(nThread))
		{
			throw(new Exception("timer hasn't been started for thread " + nThread));
		}
		Long elapsed = now.getTime() - mIntTimeStartThread.get(nThread).getTime();
		//elapsed /= 1000;
		if (!mCmdTimes.containsKey(cmd)) mCmdTimes.put(cmd, 0L);
		if (!mCmdCounts.containsKey(cmd)) mCmdCounts.put(cmd, 0);
		mCmdTimes.put(cmd, elapsed + mCmdTimes.get(cmd));
		mCmdCounts.put(cmd, 1 + mCmdCounts.get(cmd));
		return elapsed;
	}		
	
	
	static String reverseComplement(String in)
	{
		in = (new StringBuffer(in)).reverse().toString().toUpperCase();
		in = in.replace('A', 't');
		in = in.replace('G', 'c');
		in = in.replace('C', 'g');
		in = in.replace('T', 'a');
		return in.toLowerCase();
	}

	static void memCheck()
	{
		System.gc();
		long total = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		long mb = total/1000000;
		long kb = (total % 1000000)/1000;
		if (mb > mMaxMemMB)
		{
			mMaxMemMB = mb;
		}
		Log.head("Memory:" + mb + " Mb, " + kb + " Kb",LogLevel.Detail);
		long free = Runtime.getRuntime().freeMemory();
		mb = free/1000000;
	}
	
	static boolean isDebug() throws Exception
	{
		return mProps.getProperty("DEBUG").equals("1");
	}
	static void appendToFile(String cmd, File dir, String filename) throws Exception
	{
		File file = new File(dir,filename);
		if (!file.exists()) file.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.append(cmd + "\n");
		bw.flush();
		bw.close();
	}
	static boolean validName(String name)
	{
		Pattern p = Pattern.compile("\\w+");
		Matcher m = p.matcher(name);
		boolean ret = m.matches();
		return ret;
	}
	
	// Save off a command string to the DB, so we have an unambiguous record of what is being run with what params
	static void recordCmd(int aid, String desc, String cmd, DBConn db) throws Exception
	{
		db.executeQuery("lock tables ASM_cmdlist write");
		db.executeUpdate("insert ignore into ASM_cmdlist (aid,descr,cmdstr) values('" + aid + "','" + desc + "','" + cmd + "')");
		db.executeQuery("unlock tables");
	}
	static int finalTC(DBConn db, int aid) throws Exception
	{
		ResultSet rs = db.executeQuery("select tcid from ASM_tc_iter where aid=" + aid + " and tctype='final'");
		int tcid = 0;
		if (rs.first())
			tcid = rs.getInt("tcid");
		else
			Log.die("The assembly is not completed!");
		return tcid;
	
	}
	static void singleLineMsg(String msg)
	{
		System.err.print("\t   " + msg + "...                                  \r");	
	}
	static void termOut(String msg)
	{
		System.err.println("\t" + msg);	
	}
	static int getPercent(int num, int denom)
	{
		if (denom == 0) return 0;
		return (100*num)/denom;		
	}
	static boolean notEmpty(String x) {
		return (x!=null && !x.contentEquals(""));
	}
	public static void snpThresholds(double erate,double minScore, String label) throws Exception
	{
		Log.columns(10,LogLevel.Detail,"depth",label);
		int curThresh = 1;
		int prevThresh = curThresh;
		double errRate = erate/3;
		for (int depth = 2; depth <= 10000; depth++)
		{
			for (; curThresh < depth; curThresh++)
			{
				double score = Utils.cumulBinom(depth, errRate, curThresh);	
				if (score <= minScore)
				{
					if (curThresh > prevThresh)
					{
						prevThresh = curThresh;
						Log.columns(10, LogLevel.Detail, depth,curThresh);
					}
					break;
				}
			}
		}
	}		

	static String getEnv(String key)
	{
		if (System.getenv().containsKey(key))
		{
			return System.getenv(key);
		}
		return "";
	}
	
	static double cumulBinom(int depth, double perr, int nsnp)
	{
		if (perr <= 0 || perr >= 1) return 1.0; // should not happen as this condition checked elsewhere. 
			
		// Sum the cumulative binomial series until convergence is achieved
		double sum = 0.0;
		double pinv = 1 - perr;
		double pinv_log = Math.log(pinv);
		double pinvstart = Math.exp(pinv_log*(depth - nsnp));
		if (nsnp > 0)
		{
			double bstart = 1.0;
			// compute the NCn r^n part
			for (int i = 1; i <= nsnp; i++)
			{
			 	bstart *= (perr*((double)(depth - i + 1))/((double)i));
			}
			bstart *= pinvstart; // the (1-p)^(N-n) part
			double bprev = bstart;
			sum = bstart;
			// Successive terms come by multiplying and dividing by integers, perr, and (1-perr)
			double prat = perr/pinv;
			for (int i = nsnp + 1; i <= depth; i++)
			{
				double bcur = 	bprev*(prat*((double)(depth - i + 1))/((double)i));
				sum += bcur;
				if (bcur/bprev < .1) break; // this ratio is strictly decreasing, hence remainder is dominated by 
											// geometric series with r=.1, hence less than .1/(1-.1) ~ .11 times current term  
				bprev = bcur;
			}
		}
		
		return sum;
	}
	public static boolean hasOption(String[] args, String name)
	{
		for (int i = 0;  i < args.length;  i++)
			if (args[i].equals(name)) 
				return true;
		return false;
	}
	
}