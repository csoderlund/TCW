package sng.amanager;

/***************************************************
 * The File Chooser for the TCW manager
 */
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretListener;

import util.database.Globalx;
import util.methods.BlastArgs;
import util.methods.ErrorReport;
import util.methods.FileHelpers;
import util.methods.Out;
import util.ui.UserPrompt;

public class FileTextField extends JPanel  {
	private static final long serialVersionUID = 6670096855895700182L;
	private static final int TEXTFIELD_WIDTH = 25;
	
	private final static String LIBDIR = Globalx.PROJDIR + "/"; 
	private final static String PROJDIR = Globalx.PROJDIR + "/";
	private final static String ANNODIR = Globalx.ANNODIR + "/";
	
	// default directory
	public static final int PROJ = 1;
	public static final int LIB = 2;
	public static final int ANNO = 3;
	private int pathType;
	
	public static final int FASTA =1;	// Sequence file
	public static final int QUAL = 2;	// Quality file
	public static final int COUNT = 3;	// TCW count file
	public static final int TAB = 4;		// Blast tab
	public static final int USAGE = 5;	// Usage for ORF finder
	public static final int OBO = 6;		// GO Slim formated file
	private int fileType;
	
	private boolean bStdout=false;
	
	// to only verify file
	public FileTextField(String pname, int t) {
		bStdout=true;
		projectName = pname;
		pathType = PROJ;
		fileType = t;
	}
	// to use 'verifyPath' from anywhere
	public FileTextField(ManagerFrame mf, String pname) {
		projectName = pname;
		if (mf==null) bStdout=true;// if bStdout=true, no popups
		else theParentFrame = mf;
	}
	// calls verify when file is selected
	public FileTextField(ManagerFrame mf, int p, int t) {
		theParentFrame = mf;
		pathType = p;
		fileType = t;
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(Globalx.BGCOLOR);
		
		txtValue = new JTextField(TEXTFIELD_WIDTH);
		Dimension dTxt = txtValue.getPreferredSize(); 
		txtValue.setMaximumSize(dTxt);
		txtValue.setMinimumSize(dTxt);
		
		btnFindFile = new JButton("...");
		btnFindFile.setBackground(Globalx.BGCOLOR);
		Dimension dBtn = new Dimension();
		dBtn.width = btnFindFile.getPreferredSize().width;
		dBtn.height = dTxt.height;
		
		btnFindFile.setMaximumSize(dBtn);
		btnFindFile.setMinimumSize(dBtn);
		btnFindFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(getPath());

					if(fc.showOpenDialog(getInstance()) == JFileChooser.APPROVE_OPTION) {
						String file = fc.getSelectedFile().getPath();
						verify(file);
						setDirLast(file, pathType);
						txtValue.setText(pathMakeRelative(file));
					}
				}
				catch(Exception e) {
					ErrorReport.prtReport(e, "Error finding file");
				}		
			}});
		
		add(txtValue);
		add(btnFindFile);
	}
	private ManagerFrame getInstance() {return theParentFrame;}
	
	private File getPath() {
		String lastDir = getLastDir();
		int lastType = getLastType();
		
		if (theParentFrame!=null) // otherwise, was set already
			projectName = theParentFrame.getProjectName();
		
		String d="";
		if (lastDir!=null && pathType==lastType) d = lastDir;
		else if (pathType==1) d = PROJDIR + projectName;
		else if (pathType==2) d = LIBDIR + projectName;
		else if (pathType==3) d = ANNODIR;
		
		File f = new File(d);
		if (!f.exists() || !f.isDirectory()) {
			System.out.println(d + " directory does not exist");
			return new File(".");
		}
		return f;
	}
	
	private boolean verifyFasta(String seqfile) {
		boolean good=true;
		try {
			BufferedReader br = FileHelpers.openGZIP(seqfile);
			if (br==null) {
				prtWarn("Sequence file", seqfile);
				return false;
			}
			String line="";
			while(line.equals("") || line.startsWith("#") || line.startsWith("/")) {
				line = br.readLine();
				if (line==null) {
					Out.PrtWarn("Empty file -- no valid lines");
					prtWarn("Sequence file", seqfile);
					return false;
				}
				else line = line.trim();
			}
			
			if (!line.startsWith(">")) {
				Out.PrtWarn("First non-blank line does not start with >\n" + "Line: " + line);
				prtWarn("Sequence file", seqfile);
				return false;
			}
			
			line = br.readLine().trim();
			if (!line.matches("^[a-zA-Z*]+$")) {
				Out.PrtWarn("First line after > is not all characters.\n" + "Line: " + line);
				prtWarn("Sequence file", seqfile);
				return false;
			}
			// is good, determine type
			int cntP=0, cntN=0;
			if (BlastArgs.isProtein(line)) cntP++;
			if (BlastArgs.isNucleotide(line)) cntN++;
			if (cntP==1 && cntN==0) bIsProtein=true;
			else if (cntP==0 && cntN==1) bIsProtein=false;
			else {
				do {
					line = br.readLine().trim();
				} while (line.startsWith(">"));
				
				if (BlastArgs.isProtein(line)) cntP++;
				if (BlastArgs.isNucleotide(line)) cntN++;
				if (cntP>cntN)  bIsProtein=true;
				else if (cntP<cntN) bIsProtein=false;
				else 
					UserPrompt.showWarn("Cannot determine if file is protein or nucleotide -- " + seqfile);
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Verifying sequence file"); good=false;}

		if (!good) prtWarn("Sequence file", seqfile);
		return good;
	}
	private boolean verifyQFasta(String qualfile) {
		boolean good=true;
		try {
			BufferedReader br = FileHelpers.openGZIP(qualfile);
			if (br==null) {
				prtWarn("Quality file", qualfile);
				return false;
			}
			String line="";
			while(line.equals("") || line.startsWith("#") || line.startsWith("/")) {
				line = br.readLine();
				if (line==null) {
					Out.PrtWarn("Empty file -- no valid lines");
					good=false;
					break;
				}
				else line = line.trim();
			}
			if (line != null) {
				if (!line.startsWith(">")) {
					Out.PrtWarn("First non-blank line does not start with >\n" + "Line: " + line);
					good=false;
				}
				else {
					line = br.readLine().trim();
					if (!line.matches("^[0-9 ]+")) {
						Out.PrtWarn("First line after > has characters other than digits and blanks.\n" + "Line: " + line);
						good=false;
					}
				} 
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Verifying quality file"); good=false;}
		if (!good) prtWarn("Quality file", qualfile);
		return good;
	}
	private boolean verifyCount(String file) {
		boolean good=true;
		try {
			BufferedReader reader = FileHelpers.openGZIP(file);
			if (reader==null) {
				prtWarn("Count file", file);
				return false;
			}
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.trim().length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine();
			}
			reader.close();
			
			if ( line == null ) {
				Out.PrtWarn("File is empty");
				good = false;
			}
			else {
		    		String[] tokens = line.split("\\s");
		    		if (tokens == null || tokens.length < 2) {
		    			Out.PrtWarn("File not white space delimited");
		    			good = false;
		    		}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "error reading " + file); good=false;}
		if (!good) prtWarn("Count file", file);
		return good;
	}
	private boolean verifyBlastTab(String file) {
		boolean good=true;
		try {
			BufferedReader reader = FileHelpers.openGZIP(file);
			if (reader==null) {
				prtWarn("Hit Tabular file", file);
				return false;
			}
			String line = reader.readLine();
			if (line != null) {
				line = line.trim();
				while ( line != null 
						&& (line.trim().length() == 0 || line.charAt(0) == '#') )
							line = reader.readLine();
			}
			reader.close();
			
			if ( line == null ) {
				Out.PrtWarn("File is empty");
				good = false;
			}
			else {
		    		String[] tokens = line.split("\t");
		    		if (tokens == null || tokens.length < 11) {
		    			Out.PrtWarn("File is not -m 8 format");
					good = false;
		    		}
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "error reading " + file); good=false;}
		if (!good) prtWarn("Hit Tabular file", file);
		return good;
	}
	
	private boolean verifyUsage(String file) {
		boolean good=true;
		try {
			BufferedReader reader = FileHelpers.openGZIP(file);
			if (reader==null) {
				prtWarn("Usage file", file);
				return false;
			}
			String line;
			int cnt=0;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length()==0 || line.startsWith("#") || line.startsWith("/")) continue;		
				String [] tok = line.split("\\s+");

				if (tok.length!=2) {
					Out.PrtWarn("Incorrect line: " + line);
					good = false;
				}
				else if (tok[0].length()<3 || tok[0].length()>3) {
					Out.PrtWarn("Incorrect line: " + line);
					good = false;
				}
				else {
					try {
						Double.parseDouble(tok[1]);
					}
					catch (Exception e) {
						Out.PrtWarn("Incorrect line: " + line);
						good = false;
					}
				}
				if (good) cnt++;
			}
			if (cnt==0) {
				Out.PrtWarn("Empty file");
				good = false;
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking file " + file);  good = false;}
		if (!good) prtWarn("Usage file", file);
		return good;
	}
	//[Term]
	//id:
	//name:
	private boolean verifyOBO(String file) {
		boolean good=true;
		try {
			Out.PrtWarn("TCW does not check consistency between GO database and OBO file\n");
			BufferedReader in = new BufferedReader ( new FileReader (file)); 
			String line, name="";
			int gonum=0;
			boolean first=false;
			
			while ((line = in.readLine()) != null) {
				if (line.equals("[Term]")) {
					if (first) {
						if (gonum>0 && !name.equals("")) {
							in.close();
							return true;
						}
						else {
							if (!bStdout) JOptionPane.showMessageDialog(null, "Incorrect OBO file");
							Out.PrtError("Incorrect OBO file:");
							Out.PrtSpMsg(1, "Must contains lines starting with [Term], id:, and name:");
							in.close();
							return false;
						}
					}
					first=true;
				}
				else if (line.startsWith("id:") && line.contains("GO:")) {
					String tmp = line.substring(3).trim(); // remove id:
					tmp = tmp.substring(3).trim(); // remove GO:
					gonum = Integer.parseInt(tmp);
				}
				else if (line.startsWith("name:")) {
					name = line.substring(5).trim();
				}
			}
			in.close();
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Checking file " + file);  good = false;}
		
		return good;
	}
	private void prtWarn(String msg, String file) {
		if (bStdout) {
			System.err.println("Error -- " + msg + " is not be correct");
			System.err.println("   File -- " + file);
		}
		else {
			System.err.println("Error in file -- " + file + "\n");
			UserPrompt.showWarn(msg + " is not correct (see terminal)");
		}
	}
	// Public
	public void addCaretListener(CaretListener l) { txtValue.addCaretListener(l); }
	
	public void setEnabled(boolean enabled) { 
		txtValue.setEnabled(enabled);
		btnFindFile.setEnabled(enabled);
	}
	public void setText(String text) { txtValue.setText(text); }
	public String getText() { return txtValue.getText(); }
	public boolean isProtein() {return bIsProtein;}
	
	public boolean verify(String file) {
		if (!bStdout) System.err.println("Verifying file " + pathRemoveAbs(file));
		
		try {
			File f = new File(file);
			if (!f.exists()) {
				System.err.println("File does not exist: " + file);
				return false;
			}
		}
		catch (Exception e) {ErrorReport.prtReport(e, "Cannot open file " + file); return false;}
		
		if (fileType==1) return verifyFasta(file);
		else if (fileType==2) return verifyQFasta(file);
		else if (fileType==3) return verifyCount(file);
		else if (fileType==4) return verifyBlastTab(file);
		else if (fileType==5) return verifyUsage(file);
		else if (fileType==6) return verifyOBO(file);
		return false;
	}
	/**************************************************
	 * Either:
	 * 	full path
	 * 	file name if in /libraries/<project> or /projects/<project>
	 *  directory starting after /projects/DBfasta
	 */
	public String pathMakeRelative(String filePath) {
		String cur = System.getProperty("user.dir") + "/";
		String absPath;
		
		// check for full path and remove canonical part
		if (pathType==1)      absPath = cur + PROJDIR + projectName;
		else if (pathType==2) absPath = cur + LIBDIR + projectName;
		else if (pathType==3) absPath = cur + ANNODIR.substring(0, ANNODIR.length()-1); // '/' at end
		else return filePath;

		if (filePath.startsWith(absPath)) 
			return filePath.substring(absPath.length()+1);
	
		if (pathType==1)  		absPath = PROJDIR + projectName;
		else if (pathType==2)   	absPath = LIBDIR + projectName;
		else 					absPath =  ANNODIR.substring(0, ANNODIR.length()-1);
	
		if (filePath.startsWith(absPath)) 
			return filePath.substring(absPath.length()+1);
		
		return filePath;
	}
	private String pathRemoveAbs(String path) {
		String d = path;
		String cur = System.getProperty("user.dir");
		int index = path.indexOf(cur);
		if(index == 0) {
			d = path.substring(index + cur.length());
			if (d.startsWith("/")) d = d.substring(1);
		}
		return d;
	}
	
	public String pathToOpen(String filePath, int p) {
		pathType = p;
		
		// full path
		if (filePath.startsWith("/"))
			if (new File(filePath).exists()) return filePath;
		
		// may have "/" before libraries or projects
		if (filePath.startsWith("/")) filePath = filePath.substring(1);
		if (new File(filePath).exists()) return filePath;
		
		// canonical TCW file name
		String fp=filePath;
		if (pathType==1)      fp = PROJDIR + projectName + "/" + filePath;
		else if (pathType==2) fp = LIBDIR + projectName + "/" + filePath;
		else if (pathType==3) fp = ANNODIR + filePath;
		if (new File(fp).exists()) return fp;
	
		// relative to current directory
		fp = System.getProperty("user.dir") + "/" + filePath;
		if (new File(fp).exists()) return fp;
		
		Out.PrtError("File in sTCW.cfg does not exist: " + filePath); 
		return null;
	}
	
	/** static so can use last directory for different FileTextField objects **/
	public static void setDirLast(String path, int t) {
		lastDir = path.substring(0, path.lastIndexOf("/"));
		lastPathType = t;
	}
	public static String getLastDir() { return lastDir;}
	private static int getLastType() { return lastPathType;}
	public static String lastDir=null;
	private static int lastPathType=0;
	
	/** instance variables **/
	private boolean bIsProtein = false;
	private String projectName = null;
	private ManagerFrame theParentFrame = null;
	private JTextField txtValue = null;
	private JButton btnFindFile = null;
}
