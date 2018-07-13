package oj;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 6/20/18.
 */
public class OddArgs {
    public Odd odd;
    public IRubyObject[] args;

    public boolean setArg(ByteList key, IRubyObject iRubyObject) {
        return false;
    }
}
