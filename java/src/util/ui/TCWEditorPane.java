package util.ui;
/***********************************************
 * Displays the HTML Help window. Called from cmp.viewer.MTCWFrame.SytelText.Panel
 * and sng.util.SytleTextTab for overview
 * This will not work with URLs, i.e. do not put URLs in Overview
 * CAS402 was used by UserPrompt, but now UserPrompt takes care of this
 */
import java.awt.Desktop;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class TCWEditorPane extends javax.swing.JEditorPane  {
	private static final long serialVersionUID = -3196882292033564654L;

	public TCWEditorPane(URL linkURL) throws Exception {
		super(linkURL);
		setEditable(false);
		
		addHyperlinkListener(new HyperlinkListener() {
		    public void hyperlinkUpdate(HyperlinkEvent e) {
		        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	        		try {
	        			String ustr = e.getDescription();
	        			if (ustr.startsWith("http://")) {
				        	if(Desktop.isDesktopSupported()) 
				        		Desktop.getDesktop().browse(e.getURL().toURI());		        				
	        			}
	        			else {
	        				URL url = TCWEditorPane.class.getResource(ustr);
	        				setPage(url);
	        			}
	        		}
	        		catch(Exception eee) {
	        			System.err.println("Cannot open link");
	        			System.err.println(eee.getMessage());
	        		}
		        }
		    }
		});
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;

		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		super.paintComponent(g2);
	}
}