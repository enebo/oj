package oj.handlers;

import oj.NumInfo;
import oj.ParseInfo;
import oj.Val;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/26/15.
 */
public interface HashSetPICall {
    void setCStr(ParseInfo pi, Val kval, ByteList str);
    void setNum(ParseInfo pi, Val kval, NumInfo ni);
    void setValue(ParseInfo pi, Val kval, IRubyObject value);
}
