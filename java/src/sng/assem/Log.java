package sng.assem;

import java.io.File;
import java.io.FileWriter;
import java.util.Formatter;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;

import sng.assem.enums.LogAction;
import sng.assem.enums.LogLevel;
import util.methods.ErrorReport;
import util.methods.TimeHelpers;


// Logging helper class. 
// Initialize it once to set the static members, and
// then all methods can be called statically, so
// a log object does not need to be passed around. 

// Most calls take a LogLevel enum, which tells which log to 
// put the message in, and whether to display it to the screen.
// (These actions are set up during initialization, using the
// LogAction enum). 


public class Log
{
	static public FileWriter logFW = null;
	static public File logFile = null;
	static private Formatter logFormat = null;
	static public String mLogPath = "";

	static public FileWriter dbgFW = null;
	static public File dbgFile = null;
	static private Formatter dbgFormat = null;
	static public String mDbgPath = "";
	static public String mLastMsg;
	
	static public HashMap<LogLevel,HashSet<LogAction>> mActions = null;

	public Log()
	{
		mActions = new HashMap<LogLevel,HashSet<LogAction>>();		
		for (LogLevel lvl : LogLevel.values())
		{
			mActions.put(lvl, new HashSet<LogAction>());
		}
	}
	private Log(FileWriter fw)
	{
		this();
		logFW = fw;
		logFormat = new Formatter(logFW);
	}

	public Log(File file) throws Exception
	{
		this(new FileWriter(file, true));
		logFile = file;
		System.err.println("Log File:" + file.getAbsolutePath()); 
	}
	public static void setDebugLog(File file) throws Exception
	{
		dbgFW = new FileWriter(file,false); 
		dbgFile = file;
		dbgFormat = new Formatter(dbgFW);
	}
	public static void addLogAction(LogLevel lvl, LogAction action)
	{
		if (!mActions.containsKey(lvl)) 
		{
			mActions.put(lvl, new HashSet<LogAction>());
		}
		mActions.get(lvl).add(action);
	}
	public static void exception(Exception e)
	{   
		System.err.println(e.getMessage() + " (see sTCW.error1.log)");
		try
		{
			if (dbgFW != null)
			{
				for (StackTraceElement ste : e.getStackTrace())
				{
					dbgFW.write(ste.toString() + "\n");
					dbgFW.flush();
				}
			}
			if (Utils.isDebug()) e.printStackTrace();
			FileWriter fw = new FileWriter(new File("sTCW.error1.log"));
			fw.write(e.getMessage());
			for (StackTraceElement elt : e.getStackTrace())
			{
				fw.write(elt.toString() + "\n");
			}
			fw.flush();
			fw.close();			
		}
		catch (Exception foo)
		{
		}
	}
	public static String head(String s, LogLevel lvl) 
	{		
		return msg("\n>>>" + s + " " + TimeHelpers.getDate() + "\n",lvl);
	}
	public static String head(String s) 
	{		
		return msg("\n>>>" + s + " " + TimeHelpers.getDate() + "\n", LogLevel.Basic);
	}

	public static String indentMsg(String s, LogLevel lvl)
	{
		return msg("\t" + s,lvl);	
	}
	public static synchronized void finish() {
		try {
			System.err.print("-------------------------------------------------------------\n");
			logFW.write("-------------------------------------------------------------\n");
			logFW.flush();
		}
		catch (Exception e) {}
	}
	// Basic writes to Terminal and Log; Detail also writes to detail log
	public static synchronized String msg(String s, LogLevel lvl) 
	{
		if (mActions == null)
		{
			System.err.println(s);
			mLastMsg = s;
			return mLastMsg;
		}
		try
		{
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.println(s);
				System.err.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFW.write(s + "\n");
				logFW.flush();
			}
			if (mActions.get(lvl).contains(LogLevel.Basic)) 
			{
				logFW.write(s + "\n");
				logFW.flush();
			}
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFW.write(s + "\n");
				dbgFW.flush();
			}		
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		mLastMsg = s + "\n";;
		return mLastMsg;
	}
	public static String msg(String s)
	{
		return msg(s, LogLevel.Detail);	
	}
	// Indented tables...
	static synchronized String columnsInd(int w, LogLevel lvl, Object... vals) 
	{
		try
		{
			String fmt = "\t";
			for (int i = 1; i <= vals.length; i++)
			{
				fmt += "%-" + w + "s";
			}
			fmt += "\n";
			
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals);
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}	
	static synchronized String columns(int w, LogLevel lvl, Object... vals) 
	{
		try
		{
			String fmt = "";
			for (int i = 1; i <= vals.length; i++)
			{
				fmt += "%-" + w + "s";
			}
			fmt += "\n";
			
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals);
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}
	static String newLine(LogLevel lvl)
	{
		msg("",lvl);	
		return mLastMsg;
	}
	static String columns(int[] ws, LogLevel lvl, Object... vals)
	{
		try
		{
			String fmt = "";
			int nfields = Math.min(ws.length,vals.length);
			for (int i = 0; i < nfields; i++)
			{
				fmt += "%-" + ws[i] + "s";
			}
			fmt += "\n";
			if (mActions.get(lvl).contains(LogAction.Log))
			{
				logFormat.format(fmt, vals);
				logFormat.flush();
			}
	
			if (mActions.get(lvl).contains(LogAction.DebugLog))
			{
				dbgFormat.format(fmt, vals);
				dbgFormat.flush();
			}
			if (mActions.get(lvl).contains(LogAction.Terminal))
			{
				System.err.printf(fmt, vals);
			}
			mLastMsg = String.format(fmt,vals) + "\n";;
		}
		catch(Exception e)
		{
			System.err.println("Error writing to log file - will try to continue");
		}
		return mLastMsg;
	}

	public static void die(String s) 
	{
		try
		{
			msg("Fatal Error: " + s,LogLevel.Basic);
		}
		catch(Exception e)
		{
			System.err.println(s);
			System.exit(-1);
		}
		System.exit(-1);
	}

	public static synchronized void  write(char c)
	{
		System.err.print(c);
	}
	public static void timeStamp()
	{
      	Date date = new java.util.Date();
 		msg("Current Date Time : " + date.toString(),LogLevel.Detail);
	}
}
