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

public final class ExecuteRequestBookmark extends AbstractDescribableImpl<ExecuteRequestBookmark> {

  private String bookmark;
  private String folder;

  @DataBoundConstructor
  public ExecuteRequestBookmark (String bookmark) {
    this.bookmark = bookmark;
    this.folder = "";
  }

  public String getBookmark () {
    return bookmark;
  }

  public String getFolder () {
    return folder;
  }

  @DataBoundSetter
  public void setFolder (String folder) {
    this.folder = folder;
  }

  @Symbol("bookmark")
  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestBookmark> {
    public String getDisplayName () {
      return "ExecuteRequestWaitConfig";
    }

    public FormValidation doCheckBookmark (@QueryParameter String bookmark) {
      ListBoxModel listBox = EMItemCache.getCachedItems("bookmark");
      FormValidation ret = FormValidation.ok();

      String msg = bookmark;
      if (msg.startsWith("ERROR") ||
              (listBox != null && (msg = listBox.get(0).value).startsWith("ERROR"))) {
        ret = FormValidation.error("Execution Manager error retrieving bookmarks - " + msg.replace("ERROR: ", "") + "!");
      } else if (StringUtils.isEmpty(bookmark)) {
        ret = FormValidation.error("A bookmark must be specified!");
      }

      return ret;
    }

    public FormValidation doCheckfolder (@QueryParameter String folder) {
      FormValidation ret = FormValidation.ok();

      return ret;
    }

    // Called whenever emRequestType or alternative EM config changes
    public ListBoxModel doFillBookmarkItems (@RelativePath("..") @QueryParameter String emRequestType,
                                             @RelativePath("../altEMConfig") @QueryParameter String url,
                                             @RelativePath("../altEMConfig") @QueryParameter String credentials) {
      return ExecuteRequest.fillItems("bookmark", url, credentials);
    }
  }
}