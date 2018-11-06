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
import org.codehaus.groovy.runtime.StackTraceUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

  // Format the provided hash map into a string format acceptable to the EM API
  private String formatParameters (HashMap<String, String> parameters) {
    return "";
  }

  public String executeRequest (String request, HashMap<String, String> parameters) {
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

  public String executeBookmark (String bookmark, String folder, HashMap<String, String> parameters) {
    HttpRequest httpRequest = HttpRequest.put(url + "api/Bookmarks/" + bookmark + "/Execute")
            .header("folder", "")
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

  public EmStatus executionStatus (String guid) {

    HttpRequest httpRequest = HttpRequest.get(url + "api/ExecutionStatus")
            .header("ExecutionHistoryID", guid)
            .header("jsonOrXml", "json");

    EmStatus status = EmStatus.Running;

    try {
      EmResult result = sendRequest(httpRequest);

      if (result.isOkAndHasResponse()) {
        JSONObject execStatus = result.getJsonData();
      } else if (result.isUnauthorized()) {
        status = EmStatus.Error;
      } else {
        status = EmStatus.Unknown;
        log.warning("EM status query failed: " + result.dumpDebug());
        // log response data
        // check for errors here
      }

    } catch (Throwable ignored) {
      status = EmStatus.Error;
    }

    return status;
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
