package de.uni.leipzig.tebaqa.helper;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class WiktionaryProvider {
    private static IWiktionaryEdition iWiktionaryEdition;
    private static Logger log = Logger.getLogger(WiktionaryProvider.class.getName());

    //do not instantiate
    private WiktionaryProvider() {
    }

    public static IWiktionaryEdition getWiktionaryInstance() {
        if (iWiktionaryEdition == null) {
            try {
                File wiktionaryDirectory = new ClassPathResource("wiktionary/").getFile();
                iWiktionaryEdition = JWKTL.openEdition(wiktionaryDirectory);
            } catch (IOException e) {
                log.error("Unable to open wiktionary files!", e);
            }
        }
        return iWiktionaryEdition;
    }
}
