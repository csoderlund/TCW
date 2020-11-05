package util.methods;
// NOTICE: this uses System.err instead of System.out 
//			because the RunCmd of sTCW interferes with System.out
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Vector;

import util.align.AAStatistics;
import util.database.Globalx;

public class Out {
	static boolean debug=true;
	static DecimalFormat df = new DecimalFormat("#,###,###");
	
	static private PrintWriter logFileObj=null; 
	static private String logFileAbsName = null, logFileRelName = null;
	static private boolean bStdout=false;
	
	/****************************************************
    * XXX Open/close log file
    ****************************************************/
	static public void createLogFile(String path, String file, boolean stdout) {
		bStdout=stdout;
		createLogFile(path,file);
	}
    static public void createLogFile(String path, String file) 
	{	  
		if (logFileObj!=null) {
			System.err.println("TCW error: previous log file not closed");
			logFileObj.close();
		}
		String dir = Globalx.logDir;
		String logPath = path +  "/" + dir;

		// Make sure log directory exists
		File logDir = new File(logPath);
		if (!logDir.exists()) {
			System.err.println("Creating project log directory ...");
			if (!logDir.mkdir()) {
				System.err.println("*** Failed to create project log directory '" + 
						logDir.getAbsolutePath() + "'.");
				return;
			}
		}
	    	
		// Create log file
		logFileAbsName = logPath + "/" + file;
		logFileRelName = FileHelpers.removeCurrentPath(logFileAbsName);
		File logFile = new File (logFileAbsName);
		long fileSize = logFile.length();
		
		try {
			if (fileSize>0)
				System.err.println("Trace output to stderr and Log file (append): " + logFileRelName + "   Size: " + FileHelpers.getSize(fileSize));
			else 
				System.err.println("Trace output to stderr and Log file: " + logFileRelName);
			FileWriter out = new FileWriter(logFile.getAbsolutePath(), true); // append
			logFileObj = new PrintWriter(out); 
		}
		catch (Exception e)
        {
            ErrorReport.reportError(e, "Error writing to " + logFile.getAbsoluteFile());
            return;
        }
	}
	static public void close() {
		if (logFileObj==null) return;
		
		Print("-----------------------------------------------------\n");
		logFileObj.close();
		logFileObj=null;
	}
	static public void closeSize() {
		if (logFileObj==null) return;
		
		File logFile = new File (logFileAbsName);
		long fileSize = logFile.length();
		System.err.println("Close Log file: " + logFileRelName + "   Size: " + FileHelpers.getSize(fileSize));
		
		Print("-----------------------------------------------------\n");
		logFileObj.close();
		logFileObj=null;
	}
	static public void Print(String s) {
		if (bStdout) System.out.println(s);
		else System.err.println(s);
	    if (logFileObj != null) { 
	    		logFileObj.println(s);
	        logFileObj.flush();
	    }
	}	
	static public void FileLog(String s) {
		if (logFileObj != null) { 
	    		logFileObj.println(s);
	        logFileObj.flush();
	    }
	}
	// Count first
	static public void PrtSpCntMsg(int sp, long cnt, String msg) {
		PrtSpMsg(sp, String.format("%7s %s", df.format(cnt), msg));
	}
	static public void PrtSpCntMsg2(int sp, int cnt, String msg, int cnt2, String msg2) {
		PrtSpMsg(sp, String.format("%7s %-10s  %7s %s", df.format(cnt), msg, df.format(cnt2), msg2));
	}
	static public void PrtSpCntMsg(int sp, int cnt, String msg) {
		PrtSpMsg(sp, String.format("%7s %s", df.format(cnt), msg));
	}
	static public void PrtSpCntMsgTimeMem(int sp, int cnt, String msg, long time) {
		PrtSpMsgTimeMem(sp, String.format("%7s %s", df.format(cnt), msg), time);
	}
	static public void PrtSpCntMsg(int sp, String cntStr, String msg) {
		PrtSpMsg(sp, String.format("%7s %s", cntStr, msg));
	}
	static public void PrtSpCntMsgNz(int sp, int cnt, String msg) {
		if (cnt>0) PrtSpMsg(sp, String.format("%7s %s", df.format(cnt), msg));
	}
	static public void PrtSpCntMsgZero(int sp, int cnt, String msg) {
		if (cnt==0) return;
		PrtSpMsg(sp, String.format("%7s %s", df.format(cnt), msg));
	}
	static public void PrtSpCntkMsg(int sp, long cnt, String msg) {
		String x=" ";
		if (cnt>1000000) {
			x = "M";
			cnt /= 1000000;
		}
		else if (cnt>1000) {
			x = "k";
			cnt /= 1000;
		}
		PrtSpMsg(sp, String.format("%6d%s %s", cnt, x, msg));
	 }
	 
	// No Space argument
	static public void PrtCntMsg(int cnt, String msg) {
		PrtSpMsg(3, String.format("%7s %s", df.format(cnt), msg));
	}
	static public void PrtCntMsg2(int cnt, String msg, int cnt2, String msg2) {
		PrtSpMsg(3, String.format("%7s %-20s  %7s %s", df.format(cnt), msg, df.format(cnt2), msg2));
	}
	
	// Time as argument
	static public void PrtSpCntMsg(int sp, long cnt, String msg, long t) {
		PrtSpMsgTime(sp, String.format("%7s %s", df.format(cnt), msg), t);
	}
	static public void PrtSpCntMsg(int sp, int cnt, String msg, long t) {
		PrtSpMsgTime(sp, String.format("%7s %s", df.format(cnt), msg), t);
	}
	static public void PrtCntMsg(int cnt, String msg, long t) {
		PrtSpMsgTime(3, String.format("%7s %40s %s", df.format(cnt), msg), t);
	}
	
	// Message first
	static public void PrtSpMsgCnt(int sp, String msg, int cnt) {
		PrtSpMsg(sp, String.format("%-18s %s", msg, df.format(cnt)));
	}
	static public void PrtSpMsgCntZero(int sp, String msg, int cnt) {
		if (cnt==0) return;
		PrtSpMsg(sp, String.format("%-18s %s", msg, df.format(cnt)));
	}
	
	// Date and time
	static public void PrtDateMsg (String msg) {
		String str = String.format("%-50s %20s", msg, TimeHelpers.getDate());
	    Print(str);
	}	
	static public void PrtSpDateMsg (int sp, String msg) {
		PrtSpMsg(sp, String.format("%-50s %20s", msg, TimeHelpers.getDate()));
	}	
    static public void PrtDateMsgTime (String msg, long t)
    {
    		String str = String.format("%-50s %20s  %s", msg, TimeHelpers.getDate(), TimeHelpers.getElapsedNanoTime(t));
        Print(str);
    }
    static public void PrtMsgTime (String msg, long t)
    {
    		String str = String.format("%-70s  %s", msg, TimeHelpers.getElapsedNanoTime(t));
        Print(str);
    }	
    static public void PrtSpMsgTime (int i, String msg, long t)
    {
    		String sp = "";
    		for (int j=0; j < i; j++) sp += "   ";
    		msg = sp + msg;
    		String str = String.format("%-70s  %s", msg, TimeHelpers.getElapsedNanoTime(t));
        Print(str);
    }
    // Used by all calculations to finish
    static public void PrtSpMsgTimeMem (int i, String msg, long t)
    {
    		String sp = "";
    		for (int j=0; j < i; j++) sp += "   ";
    		msg = sp + msg;
    		
       	String x = String.format("%-70s  %s  (%s)", msg, 
       			TimeHelpers.getElapsedNanoTime(t),
       			TimeHelpers.getMemoryUsedMb());
        Print(x);
    } 
    // msg can have newline at beginning
    static public void PrtMsgTimeMem (String msg, long t)
    {
    	String sp = "";	
    	msg = sp + msg;
       	String x = String.format("%-70s  %s  (%s)", msg, 
       			TimeHelpers.getElapsedNanoTime(t), TimeHelpers.getMemoryUsedMb());
        Print(x);
    } 
    // For AssemMain and LoadLibMain since they use different logging
    static public String getMsgTimeMem(String msg, long t) {
		String x = String.format("%-70s  %s  (%s)", msg, 
				TimeHelpers.getElapsedNanoTime(t), TimeHelpers.getMemoryUsedMb());
		return x;
    }
    // Only message - used by most of the above to add sp
    static public void PrtSpMsg (int i, String msg) {
    		String sp = "";
    		for (int j=0; j < i; j++) sp += "   ";
        sp += msg;
        Print(sp);
    }
    static public void PrtErr(String msg)
	{
	    	Print("***Error: " + msg);
	}	
	static public void PrtError(String msg)
	{
	    	Print("\n***Error: " + msg);
	}	
	static public void PrtWarn(String msg)
	{
	    	Print("+++Warning: " + msg);
	}	
	static public void die(String msg)
	{
		Print("***Abort execution: " + msg);
		close();
		System.exit(-1);
	}	
	static public long getTime () {
	    return TimeHelpers.getNanoTime(); 
	}	
	/***************************************************************
	 * Print routines
	 */
	static public void debug(String msg) { // so I can trace where these statements are
		System.err.println(msg);
	}
	static public void prt(String msg) { 
		System.err.println(msg);
	}
	static public void prtm(String msg) { 
		System.out.println(msg);
	}
	static public void prt(int i, String msg) {
		String sp = "";
		for (int j=0; j < i; j++) sp += "   ";
		msg = sp + msg;
		System.err.println(msg);
	}
	static public void r(String msg) {
		System.err.print("  " + msg + "...\r");
	}
	static public void mem(String msg) {
		System.err.print("  " + msg + "   " + TimeHelpers.getMemoryUsedMb() + "...\n");
	}
	static public void rp(String msg, int cnt, int tot) {
		int per = (int) (  ((double)cnt/(double)tot)  * 100.0);
		System.err.print("  " + msg + " " + cnt + "(" + per + "%) ...\r");
	}
	static public void rp(String msg, long cnt, int tot) {
		int per = (int) (  ((double)cnt/(double)tot)  * 100.0);
		System.err.print("  " + msg + " "  + cnt + "(" + per + "%) ...\r");
	}
	
	public static boolean yesNo(String question)
	{
		BufferedReader inLine = new BufferedReader(new InputStreamReader(System.in));

		System.err.print("?--" + question + " (y/n)? "); 
		try {
			String resp = inLine.readLine();
			if (resp.equalsIgnoreCase("y")) return true;
			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**************************************************************************
	 * XXX Codon and alignment methods 
	 */
	static public void printCodons(String name, int start, int end, String seq) {
		int width = 60, w=3;
		String line="";
		System.out.println(">>" + name + " S: " + start + " E: " + end);
  		for (int i=start; i<end-3; i+=3, w+=3) {
  			String codon = seq.substring(i, i+3);
  			if (isCodonStop(codon)) codon = codon.toUpperCase();
  			line += codon + " ";
  			if (w>=width) {
  				System.out.println(line);
  				line="";
  				w=0;
  			}
  		}
  		System.out.println(line+"\n");
	}
	static public void printAA(String name, int start, int end, String seq) {
		int width = 60, w=3;
		String line="";
		System.out.println(">>" + name + " S: " + start + " E: " + end);
  		for (int i=start; i<end-3; i+=3, w+=3) {
  			String codon = seq.substring(i, i+3);
  			if (isCodonStop(codon)) codon = codon.toUpperCase();
  			line += " " + AAStatistics.getAminoAcidFor(codon.charAt(0), codon.charAt(1),codon.charAt(2)) + " ";
  			if (w>=width) {
  				System.out.println(line);
  				line="";
  				w=0;
  			}
  		}
  		System.out.println(line+"\n");
	}
	static private boolean isCodonStop(String codon) {
		if (codon.equals("taa")) return true;
		if (codon.equals("tag")) return true;
		if (codon.equals("tga")) return true;
		return false;
	}
	/*********************************************************
	 * Outputs alignments
	 */
	// prints to stdout for debugging
	static public void printByCodon(String name1, String alignSeq1) {
		System.out.println(name1);
		for (int i=0, j=0; i<alignSeq1.length()-3; i++, j++) {
			String codon1 = alignSeq1.substring(i, i+3);
			
			if (j==30) {
				System.out.println();
				j=0;
			}
			System.out.print(codon1 + " ");
		}
		System.out.println();
	}
 	// prints to stdout for debugging
	static public void printByCodon(String name1, String alignSeq1, String name2, String alignSeq2) {
		System.out.println(name1 + " " + name2);
		String x1="1. ", x2="1. ";
		int cnt=1;
		for (int i=0, j=0; i<alignSeq1.length(); i+=3, j++) {
			String codon1 = alignSeq1.substring(i, i+3);
			String codon2 = alignSeq2.substring(i, i+3);
			if (j==30) {
				System.out.println(x1);
				System.out.println(x2);
				j=0;
				cnt++;
				x1 = x2 = cnt + ". ";
			}
			x1 += codon1 + " ";
			x2 += codon2 + " ";
		}
		System.out.println(x1);
		System.out.println(x2);
		System.out.println();
	}
	// prints to stdout for debugging
	static public void printAlign(String name1, String name2, String alignSeq1, String alignSeq2) {
		int len = Math.min(alignSeq1.length(), alignSeq2.length());
		if (alignSeq1.length() != alignSeq2.length()) {
			prt("Unequal lengths: " + alignSeq1.length() +"!="+ alignSeq2.length());
		}
	// compute the alignMatch line 
		StringBuffer sb = new StringBuffer (len);
	   	for (int i=0; i<len; i++) {
	   		char c1 = alignSeq1.charAt(i);
	   		char c2 = alignSeq2.charAt(i);
	   		if (c1==c2) sb.append(c1);
	   		else sb.append(" ");
	   	}
	   	String alignMatch = sb.toString();
	 // alignment
	   	
		int x, lineLen=90, nEndAlign=alignMatch.length();
		sb = new StringBuffer (lineLen);
		
		for (int offset=0; offset<len; offset+=lineLen) { 
			sb.append(String.format(" %8s %4d ", name1, offset)); 
			for (x=0; x<lineLen && (x+offset)<nEndAlign; x++) {
				char c = alignSeq1.charAt(x+offset);
				sb.append(c);
			}
			sb.append(String.format("   %4d ", offset+lineLen));
			System.out.println(sb.toString());
			sb.delete(0, sb.length());
			
			sb.append(String.format(" %8s %4s ", "",""));
			for (int i=0; i<lineLen && (i+offset)<nEndAlign; i++) sb.append(alignMatch.charAt(i+offset));
			System.out.println(sb.toString());
			sb.delete(0, sb.length());
			
			sb.append(String.format(" %8s %4d ", name2, offset));
			for (x=0; x<lineLen && (x+offset)<nEndAlign; x++) {
				char c = alignSeq2.charAt(x+offset);
				sb.append(c);
			}
			sb.append(String.format("   %4d ", (offset+lineLen)));
			System.out.println(sb.toString());
			sb.delete(0, sb.length());
			
			System.out.println("");
		}
	}
	/*******************************************************
	 * XXX Table maker
	 */
	public static String makeTable(
			int nCol, int nRow, String[] fields, int [] justify, String [][] rows) 
	{	
		Vector <String> lines = new Vector <String>();
		makeTable(lines, nCol, nRow, fields, justify, rows);
		String x="";
		for (String l : lines) x += l + "\n";
		return x;
	}
	public static String makeTable(
			int nCol, int nRow, int [] justify, String [][] rows) 
	{	
		Vector <String> lines = new Vector <String>();
		makeTable(lines, nCol, nRow, justify, rows);
		String x="";
		for (String l : lines) x += l + "\n";
		return x;
	}
	
	public static void makeTable(
			Vector <String> lines, int nCol, int nRow, String[] fields, int [] justify, String [][] rows)
	{
		int c, r;
		String line;
		String space = "  ";
		
		// compute column lengths
		int []collen = new int [nCol];
		for (c=0; c < nCol; c++) collen[c] = 0;
		
        for (c=0; c< nCol; c++) { // longest value
            for (r=0; r<nRow && rows[r][c] != null; r++) {
        		if (rows[r][c] == null) rows[r][c] = "";
        		if (rows[r][c].length() > collen[c]) 
        			collen[c] = rows[r][c].length();
            }
        }
        if (fields != null) {    // heading longer than any value?
			for (c=0; c < nCol; c++) {
				if (collen[c] > 0) {
					if (fields[c] == null) fields[c] = "";
					if (fields[c].length() > collen[c]) 
						collen[c]=fields[c].length();
				}
			}
	        // output headings
	        line = "      ";
	        for (c=0; c< nCol; c++) 
	        		if (collen[c] > 0) 
	        			line += pad(fields[c],collen[c], justify[c]) + space;
	        lines.add(line);
        }
        // output rows
        for (r=0; r<nRow; r++) {
        		line = "      ";
            for (c=0; c<nCol; c++) {
                 if (collen[c] > 0) 
                	 	line += pad(rows[r][c],collen[c],justify[c]) + space;
                 rows[r][c] = ""; // so wouldn't reuse in next table
            }
            lines.add(line);
        }
	}
	/*** No column headings ***/
	public static void makeTable(
			Vector <String> lines, int nCol, int nRow, int [] justify, String [][] rows)
	{
		int c, r;
		String line;
		String space = "  ";
		
		// compute column lengths
		int []collen = new int [nCol];
		for (c=0; c < nCol; c++) collen[c] = 0;
		
        for (c=0; c< nCol; c++) { // longest value
            for (r=0; r<nRow && rows[r][c] != null; r++) {
        		if (rows[r][c] == null) rows[r][c] = "";
        		if (rows[r][c].length() > collen[c]) 
        			collen[c] = rows[r][c].length();
            }
        }
   
        // output rows
        for (r=0; r<nRow; r++) {
        		line = "      ";
            for (c=0; c<nCol; c++) {
                 if (collen[c] > 0) 
                	 	line += pad(rows[r][c],collen[c],justify[c]) + space;
                 rows[r][c] = ""; // so wouldn't reuse in next table
            }
            lines.add(line);
        }
	}
    private static String pad(String s, int width, int o)
    {
		if (s == null) return " ";
        if (s.length() > width) {
            String t = s.substring(0, width-1);
            System.out.println("'" + s + "' truncated to '" + t + "'");
            s = t;
            s += " ";
        }
        else if (o == 0) { // left
            String t="";
            width -= s.length();
            while (width-- > 0) t += " ";
            s = t + s;
        }
        else {
            width -= s.length();
            while (width-- > 0) s += " ";
        }
        return s;
    }
   
	/****************************************************
	 * XXX Different conversions
	 */
	static public int getExponent(double n) {
		int d = -1;
		try {
			String s = String.format("%.0E", n);
			int index=0;
			if (s.contains("+")) index = s.indexOf("+")+1;
			else if (s.contains("-")) index = s.indexOf("-")+1;
			else return d;
			if (index<0) return d;
			s = s.substring(index);
			d = Integer.parseInt(s);
		} catch (Exception e) {}
		return d;
	}
	static public String df(int x) {
		return df.format(x);
	}
	static public String df(long x) {
		return df.format(x);
	}
	static public double div(int x, int y) {
		if (y==0 || x==0) return 0.0;
		return (((double) x/(double) y));
	}
	static public double div(long x, long y) {
		if (y==0 || x==0) return 0.0;
		return (((double) x/(double) y));
	}
	static public double div(double x, int y) {
		if (y==0 || x==0) return 0.0;
		return (( x/(double) y));
	}
	static public int perI(int x, int y) {
		if (y==0 || x==0) return 0;
		return (int) (((double) x/(double) y) * 100.0);
	}
	static public double perF(int x, int y) {
		if (y==0 || x==0) return 0.0;
		return (((double) x/(double) y) * 100.0);
	}
	static public String avg(int x, int y) {
		if (y==0) return "NA";
		if (x==0) return "0";
		
		double p = ((double) x/ (double) y) ; // do not add 0.5 because 4.1f rounds correctly
		if (p<0.1) return String.format("<0.1");
		
		return String.format("%4.1f", p);
	}
	static public String avg(long x, long y) {
		if (y==0) return "NA";
		if (x==0) return "0";
		
		double p = ((double) x/ (double) y) ; // do not add 0.5 because 4.1f rounds correctly
		if (p<0.1) return String.format("<0.1");
		
		return String.format("%4.1f", p);
	}
	static public String avg(double x, int y) {
		if (y==0) return "NA";
		if (x==0) return "0";
		
		double p = (x/ (double) y) ; // do not add 0.5 because 4.1f rounds correctly
		if (p<0.1) return String.format("<0.1");
		
		return String.format("%4.1f", p);
	}
	static public String avg(double x, long y) {
		if (y==0) return "NA";
		if (x==0) return "0";
		
		double p = (x/ (double) y) ; // do not add 0.5 because 4.1f rounds correctly
		if (p<0.1) return String.format("<0.1");
		
		return String.format("%4.1f", p);
	}
	// with parenthesis e.g. (5%), decimal
	static public String perItxtP(int x, int y) {
		return "(" + perItxt(x, y) + ")";
	}
	static public String perItxtP(long x, long y) {
		return "(" + perItxt(x, y) + ")";
	}
	// without parenthesis, decimal i.e. 5%, decimal
	static public String perItxt(int x, int y) {
		if (y==0) return "NA";
		if (x==0) return "0%";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0) + 0.5;
	 	if (p<1.0) return String.format("<1%s", "%");
	 	
	 	int i = (int) p;
	 	return String.format("%d%s",  i, "%");
	}
	static public String perItxt(long x, long y) {
		if (y==0) return "NA";
		if (x==0) return "0%";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0) + 0.5;
	 	if (p<1.0) return String.format("<1%s", "%");
	 	
	 	int i = (int) p;
	 	return String.format("%d%s",  i, "%");
	}
	static public String perFtxtP(int x, int y) {
		return "(" + perFtxt(x,y) + ")";
	}
	static public String perFtxtP(long x, long y) {
		return "(" + perFtxt(x,y) + ")";
	}
	static public String perFtxtNo(int x, int y) { // no % at end of number
		if (y==0) return "   NA";
		if (x==0) return "    0";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0); 
		if (p<0.1) return "<0.1";
		if (p>99.9 && x!=y) return "99.9";
		return String.format("%4.1f", p);
	}
	static public String perFtxtNo(long x, long y) {
		if (y==0) return "   NA";
		if (x==0) return "    0";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0); 
		if (p<0.1) return "<0.1";
		if (p>99.9 && x!=y) return "99.9";
		return String.format("%4.1f", p);
	}
	static public String perFtxt(int x, int y) {
		if (y==0) return "   NA";
		if (x==0) return "   0%";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0); 
		if (p<0.1) return String.format("<0.1%s", "%");
		if (p>99.9 && x!=y) return String.format("99.9%s", "%");
		return String.format("%4.1f%s", p,"%");
	}
	static public String perFtxt(long x, long y) {
		if (y==0) return "   NA";
		if (x==0) return "   0%";
		
		double p = ( ( ( (double) x/ (double) y) ) *100.0);
		if (p<0.1) return String.format("<0.1%s", "%");
		if (p>99.9 && x!=y) return String.format("99.9%s", "%");
		return String.format("%4.1f%s", p,"%");
	}
	static public String perFtxt(double x, double y) { 
		if (y==0) return "   NA";
		if (x==0) return "   0%";
		
		double p =  (  x/  y)  * 100.0;
		if (p<0.1) return String.format("<0.1%s", "%");
		if (p>99.9 && x!=y) return String.format("99.9%s", "%");
		return String.format("%4.1f%s", p,"%");
	}
	// Use KB or M notation
	static public String kbFText(int x) {
		if (x<100000) return df.format(x); // 99,999 will be printed
		double y = ((double) x/1000.0);
		return String.format("%.1fk", y);
	}
	static public String kbText(int x) {
		if (x<1000) return df.format(x); // 999 will be printed
		int y = (int) (((double) x/1000.0) +0.5);
		return df.format(y)+"k";
	}
	static public String kbText(long x) {
		if (x<1000) return df.format(x);
		int y = (int) (((double) x/1000.0) +0.5);
		return df.format(y)+"k";
	}
	
	// print %7s 1000.0M
	static public String kMText(int len) {
		return kMText((long) len);
	}
	static public String kMText(long len) {
		double d = (double) len;
		String x = len+"";
		if (len>=1000000000) {
			d = d/1000000000.0;
			x = String.format("%.1fB", d);
		}
		else if (len>=1000000) {
			d = d/1000000.0;
			x = String.format("%.1fM", d);
		}
		else if (len>=1000)  {
			d = d/1000.0;
			x = String.format("%.1fk", d);
		}
		return x;
	}
	static public String mText(long len) {
		double d = (double) len;
		String x = len+"";
		if (len>=1000000000) {
			d = d/1000000.0;
			long i = (long) (d+0.5);
			x = String.format("%,dM", i);
		}
		else if (len>=1000000) {
			d = d/1000.0;
			long i = (long) (d+0.5);
			x = String.format("%,dk", i);
		}
		else {
			long i = (long) (d+0.5);
			x = String.format("%,d", i);
		}
		return x;
	}
	
	static public String formatDouble(double val) {
	    	if (val == 0) return "0.0";
	    	
	    	double a = Math.abs(val);
	    	if (a>=100.0)   return String.format("%.1f", val); 
	    	if (a>=1.0)     return String.format("%.3f", val); 
	    	if (a >= .0001) return String.format("%.3f", val); 
	    	return  String.format("%.1E", val);
	}
	
	// if projcmp is prefix and /usr/tcw/projcmp/ex is path, return /projcmp/ex
	static public String mkPathRelative(String prefix, String filePath) {
	
		if (filePath.contains(prefix))
		{
			int index = filePath.indexOf(prefix);
			return filePath.substring(index);
		}
		else return filePath;
	}
	static public String getProjectDir() {
		try {
			String projDir = Globalx.PROJDIR + "/";
			String s = Globalx.PROJDIR + "/";
			File f = new File(s);
			if (f.exists()) {
				return new File(".").getCanonicalPath();
			}
			s = "../" + projDir;
			f = new File(s);
			if (f.exists()) {
				return new File("../").getCanonicalPath();
			}
			Out.PrtError("Cannot find project directory " + projDir);
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}
}
