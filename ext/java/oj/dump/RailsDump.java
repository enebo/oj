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
import org.jruby.util.ByteList;

import static oj.Options.JXEsc;
import static oj.Options.No;
import static oj.Options.RailsEsc;

public class RailsDump extends Dump {
    public RailsDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);

        if (oj.getRailsHolder().escapeHTML) {
            opts.escape_mode = JXEsc;
        } else {
            opts.escape_mode = RailsEsc;
        }

        omit_nil = opts.dump_opts.omit_nil;
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth) {
        ByteList str = stringToByteList(bigdecimal, "to_s");
        int length = str.realSize();
        boolean nild = false;

        if (length > 1) {
            int first = str.get(0);
            int second = str.get(1);
            if (first == 'I' || first == 'N' || (first == '-' && second == 'I')) {
                dump_nil();
                nild = true;
            }
        }

        if (!nild) {
            if (opts.bigdec_as_num != No) {
                dump_raw(str);
            } else {
                dump_cstr(str, false, false);
            }
        }
    }

    @Override
    protected void dump_class(RubyModule clas) {
        dump_cstr(new ByteList(clas.getName().getBytes()), false, false);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        dump_as_string(obj, depth);
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth) {
        dump_obj(obj, depth);
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        dump_obj(obj, depth);
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        dump_as_string(rational, depth);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        dump_as_string(obj, depth);
    }

    @Override
    protected void dump_str(RubyString string) {
        ByteList str = string.getByteList();

        dump_cstr(str, false, isEscapeString(str));
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        dump_obj(obj, depth);
    }

    @Override
    protected void dump_time(RubyTime time, int depth) {
        _dump_time(time, true);
    }

    @Override
    protected String modeName() {
        return "rails";
    }

    // FIXME: not right (codes stuff back to compat with _alt calls)
    private void dump_as_string(IRubyObject obj, int depth) {
        dump_obj_comp(obj, depth);
    }

    // FIXME: not right (codes stuff back to compat with _alt calls)
    private void dump_obj(IRubyObject obj, int depth) {
        dump_obj_comp(obj, depth);
    }

    protected void visit_hash(IRubyObject key, IRubyObject value) {
        int saved_depth = depth;
        if (omit_nil && value.isNil()) return;

        if (!(key instanceof RubyString)) {
            key = key.callMethod(context, "to_s");
        }

        if (opts.dump_opts.use) {
            dump_hash_nl_indent(saved_depth);
            if (key instanceof RubyString) {
                dump_str((RubyString) key);
            } else { // FIXME: Report issue since to_s above can return anything so both C and Java ports should crash here.
                dump_sym((RubySymbol) key);
            }
            dump_colon();
        } else {
            fill_indent(saved_depth);
            if (key instanceof RubyString) {
                dump_str((RubyString) key);
            } else {
                dump_sym((RubySymbol) key);
            }
            append(':');
        }
        dump_val(value, saved_depth);
        append(',');

        depth = saved_depth;
    }
}
