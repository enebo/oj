package oj;

import oj.options.NanDump;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
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
    ObjectDump(ThreadContext context, Out out) {
        super(context, out);
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
            out.append('"');
            out.append(str.unsafeBytes(), str.begin(), str.realSize());
            out.append('"');
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
        out.append('{');
        out.append(C_KEY);
        dump_cstr(new ByteList(clas.getName().getBytes()), false, false);
        out.append('}');
    }

    @Override
    protected void dump_time(RubyTime obj, int depth) {
        out.append('{');
        out.append(T_KEY);
        switch (out.opts.time_format) {
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
        out.append('}');
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
    protected void visit_hash(IRubyObject key, IRubyObject value) {
        if (out.opts.ignore != null && dump_ignore(out, value)) return;
        if (out.omit_nil && value.isNil()) return;

        int		depth = out.depth;

        fill_indent(depth);
        if (key instanceof RubyString) {
            dump_str((RubyString) key);
            out.append(':');
            dump_val(value, depth, null);
        } else if (key instanceof RubySymbol) {
            dump_sym((RubySymbol) key);
            out.append(':');
            dump_val(value, depth, null);
        } else {
            int	d2 = depth + 1;
            int	i;
            boolean	started = false;
            int	b;

            out.append('"');
            out.append('^');
            out.append('#');
            out.hash_cnt++;
            for (i = 28; 0 <= i; i -= 4) {
                b = ((out.hash_cnt >> i) & 0x0000000F);
                if ('\0' != b) {
                    started = true;
                }
                if (started) {
                    out.append(hex_chars.charAt(b));
                }
            }
            out.append('"');
            out.append(':');
            out.append('[');
            fill_indent(d2);
            dump_val(key, d2, null);
            out.append(',');
            fill_indent(d2);
            dump_val(value, d2, null);
            fill_indent(depth);
            out.append(']');
        }
        out.depth = depth;
        out.append(',');
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal obj, int depth) {
        ByteList str = stringToByteList(obj, "to_s");
        if (out.opts.bigdec_as_num != No) {
            dump_raw(str);
        } else if (INFINITY.equals(str)) {
            dump_raw(nan_str(obj, out.opts.dump_opts.nan_dump, out.opts.mode, true));
        } else if (NINFINITY.equals(str)) {
            dump_raw(nan_str(obj, out.opts.dump_opts.nan_dump, out.opts.mode, false));
        } else {
            dump_cstr(str, false, false);
        }
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth, IRubyObject[] args) {
        long id = check_circular(obj, out);

        if (0 <= id) dump_obj_attrs(obj, obj.getMetaClass(), id, depth);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth, IRubyObject[] args) {
        // FIXME: do we really need to do all this checking....
        dump_obj_comp(obj, depth, args);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth, IRubyObject[] args) {
        // FIXME: do we really need to do all this checking....
        dump_obj_comp(obj, depth, args);
    }

    @Override
    protected void dump_range(RubyRange range, int depth) {
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        out.append('{');
        fill_indent(d2);
        out.append(U_KEY);
        fill_indent(d3);
        out.append('"');
        out.append(range.getMetaClass().getName());
        out.append('"');
        out.append(',');
        dump_val(range.begin(context), d3, null);
        out.append(',');
        dump_val(range.end(context), d3, null);
        out.append(',');
        if (range.exclude_end_p().isTrue()) {
            dump_true();
        } else {
            dump_false();
        }
        out.append(']');
        out.append('}');
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        String class_name = obj.getMetaClass().getName();
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        out.append('{');
        fill_indent(d2);
        out.append(U_KEY);
        if (class_name.charAt(0) == '#') {
            RubyArray ma = obj.members();

            int	cnt = ma.size();

            out.append('[');
            for (int i = 0; i < cnt; i++) {
                RubySymbol name = (RubySymbol) ma.eltOk(i); // struct forces all members to be symbols

                if (0 < i) {
                    out.append(',');
                }
                out.append('"');
                out.append(name.asString().getByteList());
                out.append('"');
            }
            out.append(']');
        } else {
            fill_indent(d3);
            out.append('"');
            out.append(class_name);
            out.append('"');
        }
        out.append(',');

        boolean first = true;
        for (Object n: obj.members()) {
            IRubyObject name = (IRubyObject) n;

            if (first) {
                first = false;
            } else {
                out.append(',');
            }

            fill_indent(d3);
            dump_val(obj.aref(name), d3, null);
        }

        out.append(']');
        out.append('}');
    }

    private static boolean isEscapeString(ByteList str) {
        if (str.realSize() >=2 ) {
            int s = str.get(0);
            if (s == ':') return true;
            if (s == '^'){
                s = str.get(1);
                return s == 'r' || s == 'i';
            }
        }
        return false;
    }

    private static byte[] nan_str(IRubyObject value, NanDump nd, char mode, boolean positive) {
        if (nd == NanDump.AutoNan) {
            switch (mode) {
                case CompatMode: nd = NanDump.WordNan; break;
                case StrictMode: nd = NanDump.RaiseNan; break;
            }
        }
        switch(nd) {
            case RaiseNan: raise_strict(value); break;
            case WordNan: return positive ? INFINITY_VALUE : NINFINITY_VALUE;
            case NullNan: return NULL_VALUE;
            case HugeNan:
                return INF_VALUE;
        }

        return null; // C source does this but this I believe will crash in both impls...let's see....
    }
}