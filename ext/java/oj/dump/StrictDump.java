package oj.dump;

import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StrictDump extends Dump {
    StrictDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected void dump_str(RubyString string) {
        dump_cstr(string.getByteList(), false, false);
    }

    @Override
    protected void dump_sym(RubySymbol symbol) {
        raise_strict(symbol);
    }

    @Override
    protected void dump_class(RubyModule clas) {
        raise_strict(clas);
    }

    @Override
    protected String modeName() {
        return "strict";
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dump_time(RubyTime object, int depth) {
        dump_raw(stringToByteList(object, "to_s"));
    }

    @Override
    protected void dump_other(IRubyObject object, int depth, IRubyObject[] args) {
        dump_raw(stringToByteList(object, "to_s"));
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth, IRubyObject[] args) {
        raise_strict(obj);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth, IRubyObject[] args) {
        raise_strict(obj);
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        raise_strict(obj);
    }
}