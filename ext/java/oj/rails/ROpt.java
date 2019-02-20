package oj.rails;

import oj.DumpFunc;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/23/18.
 */
public class ROpt {
    DumpFunc dump;
    boolean on;

    public ROpt(DumpFunc dump, boolean on) {
        this.dump = dump;
        this.on = on;
    }
}
