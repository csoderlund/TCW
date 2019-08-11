package sng.util;

/*******************************************************
 * This calls MainTableSort with all the necessary info for the table
 * MainTableSort has all table methods.
 * All Copy/Export routines are in this file.
 */
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import sng.dataholders.SequenceData;
import sng.viewer.STCWFrame;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
import util.methods.Stats;
import util.ui.DisplayFloat;
import util.ui.DisplayInt;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class MainTable extends MainTableSort {
	private static final long serialVersionUID = 6393222947327727667L;
	private static final String GO_FORMAT = "GO:%07d";
	private boolean isApplet=false;
	
	public MainTable (FieldMapper inFields, 
						Vector <String> tableRows, 
						int[] inDefaultFields,
						final ActionListener refreshListener)
	throws Exception
	{
		super(inFields, tableRows, inDefaultFields, refreshListener);
        
		isApplet = UIHelpers.isApplet();
		if (nTotalRows == 0) return;	

        autofitColumns();  
	}
    
    /************************************************************
     * Called from Main table to copy and export
     */
	// Copied from BasicTablePanel
	public String statsPopUpCompute(String sum) {
		try {
			String [] fields = 
				{"Column", "Average", "StdDev", "Median", "Range", "Low", "High", "Sum", "Count"};
			int [] justify =   {1,   0,  0, 0, 0, 0, 0, 0, 0};
			int intCol=3;
			
			int tableRows=getRowCount(), tableCols = getColumnCount();
			int nStat=  fields.length, nRow=0;
			
		    String [][] rows = new String[tableCols][nStat]; 
			double [] dArr = new double [tableRows];
			
		    for(int y=0; y < tableCols; y++) {
		    		String colName = getColumnName(y);
		    		if (colName.equals("Row #")) continue;
		    		
			 	Object obj = getValueAt(0,y);
			 	if (obj==null) continue;      // if the first value is null, doesn't work
			 	
				Class <?> dtype = obj.getClass();
				if (dtype==String.class) continue;
				
			 	for (int i=0; i<tableRows; i++) dArr[i]=Globalx.dNoVal;
			 	boolean isInt=false;
			 	
				for (int x=0, c=0; x<tableRows; x++) { 
					if (getValueAt(x,y) == null) continue;
					
					obj = getValueAt(x,y);
					if (obj instanceof DisplayInt) {
						int j = ((DisplayInt)obj).intValue();
						dArr[c++] = (double) j;
						isInt=true;
					}
					else if (obj instanceof Integer) {
						int j = ((Integer)   obj).intValue();
						dArr[c++] = (double) j;
						isInt=true;
					}
					else if (obj instanceof Long) {
						long j = (Long) getValueAt(x, y);
						dArr[c++] = (double) j;
						isInt=true;
					}
					else if (obj instanceof DisplayFloat) {
						dArr[c++] = ((DisplayFloat) obj).getValue();
					}
					else if (obj instanceof Float) {
						float f = ((Float)obj).floatValue();
						dArr[c++] = (double) f;
					}
					else if (obj instanceof Double) {
						dArr[c++] = ((Double)obj).doubleValue();
					}
					else { 
						Out.prt("MainTable: class? " + (String) getValueAt(x, y) + " " + dtype);
					}
				}
				// if there is a N-fold value <=-100000, this be wrong
			 	boolean noDef = (colName.equals("Rstat") || colName.contains("/"))
	    				? true : false;
			 	
				double [] results = Stats.averages(colName, dArr, noDef);
				
				rows[nRow][0] = colName;
				for (int i=0, c=1; i<results.length; i++, c++) {
					if ((i>=intCol && isInt) || (i==results.length-1)) {
						if (results[i] < Globalx.dNoScore) rows[nRow][c] = "N/A"; // overflow
						else rows[nRow][c] = String.format("%,d", (long) results[i]);
					}
					else rows[nRow][c] = Out.formatDouble(results[i]);
				}
				nRow++;
			}
			String statStr = Out.makeTable(nStat, nRow, fields, justify, rows);
			statStr += "\n" + tableRows + " rows; " + sum;
			return statStr;
		
		} catch(Exception e) {ErrorReport.reportError(e, "Error create column stats"); return "Error"; }
	}
	// Copy to clipboard
    public String copyTableToString() {
		StringBuilder target = new StringBuilder();
		try {
        // skip row# column
        for(int x=1; x < getColumnCount()-1; x++) {
            //Remove spaces from column headers
            target.append(((String)getColumnModel().getColumn(x).getHeaderValue()).replaceAll("\\s", ""));
            target.append("\t");
        }
        target.append(((String)getColumnModel().getColumn(getColumnCount()-1).getHeaderValue()).replaceAll("\\s", ""));
        target.append("\n");
        
        // skip last row
        for (int row = 0;  row < getRowCount()-1;  row++) {
            for (int col = 1;  col < getColumnCount();  col++) {
                Object obj = getValueAt(row, col);
                String s = (obj == null ? "" : obj.toString()); 
                // surrounds ',' with quotes
                if(s.indexOf(',') >= 0)
                    s = "\"" + s + "\"";
                
                target.append(s);
                if(col + 1 < getColumnCount())
                    target.append('\t');
            }
            target.append("\n");
        }
    }
    catch (Exception err) {ErrorReport.reportError(err, "Internal error copying table");}
    return target.toString();
    }
    public boolean saveToFileTabDelim(String fileName, STCWFrame frame) {
        String delim = Globalx.CSV_DELIM;
        try {
            PrintWriter pw = getWriter("Columns", fileName, frame);
            if (pw==null) return false;
           
            int rowCnt = getRowCount();
            int nCol = getColumnCount();
            Out.prt(1, "Processing " + rowCnt + " rows and " + nCol + " columns...");
            
            String val = "";
            // skip row# column
            for(int x=1; x < nCol; x++) {
                //Replace spaces from column headers
                val = ((String)getColumnModel().getColumn(x).getHeaderValue()).replaceAll("\\s", "-");
                pw.print(val);
                pw.print(delim);
            }
            pw.print("\n");
            
            for (int row = 0;  row < rowCnt;  row++) {
                if(!isApplet && row % 100 == 0) Out.r("Wrote " + row);
                for (int col = 1;  col < nCol;  col++) {
                    Object obj = getValueAt(row, col);
                    String s = (obj == null ? "" : obj.toString()); 
                    // surrounds ',' with quotes
                    if(s.indexOf(',') >= 0) s = "\"" + s + "\"";
                    
                    pw.print(s);
                    if(col + 1 < nCol) pw.print(delim);
                }
                pw.println();
            }
            Out.prt(1, "Complete writing " + rowCnt + " records ");
            pw.close();
            return true;
        }
        catch (Exception e) {ErrorReport.reportError(e, "TCW error: writing file");}
        return false;
    }
    /***************************************************
     * save sequences to fasta file
     * get seqID (contigid) from interface table and read sequence from database
     */
    public boolean saveToFasta(String fileName, STCWFrame frame) {
        try {
        	 	PrintWriter pw = getWriter("Sequences", fileName,frame);
             if (pw==null) return false;
             
             int rowCnt = getRowCount(); 
             Out.prt(1, "Processing " + rowCnt + " sequences....");
             
            // read first so they can changed table
            int seqIdx = getColumnModel().getColumnIndex("Seq ID");
            String [] seqIDs = new String [getRowCount()];
           
            for (int row = 0;  row < rowCnt;  row++) {
            		seqIDs[row ]= (String) getValueAt(row, seqIdx);
            }
            
            DBConn mdb = frame.getNewDBC();
            ResultSet rs=null;
            String query = "SELECT consensus FROM contig WHERE contigid = '";
         
            for (int row = 0;  row < rowCnt;  row++) {
                if(!isApplet && row % 100 == 0) Out.r("Wrote " + row);
                
                String ctg= seqIDs[row]; 
                rs = mdb.executeQuery(query + ctg + "'");
                if (rs.next()) {
                		String sequence = rs.getString(1);
                		pw.println(">" + ctg);
                		pw.println(sequence);
                }
                else System.err.println(row + ". could not get sequence for '" + ctg + "'");
            }
            if (rs!=null) rs.close();
            pw.close(); mdb.close();
            Out.prt(1, "Complete writing " + rowCnt + " sequences ");
            return true;
        }
        catch (Exception err) {ErrorReport.reportError(err, "Internal error: exporting  table to " + fileName);}    
        return false;
    }
    /***************************************************
     * save sequences to fasta file
     * get seqID (contigid) from interface table and read sequence from database
     */
    public boolean saveHitsToFasta(String fileName, STCWFrame frame, String filter) {
        try {
        	 	PrintWriter pw = getWriter("Hits", fileName,frame);
             if (pw==null) return false;
             
             int rowCnt = getRowCount(); 
             Out.prt(1, "Processing " + rowCnt + " sequences....");
             
            int seqIdx = getColumnModel().getColumnIndex("Seq ID");
            String [] seqIDs = new String [getRowCount()];
           
            for (int row = 0;  row < rowCnt;  row++) {
            		seqIDs[row ]= (String) getValueAt(row, seqIdx);
            }
            TreeMap <String, Integer> hitsPrt = new TreeMap <String, Integer> (); 
            
            DBConn mdb = frame.getNewDBC();
            ResultSet rs=null;
            String query = "SELECT DUHID, uniprot_id FROM pja_db_unitrans_hits WHERE contigid = '";
         
            for (int row = 0;  row < rowCnt;  row++) {
                if(!isApplet && row % 100 == 0) Out.r("Found " + row);
                
                String ctg= seqIDs[row]; 
                rs = mdb.executeQuery(query + ctg + "' and " + filter);
             
                while (rs.next()) {
                		int id = rs.getInt(1);
                		String name = rs.getString(2);
                		if (!hitsPrt.containsKey(name))
                			hitsPrt.put(name, id);
                }
            }
            Out.prt(1, "Writing " + hitsPrt.size() + " hits....              ");
            
            //>sp|Q9V2L2|1A1D_PYRAB Putative 1-ami OS=Pyrococcus abyssi GN=PYRAB00630 PE=3 SV=1v
            //>pr|2AAA_PEA Protein phosphatase PP2A regulatory subunit A OS=Pisum sativum 
            query = "SELECT hitID,description,species, dbtype, sequence, repid FROM pja_db_unique_hits WHERE DUHID = ";
            int cnt=0, nonUP=0;
            for (String hit : hitsPrt.keySet()) {
            		int hitid = hitsPrt.get(hit);
            		cnt++;
                if(!isApplet && cnt % 100 == 0) Out.r("Wrote " + cnt);
                
                rs = mdb.executeQuery(query + hitid);
                if (rs.next()) {
                		String mid;
                		String type = rs.getString(4);
                		
                		if (type.equals("sp") || type.equals("tr")) mid = "|" + rs.getString(6);
                		else {
                			nonUP++;
                			mid = "|n" + nonUP;
                		}
                		
                		String x = 	type +  mid + "|" + 	rs.getString(1) +
            				" " 	+ 	rs.getString(2) + " OS=" 	+ 	rs.getString(3);
               
                		pw.println(">" +  x);
                		pw.println(rs.getString(5));
                }
                else System.err.println(cnt + ". could not get sequence for '" + hitid + "'");
            }
            if (rs!=null) rs.close();
            pw.close(); mdb.close();
            Out.PrtSpMsgCntZero(1, "Non UniProt hits", nonUP);
            Out.prt(0, "Complete writing hit sequences                 ");
            return true;
        }
        catch (Exception err) {ErrorReport.reportError(err, "Internal error: exporting  table to " + fileName);}    
        return false;
    }
    /*******************************************************
     * save ORF to file
     * this wasn't working on MAC, getting null values
     */
    public boolean saveORFToFasta(String fileName, STCWFrame frame) {
        
        String query="";
        try {
        		PrintWriter pw = getWriter("ORFs", fileName, frame);
        		if (pw==null) return false;
        		
        		int rowCount = getRowCount();
        		Out.prt(1, "Processing " + rowCount + " ORFs....");
        		
	        int seqIdx = getColumnModel().getColumnIndex("Seq ID");
	    		String [] seqIDs = new String [getRowCount()];
	       
	        for (int row = 0;  row < rowCount;  row++) {
	        		seqIDs[row ]= (String) getValueAt(row, seqIdx);
	        }
	       
	        DBConn mdb = frame.getNewDBC();
            int ignore=0, prt=0;
            query = "SELECT o_frame, o_coding_start, o_coding_end, consensus " +
            		           " FROM contig WHERE contigid = '";
           
            for (int row = 0;  row < rowCount;  row++) {
                if(!isApplet && row % 100 == 0) Out.r("Processed " + row);
                
                String ctg = seqIDs[row]; 
                ResultSet rset = mdb.executeQuery(query + ctg + "'");
                if(rset.next()) {
                    int fr = rset.getInt(1);
                    if (fr == 0) {
                    		ignore++;
                    		continue; // no ORF
                    }
                    
                    int start = rset.getInt(2);
                    int end = rset.getInt(3);
     
                    String strSeq = rset.getString(4);
                    String orf = SequenceData.getORFtrans(ctg, strSeq, fr, start, end);
                	
                    pw.println(">" + ctg + " AAlen=" + orf.length() + " frame=" + fr);
                    pw.println(orf);
                    prt++;
                }
                rset.close();
            }
            pw.close(); mdb.close();
            Out.prt(1, "Complete writing " + prt + " records   Ignored " + ignore + " due to frame=0");
            return true;
        }
        catch (Exception err) {ErrorReport.reportError(err, "Internal error: exporting  table\nQuery: " + query);}     
        return false;
    }
    /******************************************************
     * write file of counts with replicates
     */
    public boolean saveToFileCounts(String fileName, STCWFrame frame, String [] libraries) {
    		String delim = Globalx.CSV_DELIM;
        	
        	try {
        		PrintWriter expPW = getWriter("Counts", fileName, frame);
        		if (expPW==null) return false;
        		
        		int rowCnt = getRowCount();
        		Out.prt(1, "Processing " + rowCnt + " sequences....");
        		
        		DBConn mdb = frame.getNewDBC();
        		ResultSet rs;
        		
        		int seqIdx = getColumnModel().getColumnIndex("Seq ID");
        		String [] seqIDs = new String [getRowCount()];
            
            for (int row = 0;  row < rowCnt;  row++) {
            		seqIDs[row ]= (String) getValueAt(row, seqIdx);
            }
           
        		Vector <Integer> libList = new Vector <Integer> ();
        		Vector <Integer> repList = new Vector <Integer> ();
        		StringBuffer sb = new StringBuffer();
        		
        		// Write column names.
        		// The original are not written. Instead, the number of reps is determined
        		// and the libName followed by rep# is used.
       		
       		sb.append("SeqID");
       		rs = mdb.executeQuery("select LID, libid, reps from library where ctglib=0");
       		while (rs.next()) {
       			int lib = rs.getInt(1);
       			String libid = rs.getString(2);
       			for (int i=0; i<libraries.length; i++) {
       				if (libraries[i].equals(libid)) {
       					String rl = rs.getString(3).trim(); 
       					if (rl.equals("")) continue;
       					
       					String [] reps = rl.split(",");
       					for (int r = 1; r<= reps.length; r++) {
       						libList.add(lib); 
       						repList.add(r);
       						sb.append(delim + libid + r);
       					}
       					break;
       				}
       			}
       		}
       		expPW.println(sb.toString());
       		Out.prt(1, "Libraries: " + libraries.length + " replicates " + repList.size());
       		if (repList.size()==0) {
       			Out.prt(1, "No replicates to output - exiting");
       			return false;
       		}
       		// Get displayed contigs
       		HashMap <Integer, String> ctgid = new HashMap <Integer, String> ();
        		for (int row = 0;  row < getRowCount();  row++) {
        			String ctg = seqIDs[row];
        			rs = mdb.executeQuery("Select CTGID from contig where contigid='" + ctg + "'");
        			rs.next();
        			ctgid.put(rs.getInt(1), ctg);
        		}
        		
        		int cnt=0;
        		for (int cid : ctgid.keySet()) {
        			sb = new StringBuffer();
        			sb.append(ctgid.get(cid));
        			
        			for (int r=0; r<repList.size(); r++) {
        				String sql = "select count from clone_exp " +
        						" where CID=" + cid + " and LID=" + libList.get(r) +
        						" and rep=" + repList.get(r);
        				rs = mdb.executeQuery(sql);
        				if (rs.next()) // not all sequences have expression levels, so will not be in clone_exp
        					sb.append(delim + rs.getInt(1));
        			}
        			expPW.println(sb.toString());
        			cnt++;
        			if (!isApplet && cnt%100 == 0) Out.r("Wrote " + cnt);
        		}
        		expPW.close();
        		rs.close(); mdb.close();
        		Out.prt(1, "Complete writing " + cnt + " lines to file");
        		return true;
        	}
        	catch (Exception err) {ErrorReport.reportError(err,"TCW error: exporting count table" );}   	
        	return false;
    }
    /*****************************************************
     * Save direct GOs from Best GO hit for sequences in table.
     */
    public boolean saveGOFromBest(String fileName, STCWFrame frame) {
    	 	String query="";
         try {
         	PrintWriter pw = getWriter("SeqGOs", fileName, frame);
         	if (pw==null) return false;
         		
         	int rowCount = getRowCount();
         	Out.prt(1, "Processing " + rowCount + " sequences....");
         		
         	int seqIdx = getColumnModel().getColumnIndex("Seq ID");
 	    		String [] seqIDs = new String [getRowCount()];
 	       
 	    		for (int row = 0;  row < rowCount;  row++) {
 	    			seqIDs[row ]= (String) getValueAt(row, seqIdx);
 	    		}
 	    		DBConn mdb = frame.getNewDBC();
 	    		
 	    		HashMap <String, String> goMap = new HashMap <String, String> ();
 	    		ResultSet rs = mdb.executeQuery("select gonum, term_type, descr from go_info");
 	    		while(rs.next()) {
 	    			String gonum = String.format(Globalx.GO_FORMAT, rs.getInt(1));
 	    			String type =  rs.getString(2).substring(0,4);
 	    			String descr = rs.getString(3);
 	    			goMap.put(gonum, type + "  " + descr);
 	    		}
 	    			// XXX
 	    		query = "SELECT h.hitID, h.dbtype, h.taxonomy,  h.description, h.goList from pja_db_unique_hits as h " +
 	    				"join pja_db_unitrans_hits as p on p.DUHID = h.DUHID " +
 	    				"join contig as c on c.PIDgo = p.PID " +
 	    				"WHERE c.contigid = '";
 	    			 
 	    		 int cnt=0, cntGO=0, cntNoGO=0;
             for (int row = 0;  row < rowCount;  row++) {
                 if(!isApplet && row % 100 == 0) Out.r("Processed " + row);
                 
                 String seqID = seqIDs[row]; 
                 ResultSet rset = mdb.executeQuery(query + seqID + "'");
                 if (!rset.next()) {
                	 	cntNoGO++;
                	 	continue;
                 }
                 String hitID = rset.getString(1);
                 String taxo = rset.getString(3);
                 String dbTaxo = rset.getString(2) + 
                		 	       taxo.substring(0,1).toUpperCase() + taxo.substring(1,3);
                 String desc = rset.getString(4);
                 String goList = rset.getString(5);
                 pw.format("%-15s %-18s %-6s %s\n", seqID, hitID, dbTaxo, desc);
                 
                 // break apart gos to make tab-delimited line
                 // e.g. GO:0016020:IEA;GO:0015031:IEA
                 String [] tok = goList.split(";");
                 for (String g : tok) {
                	 	int idx = g.lastIndexOf(":");
                	 	String go = g.substring(0, idx);
                	 	String ec = g.substring(idx+1);
                	 	String goDesc = goMap.get(go);
                	 	pw.format("   %s  %s   %s\n", go, ec, goDesc);
                	 	cntGO++;
                 }
                 cnt++;
                 rset.close();
             }
             pw.close(); mdb.close();
             Out.PrtSpCntMsg(1, cntNoGO, "Sequences with no GOs");
             Out.prt(0, "Complete writing " + cnt + " sequences and " + cntGO + " GOs");
             return true;
         }
         catch (Exception err) {ErrorReport.reportError(err, "Internal error: exporting GO for table\nQuery: " + query);}     
         return false;
    }
    /***********************************
    * Save the GOs from the table, for the given level. 
    * For each one, save the # of contigs and the DE columns.
    * 1. Get all ctgID/contigIDs that have a GO (could just save those from table instead of all)
    * 2. For each contig that has GOs, get from pja_unitrans_go (direct and indirect) 
    * 	 and go_info (level=y) and e-value<x
     */
    public boolean saveGOtoFile(String fileName, STCWFrame frame, String level, String eval, String type)
    {
    		try
    		{
    			String delim = Globalx.CSV_DELIM;
    			PrintWriter pw = getWriter("GOs ", fileName, frame);
    			if (pw==null) return false;
    			
    			String msg = "   using Level: " + level + ", E-value: " + eval;
    			if (!type.equals("")) msg += ", " + type;
    			Out.prt(1, msg);
    	
    			int rowCnt = getRowCount();
    			Out.prt(1, "Processing " + rowCnt + " sequences...");
    			 
    	// Step 1: get all assigned and inherited GOs
    			
    			// get Seq Names from display table 
	        int seqIdx = getColumnModel().getColumnIndex("Seq ID");
	    		String [] seqNames = new String [getRowCount()];
	        
	        for (int row = 0;  row < rowCnt;  row++) {
	        		seqNames[row]= (String) getValueAt(row, seqIdx);
	        }
	       
	       // get SeqID for database for fast query in main loop
	        HashMap <String, Integer> seqIdMap = new HashMap <String, Integer> (); 
	        DBConn mdb = frame.getNewDBC();
	        ResultSet rs = mdb.executeQuery(
	        		"select contigid, CTGID from contig where PIDgo>0");
	        while (rs.next()) seqIdMap.put(rs.getString(1), rs.getInt(2));
	        rs.close();
	        
	        Out.prt(1, "Loading GOs from sequences....");
	        TreeMap<Integer,Integer> goCntMap = new TreeMap<Integer,Integer>();
		    
		    String select = " select ug.gonum " +
		    		" from pja_unitrans_go as ug " +
		    		" join go_info as gi on gi.gonum=ug.gonum "; // for where clause
		    String where="";
			if (!eval.equals("")) where = " and ug.bestEval<=" + eval; 
		    if (!level.contains("-")) where += " and gi.level=" + level;
	        	if (!type.equals("")) where += " and gi.term_type='" + type + "'"; 
	        
	        	// find the GOs from these contigs
	       int cntGoSeqPairs=0, cnt=0;
	 	   for (String name : seqNames) 
	 	   {
	 	        if (!seqIdMap.containsKey(name)) continue;
	 	        int seqid = seqIdMap.get(name);
	 	        String query =  select + " where ug.CTGID=" + seqid + " " + where; 
                	
                rs = mdb.executeQuery(query);
                while (rs.next())
                {
	                	int gonum = rs.getInt(1);
	                	if (!goCntMap.containsKey(gonum)) goCntMap.put(gonum, 1);
	                	else goCntMap.put(gonum,1+goCntMap.get(gonum));
	                
	                	cntGoSeqPairs++;
                }
                rs.close();
              
                if (!isApplet && cnt%100==0)
  	 	          Out.r("Seq# " + (cnt+1) +
  	 	        		"   Unique GOs " + goCntMap.size() + "  #total GO-seqs " + cntGoSeqPairs );
                cnt++;
	        }
	        Out.prt(1, "GOs to write to file: " + goCntMap.size() + 
	        		"  GO-seq-pairs: " + cntGoSeqPairs + "           ");
	       
	// Step 2: Get go_info information and write to file        
	      	// make sql - get the P__ columns
		    Vector<String> pcols = new Vector<String>();
		    String prefix = Globalx.PVALUE;
		    rs = mdb.executeQuery("show columns from go_info like '" + prefix + "%'");
		    while (rs.next()) pcols.add(rs.getString(1));
		    rs.close();
		    
		 	String query = "select level,  term_type,  descr ";
		 	if (pcols.size() > 0) query += "," + Static.strVectorJoin(pcols, ",") ;
		 	query += " from go_info where gonum=";
		            
	        Out.prt(1,"Start writing....                    "); 
	        pw.write("### #Seqs: " + rowCnt + "   #GOs: " + goCntMap.size() + "   #GO-seq pairs: " + cntGoSeqPairs + "\n");
	        pw.write("GO" + delim + " #Seqs" + delim  + "Level" + delim  + " Term_type" + delim +  "Description");
	        if (pcols.size() >0)
	        {   int x = prefix.length();
		        pw.write(delim);
		        for (String p : pcols)
		        		pw.write(p.substring(x) + delim); 
		        
	        }
	        pw.write("\n");
	        int nrows = 0;
	        for (int gonum : goCntMap.keySet())
	        {
		        	rs = mdb.executeQuery(query + gonum);
		        	if (rs.first())
		        	{
		        		StringBuffer sb = new StringBuffer();
		        		sb.append(String.format(GO_FORMAT,gonum));
		        		sb.append(delim + String.format("%5d", goCntMap.get(gonum))); // count
		        		sb.append(delim + rs.getString(1)); // level
		        		sb.append(delim + rs.getString(2)); // term_type
		        		sb.append(delim + rs.getString(3)); // desc 
		        		int c=3;
		        		for (int j = 1; j <= pcols.size(); j++)
		        		{
		        			sb.append(delim + formatD(rs.getDouble(c+j))); 
		        		}
		        		pw.write(sb.toString());
		        		pw.write("\n");
		        		nrows++;
		        		
		        		if (!isApplet && nrows%100==0) Out.r("Output go#");
		        	}
		        	else{Out.PrtError("Failed to find GO:" + gonum);}
	        }
	        pw.close();
	        rs.close(); mdb.close();
	        Out.PrtSpMsg(0, "Complete writing " + nrows + " rows             ");
	        return true;
	    	}
	    	catch(Exception e){ ErrorReport.prtReport(e, "Writing GO file");}
    		return false;
    }
    
    private String formatD(Double d) {
    		if (d>=0.001) return String.format("%4.3f", d);
	 	else return String.format("%.1e", d);
	}
 
  	public PrintWriter getWriter(String label, String dName, STCWFrame frame) {
 		try {
 			Out.prt("Popup prompt for file name...");
 			 
 			String lastDir = frame.lastSaveFilePath;
			final JFileChooser fc = new JFileChooser(lastDir);	
			fc.setSelectedFile(new File(dName));
			
			if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
				Out.prt("   Cancel file chooser");
				return null;
			}
			if (fc.getSelectedFile() == null) {
				Out.prt("   No selected file");
				return null;
			}
			
			File f = fc.getSelectedFile();
			String filePath = f.getPath();
			final File d = f.getParentFile();
			if (!d.canWrite()) { 
				JOptionPane.showMessageDialog(null, 
						"You do not have permission to write to directory " + d.getName(), "Warning", JOptionPane.PLAIN_MESSAGE);
				return null;
			}
			lastDir = d.getAbsolutePath();
			boolean append=false;
			String msg="Export from table ";
			
			if (f.exists()) {
				Object[] options = {"Cancel", "Overwrite", "Append"};
				int n = JOptionPane.showOptionDialog(frame,
				    "File exists: " + filePath, "File exists",
				    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				    null, options, options[2]);
				if (n==0) return null;
				
				frame.setLastPath(f.getPath());
				if (n==2) {
					msg="Append from table ";
					append=true;
				}
			}
			Out.prt(msg + " - " + label + " to " + filePath);
			frame.lastSaveFilePath = filePath; 
			
			boolean isApplet = UIHelpers.isApplet();
			if(isApplet) {
			    UserPrompt.showMsg("Exporting a file across the network is very slow.\n" +
			    		"A message box will popup when done.");
			}
 			return new PrintWriter(new BufferedWriter(new FileWriter(f, append)));
 		} 
 		catch (Exception e) {ErrorReport.prtReport(e, "Error: cannot write file");}
 		return null;
    	}
 }
