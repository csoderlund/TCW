package cmp.viewer.seq.align;

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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Vector;

import util.ui.MultilineTextPanel;
import cmp.align.ScoreAA;
import cmp.align.PairAlignData;
import cmp.viewer.MTCWFrame;
import util.database.Globalx;

public class Pair3AlignPanel  extends BaseAlignPanel
{
	private static final char gapCh=Globalx.gapCh;
	public static int GRAPHICMODE = 1;
	public static int TEXTMODE = 0;
	private static final Font textFont = new Font("Sans", Font.PLAIN, 11);
	private static boolean isPair=true;
	
	public Pair3AlignPanel ( MTCWFrame parentFrame, PairAlignData theAlignment)
	{		
		// Setup the base class
		super (  );
		super.setBackground( parentFrame.getSettings().getFrameSettings().getBGColor() );
		super.setLayout( null );
				
		alignDataObj = theAlignment;
		descLines = alignDataObj.getDescLines();
		isNT = alignDataObj.isNT();	
		
		// XXX
		alignSeq1 = alignDataObj.getAlignFullSeq1();
		for (nStart1=0; nStart1 < alignSeq1.length() && alignSeq1.charAt(nStart1) == gapCh; nStart1++);
		for (nStop1=alignSeq1.length()-1; nStop1 >= 0 && alignSeq1.charAt(nStop1) == gapCh; nStop1--);
		
		alignSeq2 = alignDataObj.getAlignFullSeq2();
		for (nStart2=0; nStart2 < alignSeq2.length() && alignSeq2.charAt(nStart2) == gapCh; nStart2++);
		for (nStop2=alignSeq2.length() - 1; nStop2 >= 0 && alignSeq2.charAt(nStop2) == gapCh; nStop2--);
		
		seq1Label = alignDataObj.getSeqID1();
		seq2Label = alignDataObj.getSeqID2();
		
		dSequenceStart = Math.max( getTextWidth( seq1Label ), getTextWidth( seq2Label ) );
		dSequenceStart += nInsetGap * 2.0d + nGAP;
		setMinPixelX ( dSequenceStart );
		setIndexRange ( 0, Math.max(alignSeq1.length(), alignSeq2.length()) );
		
		dRulerHeight = getTextWidth( "999999" );
	}
	private void addDescLines() {
		try {
			if (headerPanel!=null) remove (headerPanel);
			
			headerPanel = new MultilineTextPanel ( textFont, descLines, nInsetGap, (int)getFrameWidth ( )-2, 1); 
			headerPanel.setBackground( Color.WHITE );  
			add ( headerPanel );  
			
			headerPanel.setLocation( nGAP + 1, nGAP + 1 );		
			dHeaderHeight = headerPanel.getHeight() + 1;
		}
		catch (Exception e) {e.printStackTrace();}
	}
	//---------------------------------JPanel Over-rides-------------------------------------//
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		
		// Draw the outside frame and divide
		Rectangle2D outsideBox = new Rectangle2D.Double ( dPAD, dPAD, getFrameWidth (), dFrameHeight );
		Rectangle2D highlightBox = new Rectangle2D.Double ( dPAD - 2, dPAD - 2, getFrameWidth () + 4, dFrameHeight + 4 );
		
		if ( hasSelection() ) // Outline whole thing
		{
			Stroke oldStroke = g2.getStroke();
			g2.setColor( Globalx.selectColor );
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
	
		drawRuler( g2, 0, dRulerTop, dRulerBottom );
		
		boolean isFirst=true;
		drawName( g2, seq1Label, dPAD + nInsetGap, dSeq1Top );
		drawSequence ( g2, seq1Label, alignSeq1, dSeq1Top, dSeq1Bottom, isFirst, isNT, isPair); 
		
		isFirst=false;
		drawName( g2, seq2Label, dPAD + nInsetGap, dSeq2Top );
		drawSequence ( g2, seq2Label, alignSeq2, dSeq2Top, dSeq2Bottom, isFirst , isNT, isPair); 
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
	
	//---------------------AlignBasePanel Over-rides-------------------------------------//
	
	protected char getChar(boolean bFirst, int nPos) { 
		if (bFirst) return alignSeq2.charAt( nPos );
		else return alignSeq1.charAt( nPos );
	}
	protected boolean isEndGap(boolean bFirst, int nPos) {
		if( bFirst && (nPos < nStart2 || nPos > nStop2)) return true;
		if(!bFirst && (nPos < nStart1 || nPos > nStop1)) return true;
		return false;
	}
	
	public void setZoom ( int nBasesPerPixel ) throws Exception
	{
		super.setZoom( nBasesPerPixel );
		super.setMinPixelX ( dSequenceStart );
		
		addDescLines(); // XXX
		
		dFrameHeight =  dHeaderHeight 						// Header
					 + nInsetGap * 2.0d + nRowHeight * 2.0d + // Sequences
					 + nInsetGap * 2.0d + dRulerHeight; 		// Ruler
		dDivider1Y =   		dPAD + dHeaderHeight;
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
	public double getGraphicalDeadWidth ( )
	{
		// The total width of what can't be used for drawing the bases in graphical mode
		return dSequenceStart + nInsetGap;
	}
	//------------------- private -----------------------/
	private double getFrameWidth ( )
	{
		double dWidth = dSequenceStart + getSequenceWidth ( ) + nInsetGap;
		return Math.max ( 700, dWidth );
	}
	private double getFrameRight ( )
	{
		return dPAD + getFrameWidth ( );
	}
	private void selectAll () { 
		bSeq1Selected = bSeq2Selected = true; 
		repaint (); 
	};
	//------------------- AlignPairView3 -----------------------//
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
  		super.setMinPixelX ( dSequenceStart );
	}
	
	public void selectNone () {	
		bSeq1Selected = bSeq2Selected = false; 
		repaint ();
	};
	
	public boolean hasSelection () { return bSeq1Selected || bSeq2Selected; };
	
	public void setBorderColor(Color newColor) {borderColor = newColor;}
	
	public void handleClick( MouseEvent e, Point localPoint ) {} // CAS312 removed code - nothing selectable
	public PairAlignData getAlignData() { 
		return alignDataObj;
	}
	/******************************************************/
	// Attibutes for positioning
	
	private PairAlignData alignDataObj=null;
	private String alignSeq1 = "";
	private int nStart1 = 0;
	private int nStop1 = 0;
	private String alignSeq2 = "";
	private int nStart2 = 0;
	private int nStop2 = 0;
	private String seq1Label = "";
	private String seq2Label = "";
	private boolean isNT = true;
	
    private MultilineTextPanel headerPanel=null;

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
	static private final double dPAD=10;
	 
	private Vector <String> descLines;
    private static final long serialVersionUID = 1;
}
