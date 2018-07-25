package oj;

import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.Variable;
import org.jruby.util.ByteList;

import java.util.HashMap;
import java.util.Map;

import static oj.Options.No;
import static oj.Options.Yes;

/**
 * Created by enebo on 8/28/15.
 */
public class ObjectParse extends Parse {
    private RubyClass structClass;
    private RubyClass timeClass;

    // In C this is in hash.c
    private Map<ByteList, RubyClass> classMap = new HashMap<>();

    public ObjectParse(ThreadContext context, Options options) {
        super(context, options, null);

        structClass = context.runtime.getStructClass();
        timeClass = context.runtime.getTime();
    }

    /**
     * Read from value at start index for a long value.
     */
    private long read_long(ByteList value, int start) {
        long	n = 0;
        int len = value.realSize() - start;

        for (int i = start; 0 < len; len--) {
            int c = value.get(i);
            if ('0' <= c && c <= '9') {
                n = n * 10 + (c - '0');
            } else {
                return -1;
            }
        }
        return n;
    }

    private IRubyObject calc_hash_key(Val kval, byte k1) {
        IRubyObject	rkey;

        if (':' == k1) {
            rkey = keyIsSym(kval.key);
        } else {
            rkey = context.runtime.newString(kval.key);
            rkey = oj_encode(rkey);
            if (Yes == options.sym_key) {
                // FIXME: This may be a common snippet and we need nicer method for making symbol maybe on Parse.
                if (rkey instanceof RubyString) {
                    return getRuntime().newSymbol(((RubyString) rkey).getByteList());
                } else if (!(rkey instanceof RubySymbol) && rkey.respondsTo("to_sym")) {
                    return Helpers.invoke(context, rkey, "to_sym");
                }
            }
        }
        return rkey;
    }

    private IRubyObject keyIsSym(ByteList value) {
        IRubyObject rkey = context.runtime.newString(value.makeShared(1, value.length() - 1));
        rkey = oj_encode(rkey);
        return rkey.callMethod(context, "to_sym");
    }

    private IRubyObject str_to_value(ByteList value, int orig) {
        int len = value.length();
        int c = at(orig);

        if (':' == c && 0 < len) {
            return keyIsSym(value);
        } else if (circ_array != null && 3 <= len && '^' == c && 'r' == at(orig + 1)) {
            int i = (int) read_long(value, 2);

            if (0 > i) {
                parseError("not a valid ID number");
                return context.nil;
            }
            return circ_array.get(i);
        } else {
            return oj_encode(context.runtime.newString(value));
        }
    }

    // The much faster approach (4x faster)
    static int parse_num(ByteList value, int str, int cnt) {
        int		n = 0;
        int	c;
        int		i;
        int end = value.realSize();

        for (i = cnt; 0 < i; i--, str++) {
            if (end <= str) {
                return -1;
            }
            c = value.get(i);
            if (c < '0' || '9' < c) {
                return -1;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }

    IRubyObject parse_xml_time(ByteList value) {
        IRubyObject[] args = new IRubyObject[8];
        int n;
        int str = 0;

        // year
        if (0 > (n = parse_num(value, str, 4))) {
            return context.nil;
        }
        str += 4;
        args[0] = context.runtime.newFixnum(n);
        if ('-' != value.get(str)) {
            return context.nil;
        }
        str++;

        // month
        if (0 > (n = parse_num(value, str, 2))) {
            return context.nil;
        }
        str += 2;
        args[1] = context.runtime.newFixnum(n);
        if ('-' != value.get(str)) {
            return context.nil;
        }
        str++;

        // day
        if (0 > (n = parse_num(value, str, 2))) {
            return context.nil;
        }
        str += 2;
        args[2] = context.runtime.newFixnum(n);
        if ('T' != value.get(str)) {
            return context.nil;
        }
        str++;

        // hour
        if (0 > (n = parse_num(value, str, 2))) {
            return context.nil;
        }
        str += 2;
        args[3] = context.runtime.newFixnum(n);
        if (':' != value.get(str)) {
            return context.nil;
        }
        str++;

        // minute
        if (0 > (n = parse_num(value, str, 2))) {
            return context.nil;
        }
        str += 2;
        args[4] = context.runtime.newFixnum(n);
        if (':' != value.get(str)) {
            return context.nil;
        }
        str++;

        // second
        if (0 > (n = parse_num(value, str, 2))) {
            return context.nil;
        }
        str += 2;
        if (str == value.realSize()) {
            args[5] = context.runtime.newFixnum(n);
            args[6] = context.runtime.newFixnum(0);
        } else {
            int end = value.realSize();
            int c = value.get(str);
            str++;

            if ('.' == c) {
                long nsec = 0;

                for (; str < end; str++) {
                    c = value.get(str);
                    if (c < '0' || '9' < c) {
                        str++;
                        break;
                    }
                    nsec = nsec * 10 + (c - '0');
                }
                args[5] = context.runtime.newFloat((double)n + ((double)nsec + 0.5) / 1000000000.0);
            } else {
                args[5] = context.runtime.newFixnum(n);
            }
            if (end < str) {
                args[6] = context.runtime.newFixnum(0);
            } else {
                if ('Z' == c) {
                    return timeClass.callMethod(context, "utc", args);
                } else if ('+' == c) {
                    int	hr = parse_num(value, str, 2);
                    int	min;

                    str += 2;
                    if (0 > hr || ':' != value.get(str)) {
                        return context.nil;
                    }
                    str++;

                    min = parse_num(value, str, 2);
                    if (0 > min) {
                        return context.nil;
                    }
                    args[6] = context.runtime.newFixnum(hr * 3600 + min * 60);
                } else if ('-' == c) {
                    int	hr = parse_num(value, str, 2);
                    int	min;

                    str += 2;
                    if (0 > hr || ':' != value.get(str)) {
                        return context.nil;
                    }
                    str++;

                    min = parse_num(value, str, 2);
                    if (0 > min) {
                        return context.nil;
                    }
                    args[6] = context.runtime.newFixnum(-(hr * 3600 + min * 60));
                } else {
                    args[6] = context.runtime.newFixnum(0);
                }
            }
        }
        return timeClass.callMethod(context, "new", args);
    }

    boolean hat_cstr(Val parent, Val kval, ByteList value) {
        ByteList key = kval.key;
        int klen = key.realSize();

        if (2 == klen) {
            switch (key.get(1)) {
                case 'o': // object
                {	// name2class sets and error if the class is not found or created
                    RubyClass	clas = nameToClass(value, Yes == options.auto_define);

                    if (null != clas) {
                        parent.val = clas.allocate();
                    }
                }
                break;
                case 'O': // odd object
                {
                    // FIXME: Handle odd types...
                    /*
                    Odd	odd = oj_get_oddc(value);

                    if (null == odd) {
                        return false;
                    }
                    parent.val = odd.clas;
                    parent.oddArgs = oj_odd_alloc_args(odd);
                    */
                }
                break;
                case 'm':
                    parent.val = keyIsSym(value);
                    break;
                case 's':
                    parent.val = oj_encode(context.runtime.newString(value));
                    break;
                case 'c': // class
                {
                    IRubyObject	clas = nameToClass(value, Yes == options.auto_define);

                    if (null == clas) {
                        return false;
                    } else {
                        parent.val = clas;
                    }
                }
                break;
                case 't': // time
                    parent.val = parse_xml_time(value);
                    break;
                default:
                    return false;
            }
            return true; // handled
        }
        return false;
    }

    boolean hat_num(Val parent, Val kval, NumInfo ni) {
        if (2 == kval.key.realSize()) {
            switch (kval.key.get(1)) {
                case 't': // time as a float
                {
                    System.err.println("T: ");
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
                        /*
                        time_t	t = (time_t)(ni.i + ni.exp);
                        struct tm	st = gmtime(t);

                        IRubyObject[] args = new IRubyObject[8];

                        args[0] = context.runtime.newFixnum(1900 + st.tm_year);
                        args[1] = context.runtime.newFixnum(1 + st.tm_mon);
                        args[2] = context.runtime.newFixnum(st.tm_mday);
                        args[3] = context.runtime.newFixnum(st.tm_hour);
                        args[4] = context.runtime.newFixnum(st.tm_min);
                        args[5] = context.runtime.newFloat((double) (st.tm_sec + ((double)nsec) / 1000000000.0));
                        args[6] = context.runtime.newFixnum(ni.exp);
                        parent.val = timeClass.callMethod("new", args);
                        */
                        // FIXME: Can this be this simple?
                        long nsecs = 1000000000L * (ni.i + ni.exp) + (long) nsec;
                        parent.val = RubyTime.newTimeFromNanoseconds(context.runtime, nsecs);
                    } else {
                        long nsecs = 1000000000L * ni.i + (long) nsec;
                        parent.val = RubyTime.newTimeFromNanoseconds(context.runtime, nsecs);
                    }
                }
                break;
                case 'i': // circular index
                    if (!ni.infinity && !ni.neg && 1 == ni.div && 0 == ni.exp && circ_array != null) { // fixnum
                        if (context.nil == parent.val) {
                            parent.val = RubyHash.newHash(context.runtime);
                        }
                        circ_array.set(ni.i, parent.val);
                    } else {
                        return false;
                    }
                    break;
                default:
                    return false;
            }
            return true; // handled
        }
        return false;
    }

    boolean hat_value(Val parent, ByteList key, IRubyObject value) {
        if (value instanceof RubyArray) {
            RubyArray e1 = (RubyArray) value;
            int	len = e1.size();
            int klen = key.length();

            if (2 == klen && 'u' == key.get(1)) {
                IRubyObject	sc;

                if (0 == len) {
                    parseError("Invalid struct data");
                    return true;
                }

                // FIXME: There was anonymous and non-anonymous struct check here I did not understand.
                IRubyObject[] args = new IRubyObject[1024];
                IRubyObject	rstr;
                int	i, cnt = e1.size();

                for (i = 0; i < cnt; i++) {
                    rstr = e1.eltInternal(i);
                    args[i] = rstr.callMethod(context, "to_sym");
                }
                sc = structClass.callMethod(context, "new", args);
                // Create a properly initialized struct instance without calling the initialize method.
                parent.val = sc;

                return true;
            } else if (3 <= klen && '#' == key.get(1)) {
                IRubyObject	a;

                if (2 != len) {
                    parseError("invalid hash pair");
                    return true;
                }
                parent.val = RubyHash.newHash(context.runtime);
                ((RubyHash) parent.val).op_aset(context, e1.eltInternal(0), e1.eltInternal(1));

                return true;
            }
        }
        return false;
    }

    void copy_ivars(IRubyObject target, IRubyObject src) {
        // Note: this is not calling instance_variables as dynamic call
        for (Variable a: src.getVariableList()) {
            ((RubyObject) target).setInstanceVariable(a.getName(), (IRubyObject) a.getValue());
        }
    }

    void set_obj_ivar(Val parent, Val kval, IRubyObject value) {
        ByteList key = kval.key;
        int		klen = key.length();

        if ('~' == key.get(0) && parent.val instanceof RubyException) {
            if (5 == klen && 0 == key.toString().compareToIgnoreCase("~mesg")) {
                IRubyObject	prev = parent.val;

                // FIXME: Not sure this will work but seems like dup would work no?
                parent.val = ((RubyException) parent.val).exception(new IRubyObject[] { value });
                copy_ivars(parent.val, prev);
            } else if (3 == klen && 0 == key.toString().compareToIgnoreCase("~bt")) {
                parent.val.callMethod(context, "set_backtrace", value);
            }
        }

        RubyClass clazz = classMap.get(key);
        ByteList var_id = null;
        if (clazz == null) {
            if ('~' == key.get(0)) {
                var_id = key.makeShared(1, key.realSize() - 1);
            } else {
                // FIXME: hacky aroundy
                var_id = new ByteList(new byte[]{'@'});
                var_id.append(key);
            }
        } else {
            if ('~' == key.get(0)) {
                var_id = key.makeShared(1, key.realSize() - 1);
            } else {
                // FIXME: hacky aroundy
                var_id = new ByteList(new byte[]{'@'});
                var_id.append(key);
            }
        }

        // FIXME: toString does not really work here
        ((RubyObject) parent.val).setInstanceVariable(var_id.toString(), value);
    }

    @Override
    public void setCStr(Val kval, ByteList value, int orig) {
        ByteList key = kval.key;
        int klen = kval.key.realSize();
        Val	parent = stack_peek();

        while (parent.val != null) {
            if (parent.val instanceof RubyNil) {
                parent.oddArgs = null; // make sure it is 0 in case not odd
                if ('^' != key.get(0) || !hat_cstr(parent, kval, value)) {
                    parent.val = RubyHash.newHash(context.runtime);
                    continue;
                }
                break;
            } else if (parent.val instanceof RubyHash) {
                ((RubyHash) parent.val).op_aset(context, calc_hash_key(kval, parent.k1), str_to_value(value, orig));
                break;
            } else if (parent.val instanceof RubyString) {
                if (4 == klen && 's' == key.get(0) && 'e' == key.get(1) && 'l' == key.get(2) && 'f' == key.get(3)) {
                    parent.val.callMethod(context, "replace_id", str_to_value(value, orig));
                } else {
                    set_obj_ivar(parent, kval, str_to_value(value, orig));
                }
                break;
            } else if (parent.val instanceof RubyClass) {
                if (null == parent.oddArgs) {
                    parseError(parent.val.getMetaClass().getName() + " is not an odd class");
                    return;
                } else if (!parent.oddArgs.setArg(kval.key, str_to_value(value, orig))) {
                    parseError(key + " is not an attribute of " + parent.val.getMetaClass().getName());
                }
                break;
            } else if (parent.val instanceof RubyObject) {
                set_obj_ivar(parent, kval, str_to_value(value, orig));
                break;
            } else {
                parseError("can not add attributes to a " + parent.val.getMetaClass().getName());
                return;
            }
        }
    }

    @Override
    public void setNum(Val kval, NumInfo ni) {
    ByteList key = kval.key;
        int		klen = kval.key.realSize();
        Val		parent = stack_peek();


        System.out.println("SETNUM");
        while (parent.val != null) {
            if (parent.val instanceof RubyNil) {
                parent.oddArgs = null; // make sure it is 0 in case not odd
                if ('^' != key.get(0) || !hat_num(parent, kval, ni)) {
                    parent.val = RubyHash.newHash(context.runtime);
                    continue;
                }
            } else if (parent.val instanceof RubyHash) {
                ((RubyHash) parent.val).op_aset(context, calc_hash_key(kval, parent.k1), ni.toNumber(context));
                break;
            } else if (parent.val instanceof RubyClass) {
                if (null == parent.oddArgs) {
                    parseError(parent.val.getMetaClass().getName() + " is not an odd class");
                    return;
                } else if (!parent.oddArgs.setArg(key, ni.toNumber(context))) {
                    parseError(key + " is not an attribute of " + parent.val.getMetaClass().getName());
                }
                break;
            } else if (parent.val instanceof RubyObject) {
                if (2 == klen && '^' == key.get(0) && 'i' == key.get(1) &&
                        !ni.infinity && !ni.neg && 1 == ni.div && 0 == ni.exp && circ_array != null) { // fixnum
                    circ_array.set(ni.i, parent.val);
                } else {
                    set_obj_ivar(parent, kval, ni.toNumber(context));
                }
                break;
            } else {
                parseError("can not add attributes to a " + parent.val.getMetaClass().getName());
                return;
            }
        }
    }

    @Override
    public void setValue(Val kval, IRubyObject value) {
        ByteList key = kval.key;
        int		klen = key.length();
        Val		parent = stack_peek();

        while (parent.val != null) {
            if (parent.val instanceof RubyNil) {
                parent.oddArgs = null; // make sure it is 0 in case not odd
                if ('^' != key.get(0) || !hat_value(parent, key, value)) {
                    parent.val = RubyHash.newHash(context.runtime);
                    continue;
                }
                break;
            } else if (parent.val instanceof RubyHash) {
                /* Note: Not sure what case this could be?  T_HASH which is not actually of type Hash?
                if (rb_cHash != rb_obj_class(parent.val)) {
                    if (4 == klen && 's' == key.get(0) && 'e' == key.get(1) && 'l' == key.get(2) && 'f' == key.get(3)){
                        parent.val.callMethod(context, "replace", value);
                    } else{
                        set_obj_ivar(parent, kval, value);
                    }
                } else {*/
                    if (3 <= klen && '^' == key.get(0) && '#' == key.get(1) && value instanceof RubyArray) {
                        RubyArray array = (RubyArray) value;

                        if (2 != array.size()) {
                            parseError("invalid hash pair");
                            return;
                        }
                        ((RubyHash) parent.val).op_aset(context, array.eltInternal(0), array.eltInternal(1));
                    } else{
                        ((RubyHash) parent.val).op_aset(context, calc_hash_key(kval, parent.k1), value);
                    }
                //}
                break;
            } else if (parent.val instanceof RubyArray) {
                if (4 == klen && 's' == key.get(0) && 'e' == key.get(1) && 'l' == key.get(2) && 'f' == key.get(3)) {
                    parent.val.callMethod(context, "replace", value);
                } else {
                    set_obj_ivar(parent, kval, value);
                }
                break;
            } else if (parent.val instanceof RubyClass) {
                if (null == parent.oddArgs) {
                    parseError(parent.val.getMetaClass().getName() + " is not an odd class");
                    return;
                } else if (!parent.oddArgs.setArg(key, value)) {
                    parseError(key +  " is not an attribute of " + parent.val.getMetaClass().getName());
                }
                break;
                // FIXME: Fairly sure T_OBJECT is ruby code and not any type at all
            } else if (parent.val instanceof RubyObject) {
                set_obj_ivar(parent, kval, value);
                break;
            } else {
                parseError("can not add attributes to a " + parent.val.getMetaClass().getName());
                return;
            }
        }
    }

    @Override
    public IRubyObject endHash() {
        Val	parent = stack_peek();

        if (context.nil == parent.val) {
            parent.val = RubyHash.newHash(context.runtime);
        } else if (null != parent.oddArgs) {
            OddArgs	oa = parent.oddArgs;

            parent.val = oa.odd.createObj.callMethod(context,  oa.odd.createOp, oa.args);
            parent.oddArgs = null;
        }

        return super.endHash();
    }

    @Override
    public void arrayAppendCStr(ByteList value, int orig) {
        int len = value.length();

        if (3 <= len && circ_array != null) {
            if ('i' == at(orig + 1)) {
                int i = (int) read_long(value, 2);

                if (0 < i) {
                    circ_array.set(i, stack_peek().val);
                    return;
                }
            } else if ('r' == at(orig + 1)) {
                int i = (int) read_long(value, 2);

                if (0 < i) {
                    ((RubyArray) stack_peek().val).push(circ_array.get(i));
                    return;
                }

            }
        }
        ((RubyArray) stack_peek().val).append(str_to_value(value, orig));
    }

    @Override
    public void appendNum(NumInfo ni) {
        ((RubyArray) stack_peek().val).append(ni.toNumber(context));
    }

    @Override
    public void addCStr(ByteList value, int orig) {
        this.value = str_to_value(value, orig);
    }

    @Override
    public void addNum(NumInfo ni) {
        value = ni.toNumber(context);
    }

    private RubyClass nameToClass(ByteList name, boolean autoDefine) {
        if (options.class_cache == No) {
            return resolveClassPath(name, autoDefine);
        }

        RubyClass clas = classMap.get(name);
        if (clas == null) {
            clas = resolveClassPath(name, autoDefine);
        }

        return clas;
    }

    private RubyClass resolveClassPath(ByteList className, boolean autoDefine) {
        RubyModule clas = context.runtime.getObject();

        int length = className.realSize();
        ByteList name = className;
        for (int index = name.indexOf(':'); index != -1 && index + 1 < name.realSize(); index = name.indexOf(':', index + 1)) {
            if (name.get(index + 1) != ':') {
                return null;
            }
            ByteList baseName = name.makeShared(0, index);
            index++; // skip past second ':'
            name = name.makeShared(index, name.realSize() - index);
            // FIXME: I think 'Foo::' may be broken?

            clas = resolveClassName(clas, baseName, autoDefine);

            if (clas == null) {
                return null;
            }
        }

        clas = resolveClassName(clas, name, autoDefine);
        if (clas == null) {
            parseError("class" + className + "is not defined");
        }

        // FIXME: This probably isn't always true...error case
        return (RubyClass) clas;
    }

    private RubyModule resolveClassName(RubyModule base, ByteList name, boolean autoDefine) {
        // FIXME: m17n issue
        IRubyObject clas = base.getConstantAt(name.toString());
        if (clas == null || autoDefine) {
            // FIXME: This should be oj Bag type
            clas = context.runtime.getHash();
        }


        return (RubyClass) clas;
    }
}
