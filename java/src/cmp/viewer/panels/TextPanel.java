package cmp.viewer.panels;

/************************************************************
 * This is only used by the multiTCW Overview
 */
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import util.methods.Static;
import util.ui.UserPrompt;
import cmp.database.Globals;

public class TextPanel extends JPanel {
	private static final long serialVersionUID = 6074102283606563987L;
	
	public TextPanel(String text, boolean useHTML, final String htmlHelp) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBackground(Globals.BGCOLOR);
		
		mainPanel = new JEditorPane();
		mainPanel.setEditable(false);
		mainPanel.setFont(new Font("monospaced", Font.BOLD, 12));

		if(useHTML) mainPanel.setContentType("text/html");
		else mainPanel.setContentType("text/plain");
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		setContent(text);
		JScrollPane scrollPane = new JScrollPane(mainPanel);
		scrollPane.setFont(new Font("monospaced", Font.BOLD, 12));
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel  row = Static.createRowPanel();
		row.add(scrollPane);
		add(row);
		add(Box.createVerticalStrut(3));
		
		row = Static.createRowPanel();
		JButton btnHelp = new JButton("Explain");
		btnHelp.setBackground(Globals.HELPCOLOR);
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				UserPrompt.displayHTMLResourceHelp(null, 
						"Overview explanation", htmlHelp);
			}
		});
		row.add(btnHelp);
		add(row);
	}
	
	public void setContent(String text) {
		mainPanel.setText(text);
	}

	private JEditorPane mainPanel = null;
}