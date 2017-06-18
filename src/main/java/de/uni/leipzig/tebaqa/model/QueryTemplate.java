package de.uni.leipzig.tebaqa.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryTemplate {
    private static Logger log = Logger.getLogger(QueryTemplate.class);
    private Pattern KEYWORD_MATCHER = Pattern.compile("\\w{2}+(?:\\s*\\w+)*");

    public void setModifiers(Set<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    private Set<Modifier> modifiers = new HashSet<>();

    public QueryTemplate(String query) {
        //TODO Get every modifier in WHERE clause
        //TODO was passiert bei geschachtelten select where clauseln?

        modifiers.addAll(getModifiers(query.trim()));
    }

    private List<Modifier> getModifiers(String query) {
        List<Modifier> modifiers = new ArrayList<>();
        String remaining = query;
        Matcher keywordMatcherCurrent = KEYWORD_MATCHER.matcher(query);
        while (keywordMatcherCurrent.find()) {
            String currentModifier = keywordMatcherCurrent.group();
            if (currentModifier.equalsIgnoreCase("union")) {
                char[] left = remaining.substring(0, remaining.indexOf(currentModifier)).toCharArray();
                char[] right = remaining.substring(remaining.indexOf(currentModifier) + currentModifier.length(),
                        remaining.length()).toCharArray();

                int lastIndexOfTriple = lastIndexOfTriple(right) + 1;
                modifiers.add(new Modifier(remaining.substring(firstIndexOfTriple(remaining, left),
                        remaining.indexOf(currentModifier) + currentModifier.length())
                        + new String(right).substring(0, lastIndexOfTriple)));
                remaining = remaining.substring(remaining.indexOf(currentModifier) + lastIndexOfTriple,
                        remaining.length());

            }
            Matcher keywordMatcherNext = KEYWORD_MATCHER.matcher(remaining);
            if (keywordMatcherNext.find()) {
                String nextModifier = keywordMatcherNext.group();

                int depthCurrent = getDepth(query, currentModifier);
                int depthNext = getDepth(query, nextModifier);
                int currentModifierIndex = remaining.indexOf(currentModifier);
                if (depthCurrent == depthNext) {
                    //TODO alle parts bis zum nächsten modifier bzw schließenden klammer
                    remaining = remaining.substring(currentModifierIndex + currentModifier.length(), remaining.length());
                    modifiers.add(new Modifier(currentModifier + remaining.substring(0, getLastModifierPartPosition(remaining))));
                } else {
                    int lastModifierPartPosition = getLastModifierPartPosition(query);
                    //TODO position
                    modifiers.add(new Modifier(currentModifier.trim() + remaining.substring(0, lastModifierPartPosition)));
                    remaining = remaining.substring(currentModifierIndex + currentModifier.length(), remaining.length());
                }
            }
        }

        return modifiers;
    }

    private int firstIndexOfTriple(String remaining, char[] left) {
        int braceCount = 0;
        int modifierStart;
        for (modifierStart = left.length; modifierStart >= 0; modifierStart--) {
            if (remaining.charAt(modifierStart) == '}') {
                braceCount++;
            } else if (remaining.charAt(modifierStart) == '{') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
        }
        return modifierStart;
    }

    private int lastIndexOfTriple(char[] right) {
        int braceCount = 0;
        int modifierEnd;
        for (modifierEnd = 0; modifierEnd < right.length; modifierEnd++) {
            if (right[modifierEnd] == '{') {
                braceCount++;
            } else if (right[modifierEnd] == '}') {
                braceCount--;
                if (braceCount == 0) {
                    break;
                }
            }
        }
        return modifierEnd;
    }

    private int getLastModifierPartPosition(String s) {
        int parenthesisCount = 0;
        int bracesCount = 0;
        char[] charArray = s.toCharArray();
        int i;
        for (i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == '(') {
                parenthesisCount++;
            } else if (c == ')') {
                parenthesisCount--;
                if (parenthesisCount < 0) {
                    return i;
                }
            } else if (c == '{') {
                bracesCount++;
            } else if (c == '}') {
                bracesCount--;
                if (bracesCount < 0) {
                    return i;
                }
            } else if (Character.isLetter(c)) {
                return i;
            }
        }
        return i;
    }

    private int getDepth(String fullString, String substring) {
        String[] split = fullString.split(substring);
        String leftPart = split[0];
        int openingLeft = StringUtils.countMatches(leftPart, "(") + StringUtils.countMatches(leftPart, "{");
        int closingLeft = StringUtils.countMatches(leftPart, ")") + StringUtils.countMatches(leftPart, "}");

        return openingLeft - closingLeft;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }
}
