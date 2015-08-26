package oj;

import java.util.Stack;
import oj.handlers.ArrayAppendPICall;
import oj.handlers.HashSetPICall;
import oj.handlers.PICall;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class ParseInfo {
    private ThreadContext context;
    public ByteList json;
    public ByteList cur;
    public Stack<Val> stack;
    public Object proc;
    public IRubyObject undef;
    private String error = null;
    public Options options;
    public IRubyObject handler;

    public PICall start_hash;
    public PICall end_hash;
    public PICall start_array;
    public PICall hash_key;
    public PICall end_array;
    public HashSetPICall hash_set;
    public ArrayAppendPICall array_append;
    public boolean expect_value;
    public String add_cstr;
    public String add_num;
    public String add_value;

    public ParseInfo(ThreadContext context) {
        this.context = context;
        this.stack = new Stack<Val>();
        this.proc = null;
        this.undef = new RubyBasicObject(null);
    }

    public void appendTo(ByteList buf) {
        // append all bytes up to current....
    }

    public char advance(int amount) {
    }

    public char current() {
    }

    public char current(int amount) {
    }

    public int offset() {
    }

    public int length() {
    }

    public Val stack_peek() {
    }

    public void add_value(Object value) {
    }

    public void err_init() {
        error = null;
    }

    public boolean err_has() {
        return error != null;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Live Ruby value objects.  Note: we keep these in ParseInfo since n Ruby runtimes can load OJ
    // in a single JVM but these values are not shared between those runtimes.

    public IRubyObject nilValue() {
        return context.runtime.getNil();
    }

    public IRubyObject trueValue() {
        return context.runtime.getTrue();
    }

    public IRubyObject falseValue() {
        return context.runtime.getFalse();
    }

    public IRubyObject undefValue() {
        return undef;
    }

    public IRubyObject newFloat(double value) {
        return context.runtime.newFloat(value);
    }

    // FIXME: This could potentially use an access point which directly consumed a bytelist (although JRuby needs to add one).
    public IRubyObject newBigDecimal(IRubyObject string) {
        return RubyBigDecimal.newBigDecimal(context.runtime.getBasicObject(), new IRubyObject[]{string}, Block.NULL_BLOCK);
    }

    public IRubyObject newString(Object stringValue) {
        return null;
    }

    public ThreadContext getContext() {
        return context;
    }

    public Ruby getRuntime() {
        return context.runtime;
    }
}
