package cmp.database;

import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashMap;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
/****************************************************
 * Load basic information so the queries do not have to be repeated.
 * CAS310 was loading some info on demand, but that assume mDB stays active. Now, all loaded at once.
 * 	Speedup this up by changing setPairs and adding setPairsEdit
 */
public class DBinfo {
	
	// this gets called after every major step in runMulti, on startup of viewMulti, and on compute summary
	// the calling routine closes dbc
	public DBinfo(DBConn dbc) {
		mDB = dbc;
		
		setASM();    // need to read assembly table for species and which are protein
		setMethod(); // need to read method table for Name and Prefix
		setCounts();
		setPairs();
		setSeqLibDE();
		setTaxa();
		setFromInfoTable();
	}
	
	private void setASM() {
		try {
			ResultSet rs = mDB.executeQuery("select ASMstr, isPep, ASMid from assembly");
			Vector <String> col = new Vector <String> ();
			while (rs.next()) {
				String asm = rs.getString(1);
				boolean ip = rs.getBoolean(2);
				col.add(asm);
				asmIsPrMap.put(asm, ip);
				asmIdxMap.put(asm, rs.getInt(3));
				
				if (ip) cntAAdb++;
				else cntNTdb++;
			}
			rs.close();
			
			if (cntNTdb>0 && cntAAdb==0) isNTonly=true;
			
			allsTCW = new String [col.size()];
			for(int x=0; x<allsTCW.length; x++) {
				allsTCW[x] = col.get(x);
			}
			col.clear(); 
			
			hasCounts = (mDB.executeCount("select count(*) from unitrans where totExp>0")>0); // CAS305
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error getting datasets");}	
	}
	
	private void setMethod() { // called on update and read
		try {
			nMethods = mDB.executeCount("select count(*) from pog_method");
			methodID = new int [nMethods];
			methodName = new String [nMethods];
			methodPrefix = new String [nMethods];
			
			ResultSet rs = mDB.executeQuery("select PMid, PMstr, prefix from pog_method" +
					" order by PMstr"); 
			
			int x=0;
			while (rs.next()) {
				methodID[x] = 		rs.getInt(1);
				methodName[x] = 		rs.getString(2);
				methodPrefix[x] = 	rs.getString(3);
			
				if (allPrefix.equals("")) allPrefix = methodPrefix[x];
				else allPrefix +=  "," + methodPrefix[x];
				
				x++;
			}
			rs.close();
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error getting methods");}
	}
	
	private void setCounts() {
		try {
			cntSeq = mDB.executeCount("select count(*) from unitrans");
			cntPair = mDB.executeCount("select count(*) from pairwise");
			cntGrp = mDB.executeCount("select count(*) from pog_groups");
			cntHit = mDB.executeCount("select count(*) from unique_hits");
			cntGO = mDB.executeCount("select count(*) from go_info");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "loading counts");}
	}
	
	private void setFromInfoTable() {
		try {
			ResultSet rs = mDB.executeQuery(
				"select allTaxa, allSeqLib, allSeqDE, pairInfo from info");
			if (!rs.next()) ErrorReport.die("Unexplained error when reading columns");
			
			String sTaxa = rs.getString(1);
			String ssLib = rs.getString(2);
			String ssDE = rs.getString(3);
			rs.close();
			
			if (sTaxa == null || sTaxa.equals("")) setTaxa();
			else allTaxa = sTaxa.trim().split(" ");
			
			if (ssLib == null || ssLib.equals("") || ssDE == null || ssDE.equals("")) 
				setSeqLibDE();
			else {
				allSeqLib = ssLib.trim().split(" ");
				allSeqDE  = ssDE.trim().split(" ");
			}
		}
		catch(Exception e) {ErrorReport.die(e, "Error loading columns");}
	}
	private void setSeqLibDE() {
		try {		
			Vector <String> colR = new Vector <String> ();
			Vector <String> colD = new Vector <String> ();
			int l1 = Globals.PRE_LIB.length(); 
			int l2 = Globals.PRE_DE.length();
			
			ResultSet rs = mDB.executeQuery("Show columns from unitrans");
			while (rs.next()) {
				String colName = rs.getString(1);
				if(colName.startsWith(Globals.PRE_LIB)) colR.add(colName.substring(l1));
				else if(colName.startsWith(Globals.PRE_DE)) colD.add(colName.substring(l2));
			}	
			allSeqLib = new String[colR.size()];
			for(int x=0; x<allSeqLib.length; x++) allSeqLib[x] = colR.get(x);
			
			allSeqDE = new String[colD.size()];
			for(int x=0; x<allSeqDE.length; x++) allSeqDE[x] = colD.get(x);

			colR.clear(); colD.clear();
			rs.close();			
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error loading dataset names");}
	}
	private void setTaxa() {
		try {
			ResultSet rs = mDB.executeQuery("SELECT taxa FROM pog_groups GROUP BY taxa ORDER BY taxa ASC");
			
			Vector<String> results = new Vector<String> ();
			while(rs.next()) {
				String val = rs.getString(1);
				if(val != null && val.length() > 0 && !results.contains(val))
					results.add(val);
			}
			allTaxa = new String [results.size()];
			for(int x=0; x<allTaxa.length; x++) {
				allTaxa[x] = results.get(x);
			}
			results.clear();
			rs.close();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error loading taxa");}
	}
	
	public boolean isPairsSet() {
		return (cntPairGrp>0);
	}
	public void setPairsEdit(DBConn mDB) {
		try {
			cntStats=cntPCC=cntKaKs=cntPairGrp=cntMulti=0;
			if (cntPair==0 || cntGrp==0) return;
			
			setPairs(); // checks are fast; counts can be long if have to check the entire table
			
			cntPairGrp = mDB.executeCount("select count(*) from pairwise where hasGrp>0");
			if (hasStats) 	cntStats =   mDB.executeCount("select count(*) from pairwise where nAlign>0");
			if (hasKaKs) 	cntKaKs =    mDB.executeCount("select count(*) from pairwise where kaks>=" + Globalx.dNullVal); 
			if (hasPCC) 	cntPCC =     mDB.executeCount("select count(*) from pairwise where PCC!=" + Globalx.dNoVal);              
			if (hasMulti) 	cntMulti =   mDB.executeCount("select count(*) from pog_groups where conLen>0");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "loading counts");}
	}
	
	// CAS310 this method was counting from pairwise, which took very long
	private void setPairs() { 
		try {
			if (cntPair>0) { 
				hasPCC =        mDB.executeBoolean("select hasPCC from info"); 
		
				hasStats =      hasVal("select pairInfo from info");   
				hasKaKs =       hasVal("select kaksInfo from info"); 
				hasNTblast =    hasVal("select ntInfo from info"); 
				
				hasMulti = mDB.executeBoolean("select hasMA from info");
				hasDBalign = hasMulti;
				
				String val = infoKeyVal("MSAscore1");
				msaScore1 = (val!=null) ? val : "Sum-of-pairs";
				
				val = infoKeyVal("MSAscore2");
				msaScore2 = (val!=null) ? val : "Unknown";
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "setting pair info");}
	}
	private boolean hasVal(String sql) {
		try {
			String x = mDB.executeString(sql);
			if (x==null || x.trim().contentEquals("")) return false;
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "setting pair info"); return false;}
	}
	//------ CAS312 use this to get all values from table -----------//
	private String infoKeyVal(String key) {
		String val="";
		try {
			val = mDB.executeString("select iVal from infoKeys where iKey='" + key + "'");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting value for " + key);}
		return val;
	}
	/*******************************************************
	 * XXX Updates from runMultiTCW
	 */
	// Called from CompilePanel after 'Build Database'
	public void updateSTCW() { // recompute all values. This automatically loads for 'gets'
		try {
			setASM();
			setSeqLibDE();
			
			mDB.executeUpdate("update info set " +
					" allASM = " + join(allsTCW) + "," + 
					" allSeqLib=" + join(allSeqLib) + "," + 
					" allSeqDE=" + join(allSeqDE) + "," +
					" hasDE=" + allSeqDE.length + "," +
					" hasLib=" + allSeqLib.length 
					);
		}
		catch(Exception e) {ErrorReport.die(e, "Error updating info species");}
	}
	//Called from CompilePanel after 'Add Methods' 
	public void updateMethod() {
		try {
			setMethod();
			setTaxa();
			
			mDB.executeUpdate("update info set " + 
					" allTaxa= " + join(allTaxa) + "," + 
					" allMethods=" + join(methodName) 
					);
		}
		catch(Exception e) {ErrorReport.die(e, "Error updating info method");}
		
	}
	private String join(String [] s) {
		if (s==null) return "'" + "" + "'";
		StringBuffer buffer = new StringBuffer();
		
		for (int i=0; i< s.length; i++) buffer.append(s[i] + " ");
		
		return "'" + buffer.toString() + "'";
	}
	//--- CAS312 add key-value pair to infoKey table ---------//
	public void updateInfoKey(String key, String val) {
		try {
			if (!key.contentEquals("MSAscore1") && !key.contentEquals("MSAscore2")) {
				Out.PrtWarn("No such infoKey keyword: " + key);
				return;
			}
			String oldVal = infoKeyVal(key);
			String sql;
			
			if (oldVal==null) sql = "insert into infoKeys set iKey= '"  + key + "', iVal='" + val + "'";
			else              sql = "update infoKeys set iVal='" + val + "' where iKey='" + key + "'";
			
			mDB.executeUpdate(sql);
		}
		catch(Exception e) {ErrorReport.die(e, "Error updating info method");}
	}
	
	/*********************************************************
	 * XXX SAMPLEs: Want to show between 20 and 1000 of each
	 */
	private final int minList=20, maxList=1000;
	/*********************************************************
	 * Sample cluster
	 * 1. >N (3,2,1) sequences from each dataset
	 * 2. >N (100,95,90,85) %shared description
	 * 3. All or limit
	 */
	public String getSampleGrp(DBConn mDB) { // CAS310 pass in mDB
		try {
			String x = mDB.executeString("select grpSQL from info");
			if (x!=null && x!="" && x.contains(":")) return x;
			if (cntGrp==0) return "Show all : 1"; 
			
			if (cntGrp==-1) {
				Out.PrtWarn("Info not loaded before creating sample cluster");
				setCounts();
			}
			
			Out.PrtSpMsg(1, "Creating sample cluster filter");
			String strQuery=null, strSum="";
			
			// cluster with >=3, or >=2 or >=1 from each sTCW
			for (int range = 3; range>0 && strQuery==null; range--) {
				String query = "(pog_groups." + Globals.PRE_ASM + allsTCW[0] + " >= " + range;
				for(int i=1; i< allsTCW.length; i++) {
					query += " and pog_groups." + Globals.PRE_ASM + allsTCW[i] + " >= " + range;
				}
				query += ")";
				
				int cnt = mDB.executeCount("select count(*) from pog_groups where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Clusters with >= " + range + " of each dataset";
				}
				if (cnt>maxList) {
					strQuery = query + " limit " + maxList;
					strSum = "Clusters with >= " + range + " of each dataset - first " + maxList;
				}
			}
			if (strQuery==null) {
				for (int p=100; p>80 && strQuery==null; p-=5) {
					String query = "perAnno>=" + p;
					
					int cnt = mDB.executeCount("select count(*) from pog_groups where " + query);
					if (cnt>=minList && cnt<=maxList) {
						strQuery = query;
						strSum = "Clusters with %shared description>=" + p;
					}
				}
			}
			if (strQuery==null) {
				if (cntGrp<=maxList) { // works
					strQuery=" 1 ";
					strSum="Show all clusters";
				}
				else {
					strQuery=" 1 limit " + maxList;
					strSum="Show first " + maxList + " clusters";
				}
			}
			String sql=strSum + ":" + strQuery;
			mDB.executeUpdate("update info set grpSQL='" + sql + "'");
			return sql;
		}
		catch (Exception e){ErrorReport.prtReport(e, "Creating grp query");}
		return "Show all: 1";
	}
	/*********************************************************
	 * Sample pair
	 * 1. kaks>N 1-6
	 * 2. has stats
	 * 3. All or limit
	 */
	public String getSamplePair(DBConn mDB) {
		try { 
			String x = mDB.executeString("select pairSQL from info");
			if (x!=null && x.contains(":")) return x;
			if (cntPair==0) return "Show all : 1";
			
			if (cntPair==-1) {
				Out.PrtWarn("Info not loaded before creating sample pair");
				setCounts();
			}
			
			Out.PrtSpMsg(1, "Creating sample pair filter");
			String strQuery=null, strSum="";
			for (int range=1; range<6; range++) {
				String query = "kaks>" + range;
				int cnt = mDB.executeCount("select count(*) from pairwise where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Pairs with Ka/Ks>" + range;	
				}
				if (cnt==0) break; // probably no kaks 
			}
			if (strQuery==null) {
				String query = "pairwise.nAlign>1";
				int cnt = mDB.executeCount("select count(*) from pairwise where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Pairs with Stats";
				}
				else if (cnt>maxList) {
					strQuery= query + " limit " + maxList;
					strSum="Pairs with Stats - first " + maxList;
				}
			}
			if (strQuery==null) { 
				if (cntPair<=maxList) { // works
					strQuery="1";
					strSum="Show all pairs";
				}
				else {
					strQuery="1 limit " + maxList;
					strSum="Show first " + maxList + " pairs";
				}
			}
			String sql=strSum + ":" + strQuery;
			mDB.executeUpdate("update info set pairSQL='" + sql + "'");
			return sql;
		}
		catch (Exception e){ErrorReport.prtReport(e, "Creating pair query");}
		return "Show all: 1";
	}
	/*****************************************************
	 * Sample sequence
	 * 1. in all clusters
	 * 2. in any cluster (add limit)
	 * 3. with annotation (add limit)
	 */
	public String getSampleSeq(DBConn mDB) {
		try {
			String x = mDB.executeString("select seqSQL from info");
			if (x!=null && x.contains(":")) return x;
			
			if (cntSeq==-1) {
				Out.PrtWarn("Info not loaded before creating sample seq");
				setCounts();
			}
			
			Out.PrtSpMsg(1, "Creating sample sequence filter");
			String strSum=null, strQuery=null;
			
			// In all clusters
			String query="";
			if (methodPrefix.length>0) {
				for (int i=0; i<methodPrefix.length; i++) {
					query = Static.combineBool(query, "unitrans." + methodPrefix[i] + " is not null");
				}
				int cnt = mDB.executeCount("select count(*) from unitrans where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Sequences found in all cluster methods";
				}
				if (cnt>maxList) {
					strQuery= query + " limit " + maxList;
					strSum="Sequences found in all cluster sets - first " + maxList;
				}	
			}
			
			if (strQuery==null && methodPrefix.length>0) {
				for (int i=0; i<methodPrefix.length; i++) {
					query = Static.combineBool(query, "unitrans." + methodPrefix[i] + " is not null", false);
				}
				int cnt = mDB.executeCount("select count(*) from unitrans where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Sequences found in any cluster set";
				}
				if (cnt>maxList) {
					strQuery= query + " limit " + maxList;
					strSum="Sequences found in any cluster set - first " + maxList;
				}	
			}
			
			if (strQuery==null) { // first part works
				query = "unitrans.hitID>0";
				int cnt = mDB.executeCount("select count(*) from unitrans where " + query);
				if (cnt>=minList && cnt<=maxList) {
					strQuery = query;
					strSum = "Sequences have annotations";
				}
				if (cnt>maxList) { // works
					strQuery= query + " limit " + maxList;
					strSum="Sequences have annotations - first " + maxList;
				}	
			}
			
			if (strQuery==null) { // Will only do this if using non-annotated
				if (cntSeq<=maxList) { // works
					strQuery=" 1 ";
					strSum="Show all sequences";
				}
				else { // all seqs with clusters
					strQuery="1 limit " + maxList; // works
					strSum="Show first " + maxList + " sequences";
				}
			}
			String sql=strSum + ":" + strQuery;
			mDB.executeUpdate("update info set seqSQL='" + sql + "'");
			return sql;
		}
		catch (Exception e){ErrorReport.prtReport(e, "Creating seq query");}
		return "Show all: 1";
	}
	
	public String getSampleHit(DBConn mDB) { // CAS310 add
		try {
			String x = mDB.executeString("select hitSQL from info");
			if (x!=null && x.contains(":")) return x;
			
			if (cntHit==-1) {
				Out.PrtWarn("Info not loaded before creating sample hit");
				setCounts();
			}
			
			Out.PrtSpMsg(1, "Creating sample hit filter");
			String strSum=null, strQuery=null;
			
			int min=30, n=40, cnt=0;
			while (n>1) {
				cnt = mDB.executeCount("select count(*) from unique_hits where nBest>" + n);
				if (cnt>min) {
					strQuery = "nBest>" + n;
					strSum = "nBest>" + n;
					n=0;
				}
				else n-=4;
			}
			if (strSum==null) {
				n=40; 
				while (n>1) {
					cnt = mDB.executeCount("select count(*) from unique_hits where nSeq>" + n);
					if (cnt>min) {
						strQuery = "nSeq>" + n;
						strSum = "nSeq>" + n;
						n=0;
					}
					else n-=4;
				}
			}
			if (strSum==null) {
				strQuery = "1";
				strSum = "Show all";
			}
			
			String sql=strSum + ":" + strQuery;
			mDB.executeUpdate("update info set hitSQL='" + sql + "'");
			return sql;
		}
		catch (Exception e){ErrorReport.prtReport(e, "Creating hit query");}
		return "Show all: 1";
	}
	/*******************************************************
	 * XXX Gets
	 */
	public int getCntSeq()  {return cntSeq;}
	public int getCntGrp()  {return cntGrp;} 
	public int getCntPair() {return cntPair;} 
	public int getCntHit()  {return cntHit;} 
	// setASM
	public boolean hasCounts() { return hasCounts;}
	public int nAAdb() {return cntAAdb;}
	public int nNTdb() {return cntNTdb;}
	public String [] getASM() { return allsTCW;}
	public int getAsmIdx(String asmName) { 
		if (asmIdxMap.containsKey(asmName)) return asmIdxMap.get(asmName);
		Out.PrtError("NO dataset name " + asmName);
		return 0;
	}
	public boolean isNTonly() {return isNTonly;}
	
	// setMethod
	public int getMethodID(String name) { 
		for (int i=0; i<nMethods; i++) {
			if (methodPrefix[i].equals(name)) return methodID[i];
		}
		Out.bug("No method with name " + name);
		return 0;
	}
	public String [] getMethodPrefix() { return methodPrefix;}
	public String getStrMethodPrefix() { return allPrefix;}
	
	public String [] getTaxa() {return allTaxa; }
	public String [] getSeqDE() { return allSeqDE; }
	public String [] getSeqLib() { return allSeqLib; }
	public String getSeqDESQL() { 
		if (allSeqDE.length==0) return "";
		
		String sql = Globals.PRE_DE + allSeqDE[0];
		for (int i=1; i<allSeqDE.length; i++) sql += "," + Globals.PRE_DE + allSeqDE[i];
		return sql;
	}
	public String getSeqLibSQL() { 
		if (allSeqLib.length==0) return "";
		
		String sql = Globals.PRE_LIB + allSeqLib[0];
		for (int i=1; i<allSeqLib.length; i++) sql += "," + Globals.PRE_LIB + allSeqLib[i];
		return sql;
	}
	public boolean hasRPKM() { 
		return (allSeqLib.length>0);
	}
	
	// XXX - EditStatsPanel only
	public int getCntPCC() {return cntPCC;}  
	public int getCntKaKs() { return cntKaKs;}
	public int getCntMultiScore() {return cntMulti;}
	public int getCntStats() {return cntStats;} 
	public int getCntPairGrp() {return cntPairGrp;} 
	
	
	public boolean hasPCC() 	{ return hasPCC;}
	public boolean hasStats() 	{return hasStats;}
	public boolean hasMultiScore() { return hasMulti;}
	public boolean hasKaKs() { return hasKaKs;}
	
	public boolean hasNTblast() { return hasNTblast;}
	public boolean hasDBalign() {return hasDBalign;}
	
	public boolean hasGOs() {return (cntGO>0);}
	public int getCntGO() { return cntGO;};
	public void resetGO(DBConn dbc) { //executed after LoadSingleGO
		try {
			cntGO = dbc.executeCount("select count(*) from go_info");
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reseting GO count");}
	}
	public String getMSA_Score1() {return msaScore1;} // CAS312 - used in Summary and GrpTable column description
	public String getMSA_Score2() {return msaScore2;} // ditto
	/**********************************************************/
	private String [] allsTCW=null;
	private HashMap <String, Integer> asmIdxMap = new HashMap <String, Integer> ();
	private HashMap <String, Boolean> asmIsPrMap = new  HashMap <String, Boolean> ();
	
	private String [] allSeqDE=null, allSeqLib=null;
	
	private int nMethods=-1;
	private String allPrefix="";
	private int [] methodID=null;
	private String [] methodName=null, methodPrefix=null;
	private String [] allTaxa=null;
	private String msaScore1="", msaScore2="";
	
	private boolean hasDBalign=false, hasNTblast=false, isNTonly=false, hasCounts=true;
	private boolean hasPCC=false, hasStats=false, hasKaKs=false, hasMulti=false;
	
	private int cntAAdb=0, cntNTdb=0;
	private int cntSeq=-1, cntGrp=-1, cntPair=-1, cntGO=-1;
	private int cntStats=-1, cntKaKs=-1, cntPairGrp=-1, cntPCC=-1, cntMulti=-1;
	private int cntHit=-1; // CAS310
	
	private DBConn mDB=null;
}
