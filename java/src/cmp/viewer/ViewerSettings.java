package cmp.viewer;
/*******************************************************
 * Stores columns in preferences. 
 * It does NOT store by database, so  if the dynamic columns get changed, 
 *    they only work for the databases that have them.
 * For example, if mTCW_x has datasets x1 and x2 and they are selected,
 *   and mTCW_xx also has x1 and x2, they will be shown. 
 * But if mTCW_y with dataset y1 and y2 are shown and selected, 
 *   then next time mTCW_x is displayed, they x1 and x2 will no longer be shown.
 */
import java.awt.Color;
import java.awt.Font;
import java.util.prefs.Preferences;

import cmp.database.Globals;


import util.methods.ErrorReport;

public class ViewerSettings {
	static final String DEFAULT_NAME = "mTCW"; 
	
	public ViewerSettings(MTCWFrame parentFrame) {
		try {
			theParentFrame = parentFrame;
			setPreferences();

			strProfileName = getCurrentProfileName();
			if(strProfileName == null || strProfileName.length() == 0) {
				setCurrentProfileName(DEFAULT_NAME);
				setProfileToDefault();
				String [] tempList = new String[1];
				tempList[0] = strProfileName;
				setProfileNames(tempList);
			}
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error loading preferences");}
		catch(Error e) {ErrorReport.reportFatalError(e, "Fatal Error loading preferences", theParentFrame);}		
	}
	
	private boolean setPreferences() {
		try {
			thePrefs = Preferences.userNodeForPackage(getClass());
			return true;
		}
		catch (Exception e) {
			try {
				thePrefs = Preferences.userNodeForPackage(getClass());
				return true;
			}
			catch (Exception e2) {
				ErrorReport.prtReport(e2, "Error loading preferences - restart viewMultiTCW");
			}
		}
		return false;
	}
	//Called when ran for this first time, or when the user resets back to default
	public void setProfileToDefault() {
		try {
			FrameSettings fs = new FrameSettings();
			fs.setBGColor(Globals.BGCOLOR);
			fs.setDefaultFont(new Font("Monospaced", Font.PLAIN, 11)); // changing this make no difference
			fs.setFrameHeight(650); // CAS310 change from 600
			fs.setFrameWidth(1000);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error setting defaults");}
		catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error setting defaults", theParentFrame);}		
	}
	
	public String getCurrentProfileName() {
		try {
			return thePrefs.get("CURRENTPROFILE", "");
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error loading profile");}
		catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error loading profile", theParentFrame);}		
		return null;
	}
	
	public void setCurrentProfileName(String name) {
		try {
			thePrefs.put("CURRENTPROFILE", name);
			strProfileName = name;
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error saving profile name");}			
	}
	
	public void setProfileNames(String [] names) {
		try {
			String storeVal = "";
			if(names != null && names.length > 0) {
				storeVal = names[0];
				for(int x=1; x<names.length; x++) storeVal += ";" + names[x];
			}
			thePrefs.put("PROFILELIST", storeVal);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error saving profile names");}		
	}
	
	public FrameSettings getFrameSettings() { return new FrameSettings(); } 
	public GrpSettings getGrpSettings() { return new GrpSettings(); } 
	public PairSettings getPairSettings() { return new PairSettings(); } 
	public MemberSettings getMemberSettings() { return new MemberSettings(); }
	public SeqSettings getSeqSettings() { return new SeqSettings(); } 
	public HitSettings getHitSettings() { return new HitSettings(); } 
	
	//Used when a new profile is loaded
	public void setProfileName(String name) { strProfileName = name; }
	public String getProfileName() { return strProfileName; }
	
	public class FrameSettings {
		public Color getBGColor()    { return getColor("BGCOLOR"); }
		public Font getDefaultFont() { return getFont("DEFAULTFONT"); }
		public int getFrameWidth()   { return getInt("FRAMEWIDTH"); }
		public int getFrameHeight()  { return getInt("FRAMEHEIGHT"); }
		
		public void setBGColor(Color background) { putColor("BGCOLOR", background); }
		public void setDefaultFont(Font f) { putFont("DEFAULTFONT", f); } 
		public void setFrameWidth(int width) { putInt("FRAMEWIDTH", width); } 
		public void setFrameHeight(int height) { putInt("FRAMEHEIGHT", height); } 
	}

	public class GrpSettings {
		public String [] getSelectedColumns() { return getString("GRPCOLUMNS").split(","); }
		
		public void setSelectedColumns(String [] columns) {
			String storeVal = columns[0];
			for(int x=1; x<columns.length; x++) storeVal += "," + columns[x];
			putString("GRPCOLUMNS", storeVal);
		}
	}
	public class PairSettings {
		public String [] getSelectedColumns() { return getString("PAIRCOLUMNS").split(","); }
		
		public void setSelectedColumns(String [] columns) {
			if (columns.length>0) {
				String storeVal = columns[0];
				for(int x=1; x<columns.length; x++) storeVal += "," + columns[x];
				putString("PAIRCOLUMNS", storeVal);
			}
		}
	}
	public class MemberSettings {
		public String [] getSelectedColumns() { return getString("MEMBERCOLUMNS").split(","); }
		
		public void setSelectedColumns(String [] columns) {
			if (columns.length>0) {
				String storeVal = columns[0];
				for(int x=1; x<columns.length; x++) storeVal += "," + columns[x];
				putString("MEMBERCOLUMNS", storeVal);
			}
		}
	}
	
	public class SeqSettings {
		public String [] getSelectedColumns() { 
			return getString("SEQCOLUMNS").split(","); 
		}
		
		public void setSelectedColumns(String [] columns) {
			if (columns.length>0) {// CAS303 else crash on columns[0]
				String storeVal = columns[0];
				for(int x=1; x<columns.length; x++) storeVal += "," + columns[x];
				putString("SEQCOLUMNS", storeVal);
			}
		}
	}
	public class HitSettings {
		public String [] getSelectedColumns() { 
			return getString("HITCOLUMNS").split(","); 
		}
		
		public void setSelectedColumns(String [] columns) {
			if (columns.length>0) {
				String storeVal = columns[0];
				for(int x=1; x<columns.length; x++) storeVal += "," + columns[x];
				putString("HITCOLUMNS", storeVal);
			}
		}
	}
	private String getString(String label) {
		try {
			return thePrefs.get(strProfileName + "." + label, "");
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error loading string from preferences");}
		return null;
	}
	
	private void putString(String label, String value) {
		try {
			thePrefs.put(strProfileName + "." + label, value);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error saving string to preferences");}
	}
	
	private int getInt(String label) {
		try {
			return thePrefs.getInt(strProfileName + "." + label, 0);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error loading int from preferences");}
		return 0;
	}
		
	private void putInt(String label, int value) {
		try {
			thePrefs.putInt(strProfileName + "." + label, value);
		}
		catch(Exception e) {ErrorReport.reportError(e, "Error saving int to preferences");}
	}
	
	private Color getColor(String label) {
		int r =  getInt(label + "RED");
		int g =  getInt(label + "GREEN");
		int b =  getInt(label + "BLUE");
		
		return new Color(r, g, b);
	}
	
	private void putColor(String label, Color c) {
		putInt(label + "RED", c.getRed());
		putInt(label + "GREEN", c.getGreen());
		putInt(label + "BLUE", c.getBlue());
	}
	
	private Font getFont(String label) {
		String name = getString(label + "NAME");
		int style = getInt(label + "STYLE");
		int size = getInt(label + "SIZE");
		
		return new Font(name, style, size);
	}
	
	private void putFont(String label, Font f) {
		putString(label + "NAME", f.getName());
		putInt(label + "STYLE", f.getStyle());
		putInt(label + "SIZE", f.getSize());
	}
	
	private MTCWFrame theParentFrame = null;
	private Preferences thePrefs = null;
	private String strProfileName = null;
}
