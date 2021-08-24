package edu.stanford.muse.ner.tokenize.test;

import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.Triple;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TokenizerTest {

    @Ignore
    @Test
    public void testCICTokenizer() {
        Tokenizer tokenizer = new CICTokenizer();
        String[] contents = new String[]{
                "A book named Information Retrieval by Christopher Manning",
                "I have visited Museum of Modern Arts aka. MoMA, MMA, MoMa",
                "Sound of the Music and Arts program by SALL Studios",
                "Performance by Chaurasia, Hariprasad was great!",
                "Dummy of the and Something",
                "Mr. HariPrasad was present.",
                "We traveled through A174 Road.",
                "The MIT school has many faculty members who were awarded the Nobel Prize in Physics",
                "We are celebrating Amy's first birthday",
                "We are meeting at Barnie's and then go to Terry's",
                "Patrick's portrayal of Barney is wonderful",
                "He won a gold in 1874 Winter Olympics",
                "India got independence in 1947",
                ">Holly Crumpton in an interview said he will never speak to public directly",
                "The popular Ellen de Generes show made a Vincent van Gogh themed episode",
                "Barack-O Obama is the President of USA",
                "CEO--Sundar attended a meeting in Delhi",
                "Subject: Jeb Bush, the presidential candidate",
                "From: Ted Cruz on Jan 15th, 2015",
                "I met Frank'O Connor in the CCD",
                "I have met him in the office yesterday",
                "Annapoorna Residence,\nHouse No: 1975,\nAlma Street,\nPalo Alto,\nCalifornia",
                //It fails here, because OpenNLP sentence model marks Mt. as end of the sentence.
                "Met Mr. Robert Creeley at his place yesterday",
                "Dear Folks, it is party time!",
                "Few years ago, I wrote an article on \"Met The President\"",
                "This is great! I am meeting with Barney   Stinson",
                "The Department of Geology is a hard sell!",
                "Sawadika!\n" +
                        "\n" +
                        "fondly,\n\n",
                "Judith C Stern MA PT\n" +
                        "AmSAT Certified Teacher of the Alexander Technique\n" +
                        "31 Purchase Street\n" +
                        "Rye NY 10580",
                "Currently I am working in a Company",
                "Unfortunately I cannot attend the meeting",
                "Personally I prefer this over anything else",
                "On Behalf of Mr. Spider Man, we would like to apologise",
                "Quoting Robert Creeley, a Black Mountain Poet",
                "Hi Mrs. Senora, glad we have met",
                "Our XXX Company, produces the best detergents in the world",
                "My Thought on Thought makes an infinite loop",
                "Regarding The Bangalore Marathon, it has been cancelled due to stray dogs",
                "I am meeting with him in Jan, and will request for one in Feb, will say OK to everything and disappear on the very next Mon or Tue, etc.",
                "North Africa is the northern portion of Africa",
                "Center of Evaluation has developed some evaluation techniques.",
                "Hi Professor Winograd, this is your student from nowhere",
                ">> Hi Professor Winograd, this is your student from nowhere",
                "Hello this is McGill & Wexley Co.",
                "Why Benjamin Netanyahu may look",
                "I am good Said Netanyahu",
                "Even Netanyahu was present at the party",
                "The New York Times is a US based daily",
                "Do you know about The New York Times Company that brutally charges for Digital subscription",
                "Fischler proposed EU-wide measures after reports from Britain and France that under laboratory conditions sheep could contract Bovine Spongiform Encephalopathy ( BSE ) -- mad cow disease",
                "Spanish Farm Minister Loyola de Palacio had earlier accused Fischler at an EU farm ministers ' meeting of causing unjustified alarm through \" dangerous generalisation .",
                "P.V. Krishnamoorthi",
                "Should Rubin be told about this?",
                "You are talking to Robert Who?",
                "I will never say a thing SAID REBECCA HALL",
                "\" Airport officials declared an emergency situation at the highest level and the fire brigade put out the flames while the plane was landing , he said .",
                "Brussels received 5.6 cm ( 2.24 inches ) of water in the past 24 hours -- compared to an average 7.4 cm ( 2.96 inches ) per month -- but in several communes in the south of the country up to 8 cm ( 3.2 inches ) fell , the Royal Meteorological Institute ( RMT ) said",
                "Danish cleaning group ISS on Wednesday said it had signed a letter of intent to sell its troubled U.S unit ISS Inc to Canadian firm Aaxis Limited",
                "That was one hell of a Series!",
                "I am from India said No one.",
                "Rachel and I went for a date in the imaginary land of geeks.",
                "I'm the one invited.",
                "Shares in Slough , which earlier announced a 14 percent rise in first-half pretax profit to 37.4 million stg , climbed nearly six percent , or 14p to 250 pence at 1009 GMT , while British Land added 12-1 / 2p to 468p , Land Securities rose 5-1 / 2p to 691p and Hammerson was 8p higher at 390 ."
        };
        String[][] tokens = new String[][]{
                new String[]{"Information Retrieval", "Christopher Manning"},
                new String[]{"Museum of Modern Arts", "MoMA", "MMA", "MoMa"},
                new String[]{"Music and Arts", "SALL Studios"},
                new String[]{"Chaurasia", "Hariprasad"},
                new String[]{},
                new String[]{"Mr. HariPrasad"},
                new String[]{"A174 Road"},
                new String[]{"MIT", "Nobel Prize in Physics"},
                new String[]{"Amy"},
                new String[]{"Barnie", "Terry"},
                new String[]{"Patrick", "Barney"},
                new String[]{"Winter Olympics"},
                new String[]{"India"},
                new String[]{"Holly Crumpton"},
                new String[]{"Ellen de Generes", "Vincent van Gogh"},
                new String[]{"Barack-O Obama", "President of USA"},
                new String[]{"CEO", "Sundar", "Delhi"},
                new String[]{"Jeb Bush"},
                //Can we do a better job here? without knowing that Ted Cruz is a person.
                new String[]{"Ted Cruz"},
                new String[]{"Frank'O Connor", "CCD"},
                new String[]{},
                new String[]{"Annapoorna Residence", "House No", "Alma Street", "Palo Alto", "California"},
                new String[]{"Mr. Robert Creeley"},
                new String[]{},
                new String[]{"President"},
                new String[]{"Barney   Stinson"},
                new String[]{"Department of Geology"},
                new String[]{"Sawadika"},
                new String[]{"Judith C Stern MA PT", "AmSAT Certified Teacher", "Alexander Technique", "Purchase Street", "Rye NY"},
                new String[]{},
                new String[]{},
                new String[]{},
                new String[]{"Mr. Spider Man"},
                new String[]{"Robert Creeley", "Black Mountain Poet"},
                new String[]{"Mrs. Senora"},
                new String[]{"XXX Company"},
                new String[]{"Thought"},
                new String[]{"Bangalore Marathon"},
                new String[]{},
                new String[]{"North Africa", "Africa"},
                new String[]{"Center of Evaluation"},
                new String[]{"Professor Winograd"},
                new String[]{"Professor Winograd"},
                new String[]{"McGill & Wexley Co"},
                new String[]{"Benjamin Netanyahu"},
                new String[]{"Netanyahu"},
                new String[]{"Netanyahu"},
                new String[]{"New York Times", "US"},
                new String[]{"New York Times Company"},
                new String[]{"Fischler", "EU-wide", "Britain and France", "Bovine Spongiform Encephalopathy", "BSE"},
                new String[]{"Spanish Farm Minister Loyola de Palacio", "Fischler", "EU"},
                new String[]{"P.V. Krishnamoorthi"},
                new String[]{"Rubin"},
                new String[]{"Robert"},
                new String[]{"REBECCA HALL"},
                new String[]{},
                new String[]{"Royal Meteorological Institute", "RMT", "Brussels"},
                new String[]{"ISS", "ISS Inc", "Canadian", "Wednesday", "Aaxis Limited","U.S","Danish"},
                new String[]{},
                new String[]{"India"},
                new String[]{"Rachel"},
                new String[]{},
                //this is a bad case
                new String[]{"Shares in", "GMT", "British Land", "Land Securities", "Hammerson"}
        };

        //failing tests
        //new String[]{"Mt. Everest"} "I have been thinking about it, and I should say it out loud. I am going to climb Mt. \nEverest"
        //new String[]{"Harvard Law School", "Dr. West"}, "Harvard Law School\n\nDr. West is on a holiday trip now.",


        for (int ci = 0; ci < contents.length; ci++) {
            String content = contents[ci];
            List<String> ts = Arrays.asList(tokens[ci]);
            //want to specifically test person names tokenize for index 3.
            List<Triple<String,Integer,Integer>> cics = tokenizer.tokenize(content);
            List<String> cicTokens = cics.stream().map(t->t.first).collect(Collectors.toList());

            boolean missing = ts.stream().anyMatch(t->!cicTokens.contains(CICTokenizer.canonicalize(t)));
            boolean wrongOffsets = cics.stream().anyMatch(t->{
                if(!ts.contains(content.substring(t.second,t.third))) {
                    System.out.println("Fail at: "+content.substring(t.second,t.third));
                    return true;
                }
                return false;
            });
            String str = "------------\n" +
                    "Test failed!\n" +
                    "Content: " + content + "\n" +
                    "Expected tokens: " + ts + "\n" +
                    "Found: " + cics + "\n";
            assertTrue("Missing tokens: "+str, cics.size() == ts.size() && !missing);
            assertTrue("Wrong offsets: "+str, cics.size() == ts.size() && !wrongOffsets);
        }
    }
}
