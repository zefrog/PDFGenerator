package com.monaf.flyingsaucer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	
	
	public PDFGenerator(String encoding, InputStream is, OutputStream os) {
		this.encoding = encoding;
		this.is = is;
		this.os = os;
	}
	
	public PDFGenerator(String encoding, Socket socket) throws IOException {
		this(encoding, socket.getInputStream(), socket.getOutputStream());
		this.socket = socket;
	}
	
	public void run() {
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
	
			// render pdf from tidy html
			renderer.setDocumentFromString(htmlOutputStream.toString(encoding));
			renderer.layout();
	
			// output to system out
			renderer.createPDF(this.os);
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
		options.addOption("port", true, "Local port to run server");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		String encoding = cmd.getOptionValue("encoding");
		String port = cmd.getOptionValue("port");
		
		// version local server
		if (null != port) {
			ServerSocket socketserver = new ServerSocket(Integer.parseInt(port));
			System.out.println("PDFConverter server started on port " + port);
			try {
				while (true) {
					Socket client = socketserver.accept();
					try {
						Thread pdfGen = new Thread(new PDFGenerator(encoding, client));
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
			Thread pdfGen = new Thread(new PDFGenerator(encoding, System.in, System.out));
			pdfGen.start();
		}
	}
}
