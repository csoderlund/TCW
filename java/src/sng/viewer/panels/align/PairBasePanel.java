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

import util.align.AAStatistics;
import util.database.Globalx;
/*
 * Drawing routines for pair wise alignment
 * PairAlignPanel extends this class
 * CAS313 clean up some dead code and rearrange 
 * CAS314 BaseAlignPanel split into PairBasePanel and ContigBasePanel 
 * 		This is now very similar to cmp.viewer.seq.align.BaseAlignPanel
*/
public class PairBasePanel extends JPanel {
	private static final long serialVersionUID = 1;
	
	public static final int GRAPHICMODE = 1;
	public static final int TEXTMODE = 0;
	
	private final int gapCh = Globalx.gapCh;
	private final int stopCh = Globalx.stopCh;
	    
	public PairBasePanel ( Font baseFont ) {
		theFont = baseFont;
		fontMetrics = getFontMetrics ( baseFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );
	}
	
	//--- Abstract methods (child class should over-ride) defined in PairAlignPanel	---//
	protected char getChar(boolean bFirst, int nPos) {return ' ';}
	protected boolean isEndGap(boolean bFirst, int nPos) {return false;}
	
	//---- Public --------//
	public void refreshPanels ( ) { }
	
	public Vector<String> getSeqIDs() { return null; }
	public void getSelectedSeqIDs ( TreeSet<String> set ) { }
	
	public void selectAll () { }
	public void selectNone () { }
	
	public void handleClick( MouseEvent e, Point localPoint ) { }
	public double getGraphicalDeadWidth ( ) { return 0; }
	public AlignData getAlignData() {return null;}
	
	//------- Setup -------------//
	
	public void setMinPixelX (double dX) {dMinPixelX = dX;}// lowest pixel on the x-axis for drawing
	
	public void setIndexRange (int nMin, int nMax) { 
		nMinIndex = nMinLen = nMin;  // 0 the lowest index for bases in the alignment
		nMaxIndex = nMaxLen = nMax;	 // len of longest - the highest index for bases in the alignment
	}
	public void setTrimRange(int nMin, int nMax) {
		nMinTrim = nMin;
		nMaxTrim = nMax+1;
	}
	
	//------- Change View methods ---------------//
	public void setDrawMode(int mode) {
		nDrawMode = mode;
		reDrawPanels();
	}
	public void setZoom ( int n ) throws Exception { 
		if (n<0) {
			nScale = -n;
			bScaleUp=true;
		}
		else { 
			nScale = n; 
			bScaleUp=false;
		}
		reDrawPanels ( ); 
	}
	public void setTrim(boolean b) {
		this.isTrim = b;
		if (isTrim) {
			nMinIndex = nMinTrim;
			nMaxIndex = nMaxTrim;
		}
		else {
			nMinIndex = nMinLen;
			nMaxIndex = nMaxLen;
		}
		reDrawPanels ( ); 
	}	
	
	public void setShowORF ( boolean b ) { bShowORF = b; reDrawPanels ( ); };
	public void setShowHit ( boolean b ) { bShowHit = b; reDrawPanels ( ); };
	
	public boolean isShowORF() { return bShowORF;}
	public boolean isShowHit() { return bShowHit;}
	public boolean isTrim() {return isTrim;}
		
	//---- Draw ---//
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		if ( bPanelsDirty ) {
			refreshPanels ();
			bPanelsDirty = false;
		}
		drawHighlightPanels ( g2 );
	}

	private void reDrawPanels ( ) {
		bPanelsDirty = true;
		
		Iterator<JPanel> iter = highlightPanels.iterator();
		while ( iter.hasNext() ) remove( (Component)iter.next() );
		highlightPanels.removeAllElements();
		
		revalidate();
		repaint();
	}

	protected void drawSequence( Graphics2D g2, String seq, double dYTop, double dYLow, 
			int start, int end, boolean isFp, boolean isFirst , boolean isDNA ) {
		if( nDrawMode == GRAPHICMODE ) 
			drawSequenceLine( g2, seq, dYTop, dYLow , start, end, isFp, isFirst, isDNA);
		else
			writeSequenceLetters( g2, seq, dYTop, dYLow, start, end,  isFirst, isDNA );
	}
	/*********************************************************************************
	 * XXX Draw the columns of bases that are currently visible 
	 ************************************************************************/
	private void writeSequenceLetters (Graphics2D g2, String alignSeq, double dYTop, double dYLow, 
			int start, int end, boolean isFirst, boolean isDNA) {
	
		double dWriteBaseline = (dYLow - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 
		g2.setColor(Color.black);
		
		int nStart = (isTrim) ? Math.max(start, nMinTrim) : start;
		int nEnd =   (isTrim) ? Math.min(end,   nMaxTrim) : end;
		
		for( int i = nStart; i <= nEnd; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			Color baseColor = Color.black;
					
			char c1 = getChar(isFirst, i); // either consensus or other half of pair
			char c2 = alignSeq.charAt(i); // current
			
			if (isEndGap(isFirst, i)) baseColor = getColorEnd(c2);
			else if (isDNA) 		  baseColor = getColorNT(c1, c2);
			else            		  baseColor = getColorAA(c1, c2);
			
			drawCenteredBase ( g2, c2, baseColor, dX, dWriteBaseline );
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
	private void drawSequenceLine( Graphics2D g2, String alignSeq, double dYTop, double dYLow,
			int start, int end, boolean isFp, boolean isFirst, boolean isDNA){
		
		int nStart = (isTrim) ? Math.max(start, nMinTrim) : start;
		int nEnd =   (isTrim) ? Math.min(end,   nMaxTrim) : end;
		
		double dHeight = 	dYLow - dYTop;   // always 15.0
		double dY 	= 		dHeight / 2.0 + dYTop; // center, so +/- hash

		int hashHeight =   (int)(dHeight/2) - 2;
		int hash_LG = 		hashHeight;
		int hash_MD = 		hashHeight - 1;
		int hash_SM = 		hashHeight - 3;
		
		double dW=0.5;  // does not fill rectangle
		if (bScaleUp && nScale>2) dW = nScale*0.5;

		// For each letter in the sequence
		for( int pos = nStart; pos < nEnd; pos++) {
			double dX = calculateDrawX ( pos);
		
			Color baseColor = Color.BLACK;

			char c1 = getChar(isFirst, pos); // either consensus or other half of pair
			char c2 = alignSeq.charAt(pos); // current
			
			if (isEndGap(isFirst, pos)) baseColor = getColorEnd(c2);
			else if (isDNA) 		    baseColor = getColorNT(c1, c2);
			else            		    baseColor = getColorAA(c1, c2);
			
			if (baseColor==Color.black) continue;
			
			int hash=hash_SM;
			if (baseColor==ntMisMatch) hash=hash_LG;
			else if (baseColor==anyGap || baseColor==aaEqZero)  hash=hash_MD;
			
			double dH = hash*2;
			drawHash(g2, dX, dY-hash, dW, dH, baseColor);
			
			if (baseColor==aaStop)  drawStop(g2, dX, dYTop);
		}
		// Draw the line for the sequence and arrow head
		double dXStart = calculateDrawX ( nStart );
		double dXEnd = 	calculateDrawX ( nEnd );

		g2.setColor( Color.black );
		g2.draw( new Line2D.Double( dXStart, dY, dXEnd, dY ) );

		double dArrowHeight = dHeight / 2.0 - 2;
		if (isFp) drawArrowHead ( g2, dXEnd, dY, dArrowHeight, isFp );
		else      drawArrowHead ( g2, dXStart, dY, dArrowHeight, isFp ); // CAS314 put arrow on correct end
		
		g2.setColor(Color.black);
	}
	
	private Color getColorEnd(char c2) {
		if (c2==stopCh)	 return aaStop;
		else 			 return anyHangUnk;
	}
	private Color getColorNT(char c1, char c2) { 
		if (c2==gapCh)  return anyGap;  // colored gap even if consensus is
		
		if (c2=='n' || c2=='N') return anyHangUnk;	// colored anyUnk even if all are anyUnk
		
		if (c1==c2)		return Color.BLACK;
		
		return ntMisMatch;
	}
	private Color getColorAA(char c1, char c2) { //isFirst c1==c2
		if (c2==gapCh)		return anyGap;
		if (c2==stopCh)		return aaStop;
		if (c2=='X') 		return anyHangUnk;	
		
		if (c1==gapCh)		return Color.BLACK;
		
		if (c1==c2)         return Color.BLACK;
		
		if (AAStatistics.isHighSub( c1, c2 )) return aaGtZero;
		if (AAStatistics.isZeroSub( c1, c2 )) return aaEqZero; 
		return aaLtZero;
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
	protected void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYLow)  {
		String tLen = (nMaxIndex>9999) ? "99999" : "9999"; // CAS314
		TextLayout layout = new TextLayout( tLen, theFont, g2.getFontRenderContext() );		
		double dY = (dYLow + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int inc = 10;
		if (nDrawMode==GRAPHICMODE) {
			if (bScaleUp) {
				 inc =  (nScale<4) ? 25 : 10;
			}
			else inc = 100;
		}
		
		int nTickStart = (nMinIndex / inc) * inc;		
		if (nTickStart < nMinIndex) nTickStart += inc;

		for ( int i = nTickStart; i < nMaxIndex; i += inc ){	
			double dX;
			if ( nDrawMode == GRAPHICMODE ) 
				dX = calculateDrawX (i);
			else 
				dX = (( calculateWriteX (i) + calculateWriteX (i+1) ) / 2);

			if ( dX > dXMin) // dXMin is 0
				drawVerticalText ( g2, String.valueOf(i), dX, dY );
		}
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
	private void drawText ( Graphics2D g2, String str, double dXLeft, double dYTop, Color theColor ) {
		if ( str == null || str.length() == 0 ) return;
		
		g2.setColor ( theColor );
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYTop + dFontAscent); 
		layout.draw( g2, (float)dXLeft, fBaseline );
	}		
	private double calculateX ( int nBasePos ) {
  		if(nDrawMode == GRAPHICMODE) return calculateDrawX ( nBasePos );
  		else return calculateWriteX ( nBasePos );	
	}
	protected double calculateWriteX ( int nBasePos ) {
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}		
	protected double calculateDrawX ( int nBasePos ) { 
		if (bScaleUp) return dMinPixelX + ( nBasePos - nMinIndex + 1 ) * nScale;
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nScale;
	}
	protected double getTextWidth ( String str ) {
		if ( str == null ) return 0;
		else return fontMetrics.stringWidth ( str );
	}
	protected void drawText ( Graphics2D g2, String str, double dX, double dY ) {
		drawText ( g2, str, dX, dY, Color.BLACK );
	}	
	protected double getSequenceWidth ( ) {
		if( nDrawMode == GRAPHICMODE ) {
			if (bScaleUp) return ( nMaxIndex - nMinIndex + 1 ) * nScale;
			else return ( nMaxIndex - nMinIndex + 1 ) / nScale;
		}
		else return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}	
	/************************************************************
	 * Highlighting for NtAA or NtNt
	 */
	protected void setupHitPanels(String tip, int start, int end,  double dYTop, double dYBottom ) {
		if (!bShowHit) return;
		
		int s = (start < nMinIndex) ? nMinIndex : start;
		int e = (end > nMaxIndex)   ? nMaxIndex : end;
		
		double dLeft = calculateX(s); // CAS314 was +1 XXX
		double dRight = calculateX(e+1);
		createHighlightPanel (tip, colorHIT, dLeft, dRight, dYTop, dYBottom);
	}
	protected void setupUtrPanels(String tip, int aStart, int aStop,  double dYTop, double dYBottom) {		
		if (!bShowORF) return;
		
		double dLeft = calculateX(aStart);
		double dRight = calculateX(aStop);
		
		createHighlightPanel (tip, colorORF, dLeft, dRight, dYTop, dYBottom );
	}
	private void drawHighlightPanels(Graphics2D g2) {
		Iterator<JPanel> iterPanel = highlightPanels.iterator();
		while ( iterPanel.hasNext() ) {
			JPanel curPanel = iterPanel.next();
			g2.setColor( curPanel.getBackground() );
			g2.fill( curPanel.getBounds() );
		}		
	}
	private void createHighlightPanel (String toolTip, Color theColor,
				double dLeft, double dRight, double dTop, double dBottom ) 
	{		
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
	
	// Set at startup
	private int nMinLen=0, nMaxLen=0;   // CAS314 Zero and length of longest sequence, respectively
	private int nMinTrim =0, nMaxTrim = 0; // CAS314 Where consensus starts/stops having gaps for trim
	private double dMinPixelX = 0;		// The lowest pixel (associated base at min index) 
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;	
	private FontMetrics fontMetrics = null;
	private Font theFont = null;
		
	// Can change	
	private int nMinIndex = 1, nMaxIndex = 1; // CAS314 isTrim changes between to Len or Trim
	private int nScale = 3;					  // always positive
	private boolean bScaleUp=false;			  // changes direction of nScale
	private int nDrawMode = GRAPHICMODE;
	private boolean isTrim=false;
	
	// Panels to show coding and hit
	private boolean bPanelsDirty = false;
	private Vector<JPanel> highlightPanels = new Vector<JPanel> ();      
    private boolean bShowHit = false;
    private boolean bShowORF = false;
   
    // For drawing    
 	private static final Color anyHangUnk = Globalx.anyHang; // new Color(180, 180, 180); mediumGray
	private static final Color anyGap 	= Globalx.anyGap;  // new Color(0,200,0); light green; shared with aaZappo
	
	private static final Color ntMisMatch = Globalx.ntMisMatch; // Color.red;
	
	private static final Color aaLtZero 	= Globalx.aaLtZero; // Color.red; 		
	private static final Color aaEqZero	= Globalx.aaEqZero; // new Color(255, 173, 190);  light red
	private static final Color aaGtZero 	= Globalx.aaGtZero; // Color.blue; 	
	private static final Color aaStop	= Globalx.aaStop;   // new Color(138, 0, 184); 	purple; shared with aaZappo
 	
	private static final Color colorORF = Color.YELLOW;
	private static final Color colorHIT = new Color(157, 189, 242);
}
