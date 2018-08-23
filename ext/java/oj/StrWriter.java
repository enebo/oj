package oj;

import oj.options.DumpType;
import org.jruby.runtime.ThreadContext;

import java.util.Stack;

public class StrWriter {
    public Out out;
    public Options opts;
    public int depth;
    public Stack<DumpType> types = new Stack<DumpType>();
    public boolean keyWritten;

    public StrWriter(ThreadContext context, OjLibrary oj) {
        opts = oj.default_options.dup(context);
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
