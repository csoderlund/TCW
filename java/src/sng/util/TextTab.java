package sng.util;

/********************************************************
 * This is only used by STCWFrame for the overview
 */
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import java.util.Vector;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;

import sng.viewer.STCWFrame;
import util.methods.Static;
import util.ui.UserPrompt;

public class TextTab extends /*JPanel*/Tab 
{
	private static final long serialVersionUID = 4461741225470611773L;
	private JTextArea textArea;
	
	public TextTab ( STCWFrame parentFrame, Tab parentTab, Vector<String> lines, final String htmlHelp )
	{
		super(parentFrame, parentTab);
		setLayout(new BoxLayout ( this, BoxLayout.PAGE_AXIS ));
		
		textArea = new JTextArea();
		textArea.setEditable(false);		
		textArea.setFont(new Font("monospaced", Font.BOLD, 12));
		setContent(lines);
		
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setFont(new Font("monospaced", Font.BOLD, 12));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel  row = Static.createRowPanel();
		row.add(scrollPane);
		add(row);
		add(Box.createVerticalStrut(3));
		
		if (htmlHelp!=null) {
			row = Static.createRowPanel();
			JButton btnHelp = Static.createButtonHelp("Reproduce", true);
			btnHelp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					UserPrompt.displayHTMLResourceHelp(null, "Reproduce Overview", htmlHelp);
				}
			});
			row.add(btnHelp);
			add(row);
		}
	}
	
	public void setContent( Vector<String> lines ) 
	{
		textArea.setText(null);
		if (lines != null)
			for (String line : lines)
				textArea.append(line+"\n");
	}
	
	public void setContent( String lines ) 
	{
		if(textArea != null) {
			textArea.setText(null);
			if (lines != null)
				textArea.append(lines);
		}
	}
	
	public void close()
	{
		textArea = null;
	}
}