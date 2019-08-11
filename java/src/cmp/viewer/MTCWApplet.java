package cmp.viewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JApplet;
import javax.swing.JOptionPane;

import cmp.database.Globals;

import util.database.DBConn;
import util.database.HostsCfg;

public class MTCWApplet extends JApplet {
	private static final long serialVersionUID = -2952501895997109083L;

	public void start() {
		theDBName = getParameter("ASSEMBLY_DB");
		theHost = getParameter("DB_URL");
		theDBUser = getParameter("DB_USER");
		theDBPass = getParameter("DB_PASS");
		
		new HostsCfg(theHost, theDBUser, theDBPass);
		if (!DBConn.checkMysqlServer(theHost, theDBUser, theDBPass))
		{
			JOptionPane.showMessageDialog(null, 
					"Unable to connect to MySQL database with URL " + theHost);
			System.exit(0);
		}
		
		openAssembly();
	}
	
	public void openAssembly ( )
	{
		if ( theFrame != null )
			theFrame.toFront();
		else
		{
			System.out.println("Opening multiTCW applet " + Globals.VERSION + " " + Globals.VDATE + ": " + theDBName);
			theFrame = new MTCWFrame ( theHost, theDBName, true, theDBUser, theDBPass );
			theFrame.setVisible(true);

			theFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					MTCWFrame.shutDownFromApplet();
				}

				public void windowClosed(WindowEvent e) {
					stop();
				}
			});	
		}
	}
	
	public void stop() 
	{
		System.out.println("Closing database: " + theDBName);
		theFrame = null;
		destroy();
		System.exit(0);
	}
	
	private MTCWFrame theFrame = null;
	private String theHost = null;
	private String theDBName = null;
	private String theDBUser = null;
	private String theDBPass = null;
}
