package sng.viewer.panels.seqDetail;

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
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import sng.database.Globals;
import sng.database.MetaData;
import sng.dataholders.ContigData;
import sng.dataholders.MultiCtgData;
import sng.viewer.STCWFrame;
import util.methods.ErrorReport;
import util.methods.Static;
import util.methods.Markov;
import util.ui.UIHelpers;
import util.ui.UserPrompt;
import util.align.AAStatistics;
import util.database.DBConn;

/********************************************************************
 * from ContigOverview (i.e. View Selected Sequence)
 * Show sequence by frame
 */
public class SeqFramePanel extends JPanel {
	private static final long serialVersionUID = 9005938171358335556L;
	
	private static final String helpFile = Globals.helpDir + "DetailFramePanel.html";
	private String HASMARK="'";
	private int yTop = 10, xLeft = 10, codonCol=20, rowHeight=15, seqNumLength=5, imageHeight=400;
	private boolean isProtein=false;
	
	private boolean hasHit=false;
	private String [] frameHitInfo;
	private int [] hitStart;
	private int [] hitEnd;
	
	public SeqFramePanel (STCWFrame frame, MultiCtgData theCluster, SeqDetailPanel seqDetail )
	{
		theMainFrame = frame;
		metaData = frame.getMetaData();
		isProtein = metaData.isAAsTCW();
		ctgData = theCluster.getContig();
		
		if (seqDetail.hasHit()) {
			frameHitInfo = 	seqDetail.getHitInfo(); 
			hitStart = 		seqDetail.getHitStart();
			hitEnd =   		seqDetail.getHitEnd();
			hasHit = true;
		}
		
		initTopPanel();
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		createToolPanel();
		add(Box.createVerticalStrut(10));
		add(toolPanel);
		add(Box.createVerticalStrut(10));
		
		createStatsPanel();
		mainScrollPane = new JScrollPane (statsPanel);
		add(mainScrollPane);
		add(Box.createVerticalStrut(5));
		
		createDrawPanel();
		JScrollPane drawScroll = new JScrollPane (drawScrollPane);
		add(drawScroll);
		
		add(Box.createVerticalGlue());
	}
	private void createToolPanel() {
		toolPanel = Static.createRowPanel();
		
		if (isProtein) {
			frameDropDown = new JComboBox <String> ();
			topDropDown = new JComboBox <String>();
			hitDropDown = new JComboBox <String> ();
			startCheckBox = Static.createCheckBox("dummy", false);
			cdsCheckBox = Static.createCheckBox("dummy", false);
			hitCheckBox = Static.createCheckBox("dummy", false); 
			return;
		}
		
		String ctgName = ctgData.getContigID();
		JLabel name = new JLabel("  " + ctgName);
		toolPanel.add(name);
		toolPanel.add( Box.createHorizontalStrut(5) );
		
		String [] labels = {"Frame 1", "Frame 2", "Frame 3", "Frame -1", "Frame -2", "Frame -3"};
		frameDropDown = Static.createCombo(labels);
		
		int def = orfFrame-1;
		if (orfFrame==0) def=2; 
		if (orfFrame==-1) def=3;
		else if (orfFrame==-2) def=4;
		else if (orfFrame==-3) def=5;
		frameDropDown.setSelectedIndex(def);

		frameDropDown.setBackground(Color.WHITE);
		frameDropDown.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					int type = frameDropDown.getSelectedIndex();
					if (type == 0) 		{dFrame=1;}
					else if (type == 1) {dFrame=2;}
					else if (type == 2) {dFrame=3;}
					else if	(type == 3)	{dFrame=-1;}
					else if	(type == 4)	{dFrame=-2;}
					else if	(type == 5)	{dFrame=-3;}
					refreshPanel();
				}
			}
		);	
		toolPanel.add( frameDropDown );
		toolPanel.add( Box.createHorizontalStrut(10) );
		
		String [] labels2 = {"ORFs/NT", "Scores/AA"};
		topDropDown = Static.createCombo(labels2);
		
		topDropDown.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					topAction=topDropDown.getSelectedIndex();
					refreshPanel();
				}
			}
		);	
		if (tupleMap!=null) {
			toolPanel.add(topDropDown );
			toolPanel.add( Box.createHorizontalStrut(10) );
		}
		else topAction=0;
	
		startCheckBox = Static.createCheckBox("Start", false); // gray CDS
		startCheckBox.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					if (startCheckBox.isSelected()) showStart=true;
					else showStart=false;

					refreshPanel();
				}
			}
		);	
		toolPanel.add( startCheckBox );
		toolPanel.add( Box.createHorizontalStrut(10) );
		
		cdsCheckBox = Static.createCheckBox("CDS", false); // colors Start, gray CDS
		cdsCheckBox.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					if (cdsCheckBox.isSelected()) showCDS=true;
					else showCDS=false;
					refreshPanel();
				}
			}
		);	
		toolPanel.add( cdsCheckBox );
		toolPanel.add( Box.createHorizontalStrut(10) );
		
		hitCheckBox = Static.createCheckBox("Hit", false);
		hitCheckBox.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					if (hitCheckBox.isSelected()) showHit=true;
					else showHit=false;
					refreshPanel();
				}
			}
		);	
		
		String [] labels3 = {"Italics", "Blue"};
		hitDropDown = Static.createCombo(labels3);
		hitDropDown.addActionListener
		( 	new ActionListener () 
			{
				public void	actionPerformed(ActionEvent e)
				{
					hitAction=hitDropDown.getSelectedIndex();
					refreshPanel();
				}
			}
		);	
		if (hasHit) {
			toolPanel.add(hitCheckBox );
			toolPanel.add(hitDropDown);
		}
		toolPanel.add( Box.createHorizontalStrut(20) );
		toolPanel.add(Box.createHorizontalGlue());
		
		JButton btnHelp = new JButton("Help2");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(),"Sequence Frame", helpFile);
			}
		});
		toolPanel.add(btnHelp);
		toolPanel.add( Box.createHorizontalStrut(5) );
		
		toolPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, 
				(int)toolPanel.getPreferredSize ().getHeight() ) );	
	}
	private SeqFramePanel getInstance() {return this;}
	/*************************************************
	 * The Coding Sequence (CDS) is the actual region of DNA that is translated to form proteins. 
	 * While the ORF may contain introns as well, the CDS refers to those nucleotides(concatenated exons) 
	 * that can be divided into codons which are actually translated into amino acids by the ribosomal 
	 * translation machinery.
	 *************************************************/
	private void createStatsPanel() {
		statsPanel = Static.createRowPanel();
		if (isProtein) {
			statsPanel.add(new JLabel("Protein length: " + sequence.length()));
			statsPanel.add(new JLabel("  Frame information not applicable"));
			return;
		}
		String text;
		if (topAction==0) { // CAS327 add orfFrame
			text="Assigned ORF: RF" + orfFrame + "  Length: " + sequence.length() + "   " + remark + "\n";
			for (OrfData o : ORFs) {
				String space = (o.frame==dFrame) ? "> " : "  ";
				text += space + o.line + "\n";
			}
		}
		else {
			text = createScoreText();
		}
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
		for (OrfData o : ORFs) 
			if (o.frame==dFrame) {
				drawOrf = o;
				break;
			}
	
		if (frameHitInfo!=null && frameHitInfo[drawOrf.frame+3]!=null) {
			hitCheckBox.setEnabled(true);
		}
		else {
			hitCheckBox.setEnabled(false);
		}
		
		mainScrollPane.remove(statsPanel);
		createStatsPanel();
		mainScrollPane.setViewportView( statsPanel );
		repaint();
		
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
		Font itFont = new Font("monospaced", Font.ITALIC, 12); // g2.setFont(itFont);
		Font nrFont = new Font("monospaced", Font.BOLD, 12);
		setFont(nrFont);
		g2.setFont(nrFont);
		FontMetrics fm = g2.getFontMetrics();	
		
		int oStart=drawOrf.rStart, oEnd=drawOrf.rEnd;
		
		boolean isNeg = (dFrame<0) ? true : false;
		String seq = (isNeg) ? seqRev : sequence;
		int seqLen = seq.length();
		String tuple;
		
		int coord = Math.abs(dFrame);
		if (isNeg) coord = seqLen-Math.abs(dFrame)+1; 
		
		int dX=xLeft;
		int dY=yTop;
		
		int inc =    3;
		int newRow = 20;
		int cnt=newRow;
		
		// ruler at top
		String sNum = String.format("%5s", ""); // seqNumLength=5
		g2.setColor(Color.black);
		g2.drawString(sNum, xLeft, dY);
		dX = xLeft + fm.stringWidth(String.valueOf(sNum)) + seqNumLength;
		String ruler;
		for (int j=inc, i=0; i<cnt; i++, j+=3) {
			int num = (topAction==0) ? j : (j/3);
			ruler = String.format(" %3d", (num-1)); // CAS333 Y-axis +/- X-axis to get last pos of codon see Help)
			g2.drawString(ruler, dX, dY);
			dX += fm.stringWidth(String.valueOf(ruler));
		}
		dX=xLeft;
		
		boolean inHit=false, inCDS=false, isStop=false, isEnd=false, isItalics=false;
		Color colorTxt = Color.black, colorCodon = Color.black;
		int frame = Math.abs(dFrame)-1, offFrame=dFrame+3;
		
		// sequence
		for (int i=frame; i<seqLen; i+=inc) {
			if (cnt==newRow) {
				dY += rowHeight;
				int num = (topAction==0) ? coord : ((coord-frame)/3)+1; 
				sNum = String.format("%5d", num); 
				g2.setColor(Color.black);
				g2.drawString(sNum, xLeft, dY);
				dX = xLeft + fm.stringWidth(String.valueOf(sNum)) + seqNumLength;
				cnt=0;
			}
			cnt++;
			tuple="";
			for (int j=inc; j>0; j--) {
				if (i+j<=seqLen) {
					tuple = seq.substring(i, i+j);
					break;
				}
			}
			if (topAction==1) { 
				if (tuple.length()==3) {
					char [] c = tuple.toCharArray();
					tuple = "  " + AAStatistics.getAminoAcidFor(c[0], c[1], c[2]);
				}
				else tuple = "  -";
			}
		// Start, Stop, N codons
			if (tuple.equals("tag") || tuple.equals("tga") || tuple.equals("taa")) {
				tuple = tuple.toUpperCase();
				colorCodon = colorStop;
				isStop=true;
			}
			else {
				isStop=false;
				if (tuple.equals("atg")) tuple = tuple.toUpperCase();
				
				if (tuple.equals("ATG") || tuple.equals("ctg") || tuple.equals("ttg")) { 
					if (showStart) {
						if (tuple.equals("ATG")) colorCodon = colorATGx;
						else colorCodon = colorStartx;
					}
					else {
						if (tuple.equals("ATG")) colorCodon = colorATG;
						else colorCodon = colorStart;
					}
				}
				else if (tuple.contains("n")) {
					colorCodon = colorN;
				}
				else colorCodon = Color.black;
			}
			
		// CDS Regio
			isItalics = false;
			if (coord==oStart) {
				inCDS=true;
				colorCodon  = colorTxt = colorCDS;
			}
			else if ((!isNeg && coord==oEnd-2) || (isNeg  && coord==oEnd+2)) {
				isEnd=true;
				inCDS=false;
				colorCodon = colorCDS; 
			}
			else if (isEnd) { // was adding "*" to delimit end of CDS, but didn't look right.
				colorTxt = Color.black; 
				isEnd=false;
			}
			tuple = " " + tuple;
				
		// Hit region - over-rides CDS color unless italics
			if (showHit) {
				if (!inHit) {
					if (coord==hitStart[offFrame]) {
						inHit=true;
						if (hitAction==0) isItalics=true;
						else colorTxt = colorHit;
					}
				}
				else { // inHit
					if ((dFrame>0 && coord>=hitEnd[offFrame]) || (dFrame<0 && coord<=hitEnd[offFrame])) {
						inHit=false;
						isItalics = false;
						if(inCDS && (showCDS || isEnd)) colorTxt = colorCDS;
						else colorTxt=Color.black;
					}
					else {
						if (hitAction==0) isItalics=true;
						else colorTxt = colorHit;
					}
				}
			}
			// Color
			if (!isStop) {
				if (showCDS && (inCDS || isEnd)) 
					colorCodon = colorTxt;
				else if (!isItalics && showHit && inHit)
					colorCodon = colorTxt;
			}
			g2.setColor(colorCodon);
			
			if (isItalics) g2.setFont(itFont);
			else g2.setFont(nrFont);
			
			g2.drawString(tuple, dX, dY);
		
			dX += fm.stringWidth(String.valueOf(tuple));
			
			if (isNeg) coord -= inc;
			else coord += inc;
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
	/****************************************************************
	 *  Usage routines 
	 */
	private void initTopPanel() {
		if (isProtein) {
			sequence = ctgData.getSeqData().getSequence();
			dFrame=orfFrame=1;
			drawOrf = new OrfData();
			drawOrf.rStart = 1;
			drawOrf.rEnd = sequence.length();
			return;
		}
		sequence = 	ctgData.getSeqData().getSequence().toLowerCase();
		seqRev = 	ctgData.getSeqData().getSeqRevComp().toLowerCase();
		seqLen = sequence.length();
		
		initTupleMap();
		
		String add="", add2=""; 
		String notes = ctgData.getTCWNotes();
		if (notes!=null && !notes.equals("")) { 
			String [] remarks = notes.split(Globals.tcwDelim);
			for (int i=0; i<remarks.length; i++) {
				String r = remarks[i].trim();
				if (r.startsWith(Globals.RMK_MultiFrame) || r.startsWith(Globals.RMK_HIT_hitSTOP))
					add2 += r + " ";
				else
					add += r + " ";
			}
		}
		remark = add2 + add;
		
		OrfData best = initReadDB();
		if (best==null) drawOrf = ORFs.get(0);	
		else drawOrf = best;
		
		for (OrfData o : ORFs) {
			initFrameLines(o);
			
			if (frameHitInfo!=null && frameHitInfo[o.frame+3]!=null) 		
				o.line = o.line + frameHitInfo[o.frame+3];
		}	
		
		dFrame = drawOrf.frame;
	}
	/***************************************************************
	 * Create 3 lines of information corrsponding to the 3 draw modes
	 */
	private void initFrameLines(OrfData o) {
		String seq = (o.frame>0) ? sequence : seqRev;
		int arf = Math.abs(o.frame);
		int countStop=0;
	
	// orf
		for (int i=arf-1; i<seqLen-3; i+=3) {
			String codon=seq.substring(i,i+3);
			if (codonStop(codon)) countStop++;
		}
		if (tupleMap==null) {
			o.line = String.format("%2d ORF %4d  (%5d%s..%5d%s)  #Stop %2d ", 
					o.frame, o.len, o.rStart, o.hasStart, o.rEnd, o.hasEnd, countStop);
			return;
		}
		String c = (o.cScore==-100.0) ? "     -" : String.format("%6.2f", o.cScore);
		String h = (o.hScore==-100.0) ? "     -" : String.format("%6.2f", o.hScore);
		o.line = String.format("%2d ORF: %4d (%5d%s..%5d%s)  Markov:%s%s Codon:%s   #Stops %2d  ", 
				o.frame, o.len, o.rStart, o.hasStart, o.rEnd, o.hasEnd, h, o.isBad, c, countStop);
	}
	// drawOrf is the OrfData and dFrame is the frame
	private String createScoreText() {
		if (drawOrf.scoreText!=null) return drawOrf.scoreText;
		return scoreObj.makeTextForSeqFrame(drawOrf.cScore, drawOrf.hScore, drawOrf.frame, drawOrf.nStart, drawOrf.nEnd, sequence);
	}
	
	private OrfData initReadDB() {
		try {
			OrfData best=null;
			orfString = ctgData.getLongestORFCoords();
			if (!orfString.equals("-")) orfFrame =    ctgData.getORFCoding().getFrame();
			else orfFrame=0;
			DBConn dbc = theMainFrame.getNewDBC();
			
			Vector <OrfData> oList = new Vector <OrfData> ();
			
			if (dbc.tableExist("tuple_orfs")) {
				int CTGid = ctgData.getCTGID();
				
				ResultSet rs = dbc.executeQuery("SELECT value FROM tuple_orfs where CTGid=" + CTGid);
				while(rs.next ())
				{
					String value = rs.getString(1);
					String [] tok = value.split(":");
					OrfData o = new OrfData();
					oList.add(o);
					
					o.frame = Integer.parseInt(tok[0]);
					o.nStart = Integer.parseInt(tok[1]);
					o.nEnd = Integer.parseInt(tok[2]);
					o.hScore = (tok[3].equals("NaN")) ? -100.0 : Double.parseDouble(tok[3]) ;
					o.cScore = (tok[4].equals("NaN")) ? -100.0 : Double.parseDouble(tok[4]);
					if (tok.length>=7) {
						o.hasStart= (tok[5].equals(Globals.ORF_NoMARK)) ? HASMARK : " ";
						o.hasEnd=   (tok[6].equals(Globals.ORF_NoMARK)) ? HASMARK : " ";
					}
					if (tok.length==8) {
						o.isBad = (tok[7].equals(Globals.ORF_NoMARK)) ? HASMARK : " ";
					}
					if (orfFrame==o.frame) best=o;
				}
				rs.close(); dbc.close();
				
				if (oList.size()!=6) {
					for (int i=3; i>=-3; i--) {
						if (i==0) continue;
						boolean found=false;
						for (OrfData o : oList) {
							if (o.frame==i) {
								found=true;
								break;
							}
						}
						if (!found) {
							OrfData o = new OrfData();
							oList.add(o);
							o.frame = i;
						}
					}
				}
			}
			else { // should always be in database now
				dbc.close();
				
				for (int i=3; i>=-3; i--) {
					if (i==0) continue;
					OrfData o = new OrfData();
					oList.add(o);
					o.frame = i;
					if (orfFrame==i) {
						best = o;
						o.nStart = ctgData.getORFCoding().getBegin();
						o.nEnd = ctgData.getORFCoding().getEnd();
					}
					else {
						o.nStart = Math.abs(i);
						o.nEnd = seqLen;
					}
				}
			}
			// CAS314 order correctly
			for (int i=3; i>=-3; i--) {
				if (i==0) continue;
				for (OrfData o : oList) {
					if (o.frame==i) {
						ORFs.add(o);
						break;
					}
				}
			}
			// RCOORDS: create reverse coordinates 
			for (OrfData o : ORFs) {
				if (o.frame<0) {
					o.rStart = seqLen - o.nStart + 1;
					o.rEnd = seqLen - o.nEnd + 1;
				}
				else {
					o.rStart = o.nStart;
					o.rEnd = o.nEnd;
				}
				if (o.nStart!=o.nEnd) o.len = Math.abs(o.nStart-o.nEnd) + 1;
			}
			return best;
		}
		catch (Exception e) {ErrorReport.reportError(e, "Cannot read database"); return null;}
	}
	private void initTupleMap() { 
		tupleMap = metaData.getTupleMap(); // tuples just read once at beginning

		if (tupleMap==null) return;
		
		String codonQ1 = "codonQ1"; // from pre-v2.5
		if (tupleMap==null || tupleMap.size()==0 || tupleMap.containsKey(codonQ1)) {;
			tupleMap=null;
			return;
		}
		
		scoreObj = new Markov(tupleMap);
	}
	
	private boolean codonStop(String codon) {
		if (codon.equals("taa")) return true;
		if (codon.equals("tag")) return true;
		if (codon.equals("tga")) return true;
		return false;
	}
	class OrfData {
		double cScore=-100.0, hScore=-100.0;
		int frame=0, len=0;
		int nStart=0, nEnd=0; // relative to sequence
		int rStart=0, rEnd=0; // relative to reverse sequence
		String hasStart=" ", hasEnd=" ", isBad=" ";
		String line = null;
		String scoreText = null;
		String header="";
	}
	private Vector <OrfData> ORFs = new Vector <OrfData> ();
	private OrfData drawOrf=null;
	private STCWFrame theMainFrame=null;
	private MetaData metaData=null;
	private ContigData ctgData;
	private int orfFrame=1;
	private String sequence, seqRev, orfString, remark;
	private int seqLen;
	
	private Markov scoreObj=null;
	private HashMap <String, Double> tupleMap = null;
	
	private JPanel toolPanel = null;
	private JComboBox <String> frameDropDown = null, topDropDown = null, hitDropDown = null;
	private JCheckBox startCheckBox = null, cdsCheckBox = null, hitCheckBox=null;
	private int topAction=0, hitAction=0; 
	private boolean showCDS=false, showStart=false, showHit=false; 
	
	private int dFrame=1;
	private JScrollPane drawScrollPane = null;
	private imageClassPanel imagePanel = null;
	
	private JScrollPane mainScrollPane = null;
	private JPanel statsPanel = null;

	private static final Color colorStop = Color.red;
	private static final Color colorStartx = Color.cyan;
	private static final Color colorATGx = Color.cyan;
	private static final Color colorATG = Color.GRAY;
	private static final Color colorStart = Color.LIGHT_GRAY;
	private static final Color colorCDS = Color.green; 
	private static final Color colorHit = Color.blue; 
	private static final Color colorN = Color.PINK;
}
