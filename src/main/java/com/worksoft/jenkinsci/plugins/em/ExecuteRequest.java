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
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ExecuteRequest extends Builder implements SimpleBuildStep {

  private String emRequestType;

  private final String bookmark;
  private final String request;
  private final ExecuteRequestCertifyProcessList processList;


  private ExecutionManagerConfig globalConfig;
  private ExecuteRequestEMConfig altEMConfig;

  private ExecuteRequestWaitConfig waitConfig;
  private ExecuteRequestParameters execParams;

  @DataBoundConstructor
  public ExecuteRequest (String emRequestType, String request, String bookmark, ExecuteRequestCertifyProcessList processList, ExecuteRequestParameters execParams, ExecuteRequestWaitConfig waitConfig, ExecuteRequestEMConfig altEMConfig) {
    this.emRequestType = emRequestType;
    this.bookmark = bookmark;
    this.request = request;
    this.execParams = execParams;
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
   */
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

  @Override
  public void perform (@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
    ExecuteRequestEMConfig emConfig = this.globalConfig != null ? this.globalConfig.getEmConfig() : null;
    if (getAltEMConfig() != null) {
      emConfig = getAltEMConfig();
    }

    if (emConfig != null) {
      ExecutionManagerServer server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
    }
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
}
