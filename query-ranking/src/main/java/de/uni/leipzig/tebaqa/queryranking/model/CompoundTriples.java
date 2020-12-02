package de.uni.leipzig.tebaqa.queryranking.model;

public class CompoundTriples {
    private final Triple knownTriple;
    private final Triple newTriple;

    public CompoundTriples(Triple knownTriple, Triple newTriple) {
        this.knownTriple = knownTriple;
        this.newTriple = newTriple;
    }

    public Triple getKnownTriple() {
        return knownTriple;
    }

    public Triple getNewTriple() {
        return newTriple;
    }
}
