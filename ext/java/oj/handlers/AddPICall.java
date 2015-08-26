package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public interface AddPICall {
    void addValue(ParseInfo pi, IRubyObject val);
    void addCStr(ParseInfo pi, ByteList str);
    void addNum(ParseInfo pi, NumInfo ni);
}
