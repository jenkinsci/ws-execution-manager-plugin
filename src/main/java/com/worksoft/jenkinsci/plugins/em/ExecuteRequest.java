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
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ExecuteRequest extends Builder implements SimpleBuildStep {
  private static final Logger log = Logger.getLogger("jenkins.ExecuteRequest");

  public class ConsoleStream extends PrintStream {
    public ConsoleStream (OutputStream out) {
      super(out);
    }

    @Override
    public void println (String string) {
      Date now = new Date();
      DateFormat dateFormatter = DateFormat.getDateTimeInstance(
              DateFormat.SHORT,
              DateFormat.MEDIUM,
              Locale.getDefault());
      Scanner scanner = new Scanner(string);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        super.println("[" + (dateFormatter.format(now)) + "] " + line);
      }
      scanner.close();
    }

    public void printlnIndented (String indent, String string) {
      Scanner scanner = new Scanner(string);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        println(indent + line);
      }
      scanner.close();
    }

    public void printlnIndented (String indent, Object[] objects) {
      for (Object obj : objects) {
        printlnIndented(indent, obj.toString());
      }
    }
  }

  // The following instance variables are those provided by the GUI
  private String emRequestType;
  private ExecuteRequestBookmark bookmark;
  private ExecuteRequestRequest request;
  private ExecuteRequestCertifyProcessList processList;
  private ExecuteRequestPostExecute postExecute;
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
  private ConsoleStream consoleOut; // Console output stream

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

    // When we get here Jenkins is saving our form values, so we can invalidate
    // this session's itemsCache.
    invalidateItemsCache();
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
  private HashMap<String, String> processParameters () throws InterruptedException, IOException {
    HashMap<String, String> ret = new HashMap<String, String>();
    EnvVars envVars = run.getEnvironment(listener);
    if (execParams != null) {
      for (ExecuteRequestParameter param : execParams.getExecParamList()) {
        String value = param.getValue();

        if (StringUtils.isNotEmpty(param.getKey()) &&
                StringUtils.isNotEmpty(value)) {
          // dereference ALL Jenkins vars within the value string

          ret.put(param.getKey(), value);
        }
      }
    }
    return ret;
  }

  private void waitForCompletion (String guid) {
    boolean aborted = false;

    // Setup timing variables
    Long maxRunTime = waitConfig == null ? null : waitConfig.maxRunTimeInMillis();
    if (maxRunTime == null) {
      // Default to 1 year maximum run time
      maxRunTime = TimeUnit.MILLISECONDS.convert(365L, TimeUnit.DAYS);
    }
    Long pollInterval = waitConfig == null ? null : waitConfig.pollIntervalInMillis();
    if (pollInterval == null) {
      // Default to 15 second poll interval
      pollInterval = TimeUnit.MILLISECONDS.convert(15L, TimeUnit.SECONDS);
    }

    // Stuff for computing elapsed time
    long startTime = System.currentTimeMillis();
    long currentTime = startTime;
    long endTime = (startTime + maxRunTime);
    SimpleDateFormat elapsedFmt = new SimpleDateFormat("HH:mm:ss.SSS");
    elapsedFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    String elapsedTime = elapsedFmt.format(new Date(currentTime - startTime));

    // loop until complete/aborted
    consoleOut.println("Waiting for execution to complete...");
    while (true) {
      EmResult statusResult = server.executionStatus(guid);

      if (!statusResult.is200()) {
        consoleOut.println("\n*** ERROR: EM error while checking execution status:");
        consoleOut.printlnIndented("*** ERROR:   ", statusResult.dumpDebug());
        break;
      }

      JSONObject response = statusResult.getJsonData();
      try {
        String jobStatus = response.getString("Status");
        String jobExecutionStatus = response.getString("ExecutionStatus");
        consoleOut.println("\nElapsed time=" + elapsedTime + " - " + jobStatus + "," + jobExecutionStatus + (aborted ? " *** ABORTING ***" : ""));
        JSONArray tasks = response.getJSONArray("Tasks");

        // Print the run's status to the build console
        consoleOut.println("Name            ExecStatus Resource        LastError       Status");
        consoleOut.println("--------------- ---------- --------------- --------------- --------------------");
        for (int i = 0; i < tasks.size(); i++) {
          JSONObject task = tasks.getJSONObject(i);
          String name = task.getString("Name");
          String executionStatus = task.getString("ExecutionStatus");
          String resourceName = task.getString("ResourceName");
          String lastReportedError = task.getString("LastReportedError");
          String status = task.getString("Status");
          consoleOut.println(String.format("%-15.15s %-10.10s %-15.15s %-15.15s %-20.20s",
                  StringUtils.abbreviate(name, 15),
                  StringUtils.abbreviate(executionStatus, 10),
                  StringUtils.abbreviate(resourceName, 15),
                  StringUtils.abbreviate(lastReportedError, 15),
                  StringUtils.abbreviate(status, 20)));
        }

        // Check for completion
        if (jobStatus.toUpperCase().equals("COMPLETED")) {
          if (!aborted && jobExecutionStatus.toUpperCase().equals("FAILED")) {
            run.setResult(Result.FAILURE);
          } else if (jobExecutionStatus.toUpperCase().equals("PASSED")) {
            run.setResult(Result.SUCCESS);
          }
          break;
        }
      } catch (JSONException e) {
        // JSON badness
        consoleOut.println("\n*** ERROR: unexpected error while processing status");
        consoleOut.println("*** ERROR: exception: " + e);
        consoleOut.println("*** ERROR: exception: " + e.getMessage());
        consoleOut.println("*** ERROR: stack trace:  ");
        consoleOut.printlnIndented("*** ERROR:    ", e.getStackTrace());
      }
      try {
        Thread.sleep(pollInterval);
        currentTime = System.currentTimeMillis();
        elapsedTime = elapsedFmt.format(new Date(currentTime - startTime));
        if (maxRunTime != null && currentTime >= endTime) {
          if (aborted) {
            consoleOut.println("\n*** ERROR: Abort timed out!!! - abandoning...");
          } else {
            consoleOut.println("\n*** ERROR: Execution timed out after " + elapsedTime + " - aborting...");


            EmResult result = server.executionAbort(guid);
            if (!result.is200()) {
              consoleOut.println("\n*** ERROR: EM error aborting execution:");
              consoleOut.printlnIndented("*** ERROR:   ", result.dumpDebug());
            }

            run.setResult(Result.ABORTED);
          }
        }
      } catch (InterruptedException e) {
        consoleOut.println("\n*** ERROR: User requested aborted of execution after " + elapsedTime);

        // Tell the EM to abort execution
        EmResult result = server.executionAbort(guid);
        if (!result.is200()) {
          consoleOut.println("\n*** ERROR: Error aborting execution:");
          consoleOut.printlnIndented("*** ERROR:   ", result.dumpDebug());
        }

        run.setResult(Result.ABORTED);
      }
      if (run.getResult() == Result.ABORTED) {
        // Once we tell the EM we'll wait six cycles for up to 60 seconds
        pollInterval = TimeUnit.MILLISECONDS.convert(10L, TimeUnit.SECONDS);
        aborted = true;

        // Set the max run time to one minute from now in order to wait for EM to complete
        // the abort.
        maxRunTime = TimeUnit.MILLISECONDS.convert(60L, TimeUnit.SECONDS);
        endTime = (currentTime + maxRunTime);
      }
    }

    consoleOut.println("\n\nExecution " + run.getResult().toString() + " after - " + elapsedTime);
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
    this.consoleOut = new ConsoleStream(listener.getLogger());

    ExecutionManagerConfig globalConfig = GlobalConfiguration.all().get(ExecutionManagerConfig.class);

    // Pick the right EM configuration
    emConfig = globalConfig != null ? globalConfig.getEmConfig() : null;
    if (altEMConfig != null && altEMConfig.isValid()) {
      emConfig = getAltEMConfig();
    }

    String guid = null;

    try {
      if (emConfig != null && emConfig.isValid()) {
        server = new ExecutionManagerServer(emConfig.getUrl(), emConfig.lookupCredentials());
        if (server.login()) {
          // Dispatch to one of the methods below
          try {
            String methName = "execute_" + emRequestType.toUpperCase().trim();
            Method meth = this.getClass().getDeclaredMethod(methName);
            guid = (String) meth.invoke(this);
          } catch (NoSuchMethodException ex) {
            consoleOut.println("\n*** ERROR: Don't know how to execute '" + emRequestType + "'");
            run.setResult(Result.FAILURE); // Fail this build step.
          } catch (IllegalAccessException ex) {
            consoleOut.println("\n*** ERROR: Couldn't execute '" + emRequestType + "'");
            consoleOut.println("*** ERROR: unexpected error while processing request: " + emRequestType);
            consoleOut.println("*** ERROR: exception: " + ex);
            consoleOut.println("*** ERROR: exception: " + ex.getMessage());
            consoleOut.println("*** ERROR: stack trace:  ");
            consoleOut.printlnIndented("*** ERROR:   ", ex.getStackTrace());

            run.setResult(Result.FAILURE); // Fail this build step.
          } catch (InvocationTargetException ex) {
            consoleOut.println("*** ERROR: Exception thrown while executing '" + emRequestType + "'");
            run.setResult(Result.FAILURE); // Fail this build step.
          }
        } else {
          EmResult result = server.getLastEMResult();
          consoleOut.println("\n*** ERROR: Can't log in to '" + emConfig.getUrl() + "':");
          consoleOut.printlnIndented("*** ERROR:   ", result.getResponseData());
          run.setResult(Result.FAILURE); // Fail this build step.
        }
      } else {
        consoleOut.println("\n*** ERROR: A valid Execution Manager configuration must be specified!");
        run.setResult(Result.FAILURE); // Fail this build step.
      }
    } catch (Exception ex) {
      consoleOut.println("\n*** ERROR: Unexpected error while processing request type: " + emRequestType);
      consoleOut.println("*** ERROR: exception: " + ex);
      consoleOut.println("*** ERROR: exception: " + ex.getMessage());
      consoleOut.println("*** ERROR: stack trace:  ");
      consoleOut.printlnIndented("*** ERROR:   ", ex.getStackTrace());

      run.setResult(Result.FAILURE); // Fail this build step.
    }
    if (run.getResult() != Result.FAILURE) {
      if (guid == null) {
        consoleOut.println("\n*** ERROR: An unexpected error occurred while requesting execution!");
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        waitForCompletion(guid);
      }
    }
  }

  // Called via reflection from the dispatcher above to execute a 'request'
  public String execute_REQUEST () throws InterruptedException, IOException {
    String guid = null;
    if (StringUtils.isEmpty(request.getRequest())) {
      consoleOut.println("\n*** ERROR: A request name or ID must be specified!");
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
          consoleOut.println("\n*** ERROR: unexpected error during execute_REQUEST:");
          consoleOut.println("*** ERROR: unexpected error while processing request: " + emRequestType);
          consoleOut.println("*** ERROR: exception: " + ex);
          consoleOut.println("*** ERROR: exception: " + ex.getMessage());
          consoleOut.println("*** ERROR: stack trace:  ");
          consoleOut.printlnIndented("*** ERROR:   ", ex.getStackTrace());
          run.setResult(Result.FAILURE); // Fail this build step.
        }
      }
      if (reqID == null) {
        consoleOut.println("\n*** ERROR: No such request '" + theReq + "'!");
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        consoleOut.println("Requesting execution of request '" + theReq + "'(id=" + reqID + ")");
        consoleOut.println("   on Execution Manager @ " + emConfig.getUrl());
        HashMap<String, String> params = processParameters();
        if (params.keySet().size() > 0) {
          consoleOut.println("   with parameters (key=value):");
          for (String key : params.keySet()) {
            consoleOut.println("      " + key + "=" + params.get(key));
          }
        }
        consoleOut.println("\n");
        guid = server.executeRequest(reqID, processParameters());
        if (guid == null) {
          consoleOut.println("\n*** ERROR: Request to execute '" + theReq + " failed:");
          consoleOut.printlnIndented("   ", server.getLastEMResult().dumpDebug());
        }
      }
    }
    return guid;
  }

  // Called via reflection from the dispatcher above to execute a 'bookmark'
  public String execute_BOOKMARK () throws InterruptedException, IOException {
    String guid = null;
    if (bookmark == null || StringUtils.isEmpty(bookmark.getBookmark())) {
      consoleOut.println("\n*** ERROR: A bookmark name or ID must be specified!");
      run.setResult(Result.FAILURE); // Fail this build step.
    } else {
      String bmarkID = null;
      String theBmark = bookmark.getBookmark().trim();
      JSONObject bmarks;

      if ((bmarks = server.bookmarks()) != null) {
        try {
          // Lookup all the bookmarks defined on the EM and find the one specified
          // by the user.
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
          consoleOut.println("\n*** ERROR: unexpected error during execute_REQUEST:");
          consoleOut.println("*** ERROR: unexpected error while processing request: " + emRequestType);
          consoleOut.println("*** ERROR: exception: " + ex);
          consoleOut.println("*** ERROR: exception: " + ex.getMessage());
          consoleOut.println("*** ERROR: stack trace:  ");
          consoleOut.printlnIndented("*** ERROR:   ", ex.getStackTrace());
          run.setResult(Result.FAILURE); // Fail this build step.
        }
      }
      if (bmarkID == null) {
        consoleOut.println("\n*** ERROR: No such bookmark '" + theBmark + "'!");
        run.setResult(Result.FAILURE); // Fail this build step.
      } else {
        consoleOut.println("Requesting execution of bookmark '" + theBmark + "'(id=" + bmarkID + ")");
        consoleOut.println("   on Execution Manager @ " + emConfig.getUrl());
        if (StringUtils.isNotEmpty(bookmark.getFolder())) {
          consoleOut.println("   with results folder='" + bookmark.getFolder() + "'");
        }
        HashMap<String, String> params = processParameters();
        if (params.keySet().size() > 0) {
          consoleOut.println("   with parameters (key=value):");
          for (String key : params.keySet()) {
            consoleOut.println("      " + key + "=" + params.get(key));
          }
        }
        consoleOut.println("\n");
        guid = server.executeBookmark(bmarkID, bookmark.getFolder(), params);
        if (guid == null) {
          consoleOut.println("\n*** ERROR: Request to execute bookmark failed:");
          consoleOut.printlnIndented("   ", server.getLastEMResult().dumpDebug());
        }
      }
    }
    return guid;
  }

  // Called via reflection from the dispatcher above to execute a 'process list'
  private String execute_PROCESSLIST () throws InterruptedException, IOException {
    return null;
  }
}
