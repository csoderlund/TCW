/*
 * Drawing routines for contig and pair wise alignment
 * PairAlignPanel and ContigAlignPanel extend this class
 * CAS313 clean up some dead code and rearrange 
*/
package sng.viewer.panels.align;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.*;

import sng.dataholders.SequenceData;
import util.database.Globalx;

public class BaseAlignPanel extends JPanel {
	private static final long serialVersionUID = 1;
	    
	public BaseAlignPanel ( Font baseFont ) {
		theFont = baseFont;
		fontMetrics = getFontMetrics ( baseFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );
	}
	
	// Abstract methods (child class should over-ride) defined in PairAlignPanel and ContigAlignPanel
	protected boolean getIsLowQualityAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsGapAt ( SequenceData seq, int nPos ) { return false;}		
	protected boolean getIsMismatchAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsAAgt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsAAeq ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsStop ( SequenceData seq, int nPos ) { return false; } 
	protected boolean getIsNAt ( SequenceData seq, int nPos ) { return false; } 
	protected boolean getIsNAt ( int nPos ) { return false; } 
	
	public void refreshPanels ( ) { }
	
	public Vector<String> getSeqIDs() { return null; }
	public void selectMatchSeqs ( String strSeqName ) { } 
	public void addSelectedSeqsToSet ( TreeSet<String> set ){ }
	public void getSelectedSeqIDs ( TreeSet<String> set ) { }
	
	public void selectAll () { }
	public void selectNone () { }
	
	public void handleClick( MouseEvent e, Point localPoint ) { }
	public double getGraphicalDeadWidth ( ) { return 0; }
	public AlignData getAlignData() {return null;}
	
	// Zoom
	public void setZoom ( int n ) throws Exception { 
		if (n<0) {
			nScale = -n;
			bScaleUp=true;
		}
		else { 
			nScale = n; 
			bScaleUp=false;
		}
		clearPanels ( ); 
	}
	
	// Min Pixel X : the lowest pixel on the x-axis for drawing; corresponds to the base at Min Index
	public void setMinPixelX ( double dX ) { dMinPixelX = dX; clearPanels ( ); }
	public double getMinPixelX ( ) { return dMinPixelX; }
	
	public void setIndexRange ( int nMin, int nMax ) { 
		if ( nMin > nMax ) 
			throw new RuntimeException ( "Min (" + nMin + ") must be less than Max (" + nMax + ")." );
		
		nMinIndex = nMin;   // the lowest index for bases in the alignment
		nMaxIndex = nMax;	// the highest index for bases in the alignment
		clearPanels ( );
	}
	public int getTotalBases ( ) { return nMaxIndex - nMinIndex + 1; }
	
	public boolean isShowORF() { return bShowORF;}
	public boolean isShowHit() { return bShowHit;}
	public void setShowORF ( boolean b ) { bShowORF = b; clearPanels ( ); };
	public void setShowHit ( boolean b )   { bShowHit = b; clearPanels ( ); };
	
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	public int getDrawMode ( ) { 
		return nDrawMode; 
	}
	public void setDrawMode(int mode) {
		nDrawMode = mode;
		clearPanels();
	}
	public void setCtgDisplay(boolean b) { // false if called from PairAlignPanel
		bCtgDisplay = b;
	}	
	public void changeDraw() {
  		if(nDrawMode == GRAPHICMODE) nDrawMode = TEXTMODE;
  		else nDrawMode = GRAPHICMODE;
  		clearPanels ( );
  	}	
	public boolean isDrawSeqMode () {
		if(nDrawMode == GRAPHICMODE) return false;
		else return true;
	}	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		if ( bPanelsDirty ) {
			refreshPanels ();
			bPanelsDirty = false;
		}
		drawCodingPanels ( g2 );
	}	
	protected double getSequenceWidth ( ) {
		if( nDrawMode == GRAPHICMODE ) {
			if (bScaleUp) return ( nMaxIndex - nMinIndex + 1 ) * nScale;
			else return ( nMaxIndex - nMinIndex + 1 ) / nScale;
		}
		else return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}	
	protected void drawSequence( Graphics2D g2, SequenceData seqData, double dYTop, double dYBottom ) {
		if( nDrawMode == GRAPHICMODE ) {
			if (bScaleUp) drawSequenceLineUp( g2, seqData, dYTop, dYBottom );
			else 	      drawSequenceLine( g2, seqData, dYTop, dYBottom );
		}
		else
			writeSequenceLetters( g2, seqData, dYTop, dYBottom );
	}
	/*********************************************************************************
	 * Letters
	 ************************************************************************/
	// XXX Draw the columns of bases that are currently visible 
	protected void writeSequenceLetters (Graphics2D g2, SequenceData seqData, double dYTop, double dYBottom) {
		Rectangle clipRect = g2.getClipBounds(); 
		double dClipMinX = clipRect.getMinX(); 
		double dClipMaxX = clipRect.getMaxX();
		double dWriteBaseline = (dYBottom - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 
		g2.setColor(Color.black);
		
        int nStart = Math.max(calculateIndexFromWriteX(dClipMinX),   nMinIndex); // seqData.getLowIndex() );
        int nEnd =   Math.min(calculateIndexFromWriteX(dClipMaxX)+1, nMaxIndex); // seqData.getHighIndex() );
        
		for( int i = nStart; i <= nEnd; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			Color baseColor = Color.black;
			if (bCtgDisplay) { // contig display
				if ( seqData.isBuried() )					baseColor = mediumGray;
				else if ( getIsNAt(i) ) 					baseColor = anyHangUnk; 
				else if ( getIsMismatchAt( seqData, i ) )	baseColor = ntMisMatch;
				else if ( getIsGapAt( seqData, i )) 		baseColor = anyGap;
				else if ( getIsLowQualityAt ( seqData, i ) )baseColor = lowQuality;

			} else { // pair display				
				if ( getIsStop ( seqData, i ) )				baseColor = aaStop;
				else if ( getIsNAt ( seqData, i ) )			baseColor = anyHangUnk; 
				else if ( getIsGapAt( seqData, i )) 		baseColor = anyGap;
				else if ( getIsAAgt ( seqData, i ))			baseColor = aaGtZero;
				else if ( getIsAAeq ( seqData, i ))	    	baseColor = aaEqZero;
				else if ( getIsMismatchAt( seqData, i ) )	baseColor = aaLtZero;
			}
			char c = seqData.getOutBaseAt(i);
			drawCenteredBase ( g2, c, baseColor, dX, dWriteBaseline );
		}
	}
	private void drawCenteredBase ( Graphics2D g2, char chBase, Color theColor, double dCellLeft, double dBaseline ) {
		g2.setColor ( theColor );
		
		String str = "" + chBase;
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
	   	Rectangle2D bounds = layout.getBounds();
		float fCharWidth = (float)bounds.getWidth();
		
		// Adjust x draw position to center the character in its rectangle
		float fX = (float)dCellLeft + (float)(dFontCellWidth - 1.0f) / 2.0f - fCharWidth / 2.0f; 
		   
		layout.draw( g2, fX, (float)dBaseline );

		g2.setColor ( Color.black );		
	}
	/***************************************************************************
	 * Line
	 ***********************************************************************/
	//-- CAS313 added for zoom N:1 Base only for Pairs on bScaleUp --/
	private void drawSequenceLineUp( Graphics2D g2, SequenceData seqData, double dYTop, double dYLow){
		int start = seqData.getLowIndex();
		int end = seqData.getHighIndex();
		
		// Determine the position of the sequence line, but don't draw until after the hashes
		double dXPosStart = calculateDrawX ( start );
		double dXPosEnd = 	calculateDrawX ( end );

		double dHeight = 	dYLow - dYTop;   // always 15.0
		double dY 	= 		dHeight / 2.0 + dYTop; // center, so +/- hash

		int hashHeight =   (int)(dHeight/2) - 2;
		int hash_LG = 		hashHeight;
		int hash_MD = 		hashHeight - 1;
		int hash_SM = 		hashHeight - 2;
		
		double dW=0.5;  // does not fill rectangle
		if (bScaleUp && nScale>2) dW = nScale*0.5;

		// For each letter in the sequence
		for( int pos = start; pos < end; pos++) {
			double dX = calculateDrawX ( pos);
		
			Color baseColor = Color.BLACK;

			if      ( getIsNAt ( seqData, pos ) )		baseColor=anyHangUnk;  	// n's and x's
			else if ( getIsGapAt ( seqData, pos ) )		baseColor=anyGap;
			else if ( getIsAAgt ( seqData, pos ) )	    baseColor=aaGtZero;     // aa only
			else if ( getIsAAeq ( seqData, pos ) )	    baseColor=aaEqZero;     // aa only
			else if ( getIsMismatchAt ( seqData, pos ))	baseColor=ntMisMatch;
			
			if ( getIsStop( seqData, pos ))				baseColor=aaStop;
			
			if (baseColor==Color.black) continue;
			
			int hash=hash_SM;
			if (baseColor==ntMisMatch) hash=hash_LG;
			else if (baseColor==anyGap || baseColor==aaEqZero)  hash=hash_MD;
			
			double dH = hash*2;
			drawHash(g2, dX, dY-hash, dW, dH, baseColor);
			
			if (baseColor==aaStop)  drawStop(g2, dX, dYTop);
		}
		// Draw the line for the sequence and arrow head
		g2.setColor( Color.black );
		g2.draw( new Line2D.Double( dXPosStart, dY, dXPosEnd, dY ) );

		double dArrowHeight = dHeight / 2.0 - 2;
		drawArrowHead ( g2, dXPosEnd, dY, dArrowHeight, true /* right */ );
		
		g2.setColor(Color.black);
	}
	
	//------ Contig and Pair Scale <=1  -----------------//
	protected void drawSequenceLine(Graphics2D g2, SequenceData seqData, double dYTop, double dYBottom) {
		drawSequenceLine(g2, seqData, dYTop, dYBottom,ContigViewPanel.HIDE_BURIED_EST);//default view, do not show ESTs
	}
	protected void drawSequenceLine( Graphics2D g2,
					SequenceData seqData, double dYTop, double dYLow, int buriedMode) {
		
		double dHeight = dYLow - dYTop;
		
		double dY      = dHeight / 2.0 + dYTop;

		int hashHeight =   (int)(dHeight/2) - 2;
		int hash_LG = 		hashHeight;
		int hash_MD = 		hashHeight - 1;
		int hash_SM = 		hashHeight - 3;
		
		double dW=0.5;  // does not fill rectangle
		int nBasesToGroup = -1;
		
		// For each letter in the sequence
		for( int pos = seqData.getLowIndex(); pos <= seqData.getHighIndex();)
		{
			if(seqData.isBuried() && buriedMode == ContigViewPanel.SHOW_BURIED_EST_LOCATION) {
				pos++;
				continue;
			}	
			
			double dX = calculateDrawX ( pos );	
		
			if (nBasesToGroup <= 0) {// May short the first group so that all grouping is aligned between ESTs
				nBasesToGroup = nScale;
				nBasesToGroup -= ( seqData.getLowIndex() - nMinIndex ) % nScale;
			}
			else {
				nBasesToGroup = nScale; // Not the first group, do the full amount
			}
			boolean bStop=false, bMM=false, bGap=false, bUnk=false, bLow=false, bGt=false, bEq=false;
			
			// Aggregate together the information for the next BASES_PER_PIXEL (nZoom) bases 
			//     Contig - low quality and mismatch will be shown together
			
			for ( int j = 0; pos <= seqData.getHighIndex() && j < nBasesToGroup; ++pos, ++j )
			{			
				if (bCtgDisplay) {  // contig display
					if      (getIsNAt(pos))					  	bUnk=true; 
					else if (getIsLowQualityAt(seqData, pos))  	bLow=true;
					
					if (getIsGapAt(seqData, pos))		 	 bGap=true;
					else if (getIsMismatchAt(seqData, pos))	 bMM=true;
				} 
				else { // pairwise display - nt or aa
					if      (getIsNAt(seqData, pos))		bUnk=true; // n's and x's, overhang
					else if (getIsGapAt(seqData, pos))		bGap=true;
					else if (getIsAAgt(seqData, pos))		bGt=true;  	// aa only
					else if (getIsAAeq(seqData, pos))		bEq=true;	// aa only
					else if (getIsMismatchAt(seqData, pos))	bMM=true;
					
					if (getIsStop(seqData, pos))			bStop=true;
				}
			}
			if (bStop) drawStop(g2, dX, dYTop);
			
			// dX, dY, dW, dH  (g2, dX, dY-hash, dW, hash*2, baseColor)
			if ( bMM )  
				drawHash(g2, dX, dY-hash_LG, dW, hash_LG*2, ntMisMatch);
			
			if ( bGap )  
				drawHash(g2, dX, dY-hash_MD, dW, hash_MD*2, anyGap );
			
			if ( bEq )  
				drawHash(g2, dX, dY-hash_MD, dW, hash_MD*2, aaEqZero);
			
			if ( bGt ) 
				drawHash(g2, dX, dY-hash_SM, dW, hash_SM*2, aaGtZero );
			
			if ( bUnk ) 
				drawHash(g2, dX, dY-hash_SM,  dW, hash_SM*2, anyHangUnk );	
			
			if( bLow ) 
				drawHash(g2, dX, dY-hash_SM, dW, hash_SM*2, lowQuality);
		}
		
		// Draw the line for the sequence
		if(buriedMode!=ContigViewPanel.HIDE_BURIED_EST && seqData.isBuried())
			g2.setColor( purple );
		else
			g2.setColor( Color.black );
		
		// Draw line
		double dXPosStart = calculateDrawX ( seqData.getLowIndex() );
		double dXPosEnd = calculateDrawX ( seqData.getHighIndex() );
		g2.draw( new Line2D.Double( dXPosStart, dY, dXPosEnd, dY ) );

		// Draw the arrow head
		if(seqData.isBuried() ) g2.setColor(Color.gray);
		
		double dArrowHeight = dHeight / 2.0 - 2;
		if ( !seqData.isReverseComplement() )	
			drawArrowHead ( g2, dXPosEnd, dY, dArrowHeight, true /* right */ );
		else
			drawArrowHead ( g2, dXPosStart, dY, dArrowHeight, false /* left */ );			
		
		g2.setColor(Color.black);
	}
	
	private void drawStop(Graphics2D g2, double dX, double dYTop ) {
		g2.setColor( aaStop);
		g2.draw( new Line2D.Double( dX,   dYTop-1, dX,   dYTop+2 ) ); // |
		g2.draw( new Line2D.Double( dX-1, dYTop-1, dX+1, dYTop-1 ) ); // -
	}
	private void drawHash ( Graphics2D g2, double dx, double dy, double dw, double dh, Color hashColor ){
		g2.setColor( hashColor );
		if (bScaleUp) {
			Rectangle2D hRect = new Rectangle2D.Double(dx, dy, dw, dh);
			g2.draw(hRect);
			g2.fill(hRect);
		}
		else { 
			g2.draw( new Line2D.Double( dx,  dy, dx, dy + dh ) ); // x1, y1, x2, y2
		}
	}
	private void drawArrowHead ( Graphics2D g2, double dX, double dY, double dH, boolean bRight ){
		final double ARROW_WIDTH = dH - 1;
		
		double dYStartTop = dY - dH;
		double dYStartBottom = dY + dH;
		double dXStart = dX;
		
		if ( bRight ) 	dXStart -= ARROW_WIDTH;
		else			dXStart += ARROW_WIDTH;

		g2.draw( new Line2D.Double( dXStart, dYStartTop, dX, dY ) );
		g2.draw( new Line2D.Double( dXStart - 1, dYStartTop, dX - 1, dY ) );
		g2.draw( new Line2D.Double( dXStart, dYStartBottom, dX, dY ) );
		g2.draw( new Line2D.Double( dXStart - 1, dYStartBottom, dX - 1, dY ) );
	}
	
	// Positioned by the center of the left side
	private void drawVerticalText ( Graphics2D g2, String str, double dX, double dY ) {
		if (str == null || str.length() == 0) return;
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );

		g2.rotate ( - Math.PI / 2.0 );
		g2.setColor ( Color.BLACK );
		float fHalf = (float)(layout.getBounds().getHeight() / 2.0f);
		layout.draw( g2, (float)-dY, (float)dX + fHalf );	
		g2.rotate ( Math.PI / 2.0 );
	}	
	
	//-- Pair and Contig --//
	protected void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYLow )  {
		// 99999 represents maximum length (mTCW uses 9999) TODO print '0' 
		TextLayout layout = new TextLayout( "99999", theFont, g2.getFontRenderContext() );		
		double dY = (dYLow + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int nTickIncrement = 100;
		if ( nDrawMode != GRAPHICMODE )
			nTickIncrement = 10;
		
		int nTickStart = ( nMinIndex / nTickIncrement ) * nTickIncrement;		
		if ( nTickStart < nMinIndex ) nTickStart += nTickIncrement;

		for ( int i = nTickStart; i < nMaxIndex; i += nTickIncrement ){	
			double dX;
			if ( nDrawMode == GRAPHICMODE ) 
				dX = calculateDrawX (i);
			else 
				dX = (( calculateWriteX (i+1) + calculateWriteX (i+2) ) / 2);

			if ( dX > dXMin) 
				drawVerticalText ( g2, String.valueOf(i), dX, dY );
		}
	}
	
	//-- Contig only --//
	protected int getCellWidthInt ( ) {
		return (int)dFontCellWidth;
	}
	protected int getWriteWidthInt ( ) {// The total width need for writing the bases
		return (int) dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}
	
	//-- Base only --//
	private double calculateX ( int nBasePos ) {
  		if(nDrawMode == GRAPHICMODE) return calculateDrawX ( nBasePos );
  		else return calculateWriteX ( nBasePos );	
	}
	//-- Base and Contig --//
	// Returns the pixel position for the left hand side of the "box" to write in
	protected double calculateWriteX ( int nBasePos ) {
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}	
	protected int calculateIndexFromWriteX ( double dX )  { 
		return (int)( (dX - dMinPixelX) / dFontCellWidth ) + nMinIndex; 
	}	
	protected double calculateDrawX ( int nBasePos ) { 
		if (bScaleUp) return dMinPixelX + ( nBasePos - nMinIndex + 1 ) * nScale;
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nScale;
	}
	protected void drawText ( Graphics2D g2, String str, double dXLeft, double dYTop, Color theColor ) {
		if ( str == null || str.length() == 0 ) return;
		
		g2.setColor ( theColor );
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYTop + dFontAscent); 
		layout.draw( g2, (float)dXLeft, fBaseline );
	}	
	
	//-- Pair and Contig --/
	protected double getTextWidth ( String str ) {
		if ( str == null ) return 0;
		else return fontMetrics.stringWidth ( str );
	}
	protected void drawText ( Graphics2D g2, String str, double dX, double dY ) {
		drawText ( g2, str, dX, dY, Color.BLACK );
	}	
	
	/************************************************************
	 * Highlighting for NtAA or NtNt
	 */
	protected void setupHitPanels(String tip, int start, int end,  double dYTop, double dYBottom ) {
		if (!bShowHit) return;
		
		double dLeft = calculateX (start+1);
		double dRight = calculateX (end+1);
		createHighlightPanel ( tip, colorHIT, dLeft, dRight, dYTop, dYBottom );
	}
	protected void setupUtrPanels (String tip, int aStart, int aStop,  double dYTop, double dYBottom ) {		
		if (!bShowORF) return;
		
		double dLeft = calculateX(aStart);
		double dRight = calculateX (aStop);
		
		createHighlightPanel (tip, colorORF, dLeft, dRight, dYTop, dYBottom );
	}
	/*************************************
	 * aStart/aStop - AA coordinates adjusted for gaps
	 */
	protected void drawCodingPanels ( Graphics2D g2 ) {
		Iterator<JPanel> iterPanel = highlightPanels.iterator();
		while ( iterPanel.hasNext() ) {
			JPanel curPanel = iterPanel.next();
			g2.setColor( curPanel.getBackground() );
			g2.fill( curPanel.getBounds() );
		}		
	}

	protected void clearPanels ( ) {
		bPanelsDirty = true;
		
		Iterator<JPanel> iter = highlightPanels.iterator();
		while ( iter.hasNext() ) remove( (Component)iter.next() );
		highlightPanels.removeAllElements();
		
		super.repaint();
	}
	private void createHighlightPanel (String toolTip, Color theColor,
				double dLeft, double dRight, double dTop, double dBottom ) {		
		JPanel thePanel = new JPanel ();
		thePanel.setBackground( theColor );
		thePanel.setSize ( (int)(dRight - dLeft), (int)(dBottom - dTop) );
		thePanel.setToolTipText(toolTip );
		thePanel.setLocation ( (int)dLeft, (int)dTop );
		thePanel.setOpaque( false );
		
		add ( thePanel );
		highlightPanels.add( thePanel );  
	}
	
	/********************************************************************************/
	private FontMetrics fontMetrics = null;
	private Font theFont = null;
	
	// Attibutes for positioning
	private double dMinPixelX = 0;		// The lowest pixel (associated base at min index) 
	private int nMinIndex = 1, nMaxIndex = 1;
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;		// The distance from the top of the write cell to the baseline
	private int nScale = 3;	
	private boolean bScaleUp=false;

	private int nDrawMode = GRAPHICMODE;
	private boolean bCtgDisplay = true;
	
	// Panels to show the coding region
	private boolean bPanelsDirty = false;
	private Vector<JPanel> highlightPanels = new Vector<JPanel> ();      
    private boolean bShowHit = false;
    private boolean bShowORF = false;
   
 // For drawing 
    // Contigs
 	public static final Color mediumGray =  new Color(180, 180, 180); // bases for buried
 	public static final Color purple = new Color(138, 0, 184);        // line for buried
 	public static final Color lowQuality = Color.blue;	
 	
 	public static final Color anyHangUnk 	= Globalx.anyHang; // new Color(180, 180, 180); mediumGray
	public static final Color anyGap 	= Globalx.anyGap;  // new Color(0,200,0); light green; shared with aaZappo
	
	public static final Color ntMisMatch = Globalx.ntMisMatch; // Color.red;
	
	public static final Color aaLtZero 	= Globalx.aaLtZero; // Color.red; 		
	public static final Color aaEqZero	= Globalx.aaEqZero; // new Color(255, 173, 190);  light red
	public static final Color aaGtZero 	= Globalx.aaGtZero; // Color.blue; 	
	public static final Color aaStop	= Globalx.aaStop;   // new Color(138, 0, 184); 	purple; shared with aaZappo
 	
 	public static final Color colorORF = Color.YELLOW;
 	public static final Color colorHIT = new Color(157, 189, 242);
}
