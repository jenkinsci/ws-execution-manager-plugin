/*
 *
 * Copyright (c) 2018 - 2018 Worksoft, Inc.
 *
 * ${CLASS_NAME}
 *
 * @author rrinehart on 9/14/2018
 */

package com.worksoft.jenkinsci.plugins.em;

import com.worksoft.jenkinsci.plugins.em.config.ExecutionManagerConfig;
import com.worksoft.jenkinsci.plugins.em.model.EmResult;
import com.worksoft.jenkinsci.plugins.em.model.ExecutionManagerServer;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecuteRequest extends Builder implements SimpleBuildStep {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");

  // The following instance variables are those provided by the GUI
  private String emRequestType;
  private ExecuteRequestBookmark bookmark;
  private ExecuteRequestRequest request;
  private ExecuteRequestCertifyProcessList processList;
  private ExecuteRequestPostExecute postExecute;
  private ExecutionManagerConfig globalConfig;
  private ExecuteRequestEMConfig altEMConfig;
  private ExecuteRequestWaitConfig waitConfig;
  private ExecuteRequestParameters execParams;

  // These instance variables are those used during execution
  private ExecuteRequestEMConfig emConfig; // EM config used during run
  private ExecutionManagerServer server;
  private Run<?, ?> run;
  private FilePath workspace;
  private Launcher launcher;
  private TaskListener listener;
  private PrintStream consoleOut; // Console output stream

  // Kludge alert - In order to fill the request/bookmark list box with values from
  // the EM and to provided the user with appropriate feedback, we need to cache
  // the list box items. We wouldn't need to do this if the 'doCheck' methods were
  // provided with the EM configuration variables so as to be able to validate them.
  // Unfortunately, does not provide their values in a consistent manner, so we
  // use this cache to remember the items from the 'doFill' methods; which we can then
  // access in the 'doCheck' methods for proper field validation and error display.
  private static HashMap<HttpSession, HashMap<String, ListBoxModel>> itemsCache =
          new HashMap<HttpSession, HashMap<String, ListBoxModel>>();

  public static ListBoxModel updateItemsCache (String fieldName, ListBoxModel items) {
    ListBoxModel prevVal = null;
    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    synchronized (itemsCache) {
      HashMap<String, ListBoxModel> sessionCache = itemsCache.get(session);
      if (sessionCache == null) {
        itemsCache.put(session, sessionCache = new HashMap<String, ListBoxModel>());
      }

      prevVal = getCachedItems(fieldName);
      sessionCache.put(fieldName, items);
      System.out.println("Updated items cache for " + fieldName + "=" + items + "(prevVal=" + prevVal + ")");
    }
    return prevVal;
  }

  public static ListBoxModel getCachedItems (String fieldName) {
    ListBoxModel retVal = null;

    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    synchronized (itemsCache) {
      HashMap<String, ListBoxModel> sessionCache = itemsCache.get(session);

      if (sessionCache != null) {
        retVal = sessionCache.get(fieldName);
      }
    }
    return retVal;
  }

  public static void invalidateItemsCache () {
    HttpServletRequest httpRequest = Stapler.getCurrentRequest();
    HttpSession session = httpRequest.getSession();
    String sessionId = session.getId();
    itemsCache.put(session, null);
    System.out.println("Invalidated items cache for " + sessionId);
  }

  static {
    // Thread to monitor the field cache and remove entries for invalid sessions
    (new Thread() {
      public void run () {
        while (true) {
          try {
            Thread.sleep(30000);
            synchronized (itemsCache) {
              for (HttpSession key : itemsCache.keySet()) {
                try {
                  Method isValidMeth = key.getClass().getMethod("isValid");
                  if (isValidMeth != null) {
                    Boolean isValid = (Boolean) isValidMeth.invoke(key);
                    if (!isValid) {
                      itemsCache.remove(key);
                      System.out.println("Expired field cache for " + key.getId());
                    }
                  }
                } catch (Exception ignored) {
                  itemsCache.put(key, null);
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


  Map<String, Execute> handlers = new HashMap<>();
  @DataBoundConstructor
  public ExecuteRequest (String emRequestType, ExecuteRequestRequest request, ExecuteRequestCertifyProcessList processList, ExecuteRequestParameters execParams, ExecuteRequestWaitConfig waitConfig, ExecuteRequestEMConfig altEMConfig, ExecuteRequestPostExecute postExecute, ExecuteRequestBookmark bookmark) {
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
    // this session's itemsCache.
    invalidateItemsCache();
    handlers.put("request", byRequest);
  }

  public boolean getExecParamsEnabled () {
    return getExecParams() != null;
  }

  public ExecuteRequestParameters getExecParams () {
    return execParams;
  }

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
    // When we get here Jenkins is loading our form values, so we can invalidate
    // this session's itemsCache.
    invalidateItemsCache();

    return bookmark;
  }

  public ExecuteRequestRequest getRequest () {
    // When we get here Jenkins is loading our form values, so we can invalidate
    // this session's itemsCache.
    invalidateItemsCache();

    return request;
  }

  public ExecuteRequestCertifyProcessList getProcessList () {
    return processList;
  }

  // Call from the jelly to determine whether radio block is checked
  public String emRequestTypeEquals (String given) {
    return String.valueOf((emRequestType != null) && (emRequestType.equals(given)));
  }

  @Symbol("execMan")
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
  }

  // Used by doFillRequestItems and doFillBookmarkItems (See ExecuteRequestBookmark class)
  public static ListBoxModel fillItems (String emRequestType, String url, String credentials) {
    ListBoxModel items = new ListBoxModel();

    // Pick the right EM configuration
    ExecutionManagerConfig globalConfig = GlobalConfiguration.all().get(ExecutionManagerConfig.class);
    ExecuteRequestEMConfig emConfig = globalConfig != null ? globalConfig.getEmConfig() : null;
    ExecuteRequestEMConfig altEMConfig = new ExecuteRequestEMConfig(url, credentials);
    if (altEMConfig != null && altEMConfig.isValid()) {
      emConfig = altEMConfig;
    }

    if (emConfig != null) {
      ExecutionManagerServer server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
      try {
        if (server.login()) {
          JSONObject retrievedObjs;
          if (emRequestType.equals("request")) {
            retrievedObjs = server.requests();
          } else {
            retrievedObjs = server.bookmarks();
          }
          if (retrievedObjs != null) {
            try {
              items.add("-- Select a " + emRequestType + " --"); // Add blank entry first

              // Lookup all the requests defined on the EM and find the one specified
              // by the user
              JSONArray objs = retrievedObjs.getJSONArray("objects");
              for (int i = 0; i < objs.size(); i++) {
                JSONObject req = objs.getJSONObject(i);
                String name = req.getString("Name");
                items.add(name);
              }
            } catch (Exception ignored) {
              // Bad JSON
              items.add("*** ERROR ***", "ERROR: Bad JSON");
              items.get(items.size() - 1).selected = true;
            }
          } else {
            // couldn't get requests
            items.add("*** ERROR ***", "ERROR: Couldn't retrieve " + emRequestType + "s");
            items.get(items.size() - 1).selected = true;
          }
        } else {
          // Couldn't log in
          items.add("*** ERROR ***", "ERROR: Couldn't log in");
          items.get(items.size() - 1).selected = true;
        }
      } catch (Exception ex) {
        // Exception while logging in
        items.add("*** ERROR ***", "ERROR: Exception while logging in");
        items.get(items.size() - 1).selected = true;
      }
    } else {
      // No EM configuration
      items.add("*** ERROR ***", "ERROR: No EM configuration");
      items.get(items.size() - 1).selected = true;
    }

    updateItemsCache(emRequestType, items);

    return items;
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

  // This method is called by Jenkins to perform the build step. It sets up some instance
  // variables, logs in to the EM and dispatches the execute to methods that follow
  // using reflection.
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

    String guid = null;

    if (emConfig != null) {
      server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
      if (server.login()) {
        // Dispatch to one of the methods below
        try {
          Method meth = this.getClass().getMethod("execute_" + emRequestType.toUpperCase().trim());
          guid = (String)meth.invoke(this);
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
      if (guid != null) {
        server.executionStatus(guid);
      }
      // wait for completion
      consoleOut.println("Console output test - Hello world");
    }
  }

  Execute byRequest = this::execute_REQUEST;

  // Called via reflection from the dispatcher above to execute a 'request'
  public String execute_REQUEST () throws InterruptedException, IOException {
    String guid = null;
    if (StringUtils.isEmpty(request.getRequest())) {
      listener.fatalError("A request name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String reqID = null;
      String theReq = request.getRequest().trim();
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
        guid = server.executeRequest(reqID, processParameters());
      }
    }
    return guid;
  }

  // Called via reflection from the dispatcher above to execute a 'bookmark'
  public String execute_BOOKMARK () throws InterruptedException, IOException {
    String guid = null;
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
        guid = server.executeBookmark(bmarkID, bookmark.getFolder(), processParameters());
      }
    }
    return guid;
  }

  // Called via reflection from the dispatcher above to execute a 'process list'
  public String execute_PROCESSLIST () throws InterruptedException, IOException {
    return null;
  }

  interface Execute {
    String exec() throws IOException, InterruptedException;
  }
}
