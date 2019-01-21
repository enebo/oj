package oj.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import oj.OjLibrary;
import oj.Options;
import oj.RubyOj;
import oj.dump.Dump;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import static oj.NextItem.*;
import static oj.Options.*;
import static oj.parse.ParserSource.EOF;

/**
 * Base class for all the parser types.
 *
 * Note: add_value() is common top-level method and addValue is pi-&gt;add_value.
 */
public abstract class Parse {
//    static final boolean HAS_PROC_WITH_BLOCK = false;
    static final int DEC_MAX = 15;
    static final int EXP_MAX = 100000;
    static final ByteList INFINITY = new ByteList(Dump.INFINITY_VALUE);
    static final ByteList INF_VALUE = new ByteList(Dump.INF_VALUE);
    static final ByteList NINF_VALUE = new ByteList(Dump.NINF_VALUE);
    static final ByteList NAN_VALUE = new ByteList(Dump.NAN_NUMERIC_VALUE);

    protected ParserSource source;
    protected ThreadContext context;
    public Stack<Val> stack;
    public Block proc;
    public IRubyObject undef;
    public String error = null;
    public Options options;
    public IRubyObject handler;
    public NumInfo ni;
    // C version uses head value of stack when stack is empty to hold this value.
    // We are using Java built-in Stack and we cannot do that.
    public IRubyObject value;
    protected OjLibrary oj = null;
    protected List<IRubyObject> circ_array;
    private static boolean debug = System.getProperty("oj.debug") != null;

    // In C this is in hash.c
    protected Map<ByteList, RubyClass> classMap = new HashMap<>();

    public Parse(ParserSource source, ThreadContext context, Options options, IRubyObject handler) {
        this.source = source;
        this.context = context;
        this.stack = new Stack<Val>();
        this.proc = null;
        this.options = options;
        this.handler = handler;
        this.ni = new NumInfo(this);
    }

    public IRubyObject stack_head_val() {
        if (stack.empty()) return value != null ? value : context.nil;

        return stack.firstElement().val;
    }

    public Val stack_peek() {
        return stack.empty() ? null : stack.peek();
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

    public IRubyObject nilValue() {
        return context.runtime.getNil();
    }

    public IRubyObject trueValue() {
        return context.runtime.getTrue();
    }

    public IRubyObject falseValue() {
        return context.runtime.getFalse();
    }

    public IRubyObject newString(ObjectParse stringValue) {
        return null;
    }

    public ThreadContext getContext() {
        return context;
    }

    public Ruby getRuntime() {
        return context.runtime;
    }

    void non_white() {
        if (debug) System.out.println(">non_white");
        while (true) {
            switch(source.current) {
            case ' ':
            case '\t':
            case '\f':
            case '\n':
            case '\r':
                source.advance();
                break;
            default:
                return;
            }
        }
    }

    void skip_comment() {
        if (debug) System.out.println(">skip_comment");
        source.advance();
        if ('*' == source.current) {
            source.advance();
            for (; source.current != EOF; source.advance()) {
                if ('*' == source.current && '/' == source.peek(1)) {
                    source.advance(1);
                    return;
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
                }
            }
        } else {
            setError("invalid comment format");
        }

        if (source.current == EOF) {
            setError("comment not terminated");
        }
    }

    void add_value(IRubyObject rval) {
        if (debug) System.out.println(">add_value: " + rval);
        Val parent = stack_peek();

        if (parent == null) { // simple add
            addValue(rval);
        } else {
            switch (parent.next) {
            case ARRAY_NEW:
            case ARRAY_ELEMENT:
                arrayAppendValue(rval);
                parent.next = ARRAY_COMMA;
                break;
                case HASH_VALUE:
                    hashSetValue(parent, rval);
                    parent.next = HASH_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next);
                    break;
            }
        }
    }

    void read_null() {
        if (debug) System.out.println(">read_null");
        if ('u' == source.advance() && 'l' == source.advance() && 'l' == source.advance()) {
            add_value(nilValue());
        } else {
            setError("expected null");
        }
    }
    
    void read_true() {
        if (debug) System.out.println(">read_true");
        if ('r' == source.advance() && 'u' == source.advance() && 'e' == source.advance()) {
            add_value(trueValue());
        } else {
            setError("expected true");
        }
    }

    void read_false() {
        if (debug) System.out.println(">read_false");
        if ('a' == source.advance() && 'l' == source.advance() && 's' == source.advance() && 'e' == source.advance()) {
            add_value(falseValue());
        } else {
            setError("expected false");
        }
    }

    // FIXME: Uses String.  Should get swapped to bytelist eventually or byte[]
    int read_hex() {
        if (debug) System.out.println(">read_hex");
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
                setError("invalid hex character");
                return 0;
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
            setError("invalid Unicode character");
        }
    }

// entered at /
    void read_escaped_str(int start) {
        if (debug) System.out.println(">read_escaped_str");
        ByteList buf = newByteList();
        int	cnt = source.currentOffset - start;
        int	code;
        Val parent = stack_peek();

        if (0 < cnt) {
            source.appendTo(buf, start);
        }

        for (int s = source.current; '"' != s; s = source.advance()) {
            if (s == EOF) {
                setError("quoted string not terminated");
                return;
            } else if ('\\' == s) {
                s = source.advance();
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
                    source.advance();
                    if (0 == (code = read_hex()) && err_has()) {
                        return;
                    }
                    source.advance(3);
                    if (0x0000D800 <= code && code <= 0x0000DFFF) {
                        int	c1 = (code - 0x0000D800) & 0x000003FF;
                        int	c2;

                        source.advance();
                        if ('\\' != source.current || 'u' != source.peek(1)) {
                            if (options.allow_invalid) {
                                source.advance(-1);
                                unicode_to_chars(buf, code);
                                break;
                            }
                            source.advance(-1);
                            setError("invalid escaped character");
                            return;
                        }
                        source.advance(2);
                        if (0 == (c2 = read_hex()) && err_has()) {
                            return;
                        }
                        source.advance(3);
                        c2 = (c2 - 0x0000DC00) & 0x000003FF;
                        code = ((c1 << 10) | c2) + 0x00010000;
                    }
                    unicode_to_chars(buf, code);
                    if (err_has()) {
                        return;
                    }
                    break;
                    default:
                        source.advance(-1);
                        setError("invalid escaped character");
                        return;
                }
            } else {
                buf.append(s);
            }
        }

        if (null == parent) {
            addCStr(buf, start);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    arrayAppendCStr(buf, start);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY:
                    if (null == (parent.key_val = hashKey(buf))) {
                    parent.key = buf.dup();
                } else {
                    parent.key = newByteList();
                }
                parent.k1 = (byte) source.at(start);
                parent.next =  HASH_COLON;
                break;
                case HASH_VALUE:
                    hashSetCStr(parent, buf, 0);
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
        // Do I source.advance or is it source.advanced at this potin
        //currentOffset = s + 1;
    }

    void read_str() {
        if (debug) System.out.println(">read_str");
        source.advance();
        int str = source.currentOffset;
        Val parent = stack_peek();

        for (; '"' != source.current; source.advance()) {
            if (source.current == EOF) {
                setError("quoted string not terminated");
                return;
            } else if ('\0' == source.current) {
                setError("NULL byte in string");
                return;
            } else if ('\\' == source.current) {
                read_escaped_str(str);
                return;
            }
        }
        if (null == parent) { // simple add
            if (debug) System.out.println(">read_str - addCStr");
            addCStr(source.subStr(str, source.currentOffset - str), str);
        } else {
            if (debug) System.out.println(">read_str - complicated: " + parent.next);
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    // FIXME: we should use begin + length instead of making bytelist here
                    arrayAppendCStr(source.subStr(str, source.currentOffset - str), str);
                    parent.next =  ARRAY_COMMA;
                    break;
                case HASH_NEW:
                case HASH_KEY: {
                    if (null == (parent.key_val = hashKey(str, source.currentOffset - str))) {
                        parent.key = source.subStr(str, source.currentOffset - str);
                    } else {
                        parent.key = newByteList();
                    }
                    parent.k1 = (byte) source.at(str);
                    parent.next = HASH_COLON;
                    if (debug) System.out.println(">read_str - complicated (key, k1): (" + parent.key + "," + (char) parent.k1 + ")");
                    break;
                }
                case HASH_VALUE:
                    hashSetCStr(parent, str, source.currentOffset - str);
                    parent.next =  HASH_COMMA;
                    break;
                case HASH_COMMA:
                case NONE:
                case ARRAY_COMMA:
                case HASH_COLON:
                default:
                    setError("expected " + parent.next + ", not a string");
                    break;
            }
        }
    }

    void read_num() {
        if (debug) System.out.println(">read_num");
        ni.reset();
        Val	parent = stack_peek();
        int start = source.currentOffset;

        ni.no_big = (FloatDec == options.bigdec_load);

        if ('-' == source.current) {
            source.advance();
            ni.neg = true;
        } else if ('+' == source.current) {
            source.advance();
        }
        if ('I' == source.current) {
            if (!options.allow_nan || !source.startsWith(INFINITY)) {
                setError("not a number or other Object");
                return;
            }
            source.advance(8);
            ni.infinity = true;
        } else if ('N' == source.current || 'n' == source.current) {
            if ('a' != source.peek(1) || ('N' != source.peek(2) && 'n' != source.peek(2))) {
                setError("not a number or other Object");
                return;
            }
            source.advance(3);
            ni.nan = true;
        } else {
            int dec_cnt = 0;
            boolean zero1 = false;

            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                if (ni.i == 0 && source.current == '0') {
                    zero1 = true;
                }
                if (ni.i > 0) {
                    dec_cnt++;
                }

                if (!ni.big) {
                    int	d = (source.current - '0');

                    if (d > 0) {
                        if (zero1 && options.mode == CompatMode) {
                            setError("not a number");
                            return;
                        }
                        zero1 = false;
                    }

                    ni.i = ni.i * 10 + d;
                    if (Long.MAX_VALUE <= ni.i || DEC_MAX < dec_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('.' == source.current) {
                source.advance();
                if (source.current < '0' || source.current > '9') {
                    setError("not a number");
                    return;
                }

                for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                    int	d = (source.current - '0');

                    if (ni.num > 0 || ni.i > 0) {
                        dec_cnt++;
                    }
                    ni.num = ni.num * 10 + d;
                    ni.div *= 10.0;
                    ni.di++;
                    if (ni.div >= Double.MAX_VALUE || DEC_MAX < dec_cnt) {
                        ni.big = true;
                    }
                }
            }
            if ('e' == source.current || 'E' == source.current) {
                boolean	eneg = false;

                ni.hasExp = true;
                source.advance();
                if ('-' == source.current) {
                    source.advance();
                    eneg = true;
                } else if ('+' == source.current) {
                    source.advance();
                }
                for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                    ni.exp = ni.exp * 10 + (source.current - '0');
                    if (EXP_MAX <= ni.exp) {
                        ni.big = true;
                    }
                }
                if (eneg) {
                    ni.exp = -ni.exp;
                }
            }
            ni.str_start = start;
            ni.str_length = source.currentOffset - start;
        }
        source.advance(-1);
        if (ni.big) {
            ByteList value = source.subStr(ni.str_start, ni.str_length);

            if (INF_VALUE.equals(value)) {
                ni.infinity = true;
            } else if  (NINF_VALUE.equals(value)) {
                ni.infinity = true;
                ni.neg = true;
            } else if  (NAN_VALUE.equals(value)) {
                ni.nan = true;
            }
        }
        if (BigDec == options.bigdec_load) {
            ni.big = true;
        }
        if (null == parent) {
            addNum(ni);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    arrayAppendNum(ni);
                    parent.next = ARRAY_COMMA;
                    break;
                case HASH_VALUE:
                    hashSetNum(parent, ni);
                    parent.next = HASH_COMMA;
                    break;
                default:
                    setError("expected " + parent.next);
                    break;
            }
        }
    }

    void array_start() {
        if (debug) System.out.println(">array_start");
        stack.push(new Val(startArray(), ARRAY_NEW));
    }

    void array_end() {
        if (debug) System.out.println(">array_end");
        Val	array = stack.pop();

        if (null == array) {
            setError("unexpected array close");
        } else if (ARRAY_COMMA != array.next && ARRAY_NEW != array.next) {
            setError("expected " + array.next + ", not an array close");
        } else {
            endArray();
            add_value(array.val);
        }
    }

    void hash_start() {
        if (debug) System.out.println(">hash_start");
        stack.push(new Val(startHash(), HASH_NEW));
    }

    void hash_end() {
        if (debug) System.out.println(">hash_end");
        Val	hash = stack_peek();

        // leave hash on stack until just before
        if (null == hash) {
            setError("unexpected hash close");
        } else if (HASH_COMMA != hash.next && HASH_NEW != hash.next) {
            setError("expected " + hash.next + ", not a hash close");
        } else {
            endHash();
            stack.pop();
            add_value(hash.val);
        }
    }

    void comma() {
        if (debug) System.out.println(">comma");
        Val	parent = stack_peek();

        if (null == parent) {
            setError("unexpected comma");
        } else if (ARRAY_COMMA == parent.next) {
            parent.next = ARRAY_ELEMENT;
        } else if (HASH_COMMA == parent.next) {
            parent.next = HASH_KEY;
        } else {
            setError("unexpected comma");
        }
    }

    void colon() {
        if (debug) System.out.println(">colon");
        Val	parent = stack_peek();

        if (null != parent && HASH_COLON == parent.next) {
            parent.next = HASH_VALUE;
        } else {
            setError("unexpected colon");
        }
    }

    void oj_parse2() {
        boolean first = true;

        err_init();
        for (source.advance(0); true; source.advance()) {
            if (debug) System.out.println("CURBEG: " + (char) source.current);
            non_white();

            if (!first && '\0' != source.current) {
                setError("unexpected characters after the JSON document");
            }

            if (!options.empty_string && first && source.current == '\0') {
                setError("unexpected character");
            }

            switch (source.current) {
                case '{':
                    hash_start();
                    break;
                case '}':
                    hash_end();
                    break;
                case ':':
                    colon();
                    break;
                case '[':
                    array_start();
                    break;
                case ']':
                    array_end();
                    break;
                case ',':
                    comma();
                    break;
                case '"':
                    read_str();
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
                    read_num();
                    break;
                case 't':
                    read_true();
                    break;
                case 'f':
                    read_false();
                    break;
                case 'n':
                    if ('u' == source.peek(1)) {
                        read_null();
                    } else {
                        read_num();
                    }
                    break;
                case '/':
                    skip_comment();
                    break;
                case '\0':
                    source.advance(-1);
                    return;
                default:
                    setError("unexpected character");
                    return;
            }
            if (err_has()) {
                return;
            }
            //System.out.println("CUREND: " + (char) source.current);
            if (stack.isEmpty()) {
                if (proc != null) {
                    proc.yield(getContext(), stack_head_val());
/*                } else {
                    if (HAS_PROC_WITH_BLOCK) {
                        Object[] args= new Object[] { stack.firstElement() };
                        proc.call(args);
                    } else {
                        throw getRuntime().newNotImplementedError("Calling a Proc with a block not supported in this version. Use func() {|x| } syntax instead.");
                    }
                }*/
                } else if (handler == null) {
                    first = false;
                }
            }
        }
    }

    IRubyObject protect_parse() {
        // nilnil option and nil source will not create a source.
        if (source != null) oj_parse2();

        return nilValue();
    }

    IRubyObject calc_hash_key(Val kval) {
        IRubyObject rkey;

        if (kval.key_val == null) {
            rkey = oj_encode(getRuntime().newString(kval.key));

            if (options.sym_key) return getRuntime().newSymbol(((RubyString) rkey).getByteList());
        } else {
            rkey = kval.key_val;
        }

        return rkey;
    }

    public static IRubyObject oj_encode(IRubyObject str) {
        // FIXME: Add 1.9 + 1.8 ability to convert to UTF-8
        return str;
    }

    public IRubyObject parse(OjLibrary oj, boolean yieldOk, Block block) {
        this.oj = oj;
        IRubyObject result;
        int			line = 0;

        if (yieldOk && block.isGiven()) {
            proc = block;
        } else {
            proc = null;
        }

        circ_array = options.circular ? new ArrayList<IRubyObject>() : null;

        protect_parse();

        result = value;
        if (!err_has()) {
            // If the stack is not empty then the JSON terminated early.
            Val	v;

            if (null != (v = stack_peek())) {
                switch (v.next) {
                    case ARRAY_NEW:
                    case ARRAY_ELEMENT:
                    case ARRAY_COMMA:
                        setError("Array not terminated");
                        break;
                    case HASH_NEW:
                    case HASH_KEY:
                    case HASH_COLON:
                    case HASH_VALUE:
                    case HASH_COMMA:
                        setError("Hash/Object not terminated");
                        break;
                    default:
                        setError("not terminated");
                }
            }
        }

        if (err_has()) {
            parseError(error);
        }

        if (handler != null) return context.nil;

        if (!options.quirks_mode) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                parseError("unexpected non-document Object: " + result.getMetaClass());
            }
        }

        return result;
    }

    // Equivalent to C-callbacks in C impl.  Look to Parse subclasses for specific overrides.
    // Java naming conventions used for call back to avoid naming conflicts with the recursive
    // descent parser method names.

    public void addCStr(ByteList value, int orig) {
    }

    public void arrayAppendCStr(ByteList value, int orig) {
    }

    public void hashSetCStr(Val kval, int start, int length) {
        hashSetCStr(kval, source.subStr(start, length), start);
    }

    public void hashSetCStr(Val parent, ByteList value, int orig) {
    }

    public void addValue(IRubyObject value) {
        if (options.trace) trace_parse_call("add_value", value);
        this.value = value;
    }

    public void arrayAppendValue(IRubyObject value) {
        ((RubyArray) stack_peek().val).append(value);
    }


    public void addNum(NumInfo value) {
    }

    public void arrayAppendNum(NumInfo value) {
    }

    public void hashSetNum(Val parent, NumInfo value) {
    }

    public void hashSetValue(Val parent, IRubyObject value) {
    }

    public IRubyObject endArray() {
        return context.nil;
    }

    public IRubyObject startArray() {
        return getRuntime().newArray();
    }

    public IRubyObject endHash() {
        if (options.trace) trace_parse_hash_end();
        return context.nil;
    }

    public IRubyObject startHash() {
        if (options.trace) trace_parse_call("start_hash");

        return RubyHash.newHash(context.runtime);
    }

    // For hash keys which came in with no escape characters.
    public IRubyObject hashKey(int start, int length) {
        return null;
    }

    public IRubyObject hashKey(ByteList key) {
        return null;
    }

    public void parseError(String message) {
        throw context.runtime.newRaiseException(oj.getParseError(), message);
    }

    protected RubyClass nameToStruct(IRubyObject name, RubyClass errorClass) {
        RubyString structName = (RubyString) TypeConverter.checkStringType(context.runtime, name);

        return (RubyClass) resolveClassPath(structName.getByteList(), false, errorClass);
    }

    protected RubyModule nameToClass(ByteList name, boolean autoDefine, RubyClass errorClass) {
        if (!options.class_cache) return resolveClassPath(name, autoDefine, errorClass);

        RubyModule clas = classMap.get(name);

        if (clas == null) clas = resolveClassPath(name, autoDefine, errorClass);

        return clas;
    }

    protected RubyModule resolveClassPath(ByteList className, boolean autoDefine, RubyClass errorClass) {
        RubyModule clas = context.runtime.getObject();

        ByteList name = className;
        for (int index = name.indexOf(':'); index != -1 && index + 1 < name.realSize(); index = name.indexOf(':')) {
            if (name.get(index + 1) != ':') {
                return null;
            }
            ByteList baseName = name.makeShared(0, index);
            index++; // skip past second ':'
            name = name.makeShared(index + 1, name.realSize() - index - 1);

            if (ByteList.EMPTY_BYTELIST.equals(name)) { // protection against 'Foo::'
                return null;
            }

            clas = resolveClassName(clas, baseName, autoDefine);

            if (clas == null) return null;
        }

        clas = resolveClassName(clas, name, autoDefine);
        if (clas == null) throw context.runtime.newRaiseException(errorClass, "class " + className + " is not defined");

        return clas;
    }

    protected RubyModule resolveClassName(RubyModule base, ByteList name, boolean autoDefine) {
        String id = name.toString(); // FIXME: m17n issue (utf-8 source needs iso8859_1 for id on 9.2)

        IRubyObject clas = base.getConstantAt(id);

        return clas == null && autoDefine ?
                base.defineClassUnder(id, RubyOj.oj(context).bag, RubyObject.OBJECT_ALLOCATOR) : (RubyModule) clas;
    }

    public static ByteList newByteList() {
        ByteList bytelist = new ByteList();
        bytelist.setEncoding(UTF8Encoding.INSTANCE);
        return bytelist;
    }

    protected void trace_parse_call(String name, IRubyObject value) {
    }

    public void trace_parse_call(String name) {
    }

    protected void trace_parse_hash_end() {
    }
}
