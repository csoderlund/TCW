package util.ui;
/***********************************************
 * There are also shared UI static methods in methods.Static
 */
// CAS304 removed applet stuff
import java.awt.*;
import javax.swing.*;
import java.net.URI;

public class UIHelpers 
{ 
    public static void setFrameWaitCursor ( Container c )
    {
        // Set the wait cursor:
        Frame parent = findParentFrame( c );
        if ( parent != null )
            parent.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
    }

    public static void clearFrameCursor ( Container c )
    {
        // Clear the cursor
        Frame parent = findParentFrame( c );
        if ( parent != null )
            parent.setCursor( null );
    }   
    
   
    // scroll the input container to the center of its scroll pane (or two
    // nested scroll panes if bDoubleScrollers is true)
    static public void scrollToCenter ( Container c, boolean bDoubleScrollers )
    { 
        Rectangle rect = new Rectangle ( 0, 0, c.getWidth(), c.getHeight() );
        Container parent = c;
        int nScrolls = 0;
        
        do 
        { 
            if ( parent instanceof JViewport ) 
            {
                JViewport thePort = (JViewport)parent;
                JScrollPane theScroller = (JScrollPane)thePort.getParent();
                
                // Calulate the centered position
                Point pntBefore = new Point ( thePort.getViewPosition() );
                Rectangle scrollTo = new Rectangle (  thePort.getViewRect ( ) );
                if ( theScroller.getHorizontalScrollBarPolicy() != JScrollPane.HORIZONTAL_SCROLLBAR_NEVER )
                    scrollTo.x = (int) ( rect.getCenterX() - scrollTo.getWidth() / 2 );
                if ( theScroller.getVerticalScrollBarPolicy() != JScrollPane.VERTICAL_SCROLLBAR_NEVER )         
                    scrollTo.y = (int) ( rect.getCenterY() - scrollTo.getHeight() / 2 );
                
                // Scroll
                thePort.scrollRectToVisible ( scrollTo );
                ++nScrolls;
                if ( nScrolls == 2 || !bDoubleScrollers )
                    break; // Done
            
                // Adjust for the scroll
                Point pntAfter = thePort.getViewPosition();
                rect.x -= pntAfter.getX() - pntBefore.getX();
                rect.y -= pntAfter.getY() - pntBefore.getY();
            }
            
            // Convert to the parent's coordiates
            rect.x += parent.getX();
            rect.y += parent.getY();
            
            // Get the parent's parent for next time around
            parent = parent.getParent();
            
        } while ( parent != null );

    } 
    
    // Find the parent frame for the input container
    static public Frame findParentFrame( Container c )
    { 
        Frame parent = null;
        while(c != null){ 
          if (c instanceof Frame) 
            parent = (Frame)c; 
          c = c.getParent(); 
        } 
        return parent; 
    } 
    
    static public void setScrollIncrements ( JScrollPane scrollBar )
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); 
        scrollBar.getHorizontalScrollBar().setUnitIncrement( (int)(screenSize.getWidth() / 10) );       
        scrollBar.getVerticalScrollBar().setUnitIncrement( (int)(screenSize.getHeight() / 10) );        
    }
   
    //   centers the input window on the screen
    static public void centerScreen( Window win ) 
    {
          Dimension dim = win.getToolkit().getScreenSize();
          Rectangle abounds = win.getBounds();
          win.setLocation((dim.width - abounds.width) / 2,
              (dim.height - abounds.height) / 2);
    }
  
    //   centers the input window relative to its parent
    static public void centerParent ( Window win ) 
    {
          int x;
          int y;
        
          Container myParent = win.getParent();
          
          // Center using the parent if it's visible, just use the
          // screen otherwise
          if ( myParent.getWidth() > 0 && myParent.getHeight() > 0 )
          { 
                Point topLeft = myParent.getLocationOnScreen();
                Dimension parentSize = myParent.getSize();
            
                Dimension mySize = win.getSize();
            
                if (parentSize.width > mySize.width) 
                    x = ((parentSize.width - mySize.width)/2) + topLeft.x;
                else 
                    x = topLeft.x;
               
                if (parentSize.height > mySize.height) 
                    y = ((parentSize.height - mySize.height)/2) + topLeft.y;
                else 
                    y = topLeft.y;
               
                win.setLocation (x, y);
          }
          else
                centerScreen ( win );
    }  
	// this is only used from MultilineTextPanel for SeqDetailPanel (which is no called for Mac)
	public static boolean tryOpenURL ( URI theLink ) // CAS405 URL->URI
    {
		// Otherwise unless we become a web start application
    	// we are stuck with the below...  I copied this from: http://www.centerkey.com/java/browser/.
    	String osName = System.getProperty("os.name"); 
    	try 
    	{ 
    		if (osName.startsWith("Mac OS")) // CAS304 this does not work, and is no used for Mac
    		{ 
    			//Class<?> fileMgr = Class.forName("com.apple.eio.FileManager"); 
    			//Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class}); 
    			//openURL.invoke(null, new Object[] {theLink}); 
    			return false;
    		} 
    		else if (osName.startsWith("Windows")) 
    		{
    			String str = "rundll32 url.dll,FileProtocolHandler " + theLink;
    			String [] x = str.split("\\s+");
    			Runtime.getRuntime().exec(x); // CAS405 add split
    			return true;
    		}
    		else 
    		{ 
    			//assume Unix or Linux 
    			String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" }; 
    			String browser = null; 
    			for (int count = 0; count < browsers.length && browser == null; count++) 
    				if (Runtime.getRuntime().exec( new String[] {"which", browsers[count]}).waitFor() == 0) 
    					browser = browsers[count]; 
    			if (browser == null) 
    				return false;
    			else 
    			{
    				Runtime.getRuntime().exec(new String[] {browser, theLink.toString()});
    				return true;
    			}
    		}
    	}
    	catch (Exception e) 
    	{ 	
    		e.toString();
    	}
	    	
		return false;
    }
}
