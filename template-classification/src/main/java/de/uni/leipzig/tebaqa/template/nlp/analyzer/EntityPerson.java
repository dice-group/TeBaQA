package de.uni.leipzig.tebaqa.template.nlp.analyzer;

import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.StanfordPipelineProvider;
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

public class EntityPerson implements IAnalyzer {
    static Logger log = LoggerFactory.getLogger(org.aksw.mlqa.analyzer.entityType.EntityPerson.class);
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;

    public EntityPerson() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(Lang.EN);
        FastVector fvWekaPerson = new FastVector(2);
        fvWekaPerson.addElement("Person");
        fvWekaPerson.addElement("NoPerson");
        this.attribute = new Attribute("Person", fvWekaPerson);
    }

    public Object analyze(String q) {
        String result = "NoPerson";
        Annotation annotation = new Annotation(q);
        this.pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        Iterator var5 = sentences.iterator();

        while (var5.hasNext()) {
            CoreMap sentence = (CoreMap) var5.next();
            Iterator var7 = ((List) sentence.get(CoreAnnotations.TokensAnnotation.class)).iterator();

            while (var7.hasNext()) {
                CoreLabel token = (CoreLabel) var7.next();
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
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
