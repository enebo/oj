package oj;

import org.jruby.RubyClass;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 6/20/18.
 */
public class Odd {
    String classname;
    RubyClass clas;
    IRubyObject createObj;
    String createOp;
    String[] attr_names;
    String[] attrs;
    AttrGetFunc[] attrFuncs;
}
