package oj.dump;

import oj.Odd;
import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
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

import static oj.Options.*;

public class ObjectDump extends Dump {
    ObjectDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected String modeName() {
        return "strict";
    }

    @Override
    protected void dump_str(RubyString string) {
        ByteList str = string.getByteList();

        boolean escape = isEscapeString(str);
        if (string.isAsciiOnly() && !escape) { // Fast path.  JRuby already knows if it is a clean ASCII string.
            append('"');
            append(str.unsafeBytes(), str.begin(), str.realSize());
            append('"');
        } else {
            dump_cstr(str, false, escape);
        }
    }

    @Override
    protected void dump_sym(RubySymbol symbol) {
        dump_cstr(((RubyString) symbol.to_s()).getByteList(), true, false);
    }

    @Override
    protected void dump_class(RubyModule clas) {
        append('{');
        append(C_KEY);
        dump_cstr(new ByteList(clas.getName().getBytes()), false, false);
        append('}');
    }

    @Override
    protected void dump_time(RubyTime obj, int depth) {
        append('{');
        append(T_KEY);
        switch (opts.time_format) {
            case Options.RubyTime: // Does not output fractional seconds
            case XmlTime:
                dump_xml_time(obj);
                break;
            case UnixZTime:
                _dump_time(obj, true);
                break;
            case UnixTime:
            default:
                _dump_time(obj, false);
                break;
        }
        append('}');
    }

    @Override
    protected void dump_hash(IRubyObject obj, int depth) {
        if (!(obj instanceof RubyHash)) {
            dump_obj_attrs(obj, obj.getMetaClass(), 0, depth);
            return;
        }

        super.dump_hash(obj, depth);
    }

    @Override
    public void dump_val_misc(IRubyObject obj, int depth) {
        RubyClass clas = obj.getMetaClass();
        Odd odd = oj.getOdd(clas);

        if (odd != null) {
            dump_odd(obj, odd, clas, depth + 1);
            return;
        }

        super.dump_val_misc(obj, depth);
    }

    @Override
    protected void visit_hash(IRubyObject key, IRubyObject value) {
        if (opts.ignore != null && dump_ignore(value)) return;
        if (omit_nil && value.isNil()) return;

        int saved_depth = depth;
        fill_indent(saved_depth);
        if (key instanceof RubyString) {
            dump_str((RubyString) key);
            append(':');
            dump_val(value, saved_depth);
        } else if (key instanceof RubySymbol) {
            dump_sym((RubySymbol) key);
            append(':');
            dump_val(value, saved_depth);
        } else {
            int	d2 = saved_depth + 1;
            int	i;
            boolean	started = false;
            int	b;

            append('"');
            append('^');
            append('#');
            hash_cnt++;
            for (i = 28; 0 <= i; i -= 4) {
                b = ((hash_cnt >> i) & 0x0000000F);
                if ('\0' != b) {
                    started = true;
                }
                if (started) {
                    append(hex_chars.charAt(b));
                }
            }
            append('"');
            append(':');
            append('[');
            fill_indent(d2);
            dump_val(key, d2);
            append(',');
            fill_indent(d2);
            dump_val(value, d2);
            fill_indent(saved_depth);
            append(']');
        }
        this.depth = saved_depth;
        append(',');
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal obj, int depth) {
        ByteList str = stringToByteList(obj, "to_s");
        if (opts.bigdec_as_num != No) {
            dump_raw(str);
        } else if (INFINITY.equals(str)) {
            dump_raw(nan_str(obj, opts.dump_opts.nan_dump, opts.mode, true));
        } else if (NINFINITY.equals(str)) {
            dump_raw(nan_str(obj, opts.dump_opts.nan_dump, opts.mode, false));
        } else {
            dump_cstr(str, false, false);
        }
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth) {
        long id = check_circular(obj);

        if (0 <= id) dump_obj_attrs(obj, obj.getMetaClass(), id, depth);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        // FIXME: do we really need to do all this checking....
        dump_obj_comp(obj, depth);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        // FIXME: do we really need to do all this checking....
        dump_obj_comp(obj, depth);
    }

    @Override
    protected void dump_range(RubyRange range, int depth) {
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        append('{');
        fill_indent(d2);
        append(U_KEY);
        fill_indent(d3);
        append('"');
        append(range.getMetaClass().getName());
        append('"');
        append(',');
        dump_val(range.begin(context), d3);
        append(',');
        dump_val(range.end(context), d3);
        append(',');
        if (range.exclude_end_p().isTrue()) {
            dump_true();
        } else {
            dump_false();
        }
        append(']');
        append('}');
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        dump_other(rational, depth);
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        String class_name = obj.getMetaClass().getName();
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        append('{');
        fill_indent(d2);
        append(U_KEY);
        if (class_name.charAt(0) == '#') {
            RubyArray ma = obj.members();

            int	cnt = ma.size();

            append('[');
            for (int i = 0; i < cnt; i++) {
                RubySymbol name = (RubySymbol) ma.eltOk(i); // struct forces all members to be symbols

                if (0 < i) {
                    append(',');
                }
                append('"');
                append(name.asString().getByteList());
                append('"');
            }
            append(']');
        } else {
            fill_indent(d3);
            append('"');
            append(class_name);
            append('"');
        }
        append(',');

        boolean first = true;
        for (Object n: obj.members()) {
            IRubyObject name = (IRubyObject) n;

            if (first) {
                first = false;
            } else {
                append(',');
            }

            fill_indent(d3);
            dump_val(obj.aref(name), d3);
        }

        append(']');
        append('}');
    }
}