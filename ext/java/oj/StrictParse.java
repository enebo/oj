package oj;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/28/15.
 */
public class StrictParse extends Parse {
    public StrictParse(ThreadContext context, Options options) {
        super(context, options, null);
    }

    @Override
    public void appendCStr(ByteList value) {
        ((RubyArray) stack_peek().val).append(oj_encode(getRuntime().newString(value)));
    }

    @Override
    public void appendNum(NumInfo ni) {
        ((RubyArray) stack_peek().val).append(ni.toNumber(context));
    }

    @Override
    public void appendValue(IRubyObject value) {
        ((RubyArray) stack_peek().val).append(value);
    }

    @Override
    public void addCStr(ByteList value) {
        stack_peek().val = oj_encode(getRuntime().newString(value));
    }

    @Override
    public void addNum(NumInfo ni) {
        if (ni.infinity || ni.nan) {
            oj_set_error_at("not a number or other value");
        }

        stack_peek().val = ni.toNumber(context);
    }

    @Override
    public void addValue(IRubyObject value) {
        stack_peek().val = value;
    }

    @Override
    public void setCStr(Val kval, int start, int length) {
        setCStr(kval, subStr(start, length));
    }

    @Override
    public void setCStr(Val kval, ByteList value) {
        RubyString rstr = getRuntime().newString(value);

        rstr = oj_encode(rstr);
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), rstr);
    }

    @Override
    public void setNum(Val kval, NumInfo ni) {
        if (ni.infinity || ni.nan) {
            oj_set_error_at("not a number or other value");
        }
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), ni.toNumber(context));
    }

    @Override
    public void setValue(Val kval, IRubyObject value) {
        ((RubyHash) stack_peek().val).fastASet(calc_hash_key(kval), value);
    }

    @Override
    public IRubyObject hashKey(int start, int length) {
        return context.nil;
    }

    @Override
    public IRubyObject hashKey(ByteList key) {
        return context.nil;
    }

    @Override
    public IRubyObject endArray() {
        return context.nil;
    }

    @Override
    public IRubyObject startArray() {
        return getRuntime().newArray();
    }

    @Override
    public IRubyObject endHash() {
        return context.nil;
    }

    @Override
    public IRubyObject startHash() {
        return RubyHash.newHash(context.runtime);
    }
}
