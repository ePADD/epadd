/*
 * Copyright (C) 2012 The Stanford MobiSocial Laboratory
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * a general set of utils by Sudheendra... don't introduce dependencies in this
 * file on ANY other libs
 * because it is used by multiple projects.
 * 
 * @author hangal
 */
// warning: do not introduce package dependencies other than java.* classes in
// this collection of utils
// utils that are specific to other libs should go in their own utils file
package edu.stanford.muse.util;

import opennlp.tools.util.featuregen.FeatureGeneratorUtil;
import org.apache.commons.logging.Log;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * a general set of utils by Sudheendra... don't introduce dependencies in this
 * file on ANY other libs
 * because it is used by multiple projects.
 * 
 * @author hangal
 */
public class Util
{
	public static String[]	stopwords	= new String[] { "but", "be", "with", "such", "then", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "there", "in", "that", "they", "was", "it", "an", "the", "as", "at", "these", "to", "of" };
	public static boolean	BLUR		= true;																																																								// blurring of fnames
	private static final long KB = 1024;
	public static final char OR_DELIMITER = ';'; // used to separate parts of fields that can have multipled OR'ed clauses

	public static void setBlur(boolean b) {
		BLUR = b;
	}

	static Pattern	emptyP	= null;
	static {
		emptyP = Pattern.compile("\\W*?\\w+.*");
	}

	// truncates given string to max len, adding ellipsis or padding if
	// necessary
	public static String truncate(String s, int max_len)
	{
		if (s == null)
			s = "???";
		int len = s.length();
		if (len <= max_len)
		{
			for (int i = 0; i < max_len - len; i++)
				s = s + " ";
		}
		else
			s = s.substring(0, max_len - 3) + "...";

		return s;
	}

	public static void ASSERT(boolean b)
	{
		if (!b)
		{
			System.err.println("Assertion failed!\n");
			RuntimeException re = new RuntimeException();
			re.fillInStackTrace();
			print_exception("Assertion failed", re, null /* log */);
			throw re;
		}
	}

	public static boolean nullOrEmpty(String x) {
		return (x == null || "".equals(x));
	}

	public static boolean nullOrNoContent(String x) {
		return (x == null || !emptyP.matcher(x).matches());
	}

	public static <E> boolean nullOrEmpty(E[] a) {
		return (a == null || a.length == 0);
	}

	public static boolean nullOrEmpty(Collection c) {
		return (c == null || c.size() == 0);
	}

	public static boolean nullOrEmpty(Map m) {
		return (m == null || m.size() == 0);
	}

	/**
	 * replaces everything but the first and last letter of the input string s
	 * by '.'
	 * useful for bluring potentially sensitive information that is needed in
	 * log files
	 * like email folder names
	 * e.g. SECRET is returned as S....T
	 */
	public static String blur(String s)
	{
		if (!BLUR)
			return s;

		if (s == null || s.length() <= 1)
			return s;
		char c[] = s.toCharArray();
		for (int i = 1; i < s.length() - 1; i++)
			c[i] = '.';
		return new String(c);
	}

	/**
	 * takes in a path string like a/b/c and blurs only the last component of it
	 */
	public static String blurPath(String s)
	{
		if (!BLUR)
			return s;

		if (s == null || s.length() <= 1)
			return s;

		// compute all path components, blurring the last one
		// s: a/b/xyz
		StringTokenizer st = new StringTokenizer(s, File.separator);
		List<String> components = new ArrayList<>();
		while (st.hasMoreTokens())
		{
			String x = st.nextToken();
			if (st.hasMoreTokens())
				components.add(x); // not last token
			else
				components.add(blur(x)); // last token
		}

		// components {a,b,xyz}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < components.size(); i++)
		{
			result.append(components.get(i));
			if (i < components.size() - 1)
				result.append(File.separator);
		}
		// result: a/b/x.z

		return result.toString();
	}

	/* like assert, bit does not crash */
	public static boolean softAssert(boolean b,Log log)
	{
		warnIf(!b, "Soft assert failed!",log);
		return true;
	}

	/* like assert, bit does not crash */
	public static boolean softAssert(boolean b, String message, Log log)
	{
		warnIf(!b, "Soft assert failed! " + message, log);
		return true;
	}

	public static void warnIf(boolean b, String message, Log log)
	{
		if (b)
		{
			log.warn("REAL WARNING: " + message + "\n");
			breakpoint();
		}
	}

public static void aggressiveWarn(String message, long sleepMillis, Log log)
	{
		String errmsg = "\n\n\n\n\n" + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
				"\n\n\n\n\n\n" + message + "\n\n\n\n\n\n" +
				"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +
				"\n\n\n\n\n";

		log.warn(errmsg);
		if (sleepMillis > 0)
			try {
				Thread.sleep(sleepMillis);
			} catch (Exception e) {
		Util.print_exception("Exception in Thread.sleep",e,log);
			}

	}

	public static void die(String reason)
	{
		System.err.println(reason);
		ASSERT(false);
	}

	public static void breakpoint()
	{
		// permanent breakpoint
	}

	public static void print_exception(String message, Throwable t, Log log)
	{
		String trace = stackTrace(t);
		String s = message + "\n" + t.toString() + "\n" + trace;
		if (log != null)
			log.warn(s);
		System.err.println(s);
	}

	public static void print_exception(Throwable t, Log log)
	{
		print_exception("", t, log);
	}

	public static void print_exception(Throwable t)
	{
		print_exception(t, null);
	}

	public static void report_exception(Throwable t)
	{
		print_exception(t);
		throw new RuntimeException(t);
	}

	public static void report_exception_and_rethrow(Throwable t, Log log)
	{
		print_exception(t, log);
		throw new RuntimeException(t);
	}

	public static String stackTrace(Throwable t)
	{
		StringWriter sw = new StringWriter(0);
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		return sw.getBuffer().toString();
	}

	public static String stackTrace()
	{
		Throwable t = new Exception("Printing current stack trace");
		t.fillInStackTrace();
		return stackTrace(t);
	}

	public static boolean is_doc_filename(String filename)
	{
		// >>Peter: I would include MS Office, Open Office, PDF, electronic
		// publication format, text, rich text format as document.
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".doc") || lower_case_name.endsWith(".docx")
				|| lower_case_name.endsWith(".xls") || lower_case_name.endsWith(".xlsx")
				|| lower_case_name.endsWith(".ppt") || lower_case_name.endsWith(".pptx")
				|| lower_case_name.endsWith(".rtf") || lower_case_name.endsWith(".txt")
				|| lower_case_name.endsWith(".pdf") || lower_case_name.endsWith(".epub")
				|| lower_case_name.endsWith(".odt") || lower_case_name.endsWith(".ods") || lower_case_name.endsWith(".odp");
	}

	public static boolean is_image_filename(String filename)
	{
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".jpg") || lower_case_name.endsWith(".jpeg") || lower_case_name.endsWith(".svg") || lower_case_name.endsWith(".gif") || lower_case_name.endsWith(".png") || lower_case_name.endsWith(".bmp") || lower_case_name.endsWith(".tif") ; // tif
																																																	// files
																																																	// don't
																																																	// render
																																																	// properly
																																																	// in
																																																	// piclens
																																																	// ||
																																																	// lower_case_name.endsWith
																																																	// (".tif");
	}

	public static boolean is_html_filename(String filename)
	{
		// common html extensions
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".htm") || lower_case_name.endsWith(".html")
				|| lower_case_name.endsWith(".asp") || lower_case_name.endsWith(".aspx")
				|| lower_case_name.endsWith(".do") || lower_case_name.endsWith(".jsp");
	}

	public static boolean is_pdf_filename(String filename)
	{
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".pdf");
	}

	public static boolean is_office_document(String filename)
	{
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".ppt") || lower_case_name.endsWith(".pptx") ||
				lower_case_name.endsWith(".doc") || lower_case_name.endsWith(".docx") ||
				lower_case_name.endsWith("xls") || lower_case_name.endsWith(".xlsx");
	}

	public static boolean is_supported_file(String filename)
	{
		String lower_case_name = filename.toLowerCase();
		return lower_case_name.endsWith(".htm") || lower_case_name.endsWith(".html");
	}

	public static void sortFilesByTime(File[] files)
	{
		// sort by creation time of png files to get correct page order
		Arrays.sort(files, (f1, f2) -> {
            long x = f1.lastModified() - f2.lastModified();
            return (x < 0) ? -1 : ((x > 0) ? 1 : 0);
        });
	}

	public static void run_command(String[] cmd) throws IOException
	{
		run_command(cmd, null);
	}

	public static void run_command(String cmd, String dir) throws IOException
	{
		StringTokenizer st = new StringTokenizer(cmd);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens())
			tokens.add(st.nextToken());

		String[] tokensArray = new String[tokens.size()];
		tokens.toArray(tokensArray);
		run_command(tokensArray, dir);
	}

	public static void run_command(String[] cmd, String dir) throws IOException
	{
		// introduced the envp array with the PATH= string after
		// /opt/local/bin/convert started failing
		// while converting pdf to jpg. it would fail
		// saying" sh: gs: command not found"

		File f = null;

		if (dir != null)
		{
			f = new File(dir);
			if (!f.exists())
				f = null;
		}

		Process p = Runtime.getRuntime().exec(cmd, new String[] { "PATH=/opt/local/bin:/bin:/sbin:/usr/bin:/opt/local/sbin" }, f);

		// printing the process's stderr on screen
		Reader r = new InputStreamReader(p.getErrorStream());
		while (true)
		{
			int i = r.read();
			if (i == -1)
				break;
			System.err.print((char) i);
		}
		r.close();

		try {
			p.waitFor();
		} catch (InterruptedException ie) {
			System.out.println("Unable to complete command");
			throw new RuntimeException(ie);
		}
	}

	public static void copy_url_to_file(String url, String filename) throws IOException
	{
		URL u = new URL(url);
		InputStream is = u.openStream();
		copy_stream_to_file(is, filename);
	}

	public static int url_content_size(String url) throws IOException
	{
		URL u = new URL(url);
		InputStream is = u.openStream();
		byte b[] = getBytesFromStream(is);
		return b.length;
	}

	public static long copy_stream_to_file(InputStream is, String filename) throws IOException
	{
		int bufsize = 64 * 1024;
		long nBytes = 0;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			bis = new BufferedInputStream(is, bufsize);
			bos = new BufferedOutputStream(new FileOutputStream(filename), bufsize);
			byte buf[] = new byte[bufsize];
			while (true)
			{
				int n = bis.read(buf);
				if (n <= 0)
					break;
				bos.write(buf, 0, n);
				nBytes += n;

			}
		} finally {
			if (bis != null)
				bis.close();
			if (bos != null)
				bos.close();
		}
		return nBytes;
	}

	public static String removeMetaTag(String contents){
		Pattern p = Pattern.compile("<meta[^>]*>(.*?)/>",
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		return p.matcher(contents).replaceAll("");
	}
	public static void copy_directory(String source,String target) throws IOException {
		final Path sourcename = new File(source).toPath();
		final Path targetname = new File(target).toPath();

		Files.walkFileTree(sourcename, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
				new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
							throws IOException
					{
						Path targetdir = targetname.resolve(sourcename.relativize(dir));
						try {
							Files.copy(dir, targetdir);
						} catch (FileAlreadyExistsException e) {
							if (!Files.isDirectory(targetdir))
								throw e;
						}
						return FileVisitResult.CONTINUE;
					}
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException
					{
						Files.copy(file, targetname.resolve(sourcename.relativize(file)));
						return FileVisitResult.CONTINUE;
					}
				});
	}

	public static void copy_file(String from_filename, String to_filename) throws IOException
	{
		copy_stream_to_file(new FileInputStream(from_filename), to_filename);
	}

	/** returns whether copy was successful */
	public static boolean copyFileIfItDoesntExist(String fromDir, String toDir, String filename) throws IOException
	{
		String toFile = toDir + File.separator + filename;
		String fromFile = fromDir + File.separator + filename;
		if (new File(toFile).exists())
			return true;
		if (!new File(fromFile).exists())
			return false;
		Util.copy_file(fromDir + File.separator + filename, toFile);
		return true;
	}

	/** warning not i18n safe */
	public static void copy_file_to_stream(String filename, Writer pw) throws IOException
	{
		int bufsize = 64 * 1024;
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(filename), bufsize);
			byte buf[] = new byte[bufsize];
			while (true)
			{
				int n = bis.read(buf);
				if (n <= 0)
					break;
				for (int i = 0; i < n; i++)
					pw.write((char) buf[i]);
			}
		} finally {
			if (bis != null)
				bis.close();
		}
	}

	/** return byte array from reading entire inputstream given */
	public static byte[] getBytesFromStream(InputStream is) throws IOException
	{
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// temporary byte buffer
		byte[] byteBuf = new byte[1024 * 64];

		while (true)
		{
			int bytesRead = bis.read(byteBuf);
			if (bytesRead <= 0)
				break;
			baos.write(byteBuf, 0, bytesRead);
		}
		bis.close();

		return baos.toByteArray();
	}

	/** returns byte array containing contents of file at specified path */
	public static byte[] getBytesFromFile(String filename) throws IOException
	{
		File f = new File(filename);
		long len = f.length();
		if (len >= (1L << 32))
			throw new RuntimeException("File " + filename + " is larger than 2^32 bytes: " + len);

		BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));

		int int_len = (int) len;
		byte b[] = new byte[int_len];
		int totalBytesRead = 0;
		while (true)
		{
			int bytesToRead = int_len - totalBytesRead;
			int bytesRead = is.read(b, totalBytesRead, bytesToRead);
			if (bytesRead == 0)
				throw new RuntimeException("Unexpected end of file: " + filename);
			totalBytesRead += bytesRead;
			if (totalBytesRead == int_len)
				break;
		}

		// make sure we read all the bytes... check that the next byte returned
		// is -1
		if (is.read() != -1)
			throw new RuntimeException("File " + filename + " has more than expected bytes: " + len);

		is.close();
		return b;
	}

	/**
	 * returns collection of lines from given file (UTF-8).
	 * trims spaces from the lines,
	 * ignores lines starting with # if ignoreCommentLines is true
	 */
	public static List<String> getLinesFromFile(String filename, boolean ignoreCommentLines) throws IOException
	{
		return getLinesFromInputStream(new FileInputStream(filename), ignoreCommentLines);
	}

	public static List<String> getLinesFromInputStream(InputStream in, boolean ignoreCommentLines) throws IOException
	{
		return getLinesFromReader(new InputStreamReader(in, "UTF-8"), ignoreCommentLines);
	}

	/**
	 * returns collection of lines from given file (UTF-8).
	 * trims spaces from the lines,
	 * ignores lines starting with # if ignoreCommentLines is true
	 */
	public static List<String> getLinesFromReader(Reader reader, boolean ignoreCommentLines) throws IOException
	{
		LineNumberReader lnr = new LineNumberReader(reader);
		List<String> result = new ArrayList<>();
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			line = line.trim();

			if (ignoreCommentLines && (line.length() == 0 || line.charAt(0) == '#'))
				continue;

			result.add(line);
		}
		return result;
	}

	public static String readFile(String file) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			line = line.trim();
			sb.append(line);
			sb.append("\n");
		}
		lnr.close();
		return sb.toString();
	}

	public static String byteArrayToHexString(byte[] ba)
	{
		if (ba == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (byte b : ba)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	/** returns true if the given string contains only digits */
	public static boolean hasOnlyDigits(String s)
	{
		for (char c : s.toCharArray())
			if (!Character.isDigit(c))
				return false;
		return true;
	}

	/** returns true if the given string contains only matchChar */
	public static boolean hasOnlyOneChar(String s, char matchChar)
	{
		for (char c : s.toCharArray())
			if (c != matchChar)
				return false;
		return true;
	}

	/** escape some special xml chars. use at your own risk. */
	public static String escapeXML(String str)
	{
		if (str == null)
			return null;

		// these are the 5 special xml chars according to
		// http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
		str = str.replace("&", "&amp;");
		str = str.replace("'", "&apos;");
		str = str.replace("\"", "&quot;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");

		StringBuilder sb = new StringBuilder();

		// can speed this up if needed by checking if it's the common case of no
		// special chars
		char ca[] = str.toCharArray();
		for (char c : ca)
		{
			if (c > 127)
				try {
					sb.append("&#x" + String.format("%04x", (int) c) + ";");
				} catch (IllegalFormatConversionException ifce) {
					System.out.println("REAL WARNING: illegal format conversion: " + ifce + " char = " + (int) c);
					// ignore it
				}
			else
				sb.append(c);
		}

		return sb.toString();
	}

	public static String repeatUnescapeHTML(String str){
		if(str== null)
			return null;
		String tmp;
		do{
			tmp = new String(str);
			str = unescapeHTML(str);
		}while(!tmp.equals(str));
		return tmp;
	}
	/**
	 * escapes the 5 special html chars - see
	 * http://www.w3schools.com/tags/ref_entities.asp
	 */
	public static String unescapeHTML(String str)
	{
		if (str == null)
			return null;

		// these are the 5 special xml chars according to
		// http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
		str = str.replace("&amp;", "&");
		str = str.replace("&apos;", "'");
		str = str.replace("&quot;", "\"");
		str = str.replace("&lt;", "<");
		str = str.replace("&gt;", ">");
		str = str.replace("\u0095", "."); // remove the damn bullets. // TODO:
											// should use \d format instead to
											// avoid the special char itself.
		return str;
	}

	/**
	 * escapes the 5 special html chars - see
	 * http://www.w3schools.com/tags/ref_entities.asp
	 */
	public static String escapeHTML(String str)
	{
		if (str == null)
			return null;

		// these are the 5 special xml chars according to
		// http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
		str = str.replace("&", "&amp;");
		str = str.replace("'", "&apos;");
		str = str.replace("\"", "&quot;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");
		return str;
	}

	// TODO: should consider escaping the escape char as well. Otherwise,
	// subject to \' -> \\' which leaves the quote unescaped.
	//
	// /** escapes single quote with backslash */
	// public static String escapeSquote (String str)
	// {
	// if (str == null)
	// return null;
	// str = str.replace ("\'", "\\\'");
	// return str;
	// }
	//
	// /** escapes double quote with backslash */
	// public static String escapeDquote (String str)
	// {
	// if (str == null)
	// return null;
	// str = str.replace ("\"", "\\\"");
	// return str;
	// }

	/** capitalizes just the first letter and returns the given string */
	public static String capitalizeFirstLetter(String str)
	{
		if (str == null)
			return null;
		if (str.length() == 0)
			return str;
		if (str.length() == 1)
			return Character.toString((Character.toUpperCase(str.charAt(0))));
		else
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/** capitalizes just the first letter and returns the given string */
	public static boolean allUppercase(String str)
	{
		if (str == null)
			return true;
		for (char c : str.toCharArray())
			if (Character.isLowerCase(c))
				return false;
		return true;
	}

	public static String ellipsize(String s, int maxChars)
	{
		if (s == null)
			return null;

		if (maxChars < 4)
			return (s.substring(0, maxChars));

		if (s.length() > maxChars)
			return s.substring(0, maxChars - 3) + "...";
		else
			return s;
	}

	/** checks if the string has i18n chars, or is plain ascii */
	public static boolean isI18N(String s)
	{
		byte bytes[] = s.getBytes();
		for (byte b : bytes)
			if (b < 0 || b > 127)
				return true;
		return false;
	}


	public static void zipDirectory(String pathToDirectory, String zipfile) throws IOException {
		int i;FileOutputStream fos = new FileOutputStream(zipfile);
		ZipOutputStream zos = new ZipOutputStream(fos);
		File[] files = new File(pathToDirectory).listFiles();

		for(i=0; i < files.length ; i++)
		{
			FileInputStream fin = new FileInputStream(files[i]);

			/*
			 * To begin writing ZipEntry in the zip file, use
			 *
			 * void putNextEntry(ZipEntry entry)
			 * method of ZipOutputStream class.
			 *
			 * This method begins writing a new Zip entry to
			 * the zip file and positions the stream to the start
			 * of the entry data.
			 */

			zos.putNextEntry(new ZipEntry(files[i].getName()));

			/*
			 * After creating entry in the zip file, actually
			 * write the file.
			 */
			int length;

			byte[] buffer=new byte[1024];
			while((length = fin.read(buffer)) > 0)
			{
				zos.write(buffer, 0, length);
			}

			/*
			 * After writing the file to ZipOutputStream, use
			 *
			 * void closeEntry() method of ZipOutputStream class to
			 * close the current entry and position the stream to
			 * write the next entry.
			 */

			zos.closeEntry();

			//close the InputStream
			fin.close();
		}
		zos.close();}
	public static String padWidth(String s, int width)
	{
		if (s == null)
			s = "";
		if (s.length() >= width)
			return s;
		StringBuilder sb = new StringBuilder(s);
		for (int i = 0; i < width - s.length(); i++)
			sb.append(" ");
		return sb.toString();
	}

	/** returns file's extension, and null if it has no extension. Extension has be < MAX_EXTENSION_LENGTH chars */
	public static String getExtension(String filename)
	{
		if (filename == null)
			return null;

		int idx = filename.lastIndexOf(".");
		int MAX_EXTENSION_LENGTH = 6;
		if (idx > 0) // note: > not >= if filename starts with ., its not considered an extension
		{
			int ext_length = filename.length() - idx;
			if (ext_length > 0 && ext_length < MAX_EXTENSION_LENGTH)
				return filename.substring(idx + 1);
		}
		return null;
	}

	public static Pair<String, String> splitIntoFileBaseAndExtension (String filename) {
		String[] parts = filename.split ("\\.(?=[^\\.]+$)"); // see http://stackoverflow.com/questions/4545937/java-splitting-the-filename-into-a-base-and-extension
		if (parts.length == 0)
			return new Pair<>("", "");
		else if (parts.length == 1)
			return new Pair<>(parts[0], "");
		else
			return new Pair<>(parts[0], parts[1]);
	}

	private static void testGetExtension()
	{
		ASSERT(getExtension(".cshrc") == null);
		ASSERT(getExtension("") == null);
		ASSERT(getExtension("no-dot") == null);
		ASSERT(getExtension("a.ppt").equals("ppt"));
	}

	/**
	 * converts a very long name.doc -> a very lon...g.doc.
	 * tries to fit s into maxChars, with a best effort to keep the extension
	 */
	public static String ellipsizeKeepingExtension(String s, int maxChars)
	{
		if (s.length() <= maxChars)
			return s;

		int idx = s.lastIndexOf(".");
		if (idx <= 0)
			return ellipsize(s, maxChars); // no extension

		int MAX_EXTENSION_LENGTH = 6;
		if (s.length() - idx > MAX_EXTENSION_LENGTH)
			return ellipsize(s, maxChars); // unusually long "extension", don't
											// what's happening, play it safe by
											// ignoring it

		// keep everything from one char before the . till the end,
		String tail = s.substring(idx - 1); // tail is [idx-1 to s.length]

		int maxCharsRemaining = maxChars - tail.length();
		return ellipsize(s.substring(0, idx - 1), maxCharsRemaining) + tail;
	}

	/**
	 * blurs a filename but keeps the extension intact.
	 * e.g. "secret.jpg" becomes "s....t.jpg"
	 */
	public static String blurKeepingExtension(String s)
	{
		if (s == null)
			return null;

		int idx = s.lastIndexOf(".");
		if (idx <= 0)
			return blur(s); // no extension

		int MAX_EXTENSION_LENGTH = 6;
		if (s.length() - idx > MAX_EXTENSION_LENGTH)
			return blur(s); // unusually long "extension", don't what's
							// happening, play it safe by ignoring it

		// tail is everything from one char before the . till the end,
		String tail = s.substring(idx); // tail is [idx-1 to s.length]

		// blur the part before tail and append the rest to it.
		return blur(s.substring(0, idx)) + tail;
	}

	public static List<String> tokenize(String s)
	{
		List<String> result = new ArrayList<>();
		if (Util.nullOrEmpty(s))
			return result;

		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens())
			result.add(st.nextToken());
		return result;
	}

	/**
	 * splits by semicolons, lowercases, trims spaces; e.g. given "A; b" returns ["a", "b"].
	 * This syntax is followed by fields that can contain an OR specification.
	 */
	public static Set<String> splitFieldForOr(String s) {
		Collection<String> tokens = Util.tokenize(s, Character.toString(OR_DELIMITER));
		Set<String> result = new LinkedHashSet<>();
		for (String token : tokens)
			result.add(token.toLowerCase().trim());
		return result;
	}


	/** returns true if the filesize satisfies the constraint. neededFileSize is as defined in the adv. search form.
	 * we probably need to avoid hardcoding these limits */
	public static boolean filesizeCheck (String neededFilesize, long size) {
		// these attachmentFilesizes parameters are hardcoded --
		// could make it more flexible if needed in the future
		// "1".."5" are the only valid filesizes. If none of these, this parameter not set
		// and we can include the blob
		if ("1".equals(neededFilesize) || "2".equals(neededFilesize) || "3".equals(neededFilesize) || "4".equals(neededFilesize) || "5".equals(neededFilesize)) { // any other value, we ignore this param
			boolean include = ("1".equals(neededFilesize) && size < 5 * KB) ||
					("2".equals(neededFilesize) && size >= 5 * KB && size <= 20 * KB) ||
					("3".equals(neededFilesize) && size >= 20 * KB && size <= 100 * KB) ||
					("4".equals(neededFilesize) && size >= 100 * KB && size <= 2 * KB * KB) ||
					("5".equals(neededFilesize) && size >= 2 * KB * KB);
			return include;
		}
		return true;
	}
	public static List<String> tokenize(String s, String delims)
	{
		List<String> result = new ArrayList<>();
		if (Util.nullOrEmpty(s))
			return result;

		StringTokenizer st = new StringTokenizer(s, delims);
		while (st.hasMoreTokens())
			result.add(st.nextToken());
		return result;
	}

	public static List<String> tokenizeAlphaChars(String s)
	{
		List<String> result = new ArrayList<>();
		if (Util.nullOrEmpty(s))
			return result;

		int startIdx = -1;
		char[] chars = s.toCharArray();
		boolean inWord = false;
		for (int i = 0; i < chars.length; i++)
		{
			boolean isAlphabetic = Character.isAlphabetic(chars[i]);

			if (isAlphabetic && !inWord) {
				inWord = true;
				startIdx = i;
			}
			// if alphabetic and inWord, nothing to be done
			if (!isAlphabetic && inWord) {
				result.add(s.substring(startIdx, i)); // i will not be included
			}

			inWord = isAlphabetic;
		}
		if (inWord)
			result.add(s.substring(startIdx));

		return result;
	}

	public static Collection<String> breakIntoParas(String input) throws IOException
	{
		List<String> paras = new ArrayList<>();
		LineNumberReader lnr = new LineNumberReader(new StringReader(input));

		StringBuilder currentPara = new StringBuilder();

		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			line = line.trim();

			if (line.length() == 0)
			{
				// end para
				if (currentPara.length() > 0)
					paras.add(currentPara.toString());
				currentPara = new StringBuilder();
			}
			else
			{
				currentPara.append(line);
				currentPara.append("\n");
			}
		}

		// add any residue
		if (currentPara.length() > 0)
			paras.add(currentPara.toString());
		return paras;
	}

	public static void testEllipsizeKeepingExtension()
	{
		ASSERT(ellipsizeKeepingExtension("Olson Melvile Reading Group Description (Revised 08.23.01).doc", 15).equals("Olson M...).doc"));
		ASSERT(ellipsizeKeepingExtension("RobertCreeleyInterview.rtf", 15).equals("RobertC...w.rtf"));
		ASSERT(ellipsizeKeepingExtension("Article_Type1_c=Article_cid=1074381008529_call_pageid=1044442959412_col=1044442957278", 15).equals("Article_Type..."));
		ASSERT(ellipsizeKeepingExtension("Harold I. Cammer to ED,.doc", 15).equals("Harold ...,.doc"));
		ASSERT(ellipsizeKeepingExtension("ED to Harold I. Cammer.doc", 15).equals("ED to H...r.doc"));
		ASSERT(ellipsizeKeepingExtension("permission creeley.doc", 15).equals("permiss...y.doc"));
	}

	/**
	 * safely splits a string into two around the first occurrence of c in s.
	 * always returns an array of 2 strings.
	 * if c does not occur in s, returns an empty string in the second place.
	 */
	public static String[] splitIntoTwo(String s, char c)
	{
		int idx = s.indexOf(c);
		if (idx < 1)
			return new String[] { s, "" };

		String[] result = new String[2];

		result[0] = (idx > 0) ? s.substring(0, idx - 1) : ""; // substring
																// should have
																// accepted args
																// (0, -1) to
																// give an empty
																// string, but
																// it doesn't.
		result[1] = s.substring(idx + 1); // this will always work even if the
											// last character is c, because
											// "abc".substring(3) returns ""
		return result;
	}

	public static String commatize(long n)
	{
		String result = "";
		do
		{
			if (result.length() > 0)
				result = "," + result;
			long trio = n % 1000; // 3 digit number to be printed
			if (trio == n) // if this is the last trio, no lead of leading 0's,
							// otherwise make sure to printf %03f
				result = String.format("%d", n % 1000) + result;
			else
				result = String.format("%03d", n % 1000) + result;

			n = n / 1000;
		} while (n > 0);
		return result;
	}

	/** if num > 1, pluralizes the desc. will also commatize the num if needed. */
	public static String pluralize(int x, String desc)
	{
		return Util.commatize(x) + " " + desc + ((x != 1) ? "s" : ""); // want plural even if x is 0, e.g. "0 messages"
	}

	public static String approximateTimeLeft(long sec)
	{
		int h = (int) sec / 3600;
		int m = (int) (sec % 3600) / 60;

		if (sec > 2 * 3600)
			return "About " + h + " hours left";
		if (h == 1)
			return "About an hour and " + m + " minutes ";
		if (sec > 120)
			return "About " + m + " minutes left";
		if (sec > 90)
			return "A minute and a bit ...";
		if (sec > 60)
			return "About a minute ...";
		if (sec > 30)
			return "Less than a minute ...";
		if (sec > 10)
			return "Less than half a minute ...";
		if (sec >= 2)
			return sec + " seconds left";

		return "Just a sec...";
	}

	/** returns yyyy-mm-dd format for given calendar object */
	public static String formatDate(Calendar c)
	{
		if (c == null)
			return "??-??";
		else
			return c.get(Calendar.YEAR) + "-" + String.format("%02d", (1 + c.get(Calendar.MONTH))) + "-" + String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
	}

	/** returns yyyy-mm-dd format for given date object */
	public static String formatDate(Date d)
	{
		if (d == null)
			return "??-??";
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		return formatDate(c);
	}

	public static String formatDateLong(Calendar d)
	{
		if (d == null)
			return "??-??";
		else
			return d.get(Calendar.YEAR) + "-" + String.format("%02d", (1 + d.get(Calendar.MONTH))) + "-" + String.format("%02d", d.get(Calendar.DAY_OF_MONTH)) + " "
					+ String.format("%02d", d.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", d.get(Calendar.MINUTE)) + ":" +
					String.format("%02d", d.get(Calendar.SECOND));
	}

	public static String formatDateLong(Date d)
	{
		if (d == null)
			return "??-??";
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		return formatDateLong(c);
	}

	// computes basename of s: if s is
	// /a/b/c/hangal@cs.stanford.edu/Mail__Sent__Mail (/ is file.separatorchar,
	// could be \ on windows)
	// returns Mail__Sent__Mail, so that it can be hyperlinked from the top
	// level html file
	public static String baseName(String s)
	{
		if (s == null)
			return null;
		String base = s;
		int idx = base.lastIndexOf(File.separatorChar);
		if (idx >= 0)
			base = base.substring(idx + 1);
		return base;
	}

	/** complement of baseName */
	public static String dirName(String s)
	{
		String dir = "";
		int idx = s.lastIndexOf(File.separatorChar);
		if (idx >= 0)
			dir = s.substring(0, idx);
		return dir;
	}

	/* returns top level domain of given link */
	public static String getTLD(String link)
	{
		int idx = link.indexOf("://");
		// strip the protocol like http:// if present
		// need to handle trailing :// or /
		if (idx > 0 && idx + 1 + "://".length() <= link.length())
			link = link.substring(idx + "://".length());

		// strip out www* if the site starts with that
		if (link.startsWith("www"))
		{
			int idxDot = link.indexOf(".");
			if (idxDot >= 0 && idxDot + 1 <= link.length())
				link = link.substring(idxDot + 1);
		}
		int idxSlash = link.indexOf("/");
		if (idxSlash >= 0)
			link = link.substring(0, idxSlash);
		StringTokenizer st = new StringTokenizer(link, ".");
		int nTokens = st.countTokens();
		int tokensNeeded = 2;
		for (int i = 0; i < (nTokens - tokensNeeded); i++)
			st.nextToken();
		String result = "";
		for (int i = 0; i < tokensNeeded; i++)
		{
			if (!st.hasMoreTokens())
				break;
			if (result.length() > 0)
				result += ".";
			result += st.nextToken();
		}

		return result.toLowerCase();
	}

	/**
	 * normalizes histogram in hist to produce ratios of each position to total
	 * sum
	 */
	public static double[] normalizeHistogram(int[] hist)
	{
		int sum = 0;
		for (int i : hist)
			sum += i;
		return normalizeHistogramToBase(hist, sum);
	}

	/** returns histogram counts divided by given base */
	public static double[] normalizeHistogramToBase(int[] hist, double base)
	{
		double[] result = new double[hist.length];
		for (int i = 0; i < hist.length; i++)
			result[i] = (base == 0) ? 0.0 : ((double) hist[i]) / base;

		return result;
	}

	/**
	 * sanitize domain name for correct email address (so we can identify sent
	 * v/s recd. emails)
	 */
	public static String normalizeServerDomain(String s)
	{
		if (s == null)
			return null;

		if (s.startsWith("imaps."))
			s = s.substring("imaps.".length()); // strip leading imap, usually
												// not part of email addresses,
												// e.g. imap.gmail.com
		else if (s.startsWith("pop3s."))
			s = s.substring("pop3s.".length()); // strip leading imap, usually
												// not part of email addresses,
												// e.g. imap.gmail.com
		else if (s.startsWith("imap."))
			s = s.substring("imap.".length()); // strip leading imap, usually
												// not part of email addresses,
												// e.g. imap.gmail.com
		else if (s.startsWith("pop3."))
			s = s.substring("pop3.".length()); // strip leading imap, usually
												// not part of email addresses,
												// e.g. imap.gmail.com
		else if (s.equals("xenon.stanford.edu"))
			s = "cs.stanford.edu";
		else if (s.equals("csl-mail.stanford.edu"))
			s = "cs.stanford.edu";
		else if (s.endsWith(".pobox.stanford.edu"))
			s = "stanford.edu";
		return s;
	}

	/** reads contents of a (text) file and returns them as a string */
	public static String getFileContents(String filename) throws IOException
	{
		BufferedReader br;
		if (filename.endsWith(".gz"))
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), "UTF-8"));
		else
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));

		StringBuilder sb = new StringBuilder();
		// read all the lines one by one till eof
		while (true)
		{
			String x = br.readLine();
			if (x == null)
				break;

			sb.append(x);
			sb.append("\n");
		}
		br.close();
		return sb.toString();
	}

	// returns a list of dates representing intervals
	// interval i is represented by [i]..[i+1] in the returned value
	public static List<Date> getMonthlyIntervals(Date start, Date end)
	{
		List<Date> intervals = new ArrayList<>();
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(start);
		int startMonth = c.get(Calendar.MONTH);
		int year = c.get(Calendar.YEAR);
		int month = startMonth;

		intervals.add(start);
		while (true)
		{
			month++;
			if (month == 12)
			{
				month = 0;
				year++;
			}

			c = new GregorianCalendar(year, month, 1, 0, 0, 0);
			intervals.add(c.getTime());
			if (c.getTime().after(end))
				break;
		}
		return intervals;
	}

	// returns a list of dates representing intervals
	// interval i is represented by [i]..[i+1] in the returned value
	public static List<Date> getYearlyIntervals(Date start, Date end)
	{
		List<Date> intervals = new ArrayList<>();
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(start);
		int startYear = c.get(Calendar.YEAR);
		int year = startYear;

		intervals.add(start);
		while (true)
		{
			year++;
			c = new GregorianCalendar(year, 0, 1, 0, 0, 0);
			intervals.add(c.getTime());
			if (c.getTime().after(end))
				break;
		}
		return intervals;
	}

	/*
	 * given jun2004-oct2008 with a window size of 12, and step size of 1,
	 * returns:
	 * jun2004-jun2005
	 * jul2004-jul2005
	 * ...
	 * nov2007-nov2008
	 */
/*
	public static List<Pair<Calendar, Calendar>> getSlidingMonthlyIntervalsForward(Calendar start, Calendar end, int windowSizeInMonths, int stepSizeInMonths)
	{
		List<Pair<Calendar, Calendar>> intervals = new ArrayList<Pair<Calendar, Calendar>>();
		if (start == null || end == null)
			return intervals;

		if (start.after(end)) // error
		{
		softAssert(false,JSPHelper.log);
			return intervals;
		}

		Calendar windowStart = start;
		int windowStartMonth = start.get(Calendar.MONTH);
		int windowStartYear = start.get(Calendar.YEAR);

		while (true)
		{
			int windowEndMonth = windowStartMonth + windowSizeInMonths;
			int windowEndYear = windowStartYear;
			if (windowEndMonth >= 12)
			{
				windowEndYear += windowEndMonth / 12;
				windowEndMonth = windowEndMonth % 12;
			}

			Calendar windowEnd = new GregorianCalendar(windowEndYear, windowEndMonth, 1, 0, 0, 0);
			intervals.add(new Pair<Calendar, Calendar>(windowStart, windowEnd));

			if (windowEnd.after(end))
				break;

			// step window start
			windowStartMonth += stepSizeInMonths;
			if (windowStartMonth >= 12)
			{
				windowStartYear += windowStartMonth / 12;
				windowStartMonth = windowStartMonth % 12;
			}

			windowStart = new GregorianCalendar(windowStartYear, windowStartMonth, 1, 0, 0, 0);
		}
		return intervals;
	}
*/

	/**
	 * like forward, but er... backward.
	 */
	public static List<Pair<Date, Date>> getSlidingMonthlyIntervalsBackward(Date start, Date end, int windowSizeInMonths, int stepSizeInMonths)
	{
		Util.die("Unimplemented");
		return null;
		/*
		 * List<Pair<Date,Date>> intervals = new ArrayList<Pair<Date,Date>>();
		 * 
		 * if (start.after(end)) // error
		 * {
		 * softAssert(false);
		 * return intervals;
		 * }
		 * 
		 * if (start == null || end == null)
		 * return intervals;
		 * 
		 * Date windowEnd = end;
		 * int windowEndMonth = end.get(Calendar.MONTH);
		 * int windowEndYear = end.get(Calendar.YEAR);
		 * 
		 * while (true)
		 * {
		 * if (windowEnd.before(start))
		 * break;
		 * 
		 * int windowStartMonth = windowEndMonth - windowSizeInMonths;
		 * int windowStartYear = windowEndYear;
		 * if (windowStartMonth < 0)
		 * {
		 * // e.g. if windowStartMonth is -5, we need to adjust year by 1 and
		 * month to 7. (windowStartMonth/12 is 0)
		 * // e.g. if windowStartMonth is -15, we need to adjust year by 2 and
		 * month to 9.
		 * 
		 * int yearsToAdjust = 1+(windowStartMonth/12);
		 * windowStartYear -= yearsToAdjust;
		 * windowStartMonth += 12*yearsToAdjust;
		 * }
		 * 
		 * Calendar windowStart = new GregorianCalendar(windowStartYear,
		 * windowStartMonth, 1, 0, 0, 0);
		 * intervals.add(new Pair<Date, Date>(windowStart, windowEnd));
		 * 
		 * // step window start
		 * windowEndMonth -= stepSizeInMonths;
		 * if (windowEndMonth < 0)
		 * {
		 * // same logic as above
		 * int yearsToAdjust = 1+(windowEndMonth/12);
		 * windowEndYear -= yearsToAdjust;
		 * windowEndMonth += 12*yearsToAdjust;
		 * }
		 * 
		 * windowEnd = new GregorianCalendar(windowEndYear, windowEndMonth, 1,
		 * 0, 0, 0);
		 * }
		 * return intervals;
		 */
	}

	// strip leading and trailing punctuation
	public static String stripPunctuation(String s)
	{
		// Util.ASSERT (!s.contains(" "));
		// Util.ASSERT (!s.contains("\t"));
		String punctuation = "\r\n\t~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./";

		int start = 0, end = s.length() - 1;
		for (; start < s.length(); start++)
		{
			char c = s.charAt(start);
			if (punctuation.indexOf(c) < 0)
				break;
		}

		// start is our starting index for non-punct
		// if no non-punct, start is s.length()
		for (; end >= start; end--)
		{
			char c = s.charAt(end);
			if (punctuation.indexOf(c) < 0)
				break;
		}

		// end is our ending point for non-punct
		// everything between start and end (inclusive) is non-punct
		if (start > end)
			return "";

		return s.substring(start, end + 1);
	}

	// strips brackets from (...) and [...] if there are any
	public static String stripBrackets(String s)
	{
		if (s.startsWith("[") && s.endsWith("]"))
			return s.substring(1, s.length() - 1);
		if (s.startsWith("(") && s.endsWith(")"))
			return s.substring(1, s.length() - 1);
		return s;
	}

	// strips double quotes from start and end. e.g. "Barack Obama" (with quotes) -> Barack Obama (without quotes)
	public static String stripDoubleQuotes(String s)
	{
		if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
			return s.substring(1, s.length() - 1);
		else
			return s;
	}

	/**
	 * returns a string with elements of the given collection concatenated,
	 * separated by given separator
	 */
	public static <E> String join(Collection<E> c, String separator)
	{
		if (c.size() == 0)
			return "";
		int n = c.size(), count = 0;
		StringBuilder result = new StringBuilder();
		for (E e : c)
		{
			result.append(e);
			count++;
			if (count < n) // no separator at the end
				result.append(separator);
		}
		return result.toString();
	}

	/**
	 * returns a string with elements of the given array concatenated, separated
	 * by given separator
	 */
	public static <E> String join(E[] c, String separator)
	{
		if (c == null)
			return null;
		if (c.length == 0)
			return "";
		int n = c.length, count = 0;
		StringBuilder result = new StringBuilder();
		for (E e : c)
		{
			result.append(e);
			count++;
			if (count < n) // no separator at the end
				result.append(separator);
		}
		return result.toString();
	}

	/**
	 * returns a string with elements of the given collection sorted and
	 * concatenated, separated by given separator
	 */
	public static <E extends Comparable<? super E>> String joinSort(Collection<E> c, String separator)
	{
		if (c == null)
			return null;
		if (c.size() == 0)
			return "";
		int n = c.size(), count = 0;
		StringBuilder result = new StringBuilder();
		List<E> tmp = new ArrayList<>(c);
		Collections.sort(tmp);
		for (E e : tmp)
		{
			result.append(e);
			count++;
			if (count < n) // no separator at the end
				result.append(separator);
		}
		return result.toString();
	}

	// convert an email folder name to something sane for a file system
	public static String sanitizeFolderName(String s)
	{
		if (s == null)
			return null;

		// clean up special chars in the folder name
		s = s.replace(":", "__");
		s = s.replace("/", "__");
		s = s.replace("\\", "__");
		s = s.replace(" ", "__");
		s = s.replace("[", "");
		s = s.replace("]", "");
		return s;
	}

	// Replacing any of the disallowed filename characters (\/:*?"<>|&) to _
	// (note: & causes problems with URLs for serveAttachment etc, so it's also
	// replaced)
	public static String sanitizeFileName(String filename) {
		if (filename == null)
			return null;
		if (filename.contains("/"))
		{
			filename = filename.replace("/", "_");
		}
		if (filename.contains(":"))
		{
			filename = filename.replace(":", "_");
		}
		if (filename.contains("*"))
		{
			filename = filename.replace("*", "_");
		}
		if (filename.contains("?"))
		{
			filename = filename.replace("?", "_");
		}
		if (filename.contains("\""))
		{
			filename = filename.replace("\"", "_");
		}
		if (filename.contains("<"))
		{
			filename = filename.replace("<", "_");
		}
		if (filename.contains(">"))
		{
			filename = filename.replace(">", "_");
		}
		if (filename.contains("|"))
		{
			filename = filename.replace("|", "_");
		}
		if (filename.contains("\\"))
		{
			filename = filename.replace("\\", "_");
		}
		if (filename.contains("&")) // ampersands cause problems with URLs for
									// serveAttachment etc, so just convert them
									// too
		{
			filename = filename.replace("&", "_");
		}
		return filename;
	}

	// parses a string and returns as byte array
	public static byte[] parseIPAddress(String str)
	{
		try {
			StringTokenizer st = new StringTokenizer(str, ".");
			List<Byte> list = new ArrayList<>();
			boolean invalidAddr = false;
			while (st.hasMoreTokens())
			{
				int x = Integer.parseInt(st.nextToken());
				if ((x & 0xffffff00) != 0)
				{
					invalidAddr = true;
					break;
				}
				list.add((byte) x);
			}

			if (list.size() != 4)
				invalidAddr = true;

			if (invalidAddr)
			{
				System.err.println("String is not a valid IPv4 address " + str);
				return null;
			}

			byte[] result = new byte[list.size()];

			int i = 0;
			for (byte b : list)
				result[i++] = b;
			return result;
		} catch (Exception e) {
			System.err.println("Error parsing " + str + " : " + e);
			return null;
		}
	}

	// Deletes all files and subdirectories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns
	// false.
	public static boolean deleteDir(File f,Log log)
	{
		if (f.isDirectory())
		{
			String[] children = f.list();
			for (String aChildren : children) {
				boolean success = deleteDir(new File(f, aChildren), log);
				if (!success) {
					System.err.println("warning: failed to delete file " + f);
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		return f.delete();
	}

	public static void deleteDir(String path, Log log)
	{
		if (path == null)
			return;
		File f = new File(path);
		if (f.exists())
		{
		boolean success = deleteDir(f,log);
			warnIf(!success, "Unable to delete file: " + f.getPath(), log);
		}
		else
			warnIf(true, "Sorry, can't delete path because it doesn't even exist: " + path,log);
	}

    static String cleanEmailStuff(String content){
        content = content.replaceAll("(Email:|To:|From:|Date:|Subject: Re:|Subject:)\\W+","");
        return content;
    }

    public static Set<String> getAcronyms(String content) {
        if (content == null)
            return null;
        Pattern acronymPattern = Pattern.compile("[A-Z]{3,}");

        content = cleanEmailStuff(content);

        Set<String> acrs = new HashSet<>();
        Matcher m = acronymPattern.matcher(content);
        while (m.find()) {
            String acr = m.group();
            String tt = FeatureGeneratorUtil.tokenFeature(acr);
            if (!tt.equals("ac")) {
                continue;
            }
            acrs.add(acr);
        }
        return acrs;
    }

    public static class MyFilenameFilter implements FilenameFilter {
		private String	prefix, suffix; // suffix is optional

		public MyFilenameFilter(String prefix) {
			this.prefix = prefix;
		}

		public MyFilenameFilter(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}

		public boolean accept(File dir, String name)
		{
			// String path = (dir.getAbsolutePath() + File.separator + name);
			if (prefix != null && !name.startsWith(prefix))
				return false;
			return !(suffix != null && !name.endsWith(suffix));
		}
	}

	/** will parse Vila Dinar\u00E9s, Pau to map the \u00E9 to the right unicode char. Useful when parsing FAST Index */
	public static String convertSlashUToUnicode (String s) {
		if (s == null)
			return s;
		if (!s.contains("\\u"))
			return s;

		List<Character> out = new ArrayList<>();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '\\' && (i + 5 < s.length()) && s.charAt(i + 1) == 'u') {
				String seq = Character.toString(s.charAt(i + 2))
						+ Character.toString(s.charAt(i + 3))
						+ Character.toString(s.charAt(i + 4))
						+ Character.toString(s.charAt(i + 5));
				ch = (char) Integer.parseInt(seq, 16);
				i += 5;
			}

			out.add(ch);
		}

		StringBuilder sb = new StringBuilder();
		for (char c : out)
			sb.append(c);
		return sb.toString();
	}

	public static Set<String> filesWithPrefixAndSuffix(String dir, String prefix, String suffix)
	{
		Set<String> result = new LinkedHashSet<>();

		if (dir == null)
			return result;
		if (!new File(dir).exists())
			return result; // empty result

		File files[] = new File(dir).listFiles(new MyFilenameFilter(prefix, suffix));
		if (files != null)
			for (File f : files)
			{
				String name = f.getName();
				if (prefix != null)
					name = name.substring(prefix.length());
				if (suffix != null)
					name = name.substring(0, name.length() - suffix.length());
				result.add(name);
			}
		return result;
	}

	public static Set<String> filesWithSuffix(String dir, String suffix)
	{
		return filesWithPrefixAndSuffix(dir, null, suffix);
	}

	/** cleans up files in directory with the given suffix */
	public static void deleteAllFilesWithSuffix(String dir, String suffix, Log log) {
		if (dir == null)
			return;
		File cache = new File(dir);
		if (!cache.exists())
			return; // empty result

		File files[] = new File(dir).listFiles(new Util.MyFilenameFilter(null, suffix));
		if (files != null)
			for (File f : files)
			{
				boolean success = f.delete();
				if (log != null)
				{
					if (success)
						log.info("Deleted file: " + f.getName());
					else
						log.warn("Failed to delete file: " + f.getName());
				}
			}
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////
	// public static void sortPairsBySecondElementInt(List<Pair<?,Integer>>
	// input)
	// {
	// Collections.sort (input, new Comparator<Pair<?,Integer>>() {
	// public int compare (Pair<?,Integer> p1, Pair<?,Integer> p2) {
	// int i1 = p1.getSecond();
	// int i2 = p2.getSecond();
	// return i2 - i1;
	// }
	// });
	// }

	// public static void sortPairsBySecondElementFloat(List<Pair<?,Float>>
	// input)
	// {
	// Collections.sort (input, new Comparator<Pair<?,Float>>() {
	// public int compare (Pair<?,Float> p1, Pair<?,Float> p2) {
	// // int i1 = p1.getSecond();
	// // int i2 = p2.getSecond();
	// // return i2 - i1;
	// return p2.getSecond().compareTo(p1.getSecond());
	// }
	// });
	// }

	/** sorts in decreasing order of second element of pair */
	public static <S, T extends Comparable<? super T>> void sortPairsBySecondElement(List<Pair<S, T>> input)
	{
		input.sort((p1, p2) -> {
			T i1 = p1.getSecond();
			T i2 = p2.getSecond();
			return i2.compareTo(i1);
		});
	}

	/** sorts in decreasing order of second element of pair */
	public static <S, T extends Comparable<? super T>> void sortPairsBySecondElementIncreasing(List<Pair<S, T>> input)
	{
		input.sort((p1, p2) -> {
			T i1 = p1.getSecond();
			T i2 = p2.getSecond();
			return i1.compareTo(i2);
		});
	}

	public static <T extends Comparable<? super T>, S> void sortPairsByFirstElement(List<Pair<T, S>> input)
	{
		input.sort((p1, p2) -> {
			return p1.getFirst().compareTo(p2.getFirst());
			// int i1 = p1.getFirst();
			// int i2 = p2.getFirst();
			// return i2 - i1;
		});
	}

	public static void main1(String args[])
	{
		System.out.println(edu.stanford.muse.ie.Util.getAcronym("UC Santa Barbara"));
		test_tail();
		Map<Integer, Integer> map = new LinkedHashMap<>();
		map.put(10, 1);
		map.put(20, 1);
		map.put(15, 1);
		List<Pair<Integer, Integer>> list = mapToListOfPairs(map);
		sortPairsBySecondElementIncreasing(list);
		Util.sortPairsByFirstElement(list);
		for (Pair<Integer, Integer> p : list)
			System.out.println(p);
	}

	public static void sortTriplesByThirdElement(List<Triple<?, ?, Integer>> input)
	{
		input.sort((t1, t2) -> {
			int i1 = t1.getThird();
			int i2 = t2.getThird();
			return i2 - i1;
		});
	}

	public static <T> List<T> permuteList(List<T> in, int seed)
	{
		// create a copy of the input
		List<T> result = new ArrayList<>();
		result.addAll(in);

		Random R = new Random(seed);
		for (int permuteSize = in.size(); permuteSize > 1; permuteSize--)
		{
			int pos = Math.abs(R.nextInt() % permuteSize); // findbugs points
															// out that Math.abs
															// (R.nextInt()) %
															// permuteSize is
															// not correct as it
															// can return a -ve
															// number if the
															// nextInt is
															// MIN_INTEGER
			// pos is in teh range 0..permuteSize-1
			// interchange elements permuteSize-1 and pos
			T tmp = result.get(permuteSize - 1);
			result.set(permuteSize - 1, result.get(pos));
			result.set(pos, tmp);
		}
		return result;
	}

	/**
	 * permutes the letters of a string. Note: it is possible for the same
	 * string to be returned
	 */
	public static String permuteString(String s, Random r)
	{
		if (s == null || s.length() < 2)
			return s;

		List<Character> list = new ArrayList<>();
		for (char c : s.toCharArray())
			list.add(c);
		list = Util.permuteList(list, r.nextInt());
		StringBuilder sb = new StringBuilder();
		for (char c : list)
			sb.append(c);
		return sb.toString();
	}

	/**
	 * takes in a map K,V and returns a List of Pairs <K,V> sorted by
	 * (descending) value
	 */
	public static <K, V> List<Pair<K, V>> mapToListOfPairs(Map<K, V> map)
	{
		List<Pair<K, V>> result = new ArrayList<>();
		for (Map.Entry<K, V> e : map.entrySet())
			result.add(new Pair<>(e.getKey(), e.getValue()));
		return result;
	}

	/**
	 * takes in a map K,V and returns a List of Pairs <K,V> sorted by
	 * (descending) value
	 */
	public static <K extends Comparable<? super K>, V> List<Pair<K, V>> sortMapByKey(Map<K, V> map)
	{
		List<Pair<K, V>> result = mapToListOfPairs(map);
		Util.sortPairsByFirstElement(result);
		return result;
	}

	/**
	 * takes in a map K,V and returns a List of Pairs <K,V> sorted by
	 * (descending) value
	 */
	public static <K, V extends Comparable<? super V>> List<Pair<K, V>> sortMapByValue(Map<K, V> map)
	{
		List<Pair<K, V>> result = new ArrayList<>();
		for (Map.Entry<K, V> e : map.entrySet())
			result.add(new Pair<>(e.getKey(), e.getValue()));
		Util.sortPairsBySecondElement(result);
		return result;
	}

	/**
	 * takes in a map K,V and returns a sorted LinkedHashMap, sorted by
	 * (descending) value
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> reorderMapByValue(Map<K, V> map)
	{
		List<Pair<K, V>> resultPairs = new ArrayList<>();
		for (Map.Entry<K, V> e : map.entrySet())
			resultPairs.add(new Pair<>(e.getKey(), e.getValue()));
		Util.sortPairsBySecondElement(resultPairs);
		Map<K, V> result = new LinkedHashMap<>();
		for (Pair<K, V> p : resultPairs)
			result.put(p.getFirst(), p.getSecond());
		return result;
	}

	/**
	 * takes in a map K,List<V> and returns a new Map of Pairs <K,List<V>>
	 * sorted by (descending) size of the lists.
	 * by sorting, we just mean that a linkedhashmap is returned which can be
	 * iterated over in sorted order.
	 */
	public static <K, V> Map<K, Collection<V>> sortMapByListSize(Map<K, Collection<V>> map)
	{
		List<Pair<K, Integer>> counts = new ArrayList<>();
		for (Map.Entry<K, Collection<V>> e : map.entrySet())
			counts.add(new Pair<>(e.getKey(), e.getValue().size()));
		Util.sortPairsBySecondElement(counts);
		Map<K, Collection<V>> result = new LinkedHashMap<>();
		for (Pair<K, Integer> p : counts)
		{
			K k = p.getFirst();
			result.put(k, map.get(k));
		}
		return result;
	}

	/**
	 * takes in a map K,List<V> and adds value to key's list - effectively a
	 * multi-map.
	 */
	public static <K, V> void addTo(Map<K, Collection<V>> map, K key, V value)
	{
		Collection<V> values = map.computeIfAbsent(key, k -> new ArrayList<>());
		values.add(value);
	}

	/** parses a day string in the format yyyymmdd */
	private static Calendar parseDate(String s)
	{
		int d, m, y;

		int x = 0;
		try {
			x = Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			System.err.println("Invalid date: " + s);
			return new GregorianCalendar();
		}

		if (x <= 9999) // only yyyy is given
			x = x * 10000 + 1 * 100 + 1; // adjust to yyyy-01-01
		else if (x <= 999999) // only yyyy-mm is given
			x = x * 100 + 1; // adjust to yyyy-mm-01

		y = x / 10000;
		m = (x % 10000) / 100;
		d = (x % 100);

		Calendar c = new GregorianCalendar(y, m - 1, d); // note month needs
															// adjustment
															// because GC is
															// zero based
		return c;
	}

	/*
	 * parses a date string in format "start-end" and returns the start and end
	 * daes
	 * e.g. 2004-20060723 is equiv to 2004-01-01 to 2006-07-23
	 * string on each side of '-' can be yyyy or yyyymm or yyyymmdd
	 * if no '-' is given, end date is assumed to be now
	 * error checking not very robust
	 */
	public static Pair<Calendar, Calendar> parseDateInterval(String calendarString)
	{
		Calendar endDate = null;
		String startDateString, endDateString = null;

		if (!calendarString.contains("-"))
		{
			endDate = new GregorianCalendar(); // current time, default
			startDateString = calendarString;
		}
		else
		{
			StringTokenizer st = new StringTokenizer(calendarString, "-");
			startDateString = st.nextToken();
			endDateString = st.nextToken();
		}

		Calendar startDate = Util.parseDate(startDateString);
		if (endDateString != null)
			endDate = parseDate(endDateString);

		return new Pair<>(startDate, endDate);
	}

	/**
	 * parses keyword strings a la google search in the given input string and
	 * returns the results.
	 * always returns lowercase
	 * currently just tokenizes the input, in future could be aware of " ... "
	 * operators for multi-word terms.
	 */
	public static List<String> parseKeywords(String keywords)
	{
		List<String> result = new ArrayList<>();
		if (keywords == null)
			return result;
		StringTokenizer st = new StringTokenizer(keywords);
		while (st.hasMoreTokens())
			result.add(st.nextToken().toLowerCase());
		return result;
	}

	public static String getMemoryStats()
	{
		Runtime r = Runtime.getRuntime();
		System.gc();
		int MB = 1024 * 1024;
		return r.freeMemory() / MB + " MB free, " + (r.totalMemory() / MB - r.freeMemory() / MB) + " MB used, " + r.maxMemory() / MB + " MB max, " + r.totalMemory() / MB + " MB total";
	}

	public static int getMinFreq(int nDocs, float pct)
	{
		int minCount = (int) ((nDocs * pct) / 100);
		if (minCount < 2)
			minCount = 2;
		if (minCount > 5)
			minCount = 5;
		return minCount;
	}

	/**
	 * converts an object to a string->string map by converting all its fields
	 * (fields may be non-public
	 * if running without security manager). expand=true expands collections
	 * (array, list, map)
	 */
	public static Map<String, String> convertObjectToMap(Object o, boolean expand)
	{
		Map<String, String> map = new LinkedHashMap<>();
		if (o == null)
			return map;

		Class c = o.getClass();

		try {
			// generate a string to string map of the fields
			Field f[] = c.getDeclaredFields();
			for (Field aF : f) {
				boolean acc = aF.isAccessible();
				if (!acc)
					aF.setAccessible(true); // ok to do in absence of a security manager

				Class t = aF.getType();
				String name = aF.getName();
				if (name.indexOf("$") >= 0) // outer class, skip" +
					continue;
				if (t == double.class)
					map.put(name, Double.toString(aF.getDouble(o)));
				else if (t == float.class)
					map.put(name, Float.toString(aF.getFloat(o)));
				else if (t == int.class)
					map.put(name, Integer.toString(aF.getInt(o)));
				else if (t == long.class)
					map.put(name, Long.toString(aF.getLong(o)));
				else if (t == char.class)
					map.put(name, aF.getChar(o) + "(" + Integer.toString(aF.getChar(o)) + ")");
				else if (t == short.class)
					map.put(name, Short.toString(aF.getShort(o)));
				else if (t == byte.class)
					map.put(name, Byte.toString(aF.getByte(o)));
				else if (t == boolean.class)
					map.put(name, Boolean.toString(aF.getBoolean(o)));
				else {
					// field is of object type
					Object val = aF.get(o); // o.f[i]'s type is t, value is
					// val
					if (val == null)
						map.put(name, "null");
					else {
						Class valClass = val.getClass();
						if (valClass.isArray()) {
							if (expand)
								for (int x = 0; x < Array.getLength(val); x++)
									map.put(name + "[" + x + "]", Array.get(val, x) + "");
						} else if (Map.class.isAssignableFrom(valClass)) // could
						// also
						// check
						// t,
						// but
						// val.getClass
						// is
						// more
						// specific
						{
							Map m = (Map) aF.get(o);
							if (expand)
								for (Object x : m.keySet())
									map.put(name + "." + x, m.get(x) + "");
						}
						// could also check t, but val.getClass is more specific
						else if (Collection.class.isAssignableFrom(valClass)) {
							Collection c1 = (Collection) aF.get(o);
							if (expand) {
								int count = 0;
								for (Object o1 : c1)
									map.put(name + "(" + count++ + ")", o1 + ""); // use
								// ()
								// instead
								// of
								// []
								// to
								// distinguish
								// from
								// arrays
							}
						} else
							map.put(name, "[" + val.toString() + "]");
					}
				}
				if (!acc)
					aF.setAccessible(false);
			}

		} catch (Throwable e) {
			Util.print_exception(e);
		}
		return map;
	}

	/**
	 * converts an object to a string representation by printing all its fields
	 * (fields may be non-public
	 * if running without security manager). expand=true expands collections
	 */
	public static String fieldsToString(Object o, boolean expand)
	{
		if (o == null)
			return "null";

		Map<String, String> map = convertObjectToMap(o, expand);

		StringBuilder result = new StringBuilder();

		// start with the class name
		Class c = o.getClass();
		result.append(stripPackageFromClassName(c.getName()) + ": ");

		// append all the fields
		for (String field : map.keySet()) {
			Object val = map.get(field);
			String valString = (val != null) ? val.toString() : "null";
			if (val instanceof Integer)
				valString = Util.commatize((Integer) val);
			if (val instanceof Long)
				valString = Util.commatize((Integer) val);
			result.append(field + "=" + valString + " ");
		}

		return result.toString();
	}

	/**
	 * converts an object to a CSV format. returns 2 strings:
	 * first string: fieldname1, fieldname2, fieldname3,...
	 * second string: fieldvalue1, fieldvalue2, fieldvalue3,...
	 * also has a trailing comma
	 */
	public static Pair<String, String> fieldsToCSV(Object o, boolean expand)
	{
		if (o == null)
			return new Pair<>("", "");
		Map<String, String> map = convertObjectToMap(o, expand);
		StringBuilder keys = new StringBuilder(), values = new StringBuilder();
		for (String field : map.keySet())
		{
			keys.append(field + ",");
			String value = map.get(field);
			value = value.replaceAll(",", "").replaceAll("\\n", "").replaceAll("\\r", ""); // get
																							// rid
																							// of
																							// commas
			values.append(value + ",");
		}
		return new Pair<>(keys.toString(), values.toString());
	}

	/**
	 * converts an object's fields to HTML TD format. returns 2 strings: <td>
	 * fieldname1</td><td>fieldname2</td>... <td>fieldvalue1</td><td>fieldvalue2</td>...
	 */
	public static Pair<String, String> fieldsToHTMLTD(Object o, boolean expand)
	{
		if (o == null)
			return new Pair<>("", "");
		Map<String, String> map = convertObjectToMap(o, expand);
		StringBuilder keys = new StringBuilder(), values = new StringBuilder();
		for (String field : map.keySet())
		{
			keys.append("<td>" + Util.escapeHTML(field) + "</td>");
			String value = map.get(field);
			values.append("<td>" + Util.escapeHTML(value) + "</td>");
		}
		return new Pair<>(keys.toString(), values.toString());
	}

	// converts fq class names to simple names
	// e.g. a.b.c.d to d
	public static String stripPackageFromClassName(String class_name)
	{
		// System.out.toString ("input is " + s);

		int z = class_name.lastIndexOf('.');
		if (z >= 0)
			class_name = class_name.substring(z + 1);
		else
		{
			z = class_name.lastIndexOf('/');
			if (z >= 0)
				class_name = class_name.substring(z + 1);
		}

		return class_name;
	}

	public static String fieldsToString(Object o)
	{
		return fieldsToString(o, false);
	}

	public static long getUnprocessedMessage(int done, int total, long elapsedMillis)
	{
		// compute unprocessed message
		// String unprocessedMessage = "--:-- remaining";
		long unprocessedTimeSeconds = -1;
		if (done > 0) // if 0, no way of estimating time remaining
		{
			// long unprocessedTimeMillis = (nTotalMessagesInAllFolders -
			// processedCount) * elapsedTimeMillis/processedCount;
			int undone = total - done;
			// this is a best guess at uncached count. we don't know how many
			// are cached in folders we haven't even looked at yet
			// we assume they are all uncached by subtracting from total
			// messages only the provably cached messages so far.
			long unprocessedTimeMillis = -1;
			if (done > 0)
				unprocessedTimeMillis = (undone * elapsedMillis) / done;

			unprocessedTimeSeconds = unprocessedTimeMillis / 1000;

			/*
			 * long hours = unprocessedTimeSeconds / 3600;
			 * long x = unprocessedTimeSeconds % 3600;
			 * long mins = x / 60;
			 * long secs = x % 60;
			 * if (hours > 0)
			 * formatter.format("%dh:", hours);
			 * 
			 * formatter.format( "%02dm:", mins);
			 * if (hours == 0 && mins == 0 && secs == 0)
			 * secs = 1; // its embarassing to show 00:00s and then make people
			 * wait (which happens sometimes), so always show at least 00:01 sec
			 * remaining
			 * formatter.format( "%02ds", secs);
			 */
		}

		return unprocessedTimeSeconds;
	}

	/* given 2 arrays of strings, returns their union */
	public static String[] unionOfStringArrays(String x[], String y[])
	{
		Set<String> set = new LinkedHashSet<>();
        Collections.addAll(set, x);
		Collections.addAll(set, y);
		String[] arr = new String[set.size()];
		set.toArray(arr);
		return arr;
	}

	/**
	 * given a string representing a path to a file, returns the url string for
	 * it.
	 * we only substitute # (we know it causes trouble -- remember CHI session
	 * called EPIC #FAIL ? :-)
	 * and '?' currently. Note. do not use URLEncoder.encode because that does
	 * other bad things like
	 * replace each space with +
	 */
	public static String URLEncodeFilePath(String s)
	{
		String s1 = s.replace("#", "%23");
		s1 = s1.replace("?", "%3F");
		return s1;
	}

	/**
	 * given any string, returns the url string for it which should be XSS-safe.
	 */
	public static String URLEncode(String s)
	{
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String tail(String s, String separator)
	{
		if (s == null)
			return null;

		// strip out the trailing separator(s) if any
		while (s.endsWith(separator)) {
			s = s.substring(0, s.length() - separator.length());
		}
		int idx = s.lastIndexOf(separator);
		if (idx >= 0)
			return s.substring(idx + 1);
		else
			return s;
	}

	public static void test_tail()
	{
		Util.ASSERT(tail(null, "|") == null);
		Util.ASSERT(tail("///", "/").equals(""));
		Util.ASSERT(tail("/ab/cd/ef", "/").equals("ef"));
		Util.ASSERT(tail("/ab/cd/ef/", "/").equals("ef"));
		Util.ASSERT(tail("\\ab\\cd\\ef\\", "\\").equals("ef"));
	}

	/**
	 * returns the component of the url after the last / i.e. the name of the
	 * actual file in the URL.
	 * returns null if the input is null.
	 */
	public static String URLtail(String url)
	{
		return tail(url, "/");
	}

	/**
	 * returns the component of the url after the last / OR \.
	 * Used for mbox names, where the file name could have been generated on a
	 * different system from the current platform separator.
	 */
	public static String filePathTail(String filePath)
	{
		String t = tail(filePath, "/");
		t = tail(t, "\\");
		return t;
	}

	/**
	 * returns the component of the url after the last platform separator
	 */
	public static String filePathTailByPlatformSeparator(String filePath)
	{
		String t = tail(filePath, "/");
		t = tail(t, "\\");
		return t;
	}

	/**
	 * if s begins with prefix, strips prefix and returns s. otherwise returns
	 * original s
	 */
	public static String stripFrom(String s, String prefix)
	{
		if (s.startsWith(prefix))
			return s.substring(prefix.length());
		else
			return s;
	}

	/**
	 * if s begins with prefix, strips prefix and returns s. otherwise returns
	 * original s
	 */
	public static String userIdFromEmail(String email)
	{
		int idx = email.indexOf("@");
		if (idx < 0)
			return email;
		else
			return email.substring(0, idx);
	}

	// remove given chars from beginning or end of given string
	public static String removeCharsFromBeginOrEnd(String s, String tabooAtBeginOrEnd)
	{
		// strip from the end
		while (s.length() > 0 && tabooAtBeginOrEnd.indexOf(s.charAt(s.length() - 1)) >= 0)
			s = s.substring(0, s.length() - 1);

		// strip from the beginning
		if (s.length() >= 1)
			while (tabooAtBeginOrEnd.indexOf(s.charAt(0)) >= 0)
			{
				if (s.length() == 0)
					break;
				s = s.substring(1);
			}
		return s;
	}

	public static List<String> stripCommonPrefix(List<String> list)
	{
		if (list.size() == 0)
			return list;

		String commonPrefix = list.get(0);
		for (String s : list)
		{
			int matchLen = 0;
			for (; matchLen < commonPrefix.length(); matchLen++)
			{
				if (s.charAt(matchLen) != commonPrefix.charAt(matchLen))
					break;
			}

			commonPrefix = commonPrefix.substring(0, matchLen);
		}

		commonPrefix = commonPrefix.substring(0, commonPrefix.lastIndexOf(File.separatorChar) + 1);
		if (commonPrefix.length() <= 1)
			return list;

		List<String> result = new ArrayList<>();
		for (String s : list)
			result.add(s.substring(commonPrefix.length()));

		return result;
	}

	/** remove everything in str before the last = */
	public static String strippedEmailAddress(String str)
	{
		int idx = str.lastIndexOf('=');
		if (idx < 0)
			return str;

		str = str.substring(idx);
		if (str.length() == 1)
			return str; // funny... str ends with '='. let's not mess with it.

		return str.substring(1);
	}

	// removes dups from the input list
	public static <T> List<T> removeDups(List<T> in)
	{
		Set<T> set = new LinkedHashSet<>();
		set.addAll(in);
		if (set.size() == in.size())
			return in;

		List<T> result = new ArrayList<>();
		result.addAll(set);
		return result;
	}

	/** util method */
	private static String bytesToHexString(byte[] bytes)
	{
		// http://stackoverflow.com/questions/332079
		// http://stackoverflow.com/questions/7166129
		StringBuilder sb = new StringBuilder();
		for (byte aByte : bytes) {
			String hex = Integer.toHexString(0xFF & aByte);
			if (hex.length() == 1)
				sb.append('0');
			sb.append(hex);
		}
		return sb.toString();
	}

	/** SHA-256 hash */
	public static String hash(String s)
	{
		MessageDigest digest = null;
		String hash = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.update(s.getBytes(StandardCharsets.UTF_8));
			hash = bytesToHexString(digest.digest());
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return hash;
	}

	public static String hash(String s, Map<String, String> map)
	{
		MessageDigest digest = null;
		String hash = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			digest.update(s.getBytes());
			hash = bytesToHexString(digest.digest());
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (map != null)
			map.put(hash, s);
		return hash;
	}

	public static void writeObjectToFile(String filename, Serializable s) throws IOException
	{
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
		oos.writeObject(s);
		oos.close();
	}

	public static Serializable readObjectFromFile(String filename) throws IOException, ClassNotFoundException
	{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename));
		Serializable s = (Serializable) ois.readObject();
		ois.close();
		return s;
	}

	public static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException e) {
				// Do your thing with the exception. Print it, log it or mail
				// it.
				e.printStackTrace();
			}
		}
	}

	public static <E> boolean hasRedundantElements(Collection<E> c)
	{
		Map<E, E> m = new LinkedHashMap<>();
		for (E e : c) {
			if (m.containsKey(e)) {
				E e1 = m.get(e);
				// assert(e == e1); // that would just fail
				assert (e.equals(e1));
				return true;
			}
			m.put(e, e);
		}
		Set<E> s = new LinkedHashSet<>(c);
		assert (s.size() <= c.size());
		return s.size() != c.size();
	}

	public static File createTempDirectory() throws IOException
	{
		final File temp = File.createTempFile("muse_", "_contents");

		if (!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if (!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	public static String maskEmailDomain(String s)
	{
		return s.replaceAll("\\b([A-Za-z0-9][A-Za-z0-9\\-_\\.]*)@[A-Za-z0-9][A-Za-z\\-0-9_]*(\\.[A-Za-z0-9][A-Za-z\\-0-9_]*)*\\.[A-Za-z]{2,4}\\b", "$1@...");
	}

	public static <E extends Comparable<? super E>> int compareToNullSafe(E a, E b)
	{
		if (a == b)
			return 0;
		if (a == null)
			return -1;
		if (b == null)
			return 1;
		return a.compareTo(b);
	}

	public static <E> boolean equalsNullSafe(E a, E b)
	{
		if (a == null)
			return b == null;
		else
			return a.equals(b);
	}

	/**
	 * Return list1 - list2. require that the elements must be sortable.
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static <E extends Comparable<? super E>> List<E> getRemoveAll(List<E> list1, Collection<E> list2)
	{
		Set<E> set1 = new LinkedHashSet<>(list1);
		set1.removeAll(list2);
		return new ArrayList<>(set1);
	}

	/** Return int[] from String[] */
	public static int[] toIntArray(String[] arr)
	{
		int[] result = null;
		if (arr != null) {
			result = new int[arr.length];
			for (int i = 0; i < arr.length; i++)
				result[i] = Integer.parseInt(arr[i]);
		}
		return result;
	}

	public static int maxTokenLength(String s)
	{
		if (s == null)
			return 0;

		int max = 0;
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens())
		{
			int tokenLength = st.nextToken().length();
			if (tokenLength > max)
				max = tokenLength;
		}
		return max;
	}

	public static int nLetterChars(String s)
	{
		int count = 0;
		for (char c : s.toCharArray())
			if (Character.isLetter(c))
				count++;
		return count;
	}

	private static Pattern spacePattern = Pattern.compile ("[\\s\\xA0]+");
	/** replaces sequences of space chars with one space */
	public static String canonicalizeSpaces(String s)
	{
		// includes replacement for 0xA0 (nbsp), which is not handled by \s
		// alone
		// http://stackoverflow.com/questions/1702601/unidentified-whitespace-character-in-java
		if (s == null)
			return s;
		return spacePattern.matcher(s).replaceAll(" ");
	}

	/**
	 * Returns the input cast as Set (modifiable) if it is indeed one,
	 * or clone it as a Set. Returns null if the input is null.
	 */
	public static <E> Set<E> castOrCloneAsSet(Collection<E> c)
	{
		return (c == null || c instanceof HashSet) ? (Set<E>) c : new LinkedHashSet<>(c);
	}

	/**
	 * Returns an intersection as Set
	 */
	public static <E> Set<E> setIntersection(Collection<E> set1, Collection<E> set2)
	{
		// see
		// http://stackoverflow.com/questions/7574311/efficiently-compute-intersection-of-two-sets-in-java
		boolean set1IsLarger = set1.size() > set2.size();
		Set<E> cloneSet = new HashSet<>(set1IsLarger ? set2 : set1);
		cloneSet.retainAll(set1IsLarger ? set1 : set2);
		return cloneSet;
		// if (s1 == null || s2 == null) return null; // let's trigger exception
		// as caller may want null to represent "all"
		// return Sets.intersection(castOrCloneAsSet(s1), castOrCloneAsSet(s2));
	}

    //retains the indices in the first list
    //null is treated as all
    public static <E> List<E> listIntersection(Collection<E> list1, Collection<E> list2)
    {
        if(list1 == null && list2 == null) return null;
        if(list1 == null)
            return new ArrayList<>(list2);
        if(list2 == null)
            return new ArrayList<>(list1);
        List<E> cloneList = new ArrayList<>(list1);
        cloneList.retainAll(list2);
        return cloneList;
    }

	/**
	 * Returns a union as Set
	 */
	public static <E> Set<E> setUnion(Collection<E> s1, Collection<E> s2)
	{
		// if (s1 == null || s2 == null) return null; // let's trigger exception
		// as caller may want null to represent "all"
		Set<E> result = new LinkedHashSet<>(s1);
		result.addAll(s2);
		return result;
		// return Sets.union(castOrCloneAsSet(s1), castOrCloneAsSet(s2));
	}

    public static <E> List<E> listUnion(Collection<E> s1, Collection<E> s2){
        if(s1 == null && s2 == null)
            return null;
        if(s1 == null)
            return new ArrayList<>(s2);
        if(s2 == null)
            return new ArrayList<>(s1);

        List<E> result = new ArrayList<>(s1);
        result.addAll(s2);
        return result;
    }

	/**
	 * Returns an intersection as Set, treating null as universal. Returns null
	 * if both inputs are null.
	 */
	public static <E> Set<E> setIntersectionNullIsUniversal(Collection<E> s1, Collection<E> s2)
	{
		if (s1 == null)
			return castOrCloneAsSet(s2);
		if (s2 == null)
			return castOrCloneAsSet(s1);
		return setIntersection(s1, s2);
	}

	/**
	 * Returns a union as Set, treating null as empty. Returns null if both
	 * inputs are null.
	 */
	public static <E> Set<E> setUnionNullIsEmpty(Collection<E> s1, Collection<E> s2)
	{
		if (s1 == null)
			return castOrCloneAsSet(s2);
		if (s2 == null)
			return castOrCloneAsSet(s1);
		return setUnion(s1, s2);
	}

    public static <E> List<E> listUnionNullIsEmpty(Collection<E> s1, Collection<E> s2)
    {
        if (s1 == null)
            return new ArrayList<>(s2);
        if (s2 == null)
            return new ArrayList<>(s1);
        return listUnion(s1, s2);
    }

    /**Cleans names by
     * removing any return chars, replaces consecutive spaces with single space, removes HTML tags,
     * removes junk chars like curly brackets and quotes.
     * Puts these in a ste and returns*/
	public static Set<String> scrubNames(Collection<String> list)
	{
		Set<String> set = new LinkedHashSet<>();
		for (String s : list) {
			s = s.replaceAll("[\\r\\n\\a]+", " ") // newlines
					.replaceAll("\\s+", " ") // whitespaces compaction
					.replaceAll("</?[A-Za-z]+[^>]*>", "") // HTML tags
					.replaceAll("\\}", "") // we see such garbage sometimes
					.replaceAll("\\{", "")
					.replaceAll("\"", "");
			s = s.trim(); // sometimes whitespace is left at the end... not sure
							// why
			set.add(s);
		}
		return set;
	}

	// both arguments have to agree on being or not being URL escaped (probably
	// have to be escaped since we assume "&" is the delimiter)
	public static int indexOfUrlParam(String allParams, String param)
	{
		allParams += "&"; // sentinel
		param += "&"; // to prevent prefix matching (e.g., param = "foo=12"
						// should not match allParams = "foo=123")
		return allParams.toLowerCase().indexOf(param.toLowerCase());
	}

	public static String excludeUrlParam(String allParams, String param)
	{
		int startIdx = indexOfUrlParam(allParams, param);
		if (startIdx < 0)
		{
			// JSPHelper.log.warn
			// ("unexpected! facet already selected but not in params: " +
			// allParams);
			return allParams;
		}

		int endIdx = startIdx + param.length();
		if (startIdx > 0 && allParams.charAt(startIdx - 1) == '&')
			startIdx--; // exclude preceding & also if present
		if (endIdx < allParams.length() && startIdx == 0 && allParams.charAt(endIdx) == '&') // should
																								// not
																								// need
																								// to
																								// check
																								// ==
																								// '&'
			endIdx++; // exclude following & if that becomes the first param
						// (should be harmless to leave it there anyway)

		// splice out [startIdx, endIdx) ; notice the exclusive upper end
		String newParams = allParams.substring(0, startIdx);
		newParams += allParams.substring(endIdx);
		return newParams;
	}

	public static boolean isWindowsPlatform()
	{
		return (System.getProperty("os.name").toLowerCase().contains("windows"));
	}

	public static String devNullPath()
	{
		return isWindowsPlatform() ? "NUL" : "/dev/null";
	}

	/** replaced w in sentence with _ */
	public static String blankout(String sentence, String w)
	{
		if (w.length() == 1)
			return sentence; // an EVR special. his first name on FB is just a
								// single letter; in that case don't bother to
								// blank it out
		String lowerCaseSentence = sentence.toLowerCase();
		w = w.toLowerCase();

		String blanks = w.replaceAll(".", "_"); // . (regexp) matches any char,
												// so blanks is a string of _ of
												// the same length as w.
												// findbugs falsely reports an
												// error on this line.
		lowerCaseSentence = lowerCaseSentence.replaceAll(w, blanks);
		char[] clueArray = new char[lowerCaseSentence.length()];

		// insert those blanks into the original sentence
		// we need to retain capitalization
		for (int i = 0; i < lowerCaseSentence.length(); i++)
			clueArray[i] = (lowerCaseSentence.charAt(i) == '_') ? '_' : sentence.charAt(i); // original
																							// sentence

		String c = new String(clueArray);
		return c;
	}

	/**
	 * check if part occurs only as a complete word in full. complete word =>
	 * char before and after the answer is not a letter.
	 * full, part should already be space canonicalized in case part can have
	 * spaces
	 * note that part could occur multiple times -- this method returns true if
	 * EVERY occurrence of answer is a word.
	 * e.g. for params ("americans in america", "america"), this method returns
	 * false
	 */
	public static boolean occursOnlyAsWholeWord(String full, String part)
	{
		full = full.toLowerCase();
		part = part.toLowerCase();
		// part might be a partial match, e.g. india matches against "indians"
		// in the full. disallow the match
		// if the char just before or just after the match is a letter. (it
		// should be space or some delim)

		int idx = full.indexOf(part); // part is already space normalized...
		if (idx < 0)
			return false;

		while (idx >= 0)
		{
			// idx is the position that has matched
			// see if the place before the match is a char
			if (idx > 0)
				if (Character.isLetter(full.charAt(idx - 1)))
					return false;

			// see if the place after the match is a char
			int end_idx = idx + part.length() - 1;
			if (end_idx + 1 < full.length())
				if (Character.isLetter(full.charAt(end_idx + 1)))
					return false;

			// ok, so the match at idx succeeded.
			// look for more matches in this string
			// end_idx+1 is the delim for the prev. occurrence of part, so now
			// look for the answer again starting at end_idx+2
			if (end_idx + 2 < full.length())
				full = full.substring(end_idx + 2);
			else
				break; // we've reached the end
			idx = full.indexOf(part); // part is already lower case and space
										// normalized...
		}
		return true;
	}

	/**
	 * removes stringsToRemove from input (case-insensitive) and returns the new
	 * list
	 */
	public static List<String> removeStrings(List<String> input, Set<String> stringsToRemove)
	{
		List<String> result = new ArrayList<>();

		for (String s : input)
		{
			boolean match = false;
			String x = s.toLowerCase();
			for (String str_to_remove : stringsToRemove)
				if (str_to_remove.toLowerCase().equals(x))
				{
					match = true;
					break;
				}

			if (!match)
				result.add(s);
		}

		return result;
	}

	// cleans and escapes all special characters in java regex, to make it
	// Pattern friendly.
	public static String cleanForRegex(String str) {
		if (str == null)
			return str;
		String cleaned = null;
		// remove trailing and leading non-word chars
		cleaned = str.replaceAll("^\\W*", "");
		cleaned = cleaned.replaceAll("\\W*$", "");

		cleaned = cleaned.replaceAll("\\(", "\\\\(");
		cleaned = cleaned.replaceAll("\\)", "\\\\)");
		cleaned = cleaned.replaceAll("\\?", "\\\\?");

		if (cleaned == null)
			return str;
		return cleaned;
	}

	/**
	 * Cleans names, especially those extracted from contacts.
	 * returns null if the name doesn't look clean
	 */
	public static String cleanName(String name) {
		final List<String> stopWords = Arrays.asList(
				"a", "an", "and", "are", "as", "at", "be", "but", "by",
				"for", "if", "in", "into", "is", "it",
				"no", /*
					 * "not"
					 * ,
					 */"of", "on", "or", "such",
				"that", "the", "their", "then", "there", "these",
				"they", "this", "to", "was", "will", "with"
				);

		if (name == null)
			return null;
		name = name.replaceAll("^\\W+|\\W+$", "");
		//trailing apostrophe
		//this could be a good signal for name(occasionally could also be org). The training data (Address book) doesn't contain such pattern, hence probably have to hard code it and I dont want to.
		name = name.replaceAll("'s$", "");
		//stuff b4 colon like subject:, from: ...
		name = name.replaceAll("\\w+:\\W+", "");
		//remove stuff in the beginning
		name = name.replaceAll("([Dd]ear|[hH]i|[hH]ello)\\W+", "");
		name = name.replaceAll("^\\W+|\\W+$", "");

		boolean clean = true;
		String[] words = name.split("\\s+");
		for (String word : words)
			if (stopWords.contains(word.toLowerCase())) {
				clean = false;
				break;
			}
		if (clean)
			if (name.contains("-"))
				clean = false;
		if (clean)
			return name;
		else
			return null;
	}

    public static int getIntParam(String txt, int num){
        try{
            return Integer.parseInt(txt);
        } catch(Exception e){
            return num;
        }
    }

	public static void testTokenizeAlphaChars() {
		String[] tests = new String[]{"12abc xyz", "abc", "abc xyz12", "Dr. Prof. Doolit"};
		for (String s : tests)
		{
			System.out.println ("--\n" + s);
			List<String> result = Util.tokenizeAlphaChars(s);
			for (String r: result)
				System.out.println (r);
		}
	}

	public static void test() {
		testEllipsizeKeepingExtension();
		testGetExtension();
		System.out.println("Tests passed ok");
		testTokenizeAlphaChars();
	}

	public static void main(String[] args){
		test();
    }
}
