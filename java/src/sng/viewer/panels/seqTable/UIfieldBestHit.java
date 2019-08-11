package sng.viewer.panels.seqTable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import util.methods.Static;

public class UIfieldBestHit extends JPanel implements ActionListener  {
	private static final long serialVersionUID = -4802796081022099509L;
	private static final int COL_WIDTH = 170;
	
	public UIfieldBestHit(String [] fieldNames, String[] fieldDesc, boolean hasGOs) {
		
		createPanel(fieldNames, fieldDesc, hasGOs);
	}
	
	private void createPanel(String [] fieldNames, String[] fieldDesc, boolean hasGOs) 
	{	
		setLayout(new BoxLayout ( this, BoxLayout.PAGE_AXIS ));
		super.setBackground(Color.WHITE);
		
		// header row
		JPanel row = Static.createRowPanel();
		row.add(Box.createHorizontalStrut(6));
		row.add(new JLabel("EV")); row.add(Box.createHorizontalStrut(8));
		row.add(new JLabel("AN")); row.add(Box.createHorizontalStrut(8));
		if (hasGOs) row.add(new JLabel("WG")) ;
		add(row);
		
		firstChk = new JCheckBox[fieldNames.length];
		overChk = new JCheckBox[fieldNames.length];
		goChk = new JCheckBox[fieldNames.length];
		
		firstLabel = new String [fieldNames.length];
		overLabel = new String [fieldNames.length];
		goLabel = new String [fieldNames.length];
		
		for (int j = 0;  j < fieldNames.length;  j++) {
			firstChk[j] = createCheckBox();
			overChk[j] = createCheckBox();
			goChk[j] = createCheckBox();
			firstLabel[j] = fieldNames[j];
			if (names[j].equals("E-value")) add(new JSeparator());
			else if (names[j].equals("GO")) add(new JSeparator());
			
			row = Static.createRowPanel();
			row.add(firstChk[j]);
			row.add(overChk[j]);
			if (hasGOs) row.add(goChk[j]) ;
			
			row.add(Box.createHorizontalStrut(5));
			JLabel lblName = new JLabel(names[j]);
			row.add(lblName);
			Dimension d = lblName.getPreferredSize();
			if(d.width < COL_WIDTH) row.add(Box.createHorizontalStrut(COL_WIDTH - d.width));
			
			JLabel lblDesc = new JLabel(desc[j]);
	   		lblDesc.setFont(new Font(lblDesc.getFont().getName(),Font.PLAIN,lblDesc.getFont().getSize()));
	   		row.add(lblDesc);
	   		
	   		add(row);
		}
	}
	private JCheckBox createCheckBox() {
		JCheckBox box = new JCheckBox();
	  	box.setBackground(Color.WHITE);
	  	box.addActionListener(this);
	  	return box;
	}
	public void addNames(String [] names, String grpName) {
		if (grpName.equals(FieldContigData.GROUP_NAME_OVER_BEST)) 
			for (int i=0; i<names.length; i++) overLabel[i] = names[i];
		else 
			for (int i=0; i<names.length; i++) goLabel[i] = names[i];
	}
	public void actionPerformed(ActionEvent e) {}
	
	public JCheckBox [] firstChk = null;
	public String [] firstLabel = null;
	public JCheckBox [] overChk = null;
	public String [] overLabel = null;
	public JCheckBox [] goChk = null;
	public String [] goLabel = null;
	
	private String [] names = {
			"Hit ID", "Description", "Species", "DB type", "Taxo", "E-value",
			"BitScore", "%Sim", "Align", "SeqStart", "SeqEnd", "%SeqCov",
			"HitStart", "HitEnd", "%HitCov",
			"GO", "InterPro", "KEGG", "Pfam", "EC"};
	private String [] desc = {
			"Identifier", "Description", "Species", 
			"Type (e.g. SP (SwissProt) or TR (TrEMBL))", "Taxonomy of the database", 
			"E-value", "Bit Score", "Percent similarity", "Alignment length (amino acid)", 
			"Start of the match to the sequence", "End of the match to the sequence", 
			"Percent coverage of sequence",
			"Start of the match to the hit", "End of the match to the hit", 
			"Percent coverage of hit",
			"GO (Gene Ontology) term", "InterPro Identifier", "KEGG Identifier", "Pfam Identifier", "EC (enzyme) ID"};	
}
