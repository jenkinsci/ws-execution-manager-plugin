/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestCertifyProcessList
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

import java.util.List;


public class ExecuteRequestCertifyProcessList extends AbstractDescribableImpl<ExecuteRequestCertifyProcessList> {

  @Exported
  String database;
  @Exported
  String project;
  @Exported
  List<ExecuteRequestCertifyProcess> processList;

  @DataBoundConstructor
  public ExecuteRequestCertifyProcessList (String database, String project, List<ExecuteRequestCertifyProcess> processList) {
    this.database = database;
    this.project = project;
    this.processList = processList;
  }

  public List<ExecuteRequestCertifyProcess> getProcessList () {
    return processList;
  }

  public String getDatabase () {
    return database;
  }

  public String getProject () {
    return project;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestCertifyProcessList> {
    public String getDisplayName () {
      return "Certify Process Config";
    }


    public FormValidation doCheckDatabase (@QueryParameter String database) {
      FormValidation ret = FormValidation.ok();
      if (StringUtils.isEmpty(database)) {
        ret = FormValidation.error("A database must be specified!");
      }
      return ret;
    }

    public FormValidation doCheckProject (@QueryParameter String project) {
      FormValidation ret = FormValidation.ok();
      if (StringUtils.isEmpty(project)) {
        ret = FormValidation.error("A project must be specified!");
      }
      return ret;
    }


//    public FormValidation doValidate(@QueryParameter final String database, @QueryParameter final String project, @QueryParameter List<ExecuteRequestCertifyProcess> processList) {
//      return FormValidation.ok("Valid");
//    }

  }

}
