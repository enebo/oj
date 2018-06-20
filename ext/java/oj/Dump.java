package oj;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.ext.stringio.RubyStringIO;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;

import static oj.DumpType.ArrayNew;
import static oj.DumpType.ArrayType;
import static oj.DumpType.ObjectNew;
import static oj.DumpType.ObjectType;
import static oj.Options.*;
import static oj.NumInfo.OJ_INFINITY;


/**
 * Created by enebo on 9/9/15.
 */
public class Dump {
    private static final byte[] BIG_O_KEY = {'"', '^', 'O', '"', ':'};
    private static final byte[] C_KEY = {'"', '^', 'c', '"', ':'};
    private static final byte[] O_KEY = {'"', '^', 'o', '"', ':'};
    private static final byte[] I_KEY = {'"', '^', 'i', '"', ':'};
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
            size += newline_friendly_chars[str.get(i)];
        }
        return size - len * (int)'0';
    }

    static int hibit_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += hibit_friendly_chars[str.get(i)];
        }
        return size - len * (int)'0';
    }

    static int ascii_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += ascii_friendly_chars[str.get(i)];
        }
        return size - len * (int)'0';
    }

    static int xss_friendly_size(ByteList str) {
        int len = str.length();
        int	size = 0;

        for (int i = 0; 0 < len; i++, len--) {
            size += xss_friendly_chars[str.get(i)];
        }
        return size - len * (int)'0';
    }

    static void fill_indent(Out out, int cnt) {
        if (0 < out.indent) {
            cnt *= out.indent;
            out.append('\n');
            for (; 0 < cnt; cnt--) {
                out.append(' ');
            }
        }
    }

    static void dump_ulong(long num, Out out) {
        /*
        char	buf[32];
        char	*b = buf + sizeof(buf) - 1;

        *b-- = '\0';
        if (0 < num) {
            for (; 0 < num; num /= 10, b--) {
                *b = (num % 10) + '0';
            }
            b++;
        } else {
            *b = '0';
        }
        for (; '\0' != *b; b++) {
            *out.append(*b);
        }*/
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
            code = str.get(b) & 0x0000000F;
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
    static long
    check_circular(IRubyObject obj, Out out) {
        int	id = 0;
        int	slot;

        if (ObjectMode == out.opts.mode && Yes == out.opts.circular) {
            if (0 == (id = oj_cache8_get(out.circ_cache, obj, slot))) {
                out.circ_cnt++;
                id = out.circ_cnt;
                slot = id;
            } else {
                out.append('"');
                out.append('^');
                out.append('r');
                dump_ulong(id, out);
                out.append('"');

                return -1;
            }
        }
        return (long)id;
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
        int	b = 0;
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
                buf[b--] = '-';
            }
        }
        out.append(buf, b + 1, 32 - b);
    }

    static void dump_bignum(ThreadContext context, RubyBignum obj, Out out) {
        out.append(obj.to_s(context.runtime.newFixnum(10)).convertToString().getByteList());
    }

    // Removed dependencies on math due to problems with CentOS 5.4.
    static void dump_float(ThreadContext context, RubyFloat obj, Out out) {
        double	d = obj.getDoubleValue();

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
                    out.append(NULL_VALUE);
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
            if (is_sym) {
                out.append(':');
            }
            // FIXME: I do not think we will see \0
            for (; '\0' != str.get(str_i); str_i++) {
                out.append(str.get(str_i);
            }
            out.append('"');
        } else {
            int end = str_i + cnt;

            if (is_sym) {
                out.append(':');
            }
            for (; str_i < end; str_i++) {
                switch (cmap[(int)str.get(str_i)]) {
                    case '1':
                        out.append(str.get(str_i));
                        break;
                    case '2':
                        out.append('\\');
                        switch (str.get(str_i)) {
                        case '\\':	out.append('\\');	break;
                        case '\b':	out.append('b');	break;
                        case '\t':	out.append('t');	break;
                        case '\n':	out.append('n');	break;
                        case '\f':	out.append('f');	break;
                        case '\r':	out.append('r');	break;
                        default:	out.append(str.get(str_i));	break;
                    }
                    break;
                    case '3': // Unicode
                        str_i = dump_unicode(context, str, str_i, end, out);
                        break;
                    case '6': // control characters
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

    static void dump_str_obj(ThreadContext context, IRubyObject obj, RubyClass clas, int depth, Out out) {
        if (null != clas && !(obj instanceof RubyString)) {
            dump_obj_attrs(context, obj, clas, 0, depth, out);
        } else {
            ByteList str = ((RubyString) obj).getByteList();
            int s = str.get(0);
            int s1 = str.get(1);
            boolean escape = ':' == s || ('^' == s && ('r' == s1 || 'i' == s1));

            dump_cstr(context, str, false, escape, out);
        }
    }

    static void dump_sym_comp(ThreadContext context, RubySymbol obj, Out out) {
        dump_cstr(context, ((RubyString) obj.to_s()).getByteList(), false, false, out);
    }

    static void dump_sym_obj(ThreadContext context, RubySymbol obj, Out out) {
        dump_cstr(context, ((RubyString) obj.to_s()).getByteList(), true, false, out);
    }

    static void dump_class_comp(ThreadContext context, IRubyObject obj, Out out) {
        dump_cstr(context, new ByteList(obj.getMetaClass().getName().getBytes()), false, false, out);
    }

    static void dump_class_obj(ThreadContext context, IRubyObject obj, Out out) {
        out.append(C_KEY);
        dump_cstr(context, new ByteList(obj.getMetaClass().getName().getBytes()), false, false, out);
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
                            out.append(out.opts.dump_opts.indent);
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
                //printf("*** d2: %u  indent: %u '%s'\n", d2, out.opts.dump_opts.indent_size, out.opts.dump_opts.indent);
                if (0 < out.opts.dump_opts.array_size) {
                    out.append(out.opts.dump_opts.array_nl);
                }
                if (0 < out.opts.dump_opts.indent_size) {
                    for (int j = depth; 0 < j; j--) {
                        out.append(out.opts.dump_opts.indent);
                    }
                }
            }
            out.append(']');
        }
    }

    static int hash_cb_strict(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
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
                    out.append(out.opts.dump_opts.indent);
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

        return ST_CONTINUE;
    }

    static int hash_cb_compat(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
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
                    out.append(out.opts.dump_opts.indent);
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

        return ST_CONTINUE;
    }

    static int hash_cb_object(ThreadContext context, IRubyObject key, IRubyObject value, Out out) {
        int		depth = out.depth;

        fill_indent(out, depth);
        if (key instanceof RubyString) {
            dump_str_obj(context, key, null, depth, out);
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

        return ST_CONTINUE;
    }

    static void dump_hash(ThreadContext context, IRubyObject obj, RubyClass clas, int depth, int mode, Out out) {
        int		cnt;

        if (null != clas && !(obj instanceof RubyHash) && ObjectMode == mode) {
            dump_obj_attrs(context, obj, clas, 0, depth, out);
            return;
        }
        cnt = ((RubyHash) obj).size();
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
                rb_hash_foreach(obj, hash_cb_object, (IRubyObject)out);
            } else if (CompatMode == mode) {
                rb_hash_foreach(obj, hash_cb_compat, (IRubyObject)out);
            } else {
                rb_hash_foreach(obj, hash_cb_strict, (IRubyObject)out);
            }
            if (',' == out.get(-1)) {
                out.seek(-1); // backup to overwrite last comma
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
                        out.append(out.opts.dump_opts.indent);
                    }
                }
            }
            out.append('}');
        }
            }

    static void dump_time(IRubyObject obj, Out out, int withZone) {
        char		buf[64];
        char		*b = buf + sizeof(buf) - 1;
        long		size;
        char		*dot;
        int			neg = 0;
        long		one = 1000000000;
        #if HAS_RB_TIME_TIMESPEC
        struct timespec	ts = rb_time_timespec(obj);
        long 	sec = ts.tv_sec;
        long		nsec = ts.tv_nsec;
        #else
        long 	sec = NUM2LONG(rb_funcall2(obj, oj_tv_sec_id, 0, 0));
        #if HAS_NANO_TIME
        long		nsec = rb_num2ll(rb_funcall2(obj, oj_tv_nsec_id, 0, 0));
        #else
        long		nsec = rb_num2ll(rb_funcall2(obj, oj_tv_usec_id, 0, 0)) * 1000;
        #endif
        #endif

                *b-- = '\0';
        if (withZone) {
            long	tzsecs = NUM2LONG(rb_funcall2(obj, oj_utc_offset_id, 0, 0));
            int	zneg = (0 > tzsecs);

            if (0 == tzsecs && Qtrue == rb_funcall2(obj, oj_utcq_id, 0, 0)) {
                tzsecs = 86400;
            }
            if (zneg) {
                tzsecs = -tzsecs;
            }
            if (0 == tzsecs) {
                *b-- = '0';
            } else {
                for (; 0 < tzsecs; b--, tzsecs /= 10) {
                    *b = '0' + (tzsecs % 10);
                }
                if (zneg) {
                    *b-- = '-';
                }
            }
            *b-- = 'e';
        }
        if (0 > sec) {
            neg = 1;
            sec = -sec;
            if (0 < nsec) {
                nsec = 1000000000 - nsec;
                sec--;
            }
        }
        dot = b - 9;
        if (0 < out.opts.sec_prec) {
            if (9 > out.opts.sec_prec) {
                int	i;

                for (i = 9 - out.opts.sec_prec; 0 < i; i--) {
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
                *b = '0' + (nsec % 10);
            }
            *b-- = '.';
        }
        if (0 == sec) {
            *b-- = '0';
        } else {
            for (; 0 < sec; b--, sec /= 10) {
                *b = '0' + (sec % 10);
            }
        }
        if (neg) {
            *b-- = '-';
        }
        b++;
        size = sizeof(buf) - (b - buf) - 1;
        memcpy(out.cur, b, size);
        out.cur += size;
            }

    static void dump_ruby_time(ThreadContext context, IRubyObject obj, Out out) {
        dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
    }

    static void dump_xml_time(ThreadContext context, IRubyObject obj, Out out) {
        char		buf[64];
        struct tm		*tm;
        long		one = 1000000000;
        #if HAS_RB_TIME_TIMESPEC
        struct timespec	ts = rb_time_timespec(obj);
        long 	sec = ts.tv_sec;
        long		nsec = ts.tv_nsec;
        #else
        long 	sec = NUM2LONG(rb_funcall2(obj, oj_tv_sec_id, 0, 0));
        #if HAS_NANO_TIME
        long		nsec = rb_num2ll(rb_funcall2(obj, oj_tv_nsec_id, 0, 0));
        #else
        long		nsec = rb_num2ll(rb_funcall2(obj, oj_tv_usec_id, 0, 0)) * 1000;
        #endif
        #endif
        long		tzsecs = NUM2LONG(rb_funcall2(obj, oj_utc_offset_id, 0, 0));
        int			tzhour, tzmin;
        char		tzsign = '+';

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
        tm = gmtime(sec);

        if (0 > tzsecs) {
            tzsign = '-';
            tzhour = (int)(tzsecs / -3600);
            tzmin = (int)(tzsecs / -60) - (tzhour * 60);
        } else {
            tzhour = (int)(tzsecs / 3600);
            tzmin = (int)(tzsecs / 60) - (tzhour * 60);
        }

        if (0 == nsec || 0 == out.opts.sec_prec) {
            if (0 == tzsecs && Qtrue == rb_funcall2(obj, oj_utcq_id, 0, 0)) {
                sprintf(buf, "%04d-%02d-%02dT%02d:%02d:%02dZ",
                        tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
                        tm.tm_hour, tm.tm_min, tm.tm_sec);
                dump_cstr(buf, 20, 0, 0, out);
            } else {
                sprintf(buf, "%04d-%02d-%02dT%02d:%02d:%02d%c%02d:%02d",
                        tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
                        tm.tm_hour, tm.tm_min, tm.tm_sec,
                        tzsign, tzhour, tzmin);
                dump_cstr(buf, 25, 0, 0, out);
            }
        } else if (0 == tzsecs && Qtrue == rb_funcall2(obj, oj_utcq_id, 0, 0)) {
            char	format[64] = "%04d-%02d-%02dT%02d:%02d:%02d.%09ldZ";
            int	len = 30;

            if (9 > out.opts.sec_prec) {
                format[32] = '0' + out.opts.sec_prec;
                len -= 9 - out.opts.sec_prec;
            }
            sprintf(buf, format,
                    tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
                    tm.tm_hour, tm.tm_min, tm.tm_sec, nsec);
            dump_cstr(buf, len, 0, 0, out);
        } else {
            char	format[64] = "%04d-%02d-%02dT%02d:%02d:%02d.%09ld%c%02d:%02d";
            int	len = 35;

            if (9 > out.opts.sec_prec) {
                format[32] = '0' + out.opts.sec_prec;
                len -= 9 - out.opts.sec_prec;
            }
            sprintf(buf, format,
                    tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
                    tm.tm_hour, tm.tm_min, tm.tm_sec, nsec,
                    tzsign, tzhour, tzmin);
            dump_cstr(context, buf, false, false, out);
        }
    }

    static void dump_data_strict(ThreadContext context, IRubyObject obj, Out out) {
        IRubyObject	clas = obj.getMetaClass();

        if (clas instanceof RubyBigDecimal) {
            dump_raw(stringToByteList(context, obj, "to_s"), out);
        } else {
            raise_strict(obj);
        }
    }

    static void dump_data_null(ThreadContext context, IRubyObject obj, Out out) {
        IRubyObject	clas = obj.getMetaClass();

        if (clas instanceof RubyBigDecimal) {
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
                    case UnixZTime:	dump_time(obj, out, 1);		break;
                    case UnixTime:
                    default:		dump_time(obj, out, 0);		break;
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
            out.append('"');
            out.append('^');
            out.append('t');
            out.append('"');
            out.append(':');
            switch (out.opts.time_format) {
                case RubyTime: // Does not output fractional seconds
                case XmlTime:
                    dump_xml_time(context, obj, out);
                    break;
                case UnixZTime:
                    dump_time(obj, out, 1);
                    break;
                case UnixTime:
                default:
                    dump_time(obj, out, 0);
                    break;
            }
            out.append('}');
        } else if (obj instanceof RubyBigDecimal) {
            if (Yes == out.opts.bigdec_as_num) {
                dump_raw(stringToByteList(context, obj, "to_s"), out);
            } else {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
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
            } else if (oj_datetime_class == clas || oj_date_class == clas || rb_cRational == clas) {
                dump_cstr(context, stringToByteList(context, obj, "to_s"), false, false, out);
            } else {
                dump_obj_attrs(context, obj, null, 0, depth, out);
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

        // dump instance variables.
            size = d2 * out.indent + 1;
            for (i = cnt; 0 < i; i--, np++) {
                vid = rb_to_id(*np);
                attr = rb_id2name(vid);
                if (first) {
                    first = 0;
                } else {
                    out.append(',');
                }

                fill_indent(out, d2);
                if ('@' == *attr) {
                    attr++;
                    dump_cstr(attr, false, false, out);
                } else {
                    char	buf[32];

                    *buf = '~';
                    strncpy(buf + 1, attr, sizeof(buf) - 2);
                    buf[sizeof(buf) - 1] = '\0';
                    dump_cstr(buf, strlen(attr) + 1, 0, 0, out);
                }
                out.append(':');
                dump_val(context, rb_ivar_get(obj, vid), d2, out, 0, 0);
            }
            out.depth = depth;
        }
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
        int		i;
        int		d2 = depth + 1;
        int		d3 = d2 + 1;

        out.append('{');
        fill_indent(out, d2);
        out.append(U_KEY);
        if (class_name.charAt(0) == '#') {
            RubyArray ma = obj.members();

            int	cnt = ma.size();

            out.append('[');
            for (i = 0; i < cnt; i++) {
                IRubyObject name = ma.eltOk(i);

                if (0 < i) {
                    out.append(',');
                }
                out.append('"');
                out.append(name);
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

        // FIXME: Need values (not sure if members above is key, value or key, or value.
        RubyArray values = obj.members();
        IRubyObject	vp;

            for (i = (int)RSTRUCT_LEN(obj), vp = RSTRUCT_PTR(obj); 0 < i; i--, vp++) {
                fill_indent(out, d3);
                dump_val(context, vp, d3, out, null);
                out.append(',');
            }

        out.seek(-1); // back up on ','
        out.append(']');
        out.append('}');
            }

    static void dump_odd(ThreadContext context, IRubyObject obj, Odd odd, RubyClass clas, int depth, Out out) {
        ID			*idp;
        AttrGetFunc		*fp;
        IRubyObject	v;
        String name;
        int			d2 = depth + 1;

        out.append('{');
        if (null != clas) {
            ByteList class_name = ((RubyString) clas.name()).getByteList();

            fill_indent(out, d2);
            out.append(BIG_O_KEY);
            dump_cstr(context, class_name, false, false, out);
            out.append(',');
        }
        size = d2 * out.indent + 1;
        for (idp = odd.attrs, fp = odd.attrFuncs; null != *idp; idp++, fp++) {
            int	nlen;

            name = rb_id2name(*idp);
            nlen = strlen(name);
            if (null != *fp) {
                v = (*fp)(obj);
            } else if (0 == strchr(name, '.')) {
                v = rb_funcall(obj, *idp, 0);
            } else {
                char	nbuf[256];
                char	*n2 = nbuf;
                char	*n;
                char	*end;
                ID		i;

                if (sizeof(nbuf) <= nlen) {
                    n2 = strdup(name);
                } else {
                    strcpy(n2, name);
                }
                n = n2;
                v = obj;
                while (null != (end = strchr(n, '.'))) {
                    *end = '\0';
                    i = rb_intern(n);
                    v = rb_funcall(v, i, 0);
                    n = end + 1;
                }
                i = rb_intern(n);
                v = rb_funcall(v, i, 0);
            }
            fill_indent(out, d2);
            dump_cstr(context, name, false, false, out);
            out.append(':');
            dump_val(context, v, d2, out, null);
            out.append(',');
        }
        out.seek(-1);
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
                case CompatMode:	dump_class_comp(context, obj, out);	break;
                case ObjectMode:
                default:		dump_class_obj(context, obj, out);	break;
            }
        } else if (obj instanceof RubySymbol) {
            switch (out.opts.mode) {
                case StrictMode:	raise_strict(obj);		break;
                case NullMode:		dump_nil(out);			break;
                case CompatMode:	dump_sym_comp(context, (RubySymbol) obj, out);	break;
                case ObjectMode:
                default:		dump_sym_obj(context, (RubySymbol) obj, out);		break;
            }
        } else if (obj instanceof RubyStruct) {
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
                    dump_struct_obj(context, (RubyStruct) obj, depth, out);
                    break;
            }
        } else {
            // Most developers have enough sense not to subclass primitive types but
            // since these classes could potentially be subclassed a check for odd
            // classes is performed.
            {
                RubyClass clas = obj.getMetaClass();
                Odd		odd;

                if (ObjectMode == out.opts.mode && null != (odd = oj_get_odd(clas))) {
                    dump_odd(context, obj, odd, clas, depth + 1, out);
                    return;
                }
                if (obj instanceof RubyBignum) {
                    dump_bignum(context, (RubyBignum) obj, out);
                } else if (obj instanceof RubyString) {
                    switch (out.opts.mode) {
                        case StrictMode:
                        case NullMode:
                        case CompatMode:
                            dump_str_comp(context, (RubyString) obj, out);
                            break;
                        case ObjectMode:
                        default:
                            dump_str_obj(context, obj, clas, depth, out);
                            break;
                    }
                } else if (obj instanceof RubyArray) {
                    dump_array(context, obj, clas, depth, out);
                } else if (obj instanceof RubyHash) {
                    dump_hash(context, obj, clas, depth, out.opts.mode, out);
                } else if (obj instanceof RubyComplex || obj instanceof RubyRegexp) {
                    switch (out.opts.mode) {
                        case StrictMode:	raise_strict(obj);		break;
                        case NullMode:		dump_nil(out);			break;
                        case CompatMode:
                        case ObjectMode:
                        default:		dump_obj_comp(context, obj, depth, out, argv);	break;
                    }
                } else if (obj.getMetaClass().getNativeTypeIndex() > 0) { // FIXME: Not entirely sure on this
                    switch (out.opts.mode) {
                        case StrictMode:	dump_data_strict(context, obj, out);	break;
                        case NullMode:		dump_data_null(context, obj, out);	break;
                        case CompatMode:	dump_obj_comp(context, obj, depth, out, argv);	break;
                        case ObjectMode:
                        default:		dump_obj_obj(context, obj, depth, out);	break;
                    }
                } else {
                    throw context.runtime.newNotImplementedError("\"Failed to dump '" + obj.getMetaClass().getName() + "'.");
                }
            }
        }
    }

    void
    oj_dump_obj_to_json(ThreadContext context, IRubyObject obj, Options copts, Out out) {
        oj_dump_obj_to_json_using_params(context, obj, copts, out, null);
    }

    void
    oj_dump_obj_to_json_using_params(ThreadContext context, IRubyObject obj, Options copts, Out out, IRubyObject[] argv) {
        out.circ_cnt = 0;
        out.opts = copts;
        out.hash_cnt = 0;
        if (Yes == copts.circular) {
            oj_cache8_new(out.circ_cache);
        }
        out.indent = copts.indent;
        dump_val(context, obj, 0, out, argv);
        if (0 < out.indent) {
            switch (out.peek(-1)) {
                case ']':
                case '}':
                    out.append('\n');
                default:
                    break;
            }
        }
        if (Yes == copts.circular) {
            oj_cache8_delete(out.circ_cache);
        }
    }

    void oj_write_obj_to_file(ThreadContext context, IRubyObject obj, String path, Options copts) {
        Out out = new Out();
        FileOutputStream f = null;

        oj_dump_obj_to_json(obj, copts, out);

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

    void oj_write_obj_to_stream(ThreadContext context, IRubyObject obj, IRubyObject stream, Options copts) {
        Out out = new Out();

        oj_dump_obj_to_json(context, obj, copts, out);

        // Note: Removed Windows path as it called native write on fileno and JRuby does work the same way.
        if (obj instanceof RubyStringIO) {
            ((RubyStringIO) obj).write(context, out.buf);
        } else if (stream.respondsTo("write")) {
            stream.callMethod(context, "write", out.buf);
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
            case RUBY_VAL:
                dump_cstr(context, leaf.value, false, false, out);
                break;
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
        if (leaf.elements.length > 0) {
            fill_indent(out, d2);
            dump_leaf(context, leaf.elements[0], d2, out);
            for (int i = 1; i < leaf.elements.length; i++) {
                fill_indent(out, d2);
                dump_leaf(context, leaf.elements[i], d2, out);
                out.append(',');
            }
            fill_indent(out, depth);

        }
        out.append(']');
    }

    static void dump_leaf_hash(ThreadContext context, Leaf leaf, int depth, Out out) {
        int	d2 = depth + 1;

        out.append('{');
        if (leaf.elements.length > 0) {
            fill_indent(out, d2);
            dump_cstr(leaf.elements[0].key, false, false, out);
            out.append(':');
            dump_leaf(context, leaf.elements[0], d2, out);
            for (int i = 1; i < leaf.elements.length; i++) {
                fill_indent(out, d2);
                dump_cstr(leaf.elements[i].key, false, false, out);
                out.append(':');
                dump_leaf(context, leaf.elements[i], d2, out);
                out.append(',');
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

    Out oj_dump_leaf_to_json(ThreadContext context, Leaf leaf, Options copts) {
        Out out = new Out();
        out.circ_cnt = 0;
        out.opts = copts;
        out.hash_cnt = 0;
        out.indent = copts.indent;
        dump_leaf(context, leaf, 0, out);

        return out;
    }

    void oj_write_leaf_to_file(ThreadContext context, Leaf leaf, String path, Options copts) {
        Out out = oj_dump_leaf_to_json(context, leaf, copts);
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
        DumpType type = sw.types.peek();

        if (null == key && (ObjectNew == type || ObjectType == type)) {
            throw context.runtime.newStandardError("Can not push onto an Object without a key.");
        }
    }

    static void push_type(StrWriter sw, DumpType type) {
        sw.types.push(type);
    }

    static void maybe_comma(StrWriter sw) {
        switch (sw.types.peek()) {
            case ObjectNew:
                sw.types.set(sw.types.size(), ObjectType);
                break;
            case ArrayNew:
                sw.types.set(sw.types.size(), ArrayType);
                break;
            case ObjectType:
            case ArrayType:
                // Always have a few characters available in the out.buf.
                sw.out.append(',');
                break;
        }
    }

    void
    oj_str_writer_push_key(ThreadContext context, StrWriter sw, ByteList key) {
        DumpType type = sw.types.peek();

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

    void oj_str_writer_push_object(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('{');
        push_type(sw, ObjectNew);
    }

    void
    oj_str_writer_push_array(ThreadContext context, StrWriter sw, ByteList key) {
        dump_key(context, sw, key);
        sw.out.append('[');
        push_type(sw, ArrayNew);
    }

    void oj_str_writer_push_value(ThreadContext context, StrWriter sw, IRubyObject val, ByteList key) {
        dump_key(context, sw, key);
        dump_val(context, val, sw.types.size(), sw.out, null);
    }

    void oj_str_writer_push_json(ThreadContext context, StrWriter sw, ByteList json, ByteList key) {
        dump_key(context, sw, key);
        dump_raw(json, sw.out);
    }

    private void dump_key(ThreadContext context, StrWriter sw, ByteList key) {
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

    void
    oj_str_writer_pop(ThreadContext context, StrWriter sw) {
        DumpType type = sw.types.pop();

        if (sw.keyWritten) {
            sw.keyWritten = false;
            throw context.runtime.newStandardError("Can not pop after writing a key but no value.");
        }

        if (sw.types.empty()) {
            throw context.runtime.newStandardError("Can not pop with no open array or object.");
        }

        fill_indent(sw.out, sw.types.size());
        switch (type) {
            case ObjectNew:
            case ObjectType:
                sw.out.append('}');
                break;
            case ArrayNew:
            case ArrayType:
                sw.out.append(']');
                break;
        }
        if (sw.types.empty() && 0 <= sw.out.indent) {
            sw.out.append('\n');
        }
    }

    void oj_str_writer_pop_all(ThreadContext context, StrWriter sw) {
        while (!sw.types.empty()) {
            oj_str_writer_pop(context, sw);
        }
    }

}
