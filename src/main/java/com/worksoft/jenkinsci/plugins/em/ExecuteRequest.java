/*
 *
 * Copyright (c) 2018 - 2018 Worksoft, Inc.
 *
 * ${CLASS_NAME}
 *
 * @author rrinehart on 9/14/2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.google.common.primitives.Ints;
import com.worksoft.jenkinsci.plugins.em.config.ExecutionManagerConfig;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

public class ExecuteRequest extends Builder implements SimpleBuildStep {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");

  private String emRequestType;

  private final String bookmark;
  private final String request;
  private final ExecuteRequestCertifyProcessList processList;

  public final ExecuteRequestPostExecute postExecute;
  private ExecutionManagerConfig globalConfig;
  private ExecuteRequestEMConfig altEMConfig;

  private ExecuteRequestWaitConfig waitConfig;
  private ExecuteRequestParameters execParams;

  @DataBoundConstructor
  public ExecuteRequest (String emRequestType, String request, String bookmark, ExecuteRequestCertifyProcessList processList, ExecuteRequestParameters execParams, ExecuteRequestWaitConfig waitConfig, ExecuteRequestEMConfig altEMConfig, ExecuteRequestPostExecute postExecute) {
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

  public String getBookmark () {
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

    public ComboBoxModel doFillBookmarkItems () {
      return new ComboBoxModel("One", "Two", "Three");
    }
  }

  @Override
  public void perform (@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
    ExecuteRequestEMConfig emConfig = this.globalConfig != null ? this.globalConfig.getEmConfig() : null;
    if (altEMConfig != null && altEMConfig.isValid()) {
      emConfig = getAltEMConfig();
    }

    if (emConfig != null) {
      ExecutionManagerServer server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
      if (server.login()) {
        if (emRequestType.equals("request")) {
          String reqID = null;

          // If the 'request' is a positive integer, assume it's an ID and don't look it up
          Integer val;
          if (!StringUtils.isEmpty(request)) {
            if ((val = Ints.tryParse(request)) != null) {
              if (val > 0) {
                reqID = request;
              }
            }
          }
          if (reqID == null) {
            JSONArray reqs = server.requests().getJSONArray("requests");
            for (int i = 0; i < reqs.size(); i++) {
              JSONObject req;
              if ((req = reqs.getJSONObject(i)).getString("Name").equals(request)) {
                reqID = req.getString("RequestID");
                break;
              }
            }
          }
          if (reqID == null) {
            log.warning("");
            reqID = request;
          }
          server.executeRequest(reqID);
        } else if (emRequestType.equals("bookmark")) {
          //server.executeBookmark(request);
        } else if (emRequestType.equals("processList")) {
          //server.executeBookmark(request);
        }
      } else {
        System.out.println("Bad credentials!");
        throw new RuntimeException("Bad credentials!");
      }
    }
  }
}
