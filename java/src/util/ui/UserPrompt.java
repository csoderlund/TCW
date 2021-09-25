package util.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import util.methods.ErrorReport;
import util.methods.Out;

public class UserPrompt extends JDialog {
	private static final long serialVersionUID = 1L;
	public static final Color PROMPT = new Color(0xEEFFEE);
	
	// none of the following are used: dialog.dispose() closes it
	public static JDialog calcPopup(JFrame parentFrame, String msg) {
		String message = "Calculating " + msg + "\nPlease wait...";
        JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(parentFrame, "Calculating....");
        dialog.setModal(false);
        dialog.setVisible(true);
        return dialog;
	}
	
	public static void displayInfo(String title, String [] message, boolean isModal) {
		displayInfo(null, title, message, isModal);
	}
	public static void displayInfo(String title, String [] message) {
		displayInfo(null, title, message, false);
	}
	public static void displayInfo(JFrame parentFrame, String title, String [] message, boolean isModal) {
		JOptionPane pane = new JOptionPane();
		pane.setMessage(message);
		pane.setMessageType(JOptionPane.PLAIN_MESSAGE);
		JDialog helpDiag = pane.createDialog(parentFrame, title);
		helpDiag.setModal(isModal);
		helpDiag.setVisible(true);		
	}
	/*******************************************************/
	// line-delimited string, does not use parentFrame, modal=false
	public static void displayInfoMonoSpace(Component parentFrame, String title, String theMessage) {
		if (parentFrame==null) 
			 displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, false, false);
	}
	// string array, does not use parentFrame, modal=false
	public static void displayInfoMonoSpace(Component parentFrame, String title, String [] message) {
		String theMessage = "";
		for(int x=0; x<message.length; x++)
			theMessage += message[x] + "\n";
		
		if (parentFrame==null) displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, false, false);
	}
	
	// string array, user sets sizeToParent and isModal
	public static void displayInfoMonoSpace(Component parentFrame, String title, 
			String [] message, boolean isModal, boolean sizeToParent) {
		
		String theMessage = "";
		for(int x=0; x<message.length; x++) theMessage += message[x] + "\n";
		
		if (parentFrame==null) 
			 displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, isModal, sizeToParent);
	}
	/***********************************************************************
	// the 3 above call this
	 * @param parentFrame - may be null, in which case sizeToParent and halfWidth are ignoreed
	 * @param title
	 * @param theMessage
	 * @param isModal - true means that everything is frozen until the window is closed
	 * @param sizeToParent if
	 */
	private static void displayInfoMonoSpace(Component parentFrame, String title, 
			String theMessage, boolean isModal, boolean sizeToParent) {
		JOptionPane pane = new JOptionPane();
		
		JTextArea messageArea = new JTextArea(theMessage);

		JScrollPane sPane = new JScrollPane(messageArea); 
		messageArea.setFont(new Font("monospaced", Font.BOLD, 12));
		messageArea.setEditable(false);
		messageArea.setSelectionColor(Color.gray); // CAS313 makes text selectable and copy
		messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		pane.setMessage(sPane);
		pane.setMessageType(JOptionPane.PLAIN_MESSAGE);

		JDialog helpDiag = pane.createDialog(parentFrame, title);
		helpDiag.setModal(isModal);
		helpDiag.setResizable(true);
		
		if (parentFrame != null && sizeToParent) {
			int w = parentFrame.getWidth();  
			int h = parentFrame.getHeight();  
			if (helpDiag.getWidth() >= w || helpDiag.getHeight() >= h) {	
				Dimension d = new Dimension (w, h);
				helpDiag.setSize(d);
			}
		}
		helpDiag.setVisible(true);		
	}
	/********************************************************************
	 * CAS333 add
	 * @param parentFrame - if supplied, parentframe will not go in front of popup
	 * @param title
	 * @param [] message
	 * @param w	width
	 * @param h	height
	 */
	public static void displayInfoMonoSpace(Component parentFrame, String title, String [] message,  int w, int h) {
		String theMessage = "";
		for(int x=0; x<message.length; x++) theMessage += message[x] + "\n";
		
		JOptionPane pane = new JOptionPane();
		JTextArea messageArea = new JTextArea(theMessage);

		JScrollPane sPane = new JScrollPane(messageArea); 
		messageArea.setFont(new Font("monospaced", Font.BOLD, 12));
		messageArea.setEditable(false);
		messageArea.setSelectionColor(Color.gray); // CAS313 makes text selectable and copy
		messageArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		pane.setMessage(sPane);
		pane.setMessageType(JOptionPane.PLAIN_MESSAGE);

		JDialog helpDiag = pane.createDialog(parentFrame, title);
		helpDiag.setModal(false);	// do not freeze other windows
		helpDiag.setResizable(true);
		
		Dimension d = new Dimension (w, h);
		helpDiag.setSize(d);
		
		helpDiag.setVisible(true);		
	}
	/*******************************************************/
	public static void displayHTML(String title, String [] text) {
		displayHTML(title, text, false);
	}
	public static void displayHTML(String title, String [] text, boolean isModel) {
		try {
			JEditorPane pane = new JEditorPane ();
			pane.setContentType("text/html");
			
			String theText = "";
			for(int x=0; x<text.length; x++)
				theText += text[x] + "\n";
			
			pane.setText(theText);
			pane.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
			pane.setMaximumSize(pane.getPreferredSize());
			pane.setMinimumSize(pane.getPreferredSize());
			
			JScrollPane sPane = new JScrollPane(pane);
			sPane.setMinimumSize(pane.getPreferredSize());
			
			JDialog textDiag = new JDialog();
			textDiag.setBackground(Color.WHITE);
			textDiag.setModal(isModel);
			textDiag.add(sPane, BorderLayout.CENTER);
			textDiag.pack();
			
			textDiag.setLocationRelativeTo(null); 
			textDiag.setTitle(title);
			textDiag.setVisible(true);
		}
		catch(Exception ex) {
			ErrorReport.prtReport(ex, "Error with display HTML");
		}
	}
	
	/*******************************************************/
	public static void displayHTMLResourceHelp(Component parentFrame, String title, String urlstr) {
		try {
			if (!urlstr.startsWith("/")) urlstr = "/" + urlstr;
			TCWEditorPane hlpPane = new TCWEditorPane(UserPrompt.class.getResource(urlstr));
	        
			hlpPane.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
			hlpPane.setMaximumSize(hlpPane.getPreferredSize());
			hlpPane.setMinimumSize(hlpPane.getPreferredSize());
			
			JScrollPane sPane = new JScrollPane(hlpPane);
			sPane.setMinimumSize(hlpPane.getPreferredSize());
			JDialog hlpDiag = new JDialog();
			hlpDiag.setBackground(Color.WHITE);
			hlpDiag.setModal(false);
			hlpDiag.add(sPane, BorderLayout.CENTER);
			hlpDiag.pack();
			
			if (parentFrame!=null) {
				Dimension d = hlpPane.getPreferredSize();
				Dimension dmax = parentFrame.getPreferredSize();
				d.width += 100;
				d.height += 100;
				if (d.height > dmax.height) d.height = dmax.height;
				hlpDiag.setSize(d);
			}
			hlpDiag.setLocationRelativeTo(null); // center
			hlpDiag.setTitle(title);
			hlpDiag.setVisible(true);
		}
		catch(Exception ex) {
			ErrorReport.prtReport(ex, "Error loading manager help: " + urlstr);
		}
	}
	
	/*******************************************************/
	static public void showError (String msg) {
		JOptionPane.showMessageDialog(null, 
				msg, "Error", JOptionPane.PLAIN_MESSAGE);
	}
	static public void showWarn (String msg) {
		JOptionPane.showMessageDialog(null, 
				msg, "Warning", JOptionPane.PLAIN_MESSAGE);
	}
	static public void showMsg (String msg) {
		JOptionPane.showMessageDialog(null, 
				msg, "Message", JOptionPane.PLAIN_MESSAGE);
	}
	
	static public void showError (Component c, String msg) {
		JOptionPane.showMessageDialog(c, 
				msg, "Error", JOptionPane.PLAIN_MESSAGE);
	}
	static public void showWarn (Component c,String msg) {
		JOptionPane.showMessageDialog(c, 
				msg, "Warning", JOptionPane.PLAIN_MESSAGE);
	}
	static public void showMsg (Component c,String msg) {
		JOptionPane.showMessageDialog(c, 
				msg, "Message", JOptionPane.PLAIN_MESSAGE);
	}
	// No is cancel
	static public boolean showConfirm (String title, String msg) {
		String [] options = {"Confirm", "Cancel"};
		int ret = JOptionPane.showOptionDialog(null, 
				msg,
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
	static public boolean showContinue (String title, String msg) {
		String [] options = {"Continue", "Cancel"};
		int ret = JOptionPane.showOptionDialog(null, 
				msg + "\nContinue?",
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
	static public boolean showContinue (Component c,String title, String msg) {
		String [] options = {"Continue", "Cancel"};
		int ret = JOptionPane.showOptionDialog(c, 
				msg + "\nContinue?",
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
	static public boolean showYesNo (String title, String msg) {
		String [] options = {"Yes", "No"};
		int ret = JOptionPane.showOptionDialog(null, msg,
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
	static public boolean showOptions (String [] options, String title, String msg) { // CAS330 changed name
		int ret = JOptionPane.showOptionDialog(null, msg,
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
}
