/**
 * Displays alignment of a pair in two nucleotide or two amino acid
 */
package sng.viewer.panels;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;
import java.awt.Graphics2D;

import java.util.TreeSet;
import java.util.Vector;

import sng.database.Globals;
import sng.dataholders.CodingRegion;
import sng.dataholders.SequenceData;
import util.align.AAStatistics;
import util.align.AlignData;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.MultilineTextPanel;

public class MainPairAlignPanel extends MainAlignPanel
{
	// gets called first with the nucleotide alignment, then the AA alignment
	// Called from MainToolBarPanel.createPairAlignPanel
	
	Color background1 = new Color(0xEEFFEE);
	Color background2 = new Color(0xEEFFFF);
	
	public MainPairAlignPanel ( int num, AlignData align, boolean alt,
			int nInTopGap, int nInBottomGap, int nInLeftGap, int nInRightGap )
	{		
		super ( Globals.textFont );
		Color c;
		numAlign = num;
		
		if (alt) c = background1; 
		else c = background2;
		
		super.setBackground( c );
		super.setLayout( null );
		dSequenceStart = Math.max( super.getTextWidth( align.getDisplayStr1() ), 
									super.getTextWidth( align.getDisplayStr2() ) );
		dSequenceStart += nInsetGap * 2.0d + nInLeftGap;
		super.setMinPixelX ( dSequenceStart );
		
		boolean isSeq = super.isDrawSeqMode();
		super.setIndexRange ( align.getLowIndex(isSeq), align.getHighIndex(isSeq) );
		super.setCtgDisplay(false);

		alignData = align;
		seqData1 = alignData.getSequence1();
		seqData2 = alignData.getSequence2();
		nTopGap = nInTopGap;
		nBottomGap = nInBottomGap;
		nRightGap = nInRightGap;
		nLeftGap = nInLeftGap;		

		if (seqData1.isDNA()) isDNA=true;
		else isDNA=false;
		
		dFrameLeftX = nInLeftGap;
		dFrameTopY = nInTopGap;
		dRulerHeight = super.getTextWidth( "999999" );
	}
	
	public void refreshPanels ( ) 
	{ 
		selectionBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY,  getFrameWidth (), dFrameHeight );
		
		if (super.isShowHit())
			if (alignData.isNtAA() || alignData.isAAAA()) 
				highlightHit();
		if (super.isShowORF()) 
			if (alignData.isNtAA() && alignData.getFrame1()==alignData.getORFCoding1().getFrame())
				highlightAAORF ();
	};
	
	//---------------------------------JPanel Over-rides-------------------------------------//

	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY, 
				getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dFrameLeftX - 2, dFrameTopY - 2, 
				getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() )
		{
			// Outline whole thing
			Stroke oldStroke = g2.getStroke();
			g2.setColor( Globalx.selectColor );
			g2.setStroke( new BasicStroke (3) );
			g2.draw( highlightBox );
			g2.setStroke ( oldStroke );
			
			if ( bSeq1Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectionRow1Top, 
						getFrameRight ( ) - dFrameLeftX, dSelectionMid - dSelectionRow1Top ) );		

			if ( bSeq2Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectionMid, 
						getFrameRight ( ) - dFrameLeftX, dSelectionRow2Bottom - dSelectionMid ) );					
			
			g2.setColor( Color.BLACK );
		}	
		g2.setColor( Color.BLACK );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dFrameLeftX, dDivider1Y, getFrameRight ( ), dDivider1Y ) );
		
		super.drawRuler( g2, 0, dRulerTop, dRulerBottom ); // FIXME for amino acid alignment	
	
		super.drawText( g2, alignData.getDisplayStr1(), dFrameLeftX + nInsetGap, dSeq1Top );
		super.drawSequence ( g2, seqData1, dSeq1Top, dSeq1Bottom );
		
		super.drawText( g2, alignData.getDisplayStr2(), dFrameLeftX + nInsetGap, dSeq2Top );
		super.drawSequence ( g2, seqData2, dSeq2Top, dSeq2Bottom );
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
	
	protected boolean getOverHang ( SequenceData seq, int nPos ) 
	{ 	
		char x1 = seqData1.safeGetBaseAt( nPos );
		char x2 = seqData2.safeGetBaseAt( nPos );
		
		if (x1 == ' ' || x2 == ' ') return true; // space is no base

		return false;
	}
	
	protected boolean getIsStop ( SequenceData seq, int nPos ) // 
	{   // get changed to upper case in safeGetBaseAt

		if (isDNA) return false;
		
		if (seq == seqData1 && seqData1.safeGetBaseAt( nPos ) == '*') return true;
		if (seq	 == seqData2 && seqData2.safeGetBaseAt( nPos ) == '*') return true;

		return false;
	}
	
	protected boolean getIsNAt ( SequenceData seq, int nPos ) // ambiguous
	{   // get changed to upper case in safeGetBaseAt
		char x = 'X';
		if (isDNA) x = 'N';
		char c1 = seqData1.safeGetBaseAt( nPos );
		char c2 = seqData2.safeGetBaseAt( nPos );
		
		if (seq == seqData1 && (c1 == x || c2 == ' ')) return true;
		if (seq == seqData2 && (c2 == x || c1 == ' ')) return true;
		return false;
	}
	
	protected boolean getIsGapAt ( SequenceData seq, int nPos ) // only green on gap strand
	{ 
		if (seq == seqData1 && seqData1.safeGetBaseAt( nPos ) == Globals.gapCh) return true;
		if (seq == seqData2 && seqData2.safeGetBaseAt( nPos ) == Globals.gapCh) return true;
		return false;
	}
	
	protected boolean getIsMismatchAt ( SequenceData seq, int nPos ) 
	{ 	
		char x = 'X';
		if (isDNA) x = 'N';
		char x1 = seqData1.safeGetBaseAt( nPos );
		char x2 = seqData2.safeGetBaseAt( nPos );
		
		if (x1 == ' ' || x1 == Globals.gapCh || x1 == x) return false; // space is no base
		if (x2 == ' ' || x2 == Globals.gapCh || x2 == x) return false; // 

		return x1 != x2; // mis both strands
	}
	// whether to color substitution<0
	protected boolean getIsHighSubAt ( SequenceData seq, int nPos )         // sub both strands
	{ 
		char c1 = seqData1.safeGetBaseAt( nPos );
		char c2 = seqData2.safeGetBaseAt( nPos );
		if (isDNA) return false;
		if (c1==c2) return false;
		return AAStatistics.isHighSub( c1, c2 ); 
	}
	
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
		boolean isSeq = super.isDrawSeqMode();
		super.setIndexRange ( alignData.getLowIndex(isSeq), alignData.getHighIndex(isSeq) );
  		super.setMinPixelX ( dSequenceStart );
	}
	
	public void changeDraw()
  	{
		super.changeDraw ();
  		super.setMinPixelX ( dSequenceStart );
  	}
	// XXX
	public void setBasesPerPixel ( int nBasesPerPixel ) throws Exception
	{
		super.setBasesPerPixel( nBasesPerPixel );
		super.setMinPixelX ( dSequenceStart );
		
		if ( headerPanel != null )
			remove ( headerPanel );
		
		headerPanel = new MultilineTextPanel ( Globals.textFont, 
						alignData.getDescription(numAlign), 
						nInsetGap, (int)getFrameWidth ( ) - 2, 1); 
		headerPanel.setBackground( Color.WHITE );  
		add ( headerPanel );  
        headerPanel.setLocation( nLeftGap + 1, nTopGap + 1 );		
        dHeaderHeight = headerPanel.getHeight() + 1;
		dFrameHeight = dHeaderHeight 						// Header
					+ nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					+ nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = dFrameTopY + dHeaderHeight;
		
		dRulerTop = dDivider1Y;// + nInsetGap;
		dRulerBottom = dRulerTop + dRulerHeight;
		
		dSeq1Top =    dRulerBottom + (nInsetGap-2); 
		dSeq1Bottom = dSeq1Top + nRowHeight;
			
		dSeq2Top = 	  dSeq1Bottom + (nInsetGap+2);
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
	
	public void selectAll () { bSeq1Selected = bSeq2Selected = true; repaint (); };
	
	public void selectNone () {	bSeq1Selected = bSeq2Selected = false; repaint ();};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	// called for MultiAlignmentPaenel.alignSelectedESTs
	public Vector<String> getContigIDs ( ) 
	{		
		Vector<String> out = new Vector<String>();
		out.add(alignData.getName1());
		out.add(alignData.getName2());
		
		return out;
	}
	
	public void getSelectedContigIDs ( TreeSet<String> set ) 
	{
		if ( alignData.getName1() != null && bSeq1Selected )
			set.add( alignData.getName1() );
		if ( alignData.getName2() != null && bSeq2Selected )
			set.add( alignData.getName2() );		
	};
	
	public void setSelectedContigIDs ( TreeSet<String> set ) 
	{ 
		selectNone ();  // Clear old selections
		
		if ( alignData.getName1() != null && set.contains( alignData.getName1() ) )
			bSeq1Selected = true;
		if ( alignData.getName2() != null && set.contains( alignData.getName2() ) )
			bSeq2Selected = true;		
	};
	
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
	    				selectAll ();
            }
        }
        else if ( !e.isControlDown() && !e.isShiftDown() )
            // Click outside of the main box, clear everything unless shift or control is down 
            selectNone ();                
		
		repaint();
	}
	
	//---------------------------------Private Methods-------------------------------------//
	
	// The coords are sometimes +1
	// no hit for Nt-Nt pair, though the Hit can be NT
	private void highlightHit ( )
	{
		if (!alignData.isAAdb() && !alignData.isNtAA()) return;
		
	try {
		String alignSeq = alignData.getAlignSeq1();
		double start = (double) alignData.getHitData().getCtgStart();
		double end =  (double) alignData.getHitData().getCtgEnd();
		
		if (!alignData.isAAdb() && !alignData.isNtHit()) {
			int aFrame = Math.abs(alignData.getFrame1());
			if (aFrame==3) aFrame=0;
			
			if (start>end) {
				start = (start/3.0);
				end  =  ((end-aFrame)/3.0);
			}
			else {
				start = ((start-aFrame)/3.0);
				end  =  (end/3.0);
			}
		}
		if (start>end) {
			int aaLen = 0;
			for (int i = 0; i < alignSeq.length(); i++)
				if (alignSeq.charAt(i) != Globals.gapCh) ++aaLen;
			start = (double)aaLen-start+1.0;
			end =   (double)aaLen-end+1.0;
		}
		
		double top = dSeq1Top - nInsetGap / 2.0;
		double bottom = dSeq1Bottom + nInsetGap / 2.0;
		
		// drawing coords
		int nStart = (int) start;
		int nEnd = (int)   end;
		int gapStart = highAlignGap(alignSeq, nStart); 
		// if  ---Hit start, the gapStart is at the beginning of gaps; move to end
		while (alignSeq.charAt(gapStart)==Globals.gapCh && gapStart<alignSeq.length()) 
			gapStart++;
		
		int gapStop =  highAlignGap(alignSeq, nEnd);
		
		String tip = String.format("Hit: %d,%d  Gap adjust: %d,%d", 
				(int)start, (int)end, gapStart, gapStop);
		super.setupHitPanels(tip, gapStart, gapStop, top, bottom); // MainAlignPanel
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Showing hit region");}
	}
	
	private void highlightAAORF() {
	try {
		double top = dSeq1Top  - nInsetGap / 2.0;
		double bottom = dSeq1Bottom + nInsetGap / 2.0;
		SequenceData seq = seqData1;
		CodingRegion orf = alignData.getORFCoding1 ();
		String alignSeq = alignData.getAlignSeq1();
			
		int oFrame = Math.abs(orf.getFrame());
		int oEnd5 = orf.getBegin()-oFrame;
		int oStart3 =  orf.getEnd()-oFrame;
		
		oEnd5 =   (oEnd5/3)+1; 
		oStart3 = (oStart3/3)+1;		
		
		int gapEnd5 =  highAlignGap(alignSeq, oEnd5); 
		int gapStart3 =   highAlignGap(alignSeq, oStart3);
		
		int startAlign = seq.getLowIndex(); // already in AA coords if AA
		int endAlign = seq.getHighIndex();
		
		String tip5 = String.format("5'UTRs: NT %d  AA %d  Gapped %d,%d", 
				orf.getBegin(), oEnd5,   startAlign, gapEnd5);
		String tip3 = String.format("3'UTRs: NT %d  AA %d  Gapped %d,%d", 
				orf.getEnd(), oStart3, gapStart3, endAlign);
		
		super.setupUtrPanels(tip5, startAlign, gapEnd5, top, bottom); // MainAlignPanel
		super.setupUtrPanels(tip3, gapStart3, endAlign, top, bottom); // MainAlignPanel
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Showing UTRs region");}
	}
	
	private int highAlignGap(String alignSeq, int endCoord) {
	try {
		int gapCoord=endCoord;
		int aLen = alignSeq.length()-1;
		int aLast=aLen;
		
		while (alignSeq.charAt(aLast) == Globals.gapCh && aLast>1) // trailing gaps
			aLast--;
			
		for (int sIdx=0, aIdx=0; sIdx<endCoord && aIdx<aLast; sIdx++, aIdx++) {
			while (alignSeq.charAt(aIdx) == Globals.gapCh && aIdx<aLast) {
				gapCoord++;
				aIdx++;
			}
		}
		return gapCoord;
		}
	catch (Exception e) {ErrorReport.prtReport(e, "Extending coords across gaps");}
	return endCoord;
	}
	
	public AlignData getAlignData() {return alignData;}
	
	private SequenceData seqData1 = null;
	private SequenceData seqData2 = null;
	private AlignData alignData = null;
	private boolean isDNA = true;
	private int numAlign = 0;
	
    private MultilineTextPanel headerPanel;

	private boolean bSeq1Selected = false, bSeq2Selected = false;	
	private Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	
	// Attributes for where to draw
	private int nTopGap = 0, nBottomGap = 0, nRightGap = 0, nLeftGap = 0;
	private double dFrameLeftX = 0, dFrameTopY = 0, dFrameHeight = 0;
	private double dDivider1Y = 0, dRulerTop = 0, dRulerBottom = 0;
	
	private double dSeq1Top = 0, dSeq1Bottom = 0, dSeq2Top = 0, dSeq2Bottom = 0;
	private double dSelectionRow1Top = 0, dSelectionMid = 0, dSelectionRow2Bottom = 0;
	private double dHeaderHeight = 0, dRulerHeight = 0, dSequenceStart = 0;
	
	static private final int nInsetGap = 5, nRowHeight = 15;
	
    private static final long serialVersionUID = 1;
}
