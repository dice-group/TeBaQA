package de.uni.leipzig.tebaqa.queryranking.model;

import java.util.List;

public class CompoundTripleCandidates {

    private TripleTemplate templateWith2Res;
    private TripleTemplate templateWith1Res;
    private List<CompoundTriples> candidates;

    public CompoundTripleCandidates(TripleTemplate templateWith2Res, TripleTemplate templateWith1Res, List<CompoundTriples> candidates) {
        this.templateWith2Res = templateWith2Res;
        this.templateWith1Res = templateWith1Res;
        this.candidates = candidates;
    }

    public TripleTemplate getTemplateWith2Res() {
        return templateWith2Res;
    }

    public void setTemplateWith2Res(TripleTemplate templateWith2Res) {
        this.templateWith2Res = templateWith2Res;
    }

    public TripleTemplate getTemplateWith1Res() {
        return templateWith1Res;
    }

    public void setTemplateWith1Res(TripleTemplate templateWith1Res) {
        this.templateWith1Res = templateWith1Res;
    }

    public List<CompoundTriples> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<CompoundTriples> candidates) {
        this.candidates = candidates;
    }
}
