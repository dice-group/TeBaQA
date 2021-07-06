package de.uni.leipzig.tebaqa.nlp.core;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public enum NLPLang {

    EN("en") {
        @Override
        public StanfordCoreNLP getPipeline() {
            //disables logging messages from stanford
            RedwoodConfiguration.current().clear().apply();

            LOGGER.info("Creating StanfordCoreNLP " + this.name() + " pipeline...");
            StanfordCoreNLP instance = new StanfordCoreNLP(PropertiesUtils.asProperties(
                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,ner",
//                    "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,depparse,ner",
                    "ssplit.isOneSentence", "true",
                    "tokenize.language", "en"));
            return instance;
        }

        @Override
        public NLPAnalyzer getSemanticAnalysisHelper() {
            return new NLPAnalyzerEnglish();
        }
    },
    DE("de") {
        @Override
        public StanfordCoreNLP getPipeline() {
            //disables logging messages from stanford
            RedwoodConfiguration.current().clear().apply();

            Properties props = new Properties();
            try {
                props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //props.remove("annotators");
            //props.setProperty("annotators","tokenize,ssplit,pos,ner,parse,depparse");

            return new StanfordCoreNLP(props);
        }

        @Override
        public NLPAnalyzer getSemanticAnalysisHelper() {
            return new NLPAnalyzerGerman();
        }
    };

    private static final Logger LOGGER = LogManager.getLogger(StanfordPipelineProvider.class);
    private final String languageCode;

    NLPLang(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public static NLPLang getForCode(String languageCode) {
        for (NLPLang val : NLPLang.values()) {
            if (val.languageCode.equalsIgnoreCase(languageCode))
                return val;
        }
        return null;
    }

    public abstract StanfordCoreNLP getPipeline();

    public abstract NLPAnalyzer getSemanticAnalysisHelper();

}