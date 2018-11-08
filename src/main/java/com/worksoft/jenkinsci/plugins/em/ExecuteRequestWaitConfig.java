/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestWaitConfig
 *
 * @author dtheobald on Tue, 23 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.google.common.primitives.Ints;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import java.util.concurrent.TimeUnit;

public final class ExecuteRequestWaitConfig extends AbstractDescribableImpl<ExecuteRequestWaitConfig> {

  @Exported
  public String pollInterval;
  @Exported
  public String maxRunTime;

  @DataBoundConstructor
  public ExecuteRequestWaitConfig (String pollInterval, String maxRunTime) {
    this.pollInterval = pollInterval;
    this.maxRunTime = maxRunTime;
  }

  public String getPollInterval () {
    return pollInterval;
  }

  public String getMaxRunTime () {
    return maxRunTime;
  }

  private Long stringSecondsToMillis (String sVal) {
    Integer val;
    Long retVal = null;
    if (!(StringUtils.isEmpty(sVal))) {
      if ((val = Ints.tryParse(sVal)) != null) {
        if ((retVal = (long) val) <= 0) {
          retVal = null;
        }
      }
    }

    if (retVal != null) {
      retVal = TimeUnit.MILLISECONDS.convert(retVal, TimeUnit.SECONDS);
    }
    return retVal;
  }

  public Long pollIntervalInMillis () {
    return stringSecondsToMillis(getPollInterval());
  }

  public Long maxRunTimeInMillis () {
    return stringSecondsToMillis(getMaxRunTime());
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestWaitConfig> {
    public String getDisplayName () {
      return "ExecuteRequestWaitConfig";
    }

    public FormValidation doCheckPollInterval (@QueryParameter String pollInterval) {
      FormValidation ret = FormValidation.ok();
      Integer val;
      if (StringUtils.isEmpty(pollInterval)) {
        ret = FormValidation.error("A polling interval must be specified!");
      } else if ((val = Ints.tryParse(pollInterval)) == null) {
        ret = FormValidation.error("The polling interval must be an integer!");
      } else if (val <= 0) {
        ret = FormValidation.error("The polling interval must be a positive integer!");
      }

      return ret;
    }

    public FormValidation doCheckMaxRunTime (@QueryParameter String maxRunTime) {
      FormValidation ret = FormValidation.ok();
      Integer val;
      if (StringUtils.isEmpty(maxRunTime)) {
        ret = FormValidation.error("A maximum run time must be specified!");
      } else if ((val = Ints.tryParse(maxRunTime)) == null) {
        ret = FormValidation.error("The maximum run time must be an integer!");
      } else if (val <= 0) {
        ret = FormValidation.error("The maximum run time must be a positive integer!");
      }

      return ret;
    }
  }
}