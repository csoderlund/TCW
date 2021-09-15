package sng.database;

/**********************************************************
 * 1. Called during viewSingleTCW startup to create lines for Overview
 * 2. Called during execAnno to create the annotation part of Overview
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.text.DecimalFormat; 

import sng.viewer.STCWMain;
import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.TimeHelpers;

public class Overview {
	private final DecimalFormat dff = new DecimalFormat("#,###,###");
	private final int NSPECIES = 15;
	private int COVER1 = 50; // percent similarity cutoff and percent hit-align
	private int COVER2 = 90; // percent similarity cutoff and percent hit-align
	public Overview(DBConn dbC) {
		mDB = dbC;
	}
	// update overview from execAnno when COVER set (see below for update overview from viewSingle)
	public Overview(DBConn dbC, int c1, int c2) { 
		mDB = dbC;
		if (c1!=0) COVER1 = c1;
		if (c2!=0) COVER2 = c2;
		Out.Print("Using Cover values " + COVER1 + " " + COVER2);
		try { // CAS326
			mDB.executeUpdate("update assem_msg set anno_msg=''");
		}
		catch ( Exception err ) {ErrorReport.reportError(err,"Regenerate overview");}
	}
	/*****************************************************
	 * called from runSTCMain, ManagerFrame (Get Start if pja_msg==null), QRFrame, Schema (updateTo41)
	 */
	public String createOverview(Vector<String> lines) throws Exception {
        try { 
            return computeOverview(lines);
        }
        catch ( Exception err ) {
        	ErrorReport.reportError(err,"Error: processing data for overview");
	        return null;
        }
	}
	
	/**
	 * called from STCWFrame.addOverviewTab on startup  to be displayed in Overview panel
	 * if STCWMain.updateMSG, overview is regenerated
	 */
    public void overview (Vector<String> lines ) {   
        try {   
        	boolean found=true;
        	String pga_msg=null, meta_msg=null;
    		
        	if (!STCWMain.updateMSG) {
	            ResultSet rset = mDB.executeQuery("SELECT pja_msg FROM assem_msg");
	            if (rset.next() ) {
	            	pga_msg = rset.getString(1);
	            	try {
	            		rset = mDB.executeQuery("SELECT meta_msg FROM assem_msg");
	            		if (rset.next()) meta_msg = rset.getString(1);
	            	} catch (Exception e) {};
	            }
	            else{
	            	lines.add("Error creating overview (email tcw@agcol.arizona.edu for fix)");
	            	found=false;
	            }
	            rset.close();
	            if (!found) return;
        	}
        	else {// Update overview from viewSingleTCW
        		if (STCWMain.COVER1!=0) {
        			COVER1 = STCWMain.COVER1;
        			Out.prtSp(1, "Using Cover1 values " + COVER1);
        			mDB.executeUpdate("update assem_msg set anno_msg=''");
        		}
        		if (STCWMain.COVER2!=0) {
        			COVER2 = STCWMain.COVER2;
        			Out.prtSp(1, "Using Cover2 values " + COVER2);
        			mDB.executeUpdate("update assem_msg set anno_msg=''");
        		}
        	}
          
            if (pga_msg != null) {
                lines.add(pga_msg);
                lines.add(meta_msg);
            }
            else { 
            	computeOverview(lines);
            }     
            return;
        }
        catch ( Exception err ) {
    		ErrorReport.reportError(err,"Error: processing data for overview");
            return;
        }
    }
   
    /*****************************************************
     * Compute overview
     ****************************************************/
    private String computeOverview(Vector <String> lines)  {
    	try { 
    /* Compute */
    		setFlags();
		    computeSections(lines, true); 
		    
	/* save & put together results */
		    if (lines.size()<3)  lines.add("Error creating overview");
    	    
    		String text = "";
    		for (int i=0; i< lines.size(); i++)
    			text = text + lines.get(i) + "\n";
  
    		if (text.contains("\"")) {
    			text = text.replace("\"", "'"); // crashes if double-quote in title
    		}
   
         	// If this is a SKIP_ASSEMBLY, there is no entry so it needs to be inserted
            ResultSet rset = mDB.executeQuery("SELECT pja_msg FROM assem_msg where AID=1");
            if (rset.next() ) 
            	mDB.executeUpdate("update assem_msg set pja_msg = \"" + text + "\" where AID=1"); 
            else 
                mDB.executeUpdate("insert assem_msg set AID = 1, pja_msg = \"" + text + "\""); 
            rset.close();
            
      /* AnnoDBs and Legend: second part is separate so the message is not so long */
            String mtext = "";  
            Vector <String> mlines = new Vector <String> ();
         
        	finalAnnoDBs(mlines);
            finalLegend(mlines);
             
			for (int i=0; i< mlines.size(); i++) {
				mtext = mtext + mlines.get(i) + "\n";
				lines.add(mlines.get(i));
			}
            
			String fullText = text + "\n" + mtext;
			if (mDB.tableColumnExists("assem_msg", "meta_msg"))
				mDB.executeUpdate("update assem_msg set meta_msg = \"" + mtext + "\" where AID=1"); 
			else // strictly to update published v1.3.7 without any other changes
				mDB.executeUpdate("update assem_msg set pja_msg = \"" + fullText + "\" where AID=1");
            rset.close();
            	
      /* Finish */
           	writeHTML(fullText);
           	Out.prtSp(0, "Complete Overview");
           	return fullText;
        }  
        catch ( Exception err ) {
    		ErrorReport.reportError(err,"Error: adding overview");
            return null;
        }    
    }
    /**********************************************************************/
    private boolean computeSections(Vector<String> lines, boolean ask) {
    	try {
		    Out.prt("\nUpdating overview, this can take awhile on large databases, " +
		    		" but only is done when database content has changed...");
		    Out.prtSp(0, "Dataset statistics....");
			
		    if (!topSection(lines)) return false;
		    
		    if (!inputSection(lines)) return false;
			
			if (!annoSection(lines, ask)) return false;
			
			if (!expSection(lines)) return false;	
			
			if (!seqSection(lines)) return false;  // includes ORFs & GC

			if (!otherSection(lines)) return false; // Pairs and locations
			
			return true;
    	}
	    catch ( Exception err ) {
       		ErrorReport.reportError(err,"Error: creating overview");
            return false;
        }
    }
  
    private boolean topSection(Vector<String> lines) {
	    try {	 
	    	if (mDB==null) { 
    			lines.add( "Project: cannot create overview");
    			return false;
    		}
	        strAssmID = mDB.executeString( "SELECT assemblyid from assembly");
	        if (strAssmID==null || strAssmID=="" || strAssmID.contains("Not assembled")) {
	        	hasTranscripts=false;
	        	lines.add( "Project: Not instantiated yet");
	        }
	        else { // CAS303 add more information in header
	        	String h = "Project:  " + strAssmID ;
	        	if (isProteinDB) h += "  Protein";
	        	
	        	if (!hasNoAssembly) h += "   #Contigs: "  + dff.format(numSeqs);
	        	else 				h += "   #Seqs: " + dff.format(numSeqs);
	        	if (nUniqueHits>0)  h += "   #Hits: " + dff.format(nUniqueHits);
	        	if (nUniqueGOs>0)   h += "   #GOs: " + dff.format(nUniqueGOs);
	        	
	        	h += "   ";
	        	if (hasNorm) h += " " + normType + " ";
	        	if (hasSeqDE) h += " Seq-DE ";
	        	if (hasGODE) h += " GO-Enrich ";
	        	
	        	if (hasPairwise) h += "  Pairs";
	        	
	        	lines.add(h); 
	        }
        	lines.add(" ");

        // CAS319 put dates at top
        // build
        	String assemblyDate = mDB.executeString ( "SELECT assemblydate FROM assembly");
    		if (assemblyDate==null) return true;
    		
    		String msg = String.format("%s %-15s ", TimeHelpers.convertDate(assemblyDate), "Build Database");
    		
    	    if (hasNoAssembly) {
    		    if (isProteinDB) lines.add(msg + "proteins loaded from external source");
    		    else             lines.add(msg + "sequences loaded from external source");
    			
    	    }
    	    else lines.add(msg + "sequences assembled");
    	
    	 // annotate
        	msg="";
        	hasAnno = true;
             
            String annotationDate = mDB.executeString (  "SELECT annotationdate FROM assembly");    
            if (annotationDate==null || annotationDate.equals("") || annotationDate.equals("0000-00-00")) {
            	if (nUniqueHits>0) 
            		msg += "Annotation seems to have been interrupted";
            	else {
            		msg += "Annotatation has not been run yet"; 
            		hasAnno = false;
            	};
                lines.add(msg); lines.add("");
               
                return true; 
            }
           
            msg = String.format("%s %-15s ", TimeHelpers.convertDate(annotationDate), "Last Annotation");
            
            String annoVer = mDB.executeString( "SELECT annoVer from schemver");
    		if (annoVer != null) { 							// CAS318 put this second
    			msg += "with sTCW v" + annoVer;
    			// CAS331 this no longer happens - I think
    			if (!annoVer.equals(Version.strTCWver)) msg += "    Updated with v" + Version.strTCWver;
    		}
            lines.add(msg);
            lines.add("");
           
	        return true;
	    }
		catch ( Exception err ) {
    		ErrorReport.reportError(err,"Top lines");
    		return false;
    	}
    }
/**
* XXX Libraries and Sequence Sets
**/
    private boolean inputSection(Vector<String> lines) {
	    try {	 
        	lines.add("INPUT");
        
        	if (!inputExp(lines)) return false;
        	if (!inputSeq(lines)) return false;
        	
        	if (hasTranscripts==false) return false;
	        return true;
	    }
    	catch ( Exception err ) {
    		ErrorReport.reportError(err,"Error: processing data for overview libriries");
    		return false;
    	}
    }
    private boolean inputExp(Vector<String> lines) {
		try {
    		int nReadLibs = mDB.executeCount("SELECT COUNT(*) FROM library " +
        		"WHERE avglen = 0 && ctglib=0");
    		if (nReadLibs==0) return true;
  
    		lines.add("   Counts: ");	 
    		String [] fields = {"SEQID", "ID", "SIZE", "TITLE", "SPECIES", 
    						"CULTIVAR", "STRAIN", "TISSUE", "STAGE", 
    						"TREATMENT", "SOURCE", "YEAR", "#REPS"};     		
    		int [] just = {1, 1,0,1,1, 1,1,1,1, 1,1,1,0};
    
    		int nCol=fields.length; 
    		rows = new String[nReadLibs][nCol];
    		int r=0;
       
    		ResultSet rset = mDB.executeQuery("SELECT parent, libid, libsize, title, organism, cultivar, " +
				"strain, tissue, stage, treatment, source, year, reps  " +
				"FROM library WHERE ctglib=0 && avglen=0");	
        	    
	        while(rset.next()) {
	            for (int i=0; i< nCol; i++) {
	            	if (i==nCol-1) {
	        			int n=0;
	        			String rep = rset.getString(i+1);
	        			if (rep==null || rep.equals("")) n=1; // make Reps=0 be Reps=1
	        			else n = rep.split(",").length;
	        			rows[r][i] = dff.format(n);
	        		}
	            	else if (just[i]==0) {
	        			int v = rset.getInt(i+1);
	        			rows[r][i] = dff.format(v);
	        		}
	        		else rows[r][i] = rset.getString(i+1);
	            }
	            r++;
	        }
	        rset.close();
	        makeTable(nCol, r, fields, just, lines);
	        return true;
    	}
    	catch ( Exception err ) {
    		ErrorReport.reportError(err,"Error: processing data for overview libriries");
    		return false;
    	}       
    }
    
    private boolean inputSeq(Vector<String> lines) {
    	try {
    		int nSets = mDB.executeCount( "SELECT COUNT(*) FROM library WHERE avglen > 0");
    		if (nSets==0) return true;
        
    		lines.add("   Sequences: ");
    		String[] fields2 = {"SEQID", "SIZE", "TITLE", "SPECIES", 
        			"CULTIVAR", "STRAIN", "TISSUE", "STAGE", 
        			"TREATMENT", "SOURCE", "YEAR", "AVG-len", 
        			"MED-len"};
    		int [] just2 = {1,0,1,1, 1,1,1,1, 1,1,1,0, 0};
    		int nCol=fields2.length;
    		rows = new String[nSets][nCol];
    		int r=0;
           
    		ResultSet rset = mDB.executeQuery("SELECT libid, libsize," +
        		"title, organism, cultivar, strain, tissue, stage, treatment, source, year, " +
        		"avglen, medlen FROM library WHERE avglen>0");	

	        while(rset.next()) {
	    		for (int i=0; i< nCol; i++) {
	    			if (just2[i]==0) {
	        			int v = rset.getInt(i+1);
	        			rows[r][i] = dff.format(v);
	        		}
	        		else rows[r][i] = rset.getString(i+1);
	            }
	            r++;
	        }
	        rset.close();
	        
	        makeTable(nCol, r, fields2, just2,  lines);
	        return true;
		}
		catch (Exception err) {
			ErrorReport.reportError(err,"Error: reading database for overview libraries");
			return false;
		}
    }
   
 /******************************************************
  * XXX Anno Section
  ******************************************************/
    private boolean annoSection(Vector<String> lines, boolean ask) {
		if (hasAnno && nUniqueHits>0) { 
			lines.add( "ANNOTATIONS" ); 
			Out.prtSp(1, "Annotation statistics....");
			
			try { 
	        	String annoMsg = mDB.executeString( "Select anno_msg from assem_msg");
	        	
	        	if (annoMsg==null || annoMsg.equals("")) {
	        		annoMsg = annoCreate(ask);
	        		if (annoMsg == null) return false;
	        	}
	        	// create lines from anno_msg
    			String [] tok = annoMsg.split(Globals.tcwDelim);
    			for (int i=0; i<tok.length; i++) 
    				lines.add(tok[i]);	
        		lines.add("");
			}
			catch (Exception err) {
				ErrorReport.reportError(err,"Error: reading database for overview annoDBs");
				return false;
			}
			
			/** Always redo GO **/
			if (!annoGO(lines)) return false;
		}
		return true;
    }
    /*************************************************************
     * CAS326 create once and store
     */
    private String annoCreate(boolean ask) {
    	try {
    		String msg="";
    		Vector<String> tlines = new Vector <String> ();
    		if (!annoStats(tlines, ask)) return null;	
			if (!annoDatabases(tlines)) return null;
			if (!annoSpecies(tlines)) return null;
			
			for (String l : tlines) msg += l + Globals.tcwDelim;
			mDB.executeUpdate("update assem_msg set anno_msg='" + msg + "'");
			
    		return msg;
    	}
    	catch (Exception err) {
			ErrorReport.reportError(err,"Error: reading database for overview annoDBs");
			return null;
		}
    }
    /* make Annotation stat */
    private boolean annoStats(Vector<String> lines, boolean ask) {     				
		lines.add("   Hit Statistics:");
		 
        try {
		    int nCtgHits = mDB.executeCount( "SELECT count(*) FROM contig WHERE bestmatchid is not null" );
		    	
		    Out.prtSpCnt(1, nUniqueHits,"Unique hits");
		    
		    int nTotalHits = mDB.executeCount( "SELECT count(*) FROM pja_db_unitrans_hits ");
		   
		    if (nTotalHits==0 || nUniqueHits==0) {
			    lines.add("   Annotation not complete");
		        return true;
		    }
		    long sumLen = mDB.executeLong( "SELECT SUM(consensus_bases) from contig");
		    // the join makes it sum alignment_len for only the best PID, i.e. just one alignment_len per contig
		    long sumHit = mDB.executeLong( "SELECT SUM(alignment_len) FROM pja_db_unitrans_hits " +
		    			" JOIN contig on contig.PID=pja_db_unitrans_hits.PID");
		    if (!isProteinDB) sumHit = sumHit*3; 
		   
	        int [] just = {1, 0, 1, 1, 0, 1};
	        	rows = new String[40][6];
	        int r = 0, c=0;
	    
	        rows[r][c++] = "Sequences with hits";
	        rows[r][c++] = dff.format(nCtgHits);
	        rows[r][c++] = Out.perFtxtP(nCtgHits, numSeqs);
	        
	        rows[r][c++] = "   Bases covered by hit";
	        rows[r][c++] = dff.format(sumHit);
	        rows[r][c++] = Out.perFtxtP(sumHit, sumLen);
	         
	        r++; c=0;
	        rows[r][c++] = "Unique hits";
	        rows[r][c++] = dff.format(nUniqueHits);
	        rows[r][c++] = "";
	        
	        rows[r][c++] = "   Total bases";
	        rows[r][c++] = dff.format(sumLen);
	        rows[r][c++] = "";
	        
	        r++; c=0;
	        rows[r][c++] = "Total sequence hits";
	        rows[r][c++] = dff.format(nTotalHits);
	        rows[r][c++] = "";
	       
	        makeTable(just.length, r+1, null, just, lines); 
        }    
        catch ( Exception err ) {
        	ErrorReport.reportError(err,"Anno Stats");
        	return false;
        }
        return true;
    }
   
	 // ANNO DBS
    private boolean annoDatabases(Vector <String> lines) {	  
    	try {    			
	        Out.prtSp(1, "Processing each annoDB.....");
	        lines.add("   annoDBs (Annotation databases): " + nAnnoDBs + "   (see Legend below)");
	        
	   /* Loop through seq-hits for each annoDB creating stats */
	        int nDB = (nAnnoDBs+1);  // 1..nAnnoDB
	        double [] totSimSum = new double [nDB]; // divide by totPairs
	        double [] bestSimSum = new double [nDB]; // divide by hitSeq[i] 
	        int [] hitSeq = new int [nDB]; // percent of numSeqs
	        int [] cover1 = new int [nDB]; // percent of hitSeq[i] at COVER1
	        int [] cover2 = new int [nDB]; // percent of hitSeq[i] at COVER2
	        for (int i=0; i<nDB; i++) {
	        	totSimSum[i]=bestSimSum[i]=0.0;
	        	hitSeq[i]=cover1[i]=cover2[i]=0;
	        }
	        int maxIdx = mDB.executeCount("SELECT max(CTGID) FROM contig")+1;
	     	int [] noHit = new int [maxIdx];
	     	int badRank=0;
	     	
	        // CAS332 Instead of checking rank=1, I was checking for the first hit because
	        // some sequences end up with rank=2 due to duplicates. Still fixing...
	     
	     	for (int dbid=1; dbid < nDB; dbid++) {
	     		for (int i=0; i<maxIdx; i++) noHit[i]=0;
	     		
	     		ResultSet rs = mDB.executeQuery("select " +
		    	    	" seq.percent_id, seq.prot_cov, seq.blast_rank, seq.CTGID, seq.contigid " +
		    			" from pja_db_unitrans_hits as seq " +
		    			" right join pja_db_unique_hits as hit on seq.DUHID = hit.DUHID " +
		    			" WHERE hit.DBID=" + dbid +
		    			" order by seq.CTGID, seq.blast_rank");			
	     		
     			while (rs.next()) {
     				int i=1;
	    			int pSim = rs.getInt(i++);
	    			int hitCov = rs.getInt(i++); // CAS332 was prot_end-prot_start
	    			int rank = rs.getInt(i++);
	    			int idx = rs.getInt(i++);
	    			
	    			if (Globalx.debug && noHit[idx]==0 && rank==2) {
	    				noHit[idx]=2;
	    				badRank++;
	    				if (badRank<=3) Out.PrtWarn("First hit not rank=1 - #DB" + dbid + " " + rs.getString(i));
	    				if (badRank==3) Out.PrtSpMsg(1,"Further messages surpressed. The hit file and DB fasta file may be inconsistent.");
	    			}
	    			
	    			if (rank==1) { 
	    				noHit[idx]=1;
	    				hitSeq[dbid]++;
	    				if (pSim>=COVER1 && hitCov>=COVER1) {
	    					cover1[dbid]++;
	    					
	    					if (pSim>=COVER2 && hitCov>=COVER2) cover2[dbid]++;
	    				}
	    				bestSimSum[dbid] += (double) pSim;
	    			}
	    			totSimSum[dbid] += (double) pSim;
	    		}
	    		rs.close();
	    		if (Globalx.debug) {
	    			int cntNoHit=0;
	    			for (int x : noHit) if (x==2) cntNoHit++;
	    			Out.PrtSpCntMsg2(1, hitSeq[dbid], "Seqs with hits for DB#" + dbid, cntNoHit, "No Rank=1");
	    		}
	    		else Out.PrtSpCntMsg(1, hitSeq[dbid], "Seqs with hits for DB#" + dbid + "             ");
	     	}
	     	
	   /* Loop through annoDBs creating table */ // CAS327 change BEST HIT to Rank=1
	        String [] dfields =  {"ANNODB",  "ONLY", "BITS", "ANNO", "UNIQUE", "TOTAL", "AVG", "Rank", "HAS (%Seqs)", "AVG", "COVER", "COVER"};
	        String [] dfields2 = {"",          "",     "",    "",    "",        "",     "%SIM"," =1 ", "HIT        ", "%SIM", ">="+COVER1, ">="+COVER2};
	        int [] djust = {1,  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	        
	        ResultSet rs = mDB.executeQuery("SELECT DBID, dbtype, taxonomy, nUniqueHits, " +
	        		" nOnlyDB, nBestHits, nOVBestHits, nTotalHits FROM pja_databases");	
	       
	        int nCol = dfields.length;
	        rows = new String[nDB+1][nCol];
	        for (int i=0; i<nCol; i++) rows[0][i]=dfields2[i];
	        int r=1;     
	        
	        while(rs.next()) {
	        	String s, s2;
	        	int c=0, j;
	      
	        	int dbid = rs.getInt(1);
	        	
	        	s = rs.getString(2).toUpperCase(); 					  //dbtype
	        	s2 = rs.getString(3);	rows[r][c++] = s+"-"+s2; // taxonomy
	       	   
	        	j = rs.getInt(5);		rows[r][c++] = Out.kbFText(j);    //nOnlyDB
	        	j = rs.getInt(6);		rows[r][c++] = Out.kbFText(j);		//nBestHits
	        	j = rs.getInt(7);		rows[r][c++] = Out.kbFText(j);	   //nOVBesthits
    	        	
    	        j = rs.getInt(4);		rows[r][c++] = Out.kbFText(j);    //nUniqueHits
    	        
    	       
    	        int totPairs = rs.getInt(8);	rows[r][c++] = Out.kbFText(totPairs);  //total hit-seq pairs    	        
    	       	rows[r][c++] = Out.avg(totSimSum[dbid], totPairs);  
    	       	
    	        rows[r][c++] = " | "; // rank=1
    	        
    	       	int hseq = hitSeq[dbid];
    	      	rows[r][c++] =  Out.kbFText(hseq) + " " + Out.perFtxtP(hseq, numSeqs); 
     	  
    	        rows[r][c++] =  Out.avg(bestSimSum[dbid], hseq); 
    	        rows[r][c++] =  Out.perFtxt(cover1[dbid], hseq); 
    	        rows[r][c++] =  Out.perFtxt(cover2[dbid], hseq); 
        	    
	            r++;
	        }
	        rs.close();
	        
	        makeTable(nCol, r, dfields, djust, lines); // finish database
    	}    
        catch ( Exception err ) {
        	ErrorReport.reportError(err,"annoDBs");
        	return false;
        }
    	return true;
    }
   
 // SPECIES table
    private boolean annoSpecies(Vector <String> lines) {
    	try {
    	    int nSpecies = mDB.executeCount(  "SELECT count(*) FROM pja_db_species "); 
    	    if (nSpecies==0) return true;
    	    
    	 	lines.add("   Top " + NSPECIES + " species from total: "+ dff.format(nSpecies));
    	 	Out.prtSpCnt(1, nSpecies, "Species");
    	 	
    	    String [] sfields = {"SPECIES (25 char)", " BITS", " ANNO", "TOTAL", "","SPECIES", " BITS", " ANNO", "TOTAL"};
            int nGrp = 4;
        	    int nCol = sfields.length;
            int nRow = (int) ((float)(NSPECIES/2)+1.0);
            if (nSpecies < NSPECIES) nRow = (int) ((float)(nSpecies/2)+1.0);
            int [] justify2 = {1, 0, 0, 0, 1, 1, 0, 0, 0};
            rows = new String[nRow][nCol];
                	
        	ResultSet rset = mDB.executeQuery("SELECT species, count, nBestHits, nOVBestHits FROM pja_db_species " + 
  			      " ORDER BY nBestHits DESC, nOVBestHits DESC, count DESC");
    
        	int r = 0, c = 0, inc=0, oCnt=0, oBest=0, oAnno=0, ne=0, na=0, t=0;
        	while(rset.next()) {
        		String s = rset.getString(1);
        		if (s.length()>25) s = s.substring(0,25);
       
        		t = rset.getInt(2);
        		ne = rset.getInt(3);
        		na = rset.getInt(4);

        		if (r == nRow && inc == 0) {      			
        		    r = 0;
        		    inc = nGrp;
        		}
        		else if (r == nRow-1 && inc == nGrp){
    		    	oCnt+=t;
    		    	oBest+=ne;
    		    	oAnno+=na;
    		    	continue;
        		}
        		c=inc;
        		if (inc == nGrp) rows[r][c++] = " ";	        		
            	rows[r][c++] = s;	            	   		
        		rows[r][c++] = Out.kbFText(ne);
        		rows[r][c++] = Out.kbFText(na);	
        		rows[r][c++] = Out.kbFText(t);
        			
        		r++;        		
        	}	
        	rset.close();
        	
        	if (inc==nGrp && r==nRow-1) {
        		rows[r][nGrp]= " "; 
        		rows[r][nGrp+1]= "Other";
        		rows[r][nGrp+2] = Out.kbFText(oBest);
        		rows[r][nGrp+3] = Out.kbFText(oAnno);
        		
        		rows[r][nGrp+4] = Out.kbFText(oCnt);
    		}
        	else if (inc==0) {nRow = r; nCol=nGrp;}
        	      
        	makeTable(nCol, nRow, sfields, justify2, lines);
        	
			return true;
        }
        catch ( Exception err ) {
        	ErrorReport.reportError(err,"species");
        	return false;
        }
    }
    // GO table
    private boolean annoGO(Vector <String> lines) {
    	try {	
    	    if (!hasGO) {
    	    	lines.add("   Gene Ontology Statistics: none");
    	    	lines.add("");
    	    	return true;
    	    }
    		lines.add("   Gene Ontology Statistics:");
    		
		    int nCtgGOs=0, nCtgBestGOs=0, nHitGOs=0, nSlims=0;
		    String  slimSubset="";
	        
	        nCtgGOs = mDB.executeCount( "SELECT count(DISTINCT CTGID) FROM pja_unitrans_go" );        
	        
	        nCtgBestGOs = mDB.executeCount("SELECT count(*) FROM pja_db_unitrans_hits as tn, pja_db_unique_hits as uq, contig as ct " +
	        		" WHERE tn.DUHID = uq.DUHID AND tn.CTGID = ct.CTGID AND tn.filter_best=1 AND uq.goBrief <> '' ");
	        
	        nHitGOs = mDB.executeCount( "select count(*) from pja_db_unique_hits where goList!=''");
	        
	        nSlims = mDB.executeCount("select count(*) from go_info where slim=1");
	        if (nSlims>0) 
	        	slimSubset = mDB.executeString("select go_slim from assem_msg");
	        
	        int nBio=0, nMol=0, nCel=0, nISA=0, nPOF=0;
	        
	        nBio = mDB.executeCount("select count(*) from go_info where term_type='biological_process'");
	        nMol = mDB.executeCount("select count(*) from go_info where term_type='molecular_function'");
	        nCel = mDB.executeCount("select count(*) from go_info where term_type='cellular_component'");
	        
	        TreeMap <String, Integer> relType = MetaData.getGoRelTypes(mDB); // Case gets changed in further
	        int isa = (relType.containsKey("is_a")) ? relType.get("is_a") : 1; 
	        int po = (relType.containsKey("part_of")) ? relType.get("part_of") : 2;
	        nISA = mDB.executeCount("select count(*) from go_term2term where relationship_type_id=" + isa);
	        nPOF = mDB.executeCount("select count(*) from go_term2term where relationship_type_id=" + po);
	        
	        int [] just = {1, 0, 1, 1, 0, 1};
	        rows = new String[40][6];
	        int r = 0, c=0;
	      
	        rows[r][c++] = "Unique GOs";
            rows[r][c++] = dff.format(nUniqueGOs);
            rows[r][c++] = "";
            
            rows[r][c++] = "   Unique hits with GOs";
            rows[r][c++] = dff.format(nHitGOs);
            rows[r][c++] = Out.perFtxtP(nHitGOs, nUniqueHits);
            
            r++; c=0;
       		rows[r][c++] = "Sequences with GOs";
            rows[r][c++] = dff.format(nCtgGOs);
            rows[r][c++] = Out.perFtxtP(nCtgGOs, numSeqs);
            
            rows[r][c++] = "   Seq best hit has GOs";
            rows[r][c++] = dff.format(nCtgBestGOs);
            rows[r][c++] = Out.perFtxtP(nCtgBestGOs, numSeqs);
            
            r++; c=0;
            
            if (nSlims>0) {
            	rows[r][c++] = "Has " + slimSubset;
                rows[r][c++] = dff.format(nSlims);
                rows[r][c++] = "";
                
                rows[r][c++] = "";
                rows[r][c++] = "";
                rows[r][c++] = "";
                r++; c=0;
            }
            
            boolean isV318 = mDB.tableColumnExists("assem_msg", "go_rtypes");
    		
            r++; c=0; // blank line
            rows[r][c++] = "biological_process";
            rows[r][c++] = dff.format(nBio);
            rows[r][c++] = Out.perFtxtP(nBio, nUniqueGOs);
            
            if (isV318) {
	            rows[r][c++] = "   is_a";
	            rows[r][c++] = dff.format(nISA);
	            rows[r][c++] = "";
            }
            
            r++; c=0;
            rows[r][c++] = "molecular_function";
            rows[r][c++] = dff.format(nMol);
            rows[r][c++] = Out.perFtxtP(nMol, nUniqueGOs);
            
            if (isV318) {
	            rows[r][c++] = "   part_of";
	            rows[r][c++] = dff.format(nPOF);
	            rows[r][c++] = "";
            }
            
            r++; c=0;
            rows[r][c++] = "cellular_component";
            rows[r][c++] = dff.format(nCel);
            rows[r][c++] = Out.perFtxtP(nCel, nUniqueGOs);
            
	        makeTable(just.length, r+1, null, just, lines); 
           
    		return true;
    	}
    	catch ( Exception err ) {
        	ErrorReport.reportError(err,"GO annotation");
        	return false;
        }
    }
 
 /******************************************************
  * XXX Expression Section
  **/
    private boolean expSection(Vector<String> lines) 
    {
		lines.add( "EXPRESSION" );
		if (hasNorm==false) {
			lines.add("   None");
			lines.add("");
			return true;
		}	
		Out.prtSp(1, "Expression statistics....");
		if (!expStatsRPKM(lines)) return false;
    		
        if (hasSeqDE==false) return true;
        if (!expStatsDE((double)numSeqs, "Sequence", "contig", lines)) return false;
        
        if (!hasGODE) return true; 
        if (!expStatsDE((double) nUniqueGOs, "GO", "go_info", lines)) return false;      
        return true;
    }
    private boolean expStatsDE(double nTot, String title, String table, Vector<String> lines) {
        try {     
        	boolean isGO = table.contentEquals("go_info");
            String strQ=  "SELECT ";
            ResultSet rset = mDB.executeQuery("select pCol, goCutoff from libraryDE"); // CAS326 changed to use libraryDE
            Vector <String> rowLab = new Vector <String> ();
            while(rset.next()) {
                String col = rset.getString(1);
                double cut = rset.getDouble(2);
                if (!isGO || (isGO && cut>0.0)) {
                    if (rowLab.size() > 0) strQ += ",";
                    strQ +=  col;
                    rowLab.add(col);
                }
            }
            rset.close();
            strQ += " FROM " + table;
             
            if (title.equals("GO")) 
            	lines.add("   Gene ontology enrichment: (% of " + dff.format(nTot) + ")");
            else 
            	lines.add("   Differential expression:  (% of " + dff.format(nTot) + ")");
            
            double [] cuts =    {     1e-5,    1e-4,    0.001,   0.01,     0.05};
            String [] dfields = {"", "<1E-5", "<1E-4", "<0.001", "<0.01", "<0.05"};
            int [] djust =      {1,       0,       0,      0,       0,        0};
            
            int nCnt = cuts.length;
            int nRow = rowLab.size();
            int nCol = dfields.length;
            rows = new String[nRow+1][nCol];    
            int [][] cnts = new int [nRow][nCnt];
            for (int i=0; i<nRow; i++)
            		for (int j=0; j<nCnt; j++) cnts[i][j]=0;
            
            rset = mDB.executeQuery(strQ);
            while(rset.next()) {
                for (int i=0; i<nRow; i++) {
                    double de = rset.getDouble(rowLab.get(i)); // DE values for row
                    for (int j=0; j<cuts.length; j++) {
                        if (Math.abs(de) < cuts[j]) {
                        		cnts[i][j]++;
                        }
                    }
                }
            }
            rset.close();
            
            for (int i=0; i<nRow; i++) {
                rows[i][0] = rowLab.get(i).substring(2);
                for (int j=0; j<nCnt; j++) {
                    String x = Out.kbFText(cnts[i][j]);      
                    rows[i][j+1]= String.format("%s%5s", x, Out.perItxtP(cnts[i][j], (int) nTot));
                }
            }
            
            makeTable(nCol, nRow, dfields, djust, lines);
            return true;    
        }
        catch (Exception e) {
            ErrorReport.reportError(e,"reading database for DE");
            return false;
        }
    }
    private boolean expStatsRPKM(Vector<String> lines) {
        try {        
            String strQ=  "SELECT ";
            Vector <String> libCol = new  Vector <String> ();
            for (String l : libs) {
                String col = Globals.LIBRPKM + l;
                if (libCol.size() > 0) strQ += ",";
                strQ +=  col;
                libCol.add(col);
            }
            strQ += " FROM contig";
            
            String [] dfields = {"", "<2.0", "2-5", "5-10", "10-50", "50-100", "100-1k", "1k-5k", ">=5k"};
    	    int[] start=   {0, 2, 5,10, 50,100, 1000, 5000};
    	    int[] end=     {2, 5,10,50,100,1000,5000, 10000000};
            int [] djust = {1, 0, 0, 0, 0, 0, 0, 0, 0};
    	    		
            int nCol=start.length+1;
    	    int nrow = libs.size();
    	    rows = new String[nrow+1][nCol]; 
    	    		
    	    int [][] cnts = new int [nrow][start.length];
    	    ResultSet rs = mDB.executeQuery(strQ);
    	    while(rs.next()) {
                for (int i=0; i<nrow; i++) {
                    double ln = rs.getDouble(libCol.get(i));
                    for (int j=0; j<start.length; j++) {
                        if (ln >= start[j] && ln<end[j]) {
                        	cnts[i][j]++;
                        	break;
                        }
                    }
                }
            }
            rs.close();
        		
    		lines.add("   " +  normType + ": (% of " + dff.format(numSeqs) + ")");
    		for (int i=0; i<libs.size(); i++) {
                rows[i][0] = libs.get(i);
                for (int j=0; j<start.length; j++) {
                     String x = Out.kbFText(cnts[i][j]);      
                     rows[i][j+1]= String.format("%s%5s", x, Out.perItxtP(cnts[i][j], numSeqs));
                }
            }  
            makeTable(nCol, nrow, dfields, djust, lines);
    	   
            return true;    
        }
        catch (Exception e) {ErrorReport.reportError(e,"Computing DE");return false;}
    }
    /*
     * XXX make Assembly stats
     */
    private boolean seqSection (Vector<String> lines ) throws Exception
    {
    	if (!hasTranscripts) return true;
    	Out.prtSp(1, "Sequence statistics....");
    	
		lines.add("SEQUENCES");
		
		if (!hasNoAssembly) seqAssmStats(lines);
	 
	    seqLenTable(lines);
	    if (!isProteinDB) {
	    	seqQualsTable(lines);
	    	seqORFsAndGC(lines);
	    }
	    return true;
    }
    /* Assembly only */
    private void seqAssmStats(Vector<String> lines) throws Exception {
    	
    	lines.add("   Assembly Statistics:");
    	 
    	int [] djust = {1, 0};
 	    rows = new String[20][2];
 	    int r = 0;
 	    int nClones = mDB.executeCount ( 
 		        "SELECT COUNT(*) FROM contig JOIN contclone " +
 		        "ON contig.contigid=contclone.contigid");	 
 	    Out.prtSpCnt(1, nClones, "Sequestes");
 	    
 	    rows[r][0] = "Reads:";
 	    rows[r][1] = dff.format(nClones);
 	    r++;
 	    
    	if (hasBuried) {
    		int nBuried = mDB.executeCount(  "SELECT COUNT(*) FROM contig " +
    		        "JOIN contclone ON contig.contigid=contclone.contigid " +
    		        "WHERE buried=1");
    		rows[r][0] = "   Buried:";
    		rows[r][1] = dff.format(nBuried);
    		r++;
    	}
 		if (hasMatePairs) {
 		    int nMatePairs = mDB.executeCount(  "SELECT SUM(frpairs) " +
 				"FROM contig");
 				
 		    int nLoners = mDB.executeCount(  "SELECT SUM(est_loners) " +
 		        "FROM contig");

 		    rows[r][0] = "   Mate-pairs:";
 		    rows[r][1] = dff.format(nMatePairs);
 		    r++;
 		    rows[r][0] = "   Unmated 3' & 5':";
 		    rows[r][1] = dff.format(nLoners);
 		    r++;
 		}
 		
 	    int nMultContigs = mDB.executeCount(  "SELECT count(*) FROM contig "
 	            + "WHERE numclones>1" );
 	    int nSingletons = mDB.executeCount( "SELECT count(*) FROM contig " +
 	            "WHERE numclones=1" );
 	    int nContigs = nSingletons + nMultContigs;
 	    rows[r][0] = "Contigs:";
 	    rows[r][1] = dff.format(nContigs);
 	    r++;
 	    
 	    rows[r][0] = "   Multi-read:";
 	    rows[r][1] = dff.format(nMultContigs);
 	    r++;
 	    rows[r][0] = "   Singletons:";
 	    rows[r][1] = dff.format(nSingletons); 
 		r++;

 	    if (hasMatePairs) {		
 	        int mmp = mDB.executeCount(  "SELECT count(*) FROM contig "
 	            + "WHERE numclones>1 AND !(numclones=2 AND frpairs=1)" );
 	
 	        int jn = mDB.executeCount(
 	            "SELECT COUNT(*) FROM contig " + 
 	            	"WHERE numclones>1 AND " +
 	            "!(numclones=2 and frpairs=1) AND has_ns!=0" );
 	  
 	        int on = mDB.executeCount ( 
 	            "SELECT count(*) FROM contig " +
 	            "WHERE numclones=2 AND frpairs=1" );
 	
 	        int ojn = mDB.executeCount ("SELECT count(*) FROM contig " +
 	            "WHERE numclones=2 AND frpairs=1 AND has_ns!=0" );

 		    rows[r][0] = "   Multi mate-pairs:";
 		    rows[r][1] = dff.format(mmp);
 		    r++;
 		    rows[r][0] = "      joined by n's:";
 		    rows[r][1] = dff.format(jn);
 		    r++;
 		    rows[r][0] = "   One mate-pair:";
 		    rows[r][1] = dff.format(on);
 		    r++;
 		    rows[r][0] = "      joined by n's:";
 		    rows[r][1] = dff.format(ojn); 
 		    r++;
 	    }
 	   
 		makeTable(2, r, null, djust, lines);
   
	    String [] dfields = {"=2", "3-5", "6-10", "11-20", "21-50", "51-100", "101-1k", ">1k"};
	    int[] start= {1,2, 5,10,20, 50, 100,1001};
	    int[] end=   {2,5,10,20,50,100,1000,100000000};
        int [] djust2 = {0, 0, 0, 0, 0, 0, 0, 0};
	    int n=start.length;
	    rows = new String[3][n+1];
	    
	    try {
	    	int c=0;
		    for (int i = 0; i<n; i++) {
		    	 int cnt = mDB.executeCount (  "SELECT count(*) FROM contig " +
		   	          "WHERE numclones > " + start[i] + " AND numclones <= " + end[i]);
		         rows[0][i] = Out.kbFText(cnt);
		         c++;
		    }
		    lines.add("   Contig Counts: ");
		    makeTable(c, 1, dfields, djust2, lines);
	    }
	    catch (Exception err) {ErrorReport.reportError(err,"reading database for overview len table");}
    }
    /* Quals */
    private void seqQualsTable(Vector<String> lines) {
		try {
			int cntN1 = mDB.executeCount( "SELECT count(*) FROM contig " +
		    		"WHERE cnt_ns>0" );
			if (cntN1==0) return;
			
		    int cntN10 = mDB.executeCount( "SELECT count(*) FROM contig " +
		    		"WHERE cnt_ns>10" );
		    int cntN100 = mDB.executeCount( "SELECT count(*) FROM contig " +
		    		"WHERE cnt_ns>100" );
		    int cntN1000 = mDB.executeCount( "SELECT count(*) FROM contig " +
		    		"WHERE cnt_ns>1000" );
		   
			lines.add("   Quality:");
			int [] djust = {1, 0, 1};
    	    rows = new String[20][3];
    	    int r = 0;
    	    
    		rows[r][0] = "Sequences with #n>0:";
 	        rows[r][1] = dff.format(cntN1);
 	        rows[r][2] = Out.perFtxtP(cntN1, numSeqs);
 	        r++;
 	        rows[r][0] = "Sequences with #n>10:";
 	        rows[r][1] = dff.format(cntN10);
 	        rows[r][2] = Out.perFtxtP(cntN10, numSeqs);
 	        r++;
 	        if (cntN100>0) {
 	        	rows[r][0] = "Sequences with #n>100:";
	 	        rows[r][1] = dff.format(cntN100);
	 	        rows[r][2] = Out.perFtxtP(cntN100, numSeqs);
	 	        r++;
 	        }
 	       if (cntN1000>0) {
	        	rows[r][0] = "Sequences with #n>1000:";
	 	        rows[r][1] = dff.format(cntN1000);
	 	        rows[r][2] = Out.perFtxtP(cntN1000, numSeqs);
	 	        r++;
 	       }
	         
 	       makeTable(3, r, null, djust, lines);
    	}
    	catch (Exception err) {ErrorReport.reportError(err,"reading database for overview quality info");}
    }
    /* Sequence lengths */
    private void seqLenTable(Vector<String> lines) {
        String [] dfields = 
		{"<=100", "101-500", "501-1000", "1001-2000", "2001-3000", "3001-4000", "4001-5000", ">5000"};
        int [] djust = {0, 0, 0, 0, 0, 0, 0, 0};
		
	    int[] start= {0,  100, 500, 1000,2000, 3000,4000,5000};
	    int[] end =  {100,500, 1000,2000,3000, 4000,5000,100000000};
	    int n = start.length;
	    rows = new String[3][n+1];
	    int cnt=0;
	    try {
		    for (int i = 0; i<n; i++) {
		       int cntLen = mDB.executeCount (  "SELECT count(*) FROM contig " +
		         "WHERE consensus_bases > " + start[i] + " AND consensus_bases <= " + end[i]);
		        rows[0][i] = Out.kbFText(cntLen) + Out.perItxtP(cntLen, numSeqs);
		        cnt += cntLen;
		    }
		    if (!isProteinDB) lines.add("   Sequence lengths:");
		    else             lines.add("   Protein lengths:");
		    makeTable(n, 1, dfields, djust, lines);
		    if (cnt!=numSeqs) Out.PrtErr("Incorrect sequence lengths count " + cnt);
	    }
	    catch (Exception err) {
			ErrorReport.reportError(err,"Error: reading database for overview len table");
			return;
	    }	
    }
    /* seqORFsAndGC*/
    private void seqORFsAndGC(Vector<String> lines) {
    	if (!hasAnno) return;
        String [] dfields = 
		{"<=100", "101-500", "501-1000", "1001-2000", "2001-3000", "3001-4000", "4001-5000", ">5000"};
        int [] djust = {0, 0, 0, 0, 0, 0, 0, 0};
		
	    int[] start= {0,  100, 500, 1000,2000, 3000,4000,5000};
	    int[] end =  {100,500, 1000,2000,3000, 4000,5000,100000000};
	    int[] cnt =  {0,  0,   0,   0,   0,    0,   0,   0};
	    int n = start.length;
	    rows = new String[3][n+1];
	
	    try {
    		ResultSet rset = mDB.executeQuery("Select o_coding_start, o_coding_end from contig");
    		while (rset.next()) {
    			int l = rset.getInt(2)-rset.getInt(1)+1;
    			for (int i = 0; i<n; i++) {
    				if (l>start[i] && l <= end[i]) {
    					cnt[i]++;
    					break;
    				}
    			}
    		}
    		rset.close();
	    		
		    for (int i = 0; i<n; i++) {
		        rows[0][i] = Out.kbFText(cnt[i]) + Out.perItxtP(cnt[i], numSeqs);;
		    }
		    lines.add("   ORF lengths:");
		    makeTable(n, 1, dfields, djust, lines);
	    }
	    catch (Exception err) {
			ErrorReport.reportError(err," reading database for overview len table");
			return;
	    }	
   
        try {
            if (isProteinDB) return;
            
            if (!mDB.tableColumnExists("assem_msg", "gc_msg")) {
            		lines.add("    No ORF or GC information");
            		lines.add("");
            		return;
            }
            
            // compute ORF overview with GC in DoORF
	    	String gcMsg = mDB.executeString("select gc_msg from assem_msg");
	    	if (gcMsg==null || gcMsg=="") {
	    		lines.add("    No ORF or GC information");
	    		lines.add("");
	    		return;
	    	}
		    	
	        String [] gc = gcMsg.split("\n");
	        for (String l : gc) lines.add(l);
	        
	        return;
        }
        catch ( Exception err ) {
	    	ErrorReport.reportError(err, "Error: processing data for annotation header");
			hasAnno = false;
			lines.add("   execAnno encountered error" );
			return;
     	} 	
    }
    /**************************************************************
     * Other
     */
    private boolean otherSection (Vector<String> lines ) throws Exception
    {
    	if (!otherPair(lines)) return false;
    	if (!otherLoc(lines)) return false;
    	
    	return true;
    }
    // Pairs
    private boolean otherPair (Vector<String> lines ) throws Exception
    {
		if (!hasAnno) return true;
		if (!hasPairwise) return true;
			
		Out.prtSp(1, "Similarity....");
		
        int [] djust = {1, 0};
        rows = new String[20][2];
        int r = 0;
        	    
        try {
	        int nPairs = mDB.executeCount( "SELECT COUNT(*) FROM pja_pairwise");     
	        
	        lines.add("   Similar pairs: " + nPairs);
	        
        	String msg = mDB.executeString( "SELECT pair_msg from assem_msg"); // CAS314
        	if (msg==null || msg.contentEquals("")) return true; // CAS317
        	
        	String [] tok = msg.split("::");
        	int cntAA=0, cntNT=0, cntORF=0;
        	
        	if (tok.length>=3) { // can be a DP on end
        		String x = tok[0].split(" ")[1];
            	cntAA = Integer.parseInt(x);
            	x = tok[1].split(" ")[1];
            	cntORF = Integer.parseInt(x);
            	x = tok[2].split(" ")[1];
            	cntNT = Integer.parseInt(x);
        	}	
    		
    		if (cntNT>0) { 
    			rows[r][0] = "Nucleotide ";
    			rows[r][1] = dff.format(cntNT);
    			r++;
    		}
    		if (cntAA>0) {
    			rows[r][0] = "Translated nucleotide";
    			rows[r][1] = dff.format(cntAA);
    			r++;
    		}
    		if (cntORF>0) {
    			rows[r][0] = "Translated ORFs";
    			rows[r][1] = dff.format(cntORF);
    			r++;
    		}
        	makeTable(2, r, null, djust, lines);
    		return true;
        }
        catch (SQLException err) {
			ErrorReport.reportError(err,"reading database for overview pairwise");
			return false;
		}
     }
    /*********************************************************
     * Locations
     */
    private boolean otherLoc (Vector<String> lines ) throws Exception
    {
		try {
			if (!mDB.tableColumnExists("assem_msg", "hasLoc")) return true;
			
			int cntLoc = mDB.executeCount("select count(*) from contig where seq_end>0");
			if (cntLoc==0) return true;
			
			Out.prtSp(1, "Locations....");
			
			lines.add("LOCATIONS");
			
			String [] dfields = {"1", "2", "3-4", "5-7", "8-10", "11-20", "21-30", ">30"};
		    int[] start= {1,2, 3,5,8, 11, 21,31};
		    int[] end=   {2,3, 5,8,11,21, 31,100000000};
	        int [] djust = {0, 0, 0, 0, 0, 0, 0, 0};
	        int [] cnt = {0,0,0,0,0,0,0,0};
		    int n=start.length;
		    rows = new String[3][n+1];
		    
		    HashMap <String, Integer> seqMap = new HashMap <String, Integer> ();
		    HashMap <String, Integer> seqMapCnt = new HashMap <String, Integer> ();
		    int cnt_pos=0, cnt_neg=0, total=0;
		    ResultSet rs = mDB.executeQuery("select seq_group, seq_strand, seq_start, seq_end from contig" +
		    		" where seq_end>0");
		    while (rs.next()) {
	    		total++;
	    		String grp = rs.getString(1);
	    		String strand = rs.getString(2);
	    		
	    		if (seqMap.containsKey(grp)) seqMap.put(grp, seqMap.get(grp)+1);
	    		else seqMap.put(grp, 1);
	    		
	    		if (strand.equals("-")) cnt_neg++; else cnt_pos++;
	    		
	    		String key = grp + ":" + rs.getString(3) + ":" + rs.getString(4) + strand;
	    		if (seqMapCnt.containsKey(key)) seqMapCnt.put(key, seqMapCnt.get(key)+1);
	    		else seqMapCnt.put(key, 1);
		    }
		    rs.close();
		    
		    for (String g : seqMap.keySet()) {
	    		int num = seqMap.get(g);
	    		for (int i=0; i< n; i++) {
	    			if (num < end[i]) {
	    				cnt[i]++;
	    				break;
	    			}
	    		}
		    }
		    for (int i=0; i<n; i++) rows[0][i] = dff.format(cnt[i]);
			
		    lines.add("   Sequences with location:       " + String.format("%5d",total) +
		    			    "  unique locations: " + seqMapCnt.size());
		    lines.add("   Sequences on positive strand:  " + String.format("%5d",cnt_pos) + 
		    				"  negative strand:  " + cnt_neg);
		    lines.add("   Sequences per group:");
		    makeTable(n, 1, dfields, djust, lines);   
		}
		catch (Exception err) {
			ErrorReport.reportError(err," reading database for gene location information");
			return false;
		}
		return true;
    }
   
	/************************************************************
	 * XXX AnnoDBs & Legend
	 */
    private boolean finalAnnoDBs(Vector<String> lines) 
    {     
    	lines.add("-------------------------------------------------------------------");
    	lines.add("PROCESSING INFORMATION:");
		lines.add("   AnnoDB Files:");
		 
        try {
    		if (hasDBhitData && hasAnno) {
		        int nDBs = mDB.executeCount ( "SELECT COUNT(*) FROM pja_databases");  
		        String [] dfields = 
		        			{"Type", "Taxo", "FILE", "DB DATE", "ADD DATE", "EXECUTE"};
		        int [] djust = {1, 1, 1, 1, 1, 1};
	
		        String strQ = "SELECT dbtype, taxonomy, path, dbdate, addDate, parameters" +
		        					" FROM pja_databases";	
		        	        
		        int nCol = dfields.length;
		        rows = new String[nDBs][nCol];
		        int r=0;     
		        ResultSet rset = mDB.executeQuery(strQ);
			        
		        while(rset.next()) {
		        	String s;
		        	int i=0;
		        	
		        	s = rset.getString("dbtype");
		        	rows[r][i++] = s;
		        	s = rset.getString("taxonomy");
		        	rows[r][i++] = s;
		        	
		        	s = rset.getString("path");
		        	int last = s.lastIndexOf("/");
	        		rows[r][i++] = s.substring(last+1);
		        	
	        		s = rset.getString("dbDate");
	        		rows[r][i++] = TimeHelpers.convertDate(s);        		
	        		s = rset.getString("addDate");
	        		rows[r][i++] = TimeHelpers.convertDate(s);
	        		
	        		s = rset.getString("parameters");
	        		if (s==null) s="unknown";
	        		else {
	        			s = zRemoveSubStr(s, "--tmpdir tmpDmnd");
	        			s = zRemoveSubStr(s, "-t tmpDmnd");
	        			s = zRemoveSubStr(s, "--compress 0");
	        		}
	        		rows[r][i++] = s;
	            		
		            r++;
		        }
		        rset.close();
		        
		        makeTable(nCol, r, dfields, djust, lines); // finish database  
		        
		        // PRUNE
		        if (mDB.tableColumnExists("assem_msg", "prune")) {
	    			int prune = mDB.executeInteger( "Select prune from assem_msg");
	    			if (prune<=0) lines.add("   Prune: none"); 
	    			else if (prune==1) lines.add("   Prune: same alignment");
	    			else if (prune==2) lines.add("   Prune: same description");
		        }
		        lines.add("");
	    	} 
        		// GO 
    		if (mDB.tableColumnExists("assem_msg", "go_msg")) {
    			String go = mDB.executeString( "Select go_msg from assem_msg");
    			if (go!=null && !go.equals("")) {
    				if (go.endsWith(".tar.gz")) go = go.replace(".tar.gz", "");
    				lines.add("   Gene Ontology: " + go); // CAS318 had 'Over-represented'
    				
    				if (mDB.tableColumnExists("assem_msg", "go_slim")) { 
        				String slim = mDB.executeString( "Select go_slim from assem_msg");
            			if (slim!=null && !slim.equals("")) {
            				lines.add("   GO Slim: " + slim);
            			}
            		}
        			lines.add("");
    			}
    		}
        	
	        // ORF
	        if (!isProteinDB && mDB.tableColumnExists("assem_msg", "orf_msg")) {
        		String orf = mDB.executeString( "Select orf_msg from assem_msg");
        		if (orf!=null && !orf.equals("")) {
	        		lines.add("   ORF finder:"); // use directly what DoORFs created
        			String [] tok = orf.split(Globals.tcwDelim);
        			for (int i=0; i<tok.length; i++) {
        				lines.add("      " + tok[i]);	
        			}
	        		lines.add("");
        		}
	        }
	        
	        // DE
	        if (!hasSeqDE) return false;
    		lines.add("   Differential Expression computation: ");
    		String msg = String.format("      %-12s %-30s %s", "Column",  "Method", "Conditions");
    		lines.add(msg);
    		
    		ResultSet rs = mDB.executeQuery("Select pCol, title, method from libraryDE");
    		while (rs.next()) {
    			String de = rs.getString(1).substring(2); // remove P_
    			msg = String.format("      %-12s %-30s %s", de, rs.getString(3), rs.getString(2));
        		lines.add(msg);
    		}
    		rs.close();
    		lines.add("");
	        
    		// GO DE
	        if (!hasGO) return true;
    		lines.add("   GO enrichment computation: ");
    		msg = String.format("      %-12s %-30s %-5s", "Column", "Method", "Cutoff");
    		
    		int cnt=0;
    		rs = mDB.executeQuery("Select pCol, goCutoff, goMethod from libraryDE where goCutoff>0.0");
    		while (rs.next()) {
    			if (cnt==0) lines.add(msg);
    			String de = rs.getString(1).substring(2); // remove P_
				msg = String.format("      %-12s %-30s %.1e", de, rs.getString(3).trim(), rs.getDouble(2));
				lines.add(msg);
				cnt++;
    		}
    		rs.close();
    		if (cnt==0) lines.add("     Not computed");
    		lines.add("");
        
    		return true;
        }
        catch ( Exception err ) {
        	ErrorReport.reportError(err,"processing data for overview annotation");
        	return false;
        }
    }
	private void finalLegend(Vector<String> lines) {    
		if (nUniqueHits==0) return;
    	lines.add("-------------------------------------------------------------------");
    	lines.add("LEGEND:");
        lines.add("   annoDB:");
        lines.add("      ANNODB    is DBTYPE-TAXO, which is the DBtype and taxonomy");
        lines.add("      ONLY      #Seqs that hit the annoDB and no others");
        lines.add("      BITS      #Seqs with the overall best bitscore from the annoDB");
        lines.add("      ANNO      #Seqs with the overall best annotation from the annoDB ");
        lines.add("      UNIQUE    #Unique hits to the annoDB");
        lines.add("      TOTAL     #Total seq-hit pairs for the annoDB");
        lines.add("      AVG %SIM  Average percent similarity of the total seq-hit pairs");
        lines.add("      HIT-SEQ   Percent of #Seqs that have at least one hit from the annoDB");
        lines.add("      BEST HIT  The following columns refer to the best hit (Rank=1):");
        lines.add("         AVG %SIM  Average percent similarity of the best hit seq-hit pairs");
        lines.add("         Cover>=N  Percent of HIT-SEQ where the best hit has similarity>=N% and hit coverage>=N%");
        lines.add("");
        lines.add("   #Seqs is listed at top of overview");
        lines.add("   Best Annotation:");
      	lines.add("      Descriptions may not contain words such as 'uncharacterized protein'");
      	try { 
      		boolean subset = mDB.executeBoolean("select spAnno from assem_msg");
      		if (subset) 
      		  lines.add("      Precedence is given to SwissProt hits since they are manually curated");
      	} catch (Exception e) {ErrorReport.prtReport(e, "Reading subset column from database table");}
    
    }
	  
    /*
     * setflags used in overview calculation 
     * there is no way to tell the difference between a single library with no expressions,
     * and an assembled library.
     * 				exploaded   ctglib   orig_libid   parent
     * translib		1			1		 
     * explib		0			0		 name		 parent
     * assmlib		0			0		 name
     * singlib		0			0		 name				# don't want L_ columns, but added before Instantiate
     */
	private boolean setFlags() {
	   try {   
		   numSeqs = mDB.executeCount( "SELECT COUNT(*) FROM contig"); 			
		   if (numSeqs == 0) return false;
		  
		   // If all contigs == 1, then it was probably instantiated
		   int num = mDB.executeCount( "SELECT COUNT(*) FROM contig WHERE numclones > 1"); 	
		   
		   if (num==0) hasNoAssembly = true;
		   else {	
			   num = mDB.executeCount( "SELECT COUNT(*) FROM buryclone LIMIT 1");
			   if (num > 0) hasBuried = true;
			   
			   // has n's and buried ?
			   num = mDB.executeCount( "SELECT COUNT(*) FROM contig WHERE frpairs > 1 LIMIT 1"); 			
			   if (num > 0) hasMatePairs = true;
		   }
          
		   // DB hits ?
		   int nDBHits = mDB.executeCount (  "SELECT count(*) FROM contig WHERE bestmatchid is not null" );
  	       if (nDBHits > 0) hasDBhitData = true;
  	        
  	       nAnnoDBs = mDB.executeCount (  "SELECT count(*) FROM pja_databases ");
  	       nUniqueHits = mDB.executeCount( "SELECT count(*) FROM pja_db_unique_hits ");
  	       
  	       // pairs ?
		   int nPairs = mDB.executeCount( "SELECT COUNT(*) FROM pja_pairwise");
  	       if (nPairs > 0) hasPairwise = true;	          
  	        	
  	       //Peptide db
  	       isProteinDB = mDB.tableColumnExists("assem_msg", "peptide");
 
  	       // Expression libs; a single unassemblied library has ctglib=0 and 
  	       ResultSet rset = mDB.executeQuery("Select libid  from library where ctglib=0");
  	       while (rset.next()) libs.add(rset.getString(1));
  	       rset.close();
  	       
  	       if (libs.size()>0) {
	  	       	Vector <String> del = new Vector <String> ();
	  	       	for (String lib : libs) {
  	    	   		String col = Globalx.LIBCNT + lib;
  	    	   		int cnt = mDB.executeCount( "Select count(*) from contig where " + col + ">1");
  	    	   		if (cnt==0) del.add(lib);
	  	       	}
	  	       	// Seriously kludgy. There is no way to tell the difference between
	  	       	// a library of assembled sequences and unassembled library with no exp levels
	  	       	// that caused an error in chkLibExpLevel in setUIForGroupFromFieldsForLibrary in FieldTab
  	       		for (String lib : del) {
  	       			libs.remove(lib);
  	       			//stmt.executeUpdate("update library set ctglib=1 where libid='" + lib + "'" );
	   		   }
  	       }
  	       if (libs.size()>0) {
  	    	   hasNorm=true;
  	    	   if (mDB.tableColumnExists("assem_msg", "norm"))  // CAS304
  	    		   normType = mDB.executeString("select norm from assem_msg");
  	    	   else 
  	    		 normType="RPKM";
  	       }
  	       
  	       // String prefix = QRFrame.pValColPrefix;
  	       hasSeqDE=hasGODE=false;
           rset = mDB.executeQuery("SHOW COLUMNS FROM contig");
           while(rset.next()) {
               String col = rset.getString(1);
               if(col.startsWith(Globals.PVALUE)) hasSeqDE=true;
           }
           
           if (mDB.tableExists("go_info")) { // CAS319 318 bug had go_tree
               hasGO = true;
           
               nUniqueGOs = mDB.executeCount( "SELECT count(*) FROM go_info ");
               
               rset = mDB.executeQuery("SHOW COLUMNS FROM go_info");
               while(rset.next()) {
                   String col = rset.getString(1);
                   if(col.startsWith(Globals.PVALUE)) {
                       hasGODE=true;
                       break;
                   }
               }
           }
           else hasGO = false;
  	       rset.close();
  	       return true;
	   }
	   catch (Exception err) {
			ErrorReport.reportError(err,"Error reading database");
			return false;
	   }
	}
	// HTML
	 private void writeHTML(String text) {
		try {
			if (strAssmID==null) return; // CASz 10oct19
			String file=strAssmID + ".html";
			if (new File("./projects").exists()) {
				File h = new File("./projects/" + Globalx.HTMLDIR);
				if (!h.exists()) {
					System.out.println("Creating directory projects/" + Globalx.HTMLDIR);
					h.mkdir();
				}
				if (h.exists()) file = "./projects/"  + Globalx.HTMLDIR + "/" + file;
			}
			Out.prtSp(1, "Writing overview HTML file: " + file);
			FileOutputStream out = new FileOutputStream(file);
			PrintWriter fileObj = new PrintWriter(out); 
			fileObj.println("<html>");
			fileObj.println("<title>" + strAssmID + " overview</title>");
			fileObj.println("<body>");
			fileObj.println("<center>");
			fileObj.println("<h2>TCW overview for " + strAssmID + " </h2>");
			fileObj.println("<table width=700 border=1><tr><td>");
			fileObj.println("<pre>");
    		fileObj.println(text);
    		fileObj.println("</pre>");
    		
    		fileObj.println("</body>");
    		fileObj.println("</html>");
    		fileObj.close();
		}
		catch (Exception err) {ErrorReport.reportError(err," writing HTML of overview");}
    }
    ////////////////////////////////////////////////////////////////////////
    // XXX -- only outputs columns that have values in at least one row
	private void makeTable(int nCol, int nRow, String[] fields, int [] justify, Vector<String> lines)
	{
		int c, r;
		String line;
		String space = "  ";
		
		// compute column lengths
		int [] collen = new int [nCol];
		for (c=0; c < nCol; c++) collen[c] = 0;
		
        for (c=0; c< nCol; c++) {
            for (r=0; r<nRow; r++) {
	        		if (rows[r][c] == null) rows[r][c] = "";
	        		if (rows[r][c].length() > collen[c]) 
	        			collen[c] = rows[r][c].length();
            }
        }
        if (fields != null) {
			for (c=0; c < nCol; c++) {
				if (collen[c] > 0) {
					if (fields[c].length() > collen[c]) 
						collen[c]=fields[c].length();
				}
			}
	        // output headings
	        line = "      ";
	        for (c=0; c< nCol; c++) 
	        	if (collen[c] > 0) 
	        		line += pad(fields[c],collen[c],justify[c]) + space; 
	        lines.add(line);
        }
        // output rows
        for (r=0; r<nRow; r++) {
        		line = "      ";
            for (c=0; c<nCol; c++) {
                 if (collen[c] > 0) 
                	 	line += pad(rows[r][c],collen[c],justify[c]) + space;
                 rows[r][c] = ""; // so wouldn't reuse in next table
            }
            lines.add(line);
        }
        lines.add("");
	}
	
    private static String pad(String s, int width, int o){
		if (s == null) return " ";
        if (s.length() > width) {
            String t = s.substring(0, width-1);
            Out.prt("'" + s + "' truncated to '" + t + "'");
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
    
    /**
     *  Utility routines
     */
    private String zRemoveSubStr(String x, String sub) {
  		if (!x.contains(sub)) return x;
  		
  		int index = x.indexOf(sub);
  		if (index + sub.length() == x.length()) return x.substring(0, index);
  		
  		return x.substring(0, index) + x.substring(index+sub.length());
    }
	
	private String strAssmID = "";
	private boolean hasAnno = true, hasTranscripts=true;; 
	// set flags
	private int numSeqs = 0, nUniqueHits=0, nUniqueGOs=0, nAnnoDBs=0;
    private boolean hasMatePairs=false, hasBuried=false;
    private boolean hasNoAssembly = false, hasGO;
    private boolean hasDBhitData = false, hasPairwise=false;
    private boolean isProteinDB = false, hasNorm=false, hasSeqDE=false, hasGODE=false;
    private Vector <String> libs = new Vector <String> ();
    private String normType="";

	private DBConn mDB = null;
	private String [][] rows = null;
}
