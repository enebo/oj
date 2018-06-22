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
    
    public StrWriter(ThreadContext context) {
        RubyModule oj = context.runtime.getModule("Oj");
        OjLibrary library = RubyOj.resolveOj(oj);
        this.opts = library.default_options; // FIXME: Clone?
        this.depth = 0;
        this.keyWritten = false;

        this.out.reset();
        this.out.circ_cnt = 0;
        this.out.hash_cnt = 0;
        this.out.opts = this.opts;
        this.out.indent = this.opts.indent;
        this.out.depth = 0;        
    }
}
