package oj;

import org.jruby.Ruby;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.load.Library;

/**
 * Created by enebo on 8/28/15.
 */
public class OjLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) {
        RubyKernel.require(runtime.getKernel(), runtime.newString("oj"), Block.NULL_BLOCK);
        RubyModule ojModule = runtime.getOrCreateModule("Oj");
        ojModule.defineAnnotatedMethods(RubyOj.class);
    }
}
