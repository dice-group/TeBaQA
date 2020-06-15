package de.uni.leipzig.tebaqa.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CooccurenceGroup {
    Set<String>coOccurrences;

    public CooccurenceGroup(){
        coOccurrences=new HashSet<>();
    }
    public Set<String>getCoOccurences(){
        return coOccurrences;
    }
    public void addCooccurence(String coOccurence, List<String> candidates){
        coOccurrences.add(coOccurence);
    }

}
