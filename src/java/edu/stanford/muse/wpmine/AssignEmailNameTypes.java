package edu.stanford.muse.wpmine;

import com.google.gson.Gson;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;

import java.io.*;
import java.util.*;

public class AssignEmailNameTypes {

	private static PrintStream out = System.out;
    private static final PrintStream err = System.err;
	
	private static final Map<String, Info> hitTitles = new LinkedHashMap<>();
	private static final String typesFile = "instance_types_en.nt1";
	static String abstractsFile = "short_abstracts_en.nt";
	private static final String pageLengthsFile = "page-lengths";
	private static String title = "NONE";
	
	static class Info implements Comparable<Info>, Serializable { 
		final String title;
		String snippet;
		String type = "notype"; int score, times;
		Info(String t) { this.title = t; }
		public int compareTo(Info other)
		{
			return other.score - this.score;
		}
		
		public String toString()
		{
			return score/1000 + "k " + title + " (" + type + ") # " + snippet;
		}
		
		String toString(boolean b)
		{
			return score/1000 + "k " + title + " (" + Util.pluralize(times,  "time") + ") # " + snippet;
		}
	}
	
	private static void readTypes() {
		int count = 0;
		try {
			LineNumberReader lnr = new LineNumberReader(new FileReader(typesFile));
			while (true)
			{
				String line = lnr.readLine();
				if (line == null)
					break;
				count++;
				StringTokenizer st = new StringTokenizer(line);
				String r = st.nextToken().toLowerCase().trim();
				Info I = hitTitles.get(r);
				if (I == null)
					continue;
				I.type = st.nextToken();
			}
			lnr.close();
		} catch (Exception e) {
			err.println ("Unable to read types file, err at line "  + count);
			e.printStackTrace(err);
		}
	}
	
	public static void main (String args[]) throws IOException
	{
		Set<String> posArgs = new LinkedHashSet<>();
		Set<String> negArgs = new LinkedHashSet<>();
		args = new String[]{"names"};
		title = args[0];		
		out = new PrintStream(new FileOutputStream(title + ".out"));
		out.println (posArgs.size() + " positive term(s), " + negArgs.size() + " negative term(s)");
		
		LineNumberReader lnr = new LineNumberReader(new FileReader("/tmp/names"));
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			StringTokenizer st = new StringTokenizer(line, "|");
			String title = st.nextToken().trim().replaceAll(" ", "_");
			int times = 0;
			try { times = Integer.parseInt(st.nextToken().trim()); } catch (NumberFormatException e)  {Util.print_exception(e, JSPHelper.log); }
			Info I = new Info(title);
			I.times = times;
			I.snippet = "";
			hitTitles.put(title, I);
		}
		lnr.close();
		out.println (hitTitles.size() + " names to look up");

		readTypes();
		
		lnr = new LineNumberReader(new FileReader(pageLengthsFile));
		int count = 0;
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			if (count++ % 100000 == 0)
				out.println (count + " lines read");
			StringTokenizer st = new StringTokenizer(line);
			int len = Integer.parseInt(st.nextToken());
			String title = "";
			while (st.hasMoreTokens())
				title += st.nextToken() + " ";
			title = title.trim().toLowerCase().replaceAll(" ", "_");
			Info I = hitTitles.get(title);
			if (I != null)
				I.score = len;
		}
		
		List<Info> list = new ArrayList<>(hitTitles.values());
		Collections.sort(list);
		
		Map<String, Collection<Info>> typedHits = new LinkedHashMap<>();
		for (Info I : list)
		{
			out.println (I);
			String type = I.type;
			Collection<Info> list1 = (typedHits.computeIfAbsent(type, k -> new ArrayList<>()));
			list1.add(I);
		}
		
		out.println ("-------------\n" + typedHits.size() + " categories of typed hits \n--------------");

		typedHits = Util.sortMapByListSize(typedHits);

		for (String s: typedHits.keySet())
		{
			List<Info> list1 = (List) typedHits.get(s);
			Collections.sort(list1);
			out.println ("----- " + s + " (" + list1.size() + " hit(s))");
			for (Info I: list1)
				out.println (I.toString(false));
		}
		out.close();
		
		String s = new Gson().toJson(list);
		PrintStream ps = new PrintStream(new FileOutputStream(title + ".data.js"));
		ps.println("var pages = " + s);
		ps.close();
		
		lnr.close();
	}
}
