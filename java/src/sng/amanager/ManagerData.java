package sng.amanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

import sng.database.Globals;
import sng.database.Version;
import util.database.DBConn;
import util.database.Globalx;
import util.database.HostsCfg;
import util.file.FileC;
import util.file.FileHelpers;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.TCWprops;
import util.methods.Out;
import util.ui.UserPrompt;

/*************************************************************
 * Store all data and shares with the appropriate panel
 * Reads/writes LIB.cfg and sTCW.cfg (separate from CfgAnno - which shares with computation methods)
 * Import annoDB
 * Reads HOSTS.cfg
 */
// CAS304 for -Xlint
//  changed SeqData and CountData extends Attributes to having an attribute object 
//  removed equal method for CountData and AnnodbData

public class ManagerData {
	private boolean debug=false;
	private final  int NUMANNODBS = Globalx.numDB;
	private final static String LIBDIR = Globalx.PROJDIR + "/";
	private final static String PROJDIR = Globalx.PROJDIR  + "/";
	private final static String ANNODIR = Globalx.ANNODIR  + "/";
	private final static String STCW = Globalx.STCW;
	private final static String STCWCFG = Globals.STCWCFG;
	private final static String LIBCFG = Globals.LIBCFG;
	
	public ManagerData(ManagerFrame pf, String projectName, HostsCfg h) {
		theManFrame = pf;
		strProjDir = projectName;
		hostObj = h;
		
		TCWprops.newDB();
		validateProjectFiles(strProjDir);
		readLIBcfg_sTCWcfg();
	}
	/***************************************************
	 * Works for new projects or if a libraries/projectName was created with nothing in it
	 */
	public void validateProjectFiles(String projectName) {
		try {
			File projFile = new File(PROJDIR + projectName);
			
			if(!projFile.exists()) {
				Out.PrtSpMsg(1,"Creating: " + projFile.getAbsolutePath());
				projFile.mkdir();
			}
		    String db = projectName;
	        if (!db.startsWith(STCW)) db = STCW + db;		

			String cfg = PROJDIR + projectName + "/" + STCWCFG;
			projFile = new File(cfg);
			if(!projFile.exists()) {
				Out.PrtSpMsg(1,"Creating " + cfg);
				projFile = new File(cfg);
				BufferedWriter out = new BufferedWriter(new FileWriter(projFile));
				
				out.write("# " + projectName + " " + STCWCFG + " " + Globalx.strTCWver + "\n");
				out.write("SingleID = " + projectName + "     # singleTCW ID\n");
				out.write("STCW_db    = " + db +          "     # sTCW Database\n");
				out.close();
			}
			cfg = PROJDIR + projectName + "/" + LIBCFG;
			projFile = new File(cfg);
			if(!projFile.exists()) {
				Out.PrtSpMsg(1,"Creating " + cfg);
				BufferedWriter out = new BufferedWriter(new FileWriter(projFile));
		
				out.write("# " + projectName +  " " + LIBCFG + " "  + Globalx.strTCWver + "\n");
				out.write("STCW_db  = " + db + "   # sTCW database\n");
				out.close();
			}
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error creating default values");}
	}
	
	/*************************************************************
	 * save LIB.cfg and sTCW.cfg
	 */
	public boolean saveCopyCfg(String id, String db, String proj) {
		String savProj = strProjDir;
		String savDB = strTCWdb;
		String savID = strProjID;
		
		strProjDir = proj;
		strTCWdb = db;
		strProjID = id;
		
		boolean rc1 = libObj.saveLIBcfg();
		boolean rc2 = stcwObj.saveSTCWcfg();
		
		strProjDir = savProj;
		strTCWdb = savDB;
		strProjID = savID;
		return rc1 && rc2;
	}
	
	public boolean saveLIBcfg() {
		return libObj.saveLIBcfg();
	}
	public boolean saveSTCWcfg() {
		return stcwObj.saveSTCWcfg();
	}
	public boolean saveLIBcfg_sTCWcfg() {
		boolean rc = libObj.saveLIBcfg();
		boolean rc2 = stcwObj.saveSTCWcfg();
		return rc && rc2;
	}
	
	private void saveMetaData(SeqData trans, BufferedWriter out) {
		try {
			Attributes attrObj = trans.getAttr();
			if(attrObj.getTitle().length() > 0)		out.write("title     = " + attrObj.getTitle() + "\n");
			if(attrObj.getCultivar().length() > 0)	out.write("cultivar  = " + attrObj.getCultivar() + "\n");
			if(attrObj.getTissue().length() > 0)	out.write("tissue    = " + attrObj.getTissue() + "\n");
			if(attrObj.getStage().length() > 0)		out.write("stage     = " + attrObj.getStage() + "\n");
			if(attrObj.getTreatment().length() > 0)	out.write("treatment = " + attrObj.getTreatment() + "\n");
			if(attrObj.getOrganism().length() > 0)	out.write("organism  = " + attrObj.getOrganism() + "\n");
			if(attrObj.getStrain().length() > 0)	out.write("strain    = " + attrObj.getStrain() + "\n");
			if(attrObj.getYear().length() > 0)		out.write("year      = " + attrObj.getYear() + "\n");
			if(attrObj.getSource().length() > 0)	out.write("source    = " + attrObj.getSource() + "\n");
			out.write("\n");
			out.flush();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error writing config file metadata");}
	}
	private void saveMetaData(CountData exp, BufferedWriter out) {
		try {
			Attributes a = exp.getAttr();
			if(a.getTitle().length() > 0)		out.write("title     = " + a.getTitle() + "\n");
			if(a.getCultivar().length() > 0)	out.write("cultivar  = " + a.getCultivar() + "\n");
			if(a.getTissue().length() > 0)		out.write("tissue    = " + a.getTissue() + "\n");
			if(a.getStage().length() > 0)		out.write("stage     = " + a.getStage() + "\n");
			if(a.getTreatment().length() > 0)	out.write("treatment = " + a.getTreatment() + "\n");
			if(a.getOrganism().length() > 0)	out.write("organism  = " + a.getOrganism() + "\n");
			if(a.getStrain().length() > 0)		out.write("strain    = " + a.getStrain() + "\n");
			if(a.getYear().length() > 0)		out.write("year      = " + a.getYear() + "\n");
			if(a.getSource().length() > 0)		out.write("source    = " + a.getSource() + "\n");
			out.write("\n");
			out.flush();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error writing config file metadata");}
	}
	
	/***********************************************************
	 * XXX Reads the LIB.cfg and sTCW.cfg when a project is loaded
	 */
	public boolean readLIBcfg_sTCWcfg() {
		try {
			System.err.println("Loading " + strProjDir);
			cntErrors=0;
			File libFile = new File(LIBDIR + strProjDir + "/" + LIBCFG);
			if (!libFile.exists()) {
				UserPrompt.showWarn("File does not exist: " + LIBDIR + strProjDir + "/LIB.cfg");
			    return false;
			}
			File projFile = new File(PROJDIR + strProjDir + "/" + STCWCFG);
			if(!projFile.exists()) {
				UserPrompt.showWarn("File does not exist: " + PROJDIR + strProjDir + "/sTCW.cfg");
	            return false;
			}	
			
			libObj.readLIBcfg(libFile);
			stcwObj.readSTCWcfg(projFile);
			if (cntErrors>0)
				UserPrompt.showWarn(cntErrors + " problems reading LIB.cfg and sTCW.cfg (see terminal)");
			return true;
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Reading project data");}
		return false;
	}
	public void importAnnoDBs(String source) {
		stcwObj.importAnnoDBs(new File(source));
		stcwObj.saveSTCWcfg(); 
	}
	private String checkFile(String path, int pathType, int fileType) {
		String f = FileC.addFixedPath(strProjDir, path, pathType);
		if (f==null) {
			cntErrors++;
			return path;
		}
		return FileC.removeFixedPath(strProjDir, path, pathType);
	}
	
	 //if file not there, read rep labels from database
	private String [] readLibCountsFromDB(String transID) {
		try {
			if (!hostObj.checkDBConnect(strTCWdb)) return null; 
           DBConn conn = hostObj.getDBConn(strTCWdb);
           if (conn==null) return null;
        	   
           Vector <String> reps = new Vector <String> ();
		   ResultSet rs = conn.executeQuery("SELECT reps FROM library where parent='" + transID + "'");
		   while (rs.next()) {
			   String r = rs.getString(1); 
			   String [] repNames = r.split(",");
			   for (int i=0; i<repNames.length; i++) reps.add(repNames[i]);
		   } 
		   
		   String [] repNames = new String [reps.size()];
		   for (int i=0; i<reps.size(); i++) {
			   repNames[i] = reps.get(i);
		   }
		   return repNames;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error reading reps from database " + strTCWdb);}
		return null;
	}
	/****************************************************************
	 * Reads an expression file for first line which contains library names
	 */
	public  String [] readLibCountsFromFile(File sourceFile) {
		try {
			BufferedReader projReader = FileHelpers.openGZIP(sourceFile.getAbsolutePath()); // CAS315
			String line = "";
			
			String [] repNames = null;
			if((line = projReader.readLine()) != null) {
				String [] vals = line.split("\\s+");
				if(vals.length > 1) {
					repNames = new String[vals.length - 1];
					for(int x=1; x<vals.length; x++) {
						repNames[x-1] = vals[x].trim();
					}
				}
				else { 
					cntErrors++;
					String msg = "+++ Error parsing header for sample names\n";
					if (line.contains(",")) 
						msg = "Count file appears comma-delimited instead of space-delimited\n" +
							"Use sed -i 's/,/ /g' <file> to replace commas with spaces.";
					Out.PrtWarn(msg);
					Out.PrtSpMsg(2, "Line: " + line);
					projReader.close();
					return null;
				}
			}
			projReader.close();
			
			for (int i=0; i<repNames.length; i++) {
				if (strProjID.equals(repNames[i])) { 
					JOptionPane.showMessageDialog(null, 
							"singleTCW ID cannot be the same as a condition: '" + strProjID + "'", 
							"Warning", JOptionPane.PLAIN_MESSAGE);
				}
			}
			return repNames;
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Loading count data failed"); cntErrors++;}
		return null;
	}

	/**********************************************************
	 * XXX Called from EditTransLibPanel.updateTransLibUI on Add
	 * always single count/rep as replicates have not been defined yet.
	 */
	public boolean addLibsToCount(String seqID, String [] libs) {
		int err=0;
		for (String repCol : libs) {
			if (repMap.containsKey(repCol)) {
				err++;
				Out.PrtWarn("Replicate name exists " + repCol);
			}
		}
		if (err>0) {
			UserPrompt.showError(err + " duplicate names (see terminal) - make all names unique");
			return false;
		}
		for (String repCol : libs) {
			countObjList.add(new CountData(repCol, seqID));
		}
		return true;
	}
	/** 
	 * called from GenRepsPanel (Define Replicates) to replace all count/rep data 
	 ***/
	public void replaceDefinedReps(Vector <String> seqOrder, HashMap <String, String> newRepMap) {
		countObjList.clear();
		repMap.clear();
		CountData cObj=null;
		for (String seqCond : seqOrder) { // seq:cond = repList
			String [] tok = seqCond.split(":");
			cObj = new CountData(tok[0], newRepMap.get(seqCond), tok[1]); // seq, repList, cond
			countObjList.add(cObj);
		}
		showData("Define Replicates " + newRepMap.size());
	}
	
	// for updating the database
    public String getTransLib(int index) {
		Attributes t = seqObjList.get(index).getAttr();
		String theStatement = "UPDATE library SET ";
		theStatement += "title = '" + fix(t.getTitle()) + "', ";
		theStatement += "organism = '" + fix(t.getOrganism()) + "', ";
		theStatement += "cultivar = '" + fix(t.getCultivar()) + "', ";
		theStatement += "strain = '" + fix(t.getStrain()) + "', ";
		theStatement += "tissue = '" + fix(t.getTissue()) + "', ";
		theStatement += "stage = '" + fix(t.getStage()) + "', ";
		theStatement += "treatment = '" + fix(t.getTreatment()) + "', ";
		theStatement += "year = '" + fix(t.getYear()) + "', ";
		theStatement += "source = '" + fix(t.getSource()) + "' ";
		theStatement += "WHERE libid = '" + seqObjList.get(index).getSeqID() + "'";
		return theStatement;
    }
    public String getCountLib(int index) {
    	CountData c = countObjList.get(index);
    	Attributes a = c.getAttr();
    	String theStatement = "UPDATE library SET ";
		theStatement += "title = '" + fix(a.getTitle()) + "', ";
		theStatement += "organism = '" + fix(a.getOrganism()) + "', ";
		theStatement += "cultivar = '" + fix(a.getCultivar()) + "', ";
		theStatement += "strain = '" + fix(a.getStrain()) + "', ";
		theStatement += "tissue = '" + fix(a.getTissue()) + "', ";
		theStatement += "stage = '" + fix(a.getStage()) + "', ";
		theStatement += "treatment = '" + fix(a.getTreatment()) + "', ";
		theStatement += "year = '" + fix(a.getYear()) + "', ";
		theStatement += "source = '" + fix(a.getSource()) + "' ";
		theStatement += "WHERE libid = '" + c.getCondID() + "'";
		return theStatement;
    }
    private String fix(String str) {
    		if (str.contains("\"")) {
    			str = str.replace("\"", " ");
    			Out.PrtWarn("Replaced quotes with blank: " + str);
    		}
    		if (str.contains("\'")) {
    			str = str.replace("\'", " ");
    			Out.PrtWarn("Replaced quotes with blank: " + str);
    		}
    		return str;
    }
    // EditAnnoPanel called on Keep - return true cancels the keep
    public boolean hasAnnoFile(String file) {
    		if (file==null || file.contentEquals("")) return false;
    		
    		String xfile = file;
			String cur = System.getProperty("user.dir");
			int index = file.indexOf(cur);
			if(index == 0) {
				xfile = file.substring(index + cur.length());
				if (xfile.startsWith("/")) xfile = xfile.substring(1);
			}
			if (xfile.startsWith(ANNODIR)) xfile = xfile.replace(ANNODIR, "");
    		
    		for (AnnodbData annoObj: annoObjList) {
    			if (xfile.equals(annoObj.strFastaDB)) {
    				if (UserPrompt.showConfirm("AnnoDB",
    						"AnnoDB file exists in table \n   " + xfile)) return false;
    				else return true;
    			}
    		}
    		return false;
    }
   
	public int getNumCountLibs() { return countObjList.size(); }
	public int getNumSeqLibs() { return seqObjList.size(); }	
	public String [] getSeqID() { 
		String [] seqList = new String [seqObjList.size()];
		int i=0;
		for (SeqData sd : seqObjList) seqList[i++] = sd.strSeqID;
		return seqList;
	}
	public int getNumAnnoDBs() { return annoObjList.size(); }
	
	/**
	 * @return index of new element
	 */
	public int addNewTransLib() {
		seqObjList.add(new SeqData());
		return seqObjList.size() -1;
	}
	public int addNewAnnoDB() {
		AnnodbData ad = new AnnodbData();
		ad.setDefaults();
		annoObjList.add(ad);
		return annoObjList.size() - 1;
	}
	public void removeCountLib(int index) {
		countObjList.remove(index);
	}

	public void removeTransLib(int index) {
		SeqData temp = seqObjList.get(index);
		
		for(int x=countObjList.size()-1; x>=0; x--) {
			CountData cObj = countObjList.get(x);
			if(cObj.strSeqID.equals(temp.strSeqID)) {
				for (String rep: cObj.repList) repMap.remove(rep);
				countObjList.remove(x);
			}
		}
		seqObjList.remove(index);
		repMap.clear(); 
		showData("Remove Seq Data" + temp.strSeqID);
	}
	
	public void removeAnnoDB(int index) {
		annoObjList.remove(index);
	}
	
	public void moveAnnoDB(int from, int to) {
		if(from < 0 || to < 0 || from >= annoObjList.size() || to >= annoObjList.size()) return;
		
		AnnodbData temp = annoObjList.get(from);
		annoObjList.set(from, annoObjList.get(to));
		annoObjList.set(to, temp);
	}
	
	public void setSkipAssembly(boolean skip) { bSkipAssembly = skip; }
	public boolean getSkipAssembly() { return bSkipAssembly; }
	
	public void setUseTransNames(boolean useNames) { bUseTransNames = useNames; }
	public boolean getUseTransNames() { return bUseTransNames; }
	
	public boolean isProteinDB() { return isAAstcw; }
	public void setProteinDB(boolean isProteinDB) { isAAstcw = isProteinDB; }
	
	public AnnodbData getAnnoDBAt(int pos) { return annoObjList.get(pos); }
	public void setAnnoDBAt(int pos, AnnodbData data) { annoObjList.set(pos, data); }
	public boolean isAnnoLoaded() {
		for (AnnodbData adb : annoObjList) 
			if (adb.isLoaded()) return true;
		return false;
	}
	public void clearAnnoLoaded() {
		for (AnnodbData adb : annoObjList) adb.setLoaded(false);
	}
	public CountData getCountLibAt(int pos) { return countObjList.get(pos); }
	
	public SeqData getTransLibraryAt(int pos) { return seqObjList.get(pos); }
	
	public void setProjID(String id) { strProjID = id; }
	public String getProjID() { return strProjID; }
	
	public void setTCWdb(String db) { strTCWdb = db; }
	public String getTCWdb() { return strTCWdb; }

	public void setNumCPUs(int CPUs) { nCPUs = CPUs; }
	public int getNumCPUs() { return nCPUs; }
	
	public AsmData getAsmObj() {return asmObj;}
	public AnnoData getAnnoObj() {return annoObj;}
	
	public void setGODB(String dbName) { strGODB = dbName;  }
	public String getGODB() { return strGODB; }
	public void setNoGO(String b) {strNoGO=b;} // CAS331 add
	public String getNoGO() {return strNoGO;}  // CAS331 add
	public void setSlimSubset(String x) { strSlimSubset = x.trim();  }
	public String getSlimSubset() { return strSlimSubset; }
	public void setSlimFile(String x) { strSlimFile = x.trim(); }
	public String getSlimFile() { return strSlimFile; }
	
	public void setProjDir(String name) { strProjDir = name; }
	public String getProjDir() 			{ return strProjDir; }
	
	public boolean isLibraryLoaded() { return bLibLoaded; }
	public void setLibraryLoaded(boolean loaded) { bLibLoaded = loaded; }
	
	public boolean isInstantiated() { return bInstantiated; }
	public void setInstantiated(boolean loaded) { bInstantiated = loaded; }
	
	public void clearAnnoDBload() { 
		for (AnnodbData ad : annoObjList) ad.setLoaded(false);
	}
	public void clear() {
		annoObjList.clear();
		countObjList.clear();
		seqObjList.clear();
		asmObj.tcList.clear();
		repMap.clear(); 
	}
	private  String removeComment(String line) {
		if (line.lastIndexOf("#")>0)
			return line.substring(0, line.lastIndexOf('#'));
		return line;
	}
	private  void showData(String msg) { // for debugging
		if (!debug) return;
		System.out.println(">>" + msg);
			System.out.println("Sequences");
			for(SeqData sObj : seqObjList) {
				System.out.print("\n\tseqID: " + sObj.strSeqID);
				if (sObj.strSeqFile.length() > 0)
					System.out.print("\t   SeqFile: " + sObj.strSeqFile);
				if (sObj.strCountFile.length() > 0)
					System.out.print("\t   ExpFile: " + sObj.strCountFile);
				if (sObj.repArray!=null)
					System.out.print("\t   Reps: " + sObj.repArray.length);
			}
			
			System.out.println("\nCounts");
			for(CountData cntObj : countObjList) {
				System.out.print("\n\tLibId: " + cntObj.strCondID);
				System.out.print("\tseqId: " + cntObj.strSeqID);
				System.out.print("\tReps: " + cntObj.strReps);
			}
			System.out.println("\nReps " + repMap.size());
			for (String rep : repMap.keySet()) {
				CountData rObj = repMap.get(rep);
				System.out.println("\t" + rep + "\t" + rObj.strCondID + "\t" + rObj.strSeqID);
			}
		System.out.println("\ndone");
	}
	/*******************************************************
	 * Data for Transcript or Read libraries
	 * 	note: Read libraries have 'libid' like expressions libraries, but are grouped with TransLibs.
	 *  Conditions and repNames are linked by CountData.strSeqID field
	 */
	public class SeqData  {
		public SeqData() {}
		public SeqData(String id, boolean b) {
			strSeqID=id;
			isTrans=b;
		}
		public void   setSeqID(String id) { strSeqID = id; }
		public String getSeqID() { return strSeqID; }		
		public void   setSeqFile(String filename) { strSeqFile = filename; }
		public String getSeqFile() { return strSeqFile; }	
		public void   setQualFile(String filename) { strQualFile = filename; }
		public String getQualFile() { return strQualFile; }		
		public void   setExpFile(String filename) { strCountFile = filename; }
		public String getCountFile() { return strCountFile; }
		public void   setFivePrimeSuffix(String suffix) { strFivePrimeSuf = suffix; }
		public String getFivePrimeSuffix() { return strFivePrimeSuf; }
		public void   setThreePrimeSuffix(String suffix) { strThreePrimeSuf = suffix; }
		public String getThreePrimeSuffix() { return strThreePrimeSuf; }
		public Attributes getAttr() { return attrObj;}

		public String strSeqID = "";
		public String strSeqFile = "";
		public String strQualFile = "";
		public String strCountFile = "";
		public String strFivePrimeSuf = "";
		public String strThreePrimeSuf = "";
		public boolean isTrans=true;
		public Attributes attrObj = new Attributes();
		
		public String [] repArray; // used in for readLIBcfg
	}
	/***************************************************
	 * Data for Count (expression level) libraries
	 */
	public class CountData {
		// add one count/rep from EditTransLibPanel reading file
		public CountData(String libid, String seqid) {
			strCondID = libid;
			strSeqID = seqid;
			repList.add(strCondID);
			repMap.put(libid, this);
		}
		// add count/reps from define Replicates (all previous have been deleted)
		public CountData(String seq, String allrep, String cond) {
			strSeqID = seq;
			strCondID = cond;
			strReps = allrep;
	
			String [] reps = allrep.split(",");
			for (String r : reps) {
				r = r.trim();
				repList.add(r);
				repMap.put(r, this);
			}
		}
		// read from file
		public CountData(SeqData seqObj) {
			strCondID = seqObj.strSeqID;
			
			if (seqObj.repArray==null) {
				repList.add(strCondID);
			}
			else {
				for (int i=0; i<seqObj.repArray.length; i++) {
					String r = seqObj.repArray[i].trim();
					repList.add(r);
					if (strReps=="") strReps = r;
					else  strReps += "," + r;
				}
			}
			for (String r : repList) repMap.put(r, this);
			
			Attributes sAttrObj = seqObj.getAttr();
			attrObj.setTitle(sAttrObj.getTitle());
			attrObj.setCultivar(sAttrObj.getCultivar());
			attrObj.setTissue(sAttrObj.getTissue());
			attrObj.setStage(sAttrObj.getStage());
			attrObj.setTreatment(sAttrObj.getTreatment());
			attrObj.setOrganism(sAttrObj.getOrganism());
			attrObj.setStrain(sAttrObj.getStrain());
			attrObj.setYear(sAttrObj.getYear());
			attrObj.setSource(sAttrObj.getSource());
		}
		
		public void setCondID(String id) { strCondID = id; }
		public String getCondID() { return strCondID; }		
		public void setSeqID(String id) { strSeqID = id; }
		public String getSeqID() { return strSeqID; }	
		public int getNumReps() { return repList.size(); }
		public Vector <String> getRepList() { return repList;}
		public void setRepList(Vector <String> rl) { repList = rl;}
		public Attributes getAttr() {return attrObj;}
		
		private String strCondID = "";
		private String strSeqID = "";
		private String strReps = "";
		private Vector <String> repList = new Vector <String> ();
		private Attributes attrObj = new Attributes ();
	}
	/**************************************************************
	 * Attributes for either Transcript/Read or Expression Level Libraries
	 */
	public class Attributes {
		public boolean setAttribute(String key, String value) {
			if(key.equalsIgnoreCase("title")) strTitle = value;
			else if(key.equalsIgnoreCase("cultivar")) strCultivar = value;
			else if(key.equalsIgnoreCase("tissue"))	strTissue = value;
			else if(key.equalsIgnoreCase("stage"))	strStage = value;
			else if(key.equalsIgnoreCase("treatment"))	strTreatment = value;
			else if(key.equalsIgnoreCase("organism"))	strOrganism = value;
			else if(key.equalsIgnoreCase("strain"))	strStrain = value;
			else if(key.equalsIgnoreCase("year"))	strYear = value;
			else if(key.equalsIgnoreCase("source"))	strSource = value;
			else	return false;
			return true;
		}

		public void   setTitle(String title) { strTitle = title; }
		public String getTitle() { return strTitle; }	
		public void   setCultivar(String cultiVar) { strCultivar = cultiVar; }
		public String getCultivar() { return strCultivar; }		
		public void   setTissue(String tissue) { strTissue = tissue; }
		public String getTissue() { return strTissue; }		
		public void   setStage(String stage) { strStage = stage; }
		public String getStage() { return strStage; }		
		public void   setTreatment(String treatment) { strTreatment = treatment; }
		public String getTreatment() { return strTreatment; }		
		public void   setOrganism(String organism) { strOrganism = organism; }
		public String getOrganism() { return strOrganism; }		
		public void   setStrain(String strain) { strStrain = strain; }
		public String getStrain() { return strStrain; }		
		public void   setYear(String year) { strYear = year; }
		public String getYear() { return strYear; }		
		public void   setSource(String source) { strSource = source; }
		public String getSource() { return strSource; }
		
		private String strTitle = "";
		private String strCultivar = "";
		private String strTissue = "";
		private String strStage = "";
		private String strTreatment = "";
		private String strOrganism = "";
		private String strStrain = "";
		private String strYear = "";
		private String strSource = "";
	}
	/**************************************************
	 * Data for annoDB
	 */
	public class AnnodbData {
		public void setLoaded(boolean loaded) 	{ bLoaded = loaded; }
		public boolean isLoaded() 				{ return bLoaded; }		
		public void setSelected(boolean selected) { bSelected = selected; }
		public boolean isSelected() 			{ return bSelected; }	
		
		public void setTaxo(String taxo) 	{ strTaxo = taxo; }
		public String getTaxo() 			{ return strTaxo; }		
		public void setTabularFile(String filename) { strTabularFile = filename; }
		public String getTabularFile() 		{ return strTabularFile; }		
		public void setFastaDB(String filename) { strFastaDB = filename; }
		public String getFastaDB() 			{ return strFastaDB; }		
		public void setParams(String args) 	{ strParams = args; }
		public String getParams() 			{ return strParams; }
		public void setDate(String date) 	{ strDate = date; }
		public String getDate() 			{ return strDate; }
		public void setSearchPgm(String p) 	{ searchPgm = p; }
		public String getSearchPgm() 		{ return searchPgm; }
		
		public void setDefaults() {
			if (!Globals.hasVal(searchPgm)) searchPgm = BlastArgs.defPgm;
			if (!Globals.hasVal(strParams)) 
				strParams = BlastArgs.getArgsDB(searchPgm, "blastp");
		}
		private boolean bLoaded = false;
		private boolean bSelected = true;
		private String strTaxo = "";
		private String strTabularFile = "";
		private String strFastaDB = "";
		private String strParams = "-"; 
		private String strDate = "";
		private String searchPgm="";
	}
	// the next two have a single object to contain the specific variables and calls
	public class AnnoData {
		// Assign best anno
		public void setSPpref(String b) {strSPpref=b;}
		public String getSPpref() {return strSPpref;}
		
		public void setRmECO(String b) {strRmECO=b;} // CAS305
		public String getRmECO() {return strRmECO;}
		
		public void setPruneType(String b) {strPruneType=b;} // CAS331
		public String getPruneType() {return strPruneType;}

	// similarity - the filename is fixed in CfgAnno
		public void   setSelfBlastn(boolean b) 				{ bBlastn=b;}
		public void   setSelfBlastnParams(String param) 	{ self_blastn_params = param; }
		public boolean getSelfBlastn() 						{ return bBlastn; }
		public String  getSelfBlastnParams() 				{ return self_blastn_params; }
		
		public void 	setSelfTblastx(boolean b) 			{ bTblastx=b;}
		public void   	setSelfTblastxParams(String param) 	{ self_tblastx_params = param; }
		public boolean  getSelfTblastx() 					{ return bTblastx; }
		public String   getSelfTblastxParams() 	{ return self_tblastx_params; }
		
		public void   	setSelfBlastp(boolean b) 			{ bBlastp=b; }
		public void   	setSelfBlastpParams(String param) 	{ self_blastp_params = param; }
		public void   	setSelfBlastpPgm(String pgm) 		{ self_blastp_pgm = pgm; }
		public boolean 	getSelfBlastp() 					{ return bBlastp; }
		public String  	getSelfBlastpParams() 				{ return self_blastp_params;} 
		public String 	getSelfBlastpPgm() 					{ return self_blastp_pgm; }
		
		public int getDoPairs() {
			if (bTblastx || bBlastn || bBlastp) return nPairsLimit;
			else return 0;
		}
		public void setPairsLimit(int limit) 	{ nPairsLimit = limit; }
		public int getPairsLimit() 				{ return nPairsLimit; }
		
		private boolean bTblastx=false, bBlastn=false, bBlastp=false;
		private String self_blastn_params =   null; // not '' because blank is acceptable (use defaults)
		private String self_tblastx_params =  null;
		private String self_blastp_params =   null;
		private String self_blastp_pgm = "diamond";
		
		private int nPairsLimit = 1000;
		
	// ORF
		public void setORFaltStart(String r) { strORFaltStart = r; }
		public String getORFaltStart() { return strORFaltStart; }
				
		public void setORFhitEval(String r) { strORFhitEval = r; }
		public String getORFhitEval() { return strORFhitEval; }
		
		public void setORFhitSim(String r) { strORFhitSim = r; }
		public String getORFhitSim() { return strORFhitSim; }
		
		public void setORFlenDiff(String r) { strORFlenDiff = r; }
		public String getORFlenDiff() { return strORFlenDiff; }
		
		public void setORFmkDiff(String r) { strORFmkDiff = r; }
		public String getORFmkDiff() { return strORFmkDiff; }
		
		public void setORFtrainMinSet(String r) { strORFtrainMinSet = r; }
		public String getORFtrainMinSet() { return strORFtrainMinSet; }
		
		public void setORFtrainCDSfile(String filename) { strORFtrainCDSfile = filename; }
		public String getORFtrainCDSfile() { return strORFtrainCDSfile; }
				
		private String strORFaltStart="0";
		
		private String strORFhitEval= Globals.pHIT_EVAL; 			// Automatically use this frame
		private String strORFhitSim = Globals.pHIT_SIM;  		
		
		private String strORFlenDiff=Globals.pDIFF_LEN;
		private String strORFmkDiff=Globals.pDIFF_MK;		// CAS334 Log difference of markov scores
		
		private String strORFtrainMinSet=Globals.pTRAIN_MIN;
		private String strORFtrainCDSfile="";
		
		private String strSPpref=Globals.pSP_PREF; // 0/1
		private String strRmECO=Globals.pRM_ECO; // 0/1
		private String strPruneType = Globals.pPRUNE; // 1,2,3 pruneType
	}
	public class AsmData {
		public void setClique(String clique) { strClique = clique; }
		public String getClique() { return strClique; }
		
		public void setCliqueBlastEval(String eval) { strCliqueBlastEval = eval; }
		public String getCliqueBlastEval() { return strCliqueBlastEval; }
		
		
		public void setCliqueBlastParam(String param) { strCliqueBlastParam = param; }
		public String getCliqueBlastParam() { return strCliqueBlastParam; }
		
		public void setSelfJoin(String selfJoin) { strSelfJoin = selfJoin; }
		public String getSelfJoin() { return strSelfJoin; }
		
		public void setBuryBlastEval(String eval) { strBuryBlastEval = eval; }
		public String getBuryBlastEval() { return strBuryBlastEval; }
		
		public void setBuryBlastIdentity(String identity) { strBuryBlastIdentity = identity; }
		public String getBuryBlastIdentity() { return strBuryBlastIdentity; }
		
		public void setBuryBlastParams(String params) { strBuryBlastParams = params; }
		public String getBuryBlastParams() { return strBuryBlastParams; }
		
		public void setCAPArgs(String args) { strCAPArgs = args; }
		public String getCAPArgs() { return strCAPArgs; }
		
		public void setTCBlastEval(String eval) { strTCBlastEval = eval; }
		public String getTCBlastEval() { return strTCBlastEval; }
		
		public void setTCBlastParams(String params) { strTCBlastParams = params; }
		public String getTCBlastParams() { return strTCBlastParams; }
		
		public void setUserESTSelfBlast(String file) { strUserESTSelfBlast = file; }
		public String getUserESTSelfBlast() { return strUserESTSelfBlast; }
		
		public int getNumTCs() { return tcList.size(); }
		public void addTC(String tc) { tcList.add(tc); }
		public String getTCAt(int pos) { return tcList.get(pos); }
		public void clearTCs() { tcList.clear(); }
		
		private String strClique = "";
		private String strCliqueBlastEval = "";
		private String strCliqueBlastParam = "";
		private String strSelfJoin = "";
		
		private String strBuryBlastEval = "";
		private String strBuryBlastIdentity = "";
		private String strBuryBlastParams = "";
		private String strCAPArgs = "";
		private String strTCBlastEval = "";
		private String strTCBlastParams = "";
		private String strUserESTSelfBlast = "";
		private Vector<String> tcList = new Vector<String> ();
	}
	private class LIBmethods {
		/************************************
		 * LIB.cfg
		 */
		private void readLIBcfg(File libFile) {
			try {
				Vector <SeqData> tmpSeqObjList = new Vector <SeqData> ();
				SeqData seqObj=null;
				String line = "", key = "", value = "";
				BufferedReader libReader = new BufferedReader(new FileReader(libFile));
				
				while((line = libReader.readLine()) != null) {
					line = removeComment(line);
					if(line.indexOf('=') == 0) continue;	
					String [] lineVal = line.split("=");
					if(lineVal.length <= 1) continue;
					
					key = lineVal[0].trim();
					value = lineVal[1].trim();

					if(key.equalsIgnoreCase("STCW_db")) {
						strTCWdb=value; 
						continue;
					}
					// these 3 are now read from HOSTS.cfg
					if (key.equalsIgnoreCase("DB_host") ) {continue;}
					if (key.equalsIgnoreCase("DB_user")) {continue;}
					if (key.equalsIgnoreCase("DB_password") ) {continue;}
					
					if (key.equalsIgnoreCase("libid")) { //  assemble or count
						seqObj = new SeqData(value, false);
						tmpSeqObjList.add(seqObj);
						continue;
					}
					if (key.equalsIgnoreCase("translib")) {
						seqObj = new SeqData(value, true);
						tmpSeqObjList.add(seqObj);
					}
					if (seqObj==null) {
						Out.PrtWarn("Incorrect line in LIB.cfg -- " + line);
						UserPrompt.showError("Incorrect line in LIB.cfg (see terminal)");
						libReader.close();
						return;
					}
					if (key.equals("reps")) seqObj.repArray =  value.split(",");
					else if(key.equalsIgnoreCase("seqfile")) {
						seqObj.strSeqFile = checkFile(value, FileC.dPROJ, FileC.fFASTA);
					}
					else if(key.equalsIgnoreCase("expfile")) {
						seqObj.strCountFile = value; // checked below
					}
					else if(key.equalsIgnoreCase("qualfile")) {
						seqObj.strQualFile = checkFile(value, FileC.dPROJ, FileC.fQUAL);
					}
					else if(key.equalsIgnoreCase("fiveprimesuf")) seqObj.strFivePrimeSuf = value;
					else if(key.equalsIgnoreCase("threeprimesuf")) seqObj.strThreePrimeSuf = value;
					else if(key.equalsIgnoreCase("expfile")) seqObj.strCountFile = value;
					else seqObj.getAttr().setAttribute(key, value);
				} // end while loop through file
				libReader.close();
			
			/** XXX Assign objects to SeqData or CountData, and read count files **/
				for (SeqData sObj : tmpSeqObjList) {
					if (sObj.strSeqFile != ""  && sObj.repArray != null) {
						Out.PrtWarn(sObj.strSeqID + " has sequence file with following reps in LIB.cfg - ignore reps");
						cntErrors++;
					}
					if (sObj.strSeqFile != "") {
						seqObjList.add(sObj);
					}
					else {
						CountData cntObj = new CountData(sObj);
						countObjList.add(cntObj);
					}
				}
				tmpSeqObjList.clear();

				for (SeqData sObj : seqObjList) {
					if (sObj.strCountFile==null || sObj.strCountFile == "") continue; // ESTs
					
					String fileName = FileC.addFixedPath(strProjDir, sObj.strCountFile, FileC.dPROJ);
					if (fileName==null || fileName=="") {
						cntErrors++;
						sObj.repArray = readLibCountsFromDB(sObj.strSeqID);
						if(sObj.repArray == null) {
							Out.PrtWarn("No count file: " + sObj.strCountFile + "\n      And no reps in database for " + sObj.strSeqID);
							cntErrors++;
						}
					}
					else {
						sObj.repArray = readLibCountsFromFile(new File (fileName));
						if(sObj.repArray == null) {
							Out.PrtWarn("No counts in file: " + sObj.strCountFile);
							cntErrors++;
						}
					}
					if(sObj.repArray == null) continue;
					
					// check for consistency with LIB.cfg
					for (int i=0; i< sObj.repArray.length; i++) {
						String repName = sObj.repArray[i];
						if (!repMap.containsKey(repName)) {
							Out.PrtWarn("condition " + repName + " not in LIB.cfg");
							cntErrors++;
						}
						if (sObj!=null) {
							CountData cObj = repMap.get(repName);
							if (cObj!=null) cObj.strSeqID = sObj.strSeqID;
						}
					}
				}
				Vector <CountData> remove = new Vector <CountData> ();
				for (CountData cObj : countObjList) {
					if (cObj.strSeqID=="") {
						Out.PrtWarn(cObj.strReps + " not in any count file");
						cntErrors++;
						remove.add(cObj);
					}
				}
				for (CountData cObj : remove) countObjList.remove(cObj);
				
	 			showData("Final list");	
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Loading LIB.cfg failed");}
		}
		private boolean saveLIBcfg() {
			try {
				String libDir = LIBDIR + strProjDir;
				File libFile = new File(libDir + "/" + LIBCFG);
			
				BufferedWriter out = new BufferedWriter(new FileWriter(libFile));
				out.write("STCW_db  = " + strTCWdb + "\n\n");
				
				out.write("# Datasets with sequences to be assembled.\n\n");
				for(int x=0; x<getNumSeqLibs(); x++) {
					SeqData trans = getTransLibraryAt(x);
					if(trans.getCountFile().length() != 0) continue;
					
					out.write("libid         = " + trans.getSeqID() + "\n");
					if(trans.getSeqFile().length() > 0) out.write("seqfile       = " + trans.getSeqFile() + "\n");
					if(trans.getQualFile().length() > 0)out.write("qualfile      = " + trans.getQualFile() + "\n");
					if(trans.getFivePrimeSuffix().length() > 0) out.write("fiveprimesuf  = " + trans.getFivePrimeSuffix() + "\n");
					if(trans.getThreePrimeSuffix().length() > 0)out.write("threeprimesuf = " + trans.getThreePrimeSuffix() + "\n");
					
					saveMetaData(trans, out);
				}
				
				out.write("# Expression (count, spectra) corresponding to translib\n");
				out.write("# Each libid should occur in a 'expfile' defined by a translib entry below\n\n");
				// XXX
				for(CountData cObj : countObjList) {
					if(cObj.strCondID.length() == 0) continue;
				
					out.write("libid     = " + cObj.strCondID + "\n");
					if (cObj.strReps != "") 
						out.write("reps       = " + cObj.strReps+ "\n");
					saveMetaData(cObj, out);
				}
				
				out.write("# Sequence (Transcript or protein) dataset, with optional expression (count, spectra) data.\n\n");
				for(int x=0; x<getNumSeqLibs(); x++) {
					SeqData trans = getTransLibraryAt(x);
					if(trans.getCountFile().length() == 0) continue;
					
					out.write("translib     = " + trans.getSeqID() + "\n");
					if(trans.getSeqFile().length() > 0) out.write("seqfile      = " + trans.getSeqFile() + "\n");
					if(trans.getQualFile().length() > 0)out.write("qualfile     = " + trans.getQualFile() + "\n");
					out.write("expfile      = " + trans.getCountFile() + "\n");
					
					saveMetaData(trans, out);
				}
				out.close();
				return true;
			}
			catch(Exception e) {
				ErrorReport.prtReport(e, "Error writing config files");
				return false;
			}
		}
	}
	private class STCWmethods {
		private boolean saveSTCWcfg() {
			try {
				File projFile = new File(PROJDIR + strProjDir + "/" + STCWCFG);
				BufferedWriter out = new BufferedWriter(new FileWriter(projFile));
				TCWprops theProps = new TCWprops(TCWprops.PropType.Assem);
				
				out.write("# " + strProjDir + " sTCW.cfg v" + Globalx.strTCWver + "\n");
				out.write("SingleID = " + strProjID + "      # singleTCW ID\n");
				out.write("STCW_db    = " + strTCWdb +      "      # Database (same as in LIB.cfg)\n");
				out.write("CPUs = " + getNumCPUs() + "\n\n");
				
				out.write("# Instantiation\n");
				if(asmObj.strClique.length() > 0 && !asmObj.strClique.equals(theProps.getProperty("CLIQUE")))
					out.write("CLIQUE     = " + asmObj.strClique +"\n");
				if(asmObj.strCliqueBlastEval.length() > 0 && !asmObj.strCliqueBlastEval.equals(theProps.getProperty("CLIQUE_BLAST_EVAL")))
					out.write("CLIQUE_BLAST_EVAL  = " + asmObj.strCliqueBlastEval +"\n");
				if(asmObj.strCliqueBlastParam.length() > 0 && !asmObj.strCliqueBlastParam.equals(theProps.getProperty("CLIQUE_BLAST_PARAMS")))
					out.write("CLIQUE_BLAST_PARAMS   = " + asmObj.strCliqueBlastParam +"\n");
				if(asmObj.strSelfJoin.length() > 0 && !asmObj.strSelfJoin.equals(theProps.getProperty("SELF_JOIN")))
					out.write("SELF_JOIN = " + asmObj.strSelfJoin +"\n");
				if(asmObj.strBuryBlastEval.length() > 0 && !asmObj.strBuryBlastEval.equals(theProps.getProperty("BURY_BLAST_EVAL")))
					out.write("BURY_BLAST_EVAL = " + asmObj.strBuryBlastEval +"\n");
				if(asmObj.strBuryBlastIdentity.length() > 0 && !asmObj.strBuryBlastIdentity.equals(theProps.getProperty("BURY_BLAST_IDENTITY")))
					out.write("BURY_BLAST_IDENTITY = " + asmObj.strBuryBlastIdentity +"\n");
				if(asmObj.strBuryBlastParams.length() > 0 && !asmObj.strBuryBlastParams.equals(theProps.getProperty("BURY_BLAST_PARAMS")))
					out.write("BURY_BLAST_PARAMS = " + asmObj.strBuryBlastParams +"\n");
				if(asmObj.strCAPArgs.length() > 0 && !asmObj.strCAPArgs.equals(theProps.getProperty("CAP_ARGS")))
					out.write("CAP_ARGS = " + asmObj.strCAPArgs +"\n");
				if(asmObj.strTCBlastEval.length() > 0 && !asmObj.strTCBlastEval.equals(theProps.getProperty("TC_BLAST_EVAL")))
					out.write("TC_BLAST_EVAL = " + asmObj.strTCBlastEval +"\n");
				if(asmObj.strTCBlastParams.length() > 0 && !asmObj.strTCBlastParams.equals(theProps.getProperty("TC_BLAST_PARAMS")))
					out.write("TC_BLAST_PARAMS = " + asmObj.strTCBlastParams +"\n");
				if(asmObj.strUserESTSelfBlast.length() > 0)
					out.write("User_EST_selfblast = " + asmObj.strUserESTSelfBlast +"\n");
				
				// TCs
				int numTCs = -1;
				for(int x=1; numTCs < 0; x++)
					if(!theProps.containsKey("TC" + x)) numTCs = x-1;
			
				boolean writeTCs = (numTCs != asmObj.tcList.size());
				
				for(int x=0; x<asmObj.tcList.size() && !writeTCs; x++) {
					try {
						String val = theProps.getProperty("TC" + (x+1));
						if(val != null && !asmObj.tcList.get(x).equals(val))
							writeTCs = true;
					}
					catch(Exception e) {writeTCs = true;}
				}
				if(writeTCs) {
					for(int x=0; x<asmObj.tcList.size(); x++) {
						out.write("TC" + (x+1) + " = " + asmObj.tcList.get(x) +"\n");
					}
				}
				if (!bSkipAssembly) out.write("SKIP_ASSEMBLY = 0\n"); // Note, now defaulting to 1
				if (bUseTransNames)
					out.write("USE_TRANS_NAME = " + (bUseTransNames?"1":"0") +"\n");
				out.flush();

			//------- ANNOTAION--------------//
			// ORFs
				theProps = new TCWprops(TCWprops.PropType.Annotate);
				
				out.write("\n### Annotation Start ###\n");
				
				if (!isAAstcw) {
					out.write("\n# ORF finding\n");
					
					if (annoObj.strORFaltStart.equals("1")) out.write("Anno_ORF_alt_start = 1\n");
					
					if (!annoObj.strORFhitEval.equalsIgnoreCase(theProps.getProperty("Anno_ORF_hit_evalue")))
						out.write("Anno_ORF_hit_evalue = " + annoObj.strORFhitEval + "\n");
					
					if (!annoObj.strORFhitSim.equals(theProps.getProperty("Anno_ORF_hit_sim")))
						out.write("Anno_ORF_hit_sim = " + annoObj.strORFhitSim + "\n");
					
					if (!annoObj.strORFlenDiff.equals(theProps.getProperty("Anno_ORF_len_diff")))
						out.write("Anno_ORF_len_diff = " + annoObj.strORFlenDiff + "\n");
					
					if (!annoObj.strORFmkDiff.equals(theProps.getProperty("Anno_ORF_mk_diff")))
						out.write("Anno_ORF_mk_diff = " + annoObj.strORFmkDiff + "\n");
					
					String file = annoObj.strORFtrainCDSfile;
					if(file != null && !file.equals("") && !file.equals("-1")) 
						out.write("Anno_ORF_train_CDS_file = " + file + "\n");
					else {
						if (!annoObj.strORFtrainMinSet.equalsIgnoreCase(theProps.getProperty("Anno_ORF_train_min_set")))
							out.write("Anno_ORF_train_min_set = " + annoObj.strORFtrainMinSet + "\n");
					}
				}
		// Self blast - v314 change parameters - not backwards compatible
				out.write("\n# Self search for pairs\n");
				String parm; // can be blank
				
				if(annoObj.bBlastn) out.write("Anno_pairs_blastn = 1\n");
				parm = annoObj.self_blastn_params;
				if (Globals.hasVal(parm) && !parm.equals(BlastArgs.getBlastnArgs()))
					out.write("Anno_pairs_blastn_args = " + parm + "\n");
				
				if (annoObj.bTblastx) out.write("Anno_pairs_tblastx = 1\n");
				parm = annoObj.self_tblastx_params;
				if (Globals.hasVal(parm) && !parm.equals(BlastArgs.getTblastxArgs()))
					out.write("Anno_pairs_tblastx_args = " + parm + "\n");
				
				if (annoObj.bBlastp) out.write("Anno_pairs_blastp = 1\n");
				parm = annoObj.self_blastp_params;
				if (Globals.hasVal(parm) && annoObj.self_blastp_pgm.equals("blast")) {
					out.write("Anno_pairs_blastp_pgm = blast\n"); 
					if (!parm.equals(BlastArgs.getBlastArgsORF()))
						out.write("Anno_pairs_blastp_args = " + parm + "\n");
				}
				else {
					if (Globals.hasVal(parm) &&  !parm.equals(BlastArgs.getDiamondArgsORF()))
						out.write("Anno_pairs_blastp_args = " + parm + "\n");
				}
				if (annoObj.nPairsLimit != 1000)
					out.write("Anno_pairs_limit = " + annoObj.nPairsLimit + "\n");
				out.flush();
				
		// GO		
				out.write("\n# Gene Ontology\n");
				if(strGODB.length() > 0) {
					out.write("Anno_GO_DB = " + strGODB + "\n");
					
					if (strSlimSubset.length()>0)
						out.write("Anno_SLIM_SUBSET = " + strSlimSubset + "\n");
					else if (strSlimFile.length()>0)
						out.write("Anno_SLIM_OBOFile = " + strSlimFile + "\n");
					
					if (strNoGO.contentEquals("1")) out.write("Anno_No_GO = 1\n"); // CAS331
				}
				
		// Anno DBs		
				out.write("\n# AnnoDBs\n");
				boolean sp=false;
				if (annoObj.strSPpref.equals("1")) {out.write("Anno_SwissProt_pref = 1\n"); sp=true;}
				if (annoObj.strRmECO.equals("0"))  {out.write("Anno_Remove_ECO = 0\n");  sp=true;}
				if (!annoObj.strPruneType.equals(Globals.pPRUNE))  
						{out.write("Anno_Prune_type = " + annoObj.strPruneType + "\n");  sp=true;}// CAS331
				if (sp) out.write("\n");
				
				for(int x=0; x<getNumAnnoDBs(); x++) {
					if(getAnnoDBAt(x) == null) continue;
					AnnodbData annoDB = getAnnoDBAt(x);
					String comment = "";
					if(!annoDB.isSelected()) comment = "#";
						
					if (Globals.hasVal(annoDB.getTaxo()))
						out.write(comment + "Anno_DBtaxo_" + (x+1) + " = " + annoDB.getTaxo() + "\n");
					if(Globals.hasVal(annoDB.getFastaDB()))
						out.write(comment + "Anno_DBfasta_" + (x+1) + " = " + annoDB.getFastaDB() + "\n");
					if (Globals.hasVal(annoDB.getDate()))
						out.write(comment + "Anno_DBdate_" + (x+1) + " = " + annoDB.getDate() + "\n");
					
					if(Globals.hasVal(annoDB.getTabularFile()))
						out.write(comment + "Anno_DBtab_" + (x+1) + " = " + annoDB.getTabularFile() + "\n");
					else {
						String pgm = annoDB.getSearchPgm();
						if(Globals.hasVal(pgm) && !pgm.equals(BlastArgs.defPgm))
							out.write(comment + "Anno_DBsearch_pgm_" + (x+1) + " = " + annoDB.getSearchPgm() + "\n");
						
						String param = annoDB.getParams();
						boolean rc=true;
						if (Globals.hasVal(parm)) {
							if (pgm.equals("blast")   && param.equals(BlastArgs.getBlastArgsDB())) rc= false;
							if (pgm.equals("diamond") && param.equals(BlastArgs.getDiamondArgsDB())) rc= false;
						}
						if (rc) 
							out.write(comment + "Anno_DBargs_" + (x+1) + " = " + annoDB.getParams() + "\n");
					}
					out.write("\n");
					out.flush();
				}
				out.close();
				return true;
			}
			catch(Exception e) {
				ErrorReport.prtReport(e, "Error writing config files");
				return false;
			}
		}

		/***************************************
		 * sTCW.cfg
		 */
		private void readSTCWcfg(File projFile) {
			try {
				String line = "", key = "", value = "";
				BufferedReader projReader = new BufferedReader(new FileReader(projFile));
				numAnnoMap.clear();
				AnnodbData [] tempAnnos = new AnnodbData[NUMANNODBS];
				for(int x=0; x<tempAnnos.length; x++)
					tempAnnos[x] = null;
				
				while((line = projReader.readLine()) != null) {
					line = removeComment(line);
					if(line.indexOf('=') > 0) {
						String [] lineVal = line.split("=");
						
						if(lineVal.length > 1) {
							key = lineVal[0].trim();
							value = lineVal[1].trim();
							readSTCWkeyVal(key, value, tempAnnos);
						}
					}					
				}
				// Set default args
				if (!Globals.hasVal(annoObj.self_blastn_params)) 
					annoObj.self_blastn_params = BlastArgs.getBlastnArgs();
				if (!Globals.hasVal(annoObj.self_tblastx_params)) 
					annoObj.self_tblastx_params = BlastArgs.getTblastxArgs();
				if (!Globals.hasVal(annoObj.self_blastp_params)) 
					annoObj.self_blastp_params = BlastArgs.getArgsORF(annoObj.self_blastp_pgm); 
				for(int x=0; x<tempAnnos.length; x++) { 
					if(tempAnnos[x] != null) {
						tempAnnos[x].setDefaults(); // DB params defaults set here
						annoObjList.add(tempAnnos[x]); 
					}
				}
				projReader.close();	
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Loading sTCW.cfg  failed");}
		}
		
		private boolean readSTCWkeyVal(String key, String value, AnnodbData [] annoArray) {
			boolean isSel=true, isAnnoDB=false;
			
			if(key.equalsIgnoreCase("SingleID"))	strProjID = value;
			else if(key.equalsIgnoreCase("STCW_db"))strTCWdb = value;
			else if(key.equalsIgnoreCase("CPUs"))	nCPUs = Integer.parseInt(value);
			
		// Assembly/Instantiation
			else if(key.equalsIgnoreCase("SKIP_ASSEMBLY")) {
				if(value.equals("1"))	bSkipAssembly = true;
				else	 				bSkipAssembly = false;
			}
			else if(key.equalsIgnoreCase("USE_TRANS_NAME")) 	bUseTransNames = value.equals("1");
			else if(key.equalsIgnoreCase("CLIQUE"))				asmObj.strClique = value;
			else if(key.equalsIgnoreCase("CLIQUE_BLAST_EVAL"))	asmObj.strCliqueBlastEval = value;
			else if(key.equalsIgnoreCase("CLIQUE_BLAST_PARAMS"))asmObj.strCliqueBlastParam = value;
			else if(key.equalsIgnoreCase("SELF_JOIN"))			asmObj.strSelfJoin = value;
			else if(key.equalsIgnoreCase("BURY_BLAST_EVAL"))	asmObj.strBuryBlastEval = value;
			else if(key.equalsIgnoreCase("BURY_BLAST_IDENTITY"))asmObj.strBuryBlastEval = value;
			else if(key.equalsIgnoreCase("BURY_BLAST_PARAMS"))	asmObj.strBuryBlastEval = value;
			else if(key.equalsIgnoreCase("CAP_ARGS"))			asmObj.strCAPArgs = value;
			else if(key.equalsIgnoreCase("TC_BLAST_EVAL"))		asmObj.strTCBlastEval = value;
			else if(key.equalsIgnoreCase("TC_BLAST_PARAMS"))	asmObj.strTCBlastParams = value;
			else if(key.equalsIgnoreCase("User_EST_selfblast"))	asmObj.strUserESTSelfBlast = value;
			else if(key.matches("TC[0-9]+")) 					asmObj.tcList.add(value);
			
			// Assign best anno
			else if(key.equalsIgnoreCase("Anno_SwissProt_pref")) { 
				if (value.equals("1") || value.equals("0")) annoObj.strSPpref = value;
				else {
					Out.PrtWarn("Anno_SwissProt_pref must be 0 or 1 - ignore line");
					cntErrors++;
				}
			}
			// Remove {ECO.....
			else if(key.equalsIgnoreCase("Anno_Remove_ECO")) { 
				if (value.equals("1") || value.equals("0")) annoObj.strRmECO = value;
				else {
					Out.PrtWarn("Anno_Remove_ECO must be 0 or 1 - ignore line");
					cntErrors++;
				}
			}
			else if(key.equalsIgnoreCase("Anno_Prune_type")) { // CAS331
				if (value.equals("1") || value.equals("0") || value.equals("2")) annoObj.strPruneType = value;
				else {
					Out.PrtWarn("Anno_Prune_type must be 0, 1 or 2 - ignore line");
					cntErrors++;
				}
			}
	// ORF finding
			else if(key.equalsIgnoreCase("Anno_ORF_alt_start")) {
				if (value.equals("1") || value.equals("0")) annoObj.strORFaltStart = value;
				else {
					Out.PrtWarn("Anno_ORF_alt_start must be 0 or 1 - ignore line");
					cntErrors++;
				}
			}
			else if(key.equalsIgnoreCase("Anno_ORF_hit_evalue")) {
				if (isDouble("Anno_ORF_hit_evalue", value)) annoObj.strORFhitEval = value;
			}
			else if(key.equalsIgnoreCase("Anno_ORF_hit_sim")) {
				if (isInteger("Anno_ORF_hit_sim", value)) annoObj.strORFhitSim = value;
			}
			else if(key.equalsIgnoreCase("Anno_ORF_len_diff")) {
				if (isDouble("Anno_ORF_len_diff", value)) annoObj.strORFlenDiff = value;
			}
			else if(key.equalsIgnoreCase("Anno_ORF_mk_diff")) {
				if (isDouble("Anno_ORF_mk_diff", value)) annoObj.strORFmkDiff = value;
			}
			else if(key.equalsIgnoreCase("Anno_ORF_train_min_set")) {
				if (isInteger("Anno_ORF_train_min_set", value)) annoObj.strORFtrainMinSet = value;
			}
			else if(key.equalsIgnoreCase("Anno_ORF_train_CDS_file")) {
				annoObj.strORFtrainCDSfile = checkFile(value, FileC.dPROJ, FileC.fFASTA);
			}
			
		// Similarity CAS314 change keywords - not backward compatible
			// Even if not blast is selected, save any parameters in case they reset
			else if(key.equalsIgnoreCase("Anno_pairs_limit")) {
				if (Static.isInteger(value)) annoObj.nPairsLimit = Integer.parseInt(value);
				else annoObj.nPairsLimit = -1;
			}
			else if(key.equalsIgnoreCase("Anno_pairs_blastn"))  {
				if (value.contentEquals("1")) annoObj.bBlastn=true;
			}
			else if(key.equalsIgnoreCase("Anno_pairs_tblastx")) {
				if (value.contentEquals("1")) annoObj.bTblastx=true;
			}
			else if(key.equalsIgnoreCase("Anno_pairs_blastp")) {
				if (value.contentEquals("1")) annoObj.bBlastp=true;
			}
			else if (key.equalsIgnoreCase("Anno_pairs_tblastx_args")) annoObj.self_tblastx_params = value;
			else if (key.equalsIgnoreCase("Anno_pairs_blastn_args"))  annoObj.self_blastn_params = value;
			else if (key.equalsIgnoreCase("Anno_pairs_blastp_args"))  annoObj.self_blastp_params = value;
			else if (key.equalsIgnoreCase("Anno_pairs_blastp_pgm"))	  annoObj.self_blastp_pgm = value;
			
		// GO
			else if(key.equalsIgnoreCase("Anno_GO_DB")) 		strGODB = value;
			else if(key.equalsIgnoreCase("Anno_No_GO")) 		strNoGO = value; // CAS331 add
			else if(key.equalsIgnoreCase("Anno_SLIM_SUBSET")) 	strSlimSubset = value;
			else if(key.equalsIgnoreCase("Anno_SLIM_OBOFile")) 	strSlimFile = value;
			else isAnnoDB=true;
			
			if (!isAnnoDB) return true; // if none of the above match...
			
		// annoDB get index
			if (key.startsWith("#")) {
				isSel = false;
				key = key.substring(1);
			}
			
			Pattern fasta =  Pattern.compile("Anno_(\\w+)_(\\d+)"); 
			Matcher x = fasta.matcher(key);
			if (!x.find()) {
				if (isSel) return true; // # comment -- ignore
				else {
					Out.PrtWarn("Invalid sTCW.cfg keyword: " + key);
					cntErrors++;
					return false;
				}
			}
			int index= -1;
			int num = Integer.parseInt(x.group(2));
			if (numAnnoMap.containsKey(num)) 
				index = numAnnoMap.get(num);
			else {
				for (int i=0; i<NUMANNODBS; i++)
					if (annoArray[i]==null) {
						index=i;
						break;
					}
				if (index== -1) {
					Out.PrtError("Exceeded limit of annoDBs -- " + NUMANNODBS);
					cntErrors++;
				}
				numAnnoMap.put(num, index);
				annoArray[index] = new AnnodbData();
			}
			
			if(key.startsWith("Anno_DBtaxo_")) { 
				annoArray[index].strTaxo = value;
				annoArray[index].setSelected(isSel);
			}
			else if(key.startsWith("Anno_DBtab_")) { 
				annoArray[index].strTabularFile = checkFile(value, FileC.dPROJ, FileC.fTAB);
				annoArray[index].setSelected(isSel);
			}
			else if(key.startsWith("Anno_DBsearch_pgm_")) { 
				if (value.equals("TCW Select")) value = "diamond"; // don't use TCW Select anymore
				if (value.equals("diamond") && !BlastArgs.isDiamond()) value = "blast"; // diamond part of package now
				annoArray[index].searchPgm = value;
				annoArray[index].setSelected(isSel);
			}
			else if(key.startsWith("Anno_DBfasta_")) { 
				annoArray[index].strFastaDB = checkFile(value, FileC.dANNO, FileC.fFASTA);
				annoArray[index].setSelected(isSel);
			}
			else if(key.startsWith("Anno_DBargs_")) { 
				annoArray[index].strParams = value;
				annoArray[index].setSelected(isSel);
			}
			else if(key.startsWith("Anno_DBdate_")) { //AnnoDB
				annoArray[index].strDate = value;
				annoArray[index].setSelected(isSel);
			}
			else {
				Out.PrtWarn("Invalid sTCW.cfg keyword: " + key);
				cntErrors++;
				return false;
			}
			return true;
		}
		private boolean isDouble(String msg, String d) {
			if (Static.isDouble(d)) return true;
			Out.PrtWarn(msg + " the value '" + d + "' is not a floating point number");
			cntErrors++;
			return false;
		}
		private boolean isInteger(String msg, String d) {
			if (Static.isInteger(d)) return true;
			Out.PrtWarn(msg + " the value '" + d + "' is not an integer");
			cntErrors++;
			return false;
		}
		private void importAnnoDBs(File source) {
			try {
			    System.err.println("Reading annoDBs from " + source.getPath());
				String line = "", key, value;
				BufferedReader projReader = new BufferedReader(new FileReader(source));
				
				numAnnoMap.clear();
				AnnodbData [] tempAnnos = new AnnodbData[NUMANNODBS];
				for(int x=0; x<tempAnnos.length; x++) tempAnnos[x] = null;
				
				while((line = projReader.readLine()) != null) {
					if (line.startsWith("#")) line = line.substring(1).trim(); 
					if(line.indexOf('=') == 0) continue;
					
					String [] lineVal = line.split("=");
					if(lineVal.length < 2)  continue;
					
					key = lineVal[0].trim();
					value = lineVal[1].trim();
						
					if(key.startsWith("Anno_DBtaxo_") || key.startsWith("Anno_DBfasta_") || 
					   key.startsWith("Anno_unitrans_DBblast_") ||
					   key.startsWith("Anno_DBargs_") || 
					   key.startsWith("Anno_DBdate_") || key.startsWith("Anno_DBsearch_pgm_") ||
					   key.startsWith("Anno_GO_DB") || key.startsWith("Anno_SwissProt"))
					{
						readSTCWkeyVal(key, value, tempAnnos);	
					}
				}
				projReader.close();
				
				// does not replace if already has the file name
				for(int x=0; x<tempAnnos.length; x++) {
					if(tempAnnos[x] == null) continue;
					
					boolean found = false;
					for(int y=0; y<getNumAnnoDBs() && !found; y++) {
						if (getAnnoDBAt(y)!=null) 
							found = getAnnoDBAt(y).getFastaDB().equals(tempAnnos[x].getFastaDB());
					}
					if(!found) {
						tempAnnos[x].setDefaults(); // CAS315
						annoObjList.add(tempAnnos[x]);
					}
				}
			}
			catch(Exception e) {ErrorReport.prtReport(e, "Error importing annoDBs");}
		}
		
	}
	
	// The manager data for a project consists of the following arrays of data
	private Vector<SeqData> seqObjList = new Vector<SeqData> ();
	private Vector<CountData> countObjList = new Vector<CountData> ();
	private Vector<AnnodbData> annoObjList = new Vector<AnnodbData> ();
	private HashMap<String, CountData> repMap = new HashMap<String, CountData> ();
	
	private AsmData asmObj = new AsmData ();
	private AnnoData annoObj = new AnnoData ();
	private STCWmethods stcwObj = new STCWmethods();
	private LIBmethods libObj = new LIBmethods();
	
	// for reading sTCW.cfg
	private HashMap <Integer, Integer> numAnnoMap = new HashMap <Integer, Integer>  ();
	private int cntErrors=0;
	
	//Common Settings
	private String strProjDir = "";
	private String strProjID = "";
	private String strTCWdb = "";

	private int nCPUs = -1;
	private boolean bSkipAssembly = true;
	private boolean bUseTransNames = false;
	private String strGODB = "";
	private String strNoGO="0"; //CAS331
	private String strSlimSubset="";
	private String strSlimFile="";
	
	//Load status flags
	private boolean bLibLoaded = false;
	private boolean bInstantiated = false;
	
	private boolean isAAstcw = false;
	private HostsCfg hostObj=null;
	private ManagerFrame theManFrame=null;
}
