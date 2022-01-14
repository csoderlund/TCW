package cmp.viewer.align;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import util.methods.ErrorReport;
import util.ui.MultilineTextPanel;
import cmp.align.PairAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class PairNAlignPanel extends BaseAlignPanel
{
	private static final char gapCh=Globals.gapCh;
	private static final boolean isPair=true;
	
	// gets called first with the nucleotide alignment, then the AA alignment
	public PairNAlignPanel ( MTCWFrame parentFrame, PairAlignData theAlignment, boolean isNT)
	{		
		// Setup the base class
		super (  );
		super.setBackground( parentFrame.getSettings().getFrameSettings().getBGColor() );
		super.setLayout( null );

		isDNA=isNT;
		alignDataObj = theAlignment;
		
		init();
	}
	
	//---------------------------------JPanel Over-rides-------------------------------------//

	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dPAD - 2, dPAD - 2, getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() ) {// Outline whole thing
			Stroke oldStroke = g2.getStroke();
			g2.setColor( selectColor );
			g2.setStroke( new BasicStroke (3) );
			g2.draw( highlightBox );
			g2.setStroke ( oldStroke );
			// getFrameRight() = dPAD + getFrameWidth ( ); changed to getFrameWidth
			if ( bSeq1Selected )
				g2.fill( new Rectangle2D.Double( dPAD, dSelectRow1Top, getFrameWidth(), dSelectMid - dSelectRow1Top ) );		

			if ( bSeq2Selected )
				g2.fill( new Rectangle2D.Double( dPAD, dSelectMid, getFrameWidth(), dSelectRow2Bottom - dSelectMid ) );					
			
			g2.setColor( Color.BLACK );
		}
			
		g2.setColor( borderColor );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dPAD, dDivider1Y, dPAD + getFrameWidth ( ), dDivider1Y ) );
		
		super.drawRuler( g2, 0, dRulerTop, dRulerBottom );
		
		boolean isFirst=true;
		super.drawName( g2, seq1Label, dPAD + nInsetGap, dSeq1Top );
		drawSequence  ( g2, seq1Label, alignSeq1, dSeq1Top, dSeq1Bottom , nTrimStart1, nTrimStop1, isFirst, isDNA, isPair);
		
		isFirst=false;
		super.drawName( g2, seq2Label, dPAD + nInsetGap, dSeq2Top );
		drawSequence ( g2, seq2Label, alignSeq2, dSeq2Top, dSeq2Bottom, nTrimStart2, nTrimStop2, isFirst, isDNA , isPair);
	}
	public Dimension getMaximumSize() {
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	public Dimension getPreferredSize(){
		int nWidth = nGAP + (int)getFrameWidth ( ) + nGAP;
		int nHeight = nGAP + (int)dFrameHeight + nGAP;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//---------------------------------AlignBasePanel Over-rides-------------------------------------//
	protected char getChar(boolean bFirst, int nPos) { 
		if (bFirst) return alignSeq2.charAt( nPos );
		else        return alignSeq1.charAt( nPos );
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if( bFirst && (nPos < nTrimStart2 || nPos > nTrimStop2)) return true;
		if(!bFirst && (nPos < nTrimStart1 || nPos > nTrimStop1)) return true;
		return false;
	}
	
	//--------------- PairNViewPanel calls and Over-rides ---------------------------//
	public void refreshPanels ( ) { 
		selectionBox = new Rectangle2D.Double ( dPAD, dPAD,  getFrameWidth (), dFrameHeight );
	};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	//-------------------------- AlignPairViewNPanel -----------------------------/
	
	public Vector<String> getSelectedSeqIDs () {
		Vector<String> retVal = new Vector<String> ();
		if ( seq1Label != null && bSeq1Selected ) retVal.add( seq1Label );
		if ( seq2Label != null && bSeq2Selected ) retVal.add( seq2Label );
		
		return retVal;
	};
	public boolean hasSeqIDs(String [] theIDs, boolean mustHaveBoth) {
		boolean firstInc = false, secondInc = false;
		
		for(int x=0; x<theIDs.length && (!firstInc || !secondInc); x++) {
			firstInc =  firstInc  || seq1Label.equals(theIDs[x]);
			secondInc = secondInc || seq2Label.equals(theIDs[x]);
		}
		if(mustHaveBoth)
			return firstInc && secondInc;
		return firstInc || secondInc;
	}
	public void selectNone () {	
		bSeq1Selected = bSeq2Selected = false; 
		repaint ();
	}
	public void handleClick( MouseEvent e, Point localPoint ) { // CAS313 can only select one
		selectNone(); 
		if ( dSelectRow1Top <= localPoint.y && localPoint.y <= dSelectMid )
			bSeq1Selected = !bSeq1Selected;
		else if ( dSelectMid <= localPoint.y && localPoint.y <= dSelectRow2Bottom )
			bSeq2Selected = !bSeq2Selected;
		repaint();
	}
	public PairAlignData getAlignment() { return alignDataObj;}
	
	//-------------------------- private -----------------------------/
	private double getFrameWidth ( ){
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	private void init() {
	try {
		alignSeq1 = alignDataObj.getAlignFullSeq1(); 
		for(nTrimStart1=0; nTrimStart1 < alignSeq1.length() && alignSeq1.charAt(nTrimStart1) == gapCh; nTrimStart1++);
		for(nTrimStop1=alignSeq1.length() - 1; nTrimStop1 >= 0 && alignSeq1.charAt(nTrimStop1) == gapCh; nTrimStop1--);
		
		alignSeq2 = alignDataObj.getAlignFullSeq2(); 
		for(nTrimStart2=0; nTrimStart2 < alignSeq2.length() && alignSeq2.charAt(nTrimStart2) == gapCh; nTrimStart2++);
		for(nTrimStop2=alignSeq2.length() - 1; nTrimStop2 >= 0 && alignSeq2.charAt(nTrimStop2) == gapCh; nTrimStop2--);
		
		super.setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );
		
		super.setTrimRange(Math.max(nTrimStart1, nTrimStart2), Math.min(nTrimStop1, nTrimStop2)); // CAS313 add
	
		seq1Label = alignDataObj.getSeqID1();
		seq2Label = alignDataObj.getSeqID2();
		
		dSequenceStart = Math.max( super.getTextWidth( seq1Label ), super.getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nGAP;
		super.setMinPixelX ( dSequenceStart );
		
		dRulerHeight = super.getTextWidth( "999999" );
		//description = alignDataObj.getDescription();
		
		dFrameHeight = nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
				 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = 		dPAD +  dHeaderHeight;
		dRulerTop = 		dDivider1Y +  nInsetGap;
		dRulerBottom = 		dRulerTop +   dRulerHeight;
		dSeq1Top = 			dRulerBottom + nInsetGap;
		dSeq1Bottom = 		dSeq1Top +     nRowHeight;
		dSeq2Top = 			dSeq1Bottom + nInsetGap;
		dSeq2Bottom = 		dSeq2Top + nRowHeight;
		dSelectRow1Top =    dSeq1Top - nInsetGap / 2.0;
		dSelectMid = 	    dSeq1Bottom + nInsetGap / 2.0;
		dSelectRow2Bottom = dSeq2Bottom + nInsetGap;
	}
	catch(Exception e) {ErrorReport.prtReport(e, "init PairN");}
	}
	/****************************************************************************/
	private PairAlignData alignDataObj=null;
	private String alignSeq1 = "", alignSeq2 = "";
	private int nTrimStart1 = 0, nTrimStop1 = 0, nTrimStart2 = 0, nTrimStop2 = 0; 
	private String seq1Label = "", seq2Label = "";
	private boolean isDNA = true;
//	private Vector<String> description = null;
	
    MultilineTextPanel headerPanel;

	boolean bSeq1Selected = false, bSeq2Selected = false;	
	Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	private double dFrameHeight = 0, dDivider1Y = 0;
	private double dRulerTop = 0, dRulerBottom = 0;
	private double dSeq1Top = 0, dSeq1Bottom = 0, dSeq2Top = 0, dSeq2Bottom = 0;
	private double dSelectRow1Top = 0, dSelectMid = 0, dSelectRow2Bottom = 0;
	
	private double dHeaderHeight = 0, dRulerHeight = 0, dSequenceStart = 0;
	
	static private final int nInsetGap = 5;
	static private final int nRowHeight = 15;
	static private final int nGAP=10;
	static private final double dPAD=10.0;
	
    private static final long serialVersionUID = 1;
}
