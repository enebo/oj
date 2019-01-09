package oj.dump;

import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyArray;
import org.jruby.RubyFloat;
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
import org.jruby.util.Sprintf;

import static oj.parse.NumInfo.OJ_INFINITY;

public class WabDump extends Dump {
    public WabDump(ThreadContext context, OjLibrary oj, Options opts) {
        super(context, oj, opts);
    }

    @Override
    protected void dump_array(RubyArray array, int depth) {
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
    protected void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth) {
        dump_raw(stringToByteList(bigdecimal, "to_s"));
    }

    @Override
    protected void dump_class(RubyModule clas) {
        raise_wab(clas);
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        raise_wab(obj);
    }

    @Override
    protected void dump_float(RubyFloat obj) {
        double d = obj.getDoubleValue();

        if (d == 0.0) {
                append(ZERO_POINT_ZERO);
        } else if (d == OJ_INFINITY || d == -OJ_INFINITY || Double.isNaN(d)) {
            raise_wab(obj);
        } else {
            ByteList buf = new ByteList();
            Sprintf.sprintf(buf, "%0.16g", obj);
            append(buf);
        }
    }

    // FIXME: strict,null,and wab all have same basic body for these except for circ check at top.
    @Override
    protected void dump_hash(IRubyObject obj, int dep) {
        RubyHash hash = (RubyHash) obj;

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
    protected void dump_other(IRubyObject obj, int depth) {
        raise_wab(obj);
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        raise_wab(obj);
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        raise_wab(rational);
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        raise_wab(obj);
    }

    // FIXME: audit and make non-abstract on Dump
    @Override
    protected void dump_str(RubyString string) {
        dump_cstr(string.getByteList(), false, false);
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        raise_wab(obj);
    }

    @Override
    protected void dump_time(RubyTime time, int depth) {
        //FIXME: impl still needed
        super._dump_time(time, true);
    }

    @Override
    protected String modeName() {
        return ":wab";
    }

    private void raise_wab(IRubyObject obj) {
        throw context.runtime.newTypeError("Failed to dump " + obj.getMetaClass() + " Object to JSON in wab mode.\n");
    }

    protected void visit_hash(IRubyObject key, IRubyObject value) {
        int saved_depth = depth;
        if (omit_nil && value.isNil()) return;

        if (!(key instanceof RubySymbol)) {
            throw context.runtime.newTypeError("In :" + modeName() + " mode all Hash keys must be Symbols, not " + key.getMetaClass().getName());
        }
        fill_indent(saved_depth);
        dump_sym((RubySymbol) key);
        append(':');
        dump_val(value, saved_depth);
        append(',');

        depth = saved_depth;
    }
}
