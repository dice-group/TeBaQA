package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;

public class QueryBuilder {
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

        List<IndexedWord> sequence = analysis.getDependencySequence(semanticGraph);
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

    public List<CustomQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<CustomQuestion> questions) {
        this.questions = questions;
    }
}
