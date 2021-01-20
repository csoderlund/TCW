package util.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;

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
	// send line-delimited string or array of strings
	public static void displayInfoMonoSpace(Component parentFrame, String title, String theMessage) {
		if (parentFrame==null) 
			 displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, false, false);
	}
	
	public static void displayInfoMonoSpace(Component parentFrame, String title, String [] message) {
		String theMessage = "";
		for(int x=0; x<message.length; x++)
			theMessage += message[x] + "\n";
		
		if (parentFrame==null) 
			 displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, false, false);
	}
	
	public static void displayInfoMonoSpace(Component parentFrame, String title, 
			String [] message, boolean isModal, boolean sizeToParent) {
		
		String theMessage = "";
		for(int x=0; x<message.length; x++) theMessage += message[x] + "\n";
		
		if (parentFrame==null) 
			 displayInfoMonoSpace(null, title, theMessage, false, false);
		else displayInfoMonoSpace(parentFrame, title, theMessage, isModal, sizeToParent);
	}
	// the three above call this
	//isModal=true means that everything is frozen until the window is closed
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
		
		if(sizeToParent && parentFrame != null && (helpDiag.getWidth() >= parentFrame.getWidth() || helpDiag.getHeight() >= parentFrame.getHeight()))
				helpDiag.setSize(parentFrame.getSize());
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
	static public boolean showYesNo (String [] options, String title, String msg) {
		int ret = JOptionPane.showOptionDialog(null, msg,
				title, JOptionPane.YES_NO_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (ret == JOptionPane.NO_OPTION) return false;
		return true;
	}
	/*************************************************************
	 * General class for requesting popup - CAS313 add
	 * UserPrompt p = UserPrompt(theMainFrame, title-filename)
	 * if (p.cont()) {
	 * 		make vector
	 * 		doOutAction(vector)
	 * }
	 */
	public UserPrompt(JFrame theMainFrame, String msg) {
		this.theMainFrame = theMainFrame;
		
    	setModal(true);
    	setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    	setTitle(msg);
    		
    	JRadioButton btnPop = Static.createRadioButton("Show information in popup", true);
    	btnPop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nMode = 1;
			}
		});	
    	JRadioButton btnAll = Static.createRadioButton("Export all information to file", false);
        btnAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                nMode = 2;
            }
        });
        JButton btnOK = Static.createButton("OK", true);
			btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		JButton btnCancel = Static.createButton("Cancel", true);
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nMode=0;
				setVisible(false);
			}
		});
		
		btnOK.setPreferredSize(btnCancel.getPreferredSize());
		btnOK.setMaximumSize(btnCancel.getPreferredSize());
		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		
    	ButtonGroup grp = new ButtonGroup();
    	grp.add(btnPop);
        grp.add(btnAll); 
          
    	JPanel selectPanel = Static.createPagePanel();
    	selectPanel.add(btnPop);
    	selectPanel.add(Box.createVerticalStrut(5));
    	selectPanel.add(btnAll);
        selectPanel.add(Box.createVerticalStrut(5));
        
    	JPanel buttonPanel = Static.createRowPanel();
    	buttonPanel.add(btnOK);
    	buttonPanel.add(Box.createHorizontalStrut(20));
    	buttonPanel.add(btnCancel);
    	buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());

       	JPanel mainPanel = Static.createPagePanel();
    	mainPanel.add(selectPanel);
    	mainPanel.add(Box.createVerticalStrut(15));
    	mainPanel.add(buttonPanel);
    		
    	mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    	add(mainPanel);
    		
    	pack();
    	this.setResizable(false);
    	UIHelpers.centerScreen(this);
	}
	public boolean getAction(String dir, String fileName) {
		try {
			if (nMode==0) return false;
			if (nMode==1) return true;
			if (nMode==2) return getFileHandle(dir, fileName + ".txt");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in selection");}
		return false;
	}
	/**
	 * Either writes to 1. pop-up or 2. lines to file
	 * Over 2000 lines can hang VNC - see MultiAlignPanel for a merge method
	 */
	public void doOutAction(String [] alines) {
		try {
			// Popup
			if (nMode==1) { 
				UserPrompt.displayInfoMonoSpace(null, fileName, alines);
				return;
			}
			// Write to file
			if (exportFH==null) {
				JOptionPane.showMessageDialog(null, "TCW error: file is null");
				return;
			}
			Out.PrtSpMsg(0, "Writing " + exportFH.getName() + " ...");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportFH)));
			int cnt=0;
			
			for (String l : alines) {
				pw.println(l);
				cnt++;
			}
			pw.close();
			Out.PrtSpMsg(0, "Wrote " + cnt + " lines");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in seleciton"); return;}
		
	}
	public boolean getFileHandle(String dirName, String fileName) {
		try {
			File nDir = new File(dirName);
			if (!nDir.exists()) if (nDir.mkdir()) Out.prt("Create " + dirName);
	
			final JFileChooser fc = new JFileChooser(dirName);
			fc.setSelectedFile(new File(fileName));
			
			if (fc.showSaveDialog(theMainFrame) != JFileChooser.APPROVE_OPTION) return false;
			if (fc.getSelectedFile() == null) return false;
			
			final File f = fc.getSelectedFile();
			final File d = f.getParentFile();
			if (!d.canWrite()) { 
				JOptionPane.showMessageDialog(null, 
						"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
			}
			else {
				int writeOption = JOptionPane.YES_OPTION;
				if (f.exists()) {
					writeOption = JOptionPane.showConfirmDialog(theMainFrame,
						    " The file '" + f.getName() + "' already exists. \nOverwrite it?", "Save to File",
						    JOptionPane.YES_NO_OPTION);
				}
				if (writeOption==JOptionPane.YES_OPTION) {
					exportFH = f;
					return true;
				}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in get file selection"); }
		return false;
	}
	public boolean isPopUp() {return (nMode==1);}
	
	private JFrame theMainFrame=null;
	private int nMode=1; // 1=popup, 2=write all, 0 = cancel
	private File exportFH=null;
	private String fileName="";
}
