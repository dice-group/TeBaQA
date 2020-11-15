package de.uni.leipzig.tebaqa.queryranking.util;

import de.uni.leipzig.tebaqa.queryranking.model.TripleTemplate;
import de.uni.leipzig.tebaqa.tebaqacommons.model.ResourceCandidate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryRankingUtils {
    public static Pattern BETWEEN_CURLY_BRACES = Pattern.compile("\\{(.*?)\\}");
    private static String SPO_DEFAULT_SEPARATOR = "_";

    public static List<TripleTemplate> extractTripleTemplates(String query) {
        List<String> triples = new ArrayList<>();
        Matcher curlyBracesMatcher = BETWEEN_CURLY_BRACES.matcher(query);
        while (curlyBracesMatcher.find()) {
            String group = curlyBracesMatcher.group().replace("{", "").replace("}", "");
            int lastFoundTripleIndex = 0;
            for (int i = 0; i < group.length(); i++) {
                char currentChar = group.charAt(i);
                //TODO handle triples with predicate-object lists and object lists; see: https://stackoverflow.com/a/18214862/2514164
                if (currentChar == '.' || currentChar == ';' || currentChar == ',') {
                    String possibleTriple = group.substring(lastFoundTripleIndex, i);
                    int openingAngleBrackets = StringUtils.countMatches(possibleTriple, "<");
                    int closingAngleBrackets = StringUtils.countMatches(possibleTriple, ">");
                    if (openingAngleBrackets == closingAngleBrackets) {
                        lastFoundTripleIndex = i;
                        String t = removeDotsFromStartAndEnd(possibleTriple);
                        t = expandCompoundPattern(t, triples);
                        if (!t.isEmpty()) {
                            triples.add(t);
                        }
                    }
                }
            }
            //Add the last triple which may not end with a .
            String t = removeDotsFromStartAndEnd(group.substring(lastFoundTripleIndex, group.length()));
            t = expandCompoundPattern(t, triples);
            if (!t.isEmpty()) {
                triples.add(t);
            }
        }

        List<TripleTemplate> templates = new ArrayList<>(triples.size());
        for (String triplePattern : triples) {
            templates.add(new TripleTemplate(triplePattern.trim().replaceAll("\\s", SPO_DEFAULT_SEPARATOR)));
        }
        return templates;
    }

    private static String expandCompoundPattern(String currentPattern, List<String> previousTriples) {
        // To handle compound patterns which express two patterns with same subject with a semicolon
        // e.g. ?cs res/0 res/1 ; ?rel res/2 ; res/3 ?tel
        if (currentPattern.startsWith(";") && previousTriples.size() > 0) {
            String previousTriple = previousTriples.get(previousTriples.size() - 1); // Get previous triple to retrieve subject from it
            String previousTripleSubject = previousTriple.split("\\s+")[0];

            // Replace leading semicolon in current triple with previous triple's subject
            currentPattern = previousTripleSubject + currentPattern.substring(1);
        }

        return currentPattern;
    }

    private static String removeDotsFromStartAndEnd(String possibleTriple) {
        possibleTriple = possibleTriple.trim();
        if (!possibleTriple.isEmpty()) {
            if (possibleTriple.charAt(0) == '.') {
                possibleTriple = possibleTriple.substring(1).trim();
            }
            if (!possibleTriple.isEmpty() && possibleTriple.charAt(possibleTriple.length() - 1) == '.') {
                possibleTriple = possibleTriple.substring(0, possibleTriple.length() - 1).trim();
            }
        }
        return possibleTriple;
    }

    public static int countResourcesToMatch(Collection<TripleTemplate> patterns) {
        int resourcesToMatch = 0;
        for (TripleTemplate p : patterns) {
            if (p.getSubject().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER)) resourcesToMatch++;
            if (p.getPredicate().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER)) resourcesToMatch++;
            if (p.getObject().startsWith(TripleTemplate.RESOURCE_PREFIX_IDENTIFIER)) resourcesToMatch++;
        }
        return resourcesToMatch;
    }

    public static ResourceCandidate detectCandidateByUri(Collection<? extends ResourceCandidate> candidates, String uri) {
        Optional<? extends ResourceCandidate> detected = candidates.stream().filter(pc -> pc.getUri().equalsIgnoreCase(uri)).findFirst();
        return detected.orElse(null);
    }

    public static boolean hasOverlap(String str1, String str2) {
        return str1.contains(str2) || str2.contains(str1);
    }

}
