package oj.options;

/**
 * Created by enebo on 8/23/18.
 */
public enum DumpCaller {
    CALLER_DUMP('d'), CALLER_TO_JSON('t'), CALLER_GENERATE('g');

    private char letter;

    DumpCaller(char letter) {
        this.letter = letter;
    }
}
