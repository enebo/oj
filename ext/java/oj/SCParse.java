package oj;


import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

// FIXME: Can cache method lookups
// FIXME: Can use helpers to create unboxed call paths
/**
 * Created by enebo on 8/30/15.
 */
public class SCParse extends Parse {
    private final boolean dispatchStartArray;
    private final boolean dispatchEndArray;
    private final boolean dispatchStartHash;
    private final boolean dispatchEndHash;
    private final boolean dispatchHashKey;
    private final boolean dispatchHashSet;
    private final boolean dispatchArrayAppend;
    private final boolean dispatchValueAdd;

    public SCParse(ParserSource source, ThreadContext context, Options options, IRubyObject handler) {
        super(source, context, options, handler);

        dispatchStartHash = handler.respondsTo("hash_start");
        dispatchEndHash = handler.respondsTo("hash_end");
        dispatchHashKey = handler.respondsTo("hash_key");
        dispatchStartArray = handler.respondsTo("array_start");
        dispatchEndArray = handler.respondsTo("array_end");
        dispatchHashSet = handler.respondsTo("hash_set");
        dispatchArrayAppend = handler.respondsTo("array_append");
        dispatchValueAdd = handler.respondsTo("add_value");
    }


    @Override
    public void arrayAppendCStr(ByteList value, int orig) {
        if (dispatchArrayAppend) {
            Helpers.invoke(context, handler, "array_append",
                    stack_head_val(), oj_encode(getRuntime().newString(value)));
        }
    }

    @Override
    public void appendNum(NumInfo ni) {
        if (dispatchArrayAppend) {
            IRubyObject value = ni.toNumber(context);
            Helpers.invoke(context, handler, "array_append", stack_head_val(), value);
        }
    }

    @Override
    public void appendValue(IRubyObject value) {
        if (dispatchArrayAppend) {
            Helpers.invoke(context, handler, "array_append", stack_head_val(), value);
        }
    }

    @Override
    public void addCStr(ByteList value, int orig) {
        if (dispatchValueAdd) {
            Helpers.invoke(context, handler, "add_value", oj_encode(getRuntime().newString(value)));
        }
    }

    @Override
    public void addNum(NumInfo ni) {
        if (dispatchValueAdd) {
            Helpers.invoke(context, handler, "add_value", ni.toNumber(context));
        }
    }

    @Override
    public void addValue(IRubyObject value) {
        if (dispatchValueAdd) {
            Helpers.invoke(context, handler, "add_value", value);
        }
    }

    public void setCStr(Val parent, int start, int length) {
        if (dispatchHashSet) {
            setCStr(parent, source.subStr(start, length), start);
        }
    }

    @Override
    public void setCStr(Val kval, ByteList value, int orig) {
        if (dispatchHashSet) {
            Helpers.invoke(context, handler, "hash_set",
                    stack_head_val(), calc_hash_key(kval), oj_encode(getRuntime().newString(value)));
        }
    }

    @Override
    public void setNum(Val kval, NumInfo ni) {
        if (dispatchHashSet) {
            Helpers.invoke(context, handler, "hash_set",
                    stack_head_val(), calc_hash_key(kval), ni.toNumber(context));
        }
    }

    @Override
    public void setValue(Val kval, IRubyObject value) {
        if (dispatchHashSet) {
            Helpers.invoke(context, handler, "hash_set",
                     stack_head_val(), calc_hash_key(kval), value);
        }
    }

    @Override
    public IRubyObject hashKey(int start, int length) {
        return dispatchHashKey ? hashKey(source.subStr(start, length)) : undef;
    }

    @Override
    public IRubyObject hashKey(ByteList key) {
        return dispatchHashKey ? Helpers.invoke(context, handler, "hash_key", getRuntime().newString(key)) : context.nil;
    }

    @Override
    public IRubyObject endArray() {
        return dispatchEndArray ? Helpers.invoke(context, handler, "array_end") : context.nil;
    }

    @Override
    public IRubyObject startArray() {
        return dispatchStartArray ? Helpers.invoke(context, handler, "array_start") : context.nil;
    }

    @Override
    public IRubyObject endHash() {
        return dispatchEndHash ? Helpers.invoke(context, handler, "hash_end") : context.nil;
    }

    @Override
    public IRubyObject startHash() {
        return dispatchStartHash ? Helpers.invoke(context, handler, "hash_start") : context.nil;
    }
}
