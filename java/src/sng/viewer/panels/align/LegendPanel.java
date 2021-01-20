package sng.viewer.panels.align;

import java.awt.*;
import javax.swing.*;

import util.database.Globalx;

/*
 * Legend beneath contig and pair
 */
public class LegendPanel extends JPanel
{
	private static final long serialVersionUID = -5292053168212278988L;
	private boolean isPair=false;

	public LegendPanel (boolean isPair)         
	{
		this.isPair=isPair;
		setLayout( null );
        setBackground( Color.WHITE );
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        
        if (isPair) setMinimumSize(new Dimension(330, 130));
        else        setMinimumSize(new Dimension(500, 200)); // w,h
        
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

		if (isPair) { // know if db is AA/NT, but not if hit is
			drawKey(0, g2, Globalx.anyGap, 	"Gap", 		x+20, y+=15);
			
			drawKey(0, g2, Globalx.ntMisMatch, "AA " + Globalx.blosumLtEq + " or NT Mismatch ", x+20, y+=15);
			drawKey(0, g2, Globalx.aaEqZero,   "AA " + Globalx.blosumEq, 		x+20, y+=15);
			drawKey(0, g2, Globalx.aaGtZero,   "AA " + Globalx.blosumGtLegend, x+20, y+=15);
			drawKey(1, g2, Globalx.aaStop, 	   "AA Stop Codon", 	 x+20, y+=15);
			drawKey(0, g2, Globalx.anyHang, 	"Unknown or extended end", 	 x+20, y+=15);
		}
		else {
			g2.drawString("Foreground:", x, y+=25);
			drawKey(0, g2,  Globalx.ntMisMatch,   "Mismatch", 	x+20, y+=15);
			drawKey(0, g2, ContigAlignPanel.lowQuality, "Poor Quality Region (phred < 20)", x+20, y+=15);
			drawKey(0, g2, Globalx.anyGap, 	  "Gap (column of gaps may occur if buried not shown)", 	x+20, y+=15);
			drawKey(0, g2, Globalx.anyHang, 	  "N or extended end", x+20, y+=15);
			
			g2.drawString("Background:", x, y+=30);
			drawKey(0, g2, ContigAlignPanel.contigFor, "Forward", x+20, y+=15);
			drawKey(0, g2, ContigAlignPanel.contigRev, "Reverse", x+20, y+=15);
			drawKey(0, g2, ContigAlignPanel.contigErr, "EST has unexpected orientation (e.g. 5' reverse complemented)", x+20, y+=15);
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
}
