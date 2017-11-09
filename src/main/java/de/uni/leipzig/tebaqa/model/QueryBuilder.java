package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;

public class QueryBuilder {
    private static Logger log = Logger.getLogger(QueryBuilder.class);
    private SemanticAnalysisHelper analysis;
    private List<CustomQuestion> questions;

    public QueryBuilder(List<CustomQuestion> questions, SemanticAnalysisHelper analysis) {
        this.analysis = analysis;
        for (CustomQuestion question : questions) {
            int i = questions.indexOf(question);
            Map<String, String> dependencySequence = processQuestion(question.getQuestionText());
            question.setDependencySequencePosMap(dependencySequence);
            questions.set(i, question);
        }
        this.questions = questions;
    }

    private Map<String, String> processQuestion(String question) {
        //TODO detect entities, properties and classes from the question
        SemanticGraph semanticGraph = analysis.extractDependencyGraph(question);

        List<IndexedWord> sequence = getDependencySequence(semanticGraph);
        Map<String, String> posSequence = new HashMap<>();
        //Remove the part-of-speech tag from the word: "Atacama/NNP" => "Atacama"
        for (int i = 0; i < sequence.size(); i++) {
            IndexedWord word = sequence.get(i);
            posSequence.put(word.toString().split("/")[0],
                    word.get(PartOfSpeechAnnotation.class) + i);
        }
        //log.info(semanticGraph);
        return posSequence;
    }

    private List<IndexedWord> getDependencySequence(SemanticGraph semanticGraph) {
        IndexedWord firstRoot = semanticGraph.getFirstRoot();
        List<IndexedWord> sequence = getDependenciesFromEdge(firstRoot, semanticGraph);
        log.debug(sequence);
        return sequence;
    }

    private List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {
        final String posExclusion = "DT|IN|WDT|W.*|\\.";
        final String lemmaExclusion = "have|do|be|many|much|give|call|list";
        List<IndexedWord> sequence = new ArrayList<>();
        String rootPos = root.get(PartOfSpeechAnnotation.class);
        String rootLemma = root.get(CoreAnnotations.LemmaAnnotation.class);
        if (!rootPos.matches(posExclusion) && !rootLemma.matches(lemmaExclusion)) {
            sequence.add(root);
        }
        Set<IndexedWord> childrenFromRoot = semanticGraph.getChildren(root);

        for (IndexedWord word : childrenFromRoot) {
            String wordPos = word.get(PartOfSpeechAnnotation.class);
            String wordLemma = word.get(CoreAnnotations.LemmaAnnotation.class);
            if (!wordPos.matches(posExclusion) && !wordLemma.matches(lemmaExclusion)) {
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

    public List<CustomQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<CustomQuestion> questions) {
        this.questions = questions;
    }
}
