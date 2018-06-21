package oj;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 6/20/18.
 */
public interface AttrGetFunc {
    IRubyObject execute(IRubyObject value);

}
