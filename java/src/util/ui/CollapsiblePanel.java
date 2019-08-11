package util.ui;

/***********************************
 * Used by sTCW Filter and Column panels for +/- sections
 * the can expand and collapse
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CollapsiblePanel extends JPanel {
	private static final long serialVersionUID = 4157583475514158260L;

	private JPanel panel; // for user-added components
	private JSeparator sep;
	private boolean collapsed;
	private JButton expandButton;
	private ImageIcon expandIcon, collapseIcon;
	private JLabel lblDesc = null;
	private Dimension collapsedSize;

	public CollapsiblePanel(String strTitle, String strDescription) {
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		expandIcon = createImageIcon("/images/plus.gif");
		collapseIcon = createImageIcon("/images/minus.gif");
		
		expandButton = new JButton(strTitle);
		expandButton.setBorderPainted(false);
		expandButton.setFocusPainted(false);
		expandButton.setContentAreaFilled(false);
		expandButton.setMargin(new Insets(5, 0, 5, 0));
		expandButton.addActionListener(
			new ActionListener( ) {
				public void actionPerformed(ActionEvent e) {
					if (collapsed) expand();
					else collapse();
		}});
		
		if (strDescription!=null && strDescription.length()>0) {
			lblDesc = new JLabel("     " + strDescription);
			lblDesc.setFont(new Font(lblDesc.getFont().getName(), Font.PLAIN, lblDesc.getFont().getSize()));
		}
		
		sep = new JSeparator();

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder ( BorderFactory.createEmptyBorder(5, 20, 10, 20) );
		panel.setBackground(Color.WHITE);
		collapse();
		
		super.setBackground(Color.WHITE);
		super.add( expandButton );
		if (lblDesc != null) super.add( lblDesc );
		super.add( sep );
		super.add( panel );
		
		collapsedSize = getPreferredSize();
		setMinimumSize(collapsedSize);
	}
	
	public Component add(Component comp) {
		comp.setVisible(!collapsed);
		panel.add(comp);
		return comp;
	}
	
	private static ImageIcon createImageIcon(String path) {
	    java.net.URL imgURL = CollapsiblePanel.class.getResource(path);
	    if (imgURL != null) return new ImageIcon(imgURL);
	    else {
	    		//System.err.println("Couldn't find icon: "+path);
	    		return null;
	    }
	}
	
	public void collapse() {
		collapsed = true;
		expandButton.setIcon(expandIcon);
		expandButton.setToolTipText("Show options");
		sep.setVisible(false);
		for (int i = 0;  i < panel.getComponentCount();  i++)
			panel.getComponent(i).setVisible(false);
		setMaximumSize(collapsedSize); // prevent vertical stretching problem
	}
	
	public void expand() {
		collapsed = false;
		expandButton.setIcon(collapseIcon);
		expandButton.setToolTipText("Hide options");
		sep.setVisible(true);
		for (int i = 0;  i < panel.getComponentCount();  i++) 
			panel.getComponent(i).setVisible(true);
		setMaximumSize(getPreferredSize()); // prevent vertical stretching problem
	}
	
	public String getTitle() {
		return expandButton.getText();
	}
}
