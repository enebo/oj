package oj;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class Val {
    IRubyObject val;
    ByteList key;
    char[]		karray;//[32];
    Object key_val;
    String classname;
    Object oddArgs;

    short clen;

    NextItem next; // ValNext
    int		k1;   // first original character in the key
    char		kalloc;

    public Val(IRubyObject val, NextItem next) {
        this.val = val;
        this.next = next;
    }
}
