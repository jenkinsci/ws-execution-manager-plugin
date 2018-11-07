/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestRequest
 *
 * @author dtheobald on Mon, 5 Nov 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

public final class ExecuteRequestRequest extends AbstractDescribableImpl<ExecuteRequestRequest> {

  @Exported
  public String request;

  @DataBoundConstructor
  public ExecuteRequestRequest (String request) {
    this.request = request;
  }

  public String getRequest () {
    return request;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestRequest> {
    public String getDisplayName () {
      return "ExecuteRequestRequest";
    }

    public FormValidation doCheckRequest (@QueryParameter String request) {
      ListBoxModel listBox = ExecuteRequest.getCachedItems("request");
      FormValidation ret = FormValidation.ok();

      String msg = request;
      if (msg.startsWith("ERROR") ||
              (listBox != null && (msg = listBox.get(0).value).startsWith("ERROR"))) {
        ret = FormValidation.error("Execution Manager error retrieving requests - " + msg.replace("ERROR: ", "") + "!");
      } else if (StringUtils.isEmpty(request)) {
        ret = FormValidation.error("A request must be specified!");
      }

      return ret;
    }

    // Called whenever emRequestType or alternative EM config changes
    public ListBoxModel doFillRequestItems (@RelativePath("..") @QueryParameter String emRequestType,
                                            @RelativePath("../altEMConfig") @QueryParameter String url,
                                            @RelativePath("../altEMConfig") @QueryParameter String credentials) {
      return ExecuteRequest.fillItems("request", url, credentials);
    }
  }
}