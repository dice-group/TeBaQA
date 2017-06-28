package de.uni.leipzig.tebaqa.Analyzer;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import org.apache.xerces.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import weka.core.Attribute;

import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

public class TripleCandidates implements IAnalyzer {
    private static Logger log = LoggerFactory.getLogger(TripleCandidates.class);
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;
    private String serializedClassifier = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

    public TripleCandidates() throws IOException, ClassNotFoundException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        props.setProperty("ner.useSUTime", "false");
        pipeline = new StanfordCoreNLP(props);

        attribute = new Attribute("TripleCandidatesCount");
    }

    @Override
    public Object analyze(String q) {
        List<String> nouns = new ArrayList<>();
        List<String> verbs = new ArrayList<>();
        List<String> adj = new ArrayList<>();
        List<String> neList = getNEList(q);
        String exceptions = "have||do||be||many||much||give||call||list";

        //prevents double recognizing named entities
        String qWithoutNE = "";
        for (String ne : neList) {
            qWithoutNE = q.replace(ne, "");
        }
        Annotation annotation = new Annotation(qWithoutNE);
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> labels = sentence.get(TokensAnnotation.class);
            for (int i = 0; i < labels.size(); i++) {
                CoreLabel token = labels.get(i);
                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                String lemma = token.get(LemmaAnnotation.class);
                if (!lemma.matches(exceptions)) {
                    if (pos.startsWith("VB") && !neList.contains(word)) {
                        verbs.add(word);
                    } else if (pos.startsWith("NN") && !neList.contains(word)) {
                        if (i > 0) {
                            String previousWord = labels.get(i - 1).get(PartOfSpeechAnnotation.class);
                            if (!previousWord.startsWith("NN") && !neList.contains(previousWord)) {
                                nouns.add(word);
                            }
                        }

                    } else if (pos.startsWith("JJ") && !neList.contains(word)) {
                        adj.add(word);
                    }
                }

            }
        }

        int tokenCount = adj.size() + verbs.size() + nouns.size() + neList.size();
        if (tokenCount > 4) {
            return (double) 4;
        } else if (tokenCount == 4) {
            return (double) 3;
        } else if (tokenCount == 3) {
            return (double) 2;
        }
        return (double) 1;

    }

    private List<String> getNEList(String text) {
        List<String> neList = new ArrayList<>();

        try {
            String classifiedText = classifier.classifyWithInlineXML(text);
            String xml = "<sentence>" + classifiedText.trim() + "</sentence>";
            DOMParser parser = new DOMParser();
            try {
                parser.parse(new InputSource(new StringReader(xml)));
                Document doc = parser.getDocument();
                XPath xpath = XPathFactory.newInstance().newXPath();
                XPathExpression expr1 = xpath.compile("/sentence/*/text()");
                NodeList nodeList = (NodeList) expr1.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    neList.add(node.getNodeValue());
                }
            } catch (SAXException | IOException | XPathExpressionException e) {
                log.error("Could not extract NER sequence out of:" + text, e);
            }
        } catch (ClassCastException e) {
            log.error("Could not extract NER sequence out of:" + text, e);
        }

        return neList;
    }

    public Attribute getAttribute() {
        return attribute;
    }
}
