package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

class QueryBuilder {
    private static Logger log = Logger.getLogger(QueryBuilder.class);

    /**
     * Builds a SPARQL Query based on a question and its template.
     *
     * @param question The question which contains the necessary information to build the query.
     * @param graph    The graph which represents the query structure (e.g.  {"1" @"p" "2"}). This graph is generated
     *                 by {@link de.uni.leipzig.tebaqa.controller.QueryIsomorphism}.
     */
    QueryBuilder(String question, String graph) {
        SemanticAnalysisHelper analysis = new SemanticAnalysisHelper();
        SemanticGraph semanticGraph = analysis.extractDependencyGraph(question);

        getDependencySequence(semanticGraph);
        log.debug(semanticGraph);

    }

    private void getDependencySequence(SemanticGraph semanticGraph) {
        IndexedWord firstRoot = semanticGraph.getFirstRoot();
        List<IndexedWord> sequence = getDependenciesFromEdge(firstRoot, semanticGraph);
        log.debug(sequence);

    }

    private List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {
        String exclusion = "DT|IN|\\.";
        List<IndexedWord> sequence = new ArrayList<>();
        String rootPos = root.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        if (!rootPos.matches(exclusion)) {
            sequence.add(root);
        }
        for (IndexedWord word : semanticGraph.getChildren(root)) {
            String wordPos = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            if (!wordPos.matches(exclusion)) {
                sequence.add(word);
            }
            if (semanticGraph.hasChildren(word)) {
                for (IndexedWord child : semanticGraph.getChildren(word))
                    sequence.addAll(getDependenciesFromEdge(child, semanticGraph));
            }
        }
        return sequence;
    }

    String getQuery() {
        return null;
    }
}
