package oj;

import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class Val {
    Object val;
    ByteList key;
    char[]		karray;//[32];
    Object key_val;
    String classname;
    Object oddArgs;

    short clen;

    NextItem next; // ValNext
    char		k1;   // first original character in the key
    char		kalloc;
}
