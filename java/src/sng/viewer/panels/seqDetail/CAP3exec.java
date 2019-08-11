package sng.viewer.panels.seqDetail;

/****************************************
 * Runs CAP3 for the Contig display of Sequence Details
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.channels.ClosedByInterruptException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import sng.dataholders.MultiCtgData;
import sng.dataholders.SequenceData;
import util.database.HostsCfg;
import util.methods.ErrorReport;
import util.methods.TCWprops;

public class CAP3exec {
	final String CAP3_DEF = TCWprops.getExtDir() + "/CAP3/cap3";
	
	public CAP3exec(LoadFromDB dbo) {
		pathCAP3 = new HostsCfg().getCapPath(); 
		dbLoadObj = dbo;
	}
    public MultiCtgData executeCAP ( String strCommand,
    										TreeSet<String> estList,
    										String strOptions,
    										int recordNum) throws Exception
    {    
	    	String strContigName;
	    	String strSingletonName = "Singleton";
	    	strContigName = "CAP3";
	    	strContigName += " #" + (recordNum+1) + ".";
	    	
	    	BufferedReader aceOutFile = executeCAPLocal ( strCommand, estList, strOptions );
	    	if ( aceOutFile == null ) return null;
	    	 
	    	MultiCtgData theCluster = CAP3AceFileReader.readClusterFrom( this, dbLoadObj, aceOutFile, 
	    			estList, strContigName, strSingletonName );
	   	return theCluster;
    }
    
    private BufferedReader executeCAPLocal ( String strCommand,
					TreeSet<String> estList, String strOptions ) throws Exception
    {
	    	String strFASTAName = "/tmp/tempFASTA";
	    	File aceOutFile = new File ( strFASTAName + ".ace" );
	    	File singletsOutFile  = new File ( strFASTAName + ".singlets" );
	    	if ( strCommand.equals( "cap3" ) ) 
	    	{
	    		aceOutFile = new File ( strFASTAName + ".cap.ace" );
	            singletsOutFile = new File ( strFASTAName + ".cap.singlets" );	
	    	}
	    	File basesFASTA = new File ( strFASTAName );
	    	File qualFASTA  = new File ( strFASTAName + ".qual" );
	    	String str="<command>";
	    	
	    	try
	    	{	
	    		// Create the FASTA files with the selected sequences
	    		PrintStream seqStream = new PrintStream ( new FileOutputStream( basesFASTA ) );
	    		PrintStream qualStream = new PrintStream ( new FileOutputStream( qualFASTA ) );
	    		Vector<SequenceData> clones = dbLoadObj.loadClones( estList );
	    		Iterator<SequenceData> iterClone = clones.iterator();
	    		while ( iterClone.hasNext() )
	    		{
	    			SequenceData seq = (SequenceData)iterClone.next();
	    			seq.appendToFASTAFiles( seqStream, qualStream );
	    		}
	    		
	    		seqStream.close();
	    		qualStream.close();	
	    		
	    		// Run the alignment
	    		str = pathCAP3;
	    		str += " " + basesFASTA.getAbsolutePath();
	    		str += " " + strOptions;
	
	    		int nExitState = runCommand ( str );
	    		if ( nExitState == Integer.MIN_VALUE )
	    			return null;
	    		if ( nExitState > 0 )
	    			System.err.println ( "Application exited with " + nExitState + "." ); 
							
	    		return new BufferedReader ( new FileReader ( aceOutFile ) );    
	    	} catch (Throwable e) {
	    		String s = "Error: cannot execute " + strCommand + " command: " + str;
			ErrorReport.reportError(e, s);
	    	}
	    	finally
	    	{        
	    		// Delete all temporary files
	    		if ( strCommand.equals( "phrap" ) ) 
	    		{
		    		File contigsOutFile  = new File ( strFASTAName + ".contigs" );
		    		File qualOutFile  = new File ( strFASTAName + ".contigs.qual" );
		    		File problemsOutFile  = new File ( strFASTAName + ".problems" );
		    		File problemsQualOutFile  = new File ( strFASTAName + ".problems.qual" );
		    		File logOutFile  = new File ( strFASTAName + ".log" );
		    		
		    		contigsOutFile.delete();
		    		problemsOutFile.delete();
		    		problemsQualOutFile.delete();
		    		qualOutFile.delete();
		    		logOutFile.delete();	    		
	    		}
	    		else
	    		{
	        		File contigsOutFile  = new File ( strFASTAName + ".cap.contigs" );
	        		File linksOutFile  = new File ( strFASTAName + ".cap.contigs.links" );
	        		File qualOutFile  = new File ( strFASTAName + ".cap.contigs.qual" );
	        		File infoOutFile  = new File ( strFASTAName + ".cap.info" );
	        		
	        		contigsOutFile.delete();
	        		linksOutFile.delete();
	        		qualOutFile.delete();
	        		infoOutFile.delete();
	        	}
	    		
	    		singletsOutFile.delete();
	    		basesFASTA.delete();
	    		qualFASTA.delete();	
	    		aceOutFile.delete();
	    	}
	    	return null;
    }
    
	
    public static int runCommand ( String strCommand ) throws Exception
    {
    		return runCommand ( strCommand, null ); 
    }
    
    public static int runCommand ( String strCommand, Writer outWriter ) throws Exception
    {
        boolean bDone = false;
        Process cap3 = Runtime.getRuntime().exec( strCommand );
        InputStream stdout = cap3.getInputStream();       
        InputStreamReader osr = new InputStreamReader(stdout);
        BufferedReader brOut = new BufferedReader(osr);
        
        InputStream stderr = cap3.getErrorStream();       
        InputStreamReader esr = new InputStreamReader(stderr);
        BufferedReader brErr = new BufferedReader(esr);
        
        int nExitState = Integer.MIN_VALUE;
        
        try
        {
            synchronized ( stdout )
            {
                while ( !bDone )
                {
                    
                    // Send the sub-processes stderr to out stderr
                    while ( brErr.ready() )
                    {
                        System.err.print( (char)brErr.read() );
                    } 
                    
                    // Consume stdout so the process doesn't hang...
                    while ( brOut.ready() )
                    {
                    	int nData = brOut.read();
                    	if ( outWriter != null )
                    		outWriter.write( nData );
                    } 
                    
                    stdout.wait( 3000 /*milliseconds*/ );
                    
                    try
                    {
                        nExitState = cap3.exitValue();
                        bDone = true;
                    }
                    catch ( Exception err ){}
                }
            }
            // Send the sub-processes stderr to out stderr
            while ( brErr.ready() )
            {
                System.err.print( (char)brErr.read() );
            } 
        }
        catch ( ClosedByInterruptException ignore ){}
        catch ( InterruptedException ignore ){}
        
        // Kill the process if it's still running.  (Probably the thread was killed by user clicking cancel 
        // and threw one of the above exceptions.)
        if ( !bDone )
            cap3.destroy();          
        if ( outWriter != null )
        	outWriter.flush();
        
        return nExitState;  
    }
   
	private String pathCAP3 = null;
	private LoadFromDB dbLoadObj = null;
}
