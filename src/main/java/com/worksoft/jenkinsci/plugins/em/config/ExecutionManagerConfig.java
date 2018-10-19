/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerConfig
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.config;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
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
import jodd.net.URLCoder;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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

  
  /**
   * Looks up the provided credentialId (Jenkins GUID)
   *
   * @param url          - Execution manager's URL
   * @param credentialId - GUID identifying Jenkins credential
   * @return - null, if GUID not found
   */
  private StandardUsernamePasswordCredentials lookupCredentials(String url, String credentialId) {
    return StringUtils.isBlank(credentialId) ? null : CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.getInstanceOrNull(),
                    ACL.SYSTEM,
                    URIRequirementBuilder.fromUri(url).build()
            ),
            CredentialsMatchers.allOf(
                    CredentialsMatchers.withScope(CredentialsScope.GLOBAL),
                    CredentialsMatchers.withId(credentialId)
            ));
  }
  public FormValidation doTestConnection(@QueryParameter final String url, @QueryParameter final String credentials) {
    if (StringUtils.isBlank(credentials)) {
      return FormValidation.error("Credentials must be selected!");
    }


    try {
      URL foo = new URL(url);
    } catch (MalformedURLException e) {
      return FormValidation.error("URL is invalid " + e.getMessage());
    }

    StandardUsernamePasswordCredentials creds = lookupCredentials(url, credentials);
    if (creds == null)
      return FormValidation.error("Credentials lookup error!");

    try {
      ExecutionManagerServer ems = new ExecutionManagerServer(url, "", creds);
      if (!ems.login()) {
        return FormValidation.error("Authorization Failed!");
      }
    } catch (Exception e) {
      return FormValidation.error(e.getMessage());
    }

    return FormValidation.ok("Success");
  }


  public ExecutionManagerServer getEmServer () {
    return null;
  }
}
