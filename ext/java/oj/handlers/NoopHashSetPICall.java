package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import oj.Val;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public class NoopHashSetPICall implements HashSetPICall {
    @Override
    public void setCStr(ParseInfo pi, Val kval, ByteList str) {
    }

    @Override
    public void setNum(ParseInfo pi, Val kval, NumInfo ni) {
    }

    @Override
    public void setValue(ParseInfo pi, Val kval, IRubyObject value) {
    }
}
