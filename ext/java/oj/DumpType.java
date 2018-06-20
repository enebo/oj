package oj;

/**
 * Created by enebo on 9/11/15.
 */
public enum DumpType {
    ArrayNew('A'), ObjectType('O'), ArrayType('a'), ObjectNew('o');

    public char label;

    DumpType(char label) {
        this.label = label;
    }
}
