package oj.parse;

import oj.Doc;
import oj.Leaf;
import oj.LeafType;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.LeafType.*;

/**
 * This uses ParserSource as a source and pi->s is source.currentOffset,
 */
public class FastParse {
    Doc doc;
    ParserSource source;
    ThreadContext context;

    public FastParse(ThreadContext context, ByteList json) {
        this.context = context;
        this.source = new StringParserSource(json);
        this.doc = new Doc(context);
    }

    void next_non_white() {
        for (; true; source.advance()) {
            switch(source.current) {
                case ' ':
                case '\t':
                case '\f':
                case '\n':
                case '\r':
                    break;
                case '/':
                    skip_comment();
                    break;
                default:
                    return;
            }
        }
    }

    Doc self_doc(IRubyObject self) {
        if (null == doc) {
            throw context.runtime.newIOError("Document already closed or not open.");
        }
        return doc;
    }

    void skip_comment() {
        source.advance(); // skip first /
        if ('*' == source.current) {
            source.advance();
            for (; '\0' != source.current; source.advance()) {
                if ('*' == source.current && '/' == source.peek(1)) {
                    source.advance();
                    return;
                } else if ('\0' == source.current) {
                    raiseError("comment not terminated");
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
            raiseError("invalid comment");
        }
    }

    Leaf read_next() {
        Leaf leaf = null;

        source.advance(0); // FIXME: fix this in parse and fastparse to happen via Source
        next_non_white();	// skip white space
        switch (source.current) {
            case '{':
                leaf = read_obj();
                break;
            case '[':
                leaf = read_array();
                break;
            case '"':
                leaf = read_str();
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
                leaf = read_num();
                break;
            case 't':
                leaf = read_true();
                break;
            case 'f':
                leaf = read_false();
                break;
            case 'n':
                leaf = read_nil();
                break;
            case '\0':
            default:
                break; // returns 0
        }
        doc.size++;

        return leaf;
    }

    Leaf read_obj() {
        Leaf h = new Leaf(context, T_HASH);

        source.advance();
        next_non_white();
        if ('}' == source.current) {
            source.advance();
            return h;
        }
        while (true) {
            next_non_white();
            ByteList key = null;
            if ('"' != source.current || null == (key = read_quoted_value())) {
                raiseError("unexpected character");
            }
            next_non_white();
            if (':' == source.current) {
                source.advance();
            } else {
                raiseError("invalid format, expected :");
            }
            Leaf val = read_next();
            if (val == null) raiseError("unexpected character");

            h.append_hash_element(val, key);
            next_non_white();
            if ('}' == source.current) {
                source.advance();
                break;
            } else if (',' == source.current) {
                source.advance();
            } else {
                //printf("*** '%s'\n", pi->s);
                raiseError("invalid format, expected , or } while in an object");
            }
        }
        return h;
    }

    Leaf read_array() {
        Leaf array = new Leaf(context, T_ARRAY);

        source.advance();
        next_non_white();
        if (']' == source.current) {
            source.advance();
            return array;
        }
        for(int cnt = 1; ; cnt++) {
            next_non_white();
            Leaf element = read_next();
            if (element == null) raiseError("unexpected character");
            array.append_array_element(element, cnt);
            next_non_white();
            if (',' == source.current) {
                source.advance();
            } else if (']' == source.current) {
                source.advance();
                break;
            } else {
                raiseError("invalid format, expected , or ] while in an array");
            }
        }
        return array;
    }

    Leaf read_str() {
        Leaf leaf = new Leaf(context, T_STRING);

        leaf.str = read_quoted_value();

        return leaf;
    }

    Leaf read_num() {
        int start = source.currentOffset;
        LeafType type = T_FIXNUM;

        if ('-' == source.current) {
            source.advance();
        }
        // digits
        for (; '0' <= source.current && source.current <= '9'; source.advance()) {
        }
        if ('.' == source.current) {
            type = T_FLOAT;
            source.advance();
            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
            }
        }
        if ('e' == source.current || 'E' == source.current) {
            source.advance();
            if ('-' == source.current || '+' == source.current) {
                source.advance();
            }
            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
            }
        }
        Leaf leaf = new Leaf(context, type);
        leaf.str = source.subStr(start, source.currentOffset - start);

        return leaf;
    }

    Leaf read_true() {
        Leaf leaf = new Leaf(context, T_TRUE);

        source.advance();
        if ('r' != source.current || 'u' != source.peek(1) || 'e' != source.peek(2)) {
            raiseError("invalid format, expected 'true'");
        }
        source.advance(3);

        return leaf;
    }

    Leaf read_false() {
        Leaf leaf = new Leaf(context, T_FALSE);

        source.advance();
        if ('a' != source.current || 'l' != source.peek(1) || 's' != source.peek(2) || 'e' != source.peek(3)) {
            raiseError("invalid format, expected 'false'");
        }
        source.advance(4);

        return leaf;
    }

    Leaf read_nil() {
        Leaf leaf = new Leaf(context, T_NIL);

        source.advance();
        if ('u' != source.current || 'l' != source.peek(1) || 'l' != source.peek(2)) {
            raiseError("invalid format, expected 'nil'");
        }
        source.advance(3);

        return leaf;
    }

    private void raiseError(String message) {
        // FIXME: Throw real oj error.
        throw new RuntimeException(message);
    }

    int read_4hex() {
        int b = 0;
        int i;

        for (i = 0; i < 4; i++, source.advance()) {
            b = b << 4;
            if ('0' <= source.current && source.current <= '9') {
                b += source.current - '0';
            } else if ('A' <= source.current && source.current <= 'F') {
                b += source.current - 'A' + 10;
            } else if ('a' <= source.current && source.current <= 'f') {
                b += source.current - 'a' + 10;
            } else {
                raiseError("invalid hex character");
            }
        }
        source.advance(-1);
        return b;
    }

    void unicode_to_chars(ByteList buf, int code) {
        if (0x0000007F >= code) {
            buf.append((char)code);
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
            raiseError("invalid Unicode character");
        }
    }

    /*
     * Note: This should probably not append unconditionally to a bytelist and instead region
     * copy unless we experience an escape character.
     */
    ByteList read_quoted_value() {
        ByteList value = new ByteList();
        value.setEncoding(UTF8Encoding.INSTANCE);

        source.advance();
        // int start = source.currentOffset;
        // boolean foundEscape = false;
        for (; '"' != source.current; source.advance()) {
            if ('\0' == source.current) {
                raiseError("quoted string not terminated");
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
                        code = read_4hex();
                        if (0x0000D800 <= code && code <= 0x0000DFFF) {
                            int	c1 = (code - 0x0000D800) & 0x000003FF;
                            int c2;

                            source.advance();
                            if ('\\' != source.current || 'u' != source.peek(1)) {
                                raiseError("invalid escaped character");
                            }
                            c2 = read_4hex();
                            c2 = (c2 - 0x0000DC00) & 0x000003FF;
                            code = ((c1 << 10) | c2) + 0x00010000;
                        }
                        unicode_to_chars(value, code);
                        break;
                    }
                    default:
                        raiseError("invalid escaped character");
                        break;
                }
            } else {//if (t != source.currentOffset) {  // FIXME: see note above about doing this more efficiently
                value.append(source.current);
            }
        }
        source.advance();

        return value;
    }

    IRubyObject protect_open_proc(Block block) {
        doc.data = read_next(); // parse
        doc.wheres[doc.where] = doc.data;
        doc.where = doc.where_path;
        if (block.isGiven()) {
            return block.yield(context, doc);
        }
        return context.nil;
    }

    public IRubyObject parse_json(Block block) {
        // skip UTF-8 BOM if present
        if (0xEF == source.peek(0) && 0xBB == source.peek(1) && 0xBF == source.peek(2)) {
            source.advance(3);
        }

        // Omitted getrlimit stack calculation here.

        IRubyObject result = protect_open_proc(block);
        return !block.isGiven() ? doc : result;
    }
}
