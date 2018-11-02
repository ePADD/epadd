package edu.stanford.muse.ner.EntityExtractionManager;

import edu.stanford.muse.ner.model.NEType;

public class CICFact{
    String nCIC=null;
    long nStart;
    long nEnd;
    String nMID=null;
    String nNextCIC=null;
    String nInBetween=null;
    NEType.Type nType = null;
    int nEntityID=-1;
    @Override
    public boolean equals(Object other){
        if (other == this) return true;
        if (!(other instanceof CICFact)) {
            return false;
        }

        CICFact otherfact = (CICFact) other;

        return otherfact.nCIC.equals(nCIC) &&
                otherfact.nStart == nStart &&
                otherfact.nEnd == nEnd &&
                otherfact.nMID.equals(nMID) &&
                otherfact.nNextCIC.equals(nNextCIC) &&
                otherfact.nInBetween.equals(nInBetween);//not considering type and typeid for equality right now.
    }
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + nCIC.hashCode();
        result = 31 * result + (int)nStart;
        result = 31 * result + (int)nEnd;
        result = 31 * result + nMID.hashCode();
        result = 31 * result + nNextCIC.hashCode();
        result = 31 * result + nInBetween.hashCode();
        return result;
    }

}
