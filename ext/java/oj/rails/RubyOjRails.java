package oj.rails;

import oj.DumpFunc;
import oj.OjLibrary;
import oj.Options;
import oj.RubyMimic;
import oj.RubyOj;
import oj.dump.Dump;
import oj.dump.RailsDump;
import oj.parse.ParserSource;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHelper;

import java.util.HashMap;
import java.util.Map;

import static oj.Options.RailsMode;

@JRubyModule(name = "Rails")
public class RubyOjRails extends RubyModule {
    // FIXME: These values are guesses and not tuned.
    private static Map<ByteList, DumpFunc> dumpMap = new HashMap<>(7);

    public RubyOjRails(Ruby runtime) {
        super(runtime);
    }

    public static OjLibrary oj(ThreadContext context) {
        return resolveOj(context.runtime.getObject().getConstantAt("Oj"));
    }

    public static OjLibrary resolveOj(IRubyObject self) {
        return (OjLibrary) self.getInternalVariables().getInternalVariable("_oj");
    }

    public static IRubyObject optimize(ThreadContext context, IRubyObject[] args, Map<IRubyObject, ROpt> rot, boolean optimize) {
        RailsHolder holder = holder(context);
        if (args.length == 0) {
            holder.railsHashOpt = optimize;
            holder.railsArrayOpt = optimize;
            holder.railsFloatOpt = optimize;

            for (ByteList className : dumpMap.keySet()) {
                RubyClass clas = checkIsClass(resolveClasspath(context, className));


                if (clas != null && railsGetOpt(context, rot, clas) == null) createOpt(context, rot, clas);
            }

            for (ROpt ro : rot.values()) {
                ro.on = optimize;
            }
        } else {
            for (IRubyObject arg : args) {
                if (arg == context.runtime.getHash()) {
                    holder.railsHashOpt = optimize;
                } else if (arg == context.runtime.getArray()) {
                    holder.railsArrayOpt = optimize;
                } else if (arg == context.runtime.getFloat()) {
                    holder.railsFloatOpt = optimize;
                } else if (arg instanceof RubyClass) { // FIXME: what happens with noo-class here?
                    ROpt ro = railsGetOpt(context, rot, arg);
                    if (ro == null) ro = createOpt(context, rot, (RubyClass) arg);

                    if (ro != null) ro.on = optimize;
                }
            }

        }

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject encode(ThreadContext context, IRubyObject self, IRubyObject obj) {
        OjLibrary oj = RubyOj.oj(context);
        Options options = OjLibrary.getDefaultOptions(context);
        ByteList result = new RailsDump(context, oj, options).obj_to_json(obj);
        return context.runtime.newString(result); // FIXME: should use oj_encode.
    }

    @JRubyMethod(module = true)
    public static IRubyObject encode(ThreadContext context, IRubyObject self, IRubyObject obj, IRubyObject opts) {
        OjLibrary oj = RubyOj.oj(context);
        Options options = OjLibrary.getDefaultOptions(context);

        RubyOj.parse_options(context, opts, options);
        ByteList result = new RailsDump(context, oj, options).obj_to_json(obj);
        return context.runtime.newString(result); // FIXME: should use oj_encode.
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject optimize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return optimize(context, args, holder(context).ropts, true);
    }

    @JRubyMethod(module = true, rest = true)
    public static IRubyObject deoptimize(ThreadContext context, IRubyObject self, IRubyObject[] args) {
        return optimize(context, args, holder(context).ropts, false);
    }

    @JRubyMethod(name = "optimized?")
    public static IRubyObject optimized_p(ThreadContext context, IRubyObject self, IRubyObject clas) {
        ROpt ro = railsGetOpt(context, holder(context).ropts, clas);

        return context.runtime.newBoolean(ro != null && ro.on);
    }

    @JRubyMethod(module = true)
    public static IRubyObject mimic_JSON(ThreadContext context, IRubyObject self) {
        RubyModule json = context.runtime.getOrCreateModule("JSON");

        json.defineAnnotatedMethods(RubyMimic.class);

        OjLibrary.default_options.mode = RailsMode;

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject set_encoder(ThreadContext context, IRubyObject self) {
        OjLibrary oj = oj(context);
        RubyModule active = activeSupport(context);

        active.callMethod(context, "json_encoder=", oj.getOjRailsEncoder());

        RubyModule json = module(context, active, "JSON");
        RubyModule encoding = module(context, json, "Encoding");

        // FIXME: Unsure why cext needs to undef when it will redef over it?
        encoding.getMetaClass().undef(context, "use_standard_json_time_format=");
        encoding.getMetaClass().undef(context, "escape_html_entities_in_json=");
        encoding.getMetaClass().undef(context, "time_precision=");

        /*encoding.getMetaClass().addModuleFunction("use_standard_json_time_format=", new DynamicMethod("use_standard_json_time_format=") {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject iRubyObject, RubyModule rubyModule, String s, IRubyObject[] args, Block block) {
                if (args.length != 1) throw context.runtime.newArgumentError(args.length, 1);

                IRubyObject state = context.runtime.newBoolean(args[0].isTrue());

                self.getInstanceVariables().setInstanceVariable("@use_standard_json_time_format", state);

                return state;
            }

            @Override
            public DynamicMethod dup() {
                return null;
            }
        });*/

        encoding.getMetaClass().defineAnnotatedMethods(RubyOjRailsEncoderReplacements.class);

        OjLibrary.default_options.sec_prec = encoding.getInstanceVariable("@time_precision").convertToInteger().getIntValue();

        return context.nil;
    }

    @JRubyMethod(module = true)
    public static IRubyObject set_decoder(ThreadContext context, IRubyObject self) {
        RubyModule json = context.runtime.getOrCreateModule("JSON");
        RubyClass jsonError = json.defineClassUnder("JSONError", context.runtime.getStandardError(), RubyException.EXCEPTION_ALLOCATOR);
        json.defineClassUnder("ParserError", jsonError, RubyException.EXCEPTION_ALLOCATOR);

        json.undef(context, "parse");
        json.defineAnnotatedMethods(RubyOjRailsDecoderReplacements.class);

        return context.nil;
    }

    private static RubyModule module(ThreadContext context, RubyModule parent, String name) {
        IRubyObject module = parent.getConstantAt(name);

        if (module == null) throw context.runtime.newStandardError(name + " is not loaded");
        if (!(module instanceof RubyModule)) throw context.runtime.newTypeError(name + " is not a module");

        return (RubyModule) module;
    }

    // FIXME: cache
    private static RubyModule activeSupport(ThreadContext context) {
        return module(context, context.runtime.getObject(), "ActiveSupport");
    }

    // FIXME: cache
    private static RubyModule activeRecord(ThreadContext context) {
        return module(context, context.runtime.getObject(), "ActiveRecord");
    }

    // FIXME: cache
    private static RubyModule activeRecordBase(ThreadContext context) {
        return module(context, activeRecord(context), "Base");
    }

    private static ByteList COLONS = new ByteList(new byte[]{':', ':'});

    private static IRubyObject resolveClasspath(ThreadContext context, ByteList fullName) {
        RubyString fullNameString = context.runtime.newString(fullName);
        final Ruby runtime = context.runtime;

        return ByteListHelper.split(fullName, COLONS, (index, segment, module) -> {
            if (index == 0) {
                if (segment.realSize() == 0) return runtime.getObject();  // '::Foo...'
                module = runtime.getObject();
            }

            String id = RubySymbol.newConstantSymbol(runtime, fullNameString, segment).idString();
            IRubyObject obj = ((RubyModule) module).getConstantAt(id);

            if (obj == null) return null;
            if (!(obj instanceof RubyModule)) throw runtime.newTypeError(segment + " does not refer to class/module");

            return obj;
        }, (index, segment, module) -> {
            if (module == null) module = runtime.getObject(); // Bare 'Foo'

            String id = RubySymbol.newConstantSymbol(runtime, fullNameString, segment).idString();
            return ((RubyModule) module).getConstantAt(id);
        });
    }

    // c: oj_rails_get_opt
    private static ROpt railsGetOpt(ThreadContext context, Map<IRubyObject, ROpt> rot, IRubyObject clas) {
        // FIXME: Do we and cext really need this check?
        if (rot == null) rot = holder(context).ropts;

        return rot.get(clas);
    }

    // c: create_opt
    private static ROpt createOpt(ThreadContext context, Map<IRubyObject, ROpt> rot, RubyClass clas) {
        // FIXME: This is likely wrong if it is already a class.
        ByteList className = clas.rubyName().getByteList();
        ROpt ro = new ROpt(DumpObjAttrs, true);

        rot.put(clas, ro);

        for (ByteList name : dumpMap.keySet()) {
            if (className.equals(name)) ro.dump = dumpMap.get(name);
        }

        if (ro.dump == DumpObjAttrs) {
            RubyModule base = activeRecordBase(context);

            if (clas.isKindOfModule(base)) {
                ro.dump = DumpActiveRecord;
            } else if (clas.isKindOfModule(context.runtime.getStructClass())) {
                ro.dump = DumpStruct;
            } else if (clas.isKindOfModule(context.runtime.getEnumerable())) {
                ro.dump = DumpEnumerable;
            } else if (clas.isKindOfModule(context.runtime.getException())) {
                ro.dump = DumpToS;
            }
        }

        return ro;
    }

    private static RubyClass checkIsClass(IRubyObject obj) {
        if (!(obj instanceof RubyClass)) throw obj.getRuntime().newTypeError("Expecting a class.  Got " + obj);

        return (RubyClass) obj;
    }

    // FIXME: Implement calling into proper dumper without standing up entire dump object
    private static DumpFunc DumpActiveRecord = null;
    private static DumpFunc DumpEnumerable = null;
    private static DumpFunc DumpObjAttrs = null;
    private static DumpFunc DumpStruct = null;
    private static DumpFunc DumpToS = null;

    private static RailsHolder holder(ThreadContext context) {
        return oj(context).getRailsHolder();
    }
}