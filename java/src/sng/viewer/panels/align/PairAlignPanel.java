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
 * CAS332 made multiple alignments line up the same. Moved all final variables to here.
 */
public class PairAlignPanel extends PairBasePanel {
	private final Color background1 = new Color(0xEEFFEE);
	private final Color background2 = new Color(0xEEFFFF);
	private final char gapCh = Globalx.gapCh;
	
	public PairAlignPanel ( int num, AlignData alignObj, boolean alt, boolean bHit, String maxStr )
	{		
		super ( Globals.textFont );
		dRulerHeight =  super.getTextWidth( "999999" );
		
		numAlign = num;
		isHit = bHit;
		alignData = alignObj;
		
		Color c;
		if (alt) c = background1; 
		else c = background2;
		
		super.setBackground( c );
		super.setLayout( null );
		
		init(maxStr);
	}
	
	public void refreshPanels ( )  { 
		selectionBox = new Rectangle2D.Double ( dFrameLeftX, dFrameTopY,  getFrameWidth (), dFrameHeight );
		
		if (super.isShowHit())
			if (isHit) 
				highlightHit();
		if (super.isShowORF()) 
			if (alignData.isNTsTCW()) {
				if (alignData.getFrame1()==alignData.getORFCoding1().getFrame())
						highlightUTR ();
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
		
		if ( hasSelection() ) {// Outline whole thing
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
		
		super.drawRuler(g2, dRulerTop, dRulerLow); 	
	
		// XXX Uses nTrimStart1,nTrimStop1 to avoid printing leading/trailing gaps
		boolean isFirst=true;
		super.drawName(g2, alignData.getDisplayStr1(), dFrameLeftX + fInsetGap, dSeq1Top );
		super.drawSequence (g2, alignSeq1, dSeq1Top, dSeq1Low, nTrimStart1, nTrimStop1, isFp1, isFirst, isDNA );
		
		isFirst=false;
		super.drawName(g2, alignData.getDisplayStr2(), dFrameLeftX + fInsetGap, dSeq2Top );
		super.drawSequence (g2, alignSeq2, dSeq2Top, dSeq2Low, nTrimStart2, nTrimStop2, isFp2, isFirst, isDNA );
	}
	
	public Dimension getMaximumSize() {
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize() {
		int nWidth = fLeftGap + (int)getFrameWidth ( ) + fRightGap;
		int nHeight = fTopGap + (int)dFrameHeight + fLowGap;
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
	private double getFrameWidth ( ) {
		double dWidth = dSeqStart + getSequenceWidth ( ) + fInsetGap;
		return Math.max ( fMaxWidth, dWidth );
	}
	//protected double getFrameLeft ( ){return dFrameLeftX;}	
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
	/************************************************************
	* Search coordinates:
	* 		starts at 1
	* 		do not include gaps
	* 		are in nt coords - 
	* 			start - base before first codon
	* 			end  -  base of last codon
	*************************************************************/
	
	private void highlightHit( ) {
		if (alignData.getHitData()==null) return; 
		
		try {
			String alignSeq = alignData.getAlignSeq1();
			double start = (double) alignData.getHitData().getCtgStart(); 
			double end =   (double) alignData.getHitData().getCtgEnd();

			if (alignData.isNTsTCW() && !alignData.isNTalign()) { // change to AA coords starting at 1
				if (start>end) { // reverse start and end (ruler is still 10, 20, so do not RC sequence)
					start = (double) alignData.getNtLen1() - start +1;
					end =   (double) alignData.getNtLen1() - end +1;
				}
				int aFrame = Math.abs(alignData.getFrame1());
				start = (start-aFrame)/3.0;	  
				end =   (end-aFrame-2)/3.0; // -2 is first base of codon 
			}
			else { // AA-AA and NT-NT (the 2nd may get reversed); this changes to 0-coordinate
				start--;
				end--;
			}
			// drawing coords - add gaps
			int gapStart = addGaps(alignSeq, (int) start); // gaps before hit
			int gapStop =  addGaps(alignSeq, (int) end);   // gaps within hit
			
			String tip = String.format("Seq Coords of Hit: %d-%d  Gap adjust: %d-%d", 
							(int)start, (int)end, gapStart, gapStop);

			double top = dSeq1Top - fInsetGap / 2.0;
			double low = dSeq1Low + fInsetGap / 2.0;
			
			super.setupHitPanels(tip, gapStart, gapStop, top, low); 
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Showing hit region");}
	}
	
	/********************************************
	 * CAS333 rewrote - was extending UTR over hit overhang
	 */
	private void highlightUTR() {
	try {
		double top = dSeq1Top  - fInsetGap / 2.0;
		double bottom = dSeq1Low + fInsetGap / 2.0;
		
		CodingRegion orf = alignData.getORFCoding1 ();
			
		int oFrame = 	Math.abs(orf.getFrame());
		int oEnd5 = 	orf.getBegin()-oFrame;
		int oStart3 =  	orf.getEnd()-oFrame;
		
		oEnd5 =   (oEnd5/3)+1; 		
		oStart3 = (oStart3/3)+1;		
		
		int gapEnd5 =  	  addGaps(alignSeq1, oEnd5)-1; 
		int gapStart3 =   addGaps(alignSeq1, oStart3)-1;
			
		if (nTrimStart1 != gapEnd5) {
			String tip5 = String.format("5'UTRs  Gapped Coords %d-%d", nTrimStart1, gapEnd5-1);
			super.setupUtrPanels(tip5, nTrimStart1, gapEnd5, top, bottom); 
		}
		if (gapStart3 < nTrimStop1) {
			String tip3 = String.format("3'UTRs  Gapped Coords %d-%d", gapStart3, nTrimStop1);
			super.setupUtrPanels(tip3, gapStart3, nTrimStop1, top, bottom); 
		}
	}
	catch (Exception e) {ErrorReport.prtReport(e, "Showing UTRs region");}
	}
	/******************************************************
	 * ----1234-567----	
	 * endCoord=4	gapCoord=8
	 * 
	 * return coordinate from beginning to endCoord with gaps added
	 * endCoord does not account for gaps
	 */
	private int addGaps(String alignSeq, int endCoord) { 
	try {
		int gapCoord = endCoord;
		int aLen = alignSeq.length()-1;
		
		int aLast=aLen;
		while (alignSeq.charAt(aLast) == Globals.gapCh && aLast>1) aLast--;// trailing gaps
		
		for (int s=0, a=0; s<=endCoord && a<aLast; a++) { // s=seq count w/o gaps, a=align count w gaps 
			if (alignSeq.charAt(a) == Globals.gapCh) 
				gapCoord++;
			else s++;
		}
		return gapCoord;
	}
	catch (Exception e) {Out.prt("TCW error");ErrorReport.prtReport(e, "Extending coords across gaps");}
	return endCoord;
	}
	
	public AlignData getAlignData() {return alignData;}
	
	private void init(String maxStr) {
	try {
		dFrameLeftX = 	(double) fLeftGap;
		dFrameTopY = 	(double) fTopGap;
		
		//dSeqStart = Math.max( super.getTextWidth( alignData.getDisplayStr1() ), super.getTextWidth( alignData.getDisplayStr2() ) );
		dSeqStart  = super.getTextWidth( maxStr); // CAS332 all start at same place
		dSeqStart += fInsetGap * 2.0d + fLeftGap; 
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
		
		super.setIndexRange (Math.max(alignSeq1.length(), alignSeq2.length()) );// CAS333 is final 1
		
		super.setTrimRange(Math.max(nTrimStart1, nTrimStart2), Math.min(nTrimStop1, nTrimStop2)); // CAS313 add	
		
		isDNA = alignData.getSeqData1().isNT();
		
		// was in setZoom
		if ( headerPanel != null ) remove ( headerPanel );
		
		headerPanel = new MultilineTextPanel ( Globals.textFont, 
				alignData.getDescription(numAlign), fInsetGap, (int)getFrameWidth ( ) - 2, 1); 
		headerPanel.setBackground( Color.WHITE );  
		add ( headerPanel );  
        headerPanel.setLocation( fLeftGap + 1, fTopGap + 1 );	
        
        dHeaderHeight = headerPanel.getHeight() + 1;
		dFrameHeight = dHeaderHeight 						// Header
					+ fInsetGap * 2.0d + fRowHeight * 2.0d + // Sequences
					+ fInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y = dFrameTopY + dHeaderHeight;
		
		dRulerTop = dDivider1Y;
		dRulerLow = dRulerTop + dRulerHeight;
		
		dSeq1Top = dRulerLow + (fInsetGap-2); 
		dSeq1Low = dSeq1Top + fRowHeight;
			
		dSeq2Top = dSeq1Low + (fInsetGap+2);
		dSeq2Low = dSeq2Top + fRowHeight;
		
		dSelectRow1Top = 	dSeq1Top - fInsetGap / 2.0;
		dSelectMid = 		dSeq1Low + fInsetGap / 2.0;
		dSelectRow2Low = 	dSeq2Low + fInsetGap;
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
	private double dFrameLeftX = 0, dFrameTopY = 0, dFrameHeight = 0;
	private double dDivider1Y = 0, dRulerTop = 0, dRulerLow = 0;
	
	private double dSeq1Top = 0, dSeq1Low = 0, dSeq2Top = 0, dSeq2Low = 0;
	private double dSelectRow1Top = 0, dSelectMid = 0, dSelectRow2Low = 0;
	private double dHeaderHeight = 0, dRulerHeight = 0, dSeqStart = 0;
	
	static final private int fMaxWidth = 700; // doesn't seem to change anything when I change
	static final private int fInsetGap = 5;
	static final private int fRowHeight = 15;
	static final private int fTopGap = 10, fLowGap = 5, fRightGap = 10, fLeftGap = 10; // CAS332 moved from PairViewPanel as always the same
	
    private static final long serialVersionUID = 1;
}
