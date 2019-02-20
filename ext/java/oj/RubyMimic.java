package oj;

import oj.dump.Dump;
import oj.parse.CompatParse;
import oj.parse.ParserSource;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static oj.Options.CompatMode;
import static oj.Options.FloatDec;
import static oj.Options.JXEsc;
import static oj.RubyOj.oj;
import static oj.dump.Dump.MAX_DEPTH;
import static oj.options.NanDump.RaiseNan;
import static oj.parse.Parse.oj_encode;

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
        IRubyObject dummy = ext.getConstantNoConstMissing("Parser");

        if (dummy == null) {
            ext.defineClassUnder("Parser", runtime.getObject(), OBJECT_ALLOCATOR);
        }

        // FIXME: possible to crash here
        RubyModule generator = (RubyModule) ext.getConstantNoConstMissing("Generator");
        if (dummy == null) {
            ext.defineClassUnder("Generator", runtime.getObject(), OBJECT_ALLOCATOR);
        }

        dummy = runtime.getGlobalVariables().get("$LOADED_FEATURES");
        // convince Ruby that the json gem has already been loaded
        if (dummy instanceof RubyArray) {
            RubyArray ary = (RubyArray) dummy;

            ary.append(runtime.newString("json"));

            RubyModule oj = runtime.getModule("Oj");
            if (args.length > 0) {
                Helpers.invoke(context, oj, "mimic_loaded", args[1]);
            } else {
                Helpers.invoke(context, oj, "mimic_loaded");
            }
        }

        runtime.getGlobalVariables().set("$VERBOSE", runtime.getFalse());

        mimic.setInternalVariable("_oj", ojLibrary);
        mimic.defineAnnotatedMethods(RubyMimic.class);


        //rb_define_module_function(rb_cObject, "JSON", mimic_dump_loadThreadContext context, IRubyObject self, IRubyObject[] args) {}
        //rb_define_method(rb_cObject, "to_json", mimic_object_to_json, -1);


        if (mimic.getConstantNoConstMissing("ParseError") != null) {
            mimic.remove_const(context, runtime.newSymbol("ParseError"));
        }
        mimic.setConstant("ParseError", ojLibrary.getParseError());

        if (mimic.getConstantNoConstMissing("State") == null) {
            runtime.getLoadService().require("oj/state");
        }

        ojLibrary.setMimicState(generator.getConstantAt("State"));

        ojLibrary.default_options = Options.mimicOptions(context);
        ojLibrary.default_options.to_json = true;

        return mimic;
    }

    @JRubyMethod(module = true, name = "create_id=", rest = true)
    public static IRubyObject create_id_set(ThreadContext context, IRubyObject self, IRubyObject id) {
        if (!id.isNil()) {
            OjLibrary.default_options.create_id = id.convertToString().getByteList();
        }

        return id;
    }

    @JRubyMethod(module = true)
    public static IRubyObject create_id(ThreadContext context, IRubyObject self) {
        if (OjLibrary.default_options.create_id != null) {
            return oj_encode(context.runtime.newString(OjLibrary.default_options.create_id));
        }

        return context.runtime.newString("json_class");
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject dump(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        OjLibrary oj = oj(context);
        Options	copts = OjLibrary.getDefaultOptions(context);

        copts.str_rx.clear();
        copts.escape_mode = JXEsc;
        copts.mode = CompatMode;
        copts.dump_opts.max_depth = MAX_DEPTH; // when using dump there is no limit

        if (args.length >= 2) {
            int	limit;

            // The json gem take a more liberal approach to optional
            // arguments. Expected are (obj, anIO=nil, limit=nil) yet the io
            // argument can be left off completely and the 2nd argument is then
            // the limit.
            if ((limit = limitArg(context, args[1])) >= 0) {
                copts.dump_opts.max_depth = limit;
            }
            if (3 <= args.length && 0 <= (limit = limitArg(context, args[2]))) {
                copts.dump_opts.max_depth = limit;
            }
        }

        Dump dump = Dump.createDump(context, oj, copts);
        // FIXME: Hmmm
        //dump.caller = CALLER_DUMP
        dump.omit_nil = copts.dump_opts.omit_nil;

        IRubyObject rstr = oj_encode(context.runtime.newString(dump.obj_to_json(args[0])));

        if (2 <= args.length && context.nil != args[1] && args[1].respondsTo("write")) {
            IRubyObject io = args[1];
            io.callMethod(context, "write", rstr);
            rstr = io;
        }

        return rstr;
    }

    @JRubyMethod(name = {"load", "restore"}, module = true, rest = true)
    public static IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        OjLibrary oj = RubyOj.resolveOj(self);
        Options options = oj.default_options.dup(context);
        ParserSource source = RubyOj.processArgs(context, args, options);

        IRubyObject obj = new CompatParse(source, context, options).parse(oj, false, Block.NULL_BLOCK);

        // FIXME: We need to make some common logic to extract explicit proc arg with implicit block.
        /*
        if (args.length >= 2) {
            p = args[1];
        } else {
            p = context.nil;
        }*/

        mimic_walk(context, context.nil, obj, block);

        return obj;
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject recurse_proc(ThreadContext context, IRubyObject self, IRubyObject obj, Block block) {
        mimic_walk(context, context.nil, obj, block);

        return context.nil;
    }

    @JRubyMethod(module = true, name = "[]", rest = true)
    public static IRubyObject aref(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        if (args.length == 0) throw context.runtime.newArgumentError("wrong number of arguments (0 for 1)");

        if (args[0] instanceof RubyString) {
            return load(context, self, args, Block.NULL_BLOCK);
        } else {
            return dump(context, self, args);
        }
    }

    @JRubyMethod(name = {"generate", "fast_generate", "unparse", "fast_unparse"}, module = true, required = 1, optional = 1)
    public static IRubyObject generate(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Options copts = OjLibrary.getDefaultOptions(context);

        copts.str_rx.clear();

        return generateCore(context, args, copts);
    }

    @JRubyMethod(name = {"pretty_generate", "pretty_unparse"}, module = true, rest = true)
    public static IRubyObject pretty_generate(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        Options copts = OjLibrary.getDefaultOptions(context);
        IRubyObject[] rargs = new IRubyObject[2];

        // Some (all?) json gem to_json methods need a State instance and not just
        // a Hash. I haven't dug deep enough to find out why but using a State
        // instance and not a Hash gives the desired behavior.
        rargs[0] = args[0];

        // FIXME: In C code dyn dispatch to has_key? but then rb_hash_aset which means args[1] must be a hash to work...Forcing to hash here.. correct or no?
        RubyHash h = args.length == 1 ? RubyHash.newSmallHash(context.runtime) : args[1].convertToHash();
        setHashEntryIfFalse(context, h, "indent", "  ");
        setHashEntryIfFalse(context, h, "space_before", "");
        setHashEntryIfFalse(context, h, "space", " ");
        setHashEntryIfFalse(context, h, "object_nl", "\n");
        setHashEntryIfFalse(context, h, "array_nl", "\n");
        rargs[1] = oj(context).getMimicState().callMethod(context, "new", h);

        copts.str_rx.clear();
        copts.dump_opts.indent_str = TWO_SPACES;
        copts.dump_opts.before_sep = ByteList.EMPTY_BYTELIST;
        copts.dump_opts.after_sep = ONE_SPACE;
        copts.dump_opts.hash_nl = NEWLINE;
        copts.dump_opts.array_nl = NEWLINE;
        copts.dump_opts.use = true;

        return generateCore(context, rargs, copts);
    }

    @JRubyMethod(module = true, required = 1, optional = 1)
    public static IRubyObject parse(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        if (args.length == 2) {
            return parseCore(context, args[0], args[1], false);
        } else {
            return parseCore(context, args[0], null, false);
        }
    }

    @JRubyMethod(module = true, name = "parse!", rest = true)
    public static IRubyObject parse_bang(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        if (args.length == 2) {
            return parseCore(context, args[0], args[1], true);
        } else {
            return parseCore(context, args[0], null, true);
        }
    }

    @JRubyMethod()
    public static IRubyObject state(ThreadContext context, IRubyObject self) {
        return RubyOj.oj(context).getMimicState();
    }

    private static void mimic_walk(final ThreadContext context, IRubyObject key, IRubyObject obj, final Block proc) {
        if (obj instanceof RubyHash) {
            RubyHash hash = (RubyHash) obj;
            // FIXME: Make inner class instead of anonymous
            hash.visitAll(new RubyHash.Visitor() {
                @Override
                public void visit(IRubyObject hashKey, IRubyObject hashValue) {
                    mimic_walk(context, hashKey, hashValue, proc);
                }
            });
        } else if (obj instanceof RubyArray) {
            RubyArray ary = (RubyArray) obj;
            int cnt = ary.getLength();
            for (int i = 0; i < cnt; i++) {
                mimic_walk(context, context.nil, ary.entry(i), proc);
            }
        }

        if (proc.isGiven()) {
            proc.yield(context, obj);
        }
    }

    private static int limitArg(ThreadContext context, IRubyObject value) {
        if (value == context.nil || !(value instanceof RubyFixnum)) return -1;

        return ((RubyFixnum) value).getIntValue();
    }


    private static IRubyObject generateCore(ThreadContext context, IRubyObject[] args, Options copts) {
        if (args.length < 1) throw context.runtime.newArgumentError(0 , 1);
        if (2 == args.length) RubyOj.parse_options(context, args[1], copts); // FIXME: This is oj_parse_mimic_dump_options in C

        copts.dump_opts.nan_dump = RaiseNan;
        copts.mode = CompatMode;
        copts.to_json = true;
        OjLibrary oj = oj(context);

        Dump dump = Dump.createDump(context, oj, copts);
        // FIXME: figure this out
        // dump.caller = CALLER_GENERATE;

        IRubyObject[] params = new IRubyObject[args.length - 1];
        System.arraycopy(args, 1, params, 0, args.length - 1);
        ByteList result = dump.obj_to_json_using_params(args[0], params);

        return context.runtime.newString(result); // FIXME: should use oj_encode.

    }

    private static IRubyObject parseCore(ThreadContext context, IRubyObject input, IRubyObject opts, boolean bang) {
        Options options = OjLibrary.getDefaultOptions(context);
        options.auto_define = false;
        options.quirks_mode = true;
        options.allow_invalid = false;
        options.empty_string = false;
        options.create_ok = false;
        options.allow_nan = (bang ? true : false);
        options.nilnil = false;
        options.bigdec_load = FloatDec;
        options.mode = CompatMode;
        // FIXME: no usage of max_depth.
        int max_depth = 100;

        if (opts != null) {
            RubyHash ropts = opts.convertToHash();
            IRubyObject v;

            v = ropts.op_aref(context, context.runtime.newSymbol("symbolize_names"));
            if (!v.isNil()) options.sym_key = v.isTrue();

            v = ropts.op_aref(context, context.runtime.newSymbol("quirks_mode"));
            if (!v.isNil()) options.quirks_mode = v.isTrue();

            v = ropts.op_aref(context, context.runtime.newSymbol("create_additions"));
            if (!v.isNil()) options.create_ok = v.isTrue();

            v = ropts.op_aref(context, context.runtime.newSymbol("allow_nan"));
            if (!v.isNil()) options.allow_nan = v.isTrue();

            RubySymbol s = context.runtime.newSymbol("hash_class");
            if (ropts.callMethod(context, "has_key?", s).isTrue()) {
                v = ropts.op_aref(context, s);
                if (v.isNil()) {
                    options.hash_class = context.nil;
                } else {
                    options.hash_class = checkIsClass(v);
                }
            }

            s = context.runtime.newSymbol("object_class");
            if (ropts.callMethod(context, "has_key?", s).isTrue()) {
                v = ropts.op_aref(context, s);
                if (v.isNil()) {
                    options.hash_class = context.nil;
                } else {
                    options.hash_class = checkIsClass(v);
                }
            }

            s = context.runtime.newSymbol("array_class");
            if (ropts.callMethod(context, "has_key?", s).isTrue()) {
                v = ropts.op_aref(context, s);
                if (v.isNil()) {
                    options.hash_class = context.nil;
                } else {
                    options.hash_class = checkIsClass(v);
                }
            }

            v = ropts.op_aref(context, context.runtime.newSymbol("max_nesting"));
            if (v.isTrue()) {
                max_depth = 100;
            } else if (v instanceof RubyFixnum) {
                max_depth = ((RubyFixnum) v).getIntValue();
            } else if (v.isNil() || v == context.runtime.getFalse()) {
                max_depth = 0;
            }

            RubyOj.parseOptMatchString(context, options.str_rx, ropts);

            if (options.create_ok && options.sym_key) {
                throw context.runtime.newArgumentError(":symbolize_names and :create_additions can not both be true.");
            }
        }

        ParserSource source = RubyOj.getSource(context, input, options);
        return new CompatParse(source, context, options).parse(RubyOj.oj(context), true, Block.NULL_BLOCK);
    }

    private static void setHashEntryIfFalse(ThreadContext context, RubyHash hash, String symbolName, String value) {
        RubySymbol symbol = context.runtime.newSymbol(symbolName);

        if (hash.has_key_p(symbol).isFalse()) hash.aset(symbol, context.runtime.newString(value));
    }

    // FIXME: consolidate (another copy in rubyojrails)
    private static RubyClass checkIsClass(IRubyObject obj) {
        if (!(obj instanceof RubyClass)) throw obj.getRuntime().newTypeError("Expecting a class.  Got " + obj);

        return (RubyClass) obj;
    }


    private static final ByteList TWO_SPACES = new ByteList(new byte[] {' ', ' '});
    private static final ByteList ONE_SPACE = new ByteList(new byte[] {' '});
    private static final ByteList NEWLINE = new ByteList(new byte[] {'\n'});
    private static final ByteList JSON_CLASS = new ByteList(new byte[] {'j', 's', 'o', 'n', '_', 'c', 'l', 'a', 's', 's'});
}
