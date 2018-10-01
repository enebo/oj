package oj.parse;

import oj.OjLibrary;
import oj.Options;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class CompatParse extends StrictParse {
    public CompatParse(ParserSource source, ThreadContext context, Options options) {
        super(source, context, options);
    }

    @Override
    public IRubyObject parse(OjLibrary oj, boolean yieldOk, Block block) {
        options.nilnil = true;
        options.empty_string = false;

        return super.parse(oj, yieldOk, block);
    }

    @Override
    public void hashSetCStr(Val kval, ByteList str, int orig) {
        ByteList key = kval.key;
        Val	parent = stack_peek();
        IRubyObject rkey = kval.key_val;

        if (null == rkey && options.create_id != null && options.create_id.equals(key)) {
            parent.classname = str.dup();
        } else {
            IRubyObject rstr = getRuntime().newString(str);

            if (rkey == null) {
                rkey = oj_encode(getRuntime().newString(key));

                if (options.sym_key) rkey = context.runtime.newSymbol(((RubyString)rkey).getByteList());
            }
            ((RubyHash) parent.val).fastASet(rkey, rstr);
        }
    }

    @Override
    public IRubyObject endHash() {
        Val	parent = stack_peek();

        if (parent.classname != null) {
            IRubyObject clas = nameToClass(parent.classname, false, context.runtime.getArgumentError());

            if (clas != null) parent.val = clas.callMethod(context, "json_create", parent.val);

            parent.classname = null;
        }

        return context.nil;
    }

    @Override
    public void addNum(NumInfo ni) {
        value = ni.toNumber(context);
    }

    @Override
    public void hashSetNum(Val kval, NumInfo ni) {
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), ni.toNumber(context));
    }

    @Override
    public void arrayAppendNum(NumInfo ni) {
        ((RubyArray) stack_peek().val).append(ni.toNumber(context));
    }
}
