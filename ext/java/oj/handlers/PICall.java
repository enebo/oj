package oj.handlers;

import oj.ParseInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/26/15.
 */
public interface PICall {
    IRubyObject call(ParseInfo pi);
    IRubyObject call(ParseInfo pi, IRubyObject arg1);
}
