package cmp.compile;

/*********************************************************
 * Loads the data from all sTCW databases to be analyzed
 */
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JOptionPane;

import util.align.AAStatistics;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.Converters;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.UserPrompt;

import cmp.database.*;
import cmp.compile.panels.CompilePanel;
import cmp.compile.panels.SpeciesPanel;

public class LoadSingleTCW {
	private static final int COMMIT=10000;
	private static final long MAX_RANK = 5; // load top N hits transferred from sTCW to mTCW
	private int idx5=0, idxC=1, idx3=2;
	
	// The nucleotide sequence is reverse complemented if frame<0; the start and end are changed, but frame stays neg
	public LoadSingleTCW(DBConn dbc, CompilePanel panel) {
		mDB = dbc;
		cmpPanel = panel;
		theSpeciesPanel = cmpPanel.getSpeciesPanel();
	}
	public boolean run() {
		long startTime = Out.getTime();
		Out.PrtDateMsg("\nLoading SingleTCWs with top " + MAX_RANK + " hits");
		
    		if (!step1_datasetTable()) return false;
    		if (!Schema.addDynamicCountColumns(mDB, cmpPanel)) return false;
     	if (!step3_uniqueHitsTable()) return false; // create hitMap  	
		if (!step4_seqTable()) return false;			// create seqMap and expMap    
		if (!step5_seqHitsTable()) return false;
		if (!removeExtra()) return false;  // major kludge - need to rewrite so do not do step3
		if (!step6_computeInfo()) return false;
		Out.PrtSpMsgTime(0, "Finish loading from database", startTime);
		return true;
	}
		
	/*********************************************
	 * creates the table of databases used in this mTCW
	 */
	private boolean step1_datasetTable() {
	try {  	
		Out.PrtSpMsg(1, "Creating Dataset Table");
		
		for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
			Out.PrtSpMsg(2, "Adding " + cmpPanel.getSpeciesDB(x));
			String remark = theSpeciesPanel.getRemarkAt(x);
			
			DBConn stcwDBC = runMTCWMain.getDBCstcw(cmpPanel, x);
			if (stcwDBC==null) return false; // error already printed
			
			ResultSet rs = stcwDBC.executeQuery(
			"SELECT assemblyid, username, assemblydate, annotationdate FROM assembly");
			if (!rs.next()) {
				String msg = "Incomplete or invalid sTCW database: " +
						cmpPanel.getSpeciesDB(x) + "\nMake sure you Instantiate";
				return prtError(msg);
			}
			String sqlAsm =  
					"ASMstr=" +   		quote(rs.getString("assemblyid")) 		+ "," +	
					"prefix=" +			quote(theSpeciesPanel.getPrefixAt(x))	+ "," +
					"username=" + 		quote(rs.getString("username")) 		+ "," +
					"assemblydate=" + 	quote(rs.getString("assemblydate")) 	+ "," +
					"annotationdate=" + quote(rs.getString("annotationdate")) 	+ "," +
					"remark=" +			quote(remark) + ",";			
			
			rs = stcwDBC.executeQuery("SELECT dbDate FROM pja_databases");
			if (rs.next()) // maybe not annotated
				sqlAsm += "annoDBdate=" + quote(rs.getString("dbDate")) + ",";
			
			rs = stcwDBC.executeQuery("SELECT COUNT(*) FROM pja_databases");
			if (rs.next()) sqlAsm += "nAnnoDB=" + rs.getInt(1);
			else sqlAsm += "nAnnoDB=0";
			
			if (stcwDBC.tableColumnExists("assem_msg", "peptide")) 
				sqlAsm += ", isPep=1";
			else nNT++; // CAS305
			
			try {
				mDB.executeUpdate("INSERT INTO assembly SET " + sqlAsm);
			}
			catch (Exception e) { 
				Out.Print("Lost connection, trying again...");
				mDB = cmpPanel.getDBconn();
				mDB.executeUpdate("INSERT INTO assembly SET " + sqlAsm);
			}
			Schema.addDynamicSTCW(mDB, cmpPanel.getSpeciesSTCWid(x));
		}
		return true;
	} catch(Exception e) {ErrorReport.prtReport(e, "Error on building database");
						return prtError("Fatal error");}
	}

	/****************************************************
	 * Build table of  unique hits 
	 * Everything is loaded so have HITid when sequence hits are loaded,
	 * but as only hits with rank<= 3 are added, some extra hits get loaded
	 */
	private boolean step3_uniqueHitsTable() {
	try {
		long startTime = Out.getTime();
		Out.PrtSpMsg(1, "Creating unique hit table");
	
		HashSet <String> hitSet = new HashSet <String> ();
		int dup=0;
		
		PreparedStatement ps = mDB.prepareStatement("INSERT INTO unique_hits " +
				"(HITstr, dbtype, taxonomy, isProtein, " +
							"description, species, length, sequence, goList, nGO)" +
				" values(?,?,?,?,?,?,?,?,?, ?)");
		
		for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
			mDB.openTransaction();  
			
  			Out.PrtSpMsg(2, "Dataset " + cmpPanel.getSpeciesSTCWid(x));
			DBConn stcwDBC = runMTCWMain.getDBCstcw(cmpPanel, x);
			
			int cnt=0, cntSave=0;
			int numRows = stcwDBC.executeCount("SELECT COUNT(*) FROM pja_db_unique_hits");       		
			       		
			ResultSet	rs = stcwDBC.executeQuery(
				"SELECT hitID, dbtype, taxonomy, isProtein, description, " +
				"species, length, sequence, goList FROM pja_db_unique_hits ORDER BY hitID ASC ");
  	  
			while(rs.next()) 
			{
				String HITstr = rs.getString(1).trim(); //rs.getString("hitID"); is hit String id
			
				if (!hitSet.contains(HITstr)) 
				{		
					String goList = rs.getString(9);
					int nGO=0;
					if (goList!=null && goList.length()>0) {
						nGO=1;
						for (int i=0; i<goList.length(); i++)
							if (goList.charAt(i)==';') nGO++;
					}
					ps.setString(1,HITstr);
					ps.setString(2,rs.getString(2)); 	// dbtype
					ps.setString(3,rs.getString(3)); 	// tax
					ps.setInt(4,rs.getInt(4));			// isProtein
					ps.setString(5,rs.getString(5)); 	// description
					ps.setString(6,rs.getString(6)); 	// species
					ps.setInt(7,rs.getInt(7));			// length
					ps.setString(8,rs.getString(8)); 	// sequence
					ps.setString(9, goList);			// goList
					ps.setInt(10, nGO);
					ps.addBatch();			
					cnt++; cntSave++; 
					if(cntSave==COMMIT)  {
						Out.rp("unique hits ", cnt, numRows); 
						cntSave=0;
						ps.executeBatch();
					}	
					hitSet.add(HITstr);
				}
				else dup++;
			}
			if (cntSave>0) ps.executeBatch();
			rs.close(); stcwDBC.close();
			mDB.closeTransaction();  
			// hitSet.clear(); do not clear - causes a lot duplicates!!
			
    			System.err.print("                                                   \r");
			Out.PrtSpCntMsg(3, cnt, "Unique hits for dataset");
		} // end loop through databases
		ps.close();	
		
		ResultSet rs = mDB.executeQuery("select HITid, HITstr from unique_hits");
		while (rs.next()) {
			hitMap.put(rs.getString(2), rs.getInt(1));
		}
		rs.close();
		
		Out.PrtSpMsg(2, "Total");
		Out.PrtSpCntMsg(3, hitMap.size(), "Unique hits ", startTime);
		Out.PrtSpCntMsg(3, dup, "Duplicates ");
		return true;
	} catch(Exception e) {
		ErrorReport.reportError(e);return false;
	} catch (OutOfMemoryError error) {
		ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run"); return false;
	}
	}
	
	/****************************************************
	 * create table of all sequences
	 */
	private boolean step4_seqTable() {
	try { 
		long startTime = Out.getTime();
		Out.PrtSpMsg(1, "Creating Sequence table");	
		int seqCnt = 0, ASMid=0;
    		
		String sqlSR1 = "SELECT pja_db_unitrans_hits.uniprot_id, pja_db_unitrans_hits.e_value," +
				"contig.contigid, contig.consensus, contig.numclones,  " +
				"contig.totalexp, contig.totalexpN, " +
				"contig.o_frame, contig.o_coding_start, contig.o_coding_end ";
		String sqlSR3 = " FROM contig " +
				"LEFT JOIN pja_db_unitrans_hits ON pja_db_unitrans_hits.PID = contig.PIDov " +
				"ORDER by contig.contigid ASC ";	
		   		
 		for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
 			mDB.openTransaction();  
 			
   			Out.PrtSpMsg(2, "Dataset " + cmpPanel.getSpeciesSTCWid(x));	
   			boolean isProteinDB = theSpeciesPanel.isPeptideDBAt(x);
			
   			DBConn stcwDBC = runMTCWMain.getDBCstcw(cmpPanel, x);
   			if (!isProteinDB) {
	   			String orfs = stcwDBC.executeString("select orf_msg from assem_msg");
	   			boolean noOrf = (orfs==null || orfs=="" || orfs.length()<10);
	   			if (noOrf) { // CAS304
	   				int cnt = stcwDBC.executeCount("select count(*) from contig where o_len>0"); // CAS305 was contigs
	   				noOrf = (cnt==0);
	   			}
				if (noOrf) {
					String msg = "The sTCW database has not been annotated; you must at least execute 'ORF only'";
					System.err.println(msg);
					JOptionPane.showMessageDialog(null, "No annotation in database " + cmpPanel.getSpeciesSTCWid(x) + "\n"
							+ msg, "Error", JOptionPane.PLAIN_MESSAGE);
					return false;
				}
   			}
			Vector<String> uniqueLibs = new Vector<String> ();   			
   	  		
   	  		int nSeq=0, nLib=0, nAnnoUT=0;
   	  		long nAlign=0, nExp=0;
   			ASMid++;
	   		
   	// Create dynamic part of query
   			// get libraries for this source assembly
   			String sqlSR2="";
   			Vector <String> libList = new Vector <String>();
   			Vector <String> deList = new Vector <String> ();
   			ResultSet rs;
   			
   			int cntlib = stcwDBC.executeCount("select count(*) from library where ctglib=1");
   			if (cntlib>0) { // ctgLib=1 means it has expression libraries
	   	   		rs = stcwDBC.executeQuery("select libid from library where ctglib=0"); 
				while (rs.next())
				{
					String lib = rs.getString(1);
					sqlSR2 += ", " + Globals.PRE_S_CNT + lib + ", " + Globals.PRE_S_RPKM + lib;
					libList.add(lib);
				}
				rs.close();
				
				// get DE columns for this dataset
				rs = stcwDBC.executeQuery("show columns from contig"); 
				while (rs.next())
				{
					String de = rs.getString(1);
					if (de.startsWith(Globals.PRE_S_DE)) {
						sqlSR2 += ", " + de;		// for singleTCW query
						de = de.substring(Globals.PRE_S_DE.length()); // for multiTCW insert
						deList.add(de);
					}
				}
				rs.close();	
   			}
			nLib = libList.size();
		
			// create dynamic insert into mDB statement
			String libSQL="", deSQL="", qSQL="";
			for (String lib : libList) {
			 	libSQL += "," + Globals.PRE_LIB + lib;
			 	qSQL += ",?";
			} 			
			for (String de : deList) {
		 		deSQL += "," + Globals.PRE_DE + de; 
		 		qSQL += ",?";
			}

			PreparedStatement ps2 = mDB.prepareStatement("INSERT INTO unitrans " +
					"(UTid, UTstr, ASMid, numAlign, totExp, totExpN, orf_frame, orf_start, orf_end," +
					" ntSeq, ntLen, aaSeq, aaLen, HITid, HITstr, e_value" +
					libSQL + deSQL + ", expList, expListN)" +
					" values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" + 
					qSQL + ",?,?)");
			
			int cntSave=0;
	 // loop through contigs transferring data
	   		int numRows = stcwDBC.executeCount("SELECT COUNT(*) FROM contig");
	   			
	   		rs = stcwDBC.executeQuery(sqlSR1 + sqlSR2 + sqlSR3);

	   		while(rs.next()) {
				String HITstr = rs.getString(1); // uniprot_id
				String seqName = rs.getString(3); // contigid
				String seqTmp = rs.getString(4); // consensus
				int numAlign = rs.getInt(5);  // numclones
				int totalExp = rs.getInt(6);  // totalexp
				int totalExpN = rs.getInt(7); // totalexpN
				
				int frame = rs.getInt(8)	; // o_frame
				int start = rs.getInt(9); // o_coding_start
				int end = rs.getInt(10);     // o_coding_end
				
	    			nAlign += (long) numAlign;
	        		nExp   += (long) totalExp;  		
	        		
	        		if (seqMap.containsKey(seqName)) {
	        			UserPrompt.showError("Abort build\nDuplicate sequence ID'" + seqName + "'");
	        			Out.PrtError("Duplicate sequence ID'" + seqName + "'");
	        			return false;
	        		}
	        		seqCnt++;
	        		seqMap.put(seqName, seqCnt);  
	        				
	        		String aaSeq="", ntSeq="";
	        		if (isProteinDB) {
	        			aaSeq = seqTmp.toUpperCase();
	        		}
	        		else {
	        			ntSeq = seqTmp.toLowerCase();
	        			ntSeq = ntSeq.replace("*", "n"); // TCW assembly can leave '*' in string
	        			if (frame<0)  ntSeq = getRevCompl(ntSeq);
	        			// create AA seq
	        			int cntStop=0;
	        			char c=' ';
	        			for (int i = start-1; i <end && (i+2)<ntSeq.length(); i+=3) {
	        				c = AAStatistics.getAminoAcidFor(
	        						ntSeq.charAt(i), ntSeq.charAt(i+1),ntSeq.charAt(i+2));
	        				aaSeq += c;
	        				if (c=='*') cntStop++;
	        			}
	        			if (c=='*') cntStop--; // stop codon
	        			if (cntStop>0) Out.PrtWarn(seqName + " has stop " + nSeq + " codons in translation");
	        		}
	        		ps2.setInt(1, seqCnt);
	        		ps2.setString(2, seqName);
	        		ps2.setInt(3, ASMid);
	        		ps2.setInt(4, numAlign);
	        		ps2.setInt(5, totalExp);
	        		ps2.setInt(6, totalExpN);
	        		ps2.setInt(7, frame);
	        		ps2.setInt(8, start);
	        		ps2.setInt(9, end);
	        		ps2.setString(10, ntSeq);
	        		ps2.setInt(11, ntSeq.length());
	        		ps2.setString(12, aaSeq);
	        		ps2.setInt(13, aaSeq.length());
	        		
		        	// add HITid, HITstr - this is best annotation (PIDov)
	        		 int HITid=0; // sTCW has the default HITid=null; mTCW uses HITid=0
	        		 if (HITstr != null) {
	        		 	nAnnoUT++;
	        		 	if (hitMap.containsKey(HITstr)) 
	        			{
	        				HITid = hitMap.get(HITstr);
	        				ps2.setInt(14, HITid);
	        				ps2.setString(15, HITstr);
	        				ps2.setDouble(16, rs.getDouble(2)); // e_value
	        		 	}
	        		 	else {
	        		 		Out.PrtError("No unique hit for dataset ID " + ASMid + " hit " + HITstr);
	        		 		return false;
	        		 	}
	        		 }
	        		 else {
	        			ps2.setInt(14, 0);
	        			ps2.setString(15, null);
	        			ps2.setDouble(16, Globalx.dNoVal); // was setting to null
	        		 }

	        		 int k=17;
		        	// add expList, expListN plus the actual LN values
	        		 String expList="", expListN="";
	        		 for (int i=0; i<libList.size(); i++) {
    		 			String lib = libList.get(i);
    		 			int expCnt = rs.getInt(Globals.PRE_S_CNT + lib);
    		 			double expRPKM = rs.getDouble(Globals.PRE_S_RPKM + lib);
    		 	
    		 			if(!expMap.containsKey(cmpPanel.getSpeciesSTCWid(x))) {
    		 				expMap.put(cmpPanel.getSpeciesSTCWid(x), lib);
    		 				uniqueLibs.add(lib);
    		 			}
    		 			else {
    		 				if(!uniqueLibs.contains(lib)) {
    		 					uniqueLibs.add(lib);
    		 					expMap.put(cmpPanel.getSpeciesSTCWid(x), expMap.get(cmpPanel.getSpeciesSTCWid(x)) + " " + lib);
    		 				}
    		 			}
    		 			expList +=  String.format("%s=%d ", lib, expCnt);
    		 			expListN += String.format("%s=%s ", lib, Converters.roundBoth(expRPKM, 3, 2));
    		 			ps2.setDouble(k++, expRPKM);
    		 		}
	        		 		// add the DE values
    		 		for (String de : deList) {
    		 			double p = rs.getDouble(Globals.PRE_S_DE + de);
    		 			ps2.setDouble(k++, p);
    		 		}
    		 		ps2.setString(k++, expList);
     		 	ps2.setString(k++, expListN);
    	        			
     		 	ps2.addBatch();			
				nSeq++; cntSave++; 
				if(cntSave==COMMIT)  {
					Out.rp("sequences ", nSeq, numRows); 
					cntSave=0;
					ps2.executeBatch();
				}		
        		} // end while through this dataset
        		if (cntSave>0) ps2.executeBatch();
   			rs.close();	ps2.close();
        		stcwDBC.close(); 
        		
        		System.err.print("                                                   \r");
        		Out.PrtSpCntMsg(3, nSeq, "Sequences ");
	        		
        		mDB.executeUpdate("update assembly set " +
    				"nUT = " 		+ nSeq 		+ "," +
    				"nAnnoUT = " 	+ nAnnoUT 	+ "," +
    				"nAlign = " 		+ nAlign 	+ "," +
    				"nExp = " 		+ nExp		+ ", " +
    				"nLib = "		+ nLib		+ " " +
        			"where ASMid = " + ASMid);
        		mDB.closeTransaction();  
     	} // end loop through databases
 		
 		Out.PrtSpMsg(2, "Total");
 		Out.PrtSpCntMsg(3, seqMap.size(), "Sequences ", startTime);
     	return true;
	}catch (Exception e) {ErrorReport.prtReport(e, "Creating sequence table");return false;
	} catch (OutOfMemoryError error) {
		ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run"); return false;
	} 
	}
	
	/****************************************************
	 * Build table of  unitrans hits 
	 * All sequence hits are added that have a rank <= N, which includes the best anno and best eval
	 */
	private boolean step5_seqHitsTable() {
	try {
		long startTime = Out.getTime();
		Out.PrtSpMsg(1, "Creating Sequence hit table");
		int countAll=0;
		PreparedStatement ps3 = mDB.prepareStatement("INSERT INTO unitrans_hits " +
			"(HITid, HITstr, UTid, UTstr, percent_id, alignment_len, seq_start, seq_end, " +
			"hit_start, hit_end, e_value, bit_score, type, bestEval, bestAnno, bestGO)" +
				" values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		for(int x=0; x<cmpPanel.getSpeciesCount(); x++) {
			mDB.openTransaction();  
			
  			Out.PrtSpMsg(2, "Dataset " + cmpPanel.getSpeciesSTCWid(x));
			DBConn stcwDBC = runMTCWMain.getDBCstcw(cmpPanel, x);
	  					
			int nHit=0, cntNotFound=0, cntSave=0;
			  
			ResultSet rs = stcwDBC.executeQuery(
				"SELECT contigid, uniprot_id, percent_id, alignment_len, " +
					"ctg_start, ctg_end, prot_start, prot_end, e_value, bit_score, dbtype, taxonomy, " +
					"filter_best, filter_ovbest, filter_gobest " +
					"FROM pja_db_unitrans_hits " +
					"where blast_rank <=" + MAX_RANK + 
					" or filter_best>0 or filter_ovbest>0 or filter_gobest>0"); 
  
			while(rs.next()) 
			{
				String CTGstr = rs.getString(1);  
				if (!seqMap.containsKey(CTGstr)) {
					ErrorReport.reportError("Cannot find seqID " + CTGstr);
					continue;
				}
				int CTGid = seqMap.get(CTGstr);
				String HITstr = rs.getString(2);
				if (!hitMap.containsKey(HITstr)) { 
					if (cntNotFound<5) ErrorReport.reportError("Cannot find hitID " + HITstr + "                  ");
					else if (cntNotFound==5) ErrorReport.reportError("Surpress further not found errors");
					cntNotFound++;
					continue;
				}
				int HITid = hitMap.get(HITstr);
				
				String tp = rs.getString(11);
				String tx = rs.getString(12);
				if (tp.length() > 2) tp = tp.substring(0,2);
				if (tx.length() > 3) tx = tx.substring(0,3);
				String type = 	tp.toUpperCase() + tx;
				
				ps3.setInt(1, HITid);
        			ps3.setString(2, HITstr);
        			ps3.setInt(3, CTGid);
        			ps3.setString(4, CTGstr);
        			ps3.setInt(5, rs.getInt(3));// percent_id
        			ps3.setInt(6, rs.getInt(4));// alignment len
        			ps3.setInt(7, rs.getInt(5));// seq_start	
        			ps3.setInt(8, rs.getInt(6));// seq_end	
        			ps3.setInt(9, rs.getInt(7));// hit_start	
        			ps3.setInt(10, rs.getInt(8));// hit_end	
        			ps3.setDouble(11, rs.getDouble(9));// e_value		
        			ps3.setFloat(12, rs.getFloat(10));// bit_score	
        			ps3.setString(13, type);	
        			ps3.setInt(14, rs.getInt(13));// filter_best
        			ps3.setInt(15, rs.getInt(14));// filter_ovbest
        			ps3.setInt(16, rs.getInt(15));// filter_gobest	
					
        			ps3.addBatch();			
    				nHit++; cntSave++; 
    				if(cntSave==COMMIT)  {
    					Out.r("hits " + nHit);
    					cntSave=0;
    					ps3.executeBatch();
    				}		
			} // end while 
			if (cntSave>0) ps3.executeBatch();
   			rs.close();
   			stcwDBC.close();
   			mDB.closeTransaction(); 
   			
        		countAll+=nHit;
			Out.PrtSpCntMsg(3, nHit, "Sequence hits                                 ");
		} // end loop through databases
		ps3.close();
		
		Out.PrtSpMsg(2, "Total");
   		Out.PrtSpCntMsg(3, countAll, "Sequence hits ", startTime);
   		return true;
	} catch(Exception e) {ErrorReport.reportError(e);return false;
	} catch (OutOfMemoryError error) {
		ErrorReport.prtReport(error, "Out of memory -- increase memory in runMultiTCW and re-run"); return false;
	}
	}
	private boolean removeExtra() {
		try {
			Out.PrtSpMsg(2, "Remove extras");
			int nRow = mDB.executeCount("select count(*) from unique_hits");
			HashSet <Integer> keep = new HashSet<Integer> ();
			ResultSet rs = mDB.executeQuery("select unique_hits.HITid from unique_hits " +
					"join unitrans_hits on unitrans_hits.HITid = unique_hits.HITid");
			while (rs.next()) keep.add(rs.getInt(1));
			
			int rm=nRow-keep.size();
			Out.PrtCntMsg(rm, "To be removed");
			mDB.openTransaction(); 
			int cnt=0;
			for (int i=0; i<nRow; i++) {
				if (!keep.contains(i)) {
					mDB.executeUpdate("delete from unique_hits where HITid=" + i);
					cnt++;
					if (cnt%1000==0) Out.rp("Remove ", cnt, rm);
				}
			}
			Out.PrtCntMsg(cnt, "Complete removal ");
			mDB.closeTransaction();
			
			return true;
		}
		catch (Exception e) {ErrorReport.die(e, "Error updating schema");}
		return false;
	}
	
	/**********************************************
	 * For each sequence: compute CpG[o/e], GC, lengths for UTRs and CDS
	 * For overview: compute avg length of UTRs, CDS, GC and CpG -- SE for GC and CpG
	 */
	private boolean step6_computeInfo() {
		try {
			if (nNT==0) return true;
			
			Out.PrtSpMsg(1,"Compute CpG [O/E] per sequence and overall");
			
		// Initialize dataset and sequence information
			ResultSet rs = null;
			HashMap <Integer, Asm> asmMap = new HashMap<Integer, Asm> ();
			rs = mDB.executeQuery("select ASMid, prefix, isPep from assembly");
			while (rs.next()) {
				Asm asm = new Asm();
				asm.isPep = (rs.getInt(3)==1);
				asmMap.put(rs.getInt(1), asm);
			}
			
			HashMap <Integer, Info> seqMap = new HashMap <Integer, Info> ();
			rs = mDB.executeQuery("select UTid, ASMid, orf_start, orf_end, ntSeq from unitrans");
			while (rs.next()) {
				int asmid = rs.getInt(2);
				if (asmMap.get(asmid).isPep) continue; 
				
				Info in = new Info();
				int seqid = rs.getInt(1);
				in.asmid = asmid;
				in.start = rs.getInt(3);
				in.end = rs.getInt(4);
				in.seq = rs.getString(5).toLowerCase();
				in.len = in.seq.length();
				
				seqMap.put(seqid, in);
			}
			rs.close();
			
			int cnt=0, cntSave=0;
			Out.PrtSpCntMsg(2, seqMap.size(), " sequences to be analyzed");
			
			PreparedStatement ps = mDB.prepareStatement("update unitrans set " +
					"utr5Len=?, cdsLen=?, utr3Len=?, utr5Ratio=?, cdsRatio=?, utr3Ratio=?, GC=?, CpG=? " +
					" where UTID=?");
			mDB.openTransaction();  
		// Process each sequence
			String [] region = new String [3];
			double [] ratio = new double [3];
			
			for (int id : seqMap.keySet()) {
				Info seqObj = seqMap.get(id);
				Asm as = asmMap.get(seqObj.asmid);
				
				int seqAllGC=0, seqAllCpG=0;
				
				region[idx5] = seqObj.seq.substring(0, seqObj.start-1);
				region[idxC] = seqObj.seq.substring(seqObj.start-1, seqObj.end);
				region[idx3] = seqObj.seq.substring(seqObj.end);
				
				for (int x=0; x<3; x++) {
					char last=' ';
					int nCpG=0, nC=0, nG=0, regLen=region[x].length();
					for (int i=0; i<regLen; i++) {
						char c = region[x].charAt(i);
						if (c=='c' || c=='g')    {
							seqAllGC++;
							if (c=='c') nC++;
							else if (c=='g') nG++;
						}
						if (last=='c' && c=='g') {
							seqAllCpG++;
							nCpG++;
						}
						last = c;
					}
				
					if (nC>0 && nG>0) 
						ratio[x] = ((double)nCpG/(double)(nC*nG))* (double)regLen;	
					else ratio[x] = 0.0;
					
					as.cntC[x] += nC;
					as.cntG[x] += nG;
					as.cntCpG[x] += nCpG;
					as.sumLen[x] += regLen;
				}
				
				double pGC =  ((double)seqAllGC /(double)seqObj.len)*100.0;
				double pCpG = ((double)(seqAllCpG*2)/(double)seqObj.len)*100.0; // CASX 6/30/19 added *2
				
				ps.setInt(1, region[idx5].length());
				ps.setInt(2, region[idxC].length());
				ps.setInt(3, region[idx3].length());
				ps.setDouble(4, ratio[idx5]);
				ps.setDouble(5, ratio[idxC]);
				ps.setDouble(6, ratio[idx3]);
				ps.setDouble(7, pGC);
				ps.setDouble(8, pCpG);
				ps.setInt(9, id);
				ps.addBatch();	
				cntSave++;   
				
				if(cntSave==COMMIT)  {
					Out.r("process " + cnt);
					cntSave=0;
					ps.executeBatch();
				}	
				
				as.pCnt++;
				cnt++;
					
			} // end while 
			if (cntSave>0) ps.executeBatch();
			ps.close(); 
			mDB.closeTransaction();  
			seqMap.clear();
			
			// OVERVIEW-INFO
			// Average Length   %GC    			CpG-O/E
			// 5UTR CDS 3UTR5	5UTR CDS 3UTR	5UTR CDS 3UTR
			Out.PrtSpMsg(1, "Compute overview info for datasets");
			String [] ch1 = {"", "Average", "Lengths", "", "", "%GC ", "", "", "", "CpG O/E", "", ""};
			String [] ch2 = {"", "5UTR", "CDS", "3UTR", "  ", "5UTR", "CDS", "3UTR", "  ", "5UTR", "CDS", "3UTR"};
			int [] justify = {0,0,0,0, 0,0,0,0, 0,0,0,0};
			
			int nRow = asmMap.size()+2;
		    int nCol=  ch2.length;
		    String [][] rows = new String[nRow][nCol];
			
			int r=0;
			for (int i=0; i<ch1.length; i++) rows[r][i] = ch1[i];
			r++;
			for (int i=0; i<ch2.length; i++) rows[r][i] = ch2[i];
			r++;
			
			for (int asmid : asmMap.keySet()) {
				Asm as = asmMap.get(asmid);
				if (asmMap.get(asmid).isPep) continue;
				
				for (int x=0; x<3; x++) {
					as.avgLen[x] = String.format("%5.1f",((double) as.sumLen[x]/ (double) as.pCnt));
					as.pGC[x] =    Out.perFtxt((as.cntG[x]+as.cntC[x]), as.sumLen[x]);
					
					double p = ((double)as.cntCpG[x] / (double)(as.cntG[x]*as.cntC[x])) * as.sumLen[x];
					as.CpG_OE[x] = String.format("%5.3f", p);
				}
				String prefix = mDB.executeString("select ASMstr from assembly where ASMid=" + asmid);
				
				rows[r][0]=prefix;
				rows[r][1]= as.avgLen[idx5];
				rows[r][2]= as.avgLen[idxC];
				rows[r][3]= as.avgLen[idx3];
				
				rows[r][4]="";
				rows[r][5]=as.pGC[idx5];
				rows[r][6]=as.pGC[idxC];
				rows[r][7]=as.pGC[idx3];
				
				rows[r][8]="";
				rows[r][9]=as.CpG_OE[idx5];
				rows[r][10]=as.CpG_OE[idxC];
				rows[r][11]=as.CpG_OE[idx3];
				r++;
			}
			String dbInfoMsg = Out.makeTable(nCol, nRow, justify, rows);
			Out.PrtSpMsg(1, "Complete update");
			System.out.println(dbInfoMsg);
			mDB.executeUpdate("update info set seqInfo='"+ dbInfoMsg + "'");
			return true;
		}
		catch (Exception e) {ErrorReport.die(e, "Error computing summary info"); return false;}
	}
	private class Info {
		int start, end, len, asmid;
		String seq;
	}
	private class Asm {
		int pCnt=0; // index into pgc, pcgp as adding these values - ends up = seqCnt
		long [] cntG = new long [3];
		long [] cntC = new long [3];
		long [] cntCpG = new long [3];
		long [] sumLen = new long [3];
		String [] avgLen = new String[3];
		String [] pGC = new String[3];
		String [] CpG_OE = new String[3];
		boolean isPep=false;
	}

	private String quote(String word) {
		if (word==null || word.equalsIgnoreCase("null"))
		{
			return word;
		}
		return "\"" + word + "\""; 
	}
	
	private String getRevCompl(String seqIn) {
		String compSeq = "";
		for (int i = seqIn.length() - 1; i >= 0; --i) {
			compSeq += getBaseCompl(seqIn.charAt(i));
		}
		return compSeq;
	}
	private char getBaseCompl(char chBase) {
		switch (chBase) {
		case 'a': return 't'; case 'A': return 'T';
		case 'c': return 'g'; case 'C': return 'G';
		case 'g': return 'c'; case 'G': return 'C';
		case 't': return 'a'; case 'T': return 'A';
		case 'R': return 'Y'; case 'Y': return 'R';
		case 'M': return 'K'; case 'K': return 'M';
		case 'H': return 'D'; case 'B': return 'V';
		case 'V': return 'B'; case 'D': return 'H';
		case 'W': case 'S': case 'N': case 'n': 
		case Globals.gapCh: return chBase;
		default: 		
			return chBase;
		}
	}
	private boolean prtError(String msg) {
		JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.PLAIN_MESSAGE);
		Out.PrtError(msg);
		System.err.println("Remove database, correct error, and then run again");
		return false;
	}
	private HashMap <String, Integer> seqMap = new HashMap <String, Integer> ();  // seqName, UTid (multi)
	private HashMap <String, Integer> hitMap = new HashMap <String, Integer> (); //  hitName, HITid (multi)
	private HashMap <String, String>  expMap = new HashMap <String, String> ();
	
	private CompilePanel cmpPanel;
	private SpeciesPanel theSpeciesPanel;
	private DBConn mDB;
	private int nNT=0;
}
