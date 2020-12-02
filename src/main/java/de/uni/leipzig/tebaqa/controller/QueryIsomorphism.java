package de.uni.leipzig.tebaqa.controller;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.uni.leipzig.tebaqa.helper.Utilities;
import de.uni.leipzig.tebaqa.model.Cluster;
import de.uni.leipzig.tebaqa.model.CustomQuestion;
import org.aksw.qa.commons.datastructure.Question;
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
import org.apache.log4j.Logger;
import org.dllearner.utilities.QueryUtils;

import java.util.*;

import static de.uni.leipzig.tebaqa.controller.PipelineController.getSimpleModifiers;

/**
 * Source: https://github.com/AKSW/NLIWOD/blob/master/qa.hawk/src/main/java/org/aksw/hawk/experiment/QueryIsomorphism.java
 */
public class QueryIsomorphism {
    private static Logger log = Logger.getLogger(QueryIsomorphism.class);

    private List<Cluster> clusters;

    QueryIsomorphism(Map<String, String> queries,HashMap<String,Set<String>>[]commonPredicates) {
        //HashMap<String,Set<String>>predicateToSubjectType=commonPredicates[0];
        //HashMap<String,Set<String>>predicateToObjectType=commonPredicates[1];
        log.debug("Generating SPARQL Query graphs...");
        BiMap<String, String> inverseQueryMap = HashBiMap.create(queries).inverse();
        clusters = new ArrayList<>(new ArrayList<>());
        HashMap<Graph, Integer> graphs = new HashMap<>();
        HashMap<String, List<String>> graphsWithQuestion = new HashMap<>();
        for (String s : queries.keySet()) {
            //build the graph associated to the query
            final Graph g = GraphFactory.createDefaultGraph();
            Query query = new Query();
            try {
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.append(s);
                query = pss.asQuery();
            } catch (QueryParseException e) {
                log.warn(e.toString(), e);
            }
            try {
                ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
                    public void visit(ElementPathBlock el) {
                        PathBlock path = el.getPattern();
                        HashMap<Node, Node> dict = new HashMap<>();
                        int i = 1;
                        for (TriplePath t : path.getList()) {
                            Node s = t.getSubject();
                            String s_string="";
                            if(s.isVariable()) s_string="v"+String.valueOf(i);
                            else s_string="r"+String.valueOf(i);
                            if (!dict.containsKey(s)) {
                                dict.put(s, NodeFactory.createLiteral(s_string));
                                i++;
                            }

                            Node o = t.getObject();
                            String o_string="";
                            if(o.isVariable()) o_string="v"+String.valueOf(i);
                            else o_string="r"+String.valueOf(i);
                            if (!dict.containsKey(o)) {
                                dict.put(o, NodeFactory.createLiteral(o_string));
                                i++;
                            }
                            Node p=t.getPredicate();
                            String p_string="";
                            if(p.isVariable()) p_string="v"+String.valueOf(i);
                            //else if(p.isURI()&&predicateToSubjectType.containsKey(p.getURI())) p_string=p.getURI();
                            //else if(p.isURI()&&predicateToObjectType.containsKey(p.getURI()))p_string=p.getURI();
                            else p_string="r"+String.valueOf(i);
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

                Boolean present = false;
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
            } catch (NullPointerException e) {
                log.warn(e.toString(), e);
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
                CustomQuestion question = new CustomQuestion(inverseQueryMap.get(s),s,getSimpleModifiers(inverseQueryMap.get(s)),graph);
                HashMap<String, String> languageToQuestion = new HashMap<>();
                languageToQuestion.put("de", s);
                //question.setLanguageToQuestion(languageToQuestion);
                //question.setSparqlQuery(inverseQueryMap.get(s));
                cluster.addQuestion(question);
            }
            //if(cluster.getQuestions().size()>=10)
            clusters.add(cluster);
        }
    }


    public static boolean areIsomorph(String q1, String q2) {
        List<String> queries = new ArrayList<>();
        queries.add(q1);
        queries.add(q2);
        HashMap<Graph, Integer> graphs = new HashMap<Graph, Integer>();
        HashMap<String,ArrayList<String>>variableStrings=new HashMap<>();
        for (String s : queries) {
            ArrayList<String>tripleStructures=new ArrayList<>();
            //build the graph associated to the query
            final Graph g = GraphFactory.createDefaultGraph();
            Query query = new Query();
            try {
                ParameterizedSparqlString pss = new ParameterizedSparqlString();
                pss.append(s);
                query = pss.asQuery();
            } catch (QueryParseException e) {
                log.warn(e.toString(), e);
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
                            String tripleStructure="";
                            if(s.isVariable())tripleStructure+="var";
                            else tripleStructure+="val";
                            if(t.getPredicate().isVariable())tripleStructure+="-var-";
                            else tripleStructure+="-val-";
                            if(o.isVariable())tripleStructure+="var";
                            else tripleStructure+="val";
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
                variableStrings.put(s,tripleStructures);
                Boolean present = false;
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
                log.warn(e.toString(), e);
            }
        }
        ArrayList<String>tripleStructures1=variableStrings.get(q1);
        ArrayList<String>tripleStructures2=variableStrings.get(q2);
        if(graphs.size() != 1)
            return false;
        for(String tripleStructure:tripleStructures1){
            if(!tripleStructures2.contains(tripleStructure)){
                return false;
            }
            tripleStructures2.remove(tripleStructure);
        }
        return true;
    }

    List<Cluster> getClusters() {
        return clusters;
    }
}
