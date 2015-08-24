package oj;

/**
 * Created by enebo on 8/24/15.
 */
public enum NextItem {
    NONE(' ', "nothing"), ARRAY_NEW('a', "array element or close"), ARRAY_ELEMENT('e', "array element"),
    ARRAY_COMMA(',', "comma"), HASH_NEW('h', "hash pair or close"), HASH_KEY('k', "hash key"),
    HASH_COLON(':', "colon"),  HASH_VALUE('v', "hash value"), HASH_COMMA('n', "comma");

    private char c;
    private String message;

    NextItem(char c, String message) {
        this.c = c;
        this.message = message;
    }
}
