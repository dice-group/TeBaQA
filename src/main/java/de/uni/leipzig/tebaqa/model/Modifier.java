package de.uni.leipzig.tebaqa.model;

import java.util.Objects;

public class Modifier {
    private String modifier;

    //TODO Position speichern notwendig?
    public enum Position {
        BETWEEN_ASK_WHERE,
        INSIDE_TRIPLE,
        AFTER_TRIPLE,
        BETWEEN_TRIPLES,
        AS_TRIPLE,
        END_OF_QUERY
    }

    public Modifier(String modifier) {
        this.modifier = modifier.trim();
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    @Override
    public String toString() {
        return "Modifier{" +
                "modifier='" + modifier + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof Modifier)) return false;
        Modifier otherModifier = (Modifier) other;

        return Objects.equals(otherModifier.modifier, this.modifier);
    }

    @Override
    public int hashCode() {
        return this.modifier.length();
    }

}
