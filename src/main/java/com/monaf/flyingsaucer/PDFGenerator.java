package com.monaf.flyingsaucer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
//import org.xhtmlrenderer.util.XRLog;

import com.lowagie.text.DocumentException;

public class PDFGenerator implements Runnable{
	
	private String encoding;
	private InputStream is;
	private OutputStream os;
	private Socket socket = null;
	private boolean debug = false;
	
	public PDFGenerator(String encoding, InputStream is, OutputStream os, boolean debug) {
		this.encoding = encoding;
		this.is = is;
		this.os = os;
		this.debug = debug;
	}
	
	public PDFGenerator(String encoding, Socket socket, boolean debug) throws IOException {
		this(encoding, socket.getInputStream(), socket.getOutputStream(), debug);
		this.socket = socket;
	}

	public void run() {
		if (debug) {
			System.out.println("Starting new converter thread");
		}
		try {
			// create pdf renderer
			ITextRenderer renderer = new ITextRenderer();
	
			// get file output stream for a tidy html
			ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();
	
			// clean up html
			Tidy tidy = new Tidy();
			tidy.setXHTML(true);
			tidy.setHideComments(true);
			tidy.setShowWarnings(false);
			tidy.setQuiet(true);
			tidy.setInputEncoding(encoding);
			tidy.setOutputEncoding(encoding);
			tidy.parse(this.is, htmlOutputStream);
			htmlOutputStream.close();
			
			if (debug) {
				System.out.println("HTML input parsed");
			}
			
			// render pdf from tidy html
			renderer.setDocumentFromString(htmlOutputStream.toString(encoding));
			renderer.layout();
	
			// output to system out
			renderer.createPDF(this.os);
			
			if (debug) {
				System.out.println("PDF Created, closing connection");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// close socket if exists
			if (null != this.socket) {
				try {
					socket.close();
				} catch(IOException e) {}
			}
		}
	}
	
	public static void main(String[] args) throws IOException,
			DocumentException, ParseException {
		
		// enable logging
//		System.getProperties().setProperty("xr.util-logging.loggingEnabled", "true");
//		XRLog.setLoggingEnabled(true);

		// get options
		Options options = new Options();
		options.addOption("encoding", true, "Encoding");
		options.addOption("host", true, "Hostname to run on (default localhost)");
		options.addOption("port", true, "Local port to run server");
		options.addOption("debug", false, "Debug mode");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		String encoding = cmd.getOptionValue("encoding");
		String port = cmd.getOptionValue("port");
		// if null, then the loopback is returned (localhost)
		String host = cmd.getOptionValue("host");
		boolean debug = false;
		if (cmd.hasOption("debug")) {
			debug = true;
		}
		
		// version local server
		if (null != port) {
			InetAddress addr = InetAddress.getByName(host);
			ServerSocket socketserver = new ServerSocket(Integer.parseInt(port), 0, addr);
			System.out.println("PDFConverter server started on " + addr + ":" + port);
			try {
				while (true) {
					Socket client = socketserver.accept();
					try {
						Thread pdfGen = new Thread(new PDFGenerator(encoding, client, debug));
						pdfGen.start();
					} catch(IOException e) {
						client.close();
					}
				}
			} finally {
				socketserver.close();
			}
		} else {
			// version classique, STDIN > STDOUT
			Thread pdfGen = new Thread(new PDFGenerator(encoding, System.in, System.out, debug));
			pdfGen.start();
		}
	}
}
