package edu.stanford.muse.ie;

import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.index.*;
import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.*;

public class NameTypes {
	private static Log			log					= LogFactory.getLog(NameTypes.class);
	private final static long	serialVersionUID	= 1L;

	static String				typesFile			= "instance_types_en.nt1.gz";

	/*
	 * hitTitles key is canonicalized answer, the corr. NameInfo's title field
	 * is the uncanonicalized version.
	 */
	public static void readTypes(Map<String, NameInfo> hitTitles) throws IOException
	{
		// types.gz is of the form First_Last Subtype|Type
		try {
			Map<String, String> dbpedia = EmailUtils.readDBpedia();
			//LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(NameTypes.class.getClassLoader().getResourceAsStream(typesFile)), "UTF-8"));
			Set<String> seenTitles = new HashSet<>();
			Set<String> ambiguousTitles = new HashSet<>();
			for (String title : dbpedia.keySet()) {
				String r = title.toLowerCase();
				String cr = r.replaceAll(" \\(.*?\\)", "");
				if (!hitTitles.containsKey(cr))
					continue;

				if (seenTitles.contains(cr))
					ambiguousTitles.add(cr);
				else
					seenTitles.add(cr);
			}

			//lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(NameTypes.class.getClassLoader().getResourceAsStream(typesFile)), "UTF-8"));
			for (String title : dbpedia.keySet())
			{
				if (title == null)
					break;
				String r = title.toLowerCase();
				NameInfo I = hitTitles.get(r);
				if (I == null || ambiguousTitles.contains(r))
					continue;
				String type = dbpedia.get(title);

				if (!r.contains(" ") && type.endsWith("Person")) // if it's a person, it should have at least 2 tokens. Note: _ is the word separator for r
					continue;
				I.type = type;
				//System.err.println("Assigning type: " + type + " to: " + title);
			}
			//lnr.close();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
	}

	/** returns ctitle -> nameinfo */
	public static Map<String, NameInfo> computeNameMap(Archive archive, Collection<EmailDocument> allDocs) throws IOException
	{
		if (allDocs == null)
			allDocs = (List) archive.getAllDocs();

		// compute name -> nameInfo
		Map<String, NameInfo> hitTitles = new LinkedHashMap<>();

		int i = 0;
		List<String> upnames = new ArrayList<>(), unernames = new ArrayList<>();
        Tokenizer tokenizer = new CICTokenizer();
        for (EmailDocument ed : allDocs) {
			if (i % 1000 == 0)
				log.info("Collected names from :" + i + "/" + allDocs.size());
			i++;
			String id = ed.getUniqueId();
			//String content = archive.getContents(ed, false);
			//Set<String> pnames = tokenize.tokenizeWithoutOffsets(content, true);
			//Note that archive.getAllNames does not fetch the corr. names, but NER names.
            List<String> pnames = ed.getAllNames();
            List<String> names = new ArrayList<>();

			//temp to remove duplication.
			Set<String> unames = new HashSet<>();
			unames.addAll(pnames);
			names.addAll(unames);
			//totalNames += names.size();

			for (String name : names)
			{
				if (name == null || !name.contains(" "))
					continue;
				String cTitle = name.trim().toLowerCase(); // canonical title
				// these are noisy "names"
				if ("best_wishes".equals(cTitle) || "best_regards".equals(cTitle) || "uncensored".equals(cTitle))
					continue;

				NameInfo I = hitTitles.get(cTitle);
				if (I == null)
				{
					I = new NameInfo(name);
					I.times = 1;
					I.snippet = "";
					hitTitles.put(cTitle, I);
				}
				else
					I.times++;
			}
		}
		Set<String> unp = new HashSet<>(), unner = new HashSet<>();
		unp.addAll(upnames);
		for (String u : unernames)
			unner.add(u);

		return hitTitles;
	}

	/**
	 * looks up all names in the given docs in the names archive and assigns
	 * types to them
	 */
	public static Map<String, Collection<NameInfo>> assignTypes(Map<String, NameInfo> nameMap) throws IOException
	{
		// assign types to all the names
		readTypes(nameMap);

		// sort by size of the type
		List<NameInfo> list = new ArrayList<>(nameMap.values());
		Collections.sort(list);

		Map<String, Collection<NameInfo>> typedHits = new LinkedHashMap<>();
		for (NameInfo I : list)
		{
			String type = I.type;
			List<NameInfo> list1 = (List) typedHits.get(type);
			if (list1 == null)
			{
				list1 = new ArrayList<>();
				typedHits.put(type, list1);
			}
			list1.add(I);
		}

		// sort list by importance of term
		for (Collection<NameInfo> l : typedHits.values())
			Collections.sort((List) l);

		log.info("-------------\n" + typedHits.size() + " categories of typed hits identified \n--------------");

		typedHits = Util.sortMapByListSize(typedHits);

		return typedHits;
	}

	public static void computeInfo(Map<String, NameInfo> nameMap, Collection<EmailDocument> allDocs, Archive archive, Lexicon lex) throws IOException
	{
		// assign types to all the names
		if (allDocs == null)
			allDocs = (List) archive.getAllDocs();
		// compute name -> nameInfo
		Map<String, Collection<Document>> sentimentToDocs = archive.getSentimentMap(lex, true /** original content only*/);
		for (EmailDocument ed : allDocs)
		{
			String id = ed.getUniqueId();
			List<String> names = archive.getNamesForDocId(id, Indexer.QueryType.FULL);
			List<Address> mentionedAddresses = ed.getToCCBCC();
			Set<String> sentimentsForDoc = new LinkedHashSet<>();
			for (String sentiment : sentimentToDocs.keySet()) {
				if (sentimentToDocs.get(sentiment).contains(ed))
					sentimentsForDoc.add(sentiment);
			}

			for (String name : names)
			{
				String cTitle = name.trim().toLowerCase().replaceAll(" ", "_"); // canonical title			
				NameInfo I = nameMap.get(cTitle);
				if (I == null)
				{
					log.info("Warning: null info for name: " + name);
					continue;
				}

				//Map sentiment to its prominence in document.
				if (I.sentimentCatToCount == null)
					I.sentimentCatToCount = new LinkedHashMap<>();
				for (String sentiment : sentimentsForDoc) {
					if (!I.sentimentCatToCount.containsKey(sentiment)) //if the sentiment isn't there.
						I.sentimentCatToCount.put(sentiment, 1);
					else {
						int sum = I.sentimentCatToCount.get(sentiment);
						sum = sum + 1;
						I.sentimentCatToCount.put(sentiment, sum);
					}
				}
				I.sentimentCatToCount = Util.reorderMapByValue(I.sentimentCatToCount);
				//while (I.sentimentCatToCount.containsKey(null)){ //clean sentimentCatToCount
				//I.sentimentCatToCount.remove(null);
				//System.out.println("Cleaned sentimentCatToCount.");
				//}
				//obtain list of contacts to whom email is being sent.
				for (Address adr : mentionedAddresses) {
					InternetAddress emailadr = (InternetAddress) adr;
					String address_string = emailadr.getAddress();
					Contact associatedcontact = archive.addressBook.lookupByEmail(address_string);
					if (I.peopleToCount == null)
						I.peopleToCount = new LinkedHashMap<>();

					if (!I.peopleToCount.containsKey(associatedcontact)) //if the contact is not yet associated.
						I.peopleToCount.put(associatedcontact, 1);
					else {
						int sum = I.peopleToCount.get(associatedcontact);
						sum = sum + 1;
						I.peopleToCount.put(associatedcontact, sum);
					}
				}
				if (I.peopleToCount != null)
					I.peopleToCount = Util.reorderMapByValue(I.peopleToCount);
				//while (I.peopleToCount.containsKey(null)){ //clean peopleToCount
				//I.peopleToCount.remove(null);
				//System.out.println ("Cleaned peopleToCount.");
				//}
				//determine start and end dates of the term.
				Date documentDate = ed.getDate();
				if (I.firstDate == null)
					I.firstDate = documentDate;
				if (I.lastDate == null)
					I.lastDate = documentDate;

				if (I.firstDate.after(documentDate))
					I.firstDate = documentDate;
				if (I.lastDate.before(documentDate))
					I.lastDate = documentDate;
				//System.out.println("Name " + name + " FirstDate: " + (I.firstDate.toString()) + " LastDate:" + (I.lastDate.toString()));
			}
		}
		// compute map of sentiment -> docs for each sentiment in goodSentiments
		//for every document, get canonical name of a person who is associated with it.
		// for each docs in archive, get the list of names
		// for each name, update the first/last date, get the list of docs, and count how many of them are in the intersection with each sentiment		
	}

	public static void main(String args[])
	{
		TypeHierarchy th = new TypeHierarchy();

		th.recordTypeCount("A|B|C", 3);
		th.recordTypeCount("D|B|C", 3);
		th.recordTypeCount("X|C", 3);
		th.recordTypeCount("M|N", 5);
		System.out.println(th.toString(true));
	}
}
