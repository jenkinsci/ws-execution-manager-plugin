/*
 *
 * Copyright (c) 2018 - 2018 Worksoft, Inc.
 *
 * ${CLASS_NAME}
 *
 * @author rrinehart on 9/14/2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.worksoft.jenkinsci.plugins.em.config.ExecutionManagerConfig;
import com.worksoft.jenkinsci.plugins.em.model.EmResult;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecuteRequest extends Builder implements SimpleBuildStep {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");

  private final String url;
  private final String credentials;

  public String getUrl () {
    return url;
  }

  public String getCredentials () {
    return credentials;
  }

  // The following instance variables are those provided by the GUI
  private final String emRequestType;
  private final ExecuteRequestBookmark bookmark;
  private final String request;
  private final ExecuteRequestCertifyProcessList processList;
  private final ExecuteRequestPostExecute postExecute;
  private final ExecutionManagerConfig globalConfig;
  private final ExecuteRequestEMConfig altEMConfig;
  private final ExecuteRequestWaitConfig waitConfig;
  private final ExecuteRequestParameters execParams;

  // These instance variables are those used during execution
  private ExecuteRequestEMConfig emConfig; // EM config used during run
  private ExecutionManagerServer server;
  private Run<?, ?> run;
  private FilePath workspace;
  private Launcher launcher;
  private TaskListener listener;
  private PrintStream consoleOut; // Console output stream

  // Kludge alert - In order to provide a drop down list of valid requests/bookmarks
  // for a given Execution Manager, we need to be able to log in and get an API key.
  // If we only had to handle the global EM config, this would be easy. However,
  // we provide the user with a means to override the global setting per build step, but
  // the override setting only exist in the browser until the build step is saved. Fortunately,
  // Jenkins calls 'doCheckXYZ' methods as the form is loaded and when the user changes
  // values which gives us an opportunity to save those value for later reference. That (in a
  // nutshell) is the reasoning behind the following cache and the accompanying methods
  // to manipulate it.
  private static HashMap<HttpSession, HashMap<Class, HashMap<String, String>>> fieldCache =
          new HashMap<HttpSession, HashMap<Class, HashMap<String, String>>>();

  public static String updateFieldCache (Class clazz, String fieldName, String fieldValue) {
    String prevVal = null;
    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    synchronized (fieldCache) {
      HashMap<Class, HashMap<String, String>> sessionCache = fieldCache.get(session);
      if (sessionCache == null) {
        fieldCache.put(session, sessionCache = new HashMap<Class, HashMap<String, String>>());
      }

      HashMap<String, String> classCache = sessionCache.get(clazz);
      if (classCache == null) {
        sessionCache.put(clazz, classCache = new HashMap<String, String>());
      }

      prevVal = getCachedFieldValue(clazz, fieldName);
      classCache.put(fieldName, fieldValue);
      System.out.println("Updated field cache for " + clazz + "::" + fieldName + "=" + fieldValue + "(prevVal=" + prevVal + ")");
    }
    return prevVal;
  }

  public static String getCachedFieldValue (Class clazz, String fieldName) {
    String retVal = null;

    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    synchronized (fieldCache) {
      HashMap<Class, HashMap<String, String>> sessionCache = fieldCache.get(session);

      if (sessionCache != null) {
        HashMap<String, String> classCache = sessionCache.get(clazz);
        if (classCache != null) {
          retVal = classCache.get(fieldName);
        }
      }
    }
    return retVal;
  }

  static {
    // Thread to monitor the field cache and remove entries for invalid sessions
    (new Thread() {
      public void run () {
        while (true) {
          try {
            Thread.sleep(30000);
            synchronized (fieldCache) {
              for (HttpSession key : fieldCache.keySet()) {
                try {
                  Method isValidMeth = key.getClass().getMethod("isValid");
                  if (isValidMeth != null) {
                    Boolean isValid = (Boolean) isValidMeth.invoke(key);
                    if (!isValid) {
                      fieldCache.remove(key);
                      System.out.println("Expired field cache for " + key.getId());
                    }
                  }
                } catch (Exception ignored) {
                  fieldCache.put(key, null);
                  System.out.println("Exception expired field cache for " + key.getId());
                }
              }
            }
          } catch (Exception ignored) {
          }
        }
      }
    }).start();
  }

  @DataBoundConstructor
  public ExecuteRequest (String emRequestType, String request, ExecuteRequestCertifyProcessList processList, ExecuteRequestParameters execParams, ExecuteRequestWaitConfig waitConfig, ExecuteRequestEMConfig altEMConfig, ExecuteRequestPostExecute postExecute, ExecuteRequestBookmark bookmark, String url, String credentials) {
    this.url = url;
    this.credentials = credentials;

    this.emRequestType = emRequestType;
    this.bookmark = bookmark;
    this.request = request;
    this.execParams = execParams;
    this.postExecute = postExecute;
    this.waitConfig = waitConfig;
    this.altEMConfig = altEMConfig;
    this.processList = processList;
    globalConfig = GlobalConfiguration.all().get(ExecutionManagerConfig.class);

    // When we get here Jenkins is saving our form values, so we can invalidate
    // this session's fieldCache.
    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    fieldCache.put(session, null);
    System.out.println("Invalidated field cache for " + sessionId);
  }

  /**
   * Stapler methods for handling Execute Request Parameters
   */
  public boolean getExecParamsEnabled () {
    return getExecParams() != null;
  }

  public ExecuteRequestParameters getExecParams () {
    return execParams;
  }

  /**
   * Stapler methods for handling Execute Request Wait Configuration
   * }
   * /** Stapler methods for handling Execute Request Post Execute Action
   */

  public boolean getPostExecuteEnabled () {
    return getPostExecute() != null;
  }

  public ExecuteRequestPostExecute getPostExecute () {
    return postExecute;
  }

  public boolean getWaitConfigEnabled () {
    return getWaitConfig() != null;
  }

  public ExecuteRequestWaitConfig getWaitConfig () {
    return waitConfig;
  }

  /**
   * Stapler methods for handling Execute Request Alt Configuration
   */
  public boolean getAltEMConfigEnabled () {
    return getAltEMConfig() != null;
  }

  public ExecuteRequestEMConfig getAltEMConfig () {
    return altEMConfig;
  }

  public String getEmRequestType () {
    return emRequestType;
  }

  public ExecuteRequestBookmark getBookmark () {
    return bookmark;
  }

  public String getRequest () {
    return request;
  }

  public String emRequestTypeEquals (String given) {
    String ret = String.valueOf((emRequestType != null) && (emRequestType.equals(given)));
    return ret;
  }

  public ExecuteRequestCertifyProcessList getProcessList () {
    return processList;
  }

  @Extension
  public static final class ExecutionManagerBuilderDescriptor extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable (Class<? extends AbstractProject> jobType) {
      return true;
    }

    @Override
    public String getDisplayName () {
      return "Run Execution Manager Request";
    }

    public ListBoxModel doFillRequestItems (@QueryParameter String emRequestType, @QueryParameter String url, @QueryParameter String credentials) {
      ListBoxModel items = new ListBoxModel();

      // Pick the right EM configuration
      /*ExecutionManagerConfig globalConfig = GlobalConfiguration.all().get(ExecutionManagerConfig.class);
      ExecuteRequestEMConfig emConfig = globalConfig != null ? globalConfig.getEmConfig() : null;
      ExecuteRequestEMConfig altEMConfig = ExecuteRequestEMConfig.createFromFieldCache();
      if (altEMConfig != null && altEMConfig.isValid()) {
        emConfig = altEMConfig;
      }

      if (emConfig != null) {
        ExecutionManagerServer server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
        try {
          if (server.login()) {
            JSONObject reqs;

            if ((reqs = server.requests()) != null) {
              try {
                // Lookup all the requests defined on the EM and find the one specified
                // by the user
                JSONArray objs = reqs.getJSONArray("objects");
                for (int i = 0; i < objs.size(); i++) {
                  JSONObject req = objs.getJSONObject(i);
                  String name = req.getString("Name");
                  items.add(name);
                }
              } catch (Exception ignored) {
                // Bad JSON
              }
            } else {
              // couldn't get requests
            }
          } else {
            // Couldn't log in
          }
        } catch (Exception ex) {
        }
      } else {
        // No EM configuration
      }*/
      return items;
    }

    public FormValidation doCheckUrl (@QueryParameter String url) {
      FormValidation ret = FormValidation.ok();

      try {
        new URL(url);
        ExecuteRequest.updateFieldCache(getT(), "url", url);
      } catch (MalformedURLException e) {
        ret = FormValidation.error("URL is invalid " + e.getMessage());
      }

      return ret;
    }

    public FormValidation doCheckCredentials (@QueryParameter String credentials) {
      FormValidation ret = FormValidation.ok();
      ExecuteRequest.updateFieldCache(getT(), "credentials", credentials);
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
          return FormValidation.error("Authorization Failed!");
        }
      } catch (Exception e) {
        return FormValidation.error(e.getMessage());
      }

      return FormValidation.ok("Success");
    }

  }

  // Process the user provided parameters by substituting Jenkins environment
  // variables referenced in a parameter's value.
  HashMap<String, String> processParameters () throws InterruptedException, IOException {
    HashMap<String, String> ret = new HashMap<String, String>();
    EnvVars envVars = run.getEnvironment(listener);
    if (execParams != null) {
      for (ExecuteRequestParameter param : execParams.getExecParamList()) {
        String value = param.getValue();

        // dereference ALL Jenkins vars within the value string

        ret.put(param.getKey(), value);
      }
    }
    return ret;
  }

  @Override
  public void perform (@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
    // Save perform parameters in instance variables for future reference.
    this.run = run;
    this.workspace = workspace;
    this.launcher = launcher;
    this.listener = listener;
    this.consoleOut = listener.getLogger();

    // Pick the right EM configuration
    emConfig = this.globalConfig != null ? this.globalConfig.getEmConfig() : null;
    if (altEMConfig != null && altEMConfig.isValid()) {
      emConfig = getAltEMConfig();
    }

    if (emConfig != null) {
      server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
      if (server.login()) {
        // Dispatch to one of the methods below
        try {
          Method meth = this.getClass().getMethod("execute_" + emRequestType.toUpperCase().trim());
          meth.invoke(this);
        } catch (NoSuchMethodException ex) {
          listener.fatalError("Don't know how to execute '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.
        } catch (IllegalAccessException ex) {
          listener.fatalError("Couldn't execute '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.

          log.severe("ERROR: unexpected error while processing request: " + emRequestType);
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        } catch (InvocationTargetException ex) {
          listener.fatalError("Exception thrown while executing '%s'", emRequestType);
          run.setResult(Result.FAILURE); // Fail this build step.
        }
      } else {
        EmResult result = server.getLastEMResult();
        listener.fatalError("Can't log in to '%s' - %s",
                emConfig.getUrl(), result.getResponseData());
        log.log(Level.SEVERE, "Bad credentials!"); // Ends up in the log
        run.setResult(Result.FAILURE); // Fail this build step.
        //throw new RuntimeException("Bad credentials!"); // In console output end build status to fail
      }
    } else {
      listener.fatalError("An Execution Manager configuration must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    }

    if (run.getResult() != Result.FAILURE) {
      // wait for completion
      consoleOut.println("Console output test - Hello world");
    }
  }

  // Called via reflection from the dispatcher above
  public void execute_REQUEST () throws InterruptedException, IOException {
    if (StringUtils.isEmpty(request)) {
      listener.fatalError("A request name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String reqID = null;
      String theReq = request.trim();
      JSONObject reqs;

      if ((reqs = server.requests()) != null) {
        try {
          // Lookup all the requests defined on the EM and find the one specified
          // by the user
          JSONArray objs = reqs.getJSONArray("objects");
          for (int i = 0; i < objs.size(); i++) {
            JSONObject req;
            if ((req = objs.getJSONObject(i)).getString("Name").equals(theReq) ||
                    req.getString("RequestID").equals(theReq)) {
              reqID = req.getString("RequestID");
              break;
            }
          }
        } catch (Exception ex) {
          log.severe("ERROR: unexpected error during execute_REQUEST:");
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        }
      }
      if (reqID == null) {
        listener.fatalError("No such request '%s'!", theReq);
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        //server.executeRequest(reqID, processParameters());
      }
    }
  }

  public void execute_BOOKMARK () throws InterruptedException, IOException {
    if (bookmark == null || StringUtils.isEmpty(bookmark.getBookmark())) {
      listener.fatalError("A bookmark name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String bmarkID = null;
      String theBmark = bookmark.getBookmark().trim();
      JSONObject bmarks;

      if ((bmarks = server.bookmarks()) != null) {
        try {
          // Lookup all the bookmarks defined on the EM and find the one specified
          // by the user
          JSONArray objs = bmarks.getJSONArray("objects");
          for (int i = 0; i < objs.size(); i++) {
            JSONObject bmark;
            if ((bmark = objs.getJSONObject(i)).getString("Name").equals(theBmark) ||
                    bmark.getString("Id").equals(theBmark)) {
              bmarkID = bmark.getString("Id");
              break;
            }
          }
        } catch (Exception ex) {
          log.severe("ERROR: unexpected error during execute_BOOKMARK");
          log.severe("ERROR: exception: " + ex);
          log.severe("ERROR: exception: " + ex.getMessage());
          log.severe("ERROR: stack trace:  ");
          StackTraceUtils.printSanitizedStackTrace(ex.getCause());
        }
      }
      if (bmarkID == null) {
        listener.fatalError("No such bookmark '%s'!", theBmark);
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        //server.executeBookmark(bmarkID, processParameters(), bmark.getFolder());
      }
    }
  }

  public void execute_PROCESSLIST () throws InterruptedException, IOException {

  }
}
