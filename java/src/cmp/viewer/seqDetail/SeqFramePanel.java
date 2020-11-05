package cmp.viewer.seqDetail;

/****************************************************
 * From Sequence page, "Frame" button displays this
 */
import java.sql.ResultSet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cmp.database.Globals;
import cmp.viewer.MTCWFrame;

import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.UIHelpers;
import util.database.DBConn;

public class SeqFramePanel extends JPanel {
	private static final long serialVersionUID = 9005938171358335556L;
	private int yTop = 10;
	private int xLeft = 10;
	private int codonCol=20;
	private int rowHeight=15;
	private int seqNumLength=5;
	private int imageHeight=400;
	
	public SeqFramePanel (MTCWFrame frame, int index, String name)
	{
		theViewerFrame = frame;
		seqName = name;
		seqIndex = index;
		loadData();
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		JPanel page = Static.createPagePanel();
		createToolPanel();
		page.add(toolPanel);
		page.add(Box.createVerticalStrut(10));
		
		createStatsPanel();
		page.add(statsPanel);
		page.add(Box.createVerticalStrut(5));
		
		createDrawPanel();
		page.add(drawScrollPane);
		
		add(page);
	}
	private void createToolPanel() {
		toolPanel = Static.createRowPanel();
		
		JLabel name = new JLabel("  " + seqName);
		toolPanel.add(name);
		toolPanel.add( Box.createHorizontalStrut(40) );
		
		startCheckBox = Static.createCheckBox("Start", false);
		startCheckBox.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					showStart = startCheckBox.isSelected();
					refreshPanel();
				}
			}
		);	
		toolPanel.add(startCheckBox );
		toolPanel.add( Box.createHorizontalStrut(20) );
		
		hitCheckBox = Static.createCheckBox("Hit", false);
		hitCheckBox.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					showHit = hitCheckBox.isSelected();
					refreshPanel();
				}
			}
		);	
		if (bestHit!="") toolPanel.add( hitCheckBox );
		toolPanel.add( Box.createHorizontalStrut(20) );
		toolPanel.add(Box.createHorizontalGlue());
		
		toolPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)toolPanel.getPreferredSize ().getHeight() ) );	
	}
	/*************************************************/
	private void createStatsPanel() {
		statsPanel = Static.createRowPanel();
		
		if (header==null) {
			int len = oEnd-oStart+1;
			header = " Length: " + seqLen + "   ORF: Frame " + oFrame + " Len " + len + " (" + oStart + ".." + oEnd + ") "; 
			if (hitEnd!=0) {
				String e = String.format("%.1E", eval);
				header +=  "  Hit: (" + hitStart + ".." + hitEnd + ") " + e;
			}
		}
		String text = header + "\n";
		text = text.substring(0, text.length()-1);
		
		JTextArea textAreaTop = new JTextArea ( text );
		textAreaTop.setEditable( false );		
		textAreaTop.setFont(new Font("monospaced", Font.PLAIN, 12));
		int nTextWidth = Math.max((int)textAreaTop.getPreferredSize().getWidth() + 5, 600);		
		textAreaTop.setMaximumSize( new Dimension ( nTextWidth, 
				(int)textAreaTop.getPreferredSize().getHeight() ) );	
		textAreaTop.setAlignmentX(LEFT_ALIGNMENT);
		
		statsPanel.add(textAreaTop);
		
		statsPanel.add(Box.createHorizontalGlue());
		statsPanel.setMaximumSize(statsPanel.getPreferredSize());
	}
	/*************************************************/
	private void createDrawPanel() {
		imagePanel = new imageClassPanel(this);	
		drawScrollPane = new JScrollPane(imagePanel);
		UIHelpers.setScrollIncrements( drawScrollPane );
		drawScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
		drawScrollPane.setPreferredSize(new Dimension(getWidth(), getHeight()+imageHeight));
		refreshPanel();
	}
	private void refreshPanel() {
		int lineChar = (seqNumLength + ((sequence.length()/3)/codonCol));
		int height = lineChar*rowHeight;
		imagePanel.setPreferredSize(new Dimension(getWidth(), height));
		imagePanel.removeAll();
		imagePanel.revalidate();
		imagePanel.repaint();
	}
	/*************************************************/
	private void drawImage(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		super.paintComponent( g );
		setBackground(Color.WHITE);
		Font itFont = new Font("monospaced", Font.ITALIC, 12); // | Font.BOLD, 12);
		Font nrFont = new Font("monospaced", Font.BOLD, 12);
		setFont(nrFont);
		g2.setFont(nrFont);
		FontMetrics fm = g2.getFontMetrics();	
		
		boolean inHit=false, hitDone=false;
		String space=" ";
		
		int dX=xLeft;
		int dY=yTop;
		
		int inc =    3;
		int newRow = 20;
		int cntRowNT=newRow;
		
		// ruler at top
		String sNum = String.format("%5s", ""); // seqNumLength=5
		g2.setColor(Color.black);
		g2.drawString(sNum, xLeft, dY);
		dX = xLeft + fm.stringWidth(String.valueOf(sNum)) + seqNumLength;
		String ruler;
		for (int j=inc, i=0; i<cntRowNT; i++, j+=3) {
			if (inc==6) ruler = String.format(" %6d", j);
			else ruler = String.format(" %3d", j);
			g2.drawString(ruler, dX, dY);
			dX += fm.stringWidth(String.valueOf(ruler));
		}
		dX=xLeft;
		
		int coord=Math.abs(oFrame);
		
		for (int i=Math.abs(oFrame)-1; i<seqLen; i+=inc) {
			if (cntRowNT==newRow) {
				dY += rowHeight;
				sNum = String.format("%5d", coord); // seqNumLength=5
				g2.setColor(Color.black);
				g2.drawString(sNum, xLeft, dY);
				dX = xLeft + fm.stringWidth(String.valueOf(sNum)) + seqNumLength;
				cntRowNT=0;
			}
			cntRowNT++;
			String codon="";
			for (int j=inc; j>0; j--) {
				if (i+j<=seqLen) { 
					codon = sequence.substring(i, i+j);
					break;
				}
			}
			if (inc!=6 && codon.equals("tag") || codon.equals("tga") || codon.equals("taa")) {
				codon = codon.toUpperCase();
				g2.setColor(colorStop);
			}
			else {
				if (codon.equals("atg") || codon.equals("ctg") || codon.equals("ttg")) { 
					if (codon.equals("atg")) {
						codon = codon.toUpperCase();
						if (!showStart) g2.setColor(colorATG);
						else g2.setColor(colorATGx);
					}
					else {
						if (!showStart) g2.setColor(colorStart);
						else g2.setColor(colorStartx);
					}
				}
				else if (codon.contains("n")) g2.setColor(colorN);
				else g2.setColor(Color.BLACK);
			}
			
			space = " ";
			if (coord==oStart) space = "*";
			else if (coord==oEnd-2) space="*";
			if (space.equals("*")) g2.setColor(colorORF);
			
			if (showHit) {
				if (!inHit && !hitDone) {
					if (coord >= hitStart) {
						g2.setFont(itFont);
						inHit=true;
					}
				}
				else {
					if (coord>=hitEnd) {
						g2.setFont(nrFont);
						inHit=false;
						hitDone=true;
					}
				}
			}
			codon = space + codon;
			g2.drawString(codon, dX, dY);
		
			dX += fm.stringWidth(String.valueOf(codon));
			
			coord += inc;
		}
	}

	/****************************************************
	 * image class
	 */
	private class imageClassPanel extends JPanel
	{
		private static final long serialVersionUID = -5567198149411527973L;
		SeqFramePanel csp;
		public imageClassPanel(SeqFramePanel csp) 
		{
			super();
			setBackground(Color.white);
			setAlignmentX(Component.LEFT_ALIGNMENT);
			this.csp = csp; 
		}
	    public void paintComponent(Graphics g) 
	    {
	        super.paintComponent(g); 
	        csp.drawImage(g);
	    }  	    
	}
	
	private void loadData() {
		try {
			DBConn mdb = theViewerFrame.getDBConnection();
	
			String sql="Select ntSeq, orf_frame, orf_start, orf_end, HITid " +
					" from unitrans where UTid=" + seqIndex;
			
			ResultSet rs = mdb.executeQuery(sql);
			if (rs == null) ErrorReport.die("null result on database query in load Frame data");
			if (!rs.next()) ErrorReport.die("load Frame data has no sequence for seqID " + seqIndex);
			sequence = rs.getString(1);
			seqLen = sequence.length();
			
			oFrame = rs.getInt(2);
			oStart = rs.getInt(3);
			oEnd = rs.getInt(4);
			int hitID = rs.getInt(5);
			
			if (hitID==0) {
				rs.close(); mdb.close();
				return;
			}
			
			sql="Select HITstr, seq_start, seq_end, e_value from unitrans_hits " +
					" where UTid=" + seqIndex + " and bestEval=1";
			rs = mdb.executeQuery(sql);
			if (rs == null) ErrorReport.die("null result on database query in load Frame data");
			if (rs.next()) {
				bestHit = rs.getString(1);
				hitStart = rs.getInt(2);
				hitEnd = rs.getInt(3);
				if (hitStart>hitEnd) {
					hitStart = seqLen - hitStart + 1;
					hitEnd =   seqLen - hitEnd + 1;
				}
				eval = rs.getDouble(4);
			} else bestHit="";
			
			rs.close(); mdb.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Loading frame data");}
	}
	
	private JPanel toolPanel = null;
	private JCheckBox startCheckBox = null, hitCheckBox=null;
	private boolean showStart=false, showHit=false;
	
	private JScrollPane drawScrollPane = null;
	private imageClassPanel imagePanel = null;
	
	private JPanel statsPanel = null;

	private static final Color colorORF = Color.green; 
	private static final Color colorN = Color.PINK;
	private static final Color colorStop = Color.red;
	private static final Color colorATGx = Color.blue;
	private static final Color colorStartx = Color.cyan;
	private static final Color colorATG = Color.GRAY;
	private static final Color colorStart = Color.LIGHT_GRAY;
	
	private MTCWFrame theViewerFrame=null;
	
	private int oFrame=0, oStart=0, oEnd=0;
	private String header=null;
	
	private String bestHit="";
	private int hitStart=0, hitEnd=0;
	private double eval;
	
	private int seqIndex;
	private String seqName;
	private String sequence;
	
	private int seqLen;
}
