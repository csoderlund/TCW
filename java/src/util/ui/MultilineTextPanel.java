package util.ui;
import util.methods.ErrorReport;

import java.awt.event.MouseEvent;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Vector;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.net.URI;
import java.awt.event.MouseListener;

/***********************************************
 * inTextLines[i]: if text contains \n, then second part is URL
 * usually its just used for text lines.
 */
public class MultilineTextPanel extends JPanel
{
    public MultilineTextPanel ( Font inFont, Vector<String> inTextLines, 
    		int nInInset, int nInWidth, int nMinColumns ) throws Exception
    {
       try {
	   		FontMetrics metrics = getFontMetrics( inFont );
	   		setLayout ( null );
    
	   		nWidth = nInWidth;
	   		nInset = nInInset;
	   		textLines = new String [ inTextLines.size() ];
	   		htmlLines = new URI [ inTextLines.size() ];
	   		nColumnSpace = nInInset * 3;
	   		int nMaxColumnWidth = ( nWidth - nInset * 2 + nColumnSpace ) / nMinColumns - nColumnSpace;
        
	        // Go through the list of lines to
	        // 1) Determine the column width
	        // 2) Parse any links off of the input text lines
	        nColumnWidth = 0;
	        for ( int i = 0; i < inTextLines.size(); ++i )
	        {
		        textLines [i] = null;
		        htmlLines [i] = null;
        	
	            String str = inTextLines.get(i);
	            if ( str == null || str.length() == 0 ) continue;
	            
	            String [] strList = str.split( "\n" );
	            textLines [i] = strList[0];
	            if ( strList.length > 1 )
	            		htmlLines [i] = new URI ( strList[1] ); // CAS405 URL->URI
	            
	            nColumnWidth = Math.max( metrics.stringWidth( textLines [i] ), nColumnWidth );
	            nColumnWidth = Math.min( nColumnWidth, nMaxColumnWidth );
	        }
        
	        // force single column layout
	        nRows = textLines.length;
	        nColumns = 1;
	        
	        // Determine the height        
	        nRowHeight = metrics.getHeight();     
	        nHeight = nRows * nRowHeight + 2 * nInset;

	        // Now that we know the number of rows and columns, lay out the control
	        for ( int i = 0; i < nColumns; ++i ) {
	            for ( int j = 0; j < nRows; ++j )
	            {
	                float fX = nInset + i * (nColumnWidth + nColumnSpace);
	                float fY = nInset + j * nRowHeight;
	                
	                int nIdx = j + (i * nRows);
	                if ( nIdx >= textLines.length )
	                    break;
	                
	                String str = textLines[nIdx];
	                if ( str==null || str.length()==0 ) continue;
	                    
	                // Create a label to display the text and handle the link/tool-tip
	                JLabel label = createLabel ( textLines [nIdx], htmlLines[nIdx] );
	                
	                label.setFont( inFont );
	                label.setLocation( (int)fX, (int)fY );
	                Dimension size = new Dimension ( nColumnWidth, nRowHeight );
	                label.setSize( size );
	                label.setPreferredSize( size );
	                add ( label );
	            }  
	        }
	        // Set the boundaries of the base class panel
	        Dimension dim =  new Dimension ( nWidth, nHeight );
	        setSize ( dim );
	        setPreferredSize( dim ); 
       }
       catch (Exception e) {ErrorReport.reportError(e, "Internal error in MultilineTextPanel");}
    }
    
    private JLabel createLabel ( String strText, final URI theLink )
    { 	
	    	JLabel label = null;
	    	
	    	if ( theLink != null )
	    	{
	    		final Color linkColor = Color.BLUE;
	    		
	    		label = new JLabel ( strText )
	    		{
	    			// Convoluted work-around to make Java underline text...
			    	public void paint(Graphics g)
			        {
			            super.paint(g);
		     
		                 // really all this size stuff below only needs to be recalculated if font or text changes
		                Rectangle2D textBounds =  getFontMetrics(getFont()).getStringBounds(getText(), g);
		                
		                 //this layout stuff assumes the icon is to the left, or null
		                int y = getHeight()/2 + (int)(textBounds.getHeight()/2);
		                int w = (int)textBounds.getWidth();
		                int x = (getIcon()==null ? 0 : getIcon().getIconWidth() + getIconTextGap()); 
		     
		                g.setColor(linkColor);
		                g.drawLine(0, y, x + w, y);
			        }
			    	
			        private static final long serialVersionUID = 1;
	    		};
	    		
	    		label.addMouseListener
	    		(	new MouseListener ()
	    			{
	    				public void mouseClicked(MouseEvent e)
	    				{
	    					// Opens the link
	    					UIHelpers.tryOpenURL( theLink );
	    				}
	    				public void mouseEntered(MouseEvent e)
	    				{
				    		// Changes the cursor to a hand if a valid link was passed into the constructor
				    	    setCursor( new Cursor(Cursor.HAND_CURSOR) );
	    				}
	    				public void mouseExited(MouseEvent e)
	    				{
			    			// Change back the cursor
			    			setCursor( new Cursor(Cursor.DEFAULT_CURSOR) );
	    				}		
	    				public void	mousePressed(MouseEvent e) { };
	    				public void	mouseReleased(MouseEvent e) { };
	    			}
	    		);
	    		label.setForeground( linkColor );
	    	}
	    	else
	    	{
	    		label = new JLabel ( strText );
	    	}
	    	label.setToolTipText( strText );
	    	
	    	return label;
    }
    
    private String textLines [] = null;
    private URI htmlLines [] = null;
    private int nColumnSpace;
    private int nRowHeight;
    private int nColumnWidth;
    private int nColumns;
    private int nRows;
    private int nWidth;
    private int nHeight;
    private int nInset;    
    
    private static final long serialVersionUID = 1;

}
