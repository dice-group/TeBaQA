package de.uni.leipzig.tebaqa.tebaqacontroller.utils;

import com.google.common.collect.Lists;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.SPARQLResultSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.ResultBinding;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SPARQLUtilities {
    public final static String LABEL_SPARQL = "SELECT ?label  WHERE { <%1$s> <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER(lang(?label) = \"en\")}";
    public final static String IMAGE_SPARQL = "SELECT ?thumbnail WHERE { <%1$s> <http://dbpedia.org/ontology/thumbnail> ?thumbnail . }";
    public final static String WIKI_LINK_SPARQL = "SELECT ?primaryTopic WHERE { <%1$s> <http://xmlns.com/foaf/0.1/isPrimaryTopicOf> ?primaryTopic  . }";
    public final static String DESCRIPTION_SPARQL = "SELECT ?description WHERE { <%1$s> <http://purl.org/dc/terms/description> ?description . FILTER(lang(?description)=\"en\") }";
    public final static String ABSTRACT_SPARQL = "SELECT ?abstract WHERE { <%1$s> <http://dbpedia.org/ontology/abstract> ?abstract .  FILTER(lang(?abstract)=\"en\")  }";
    private final static String GET_REDIRECTS_SPARQL = "SELECT ?redirectsTo WHERE { <%1$s> <http://dbpedia.org/ontology/wikiPageRedirects> ?redirectsTo }";
    static final String FULLTEXT_SEARCH_SPARQL = "SELECT DISTINCT ?s ?label WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label . FILTER (lang(?label) = 'en'). ?label <bif:contains> \"'%s'\" . ?s <http://purl.org/dc/terms/subject> ?sub }";
    private static Pattern SPLIT_TRIPLE_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
    private static Logger log = Logger.getLogger(SPARQLUtilities.class);
    public static int QUERY_TYPE_UNKNOWN = -1;
    public static int ASK_QUERY = 1;
    public static int SELECT_QUERY = 2;
    public static int SELECT_SUPERLATIVE_ASC_QUERY = 3;
    public static int SELECT_SUPERLATIVE_DESC_QUERY = 4;
    public static int SELECT_COUNT_QUERY = 5;


    /**
     * Determines the type of a SPARQL query.
     *
     * @param q The SPARQL query.
     * @return 0 for a ASK query, 1 for a SELECT query and -1 for everything else.
     */
    public static int getQueryType(String q) {
        Query query = QueryFactory.create(q);
        if (query.isAskType()) {
            return ASK_QUERY;
        } else if (query.isSelectType()) {
            return SELECT_QUERY;
        } else {
            return QUERY_TYPE_UNKNOWN;
        }
    }

    public static List<SPARQLResultSet> executeSPARQLQuery(String sparlQuery) {
        List<SPARQLResultSet> results = new ArrayList<>();
        int resultType;
        List<String> result = new ArrayList<>();
        ParameterizedSparqlString qs = new ParameterizedSparqlString(sparlQuery);
        if (sparlQuery.contains("<^")) {
            log.error("ERROR: Invalid SPARQL Query: " + sparlQuery);
            return results;
        } else {
            Query query;
            try {
                query = qs.asQuery();
            } catch (QueryParseException e) {
                log.error("QueryParseException: Unable to parse query: " + qs, e);
                return results;
            }
            QueryExecution qe = QueryExecutionFactory.sparqlService("http://limbo-triple.cs.upb.de:3030/limbo/query", query);
//            QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
            qe.setTimeout(10000, 10000);
            boolean isAskType = query.isAskType();
            boolean isSelectType = query.isSelectType();

            if (isSelectType) {
                ResultSet rs;
                try {
                    rs = ResultSetFactory.copyResults(qe.execSelect());
                } catch (QueryExceptionHTTP e) {
                    log.error("HTTP Exception while executing SPARQL query: " + sparlQuery, e);
                    return results;
                }
                while (rs.hasNext()) {
                    QuerySolution s = rs.nextSolution();
                    //String message;
                    try {
                        Iterator<String> varNames = s.varNames();
                        for (Iterator<String> it = varNames; it.hasNext(); ) {
                            String varName = it.next();
                            result.add(s.get(varName).toString());
                        }
                        //message = String.valueOf(s.getResource(resultVariableName));
                    } catch (ClassCastException e) {
                        log.error("Unable to parse response! SPARQL: " + sparlQuery, e);
                        return results;
                    }
                    //log.info(String.join("; ", message));
                    //result.add(message);
                }
                if (result.size() > 1) {
                    boolean listIsMixed = result.parallelStream().anyMatch(s -> !isResource(s));
                    if (listIsMixed) {
                        Set<String> dates = result.parallelStream().filter(SPARQLUtilities::isDateFromXMLScheme).collect(Collectors.toSet());
                        if (dates.size() > 0) {
                            SPARQLResultSet dateResult = new SPARQLResultSet(Lists.newArrayList(dates), SPARQLResultSet.DATE_ANSWER_TYPE);
                            results.add(dateResult);
                        }

                        Set<String> numbers = result.parallelStream().filter(SPARQLUtilities::isNumberFromXMLScheme).collect(Collectors.toSet());
                        if (numbers.size() > 0) {
                            SPARQLResultSet numberResult = new SPARQLResultSet(Lists.newArrayList(numbers), SPARQLResultSet.NUMBER_ANSWER_TYPE);
                            results.add(numberResult);
                        }

                        Set<String> resources = result.parallelStream().filter(SPARQLUtilities::isResource).collect(Collectors.toSet());
                        if (resources.size() > 0) {
                            SPARQLResultSet resourceResult;
                            if (resources.size() > 1) {
                                resourceResult = new SPARQLResultSet(Lists.newArrayList(resources), SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE);
                            } else {
                                resourceResult = new SPARQLResultSet(Lists.newArrayList(resources), SPARQLResultSet.SINGLE_ANSWER);
                            }
                            results.add(resourceResult);
                        }

                        Set<String> strings = result.parallelStream().filter(s -> !isResource(s) && !isDateFromXMLScheme(s) && !isResource(s) || isStringFromXMLScheme(s)).collect(Collectors.toSet());
                        if (strings.size() > 0) {
                            SPARQLResultSet stringResult = new SPARQLResultSet(Lists.newArrayList(strings), SPARQLResultSet.SINGLE_ANSWER);
                            results.add(stringResult);
                        }
                    } else {
                        resultType = SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
                        results.add(new SPARQLResultSet(result, resultType));
                    }
                } else if (result.size() == 1) {
                    String s = result.get(0);
                    //Check for scientific numbers like 3.40841e+10
                    if (isNumericOrScientific(s)) {
                        resultType = SPARQLResultSet.NUMBER_ANSWER_TYPE;
                    } else if (isNumberFromXMLScheme(s)) {
                        resultType = SPARQLResultSet.NUMBER_ANSWER_TYPE;
                    } else if (isDateFromXMLScheme(s)) {
                        resultType = SPARQLResultSet.DATE_ANSWER_TYPE;
                    } else if (isResource(s)) {
                        resultType = SPARQLResultSet.SINGLE_ANSWER;
                    } else {
                        resultType = SPARQLResultSet.SINGLE_ANSWER;
                    }
                    results.add(new SPARQLResultSet(result, resultType));
                } else {
                    resultType = SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
                    results.add(new SPARQLResultSet(result, resultType));
                }
            } else if (isAskType) {
                try {
                    boolean rs = qe.execAsk();
                    result.add(String.valueOf(rs));
                    resultType = SPARQLResultSet.BOOLEAN_ANSWER_TYPE;
                    results.add(new SPARQLResultSet(result, resultType));
                } catch (Exception e) {
                    log.error("HTTP Exception while creating query: " + sparlQuery, e);
                    //throw e;
                }
            } else {
                log.error("Unknown query type: " + sparlQuery);
            }
        }

        //if (!result.isEmpty()) {
        //log.info("Result: " + Strings.join(result, "; "));
        //}
        return results;
    }
    private static String restoreQuery(ResultsetBinding binding, String originalQuery){
        Query query=QueryFactory.create(originalQuery);
        ElementTriplesBlock block = new ElementTriplesBlock();

        ElementWalker.walk(query.getQueryPattern(),
                new ElementVisitorBase() {
                    public void visit(ElementPathBlock el) {
                        Iterator<TriplePath> triples = el.patternElts();
                        while (triples.hasNext()) {
                            Node subject;
                            Node predicate;
                            Node object;
                            TriplePath t = triples.next();
                            if (t.getSubject().toString().startsWith("?var_"))
                                subject = NodeFactory.createURI(binding.getBindings().get(t.getSubject().getName()));
                            else subject = t.getSubject();
                            if (t.getPredicate().toString().startsWith("?var_"))
                                predicate = NodeFactory.createURI(binding.getBindings().get(t.getPredicate().getName()));
                            else predicate = t.getPredicate();
                            if (t.getObject().toString().startsWith("?var_")) {
                                object = NodeFactory.createURI(binding.getBindings().get(t.getObject().getName()));
                            }else object = t.getObject();
                            block.addTriple(new Triple(subject,predicate,object));
                            //Var uri =Var.alloc(t.getPredicate());
                            //vars.add(uri);
                            //resourceLinker.mappedProperties.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                        }

                    }



                }
        );
        query.setQueryPattern(block);

        return query.toString();

    }
    public static ResultsetBinding executeQuery(String queryString){
        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.sparqlService("http://limbo-triple.cs.upb.de:3030/limbo/query", query);
//        QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
        ResultsetBinding b=new ResultsetBinding();
        if (query.isSelectType()){

            ResultSet rs=qe.execSelect();
            while(rs.hasNext()){
                RDFNode v=rs.nextSolution().get(query.getResultVars().get(0));
                if(v.isLiteral())
                    b.addResult(v.asLiteral().getLexicalForm());
                else b.addResult(v.toString());
            }
        }

        else{
            boolean res=qe.execAsk();
            if(res)b.addResult("true");
            else b.addResult("false");
        }
        b.setQuery(queryString);
        b.setAnswerType(determineAnswerType(b));
        return b;
    }
    public static List<ResultsetBinding> retrieveBinings(String queryWithValues,String pattern) {
        String sparlQuery = replaceWithWildcard(queryWithValues);
        ParameterizedSparqlString qs = new ParameterizedSparqlString(sparlQuery);
        boolean isAskType;
        List<ResultsetBinding> mergedBindings = new ArrayList<>();

        if (sparlQuery.contains("<^")) {
            log.error("ERROR: Invalid SPARQL Query: " + sparlQuery);
        } else {
            Query query = new Query();
            try {
                query = qs.asQuery();
            } catch (QueryParseException e) {
                log.error("QueryParseException: Unable to parse query: " + qs, e);
            }
            Optional<String>orderVar=Optional.empty();
            if(query.getOrderBy()!=null)
                orderVar=Optional.of(query.getOrderBy().get(0).expression.getExprVar().asVar().getVarName());
                isAskType = QueryFactory.create(queryWithValues).isAskType();
            boolean isCountQuery = isCountQuery(queryWithValues);
            boolean isLiteralResult=false;
//            QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
            QueryExecution qe = QueryExecutionFactory.sparqlService("http://limbo-triple.cs.upb.de:3030/limbo/query", query);
            qe.setTimeout(10000, 10000);
            if (query.isSelectType()) {
                ResultSet rs = null;
                try {
                    rs = ResultSetFactory.copyResults(qe.execSelect());
                    qe.close();
                } catch (QueryExceptionHTTP e) {
                    log.error("HTTP Exception while executing SPARQL query: " + sparlQuery, e);
                }



                while (rs!=null&&rs.hasNext()) {
                    boolean isValidOrderedByNumericValue=true;
                    isAskType = QueryFactory.create(queryWithValues).isAskType();
                    Set<String>results=new HashSet<>();
                    Map<String, String>placeHolderBinding=new HashMap<>();
                    ResultBinding s = (ResultBinding) rs.nextSolution();
                    Map<String, Node> binding = getBinding(s);
                    for (String variable : binding.keySet()) {
                        if(variable.startsWith("var_")) {
                            placeHolderBinding.put(variable, binding.get(variable).toString());
                        }
                        if(orderVar.isPresent()&&variable.equals(orderVar.get())){
                            Node v=binding.get(variable);
                            if(v.isLiteral()&&!v.getLiteral().toString().contains("[a-zA-Z]+")) {
                                isValidOrderedByNumericValue = false;
                                System.out.println("---------ValidOrderedByNumericValue was set------");
                            }

                        }
                        else if ("uri".equals(variable)) {
                            Node v=binding.get(variable);
                            if(v.isLiteral()){
                                isLiteralResult=true;
                                results.add(binding.get(variable).getLiteralLexicalForm());
                            }
                            else results.add(binding.get(variable).toString());
                        }
                    }
                    boolean alreadyKnown=false;
                    for(ResultsetBinding bind:mergedBindings){
                        if(bind.isSameBinding(placeHolderBinding)) {
                            if(!isAskType) bind.getResult().addAll(results);

                            alreadyKnown=true;
                        }

                    }
                    if(isValidOrderedByNumericValue&&
                            !alreadyKnown&&!hasDoubleBoundResource(placeHolderBinding)&&
                            !(isLiteralResult&&isCountQuery)){
                        ResultsetBinding b=new ResultsetBinding();
                        b.getBindings().putAll(placeHolderBinding);
                        if(isAskType){
                            if(results.size()>0) b.addResult("true");
                            else b.addResult("false");

                        }
                        else b.getResult().addAll(results);
                        b.setQuery(restoreQuery(b,pattern));
                        mergedBindings.add(b);
                    }

                }
                /*if(isCountQuery&&isLiteralResult){
                    mergedBindings.clear();
                    return mergedBindings;
                }*/
                if(isCountQuery)mergedBindings.forEach(bind->{
                    String result=""+bind.getResult().size();
                    bind.getResult().clear();
                    bind.addResult(result);
                });
                mergedBindings.forEach(bin->bin.setAnswerType(determineAnswerType(bin)));

            }
        }



        return mergedBindings;
    }
    private static boolean hasDoubleBoundResource(Map<String, String>placeHolderBinding){
        HashSet found=new HashSet();
        for(String key:placeHolderBinding.keySet()){
            if(!found.contains(placeHolderBinding.get(key)))
                found.add(placeHolderBinding.get(key));
            else return true;

        }
        return false;
    }
    private static boolean isCountQuery(String query) {
        return TextUtilities.trim(query.toLowerCase()).startsWith("select (count");
    }

    static String transformCountToStarQuery(String s) {
        s = TextUtilities.trim(s);
        return "SELECT * " + s.substring(s.toLowerCase().indexOf(" where"), s.length());

    }

    private static String addUriResultVariable(String s) {
        return s.replaceFirst("SELECT ", "SELECT ?uri ");
    }

    static int determineAnswerType(ResultsetBinding rs) {
        Set<String> result = rs.getResult();

        if(result.isEmpty())
            return SPARQLResultSet.UNKNOWN_ANSWER_TYPE;

        //Check for scientific numbers like 3.40841e+10
        if (result.stream().allMatch(SPARQLUtilities::isNumericOrScientific)) {
            return SPARQLResultSet.NUMBER_ANSWER_TYPE;
        } else if (result.stream().allMatch(SPARQLUtilities::isNumberFromXMLScheme)) {
            return SPARQLResultSet.NUMBER_ANSWER_TYPE;
        } else if (result.stream().allMatch(SPARQLUtilities::isDateFromXMLScheme)) {
            return SPARQLResultSet.DATE_ANSWER_TYPE;
        } else if (result.size() == 1) {
            final String s = result.stream().findFirst().get();
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                return SPARQLResultSet.BOOLEAN_ANSWER_TYPE;
            } else {
                return SPARQLResultSet.SINGLE_ANSWER;
            }
        } else if (result.size() > 1 && (result.stream().allMatch(SPARQLUtilities::isResource) || result.stream().noneMatch(SPARQLUtilities::isResource))) {
            return SPARQLResultSet.LIST_OF_RESOURCES_ANSWER_TYPE;
        } else {
            return SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
        }
    }

    static String replaceWithWildcard(String sparlQuery) {
        int endPosition = sparlQuery.toLowerCase().indexOf("where");
        sparlQuery = "SELECT * " + sparlQuery.substring(endPosition, sparlQuery.length());
        return sparlQuery;
    }

    public static String rebuildSeperatedQuery(Map<String, String> bindings, String query) {
        query = removeValueAndFilterStatementsFromQuerie(query);
        for (String variable : bindings.keySet()) {
            if (!"uri".equals(variable)) {
                query = query.replace("?" + variable, "<" + bindings.get(variable) + ">");
            }
        }

        return query;
    }

    private static String removeValueAndFilterStatementsFromQuerie(String query) {
        String valueStatement = "VALUES ";
        boolean containsValueStatements = query.contains(valueStatement);
        while (containsValueStatements) {
            int startPosition = query.indexOf(valueStatement);
            int endPosition = query.substring(startPosition, query.length() - 1).indexOf("}") + 1 + startPosition;
            String toRemove = query.substring(startPosition, endPosition);
            query = query.replace(toRemove, "");
            if (!query.contains(valueStatement)) {
                containsValueStatements = false;
            }
        }
        String filterStatement = "FILTER (CONCAT( ";
        boolean containsFilterStatements = query.contains(filterStatement);
        while (containsFilterStatements) {
            int startPosition = query.indexOf(filterStatement);
            //The + 3 is from the double closing brackets and the space " ))"
            int endPosition = query.substring(startPosition, query.length() - 1).indexOf(" ))") + 3 + startPosition;
            String toRemove = query.substring(startPosition, endPosition);
            query = query.replace(toRemove, "");
            if (!query.contains(filterStatement)) {
                containsFilterStatements = false;
            }
        }
        return TextUtilities.trim(query);
    }


    private static Map<String, Node> getBinding(ResultBinding s) {
        Map<String, Node> bindings = new HashMap<>();
        Binding binding = s.getBinding();
        Iterator<Var> vars = binding.vars();
        while (vars.hasNext()) {
            Var next = vars.next();
            Node node = binding.get(next);
            //if (node.isURI()) {
            bindings.put(next.getVarName(), node);
            /*} else if (node.isLiteral()) {
                bindings.put("uri", node.getLiteral().toString());
            }*/
        }
        return bindings;
    }

    public static boolean isResource(String s) {
        return s.toLowerCase().startsWith("http:") || s.toLowerCase().startsWith("urn:");
    }

    public static boolean isNumericOrScientific(String s) {
        return NumberUtils.isNumber(s) || (Character.isDigit(s.charAt(0))
                && Character.isDigit(s.charAt(s.length() - 1)) && (s.toLowerCase().contains("e+") || s.toLowerCase().contains("e-")));
    }

    public static boolean isDateFromXMLScheme(String s) {
        return s.endsWith("^^http://www.w3.org/2001/XMLSchema#date") || s.endsWith("^^http://www.w3.org/2001/XMLSchema#gYear") || isDate(s);
    }

    public static boolean isDate(String s) {
        if (s.length() == 4 && StringUtils.isNumeric(s)) {
            try {
                if (Integer.parseInt(s) < 3000) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        } else if (s.length() >= 6 && s.length() <= 10) {
            if (s.contains("-")) {
                String possibleDateWithoutHyphen = s.replace("-", "");
                try {
                    Integer.parseInt(possibleDateWithoutHyphen);
                    return true;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    public static boolean isStringFromXMLScheme(String s) {
        return s.endsWith("^^http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
    }

    public static boolean isNumberFromXMLScheme(String s) {
        return s.endsWith("^^http://www.w3.org/2001/XMLSchema#integer")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#positiveInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#float")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#decimal")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#nonPositiveInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#negativeInteger")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#int")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#short")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#long")
                || s.endsWith("^^http://www.w3.org/2001/XMLSchema#double");
    }

    static boolean isDBpediaEntity(String s) {
        if (s.contains("^^")) {
            return false;
        } else {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("ASK { VALUES (?r) {(<%s>)} {?r ?p ?o} UNION {?s ?r ?o} UNION {?s ?p ?r} }", s));
            if (sparqlResultSets.size() == 1) {
                List<String> result = sparqlResultSets.get(0).getResultSet();
                return result.size() == 1 && Boolean.valueOf(result.get(0));
            } else {
                return false;
            }
        }
    }

    /**
     * Resolves all namespaces in a sparql query.
     *
     * @param query The query with namespaces
     * @return The given query where all namespaces are replaced with their full URI.
     */
    public static String resolveNamespaces(String query) {
        if (!query.toLowerCase().contains("prefix")) {
            return query;
        } else {
            Query q;
            try {
                q = QueryFactory.create(query);
            } catch (QueryException e) {
                log.error("QueryException: Unable to parse query to remove : " + query);
                return "";
            }
            Map<String, String> nsPrefixMap = q.getPrefixMapping().getNsPrefixMap();
            if (nsPrefixMap.isEmpty()) {
                return query.trim();
            } else {
                String queryLowerCase = query.toLowerCase();
                int startOfQuery = 0;
                if (queryLowerCase.contains("select")) {
                    startOfQuery = queryLowerCase.indexOf("select");
                } else if (queryLowerCase.contains("ask")) {
                    startOfQuery = queryLowerCase.indexOf("ask");
                } else {
                    log.error("Unable to determine query type of query: " + query);
                }
                final String[] queryWithoutPrefix = {query.substring(startOfQuery, query.length())};
                nsPrefixMap.forEach((s, s2) -> queryWithoutPrefix[0] = queryWithoutPrefix[0].replace(s + ":", "<" + s2));
                int startPosition = -1;
                int endPosition = -1;
                for (int i = 0; i < queryWithoutPrefix[0].length(); i++) {
                    if (queryWithoutPrefix[0].charAt(i) == '<' && i + 1 < queryWithoutPrefix[0].length()
                            && (queryWithoutPrefix[0].charAt(i + 1) != '?' && queryWithoutPrefix[0].charAt(i + 1) != ' ')) {
                        startPosition = i;
                    } else if ((queryWithoutPrefix[0].charAt(i) == ' '
                            || queryWithoutPrefix[0].charAt(i) == ';')
                            && (i > 0 && queryWithoutPrefix[0].charAt(i - 1) != '>')) {
                        endPosition = i;
                    } else if (queryWithoutPrefix[0].charAt(i) == '.'
                            && i < queryWithoutPrefix[0].length()
                            && (queryWithoutPrefix[0].charAt(i + 1) == ' '
                            || queryWithoutPrefix[0].charAt(i + 1) == '\n'
                            || queryWithoutPrefix[0].charAt(i + 1) == '}')) {
                        endPosition = i;
                    } else if (i > 0 && queryWithoutPrefix[0].charAt(i - 1) == '>') {
                        startPosition = -1;
                    }
                    if (startPosition > 0 && endPosition > 0 && startPosition < endPosition) {
                        queryWithoutPrefix[0] = queryWithoutPrefix[0].replace(queryWithoutPrefix[0].substring(startPosition, endPosition),
                                queryWithoutPrefix[0].substring(startPosition, endPosition) + ">");
                        startPosition = -1;
                        endPosition = -1;
                    }
                }

                String[] split = queryWithoutPrefix[0].replaceAll("\\s+", " ").split(" ");
                for (int i = 0; i < split.length; i++) {
                    if (split[0].startsWith("http://") || split[0].startsWith("https://")) {
                        split[0] = "<" + split[0] + ">";
                    }
                }
                return String.join(" ", split).trim();
            }
        }
    }

    static String createFilterClauses(List<String> triples, Map<String, String> replacements) {
        StringBuilder result = new StringBuilder();
        List<List<String>> triplesSplitted = new ArrayList<>();
        triples.forEach(s -> {
                    Matcher matcher = SPLIT_TRIPLE_PATTERN.matcher(s);
                    List<String> currentTriple = new ArrayList<>();
                    while (matcher.find()) {
                        String group = matcher.group();
                        if (group.startsWith("@")) {
                            String element = currentTriple.get(currentTriple.size() - 1) + group;
                            currentTriple.set(currentTriple.size() - 1, element);
                        } else {
                            if (!group.toLowerCase().startsWith("?") && !group.toLowerCase().startsWith("<")
                                    && !group.toLowerCase().startsWith("'") && !group.toLowerCase().startsWith("\"")) {
                                group = "'" + group + "'";
                            }
                            currentTriple.add(replacements.getOrDefault(group, group));
                        }
                    }
                    triplesSplitted.add(currentTriple);
                }
        );

        List<Map<List<String>, List<String>>> filterClauses = new ArrayList<>();
        triplesSplitted.forEach(currentTriple -> triplesSplitted.forEach(otherTriple -> {
            if (!currentTriple.equals(otherTriple)) {
                Map<List<String>, List<String>> filterClause = new HashMap<>();
                filterClause.put(currentTriple, otherTriple);
                filterClauses.add(filterClause);
            }
        }));

        filterClauses.forEach((filterMap) -> {
            Optional<Entry<List<String>, List<String>>> any = filterMap.entrySet().parallelStream().findAny();
            if (any.isPresent()) {
                Entry<List<String>, List<String>> filterMapping = any.get();
                List<String> triple1 = filterMapping.getKey();
                List<String> triple2 = filterMapping.getValue();

                if (triple1.size() == 3 && triple2.size() == 3) {
                    result.append(String.format(" FILTER (CONCAT( %s, %s, %s ) != CONCAT( %s, %s, %s )) ",
                            triple1.get(0), triple1.get(1), triple1.get(2), triple2.get(0), triple2.get(1), triple2.get(2)));
                } else {
                    log.error(String.format("ERROR: Unable to generate FILTER statements because the triples don't have " +
                            "exactly 3 parts! current triple: {%s}; next triple: {%s}", String.join(" ", triple1), String.join(" ", triple2)));
                }
            }
        });
        return result.toString();

    }

    /**
     * Fetches the page rank for a given resource. If the resource isn't valid (e.g. if it's a ontology instead), {@link Double}.MAX_VALUE is returned instead.
     *
     * @param uri The resource which page rank is returned.
     * @return The page rank of the resource.
     */
    public static Double getPageRank(String uri) {
        if (isResource(uri)) {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("PREFIX vrank:<http://purl.org/voc/vrank#> SELECT ?v FROM <http://people.aifb.kit.edu/ath/#DBpedia_PageRank> WHERE { <%s> vrank:hasRank/vrank:rankValue ?v. }", uri));
            List<String> resultSet = new ArrayList<>();
            sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
            if (resultSet.size() == 1) {
                return Double.valueOf(resultSet.get(0).split("\\^")[0]);
            } else {
                return Double.MAX_VALUE;
            }
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static Double countOntologyUsage(String uri) {
        String[] split = uri.split("/");
        String entity = split[split.length - 1];
        if (uri.startsWith("http://dbpedia.org/ontology/") && Character.isUpperCase(entity.charAt(0))) {
            List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format("SELECT DISTINCT (COUNT(DISTINCT ?uri) as ?c) WHERE {  ?uri a <%s> . } ", uri));
            List<String> resultSet = new ArrayList<>();
            sparqlResultSets.forEach(sparqlResultSet -> resultSet.addAll(sparqlResultSet.getResultSet()));
            if (resultSet.size() == 1) {
                return Double.valueOf(resultSet.get(0).split("\\^")[0]);
            } else {
                return Double.MAX_VALUE;
            }
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static String getRedirect(String resource) {
        List<SPARQLResultSet> sparqlResultSets = executeSPARQLQuery(String.format(GET_REDIRECTS_SPARQL, resource));
        if (sparqlResultSets.size() >= 1) {
            List<String> resultSet = sparqlResultSets.get(0).getResultSet();
            if (resultSet.size() > 0) {
                //There should be only one redirection
                return resultSet.get(0);
            }
        }
        return resource;
    }

    public static String getBaseNameFromDBpediaEntitiy(String uri) {
        String[] split;
        if (uri.startsWith("http://dbpedia.org/resource/")) {
            split = uri.split("http://dbpedia.org/resource/");
        } else if (uri.startsWith("http://dbpedia.org/property/")) {
            split = uri.split("http://dbpedia.org/property/");
        } else if (uri.startsWith("http://dbpedia.org/ontology/")) {
            split = uri.split("http://dbpedia.org/ontology/");
        } else if (uri.contains("/")) {
            split = uri.split("/");
        } else {
            return uri;
        }
        return split[split.length - 1].replace("_", " ");
    }
}
