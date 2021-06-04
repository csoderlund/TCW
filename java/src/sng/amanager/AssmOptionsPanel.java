package sng.amanager;

/*******************************************************
 * Panel for the Options button for instantiate.
 * It defines assembly parameters
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;


import sng.database.Globals;
import util.database.Globalx;
import util.methods.ErrorReport;
import util.methods.TCWprops;
import util.methods.Static;
import util.ui.UserPrompt;

public class AssmOptionsPanel extends JPanel {
	private static final long serialVersionUID = 6967674088911400148L;
	private static final String [] THREE_INT_VALS = { "Minimal Overlap", "Minimal % Identity", "Maximal Overhang" };
	private static final int TEXTFIELD_WIDTH = 15;
	private final String helpHTML = Globals.helpRunDir + "AssemblyOptions.html";
	
	public AssmOptionsPanel(ManagerFrame parentFrame) {
		try {
			theParentFrame = parentFrame;
			
			pnlAssemblyOptions = Static.createPagePanel();
			pnlAssemblyOptions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			JPanel innerPanel = Static.createPageCenterPanel();
			
			JPanel row = Static.createRowPanel();	
			JLabel title = new JLabel("Assembly Options");
			title.setFont(pnlAssemblyOptions.getFont().deriveFont(Font.BOLD, 18));
			row.add(Box.createHorizontalGlue());
			row.add(title);
			row.add(Box.createHorizontalGlue());
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(20));

			row = Static.createRowPanel();	
			innerPanel.add(new JLabel("Assemble: "));
			innerPanel.add(Box.createVerticalStrut(5));
			
			lblClique 			= new JLabel("Clique");
			lblCliqueBlastEval 	= new JLabel("Clique Blast Eval");
			lblCliqueBlastParams = new JLabel("Clique Blast Params");
			lblSelfJoin	 		= new JLabel("Self Join");
			lblBuryBlastEval 	= new JLabel("Bury Blast Eval");
			lblBuryBlastIdentity = new JLabel("Bury Blast Identity");
			lblBuryBlastParams 	= new JLabel("Bury Blast Params");
			lblCapArgs 			= new JLabel("CAP Args");
			lblTCBlastEval		= new JLabel("TC Blast Eval");
			lblTCBlastParams	= new JLabel("TC Blast Params");
			lblUserESTSelfBlast  = new JLabel("User EST Self Blast"); 
			
			lblClique.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblClique.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblClique.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastEval.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastEval.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastEval.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastParams.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastParams.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblCliqueBlastParams.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblSelfJoin.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblSelfJoin.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblSelfJoin.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastEval.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastEval.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastEval.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastIdentity.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastIdentity.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastIdentity.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastParams.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastParams.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblBuryBlastParams.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblCapArgs.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblCapArgs.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblCapArgs.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastEval.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastEval.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastEval.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastParams.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastParams.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblTCBlastParams.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			lblUserESTSelfBlast.setMaximumSize(lblCliqueBlastParams.getPreferredSize());
			lblUserESTSelfBlast.setMinimumSize(lblCliqueBlastParams.getPreferredSize());
			lblUserESTSelfBlast.setPreferredSize(lblCliqueBlastParams.getPreferredSize());
			
			CaretListener changeListener = new CaretListener() {
				public void caretUpdate(CaretEvent e) {
					if(pnlClique.isValidVals() && pnlSelfJoin.isValidVals())
						btnKeepAssem.setEnabled(true);
					else
						btnKeepAssem.setEnabled(false);
				}
			};
			row = Static.createRowPanel();		
			pnlClique = new MultiIntPanel(THREE_INT_VALS);
			pnlClique.setChangeListener(changeListener);
			row.add(lblClique);
			row.add(Box.createHorizontalStrut(10));
			row.add(pnlClique);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(10));
	
			row = Static.createRowPanel();		
			txtCliqueBlastEval = new JTextField(TEXTFIELD_WIDTH);
			txtCliqueBlastEval.setMaximumSize(txtCliqueBlastEval.getPreferredSize());
			txtCliqueBlastEval.setMinimumSize(txtCliqueBlastEval.getPreferredSize());
			row.add(lblCliqueBlastEval);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtCliqueBlastEval);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(10));
			
			row = Static.createRowPanel();		
			txtCliqueBlastParams = new JTextField(TEXTFIELD_WIDTH);
			txtCliqueBlastParams.setMaximumSize(txtCliqueBlastParams.getPreferredSize());
			txtCliqueBlastParams.setMinimumSize(txtCliqueBlastParams.getPreferredSize());
			row.add(lblCliqueBlastParams);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtCliqueBlastParams);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(10));
			
			row = Static.createRowPanel();		
			pnlSelfJoin = new MultiIntPanel(THREE_INT_VALS);
			pnlSelfJoin.setChangeListener(changeListener);
			row.add(lblSelfJoin);
			row.add(Box.createHorizontalStrut(10));
			row.add(pnlSelfJoin);
			innerPanel.add(row);
	
			innerPanel.add(Box.createVerticalStrut(20));
			innerPanel.setMaximumSize(innerPanel.getPreferredSize());
			
			row = Static.createRowPanel();		
			txtBuryBlastEval = new JTextField(TEXTFIELD_WIDTH);
			txtBuryBlastEval.setMaximumSize(txtBuryBlastEval.getPreferredSize());
			txtBuryBlastEval.setMinimumSize(txtBuryBlastEval.getPreferredSize());
			row.add(lblBuryBlastEval);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtBuryBlastEval);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();		
			txtBuryBlastIdentity = new JTextField(TEXTFIELD_WIDTH);
			txtBuryBlastIdentity.setMaximumSize(txtBuryBlastIdentity.getPreferredSize());
			txtBuryBlastIdentity.setMinimumSize(txtBuryBlastIdentity.getPreferredSize());
			row.add(lblBuryBlastIdentity);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtBuryBlastIdentity);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();		
			txtBuryBlastParams = new JTextField(TEXTFIELD_WIDTH);
			txtBuryBlastParams.setMaximumSize(txtBuryBlastParams.getPreferredSize());
			txtBuryBlastParams.setMinimumSize(txtBuryBlastParams.getPreferredSize());
			row.add(lblBuryBlastParams);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtBuryBlastParams);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();		
			txtCapArgs = new JTextField(TEXTFIELD_WIDTH);
			txtCapArgs.setMaximumSize(txtCapArgs.getPreferredSize());
			txtCapArgs.setMinimumSize(txtCapArgs.getPreferredSize());
			row.add(lblCapArgs);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtCapArgs);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();		
			txtTCBlastEval = new JTextField(TEXTFIELD_WIDTH);
			txtTCBlastEval.setMaximumSize(txtTCBlastEval.getPreferredSize());
			txtTCBlastEval.setMinimumSize(txtTCBlastEval.getPreferredSize());
			row.add(lblTCBlastEval);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtTCBlastEval);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();		
			txtTCBlastParams = new JTextField(TEXTFIELD_WIDTH);
			txtTCBlastParams.setMaximumSize(txtTCBlastParams.getPreferredSize());
			txtTCBlastParams.setMinimumSize(txtTCBlastParams.getPreferredSize());
			row.add(lblTCBlastParams);
			row.add(Box.createHorizontalStrut(10));
			row.add(txtTCBlastParams);
			innerPanel.add(row);
			innerPanel.add(Box.createVerticalStrut(5));
			
			row = Static.createRowPanel();
			tblTC = new TCTable();
			tblTC.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent arg0) {
					btnTCRemove.setEnabled(tblTC.getSelectedRow() >= 0);
				}
			});

			tcPane = new JScrollPane(tblTC);
			tcPane.getViewport().setBackground(Globals.BGCOLOR);
			
			Dimension tcDim = tcPane.getPreferredSize();
			tcDim.height = 100;
			tcPane.setPreferredSize(tcDim);
			
			JPanel TCTableCtrlPanel = Static.createPageCenterPanel();
			
			btnTCAdd = new JButton("Add");
			btnTCAdd.setBackground(Globals.BGCOLOR);
			btnTCAdd.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					String val = tblTC.getLastRow();
					if(val.length() == 0) val = "0 0 0";
					tblTC.addRow(val);
					
					tblTC.setVisible(false);
					tblTC.setVisible(true);
				}
			});
			btnTCRemove = new JButton("Remove");
			btnTCRemove.setBackground(Globals.BGCOLOR);
			btnTCRemove.setEnabled(false);
			btnTCRemove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					tblTC.removeRow(tblTC.getSelectedRow());
					tblTC.getSelectionModel().clearSelection();
					
					tblTC.setVisible(false);
					tblTC.setVisible(true);
				}
			});
			btnTCRemove.setMaximumSize(btnTCRemove.getPreferredSize());
			btnTCRemove.setMinimumSize(btnTCRemove.getPreferredSize());
			btnTCAdd.setMaximumSize(btnTCRemove.getPreferredSize());
			btnTCAdd.setMinimumSize(btnTCRemove.getPreferredSize());
			
			TCTableCtrlPanel.add(btnTCAdd);
			TCTableCtrlPanel.add(Box.createVerticalStrut(10));
			TCTableCtrlPanel.add(btnTCRemove);
			
			row.add(tcPane);
			row.add(Box.createHorizontalStrut(5));
			row.add(TCTableCtrlPanel);
			innerPanel.add(row);
			
			JButton btnResetDefaults = new JButton(Globalx.defaultBtn);
			btnResetDefaults.setBackground(Globals.BGCOLOR);
			btnResetDefaults.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					try {
						TCWprops theProps = new TCWprops(TCWprops.PropType.Assem);
						pnlClique.setValue(theProps.getProperty("CLIQUE"));
						txtCliqueBlastEval.setText(theProps.getProperty("CLIQUE_BLAST_EVAL"));
						txtCliqueBlastParams.setText(theProps.getProperty("CLIQUE_BLAST_PARAMS"));
						pnlSelfJoin.setValue(theProps.getProperty("SELF_JOIN"));
						txtBuryBlastEval.setText(theProps.getProperty("BURY_BLAST_EVAL"));
						txtBuryBlastIdentity.setText(theProps.getProperty("BURY_BLAST_IDENTITY"));
						txtBuryBlastParams.setText(theProps.getProperty("BURY_BLAST_PARAMS"));
						txtCapArgs.setText(theProps.getProperty("CAP_ARGS"));
						txtTCBlastEval.setText(theProps.getProperty("TC_BLAST_EVAL"));
						txtTCBlastParams.setText(theProps.getProperty("TC_BLAST_PARAMS"));
						
						tblTC.clearAll();
						boolean doneReadingTC = false;
						int TCIdx = 1;
						while(!doneReadingTC) {
							try {
								String val = theProps.getProperty("TC" + TCIdx);
								if(val.length() > 0)
									tblTC.addRow(val);
								else
									doneReadingTC = true;
								TCIdx++;
							}
							catch(Exception e) {
								doneReadingTC = true;
							}
						}
						tblTC.setVisible(false);
						tblTC.setVisible(true);
					}
					catch(Exception e) {
						ErrorReport.prtReport(e, "");
					}
				}
			});
			
			btnKeepAssem = new JButton(Globalx.keepBtn);
			btnKeepAssem.setBackground(Globals.BGCOLOR);
			btnKeepAssem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ManagerData.AsmData asmObj = theParentFrame.getCurManData().getAsmObj();
					asmObj.setClique(pnlClique.getValue());
					asmObj.setCliqueBlastEval(txtCliqueBlastEval.getText());
					asmObj.setCliqueBlastParam(txtCliqueBlastParams.getText());
					asmObj.setSelfJoin(pnlSelfJoin.getValue());
					asmObj.setBuryBlastEval(txtBuryBlastEval.getText());
					asmObj.setBuryBlastIdentity(txtBuryBlastIdentity.getText());
					asmObj.setBuryBlastParams(txtBuryBlastParams.getText());
					asmObj.setCAPArgs(txtCapArgs.getText());
					asmObj.setSelfJoin(pnlSelfJoin.getValue());
					asmObj.setTCBlastEval(txtTCBlastEval.getText());
					asmObj.setTCBlastParams(txtTCBlastParams.getText());
					asmObj.clearTCs();
					for(int x=0; x<tblTC.getRowCount(); x++)
						asmObj.addTC(tblTC.getRawRow(x));
					
					setVisible(false);
					theParentFrame.setMainPanelVisible(true);
					
					//theParentFrame.updateUI();
					theParentFrame.saveProject();
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}
			});
			JButton btnDiscard = new JButton(Globalx.cancelBtn);
			btnDiscard.setBackground(Globals.BGCOLOR);
			btnDiscard.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					theParentFrame.setMainPanelVisible(true);
					theParentFrame.setFrameMode(ManagerFrame.FRAME_MODE_MAIN);
				}
			});
			
			JButton btnHelp = new JButton("Help");
			btnHelp.setBackground(Globals.HELPCOLOR);
			btnHelp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					UserPrompt.displayHTMLResourceHelp(theParentFrame, 
							"Assembly options help", helpHTML);
				}
			});
			
			JPanel buttonRow = new JPanel();
			buttonRow.setLayout(new BoxLayout(buttonRow, BoxLayout.LINE_AXIS));
			buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
			buttonRow.setBackground(Globals.BGCOLOR);
			
			buttonRow.add(btnKeepAssem);
			buttonRow.add(Box.createHorizontalStrut(15));
			buttonRow.add(btnDiscard);
			buttonRow.add(Box.createHorizontalStrut(15));
			buttonRow.add(btnResetDefaults);
			buttonRow.add(Box.createHorizontalStrut(15));
			buttonRow.add(btnHelp);
			
			buttonRow.setMaximumSize(buttonRow.getPreferredSize());
			buttonRow.setMinimumSize(buttonRow.getPreferredSize());
			
			innerPanel.setMaximumSize(innerPanel.getPreferredSize());
			innerPanel.setMinimumSize(innerPanel.getPreferredSize());
			
			pnlAssemblyOptions.add(innerPanel);
			pnlAssemblyOptions.add(Box.createVerticalStrut(10));
			pnlAssemblyOptions.add(buttonRow);
	
			setBackground(Globals.BGCOLOR);
			add(pnlAssemblyOptions);
			setVisible(false);
		}
		catch(Exception e) {
			ErrorReport.prtReport(e, "Error Building assembly option panel");
		}
	}
	public void actionOptionsTrans() {
		try {
			TCWprops theProps = new TCWprops(TCWprops.PropType.Assem);
			ManagerData.AsmData asmObj = theParentFrame.getCurManData().getAsmObj();
			asmObj = theParentFrame.getCurManData().getAsmObj();
			if(asmObj.getClique().length() > 0)
				pnlClique.setValue(asmObj.getClique());
			else
				pnlClique.setValue(theProps.getProperty("CLIQUE"));
			
			if(asmObj.getCliqueBlastEval().length() > 0)
				txtCliqueBlastEval.setText(asmObj.getCliqueBlastEval());
			else
				txtCliqueBlastEval.setText(theProps.getProperty("CLIQUE_BLAST_EVAL"));
			
			if(asmObj.getCliqueBlastParam().length() > 0)
				txtCliqueBlastParams.setText(asmObj.getCliqueBlastParam());
			else
				txtCliqueBlastParams.setText(theProps.getProperty("CLIQUE_BLAST_PARAMS"));

			if(asmObj.getSelfJoin().length() > 0)
				pnlSelfJoin.setValue(asmObj.getSelfJoin());
			else
				pnlSelfJoin.setValue(theProps.getProperty("SELF_JOIN"));
			
			if(asmObj.getBuryBlastEval().length() > 0)
				txtBuryBlastEval.setText(asmObj.getBuryBlastEval());
			else
				txtBuryBlastEval.setText(theProps.getProperty("BURY_BLAST_EVAL"));

			if(asmObj.getBuryBlastIdentity().length() > 0)
				txtBuryBlastIdentity.setText(asmObj.getBuryBlastIdentity());
			else
				txtBuryBlastIdentity.setText(theProps.getProperty("BURY_BLAST_IDENTITY"));

			if(asmObj.getBuryBlastParams().length() > 0)
				txtBuryBlastParams.setText(asmObj.getBuryBlastParams());
			else
				txtBuryBlastParams.setText(theProps.getProperty("BURY_BLAST_PARAMS"));

			if(asmObj.getCAPArgs().length() > 0)
				txtCapArgs.setText(asmObj.getCAPArgs());
			else
				txtCapArgs.setText(theProps.getProperty("CAP_ARGS"));

			if(asmObj.getTCBlastEval().length() > 0)
				txtTCBlastEval.setText(asmObj.getTCBlastEval());
			else
				txtTCBlastEval.setText(theProps.getProperty("TC_BLAST_EVAL"));

			if(asmObj.getTCBlastParams().length() > 0)
				txtTCBlastParams.setText(asmObj.getTCBlastParams());
			else
				txtTCBlastParams.setText(theProps.getProperty("TC_BLAST_PARAMS"));

			if(asmObj.getNumTCs() > 0) {
				tblTC.clearAll();
				for(int x=0; x<asmObj.getNumTCs(); x++)
					tblTC.addRow(asmObj.getTCAt(x));
			}
			else {
				tblTC.clearAll();
				boolean doneReadingTC = false;
				int TCIdx = 1;
				while(!doneReadingTC) {
					try {
						String val = theProps.getProperty("TC" + TCIdx);
						if(val.length() == 0)
							doneReadingTC = true;
						else
							tblTC.addRow(val);
						TCIdx++;
					}
					catch(Exception e) {
						doneReadingTC = true;
					}
				}
			}			
			setBackground(Globals.BGCOLOR);
			add(pnlAssemblyOptions);
			setVisible(false);
			
			updateUI();
		}
		catch(Exception e) {ErrorReport.prtReport(e, "Error setting Assembly options");}
	}
	
	public class MultiIntPanel extends JPanel {
		private static final long serialVersionUID = -5168685239942043613L;
		
		public MultiIntPanel(String [] labels) {
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			setBackground(Globals.BGCOLOR);
			
			theLabels = new JLabel[labels.length];
			theFields = new JTextField[labels.length];
			for(int x=0; x<theLabels.length; x++) {
				JPanel section = new JPanel();
				section.setLayout(new BoxLayout(section, BoxLayout.PAGE_AXIS));
				section.setBackground(Globals.BGCOLOR);
				
				theLabels[x] = new JLabel(labels[x]);
				theLabels[x].setAlignmentX(Component.CENTER_ALIGNMENT);
				theFields[x] = new JTextField(5);
				theFields[x].setAlignmentX(Component.CENTER_ALIGNMENT);
				theFields[x].setMaximumSize(theFields[x].getPreferredSize());
				theFields[x].setMinimumSize(theFields[x].getPreferredSize());

				section.add(theLabels[x]);
				section.add(theFields[x]);
				
				add(section);
				if(x < theLabels.length - 1)
					add(Box.createHorizontalStrut(10));
			}
		}
		
		public boolean isEmptyVals() {
			if(theFields == null) return true;
			boolean empty = true;
			for(int x=0; x<theFields.length && empty; x++) {
				if(theFields[x].getText().length() > 0) {
					empty = false;
				}
			}
			return empty;
		}
	
		public void setChangeListener(CaretListener l) {
			for(int x=0; x<theFields.length; x++) {
				theFields[x].addCaretListener(l);
			}
		}
		
		public boolean isValidVals() {
			if(isEmptyVals()) return true;
			
			boolean retVal = true;
			
			for(int x=0; x<theFields.length && retVal; x++) {
				if(theFields[x].getText().length() > 0) {
					try {
						Integer.parseInt(theFields[x].getText());
					}
					catch(Exception e) {
						retVal = false;
					}
				}
				else
					retVal = false;
			}
			
			return retVal;
		}
	
		public void setValue(String value) {
			if(value.length() == 0) {
				for(int x=0; x<theFields.length; x++)
					theFields[x].setText("");
			}
			else {
				String [] vals = value.split(" ");
				for(int x=0; x<vals.length; x++) {
					theFields[x].setText(vals[x]);
				}
			}
		}
		
		public String getValue() {
			if(isEmptyVals()) return "";
			
			String retVal = "";
			for(int x=0; x<theFields.length; x++) {
				int val = 0;
				if(theFields[x].getText().length() > 0)
					val = Integer.parseInt(theFields[x].getText());
				retVal += val + " ";
			}
			return retVal.trim();
		}
		
		public void setEnabled(boolean enabled) {
			for(int x=0; x<theFields.length; x++)
				theFields[x].setEnabled(enabled);
			for(int x=0; x<theLabels.length; x++)
				theLabels[x].setEnabled(enabled);
		}
		
		private JTextField [] theFields = null;
		private JLabel [] theLabels = null;
	}	
	public class TCTable extends JTable {
		private static final long serialVersionUID = -5732180166542985071L;

		private final String [] COL_NAMES = { "TC", "Minimal Overlap", "Minimal % Identity", "Maximal Overhang" };
		
		public TCTable() {
			theModel = new TCTableModel();
			
	        setAutoCreateColumnsFromModel( true );
	       	setColumnSelectionAllowed( false );
	       	setCellSelectionEnabled( false );
	       	setRowSelectionAllowed( true );
	       	setShowHorizontalLines( false );
	       	setShowVerticalLines( true );	
	       	setIntercellSpacing ( new Dimension ( 1, 0 ) );
	       	setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	       	setOpaque(true);

	       	setModel(theModel);
	       	
	       	getTableHeader().setBackground(Globals.BGCOLOR);
		}
		
		public void addRow(String rowVal) { theModel.addRow(rowVal); }
		public void removeRow(int rowNum) { theModel.removeRow(rowNum); }
		public void clearAll() { theModel.clearAll(); }
		public String getLastRow() { return theModel.getLastRow(); }
		public String getRawRow(int row) { return theModel.getFullRow(row); }
		
		private TCTableModel theModel = null;
		
		private class TCTableModel extends AbstractTableModel {
			private static final long serialVersionUID = 4271295746709791376L;
			
			public TCTableModel() {
				theRows = new Vector<String> ();
			}

			public void addRow(String val) { 
				theRows.add(val);
			}
			
			public void removeRow(int row) {
				theRows.remove(row);
			}
			
			public void clearAll() {
				theRows.clear();
			}
			
			public int getColumnCount() {
				return COL_NAMES.length;
			}
			
		   	public String getColumnName(int columnIndex) {
	    		return COL_NAMES[columnIndex];
	    	}

			public int getRowCount() {
				return theRows.size();
			}

			public Object getValueAt(int row, int col) {
				if(col == 0) return "TC" + (row + 1);
				return theRows.get(row).split(" ")[col-1];
			}
			
			public String getFullRow(int pos) { return theRows.get(pos); }
				
			public String getLastRow() {
				if(theRows.size() > 0)
					return theRows.get(theRows.size()-1);
				return "";
			}
			
			public void setValueAt(Object val, int row, int column) {
				//Check if valid first
				try {
					Integer.parseInt((String)val);
				}
				catch(Exception e) {
					return ;
				}
				
				String [] vals = theRows.get(row).split(" ");
				vals[column - 1] = (String)val;
				
				String newRow = "";
				for(int x=0; x<vals.length; x++) {
					newRow += vals[x];
					if(x<vals.length-1)
						newRow += " ";
				}
				theRows.set(row, newRow);
			}

			public boolean isCellEditable (int row, int column) { return column>0; }

			private Vector<String> theRows = null;
		}
	}

	//Assembly controls
	private JPanel pnlAssemblyOptions = null;
	private MultiIntPanel pnlClique = null;
	private MultiIntPanel pnlSelfJoin = null;
	
	private JTextField txtCliqueBlastEval = null;
	private JTextField txtCliqueBlastParams = null;
	
	private JTextField txtBuryBlastEval = null;
	private JTextField txtBuryBlastIdentity = null;
	private JTextField txtBuryBlastParams = null;
	private JTextField txtCapArgs = null;
	private JTextField txtTCBlastEval = null;
	private JTextField txtTCBlastParams = null;
	
	private JScrollPane tcPane = null;
	private TCTable tblTC = null;
	private JButton btnTCAdd = null;
	private JButton btnTCRemove = null;
	private JButton btnKeepAssem = null;
	
	private JLabel lblClique 			= null;
	private JLabel lblCliqueBlastEval 	= null;
	private JLabel lblCliqueBlastParams = null;
	private JLabel lblSelfJoin	 		= null;
	private JLabel lblBuryBlastEval 	= null;
	private JLabel lblBuryBlastIdentity = null;
	private JLabel lblBuryBlastParams 	= null;
	private JLabel lblCapArgs 			= null;
	private JLabel lblTCBlastEval		= null;
	private JLabel lblTCBlastParams		= null;
	private JLabel lblUserESTSelfBlast  = null; 
	
	ManagerFrame theParentFrame;
}
