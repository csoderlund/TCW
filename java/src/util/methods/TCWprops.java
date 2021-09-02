package util.methods;

import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Set;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;

import sng.database.Globals; // cmp does not access anything that accesses this, otherwise, would crash
import util.database.Globalx;
import util.file.FileHelpers;

/**
 * Parse and store the sTCW.cfg and LIB.cfg properties. 
 * This is a mess. Everything uses it in a different way. 
 */

public class TCWprops
{	
	public enum PropType {
		Lib, Assem, Annotate, Cmp
	}
	private static Properties mProps = null;
	private static Properties mLibPropsDef = null;
	private PropType mType;
	public Vector<Properties> mLibProps = null;
	public TreeSet<String> mUserKeys; 			// keys actually set by the user for assembly
	private HashMap<String, String> aUserKeys; 	// keys-value set by user for annotation

	public TCWprops(PropType type)
	{
		mType = type;
		
		if (mProps==null) mProps = new Properties(); // mProps is Static! Cleared when new db loaded

		mUserKeys = new TreeSet<String>();
		
		switch (mType)
		{
		case Lib:
			mProps.setProperty("STCW_db", ""); 
			
			// Must also add to the list in Library.java!
			mLibPropsDef = new Properties();
			mLibPropsDef.setProperty("MIN_SEQ_LEN", "50");
			mLibPropsDef.setProperty("fiveprimesuf",  Globals.def5p);
			mLibPropsDef.setProperty("threeprimesuf", Globals.def3p);		
			mLibPropsDef.setProperty("libid", "");				
			mLibPropsDef.setProperty("translib", "");				
			mLibPropsDef.setProperty("ctglib", "0");				
			mLibPropsDef.setProperty("reps", "");				
			mLibPropsDef.setProperty("title", "");				
			mLibPropsDef.setProperty("seqfile", "");				
			mLibPropsDef.setProperty("qualfile", "");				
			mLibPropsDef.setProperty("expfile", "");				
			mLibPropsDef.setProperty("source", "");				
			mLibPropsDef.setProperty("organism", "");				
			mLibPropsDef.setProperty("cultivar", "");				
			mLibPropsDef.setProperty("strain", "");				
			mLibPropsDef.setProperty("tissue", "");				
			mLibPropsDef.setProperty("stage", "");				
			mLibPropsDef.setProperty("treatment", "");				
			mLibPropsDef.setProperty("year", "");				
			mLibPropsDef.setProperty("sourcelink", "");				
			//mLibPropsDef.setProperty("default_qual", "18");
			mLibPropsDef.setProperty("prefix", "");
			mProps.setProperty("DEBUG", "0");
			break;
			
		case Assem:
			// set defaults for all possible properties so we can flag unknown properties in the cfg file
			// except for TCNN...
			// these are also used by annotator
			mProps.setProperty("STCW_db", ""); 	
			mProps.setProperty("SingleID", "");
			mProps.setProperty("CPUs", "1");
			mProps.setProperty("DEBUG", "0");
			
			mProps.setProperty("ALLOW_REV_BURIES","1");
			mProps.setProperty("BASECALL_ERROR_RATE", ".005");
			mProps.setProperty("BLAST_BURY_MISMATCH", "3");
			mProps.setProperty("BLAST_NATIVE_THREADING","0");
			mProps.setProperty("BLAST_TYPE", "Megablast");
			mProps.setProperty("BURY_BLAST_EVAL","1e-50");
			mProps.setProperty("BURY_BLAST_IDENTITY","99");
			mProps.setProperty("CAP_ARGS","-p 85 -y 70 -b 80 -o 49 -t 10000");
			mProps.setProperty("CAP_CMD", getExtDir() + "/CAP3/cap3");
			mProps.setProperty("CAP_BURY_MIN_DEPTH","5");
			mProps.setProperty("CAP_BURY_MAX_HANG","10");
			mProps.setProperty("CLIQUE", "100 98 20");
			mProps.setProperty("CLIQUE_BLAST_EVAL","1e-20");
			mProps.setProperty("CLIQUE_SIZE_SING", "2");
			mProps.setProperty("CLIQUE_SIZE_PAIR", "4");
			mProps.setProperty("DO_INITIAL_BURY","1");
			mProps.setProperty("DO_CAP_BURY","1");
			mProps.setProperty("DO_RECAP","1");
			mProps.setProperty("DO_CLIQUES","1");
			mProps.setProperty("EXTRA_CONFIRM","2");
			mProps.setProperty("EXTRA_RATE",".005");				
			mProps.setProperty("EXTRA_SCORE",".001");				
			mProps.setProperty("FDB_ARGS","-pF -oT");
			mProps.setProperty("HEURISTICS", "1");
			mProps.setProperty("IGNORE_HPOLY","3");
			mProps.setProperty("INDEL_CONFIRM","2");
			mProps.setProperty("libraries", "");				
			mProps.setProperty("MAX_CLIQUE_SIZE_SING", "600");
			mProps.setProperty("MAX_CLIQUE_SIZE_PAIR", "400");				
			mProps.setProperty("MAX_CAP_SIZE", "1500");				
			mProps.setProperty("MAX_MERGE_LEN","0");
			mProps.setProperty("MIN_UNBURIED","0");
			mProps.setProperty("NO_TEST4", "0");			
			mProps.setProperty("POOR_ALIGN_PCT","97"); 
			mProps.setProperty("RECAP_ARGS","-p 70 -y 70 -b 80 -o 49 -t 10000");
			mProps.setProperty("REQUIRE_TWO_BRIDGE", "0");
			mProps.setProperty("SELF_JOIN", "50 97 20");
			mProps.setProperty("SKIP_ASSEMBLY", "1");
			mProps.setProperty("SNP_CONFIRM","2");
			mProps.setProperty("SNP_SCORE",".001");
			mProps.setProperty("TC1","200 98 20");
			mProps.setProperty("TC2","200 98 20");
			mProps.setProperty("TC3","150 97 20");
			mProps.setProperty("TC4","150 97 20");
			mProps.setProperty("TC5","100 97 20");
			mProps.setProperty("TC6","100 97 20");
			mProps.setProperty("TC_BLAST_EVAL","1e-20");
			mProps.setProperty("TEST_CTG","");
			mProps.setProperty("UNBURY_SNP","1");
			mProps.setProperty("USE_TRANS_NAME","0");				
			mProps.setProperty("User_EST_selfblast", "");
			
			break;
			
		case Annotate:
			/******************************************
			 * All defaults are here. User supplied values are written to aUserKey.
			 * Reflect changes in 
			 *	 CfgAnno - read sTCW.cfg for execution and sends values to appropriate methods (e.g. DoBlast)
			 * 		Defaults: Pair search args are set in CfgAnno and DB search args set in DoBlast
			 *   ManagerData - read sTCW.cfg for interface and sends values to interface methods
			 * 		Defaults: STCWmethods: some defaults are hard coded. 
			 * 			      Search Args: in STCWmethods, EditAnnoPanel, AnnoOptionsPanel
			 *	 CAS314 Search args must have a value 
			*  getProperty() 		returns default; 
			*  getAnnoProperty() 	returns user supplied or default
			*  getNotDefault() 		only return value if user supplied.
			*****************************************************/
			mProps.setProperty("STCW_db", ""); 
			mProps.setProperty("SingleID", ""); 	
			mProps.setProperty("CPUs", "1");							
			
			mProps.setProperty("Anno_flanking_region", "30"); // not documented; used for setFilterOverlap
			mProps.setProperty("Anno_min_bitscore", "-1");    // not documented
			
			mProps.setProperty("Anno_SwissProt_pref", Globals.pSP_PREF); 
			mProps.setProperty("Anno_Remove_ECO", Globals.pRM_ECO); // CAS305
			
			mProps.setProperty("Anno_Prune_type", Globals.pPRUNE); // CAS331
			mProps.setProperty("Anno_No_GO", Globals.pNO_GO); // CAS331
			
			mProps.setProperty("Anno_DBdate", "-");  // Global DBdate if individual DBdata not set   
			
			for (int i=1; i<Globals.numDB; i++) {
				mProps.setProperty("Anno_DBtab_" +   Integer.toString(i), "-"); // CAS314 was "Anno_unitrans_DBblast_"
				mProps.setProperty("Anno_DBfasta_" + Integer.toString(i), "-");
				mProps.setProperty("Anno_DBtaxo_" +  Integer.toString(i), "-");
				mProps.setProperty("Anno_DBargs_" +  Integer.toString(i), "-"); 
				mProps.setProperty("Anno_DBsearch_pgm_" + Integer.toString(i), "-");
				mProps.setProperty("Anno_DBdate_" +  Integer.toString(i), "-");
			}
			
			// self blast  - v314 changed all parameter - not backwards compatible
			mProps.setProperty("Anno_pairs_limit", "1000");
			mProps.setProperty("Anno_pairs_blastn", "0"); 
			mProps.setProperty("Anno_pairs_blastn_args", "-");      
			mProps.setProperty("Anno_pairs_tblastx", "0");
			mProps.setProperty("Anno_pairs_tblastx_args", "-");  
			mProps.setProperty("Anno_pairs_blastp", "0");
			mProps.setProperty("Anno_pairs_blastp_args", "-");  
			mProps.setProperty("Anno_pairs_blastp_pgm", "diamond");
			
			// NOTE defaults are hardcoded in ManagerData!!!
			mProps.setProperty("Anno_ORF_alt_start", "0"); 
			mProps.setProperty("Anno_ORF_hit_evalue", Globals.pHIT_EVAL); 
			mProps.setProperty("Anno_ORF_hit_sim", Globals.pHIT_SIM);     
			
			mProps.setProperty("Anno_ORF_len_diff", Globals.pDIFF_LEN);
			
			mProps.setProperty("Anno_ORF_train_evalue", "1E-75"); // obsolete.
			mProps.setProperty("Anno_ORF_train_min_set", Globals.pTRAIN_MIN); 
			mProps.setProperty("Anno_ORF_train_CDS_file", "-"); // CAS327 was -1
 
			mProps.setProperty("Anno_GO_DB", ""); 
			mProps.setProperty("Anno_SLIM_SUBSET", ""); 
			mProps.setProperty("Anno_SLIM_OBOFile", ""); 
			break;		
		
		case Cmp: // cmp.compile.panels/CompilePanel.java
			mProps.setProperty("MTCW_db", "");
			mProps.setProperty("MTCW_host", "");
			
			// CAS316 AA is always run, but can change search program and parameters
			mProps.setProperty("MTCW_DBsearch_pgm", "diamond"); 
			mProps.setProperty("MTCW_search_params", "");	// set to defaults
			
			// NT is optional, and always uses blastn
			mProps.setProperty("MTCW_run_blastn", "1");
			mProps.setProperty("MTCW_blastn_params", "");	// set to defaults
			
			mProps.setProperty("MTCW_GO_DB", "go_demo");
			
			for (int i=1; i<Globals.numDB; i++) {
				mProps.setProperty("STCW_db" + Integer.toString(i), "-");
				mProps.setProperty("STCW_STCWid" + Integer.toString(i), "-");
				mProps.setProperty("STCW_id" + Integer.toString(i), "-");
				mProps.setProperty("STCW_host" + Integer.toString(i), "-");
				mProps.setProperty("STCW_AAFile" + Integer.toString(i), "-");
				mProps.setProperty("STCW_remark" + Integer.toString(i), "-");
				mProps.setProperty("STCW_prefix" + Integer.toString(i), "-");
			}
			for (int i=1; i<Globals.numDB; i++) {
				mProps.setProperty("CLST_method_name"+ Integer.toString(i), "");
				mProps.setProperty("CLST_method_type"+ Integer.toString(i), "");
				mProps.setProperty("CLST_method_prefix"+ Integer.toString(i), "");
				mProps.setProperty("CLST_file"+ Integer.toString(i), "");
				mProps.setProperty("CLST_comment"+ Integer.toString(i), "");
				mProps.setProperty("CLST_settings"+ Integer.toString(i), ""); // CAS325 missing 's'
			}
			break;
			
		default:
			Out.prtToErr("TCWprops " + type + " " + mType);	
		}		
	}

	public String getProperty(String key) 
	{
		try
		{
			if (mProps.containsKey(key))
			{
				return mProps.getProperty(key);
			}
		}
		catch(Exception e)
		{
			if (key!=null) System.out.println("Warning: check for nonexistent config property " + key);
		}
		return "";
	}

	public void setProperty(String key, String val)
	{
		mProps.setProperty(key, val);
	}

	public boolean containsKey(String key)
	{
		return mProps.containsKey(key);
	}
	public boolean hasSetKey(String key) // CAS325
	{
		return mUserKeys.contains(key);
	}
	public String fixCase(String in) throws Exception
	{
		String out = in;
		if (in.startsWith("tc") || in.startsWith("Tc") || in.startsWith("tC") || in.startsWith("TC")) 
		{
			out = in.toUpperCase();
		}
		else
		{
			for (Object keyObj : mProps.keySet())
			{
				String key = keyObj.toString();
				if (in.equalsIgnoreCase(key))
				{
					out = key;
					break;
				}
			}
		}
		return out;
	}
	/*
	 * Load sTCW.cfg for assembly and library
	 */
	private void clearTCs()
	{
		for (int i = 1; i <= 100; i++)
		{
			String key = "TC" + i;
			if (mProps.containsKey(key))
			{
				mProps.remove(key);
			}
		}
	}
	/***********************
	 *  load for assembly and load library
	 */
	public void load(File f) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(f));
		boolean foundTC = false;
		while (br.ready())
		{	
			String line = br.readLine();
			line = line.replaceAll("#.*","");
			line += " "; // so the split works right 
			String[] fields = line.split("=");
			if (fields.length != 2) continue;

			String key = fields[0].trim();
			String val = fields[1].trim();
			String keyFixedCase = fixCase(key);
			if (key.toLowerCase().startsWith("count_")) continue;
			
			if (keyFixedCase.startsWith("TC"))
			{
				boolean match = false;
				// Note several params start with TC
				for (int i = 1; i <= 100; i++)
				{
					String tck = "TC" + i;
					if (keyFixedCase.equals(tck))
					{
						match=true; break;
					}
				}
				if (match)
				{
					// Found a TC entry, hence clear out the 6 defaulted ones
					if (!foundTC)
					{
						clearTCs();
						foundTC = true;
					}
				}
			}
			if (!key.equals(""))
			{					
				if (mProps.containsKey(keyFixedCase) || keyFixedCase.startsWith("TC"))
				{
					mProps.setProperty(keyFixedCase,val);
					mUserKeys.add(keyFixedCase);
				}
				else if (mType == PropType.Assem )
				{
					if (!key.toUpperCase().startsWith("ANNO"))
					{
						ErrorReport.reportError("sTCW.cfg: Ignoring unknown parameter: " + key);
					}
				}
				else if (mType == PropType.Annotate )
				{
					if (key.toUpperCase().startsWith("ANNO"))
					{
						ErrorReport.reportError(" sTCW.cfg: Ignoring unknown parameter: " + key);
					}
				}					
				else if (mType == PropType.Lib && !mLibPropsDef.containsKey(keyFixedCase))
				{
					ErrorReport.reportError(" sTCW.cfg: Ignoring unknown parameter: " + key);
				}
				else if (mType == PropType.Cmp )
				{
					ErrorReport.reportError(" mTCW.cfg: Ignoring unknown parameter:" + key);
				}		
			}
		}	
		br.close();
		
		fixProps(mProps);
		checkRequiredFields();
		if (mType == PropType.Lib)
		{
			loadLibProps(f);
			TreeSet<String> libIDs = new TreeSet<String>();
			for (Properties p : mLibProps)
			{
				fixProps(p);
				String libid = p.getProperty("libid");
				if (libIDs.contains(libid))
				{
					ErrorReport.die("Duplicate ID " + libid);
				}
				libIDs.add(libid);
			}
		}
	}

	/*
	 * CfgAnno uses this to read sTCW.cfg 
	 * ManagerData reads and writes sTCW.cfg directly
	 */
	public void loadAnnotate(File f) throws Exception
	{	
		int error=0;
		aUserKeys = new HashMap<String, String>  ();
		
		BufferedReader br = new BufferedReader(new FileReader(f));
		while (br.ready())
		{	
			String line = br.readLine();
			line = line.replaceAll("#.*","");
			if (line.trim().length() == 0) continue;
			line += " ";
			String[] fields = line.split("=");
			if (fields.length == 2)
			{
				String key = fields[0].trim();
				if (key.equals("")) continue;
				
				String val = fields[1].trim();
									
				if (mProps.containsKey(key))
				{
					if (aUserKeys.containsKey(key)) {
						ErrorReport.reportError("sTCW.cfg: duplicate parameter:  " + line);
						error++;
					}
					else {
						aUserKeys.put(key, val);
					}
				}
				else if (key.startsWith("Anno"))
				{
					ErrorReport.reportError("sTCW.cfg: Ignoring unknown parameter: " + key);
					error++;
				}
			}
			else {
				ErrorReport.reportError("sTCW.cfg: incorrect line: " + line);
				error++;
			}
		}	
		br.close();
		if (error>0) {
			if (!FileHelpers.yesNo("Continue? ")) ErrorReport.die(" user terminated ");
		}
		fixProps(mProps);
		checkRequiredFields();
	}
	public String getAnnoProperty(String key) {
		if (aUserKeys.containsKey(key)) return aUserKeys.get(key);
		else return mProps.getProperty(key);
	}
	public String getAnnoNotDefault(String key) {
		if (aUserKeys.containsKey(key)) return aUserKeys.get(key);
		else return null;
	}
	public boolean getAnnoIsNotDefault(String key) {
		if (aUserKeys.containsKey(key)) return true;
		else return false;
	}
	/*
	 * Load mTCW.cfg for runMultiTCW
	 */
	public void loadMTCWcfg(File f) throws Exception
	{	
		try {			
			int error=0;
			String line = "";
			BufferedReader br = new BufferedReader(new FileReader(f));
			while ((line = br.readLine()) != null)
			{	
				line = line.replaceAll("#.*","");
				if (line.trim().length() == 0) continue;
				line += " ";
				String[] fields = line.split("=");
				
				if (fields.length!=2) {
					ErrorReport.reportError("mTCW.cfg: incorrect line: " + line);
					error++;
					continue;
				}
				String key = fields[0].trim();
				String val = fields[1].trim();
				
				// Changed 1/20/19 
				key = key.replace("CPAVE", "MTCW");
				key = key.replace("PAVE", "STCW");
				key = key.replace("POG",   "CLST");
				key = key.replace("_blast_", "_search_");
				
				String keyFixedCase = fixCase(key);
				if (!key.equals(""))
				{		
					if (mProps.containsKey(keyFixedCase))
					{
						if (mUserKeys.contains(keyFixedCase)) {
							ErrorReport.reportError("mTCW.cfg: duplicate parameter:  " + line);
							error++;
						}
						else {
							mProps.setProperty(keyFixedCase,val);
							mUserKeys.add(keyFixedCase);
						}
					}
					else if (key.toUpperCase().startsWith("MTCW"))
					{
						ErrorReport.reportError("mTCW.cfg: Ignoring unknown parameter:" + key);
						error++;
					}
				}
			}	
			if (error>0) { // prompt to continue causes VNC to seriously hang
				ErrorReport.reportError("***" + error + " errors in mTCW.cfg -- continuing....");
			}
			br.close();
			fixProps(mProps);
			checkRequiredFields();
		} catch(Exception e) {
		    ErrorReport.reportError(e, "Reading mTCW.cfg");
		}
	}


	public void checkRequiredFields() throws Exception
	{
		if (mType == PropType.Assem)
		{
			parseCapProps();
		}
		for (Enumeration<?> e = mProps.propertyNames(); e.hasMoreElements(); )
		{
			String propName = e.nextElement().toString();
			if (propName.contains("ARGS"))
			{
				String val = mProps.getProperty(propName);
				if (val.contains(">"))
				{
					ErrorReport.die("Configuration file property " + propName + " contains illegal character '>'");
				}
			}
		}
	}
	public void parseCapProps() throws Exception
	{
		String[] sjFields = mProps.getProperty("SELF_JOIN").trim().split("\\s+");
		if (sjFields.length != 3)
		{
			throw(new Exception("Wrong number of parameters in SELF_JOIN setting"));
		}
		mProps.setProperty("MIN_MATCH_SELF", sjFields[0]);
		mProps.setProperty("MIN_ID_SELF", sjFields[1]);
		mProps.setProperty("MAX_HANG_SELF", sjFields[2]);
		
		String[] cFields = mProps.getProperty("CLIQUE").trim().split("\\s+");
		if (cFields.length != 3)
		{
			throw(new Exception("Wrong number of parameters in CLIQUE setting"));
		}
		mProps.setProperty("MIN_MATCH_CLIQUE", cFields[0]);
		mProps.setProperty("MIN_ID_CLIQUE", cFields[1]);
		mProps.setProperty("MAX_HANG_CLIQUE", cFields[2]);		
	}
	public void fixProps(Properties props)
	{
		Enumeration<?> keys = props.propertyNames();
		while (keys.hasMoreElements())
		{
			String key = keys.nextElement().toString();
			String val = props.getProperty(key);
			val = val.replaceAll("#.*", "");
			props.setProperty(key, val.trim());
		}
	}

	public Set<Object> keySet()
	{
		return mProps.keySet();	
	}
	// The lib sections duplicate parameter names so we have to parse separately
	// First break into separate strings, then use the standard property parse
	// on a stream made from the string
	public void loadLibProps(File f) throws Exception
	{
		Vector<String> libSecs = new Vector<String>();
		BufferedReader reader = new BufferedReader(new FileReader(f));
		int i = 0;
		libSecs.add(i, "");
		String line;

		while ((line = reader.readLine()) != null)
		{
			line = line.trim();
			if (line.toLowerCase().startsWith("libid") || line.toLowerCase().startsWith("translib") )
			{
				if (line.contains("__"))
				{
					ErrorReport.die("Dataset name should not contain '__':\n" + line );
				}
				i++;
				libSecs.add(i, "");
			}
			libSecs.set(i, libSecs.get(i) + line + "\n");
		}
		reader.close();
		mLibProps = new Vector<Properties>();
		for (i = 1; i < libSecs.size(); i++)
		{
			mLibProps.add(i - 1, new Properties(mLibPropsDef));
			mLibProps.get(i - 1).load(new ByteArrayInputStream(libSecs.get(i).getBytes()));
			adjustLibProps(mLibProps.get(i-1));
		}
	}
	
	public void adjustLibProps(Properties p)
	{
		p.setProperty("orig_libid", p.getProperty("libid"));
		if (p.containsKey("translib"))
		{
			p.setProperty("libid", p.getProperty("translib"));
			p.setProperty("ctglib","1");
		}
		else
		{
			if (p.containsKey("prefix"))
			{
				String pfx = p.getProperty("prefix").trim();
				p.setProperty("libid",pfx + p.getProperty("libid"));
			}
		}
	}
	
	public static String getExtDir() // this is also in FileHelpers
	{
		if (FileHelpers.isMac()) return Globalx.macDir;
		return Globalx.lintelDir;
	}
	public static void newDB() {
		mProps=null;
		new TCWprops(PropType.Assem);
		new TCWprops(PropType.Annotate);
	}
}
