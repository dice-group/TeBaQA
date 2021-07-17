package de.uni.leipzig.tebaqa.template.model;

import de.uni.leipzig.tebaqa.template.util.TextUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Source: https://github.com/AKSW/NLIWOD/blob/master/qa.hawk/src/main/java/org/aksw/hawk/experiment/QueryIsomorphism.java
 */
public class QueryIsomorphism {
    private static final Logger LOGGER = LogManager.getLogger(QueryIsomorphism.class);

    private final List<Cluster> clusters;

    public QueryIsomorphism(Map<String, String> queries) {
        //HashMap<String,Set<String>>predicateToSubjectType=commonPredicates[0];
        //HashMap<String,Set<String>>predicateToObjectType=commonPredicates[1];
        LOGGER.debug("Generating SPARQL Query graphs...");
        clusters = new ArrayList<>();
        HashMap<Graph, Integer> graphs = new HashMap<>();
        HashMap<String, List<String>> graphsWithQuestion = new HashMap<>();
        Map<String, Query> parsedQueries = new HashMap<>();

        for (String s : queries.keySet()) {
            //build the graph associated to the query
            final Graph g = GraphFactory.createDefaultGraph();
//            Query query = new Query();
            Query query = null;

            try {
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.append(s);
//                pss.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
//                pss.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
//                pss.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
//                pss.setNsPrefix("dbo", "http://dbpedia.org/ontology/");
//                pss.setNsPrefix("yago", "http://dbpedia.org/class/yago/");
                query = pss.asQuery();
            } catch (QueryParseException e) {
                LOGGER.warn("Query: " + s);
                LOGGER.warn(e.toString());
            }
            try {
                ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
                    public void visit(ElementPathBlock el) {
                        PathBlock path = el.getPattern();
                        HashMap<Node, Node> dict = new HashMap<>();
                        int i = 1;
                        for (TriplePath t : path.getList()) {
                            Node s = t.getSubject();
                            String s_string = "";
                            if (s.isVariable()) s_string = "v" + i;
                            else s_string = "r" + i;
                            if (!dict.containsKey(s)) {
                                dict.put(s, NodeFactory.createLiteral(s_string));
                                i++;
                            }

                            Node o = t.getObject();
                            String o_string = "";
                            if (o.isVariable()) o_string = "v" + i;
                            else o_string = "r" + i;
                            if (!dict.containsKey(o)) {
                                dict.put(o, NodeFactory.createLiteral(o_string));
                                i++;
                            }
                            Node p = t.getPredicate();
                            String p_string = "";
                            if (p.isVariable()) p_string = "v" + i;
                                //else if(p.isURI()&&predicateToSubjectType.containsKey(p.getURI())) p_string=p.getURI();
                                //else if(p.isURI()&&predicateToObjectType.containsKey(p.getURI()))p_string=p.getURI();
                            else p_string = "r" + i;
                            //Node p = NodeFactory.createLiteral("p");
                            if (!dict.containsKey(p)) {
                                dict.put(p, NodeFactory.createLiteral(p_string));
                                i++;
                            }
                            Triple tmp = Triple.create(dict.get(s), dict.get(p), dict.get(o));
                            g.add(tmp);
                        }
                    }
                });
                //if the Graph is not isomorphic to previously seen graphs add it to the List

                boolean present = false;
                for (Graph g_present : graphs.keySet()) {
                    if (g.isIsomorphicWith(g_present)) {
                        present = true;
                        int i = graphs.get(g_present) + 1;
                        graphs.put(g_present, i);
                        List<String> currGraphQuestions = graphsWithQuestion.get(g.toString());
                        currGraphQuestions.add(queries.get(s));
                    }
                }
                if (!present) {
                    graphs.put(g, 1);
                    List<String> currGraph = new ArrayList<>();
                    currGraph.add(queries.get(s));
                    graphsWithQuestion.put(g.toString(), currGraph);
                }

                parsedQueries.put(queries.get(s), query);

            } catch (NullPointerException e) {
                LOGGER.warn(e.toString());
            }

        }
        //look at some properties
        //int i = 0;
        //int j = 0;
        //for (Graph g : graphs.keySet()) {
        //    i += graphs.get(g);
        //    if (graphs.get(g) > 10) {
        //        j += graphs.get(g);
        //    }
        //}
        //uncomment these lines to for debugging
        //System.out.print(i + "\n");
        //System.out.print(j);
        for (String graph : graphsWithQuestion.keySet()) {
            Cluster cluster = new Cluster(graph);
            List<String> list = graphsWithQuestion.get(graph);
            for (String s : list) {
                CustomQuestion question = new CustomQuestion(parsedQueries.get(s).toString(), s, getSimpleModifiers(parsedQueries.get(s).toString()), graph);
                HashMap<String, String> languageToQuestion = new HashMap<>();
                languageToQuestion.put("en", s); //TODO which attributes are important?
                //question.setLanguageToQuestion(languageToQuestion);
                //question.setSparqlQuery(inverseQueryMap.get(s));
                cluster.addQuestion(question);
            }
            if (cluster.getQuestions().size() >= 10)
                clusters.add(cluster);
        }
    }


    public static boolean areIsomorph(String q1, String q2) {
        List<String> queries = new ArrayList<>();
        queries.add(q1);
        queries.add(q2);
        HashMap<Graph, Integer> graphs = new HashMap<Graph, Integer>();
        HashMap<String, ArrayList<String>> variableStrings = new HashMap<>();
        for (String s : queries) {
            ArrayList<String> tripleStructures = new ArrayList<>();
            //build the graph associated to the query
            final Graph g = GraphFactory.createDefaultGraph();
            Query query = new Query();
            try {
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.append(s);
                query = pss.asQuery();
            } catch (QueryParseException e) {
                LOGGER.warn(e.toString(), e);
            }
            try {
                ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
                    public void visit(ElementPathBlock el) {
                        PathBlock path = el.getPattern();
                        HashMap<Node, Node> dict = new HashMap<Node, Node>();
                        int i = 1;
                        for (TriplePath t : path.getList()) {
                            Node s = t.getSubject();
                            Node o = t.getObject();
                            String tripleStructure = "";
                            if (s.isVariable()) tripleStructure += "var";
                            else tripleStructure += "val";
                            if (t.getPredicate().isVariable()) tripleStructure += "-var-";
                            else tripleStructure += "-val-";
                            if (o.isVariable()) tripleStructure += "var";
                            else tripleStructure += "val";
                            tripleStructures.add(tripleStructure);
                            Node p = NodeFactory.createLiteral("p");
                            if (!dict.containsKey(s)) {
                                dict.put(s, NodeFactory.createLiteral(String.valueOf(i)));
                                i++;
                            }
                            if (!dict.containsKey(o)) {
                                dict.put(o, NodeFactory.createLiteral(String.valueOf(i)));
                                i++;
                            }
                            Triple tmp = Triple.create(dict.get(s), p, dict.get(o));

                            g.add(tmp);
                        }
                    }
                });
                //if the Graph is not isomorphic to previously seen graphs add it to the List
                variableStrings.put(s, tripleStructures);
                boolean present = false;
                for (Graph g_present : graphs.keySet()) {
                    if (g.isIsomorphicWith(g_present)) {
                        present = true;
                        int i = graphs.get(g_present) + 1;
                        graphs.put(g_present, i);
                    }
                }
                if (!present) {
                    graphs.put(g, 1);
                }
            } catch (NullPointerException e) {
                LOGGER.warn(e.toString(), e);
            }
        }
        ArrayList<String> tripleStructures1 = variableStrings.get(q1);
        ArrayList<String> tripleStructures2 = variableStrings.get(q2);
        if (graphs.size() != 1)
            return false;
        for (String tripleStructure : tripleStructures1) {
            if (!tripleStructures2.contains(tripleStructure)) {
                return false;
            }
            tripleStructures2.remove(tripleStructure);
        }
        return true;
    }

    private static List<String> getSimpleModifiers(String queryString) {
        Pattern KEYWORD_MATCHER = Pattern.compile("\\w{2}+(?:\\s*\\w+)*");
        try {
            String trimmedQuery = TextUtils.cleanQuery(queryString);

            Matcher keywordMatcherCurrent = KEYWORD_MATCHER.matcher(trimmedQuery);
            List<String> modifiers = new ArrayList<>();
            while (keywordMatcherCurrent.find()) {
                String modifier = keywordMatcherCurrent.group();
                if (modifier.equalsIgnoreCase("en OPTIONAL")) {
                    modifiers.add("OPTIONAL");
                } else if (!modifier.equalsIgnoreCase("_type")
                        && !modifier.equalsIgnoreCase("en")
                        && !modifier.equalsIgnoreCase("es")) {
                    modifiers.add(modifier);
                }
            }
            return modifiers;
        } catch (QueryParseException e) {
            LOGGER.warn("Unable to parse query: " + queryString, e);
        }
        return emptyList();
    }

    public List<Cluster> getClusters() {
        return clusters;
    }
}
