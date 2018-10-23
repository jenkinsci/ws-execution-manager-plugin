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
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class ExecuteRequest extends Builder implements SimpleBuildStep {

    public final String bookmark;
    public final String request;
    public final String postAction = null;
    public final String postActionParams = null;
    public final String pollInterval = null;

    private ExecutionManagerConfig config;
    private String altConfigURL;

    public final ExecuteRequestParameters execParams;
    public List<ExecuteRequestParameter> execParamList; // Not used. We get these out of 'execParams' instead

    @DataBoundConstructor
    public ExecuteRequest(String request, String bookmark, ExecuteRequestParameters execParams, List<ExecuteRequestParameter> _execParams) {//}, String postAction, String postActionParams, String pollInterval, String maxRunTime, List<ExecuteRequestParameter> execParams, String altConfigURL) {
        this.bookmark = bookmark;
        this.request = request;
        /*this.execParams = execParams;
        this.postAction = postAction;
        this.postActionParams = postActionParams;
        this.pollInterval = pollInterval;
        this.maxRunTime = maxRunTime;
        this.altConfigURL = altConfigURL;*/
        this.execParams = execParams;
        config = GlobalConfiguration.all().get(ExecutionManagerConfig.class);
    }

    /** Stapler methods for handling Execute Request Parameters */
    public boolean getExecParamsEnabled() {
        return getExecParams() != null;
    }

    public ExecuteRequestParameters getExecParams() {
        return execParams;
    }

    public List<ExecuteRequestParameter> getExecParamList() {
        return execParams != null ? execParams.execParamList : null;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        ExecutionManagerServer server = config.getEmServer();
    }

    @Extension
    public static final class ExecutionManagerBuilderDescriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Run Execution Manager Request";
        }

        public ComboBoxModel doFillRequestItems() {
            return new ComboBoxModel("Apple", "Banana", "Oreo");
        }

        public ComboBoxModel doFillBookmarkItems() {
            return new ComboBoxModel("One", "Two", "Three");
        }
    }
}
