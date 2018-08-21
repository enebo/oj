package oj;

import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;

import java.util.Stack;

/**
 * Created by enebo on 9/11/15.
 */
public class StrWriter {
    public Stack<DumpType> types = new Stack<DumpType>();
    public boolean keyWritten;
    public Options opts;
    public Out out;
    public int depth;
    
    public StrWriter(ThreadContext context, OjLibrary oj) {
        opts = oj.default_options.dup();
        depth = 0;
        keyWritten = false;
        out = new Out(oj, opts);
    }

    /**
     * We are using Java standard library Stack which throws on empty stack.  This
     * method protects against that and returns null instead of an exception.
     */
    public DumpType peekTypes() {
        return types.isEmpty() ? null : types.peek();
    }
}
