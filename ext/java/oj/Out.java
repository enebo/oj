package oj;

import java.io.FileOutputStream;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 9/11/15.
 */
public class Out {
    public Options opts;
    public int indent;
    public int depth;
    public int hash_cnt = 0;

    public void append(int aByte) {

    }

    public void append(byte[] bytes) {
    }

    public void append(byte[] bytes, int start, int length) {

    }

    public void append(ByteList bytes) {
    }

    public void append(IRubyObject out) {

    }

    public void append(String string) {

    }

    public int get(int offset) {
        return 0;
    }


    // return curr +- offset char
    public int peek(int offset) {
        return 0;
    }

    public void seek(int offset) {
    }

    public int size() {
        return 0;
    }

    public void write(FileOutputStream f) {

    }
}
