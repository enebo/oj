package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public interface ArrayAppendPICall {
    void appendCStr(ParseInfo pi, ByteList str);
    void appendNum(ParseInfo pi, NumInfo ni);
    void appendValue(ParseInfo pi, IRubyObject value);
}
