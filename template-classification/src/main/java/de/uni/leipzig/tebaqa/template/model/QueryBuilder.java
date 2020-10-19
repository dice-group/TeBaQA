package de.uni.leipzig.tebaqa.template.model;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.ISemanticAnalysisHelper;

import java.util.List;
import java.util.Map;

public class QueryBuilder {
    private final ISemanticAnalysisHelper semanticAnalysisHelper;
    private List<Cluster> questions;
    //private List<CustomQuestion> questions;

    /*public QueryBuilder(List<CustomQuestion> questions, SemanticAnalysisHelper analysis) {
        this.analysis = analysis;
        for (CustomQuestion question : questions) {
            int i = questions.indexOf(question);
            Map<String, String> dependencySequence = processQuestion(question.getQuestionText());
            question.setDependencySequencePosMap(dependencySequence);
            questions.set(i, question);
        }
        this.questions = questions;
    }*/
    public QueryBuilder(List<Cluster> clusters, ISemanticAnalysisHelper semanticAnalysisHelper) {
        this.semanticAnalysisHelper = semanticAnalysisHelper;
        for (Cluster c : clusters) {
            for (CustomQuestion question : c.getQuestions()) {
                //int i = c,getQu.indexOf(question);
                Map<String, String> dependencySequence = processQuestion(question.getQuestionText());
                question.setDependencySequencePosMap(dependencySequence);
                //questions.set(i, question);
            }
        }
        this.questions = clusters;
    }

    public List<Cluster> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Cluster> questions) {
        this.questions = questions;
    }

    private Map<String, String> processQuestion(String question) {
        //detect entities, properties and classes from the question TODO
        //SemanticGraph semanticGraph = analysis.extractDependencyGraph(question);

        //List<IndexedWord> sequence = analysis.getDependencySequence(semanticGraph);
        //Map<String, String> posSequence = new HashMap<>();
        //Remove the part-of-speech tag from the word: "Atacama/NNP" => "Atacama"
        /*for (int i = 0; i < sequence.size(); i++) {
            IndexedWord word = sequence.get(i);
            posSequence.put(word.toString().split("/")[0],
                    word.get(PartOfSpeechAnnotation.class) + i);
        }*/
        Map<String, String> posSequence = semanticAnalysisHelper.getPosTags(question);
        //log.info(semanticGraph);
        return posSequence;
    }


}
