package oj;

/**
 * Created by enebo on 9/11/15.
 */
public class DumpType {
    public static DumpType ArrayNew = new DumpType('A');
    public static DumpType ObjectType = new DumpType('O');
    public static DumpType ArrayType = new DumpType('a');
    public static DumpType ObjectNew = new DumpType('o');

    public char label;

    DumpType(char label) {
        this.label = label;
    }
}
