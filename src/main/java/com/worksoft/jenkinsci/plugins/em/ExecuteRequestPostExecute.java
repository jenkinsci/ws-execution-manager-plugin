/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestPostExecute
 *
 * @author oullah on Mon, 29 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public final class ExecuteRequestPostExecute extends AbstractDescribableImpl<ExecuteRequestPostExecute> {
  @Exported
  public String postExecuteActionName;
  @Exported
  public String postExecuteActionParams;

  @DataBoundConstructor
  public ExecuteRequestPostExecute (String postExecuteActionName, String postExecuteActionParams) {
    this.postExecuteActionName = postExecuteActionName;
    this.postExecuteActionParams = postExecuteActionParams;
  }

  public String getpostExecuteActionName () {
    return postExecuteActionName;
  }

  public String getpostExecuteActionParams () {
    return postExecuteActionParams;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestPostExecute> {
    public String getDisplayName () {
      return "ExecuteRequestPostExecute";
    }
  }
}
