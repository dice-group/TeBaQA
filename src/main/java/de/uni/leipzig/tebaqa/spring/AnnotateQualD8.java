package de.uni.leipzig.tebaqa.spring;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import de.uni.leipzig.tebaqa.controller.PipelineControllerTripleTemplates;
import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.helper.FillTemplatePatternsWithResources;
import de.uni.leipzig.tebaqa.helper.ResourceLinker;
import de.uni.leipzig.tebaqa.model.AnswerToQuestion;
import moa.recommender.rc.utils.Hash;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.aksw.qa.commons.utils.SPARQLExecutor;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipeline;
import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipelineTripleTemplates;
import static de.uni.leipzig.tebaqa.helper.PipelineProvider.getQAPipelinecalculateModelPipeline;

public class AnnotateQualD8 {

    public static List<IQuestion>loadQuald9(){
        QaldJson json = null;
        List<IQuestion> out=null;
        String deriveUri=null;
        try {
            json = (QaldJson) ExtendedQALDJSONLoader.readJson(new FileInputStream(new File("C:/Users/Jan/Desktop/qald-9-train-multilingual.json")), QaldJson.class);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;

    }
    public static List<IQuestion>loadQuald9Test(){
        QaldJson json = null;
        List<IQuestion> out=null;
        String deriveUri=null;
        try {
            json = (QaldJson) ExtendedQALDJSONLoader.readJson(new FileInputStream(new File("C:/Users/Jan/Desktop/qald-9-test-multilingual.json")), QaldJson.class);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;

    }
    public static List<IQuestion>loadLimbo(){
        QaldJson json = null;
        List<IQuestion> out=null;
        String deriveUri=null;
        try {
            json = (QaldJson) ExtendedQALDJSONLoader.readJson(new FileInputStream(new File("datasetdownload_6670.json")), QaldJson.class);
            out = EJQuestionFactory.getQuestionsFromQaldJson(json);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;

    }

    public static void main(String[]args){

        Dataset testDataset=Dataset.QALD8_Test_Multilingual;

        //List<IQuestion> load = LoaderController.load(testDataset);
        List<IQuestion> load = loadLimbo();
        HashMap<String,String> questionToId=new HashMap<>();
        HashMap<String,String> idToSparql=new HashMap<>();
        load.forEach(q->{
            Map<String,String> questions=q.getLanguageToQuestion();
            questionToId.put(q.getId(),questions.get("de"));
            idToSparql.put(q.getId(),q.getSparqlQuery());
        }
        );
        //PipelineControllerTripleTemplates qaPipeline=getQAPipelineTripleTemplates();
        //PipelineController qaPipeline=getQAPipelinecalculateModelPipeline();
        PipelineController qaPipeline=getQAPipeline();
        HashMap<String,AnswerToQuestion>idToAnswer=new HashMap<>();
        questionToId.keySet().forEach(question->idToAnswer.put((question),qaPipeline.answerLimboQuestion(questionToId.get(question))));

        /*SemanticAnalysisHelper h=new SemanticAnalysisHelper();
        questionToId.keySet().forEach(q-> {
            FillTemplatePatternsWithResources fillTemplatePatternsWithResources = new FillTemplatePatternsWithResources(h);
            fillTemplatePatternsWithResources.extractEntities(questionToId.get(q));
        });*/
        /*questionToId.keySet().forEach(q->{
            ResourceLinker l=new ResourceLinker(h);
            System.out.println("Question: "+questionToId.get(q));
            System.out.println("Query: "+idToSparql.get(q));
            l.extractEntities(questionToId.get(q));
            System.out.println("SP");
            l.spTupels.forEach(t->System.out.println(t.getSubject().getCoOccurence()+","+t.getPredicate().getCoOccurence()));
            l.spTupels.forEach(t->System.out.println(t.getSubject().getUri()+","+t.getPredicate().getUri()));
            System.out.println("PO");
            l.poTupels.forEach(t->System.out.println(t.getPredicate().getCoOccurence()+","+t.getObject().getCoOccurence()));
            l.poTupels.forEach(t->System.out.println(t.getPredicate().getUri()+","+t.getObject().getUri()));
            System.out.println("SO");
            l.soTupels.forEach(t->System.out.println(t.getSubject().getCoOccurence()+","+t.getObject().getCoOccurence()));
            l.soTupels.forEach(t->System.out.println(t.getSubject().getUri()+","+t.getObject().getUri()));
            System.out.println("Class");
            l.mappedClasses.forEach(c->System.out.println(c.getCoOccurence()+"->"+c.getUri()));
            System.out.println("Property");
            l.mappedProperties.forEach(c->System.out.println(c.getCoOccurence()+"->"+c.getUri()));
            System.out.println("Entity");
            l.mappedEntities.forEach(c->System.out.println(c.getCoOccurence()+"->"+c.getUri()));
            System.out.println();
        });*/
        //idToAnswer.put(("22"),qaPipeline.answerQuestion(questionToId.get("22")));
        questionToId.keySet().forEach(question->idToAnswer.put((question),qaPipeline.answerQuestion(questionToId.get(question))));
        idToAnswer.keySet().forEach(as->{
            System.out.println(questionToId.get(as));
            System.out.println(idToSparql.get(as));
            System.out.println(idToAnswer.get(as).getSparqlQuery());
            System.out.println();
        });
        String answers=ExtendedQALDAnswer.ExtendedQALDAnswerFromMap(idToAnswer);
        System.out.println(answers);


    }
}
