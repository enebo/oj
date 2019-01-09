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
}