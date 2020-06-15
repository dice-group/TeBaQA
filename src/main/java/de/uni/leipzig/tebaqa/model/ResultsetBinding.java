package de.uni.leipzig.tebaqa.model;

import de.uni.leipzig.tebaqa.helper.SPARQLUtilities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ResultsetBinding {
    private Map<String, String> bindings;
    private Set<String> result;
    private String query;
    private Double rating;
    private int answerType;

    private static DateTimeFormatter dateTimeFormatterLong = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static DateTimeFormatter dateTimeFormatterShortMonth = DateTimeFormatter.ofPattern("yyyy-M-dd");
    private static DateTimeFormatter dateTimeFormatterShortDay = DateTimeFormatter.ofPattern("yyyy-MM-d");

    public ResultsetBinding() {
        this.bindings = new HashMap<>();
        this.result = new HashSet<>();
        this.query = "";
        this.rating = -1.0;
        this.answerType = SPARQLResultSet.UNKNOWN_ANSWER_TYPE;
    }
    public boolean isSameBinding(Map<String,String>binding){
        for(String key:this.bindings.keySet()){
            if(!this.bindings.get(key).equals(binding.get(key)))
                return false;
        }
        return true;
    }
    public Map<String, String> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, String> bindings) {
        this.bindings = bindings;
    }

    public Set<String> getResult() {
        return result;
    }

    public void retrieveRedirects() {
        this.result = this.result.parallelStream().map(resource -> {
            if (resource.startsWith("http://dbpedia.org/resource/")) {
                return SPARQLUtilities.getRedirect(resource);
            } else {
                return resource;
            }
        }).collect(Collectors.toSet());
    }

    public void setResult(Set<String> result) {
        this.result = new HashSet<>();
        result.parallelStream().forEach(this::addResult);
    }

    public void addBinding(String variable, String uri) {
        this.bindings.put(variable, uri);
    }

    public void addResult(String s) {
        if (this.result.size() < 50) {
            if (s.toLowerCase().startsWith("http://dbpedia.org/resource/")) {
                this.result.add(s);
            } else if (SPARQLUtilities.isDateFromXMLScheme(s)) {
                this.result.add(getDateFromXMLScheme(s));
            } else if (SPARQLUtilities.isStringFromXMLScheme(s)) {
                this.result.add(s.substring(0, s.indexOf("^^")));
            } else if (SPARQLUtilities.isNumberFromXMLScheme(s)) {
                this.result.add(s.substring(0, s.indexOf("^^")));
            } else if (SPARQLUtilities.isDate(s)) {
                this.result.add(getLongDate(s));
            } else {
                this.result.add(s);
            }
        }
    }

    String getLongDate(String s) {
        if (s.length() >= 6 && s.length() <= 10) {
            LocalDate date = null;

            try {
                date = LocalDate.parse(s, dateTimeFormatterLong);
            } catch (DateTimeParseException ignored) {
            }

            try {
                date = LocalDate.parse(s, dateTimeFormatterShortDay);
            } catch (DateTimeParseException ignored) {
            }

            try {
                date = LocalDate.parse(s, dateTimeFormatterShortMonth);
            } catch (DateTimeParseException ignored) {
            }

            try {
                date = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-M-d"));
            } catch (DateTimeParseException ignored) {
            }
            if (date != null) {
                return date.format(dateTimeFormatterLong);
            }
        }
        return s;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public int getAnswerType() {
        return answerType;
    }

    public void setAnswerType(int answerType) {
        this.answerType = answerType;
    }

    private String getDateFromXMLScheme(String s) {
        if (s.contains("-")) {
            try {
                return LocalDate.parse(s, dateTimeFormatterLong).toString();
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDate.parse(s, dateTimeFormatterShortDay).toString();
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDate.parse(s, dateTimeFormatterShortMonth).toString();
            } catch (DateTimeParseException ignored) {
            }
        }
        if (s.contains("^^")) {
            return s.substring(0, s.indexOf("^^"));
        } else {
            return s;
        }
    }

    @Override
    public String toString() {
        return "ResultsetBinding{" +
                "bindings=" + bindings +
                ", result=" + result +
                ", query='" + query + '\'' +
                ", rating=" + rating +
                ", answerType=" + answerType +
                '}';
    }
}
