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
    private static Set<RDFNode> subjects = new HashSet<>();

    //Do no instantiate
    private NTripleParser() {
    }

    private static Set<RDFNode> readNodes() {
        final Model[] model = new Model[1];
        Set<RDFNode> subjects = new HashSet<>();
        List<String> fileNames = new ArrayList<>();
        fileNames.add("dbpedia_2016-10.nt");
        fileNames.add("dbpedia_3Eng_class.ttl");
        fileNames.add("dbpedia_3Eng_property.ttl");
        fileNames.forEach(fileName -> {
            model[0] = ModelFactory.createDefaultModel();
            InputStream is = FileManager.get().open(fileName);
            if (is != null) {
                model[0].read(is, null, "N-TRIPLE");
            } else {
                log.error("cannot read " + fileName);
            }
            model[0].listSubjects().forEachRemaining(subjects::add);
        });
        return subjects;
    }


    public static Set<RDFNode> getNodes() {
        if (subjects.isEmpty()) {
            subjects = readNodes();
        }
        return subjects;
    }
}
