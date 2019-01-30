package edu.stanford.muse.ie;

import java.io.*;
import java.util.*;

import edu.stanford.muse.Config;

public class KillPhrases {
	private static final Set<String> killPhrases = new LinkedHashSet<>();
	static {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(Config.getResourceAsStream(Config.TABOO_FILE)));
			String line = null;
			int lineNum = 0;
			while ((line = br.readLine()) != null) {
				String s = line.trim().toLowerCase();
				s = edu.stanford.muse.util.Util.canonicalizeSpaces (s);
				killPhrases.add(line.trim().toLowerCase());
				lineNum++;
			}
			System.err.println("Read #" + lineNum + " from config file: " + Config.TABOO_FILE);
			br.close();
		} catch (Exception e) {
			System.err.println("Exception while reading taboo list from config file: " + Config.TABOO_FILE);
			e.printStackTrace();
		}
	}

	public static boolean isKillPhrase (String s) {
		if (s == null)
			return true;

		String s1 = s.trim().toLowerCase();
		s1 = edu.stanford.muse.util.Util.canonicalizeSpaces (s1);
		return killPhrases.contains (s1);
	}
}
