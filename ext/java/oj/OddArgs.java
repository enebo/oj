package oj;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 6/20/18.
 */
public class OddArgs {
    public Odd odd;
    public IRubyObject[] args;

    public OddArgs(ThreadContext context, Odd odd) {
        this.odd = odd;

        int count = odd.attrs.length;

        args = new IRubyObject[count];
        for (int i = 0; i < count; i++) {
            args[i] = context.nil;
        }
    }

    public boolean setArg(ByteList key, IRubyObject value) {
        String id = key.toString();  // FIXME: I think this will onyl work for US-ASCII
        int length = odd.attrs.length;

        for (int i = 0; i < length; i++) {
            if (odd.attrs[i].equals(id)) {
                args[i] = value;
                return true;
            }
        }

        return false;
    }
}
