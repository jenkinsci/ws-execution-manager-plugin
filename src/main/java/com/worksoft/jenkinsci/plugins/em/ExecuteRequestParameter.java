/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestParameter
 *
 * @author dtheobald on Fri, 19 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public final class ExecuteRequestParameter extends AbstractDescribableImpl<ExecuteRequestParameter> {

    @Exported public String key;
    @Exported public String value;

    @DataBoundConstructor
    public ExecuteRequestParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static ExecuteRequestParameter[] getSomeDefaults() {
        return new ExecuteRequestParameter[] { new ExecuteRequestParameter("valueA", "valueB") };
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExecuteRequestParameter> {
        public String getDisplayName() {
            return "ExecuteRequestParameter";
        }
    }
}