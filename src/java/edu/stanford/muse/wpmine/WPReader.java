package edu.stanford.muse.wpmine;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * reads wikipedia xml and generates a page length file with the length of each article
 */

class WPReader {
    private static   PrintStream pageLenStream;
    private static   PrintStream equivsStream;


    static {
        try {
            pageLenStream = new PrintStream(new FileOutputStream("page-lengths"));
            equivsStream = new PrintStream(new FileOutputStream("equiv-pages"));
        } catch (IOException ioe) {
            System.err.println ("Sorry, IOException: " + ioe);
        }
    }

    public static void main(String argv[]) {

        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {

                boolean intext = false;
                boolean intitle = false;
                String text, title = "", redirectTitle = "";
                int pageLen = 0;

                public void startElement(String uri, String localName, String qName,
                                         Attributes attributes) {

                    if (qName.equalsIgnoreCase("title")) {
                        intitle = true;
                        title = "";
                    }

                    if (qName.equalsIgnoreCase("text")) {
                        intext = true;
                    }

                    if (qName.equalsIgnoreCase("redirect")) {
                        redirectTitle = attributes.getValue("title");
                    }
                }

                public void endElement(String uri, String localName, String qName) {

                    if (qName.equalsIgnoreCase("title")) {
                        intitle = false;
                    }

                    if (qName.equalsIgnoreCase("text")) {
                        intext = false;
                    }
                    if (qName.equalsIgnoreCase("page")) {
                        pageLenStream.println(pageLen + " " + title);
                        pageLen = 0;
                    }
                    if (qName.equalsIgnoreCase("redirect")) {
                        equivsStream.println(title.replaceAll(" ", "_") + " " + redirectTitle.replaceAll(" ", "_"));
                    }
                }

                public void characters(char ch[], int start, int length) {

                    if (intitle) {
                        title += new String(ch, start, length);
                    }

                    if (intext) {
                        text = new String(ch, start, length);
                        pageLen += text.length();
                    }
                }

            };

            saxParser.parse(new File(argv[0]), handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
        pageLenStream.close();
        equivsStream.close();

    }

}
