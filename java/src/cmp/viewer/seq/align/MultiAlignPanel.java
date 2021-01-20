package cmp.viewer.seq.align;

/*********************************************************
 * Draw the multi alignment
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Vector;
import javax.swing.JFrame;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;
import cmp.align.*;
import cmp.database.Globals;

public class MultiAlignPanel extends BaseAlignPanel {
	private static final long serialVersionUID = 8622662337486159085L;
	
	public MultiAlignPanel(MultiViewPanel parentPanel, boolean isAA, MultiAlignData alignData) {
		super ();
		super.setBackground(Globals.BGCOLOR );
		super.setLayout( null );
		
		viewPanel =  parentPanel;
		seqArr =     alignData.getAlignSeq();
		if (seqArr.length==0) {
			Out.PrtError("Bad MSA");
			return;
		}
		seqNameArr = alignData.getSequenceNames();
		colScore1 =  alignData.getColScores1(); // CAS313
		colScore2 =  alignData.getColScores2();
		
		isDNA=!isAA;
		
		dRulerHeight = super.getTextWidth( "999999" );
		
		init();
		initColInfo();
	}
	
	//---------------------------------JPanel Over-rides-------------------------------------//
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		
		if(super.getViewMode() == TEXTMODE) {
			double width = getTextWidth("?");

			g2.setColor( selectColor );
			for(int x=0; x<colSelect.length; x++) {
				if(colSelect[x]) {
					double pos = calculateWriteX(x);
					g2.fill( new Rectangle2D.Double( pos, dPAD, width, dFrameHeight) );
				}
			}			
		}
		if (hasSelection()) {// Outline whole thing
			g2.setColor( selectColor );
			
			for(int x=0; x<seqSelect.length; x++) {
				if (seqSelect[x]) {
					g2.fill( new Rectangle2D.Double( dPAD, topSelect[x], 
							getFrameWidth(), lowSelect[x]-topSelect[x]) );
				}
			}
			g2.setColor( Color.BLACK );
		}
		g2.setColor( borderColor );
		
		Rectangle2D outsideBox = new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );		
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double(dPAD, dDivider1Y, getFrameWidth()+dPAD, dDivider1Y ) );
		
		super.drawRuler( g2, 0, dRulerTop, dRulerLow );

		for(int x=0; x<seqArr.length; x++) {
			super.drawName(g2, seqNameArr[x], dPAD + nInsetGap, seqTop[x] );
			super.drawSequence (g2, seqNameArr[x], seqArr[x], seqTop[x], seqLow[x], 
					seqStarts[x], seqStops[x], (x==0), isDNA , false /* isPair */);
		}
	}
	public Dimension getMaximumSize(){
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	public Dimension getPreferredSize() {
		int nWidth  = nPAD + (int)getFrameWidth() + nPAD;
		int nHeight = nPAD + (int)dFrameHeight    + nPAD;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//---------------------------------AlignBasePanel Over-rides-------------------------------------//
	
	protected char getChar(boolean bFirst, int nPos) { // nPos char of consensus sequence; ignore bFirst
		if (nPos>=seqArr[0].length()) return ' ';
		return seqArr[0].charAt(nPos);
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if (nPos < seqStarts[0] || nPos >seqStops[0]) return true;
		return false;
	}
	
	//---------------AlignMultiViewPanel and Over-rides -------------------/
	public void refreshPanels ( )  { 
		alignBox = new Rectangle2D.Double ( dPAD, dPAD,  getFrameWidth (), dFrameHeight );
	}
	public void selectNoRows () {	
		for(int x=0; x<numSeq; x++) seqSelect[x] = false;
		repaint ();
	}
	public void selectNoColumns () {
		for(int x=0; x<colSelect.length; x++) colSelect[x] = false;
		repaint();
	}
	public boolean hasSelection () {
		if (seqSelect==null) return false; // happens when bad MSAdb
		
		boolean retVal = false;
		for(int x=0; x<seqSelect.length && !retVal; x++)
			retVal = seqSelect[x];
		return retVal;
	}
	public void handleClick( MouseEvent e, Point localPoint ) {
		String msg="";
		
        if ( alignBox.contains(localPoint) ) {  
        	 // Column
    		if(super.getViewMode() == TEXTMODE) {
    			int xPos = super.calculateIndexFromWriteX ( localPoint.x );
    			if(xPos >= 0 && xPos < numCol) {
        			selectNoColumns();
    				colSelect[xPos] = true;
    				msg = colInfo[xPos];
    			}
    		}
            // Row
        	if (topSelect[0] <= localPoint.y && localPoint.y <= lowSelect[numSeq-1]){
        		selectNoRows();
    			
    			for(int x=0; x<numSeq; x++) {
        			if (topSelect[x] <= localPoint.y && localPoint.y <= lowSelect[x] ) {
        				seqSelect[x] = true; 
        				if (!seqNameArr[x].equals(Globals.MSA.consName)) {
        					msg +=  "    Seq: " + seqNameArr[x];
        				}
        			}
    			}
    		}
        }
        if (msg.contentEquals("")) {
            selectNoRows ();
            selectNoColumns ();
        }	
        viewPanel.updateInfo(msg); // sets back to headerline if ""
        
		repaint();
	}
	//--------------- multiViewPanel ----------------/
	public void showScores(JFrame theMainFrame, String s1, String s2, String header) {
	try {
	// create header lines
		String title="MSA_Col_Scores";
		String line1 = "#Seqs: " + (numSeq-1) + "   #Columns: " + numCol;
		String line2 = "";
		if (header.startsWith("Cluster") && header.contains(";")) {
			title = header.substring(0, header.indexOf(";")) + " scores";
			
			if (header.contains("(") && header.endsWith(")")) {
				String s = header.substring(header.lastIndexOf("(")+1, header.lastIndexOf(")"));
				line2 =  "Global: " + s;
			}
		}
		line2= "Score1: " + s1 + "  Score2: " + s2 + "    " + line2;
		if (isDNA) line2="Nucleotide - no scores";
		
	// popup
		UserPrompt pObj = new UserPrompt(theMainFrame, "MSA_Col_Scores");
		pObj.setVisible(true);
		String dir = System.getProperty("user.dir")+ "/" + Globalx.ALIGNDIR;
		String fileName = title.replace(" ","_"); // no ':' allowed either
		if (!pObj.getAction(dir, fileName)) return;
			
	// create vector
		Vector <String> lines = new Vector <String> ();
		lines.add(line2);
		lines.add(line1);
		
		if (isDNA)
			lines.add(String.format("%6s   %s", "Column", "Composition")); 
		else 
			lines.add(String.format("%6s    %s %s    %s", "Column", "Score1", "Score2", "Composition")); 
		for (int c=0; c < numCol; c++) lines.add(colInfo[c]); // formated in init
		
		String [] alines;
		if (pObj.isPopUp() && numCol>1000)  {
			alines = merge(lines, 2);
		}
		else {
			alines = new String [lines.size()];
			lines.toArray(alines);
		}

		pObj.doOutAction(alines);
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Show scores");};
	}
// over 2000 lines can hang VCM
	private String [] merge(Vector <String> lines, int colHead) {
		try {
			int mlen = (lines.size()/2)+colHead;
			String [] aLines = new String [mlen];
			
			for (int i=0; i<colHead; i++) aLines[i] = lines.get(i);
			
			int maxCol=0;
			for (int i=colHead; i<mlen; i++) {
				maxCol = Math.max(maxCol, lines.get(i).length());
				aLines[i]="";
			}
		
			String head = lines.get(colHead);
			for (int i=head.length(); i<maxCol; i++) head += " ";
			aLines[colHead] = head + "     " + lines.get(colHead);
			
			for (int i=colHead+1; i<mlen; i++) {
				aLines[i] = lines.get(i);
				for (int k=lines.get(i).length(); k<maxCol; k++) aLines[i] += " "; // pad
			}
			for (int i=mlen, j=colHead+1; i<lines.size() && j<mlen; i++, j++) {
				aLines[j] += "     " + lines.get(i);
			}
			return aLines;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Split lines"); return new String [1];}
	}
	//--------------- private -----------------------/

	private double getFrameWidth ( ) {
		double dWidth = dSeqStart + super.getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	
	private void init() {
	try {	
		int r=0, c=0;
		
		numSeq = seqArr.length;
		numCol = seqArr[0].length(); // all aligned sequences are the same length
		
		seqStarts = new int[numSeq];
		seqStops =  new int[numSeq];
		
		seqTop =    new double[numSeq];
		seqLow = 	new double[numSeq];
		topSelect = new double[numSeq];
		lowSelect = new double[numSeq];
		seqSelect = new boolean[numSeq];
		
		colSelect = new boolean[numCol];
		colInfo   = new String[numCol];
		
	// Gap start and stop of each sequence
		
		for(r=0; r<numSeq; r++) { // consensus is used for determining hanging gaps
			for(c = 0; c <seqArr[r].length() && seqArr[r].charAt(c)==Globals.gapCh; c++);
			seqStarts[r]=c;
					
			for(c=seqArr[r].length()-1; c>=0 && seqArr[r].charAt(c)==Globals.gapCh; c--);
			seqStops[r]= (c!=0) ? c : seqArr[r].length()-1;
		}
		super.setIndexRange ( 0, numCol );
	
	// CAS313 - trim - MAFFT can align a few AA between long stretches of gaps, hence, heuristics to skip them based on consensus.
		int [] t = ScoreMulti.trimCoords(seqArr[0]);
		super.setTrimRange ( t[0], t[1] ); 
		
		// CAS313 most of this was run for every display in setZoom (which is now deleted).		
		dFrameHeight = nInsetGap * ((double)numSeq) + nRowHeight * ((double)numSeq) + // Sequences
				     + nInsetGap * ((double)numSeq) + dRulerHeight; 		// Ruler
		dDivider1Y 		= dPAD + dHeaderHeight;
		dRulerTop 		= dDivider1Y + nInsetGap;
		dRulerLow 		= dRulerTop + dRulerHeight;
		
		double top 		= dRulerLow + nInsetGap;
		dSeqStart = 0.0;
		
		for(r=0; r < numSeq; r++) {
			seqTop[r]    = top;
			seqLow[r] = seqTop[r] + nRowHeight;
			
			topSelect[r]    = seqTop[r]    - nInsetGap / 2.0;
			lowSelect[r] = seqLow[r] + nInsetGap / 2.0;
			
			top = seqLow[r] + nInsetGap;
			
			dSeqStart = Math.max(dSeqStart, super.getTextWidth(seqNameArr[r])); // length of name column
			seqSelect[r] = false;
		}
		dSeqStart += nInsetGap * 2.0d + nPAD;
		super.setMinPixelX ( dSeqStart );
	}
	catch (Exception e) {Out.prt("MultiAlign");ErrorReport.reportError(e);}
	}
	private void initColInfo() {
	try {
		int c,r;
		HashMap <Character, Integer> aaMap = new HashMap <Character, Integer> ();
		TreeSet <String> prtSet = new TreeSet <String> ();
		
		String [] score1=null;
		String [] score2=null;
		if (colScore1!=null) {
			 score1 = colScore1.split(",");
			 score2 = colScore2.split(",");
		}
		for(c=0; c<numCol; c++) { 
			colSelect[c] = false;
			
		// count residues - duplicated in ScoreMulti to write to file
			aaMap.clear(); prtSet.clear();
			
			for (r=1; r<numSeq; r++) {
				char a = seqArr[r].charAt(c);
				if (a==Globalx.gapCh) 
					if (c < seqStarts[r] || c > seqStops[r]) a=Globalx.hangCh;
				
				if (aaMap.containsKey(a)) aaMap.put(a, aaMap.get(a)+1);
				else aaMap.put(a, 1);
			}
			for (char a : aaMap.keySet()) 
				prtSet.add(String.format("%02d:%c", aaMap.get(a), a)); // need leading zeros
			
			String colCh=null;
			for (String info : prtSet) {
				if (info.startsWith("0")) info = info.substring(1);
				if (colCh == null) colCh = info;
				else               colCh = info + ", " + colCh; // treemap sorts descending
			}
			
		// create info string
			String col = "#" + c;
			if (score1!=null && c<score1.length)  {
				String s1 = score1[c].trim().replace(".000","    ");
				String s2 = score2[c].trim().replace(".000","    ");
				colInfo[c] = String.format("%-6s  %7s %7s     %s", col, s1, s2, colCh);
			}
			else 
				colInfo[c] = String.format("-%6s   %s", col,  colCh);
		}
	}
	catch (Exception e) {Out.prt("MultiAlign initColInfo");ErrorReport.reportError(e);}
	}
	/****************************************************************************/
	private MultiViewPanel viewPanel=null;
	private boolean isDNA = true;
	private int numSeq=0, numCol=0; // numSeq includes consensus
	
	private String [] seqArr = null;
	private String [] seqNameArr = null;
	private String colScore1, colScore2;
	
	// arrays allocated in init
	private int [] seqStarts = null;
	private int [] seqStops = null;
	private String [] colInfo = null;
    private boolean [] seqSelect = null;
    private boolean [] colSelect = null;
	private Rectangle2D alignBox = new Rectangle2D.Double (0,0,0,0);
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	private double dFrameHeight = 0, dDivider1Y = 0;
	private double dRulerTop = 0, dRulerLow = 0;
	
	private double [] seqTop = null;
	private double [] seqLow = null;
	
	private double [] topSelect = null;
	private double [] lowSelect = null;
	
	private double dHeaderHeight = 0, dRulerHeight = 0, dSeqStart = 0;
	
	private final int nPAD=10;
	private final double dPAD=10.0;	
	private final int nInsetGap = 5;
	private final int nRowHeight = 15;
}
