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
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import java.util.List;

public final class ExecuteRequestParameters extends AbstractDescribableImpl<ExecuteRequestParameters> {
    @Exported public List<ExecuteRequestParameter> execParamList;

    @DataBoundConstructor
    public ExecuteRequestParameters(List<ExecuteRequestParameter> execParamList) {
        this.execParamList = execParamList;
    }

    public List<ExecuteRequestParameter> getExecParamList() {
        return execParamList;
    }
}