package oj.dump;

import oj.OjLibrary;
import oj.Options;
import oj.options.NanDump;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubyTime;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StrictDump extends Dump {
    StrictDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected void dump_array(RubyArray array, int depth) {
        if (opts.circular && check_circular(array) < 0) {
            dump_nil();
            return;
        }

        int d2 = depth + 1;

        append('[');

        if (array.isEmpty()) {
            append(']');
        } else {
            int cnt = array.getLength() - 1;

            for (int i = 0; i <= cnt; i++) {
                indent(d2, opts.dump_opts.array_nl);
                dump_val(array.eltInternal(i), d2);
                if (i < cnt) append(',');
            }
            indent(depth, opts.dump_opts.array_nl);
            append(']');
        }
    }

    @Override
    protected void dump_str(RubyString string) {
        dump_cstr(string.getByteList(), false, false);
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
        // FIXME: This will explode is to_s returns other than string
        dump_raw(((RubyString) obj.callMethod(context, "to_s")).getByteList());
    }

    @Override
    protected void dump_time(RubyTime object, int depth) {
        raise_strict(object);
    }

    @Override
    protected void dump_other(IRubyObject object, int depth) {
        raise_strict(object);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dump_hash(IRubyObject obj, int dep) {
        RubyHash hash = (RubyHash) obj;

        if (opts.circular && check_circular(hash) < 0) {
            dump_nil();
            return;
        }

        if (hash.isEmpty()) {
            append(EMPTY_HASH);
        } else {
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

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        dump_raw(stringToByteList(rational, "to_s"));
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        raise_strict(obj);
    }

    @Override
    protected void dumpNanNanForFloat(IRubyObject value) {
        NanDump nd = opts.dump_opts.nan_dump;

        if (nd == NanDump.AutoNan) nd = NanDump.NullNan;

        switch(nd) {
            case RaiseNan:
            case WordNan:
                raise_strict(value); break;
            case NullNan:
                append(NULL_VALUE); break;
            case HugeNan:
            default:
                append(NAN_NUMERIC_VALUE); break;
        }
    }

    @Override
    protected void dumpInfNanForFloat(IRubyObject value, byte[] inf_value, boolean positive) {
        NanDump nd = opts.dump_opts.nan_dump;

        if (nd == NanDump.AutoNan) nd = NanDump.NullNan;

        switch(nd) {
            case RaiseNan:
            case WordNan:
                raise_strict(value); break;
            case NullNan:
                append(NULL_VALUE); break;
            case HugeNan:
            default:
                append(positive ? INF_VALUE : NINF_VALUE); break;
        }
    }
}