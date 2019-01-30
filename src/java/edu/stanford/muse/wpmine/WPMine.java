package edu.stanford.muse.wpmine;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.gson.Gson;
import edu.stanford.muse.util.Util;

class WPMine {

	private static PrintStream out = System.out;
    private static final PrintStream err = System.err;
	
	private static final Map<String, Info> hitTitles = new LinkedHashMap<>();
	private static final String typesFile = "instance_types_en.nt1";
	private static final String abstractsFile = "short_abstracts_en.nt";
	private static final String pageLengthsFile = "page-lengths";
	private static String title = "NONE";
	
	static class Info implements Comparable<Info>, Serializable { 
		final String title;
		String snippet;
		String type = "notype";
		String url; int score;
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
			return (score == 0 ? "- " : (score/1000) + "k ") + title + " # " + snippet;
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
		title = args[0];
		
		for (String arg: args)
			if (arg.startsWith("-"))
				negArgs.add(arg.toLowerCase().substring(1));
			else
				posArgs.add(arg.toLowerCase());
		
		out = new PrintStream(new FileOutputStream(title + ".out"));
		out.println (posArgs.size() + " positive term(s), " + negArgs.size() + " negative term(s)");
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(abstractsFile));
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			
			int firstMatchIdx = Integer.MAX_VALUE;
			String lineLower = line.toLowerCase();
			for (String arg: posArgs)
			{
				int idx = lineLower.indexOf(arg);
				if (idx >= 0 && firstMatchIdx > idx)
					firstMatchIdx = idx;
			}
			
			if (firstMatchIdx == Integer.MAX_VALUE)
				continue;
			
			int negFirstMatchIdx = Integer.MAX_VALUE;

			for (String arg: negArgs)
			{
				int idx = lineLower.indexOf(arg);
				if (idx >= 0 && negFirstMatchIdx > idx)
					negFirstMatchIdx = idx;
			}
			
			if (negFirstMatchIdx < firstMatchIdx)
				continue; // dropping because neg matches before pos
			
			String title = line.replaceAll(" .*", "");
			String url = title.substring(1).substring(0, title.length()-2);
			title = title.replaceAll("<http://dbpedia.org/resource/", "").replaceAll(">", ""); // .replaceAll("_", " ");
			title = title.toLowerCase();
			if (title.contains("_of_"))
				continue;
			if (title.contains("_in_"))
				continue;
			if (title.contains("_at_"))
				continue;
			if (title.contains("_the_"))
				continue;
			Info I = new Info(title);
			I.url = url;
			I.snippet = Util.ellipsize(line.replaceAll("<.*>", ""), 200);
			hitTitles.put(title, I);
		}
		lnr.close();
		out.println (hitTitles.size() + " pages hit");

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
