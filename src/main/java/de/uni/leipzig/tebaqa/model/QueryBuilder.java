package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.aksw.qa.commons.datastructure.Question;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;

public class QueryBuilder {
    private static Logger log = Logger.getLogger(QueryBuilder.class);
    private SemanticAnalysisHelper analysis = new SemanticAnalysisHelper();

    public QueryBuilder(List<Cluster> clusters) {
        for (Cluster cluster : clusters) {
            log.info("Graph: " + cluster.getGraph());
            for (Question question : cluster.getQuestions()) {
                log.info("\t" + String.join(" ", processQuestion(question.getLanguageToQuestion().get("en"),
                        cluster.getGraph())));
            }
        }
    }

    private List<String> processQuestion(String question, String graph) {
        //TODO detect entities, properties and classes from the question
        SemanticGraph semanticGraph = analysis.extractDependencyGraph(question);

        List<IndexedWord> sequence = getDependencySequence(semanticGraph);
        List<String> posSequence = new ArrayList<>();
        sequence.forEach(word -> posSequence.add(word.get(PartOfSpeechAnnotation.class)));
        log.debug(String.join(" ", posSequence));

        log.debug(semanticGraph);
        return posSequence;
    }

    private List<IndexedWord> getDependencySequence(SemanticGraph semanticGraph) {
        IndexedWord firstRoot = semanticGraph.getFirstRoot();
        List<IndexedWord> sequence = getDependenciesFromEdge(firstRoot, semanticGraph);
        log.debug(sequence);
        return sequence;
    }

    private List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {
        final String exclusion = "DT|IN|WDT\\.";
        List<IndexedWord> sequence = new ArrayList<>();
        String rootPos = root.get(PartOfSpeechAnnotation.class);
        if (!rootPos.matches(exclusion)) {
            sequence.add(root);
        }
        Set<IndexedWord> childrenFromRoot = semanticGraph.getChildren(root);

        for (IndexedWord word : childrenFromRoot) {
            String wordPos = word.get(PartOfSpeechAnnotation.class);
            if (!wordPos.matches(exclusion)) {
                sequence.add(word);
            }
            List<IndexedWord> children = semanticGraph.getChildList(word);
            //In some cases a leaf has itself as children which results in endless recursion.
            if (children.contains(root)) {
                children.remove(root);
            }
            for (IndexedWord child : children) {
                sequence.addAll(getDependenciesFromEdge(child, semanticGraph));
            }
        }
        return sequence;

    }
}
