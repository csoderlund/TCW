package scripts;
/*******************************************
 * TCW blasted nr.gz against demoTra. Move the demo-nr.tab to blast/nr.tab
 * Run from main directory in order to make a nrDemo fasta file of only the hit proteins.
 * 1. Read blast .tab file. 2. Read nr.gz. 3. Write a subset of nr.gz fasta file.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

public class parseNR {
	static private String inFasta = "projects/DBfasta/nr_Oct2016/nr.gz";
	static private String inTab = "blast/nr.tab";
	static private String outFasta = "blast/nrDemo";
	
	public static void main(String[] args) {
		System.out.println("Read " + inFasta + " and  " + inTab);
		System.out.println("Create " + outFasta);
		readTab();
		readGZ();
		System.out.println("Finished");
	}
	private static void readTab() {
		try {
			BufferedReader in = new BufferedReader ( new FileReader (inTab)); 
			String line;
			while ((line = in.readLine()) !=null) {
				String [] tok = line.split("\t");
				nrIDs.add(tok[1]);
			}
			System.out.println("Read " + nrIDs.size());
		}
		catch (Exception e) {e.printStackTrace();};
	}
	private static void readGZ() {
		try {
			BufferedReader in = openGZIP(inFasta);
			PrintWriter out = new PrintWriter(new FileOutputStream(outFasta, false));
			int cnt=0;
			String line, name;
			boolean prt=false;
			while ((line = in.readLine()) != null) {
				if (line.startsWith(">")) {
					if (line.contains(" ")) name = line.substring(1, line.indexOf(" "));
					else 	name = line.substring(1);
					prt  = false;
					if (nrIDs.contains(name)) {
						prt = true;
						out.format("%s\n", line);
						cnt++;
					}
					else prt=false;
				}
				else if (prt) out.format("%s\n", line);
			}
			System.out.println("Wrote " + cnt);
		}
		catch (Exception e) {e.printStackTrace();};
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
					else {
						System.err.println("Cannot open file " + file);
						System.exit(-1);
					}
				}
			}
			if (file.endsWith(".gz")) {
				FileInputStream fin = new FileInputStream(file);
				GZIPInputStream gzis = new GZIPInputStream(fin);
				InputStreamReader xover = new InputStreamReader(gzis);
				return new BufferedReader(xover);
			}
			else System.err.println("Do not recognize file suffix: " + file);
		}
		catch (Exception e) {e.printStackTrace();}
	  
		return null;
	}
	private static HashSet <String> nrIDs = new HashSet <String> ();
}
