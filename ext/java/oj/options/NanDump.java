package oj.options;

/**
 * Created by enebo on 8/23/18.
 */
public enum NanDump {
    AutoNan('a'), NullNan('n'), HugeNan('h'), WordNan('w'), RaiseNan('r');

    private char letter;

    NanDump(char letter) {
        this.letter = letter;
    }
}
