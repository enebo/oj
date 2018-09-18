package oj.dump;

import oj.Leaf;
import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
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
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import java.io.FileOutputStream;
import java.io.IOException;

public class LeafDump extends Dump {
    public LeafDump(ThreadContext context, OjLibrary oj, Options options) {
        super(context, oj, options);
    }

    // ENTRY POINT
    void dump_leaf(Leaf leaf, int depth) {
        switch (leaf.rtype) {
            case T_NIL:
                dump_nil();
                break;
            case T_TRUE:
                dump_true();
                break;
            case T_FALSE:
                dump_false();
                break;
            case T_STRING:
                dump_leaf_str(leaf);
                break;
            case T_FIXNUM:
                dump_leaf_fixnum(leaf);
                break;
            case T_FLOAT:
                dump_leaf_float(leaf);
                break;
            case T_ARRAY:
                dump_leaf_array(leaf, depth);
                break;
            case T_HASH:
                dump_leaf_hash(leaf, depth);
                break;
            default:
                throw context.runtime.newTypeError("Unexpected type " + leaf.rtype);
        }
    }

    public ByteList leafToJSON(Leaf leaf) {
        dump_leaf(leaf, 0);

        return buf;
    }

    public void leafToFile(Leaf leaf, String path) {
        leafToJSON(leaf);
        FileOutputStream f = null;

        try {
            f = new FileOutputStream(path);

            write(f);
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        } finally {
            if (f != null) {
                try { f.close(); } catch (IOException e) {}
            }
        }
    }

    protected void dump_leaf_array(Leaf leaf, int depth) {
        int	d2 = depth + 1;

        append('[');
        if (leaf.hasElements()) {
            boolean first = true;
            for (Leaf element: leaf.elements) {
                if (!first) append(',');
                fill_indent(d2);
                dump_leaf(element, d2);
                first = false;
            }
            fill_indent(depth);
        }
        append(']');
    }

    protected void dump_leaf_fixnum(Leaf leaf) {
        switch (leaf.value_type) {
            case STR_VAL:
                append(leaf.str);
                break;
            case RUBY_VAL:
                if (leaf.value instanceof RubyBignum) {
                    dump_bignum((RubyBignum) leaf.value);
                } else {
                    dump_fixnum((RubyFixnum) leaf.value);
                }
                break;
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    protected void dump_leaf_float(Leaf leaf) {
        switch (leaf.value_type) {
            case STR_VAL:
                append(leaf.str);
                break;
            case RUBY_VAL:
                dump_float((RubyFloat) leaf.value);
                break;
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    protected void dump_leaf_hash(Leaf leaf, int depth) {
        int	d2 = depth + 1;

        append('{');
        if (leaf.hasElements()) {
            boolean first = true;
            for (Leaf element: leaf.elements) {
                if (!first) append(',');
                fill_indent(d2);
                dump_cstr(element.key, false, false);
                append(':');
                dump_leaf(element, d2);
                first = false;
            }
            fill_indent(depth);
        }
        append('}');
    }

    protected void dump_leaf_str(Leaf leaf) {
        switch (leaf.value_type) {
            case STR_VAL:
                dump_cstr(leaf.str, false, false);
                break;
            case RUBY_VAL: {
                // FIXME: I think this will always be a string or raise here.
                RubyString value = (RubyString) TypeConverter.checkStringType(context.runtime, leaf.value);

                value = StringSupport.checkEmbeddedNulls(context.runtime, value);

                dump_cstr(value.getByteList(), false, false);
                break;
            }
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    @Override
    protected void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth) {
        throw new RuntimeException("dump_bigdecimal called in leafdump");
    }

    @Override
    protected void dump_str(RubyString string) {
        throw new RuntimeException("dump_str called in leafdump");
    }

    @Override
    protected void dump_time(RubyTime time, int depth) {
        throw new RuntimeException("dump_time called in leafdump");
    }

    @Override
    protected void dump_sym(RubySymbol symbol) {
        throw new RuntimeException("dump_sym called in leafdump");
    }

    @Override
    protected void dump_class(RubyModule clas) {
        throw new RuntimeException("dump_class called in leafdump");
    }

    @Override
    protected String modeName() {
        return "leaf";
    }

    @Override
    protected void dump_other(IRubyObject obj, int depth) {
        throw new RuntimeException("dump_other called in leafdump");
    }

    @Override
    protected void dump_complex(IRubyObject obj, int depth) {
        throw new RuntimeException("dump_complex called in leafdump");
    }

    @Override
    protected void dump_regexp(IRubyObject obj, int depth) {
        throw new RuntimeException("dump_regexp called in leafdump");
    }

    @Override
    protected void dump_range(RubyRange obj, int depth) {
        throw new RuntimeException("dump_range called in leafdump");
    }

    @Override
    protected void dump_rational(RubyRational rational, int depth) {
        throw new RuntimeException("dump_rational called in leafdump");
    }

    @Override
    protected void dump_struct(RubyStruct obj, int depth) {
        throw new RuntimeException("dump_struct called in leafdump");
    }
}
