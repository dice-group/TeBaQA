package de.uni.leipzig.tebaqa.preprocessing;

import de.tudarmstadt.ukp.jwktl.JWKTL;
import de.tudarmstadt.ukp.jwktl.api.IWiktionaryEdition;
import org.apache.log4j.Logger;

import java.io.File;

public class WiktionaryProvider {
    private static IWiktionaryEdition iWiktionaryEdition;
    private static Logger log = Logger.getLogger(WiktionaryProvider.class.getName());

    //do not instantiate
    private WiktionaryProvider() {
    }

    public static IWiktionaryEdition getWiktionaryInstance() {
        File wiktionaryFolder = new File("./src/main/resources/wiktionary/");
        createWiktionaryFiles(wiktionaryFolder);

        if (iWiktionaryEdition == null) {
            iWiktionaryEdition = JWKTL.openEdition(wiktionaryFolder);
        }
        return iWiktionaryEdition;
    }

    private static void createWiktionaryFiles(File outputDirectory) {
        log.info("Creating wiktionary files (this may take a while)...");
        File dumpFile = new File("./src/main/resources/enwiktionary-20171201-pages-articles-multistream.xml.bz2");
        JWKTL.parseWiktionaryDump(dumpFile, outputDirectory, true);
    }
}
