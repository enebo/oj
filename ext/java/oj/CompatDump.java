package oj;

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
import org.jruby.util.TypeConverter;

import static oj.Options.*;

public class CompatDump extends Dump {
    CompatDump(ThreadContext context, Out out) {
        super(context, out);
    }

    @Override
    protected String modeName() {
        return "compat";
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal obj, int depth) {
        if (obj.respondsTo("to_hash")) {
            dump_to_hash(obj, depth);
        } else if (out.opts.bigdec_as_num == Yes) {
            dump_raw(stringToByteList(obj, "to_s"));
        } else if (obj.respondsTo("as_json")) {
            dump_as_json(obj, depth);
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(obj, "to_json"));
        } else {
            dump_cstr(stringToByteList(obj, "to_s"), false, false);
        }
    }

    @Override
    protected void dump_str(RubyString string) {
        dump_cstr(string.getByteList(), false, false);
    }

    @Override
    protected void dump_time(RubyTime obj, int depth) {
        if (obj.respondsTo("to_hash")) {
            dump_to_hash(obj, depth);
        } else if (obj.respondsTo("as_json")) {
            dump_as_json(obj, depth);
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(obj, "to_json"));
        } else {
            switch (out.opts.time_format) {
                case RubyTime: dump_ruby_time(obj); break;
                case XmlTime: dump_xml_time(obj); break;
                case UnixZTime: _dump_time(obj, true); break;
                case UnixTime:
                default: _dump_time(obj, false); break;
            }
        }
    }

    @Override
    protected void dump_sym(RubySymbol symbol) {
        dump_cstr(((RubyString) symbol.to_s()).getByteList(), false, false);
    }

    @Override
    protected void dump_class(RubyModule clas) {
        dump_cstr(new ByteList(clas.getName().getBytes()), false, false);
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth, IRubyObject[] args) {
        dump_obj_comp(obj, depth, args); // FIXME: is this really common code with obj mode
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth, IRubyObject[] args) {
        dump_obj_comp(obj, depth, args); // FIXME: is this really common code with obj mode
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth, IRubyObject[] args) {
        dump_obj_comp(obj, depth, args); // FIXME: is this really common code with obj mode
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        dump_struct_comp(obj, depth); // FIXME: do we really need to do all this checking....
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        dump_struct_comp(obj, depth); // FIXME: do we really need to do all this checking....
    }

    @Override
    protected void visit_hash(IRubyObject key, IRubyObject value) {
        int depth = out.depth;

        indent(depth, out.opts.dump_opts.hash_nl);

        if (key instanceof RubyString) {
            dump_str((RubyString) key);
        } else if (key instanceof RubySymbol) {
            dump_sym((RubySymbol) key);
        } else {
            dump_cstr(stringToByteList(key, "to_s"), false, false);
        }
        if (out.opts.dump_opts.use) {
            if (out.opts.dump_opts.before_sep != ByteList.EMPTY_BYTELIST) {
                out.append(out.opts.dump_opts.before_sep);
            }
            out.append(':');
            if (out.opts.dump_opts.after_sep != ByteList.EMPTY_BYTELIST) {
                out.append(out.opts.dump_opts.after_sep);
            }
        } else {
            out.append(':');
        }
        dump_val(value, depth, null);
        out.depth = depth;
        out.append(',');
    }

    private void dump_struct_comp(IRubyObject obj, int depth) {
        if (obj.respondsTo("to_hash")) {
            IRubyObject h = obj.callMethod(context, "to_hash");

            dump_hash(TypeConverter.checkHashType(runtime, h), depth);
        } else if (obj.respondsTo("as_json")) {
            IRubyObject aj = obj.callMethod(context, "as_json");

            // Catch the obvious brain damaged recursive dumping.
            if (aj == obj) {
                dump_cstr(stringToByteList(obj, "to_s"), false, false);
            } else {
                dump_val(aj, depth, null);
            }
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(obj, "to_json"));
        } else {
            dump_cstr(stringToByteList(obj, "to_s"), false, false);
        }
    }
}