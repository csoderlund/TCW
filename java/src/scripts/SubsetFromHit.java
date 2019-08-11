package scripts;
/***********************************************
 * Read a hit.tab file and FASTA file and create a subset FASTA
 * of sequences with hits.
 * This was used to create the Sb subset for the TCW BBH vs Galaxy comparison
 */
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;

public class SubsetFromHit {
	String dir = "paper/SbSubset/";
	String infile1 = dir + "os2661_NTSb.tab"; // Annotated os2661 with file2; resulting hit file  
	String infile2 = dir + "GCF_000003195.3_Sorghum_bicolor_NCBIv3_rna.fna"; 
	String outfile = dir + "sbSubset.fna";
	
	double eCutoff=1E-40, simCutoff=40.0;
	int maxSeqs=2600;
	
	public static void main(String[] args) {
		new SubsetFromHit();
	}
	public SubsetFromHit() {
		try {
			HashSet <String> names = new HashSet <String> ();
			BufferedReader in = new BufferedReader ( new FileReader (infile1)); 
			String line, name;
			
			// blast tab format
			while ((line = in.readLine()) != null) {
				String [] tok =  line.split("\\t");
				
				double sim = Double.parseDouble(tok[2]);
				if (sim<simCutoff) continue;
				double eval = Double.parseDouble(tok[10]);
				if (eval>eCutoff) continue;
				
				if (!names.contains(tok[1])) names.add(tok[1]);
			}
			in.close();
			System.out.println("Found " + names.size() + " unique names");
			
			in = new BufferedReader ( new FileReader (infile2)); 
			PrintWriter out = new PrintWriter(new FileOutputStream(outfile, false));
			
			int cnt1=0, cnt2=0;
			boolean bPrt=false;
			
			// fasta format
			while ((line = in.readLine()) != null) {
				if (line.startsWith(">")) {
					cnt1++;
					line = line.substring(1);
					name = line.split(" ")[0];
					if (names.contains(name)) {
						bPrt=true;
						out.println(">" + line);
						cnt2++;
					}
					else bPrt=false;
				}
				else if (bPrt) out.println(line);
				if (cnt2>=maxSeqs) break;
			}
			System.out.println("Read " + cnt1 + " Wrote " + cnt2 );
		}
		catch (Exception e) {e.printStackTrace();}
	}
}
