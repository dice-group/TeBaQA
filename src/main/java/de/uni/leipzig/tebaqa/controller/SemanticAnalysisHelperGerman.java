package de.uni.leipzig.tebaqa.controller;

import com.clarkparsia.pellet.sparqldl.model.Core;
import de.uni.leipzig.tebaqa.analyzer.Analyzer;
import de.uni.leipzig.tebaqa.helper.*;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import de.uni.leipzig.tebaqa.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.model.SPARQLResultSet;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.log4j.Logger;
import org.apache.lucene.search.spans.SpanQuery;
import sun.awt.SunHints;
import weka.classifiers.Classifier;
import weka.core.Instance;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.uni.leipzig.tebaqa.helper.HypernymMappingProvider.getHypernymMapping;
import static de.uni.leipzig.tebaqa.helper.TextUtilities.NON_WORD_CHARACTERS_REGEX;
import static de.uni.leipzig.tebaqa.helper.Utilities.ARGUMENTS_BETWEEN_SPACES;
import static de.uni.leipzig.tebaqa.helper.Utilities.getLevenshteinRatio;

public class SemanticAnalysisHelperGerman extends SemanticAnalysisHelper {

        private static Logger log = Logger.getLogger(de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelperGerman.class);
        //private StanfordCoreNLP pipeline;
        private static DateTimeFormatter dateTimeFormatterLong = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static DateTimeFormatter dateTimeFormatterShortMonth = DateTimeFormatter.ofPattern("yyyy-M-dd");
        private static DateTimeFormatter dateTimeFormatterShortDay = DateTimeFormatter.ofPattern("yyyy-MM-d");


        public SemanticAnalysisHelperGerman() {
            super(StanfordPipelineProviderGerman.getSingletonPipelineInstance());
//            super(StanfordPipelineProviderGerman.getPipelineInstanceForAnnotators(StanfordPipelineProviderGerman.ANNOTATORS_WITHOUT_DEPPARSE));

            //this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
        }
        @Override
        public int determineQueryType(String q) {
            //List<String> selectIndicatorsList = Arrays.asList("list|give|show|who|when|were|what|why|whose|how|where|which".split("\\|"));
            //List<String> askIndicatorsList = Arrays.asList("is|are|did|was|does|can".split("\\|"));
            List<String> selectIndicatorsList = Arrays.asList("welche|liste|wie|wo|wann|warum|wessen|gib|in".split("\\|"));
            List<String> askIndicatorsList = Arrays.asList("sind".split("\\|"));
            //log.debug("String question: " + q);
            String[] split = q.split("\\s+");
            List<String> firstThreeWords = new ArrayList<>();
            if (split.length > 3) {
                firstThreeWords.addAll(Arrays.asList(split).subList(0, 3));
            } else {
                firstThreeWords.addAll(Arrays.asList(split));
            }
            if (hasAscAggregation(q)) {
                return SPARQLUtilities.SELECT_SUPERLATIVE_ASC_QUERY;
            } else if (hasDescAggregation(q)) {
                return SPARQLUtilities.SELECT_SUPERLATIVE_DESC_QUERY;
            } else if (hasCountAggregation(q)) {
                return SPARQLUtilities.SELECT_COUNT_QUERY;
            } else if (firstThreeWords.parallelStream().anyMatch(s -> selectIndicatorsList.contains(s.toLowerCase()))) {
                return SPARQLUtilities.SELECT_QUERY;
            } else if (firstThreeWords.parallelStream().anyMatch(s -> askIndicatorsList.contains(s.toLowerCase()))) {
                return SPARQLUtilities.ASK_QUERY;
            } else {
                return SPARQLUtilities.QUERY_TYPE_UNKNOWN;
            }
        }
        /*@Override
        public Annotation annotate(String text) {
            Annotation annotation = new Annotation(text);
            pipeline.annotate(annotation);
            return annotation;
        }*/



        /*@Override
        public SemanticGraph extractDependencyGraph(String text) {
            Annotation annotation = new Annotation(text);
            pipeline.annotate(annotation);

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences.size() > 1) {
                log.error("There is more than one sentence to analyze: " + text);
            }
            CoreMap sentence = sentences.get(0);
            Set<Dependency<Label, Label, Object>> a=sentence.get( TreeCoreAnnotations.TreeAnnotation.class).dependencies();
            SemanticGraph dependencyGraph= SemanticGraphFactory.generateEnhancedDependencies(sentence.get( TreeCoreAnnotations.TreeAnnotation.class));
            //SemanticGraph dependencyGraph = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);

            return dependencyGraph;
        }*/




        public Map<String, String> getLemmas(String q) {
            if (q.isEmpty()) {
                return new HashMap<>();
            }
            Map<String, String> lemmas = new HashMap<>();
            Annotation annotation = new Annotation(q);
            Properties props = new Properties();
            try {
                props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            props.remove("annotators");
            props.setProperty("annotators","tokenize,ssplit,pos,ner,parse,lemma");
//            props.setProperty("annotators","tokenize,ssplit,pos,ner,parse,lemma,depparse");
            StanfordCoreNLP pipeline= new StanfordCoreNLP(props);
            //StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
            pipeline.annotate(annotation);
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
                for (CoreLabel token : labels) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                    lemmas.put(word, lemma);
                }
            }
            return lemmas;
        }
        private static class SortbyStartPosition implements Comparator<Marking>
        {
            public int compare(Marking a, Marking b) {
                System.out.println(((NamedEntity) a).getStartPosition() - ((NamedEntity) b).getStartPosition());
                return ((NamedEntity) a).getStartPosition() - ((NamedEntity) b).getStartPosition();
            }
        }

        public Map<String, String> getPOS(String q) {
            if (q.isEmpty()) {
                return new HashMap<>();
            }
            Map<String, String> pos = new HashMap<>();
            Annotation annotation = new Annotation(q);
            Properties props = new Properties();
            try {
                props.load(IOUtils.readerFromString("StanfordCoreNLP-german.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            props.remove("annotators");
            props.setProperty("annotators","tokenize,ssplit,pos,ner,parse,lemma,depparse");
            StanfordCoreNLP pipeline= new StanfordCoreNLP(props);
            //StanfordCoreNLP pipeline = StanfordPipelineProvider.getSingletonPipelineInstance();
            pipeline.annotate(annotation);
            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            for (CoreMap sentence : sentences) {
                List<CoreLabel> labels = sentence.get(CoreAnnotations.TokensAnnotation.class);
                for (CoreLabel token : labels) {
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    String posAnnotation = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    pos.put(word, posAnnotation);
                }
            }
            return pos;
        }

        /**
         * Checks if a given sentence uses superlatives like first, least and so on which are indicators for aggregation queries.
         *
         * @param sentence A string which contains a sentence.
         * @return If the sentence contains keywords which are used in ascending aggregation queries.
         */
        public boolean hasAscAggregation(String sentence) {
            String[] ascIndicators = new String[]{"erste", "älteste", "kleinste", "tiefste", "kürzeste", "wenigste"};
            String[] words = sentence.split(NON_WORD_CHARACTERS_REGEX);
            return Arrays.stream(words).anyMatch(Arrays.asList(ascIndicators)::contains);
        }

        /**
         * Checks if a given sentence uses superlatives like largest, last, highest and so on which are indicators for aggregation queries.
         *
         * @param sentence A string which contains a sentence.
         * @return If the sentence contains keywords which are used in descending aggregation queries.
         */
        public boolean hasDescAggregation(String sentence) {
            String[] descIndicators = new String[]{"größte", "letzte", "höchste", "meiste", "jüngste", "längste", "schwerste"};
            String[] words = sentence.split(NON_WORD_CHARACTERS_REGEX);
            return Arrays.stream(words).anyMatch(Arrays.asList(descIndicators)::contains);
        }


        private boolean hasCountAggregation(String sentence) {
            return sentence.toLowerCase().trim().startsWith("wie viele");
        }




        /**
         * Checks, if a question is inside a Map.
         *
         * @param map  The map in which the question is not is not.
         * @param text The question text.
         * @return true if the text is inside, false otherwise.
         */
        boolean containsQuestionText(Map<String, String> map, String text) {
            boolean isInside = false;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (entry.getValue().equals(text)) {
                    isInside = true;
                    break;
                }
            }
            return isInside;
        }

        /**
         * Removes all variables, prefixes, newlines, standard keywords like ASK, SELECT, WHERE, DISTINCT.
         *
         * @param queryString The sparql query string.
         * @return A string which only contains sparql modifiers, a '?' as placeholder for a variable and '<>' as
         * placeholders for strings like this: { <> <> ? . ? <> ? FILTER regex( ? , ? ) }
         */
        String cleanQuery(String queryString) {
            Query query = QueryFactory.create(queryString);
            query.setPrefixMapping(null);
            return query.toString().trim()
                    //replace newlines with space
                    .replaceAll("\n", " ")
                    //replace every variable with ?
                    .replaceAll("\\?[a-zA-Z\\d]+", " ? ")
                    //replace every number(e.g. 2 or 2.5) with a ?
                    .replaceAll("\\s+\\d+\\.?\\d*", " ? ")
                    //replace everything in quotes with ?
                    .replaceAll("([\"'])(?:(?=(\\\\?))\\2.)*?\\1", " ? ")
                    //remove everything between <>
                    .replaceAll("<\\S*>", " <> ")
                    //remove all SELECT, ASK, DISTINCT and WHERE keywords
                    .replaceAll("(?i)(select|ask|where|distinct)", " ")
                    //remove every consecutive spaces
                    .replaceAll("\\s+", " ");
        }

        public List<IndexedWord> getDependencySequence(SemanticGraph semanticGraph) {
            IndexedWord firstRoot = semanticGraph.getFirstRoot();
            return getDependenciesFromEdge(firstRoot, semanticGraph);
        }

        private static List<IndexedWord> getDependenciesFromEdge(IndexedWord root, SemanticGraph semanticGraph) {
            final String posExclusion = "DT|IN|WDT|W.*|\\.";
            //final String lemmaExclusion = "haben|ist|viele|liste";
            List<IndexedWord> sequence = new ArrayList<>();
            String rootPos = root.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            //String rootLemma = root.get(CoreAnnotations.LemmaAnnotation.class);
            if (!rootPos.matches(posExclusion) /*&& !rootLemma.matches(lemmaExclusion)*/) {
                sequence.add(root);
            }
            Set<IndexedWord> childrenFromRoot = semanticGraph.getChildren(root);

            for (IndexedWord word : childrenFromRoot) {
                String wordPos = word.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                //String wordLemma = word.get(CoreAnnotations.LemmaAnnotation.class);
                if (!wordPos.matches(posExclusion) /*&& !wordLemma.matches(lemmaExclusion)*/) {
                    sequence.add(word);
                }
                List<IndexedWord> children = semanticGraph.getChildList(word);
                //In some cases a leaf has itself as children which results in endless recursion.
                if (children.contains(root)) {
                    children.remove(root);
                }
                for (IndexedWord child : children) {
                    sequence.addAll(getDependenciesFromEdge(child, semanticGraph));
                }
            }
            return sequence;
        }

        public int detectQuestionAnswerType(String question) {
            //de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper semanticAnalysisHelper = new de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper();
            //HashMap<String,String> posTags = getPosTags(question);
            Pattern pattern = Pattern.compile("\\w+");
            List<CoreLabel>tokens=this.getTokens(question);
            Matcher m = pattern.matcher(question);
            if (m.find()) {
                String first = m.group();
                //if (first.isPresent()) {
                if (first.toLowerCase().matches("ist|hat|haben"))
                        //if (first.get().toLowerCase().matches("be|do|have"))
                    return SPARQLResultSet.BOOLEAN_ANSWER_TYPE;
            }
            //}
            if (question.toLowerCase().startsWith("wie lang") || question.toLowerCase().startsWith("wie viele")|| question.toLowerCase().startsWith("wie hoch")
                    || question.toLowerCase().startsWith("wie schwer") || question.toLowerCase().startsWith("wie breit")||question.toLowerCase().startsWith("wie ist")) {
                return SPARQLResultSet.NUMBER_ANSWER_TYPE;
            }
            if (question.toLowerCase().startsWith("wie") && tokens.size() >= 2) {
                CoreLabel token = tokens.get(1);
                //For cases like how big, how small, ...
                if (token.getString(CoreAnnotations.PartOfSpeechAnnotation.class).startsWith("AD")) {
                    return SPARQLResultSet.NUMBER_ANSWER_TYPE;
                }

                if(question.toLowerCase().startsWith("wie lauten")) {
                    return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
                }

                if(question.toLowerCase().startsWith("wie lautet")) {
                    return SPARQLResultSet.SINGLE_ANSWER;
                }

            }
            if(question.toLowerCase().startsWith("in welchen"))
                return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
            if(question.toLowerCase().startsWith("in welcher"))
                return SPARQLResultSet.SINGLE_ANSWER;
            if (question.toLowerCase().startsWith("wann")) {
                return SPARQLResultSet.DATE_ANSWER_TYPE;
            }
            if (question.toLowerCase().startsWith("welche")&&question.toLowerCase().contains("gibt es")) {
                return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
            }
            if (question.toLowerCase().startsWith("wann beginnen")) {
                return SPARQLResultSet.SINGLE_ANSWER;
            }

            if (question.toLowerCase().startsWith("welche")||question.toLowerCase().startsWith("welcher")||
                    question.toLowerCase().startsWith("gib")) {
                boolean hasPluralNoun = false;
                for (CoreLabel token : tokens) {
                    String posTag = token.getString(CoreAnnotations.PartOfSpeechAnnotation.class);
                    if (posTag.equalsIgnoreCase("NN")) {
                        String nounText = token.getString(CoreAnnotations.OriginalTextAnnotation.class);
                        if (nounText.endsWith("en")) {
                            hasPluralNoun = true;
                            break;
                        }
                    }
                }

                if (hasPluralNoun) {
                    return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
                } else {
                    return SPARQLResultSet.SINGLE_ANSWER;
                }
            }

            return SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
        }




        public List<String> getHypernymsFromWiktionary(String s) {
            Map<String, List<String>> hypernymMapping = getHypernymMapping();
            return hypernymMapping.getOrDefault(s, new ArrayList<>());
        }


        public static long countUpperCase(String s) {
            return s.chars().filter(Character::isUpperCase).count();
        }

        public String removeQuestionWords(String question) {
            List<String> questionWords = Arrays.asList("liste|gib|zeig mir|wer|wo|wann|was|warum|wessen|wie|welche|welches|welcher|ist|sind|hat|war".split("\\|"));
            questionWords.sort(Comparator.reverseOrder());

            for (String questionWord : questionWords) {
                if (question.toLowerCase().startsWith(questionWord)) {
                    return question.substring(questionWord.length(), question.length()).trim();
                }
            }
            return question;
        }


    public ResultsetBinding getBestAnswer(List<ResultsetBinding> results, int expectedAnswerType, boolean forceResult) {
        long answersWithExpectedTypeCount = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).count();

        if(answersWithExpectedTypeCount > 0) {
            List<ResultsetBinding> matchingResults = results.stream().filter(resultsetBinding -> resultsetBinding.getAnswerType() == expectedAnswerType).collect(Collectors.toList());

            // If only one answer of expected type, then return it
            if(answersWithExpectedTypeCount == 1) {
                return matchingResults.get(0);
            }

            // If more than one answer of expected type, then ranking
            if(SPARQLResultSet.NUMBER_ANSWER_TYPE == expectedAnswerType) {
                List<ResultsetBinding> nonZeroCounts = matchingResults.stream().filter(resultsetBinding ->resultsetBinding.getResult().size()==1 && resultsetBinding.getNumericalResultValue() != 0).collect(Collectors.toList());
                if(nonZeroCounts.size() == 0)
                    return matchingResults.get(0);

                Map<Double, List<ResultsetBinding>> resultFrequencies = nonZeroCounts.stream().collect(Collectors.groupingBy(ResultsetBinding::getNumericalResultValue));
                if(resultFrequencies.size() == 1) {
                    return nonZeroCounts.get(0);
                }
                else {
                    // Use those numerical results which appear most frequently
                    int maxFrequency = 0;
                    double valueWithMaxFrequency = 0;
                    for(double key : resultFrequencies.keySet()) {
                        int frequency = resultFrequencies.get(key).size();
                        if(frequency > maxFrequency) {
                            maxFrequency = frequency;
                            valueWithMaxFrequency = key;
                        }
                    }

                    double finalValueWithMaxFrequency = valueWithMaxFrequency;
                    return nonZeroCounts.stream().filter(resultsetBinding -> resultsetBinding.getNumericalResultValue() == finalValueWithMaxFrequency).findFirst().get();
                }

            } else if (SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE == expectedAnswerType) {
                // Ranking criteria: More number of results mean less significant query
                matchingResults.forEach(resultsetBinding -> {
                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
                });
            } else {
                // Ranking criteria: More number of results mean less significant query
                matchingResults.forEach(resultsetBinding -> {
                    resultsetBinding.setRating(resultsetBinding.getRating() / resultsetBinding.getResult().size());
                });
            }
            return matchingResults.stream().max(Comparator.comparingDouble(ResultsetBinding::getRating)).get();

        }
        else {
            return new ResultsetBinding();
        }

    }


        public static void main(String[]args){
            SemanticAnalysisHelper sem=new SemanticAnalysisHelperGerman();
            List<CoreLabel>tok=sem.getTokens("Welche Stadt hat mehr als 5000 Einwohner");
            System.out.println();
        }


}
