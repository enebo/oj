package oj;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 9/11/15.
 */
public class Leaf {
    public LeafValue value_type;
    public LeafType rtype;
    public ByteList str;
    public IRubyObject value;
    public Leaf[] elements;
    public String key;
}
