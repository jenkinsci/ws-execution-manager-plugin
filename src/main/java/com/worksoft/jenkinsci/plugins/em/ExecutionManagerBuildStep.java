/*
 *
 * Copyright (c) 2018 - 2018 Worksoft, Inc.
 *
 * ${CLASS_NAME}
 *
 * @author rrinehart on 9/14/2018
 */


package com.worksoft.jenkinsci.plugins.em;

import hudson.DescriptorExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * Base class for all execution manager steps
 */
public class ExecutionManagerBuildStep implements Describable<ExecutionManagerBuildStep> {

  public ExecutionManagerBuildStep () {
  }

  public static DescriptorExtensionList<ExecutionManagerBuildStep, ExeutionManagerBuildStepDescriptor> all() {
    return Jenkins.getInstance().getDescriptorList(ExecutionManagerBuildStep.class);
  }

  @Override
  public Descriptor<ExecutionManagerBuildStep> getDescriptor () {
    return null;
  }


  public static abstract class ExeutionManagerBuildStepDescriptor extends Descriptor<ExecutionManagerBuildStep> {

  }
}
