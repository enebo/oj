package oj.parse;

import org.jruby.util.ByteList;

/**
 * Base class for string/stream parsers.
 */
public abstract class ParserSource {
    protected static final int EOF = 0;

    public int current;

    /**
     * In the string parser this represents the offset since beginning the parse.
     */
    protected int currentOffset = 0;

    abstract void appendTo(ByteList buf, int start);
    abstract int advance();
    abstract int advance(int amount);
    abstract int at(int offset);
    abstract ByteList subStr(int offset, int length);
    abstract boolean startsWith(ByteList str);
    abstract int peek(int amount);

    /**
     * Can this source type determine the line and column offsets?
     * @return true if so
     */
    public boolean canCalculateSourcePositions() {
        return false;
    }

    public int column() {
        throw new RuntimeException("column() not implemented for this type: " + this.getClass());
    }

    public int row() {
        throw new RuntimeException("row() not implemented for this type: " + this.getClass());
    }
}
