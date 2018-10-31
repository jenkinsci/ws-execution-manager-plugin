/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestBookmark
 *
 * @author dtheobald on Tue, 30 Oct 2018
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

public final class ExecuteRequestBookmark extends AbstractDescribableImpl<ExecuteRequestBookmark> {

    @Exported
    public String bookmark;
    @Exported
    public String folder;

    @DataBoundConstructor
    public ExecuteRequestBookmark (String bookmark, String folder) {
        this.bookmark = bookmark;
        this.folder = folder;
    }

    public String getBookmark() {
        return bookmark;
    }

    public String getFolder() {
        return folder;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExecuteRequestBookmark> {
        public String getDisplayName() {
            return "ExecuteRequestWaitConfig";
        }

        public FormValidation doCheckBookmark(@QueryParameter String bookmark) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isEmpty(bookmark))
                ret = FormValidation.error("A bookmark must be specified!");

            return ret;
        }

        public FormValidation doCheckfolder(@QueryParameter String folder) {
            FormValidation ret = FormValidation.ok();

            return ret;
        }
    }
}