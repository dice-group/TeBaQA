package de.uni.leipzig.tebaqa.analyzer;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Attribute;
import weka.core.FastVector;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class EntityPerson implements IAnalyzer {
    //static Logger log = LoggerFactory.getLogger(org.aksw.mlqa.analyzer.entityType.EntityPerson.class);
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public EntityPerson() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        FastVector fvWekaPerson = new FastVector(2);
        fvWekaPerson.addElement("Person");
        fvWekaPerson.addElement("NoPerson");
        this.attribute = new Attribute("Person", fvWekaPerson);
    }

    public Object analyze(String q) {
        String result = "NoPerson";
        Annotation annotation = new Annotation(q);
        this.pipeline.annotate(annotation);
        List<CoreMap> sentences = (List)annotation.get(CoreAnnotations.SentencesAnnotation.class);
        Iterator var5 = sentences.iterator();

        while(var5.hasNext()) {
            CoreMap sentence = (CoreMap)var5.next();
            Iterator var7 = ((List)sentence.get(CoreAnnotations.TokensAnnotation.class)).iterator();

            while(var7.hasNext()) {
                CoreLabel token = (CoreLabel)var7.next();
                String ne = (String)token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                if (ne.equals("PERSON")) {
                    result = "Person";
                }
            }
        }

        return result;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }
}
