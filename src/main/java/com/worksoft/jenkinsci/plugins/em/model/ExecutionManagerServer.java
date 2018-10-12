/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * ExecutionManagerServer
 *
 * @author rrinehart on Wed, 19 Sep 2018
 */

package com.worksoft.jenkinsci.plugins.em.model;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;

import java.net.URL;

public class ExecutionManagerServer {


  private final URL url;
  private final UsernamePasswordCredentials credentials;
  private final String name;

  public ExecutionManagerServer (URL url, String name, UsernamePasswordCredentials credentials) {
    this.url = url;
    this.credentials = credentials;
    this.name = name;
  }
}
