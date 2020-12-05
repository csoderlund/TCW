package cmp.viewer.seq.align;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import util.methods.Out;
import util.ui.MultilineTextPanel;
import cmp.align.*;
import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

public class MultiAlignPanel extends BaseAlignPanel {
	private static final long serialVersionUID = 8622662337486159085L;

	public MultiAlignPanel(MTCWFrame parentFrame, boolean isAA, MultiAlignData alignData) {
		
		super ();
		super.setBackground( parentFrame.getSettings().getFrameSettings().getBGColor() );
		super.setLayout( null );
		
		seqArr =     alignData.getAlignSeq();
		seqNameArr = alignData.getSequenceNames();
		
		int numSeq = seqArr.length;
		seqStarts = new int[numSeq];
		seqStops =  new int[numSeq];
		seqSelected = new boolean[numSeq];
		dSeqStart = 0.0;
		
		int maxSeqLen = 0;
		for(int x=0; x<numSeq; x++) { 
			for(seqStarts[x] = 0; 
				seqStarts[x]<seqArr[x].length() && seqArr[x].charAt(seqStarts[x])==Globals.gapCh; 
				seqStarts[x]++);
			for(seqStops[x]=seqArr[x].length()-1; 
				seqStops[x]>=0 && seqArr[x].charAt(seqStops[x])==Globals.gapCh; 
				seqStops[x]--);
			dSeqStart = Math.max(dSeqStart, super.getTextWidth(seqNameArr[x]));
			maxSeqLen = Math.max(maxSeqLen, seqArr[x].length());
			seqSelected[x] = false;
		}
		columnSelected = new boolean[maxSeqLen];
		for(int x=0; x<columnSelected.length; x++) columnSelected[x] = false;

		dSeqStart += nInsetGap * 2.0d + nPAD;
		super.setMinPixelX ( dSeqStart );
		super.setIndexRange ( 0, maxSeqLen );
		
		isDNA=!isAA;
		
		dRulerHeight = super.getTextWidth( "999999" );
	}
	
	//---------------------------------JPanel Over-rides-------------------------------------//
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );
		
		if(super.getDrawMode() == TEXTMODE) {
			double width = getTextWidth("?");

			g2.setColor( selectColor );
			for(int x=0; x<columnSelected.length; x++) {
				if(columnSelected[x]) {
					double pos = calculateWriteX(x);
					
					g2.fill( new Rectangle2D.Double( pos, dPAD, width, dFrameHeight) );
				}
			}			
		}
		if ( hasSelection() )// Outline whole thing
		{
			g2.setColor( selectColor );
			
			for(int x=0; x<seqSelected.length; x++) {
				if(seqSelected[x]) {
					g2.fill( new Rectangle2D.Double( dPAD, topSelect[x], getFrameRight ( ) - dPAD, bottomSelect[x] - topSelect[x]) );
				}
			}
			g2.setColor( Color.BLACK );
		}
			
		g2.setColor( borderColor );
		g2.draw ( outsideBox );
		g2.draw( new Line2D.Double( dPAD, dDivider1Y, getFrameRight ( ), dDivider1Y ) );
		
		super.drawRuler( g2, 0, dRulerTop, dRulerBottom );

		for(int x=0; x<seqArr.length; x++) {
			super.drawName( g2, seqNameArr[x], dPAD + nInsetGap, seqTop[x] );
			super.drawSequence ( g2, seqNameArr[x], seqArr[x], seqTop[x], seqBottom[x], (x==0), isDNA , false /* isPair */);
		}
	}
	
	public Dimension getMaximumSize()
	{
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize()
	{
		int nWidth = nPAD + (int)getFrameWidth ( ) + nPAD;
		int nHeight = nPAD + (int)dFrameHeight + nPAD;
		return new Dimension( nWidth, nHeight ); 
	}
	
	//---------------------------------AlignBasePanel Over-rides-------------------------------------//
	
	protected char getChar(boolean bFirst, int nPos) { // nPos char of consensus sequence; ignore bFirst
		if (nPos>=seqArr[0].length()) return ' ';
		return seqArr[0].charAt(nPos);
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if(nPos < seqStarts[0] || nPos >seqStops[0]) return true;
		return false;
	}
	
	public void refreshPanels ( ) 
	{ 
		selectionBox = new Rectangle2D.Double ( dPAD, dPAD,  getFrameWidth (), dFrameHeight );
	}
	//--------------- private -----------------------/

	private double getFrameWidth ( )
	{
		double dWidth = dSeqStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	private double getFrameRight ( )
	{
		return dPAD + getFrameWidth ( );
	}
	
	//---------------AlignMultiViewPanel -------------------/
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
  		super.setMinPixelX ( dSeqStart );
	}
	
	public void setZoom ( int nBasesPerPixel ) throws Exception
	{
		super.setZoom( nBasesPerPixel );
		super.setMinPixelX ( dSeqStart );

		dFrameHeight = nInsetGap * ((double)seqArr.length) + nRowHeight * ((double)seqArr.length) + // Sequences
					 + nInsetGap * ((double)seqArr.length) + dRulerHeight; 		// Ruler
		dDivider1Y = dPAD + dHeaderHeight;
		dRulerTop = dDivider1Y + nInsetGap;
		dRulerBottom = dRulerTop + dRulerHeight;
		
		seqTop = new double[seqArr.length];
		seqBottom = new double[seqArr.length];
		topSelect = new double[seqArr.length];
		bottomSelect = new double[seqArr.length];
		
		double top = dRulerBottom + nInsetGap;
		for(int x=0; x < seqArr.length; x++) {
			seqTop[x] = top;
			seqBottom[x] = seqTop[x] + nRowHeight;
			
			topSelect[x] = seqTop[x] - nInsetGap / 2.0;
			bottomSelect[x] = seqBottom[x] + nInsetGap / 2.0;
			
			top = seqBottom[x] + nInsetGap;
		}
	}
	
	public double getGraphicalDeadWidth ( ) {
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSeqStart + nInsetGap;
	}
	
	public void setBorderColor(Color newColor) {
		borderColor = newColor;
	}
	public void selectAllRows () { 
		for(int x=0; x<seqSelected.length; x++) seqSelected[x] = true;
		repaint (); 
	}
	public void selectNoRows () {	
		for(int x=0; x<seqSelected.length; x++) seqSelected[x] = false;
		repaint ();
	}
	public void selectAllColumns () {
		for(int x=0; x<columnSelected.length; x++) columnSelected[x] = true;
		repaint();
	}
	public void selectNoColumns () {
		for(int x=0; x<columnSelected.length; x++) columnSelected[x] = false;
		repaint();
	}
	public boolean hasSelection () {
		boolean retVal = false;
		for(int x=0; x<seqSelected.length && !retVal; x++)
			retVal = seqSelected[x];
		return retVal;
	}
	public int getNumSequencesSelected() {
		int retVal = 0;
		for(int x=0; x<seqSelected.length; x++)
			if(seqSelected[x])
				retVal++;
		return retVal;
	}
	public String getSelectedSequence() {
		String retVal = "";
		for(int x=0; x<seqSelected.length && retVal.length() == 0; x++) {
			if(seqSelected[x])
				retVal = seqArr[x];
		}
		return retVal.replaceAll(Globals.gapStr, ""); 
	}
	public void handleClick( MouseEvent e, Point localPoint )
	{
		boolean highlightChanged = false;
        if ( selectionBox.contains(localPoint) )
        {     
        	//Determine the column for the click
    		if(super.getDrawMode() == TEXTMODE) {
    			int xPos = calculateIndexFromWriteX ( localPoint.x );
    			if(xPos >= 0 && xPos < columnSelected.length) {
    				highlightChanged = true;
        			if ( !e.isControlDown() && !e.isShiftDown() ) selectNoColumns();
    				columnSelected[xPos] = !columnSelected[xPos];
    			}
    		}
        	
            // Determine the row for the click.
        	if ( topSelect[0] <= localPoint.y && localPoint.y <= bottomSelect[bottomSelect.length-1])
    		{
        		highlightChanged = true;
    			// Clear all selected rows if neither shift nor control are down
    			if ( !e.isControlDown() && !e.isShiftDown() ) selectNoRows();
    			
    			for(int x=0; x<topSelect.length; x++) {
        			if ( topSelect[x] <= localPoint.y && localPoint.y <= bottomSelect[x] )
        				seqSelected[x] = !seqSelected[x];    				
    			}
    		}
    		if(!highlightChanged) {	
                // Click is inside the box, but not within any of the sequences (i.e. in the header)
    			if ( ( e.isControlDown() || e.isShiftDown() ) && hasSelection () ) {
    				selectNoRows ();
    				selectNoColumns ();
    			}
    			else {// Select all sequences
    				selectAllRows ();
    				selectAllColumns ();
    			}
            }
        }
        else if ( !e.isControlDown() && !e.isShiftDown() ) {
            // Click outside of the main box, clear everything unless shift or control is down 
            selectNoRows ();
            selectNoColumns ();
        }
		
		repaint();
	}
	
	private String [] seqArr = null;
	private String [] seqNameArr = null;
	private int [] seqStarts = null;
	private int [] seqStops = null;
	private boolean isDNA = true;
	
    MultilineTextPanel headerPanel;

    private boolean [] seqSelected = null;
    private boolean [] columnSelected = null;
	Rectangle2D selectionBox = new Rectangle2D.Double (0,0,0,0);
	private Color borderColor = Color.BLACK;
	
	// Attributes for where to draw
	private double dFrameHeight = 0;
	private double dDivider1Y = 0;
	private double dRulerTop = 0;
	private double dRulerBottom = 0;
	
	private double [] seqTop = null;
	private double [] seqBottom = null;
	
	private double [] topSelect = null;
	private double [] bottomSelect = null;
	
	private double dHeaderHeight = 0;
	private double dRulerHeight = 0;
	private double dSeqStart = 0;
	
	private final int nPAD=10;
	private final double dPAD=10.0;	
	private final int nInsetGap = 5;
	private final int nRowHeight = 15;
}
