package util.methods;

/*******************************************************
 * Creates the 'Show' outputs for GO, e.g. ancestors, descendants, and hit lists
 * BasicGOQueryTab, ContigGOPanel and BasicQueryTab
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Stack;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import sng.util.ExportFile;
import sng.viewer.STCWFrame;
import sng.viewer.panels.Basic.BasicGOTablePanel;
import util.database.DBConn;
import util.database.Globalx;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.methods.Static;

public class GOtree {
	private static final String GO_FORMAT = Globalx.GO_FORMAT;
	private static final String GO_PATH_FILE = "AllGoPaths";
	private static final String GO_LONG_FILE = "AllGoLongestPaths";
	private static final String GO_ANC_FILE = "AllGoAncestors";
	
	public static final int ANCESTORS=0;
	public static final int DESCENDENTS=1;
	public static final int NEIGHBORS=2;
	public static final int RELATED=3;
	public static final int PATHS=4;
	public static final int ANC_DIST=5; 
	
	public static final int HITS=6;
	public static final int ALL_HITS=7;
	public static final int HIT_GO_SEQ=8;
	
	public static final int ALL_ANCESTORS=9;
	public static final int LONGEST_PATHS=10;
	public static final int ALL_PATHS=11;
	
	public static final int SELECTED_HIT_ASSIGNED = 12;
	public static final int SELECTED_HIT_ALL = 13;
	
	private static String [] action = { // these are parsed for file names
		"GO Ancestor List", "GO Descendent List", "GO Neighbors List", "GO Related in table",
		"GO Ancestor Paths", "GO Ancestor Ordered List",
		"Hits Assigned List ", "Hits Inherited List", "Sequences with GOs",
		 "All GO Ancestors", "All Longest GO Paths", "All GO paths", 
		"Assigned GOs for selected hit", "Assigned and inherited GOs for selected hit"
	};
	public GOtree(STCWFrame f) {
		theMainFrame = f;
	}
	/****************************************************
	 * Show Related
	 */
	public void popup(int gonum, String desc, int [] set, HashSet<Integer> trim, JButton show, BasicGOTablePanel goTabObj) {
		final int go = gonum;
		final int [] goset = set;
		final HashSet<Integer> trimSet = trim;
		final String descr = desc;
		final JButton showButton = show; // Show button from parent frame gets diabled/enabled
		final BasicGOTablePanel goTabPanelObj = goTabObj;
		final int type=RELATED;
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					showButton.setEnabled(false);
					
					String gostr = String.format(GO_FORMAT, go);
					String label = gostr + "_Related";
					
					SelDialog actObj = new SelDialog(true, label, type);
					actObj.setVisible(true);
					if (actObj.getSelection()==0) {
						showButton.setEnabled(true);
						return;
					}
					
					JDialog dialog = null;
					if (UIHelpers.isApplet())
						dialog = UserPrompt.calcPopup(theMainFrame, action[type] + "  \n for " + gostr + " ");
					
					Vector <String> lines = 
							showGoRelatedInTable(go, descr, goset, trimSet, goTabPanelObj);
					
					actObj.doOutAction(lines);
					
					if (dialog!=null) dialog.dispose();
					showButton.setEnabled(true);
				}
				catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	/** 
	 * GO input - either GO or Hit list - BasicGOQuery and SeqGOPanel 
	 ***/
	public void popup(int gonum, String desc, int searchtype, JButton show) {
		final int go = gonum;
		final String descr = desc;
		final int type = searchtype;
		final JButton showButton = show; // Show button from parent frame gets diabled/enabled
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {		
					showButton.setEnabled(false);
					
					String gostr = String.format(GO_FORMAT, go);
					String [] tmp = action[type].split("\\s+");
					String label = gostr + "_" + tmp[1] + "_" + tmp[2];
					
					boolean bGOnames=false;
					if (type==ANCESTORS || type==DESCENDENTS || type==NEIGHBORS)
						bGOnames=true;
					
					SelDialog actObj = new SelDialog(bGOnames, label, type);
					actObj.setVisible(true);
					if (actObj.getSelection()==0) {
						showButton.setEnabled(true);
						return;
					}
					
					JDialog dialog = null;
					if (UIHelpers.isApplet())
						dialog = UserPrompt.calcPopup(theMainFrame, action[type] + "  \n for " + gostr + " ");
					
					Vector <String> lines = null;
					if (type==ANCESTORS) {
						lines = showGoAncByLevelList(go, descr);
					}
					else if (type==DESCENDENTS) {
						lines = showGoDescByLevelList(go, descr);
					}
					else if (type==NEIGHBORS) {
						lines = showGoNeighborsList(go, descr);
					}
					else if (type==PATHS) {
						lines = showGoAncPathsHtml(go, descr);
					}
					else if (type==ANC_DIST) {
						lines = showGoAncByDistList(go, descr);
					}
					else if (type==HITS) {
						lines = showHitsAssigned(go, descr);
					}
					else if (type==ALL_HITS) {
						lines = showHitsInherited(go, descr);
					}
					else if (type==HIT_GO_SEQ) {
						lines = showHitGoSeq(go, descr);
					}
					
					actObj.doOutAction(lines);
					
					if (dialog!=null) dialog.dispose();
					showButton.setEnabled(true);
				}
				catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
	/** 
	 * Hit input - GO list - BasicHitQuery 
	 ***/
	public void popup(String hit, JButton show, int displaytype) {
		final String hitID = hit;
		final JButton showButton = show;
		final int type = displaytype;
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					String msg = action[type] + " " +  hitID;
					showButton.setEnabled(false);
					JDialog dialog = null;
					if (UIHelpers.isApplet()) dialog = UserPrompt.calcPopup(theMainFrame, msg);
					
					Vector <String> lines = null;
					if (type==SELECTED_HIT_ASSIGNED) lines = showHitGoList(hitID);
					else lines = showHitGoListAll(hitID);
					
					if (lines==null) {
						JOptionPane.showMessageDialog(null, "Query failed");
					}
					else {
						String [] alines = new String [lines.size()];
						lines.toArray(alines);
						UserPrompt.displayInfoMonoSpace(theMainFrame, msg, alines);
					}
					if (dialog!=null) dialog.dispose();
					showButton.setEnabled(true);
				}
				catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}	
	/*********************************************************
	 * XXX GO input - GO list - fast searches using go_graph_path
	 */
	private Vector <String> showGoRelatedInTable(int gonum, String godesc, int [] goset,
			   HashSet<Integer> trimSet, BasicGOTablePanel goTabObj) {
		try {		
			ancMap = new HashMap <Integer, Integer> ();
			Vector <String> ancLines = showGoAncByLevelList(gonum, godesc); 
			Vector <String> ancTab = new Vector <String> ();
			int cntTrim=0;
			for (int row=0; row<goset.length; row++) {
				int go = goset[row];
				if (ancMap.containsKey(go)) {
					int idx = ancMap.get(go);
					String isTrim = (trimSet!=null && trimSet.contains(go)) ? "*" : " ";
					if (isTrim.equals("*")) cntTrim++;
					ancTab.add(ancLines.get(idx) + " " + isTrim);
				}
			}
			
			descMap = new HashMap <Integer, Integer> ();
			Vector <String> descLines = showGoDescByLevelList(gonum, godesc);
			Vector <String> descTab = new Vector <String> ();
			
			for (int row=0; row<goset.length; row++) {
				int go = goset[row];
				if (descMap.containsKey(go)) {
					int idx = descMap.get(go);
					String isTrim = (trimSet!=null && trimSet.contains(go)) ? "*" : " ";
					if (isTrim.equals("*")) cntTrim++;
					descTab.add(descLines.get(idx) + " " + isTrim);
				}
			}
			
			Vector <String> lines = new Vector <String> ();
			
			lines.add(String.format("Anc:Desc %d:%d of %d:%d  ", 
						ancTab.size(), descTab.size(), ancMap.size(), descMap.size()) + 
					String.format(GO_FORMAT, gonum) + " - " + godesc
				);
			
			if (ancTab.size()>0) {
				lines.add("");
				lines.add("Ancestors in table");
				lines.add(String.format("%-10s  %-5s  %s", "GO term", "Level",  "Description"));		
				for (String g: ancTab) lines.add(g);
			}
			if (descTab.size()>0) {
				lines.add("");
				lines.add("Descendants in table");
				lines.add(String.format("%-10s  %-5s  %s", "GO term", "Level",  "Description"));		
				for (String g: descTab) lines.add(g);
			}
			if (cntTrim>0) {
				lines.add("");
				lines.add("* In trim set");
			}
			ancMap.clear();  ancMap=null;
			descMap.clear(); descMap=null;
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	/**************** GO Ancestors *****************/ 
	private Vector <String> showGoAncByLevelList(int gonum, String godesc) {
		try {
																		
			DBConn mDB = theMainFrame.getNewDBC();
			setTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + rs.getString(1) + ")";
			
			HashSet <Integer> dups = new HashSet <Integer> (); // can have is_a and part_of
			rs = mDB.executeQuery("select " +
					" p.ancestor, g.descr, g.level " +
					" from go_graph_path as p" +
					" join go_info as g on g.gonum=p.ancestor" +
					" where child=" + gonum + 
					" order by g.level, p.ancestor"); 
			
			int offSet=3; // For related, start at line.get(3)
			
			Vector<String> goLines = new Vector<String> (); 
			while (rs.next()) { 
				int gonum2 = rs.getInt(1);
				if (gonum2==gonum) continue;
				if (dups.contains(gonum2)) continue; // can be is_a and part_of
				dups.add(gonum2);
				
				String go = String.format(GO_FORMAT, gonum2);
				String desc = rs.getString(2);
				String level = "  " + rs.getInt(3);
				String l = String.format("%-10s  %-5s  %s", go, level, desc);
				
				goLines.add(l);
				if (ancMap!=null) ancMap.put(gonum2, offSet);
				offSet++;
			}
			if (rs!=null) rs.close(); mDB.close();
			
			Vector <String> lines = new Vector <String> ();
			lines.add(goLines.size() + " Ancestors of " + String.format(GO_FORMAT, gonum) + " - " + godesc + term);
			lines.add("");
			
			if (goLines.size()>0) {
				lines.add(String.format("%-10s  %-5s  %s", "GO term", "Level",  "Description"));		
				for (String g : goLines) lines.add(g);
			}
			lines.add("");
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	// Ancestor sorted by distance
	private Vector <String> showGoAncByDistList(int gonum, String godesc) {
		try {
																		
			DBConn mDB = theMainFrame.getNewDBC();
			setTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + rs.getString(1) + ")";
			
			rs = mDB.executeQuery("select " +
					" p.ancestor, p.distance, p.relationship_type_id, g.descr " +
					" from go_graph_path as p" +
					" join go_info as g on g.gonum=p.ancestor" +
					" where child=" + gonum + 
					" order by p.distance DESC, p.ancestor"); 
			
			// The is_a and part_of have different distances, hence, two tables
			Vector<String> goIsA = new Vector<String> (); 
			Vector<String> goPartOf = new Vector<String> (); 
			while (rs.next()) { 
				int gonum2 = rs.getInt(1);
				if (gonum==gonum2) continue;
				
				String go = String.format(GO_FORMAT, gonum2);
				String level = "  " + rs.getInt(2);
				int type = rs.getInt(3);
				String desc = rs.getString(4);
				
				String l = String.format("%-10s  %-5s  %s", go, level, desc);
				
				if (!typeMap.containsKey(type)) continue;
				String relType = typeMap.get(type);
				if (relType.equals("is_a")) goIsA.add(l);
				else goPartOf.add(l);
			}
			if (rs!=null) rs.close(); mDB.close();
			
			Vector <String> lines = new Vector <String> ();
			lines.add("Ancestors of " + String.format(GO_FORMAT, gonum) + " - " + godesc + term);
			lines.add("");
			
			lines.add(goIsA.size() + " Relation: is_a");
			if (goIsA.size()>0) {	
				lines.add(String.format("%-10s  %-5s  %s", "GO term", "Dist",  "Description"));		
				for (String g : goIsA) lines.add(g);
			}
			
			lines.add("");
			
			lines.add(goPartOf.size() + " Relation: part_of");
			if (goPartOf.size()>0) {
				lines.add(String.format("%-10s  %-5s  %s", "GO term", "Dist",  "Description"));		
				for (String g : goPartOf) lines.add(g);
			}
			
			lines.add("");
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list by Distance");}
		return null;
	}
	/***************** GO Descendants **************/ 
	private Vector <String> showGoDescByLevelList(int gonum, String goDesc) {
		try {
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
			setTypes(mDB);
			
			HashSet <Integer> dups = new HashSet <Integer> (); // can have is_a and part_of
			int offset=3; // For Related, first GO at lines.get(3)
			
			rs = mDB.executeQuery("Select child, distance, relationship_type_id, i.descr, i.level " +
					" from go_graph_path as p, go_info as i " +
					" where p.child=i.gonum and ancestor=" + gonum + 
					" order by i.level, p.child"); 
			
			Vector <String> goLines = new Vector <String> ();
			while (rs.next()) { 
				int gonum2 = rs.getInt(1);
				if (gonum2==gonum) continue;
				if (dups.contains(gonum2)) continue;
				dups.add(gonum2);
				
				String go = String.format(GO_FORMAT, gonum2);
				String desc = rs.getString(4);
				String level = String.format(" %2d", rs.getInt(5));
			
				String l = String.format("%-10s %-5s %s", go, level, desc);
				goLines.add(l);
				if (descMap!=null) descMap.put(gonum2, offset);
				offset++;
			}
			
			Vector <String> lines = new Vector <String> ();
			lines.add(goLines.size() + " Descendents of " + String.format(GO_FORMAT, gonum) + " - " + goDesc);
			lines.add("");
			
			if (goLines.size()>0) {
				lines.add(String.format("%-10s %-5s %s ", "GO term", "Level", "Description"));
				for (String g : goLines) lines.add(g);
			}
			lines.add("");
			
			lines.add("Descendents are only shown if they have a hit in the sTCWdb");
			if (rs!=null) rs.close(); mDB.close();
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/**************** GO neighbor list **************************/
	private Vector <String> showGoNeighborsList(int gonum, String godesc) {
		try {															
			DBConn mDB = theMainFrame.getNewDBC();
			setTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + rs.getString(1) + ")";
			
			Vector <String> lines = new Vector <String> ();
			lines.add("Neighbors of " + String.format(GO_FORMAT, gonum) + " - " + godesc + term);
			lines.add("");	
			
			HashMap <Integer, GOterm> neighGOs = new HashMap <Integer, GOterm> ();
			for (int i=0; i<2; i++) {
				if (i==0) {
					lines.add("Parents:");
					rs = mDB.executeQuery("select t.parent, t.relationship_type_id, i.descr " +
						" from go_term2term as t, go_info as i" +
						" where parent>0 and child=" + gonum + " and t.parent=i.gonum ");
				}
				else {
					lines.add("Children:");
					rs = mDB.executeQuery("select t.child, t.relationship_type_id, i.descr " +
							" from go_term2term as t, go_info as i" +
							" where child>0 and parent=" + gonum + " and t.child=i.gonum");
				}
				while (rs.next()) { 
					int gonum2 = rs.getInt(1);
					String type = typeMap.get(rs.getInt(2));
					String desc = rs.getString(3);
					if (neighGOs.containsKey(gonum2)) {
						GOterm gt = neighGOs.get(gonum2);
						if (!type.equals(gt.type)) gt.type="both";
					}
					else {
						neighGOs.put(gonum2,  new GOterm(gonum2, type, desc));
					}
				}
				if (neighGOs.size()==0) lines.add("None");
				else {
					lines.add(String.format("%-10s  %-8s  %s", "GO term", "Relation", "Description"));
					for (int gonum2 : neighGOs.keySet()) { 
						GOterm gt = neighGOs.get(gonum2);
						String go = String.format(GO_FORMAT, gt.gonum);
						String l = String.format("%-10s  %-8s  %s", go, gt.type, gt.desc);
						lines.add(l);
					}
				}
				neighGOs.clear();
				lines.add("");
			}
			lines.add("Children only shown if they have a hit in the sTCWdb");
			if (rs!=null) rs.close(); 
			mDB.close();
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	
	/*********************************************************
	 * GO input - GO num - slow searches using recurse on term2term
	 */
	/**** GO Paths - Basic GO and Seq GO ***/
	private Vector <String> showGoAncPathsHtml(int gonum, String desc) {
		try {
			DBConn mDB = theMainFrame.getNewDBC();
			go_allPaths(mDB, gonum);
			go_allGOs(mDB);
			mDB.close();
			
			Vector <String> lines = new Vector <String> ();
			int nPaths=allPaths.size();
			String goTerm = (gonum>=0) ? String.format(GO_FORMAT, gonum) + " - " : "";
			String td = "<td width=100>";
			String tdAll = "<td bgcolor=\"#d3d3d3 \" colSpan=";
			String tdAllx = "<td align=center bgcolor=\"#d3d3d3 \" colSpan=";
			if (nPaths>30) td = "<td width=50>";
			else if (nPaths>20) td = "<td width=75>";
			
			lines.add("<html><body>");
			lines.add("<center>" + goTerm + desc + "</center>");
			lines.add("<center> Paths: " + allPaths.size() + " Ancestors: " + (allGOs.size()-1) + "</center>");
			
			lines.add("<table border=1 width=>");
			String line="<tr><th>&nbsp;";
			for (int i=0; i<nPaths; i++) 
				line += "<th>Path" + (i+1);
			lines.add(line);
			
			for (int lev=1; lev<=maxLevel; lev++) { 		// rows
				boolean colSpan=false;
				line="<tr><td>" + lev;
				for (Vector <Integer> path : allPaths) { // columns
					boolean found=false;
					for (int go : path) {					// find level in each path
						GOterm gt = allGOs.get(go);
						if (gt.level!=lev) continue;
						String descLabel = gt.desc + gt.msg;
						
						if (gt.count==nPaths) {
							if (nPaths>18) line += tdAll   + nPaths + ">" + descLabel;
							else           line += tdAllx + nPaths + ">" + descLabel;
							colSpan=true;
						}
						else {
							line += td + descLabel;
							found=true;
						}
						break; 
					}
					if (colSpan) break; 
					if (!found) line+="<td>&nbsp;";
				}
				lines.add(line);
			}
			
			lines.add("</table>");
			lines.add("<p>Relations: is_a or part_of");
			lines.add("</body></html>");
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "GO tree");}
		return null;
	}
	
	// makes allPaths<gonum, list of ancestors> 
	private Stack<Integer> goStack = new Stack<Integer>();
	private void go_allPaths(DBConn mDB, int gonum) { // recursive
		try {
			goStack.push(gonum);
			ResultSet rs = mDB.executeQuery("select parent from go_term2term " +
					" where parent>0 and child=" + gonum);
			Stack<Integer> cur = new Stack<Integer>();
			while (rs.next()) {
				int gonum2=rs.getInt(1);
				if (gonum!=gonum2) {
					cur.push(gonum2);
				}
			}
			rs.close();
			if (cur.isEmpty()) {
				Stack <Integer> p = new Stack <Integer> ();
				for (int i=goStack.size()-1; i>=0; i--) p.push(goStack.get(i));
				allPaths.add(p);
			}
			else {
				while (!cur.isEmpty()) {
					go_allPaths(mDB, cur.pop());
				}
			}
			goStack.pop();
		}
		catch (Exception e) {ErrorReport.die(e, "traverse");}
	}
	
	// get description for GOs in allPaths, and set level
	private void go_allGOs(DBConn mDB) {
		try {
			// make list and String of GOterms; set levels
			String golist="";
			for (Vector <Integer> path : allPaths) {
				int lev=1;
				for (int gonum : path) {
					if (allGOs.containsKey(gonum)) {
						GOterm gt = allGOs.get(gonum);
						if (lev>gt.level) gt.level = lev;
						gt.count++;
					}
					else {
						allGOs.put(gonum,  new GOterm(gonum, lev));
						
						if (golist.equals("")) golist = ""+ gonum;
						else golist += "," + gonum;
					}
					lev++;
				}
			}
			
			ResultSet rs = mDB.executeQuery("select gonum, descr from go_info where" +
					" gonum in ( " + golist + ")");
			while (rs.next()) {
				int g = rs.getInt(1);
				GOterm gt = allGOs.get(g);
				gt.desc = rs.getString(2);
				//gt.desc = d.replace("_", " "); // this is done for html tables only 
				//gt.desc = d.replace("-", " "); // so the phrases can be split in cells
				if (gt.level>maxLevel) maxLevel=gt.level;
			}
			rs.close();
		}
		catch (Exception e) {ErrorReport.die(e, "getPaths");}
	}
	
	/********************************************************
	 * XXX GO input - Hit list
	 */
	/***  GO Hit Assigned List ****/
	private Vector <String> showHitsAssigned(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("Hits with assigned " + String.format(GO_FORMAT, gonum) + " - " + godesc);
			lines.add("");
			String line = String.format("%-16s  %-3s  %-40s  %s", 
					"Hit ID", "EC", "Description", "Species");
			lines.add(line);
				
			int count=0;
			
			String query = "SELECT up.hitID, g.EC, up.description, up.species " +
					"FROM pja_uniprot_go as g " +
					"join pja_db_unique_hits as up on g.DUHID=up.DUHID " +
					"WHERE g.gonum=" + gonum + " order by up.hitID";
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery(query);
			while (rs.next()) {
				String desc = rs.getString(3);
				if (desc.length()>40) desc = desc.substring(0,38) + "..";
				String spec = rs.getString(4);
				if (spec.length()>40) spec = spec.substring(0,38) + "..";
				String l = String.format("%-16s  %3s  %-40s  %s", 
					rs.getString(1), rs.getString(2), desc, spec);
				lines.add(l);
				count++;
			}
			lines.add("");
			lines.add("Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/***  GO Hit Inherited List **/ 
	private Vector <String> showHitsInherited(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("Hits with inherited " + String.format(GO_FORMAT, gonum) + " - " + godesc);
			lines.add("");
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
			
			Vector <GOterm> goList = new Vector <GOterm> ();
			rs = mDB.executeQuery("select child, distance, g.descr, g.level " +
					"from go_graph_path as p, go_info as g " +
					"where p.child=g.gonum and child>0 and ancestor=" + gonum + 
					" order by g.level, p.child"); 
			while (rs.next()) {
				int gonum2=rs.getInt(1);
				if (gonum!= gonum2) {
					int dist = rs.getInt(2);
					String desc = rs.getString(3);
					int level = rs.getInt(4);
					goList.add(new GOterm(gonum2, level, dist, desc));
				}
			}
			rs.close();
			
			if (goList.size()==0) {
				lines.add("No inherited hits");
				mDB.close();
				return lines;
			}
			
			int count=0;
			String line = String.format("%-16s %-3s %-30s   %-10s  %-5s  %-30s  ", 
					 "Hit ID", "EC", "Description",   "Descendent","Level","Description");
			lines.add(line);

			for (GOterm gt : goList) {
				String rd = String.format(GO_FORMAT,  gt.gonum);
				String level = String.format(" %2d", gt.level);
				if (gt.desc.length()>30) gt.desc = gt.desc.substring(0,28) + "..";
				
				rs = mDB.executeQuery("SELECT up.hitID, g.EC, up.description " +
						"FROM pja_uniprot_go as g " +
						"join pja_db_unique_hits as up on g.DUHID=up.DUHID " +
						"WHERE g.gonum=" + gt.gonum + " order by up.hitID");
				
				while (rs.next()) {
					String hitID = rs.getString(1);
					String ec = rs.getString(2);
					String desc = rs.getString(3);
					if (desc.length()>30) desc = desc.substring(0,28) + "..";
					
					String l = String.format("%-16s %-3s %-30s   %-10s  %-5s  %-30s  ", 
						 hitID,  ec, desc,  rd, level, gt.desc);
					lines.add(l);
					count++;
				}
			}
			if (rs!=null) rs.close();
			mDB.close(); 
			
			lines.add("");
			lines.add("Count: " + count);
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/***  GO Hit Inherited List **/ 
	private Vector <String> showHitGoSeq(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("Sequences with hit with " + String.format(GO_FORMAT, gonum) + " - " + godesc);
			lines.add("");
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
			
			Vector <SeqHit> hitList = new Vector <SeqHit> ();
			rs = mDB.executeQuery("select CTGID, bestDUH, bestEval, bestEV, bestAN, EC " +
					"from pja_unitrans_go where gonum =" + gonum + " order by bestEval");
			while (rs.next()) {
				SeqHit sh = new SeqHit();
				sh.seqID = rs.getInt(1);
				sh.hitID = rs.getInt(2);
				sh.eval = rs.getDouble(3);
				sh.bestEval = rs.getBoolean(4);
				sh.bestAnno = rs.getBoolean(5);
				sh.ec = rs.getString(6);
				hitList.add(sh);
			}
			rs.close();
			
			if (hitList.size()==0) {
				lines.add("No sequences for this GO - not possible");
				mDB.close();
				return lines;
			}
			for (SeqHit sh : hitList) {
				rs = mDB.executeQuery("select contigid from contig where CTGID=" + sh.seqID);
				rs.next();
				sh.seqName = rs.getString(1);
				rs = mDB.executeQuery("select hitid from pja_db_unique_hits where DUHID=" + sh.hitID);
				rs.next();
				sh.hitName = rs.getString(1);
			}
			rs.close();mDB.close(); 
			
			int count=0;
			String line = String.format("%-15s  %-20s  %6s  %5s  %5s  %3s", 
					"Sequence", "Hit", "E-val", "Is EV", "Is AN", "EC");
			lines.add(line);

			for (SeqHit sh : hitList) {
				String eval = String.format("%.0E", sh.eval);
				String l = String.format("%-15s  %-20s  %6s  %5s  %5s  %3s", 
					sh.seqName, sh.hitName, eval, sh.bestEval, sh.bestAnno,  sh.ec);
					lines.add(l);
					count++;
			}
			
			lines.add("");
			lines.add("Count: " + count);
			lines.add("");
			lines.add("EC (evidence code): If EC is in parenthesis, it is inherited.");
			lines.add("");
			lines.add("Note: Each sequence may have many hits with this GO, where the hits have different ECs.");
			lines.add("      Only the hit with the best e-value is shown with its associated EC.");
			lines.add("Note: All sequences with the GO are shown, regardless of their DE values,");
			lines.add("      hence, the number shown may be greater than #Seqs.");
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/***********************************************
	 * XXX Hit input - GO list
	 */
	private Vector <String> showHitGoList(String hitName) {
		try {
			Vector <String> lines = new Vector <String> ();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("select DUHID from pja_db_unique_hits where hitID='" 
					+ hitName + "'");
			int duhid = 0;
			if (rs.next()) duhid =  rs.getInt(1);
			else return null;
			
			lines.add("Assigned GOs for " + hitName);
			lines.add("");
			
			int count=0;
			rs = mDB.executeQuery("SELECT p.gonum, p.EC, g.descr, g.level, g.term_type " +
					"FROM pja_uniprot_go as p " +
					"join go_info as  g on g.gonum=p.gonum " +
					"WHERE p.duhid=" + duhid + " order by g.level, p.gonum"); 
			lines.add(String.format("%-10s %-5s %-3s  %-4s  %s", 
								"GO term", "Level", "EC", "Type", "Description"));
			while (rs.next()) {
				String go = String.format(GO_FORMAT, rs.getInt(1));
				String ec = rs.getString(2);
				String desc = rs.getString(3);
				String level = "  " + rs.getInt(4);
				String term = rs.getString(5).substring(0,4);
				String l = String.format("%-10s %-5s %-3s  %-4s  %s", go, level, ec, term, desc);
				lines.add(l);
				count++;
			}
			lines.add("");
			lines.add("Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	private Vector <String> showHitGoListAll(String hitName) {
		try {
			Vector <String> lines = new Vector <String> ();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("select DUHID from pja_db_unique_hits where hitID='" 
					+ hitName + "'");
			int duhid = 0;
			if (rs.next()) duhid =  rs.getInt(1);
			else return null;
			
			lines.add("Assigned and inherited GOs for " + hitName);
			lines.add("");
			
			Vector <Integer> goSet = new Vector <Integer> ();
			HashMap <Integer, String> goMap = new HashMap <Integer, String> ();
			int count=0;
			rs = mDB.executeQuery("SELECT p.gonum, p.EC, g.descr, g.level, g.term_type " +
					"FROM pja_uniprot_go as p " +
					"join go_info as  g on g.gonum=p.gonum " +
					"WHERE p.duhid=" + duhid + " order by g.term_type, g.level, p.gonum"); 
			lines.add(String.format("%-10s %-5s %-3s  %-4s  %s", 
								"GO term", "Level", "EC", "Type", "Description"));
			while (rs.next()) {
				int gonum = rs.getInt(1);
				String go = String.format(GO_FORMAT, gonum);
				goSet.add(gonum);
				String ec = rs.getString(2);
				String desc = rs.getString(3);
				String level = "  " + rs.getInt(4);
				String term = rs.getString(5).substring(0,4);
				String l = String.format("%-10s %-5s %-3s  %-4s  %s", go, level, ec, term, desc);
				goMap.put(gonum, l);
				count++;
			}
			HashSet <Integer> foundSet = new HashSet <Integer> ();
			for (int child : goSet) {
				rs = mDB.executeQuery("select p.ancestor, g.descr, g.level, g.term_type " +
						" from go_graph_path as p " +
						" join go_info as g on g.gonum=p.ancestor " +
						" where child=" + child + " order by g.level, p.ancestor"); 
				
				while (rs.next()) {
					int gonum2 = rs.getInt(1);
					if (child==gonum2) continue;
					if (foundSet.contains(gonum2)) continue; // can be is_a and part_of
					
					foundSet.add(gonum2);
					String go2 = String.format(GO_FORMAT, gonum2);
					String desc = rs.getString(2);
					String level = "  " + rs.getInt(3);
					String term = rs.getString(4).substring(0,4);
					String l = String.format("%-10s %-5s %-3s  %-4s  %s", go2, level, "", term, desc);
					lines.add(l);
					count++;
				}
				lines.add(goMap.get(child));
			}
			lines.add("");
			lines.add("Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}	
	
	/****************** Map is_a and part_of to index *****************/
	// The partof does change 'id', which is found in the GO MySQL term table.
	private void setTypes(DBConn mDB) {
		try {
			int x=1, total;
			ResultSet rs = mDB.executeQuery("select isa from assem_msg");
			if (rs.next()) x = rs.getInt(1);
			typeMap.put(x, "is_a");
			total=x;
			
			rs = mDB.executeQuery("select partof from assem_msg");
			if (rs.next()) x = rs.getInt(1);
			typeMap.put(x, "part_of");
			total += x;
			
			typeMap.put(0, "");
			typeMap.put(total, "both");
		}
		catch(Exception e) {ErrorReport.prtReport(e, "set types");}
	}
	
	/******************************************************************/
	class GOterm implements Comparable<GOterm> {
		GOterm (int gonum, int level) {
			this.gonum = gonum;
			this.level = level;
		}
		GOterm (int gonum, String desc) {
			this.gonum = gonum;
			this.desc = desc;
		}
		GOterm (int gonum, String type,  String desc) { // go neighbors
			this.gonum = gonum;
			this.type = type; // is_a, part_of
			this.desc = desc;
		}
		GOterm (int gonum, int level, int dist, String desc) { // go Hits
			this.gonum = gonum;
			this.level = level;
			this.dist = dist;
			this.desc = desc;
		}
		GOterm (int gonum, int level, String term, String desc) {//All ancestors
			this.gonum = gonum;
			this.level = level;
			this.term = term.substring(0,4);
			this.desc = desc;
		}
		public void setMsg(String msg) {this.msg = msg;}
		
		public int compareTo(GOterm t) { 
			if (!this.term.equals(t.term)) {
				return this.term.compareToIgnoreCase(t.term);
			}
			if (bSORT_BY_DIST) {
				if (this.level < t.level) return 1;
				if (this.level > t.level) return -1;
			}
			else {
				if (this.level > t.level) return 1;
				if (this.level < t.level) return -1;
			}
			return (this.gonum - t.gonum); 
		}
		int gonum;
		String desc= "", type="", term="", msg="";
		int level=0;
		int count=1;
		int dist=0; // go_graph_path.distance
	}	
	private class SeqHit {
		String seqName, hitName;
		int seqID, hitID;
		double eval;
		boolean bestEval, bestAnno;
		String ec;
	}
	private class SelDialog extends JDialog {
		private static final long serialVersionUID = 1L;
		private int nMode=1; // 1=popup, 2=write name, 3=write all, 0 = cancel
		private File exportFH=null;
		private String fileName="";
		private int type=0;
		
		public SelDialog(boolean bGOnames, String msg, int type) {
			this.fileName = msg;
			this.type = type;
        		setModal(true);
        		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        		setTitle(msg);
        		
        		JRadioButton btnPop = Static.createRadioButton("Show information in popup", true);
        		btnPop.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent arg0) {
    					nMode = 1;
    				}
    			});
        	
        		JRadioButton btnName =  Static.createRadioButton("Export names to file", false);
	        	btnName.addActionListener(new ActionListener() {
    				public void actionPerformed(ActionEvent arg0) {
    					nMode = 2;
    				}
    			});
        		JRadioButton btnAll = Static.createRadioButton("Export all information to file", false);
            btnAll.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    nMode = 3;
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
        		grp.add(btnName);
	        grp.add(btnAll); 
	          
	    		JPanel selectPanel = Static.createPagePanel();
	    		selectPanel.add(btnPop);
	    		selectPanel.add(new JSeparator());
	    		selectPanel.add(Box.createVerticalStrut(5));
	    		if (bGOnames) selectPanel.add(btnName);
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
		public int getSelection() {
			try {
				if (nMode==2 || nMode==3) {
					String f = fileName.replace(":","_"); // linux filenames can not have ':'
					String fName = (nMode==2) ? f+"_names" : f+"_lines";
					if (type==PATHS) fName += ".html";
					else fName += ".txt";
					
					if (nMode==2) {
						exportFH = ExportFile.getFileHandle(fName, theMainFrame);
						if (exportFH==null) nMode=0;
					}
					else if (nMode==3) {
						exportFH = ExportFile.getFileHandle(fName, theMainFrame);
						if (exportFH==null) nMode=0;
					}
				}
				if (nMode==0) Out.prt("Cancel operation");
				
				return nMode;
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Error in selection"); return 3;}
		}
		/**
		 * Either writes to 1. pop-up or 2. names to file. 3. lines to file
		 */
		private void doOutAction(Vector <String> lines) {
			try {
				if (nMode==1) { 
					if (lines==null) {
						JOptionPane.showMessageDialog(null, fileName);
					}
					else {
						String [] alines = new String [lines.size()];
						lines.toArray(alines);
						if (type==PATHS) UserPrompt.displayHTML(fileName, alines);
						else UserPrompt.displayInfoMonoSpace(null, fileName, alines);
					}
					return;
				}
				if (exportFH==null) {
					JOptionPane.showMessageDialog(null, "TCW error: file is null");
					return;
				}
				Out.PrtSpMsg(0, "Writing " + exportFH.getName() + " ...");
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportFH)));
				int cnt=0;
				if (nMode==2) {
					String [] go = fileName.split("_");
					if (go[0].startsWith("GO:")) {// it should
						pw.println(go[0]);
						cnt++;
					}
					
					for (String l : lines) {
						if (l.startsWith("GO:")) {
							String [] tok = l.split("\\s+");
							pw.println(tok[0]);
							cnt++;
						}
					}
				}
				else {
					for (String l : lines) {
						pw.println(l);
						cnt++;
					}
				}
				pw.close();
				Out.PrtSpMsg(0, "Wrote " + cnt + " lines");
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Error in seleciton"); return;}
			
		}
	}
	/**************************************************************
	 * XXX  GO annotation: Table... methods
	 */
	public void popupExportAll(int type, TreeSet <Integer> gos) {
		final  TreeSet <Integer> goMap = gos;
		final int showType=type;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {		
					JDialog dialog = null;
					if (UIHelpers.isApplet() || goMap.size()>1000)
						dialog = UserPrompt.calcPopup(theMainFrame, action[showType]);
					
					AllDialog allObj = new AllDialog(showType);
					allObj.setVisible(true);
					
					if (allObj.getSelection()) allObj.doAction(goMap);
					
					if (dialog!=null) dialog.dispose();
				}
				catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
/******************************************************
 * 		  allAnc			longPath		allPaths
 * popup  1 (" ") 		html			--   
 * file	  1 ("\t")	tsv/html		tsv/html
 */
private class AllDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private final int MODE_POP=1, MODE_WRITE=2, MODE_CANCEL=0;
	private final int INFO_DESC=1, INFO_TERM=2;
	private final int OUT_HTML=1, OUT_TSV=2, OUT_TXT=3;

	private int nMode=MODE_POP; 
	private int nInfo=INFO_DESC; // 1=descriptions 2=GO term
	private int nOut=1;  // 1=html, 2=tsv
	
	private int nType;   //  parameter all, paths, anc
	private String msg="", fName="";
	private File exportFH=null;
	
	
	public AllDialog(int type) {
		nType = type;
		if (nType==ALL_ANCESTORS)  {msg = "All ancestors"; fName=GO_ANC_FILE;}
		else if (nType==ALL_PATHS) {msg = "All paths";     fName=GO_PATH_FILE;}
		else                       {msg = "Longest paths"; fName=GO_LONG_FILE;}
		
		Out.Print("Starting " + msg);
		
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle(msg);
    		
    		JPanel selectPanel = Static.createPagePanel();
    		
    		JRadioButton btnDesc = Static.createRadioButton("GO Descriptions", true);
    		btnDesc.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nInfo = INFO_DESC;
				}
			});
    	
    		JRadioButton btnTerm =  Static.createRadioButton("GO terms", false);
        	btnTerm.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nInfo = INFO_TERM;
				}
			});
        	ButtonGroup grp2 = new ButtonGroup();
    		grp2.add(btnDesc);
    		grp2.add(btnTerm);
    		
    		if (nType==ALL_PATHS || nType==LONGEST_PATHS) {
    			selectPanel.add(btnDesc); selectPanel.add(Box.createHorizontalStrut(10));
    			selectPanel.add(btnTerm);
    			selectPanel.add(new JSeparator());
    		}
    	
    		JRadioButton btnPop = Static.createRadioButton("Show in popup", true);
    		btnPop.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nMode = MODE_POP;
					nOut= (nType==ALL_ANCESTORS) ? OUT_TXT : OUT_HTML;
				}
			});
    		
    		JRadioButton btnTsv =  Static.createRadioButton("Export to file (.tsv)", false);
        	btnTsv.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nMode = MODE_WRITE;
					nOut = OUT_TSV;
				}
			});
        	JRadioButton btnHtml =  Static.createRadioButton("Export to file (.html)", false);
        	btnHtml.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nMode = MODE_WRITE;
					nOut = OUT_HTML;
				}
			});
    		  
        	ButtonGroup grp1 = new ButtonGroup();
    		grp1.add(btnPop);
    		grp1.add(btnTsv);
    		grp1.add(btnHtml);
       
    		selectPanel.add(btnPop);
    		selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(btnTsv);
    		selectPanel.add(Box.createVerticalStrut(5));
    		
    		if (nType==ALL_PATHS || nType==LONGEST_PATHS) {
    			selectPanel.add(btnHtml);
    			selectPanel.add(Box.createVerticalStrut(5));
    		}
    		
        JButton btnOK = Static.createButton("OK", true);
			btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
    		JButton btnCancel = Static.createButton("Cancel", true);
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode=MODE_CANCEL;
					setVisible(false);
				}
			});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		
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
	public boolean getSelection() {
		try {
			if (nMode==MODE_CANCEL) {
				Out.prt("Cancel operation");
				return false;
			}
			if (nMode==MODE_WRITE) {
				fName += (nOut==OUT_TSV) ? Globalx.CSV_SUFFIX : ".html";
				exportFH = ExportFile.getFileHandle(fName, theMainFrame);	
				if (exportFH==null) {
					Out.prt("Cancel file");
					return false;
				}
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in selection"); return false;}
	}
	
	private boolean doAction(TreeSet <Integer> goMap) {
		try {
			Out.Print("   " + goMap.size() + " GOs from table");
			Vector <String> lines = null;
			if (nType==ALL_ANCESTORS) {
				lines = goAllAncestors(goMap);
			}
			else  { 
				if (nOut==OUT_HTML) lines=goPathsHtml(goMap);
				else                lines=goPathsTsv(goMap);
			}
			if (lines==null) {
				JOptionPane.showMessageDialog(null, "No ouput");
				return false;
			}
		
			if (nMode==MODE_POP) { 
				String [] alines = new String [lines.size()];
				lines.toArray(alines);
				if (nType==ALL_ANCESTORS)
					UserPrompt.displayInfoMonoSpace(null, msg, alines); // adds nl
				else 
					UserPrompt.displayHTML(msg, alines); // do not need nl
				
				Out.Print("Complete popup of " + lines.size() + " lines");
			}
		
			if (nMode==MODE_WRITE) {
				Out.Print("Writing " + exportFH.getName() + " ...");
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportFH)));
				
				for (String l : lines) pw.println(l);
		
				pw.close();
				Out.Print("Wrote " + lines.size() + " lines");
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in seleciton");}
		return false;
	}
	// compute and output longest paths in text form (Popup) or tsv (Write)
	private Vector <String> goAllAncestors(TreeSet <Integer> goMap) {
		try {	
			Vector <String> lines = new Vector <String> ();
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
		
			Vector <GOterm> goList = new Vector <GOterm> ();
			HashSet <Integer> goDup = new HashSet <Integer> ();
			
			for (int gonum : goMap) {
				rs = mDB.executeQuery("select " +
						" descr, level, term_type from go_info " +
						" where gonum=" + gonum); 
				
				if (rs.next()) {	
					String desc = rs.getString(1);
					int level = rs.getInt(2);
					String term_type = rs.getString(3);
					
					GOterm gt = new GOterm(gonum, level, term_type, desc);
					gt.msg=" *";
					goList.add(gt);	
					goDup.add(gonum);
				}
				else Out.PrtError("TCW error on gonum=" + gonum);
			}
			
			for (int gonum : goMap) {
				rs = mDB.executeQuery("select " +
						" p.ancestor, g.descr, g.level, g.term_type " +
						" from go_graph_path as p" +
						" join go_info as g on g.gonum=p.ancestor" +
						" where child=" + gonum); 
				
				while (rs.next()) {
					int gonum2 = rs.getInt(1);
					if (gonum2==gonum) continue;
					if (goDup.contains(gonum2)) continue;
					
					String desc = rs.getString(2);
					int level = rs.getInt(3);
					String term_type = rs.getString(4);
					
					GOterm gt = new GOterm(gonum2, level, term_type, desc);
					goList.add(gt);
					goDup.add(gonum2);
				}
			}
			goDup.clear();
			if (rs!=null) rs.close(); mDB.close();
			
			Collections.sort(goList);
			
			String delim = (nMode==MODE_POP) ? " "  : Globalx.CSV_DELIM; 
			String format = "%-11s" + delim + "%6s" + delim +"%-5s" + delim + "%s";
			lines.add(String.format(format, "GO term", "Domain", "Level", "Description"));
			
			for (GOterm gt : goList) { 
				String go = String.format(GO_FORMAT, gt.gonum);
				String level = " " + gt.level + "  ";
				lines.add(String.format(format, go, gt.term, level, (gt.desc+gt.msg)));
			}
			lines.add("");
			lines.add("* From GO table: " + goMap.size() + "    Total GOs: " + goList.size());
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Ancestor list");}
		return null;
	}
	// pathCreate and output as HTML
	private Vector <String> goPathsHtml(TreeSet <Integer> goMap) {
		try {
			int [] stat = pathCreate(goMap);
			
			Vector <String> lines = new Vector <String> ();
			lines.add("<html><body>");
			lines.add(msg + " for " + goMap.size() + " GOs");
			
			String x = "<br> All Paths : " + stat[0];
			if (nType==LONGEST_PATHS) x += "&nbsp;&nbsp;&nbsp;Longest: " + stat[1];
			lines.add(x + "&nbsp;&nbsp;&nbsp;GOs: " + allGOs.size());	
			
			lines.add("<table border=1>"  + "\n");
			String line="<tr><th>&nbsp;";
			for (int i=1; i<=maxLevel; i++) line += "<th>Level " + i;
			lines.add(line  + "\n");
			
			int cnt=1;
			for (int [] row : prtPaths) { 
				line = "<tr><td>" + cnt;
				cnt++;
				for (int gonum : row) {
					String godesc = "";
					if (gonum!=NOLEV) {
						if (allGOs.containsKey(gonum)) {
							GOterm gt = allGOs.get(gonum);
							godesc = (nInfo == INFO_DESC) ? 
									gt.desc : String.format(GO_FORMAT, gt.gonum); 
							godesc += gt.msg;
							
							if (gt.msg.equals("*")) godesc = "<i>" + godesc + "</i>";
						}
						else Out.debug("no " + gonum);
					}
					line += "<td width=50>" + godesc;
				}
				lines.add(line);
			}
			
			lines.add("</table><p>&nbsp;* From GO table&nbsp;&nbsp;&nbsp;Relations: is_a or part_of</body></html>");
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Paths html");}
		return null;
	}
	private Vector <String> goPathsTsv(TreeSet <Integer> goMap) {
		try {
			pathCreate(goMap);
			
			Vector <String> lines = new Vector <String> ();
			
			String line = "";
			for (int i=1; i<=maxLevel; i++) line += "Level" + i + Globalx.CSV_DELIM;
			lines.add(line);
			
			for (int [] row : prtPaths) { 
				line = "";
				for (int gonum : row) {
					String godesc = "";
					if (gonum!=NOLEV) {
						if (allGOs.containsKey(gonum)) {
							GOterm gt = allGOs.get(gonum);
							godesc = (nInfo == INFO_DESC) ? 
									gt.desc : String.format(GO_FORMAT, gt.gonum); 
							godesc += gt.msg;
						}
						else Out.debug("no " + gonum);
					}
					line +=  godesc + "\t";
				}
				lines.add(line);
			}	
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "All paths . tsv");}
		return null;
	}
	
	private int []  pathCreate(TreeSet <Integer> goMap) {
		try {
			int [] stat = {0,0};
			
			DBConn mDB = theMainFrame.getNewDBC();
			
			for (int gonum : goMap) { 	// allPaths - creates TreeMap <Integer, GOterm> allGOs of unique GOs
				go_allPaths(mDB, gonum);	
			}
			stat[0] = allPaths.size(); // ancestors and children
			
			if (nType==LONGEST_PATHS) {
				go_MergeAllPaths();
				stat[1] = allPaths.size();
			}
			
			go_allGOs(mDB); 					// allGOs<GOterm> - add desc and levels
			
			for (int gonum : goMap) {
				if (allGOs.containsKey(gonum)) {
					GOterm gt = allGOs.get(gonum);
					gt.setMsg("*");         // in GO annotation table
				}
			}
			mDB.close();
		
			pathSort();
			
			return stat;
		} 
		catch(Exception e) {ErrorReport.prtReport(e, "Create Paths");}
		return null;
	}
	/*********************************
	 * sort allPaths by first element and length
	 * go though the rows, 
	 * 		first adding all contained
	 * 		then add one with longest prefix
	 */
	final private int NOLEV=-1;
	
	private void pathSort() {
		try {
			int nRow = allPaths.size();
			prtPaths = new int[nRow][maxLevel];
			
			for (int i=0; i<nRow; i++) {
				Vector <Integer> path = allPaths.get(i);
				for (int k=0; k<maxLevel; k++) prtPaths[i][k] = NOLEV;
				
				
				for (int gonum : path) {
					GOterm x = allGOs.get(gonum);
					prtPaths[i][x.level-1]= gonum;
				}
			}
			allPaths.clear();
			
			String [] code = new String [nRow];
			for (int i=0; i<nRow; i++) {
				code[i]="";
				for (int j=0; j<maxLevel; j++)
					code[i] += prtPaths[i][j];
			}
			
			for (int i=0; i<nRow-1; i++) {
				for (int j=i+1; j<nRow; j++) {
					if (code[i].compareTo(code[j])<0) {
						int [] tmp = prtPaths[i];
						prtPaths[i]=prtPaths[j];
						prtPaths[j]=tmp;
						
						String c = code[i];
						code[i] = code[j];
						code[j] = c;
					}
				}
			}
			
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Sort Paths");}
	}
	
	private void go_MergeAllPaths() {
		Vector <Boolean> keep = new Vector <Boolean> ();
		for (int i=0; i<allPaths.size(); i++) keep.add(true);
			
		for (int i=0; i<allPaths.size()-1; i++) {
			Vector <Integer> path1 = allPaths.get(i);
			if (!keep.get(i)) continue;
			
			for (int j=i+1; j<allPaths.size(); j++) {
				Vector <Integer> path2 = allPaths.get(j);
				if (path1.size()==path2.size()) continue;
				if (!keep.get(j)) continue;
				
				if (path1.size()>path2.size()) {
					if (go_MergeContained(path1, path2)) keep.set(j, false);
				}
				else if (path2.size()>path1.size()) {
					if (go_MergeContained(path2, path1)) {
						keep.set(i, false);
						break;
					}
				}
			}
		}
		Vector <Vector <Integer>> newPaths = new Vector <Vector <Integer>> ();
		for (int i=0; i<allPaths.size(); i++) {
			Vector <Integer> path1 = allPaths.get(i);
			if (!keep.get(i)) continue;
			newPaths.add(path1);
		}
		
		allPaths.clear();
		allPaths = newPaths;
	}
	// every element in path2 must be in path1 in the same order, though some may be missing
	//0. 5575;5623;44464;12505;5783;
	//1. 5575;     44464;12505;5783;
	private boolean go_MergeContained(Vector <Integer> path1, Vector <Integer> path2) {
		int cnt=0, i2=0, last=0;
		for (int i1=0; i1<path1.size(); i1++) {
			i2=last;
			int g1 = path1.get(i1); 
			while (i2<path2.size()) {
				int g2 = path2.get(i2);
				if (g1 != g2) i2++;
				else {
					last=i2+1;
					cnt++;
					break;
				}
			}
		}
		if (cnt==path2.size()) return true;
		return false;
	}
	}

	private int maxLevel=0;
	private HashMap <Integer, GOterm> allGOs = new HashMap <Integer, GOterm> ();
	private Vector <Vector <Integer>> allPaths = new Vector <Vector <Integer>> (); // vector of go terms for each path
	private int [][] prtPaths=null;
	
	private HashMap <Integer, String> typeMap = new HashMap <Integer, String> ();
	private HashMap <Integer, Integer> ancMap=null, descMap=null;
	
	private boolean bSORT_BY_DIST = false; // not used
	private STCWFrame theMainFrame = null;
}
