package sng.viewer.panels.align;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;

import sng.dataholders.SequenceData;
import util.database.Globalx;
/*
 * Drawing routines for contig and pair wise alignment
 * PairAlignPanel and ContigAlignPanel extend this class
 * CAS313 clean up some dead code and rearrange 
 * CAS314 broke this off into separate code so I don't have to update ContigAlignPanel in order to 
 * change PairAlignPanel and its base. Its some duplicate code, but simplifies, and changing one
 * file does not mess up the other.
*/
public class ContigBasePanel extends JPanel {
	private static final long serialVersionUID = 1;
	    
	public ContigBasePanel ( Font baseFont ) {
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
		nScale = n; 
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
	
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	public int getDrawMode ( ) { 
		return nDrawMode; 
	}
	public void setDrawMode(int mode) {
		nDrawMode = mode;
		clearPanels();
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
		super.paintComponent( g );
	}	
	protected double getSequenceWidth ( ) {
		if( nDrawMode == GRAPHICMODE ) return ( nMaxIndex - nMinIndex + 1 ) / nScale;
		else return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}	
	protected void drawSequence( Graphics2D g2, SequenceData seqData, double dYTop, double dYBottom ) {
		if( nDrawMode == GRAPHICMODE ) 
			drawSequenceLine( g2, seqData, dYTop, dYBottom );
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
			
			if ( seqData.isBuried() )					baseColor = mediumGray;
			else if ( getIsNAt(i) ) 					baseColor = anyHangUnk; 
			else if ( getIsMismatchAt( seqData, i ) )	baseColor = ntMisMatch;
			else if ( getIsGapAt( seqData, i )) 		baseColor = anyGap;
			else if ( getIsLowQualityAt ( seqData, i ) )baseColor = lowQuality;

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
	protected void drawSequenceLine(Graphics2D g2, SequenceData seqData, double dYTop, double dYLow) {
		drawSequenceLine(g2, seqData, dYTop, dYLow,ContigViewPanel.HIDE_BURIED_EST);//default view, do not show ESTs
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
			boolean bMM=false, bGap=false, bUnk=false, bLow=false;
			
			// Aggregate together the information for the next BASES_PER_PIXEL (nZoom) bases 
			//     Contig - low quality and mismatch will be shown together
			
			for ( int j = 0; pos <= seqData.getHighIndex() && j < nBasesToGroup; ++pos, ++j )
			{			
				if      (getIsNAt(pos))					  	bUnk=true; 
				else if (getIsLowQualityAt(seqData, pos))  	bLow=true;
				
				if (getIsGapAt(seqData, pos))		 	 bGap=true;
				else if (getIsMismatchAt(seqData, pos))	 bMM=true;
				
			}
			// dX, dY, dW, dH  (g2, dX, dY-hash, dW, hash*2, baseColor)
			if ( bMM )  
				drawHash(g2, dX, dY-hash_LG, dW, hash_LG*2, ntMisMatch);
			
			if ( bGap )  
				drawHash(g2, dX, dY-hash_MD, dW, hash_MD*2, anyGap );
			
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
	
	private void drawHash ( Graphics2D g2, double dx, double dy, double dw, double dh, Color hashColor ){
		g2.setColor( hashColor );
		g2.draw( new Line2D.Double( dx,  dy, dx, dy + dh ) ); // x1, y1, x2, y2
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
	
	protected int getCellWidthInt ( ) {
		return (int)dFontCellWidth;
	}
	protected int getWriteWidthInt ( ) {// The total width need for writing the bases
		return (int) dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}
	
	// Returns the pixel position for the left hand side of the "box" to write in
	protected double calculateWriteX ( int nBasePos ) {
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}	
	protected int calculateIndexFromWriteX ( double dX )  { 
		return (int)( (dX - dMinPixelX) / dFontCellWidth ) + nMinIndex; 
	}	
	protected double calculateDrawX ( int nBasePos ) { 
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nScale;
	}
	protected void drawText ( Graphics2D g2, String str, double dXLeft, double dYTop, Color theColor ) {
		if ( str == null || str.length() == 0 ) return;
		
		g2.setColor ( theColor );
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYTop + dFontAscent); 
		layout.draw( g2, (float)dXLeft, fBaseline );
	}	
	
	protected double getTextWidth ( String str ) {
		if ( str == null ) return 0;
		else return fontMetrics.stringWidth ( str );
	}
	protected void drawText ( Graphics2D g2, String str, double dX, double dY ) {
		drawText ( g2, str, dX, dY, Color.BLACK );
	}	
	
	protected void clearPanels ( ) {
		super.repaint();
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

	private int nDrawMode = GRAPHICMODE;
	  
 // For drawing 
 	public static final Color mediumGray =  new Color(180, 180, 180); // bases for buried
 	public static final Color purple = new Color(138, 0, 184);        // line for buried
 	public static final Color lowQuality = Color.blue;	
 	
 	public static final Color anyHangUnk 	= Globalx.anyHang; // new Color(180, 180, 180); mediumGray
	public static final Color anyGap 	= Globalx.anyGap;  // new Color(0,200,0); light green; shared with aaZappo
	
	public static final Color ntMisMatch = Globalx.ntMisMatch; // Color.red;
}
