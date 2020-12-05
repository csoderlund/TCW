package cmp.viewer.seq.align;

import java.awt.Color;
import java.awt.Component;
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
import java.util.Iterator;
import java.util.TreeSet;
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
	
	public BaseAlignPanel ( )
	{
		theFont = new Font("Monospaced", Font.PLAIN, 11); // CAS312 baseFont;
		theBoldFont = new Font("Monospaced", Font.BOLD, 12);
		fontMetrics = getFontMetrics ( theBoldFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );
		setZappo();
	}
	
	// Abstract methods (child class should over-ride)
	// called from classes other than extend AlignBasePanel 
	public void handleClick( MouseEvent e, Point localPoint ) { }
	public double getGraphicalDeadWidth ( ) { return 0; }
	public void refreshPanels ( ) { }

	public    void 		selectNoRows () { }
	public    void 		selectNoColumns () { }
	public    void 		getSelectedSeqIDs ( TreeSet<String> set ) { }
	public    boolean 	hasSelection () { return false; }
	
	protected void 		selectAllRows () { }
	protected void 		selectAllColumns () { }
	
	protected char getChar(boolean bFirst, int nPos) {return ' ';}
	protected boolean isEndGap(boolean bFirst, int nPos) {return false;}
	
	public void setZoom ( int n ) throws Exception
	{ 
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
	public void setMinPixelX ( double dX ) { 
		dMinPixelX = dX; 
		clearPanels ( ); 
	}
	public void setIndexRange ( int nMin, int nMax )  { 
		nMinIndex = nMin;   // the lowest index for bases in the alignment (always 0)
		nMaxIndex = nMax;	// the highest index for bases in the alignment
		clearPanels ( );
	}		
	public int getDrawMode ( ) { 
		return nDrawMode; 
	}
	public void setDrawMode(int mode) {
		nDrawMode = mode;
		clearPanels();
	}
	public void setColorMode(int mode) {
		nColorMode=mode;
	}
	public void setDotMode(boolean mode) {
		bDot = mode;
	}
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		if ( bPanelsDirty ){
			refreshPanels ();
			bPanelsDirty = false;
		}
		drawCodingPanels ( g2 );
	}
	
	//------------- Called from align panels --------------------//
	protected void drawSequence( Graphics2D g2, String label, String alignSeq, 
			double dYTop, double dYBottom , boolean isFirst, boolean isDNA, boolean isPair)
	{
		int start=0, end=alignSeq.length();
		while(start<end-1 && alignSeq.charAt(start) == Globalx.gapCh)  start++;
		while(end>0 &&       alignSeq.charAt(end-1) == Globalx.gapCh)  end--;
		if (end==0) end=alignSeq.length();
		
		if( nDrawMode == GRAPHICMODE )
			drawGraphics( g2, alignSeq, dYTop, dYBottom , isFirst, isDNA, start, end);
		else
			writeSequence( g2, alignSeq, dYTop, dYBottom , isFirst, isDNA, isPair, start, end);
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

		g2.setColor(Color.black);
			
		for( int i = start; i < end; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			char c1 = getChar(isFirst, i); // either consensus or other half of pair
			char c2 = alignSeq.charAt(i); // current
			Color baseColor = Color.BLACK;
			
			if (isEndGap(isFirst, i)) baseColor = getColorEnd(c2);
			else if (isDNA) 		 baseColor = getColorNT(c1, c2);
			else            		 baseColor = getColorAA(c1, c2);
			
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
	private void drawGraphics( Graphics2D g2, String alignSeq, 
			double dYTop, double dYBottom, boolean isFirst, boolean isDNA, int start, int end)
	{
		// Determine the position of the sequence line, but don't draw until after the hashes
		double dXPosStart = calculateDrawX ( start );
		double dXPosEnd = 	calculateDrawX ( end );

		double dHeight = 	dYBottom - dYTop;   // always 15.0
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
		
			char c1 = getChar(isFirst, pos); // either consensus or other half of pair
			char c2 = alignSeq.charAt(pos); // current
			Color baseColor = Color.BLACK;

			if (isEndGap(isFirst, pos)) baseColor = getColorEnd(c2);
			else if (isDNA) 		   baseColor = getColorNT(c1, c2);
			else            		   baseColor = getColorAA(c1, c2);
			
			if (baseColor==Color.black) continue;
				
			int hash=hash_SM;
			if (nColorMode==1) {
				if (aaDefault(c1,c2)==aaLtZero) hash = hash_LG; // get size of hash
			}
			else {
				if ( baseColor== aaLtZero || baseColor == ntMisMatch) hash=hash_LG;
				else if ( baseColor== anyGap || baseColor == anyUnk)  hash=hash_MD;
			}
			
			double dH = hash*2;
			if (isFirst) {
				Hash h = new Hash( g2, dX, dY-hash, dW, dH,  baseColor , c2, pos);
				if (hiPoint!=null) h.isHigh(g2, hiPoint);
				hashSet.add(h);
			}
			else drawHash ( g2, dX, dY-hash, dW, dH, baseColor );
			
			if (baseColor==aaStop)  drawStop(g2, dX, dYTop);
		}
		// Draw the line for the sequence and arrow head
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
	private void drawDot ( Graphics2D g2, double dX, double dY, Color dotColor ) {
		g2.setColor( dotColor );
		g2.draw( new Line2D.Double( dX - 0.5, dY, dX + 0.5, dY ) );		
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
			this.ch = ch;
			this.index = index;
		} 
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
		//private double dx, dy, dw, dh; 
		private char ch;
		private int index;
		private Rectangle2D hRect=null;
	}
	/****************************************************************
	 * XXX Color for both line and text
	 * Drawing c2; c1 is either consensus or opposite pair
	 */
	private Color getColorNT(char c1, char c2) { 
		if (c2==gapCh)  return anyGap;  // colored gap even if consensus is
		if (c1==gapCh)	return Color.black;
		
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
		if (c1==gapCh)		return Color.black;
		if (c2==stopCh)		return aaStop;
		
		if (c2=='X') 		return anyUnk;	
		
		if (c1==c2)         return Color.BLACK;
		
		if (blosumObj.isHighSub(c1, c2))return aaGtZero;
		else							return aaLtZero;
	}
	
	//---------	 Draw a ruler indicating base position-------------------//
	protected void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYBottom )   {
		// Center the text (as much as possible in the input box)
		TextLayout layout = new TextLayout( "9999", theFont, g2.getFontRenderContext() );		
		double dY = (dYBottom + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int nTickIncrement = 100;
		if ( nDrawMode != GRAPHICMODE ) nTickIncrement = 10;
		
		int nTickStart = ( nMinIndex / nTickIncrement ) * nTickIncrement;		
		if ( nTickStart < nMinIndex ) nTickStart += nTickIncrement;
		
		for ( int i = nTickStart; i < nMaxIndex; i += nTickIncrement )
		{	
			double dX;
			if ( nDrawMode != GRAPHICMODE )
				dX = ( calculateWriteX (i) + calculateWriteX (i+1) ) / 2;
			else
				dX = calculateDrawX (i);
			
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
		if( nDrawMode == GRAPHICMODE ) {
			if (bScaleUp) return ( nMaxIndex - nMinIndex + 1 ) * nScale;
			else return ( nMaxIndex - nMinIndex + 1 ) / nScale;
		}
		else return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}

	//-- Remove if coding not added to NT ---------/
	private void drawCodingPanels ( Graphics2D g2 )
	{
		Iterator<JPanel> iterPanel = codingPanels.iterator();
		while ( iterPanel.hasNext() ) {
			JPanel curPanel = iterPanel.next();
			g2.setColor( curPanel.getBackground() );
			g2.fill( curPanel.getBounds() );
		}		
	}
	private void clearPanels ( )
	{
		// Remove the old panels
		bPanelsDirty = true;
		Iterator<JPanel> iter = codingPanels.iterator();
		while ( iter.hasNext() )
			remove( (Component)iter.next() );
		codingPanels.removeAllElements();
		super.repaint();
	}
	/*****************************************************************************/
	public void mouseClicked(MouseEvent e) {  
        for (Hash h : hashSet) {
        	if (h.contains(e.getPoint())) {
        		hiPoint = e.getPoint();
        		repaint();
        		return;
        	}
        }
    }  
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
	private int nMinIndex = 1;			// The lowest base position in the alignment
	private int nMaxIndex = 1;
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;		// The distance from the top of the write cell to the baseline
	
	// Panels to show the coding region - not used, but maybe add to pairwise NT align
	private boolean bPanelsDirty = false;
	private Vector<JPanel> codingPanels = new Vector<JPanel> ();      
	
	private Vector <Hash> hashSet = new Vector <Hash> ();
	private Point hiPoint = null;
	private ScoreAA blosumObj = new ScoreAA();
	
	// Set by calling program in interface
	private int nDrawMode = GRAPHICMODE;
	
	private int     nScale = 1;		// Zoom	
	private boolean bScaleUp=false;	
	
	private boolean bTrim=false;

	private boolean bDot	=true;
	private int nColorMode	= 0;
	
	//-- don't set any color black ---//
	// Default 
	public static Color anyHang 	= new Color(180, 180, 180); // mediumGray
	public static Color anyUnk		= Color.cyan;
	public static Color anyGap 		= new Color(0,200,0);		// light green; shared with aaZappo
	public static Color aaStop		= new Color(138, 0, 184); 	// purple; shared with aaZappo
	
	public static Color ntMisMatch 	= Color.red;
	
	public static Color aaLtZero 	= Color.red; 		// new Color(255, 173, 190); // light red;
	public static Color aaGtZero 	= Color.blue; 		
	
	// Zappo
	private HashMap <Character, Color>  aaZappo	= new HashMap <Character, Color> ();
	public static Color zPhobic		= Color.pink; // new Color(255, 0, 127);	// Rose;
	public static Color zAro		= Color.orange;
	public static Color zPos		= Color.blue;
	public static Color zNeg		= Color.red;
	public static Color zPhilic		= Color.green;
	public static Color zCys		= Color.gray;
	public static Color zCon		= Color.magenta;
	
	protected static final Color selectColor = Color.GRAY;
}
