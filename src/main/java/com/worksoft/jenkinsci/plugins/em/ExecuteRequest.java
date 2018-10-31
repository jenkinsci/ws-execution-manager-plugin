/*
 *
 * Copyright (c) 2018 - 2018 Worksoft, Inc.
 *
 * ${CLASS_NAME}
 *
 * @author rrinehart on 9/14/2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.worksoft.jenkinsci.plugins.em.config.ExecutionManagerConfig;
import com.worksoft.jenkinsci.plugins.em.model.EmResult;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ComboBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecuteRequest extends Builder implements SimpleBuildStep {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");

  // The following instance variables are those provided by the GUI
  private final String emRequestType;
  private final ExecuteRequestBookmark bookmark;
  private final String request;
  private final ExecuteRequestCertifyProcessList processList;
  public final ExecuteRequestPostExecute postExecute;
  private final ExecutionManagerConfig globalConfig;
  private final ExecuteRequestEMConfig altEMConfig;
  private final ExecuteRequestWaitConfig waitConfig;
  private final ExecuteRequestParameters execParams;

  // These instance variables are those used during execution
  private ExecuteRequestEMConfig emConfig; // EM config used during run
  private ExecutionManagerServer server;
  private Run<?, ?> run;
  private FilePath workspace;
  private Launcher launcher;
  private TaskListener listener;

  @DataBoundConstructor
  public ExecuteRequest (String emRequestType, String request, ExecuteRequestCertifyProcessList processList, ExecuteRequestParameters execParams, ExecuteRequestWaitConfig waitConfig, ExecuteRequestEMConfig altEMConfig, ExecuteRequestPostExecute postExecute, ExecuteRequestBookmark bookmark) {
    this.emRequestType = emRequestType;
    this.bookmark = bookmark;
    this.request = request;
    this.execParams = execParams;
    this.postExecute = postExecute;
    this.waitConfig = waitConfig;
    this.altEMConfig = altEMConfig;
    this.processList = processList;
    globalConfig = GlobalConfiguration.all().get(ExecutionManagerConfig.class);
  }

  /**
   * Stapler methods for handling Execute Request Parameters
   */
  public boolean getExecParamsEnabled () {
    return getExecParams() != null;
  }

  public ExecuteRequestParameters getExecParams () {
    return execParams;
  }

  /**
   * Stapler methods for handling Execute Request Wait Configuration
   * }
   * /** Stapler methods for handling Execute Request Post Execute Action
   */

  public boolean getPostExecuteEnabled () {
    return getPostExecute() != null;
  }

  public ExecuteRequestPostExecute getPostExecute () {
    return postExecute;
  }

  public boolean getWaitConfigEnabled () {
    return getWaitConfig() != null;
  }

  public ExecuteRequestWaitConfig getWaitConfig () {
    return waitConfig;
  }

  /**
   * Stapler methods for handling Execute Request Alt Configuration
   */
  public boolean getAltEMConfigEnabled () {
    return getAltEMConfig() != null;
  }

  public ExecuteRequestEMConfig getAltEMConfig () {
    return altEMConfig;
  }

  public String getEmRequestType () {
    return emRequestType;
  }

  public ExecuteRequestBookmark getBookmark () {
    return bookmark;
  }

  public String getRequest () {
    return request;
  }

  public String emRequestTypeEquals (String given) {
    String ret = String.valueOf((emRequestType != null) && (emRequestType.equals(given)));
    return ret;
  }

  public ExecuteRequestCertifyProcessList getProcessList () {
    return processList;
  }

  @Extension
  public static final class ExecutionManagerBuilderDescriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable (Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName () {
      return "Run Execution Manager Request";
    }

    public ComboBoxModel doFillRequestItems () {
      return new ComboBoxModel("Apple", "Banana", "Oreo");
    }
  }

  @Override
  public void perform (@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
    // Save perform parameters in instance variables for future reference.
    this.run = run;
    this.workspace = workspace;
    this.launcher = launcher;
    this.listener = listener;

    // Pick the right EM configuration
    emConfig = this.globalConfig != null ? this.globalConfig.getEmConfig() : null;
    if (altEMConfig != null && altEMConfig.isValid()) {
      emConfig = getAltEMConfig();
    }

    if (emConfig != null) {
      server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
      if (server.login()) {
        // Dispatch to one of the methods below
        try {
          Method meth = this.getClass().getMethod("execute_" + emRequestType.toUpperCase().trim());
          meth.invoke(this);
        } catch (NoSuchMethodException ex) {
          listener.fatalError("Don't know how to execute '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.
        } catch (IllegalAccessException ex) {
          listener.fatalError("Couldn't execute '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.

          log.severe("ERROR: unexpected error while processing request: " + emRequestType);
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        } catch (InvocationTargetException ex) {
          listener.fatalError("Exception thrown while executing '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.
        }
      } else {
        EmResult result = server.getLastEMResult();
        listener.fatalError("Can't log in to '%s' - %s",
                emConfig.getUrl(), result.getResponseData());
        log.log(Level.SEVERE, "Bad credentials!"); // Ends up in the log
        run.setResult(Result.FAILURE); // Fail this build step.
        //throw new RuntimeException("Bad credentials!"); // In console output end build status to fail
      }
    } else {
      listener.fatalError("An Execution Manager configuration must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    }

    if (run.getResult() != Result.FAILURE) {
      // wait for completion
    }
  }

  // Called via reflection from the dispatcher above
  public void execute_REQUEST () throws InterruptedException, IOException {
    if (StringUtils.isEmpty(request)) {
      listener.fatalError("A request name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String reqID = null;
      String theReq = request.trim();
      JSONObject reqs;

      if ((reqs = server.requests()) != null) {
        try {
          // Lookup all the requests defined on the EM and find the one specified
          // by the user
          JSONArray objs = reqs.getJSONArray("objects");
          for (int i = 0; i < objs.size(); i++) {
            JSONObject req;
            if ((req = objs.getJSONObject(i)).getString("Name").equals(theReq) ||
                    req.getString("RequestID").equals(theReq)) {
              reqID = req.getString("RequestID");
              break;
            }
          }
        } catch (Exception ex) {
          log.severe("ERROR: unexpected error during execute_REQUEST:");
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        }
      }
      if (reqID == null) {
        listener.fatalError("No such request '%s'!", theReq);
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        //server.executeRequest(reqID);
      }
    }
  }

  public void execute_BOOKMARK () throws InterruptedException, IOException {
    if (bookmark == null || StringUtils.isEmpty(bookmark.getBookmark())) {
      listener.fatalError("A bookmark name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String bmarkID = null;
      String theBmark = bookmark.getBookmark().trim();
      JSONObject bmarks;

      if ((bmarks = server.bookmarks()) != null) {
        try {
          // Lookup all the bookmarks defined on the EM and find the one specified
          // by the user
          JSONArray objs = bmarks.getJSONArray("objects");
          for (int i = 0; i < objs.size(); i++) {
            JSONObject bmark;
            if ((bmark = objs.getJSONObject(i)).getString("Name").equals(theBmark) ||
                    bmark.getString("Id").equals(theBmark)) {
              bmarkID = bmark.getString("Id");
              break;
            }
          }
        } catch (Exception ex) {
          log.severe("ERROR: unexpected error during execute_BOOKMARK");
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        }
      }
      if (bmarkID == null) {
        listener.fatalError("No such bookmark '%s'!", theBmark);
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        //server.executeBookmark(bmarkID, bmark.getFolder());
      }
    }
  }

  public void execute_PROCESSLIST () throws InterruptedException, IOException {

  }
}
