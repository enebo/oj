package oj;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import static oj.NextItem.*;
import static oj.Options.*;
import static oj.ParserSource.EOF;

/**
 * Base class for all the parser types.
 *
 * Note: add_value() is common top-level method and addValue is pi-&gt;add_value.
 */
public abstract class Parse {
//    static final boolean HAS_PROC_WITH_BLOCK = false;
    static final int DEC_MAX = 15;
    static final int EXP_MAX = 100000;
    static final ByteList INFINITY = new ByteList(new byte[] { 'I', 'n', 'f', 'i', 'n', 'i', 't', 'y'});

    protected ParserSource source;
    protected ThreadContext context;
    public Stack<Val> stack;
    public Block proc;
    public IRubyObject undef;
    public String error = null;
    public Options options;
    public IRubyObject handler;
    public NumInfo ni;
    // C version uses head value of stack even when stack is empty.  We are using
    // Java Stack and we cannot do that.  Store into this field instead.
    public IRubyObject value;
    protected OjLibrary oj = null;
    protected List<IRubyObject> circ_array;
    private boolean debug = System.getProperty("oj.debug") != null;

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
        if (stack.empty()) {
            if (value != null) {
                return value;
            }

            return context.nil;
        }

        return stack.firstElement().val;
    }

    public Val stack_peek() {
        if (stack.empty()) {
            return null;
        }

        return stack.peek();
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
                appendValue(rval);
                parent.next = ARRAY_COMMA;
                break;
                case HASH_VALUE:
                    setValue(parent, rval);
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
        int zero_cnt = 0;
        int start = source.currentOffset;

        ni.no_big = (FloatDec == options.bigdec_load);

        if ('-' == source.current) {
            source.advance();
            ni.neg = true;
        } else if ('+' == source.current) {
            source.advance();
        }
        if ('I' == source.current) {
            if (!source.startsWith(INFINITY)) {
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
            for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                ni.dec_cnt++;
                if (ni.big) {
                    //ni.big++; // What is this for?
                } else {
                    int	d = (source.current - '0');

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
            if ('.' == source.current) {
                source.advance();
                for (; '0' <= source.current && source.current <= '9'; source.advance()) {
                    int	d = (source.current - '0');

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
            ni.dec_cnt -= zero_cnt;
            ni.str_start = start;
            ni.str_length = source.currentOffset - start;
        }
        source.advance(-1);
        if (BigDec == options.bigdec_load) {
            ni.big = true;
        }
        if (null == parent) {
            addNum(ni);
        } else {
            switch (parent.next) {
                case ARRAY_NEW:
                case ARRAY_ELEMENT:
                    appendNum(ni);
                    parent.next = ARRAY_COMMA;
                    break;
                case HASH_VALUE:
                    setNum(parent, ni);
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
                } else {
                    first = false;
                }
            }
        }
    }

    void oj_set_error_at(String format, String... data) {
        //RubyModule errorClass = getRuntime().getClass("Oj").getClass("ParseError");
        String file = context.getFile();
        int line = context.getLine();

     //   if (null == json) {
            // FIXME:
            //oj_err_set(err, err_clas, "%s at line %d, column %d [%s:%d]", msg, rd.line, rd.col, file, line);
//        } else {
            // FIXME:
            //_oj_err_set_with_location(err, err_clas, msg, json, currentOffset - 1, file, line);
//        }
}

    IRubyObject protect_parse() {
        // nilnil option and nil source will not create a source.
        if (source != null) oj_parse2();

        return nilValue();
    }

    IRubyObject calc_hash_key(Val kval) {
        IRubyObject rkey;

        if (null == kval.key_val) {
            rkey = oj_encode(getRuntime().newString(kval.key));

            if (Yes == options.sym_key) {
                return getRuntime().newSymbol(((RubyString) rkey).getByteList());
            }
        } else {
            rkey = kval.key_val;
        }

        return rkey;
    }

    public static IRubyObject oj_encode(IRubyObject str) {
        // FIXME: Add 1.9 + 1.8 ability to convert to UTF-8
        return str;
    }

    // FIXME:
    public IRubyObject sparse(OjLibrary oj, IRubyObject[] args, InputStream fd, Block block) {
        this.oj = oj;

        return null;
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

        if (Yes == options.circular) {
            circ_array = new ArrayList<>();
        } else {
            circ_array = null;
        }

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
        // proceed with cleanup
        if (0 != line) {
            // FIXME:
            //rb_jump_tag(line);
        }

        if (err_has()) {
            parseError(error);
        }

        if (options.quirks_mode == No) {
            if (result instanceof RubyNil || result instanceof RubyBoolean || result instanceof RubyFixnum ||
                    result instanceof RubyFloat || result instanceof RubyModule || result instanceof RubySymbol) {
                parseError("unexpected non-document Object");
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
        this.value = value;
    }

    public void appendValue(IRubyObject value) {
        ((RubyArray) stack_peek().val).append(value);
    }

    public void setValue(Val parent, IRubyObject value) {
    }

    public void addNum(NumInfo value) {
    }

    public void appendNum(NumInfo value) {
    }

    public void setNum(Val parent, NumInfo value) {
    }

    public IRubyObject endArray() {
        return context.nil;
    }

    public IRubyObject startArray() {
        return getRuntime().newArray();
    }

    public IRubyObject endHash() {
        return context.nil;
    }

    public IRubyObject startHash() {
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

    protected RubyClass nameToStruct(IRubyObject name) {
        RubyString structName = (RubyString) TypeConverter.checkStringType(context.runtime, name);

        return resolveClassPath(structName.getByteList(), false);
    }

    protected RubyClass nameToClass(ByteList name, boolean autoDefine) {
        if (options.class_cache == No) {
            return resolveClassPath(name, autoDefine);
        }

        RubyClass clas = classMap.get(name);
        if (clas == null) {
            clas = resolveClassPath(name, autoDefine);
        }

        return clas;
    }

    protected RubyClass resolveClassPath(ByteList className, boolean autoDefine) {
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
        if (clas == null) parseError("class " + className + " is not defined");

        // FIXME: This could module or class here???
        return (RubyClass) clas;
    }

    protected RubyModule resolveClassName(RubyModule base, ByteList name, boolean autoDefine) {
        // FIXME: m17n issue
        IRubyObject clas = base.getConstantAt(name.toString());
        if (clas == null && autoDefine) {
            // FIXME: This should be oj Bag type
            clas = context.runtime.getHash();
        }

        return (RubyModule) clas;
    }

    public static ByteList newByteList() {
        ByteList bytelist = new ByteList();
        bytelist.setEncoding(UTF8Encoding.INSTANCE);
        return bytelist;
    }
}
