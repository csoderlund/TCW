package sng.util;

/**************************************
 * Columns are declared here (FieldSeqData, FieldPairsData), 
 * set as visible (selected) (FieldSeqTab, FieldPairsTab), 
 * and returned for query (QueryTab) and display of table (MainTableSort).
 * 
 * CAS322 no change, just tidied up formatting and comments
 */
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashSet;
import java.sql.ResultSet;

import sng.viewer.STCWFrame;
import sng.viewer.panels.seqTable.FieldSeqData;
import sng.viewer.panels.seqTable.FieldSeqTab;
import sng.viewer.panels.seqTable.UIfieldNFold;
import util.methods.Converters;
import util.methods.ErrorReport;
import util.methods.Out;
import util.ui.DisplayFloat;
import util.ui.DisplayInt;
import util.database.Globalx;

/**
 * The listFields is a vector of FieldData objects, which is a private class
 * defined in this file. Each FieldData object represents a column in the table.
 * The columns are mapped to fields in the database.
 */
public class FieldMapper
{   
	// added these for debugging
	static int mapindex=1;
	private int mapNum;
	private String label;
	public String getTitle() { return label + " #" + mapNum;}
	
	// FieldPairData
	public FieldMapper(String label) {
		mapNum=mapindex++;
		this.label = label;
	}
	
	// FieldSeqData 
	public FieldMapper(String label, String [] allLibraries, STCWFrame theFrame)
	{
		mapNum=mapindex++;
		this.label = label;
		
		isSeq = (allLibraries!=null) ? true : false;
		if(allLibraries != null) {
			allLibNames = new Vector<String> ();
			for(int x=0; x<allLibraries.length; x++) 
				allLibNames.add(allLibraries[x]);
		}
		theParentFrame = theFrame;
	}
	 // CAS322 
    public String getDBcolumnByIdx(int idx) {
    	if (visibleFields==null || visibleFields.length==0) return null;
    	if (idx>0 && idx <visibleFields.length) return visibleFields[idx].strField;
    	else return null;
    }
    
    // FieldSeqTab and FieldPairsTab
	public String [] getVisibleFieldNames () {
	    return getNamesFromIDs ( getVisibleFieldIDs ( ) );
	}
	
	/*******************************************
	 * MainTableSort
	 */
    public int getNumFields ( ){
        return listFields.size();
    }
    public String getFieldNameByID ( int nFieldID ){
        return getFieldByID ( nFieldID ).strName;
    }
    public String getDBNameByID ( int nFieldID ){ // - sortByColumn
        return getFieldByID ( nFieldID ).strField;
    }  
    public Object extractFieldByID ( String line, int nField ){
    	FieldData [] fields = getVisibleFields ( );
    	String [] row = line.split("\t");
    	for ( int i = 0; i < fields.length; ++i ) {
    		if ( fields[i] != null && fields[i].nID == nField && i < row.length)
    			return convertFromDBObject2( fields[i], row[i] );
    	}
        return null;
    }
       
    // SeqTableTab and PairTableTab
    public Object extractFieldByID ( Object obj, int nField) {
        Object[] row = (Object[])obj;
	    	FieldData [] fields = getVisibleFields ( );
	    	for ( int i = 0; i < fields.length; ++i ) {
	    		if ( fields[i].nID == nField && i < row.length) 
	    			return row[i];
	    	}
        return null;
    }
    // FieldPairsData
    public void setFieldRequired ( int nID ) {
    	getFieldByID ( nID ).bRequired = true;
    }
    
    // FieldSeqTab and FieldMapper
    public boolean isNFoldField( int id ) {
    	return id >= FieldSeqData.N_FOLD_LIB && id<= FieldSeqData.N_FOLD_LIB_LIMIT;
    }  
    
    // FieldSeqData, FieldPairsData
    public void setDefaultFieldIDs ( int [] fieldIDs ) {        
        defaultFieldIDs = fieldIDs;
    }    
    
    // called at startup by FieldTab.setUIFromFields
    public boolean getFieldRequiredByName ( String strName )
    {
    	FieldData f = getFieldByName ( strName );
    	if (f==null) return false; // not all databases have same columns
    	return f.bRequired;
    }   
    // called by FieldTab.getFieldsFromUI
    public boolean hasFieldName(String name) {
    	String[] fieldNames = getFieldNames();
    	for (int i = 0;  i < fieldNames.length;  i++)
    		if (name.equals(fieldNames[i])) return true;
    	return false;
    }
 // FieldTab, FieldMapper
    public String [] getFieldNames ( ) {
        String [] names = new String [ listFields.size() ];
        int i = 0;
        Iterator<FieldData> iter = listFields.iterator();
        while ( iter.hasNext() ) {
            FieldData theField = iter.next();        
            names [i] = theField.strName;
            ++i;
        }
        return names;
    }
    // XXX FieldSeqTab
    public void setVisibleField ( Object[] fieldNames )
    {
    	visibleFields = null; // To force it to recreate when getVisibleFields() is called
        visibleFieldIDs = getIDsFromNames ( fieldNames );
        Arrays.sort(visibleFieldIDs);
    }
  
    public void setVisibleFieldNames ( Object[] fieldNames )
    {
        visibleFieldIDs = getIDsFromNames ( fieldNames );
        Arrays.sort(visibleFieldIDs);
    }
    // FieldSeqTab and FieldPairsTab
    public boolean isFieldVisible ( String strFieldName )
    {
	    FieldData f = getFieldByName ( strFieldName );
	    if (f == null) 	return false;
       	return isFieldVisible(f.nID);
    }
    private FieldData getFieldByName ( String str ) {
    	if (str==null) return null;
    		
        int n = getIndexForName ( str );
        if ( n >= 0)  return listFields.get( n );
        
		if (theParentFrame!=null && isSeq) { // Nfold column may not be added yet
    		FieldSeqTab fieldObj = theParentFrame.getFieldSeqTab();
    		if (fieldObj!=null) {
	    		UIfieldNFold nfoldObj = fieldObj.getNFoldObj();
	    		if (nfoldObj!=null) {
	        		n = nfoldObj.getIDForName(str);
	        		if (n>=0) {
	        			addFloatField(n, str, null, null, null, null, null);
	        			return listFields.get( getIndexForID(n) );
	        		}
	        	}
    		}
		}
	
	   	return null;
    }      
    private FieldData getFieldByID ( int nField )
    {
        int nIdx = getIndexForID ( nField );
        if ( nIdx < 0 ) nIdx = 0;
        return listFields.get( nIdx );
    }    
    private int getIndexForName ( String strName )
    {
    	int rc=-1;
        if (isEmpty(strName) ) return -1;
        
        for ( int i = 0; i < listFields.size(); ++i ) {
            FieldData curField = listFields.get(i);
            if ( curField.strName.equals( strName ) ) {
                rc = i;
                break;
            }
        }
        return rc;
    }   
    private int getIndexForID ( int nID )
    {
        for ( int i = 0; i < listFields.size(); ++i ) {
            FieldData curField = listFields.get(i);
            if ( curField.nID == nID )
                return i;
        }
        return -1;
    }       
    private int [] getIDsFromNames ( Object [] fields )
    {
        Vector<Integer> ids = new Vector<Integer> ();
        for ( int i = 0; i < fields.length; ++i )
        {
            String strCur = (String)fields[i];
            if(getFieldByName(strCur) != null)
            		ids.add(getFieldByName ( strCur ).nID);
        }
        
        int [] retVal = new int[ids.size()];
        for(int x=0; x<retVal.length; x++) retVal[x] = ids.get(x);
        return retVal;
    }      
    private String [] getNamesFromIDs ( int [] fields )
    {
        String [] names = new String [ fields.length ];
        for ( int i = 0; i < fields.length; ++i ) {
            names[i] = getFieldByID ( fields[i] ).strName;
        }
        return names;
    }       
    private FieldData [] getVisibleFields ( )
    {   
      	if ( visibleFields != null ) return visibleFields;
      	
      	CmpFieldData fieldCmp = new CmpFieldData();
 
      	Vector<FieldData> tempVisibleFields = new Vector<FieldData> ();
        for (FieldData theField : listFields ) {
            if ( isFieldVisible ( theField.nID ))
            	tempVisibleFields.add(theField);
        }
        visibleFields = tempVisibleFields.toArray(new FieldData[0]);
        Arrays.sort(visibleFields, fieldCmp);
 
        return visibleFields;
    }   
    private boolean isFieldVisible ( int nID )
    {
    	int ids [] = getVisibleFieldIDs ();
    	
    	for ( int i = 0; i <  ids.length; ++i )	
    		if ( ids[i] == nID ) return true;
    	return false;
    }    
    // ContigListTab and ContigPairListTab
    public int [] getVisibleFieldIDs ( )
    {
        if ( visibleFieldIDs == null ) {
	        if ( defaultFieldIDs != null ) return defaultFieldIDs;
	        else    return getAllFieldIDs ( );
        }
        else  return visibleFieldIDs;
    }
    // NFoldColumnSelectPanel, FieldContigTab, FieldMapper
    public int [] getAllFieldIDs ( )
    {
    	int [] ret = new int [ listFields.size() ];
    	for ( int i = 0; i < listFields.size(); ++i )
    	{
    		ret [i] = (listFields.get(i)).nID;
    	}
    	return ret;
    } 
    // FieldContigTab setPrefsFromMapper and setSelectedLibsFromMapper, FieldParisTab
    public String [] getVisibleFieldIDsStr ( )
    {
    	int ids [] = getVisibleFieldIDs ();
    	Vector<String> results = new Vector<String> ();
    	String tempID;
    	for ( int i = 0; i < ids.length; ++i ) {
    		tempID = String.valueOf( ids[i] );
    		
    		if(!results.contains(tempID))
    			results.add(tempID);
    	}
    	return results.toArray(new String [0]);
    }     
    // STCWFrame, FieldSeqTab
    public void setVisibleFieldIDsList ( String [] strVisFieldIDs )
    {
    	if ( strVisFieldIDs == null || strVisFieldIDs.length == 0 || 
    		( strVisFieldIDs.length == 1 && isEmpty( strVisFieldIDs[0] ) ) )
    	{
    		visibleFieldIDs = defaultFieldIDs;
        	Arrays.sort(visibleFieldIDs);
    	}
    	else {
    		// parse each id and validate it
    		Vector<Integer> fieldsInts = new Vector<Integer> ();
    		for ( int i = 0; i < strVisFieldIDs.length; ++i ) {
    			int nFieldID = Integer.parseInt( strVisFieldIDs[i] ); 
    		    if ((getIndexForID(nFieldID)>=0) || isNFoldField(nFieldID)) {
    		    		fieldsInts.add(nFieldID);
    		    }
    		}
    		visibleFieldIDs = Converters.intCollectionToIntArray ( fieldsInts );
        	Arrays.sort(visibleFieldIDs);
	   	}
	}  
    /**
     * Methods for Groups, where a group is Label for a set of columns on the Select Column page
     */
    // FieldTab while building UI
    public String [] getGroupNames() {
    	return listGroups.toArray(new String[0]);
    }
    // FieldTab while building UI
    public String [] getGroupDescriptions() {
    	Vector<String> groupDescriptions = new Vector<String>();
    	
    	Iterator<FieldData> iter = listFields.iterator();
    	while (iter.hasNext()) {
    		FieldData data = iter.next();
    		if (data.strGroupDescription != null 
    			&& !groupDescriptions.contains(data.strGroupDescription))
    		{
    			groupDescriptions.add(data.strGroupDescription);
    		}
    	}
    	return groupDescriptions.toArray(new String[0]);
    }
    // FieldTab while building UI
    public String [] getFieldNamesByGroup(String strGroupName) {
    	Vector<String> fieldNames = new Vector<String>();
    	
    	Iterator<FieldData> iter = listFields.iterator();
    	while (iter.hasNext()) {
    		FieldData data = iter.next();
    		if (strGroupName.equals(data.strGroup) && !fieldNames.contains(data.strName)) {
    			fieldNames.add(data.strName);
    		}
    	}    	
    	return fieldNames.toArray(new String[0]);
    }
    // FieldTab
    public Integer [] getFieldIDsByGroup(String strGroupName) {
    	Vector<Integer> fieldIDs = new Vector<Integer> ();
    	
    	Iterator<FieldData> iter = listFields.iterator();
    	while(iter.hasNext()) {
    		FieldData data = iter.next();
    		if (strGroupName.equals(data.strGroup) && !fieldIDs.contains(data.nID))
    			fieldIDs.add(data.nID);
    	}
    	return fieldIDs.toArray(new Integer[0]);
    }
    // FieldTab
    public void hideIDRange(int min, int max) {
    	if(visibleFields == null)
    		return;
    	
    	Vector<FieldData> newVals = new Vector<FieldData> ();
    	for(int x=0; x<visibleFields.length; x++) {
    		if(visibleFields[x].nID < min || visibleFields[x].nID > max)
    			newVals.add(visibleFields[x]);
    	}
    	visibleFields = newVals.toArray(new FieldData[0]);
    }
    // FieldTab while building UI
    public String [] getFieldDescriptionsByGroup(String strGroupName) {
    	Vector<String> fieldNames = new Vector<String>();
    	
    	Iterator<FieldData> iter = listFields.iterator();
    	while (iter.hasNext()) {
    		FieldData data = iter.next();
    		if (strGroupName.equals(data.strGroup)) {
    			fieldNames.add(data.strDescription);
    		}
    	}    	
    	return fieldNames.toArray(new String[0]);
    }
    /**
     * Methods for mapping the columns to the database
     * which include ones that build the SQL
     */
    private String convertFromDBStr ( FieldData data, Object obj )
    {
    	if ( obj == null ) return ""; //return null;
    	
        switch ( data.nType ) {
        case FieldData.PERCENT_TYPE:
             obj = new DisplayFloat ( obj, 100 ); // Round according to user request on Column page
             break;
        case FieldData.FLOAT_TYPE:          
             obj = new DisplayFloat ( obj );
             break;
        case FieldData.BOOLEAN_TYPE:
            if (obj instanceof Boolean) { 
	            Boolean b = (Boolean)obj;
	            obj = (b) ? "Yes" : "No";
            }
            break;
        }
        return obj.toString();
    }
    private Object convertFromDBObject ( FieldData data, Object obj )
    {
    	if ( obj == null ) return obj;
    	
        switch ( data.nType ) {
        case FieldData.PERCENT_TYPE:
             obj = new DisplayFloat ( obj, 100 );
             break;
        case FieldData.FLOAT_TYPE:          
             obj = new DisplayFloat ( obj );
             break;
        case FieldData.RECORD_ID: 
        case FieldData.INTEGER_TYPE: 
        	obj = new DisplayInt ( obj.toString() );
        	break;
        case FieldData.BOOLEAN_TYPE:
            if (obj instanceof Boolean) { 
	            Boolean b = (Boolean)obj;
	            String x = (b) ? "Yes" : "No";
	            obj = x;
            }
            break;
        }
        return obj;
    }
    // For MainTable Sort
    private Object convertFromDBObject2 ( FieldData data, Object obj ) // don't multiply percent values
    {
    	if(obj.toString().length() == 0) return null;
        switch ( data.nType )
        {
        	case FieldData.PERCENT_TYPE:
        		return new DisplayFloat ( obj );
        	default :
        		return convertFromDBObject( data, obj );
        }
    }
    // XXX FieldSeqData
    // Returns the list of visible field names formatted to be part of a SQL statement
    // CAS335 add Nfold libs here without replicate
    public String getDBFieldList (UIfieldNFold nFoldObj )
    {
    	HashSet <String> tpmVal = new HashSet <String> ();
    	
    	String LN = Globalx.LIBRPKM;
    	
    	if (nFoldObj!=null) {
    		String [] libs = nFoldObj.getFoldLibs();
    	
	 	    if(libs != null && libs.length > 0) {    		
	 	    	for(int x=0; x<libs.length; x++) 
	 	    		tpmVal.add(LN + libs[x]);
	 	    }
    	}
    	String colList = "";
    	FieldData [] fields = getVisibleFields ( );
    	
        for ( int i = 0; i < fields.length; ++i )
        {
            if ( colList != "" ) colList += ", ";
 
            if ( fields[i] == null || fields[i].strField == null || fields[i].strField.length()==0) {
            	colList += "NULL"; // Place holder so we have the correct number of columns; n-fold
            }
            else if (fields[i].nID==FieldSeqData.SEQ_ID_FIELD) {
            	colList += fields[i].strTable + "." + fields[i].strField;
            }
            else if (fields[i].nID>=FieldSeqData.LIBRARY_TPM_ALL && fields[i].nID<=FieldSeqData.LIBRARY_TPM_ALL) {
            	colList += fields[i].strTable + "." + fields[i].strField;
            	if (tpmVal.contains(fields[i].strField)) tpmVal.remove(fields[i].strField);
            }
            else if ( fields[i].strSubQuery != null ) { // CAS304  strSubquery for 3 types of Best Hits 
            	colList += fields[i].strSubQuery;
            }
            else if ( fields[i].strTable != null ) {
                colList += fields[i].strTable + "." + fields[i].strField;
            }
            else {
                colList += fields[i].strField;  
            }
        }
        for (String col : tpmVal) {
        	if ( colList != "" ) colList += ", ";
        	colList += "contig." + col;
        }
        return colList;
    }      
    /*
     * sng.util.RunQuery.runDBQuery
     * The database has been queried. The fields array has all the database fields
     * that are to be shown. Hence, the values can be extracted from the result set.
     */
    public String getObjFromSeqResultSet (ResultSet rs, int nRow, FieldSeqTab fieldObj) 
    {    	
    	FieldData [] fields = getVisibleFields ( );
    	int i=0;
   
    	try {
    		StringBuffer sb = new StringBuffer();
			String nu = null;
			
			for (i = 0; i < fields.length; ++i )
	    	{
				if( fields[i] == null)  {
					sb.append(nu); 
				}
				else if ( fields[i].nType == FieldData.RECORD_ID ) {
					sb.append(nRow );
				}
	    		else if (fields[i].nID >= FieldSeqData.N_FOLD_LIB && fields[i].nID <= FieldSeqData.N_FOLD_LIB_LIMIT) {
	    			UIfieldNFold nFoldObj = fieldObj.getNFoldObj();
	    			String delim = nFoldObj.getDelim();
	    			
	    			String [] libs = fields[i].strName.split(delim);
	    				
	    			double A = rs.getDouble(Globalx.LIBRPKM + libs[0]);
	    			double B = rs.getDouble(Globalx.LIBRPKM + libs[1]);
	    			
	    			double result = nFoldObj.getFC(A, B);// CAS335 add log2(A/B)
	    			
	    			sb.append(result);
	    		}
	    		else { // everything else
	    			Object data = rs.getObject( fields[i].strField );
		    		sb.append(convertFromDBStr( fields[i], data )); 
	    		}
				sb.append(MainTable.ROW_DELIMITER);
	    	}
	    	return sb.toString();
    	}
    	catch (Exception e) {
			ErrorReport.prtReport(e, "Parsing results: Column " + i + " '"+ fields[i].strField + "'");
			return null;
    	}
    }  
    public String getObjFromPairResultSet ( ResultSet rs, int nRow) throws Exception
    {    	
    	FieldData [] fields = getVisibleFields ( );
    	StringBuffer sb = new StringBuffer();
    	String nu=null;
    	for ( int i = 0; i < fields.length; ++i )
    	{
    		if( fields[i] == null) 
    			sb.append(nu);
    		else if ( fields[i].nType == FieldData.RECORD_ID ) // has empy strField
    			sb.append(nRow);
    		else 
    		{
	     	Object data = rs.getObject( fields[i].strField );
	    		sb.append(convertFromDBStr( fields[i], data )); 
    		}
    		sb.append(MainTable.ROW_DELIMITER);
    	}
    	return sb.toString();
    }
    // FieldSeqData
    // Returns true if any of the fields match the input table
    public boolean haveDBFieldWithTable ( String strTable )
    {
        if ( strTable == null )
            return false;
        
        Iterator<FieldData> iter = listFields.iterator();
        while ( iter.hasNext() )
        {
            FieldData theField = iter.next();
            if ( theField.strTable != null && 
                   theField.strTable.equals(strTable) && isFieldVisible ( theField.nID ) )
                return true;
        }
        return false;    
    }
    private boolean isEmpty(String x) {
    	if (x==null || x.length()==0) return true;
    	return false;
    }
    
    /**
     * The following methods add a column to the listField vector
     * there is a method for each possible data type
     */
    private void addField ( int nID, String strName, String strTable,
    		String strField, String strGroupName, String strGroupDescription, 
    		String strFieldDescription, int nType)
    {
        FieldData theField = new FieldData ();
        theField.strName = strName;
        theField.strTable = strTable;
        theField.strField = strField;
        theField.nType = nType;
        theField.nID = nID;
        theField.strGroup = strGroupName; 					
        theField.strDescription = strFieldDescription; 		
        theField.strGroupDescription = strGroupDescription;	
        addFieldIfUnique ( theField );
    }
    private void addFieldIfUnique ( FieldData theField )
    {
    	String err=null;
        if ( isEmpty( theField.strName ) )
            err = "The column name can not be empty.";            
        else if ( getIndexForName ( theField.strName ) != -1 )
            err = "The column name " + theField.strName + " is already used.";
        else if ( getIndexForID ( theField.nID ) != -1 )
            err = "The column id " + theField.nID + " is already used. Column " + theField.strName ;        
        else if ( !isEmpty( theField.strTable ) && isEmpty( theField.strField ) )
            err = "The column name is not set.";
        
        if (err != null) {
    		RuntimeException e = new RuntimeException();
    		ErrorReport.reportError(e, "FieldMapper warning: " + err);
        }
        else {
    		listFields.add ( theField );
    		if (!listGroups.contains(theField.strGroup)) listGroups.add(theField.strGroup);
        }
    }
    /*******************************************************
     * FieldContigData and FieldPairsData to add columns
     */
    
    public void addBoolField ( int nID, String strName, String strTable, String strField, 
    		String strGroupName, String strGroupDescription, String strFieldDescription )
    {
        addField ( nID, strName, strTable, strField, strGroupName, 
        		strGroupDescription, strFieldDescription, FieldData.BOOLEAN_TYPE ); 
    }
    public void addStringField ( int nID, String strName, String strTable, String strField, 
    		String strGroupName, String strGroupDescription, String strFieldDescription ) 
    {
        addField ( nID, strName, strTable, strField, strGroupName, 
        		strGroupDescription, strFieldDescription, FieldData.STRING_TYPE );
    } 
    public void addIntField ( int nID, String strName, String strTable, String strField, 
    		String strGroupName, String strGroupDescription, String strFieldDescription ) 
    {
        addField ( nID, strName, strTable, strField, strGroupName, 
        		strGroupDescription, strFieldDescription, FieldData.INTEGER_TYPE );
    }
    public void addFloatField ( int nID, String strName, String strTable, String strField, 
    		String strGroupName, String strGroupDescription, String strFieldDescription )
    {
        addField ( nID, strName, strTable, strField, strGroupName, 
        		strGroupDescription, strFieldDescription, FieldData.FLOAT_TYPE );
    }
    public void addPercentField ( int nID, String strName, String strTable, String strField, 
    		String strGroupName, String strGroupDescription, String strFieldDescription ) 
    {
        addField ( nID, strName, strTable, strField, strGroupName, 
        		strGroupDescription, strFieldDescription, FieldData.PERCENT_TYPE );
    }
    public void setFieldSubQuery ( int nID, String strField, String strQuery ) {
    	FieldData field = getFieldByID ( nID );
    	field.strField = strField;
    	field.strSubQuery = "(" + strQuery + ") as " + strField;
    }
       
    /**
     * FieldData private class - contains information about a column in a table
     */        
    private class FieldData
    {
        static final public int UNKNOWN_TYPE = 0;
        static final public int INTEGER_TYPE = 1;
        static final public int BOOLEAN_TYPE = 2;
        static final public int FLOAT_TYPE   = 3;
        static final public int STRING_TYPE  = 4;
        static final public int PERCENT_TYPE = 5; // The value from the database is multiplied by 100
        static final public int RECORD_ID    = 6; // The record's position in the result set
      
        int nID = Integer.MIN_VALUE;// Unique ID for the field
        String strName = ""; 		// The name displayed to the end-user
        String strTable = null; 	// The database table (maybe null)
        String strField = null; 	// The database field (maybe null, e.g. if not from database)
        String strDescription = "";	// field description
        String strSubQuery = null; 	// The database subquery for the field
        int nType = UNKNOWN_TYPE; 	// The data type, used to create the display object
        boolean bRequired = false;
        String strGroup = null;		
        String strGroupDescription = ""; // group description (redundant, but it works)
    } 
    
    private class CmpFieldData implements Comparator<FieldData> 
    {
		public int compare(FieldData arg0, FieldData arg1) {
			if(arg0 == null && arg1 == null) return 0;
			else if(arg0 == null) return 1;
			else if(arg1 == null) return -1;
			
			if(arg0.nID < arg1.nID) return -1;
			else if(arg0.nID > arg1.nID) return 1;
			else return 0;
		}
    }
    public void prtFieldByID(int nField, String msg) {
    	FieldData f = getFieldByID(nField);
    	Out.tmp(msg + ": " + f.nID + " " + f.strName + "  " + f.strTable + "  " + f.strSubQuery);
    }
    public void prt() {
    	Out.tmp("From FieldMapper:");
    	FieldData [] fields = getVisibleFields ( );
        for ( int i = 0; i < fields.length; ++i )
        	Out.tmp(i+ " " + fields[i].nID + " " + fields[i].strName );
    }
    /* Instance variables */   
    private Vector<FieldData> listFields = new Vector<FieldData> ();
    private Vector <String> listGroups = new Vector<String>();
    
    private FieldData [] visibleFields = null;
    private int [] visibleFieldIDs = null;
    
    private int [] defaultFieldIDs = null;
    private Vector<String> allLibNames = null;
    
    private STCWFrame theParentFrame = null;
   
    private boolean isSeq=true;
}
