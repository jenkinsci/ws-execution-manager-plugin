/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerConfig
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.config;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.URL;

@Extension
public class ExecutionManagerConfig extends GlobalConfiguration {

  public URL url;

  public String name;

  public transient UsernamePasswordCredentials credentials;

  public ExecutionManagerConfig () {
    load();
  }

  public URL getUrl () {
    return url;
  }

  public void setUrl (URL url) {
    this.url = url;
  }

  public String getName () {
    return name;
  }

  public void setName (String name) {
    this.name = name;
  }

  public UsernamePasswordCredentials getCredentials () {
    return credentials;
  }

  public void setCredentials (UsernamePasswordCredentials credentials) {
    this.credentials = credentials;
  }

  /**
   * Checks if the provided values are valid.
   */
  public FormValidation doValidate (@QueryParameter String url,
                                    @QueryParameter String name,
                                    @QueryParameter String credentials) {
    return FormValidation.ok("Success");

  }

  public ListBoxModel doFillCredentialsItems (@AncestorInPath ItemGroup context,
                                              @QueryParameter String url,
                                              @QueryParameter String credentialsId) {
    ListBoxModel data = null;

    AccessControlled _context = (context instanceof AccessControlled ? (AccessControlled) context : Jenkins.getInstance());
    if (_context == null || !_context.hasPermission(Jenkins.ADMINISTER)) {
      data = new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
    } else {
      data = new StandardUsernameListBoxModel()
              .includeEmptyValue()
              .includeMatchingAs(context instanceof Queue.Task
                              ? Tasks.getAuthenticationOf((Queue.Task) context)
                              : ACL.SYSTEM,
                      context,
                      StandardUsernamePasswordCredentials.class,
                      URIRequirementBuilder.fromUri(url).build(),
                      CredentialsMatchers.withScope(CredentialsScope.GLOBAL))
              .includeCurrentValue(credentialsId);

    }
    return data;
  }

  @Override
  public boolean configure (StaplerRequest req, JSONObject json) throws FormException {
    req.bindJSON(this, json);
    save();
    return true;
  }

  public FormValidation doTestConnection(@QueryParameter final String url, @QueryParameter final String credentials) {

    return FormValidation.ok("Success");
  }


  public ExecutionManagerServer getEmServer () {
    return null;
  }
}
