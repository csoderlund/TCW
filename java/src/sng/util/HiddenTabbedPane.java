package sng.util;

import java.awt.CardLayout;
import java.awt.Component;
import javax.swing.JPanel;

public class HiddenTabbedPane extends JPanel {
	private static final long serialVersionUID = -685349637906303338L;
	private static final boolean DEBUG = false;
	
	private Tab selectedTab = null;
	
	// created in StcwFrame.initialize
	public HiddenTabbedPane() {
		setLayout(new CardLayout());
	}
	
	public void addTab(String strTitle, Tab tab) {
		prtMsg("ADDTab::" + strTitle, tab.getID(), tab.getClass().getName() + " #Tabs " + getComponentCount());
		String strTabName = Integer.toString(tab.getID()); // must be unique for card layout
		tab.setTitle(strTitle);
		tab.setName(strTabName);
		add(tab, strTabName);
		printTabs();
	}
	
	public Tab[] getTabs() {
		Component[] comps = getComponents();
		Tab[] tabs = new Tab[comps.length];
		for (int i = 0;  i < comps.length;  i++)
			tabs[i] = (Tab)comps[i];
		return tabs;
	}
	
	// called from StcwFrame when any event in the menuTree occurs
	// also called from CAP3orPhrapTab and ResultSummaryTab
	// sometimes called on view change, e.g. Show Table of SNP but not Show Contig Alignments 
	public void setSelectedTab(Tab t) {
		prtMsg("SETtab: ", t.getID(), t.getClass().getName());

		if (hasTab(t)) {
			CardLayout layout = (CardLayout)getLayout();
			layout.show(this, t.getName());
			selectedTab = t;
		}
		else 
			if (DEBUG) System.err.println("HiddenTabbedPane.setSelectedTab: tab not found ");
	}
	
	public Tab getTabWithTitle(String strTitle) {
		Tab[] tabs = getTabs();
		for (int i = 0;  i < tabs.length;  i++) {
			if (strTitle.equals(tabs[i].getTitle()))
				return tabs[i];
		}
		
		return null;
	}
	
	public void swapInTab(Tab oldTab, String strNewTitle, Tab newTab) {
		prtMsg("swapinTab old: ", oldTab.getID(), oldTab.getClass().getName());
		prtMsg("swapinTab new: ", newTab.getID(), newTab.getClass().getName());
		
		remove(oldTab);
		addTab(strNewTitle, newTab);
		if (selectedTab == null) // caused by remove(oldTab)
			setSelectedTab(newTab);
	}
	
	public boolean hasTab(Tab tab) {
		Tab[] tabs = getTabs();
		for (int i = 0;  i < tabs.length;  i++)
			if (tab == tabs[i])
				return true;
		return false;
	}
	
	public void remove(Tab tab) {
		prtMsg("remove: ", tab.getID(), tab.getClass().getName());

		if (hasTab(tab)) {
			super.remove(tab);
			if (selectedTab == tab) selectedTab = null;
		}
		else if (DEBUG) System.err.println("remove: tab not found");
	}
	
	public void removeAll() {
		prtMsg("removeAll: ", 0, "");
		super.removeAll();
		selectedTab = null;
	}
	
	private void printTabs() {
		if (!DEBUG) return;
		if (getComponentCount()<16) return;
		
		System.err.println("\nTabs: ");
		for (int i = 0;  i < getComponentCount();  i++) {
			Tab tab = (Tab)getComponents()[i];
			System.err.println(i+": TABid="+tab.getID()+" "+tab.getClass().getName());
		}
	}
	private void prtMsg(String from, int id, String parent) {
		if (DEBUG) System.out.format("HiddenTab %-20s  %2d  %s\n", from, id, parent);
	}
}
