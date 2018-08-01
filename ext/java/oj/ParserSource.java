package oj;

import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/1/18.
 */
public abstract class ParserSource {
    public static final int EOF = 0;

    public int current;

    /**
     * In the string parser this represents the offset since beginning the parse.
     */
    public int currentOffset = 0;

    abstract void appendTo(ByteList buf, int start);
    abstract int advance();
    abstract int advance(int amount);
    abstract int at(int offset);
    abstract ByteList subStr(int offset, int length);
    abstract boolean startsWith(ByteList str);
    abstract int peek(int amount);
}
