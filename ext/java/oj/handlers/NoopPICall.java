package oj.handlers;

import oj.ParseInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/26/15.
 */
public class NoopPICall implements PICall {
    public IRubyObject call(ParseInfo pi) {
        return pi.getContext().nil;
    }

    public IRubyObject call(ParseInfo pi, IRubyObject arg1) {
        return pi.getContext().nil;
    }
}
