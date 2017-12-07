package edu.stanford.muse.ie;

import edu.stanford.muse.email.AddressBookManager.Contact;
import edu.stanford.muse.index.*;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.zip.GZIPInputStream;

class NameTypesMemory {
    private static Log log = LogFactory.getLog(NameTypes.class);
	private final static long serialVersionUID = 1L;
	private static String typesFile = "instance_types_en.nt1.gz";
	
	private static void readTypes(Map<String, NameInfoMemory> hitTitles) throws IOException
	{
		// types.gz is of the form First_Last Subtype|Type
		try {
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(NameTypes.class.getClassLoader().getResourceAsStream(typesFile)), "UTF-8"));
			while (true)
			{
				String line = lnr.readLine();
				if (line == null)
					break;
				StringTokenizer st = new StringTokenizer(line);
				String r = st.nextToken().toLowerCase().trim();
				NameInfoMemory I = hitTitles.get(r);
				if (I == null)
					continue;
				String type = st.nextToken();
				
				// type specific stuff
//				if ("GivenName|Name".equals(type)) // this is a useless type
//					continue; 
				
				String badSuffix = "|Agent";
				if (type.endsWith(badSuffix) && type.length() > badSuffix.length())
					type = type.substring(0, type.length()-badSuffix.length());
				if (!r.contains("_") && type.endsWith("Person")) // if it's a person, it should have at least 2 tokens. Note: _ is the word separator for r
						continue;
				I.type = type;
			}
			lnr.close();
		} catch (Exception e) {
			Util.print_exception(e, log);
		}
	}
	
	public static Map<String, NameInfoMemory> computeNameMap (Archive archive, Collection<EmailDocument> allDocs) throws IOException
	{
		final Indexer.QueryType qt = Indexer.QueryType.ORIGINAL;
		if (allDocs == null)
			allDocs = (List) archive.getAllDocs();

		// compute name -> NameInfoMemory
		Map<String, NameInfoMemory> hitTitles = new LinkedHashMap<String, NameInfoMemory>();
		for (EmailDocument ed: allDocs)
		{
			String id = ed.getUniqueId();
			List<String> names = archive.getNamesForDocId(id, qt);
			for (String name: names)
			{
				String cTitle = name.trim().toLowerCase().replaceAll(" ", "_"); // canonical title
				// these are noisy "names"
				if ("best_wishes".equals(cTitle) || "best_regards".equals(cTitle) || "uncensored".equals(cTitle))
					continue;
				
				NameInfoMemory I = hitTitles.get(cTitle);
				if (I == null)
				{
					I = new NameInfoMemory(name);
					I.times = 1;
					I.snippet = "";
					hitTitles.put(cTitle, I);
				}
				else
					I.times++;
			}
		}
		return hitTitles;
	}
	
	/** looks up all names in the given docs in the names archive and assigns types to them */
	public static Map<String, Collection<NameInfoMemory>> assignTypes (Map<String, NameInfoMemory> nameMap) throws IOException
	{
		// assign types to all the names
		readTypes(nameMap);
				
		// sort by size of the type
		List<NameInfoMemory> list = new ArrayList<NameInfoMemory>(nameMap.values());
		Collections.sort(list);

		Map<String, Collection<NameInfoMemory>> typedHits = new LinkedHashMap<String, Collection<NameInfoMemory>>();
		for (NameInfoMemory I : list)
		{
			String type = I.type;
			List<NameInfoMemory> list1 = (List) typedHits.get(type);
			if (list1 == null)
			{
				list1 = new ArrayList<NameInfoMemory>();
				typedHits.put(type, list1);
			}
			list1.add(I);
		}
		
		// sort list by importance of term
		for (Collection<NameInfoMemory> l : typedHits.values())
			Collections.sort((List) l);
		
		log.info ("-------------\n" + typedHits.size() + " categories of typed hits identified \n--------------");

		typedHits = Util.sortMapByListSize(typedHits);

		return typedHits;
	}
	
	public static void computeInfo (Map<String, NameInfoMemory> nameMap, Collection<EmailDocument> allDocs, Archive archive, Lexicon lex) throws IOException
	{
		Indexer.QueryType qt = Indexer.QueryType.FULL;
		// assign types to all the names
		if (allDocs == null)
			allDocs = (List) archive.getAllDocs();		
		// compute name -> NameInfoMemory
		Map<String, Collection<Document>> sentimentToDocs = archive.getSentimentMap(lex, true /* original content only */);
		for (EmailDocument ed: allDocs)
		{
			String id = ed.getUniqueId();
			List<String> names = archive.getNamesForDocId(id, qt);
			List<Address> mentionedAddresses = ed.getToCCBCC();
			Set<String> sentimentsForDoc = new LinkedHashSet<String>();
			for (String sentiment :sentimentToDocs.keySet()){
				if (sentimentToDocs.get(sentiment).contains(ed))
					sentimentsForDoc.add(sentiment);
			}	
			
			for (String name: names)
			{
				String cTitle = name.trim().toLowerCase().replaceAll(" ", "_"); // canonical title			
				NameInfoMemory I = nameMap.get(cTitle);
				if (I == null)
				{
					log.info ("Warning: null info for name: " + name);
					continue;
				}
				
				//Map sentiment to its prominence in document.
				if (I.sentimentCatToCount == null)
					I.sentimentCatToCount = new LinkedHashMap<String, Integer>();
				for (String sentiment :sentimentsForDoc){
					if (!I.sentimentCatToCount.containsKey(sentiment)) //if the sentiment isn't there.
						I.sentimentCatToCount.put(sentiment, 1);
					else {
						int sum = I.sentimentCatToCount.get(sentiment);
						sum = sum +1;
						I.sentimentCatToCount.put(sentiment, sum);
					}
				}
				I.sentimentCatToCount = Util.reorderMapByValue(I.sentimentCatToCount);
				//while (I.sentimentCatToCount.containsKey(null)){ //clean sentimentCatToCount
					//I.sentimentCatToCount.remove(null);
					//System.out.println("Cleaned sentimentCatToCount.");
				//}
				//obtain list of contacts to whom email is being sent.
				for (Address adr :mentionedAddresses){
					InternetAddress emailadr = (InternetAddress) adr;
					String address_string = emailadr.getAddress();
					Contact associatedcontact = archive.addressBook.lookupByEmail(address_string);
					if (I.peopleToCount == null)
						I.peopleToCount = new LinkedHashMap<Contact, Integer>();
					
					if (!I.peopleToCount.containsKey(associatedcontact)) //if the contact is not yet associated.
						I.peopleToCount.put(associatedcontact, 1);
					else {
						int sum = I.peopleToCount.get(associatedcontact);
						sum = sum +1;
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
				
				if (I.firstDate.after(documentDate)){
					I.firstDate = documentDate;
				}
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
	public static void main (String args[])
	{
		TypeHierarchy th = new TypeHierarchy();
		
		th.recordTypeCount("A|B|C", 3);
		th.recordTypeCount("D|B|C", 3);
		th.recordTypeCount("X|C", 3);
		th.recordTypeCount("M|N", 5);
		System.out.println(th.toString(true));
	}
}


