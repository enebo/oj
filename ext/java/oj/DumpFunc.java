package oj;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/23/18.
 */
public interface DumpFunc {
    void run(IRubyObject obj, int depth, Out out, boolean as_ok);
}
