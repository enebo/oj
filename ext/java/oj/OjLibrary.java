package oj;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

/**
 * Created by enebo on 8/28/15.
 */
public class OjLibrary implements Library {
    public Options default_options;
    private RubyClass parseError;

    public void load(Ruby runtime, boolean wrap) {
        RubyKernel.require(runtime.getKernel(), runtime.newString("oj"), Block.NULL_BLOCK);
        RubyModule ojModule = runtime.getOrCreateModule("Oj");
        ojModule.defineAnnotatedMethods(RubyOj.class);

        default_options = new Options();

        ojModule.setInternalVariable("_oj", this);

        // FIXME: Can crash
        parseError = (RubyClass) ojModule.getConstant("ParseError");
    }

    public RubyClass getParseError() {
        return parseError;
    }
}
