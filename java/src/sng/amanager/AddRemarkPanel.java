package sng.amanager;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import sng.database.Globals;
import sng.database.Schema;
import util.database.DBConn;
import util.file.FileC;
import util.file.FileRead;
import util.file.FileHelpers;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UserPrompt;

/************************************************
 * Add remark replaces any existing remark
 * Remove remarks removes all
 * Add file of locations
 */

public class AddRemarkPanel extends JPanel {
	private static final long serialVersionUID = -2526722262077846590L;
	private static final String helpDir = Globals.helpRunDir + "RemarkHelp.html";

	private void addLoc(String fileName) {
		// pattern is also in Library.java and AssemMain; used when sequence names are locations
		Pattern mBEDPat = Pattern.compile("(\\S+):(\\d+)-(\\d+)");  // XXX
		Pattern mGrpPat = Pattern.compile("\\A(\\D+)(\\d+)\\Z");  // beginning (not number) (number) end
		Out.PrtSpMsg(1, "Reading location file " + fileName);
		
		String query="";
		try {
			if (fileName==null || fileName.equals("")) {
				UserPrompt.showError("Enter filename");
				return;
			}

			fileName = FileC.addFixedPath(projName, fileName, FileC.dPROJ);
			if (fileName==null) {
				UserPrompt.showError("Error finding file " + fileName);
				return;
			}
			/************************************
			 * determine if the group name is number, e.g. scaffold
			 */
			boolean hasNum=true;
			int maxNum=-1;
			String prefix="";
			String line;
			BufferedReader file = FileHelpers.openGZIP(fileName);
			while((line = file.readLine()) != null) {
				line.replace("\n","").trim();
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				String [] tok = line.split("\\s+");
				String [] grp = tok[1].split(":");
				Matcher m = mGrpPat.matcher(grp[0]);
				if (m.find()) { 
					String group = m.group(1).toLowerCase();
					int num = Integer.parseInt(m.group(2));
					if (num>maxNum) maxNum=num;
					if (prefix=="") prefix=group;
					else if (!prefix.equals(group)) hasNum=false; // not consistent
				}
			}
			file.close();
			if (maxNum == -1) hasNum=false;
			if (hasNum) 
				Out.PrtSpMsg(2, "Found prefix: " + prefix + " max number " + maxNum);
			
			/*******************************************
			 * Database
			 */
			DBConn mDB = theManFrame.tcwDBConn();
			if (mDB==null) {
				UserPrompt.showError("Cannot open database");
				Out.PrtError("Cannot open database");
				return;
			}
			
			HashMap <String, Integer> nameIDMap = new HashMap <String, Integer> ();
			ResultSet rs = mDB.executeQuery("SELECT CTGID, contigid FROM contig");
			while (rs.next()) {
				nameIDMap.put(rs.getString(2), rs.getInt(1));
			}
			if (nameIDMap.size()==0) {
				Out.PrtError("No sequences in database -- run Instantiate");
				UserPrompt.showError("No sequences in database -- run Instantiate");
				return;
			}
			mDB.executeUpdate("update assem_msg set pja_msg=NULL");
			
			rs = mDB.executeQuery("show columns from contig where field='seq_group'");
			if (!rs.first()) {
				mDB.executeUpdate("alter table contig add seq_group VARCHAR(30) ");
				mDB.executeUpdate("alter table contig add seq_start bigint default 0 ");
				mDB.executeUpdate("alter table contig add seq_end bigint default 0 ");
				mDB.executeUpdate("alter table contig add seq_strand tinytext ");
			}
			mDB.executeUpdate("update contig set seq_group='', seq_start=0,seq_end=0,seq_strand=''");
			
			if (hasNum) {
				mDB.tableCheckAddColumn("contig", "seq_ngroup", "int default 0", "seq_group");
				mDB.executeUpdate("update contig set seq_ngroup=0");
			}
			else {
				mDB.tableCheckDropColumn("contig", "seq_ngroup");
			}
			/*****************************************
			 * Read file
			 */
			int add=0, bad1=0, notfound=0;
			file = FileHelpers.openGZIP(fileName);
			while((line = file.readLine()) != null) {
				line.replace("\n","").trim();
				if (line.equals("")) continue;
				if (line.startsWith("#")) {
					Out.prt(line);
					continue;
				}
				int CID=0;
				String [] tok = line.split("\\s+");
				if (tok==null) continue;
				
				if (nameIDMap.containsKey(tok[0])) CID = nameIDMap.get(tok[0]);
				else {
					if (notfound < 10) 
						Out.PrtWarn("No sequence name in database: " + line);
					else if (notfound==10) Out.PrtWarn("Repress further no sequence found errors");
					notfound++;
					continue;
				}
				
				String loc = tok[1];
				Matcher m = mBEDPat.matcher(loc);
				if (m.find()) { 
					String group = m.group(1);
					int start = Integer.parseInt(m.group(2));
					int end = Integer.parseInt(m.group(3));
					String sense=  (loc.contains("(-)")) ? "-" : "+";
					
					query = "update contig set seq_group='" + group + 
							"', seq_start=" + start + ",seq_end=" + end +  ",seq_strand='" + sense + "'";		
					if (hasNum) {
						int num=0;
						m = mGrpPat.matcher(group);
						if (m.find()) num = Integer.parseInt(m.group(2));
						else if (group.endsWith("X")) num = maxNum+1;
						else if (group.endsWith("Y")) num = maxNum+2;
						else                          num = maxNum+3;
						query += ", seq_ngroup=" + num;
					}
					query += " where CTGID=" + CID; 
					mDB.executeUpdate(query);
					add++;
				} else {
					if (bad1 < 10) Out.PrtWarn("Bad location: " + loc);
					bad1++;	
				}
				int x = add+bad1;
				if (x % 1000 == 0) Out.r("Add " + add); 
			}	
			
			rs = mDB.executeQuery("show columns from assem_msg like 'hasLoc'");
			if (!rs.first()) 
				mDB.executeUpdate("alter table assem_msg add hasLoc tinyint");
			
			rs.close(); mDB.close();
			Out.PrtSpMsg(1, "Added " + add + " locations to the database");
			Out.PrtSpCntMsgZero(1, bad1, "Ignored lines");
			Out.PrtSpCntMsgZero(1, notfound, "No sequence IDs in database ");
		}
		catch (Exception e) {
			if (!query.equals("")) System.out.println("Query: " + query);
			ErrorReport.reportError(e, "Reading location file " + fileName);
		}		
	}
	private void addRemark(String fileName) {
		try {
			if (fileName==null || fileName.equals("")) {
				UserPrompt.showError("Enter filename");
				return;
			}
			Pattern pat = Pattern.compile("(\\S+)\\s+(.*)$"); 
			Out.PrtSpMsg(1, "Reading remark file " + fileName);
			
			fileName = FileC.addFixedPath(projName, fileName, FileC.dPROJ);
			if (fileName==null) {
				UserPrompt.showError("Error finding file " + fileName);
				Out.PrtError("Error finding file " + fileName);
				return;
			}
			DBConn mDB = theManFrame.tcwDBConn();
			if (mDB==null) {
				UserPrompt.showError("Cannot open database ");
				Out.PrtError("Cannot open database");
				return;
			}
			Schema s = new Schema(mDB); // need to do this for 5.2 update
			if (!s.current()) s.update();
			
			HashMap <String, Integer> ctgMap = new HashMap <String, Integer> ();
			HashMap <Integer,String> ctgMap2 = new HashMap <Integer, String> ();
			HashMap<Integer,String> curRems = new HashMap<Integer,String>();
			ResultSet rs = mDB.executeQuery("SELECT CTGID, contigid, user_notes FROM contig");
			while (rs.next()) {
				ctgMap.put(rs.getString(2), rs.getInt(1));
				ctgMap2.put(rs.getInt(1), rs.getString(2));
				String rem = (rs.getString(3) == null ? "" : rs.getString(3).trim());
				if (!rem.equals("")) 
					curRems.put(rs.getInt(1),rem);
			}
			if (ctgMap.size()==0) {
				UserPrompt.showError("No sequences in database -- run Instantiate");
				Out.PrtError("No sequences in database -- run Instantiate");
				return;
			}
				
			BufferedReader file = FileHelpers.openGZIP(fileName); // CAS315
			String line;
			int cnt=0, add=0, append=0, bad1=0, notfound=0;
			
			Vector<String> longRems = new Vector<String>();
			while((line = file.readLine()) != null) {
				line = line.replace("\n","").trim();
				line = line.replace("\t", " "); // the \t causes it to be shown in next column of view table!
				if (line.equals("")) continue;
				if (line.startsWith("#")) {
					Out.prt(line);
					continue;
				}
				cnt++;
				if (cnt % 1000 == 0) Out.r("Read " + cnt);
				if (line.contains("\"") || line.contains("\'")) {
					line = line.replace("\"", " ");
					line = line.replace("\'", " ");
					Out.PrtWarn("Replacing single and double quotes with blanks: " + line);
				}	
				if (line.contains(Globals.tcwDelim)) {
					line = line.replace(";", ":");
					Out.PrtWarn("Replacing semi-colon with colon: " + line);
				}
				Matcher m = pat.matcher(line);
				if (!m.find()) { bad1++; continue;} 
				String name = m.group(1).trim();
				String value = m.group(2).trim();
				int CID=0;
				
				if (ctgMap.containsKey(name)) CID = ctgMap.get(name);
				else {
					String tmp = name;
					Pattern mNameFixPat = Pattern.compile("[^\\w\\.]");
					Matcher mm = mNameFixPat.matcher(tmp);
					name =  mm.replaceAll("_");
					if (tmp.equals(name)) {
						if (notfound < 10) Out.PrtWarn("Error no sequence name in database: " + line);
						notfound++;
						continue;
					}
					else {
						if (ctgMap.containsKey(name)) CID = ctgMap.get(name);
						else {
							if (notfound < 10) Out.PrtWarn("Error no sequence name in database " + tmp + " or " + name);
							notfound++;
							continue;
						}
					}
				}				
				if (curRems.containsKey(CID)) {
					value = value + "; " + curRems.get(CID);
					append++;
				}
				else add++;
				if (value.length() > 250) {
					longRems.add(name);
					value = value.substring(0, 250);
				}
				curRems.put(CID, value);
			}
			file.close();
			
			for (int CID : curRems.keySet())
			{
				String value = curRems.get(CID);
				try {
					mDB.executeUpdate("UPDATE contig SET user_notes='" + value + "'" +
						" WHERE CTGID=" + CID);
		
				} catch (Exception e){
					ErrorReport.die(e, "Error entering remark for " + ctgMap2.get(CID) + " " + value);
				}
				if (cnt%1000 == 0) Out.r("Read " + cnt + "  Add " + add + " Append " + append);
			}
			rs.close(); mDB.close();
			Out.PrtSpMsg(1, "Added " + (add + append) +" remarks to the database");
			Out.PrtSpCntMsgZero(1, bad1, "Ignored lines ");
			Out.PrtSpCntMsgZero(1, notfound, "No sequence IDs in database " );
			if (longRems.size() > 0) {
				Out.PrtSpMsg(1,longRems.size() + " contigs had remarks longer than 250 chars and were truncated:");
				Out.PrtSpMsg(1,Static.strVectorJoin(longRems, ","));
			}
		} catch (Exception e) {
			ErrorReport.reportError(e, "Reading remark file " + fileName);
		}
	}
	
	private void removeRemark() {
		try {
			DBConn mDB = theManFrame.tcwDBConn();
			if (mDB==null) return;
			
			if (!UserPrompt.showConfirm("Remove Remarks", "Remove user remarks")) return;
			if (mDB.tableColumnExists("contig", "user_notes"))
				mDB.executeUpdate("Update contig set user_notes=''");
			mDB.close();
			Out.PrtSpMsg(1, "Removed all remarks");
			
			mDB.close();
		}
		catch (Exception e) {ErrorReport.reportError(e, "Error removing remarks");}
	}
	
	/****************************************************************
	 * Panel 
	 */
	public AddRemarkPanel(ManagerFrame parentFrame) { 
		theManFrame = parentFrame;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		JPanel titleRow = Static.createRowCenterPanel();
		JLabel title = Static.createLabel("Remarks or Locations");
		title.setFont(getFont().deriveFont(Font.BOLD, 18));
		title.setBackground(Color.WHITE);
		titleRow.add(title);
		titleRow.setMaximumSize(titleRow.getPreferredSize());
		titleRow.setMinimumSize(titleRow.getPreferredSize());
		add(titleRow);
		add(Box.createVerticalStrut(50));
		
		// Location
		JPanel locRow = Static.createRowCenterPanel();
		locRow.add(Static.createLabel("Location File"));	locRow.add(Box.createHorizontalStrut(5));
		
		txtLocFile = Static.createTextField("", 25);
		locRow.add(txtLocFile);						locRow.add(Box.createHorizontalStrut(5));
		
		btnLocFile = Static.createButtonFile("...", true);;
		btnLocFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				projName = theManFrame.getProjDir(); // project know when this is selected
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				if (fc.run(btnLocFile, "Location", FileC.dPROJ, FileC.fTXT)) { 
					txtLocFile.setText(fc.getRemoveFixedPath());
				}
			}
		});
		locRow.add(btnLocFile);					locRow.add(Box.createHorizontalStrut(5));
		
		btnAddLoc = Static.createButtonRun("Add", true);
		btnAddLoc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String file=txtLocFile.getText().trim();
				if (!file.equals("")) {
					addLoc(txtLocFile.getText());
				}
			}
		});
		btnAddLoc.setEnabled(true);
		locRow.add(btnAddLoc);
		
		locRow.setMaximumSize(locRow.getPreferredSize());
		locRow.setMinimumSize(locRow.getPreferredSize());
		add(locRow);
		add(Box.createVerticalStrut(20));
		
	// Remarks
		JPanel rmkRow = Static.createRowCenterPanel();
		rmkRow.add(Static.createLabel("Remarks File"));			rmkRow.add(Box.createHorizontalStrut(5));
		
		txtRmkFile = Static.createTextField("", 25);
		rmkRow.add(txtRmkFile);							rmkRow.add(Box.createHorizontalStrut(5));
		
		btnRmkFile = Static.createButtonFile("...", true);
		btnRmkFile.addActionListener(new ActionListener()  {
			public void actionPerformed(ActionEvent arg0) {
				projName = theManFrame.getProjDir();
				FileRead fc = new FileRead(projName, FileC.bDoVer, FileC.bDoPrt);
				
				if (fc.run(btnRmkFile, "Remark", FileC.dPROJ, FileC.fTXT)) { 
					txtRmkFile.setText(fc.getRemoveFixedPath());
				}
			}
		});
		rmkRow.add(btnRmkFile);							rmkRow.add(Box.createHorizontalStrut(5));
		
		btnAddRmk = Static.createButtonRun("Add", true);
		btnAddRmk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String file = txtRmkFile.getText().trim();
				if (!file.equals("")) {
					addRemark(txtRmkFile.getText());
				}
			}
		});
		rmkRow.add(btnAddRmk);
		
		rmkRow.setMaximumSize(rmkRow.getPreferredSize());
		rmkRow.setMinimumSize(rmkRow.getPreferredSize());
		add(rmkRow);
		add(Box.createVerticalStrut(20));
		
		// button row
		JPanel buttonRow = Static.createRowCenterPanel();
		btnClose = Static.createButton("Close", true);
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				theManFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				setVisible(false);
				theManFrame.setMainPanelVisible(true);
			}
		});
		buttonRow.add(btnClose);
		buttonRow.add(Box.createHorizontalStrut(10));

		btnRemove = Static.createButton("Remove Remarks", true);
		btnRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeRemark();
			}
		});
		buttonRow.add(btnRemove);
		buttonRow.add(Box.createHorizontalStrut(10));
		
		btnHelp = Static.createButtonHelp("Help", true);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theManFrame, "Remark Help", helpDir);
			}
		});
		buttonRow.add(btnHelp);
		buttonRow.setMaximumSize(buttonRow.getPreferredSize());
		buttonRow.setMinimumSize(buttonRow.getPreferredSize());
		add(buttonRow);
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setVisible(false);
	}
	
	public void reset() {
		txtRmkFile.setText("");
		txtLocFile.setText("");
	}
	
	private JTextField txtRmkFile = null, txtLocFile = null;
	private JButton btnAddRmk = null, btnAddLoc = null;
	private JButton btnRmkFile = null, btnLocFile = null;

	private JButton btnRemove = null, btnClose = null, btnHelp = null;
	
	private ManagerFrame theManFrame = null;
	private String projName="";
}
