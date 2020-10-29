package de.uni.leipzig.tebaqa.entitylinking.cache;

import de.uni.leipzig.tebaqa.tebaqacommons.model.ClassCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityCandidate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.PropertyCandidate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ResourceCache {

    private final Map<String, Set<EntityCandidate>> entityCache;
    private final Map<String, Set<PropertyCandidate>> propertyCache;
    private final Map<String, Set<ClassCandidate>> classCache;

    public ResourceCache() {
        this.entityCache = new HashMap<>();
        this.propertyCache = new HashMap<>();
        this.classCache = new HashMap<>();
    }

    public Set<EntityCandidate> getEntitiesFor(String coOccurrence) {
        return this.entityCache.get(coOccurrence);
    }

    public Set<PropertyCandidate> getPropertiesFor(String coOccurrence) {
        return this.propertyCache.get(coOccurrence);
    }

    public Set<ClassCandidate> getClassesFor(String coOccurrence) {
        return this.classCache.get(coOccurrence);
    }

    public boolean addEntitiesToCache(String coOccurrence, Set<EntityCandidate> entities) {
        boolean alreadyExists = this.entityCache.containsKey(coOccurrence);
        this.entityCache.put(coOccurrence, entities);
        return alreadyExists;
    }

    public boolean addPropertiesToCache(String coOccurrence, Set<PropertyCandidate> properties) {
        boolean alreadyExists = this.propertyCache.containsKey(coOccurrence);
        this.propertyCache.put(coOccurrence, properties);
        return alreadyExists;
    }

    public boolean addClassesToCache(String coOccurrence, Set<ClassCandidate> classes) {
        boolean alreadyExists = this.classCache.containsKey(coOccurrence);
        this.classCache.put(coOccurrence, classes);
        return alreadyExists;
    }

    public boolean hasEntities(String coOccurrence) {
        return this.entityCache.containsKey(coOccurrence);
    }

    public boolean hasProperties(String coOccurrence) {
        return this.propertyCache.containsKey(coOccurrence);
    }

    public boolean hasClasses(String coOccurrence) {
        return this.classCache.containsKey(coOccurrence);
    }

}
