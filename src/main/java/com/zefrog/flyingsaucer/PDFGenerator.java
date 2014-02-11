package com.zefrog.flyingsaucer;

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

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

import com.lowagie.text.DocumentException;

public class PDFGenerator implements Runnable{
	
	private String encoding;
	private InputStream is;
	private OutputStream os;
	private Socket socket = null;
	
	private static final Logger log4j = Logger.getLogger(PDFGenerator.class);
	
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
		log4j.info("Starting new converter thread");

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
			
			log4j.info("HTML input parsed");
			
			// render pdf from tidy html
			renderer.setDocumentFromString(htmlOutputStream.toString(encoding));
			renderer.layout();
	
			// output to system out
			renderer.createPDF(this.os);
			
			log4j.info("PDF Created, closing connection");
			
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

		// configure log4j
		BasicConfigurator.configure();
		
		// get options
		Options options = new Options();
		options.addOption("encoding", true, "Encoding");
		options.addOption("host", true, "Hostname to run on (default localhost)");
		options.addOption("port", true, "Local port to run server");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		String encoding = cmd.getOptionValue("encoding");
		String port = cmd.getOptionValue("port");
		// if null, then the loopback is returned (localhost)
		String host = cmd.getOptionValue("host");
		
		// version local server
		if (null != port) {
			InetAddress addr = InetAddress.getByName(host);
			ServerSocket socketserver = new ServerSocket(Integer.parseInt(port), 0, addr);
			log4j.info("PDFConverter server started on " + addr + ":" + port);
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
			// classic version, STDIN > STDOUT
			Thread pdfGen = new Thread(new PDFGenerator(encoding, System.in, System.out));
			pdfGen.start();
		}
	}
}
