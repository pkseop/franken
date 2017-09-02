package kr.co.future.sslvpn.core.pipeline;

/**
 * Pipeline holds a number of stages which will be executed.
 * 
 *  @author Benoy Antony (benoy@ideaimpl.com) (http://www.ideaimpl.com) 
 *
 */
public abstract class Pipeline extends Stage {

	/**
	 * appends a stage to the pipeline
	 * @param stage
	 */
	 public abstract void addStage ( Stage stage);
	 
	 /**
	  * adds a stage to the error handling sequence of stages
	  * @param stage
	  */
	 public abstract void addErrorStage (Stage stage);
	 
	 /**
	  * adds a stage to the "final" sequence of stages
	  * The final sequence of stages will be executed even if there is any error
	  * @param stage
	  */
	 public abstract void addFinalStage (Stage stage);	 
}
