package util.ui;
import javax.swing.*;
import java.awt.*;

/**
 * Created on Jun 20, 2005
 *
 * A JPanel derived class that displays the message passed to the constructor in
 * the center of its display area.  (Just another compulsive thing to reduce redundant code.)
 *
 * @author brian
 *
 */
public class CenteredMessagePanel extends JPanel
{
	public CenteredMessagePanel ( String strMessage )
	{		
		JPanel centerPanel = new JPanel ( new GridBagLayout () );
		centerPanel.add( new JLabel ( strMessage ) );
		setLayout ( new BorderLayout(5,5) );
		add( BorderLayout.CENTER, centerPanel );		
	}
    
    private static final long serialVersionUID = 1;
}
