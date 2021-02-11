package util.file;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

/********************************************************
 * Popup that asks (1) Display information in popup (2) Export information to file
 * CAS316 created - was in UserPrompt, added there CAS313
 * 	if (p.cont()) {
 * 		make vector
 * 		doOutAction(vector)
 *  }
 */
public class FileOrPop  extends JDialog {
	private static final long serialVersionUID = 1L;
	
	public FileOrPop(JFrame theMainFrame, String msg) {
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
