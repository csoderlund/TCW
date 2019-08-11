package sng.viewer.panels.Basic;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.ButtonComboBox;


public class BasicGOLoadFromDB  {
	private static final int GOindex=BasicGOTablePanel.GOindex;
	private static final int GOdomain=BasicGOTablePanel.GOdomain;
	
	public static final int BUILD=1;
	public static final int TRIM=2;
	
	public BasicGOLoadFromDB(STCWFrame f, BasicGOQueryTab g, BasicGOTablePanel t) {
		theMainFrame=f;
		goQueryPanel=g;
		goTablePanel=t;
	}
	public boolean runQuery(int runtype) {
		final int type=runtype;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					if (type==BUILD) loadBuildTable();
					if (type==TRIM) applyDETreeFilter();
				}
				catch (Exception err) {
					goQueryPanel.setErrorStatus("Error during query");
					JOptionPane.showMessageDialog(null, "Query failed due to unknown reasons ");
					ErrorReport.reportError(err, "Internal error: building hit table");
				}
			}
		});
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
		return true;
	}
	/*************************************************************
	 * XXX type = -1 down, 0 normal, 1 up
	 * if not 0, then get cutoff from assem_msg.goseq (RhRo:0.05, etc)
	 * then query contig.P_RhRo for (abs()<0.05 and <0 or >0) and is in go_unitrans table.
	 * this just replaces nSeq.
	 */
	public void loadBuildTable() {
		try {
			String query = goQueryPanel.makeQuerySelectClause();	
			Class<?> [] COLUMN_TYPES = goTablePanel.getColumnTypes();
			int endStatic = goTablePanel.getNumStaticCols();
			int endEC = goTablePanel.getEndEC();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int numCols = rsmd.getColumnCount();
			int cnt=0, rowcnt=goTablePanel.getRowCount();
			
			// deUp and deDn
			String deWhere = goQueryPanel.makeNseqClause(true); 
			Vector <Integer> gonum = new Vector <Integer> ();
			String [] terms = Globalx.GO_TERM_LIST;
			String [] abbr =  Globalx.GO_TERM_ABBR;
			
			while(rs.next()) {
				Object [] vals = new Object[numCols]; //COLUMN_MYSQL.length + theDENames.size()];
				for(int x=0; x<vals.length; x++) {
					if (x>=endEC) vals[x] = rs.getDouble(x+1);
					else if (x>=endStatic) vals[x] = rs.getInt(x+1); 
					else if(COLUMN_TYPES[x] == Long.class) vals[x] = rs.getLong(x+1);
					else if(COLUMN_TYPES[x] == Integer.class) vals[x] = rs.getInt(x+1);
					else if(COLUMN_TYPES[x] == Double.class) vals[x] = rs.getDouble(x+1);
					else if(COLUMN_TYPES[x] == String.class) {
						vals[x] = rs.getString(x+1);
		
						if (x==GOdomain) {
							for (int i=0; i<terms.length; i++) {
								if (vals[x].equals(terms[i])) {
									vals[x] = abbr[i];
									break;
								}
							}
						}
					}
					// make list of GOs if DE selected with nSeqs
					if (x==BasicGOTablePanel.GOindex && deWhere!="")
						gonum.add((Integer) vals[BasicGOTablePanel.GOindex]);
				} 
				goTablePanel.addResult(vals);
				cnt++; rowcnt++;
			}
			rs.close();
			
			// The nSeq column value needs to be changed, and if <nSeqFilter, then removed.
			int nSeqFilter = goQueryPanel.getNseqs();
			if (deWhere!=null) {
				deWhere+="=";
				
				int cntAdd=0, cntTotal = gonum.size();
				// needs to be in descending order because rows may be removed based on nseq value
				for (int row=gonum.size()-1; row>=0; row--) {
					int nseq = mDB.executeCount("select count(*) " + deWhere + gonum.get(row));
					
					boolean rm = goTablePanel.changeNSeq(row, nseq, nSeqFilter);
					if (rm) {cnt--; rowcnt--; }
					else cntAdd++;
					
					if (row%1000==0) 
						goQueryPanel.setStatus("Added " + cntAdd + " of possible " +cntTotal + "...");
				}
			}
			mDB.close();
			
			goQueryPanel.loadBuildFinish(cnt, rowcnt);
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Querying GO table");
			goQueryPanel.setErrorStatus("Error in query");
		}
	}
	/**
	// XXX DBQ pja_gotree to create DE trim set
	// Do the tree-aware GO DE filtering on the GOs currently in the result set,
	// using the current DE filter threshold setting.
	//
	// The method is to traverse the tree depth-first, using the pre-calculated pja_gotree, 
	// and compare each parent/child. If the child is lower, the parent is removed (note
	// that the initial theResults list should have all the results which passed the
	// query conditions, and the final trimmed set will be a subset of this). 
	// The set of siblings is tracked at each level. 
	// After a node's subtree is fully walked, i.e. when moving back up-level to that node,
	// if that node has not yet been removed, then it is better than all its child nodes,
	// hence they are removed.
	**/
	private void applyDETreeFilter()
	{
		try
		{
			Vector<Object []> theResults = goTablePanel.getTheResults();
			ButtonComboBox cmbTermTypes = goQueryPanel.getCmbTermTypes();
			
			// Make some helper maps showing the DE of a go and which result row it is
			HashMap<Integer,Integer> go2row = new HashMap<Integer,Integer>();
			HashMap<Integer,Double> go2DE = new HashMap<Integer,Double>();

			int rnum = 0;
			for (Object[] r : theResults)
			{
				Integer go = (Integer)r[GOindex];
				Double DE = combinedGODEValue(r);
				go2row.put(go,rnum);
				go2DE.put(go, DE);
				rnum++;
			}
			String query = "select gonum, level+1, tblidx from pja_gotree where 1 "; 
			if(cmbTermTypes.getSelectedIndex() > 0) {
				query += " and term_type = '" + cmbTermTypes.getSelectedItem() + "' ";
			}
			query += " order by tblidx asc"; 
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery(query);
			
			int prevLevel = 0;
			Integer prevGO = 0;
			double prevDE = 1.0;
			HashSet<Integer>         removeNode =  new HashSet<Integer>();
			
			Stack <Integer>          curGoPath =   new Stack<Integer>();
			Stack <Double>           curDEPath =   new Stack<Double>();
			Stack <HashSet<Integer>> pathSibs =    new Stack<HashSet<Integer>>();
			HashSet <Integer>        curSibs =     null; // collecting siblings at each level	
			
			while (rs.next())
			{
				Integer go = rs.getInt(1);
				int level = rs.getInt(2);
				double DE = (go2DE.containsKey(go) ? go2DE.get(go) : 1.0);
				if (level > prevLevel+1)
				{
					ErrorReport.reportError("Too large level jump: " + level + " to " + (prevLevel+1));
				}
				if (level < prevLevel)
				{
					// We're jumping back up the tree, possibly more than one level
					// This means that the current sibling set is complete, as are all those
					// which we pass through on the way up. 
					// For each sibling set, if their parent is DE and has not yet been removed, it means
					// all the kids should be removed.
					
					while(prevLevel > level)
					{
						Integer parentGO = curGoPath.peek();
						if (go2row.containsKey(parentGO)) 
						{
							int parentRow = go2row.get(parentGO);
							if (!removeNode.contains(parentRow)) // parent exists, remove kids
							{
								for (Integer kid : curSibs)
								{
									if (go2row.containsKey(kid))
									{
										if (!removeNode.contains(go2row.get(kid)))
										{
											removeNode.add(go2row.get(kid));
										}
									}
								}
							}
						}
						curGoPath.pop();
						curDEPath.pop();
						curSibs = pathSibs.pop();
						prevLevel--;
					}
				}
				else if (level > prevLevel)
				{
					// We just went one level deeper - push the *previous* go and its current sibling set (which
					// may not yet be complete) onto the stack
					// Although note that we start by pushing 0
					curGoPath.push(prevGO);
					curDEPath.push(prevDE);
					pathSibs.push(curSibs);
					
					prevLevel = level;
					curSibs = new HashSet<Integer>();
				}
				
				Integer parentGO = curGoPath.peek();
				double parentDE = curDEPath.peek();
				if (go2row.containsKey(go) && go2row.containsKey(parentGO))
				{
					if (parentDE >= DE)
					{
						removeNode.add(go2row.get(parentGO)); // parent is not better, hence prefer child
					}
				}
				curSibs.add(go);
				prevGO = go;
				prevDE = DE;
			}
			rs.close(); 
			mDB.close();
			
			// transfer results
			for (int i = 0; i < theResults.size(); i++)
			{
				if (!removeNode.contains(i))
				{
					goTablePanel.treeAdd((Integer)theResults.get(i)[GOindex]);
				}
			}
			goQueryPanel.trimFinish();
		}
		catch(Exception e){
			ErrorReport.prtReport(e, "Compute trimmed set");
			goQueryPanel.setErrorStatus("Error in computing trimmed set");
		}		
	}
	public Double combinedGODEValue(Object[] r)
	{
		Vector <String> deColumnNames = goTablePanel.getDEcolumns();
		if (deColumnNames.size() == 0) return 1.0;
		
		JCheckBox [] chkDEfilter = goQueryPanel.getDEselect() ;
		int endEC = goTablePanel.getEndEC();
		boolean hasDE=false;
		Double ret = 1.0;
		for (int i = 0; i < deColumnNames.size(); i++)
		{
			JCheckBox chk = chkDEfilter[i];
			if (chk.isSelected())
			{
				int pos = endEC + i;
				Double de = (Double)r[pos];
				if (de<0.0) hasDE=true;
				ret *= de;
			}
		}
		if (!hasDE) return 2.0;
		return ret;
	}
	private STCWFrame theMainFrame;
	private BasicGOQueryTab goQueryPanel; // the parent panel
	private BasicGOTablePanel goTablePanel;
}
