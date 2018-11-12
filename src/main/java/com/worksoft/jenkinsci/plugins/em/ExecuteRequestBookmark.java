/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestBookmark
 *
 * @author dtheobald on Tue, 30 Oct 2018
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
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import javax.annotation.Nonnull;

public final class ExecuteRequestBookmark extends AbstractDescribableImpl<ExecuteRequestBookmark> {

  @Exported
  private String name;
  @Exported
  private String folder;

  @DataBoundConstructor
  public ExecuteRequestBookmark (String name) {
    this.name = name;
    this.folder = "";
  }

  public String getName () {
    return name;
  }

  public String getFolder () {
    return folder;
  }

  @DataBoundSetter
  public void setFolder (String folder) {
    this.folder = folder;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestBookmark> {
    @Nonnull
    public String getDisplayName () {
      return "Execute Bookmark";
    }

    public FormValidation doCheckBookmark (@QueryParameter String name) {
      ListBoxModel listBox = ExecuteRequest.getCachedItems("bookmark");
      FormValidation ret = FormValidation.ok();

      String msg = name;
      if (msg.startsWith("ERROR") ||
              (listBox != null && (msg = listBox.get(0).value).startsWith("ERROR"))) {
        ret = FormValidation.error("Execution Manager error retrieving bookmarks - " + msg.replace("ERROR: ", "") + "!");
      } else if (StringUtils.isEmpty(name)) {
        ret = FormValidation.error("A bookmark must be specified!");
      }

      return ret;
    }

    public FormValidation doCheckfolder (@QueryParameter String folder) {
      FormValidation ret = FormValidation.ok();

      return ret;
    }

    // Called whenever emRequestType or alternative EM config changes
    public ListBoxModel doFillNameItems (@RelativePath("..") @QueryParameter String requestType,
                                             @RelativePath("../altEMConfig") @QueryParameter String url,
                                             @RelativePath("../altEMConfig") @QueryParameter String credentials) {
      return ExecuteRequest.fillItems("bookmark", url, credentials);
    }
  }
}