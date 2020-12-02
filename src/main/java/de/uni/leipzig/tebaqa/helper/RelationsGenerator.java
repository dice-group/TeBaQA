package de.uni.leipzig.tebaqa.helper;


import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionHttpWrapper;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.SymmetricConciseBoundedDescriptionGeneratorImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RelationsGenerator {
    private QueryExecutionFactory qef;
    private ConciseBoundedDescriptionGenerator cbdGen;

    public RelationsGenerator(){
        long timeToLive = TimeUnit.DAYS.toMillis(30);
        CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("/tmp/qald/sparql", true, timeToLive);
        qef  = FluentQueryExecutionFactory
                .http("http://limbo-triple.cs.upb.de:3030//limbo-suche/query")
                .config().withPostProcessor(qe -> ((QueryEngineHTTP) ((QueryExecutionHttpWrapper) qe).getDecoratee())
                        .setModelContentType(WebContent.contentTypeRDFXML))
                .withCache(cacheFrontend)
                .end()
                .create();
        cbdGen = new SymmetricConciseBoundedDescriptionGeneratorImpl(qef);
    }
    public Model getCBD(String uri){
       return cbdGen.getConciseBoundedDescription(uri);
    }
    public List<String[]> getRelatedResourcesByType(String resource, String type){
        Query q= QueryFactory.create("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT DISTINCT ?o ?l  WHERE {{\n" +
                " ?r ?p ?o.\n" +
                "  ?o a ?t.\n" +
                " ?o rdfs:label ?l.\n" +
                "}UNION{\n" +
                "    ?o ?p ?r.\n" +
                "    ?o a ?t.\n" +
                "    ?o rdfs:label ?l.\n" +
                "  }}");
        List<Var>vars=new ArrayList<>();
        vars.add(Var.alloc("r"));
        vars.add(Var.alloc("t"));
        List<Binding> bindings=new ArrayList<Binding>();
        Binding bind=BindingFactory.binding(vars.get(0), NodeFactory.createURI(resource));
        Binding bind2=BindingFactory.binding(bind,vars.get(1), NodeFactory.createURI(type));
        bindings.add(bind2);
        //bindings.add(bindings.get(0),BindingFactory.binding(vars.get(1), NodeFactory.createLiteral(type)));

        q.setValuesDataBlock(vars,bindings);
        QueryExecution qe =qef.createQueryExecution(q);
        ResultSet rs = ResultSetFactory.copyResults(qe.execSelect());
        List<String[]>resourceToLabel=new ArrayList<>();
        while(rs.hasNext()){
            QuerySolution s=rs.nextSolution();
            resourceToLabel.add(new String[]{s.get("o").toString(),s.get("l").toString()});
        }
        return resourceToLabel;

    }
    public List<String[]> getRelatedResourcesByTypeProperty(String property, String type){
        Query q= QueryFactory.create("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
                "SELECT DISTINCT ?o ?l  WHERE {{\n" +
                " ?r ?p ?o.\n" +
                "  ?o a ?t.\n" +
                " ?o rdfs:label ?l.\n" +
                "}UNION{\n" +
                "    ?o ?p ?r.\n" +
                "    ?o a ?t.\n" +
                "    ?o rdfs:label ?l.\n" +
                "  }}");
        List<Var>vars=new ArrayList<>();
        vars.add(Var.alloc("p"));
        vars.add(Var.alloc("t"));
        List<Binding> bindings=new ArrayList<Binding>();
        Binding bind=BindingFactory.binding(vars.get(0), NodeFactory.createURI(property));
        Binding bind2=BindingFactory.binding(bind,vars.get(1), NodeFactory.createURI(type));
        bindings.add(bind2);
        //bindings.add(bindings.get(0),BindingFactory.binding(vars.get(1), NodeFactory.createLiteral(type)));

        q.setValuesDataBlock(vars,bindings);
        ResultSet rs=qef.createQueryExecution(q).execSelect();
        List<String[]>resourceToLabel=new ArrayList<>();
        while(rs.hasNext()){
            QuerySolution s=rs.nextSolution();
            resourceToLabel.add(new String[]{s.get("o").toString(),s.get("l").toString()});
        }
        return resourceToLabel;

    }

    public static void main(String[]args){
        RelationsGenerator c=new RelationsGenerator();
        //HashMap res =c.getRelatedResourcesByType("https://portal.limbo-project.org/bahnhof/3631","http://linkedgeodata.org/vocabulary#platform");
        System.out.println();
    }
}


