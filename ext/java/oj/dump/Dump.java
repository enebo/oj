package oj.dump;

import jnr.posix.util.Platform;
import oj.Odd;
import oj.OjLibrary;
import oj.Options;
import oj.Out;
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
import java.util.List;
import java.util.TimeZone;

import static oj.NumInfo.OJ_INFINITY;
import static oj.Options.*;
import static oj.Options.Yes;

/**
 * Created by enebo on 8/29/18.
 */
public abstract class Dump {
    protected ThreadContext context;
    protected Ruby runtime;
    public Out out;

    public static Dump createDump(ThreadContext context, OjLibrary oj, Options opts) {
        Out out = new Out(oj, opts);

        switch (opts.mode) {
            case NullMode: return new NullDump(context, out);
            case StrictMode: return new StrictDump(context, out);
            case CompatMode: return new CompatDump(context, out);
            case ObjectMode:
            default: //FIXME consider not defaulting or understand what default is.
                return new ObjectDump(context, out);
        }
    }

    public static Dump createDump(ThreadContext context, Out out, int mode) {
        switch (mode) {
            case NullMode: return new NullDump(context, out);
            case StrictMode: return new StrictDump(context, out);
            case CompatMode: return new CompatDump(context, out);
            case ObjectMode:
            default: //FIXME consider not defaulting or understand what default is.
                return new ObjectDump(context, out);
        }
    }

    public Dump(ThreadContext context, Out out) {
        this.context = context;
        this.runtime = context.runtime;
        this.out = out;
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
            ((StringIO) obj).write(context, context.runtime.newString(out.buf));
        } else if (stream.respondsTo("write")) {
            stream.callMethod(context, "write", context.runtime.newString(out.buf));
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
        if (out.indent > 0) {
            cnt *= out.indent;
            out.append('\n');
            for (; 0 < cnt; cnt--) {
                out.append(' ');
            }
        }
    }

    static void dump_ulong(long num, Out out) {
        byte[] buf = new byte[32]; // FIXME: Can be instance variable
        int	b = buf.length - 1;

        if (0 < num) {
            for (; 0 < num; num /= 10, b--) {
                buf[b] = (byte) ((num % 10) + '0');
            }
            b++;
        } else {
            buf[b] = '0';
        }
        for (; b < buf.length; b++) {
            out.append(buf[b]);
        }
    }

    static void dump_hex(int c, Out out) {
        int	d = (c >> 4) & 0x0F;

        out.append(hex_chars.charAt(d));
        d = c & 0x0F;
        out.append(hex_chars.charAt(d));
    }

    public void dump_raw(byte[] str) {
        out.append(str);
    }

    public void dump_raw(ByteList str) {
        out.append(str);
    }

    static int dump_unicode(ThreadContext context, ByteList str, int str_i, int end, Out out) {
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
            out.append('\\');
            out.append('u');
            for (i = 3; 0 <= i; i--) {
                out.append(hex_chars.charAt((int)(c1 >> (i * 4)) & 0x0F));
            }
        }
        out.append('\\');
        out.append('u');
        for (i = 3; 0 <= i; i--) {
            out.append(hex_chars.charAt((int)(code >> (i * 4)) & 0x0F));
        }
        return str_i - 1;
    }

    // returns 0 if not using circular references, -1 if not further writing is
    // needed (duplicate), and a positive value if the object was added to the cache.
    protected long check_circular(IRubyObject obj, Out out) {
        Integer	id = 0;

        if (ObjectMode == out.opts.mode && Yes == out.opts.circular) {
            id = out.circ_cache.get(obj);
            if (id == null) {
                out.circ_cnt++;
                id = out.circ_cnt;
                out.circ_cache.put(obj, id);
            } else {
                out.append(PARTIAL_R_KEY);
                dump_ulong(id.longValue(), out);
                out.append('"');

                return -1;
            }
        }
        return id.longValue();
    }

    protected void dump_nil() {
        out.append(NULL_VALUE);
    }

    protected void dump_true() {
        out.append(TRUE_VALUE);
    }

    protected void dump_false() {
        out.append(FALSE_VALUE);
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
        out.append(buf, b, size);
    }

    protected void dump_bignum(RubyBignum obj) {
        // Note: This uses boxed call to to_s because 9.1 -> 9.2 changed return type on non-boxed version
        // from IRubyObject -> RubyString.
        out.append(obj.to_s(new IRubyObject[] { context.runtime.newFixnum(10) }).convertToString().getByteList());
    }

    // fIXME: this may be for object only
    protected void dump_float(RubyFloat obj) {
        double d = obj.getDoubleValue();

        if (d == 0.0) {
            out.append(ZERO_POINT_ZERO);
        } else if (d == OJ_INFINITY) {
            dumpInfNanForFloat(out, obj, INF_VALUE, INFINITY_VALUE);
        } else if (d == -OJ_INFINITY) {
            dumpInfNanForFloat(out, obj, NINF_VALUE, NINFINITY_VALUE);
        } else if (Double.isNaN(d)) {
            dumpNanNanForFloat(out, obj);
        } else if (0 == out.opts.float_prec) {
            IRubyObject	rstr = Helpers.invoke(context, obj, "to_s");
            RubyString str = (RubyString) TypeConverter.checkStringType(context.runtime, rstr);
            out.append(str.getByteList().bytes());
        } else {
            ByteList buf = new ByteList();
            Sprintf.sprintf(buf, out.opts.float_fmt, obj);
            out.append(buf);
        }
    }

    private void dumpNanNanForFloat(Out out, IRubyObject value) {
        if (out.opts.mode == ObjectMode) {
            out.append(NAN_NUMERIC_VALUE);
        } else {
            NanDump nd = out.opts.dump_opts.nan_dump;

            if (nd == NanDump.AutoNan) {
                switch (out.opts.mode) {
                    case CompatMode: nd = NanDump.WordNan; break;
                    case StrictMode: nd = NanDump.RaiseNan; break;
                    case NullMode: nd = NanDump.NullNan; break;
                }
            }

            switch(nd) {
                case RaiseNan: raise_strict(value); break;
                case WordNan: out.append(NAN_VALUE); break;
                case NullNan: out.append(NULL_VALUE); break;
                case HugeNan:
                default: out.append(NAN_NUMERIC_VALUE); break;
            }
        }
    }
    private void dumpInfNanForFloat(Out out, IRubyObject value, byte[] inf_value, byte[] infinity_value) {
        if (out.opts.mode == ObjectMode) {
            out.append(inf_value);
        } else {
            NanDump nd = out.opts.dump_opts.nan_dump;

            if (nd == NanDump.AutoNan) {
                switch (out.opts.mode) {
                    case CompatMode: nd = NanDump.WordNan; break;
                    case StrictMode: nd = NanDump.RaiseNan; break;
                    case NullMode: nd = NanDump.NullNan; break;
                    case CustomMode:  nd = NanDump.NullNan; break;
                }
            }

            switch(nd) {
                case RaiseNan: raise_strict(value); break;
                case WordNan: out.append(infinity_value); break;
                case NullNan: out.append(NULL_VALUE); break;
                case HugeNan:
                default: out.append(inf_value); break;
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

        switch (out.opts.escape_mode) {
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

        out.append('"');
        if (escape1) {
            out.append('\\');
            out.append('u');
            out.append('0');
            out.append('0');
            dump_hex(str.get(str_i), out);
            cnt--;
            size--;
            str_i++;
            is_sym = false; // just to make sure
        }
        if (cnt == size) {
            if (is_sym) out.append(':');
            out.append(str.unsafeBytes(), str.begin() + str_i, cnt);
            out.append('"');
        } else {
            if (is_sym) {
                out.append(':');
            }
            for (; str_i < cnt; str_i++) {
                switch (cmap[(int)str.get(str_i) & 0xff]) {
                    case 1:
                        out.append(str.get(str_i));
                        break;
                    case 2:
                        out.append('\\');
                        switch ((byte) str.get(str_i)) {
                            case '\\':	out.append('\\');	break;
                            case '\b':	out.append('b');	break;
                            case '\t':	out.append('t');	break;
                            case '\n':	out.append('n');	break;
                            case '\f':	out.append('f');	break;
                            case '\r':	out.append('r');	break;
                            default:	out.append(str.get(str_i));	break;
                        }
                        break;
                    case 3: // Unicode
                        str_i = dump_unicode(context, str, str_i, cnt, out);
                        break;
                    case 6: // control characters
                        out.append('\\');
                        out.append('u');
                        out.append('0');
                        out.append('0');
                        dump_hex(str.get(str_i), out);
                        break;
                    default:
                        break; // ignore, should never happen if the table is correct
                }
            }
            out.append('"');
        }
    }

    protected abstract void dump_bigdecimal(RubyBigDecimal bigdecimal, int depth);
    protected abstract void dump_str(RubyString string);
    protected abstract void dump_time(RubyTime time, int depth);
    protected abstract void dump_sym(RubySymbol symbol);
    protected abstract void dump_class(RubyModule clas);
    protected abstract String modeName();

    protected void dump_array(RubyArray array, int depth) {
        int d2 = depth + 1;
        long id = check_circular(array, out);

        if (id < 0) return; // duplicate found (written out in check_circular)

        out.append('[');
        if (id > 0) {
            fill_indent(d2);
            out.append(PARTIAL_I_KEY);
            dump_ulong(id, out);
            out.append('"');
        }

        if (array.isEmpty()) {
            out.append(']');
        } else {
            if (id > 0) out.append(',');

            int cnt = array.getLength() - 1;

            for (int i = 0; i <= cnt; i++) {
                indent(d2, out.opts.dump_opts.array_nl);
                dump_val(array.eltInternal(i), d2, null);
                if (i < cnt) out.append(',');
            }
            indent(depth, out.opts.dump_opts.array_nl);
            out.append(']');
        }
    }

    protected void indent(int depth, ByteList nl) {
        if (out.opts.dump_opts.use) {
            if (nl != ByteList.EMPTY_BYTELIST) out.append(nl);

            if (out.opts.dump_opts.indent_str != ByteList.EMPTY_BYTELIST) {
                for (int j = depth; 0 < j; j--) {
                    out.append(out.opts.dump_opts.indent_str);
                }
            }
        } else {
            fill_indent(depth);
        }
    }

    protected void visit_hash(IRubyObject key, IRubyObject value) {
        if (out.omit_nil && value.isNil()) return;

        int depth = out.depth;

        if (!(key instanceof RubyString)) {
            throw context.runtime.newTypeError("In :" + modeName() + " mode all Hash keys must be Strings, not " + key.getMetaClass().getName());
        }
        if (out.opts.dump_opts.use) {
            if (out.opts.dump_opts.hash_nl != ByteList.EMPTY_BYTELIST) {
                out.append(out.opts.dump_opts.hash_nl);
            }
            if (out.opts.dump_opts.indent_str != ByteList.EMPTY_BYTELIST) {
                int	i;
                for (i = depth; 0 < i; i--) {
                    out.append(out.opts.dump_opts.indent_str);
                }
            }
            dump_str((RubyString) key);
            if (out.opts.dump_opts.before_sep != ByteList.EMPTY_BYTELIST) {
                out.append(out.opts.dump_opts.before_sep);
            }
            out.append(':');
            if (out.opts.dump_opts.after_sep != ByteList.EMPTY_BYTELIST) {
                out.append(out.opts.dump_opts.after_sep);
            }
        } else {
            fill_indent(depth);
            dump_str((RubyString) key);
            out.append(':');
        }
        dump_val(value, depth, null);
        out.depth = depth;
        out.append(',');
    }

    protected void dump_hash(IRubyObject obj, int depth) {
        RubyHash hash = (RubyHash) obj;
        int cnt = hash.size();
        if (0 == cnt) {
            out.append('{');
            out.append('}');
        } else {
            long id = check_circular(hash, out);
            if (id < 0) return;

            out.append('{');
            if (id > 0) {
                fill_indent(depth + 1);
                out.append(I_KEY);
                dump_ulong(id, out);
                out.append(',');
            }
            out.depth = depth + 1;
            hash.visitAll(context,
                    new RubyHash.VisitorWithState<Out>() {
                        @Override
                        public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Out out) {
                            visit_hash(key, value);
                        }
                    },
                    out);
            if (',' == out.get(-1)) out.pop(); // backup to overwrite last comma
            indent(depth, out.opts.dump_opts.hash_nl);
            out.append('}');
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
        if (0 < out.opts.sec_prec) {
            if (9 > out.opts.sec_prec) {
                for (int i = 9 - out.opts.sec_prec; 0 < i; i--) {
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
        out.append(buf, b, size);
    }

    protected void dump_obj_comp(IRubyObject obj, int depth, IRubyObject[] argv) {
        if (obj.respondsTo("to_hash")) {
            dump_to_hash(obj, depth);
        } else if (obj.respondsTo("as_json")) {
            dump_as_json(obj, depth);
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(obj, "to_json"));
        } else {
            if (obj instanceof RubyBigDecimal) {
                ByteList rstr = stringToByteList(obj, "to_s");
                if (Yes == out.opts.bigdec_as_num) {
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

        if (9 > out.opts.sec_prec) {
            int	i;

            for (i = 9 - out.opts.sec_prec; 0 < i; i--) {
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

        if (0 == nsec || 0 == out.opts.sec_prec) {
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

            if (9 > out.opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + out.opts.sec_prec);
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec);
            dump_cstr(buf.toString(), false, false);
        } else {
            String format = "%04d-%02d-%02dT%02d:%02d:%02d.%09d%c%02d:%02d";

            if (9 > out.opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + out.opts.sec_prec) + "d%c%02d:%02d";
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec,
                    tzsign, tzhour, tzmin);
            dump_cstr(buf.toString(), false, false);
        }
    }

    protected abstract void dump_other(IRubyObject obj, int depth, IRubyObject[] args);


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
            dump_val(aj, depth, null);
        }
    }

    protected abstract void dump_complex(IRubyObject obj, int depth, IRubyObject[] args);
    protected abstract void dump_regexp(IRubyObject obj, int depth, IRubyObject[] args);

    protected void dump_obj_attrs(IRubyObject obj, RubyClass clas, long id, int depth) {
        int		d2 = depth + 1;

        out.append('{');
        if (clas != null) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(d2);
            out.append(O_KEY);
            dump_cstr(class_name, false, false);
        }
        if (0 < id) {
            out.append(',');
            fill_indent(d2);
            out.append(I_KEY);
            dump_ulong(id, out);
        }

        if (obj instanceof RubyString) {
            out.append(',');
            fill_indent(d2);
            out.append(SELF_KEY);
            dump_cstr(((RubyString) obj).getByteList(), false, false);
        } else if (obj instanceof RubyArray) {
            out.append(',');
            fill_indent(d2);
            out.append(SELF_KEY);
            dump_array((RubyArray) obj, depth + 1);
        } else if (obj instanceof RubyHash) {
            out.append(',');
            fill_indent(d2);
            out.append(SELF_KEY);
            dump_hash(obj, depth + 1);
        }

        List<Variable<Object>> variables = obj.getVariableList();

        if (clas != null && !variables.isEmpty()) {
            out.append(',');
        }

        boolean first = true;
        for (Variable<Object> variable: variables) {
            String name = variable.getName();
            // FIXME: We may crash if non ruby object is internal????
            IRubyObject value = (IRubyObject) variable.getValue();

            if (out.opts.ignore != null && dump_ignore(out, value)) continue;
            if (out.omit_nil && value.isNil()) continue;

            if (first) {
                first = false;
            } else {
                out.append(',');
            }

            fill_indent(d2);

            if (name.charAt(0) == '@') {
                dump_cstr(name.substring(1), false, false);
            } else {
                dump_cstr("~" + name, false, false);
            }
            out.append(':');
            dump_val(value, d2, null);
        }
        out.depth = depth;
        fill_indent(depth);
        out.append('}');
    }

    protected abstract void dump_range(RubyRange obj, int depth);
    protected abstract void dump_struct(RubyStruct obj, int depth);

    protected void dump_odd(IRubyObject obj, Odd odd, RubyClass clas, int depth) {
        int	d2 = depth + 1;

        out.append('{');
        if (clas != null) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(d2);
            out.append(BIG_O_KEY);
            dump_cstr(class_name, false, false);
            out.append(',');
        }
        if (odd.raw) {
            RubyString str = (RubyString) obj.callMethod(context, odd.attrs[0]).checkStringType();
            out.append('"');
            out.append(odd.attrs[0]);
            out.append('"');
            out.append(':');
            out.append(str.getByteList());
        } else {
            for (int index = 0; index < odd.attrs.length; index++) {
                String name = odd.attrs[index];
                IRubyObject value = oddValue(context, obj, name, odd, index);
                fill_indent(d2);
                dump_cstr(name, false, false);
                out.append(':');
                dump_val(value, d2, null);
                out.append(',');
            }
            out.pop(); // remove last ','
        }
        out.append('}');
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

    public void dump_val(IRubyObject obj, int depth, IRubyObject[] argv) {
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

            if (ObjectMode == out.opts.mode && null != (odd = out.oj.getOdd(clas))) {
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
                dump_complex(obj, depth, argv);
            } else if (obj instanceof RubyRegexp) {
                dump_regexp(obj, depth, argv);
            } else if (obj instanceof RubyTime) {
                dump_time((RubyTime) obj, depth);
            } else if (obj instanceof RubyBigDecimal) {  // FIXME: not sure it is only these two types.
                dump_bigdecimal((RubyBigDecimal) obj, depth);
            } else {
                dump_other(obj, depth, argv);
            }
        }
    }

    protected ByteList obj_to_json_using_params(IRubyObject obj, IRubyObject[] argv) {
        if (Yes == out.opts.circular) out.new_circ_cache();

        dump_val(obj, 0, argv);
        if (out.indent > 0) {
            switch (out.peek(0)) {
                case ']':case '}': out.append('\n');
            }
        }

        if (Yes == out.opts.circular) out.delete_circ_cache();

        return out.buf;
    }

    // Unlike C version we assume check has been made that there is an ignore list
    // and we are in the correct mode.
    static boolean dump_ignore(Out out, IRubyObject value) {
        RubyModule clas = value.getMetaClass();

        for (RubyModule module: out.opts.ignore) {
            if (module == clas) return true;
        }

        return false;
    }
}