package cmp.viewer.seq.align;

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

import util.database.Globalx;
import util.ui.MultilineTextPanel;
import cmp.align.ScoreAA;
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
		
		// set start and stops
		alignSeq1 = theAlignment.getAlignFullSeq1(); 
		for(nStart1=0; nStart1 < alignSeq1.length() && alignSeq1.charAt(nStart1) == gapCh; nStart1++);
		for(nStop1=alignSeq1.length() - 1; nStop1 >= 0 && alignSeq1.charAt(nStop1) == gapCh; nStop1--);
		
		alignSeq2 = theAlignment.getAlignFullSeq2(); 
		for(nStart2=0; nStart2 < alignSeq2.length() && alignSeq2.charAt(nStart2) == gapCh; nStart2++);
		for(nStop2=alignSeq2.length() - 1; nStop2 >= 0 && alignSeq2.charAt(nStop2) == gapCh; nStop2--);
		seq1Label = theAlignment.getSeqID1();
		seq2Label = theAlignment.getSeqID2();
//		description = theAlignment.getDescription();
		
		dSequenceStart = Math.max( super.getTextWidth( seq1Label ), super.getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nGAP;
		super.setMinPixelX ( dSequenceStart );
		super.setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );

		dRulerHeight = super.getTextWidth( "999999" );
	}
	
	//---------------------------------JPanel Over-rides-------------------------------------//

	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dPAD - 2, dPAD - 2, getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() )
		{
			// Outline whole thing
			Stroke oldStroke = g2.getStroke();
			g2.setColor( selectColor );
			g2.setStroke( new BasicStroke (3) );
			g2.draw( highlightBox );
			g2.setStroke ( oldStroke );
			
			if ( bSeq1Selected )
				g2.fill( new Rectangle2D.Double( dPAD, dSelectionRow1Top, getFrameRight ( ) - dPAD, dSelectionMid - dSelectionRow1Top ) );		

			if ( bSeq2Selected )
				g2.fill( new Rectangle2D.Double( dPAD, dSelectionMid, getFrameRight ( ) - dPAD, dSelectionRow2Bottom - dSelectionMid ) );					
			
			g2.setColor( Color.BLACK );
		}
			
		g2.setColor( borderColor );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dPAD, dDivider1Y, getFrameRight ( ), dDivider1Y ) );
		
		super.drawRuler( g2, 0, dRulerTop, dRulerBottom );
		
		boolean isFirst=true;
		super.drawName( g2, seq1Label, dPAD + nInsetGap, dSeq1Top );
		drawSequence  ( g2, seq1Label, alignSeq1, dSeq1Top, dSeq1Bottom , isFirst, isDNA, isPair);
		
		isFirst=false;
		super.drawName( g2, seq2Label, dPAD + nInsetGap, dSeq2Top );
		drawSequence ( g2, seq2Label, alignSeq2, dSeq2Top, dSeq2Bottom, isFirst, isDNA , isPair);
	}
	
	public Dimension getMaximumSize()
	{
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize()
	{
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
		if( bFirst && (nPos < nStart2 || nPos > nStop2)) return true;
		if(!bFirst && (nPos < nStart1 || nPos > nStop1)) return true;
		return false;
	}
	
	// necessary for selection
	public void refreshPanels ( ) { 
		selectionBox = new Rectangle2D.Double ( dPAD, dPAD,  getFrameWidth (), dFrameHeight );
	};
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
  		super.setMinPixelX ( dSequenceStart );
	}
	
	public void setZoom ( int nBasesPerPixel ) throws Exception
	{
		super.setZoom( nBasesPerPixel );
		super.setMinPixelX ( dSequenceStart );

		dFrameHeight = nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = 		dPAD +  dHeaderHeight;
		dRulerTop = 		dDivider1Y +  nInsetGap;
		dRulerBottom = 		dRulerTop +   dRulerHeight;
		dSeq1Top = 			dRulerBottom + nInsetGap;
		dSeq1Bottom = 		dSeq1Top +     nRowHeight;
		dSeq2Top = 			dSeq1Bottom + nInsetGap;
		dSeq2Bottom = 		dSeq2Top + nRowHeight;
		dSelectionRow1Top = dSeq1Top - nInsetGap / 2.0;
		dSelectionMid = 	dSeq1Bottom + nInsetGap / 2.0;
		dSelectionRow2Bottom = dSeq2Bottom + nInsetGap;
	}
	
	public double getGraphicalDeadWidth ( )
	{
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSequenceStart + nInsetGap;
	}
	public void selectAll () { bSeq1Selected = bSeq2Selected = true; repaint (); };
	
	public void selectNone () {	bSeq1Selected = bSeq2Selected = false; repaint ();};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	//-------------------------- private -----------------------------/
	private double getFrameWidth ( )
	{
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	
	private double getFrameRight ( )
	{
		return dPAD + getFrameWidth ( );
	}
	
	//-------------------------- AlignPairViewNPanel -----------------------------/
	public void setBorderColor(Color newColor) {
		borderColor = newColor;
	}
	
	public Vector<String> getSelectedSeqIDs () 
	{
		Vector<String> retVal = new Vector<String> ();
		if ( seq1Label != null && bSeq1Selected ) retVal.add( seq1Label );
		if ( seq2Label != null && bSeq2Selected ) retVal.add( seq2Label );
		
		return retVal;
	};
	
	public boolean hasSeqIDs(String [] theIDs, boolean mustHaveBoth) {
		boolean firstInc = false, secondInc = false;
		
		for(int x=0; x<theIDs.length && (!firstInc || !secondInc); x++) {
			firstInc = firstInc || seq1Label.equals(theIDs[x]);
			secondInc = secondInc || seq2Label.equals(theIDs[x]);
		}
		
		if(mustHaveBoth)
			return firstInc && secondInc;
		return firstInc || secondInc;
	}
	
	public void handleClick( MouseEvent e, Point localPoint )
	{
        if ( selectionBox.contains(localPoint) )
        {       	
            // Determine the row for the click.
    		if ( dSelectionRow1Top <= localPoint.y && localPoint.y <= dSelectionRow2Bottom )
    		{
    			// Clear all selected rows if neither shift nor control are down
    			if ( !e.isControlDown() && !e.isShiftDown() )
    				selectNone();
    			
    			// determine the newly selected sequence and toggle it
    			if ( dSelectionRow1Top <= localPoint.y && localPoint.y <= dSelectionMid )
    				bSeq1Selected = !bSeq1Selected;
    			else if ( dSelectionMid <= localPoint.y && localPoint.y <= dSelectionRow2Bottom )
    				bSeq2Selected = !bSeq2Selected;
    		}
    		else
    		{	
                // Click is inside the box, but not within any of the sequences (i.e. in the header)
    			if ( ( e.isControlDown() || e.isShiftDown() ) && hasSelection () )
    				selectNone ();
    			else
    				// Select whole contig
    				selectAll ();
            }
        }
        else if ( !e.isControlDown() && !e.isShiftDown() )
            // Click outside of the main box, clear everything unless shift or control is down 
            selectNone ();                
		
		repaint();
	}
	public PairAlignData getAlignment() { return alignDataObj;}
	
	/****************************************************************************/
	private PairAlignData alignDataObj=null;
	private String alignSeq1 = "";
	private int nStart1 = 0;
	private int nStop1 = 0;
	private String alignSeq2 = "";
	private int nStart2 = 0;
	private int nStop2 = 0;
	private String seq1Label = "";
	private String seq2Label = "";
	private boolean isDNA = true;
//	private Vector<String> description = null;
	
    MultilineTextPanel headerPanel;

	boolean bSeq1Selected = false;
	boolean bSeq2Selected = false;	
	Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	
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
	static private final int nGAP=10;
	static private final double dPAD=10.0;
	
    private static final long serialVersionUID = 1;
}
