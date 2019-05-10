package de.uni.leipzig.tebaqa.analyzer;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import weka.core.Attribute;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Verb implements IAnalyzer {
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public Verb() {
        Properties props = new Properties();
        try {
            props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        props.remove("annotators");
        props.setProperty("annotators","tokenize,ssplit,pos,ner,parse");
        this.pipeline= new StanfordCoreNLP(props);
        //pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        attribute = new Attribute("NumberOfVerbs");
    }

    public Object analyze(String q) {
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        double verbCnt = 0;
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel token : tokens) {
                String posTag = token.tag();
                if (posTag.startsWith("VB")) {
                    verbCnt++;
                }
            }

        }
        return verbCnt;
    }

    @Override
    public Attribute getAttribute() {
        return attribute;
    }
}
