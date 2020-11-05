package cmp.database;

/****************************************************
 * CAS310 Added to be shared between Sequence Copy and Detail Copy
 */
import java.sql.ResultSet;

import cmp.viewer.MTCWFrame;
import util.database.DBConn;
import util.methods.ErrorReport;
import util.methods.Out;

public class Load {

	public Load(MTCWFrame theViewerFrame) {
		this.theViewerFrame = theViewerFrame;
	}
	public String  loadSeq(int seqIndex) {
		try {
			DBConn conn = theViewerFrame.getDBConnection();
			
			ResultSet rs = conn.executeQuery("Select ntSeq, orf_start, orf_end, aaSeq, UTstr  from unitrans where UTid=" + seqIndex);
			
			if (rs == null) {
				Out.PrtErr("Error loading sequence for SeqIndex " + seqIndex);
				return null;
			}
			rs.next();
			ntSeq = rs.getString(1);
			if (ntSeq!=null && ntSeq.length()>0) cdsSeq = ntSeq.substring(rs.getInt(2)-1, rs.getInt(3));
			else cdsSeq="";
	
			aaSeq = rs.getString(4);
			String utStr = rs.getString(5);			
			rs.close(); conn.close();
			return utStr; // CAS305 added the utStr to the clipboard for copy sequences
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading ntSeq"); return null;}
	}
	public String  loadSeq(String seqID) {
		try {
			DBConn conn = theViewerFrame.getDBConnection();
			
			ResultSet rs = conn.executeQuery("Select ntSeq, orf_start, orf_end, aaSeq, UTstr  from unitrans where UTstr='" + seqID + "'");
			
			if (rs == null) {
				Out.PrtErr("Error loading sequence for SeqID " + seqID);
				return null;
			}
			rs.next();
			ntSeq = rs.getString(1);
			if (ntSeq!=null && ntSeq.length()>0) cdsSeq = ntSeq.substring(rs.getInt(2)-1, rs.getInt(3));
			else cdsSeq="";
	
			aaSeq = rs.getString(4);
			String utStr = rs.getString(5);			
			rs.close(); conn.close();
			return utStr; // CAS305 added the utStr to the clipboard for copy sequences
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading ntSeq"); return null;}
	}
	public String loadHit(String hitID) {
		try {
			DBConn conn = theViewerFrame.getDBConnection();
			
			ResultSet rs = conn.executeQuery("Select sequence from unique_hits where HITstr='" + hitID + "'");
			if (rs == null) {
				Out.PrtErr("Error loading sequence for hitID " + hitID);
				return null;
			}
			rs.next();
			hitSeq = rs.getString(1);
				
			rs.close(); conn.close();
			return hitID; // just to show it worked
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading ntSeq"); return null;}
	}
	public String getNTseq() {return ntSeq;}
	public String getCDSseq() {return cdsSeq;}
	public String getAAseq() {return aaSeq;}
	public String getHitseq() {return hitSeq;}
	
	private MTCWFrame theViewerFrame = null;
	private String ntSeq=null, aaSeq=null, cdsSeq=null, hitSeq=null;
}
