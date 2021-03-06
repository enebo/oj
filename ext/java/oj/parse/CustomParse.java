package oj.parse;

import oj.OjLibrary;
import oj.Options;
import org.joda.time.DateTime;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyComplex;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

// Note: This shopuld really extend CompatParse but parse_xml_time seems to be only shared thing and that is in ObjectParse?
public class CustomParse extends ObjectParse {
    private static final ByteList TIME = new ByteList(new byte[] {'t', 'i', 'm', 'e'});
    private static final ByteList O_KEY = new ByteList(new byte[] {'^', 'o'});

    public CustomParse(ParserSource source, ThreadContext context, Options options) {
        super(source, context, options);
    }

    @Override
    public IRubyObject parse(OjLibrary oj, boolean yieldOk, Block block) {
        options.nilnil = true;

        return super.parse(oj, yieldOk, block);
    }

    @Override
    public void hashSetCStr(Val kval, ByteList str, int orig) {
        ByteList key = kval.key;
        Val	parent = stack_peek();
        IRubyObject	rkey = kval.key_val;

        if (rkey == null && options.create_ok && options.create_id != null &&  options.create_id.equals(key)) {
            parent.clas = (RubyClass) nameToClass(str, false, context.runtime.getArgumentError());
            if (O_KEY.equals(key) && parent.clas != null && !code_has(parent.clas)) {
                parent.val = parent.clas.allocate();
            }
        } else {
            IRubyObject	rstr = getRuntime().newString(str);

            if (rkey == null) {
                rkey = oj_encode(context.runtime.newString(key));
                rstr = oj_encode(rstr);
                if (options.sym_key) rkey = context.runtime.newSymbol(((RubyString) rkey).getByteList());
            }
            if (options.create_ok && !options.str_rx.isEmpty()) {
                IRubyObject	clas = options.rxclass_match(str);

                if (clas != null) rstr = clas.callMethod(context, "json_create", rstr);
            }

            if (parent.val instanceof RubyHash) {
                if (4 == parent.key.realSize() && context.runtime.getTime() == parent.clas && TIME.equals(parent.key)) {
                    if (context.nil == (parent.val = parse_xml_time(str))) {
                        parent.val = context.runtime.getTime().callMethod(context, "parse", context.runtime.newString(str));
                    }
                } else {
                    ((RubyHash) parent.val).fastASet(rkey, rstr);
                }
            } else if (parent.val instanceof RubyObject) {
                ((RubyObject) parent.val).setInstanceVariable(kval.keyAsInstanceVariableId(), value);
            }

            if (options.trace) trace_parse_call("set_string", rstr);
        }
    }

    @Override
    public IRubyObject endHash() {
        Val	parent = stack_peek();

        if (parent.clas != null && parent.clas != parent.val.getMetaClass()) {
            IRubyObject	obj = code_load(parent.clas, (RubyHash) parent.val);

            if (!obj.isNil()) {
                parent.val = obj;
            } else {
                parent.val = parent.clas.callMethod(context, "json_create", parent.val);
            }
            parent.clas = null;
        }

        return super.endHash();
    }

    @Override
    IRubyObject calc_hash_key(Val parent) {
        IRubyObject	rkey = parent.key_val != null ? parent.key_val : context.runtime.newString(parent.key);

        rkey = oj_encode(rkey);

        if (options.sym_key) rkey = context.runtime.newSymbol(((RubyString) rkey).getByteList());

        return rkey;
    }

    @Override
    public void hashSetNum(Val kval, NumInfo ni) {
        Val parent = stack_peek();
        IRubyObject rval = ni.toNumber(context);

        if (parent.val instanceof RubyHash) {
            if (parent.key != null && parent.key.realSize() == 4 && context.runtime.getTime() == parent.clas && TIME.equals(parent.key)) {
                double nsec = ni.num * 1000000000L / ni.div;

                if (ni.neg) {
                    ni.i = -ni.i;
                    if (0 < nsec) {
                        ni.i--;
                        nsec = 1000000000L - nsec;
                    }
                }
                if (86400 == ni.exp) { // UTC time
                    long nsecs = 1000000000L * ni.i + (long) nsec;
                    parent.val = RubyTime.newTimeFromNanoseconds(context.runtime, nsecs);

                    // Since the ruby C routines alway create local time, the
                    // offset and then a convertion to UTC keeps makes the time
                    // match the expected IRubyObject.
                    parent.val = parent.val.callMethod(context, "utc");
                } else if (ni.hasExp) {
                    parent.val = new RubyTime(context.runtime, context.runtime.getTime(),
                            new DateTime(ni.i * 1000, RubyTime.getTimeZone(context.runtime, ni.exp)));
                    ((RubyTime) parent.val).setNSec((long) nsec);
                } else {
                    long nsecs = 1000000000L * ni.i + (long) nsec;
                    parent.val = RubyTime.newTimeFromNanoseconds(context.runtime, nsecs);
                }
                rval = parent.val;
            } else {
                ((RubyHash) parent.val).fastASet(calc_hash_key(kval), rval);
            }

        } else if (parent.val instanceof RubyObject) {
            ((RubyObject) parent.val).setInstanceVariable(kval.keyAsInstanceVariableId(), rval);
        }

        if (options.trace) trace_parse_call("set_num", rval);
    }

    @Override
    public void hashSetValue(Val kval, IRubyObject value) {
        Val	parent = stack_peek();

        if (parent.val instanceof RubyHash) {
            ((RubyHash) parent.val).fastASet(calc_hash_key(kval), value);
        } else if (parent.val instanceof RubyObject) {
            ((RubyObject) parent.val).setInstanceVariable(kval.keyAsInstanceVariableId(), value);
        }

        if (options.trace) trace_parse_call("set_value", value);
    }

    @Override
    public void arrayAppendNum(NumInfo ni) {
        Val parent = stack_peek();
        IRubyObject	rval = ni.toNumber(context);

        ((RubyArray) parent.val).append(rval);
        if (options.trace) trace_parse_call("append_number", rval);
    }

    @Override
    public void arrayAppendCStr(ByteList str, int orig) {
        IRubyObject	rstr = context.runtime.newString(str);

        rstr = oj_encode(rstr);
        if (options.create_ok && !options.str_rx.isEmpty()) {
            IRubyObject	clas = options.rxclass_match(str);

            if (clas != null) {
                ((RubyArray) stack_peek().val).append(clas.callMethod(context, "json_create", rstr));
                return;
            }
        }
        ((RubyArray) stack_peek().val).append(rstr);
        if (options.trace) trace_parse_call("append_string", rstr);
    }

    // FIXME: Should cache Date/Datetime if they exist
    private boolean code_has(RubyClass clas) {
        return clas == context.runtime.getComplex() ||
                clas == context.runtime.getObject().getConstantAt("Date") ||
                clas == context.runtime.getObject().getConstantAt("DateTime") ||
                clas == context.runtime.getObject().getConstantAt("OpenStruct") ||
                clas == context.runtime.getRange() ||
                clas == context.runtime.getRational() ||
                clas == context.runtime.getRegexp() ||
                clas == context.runtime.getTime();
    }

    private IRubyObject code_load(RubyClass clas, RubyHash hash) {
        if (clas.equals(context.runtime.getComplex())) return load_complex(hash);
        if (clas == context.runtime.getObject().getConstant("Date")) return load_date(hash);
        if (clas == context.runtime.getObject().getConstant("DateTime")) return load_datetime(hash);
        if (clas == context.runtime.getObject().getConstantAt("OpenStruct")) return load_openstruct(hash);
        if (clas == context.runtime.getRange()) return load_range(hash);
        if (clas == context.runtime.getRational()) return load_rational(hash);
        if (clas == context.runtime.getRegexp()) return load_regexp(hash);
        if (clas == context.runtime.getTime()) return load_time(hash);

        throw new RuntimeException("Should not reach end of code_load");
    }

    private IRubyObject load_complex(RubyHash attrs) {
        return RubyComplex.newInstance(context, context.runtime.getComplex(), aref(attrs, "real"), aref(attrs, "imag"));
    }

    // FIXME: Use field for Date/DateTime vs looking up.
    private IRubyObject load_date(RubyHash attrs) {
        IRubyObject s = aref(attrs, "s");

        return s.isNil() ? context.nil : context.runtime.getObject().getClass("Date").callMethod(context, "parse", s);
    }

    private IRubyObject load_datetime(RubyHash attrs) {
        IRubyObject s = aref(attrs, "s");

        return s.isNil() ? context.nil : context.runtime.getObject().getClass("DateTime").callMethod(context, "parse", s);
    }

    private IRubyObject load_openstruct(RubyHash attrs) {
        return context.runtime.getObject().getConstantAt("OpenStruct").callMethod(context, "new", aref(attrs, "table"));
    }

    private IRubyObject load_range(RubyHash attrs) {
        IRubyObject args[] = new IRubyObject[] { aref(attrs, "begin"), aref(attrs, "end"), aref(attrs, "exclude_end?") };

        return context.runtime.getRange().newInstance(context, args, Block.NULL_BLOCK);
    }

    private IRubyObject load_rational(RubyHash attrs) {
        IRubyObject[] args = new IRubyObject[] { aref(attrs, "numerator"), aref(attrs, "denominator") };

        return context.runtime.getObject().callMethod(context, "Rational", args);
    }

    private IRubyObject load_regexp(RubyHash attrs) {
        IRubyObject s = aref(attrs, "s");

        return s.isNil() ? context.nil : context.runtime.getRegexp().callMethod(context, "new", s);
    }

    private IRubyObject load_time(RubyHash attrs) {
        return attrs;
    }

    private IRubyObject aref(RubyHash attrs, String keyName) {
        return attrs.fastARef(key(keyName));
    }

    private IRubyObject key(String keyName) {
        return options.sym_key ? context.runtime.newSymbol(keyName) : context.runtime.newString(keyName);
    }

    // FIXME: determine why ObjectParse is extended here?  This is Parse definition copied here.
    @Override
    public IRubyObject startHash() {
        if (options.trace) trace_parse_call("start_hash");

        return RubyHash.newHash(context.runtime);
    }
}
