package oj;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFile;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;

import static oj.NextItem.*;
import static oj.Options.*;

public class Parse {
//    static final boolean HAS_PROC_WITH_BLOCK = false;
    static final int DEC_MAX = 15;
    static final int EXP_MAX = 100000;
    static final double OJ_INFINITY = Double.POSITIVE_INFINITY; // This was div by 0.0 in oj C src
    static final ByteList INFINITY = new ByteList(new byte[] { 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'});

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

    static void add_value(ParseInfo pi, IRubyObject rval) {
        Val parent = pi.stack_peek();

        if (parent == null) { // simple add
            pi.add.addValue(pi, rval);
        } else {
            switch (parent.next) {
            case ARRAY_NEW:
            case ARRAY_ELEMENT:
                pi.array_append.appendValue(pi, rval);
                parent.next = ARRAY_COMMA;
                break;
                case HASH_VALUE:
                    pi.hash_set.setValue(pi, parent, rval);
                    // FIXME: key is offset in contiguous pointer.  cur < key must be recorded another way
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
            pi.add.addValue(pi, pi.nilValue());
        } else {
            pi.setError("expected null");
        }
    }
    
    static void read_true(ParseInfo pi) {
        if ('r' == pi.advance(1) && 'u' == pi.advance(1) && 'e' == pi.advance(1)) {
            pi.add.addValue(pi, pi.trueValue());
        } else {
            pi.setError("expected true");
        }
    }

    static void read_false(ParseInfo pi) {
        if ('a' == pi.advance(1) && 'l' == pi.advance(1) && 's' == pi.advance(1) && 'e' == pi.advance(1)) {
            pi.add.addValue(pi, pi.falseValue());
        } else {
            pi.setError("expected false");
        }
    }

    // FIXME: Uses String.  Should get swapped to bytelist eventually or byte[]
    static int read_hex(ParseInfo pi) {
        int	b = 0;

        for (int i = 0; i < 4; i++) {
            int h = pi.current(i);
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
        // FIXME: I think this will not work as written with signed ints (we should use jcodings
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
    static void read_escaped_str(ParseInfo pi, int start) {
        ByteList buf = new ByteList();
        int	cnt = pi.offset();  // was cur - start
        int	code;
        Val parent = pi.stack_peek();

        if (0 < cnt) {
            pi.appendTo(buf);
        }

        for (int s = pi.current(); '"' != s; s = pi.advance(1)) {
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
                            pi.advance(-1);
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
                        pi.advance(-1);
                        pi.setError("invalid escaped character");
                        return;
                }
            } else {
                buf.append(s);
            }
        }

        if (null == parent) {
            pi.add.addCStr(pi, buf);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append.appendCStr(pi, buf);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (pi.undefValue() == (parent.key_val = pi.hash_key.call(pi, pi.getRuntime().newString(buf)))) {
                    parent.key = buf.dup();
                } else {
                    parent.key = new ByteList();
                }
                parent.k1 = start;
                parent.next =  HASH_COLON;
                break;
                case HASH_VALUE:
                    pi.hash_set.setCStr(pi, parent, buf);
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
        // Do I advance or is it advanced at this potin
        //pi.cur = s + 1;
    }

    static void read_str(ParseInfo pi) {
        int str = pi.offset();
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
            System.out.println("HERE: " + pi.getRuntime().newString(pi.subStr(str, pi.offset() - str)));
            pi.add.addCStr(pi, pi.subStr(str, pi.offset() - str));
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append.appendCStr(pi, pi.subStr(str, pi.offset() - str));
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (pi.undefValue() == (parent.key_val = pi.hash_key.call(pi, pi.getRuntime().newString(pi.subStr(str, pi.offset() - str))))) {
                        parent.key = pi.subStr(str, pi.offset() - str);
                    } else {
                        parent.key = new ByteList(new byte[] {});
                    }
                    parent.k1 = str;
                    parent.next =  HASH_COLON;
                    break;
                case HASH_VALUE:
                    pi.hash_set.setCStr(pi, parent, pi.subStr(str, pi.offset() - str));
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
        NumInfo	ni = new NumInfo();
        Val	parent = pi.stack_peek();
        int zero_cnt = 0;
        int start = pi.offset();

        ni.no_big = (FloatDec == pi.options.bigdec_load);

        if ('-' == pi.current()) {
            pi.advance(1);
            ni.neg = true;
        } else if ('+' == pi.current()) {
            pi.advance(1);
        }
        if ('I' == pi.current()) {
            if (!pi.startsWith(INFINITY)) {
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
            ni.str = pi.subStr(start, pi.offset() - start);
        }
        if (BigDec == pi.options.bigdec_load) {
            ni.big = true;
        }
        if (null == parent) {
            pi.add.addNum(pi, ni);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    pi.array_append.appendNum(pi, ni);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_VALUE:
                    pi.hash_set.setNum(pi, parent, ni);
                    parent.next =  HASH_COMMA;
                    break;
                default:
                    pi.setError("expected " + parent.next);
                    break;
            }
        }
    }

    static void array_start(ParseInfo pi) {
        pi.stack.push(new Val(pi.start_array.call(pi), ARRAY_NEW));
    }

    static void array_end(ParseInfo pi) {
        Val	array = pi.stack.pop();

        if (null == array) {
            pi.setError("unexpected array close");
        } else if (ARRAY_COMMA != array.next && ARRAY_NEW != array.next) {
            pi.setError("expected " + array.next + ", not an array close");
        } else {
            pi.end_array.call(pi);
            add_value(pi, array.val);
        }
    }

    static void hash_start(ParseInfo pi) {
        pi.stack.push(new Val(pi.start_hash.call(pi), HASH_NEW));
    }

    static void hash_end(ParseInfo pi) {
        Val	hash = pi.stack_peek();

        // leave hash on stack until just before
        if (null == hash) {
            pi.setError("unexpected hash close");
        } else if (HASH_COMMA != hash.next && HASH_NEW != hash.next) {
            pi.setError("expected " + hash.next + ", not a hash close");
        } else {
            pi.end_hash.call(pi);
            pi.stack.pop();
            add_value(pi, hash.val);
        }
    }

    static void comma(ParseInfo pi) {
        Val	parent = pi.stack_peek();

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
        Val	parent = pi.stack_peek();

        if (null != parent && HASH_COLON == parent.next) {
            parent.next =  HASH_VALUE;
        } else {
            pi.setError("unexpected colon");
        }
    }

    static void oj_parse2(ParseInfo pi) {
        boolean first = true;

        pi.err_init();
        while (true) {
            non_white(pi);
            if (!first && '\0' != pi.current()) {
                pi.setError("unexpected characters after the JSON document");
            }

            int c = pi.current();
            pi.advance(1);
            switch (c) {
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
                if (pi.proc.isGiven()) {
                    pi.proc.yield(pi.getContext(), pi.stack.firstElement().val);
/*                } else {
                    if (HAS_PROC_WITH_BLOCK) {
                        Object[] args= new Object[] { pi.stack.firstElement() };
                        pi.proc.call(args);
                    } else {
                        throw pi.getRuntime().newNotImplementedError("Calling a Proc with a block not supported in this version. Use func() {|x| } syntax instead.");
                    }
                }*/
                } else {
                    first = false;
                }
            }
        }
    }

IRubyObject oj_num_as_value(ParseInfo pi, NumInfo ni) {
    IRubyObject rnum;

    if (ni.infinity) {
	if (ni.neg) {
	    rnum = pi.newFloat(-OJ_INFINITY);
	} else {
	    rnum = pi.newFloat(OJ_INFINITY);
	}
    } else if (ni.nan) {
        rnum = pi.newFloat(Double.NaN);
    } else if (1 == ni.div && 0 == ni.exp) { // fixnum
        if (ni.big) {
            if (256 > ni.len) {
                rnum = ConvertBytes.byteListToInum19(pi.getRuntime(), ni.str, 10, true);
            } else {
                rnum = ConvertBytes.byteListToInum19(pi.getRuntime(), ni.str, 10, true);
            }
        } else {
            if (ni.neg) {
                rnum = RubyBignum.bignorm(pi.getRuntime(), RubyBignum.long2big(-ni.i));
            } else {
                rnum = RubyBignum.bignorm(pi.getRuntime(), RubyBignum.long2big(ni.i));
            }
        }
    } else { // decimal
	if (ni.big) {
        rnum = pi.newBigDecimal(pi.getRuntime().newString(ni.str));
	    if (ni.no_big) {
            rnum = rnum.callMethod(pi.getContext(), "to_f");
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
            // FIXME:
            //oj_err_set(pi.err, err_clas, "%s at line %d, column %d [%s:%d]", msg, pi.rd.line, pi.rd.col, file, line);
        } else {
            // FIXME:
            //_oj_err_set_with_location(pi.err, err_clas, msg, pi.json, pi.cur - 1, file, line);
        }
}

    static IRubyObject protect_parse(ParseInfo pip) {
        oj_parse2(pip);

        return pip.nilValue();
    }

    static void oj_pi_set_input_str(ParseInfo pi, ByteList input) {
        pi.json = input;
    }

    static IRubyObject oj_pi_parse(ThreadContext context, IRubyObject[] args, ParseInfo pi, ByteList json, int len, boolean yieldOk, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject	input;
        IRubyObject result;
        int			line = 0;

        if (args.length < 1) {
            throw pi.getRuntime().newArgumentError("Wrong number of arguments to parse.");
        }
        input = args[0];
        if (2 == args.length) {
            RubyOj.oj_parse_options(context, args[1], pi.options);
        }
        if (yieldOk && block.isGiven()) {
            pi.proc = block;
        } else {
            pi.proc = Block.NULL_BLOCK;
        }
        if (null != json) {
            pi.json = json;
        } else if (input instanceof RubyString) {
            oj_pi_set_input_str(pi, ((RubyString) input).getByteList());
        } else if (pi.nilValue() == input && Yes == pi.options.nilnil) {
            return pi.nilValue();
        } else {
            RubyModule clas = input.getMetaClass();

            if (runtime.getClass("StringIO") == clas) {
                input = input.callMethod(context, "string");
            } else if (!Platform.IS_WINDOWS && runtime.getFile() == clas && 0 == input.callMethod(context, "pos").convertToInteger().getLongValue()) {
                input = ((RubyFile) input).read(context);
            } else if (input.respondsTo("read")) {
                throw new IllegalArgumentException();
                // use stream parser instead
                // FIXME:
                //return oj_pi_sparse(args, pi, 0);
            } else {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }

            if (!(input instanceof RubyString)) {
                throw runtime.newArgumentError("strict_parse() expected a String or IO Object.");
            }
            pi.json = ((RubyString) input).getByteList();
        }

        // FIXME:
        /*
        if (Yes == pi.options.circular) {
            pi.circ_array = oj_circ_array_new();
        } else {
            pi.circ_array = null;
        }*/

        protect_parse(pi);

        result = pi.stack.firstElement().val;
        if (!pi.err_has()) {
            // If the stack is not empty then the JSON terminated early.
            Val	v;

            if (null != (v = pi.stack_peek())) {
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
            // FIXME:
            //rb_jump_tag(line);
        }

        if (pi.err_has()) {
            // FIXME: Should be JSon::ParseError but we need mimic for this impld
            throw context.runtime.newArgumentError(pi.error);
        }

        if (pi.options.quirks_mode == No) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                // FIXME: Should be JSon::ParseError but we need mimic for this impld
                throw context.runtime.newArgumentError("unexpected non-document Object");
            }
        }
        return result;
    }
}
