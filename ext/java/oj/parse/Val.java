package oj.parse;

import oj.NextItem;
import oj.OddArgs;
import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class Val {
    IRubyObject val;
    ByteList key;
    IRubyObject key_val;
    ByteList classname;
    RubyClass clas;
    OddArgs oddArgs;
    NextItem next; // ValNext
    byte k1;   // first original character in the key

    public Val(IRubyObject val, NextItem next) {
        this.val = val;
        this.next = next;
    }

    // FIXME: mbc keys will not work on 9.2.x (and partially on 9.1.x)
    public String keyAsInstanceVariableId() {
        return "@" + key;
    }

    public String toString() {
        return "key: " + key + ", key_val: " + key_val + ", val: " + val;
    }
}
