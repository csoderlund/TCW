package cmp.align;
/******************************************************
 * runMultiTCW-> PairsStats    - compute stats per sequence, enter into database. 
 * 
 * runMultiTCW -> summary stats for overview
 * 
 * viewMultiTCW->PairAlignText - display stats and create 'match' string.
 */
import java.sql.PreparedStatement;
import java.util.Vector;

import cmp.compile.Summary;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

public class ScoreCDS 
{	
	/** Codon based counts **/
	final private int IDX_nCodonsNoIndel 	= 0; // nCodons no gaps 
	final private int IDX_cExact 	= 1; 
	final private int IDX_cSyn 		= 2; // synonymous codons (2d, 4d, xd)
	final private int IDX_cNonSyn 	= 3; // nonsynonymous (nd)
	final private int IDX_2d 		= 4; // twofold degenerate
	final private int IDX_4d 		= 5; 
	final private int IDX_xd			= 6;  // not 4d or 2d - not used
	final private int IDX_aaExact 	= 7; // exact match = (codon exact+syn)
	final private int IDX_aaPos 		= 8; // blosum>=0
	final private int IDX_aaNeg 		= 9; // blosum<0
	
	// by position
	final private int IDX_ts1 		= 12;
	final private int IDX_ts2 		= 13;
	final private int IDX_ts3 		= 14;
	final private int IDX_tv1 		= 15;
	final private int IDX_tv2 		= 16;
	final private int IDX_tv3 		= 17;
	
	final private int IDX_GC_B		= 18; // by nucleotide intersection
	final private int IDX_GC_E		= 19; // by nucleotide union
	final private int IDX_CpGn_B		= 20; // by nucleotide intersection
	final private int IDX_CpGn_E		= 21; // by nucleotide union
	final private int IDX_CpGc_B		= 22; // do not cross codon boundary intersection
	final private int IDX_CpGc_E		= 23; // do not cross codon boundary union
	
	final private int IDX_open 		= 24; 
	final private int IDX_gap		= 25; // all gaps including indels
	
	// UTRs/CDs diff nucleotide counts
	final private int IDX_nSNPs	= 11; // differences no gaps (SNPs)
	
	// compute in Diff class
	final private int IDX_nCDSlenNoHang    = 26; // len with gaps
	final private int IDX_n5UTRlenNoHang   = 27; 
	final private int IDX_n3UTRlenNoHang   = 28; 
	
	final private int IDX_nCDSdiffNoHang    = 29; // differences with gaps
	final private int IDX_n5UTRdiffNoHang   = 30; 
	final private int IDX_n3UTRdiffNoHang   = 31; 
	
	final private int IDX_nCDSlenWithHang    = 32; // not used for summary header only
	final private int IDX_n5UTRlenWithHang   = 33; 
	final private int IDX_n3UTRlenWithHang   = 34; 
	final private int IDX_nCDSdiffWithHang    = 35; 
	final private int IDX_n5UTRdiffWithHang   = 36; 
	final private int IDX_n3UTRdiffWithHang   = 37; 
	
	final private int nCounts	= 38;
	
	final private String bPos = Globalx.blosumPos;
	final private String bNeg = Globalx.blosumNeg;
		
	public ScoreCDS () {
		curScore = new int[nCounts];
		sumAllPairs = new long[nCounts];
		for (int i=0; i<nCounts; i++) {
			sumAllPairs[i]=curScore[i]=0;
		}
	}
	public void sumScore() {
		ySumJI(sumJI_CG,   curScore[IDX_GC_B],   curScore[IDX_GC_E]);
		ySumJI(sumJI_CpG,  curScore[IDX_CpGn_B], curScore[IDX_CpGn_E]);
		ySumJI(sumJI_CpGc, curScore[IDX_CpGc_B], curScore[IDX_CpGc_E]);
		
		for (int i=0; i<nCounts; i++) {
			if (curScore[i]!=Globalx.iNoVal) sumAllPairs[i]+=curScore[i]; 
			curScore[i]=0;
		}
	}
	/**************************************************
	 * runMulti and viewMulti summary
	 * runMulti columns, which needs cdsLen1, cdsLen2 for columns cov1 and cov2
	 */
	public void scoreCDS(int [] cdsLen,  String [] cds) {
		this.cdsLen1=cdsLen[0];
		this.cdsLen2=cdsLen[1];
		scoreCDS(cds[0], cds[1]);
	}
	/*********************************************************
	 * Scores the current cds pair. 
	 */
	public void scoreCDS(String cds1, String cds2) { // cropped alignments, no overhang
		cntCDS++;
		for (int i=0; i<nCounts; i++) curScore[i]=0;
		
		// by nucleotide
		boolean extendGap=false;
		char lastc1= ' ', lastc2=' ';
		for (int i=0; i<cds1.length(); i++) {
			char c1 = cds1.charAt(i), c2 = cds2.charAt(i);

			if (c1==Share.gapCh || c2==Share.gapCh) {
				curScore[IDX_gap]++;	
				if (!extendGap) {
					curScore[IDX_open]++;
					extendGap=true;
				}
			}
			else {
				extendGap=false;
				if (c1!=c2) curScore[IDX_nSNPs]++; 
			}
			
			// GC and CpG-NT
			boolean gc1 =  (c1=='c' || c1=='g');
			boolean gc2 =  (c2=='c' || c2=='g');
			if (gc1  || gc2)  curScore[IDX_GC_E]++;
			if (gc1  && gc2)  curScore[IDX_GC_B]++;
			
			boolean cpg1 = (lastc1=='c' && c1=='g');
			boolean cpg2 = (lastc2=='c' && c2=='g');	
			if (cpg1 && cpg2) curScore[IDX_CpGn_B]++;
			if (cpg1 || cpg2) curScore[IDX_CpGn_E]++;
			lastc1 = c1; lastc2=c2;
		}
		
		// by codon
		for (int i=0; i<cds1.length()-2; i+=3) {
			String codon1 = cds1.substring(i, i+3);
			String codon2 = cds2.substring(i, i+3);
			
			scoreByPos(codon1, codon2);
			
			if (codon1.contains(Share.gapStr) || codon2.contains(Share.gapStr)) {
				continue;
			}
			curScore[IDX_nCodonsNoIndel]++;	// aligned codons without indels
			
			char a1 = convert.codonToAA(codon1);
			char a2 = convert.codonToAA(codon2);
			
			// aa scores
			if (a1==a2)	             			curScore[IDX_aaExact]++;
			else if (convert.isHighSub(a1, a2)) curScore[IDX_aaPos]++;
			else                         		curScore[IDX_aaNeg]++;
			
			// codon scores
			if  (codon1.equals(codon2))  curScore[IDX_cExact]++;
			else if (a1==a2) 			 curScore[IDX_cSyn]++;
			else        				 curScore[IDX_cNonSyn]++;
			
			// degenerate codons
			if (a1==a2 && !codon1.equals(codon2)) {
				String d1 = foldN(a1, codon1);
				String d2 = foldN(a2, codon2);
					
				if (d1.equals(d2)) {
					if (d1.equals(Share.N2D)) 		curScore[IDX_2d]++;
					else if (d1.equals(Share.N4D)) 	curScore[IDX_4d]++;	
				}
				else 								curScore[IDX_xd]++; 
			}
		}
	}
	
	private void scoreByPos(String codon1, String codon2) {
		char lastc1= ' ', lastc2=' ';
		for (int j=0; j<3; j++) {
			char c1 = codon1.charAt(j);
			char c2 = codon2.charAt(j);
			
			boolean cpg1 = (lastc1=='c' && c1=='g');
			boolean cpg2 = (lastc2=='c' && c2=='g');
			if (cpg1 && cpg2) curScore[IDX_CpGc_B]++;
			if (cpg1 || cpg2) curScore[IDX_CpGc_E]++;
			lastc1 = c1; lastc2=c2;
			
			if (c1==c2 || c1==Share.gapCh || c2==Share.gapCh) continue;
			
			boolean isTS = isTS(c1, c2);
			if (j==0) {
				if (isTS) curScore[IDX_ts1]++;
				else      curScore[IDX_tv1]++;
			}
			else if (j==1) {
				if (isTS) curScore[IDX_ts2]++;
				else      curScore[IDX_tv2]++;
			}
			else if (j==2) {
				if (isTS) curScore[IDX_ts3]++;
				else      curScore[IDX_tv3]++;
			}
		}
	}
	/************************************************
	 *  2d and 4d from table in Lehmann and Libchaber RNA (2008), 14:1264-1269.
	 *  Shortened from their 1st paragraph:	
	 *  		When the first two positions are G or C, 4d;
	 *  		when the first two positions are A or T, 2d (or 3d)
	 *  		otherwise, it depends on the second position:
	 *  			a pyrimidine C or T is 4d
	 *  			a purine     A or G is 2d
	 *  Every codon is either 4d of 2d, including MET and TRP
	 *  Before calling, it is check that they are different codons but same AA
	 *  
	 */
	private String foldN(char aa, String codon) {
		char c1=codon.charAt(0);
		char c2=codon.charAt(1);
		
		if ((c1=='c' || c1=='g') && (c2=='c' || c2=='g')) return Share.N4D;
		if ((c1=='a' || c1=='t') && (c2=='a' || c2=='t')) return Share.N2D;
		
		if (c2=='c' || c2=='t')  return Share.N4D;
		if (c2=='a' || c2=='g')  return Share.N2D; 
		
		return "?";
	}
	
	private boolean isTS(char c1, char c2) {
		if      (c1=='a' && c2=='g') return true;
		else if (c1=='g' && c2=='a') return true;
		else if (c1=='c' && c2=='t') return true;
		else if (c1=='t' && c2=='c') return true;
		else return false;
	}
	
	/***************************************************************
	 * Save computed values in database 
	 *  idx1 and idx2 were loaded from pairwise -- they are in correct order
	 */
	public void saveStatsToDB(DBConn mDB, int idx1, int idx2) {
		try {
			PreparedStatement ps1 = mDB.prepareStatement("update pairwise set " +
					"nAlign=?, pOlap1=?, pOlap2=?, nSNPs=?, nGap=?, nOpen=?,"  +
					"Calign=?, pCmatch=?, pCsyn=?, pC4d=?, pC2d=?, pCnonsyn=?," +
					"pAmatch=?, pAsub=?, pAmis=?, " +
					"pDiffCDS=?, pDiffUTR5=?, pDiffUTR3=?, GC=?, CpGn=?, CpGc=?, tstv=? " +
					"where UTid1=? and UTid2=?");
			
			int alignBases =  curScore[IDX_nCDSlenNoHang];  // bases with gap
			int alignCodons = curScore[IDX_nCodonsNoIndel]; // codons no gap
			
			int i=1;
			ps1.setInt(i++, alignBases);
			ps1.setDouble(i++, xPctCol(alignBases, cdsLen1)); // if use alignWithGaps, can be longer than len
			ps1.setDouble(i++, xPctCol(alignBases, cdsLen2));
			ps1.setInt(i++, curScore[IDX_nSNPs]);
			ps1.setInt(i++, curScore[IDX_gap]); 
			ps1.setInt(i++, curScore[IDX_open]); 
			
			ps1.setDouble(i++, alignCodons);
			ps1.setDouble(i++, xPctCol(curScore[IDX_cExact], 	alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_cSyn],		alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_4d],		alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_2d],		alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_cNonSyn],	alignCodons));
			
			ps1.setDouble(i++, xPctCol(curScore[IDX_aaExact],	alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_aaPos],		alignCodons));
			ps1.setDouble(i++, xPctCol(curScore[IDX_aaNeg],		alignCodons));
			
			ps1.setDouble(i++, xPctCol(curScore[IDX_nCDSdiffNoHang],  curScore[IDX_nCDSlenNoHang]));
			ps1.setDouble(i++, xPctCol(curScore[IDX_n5UTRdiffNoHang], curScore[IDX_n5UTRlenNoHang]));
			ps1.setDouble(i++, xPctCol(curScore[IDX_n3UTRdiffNoHang], curScore[IDX_n3UTRlenNoHang]));
			
			ps1.setDouble(i++, xJI(curScore[IDX_GC_B],   curScore[IDX_GC_E]));
			ps1.setDouble(i++, xJI(curScore[IDX_CpGn_B], curScore[IDX_CpGn_E]));
			ps1.setDouble(i++, xJI(curScore[IDX_CpGc_B], curScore[IDX_CpGc_E]));
			
			ps1.setDouble(i++, xTsTv());
			
			ps1.setInt(i++, idx1);
			ps1.setInt(i++, idx2);
			ps1.execute();
		}
		catch(Exception e) {ErrorReport.die(e, "computing stats");}
	}
	
	/*********************************************************************
	 * XXX OVERVIEW-INFO
	 *     called from ScoreCDS after computing stats with alignments
	 *     called from SumStats after computing stats with recreated alignments
	 */
	public String createSummary() {
		try {
			// denominators except for 5'UTR and 3'UTR
			long nCodons = sumAllPairs[IDX_nCodonsNoIndel];
		    long nSNPs =   sumAllPairs[IDX_nSNPs];
		    long nNoHang = sumAllPairs[IDX_nCDSlenNoHang];
			
			String sum = String.format(
				"%sAligned: %s   CDS: %sb   5UTR: %sb   3UTR: %sb",  Summary.padTitle, 
				Out.mText(cntCDS), Out.kMText(sumAllPairs[IDX_nCDSlenNoHang]),  
				Out.kMText(sumAllPairs[IDX_n5UTRlenNoHang]),
				Out.kMText(sumAllPairs[IDX_n3UTRlenNoHang]));
			
			Vector <String> sumLines = new Vector <String> ();
			   
			String [] fields = {"", "", "", "", "", ""};
			int [] just = {1,0, 1,0, 1,0};
			
			int nRow = 6;
		    int nCol=  fields.length;
		    String [][] rows = new String[nRow][nCol];   
		    
			int r=0, c=0;
		    rows[r][c] = "Codons";			rows[r++][c+1] =  Out.kMText(nCodons);
		    rows[r][c] = "Exact"; 			rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_cExact], nCodons);
		    rows[r][c] = "Synonymous"; 		rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_cSyn], nCodons);
		    rows[r][c] = "  Fourfold";		rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_4d], nCodons);
		    rows[r][c] = "  Twofold"; 		rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_2d], nCodons);
		    rows[r][c] = "Nonsynonymous"; 	rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_cNonSyn], nCodons);
		    
		    r=0; c=2;
		    rows[r][c] = "   Amino Acids"; 	rows[r++][c+1] =  "";
		    rows[r][c] = "   Exact"; 		rows[r++][c+1] =xAvgPctPrt(sumAllPairs[IDX_aaExact], nCodons);
		    rows[r][c] = "   " + bPos; 		rows[r++][c+1] =	xAvgPctPrt(sumAllPairs[IDX_aaPos], nCodons);
		    rows[r][c] = "   " + bNeg;  		rows[r++][c+1] =	xAvgPctPrt(sumAllPairs[IDX_aaNeg], nCodons);
		    rows[r][c] = "";					rows[r++][c+1] = "";
		    rows[r][c] = "";					rows[r++][c+1] = "";
		    
		    r=0; c=4;
		    rows[r][c] = "   Nucleotides"; rows[r++][c+1] =  "";
		    rows[r][c] = "   CDS  Diff";	  rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_nCDSdiffNoHang], nNoHang);
		    rows[r][c] = "        Gaps";   rows[r++][c+1] =  xAvgPctPrt(sumAllPairs[IDX_gap],   nNoHang);    
		       
		    rows[r][c] = "        SNPs";   rows[r++][c+1] =  xAvgPctPrt(nSNPs, nNoHang); 
		    
		    rows[r][c] = "   5UTR Diff";	  rows[r++][c+1] =   
		    		xAvgPctPrt(sumAllPairs[IDX_n5UTRdiffNoHang],   sumAllPairs[IDX_n5UTRlenNoHang]);
		    rows[r][c] = "   3UTR Diff";	  rows[r++][c+1] =   
		    		xAvgPctPrt(sumAllPairs[IDX_n3UTRdiffNoHang],   sumAllPairs[IDX_n3UTRlenNoHang]);
		 
			Out.makeTable(sumLines, nCol, nRow, fields, just, rows);
			sumLines.add("");
			
			String [] fields2 = {"", "Pos1", "Pos2", "Pos3", "Total", "","", "GC", "CpG-Nt", "CpG-Cd"};
			int [] just2 = {1,0,0,0,0,0,1,0, 0,0};
			nRow = 3;
		    nCol=  fields2.length;
		    rows = new String[nRow][nCol];
		    
		    rows[0][0] = "Transition"; 
		    long ts1 = sumAllPairs[IDX_ts1], ts2 = sumAllPairs[IDX_ts2], ts3 = sumAllPairs[IDX_ts3];
		    
		    rows[0][1] =  xAvgPctPrt(ts1, nSNPs);
		    rows[0][2] =  xAvgPctPrt(ts2, nSNPs);
		    rows[0][3] =  xAvgPctPrt(ts3, nSNPs);
		    long ts = (ts1+ts2+ts3);
		    rows[0][4] =  xAvgPctPrt(ts, nSNPs);
		    
		    rows[1][0] = "Transversion"; 
		    long tv1 = sumAllPairs[IDX_tv1], tv2 = sumAllPairs[IDX_tv2], tv3 = sumAllPairs[IDX_tv3];
		    rows[1][1] =  xAvgPctPrt(tv1, nSNPs);
		    rows[1][2] =  xAvgPctPrt(tv2, nSNPs);
		    rows[1][3] =  xAvgPctPrt(tv3, nSNPs);
		    long tv = (tv1+tv2+tv3);
		    rows[1][4] =  xAvgPctPrt(tv, nSNPs);
		    
		    rows[2][0] = "ts/tv"; 
		    rows[2][1] =  xDivPrt(sumAllPairs[IDX_ts1],sumAllPairs[IDX_tv1]);
		    rows[2][2] =  xDivPrt(sumAllPairs[IDX_ts2],sumAllPairs[IDX_tv2]);
		    rows[2][3] =  xDivPrt(sumAllPairs[IDX_ts3],sumAllPairs[IDX_tv3]);
		    rows[2][4] =  xDivPrt(ts, tv);
		    
		    // CASX 6/29/19  added CpG*2 since a site is 2-nt
		    rows[0][5] = "   ";
		    rows[0][6] = "Both";
		    rows[0][7] = xAvgPctPrt(sumAllPairs[IDX_GC_B],   nNoHang);
		    rows[0][8] = xAvgPctPrt(sumAllPairs[IDX_CpGn_B]*2, nNoHang);
		    rows[0][9] = xAvgPctPrt(sumAllPairs[IDX_CpGc_B]*2, nNoHang);
		    
		    rows[1][5] = "   ";
		    rows[1][6] = "Either";
		    rows[1][7] = xAvgPctPrt(sumAllPairs[IDX_GC_E],   nNoHang);
		    rows[1][8] = xAvgPctPrt(sumAllPairs[IDX_CpGn_E]*2, nNoHang);
		    rows[1][9] = xAvgPctPrt(sumAllPairs[IDX_CpGc_E]*2, nNoHang);
		   
		    rows[2][5] = "   ";
		    rows[2][6] = "Jaccard";
		    rows[2][7] = String.format("%5.2f", xJI(sumAllPairs[IDX_GC_B],   sumAllPairs[IDX_GC_E]));
		    rows[2][8] = String.format("%5.2f", xJI(sumAllPairs[IDX_CpGn_B], sumAllPairs[IDX_CpGn_E]));
		    rows[2][9] = String.format("%5.2f", xJI(sumAllPairs[IDX_CpGc_B], sumAllPairs[IDX_CpGc_E]));
		    Out.makeTable(sumLines, nCol, nRow, fields2, just2, rows);	
		    
			for (String l : sumLines) sum += l + "\n";
			return sum;
		} catch (Exception e) {ErrorReport.prtReport(e, "Writing summary for aa"); return "error";}
	}
	public String createOverhang() {
		String sum="   Alignments with overhang\n";
		String [] fields2 = {"", "Align", "Diff", "%"};
		int [] just2 = {1,0,0,0};
		int nRow = 3;
	    int nCol=  fields2.length;
	    String [][] rows = new String[nRow][nCol];
	    rows[0][0]="CDS";
	    rows[1][0]="5UTR";
	    rows[2][0]="3UTR";
	    rows[0][1]=Out.kMText(sumAllPairs[IDX_nCDSlenWithHang]);
	    rows[1][1]=Out.kMText(sumAllPairs[IDX_n5UTRlenWithHang]);
	    rows[2][1]=Out.kMText(sumAllPairs[IDX_n3UTRlenWithHang]);
	    rows[0][2]=Out.kMText(sumAllPairs[IDX_nCDSdiffWithHang]);
	    rows[1][2]=Out.kMText(sumAllPairs[IDX_n5UTRdiffWithHang]);
	    rows[2][2]=Out.kMText(sumAllPairs[IDX_n3UTRdiffWithHang]);
	    rows[0][3]=xAvgPctPrt(sumAllPairs[IDX_nCDSdiffWithHang],   sumAllPairs[IDX_nCDSlenWithHang]);
	    rows[1][3]=xAvgPctPrt(sumAllPairs[IDX_n5UTRdiffWithHang],   sumAllPairs[IDX_n5UTRlenWithHang]);
	    rows[2][3]=xAvgPctPrt(sumAllPairs[IDX_n3UTRdiffWithHang],   sumAllPairs[IDX_n3UTRlenWithHang]);
	    sum += Out.makeTable(nCol, nRow, fields2, just2, rows);	
	    
	    return sum;
	}
	/***********************************************************************
	 * XXX PairAlignText
	 ***********************************************************************/
	public void addHeader(int cdsMode, String alignSeq1, String alignSeq2, Vector <String> lines, String [] hangCol) {
		scoreCDS(alignSeq1, alignSeq2);
		
		curScore[IDX_nCDSlenNoHang] = alignSeq1.length();
		int diff=0;
		char [] c1 = alignSeq1.toCharArray();
		char [] c2 = alignSeq2.toCharArray();
		for (int i=0; i<c1.length; i++) if (c1[i]!=c2[i]) diff++;
		curScore[IDX_nCDSdiffNoHang] = diff;
		
		if (cdsMode==Share.CDS_MATCH)   headerCDS(lines, hangCol, true);
		else if (cdsMode==Share.CDS_AA) headerCDS(lines, hangCol, false);
		else if (cdsMode==Share.CDS_ND) headerND(lines, hangCol);
		else if (cdsMode==Share.CDS_TV) headerTV(lines, hangCol);
		else if (cdsMode==Share.CDS_CpG)headerCpG(lines, hangCol);
	}
	private void headerCDS(Vector <String> lines, String [] hangCol, boolean isCDS) {
		String r1c1 = String.format("%s %3s",  hangCol[0],"");
		String r2c1 = String.format("%s %3s",  hangCol[1],"");
		String r3c1 = String.format("%s %3s",  hangCol[2],"");
		 
		int c=curScore[IDX_nCodonsNoIndel];
		String r1c2 = String.format("%-8s %5d %4s","Calign:", c, "");		
		String r2c2 = String.format("%-8s %5d %4s","GapOpen:",curScore[IDX_open],"");
		String r3c2 = String.format("%-8s %5d %4s","Gaps:",  curScore[IDX_gap], "");
		
		String r1c3 = String.format("%-14s %4d %3s", "Codon exact:", curScore[IDX_cExact],""); 
		String r2c3 = String.format("%-14s %4d %3s", "Synonymous:", curScore[IDX_cSyn],""); 
		String r3c3 = String.format("%-14s %4d %3s", "Nonsynonymous:", curScore[IDX_cNonSyn],""); 
		
		String r1c4 = String.format("%-17s %3d %4s", "Amino exact:",   curScore[IDX_aaExact],""); 
		String r2c4 = String.format("%-17s %3d %4s", Globalx.blosumPos + ":",curScore[IDX_aaPos],"");
		String r3c4 = String.format("%-17s %3d %4s", Globalx.blosumNeg + ":", curScore[IDX_aaNeg],"");
	
		if (isCDS) {
			r1c3 = String.format("%-14s %4d %6s %3s", "Codon exact:", curScore[IDX_cExact],
					Out.perFtxtP(curScore[IDX_cExact], c), "");
			r2c3 = String.format("%-14s %4d %6s %3s", "Synonymous:", curScore[IDX_cSyn],
					Out.perFtxtP(curScore[IDX_cSyn], c), ""); 
			r3c3 = String.format("%-14s %4d %6s %3s", "Nonsynonymous:", curScore[IDX_cNonSyn],
					Out.perFtxtP(curScore[IDX_cNonSyn], c), ""); 
		}
		else {
			r1c4 = String.format("%-17s %3d %6s %4s", "Amino exact:",   curScore[IDX_aaExact],
					Out.perFtxtP(curScore[IDX_aaExact], c), "");
			r2c4 = String.format("%-17s %3d %6s %4s", Globalx.blosumPos + ":",curScore[IDX_aaPos],
					Out.perFtxtP(curScore[IDX_aaPos], c), "");
			r3c4 = String.format("%-17s %3d %6s %4s", Globalx.blosumNeg + ":", curScore[IDX_aaNeg],
					Out.perFtxtP(curScore[IDX_aaNeg], c), "");
		}
		lines.add(r1c1+r1c2+r1c3+r1c4);
		lines.add(r2c1+r2c2+r2c3+r2c4);
		lines.add(r3c1+r3c2+r3c3+r3c4);
	}
	public void headerND(Vector <String> lines, String [] hangCol) {
		String r1c1 = String.format("%s %3s",  hangCol[0],"");
		String r2c1 = String.format("%s %3s",  hangCol[1],"");
		String r3c1 = String.format("%s %3s",  hangCol[2],"");	
		
		int d=curScore[IDX_nCodonsNoIndel];
		String r1c2 = String.format("%-8s %5d %4s","Calign:", d, "");		
		String r2c2 = String.format("%-8s %5d %4s","GapOpen:",curScore[IDX_open],"");
		String r3c2 = String.format("%-8s %5d %4s","Gaps:",  curScore[IDX_gap], "");
		
		String r1c3 = String.format("%-14s %4d %6s %3s", 
				"Exact:", curScore[IDX_cExact], Out.perFtxtP(curScore[IDX_cExact], d), "");
		String r2c3 = String.format("%-14s %4d %6s %3s", 
				"Nonsynonymous:", curScore[IDX_cNonSyn], Out.perFtxtP(curScore[IDX_cNonSyn],d), "");
		String r3c3 = String.format("%-14s %4d %6s %3s", 
				"Synonymous:", curScore[IDX_cSyn], Out.perFtxtP(curScore[IDX_cSyn],d), "");
		
		String r1c4 = String.format("%-3s %4d %6s %3s", "4d:", 
				curScore[IDX_4d], Out.perFtxtP(curScore[IDX_4d],d),"");
		String r2c4 = String.format("%-3s %4d %6s %3s", "2d:", 
				curScore[IDX_2d], Out.perFtxtP(curScore[IDX_2d],d), "");
		String r3c4 = String.format("%-3s %4d %6s %3s", "xd:", 
				curScore[IDX_xd], Out.perFtxtP(curScore[IDX_xd],d),"");
		
		lines.add(r1c1+r1c2+r1c3+r1c4);
		lines.add(r2c1+r2c2+r2c3+r2c4);
		lines.add(r3c1+r3c2+r3c3+r3c4);
	}
	public void headerTV(Vector <String> lines, String [] hangCol) {
		String r1c1 = String.format("%s %3s",  hangCol[0],"");
		String r2c1 = String.format("%s %3s",  hangCol[1],"");
		String r3c1 = String.format("%s %3s",  hangCol[2],"");
		
		int ts = curScore[IDX_ts1] + curScore[IDX_ts2] + curScore[IDX_ts3];
		int tv = curScore[IDX_tv1] + curScore[IDX_tv2] + curScore[IDX_tv3];
		double tstv = (double)ts/ (double)tv;
		
		String r1c2 = String.format("%-8s %5d %4s", "NT Diff:", curScore[IDX_nCDSdiffNoHang], ""); // includes gaps
		String r2c2 = String.format("%-8s %5s %4s", "%diff:", Out.perFtxt(curScore[IDX_nCDSdiffNoHang], curScore[IDX_nCDSlenNoHang]), "");
		String r3c2 = String.format("%-8s %5s %4s", "", "", "");
		
		String r1c3 = String.format("%-8s %4d %4s","SNPs:",  curScore[IDX_nSNPs],"");
		String r2c3 = String.format("%-8s %4d %4s","GapOpen:", curScore[IDX_open], "");
		String r3c3 = String.format("%-8s %4d %4s","Gaps:",  curScore[IDX_gap], "");
		
		String r1c4 = String.format("%-6s %4.2f %3s",  "ts/tv:", tstv, "");
		String r2c4 = String.format("%-6s %4d %3s",    "ts:", ts, "");
		String r3c4 = String.format("%-6s %4d %3s",    "tv:", tv, "");
		
		String r1c5 = String.format("%4s %4s %4s", "pos1", "pos2", "pos3");
		String r2c5 = String.format("%4d %4d %4d   ", curScore[IDX_ts1], curScore[IDX_ts2], curScore[IDX_ts3]);
		String r3c5 = String.format("%4d %4d %4d   ", curScore[IDX_tv1], curScore[IDX_tv2], curScore[IDX_tv3]);
		
		lines.add(r1c1+r1c2+r1c3+r1c4+r1c5);
		lines.add(r2c1+r2c2+r2c3+r2c4+r2c5);
		lines.add(r3c1+r3c2+r3c3+r3c4+r3c5);
	}
	
	public void headerCpG(Vector <String> lines, String [] hangCol) {
		String r0c1 = String.format("%20s %3s",  "",        "");
		String r1c1 = String.format("%20s %3s",  hangCol[0],"");
		String r2c1 = String.format("%20s %3s",  hangCol[1],"");
		String r3c1 = String.format("%20s %3s",  hangCol[2],"");
		
		String r0c2 = String.format("%-11s %5s %4s",   "",           "",             "");
		String r1c2 = String.format("%-11s %5d %4s",   "GC Both:   ",   curScore[IDX_GC_B], "");
		String r2c2 = String.format("%-11s %5d %4s",   "GC Either: ", curScore[IDX_GC_E], "");
		String r3c2 = String.format("%-11s %5.3f %4s", "GC Jaccard:",    xJI(curScore[IDX_GC_B], curScore[IDX_GC_E]), "");
		
		String r0c3 = String.format("%-12s %5s %4s",   "All CpG","", "");
		String r1c3 = String.format("%-12s %5d %4s",   "CpG Both:   ",    curScore[IDX_CpGn_B], "");
		String r2c3 = String.format("%-12s %5d %4s",   "CpG Either: ",  curScore[IDX_CpGn_E],"");
		String r3c3 = String.format("%-12s %5.3f %4s", "CpG Jaccard:",     xJI(curScore[IDX_CpGn_B], curScore[IDX_CpGn_E]), "");
		
		String r0c4 = String.format("%-12s %5s %4s",   "By Codon",     "",  "");
		String r1c4 = String.format("%-12s %5d %4s",   "CpG Both:   ",     curScore[IDX_CpGc_B], "");
		String r2c4 = String.format("%-12s %5d %4s",   "CpG Either: ",   curScore[IDX_CpGc_E], "");
		String r3c4 = String.format("%-12s %5.3f %4s", "CpG Jaccard:",      xJI(curScore[IDX_CpGc_B], curScore[IDX_CpGc_E]), "");
		
		lines.add(r0c1+r0c2+r0c3+r0c4);
		lines.add(r1c1+r1c2+r1c3+r1c4);
		lines.add(r2c1+r2c2+r2c3+r2c4);
		lines.add(r3c1+r3c2+r3c3+r3c4);
		
	}
	
	public String codonMatch(int cdsMode, String codon1, String codon2) {
		String c=" ", x="";
		
		if (cdsMode==Share.CDS_CpG) { 
			char lastc1= ' ', lastc2=' ';
			
			for (int i=0; i<3; i++) {
				char c1 = codon1.charAt(i);
				char c2 = codon2.charAt(i);
				boolean cpg1 = (lastc1=='c' && c1=='g');
				boolean cpg2 = (lastc2=='c' && c2=='g');
				
				if (cpg1 || cpg2) {
					if (cpg1 && cpg2) x+=Share.CpG12;
					else if (cpg1 && !cpg2) x+=Share.CpG1;
					else if (!cpg1 && cpg2) x+=Share.CpG2;
				} else x+=" ";
				
				lastc1=c1; lastc2=c2;
			}
			return x;
		}
				
		if (codon1.contains(Share.gapStr)) return "   ";
		if (codon2.contains(Share.gapStr)) return "   ";
		
		char a1 = convert.codonToAA(codon1);
		char a2 = convert.codonToAA(codon2);
			
		if (cdsMode==Share.CDS_AA) {
			return " " + ScoreAA.getSubChar(a1, a2) + " ";
		}
		
		if (cdsMode==Share.CDS_ND) {
			if (a1!=a2) return " " + Share.NND + " ";
			if (codon1.equals(codon2)) return "   ";
			String d1 = foldN(a1,codon1);
			String d2 = foldN(a1,codon2); 
			if (d1.equals(d2)) {
				return " " + d1 + " ";
			}
			else return " " + Share.NxD + " ";
		}
		
		////////////////////////////////////////
		if (codon1.equals(codon2)) return "   ";
		
		if (cdsMode==Share.CDS_MATCH) {
			c=Share.AA_NEG;
			if (a1==a2)	  c= Share.CDS_SYM;
			else c=Share.CDS_NONSYM;
			
			for (int i=0; i<3; i++) {
				if (codon1.charAt(i)==codon2.charAt(i)) x+=" ";
				else x+=c;
			}
			return x;
		}
		
		if (cdsMode==Share.CDS_TV) {
			for (int i=0; i<3; i++) {
				if (codon1.charAt(i)==codon2.charAt(i)) x+=" ";
				else if (isTS(codon1.charAt(i), codon2.charAt(i)))x+=Share.TS;
				else x+=Share.TV;
			}
			if (a1!=a2) return x.toUpperCase(); 
			return x;
		}
		
		return "   ";
	}
	public void legendCDS(int cdsMode, Vector <String> lines) {
		lines.add("");
		if (cdsMode==Share.CDS_MATCH) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.CDS_SYM +     " = Synonymous codon");
			lines.add("        " + Share.CDS_NONSYM +  " = Nonsynonymous codon");
		}
		else if (cdsMode==Share.CDS_AA) {
			lines.add("LEGEND: ");
			lines.add("  AA char = AA match");
			lines.add("        " + Share.AA_POS +  " = " + Globalx.blosumPosLegend);
			lines.add("        " + Share.AA_NEG +  " = " + Globalx.blosumNeg);
		}
		else if (cdsMode==Share.CDS_ND) {
			lines.add("LEGEND: ");
			lines.add("         " + Share.NND + " = nd - non-degenerate       (not same AA, nonsynonymous)");
			lines.add("         " + Share.N2D + " = 2d - two-fold degenerate  (same AA, both 2d; any of the 2-3 bases in the ith position)");
			lines.add("         " + Share.N4D + " = 4d - four-fold degenerate (same AA, both 4d; any of the 4 bases in the ith position)");
			lines.add("         " + Share.NxD + " = xd - not 2d or 4d         (same AA, different degenerate)");
		}
		else if (cdsMode==Share.CDS_TV) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.TS + " = Transition   (ts) substitution of purine (ag) or pyrimidine (ct)");
			lines.add("        " + Share.TV + " = Transversion (tv) substitution of a purine with a pyrimidine (ac,at,gc,gt)");
			lines.add("        Upper case " + Share.TS.toUpperCase() + " or "
					+ Share.TV.toUpperCase() + " is a nonsynonymous codon");
		}
		else if (cdsMode==Share.CDS_CpG) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.CpG12 + " = both codons have CpG");
			lines.add("        " + Share.CpG1  + " = only one codon has CpG");
			lines.add("Note: CpG crossing codon boundaries are not shown.");
		}
	}
	/*****************************************
	 *  Formatting and sumAllAvg
	 ***********************/
	
	private String xDivPrt(long x, long y) {
		return String.format("%4.2f", Out.div(x, y));
	}
	private String xAvgPctPrt(long x, long y) {
		double pct = 0.0;
		if (x>0 && y>0) pct = ((double)x/(double)y) * 100.0;
		return String.format("%4.1f%s", pct, "%");
	}
	
	// for columns only
	private double xPctCol(int x, int y) {
		if (x<=0) return Globalx.dNoVal; // -2
		if (y<=0) return Globalx.dNoVal; // -2
		double p = ((double)x/(double)y)*100.0;
		return p;
	}
	private double xJI(int x, int y) {
		if (y==0 && x==0) return 1.0; 
		if (y==0 || y==0) return 0.0;
		return (((double) x/ (double)y));
	}
	private double xJI(long x, long y) {
		if (y==0 && x==0) return 1.0; 
		if (y==0 || y==0) return 0.0;
		return (((double) x/ (double)y));
	}
	private double xTsTv() {
		double ts = curScore[IDX_ts1]+curScore[IDX_ts2]+curScore[IDX_ts3];
		double tv = curScore[IDX_tv1]+curScore[IDX_tv2]+curScore[IDX_tv3];
		return (ts>0 && tv>0) ? ts/tv : 0.0;
	}	
	
	// Jaccard Index 
	private void ySumJI(double ji, int x, int y) {
		double p;
		if (y==0 && x==0) p = 1.0; 
		else if (y==0 || y==0) p = 0.0;
		else p = (((double) x/ (double)y));
		ji += p;
	}

	/*********************************************
	 * runMulti: PairStats to compute diff scores
	 */
	public void diffScoreAll(String [] name, PairAlignData cdsObj, PairAlignData utr5Obj, PairAlignData utr3Obj) {
		String [] crop = new String [2];
		int minLen = Share.minAlignLen;
		
		curScore[IDX_nCDSlenWithHang] =  cdsObj.getAlignFullSeq1().length();
		curScore[IDX_nCDSdiffWithHang] = diffScore(cdsObj.getAlignFullSeq1(), cdsObj.getAlignFullSeq2());
		crop[0]=cdsObj.getAlignCropSeq1();
		crop[1]=cdsObj.getAlignCropSeq2();
		
		if (crop[0].length()>minLen && crop[1].length()>minLen) { 
			curScore[IDX_nCDSdiffNoHang] = diffScore(crop[0], crop[1]);
			curScore[IDX_nCDSlenNoHang] =  crop[0].length();	
		}
		else {
			curScore[IDX_nCDSdiffNoHang] = curScore[IDX_nCDSlenNoHang] = Globalx.iNoVal;	
		}
		
		curScore[IDX_n5UTRlenWithHang]  = utr5Obj.getAlignFullSeq1().length();
		curScore[IDX_n5UTRdiffWithHang] = diffScore(utr5Obj.getAlignFullSeq1(), utr5Obj.getAlignFullSeq2());
		crop[0]=utr5Obj.getAlignCropSeq1();
		crop[1]=utr5Obj.getAlignCropSeq2();
		
		if (crop[0].length()>minLen && crop[1].length()>minLen) { 
			curScore[IDX_n5UTRdiffNoHang] = diffScore(crop[0],crop[1]);
			curScore[IDX_n5UTRlenNoHang] =  crop[0].length();	
		}
		else { 
			curScore[IDX_n5UTRdiffNoHang] = curScore[IDX_n5UTRlenNoHang] = Globalx.iNoVal;
		}
		
		curScore[IDX_n3UTRlenWithHang] = utr3Obj.getAlignFullSeq1().length();
		curScore[IDX_n3UTRdiffWithHang] = diffScore(utr3Obj.getAlignFullSeq1(), utr3Obj.getAlignFullSeq2());
		
		crop[0]=utr3Obj.getAlignCropSeq1();
		crop[1]=utr3Obj.getAlignCropSeq2();
		if (crop[0].length()>minLen && crop[1].length()>minLen) { 	
			curScore[IDX_n3UTRdiffNoHang] = diffScore(crop[0], crop[1]);
			curScore[IDX_n3UTRlenNoHang] = crop[0].length();
		}
		else {
			curScore[IDX_n3UTRdiffNoHang] = curScore[IDX_n3UTRlenNoHang] = Globalx.iNoVal;
		}
		crop=null;
	}
	private int diffScore(String seq1, String seq2) {
		try {
			if (seq1.length()==0 || seq2.length()==0) return 0;
			
			int diff=0;
			
			char [] c1 = seq1.toCharArray();
			char [] c2 = seq2.toCharArray();
			
			for (int i=0; i<c1.length; i++) {
				if (c1[i]!=c2[i]) diff++;
			}
			return diff;
		}
		catch(Exception e) {ErrorReport.die(e, "Scoring differences in NT sequencs"); return -3;}
	}
		
	/*************************************************************/
	private ScoreAA convert = new ScoreAA();
	
	// for summary only, 
	PairAlignData cropObj = new PairAlignData ();
			
	 // for saveStatsToDB for a pair
	private int [] curScore=null;     
	private int cdsLen1=0, cdsLen2=0; // actual lengths
	
	// for summary
	private long [] sumAllPairs=null;
	private double sumJI_CpG=0.0, sumJI_CG, sumJI_CpGc=0.0;
	
	private int cntCDS=0;
}
