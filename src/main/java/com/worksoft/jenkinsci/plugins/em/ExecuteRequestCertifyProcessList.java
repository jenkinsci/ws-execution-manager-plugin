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
  String folder;
  @Exported
  String requestName;
  @Exported
  List<ExecuteRequestCertifyProcess> processList;

  @DataBoundConstructor
  public ExecuteRequestCertifyProcessList (String database, String project, List<ExecuteRequestCertifyProcess> processList, String folder, String requestName) {
    this.database = database;
    this.project = project;
    this.processList = processList;
    this.folder = folder;
    this.requestName = requestName;
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

  public String getFolder () {
    return folder;
  }

  public String getRequestName () {
    return requestName;
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

    public FormValidation doCheckRequestName (@QueryParameter String requestName) {
      FormValidation ret = FormValidation.ok();
      if (StringUtils.isEmpty(requestName)) {
        ret = FormValidation.error("A request name must be specified!");
      }
      return ret;
    }
  }

}
