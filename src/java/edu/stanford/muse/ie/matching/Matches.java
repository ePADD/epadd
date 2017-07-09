package edu.stanford.muse.ie.matching;

import com.google.gson.Gson;
import edu.stanford.muse.util.Util;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/* class that represent the results of matching of a given string. Matching currently means expansion of a single word name or acronym, but it could be generalized in the future */
public class Matches {

    private String matchString;
    private int maxMatches;
    private transient Set<String> matchedCstrings = new LinkedHashSet();
    private List<Matches.Match> matches = new ArrayList();

    /* small class that encapsulates information of a given match */
    public class Match {
        private String originalString;
        private String match;
        private float score;
        private StringMatchType matchType;
        private String matchDescription;
        private boolean isContact;

        public Match(String matchString, float score, StringMatchType matchType, String matchDescription, boolean isContact) {
            this.score = score;
            this.match = matchString;
            this.matchType = matchType;
            this.matchDescription = matchDescription;
            this.isContact = isContact;
        }
    }


    public Matches(String matchString, int maxMatches) {
        this.matchString = matchString;
        this.maxMatches = maxMatches;
    }

    public String getMatchString() {
        return this.matchString;
    }

    /* any string comparison should only be called on a canonicalized string */
    private static String canonicalize(String s) {
        return Util.canonicalizeSpaces(s.toLowerCase().trim());
    }

    /** returns match type of s with candidate */
    public static StringMatchType match(String s, String candidate) {

        if (s == null || candidate == null)
            return null;

        String cs = canonicalize(s);
        String ccandidate = canonicalize(candidate);

        // check if s is a substring of candidate
        if (ccandidate.contains(cs) && !ccandidate.equals(cs))
            return StringMatchType.CONTAINED;

        List<String> candidateTokensLower = Util.tokenize(ccandidate);
        // check if s is an acronym of candidate
        {
            if (Util.allUppercase(s)) {
                String candidateAbbrevLower = "";
                for (String candidateToken : candidateTokensLower) {
                    if (candidateToken.length() >= 1) {
                        candidateAbbrevLower = candidateAbbrevLower + candidateToken.charAt(0);
                    }
                }

                if (cs.equals(candidateAbbrevLower)) {
                    return StringMatchType.ACRONYM;
                }
            }
        }

        // check if s contains same tokens as candidate but in a different order (repeated tokens are ignored)
        {
            Set<String> candidateTokensLowerSet = new LinkedHashSet(candidateTokensLower);
            Set<String> sTokensSet = new LinkedHashSet(Util.tokenize(cs));
            if (candidateTokensLowerSet.containsAll(sTokensSet)) {
                return StringMatchType.CONTAINED_DIFFERENT_ORDER;
            }
        }
        return null;
    }

    /* returns whether number of matches is >= maxMatches after adding a match with the given details. */
    boolean addMatch(String matchString, float score, StringMatchType matchType, String matchDescription, boolean isContact) {
        String cmatchString = canonicalize(matchString);
        // if already matched, no need to do anything
        if(!this.matchedCstrings.contains(cmatchString)) {
            this.matches.add(new Matches.Match(matchString, score, matchType, matchDescription, isContact));
            this.matchedCstrings.add(cmatchString);
        }

        return this.matches.size() >= this.maxMatches;
    }

    public String toJson() {
        return (new Gson()).toJson(this);
    }
}

