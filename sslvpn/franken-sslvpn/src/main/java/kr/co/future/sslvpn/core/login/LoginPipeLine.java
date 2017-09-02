package kr.co.future.sslvpn.core.login;

import kr.co.future.sslvpn.core.pipeline.PipelineContext;
import kr.co.future.sslvpn.core.pipeline.SequentialPipeline;
import kr.co.future.sslvpn.core.pipeline.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPipeLine extends SequentialPipeline{
	private final Logger logger = LoggerFactory.getLogger(LoginPipeLine.class);
	
	@Override
	public void addStage(Stage stage) {
		m_stages.add(stage);
	}

   @Override
	public void addErrorStage(Stage stage) {
		m_errorStages.add(stage);
	}
	
   @Override
	public void addFinalStage(Stage stage) {
		m_finalStages.add(stage);
	}
	
	@Override
	public void execute(PipelineContext context) {
		/* execute the stages */
		int i = 1;
		for (Stage stage:m_stages){
			logger.trace("stage no. [{}]", i++);
			
			stage.execute(context);
			LoginContext loginContext = (LoginContext)context;
			if(loginContext.getResult() != null)
				break;
			
			if (context.getErrors()!= null && !context.getErrors().isEmpty()){
				break;
			}

		}
		/* if any error occurred, execute the error stages*/
		if (context.getErrors()!= null && !context.getErrors().isEmpty()){
			for (Stage errorStage: m_errorStages){
				errorStage.execute(context);
			}			
		}
	}
}
