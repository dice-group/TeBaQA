package de.uni.leipzig.tebaqa.helper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.FileManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NTripleParser {
    private static Logger log = Logger.getLogger(NTripleParser.class);
    private Model model;
    private Set<RDFNode> subjects = new HashSet<>();

    public NTripleParser() {
        List<String> fileNames = new ArrayList<>();
        fileNames.add("dbpedia_2016-10.nt");
        fileNames.add("dbpedia_3Eng_class.ttl");
        fileNames.add("dbpedia_3Eng_property.ttl");
        fileNames.forEach(fileName -> {
            model = ModelFactory.createDefaultModel();
            InputStream is = FileManager.get().open(fileName);
            if (is != null) {
                model.read(is, null, "N-TRIPLE");
            } else {
                log.error("cannot read " + fileName);
            }
            model.listSubjects().forEachRemaining(subjects::add);
        });

    }


    public Set<RDFNode> getNodes() {
        return subjects;
    }

    public Model getModel() {
        return model;
    }
}
