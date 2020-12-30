package cmp.viewer.seq.align;

/**************************************************************
 * Draws lines or characters for a sequences
 * Inherited by MultiAlignPanel, PairNAlignPanel, Pair3AlignPanel
 */
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JPanel;

import cmp.align.ScoreAA;
import util.database.Globalx;
import util.methods.Out;

public class BaseAlignPanel extends JPanel implements MouseListener
{
	private static final long serialVersionUID = 1;
	  
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	public static String [] colorSchemes = {"BLOSUM62", "Zappo"};
	
	private final char gapCh = Globalx.gapCh;
	private final char stopCh = Globalx.stopCh;
	
	public BaseAlignPanel ( ) {
		theFont = new Font("Monospaced", Font.PLAIN, 11); // CAS312 baseFont;
		theBoldFont = new Font("Monospaced", Font.BOLD, 12);
		fontMetrics = getFontMetrics ( theBoldFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );
		setZappo();
	}
	
	// Abstract methods (child class must over-ride the following four)
	public void handleClick( MouseEvent e, Point localPoint ) { }
	public void refreshPanels ( ) { }
	public void paintComponent(Graphics g){
		super.paintComponent( g ); // XXX LINUX
		refreshPanels();
	}
	
	protected char getChar(boolean bFirst, int nPos) {return ' ';}
	protected boolean isEndGap(boolean bFirst, int nPos) {return false;}
	
	//------- setup -------//
	public void setMinPixelX ( double dX ) { //  lowest pixel on the x-axis for drawing
		dMinPixelX = dX; 
	}
	public void setTrimRange(int nMin, int nMax) {
		nMinTrim = nMin;
		nMaxTrim = nMax+1;
	}
	public void setIndexRange ( int nMin, int nMax )  { 
		nMinIndex = nMinLen = nMin;   // the lowest index for bases in the alignment (always 0)
		nMaxIndex = nMaxLen = nMax;	  // the highest index for bases in the alignment
	}		
	
	//---- change in drawing ----//
	public int getViewMode ( ) { 
		return nViewMode; 
	}
	public void setOpts(int nViewMode, int nColorMode, boolean bDot, boolean bTrim, int nZoom) {
		this.nViewMode = nViewMode;
		this.nColorMode = nColorMode;
		this.bDot = bDot;
		
		this.bTrim = bTrim;
		if (bTrim) {
			nMinIndex = nMinTrim;
			nMaxIndex = nMaxTrim;
		}
		else {
			nMinIndex = nMinLen;
			nMaxIndex = nMaxLen;
		}
		
		if (nZoom<0) {
			nScale = -nZoom;
			bScaleUp=true;
		}
		else { 
			nScale = nZoom; 
			bScaleUp=false;
		}
		super.repaint(); // XXX LINUX refreshPanels();
	}
	
	//------------- Called from align panels --------------------//
	protected void drawSequence( Graphics2D g2, String label, String alignSeq, 
			double dYTop, double dYBottom , int start, int end,
			boolean isFirst, boolean isDNA, boolean isPair) 
	{	
		if( nViewMode == GRAPHICMODE )
			drawSequenceLine( g2, alignSeq, dYTop, dYBottom , isFirst, isDNA, start, end+1); 
		else
			writeSequence( g2, alignSeq, dYTop, dYBottom , isFirst, isDNA, isPair, start, end+1);
	}
	//--------------- View NT/AA -------------------------------/
	// BOLD: (1) isGrp and isFirst (consensus)
	//       (2) is different from consensus or other sequence in pair, and not a gap
	// DOT:  bDot and is the same, but not consensus
	private void writeSequence ( Graphics2D g2, String alignSeq, 
			double dYTop, double dYBottom , boolean isFirst , boolean isDNA, boolean isPair, int start, int end) 
	{
		boolean isMulti = !isPair;
		double dWriteBaseline = (dYBottom - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 

		int s = (bTrim) ? Math.max(start, nMinTrim) : start;
		int e = (bTrim) ? Math.min(end,   nMaxTrim) : end;
		g2.setColor(Color.black);
			
		for( int i = s; i < e; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			char c1 = getChar(isFirst, i); // either consensus or other half of pair
			char c2 = alignSeq.charAt(i); // current
			Color baseColor = Color.BLACK;
			
			if (isEndGap(isFirst, i)) baseColor = getColorEnd(c2);
			else if (isDNA) 		  baseColor = getColorNT(c1, c2);
			else            		  baseColor = getColorAA(c1, c2);
			
			boolean isBold=false, isDot=false;
	
			if (isFirst && isMulti) isBold=true; // Consensus always bold and no dot
			else {
				if (c1!=c2) isBold=true;
				else if (bDot && !isFirst) isDot=true;
			}
			writeCenteredBase ( g2, c2, baseColor, dX, dWriteBaseline, isBold, isDot);
		}
	}
	
	private void writeCenteredBase ( Graphics2D g2, char chBase, Color theColor, double dCellLeft, 
			double dBaseline, boolean isBold, boolean isDot )
	{
		g2.setColor ( theColor ); // colors the text
		
		String str = (isDot) ? "." : ("" + chBase);
		Font f = (isBold) ? theBoldFont : theFont;
		
		TextLayout layout = new TextLayout( str, f, g2.getFontRenderContext() );
		
	   	Rectangle2D bounds = layout.getBounds();
		float fCharWidth = (float)bounds.getWidth();
		
		// Adjust x draw position to center the character in its rectangle
		float fX = (float)dCellLeft + (float)(dFontCellWidth - 1.0f) / 2.0f - fCharWidth / 2.0f; 
		   
		layout.draw( g2, fX, (float)dBaseline );

		g2.setColor ( Color.black );		
	}
	/*******************************************************************************/
	//--------------------- Draw line with hashes ---------------------------------//
	private void drawSequenceLine( Graphics2D g2, String alignSeq, 
			double dYTop, double dYBottom, boolean isFirst, boolean isDNA, int start, int end)
	{
		// Determine the position of the sequence line, but don't draw until after the hashes
		int s = (bTrim) ? Math.max(start, nMinTrim) : start;
		int e = (bTrim) ? Math.min(end,   nMaxTrim) : end;
		
		double dHeight = 	dYBottom - dYTop;   // always 15.0
		double dY 	= 		dHeight / 2.0 + dYTop; // center, so +/- hash

		int hashHeight =   (int)(dHeight/2) - 2;
		int hash_LG = 		hashHeight;
		int hash_MD = 		hashHeight - 1;
		int hash_SM = 		hashHeight - 2;
		
		double dW=0.5;  // width of line
		if (bScaleUp && nScale>2) dW = nScale*0.5;

		// For each letter in the sequence
		for( int pos = s; pos < e; pos++) {
			double dX = calculateDrawX ( pos);
		
			char c1 = getChar(isFirst, pos); // either consensus or other half of pair
			char c2 = alignSeq.charAt(pos); // current
			Color baseColor = Color.BLACK;

			if (isEndGap(isFirst, pos)) baseColor = getColorEnd(c2);
			else if (isDNA) 		    baseColor = getColorNT(c1, c2);
			else            		    baseColor = getColorAA(c1, c2);
			
			if (baseColor==Color.black) continue;
				
			int hash=hash_SM;
			if (nColorMode==1) {
				Color cHigh = aaDefault(c1,c2);
				if (cHigh==aaLtZero) hash = hash_LG; // get size of hash
				else if (cHigh==aaEqZero) hash = hash_MD;
			}
			else {
				if ( baseColor== aaLtZero || baseColor == ntMisMatch) hash=hash_LG;
				else if ( baseColor== anyGap || baseColor == anyUnk || baseColor==aaEqZero)  hash=hash_MD;
			}
			
			double dH = hash*2;
			if (isFirst) {
				Hash h = new Hash( g2, dX, dY-hash, dW, dH,  baseColor , c2, pos);
				hashSet.add(h);
			}
			else drawHash ( g2, dX, dY-hash, dW, dH, baseColor );
			
			if (baseColor==aaStop)  drawStop(g2, dX, dYTop);
		}
		// Draw the line for the sequence and arrow head
		double dXPosStart = calculateDrawX ( s );
		double dXPosEnd = 	calculateDrawX ( e );

		g2.setColor( Color.black );
		g2.draw( new Line2D.Double( dXPosStart, dY, dXPosEnd, dY ) );

		double dArrowHeight = dHeight / 2.0 - 2;
		drawArrowHead ( g2, dXPosEnd, dY, dArrowHeight, true /* right */ );
		
		g2.setColor(Color.black);
	}
	private void drawStop(Graphics2D g2, double dx, double dYTop) {
		g2.setColor( aaStop);
		g2.draw( new Line2D.Double( dx,   dYTop-1, dx,   dYTop+2 ) ); // |
		g2.draw( new Line2D.Double( dx-1, dYTop-1, dx+1, dYTop-1 ) ); // -
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
	
	private void drawHash ( Graphics2D g2, double dx, double dy, double dw, double dh, Color hashColor ){
		g2.setColor( hashColor );
		if (bScaleUp) {
			Rectangle2D hRect = new Rectangle2D.Double(dx, dy, dw, dh);
			g2.draw(hRect);
			g2.fill(hRect);
		}
		else { // this still bleeds over with 1:2 where is doesn't for 3.1.1
			g2.draw( new Line2D.Double( dx,  dy, dx, dy + dh ) ); // x1, y1, x2, y2
		}
	}
	/*****************************************************************************/
	private class Hash  {
		public Hash( Graphics2D g2, double dx, double dy, double dw, double dh, Color hashColor, char ch, int index)
		{
			g2.setColor( hashColor );
		
			if (bScaleUp) {
				hRect = new Rectangle2D.Double(dx, dy, dw, dh);
				g2.draw(hRect);
				g2.fill(hRect);
			}
			else {
				g2.draw( new Line2D.Double( dx,  dy, dx, dy+dh ) );
			}
			//this.ch = ch;
			//this.index = index;
		} 
		/** this is to make consensus rectangle clickable for information as shown for sequence
		public boolean isHigh(Graphics2D g2, Point p) {// CAS313 will be for clickable hashes
			if (hRect!=null && contains(p)) {
				g2.setColor(Color.black);
				//g2.drawRect(dx, dy, dw, dh);
				Out.debug("hash click " + ch + " " + index);
				return true;
			}
			else return false;
		}
		public boolean contains(Point p) {
			double x1 = hRect.getX()-1;
			double x2 = x1+hRect.getWidth()+1;
			double y1 = hRect.getY()-1;
			double y2 = y1+hRect.getHeight()+1;
			if (p.getX()>=x1 && p.getX()<=x2 && p.getY()>=y1 && p.getY()<=y2) {
				return true;
			}
			else return false;
		}
		**/
		//private double dx, dy, dw, dh; 
		//private char ch;
		//private int index;
		private Rectangle2D hRect=null;
	}
	/****************************************************************
	 * XXX Color for both line and text
	 * Drawing c2; c1 is either consensus or opposite pair
	 */
	private Color getColorNT(char c1, char c2) { 
		if (c2==gapCh)  return anyGap;  // colored gap even if consensus is
		
		if (c2=='n') 	return anyUnk;	// colored anyUnk even if all are anyUnk
		
		if (c1==c2)		return Color.BLACK;
		
		return ntMisMatch;
	}
	// There are no blanks; only gap, N, A, C, T, G; everything is upper case
	private Color getColorEnd(char c2) {
		if (c2==stopCh)	 return aaStop;
		else 			 return anyHang;
	}
	
	private Color getColorAA(char c1, char c2) {
		if (nColorMode==0) return aaDefault(c1, c2);
		else               return aaZappo.get(c2);
	}
	
	private Color aaDefault(char c1, char c2) { //isFirst c1==c2
		if (c2==gapCh)		return anyGap;
		if (c2==stopCh)		return aaStop;
		if (c2=='X') 		return anyUnk;	
		
		if (c1==gapCh)		return Color.BLACK;
		
		if (c1==c2)         return Color.BLACK;
		
		int s = ScoreAA.getBlosum(c1, c2);
		if (s>0) return aaGtZero;
		if (s<0) return aaLtZero;
		return aaEqZero;
	}
	
	//---------	 Draw a ruler indicating base position-------------------//
	protected void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYBottom )   {
		// Center the text (as much as possible in the input box)
		TextLayout layout = new TextLayout( "9999", theFont, g2.getFontRenderContext() );		
		double dY = (dYBottom + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int nTickInc = ( nViewMode==GRAPHICMODE ) ? 100 : 10;
		
		int nTickStart = ( nMinIndex / nTickInc ) * nTickInc;		
		if ( nTickStart < nMinIndex ) nTickStart += nTickInc;
		
		for ( int i = nTickStart; i < nMaxIndex; i += nTickInc ) {	
			double dX;
			if ( nViewMode == GRAPHICMODE ) 
				dX = calculateDrawX (i);
			else 
				dX = (calculateWriteX(i) + calculateWriteX(i+1)) / 2;
			
			if ( dX > dXMin ) 
				drawVerticalText ( g2, String.valueOf(i), dX, dY );
		}
	}
	private void drawVerticalText ( Graphics2D g2, String str, double dX, double dY ){
		if ( str.length() == 0 ) return;
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );

		g2.rotate ( - Math.PI / 2.0 );
		g2.setColor ( Color.BLACK );
		float fHalf = (float)(layout.getBounds().getHeight() / 2.0f);
		layout.draw( g2, (float)-dY, (float)dX + fHalf );	
		g2.rotate ( Math.PI / 2.0 );
	}	
	
	//------------------ Utility methods -------------------------/
	private double calculateDrawX ( int nBasePos ) { 
		if (bScaleUp) return dMinPixelX + ( nBasePos - nMinIndex + 1 ) * nScale;
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nScale;
	}
	
	// Returns the pixel position for the left hand side of the "box" to write in
	protected double calculateWriteX ( int nBasePos ) {
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}	
	protected double getTextWidth ( String str )  { // all align call
		if ( str == null ) return 0;
		else
			return fontMetrics.stringWidth ( str );
	}
	protected void drawName ( Graphics2D g2, String name, double dXleft, double dYtop ){
		//drawText ( g2, str, dX, dY, Color.BLACK );
		if ( name.length() == 0 ) return;
		
		g2.setColor (Color.BLACK );
		
		TextLayout layout = new TextLayout( name, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYtop + dFontAscent); 
		layout.draw( g2, (float) dXleft, fBaseline );
	}
	protected int calculateIndexFromWriteX ( double dX ) {// called in multiAlign
		return (int)( (dX - dMinPixelX) / dFontCellWidth ) + nMinIndex; 
	}	
	protected double getSequenceWidth ( ){
		if( nViewMode == GRAPHICMODE ) {
			if (bScaleUp) return ( nMaxIndex - nMinIndex + 1 ) * nScale;
			else          return ( nMaxIndex - nMinIndex + 1 ) / nScale;
		}
		else return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}
	/*****************************************************************************/
	public void mouseClicked(MouseEvent e) { }  
    public void mouseEntered(MouseEvent e) {}  
    public void mouseExited(MouseEvent e) {}  
    public void mousePressed(MouseEvent e) {}  
    public void mouseReleased(MouseEvent e) {} 
    public void mouseMoved(MouseEvent e) {} 
    /*****************************************************************************/
	private void setZappo() {
		aaZappo.put('I', zPhobic); 	aaZappo.put('L', zPhobic); 	aaZappo.put('V', zPhobic);
		aaZappo.put('A', zPhobic); 	aaZappo.put('M', zPhobic);
		aaZappo.put('F', zAro);		aaZappo.put('W', zAro);		aaZappo.put('Y', zAro);
		aaZappo.put('K', zPos);		aaZappo.put('R', zPos);		aaZappo.put('H', zPos);
		aaZappo.put('D', zNeg);		aaZappo.put('E', zNeg);
		aaZappo.put('S', zPhilic); 	aaZappo.put('T', zPhilic);
		aaZappo.put('N', zPhilic); 	aaZappo.put('Q', zPhilic);
		aaZappo.put('P', zCon);		aaZappo.put('G', zCon);
		aaZappo.put('C', zCys); 	aaZappo.put('-', anyGap);
		aaZappo.put('X', anyUnk); 	aaZappo.put('*', aaStop);
	}
	
	private FontMetrics fontMetrics = null;
	private Font theFont = null;
	private Font theBoldFont = null;
	
	// Attibutes for positioning
	private double dMinPixelX = 0;		// The lowest pixel (associated base at min index) 
	private int nMinIndex = 0, nMaxIndex = 0; // Either nMinLen or nMinTrim; 
	private int nMinLen=0, nMaxLen=0;   // Zero and length of longest sequence, respectively
	private int nMinTrim =0, nMaxTrim = 0; // CAS313 Where consensus starts/stops having gaps for trim
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;		// The distance from the top of the write cell to the baseline
	
	private Vector <Hash> hashSet = new Vector <Hash> ();
	
	// Set by calling program in interface
	private int nViewMode = GRAPHICMODE;
	
	private int     nScale = 1;		// Zoom	
	private boolean bScaleUp=false;	
	
	private boolean bDot	=true;
	private boolean bTrim	=false; // CAS313
	private int nColorMode	= 0;
	
	//-- don't set any color black ---//
	// Default 
	public static final Color anyHang 	= Globalx.anyHang; // new Color(180, 180, 180); mediumGray
	public static final Color anyUnk	= Globalx.anyUnk;  // Color.cyan;
	public static final Color anyGap 	= Globalx.anyGap;  // new Color(0,200,0); light green; shared with aaZappo
	
	public static final Color ntMisMatch = Globalx.ntMisMatch; // Color.red;
	
	public static final Color aaLtZero 	= Globalx.aaLtZero; // Color.red; 		
	public static final Color aaEqZero	= Globalx.aaEqZero; // new Color(255, 173, 190);  light red
	public static final Color aaGtZero 	= Globalx.aaGtZero; // Color.blue; 	
	public static final Color aaStop	= Globalx.aaStop;   // new Color(138, 0, 184); 	purple; shared with aaZappo
	
	// Zappo
	private HashMap <Character, Color>  aaZappo	= new HashMap <Character, Color> ();
	public static final Color zPhobic	= Color.pink; // new Color(255, 0, 127);	// Rose;
	public static final Color zAro		= Color.orange;
	public static final Color zPos		= Color.blue;
	public static final Color zNeg		= Color.red;
	public static final Color zPhilic	= Color.green;
	public static final Color zCys		= Color.gray;
	public static final Color zCon		= Color.magenta;
	
	protected static final Color selectColor = Color.GRAY;
}
