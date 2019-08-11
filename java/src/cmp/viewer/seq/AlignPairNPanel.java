package cmp.viewer.seq;

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
import java.util.TreeSet;
import java.util.Vector;

import util.ui.MultilineTextPanel;
import cmp.align.ScoreAA;
import cmp.align.PairAlignData;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class AlignPairNPanel extends AlignBasePanel
{
	private static final char gapCh=Globals.gapCh;
	// gets called first with the nucleotide alignment, then the AA alignment
	public AlignPairNPanel ( MTCWFrame parentFrame, PairAlignData theAlignment, //int frame, 
			int nInTopGap, int nInBottomGap, int nInLeftGap, int nInRightGap, boolean isNT)
	{		
		// Setup the base class
		super ( parentFrame.getSettings().getFrameSettings().getDefaultFont() );
		super.setBackground( parentFrame.getSettings().getFrameSettings().getBGColor() );
		super.setLayout( null );

		isDNA=isNT;
		
		alignDataObj = theAlignment;
		sequence1 = theAlignment.getAlignFullSeq1(); 
		for(nSeq1Start=0; 
				nSeq1Start < sequence1.length() && sequence1.charAt(nSeq1Start) == gapCh; 
				nSeq1Start++);
		for(nSeq1Stop=sequence1.length() - 1; 
				nSeq1Stop >= 0 && sequence1.charAt(nSeq1Stop) == gapCh; 
				nSeq1Stop--);
		sequence2 = theAlignment.getAlignFullSeq2(); 
		for(nSeq2Start=0; 
				nSeq2Start < sequence2.length() && sequence2.charAt(nSeq2Start) == gapCh; 
				nSeq2Start++);
		for(nSeq2Stop=sequence2.length() - 1; 
				nSeq2Stop >= 0 && sequence2.charAt(nSeq2Stop) == gapCh; 
				nSeq2Stop--);
		seq1Label = theAlignment.getSeqID1();
		seq2Label = theAlignment.getSeqID2();
//		description = theAlignment.getDescription();
		
		dSequenceStart = Math.max( super.getTextWidth( seq1Label ), 
									super.getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nInLeftGap;
		super.setMinPixelX ( dSequenceStart );
		super.setIndexRange ( 0, Math.max(sequence1.length(), sequence2.length()) );
		super.setCtgDisplay(false);

		nTopGap = nInTopGap;
		nBottomGap = nInBottomGap;
		nRightGap = nInRightGap;
		nLeftGap = nInLeftGap;	
		
		// Calculate drawing positions
		dFrameLeftX = nInLeftGap;
		dFrameTopY = nInTopGap;
		dRulerHeight = super.getTextWidth( "999999" );
	}
	
	public void refreshPanels ( ) 
	{ 
		selectionBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY,  getFrameWidth (), dFrameHeight );
	};
	
	//---------------------------------JPanel Over-rides-------------------------------------//

	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY, getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dFrameLeftX - 2, dFrameTopY - 2, getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() )
		{
			// Outline whole thing
			Stroke oldStroke = g2.getStroke();
			g2.setColor( selectColor );
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
		
		super.drawRuler( g2, 0, dRulerTop, dRulerBottom );
		
		super.drawText( g2, seq1Label, dFrameLeftX + nInsetGap, dSeq1Top );
		drawSequence ( g2, seq1Label, sequence1, dSeq1Top, dSeq1Bottom );
		
		super.drawText( g2, seq2Label, dFrameLeftX + nInsetGap, dSeq2Top );
		drawSequence ( g2, seq2Label, sequence2, dSeq2Top, dSeq2Bottom );
	}
	
	public Dimension getMaximumSize()
	{
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize()
	{
		int nWidth = nLeftGap + (int)getFrameWidth ( ) + nRightGap;
		int nHeight = nTopGap + (int)dFrameHeight + nBottomGap;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//---------------------------------AlignmentPanelBase Over-rides-------------------------------------//
	
	protected boolean getIsStop ( String seq, int nPos ) // 
	{   // they get changed to upper case in safeGetBaseAt

		if (isDNA) return false;
		
		if (seq.equals(sequence1) && sequence1.charAt( nPos ) == '*') return true;
		if (seq.equals(sequence2) && sequence2.charAt( nPos ) == '*') return true;

		return false;
	}
	
	protected boolean getIsNAt ( String seq, int nPos ) // ambiguous
	{   // they get changed to upper case in safeGetBaseAt
		char x = 'X';
		if (isDNA) x = 'N';
		char c1 = sequence1.charAt( nPos );
		char c2 = sequence2.charAt( nPos );
		
		if (seq.equals(sequence1) && (c1 == x || c2 == ' ')) return true;
		if (seq.equals(sequence2) && (c2 == x || c1 == ' ')) return true;
		return false;
	}
	
	protected boolean getIsEndGapAt( String seq, int nPos) {
		if(seq.equals(sequence1) && (nPos < nSeq2Start || nPos > nSeq2Stop)) return true;
		if(seq.equals(sequence2) && (nPos < nSeq1Start || nPos > nSeq1Stop)) return true;
		return false;
	}
	protected boolean getIsGapAt ( String seq, int nPos ) // only green on gap strand
	{ 
		if (seq.equals(sequence1) && sequence1.charAt( nPos ) == Globals.gapCh) return true;
		if (seq.equals(sequence2) && sequence2.charAt( nPos ) == Globals.gapCh) return true;
		return false;
	}
	
	protected boolean getIsMismatchAt ( String seq, int nPos ) 
	{ 	
		char x = 'X';
		if (isDNA) x = 'N';
		char x1 = sequence1.charAt( nPos );
		char x2 = sequence2.charAt( nPos );
		
		if (x1 == ' ' || x1 == Globals.gapCh || x1 == x) return false; // space is no base
		if (x2 == ' ' || x2 == Globals.gapCh || x2 == x) return false; // 

		return x1 != x2; // mis both strands
	}
	
	protected boolean getIsHighSubAt ( String seq, int nPos )         // sub both strands
	{ 
		if (isDNA) return false;
		char a1 = sequence1.charAt( nPos );
		char a2 = sequence2.charAt( nPos );
		if (a1==a2) return false;
		return scoreObj.isHighSub(a1, a2);
	}
	
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
  		super.setMinPixelX ( dSequenceStart );
	}
	
	public void changeDraw()
  	{
		super.changeDraw ();
  		super.setMinPixelX ( dSequenceStart );
  	}
	
	public void setBasesPerPixel ( int nBasesPerPixel ) throws Exception
	{
		super.setBasesPerPixel( nBasesPerPixel );
		super.setMinPixelX ( dSequenceStart );

		dFrameHeight = nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = dFrameTopY + dHeaderHeight;
		dRulerTop = dDivider1Y + nInsetGap;
		dRulerBottom = dRulerTop + dRulerHeight;
		dSeq1Top = dRulerBottom + nInsetGap;
		dSeq1Bottom = dSeq1Top + nRowHeight;
		dSeq2Top = dSeq1Bottom + nInsetGap;
		dSeq2Bottom = dSeq2Top + nRowHeight;
		dSelectionRow1Top = dSeq1Top - nInsetGap / 2.0;
		dSelectionMid = dSeq1Bottom + nInsetGap / 2.0;
		dSelectionRow2Bottom = dSeq2Bottom + nInsetGap;
	}
	
	public double getGraphicalDeadWidth ( )
	{
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSequenceStart + nInsetGap;
	}
	
	private double getFrameWidth ( )
	{
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	
	protected double getFrameLeft ( )
	{
		return dFrameLeftX;
	}
	
	protected double getFrameRight ( )
	{
		return dFrameLeftX + getFrameWidth ( );
	}
	
	public void setBorderColor(Color newColor) {
		borderColor = newColor;
	}
	
	public void selectAll () { bSeq1Selected = bSeq2Selected = true; repaint (); };
	
	public void selectNone () {	bSeq1Selected = bSeq2Selected = false; repaint ();};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	public Vector<String> getContigIDs ( ) 
	{		
		Vector<String> out = new Vector<String>();
		out.add(seq1Label);
		out.add(seq2Label);
		
		return out;
	}
	
	public String getSelectedSequence() {
		if(bSeq1Selected)
			return sequence1;
		if(bSeq2Selected)
			return sequence2;
		return null;
	}
	
	public Vector<String> getSelectedContigIDs () 
	{
		Vector<String> retVal = new Vector<String> ();
		if ( seq1Label != null && bSeq1Selected )
			retVal.add( seq1Label );
		if ( seq2Label != null && bSeq2Selected )
			retVal.add( seq2Label );
		
		return retVal;
	};
	
	public void setSelectedContigIDs ( TreeSet<String> set ) 
	{ 
		selectNone ();  // Clear old selections
		
		if ( seq1Label != null && set.contains( seq1Label ) )
			bSeq1Selected = true;
		if ( seq2Label != null && set.contains( seq2Label ) )
			bSeq2Selected = true;		
	}
	
	public boolean hasContigIDs(String [] theIDs, boolean mustHaveBoth) {
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
	
	private PairAlignData alignDataObj=null;
	private String sequence1 = "";
	private int nSeq1Start = 0;
	private int nSeq1Stop = 0;
	private String sequence2 = "";
	private int nSeq2Start = 0;
	private int nSeq2Stop = 0;
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
	private int nTopGap = 0;
	private int nBottomGap = 0;
	private int nRightGap = 0;
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
	private ScoreAA scoreObj = new ScoreAA();
	
	static private final int nInsetGap = 5;
	static private final int nRowHeight = 15;
	
    private static final long serialVersionUID = 1;
}
