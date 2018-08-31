package oj;

import oj.dump.Dump;
import oj.options.DumpType;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import java.util.Stack;

import static oj.options.DumpType.*;

/** Document-class: Oj::StringWriter
 *
 * Supports building a JSON document one element at a time. Build the document
 * by pushing IRubyObjects into the document. Pushing an array or an object will
 * create that element in the JSON document and subsequent pushes will add the
 * elements to that array or object until a pop() is called. When complete
 * calling to_s() will return the JSON document. Note tha calling to_s() before
 * construction is complete will return the document in it's current state.
 */
public class StringWriter extends RubyObject {
    public Options opts;
    public int depth = 0;
    public Stack<DumpType> types = new Stack<>();
    public boolean keyWritten = false;
    public Dump dump;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StringWriter(runtime, klass);
        }
    };

    public static RubyClass createStringWriterClass(Ruby runtime, RubyModule oj) {
        RubyClass clazz = oj.defineClassUnder("StringWriter", runtime.getObject(), ALLOCATOR);
        clazz.defineAnnotatedMethods(StringWriter.class);
        return clazz;
    }

    public StringWriter(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] argv) {
        OjLibrary oj = RubyOj.oj(context);
        opts = oj.default_options.dup(context);

        if (argv.length == 1) RubyOj.parse_options(context, argv[0], opts);
        dump = Dump.createDump(context, oj, opts);

        return this;
    }

    /* call-seq: push_key(key)
     *
     * Pushes a key onto the JSON document. The key will be used for the next push
     * if currently in a JSON object and ignored otherwise. If a key is provided on
     * the next push then that new key will be ignored.
     * @param [String] key the key pending for the next push
     */
    @JRubyMethod
    public IRubyObject push_key(ThreadContext context, IRubyObject key) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
        push_key(context, string.getByteList());
        return context.nil;
    }

    /* call-seq: push_object(key=nil)
     *
     * Pushes an object onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_object(ThreadContext context, Block block) {
        push_object(context, (ByteList) null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_object(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            push_object(context, (ByteList) null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_object(context, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context);
        }

        return context.nil;
    }

    /* call-seq: push_array(key=nil)
     *
     * Pushes an array onto the JSON document. Future pushes will be to this object
     * until a pop() is called.
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_array(ThreadContext context, Block block) {
        push_array(context, (ByteList) null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_array(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            push_array(context, (ByteList) null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_array(context, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            pop(context);
        }
        return context.nil;
    }

    /* call-seq: push_value(value, key=nil)
     *
     * Pushes a IRubyObject onto the JSON document.
     * @param [Object] IRubyObject IRubyObject to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_value(ThreadContext context, IRubyObject value) {
        push_value(context, value, (ByteList) null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_value(ThreadContext context, IRubyObject value, IRubyObject key) {
        if (context.nil == key) {
            push_value(context, value, (ByteList) null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_value(context, value, string.getByteList());
        }
        return context.nil;
    }

    /* call-seq: push_json(value, key=nil)
     *
     * Pushes a string onto the JSON document. The String must be a valid JSON
     * encoded string. No additional checking is done to verify the validity of the
     * string.
     * @param [String] IRubyObject JSON document to add to the JSON document
     * @param [String] key the key if adding to an object in the JSON document
     */
    @JRubyMethod
    public IRubyObject push_json(ThreadContext context, IRubyObject value) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, value);
        push_json(context, string.getByteList(), null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_json(ThreadContext context, IRubyObject value, IRubyObject key) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, value);

        if (context.nil == key) {
            push_json(context, string.getByteList(), null);
        } else {
            RubyString keyString = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            push_json(context, string.getByteList(), keyString.getByteList());
        }
        return context.nil;
    }


    /* call-seq: pop()
     *
     * Pops up a level in the JSON document closing the array or object that is
     * currently open.
     */
    @JRubyMethod
    public IRubyObject pop(ThreadContext context) {
        if (keyWritten) {
            keyWritten = false;
            throw context.runtime.newStandardError("Can not pop after writing a key but no value.");
        }

        if (types.empty()) throw context.runtime.newStandardError("Can not pop with no open array or object.");

        DumpType type = types.pop();

        dump.fill_indent(types.size());
        if (type == ObjectNew || type == ObjectType) {
            dump.out.append('}');
        } else if (type == ArrayNew || type == ArrayType) {
            dump.out.append(']');
        }

        if (types.empty() && 0 <= dump.out.indent) dump.out.append('\n');

        return context.nil;
    }

    /* call-seq: pop_all()
     *
     * Pops all level in the JSON document closing all the array or object that is
     * currently open.
     */
    @JRubyMethod
    public IRubyObject pop_all(ThreadContext context) {
        while (!types.empty()) {
            pop(context);
        }
        return context.nil;
    }

    /* call-seq: reset()
     *
     * Reset the writer back to the empty state.
     */
    @JRubyMethod
    public IRubyObject reset(ThreadContext context) {
        depth = 0;
        types.removeAllElements();
        keyWritten = false;
        dump.out.reset();

        return context.nil;
    }

    /* call-seq: to_s()
     *
     * Returns the JSON document string in what ever state the construction is at.
     */
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        RubyString rstr = dump.out.asString(context);

        rstr.setEncoding(UTF8Encoding.INSTANCE);

        return rstr;
    }


    void key_check(ThreadContext context, ByteList key) {
        DumpType type = peekTypes();

        if (null == key && (ObjectNew == type || ObjectType == type)) {
            throw context.runtime.newStandardError("Can not push onto an Object without a key.");
        }
    }

    void push_type(ThreadContext context, DumpType type) {
        types.push(type);
    }

    void maybe_comma() {
        DumpType type = peekTypes();
        if (type == ObjectNew) {
            types.set(types.size() - 1, ObjectType);
        } else if (type == ArrayNew) {
            types.set(types.size() - 1, ArrayType);
        } else if (type == ObjectType || type == ArrayType) {
            // Always have a few characters available in the out.buf.
            dump.out.append(',');
        }
    }

    void push_key(ThreadContext context, ByteList key) {
        DumpType type = peekTypes();

        if (keyWritten) {
            throw context.runtime.newStandardError("Can not push more than one key before pushing a non-key.");
        }
        if (ObjectNew != type && ObjectType != type) {
            throw context.runtime.newStandardError("Can only push a key onto an Object.");
        }

        maybe_comma();
        if (!types.empty()) dump.fill_indent(types.size());

        dump.dump_cstr(key, false, false);
        dump.out.append(':');
        keyWritten = true;
    }

    void push_object(ThreadContext context, ByteList key) {
        dump_key(context, key);
        dump.out.append('{');
        push_type(context, ObjectNew);
    }

    void push_array(ThreadContext context, ByteList key) {
        dump_key(context, key);
        dump.out.append('[');
        push_type(context, ArrayNew);
    }

    void push_value(ThreadContext context, IRubyObject val, ByteList key) {
        dump_key(context, key);
        dump.dump_val(val, types.size(), null);
    }

    void push_json(ThreadContext context, ByteList json, ByteList key) {
        dump_key(context, key);
        dump.dump_raw(json);
    }

    void dump_key(ThreadContext context, ByteList key) {
        if (keyWritten) {
            keyWritten = false;
        } else {
            key_check(context, key);
            maybe_comma();
            if (!types.empty()) {
                dump.fill_indent(types.size());
            }
            if (null != key) {
                dump.dump_cstr(key, false, false);
                dump.out.append(':');
            }
        }
    }

    /**
     * We are using Java standard library Stack which throws on empty stack.  This
     * method protects against that and returns null instead of an exception.
     */
    public DumpType peekTypes() {
        return types.isEmpty() ? null : types.peek();
    }
}
