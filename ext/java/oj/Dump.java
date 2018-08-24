package oj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.TimeZone;

import jnr.posix.util.Platform;
import oj.options.DumpType;
import org.jcodings.specific.UTF8Encoding;
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
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;

import static oj.options.DumpType.ArrayNew;
import static oj.options.DumpType.ArrayType;
import static oj.options.DumpType.ObjectNew;
import static oj.options.DumpType.ObjectType;
import static oj.Options.*;
import static oj.NumInfo.OJ_INFINITY;


/**
 * Created by enebo on 9/9/15.
 */
public class Dump {
    static int MAX_DEPTH = 1000;

    private static final byte[] BIG_O_KEY = {'"', '^', 'O', '"', ':'};
    private static final byte[] C_KEY = {'"', '^', 'c', '"', ':'};
    private static final byte[] O_KEY = {'"', '^', 'o', '"', ':'};
    private static final byte[] I_KEY = {'"', '^', 'i', '"', ':'};
    private static final byte[] T_KEY = {'"', '^', 't', '"', ':'};
    private static final byte[] U_KEY = {'"', '^', 'u', '"', ':', '['};
    private static final byte[] SELF_KEY = {'"', 's', 'e', 'l', 'f', '"', ':'};
    private static final byte[] NULL_VALUE = {'n', 'u', 'l', 'l'};
    private static final byte[] TRUE_VALUE = {'t', 'r', 'u', 'e'};
    private static final byte[] FALSE_VALUE = {'f', 'a', 'l', 's', 'e'};
    private static final byte[] INFINITY_VALUE = {'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'};
    private static final byte[] NAN_VALUE = {'N', 'a', 'N'};

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

    static void fill_indent(Out out, int cnt) {
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

    static void dump_raw(ByteList str, Out out) {
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
    static long check_circular(IRubyObject obj, Out out) {
        Integer	id = 0;

        if (ObjectMode == out.opts.mode && Yes == out.opts.circular) {
            id = out.circ_cache.get(obj);
            if (id == null) {
                out.circ_cnt++;
                id = out.circ_cnt;
                out.circ_cache.put(obj, id);
            } else {
                out.append('"');
                out.append('^');
                out.append('r');
                dump_ulong(id.longValue(), out);
                out.append('"');

                return -1;
            }
        }
        return id.longValue();
    }

    static void dump_nil(Out out) {
        out.append(NULL_VALUE);
    }

    static void dump_true(Out out) {
        out.append(TRUE_VALUE);
    }

    static void dump_false(Out out) {
        out.append(FALSE_VALUE);
    }

    static void dump_fixnum(RubyFixnum obj, Out out) {
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

    static void dump_bignum(ThreadContext context, RubyBignum obj, Out out) {
        // Note: This uses boxed call to to_s because 9.1 -> 9.2 changed return type on non-boxed version
        // from IRubyObject -> RubyString.
        out.append(obj.to_s(new IRubyObject[] { context.runtime.newFixnum(10) }).convertToString().getByteList());
    }

    static void dump_float(ThreadContext context, RubyFloat obj, Out out) {
        double d = obj.getDoubleValue();

        if (0.0 == d) {
            out.append('0');
            out.append('.');
            out.append('0');
        } else if (OJ_INFINITY == d) {
            switch (out.opts.mode) {
                case StrictMode:
                    raise_strict(obj);
                case NullMode:
                    out.append(NULL_VALUE);
                    break;
                default:
                    out.append(INFINITY_VALUE);
                    break;
            }
        } else if (-OJ_INFINITY == d) {
            switch (out.opts.mode) {
                case StrictMode:
                    raise_strict(obj);
                case NullMode:
                    out.append(NULL_VALUE);
                    break;
                default:
                    out.append('-');
                    out.append(INFINITY_VALUE);
                    break;
            }
        } else if (Double.isNaN(d)) {
            switch (out.opts.mode) {
                case StrictMode:
                    raise_strict(obj);
                case NullMode:
                    out.append(NULL_VALUE);
                    break;
                default:
                    out.append(NAN_VALUE);
                    break;
            }
        /*} else if (d == (double)(long)d) {  // FIXME: Precision overflow?
            cnt = snprintf(buf, sizeof(buf), "%.1f", d);*/
        } else if (0 == out.opts.float_prec) {
            IRubyObject	rstr = Helpers.invoke(context, obj, "to_s");

            if (!(rstr instanceof RubyString)) {
                throw context.runtime.newArgumentError("Expected a String");
            }

            out.append(((RubyString) rstr).getByteList().bytes());
        } else {
            ByteList buf = new ByteList();
            Sprintf.sprintf(buf, out.opts.float_fmt, obj);
            out.append(buf);
        }
    }

    // FIXME: This is going to mess up blindly grabbing bytes if it mismatches UTF-8 (I believe all strings
    // will be UTF-8 or clean ascii 7-bit).
    static void dump_cstr(ThreadContext context, String str, boolean is_sym, boolean escape1, Out out) {
        dump_cstr(context, new ByteList(str.getBytes(), UTF8Encoding.INSTANCE), is_sym, escape1, out);
    }

    static void dump_cstr(ThreadContext context, ByteList str, boolean is_sym, boolean escape1, Out out) {
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

    static void dump_str_comp(ThreadContext context, RubyString obj, Out out) {
        dump_cstr(context, obj.getByteList(), false, false, out);
    }

    static void dump_str_obj(ThreadContext context, RubyString string, Out out) {
        ByteList str = string.getByteList();

        boolean escape = isEscapeString(str);
        if (string.isAsciiOnly() && !escape) { // Fast path.  JRuby already knows if it is a clean ASCII string.
            out.append('"');
            out.append(str.unsafeBytes(), str.begin(), str.realSize());
            out.append('"');
        } else {
            dump_cstr(context, str, false, escape, out);
        }
    }

    static boolean isEscapeString(ByteList str) {
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

    static void dump_sym_comp(ThreadContext context, RubySymbol obj, Out out) {
        dump_cstr(context, ((RubyString) obj.to_s()).getByteList(), false, false, out);
    }

    static void dump_sym_obj(ThreadContext context, RubySymbol obj, Out out) {
        dump_cstr(context, ((RubyString) obj.to_s()).getByteList(), true, false, out);
    }

    static void dump_class_comp(ThreadContext context, RubyModule clas, Out out) {
        dump_cstr(context, new ByteList(clas.getName().getBytes()), false, false, out);
    }

    static void dump_class_obj(ThreadContext context, RubyModule clas, Out out) {
        out.append('{');
        out.append(C_KEY);
        dump_cstr(context, new ByteList(clas.getName().getBytes()), false, false, out);
        out.append('}');
    }

    static void dump_array(ThreadContext context, IRubyObject a, RubyClass clas, int depth, Out out) {
        int		i, cnt;
        int		d2 = depth + 1;
        long	id = check_circular(a, out);

        if (id < 0) {
            return;
        }
        if (null != clas && !(a instanceof RubyArray) && ObjectMode == out.opts.mode) {
            dump_obj_attrs(context, a, clas, 0, depth, out);
            return;
        }
        cnt = ((RubyArray) a).getLength();
        out.append('[');
        if (0 < id) {
            fill_indent(out, d2);
            out.append('"');
            out.append('^');
            out.append('i');
            dump_ulong(id, out);
            out.append('"');
        }

        if (0 == cnt) {
            out.append(']');
        } else {
            if (0 < id) {
                out.append(',');
            }

            cnt--;
            for (i = 0; i <= cnt; i++) {
                if (null == out.opts.dump_opts) {
                    fill_indent(out, d2);
                } else {
                    if (0 < out.opts.dump_opts.array_size) {
                        out.append(out.opts.dump_opts.array_nl);
                    }
                    if (0 < out.opts.dump_opts.indent_size) {
                        for (int j = d2; 0 < j; j--) {
                            out.append(out.opts.dump_opts.indent_str);
                        }
                    }
                }
                dump_val(context, ((RubyArray) a).eltOk(i), d2, out, null);
                if (i < cnt) {
                    out.append(',');
                }
            }
            if (null == out.opts.dump_opts) {
                fill_indent(out, depth);
            } else {
                //printf("*** d2: %u  indent_str: %u '%s'\n", d2, out.opts.dump_opts.indent_size, out.opts.dump_opts.indent_str);
                if (0 < out.opts.dump_opts.array_size) {
                    out.append(out.opts.dump_opts.array_nl);
                }
                if (0 < out.opts.dump_opts.indent_size) {
                    for (int j = depth; 0 < j; j--) {
                        out.append(out.opts.dump_opts.indent_str);
                    }
                }
            }
            out.append(']');
        }
    }

    static void hash_cb_strict(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
        int		depth = out.depth;

        if (!(key instanceof RubyString)) {
            throw context.runtime.newTypeError("In :strict mode all Hash keys must be Strings, not " + key.getMetaClass().getName());
        }
        if (null == out.opts.dump_opts) {
            fill_indent(out, depth);
            dump_str_comp(context, (RubyString) key, out);
            out.append(':');
        } else {
            if (0 < out.opts.dump_opts.hash_size) {
                out.append(out.opts.dump_opts.hash_nl);
            }
            if (0 < out.opts.dump_opts.indent_size) {
                int	i;
                for (i = depth; 0 < i; i--) {
                    out.append(out.opts.dump_opts.indent_str);
                }
            }
            dump_str_comp(context, (RubyString) key, out);
            if (0 < out.opts.dump_opts.before_size) {
                out.append(out.opts.dump_opts.before_sep);
            }
            out.append(':');
            if (0 < out.opts.dump_opts.after_size) {
                out.append(out.opts.dump_opts.after_sep);
            }
        }
        dump_val(context, value, depth, out, null);
        out.depth = depth;
        out.append(',');
    }

    static void hash_cb_compat(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
        int		depth = out.depth;

        if (null == out.opts.dump_opts) {
            fill_indent(out, depth);
        } else {
            if (0 < out.opts.dump_opts.hash_size) {
                out.append(out.opts.dump_opts.hash_nl);
            }
            if (0 < out.opts.dump_opts.indent_size) {
                int	i;
                for (i = depth; 0 < i; i--) {
                    out.append(out.opts.dump_opts.indent_str);
                }
            }
        }

        if (key instanceof RubyString) {
            dump_str_comp(context, (RubyString) key, out);
        } else if (key instanceof RubySymbol) {
            dump_sym_comp(context, (RubySymbol) key, out);
        } else {
            /*rb_raise(rb_eTypeError, "In :compat mode all Hash keys must be Strings or Symbols, not %s.\n", rb_class2name(rb_obj_class(key)));*/
            dump_cstr(context, stringToByteList(context, key, "to_s"), false, false, out);
        }
        if (null == out.opts.dump_opts) {
            out.append(':');
        } else {
            if (0 < out.opts.dump_opts.before_size) {
                out.append(out.opts.dump_opts.before_sep);
            }
            out.append(':');
            if (0 < out.opts.dump_opts.after_size) {
                out.append(out.opts.dump_opts.after_sep);
            }
        }
        dump_val(context, value, depth, out, null);
        out.depth = depth;
        out.append(',');
    }

    static void hash_cb_object(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
        if (out.opts.ignore != null && dump_ignore(out, value)) return;
        if (out.omit_nil && value.isNil()) return;

        int		depth = out.depth;

        fill_indent(out, depth);
        if (key instanceof RubyString) {
            dump_str_obj(context, (RubyString) key, out);
            out.append(':');
            dump_val(context, value, depth, out, null);
        } else if (key instanceof RubySymbol) {
            dump_sym_obj(context, (RubySymbol) key, out);
            out.append(':');
            dump_val(context, value, depth, out, null);
        } else {
            int	d2 = depth + 1;
            int	i;
            boolean	started = false;
            int	b;

            out.append('"');
            out.append('^');
            out.append('#');
            out.hash_cnt++;
            for (i = 28; 0 <= i; i -= 4) {
                b = (int)((out.hash_cnt >> i) & 0x0000000F);
                if ('\0' != b) {
                    started = true;
                }
                if (started) {
                    out.append(hex_chars.charAt(b));
                }
            }
            out.append('"');
            out.append(':');
            out.append('[');
            fill_indent(out, d2);
            dump_val(context, key, d2, out, null);
            out.append(',');
            fill_indent(out, d2);
            dump_val(context, value, d2, out, null);
            fill_indent(out, depth);
            out.append(']');
        }
        out.depth = depth;
        out.append(',');
    }

    static void dump_hash(final ThreadContext context, IRubyObject obj, RubyClass clas, int depth, int mode, Out out) {
        int		cnt;

        if (null != clas && !(obj instanceof RubyHash) && ObjectMode == mode) {
            dump_obj_attrs(context, obj, clas, 0, depth, out);
            return;
        }
        RubyHash hash = (RubyHash) obj;
        cnt = hash.size();
        if (0 == cnt) {
            out.append('{');
            out.append('}');
        } else {
            long	id = check_circular(obj, out);

            if (0 > id) {
                return;
            }
            out.append('{');
            if (0 < id) {
                fill_indent(out, depth + 1);
                out.append('"');
                out.append('^');
                out.append('i');
                out.append('"');
                out.append(':');
                dump_ulong(id, out);
                out.append(',');
            }
            out.depth = depth + 1;
            if (ObjectMode == mode) {
                hash.visitAll(context,
                        new RubyHash.VisitorWithState<Out>() {
                            @Override
                            public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Out out) {
                                hash_cb_object(context, key, value, out);
                            }
                        },
                        out);
            } else if (CompatMode == mode) {
                hash.visitAll(context,
                        new RubyHash.VisitorWithState<Out>() {
                            @Override
                            public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Out out) {
                                hash_cb_compat(context, key, value, out);
                            }
                        },
                        out);
            } else {
                hash.visitAll(context,
                        new RubyHash.VisitorWithState<Out>() {
                            @Override
                            public void visit(ThreadContext threadContext, RubyHash rubyHash, IRubyObject key, IRubyObject value, int index, Out out) {
                                hash_cb_strict(context, key, value, out);
                            }
                        },
                        out);
            }
            if (',' == out.get(-1)) {
                out.pop(); // backup to overwrite last comma
            }
            if (null == out.opts.dump_opts) {
                fill_indent(out, depth);
            } else {
                if (0 < out.opts.dump_opts.hash_size) {
                    out.append(out.opts.dump_opts.hash_nl);
                }
                if (0 < out.opts.dump_opts.indent_size) {
                    int	i;

                    for (i = depth; 0 < i; i--) {
                        out.append(out.opts.dump_opts.indent_str);
                    }
                }
            }
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
            RubyTime time;
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

    static void dump_time(ThreadContext context, IRubyObject obj, Out out, boolean withZone) {
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

    static void dump_ruby_time(ThreadContext context, IRubyObject obj, Out out) {
        dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
    }

    static void dump_xml_time(ThreadContext context, IRubyObject obj, Out out) {
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
                dump_cstr(context, buf.toString(), false, false, out);
            } else {
                formatter.format("%04d-%02d-%02dT%02d:%02d:%02d%c%02d:%02d",
                        tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                        tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND),
                        tzsign, tzhour, tzmin);
                dump_cstr(context, buf.toString(), false, false, out);
            }
        } else if (0 == tzsecs && obj.callMethod(context, "utc?").isTrue()) {
            String format = "%04d-%02d-%02dT%02d:%02d:%02d.%09dZ";

            if (9 > out.opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + out.opts.sec_prec);
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec);
            dump_cstr(context, buf.toString(), false, false, out);
        } else {
            String format = "%04d-%02d-%02dT%02d:%02d:%02d.%09d%c%02d:%02d";

            if (9 > out.opts.sec_prec) {
                format = "%04d-%02d-%02dT%02d:%02d:%02d.%0" + (char) ('0' + out.opts.sec_prec) + "d%c%02d:%02d";
            }
            formatter.format(format,
                    tm.get(Calendar.YEAR), tm.get(Calendar.MONTH) + 1, tm.get(Calendar.DAY_OF_MONTH),
                    tm.get(Calendar.HOUR_OF_DAY), tm.get(Calendar.MINUTE), tm.get(Calendar.SECOND), nsec,
                    tzsign, tzhour, tzmin);
            dump_cstr(context, buf.toString(), false, false, out);
        }
    }

    static void dump_data_strict(ThreadContext context, IRubyObject obj, Out out) {
        if (obj instanceof RubyBigDecimal) {
            dump_raw(stringToByteList(context, obj, "to_s"), out);
        } else {
            raise_strict(obj);
        }
    }

    static void dump_data_null(ThreadContext context, IRubyObject obj, Out out) {
        if (obj instanceof RubyBigDecimal) {
            dump_raw(stringToByteList(context, obj, "to_s"), out);
        } else {
            dump_nil(out);
        }
    }

    static void dump_data_comp(ThreadContext context, IRubyObject obj, int depth, Out out) {
        if (obj.respondsTo("to_hash")) {
            IRubyObject h = obj.callMethod(obj.getRuntime().getCurrentContext(), "to_hash");

            if (!(h instanceof RubyHash)) {
                throw context.runtime.newTypeError(obj, context.runtime.getHash());
            }
            dump_hash(context, h, null, depth, out.opts.mode, out);

        } else if (Yes == out.opts.bigdec_as_num && obj instanceof RubyBigDecimal) {
            dump_raw(stringToByteList(context, obj, "to_s"), out);
        } else if (obj.respondsTo("as_json")) {
            IRubyObject aj = obj.callMethod(obj.getRuntime().getCurrentContext(), "as_json");

            // Catch the obvious brain damaged recursive dumping.
            if (aj == obj) {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            } else {
                dump_val(context, aj, depth, out, null);
            }
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(context, obj, "to_json"));
        } else {
            if (obj instanceof RubyTime) {
                switch (out.opts.time_format) {
                    case RubyTime:	dump_ruby_time(context, obj, out);	break;
                    case XmlTime:	dump_xml_time(context, obj, out);	break;
                    case UnixZTime:	dump_time(context, obj, out, true);		break;
                    case UnixTime:
                    default:		dump_time(context, obj, out, false);		break;
                }
            } else if (obj instanceof RubyBigDecimal) {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            } else {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            }
        }
    }

    static void dump_data_obj(ThreadContext context, IRubyObject obj, int depth, Out out) {
        if (obj instanceof org.jruby.RubyTime) {
            out.append('{');
            out.append(T_KEY);
            switch (out.opts.time_format) {
                case RubyTime: // Does not output fractional seconds
                case XmlTime:
                    dump_xml_time(context, obj, out);
                    break;
                case UnixZTime:
                    dump_time(context, obj, out, true);
                    break;
                case UnixTime:
                default:
                    dump_time(context, obj, out, false);
                    break;
            }
            out.append('}');
        } else if (obj instanceof RubyBigDecimal) {
            ByteList str = stringToByteList(context, obj, "to_s");
            if (out.opts.bigdec_as_num != No) {
                dump_raw(str, out);
            } else {
                dump_cstr(context, str, false, false, out);
            }
        } else {
            dump_nil(out);
        }
    }

     // FIXME: both C and Java can crash potentially I added check here but
    // I should see if C oj crashes for these cases.
    static ByteList stringToByteList(ThreadContext context, IRubyObject obj, String method) {
        IRubyObject stringResult = obj.callMethod(context, method);
        
        if (!(stringResult instanceof RubyString)) {
            throw context.runtime.newTypeError("Expected a String");
        }

        return ((RubyString) stringResult).getByteList();
    }

    static void dump_obj_comp(ThreadContext context, IRubyObject obj, int depth, Out out, IRubyObject[] argv) {
        if (obj.respondsTo("to_hash")) {
            IRubyObject h = obj.callMethod(context, "to_hash");

            if (!(h instanceof RubyHash)) {
                throw context.runtime.newTypeError(h.getMetaClass().getName() + ".to_hash() did not return a Hash");
            }
            dump_hash(context, h, null, depth, out.opts.mode, out);
        } else if (obj.respondsTo("as_json")) {
            IRubyObject	aj = obj.callMethod(context, "as_json", argv);

            // Catch the obvious brain damaged recursive dumping.
            if (aj == obj) {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            } else {
                dump_val(context, aj, depth, out, null);
            }
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(context, obj, "to_json"));
        } else {
            if (obj instanceof RubyBigDecimal) {
                ByteList rstr = stringToByteList(context, obj, "to_s");
                if (Yes == out.opts.bigdec_as_num) {
                    dump_raw(rstr, out);
                } else {
                    dump_cstr(context, rstr, false, false, out);
                }
                //FIXME: what if datetime does not exist?
            } else  {
                RubyClass dateTime = context.runtime.getClass("DateTime");
                RubyClass date = context.runtime.getClass("Date");
                if (dateTime != null && dateTime.isInstance(obj) || date != null && date.isInstance(obj) || obj instanceof RubyRational) {
                    dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
                } else {
                    dump_obj_attrs(context, obj, null, 0, depth, out);
                }
            }
        }
    }

     static void dump_obj_obj(ThreadContext context, IRubyObject obj, int depth, Out out) {
        long id = check_circular(obj, out);

        if (0 <= id) {
            if (obj instanceof RubyBigDecimal) {
                ByteList rstr = stringToByteList(context, obj, "to_s");
                dump_raw(rstr, out);
            } else {
                dump_obj_attrs(context, obj, obj.getMetaClass(), id, depth, out);
            }
        }
    }


    static void dump_obj_attrs(ThreadContext context, IRubyObject obj, RubyClass clas, long id, int depth, Out out) {
        int		d2 = depth + 1;

        out.append('{');
        if (null != clas) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(out, d2);
            out.append(O_KEY);
            dump_cstr(context, class_name, false, false, out);
        }
        if (0 < id) {
            out.append(',');
            fill_indent(out, d2);
            out.append(I_KEY);
            dump_ulong(id, out);
        }
        
        if (obj instanceof RubyString) {
            out.append(',');
            fill_indent(out, d2);
            out.append(SELF_KEY);
            dump_cstr(context, ((RubyString) obj).getByteList(), false, false, out);
        } else if (obj instanceof RubyArray) {
            out.append(',');
            fill_indent(out, d2);
            out.append(SELF_KEY);
            dump_array(context, obj, null, depth + 1, out);
        } else if (obj instanceof RubyHash) {
            out.append(',');
            fill_indent(out, d2);
            out.append(SELF_KEY);
            dump_hash(context, obj, null, depth + 1, out.opts.mode, out);
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

            fill_indent(out, d2);

            if (name.charAt(0) == '@') {
                dump_cstr(context, name.substring(1), false, false, out);
            } else {
                dump_cstr(context, "~" + name, false, false, out);
            }
            out.append(':');
            dump_val(context, value, d2, out, null);
        }
        out.depth = depth;
        fill_indent(out, depth);
        out.append('}');
    }

    static void dump_struct_comp(ThreadContext context, IRubyObject obj, int depth, Out out) {
        if (obj.respondsTo("to_hash")) {
            IRubyObject h = obj.callMethod(context, "to_hash");

            if (!(h instanceof RubyHash)) {
                throw context.runtime.newTypeError(obj.getMetaClass().getName() + ".to_hash() did not return a Hash.");
            }
            dump_hash(context, h, null, depth, out.opts.mode, out);
        } else if (obj.respondsTo("as_json")) {
            IRubyObject aj = obj.callMethod(context, "as_json");

            // Catch the obvious brain damaged recursive dumping.
            if (aj == obj) {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            } else {
                dump_val(context, aj, depth, out, null);
            }
        } else if (Yes == out.opts.to_json && obj.respondsTo("to_json")) {
            out.append(stringToByteList(context, obj, "to_json"));
        } else {
            dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
        }
    }

    static void dump_struct_obj(ThreadContext context, RubyStruct obj, int depth, Out out) {
        String class_name = obj.getMetaClass().getName();
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        out.append('{');
        fill_indent(out, d2);
        out.append(U_KEY);
        if (class_name.charAt(0) == '#') {
            RubyArray ma = obj.members();

            int	cnt = ma.size();

            out.append('[');
            for (int i = 0; i < cnt; i++) {
                RubySymbol name = (RubySymbol) ma.eltOk(i); // struct forces all members to be symbols

                if (0 < i) {
                    out.append(',');
                }
                out.append('"');
                out.append(name.asString().getByteList());
                out.append('"');
            }
            out.append(']');
        } else {
            fill_indent(out, d3);
            out.append('"');
            out.append(class_name);
            out.append('"');
        }
        out.append(',');

        boolean first = true;
        for (Object n: obj.members()) {
            IRubyObject name = (IRubyObject) n;

            if (first) {
                first = false;
            } else {
                out.append(',');
            }

            fill_indent(out, d3);
            dump_val(context, obj.aref(name), d3, out, null);
        }

        out.append(']');
        out.append('}');
    }

    static void dump_range_obj(ThreadContext context, RubyRange obj, int depth, Out out) {
        String class_name = obj.getMetaClass().getName();
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        out.append('{');
        fill_indent(out, d2);
        out.append(U_KEY);
        fill_indent(out, d3);
        out.append('"');
        out.append(class_name);
        out.append('"');
        out.append(',');
        dump_val(context, obj.begin(context), d3, out, null);
        out.append(',');
        dump_val(context, obj.end(context), d3, out, null);
        out.append(',');
        dump_val(context, obj.exclude_end_p(), d3, out, null);
        out.append(']');
        out.append('}');
    }


    static void dump_odd(ThreadContext context, IRubyObject obj, Odd odd, RubyClass clas, int depth, Out out) {
        AttrGetFunc fp;
        IRubyObject	v;
        String name;
        int	d2 = depth + 1;

        out.append('{');
        if (null != clas) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(out, d2);
            out.append(BIG_O_KEY);
            dump_cstr(context, class_name, false, false, out);
            out.append(',');
        }

        int index = 0;
        for (; index < odd.attrs.length; index++) {
            fp = odd.attrFuncs[index];
            name = odd.attrs[index];
            if (null != fp) {
                v = fp.execute(context, obj);
            } else if (-1 == name.indexOf('.')) {
                v = obj.callMethod(context, name);
            } else {
                v = obj;
                for (String segment: name.split("\\.")) {
                    v = v.callMethod(context, segment);
                }
            }
            fill_indent(out, d2);
            dump_cstr(context, name, false, false, out);
            out.append(':');
            dump_val(context, v, d2, out, null);
            out.append(',');
        }
        out.pop();
        out.append('}');
    }

    static void raise_strict(IRubyObject obj) {
        throw obj.getRuntime().newTypeError("Failed to dump " + obj.getMetaClass().getName() + " Object to JSON in strict mode.");
    }

    static void dump_val(ThreadContext context, IRubyObject obj, int depth, Out out, IRubyObject[] argv) {
        if (MAX_DEPTH < depth) {
            throw new RaiseException(context.runtime, context.runtime.getNoMemoryError(), "Too deeply nested.", true);
        }

        if (obj instanceof RubyNil) {
            dump_nil(out);
        } else if (obj instanceof RubyBoolean) {
            if (obj == context.runtime.getTrue()) {
                dump_true(out);
            } else {
                dump_false(out);
            }
        } else if (obj instanceof RubyFixnum) {
            dump_fixnum((RubyFixnum) obj, out);
        } else if (obj instanceof RubyFloat) {
            dump_float(context, (RubyFloat) obj, out);
        } else if (obj instanceof RubyModule) {  // Also will be RubyClass
            switch (out.opts.mode) {
                case StrictMode:	raise_strict(obj);		break;
                case NullMode:		dump_nil(out);			break;
                case CompatMode:	dump_class_comp(context, (RubyModule) obj, out);	break;
                case ObjectMode:
                default:		dump_class_obj(context, (RubyModule) obj, out);	break;
            }
        } else if (obj instanceof RubySymbol) {
            switch (out.opts.mode) {
                case StrictMode:
                    raise_strict(obj);
                    break;
                case NullMode:
                    dump_nil(out);
                    break;
                case CompatMode:
                    dump_sym_comp(context, (RubySymbol) obj, out);
                    break;
                case ObjectMode:
                default:
                    dump_sym_obj(context, (RubySymbol) obj, out);
                    break;
            }
        } else if (obj instanceof RubyStruct || obj instanceof RubyRange) { // In MRI T_STRUCT is also Range
            switch (out.opts.mode) {
                case StrictMode:
                    raise_strict(obj);
                    break;
                case NullMode:
                    dump_nil(out);
                    break;
                case CompatMode:
                    dump_struct_comp(context, obj, depth, out);
                    break;
                case ObjectMode:
                default:
                    if (obj instanceof RubyRange) {
                        dump_range_obj(context, (RubyRange) obj, depth, out);
                    } else {
                        dump_struct_obj(context, (RubyStruct) obj, depth, out);
                    }
                    break;
            }
        } else {
            // Most developers have enough sense not to subclass primitive types but
            // since these classes could potentially be subclassed a check for odd
            // classes is performed.
            {
                RubyClass clas = obj.getMetaClass();
                Odd odd;

                if (ObjectMode == out.opts.mode && null != (odd = out.oj.getOdd(clas))) {
                    dump_odd(context, obj, odd, clas, depth + 1, out);
                    return;
                }

                if (obj instanceof RubyBignum) {
                    dump_bignum(context, (RubyBignum) obj, out);
                } else if (obj.getMetaClass() == context.runtime.getString()) {
                    switch (out.opts.mode) {
                        case StrictMode:
                        case NullMode:
                        case CompatMode:
                            dump_str_comp(context, (RubyString) obj, out);
                            break;
                        case ObjectMode:
                        default:
                            dump_str_obj(context, (RubyString) obj, out);
                            break;
                    }
                } else if (obj.getMetaClass() == context.runtime.getArray()) {
                    dump_array(context, obj, clas, depth, out);
                } else if (obj.getMetaClass() == context.runtime.getHash()) {
                    dump_hash(context, obj, clas, depth, out.opts.mode, out);
                } else if (obj instanceof RubyComplex || obj instanceof RubyRegexp) {
                    switch (out.opts.mode) {
                        case StrictMode:	raise_strict(obj);		break;
                        case NullMode:		dump_nil(out);			break;
                        case CompatMode:
                        case ObjectMode:
                        default:		dump_obj_comp(context, obj, depth, out, argv);	break;
                    }
                } else if (obj instanceof RubyTime || obj instanceof RubyBigDecimal) {  // FIXME: not sure it is only these two types.
                    switch (out.opts.mode) {
                        case StrictMode:	dump_data_strict(context, obj, out);	break;
                        case NullMode:		dump_data_null(context, obj, out);	break;
                        case CompatMode:	dump_data_comp(context, obj, depth, out);	break;
                        case ObjectMode:
                        default:		dump_data_obj(context, obj, depth, out);	break;
                    }
                } else {
                    switch (out.opts.mode) {
                        case StrictMode:	dump_data_strict(context, obj, out);	break;
                        case NullMode:		dump_data_null(context, obj, out);	break;
                        case CompatMode:	dump_obj_comp(context, obj, depth, out, argv);	break;
                        case ObjectMode:
                        default:		dump_obj_obj(context, obj, depth, out);	break;
                    }


                    // What type causes this? throw context.runtime.newNotImplementedError("\"Failed to dump '" + obj.getMetaClass().getName() + "'.");
                }
            }
        }
    }

    static void obj_to_json(ThreadContext context, IRubyObject obj, Options copts, Out out) {
        obj_to_json_using_params(context, obj, copts, out, IRubyObject.NULL_ARRAY);
    }

    static void obj_to_json_using_params(ThreadContext context, IRubyObject obj, Options copts, Out out, IRubyObject[] argv) {
        out.circ_cnt = 0;
        out.opts = copts;
        out.hash_cnt = 0;
        if (Yes == copts.circular) {
            out.new_circ_cache();
        }
        out.indent = copts.indent;
        dump_val(context, obj, 0, out, argv);
        if (0 < out.indent) {
            switch (out.peek(0)) {
                case ']':
                case '}':
                    out.append('\n');
                default:
                    break;
            }
        }
        if (Yes == copts.circular) {
            out.delete_circ_cache();
        }
    }

    void oj_write_obj_to_file(ThreadContext context, OjLibrary oj, IRubyObject obj, String path, Options copts) {
        Out out = new Out(oj);
        FileOutputStream f = null;

        out.omit_nil = copts.dump_opts.omit_nil;

        obj_to_json(context, obj, copts, out);

        try {
            f = new FileOutputStream(path);

            out.write(f);
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

    static void oj_write_obj_to_stream(ThreadContext context, OjLibrary oj, IRubyObject obj, IRubyObject stream, Options copts) {
        Out out = new Out(oj);

        out.omit_nil = copts.dump_opts.omit_nil;

        obj_to_json(context, obj, copts, out);

        // Note: Removed Windows path as it called native write on fileno and JRuby does work the same way.
        if (obj instanceof StringIO) {
            ((StringIO) obj).write(context, context.runtime.newString(out.buf));
        } else if (stream.respondsTo("write")) {
            stream.callMethod(context, "write", context.runtime.newString(out.buf));
        } else {
            throw context.runtime.newArgumentError("to_stream() expected an IO Object.");
        }
    }

// dump leaf functions

    static void dump_leaf_str(ThreadContext context, Leaf leaf, Out out) {
        switch (leaf.value_type) {
            case STR_VAL:
                dump_cstr(context, leaf.str, false, false, out);
                break;
            case RUBY_VAL: {
                // FIXME: I think this will always be a string or raise here.
                RubyString value = (RubyString) TypeConverter.checkStringType(context.runtime, leaf.value);

                value = StringSupport.checkEmbeddedNulls(context.runtime, value);

                dump_cstr(context, value.getByteList(), false, false, out);
                break;
            }
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    static void dump_leaf_fixnum(ThreadContext context, Leaf leaf, Out out) {
        switch (leaf.value_type) {
            case STR_VAL:
                out.append(leaf.str);
                break;
            case RUBY_VAL:
                if (leaf.value instanceof RubyBignum) {
                    dump_bignum(context, (RubyBignum) leaf.value, out);
                } else {
                    dump_fixnum((RubyFixnum) leaf.value, out);
                }
                break;
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    static void dump_leaf_float(ThreadContext context, Leaf leaf, Out out) {
        switch (leaf.value_type) {
            case STR_VAL:
                out.append(leaf.str);
                break;
            case RUBY_VAL:
                dump_float(context, (RubyFloat) leaf.value, out);
                break;
            case COL_VAL:
            default:
                throw context.runtime.newTypeError("Unexpected value type " + leaf.value_type + ".");
        }
    }

    static void dump_leaf_array(ThreadContext context, Leaf leaf, int depth, Out out) {
        int	d2 = depth + 1;

        out.append('[');
        if (leaf.hasElements()) {
            boolean first = true;
            for (Leaf element: leaf.elements) {
                if (!first) out.append(',');
                fill_indent(out, d2);
                dump_leaf(context, element, d2, out);
                first = false;
            }
            fill_indent(out, depth);
        }
        out.append(']');
    }

    static void dump_leaf_hash(ThreadContext context, Leaf leaf, int depth, Out out) {
        int	d2 = depth + 1;

        out.append('{');
        if (leaf.hasElements()) {
            boolean first = true;
            for (Leaf element: leaf.elements) {
                if (!first) out.append(',');
                fill_indent(out, d2);
                dump_cstr(context, element.key, false, false, out);
                out.append(':');
                dump_leaf(context, element, d2, out);
                first = false;
            }
            fill_indent(out, depth);
        }
        out.append('}');
    }

    static void dump_leaf(ThreadContext context, Leaf leaf, int depth, Out out) {
        switch (leaf.rtype) {
            case T_NIL:
                dump_nil(out);
                break;
            case T_TRUE:
                dump_true(out);
                break;
            case T_FALSE:
                dump_false(out);
                break;
            case T_STRING:
                dump_leaf_str(context, leaf, out);
                break;
            case T_FIXNUM:
                dump_leaf_fixnum(context, leaf, out);
                break;
            case T_FLOAT:
                dump_leaf_float(context, leaf, out);
                break;
            case T_ARRAY:
                dump_leaf_array(context, leaf, depth, out);
                break;
            case T_HASH:
                dump_leaf_hash(context, leaf, depth, out);
                break;
            default:
                throw context.runtime.newTypeError("Unexpected type " + leaf.rtype);
        }
    }

    public static Out leaf_to_json(ThreadContext context, OjLibrary oj, Leaf leaf, Options copts) {
        Out out = new Out(oj);
        out.circ_cnt = 0;
        out.opts = copts;
        out.hash_cnt = 0;
        out.indent = copts.indent;
        dump_leaf(context, leaf, 0, out);

        return out;
    }

    public static void leaf_to_file(ThreadContext context, OjLibrary oj, Leaf leaf, String path, Options copts) {
        Out out = leaf_to_json(context, oj, leaf, copts);
        FileOutputStream f = null;

        try {
            f = new FileOutputStream(path);

            out.write(f);
        } catch (IOException e) {
            throw context.runtime.newIOErrorFromException(e);
        } finally {
            if (f != null) {
                try { f.close(); } catch (IOException e) {}
            }
        }
    }

// string writer functions

    static void key_check(ThreadContext context, StrWriter sw, ByteList key) {
        DumpType type = sw.peekTypes();

        if (null == key && (ObjectNew == type || ObjectType == type)) {
            throw context.runtime.newStandardError("Can not push onto an Object without a key.");
        }
    }

    static void push_type(StrWriter sw, DumpType type) {
        sw.types.push(type);
    }

    static void maybe_comma(StrWriter sw) {
        DumpType type = sw.peekTypes();
        if (type == ObjectNew) {
            sw.types.set(sw.types.size() - 1, ObjectType);
        } else if (type == ArrayNew) {
            sw.types.set(sw.types.size() - 1, ArrayType);
        } else if (type == ObjectType || type == ArrayType) {
            // Always have a few characters available in the out.buf.
            sw.out.append(',');
        }
    }

    static void push_key(ThreadContext context, StrWriter sw, ByteList key) {
        DumpType type = sw.peekTypes();

        if (sw.keyWritten) {
            throw context.runtime.newStandardError("Can not push more than one key before pushing a non-key.");
        }
        if (ObjectNew != type && ObjectType != type) {
            throw context.runtime.newStandardError("Can only push a key onto an Object.");
        }

        maybe_comma(sw);
        if (!sw.types.empty()) {
            fill_indent(sw.out, sw.types.size());
        }
        dump_cstr(context, key, false, false, sw.out);
        sw.out.append(':');
        sw.keyWritten = true;
    }

    static void push_object(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('{');
        push_type(sw, ObjectNew);
    }

    static void push_array(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('[');
        push_type(sw, ArrayNew);
    }

    static void push_value(ThreadContext context, StrWriter sw, IRubyObject val, ByteList key) {
        dump_key(context, sw, key);
        dump_val(context, val, sw.types.size(), sw.out, null);
    }

    static void push_json(ThreadContext context, StrWriter sw, ByteList json, ByteList key) {
        dump_key(context, sw, key);
        dump_raw(json, sw.out);
    }

    private static void dump_key(ThreadContext context, StrWriter sw, ByteList key) {
        if (sw.keyWritten) {
            sw.keyWritten = false;
        } else {
            key_check(context, sw, key);
            maybe_comma(sw);
            if (!sw.types.empty()) {
                fill_indent(sw.out, sw.types.size());
            }
            if (null != key) {
                dump_cstr(context, key, false, false, sw.out);
                sw.out.append(':');
            }
        }
    }

    static void pop(ThreadContext context, StrWriter sw) {
        if (sw.keyWritten) {
            sw.keyWritten = false;
            throw context.runtime.newStandardError("Can not pop after writing a key but no value.");
        }

        if (sw.types.empty()) {
            throw context.runtime.newStandardError("Can not pop with no open array or object.");
        }

        DumpType type = sw.types.pop();

        fill_indent(sw.out, sw.types.size());
        if (type == ObjectNew || type == ObjectType) {
            sw.out.append('}');
        } else if (type == ArrayNew || type == ArrayType) {
            sw.out.append(']');
        }
        if (sw.types.empty() && 0 <= sw.out.indent) {
            sw.out.append('\n');
        }
    }

    static void pop_all(ThreadContext context, StrWriter sw) {
        while (!sw.types.empty()) {
            pop(context, sw);
        }
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
