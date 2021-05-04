package cmp.viewer.seqDetail;
/***********************************************
 * SeqGOPanel: called from SeqTopRowPanel to show GOs
 * for the Best Hit or Selected hit - both direct and indirect
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UIHelpers;
import util.database.DBConn;
import util.database.Globalx;

import cmp.viewer.MTCWFrame;

public class SeqGOPanel extends JPanel {
	private static final long serialVersionUID = 1L;	
	private static final int MAX_COL = 250;

	static final public int SHOW_ASSIGNED_GO = SeqTopRowPanel.SHOW_ASSIGNED_GO;
	static final public int SHOW_ALL_GO = SeqTopRowPanel.SHOW_ALL_GO;
	static final public int SHOW_SEL_GO = SeqTopRowPanel.SHOW_SEL_GO;
	static final public int SHOW_SEL_ALL_GO = SeqTopRowPanel.SHOW_SEL_ALL_GO;
	
	public SeqGOPanel(MTCWFrame frame, SeqDetailsPanel p, int seqIdx, String name, 
			String [] hit, int dType) {
		
		theParentFrame = frame;
		seqDetailPanel = p;
		seqID = seqIdx;
		seqName = name;
		hitInfo = hit;
		displayType = dType;
		goMap = Static.getGOtermMap(); // CAS322
		
		if (displayType==SHOW_ALL_GO) loadAllGOs();
		else if (displayType==SHOW_ASSIGNED_GO) loadAssignedGOs();
		else loadHitGOs();
		loadInfoForGOs();
		
		setLayout( new BoxLayout ( this, BoxLayout.PAGE_AXIS ) );
		setBackground(Color.WHITE);
		
		createMainPanel();
		mainScrollPanel = new JScrollPane ( goPanel );
		mainScrollPanel.setBackground(Color.WHITE);
		mainScrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		mainScrollPanel.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		mainScrollPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		UIHelpers.setScrollIncrements( mainScrollPanel );
		add ( mainScrollPanel );	
		
		add(Box.createVerticalGlue());
	}

	/**********************************************************************/
	private void createMainPanel() {
		goPanel = Static.createPagePanel();
		goPanel.add( Box.createVerticalStrut(10) );
		super.setBackground(Color.WHITE);
		
		String textArea = makeTextArea();
		textAreaTop = new JTextArea ( textArea );
		textAreaTop.setEditable( false );		
		textAreaTop.setFont(new Font("monospaced", Font.PLAIN, 12));
		int nTextWidth = Math.max((int)textAreaTop.getPreferredSize().getWidth() + 5, 600);		
		textAreaTop.setMaximumSize( new Dimension ( nTextWidth, 
				(int)textAreaTop.getPreferredSize().getHeight() ) );	
		textAreaTop.setAlignmentX(LEFT_ALIGNMENT);
		
		JPanel tableAndHeader = Static.createPagePanel();
		tableAndHeader.add ( textAreaTop );
		
		createTablePanel(); // XXX
		
		tableAndHeader.add(scrollTablePane);	
		tableAndHeader.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
			
		goPanel.add( tableAndHeader );
		goPanel.add( Box.createVerticalStrut(10) );			
		invalidate();
		
		goPanel.add ( Box.createVerticalGlue() );	
	}
	
	/***************************************************
	 * Make text area
	 */
	private String makeTextArea() {
		String textArea = "Sequence: " + seqName + "        ";
		if (goOrder.size()==0) {
			textArea += "\nNo GOs\n\n";
		}
		else if (displayType==SHOW_ALL_GO) { // All GOs
			 textArea += "Assigned and inherited for all hits\n";
			 textArea += "Unique GOs: " + theGoData.size() + "\n\n";
		}
		else if (displayType==SHOW_ASSIGNED_GO) { // All GOs
			 textArea += "Assigned GOs for all hits\n";
			 textArea += "Unique GOs: " + theGoData.size() + "\n\n";
		}
		else { 
			String msg = (displayType==SHOW_SEL_GO) ? "Assigned " : "All ";
			textArea += msg + "GOs for: " + hitInfo[0] + " E-value " + hitInfo[2] + "\n";
			textArea += "\nDescript: " + hitInfo[1] + "\n";
		}
		return textArea;
	}
 
    /********************************************
     * Add Go information to the GO list.
     */
	 private void loadInfoForGOs() {
	    	try {
			ResultSet rs=null;
			DBConn mDB = theParentFrame.getDBConnection();
			String strQuery;
			
			for (GOinfo gi : goOrder) {
				strQuery= "select gonum, descr, term_type, level " +
					  " from go_info where gonum =" + gi.gonum;
				rs = mDB.executeQuery(strQuery);
				if (!rs.next()) {
					Out.PrtWarn("No go number " + gi.gonum);
					continue;
				}
				
				gi.desc=   rs.getString(2);
				gi.type =  goMap.get(rs.getString(3));
				gi.level = rs.getInt(4);
			}
			if (rs!=null) rs.close(); mDB.close();
			
			Collections.sort(goOrder);
			
			for (GOinfo gi : goOrder) {
				theGoData.add(new GoListData(gi.gonum, gi.desc, gi.type, gi.level, 
						gi.evidList, gi.direct, gi.eval, gi.hitName, gi.nHit));
			}	
		}
		catch(Exception e) {ErrorReport.reportError(e, "Generating GO table");}
 }
	private void loadAllGOs() {
	    try {
	    	DBConn mDB = theParentFrame.getDBConnection();
			ResultSet rs = mDB.executeQuery(
					"select gonum, bestEval, direct, EC, bestHitstr " +
					"from go_seq where UTid=" + seqID);
			
			while (rs.next()) {
				GOinfo gi = new GOinfo();
				gi.gonum = rs.getInt(1);
				gi.eval = rs.getDouble(2);
				gi.direct = rs.getBoolean(3);
				gi.evidList = rs.getString(4);
				gi.hitName = rs.getString(5);
				goOrder.add(gi);
			}
			if (rs!=null) rs.close(); mDB.close();
    	}
    	catch(Exception e) {ErrorReport.reportError(e, "Generating GO table");}
	}
	private void loadAssignedGOs() {
		HashSet <String> hitList = seqDetailPanel.getHitNameGO();
		ResultSet rs=null;
		
		HashMap <Integer, GOinfo> goInfoMap = new HashMap <Integer, GOinfo> ();
		
		try {
			DBConn mDB = theParentFrame.getDBConnection();
			for (String hitName : hitList) {
				String [] hitGoList;
				
				rs = mDB.executeQuery("select goList from unique_hits" +
						" where hitStr='" + hitName + "' limit 1");
				if (rs.next()) {
					String hitGoEvidStr = rs.getString(1);
					hitGoList = hitGoEvidStr.split(";");
				}
				else {
					Out.PrtWarn("No GOs for " + hitName);
					continue;
				}
				
				double evalue = seqDetailPanel.getHitEval(hitName);
				int best = seqDetailPanel.getHitBest(hitName);
				
				for (String go : hitGoList) {
					if (go.startsWith("#") || go.equals("")) continue;
					String [] tok = go.split(":"); // GO:00nnnnn:EVC
					if (tok.length!=3) continue;
					
					int gonum = Integer.parseInt(tok[1]);
					String evid = tok[2].trim();
					if (goInfoMap.containsKey(gonum)) {
						GOinfo gi = goInfoMap.get(gonum);
						gi.nHit++;
						if (gi.evidMap.containsKey(evid)) 
							 gi.evidMap.put(evid, gi.evidMap.get(evid)+1);
						else gi.evidMap.put(evid, 1);
						if (evalue<gi.eval || (evalue==gi.eval && best>gi.nBest)) {
							gi.eval=evalue;
							gi.hitName=hitName;
							gi.nBest = best;
						}
					}
					else {
						GOinfo gi = new GOinfo ();
						gi.gonum = gonum;
						gi.nHit=1;
						gi.eval=evalue;
						gi.hitName=hitName;
						gi.evidMap.put(evid, 1);
						goInfoMap.put(gonum, gi);
						goOrder.add(gi);
					}
				}
			}
			if (rs!=null) rs.close();
			mDB.close();
			
			for (GOinfo gi : goOrder) {
				for (String evid : gi.evidMap.keySet()) {
					if (gi.evidList.equals("")) 
						 gi.evidList = gi.evidMap.get(evid) + ":" + evid;
					else gi.evidList += "," + gi.evidMap.get(evid) + ":" + evid;
				}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reading database for goList");}
	}
    private void loadHitGOs() {
	    try {
	    	if (hitInfo==null || hitInfo[0]==null) return;
	    		
	    	DBConn mDB = theParentFrame.getDBConnection();
			ResultSet rs = mDB.executeQuery(
					"select goList from unique_hits where HITstr='" + hitInfo[0] + "'");
			
			if (!rs.next()) {
				Out.PrtError("No record for " + hitInfo[0]);
				return;
			}
			double eval = Double.parseDouble(hitInfo[2]);
			
			HashMap <Integer, String> goMapEC= new HashMap <Integer, String> ();
			HashSet <Integer> goSet = new HashSet <Integer> ();
			
			String goList = rs.getString(1);
			String [] tok = goList.split(";");
			for (String go : tok) {
				GOinfo gi = new GOinfo();
				int idx = go.lastIndexOf(":");
				gi.goStr = go.substring(0,idx);
				gi.gonum = Integer.parseInt(gi.goStr.substring(3));
				gi.evidList = go.substring(idx+1);
				gi.eval=eval;
				goOrder.add(gi);
				goMapEC.put(gi.gonum, gi.evidList);
				goSet.add(gi.gonum);
			}
			if (displayType==SHOW_SEL_ALL_GO) {
				for (int gonum : goMapEC.keySet()) {
					String ec = "(" + goMapEC.get(gonum) + ")";
					rs = mDB.executeQuery("select ancestor from go_graph_path where child=" + gonum);
					while (rs.next()) {
						int gonum2 = rs.getInt(1);
						if (!goSet.contains(gonum2)) {
							GOinfo gi = new GOinfo ();
							gi.gonum = gonum2;
							gi.evidList=ec;
							goOrder.add(gi);
							goSet.add(gonum2);
						}
					}
				}
			}
			if (rs!=null) rs.close(); mDB.close();
		}
		catch(Exception e) {ErrorReport.reportError(e, "Generating GO table");}
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
		theGoTable.setBackground(Color.WHITE);
		scrollTablePane = new JScrollPane(theGoTable);
		scrollTablePane.setBackground(Color.WHITE); 
		scrollTablePane.getViewport().setBackground(Color.WHITE); // CAS310 will this fix linux problem of gray background?
		
		theGoTable.setColumnSelectionAllowed( false );
		theGoTable.setCellSelectionEnabled( false );
		theGoTable.setRowSelectionAllowed( true);
		theGoTable.setShowHorizontalLines( false );
		theGoTable.setShowVerticalLines( false );	 
		
		JTableHeader header = theGoTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(theGoTableModel.new ColumnListener(theGoTable));
		header.setReorderingAllowed(true);
		
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
			if(colIndex == 0) return Globalx.goID;
			if(colIndex == 1) return Globalx.goTerm;
			if(colIndex == 2) return Globalx.goOnt;
			if(colIndex == 3) return "Level";
				
			if (displayType==SHOW_ASSIGNED_GO) {
				if (colIndex==4) return "#Hits";
				if (colIndex == 5) return "#:" + Globalx.evCode;
				if (colIndex == 6) return "Best Hit ID";
				if (colIndex == 7) return "E-value";
			}
			else if (displayType==SHOW_ALL_GO) {
				if (colIndex==4) return "Assign";
				if (colIndex == 5) return "Best Hit ID";
				if (colIndex == 6) return "E-value";
			}
			else if (displayType>=SHOW_SEL_GO) {
				if (colIndex == 4) return Globalx.evCode;
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
				if (col == 6) return rd.hitName;
				if (col == 7) return (new DecimalFormat("0E0")).format((Double)rd.eval); 
			}
			else if (displayType==SHOW_ALL_GO) {
				if (col == 4) return rd.direct;
				if (col == 5) return rd.hitName;
				if (col == 6) return (new DecimalFormat("0E0")).format((Double)rd.eval); 
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
			return obj1.compareTo(obj2, bSortAsc, nMode); 
		}
		
		private boolean bSortAsc;
		private int nMode;
	}
	private class GoListData {
		public GoListData(int gonum, String description, String type, int level, String evid, 
				boolean isDirect, double ev, String name, int n) {
			nGoNum = gonum;
			strDescription = description;
			strType = type;
			nLevel = level;
			strEvid = evid;	
			direct = (isDirect) ? "Yes" : "No";
			eval=ev;
			hitName=name;
			nHit=n;
		}
		
		private int nGoNum = -1;
		private String strDescription = "";
		private String strType="";
		private String strEvid = "";
		private int nLevel = -1;
		private String direct;
		private double eval;
		private String hitName="";
		private int nHit=0;
		
		public int compareTo(GoListData obj, boolean sortAsc, int colIndex) {
			int order = 1;
			if(!sortAsc) order = -1;
			if(colIndex == 0) return order * ((Integer) nGoNum).compareTo((Integer) obj.nGoNum);
			if(colIndex == 1) return order * strDescription.compareTo(obj.strDescription);
			if(colIndex == 2) return order * strType.compareTo(obj.strType);
			if(colIndex == 3) return order * ((Integer)nLevel).compareTo((Integer)obj.nLevel);
			
			if (displayType==SHOW_ASSIGNED_GO) {
				if (colIndex==4)    return order * ((Integer)nHit).compareTo((Integer)obj.nHit);
				if (colIndex == 5)  return order * strEvid.compareTo(obj.strEvid);
				if (colIndex == 6)  return order * hitName.compareTo(obj.hitName);
				if (colIndex == 7)  return order * ((Double) eval).compareTo((Double) obj.eval);
			}
			else if (displayType==SHOW_ALL_GO) {
				if (colIndex==4)   return order * direct.compareTo(obj.direct);
				if (colIndex == 5) return order * hitName.compareTo(obj.hitName);
				if (colIndex == 6) return order * ((Double) eval).compareTo((Double) obj.eval);	
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
	
    private class GOinfo implements Comparable <GOinfo>{ // direct hits
		int gonum;
		String goStr;
		String desc="";
		String type="";
		int level=0;
		String evidList="";
		boolean direct=true;
		double eval=-100;
		int nHit=0, nBest=0;
		String hitName="";
		TreeMap <String, Integer> evidMap = new TreeMap <String, Integer> ();
		
		public int compareTo(GOinfo gi) {
			if (type!=null && gi.type!=null) {
				if (type.compareTo(gi.type) < 0) return -1;
				if (type.compareTo(gi.type) > 0) return 1;
			}
			if (level<gi.level) return -1;
			if (level>gi.level) return 1;
			if (gonum<gi.gonum) return -1;
			return 1;
			//return (desc.compareTo(gi.desc));
		}
    }
    
    /********************************************************
   	 * private variables
   	 */
    private JPanel goPanel = null;
   	private JTextArea textAreaTop = null;
   	private JScrollPane mainScrollPanel = null;
   	
   	private Vector <GOinfo> goOrder = new Vector <GOinfo> ();
   	private Vector<GoListData> theGoData = new Vector <GoListData> ();
   	private JTable theGoTable = null;
   	private GoTableModel theGoTableModel = null;
   	private JScrollPane scrollTablePane = null;
 
	// parameters
   	private int displayType=0;
	private String seqName;
	private String [] hitInfo;
	
	private MTCWFrame theParentFrame = null;
	private SeqDetailsPanel seqDetailPanel = null;
	private int seqID;
	private HashMap <String, String> goMap;
}
