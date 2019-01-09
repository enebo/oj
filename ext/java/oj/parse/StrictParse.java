package oj.parse;

import oj.Options;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/28/15.
 */
public class StrictParse extends Parse {
    public StrictParse(ParserSource source, ThreadContext context, Options options) {
        super(source, context, options, null);
    }

    @Override
    public void arrayAppendCStr(ByteList value, int orig) {
        ((RubyArray) stack_peek().val).append(oj_encode(getRuntime().newString(value)));
    }

    @Override
    public void arrayAppendNum(NumInfo ni) {
        if (ni.infinity || ni.nan) parseError("not a number or other value");

        ((RubyArray) stack_peek().val).append(ni.toNumber(context));
    }

    @Override
    public void addCStr(ByteList value, int orig) {
        this.value = oj_encode(getRuntime().newString(value));
    }

    @Override
    public void addNum(NumInfo ni) {
        if (ni.infinity || ni.nan) parseError("not a number or other value");

        value = ni.toNumber(context);
    }

    @Override
    public void hashSetCStr(Val kval, int start, int length) {
        hashSetCStr(kval, source.subStr(start, length), start);
    }

    @Override
    public void hashSetCStr(Val kval, ByteList value, int orig) {
        IRubyObject rstr = oj_encode(getRuntime().newString(value));

        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), rstr);
    }

    @Override
    public void hashSetNum(Val kval, NumInfo ni) {
        if (ni.infinity || ni.nan) parseError("not a number or other value");

        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), ni.toNumber(context));
    }

    @Override
    public void hashSetValue(Val kval, IRubyObject value) {
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), value);
    }
}
