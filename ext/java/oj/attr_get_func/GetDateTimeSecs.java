package oj.attr_get_func;

import oj.AttrGetFunc;
import org.jruby.RubyNumeric;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 8/21/18.
 */
public class GetDateTimeSecs implements AttrGetFunc {
    // FIXME: conditionally dyndispatch
    @Override
    public IRubyObject execute(ThreadContext context, IRubyObject obj) {
        IRubyObject rsecs = obj.callMethod(context, "sec");
        IRubyObject rfrac = obj.callMethod(context, "sec_fraction");
        long sec = RubyNumeric.num2long(rsecs);
        long num = RubyNumeric.num2long(rfrac.callMethod(context, "numerator"));
        long den = RubyNumeric.num2long(rfrac.callMethod(context, "denominator"));

        num += sec * den;

        return context.runtime.getObject().callMethod("Rational",
                context.runtime.newFixnum(num), context.runtime.newFixnum(den));
    }
}
