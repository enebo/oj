package oj;

import java.util.Stack;
import org.jruby.Ruby;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Created by enebo on 8/24/15.
 */
public class ParseInfo {
    private Ruby runtime;
    public Stack<Val> stack;
    public Object proc;
    private Object undef = new Object();
    private String error = null;
    public Options options;
    public IRubyObject handler;

    public ParseInfo(Ruby runtime) {
        this.runtime = runtime;
        this.stack = new Stack<Val>();
        this.proc = null;
    }

    public Val start_array(NextItem nextItem) {
        return null;
    }

    public void end_array() {

    }

    public Val start_hash(NextItem nextItem) {
        return null;
    }

    public void appendTo(ByteList buf) {
        // append all bytes up to current....
    }

    public void end_hash() {

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

    public void array_append_value(Object value) {
    }

    public void hash_set_value(Val parent, Object rval) {
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
        return runtime.getNil();
    }

    public IRubyObject trueValue() {
        return runtime.getTrue();
    }

    public IRubyObject falseValue() {
        return runtime.getFalse();
    }

    public Object undefValue() {
        return undef;
    }

    public IRubyObject newFloat(double value) {
        return runtime.newFloat(value);
    }

    // FIXME: This could potentially use an access point which directly consumed a bytelist (although JRuby needs to add one).
    public IRubyObject newBigDecimal(IRubyObject string) {
        return RubyBigDecimal.newBigDecimal(runtime.getBasicObject(), new IRubyObject[] {string}, Block.NULL_BLOCK);
    }

    public IRubyObject newString(Object stringValue) {
        return null;
    }

    public Ruby getRuntime() {
        return runtime;
    }
}
