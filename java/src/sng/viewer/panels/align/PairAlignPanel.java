package sng.viewer.panels.align;

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
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.MultilineTextPanel;

/**
 * Displays alignment of a pair in two nucleotide or two amino acid
 * CAS314 removed all references to SequenceData class; use renamed PairBasePanel with Trim
 */
public class PairAlignPanel extends PairBasePanel {
	private final Color background1 = new Color(0xEEFFEE);
	private final Color background2 = new Color(0xEEFFFF);
	private final char gapCh = Globalx.gapCh;
	
	public PairAlignPanel ( int num, AlignData alignObj, boolean alt, boolean isHit,
			int nInTopGap, int nInBottomGap, int nInLeftGap, int nInRightGap )
	{		
		super ( Globals.textFont );
		Color c;
		numAlign = num;
		this.isHit = isHit;
		
		alignData = alignObj;
		nTopGap = 		nInTopGap;
		nLowGap = 		nInBottomGap;
		nRightGap = 	nInRightGap;
		nLeftGap = 		nInLeftGap;
		dFrameLeftX = 	(double) nInLeftGap;
		dFrameTopY = 	(double) nInTopGap;
		dRulerHeight = super.getTextWidth( "999999" );
		
		if (alt) c = background1; 
		else c = background2;
		
		super.setBackground( c );
		super.setLayout( null );
		
		init();
	}
	
	public void refreshPanels ( )  { 
		selectionBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY,  getFrameWidth (), dFrameHeight );
		
		if (super.isShowHit())
			if (isHit) 
				highlightHit();
		if (super.isShowORF()) 
			if (alignData.isNTsTCW()) {
				if (alignData.getFrame1()==alignData.getORFCoding1().getFrame())
						highlightAAORF ();
			}
	};
	
	//---------------------------------JPanel Over-rides-------------------------------------//

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY, 
				getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dFrameLeftX - 2, dFrameTopY - 2, 
				getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() )// Outline whole thing
		{	
			Stroke oldStroke = g2.getStroke();
			g2.setColor( Globalx.selectColor );
			g2.setStroke( new BasicStroke (3) );
			g2.draw( highlightBox );
			g2.setStroke ( oldStroke );
			
			if ( bSeq1Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectRow1Top, 
						getFrameRight ( ) - dFrameLeftX, dSelectMid - dSelectRow1Top ) );		

			if ( bSeq2Selected )
				g2.fill( new Rectangle2D.Double( dFrameLeftX, dSelectMid, 
						getFrameRight ( ) - dFrameLeftX, dSelectRow2Low - dSelectMid ) );					
			
			g2.setColor( Color.BLACK );
		}	
		g2.setColor( Color.BLACK );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dFrameLeftX, dDivider1Y, getFrameRight ( ), dDivider1Y ) );
		
		super.drawRuler(g2, 0, dRulerTop, dRulerLow); 	
	
		boolean isFirst=true;
		super.drawText(g2, alignData.getDisplayStr1(), dFrameLeftX + nInsetGap, dSeq1Top );
		super.drawSequence (g2, alignSeq1, dSeq1Top, dSeq1Low, nTrimStart1, nTrimStop1, isFp1, isFirst, isDNA );
		
		isFirst=false;
		super.drawText(g2, alignData.getDisplayStr2(), dFrameLeftX + nInsetGap, dSeq2Top );
		super.drawSequence (g2, alignSeq2, dSeq2Top, dSeq2Low, nTrimStart2, nTrimStop2, isFp2, isFirst, isDNA );
	}
	
	public Dimension getMaximumSize() {
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize() {
		int nWidth = nLeftGap + (int)getFrameWidth ( ) + nRightGap;
		int nHeight = nTopGap + (int)dFrameHeight + nLowGap;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//------------------ PairBasePanel Over-rides-------------------------------------//
	protected char getChar(boolean bFirst, int nPos) { 
		if (bFirst) return alignSeq2.charAt( nPos );
		else        return alignSeq1.charAt( nPos );
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if( bFirst && (nPos < nTrimStart2 || nPos > nTrimStop2)) return true;
		if(!bFirst && (nPos < nTrimStart1 || nPos > nTrimStop1)) return true;
		return false;
	}
	
	public double getGraphicalDeadWidth ( ) {
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSeqStart + nInsetGap;
	}
	private double getFrameWidth ( ) {
		double dWidth = dSeqStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	protected double getFrameLeft ( ){
		return dFrameLeftX;
	}	
	protected double getFrameRight ( ){
		return dFrameLeftX + getFrameWidth ( );
	}
	
	public void selectAll () { bSeq1Selected = bSeq2Selected = true; repaint (); };
	
	public void selectNone () {	bSeq1Selected = bSeq2Selected = false; repaint ();};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	public Vector<String> getSeqIDs ( ) {		
		Vector<String> out = new Vector<String>();
		out.add(alignData.getName1());
		out.add(alignData.getName2());
		
		return out;
	}
	public void getSelectedSeqIDs ( TreeSet<String> set ) {
		if ( alignData.getName1() != null && bSeq1Selected ) 
			set.add( alignData.getName2() ); // CAS313 was getName1, but selects the 1st alignment always
		if ( alignData.getName2() != null && bSeq2Selected ) set.add( alignData.getName2() );		
	};
	
	public void handleClick( MouseEvent e, Point localPoint ){
        if ( selectionBox.contains(localPoint) )
        {       	
            // Determine the row for the click.
    		if ( dSelectRow1Top <= localPoint.y && localPoint.y <= dSelectRow2Low ) {
    			// Clear all selected rows if neither shift nor control are down
    			if ( !e.isControlDown() && !e.isShiftDown() )
    				selectNone();
    			
    			// determine the newly selected sequence and toggle it
    			if ( dSelectRow1Top <= localPoint.y && localPoint.y <= dSelectMid )
    				bSeq1Selected = !bSeq1Selected;
    			else if ( dSelectMid <= localPoint.y && localPoint.y <= dSelectRow2Low )
    				bSeq2Selected = !bSeq2Selected;
    		}
    		else {	
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
	
	private void highlightHit ( ) {
		if (alignData.getHitData()==null) return; 
		
		try {
			String alignSeq = alignData.getAlignSeq1();
			double start = (double) alignData.getHitData().getCtgStart();
			double end =   (double) alignData.getHitData().getCtgEnd();
			
			if (alignData.isNTsTCW() && !alignData.isNTalign()) {
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
			double low = dSeq1Low + nInsetGap / 2.0;
			
			// drawing coords
			int nStart = (int) start;
			int nEnd   = (int)   end;
			int gapStart = addGaps(alignSeq, nStart); // beginning of hit
			while (alignSeq.charAt(gapStart)==Globals.gapCh && gapStart<alignSeq.length()) // skip any leading gaps
				gapStart++;
			
			int gapStop =  addGaps(alignSeq, nEnd); 
			
			String tip = String.format("Seq Coords of Hit: %d-%d  Gap adjust: %d-%d", 
							(int)start, (int)end, gapStart, gapStop);
			
			super.setupHitPanels(tip, gapStart, gapStop, top, low); 
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Showing hit region");}
	}
	
	private void highlightAAORF() {
	try {
		double top = dSeq1Top  - nInsetGap / 2.0;
		double bottom = dSeq1Low + nInsetGap / 2.0;
		
		CodingRegion orf = alignData.getORFCoding1 ();
		String alignSeq  = alignData.getAlignSeq1();
			
		int oFrame = 	Math.abs(orf.getFrame());
		int oEnd5 = 	orf.getBegin()-oFrame;
		int oStart3 =  	orf.getEnd()-oFrame;
		
		oEnd5 =   (oEnd5/3)+1; 
		oStart3 = (oStart3/3)+1;		
		
		int gapEnd5 =  	  addGaps(alignSeq, oEnd5); 
		int gapStart3 =   addGaps(alignSeq, oStart3);
		
		int startAlign = (super.isTrim()) ? nTrimStart1 : 0; // already in AA coords if AA
		int endAlign =   alignSeq1.length();
		
		String tip5 = String.format("5'UTRs Gapped Coords %d-%d", startAlign,gapEnd5);
		String tip3 = String.format("3'UTRs Gapped Coords %d-%d", gapStart3, endAlign);
	
		super.setupUtrPanels(tip5, startAlign, gapEnd5, top, bottom); 
		super.setupUtrPanels(tip3, gapStart3, endAlign, top, bottom); 
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Showing UTRs region");}
	}
	// aaaaaaa--aaaaaaaaa
	// ---aaaabb
	private int addGaps(String alignSeq, int endCoord) { 
	try {
		int gapCoord=endCoord;
		int aLen = alignSeq.length()-1;
		int aLast=aLen;
		
		while (alignSeq.charAt(aLast) == Globals.gapCh && aLast>1) aLast--;// trailing gaps
			
		for (int sIdx=0, aIdx=0; sIdx<endCoord && aIdx<aLast; sIdx++, aIdx++) {
			
			while (alignSeq.charAt(aIdx) == Globals.gapCh && aIdx<aLast) {
				gapCoord++;
				aIdx++;
			}
		}
		return gapCoord;
	}
	catch (Exception e) {Out.prt("TCW error");ErrorReport.prtReport(e, "Extending coords across gaps");}
	return endCoord;
	}
	
	public AlignData getAlignData() {return alignData;}
	
	private void init() {
	try {
		dSeqStart = Math.max( super.getTextWidth( alignData.getDisplayStr1() ), 
				  super.getTextWidth( alignData.getDisplayStr2() ) );
		dSeqStart += nInsetGap * 2.0d + nLeftGap;
		super.setMinPixelX ( dSeqStart );
		
		int x=0, aLen = alignData.getAlignSeq1().length();
		
		alignSeq1 = alignData.getAlignSeq1(); 
		for (x=0; x < aLen && alignSeq1.charAt(x)==gapCh; x++);
		nTrimStart1=x;
		
		for(x=aLen-1; x>=0 && alignSeq1.charAt(x)==gapCh; x--);
		nTrimStop1=x;
		
		if (alignData.getFrame1()<0) isFp1=false;
		
		alignSeq2 = alignData.getAlignSeq2(); 
		for (x=0; x < aLen && alignSeq2.charAt(x)==gapCh; x++);
		nTrimStart2=x;
		
		for(x=aLen-1; x>=0 && alignSeq2.charAt(x)==gapCh; x--);
		nTrimStop2=x;
		
		if (alignData.getFrame2()<0) isFp2=false;
		
		super.setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );
		
		super.setTrimRange(Math.max(nTrimStart1, nTrimStart2), Math.min(nTrimStop1, nTrimStop2)); // CAS313 add	
		
		if (alignData.getSeqData1().isNT()) isDNA=true;
		else isDNA=false;
		
		// was in setZoom
		if ( headerPanel != null ) remove ( headerPanel );
		
		headerPanel = new MultilineTextPanel ( Globals.textFont, 
				alignData.getDescription(numAlign), nInsetGap, (int)getFrameWidth ( ) - 2, 1); 
		headerPanel.setBackground( Color.WHITE );  
		add ( headerPanel );  
        headerPanel.setLocation( nLeftGap + 1, nTopGap + 1 );	
        
        dHeaderHeight = headerPanel.getHeight() + 1;
		dFrameHeight = dHeaderHeight 						// Header
					+ nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					+ nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = dFrameTopY + dHeaderHeight;
		
		dRulerTop = dDivider1Y;
		dRulerLow = dRulerTop + dRulerHeight;
		
		dSeq1Top =    dRulerLow + (nInsetGap-2); 
		dSeq1Low = dSeq1Top + nRowHeight;
			
		dSeq2Top = 	  dSeq1Low + (nInsetGap+2);
		dSeq2Low = dSeq2Top + nRowHeight;
		
		dSelectRow1Top = dSeq1Top - nInsetGap / 2.0;
		dSelectMid = dSeq1Low + nInsetGap / 2.0;
		dSelectRow2Low = dSeq2Low + nInsetGap;
	}
	catch(Exception e) {ErrorReport.prtReport(e, "init PairAlign");}
	}
	/************************************************************************/
	private boolean isFp1=true, isFp2=true;
	private int nTrimStart1=0, nTrimStart2=0, nTrimStop1=0, nTrimStop2=0;
	private String alignSeq1 = null, alignSeq2 = null; // has leading and internal gaps
	private AlignData alignData = null;
	private boolean isDNA = true, isHit=true;
	private int numAlign = 0;
	
    private MultilineTextPanel headerPanel;

	private boolean bSeq1Selected = false, bSeq2Selected = false;	
	private Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	
	// Attributes for where to draw
	private int nTopGap = 0, nLowGap = 0, nRightGap = 0, nLeftGap = 0;
	private double dFrameLeftX = 0, dFrameTopY = 0, dFrameHeight = 0;
	private double dDivider1Y = 0, dRulerTop = 0, dRulerLow = 0;
	
	private double dSeq1Top = 0, dSeq1Low = 0, dSeq2Top = 0, dSeq2Low = 0;
	private double dSelectRow1Top = 0, dSelectMid = 0, dSelectRow2Low = 0;
	private double dHeaderHeight = 0, dRulerHeight = 0, dSeqStart = 0;
	
	static private final int nInsetGap = 5, nRowHeight = 15;
	
    private static final long serialVersionUID = 1;
}
