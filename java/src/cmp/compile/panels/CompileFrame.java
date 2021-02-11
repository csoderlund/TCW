package cmp.compile.panels;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import util.database.HostsCfg;
import util.file.FileHelpers;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.TCWprops;
import util.ui.UIHelpers;
import cmp.compile.runMTCWMain;
import cmp.database.Globals;

public class CompileFrame extends JFrame {
	private static final long serialVersionUID = -3595052029641659355L;
	/***************************************
	 * runMultiTCW -- called from CompileMain
	 */
	public CompileFrame(runMTCWMain parent) {
		theParent = parent;
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		setWindowSettings("runMultiTCW");
		setHosts();
		runMultiTCW();
	}
	
	private void runMultiTCW() {
		Vector<String> hostNames = new Vector<String>();
		hostNames.add(hostsObj.host());
		setTitle("runMultiTCW " + Globalx.strTCWver); 
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		runMultiPanel = new CompilePanel(this, hostNames, hostsObj);
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				runMultiPanel.resizedEvent(e);
			}
		});
		pack();
	}
	
	public runMTCWMain getParentMain() { return theParent; }
	
	public CompilePanel getCompilePanel() {
		if (runMultiPanel == null) ErrorReport.die("Creating the main panel");

		return runMultiPanel;
	}
	private void setWindowSettings(final String prefix) {
		// Load window dimensions from preferences
		Preferences userPrefs = Preferences.userRoot();
		int nX = userPrefs.getInt(prefix + "_frame_win_x", Integer.MAX_VALUE);
		int nY = userPrefs.getInt(prefix + "_frame_win_y", Integer.MAX_VALUE);
		int nWidth = userPrefs.getInt(prefix + "_frame_win_width", Integer.MAX_VALUE);
		int nHeight = userPrefs.getInt(prefix + "_frame_win_height", Integer.MAX_VALUE);
		if (nX == Integer.MAX_VALUE) {
			UIHelpers.centerScreen(this);
		} else
			setBounds(nX, nY, nWidth, nHeight);

		// Setup to save window dimensions when it is closed
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					// Save the window dimensions to the preferences
					Preferences userPrefs = Preferences.userRoot();
					userPrefs.putInt(prefix + "_frame_win_x", getX());
					userPrefs.putInt(prefix + "_frame_win_y", getY());
					userPrefs.putInt(prefix + "_frame_win_width", getWidth());
					userPrefs.putInt(prefix + "_frame_win_height", getHeight());
					userPrefs.flush();

				} catch (Exception err) {
					ErrorReport.reportError(err, "Error: initializing");
				}
			}
		});
	}
	private void setHosts() {
		if (blastIsSet) return;
		blastIsSet = true;
		try {
			new File("HOSTS.cfg");
			hostsObj = new HostsCfg();
			
			// Blast path gets checked in HostsCfg
			// Check muscle/muscle, mafft, mstat
			String cmdPath = TCWprops.getExtDir();
			if (!FileHelpers.existDir(cmdPath)) {
				Out.PrtError("directory does not exists: " + cmdPath);
				return;
			}
			checkExternal(cmdPath + Globals.Ext.mafftExe);
			checkExternal(cmdPath + Globals.Ext.muscleExe);
			checkExternal(cmdPath + Globals.Ext.mstatxExe);
			checkExternal(cmdPath + Globals.Ext.kaksExe);
			checkExternal(cmdPath + Globals.Ext.orthoTryExe);
		}
		catch(Exception e) {System.err.println("Error reading HOSTS.cfg"); }

	}
	private void checkExternal(String filePath) {
		try {
			if (!FileHelpers.fileExists(filePath))
				Out.PrtError("file does not exists: " + filePath);
			else if (!FileHelpers.fileExec(filePath))
				Out.PrtError("file is not executable: " + filePath);
		}
		catch(Exception e) {System.err.println("Check External"); }
	}
	private runMTCWMain theParent = null;
	private boolean blastIsSet = false;
	
	private CompilePanel runMultiPanel = null;
	
	private HostsCfg hostsObj=null;
}
