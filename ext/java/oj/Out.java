package oj;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 9/11/15.
 */
public class Out {
    public Options opts;
    public int indent;
    public int depth;
    public int hash_cnt = 0;
    public int circ_cnt = 0;
    public Map<Object,Integer> circ_cache = null;
    public ByteList buf;
    public OjLibrary oj;

    public Out(OjLibrary oj) {
        reset();
        this.oj = oj;
    }

    public Out(OjLibrary oj, Options opts) {
        reset();
        circ_cnt = 0;
        hash_cnt = 0;
        this.opts = opts;
        indent = opts.indent;
        depth = 0;
        this.oj = oj;
    }

    public void append(int aByte) {
        buf.append((byte)aByte);
    }

    public void append(byte[] bytes) {
        buf.append(bytes);
    }

    public void append(byte[] bytes, int start, int length) {
        buf.append(bytes, start, length);
    }

    public void append(ByteList bytes) {
        buf.append(bytes);
    }

    public void append(String string) {
        buf.append(string.getBytes());
    }

    public int get(int offset) {
        return buf.get(buf.realSize()+offset);
    }


    // return curr +- offset char
    public int peek(int offset) {
        return buf.get(buf.realSize() - 1 + offset);
    }

    public void pop() {
        buf.realSize(buf.realSize() - 1);
    }

    public int size() {
        return buf.realSize();
    }

    public void write(FileOutputStream f) throws IOException {
        f.write(buf.unsafeBytes(), buf.begin(), buf.realSize());
    }

    // FIXME: If out instances live across many of these we should just reset and not realloc over and over.
    public void new_circ_cache() {
        circ_cache = new HashMap<>();
    }

    public void delete_circ_cache() {
        circ_cache = null;
    }

    public void reset() {
        buf = Parse.newByteList();
    }

    public RubyString asString(ThreadContext context) {
        return context.runtime.newString(buf);
    }
}
