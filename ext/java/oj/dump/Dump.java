package oj.dump;

import jnr.posix.util.Platform;
import oj.Odd;
import oj.OjLibrary;
import oj.Options;
import oj.parse.Parse;
import oj.ROptTable;
import oj.options.DumpCaller;
import oj.options.NanDump;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyComplex;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyNumeric;
import org.jruby.RubyRange;
import org.jruby.RubyRational;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.ext.stringio.StringIO;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;
import org.jruby.util.TypeConverter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static oj.parse.NumInfo.OJ_INFINITY;
import static oj.Options.*;
import static oj.Options.Yes;

/**
 * Base class for all Dumpers.
 */
public abstract class Dump {
    protected ThreadContext context;
    protected Ruby runtime;
    public ByteList buf;
    public Map<Object,Integer> circ_cache = null;
    public int circ_cnt = 0;
    public int indent;
    public int depth = 0; // used by dump_hash
    public Options opts;
    public int hash_cnt = 0;
    public boolean allocated;
    public boolean omit_nil;
    IRubyObject[] argv;
    DumpCaller caller = DumpCaller.CALLER_DUMP; // use for mimic json only
    ROptTable ropts;
    public OjLibrary oj;
    byte[] num_buf = new byte[32];

    public static Dump createDump(ThreadContext context, OjLibrary oj, Options opts) {
        switch (opts.mode) {
            case NullMode: return new NullDump(context, oj, opts);
            case StrictMode: return new StrictDump(context, oj, opts);
            case CompatMode: return new CompatDump(context, oj, opts);
            case CustomMode: return new CustomDump(context, oj, opts);
            case ObjectMode:
            default: //FIXME consider not defaulting or understand what default is.
                return new ObjectDump(context, oj, opts);
        }
    }

    public Dump(ThreadContext context, OjLibrary oj, Options opts) {
        this.context = context;
        this.runtime = context.runtime;
        reset();
        this.opts = opts;
        indent = opts.indent;
        omit_nil = opts.dump_opts.omit_nil;
        this.oj = oj;
    }

    // Entry Point
    public ByteList obj_to_json(IRubyObject obj) {
        return obj_to_json_using_params(obj, IRubyObject.NULL_ARRAY);
    }

    // Entry Point
    public void write_obj_to_file(IRubyObject obj, String path) {
        FileOutputStream f = null;
        ByteList json = obj_to_json(obj);

        try {
            f = new FileOutputStream(path);

            f.write(json.unsafeBytes(), json.begin(), json.realSize());
        } catch (FileNotFoundException e) {
            throw context.runtime.newIOErrorFromException(e);
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        } finally {
            if (f != null) {
                try { f.close(); } catch (IOException e) {}
            }
        }
    }

    // Entry Point
    public void write_obj_to_stream(IRubyObject obj, IRubyObject stream) {
        obj_to_json(obj);

        // Note: Removed Windows path as it called native write on fileno and JRuby does work the same way.
        if (obj instanceof StringIO) {
            ((StringIO) obj).write(context, context.runtime.newString(buf));
        } else if (stream.respondsTo("write")) {
            stream.callMethod(context, "write", context.runtime.newString(buf));
        } else {
            throw context.runtime.newArgumentError("to_stream() expected an IO Object.");
        }
    }

    public static int MAX_DEPTH = 1000;

    protected static final byte[] BIG_O_KEY = {'"', '^', 'O', '"', ':'};
    protected static final byte[] C_KEY = {'"', '^', 'c', '"', ':'};
    protected static final byte[] O_KEY = {'"', '^', 'o', '"', ':'};
    protected static final byte[] I_KEY = {'"', '^', 'i', '"', ':'};
    protected static final byte[] PARTIAL_I_KEY = {'"', '^', 'i'};
    protected static final byte[] PARTIAL_R_KEY = {'"', '^', 'r'};
    protected static final byte[] T_KEY = {'"', '^', 't', '"', ':'};
    protected static final byte[] U_KEY = {'"', '^', 'u', '"', ':', '['};
    protected static final byte[] SELF_KEY = {'"', 's', 'e', 'l', 'f', '"', ':'};
    protected static final byte[] NULL_VALUE = {'n', 'u', 'l', 'l'};
    protected static final byte[] TRUE_VALUE = {'t', 'r', 'u', 'e'};
    protected static final byte[] FALSE_VALUE = {'f', 'a', 'l', 's', 'e'};
    public static final byte[] INFINITY_VALUE = {'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    protected static final byte[] NINFINITY_VALUE = {'-', 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    protected static final byte[] NAN_VALUE = {'N', 'a', 'N'};
    public static final byte[] INF_VALUE = {'3', '.', '0', 'e', '1', '4', '1', '5', '9', '2', '6', '5', '3', '5', '8', '9', '7', '9', '3', '2', '3', '8', '4', '6'};
    public static final byte[] NINF_VALUE = {'-', '3', '.', '0', 'e', '1', '4', '1', '5', '9', '2', '6', '5', '3', '5', '8', '9', '7', '9', '3', '2', '3', '8', '4', '6'};
    public static final byte[] NAN_NUMERIC_VALUE = {'3', '.', '3', 'e', '1', '4', '1', '5', '9', '2', '6', '5', '3', '5', '8', '9', '7', '9', '3', '2', '3', '8', '4', '6'};
    protected static final byte[] ZERO_POINT_ZERO = {'0', '.', '0'};
    protected static final ByteList INFINITY = new ByteList(INFINITY_VALUE);
    protected static final ByteList NINFINITY = new ByteList(NINFINITY_VALUE);
    protected static final byte[] EMPTY_HASH = {'{', '}'};

    static String	hex_chars = "0123456789abcdef";

    // JSON standard except newlines are no escaped
    static int newline_friendly_chars[] = {
            6,6,6,6,6,6,6,6,2,2,1,6,2,2,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    // JSON standard
    static int hibit_friendly_chars[] = {
            6,6,6,6,6,6,6,6,2,2,2,6,2,2,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};

    // High bit set characters are always encoded as unicode. Worse case is 3
    // bytes per character in the output. That makes this conservative.
    static int	ascii_friendly_chars[] = {
            6,6,6,6,6,6,6,6,2,2,2,6,2,2,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3};

    // XSS safe mode
    static int xss_friendly_chars[] = {
            6,6,6,6,6,6,6,6,2,2,2,6,2,2,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            1,1,2,1,1,1,6,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,6,1,6,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,
            1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,6,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3};


    static int newline_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += newline_friendly_chars[str.get(i) & 0xff];
        }
        return size - len * (int)'0';
    }

    static int hibit_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += hibit_friendly_chars[str.get(i) & 0xff];
        }
        return size - len * (int)'0';
    }

    static int ascii_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += ascii_friendly_chars[str.get(i) & 0xff];
        }
        return size - len * (int)'0';
    }

    static int xss_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += xss_friendly_chars[str.get(i) & 0xff];
        }
        return size - len * (int)'0';
    }

    public void fill_indent(int cnt) {
        if (indent > 0) {
            cnt *= indent;
            append('\n');
            for (; 0 < cnt; cnt--) {
                append(' ');
            }
        }
    }

    void dump_long(long num) {
        int	b = num_buf.length - 1;
        boolean negative = false;

        if (num == 0) {
            append('0');
            return;
        } else if (num < 0) {
            negative = true;
            num = -num;
        }

        do {
            num_buf[--b] = (byte) ((num % 10) + '0');
        } while ((num /= 10) > 0);
        if (negative) num_buf[--b] = '-';

        append(num_buf, b, num_buf.length - b);
    }

    void dump_ulong(long num) {
        int	b = num_buf.length - 1;

        if (num == 0) {
            append('0');
            return;
        }

        do {
            num_buf[--b] = (byte) ((num % 10) + '0');
        } while ((num /= 10) > 0);

        append(num_buf, b, num_buf.length - b - 1);
    }

    void dump_hex(int c) {
        int	d = (c >> 4) & 0x0F;

        append(hex_chars.charAt(d));
        d = c & 0x0F;
        append(hex_chars.charAt(d));
    }

    public void dump_raw(byte[] str) {
        append(str);
    }

    public void dump_raw(ByteList str) {
        append(str);
    }

    int dump_unicode(ByteList str, int str_i, int end) {
        int	code;
        int	b = str.get(str_i);
        int		i, cnt;

        if (0xC0 == (0xE0 & b)) {
            cnt = 1;
            code = b & 0x0000001F;
        } else if (0xE0 == (0xF0 & b)) {
            cnt = 2;
            code = b & 0x0000000F;
        } else if (0xF0 == (0xF8 & b)) {
            cnt = 3;
            code = b & 0x00000007;
        } else if (0xF8 == (0xFC & b)) {
            cnt = 4;
            code = b & 0x00000003;
        } else if (0xFC == (0xFE & b)) {
            cnt = 5;
            code = b & 0x00000001;
        } else {
            throw context.runtime.newEncodingError("Invalid Unicode\n");
        }
        str_i++;
        for (; 0 < cnt; cnt--, str_i++) {
            b = str.get(str_i);
            if (end <= str_i || 0x80 != (0xC0 & b)) {
                throw context.runtime.newEncodingError("Invalid Unicode\n");
            }
            code = (code << 6) | (b & 0x0000003F);
        }
        if (0x0000FFFF < code) {
            int	c1;

            code -= 0x00010000;
            c1 = ((code >> 10) & 0x000003FF) + 0x0000D800;
            code = (code & 0x000003FF) + 0x0000DC00;
            append('\\');
            append('u');
            for (i = 3; 0 <= i; i--) {
                append(hex_chars.charAt((int)(c1 >> (i * 4)) & 0x0F));
            }
        }
        append('\\');
        append('u');
        for (i = 3; 0 <= i; i--) {
            append(hex_chars.charAt((int)(code >> (i * 4)) & 0x0F));
        }
        return str_i - 1;
    }

    // returns 0 if not using circular references, -1 if not further writing is
    // needed (duplicate), and a positive value if the object was added to the cache.
    protected long check_circular(IRubyObject obj) {
        Integer	id = 0;

        if (opts.circular) {
            id = circ_cache.get(obj);
            if (id == null) {
                circ_cnt++;
                id = circ_cnt;
                circ_cache.put(obj, id);
            } else {
                if (opts.mode == ObjectMode) {
                    append(PARTIAL_R_KEY);
                    dump_ulong(id.longValue());
                    append('"');
                }
                return -1;
            }
        }
        return id.longValue();
    }

    protected void dump_nil() {
        append(NULL_VALUE);
    }

    protected void dump_true() {
        append(TRUE_VALUE);
    }

    protected void dump_false() {
        append(FALSE_VALUE);
    }

    protected void dump_fixnum(RubyFixnum obj) {
        byte buf[] = new byte[32];
        int	b = buf.length - 1;
        long num = obj.getLongValue();
        boolean neg = false;

        if (num < 0) {
            num = -num;
            neg = true;
        }

        if (num == 0) {
            buf[b] = '0';
        } else {
            for (; 0 < num; num /= 10, b--) {
                buf[b] = (byte) ((num % 10) + '0');
            }
            if (neg) {
                buf[b] = '-';
            } else {
                b++;
            }
        }

        int size = buf.length - b;
        append(buf, b, size);
    }

    protected void dump_bignum(RubyBignum obj) {
        // Note: This uses boxed call to to_s because 9.1 -> 9.2 changed return type on non-boxed version
        // from IRubyObject -> RubyString.
        append(obj.to_s(new IRubyObject[] { context.runtime.newFixnum(10) }).convertToString().getByteList());
    }

    // fIXME: this may be for object only
    protected void dump_float(RubyFloat obj) {
        double d = obj.getDoubleValue();

        if (d == 0.0) {
            append(ZERO_POINT_ZERO);
        } else if (d == OJ_INFINITY) {
            dumpInfNanForFloat(obj, INF_VALUE, INFINITY_VALUE);
        } else if (d == -OJ_INFINITY) {
            dumpInfNanForFloat(obj, NINF_VALUE, NINFINITY_VALUE);
        } else if (Double.isNaN(d)) {
            dumpNanNanForFloat(obj);
        } else if (0 == opts.float_prec) {
            IRubyObject	rstr = Helpers.invoke(context, obj, "to_s");
            RubyString str = (RubyString) TypeConverter.checkStringType(context.runtime, rstr);
            append(str.getByteList().bytes());
        } else {
            ByteList buf = new ByteList();
            Sprintf.sprintf(buf, opts.float_fmt, obj);
            append(buf);
        }
    }

    private void dumpNanNanForFloat(IRubyObject value) {
        if (opts.mode == ObjectMode) {
            append(NAN_NUMERIC_VALUE);
        } else {
            NanDump nd = opts.dump_opts.nan_dump;

            if (nd == NanDump.AutoNan) {
                switch (opts.mode) {
                    case CompatMode: nd = NanDump.WordNan; break;
                    case StrictMode: nd = NanDump.RaiseNan; break;
                    case NullMode: nd = NanDump.NullNan; break;
                }
            }

            switch(nd) {
                case RaiseNan: raise_strict(value); break;
                case WordNan: append(NAN_VALUE); break;
                case NullNan: append(NULL_VALUE); break;
                case HugeNan:
                default: append(NAN_NUMERIC_VALUE); break;
            }
        }
    }
    private void dumpInfNanForFloat(IRubyObject value, byte[] inf_value, byte[] infinity_value) {
        if (opts.mode == ObjectMode) {
            append(inf_value);
        } else {
            NanDump nd = opts.dump_opts.nan_dump;

            if (nd == NanDump.AutoNan) {
                switch (opts.mode) {
                    case CompatMode: nd = NanDump.WordNan; break;
                    case StrictMode: nd = NanDump.RaiseNan; break;
                    case NullMode: nd = NanDump.NullNan; break;
                    case CustomMode:  nd = NanDump.NullNan; break;
                }
            }

            switch(nd) {
                case RaiseNan: raise_strict(value); break;
                case WordNan: append(infinity_value); break;
                case NullNan: append(NULL_VALUE); break;
                case HugeNan:
                default: append(inf_value); break;
            }
        }
    }

    // FIXME: This is going to mess up blindly grabbing bytes if it mismatches UTF-8 (I believe all strings
    // will be UTF-8 or clean ascii 7-bit).
    protected void dump_cstr(String str, boolean is_sym, boolean escape1) {
        dump_cstr(new ByteList(str.getBytes(), UTF8Encoding.INSTANCE), is_sym, escape1);
    }

    public void dump_cstr(ByteList str, boolean is_sym, boolean escape1) {
        int	size;
        int[] cmap;
        int str_i = 0;

        switch (opts.escape_mode) {
            case NLEsc:
                cmap = newline_friendly_chars;
                size = newline_friendly_size(str);
                break;
            case ASCIIEsc:
                cmap = ascii_friendly_chars;
                size = ascii_friendly_size(str);
                break;
            case XSSEsc:
                cmap = xss_friendly_chars;
                size = xss_friendly_size(str);
                break;
            case JSONEsc:
            default:
                cmap = hibit_friendly_chars;
                size = hibit_friendly_size(str);
        }
        int cnt = str.length();

        append('"');
        if (escape1) {
            append('\\');
            append('u');
            append('0');
            append('0');
            dump_hex(str.get(str_i));
            cnt--;
            size--;
            str_i++;
            is_sym = false; // just to make sure
        }
        if (cnt == size) {
            if (is_sym) append(':');
            append(str.unsafeBytes(), str.begin() + str_i, cnt);
            append('"');
        } else {
            if (is_sym) {
                append(':');
            }
            for (; str_i < cnt; str_i++) {
                switch (cmap[(int)str.get(str_i) & 0xff]) {
                    case 1:
                        append(str.get(str_i));
                        break;
                    case 2:
                        append('\\');
                        switch ((byte) str.get(str_i)) {
                            case '\\':	append('\\');	break;
                            case '\b':	append('b');	break;
                            case '\t':	append('t');	break;
                            case '\n':	append('n');	break;
                            case '\f':	append('f');	break;
                            case '\r':	append('r');	break;
                            default:	append(str.get(str_i));	break;
                        }
                        break;
                    case 3: // Unicode
                        str_i = dump_unicode(str, str_i, cnt);
                        break;
                    case 6: // control characters
                        append('\\');
                        append('u');
                        append('0');
                        append('0');
                        dump_hex(str.get(str_i));
                        break;
                    default:
                        break; // ignore, should never happen if the table is correct
                }
            }
            append('"');
        }
    }

    protected abstract void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth);
    protected abstract void dump_class(RubyModule clas);
    protected abstract void dump_complex(IRubyObject obj, int depth);
    protected abstract void dump_other(IRubyObject obj, int depth);
    protected abstract void dump_range(RubyRange obj, int depth);
    protected abstract void dump_rational(RubyRational rational, int depth);
    protected abstract void dump_regexp(IRubyObject obj, int depth);
    protected abstract void dump_str(RubyString string);
    protected abstract void dump_struct(RubyStruct obj, int depth);
    protected abstract void dump_sym(RubySymbol symbol);
    protected abstract void dump_time(RubyTime time, int depth);
    protected abstract String modeName();

    protected void dump_array(RubyArray array, int depth) {
        int d2 = depth + 1;
        long id = check_circular(array);

        if (id < 0) return; // duplicate found (written out in check_circular)

        append('[');
        if (id > 0) {
            fill_indent(d2);
            append(PARTIAL_I_KEY);
            dump_ulong(id);
            append('"');
        }

        if (array.isEmpty()) {
            append(']');
        } else {
            if (id > 0) append(',');

            int cnt = array.getLength() - 1;

            for (int i = 0; i <= cnt; i++) {
                indent(d2, opts.dump_opts.array_nl);
                dump_val(array.eltInternal(i), d2);
                if (i < cnt) append(',');
            }
            indent(depth, opts.dump_opts.array_nl);
            append(']');
        }
    }

    protected void indent(int depth, ByteList nl) {
        if (opts.dump_opts.use) {
            if (nl != ByteList.EMPTY_BYTELIST) append(nl);

            if (opts.dump_opts.indent_str != ByteList.EMPTY_BYTELIST) {
                for (int j = depth; 0 < j; j--) {
                    append(opts.dump_opts.indent_str);
                }
            }
        } else {
            fill_indent(depth);
        }
    }

    /**
     * C: hash_cb or similar *_cb
     *
     * Visits each member of the hash which is being dumped.
     */
    protected void visit_hash(IRubyObject key, IRubyObject value) {
        int saved_depth = depth;
        if (omit_nil && value.isNil()) return;

        if (!(key instanceof RubyString)) {
            throw context.runtime.newTypeError("In :" + modeName() + " mode all Hash keys must be Strings, not " + key.getMetaClass().getName());
        }
        if (opts.dump_opts.use) {
            dump_hash_nl_indent(saved_depth);
            dump_str((RubyString) key);
            dump_colon();
        } else {
            fill_indent(saved_depth);
            dump_str((RubyString) key);
            append(':');
        }
        dump_val(value, saved_depth);
        append(',');

        depth = saved_depth;
    }

    protected void dump_hash_nl_indent(int depth) {
        if (opts.dump_opts.hash_nl != ByteList.EMPTY_BYTELIST) {
            append(opts.dump_opts.hash_nl);
        }
        if (opts.dump_opts.indent_str != ByteList.EMPTY_BYTELIST) {
            for (int i = depth; 0 < i; i--) {
                append(opts.dump_opts.indent_str);
            }
        }
    }

    protected void dump_colon() {
        if (opts.dump_opts.before_sep != ByteList.EMPTY_BYTELIST) {
            append(opts.dump_opts.before_sep);
        }
        append(':');
        if (opts.dump_opts.after_sep != ByteList.EMPTY_BYTELIST) {
            append(opts.dump_opts.after_sep);
        }
    }

    protected void dump_hash(IRubyObject obj, int dep) {
        RubyHash hash = (RubyHash) obj;

        if (hash.isEmpty()) {
            append(EMPTY_HASH);
        } else {
            long id = check_circular(hash);
            if (id < 0) return;

            append('{');
            if (id > 0) {
                fill_indent(dep + 1);
                append(I_KEY);
                dump_ulong(id);
                append(',');
            }
            depth = dep + 1;
            hash.visitAll(context,
                    new RubyHash.VisitorWithState<Dump>() {
                        @Override
                        public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Dump dump) {
                            visit_hash(key, value);
                        }
                    }, this);
            if (',' == get(-1)) pop(); // backup to overwrite last comma
            indent(dep, opts.dump_opts.hash_nl);
            append('}');
        }
    }

    // In JRuby rb_time_timespec equivalent is not public.
    // FIXME: Move into EmbeddedAPI when I make it
    private static long[] extractTimespec(ThreadContext context, IRubyObject value) {
        long[] timespec = new long[2];

        if (value instanceof RubyFloat) {
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            double fraction = ((RubyFloat) value).getDoubleValue() % 1.0;
            timespec[1] = (long)(fraction * 1e9 + 0.5);
        } else if (value instanceof RubyNumeric) {
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            timespec[1] = 0;
        } else {
            org.jruby.RubyTime time;
            if (value instanceof RubyTime) {
                time = ((RubyTime) value);
            } else {
                time = (RubyTime) TypeConverter.convertToType(value, context.runtime.getTime(), "to_time", true);

            }
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.to_i()) : RubyNumeric.num2long(time.to_i());
            timespec[1] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.nsec()) : RubyNumeric.num2long(time.nsec());
        }

        return timespec;
    }

    protected void _dump_time(IRubyObject obj, boolean withZone) {
        long[] timespec = extractTimespec(context, obj);
        long sec = timespec[0];
        long nsec = timespec[1];
        byte[] buf = new byte[64];
        int b = buf.length - 1;
        boolean neg = false;
        int dot;
        long one = 1000000000;

        // JRuby returns negative nsec....not sure if this is correct or whether sec should subtract 1 too?
        if (nsec < 0) {
            nsec = one + nsec;
            sec--;
        }

        if (withZone) {
            long	tzsecs = obj.callMethod(context, "utc_offset").convertToInteger().getLongValue();
            boolean	zneg = (0 > tzsecs);

            if (0 == tzsecs && obj.callMethod(context, "utc?").isTrue()) {
                tzsecs = 86400;
            }
            if (zneg) {
                tzsecs = -tzsecs;
            }
            if (0 == tzsecs) {
                buf[b] = '0';
                b--;
            } else {
                for (; 0 < tzsecs; b--, tzsecs /= 10) {
                    buf[b] = (byte) ('0' + (tzsecs % 10));
                }
                if (zneg) {
                    buf[b] = '-';
                    b--;
                }
            }
            buf[b] = 'e';
            b--;
        }
        if (0 > sec) {
            neg = true;
            sec = -sec;
            if (0 < nsec) {
                nsec = 1000000000 - nsec;
                sec--;
            }
        }
        dot = b - 9;
        if (0 < opts.sec_prec) {
            if (9 > opts.sec_prec) {
                for (int i = 9 - opts.sec_prec; 0 < i; i--) {
                    dot++;
                    nsec = (nsec + 5) / 10;
                    one /= 10;
                }
            }
            if (one <= nsec) {
                nsec -= one;
                sec++;
            }
            for (; dot < b; b--, nsec /= 10) {
                buf[b] = (byte) ('0' + (nsec % 10));
            }
            buf[b] = '.';
            b--;
        }
        if (0 == sec) {
            buf[b] = '0';
            b--;
        } else {
            for (; 0 < sec; b--, sec /= 10) {
                buf[b] = (byte) ('0' + (sec % 10));
            }
        }
        if (neg) {
            buf[b] = '-';
            b--;
        }
        b++;

        int size = buf.length - b;
        append(buf, b, size);
    }

    protected void dump_obj_comp(IRubyObject obj, int depth) {
        if (obj.respondsTo("to_hash")) {
            dump_to_hash(obj, depth);
        } else if (obj.respondsTo("as_json")) {
            dump_as_json(obj, depth);
        } else if (opts.to_json && obj.respondsTo("to_json")) {
            append(stringToByteList(obj, "to_json"));
        } else {
            if (obj instanceof RubyBigDecimal) {
                ByteList rstr = stringToByteList(obj, "to_s");
                if (Yes == opts.bigdec_as_num) {
                    dump_raw(rstr);
                } else {
                    dump_cstr(rstr, false, false);
                }
                //FIXME: what if datetime does not exist?
            } else  {
                RubyClass dateTime = context.runtime.getClass("DateTime");
                RubyClass date = context.runtime.getClass("Date");
                if (dateTime != null && dateTime.isInstance(obj) || date != null && date.isInstance(obj) || obj instanceof RubyRational) {
                    dump_cstr(stringToByteList(obj, "to_s"), false, false);
                } else {
                    dump_obj_attrs(obj, null, 0, depth);
                }
            }
        }
    }

    protected void dump_ruby_time(IRubyObject obj) {
        dump_cstr(stringToByteList(obj, "to_s"), false, false);
    }

    protected void dump_xml_time(IRubyObject obj) {
        long[] timespec = extractTimespec(context, obj);
        long sec = timespec[0];
        long nsec = timespec[1];
        StringBuilder buf = new StringBuilder();
        Formatter formatter = new Formatter(buf);
        long one = 1000000000;
        long tzsecs = obj.callMethod(context, "utc_offset").convertToInteger().getLongValue();
        int tzhour, tzmin;
        char tzsign = '+';

        if (9 > opts.sec_prec) {
            int	i;

            for (i = 9 - opts.sec_prec; 0 < i; i--) {
                nsec = (nsec + 5) / 10;
                one /= 10;
            }
            if (one <= nsec) {
                nsec -= one;
                sec++;
            }
        }
        // 2012-01-05T23:58:07.123456000+09:00
        //tm = localtime(&sec);
        sec += tzsecs;
        Date date = new Date(sec*1000); // milliseconds since epoch
        Calendar tm = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        tm.setTime(date);

        if (0 > tzsecs) {
            tzsign = '-';
            tzhour = (int)(tzsecs / -3600);
            tzmin = (int)(tzsecs / -60) - (tzhour * 60);
        } else {
            tzhour = (int)(tzsecs / 3600);
            tzmin = (int)(tzsecs / 60) - (tzhour * 60);
        }

        if (0 == nsec || 0 == opts.sec_prec) {
            if (0 == tzsecs && obj.callMethod(context, "utc?").isTrue()) {
                formatter.format("%04d-%02d-%02dT%02d:%02d:%02dZ",
                        tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                        tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND));
                dump_cstr(buf.toString(), false, false);
            } else {
                formatter.format("%04d-%02d-%02dT%02d:%02d:%02d%c%02d:%02d",
                        tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                        tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND),
                        tzsign, tzhour, tzmin);
                dump_cstr(buf.toString(), false, false);
            }
        } else if (0 == tzsecs && obj.callMethod(context, "utc?").isTrue()) {
            String format = "%04d-%02d-%02dT%02d:%02d:%02d.%09dZ";

            if (9 > opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + opts.sec_prec);
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec);
            dump_cstr(buf.toString(), false, false);
        } else {
            String format = "%04d-%02d-%02dT%02d:%02d:%02d.%09d%c%02d:%02d";

            if (9 > opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + opts.sec_prec) + "d%c%02d:%02d";
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec,
                    tzsign, tzhour, tzmin);
            dump_cstr(buf.toString(), false, false);
        }
    }

    // FIXME: both C and Java can crash potentially I added check here but
    // I should see if C oj crashes for these cases.
    protected ByteList stringToByteList(IRubyObject obj, String method) {
        IRubyObject stringResult = obj.callMethod(context, method);

        if (!(stringResult instanceof RubyString)) {
            throw context.runtime.newTypeError("Expected a String");
        }

        return ((RubyString) stringResult).getByteList();
    }

    protected void dump_to_hash(IRubyObject object, int depth) {
        dump_hash(TypeConverter.checkHashType(runtime, object.callMethod(context, "to_hash")), depth);
    }

    protected void dump_as_json(IRubyObject object, int depth) {
        IRubyObject aj = object.callMethod(context, "as_json");

        if (aj == object) {   // Catch the obvious brain damaged recursive dumping.
            dump_cstr(stringToByteList(object, "to_s"), false, false);
        } else {
            dump_val(aj, depth);
        }
    }

    protected void dump_obj_attrs(IRubyObject obj, RubyClass clas, long id, int depth) {
        int		d2 = depth + 1;

        append('{');
        if (clas != null) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(d2);
            append(O_KEY);
            dump_cstr(class_name, false, false);
        }
        if (0 < id) {
            append(',');
            fill_indent(d2);
            append(I_KEY);
            dump_ulong(id);
        }

        if (obj instanceof RubyString) {
            append(',');
            fill_indent(d2);
            append(SELF_KEY);
            dump_cstr(((RubyString) obj).getByteList(), false, false);
        } else if (obj instanceof RubyArray) {
            append(',');
            fill_indent(d2);
            append(SELF_KEY);
            dump_array((RubyArray) obj, depth + 1);
        } else if (obj instanceof RubyHash) {
            append(',');
            fill_indent(d2);
            append(SELF_KEY);
            dump_hash(obj, depth + 1);
        }

        List<Variable<Object>> variables = obj.getVariableList();

        if (clas != null && !variables.isEmpty()) {
            append(',');
        }

        boolean first = true;
        for (Variable<Object> variable: variables) {
            String name = variable.getName();
            // FIXME: We may crash if non ruby object is internal????
            IRubyObject value = (IRubyObject) variable.getValue();

            if (opts.ignore != null && dump_ignore(value)) continue;
            if (omit_nil && value.isNil()) continue;

            if (first) {
                first = false;
            } else {
                append(',');
            }

            fill_indent(d2);

            if (name.charAt(0) == '@') {
                dump_cstr(name.substring(1), false, false);
            } else {
                dump_cstr("~" + name, false, false);
            }
            append(':');
            dump_val(value, d2);
        }
        this.depth = depth;
        fill_indent(depth);
        append('}');
    }

    protected void dump_odd(IRubyObject obj, Odd odd, RubyClass clas, int depth) {
        int	d2 = depth + 1;

        append('{');
        if (clas != null) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(d2);
            append(BIG_O_KEY);
            dump_cstr(class_name, false, false);
            append(',');
        }
        if (odd.raw) {
            RubyString str = (RubyString) obj.callMethod(context, odd.attrs[0]).checkStringType();
            append('"');
            append(odd.attrs[0]);
            append('"');
            append(':');
            append(str.getByteList());
        } else {
            for (int index = 0; index < odd.attrs.length; index++) {
                String name = odd.attrs[index];
                IRubyObject value = oddValue(context, obj, name, odd, index);
                fill_indent(d2);
                dump_cstr(name, false, false);
                append(':');
                dump_val(value, d2);
                append(',');
            }
            pop(); // remove last ','
        }
        append('}');
    }

    private static IRubyObject oddValue(ThreadContext context, IRubyObject obj, String name, Odd odd, int index) {
        if (odd.attrFuncs[index] != null) return odd.attrFuncs[index].execute(context, obj);

        if (name.indexOf('.') == -1) return obj.callMethod(context, name);

        IRubyObject value = obj;
        for (String segment : name.split("\\.")) {
            value = value.callMethod(context, segment);
        }
        return value;
    }

    void raise_strict(IRubyObject obj) {
        throw runtime.newTypeError("Failed to dump " + obj.getMetaClass().getName() + " Object to JSON in strict mode.");
    }

    public void dump_val(IRubyObject obj, int depth) {
        if (MAX_DEPTH < depth) {
            throw new RaiseException(context.runtime, context.runtime.getNoMemoryError(), "Too deeply nested.", true);
        }

        if (obj instanceof RubyNil) {
            dump_nil();
        } else if (obj instanceof RubyBoolean) {
            if (obj == context.runtime.getTrue()) {
                dump_true();
            } else {
                dump_false();
            }
        } else if (obj instanceof RubyFixnum) {
            dump_fixnum((RubyFixnum) obj);
        } else if (obj instanceof RubyFloat) {
            dump_float((RubyFloat) obj);
        } else if (obj instanceof RubyModule) {  // Also will be RubyClass
            dump_class((RubyModule) obj);
        } else if (obj instanceof RubySymbol) {
            dump_sym((RubySymbol) obj);
        } else if (obj instanceof RubyStruct) {
            dump_struct((RubyStruct) obj, depth);
        } else if (obj instanceof RubyRange) { // In MRI T_STRUCT is also Range
            dump_range((RubyRange) obj, depth);
        } else {
            // FIXME: this else section can be another overridable method so this can
            // be supered in at top for object and possibly compat
            RubyClass clas = obj.getMetaClass();
            Odd odd;

            if (ObjectMode == opts.mode && null != (odd = oj.getOdd(clas))) {
                dump_odd(obj, odd, clas, depth + 1);
                return;
            }

            if (obj instanceof RubyBignum) {
                dump_bignum((RubyBignum) obj);
            } else if (obj.getMetaClass() == context.runtime.getString()) {
                dump_str((RubyString) obj);
            } else if (obj.getMetaClass() == context.runtime.getArray()) {
                dump_array((RubyArray) obj, depth);
            } else if (obj.getMetaClass() == context.runtime.getHash()) {
                dump_hash(obj, depth);
            } else if (obj instanceof RubyComplex) {
                dump_complex(obj, depth);
            } else if (obj instanceof RubyRegexp) {
                dump_regexp(obj, depth);
            } else if (obj instanceof RubyTime) { // T_DATA
                dump_time((RubyTime) obj, depth);
            } else if (obj instanceof RubyBigDecimal) { // T_DATA
                dump_bigdecimal((RubyBigDecimal) obj, depth);
            } else if (obj instanceof RubyRational) {
                dump_rational((RubyRational) obj, depth);
            } else {
                dump_other(obj, depth);
            }
        }
    }

    protected ByteList obj_to_json_using_params(IRubyObject obj, IRubyObject[] argv) {
        if (opts.circular) new_circ_cache();

        this.argv = argv;

        dump_val(obj, 0);
        if (indent > 0) {
            switch (peek(0)) {
                case ']':case '}': append('\n');
            }
        }

        if (opts.circular) delete_circ_cache();

        return buf;
    }

    // Unlike C version we assume check has been made that there is an ignore list
    // and we are in the correct mode.
    boolean dump_ignore(IRubyObject value) {
        if (opts.ignore == null) return false;

        RubyModule clas = value.getMetaClass();

        for (RubyModule module: opts.ignore) {
            if (module == clas) return true;
        }

        return false;
    }

    public void append(int aByte) {
        buf.append((byte)aByte);
    }

    public void append(byte[] bytes) {
        buf.append(bytes);
    }

    public void append(byte[] bytes, int start, int length) {
        buf.append(bytes, start, length);
    }

    public void append(ByteList bytes) {
        buf.append(bytes);
    }

    public void append(String string) {
        buf.append(string.getBytes());
    }

    public int get(int offset) {
        return buf.get(buf.realSize()+offset);
    }


    // return curr +- offset char
    public int peek(int offset) {
        return buf.get(buf.realSize() - 1 + offset);
    }

    public void pop() {
        buf.realSize(buf.realSize() - 1);
    }

    public int size() {
        return buf.realSize();
    }

    public void write(FileOutputStream f) throws IOException {
        f.write(buf.unsafeBytes(), buf.begin(), buf.realSize());
    }

    // FIXME: If out instances live across many of these we should just reset and not realloc over and over.
    public void new_circ_cache() {
        circ_cache = new HashMap<>();
    }

    public void delete_circ_cache() {
        circ_cache = null;
    }

    public void reset() {
        buf = Parse.newByteList();
    }

    public RubyString asString(ThreadContext context) {
        return context.runtime.newString(buf);
    }

    protected static boolean isEscapeString(ByteList str) {
        if (str.realSize() >=2 ) {
            int s = str.get(0);
            if (s == ':') return true;
            if (s == '^'){
                s = str.get(1);
                return s == 'r' || s == 'i';
            }
        }
        return false;
    }

    protected byte[] nan_str(IRubyObject value, NanDump nd, char mode, boolean positive) {
        if (nd == NanDump.AutoNan) {
            switch (mode) {
                case CompatMode: nd = NanDump.WordNan; break;
                case StrictMode: nd = NanDump.RaiseNan; break;
            }
        }
        switch(nd) {
            case RaiseNan: raise_strict(value); break;
            case WordNan: return positive ? INFINITY_VALUE : NINFINITY_VALUE;
            case NullNan: return NULL_VALUE;
            case HugeNan:
                return INF_VALUE;
        }

        return null; // C source does this but this I believe will crash in both impls...let's see....
    }
}