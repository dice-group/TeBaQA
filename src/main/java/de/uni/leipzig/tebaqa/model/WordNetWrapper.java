package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.helper.PosTransformation;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.util.WordSimilarityCalculator;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import weka.core.Stopwords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, String> lookUpWords(String s) {
        Map<String, String> synonyms = new HashMap<>();
        List<IIndexWord> indexedWords = createIndexedWords(s);
        indexedWords.forEach(iIndexWord -> {
            List<IWordID> wordIDs = iIndexWord.getWordIDs();
            wordIDs.forEach(iWordID -> {
                IWord word = dictionary.getWord(iWordID);
                List<IWord> words = word.getSynset().getWords();
                for (IWord w : words) {
                    String replace = w.getLemma().replace("_", " ");
                    synonyms.put(replace, iWordID.getLemma());
                }
            });

        });
        return synonyms;
    }

    private static void initDictionary() throws Exception {
        if (dictionary == null) {
            dictionary = new Dictionary(new ClassPathResource("dict/").getFile());
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
                if (lemma != null && pos != null && pos.length() > 0 && ner != null
                        && !ner.equals("PERSON") && !ner.equals("ORGANIZATION") && !ner.equals("LOCATION")
                        && !lemma.equalsIgnoreCase("be")
                        && !pos.equals("WP") && !pos.equals("WRB") && !pos.equals(".")) {
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

    public double semanticWordSimilarity(String word1, String word2) {
        if (Stopwords.isStopword(word1) || Stopwords.isStopword(word2)) {
            return 0.0;
        }

        WordSimilarityCalculator wordSimilarityCalculator = new WordSimilarityCalculator();
        return wordSimilarityCalculator.calcRelatednessOfWords(word1, word2, new HirstStOnge(new NictWordNet()));
    }

    public double semanticSimilarityBetweenWordgroupAndWord(String entity, String word2) {
        Double highestSimilarity = 0.0;

        if(Stopwords.isStopword(word2)){
            return 0.0;
        }
        //Regex (?=\p{Lu}) finds uppercase letters. E.g. "bodyOfWater" -> ["body", "Of", "Water"]
        String[] words = entity.split("(?=\\p{Lu})");
        for (String word : words) {
            edu.cmu.lti.jawjaw.pobj.POS pos = PosTransformation.transform(SemanticAnalysisHelper.getPOS(word).getOrDefault(word, ""));
            if (pos != null) {
                double similarity = semanticWordSimilarity(word, word2);
                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                }
            }
        }
        return highestSimilarity;
    }
}
