package edu.stanford.muse.ner.tokenize;

import edu.stanford.muse.util.NLPUtils;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Triple;
import opennlp.tools.util.Span;

import java.util.*;

/**
 * Created by vihari on 09/10/15.
 *
 * A tokenize based on POS tagging */
public class POSTokenizer implements Tokenizer{
    private final static int MAX_SENT_LENGTH = 500;

    /**
     * {@inheritDoc}
     * */
    @Override
    public List<Triple<String, Integer, Integer>> tokenize(String content){
        Span[] sents = NLPUtils.sentenceDetector.sentPosDetect(content);
        List<Triple<String, Integer, Integer>> ret = new ArrayList<>();
        for(Span span: sents) {
            String sent = span.getCoveredText(content).toString();
            if(sent==null || sent.length()>MAX_SENT_LENGTH)
                continue;
            List<Pair<String,Triple<String,Integer,Integer>>> posTags = NLPUtils.posTagWithOffsets(sent);
            List<String> allowedPOSTags = Arrays.asList("NNP", "NNS", "NN", "JJ", "IN", "POS");

            int startOffset = 0;
            int endOffset = 0;
            String str = "";
            boolean padded = false;
            int padL = 0;
            for (int pi=0;pi<posTags.size();pi++) {
                Pair<String, Triple<String,Integer,Integer>> p = posTags.get(pi);
                String tag = p.second.first;
                String nxtTag = null;
                if(pi<posTags.size()-1)
                    nxtTag = posTags.get(pi+1).second.first;

                //POS for 's
                //should not end or start in improper tags
                //!!Think twice before making changes here, dont mess up the offsets!!
                boolean startCond = str.equals("") && (tag.equals("POS")||tag.equals("IN")||p.getFirst().equals("'")||p.getFirst().equals("Dear")||p.getFirst().equals("from"));
                boolean endCond = ((nxtTag==null||!allowedPOSTags.contains(nxtTag)) && (tag.equals("POS")||tag.equals("IN")||p.getFirst().equals("'")));
                boolean isEnd = nxtTag==null||!allowedPOSTags.contains(nxtTag);
                if (allowedPOSTags.contains(tag) && !startCond && !endCond) {
                    str += p.getFirst();
                    //add the separating delimiters between tokens only if the token being considered does not end the segment
                    //the test for end is not trivial, hence the check for if the string is padded
                    if(!isEnd) {
                        String pad = sent.substring(p.second.getThird(),((pi+1)<posTags.size())?posTags.get(pi+1).getSecond().getSecond():sent.length());
                        str += pad;
                        padL = pad.length();
                        padded = true;
                    }
                    else
                        padded = false;
                }
                else {
                    if(!str.equals("")) {
                        if(padded)
                            str = str.substring(0,str.length()-padL);
                        ret.add(new Triple<>(str, startOffset, endOffset));
                        str = "";
                    }
                    if(pi<posTags.size()-1)
                        startOffset = posTags.get(pi+1).second.getSecond();
                }
                endOffset = p.second.getThird();
            }
            if (!str.equals("")) {
                if(padded)
                    str = str.substring(0,str.length()-padL);
                //sentence ending is the segment ending
                ret.add(new Triple<>(str, startOffset, endOffset));
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        String content = "..................  Zuckerberg Giving $100 Million Gift to Newark Schools By " +
                "RICHARD PREZ-PEA A gift from Mark Zuckerberg, the chief executive of  Facebook " +
                ", to the notoriously troubled Newark school  system is part of a plan to start " +
                "an educational foundation.";
        List<Triple<String,Integer,Integer>> offsets = new POSTokenizer().tokenize(content);
        System.err.println(offsets);
        for(Triple<String,Integer,Integer> t: offsets){
            if(!content.substring(t.getSecond(),t.getThird()).equals(t.getFirst()))
                System.err.println("Mismatch for: "+t);
        }
    }
}
