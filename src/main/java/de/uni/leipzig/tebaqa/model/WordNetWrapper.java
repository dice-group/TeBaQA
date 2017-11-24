package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordNetWrapper {
    private static IDictionary dictionary = null;

    private static Logger log = Logger.getLogger(WordNetWrapper.class);

    public WordNetWrapper() {
        try {
            initDictionary();
        } catch (Exception e) {
            log.error("ERROR: Unable to initiate wordnet dictionary!", e);
        }
    }

    public Set<String> lookUpWords(String s) {
        Set<String> synonyms = new HashSet<>();
        List<IIndexWord> indexedWords = createIndexedWords(s);
        indexedWords.forEach(iIndexWord -> {
            List<IWordID> wordIDs = iIndexWord.getWordIDs();
            wordIDs.forEach(iWordID -> {
                IWord word = dictionary.getWord(iWordID);
                ISynset synset = word.getSynset();
                for (IWord w : synset.getWords()) {
                    synonyms.add(w.getLemma().replace("_", " "));
                }
            });

        });
        return synonyms;
    }

    private static void initDictionary() throws Exception {
        String wordnetPath = "./src/main/resources/dict/";
        if (dictionary == null) {
            dictionary = new Dictionary(new URL("file", null, wordnetPath));
            dictionary.open();
        }
    }

    private List<IIndexWord> createIndexedWords(String s) {
        List<IIndexWord> result = new ArrayList<>();
        Annotation document = new Annotation(s);
        StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                if (lemma != null && pos != null && pos.length() > 0 && ner != null && !ner.equals("PERSON")
                        && !ner.equals("ORGANIZATION") && !ner.equals("LOCATION")) {
                    char posTag = Character.toUpperCase(pos.charAt(0));
                    if (Character.compare(posTag, 'N') == 0 || Character.compare(posTag, 'V') == 0
                            || Character.compare(posTag, 'R') == 0 || Character.compare(posTag, 'S') == 0
                            || Character.compare(posTag, 'A') == 0) {
                        POS posShort = POS.getPartOfSpeech(posTag);
                        IIndexWord idxWord = dictionary.getIndexWord(lemma, posShort);
                        if (idxWord != null) {
                            result.add(idxWord);
                        }
                    }
                }
            }
        }
        return result;
    }
}
