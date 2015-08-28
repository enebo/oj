package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public class NoopAddPICall implements AddPICall {
    @Override
    public void addValue(ParseInfo pi, IRubyObject val) {
    }

    @Override
    public void addCStr(ParseInfo pi, ByteList str) {
    }

    @Override
    public void addNum(ParseInfo pi, NumInfo ni) {
    }
}
