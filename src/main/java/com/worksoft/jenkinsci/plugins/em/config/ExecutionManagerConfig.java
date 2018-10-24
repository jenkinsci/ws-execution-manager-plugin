/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerConfig
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.config;

import com.worksoft.jenkinsci.plugins.em.ExecuteRequestAltConfig;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ExecutionManagerConfig extends GlobalConfiguration {

  public ExecuteRequestAltConfig altConfig;

  public ExecutionManagerConfig () {
    load();
  }

  public ExecuteRequestAltConfig getAltConfig () {
    return altConfig;
  }

  public void setAltConfig (ExecuteRequestAltConfig altConfig) {
    this.altConfig = altConfig;
  }

  /**
   * Checks if the provided values are valid.
   */
  public FormValidation doValidate (@QueryParameter ExecuteRequestAltConfig altConfig) {
    return FormValidation.ok("Success");

  }

  @Override
  public boolean configure (StaplerRequest req, JSONObject json) throws FormException {
    req.bindJSON(this, json.getJSONObject("execution manager"));
    save();
    return true;
  }

  public ExecutionManagerServer getEmServer () {
    return null;
  }
}
