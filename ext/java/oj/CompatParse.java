package oj;

import org.jruby.runtime.ThreadContext;

/**
 * Created by enebo on 8/28/15.
 */
public class CompatParse extends Parse {
    public CompatParse(ThreadContext context, Options options) {
        super(context, options, null);
    }
}
