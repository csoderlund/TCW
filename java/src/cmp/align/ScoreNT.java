package cmp.align;

/**********************************************************
 * viewMulti.PairAlignTxt
 */
import java.util.Vector;

import util.methods.Out;

public class ScoreNT {
	
	public String score(int mode, String crop1, String crop2) {
		alignLen=crop1.length();
		StringBuffer sb = new StringBuffer (crop1.length());
		boolean inGap=false;
		char lastc1=' ', lastc2=' ';
		
		for (int i=0;i<alignLen; i++) {
			char c1 = crop1.charAt(i);
			char c2 = crop2.charAt(i);
			
			if (c1!=c2) cntDiff++;
			
			boolean cpg1 = (lastc1=='c' && c1=='g'); 
			boolean cpg2 = (lastc2=='c' && c2=='g');
			lastc1 = c1; lastc2=c2;
			if (cpg1 || cpg2) cntCpG++;
			
			boolean gc1 =  (c1=='c' || c1=='g');
			boolean gc2 =  (c2=='c' || c2=='g');
			if (gc1  || gc2)  cntGC++;
			
			if (c1==Share.gapCh || c2==Share.gapCh) {
				if (!inGap) {
					cntOpen++; 
					inGap=true;
				} 
				else cntGap++;
				lastc1 = lastc2= ' ';
			}
			else {
				cntNoGap++;
				
				if (c1!=c2) {
					if  (isTS(c1, c2)) cntTs++;
					else cntTv++; 
					cntMis++;
				}
				else {
					cntMat++;
				}
				inGap=false;
			}
			// match line for NT alignment (using cdsModes - though appropriate for NT too.
			switch (mode) {
	   		case Share.CDS_MATCH: 
	   			if (c1==c2) sb.append(" ");
    	   		else if (c1==Share.gapCh || c2==Share.gapCh) sb.append(" ");
    	   		else sb.append(Share.NT_MM);
	   			break;
	   		case Share.CDS_TV:
	   			if (c1==c2) sb.append(" ");
	   			else if (c1==Share.gapCh || c2==Share.gapCh) sb.append(" ");
    	   		else if (isTS(c1, c2)) sb.append(Share.TS);
    	   		else sb.append(Share.TV);
	   			break;
	   		case Share.CDS_CpG:
    			if (cpg1 && cpg2) sb.append(Share.CpG12);
    			else if (cpg1)    sb.append(Share.CpG1);
    			else if (cpg2)    sb.append(Share.CpG2);
    			else sb.append(" ");
    			break;
    		default:
    			Out.prt("Add Header NT no " + mode);
	   		}
		}
		return  sb.toString();
	}
	private static boolean isTS(char c1, char c2) {
		if      (c1=='a' && c2=='g') return true;
		else if (c1=='g' && c2=='a') return true;
		else if (c1=='c' && c2=='t') return true;
		else if (c1=='t' && c2=='c') return true;
		else return false;
	}
	public void addHeader(Vector <String> lines, String [] hangCol) {
		// Align Match    GapOpen   ts		
		// GC    Diff     Extend    tv	
		// CpG   Gap  	  Total     ts/tv
		
		String r1c1 = String.format("%s %3s",  hangCol[0],"");
		String r2c1 = String.format("%s %3s",  hangCol[1],"");
		String r3c1 = String.format("%s %3s",  hangCol[2],"");
		
		double tstv = (cntTv==0) ? 0.0 : (double) cntTs/ (double) cntTv;
		
		int gaps = cntOpen+cntGap;
		int mmm = cntDiff-gaps;
		
		String r1c2 = String.format("%-8s %5d %3s", "NT Diff:", cntDiff, ""); // includes gaps
		String r2c2 = String.format("%-8s %5s %3s", "%Diff:", Out.perFtxtNo(cntDiff, alignLen), "");
		String r3c2 = String.format("%-8s %5d %3s", "Match:", cntMat, "");
		
		String r1c3 = String.format("%-9s %5d %4s", "SNPs:", mmm, ""); // corresponds to '|'
		String r2c3 = String.format("%-9s %5d %4s", "GapOpen:", cntOpen, "");
		String r3c3 = String.format("%-9s %5d %4s", "Gaps:", gaps, "");
		
		String r1c4 = String.format("%-6s %4d %4s",   "ts:", cntTs, "");
		String r2c4 = String.format("%-6s %4d %4s",   "tv:", cntTv, "");
		String r3c4 = String.format("%-6s %4.2f %4s", "ts/tv:", tstv, "");
		
		String r1c5 = String.format("%-6s %3s %4s", "Either", "", "");	
		String r2c5 = String.format("%-6s %3d %4s", "GC:",   cntGC, "");
		String r3c5 = String.format("%-6s %3d %4s", "CpG:", cntCpG, "");	
		
		
		lines.add(r1c1+r1c2+r1c3+r1c4+r1c5);
		lines.add(r2c1+r2c2+r2c3+r2c4+r2c5);
		lines.add(r3c1+r3c2+r3c3+r3c4+r3c5);
		lines.add("");
	}
	public void legendNT(int cdsMode, Vector <String> lines) {
		if (cdsMode==Share.CDS_MATCH) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.NT_MM + " = SNP");
		}
		else if (cdsMode==Share.CDS_TV) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.TS + " = Transition (ts)  ");
			lines.add("        " + Share.TV + " = Transversion (tv)");
		}
		else if (cdsMode==Share.CDS_CpG) {
			lines.add("LEGEND: ");
			lines.add("        " + Share.CpG12 + " = both sequences CpG");
			lines.add("        " + Share.CpG1  + " = one  sequence  CpG");
		}
		lines.add("        UTRs are in upper case");
	}
	int cntGap=0, cntOpen=0, cntMat=0, cntMis=0, cntTv=0, cntTs=0;
	int cntGC=0, cntG=0, cntC=0, cntCpG=0, cntNoGap=0, alignLen=0, cntDiff=0;
}
