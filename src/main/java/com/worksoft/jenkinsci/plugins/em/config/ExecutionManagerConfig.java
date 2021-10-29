/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerConfig
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.config;

import com.worksoft.jenkinsci.plugins.em.ExecuteRequestEMConfig;
import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ExecutionManagerConfig extends GlobalConfiguration {

  public ExecuteRequestEMConfig emConfig;

  public ExecutionManagerConfig () {
    load();
  }

  public ExecuteRequestEMConfig getEmConfig () {
    return emConfig;
  }

  public void setEmConfig (ExecuteRequestEMConfig emConfig) {
    this.emConfig = emConfig;
  }

  /**
   * Checks if the provided values are valid.
   *
   * @param altConfig The alternate EM server config to validate
   *
   * @return true if config is valid, false otherwise
   */
  public FormValidation doValidate (@QueryParameter ExecuteRequestEMConfig altConfig) {
      return FormValidation.ok("Success");
  }

  @Override
  public boolean configure (StaplerRequest req, JSONObject json) throws FormException {
    req.bindJSON(this, json.getJSONObject("execution manager"));
    save();
    return true;
  }
}
