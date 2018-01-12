package de.uni.leipzig.tebaqa.helper;

import edu.cmu.lti.jawjaw.pobj.POS;
import org.jetbrains.annotations.Nullable;

public class PosTransformation {

    @Nullable
    public static POS transform(String pos) {
        POS currentWordPOS;
        if (pos.toLowerCase().startsWith("a")) {
            currentWordPOS = POS.a;
        } else if (pos.toLowerCase().startsWith("r")) {
            currentWordPOS = POS.r;
        } else if (pos.toLowerCase().startsWith("n")) {
            currentWordPOS = POS.n;
        } else if (pos.toLowerCase().startsWith("v")) {
            currentWordPOS = POS.v;
        } else {
            //TODO return empty map?
            return null;
        }
        return currentWordPOS;
    }
}
