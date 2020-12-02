package de.uni.leipzig.tebaqa.indexGeneration;

import java.util.HashSet;
import java.util.Set;

public class Resource {
    private String name;
    private Set<String>connectedProperties;
    private Set <String>connectedResources;
    private Set<String>types;
    private Set<String>labels;
    public Resource(String name){
        this.name=name;
        this.connectedResources=new HashSet<>();
        this.connectedProperties=new HashSet<>();
        this.types=new HashSet<>();
        this.labels=new HashSet<>();
    }
    public void addConnectedProperty(String property){
        this.connectedProperties.add(property);
    }
    public void addConnectedResource(String resource){
        this.connectedResources.add(resource);
    }
    public void addType(String type){
        this.types.add(type);
    }
    public void addLabel(String label){
        this.labels.add(label);
    }
    public Set<String>getConnectedResources(){
        return connectedResources;
    }
    public Set<String>getConnectedProperties(){
        return connectedProperties;
    }
    public Set<String>getTypes(){
        return types;
    }
    public Set<String>getLabels(){
        return labels;
    }
    public String getName(){
        return name;
    }
}
