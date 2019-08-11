package cmp.database;

import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;

import util.database.DBConn;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Out;
import util.methods.Static;
/****************************************************
 * Load basic information so the queries do not have to be repeated.
 */
public class DBinfo {
	/****************************************************************
	 * viewMulti on startup
	 */
	public DBinfo(DBConn dbc) {
		mDB = dbc;
		loadFromInfoTable();
		loadFromCompute();
	}
	
	public void loadFromInfoTable() {
		try {
			ResultSet rs = mDB.executeQuery(
				"select allTaxa, allSeqLib, allSeqDE, pairInfo from info");
			if (!rs.next()) ErrorReport.die("Unexplained error when reading columns");
			
			String sTaxa = rs.getString(1);
			String ssLib = rs.getString(2);
			String ssDE = rs.getString(3);
			String sumStats = rs.getString(4);
			rs.close();
			
			hasSumStats = (sumStats==null || sumStats.equals("")) ? false : true;
			
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
	private void loadFromCompute() {
		try {
			setASM();    // need to read assembly table for species and which are protein
			setMethod(); // need to read method table for Name and Prefix
			
			cntSeq = mDB.executeCount("select count(*) from unitrans");
			cntPair = mDB.executeCount("select count(*) from pairwise");
			cntGrp = mDB.executeCount("select count(*) from pog_groups");
			
			if (cntPair>0) {
				int cnt = mDB.executeCount("select count(*) from pairwise " +
						" where ntSim>0 limit 1");
				if (cnt>0) hasNTblast=true;
				
				cntStats = mDB.executeCount("select count(*) from pairwise where nAlign>0");
				if (cntStats>0) hasStats=true;
				
				cntKaKs = mDB.executeCount("select count(*) from pairwise where kaks>=" + Globalx.dNullVal); 
				if (cntKaKs>0) hasKaKs=true;
				
				// PCC is between -1 and 1, was checking for >0
				cntPCC = mDB.executeCount("select count(*) from pairwise where PCC!=" + Globalx.dNoVal); 
				if (cntPCC>0) hasPCC=true;
				
				cntPairGrp = mDB.executeCount("select count(*) from pairwise where hasGrp>0");
				
				cntGrpPCC = mDB.executeCount("select count(*) from pog_groups where perPCC > -1");
				
				cntMultiScore = mDB.executeCount("select count(*) from pog_groups where conLen>0");
				if (cntMultiScore>0) hasMultiScore=true;
				int cntDBalign = mDB.executeCount("select count(*) from pog_groups where conSeq is not null limit 1");
				if (cntDBalign>0) hasDBalign=true;
			}
			int cnt = mDB.executeCount("select count(*) from go_info limit 1");
			hasGOs = (cnt>0);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "loading counts");}
	}
	/*******************************************************
	 * Called from CompilePanel after 'Build Database'
	 */
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
			
			if      (cntAAdb>0 && cntNTdb==0) isAAonly=true;
			else if (cntNTdb>0 && cntAAdb==0) isNTonly=true;
			else isMixed=true;
			
			allsTCW = new String [col.size()];
			for(int x=0; x<allsTCW.length; x++) {
				allsTCW[x] = col.get(x);
			}
			col.clear(); 
		}
		catch (Exception e){ErrorReport.prtReport(e, "Error getting datasets");}	
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

	/*********************************************
	 * Called from CompilePanel after 'Add Methods' 
	 */
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
	
	private String join(String [] s) {
		if (s==null) return "'" + "" + "'";
		StringBuffer buffer = new StringBuffer();
		
		for (int i=0; i< s.length; i++) buffer.append(s[i] + " ");
		
		return "'" + buffer.toString() + "'";
	}
	/*******************************************************
	 * Gets
	 */
	public String [] getTaxa() { return allTaxa; }
	public String [] getSeqDE() { return allSeqDE; }
	public String [] getSeqLib() { return allSeqLib; }
	
	public int [] getMethodIDs() { return methodID;}
	public int getMethodID(String name) { 
		for (int i=0; i<nMethods; i++) {
			if (methodPrefix[i].equals(name)) return methodID[i];
		}
		Out.debug("No method with name " + name);
		return 0;
	}
	public String [] getMethods() { return methodName;}
	public String [] getMethodPrefix() { return methodPrefix;}
	public String getStrMethodPrefix() { return allPrefix;}
	
	public int nAAdb() { return cntAAdb;}
	public int nNTdb() { return cntNTdb;}
	
	public String [] getASM() { return allsTCW;}
	public int getASMnum() { return allsTCW.length;}
	public HashMap <String, Boolean>getAsmPrMap() {return asmIsPrMap;}
	public HashMap <String, Integer>getAsmIdxMap() {return asmIdxMap;}
	public int getAsmIdx(String asmName) {
		if (asmIdxMap.containsKey(asmName)) return asmIdxMap.get(asmName);
		Out.PrtError("NO dataset name " + asmName);
		return 0;
	}
	
	public String getSeqDESQL() { 
		if (allSeqDE==null || allSeqDE.length==0) return "";
		String sql = Globals.PRE_DE + allSeqDE[0];
		for (int i=1; i<allSeqDE.length; i++) sql += "," + Globals.PRE_DE + allSeqDE[i];
		return sql;
	}
	public String getSeqLibSQL() { 
		if (allSeqLib==null || allSeqLib.length==0) return "";
		String sql = Globals.PRE_LIB + allSeqLib[0];
		for (int i=1; i<allSeqLib.length; i++) sql += "," + Globals.PRE_LIB + allSeqLib[i];
		return sql;
	}
	
	public boolean isReservedWords(String w) {
		try {
			String ww = w.toLowerCase();
			HashSet <String> words = new HashSet <String> ();
			ResultSet rs = mDB.executeQuery("SELECT name FROM mysql.help_keyword");
			while (rs.next()) words.add(rs.getString(1).toLowerCase());
			return words.contains(ww);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Getting reserved words");}
		return false;
	}
	/*********************************************************
	 * Want to show between 20 and 1000 of each
	 */
	private final int minList=20, maxList=1000;
	/*********************************************************
	 * Sample cluster
	 * 1. >N (3,2,1) sequences from each dataset
	 * 2. >N (100,95,90,85) %shared description
	 * 3. All or limit
	 */
	public String getSampleGrp() {
		try {
			String x = mDB.executeString("select grpSQL from info");
			if (x!=null && x!="" && x.contains(":")) return x;
			if (cntGrp==0) return "Show all : 1"; 
			
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
	public String getSamplePair() {
		try { // added for v2.6
			String x = mDB.executeString("select pairSQL from info");
			if (x!=null && x.contains(":")) return x;
			if (cntPair==0) return "Show all : 1";
			
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
	public String getSampleSeq() {
		try {
			String x = mDB.executeString("select seqSQL from info");
			if (x!=null && x.contains(":")) return x;
			
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
	public boolean hasNegDE() {
		if (cntNegDE!=1) return (cntNegDE>0);
		
		try {
			ResultSet rs=null;
			for (int i=0; i< allSeqDE.length && cntNegDE==-1; i++) {
				String col = Globals.PRE_DE + allSeqDE[i];
				rs = mDB.executeQuery("select UTstr from unitrans where " + 
						 col + " < 0  limit 1"); 
				if (rs.first()) cntNegDE=1;
			}
			if (cntNegDE==-1) cntNegDE=0;
			return (cntNegDE>0);
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Could not get GO cnt"); return false;}
	}
	public boolean hasNTblast() { return hasNTblast;}
	public boolean hasSumStats() {return hasSumStats;}
	public boolean hasStats() {return hasStats;}
	public boolean hasKaKs() {return hasKaKs;}
	public boolean hasPCC() {return hasPCC;}
	public boolean isAAonly() {return isAAonly;}
	public boolean isNTonly() {return isNTonly;}
	public boolean isMixed() {return isMixed;}
	public boolean hasAA() {return (isAAonly || isMixed);}
	public boolean hasGOs() {return hasGOs;}
	public boolean hasMultiScore() {return hasMultiScore;}
	public boolean hasDBalign() {return hasDBalign;}
	public boolean hasRPKM() {return (allSeqLib!=null && allSeqLib.length>0);}
	
	public int getCntSeq() {return cntSeq;}
	public int getCntGrp() {return cntGrp;} 
	public int getCntPair() {return cntPair;} // pairs in table
	public int getCntPCC() {return cntPCC;}  // pairs with PCC = should equal cntPair
	public int getCntGrpPCC() {return cntGrpPCC;}  // pairs with PCC = should equal cntPair
	public int getCntMultiScore() {return cntMultiScore;}
	
	public void resetGO() { // executed after LoadSingleGO
		cntGO=-1;
		getCntGO();
	}
	public int getCntGO() { 
		if (cntGO ==-1) {
			try {
				cntGO = mDB.executeCount("select count(*) from go_info");
			}
			catch (Exception e) {ErrorReport.prtReport(e, "Could not get GO cnt"); return -1;}
		}
		return cntGO;
	};

	public int getCntStats() {return cntStats;} // pairs with stats = should equal cntPairGrp
	
	public int getCntPairGrp() { return cntPairGrp;} // pairs with pairwise.hasGrp
	public int getCntKaKs() { return cntKaKs;}
	public String [] getTermTypes() {
		if (termTypes!=null) return termTypes;
		try {
			ResultSet rs = mDB.executeQuery("SHOW COLUMNS FROM go_info LIKE 'term_type'");
			if(rs.next()) {
				String val = rs.getString(2);
				val = val.substring(val.indexOf('(') + 1, val.length() - 1);
				String [] valArr = val.split(",");
				termTypes = new String[valArr.length];
				for(int x=0; x<valArr.length; x++) {
					termTypes[x] = valArr[x].substring(1, valArr[x].length()-1);
				}
			}
			rs.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Could not get termType"); return null;}
		return termTypes;
	}
	
	/**********************************************************/
	private String [] allsTCW=null;
	private HashMap <String, Integer> asmIdxMap = new HashMap <String, Integer> ();
	private HashMap <String, Boolean> asmIsPrMap = new  HashMap <String, Boolean> ();
	
	private String [] allSeqDE=null, allSeqLib=null;
	
	private boolean isAAonly=false, isNTonly=false, isMixed=false;
	private String [] termTypes = null;
	
	private int nMethods=0;
	private String allPrefix="";
	private int [] methodID=null;
	private String [] methodName=null, methodPrefix=null;
	private String [] allTaxa=null;
	
	private boolean hasStats=false, hasKaKs=false, hasPCC=false, hasMultiScore=false, hasDBalign=false;
	private boolean hasSumStats=false, hasGOs=false, hasNTblast=false;
	private int cntAAdb=0, cntNTdb=0, cntSeq=0, cntGrp=0, cntPair=0, cntGO=-1;
	private int cntStats=0, cntKaKs=0, cntPairGrp=0, cntPCC=0, cntMultiScore=0;
	private int cntGrpPCC=0;
	private int cntNegDE=-1; // not used right now
	
	private DBConn mDB=null;
}
