package oj;

import org.jruby.runtime.builtin.IRubyObject;

import static oj.Attr.AttrType.VALUE;

/**
 * Created by enebo on 8/31/18.
 */
public class Attr {
    public enum AttrType { VALUE, TIME, NUMBER };
    public String name;
    public IRubyObject value;
    public AttrType type = VALUE;

    public Attr(String name, IRubyObject value) {
        this.name = name;
        this.value = value;
    }
}
