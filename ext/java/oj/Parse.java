package oj;

import java.io.InputStream;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.NextItem.*;
import static oj.Options.*;

public class Parse {
    static final boolean HAS_PROC_WITH_BLOCK = false;
    static final boolean IS_WINDOWS = false;
    static final int DEC_MAX = 15;
    static final int EXP_MAX = 100000;

    static void non_white(ParseInfo pi) {
        while (true) {
            switch(pi.advance(1)) {
            case ' ':
            case '\t':
            case '\f':
            case '\n':
            case '\r':
                break;
            default:
                return;
            }
        }
    }

    static void skip_comment(ParseInfo pi) {
        if ('*' == pi.current()) {
            pi.advance(1);
            for (; pi.offset() < pi.length(); pi.advance(1)) {
                if ('*' == pi.current() && '/' == pi.current(1)) {
                    pi.advance(2);
                    return;
                } else if (pi.length() <= pi.offset()) {
                    pi.setError("comment not terminated");
                    return;
                }
            }
        } else if ('/' == pi.current()) {
            for (; true; pi.advance(1)) {
                switch (pi.current()) {
                case '\n':
                case '\r':
                case '\f':
                case '\0':
                    return;
                default:
                    break;
                }
            }
        } else {
            pi.setError("invalid comment format");
        }
    }

    static void add_value(ParseInfo pi, Object rval) {
        Val parent = pi.stack_peek();

        if (parent == null) { // simple add
            pi.add_value(rval);
        } else {
            switch (parent.next) {
            case ARRAY_NEW:
            case ARRAY_ELEMENT:
                pi.array_append_value(rval);
                parent.next = ARRAY_COMMA;
                break;
                case HASH_VALUE:
                    pi.hash_set_value(parent, rval);
                    // FIXME: key is offset in contiguous pointer.  cur < key must be recorded another way
                    if (parent.key != null && 0 < parent.key.getRealSize() && (parent.key < pi.json || pi.cur < parent.key)) {
                        parent.key = null;
                    }
                    parent.next = HASH_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    pi.setError("expected " + parent.next);
                    break;
            }
        }
    }

    static void read_null(ParseInfo pi) {
        if ('u' == pi.advance(1) && 'l' == pi.advance(1) && 'l' == pi.advance(1)) {
            pi.add_value(pi.nilValue());
        } else {
            pi.setError("expected null");
        }
    }
    
    static void read_true(ParseInfo pi) {
        if ('r' == pi.advance(1) && 'u' == pi.advance(1) && 'e' == pi.advance(1)) {
            pi.add_value(pi.trueValue());
        } else {
            pi.setError("expected true");
        }
    }

    static void read_false(ParseInfo pi) {
        if ('a' == pi.advance(1) && 'l' == pi.advance(1) && 's' == pi.advance(1) && 'e' == pi.advance(1)) {
            pi.add_value(pi.falseValue());
        } else {
            pi.setError("expected false");
        }
    }

    // FIXME: Uses String.  Should get swapped to bytelist eventually or byte[]
    static int read_hex(ParseInfo pi, String str) {
        int	b = 0;

        for (int i = 0; i < 4; i++) {
            char h = str.charAt(i);
            b = b << 4;
            if ('0' <= h && h <= '9') {
                b += h - '0';
            } else if ('A' <= h && h <= 'F') {
                b += h - 'A' + 10;
            } else if ('a' <= h && h <= 'f') {
                b += h - 'a' + 10;
            } else {
                pi.setError("invalid hex character");
                return 0;
            }
        }
        return b;
    }

    static void unicode_to_chars(ParseInfo pi, ByteList buf, int code) {
        if (0x0000007F >= code) {
            buf.append((char) code);
        } else if (0x000007FF >= code) {
            buf.append(0xC0 | (code >> 6));
            buf.append(0x80 | (0x3F & code));
        } else if (0x0000FFFF >= code) {
            buf.append(0xE0 | (code >> 12));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x001FFFFF >= code) {
            buf.append(0xF0 | (code >> 18));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x03FFFFFF >= code) {
            buf.append(0xF8 | (code >> 24));
            buf.append(0x80 | ((code >> 18) & 0x3F));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else if (0x7FFFFFFF >= code) {
            buf.append(0xFC | (code >> 30));
            buf.append(0x80 | ((code >> 24) & 0x3F));
            buf.append(0x80 | ((code >> 18) & 0x3F));
            buf.append(0x80 | ((code >> 12) & 0x3F));
            buf.append(0x80 | ((code >> 6) & 0x3F));
            buf.append(0x80 | (0x3F & code));
        } else {
            pi.setError("invalid Unicode character");
        }
    }

// entered at /
    static void read_escaped_str(ParseInfo pi, String start) {
        ByteList buf = new ByteList();
        int	cnt = pi.offset();  // was cur - begin
        int	code;
        Val parent = pi.stack_peek();

        if (0 < cnt) {
            pi.appendTo(buf);
        }
        for (char s = pi.current(); '"' != s; s = pi.advance(1)) {
            if (pi.offset() >= pi.length()) {
                pi.setError("quoted string not terminated");
                return;
            } else if ('\\' == s) {
                pi.advance(1);
                switch (s) {
                case 'n':	buf.append('\n');	break;
                case 'r':	buf.append('\r');	break;
                case 't':	buf.append('\t');	break;
                case 'f':	buf.append('\f');	break;
                case 'b':	buf.append('\b');	break;
                case '"':	buf.append('"');	break;
                case '/':	buf.append('/');	break;
                case '\\':	buf.append('\\');	break;
                case 'u':
                    pi.advance(1);
                    if (0 == (code = read_hex(pi)) && pi.err_has()) {
                        return;
                    }
                    pi.advance(3);
                    if (0x0000D800 <= code && code <= 0x0000DFFF) {
                        int	c1 = (code - 0x0000D800) & 0x000003FF;
                        int	c2;

                        pi.advance(1);
                        if ('\\' != s || 'u' != pi.current(1)) {
                            pi.cur = s;
                            pi.setError("invalid escaped character");
                            return;
                        }
                        pi.advance(2);
                        if (0 == (c2 = read_hex(pi)) && pi.err_has()) {
                            return;
                        }
                        pi.advance(3);
                        c2 = (c2 - 0x0000DC00) & 0x000003FF;
                        code = ((c1 << 10) | c2) + 0x00010000;
                    }
                    unicode_to_chars(pi, buf, code);
                    if (pi.err_has()) {
                        return;
                    }
                    break;
                    default:
                        pi.cur = s;
                        pi.setError("invalid escaped character");
                        return;
                }
            } else {
                buf.append(s);
            }
        }
        if (null == parent) {
            pi.add_cstr(pi, buf, start);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append_cstr(pi, buf, start);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (pi.undefValue() == (parent.key_val = pi.hash_key(pi, buf, buf.getRealSize())))) {
                    parent.key = buf.dup();
                } else {
                    parent.key = new ByteList();
                }
                parent.k1 = *start;
                parent.next =  HASH_COLON;
                break;
                case HASH_VALUE:
                    pi.hash_set_cstr(pi, parent, buf, start);
                    if (null != parent.key && 0 < parent.key.getRealSize() && (parent.key < pi.json || pi.cur < parent.key)) {
                        parent.key = null;
                    }
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    pi.setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
        pi.cur = s + 1;
    }

    static void read_str(ParseInfo pi) {
        char	*str = pi.cur;
        Val parent = pi.stack_peek();

        for (; '"' != pi.current(); pi.advance(1)) {
            if (pi.length() <= pi.offset()) {
                pi.setError("quoted string not terminated");
                return;
            } else if ('\0' == pi.current()) {
                pi.setError("NULL byte in string");
                return;
            } else if ('\\' == pi.current()) {
                read_escaped_str(pi, str);
                return;
            }
        }
        if (null == parent) { // simple add
            pi.add_cstr(pi, str, pi.cur - str, str);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append_cstr(pi, str, pi.cur - str, str);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (pi.undefValue() == (parent.key_val = pi.hash_key(pi, str, pi.cur - str))) {
                        parent.key = str;
                        parent.klen = pi.cur - str;
                    } else {
                        parent.key = "";
                        parent.klen = 0;
                    }
                    parent.k1 = str;
                    parent.next =  HASH_COLON;
                    break;
                case HASH_VALUE:
                    pi.hash_set_cstr(pi, parent, str, pi.cur - str, str);
                    if (null != parent.key && 0 < parent.klen && (parent.key < pi.json || pi.cur < parent.key)) {
                        parent.key = null;
                    }
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    pi.setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
        pi.advance(1); // move past "
    }

    static void read_num(ParseInfo pi) {
        NumInfo	ni;
        Val	parent = pi.stack_peek();
        int zero_cnt = 0;

        ni.str = pi.cur;
        ni.i = 0;
        ni.num = 0;
        ni.div = 1;
        ni.len = 0;
        ni.exp = 0;
        ni.dec_cnt = 0;
        ni.big = false;
        ni.infinity = false;
        ni.nan = false;
        ni.neg = false;
        ni.hasExp = false;
        ni.no_big = (FloatDec == pi.options.bigdec_load);

        if ('-' == pi.current()) {
            pi.advance(1);
            ni.neg = true;
        } else if ('+' == pi.current()) {
            pi.advance(1);
        }
        if ('I' == pi.current()) {
            if (0 != strncmp("Infinity", pi.cur, 8)) {
                pi.setError("not a number or other Object");
                return;
            }
            pi.advance(8);
            ni.infinity = true;
        } else if ('N' == pi.current() || 'n' == pi.current()) {
            if ('a' != pi.current(1) || ('N' != pi.current(2) && 'n' != pi.current(2))) {
                pi.setError("not a number or other Object");
                return;
            }
            pi.advance(3);
            ni.nan = true;
        } else {
            for (; '0' <= pi.current() && pi.current() <= '9'; pi.advance(1)) {
                ni.dec_cnt++;
                if (ni.big) {
                    //ni.big++; // What is this for?
                } else {
                    int	d = (pi.current() - '0');

                    if (0 == d) {
                        zero_cnt++;
                    } else {
                        zero_cnt = 0;
                    }
                    // TBD move size check here
                    ni.i = ni.i * 10 + d;
                    if (Long.MAX_VALUE <= ni.i || DEC_MAX < ni.dec_cnt - zero_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('.' == pi.current()) {
                pi.advance(1);
                for (; '0' <= pi.current() && pi.current() <= '9'; pi.advance(1)) {
                    int	d = (pi.current() - '0');

                    if (0 == d) {
                        zero_cnt++;
                    } else {
                        zero_cnt = 0;
                    }
                    ni.dec_cnt++;
                    // TBD move size check here
                    ni.num = ni.num * 10 + d;
                    ni.div *= 10;
                    if (Long.MAX_VALUE <= ni.div || DEC_MAX < ni.dec_cnt - zero_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('e' == pi.current() || 'E' == pi.current()) {
                boolean	eneg = false;

                ni.hasExp = true;
                pi.advance(1);
                if ('-' == pi.current()) {
                    pi.advance(1);
                    eneg = true;
                } else if ('+' == pi.current()) {
                    pi.advance(1);
                }
                for (; '0' <= pi.current() && pi.current() <= '9'; pi.advance(1)) {
                    ni.exp = ni.exp * 10 + (pi.current() - '0');
                    if (EXP_MAX <= ni.exp) {
                        ni.big = true;
                    }
                }
                if (eneg) {
                    ni.exp = -ni.exp;
                }
            }
            ni.dec_cnt -= zero_cnt;
            ni.len = pi.cur - ni.str;
        }
        if (BigDec == pi.options.bigdec_load) {
            ni.big = true;
        }
        if (null == parent) {
            add_num(pi, ni);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append_num(pi, ni);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_VALUE:
                    pi.hash_set_num(pi, parent, ni);
                    if (null != parent.key && 0 < parent.key.getRealSize() && (parent.key < pi.json || pi.cur < parent.key)) {
                        parent.key = null;
                    }
                    parent.next =  HASH_COMMA;
                    break;
                default:
                    pi.setError("expected " + parent.next);
                    break;
            }
        }
    }

    static void array_start(ParseInfo pi) {
        pi.stack.push(pi.start_array(ARRAY_NEW));
    }

    static void
    array_end(ParseInfo pi) {
        Val	array = pi.stack.pop();

        if (null == array) {
            pi.setError("unexpected array close");
        } else if (ARRAY_COMMA != array.next && ARRAY_NEW != array.next) {
            pi.setError("expected " + array.next + ", not an array close");
        } else {
            pi.end_array();
            add_value(pi, array.val);
        }
    }

    static void
    hash_start(ParseInfo pi) {
        pi.stack.push(pi.start_hash(HASH_NEW));
    }

    static void
    hash_end(ParseInfo pi) {
        Val	hash = pi.stack.peek();

        // leave hash on stack until just before
        if (null == hash) {
            pi.setError("unexpected hash close");
        } else if (HASH_COMMA != hash.next && HASH_NEW != hash.next) {
            pi.setError("expected " + hash.next + ", not a hash close");
        } else {
            pi.end_hash();
            pi.stack.pop();
            add_value(pi, hash.val);
        }
    }

    static void comma(ParseInfo pi) {
        Val	parent = pi.stack.peek();

        if (null == parent) {
            pi.setError("unexpected comma");
        } else if (ARRAY_COMMA == parent.next) {
            parent.next =  ARRAY_ELEMENT;
        } else if (HASH_COMMA == parent.next) {
            parent.next =  HASH_KEY;
        } else {
            pi.setError("unexpected comma");
        }
    }

    static void colon(ParseInfo pi) {
        Val	parent = pi.stack.peek();

        if (null != parent && HASH_COLON == parent.next) {
            parent.next =  HASH_VALUE;
        } else {
            pi.setError("unexpected colon");
        }
    }

    static void oj_parse2(ParseInfo pi) {
        boolean first = true;

        pi.cur = pi.json;
        pi.err_init();
        while (true) {
        non_white(pi);
        if (!first && '\0' != pi.current()) {
            pi.setError("unexpected characters after the JSON document");
        }
        switch (pi.advance(1)) {
            case '{':
                hash_start(pi);
                break;
            case '}':
                hash_end(pi);
                break;
            case ':':
                colon(pi);
                break;
            case '[':
                array_start(pi);
                break;
            case ']':
                array_end(pi);
                break;
            case ',':
                comma(pi);
                break;
            case '"':
                read_str(pi);
                break;
            case '+':
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'I':
            case 'N':
                pi.advance(-1);
                read_num(pi);
                break;
            case 't':
                read_true(pi);
                break;
            case 'f':
                read_false(pi);
                break;
            case 'n':
                if ('u' == pi.current()) {
                    read_null(pi);
                } else {
                    pi.advance(-1);
                    read_num(pi);
                }
                break;
            case '/':
                skip_comment(pi);
                break;
            case '\0':
                pi.advance(-1);
                return;
            default:
                pi.setError("unexpected character");
                return;
        }
        if (pi.err_has()) {
            return;
        }
        if (pi.stack.isEmpty()) {
            if (pi.undefValue() != pi.proc) {
                if (pi.nilValue() == pi.proc) {
                    yield(pi.stack.firstElement());
                } else {
                    if (HAS_PROC_WITH_BLOCK) {
                        Object[] args= new Object[] { pi.stack.firstElement() };
                        pi.proc.call(args);
                    } else {
                        throw pi.getRuntime().newNotImplementedError("Calling a Proc with a block not supported in this version. Use func() {|x| } syntax instead.");
                    }
                }
            } else {
                first = false;
            }
        }
    }
}

Object  oj_num_as_value(ParseInfo pi, NumInfo ni) {
    IRubyObject rnum = pi.nilValue();

    if (ni.infinity) {
	if (ni.neg) {
	    rnum = pi.newFloat(-OJ_INFINITY);
	} else {
	    rnum = pi.newFloat(OJ_INFINITY);
	}
    } else if (ni.nan) {
	rnum = pi.newFloat(0.0/0.0);
    } else if (1 == ni.div && 0 == ni.exp) { // fixnum
	if (ni.big) {
	    if (256 > ni.len) {
		char	buf[256];

		memcpy(buf, ni.str, ni.len);
		buf[ni.len] = '\0';
		rnum = rb_cstr_to_inum(buf, 10, 0);
	    } else {
		char	*buf = ALLOC_N(char, ni.len + 1);

		memcpy(buf, ni.str, ni.len);
		buf[ni.len] = '\0';
		rnum = rb_cstr_to_inum(buf, 10, 0);
	    }
	} else {
	    if (ni.neg) {
		rnum = rb_ll2inum(-ni.i);
	    } else {
		rnum = rb_ll2inum(ni.i);
	    }
	}
    } else { // decimal
	if (ni.big) {
        rnum = pi.newBigDecimal(pi.newString(ni.str));
	    if (ni.no_big) {
            rnum = rnum.callMethod(pi.getRuntime().getCurrentContext(), "to_f");
	    }
	} else {
	    double	d = (double)ni.i + (double)ni.num * (1.0 / ni.div);

	    if (ni.neg) {
            d = -d;
	    }
	    if (0 != ni.exp) {
            d *= Math.pow(10.0, ni.exp);
	    }
	    rnum = pi.newFloat(d);
	}
    }
    return rnum;
}

    static void oj_set_error_at(ParseInfo pi, Object err_clas, String file, int line, String format, String... data) {
        if (null == pi.json) {
            oj_err_set(pi.err, err_clas, "%s at line %d, column %d [%s:%d]", msg, pi.rd.line, pi.rd.col, file, line);
        } else {
            _oj_err_set_with_location(pi.err, err_clas, msg, pi.json, pi.cur - 1, file, line);
        }
}

    static Object protect_parse(Object pip) {
        oj_parse2((ParseInfo) pip);

        return ((ParseInfo) pip).nilValue();
    }

    void oj_pi_set_input_str(ParseInfo pi, InputStream input) {
        pi.json = input;
        pi.end = pi.json + RSTRING_LEN(input);
    }

    static Object oj_pi_parse(Object[] argv, ParseInfo pi, String json, int len, int yieldOk) {
        StringBuilder buf = null;
        Object	input;
        Object	wrapped_stack;
        Object		result = pi.nilValue();
        int			line = 0;
        int			free_json = 0;

        if (argv.length < 1) {
            throw pi.getRuntime().newArgumentError("Wrong number of arguments to parse.");
        }
        input = argv[0];
        if (2 == argv.length) {
            oj_parse_options(argv[1], pi.options);
        }
        if (yieldOk && rb_block_given_p()) {
            pi.proc = pi.nilValue();
        } else {
            pi.proc = pi.undefValue();
        }
        if (null != json) {
            pi.json = json;
            pi.end = json + len;
            free_json = 1;
        } else if (input instanceof RubyString) {
            oj_pi_set_input_str(pi, input);
        } else if (pi.nilValue() == input && Yes == pi.options.nilnil) {
            return pi.nilValue();
        } else {
            Object clas = rb_obj_class(input);
            Object	s;

            if (oj_stringio_class == clas) {
                s = rb_funcall2(input, oj_string_id, 0, 0);
                oj_pi_set_input_str(pi, s);
            } else if (!IS_WINDOWS && rb_cFile == clas && 0 == FIX2INT(rb_funcall(input, oj_pos_id, 0))) {
                int	fd = FIX2INT(rb_funcall(input, oj_fileno_id, 0));
                int cnt;
                int len = lseek(fd, 0, SEEK_END);

                lseek(fd, 0, SEEK_SET);
                buf = ALLOC_N(char, len + 1);
                pi.json = buf;
                pi.end = buf + len;
                if (0 >= (cnt = read(fd, (char*)pi.json, len)) || cnt != len) {
                    throw pi.getRuntime().newIOError("failed to read from IO Object.");
                }
                ((char*)pi.json)[len] = '\0';
                /* skip UTF-8 BOM if present */
                if (0xEF == (uint8_t)*pi.json && 0xBB == (uint8_t)pi.json[1] && 0xBF == (uint8_t)pi.json[2]) {
                    pi.json += 3;
                }
            } else if (rb_respond_to(input, oj_read_id)) {
                // use stream parser instead
                return oj_pi_sparse(argc, argv, pi, 0);
            } else {
                throw pi.getRuntime().newArgumentError("strict_parse() expected a String or IO Object.");
            }
        }
        if (Yes == pi.options.circular) {
            pi.circ_array = oj_circ_array_new();
        } else {
            pi.circ_array = null;
        }

        result = pi.stack.firstElement();
        if (!pi.err_has()) {
            // If the stack is not empty then the JSON terminated early.
            Val	v;

            if (null != (v = pi.stack.peek())) {
                switch (v.next) {
                    case ARRAY_NEW:
                    case ARRAY_ELEMENT:
                    case ARRAY_COMMA:
                        pi.setError("Array not terminated");
                        break;
                    case HASH_NEW:
                    case HASH_KEY:
                    case HASH_COLON:
                    case HASH_VALUE:
                    case HASH_COMMA:
                        pi.setError("Hash/Object not terminated");
                        break;
                    default:
                        pi.setError("not terminated");
                }
            }
        }
        // proceed with cleanup
        if (0 != line) {
            rb_jump_tag(line);
        }
        if (pi.err_has()) {
            oj_err_raise(pi.err);
        }
        if (pi.options.quirks_mode == No) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                throw pi.error("unexpected non-document Object");
            }
        }
        return result;
    }
}
