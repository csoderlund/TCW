package sng.viewer.panels.seqDetail;
import sng.database.Globals;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.dataholders.SequenceData;
import util.methods.*;

import java.io.Reader;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.io.StreamTokenizer;
import java.lang.RuntimeException;


/**
 * CAP3AceFileReader -- a class for reading the ACE file output by CAP3.
 * The class doesn't read all of the data; only what we need.
 */

public class CAP3AceFileReader 
{
	static public MultiCtgData readClusterFrom ( CAP3exec execObj, LoadFromDB dbObj,
		Reader aceFile, TreeSet<String> estSet,
		String strContigName, String strSingletonName ) throws Exception
	{
		Vector<ContigData> contigList = CAP3AceFileReader.readContigsFrom( aceFile );
		
		// Rename the contigs
		int nCount = 1;
		Iterator<ContigData> iterContig = contigList.iterator();
		while ( iterContig.hasNext() )
		{
			ContigData curContig = iterContig.next();
			curContig.setContigID( strContigName + nCount );
			++nCount;
		}

		// Go through the lists of clones.  Add a singleton for
		// each clone that didn't make it into a contig
		TreeSet<String> singletonList = new TreeSet<String> ();
	    Iterator<String> iterClone = estSet.iterator();
		while ( iterClone.hasNext() )
		{
			String curSeq = iterClone.next ();
			boolean bFound = false;
			
			// Look for the current clone in each contig
			iterContig = contigList.iterator();
			while ( iterContig.hasNext() && !bFound )
			{
				ContigData curContig = iterContig.next();
				bFound = curContig.getSequenceByName( curSeq ) != null;
			}
			
			if ( !bFound )	singletonList.add( curSeq );
		}
		
		if ( !singletonList.isEmpty() )// Create contigs for all of the singletons
		{
			Vector<SequenceData> singletonClones = dbObj.loadClones( singletonList );
				
			Iterator<SequenceData>iterSeq = singletonClones.iterator();
			while ( iterClone.hasNext() )
			{
				SequenceData curSeq = iterSeq.next();
				ContigData curContig = new ContigData ();
				curContig.setContigID ( strSingletonName + " " + nCount );
				curContig.setSeqData( curSeq );
				curContig.addSequence( curSeq );	
				++nCount;	
				
				contigList.add( curContig );
			}
		}
			
		// Cross index the new contigs with the database to pickup data we don't get out of CAP
        dbObj.crossIndexCAP3Output( contigList );		
        
		// Make a psuedo cluster out of the results so we can display it
		MultiCtgData theCluster = new MultiCtgData ();
		for (int i = 0; i < contigList.size(); ++i)
			theCluster.addContig( contigList.get(i) );		
        return theCluster;
	}
	
	static public Vector<ContigData> readContigsFrom ( Reader aceFile ) throws Exception
	{
		// Open the StreamTokenizer...  For the record, I don't recommend using this class. 
		StreamTokenizer textFile = new StreamTokenizer ( aceFile );
		textFile.resetSyntax();
		textFile.eolIsSignificant( true );
		textFile.whitespaceChars ( '\0', ' ');
		textFile.wordChars( '!', '~' ); // This grabs most of the ASCII non-whitespace characters
				
		// Read the header "AS" line
		textFile.nextToken();
		verifyTag ( textFile, "AS" );
		int nNumContigs = getNextInt ( textFile );
		int nNumClones = getNextInt ( textFile );
		int nParsedClones = 0;
		Vector<ContigData> outContigs = new Vector<ContigData> ();
		
		ContigData curContig;
		do 
		{
			curContig = readNextContig ( textFile );
			
			if ( curContig != null )
			{
				outContigs.add ( curContig );
				nParsedClones += curContig.getNumSequences();
			}
		} while ( curContig != null );
		
		// Sanity check: parsed the expected number of contigs
		curContig = readNextContig ( textFile );		
		if ( nNumContigs != outContigs.size() )
			throw new RuntimeException ( "Failed sanity test for ACE file reader.  " +
				"Header specified " + nNumContigs + " contigs but parsed " + outContigs.size() + "." );
		
		// Sanity check: parsed the expected number of clones
		if ( nParsedClones != nNumClones )
			throw new RuntimeException ( "Failed sanity test for ACE file reader.  " +
				"Header specified " + nParsedClones + " clones but parsed " + nNumClones + "." );

		return outContigs;
	}
	
	static ContigData readNextContig ( StreamTokenizer tokens ) throws Exception
	{
		if ( readToTag ( tokens, "CO" ) == null )
			return null;
		
		String strContigName = getNextString ( tokens );
		int nNumBases = getNextInt ( tokens );
		int nNumClones = getNextInt ( tokens );
		
		//  <# of base segments in contig> 
		getNextInt ( tokens ); // Based on gaps?  Don't need...
		
		// Complement <U or C>
		boolean bComplement = getNextString ( tokens ).equals( "C" );
		
		// Sequence
		getNextAndCheck ( tokens, StreamTokenizer.TT_EOL );
		String strBases = getSequenceSection ( tokens, strContigName, nNumBases );
		strBases = SequenceData.normalizeBases ( strBases, '*', Globals.gapCh);
		
		// Qualities
		if ( readToTag ( tokens, "BQ" ) == null )
			throw new RuntimeException ( "Failed sanity test for ACE file " +
					"reader.  No BQ section for " + strContigName + "." );
		getNextAndCheck ( tokens, StreamTokenizer.TT_EOL );
		Vector<Integer> qualityVals = getQualitySection ( tokens );	
		
		// Setup object for reference sequence
		SequenceData refSequence = new SequenceData ("CAP");
		refSequence.setName ( strContigName );
		refSequence.setComplement ( bComplement );
		refSequence.setSequence ( strBases );		
		refSequence.padAndSetQualities ( qualityVals );
		
		// Setup the contig object
		ContigData thisContig = new ContigData ();
		thisContig.setContigID( strContigName );
		thisContig.setSeqData( refSequence );
		
		// Load AF data for the contig's sequences.  Stop when a "BS" tag is hit.
		while ( Converters.compareObjects( readToEitherTag ( tokens, "AF", "BS" ), "AF" )  )
		{
			String strQualifiedCloneName = getNextString ( tokens );
			boolean bCloneComplement =  getNextString ( tokens ).equals( "C" );
			int nStart = getNextInt ( tokens );// <padded start consensus position>
			
			// Make sure this is the first AF line for this EST.  I've seen situations
			// where cap3 lists the same EST twice.
			SequenceData cloneSeq = new SequenceData ("CAP");
			cloneSeq.setLeftPos( nStart );
			cloneSeq.setName( strQualifiedCloneName );
			cloneSeq.setComplement( bCloneComplement );
			
			// Add to the contig
			thisContig.addSequence ( cloneSeq );	
		}
		
		// Load the padded sequence (RD data).  Stop when a QA tag is hit.
		int nNumRDs = 0;
		while ( Converters.compareObjects( readToEitherTag ( tokens, "RD", "CO" ), "RD" ) )
		{
			++nNumRDs;
			String strCloneName = getNextString ( tokens );
			int nNumCloneBases = getNextInt ( tokens );
			getNextInt ( tokens );// <# of whole read info items> 
			getNextInt ( tokens );// <# of read tags>
			
			// Sequence
			getNextAndCheck ( tokens, StreamTokenizer.TT_EOL );
			String strCloneBases = getSequenceSection ( tokens, strContigName, nNumCloneBases );
			strCloneBases = SequenceData.normalizeBases ( strCloneBases, '*', Globals.gapCh );
			
			// Get the existing object (from the "BS" data) and append to it
			SequenceData cloneSeq = thisContig.getSequenceByName ( strCloneName );
			if ( cloneSeq == null )
				throw new RuntimeException ( "Failed sanity test for ACE file reader.  " +
						"Cannot cross-index clone " + strCloneName + 
						" in the RD section at line " + tokens.lineno() + "." );
			
			cloneSeq.setSequence ( strCloneBases );
		}

		// Sanity check: parsed the expected number of RD sections
		if ( nNumRDs != nNumClones )
			throw new RuntimeException ( "Failed sanity test for ACE file reader.  " +
					"Header specified " + nNumClones + " clones for " + strContigName + 
					" but only parsed " + nNumRDs + " RD sections." );
		
		return thisContig;
	}
	
	static private String readToTag ( StreamTokenizer tokens, String strTag1 ) throws Exception
	{
		String strRes = readToEitherTag ( tokens, strTag1, null );
		if ( strRes == null || !strRes.equals(strTag1) )
			return null;
		else 
			return strRes;
	}
	
	static private String readToEitherTag ( StreamTokenizer tokens, 
			String strTag1, String strTag2 ) throws Exception
	{
		boolean bLastWasEOL = true; // Assumes the input stream of tokens is always at the start of a line
		
		while ( tokens.ttype != StreamTokenizer.TT_EOF )
		{
			if ( bLastWasEOL && tokens.ttype == StreamTokenizer.TT_WORD )
			{
				if ( tokens.sval.equals( strTag1 ) )
					return strTag1; // Matched tag 1
				if ( strTag2 != null && tokens.sval.equals( strTag2 ) )
					return strTag2; // Matched tag 2
			}
			
			bLastWasEOL = tokens.ttype == StreamTokenizer.TT_EOL;
			
			tokens.nextToken();
		}
		
		return "";
	}
	
	static private void verifyTag ( StreamTokenizer tokens, String strTag ) throws Exception
	{
		if ( tokens.ttype != StreamTokenizer.TT_WORD || !tokens.sval.equals( strTag ) )		
			throw new RuntimeException ( "Failed sanity test for ACE file reader.  Expected " + strTag + 
											" section at line " + tokens.lineno() + "." );
	}
	static private int getCurInt ( StreamTokenizer tokens ) throws Exception
	{
		verifyType ( tokens, tokens.ttype, StreamTokenizer.TT_NUMBER, tokens.lineno() );
		if ( tokens.ttype == StreamTokenizer.TT_WORD )
			return Integer.parseInt( tokens.sval );
		else
			return (int)tokens.nval;
	}
	static private int getNextInt ( StreamTokenizer tokens ) throws Exception
	{
		// Note: I gave up on letting the class parse numbers since the parsing was very greedy.
		// E.g. 103-HGHGH would parse as the number 103 then the word -HGHGH.
		verifyType ( tokens, tokens.nextToken(), StreamTokenizer.TT_NUMBER, tokens.lineno() );
		if ( tokens.ttype == StreamTokenizer.TT_WORD )
			return Integer.parseInt( tokens.sval );
		else
			return (int)tokens.nval;
	}
	
	static private String getCurString ( StreamTokenizer tokens ) throws Exception
	{
		verifyType ( tokens, tokens.ttype, StreamTokenizer.TT_WORD, tokens.lineno() );
		return tokens.sval;
	}
	
	static private String getNextString ( StreamTokenizer tokens ) throws Exception
	{
		verifyType ( tokens, tokens.nextToken(), StreamTokenizer.TT_WORD, tokens.lineno() );
		return tokens.sval;
	}
	
	static private void getNextAndCheck ( StreamTokenizer tokens, int nDesType ) 
	throws Exception
	{
		verifyType ( tokens, tokens.nextToken(), nDesType, tokens.lineno() );
	}
	
	static private void verifyType ( StreamTokenizer tokens, int nActualType, 
			int nDesType, int nLine ) throws Exception
	{
		if ( nDesType == StreamTokenizer.TT_NUMBER && nActualType == StreamTokenizer.TT_WORD )
		{
			try
			{
				Integer.parseInt( tokens.sval );			
				nActualType = StreamTokenizer.TT_NUMBER;
			}
			catch ( Exception err )
			{
				// Below code will throw an exception with the line number
			}
		}
		
		if ( nActualType != nDesType )
			throw new RuntimeException ( "Failed sanity test for ACE file reader at line " 
				+ nLine  + "." +
				"Expected a token of type " + tokenTypeToString ( nDesType ) + 
				" but found " + tokenTypeToString ( nActualType )  + "." );		
	}
	
	static private String tokenTypeToString ( int nType )
	{
		switch ( nType )
		{
			case StreamTokenizer.TT_EOF: return "EOF";
			case StreamTokenizer.TT_EOL: return "EOL";
			case StreamTokenizer.TT_WORD: return "WORD";
			case StreamTokenizer.TT_NUMBER: return "NUMBER";
		}
		return "";
	}
	
	static private String getSequenceSection ( StreamTokenizer tokens, 
			String strName, int nNumBases ) throws Exception
	{
		// Assumes the current token is the EOL before the section
		String strBases = "";
		tokens.nextToken();
		
		while ( tokens.ttype == StreamTokenizer.TT_WORD )
		{
			strBases += getCurString ( tokens ).trim ();

			// Skip the EOL if we hit it.  (Note: double EOL is the end of the section.)
			tokens.nextToken();
			if ( tokens.ttype == StreamTokenizer.TT_EOL )
				tokens.nextToken();
		};
		
		// Sanity
		if (  nNumBases != strBases.length() )
			throw new RuntimeException ( "Failed sanity test for ACE file reader.  " +
											"For " + strName + " found " + strBases.length() + " bases, " + 
												" but header specified " + nNumBases + "." );
		
		return strBases;
	}
	
	static private Vector<Integer> getQualitySection ( StreamTokenizer tokens ) throws Exception
	{
		Vector<Integer> qualities = new Vector<Integer> ();
		
		// Keep reading integers until we find a double EOL
		// Assumes the current token is the EOL before the section
		while ( true )
		{
			tokens.nextToken();
			if ( tokens.ttype == StreamTokenizer.TT_EOL )
			{
				tokens.nextToken();
				if ( tokens.ttype == StreamTokenizer.TT_EOL )
					break;
			}
				
			qualities.add( getCurInt ( tokens ) );		
		}	
		return qualities;
	}
}
