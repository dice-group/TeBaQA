package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.ElasticSearchEntityIndex;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static de.uni.leipzig.tebaqa.helper.QueryMappingFactoryLabels.getNeighborCoOccurrencePermutations;

public class SearchOntology {
    private SemanticAnalysisHelper semanticAnalysisHelper;
    ElasticSearchEntityIndex index;
    String propertyIndexName;
    String classIndexName;
    private double threshold;
    public SearchOntology(SemanticAnalysisHelper semanticAnalysisHelper){
        try {
            Properties prop = new Properties();
            InputStream input = new FileInputStream("src/main/resources/application.properties");
            prop.load(input);
            this.index=new ElasticSearchEntityIndex();
            this.semanticAnalysisHelper=semanticAnalysisHelper;
            propertyIndexName=prop.getProperty("property_index");
            classIndexName=prop.getProperty("class_index");
            threshold=Double.parseDouble(prop.getProperty("threshold"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private List<String[]>search(String coOccurence,String lang,String indexname){
        List<String[]>candidates;
        candidates=index.search(coOccurence,indexname,lang);
        List<String[]>candidatesFiltered=new ArrayList<>();
        for(String[] cand:candidates){
            double levensthein=Utilities.getLevenshteinRatio(coOccurence, cand[1]);
            if(levensthein<threshold)
                candidatesFiltered.add(cand);
        }
        return candidatesFiltered;
    }
    public HashMap<String,List<String[]>> getProperties(String question,String lang){
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question=question.replace("(","");
        question=question.replace(")","");
        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");
        List<String>coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        HashMap<String,List<String[]>>propertyCandidates=new HashMap<>();
        //TODO maybe remove plural forms before
        for(String coOccurence:coOccurrences) {
            List<String[]>foundProperties=search(coOccurence,lang,propertyIndexName);
            if (foundProperties.size()>0) propertyCandidates.put(coOccurence,foundProperties);
        }
        return propertyCandidates;
    }
    public HashMap<String,List<String[]>> getClasses(String question,String lang){
        question = semanticAnalysisHelper.removeQuestionWords(question);
        question=question.replace("(","");
        question=question.replace(")","");
        String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");
        List<String>coOccurrences = getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
        coOccurrences.sort((s1, s2) -> -(s1.length() - s2.length()));
        HashMap<String,List<String[]>>propertyCandidates=new HashMap<>();
        for(String coOccurence:coOccurrences) {
            List<String[]>foundProperties=search(coOccurence,lang,classIndexName);
            if (foundProperties.size()>0) propertyCandidates.put(coOccurence,foundProperties);
        }
        return propertyCandidates;
    }
}
