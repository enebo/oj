package oj.parse;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.Leaf.NUM_MAX;
import static oj.parse.NumInfo.OJ_INFINITY;
import static oj.parse.Parse.INFINITY;
import static oj.parse.ParserSource.EOF;

/** This JSON parser is a single pass, destructive, callback parser. It is a
* single pass parse since it only make one pass over the characters in the
* JSON document string. It is destructive because it re-uses the content of
* the string for IRubyObjects in the callback and places \0 characters at various
* places to mark the end of tokens and strings. It is a callback parser like
* a SAX parser because it uses callback when document elements are
* encountered.
*
* Parsing is very tolerant. Lack of headers and even mispelled element
* endings are passed over without raising an error. A best attempt is made in
* all cases to parse the string.
*/
public class SajParse { // extends Parse {
    ThreadContext context;
    ParserSource source;
    IRubyObject handler;
    boolean has_error;
    boolean has_hash_start;
    private boolean has_add_value;
    private boolean has_hash_end;
    private boolean has_array_start;
    private boolean has_array_end;
    private static boolean debug = System.getProperty("oj.debug") != null;

    public SajParse(ParserSource source, ThreadContext context) {
        this.source = source;
        this.context = context;
    }

    void call_error(String msg) {
        StringBuilder buf = new StringBuilder(msg);

        int row = -1;
        int column = -1;

        if (source.canCalculateSourcePositions()) {
            row = source.row();
            column = source.column();

            buf.append(" at line " + row + ", column " + column);
        }

        handler.callMethod(context, "error",
                new IRubyObject[] {context.runtime.newString(buf.toString()),
                context.runtime.newFixnum(row),
                context.runtime.newFixnum(column)});
    }

    void non_white() {
        while (true) {
            switch(source.current) {
                case ' ':
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                    source.advance();
                    break;
                case '/':
                    skip_comment();
                default:
                    return;
            }
        }
    }

    public void call_add_value(IRubyObject value, ByteList key) {
        IRubyObject	k = null == key ?
                context.nil : Parse.oj_encode(context.runtime.newString(key));

        handler.callMethod(context, "add_value", new IRubyObject[] {value, k});
    }

    public void call_no_value(String method, ByteList key) {
        IRubyObject	k = null == key ?
                context.nil : Parse.oj_encode(context.runtime.newString(key));

        handler.callMethod(context, method, k);
    }

    public void skip_comment() {
        source.advance(); // skip first /
        if ('*' == source.current) {
            source.advance();
            for (; source.current != EOF; source.advance()) {
                if ('*' == source.current && '/' == source.peek(1)) {
                    source.advance(1);
                    return;
                } else if (source.current == EOF) {
                    error("comment not terminated");
                }
            }
        } else if ('/' == source.current) {
            for (; true; source.advance()) {
                switch (source.current) {
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
            error("invalid comment");
        }
    }

    public void read_next(ByteList key) {
        non_white();	// skip white space
        if (debug) System.out.println("read_next - current: " + (char) source.current);
        switch (source.current) {
            case '{':
                read_hash(key);
                break;
            case '[':
                read_array(key);
                break;
            case '"':
                read_str(key);
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
                read_num(key);
                break;
            case 'I':
                read_num(key);
                break;
            case 't':
                read_true(key);
                break;
            case 'f':
                read_false(key);
                break;
            case 'n':
                read_nil(key);
                break;
            case '\0':
                return;
            default:
                return;
        }
    }

    public void read_hash(ByteList key) {
        if (debug) System.out.println("read_hash: " + (char) source.current);
        ByteList ks;

        if (has_hash_start) call_no_value("hash_start", key);

        source.advance();
        non_white();
        if ('}' == source.current) {
            source.advance();
        } else {
            while (true) {
                non_white();
                ks = read_quoted_value();
                non_white();
                if (':' == source.current) {
                    source.advance();
                } else {
                    error("invalid format, expected :");
                }
                read_next(ks);
                non_white();
                if ('}' == source.current) {
                    source.advance();
                    break;
                } else if (',' == source.current) {
                    source.advance();
                } else {
                    error("invalid format, expected , or } while in an object");
                }
            }
        }
        if (has_hash_end) call_no_value("hash_end", key);
    }

    public void read_array(ByteList key) {
        if (has_array_start) call_no_value("array_start", key);

        source.advance();
        non_white();
        if (']' == source.current) {
            source.advance();
        } else {
            while (true) {
                read_next(null);
                non_white();
                if (',' == source.current) {
                    source.advance();
                } else if (']' == source.current) {
                    source.advance();
                    break;
                } else {
                    error("invalid format, expected , or ] while in an array");
                }
            }
        }
        if (has_array_end) call_no_value("array_end", key);
    }

    void read_str(ByteList key) {
        ByteList text = read_quoted_value();
        if (has_add_value) {
            IRubyObject	s = Parse.oj_encode(context.runtime.newString(text));
            call_add_value(s, key);
        }
    }


    void read_num(ByteList key) {
        int start = source.currentOffset;
        long n = 0;
        long a = 0;
        long div = 1;
        long e = 0;
        boolean neg = false;
        boolean eneg = false;
        int big = 0;

        if ('-' == source.current) {
            source.advance();
            neg = true;
        } else if ('+' == source.current) {
            source.advance();
        }
        if ('I' == source.current) {
            if (!source.startsWith(INFINITY)) error("number or other IRubyObject");

            source.advance(8);
            if (neg) {
                if (has_add_value) call_add_value(context.runtime.newFloat(-OJ_INFINITY), key);
            } else {
                if (has_add_value) call_add_value(context.runtime.newFloat(OJ_INFINITY), key);
            }
            return;
        }
        for (; '0' <= source.current && source.current <= '9'; source.advance()) {
            if (big > 0) {
                big++;
            } else {
                n = n * 10 + (source.current - '0');
                if (NUM_MAX <= n) {
                    big = 1;
                }
            }
        }
        if ('.' == source.current) {
            source.advance();
            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                a = a * 10 + (source.current - '0');
                div *= 10;
                if (NUM_MAX <= div) {
                    big = 1;
                }
            }
        }
        if ('e' == source.current || 'E' == source.current) {
            source.advance();
            if ('-' == source.current) {
                source.advance();
                eneg = true;
            } else if ('+' == source.current) {
                source.advance();
            }
            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                e = e * 10 + (source.current - '0');
                if (NUM_MAX <= e) {
                    big = 1;
                }
            }
        }
        if (0 == e && 0 == a && 1 == div) {
            if (big > 0) {
                if (has_add_value) {
                    // FIXME: make better method that substr for offset -> current to make bytelist
                    IRubyObject val = context.runtime.newString(source.subStr(start, source.currentOffset));
                    IRubyObject value = RubyBigDecimal.newInstance(context, context.runtime.getClass("BigDecimal"), val);
                    call_add_value(value, key);
                }
            } else {
                if (neg) n = -n;

                if (has_add_value) call_add_value(context.runtime.newFixnum(n), key);
            }
            return;
        } else { // decimal
            if (big > 0) {
                if (has_add_value) {
                    IRubyObject val = context.runtime.newString(source.subStr(start, source.currentOffset));
                    IRubyObject value = RubyBigDecimal.newInstance(context, context.runtime.getClass("BigDecimal"), val);
                    call_add_value(value, key);
                }
            } else {
                double	d = (double)n + (double)a / (double)div;

                if (neg) {
                    d = -d;
                }
                if (1 < big) {
                    e += big - 1;
                }
                if (0 != e) {
                    if (eneg) {
                        e = -e;
                    }
                    d *= Math.pow(10.0, e);
                }
                if (has_add_value) call_add_value(context.runtime.newFloat(d), key);
            }
        }
    }

    void read_true(ByteList key) {
        source.advance();
        if ('r' != source.current || 'u' != source.peek(1) || 'e' != source.peek(2)) {
            error("invalid format, expected 'true'");
        }
        source.advance(3);
        if (has_add_value) call_add_value(context.tru, key);
    }

    void read_false(ByteList key) {
        source.advance();
        if ('a' != source.current || 'l' != source.peek(1) || 's' != source.peek(2) || 'e' != source.peek(3)) {
            error("invalid format, expected 'false'");
        }
        source.advance(4);
        if (has_add_value) call_add_value(context.fals, key);
    }

    void read_nil(ByteList key) {
        source.advance();
        if ('u' != source.current || 'l' != source.peek(1) || 'l' != source.peek(2)) {
            error("invalid format, expected 'null'");
        }
        source.advance(3);
        if (has_add_value) call_add_value(context.nil, key);
    }

    // FIXME: Uses String.  Should get swapped to bytelist eventually or byte[]
    int read_hex() {
        int	b = 0;

        for (int i = 0; i < 4; i++) {
            int h = source.peek(i);
            b = b << 4;
            if ('0' <= h && h <= '9') {
                b += h - '0';
            } else if ('A' <= h && h <= 'F') {
                b += h - 'A' + 10;
            } else if ('a' <= h && h <= 'f') {
                b += h - 'a' + 10;
            } else {
                source.current = h;
                error("invalid hex character");
            }
        }
        return b;
    }

    void unicode_to_chars(ByteList buf, int code) {
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
            error("invalid Unicode");
        }
    }

    ByteList read_quoted_value() {
        ByteList value = new ByteList();
        value.setEncoding(UTF8Encoding.INSTANCE);

        source.advance();
        // int start = source.currentOffset;
        // boolean foundEscape = false;
        for (; '"' != source.current; source.advance()) {
            if ('\0' == source.current) {
                error("quoted string not terminated");
            } else if ('\\' == source.current) {
                source.advance();
                switch (source.current) {
                    case 'n': value.append('\n');	break;
                    case 'r': value.append('\r');	break;
                    case 't': value.append('\t');	break;
                    case 'f': value.append('\f');	break;
                    case 'b': value.append('\b');	break;
                    case '"': value.append('"');	break;
                    case '/': value.append('/');	break;
                    case '\\': value.append('\\');	break;
                    case 'u': {
                        int code;

                        source.advance();
                        code = read_hex();
                        if (0x0000D800 <= code && code <= 0x0000DFFF) {
                            int	c1 = (code - 0x0000D800) & 0x000003FF;
                            int c2;

                            source.advance();
                            if ('\\' != source.current || 'u' != source.peek(1)) {
                                error("invalid escaped character");
                            }
                            c2 = read_hex();
                            c2 = (c2 - 0x0000DC00) & 0x000003FF;
                            code = ((c1 << 10) | c2) + 0x00010000;
                        }
                        unicode_to_chars(value, code);
                        break;
                    }
                    default:
                        error("invalid escaped character");
                        break;
                }
            } else {//if (t != source.currentOffset) {  // FIXME: see note above about doing this more efficiently
                value.append(source.current);
            }
        }
        source.advance();

        return value;
    }

    public void parse(IRubyObject handler) {
        source.advance(0);
        // skip UTF-8 BOM if present
        if (0xEF == source.peek(0) && 0xBB == source.peek(1) && 0xBF == source.peek(2)) {
            source.advance(3);
        }

        // initialize parse info
        this.handler = handler;
        has_hash_start = handler.respondsTo("hash_start");
        has_hash_end = handler.respondsTo("hash_end");
        has_array_start = handler.respondsTo("array_start");
        has_array_end = handler.respondsTo("array_end");
        has_add_value = handler.respondsTo("add_value");
        has_error = handler.respondsTo("error");
        read_next(null);
        non_white();

        if (EOF != source.current) error("invalid format, extra characters");
    }

    private void error(String message) {
        if (has_error) {
            call_error(message);
        } else {
            // FIXME: Use real error here
            throw context.runtime.newArgumentError("BOOO: " + message);
            //raise_error(message, pi.str, pi.s);
        }
    }
}
