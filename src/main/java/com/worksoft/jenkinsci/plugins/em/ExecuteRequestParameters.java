/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestParameters
 *
 * @author dtheobald on Mon, 22 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import hudson.model.AbstractDescribableImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.util.ArrayList;
import java.util.List;

public final class ExecuteRequestParameters extends AbstractDescribableImpl<ExecuteRequestParameters> {
    @Exported
    private List<ExecuteRequestParameter> execParamList;

    @DataBoundConstructor
    public ExecuteRequestParameters(List<ExecuteRequestParameter> execParamList) {
        this.execParamList = execParamList;
    }

    public List<ExecuteRequestParameter> getExecParamList() {
        return execParamList;
    }
}