package oj;

import oj.handlers.AddPICall;
import oj.handlers.ArrayAppendPICall;
import oj.handlers.DispatchAddPICall;
import oj.handlers.DispatchArrayAppendPICall;
import oj.handlers.DispatchHashSetPICall;
import oj.handlers.DispatchPICall;
import oj.handlers.HashSetPICall;
import oj.handlers.NoopAddPICall;
import oj.handlers.NoopArrayAppendPICall;
import oj.handlers.NoopHashSetPICall;
import oj.handlers.NoopPICall;
import oj.handlers.PICall;
import org.jruby.Ruby;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.load.Library;

/**
 * Created by enebo on 8/28/15.
 */
public class OjLibrary implements Library {
    // FIXME: This should be internal variable
    public Options default_options;

    public PICall endArrayDispatch;
    public PICall endHashDispatch;
    public PICall startArrayDispatch;
    public PICall startHashDispatch;
    public PICall hashKeyDispatch;
    public PICall noopPICall;
    public HashSetPICall scpHashSetDispatch;
    public HashSetPICall hashSetNoop;
    public ArrayAppendPICall scpArrayAppendDispatch;
    public ArrayAppendPICall arrayAppendNoop;
    public AddPICall scpAddDispatch;
    public AddPICall noopAdd;

    public void load(Ruby runtime, boolean wrap) {
        RubyKernel.require(runtime.getKernel(), runtime.newString("oj"), Block.NULL_BLOCK);
        RubyModule ojModule = runtime.getOrCreateModule("Oj");
        ojModule.defineAnnotatedMethods(RubyOj.class);

        default_options = new Options();

        endHashDispatch = new DispatchPICall("hash_end");
        endArrayDispatch = new DispatchPICall("array_end");
        startHashDispatch = new DispatchPICall("hash_start");
        startArrayDispatch = new DispatchPICall("array_start");
        hashKeyDispatch = new DispatchPICall("hash_key");
        noopPICall = new NoopPICall();
        scpHashSetDispatch = new DispatchHashSetPICall();
        hashSetNoop = new NoopHashSetPICall();
        scpArrayAppendDispatch = new DispatchArrayAppendPICall();
        arrayAppendNoop = new NoopArrayAppendPICall();
        scpAddDispatch = new DispatchAddPICall();
        noopAdd = new NoopAddPICall();

        ojModule.setInternalVariable("_oj", this);
    }
}
