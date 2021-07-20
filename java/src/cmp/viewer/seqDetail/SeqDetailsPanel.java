package cmp.viewer.seqDetail;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import cmp.viewer.MTCWFrame;
import cmp.viewer.table.FieldData;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.ui.UIHelpers;

public class SeqDetailsPanel extends JPanel {
	private static final long serialVersionUID = 4196828062981388452L;
	private  final int NUM_PAIR_ROWS = 8; // not really # rows displayed, just a scale
	private  final int NUM_HIT_ROWS = 12; // not really # rows displayed, just a scale
	private  final int MAX_TABLE_WIDTH = 550;
	private  final int MAX_COL = 180; // maximum size of column, e.g. description and species
	private  final String HIT_TABLE = FieldData.HIT_TABLE;
	private  final String PAIR_TABLE = FieldData.PAIR_TABLE;
	
	public SeqDetailsPanel(MTCWFrame parentFrame, String name, int seqid) {
		theParentFrame = parentFrame;
		seqIndex = seqid;
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Color.WHITE);
		
		try {
			mDB = theParentFrame.getDBConnection();
			loadTextArea();
			
			if (loadPairTable()==0) textArea += "\nNo pair data\n";
			else createPairTablePanel();
			
			if (loadHitTable()==0) textArea += "\nNo database hits\n";
			else createHitTablePanel();
			
			createViewPanel();
			mDB.close();
		}
		catch (Exception e) {ErrorReport.reportError(e, "trying to open View Detail Panel"); }
	}
	
	/***************************************************
	 * compute Text Area
	 */
	private void loadTextArea() {
		try {
			String sql = "SELECT UTstr, expListN, expList, ntLen, aaLen, " +
					" orf_frame, orf_start, orf_end, origStr " +
					" from unitrans where UTid=" + seqIndex;
			ResultSet rs = mDB.executeQuery(sql);
			rs.next();
			seqName = rs.getString(1);
			String rpkm	= rs.getString(2);
			String counts = rs.getString(3);
			String ntlen = rs.getString(4);
			if (ntlen.equals("0")) bAAonly=true; 
			aaLen = rs.getInt(5);
		
			int frame = rs.getInt(6);
			int start = rs.getInt(7);
			int end = rs.getInt(8);
			String origStr = rs.getString(9); // CAS327
			if (origStr==null || origStr.contentEquals(seqName)) origStr="";
			else origStr = "   " + origStr;

			String grps="   Clusters: ";
			sql = "SELECT PGstr from pog_members where UTid=" + seqIndex;
			rs = mDB.executeQuery(sql);
			while (rs.next()) {
				grps += rs.getString(1) + " ";
			}
			rs.close();
			
			String libStr="", rawStr="", normStr="";
			String [] rawTok = counts.split(" ");
			String [] normTok = rpkm.split(" ");	
			for (int i=0; i<rawTok.length; i++) {
				String [] tok = rawTok[i].split("=");
				if (tok.length==2) {
					libStr += String.format("%9s ", tok[0]);
					rawStr += String.format("%9s ", tok[1]);
				}
				tok = normTok[i].split("=");
				if (tok.length==2)
					normStr += String.format("%9s ", tok[1]);	
			}
			textArea = " " + seqName + origStr + "   AA Len: " + aaLen;
			if (!bAAonly)
				textArea += "   NT Len: " + ntlen + "  Best ORF: RF" + frame + " " + start +".." + end ;
			textArea += "\n\n";
			
			if (!libStr.equals(""))
				textArea += "           " + libStr + "\n" +
						"   Counts: " + rawStr + "\n" +
						"   TPM   : " + normStr + "\n\n";
			
			textArea += grps + "\n";
		}
		catch (Exception e) {
			ErrorReport.reportError(e, "trying to create sequence info"); 
			textArea = "error in processing";
		}
	}
	/***************************************************
	 * compute Pair Table
	 */
	private int loadPairTable() {
		try {
			if (!mDB.tableExist("pairwise")) return 0;
			thePairData = new ArrayList<PairListData> ();
			String methods = theParentFrame.getInfo().getStrMethodPrefix();
			if (methods!="") methods = "," + methods;
			String sql = 
					"SELECT UTid1, UTstr1, UTstr2, ntEval, aaEval, PCC, HITid, PAIRid " + methods +
					" from " + PAIR_TABLE + " as p " +
					" where UTid1=" + seqIndex + " or UTid2=" + seqIndex +
					" order by aaEval"; 
			int nMethods = theParentFrame.getInfo().getMethodPrefix().length;
			ResultSet rs = mDB.executeQuery(sql);
			while (rs.next()) {
				int id1= rs.getInt(1); 
				String name = (id1==seqIndex) ? rs.getString(3) : rs.getString(2);
				
				String seqMethods="";
				for (int i=9; i<9+nMethods; i++) {
					String m = rs.getString(i);
					if (m!=null && m!="") seqMethods += m + " ";
					
				}
				thePairData.add(new PairListData(name, rs.getDouble(4), rs.getDouble(5), 
						rs.getDouble(6), rs.getInt(7), rs.getInt(8), seqMethods));
			}
			for (PairListData pd : thePairData) {
				int hit = pd.hitID;
				if (hit>0) {
					rs = mDB.executeQuery("select description from unique_hits where HITid=" +hit);
					if (rs.next()) pd.desc = rs.getString(1);
				}
			}
			rs.close();
			return thePairData.size();
		}
		catch (Exception e) {ErrorReport.reportError(e, "trying to create pair table"); return 0;}
	}
	/********************************************************
	 * Compute Hit table 
	 * CAS305 changed tr.start, tr.end to aaCov and hitCov
	 */
	private int loadHitTable() {	
	try {
		if (!mDB.tableExist("unitrans_hits")) return 0;
		theHitData = new ArrayList<HitListData> ();
		hitMap = new HashMap <String, HitListData> ();
		
		bestHitGO[0]=null;
		int cnt=0;
		String sql = 
		  "SELECT tr.HITstr,  tr.percent_id, tr.alignment_len, " +
				" tr.e_value, tr.type, tr.bestEval, tr.bestAnno, tr.bestGO, " +
				" uq.description, uq.species, uq.nGO, uq.length " + 
				" FROM unitrans_hits as tr " +
				" JOIN " + HIT_TABLE + " as uq on tr.HITid=uq.HITid " + 
				" WHERE tr.UTid=" + seqIndex + " order by tr.bit_score DESC, tr.e_value ASC"; // CAS327 add bit_score
		ResultSet rs = mDB.executeQuery(sql);
		if (rs == null) ErrorReport.die("null result on database query in SeqDetails loadHitTable");
		
		while (rs.next()) {	
			int i=1;
			String hitName = rs.getString(i++);
			int pid = rs.getInt(i++);
			int alignlen = rs.getInt(i++);
			
			double eval = rs.getDouble(i++);
			String type = rs.getString(i++);
			boolean bestEval = rs.getBoolean(i++);
			boolean bestAnno = rs.getBoolean(i++);
			boolean bestGO = rs.getBoolean(i++);
			
			String desc = rs.getString(i++);
			String spec = rs.getString(i++);
			int nGO = rs.getInt(i++);
			int hitLen = rs.getInt(i++);
			
			if (nGO>0) {
				if (bestEval || bestAnno || bestHitGO[0]==null) {
					bestHitGO[0]=hitName;
					bestHitGO[1]=desc;
					bestHitGO[2]=eval+"";
				}
			}
			double aaCov= ((double) alignlen/(double) aaLen) * 100;
			int aCov = (int)((aaCov)+0.5);
			double hhCov= ((double) alignlen/(double) hitLen) * 100;
			int hCov = (int)((hhCov)+0.5);
			
			HitListData hd = new HitListData(hitName, pid, alignlen, aCov, hCov,eval, type, desc, spec, 
					nGO, bestEval, bestAnno, bestGO);
			theHitData.add(hd);
			hitMap.put(hitName, hd);
			cnt++;
		}
		if (rs!=null) rs.close();
		return cnt;
	}
	catch (Exception e) {ErrorReport.reportError(e, "Creating hit table"); return 0;}
	}
	
	/**********************************************
	 * create panel - with textArea and hitTable
	 **/
	private void createViewPanel() {
		mainPanel = Static.createPagePanel();
	
		JPanel tableAndHeader = Static.createPagePanel();
		
		textAreaTop = new JTextArea ( textArea );
		textAreaTop.setEditable( false );		
		textAreaTop.setFont(new Font("monospaced", Font.PLAIN, 12));
		int nTextWidth = Math.max((int)textAreaTop.
				getPreferredSize().getWidth() + 5, 600);		
		textAreaTop.setMaximumSize( new Dimension ( nTextWidth, 
				(int)textAreaTop.getPreferredSize().getHeight() ) );			
		textAreaTop.setAlignmentX(LEFT_ALIGNMENT);
		tableAndHeader.add ( textAreaTop );	
		
		if(thePairTable != null) {
			tableAndHeader.add(new JLabel("#Pairs " + thePairData.size()));
			tableAndHeader.add ( pairTableScroll );
		}
		if(theHitTable != null) {
			tableAndHeader.add(Box.createVerticalStrut(3));
			tableAndHeader.add(new JLabel("#Hits " + theHitData.size()));
			tableAndHeader.add ( hitTableScroll );	
		}
		tableAndHeader.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
		
		mainPanel.add( tableAndHeader );
		mainPanel.add( Box.createVerticalStrut(10) );			
		invalidate();
		mainPanel.add ( Box.createVerticalGlue() );	
		
		mainScroll = new JScrollPane ( mainPanel );
		mainScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		mainScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
		mainScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		UIHelpers.setScrollIncrements( mainScroll );
		
		add ( Box.createVerticalStrut(5) );
		add ( mainScroll );	
	}
	
	/** 
	 * hitTable  -- should not need changing 
	 ***/
	private void createHitTablePanel() {
		theHitTable = new JTable();
		theHitTable.setColumnSelectionAllowed( false );
		theHitTable.setCellSelectionEnabled( false );
		theHitTable.setRowSelectionAllowed( true );
		theHitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // CAS310
		theHitTable.setShowHorizontalLines( false );
		theHitTable.setShowVerticalLines( false );	
		theHitTable.setIntercellSpacing ( new Dimension ( 1, 0 ) );
		theHitModel = new HitTableModel(theHitData);
		theHitTable.setModel(theHitModel);
	
		JTableHeader header = theHitTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(theHitModel.new ColumnListener(theHitTable));
		header.setReorderingAllowed(true);
		resizeHitColumns();
		
		hitTableScroll = new JScrollPane(theHitTable);
		hitTableScroll.setBorder( null );
		Dimension size = theHitTable.getPreferredSize();
		size.height = NUM_HIT_ROWS * theHitTable.getRowHeight(); 
		size.width = Math.min(MAX_TABLE_WIDTH, theHitTable.getWidth());
		hitTableScroll.setPreferredSize(size);
		hitTableScroll.getViewport().setBackground(Color.WHITE);
		hitTableScroll.setAlignmentX(LEFT_ALIGNMENT);	
		hitTableScroll.setBorder( BorderFactory.createLineBorder( Color.BLACK ) );
	}
	
	private class HitTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;
		
		private String [] theLabels = null;
		
		public HitTableModel(ArrayList<HitListData> displayValues) { 
			theDisplayValues = displayValues; 
			theLabels = hitColumnHeadings();
		}
		public int getColumnCount() { return theLabels.length; }
        public int getRowCount() { return theDisplayValues.size(); }
        public Object getValueAt(int row, int col) { 
        	return theDisplayValues.get(row).getValueAt(col); }
        public String getColumnName(int col) { return theLabels[col]; }
        
        private ArrayList<HitListData> theDisplayValues = null;
		private boolean sortAsc = true;
		private int sortMode = HitListData.SORT_BY_EVAL;
        
  	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {
		    		table = t;
		    }
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
			    	Collections.sort(theDisplayValues, new HitListComparator(sortAsc, sortMode));
			    	table.tableChanged(new TableModelEvent(HitTableModel.this));
			    	table.repaint();
		    }
  	  	}
	}
	private class HitListComparator implements Comparator<HitListData> {
		public HitListComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}		
		public int compare(HitListData obj1, HitListData obj2) { 
			return obj1.compareTo(obj2, bSortAsc, nMode); }		
		private boolean bSortAsc;
		private int nMode;
	}	
	
	private void resizeHitColumns() {
		theHitTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int margin = 2;
		
		for(int x=0; x < theHitTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) 
				theHitTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;		
			TableCellRenderer renderer = col.getHeaderRenderer();
			if(renderer == null)
				renderer = theHitTable.getTableHeader().getDefaultRenderer();
			Component comp = renderer.getTableCellRendererComponent(theHitTable, 
					col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
			
			for(int y=0; y<theHitTable.getRowCount(); y++) {
				renderer = theHitTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(theHitTable, 
						theHitTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += 2 * margin;			
			col.setPreferredWidth(Math.min(maxSize, MAX_COL));
		}
		((DefaultTableCellRenderer) 
				theHitTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}	
	/************  End Hit table code *************************/
	
	/** 
	 * PairTable  -- should not need changing 
	 ***/
	private void createPairTablePanel() {
		thePairTable = new JTable();
		thePairTable.setBackground(Color.WHITE);
		thePairTable.setColumnSelectionAllowed( false );
		thePairTable.setCellSelectionEnabled( false );
		thePairTable.setRowSelectionAllowed( true );
		thePairTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // CAS310
		thePairTable.setShowHorizontalLines( false );
		thePairTable.setShowVerticalLines( false );	
		thePairTable.setIntercellSpacing ( new Dimension ( 1, 0 ) );
		thePairModel = new PairTableModel(thePairData);
		thePairTable.setModel(thePairModel);
	
		JTableHeader header = thePairTable.getTableHeader();
		header.setUpdateTableInRealTime(true);
		header.addMouseListener(thePairModel.new ColumnListener(thePairTable));
		header.setReorderingAllowed(true);
		resizePairColumns();
		
		pairTableScroll = new JScrollPane(thePairTable);
		pairTableScroll.setBackground(Color.WHITE);
		pairTableScroll.setBorder( null );
		Dimension size = thePairTable.getPreferredSize();
		size.height = NUM_PAIR_ROWS * thePairTable.getRowHeight(); 
		size.width = Math.min(MAX_TABLE_WIDTH, thePairTable.getWidth());
		pairTableScroll.setPreferredSize(size);
		pairTableScroll.getViewport().setBackground(Color.WHITE);
		pairTableScroll.setAlignmentX(LEFT_ALIGNMENT);			
	}
	
	private class PairTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 774460555629612058L;
		
		private String [] theLabels = null;
		
		public PairTableModel(ArrayList<PairListData> displayValues) { 
			theDisplayValues = displayValues; 
			theLabels = pairColumnHeadings();
		}
		public int getColumnCount() { return theLabels.length; }
        public int getRowCount() { return theDisplayValues.size(); }
        public Object getValueAt(int row, int col) { 
        	return theDisplayValues.get(row).getValueAt(col); }
        public String getColumnName(int col) { return theLabels[col]; }
        
        private ArrayList<PairListData> theDisplayValues = null;
		private boolean sortAsc = true;
		private int sortMode = PairListData.SORT_BY_NTEVAL;
        
  	  	public class ColumnListener extends MouseAdapter {
		    protected JTable table;

		    public ColumnListener(JTable t) {
		    		table = t;
		    }
		    public void mouseClicked(MouseEvent e) {
			    	TableColumnModel colModel = table.getColumnModel();
			    	int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
			    	if (columnModelIndex < 0) return; 
			    	int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();
			    	if (modelIndex < 0) return;
			    	
			    	if (sortMode == modelIndex) sortAsc = !sortAsc;
			    	else sortMode = modelIndex;
	
			    	for (int i = 0; i < table.getColumnCount(); i++) { 
			    		TableColumn column = colModel.getColumn(i);
			    		column.setHeaderValue(getColumnName(column.getModelIndex()));
			    	}
			    	table.getTableHeader().repaint();
			    	Collections.sort(theDisplayValues, new PairListComparator(sortAsc, sortMode));
			    	table.tableChanged(new TableModelEvent(PairTableModel.this));
			    	table.repaint();
		    }
  	  	}
	}
	private class PairListComparator implements Comparator<PairListData> {
		public PairListComparator(boolean sortAsc, int mode) {
			bSortAsc = sortAsc;
			nMode = mode;
		}	
		public int compare(PairListData obj1, PairListData obj2) {
			return obj1.compareTo(obj2, bSortAsc, nMode); 
		}	
		private boolean bSortAsc;
		private int nMode;
	}	
	
	private void resizePairColumns() {
		thePairTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int margin = 2;
		
		for(int x=0; x < thePairTable.getColumnCount(); x++) {
			DefaultTableColumnModel colModel = (DefaultTableColumnModel) 
				thePairTable.getColumnModel();
			TableColumn col = colModel.getColumn(x);
			int maxSize = 0;		
			TableCellRenderer renderer = col.getHeaderRenderer();
			if(renderer == null)
				renderer = thePairTable.getTableHeader().getDefaultRenderer();
			Component comp = renderer.getTableCellRendererComponent(thePairTable, 
					col.getHeaderValue(), false, false, 0, 0);
			maxSize = comp.getPreferredSize().width;
			
			for(int y=0; y<thePairTable.getRowCount(); y++) {
				renderer = thePairTable.getCellRenderer(y, x);
				comp = renderer.getTableCellRendererComponent(thePairTable, 
						thePairTable.getValueAt(y, x), false, false, y, x);
				maxSize = Math.max(maxSize, comp.getPreferredSize().width);
			}
			maxSize += 2 * margin;	
			if (x==thePairTable.getColumnCount()-1) col.setPreferredWidth(maxSize);
			else col.setPreferredWidth(Math.min(maxSize, MAX_COL)); 
		}
		((DefaultTableCellRenderer) 
				thePairTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}	
	/************  End Pair table code *************************/
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	/**
	 * XXX HitListData class for Hit table
	 */
	private String [] hitColumnHeadings() { 
		String [] x = {"HitID", "Type", "Description", "Species", "#GO", "Best", "E-value",
				       "%Sim", "Align", "%aaCov", "%hitCov"}; // CAS305 was start and end
		return x;
	}
	private class HitListData {
		public static final int SORT_BY_NAME = 0; 
		public static final int SORT_BY_TYPE = 1; 
		public static final int SORT_BY_DESC = 2; 
		public static final int SORT_BY_SPEC = 3; 
		public static final int SORT_BY_GO = 4;
		public static final int SORT_BY_BEST = 5;
		public static final int SORT_BY_EVAL = 6; 
		public static final int SORT_BY_PERCENT = 7; 
		
		public static final int SORT_BY_ALIGNLEN = 8;
		public static final int SORT_BY_START = 9; 
		public static final int SORT_BY_END = 10; 
		
		public HitListData(String name, int percent, int len, int start, int end, 
				double eval, String t, String d, String s, int ng, boolean bE, boolean bA, boolean bG) {
			hitName = name;
			nPercent = percent;
			nAlignLen = len;
			nStart = start;
			nEnd = end;
			dEVal = eval;
			type = t;
			desc = d;
			species = s;
			nGO = ng;
			bestEval = bE;
			bestAnno = bA;
			if (bestEval && bestAnno && bG) best="All";
			else {
				if (bestEval) 	best="BS";
				if (bestAnno) 	best=Static.strMerge(best, "AN");
				if (bG) 		best=Static.strMerge(best, "WG");
			}
		}
		
		public Object getValueAt(int pos) {
			switch(pos) {
			case 0: return hitName;
			case 1: return type;
			case 2: return desc;
			case 3: return species;
			case 4: return nGO;
			case 5: return best;
			case 6: 
				if (dEVal==0.0) return "0.0"; 
				else return String.format("%.0E", dEVal); // CAS305 Out.formatDouble(dEVal); 
			case 7: return nPercent;
			case 8: return nAlignLen;
			case 9: return nStart;
			case 10: return nEnd;
			}
			return null;
		}
		
		public int compareTo(HitListData obj, boolean sortAsc, int field) {
			int order = 1;
			if(!sortAsc) order = -1;
			
			switch(field) { // CAS304 new Integer (x) -> (Integer) x
			case SORT_BY_NAME: return order * hitName.compareTo(obj.hitName);
			case SORT_BY_TYPE: return order * type.compareTo(obj.type);
			case SORT_BY_DESC: return order * desc.compareTo(obj.desc);
			case SORT_BY_SPEC: return order * species.compareTo(obj.species);
			case SORT_BY_GO: return order * ((Integer) nGO).compareTo((Integer) obj.nGO);
			case SORT_BY_BEST: return order * best.compareTo(obj.best);
			case SORT_BY_EVAL: return order * ((Double) dEVal).compareTo((Double) obj.dEVal);
			case SORT_BY_PERCENT: return order * ((Integer) nPercent).compareTo((Integer) obj.nPercent);
			case SORT_BY_ALIGNLEN: return order * ((Integer) nAlignLen).compareTo((Integer) obj.nAlignLen);
			case SORT_BY_START: return order * ((Integer) nStart).compareTo((Integer)obj.nStart);
			case SORT_BY_END: return order * ((Integer)nEnd).compareTo((Integer) obj.nEnd);
			}
			return 0;
		}
		private String hitName = "";
		private int nPercent;
		private int nAlignLen;
		private int nStart = -1;
		private int nEnd = -1;
		private double dEVal = 0;
		private String type = "";
		private String desc = "";
		private String species = "";
		private int nGO=0;
		private boolean bestEval, bestAnno;
		private String best="";
	} // end HitData class
	 
	
	/**
	 * XXX PairListData class for Pair table
	 */
	private String [] pairColumnHeadings() { 
		String [] x = {"PairID", "SeqID", "NT E-val","AA E-val", "PCC", "Pair Description", "Shared Clusters"};
		return x;
	}
	private class PairListData {
		public static final int SORT_BY_PAIR = 0; 
		public static final int SORT_BY_NAME = 1; 
		public static final int SORT_BY_NTEVAL = 2; 
		public static final int SORT_BY_AAEVAL = 3; 
		public static final int SORT_BY_PCC = 4; 
		public static final int SORT_BY_DESC = 5;
		public static final int SORT_BY_METHODS = 6;
	
		public PairListData(String name, double nt, double aa, double p, int hit, int n, String m) {
			seqID = name;
			ntEval = nt;
			aaEval = aa;
			pcc = p;
			hitID = hit;
			pairID = n;
			methods = m;
		}
		
		public Object getValueAt(int pos) {
			switch(pos) {
			case 0: return pairID;
			case 1: return seqID;
			case 2:
				if (ntEval < -1.0) return new String("-");
				return Out.formatDouble(ntEval); 
			case 3: 
				if (aaEval < -1.0) return new String("-");
				return Out.formatDouble(aaEval); 
			case 4: 
				if (pcc==Globalx.dNoVal) return "-";
				return String.format("%5.2f", pcc);					
			case 5:
				return desc;
			case 6:
				return methods;											
			}
			return null;
		}
		
		public int compareTo(PairListData obj, boolean sortAsc, int field) {
			int order = 1;
			if(!sortAsc) order = -1;
			
			switch(field) {
			case SORT_BY_PAIR: return order * ((Integer) pairID).compareTo((Integer) obj.pairID);
			case SORT_BY_NAME: return order * seqID.compareTo(obj.seqID);
			case SORT_BY_NTEVAL: return order * ((Double) ntEval).compareTo((Double) obj.ntEval);
			case SORT_BY_AAEVAL: return order * ((Double) aaEval).compareTo((Double) obj.aaEval);
			case SORT_BY_PCC: return order * ((Double) pcc).compareTo((Double)obj.pcc);
			case SORT_BY_DESC: return order * desc.compareTo(obj.desc);
			case SORT_BY_METHODS: return order * methods.compareTo(obj.methods);
			}
			return 0;
		}
		private String seqID = "";
		private int pairID = 0;
		private double ntEval = -2;
		private double aaEval = -2;
		private double pcc = -2;
		private String desc = "";
		private int hitID=0; // not for display
		private String methods = "";
	} 
	public boolean isAAonly() { return bAAonly;}
	
	public String getSelectedSeqID() {
		int [] selections = thePairTable.getSelectedRows();
		if (selections.length==0) return thePairData.get(0).seqID;
		
		return thePairData.get(selections[0]).seqID;
	}
	public void clearSelection() {
		theHitTable.clearSelection();
		thePairTable.clearSelection();
	}
	public String getSelectedHitID() {
		int [] selections = theHitTable.getSelectedRows();
		if (selections.length==0) return bestHitGO[0];
		
		return theHitData.get(selections[0]).hitName;
	}
	public String getSelectedHitDesc() {
		int [] selections = theHitTable.getSelectedRows();
		if (selections.length==0) return bestHitGO[1];
		
		return theHitData.get(selections[0]).desc;
	}
	public String [] getHitInfo() {
		if (cntSelectedHits()==0) return bestHitGO;
		
		String hit = getSelectedHitID();
		String [] info = new String [3];
		info[0] = hit;
		info[1] = hitMap.get(hit).desc;
		info[2] = String.format("%.0E", hitMap.get(hit).dEVal);
		return info;
	}
	private int cntSelectedHits() {
		if (theHitTable == null) return 0;
		int [] selections = theHitTable.getSelectedRows();
		return selections.length;
	}
	
	public HashSet <String> getHitNameGO() { 
		if (hitMap==null || hitMap.size()==0) return null;
		HashSet <String> hits = new HashSet <String> ();
		for (String hitID : hitMap.keySet())  {
			int ngo = hitMap.get(hitID).nGO;
			if (ngo>0)
				hits.add(hitID);
		}
		return hits;
	}
	public double getHitEval(String hitID) {
		return hitMap.get(hitID).dEVal;
	}
	public int getHitBest(String hitID) {
		int e = (hitMap.get(hitID).bestEval) ? 1 : 0;
		int a = (hitMap.get(hitID).bestAnno) ? 1 : 0;
		return e+a;
	}
	private String textArea = "";
	private HitTableModel theHitModel = null;
	private ArrayList<HitListData> theHitData = null;
	private HashMap <String, HitListData> hitMap;
	
	private PairTableModel thePairModel = null;
	private ArrayList<PairListData> thePairData = null;
	// Views
	private JPanel mainPanel = null;
	private JScrollPane mainScroll = null;
	
	private JTextArea textAreaTop = null;
	private JTable theHitTable = null;
	private JScrollPane hitTableScroll = null;
	
	private JTable thePairTable = null;
	private JScrollPane pairTableScroll = null;
	
	// saved from db load
	private boolean bAAonly=false;
	
	// parameters
	private MTCWFrame theParentFrame = null;
	private int seqIndex = -1;
	private DBConn mDB = null;
	
	private String [] bestHitGO = new String [3];
	private int aaLen=0;
	
	// for tag on left panel
	private String seqName="";
}
