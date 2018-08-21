package oj;

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
import org.jruby.util.TypeConverter;

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
    private StrWriter sw;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StringWriter(runtime, klass);
        }
    };

    public static void createStringWriterClass(Ruby runtime, RubyModule oj) {
        RubyClass clazz = oj.defineClassUnder("StringWriter", runtime.getObject(), ALLOCATOR);
        clazz.setInternalVariable("_oj", oj);
        clazz.defineAnnotatedMethods(StringWriter.class);
    }

    public StringWriter(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] argv) {
        OjLibrary oj = RubyOj.resolveOj(this);
        sw = new StrWriter(context, oj);

        if (1 == argv.length) {
            RubyOj.parse_options(context, argv[0], sw.opts);
        }
        sw.out.indent = sw.opts.indent;

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
        Dump.push_key(context, sw, string.getByteList());
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
        Dump.push_object(context, sw, null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            Dump.pop(context, sw);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_object(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            Dump.push_object(context, sw, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            Dump.push_object(context, sw, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            Dump.pop(context, sw);
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
        Dump.push_array(context, sw, null);

        if (block.isGiven()) {
            block.yield(context, context.nil);
            Dump.pop(context, sw);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_array(ThreadContext context, IRubyObject key, Block block) {
        if (context.nil == key) {
            Dump.push_array(context, sw, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            Dump.push_array(context, sw, string.getByteList());
        }

        if (block.isGiven()) {
            block.yield(context, context.nil);
            Dump.pop(context, sw);
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
        Dump.push_value(context, sw, value, null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_value(ThreadContext context, IRubyObject value, IRubyObject key) {
        if (context.nil == key) {
            Dump.push_value(context, sw, value, null);
        } else {
            RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            Dump.push_value(context, sw, value, string.getByteList());
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
        Dump.push_json(context, sw, string.getByteList(), null);
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject push_json(ThreadContext context, IRubyObject value, IRubyObject key) {
        RubyString string = (RubyString) TypeConverter.checkStringType(context.runtime, value);

        if (context.nil == key) {
            Dump.push_json(context, sw, string.getByteList(), null);
        } else {
            RubyString keyString = (RubyString) TypeConverter.checkStringType(context.runtime, key);
            Dump.push_json(context, sw, string.getByteList(), keyString.getByteList());
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
        Dump.pop(context, sw);
        return context.nil;
    }

    /* call-seq: pop_all()
     *
     * Pops all level in the JSON document closing all the array or object that is
     * currently open.
     */
    @JRubyMethod
    public IRubyObject pop_all(ThreadContext context) {
        Dump.pop_all(context, sw);
        return context.nil;
    }

    /* call-seq: reset()
     *
     * Reset the writer back to the empty state.
     */
    @JRubyMethod
    public IRubyObject reset(ThreadContext context) {
        sw.depth = 0;
        sw.types.removeAllElements();
        sw.keyWritten = false;
        sw.out.reset();

        return context.nil;
    }

    /* call-seq: to_s()
     *
     * Returns the JSON document string in what ever state the construction is at.
     */
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        RubyString rstr = sw.out.asString(context);

        rstr.setEncoding(UTF8Encoding.INSTANCE);

        return rstr;
    }
}
