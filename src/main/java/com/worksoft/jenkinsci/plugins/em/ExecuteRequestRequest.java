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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;

public final class ExecuteRequestRequest extends AbstractDescribableImpl<ExecuteRequestRequest> {

  @Exported
  public String name;

  @DataBoundConstructor
  public ExecuteRequestRequest (String name) {
    this.name = name;
  }

  public String getName () {
    return name;
  }

  @Symbol("emRequest")
  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestRequest> {
    @Nonnull
    public String getDisplayName () {
      return "Execute Request";
    }

    public FormValidation doCheckName (@QueryParameter String name) {
      ListBoxModel listBox = EMItemCache.getCachedItems("request");
      FormValidation ret = FormValidation.ok();

      String msg = name;
      if (msg.startsWith("ERROR") ||
              (listBox != null && (msg = listBox.get(0).value).startsWith("ERROR"))) {
        ret = FormValidation.error("Execution Manager error retrieving requests - " + msg.replace("ERROR: ", "") + "!");
      } else if (StringUtils.isEmpty(name)) {
        ret = FormValidation.error("A request must be specified!");
      }

      return ret;
    }

    // Called whenever emRequestType or alternative EM config changes
    public ListBoxModel doFillNameItems (@RelativePath("..") @QueryParameter String requestType,
                                            @RelativePath("../_.altEMConfig") @QueryParameter String url,
                                            @RelativePath("../_.altEMConfig") @QueryParameter String credentials) {
      return ExecuteRequest.fillItems("request", url, credentials);
    }
  }
}