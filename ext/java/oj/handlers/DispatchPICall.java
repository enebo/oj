package oj.handlers;

import oj.ParseInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/26/15.
 */
public class DispatchPICall implements PICall {
    private String method;
    public DispatchPICall(String method) {
        this.method = method;
    }

    public IRubyObject call(ParseInfo pi) {
        return pi.handler.callMethod(pi.getContext(), method);
    }

    public IRubyObject call(ParseInfo pi, IRubyObject arg1) {
        return pi.handler.callMethod(pi.getContext(), method, arg1);
    }
}
