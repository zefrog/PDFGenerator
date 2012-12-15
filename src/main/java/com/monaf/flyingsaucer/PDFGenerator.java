package com.monaf.flyingsaucer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
//import org.xhtmlrenderer.util.XRLog;

import com.lowagie.text.DocumentException;

public class PDFGenerator {

	public static void main(String[] args) throws IOException,
			DocumentException, ParseException {
		
		// enable logging
//		System.getProperties().setProperty("xr.util-logging.loggingEnabled", "true");
//		XRLog.setLoggingEnabled(true);

		// get options
		Options options = new Options();
		options.addOption("encoding", true, "Encoding");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		String encoding = cmd.getOptionValue("encoding");
		
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
		tidy.parse(System.in, htmlOutputStream);
		htmlOutputStream.close();

		// render pdf from tidy html
		renderer.setDocumentFromString(htmlOutputStream.toString(encoding));
		renderer.layout();

		// output to system out
		renderer.createPDF(System.out, false);
		renderer.finishPDF();
	}
}
