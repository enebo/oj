package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public class NoopArrayAppendPICall implements ArrayAppendPICall {
    @Override
    public void appendCStr(ParseInfo pi, ByteList str) {
    }

    @Override
    public void appendNum(ParseInfo pi, NumInfo ni) {
    }

    @Override
    public void appendValue(ParseInfo pi, IRubyObject value) {
    }
}
