package oj;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.ConvertDouble;

import java.util.ArrayList;
import java.util.List;

import static oj.LeafValue.COL_VAL;
import static oj.LeafValue.RUBY_VAL;
import static oj.LeafValue.STR_VAL;

/**
 * This implementation differs from C in that it does not use a circular linked list.
 */
// FIXME: Profile how expensive using an ArrayList is versus taking this down to a primitive array.
public class Leaf {
    public static final long NUM_MAX = RubyFixnum.MAX;

    public LeafValue value_type;
    public LeafType rtype;
    public ParentType parentType = ParentType.None;
    public ByteList str;
    public IRubyObject value;
    public List<Leaf> elements;
    public ByteList key;
    public int index;

    public Leaf(ThreadContext context, LeafType type) {
        this.rtype = type;

        // We initialize some of these values because I think in some code paths we may
        // not call leaf.value() beforehand?
        switch (type) {
            case T_ARRAY:
            case T_HASH:
                value_type = COL_VAL;
                break;
            case T_NIL:
                value = context.nil;
                value_type = RUBY_VAL;
                break;
            case T_TRUE:
                value = context.tru;
                value_type = RUBY_VAL;
                break;
            case T_FALSE:
                value = context.fals;
                value_type = RUBY_VAL;
                break;
            case T_FIXNUM:
            case T_FLOAT:
            case T_STRING:
            default:
                value_type = STR_VAL;
                break;
        }
    }

    public boolean hasElements() {
        return elements != null;
    }

    // C: fast.c:leaf_value()
    public IRubyObject value(ThreadContext context) {
        if (RUBY_VAL != value_type) {
            value = initializeLeafRubyValue(context);
        }

        return value;
    }

    // C: fast.c:each_value()
    public void each_value(ThreadContext context, Block block) {
        if (COL_VAL == value_type) {
            if (null != elements) {
                for (Leaf element : elements) {
                    element.each_value(context, block);
                }
            }
        } else {
            block.yield(context, value(context));
        }
    }

    // FIXME: We maybe need to tune arraylist size
    // C: fast.c:leaf_append_element
    public void append_array_element(Leaf element, int index) {
        if (elements == null) elements = new ArrayList<>();

        element.index = index;
        element.parentType = ParentType.Array;

        elements.add(element);
    }

    public void append_hash_element(Leaf element, ByteList key) {
        if (elements == null) elements = new ArrayList<>();

        element.key = key;
        element.parentType = ParentType.Hash;

        elements.add(element);
    }


    private IRubyObject initializeLeafRubyValue(ThreadContext context) {
        IRubyObject value;

        switch (rtype) {
            case T_NIL:
                value = context.nil;
                break;
            case T_TRUE:
                value = context.tru;
                break;
            case T_FALSE:
                value = context.fals;
                break;
            case T_FIXNUM:
                value = fixnum_value(context);
                break;
            case T_FLOAT:
                value = float_value(context);
                break;
            case T_STRING:
                value = Parse.oj_encode(context.runtime.newString(str));
                value_type = RUBY_VAL;
                break;
            case T_ARRAY:
                value = array_value(context);
                break;
            case T_HASH:
                value = hash_value(context);
                break;
            default:
                RubyClass error = (RubyClass) ((RubyModule) context.runtime.getObject().getConstant("Oj")).getConstantAt("Error");

                throw context.runtime.newRaiseException(error, "Unexpected type " + rtype);
        }
        return value;
    }

    IRubyObject fixnum_value(ThreadContext context) {
        int s = 0;
        long n = 0;
        boolean neg = false;
        boolean big = false;

        if ('-' == str.get(s)) {
            s++;
            neg = true;
        } else if ('+' == str.get(s)) {
            s++;
        }
        for (; '0' <= str.get(s) && str.get(s) <= '9'; s++) {
            // FIXME: should be getting str.get(s) only once.
            n = n * 10 + (str.get(s) - '0');
            if (NUM_MAX <= n) {
                big = true;
            }
        }
        if (big) {
            // boxing into string because no bytelist-variant.
            return ConvertBytes.byteListToInum19(context.runtime, str, 10, false);
        } else {
            return context.runtime.newFixnum(neg? -n : n);
        }
    }

    IRubyObject float_value(ThreadContext context) {
        return context.runtime.newFloat(ConvertDouble.byteListToDouble19(str, false));
    }


    IRubyObject array_value(ThreadContext context) {
        RubyArray array = context.runtime.newArray();

        if (elements != null) {
            for (Leaf element: elements) {
                array.push(element.value(context));
            }
        }

        return array;
    }

    IRubyObject hash_value(ThreadContext context) {
        RubyHash hash = RubyHash.newHash(context.runtime);

        if (elements != null) {
            for (Leaf element: elements) {
                IRubyObject key = Parse.oj_encode(context.runtime.newString(element.key));
                hash.op_aset(context, key, element.value(context));
            }
        }

        return hash;
    }

    public String toString() {
        return "Leaf(RTYPE: " + rtype + ", VALUE: " + value + ", ELEMENTS#: " + (elements != null ? elements.size() : 0) + ")";
    }
}
