package cmp.viewer.align;

/****************************************************
 * PairViewPanel calls this to draw the graphics of one pair, either CDS, NT, or AA
 * The alignment has already been done and the results are in PairAlignData
 * (note: MemAlignPanel is to draw one of many pairs).
 * 
 * BUG: ruler is one off. Color doesn't show until click in frame
 * Then add ORF shading.
 * Then make graphic tick marks ever other one.
 * 
 * Problem: 
 * If I setLayout with BoxLayout, only the information lines shows, but I get the scroller
 * If I setLayout(null), I get the information line and graphics, but no scroller 
 * I removed the thread, and it quite showing anything. I can't even move the code around 
 * a bit without it stop working
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import util.ui.MultilineTextPanel;
import cmp.align.PairAlignData;
import cmp.viewer.MTCWFrame;
import util.database.Globalx;
import util.methods.ErrorReport;

public class Pair3AlignPanel  extends BaseAlignPanel
{
	private static final char gapCh=Globalx.gapCh;
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	private static final Font textFont = new Font("Sans", Font.PLAIN, 11);
	private static boolean isPair=true;
	
	public Pair3AlignPanel ( MTCWFrame parentFrame, PairAlignData theAlignment){		
		super (  );
		super.setBackground( Globalx.BGCOLOR );
		super.setLayout( null );
				
		alignDataObj = theAlignment;
		
		init();
	}
	
	//---------------------------------JPanel Over-rides-------------------------------------//
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
	
		Rectangle2D outsideBox =   new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );
		
		g2.setColor( borderColor );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dPAD, dDivider1Y, dPAD + getFrameWidth ( ), dDivider1Y ) );
	
		drawRuler( g2, 0, dRulerTop, dRulerLow );
		
		boolean isFirst=true;
		super.drawName( g2, seq1Label, dPAD + nInsetGap, dSeq1Top );
		super.drawSequence ( g2, seq1Label, alignSeq1, dSeq1Top, dSeq1Low, nStart1, nStop1, isFirst, isNT, isPair); 
		
		isFirst=false;
		super.drawName( g2, seq2Label, dPAD + nInsetGap, dSeq2Top );
		super.drawSequence ( g2, seq2Label, alignSeq2, dSeq2Top, dSeq2Low,  nStart2, nStop2, isFirst , isNT, isPair); 
	}
	
	public Dimension getMaximumSize() {
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize() {
		int nWidth =  nGAP + (int)getFrameWidth() + nGAP;
		int nHeight = nGAP + (int)dFrameHeight + nGAP;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//---------------------AlignBasePanel Over-rides-------------------------------------//
	
	protected char getChar(boolean bFirst, int nPos) { 
		if (bFirst) return alignSeq2.charAt( nPos );
		else return alignSeq1.charAt( nPos );
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if( bFirst && (nPos < nStart2 || nPos > nStop2)) return true;
		if(!bFirst && (nPos < nStart1 || nPos > nStop1)) return true;
		return false;
	}
	
	//------------------- Pair3AlignPanel calls -----------------------//
	
	public PairAlignData getAlignData() { 
		return alignDataObj;
	}
	public void addDescLines() {
		try {
			if (headerPanel!=null) remove (headerPanel);
			
			headerPanel = new MultilineTextPanel ( textFont, descLines, nInsetGap, (int)getFrameWidth()-2, 1); 
			headerPanel.setBackground( Globalx.BGCOLOR );  
			add ( headerPanel );  
			
			headerPanel.setLocation( nGAP + 1, nGAP + 1 );		
			dHeaderHeight = headerPanel.getHeight() + 1;
			
			initBoundaries();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	//------------------- private -----------------------/
	private double getFrameWidth ( ){
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	
	private void init() {
	try {
		descLines = alignDataObj.getDescLines();
		isNT = alignDataObj.isNT();	
		dRulerHeight = getTextWidth( "999999" );
		
		alignSeq1 = alignDataObj.getAlignFullSeq1();
		for (nStart1=0; nStart1 < alignSeq1.length() && alignSeq1.charAt(nStart1) == gapCh; nStart1++);
		for (nStop1=alignSeq1.length()-1; nStop1 >= 0 && alignSeq1.charAt(nStop1) == gapCh; nStop1--);
		
		alignSeq2 = alignDataObj.getAlignFullSeq2();
		for (nStart2=0; nStart2 < alignSeq2.length() && alignSeq2.charAt(nStart2) == gapCh; nStart2++);
		for (nStop2=alignSeq2.length() - 1; nStop2 >= 0 && alignSeq2.charAt(nStop2) == gapCh; nStop2--);
		
		super.setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );
		
		super.setTrimRange(Math.max(nStart1, nStart2), Math.min(nStop1, nStop2)); // CAS313 add
		
		seq1Label = alignDataObj.getSeqID1();
		seq2Label = alignDataObj.getSeqID2();
		
		dSequenceStart = Math.max( getTextWidth( seq1Label ), getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nGAP;
		super.setMinPixelX ( dSequenceStart );
		
		initBoundaries();
	}
	catch (Exception e) {ErrorReport.prtReport(e, "init pair 3");}
	}
	private void initBoundaries() {
		dFrameHeight =  dHeaderHeight 						// Header
				 + nInsetGap * 2.0d + nRowHeight * 2.0d +   // Sequences
				 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y =   		dPAD + dHeaderHeight;
		dRulerTop =    	dDivider1Y + nInsetGap;
		dRulerLow = 	dRulerTop + dRulerHeight;
		dSeq1Top =     	dRulerLow + nInsetGap;
		dSeq1Low =  	dSeq1Top + nRowHeight;
		dSeq2Top =     	dSeq1Low + nInsetGap;
		dSeq2Low =  	dSeq2Top + nRowHeight;
	}
	/******************************************************/
	private PairAlignData alignDataObj=null;
	private String alignSeq1 = "", alignSeq2 = "";
	private int nStart1 = 0, nStop1 = 0, nStart2 = 0, nStop2 = 0;
	private String seq1Label = "", seq2Label = "";
	private Vector <String> descLines;
	private boolean isNT = true;
	
    private MultilineTextPanel headerPanel=null;
	
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	private double dFrameHeight = 0, dDivider1Y = 0;
	private double dRulerTop = 0, dRulerLow = 0;
	private double dSeq1Top = 0, dSeq1Low = 0;
	private double dSeq2Top = 0, dSeq2Low = 0;

	private double dHeaderHeight = 0, dRulerHeight = 0, dSequenceStart = 0;
	
	private final int nInsetGap = 5;
	private final int nRowHeight = 15;
	private final int nGAP=10;
	private final double dPAD=10;
	 
    private static final long serialVersionUID = 1;
}
