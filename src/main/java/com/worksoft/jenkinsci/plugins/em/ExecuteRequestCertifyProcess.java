/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestCertifyProcess
 *
 * @author rrinehart on Wed, 24 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;


public class ExecuteRequestCertifyProcess extends AbstractDescribableImpl<ExecuteRequestCertifyProcess> {

  @Exported
  String processPath;

  @DataBoundConstructor
  public ExecuteRequestCertifyProcess (String processPath) {
    this.processPath = processPath;
  }

  public String getProcessPath () {
    return processPath;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestCertifyProcess> {
    public String getDisplayName () {
      return "Certify Process Path";
    }


    public FormValidation doCheckProcessPath (@QueryParameter String processPath) {
      FormValidation ret = FormValidation.ok();
      if (StringUtils.isEmpty(processPath)) {
        ret = FormValidation.error("A process Path must be specified!");
      }
      return ret;
    }
  }
}
