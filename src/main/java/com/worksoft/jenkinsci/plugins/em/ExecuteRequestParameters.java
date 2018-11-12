/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestParameters
 *
 * @author dtheobald on Mon, 22 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.Exported;

import java.util.List;

public final class ExecuteRequestParameters extends AbstractDescribableImpl<ExecuteRequestParameters> {
  @Exported
  private List<ExecuteRequestParameter> list;

  @DataBoundConstructor
  public ExecuteRequestParameters (List<ExecuteRequestParameter> list) {
    this.list = list;
  }

  public List<ExecuteRequestParameter> getList () {
    return list;
  }

  @DataBoundSetter
  public void setList (List<ExecuteRequestParameter> list) {
    this.list = list;
  }

  @Symbol("execParams")
  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestParameters> {
    public String getDisplayName () {
      return "Execution Parameters";
    }
  }
}