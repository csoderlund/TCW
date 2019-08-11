package cmp.viewer.seq;

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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JPanel;

import util.ui.MultilineTextPanel;
import cmp.align.ScoreAA;
import cmp.align.PairAlignData;
import cmp.viewer.MTCWFrame;
import util.database.Globalx;
import util.methods.Out;

public class AlignPair3Panel  extends JPanel
{
	private static final char gapCh=Globalx.gapCh;
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	private Color background1 = new Color(0xEEFFEE);
	private Color background2 = new Color(0xEEFFFF);
	private static final Font textFont = new Font("Sans", Font.PLAIN, 11);
	
	public AlignPair3Panel ( MTCWFrame parentFrame, PairAlignData theAlignment)
	{		
		alignDataObj = theAlignment;
		descLines = alignDataObj.getDescLines();
		isDNA = alignDataObj.isNT();
		
		Color c = background2; // only works after redisplay
		if (isDNA) c = background1; 
		
		super.setBackground( Globalx.BGCOLOR );
		super.setLayout(null); // Only descLines or graphics will show if declared with a regulat setLayout(new...
								// but lose scroll with this.
		//setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		nTopGap = 10;
		nLeftGap = 10;	
		
		theFont = textFont;
		fontMetrics = getFontMetrics ( textFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );

		// XXX
		alignSeq1 = alignDataObj.getAlignFullSeq1();
		for (nSeq1Start=0; 
				nSeq1Start < alignSeq1.length() && alignSeq1.charAt(nSeq1Start) == gapCh; 
				nSeq1Start++);
		for (nSeq1Stop=alignSeq1.length() - 1; 
				nSeq1Stop >= 0 && alignSeq1.charAt(nSeq1Stop) == gapCh; 
				nSeq1Stop--);
		
		alignSeq2 = alignDataObj.getAlignFullSeq2();
		for (nSeq2Start=0; 
				nSeq2Start < alignSeq2.length() && alignSeq2.charAt(nSeq2Start) == gapCh; 
				nSeq2Start++);
		for (nSeq2Stop=alignSeq2.length() - 1; 
				nSeq2Stop >= 0 && alignSeq2.charAt(nSeq2Stop) == gapCh; 
				nSeq2Stop--);
		
		seq1Label = alignDataObj.getSeqID1();
		seq2Label = alignDataObj.getSeqID2();
		
		dSequenceStart = Math.max( getTextWidth( seq1Label ), getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nLeftGap;
		setMinPixelX ( dSequenceStart );
		setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );
		
		dFrameLeftX = nLeftGap;
		dFrameTopY = nTopGap;
		dRulerHeight = getTextWidth( "999999" );
	}
	private void addDescLines() {
		try {
			if (headerPanel!=null) remove (headerPanel);
			
			headerPanel = new MultilineTextPanel ( textFont, descLines, 
					nInsetGap, (int)getFrameWidth ( ) - 2, 1); 
			headerPanel.setBackground( Color.WHITE );  
			add ( headerPanel );  
			
			headerPanel.setLocation( nLeftGap + 1, nTopGap + 1 );		
			dHeaderHeight = headerPanel.getHeight() + 1;
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY, getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dFrameLeftX - 2, dFrameTopY - 2, getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() )
		{
			// Outline whole thing
			Stroke oldStroke = g2.getStroke();
			g2.setColor( Globalx.selectColor );
			g2.setStroke( new BasicStroke (3) );
			g2.draw( highlightBox );
			g2.setStroke ( oldStroke );
			
			if ( bSeq1Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectionRow1Top, getFrameRight ( ) - dFrameLeftX, dSelectionMid - dSelectionRow1Top ) );		

			if ( bSeq2Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectionMid, getFrameRight ( ) - dFrameLeftX, dSelectionRow2Bottom - dSelectionMid ) );					
			
			g2.setColor( Color.BLACK );
		}
		g2.setColor( borderColor );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dFrameLeftX, dDivider1Y, getFrameRight ( ), dDivider1Y ) );
	
		drawRuler( g2, 0, dRulerTop, dRulerBottom );
		
		drawText( g2, seq1Label, dFrameLeftX + nInsetGap, dSeq1Top );
		drawSequence ( g2, true, seq1Label, alignSeq1, dSeq1Top, dSeq1Bottom );
		
		drawText( g2, seq2Label, dFrameLeftX + nInsetGap, dSeq2Top );
		drawSequence ( g2, false, seq2Label, alignSeq2, dSeq2Top, dSeq2Bottom );
	}
	private void drawSequence( Graphics2D g2, boolean isFirst, String label, String sequence, double dYTop, double dYBottom )
	{
		if( nDrawMode == GRAPHICMODE )
			drawSequenceLine( g2, isFirst, label, sequence, dYTop, dYBottom );
		else
			writeSequenceLetters( g2, isFirst, label, sequence, dYTop, dYBottom );
	}
	
	private void writeSequenceLetters ( Graphics2D g2, boolean isFirst, 
			String label, String sequence, double dYTop, double dYBottom  ) 
	{
		double dWriteBaseline = (dYBottom - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 

		g2.setColor(Color.black);
			
        int nStart = 0;
        int nEnd = sequence.length()-1; 
        
		for( int i = nStart; i < nEnd; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			Color baseColor = Color.black;
			
			if ( getIsStop (isFirst, sequence, i ) )			baseColor = Color.gray;
			else if ( getIsNAt (isFirst, sequence, i ) )		baseColor = Globalx.mediumGray;  
			else if ( getIsHighSubAt ( sequence, i ) ) 		baseColor = Globalx.purple;
			else if ( getIsGapAt(isFirst, sequence, i )) 	baseColor = Globalx.darkGreen;
			else if ( getIsMismatchAt( sequence, i ) )		baseColor = Globalx.mismatchRed;
			
			drawCenteredBase ( g2, sequence.charAt(i), baseColor, dX, dWriteBaseline );
		}
	}
	private void drawSequenceLine( Graphics2D g2, boolean isFirst,
			String label,String sequence,double dYTop, double dYBottom)
	{
		int startGapEnd = 0;
		int endGapStart = sequence.length();
		
		// XXX Find the locations of the start and stop 
		while(startGapEnd<endGapStart-1 &&
				sequence.charAt(startGapEnd) == Globalx.gapCh)   startGapEnd++;
		while(endGapStart>0 && 
				sequence.charAt(endGapStart-1) == Globalx.gapCh) endGapStart--;
		if (endGapStart==0) endGapStart=sequence.length();
		
		// Determine the position of the sequence line, but don't draw until after the hashes
		// draw the line and arrow heads
		double dXPosStart = calculateDrawX ( startGapEnd );
		double dXPosEnd = calculateDrawX ( endGapStart );
		
		double dHeight = dYBottom - dYTop;
		double dYCenter = dHeight / 2.0 + dYTop;
		
		int RED_HASH_UP = (int)(dHeight/2) - 2;
		int RED_HASH_DOWN = RED_HASH_UP;
		int GREEN_HASH_UP = RED_HASH_DOWN - 1;
		int GREEN_HASH_DOWN = RED_HASH_DOWN - 1;
		int GRAY_HASH_UP = GREEN_HASH_UP - 2;
		int GRAY_HASH_DOWN = GREEN_HASH_DOWN - 2;
		int BLUE_HASH_UP = GREEN_HASH_UP - 2;
		int BLUE_HASH_DOWN = GREEN_HASH_DOWN - 2;	
	
		boolean bBlueStopHash;  
		boolean bGreenHash;
		boolean bRedHash;
		boolean bGreyHash;
		boolean bBlueHash;
		boolean bPurpleHash;
		boolean bGrayHash;
		int nCurBasesToGroup = -1;

		// For each letter in the sequence
		for( int i = startGapEnd; i < endGapStart;)
		{
			double dHashXPos = calculateDrawX ( i );
			
			bBlueStopHash = false;
			bGreenHash = false;
			bRedHash = false;
			bBlueHash = false;
			bGreyHash = false;
			bPurpleHash = false;
			bGrayHash = false;
	
			if ( nCurBasesToGroup <= 0 )
			{
				// We may short the first group so that all grouping is aligned between ESTs
				nCurBasesToGroup = getBasesPerPixel ();
				nCurBasesToGroup -= ( getMinIndex () ) % getBasesPerPixel ();
			}
			else
			{
				// Not the first group, do the full amount
				nCurBasesToGroup = getBasesPerPixel ();
			}
			for ( int j = 0; i < sequence.length() && j < nCurBasesToGroup; ++i, ++j )
			{			
				if ( getIsEndGapAt(isFirst, sequence, i))		bGrayHash = true;
				else if ( getIsNAt (isFirst, sequence, i ) )		bGreyHash = true;  		// n's and x's
				else if ( getIsHighSubAt ( sequence, i ) )	bPurpleHash = true;       // aa only
				else if ( getIsGapAt (isFirst, sequence, i ) )	bGreenHash = true;
				else if ( getIsMismatchAt ( sequence, i ))		bRedHash = true;
				if ( getIsStop(isFirst, sequence, i ))			bBlueStopHash = true;
			}
			if ( bBlueStopHash ) // little T above sequence
			{
				drawHash ( g2, dHashXPos, dYTop, dYTop + 2, Globalx.mediumGray );	
				g2.draw( new Line2D.Double( dHashXPos - 1, dYTop, dHashXPos + 1, dYTop ) );
			}
			if ( bRedHash ) // larger up down - mismatch
				drawHash ( g2, dHashXPos, dYCenter - RED_HASH_UP, dYCenter + RED_HASH_DOWN, Globalx.mismatchRed );

			if ( bGreenHash )// little smaller up down - gap
				drawHash ( g2, dHashXPos, dYCenter - GREEN_HASH_UP, dYCenter + GREEN_HASH_DOWN, Globalx.gapGreen );

			if( bGrayHash )
				drawHash ( g2, dHashXPos, dYCenter - GRAY_HASH_UP, dYCenter + GRAY_HASH_DOWN, Globalx.mediumGray );

			if ( bGreyHash ) // smaller up and down - N's or x's
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.mediumGray );	

			if ( bPurpleHash )  // little smaller up down - substitute
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.purple );

			if( bBlueHash ) // smaller up down - low quality
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.lowQualityBlue );
		}

		// Draw the line for the sequence
		g2.setColor( Color.black );
		g2.draw( new Line2D.Double( dXPosStart, dYCenter, dXPosEnd, dYCenter ) );

		// Put a green dot on the line anywhere there is a gap (overwrite that position of the line)
		// getDrawGreenDotAt always returns false
		for(int i = 0; i < sequence.length(); ++i )
		{
			if ( getDrawGreenDotAt ( sequence, i ) )
			{
				double dHashXPos = calculateDrawX (i);
				drawDot ( g2, dHashXPos, dYCenter, Globalx.gapGreen );
			}
		}

		// Draw the arrow head
		{
			double dArrowHeight = dHeight / 2.0 - 2;
			drawArrowHead ( g2, dXPosEnd, dYCenter, dArrowHeight, true /* right */ );
			
			g2.setColor(Color.black);
		}
	}

	private void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYBottom )  
	{
		// Center the text (as much as possible in the input box)
		TextLayout layout = new TextLayout( "9999", theFont, g2.getFontRenderContext() );		
		double dY = (dYBottom + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int nTickIncrement = ( nDrawMode == GRAPHICMODE ) ? 100 : 10;
		
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
	private boolean getIsStop (boolean isFirst,  String seq, int nPos ) // 
	{   
		if (isDNA) return false;
		
		if (isFirst && alignSeq1.charAt( nPos ) == '*') return true;
		if (!isFirst && alignSeq2.charAt( nPos ) == '*') return true;

		return false;
	}
	
	private boolean getIsNAt (boolean isFirst, String seq, int nPos ) // ambiguous
	{  
		char x = 'X';
		if (isDNA) x = 'N';
		
		char c1 = alignSeq1.charAt( nPos );
		char c2 = alignSeq2.charAt( nPos );
		
		if (isFirst && (c1 == x || c2 == ' ')) return true;
		if (!isFirst && (c2 == x || c1 == ' ')) return true;
		return false;
	}
	
	private boolean getIsEndGapAt(boolean isFirst, String seq, int nPos) {
		if (isFirst  && (nPos < nSeq2Start || nPos > nSeq2Stop)) return true;
		if (!isFirst && (nPos < nSeq1Start || nPos > nSeq1Stop)) return true;
		return false;
	}
	private boolean getIsGapAt (boolean isFirst, String seq, int nPos ) // only green on gap strand
	{ 
		if ( isFirst && alignSeq1.charAt( nPos ) == Globalx.gapCh) return true;
		if (!isFirst && alignSeq2.charAt( nPos ) == Globalx.gapCh) return true;
		return false;
	}
	
	private boolean getIsMismatchAt ( String seq, int nPos ) 
	{ 	
		char x = 'X';
		if (isDNA) x = 'N';
		char x1 = alignSeq1.charAt( nPos );
		char x2 = alignSeq2.charAt( nPos );
		
		if (x1 == ' ' || x1 == Globalx.gapCh || x1 == x) return false; // space is no base
		if (x2 == ' ' || x2 == Globalx.gapCh || x2 == x) return false; // 

		return x1 != x2; // mis both strands
	}
	
	private boolean getIsHighSubAt ( String seq, int nPos )         // sub both strands
	{ 
		if (isDNA) return false;
		char a1 = alignSeq1.charAt( nPos );
		char a2 = alignSeq2.charAt( nPos );
		if (a1==a2) return false;
		return scoreObj.isHighSub(a1, a2);
	}
	private double getFrameWidth ( )
	{
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	private double getSequenceWidth ( )
	{
		if( nDrawMode == GRAPHICMODE ) return getDrawWidth();
		else return getWriteWidth();
	}
	private double getFrameRight ( )
	{
		return dFrameLeftX + getFrameWidth ( );
	}
	
	private void selectAll () { 
		bSeq1Selected = bSeq2Selected = true; 
		repaint (); 
	};
	
	public void selectNone () {	
		bSeq1Selected = bSeq2Selected = false; 
		repaint ();
	};
	
	private boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	public void setBorderColor(Color newColor) {
		borderColor = newColor;
	}
	public void setDrawMode(int mode) {
		nDrawMode = mode;
  		setMinPixelX ( dSequenceStart );
	}
	public void setBasesPerPixel ( int n ) throws Exception
	{
		setMinPixelX ( dSequenceStart );
		nBasesPerPixel = n; 
		addDescLines();
		
		dFrameHeight =  dHeaderHeight 						// Header
					 + nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y =   		dFrameTopY + dHeaderHeight;
		dRulerTop =    		dDivider1Y + nInsetGap;
		dRulerBottom = 		dRulerTop + dRulerHeight;
		dSeq1Top =     		dRulerBottom + nInsetGap;
		dSeq1Bottom =  		dSeq1Top + nRowHeight;
		dSeq2Top =     		dSeq1Bottom + nInsetGap;
		dSeq2Bottom =  		dSeq2Top + nRowHeight;
		dSelectionRow1Top = dSeq1Top - nInsetGap / 2.0;
		dSelectionMid = 		dSeq1Bottom + nInsetGap / 2.0;
		dSelectionRow2Bottom = dSeq2Bottom + nInsetGap;
	}
	public void handleClick( MouseEvent e, Point localPoint )
	{
        if ( selectionBox.contains(localPoint) )
        {  
	    		if ( dSelectionRow1Top <= localPoint.y && localPoint.y <= dSelectionRow2Bottom )
	    		{
	    			if ( !e.isControlDown() && !e.isShiftDown() )selectNone();
	    			
	    			if ( dSelectionRow1Top <= localPoint.y && localPoint.y <= dSelectionMid )
	    				bSeq1Selected = !bSeq1Selected;
	    			else if ( dSelectionMid <= localPoint.y && localPoint.y <= dSelectionRow2Bottom )
	    				bSeq2Selected = !bSeq2Selected;
	    		}
	    		else
	    		{	
	                // Click is inside the box, but not within any of the sequences (i.e. in the header)
	    			if ( ( e.isControlDown() || e.isShiftDown() ) && hasSelection () ) selectNone ();
	    			else selectAll ();
            }
        }
        else if ( !e.isControlDown() && !e.isShiftDown() ) {       
            selectNone ();                
        }
     
		repaint();
	}
	public PairAlignData getAlignData() { 
		return alignDataObj;
	}
	
	private boolean getDrawGreenDotAt ( String seq, int nPos ) { return false; }
	
	private double getDrawWidth ( )
	{
		return ( nMaxIndex - nMinIndex + 1 ) / nBasesPerPixel;
	}

	private double getWriteWidth ( )
	{
		return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}
	
	// Returns the pixel position for the left hand side of the "box" to write in
	private double calculateWriteX ( int nBasePos ) 
	{
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}	
	private double calculateDrawX ( int nBasePos ) 
	{ 
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nBasesPerPixel;
	}
	private double getTextWidth ( String str )
	{
		if ( str == null ) return 0;
		else return fontMetrics.stringWidth ( str );
	}
	private void drawText ( Graphics2D g2, String str, double dXLeft, double dYTop, Color theColor )
	{
		if ( str.length() == 0 ) return;
		
		g2.setColor ( theColor );
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYTop + dFontAscent); 
		layout.draw( g2, (float)dXLeft, fBaseline );
	}
	private void drawText ( Graphics2D g2, String str, double dX, double dY )
	{
		drawText ( g2, str, dX, dY, Color.BLACK );
	}
	
	private void drawCenteredBase ( Graphics2D g2, char chBase, Color theColor, double dCellLeft, double dBaseline )
	{
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
	
	private void clearPanels ( )
	{
		Iterator<JPanel> iter = codingPanels.iterator();
		while ( iter.hasNext() )
			remove( (Component)iter.next() );
		codingPanels.removeAllElements();
		repaint();
	}
	
	private void drawHash ( Graphics2D g2, double dHashXPos, double dHashTopY, double dHashBottomY, Color hashColor )
	{
		g2.setColor( hashColor );
		g2.draw( new Line2D.Double( dHashXPos,  dHashTopY, dHashXPos, dHashBottomY ) );
	}
	
	private void drawDot ( Graphics2D g2, double dX, double dY, Color dotColor )
	{
		g2.setColor( dotColor );
		g2.draw( new Line2D.Double( dX - 0.5, dY, dX + 0.5, dY ) );		
	}
	
	private void drawArrowHead ( Graphics2D g2, double dArrowPntX, double dArrowPntY, double dHeight, boolean bRight )
	{
		final double ARROW_WIDTH = dHeight - 1;
		
		double dYStartTop = dArrowPntY - dHeight;
		double dYStartBottom = dArrowPntY + dHeight;
		double dXStart = dArrowPntX;
		
		if ( bRight ) 	dXStart -= ARROW_WIDTH;
		else			dXStart += ARROW_WIDTH;

		g2.draw( new Line2D.Double( dXStart, dYStartTop, dArrowPntX, dArrowPntY ) );
		g2.draw( new Line2D.Double( dXStart - 1, dYStartTop, dArrowPntX - 1, dArrowPntY ) );
		g2.draw( new Line2D.Double( dXStart, dYStartBottom, dArrowPntX, dArrowPntY ) );
		g2.draw( new Line2D.Double( dXStart - 1, dYStartBottom, dArrowPntX - 1, dArrowPntY ) );
	}
	
	// Positioned by the center of the left side
	private void drawVerticalText ( Graphics2D g2, String str, double dX, double dY )
	{
		if ( str.length() == 0 || str.equals("0")) return;
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );

		g2.rotate ( - Math.PI / 2.0 );
		g2.setColor ( Color.BLACK );
		float fHalf = (float)(layout.getBounds().getHeight() / 2.0f);
		layout.draw( g2, (float)-dY, (float)dX + fHalf );	
		g2.rotate ( Math.PI / 2.0 );
	}	
	
	private int getBasesPerPixel ( ) { return nBasesPerPixel; }
	
	// Min Pixel X : the lowest pixel on the x-axis for drawing; corresponds to the base at Min Index
	private void setMinPixelX ( double dX ) { 
		dMinPixelX = dX; 
		clearPanels ( ); 
	}
	
	private void setIndexRange ( int nMin, int nMax ) 
	{ 
		if ( nMin >= nMax ) { 
			System.err.println("TCW error: The minimum index " + nMin + " is >= to the maximum " + nMax);
			nMin=0;
			nMax=10; // force an unmatched alignment
		}
		nMinIndex = nMin;   // the lowest index for bases in the alignment
		nMaxIndex = nMax;	// the highest index for bases in the alignment
		clearPanels ( );
	}
	private int getMinIndex ( ) { return nMinIndex; }	
			
	public double getGraphicalDeadWidth ( )
	{
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSequenceStart + nInsetGap;
	}
	public int getTotalBases ( ) { return nMaxIndex - nMinIndex + 1; }
	
	private FontMetrics fontMetrics = null;
	private Font theFont = null;
	
	// Attibutes for positioning
	private double dMinPixelX = 0;		// The lowest pixel (associated base at min index) 
	private int nMinIndex = 1;			// The lowest base position in the alignment
	private int nMaxIndex = 1;
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;		// The distance from the top of the write cell to the baseline
	private int nBasesPerPixel = 3;			

	private int nDrawMode = GRAPHICMODE;
	
	private PairAlignData alignDataObj=null;
	private String alignSeq1 = "";
	private int nSeq1Start = 0;
	private int nSeq1Stop = 0;
	private String alignSeq2 = "";
	private int nSeq2Start = 0;
	private int nSeq2Stop = 0;
	private String seq1Label = "";
	private String seq2Label = "";
	private boolean isDNA = true;
	
    private MultilineTextPanel headerPanel=null;

	boolean bSeq1Selected = false;
	boolean bSeq2Selected = false;	
	Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	private int nTopGap = 0;
	private int nLeftGap = 0;
	private double dFrameLeftX = 0;
	private double dFrameTopY = 0;
	private double dFrameHeight = 0;
	private double dDivider1Y = 0;
	private double dRulerTop = 0;
	private double dRulerBottom = 0;
	private double dSeq1Top = 0;
	private double dSeq1Bottom = 0;
	private double dSeq2Top = 0;
	private double dSeq2Bottom = 0;
	private double dSelectionRow1Top = 0;
	private double dSelectionMid = 0;
	private double dSelectionRow2Bottom = 0;

	private double dHeaderHeight = 0;
	private double dRulerHeight = 0;
	private double dSequenceStart = 0;
	static private final int nInsetGap = 5;
	static private final int nRowHeight = 15;
	
	private Vector<JPanel> codingPanels = new Vector<JPanel> ();  
	private Vector <String> descLines;
	private ScoreAA scoreObj = new ScoreAA();
    private static final long serialVersionUID = 1;
}
