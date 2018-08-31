package oj;

import oj.dump.Dump;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/23/18.
 */
public interface DumpFunc {
    void run(IRubyObject obj, int depth, Dump dump, boolean as_ok);
}
