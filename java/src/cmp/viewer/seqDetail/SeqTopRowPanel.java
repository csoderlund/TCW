package cmp.viewer.seqDetail;
/*******************************************************
 * Called from All Sequence Table or Members Table on View Details.
 * It creates one row of buttons:
 * 	 Details  Frame 			Prev Next
 */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JLabel;

import util.methods.Static;
import util.ui.UserPrompt;
import cmp.database.Globals;
import cmp.database.Load;
import cmp.viewer.MTCWFrame;
import cmp.viewer.align.AlignButtons;
import cmp.viewer.seq.SeqsTablePanel;
import cmp.viewer.table.FieldData;

public class SeqTopRowPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final String helpHTML = Globals.helpDir + "Details.html"; // CAS305 add
	
	private Color panelColor = Static.PANELCOLOR; 
	private Color activeColor = Static.ACTIVECOLOR; 
	
	private static final String SEQID = FieldData.SEQID;
	
	static final public int SHOW_ASSIGNED_GO = 1;
	static final public int SHOW_ALL_GO = 2;
	static final public int SHOW_SEL_GO = 3;
	static final public int SHOW_SEL_ALL_GO = 4;
	
	public SeqTopRowPanel(MTCWFrame parentFrame, SeqsTablePanel seqTable, String name, int seqid, int row) {
		
		hasGOs = parentFrame.getInfo().hasGOs(); // in database
		isNTdb = parentFrame.getInfo().isNTonly();
		
		theSeqTable = seqTable;
		buildPanel(parentFrame, name, seqid, row);
	}
	private void buildPanel(MTCWFrame parentFrame, String name, int seqid, int row) {
		theViewerFrame = parentFrame;
	
		seqIndex = seqid;
		seqName = name;
		nParentRow = row;
		
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setAlignmentY(LEFT_ALIGNMENT);
		setBackground(Color.WHITE);

		detailPanel = new SeqDetailsPanel(theViewerFrame, seqName, seqIndex); // CAS341 do first so can check for seq info
		 
		JPanel topRow = createTopRow();
	    upperPanel = Static.createPagePanel();
	    upperPanel.add(topRow);
	    upperPanel.add(Box.createVerticalStrut(5)); 
		upperPanel.setMaximumSize( new Dimension ( Integer.MAX_VALUE, (int)upperPanel.getPreferredSize ().getHeight() ) );
	    add(upperPanel);
	    
	    lowerPanel = Static.createPagePanel(); // Details or Frame
	   
	   
	    lowerPanel.add(detailPanel);
	    add(lowerPanel);
	    
	    setInActive(btnDetails);
	}
	
	private JPanel createTopRow() {
	 	JPanel topRow = Static.createRowPanel();
	 	btnDetails = Static.createButtonPanel("Details", true);
	    btnDetails.addActionListener(new ActionListener() {
    	   	public void actionPerformed(ActionEvent arg0) {
    	   		setInActive(btnDetails);
    	   		opDetails();
    	   	}
	    });
	    topRow.add(btnDetails);	 topRow.add(Box.createHorizontalStrut(2));
	   
	    btnFrame = Static.createButtonPanel("Frame", true);
	    btnFrame.addActionListener(new ActionListener() {
    	   	public void actionPerformed(ActionEvent arg0) {
    	   		if (detailPanel.isAAonly()) btnFrame.setEnabled(false); 
    	   		else {
	    	   		setInActive(btnFrame);
	    	   		opFrame();
    	   		}
    	   	}
	    });
	    if (detailPanel.isAAonly()) btnFrame.setEnabled(false); 
	    topRow.add(btnFrame); topRow.add(Box.createHorizontalStrut(2));
	    
	    createBtnGO();
	    if (hasGOs) {
	    	topRow.add(btnGO);
	    	topRow.add(Box.createHorizontalStrut(2));
	    	
	    	if (!detailPanel.hasHits()) btnGO.setEnabled(false);
	    }
	    else topRow.add(Box.createHorizontalStrut(10));
	    lblSelect = Static.createLabel(Globals.select);
	    topRow.add(lblSelect);  topRow.add(Box.createHorizontalStrut(2));
	    
	    AlignButtons bObj = new AlignButtons(theViewerFrame, this); // CAS341 new
	    btnPairwise = bObj.createBtnPairAlign();
	    topRow.add(btnPairwise);					topRow.add(Box.createHorizontalStrut(2));    	
	       
	    createBtnCopy(); // CAS310 add
	    topRow.add(btnCopy); 	topRow.add(Box.createHorizontalGlue());
	    
	    if (!detailPanel.hasHits() && !detailPanel.hasPairs()) {
	    	btnPairwise.setEnabled(false);
		}
	    
	    btnClear  = Static.createButton("Clear", true);
	    btnClear.addActionListener(new ActionListener() {
    	   	public void actionPerformed(ActionEvent arg0) {
    	   		detailPanel.clearSelection();
    	   	}
	    });
	    //topRow.add(btnClear);	topRow.add(Box.createHorizontalGlue()); CAS341 no room
	    
	    btnHelp = Static.createButtonHelp("Help", true);
	    btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(theViewerFrame,  "Details",  helpHTML);
			}
		});
	    topRow.add(btnHelp);
	    
	    JPanel rowChangePanel = Static.createRowPanel();
 	   
	    btnPrevRow = Static.createButton(Globals.prev, true);
	    btnPrevRow.addActionListener(new ActionListener() {
	    		public void actionPerformed(ActionEvent arg0) {
 			   nParentRow = theSeqTable.getTranslatedRow(nParentRow - 1);
 			   loadNewRow(nParentRow);
 		   }
 	   });
	   rowChangePanel.add(btnPrevRow); topRow.add(Box.createHorizontalStrut(1));
	    
 	   btnNextRow = Static.createButton(Globals.next, true);
 	   btnNextRow.addActionListener(new ActionListener() {
 		   public void actionPerformed(ActionEvent arg0) {
 			   nParentRow = theSeqTable.getTranslatedRow(nParentRow + 1);
 			   loadNewRow(nParentRow);
 		   }
 	   });
 	   rowChangePanel.add(btnNextRow);
 	   
 	   topRow.add(rowChangePanel);
 	   return topRow;
	}
	private void createBtnGO() {
		 final JPopupMenu popup = new JPopupMenu();	
	    popup.setBackground(Color.WHITE);
	    
	    popup.add(new JMenuItem(new AbstractAction("Assigned GOs for hits") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				setInActive(btnGO);
    	   		opGO(SHOW_ASSIGNED_GO);
 			}
 		}));
	    popup.add(new JMenuItem(new AbstractAction("All GOs for hits") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				setInActive(btnGO);
    	   		opGO(SHOW_ALL_GO);
 			}
 		}));
	    popup.add(new JMenuItem(new AbstractAction("Assigned GOs for selected hit (or best)") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				setInActive(btnGO);
    	   		opGO(SHOW_SEL_GO);
 			}
 		}));
	    popup.add(new JMenuItem(new AbstractAction("All GOs for selected hit (or best)") {
 			private static final long serialVersionUID = 1L;
 			public void actionPerformed(ActionEvent e) {
 				setInActive(btnGO);
    	   		opGO(SHOW_SEL_ALL_GO);
 			}
 		}));
	    btnGO = Static.createButtonPanel("GO...", true);
		btnGO.addMouseListener(new MouseAdapter() {
	          public void mousePressed(MouseEvent e) {
	              popup.show(e.getComponent(), e.getX(), e.getY());
	          }
	    }); 
	}
	private void opDetails() {
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		lowerPanel.add(detailPanel);
		setVisible(false);
		setVisible(true);
	}
	private void opFrame() {
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		if (theFramePanel!=null) {
			lowerPanel.add(theFramePanel);
			setVisible(false);
			setVisible(true);
		}
		else {
			final MTCWFrame theFrame = theViewerFrame;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						theFramePanel = new SeqFramePanel(theFrame, seqIndex, seqName);
						lowerPanel.add(theFramePanel);
						setVisible(false);
						setVisible(true); // has to be here to work... weird
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}
	private void opGO(int dType) {
		lowerPanel.removeAll();	
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		
		final int displayType = dType;
		final String [] hitInfo=detailPanel.getHitInfo(); // returns best Hit GO if none selected
	
		if (lastDisplay!=dType) theGOPanel = null;
		
		if (displayType>=SHOW_SEL_GO) {
			if (hitInfo[0] != goHit) theGOPanel = null;
			goHit = hitInfo[0];
		}
		else goHit=null;
		
		if (theGOPanel!=null) {
			lowerPanel.add(theGOPanel);
			setVisible(false);
			setVisible(true);
		}
		else {
			lastDisplay=dType;
			final MTCWFrame theFrame = theViewerFrame;
			Thread thread = new Thread(new Runnable() {
				public void run() {
					try {
						if (theGOPanel==null) {
							theGOPanel = new SeqGOPanel(theFrame, detailPanel, 
								seqIndex, seqName, hitInfo, displayType);
						}
						lowerPanel.add(theGOPanel);
						setVisible(false);
						setVisible(true); // has to be here to work... weird
					} catch (Exception e) {e.printStackTrace();}
				}
			});
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}
	private void createBtnCopy() {
		btnCopy = Static.createButtonMenu("Copy...", true);
	    final JPopupMenu copyPop = new JPopupMenu();
	    copyPop.setBackground(Color.WHITE);
	    
	    copyPop.add(new JMenuItem(new AbstractAction(SEQID) {
 			private static final long serialVersionUID = 4692812516440639008L;
 			public void actionPerformed(ActionEvent e) {
 				String seqid = detailPanel.getSeqID();
 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
 				cb.setContents(new StringSelection(seqid), null);
 			}
 		}));
		
		copyPop.add(new JMenuItem(new AbstractAction("AA  Sequence") {
			private static final long serialVersionUID = 4692812516440639008L;
			public void actionPerformed(ActionEvent e) {
				String seqid = detailPanel.getSeqID();
				Load lObj = new Load(theViewerFrame);
				String id = lObj.loadSeq(seqid);
				if (id!=null) {
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(">" + id + "\n" + lObj.getAAseq()), null);
				}
			}
		}));
		if (isNTdb) {
			copyPop.add(new JMenuItem(new AbstractAction("CDS Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String seqid = detailPanel.getSeqID();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadSeq(seqid);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getCDSseq()), null);
					}
				}
			}));
			copyPop.add(new JMenuItem(new AbstractAction("NT  Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String seqid = detailPanel.getSeqID();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadSeq(seqid);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getNTseq()), null);
					}
				}
			}));
		}
		
	    if (detailPanel.hasPairs()) {
	    	copyPop.addSeparator();
			
	 		copyPop.add(new JMenuItem(new AbstractAction("Pair " + SEQID) {
	 			private static final long serialVersionUID = 4692812516440639008L;
	 			public void actionPerformed(ActionEvent e) {
	 				String seqid = detailPanel.getSelectedSeqID();
	 				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
	 				cb.setContents(new StringSelection(seqid), null);
	 			}
	 		}));
			
			copyPop.add(new JMenuItem(new AbstractAction("Pair AA  Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String seqid = detailPanel.getSelectedSeqID();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadSeq(seqid);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getAAseq()), null);
					}
				}
			}));
			if (isNTdb) {
				copyPop.add(new JMenuItem(new AbstractAction("Pair CDS Sequence") {
					private static final long serialVersionUID = 4692812516440639008L;
					public void actionPerformed(ActionEvent e) {
						String seqid = detailPanel.getSelectedSeqID();
						Load lObj = new Load(theViewerFrame);
						String id = lObj.loadSeq(seqid);
						if (id!=null) {
							Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
							cb.setContents(new StringSelection(">" + id + "\n" + lObj.getCDSseq()), null);
						}
					}
				}));
				copyPop.add(new JMenuItem(new AbstractAction("Pair NT  Sequence") {
					private static final long serialVersionUID = 4692812516440639008L;
					public void actionPerformed(ActionEvent e) {
						String seqid = detailPanel.getSelectedSeqID();
						Load lObj = new Load(theViewerFrame);
						String id = lObj.loadSeq(seqid);
						if (id!=null) {
							Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
							cb.setContents(new StringSelection(">" + id + "\n" + lObj.getNTseq()), null);
						}
					}
				}));
			}
	    }
	    if (detailPanel.hasHits()) {
	    	copyPop.addSeparator();
	    	
			copyPop.add(new JMenuItem(new AbstractAction("Hit ID") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String hitid = detailPanel.getSelectedHitID();
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(hitid), null);
				}
			}));
		
			copyPop.add(new JMenuItem(new AbstractAction("Hit Description") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String desc = detailPanel.getSelectedHitDesc();
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					cb.setContents(new StringSelection(desc), null);
				}
			}));
			
			copyPop.add(new JMenuItem(new AbstractAction("Hit Sequence") {
				private static final long serialVersionUID = 4692812516440639008L;
				public void actionPerformed(ActionEvent e) {
					String hitid = detailPanel.getSelectedHitID();
					Load lObj = new Load(theViewerFrame);
					String id = lObj.loadHit(hitid);
					if (id!=null) {
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						cb.setContents(new StringSelection(">" + id + "\n" + lObj.getHitseq()), null);
					}
				}
			}));
	    }
		btnCopy.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				copyPop.show(e.getComponent(), e.getX(), e.getY());
			}
	    });
	}
	
	private void loadNewRow(int rowNum) {
		int index;
		String name;
		
		index = theSeqTable.getNextSeqForDetail(nParentRow);
		name = theSeqTable.getRowSeqName(nParentRow);
		upperPanel.removeAll();
		lowerPanel.removeAll();
		theFramePanel=null;
		theGOPanel = null; // CAS316
		
		nParentRow = rowNum;
		setVisible(false);
		buildPanel(theViewerFrame, name, index, rowNum);
	
        theViewerFrame.changePanelName(this, Globals.tagDETAIL + name + "-" + (rowNum+1), name);
         
		setVisible(true);
	}
	// changes background color 
	private void setInActive(JButton button) {
		// this works on mac but not linux
		btnDetails.setSelected(false);
		btnFrame.setSelected(false);
		btnGO.setSelected(false);
		
		// this works on linux but not mac
		btnDetails.setBackground(panelColor);
		btnFrame.setBackground(panelColor);
		btnGO.setBackground(panelColor);
		
		button.setBackground(activeColor); // CAS337 put here instead of on every button
		button.setSelected(true);
	}
	
	/******************* Pairwise *********************/
	private String tab="", sum="";
	public String [] getAlignSeqIDs() {
		String [] seqs = new String [2];
		seqs[0] = seqName;
		seqs[1] = detailPanel.getSelectedSeqID();
		tab = seqs[1];
		sum = seqName + " to " + seqs[1];
		return seqs;
	}
	public String getHitStr() {
		tab = detailPanel.getSelectedHitID();
		sum = tab + " " + detailPanel.getSelectedHitDesc();
		return tab;
	}
	public String [] getTabSum() {
		String [] retVal = new String [3];
		retVal[0] = tab;
		retVal[1] = sum;
		retVal[2] = "-1";
		return retVal;
	}
	public boolean hasHits() {
		return detailPanel.hasHits();
	}
	public boolean hasPairs() {
		return detailPanel.hasPairs();
	}
	/**************************************************************/
	private JPanel upperPanel;
    private JButton btnDetails = null, btnFrame = null, btnGO = null, btnHelp = null;
    private JButton btnPairwise = null, btnCopy = null,  btnClear = null;
    private JButton btnNextRow = null, btnPrevRow = null;
    private JLabel 	lblSelect = null;
    
    private JPanel 	lowerPanel;
    private SeqDetailsPanel detailPanel = null; // created as startup
    private SeqFramePanel   theFramePanel = null;// created if Frame is selected
    private SeqGOPanel 		theGOPanel = null;
    
    private String goHit=null, seqName;
    private boolean hasGOs=false, isNTdb=true;
    private int lastDisplay=0, nParentRow=0, seqIndex;
	
	private MTCWFrame theViewerFrame = null;
	private SeqsTablePanel theSeqTable = null;
}
