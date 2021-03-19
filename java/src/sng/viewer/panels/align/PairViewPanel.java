package sng.viewer.panels.align;
/**
 * Display one or more aligned pairs 
 */
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import sng.database.Globals;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.Static;
import util.ui.MenuMapper;
import util.ui.UIHelpers;
import util.ui.UserPrompt;

public class PairViewPanel extends JPanel implements ClipboardOwner 
{		
	public static int GAP = 12;
	public static int EXTEND = 2;
	private final String helpHTML = Globals.helpDir + "Align.html";
	
	public void lostOwnership(Clipboard clipboard, Transferable contents) { }
	
	// this can be two sequences or sequence and hit. AlignData contains the alignments.
	static public PairViewPanel createPairAlignPanel (
			boolean isHit, 		// alignment against Hit
			boolean isAllFrame, // seqDetailAlign option to show all frames
			Vector<AlignData> inAlignmentLists) 
	{
		boolean alt = true, isAAsTCW=false;
		int cnt=1;
		String lastHit="";
		Vector<PairAlignPanel> subPanels = new Vector<PairAlignPanel> ();
		
		Iterator<AlignData> i = inAlignmentLists.iterator();
		while ( i.hasNext() )
		{
			AlignData alignData = i.next ();
			isAAsTCW = !alignData.isNTsTCW();
			String hitID = alignData.getSeqData2().getName(); // Only change if hitID is different
			if (!hitID.equals(lastHit)) {
				alt = !alt;
				lastHit=hitID;
			}
			PairAlignPanel curPanel = new PairAlignPanel ( 
					cnt, alignData, alt, isHit, nTopGap, nBottomGap, nSideGaps, nSideGaps );
			subPanels.add( curPanel );	
			cnt++;
		}
		
		PairViewPanel panel = new PairViewPanel (isHit, isAAsTCW);
		panel.createMainPanelFromSubPanels(subPanels);
		
		panel.add ( panel.createButtons (isAllFrame ) );
		panel.add ( Box.createVerticalStrut(5) );
		panel.add ( panel.scroller );	
		return panel;
	}	
	
	/*******************************************
	 * XXX Class starts here
	 */
	private PairViewPanel (boolean b,  boolean a) {
		isHit = b; // versus pair align
		isAAsTCW = a;
				
		scroller = new JScrollPane ( );
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				handleClick(e);
			}
		});	
        UIHelpers.setScrollIncrements( scroller );
        
		scroller.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				menuZoom.setSelectedIndex ( 4 ); // initial draw
				scroller.removeComponentListener(this);
			}
		});
        
		setLayout( new BoxLayout ( this, BoxLayout.Y_AXIS ) );
		super.setBackground(Color.WHITE);
	}
	
	private void createMainPanelFromSubPanels ( Vector <PairAlignPanel>  pairPanels ){
		JPanel mainPanel = Static.createPagePanel();
		mainPanel.setVisible( false );
		
		baseAlignPanelsVec = new Vector <PairBasePanel> ();
		
		Iterator <PairAlignPanel>iter = pairPanels.iterator();
		while ( iter.hasNext() )
		{
			PairBasePanel curPanel = (PairBasePanel) iter.next ();
			mainPanel.add( curPanel );
			baseAlignPanelsVec.add( curPanel );
		}

		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( new LegendPanel(true /*isPair*/));
		mainPanel.add( Box.createVerticalStrut(30) );
		mainPanel.add( Box.createVerticalGlue() ); 	
		
		scroller.setViewportView( mainPanel );
	}
		
	/*******************************************************
	 * Pairwise - either NTAA, NTNT or AAAA
	 */
	private JPanel createButtons (boolean isAllFrames)
	{
		JPanel topPanel = Static.createRowPanel();
		topPanel.add( Box.createHorizontalStrut(5) );
		topPanel.add(new JLabel("View: ")); topPanel.add( Box.createHorizontalStrut(1) );
		
		btnViewType = new JButton ("Line"); // CAS310 changed from View Bases. CAS313 further changed...
		btnViewType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionChangeView();
			}
		});
		topPanel.add( btnViewType );		topPanel.add( Box.createHorizontalStrut(3) );
		
		menuZoom = Static.createZoom2();
		menuZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					MenuMapper selection = (MenuMapper) menuZoom.getSelectedItem();
					actionChangeZoom(selection.asInt());
				} catch (Exception err) { ErrorReport.reportError(err);}
				
				revalidate(); 
				repaint();
			}
		});		
		topPanel.add( menuZoom );			topPanel.add( Box.createHorizontalStrut(3) );
		
		chkTrim = Static.createCheckBox("Trim", false);
		chkTrim.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionChangeTrim(chkTrim.isSelected());
				revalidate(); 
				repaint();
			}
		});
		topPanel.add( chkTrim );			topPanel.add( Box.createHorizontalStrut(5) );
		
	// Hit Only 
		chkUTR = Static.createCheckBox("UTRs", false);
		chkUTR.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionHighlight(0, chkUTR.isSelected());
				revalidate(); 
			}
		});
		
		chkHit = Static.createCheckBox("Hit", false);
		chkHit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionHighlight(1, chkHit.isSelected());
				revalidate(); 
			}
		});
		
		JButton btnHelp = new JButton("Help2");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				UserPrompt.displayHTMLResourceHelp(getInstance(),"Seq-Hit Alignment", helpHTML);
			}
		});
			
		btnAlign = new JButton("Align..."); 
		btnAlign.setBackground(Globals.PROMPTCOLOR);
		btnAlign.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				actionAlignPopup();
			}
		});
		if (isHit && !isAllFrames) {
			if (!(isAAsTCW)) {
				topPanel.add( chkUTR ); 
				topPanel.add( Box.createHorizontalStrut(5) );
			}
			topPanel.add( chkHit ); 
    		topPanel.add( Box.createHorizontalStrut(10) ); 
    		
    		// Align doesn't work for Pairs and AllFrames because only has name, which is not unique (could use #panel)
            topPanel.add(new JLabel("Selected:"));	topPanel.add( Box.createHorizontalStrut(3) ); 
            topPanel.add(btnAlign);					topPanel.add( Box.createHorizontalStrut(15) );
            topPanel.add( Box.createHorizontalGlue() );
            topPanel.add(btnHelp);						topPanel.add( Box.createHorizontalStrut(5) );
        }
	    topPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)topPanel.getPreferredSize().getHeight() ) );
	    topPanel.setBackground(Color.WHITE);
	    
	    return topPanel;
	}
	
	private PairViewPanel getInstance() {return this;}
	
	private TreeSet<String> getSelectedSeqIDs ( ) {
		TreeSet<String> selection = new TreeSet<String> ();
		
		for (PairBasePanel curPanel : baseAlignPanelsVec) {	
			curPanel.getSelectedSeqIDs( selection );
		}		
		if (selection.size()>0) return selection;
		else return getFirstSeqID(); // CAS304
	}
	private TreeSet<String> getFirstSeqID() { // CAS304 do not have to select an alignment
		TreeSet<String> selection = new TreeSet<String> ();
		
		for (PairBasePanel curPanel : baseAlignPanelsVec) {	
			Vector <String> ids = curPanel.getSeqIDs( );
			selection.add(ids.get(0));
			return selection;
		}			
		return selection;
	}
	
	/*********************************************************/
	private void handleClick (MouseEvent e)
	{
		// Convert to view relative coordinates
		int viewX = (int) (e.getX() + scroller.getViewport().getViewPosition().getX());
		int viewY = (int) (e.getY() + scroller.getViewport().getViewPosition().getY());
		
		// Go through  all the panels and see which one was clicked on:
		Iterator<PairBasePanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() )
		{
			// Get the panel and convert to panel relative coordinates
			PairBasePanel curPanel = iter.next();
			int nPanelX = viewX - curPanel.getX();
			int nPanelY = viewY - curPanel.getY();
			
			if ( curPanel.contains( nPanelX, nPanelY ) ) {
				// Click is in current panel, let the object handle it
				curPanel.handleClick( e, new Point( nPanelX, nPanelY ) );
			}
			else
				// Clear all selections in the panel unless shift or control are down
				if ( !e.isShiftDown() && !e.isControlDown() )
					curPanel.selectNone();
		}
	}
	
	private void actionChangeZoom ( int n ) throws Exception // Notify all sub-panels
	{
		Iterator<PairBasePanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ){
			PairBasePanel curPanel = iter.next ();
			curPanel.setZoom ( n );
		}		
		scroller.getViewport().getView().setVisible( true );
	}
	private void actionChangeTrim ( boolean b ) // Notify all sub-panels
	{
		boolean setUTR = chkUTR.isSelected();// CAS314 doesn't work with trim
		if (b) {
			chkUTR.setSelected(false);
			chkUTR.setEnabled(false);
			setUTR=false;
		}
		else chkUTR.setEnabled(true);
		
		Iterator<PairBasePanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() ) {
			PairBasePanel curPanel = iter.next ();
			curPanel.setTrim ( b );
			curPanel.setShowORF(setUTR); 
		}		
		scroller.getViewport().getView().setVisible( true );
	}
	
	private void actionChangeView ( )
	{
		if ( nViewType == PairBasePanel.GRAPHICMODE )
			nViewType = PairBasePanel.TEXTMODE;
		else
			nViewType = PairBasePanel.GRAPHICMODE;
		
		setViewType();
	}
	
	private void setViewType ()
	{
		// Notify all sub-panels
		Iterator<PairBasePanel> iter = baseAlignPanelsVec.iterator();

		if ( nViewType == PairBasePanel.GRAPHICMODE )
		{
			btnViewType.setText( "Line" );
			menuZoom.setEnabled ( true );
			while ( iter.hasNext() )
			{
				PairBasePanel curPanel = iter.next ();
				curPanel.setDrawMode (PairBasePanel.GRAPHICMODE);	
			}
		}
		else
		{
			btnViewType.setText( "Seq" );		
			menuZoom.setEnabled ( false );
			while ( iter.hasNext() )
			{
				PairBasePanel curPanel = iter.next ();
				curPanel.setDrawMode (PairBasePanel.TEXTMODE);	
			}
		}		
		scroller.setViewportView( scroller.getViewport().getView() );		
	}
	
	
	
	/****************************************************************
	 * XXX Align Pair for Pairwise
	 */
	private void actionHighlight(int type, boolean set) {
		Iterator<PairBasePanel> iter = baseAlignPanelsVec.iterator();
		while ( iter.hasNext() )
		{
			PairBasePanel curPanel = iter.next ();
			if (type==0) curPanel.setShowORF(set);
			else if (type==1) curPanel.setShowHit(set);
		}
	}
	/*********************************************************
	 * Align Popup
	 */
	private void actionAlignPopup() {
		TreeSet <String> selectedCtg = getSelectedSeqIDs();
		
		if (selectedCtg==null || selectedCtg.size()==0) {
			JOptionPane.showMessageDialog( 	null, 
					"Select a hit in one of the alignments.","No hit selected", JOptionPane.PLAIN_MESSAGE );
		}
		else {
			final String name = selectedCtg.first();
			final AlignType at = new AlignType(name);
			at.setVisible(true);
			final int mode = at.getSelection();
			
			if(mode != AlignType.Align_cancel) {
				Vector <String> lines = alignPopup(at.getGap(), at.getExtend(), mode);
				if (lines!=null) 
				{
					String [] alines = new String [lines.size()];
					lines.toArray(alines);
					UserPrompt.displayInfoMonoSpace(null, "Hit Alignment", alines);
				}
			}
		}
	}
	private Vector <String> alignPopup(int gap, int extend, int type) {
		Vector <String> lines = new Vector <String> ();
	
		int inc = 60;
		TreeSet <String> selectedCtg = getSelectedSeqIDs();
		String name = selectedCtg.first();
		
		AlignData aDataObj=null;
		for (PairBasePanel ap : baseAlignPanelsVec) {
			AlignData ad = ap.getAlignData();
			if (name.equals(ad.getName1()) || name.equals(ad.getName2())) {
				aDataObj=ad;
				break;
			}
		}
		if (aDataObj.isNTalign() && type!=0) { // CAS313
			lines.add("Only the Original works for NT hits");
			return lines;
		}
		String aSeq1, aSeq2, aSeqM, method;
		int nStart1, nEnd1, nStart2, nEnd2, nStart,  nEnd, score=-1;
		if (type!=0) { // new alignment with PairAlign. Local. 
			AlignPairAA aExecObj = new AlignPairAA(gap, extend, 
					aDataObj.getOrigSeq1(), aDataObj.getOrigSeq2(), type);
			aSeq1 = aExecObj.getAlignOne();
			aSeq2 = aExecObj.getAlignTwo();
			aSeqM = aExecObj.getAlignMatch();
			
			nStart=0; 
			nEnd=aSeqM.length();
			int [] ends = aExecObj.getEnds();
			nStart1=ends[0]+1; 
			nEnd1=  ends[1];
			nStart2=ends[2]+1; 
			nEnd2=  ends[3];
			score = aExecObj.getScore();
			method = aExecObj.getMethod();
		}
		else { // current alignment, already done. Global
			aDataObj.computeMatch();
			aSeq1 = aDataObj.getAlignSeq1();
			aSeq2 = aDataObj.getAlignSeq2();
			aSeqM = aDataObj.getMatcheSeq();
			nStart =  aDataObj.getStartAlign();
			nEnd = aDataObj.getEndAlign();
			int [] ends = aDataObj.getEnds();
			nStart1=ends[0]; 
			nEnd1=  ends[1];
			nStart2=ends[2]; 
			nEnd2=  ends[3];
			score = aDataObj.getScore();
			method = "Original semi-global with affine gap";
		}
		// create info for left label
		String name1 = aDataObj.getName1();
		String name2 = aDataObj.getName2();
		int max = Math.max(name1.length(), name2.length());
		int max2 = Math.max(nEnd1, nEnd2);
		int y=5;
		if (max2<999) y=3;
		else if (max2<9999) y=4;
		String format = "%" + max + "s " + "%" + y + "d  ";
		String format1 = "%" + max + "s " + "%" + y + "s  ";
		
		// header score
		boolean inGap=false;
		int cntPos=0, cntNeg=0, cntGap=0, cntOpen=0, cntMat=0;
		for (int i=nStart;i<nEnd && i<aSeqM.length(); i++) {
			char c1 = aSeq1.charAt(i);
			char c2 = aSeq2.charAt(i);
			char cM = aSeqM.charAt(i);
			if (c1=='-' || c2=='-') {
				if (!inGap) {
					cntOpen++; 
					inGap=true;
				} 
				else cntGap++;
			}
			else {
				if (cM==Globalx.cAA_NEG) cntNeg++;
				else if (cM==Globalx.cAA_POS) cntPos++;
				else cntMat++;
				inGap=false;
			}
		}
		// header 
		String msg = "Method: " + method;
		if (type>0) msg += "    Penalties: Gap " + gap;
		if (type>1) msg += " Extend " + extend;
		lines.add(msg);
		
		if (aDataObj.isNTalign()) msg = String.format("Score: %4d     Gap open:   %3d", score,  cntOpen);
		else msg = String.format("Score: %4d   %s: %3d  Gap open:   %3d", score, Globalx.blosumGt, cntPos, cntOpen);
		lines.add(msg);
		
		if (aDataObj.isNTalign()) msg = String.format("Match: %4d     Gap extend: %3d",cntMat,  cntGap);
		else msg = String.format("Match: %4d   %s: %3d  Gap extend: %3d",cntMat, Globalx.blosumLtEq, cntNeg, cntGap);
		lines.add(msg); 
		lines.add("");
		
		// alignment
		int x;
		StringBuffer sb = new StringBuffer (inc);
		
		for (int offset=nStart; offset<nEnd; offset+=inc) {
			sb.append(String.format(format, name1, nStart1)); 
			for (x=0; x<inc && (x+offset)<nEnd; x++) sb.append(aSeq1.charAt(x+offset));
			sb.append("  " + (nStart1+x));
			lines.add(sb.toString());
			sb.delete(0, sb.length());
			
			sb.append(String.format(format1, "",""));
			for (int i=0; i<inc && (i+offset)<nEnd; i++) sb.append(aSeqM.charAt(i+offset));
			lines.add(sb.toString());
			sb.delete(0, sb.length());
			
			sb.append(String.format(format, name2, nStart2));
			for (x=0; x<inc && (x+offset)<nEnd; x++) sb.append(aSeq2.charAt(x+offset));
			sb.append("  " + (nStart2+x));
			lines.add(sb.toString());
			sb.delete(0, sb.length());
			
			lines.add("");
			nStart1+=inc;
			nStart2+=inc;
		}
		return lines;
	}
	private class AlignType extends JDialog {
		private static final long serialVersionUID = 6152973237315914324L;

		public static final int Align_orig= 0;
		public static final int Align_local = 1;
	    public static final int Align_local_affine= 2;
	    public static final int Align_cancel= 3;
	   
    	public AlignType(String name) {
    		setModal(true);
    		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    		setTitle("Align " + name);
    		
    		JRadioButton btnOrig = new JRadioButton("Original semi-global with affine gaps");
    		btnOrig.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nMode = Align_orig;
				}
			});
    		JRadioButton btnLocal =  new JRadioButton("Local");
    		btnLocal.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					nMode = Align_local;
				}
			});
    		JRadioButton btnLocalAffine = new JRadioButton("Local with affine gaps");
    		btnLocalAffine.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent arg0) {
    				nMode = Align_local_affine;
    			}
    		});
	   	  
    		ButtonGroup grp = new ButtonGroup();
    		grp.add(btnOrig);
    		grp.add(btnLocal);
	        grp.add(btnLocalAffine); 
	          
	    	btnOrig.setSelected(true);
	    	nMode = Align_orig;
	    	
	    	// Parameter for local
    		JLabel gapLabel = new JLabel("Gap ");
			txtGap  = new JTextField(3);
			txtGap.setMaximumSize(txtGap.getPreferredSize());
			txtGap.setText("12");
			
			JLabel extendLabel = new JLabel("Extend ");
            txtExtend  = new JTextField(3);
            txtExtend.setMaximumSize(txtExtend.getPreferredSize());
            txtExtend.setText("2");
    			       		
        	JButton btnOK = new JButton("OK");
        	btnOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}
			});
    		JButton btnCancel = new JButton("Cancel");
    		btnCancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nMode = Align_cancel;
					setVisible(false);
				}
			});
    		
    		btnOK.setPreferredSize(btnCancel.getPreferredSize());
    		btnOK.setMaximumSize(btnCancel.getPreferredSize());
    		btnOK.setMinimumSize(btnCancel.getPreferredSize());
    		
    	
            JPanel affinePanel = new JPanel();
            affinePanel.setLayout(new BoxLayout(affinePanel, BoxLayout.LINE_AXIS));
            affinePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            affinePanel.add(Box.createHorizontalStrut(15));
            affinePanel.add(new JLabel("Penalties:")); affinePanel.add(Box.createHorizontalStrut(5));
            affinePanel.add(gapLabel);
            affinePanel.add(txtGap);					affinePanel.add(Box.createHorizontalStrut(5));
            affinePanel.add(extendLabel);
            affinePanel.add(txtExtend);					affinePanel.add(Box.createHorizontalGlue());
	
    		JPanel selectPanel = new JPanel();
    		selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.PAGE_AXIS));
    		selectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    		selectPanel.add(btnOrig);
    		selectPanel.add(new JSeparator());
    		selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(new JLabel("AA Only")); 	selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(btnLocal); 					selectPanel.add(Box.createVerticalStrut(5));
    		selectPanel.add(btnLocalAffine);			selectPanel.add(Box.createVerticalStrut(5));
	        selectPanel.add(affinePanel);
	        selectPanel.add(new JSeparator());
	        
    		JPanel buttonPanel = new JPanel();
    		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
    		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    		buttonPanel.add(btnOK);						buttonPanel.add(Box.createHorizontalStrut(20));
    		buttonPanel.add(btnCancel);					buttonPanel.setMaximumSize(buttonPanel.getPreferredSize());
  
           	JPanel mainPanel = new JPanel();
    		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
    		mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    		mainPanel.add(selectPanel); 				mainPanel.add(Box.createVerticalStrut(15));
    		mainPanel.add(buttonPanel);
    		
    		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    		add(mainPanel);
    		
    		pack();
    		this.setResizable(false);
    		UIHelpers.centerScreen(this);
        }
        public int getGap() { return getInt(txtGap.getText().trim(), GAP); }
        public int getExtend() { return getInt(txtExtend.getText().trim(), EXTEND); }
        public int getSelection() { return nMode; }
        	
        JTextField txtGap = null;
        JTextField txtExtend = null;        	
        int nMode = -1;
	}
	private int getInt(String x, int def) {
		try {
			return Integer.parseInt(x);
		}
		catch (Exception e) {}
		return def;
	}
	/**
	 * Called before new pair loaded from either a next>> or <<prev click
	 */
	public int [] getDisplaySettings() {
		int [] retVal = new int[2];
		
		if(menuZoom != null)  retVal[0] = menuZoom.getSelectedIndex();
		retVal[1] = nViewType;
		return retVal;
	}
	/**
	 * Called after new pair loaded on a next, prev click for the PAIR table
	 */
	public void applyDisplaySettings(int [] settings) {
		if(settings == null) return;
		
		if(menuZoom != null && settings[0]<menuZoom.getItemCount()) 
				menuZoom.setSelectedIndex(settings[0]);
		nViewType = settings[1];
		setViewType();
	}
	/***************************************************************/
    private boolean isAAsTCW=false;
    private boolean isHit=true;
	
	// Pair/Hit align
	private JButton btnAlign = null;
	private JCheckBox chkUTR = null;
	private JCheckBox chkHit = null;
	private JCheckBox chkTrim = null;
	
	// All (bases vs graphics)
    private JButton btnViewType = null;
    private int nViewType = PairBasePanel.GRAPHICMODE;
	private JComboBox <MenuMapper> menuZoom = null;
	
	private JScrollPane scroller = null;
	private Vector<PairBasePanel> baseAlignPanelsVec = null;
    
	static final private int GAP_WIDTH = 10, nTopGap = GAP_WIDTH, nBottomGap = GAP_WIDTH / 2, nSideGaps = GAP_WIDTH; 
    private static final long serialVersionUID = 1;	
}
