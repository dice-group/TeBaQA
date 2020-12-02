package de.uni.leipzig.tebaqa.model;

import java.util.List;

public class CompoundPatternCandidates {

    private TripleTemplate patternWith2Res;
    private TripleTemplate patternWith1Res;
    private List<Triple[]> candidates;

    public CompoundPatternCandidates(TripleTemplate patternWith2Res, TripleTemplate patternWith1Res, List<Triple[]> candidates) {
        this.patternWith2Res = patternWith2Res;
        this.patternWith1Res = patternWith1Res;
        this.candidates = candidates;
    }

    public TripleTemplate getPatternWith2Res() {
        return patternWith2Res;
    }

    public void setPatternWith2Res(TripleTemplate patternWith2Res) {
        this.patternWith2Res = patternWith2Res;
    }

    public TripleTemplate getPatternWith1Res() {
        return patternWith1Res;
    }

    public void setPatternWith1Res(TripleTemplate patternWith1Res) {
        this.patternWith1Res = patternWith1Res;
    }

    public List<Triple[]> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Triple[]> candidates) {
        this.candidates = candidates;
    }
}
