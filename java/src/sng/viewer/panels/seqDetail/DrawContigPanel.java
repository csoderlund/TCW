package sng.viewer.panels.seqDetail;
/**
 * Draws the contig
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.PrintStream;

import sng.database.Globals;
import sng.dataholders.ContigData;
import sng.dataholders.SequenceData;
import sng.viewer.panels.MainAlignPanel;
import sng.viewer.panels.MainToolAlignPanel;
import util.ui.MultilineTextPanel;
import util.ui.UIHelpers;
import util.database.Globalx;
import util.methods.Converters;
import util.methods.ErrorReport;

public class DrawContigPanel extends MainAlignPanel 
{	
	private static final boolean DEBUG = false;
	
	public DrawContigPanel (	ContigData inContig, 
							int nTopGap, 
							int nBottomGap, 
							int nLeftGap, 
							int nRightGap ) throws Exception 
	{
		super ( Globals.textFont );
		
		if (DEBUG)	System.err.println("ContigPanel: contigID="+inContig.getContigID());
		
		theContig = inContig;
		theContig.setSortOrder( ContigData.SORT_BY_GROUPED_LEFT_POS );
		theRefSequence = theContig.getSeqData ( );
		
		// Setup all of the constants based on the input gap sizes
		TOP_GAP = nTopGap;
		BOTTOM_GAP = nBottomGap;
		LEFT_GAP = nLeftGap;
		RIGHT_GAP = nRightGap;
        
        // Determine the maximum width of any of the contig names
        WIDEST_CLONE_NAME = getMaxNameWidth ();
        
        // Graphical view X positions 
        graph_COL1_X = INSET_WIDTH + LEFT_GAP;
        graph_COL2_X = graph_COL1_X + WIDEST_CLONE_NAME;
        graph_COL3_X = graph_COL2_X;
        graph_DATA_X_START_POS = graph_COL3_X + 15;
        graph_ORIG_CONTIG_WIDTH = 0;
        if ( theContig.haveOldContigData () )
            graph_ORIG_CONTIG_WIDTH = 120;

		// Clear the layout manager, since setupRows will place sub-panels is specific spots.
		// (Over the P's.)
		setLayout( null );
        setBackground( Color.WHITE );
		
		// Set the name for the reference sequence
		strRefSeqName = "Consensus";
        
        setBasesPerPixel ( 3 );	
		super.setIndexRange( theContig.getLowIndex(), theContig.getHighIndex() );
		super.setMinPixelX ( graph_DATA_X_START_POS );
	}	
	
	public ContigData getContig() 
	{
		return theContig;
	}
	
	public String getContigID ( )
	{
		return theContig.getContigID();
	}
	
	public Vector<String> getContigIDs ( ) 
	{
		Vector<String> out = new Vector<String>();
		out.add(getContigID());
		
		return out;
	}
	
	public int getTotalBaseWidth ( )
	{
		return theContig.getTotalBaseWidth();
	}
	
	public double getGraphicalDeadWidth ( )
	{
		// The total width of what can't be used for drawing the 
		// bases in graphical mode
		return graph_DATA_X_START_POS + 
			graph_ORIG_CONTIG_WIDTH + INSET_WIDTH;
	}
	
	protected double getFrameLeft () 
	{ 
		return LEFT_GAP; 
	}
	
	protected double getFrameRight () 
	{ 
		return LEFT_GAP + insideWIDTH; 
	}
	
	public int getNumBuried() {
		int count = 0;
		
		Iterator  <Object> iter = rowsOfESTS.iterator();
		while (iter.hasNext()) {
			Object row = iter.next();
			if (row instanceof SequenceData) {
				if (((SequenceData)row).isBuried()) count++;
			}
			else {
				SequenceData est1 = ((SequenceData[])row)[0]; 
				SequenceData est2 = ((SequenceData[])row)[1]; 
				
				if (est1.isBuried()) count++;
				if (est2.isBuried()) count++;
			}
		}		
		return count;
	}
	
	public void setBasesPerPixel ( int nBasesPerPixel ) throws Exception
	{
		super.setBasesPerPixel( nBasesPerPixel );
		
		basesPerPIXEL = nBasesPerPixel;		
		
		graph_ORIG_CONTIG_X = ( theContig.getTotalBaseWidth() ) / 
			basesPerPIXEL + graph_DATA_X_START_POS + INSET_WIDTH;
		graph_END_X = graph_ORIG_CONTIG_X + graph_ORIG_CONTIG_WIDTH;
					// width of region 
		graph_WIDTH = Math.max( graph_END_X - LEFT_GAP, 1000); 
        
        if ( textPanel != null )
            remove ( textPanel );
        textPanel = new MultilineTextPanel ( theFont, theContig.getDescription(), 
        	INSET_WIDTH, graph_WIDTH - 2, 1 ); 
        textPanel.setBackground( Color.WHITE );     

        // Header Y positions
        seq_Y_BOTTOM = TOP_GAP + textPanel.getHeight() + INSET_WIDTH * 4 
        		+ (int)getTextWidth ( "9999" );
        read_Y_POS_START = seq_Y_BOTTOM + 5 + READ_Y_ROW_HEIGHT;
        read_TOP_Y_POS_START = read_Y_POS_START - READ_Y_ROW_HEIGHT;
        headerHEIGHT = read_TOP_Y_POS_START - TOP_GAP;
		seq_Y_TOP = seq_Y_BOTTOM - READ_Y_ROW_HEIGHT;
        
        // Base view X positions
        base_TITLE_X_START_POS = INSET_WIDTH + LEFT_GAP;
        base_DATA_X_START_POS = base_TITLE_X_START_POS + WIDEST_CLONE_NAME + 20;
        base_TITLE_WIDTH = base_DATA_X_START_POS - base_TITLE_X_START_POS;  
        
        // Add all of the sub panels for tool tips, drawing the header, etc
        refreshPanels ( );        
	}
	
	public void refreshPanels ( )
	{
		setupRows ( );
		repaint ();
	}
	
	public void setShowSNPs ( boolean b )
	{
		bShowSNPs = b;	
		theContig.prepareSNPs(); 	
		repaint ();
	}

	public void setShowBuried ( int showMode )
	{
		nShowBuriedMode = showMode;
		setupRows();
		repaint ();
	}

	
	//	--------------Base Class Over-rides----------------------------//
	
	// Ref never has mismatches
	protected boolean getIsMismatchAt ( SequenceData seq, int nPos ) 
	{ 	char c = seq.safeGetBaseAt( nPos );
		if (c == Globals.gapCh || c == ' ') return false;	
		char r = theRefSequence.safeGetBaseAt( nPos );
		if (r == Globals.gapCh || r == ' ') return false;	
		return c != r; 
	}
	
	// Ref or ESTs can have GAPs
	protected boolean getIsGapAt ( SequenceData seq, int nPos ) 
	{ 
		char c = seq.safeGetBaseAt( nPos );
		if (c == Globals.gapCh) return true;
		else return false;
	}
	protected boolean getIsLowQualityAt ( SequenceData seq, int nPos ) 
	{ 
		return seq.isValidIndex ( nPos ) &&  seq.isLowQualityAt( nPos ); 
	}	

	// The joining n's in the consensus
	protected boolean getIsNAt( int nPos) 
	{
		return theRefSequence.isValidIndex( nPos ) 
				&& theRefSequence.isNAt(nPos);
	}
	// not currently used
	protected boolean getIsReferenceNAt ( SequenceData seq, int nPos ) 
	{ 
		return theRefSequence.isValidIndex( nPos ) 
				&& seq.isValidIndex( nPos ) 
				&& theRefSequence.isGapAt( nPos )
				&& !seq.isGapAt( nPos ); 
	}
	
	//---------------------------DRAW-------------------//
	
	public void paintComponent(Graphics g)
	{
		try
		{
			super.paintComponent(g); 
			g2 = (Graphics2D)g; 
            
			/************** Background **************/
			
			// Fill in the backgrounds for non-coding regions
			drawCodingPanels ( g2 );
			
			// Draw the colored background and the column text for each EST 
			drawESTRows ( true /* background */ );	

			// Highlight SNPS if this is an EST contig
			if ( bShowSNPs && theContig.getSNPCount() > 0 ) 
				highlightSNPs ();
			
			// TODO make draw like blast. If a column is selected, fill in the background
			if ( super.getDrawMode() != GRAPHICMODE )
			{		
				int nMaxX =  super.getWriteWidthInt( ) + base_DATA_X_START_POS;
				
				if( base_DATA_X_START_POS <= nHighlightX && nHighlightX <= nMaxX )
				{
					int nColumn = super.calculateIndexFromWriteX( nHighlightX );
					int nX = (int)super.calculateWriteX( nColumn );
					
					g2.setColor( Globalx.selectColor );
					g2.fill( new Rectangle2D.Double ( nX, TOP_GAP, 
							super.getCellWidthInt ( ), insideHEIGHT ) );
				}			
			}
			
			/************** Header **************/
			Rectangle clipRect = g2.getClipBounds();
			if ( clipRect.getMinY() < read_Y_POS_START )
			{							
				// Ruler for base position
				int nRulerTop = seq_Y_TOP - INSET_WIDTH - 
						(int)getTextWidth ( "9999" );
				super.drawRuler ( g2, INSET_WIDTH, nRulerTop, seq_Y_TOP );
				
				// draw/write Consensus
				if( super.getDrawMode() == GRAPHICMODE )
					super.drawSequenceLine ( g2, theRefSequence, 
							seq_Y_TOP, seq_Y_BOTTOM );	
				else
					super.writeSequenceLetters( g2, theRefSequence, 
							seq_Y_TOP, seq_Y_BOTTOM );					
			}
			
			/************** Foreground for ESTs **************/
			drawESTRows ( false /* !background */ );
			
			/*************** Box Around Everything *****************/

			// Outline the whole thing
			if ( !selectedESTs.isEmpty() )
			{
				Stroke oldStroke = g2.getStroke();
				g2.setColor(Globalx.selectColor);
				g2.setStroke( new BasicStroke (3) );
				g2.draw( outsideBox );
				g2.setColor( Color.BLACK );
				g2.setStroke ( oldStroke );
			}
			
			g2.draw( new Rectangle2D.Double ( LEFT_GAP, TOP_GAP, insideWIDTH, insideHEIGHT ) );
		
			// Line between consensus and clones
			int nDivideY = read_TOP_Y_POS_START - 1;
			g2.draw( new Line2D.Double( LEFT_GAP, nDivideY, LEFT_GAP + insideWIDTH, nDivideY ) );
		} 
		catch (Exception e) 
		{
			ErrorReport.reportError("ContigAlignPanel Internal error: painting components");
		}
	}
		
	void drawESTRows ( boolean bBackground)
	{
		Rectangle clipRect = g2.getClipBounds();
		SequenceData prevEST = null;
		SequenceData curEST = null;
		SequenceData curMateEST = null;
		SequenceData nextEST = null;
		SequenceData nextMateEST = null;
		int numExtras;
		
		boolean bHaveMate = false;

        // Choose the range of row indices for the ESTs that are visible
        int nStart = Math.max( (int) ( (clipRect.getMinY() - 
        		read_Y_POS_START) / READ_Y_ROW_HEIGHT ), 0 );
        int nEnd = Math.min( (int)( (clipRect.getMaxY() - read_Y_POS_START) / 
        		READ_Y_ROW_HEIGHT  ) + 2, rowsOfESTS.size() );
        
		double dYTop = read_TOP_Y_POS_START + nStart * READ_Y_ROW_HEIGHT;
		double dYBottom = dYTop + READ_Y_ROW_HEIGHT;
		
		// Draw out the clones row by row.  If possible put the clone and its mate in the same row
		for ( int i = nStart; i <= nEnd; ++i )
		{
			numExtras = 0;
			// Downgrade next, current, previous from last iteration
			prevEST = curEST;
			curEST = nextEST;
			curMateEST = nextMateEST;
							
			// Load the EST(s) for the next iteration
			nextEST = getFirstESTForRow ( i );
			nextMateEST = getSecondESTForRow ( i );
			
			// If this is the first iteration, continue since we don't have a current node yet
			if ( i == nStart )
				continue;
			
			// See if there is a mate for this clone in the contig
			bHaveMate = theContig.getSequenceMate ( curEST ) != null;
			
			numExtras = curEST.getNumTGaps();
			// Set the enumerated value
			int nMatePos = MATE_NOT_IN_CONTIG;
			if ( curMateEST != null ) {
				nMatePos = MATE_IN_SAME_ROW_PART_1;
				numExtras += curMateEST.getNumTGaps();
			}
			else if ( !bHaveMate )
				nMatePos = MATE_NOT_IN_CONTIG;
			else if ( ContigData.areMates( curEST, nextEST ) )
				nMatePos = MATE_IS_NEXT;
			else if ( ContigData.areMates( prevEST, curEST ) )
				nMatePos = MATE_IS_PREV;
			else 
				nMatePos = MATE_NOT_ADJACENT;		
			
			boolean bRowSelected = selectedESTs.contains( curEST.getName() );
			if ( curMateEST != null ) bRowSelected |= selectedESTs.contains( curMateEST.getName() ); 
			
			// Fill in the foreground or background for the row
			if (super.getDrawMode() == GRAPHICMODE) {	
				// Graphical view
				if ( bBackground )
					drawRowBackground ( curEST, dYTop, dYBottom, nMatePos, bRowSelected );
				else {
					drawRowForeground ( curEST, numExtras, dYTop, dYBottom, nMatePos, bRowSelected );
					super.drawSequenceLine ( g2, curEST, dYTop, dYBottom, nShowBuriedMode );						
					if ( curMateEST != null )
						// Draw mate EST on the same row
						super.drawSequenceLine ( g2, curMateEST, dYTop, dYBottom, nShowBuriedMode );	
				}
			}
			else {
				// Sequence view
				if ( bBackground )
					writeBackground ( curEST, dYTop, dYBottom, nMatePos, bRowSelected );
				else {
					writeForeground ( curEST, dYTop, dYBottom, nMatePos, bRowSelected );
					super.writeSequenceLetters( g2, curEST, dYTop, dYBottom );	
				}
			}				
			
			// Next row:
			dYBottom += READ_Y_ROW_HEIGHT;
			dYTop += READ_Y_ROW_HEIGHT; 
		}
	}
   
	void highlightSNPs ()
	{
		// Highlight each SNP's column
		int nYTop = read_TOP_Y_POS_START;
		int nDataHeight = rowsOfESTS.size() * READ_Y_ROW_HEIGHT;
		int nYBottom = nYTop + nDataHeight;			
		
		for( int i = theContig.getLowIndex(); i <= theContig.getHighIndex(); i++ ) {
			if ( theContig.maybeSNPAt(i) ) {
				if ( super.getDrawMode() == GRAPHICMODE ) {
					int nX = (int)calculateDrawX(i);
					g2.setColor (Globalx.mediumGray);
					g2.drawLine ( nX, nYTop, nX, nYBottom );
				}
				else {
					int nX1 = (int)super.calculateWriteX(i);
					g2.setColor ( SNPbgColor );
					g2.fillRect( nX1, nYTop, super.getCellWidthInt ( ), nDataHeight );
				}
			}
		}		
	}

	private String[] getESTsForRow ( int i ) 
	{
		if ( rowsOfESTS == null || i >= rowsOfESTS.size() || i < 0 ) // Invalid index
			return null;
		else if ( rowsOfESTS.get(i) instanceof SequenceData ) //  single EST in row
			return new String[] { ((SequenceData)rowsOfESTS.get(i)).getName() };
		else {
			// pair of ESTs for the row
			SequenceData[] ESTPair = (SequenceData[])rowsOfESTS.get(i);
			return new String[] { ESTPair[0].getName(), ESTPair[1].getName() };
		}		
	}
	
	private SequenceData getFirstESTForRow ( int i )
	{
		if ( rowsOfESTS == null || i >= rowsOfESTS.size() || i < 0 ) // Invalid index
			return null;
		else if ( rowsOfESTS.get(i) instanceof SequenceData ) //  single EST in row
			return (SequenceData)rowsOfESTS.get(i);
		else {
			// pair of ESTs for the row
			SequenceData[] ESTPair = (SequenceData[])rowsOfESTS.get(i);
			return ESTPair[0];	
		}		
	}
	
	private SequenceData getSecondESTForRow ( int i )
	{
		if ( i == rowsOfESTS.size() || i < 0 ) // Invalid index
			return null;
		else if ( rowsOfESTS.get(i) instanceof SequenceData[] ) {
			// pair of ESTs for the row
			SequenceData[] ESTPair = (SequenceData[])rowsOfESTS.get(i);
			return ESTPair[1];	
		}	
		else
			return null;
	}
	
	private void drawRowBackground( SequenceData seqData, 
										double dYTop, 
										double dYBottom,
										int nMatePos,
										boolean bSelectedRow )
	{	
		// Fill in the background based on context
		if ( bSelectedRow ) g2.setColor( Globalx.selectColor );
		else if( !seqData.isReverseComplement()  ) {
			if ( !seqData.hasStrandLabel () || seqData.isForwardStrand() )
				g2.setColor( contigFor );
			else
				g2.setColor( contigErr );				
		}
		else {
			if ( !seqData.hasStrandLabel () || !seqData.isForwardStrand() )
				g2.setColor( contigRev );
			else
				g2.setColor( contigErr );				
		}
		g2.fill( new Rectangle2D.Double( LEFT_GAP, dYTop, graph_WIDTH, 
				READ_Y_ROW_HEIGHT ) );
		g2.setColor(Color.black);
	}
	
	void drawRowForeground ( SequenceData seqData, 
								int numExtras,
								double dYTop, 
								double dYBottom,
								int nMatePos,
								boolean bSelectedRow )
	{
		// If the clone is being drawn on the same line as its mate, adjust the name
		String clonename = getSequenceName ( seqData );
		if ( clonename != null && clonename.length() >= 2 )
		{
			char mysuffix = clonename.charAt(clonename.length() - 1);
			if( nMatePos == MATE_IN_SAME_ROW_PART_1 )
			{
				clonename = clonename.substring(0, clonename.length() - 1);
				if(mysuffix == 'r') clonename += "f-r";
				if(mysuffix == 'f') clonename += "r-f";
			} 
		}
			
		//	Draw the first three columns of text
		int nWidth = (int)getTextWidth ( clonename );
		if ( nMatePos == MATE_IS_NEXT ||
				nMatePos == MATE_IN_SAME_ROW_PART_1 )
		{
			int nRows = 1;
			if ( nMatePos == MATE_IS_NEXT ) nRows = 2;
			outlineMatePairs ( graph_COL1_X, (int)dYTop, nWidth, null, nRows );
		}

		drawText( g2, clonename, graph_COL1_X, dYTop ); // Name
		
		if ( numExtras > 0 )
			drawText ( g2, "("+numExtras+")", graph_COL1_X + nWidth + 10, dYTop, Color.red );
		
		// Draw the mate information/best match 
		if ( theContig.haveOldContigData() )
			drawText( g2, seqData.getOldContig(), graph_ORIG_CONTIG_X, dYTop ); // The orignal contig	
	
		g2.setColor(Color.black);		
	}
	

	//----------------TEXT MODE----------------//
	
	private void writeBackground ( SequenceData seqData, double dYTop, double dYBottom, int nMatePos, boolean bSelectedRow  ) 
	{
		// If the current row is selected, fill in the background
		if ( bSelectedRow )
		{
			g2.setColor( Globalx.selectColor );
			int nWidth = base_DATA_X_START_POS + getWriteWidthInt ( );
			g2.fill( new Rectangle2D.Double( LEFT_GAP, dYTop, nWidth, READ_Y_ROW_HEIGHT ) );		
		}
		g2.setColor(Color.black);
	}

	private void writeForeground ( SequenceData seqData, double dYTop, double dYBottom, int nMatePos, boolean bSelectedRow  ) 
	{			
		// EST name
		if ( nMatePos == MATE_IS_NEXT ||
					nMatePos == MATE_IN_SAME_ROW_PART_1 )
		{
			int nRows = 1;
			if ( nMatePos == MATE_IS_NEXT )
				nRows = 2;
			
			int nWidth = (int)getTextWidth ( seqData.getName() );
			outlineMatePairs ( base_TITLE_X_START_POS, (int)dYTop, nWidth, null, nRows );
		}
		
		drawText( g2, getSequenceName ( seqData ), base_TITLE_X_START_POS, dYTop ); // Name
	}
	
	private void outlineMatePairs ( int nX, int nYTop, int nTextWidth, Color bgColor, int nRows )
	{
		int ARC_WIDTH = 9;
		int HORIZ_OUTLINE_GAP = 4;
		
		int nHeight = READ_Y_ROW_HEIGHT * nRows;
		int nTop = nYTop;		
		int nLeft = nX - HORIZ_OUTLINE_GAP;
		int nWidth = nTextWidth + 2 * HORIZ_OUTLINE_GAP;
		
		// Fill background color if we have one 
		if ( bgColor != null )
		{
			g2.setColor( bgColor );
			g2.fillRoundRect( nLeft, nTop, nWidth, nHeight, ARC_WIDTH, ARC_WIDTH );
		}
			
		// Draw the out line
		g2.setColor( Color.blue );
		g2.drawRoundRect( nLeft, nTop, nWidth, nHeight, ARC_WIDTH, ARC_WIDTH );
	}	

	private int getMaxNameWidth ( )
	{
		int nMax = 0;
		for ( int i = 0; i < theContig.getNumSequences (); ++i )
		{
			SequenceData cloneData = theContig.getSequenceAt( i );
			int nWidth = (int)getTextWidth ( getSequenceName ( cloneData ) ) 
							+ (int)getTextWidth( "("+cloneData.getBuriedChildCount()+")" ) 
							+ 15; // +15 is for "r-f" if mate-pair
			nMax = Math.max( nMax, nWidth ); 
		}
		return Math.max( nMax, (int)getTextWidth ( strRefSeqName ) );
	}
	
	private void setupRows ( )
	{
		// Find the object for the row that was clicked last
		SequenceData lastESTClicked = getFirstESTForRow ( nLastRowClicked );
		
		// Remove the old find mate panels
		removeAll ( );
        
        // Add the panel the draws the header text
        add ( textPanel );  
        textPanel.setLocation( LEFT_GAP + 1, TOP_GAP + 1 );
		
		// Clear any row selection data
		nLastRowClicked  = -1; 
		
		// Clear column selection
		nHighlightX = 0;
		
		// Place ESTs into rows according to the current sort order
		boolean bHaveMate, bNextIsMate, bDrawWithMate; 
		
		rowsOfESTS = new Vector <Object>( ); // SequenceData or SequenceData[]
		bNextIsMate = false;
		
	
		for ( int i = 0; i < theContig.getNumSequences (); ++i )
		{
			SequenceData cloneData = theContig.getSequenceAt( i );
			if(cloneData.isBuried() && nShowBuriedMode == MainToolAlignPanel.HIDE_BURIED_EST )
				continue;
			
			if ( Converters.compareObjects( lastESTClicked, cloneData ) )
				nLastRowClicked = i;
				
			// See if there is a mate for this clone in the contig
			//Check if mate exists, and if one or both is buried, if so, not a mate
			SequenceData tempEST = theContig.getSequenceMate ( cloneData );
			bHaveMate = tempEST != null && !cloneData.isBuried() && !tempEST.isBuried();

			// See if the next clone is the mate for this one
			bNextIsMate = bHaveMate && 
							(i + 1) < theContig.getNumSequences () &&
								theContig.areMatesAt( i, i + 1 ); 

			// See if we can draw this EST in the same row as the next one
			SequenceData cloneMateData = null;
			bDrawWithMate = false;
			if ( bNextIsMate && super.getDrawMode() == GRAPHICMODE )
			{
				// Find the span of indexes for the first clone in terms of the reference sequence
				int nFirstMateStart = cloneData.getLowIndex();
				int nFirstMateEnd = cloneData.getHighIndex();
				
				// Get the data for this clone's mate
				cloneMateData = theContig.getSequenceAt( i + 1 );
				bDrawWithMate = cloneMateData.getLowIndex() > nFirstMateEnd ||
									cloneMateData.getHighIndex() < nFirstMateStart;
			}
		
			if ( !bDrawWithMate )
			{
				if ((nShowBuriedMode != MainToolAlignPanel.HIDE_BURIED_EST) ||
						!cloneData.isBuried()) rowsOfESTS.add( cloneData ); 
			}
			else
			{	
				SequenceData [] ESTPair = new SequenceData[2];
				ESTPair[0] = cloneData;
				ESTPair[1] = cloneMateData;				
				
				if (nShowBuriedMode != MainToolAlignPanel.HIDE_BURIED_EST || !cloneData.isBuried() || 
						!cloneMateData.isBuried()) rowsOfESTS.add( ESTPair );
				++i;
			}
		}		
        // Calculate the pixel dimensions of the grid holding the letters for the bases
        insideHEIGHT = headerHEIGHT + rowsOfESTS.size() * READ_Y_ROW_HEIGHT;          
        if(super.getDrawMode() == GRAPHICMODE)
        {
            insideWIDTH = graph_WIDTH; 
        }
        else
        {
            insideWIDTH = getWriteWidthInt ( ) + base_TITLE_WIDTH + INSET_WIDTH * 2; 
        }
        outsideBox = new Rectangle2D.Double ( LEFT_GAP - 2, TOP_GAP - 2, 
        		insideWIDTH + 4, insideHEIGHT + 4 );

	}
	
	public boolean scrollToMate ( String str )
	{
		JPanel matesPanel = splitPairMap.get ( str );
		if ( matesPanel == null )
			return false;
		else
		{
			UIHelpers.scrollToCenter( matesPanel, false );
			return true;
		}
	}
	
	private String getSequenceName ( SequenceData seq )
	{
		return seq.getName();
	}
	
	public void handleClick( MouseEvent e, Point localPoint )
	{
		// If the click was in the column of names, don't save the new x value
		if ( localPoint.x > base_DATA_X_START_POS )
			nHighlightX = localPoint.x;

        if ( outsideBox.contains(localPoint) ) {
            // Determine the row for the click.
            int nClickRow = (localPoint.y - read_TOP_Y_POS_START) / READ_Y_ROW_HEIGHT;
    		if ( nClickRow >= 0 && nClickRow < rowsOfESTS.size() ){
    			// Clear all selected rows if neither shift nor control are down
    			if ( !e.isControlDown() && !e.isShiftDown() )
    				selectNone();
    			
    			// determine the newly selected row(s)...
    			int nRowLow, nRowHigh;
    			if ( nLastRowClicked != -1 && e.isShiftDown() ) {
    				nRowLow = Math.min ( nClickRow, nLastRowClicked );
    				nRowHigh = Math.max ( nClickRow, nLastRowClicked );
    			}
    			else {
    				nRowLow = nClickRow;
    				nRowHigh = nClickRow;
    
    				// Save the pivot row in case the next one is shift-click
    				nLastRowClicked = nRowLow;
    			}
    			
    			// Add all newly selected ESTs to the set
    			toggleRowSelection ( nRowLow, nRowHigh, e.isControlDown() );
    		}
    		else {	
                // Click is inside the box, but not within any of the sequences (i.e. in the
                // header)
    			if ( e.isControlDown() && !selectedESTs.isEmpty () )
    				// Toggle selection to none
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

	private void toggleRowSelection ( int nRowLow, int nRowHigh, boolean bControlKey )
	{
		String strEST1 = null, strEST2 = null;
		for ( int i = nRowLow; i <= nRowHigh; ++i )
		{
			
			if ( rowsOfESTS.get(i) instanceof SequenceData )
			{
				// Add the single EST in the row
				strEST1 = ((SequenceData)rowsOfESTS.get(i)).getName();
				
				if ( selectedESTs.contains( strEST1 ) && bControlKey )
					selectedESTs.remove( strEST1 );						
				else
					selectedESTs.add( strEST1 );
			}
			else
			{
				// Add both ESTs in the row
				SequenceData[] ESTPair = (SequenceData[])rowsOfESTS.get(i);
				strEST1 = ESTPair[0].getName();
				strEST2 = ESTPair[1].getName();
				
				if ( selectedESTs.contains( strEST1 ) && bControlKey )
				{
					selectedESTs.remove( strEST1 );			
					selectedESTs.remove( strEST2 );									
				}
				else
				{
					selectedESTs.add( strEST1 );
					selectedESTs.add( strEST2 );
				}
			}				
		}
	}
	
	public void changeSortOrder ( int nNewOrder )
	{
		theContig.setSortOrder ( nNewOrder );
		setupRows ( );
		repaint ();
	}
	
	public void selectAll ( )
	{
		nLastRowClicked  = -1;
		toggleRowSelection ( 0, rowsOfESTS.size() - 1, false );
		repaint ();		
	}

	public void selectNone ( )
	{
		nLastRowClicked  = -1;
		selectedESTs.clear();
		repaint ();		
	}
	
	public void getSelectedContigIDs ( TreeSet <String> set ) 
	{
		String str = theContig.getContigID();
		if (str==null || str.length() == 0) return;
		if ( hasSelection ()) set.add( str );	
	};
	
	public void setSelectedContigIDs ( TreeSet <String> set ) 
	{ 
		String str = theContig.getContigID();
		if (str==null || str.length() == 0) selectNone();
		else if (set.contains(str)) selectAll();
		else selectNone();
	};
	
	public boolean hasSelection ()
	{
		return !selectedESTs.isEmpty();
	}
	
	public void setSelectedSequences ( TreeSet <String> toSelect )
	{	
		// Clear the old selection
		selectNone ();
		
		// Intersect the input set, with the displayed sequence (names)
		selectedESTs = new TreeSet<String> ( theContig.getSetOfSequenceNames () );
		selectedESTs.retainAll( toSelect );
		
		// If a selected EST is buried then enable buried display
		for (String s : selectedESTs) {
			for ( int i = 0; i < theContig.getNumSequences (); ++i )
			{
				SequenceData est = theContig.getSequenceAt( i );
				if (s.equals(est.getName()) && est.isBuried()) {
					nShowBuriedMode = MainToolAlignPanel.SHOW_BURIED_EST_DETAIL;
				}
			}
		}
		setupRows();
	}

	private void addMatches(TreeSet<String> matches, String strRegEx) {
		Pattern p = Pattern.compile(strRegEx, Pattern.CASE_INSENSITIVE);
		Matcher m;
		Boolean isZeroLength = (strRegEx.length() == 0); // for speed, any improvement?
		
		for (int i = 0;  i < rowsOfESTS.size();  i++) { 
			String[] ests = getESTsForRow(i);
			for (int j = 0;  j < ests.length;  j++) {
				m = p.matcher(ests[j]);
				if (isZeroLength || m.matches())
					matches.add(ests[j]);
			}
		}
	}

	public void selectMatchingSequences( String strSeqName ) 
	{
		if (strSeqName.length() == 0) {
			selectNone();
			return;
		}
		
		strSeqName = strSeqName.replaceAll("\\*", ".*");
		
		TreeSet<String> matches = new TreeSet<String>();
		
		addMatches(matches, strSeqName);
		if (matches.size() == 0 // no matches
				&& !strSeqName.startsWith(".*") 
				&& !strSeqName.endsWith(".*"))
		{
			addMatches(matches, ".*"+strSeqName+".*"); // search again with wildcards at each end
		}
			
		setSelectedSequences(matches);
	}
	
	public void addSelectedConsensusToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception
	{
		if ( hasSelection() )
		{
			addConsensusToFASTA(seqFile, qualFile); 
		}
	}

	public void addConsensusToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception
	{
		SequenceData refSeq = theContig.getSeqData();
		refSeq = refSeq.newSeqDataNoGap();	
		refSeq.appendToFASTAFiles( seqFile, qualFile );
	}
	
	public void addSelectedSequencesToFASTA ( PrintStream seqFile, PrintStream qualFile ) throws Exception
	{
		Iterator <String> iter = selectedESTs.iterator();
		while ( iter.hasNext() )
		{
			SequenceData curEST = theContig.getSequenceByName( iter.next() );
			curEST = curEST.newSeqDataNoGap(); 
			curEST.appendToFASTAFiles( seqFile, qualFile );				
		}
	}

	public void addAllSequencesToFASTA(PrintStream seqFile, PrintStream qualFile) throws Exception
	{
		int cnt=0;
		for (Object obj : theContig.getAllSequences()) {
			cnt++;
			if ((cnt % 1000) == 0) System.err.println("Wrote " + cnt + " sequences...\r");
			if (obj instanceof SequenceData) {
				SequenceData est = (SequenceData)obj;
				est.newSeqDataNoGap().appendToFASTAFiles( seqFile, qualFile );	
			}
			else if (obj instanceof SequenceData[]) {
				SequenceData[] ests = (SequenceData[])obj;
				ests[0].newSeqDataNoGap().appendToFASTAFiles( seqFile, qualFile );	
				ests[1].newSeqDataNoGap().appendToFASTAFiles( seqFile, qualFile );	
			}
		}
		System.err.println("Complete writing " + cnt + " sequences and quals to file");
	}
	
	public void addSelectedSequencesToSet ( TreeSet <String> set )
	{ 
		set.addAll( selectedESTs );		
	}
	
	public void setDrawMode(int mode) {
		super.setDrawMode(mode);
  		if(super.getDrawMode() == GRAPHICMODE)
  		{
  			super.setMinPixelX ( graph_DATA_X_START_POS );
  		}
    	else
    	{
    		super.setMinPixelX ( base_DATA_X_START_POS );
    	}
		setupRows ( );
		repaint ();		
	}
	
	public void changeDraw()
  	{
		super.changeDraw();
  		if(super.getDrawMode() == GRAPHICMODE)
  		{
  			super.setMinPixelX ( graph_DATA_X_START_POS );
  		}
    	else
    	{
    		super.setMinPixelX ( base_DATA_X_START_POS );
    	}
		setupRows ( );
		repaint ();
  	}
	
	public Dimension getMaximumSize()
	{
		return new Dimension ( Integer.MAX_VALUE, (int)getPreferredSize ().getHeight() );
	}
	
	public Dimension getPreferredSize()
	{
		if(super.getDrawMode() == GRAPHICMODE)
		{
			int nHeight = read_TOP_Y_POS_START + (rowsOfESTS.size()) * 
			READ_Y_ROW_HEIGHT + BOTTOM_GAP;
			return new Dimension( graph_END_X + RIGHT_GAP, nHeight ); 
		}
		else
		{
			int nHeight = read_TOP_Y_POS_START + ( theContig.getNumSequences() ) * 
			READ_Y_ROW_HEIGHT + BOTTOM_GAP;
			return new Dimension( getWriteWidthInt ( ) + base_DATA_X_START_POS + RIGHT_GAP, nHeight); 
		}
	}

	// Constants determining where things get drawn.  Y values are normally baseline values.
	private static final int READ_Y_ROW_HEIGHT = 15;
	private static final int INSET_WIDTH = 5;

	private int basesPerPIXEL = 1; // When we're not showing the sequence letters, each column of 
									// pixels represents this number of sequence letters

	private final int TOP_GAP;
	private final int BOTTOM_GAP;
	private final int LEFT_GAP;
	private final int RIGHT_GAP;
	
    // The dimensions of the box around the region that is drawn
    private int insideWIDTH;
    private int insideHEIGHT;
    private Rectangle2D outsideBox;
    
    private final int WIDEST_CLONE_NAME;
    
    private int headerHEIGHT;
	private int seq_Y_TOP;
	private int seq_Y_BOTTOM;
	private int read_Y_POS_START;
	private int read_TOP_Y_POS_START;
	
	private int base_TITLE_X_START_POS;
	private int base_DATA_X_START_POS;
	private int base_TITLE_WIDTH;
	
	private int graph_COL1_X;
	private int graph_COL2_X;
	private int graph_COL3_X;
	private int graph_DATA_X_START_POS;
	private int graph_ORIG_CONTIG_X;
	private int graph_ORIG_CONTIG_WIDTH;
	private int graph_WIDTH;
	private int graph_END_X;
	
	private final int MATE_NOT_ADJACENT = 0;
	private final int MATE_IS_PREV = 1;
	private final int MATE_IS_NEXT = 2;
	private final int MATE_IN_SAME_ROW_PART_1 = 3;	
	private final int MATE_NOT_IN_CONTIG = 5;
	
	private final String strRefSeqName;
												
	private Graphics2D g2; // what draws stuff

	private TreeSet<String> selectedESTs = new TreeSet<String> (); 
	// Maps the EST name, to its "find EST" panel
	private TreeMap<String,JPanel> splitPairMap = new TreeMap<String,JPanel> (); 
    MultilineTextPanel textPanel;
	private Vector <Object> rowsOfESTS = null; // SequenceData or SequenceData[]
	private int nHighlightX = 0;
	private int nLastRowClicked = -1;
	
	public static final Color contigErr = new Color(255, 230, 230);
	public static final Color contigFor = new Color(230, 230, 255);
	public static final Color contigRev = Globalx.lightGray;
	
	public static final Color colorGreen = new Color ( 0x12B600 );
	public static final Color colorYellow = new Color ( 0xC69E00 );
	
	public static final Color SNPbgColor = new Color ( 0xEEF2FF );
	
	Font theFont = Globals.textFont;
	
	ContigData theContig;
	SequenceData theRefSequence;
	
	boolean bShowSNPs = false;
	int nShowBuriedMode = MainToolAlignPanel.HIDE_BURIED_EST; 

    private static final long serialVersionUID = 1;
}

