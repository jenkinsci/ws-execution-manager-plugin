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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.logging.Logger;

public final class ExecuteRequestParameters extends AbstractDescribableImpl<ExecuteRequestParameters> {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");
  
  @Exported
  private List<ExecuteRequestParameter> list;

  @DataBoundConstructor
  public ExecuteRequestParameters (List<ExecuteRequestParameter> list) {
    try {
      this.list = list;
    } catch (Exception e)
    {
      log.info("ExecuteRequestParameters: error " + e);
    }
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
    @Nonnull
    public String getDisplayName () {
      return "Execution Parameters";
    }
  }
}