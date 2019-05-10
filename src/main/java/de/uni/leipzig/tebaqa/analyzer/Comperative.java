package de.uni.leipzig.tebaqa.analyzer;

import edu.stanford.nlp.io.IOUtils;
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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class Comperative implements IAnalyzer {
        static Logger log = LoggerFactory.getLogger(org.aksw.mlqa.analyzer.comperative.Comperative.class);
        private Attribute attribute = null;
        private StanfordCoreNLP pipeline;

        public Comperative() {
            //Properties props = new Properties();
            //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
            //props.setProperty("ner.useSUTime", "false");
            //this.pipeline = new StanfordCoreNLP(props);
            Properties props = new Properties();
            try {
                props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            props.remove("annotators");
            props.setProperty("annotators","tokenize,ssplit,pos,ner,lemma,parse");
            this.pipeline= new StanfordCoreNLP(props);
            FastVector fvWekaComperative = new FastVector(2);
            fvWekaComperative.addElement("Comperative");
            fvWekaComperative.addElement("NoComperative");
            this.attribute = new Attribute("Comperative", fvWekaComperative);
        }

        public Object analyze(String q) {
            String result = "NoComperative";
            Annotation annotation = new Annotation(q);
            this.pipeline.annotate(annotation);
            List<CoreMap> sentences = (List)annotation.get(CoreAnnotations.SentencesAnnotation.class);
            Iterator var5 = sentences.iterator();

            label27:
            while(var5.hasNext()) {
                CoreMap sentence = (CoreMap)var5.next();
                Iterator var7 = ((List)sentence.get(CoreAnnotations.TokensAnnotation.class)).iterator();

                while(true) {
                    String pos;
                    do {
                        if (!var7.hasNext()) {
                            continue label27;
                        }

                        CoreLabel token = (CoreLabel)var7.next();
                        pos = (String)token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    } while(!pos.equals("RBR") && !pos.equals("JJR"));

                    result = "Comperative";
                }
            }

            return result;
        }

        public Attribute getAttribute() {
            return this.attribute;
        }
    }

