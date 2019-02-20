package oj.rails;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyOjRailsEncoder extends RubyClass {
    public static final ObjectAllocator INSTANCE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public RubyOjRailsEncoder allocate(Ruby runtime, RubyClass klass) {
            return new RubyOjRailsEncoder(runtime);
        }
    };

    protected RubyOjRailsEncoder(Ruby runtime) {
        super(runtime);
    }

    @JRubyMethod
    public IRubyObject encode(ThreadContext context, IRubyObject obj) {
        return context.nil;
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
