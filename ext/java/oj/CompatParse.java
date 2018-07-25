package oj;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.Options.Yes;

/**
 * Created by enebo on 8/28/15.
 */
public class CompatParse extends StrictParse {
    public CompatParse(ThreadContext context, Options options) {
        super(context, options);
    }

    @Override
    public void setCStr(Val kval, ByteList str, int orig) {
        ByteList key = kval.key;
        Val	parent = stack_peek();
        IRubyObject rkey = kval.key_val;

        if (undef == rkey && options.create_id != null && options.create_id.equals(key)) {
            parent.classname = str.dup();
        } else {
            IRubyObject rstr = getRuntime().newString(str);

            if (undef == rkey) {
                rkey = getRuntime().newString(key);
                rstr = oj_encode(rstr);
                rkey = oj_encode(rkey);
                if (Yes == options.sym_key) {
                    rkey = ((RubyString)rkey).intern19();
                }
            }
            ((RubyHash) parent.val).fastASet(rkey, rstr);
        }
    }

    @Override
    public IRubyObject endHash() {
        Val	parent = stack_peek();

        if (null != parent.classname) {
            IRubyObject clas = nameToClass(parent.classname, false);
            if (undef != clas) { // else an error
                parent.val = clas.callMethod(context, "json_create", parent.val);
            }
            if (null != parent.classname) {
                parent.classname = null;
            }
        }
        return context.nil;
    }

    @Override
    public void addNum(NumInfo ni) {
        value = ni.toNumber(context);
    }

    @Override
    public void setNum(Val kval, NumInfo ni) {
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), ni.toNumber(context));
    }

    @Override
    public void appendNum(NumInfo ni) {
        ((RubyArray) stack_peek().val).append(ni.toNumber(context));
    }
}
