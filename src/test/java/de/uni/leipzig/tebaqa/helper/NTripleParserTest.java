package de.uni.leipzig.tebaqa.helper;

import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class NTripleParserTest {

    @Test
    public void testContainsSourceCountry() {
        Set<String> collect = NTripleParser.getNodes().parallelStream().filter(rdfNode -> rdfNode.toString().equals("http://dbpedia.org/ontology/sourceCountry"))
                .map(RDFNode::toString).collect(Collectors.toSet());

        assertTrue(collect.contains("http://dbpedia.org/ontology/sourceCountry"));
    }

}