package sng.runAS;

import java.io.BufferedReader;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Stack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.file.FileHelpers;
import util.methods.TimeHelpers;
import util.ui.UserPrompt;
import util.methods.ErrorReport;
import util.methods.Out;

/***************************************************
 * Create MySQL GOdb from GO OBO file - CAS318 finally changed over from the mysql GO
 * 1. download file
 * 2. create database and load file
 * 3. load UniProt
 * 4. create levels - rewrote without go_tree, and the time dropped from 3m to 15s
 * 5. create graph_path
 * Everything in the rest of sTCW will work exactly the same as with the original mysql GO
 * 
 * Relations: is_a, part_of, alt_id
 *   DoOBO: The relations are put into term as the first three rows, so their indexes are used in term2term
 *   sng.annotator: DoGO: the indexes are not put in the term table, but stored in assem_msg.go_rtypes. 
 *   sng.database.Metadata: reads them from assem_msg.go_rtypes.
 *   It is expected that: is_a:1 part_of:2 alt_id:3 -- where the relation (e.g. is_a) are hardcoded in places
 */
public class DoOBO {
	private static final String goURL = GlobalAS.goURL; 	// "http://current.geneontology.org/ontology/";
	private static final String goFile = GlobalAS.goFile; 	// "go-basic.obo";
	
	private static final String datSuffix = GlobalAS.datSuffix, datGzSuffix=GlobalAS.datGzSuffix;

	private static final int ISA = 1;
	private static final int PARTOF = 2;
	private static final int ALTID = 3; // CAS320 changed from replaced_by
	
	private static final String goMetaTable = Globalx.goMetaTable; // CAS318 was prefixed with PAVE_
	private static final String goUpTable =   Globalx.goUpTable;
	
	public DoOBO(ASFrame asf) { 
		frameObj = asf;
		hostObj = new HostsCfg(); // Gets mysql user/pass and checks that mySQL can be accessed
	}
	/******************************************************
	// upPath - to UniProt directory
	// goPath - to GO directory
	// godb - name of GOdb
	// hasGOfile - don't need to download GO obo file
	 */
	public void run(String upPath, String goPath, String godb, boolean hasGOfile) {
		Out.PrtDateMsg("\nStart GO processing " + godb);
		long startTime = Out.getTime();
		
		upDir = upPath;
		goDir =   goPath;
		goDBname = godb;
	
		Out.PrtSpMsg(1, "UniProt directory: " + upPath);
		Out.PrtSpMsg(1, "GO temporary directory: " + goDir);
			
		try {
			if (!goDBdelete()) return;
			
			if (!hasGOfile) {
				Out.PrtSpMsg(1, "URL: " + goURL);
				if (!goDirMake()) return;
				if (!goOBOdownload()) return;
			}
			else Out.PrtSpMsg(1, "Use existing GO file");
			
			if (!goDBcreate()) return;
			
			if (!loadGOdbFromOBO()) return;
			
			if (!loadGOdbfromUniProt(upDir)) return;
			
			if (!computeLevels()) return;
			
			if (!computeAncestors()) return;
		}
		catch (Exception e) {
			ErrorReport.prtReport(e, "Creating GO database");
		}
		finally {
			if (goDB!=null) goDB.close();
		}	
		Out.PrtSpMsgTimeMem(0, "Complete creating GO database " + goDBname, startTime);
	}
	/**********************************
	 * Return:
	 * 0 - does not exist
	 * 1 - exists but not TCW modified
	 * 2 - exists and complete
	 */
	public int goDBcheck(String goname)
	{	int x = 0;
		try {
			boolean rc = DBConn.checkMysqlDB("", hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!rc) return 0; 
			
			DBConn goDB = new DBConn(hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!goDB.tableExists(goUpTable)) x=1; // gets added in modfiyGOdb
			else x=2; 
			
			goDB.close();
			return x;
		}
		catch(Exception e) {Out.PrtErr("Could not check GO database " + goDBname);}
		return 0;
	}
	public int goDBcheckPrtUPs(String goname)
	{	int x = 0;
		try {
			boolean rc = DBConn.checkMysqlDB("", hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!rc) return 0; 
			
			goDB = new DBConn(hostObj.host(), goname, hostObj.user(), hostObj.pass());
			if (!goDB.tableExists(goUpTable)) x=1; // gets added in modfiyGOdb
			else x=2; 
			
			if (x==2) { // CAS343
				if (goDB.tableColumnExists("TCW_metadata", "upDBs")) {
					String dbs = goDB.executeString("select upDBs from TCW_metadata");
					Out.prt("UniProts: " + dbs);
				}
				else {
					if (UserPrompt.showYesNo("Show UniProts", 
							"Would you like to see the UniProts in " + goname)) {
						Out.prt("+++ This will take a minute, but will be saved in the database for next time");
						Out.prt("    If your goDB is very large, it can take a few minutes. No output to terminal.");
						
						String msg = computeUPdbs(); 
						if (msg!=null) Out.prt(msg);
					}
				}
			}
			goDB.close();
			return x;
		}
		catch(Exception e) {Out.PrtErr("Could not check GO database " + goDBname);}
		return 0;
	}
	/***************************************************
	 * Initial methods - only do if not done
	 */
	private boolean goDBdelete() {
		try {
			int rc = goDBcheck(goDBname);
			if (rc==0) return true;
			
			Out.PrtSpMsg(1, "Delete mySQL database " + goDBname);
			DBConn.deleteMysqlDB(hostObj.host(), goDBname, hostObj.user(), hostObj.pass());
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Delete GO database: " + goDBname);}
		return false;
	}
	
	private boolean goDirMake() {
		try {
			File d = new File(goDir);
			if (d.exists() ) return true;
			
			Out.PrtSpMsg(1, "Create " + goDir);
			if (d.mkdir()) return true;
		    		
			JOptionPane.showMessageDialog(frameObj,"Could not create directory: " + goDir + "\n"); 
		    Out.PrtErr("Could not create directory: " + goDir);
		}
    	catch(Exception e){ErrorReport.reportError(e, "Creating GO directory: " + goDir);}
		return false;
	}
	
	private boolean goOBOdownload() {
		String goPath = goDir+ "/" + goFile;
		String msg = "Could not download: " + goURL +" " +  goFile + " to " + goPath;
		
		try {
			if (new File(goPath).exists()) return true;
			
			if (ASMain.ioURL(goURL, goFile, goPath)) return true;
			
			JOptionPane.showMessageDialog(frameObj, msg + "\n"); 
	    	Out.PrtErr(msg);
		}
    	catch(Exception e){ErrorReport.reportError(e, msg);}
		return false;
	}
	/******************************************************************
	 * Create database and add schema
	 */
	private boolean goDBcreate() {
		try {
			DBConn.createMysqlDB(hostObj.host(), goDBname, hostObj.user(), hostObj.pass());
			goDB = new DBConn(hostObj.host(), goDBname, hostObj.user(), hostObj.pass());
	
			// Create from OBO file
				// Term_type: 
					//  OBO "namespace" (biological_process, molecular_function, cellular_component) 
					//  OBO "subsetdef", i.e. goslim
					//  1 is_a, 2 part_of, 3 alt_id
				//  no acc (is_a, part_of, GO:...); no is_obsolete, is_root, is_relation
			goDB.executeUpdate("CREATE TABLE term (" +
					" id integer PRIMARY KEY AUTO_INCREMENT, " + 
					" name varchar(255) NOT NULL, " +  		// OBO "name" (description)
					" term_type varchar(55) NOT NULL, " +  	// OBO see above
					" gonum int unsigned default 0, " +		// OBO "id" w/o GO:
					" is_obsolete tinyint default 0, " +	// OBO "def: \"OBSOLETE"
					" level smallint default 0, " +			// TCW specific
					" index idx1 (gonum)" +
					" ) ENGINE=MyISAM;");
			// Create from OBO file
				// no term1_id, term2_id, complete
			goDB.executeUpdate("CREATE TABLE term2term (" +
					" id integer PRIMARY KEY AUTO_INCREMENT, " +
					" relationship_type_id int default 0, " +	// OBO "is_a:" "relationship: part_of"
					" parent int unsigned default 0, " + 		// OBO is_a or part_of GOnum
					" child int unsigned default 0, " +			// OBO id
					" index idx1 (parent)," +
					" index idx2 (child)" +
					" ) ENGINE=MyISAM;");
			// Create from term2term
				// distance and relation_distance in MySQL but not in OBO. Coped to sTCW and mTCW
				// distance is used in Basic.goTree, but is not necessary.
				// sTCW/ mTCW removed relation_distance for CAS318
				// no term1_id, term2_id
			goDB.executeUpdate("CREATE TABLE graph_path (" +
					" id integer PRIMARY KEY AUTO_INCREMENT, " +
					" relationship_type_id int default 0, " + // never use - remove from everything
					" distance int default 0, " +			
					" ancestor int unsigned default 0, " + 
					" child int unsigned default 0, " +
					" index idx1 (ancestor)," +
					" index idx2 (child)" +
					" ) ENGINE=MyISAM;");
			
			goDB.executeUpdate("CREATE TABLE term_subset (" +
					" gonum int default 0, " +
					" subset_id int default 0, " +
					" index idx1 (subset_id)" +
					" ) ENGINE=MyISAM;");
			
			// Create like in DoGO
				// filename: mysql create date; OBO "data-version: releases/2021-02-01"
			goDB.executeUpdate("CREATE TABLE " + goMetaTable + " (" +
					" filename text null, " + 
					" isOBO tinyint default 1, "	+ // only exists if from OBO file
					" upDBs text" +
					" ) ENGINE=MyISAM;");
			
			goDB.executeUpdate("CREATE TABLE " + goUpTable + " (" +
                    "UPindex bigint unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "UPid varchar(25), " +
                    "acc varchar(15)," +
                    "go text, pfam text, kegg text, ec text, interpro text,"+
                    "unique (UPid) " + 
                    ") ENGINE=MyISAM;");
			
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Creating GO database"); return false;}
	}
	/************************************************************
	 * Load from OBO file
	 */
	private boolean loadGOdbFromOBO() {
		try {
			Pattern patDT = Pattern.compile("([0-9]{4})-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])");
			Pattern patSB = Pattern.compile("subsetdef:\\s+(\\S*)\\s+(.+$)");
			Pattern patID = Pattern.compile("id:\\s+GO:(\\d+)");
			Pattern patALT = Pattern.compile("alt_id:\\s+GO:(\\d+)");
			Pattern patRL = Pattern.compile("(.*)\\s+GO:(\\d+)(.*)");
			// CAS320 - make list of GOs to check in DoUPdat that they are all there (sanity check)
			Pattern patIDStr = Pattern.compile("id:\\s+(GO:\\d+)");
			Pattern patALTStr = Pattern.compile("alt_id:\\s+(GO:\\d+)");
			
			long time=Out.getTime();
			BufferedReader inFH=FileHelpers.openGZIP(goDir+"/"+goFile);
			if (inFH==null) return false;
			String line;
			
	/** Header lines **/
			// format-version: 1.2
			line = inFH.readLine();
			String [] tok = line.split(":");
			if (tok.length!=2 || !tok[0].contains("format-version")) {
				Out.PrtErr("Incorrect OBO file: " + line);
				return false;
			}
			if (!tok[1].trim().contentEquals("1.2")) {
				Out.PrtWarn("Expecting version 1.2 - reading " + tok[1]);
			}
			
			// data-version: releases/2021-02-01
			line = inFH.readLine();
			tok = line.split(":");
			String file = goDir;
			if (tok.length!=2 || !tok[0].contains("data-version")) {
				Out.PrtErr("Incorrect OBO file: " + line);
				return false;
			}
			tok = line.split(":");
			String dt = tok[1].replace("releases/", "");
			
	 		Matcher m = patDT.matcher(dt.trim());
			if (m.matches()) {
				String y = m.group(1);
				String x = m.group(2);
				file = goFile + "-" + TimeHelpers.getMonth(x) + y;
			}
			else {
				Out.PrtWarn("Could not parse date: " + dt);
			}
			goDB.executeUpdate("insert into " + goMetaTable + " set filename='" + file + "'");
			
			// subsetdef: goslim_metagenomics "Metagenomics GO slim"
			TreeMap <String, Integer> subIdMap = new TreeMap <String, Integer>  ();
			while ((line = inFH.readLine()) != null) {
				if (line.startsWith("subsetdef")) {
					m = patSB.matcher(line);
					if (m.matches()) {
						String y = m.group(1);
						if (y.startsWith("goslim_")) subIdMap.put(y, 0);
					}
					else Out.PrtWarn("Cannot parse: " + line);
				}
				else if (line.startsWith("[Term]")) {
					break;
				}
			}	
			
	/** Enter header info **/
			goDB.openTransaction(); 
			
			PreparedStatement ps1 = goDB.prepareStatement(
					"insert ignore into term (name, term_type, gonum, is_obsolete) " +
					"values (?,?,?,?)");
			PreparedStatement ps2 = goDB.prepareStatement(
					"insert ignore into term2term (relationship_type_id, parent, child) " +
					"values (?,?,?)");
			PreparedStatement ps3 = goDB.prepareStatement( 
					"insert ignore into term_subset (gonum, subset_id) " +
					"values (?,?)");
		
		/** External and Goslim entries **/
			// The order these are put in is very important as the id's are assumed to be 1,2,3 
			ps1.setString(1, "is_a");    ps1.setString(2, "external"); ps1.setInt(3, 0); ps1.setInt(4, 0); //id=1
    	 	ps1.addBatch();
    	 	ps1.setString(1, "part_of"); ps1.setString(2, "external"); ps1.setInt(3, 0); ps1.setInt(4, 0); //id=2
    	 	ps1.addBatch();
    	 	ps1.setString(1, "alt_id"); ps1.setString(2, "external"); ps1.setInt(3, 0); ps1.setInt(4, 0); //id=3
    	 	ps1.addBatch();
    	 	ps1.executeBatch();
    	 	
    	 	int index=4;
    	 	for (String ss : subIdMap.keySet()) {
    	 		ps1.setString(1, ss); ps1.setString(2, "subset"); ps1.setInt(3, 0); ps1.setInt(4, 0); 
    	 		ps1.addBatch();
    	 		subIdMap.put(ss, index);
    	 		index++;
    	 	}
    	 	ps1.executeBatch();
  
    	 /** GO TERMs **/
			int cntGO=0, cnt1=0, cnt2=0, cnt3=0, cntErr=0;
			int cntBio=0, cntMole=0, cntCell=0, cntAlt=0;
			int cntBioIA=0, cntMoleIA=0, cntCellIA=0, cntBioPO=0, cntMolePO=0, cntCellPO=0;
					
			int gonum=0, is_obsolete=0;
			String name="", term_type="", goID="";
			Vector <String>  parVec = new Vector <String> ();
			Vector <Integer> subVec = new Vector <Integer> ();
			Vector <Integer> goAltVec = new Vector <Integer> (); // this includes the Alts
	
			while ((line = inFH.readLine()) != null) {
				if (cntErr>25) Out.die("Too many errors");
				
				/******** finish **********/
				if (line.startsWith("[Term]") || line.startsWith("[Typedef]")) {// CAS320 added typedef
					if (gonum==0) continue;
					
					for (int go : goAltVec) { // The GO and any alts
						cntGO++;
						
						boolean isBio=false, isMole=false, isCell=false;
						if (term_type.contentEquals("biological_process")) 		{cntBio++; isBio=true;}
						else if (term_type.contentEquals("molecular_function")) {cntMole++;isMole=true;}
						else if (term_type.contentEquals("cellular_component")) {cntCell++;isCell=true;}
						
						ps1.setString(1, name);		// term
						ps1.setString(2, term_type);
						ps1.setInt(3, go);
						ps1.setInt(4, is_obsolete);
						ps1.addBatch();
						cnt1++; 
						if (cnt1 == 1000) {
		 	     	        Out.r("Add GO #" + (cntGO+cntAlt));
		 					ps1.executeBatch();
		 					cnt1=0;
			 			} 
					
						if ((go!=gonum)) { // term2term 		alt 	replacedby 	gonum
							cntAlt++;
							ps2.setInt(1, ALTID);
							ps2.setInt(2, gonum);
							ps2.setInt(3, go);
							ps2.addBatch();
							cnt2++;
						}

						for (String p : parVec) { // term2term gonum/alt is_a/part_of parent
							tok = p.split(":");
							int r = Integer.parseInt(tok[0]);
							if (r==ISA) { //is a
								if (isBio) cntBioIA++;
								else if (isMole) cntMoleIA++;
								else if (isCell) cntCellIA++;
							}
							else if (r==PARTOF){ // part_of
								if (isBio) cntBioPO++;
								else if (isMole) cntMolePO++;
								else if (isCell) cntCellPO++;
							}
							
							String s = tok[1].trim().replaceFirst("^0+(?!$)", "");
							int parGo = 0;
							try {parGo = Integer.parseInt(s);}
							catch (Exception e) {Out.PrtErr("Bad GO: " + tok[1]); cntErr++;}
							
							ps2.setInt(1, r);
							ps2.setInt(2, parGo);
							ps2.setInt(3, go);
							ps2.addBatch();
							cnt2++;
							if (cnt2 == 1000) {
			 					ps2.executeBatch();
			 					cnt2=0;
				 			}
						}
					}
					for (int id : subVec) { // only the main id gets assigned the subset
						ps3.setInt(1, gonum);
						ps3.setInt(2, id);
						ps3.addBatch();
						cnt3++;
						if (cnt3 == 1000) {
		 					ps3.executeBatch();
		 					cnt3=0;
			 			}
					}
	 	            parVec.clear(); subVec.clear(); goAltVec.clear();
	 	            gonum=is_obsolete = 0;
	 	            name=term_type=goID="";
	 	            
	 	            if (line.startsWith("[Typedef]")) break; // do not process rest of file
	 	            else continue;
				} // end saving last GO
				
				/********** parse ***************/
				// id: GO:0000001
				if (line.startsWith("id:")) {
					m = patID.matcher(line);
					if (m.matches()) {
						String goStr = m.group(1);
						try {
							gonum = Integer.parseInt(goStr);
							goAltVec.add(gonum);
						}
						catch (Exception e) {
							Out.PrtErr("Bad id: '" + line + "'");cntErr++;}
						
						m = patIDStr.matcher(line);
						if (m.matches()) {
							goID = m.group(1);
							goSet.add(goID);
						}
						else {Out.PrtErr("Bad GO: '" + line + "'");cntErr++;}
					}
					continue;
				}
				// alt_id: GO:0019952 
				if (line.startsWith("alt_id:")) {
					m = patALT.matcher(line);
					if (m.matches()) {
						String goStr = m.group(1);
						try {
							goSet.add(goStr);
							int n = Integer.parseInt(goStr);
							goAltVec.add(n);
						}
						catch (Exception e) {
							Out.PrtErr("Bad alt_id: '" + line + "'");cntErr++;}
						
						m = patALTStr.matcher(line);
						if (m.matches()) goSet.add(m.group(1));
						else {Out.PrtErr("Bad GO: '" + line + "'");cntErr++;}
					}
					continue;
				}
				
				// name: mitochondrion inheritance
				if (line.startsWith("name:")) {
					tok = line.split(":");
					if (tok.length>=2) name = tok[1].trim();
					else {
						Out.PrtErr("Bad name: '" + line+ "'"); cntErr++;};
					continue;
				}
				// namespace: biological_process
				if (line.startsWith("namespace:")) {
					tok = line.split(":");
					if (tok.length>=2) term_type = tok[1].trim();
					else {
						Out.PrtErr("Bad namespace: '" + line+ "'");cntErr++;}
					continue;
				}
				// is_a: GO:0048308 ! organelle inheritance
				if (line.startsWith("is_a:")) {
					m = patRL.matcher(line);
					if (m.matches()) {
						String y = m.group(2);
						parVec.add("1: " + y); // is_a had id=1
					}
					// is_a: ends_during ! ends_during
					// else {Out.PrtErr("Bad is_a: '" + line+ "'");cntErr++;}
					continue;
				}
				// relationship: part_of GO:0005829 ! cytosol
				if (line.startsWith("relationship:") && line.contains("part_of")) {
					m = patRL.matcher(line);
					if (m.matches()) {
						String y = m.group(2);
						parVec.add("2: " + y); // part_of had id=2
					}
					else {
						Out.PrtErr("Bad part_of: '" + line+ "'");cntErr++;}
					continue;
				}
				// subset: goslim_metagenomics
				if (line.startsWith("subset:")) {
					tok = line.split(":");
					if (tok.length>=2) {
						String sub = tok[1].trim();
						if (sub.startsWith("goslim_")) {
							if (subIdMap.containsKey(sub))
								subVec.add(subIdMap.get(sub));
							else 
								{Out.PrtErr("No such subset: '" + line+ "'");cntErr++;}
						}
					}
					else {Out.PrtErr("Bad subset: " + line+ "'");cntErr++;}
					continue;
				}
				// def: "OBSOLETE.
				if (line.startsWith("is_obsolete:")  && line.contains("true")) {
					is_obsolete=1;
					obsSet.add(goID);
					continue;
				}
			}// end loop through OBO file
			if (gonum!=0) Out.PrtWarn("File may be incomplete - last GO#" + gonum);
			
			if (cnt1>0) ps1.executeBatch();
			if (cnt2>0)	ps2.executeBatch();
			if (cnt3>0)	ps3.executeBatch();
			
			ps1.close(); ps2.close(); ps3.close();
			goDB.closeTransaction();
			inFH.close();
			
			PrtCntMsg3x(cntGO, 		"Total GOs", 	cntAlt, 	"Alt GOs", obsSet.size(), "Obsolete");
			PrtCntMsg3x(cntBio, 	"Biological", 	cntBioIA, 	"is_a", cntBioPO,	"part_of");
			PrtCntMsg3x(cntMole, 	"Molecular", 	cntMoleIA, 	"is_a", cntMolePO,	"part_of");
			PrtCntMsg3x(cntCell, 	"Cellular", 	cntCellIA, 	"is_a", cntCellPO,	"part_of");
			PrtCntMsg2x(subIdMap.size(), "Slims", 	cnt3, 		"GOs in Slim");
			Out.PrtSpMsgTimeMem(1, 	"Complete Load OBO file", time);
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Creating GO database"); return false;}
	}
	static DecimalFormat df = new DecimalFormat("#,###,###");
	private void PrtCntMsg2x(int cnt, String msg, int cnt2, String msg2) {
		Out.PrtSpMsg(2, String.format("%9s %-10s  %7s %s", 
				df.format(cnt), msg, df.format(cnt2), msg2));
	}
	private void PrtCntMsg3x(int cnt, String msg, int cnt2, String msg2, int cnt3, String msg3) {
		Out.PrtSpMsg(2, String.format("%9s %-10s  %7s %-8s %7s %-8s", 
				df.format(cnt), msg, df.format(cnt2), msg2, df.format(cnt3), msg3));
	}
	/********************************************
	 * Calls DoUPdat to load
	 */
	private boolean loadGOdbfromUniProt(String upDir) {
		String action = "Loading " + upDir + " to " + goDBname;
		Out.PrtSpMsg(1, action);
		try {	
			Vector <String> fullSet = new Vector <String> ();
			
			DoUPdat datObj = new DoUPdat(frameObj);
			File [] dirs = (new File(upDir)).listFiles();
			for (File d : dirs) {
				if (!d.isDirectory()) continue;
				String dname = d.getName() + "/";
				
				File [] xfiles = d.listFiles();
				for (File f : xfiles) {
					String fname = f.getName();
					if (fname.startsWith(".")) continue; 
					if (!fname.endsWith(datSuffix) && !fname.endsWith(datGzSuffix)) continue;
					
					// CAS339 was using subset dat.gz; use full dat.gz so can use any subset
					//if (dname.contains(fullTaxo) && !fname.contains(fullTaxo)) continue;
					if (dname.contains("full")) fullSet.add(dname+f.getName());
					else if (!datObj.dat2godb(upDir, dname+f.getName(),  goDB, goSet, obsSet)) return false; 
					break;
				}	
			}
			for (String file : fullSet) { // CAS339 do fulls last since they contain taxos
				if (!datObj.dat2godb(upDir, file,  goDB, goSet, obsSet)) return false; 
			}
			computeUPdbs();
			
			return datObj.prtTotals();
		}
		catch (Exception e) {ErrorReport.prtReport(e,  action);}
		return false;
	}
	private String computeUPdbs() { // CAS343
		try {
			String up = goDB.executeString("select acc from " + goUpTable +" limit 1");
			if (!up.startsWith("sp_") && !up.startsWith("tr_")) {
				return "+++ Cannot determine UPs from this goDB as pre-v339";
			}
			ResultSet rs = goDB.executeQuery("select distinct(acc) from " + goUpTable);
			String dbs="";
			while (rs.next()) {
				dbs += "\n   " + rs.getString(1);
			}
			goDB.tableCheckAddColumn("TCW_metadata", "upDBs", "text", null);
			goDB.executeUpdate("update TCW_metadata set upDBs='" + dbs + "'");
			return dbs;
		}
		catch (Exception e) {ErrorReport.prtReport(e,  "Compute UPdbs"); return "error";}
	}
	/***********************************************************
	 * Levels added to term table - these are not technically valid, but an easy way to browse
	 */
	public boolean computeLevels() {
		try {	
			long time = Out.getTime();	
			Out.PrtSpMsg(1,"Compute levels");	
			
			TreeMap<Integer,TreeSet<Integer>> parent2child = new TreeMap<Integer,TreeSet<Integer>>();
			HashMap<Integer, Integer> levMap = new HashMap<Integer, Integer> ();
			
			String sql = "select child,parent from term2term where child != 0 and parent != 0 "
					+ "and relationship_type_id!=3"; 		// CAS320
			ResultSet rs = goDB.executeQuery(sql);
			while (rs.next()) {
				int child = rs.getInt(1);
				int parent = rs.getInt(2);
				if (!parent2child.containsKey(parent))
					parent2child.put(parent,new TreeSet<Integer>());
				parent2child.get(parent).add(child);
			}
			rs.close();
			// CAS339 only shows #parents, Out.PrtCntMsg(parent2child.size(), "Parent-child");
			
			// Now get the top-level nodes and do a depth-first traversal, adding as we go.
			// Note, some gonums appear in multiple places in the "tree", up to 300+ times.
			// E.g., 433 has 55 immediate parents and 384 locations in the tree.
			// The biggest duplications are in the biological_process tree.
			// (Note, since we ignored cross-relationships above, each go is only in one of the three major trees, e.g. biological process). 
			
			String[] types = Globalx.GO_TERM_LIST; 
			int [] topGO = new int [types.length];
			int ii=0;
			for (String type : types) {
				topGO[ii++] = goDB.executeInteger("select gonum from term where name='" + type + "'");
			}
			
			for (int i=0; i<types.length; i++) { // loop 3 times...
				int nOrdered = 0, level = 0, maxLevel=0;
				
				Stack<Integer> stack = new Stack<Integer>();
				Stack<Integer> curPath = new Stack<Integer>();
				stack.push(topGO[i]);
				while (!stack.isEmpty())  {
					int gonum = stack.pop();				
					int curSubtreeRoot = (curPath.empty() ? 0 : curPath.lastElement());
					if (curSubtreeRoot == gonum) { // the whole subtree has been traversed 	
						curPath.pop();
						level--;
					}
					else {
						// For GOs at multiple levels (e.g., cell_part) we take the highest level (e.g. prefer 3 over 2). 
						// This fixes the "cell_part" type problem but is otherwise basically arbitrary.
						if (levMap.containsKey(gonum)) {
							int curLevel = levMap.get(gonum);
							if (level>curLevel) levMap.put(gonum, level);
						}
						else levMap.put(gonum, level);
						
						// Working through the children of the most recent branching node (or still at the root node)
						nOrdered++;
						if(nOrdered%1000 == 0) Out.r(types[i] + " " + nOrdered);
						
						if (parent2child.containsKey(gonum)){
							curPath.push(gonum);
							stack.push(gonum);
							stack.addAll(parent2child.get(gonum));
							level++;
							maxLevel = Math.max(maxLevel, level);
						}
					}
				}
				Out.PrtCntMsg(nOrdered, "edges for " + types[i] + "; max level " + maxLevel);
			}
			
			Out.PrtSpMsg(2,"Add GO level numbers to term table");
			goDB.openTransaction(); 
			PreparedStatement ps = goDB.prepareStatement("update term set level=? where gonum=?");
			int cntSave=0, cnt=0;
			for (int gonum : levMap.keySet()) {
				ps.setInt(1, levMap.get(gonum)+1);
				ps.setInt(2, gonum);
				ps.addBatch();
				
				cntSave++; cnt++;
				if (cntSave == 5000) { 
					cntSave=0;
					ps.executeBatch();
					Out.r("Saved " + cnt);
				}
			}
			if (cntSave>0) ps.executeBatch();
			ps.close(); 
			goDB.closeTransaction();
			
			Out.PrtSpMsgTimeMem(1, "Complete GO Levels", time);
			return true;
		}
		catch(Exception e){ErrorReport.reportError(e, "Compute levels"); return false;}
	}
	
	/******************************************************************
	 * This was in the mysql GO - create the same thing here.
	 */
	public boolean computeAncestors() {
		try {	
			long time = Out.getTime();	
			Out.PrtSpMsg(1,"Compute ancestors");	
		
		// Get all data
			String[] types = Globalx.GO_TERM_LIST; 
			int [] topGO = new int [types.length];
			int ii=0;
			for (String type : types) {
				topGO[ii++] = goDB.executeInteger("select gonum from term where name='" + type + "'");
			}
			
			String sql = "select gonum from term where gonum != 0 "; 		
			ResultSet rs = goDB.executeQuery(sql);
			while (rs.next()) {
				int gonum = rs.getInt(1);
				termMap.put(gonum, new Term(gonum));
			}
		
			sql = "select child, parent, relationship_type_id from term2term where child != 0 and parent != 0 "; 		
			rs = goDB.executeQuery(sql);
			while (rs.next()) {
				int child = rs.getInt(1);
				int parent = rs.getInt(2);
				int relation = rs.getInt(3);
				Term t = termMap.get(parent);
				t.addChild(child, relation);
			}
			rs.close();
			Out.PrtCntMsg(termMap.size(), "GOs to process ancestors");
			
		// Add all ancestors in a recursive routine starting at the root nodes
			for (int gonum : topGO) { 
				addAnc(termMap.get(gonum));
			}
		
		// Save data
			int cntSave=0, cnt=0;
			goDB.openTransaction();
			PreparedStatement ps = goDB.prepareStatement(
					"insert into graph_path (ancestor, child, relationship_type_id) values(?,?,?)");
			for (Term t : termMap.values()) {
				for (int ancGO : t.ancMap.keySet()) {
					int rel = t.ancMap.get(ancGO);
					ps.setInt(1, ancGO);
					ps.setInt(2, t.gonum);
					ps.setInt(3, rel);
					ps.addBatch();
					
					cntSave++; cnt++;
					if (cntSave == 5000) { 
						cntSave=0;
						ps.executeBatch();
						Out.r("Saved " + cnt);
					}
				}	
			}
			if (cntSave>0) ps.executeBatch();
			ps.close(); 
			goDB.closeTransaction();
			
			Out.PrtCntMsg(cnt, "Ancestor paths ");
			Out.PrtSpMsgTimeMem(1, "Complete ancestors", time);
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Compute ancestors");}
		return false;
	}
	private void addAnc(Term pTerm) {
		try {
			for (int child : pTerm.childMap.keySet()) {
				int rel = pTerm.childMap.get(child);
				
				Term cTerm = termMap.get(child);
				cTerm.addAnc(pTerm.gonum, rel);
				cTerm.addAnc(pTerm.ancMap);
				addAnc(cTerm);
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Add ancestors");}
	}
	TreeMap <Integer, Term> termMap = new TreeMap <Integer, Term>  ();
	private class Term {
		Term (int gonum) { 
			this.gonum = gonum;
		}
		void addChild(int go, int rel) {
			childMap.put(go, rel);
		}
		void addAnc(int go, int rel) {
			ancMap.put(go, rel);
		}
		void addAnc(HashMap <Integer, Integer> goMap) {
			for (int go : goMap.keySet()) ancMap.put(go, goMap.get(go));
		}
		int gonum;
		HashMap <Integer, Integer> childMap = new  HashMap <Integer, Integer> (); // children, relation
		HashMap <Integer, Integer> ancMap = new  HashMap <Integer, Integer> (); // assigned and inherited ancestors, relation
	}
	/**************************************************/
	private HashSet <String> goSet = new HashSet <String> ();  // all GO IDs
	private HashSet <String> obsSet = new HashSet <String> (); // set of obsolete GO IDS
	private String upDir;
	private String goDBname;  // e.g. go_Apr2016
	private String goDir;     // e.g. ./projects/DBfasta/GO_oboApr2016 -- downloaded here
	
	private ASFrame  frameObj=null;
	private DBConn   goDB=null;
	private HostsCfg hostObj=null;
}
