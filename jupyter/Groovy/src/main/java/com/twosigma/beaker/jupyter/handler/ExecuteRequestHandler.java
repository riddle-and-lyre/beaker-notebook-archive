/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beaker.jupyter.handler;


import com.twosigma.beaker.groovy.evaluator.GroovyEvaluatorManager;
import com.twosigma.beaker.jupyter.msg.MessageCreator;
import org.lappsgrid.jupyter.groovy.GroovyKernel;
import org.lappsgrid.jupyter.groovy.handler.AbstractHandler;
import org.lappsgrid.jupyter.groovy.msg.Header;
import org.lappsgrid.jupyter.groovy.msg.Message;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.twosigma.beaker.jupyter.msg.JupyterMessages.EXECUTE_INPUT;
import static com.twosigma.beaker.jupyter.msg.JupyterMessages.STATUS;

/**
 * Does the actual work of executing user code.
 *
 * @author konst
 */
public class ExecuteRequestHandler extends AbstractHandler<Message> {

  protected int executionCount;
  protected GroovyEvaluatorManager evaluatorManager;
  private MessageCreator messageCreator;

  public ExecuteRequestHandler(GroovyKernel kernel, GroovyEvaluatorManager evaluatorManager) {
    super(kernel);
    logger = LoggerFactory.getLogger(this.getClass());
    this.evaluatorManager = evaluatorManager;
    messageCreator = new MessageCreator(kernel);
    executionCount = 0;
  }

  @Override
  public void handle(Message message) throws NoSuchAlgorithmException {
    logger.info("Processing execute request");
    Message reply = new Message();
    Map<String, Serializable> map = new HashMap<String, Serializable>(1);
    map.put("execution_state", "busy");
    reply.setContent(map);
    reply.setHeader(new Header(STATUS, message.getHeader().getSession()));
    reply.setParentHeader(message.getHeader());
    reply.setIdentities(message.getIdentities());
    publish(reply);

    // Get the code to be executed from the message.
    String code = ((String) message.getContent().get("code")).trim();

    // Announce that we have the code.
    reply.setHeader(new Header(EXECUTE_INPUT, message.getHeader().getSession()));
    Map<String, Serializable> map1 = new HashMap<String, Serializable>(2);
    map1.put("execution_count", executionCount);
    map1.put("code", code);
    reply.setContent(map1);
    publish(reply);

    ++executionCount;
    if (!code.startsWith("%%")) {
      evaluatorManager.executeCode(code, message, executionCount);
      // execution response in ExecuteResultHandler
    } else {
      messageCreator.createMagicMessage(code, executionCount,message);
    }
  }

  @Override
  public void exit() {
    evaluatorManager.exit();
  }
  
}