package de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.rio.helpers.RDFHandlerBase;

import java.util.*;

import static de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.TeBaQAIndexer.LABEL_PREDICATES;
import static de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.TeBaQAIndexer.TYPE_PREDICATE;

public class TripleStatementHandler extends RDFHandlerBase {

    private final ElasticSearchIndexer indexer;
    private final Collection<String> excludedPredicates;

    private final Set<String> names = new HashSet<>();
    private final Map<String, StringBuilder> nameToResourceSubject = new HashMap<>();
    private final Map<String, StringBuilder> nameToResourceObject = new HashMap<>();
    private final Map<String, StringBuilder> subjectToTypes = new HashMap<>();
    private final Map<String, StringBuilder> nameToLabels = new HashMap<>();
    private final Map<String, StringBuilder> nameToProperties_subject = new HashMap<>();
    private final Map<String, StringBuilder> nameToProperties_object = new HashMap<>();

    public TripleStatementHandler(ElasticSearchIndexer indexer, Collection<String> excludedPredicates) {
        this.indexer = indexer;
        this.excludedPredicates = excludedPredicates;
    }

    @Override
    public void handleStatement(Statement st) {
        String subject = st.getSubject().stringValue();
        String predicate = st.getPredicate().stringValue();
        String object = st.getObject().stringValue();

        if (this.excludedPredicates.contains(predicate.toLowerCase())) {
            return;
        }

        names.add(subject);
        if (!names.contains(object) && st.getObject() instanceof URI)
            names.add(object);


        if (TYPE_PREDICATE.equalsIgnoreCase(predicate)) { // Statement defines rdf:type of a subject
            if (!subjectToTypes.containsKey(subject)) subjectToTypes.put(subject, new StringBuilder(object));
            else subjectToTypes.put(subject, subjectToTypes.get(subject).append(",,").append(object));

        } else if (st.getObject() instanceof URI) { // Statement with a URI as object
            if (!nameToResourceSubject.containsKey(subject))
                nameToResourceSubject.put(subject, new StringBuilder(object));
            else nameToResourceSubject.put(subject, nameToResourceSubject.get(subject).append(",,").append(object));

            if (!nameToResourceObject.containsKey(object)) nameToResourceObject.put(object, new StringBuilder(subject));
            else nameToResourceObject.put(object, nameToResourceObject.get(object).append(",,").append(subject));

            if (!nameToProperties_subject.containsKey(subject))
                nameToProperties_subject.put(subject, new StringBuilder(predicate));
            else
                nameToProperties_subject.put(subject, nameToProperties_subject.get(subject).append(",,").append(predicate));

            if (!nameToProperties_object.containsKey(subject))
                nameToProperties_object.put(object, new StringBuilder(predicate));
            else
                nameToProperties_object.put(subject, nameToProperties_object.get(object).append(",,").append(predicate));

        } else if (!LABEL_PREDICATES.contains(predicate)) { // Make sure that statement does not define a label
            if (!nameToProperties_subject.containsKey(subject))
                nameToProperties_subject.put(subject, new StringBuilder(predicate));
            else
                nameToProperties_subject.put(subject, nameToProperties_subject.get(subject).append(",,").append(predicate));
        }

        if (LABEL_PREDICATES.contains(predicate.toLowerCase())) { // This is for labels
            if (!nameToLabels.containsKey(subject)) nameToLabels.put(subject, new StringBuilder(object));
            else nameToLabels.put(subject, nameToLabels.get(subject).append(",,").append(object));
        }

        // Index in a bulk
        if (nameToProperties_subject.size() + nameToProperties_object.size() + nameToLabels.size() + nameToResourceSubject.size() + subjectToTypes.size() > 1000000)
            index();
    }

    public void index() {
        for (String name : names) {
            Set<String> labels = new HashSet<>();
            Set<String> types = new HashSet<>();
            Set<String> predicates_subject = new HashSet<>();
            Set<String> predicates_object = new HashSet<>();
            Set<String> resource_Subject = new HashSet<>();
            Set<String> resource_Object = new HashSet<>();

            if (nameToProperties_subject.containsKey(name))
                predicates_subject.addAll(Arrays.asList(nameToProperties_subject.get(name).toString().split(",,")));
            if (nameToProperties_object.containsKey(name))
                predicates_object.addAll(Arrays.asList(nameToProperties_object.get(name).toString().split(",,")));
            if (nameToResourceSubject.containsKey(name))
                resource_Subject.addAll(Arrays.asList(nameToResourceSubject.get(name).toString().split(",,")));
            if (nameToResourceObject.containsKey(name))
                resource_Object.addAll(Arrays.asList(nameToResourceObject.get(name).toString().split(",,")));
            if (nameToLabels.containsKey(name))
                labels.addAll(Arrays.asList(nameToLabels.get(name).toString().split(",,")));
            if (subjectToTypes.containsKey(name))
                types.addAll(Arrays.asList(subjectToTypes.get(name).toString().split(",,")));

            indexer.upsertResource(name, labels, types, resource_Subject, resource_Object, predicates_subject, predicates_object);
        }
        names.clear();
        nameToLabels.clear();
        nameToResourceSubject.clear();
        nameToResourceObject.clear();
        nameToProperties_subject.clear();
        nameToProperties_object.clear();
        subjectToTypes.clear();
        indexer.commit();
    }
}