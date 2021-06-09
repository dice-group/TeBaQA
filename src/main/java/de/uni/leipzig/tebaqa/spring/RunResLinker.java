package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.helper.FillTemplatePatternsWithResources;
import org.aksw.qa.commons.datastructure.IQuestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9Test;

public class RunResLinker {
    public static void main(String[]args) {
        List<IQuestion> load = loadQuald9Test();
        HashMap<String, String> questionToId = new HashMap<>();
        HashMap<String, String> idToSparql = new HashMap<>();
        load.forEach(q -> {
            Map<String, String> questions = q.getLanguageToQuestion();
            questionToId.put(q.getId(), questions.get("en"));
            idToSparql.put(q.getId(), q.getSparqlQuery());
        });
        SemanticAnalysisHelper h=new SemanticAnalysisHelper();
        HashMap<String,FillTemplatePatternsWithResources> resFiller=new HashMap<>();
        questionToId.keySet().forEach(q->{
            FillTemplatePatternsWithResources fill=new FillTemplatePatternsWithResources(h);
            fill.extractEntities(questionToId.get(q));
            resFiller.put(questionToId.get(q),fill);
        });
        System.out.println();
    }
}
