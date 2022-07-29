package util.methods;
/** 
 * Time and date methods
 * Use getTime for the starting time to be passed getElapsedTimeStr
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class TimeHelpers 
{
	// called by assembler and annotator
    static public String getDateTime ( )
    {
        Date date=new Date();
        SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss dd-MMM-yy"); 
        return sdf.format(date);
    }
    
    static public String getDateOnly ( )
    {
        Date date=new Date();
        SimpleDateFormat sdf=new SimpleDateFormat("dd-MMM-yy"); 
        return sdf.format(date);
    }
    static public String getTimeOnly ( ) // CAS404 added
    {
        Date date=new Date();
        SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss"); 
        return sdf.format(date);
    }
    static public String getDBDate ( ) // For ASFrame to print the DB date 2018-09-30
    {
        Date date=new Date();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd"); 
        return sdf.format(date);
    }
    static public String convertDate(String date) { // yyyy-dd-mm
   		int first = date.indexOf("-");
		int last = date.lastIndexOf("-");
		String yr = date.substring(0, first);
		yr = yr.substring(2); // remove 20
		String mn = date.substring(first+1, last);
		String dy = date.substring(last+1);
        return dy + "-" + TimeHelpers.getMonth(mn) + "-" + yr;
    }
    static public String getMonth(String mn) {
     	HashMap<String, String> hMap = new HashMap<String, String>();
       	hMap.put("01", "Jan"); hMap.put("02", "Feb"); hMap.put("03", "Mar");
       	hMap.put("04", "Apr"); hMap.put("05", "May"); hMap.put("06", "Jun");
       	hMap.put("07", "Jul"); hMap.put("08", "Aug"); hMap.put("09", "Sep");
       	hMap.put("10", "Oct"); hMap.put("11", "Nov"); hMap.put("12", "Dec");
       	return hMap.get(mn);
    }
    
    static public String longDate(long l) {
    		Date date = new Date(l);
    		return date.toString();
    }
    /***********************************************************
	 * Nanoseconds - This method can only be used to measure elapsed time and is not related to any other notion of system or wall-clock time.
	 */
    static public long getNanoTime () {
    		return System.nanoTime(); 
    }
	static public String getElapsedNanoTime(long t) {
		long et = System.nanoTime()-t;
		long sec = et /1000000000;
		return getTimeStr(sec);
	}
	static public String getTimeStr(long et) {
		long day = 	et/86400; //24*3600
		long time = et%86400;
		long hr =  time/3600;
		
		time %= 3600;
		long min = time/60;
		long sec = time%60;
		
		String str = " ";
		if (day > 0) str += day + "d:";
		if (hr > 0 ) str += hr + "h:";
		str += min + "m:" + sec + "s";
		return str;
	}
	
    /***********************************************
     * Clock time
     */
    static public long getTime () {
		return System.currentTimeMillis(); 
    }
	static public String getElapsedTimeStr(long t) 	{
		long et = System.currentTimeMillis() - t;
		long sec = et/1000L;
		return getTimeStr(sec);
	}
	// AssemMain uses 
	static public String getElapsedTimeStrFromInterval(long et) 	{
		long sec =  et/1000L;
		return getElapsedTimeStr(sec);
	}
	
	static public void printMemoryUsed(String space) {
	    Runtime runtime = Runtime.getRuntime();
	    runtime.gc(); // Run the garbage collector
	    double memory = runtime.totalMemory() - runtime.freeMemory();
	    String mb = String.format("%6.2f", (memory/(1024.0 * 1024.0)));
	    System.out.println(space + "Used memory: " + mb + " megabytes");
	}
	static public String getMemoryUsed() {
		 Runtime runtime = Runtime.getRuntime();
		 runtime.gc(); // Run the garbage collector
		 double memory = runtime.totalMemory() - runtime.freeMemory();
		 double d = 1024*1024;
		 if (memory>d) return String.format("%6dMb", (int) (memory/d));
		 return String.format("%6dkb", (int) (memory/1024));
	}
	static public String getMemoryUsedMb() {
		 Runtime runtime = Runtime.getRuntime();
		 runtime.gc(); // Run the garbage collector
		 double memory = runtime.totalMemory() - runtime.freeMemory();
		 double d = 1024*1024;
		 if (memory>d) return String.format("%dMb", (int) (memory/d));
		 return String.format("%db", (int) memory);
	}
}
