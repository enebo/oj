package oj.rails;

import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;

public class RailsHolder {
    boolean railsHashOpt = false;
    boolean railsArrayOpt = false;
    boolean railsFloatOpt = false;

    Map<IRubyObject, ROpt> ropts = new HashMap<>(4);

}
