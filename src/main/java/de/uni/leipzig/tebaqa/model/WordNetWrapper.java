package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.helper.PosTransformation;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
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
                List<IWord> words = word.getSynset().getWords();
                for (IWord w : words) {
                    String replace = w.getLemma().replace("_", " ");
                    synonyms.add(replace);
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

    public double semanticWordSimilarity(String word1, edu.cmu.lti.jawjaw.pobj.POS posWord1, String word2, edu.cmu.lti.jawjaw.pobj.POS posWord2) {
        ILexicalDatabase db = new NictWordNet();
        RelatednessCalculator rc = new Path(db);
        double maxScore = 0D;
        WS4JConfiguration.getInstance().setMFS(true);
        List<Concept> synsets1 = (List<Concept>) db.getAllConcepts(word1, posWord1.name());
        List<Concept> synsets2 = (List<Concept>) db.getAllConcepts(word2, posWord2.name());
        for (Concept synset1 : synsets1) {
            for (Concept synset2 : synsets2) {
                Relatedness relatedness = rc.calcRelatednessOfSynset(synset1, synset2);
                double score = relatedness.getScore();
                if (score > maxScore) {
                    maxScore = score;
                }
            }
        }
        return maxScore;
    }

    public double semanticWordSimilarity(String entity, String word2, edu.cmu.lti.jawjaw.pobj.POS posWord2) {
        Double highestSimilarity = 0.0;

        if(Stopwords.isStopword(word2)){
            return 0.0;
        }
        //Regex (?=\p{Lu}) finds uppercase letters. E.g. "bodyOfWater" -> ["body", "Of", "Water"]
        List<String> words = Arrays.asList(entity.split("(?=\\p{Lu})"));
        for (String word : words) {
            edu.cmu.lti.jawjaw.pobj.POS pos = PosTransformation.transform(SemanticAnalysisHelper.getPOS(word).getOrDefault(word, ""));
            if (pos != null) {
                double similarity = semanticWordSimilarity(word, pos, word2, posWord2);
                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                }
            }
        }
        return highestSimilarity;
    }
}
