package oj.rails;

import oj.OjLibrary;
import oj.RubyOj;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyOjRailsEncoderReplacements {

    @JRubyMethod(name="use_standard_json_time_format=")
    public static IRubyObject set_use_standard_json_time_format(ThreadContext context, IRubyObject self, IRubyObject state) {
        state = context.runtime.newBoolean(state.isTrue());
        self.getInstanceVariables().setInstanceVariable("@use_standard_json_time_format", state);

        return state;
    }

    @JRubyMethod(name="escape_html_entities_in_json=")
    public static IRubyObject set_escape_html_entities_in_json(ThreadContext context, IRubyObject self, IRubyObject state) {
        state = context.runtime.newBoolean(state.isTrue());
        self.getInstanceVariables().setInstanceVariable("@set_escape_html_entities_in_json", state);
        RubyOj.oj(context).getRailsHolder().escapeHTML = state.isTrue();

        return state;
    }
    @JRubyMethod(name="time_precision=")
    public static IRubyObject set_time_precision(ThreadContext context, IRubyObject self, IRubyObject prec) {
        self.getInstanceVariables().setInstanceVariable("@set_time_precision", prec);
        OjLibrary.default_options.sec_prec = prec.convertToInteger().getIntValue();

        return prec;
    }
}
