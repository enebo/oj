package oj.rails;

import oj.RubyMimic;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyOjRailsDecoderReplacements {
    @JRubyMethod(rest = true)
    public static IRubyObject parse(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return RubyMimic.parse(context, self, args);
    }
}
