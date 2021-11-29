package sng.viewer.panels.Basic;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashSet;
import java.util.Vector;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;

/**********************************************************
 * Load data from database for the GO table
 * CAS336 Trim was disabled after v317. The code was removed in v336 and replaced with "Select end GOs".
 */
public class BasicGOLoadFromDB  {
	private static final int GOindex=BasicGOTablePanel.GOindex;
	private static final int GOdomain=BasicGOTablePanel.GOdomain;
	
	public static final int BUILD=1, ADD=2, SELECT=3;
	
	public BasicGOLoadFromDB(STCWFrame f, BasicGOFilterTab g, BasicGOTablePanel t) {
		theMainFrame=f;
		filterPanel=g;
		tablePanel=t;
	}
	public boolean runQuery(int runtype) {
		final int type=runtype;
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					loadBuildTable(type);
				}
				catch (Exception err) {
					Out.PrtErr("");
					filterPanel.setErrorStatus("Error during query");
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
	public void loadBuildTable(int type) {
		try {
			String query 				= filterPanel.makeQuerySelectClause();	
			if (query==null) return;
			
			Class<?> [] COLUMN_TYPES 	= tablePanel.getColumnTypes();
			int endStatic 				= tablePanel.getNumStaticCols();
			int endEvC 					= tablePanel.getEndEvC();
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs = mDB.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int numCols = rsmd.getColumnCount();
			int cnt=0, rowcnt=tablePanel.getRowCount();
			
			// deUp and deDn
			String deWhere = filterPanel.makeNseqClause(true); 
			Vector <Integer> gonum = new Vector <Integer> ();
			String [] terms = Globalx.GO_TERM_LIST;
			String [] abbr =  Globalx.GO_TERM_ABBR;
			
			HashSet <Integer> goSet = new HashSet <Integer> ();
			
			int nRow=1; // CAS336 add row
			while(rs.next()) {
				Object [] vals = new Object[numCols]; //COLUMN_MYSQL.length + theEvCat.size() + thePvalNames.size()];
				for(int x=0; x<vals.length; x++) {
					if (x==0)									vals[x] = nRow++;
					else if (x>=endEvC) 						vals[x] = rs.getDouble(x+1); // pval
					else if (x>=endStatic) 						vals[x] = rs.getString(x+1); // EvC
					else if(COLUMN_TYPES[x] == Long.class) 		vals[x] = rs.getLong(x+1);
					else if(COLUMN_TYPES[x] == Integer.class) 	vals[x] = rs.getInt(x+1);
					else if(COLUMN_TYPES[x] == Double.class) 	vals[x] = rs.getDouble(x+1);
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
				} 
				// finish building row, now process
				if (type!=SELECT) {
					tablePanel.addResult(vals);	
					if (deWhere!="")
						gonum.add((Integer) vals[BasicGOTablePanel.GOindex]);
				}
				else { // CAS336 add select
					goSet.add((Integer) vals[BasicGOTablePanel.GOindex]);
				}
				cnt++; rowcnt++;
			}
			rs.close();
			
			if (type==SELECT) {
				mDB.close();
				tablePanel.selectRows("Query", goSet);
				filterPanel.loadSelectFinish(cnt);
				return;
				
			}
			// The nSeq column value needs to be changed, and if <nSeqFilter, then removed.
			int nSeqFilter = filterPanel.getNseqs();
			if (deWhere!=null) {
				deWhere+="=";
				
				int cntAdd=0, cntTotal = gonum.size();
				// needs to be in descending order because rows may be removed based on nseq value
				for (int row=gonum.size()-1; row>=0; row--) {
					int nseq = mDB.executeCount("select count(*) " + deWhere + gonum.get(row));
					
					boolean rm = tablePanel.changeNSeq(row, nseq, nSeqFilter);
					if (rm) {cnt--; rowcnt--; }
					else cntAdd++;
					
					if (row%1000==0) 
						filterPanel.setStatus("Added " + cntAdd + " of possible " +cntTotal + "...");
				}
			}
			mDB.close();
			
			filterPanel.loadTableFinish(cnt, rowcnt);
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Querying GO table");
			filterPanel.setErrorStatus("Error in query");
		}
	}
	
	/** CAS336 this is similar to the trim but does not need DE **/
	public void computeEnds() {
		try {
			listFromTable(); // create allNodes
			
			HashSet <Integer>  trimGOSet = new HashSet<Integer> ();
			
			for (Node nObj : allNodeMap.values()) {
				if (nObj.child.size()==0)
					trimGOSet.add(nObj.gonum);
			}
			allNodeMap.clear();
			
			tablePanel.selectRows("Terminal terms ", trimGOSet);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Trim GOs");}
	}
		
	/**
	 *  Create list of nodes from table and add parent list from DB, but only with parents from table */
	private void listFromTable() {
		try {
			Vector<Object []> theResults = tablePanel.getTheResults();
			for (Object[] r : theResults)
			{
				Node nodeObj = new Node ();
				nodeObj.gonum = (Integer)r[GOindex];
				nodeObj.ont = (String) r[GOdomain];
				
				allNodeMap.put(nodeObj.gonum, nodeObj);
			}
			
			DBConn mDB = theMainFrame.getNewDBC();
			ResultSet rs;
			String sql = "select child from go_graph_path where relationship_type_id!=3 and ancestor = ";
			
			for (Node nObj : allNodeMap.values()) {
				rs = mDB.executeQuery(sql + nObj.gonum);
			
				while (rs.next()) {
					int child = rs.getInt(1);
					if (allNodeMap.containsKey(child))
						allNodeMap.get(nObj.gonum).add(child);	
				}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Create list from table");}
	}
	
	private class Node implements Comparable <Node>{
		int gonum;
		String ont;
		
		void add(int pgo) {
			if (!child.contains(pgo)) child.add(pgo);
		}
		public int compareTo (Node n) {
			return gonum=n.gonum;
		}
		TreeSet <Integer> child = new TreeSet <Integer> ();
	}
	private TreeMap <Integer, Node> allNodeMap = new TreeMap <Integer, Node> ();  // gonum, Node
	
	/***********************************************************************/
	 
	private STCWFrame theMainFrame;
	private BasicGOFilterTab filterPanel; // the parent panel
	private BasicGOTablePanel tablePanel;
}
