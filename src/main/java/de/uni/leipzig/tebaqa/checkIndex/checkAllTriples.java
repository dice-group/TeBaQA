package de.uni.leipzig.tebaqa.checkIndex;

import de.uni.leipzig.tebaqa.helper.WordsGenerator;
import de.uni.leipzig.tebaqa.model.EntityCandidate;
import de.uni.leipzig.tebaqa.model.ResourceCandidate;
import de.uni.leipzig.tebaqa.model.Triple;
import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.http.HttpHost;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9;
import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9Test;

public class checkAllTriples {
    final static String TYPE="type";
    final static String LABEL="label";
    final static String CONNECTED_RESOURCE="connected_resource";
    final static String CONNECTED_PROPERTY_SUBJECT="connected_property_subject";
    final static String CONNECTED_PROPERTY_OBJECT="connected_property_object";
    static Set<String> getResourcesFromQuery(String queryString,RestHighLevelClient client){
        Set<String>falseResources=new HashSet<>();
        try {
            Query query = QueryFactory.create(queryString);

            ElementWalker.walk(query.getQueryPattern(),
                    new ElementVisitorBase() {
                        public void visit(ElementPathBlock el) {
                            Iterator<TriplePath> triples = el.patternElts();
                            while (triples.hasNext()) {
                                Node subject;
                                Node predicate;
                                Node object;
                                TriplePath t = triples.next();
                                if (!t.getObject().toString().contains("ontology")){
                                    if (t.getSubject().isURI() && t.getObject().isURI()) {
                                        EntityCandidate cand1 = searchResourceById(t.getSubject().toString(), client);
                                        if (cand1 == null) falseResources.add(t.getSubject().toString());
                                        EntityCandidate cand2 = searchResourceById(t.getObject().toString(), client);
                                        if (cand2 == null) falseResources.add(t.getObject().toString());
                                        if (cand1 != null && cand2 != null) {
                                            if (!cand1.getConnectedResourcesSubject().contains(cand2.getUri()) &&
                                                    !cand1.getConnectedPropertiesSubject().contains(t.getPredicate().toString())) {
                                                falseResources.add(t.getSubject().toString());
                                                if (!cand2.getConnectedResourcesObject().contains(cand1.getUri()) &&
                                                        !cand2.getConnectedPropertiesObject().contains(t.getPredicate().toString())) {
                                                    falseResources.add(t.getObject().toString());
                                                }
                                            }
                                        }
                                    } else if (t.getSubject().isURI()) {
                                        EntityCandidate cand = searchResourceById(t.getSubject().toString(), client);
                                        if (cand != null && !cand.getConnectedPropertiesSubject().contains(t.getPredicate().toString()))
                                            falseResources.add(t.getSubject().toString());
                                    } else if (t.getObject().isURI()) {
                                        EntityCandidate cand = searchResourceById(t.getObject().toString(), client);
                                        if (cand != null && !cand.getConnectedPropertiesObject().contains(t.getPredicate().toString()))
                                            falseResources.add(t.getObject().toString());

                                    }
                            }
                                //Var uri =Var.alloc(t.getPredicate());
                                //vars.add(uri);
                                //resourceLinker.mappedProperties.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                            }

                        }


                    }
            );
        }catch (Exception e){
            System.out.println("Wrong query");
            e.printStackTrace();
            System.out.println(queryString);
        }
            return falseResources;
    }
        static public EntityCandidate searchResourceById(String resource, RestHighLevelClient client) {
            BoolQueryBuilder queryBuilder;
            queryBuilder=new BoolQueryBuilder();

                TermQueryBuilder termQueryBuilder=new TermQueryBuilder("_id",resource);
                queryBuilder.must(termQueryBuilder);


            List<EntityCandidate> resources=new ArrayList<>();
            try {
                resources = getFromIndexLinks(100, queryBuilder,client);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //cache.put(bq, triples);
            if(!resources.isEmpty()) {
                return resources.get(0);
            }
            else return null;
        }
        static private List<EntityCandidate> getFromIndexLinks(int maxNumberOfResults, QueryBuilder bq,RestHighLevelClient client) throws IOException {
            SearchRequest searchRequest = new SearchRequest();
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(bq);
            searchSourceBuilder.size(maxNumberOfResults);
            searchRequest.source(searchSourceBuilder);
            searchRequest.indices("resourcedbpedia");
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits = searchResponse.getHits().getHits();

            List<EntityCandidate> candidates = new ArrayList<>();
            String name;
            Set<String>connectedResourcesSubject,connectedResourcesObject,connectedPropertiesSubject,connectedPropertiesObject,types,label;
            for (SearchHit hit : hits) {
                Map<String, Object> sources = hit.getSourceAsMap();
                name = hit.getId();
                label = new HashSet<>();
                label.addAll((ArrayList<String>)sources.get(LABEL));

                connectedPropertiesSubject=new HashSet<>();
                if(sources.containsKey(CONNECTED_PROPERTY_SUBJECT))
                connectedPropertiesSubject.addAll((ArrayList<String>)sources.get(CONNECTED_PROPERTY_SUBJECT));
                connectedPropertiesObject=new HashSet<>();
                if(sources.containsKey(CONNECTED_PROPERTY_OBJECT))
                connectedPropertiesObject.addAll((ArrayList<String>)sources.get(CONNECTED_PROPERTY_OBJECT));
                connectedResourcesSubject=new HashSet<>();
                if(sources.containsKey("connected_resource_subject"))
                connectedResourcesSubject.addAll((ArrayList<String>)sources.get("connected_resource_subject"));
                connectedResourcesObject=new HashSet<>();
                if(sources.containsKey("connected_resource_object"))
                connectedResourcesObject.addAll((ArrayList<String>)sources.get("connected_resource_object"));
                types=new HashSet<>();
                types.addAll((ArrayList<String>)sources.get(TYPE));

                EntityCandidate candidate = new EntityCandidate(name,label,connectedPropertiesSubject,connectedPropertiesObject,connectedResourcesSubject,connectedResourcesObject,types);
                candidates.add(candidate);
            }

            return candidates;
        }

    public static void main(String[]args) throws IOException {
        List<IQuestion> load = loadQuald9();
        List<IQuestion> loadtest = loadQuald9Test();
        HashMap<String,String> questionToId=new HashMap<>();
        List<String> sparqlQueries=new ArrayList<>();
        HashMap<String,List<String>> predicateToEntType=new HashMap<>();
        HashMap<String,List<String>>entityTypeToPredicate=new HashMap();
        Dataset quald8Train = Dataset.QALD8_Train_Multilingual;
        Dataset quald8Test = Dataset.QALD8_Test_Multilingual;
            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost("localhost", 9200, "http")));

        List<IQuestion> loadquald8 = LoaderController.load(quald8Train);
        List<IQuestion> loadquald8test = LoaderController.load(quald8Test);
        load.forEach(q->{
                    sparqlQueries.add(q.getSparqlQuery());
                }
        );
        loadtest.forEach(q->{sparqlQueries.add(q.getSparqlQuery());
        System.out.println(q.getSparqlQuery());});
        loadquald8.forEach(q->{
                    sparqlQueries.add(q.getSparqlQuery());
                }
        );
        loadquald8test.forEach(q->sparqlQueries.add(q.getSparqlQuery()));
        Set<String>falseResources=new HashSet<>();
        List<String>testQueries=new ArrayList<>();
        //testQueries.add("SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Mount_McKinley> dbo:wikiPageRedirects ?x . ?x <http://dbpedia.org/ontology/locatedInArea> ?uri. ?uri rdf:type yago:WikicatStatesOfTheUnitedStates }\n");

        //testQueries.forEach(q->falseResources.addAll(getResourcesFromQuery(q,client)));
        ArrayList<String>qs=new ArrayList<>();
        qs.add("SELECT DISTINCT ?x WHERE { ?uri <http://dbpedia.org/ontology/deathCause> ?x . }");
        qs.add("SELECT ?uri WHERE { ?uri <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Park_Chan-wook> . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/WorldHeritageSite> . { ?uri <http://dbpedia.org/property/year> ?s . } UNION { ?uri <http://dbpedia.org/property/year> ?n . } }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Actor> . ?uri <http://dbpedia.org/ontology/birthPlace> <http://dbpedia.org/resource/Paris> . ?uri <http://dbpedia.org/ontology/birthDate> ?date .  }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?film <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> . ?film <http://dbpedia.org/ontology/producer> ?uri . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Country> . ?uri <http://dbpedia.org/property/officialLanguages> ?language . }");
        qs.add("SELECT DISTINCT ?uri WHERE { <http://dbpedia.org/resource/Mount_McKinley> <http://dbpedia.org/ontology/wikiPageRedirects> ?x . ?x <http://dbpedia.org/ontology/locatedInArea> ?uri. ?uri a <http://dbpedia.org/class/yago/WikicatStatesOfTheUnitedStates> }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/occupation> <http://dbpedia.org/resource/Poet> . ?x <http://dbpedia.org/ontology/author> ?uri . ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Book> . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/occupation> <http://dbpedia.org/resource/Musician> . ?x <http://dbpedia.org/ontology/author> ?uri . ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Book> . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Actor> . ?f <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> . ?f <http://dbpedia.org/ontology/starring> ?uri . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Song> . ?uri <http://dbpedia.org/ontology/artist> <http://dbpedia.org/resource/Bruce_Springsteen> . ?uri <http://dbpedia.org/ontology/releaseDate> ?date . }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Film> . ?uri <http://dbpedia.org/ontology/director> <http://dbpedia.org/resource/Steven_Spielberg> . ?uri <http://dbpedia.org/ontology/budget> ?b . }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX dbr: <http://dbpedia.org/resource/> SELECT ?book WHERE { { ?book dbo:author dbr:Dan_Brown . ?book dbp:releaseDate ?date } UNION { ?book dbo:author dbr:Dan_Brown . ?book dbo:publicationDate ?date} }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT ?sub WHERE { ?sub dbo:goldMedalist dbr:Michael_Phelps .}");
        qs.add("PREFIX dbp: <http://dbpedia.org/property/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?uri WHERE { res:Berlin dbp:leader ?uri }");
        qs.add("SELECT ?y WHERE { <http://dbpedia.org/resource/Jacques_Cousteau> <http://dbpedia.org/ontology/child> ?x . ?x <http://dbpedia.org/ontology/child> ?y . }");
        qs.add("PREFIX res: <http://dbpedia.org/resource/> PREFIX dbp: <http://dbpedia.org/property/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE { res:The_Storm_on_the_Sea_of_Galilee dbo:author ?uri }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> PREFIX dbp: <http://dbpedia.org/property/> SELECT DISTINCT ?uri WHERE { res:Karakoram dbp:highest ?uri }");
        qs.add("PREFIX res: <http://dbpedia.org/resource/> PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbp: <http://dbpedia.org/property/> SELECT DISTINCT ?date WHERE { res:Death_of_Carlo_Giuliani dbo:deathDate ?date }");
        qs.add("SELECT DISTINCT ?num WHERE { <http://dbpedia.org/resource/Vrije_Universiteit_Amsterdam> <http://dbpedia.org/ontology/numberOfStudents> ?num }");
        qs.add("SELECT ?uri WHERE { ?uri <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:James_Bond_films> }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?uri WHERE { dbr:Tom_Hanks dbo:spouse ?uri }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/ontology/Volcano> ; <http://dbpedia.org/ontology/locatedInArea> ?area . ?area <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:Countries_in_Africa> . ?uri <http://dbpedia.org/ontology/elevation> ?elevation }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri <http://dbpedia.org/ontology/occupation> <http://dbpedia.org/resource/Poet> . ?x <http://dbpedia.org/ontology/author> ?uri . ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Book> . }");
        qs.add("SELECT DISTINCT ?date WHERE { <http://dbpedia.org/resource/Count_Dracula> <http://dbpedia.org/ontology/creator> ?x . ?x <http://dbpedia.org/ontology/deathDate> ?date. }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX dbp: <http://dbpedia.org/property/> PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE { {<http://dbpedia.org/resource/Rhine> dbo:country ?uri } UNION {dbr:Rhine dbp:country ?uri} }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE { {<http://dbpedia.org/resource/Lance_Bass> dbo:spouse ?uri} UNION {?uri dbo:spouse <http://dbpedia.org/resource/Lance_Bass>} }");
        qs.add("SELECT ?uri WHERE { ?uri <http://dbpedia.org/property/title> <http://dbpedia.org/resource/Emperor_of_China> . }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?year WHERE { res:Jack_Wolfskin dbo:foundingYear ?year }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?num WHERE { <http://dbpedia.org/resource/Pilsner_Urquell> <http://dbpedia.org/property/brewery> ?uri . ?uri dbo:foundingYear ?num }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?radius WHERE { res:Earth dbo:meanRadius ?radius }");
        qs.add("SELECT DISTINCT ?uri WHERE { ?uri a <http://dbpedia.org/class/yago/WikicatStatesOfTheUnitedStates> ; <http://dbpedia.org/property/postalabbreviation> ?s }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?date WHERE { res:Muhammad dbo:deathDate ?date }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX dbr: <http://dbpedia.org/resource/> SELECT ?uri WHERE { ?uri a dbo:MilitaryConflict ; dbo:place dbr:San_Antonio ; dbo:date ?date  }");
        qs.add("PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX res: <http://dbpedia.org/resource/> SELECT DISTINCT ?date WHERE { res:De_Beers dbo:foundingYear ?date }");
        qs.add("SELECT DISTINCT ?d WHERE { <http://dbpedia.org/resource/Diana,_Princess_of_Wales> <http://dbpedia.org/ontology/deathDate> ?d }");
        qs.add("PREFIX foaf: <http://xmlns.com/foaf/0.1/> PREFIX dbo: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT distinct ?web WHERE { ?sub dbo:numberOfEmployees ?obj . ?sub foaf:homepage ?web  . }");
        qs.forEach(q->falseResources.addAll(getResourcesFromQuery(q,client)));
        sparqlQueries.addAll(qs);
        sparqlQueries.forEach(q->falseResources.addAll(getResourcesFromQuery(q,client)));
        falseResources.forEach(r->System.out.println(r));
        List<String>triples=new ArrayList<>();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("C:/Users/Jan/Desktop/missing_new.nt"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
        for(String res:falseResources){
            System.out.println("Current: "+res);
            QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", "describe <"+res+">");
            Model m=qe.execDescribe();
            StmtIterator i=m.listStatements();
            while(i.hasNext()){
                org.apache.jena.rdf.model.Statement s=i.nextStatement();
                if(s.getObject().isLiteral()) writer.write("<"+s.getSubject()+"> "+"<"+s.getPredicate()+"> "+"\""+s.getObject()+"\" .\n");
                else writer.write("<"+s.getSubject()+"> "+"<"+s.getPredicate()+"> "+"<"+s.getObject()+"> .\n");
            }
            m.close();


        }
            writer.flush();
            writer.close();
            client.close();
            System.out.println("finished");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
