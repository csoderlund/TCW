package sng.viewer.panels.Basic;

/*******************************************************
 * Creates the 'Show' and 'Table' outputs for GO, e.g. ancestors, descendants, and hit lists
 * BasicGOQueryTab, ContigGOPanel and BasicQueryTab
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Stack;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
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

import sng.viewer.STCWFrame;
import sng.database.MetaData;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileWrite;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class GOtree {
	private static final String GO_FORMAT = Globalx.GO_FORMAT;
	private static final String GO_PATH_FILE = "GOallPaths"; 
	private static final String GO_LONG_FILE = "GOallLongestPaths";
	private static final String GO_ANC_FILE =  "GOallAncestors";
	private static final String GO_PAR_FILE =  "GOeachParents";
	
	public static final int DO_POPUP=1;
	public static final int DO_EXPORT_GO=2;
	public static final int DO_EXPORT_ALL=3;
	public static final int DO_EXPORT_ASK=4;
	
	public static final int DO_HIGH_RELATED=1;
	public static final int DO_HIGH_ANC=2;
	public static final int DO_HIGH_DESC=3;
	
	// GO basic table
	public static final int ALL_PARENTS=0;
	public static final int ALL_ANCESTORS=1;
	public static final int LONGEST_PATHS=2;
	public static final int ALL_PATHS=3;
	
	// Selected
	public static final int ANCESTORS=4;
	public static final int DESCENDANTS=5;
	public static final int NEIGHBORS=6;
	public static final int RELATED=7;
	public static final int PATHS=8;
	
	public static final int GO_HITS=9;
	public static final int GO_ALL_HITS=10;
	public static final int GO_SEQ=11;
	
	public static final int HIT_ASSIGNED = 12;
	public static final int HIT_ALL = 13;
	
	public static final int SEQ_ASSIGNED=14;
	public static final int SEQ_ALL=15;
	
	// these used for titles, and for file names with replace(" ", "-"); indexed above
	// last word indicates id or lines type
	private static final String [] action = { 
		"All Parent Relation",
		"All Ancestor Set", 
		"All Longest GO Path", 
		"All GO paths", 
		
		"Ancestor GO", 
		"Descendant GO", 
		"Neighbor GO", 
		"Related GO",
		"Ancestor Paths", 
		
		"GO Assigned Hit", 
		"GO Inherited Hit", 
		"GO Hit Seq",
		
		"Hit Assigned GO", 
		"Hit All GO",
		
		"Seq Assigned GO",
		"Seq Inherited GO"
	};
	
	private static final String altID = "alt_id";
	private static final String replacedBy = "Replaced by";
	private static final String alternateID = "Alternate ID";
	
	private boolean bSortByLevel=true;
	
	public GOtree(STCWFrame f) {
		theMainFrame = f;
		goTermMap = Static.getGOtermMap();
	}
	
	/** 
	 * BasicGO, BasicHit, SeqDetail selected options
	 ***/
	// CAS324 SeqDetail - sequence specified
	public void computeSelected(Vector <String> lines, int gonum,  
			int nActionType, int nOutType, JButton fromBtn) {
		try {
			int ans=AllorID.ALL;
			
			if (nOutType==DO_EXPORT_ASK) {
				AllorID obj =  new AllorID("All or ID");
				obj.setVisible(true);
				ans = obj.getSelection();
				if (ans==AllorID.CANCEL) return;
			}
			String gostr = String.format(GO_FORMAT, gonum);
			String name = action[nActionType].replace(" ", "_");
			String filename = gostr + "_" + name;
			
			doOutAction(lines, nActionType, nOutType, ans, filename, fromBtn);
		}
		catch (Exception e) {ErrorReport.prtReport(e, action[nActionType]);}		
	}
	// CAS324 Basic Hit table
	public void computeSelected(String hitID, String desc, int nActionType, int nOutType, JButton fromBtn) {
		try {
			int ans=AllorID.ALL;
			
			if (nOutType==DO_EXPORT_ASK) {
				AllorID obj =  new AllorID("All or ID");
				obj.setVisible(true);
				ans = obj.getSelection();
				if (ans==AllorID.CANCEL) return;
			}
			
			String name = action[nActionType].replace(" ", "_");
			String filename = hitID + "_" + name;
			
			Vector <String> lines = null;
			if (nActionType==HIT_ASSIGNED) lines = hitGoList(hitID, desc); 
			else 					 lines = hitGoListAll(hitID, desc);
			
			doOutAction(lines, nActionType, nOutType, ans, filename, fromBtn);
		}
		catch (Exception e) {ErrorReport.prtReport(e, action[nActionType]);}	
	}
	// First 4 are Basic GO and SeqDetail, the last three or Basic GO
	// CAS324 removed thread - makes it hang easy
	public void computeSelected(int gonum, String desc, int nActionType, int nOutType, JButton fromBtn) {
		try {
			int ans=AllorID.ALL;
			if (nOutType==DO_EXPORT_ASK) {
				AllorID obj =  new AllorID("All or ID");
				obj.setVisible(true);
				ans = obj.getSelection();
				if (ans==AllorID.CANCEL) return;
			}
			
			String gostr = String.format(GO_FORMAT, gonum);
			String name = action[nActionType].replace(" ", "_");
			String filename = gostr + "_" + name;
			
			Vector <String> lines = null;
			if (nActionType==ANCESTORS) {
				lines = computeGoAncestorAsk(gonum, desc);
			}
			else if (nActionType==DESCENDANTS) {
				lines = computeGoDescendantAsk(gonum, desc);
			}
			else if (nActionType==NEIGHBORS) {
				lines = computeGoNeighborsList(gonum, desc);
			}
			else if (nActionType==PATHS) {
				lines = computeGoAncPathsHtml(gonum, desc);
			}
			else if (nActionType==GO_HITS) { // Basic GO - SeqDetail does it own
				lines = computeHitsAssignedAsk(gonum, desc);
			}
			else if (nActionType==GO_ALL_HITS) {// Basic GO - SeqDetail does it own
				lines = computeHitsInheritedAsk(gonum, desc);
			}
			else if (nActionType==GO_SEQ) {// Basic GO
				lines = computeHitGoSeqAsk(gonum, desc);
			}
			
			doOutAction(lines, nActionType, nOutType, ans, filename, fromBtn);
		}
		catch (Exception e) {ErrorReport.prtReport(e, action[nActionType]);}
	}
	/************************************************************
	 * nActionType = PATHS, popup is html or Mono
	 * nOutType = DO_EXPORT_ASK, if asn=ALLorID.ID, skip # and print first item
	 */
	private void doOutAction(Vector <String> lines, int nActionType, int nOutType, int ans, String fileName, JButton btnFrom) {
		try {
			if (nOutType==DO_POPUP) { 
				if (lines==null) {
					JOptionPane.showMessageDialog(null, fileName);
				}
				else {
					String [] alines = new String [lines.size()];
					lines.toArray(alines);
					if (nActionType==PATHS) UserPrompt.displayHTML(fileName, alines);
					else 					UserPrompt.displayInfoMonoSpace(null, fileName, alines);
				}
				return;
			}
			/********************************************************/
			String f = fileName.replace(":","_"); // linux filenames can not have ':'
			
			String fName;
			int ftype;
			if (nOutType==DO_EXPORT_ASK && ans==AllorID.ID) {
				fName = f+"_ids";
				ftype = FileC.fTXT;
			}
			else {
				fName = f+"_lines";
				ftype = (nActionType==PATHS) ? FileC.fHTML : FileC.fTXT; // CAS324 TSV->TXT
			}			

			FileWrite fw = new FileWrite(FileC.bNoVer, FileC.bDoPrt); // XXX CAS316
			File exportFH = fw.run(btnFrom, fName,  ftype, FileC.wAPPEND);
			if (exportFH==null) return; // user cancelled or some problem
			
			Out.PrtSpMsg(0, "Writing " + exportFH.getName() + " ...");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportFH)));
			int cnt=0;
			
			if (nOutType==DO_EXPORT_ASK && ans==AllorID.ID) {
				boolean header=true;
				for (String l : lines) {
					if (l.startsWith("#")) continue;
					if (l.trim().contentEquals("")) continue;
					if (header) {// headings
						header=false;
						continue;
					}
					String [] tok = l.split("\\s+");
					pw.println(tok[0]);
					cnt++;
				}
			}
			else  {
				for (String l : lines) {
					pw.println(l);
					cnt++;
				}
			}
			pw.close();
			Out.PrtSpMsg(1, "Wrote " + cnt + " lines");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in seleciton"); return;}
	}
	
	/****************************************************
	 * Show Related
	 */
	public void goRelatedPopup(int gonum, String desc, int [] set, 
			int nOutMode, JButton fromBtn, BasicGOTablePanel goTabObj) {
		
		try {
			String gostr = String.format(GO_FORMAT, gonum);
			String label = gostr + "_" + action[RELATED];
				
			Vector <String> lines = goRelatedInTable(gonum, desc, set, goTabObj);
			
			doOutAction(lines, RELATED, nOutMode, AllorID.ALL, label, fromBtn);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}
	}
	/*********************************************************
	 * XXX GO input - GO list - fast searches using go_graph_path
	 */
	private Vector <String> goRelatedInTable(int gonum, String godesc, int [] goset,
			   BasicGOTablePanel goTabObj) {
		try {
			// Get all ancestors, then only keep ones from goset
			ancMap = new HashMap <Integer, Integer> ();
			Vector <String> ancLines = computeGoAncestorAsk(gonum, godesc); 
			
			
			Vector <String> ancTab = new Vector <String> ();
			
			for (int row=0; row<goset.length; row++) {
				int go = goset[row];
				if (ancMap.containsKey(go)) {
					int idx = ancMap.get(go);
					ancTab.add(ancLines.get(idx));
				}
			}
			
			// Get all descendants, then only keep ones from goset
			descMap = new HashMap <Integer, Integer> ();
			Vector <String> descLines = computeGoDescendantAsk(gonum, godesc);
			
			Vector <String> descTab = new Vector <String> ();
			
			for (int row=0; row<goset.length; row++) {
				int go = goset[row];
				if (descMap.containsKey(go)) {
					int idx = descMap.get(go);
					descTab.add(descLines.get(idx));
				}
			}
			
			Vector <String> lines = new Vector <String> ();
			
			lines.add(String.format("Anc:Desc %d:%d of %d:%d  ", 
					ancTab.size(), descTab.size(), ancMap.size(), descMap.size()) + 
					String.format(GO_FORMAT, gonum) + " - " + godesc);
			
			if (ancTab.size()>0) {
				lines.add("");
				lines.add("Ancestors in table");
				lines.add(String.format("%-10s  %-5s  %s", Globalx.goID, "Level", Globalx.goTerm));		
				for (String g: ancTab) lines.add(g);
			}
			if (descTab.size()>0) {
				lines.add("");
				lines.add("Descendants in table");
				lines.add(String.format("%-10s  %-5s  %s", Globalx.goID, "Level", Globalx.goTerm));		
				for (String g: descTab) lines.add(g);
			}
			
			ancMap.clear();  ancMap=null;
			descMap.clear(); descMap=null;
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	
	/***********************************************
	 * XXX Hit input - GO list
	 */
	private Vector <String> hitGoList(String hitName, String hitdesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("select DUHID from pja_db_unique_hits where hitID='" 
					+ hitName + "'");
			int duhid = 0;
			if (rs.next()) duhid =  rs.getInt(1);
			else return null;
			
			lines.add("# Assigned GOs for " + hitName + " - " + hitdesc);
			lines.add("");
			
			int count=0;
			rs = mDB.executeQuery("SELECT p.gonum, p.EC, g.descr, g.level, g.term_type " +
					"FROM pja_uniprot_go as p " +
					"join go_info as  g on g.gonum=p.gonum " +
					"WHERE p.duhid=" + duhid + " order by g.term_type, g.level, p.gonum"); 
			lines.add(String.format("%-10s %-5s %-3s  %-4s  %s", 
					Globalx.goID, "Level", Globalx.evCode, Globalx.goOnt, Globalx.goTerm));
			while (rs.next()) {
				String go = String.format(GO_FORMAT, rs.getInt(1));
				String ec = rs.getString(2);
				String desc = rs.getString(3);
				String level = "  " + rs.getInt(4);
				String term = goTermMap.get(rs.getString(5));
				String l = String.format("%-10s %-5s %-3s  %-4s  %s", go, level, ec, term, desc);
				lines.add(l);
				count++;
			}
			lines.add("");
			lines.add("# Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	private Vector <String> hitGoListAll(String hitName, String hitdesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery("select DUHID from pja_db_unique_hits where hitID='" 
					+ hitName + "'");
			int duhid = 0;
			if (rs.next()) duhid =  rs.getInt(1);
			else return null;
			
			lines.add("# All GOs for " + hitName + " - " + hitdesc);
			lines.add("");
			
		// Get Assigned
			Vector <Integer> goSet = new Vector <Integer> ();
			HashMap <Integer, String> goMap = new HashMap <Integer, String> ();
			HashSet <Integer> foundSet = new HashSet <Integer> ();
			
			int count=0;
			rs = mDB.executeQuery("SELECT p.gonum, p.EC, g.descr, g.level, g.term_type " +
					"FROM pja_uniprot_go as p " +
					"join go_info as  g on g.gonum=p.gonum " +
					"WHERE p.duhid=" + duhid + " order by g.term_type, g.level, p.gonum"); 
			lines.add(String.format("%-10s %-5s %-3s  %-4s  %s", 
					Globalx.goID, "Level", Globalx.evCode, Globalx.goOnt, Globalx.goTerm));
			while (rs.next()) {
				int gonum = rs.getInt(1);
				String go = String.format(GO_FORMAT, gonum);
				goSet.add(gonum);
				foundSet.add(gonum); // CAS324
				
				String ec = rs.getString(2);
				String desc = rs.getString(3);
				String level = "  " + rs.getInt(4);
				String term = goTermMap.get(rs.getString(5));
				String l = String.format("%-10s %-5s %-3s  %-4s  %s", go, level, ec, term, desc);
				goMap.put(gonum, l);
				count++;
			}
			
		// Get inherited
			
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
					String term = goTermMap.get(rs.getString(4));
					String l = String.format("%-10s %-5s %-3s  %-4s  %s", go2, level, "", term, desc);
					lines.add(l);
					count++;
				}
				lines.add(goMap.get(child));
			}
			lines.add("");
			lines.add("# Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}	
	/**************** GO Ancestors *****************/ 
	 // CAS336 new to highlight ancestors in table
	public HashSet <Integer> computeRelatedForSet(int gonum, int [] goset, int type) {
		HashSet <Integer> inTab = new HashSet <Integer> ();
		HashSet <Integer> related = new HashSet <Integer> ();
		
		try {
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs;
			
			if (type==DO_HIGH_RELATED || type==DO_HIGH_ANC) {
				rs = mDB.executeQuery("select " +
					" p.ancestor " +
					" from go_graph_path as p" +
					" join go_info as g on g.gonum=p.ancestor" +
					" where child=" + gonum + 
					" and relationship_type_id!=3 " + 
					" order by g.level, p.ancestor"); 
			
				while (rs.next()) related.add(rs.getInt(1));
			}
			
			if (type==DO_HIGH_RELATED || type==DO_HIGH_DESC) {
				rs = mDB.executeQuery("Select child, relationship_type_id, i.descr, i.level " +
						" from go_graph_path as p, " +
						" go_info as i " +
						" where p.child=i.gonum and ancestor=" + gonum + 
						" and relationship_type_id!=3 " + 
						" order by i.level, p.child");
				while (rs.next()) related.add(rs.getInt(1));
			}
			for (int go : goset) {
				if (related.contains(go)) 
					inTab.add(go);	
			}
			
			return inTab;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list"); return inTab;}
	}
	private Vector <String> computeGoAncestorAsk(int gonum, String godesc) {
		try {															
			DBConn mDB = theMainFrame.getNewDBC();
			setRelTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + goTermMap.get(rs.getString(1)) + ")"; // CAS322
			
			HashSet <Integer> dups = new HashSet <Integer> (); // can have is_a and part_of
			rs = mDB.executeQuery("select " +
					" p.ancestor, g.descr, g.level " +
					" from go_graph_path as p" +
					" join go_info as g on g.gonum=p.ancestor" +
					" where child=" + gonum + 
					" and relationship_type_id!=3 " + // CAS320
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
			lines.add("# "+ goLines.size() + " Ancestors of " + String.format(GO_FORMAT, gonum) + " - " + godesc + term);
			lines.add("");
			
			if (goLines.size()>0) {
				lines.add(String.format("%-10s  %-5s  %s", 
						Globalx.goID.replace(" ", "-"), "Level",  Globalx.goTerm));	
				
				for (String g : goLines) lines.add(g);
			}
			lines.add("");
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	
	/***************** GO Descendants **************/ 
	private Vector <String> computeGoDescendantAsk(int gonum, String goDesc) {
		try {
			DBConn mDB = theMainFrame.getNewDBC();
			setRelTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + goTermMap.get(rs.getString(1)) + ")"; // CAS322
			
			HashSet <Integer> dups = new HashSet <Integer> (); // can have is_a and part_of
			int offset=3; // For Related, first GO at lines.get(3)
			
			rs = mDB.executeQuery("Select child, relationship_type_id, i.descr, i.level " +
					" from go_graph_path as p, " +
					" go_info as i " +
					" where p.child=i.gonum and ancestor=" + gonum + 
					" and relationship_type_id!=3 " + // CAS320
					" order by i.level, p.child"); 
			
			Vector <String> goLines = new Vector <String> ();
			while (rs.next()) { 
				int gonum2 = rs.getInt(1);
				if (gonum2==gonum) continue;
				if (dups.contains(gonum2)) continue;
				dups.add(gonum2);
				
				String go = String.format(GO_FORMAT, gonum2);
				String desc = rs.getString(3);
				String level = String.format(" %2d", rs.getInt(4));
			
				String l = String.format("%-10s %-5s %s", go, level, desc);
				goLines.add(l);
				if (descMap!=null) descMap.put(gonum2, offset);
				offset++;
			}
			if (rs!=null) rs.close(); 
			
			Vector <String> lines = new Vector <String> ();
			lines.add("#" + goLines.size() + " Descendants of " + String.format(GO_FORMAT, gonum) + " - " + goDesc + term);
			lines.add("");
			
			int rbGO=0;
			if (goLines.size()==0) {
				rbGO = mDB.executeInteger("select parent from go_term2term where child=" + gonum + " and relationship_type_id=3");
				if (rbGO>0) {
					lines.add("# See Replacement " + String.format(GO_FORMAT, rbGO));
				}

			}
			mDB.close();
			
			if (goLines.size()>0) {
				lines.add(String.format("%-10s %-5s %s ", Globalx.goID.replace(" ","-"), 
						"Level", Globalx.goTerm));
				for (String g : goLines) 
					lines.add(g);
			}
			lines.add("");
			
			if (rbGO==0) lines.add("# Descendants are only shown if they have a hit in the sTCWdb");
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}

		return null;
	}
	/**************** GO neighbor list **************************/
	private Vector <String> computeGoNeighborsList(int gonum, String godesc) {
		try {															
			DBConn mDB = theMainFrame.getNewDBC();
			setRelTypes(mDB);
			
			ResultSet rs=mDB.executeQuery("select term_type from go_info where gonum=" + gonum);
			String term="";
			if (rs.next()) term = "  (" + goTermMap.get(rs.getString(1)) + ")"; // CAS322
			
			Vector <String> tmpLines = new Vector <String> ();
			
			bSortByLevel=false;
			String rType;
			HashMap <Integer, GOterm> neighGOs = new HashMap <Integer, GOterm> (); // is sorted below
			HashSet <Integer> altId = new HashSet <Integer> ();
			int repId = 0;
			for (int i=0; i<2; i++) { // 0=parent, 1=child
				if (i==0) {
					rType = "Parents: ";
					rs = mDB.executeQuery("select t.parent, t.relationship_type_id, i.descr " +
						" from go_term2term as t, go_info as i" +
						" where parent>0 and child=" + gonum + " and t.parent=i.gonum"); 
				}
				else {
					neighGOs.clear(); // CAS318
					rType = "Children: ";
					rs = mDB.executeQuery("select t.child, t.relationship_type_id, i.descr " +
						" from go_term2term as t, go_info as i" +
						" where child>0 and parent=" + gonum + " and t.child=i.gonum"); 
				}
				while (rs.next()) { 
					int gonum2 = rs.getInt(1);
					String type = relTypeMap.get(rs.getInt(2));
					String desc = rs.getString(3);
					if (type.equals(altID)) {
						if (i==1) altId.add(gonum2); // CAS320 - replace_by
						else repId = gonum2;
					}
					else if (neighGOs.containsKey(gonum2)) {
						GOterm gt = neighGOs.get(gonum2);
						if (!type.equals(gt.rtype)) gt.rtype="both";
					}
					else {
						neighGOs.put(gonum2,  new GOterm(gonum2, type, desc));
					}
				}
				if (rs!=null) rs.close(); 
				mDB.close();
															// CAS318 added sort by description to match Amigo order
				Vector <GOterm> list = new Vector <GOterm> ();
				for (GOterm g : neighGOs.values()) list.add(g);
				Collections.sort(list);
				
				tmpLines.add(rType + neighGOs.size());
				if (neighGOs.size()==0) tmpLines.add("None");
				else { 										// CAS318 put relation first
					for (GOterm gt : list) { 
						String type = gt.rtype;
						String go = String.format(GO_FORMAT, gt.gonum);
						String l = String.format("%-11s  %-10s  %s", type, go,  gt.desc);
						tmpLines.add(l);
					}
				}
				neighGOs.clear();
				tmpLines.add("");
			}
			bSortByLevel=true;
			
	// final lines	- CAS320 want to put alt_id stuff first
			Vector <String> lines = new Vector <String> ();
			
			lines.add("# Neighborhood of " + String.format(GO_FORMAT, gonum) + " - " + godesc + term);
			lines.add("");	
		
			if (altId.size()>0) {
				String msg = alternateID + ": ";
				for (int x : altId) msg += String.format(GO_FORMAT, x) + " ";
				lines.add(msg);
				lines.add("");
			}
			if (repId>0) {
				String msg = replacedBy +": " + String.format(GO_FORMAT, repId);
				lines.add(msg);
				lines.add("");
			}
			for (String t : tmpLines) lines.add(t);
			if (repId==0) lines.add("# Children only shown if they have a hit in the sTCWdb");
			else lines.add("# See 'Replaced by: GO' for children");
			if (altId.size()>0) lines.add("# Alternate IDs only shown if have a hit in the sTCWdb");	
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for Ancestor list");}
		return null;
	}
	
	/*********************************************************
	 * GO input - GO num - slow searches using recurse on term2term
	 */
	/**** GO Paths - Basic GO and Seq GO ***/
	private Vector <String> computeGoAncPathsHtml(int gonum, String desc) {
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
	private Vector <String> computeHitsAssignedAsk(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("# Hits with assigned " + String.format(GO_FORMAT, gonum) + " - " + godesc);
			lines.add("");
			String line = String.format("%-16s  %-30s  %-20s  %-3s", 
					"Hit-ID",  "Description", "Species", Globalx.evCode );
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
				if (desc.length()>30) desc = desc.substring(0,28) + "..";
				String spec = rs.getString(4);
				if (spec.length()>20) spec = spec.substring(0,18) + "..";
				String l = String.format("%-16s  %-30s  %-20s  %3s", 
					rs.getString(1), desc, spec, rs.getString(2));
				lines.add(l);
				count++;
			}
			lines.add("");
			lines.add("# Count: " + count);
			rs.close(); mDB.close(); 
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/***  GO Hit Inherited List **/ 
	private Vector <String> computeHitsInheritedAsk(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("# Hits with inherited " + String.format(GO_FORMAT, gonum) + " - " + godesc);
			lines.add("");
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
			// get descendants of this GO
			Vector <GOterm> goDescList = new Vector <GOterm> ();
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
					goDescList.add(new GOterm(gonum2, level, dist, desc));
				}
			}
			rs.close();
			
			if (goDescList.size()==0) {
				lines.add("# No descendants for " + String.format(GO_FORMAT, gonum));
				mDB.close();
				return lines;
			}
			
			int count=0, cntDup=0;
			String line = String.format("%-16s  %-30s  (%-3s)  %-10s %-5s %-30s", 
					"Hit-ID",  "Description", Globalx.evCode,  "Descendant", "Level", Globalx.goTerm);
			lines.add(line);

			// get hits to the descendants
			HashSet <String> found = new HashSet <String> (); // CAS324
			for (GOterm gt : goDescList) {
				String rd = String.format(GO_FORMAT,  gt.gonum);
				String level = String.format(" %2d", gt.level);
				if (gt.desc.length()>30) gt.desc = gt.desc.substring(0,28) + "..";
				
				rs = mDB.executeQuery("SELECT up.hitID, g.EC, up.description " +
						"FROM pja_uniprot_go as g " +
						"join pja_db_unique_hits as up on g.DUHID=up.DUHID " +
						"WHERE g.gonum=" + gt.gonum + " order by up.hitID");
				
				while (rs.next()) {
					String hitID = rs.getString(1);
					if (found.contains(hitID)) {
						cntDup++;
						continue;
					}
					found.add(hitID);
					
					String ec = rs.getString(2);
					String desc = rs.getString(3);
					if (desc.length()>30) desc = desc.substring(0,28) + "..";
					
					String l = String.format("%-16s  %-30s  (%-3s)  %-10s  %-3s  %-30s", 
						 hitID,   desc, ec, rd, level, gt.desc);
					lines.add(l);
					count++;
				}
			}
			if (rs!=null) rs.close();
			mDB.close(); 
			
			lines.add("");
			lines.add("# Count: " + count);
			if (cntDup>0) lines.add("# Hits with multiple descendant GOs: " + cntDup);
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	/**********************************************************
	 * Sequences with hit with for GO - both assigned and inherited
	 * CAS324 The 'HIT' shown is not meaningfule
	 ***/ 
	private Vector <String> computeHitGoSeqAsk(int gonum, String godesc) {
		try {
			Vector <String> lines = new Vector <String> ();
			lines.add("# Sequences with at least one hit with " + String.format(GO_FORMAT, gonum) + " - " + godesc);
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
				lines.add("# No sequences for this GO - not possible");
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
					"Seq-ID", "A Hit", "E-val", "Is BS", "Is AN", Globalx.evCode);
			lines.add(line);

			for (SeqHit sh : hitList) {
				String eval = String.format("%.0E", sh.eval);
				String l = String.format("%-15s  %-20s  %6s  %5s  %5s  %3s", 
					sh.seqName, sh.hitName, eval, sh.bestEval, sh.bestAnno,  sh.ec);
				lines.add(l);
				count++;
			}
			
			lines.add("");
			lines.add("# Count: " + count);
			lines.add("");
			lines.add("# Note: The hit information may not be useful, but see Help to understand it.");
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "query for hits");}
		return null;
	}
	
	
	/****************** Map is_a and part_of to index *****************/
	private void setRelTypes(DBConn mDB) {
		try {
			TreeMap <String, Integer> relMap = MetaData.getGoRelTypes(mDB);
			
			relTypeMap.put(0, "both");
			for (String rel : relMap.keySet()) 
				relTypeMap.put(relMap.get(rel), rel);
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
		GOterm (int gonum, String rtype,  String desc) { // go neighbors
			this.gonum = gonum;
			this.rtype = rtype; // is_a, part_of
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
			this.term = goTermMap.get(term);
			this.desc = desc;
		}
		public void setMsg(String msg) {this.msg = msg;}
		
		public int compareTo(GOterm t) { 
			if (!bSortByLevel) { // CAS318 for neighbors - to match amigo display
				return this.desc.compareToIgnoreCase(t.desc);
			}
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
		String desc= "", rtype="", term="", msg="";
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
	
	/**************************************************************
	 * GO annotation: Table... methods
	 */
	public void goPopupExportList(Component btnC, int showType, int mode, TreeSet <Integer> goMap) {
		try {		
			AllDialog allObj = new AllDialog(showType, mode, btnC);
			if (allObj.isNotCancelled()) allObj.doAction(goMap);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}	
	}
	public void goPopupExportPath(Component btnC, int showType, int mode, TreeSet <Integer> goSet) {
		try {
			AllDialog allObj = new AllDialog(showType, mode, btnC);
			allObj.setVisible(true);
			
			if (allObj.getSelection()) allObj.doAction(goSet);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "GO query failed");}		
	}
/******************************************************
 * Basic GO Table options
 */
private class AllDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private final int DIA_POP=1, DIA_WRITE=2, DIA_CANCEL=0;
	private final int INFO_DESC=1, INFO_TERM=2;
	private final int OUT_HTML=1, OUT_TSV=2;

	// for paths only
	private int dMode = DIA_WRITE;
	private int nInfo = INFO_DESC; // 1=descriptions 2=GO ID
	private int dOut  = OUT_TSV;  
	
	private int nMode; 
	private int nType;   //  parameter all, paths, anc
	private String msg="", fName="";
	private File exportFH=null;
	private Component fileBtn;
	
	/****************************************************
	 * Basic GO - table operations
	 * The All Paths and Longest paths have popup for GO Name or GO ID
	 */
	public AllDialog(int type, int mode,  Component btn) {
		nType = type;
		nMode = mode;
		fileBtn = btn;
		dMode = (nMode==DO_POPUP) ? DIA_POP : DIA_WRITE;
		
		if      (nType==ALL_PARENTS)  	{msg = "All parents";   fName=GO_PAR_FILE;}
		else if (nType==ALL_ANCESTORS)  {msg = "All ancestors"; fName=GO_ANC_FILE;}
		else if (nType==ALL_PATHS) 		{msg = "All paths";     fName=GO_PATH_FILE;}
		else                       		{msg = "Longest paths"; fName=GO_LONG_FILE;}
		
		Out.Print("Starting " + msg);
		
		if (nType==ALL_PATHS || nType==LONGEST_PATHS) {
			pathDialog();
		}
		else if (nMode!=DO_POPUP)  { 
			dOut = OUT_TSV;
			setFile();
		}
	}
	private void pathDialog() {
		setModal(true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setTitle(msg);
		
		JPanel selectPanel = Static.createPagePanel();
		
		JRadioButton btnDesc = Static.createRadioButton(Globalx.goTerm, true);
		btnDesc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nInfo = INFO_DESC;
			}
		});
		JRadioButton btnTerm =  Static.createRadioButton(Globalx.goID, false);
    	btnTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				nInfo = INFO_TERM;
			}
		});
    	ButtonGroup grp2 = new ButtonGroup();
		grp2.add(btnDesc);
		grp2.add(btnTerm);
    		
		selectPanel.add(btnDesc); selectPanel.add(Box.createHorizontalStrut(10));
		selectPanel.add(btnTerm);
		
		dOut = OUT_HTML; // path defaults to html
		if (dMode == DIA_WRITE) {	
			selectPanel.add(new JSeparator());
			
			JRadioButton btnHtml =  Static.createRadioButton("HTML file (.html)", true);
	    	btnHtml.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					dOut =  OUT_HTML;
				}
			});
			selectPanel.add(btnHtml);
			selectPanel.add(Box.createVerticalStrut(5));
			
			JRadioButton btnTsv =  Static.createRadioButton("Tab-delimited file (.tsv)", false);
	    	btnTsv.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					dOut = OUT_TSV;
				}
			});
	    	selectPanel.add(btnTsv);
			selectPanel.add(Box.createVerticalStrut(5));
			
			ButtonGroup grp1 = new ButtonGroup();
			grp1.add(btnTsv);
			grp1.add(btnHtml);
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
				dMode=DIA_CANCEL;
				setVisible(false);
			}
		});
		
		btnOK.setPreferredSize(btnCancel.getPreferredSize());
		btnOK.setMaximumSize(btnCancel.getPreferredSize());
		btnOK.setMinimumSize(btnCancel.getPreferredSize());
		
		JPanel buttonPanel = Static.createRowPanel();
		buttonPanel.add(btnOK);		buttonPanel.add(Box.createHorizontalStrut(20));
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
	private boolean getSelection() {
		try {
			if (dMode==DIA_CANCEL) {
				return false;
			}
			if (dMode==DIA_WRITE) {
				return setFile();
			}
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in selection"); return false;}
	}
	private boolean setFile() {
		try {
			FileWrite fw = new FileWrite(FileC.bNoVer, FileC.bDoPrt); // CAS314
			int ftype = (dOut==OUT_TSV) ? FileC.fTSV : FileC.fHTML;
			
			exportFH = fw.run(fileBtn, fName, ftype, FileC.wONLY);
			if (exportFH==null) {
				dMode=DIA_CANCEL;
				return false;
			}
			
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error getting file"); return false;}
	}
	private boolean isNotCancelled() { return (dMode!=DIA_CANCEL);}
	
 	private boolean doAction(TreeSet <Integer> goMap) {
		try {
			
			Vector <String> lines = null;
			if (nType==ALL_PARENTS) {
				lines = goAllParents(goMap);
			}
			else if (nType==ALL_ANCESTORS) {
				lines = goAllAncestors(goMap);
			}
			else  { // ALl paths
				if (dOut==OUT_HTML) lines=goPathsHTML(goMap);
				else                lines=goPathsTsv(goMap);
			}
			if (lines==null) {
				JOptionPane.showMessageDialog(null, "No ouput");
				return false;
			}
		
			if (dMode==DIA_POP) { 
				String [] alines = new String [lines.size()];
				lines.toArray(alines);
				if (nType==ALL_ANCESTORS || nType==ALL_PARENTS)
					UserPrompt.displayInfoMonoSpace(null, msg, alines); // adds nl
				else 
					UserPrompt.displayHTML(msg, alines); // do not need nl
			}
		
			if (dMode==DIA_WRITE) {
				Out.Print("Writing " + lines.size() + " lines ...");
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(exportFH)));
			
				for (String l : lines) pw.println(l);
		
				pw.close();
				Out.Print("Complete writing to " + exportFH.getName());
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Error in action");}
		return false;
	}
	/* CAS319 add - show all parents and relations */
	private Vector <String> goAllParents(TreeSet <Integer> goSet) {
		try {	
			Vector <String> lines = new Vector <String> ();
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs=null;
		
			Vector <GOterm> goList = new Vector <GOterm> ();
			
			for (int gonum : goSet) {
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
				}
				else Out.PrtError("TCW error on gonum=" + gonum);
			}
			
			setRelTypes(mDB);
			
			String delim = (dMode==DIA_POP) ? " "  : FileC.TSV_DELIM; 
			String format = "%-11s" + delim + "%10s" + delim + "%3s" + delim + "%s";
			
			int cnt=0, cnt1=0;
			for (GOterm gt : goList) {
				rs = mDB.executeQuery("select " +
						" p.relationship_type_id, p.parent, g.descr, g.term_type " +
						" from go_term2term as p" +
						" join go_info as g on g.gonum=p.parent" +
						" where child=" + gt.gonum); 
				
				String go = String.format(GO_FORMAT, gt.gonum);
				lines.add(String.format(format, go, "", gt.term, gt.desc));
				while (rs.next()) {
					int gonum2 = rs.getInt(2);
					if (gonum2==gt.gonum) continue;
					go = String.format(GO_FORMAT, gonum2);
					
					int rel = rs.getInt(1);
					String x = "  " + ((relTypeMap.containsKey(rel)) ? relTypeMap.get(rel) : "unk");
					if (x.contentEquals(altID)) x = replacedBy;
					
					String desc = rs.getString(3);
					
					String term = goTermMap.get(rs.getString(4));
					
					lines.add(String.format(format, x, go, term, desc));
					cnt++; cnt1++;
				}
				if (cnt1>=1000) {
					Out.r("parents " + cnt);
					cnt1=0;
				}
			}
			if (rs!=null) rs.close(); mDB.close();
			
			
			return lines;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Parent list");}
		return null;
	}
	/* All ancestors, term_type, level, desc */
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
			
			String delim = (dMode==DIA_POP) ? " "  : FileC.TSV_DELIM; 
			String format = "%-11s" + delim + "%6s" + delim +"%-5s" + delim + "%s";
			String goid = (dMode==DIA_POP) ? Globalx.goID : Globalx.goID.replace(" ", "-");
			String goterm = (dMode==DIA_POP) ? Globalx.goTerm : Globalx.goTerm.replace(" ", "-");
			lines.add(String.format(format, goid,  Globalx.goOnt, "Level", goterm));
			
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
	private Vector <String> goPathsHTML(TreeSet <Integer> goMap) {
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
						else Out.bug("no " + gonum);
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
			for (int i=1; i<=maxLevel; i++) line += "Level" + i + FileC.TSV_DELIM;
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
						else Out.bug("no " + gonum);
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
			Out.r("All Paths " + allPaths.size());
			
			if (nType==LONGEST_PATHS) {
				go_MergeAllPaths();
				stat[1] = allPaths.size();
				
				Out.r("Merged Paths " + allPaths.size());
			}
			
			go_allGOs(mDB); 					// allGOs<GOterm> - add desc and levels
			
			Out.r("Add levels ");
			for (int gonum : goMap) {
				if (allGOs.containsKey(gonum)) {
					GOterm gt = allGOs.get(gonum);
					gt.setMsg("*");         // in GO annotation table
				}
			}
			mDB.close();
		
			Out.r("Sort ");
			pathSort();
			
			Out.r("Create output ");
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
	} // END OF ALLDialog

	/****************************************************************/
	private class AllorID extends JDialog {
		private static final long serialVersionUID = 1L;
		public static final int ALL=1;
		public static final int ID=2;
		public static final int CANCEL=3;
		
		private int ans=1; // 1 All Info, 2 ID only
		
		public AllorID (String msg) {
			setModal(true);
			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setTitle(msg);
		
			JPanel selectPanel = Static.createPagePanel();
		
			JRadioButton btnAll = Static.createRadioButton("All Info", true);
			btnAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ans = 1;
				}
			});
			JRadioButton btnID =  Static.createRadioButton("ID only", false);
	    	btnID.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					ans = 2;
				}
			});
	    	ButtonGroup grp2 = new ButtonGroup();
			grp2.add(btnAll);
			grp2.add(btnID);
	    		
			selectPanel.add(btnAll); selectPanel.add(Box.createHorizontalStrut(10));
			selectPanel.add(btnID);
			
	        JButton btnOK = Static.createButton("OK", true);
				btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
			JButton btnCancel = Static.createButton("Cancel", true);
			btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ans = 3;
					setVisible(false);
				}
			});
			
			btnOK.setPreferredSize(btnCancel.getPreferredSize());
			btnOK.setMaximumSize(btnCancel.getPreferredSize());
			btnOK.setMinimumSize(btnCancel.getPreferredSize());
			
			JPanel buttonPanel = Static.createRowPanel();
			buttonPanel.add(btnOK);		buttonPanel.add(Box.createHorizontalStrut(20));
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
		private int getSelection() {
			return ans;
		}
	}
	/*************************************************************/
	private int maxLevel=0;
	private HashMap <Integer, GOterm> allGOs = new HashMap <Integer, GOterm> ();
	private Vector <Vector <Integer>> allPaths = new Vector <Vector <Integer>> (); // vector of go terms for each path
	private int [][] prtPaths=null;
	
	private HashMap <Integer, String> relTypeMap = new HashMap <Integer, String> ();
	private HashMap <Integer, Integer> ancMap=null, descMap=null;
	private HashMap <String, String> goTermMap; // CAS322
	
	private boolean bSORT_BY_DIST = false; // not used
	private STCWFrame theMainFrame = null;
}
