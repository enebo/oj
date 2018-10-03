package oj.dump;

import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NullDump extends Dump {
    NullDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth) {
        dump_raw(stringToByteList(bigdecimal, "to_s"));
    }

    @Override
    protected void dump_str(RubyString string) {
        dump_cstr(string.getByteList(), false, false);
    }

    @Override
    protected void dump_time(RubyTime time, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_sym(RubySymbol symbol) {
        dump_nil();
    }

    @Override
    protected void dump_class(RubyModule clas) {
        dump_nil();
    }

    @Override
    protected String modeName() {
        return "null";
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        dump_nil();
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        dump_nil();
    }
}