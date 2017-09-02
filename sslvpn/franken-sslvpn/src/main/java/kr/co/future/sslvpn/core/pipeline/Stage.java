package kr.co.future.sslvpn.core.pipeline;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A basic work unit in the pipeline. 
 * 
 * @author Benoy Antony (benoy@ideaimpl.com) (http://www.ideaimpl.com)
 *
 */

public abstract class Stage {
	
    ReentrantLock lock = new ReentrantLock();
    
    /**
     * executes the work to be performed.
     * The input will be read from the context
     * The output will be stored in the context	 
     * @param context - context object which keeps shared state 
     *  
     * */
    public void execute (PipelineContext context) {
    	doExecute(context);
    }
    
    public abstract void doExecute(PipelineContext context);
}
