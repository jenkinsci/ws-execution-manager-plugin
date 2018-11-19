/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerServer
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.model;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import jodd.http.HttpException;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

public class ExecutionManagerServer {

  private static final Logger log = Logger.getLogger("jenkins.ExecutionManager");

  private final String url;
  private final UsernamePasswordCredentials credentials;

  private EmAuth auth;

  private EmResult lastEMResult;

  public EmResult getLastEMResult () {
    return lastEMResult;
  }

  public ExecutionManagerServer (String url, UsernamePasswordCredentials credentials) {
    if (credentials == null) {
      throw new RuntimeException("Credentials must be provided!");
    }

    try {
      URL foo = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Execution Manager URL is invalid " + e.getMessage());
    }

    if (!url.endsWith("/")) {
      this.url = url + "/";
    } else {
      this.url = url;
    }

    this.credentials = credentials;
  }

  public boolean login () throws UnsupportedEncodingException {
    HttpRequest httpRequest = HttpRequest.post(url + "api/Token")
            .contentType("application/x-www-form-urlencoded")
            .header("Authorization", "OAuth2")
            .form("grant_type", "password",
                    "username", credentials.getUsername(),
                    "password", credentials.getPassword().getPlainText());

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      auth = new EmAuth();
      auth.save(result.getJsonData());
    }
    return result.is200();
  }

  public JSONObject bookmarks () {
    HttpRequest httpRequest = HttpRequest.get(url + "api/Bookmarks")
            .header("jsonOrXml", "json");
    httpRequest.body("");

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      return result.getJsonData();
    }

    return null;
  }

  public JSONObject requests () {
    HttpRequest httpRequest = HttpRequest.get(url + "api/Requests")
            .header("jsonOrXml", "json");
    httpRequest.body("");

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      return result.getJsonData();
    }

    return null;
  }

  public String escapeParameter (String value) {
    // Escape the value... In short, these values are passed to Certify as
    // command line arguments with in double quotes, so we should escape user input
    // double quotes and backslashes appropriately. BTW - This is misplaced
    // logic that should be performed by the EM.
    value = value.replaceAll("\\\\", "\\\\\\\\");
    value = value.replaceAll("\"", "\\\\\"");

    return value;
  }

  public String sanitizeParameter (String value) {
    // Certify's syntax for these parameters is - <key>|<value>, so if the
    // value has a pipe (|), it'll mess up Certify's parsing. BTW - This is misplaced
    // logic that should be performed by the EM.
    value = value.replaceAll("\\|", "");
    // The EM has no way of escaping curly braces, so we'll remove them or it'll
    // mess with the EM's parsing algorithm
    value = value.replaceAll("[{}]", "");

    return value;
  }

  // Format the provided hash map into a string format acceptable to the EM API
  private String formatParameters (Map<String, String> parameters) {
    String params = "";
    for (String key : parameters.keySet()) {
      String value = parameters.get(key);

      params += "{" + key + "}";
      params += "{" + escapeParameter(sanitizeParameter(value)) + "}";
    }
    return params;
  }

  public String executeRequest (String request, Map<String, String> parameters) {
    HttpRequest httpRequest = HttpRequest.put(url + "api/ExecuteRequest")
            .header("id", request)
            .header("parameters", formatParameters(parameters))
            .header("jsonOrXml", "json");
    httpRequest.body("");
    String guid = null;

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      String response = result.getResponseData();
      if (response.length() >= 2 && response.charAt(0) == '"' && response.charAt(response.length() - 1) == '"') {
        guid = response.substring(1, response.length() - 1);
      } else {
        guid = response;
      }
    }

    return guid;
  }

  public String executeBookmark (String bookmark, String folder, Map<String, String> parameters) {
    HttpRequest httpRequest = HttpRequest.put(url + "api/Bookmarks/" + bookmark + "/Execute" +
            (StringUtils.isNotEmpty(folder) ? "?folder=" + folder : ""))
            .header("parameters", formatParameters(parameters))
            .header("jsonOrXml", "json");
    httpRequest.body("");
    String guid = null;

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      String response = result.getResponseData();
      if (response.length() >= 2 && response.charAt(0) == '"' && response.charAt(response.length() - 1) == '"') {
        guid = response.substring(1, response.length() - 1);
      } else {
        guid = response;
      }
    }

    return guid;
  }

  public String executeProcesses (JSONObject processes, Map<String, String> parameters) {
    HttpRequest httpRequest = HttpRequest.put(url + "api/Processes/Execute")
            .header("parameters", formatParameters(parameters))
            .header("jsonOrXml", "json")
            .header("content-type", "application/json");
    httpRequest.body(JSONUtils.valueToString(processes, 4, 0));
    String guid = null;

    EmResult result = sendRequest(httpRequest);

    if (result.is200()) {
      String response = result.getResponseData();
      if (response.length() >= 2 && response.charAt(0) == '"' && response.charAt(response.length() - 1) == '"') {
        guid = response.substring(1, response.length() - 1);
      } else {
        guid = response;
      }
    }

    return guid;
  }


  public EmResult executionStatus (String guid) {

    HttpRequest httpRequest = HttpRequest.get(url + "api/ExecutionStatus")
            .header("APIRequestID ", guid)
            .header("jsonOrXml", "json");

    EmResult result = sendRequest(httpRequest);

    return result;
  }

  public EmResult executionAbort (String guid) {

    HttpRequest httpRequest = HttpRequest.put(url + "api/Execution/" + guid + "/Abort")
            .header("id", guid)
            .header("jsonOrXml", "json");

    httpRequest.body("");

    EmResult result = sendRequest(httpRequest);

    return result;
  }

  private EmResult sendRequest (HttpRequest request) throws HttpException {

    EmResult result;


    try {
      if (auth != null) {
        request.tokenAuthentication(auth.getAccess_token());
      }

      HttpResponse response = request.send();
      result = lastEMResult = new EmResult(response);
      if (result.is200()) {
//      status = true;

      } else if (result.statusCode() == 401) {
//      status = false;
        throw new Exception("Unauthorized");
//      payload = Unauthorized;
      } else {

        log.warning("ExecutionManager request failed " + response.toString(true));
//      status = false;
//      if (payload != null) {
//        payload = response.statusPhrase();
//      }
      }

    } catch (Throwable t) {
      result = new EmResult(null);
      log.severe("ERROR: unexpected error while processing request: " + request);
      log.severe("ERROR: exception: " + t);
      log.severe("ERROR: exception: " + t.getMessage());
      log.severe("ERROR: stack trace:  ");
      StackTraceUtils.printSanitizedStackTrace(t.getCause());
      throw new HttpException(t);
    }

    return result;
  }
}
