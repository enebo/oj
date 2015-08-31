package oj;


import org.jruby.RubyString;
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

    public SCParse(ThreadContext context, Options options, IRubyObject handler) {
        super(context, options, handler);

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
    public void appendCStr(ByteList value) {
        if (dispatchArrayAppend) {
            handler.callMethod(context, "array_append",
                    new IRubyObject[] {stack_head_val(), oj_encode(getRuntime().newString(value)) });
        }
    }

    @Override
    public void appendNum(NumInfo ni) {
        if (dispatchArrayAppend) {
            IRubyObject value = ni.toNumber(context);
            handler.callMethod(context, "array_append", new IRubyObject[] {stack_head_val(), value});
        }
    }

    @Override
    public void appendValue(IRubyObject value) {
        if (dispatchArrayAppend) {
            handler.callMethod(context, "array_append", new IRubyObject[] {stack_head_val(), value});
        }
    }

    @Override
    public void addCStr(ByteList value) {
        if (dispatchValueAdd) {
            handler.callMethod(context, "add_value", oj_encode(getRuntime().newString(value)));
        }
    }

    @Override
    public void addNum(NumInfo ni) {
        if (dispatchValueAdd) {
            handler.callMethod(context, "add_value", ni.toNumber(context));
        }
    }

    @Override
    public void addValue(IRubyObject value) {
        if (dispatchValueAdd) {
            handler.callMethod(context, "add_value", value);
        }
    }

    @Override
    public void setCStr(Val kval, ByteList value) {
        if (dispatchHashSet) {
            handler.callMethod(context, "hash_set",
                    new IRubyObject[] { stack_head_val(), calc_hash_key(kval), oj_encode(getRuntime().newString(value)) });
        }
    }

    @Override
    public void setNum(Val kval, NumInfo ni) {
        if (dispatchHashSet) {
            handler.callMethod(context, "hash_set",
                    new IRubyObject[] { stack_head_val(), calc_hash_key(kval), ni.toNumber(context) });
        }
    }

    @Override
    public void setValue(Val kval, IRubyObject value) {
        if (dispatchHashSet) {
            handler.callMethod(context, "hash_set",
                    new IRubyObject[] { stack_head_val(), calc_hash_key(kval), value });
        }
    }

    @Override
    public IRubyObject hashKey(RubyString key) {
        return dispatchHashKey ? handler.callMethod(context, "hash_key", key) : context.nil;
    }

    @Override
    public IRubyObject endArray() {
        return dispatchEndArray ? handler.callMethod(context, "array_end") : context.nil;
    }

    @Override
    public IRubyObject startArray() {
        return dispatchStartArray ? handler.callMethod(context, "array_start") : context.nil;
    }

    @Override
    public IRubyObject endHash() {
        return dispatchEndHash ? handler.callMethod(context, "hash_end") : context.nil;
    }

    @Override
    public IRubyObject startHash() {
        return dispatchStartHash ? handler.callMethod(context, "hash_start") : context.nil;
    }
}
