package cmp.viewer.table;

/*****************************************************
 * All Tables calls this for Export
 */
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Vector;
import java.util.TreeMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.file.FileC;
import util.file.FileWrite;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.Stats;
import util.ui.ButtonComboBox;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class TableUtil {
	private final String delim =  FileC.TSV_DELIM;
	
	private static final String BLASTDIR = Globalx.rHITDIR; 
	private static final String ID_SQL = FieldData.SEQ_SQLID; 
	private static final String ID1_SQL = FieldData.ID1_SQLID; 
	private static final String ID2_SQL = FieldData.ID2_SQLID; 
	private static final String PAIR_ID = FieldData.PAIRID; 	
	private static final String GRP_SQL = FieldData.GRP_SQLID;
	private static final String GRP_ID = FieldData.CLUSTERID;
	private static final String GO_FORMAT = Globals.GO_FORMAT;
	
	private static final String GrpPrefix = "ClusTable";
	private static final String PairPrefix = "PairTable";
	private static final String SeqPrefix = "SeqTable";
	private static final String HitPrefix = "HitTable";
	
	public TableUtil() {}
	
	public TableUtil(MTCWFrame theViewerFrame) {
		this.theViewerFrame = theViewerFrame;
	}
	// works for all three tables
	// For each integer or double column, have row with the seven fields
	public void statsPopUp(final String title, final SortTable theTable) {
		String info = statsPopUpCompute(theTable, title);
		UserPrompt.displayInfoMonoSpace(null, title, info);		
	}
	private String statsPopUpCompute(SortTable theTable, String sum) {
	try {
		String [] fields = 
			{"Column", "Average", "StdDev", "Median", "Range", "Low", "High", "Sum", "Count"};
		int [] justify =   {1, 0,  0, 0, 0, 0, 0, 0, 0};
		int intCol = 4;
		
		int nCol=  fields.length;
		int nRow = 0;
		int tableRows=theTable.getRowCount(), tableCols = theTable.getColumnCount();
		   
		// get column count so now number of rows in result table
		for(int y=0; y < tableCols; y++) {
		 	String colName = theTable.getColumnName(y);
		 	if(colName.equals(FieldData.ROWNUM)) continue;
		 	if(colName.equals(FieldData.PAIRID)) continue; 
		 	
			Class <?> dtype = theTable.getColumnClass(y);
			if (dtype!=String.class) nRow++;
		}
	    String [][] rows = new String[nRow][nCol];
		double [] dArr = new double [tableRows];

	    for(int y=0, r=0; y < tableCols; y++) {
		 	String colName = theTable.getColumnName(y);
		 	if(colName.equals(FieldData.ROWNUM)) continue;
		 	if(colName.equals(FieldData.PAIRID)) continue; 
		 	
			Class <?> dtype = theTable.getColumnClass(y);
			if (dtype==String.class) continue;
			
			boolean isInt=false;
			for (int x=0; x<tableRows; x++) {
				if (theTable.getValueAt(x,y) != null) {
					if (dtype == Float.class) {
						float f = (Float)  theTable.getValueAt(x, y);
						dArr[x] = f;
					}
					else if (dtype == Double.class) {
						dArr[x] = (Double) theTable.getValueAt(x, y);
					}
					else if (dtype == Integer.class) {
						int j = (Integer) theTable.getValueAt(x, y);
						dArr[x] = (double) j;
						isInt=true;
					}
					else {
						Out.prt(colName + " " + dtype);
					}
				}
			}
			double [] results = Stats.averages(colName, dArr, false); // CAS313 scores are now >= 0
			rows[r][0] = colName;
			for (int i=0, c=1; i<results.length; i++, c++) {
				if ((i>=intCol && isInt) || (i==results.length-1)) {
					if (results[i]<0) rows[r][c] = "N/A"; // overflow
					else              rows[r][c] = String.format("%,d", (long) results[i]);
				}
				else                  rows[r][c] = Out.formatDouble(results[i]);
			}
			r++;
		}
		String statStr = Out.makeTable(nCol, nRow, fields, justify, rows);
		statStr += "\n" + tableRows + " rows; " + sum;
		return statStr;
	
	} catch(Exception e) {ErrorReport.reportError(e, "Error create column stats"); return "Error"; }
	}
	    
	// works for all three tables
	public void exportTableTab(Component btnTable, SortTable theTable, int typeTable) {
	try {
		String prefix="UNK";
		if (typeTable==Globals.bGRP) prefix = GrpPrefix;
		else if (typeTable==Globals.bPAIR) prefix = PairPrefix;
		else if (typeTable==Globals.bSEQ) prefix = SeqPrefix;
		else if (typeTable==Globals.bHIT) prefix = HitPrefix;
		
		BufferedWriter outFH = fwObj.getBWriter(btnTable, prefix, prefix, FileC.fTSV, FileC.wAPPEND);
		if (outFH==null) return; // user cancels
		
		// print header line
		String val = "";
		int rowNumColIndex = -1;
		outFH.write("#"); // CAS314
		for(int x=0; x<theTable.getColumnCount()-1; x++) {
			val = theTable.getColumnName(x);
			if (val.equals(FieldData.ROWNUM)) rowNumColIndex = x;
			val = val.replaceAll("\\s", "");
			outFH.write(val + delim);
		}
		val = theTable.getColumnName(theTable.getColumnCount()-1).replaceAll("\\s", "");
		outFH.write(val + "\n");
		outFH.flush();
		
		// print table
		for(int x=0; x<theTable.getRowCount(); x++) {
			for(int y=0; y<theTable.getColumnCount()-1; y++) {
				if (y == rowNumColIndex) outFH.write((x+1) + delim);
				else {
					Object obj = theTable.getValueAt(x, y);
	                val = (obj == null) ? Globalx.sNoVal : obj.toString(); // CAS305 convert(obj.toString()); 
					outFH.write(val + delim);
				}
			}
			Object obj = theTable.getValueAt(x, theTable.getColumnCount()-1);
            val = (obj == null ? Globalx.sNoVal  : obj.toString());
			outFH.write(val + "\n");
			outFH.flush();
		}
		Out.prt("Complete writing " + theTable.getRowCount() + " lines");
		outFH.close();
	} catch(Exception e) {ErrorReport.reportError(e, "Error saving file");
	} catch(Error e) {ErrorReport.reportFatalError(e, "Fatal error saving file", theViewerFrame);}
	}
	/******************************************************************
	 * Export sequences from cluster table
	 */
	 public void exportGrpSeqFa(Component btnTable, TableData theTableData, String column) {
		try { 
			int n = theTableData.getNumRows();
			String name = column + n + "Clusters";
			BufferedWriter out = fwObj.getBWriter(btnTable, name, name, FileC.fFASTA, FileC.wAPPEND);
			if (out==null) return;
			
			int SQLidx = theTableData.getColumnHeaderIndex(GRP_SQL);
			int TABidx = theTableData.getColumnHeaderIndex(GRP_ID);
			
			int [] grpID = new int [n];
			String [] grpStr = new String [n];
			for(int i=0; i<n; i++) {
				grpID[i] = (Integer) theTableData.getValueAt(i, SQLidx);
				grpStr[i] = (String) theTableData.getValueAt(i, TABidx);
			}
			DBConn dbc = theViewerFrame.getDBConnection();	
			
			int cnt=0;
			for (int i=0; i<grpID.length; i++) {
				ResultSet rs = dbc.executeQuery("SELECT unitrans.UTstr, unitrans." + column +   
						" FROM unitrans JOIN pog_members " +
						" WHERE pog_members.PGid= " + grpID[i] + 
						" AND unitrans.UTid=pog_members.UTid");
				String cid = "     Cluster=" + grpStr[i];
				while (rs.next()) {
					out.write(">" + rs.getString(1) + cid + "\n" +rs.getString(2) + "\n");
					out.flush();
					cnt++;
				}
				if (cnt%100==0)
					System.err.print("   Wrote " + cnt + " clusters...\r");
				rs.close();
			}
			out.close();
			dbc.close();
			Out.prt("Complete writing " + n + " clusters -- " + cnt + " lines");
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error saving file");}
	 }
	/******************************************************************
	 * export both pairs of sequences from the pair table
	 */
	 public void exportPairSeqFa(Component btnTable, TableData theTableData, int pair, String column) {//pair=0 both, 1=first, 2=second
		try { 
			String file, msg;
			String msg2= column;
			if (pair==0)      {file = msg2 + "Pair";  msg="both " + msg2 + " pairs";}
			else if (pair==1) {file = msg2 + "1st";   msg="1st " + msg2 + " pairs";}
			else              {file = msg2 + "2nd";   msg="2nd " + msg2 + " pairs";}
			
			BufferedWriter out = fwObj.getBWriter(btnTable, msg, file, FileC.fFASTA, FileC.wAPPEND);
			if (out==null) return;
			
			int IDidx1 = theTableData.getColumnHeaderIndex(ID1_SQL);
			int IDidx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
			int pairID = theTableData.getColumnHeaderIndex(PAIR_ID);
			int n = theTableData.getNumRows();
			int [] seqID1 = new int [n];
			int [] seqID2 = new int [n];
			int [] pairid = new int [n];
	    		for (int i=0; i<n; i++) {
	    			seqID1[i] = (Integer)theTableData.getValueAt(i, IDidx1);
	    			seqID2[i] = (Integer)theTableData.getValueAt(i, IDidx2);
	    			pairid[i] = (Integer) theTableData.getValueAt(i, pairID);
	    		}
			
			DBConn dbc = theViewerFrame.getDBConnection();	
			ResultSet rs;
			String sql = "SELECT UTstr, " + column + " FROM unitrans";
		
			int cnt=0;
			for (int i=0; i<seqID1.length; i++) {
				if (pair==0) rs = dbc.executeQuery(sql + " WHERE UTid=" + seqID1[i] + " or UTid=" + seqID2[i]);
				else if (pair==1) rs = dbc.executeQuery(sql + " WHERE UTid=" + seqID1[i]);
				else rs = dbc.executeQuery(sql + " WHERE UTid=" + seqID2[i]);
				
				String cid = "     Pair #" + pairid[i];
				while (rs.next()) {
					out.write(">" + rs.getString(1) + cid + "\n" +rs.getString(2) + "\n");
					out.flush();
					cnt++;
				}
				if ((cnt%100)==0)
					System.err.print("   Wrote " + cnt + " pairs...\r");
				rs.close();
			}
			out.close();
			dbc.close();
			
			Out.prt("Complete " + msg + " -- " + cnt + " lines");
		} catch(Exception e) {ErrorReport.reportError(e, "Error saving file");}
	 }
	
	 /******************************************************************
	  * Export sequences from sequence table
	  */
	 public void exportSeqFa(Component btnTable, int [] seqID, String column, boolean isCDS) {
		try { 
			String fname = (isCDS) ? "cdsSeq" : column;
			BufferedWriter out = fwObj.getBWriter(btnTable, fname, fname, FileC.fFASTA, FileC.wAPPEND);
			if (out==null) return;
			
			DBConn dbc = theViewerFrame.getDBConnection();	
			int cnt=0;
			for (int i=0; i<seqID.length; i++) {
				ResultSet rs = dbc.executeQuery("SELECT UTstr, " + column +   ",ORF_start, ORF_end" +
						" FROM unitrans WHERE UTid=" + seqID[i]);
				while (rs.next()) {
					String name = rs.getString(1);
					String seq = rs.getString(2);
					if (isCDS) seq = seq.substring(rs.getInt(3)-1, rs.getInt(4));
					out.write(">" + name  + "\n" + seq + "\n");
					out.flush();
					cnt++;
				}
				if ((cnt%100)==0)
					System.err.print("   Wrote " + cnt + " sequences...\r");
				rs.close();
			}
			out.close();
			dbc.close();
			Out.prt("Complete writing " + column + " -- " + cnt + " lines");
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error saving file");}
	 }
	 /******************************************************************
	  * Export counts or TPM per cluster
	  * CAS305
	  */
	 public void exportGrpCounts(Component btnTable, TableData theTableData, String summary, boolean bdoCnt) {
		try { 
			String fname = (bdoCnt) ? "ClusSeqCounts" : "ClusSeqTPM";
			BufferedWriter out = fwObj.getBWriter(btnTable, fname, fname, FileC.fTSV, FileC.wAPPEND);;
			if (out==null) return;
			
			int nRows = theTableData.getNumRows();
			int idx1 = theTableData.getColumnHeaderIndex(GRP_SQL);
			int idx2 = theTableData.getColumnHeaderIndex(GRP_ID);
			String column = (bdoCnt) ? "expList" : "expListN";
			DBConn dbc = theViewerFrame.getDBConnection();	
			
			String list = dbc.executeString("select allSeqLib from info");
			String line = "GrpName\tSeqName";
			String [] tok = list.split(" ");
			for (String x : tok) line += "\t" + x;
			out.write(line + "\n");
			out.flush();
			
			int cnt=0;
			for (int i=0; i<nRows; i++) {
				int id = (Integer) theTableData.getValueAt(i, idx1);
				String grpName = (String) theTableData.getValueAt(i, idx2);
				ResultSet rs = dbc.executeQuery("SELECT unitrans.UTstr, " + column +  " from unitrans, pog_members "
						+ " where pog_members.UTid=unitrans.UTid and pog_members.PGid= " + id);
				while (rs.next()) {
					String name = rs.getString(1);
					line = grpName + "\t" + name;
					
					list = rs.getString(2);
					tok = list.split(" ");
					for (String x : tok) {
						String y = x.split("=")[1];
						line += "\t" + y;
					}
					out.write(line + "\n");
					out.flush();
					cnt++;
				}
				if ((cnt%100)==0)
					System.err.print("   Wrote " + cnt + " sequences...\r");
				rs.close();
			}
			out.close();
			dbc.close();
			Out.prt("Complete writing " + cnt + " lines");
			
		} catch(Exception e) {ErrorReport.reportError(e, "Error saving file");}
	 }
	 /*****************************************************
	 * XXX Export GOs - Export all cluster GOs 
	 * Get all sequences for the Cluster
	 */
	public void exportGrpGO(Component btnTable, TableData theTableData, String summary) {
	try {
		int n = theTableData.getNumRows();
		int idx1 = theTableData.getColumnHeaderIndex(GRP_SQL);
		Vector <Integer> grpID = new Vector <Integer> ();
		Vector <Integer> seqID = new Vector <Integer> ();
		
		DBConn dbc = theViewerFrame.getDBConnection();	
		ResultSet rs=null;
		
		for(int i=0; i<n; i++) {
			int id = (Integer) theTableData.getValueAt(i, idx1);
			rs = dbc.executeQuery("select UTid from pog_members where PGid=" + id);
			while (rs.next()) {
				grpID.add(i); 		// not cluster number, just group by cluster
				seqID.add(rs.getInt(1));
			}
		}
		if (rs!=null) rs.close(); dbc.close();
		exportGO(btnTable, grpID, seqID, n, "Cluster", summary, GrpPrefix); 
	}
	catch (Exception e) {ErrorReport.reportError(e, "Export GO for clusters");}
	}
	// Pairs table - Export GOs for all sequences in Pairs 
	public void exportPairGO(Component btnTable, TableData theTableData, String summary) {
	try {
		int n = theTableData.getNumRows();
		int idx1 = theTableData.getColumnHeaderIndex(ID1_SQL);
		int idx2 = theTableData.getColumnHeaderIndex(ID2_SQL);
		Vector <Integer> grpID = new Vector <Integer> ();
		Vector <Integer> seqID = new Vector <Integer> ();
		
		for(int i=0; i<n; i++) {
			int id1 = (Integer) theTableData.getValueAt(i, idx1);
			int id2 = (Integer) theTableData.getValueAt(i, idx2);
			seqID.add(id1); grpID.add(i);
			seqID.add(id2); grpID.add(i);
		}
		exportGO(btnTable, grpID, seqID, n, "Pairs", summary, PairPrefix);
	}
	catch (Exception e) {ErrorReport.reportError(e, "Export GO for pairs");}	
	}
	// "Export all GOs for sequences "
	public void exportSeqGO(Component btnTable, TableData theTableData, String summary) {
		try {
			int n = theTableData.getNumRows();
			int idx = theTableData.getColumnHeaderIndex(ID_SQL);
			Vector <Integer> grpID = new Vector <Integer> ();
			Vector <Integer> seqID = new Vector <Integer> ();
			for(int i=0; i<n; i++) {
				int id = (Integer) theTableData.getValueAt(i, idx);
				seqID.add(id);
				grpID.add(i);
			}
			exportGO(btnTable, grpID, seqID, n, "Seqs", summary, SeqPrefix);
		}
		catch (Exception e) {ErrorReport.reportError(e, "Export GO for sequences");}	
	}
	/***********************************************
	 * Called by above three
	 * grpid and seqid correspond, a grpID for each seqid
	 * Percentage is based on #Seqs, #Pairs, #Groups (numSet)
	 */
	private void exportGO(Component btnTable, Vector <Integer> grpList, Vector <Integer> seqList, 
			int numSet, String label, String summary, String filePrefix) {
		try {
			final ExportGO et = new ExportGO();
			et.setVisible(true);
			int saveMode = et.getSelection();
			if(saveMode == ExportGO.EXPORT_CANCEL)  return;
			
		// Get file name
			String fileName = et.getGOfile(filePrefix);
			BufferedWriter outBW = fwObj.getBWriter(btnTable, fileName, fileName, FileC.fTSV, FileC.wAPPEND);;
			if (outBW==null) return;
						
		// Get parameters
			String where="", sum="";
			String level = et.getGOLevel();
			if (!level.equals("0")) where = Static.combineBool(where, " i.level=" + level);
			
			String termType = et.getTermType();
			if (!termType.equals("")) where = Static.combineBool(where, " i.term_type = '" + termType + "'");
			
			String eval = et.getGOEval();
			if (!eval.equals("")) {
				where = Static.combineBool(where, " s.bestEval <= " + eval);
				sum =   Static.combineBool(sum, "E-value " + eval);
			}
			
			if (where!="") where += " and ";
			
			int cntLimit = et.getGOCnt();
			if (cntLimit>0) sum = Static.combineBool(sum, "Count limit " + cntLimit);
			
		// Get sequences GO direct and assigned
		// do not want to search to be on i.bestEval, because that may come from
		// a seq-hit not in the cluster.
			String sql = "select s.gonum from go_seq as s " +
					" join go_info as i on i.gonum=s.gonum " +
					" where  " + where + " s.UTid=";
		
			DBConn mdb = theViewerFrame.getDBConnection();	
			ResultSet rs=null;  
			
			int cnt=0, lastGrpID=0, cntGOSeqPairs=0, nSeq = seqList.size();
			TreeMap <Integer, Integer> goCntMap = new TreeMap <Integer, Integer> ();
			HashSet <Integer> goSetForSeq = new HashSet <Integer> (); 
			Out.PrtSpCntMsg(1, nSeq, " sequences to process");
			
			for (int i=0; i<nSeq; i++) {
				int sid = seqList.get(i);
				int gid = grpList.get(i);
				
				if (gid!=lastGrpID) {
					goSetForSeq.clear();
					lastGrpID=gid;
				}
				
				rs = mdb.executeQuery(sql + sid);
				while (rs.next()) {
					int gonum = rs.getInt(1);
					if (goSetForSeq.contains(gonum)) continue;
					
					goSetForSeq.add(gonum);
					if (goCntMap.containsKey(gonum)) 
						 goCntMap.put(gonum, goCntMap.get(gonum)+1);
					else goCntMap.put(gonum, 1);
					cntGOSeqPairs++;
				}
				
				cnt++;
				if (cnt==100) {
					Out.rp("  processed ", cnt, nSeq);
					cnt=0;
				}
			}
			Out.r("                                                      ");
			// Write GOs with info 
			Out.PrtSpCntMsg(1, goCntMap.size(), " GOs to process");
			
			cnt=0;
			if (sum=="") sum = "None";
			if (summary=="") summary="None";
			outBW.write("###  #" + label + ": " + numSet + " Seq-GO: " + cntGOSeqPairs +
					"   Parameters: " + sum + "  Filter: " + summary + "\n");
			
			// Have all information necessary for GO/count option. Sorted by GOnum
			if (et.isGOCnt()) {
				outBW.write("GOterm\t#" + label + "\n");
				Out.PrtSpMsg(1, "Columns: GOterm\t#" + label);
				for (int gonum : goCntMap.keySet()) {
					int cntGO = goCntMap.get(gonum);
					if (cntGO <= cntLimit) continue;
					
					String goStr = String.format(GO_FORMAT, gonum);
					String n = String.format("%5d", cntGO);
					
					outBW.write(goStr +  "\t" + n + "\n");
					
					cnt++;
					if (cnt==1000) {
						Out.r("  write " + cnt);
						cnt=0;
					}
				}
				outBW.close();
				if (rs!=null) rs.close(); mdb.close();
				Out.prt("Complete writing " + goCntMap.size() + " GOs");
				return;
			}
			
			// Get more information for Verbose and Desc/Cnt options
			int cntBio=0, cntCell=0, cntMol=0;
			Vector <GOdata> goList = new Vector <GOdata> ();
			for (int gonum : goCntMap.keySet()) {
				int cntGO = goCntMap.get(gonum);
				if (cntGO <= cntLimit) continue;
				
				rs = mdb.executeQuery("select level, term_type, descr from go_info " +
						" where gonum=" +gonum);
				if (!rs.next()) ErrorReport.die("Internal TCW error on creating go data list");
				
				GOdata gd = new GOdata();
				gd.goStr = String.format(GO_FORMAT, gonum);
				gd.cntStr = String.format("%5d", goCntMap.get(gonum));
				gd.lev = rs.getInt(1);
				gd.domain = rs.getString(2);
				gd.desc = rs.getString(3);
				goList.add(gd);
				
				if (gd.domain.startsWith("bio")) cntBio++;
				else if (gd.domain.startsWith("cell")) cntCell++;
				else if (gd.domain.startsWith("mol")) cntMol++;
				
				cnt++;
				if (cnt==1000) {
					Out.r("  GO info " + cnt);
					cnt=0;
				}
			}
			Out.PrtSpMsg(1, "Biol: " + cntBio + "   Cell: " + cntCell + "   Mol: " + cntMol);
			Collections.sort(goList);
			cnt=0;
			if (et.isAllColumns()) { 
				String col = "GOterm\t#" + label + "\tLevel\tDomain\tDescription";
				outBW.write(col+"\n");
				Out.Print("   Columns: " + col);
				
				for (GOdata gd : goList) {
					outBW.write(gd.goStr + "\t" + gd.cntStr  + "\t" + gd.lev + "\t" + gd.domain +   "\t" + gd.desc + "\n");
					cnt++;
					if (cnt==1000) {
						Out.r("  write " + cnt);
						cnt=0;
					}
				}
			}
			else {
				String col = "Domain\tDescription\t#" + label;
				outBW.write(col+"\n");
				Out.Print("   Columns: " + col);
				String savDomain="";
				for (GOdata gd : goList) {
					String d = (savDomain.equals(gd.domain)) ? "" : gd.domain;
					savDomain = gd.domain;
					outBW.write(d + "\t" + gd.desc  + "\t" + gd.cntStr + "\n");
					cnt++;
					if (cnt==1000) {
						Out.r("  write " + cnt);
						cnt=0;
					}
				}
			}
			outBW.close();
			if (rs!=null) rs.close(); mdb.close();
			Out.PrtSpMsg(0, "Complete writing " + goCntMap.size() + " GOs");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Exporting GOs ");}
	}
	
	private class ExportGO extends JDialog {
		private static final long serialVersionUID = 1L;
		public static final int EXPORT_CANCEL = 0;
		public static final int EXPORT_OK = 1;
		  
		 public ExportGO() {
			setModal(true);
     		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
     		setTitle("Export GOs.... ");
         
     		JPanel selectPanel = Static.createPagePanel();
     		
     		JLabel label = Static.createLabel("GOs from table (" + Globalx.CSV_SUFFIX + ")");
     		selectPanel.add(label);
	        selectPanel.add(Box.createVerticalStrut(5));
	        
	        JPanel goPanel = Static.createPagePanel();
	        JPanel row = Static.createRowPanel();
	        row.add(Box.createHorizontalStrut(15));
	        
	        JLabel goLabel = new JLabel("GO Level: ");
 			txtGOlevel  = new JTextField(3);
 			txtGOlevel.setMaximumSize(txtGOlevel.getPreferredSize());
 			txtGOlevel.setText("2");
 			
 			cmbTermTypes = new ButtonComboBox();
    		cmbTermTypes.addItem("Any domain");
    		for(int x=0; x<Globalx.GO_TERM_LIST.length; x++)
    				cmbTermTypes.addItem(Globalx.GO_TERM_LIST[x]);
    		
    		cmbTermTypes.setSelectedIndex(0);
    		cmbTermTypes.setMaximumSize(cmbTermTypes.getPreferredSize());
    		cmbTermTypes.setMinimumSize(cmbTermTypes.getPreferredSize());
    		cmbTermTypes.setBackground(Globalx.BGCOLOR);
    		
    		row.add(goLabel);  row.add(Box.createHorizontalStrut(2));
	        row.add(txtGOlevel);
	        row.add(Box.createHorizontalStrut(5));
	         
	        row.add(cmbTermTypes);
	        goPanel.add(row);
	        goPanel.add(Box.createVerticalStrut(5));
		         
	        row = Static.createRowPanel();
	        row.add(Box.createHorizontalStrut(15));
	         
 			JLabel goEvalLabel = Static.createLabel("E-value");
 			txtGOEval  = Static.createTextField("", 4);       
	         row.add(goEvalLabel); row.add(Box.createHorizontalStrut(2));
	         row.add(txtGOEval);   row.add(Box.createHorizontalStrut(5));
	        
	         JLabel goCntLabel = Static.createLabel("Count limit");
	         txtGOCnt = Static.createTextField("", 4);
	         row.add(goCntLabel); row.add(Box.createHorizontalStrut(2));
	         row.add(txtGOCnt);   row.add(Box.createHorizontalStrut(5));
	         
	         goPanel.add(row);
	         goPanel.add(Box.createVerticalStrut(5));
	         
	         row = Static.createRowPanel();
	         row.add(Box.createHorizontalStrut(15));
	 		 row.add(new JLabel("Columns:")); 	row.add(Box.createHorizontalStrut(2));
	 		 btnGOCnt = Static.createRadioButton("GO/Count", true);
	 		 btnDescCnt = Static.createRadioButton("Desc/Count", true);
	         btnVerbose =  Static.createRadioButton("Verbose", false);  
	         ButtonGroup colgrp = new ButtonGroup();
	         colgrp.add(btnDescCnt);colgrp.add(btnGOCnt); colgrp.add(btnVerbose);
	         btnVerbose.setSelected(true);
	         row.add(btnVerbose); 
	         row.add(Box.createHorizontalStrut(2));
	         row.add(btnDescCnt);
	         row.add(Box.createHorizontalStrut(2));
	         row.add(btnGOCnt);
	         goPanel.add(row);
	         
	        selectPanel.add(goPanel);
	        selectPanel.add(new JSeparator());
	        
         // bottom buttons
     		btnOK = new JButton("OK");
     		btnOK.addActionListener(new ActionListener() {
 				public void actionPerformed(ActionEvent e) {
 					nMode=EXPORT_OK;
 					setVisible(false);
 				}
 			});
     		btnCancel = new JButton("Cancel");
     		btnCancel.addActionListener(new ActionListener() {
 				public void actionPerformed(ActionEvent e) {
 					nMode=EXPORT_CANCEL;
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
		 
		public int getSelection() {
	    	return nMode; 
	    }
	 	public String getGOLevel() { 
	    	String x = txtGOlevel.getText().trim();
	    	if (x.equals("-") || x.equals("") || x.equals("0")) return "0";
	    		
			try {Integer.parseInt(x);}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, 
					"Incorrect level " + x + "\nUsing default 2", "Error", JOptionPane.PLAIN_MESSAGE);
				x="2";
				txtGOlevel.setText(x);
			}
	    	return x; 
	    }
	    public String getGOEval() { 
	    	String x = txtGOEval.getText().trim();
			if (x.equals("") || x.equals("-")) return "";
			
			try {Double.parseDouble(x);}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, 
					"Incorrect E-value " + x + "\nLeaving E-value blank", "Error", JOptionPane.PLAIN_MESSAGE);
				x="";
				txtGOEval.setText(x);
			}
	    	return x; 
	    }
	    
	    public int getGOCnt() { 
	    	int x = 0;
			String y = txtGOCnt.getText().trim();
			if (y.equals("")) return 0;
			
			try {x = Integer.parseInt(y);}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null, 
					"Incorrect Count limit " + y + "\nSet limit to 0", "Error", JOptionPane.PLAIN_MESSAGE);
				txtGOEval.setText("0");
			}
	    	return x; 
	    }
		public String getTermType() {
			if(cmbTermTypes.getSelectedIndex() > 0) return  cmbTermTypes.getSelectedItem();
			else return "";
		}
		
		public boolean isAllColumns() {return btnVerbose.isSelected();}
		public boolean isGOCnt() {return btnGOCnt.isSelected();}
		
		public String getGOfile(String filePrefix) {
			String file=filePrefix+"GO";
    		
    		String level = getGOLevel();
    		if (!level.equals("-")) file += "_" + level;
    		if (cmbTermTypes.getSelectedIndex() > 0) {
    			int idx = cmbTermTypes.getSelectedIndex();
    			file += "_" + Globalx.GO_TERM_ABBR[idx-1];
    		}
    		String eval = getGOEval();
    		if (!eval.equals("")) {
    			file += "_" + eval;
    		}
    		
    		if (isAllColumns()) file += "_vb";
    		else if (isGOCnt()) file += "_cnt";
    		else file += "_desc";
    		
    		return file;
		}
		private ButtonComboBox cmbTermTypes = null;
	    private JTextField txtGOlevel = null, txtGOEval = null, txtGOCnt = null;
	    private JRadioButton btnGOCnt = null, btnDescCnt = null, btnVerbose = null;
	   
	    private JButton btnOK = null, btnCancel = null;    	
	    private int nMode = -1;
	 }
	 private class GOdata implements Comparable <GOdata>{
		 String domain="", desc="", goStr="", cntStr="";
		 int lev=0;
		 public int compareTo(GOdata b) {
			 if (!domain.equals(b.domain)) return domain.compareTo(b.domain);
			 if (lev!=b.lev) return lev-b.lev;
			 return goStr.compareTo(b.goStr);  // CAS305 was domain
		 }
	 }
	/*********************************************************************
	 * For copy table for all tables
	 */
	public String createTableString(SortTable theTable) {
		StringBuilder retVal = new StringBuilder();
		int nCol = theTable.getColumnCount();
		
		int rowNumColIndex = -1;
		for(int x=0; x<nCol-1; x++) {
			String val = theTable.getColumnName(x);
			if (val.equals(FieldData.ROWNUM)) rowNumColIndex = x;
	
			retVal.append(theTable.getColumnName(x));
			retVal.append(delim);
		}
		retVal.append(theTable.getColumnName(nCol-1));
		retVal.append("\n");
		
		for(int x=0; x<theTable.getRowCount(); x++) {
			for(int y=0; y<nCol; y++) {
				if (y == rowNumColIndex) {
					retVal.append("" + (x+1));
				}
				else { 
					Object obj = theTable.getValueAt(x, y);
	                String val = (obj == null) ? "-" : obj.toString(); // CAS305 changes all 3 to '-' convert(obj.toString()); 
					retVal.append(val);
				}
				if (y != nCol-1) retVal.append(delim);
			}
			retVal.append("\n");
			
			if(x % 5 == 0)
				theViewerFrame.setStatus("Copying table: " + ((int)((((double)x)/theTable.getRowCount()) * 100)) + "%");
		}
		theViewerFrame.setStatus("");

		return retVal.toString();
	}
	 /*********************************************************************/
	
	 public void runBlast(String name, String seq, boolean tabular) {
 		try {
 			boolean runFormat = false;
				
			File baseDir = new File(BLASTDIR);
			
			if(!baseDir.exists())
				baseDir.mkdir();
			
			File combinedFasta = new File(baseDir.getAbsolutePath() + "/Combined_" + theViewerFrame.getDBName() + ".fasta");
			if(!combinedFasta.exists()) {
				if(!exportForBlast(combinedFasta))
					return;
			}					
			File phrCombined = (new File(combinedFasta.getAbsolutePath() + ".phr"));
			File pinCombined = (new File(combinedFasta.getAbsolutePath() + ".pin"));
			File psqCombined = (new File(combinedFasta.getAbsolutePath() + ".psq"));
				
			runFormat = true;
			if(phrCombined.exists() && pinCombined.exists() && psqCombined.exists()) 
			{
				if((combinedFasta.lastModified() < phrCombined.lastModified()) &&
						(combinedFasta.lastModified() < pinCombined.lastModified()) &&
							(combinedFasta.lastModified() < psqCombined.lastModified())) 
					runFormat = false;
			}

			if(runFormat) {
				String cmd = BlastArgs.getFormatp(combinedFasta.getAbsolutePath());
				System.err.println("Executing: " + cmd);
				Process pFormatDB = Runtime.getRuntime().exec(cmd);
				pFormatDB.waitFor();
			}

			Calendar timeStamp = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat("yy_MM_dd_kk_mm_ss");
			String strTimeStamp = formatter.format(timeStamp.getTime());

			File sequenceFasta = new File(baseDir.getAbsolutePath() + "/" + name + "_" + strTimeStamp + ".fasta");
			BufferedWriter out = new BufferedWriter(new FileWriter(sequenceFasta));
			out.write(">" + name + "\n");
			out.write(seq + "\n");
			out.flush();
			out.close();
			
			//Set output file name to the current date/time
			File outFile = new File(baseDir.getAbsolutePath() + "/" + "Out_" + strTimeStamp + ".txt");

			String blastCmd = BlastArgs.getBlastpCmdMTCW(sequenceFasta.getAbsolutePath(), combinedFasta.getAbsolutePath(),
					outFile.getAbsolutePath(), tabular);
			
			Process pBlastall = Runtime.getRuntime().exec(blastCmd);
			pBlastall.waitFor();
			
			Vector<String> lines = new Vector<String> ();
			String line;
			
			if(tabular) {
				line = "Query Subject %Sim Align MisMatch GapOpen qStart qEnd sStart sEnd E-value Bit";
				lines.add(line);
			}
			BufferedReader br = new BufferedReader(new FileReader(outFile));
			while((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close(); 	
			if(tabular) 
				displayInfoTable(theViewerFrame, "Blast result for " + name, 
						lines.toArray(new String[0]));
			else
				UserPrompt.displayInfoMonoSpace(theViewerFrame, "Blast result for " + name, 
						lines.toArray(new String[0])); 
			
			sequenceFasta.delete();
			outFile.delete();
 		}
 		catch (Exception e) {ErrorReport.prtReport(e, "Performing blast");}
	 }
	 private boolean exportForBlast(File target) {
		try {
			DBConn conn = theViewerFrame.getDBConnection();
			int cntDS = conn.executeCount("SELECT COUNT(*) FROM assembly");
			String [] asmPfx = new String [cntDS+1]; 
			ResultSet rs = conn.executeQuery("SELECT ASMid, prefix FROM assembly");		
			while(rs.next()) 
				asmPfx[rs.getInt(1)] = rs.getString(2);
					
	        BufferedWriter pw = new BufferedWriter(new FileWriter(target));
	        // we are allowing duplicate sequence identifiers, so need to search by ASMid
	        for (int i=1; i<= cntDS;  i++) {
	        	rs = conn.executeQuery("SELECT UTstr, aaSeq FROM unitrans" +
	        			" WHERE ASMid=" + i);
				while(rs.next()) {
					String id = rs.getString(1);
					String seq = rs.getString(2);
					if (seq != null) {
						pw.write(">" + id + "\n");
						pw.write(seq + "\n");
					}
				}
	        }
			rs.close();
			conn.close();	
			pw.flush();
			pw.close();
			return true;
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error generating fasta from database");
			return false;
		}
	}
	
	/*******************************************************/
	// used by runBlast
	public static void displayInfoTable(JFrame parentFrame, String title, final String [] message) {
		final class TheModel extends AbstractTableModel {
			private static final long serialVersionUID = 2153498168030234218L;

			public int getColumnCount() {
				if(message == null || message.length == 0)
					return 1;
				return message[0].split("\\s").length; 
			}
			public int getRowCount() { return message.length; }
			public Object getValueAt(int row, int col) {
				if(row < message.length && col < message[row].split("\\s+").length)
					return message[row].split("\\s+")[col];
				return "";
			}
		}

		JOptionPane pane = new JOptionPane();
		final JButton btnCopySeqID = new JButton("Copy Seq ID");
	
		final JTable messageTable = new JTable();
		messageTable.setModel(new TheModel());
		messageTable.setFont(new Font("monospaced", Font.BOLD, 12));
		messageTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				btnCopySeqID.setEnabled(messageTable.getSelectedRow() >= 0);
			}
		});

		btnCopySeqID.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnCopySeqID.setEnabled(false);
		btnCopySeqID.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int row = messageTable.getSelectedRow();
				
				String rowVal = (String)messageTable.getValueAt(row, 1);
				int strPos = 0;
				if((strPos = rowVal.indexOf(' ')) >= 0)
					rowVal = rowVal.substring(strPos+1);
				
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				cb.setContents(new StringSelection(rowVal), null);
			}
		});
		
		JScrollPane sPane = new JScrollPane(messageTable); 
		messageTable.setTableHeader(null);
		messageTable.setShowGrid(false);
		messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		messageTable.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		sPane.getViewport().setBackground(Color.WHITE);
		
		//Adjust column sizes
        TableColumn column;
        Component comp;
        int cellWidth;
        
        for (int i = 0;  i < messageTable.getColumnCount();  i++) { // for each column
            column = messageTable.getColumnModel().getColumn(i);
            
            cellWidth = 0;
            for (int j = 0;  j < messageTable.getModel().getRowCount();  j++) { // for each row
	            comp = messageTable.getDefaultRenderer(messageTable.getColumnClass(i)).
	                             getTableCellRendererComponent(
	                            	 messageTable, messageTable.getValueAt(j, i),
	                                 false, false, j, i);

	            if(comp != null) {
		            cellWidth = Math.max(cellWidth, comp.getMinimumSize().width);
	            }
            }

            column.setPreferredWidth(cellWidth);
        }
        
		JPanel buttonRow = new JPanel();
		buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
		
		buttonRow.add(btnCopySeqID);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		
		mainPanel.add(buttonRow);
		mainPanel.add(Box.createVerticalStrut(15));
		mainPanel.add(sPane);
		
		pane.setMessage(mainPanel);
		pane.setMessageType(JOptionPane.PLAIN_MESSAGE);

		JDialog helpDiag = pane.createDialog(parentFrame, title);
		helpDiag.setModal(false); 
		helpDiag.setResizable(true);
		helpDiag.setSize(new Dimension(620, 400));
		
		helpDiag.setVisible(true);		
	}
	
	private MTCWFrame theViewerFrame = null;
	private FileWrite fwObj = new FileWrite(FileC.bNoVer, FileC.bNoPrt);
}
