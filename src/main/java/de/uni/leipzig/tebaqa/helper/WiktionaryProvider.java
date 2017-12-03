package de.uni.leipzig.tebaqa.helper;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;

import java.io.File;

public class WiktionaryProvider {

    //do not instantiate
    private WiktionaryProvider() {
    }

    public static IWiktionaryEdition getWiktionaryInstance() {
        File wiktionaryDirectory = new File("./src/main/resources/wiktionary/");
        return JWKTL.openEdition(wiktionaryDirectory);
    }
}
