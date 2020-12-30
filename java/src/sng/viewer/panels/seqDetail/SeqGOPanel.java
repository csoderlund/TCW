package sng.viewer.panels.seqDetail;
/***********************************************
 * From ContigOverviewPanel (Sequence Details), this contains
 * code for GO button in top row
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import sng.database.Globals;
import sng.dataholders.ContigData;
import sng.viewer.STCWFrame;
import sng.viewer.panels.Basic.GOtree;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Out;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.database.DBConn;


public class SeqGOPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	static final private int SHOW_ASSIGNED_GO = SeqTopRowTab.SHOW_ASSIGNED_GO;
	static final private int SHOW_ALL_GO = SeqTopRowTab.SHOW_ALL_GO;
	static final private int SHOW_SEL_GO = SeqTopRowTab.SHOW_SEL_GO;
	static final private int SHOW_SEL_ALL_GO = SeqTopRowTab.SHOW_SEL_ALL_GO;
	
	private static final String GO_FORMAT = Globals.GO_FORMAT;
	private static final int GOHIT=1; // showGOThread
	private static final int GOSET=2; // showGOThread
	
	private static final int MAX_COL = 250;

	public SeqGOPanel(STCWFrame frame, int dType, String hit, SeqDetailPanel p, ContigData cd) {
		theParentFrame = frame;
		theGoData = new Vector<GoListData> ();
		seqDetailPanel = p;
		displayType = dType;
		
		if (displayType==SHOW_ALL_GO) 			loadAllGOs();
		else if (displayType==SHOW_ASSIGNED_GO) 	loadAssignedGOs();
		else if (displayType>=SHOW_SEL_GO) 		loadSelHitGOs(hit);
		
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		
		createToolPanel();
		add(Box.createVerticalStrut(5));
		add(toolPanel);
		
		createMainPanel();
		mainScrollPanel = new JScrollPane ( goPanel );
		mainScrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		mainScrollPanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		UIHelpers.setScrollIncrements( mainScrollPanel );
		add ( mainScrollPanel );	
		
		add(Box.createVerticalGlue());
	}
	/**********************************************************************/
	private JButton btnHit, btnAnc, btnPath, btnClust;
	private void createToolPanel() {
		toolPanel = Static.createRowPanel();
		
		toolPanel.add(new JLabel("  Show for Selected GO: "));
		toolPanel.add(Box.createHorizontalStrut(1));
		
		btnHit = Static.createButton("Hits", false);
		btnHit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGOpopupForSelected(1, btnHit);
			}
		});
		toolPanel.add(btnHit);
		toolPanel.add(Box.createHorizontalStrut(1));
		
		btnAnc = Static.createButton("Ancestors", false);
		btnAnc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGOpopupForSelected(2, btnAnc);
			}
		});
		toolPanel.add(btnAnc);
		toolPanel.add(Box.createHorizontalStrut(1));
		
		btnPath = Static.createButton("Paths",false);
		btnPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGOpopupForSelected(3, btnPath);
			}
		});
		toolPanel.add(btnPath);
		toolPanel.add(Box.createHorizontalStrut(20));
		
		JButton btnTable = Static.createButton("Table", true);
		btnTable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPopupTable();
			}
		});
		toolPanel.add(btnTable);
		toolPanel.add(Box.createHorizontalStrut(5));
		
		btnClust = Static.createButton("GO sets", true);
		btnClust.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGOThread(GOSET, 0, "", btnClust);
			}
		});
			
		if (displayType==SHOW_ASSIGNED_GO) {
			toolPanel.add(btnClust);
		}
		
		JButton btnGoHelp = Static.createButton("GO Help", true, Globals.HELPCOLOR);
		btnGoHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(), 
						"GO Help", "html/viewSingleTCW/GO.html");
			}
		});
		toolPanel.add( Box.createHorizontalGlue() );
		JButton btnHelp = Static.createButton("Help2", true, Globals.HELPCOLOR);
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(theParentFrame, 
						"Sequence GO Help", "html/viewSingleTCW/ContigGoPanel.html");
			}
		});
		
		toolPanel.add(btnGoHelp);
		toolPanel.add(Box.createHorizontalStrut(3));
		toolPanel.add(btnHelp);
		toolPanel.add(Box.createHorizontalStrut(5));
		
		toolPanel.setBackground(Color.WHITE);
		toolPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)toolPanel.getPreferredSize ().getHeight() ) );	
	}
	private SeqGOPanel getInstance() {return this;}
	/**********************************************************************/
	private void createMainPanel() {
		String textArea = makeTextArea();
		if (goOrder.size() > 0) {
			createTablePanel(); // XXX
		}
	
		goPanel = Static.createPagePanel();
		goPanel.add( Box.createVerticalStrut(10) );
		super.setBackground(Color.WHITE);
		
		textAreaTop = new JTextArea ( textArea );
		textAreaTop.setEditable( false );		
		textAreaTop.setFont(new Font("monospaced", Font.PLAIN, 12));
		int nTextWidth = Math.max((int)textAreaTop.getPreferredSize().getWidth() + 5, 600);		
		textAreaTop.setMaximumSize( new Dimension ( nTextWidth, 
				(int)textAreaTop.getPreferredSize().getHeight() ) );	

		JPanel tableAndHeader = Static.createPagePanel();
		textAreaTop.setAlignmentX(LEFT_ALIGNMENT);
		tableAndHeader.add ( textAreaTop );
		if (goInfoMap.size()>0) {
			tableAndHeader.add(scrollTablePane);	
		}
		tableAndHeader.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
			
		goPanel.add( tableAndHeader );
		goPanel.add( Box.createVerticalStrut(10) );			
		invalidate();
		
		goPanel.add ( Box.createVerticalGlue() );	
	}
	private void updateTopButtons() {
		boolean enable = (theGoTable.getSelectedRowCount()>0);
		btnHit.setEnabled(enable);
		btnAnc.setEnabled(enable);
		btnPath.setEnabled(enable);
	}
	/***************************************************
	 * Make text area
	 */
	private String makeTextArea() {
		String textArea;
		if (displayType==SHOW_ALL_GO) {
			textArea="Sequence: " + seqDetailPanel.getSeqName() +  "  Assigned and inherited GOs for all hits\n";
			textArea += "\nUnique GOs:    " + goInfoMap.size() + "\n";
			return textArea;
		}
		if (displayType==SHOW_ASSIGNED_GO) {
			textArea="Sequence: " + seqDetailPanel.getSeqName() +  "  Assigned GOs for all hits\n";
			textArea += "\nHits with GOs: " + cntHitsWithGOs;
			textArea += "\nUnique GOs:    " + goInfoMap.size() + "\n";
			return textArea;
		}
		if (displayType>=SHOW_SEL_GO) {
			String msg = (displayType==SHOW_SEL_GO) ? " Assigned" : " Assigned and inherited";
			textArea = "Sequence: " + seqDetailPanel.getSeqName() + 
					msg + " GOs for: " + strHit + " E-value " + seqDetailPanel.getHitEval(strHit) + "\n\n";
			
			if (seqDetailPanel.getHitCount()==0) {
				textArea += "No data for hit";
				return textArea;
			}
			
			// can have KEGG, etc without GO. So show regardless
			rows = new String [6][2];
			rows[0][0] = "Descript:"; rows[0][1] = seqDetailPanel.getHitDesc(strHit);
			rows[1][0] = "KEGG:";     rows[1][1] = seqDetailPanel.getHitKEGG(strHit);
			rows[2][0] = "Pfam:";     rows[2][1] = seqDetailPanel.getHitPfam(strHit);
			rows[3][0] = "EC(enzyme):"; rows[3][1] = seqDetailPanel.getHitEC(strHit);
			rows[4][0] = "InterPro:"; rows[4][1] = seqDetailPanel.getHitInterpro(strHit);
			rows[5][0] = "#GO:";      rows[5][1] = seqDetailPanel.getHitGO(strHit);;
			
			textArea += textTable(2, 5);
			return textArea;
		}
		return "";
	}
 /*  Make table in textArea */
	private String textTable(int nCol, int nRow)
	{
		String lines = "";
		int c, r;
		
		int []collen = new int [nCol];
		for (c=0; c<nCol; c++) collen[c]=0;
				
        for (c=0; c< nCol; c++) {
            for (r=0; r<nRow && rows[r][c] != null; r++) 
            		if (rows[r][c].length() > collen[c]) 
            			collen[c] = rows[r][c].length();
 
        }
        for (c=0; c< nCol; c++)  collen[c] += 4;
        
        for (r=0; r<nRow; r++) {
        		String row = " ";
            for (c=0; c<nCol && rows[r][c] != null; c++) {
                 row += pad(rows[r][c],collen[c],1);
                 rows[r][c] = null; // so wouldn't reuse in next table
            }
            lines += row + "\n";
        }
		return lines;
	}
    private static String pad(String s, int width, int o)
    {
            if (s.length() > width) {
                String t = s.substring(0, width-1);
                System.err.println("'" + s + "' truncated to '" + t + "'");
                s = t;
                s += " ";
            }
            else if (o == 0) { // left
                String t="";
                width -= s.length();
                while (width-- > 0) t += " ";
                s = t + s;
            }
            else {
                width -= s.length();
                while (width-- > 0) s += " ";
            }
            return s;
    }
    /********************************************
     * Add Go information to the GO list.
     */
    private void loadInfoForGOs() {
	    	try {
			ResultSet rs=null;
			DBConn mDB = theParentFrame.getNewDBC();
			String strQuery;
			
			for (GOinfo gi : goOrder) {
				strQuery= "select gonum, descr, term_type, level " +
					  " from go_info where gonum =" + gi.gonum;
				rs = mDB.executeQuery(strQuery);
				if (!rs.next()) {
					System.err.println("No go term " + 
							String.format(GO_FORMAT, gi.gonum) + " -- probably replaced.");
					continue;
				}
				
				gi.desc=rs.getString(2);
				gi.type = rs.getString(3).substring(0,4);
				gi.level = rs.getInt(4);
			}
			Collections.sort(goOrder);
			
			for (GOinfo gi : goOrder) {
				theGoData.add(new GoListData(gi.gonum, gi.desc, gi.type, gi.level, 
						gi.evidList, gi.nHit, gi.evalue, gi.hitID, gi.direct));
			}
			if (rs!=null) rs.close(); mDB.close();
		}
		catch(Exception e) {ErrorReport.reportError(e, "Generating GO table");}
    }
    /*********************************
     * Create goInfoMap is hashMap GOInfo objects, and goOrder is ordered vector 
     * Read pja_unitrans_go: Best hit is pre-computed. Seq-GO pairs are unique in
     * the DB table.
     */
    private void loadAllGOs() {
    		HashMap <Integer, String> hitMap = seqDetailPanel.getHitIdName();
    		
		try {
			DBConn mDB = theParentFrame.getNewDBC();	
			ResultSet rs=null;
	    		rs = mDB.executeQuery("select gonum, bestDUH, bestEval, EC, direct from " + 
	    				"pja_unitrans_go where CTGID=" + seqDetailPanel.getSeqID());
	    		 while (rs.next()) {
	    			 GOinfo gi = new GOinfo ();
	    			 int gonum = rs.getInt(1);
	   
	    			 int id = rs.getInt(2);
	    			 if (hitMap.containsKey(id)) gi.hitID = hitMap.get(id); 
	    			 else System.err.println("TCW error: gonum=" + gonum + " hitID=" + id);
	    			 
	    			 gi.gonum = gonum;
	    			 gi.evalue=rs.getDouble(3);
	    			 gi.evidList = rs.getString(4);
	    			 gi.direct = rs.getBoolean(5);
	    			 
	    			 goInfoMap.put(gonum, gi);
	    			 goOrder.add(gi);
	    		 }
	    		 mDB.close();
	    		 rs.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reading database for all GOs");}
    }
    /*********************************
     * Create goInfoMap is hashMap GOInfo objects, and goOrder is ordered vector 
     */
    private void loadAssignedGOs() {
    		HashSet <String> hitList = seqDetailPanel.getHitNameGO();
	
		DBConn mDB = theParentFrame.getNewDBC();	
		ResultSet rs=null;
	
		try {
			for (String hitName : hitList) {
				cntHitsWithGOs++;
				Vector <Integer> goWOec= new Vector <Integer> ();
				double evalue = seqDetailPanel.getHitEval(hitName);
				int best = seqDetailPanel.getHitBest(hitName);
				
				String [] hitGoList;
				
				rs = mDB.executeQuery("select goList from pja_db_unique_hits" +
						" where hitID='" + hitName + "' limit 1");
				if (rs.next()) {
					String hitGoEvidStr = rs.getString(1);
					hitGoList = hitGoEvidStr.split(";");
				}
				else {
					Out.PrtWarn("No GOs for " + hitName);
					continue;
				}
				
				for (String go : hitGoList) {
					if (go.startsWith("#") || go.equals("")) continue;
					String [] tok = go.split(":"); // GO:00nnnnn:EVC
					if (tok.length!=3) continue;
					
					int gonum = Integer.parseInt(tok[1]);
					String evid = tok[2];
					if (goInfoMap.containsKey(gonum)) {
						GOinfo gi = goInfoMap.get(gonum);
						gi.nHit++;
						if (gi.evidMap.containsKey(evid)) 
							 gi.evidMap.put(evid, gi.evidMap.get(evid)+1);
						else gi.evidMap.put(evid, 1);
						if (evalue<gi.evalue || (evalue==gi.evalue && best>gi.nBest)) {
							gi.evalue=evalue;
							gi.hitID=hitName;
							gi.nBest=best;
						}
					}
					else {
						GOinfo gi = new GOinfo ();
						gi.gonum = gonum;
						gi.nHit=1;
						gi.evalue=evalue;
						gi.hitID=hitName;
						gi.evidMap.put(evid, 1);
						goInfoMap.put(gonum, gi);
						goOrder.add(gi);
					}
					goWOec.add(gonum);
				}
				
				Collections.sort(goOrder);
				
				// used for makePopupGOclusters GOsets
				String golist="";
				for (Integer gonum : goWOec) {
					if (golist=="") golist = ""+ gonum;
					else golist += "," + gonum;
				}	
				if (goClustMap.containsKey(golist)) goClustMap.get(golist).add();
				else goClustMap.put(golist, new GOset());
			}
			if (rs!=null) rs.close();
			mDB.close();
			
			for (GOinfo gi : goInfoMap.values()) {
				for (String evid : gi.evidMap.keySet()) {
					if (gi.evidList.equals("")) gi.evidList = gi.evidMap.get(evid) + ":" + evid;
					else gi.evidList += "," + gi.evidMap.get(evid) + ":" + evid;
				}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reading database for goList");}
	}
    private void loadSelHitGOs(String hit) {
		strHit = hit;
		DBConn mDB = theParentFrame.getNewDBC();	
		ResultSet rs=null;
	
		try {
			cntHitsWithGOs++;
			String [] hitGoList;
				
			rs = mDB.executeQuery("select goList from pja_db_unique_hits" +
						" where hitID='" + strHit + "' limit 1");
			if (rs.next()) {
				String hitGoEvidStr = rs.getString(1);
				hitGoList = hitGoEvidStr.split(";");
			}
			else {
				Out.PrtWarn("No GOs for " + strHit);
				return;
			}
				
			HashMap <Integer, String> goMap= new HashMap <Integer, String> ();
			for (String go : hitGoList) {
				if (go.startsWith("#") || go.equals("")) continue;
				String [] tok = go.split(":"); // GO:00nnnnn:EVC
				if (tok.length!=3) continue;
				
				int gonum = Integer.parseInt(tok[1]);
				String evid = tok[2];
				
				GOinfo gi = new GOinfo ();
				gi.gonum = gonum;
				gi.evidMap.put(evid, 1);
				goInfoMap.put(gonum, gi);
				goOrder.add(gi);
				
				goMap.put(gonum, evid);
			}// end for loop through goList
			
			if (displayType==SHOW_SEL_ALL_GO) {
				for (int gonum : goMap.keySet()) {
					String ec = "(" + goMap.get(gonum) + ")";
					rs = mDB.executeQuery("select ancestor from go_graph_path where child=" + gonum);
					while (rs.next()) {
						int gonum2 = rs.getInt(1);
						if (!goInfoMap.containsKey(gonum2)) {
							GOinfo gi = new GOinfo ();
							gi.gonum = gonum2;
							gi.evidMap.put(ec, 1);
							goInfoMap.put(gonum2, gi);
							goOrder.add(gi);
						}
					}
				}
			}
			Collections.sort(goOrder);
				
			if (rs!=null) rs.close();
			mDB.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reading database for goList");}
		
		for (GOinfo gi : goInfoMap.values()) {
			for (String evid : gi.evidMap.keySet()) {
				gi.evidList = evid;
			}
		}
	}

    /***************** XXX GO methods ****************************/
    /**************************************************************
     * Uses hitList and queries database for GOs for each
     */
    private void showGOpopupForSelected(int type, JButton show) {
    		try {
	    		int [] selections = theGoTable.getSelectedRows();
			if (selections.length==0) {
				JOptionPane.showMessageDialog(null, "No selected GO ");
				return;
			}
			int gonum = theGoData.get(selections[0]).getID();
			String desc = theGoData.get(selections[0]).getDesc();
			
			if (type==1) {
				showGOThread(GOHIT, gonum, desc, show);
			}
			else if (type==2) {
				new GOtree(theParentFrame).popup(gonum, desc, GOtree.ANCESTORS, show);
			}
			else if (type==3)  {
				new GOtree(theParentFrame).popup(gonum, desc, GOtree.PATHS, show);
			}
    		}
    		catch (Exception e) {
    			JOptionPane.showMessageDialog(null, "Query failed");
    			ErrorReport.prtReport(e, "Creating Sequence GO popup");
    		}
    }
    /********************************************************
     * GO - hits : only for sequence hits. Uses info from
     */
    private void showGOThread(int tp, int go, String ds, JButton show) {
		final int gonum = go;
		final String desc = ds;
		final JButton showButton = show;
		final int type=tp;
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					showButton.setEnabled(false);
					String msg="";
					Vector <String> lines;
					
					if(type==GOHIT) {
						msg = seqDetailPanel.getSeqName() + " hits with assigned " 
								+ String.format(GO_FORMAT, gonum) + " ";
						lines = showHitsForGO(gonum, msg + " - " + desc);
					}
					else {
						msg = seqDetailPanel.getSeqName() + " GO sets";
						lines = showGOsets();
					}
					
					if (lines==null) {
						JOptionPane.showMessageDialog(null, "Query failed");
					}
					else {
						String [] alines = new String [lines.size()];
						lines.toArray(alines);
						UserPrompt.displayInfoMonoSpace(theParentFrame, msg, alines);
					}
					showButton.setEnabled(true);
				}
				catch (Exception e) {ErrorReport.prtReport(e, "Sequence GO query failed");}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}	
    // Sequence hits for GO - uses local structures and pja_db_unique_hits.goList
    private Vector <String> showHitsForGO(int theGonum, String msg) {
    		Vector <String> lines = new Vector <String> ();
    		lines.add(msg);
    		lines.add("");
    		
    		try {	
    			ResultSet rs=null;
    			DBConn mDB = theParentFrame.getNewDBC();
    			
    			Vector <String> sortHit = seqDetailPanel.getHitList(); // list of hits
    			Collections.sort(sortHit);
    			
    			int count=0;
    			String line = String.format("%-16s  %-3s  %6s  %4s  %5s  %4s  %s", 
    					"Hit", "EC", "E-val", "%Sim", "Align", "Best", "Description");
    			lines.add(line);
    			
    			// check all hits for sequence to see if they have this go
    			for (String hitID : sortHit) {
    				
    				String goStr="";
    				rs = mDB.executeQuery("select goList from pja_db_unique_hits " +
    						"where hitID='" + hitID + "'");
    				if (rs.next()) goStr = rs.getString(1);
    				else {
    					lines.add("Error on " + hitID);
    					return lines;
    				}
    				String [] goForHit = goStr.split(";");
    				
    				for (String go : goForHit) {
    					if (go.startsWith("#")) continue;
    					String [] tok = go.split(":");
    					if (tok.length!=3) {
    						// Getting "" go's.
    						//System.err.println("Strange: " + hitID + " :'" + go +"'"); 
    						continue;
    					}
					int gonum = Integer.parseInt(tok[1]);
					if (gonum==theGonum) { // this hit has the GO
						String evid = tok[2];
						if (evid.endsWith("...")) evid = evid.substring(0,3); // goBrief ends with '...'
						
						String eval =  (new DecimalFormat("0E0")).format(seqDetailPanel.getHitEval(hitID));
	    					int sim  =  seqDetailPanel.getHitPercent(hitID);
	    					int len = seqDetailPanel.getHitAlignLen(hitID);
	    					int filter = seqDetailPanel.getHitFiltered(hitID);
	    					String desc1 = seqDetailPanel.getHitDesc(hitID);
	    					String best = "";
	    					if ((filter & 16) !=0) best="EV";
	    					if ((filter & 32) !=0) best+="AN";
	    					line = String.format("%-16s  %3s  %6s  %4d  %5d  %4s  %s", 
	    							hitID, evid, eval, sim, len, best, desc1);
	    					lines.add(line);
	    					count++;
	    					break;
					}
    				}
    			}
    			if (rs!=null) rs.close(); mDB.close();
    			lines.add("");
    			if (count>0) lines.add("Count: " + count);
    			else lines.add("This GO is not assigned to any hits for this sequence");
    		}
    		catch (Exception e) {
    			ErrorReport.prtReport(e, "Making popup of hits for GO");
    			lines.add("Error processing GO");
    		}
    		return lines;
    }
    private void showPopupTable() {
		Vector <String> lines = new Vector <String> ();
		String line="";
		
		if (displayType==SHOW_ALL_GO) 
			line="Sequence: " + seqDetailPanel.getSeqName() +    "  Assigned and inherited GOs for all hits";
		else if (displayType==SHOW_ASSIGNED_GO) 
			line = "Sequence: " + seqDetailPanel.getSeqName() +  "  Assigned GOs for all hits";	
		else if (displayType==SHOW_SEL_GO) 
			line = "Sequence: " + seqDetailPanel.getSeqName() +  "  Assigned GOs for selected hit: " + strHit;
		else if (displayType==SHOW_SEL_GO) 
			line = "Sequence: " + seqDetailPanel.getSeqName() +  "  Assigned and inherited GOs for selected hit: " + strHit;
		lines.add(line);
		lines.add("");
		
		line = String.format("%-10s  %-30s  %4s  %5s", "GO term", "Description", "Type", "Level");
		if (displayType==SHOW_ASSIGNED_GO) 
			line += String.format("  %5s  %6s  %-15s", "#Hits", "E-val", "Best Hit ID");
		else if (displayType==SHOW_ALL_GO)
			line += String.format("  %5s  %-15s", "E-val", "Best Hit ID");
		line += "  EC";
		lines.add(line);
		
		for (GOinfo gi : goOrder) {
			String go = String.format(GO_FORMAT, gi.gonum);
			String desc = gi.desc;
			if (desc==null) desc = "probably replaced GO term";
			if (desc.length()>=30) desc = desc.substring(0,28) + "..";
			
			line = String.format("%10s  %-30s  %4s  %5d", go, desc, gi.type, gi.level);
			
			String eval =  (new DecimalFormat("0E0")).format((Double)gi.evalue);
			String hitID = gi.hitID;
			if (hitID.length()>15) hitID = hitID.substring(0,14);
			
			if (displayType==SHOW_ASSIGNED_GO) 
				line += String.format("  %5s  %6s  %-15s", gi.nHit, eval, hitID);
			else if (displayType==SHOW_ALL_GO) 
				line += String.format("  %5s  %-15s", eval, hitID);
			line += "  " + gi.evidList + " ";
			lines.add(line);	
		}
		String [] alines = new String [lines.size()];
		lines.toArray(alines);
		UserPrompt.displayInfoMonoSpace(theParentFrame, "GO table", alines);
	}
    
    // Determines what hits shares what GOs - no database queries
    private Vector <String> showGOsets() {
		// create hashSet for fast searching
    		GOset [] order = new GOset [goClustMap.size()];
    		int o=0;
		for (String goStr : goClustMap.keySet()) {
			GOset gi = goClustMap.get(goStr);
			String [] golist = goStr.split(",");
			
			for (int i=0; i<golist.length; i++) {
				try {
					int go = Integer.parseInt(golist[i]);
					gi.goSet.add(go);
				}
				catch (Exception e) {} 
			}
			order[o++]=gi;
		}
		// good ole bubble sort on cnt
		for (int o1=0; o1<order.length-1; o1++) {
			for (int o2=o1+1; o2<order.length; o2++) {
				if (order[o1].cnt<order[o2].cnt) {
					GOset gi = order[o1];
					order[o1] = order[o2];
					order[o2] = gi;
				}
			}
		}
		// heading
		Vector <String> lines = new Vector <String> ();
		String line= String.format("%14s ", "Sets");
		for (int i=0; i<goClustMap.size(); i++) line += String.format(" %2d ", (i+1));
		line += " #Sets";
		lines.add(line);
		
		line = String.format("%14s ", "#GOs in set");
		for (int o1=0; o1<order.length; o1++) {
			line += String.format(" %2d ", order[o1].goSet.size());
		}
		lines.add(line);
		
		for (GOinfo gi1 : goOrder) {
			int inSet=0;
			line = "    " + String.format(GO_FORMAT, gi1.gonum);
			for (int o1=0; o1<order.length; o1++) {
				GOset gi = order[o1];
				if (gi.goSet.contains(gi1.gonum)) {
					line +=  "   X";
					inSet++;
				}
				else line += "    ";
			}
			line += "   " + inSet;
			lines.add(line);
		}
		
		line = String.format("%14s ", "#Hits with set");
		for (int o1=0; o1<order.length; o1++) {
			line += String.format(" %2d ", order[o1].cnt);
		}
		lines.add(line);
		return lines;
    }
    
	/*************************************************************
	 * GOs for all hits - queries database
	 */
	private void createTablePanel() {
		theGoTableModel = new GoTableModel();
		theGoTable = new JTable(theGoTableModel){
			private static final long serialVersionUID = 1391536130387283980L;
			public Component prepareRenderer(
				        TableCellRenderer renderer, int row, int column)
			    {
			        Component c = super.prepareRenderer(renderer, row, column);
			        return c;
			    }
		};
		scrollTablePane = new JScrollPane(theGoTable);
		
		theGoTable.setColumnSelectionAllowed( false );
		theGoTable.setCellSelectionEnabled( false );
		theGoTable.setRowSelectionAllowed( true);
		theGoTable.setShowHorizontalLines( false );
		theGoTable.setShowVerticalLines( false );	 
		theGoTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				updateTopButtons();
			}
		});
		
		JTableHeader header = theGoTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(theGoTableModel.new ColumnListener(theGoTable));
		header.setReorderingAllowed(true);
		
		loadInfoForGOs();
		
		theGoTableModel.fireTableStructureChanged();
		theGoTableModel.fireTableDataChanged();
		resizeColumns();
	}

	/********************************************************
	 * XXX table class
	 */
	private class GoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -4168939390237438684L;

		public int getColumnCount() {
			if (displayType==SHOW_ASSIGNED_GO) return 8;
			if (displayType==SHOW_ALL_GO) return 7;
			return 5;
		}

		public String getColumnName(int colIndex) {
			if(colIndex == 0) return "GO term";
			if(colIndex == 1) return "Description";
			if(colIndex == 2) return "Type";
			if(colIndex == 3) return "Level";
				
			if (displayType==SHOW_ASSIGNED_GO) {
				if (colIndex==4) return   "#Hits";
				if (colIndex == 5) return "#:EC";
				if (colIndex == 6) return "Best Hit ID";
				if (colIndex == 7) return "E-value";
			}
			else if (displayType==SHOW_ALL_GO) {
				if (colIndex==4) return "Assign";
				if (colIndex == 5) return "Best Hit ID";
				if (colIndex == 6) return "E-value";
			}
			else if (displayType>=SHOW_SEL_GO) {
				if (colIndex == 4) return "EC";
			}
			
			return "error";
		}
		public int getRowCount() {
			return theGoData.size();
		}

		public Object getValueAt(int row, int col) {
			GoListData rd = theGoData.get(row);
			if(col == 0) return "GO:" + String.format("%07d", rd.nGoNum);
			if(col == 1)  return rd.strDescription;
			if(col == 2) return rd.strType;
			if(col == 3) return rd.nLevel;
			
			if (displayType==SHOW_ASSIGNED_GO) {
				if (col == 4) return rd.nHit;
				if (col == 5) return rd.strEvid;
				if (col == 6) return rd.hitID;
				if (col == 7) return (new DecimalFormat("0E0")).format((Double)rd.evalue); 
			}
			else if (displayType==SHOW_ALL_GO) {
				if (col == 4) return rd.direct;
				if (col == 5) return rd.hitID;
				if (col == 6) return (new DecimalFormat("0E0")).format((Double)rd.evalue); 
			}
			else if (displayType>=SHOW_SEL_GO) {
				if(col == 4) return rd.strEvid;
			}
			
			return "error";
		}	
		
		private boolean sortAsc = true;
		private int sortMode=0;
        
  	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {table = t;}

		    public void mouseClicked(MouseEvent e) {
			    	TableColumnModel colModel = table.getColumnModel();
			    	int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			    	int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();
	
			    	if (modelIndex < 0) return;
			    	
			    	if (sortMode == modelIndex) sortAsc = !sortAsc;
			    	else sortMode = modelIndex;
	
			    	for (int i = 0; i < table.getColumnCount(); i++) { 
			    		TableColumn column = colModel.getColumn(i);
			    		column.setHeaderValue(getColumnName(column.getModelIndex()));
			    	}
			    	table.getTableHeader().repaint();
	
			    	Collections.sort(theGoData, new GoListComparator(sortAsc, sortMode));
			    	table.tableChanged(new TableModelEvent(GoTableModel.this));
			    	table.repaint();
		    }
  	  	}
	}
	private class GoListComparator implements Comparator<GoListData> {
		public GoListComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(GoListData obj1, GoListData obj2) { 
			return obj1.compareTo(obj2, bSortAsc, nMode); }
		
		private boolean bSortAsc;
		private int nMode;
	}
	private class GoListData {
		public GoListData(int gonum, String description, String type, int level, String e, 
				int cnt, double ev, String id, boolean d) {
			nGoNum = gonum;
			strDescription = description;
			strType = type;
			nLevel = level;
			strEvid = e;	
			nHit=cnt;
			evalue = ev;
			hitID = id;
			direct = (d) ? "Yes" : "No";
		}
		public int getID() { return nGoNum; }
		public String getDesc() { return strDescription; }
		
		private int nHit=0;
		private int nGoNum = -1;
		private String strDescription = "";
		private String strType="";
		private String strEvid = "";
		private int nLevel = -1;
		private double evalue = -1;
		private String hitID="";
		private String direct;
		
		public int compareTo(GoListData obj, boolean sortAsc, int colIndex) {
			int order = 1;
			if(!sortAsc) order = -1;
			if(colIndex == 0) return order * ((Integer) nGoNum).compareTo((Integer) obj.nGoNum);
			if(colIndex == 1) return order * strDescription.compareTo(obj.strDescription);
			if(colIndex == 2) return order * strType.compareTo(obj.strType);
			if(colIndex == 3) return order * ((Integer)nLevel).compareTo((Integer) obj.nLevel);
			
			if (displayType==SHOW_ASSIGNED_GO) {
				if (colIndex==4)    return order * ((Integer) nHit).compareTo((Integer) obj.nHit);
				if (colIndex == 5)  return order * strEvid.compareTo(obj.strEvid);
				if (colIndex == 6)  return order * hitID.compareTo(obj.hitID);
				if (colIndex == 7)  return order * ((Double) evalue).compareTo((Double) obj.evalue);
			}
			else if (displayType==SHOW_ALL_GO) {
				if (colIndex==4)   return order * direct.compareTo(obj.direct);
				if (colIndex == 5) return order * hitID.compareTo(obj.hitID);
				if (colIndex == 6) return order * ((Double) evalue).compareTo((Double) obj.evalue);	
			}
			else if (displayType>=SHOW_SEL_GO) {
				if(colIndex == 4) return order * strEvid.compareTo(obj.strEvid);
			}
			return 0;
		}
	}
	
    private void resizeColumns() {
		theGoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int margin = 2;
		
		for(int x=0; x < theGoTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) 
				theGoTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;
			
			TableCellRenderer renderer = col.getHeaderRenderer();
			
			if(renderer == null)
				renderer = theGoTable.getTableHeader().getDefaultRenderer();
			
			Component comp = renderer.getTableCellRendererComponent(theGoTable, 
					col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
			
			for(int y=0; y<theGoTable.getRowCount(); y++) {
				renderer = theGoTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(theGoTable, 
						theGoTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += 2 * margin;
			
			col.setPreferredWidth(Math.min(maxSize, MAX_COL));
		}
		((DefaultTableCellRenderer) 
				theGoTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}
	
    /********************************************************
	 * private variables
	 */
    private JPanel goPanel = null;
	private JTextArea textAreaTop = null;
	private JScrollPane mainScrollPanel = null;
	private JPanel toolPanel = null;
	
	private Vector<GoListData> theGoData = null;
	private JTable theGoTable = null;
	private GoTableModel theGoTableModel = null;
	private JScrollPane scrollTablePane = null;
	private String [][] rows = null;
	
	private STCWFrame theParentFrame = null;
	private SeqDetailPanel seqDetailPanel = null;
	
	// for main table
	private Vector <GOinfo> goOrder = new Vector <GOinfo> ();
	private HashMap <Integer, GOinfo> goInfoMap = new HashMap <Integer, GOinfo> ();
    private class GOinfo implements Comparable <GOinfo>{ // direct hits
    		int gonum;
		String desc;
		String type;
		int level;
		String hitID="";
		double evalue;
    		int nHit;
    		String evidList="";
    		boolean direct=true;
    		int nBest=0;
    		TreeMap <String, Integer> evidMap = new TreeMap <String, Integer> ();
    		
    		public int compareTo(GOinfo gi) {
    			if (type!=null && gi.type!=null) {
    				if (type.compareTo(gi.type) < 0) return -1;
    				if (type.compareTo(gi.type) > 0) return 1;
    			}
    			if (level<gi.level) return -1;
    			if (level>gi.level) return 1;
    			if (gonum<gi.gonum) return -1;
    			if (gonum>gi.gonum) return 1;
    			return 0;
    		}
    }
    // for Show GO Sets
	private TreeMap <String, GOset> goClustMap = new TreeMap <String, GOset> (); // golist, count
    private class GOset {
    		int cnt=1;
    		HashSet <Integer> goSet = new HashSet <Integer> (); 
    		public void add() {cnt++;}
    }
	private String strHit = "";
	private int cntHitsWithGOs=0;
	private int displayType=0;
}
