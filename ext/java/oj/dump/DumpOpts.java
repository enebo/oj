package oj.dump;

import oj.options.DumpCaller;
import oj.options.NanDump;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import static oj.dump.Dump.MAX_DEPTH;

/**
 * Created by enebo on 8/24/15.
 */
public class DumpOpts implements Cloneable {
    public boolean use;
    public ByteList indent_str = ByteList.EMPTY_BYTELIST;
    public ByteList before_sep = ByteList.EMPTY_BYTELIST;
    public ByteList after_sep = ByteList.EMPTY_BYTELIST;
    public ByteList hash_nl = ByteList.EMPTY_BYTELIST;
    public ByteList array_nl = ByteList.EMPTY_BYTELIST;
    public NanDump nan_dump = NanDump.AutoNan;
    public boolean omit_nil;
    public int max_depth = MAX_DEPTH;

    public DumpOpts() {
    }

    public DumpOpts(DumpOpts old) {
        use = old.use;
        indent_str = old.indent_str;
        before_sep = old.before_sep;
        after_sep = old.after_sep;
        hash_nl = old.hash_nl;
        array_nl = old.array_nl;
        nan_dump = old.nan_dump;
        omit_nil = old.omit_nil;
        max_depth = old.max_depth;
    }

    public void maybeSetAfterSep(ThreadContext context, IRubyObject v, boolean acceptNil) {
        if (v == null) return;
        if (acceptNil && v.isNil()) {
            after_sep = ByteList.EMPTY_BYTELIST;
        } else if (!v.isNil()) {
            after_sep = ((RubyString) TypeConverter.checkStringType(context.runtime, v)).getByteList();
        }
    }

    public void maybeSetBeforeSep(ThreadContext context, IRubyObject v, boolean acceptNil) {
        if (v == null) return;
        if (acceptNil && v.isNil()) {
            before_sep = ByteList.EMPTY_BYTELIST;
        } else if (!v.isNil()) {
            before_sep = ((RubyString) TypeConverter.checkStringType(context.runtime, v)).getByteList();
        }
    }

    public void maybeSetArrayNewline(ThreadContext context, IRubyObject v, boolean acceptNil) {
        if (v == null) return;
        if (acceptNil && v.isNil()) {
            array_nl = ByteList.EMPTY_BYTELIST;
        } else if (!v.isNil()) {
            array_nl = ((RubyString) TypeConverter.checkStringType(context.runtime, v)).getByteList();
        }
    }

    public void maybeSetHashNewline(ThreadContext context, IRubyObject v, boolean acceptNil) {
        if (v == null) return;
        if (acceptNil && v.isNil()) {
            hash_nl = ByteList.EMPTY_BYTELIST;
        } else if (!v.isNil()) {
            hash_nl = ((RubyString) TypeConverter.checkStringType(context.runtime, v)).getByteList();
        }
    }
}
