/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecuteRequestEMConfig
 *
 * @author dtheobald on Tue, 23 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.worksoft.jenkinsci.plugins.em.model.EmResult;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

import java.net.MalformedURLException;
import java.net.URL;

public final class ExecuteRequestEMConfig extends AbstractDescribableImpl<ExecuteRequestEMConfig> {

  @Exported
  public String url;
  @Exported
  public String credentials;

  @DataBoundConstructor
  public ExecuteRequestEMConfig (String url, String credentials) {
    this.url = url;
    this.credentials = credentials;
  }

  public String getUrl () {
    return url;
  }

  public String getCredentials () {
    return credentials;
  }

  public boolean isValid () {
    return StringUtil.isNotEmpty(url) && StringUtils.isNotEmpty(credentials);
  }

  public StandardUsernamePasswordCredentials lookupCredentials () {
    return lookupCredentials(url, credentials);
  }

  private static StandardUsernamePasswordCredentials lookupCredentials (String url, String credentialId) {
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

  @Extension
  public static class DescriptorImpl extends Descriptor<ExecuteRequestEMConfig> {
    public String getDisplayName () {
      return "ExecuteRequestEMConfig";
    }

    public FormValidation doCheckUrl (@QueryParameter String url) {
      FormValidation ret = FormValidation.ok();

      try {
        new URL(url);
      } catch (MalformedURLException e) {
        ret = FormValidation.error("URL is invalid " + e.getMessage());
      }

      return ret;
    }

    public FormValidation doCheckCredentials (@QueryParameter String credentials) {
      FormValidation ret = FormValidation.ok();
      return ret;
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

    public FormValidation doTestConnection (@QueryParameter final String url, @QueryParameter final String credentials) {
      if (StringUtils.isBlank(credentials)) {
        return FormValidation.error("Credentials must be selected!");
      }


      try {
        URL foo = new URL(url);
      } catch (MalformedURLException e) {
        return FormValidation.error("URL is invalid " + e.getMessage());
      }

      StandardUsernamePasswordCredentials creds = lookupCredentials(url, credentials);
      if (creds == null) {
        return FormValidation.error("Credentials lookup error!");
      }

      try {
        ExecutionManagerServer ems = new ExecutionManagerServer(url, creds);
        if (!ems.login()) {
          EmResult result = ems.getLastEMResult();
          String err = result.getResponse().statusPhrase();
          if (result.getJsonData() == null) {
            return FormValidation.error(err);
          } else {
            try {
              err = result.getJsonData().getString("error_description");
            } catch (Exception ignored) {
            }
            return FormValidation.error(err);
          }
        }
      } catch (Exception e) {
        return FormValidation.error(e.getMessage());
      }

      return FormValidation.ok("Success");
    }
  }
}