/*
 * Drawing routines for contig and pair wise alignment
 * PairwiseAlignmentPanel and ContigPanel extend this class
 * The pairwise drawing is in here, and uses inherited methods in PairwiseAlignmentPanel
 * 	if I ever have time, I would love to make these two classes separate
*/
package sng.viewer.panels;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Iterator;

import javax.swing.*;

import sng.dataholders.SequenceData;
import util.align.AlignData;
import util.database.Globalx;

public class MainAlignPanel extends JPanel
{
	public MainAlignPanel ( Font baseFont )
	{
		theFont = baseFont;
		fontMetrics = getFontMetrics ( baseFont );
		dFontAscent = fontMetrics.getAscent();
		dFontMinHeight = fontMetrics.getAscent() + fontMetrics.getDescent() 
			+ fontMetrics.getLeading();
		dFontCellWidth = fontMetrics.stringWidth( "A" );
	}
	
	// Abstract methods (child class should over-ride)
	public void selectAll () { }
	public void selectNone () { }
	public boolean hasSelection () { return false; }
	public Vector<String> getContigIDs() { return null; } 
	public AlignData getAlignData() {return null;}
	public void getSelectedContigIDs ( TreeSet<String> set ) { }
	public void setSelectedContigIDs ( TreeSet<String> set ) { }
	public void selectMatchingSequences ( String strSeqName ) { } 
	public void addSelectedConsensusToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception {}
	public void addConsensusToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception {} 
	public void addSelectedSequencesToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception {}
	public void addAllSequencesToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception {} 
	public void addSelectedSequencesToSet ( TreeSet<String> set ){ }
	public void handleClick( MouseEvent e, Point localPoint ) { }
	public double getGraphicalDeadWidth ( ) { return 0; }
	public void refreshPanels ( ) { }
	public int getNumBuried() { return 0; }	
	public int getNumSNPs() { return 0; }	
	
	// Bases Per Pixel : the number of bases represent by each column of pixels in graphical mode
	public void setBasesPerPixel ( int n ) throws Exception { nBasesPerPixel = n; clearPanels ( ); }
	public int getBasesPerPixel ( ) { return nBasesPerPixel; }
	
	// Min Pixel X : the lowest pixel on the x-axis for drawing; corresponds to the base at Min Index
	public void setMinPixelX ( double dX ) { dMinPixelX = dX; clearPanels ( ); }
	public double getMinPixelX ( ) { return dMinPixelX; }
	
	public void setIndexRange ( int nMin, int nMax ) 
	{ 
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
	public int getDrawMode ( ) { return nDrawMode; }
	public void setDrawMode(int mode) {
		nDrawMode = mode;
		clearPanels();
	}
	public void setCtgDisplay(boolean b) { // false if called from MainPairAlignPanel
		bCtgDisplay = b;
	}	
	public void changeDraw()
  	{
  		if(nDrawMode == GRAPHICMODE) nDrawMode = TEXTMODE;
  		else nDrawMode = GRAPHICMODE;
  		clearPanels ( );
  	}	
	public boolean isDrawSeqMode () {
		if(nDrawMode == GRAPHICMODE) return false;
		else return true;
	}	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		if ( bPanelsDirty )
		{
			refreshPanels ();
			bPanelsDirty = false;
		}
		drawCodingPanels ( g2 );
	}	
	protected double getSequenceWidth ( )
	{
		if( nDrawMode == GRAPHICMODE ) return getDrawWidth();
		else return getWriteWidth();
	}	
	protected void drawSequence( Graphics2D g2, SequenceData seqData, 
								double dYTop, double dYBottom )
	{
		if( nDrawMode == GRAPHICMODE )
			drawSequenceLine( g2, seqData, dYTop, dYBottom );
		else
			writeSequenceLetters( g2, seqData, dYTop, dYBottom );
	}
	// XXX Draw the columns of bases that are currently visible 
	// the MainpairAlignPanelPanel has instant variables seqData1 and seqData2 that it uses.
	// the getIs methods are in MainPairAlignPanel and ContigAlignPanel
	protected void writeSequenceLetters ( Graphics2D g2,
					SequenceData seqData, double dYTop, double dYBottom  ) 
	{
		Rectangle clipRect = g2.getClipBounds(); 
		double dClipMinX = clipRect.getMinX(); 
		double dClipMaxX = clipRect.getMaxX();
		double dWriteBaseline = (dYBottom - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 
		g2.setColor(Color.black);
		
        int nStart = Math.max( calculateIndexFromWriteX ( dClipMinX ), nMinIndex); // seqData.getLowIndex() );
        int nEnd = Math.min ( calculateIndexFromWriteX ( dClipMaxX ) + 1, nMaxIndex); // seqData.getHighIndex() );
        
		for( int i = nStart; i <= nEnd; i++ )
		{		
			double dX = calculateWriteX ( i );
			
			Color baseColor = Color.black;
			if (bCtgDisplay) { // contig display
				if ( seqData.isBuried() )					baseColor = Globalx.mediumGray;
				else if ( getIsNAt(i) ) 						baseColor = Globalx.purple; // just in consensus
				else if ( getIsMismatchAt( seqData, i ) )	baseColor = Globalx.mismatchRed;
				else if ( getIsGapAt( seqData, i )) 			baseColor = Globalx.darkGreen;
				else if ( getIsLowQualityAt ( seqData, i ) )	baseColor = Globalx.lowQualityBlue;

			} else { // pair display				
				if ( getIsStop ( seqData, i ) )				baseColor = Color.gray;
				else if ( getIsNAt ( seqData, i ) )			baseColor = Globalx.mediumGray;  
				else if ( getIsHighSubAt ( seqData, i ))baseColor = Globalx.purple;
				else if ( getIsGapAt( seqData, i )) 			baseColor = Globalx.darkGreen;
				else if ( getIsMismatchAt( seqData, i ) )	baseColor = Globalx.mismatchRed;
			}
			char c = seqData.getOutBaseAt(i);
			drawCenteredBase ( g2, c, baseColor, dX, dWriteBaseline );
		}
	}
	protected void writeSequenceLetters ( Graphics2D g2,
			StringBuffer sb, StringBuffer sbc, double dYTop, double dYBottom  ) 
	{
		Rectangle clipRect = g2.getClipBounds(); 
		double dClipMinX = clipRect.getMinX(); 
		double dClipMaxX = clipRect.getMaxX();
		double dWriteBaseline = (dYBottom - dYTop) / 2 - dFontMinHeight / 2 + dYTop + dFontAscent; 
		g2.setColor(Color.black);
			
		int nStart = Math.max( calculateIndexFromWriteX ( dClipMinX ), nMinIndex); // seqData.getLowIndex() );
		int nEnd =   Math.min( calculateIndexFromWriteX ( dClipMaxX ) + 1, nMaxIndex); // seqData.getHighIndex() );
        
		for( int i = nStart; i <= nEnd; i++ )
		{		
			double dX = calculateWriteX ( i );
			Color baseColor = Color.black;
			drawCenteredBase ( g2, sb.charAt(i), baseColor, dX, dWriteBaseline );
		}
	}

	protected void drawSequenceLine(Graphics2D g2,
					SequenceData seqData, double dYTop, double dYBottom)
	{
		//default view, do not show clones
		drawSequenceLine(g2, seqData, dYTop, dYBottom, MainToolAlignPanel.HIDE_BURIED_EST);
	}
	
	protected void drawSequenceLine( Graphics2D g2,
					SequenceData seqData, double dYTop, double dYBottom,
					int buriedMode)
	{
		// Determine the position of the sequence line, but don't draw until after the hashes
		// draw the line and arrow heads
		double dXPosStart = calculateDrawX ( seqData.getLowIndex() );
		double dXPosEnd = calculateDrawX ( seqData.getHighIndex() );
		double dHeight = dYBottom - dYTop;
		double dYCenter = dHeight / 2.0 + dYTop;

		int RED_HASH_UP = (int)(dHeight/2) - 2;
		int RED_HASH_DOWN = RED_HASH_UP;
		int GREEN_HASH_UP = RED_HASH_DOWN - 1;
		int GREEN_HASH_DOWN = RED_HASH_DOWN - 1;
		int BLUE_HASH_UP = GREEN_HASH_UP - 2;
		int BLUE_HASH_DOWN = GREEN_HASH_DOWN - 2;	
		
		boolean bBlueStopHash, bGreenHash, bRedHash, bGreyHash, bBlueHash, bPurpleHash;
		int nCurBasesToGroup = -1;
		
		// For each letter in the sequence
		for( int i = seqData.getLowIndex(); i <= seqData.getHighIndex();)
		{
			if(seqData.isBuried() && buriedMode == MainToolAlignPanel.SHOW_BURIED_EST_LOCATION) {
				i++;
				continue;
			}		
			double dHashXPos = calculateDrawX ( i );	
			bBlueStopHash = bGreenHash = bRedHash = false;
			bBlueHash = bGreyHash = bPurpleHash = false;
			
			if ( nCurBasesToGroup <= 0 )
			{
				// We may short the first group so that all grouping is aligned between ESTs
				nCurBasesToGroup = getBasesPerPixel ();
				nCurBasesToGroup -= ( seqData.getLowIndex() - nMinIndex ) % getBasesPerPixel ();
			}
			else
			{
				// Not the first group, do the full amount
				nCurBasesToGroup = getBasesPerPixel ();
			}
			
			// Aggregate together the information for the next BASES_PER_PIXEL bases 
			// XXX That is, multiple bases are represented together
			for ( int j = 0; i <= seqData.getHighIndex() && j < nCurBasesToGroup; ++i, ++j )
			{			
				if (bCtgDisplay) {  // contig display
					if ( getIsNAt(i) )							bGreyHash = true; 
					else if ( getIsLowQualityAt ( seqData, i ))	bBlueHash = true;
					if ( getIsGapAt ( seqData, i ) )				bGreenHash = true;
					else if ( getIsMismatchAt ( seqData, i ))	bRedHash = true;
				} else { // pairwise display
					if ( getIsNAt ( seqData, i ) )				bGreyHash = true;  		// n's and x's
					else if ( getIsHighSubAt ( seqData, i ) )	bPurpleHash = true;       // aa only
					else if ( getIsGapAt ( seqData, i ) )		bGreenHash = true;
					else if ( getIsMismatchAt ( seqData, i ))	bRedHash = true;
					if ( getIsStop( seqData, i ))				bBlueStopHash = true;
				}
			}

			if ( bBlueStopHash ) // little T above sequence
			{
				drawHash ( g2, dHashXPos, dYTop, dYTop + 2, Globalx.mediumGray );	
				g2.draw( new Line2D.Double( dHashXPos - 1, dYTop, dHashXPos + 1, dYTop ) );
			}
			
			if ( bRedHash ) // larger up down - mismatch
				drawHash ( g2, dHashXPos, dYCenter - RED_HASH_UP, dYCenter + RED_HASH_DOWN, Globalx.mismatchRed );
			
			if ( bGreenHash )  // little smaller up down - gap
				drawHash ( g2, dHashXPos, dYCenter - GREEN_HASH_UP, dYCenter + GREEN_HASH_DOWN, Globalx.gapGreen );
			
			if ( bGreyHash ) // smaller up and down - N's or x's
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.mediumGray );	
			
			if ( bPurpleHash )  // little smaller up down - substitute
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.purple );
			
			if( bBlueHash ) // smaller up down - low quality
				drawHash ( g2, dHashXPos, dYCenter - BLUE_HASH_UP, dYCenter + BLUE_HASH_DOWN, Globalx.lowQualityBlue );
			
		}
		
		// Draw the line for the sequence
		if(buriedMode!=MainToolAlignPanel.HIDE_BURIED_EST && seqData.isBuried())
			g2.setColor( Globalx.purple );
		else
			g2.setColor( Color.black );
		g2.draw( new Line2D.Double( dXPosStart, dYCenter, dXPosEnd, dYCenter ) );
		
		// Put a green dot on the line anywhere there is a gap (overwrite that position of the line)
		// getDrawGreenDotAt always returns false
		for(int i = seqData.getLowIndex(); i <= seqData.getHighIndex(); ++i )
		{
			if ( getDrawGreenDotAt ( seqData, i ) )
			{
				double dHashXPos = calculateDrawX (i);
				drawDot ( g2, dHashXPos, dYCenter, Globalx.gapGreen );
			}
		}

		// Draw the arrow head
		if(seqData.isBuried() )
			g2.setColor(Color.gray);
		{
			double dArrowHeight = dHeight / 2.0 - 2;
			if ( !seqData.isReverseComplement() )	
				drawArrowHead ( g2, dXPosEnd, dYCenter, dArrowHeight, true /* right */ );
			else
				drawArrowHead ( g2, dXPosStart, dYCenter, dArrowHeight, false /* left */ );			
		
			g2.setColor(Color.black);
		}
	}
	
	//	 Draw a ruler indicating base position
	protected void drawRuler ( Graphics2D g2, double dXMin, double dYTop, double dYBottom )  
	{
		// Center the text (as much as possible in the input box)
		// 99999 represents maximum length
		TextLayout layout = new TextLayout( "99999", theFont, g2.getFontRenderContext() );		
		double dY = (dYBottom + dYTop) / 2 + layout.getBounds().getWidth() / 2;
		
		int nTickIncrement = 100;
		if ( nDrawMode != GRAPHICMODE )
			nTickIncrement = 10;
		
		int nTickStart = ( nMinIndex / nTickIncrement ) * nTickIncrement;		
		if ( nTickStart < nMinIndex )
			nTickStart += nTickIncrement;

		for ( int i = nTickStart; i < nMaxIndex; i += nTickIncrement )
		{	
			double dX;
			if ( nDrawMode == GRAPHICMODE ) dX = calculateDrawX (i);
			else 
				dX = (( calculateWriteX (i+1) + calculateWriteX (i+2) ) / 2);

			if ( dX > dXMin )
				drawVerticalText ( g2, String.valueOf(i), dX, dY );
		}
	}
	
	// defined in MainPairAlignPanel and ContigAlignPanel
	protected boolean getIsLowQualityAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsGapAt ( SequenceData seq, int nPos ) { return false;}		
	protected boolean getIsMismatchAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsJoinedByNAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsHighSubAt ( SequenceData seq, int nPos ) { return false; }
	protected boolean getIsStop ( SequenceData seq, int nPos ) { return false; } 
	protected boolean getIsNAt ( SequenceData seq, int nPos ) { return false; } 
	protected boolean getIsNAt ( int nPos ) { return false; } 
	
	protected boolean getDrawGreenDotAt ( SequenceData seq, int nPos ) { return false; }
	
	protected int getCellWidthInt ( )
	{
		return (int)dFontCellWidth;
	}
	
	protected double getDrawWidth ( )
	{
		return ( nMaxIndex - nMinIndex + 1 ) / nBasesPerPixel;
	}
	
	// The total width need for writing the bases
	protected int getWriteWidthInt ( )
	{
		return (int)getWriteWidth ();
	}

	protected double getWriteWidth ( )
	{
		return dFontCellWidth * ( nMaxIndex - nMinIndex + 1 );
	}
	
	protected double calculateX ( int nBasePos ) 
	{
  		if(nDrawMode == GRAPHICMODE) return calculateDrawX ( nBasePos );
  		else return calculateWriteX ( nBasePos );	
	}
	
	// Returns the pixel position for the left hand side of the "box" to write in
	protected double calculateWriteX ( int nBasePos ) 
	{
		return dMinPixelX + ( nBasePos - nMinIndex ) * dFontCellWidth;
	}	
	
	protected int calculateIndexFromWriteX ( double dX ) 
	{ 
		return (int)( (dX - dMinPixelX) / dFontCellWidth ) + nMinIndex; 
	}	

	protected double calculateDrawX ( int nBasePos ) 
	{ 
		return dMinPixelX + ( nBasePos - nMinIndex + 1 ) / nBasesPerPixel;
	}

	// Drawing utility methods
	protected double getTextWidth ( String str )
	{
		if ( str == null )
			return 0;
		else
			return fontMetrics.stringWidth ( str );
	}
	
	protected void drawText ( Graphics2D g2, String str, double dXLeft, 
			double dYTop, Color theColor )
	{
		if ( str == null || str.length() == 0 )
			return;
		
		g2.setColor ( theColor );
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );
		float fBaseline = (float)(dYTop + dFontAscent); 
		layout.draw( g2, (float)dXLeft, fBaseline );
	}
	
	protected void drawText ( Graphics2D g2, String str, double dX, double dY )
	{
		drawText ( g2, str, dX, dY, Color.BLACK );
	}
	
	private void drawCenteredBase ( Graphics2D g2, char chBase, Color theColor, 
			double dCellLeft, double dBaseline )
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
	
	protected void drawCodingPanels ( Graphics2D g2 )
	{
		Iterator<JPanel> iterPanel = highlightPanels.iterator();
		while ( iterPanel.hasNext() )
		{
			JPanel curPanel = iterPanel.next();
			g2.setColor( curPanel.getBackground() );
			g2.fill( curPanel.getBounds() );
		}		
	}
	
	protected void clearPanels ( )
	{
		// Remove the old panels
		bPanelsDirty = true;
		Iterator<JPanel> iter = highlightPanels.iterator();
		while ( iter.hasNext() )
			remove( (Component)iter.next() );
		highlightPanels.removeAllElements();
		super.repaint();
	}
	/************************************************************
	 * Highlighting for NtAA or NtNt
	 */
	protected void setupHitPanels(String tip, int start, int end,  double dYTop, double dYBottom ) {
		if (!bShowHit) return;
		
		double dLeft = calculateX (start+1);
		double dRight = calculateX (end+1);
		createHighlightPanel ( tip, Globalx.colorHIT, dLeft, dRight, dYTop, dYBottom );
	}
	protected void setupUtrPanels (String tip, int aStart, int aStop,  double dYTop, double dYBottom )
	{		
		if (!bShowORF) return;
		
		double dLeft = calculateX(aStart);
		double dRight = calculateX (aStop);
		
		createHighlightPanel (tip, Globalx.colorORF, dLeft, dRight, dYTop, dYBottom );
	}
	/*************************************
	 * aStart/aStop - AA coordinates adjusted for gaps
	 */
	protected void setupOrfPanels (String tip5, String tip3, int aStart, int aStop,  int seqEnd, double dYTop, double dYBottom )
	{		
		if (!bShowORF) return;
		
		double dLeft = calculateX(1);
		double dRight = calculateX (aStart+1);
		
		createHighlightPanel (tip5, Globalx.colorORF, dLeft, dRight, dYTop, dYBottom );
	
		dLeft = calculateX (aStop+1);
		dRight = calculateX (seqEnd+1);
				
		createHighlightPanel (tip3, Globalx.colorORF, dLeft, dRight, dYTop, dYBottom );
	}
	private void createHighlightPanel ( String toolTip, Color theColor,
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
		if (str == null || str.length() == 0)
			return;
		
		TextLayout layout = new TextLayout( str, theFont, g2.getFontRenderContext() );

		// Draw
		g2.rotate ( - Math.PI / 2.0 );
		g2.setColor ( Color.BLACK );
		float fHalf = (float)(layout.getBounds().getHeight() / 2.0f);
		layout.draw( g2, (float)-dY, (float)dX + fHalf );	
		g2.rotate ( Math.PI / 2.0 );
	}	
	
	protected void drawSquareBracket ( Graphics2D g2, double dHashXPos, double dHashTopY, double dHashBottomY, Color hashColor, boolean bRightwise )
	{
		g2.setColor( hashColor );
		g2.draw( new Line2D.Double( dHashXPos, dHashTopY, dHashXPos, dHashBottomY ) );
		double dXStart = dHashXPos;
		double dXEnd = dHashXPos;
		if ( bRightwise ) 	dXEnd += 2;
		else 				dXEnd -= 2;
		
		g2.draw( new Line2D.Double( dXStart, dHashTopY, dXEnd, dHashTopY ) );
		g2.draw( new Line2D.Double( dXStart, dHashBottomY, dXEnd, dHashBottomY ) );
	}
	
	FontMetrics fontMetrics = null;
	private Font theFont = null;
	
	// Attibutes for positioning
	private double dMinPixelX = 0;		// The lowest pixel (associated base at min index) 
	private int nMinIndex = 1;	
	private int nMaxIndex = 1;
	private double dFontMinHeight = 0;  // The minimum sized box a letter will fit in based on our font
	private double dFontCellWidth = 0;	
	private double dFontAscent = 0;		// The distance from the top of the write cell to the baseline
	private int nBasesPerPixel = 3;			

	private int nDrawMode = GRAPHICMODE;
	private boolean bCtgDisplay = true;
	
	// Panels to show the coding region
	private boolean bPanelsDirty = false;
	private Vector<JPanel> highlightPanels = new Vector<JPanel> ();      
    private boolean bShowHit = false;
    private boolean bShowORF = false;

    private static final long serialVersionUID = 1;
}
