package de.uni.leipzig.tebaqa.helper;

import com.google.common.collect.Sets;
import de.uni.leipzig.tebaqa.model.ResultsetBinding;
import de.uni.leipzig.tebaqa.model.SPTupel;
import moa.recommender.rc.utils.Hash;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

import java.util.*;
import java.util.stream.Collectors;

import static de.uni.leipzig.tebaqa.spring.AnnotateQualD8.loadQuald9;

public class CommonTupels {
    private static List<String[]> extractTuples(String queryString){
        try {
        Query query= QueryFactory.create(queryString);
        ElementTriplesBlock block = new ElementTriplesBlock();
        List<String[]>tuples=new ArrayList<>();

            ElementWalker.walk(query.getQueryPattern(),
                    new ElementVisitorBase() {
                        public void visit(ElementPathBlock el) {
                            Iterator<TriplePath> triples = el.patternElts();
                            while (triples.hasNext()) {
                                Node subject;
                                Node predicate;
                                Node object;
                                TriplePath t = triples.next();
                                if (t.getSubject().isVariable() && t.getObject().isURI())
                                    tuples.add(new String[]{t.getPredicate().toString(), t.getObject().toString(), "sp"});
                                else if (t.getObject().isVariable()&&t.getSubject().isURI())
                                    tuples.add(new String[]{t.getPredicate().toString(), t.getSubject().toString(), "po"});
                                //Var uri =Var.alloc(t.getPredicate());
                                //vars.add(uri);
                                //resourceLinker.mappedProperties.forEach(ent -> bindings.add(BindingFactory.binding(uri, NodeFactory.createURI(ent.getUri()))));
                            }

                        }


                    }
            );
            return tuples;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }



    }
    private static Set<String> getTypes(List<String>resources){
        Set<String>types=new HashSet<>();
        boolean set=false;
        for(String resource:resources) {
            QueryExecution qe = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", "SELECT DISTINCT ?type WHERE{  <" + resource + "> a ?type.}");
            ResultSet r = qe.execSelect();
            Set<String>found=new HashSet<>();
            while (r.hasNext()) {
                QuerySolution s = r.nextSolution();
                if(!set)
                    types.add(s.get("type").toString());
                else found.add(s.get("type").toString());

            }
            if(set&&found.size()>0)
                types=Sets.intersection(types,found);
            else set=true;
            qe.close();
        }
        return types;
    }

    public static HashMap<String,Set<String>>[] getCommonTuples(){
        List<IQuestion> load = loadQuald9();
        HashMap<String,String> questionToId=new HashMap<>();
        HashMap<String,String> idToSparql=new HashMap<>();
        HashMap<String,List<String>> predicateToEntType=new HashMap<>();
        HashMap<String,List<String>>entityTypeToPredicate=new HashMap();
        Dataset testDataset= Dataset.QALD8_Train_Multilingual;

        List<IQuestion> loadquald8 = LoaderController.load(testDataset);
        load.forEach(q->{
                    Map<String,String> questions=q.getLanguageToQuestion();
                    questionToId.put(q.getId(),questions.get("en"));
                    idToSparql.put(q.getId(),q.getSparqlQuery());
                }
        );
        loadquald8.forEach(q->{
                    Map<String,String> questions=q.getLanguageToQuestion();
                    questionToId.put("q8"+q.getId(),questions.get("en"));
                    idToSparql.put("q8"+q.getId(),q.getSparqlQuery());
                }
        );
        for(String id:idToSparql.keySet()){
            System.out.println(idToSparql.get(id));
            List<String[]>tuples=extractTuples(idToSparql.get(id));
            if(tuples!=null) {
                tuples.forEach(t -> {
                    if (t[2].equals("sp")&&!predicateToEntType.containsKey(t[0])){
                        List<String>a=new ArrayList<>();
                        a.add(t[1]);
                        predicateToEntType.put(t[0],a);
                    }
                    else if (t[2].equals("sp")&&predicateToEntType.containsKey(t[0])) predicateToEntType.get(t[0]).add(t[1]);
                    else if (t[2].equals("po")&&!entityTypeToPredicate.containsKey(t[0])){
                        List<String>a=new ArrayList<>();
                        a.add(t[1]);
                        entityTypeToPredicate.put(t[0],a);
                    }
                    else if (t[2].equals("po")&&entityTypeToPredicate.containsKey(t[0])) entityTypeToPredicate.get(t[0]).add(t[1]);

                });
            }
        }
        Map<String, List<String>> entityTypeToPredicateFiltered = entityTypeToPredicate.entrySet().stream()
                .filter(x -> x.getValue().size()>=10)
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        Map<String, List<String>> predicateToEntTypeFiltered = predicateToEntType.entrySet().stream()
                .filter(x -> x.getValue().size()>=10)
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
        HashMap<String,Set<String>> predicateToSubjectType=new HashMap();
        entityTypeToPredicateFiltered.forEach((s, strings) -> predicateToSubjectType.put(s,getTypes(strings)));
        HashMap<String,Set<String>> predicateToObjectType=new HashMap();
        predicateToEntTypeFiltered.forEach((s, strings) -> predicateToObjectType.put(s,getTypes(strings)));
        return new HashMap[]{predicateToSubjectType,predicateToObjectType};
    }

}
