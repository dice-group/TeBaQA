package de.uni.leipzig.tebaqa.controller;

import de.uni.leipzig.tebaqa.model.ResourceCandidate;
import org.apache.jena.sparql.algebra.Op;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class KeyWordController {
    private ElasticSearchEntityIndex index;
    public KeyWordController(){
        try {
            index=new ElasticSearchEntityIndex();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<ResourceCandidate> searchByKeywords(String searchString, Optional<String>restrict
            ,Optional<String>type,Optional<String>connectedProperty,Optional<String>connectedResource){
        Set<ResourceCandidate>candidates=new HashSet<>();
        restrict.ifPresent(s->{
            if(s.equals("entity"))candidates.addAll(index.searchEntity(searchString,connectedResource,connectedProperty,type));
            else if(s.equals("property"))candidates.addAll(index.searchResource(searchString,"property",false));
            else candidates.addAll(index.searchResource(searchString,"class",false));
        });
        if(!restrict.isPresent()){
            candidates.addAll(index.searchEntity(searchString,connectedResource,connectedProperty,type));
            candidates.addAll(index.searchResource(searchString,"property",false));
            candidates.addAll(index.searchResource(searchString,"class",false));
        }
        return candidates;
    }
}
