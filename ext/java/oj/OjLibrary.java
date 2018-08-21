package oj;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import java.util.List;

/**
 * Created by enebo on 8/28/15.
 */
public class OjLibrary implements Library {
    public static Options default_options;
    private RubyClass parseError;
    private List<Odd> odds;

    public void load(Ruby runtime, boolean wrap) {
        RubyKernel.require(runtime.getKernel(), runtime.newString("date"), Block.NULL_BLOCK);
        RubyKernel.require(runtime.getKernel(), runtime.newString("oj"), Block.NULL_BLOCK);
        RubyModule ojModule = runtime.getOrCreateModule("Oj");
        ojModule.defineAnnotatedMethods(RubyOj.class);

        default_options = new Options();

        ojModule.setInternalVariable("_oj", this);

        // FIXME: Can crash
        parseError = (RubyClass) ojModule.getConstant("ParseError");

        StringWriter.createStringWriterClass(runtime, ojModule);
        Doc.createDocClass(runtime, ojModule);

        odds = Odd.initBuiltinOdds(runtime);
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

    public static Options getDefaultOptions() {
        return default_options.dup();
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
