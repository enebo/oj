package oj.dump;

import oj.Attr;
import oj.Odd;
import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyComplex;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.ext.date.RubyDate;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.function.Supplier;

import static oj.Options.*;

// FIXME: callsites would help here
// FIXME: bypassing calls when builtin would be faster yet
// FIXME: value boxing could be arity split.
public class CustomDump extends CompatDump {
    private static String[] COMPLEX_METHODS = new String[] {"real", "imag"};
    private static String[] DATE_METHODS = new String[] {"s"};
    private static String[] OPENSTRUCT_METHODS = new String[] {"table"};
    private static String[] RANGE_METHODS = new String[] {"begin", "end", "exclude_end?"};
    private static String[] REGEXP_METHODS = new String[] {"s"};
    private static String[] RATIONAL_METHODS = new String[] {"numerator", "denominator"};
    private static String[] TIME_METHODS = new String[] {"time"};

    CustomDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    public void dump_val(IRubyObject obj, int depth) {
        // FIXME: cache openstruct.
        // FIXME: Should this be kindof?
        // FIXME: Don't like this check before all others but it may be only way of doing this vs openstruct added to base dump_val
        if (context.runtime.getObject().getConstantAt("OpenStruct") == obj.getMetaClass()) {
            dump_openstruct(obj, depth);
        } else if (context.runtime.getObject().getConstantAt("Date") == obj.getMetaClass()) {
            dump_date(obj, depth);
        } else if (context.runtime.getObject().getConstantAt("DateTime") == obj.getMetaClass()) {
            dump_datetime(obj, depth);
        } else {
            super.dump_val(obj, depth);
        }
    }

    public void dump_val_misc(IRubyObject obj, int depth) {
        if (obj instanceof RubyBignum) {
            dump_bignum((RubyBignum) obj);
        } else if (obj.getMetaClass() == context.runtime.getString()) {
            dump_str((RubyString) obj);
        } else if (obj.getMetaClass() == context.runtime.getArray()) {
            dump_array((RubyArray) obj, depth);
        } else if (obj.getMetaClass() == context.runtime.getHash()) {
            dump_hash(obj, depth);
        } else if (obj instanceof RubyComplex) {
            dump_complex(obj, depth);
        } else if (obj instanceof RubyRegexp) {
            dump_regexp(obj, depth);
        } else if (obj instanceof RubyTime) { // T_DATA
            dump_time((RubyTime) obj, depth);
        } else if (obj instanceof RubyBigDecimal) { // T_DATA
            dump_bigdecimal((RubyBigDecimal) obj, depth);
        } else if (obj instanceof RubyRational) {
            dump_rational((RubyRational) obj, depth);
        } else {
            // FIXME: this else section can be another overridable method so this can
            // be supered in at top for object and possibly compat
            RubyClass clas = obj.getMetaClass();
            Odd odd = oj.getOdd(clas);

            if (odd != null) {
                dump_odd(obj, odd, clas, depth + 1);
                return;
            }

            dump_other(obj, depth);
        }
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
    protected void dump_complex(IRubyObject obj, int depth) {
        code_attrs(obj, COMPLEX_METHODS, callAll(obj, COMPLEX_METHODS), depth, opts.create_ok, Attr.AttrType.VALUE);
    }

    protected void dump_date(IRubyObject obj, int depth) {
        if (opts.create_ok) {
            IRubyObject[] values = new IRubyObject[] {obj.callMethod(context, "iso8601")};
            code_attrs(obj, DATE_METHODS, values, depth, opts.create_ok, Attr.AttrType.VALUE);
        } else {
            switch (opts.time_format) {
                case RubyTime:
                case XmlTime:
                    dump_cstr(stringToByteList(obj.callMethod(context, "iso8601"), "to_s"), false, false);
                    break;
                case UnixZTime: {
                    IRubyObject v = obj.callMethod(context, "to_time");

                    // Does not use instanceof since DateTime < Date.
                    if (obj.getMetaClass() == context.runtime.getObject().getConstantAt("Date")) {
                        _dump_time(utcOnDate(v), false);
                    } else {
                        _dump_time(v, true);
                    }
                    break;
                }
                default: {
                    IRubyObject v = obj.callMethod(context, "to_time");

                    if (obj.getMetaClass() == context.runtime.getObject().getConstantAt("Date")) v = utcOnDate(v);
                    _dump_time(v, false);
                    break;
                }
            }
        }
    }

    // FIXME: Can use non-dyn dispatch if none of these methods have been overridden.
    private IRubyObject utcOnDate(IRubyObject object) {
        // Original code: Does this actually change anything?
        //return object.callMethod(context, "utc").callMethod(context, "+", object.callMethod(context, "utc_offset"));
        return object.callMethod(context, "+", object.callMethod(context, "utc_offset"));
    }

    protected void dump_datetime(IRubyObject obj, int depth) {
        dump_date(obj, depth);
    }

    @Override
    protected void dump_hash(IRubyObject object, int dep) {
        if (!(object instanceof RubyHash)) {
            dump_obj_attrs(object, object.getMetaClass(), 0, dep);
            return;
        }

        RubyHash hash = (RubyHash) object;

        if (hash.isEmpty()) {
            append(EMPTY_HASH);
        } else {
            long id = check_circular(hash);
            if (id < 0) {
                append("null");
                return;
            }

            append('{');
            depth = dep + 1;
            hash.visitAll(context,
                    new RubyHash.VisitorWithState<Dump>() {
                        @Override
                        public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Dump dump) {
                            visit_hash(key, value);
                        }
                    }, this);
            if (',' == get(-1)) pop(); // backup to overwrite last comma
            indent(dep, opts.dump_opts.hash_nl);
            append('}');
        }
    }

    protected void dump_openstruct(IRubyObject obj, int depth) {
        code_attrs(obj, OPENSTRUCT_METHODS, callAll(obj, OPENSTRUCT_METHODS), depth, opts.create_ok, Attr.AttrType.VALUE);
    }

    // cext: dump_common
    @Override
    protected void dump_other(IRubyObject obj, int depth) {
        if (opts.to_json && obj.respondsTo("to_json")) {
            trace("to_json", () ->  append(stringToByteList(obj, "to_json")));
        } else if (opts.as_json && obj.respondsTo("as_json")) {
            IRubyObject aj = trace("as_json", () -> obj.callMethod(context, "as_json"));

            if (aj == obj) {
                dump_cstr(stringToByteList(obj, "to_s"), false, false);
            } else {
                dump_val(aj, depth);
            }
        } else if (opts.to_hash && obj.respondsTo("to_hash")) {
            IRubyObject h = obj.callMethod(context, "to_hash");

            if (h instanceof RubyHash) {
                dump_hash(h, depth);
            } else {
                dump_val(h, depth);
            }
        } else {
            dump_obj_attrs(obj, obj.getMetaClass(), check_circular(obj), depth);
        }
    }

    private IRubyObject trace(String to_json, Supplier<IRubyObject> to_json1) {
        return to_json1.get();
    }
    private void trace(String to_json, Runnable to_json1) {
        to_json1.run();
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        code_attrs(obj, RANGE_METHODS, callAll(obj, RANGE_METHODS), depth, opts.create_ok, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_rational(RubyRational obj, int depth) {
        code_attrs(obj, RATIONAL_METHODS, callAll(obj, RATIONAL_METHODS), depth, opts.create_ok, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        IRubyObject[] values = new IRubyObject[] { obj.callMethod(context, "to_s") };
        code_attrs(obj, REGEXP_METHODS, values, depth, opts.create_ok, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_time(RubyTime obj, int depth) {
        if (opts.create_ok) {
            code_attrs(obj, TIME_METHODS, new IRubyObject[] { obj }, depth, true, Attr.AttrType.TIME);
        } else {
            switch (opts.time_format) {
                case Options.RubyTime:	dump_ruby_time(obj);	break;
                case XmlTime:	dump_xml_time(obj);	break;
                case UnixZTime:	super._dump_time(obj, true);	break;
                case UnixTime:
                default:	super._dump_time(obj, false);	break;
            }
        }
    }

    private IRubyObject[] callAll(IRubyObject obj, String[] attributes) {
        int length = attributes.length;
        IRubyObject[] values = new IRubyObject[length];

        for (int i = 0; i < length; i++) {
            values[i] = obj.callMethod(context, attributes[i]);
        }

        return values;
    }

    protected void code_attrs(IRubyObject obj, String[] attrs, IRubyObject[] values,
                              int depth, boolean with_class, Attr.AttrType type) {
        int d2 = depth + 1;
        int d3 = d2 + 1;

        append('{');
        boolean no_comma = with_class ? code_attrs_with_class(obj, d2) : true;

        int length = attrs.length;
        for (int i = 0; i < length; i++) {
            if (no_comma) {
                no_comma = false;
            } else {
                append(',');
            }
            fill_indent(d2);
            append('"');
            append(attrs[i]);
            append('"');
            dump_colon();

            switch (type) {
                case VALUE: dump_val(values[i], d3); break;
                case NUMBER: dump_long(RubyNumeric.fix2long(values[i])); break;
                case TIME:
                    switch (opts.time_format) {
                        case RubyTime:
                            dump_ruby_time(values[i]);
                            break;
                        case XmlTime:
                            dump_xml_time(values[i]);
                            break;
                        case UnixZTime:
                            _dump_time(values[i], true);
                            break;
                        case UnixTime:
                        default:
                            _dump_time(values[i], false);
                            break;
                    }
                    break;
            }
        }
        fill_indent(depth);
        append('}');
    }

    @Override
    protected void visit_hash(IRubyObject key, IRubyObject value) {
        int	saved_depth = this.depth; // FIXME: can we just pass this around vs saving as temporary field?  I think this is artifact of MRI callback api for hashes.

        if (dump_ignore(value)) return;
        if (omit_nil && value == context.nil) return;

        if (!opts.dump_opts.use) {
            fill_indent(saved_depth);
        } else {
            dump_hash_nl_indent(saved_depth);
        }

        if (key instanceof RubyString) {
            dump_str((RubyString) key);
        } else if (key instanceof RubySymbol) {
            dump_sym((RubySymbol) key);
        } else {
            // FIXME: Can we guarantee whatever is key here has to_s which returns a string? I think oj will TypeError unknown encoding but not crash
            dump_str((RubyString) key.callMethod(context, "to_s"));
        }

        if (!opts.dump_opts.use) {
            append(':');
        } else {
            dump_colon();
        }
        dump_val(value, depth);
        depth = saved_depth;
        append(',');
    }

    private boolean code_attrs_with_class(IRubyObject obj, int depth) {
        fill_indent(depth);
        append('"');
        append(opts.create_id);
        append('"');
        dump_colon();
        append('"');
        append(obj.getMetaClass().getName());
        append('"');

        return false;
    }
}
