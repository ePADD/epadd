package edu.stanford.muse.wpmine;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;


public class WPGenTypes {
	
	static Set<String> uniqueOs = new LinkedHashSet<>();
	static PrintStream out = System.out;
	
	public static void main (String[] args) throws IOException
	{
		LineNumberReader lnr = new LineNumberReader(new FileReader(args[0]));
		String currentS = "", currentO = "";
		int count = 0;
		while (true)
		{
			String line = lnr.readLine();
			if (line == null)
				break;
			if (!line.contains("http://dbpedia.org/ontology/")) // only consider dbped ontology
				continue;
			
			StringTokenizer st = new StringTokenizer(line);
			String s = st.nextToken();
			String p = st.nextToken();			
			String o = st.nextToken();
			s = s.replaceAll("<http://dbpedia.org/resource/", "");
			s = s.replaceAll(">$", "");
			o = o.replaceAll("<http://dbpedia.org/ontology/", "");
			o = o.replaceAll(">$", "");
			
			if (s.equals(currentS)) 
			{
				if (currentO.length() > 0)
					currentO += "|" + o;
				else
					currentO = o;
			}
			else
			{
				out.println (currentS + " " + currentO);
				uniqueOs.add(currentO);

				currentS = s;
				currentO = o;
			}
			
			count++;
		}
		
		if (currentO.length() > 0)
			out.println (currentS + " " + currentO);

		lnr.close();
		
		out.println ("\n-------------------------\n");
		out.println (count + " subjects, " + uniqueOs.size() + " unique types");
		for (String s: uniqueOs)
			out.println(s);
	}
}
