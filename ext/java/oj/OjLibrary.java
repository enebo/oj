package oj;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import java.util.List;

/**
 * Created by enebo on 8/28/15.
 */
public class OjLibrary implements Library {
    public static Options default_options;
    private RubyClass parseError;
    public RubyClass stringWriter;
    public RubyModule oj;
    public RubyClass bag;
    private List<Odd> odds;

    public void load(Ruby runtime, boolean wrap) {
        RubyKernel.require(runtime.getKernel(), runtime.newString("time"), Block.NULL_BLOCK);
        RubyKernel.require(runtime.getKernel(), runtime.newString("date"), Block.NULL_BLOCK);
        RubyKernel.require(runtime.getKernel(), runtime.newString("oj"), Block.NULL_BLOCK);
        oj = runtime.getOrCreateModule("Oj");
        oj.defineAnnotatedMethods(RubyOj.class);

        ThreadContext context = runtime.getCurrentContext();

        default_options = new Options(context);

        oj.setInternalVariable("_oj", this);

        parseError = (RubyClass) oj.getConstant("ParseError");

        stringWriter = StringWriter.createStringWriterClass(runtime, oj);
        StreamWriter.createStreamWriterClass(runtime, oj);
        Doc.createDocClass(runtime, oj);

        odds = Odd.initBuiltinOdds(runtime);

        RubyKernel.require(runtime.getKernel(), runtime.newString("oj/bag"), Block.NULL_BLOCK);

        bag = (RubyClass) oj.getConstantAt("Bag");
    }

    public RubyClass getParseError() {
        return parseError;
    }

    public Odd getOdd(RubyClass clas) {
        for (Odd odd: odds) {
            if (odd.clas == clas) return odd;
        }

        return null;
    }

    public Odd getOdd(String classname) {
        for (Odd odd: odds) {
            if (classname.equals(odd.classname)) return odd;
        }

        return null;
    }

    public static Options getDefaultOptions(ThreadContext context) {
        return default_options.dup(context);
    }

    public void registerOdd(RubyClass clas, IRubyObject createObject, RubySymbol createMethod, IRubyObject[] newArgs) {
        String[] ids = new String[newArgs.length];
        for (int i = 0; i < newArgs.length; i++) {
            ids[i] = newArgs[i].asJavaString();
        }

        Odd odd = new Odd(clas, ids);
        odd.createOp = createMethod.asJavaString();
        odd.createObj = createObject;

        int length = odds.size();
        for (int i = 0; i < length; i++) {
            if (odds.get(i).clas == odd.clas) {
                odds.set(i, odd);
                return;
            }
        }
        odds.add(odd);
    }
}
