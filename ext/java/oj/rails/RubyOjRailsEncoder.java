package oj.rails;

import oj.OjLibrary;
import oj.Options;
import oj.RubyOj;
import oj.dump.RailsDump;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class RubyOjRailsEncoder extends RubyObject {
    Options options = null;
    IRubyObject arg = null;

    public static final ObjectAllocator INSTANCE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public RubyOjRailsEncoder allocate(Ruby runtime, RubyClass klass) {
            return new RubyOjRailsEncoder(runtime, klass);
        }
    };

    protected RubyOjRailsEncoder(Ruby runtime, RubyClass meta) {
        super(runtime, meta);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        this.options = OjLibrary.getDefaultOptions(context);
        this.arg = context.nil;

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject options) {
        initialize(context);

        RubyOj.parse_options(context, options, this.options);

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context, IRubyObject obj) {
        OjLibrary oj = RubyOj.oj(context);
        Options options = OjLibrary.getDefaultOptions(context);

        ByteList result;
        if (!arg.isNil()) {
            result = new RailsDump(context, oj, options).obj_to_json_using_params(obj, new IRubyObject[] { arg });
        } else {
            result = new RailsDump(context, oj, options).obj_to_json(obj);
        }

        return context.runtime.newString(result); // FIXME: should use oj_encode.

    }

    @JRubyMethod
    public IRubyObject optimize(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject deoptimize(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "deoptimized?")
    public IRubyObject optimized_p(ThreadContext context) {
        return context.nil;
    }
}
