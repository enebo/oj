package oj.dump;

import oj.Attr;
import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyNumeric;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyTime;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static oj.Options.*;

// FIXME: callsites would help here
// FIXME: bypassing calls when builtin would be faster yet
// FIXME: value boxing could be arity split.
public class CustomDump extends CompatDump {
    private static String[] COMPLEX_METHODS = new String[] {"real", "imag"};
    private static String[] DATE_METHODS = new String[] {"s"};
    private static String[] OPENSTRUCT_METHODS = new String[] {"table"};
    private static String[] RANGE_METHODS = new String[] {"begin", "end", "exclude"};
    private static String[] REGEXP_METHODS = new String[] {"s"};
    private static String[] RATIONAL_METHODS = new String[] {"numerator", "denominator"};
    private static String[] TIME_METHODS = new String[] {"time"};

    CustomDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        code_attrs(obj, COMPLEX_METHODS, callAll(obj, COMPLEX_METHODS), depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    protected void dump_date(IRubyObject obj, int depth) {
        IRubyObject[] values = new IRubyObject[] { obj.callMethod(context, "iso8601") };
        code_attrs(obj, DATE_METHODS, values, depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    protected void dump_datetime(IRubyObject obj, int depth) {
        dump_date(obj, depth);
    }

    protected void dump_openstruct(IRubyObject obj, int depth) {
        code_attrs(obj, OPENSTRUCT_METHODS, callAll(obj, OPENSTRUCT_METHODS), depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        code_attrs(obj, RANGE_METHODS, callAll(obj, RANGE_METHODS), depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_rational(RubyRational obj, int depth) {
        code_attrs(obj, RATIONAL_METHODS, callAll(obj, RATIONAL_METHODS), depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        IRubyObject[] values = new IRubyObject[] { obj.callMethod(context, "to_s") };
        code_attrs(obj, REGEXP_METHODS, values, depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
    }

    @Override
    protected void dump_time(RubyTime obj, int depth) {
        if (opts.create_ok == Yes) {
            code_attrs(obj, TIME_METHODS, callAll(obj, TIME_METHODS), depth, opts.create_ok == Yes, Attr.AttrType.VALUE);
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
