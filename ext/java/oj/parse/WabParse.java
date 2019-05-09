package oj.parse;

import oj.Options;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.RubyBasicObject.UNDEF;

public class WabParse extends Parse {
    private IRubyObject wab_uuid_clas, uri_clas;

    public WabParse(ParserSource source, ThreadContext context, Options options, IRubyObject handler) {
        super(source, context, options, handler);

        wab_uuid_clas = UNDEF;
        uri_clas = UNDEF;
    }

    private IRubyObject resolve_uri_class() {
        if (uri_clas == UNDEF) {
            uri_clas = context.runtime.getObject().getConstantAt("URI");
        }

        return uri_clas;
    }

    private IRubyObject resolve_wab_uuid_class() {
        if (wab_uuid_clas == UNDEF) {
            IRubyObject wab = context.runtime.getObject().getConstantAt("WAB");
            if (wab != UNDEF && wab instanceof RubyModule) {
                wab_uuid_clas = ((RubyModule) wab).getConstantAt("UUID");
            }
        }

        return wab_uuid_clas;
    }

    // 123e4567-e89b-12d3-a456-426655440000
    boolean uuid_check(ByteList str) {
        int p = 0;

        for (int i = 0; i < 8; i++, p++) {
            if (Character.digit(str.get(p), 16) == -1) return false;
        }
        p++;
        for (int i = 0; i < 4; i++, p++) {
            if (Character.digit(str.get(p), 16) == -1) return false;
        }
        p++;
        for (int i = 0; i < 4; i++, p++) {
            if (Character.digit(str.get(p), 16) == -1) return false;
        }
        p++;
        for (int i = 0; i < 4; i++, p++) {
            if (Character.digit(str.get(p), 16) == -1) return false;
        }
        p++;
        for (int i = 0; i < 12; i++, p++) {
            if (Character.digit(str.get(p), 16) == -1) return false;
        }

        return true;
    }

    private static ByteList HTTP_COLON_SLASH_SLASH = new ByteList(new byte[] {'h', 't', 't', 'p', ':', '/', '/'});

    private IRubyObject cstr_to_rstr(ByteList str) {
        IRubyObject v = context.nil;
        int len = str.length();

        if (30 == len && '-' == str.get(4) && '-' == str.get(7) && 'T' == str.get(10) && ':' == str.get(13) &&
                ':' == str.get(16)  && '.' == str.get(19) && 'Z' == str.get(29)) {
            if (context.nil != (v = time_parse(str))) {
                return v;
            }
        }
        if (36 == len && '-' == str.get(8) && '-' == str.get(13) && '-' == str.get(18) && '-' == str.get(23) &&
                uuid_check(str)) {
            IRubyObject wabUUIDClass = resolve_wab_uuid_class();
            if (wabUUIDClass != UNDEF) return wabUUIDClass.callMethod(context, "new", context.runtime.newString(str));
        }

        v = context.runtime.newString(str);

        if (7 < len && str.startsWith(HTTP_COLON_SLASH_SLASH)) {
            try {
                IRubyObject wabURIClass = resolve_uri_class();
                if (wabURIClass != UNDEF) return resolve_uri_class().callMethod(context, "parse", v);
            } catch (Exception e) {
                // ignore error
            }
        }

        return oj_encode(v);
    }

    // Assumes positive value
    private int read_num(ByteList str, int p, int len) {
        int v = 0;

        for (; 0 < len; len--, p++) {
            int s = str.get(p);
            if ('0' <= s && s <= '9') {
                v = v * 10 + (s - '0');
            } else {
                return -1;
            }
        }

        return v;
    }

    private static final int TM_YEAR = 0;
    private static final int TM_MON = 1;
    private static final int TM_MDAY = 2;
    private static final int TM_HOUR = 3;
    private static final int TM_MIN = 4;
    private static final int TM_SEC = 5;

    private IRubyObject time_parse(ByteList str) {
        Ruby runtime = context.runtime;
        int year, mon, mday, hour, min, sec;
        boolean neg = false;
        long nsecs = 0;
        int p = 0;

        if ('-' == str.get(p)) {
            p += 1;
            neg = true;
        }

        if (-1 == (year = read_num(str, p, 4))) return context.nil;
        p += 4;

        if (neg) year = -year;

        p++; // '-'
        if (-1 == (mon = read_num(str, p, 2))) return context.nil;
        p += 2;
        p++; // '-'
        if (-1 == (mday = read_num(str, p, 2))) return context.nil;
        p += 2;
        p++; // '-'
        if (-1 == (hour = read_num(str, p, 2))) return context.nil;
        p += 2;
        p++; // '-'
        if (-1 == (min = read_num(str, p, 2))) return context.nil;
        p += 2;
        p++; // '-'
        if (-1 == (sec = read_num(str, p, 2))) return context.nil;
        p += 2;
        p++; // '-'
        for (int i = 9; 0 < i; i--,  p++) {
            int s = str.get(p);
            if ('0' <= s && s <= '9') {
                nsecs = nsecs * 10 + (s - '0');
            } else {
                return context.nil;
            }
        }

        return context.runtime.getTime().callMethod(context, "new", new IRubyObject[] {
                runtime.newFixnum(year), runtime.newFixnum(mon), runtime.newFixnum(mday),
                runtime.newFixnum(hour), runtime.newFixnum(min),
                context.runtime.newFloat((sec + ((double) nsecs) / 1000000000.0)),
                context.runtime.newFixnum(ni.exp)
        }).callMethod(context, "utc");
    }

    @Override
    public void arrayAppendCStr(ByteList value, int orig) {
        IRubyObject rval = cstr_to_rstr(value);

        ((RubyArray) stack_peek().val).push(rval);

        if (options.trace) trace_parse_call("set_value", rval);
    }

    @Override
    public void arrayAppendNum(NumInfo ni) {
        IRubyObject rval = ni.toNumber(context);

        ((RubyArray) stack_peek().val).append(rval);

        if (options.trace) trace_parse_call("append_number", rval);
    }

    @Override
    public void addCStr(ByteList value, int orig) {
        this.value = cstr_to_rstr(value);

        if (options.trace) trace_parse_call("add_string", this.value);
    }

    @Override
    public void addNum(NumInfo ni) {
        value = ni.toNumber(context);

        if (options.trace) trace_parse_call("add_num", value);
    }

    @Override
    public void hashSetCStr(Val parent, ByteList value, int orig) {
        IRubyObject rval = cstr_to_rstr(value);

        ((RubyHash) parent.val).op_aset(context, calc_hash_key(parent), rval);
        if (options.trace) trace_parse_call("set_string", rval);
    }

    @Override
    public void hashSetNum(Val parent, NumInfo ni) {
        if (ni.infinity || ni.nan) parseError("not a number or other value");

        IRubyObject rval = ni.toNumber(context);

        ((RubyHash) parent.val).op_aset(context, calc_hash_key(parent), rval);
        if (options.trace) trace_parse_call("set_number", rval);
    }

    @Override
    public void hashSetValue(Val parent, IRubyObject value) {
        ((RubyHash) parent.val).op_aset(context, calc_hash_key(parent), value);

        if (options.trace) trace_parse_call("set_value", value);
    }

    @Override
    IRubyObject calc_hash_key(Val kval) {
        IRubyObject rkey;

        if (kval.key_val != null) {
            rkey = kval.key_val;
        } else {
            rkey = oj_encode(getRuntime().newString(kval.key));
        }

        // FIXME: Can this ever be anything but a string?
        // FIXME: can we avoid the string creation before converting back to symbol?
        if (rkey instanceof RubyString) {
            rkey = getRuntime().newSymbol(((RubyString) rkey).getByteList());
        }

        return rkey;
    }

    @Override
    public IRubyObject startHash() {
        IRubyObject hash = options.createHash(context);

        if (options.trace) trace_parse_call("start_hash");

        return hash;
    }
}
