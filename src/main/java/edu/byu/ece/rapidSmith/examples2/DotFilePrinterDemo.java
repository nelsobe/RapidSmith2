package edu.byu.ece.rapidSmith.examples2;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;
import org.jdom2.JDOMException;

import java.io.IOException;


/**
 * This program is used to the test the DotFilePrinter in RapidSmith <br>
 * by displaying a dot file after it has been created <br>
 * 
 * If successful a dot file should automatically display in a separate Java window.
 * 
 * @author Thomas Townsend
 *
 */
public class DotFilePrinterDemo {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {

		if (args.length != 1) {
			System.err.println("Usage: java DotFilePrinterDemo checkpointName");
			System.exit(1);
		}

		System.out.println("Starting DotFilePrinterDemo...\n");
		
		
		System.out.println("Loading Device and Design...");		
		// replace with your file location
		String checkpoint = args[0];
		TincrCheckpoint tcp = null;
		try {
			tcp = VivadoInterface.loadTCP(checkpoint);
		} catch (JDOMException|IOException e) {
			System.err.println("Failed loading TCP");
			e.printStackTrace();
		}
		CellDesign design = tcp.getDesign();
	
		System.out.println("Printing DOT file...");
		// testing dot string stuff	
		DotFilePrinter dotFilePrinter = new DotFilePrinter(design);
		// replace with your file location
		String fileout = "netlist.dot";
		try {
			dotFilePrinter.printPlacementDotFile(fileout);
		} catch (IOException e) {
			System.err.println("Failed writing dot file out");
			e.printStackTrace();
		}

		// dot gives the best results, but the GraphViz program used to render can be changed
		try {
			displayDotFile(fileout);
		} catch (IOException e) {
			System.err.println("Could not display output");
			e.printStackTrace();
		}

		System.out.println("\nDone...");
	}
	
	/**
	 * @param dotFile
	 * @throws IOException
	 */
	public static void displayDotFile(String dotFile) throws IOException  {
		
		// Change this command to whatever command you want executed to view the dot file
		String cmd = String.format("/Applications/Graphviz.app/Contents/MacOS/Graphviz %s", dotFile);
		System.out.println("Executing: " + cmd);
		Runtime.getRuntime().exec(cmd);
	}
}
