package cmp.viewer.seq;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import cmp.database.Globals;
import util.database.Globalx;

public class LegendPanel extends JPanel
{
	private static final long serialVersionUID = -5292053168212278988L;
	
	public LegendPanel (int type) 
	{
		this.type=type;
		setLayout( null );
        setBackground( Globals.BGCOLOR );
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        
        setMinimumSize(new Dimension(340, 110)); // width, height
        setPreferredSize(getMinimumSize());
        setMaximumSize(getMinimumSize());
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent( g );
		Graphics2D g2 = (Graphics2D)g;
	
		int y = 10;
		int x = 10;
		g2.drawString("Legend", x, y+=10);
		x += 25;
		
		g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, g2.getFont().getSize()));

		drawKey(0, g2, Globalx.gapGreen, 	    "Gap", 		x+20, y+=15);
		if (type==Globals.AA) {
			drawKey(0, g2, Globalx.mismatchRed, 	Globalx.blosumNeg, x+20, y+=15);
			drawKey(0, g2, Globalx.purple, 		Globalx.blosumPosLegend, x+20, y+=15);
			drawKey(0, g2, Globalx.mediumGray, 	"Unknown or extended end", 	 x+20, y+=15);
			drawKey(1, g2, Globalx.mediumGray, 	"In Frame Stop Codons", 	 x+20, y+=15);
		}
		else {
			drawKey(0, g2, Globalx.mismatchRed, 	"Mismatch", x+20, y+=15);
			drawKey(0, g2, Globalx.mediumGray, 	"Unknown or extended end", 	 x+20, y+=15);
		}
	}

	private static void drawKey(int type, Graphics2D g2, Color c, String s, int x, int y) 
	{
		g2.setColor(c);
		if (type == 0)
			g2.fillRect(x, y, 10, 10);
		else {
			Polygon triangle = new Polygon();
			triangle.addPoint(x,y);
			triangle.addPoint(x+10,y);
			triangle.addPoint(x+5,y+10);
			g2.fill(triangle);
		}
		
		g2.setColor(Color.BLACK);
		g2.drawString("= "+s, x+15, y+9);
	}
	private int type=-1;
}
