package cmp.align;

/*****************************************************
 * Create summary for pair coding statistics
 * Used by viewMulti PairTable and runMulti PairStats and Pairwise
 */
import java.sql.ResultSet;
import java.util.Vector;

import cmp.compile.Summary;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Stats;
import util.ui.UserPrompt;

public class PairSumStats {
	private String pad = Summary.padTitle;
	
	public PairSumStats(DBConn mDB) {
		this.mDB = mDB;
	}
	
	/**********************************************
	 * runMulti: Create summary of all pairs in database
	 * This is only for when the additional alignments have been done.
	 */
	public String allCodingStats() {
		try {
			CompressFromDB cObj = new CompressFromDB ();
			cObj.loadFromDB();
			cObj.processCompressedFromDB();
			return cObj.infoStr;
		}
		catch(Exception e) {ErrorReport.reportError(e, "write summary"); return "error";}
	}
	/**********************************************
	 * runMulti: Create summary of all kaks pairs in database
	 */
	public void allKaKsStats(String method) {
		try {
			KaKs kObj = new KaKs();
			kObj.loadFromDB();
			kObj.createSummary(method);
			Out.prt("\n" + kObj.infoStr);
			
		 	mDB.executeUpdate("update info set kaksInfo='" + kObj.infoStr +"'");
		}
		catch(Exception e) {ErrorReport.reportError(e, "write kaks summary"); }
	}
	/**********************************************
	 * viewMulti: Create both coding and kaks summary of input pairs
	 */
	public void fromView(final Vector <Integer> pairs, final String summary) {
		if (pairs.size()>1000) {
			if (!UserPrompt.showContinue("Table stats", "This is slow for over 1000 pairs."))
					return;
			Out.PrtSpMsg(1, "Start table stats, this will take a few minutes......");
		}
		else Out.PrtSpMsg(1, "Start table stats.....");
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					boolean success=true;
					CompressFromDB cObj = new CompressFromDB ();
					if (cObj.loadFromDB(pairs)) {
						if (cObj.processCompressedFromDB()) {
							Out.PrtSpMsg(1, "Start KaKs stats.....");
							KaKs kObj = new KaKs();
							kObj.loadFromDB(pairs);
							kObj.createSummary("");
							Out.PrtSpMsg(1, "Complete KaKs stats                ");
						}
						else success=false;
					}
					else success=false;
	
					if (!success) {
						pairInfoStr = 
								"This only works for pairs that have alignments in the database.\n" +
								"Only pairs that are in clusters will have alignments in the database.\n" +
								"None of the " + pairs.size() + " pairs have alignments.";
					}
					pairInfoStr = summary + "\n" + pairInfoStr;
					UserPrompt.displayInfoMonoSpace(null, summary, pairInfoStr);
					mDB.close(); // close here because can get closed before used
					
				} catch (Exception e) {ErrorReport.reportError(e, "Error generating list");
				} catch (Error e) {ErrorReport.reportError(e, "Fatal error generating list");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	/*************************************************************/
	private class CompressFromDB {
		private boolean processCompressedFromDB() { 
		try {
			ScoreCDS scoreObj = new ScoreCDS ();
			int cntNotAlign=0;
			for (int i=0; i<pairid.size(); i++) {
				if (loadAndUncompress(pairid.get(i), seqid1.get(i), seqid2.get(i))) {
					
					scoreObj.scoreCDS(cdsObj.getAlignCropSeq1(), cdsObj.getAlignCropSeq2());
					
					scoreObj.diffScoreAll(name, cdsObj, utr5Obj, utr3Obj);
					
					scoreObj.sumScore();
					if ((i+1)%1000==0) Out.rp("processed", i, pairid.size());
				}
				else cntNotAlign++;
			}
			if (cntNotAlign==pairid.size()) return false;
			
			String badStr = (cntNotAlign>0) ? ("No alignment: " + cntNotAlign  + "\n") : "";
			pairInfoStr = infoStr = badStr + scoreObj.createSummary();
			return true;
		}
		catch(Exception e) {ErrorReport.die(e, "computing stats"); return false;}
		}
		
		// runMulti - redo statistics when alignments already existed in DB
		private void loadFromDB() {
		try {
			ResultSet rs = mDB.executeQuery(
				"Select PAIRid, UTid1, UTid2 from pairwise where hasGrp=1 and nAlign>0"); // 
		
			while (rs.next()) {
				pairid.add(rs.getInt(1));
				seqid1.add(rs.getInt(2));
				seqid2.add(rs.getInt(3));
			}
			Out.PrtSpCntMsg(1, pairid.size(), "Loaded sequences");
		}
		catch(Exception e) {ErrorReport.die(e, "computing stats");}
		}
		// viewMulti
		private boolean loadFromDB(Vector <Integer> pairids) {
			try {
				int count = mDB.executeCount("select count(*) from pairMap limit 1");
				if (count==0) return false;
				
				for (int id : pairids) {
					ResultSet rs = mDB.executeQuery(
						"Select PAIRid, UTid1, UTid2 from pairwise where pairid=" + id);  
				
					if (rs.next()) {
						boolean isAligned = (rs.getInt(1)>0);
						if (!isAligned) continue;
						
						pairid.add(rs.getInt(1));
						seqid1.add(rs.getInt(2));
						seqid2.add(rs.getInt(3));
					}
				}
				if (pairid.size()==0) return false;
				return true;
			}
			catch(Exception e) {
				pairInfoStr = "Error during computing stats";
				ErrorReport.reportError(e, "computing stats"); 
				return false;
			}
		}
		private Vector <Integer> pairid = new Vector <Integer> ();
		private Vector <Integer> seqid1 = new Vector <Integer> ();
		private Vector <Integer> seqid2 = new Vector <Integer> ();
		
		private String infoStr="";
	} // End of CodingFromDB
	
	private class KaKs { 
		private void createSummary(String method) {
			if (kkVec.size()==0) return;
			
			double [] qrt = Stats.setQuartiles(kkVec, Globalx.dNoVal);
			
			// table for overview
			String [] fields = {"Ka/Ks", "" ,"   Quartiles", "", "   Average", "", "   P-value", ""};
			
			int [] justify =   {1, 0, 1,   0,  1,  0,  1,  0};
			int nRow = 4;
		    int nCol=  fields.length;
		    String [][] rows = new String[nRow][nCol];
		    int r=0, c=0;
		  
		    // CAS327 changed order of columns
		    rows[r][c] = "NA ";   rows[r++][c+1] = String.format("%,d",kcnt[0]); // CAS327 was Zero
		    rows[r][c] = "KaKs~1";   rows[r++][c+1] = String.format("%,d",kcnt[1]);
		    rows[r][c] = "KaKs<1";   rows[r++][c+1] = String.format("%,d",kcnt[2]); 
		    rows[r][c] = "KaKs>1";   rows[r++][c+1] = String.format("%,d",kcnt[3]);
		    
		    r=0; c=2;
		    rows[r][c] = "   Q1(Lower)"; rows[r++][c+1] =  formatDouble(qrt[0]);
		    rows[r][c] = "   Q2(Median)"; rows[r++][c+1] = formatDouble(qrt[1]);
		    rows[r][c] = "   Q3(Upper)"; rows[r++][c+1] =  formatDouble(qrt[2]);
		    rows[r][c] = ""; rows[r++][c+1] = "";
		    
		    r=0; c=4;
		    rows[r][c] = "   Ka";      rows[r++][c+1] = formatDouble(sumKa/nKa); 
		    rows[r][c] = "   Ks";      rows[r++][c+1] = formatDouble(sumKs/nKs); 
		    rows[r][c] = "   P-value"; rows[r++][c+1] = formatDouble(sumPval/nPval);
		    rows[r][c] = "";        rows[r++][c+1] = "";
		    
		    r=0; c=6;
		    rows[r][c] = "   <1E-100"; rows[r++][c+1] = String.format("%,d",pcnt[0]);
		    rows[r][c] = "   <1E-10";  rows[r++][c+1] = String.format("%,d",pcnt[1]);
		    rows[r][c] = "   <0.001";  rows[r++][c+1] = String.format("%,d",pcnt[2]);
		    rows[r][c] = "   Other";    rows[r++][c+1] =  String.format("%,d",pcnt[3]);
		  
		    int npair = kcnt[0]+kcnt[1]+kcnt[2]+kcnt[3];
		    String sz = String.format("%,d",npair);
		    if (method!="")
		    	   infoStr =  pad +  "KaKs method: " + method + "    Pairs: " + sz + "\n"; 
		    else 
		    	   infoStr =  pad +  "KaKs pairs: " +  sz + "\n"; 
		    infoStr += Out.makeTable(nCol, nRow, fields, justify, rows);
		    pairInfoStr += infoStr;
		}
		private String formatDouble(double val) { // CAS330 moved from Out, as all others use DisplayDecimal
	    	if (val == 0) return "0.0";
	    	
	    	double a = Math.abs(val);
	    	if (a>=100.0)   return String.format("%.1f", val); 
	    	if (a>=1.0)     return String.format("%.3f", val); 
	    	if (a>=0.0001) return String.format("%.3f", val); 
	    	return  String.format("%.1E", val);
		}
		private void init() {
			for (int i=0; i<4; i++) kcnt[i]=0;
			for (int i=0; i<4; i++) pcnt[i]=0;
		}
		private void loadFromDB() {
			try {
				init();
				ResultSet rs = mDB.executeQuery("Select ka, ks, kaks, pVal " +
						"from pairwise where kaks>"  + Globalx.dNoVal); 
			
				while (rs.next()) rSet(rs);
			}
			catch(Exception e) {ErrorReport.die(e, "computing kaks stats");}
		}
		private void loadFromDB(Vector <Integer> pairs) {
			try {
				init();
				int cnt=0, total=0;
				String sql = "Select ka, ks, kaks, pVal " +
					"from pairwise where kaks>" + Globalx.dNoVal + " and pairid=";
				for (int id : pairs) {
					ResultSet rs = mDB.executeQuery(sql + id); 
					if (rs.next()) {
						rSet(rs);
						cnt++; total++;
						if (cnt==1000) {
							Out.r("Processed " + total);
							cnt=0;
						}
					}
				}
			}
			catch(Exception e) {ErrorReport.prtReport(e, "computing kaks stats");}
		}
		private void rSet(ResultSet rs) {
			try {
				double ka =   rs.getDouble(1), ks =   rs.getDouble(2);
				double kaks = rs.getDouble(3), pval = rs.getDouble(4);
				
				// CAS327 ka,ks,kaks are displayed with 3 SF, hence, it clearer for the counts to
				// be on these values, i.e. it rounds some to 1.
				if (ka  >Globalx.dNullVal) {sumKa += ka;     nKa++;}
				if (ks  >Globalx.dNullVal) {sumKs += ks;     nKs++;}
				if (kaks>Globalx.dNullVal) {kkVec.add(kaks); }
				if (pval>Globalx.dNullVal) {sumPval += pval; nPval++;}
				
				// if change here, change in PairQueryPanel 
				if (kaks==Globalx.dNullVal)     kcnt[0]++; // CAS327 was checking ka & ks separately
				else {
					if (kaks>=0.995 && kaks<1.006)  kcnt[1]++;
					else if (kaks>=0 && kaks<1.0)   kcnt[2]++; // CAS327 was >0
					else if (kaks>1.0)    			kcnt[3]++; 
				}
				if (pval<0)           pcnt[3]++;
				else if (pval<1e-100) pcnt[0]++;
				else if (pval<1e-10)  pcnt[1]++;
				else if (pval<0.001)  pcnt[2]++;
				else                  pcnt[3]++;
			}
			catch(Exception e) {ErrorReport.prtReport(e, "KaKs resultset");}
		}
		Vector <Double> kkVec = new Vector <Double> (); // for quartiles
		int [] kcnt = new int [4]; 
		int [] pcnt = new int [4]; 
		double sumKa=0.0, sumKs=0.0,  sumPval=0.0;
		int nKa=0, nKs=0, nPval=0;
		
		String infoStr = "";
	} // End KaKs class

	/*******************************************************************
	 * 1. Create and save the gap map for an alignment
	 * 2. Load and compute the alignment from the gapmap
	 * If one or both UTRs <Share.minAlignLen, then the map will be blank
	 *    right now, it is only used for the statistics, and there are no statistics if one is too short
	 */
	public void saveAlignToDB(int pairid, PairAlignData cdsObj, PairAlignData utr5Obj, PairAlignData utr3Obj) {
		try {
			String cdsMap =  Share.compress(cdsObj.getAlignFullSeq1())   + "###" + Share.compress(cdsObj.getAlignFullSeq2());
			
			String utr1 = utr5Obj.getAlignFullSeq1();
			String utr2 = utr5Obj.getAlignFullSeq2();
			String utr5Map = (utr1=="" || utr2=="") ? "" :
				Share.compress(utr1) + "###" + Share.compress(utr2);
			
			utr1 = utr3Obj.getAlignFullSeq1();
			utr2 = utr3Obj.getAlignFullSeq2();
			String utr3Map = (utr1=="" || utr2=="") ? "" :
				Share.compress(utr1) + "###" + Share.compress(utr2);
			
			mDB.executeUpdate("insert into pairMap set PAIRid=" + pairid +
					", cds='" + cdsMap + "', utr5='" + utr5Map + "', utr3='" + utr3Map + "'");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "compute gap map and save");}
	}
	// create cropped aligned sequence from stored compressed alignments
	public boolean loadAndUncompress(int pairid, int seqid1, int seqid2) {
		try {
			ResultSet rs = mDB.executeQuery("select ntSeq, orf_start, orf_end, UTstr from unitrans where UTid=" + seqid1);
			if (!rs.next()) return false;
			
			String seq =   rs.getString(1);
			int orfStart = rs.getInt(2);
			int orfEnd =   rs.getInt(3);
			name[0] = rs.getString(4);
			String cds_1seq  = seq.substring(orfStart-1, orfEnd);
			String utr5_1seq = seq.substring(0, orfStart);
			String utr3_1seq = seq.substring(orfEnd-3);	
			
			rs = mDB.executeQuery("select ntSeq, orf_start, orf_end, UTstr from unitrans where UTid=" + seqid2);
			if (!rs.next()) return false;
				
			seq =   rs.getString(1);
			orfStart = rs.getInt(2);
			orfEnd =   rs.getInt(3);
			name[1] = rs.getString(4);
			String cds_2seq  = seq.substring(orfStart-1, orfEnd);
    		String utr5_2seq = seq.substring(0, orfStart);
    		String utr3_2seq = seq.substring(orfEnd-3);
    			
    		rs = mDB.executeQuery("select cds, utr5, utr3 from pairMap where PAIRid=" + pairid);
    		if (!rs.next()) return false;
					
			String cdsPair = rs.getString(1);
			String utr5Pair = rs.getString(2);
			String utr3Pair = rs.getString(3);
				
			/* uncompress */
			String [] gapCDS = cdsPair.split("###");
			if (gapCDS.length!=2) {
				Out.PrtError(pairid + " Bad gapCDS: " + cdsPair);
				return false;
			}
			String full1 = Share.uncompress(gapCDS[0], cds_1seq);
			String full2 = Share.uncompress(gapCDS[1], cds_2seq);
				
			if (full1.length() != full2.length()) 
				Out.die(pairid + " TCW error on uncompress for CDS : " + seqid1 + " " + seqid2);
					
			cdsObj.crop(full1, full2);
				
			if (utr5Pair.contains("###")) {
				String [] gapUTR5 = utr5Pair.split("###");
				if (gapUTR5.length<2) {
					Out.bug(String.format("%s pair #%d   ids %d,%d    %s","TCW error on uncompress for 5UTR", 
							pairid, seqid1, seqid2, utr5Pair));
					Out.die("Cannot go on");
				}
				full1 = Share.uncompress(gapUTR5[0], utr5_1seq);
				full2 = Share.uncompress(gapUTR5[1], utr5_2seq);
				
				if (full1.length() != full2.length()) {
					Out.bug(String.format("%s  pair #%d   ids %d,%d    lens %d,%d  gap %s,%s",
							"TCW error on uncompress for 5UTR", pairid,
							seqid1, seqid2, full1.length(), full2.length(),gapUTR5[0],gapUTR5[1]));
					utr5Obj.clear();
				}
				else utr5Obj.crop(full1, full2);
			}
			else utr5Obj.clear();
		
			if (utr3Pair.contains("###")){
				String [] gapUTR3 = utr3Pair.split("###");
				full1 = Share.uncompress(gapUTR3[0], utr3_1seq);
				full2 = Share.uncompress(gapUTR3[1], utr3_2seq);
				
				if (full1.length() != full2.length()) {
					Out.bug(String.format("%s pair #%d   ids %d,%d    lens %d,%d  gap %s,%s",
							"TCW error on uncompress for 3UTR", pairid,
							seqid1, seqid2, full1.length(), full2.length(), gapUTR3[0],gapUTR3[1]));
					utr3Obj.clear();
				} 
				else utr3Obj.crop(full1, full2);
			}   
			else utr3Obj.clear();
				
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "recreate gapped alignment"); return false;}
	}
		
	private String [] name = new String [2]; // for debugging
	private DBConn mDB;
	private PairAlignData cdsObj= new PairAlignData (), utr5Obj=new PairAlignData (), utr3Obj=new PairAlignData ();
	private String pairInfoStr="";
}
