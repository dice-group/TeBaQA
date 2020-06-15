package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.SemanticAnalysisHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.junit.Test;

public class ResourceLinkerTest {
    @Test
    public void testResourceLinker() {
        //ParameterizedSparqlString s=new ParameterizedSparqlString("ASK WHERE{ ?uri ?var_0 ; ?var_1 ?var_2 }");
        ParameterizedSparqlString s=new ParameterizedSparqlString();
        s.setCommandText("SELECT");
        //s.setIri("var_0","http://test.com");
        Query q= QueryFactory.create("SELECT ?uri WHERE{ ?var_0 ?var_4 ?uri; ?var_1 ?var_2 }ORDER BY DESC(?uri)");
        q.setQueryPattern(null);
        System.out.println(s.toString());
        /*QueryMappingFactoryLabels q=new QueryMappingFactoryLabels("What is the highest mountain in Australia?","Select * WHERE{?uri <htttp://test.com> <htttp://test2.com>. ?uri <htttp://test.com> <htttp://test3.com>.} ",new SemanticAnalysisHelper());
        ResourceLinker l=new ResourceLinker();
        l.extractEntities("What is the highest mountain in Australia?");*/
    }
}
