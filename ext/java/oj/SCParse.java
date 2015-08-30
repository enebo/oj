package oj;

import jnr.posix.util.Platform;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.Options.*;

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
    public IRubyObject parse(IRubyObject[] args, ByteList json, boolean yieldOk, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject	input;
        IRubyObject result;
        int			line = 0;

        if (args.length < 1) {
            throw getRuntime().newArgumentError("Wrong number of arguments to parse.");
        }
        input = args[0];
        if (2 == args.length) {
            RubyOj.oj_parse_options(context, args[1], options);
        }
        if (yieldOk && block.isGiven()) {
            proc = block;
        } else {
            proc = Block.NULL_BLOCK;
        }
        if (null != json) {
            this.json = json;
        } else if (input instanceof RubyString) {
            oj_pi_set_input_str(((RubyString) input).getByteList());
        } else if (nilValue() == input && Yes == options.nilnil) {
            return nilValue();
        } else {
            RubyModule clas = input.getMetaClass();

            if (runtime.getClass("StringIO") == clas) {
                input = input.callMethod(context, "string");
            } else if (!Platform.IS_WINDOWS && runtime.getFile() == clas && 0 == input.callMethod(context, "pos").convertToInteger().getLongValue()) {
                input = ((RubyFile) input).read(context);
            } else if (input.respondsTo("read")) {
                throw runtime.newArgumentError("FIXME: No streaming parser");
                // use stream parser instead
                // FIXME:
                //return oj_pi_sparse(args, pi, 0);
            } else {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }

            if (!(input instanceof RubyString)) {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }
            this.json = ((RubyString) input).getByteList();
        }

        // FIXME:
        /*
        if (Yes == options.circular) {
            circ_array = oj_circ_array_new();
        } else {
            circ_array = null;
        }*/

        protect_parse();

        result = stack_head_val();
        if (!err_has()) {
            // If the stack is not empty then the JSON terminated early.
            Val	v;

            if (null != (v = stack_peek())) {
                switch (v.next) {
                    case ARRAY_NEW:
                    case ARRAY_ELEMENT:
                    case ARRAY_COMMA:
                        setError("Array not terminated");
                        break;
                    case HASH_NEW:
                    case HASH_KEY:
                    case HASH_COLON:
                    case HASH_VALUE:
                    case HASH_COMMA:
                        setError("Hash/Object not terminated");
                        break;
                    default:
                        setError("not terminated");
                }
            }
        }
        // proceed with cleanup
        if (0 != line) {
            // FIXME:
            //rb_jump_tag(line);
        }

        if (err_has()) {
            // FIXME: Should be JSon::ParseError but we need mimic for this impld
            throw context.runtime.newArgumentError(error);
        }

        if (options.quirks_mode == No) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                // FIXME: Should be JSon::ParseError but we need mimic for this impld
                throw context.runtime.newArgumentError("unexpected non-document Object");
            }
        }
        return result;
    }

    IRubyObject calc_hash_key(Val kval) {
        IRubyObject rkey = kval.key_val;

        if (undefValue() != rkey) {
            return rkey;
        }

        RubyString newStr = getRuntime().newString(kval.key);
        newStr = oj_encode(newStr);
        if (Yes == options.sym_key) {
            return newStr.intern19(); // I this will still be ok for 1.8 mode.
        }

        return newStr;
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
