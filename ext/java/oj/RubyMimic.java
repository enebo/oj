package oj;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyGlobal;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by enebo on 9/2/15.
 */
public class RubyMimic {
    static ObjectAllocator OBJECT_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyObject(ruby, rubyClass);
        }
    };

    public static RubyModule defineMimic(ThreadContext context, OjLibrary ojLibrary, IRubyObject[] args) {
        Ruby runtime = context.runtime;


        RubyModule mimic = runtime.getOrCreateModule("JSON");
        RubyModule ext = mimic.defineOrGetModuleUnder("Ext");
        IRubyObject dummy = ext.getConstant("Parser");

        if (dummy == null) {
            dummy = ext.defineClassUnder("Parser", runtime.getObject(), OBJECT_ALLOCATOR);
        }

        dummy = ext.getConstant("Generator");
        if (dummy == null) {
            dummy = ext.defineClassUnder("Generator", runtime.getObject(), OBJECT_ALLOCATOR);
        }

        dummy = runtime.getGlobalVariables().get("$LOADED_FEATURES");
        // convince Ruby that the json gem has already been loaded
        if (dummy instanceof RubyArray) {
            RubyArray ary = (RubyArray) dummy;

            ary.append(runtime.newString("json"));

            RubyModule oj = runtime.getClass("Oj");
            if (args.length > 0) {
                Helpers.invoke(context, oj, "mimic_loaded", args[1]);
            } else {
                Helpers.invoke(context, oj, "mimic_loaded");
            }
        }

        runtime.getGlobalVariables().set("$VERBOSE", runtime.getFalse());

        mimic.defineAnnotatedMethods(RubyMimic.class);

        //rb_define_module_function(rb_cObject, "JSON", mimic_dump_loadThreadContext context, IRubyObject[] args) {}
        //rb_define_method(rb_cObject, "to_json", mimic_object_to_json, -1);


        if (mimic.getConstant("ParseError") != null) {
            mimic.remove_const(context, runtime.newSymbol("ParseError"));
        }
        mimic.setConstant("ParseError", ojLibrary.getParseError());

        if (mimic.getConstant("State") == null) {
            mimic.defineClassUnder("State", runtime.getObject(), OBJECT_ALLOCATOR);
        }

        ojLibrary.default_options = Options.mimicOptions();
        ojLibrary.default_options.to_json = Options.Yes;

        return mimic;
    }

    @JRubyMethod(module = true, name = "parser=", rest = true) 
    public static IRubyObject parser_set(ThreadContext context, IRubyObject[] args) {
        return null;
    }
    
    @JRubyMethod(module = true, name = "generator=", rest = true)
    public static IRubyObject generator_set() {
        return null;
    }
    
    @JRubyMethod(module = true, name = "create_id=", rest = true)
    public static IRubyObject create_id_set() {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject dump(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject load(ThreadContext context, IRubyObject[] args) {
        return null;
    }
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject restore(ThreadContext context, IRubyObject[] args) {
        return null;
    }
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject recurse_proc(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, name = "[]", rest = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject generate(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject fast_generate(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject pretty_generate(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    /* for older versions of JSON, the deprecated unparse methods */
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject unparse(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject fast_unparse(ThreadContext context, IRubyObject[] args) {
        return null;
    }
    @JRubyMethod(module = true, rest = true)
    public static IRubyObject pretty_unparse(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject parse(ThreadContext context, IRubyObject[] args) {
        return null;
    }

    @JRubyMethod(module = true, name = "parse!", rest = true)
    public static IRubyObject parse_bang(ThreadContext context, IRubyObject[] args) {
        return null;
    }
}
