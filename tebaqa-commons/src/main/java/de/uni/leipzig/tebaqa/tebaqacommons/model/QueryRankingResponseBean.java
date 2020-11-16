package de.uni.leipzig.tebaqa.tebaqacommons.model;

import java.util.Collection;

public class QueryRankingResponseBean {
    private Collection<RatedQuery> generatedQueries;

    private QueryRankingResponseBean() {}

    public QueryRankingResponseBean(Collection<RatedQuery> generatedQueries) {
        this.generatedQueries = generatedQueries;
    }

    public Collection<RatedQuery> getGeneratedQueries() {
        return generatedQueries;
    }

    public void setGeneratedQueries(Collection<RatedQuery> generatedQueries) {
        this.generatedQueries = generatedQueries;
    }
}
