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
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

import java.util.List;

import static oj.LeafType.T_ARRAY;
import static oj.LeafType.T_HASH;
import static oj.LeafValue.COL_VAL;

/**
 * Created by enebo on 8/3/18.
 */
public class Doc extends RubyObject {
    public final int MAX_STACK = 100;
    public long size = 0;
    Leaf[] wheres = new Leaf[MAX_STACK];
    int where = 0;
    int where_path = 0;
    Leaf data;
    IRubyObject self;
    IRubyObject slash;

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Doc(runtime, klass);
        }
    };

    public static void createDocClass(Ruby runtime, RubyModule oj) {
        RubyClass clazz = oj.defineClassUnder("Doc", runtime.getObject(), ALLOCATOR);
        clazz.setInternalVariable("_oj", oj);
        clazz.defineAnnotatedMethods(Doc.class);
    }

    public Doc(ThreadContext context) {
        this(context.runtime, (RubyClass) ((RubyModule) context.runtime.getObject().getConstantAt("Oj")).getConstantAt("Doc"));
    }

    public Doc(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);

        self = null;
        slash = runtime.newString("/");
//        doc->batches = &doc->batch0;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject self, IRubyObject str, Block block) {
        ByteList json = ((RubyString) TypeConverter.checkStringType(context.runtime, str)).getByteList();

        return new FastParse(context, json).parse_json(block);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject open_file(ThreadContext context, IRubyObject self, IRubyObject filename, Block block) {
        String path = TypeConverter.checkStringType(context.runtime, filename).asJavaString();
        ByteList json = FileParserSource.readFileIntoByteList(context, path);

        return new FastParse(context, json).parse_json(block);
    }

    @JRubyMethod(name = "where?")
    public IRubyObject where_p(ThreadContext context) {
        if (null == wheres[where_path] || where == where_path) {
            return slash;
        } else {
            int	lp;
            int	size = 2; // leading / + ???

            // FIXME: try and remove this calculation and see how poorly bytelist autosizing works or even just waste and speculatively overalloc a little bit.
            for (lp = where_path; lp <= where; lp++) {
                Leaf leaf = wheres[lp];
                if (ParentType.Hash == leaf.parentType) {
                    size += leaf.key.realSize() + 1;
                } else if (ParentType.Array == leaf.parentType) {
                    size += leaf.index < 100 ? 3 : 11;
                }
            }

            ByteList path = new ByteList(size);
            path.setEncoding(UTF8Encoding.INSTANCE);
            boolean first = true;
            for (lp = where_path; lp <= where; lp++) {
                if (!first) path.append('/');
                Leaf leaf = wheres[lp];
                if (ParentType.Hash == leaf.parentType) {
                    path.append(leaf.key);
                } else if (ParentType.Array == leaf.parentType) {
                    ulong_fill(path, leaf.index);
                }
                first = false;
            }

            return context.runtime.newString(path);
        }
    }

    @JRubyMethod
    public IRubyObject local_key(ThreadContext context) {
        Leaf leaf = wheres[where];
        IRubyObject	key = context.nil;

        if (ParentType.Hash == leaf.parentType) {
            key = Parse.oj_encode(context.runtime.newString(leaf.key));
        } else if (ParentType.Array == leaf.parentType) {
            key = context.runtime.newFixnum(leaf.index);
        }
        return key;
    }

    @JRubyMethod
    public IRubyObject home(ThreadContext context) {
        wheres[where_path] = data;
        where = where_path;

        return slash;
    }

    @JRubyMethod
    public IRubyObject type(ThreadContext context) {
        return docTypeCommon(context, null);
    }

    @JRubyMethod
    public IRubyObject type(ThreadContext context, IRubyObject pathArg) {
        return docTypeCommon(context, asPath(context, pathArg));
    }

    private ByteList asPath(ThreadContext context, IRubyObject pathArg) {
        return ((RubyString) TypeConverter.checkStringType(context.runtime, pathArg)).getByteList();
    }

    private IRubyObject docTypeCommon(ThreadContext context, ByteList path) {
        Leaf leaf = get_doc_leaf(context, path);

        if (leaf == null) return context.nil;

        // FIXME: type can just be RubyClass reference and we can use these types directly and eliminate a switch.
        Ruby runtime = context.runtime;
        switch (leaf.rtype) {
            case T_NIL: return runtime.getNilClass();
            case T_TRUE: return runtime.getTrueClass();
            case T_FALSE: return runtime.getFalseClass();
            case T_STRING: return runtime.getString();
            case T_FIXNUM: return runtime.getFixnum();
            case T_FLOAT: return runtime.getFloat();
            case T_ARRAY: return runtime.getArray();
            case T_HASH: return runtime.getHash();
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context) {
        Leaf leaf = get_doc_leaf(context, null);
        return leaf != null ?  leaf.value(context) : context.nil;
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject pathArg) {
        return fetch(context, pathArg, context.nil);
    }

    @JRubyMethod
    public IRubyObject fetch(ThreadContext context, IRubyObject pathArg, IRubyObject value) {
        Leaf leaf = get_doc_leaf(context, asPath(context, pathArg));
        return leaf != null ?  leaf.value(context) : value;
    }

    @JRubyMethod(rest=true)
    public IRubyObject each_leaf(ThreadContext context, IRubyObject[] argv, Block block) {
        if (block.isGiven()) {
            Leaf[] save_path = new Leaf[MAX_STACK];
            ByteList path = null;
            int		wlen;

            wlen = where - where_path;
            if (0 < wlen) {
                System.arraycopy(wheres, 0, save_path, 0, wlen + 1);
            }
            if (1 <= argv.length) {
                path = asPath(context, argv[0]);
                if ('/' == path.get(0)) {
                    where = where_path;
                    path = path.makeShared(1, path.realSize() - 1);
                }
                if (0 != move_step(path, 1)) {
                    if (0 < wlen) {
                        System.arraycopy(save_path, 0, wheres, 0, wlen + 1);
                    }
                    return context.nil;
                }
            }
            each_leaf_inner(context, this, block);
            if (0 < wlen) {
                System.arraycopy(save_path, 0, where, 0, wlen + 1);
            }
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject move(ThreadContext context, IRubyObject str) {
        ByteList path = asPath(context, str);

        if ('/' == path.get(0)) {
            where = where_path;
            path = path.makeShared(1, path.realSize() - 1);
        }

        int loc = move_step(path, 1);
        if (loc != 0) {
            throw context.runtime.newArgumentError("Failed to locate element " + loc + " of the path " + path + ".");
        }

        return context.nil;
    }

    @JRubyMethod(rest=true)
    public IRubyObject each_child(ThreadContext context, IRubyObject[] argv, Block block) {
        if (block.isGiven()) {
            Leaf[] save_path = new Leaf[MAX_STACK];
            ByteList path = null;
            int		wlen;

            wlen = where - where_path;
            if (0 < wlen) {
                System.arraycopy(wheres, 0, save_path, 0, wlen + 1);
            }
            if (1 <= argv.length) {
                path = asPath(context, argv[0]);
                if ('/' == path.get(0)) {
                    where = where_path;
                    path = path.makeShared(1, path.realSize() - 1);
                }
                if (0 != move_step(path, 1)) {
                    if (0 < wlen) {
                        System.arraycopy(save_path, 0, wheres, 0, wlen + 1);
                    }
                    return context.nil;
                }
            }
            if (COL_VAL == wheres[where].value_type && null != wheres[where].elements) {
                List<Leaf> elements = wheres[where].elements;
                where++;
                for (Leaf e: elements) {
                    wheres[where] = e;
                    block.yield(context, this);
                }
            }
            if (0 < wlen) {
                System.arraycopy(save_path, 0, wheres, 0, wlen + 1);
            }
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject each_value(ThreadContext context, Block block) {
        if (block.isGiven()) {
            Leaf leaf = get_doc_leaf(context, null);
            if (leaf != null) leaf.each_value(context, block);
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject each_value(ThreadContext context, IRubyObject pathArg, Block block) {
        if (block.isGiven()) {
            Leaf leaf = get_doc_leaf(context, asPath(context, pathArg));
            if (leaf != null) leaf.each_value(context, block);
        }

        return context.nil;
    }

    @JRubyMethod(rest = true)
    public IRubyObject dump(ThreadContext context, IRubyObject[] argv) {
        ByteList path = null;
        String filename = null;

        if (1 <= argv.length) {
            if (context.nil != argv[0]) path = asPath(context, argv[0]);
            if (2 <= argv.length) path = asPath(context, argv[1]);
        }

        Leaf leaf = get_doc_leaf(context, path);
        if (leaf != null) {
            OjLibrary oj = RubyOj.resolveOj(this);
            if (null == filename) {
                Out out = Dump.leaf_to_json(context, oj, leaf, OjLibrary.getDefaultOptions());
                return context.runtime.newString(out.buf);
            } else {
                Dump.leaf_to_file(context, oj, leaf, filename, OjLibrary.getDefaultOptions());
            }
        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        return context.runtime.newFixnum(size);
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        return context.nil;
    }

    Leaf get_doc_leaf(ThreadContext context, ByteList path) {
        Leaf leaf = wheres[where];

        if (null != data && null != path) {
            Leaf[] stack = new Leaf[MAX_STACK];
            int lp = 0;

            if ('/' == path.get(0)) {
                path = path.makeShared(1, path.realSize() - 1);
                stack[0] = data;
            } else if (where == where_path) {
                stack[0] = data;
            } else {
                int cnt = where - where_path;

                stackDepthCheck(context, cnt);
                System.arraycopy(wheres, 0, stack, 0, cnt + 1);
                lp = cnt;
            }
            return get_leaf(context, stack, lp, path);
        }
        return leaf;
    }

    private void stackDepthCheck(ThreadContext context, int cnt) {
        if (MAX_STACK <= cnt) {
            RubyClass error = (RubyClass) ((RubyModule) context.runtime.getObject().getConstant("Oj")).getConstantAt("DepthError");

            throw context.runtime.newRaiseException(error, "\"Path too deep. Limit is " + MAX_STACK + " levels.");
        }
    }

    // FIXME: This substring stuff is pretty inefficient
    Leaf get_leaf(ThreadContext context, Leaf[] stack, int lp, ByteList path) {
        Leaf leaf = stack[lp];

        stackDepthCheck(context, lp);
        if (ByteList.EMPTY_BYTELIST.equals(path)) return leaf;

        if ('.' == path.get(0) && '.' == path.get(1)) {
            // FIXME: rescanning past ..
            int slash = path.indexOf('/');
            if (-1 == slash) {
                path = ByteList.EMPTY_BYTELIST;
            } else {
                path = path.makeShared(slash + 1, path.realSize() - slash - 1);
            }
            /*int skip = 2;
            if ('/' == path.get(skip)) {
                skip++;
            }*/
            if (lp > 0) {
                leaf = get_leaf(context, stack, lp - 1, path);
            } else {
                return null;
            }
        } else if (COL_VAL == leaf.value_type && null != leaf.elements) {
            LeafType type = leaf.rtype;
            List<Leaf> elements = leaf.elements;

            leaf = null;
            if (T_ARRAY == type) {
                int	cnt = 0;
                int i = 0;

                for (int c = path.get(i); '0' <= c && c <= '9'; i++, c = path.get(i + 1)) {
                    cnt = cnt * 10 + (c - '0');
                }

                // FIXME: we are rescanning all the numbers again.
                int slash = path.indexOf('/');
                if (-1 == slash) {
                    path = ByteList.EMPTY_BYTELIST;
                } else {
                    path = path.makeShared(slash + 1, path.realSize() - slash - 1);
                }

                lp++;
                stack[lp] = elements.get(cnt - 1);
                leaf = get_leaf(context, stack, lp, path);
            } else if (T_HASH == type) {
                ByteList key;
                int slash = path.indexOf('/');

                if (-1 == slash) {
                    key = path;
                    path = ByteList.EMPTY_BYTELIST;

                } else {
                    key = path.makeShared(0, slash);
                    path = path.makeShared(slash + 1, path.realSize() - slash - 1);
                }

                for (Leaf e: elements) {
                    if (key.equals(e.key)) {
                        lp++;
                        stack[lp] = e;
                        leaf = get_leaf(context, stack, lp, path);
                        break;
                    }
                }
            }
        }

        return leaf;
    }


    void each_leaf_inner(ThreadContext context, IRubyObject self, Block block) {
        if (COL_VAL == wheres[where].value_type) {
            if (wheres[where].hasElements()) {
                List<Leaf> elements = wheres[where].elements;

                where++;
                for (Leaf e: elements) {
                    wheres[where] = e;
                    each_leaf_inner(context, self, block);
                }
                where--;
            }
        } else {
            block.yield(context, self);
        }
    }

    int move_step(ByteList path, int loc) {
        if (ByteList.EMPTY_BYTELIST.equals(path)) {
            loc = 0;
        } else {
            Leaf leaf = wheres[where];

            if (leaf == null) {
                System.err.println("*** Internal error at " + path);
                return loc;
            }
            if ('.' == path.get(0) && '.' == path.get(1)) {
                Leaf init = wheres[where];

                int skip = 2;
                if (where == where_path) {
                    return loc;
                }
                if ('/' == path.get(2)) {
                    skip++;
                }
                wheres[where] = null;
                where--;
                path = path.makeShared(skip, path.realSize() - skip);
                loc = move_step(path, loc + 1);
                if (0 != loc) {
                    wheres[where] = init;
                    where++;
                }
            } else if (COL_VAL == leaf.value_type && null != leaf.elements) {
                if (T_ARRAY == leaf.rtype) {
                    int	cnt = 0;
                    int i = 0;
                    for (int c = path.get(i); '0' <= c && c <= '9'; i++, c = path.get(i)) {
                        cnt = cnt * 10 + (c - '0');
                    }

                    if (path.get(i) == '/') {
                        path = path.makeShared(i + 1, path.realSize() - i - 1);
                    } else if (i < path.realSize() - 1 || cnt == 0) { // random chars after digits or no digits at all...
                        return loc;
                    } else {
                        path = ByteList.EMPTY_BYTELIST;
                    }

                    where++;
                    wheres[where] = leaf.elements.get(cnt - 1);
                    loc = move_step(path, loc + 1);
                    if (0 != loc) {
                        wheres[where] = null;
                        where--;
                    }
                } else if (T_HASH == leaf.rtype) {
                    ByteList key;
                    int slash = path.indexOf('/');

                    if (-1 == slash) {
                        key = path;
                        path = ByteList.EMPTY_BYTELIST;

                    } else {
                        key = path.makeShared(0, slash);
                        path = path.makeShared(slash + 1, path.realSize() - slash - 1);
                    }

                    for (Leaf e: leaf.elements) {
                        if (key.equals(e.key)) {
                            where++;
                            wheres[where] = e;
                            loc = move_step(path, loc + 1);
                            if (0 != loc) {
                                wheres[where] = null;
                                where--;
                            }
                            break;
                        }
                    }
                }
            }
        }
        return loc;
    }

    static void ulong_fill(ByteList value, int num) {
        byte[] buf = new byte[11];
        int b = buf.length - 1;

        for (; 0 < num; num /= 10, b--) {
            buf[b] = (byte) ((num % 10) + '0');
        }
        if (b == buf.length - 1) {
            buf[b] = '0';
            b--;
        }

        int realLength = buf.length - b - 1;
        value.append(buf, b + 1, realLength);
    }
}