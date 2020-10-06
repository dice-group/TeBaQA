package de.uni.leipzig.tebaqa.template.nlp;

import de.uni.leipzig.tebaqa.tebaqacommons.util.SPARQLUtilities;
import de.uni.leipzig.tebaqa.template.model.Cluster;
import de.uni.leipzig.tebaqa.template.model.CustomQuestion;
import de.uni.leipzig.tebaqa.template.model.QueryTemplateMapping;
import de.uni.leipzig.tebaqa.template.util.Utilities;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticAnalysisHelperEnglish extends SemanticAnalysisHelper {

    private final StanfordCoreNLP pipeline;


    public SemanticAnalysisHelperEnglish() {
        this.pipeline = StanfordPipelineProvider.getSingletonPipelineInstance(StanfordPipelineProvider.Lang.EN);
    }

//    public SemanticAnalysisHelperEnglish(StanfordCoreNLP pipeline) {
//        this.pipeline = pipeline;
//    }

    @Override
    public HashMap<String, String> getPosTags(String text) {
        Annotation annotation = new Annotation(text);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);
        HashMap<String, String> posTags = new HashMap<>();
        for (CoreLabel token : tokens) {
            String value = token.getString(CoreAnnotations.ValueAnnotation.class);
            String pos = token.getString(CoreAnnotations.PartOfSpeechAnnotation.class);
            posTags.put(value, pos);
        }

        return posTags;
    }


    /**
     * Extracts a map of possible query templates and their graph patterns.
     *
     * @param questions The questions which contain a SPARQL query which will be used as template.
     * @return A list which contains SPARQL query templates, divided by their number of entities and classes and by
     * their query type (ASK or SELECT).
     */
    @Override
    public Map<String, QueryTemplateMapping> extractTemplates(List<Cluster> questions, HashMap<String, Set<String>>[] commonTuples) {
        Map<String, QueryTemplateMapping> mappings = new HashMap<>();
        //Set<String> wellKnownPredicates = Sets.union(commonTuples[0].keySet(), commonTuples[1].keySet());
        for (Cluster c : questions) {
            String graph = c.getGraph();
            QueryTemplateMapping mapping = new QueryTemplateMapping();
            // if (c.size() > 10){
            for (CustomQuestion question : c.getQuestions()) {
                String query = question.getQuery();
                //QueryMappingFactoryLabels queryMappingFactory = new QueryMappingFactoryLabels(question.getQuestionText(), query,this);
                String queryPattern = SPARQLUtilities.resolveNamespaces(query);
                queryPattern = queryPattern.replace(" a ", " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ");
                int i = 0;
                String regex = "<(.+?)>";
                Pattern pattern = Pattern.compile(regex);
                Matcher m = pattern.matcher(queryPattern);
                HashMap<String, Integer> mappedUris = new HashMap<>();

                while (m.find()) {
                    String group = m.group();
                    if (!group.contains("^") && !group.contains("http://www.w3.org/2001/XMLSchema")) {
                        //if (!wellKnownPredicates.contains(m.group(1))) {
                        if (!mappedUris.containsKey(Pattern.quote(group)))
                            mappedUris.put(Pattern.quote(group), i);
                        queryPattern = queryPattern.replaceFirst(Pattern.quote(group), "res/" + mappedUris.get(Pattern.quote(group)));
                        i++;
                        //}
                    }
                }
                boolean isSuperlativeDesc = false;
                boolean isSuperlativeAsc = false;
                boolean isCountQuery = false;

                if (queryPattern.toLowerCase().contains("order by desc") && queryPattern.toLowerCase().contains("order by desc") && queryPattern.toLowerCase().contains("limit 1")) {
                    isSuperlativeDesc = true;
                } else if (queryPattern.toLowerCase().contains("order by asc") && queryPattern.toLowerCase().contains("limit 1")) {
                    isSuperlativeAsc = true;
                }
                if (queryPattern.toLowerCase().contains("count")) {
                    isCountQuery = true;
                }

                if (!queryPattern.toLowerCase().contains("http://dbpedia.org/resource/")
                        && !queryPattern.toLowerCase().contains("'")
                        && !queryPattern.toLowerCase().contains("union")
                        && !queryPattern.toLowerCase().contains("sum") && !queryPattern.toLowerCase().contains("avg")
                        && !queryPattern.toLowerCase().contains("min") && !queryPattern.toLowerCase().contains("max")
                        && !queryPattern.toLowerCase().contains("filter") && !queryPattern.toLowerCase().contains("bound")) {
                    int classCnt = 0;
                    int propertyCnt = 0;

                    List<String> triples = Utilities.extractTriples(queryPattern);
                    for (String triple : triples) {
                        Matcher argumentMatcher = Utilities.ARGUMENTS_BETWEEN_SPACES.matcher(triple);
                        int argumentCnt = 0;
                        while (argumentMatcher.find()) {
                            String argument = argumentMatcher.group();
                            if (argument.startsWith("<^") && (argumentCnt == 0 || argumentCnt == 2)) {
                                classCnt++;
                            } else if (argument.startsWith("<^") && argumentCnt == 1) {
                                propertyCnt++;
                            }
                            argumentCnt++;
                        }
                    }

                    int finalClassCnt = classCnt;
                    int finalPropertyCnt = propertyCnt;
                    if (!mapping.getNumberOfProperties().contains(finalPropertyCnt))
                        mapping.getNumberOfProperties().add(finalPropertyCnt);
                    if (!mapping.getNumberOfClasses().contains(finalClassCnt))
                        mapping.getNumberOfClasses().add(finalClassCnt);
                    int queryType = SPARQLUtilities.getQueryType(query);
                    if (isSuperlativeDesc) {
                        mapping.addSelectSuperlativeDescTemplate(queryPattern, question.getQuery());
                    } else if (isSuperlativeAsc) {
                        mapping.addSelectSuperlativeAscTemplate(queryPattern, question.getQuery());
                    } else if (isCountQuery) {
                        mapping.addCountTemplate(queryPattern, question.getQuery());
                    } else if (queryType == SPARQLUtilities.SELECT_QUERY) {
                        mapping.addSelectTemplate(queryPattern, question.getQuery());
                    } else if (queryType == SPARQLUtilities.ASK_QUERY) {
                        mapping.addAskTemplate(queryPattern, question.getQuery());
                    }
                    //create a new mapping class
                    mappings.put(graph, mapping);

                    //log.info(queryPattern);
                }
            }
            //}
        }
        return mappings;
    }
}
