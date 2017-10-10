/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package edu.stanford.muse.lens;

import edu.stanford.muse.email.AddressBook;
import edu.stanford.muse.email.Contact;
import edu.stanford.muse.ie.NameInfo;
import edu.stanford.muse.index.*;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;

public class Lens {
    public static Log log = LogFactory.getLog(JSPHelper.class);

	private static Set<String> knownBadTerms = new LinkedHashSet<String>(); /** imp: canonicalized to lower case */

	static {
		String LENS_KILL_FILE = "lens-kill.txt";
		URL url = Lens.class.getClassLoader().getResource(LENS_KILL_FILE);
		InputStream in = null;
		try {
			if (url != null) 
			{
				in = Lens.class.getClassLoader().getResourceAsStream(LENS_KILL_FILE);
				List<String> lines = Util.getLinesFromInputStream(in, true /* skip comment lines */);
				for (String s: lines)
					knownBadTerms.add(s.toLowerCase()); // canonicalize to lower case
			}
			log.info(knownBadTerms.size() + " kill terms read from resource: " + url);
		} catch (IOException e) {
			log.warn ("Warning: unable to read lens kill file: " + LENS_KILL_FILE);
			Util.print_exception(e, log);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					log.warn ("Should not reach here");
					Util.print_exception(e, log);
				}
		}
	}

	private static List<JSONObject> scoreHits(List<JSONObject> list, LensPrefs lensPrefs) throws JSONException
	{
		double maxPageScore = -1.0f; 
		double totalPageScore = 0;
		double maxTermScore = -1.0f;
		double totalIndexScore = 0.0;

		for (JSONObject o: list)
		{
			double times = o.getDouble("pageScore"); // times on page is really normalized page score
			totalPageScore += times;

			if (times > maxPageScore)
				maxPageScore = times;

			double termScore = o.getDouble ("indexScore");
			totalIndexScore += termScore;

			if (termScore > maxTermScore)
				maxTermScore = termScore;
		}

		for (JSONObject o: list)
		{		
			String term = o.getString("text");
			int nMessages = o.getInt("nMessages");
			double score;

			// right now, we are going to ignore url and just use a global boost score
			float userBoost = (lensPrefs != null) ? lensPrefs.getBoost ("GLOBAL", term) : 1.0f; 
			// #boost
			if (userBoost == 0.0f || knownBadTerms.contains(term.toLowerCase()))
			{
				// equivalent to no hits
				nMessages = 0; 
				o.put("nMessages", 0);
				o.put("indexScore", 0); // make sure to set this also, it is used by lens frontend for highlighting also
			}  	

			if (nMessages == 0)
				score = 0;
			else
			{
				double pageScore = o.getDouble("pageScore");
				double indexScore = o.getDouble("indexScore");
				double present = pageScore + indexScore;
				double total = totalPageScore + totalIndexScore;
				double expected = present/total;
				double observed1 = pageScore/totalPageScore;
				double observed2 = indexScore/totalIndexScore;

				// various scoring functions are possible:
				// double f1 = (maxTermScore == 0) ? 0 : o.getDouble("indexScore")/maxTermScore;
				// double g1 = (maxPageScore == 0) ? 0 : o.getDouble("pageScore")/(1.0*maxTimesOnPage);
				//				score = Math.max ((1-f1)*g1, (1-g1)*f1);
				//				score = Math.abs(observed1-observed2);
				//				score = ((observed1-expected)*(observed1-expected) + (observed2-expected)*(observed2-expected))/expected; ///chi-squared... doesn't work so well with small numbers... low cell counts
				//				score = ((observed1-expected)*(observed1-expected) + (observed2-expected)*(observed2-expected)); // seems to work best of the lot so f

				score = pageScore * indexScore;// ((pageScore/totalPageScore) * (indexScore/totalIndexScore));
				score *= userBoost;
			}
			o.put ("score", score);
		}
		// sort the results by score
		Collections.sort (list, new Comparator<JSONObject>() { 
			public int compare (JSONObject o1, JSONObject o2)
			{
				try {
					double s1 = o1.getDouble("score");
					double s2 = o2.getDouble("score");
					//@vihari: BUG_FIX should return 0 when they are equal, else transitive prop doesn't hold and throws Illegalarguementexception
					if(s1 == s2) return 0;
					return (s2 > s1) ? 1 : -1;
				} catch (Exception e) { Util.print_exception(e); return -1; }
			}
		});
		
		if (log.isDebugEnabled())
			for (JSONObject o: list)
				log.debug("term: " + o.getString("text") + " score = " + o.getDouble("score") + " pageScore = " + o.getDouble("pageScore") + " indexScore = " + o.getDouble("indexScore"));
		return list;
	}

	/** looks up given names in address book + message content index and returns a json of scores. lensPrefs has the user's term preferences */
	public static List<JSONObject> getHitsQuick(List<Pair<String, Float>> names, LensPrefs lensPrefs, Archive archive, String baseURL, Collection<EmailDocument> allDocs) throws JSONException, IOException, GeneralSecurityException
	{
		List<JSONObject> list = new ArrayList<JSONObject>();
		
		Indexer indexer = archive.indexer;
		AddressBook ab = archive.addressBook;
		
		if (indexer == null)
			return list;
		
		for (Pair<String, Float> pair: names)
		{
			String term = pair.getFirst();
			if (term.length() <= 2)
				continue;
			
			float pageScore = pair.getSecond();
			term = JSPHelper.convertRequestParamToUTF8(term);
			//Prune all the non-alphabetical characters
			term = term.replaceAll("[\\r\\n]","");
			term = term.replaceAll("[^\\p{L}\\p{Nd}\\s\\.]", "");
			term = term.replaceAll("\\s+"," ");

			JSONObject json = new JSONObject();
			json.put ("text", term);
			json.put ("pageScore", pageScore);
			
			NameInfo ni = archive.nameLookup(term);
			if (ni != null && ni.type != null && !"notype".equals(ni.type))
				json.put ("type", ni.type);

			int NAME_IN_ADDRESS_BOOK_WEIGHT = 100;
			// look up term in 2 places -- AB and in the index
			int hitsInAddressBook = 0; // temporarily disabled AB - sgh. IndexUtils.selectDocsByPersons(ab, allDocs, new String[]{term}).size();
			int hitsInMessageContent = archive.countHitsForQuery("\"" + term + "\""); // To check: does this include subject line also...
			// weigh any docs for name in addressbook hugely more!
			double termScore = hitsInAddressBook * NAME_IN_ADDRESS_BOOK_WEIGHT + hitsInMessageContent;
			json.put ("indexScore", termScore);
			int totalHits = hitsInAddressBook + hitsInMessageContent;
			json.put ("nMessages", totalHits); // this is an over-estimate since the same message might match both in addressbook and in body. it is used only for scoring and should NEVER be shown to the user. getTermHitDetails will get the accurate count
			log.info(term  + ": " +  hitsInAddressBook + " in address book, " + hitsInMessageContent + " in messages"); 
			
			String url = baseURL + "/browse?adv-search=1&termBody=on&termSubject=on&termAttachments=on&termOriginalBody=on&term=\"" + term + "\"";
			json.put ("url", url);
		//	JSONArray messages = new JSONArray();
		//	json.put("messages", messages); // empty messages
			list.add (json);				
		}

		log.info (list.size() + " terms hit");
		list = scoreHits(list, lensPrefs);
		return list;
	}
	
	/** gets details from index for the given term */
	public static JSONObject detailsForTerm(String term, float pageScore, Archive archive, AddressBook ab, String baseURL, Collection<EmailDocument> allDocs) throws JSONException, IOException, GeneralSecurityException
	{
		if (term.length() <= 2)
			return null;

		term = JSPHelper.convertRequestParamToUTF8(term);

		JSONObject json = new JSONObject();
		json.put ("text", term);
		json.put ("pageScore", pageScore);

		int NAME_IN_ADDRESS_BOOK_WEIGHT = 100;
		// look up term in 2 places -- AB and in the index
		List<EmailDocument> docsForNameInAddressBook = (List) IndexUtils.selectDocsByPersonsAsList(ab, allDocs, new String[]{term});
		List<EmailDocument> docsForTerm = (List) new ArrayList<Document>(archive.docsForQuery("\"" + term + "\"", -1, Indexer.QueryType.FULL));
		// weigh any docs for name in addressbook hugely more!
		double termScore = docsForNameInAddressBook.size() * NAME_IN_ADDRESS_BOOK_WEIGHT + docsForTerm.size();
		json.put ("indexScore", termScore);

		Set<EmailDocument> finalDocSet = new LinkedHashSet<EmailDocument>();
		finalDocSet.addAll(docsForNameInAddressBook);
		finalDocSet.addAll(docsForTerm);
		List<EmailDocument> finalDocList = new ArrayList<EmailDocument>(finalDocSet);
		json.put ("nMessages", finalDocList.size());

		// score people
		Map<Contact, Float> peopleScores = new LinkedHashMap<>();
		for (EmailDocument ed: finalDocSet)
		{
			Collection<String> addrs = ed.getParticipatingAddrsExcept(ab.getOwnAddrs());
			for (String s: addrs)
			{
				if ("user".equals(s))
					continue;

				float weight = 1.0f/addrs.size(); // weight = 1/size
				Contact c = ab.lookupByEmail(s);
				Float F = peopleScores.get(c);
				if (F == null)
					peopleScores.put (c, weight);
				else
					peopleScores.put (c, F+weight);
			}
		}

		// add the top people
		int MAX_PEOPLE = 5;
		List<Pair<Contact, Float>> pairs = Util.sortMapByValue(peopleScores);
		JSONArray people = new JSONArray();
		Contact own = ab.getContactForSelf();
		int count = 0;
		for (Pair<Contact, Float> p: pairs)
		{
			if (count > MAX_PEOPLE)
				break;
			Contact c = p.getFirst();//ab.lookupByEmail(email);
			if (c == own)
				continue; // ignore own name
			JSONObject person = new JSONObject();
			String displayName = c==null?"":c.pickBestName();

			person.put ("person", displayName);
			person.put ("score", p.getSecond());
			people.put(count, person);
			count++;
		}
		json.put("people", people);

		if (finalDocList.size() > 0 && log.isDebugEnabled())
			log.debug ("Term: " + term + " content hits: " + docsForTerm.size() + " header hits: " + docsForNameInAddressBook.size() + " total: " + finalDocList.size());

		String url = baseURL + "/browse?term=\"" + term + "\"";
		json.put ("url", url);
		JSONArray messages = new JSONArray();

		// put up to 5 teasers in the json response
		int N_TEASERS = 5;
		for (int i = 0; i < finalDocList.size() && i < N_TEASERS; i++)
		{
			JSONObject message = finalDocList.get(i).toJSON(0);
			messages.put(i, message);
		}
		json.put("messages", messages);	
		return json;
	}
	
	/** looks up given names in the index and returns a json of scores. lensPrefs has the user's term preferences */
	public static List<JSONObject> getHits(List<Pair<String, Float>> names, LensPrefs lensPrefs, Archive archive, AddressBook ab, String baseURL, Collection<EmailDocument> allDocs) throws JSONException, IOException, GeneralSecurityException
	{
		List<JSONObject> list = new ArrayList<JSONObject>();
		
		if (archive == null)
			return list;
		for (Pair<String, Float> pair: names)
		{
			JSONObject json = detailsForTerm(pair.getFirst(), pair.getSecond(), archive, ab, baseURL, allDocs);
			if (json != null)
				list.add (json);				
		}

		log.info (list.size() + " terms hit");
		list = scoreHits(list, lensPrefs);
		return list;
	}
}
