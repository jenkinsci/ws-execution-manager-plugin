/*
 * Copyright (c) 2018 Worksoft, Inc.
 *
 * EmAuth
 *
 * @author rrinehart on Thu, 18 Oct 2018
 */

package com.worksoft.jenkinsci.plugins.em.model;

import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class EmAuth {

  private Map<Keys, String> token;

  EmAuth () {
    token = new HashMap<>();
  }

//  boolean save (Map<Keys, String> auth) {
    boolean save (JSONObject auth) {

    for (Keys key : Keys.values()) {
      String v = auth.getString(key.getKV());

      if (v != null) {
        token.put(key, v);
      }
    }
    return true;
  }

  String getAccess_token () {
    return token.get(Keys.access_token);
  }

  String getToken_type () {
    return token.get(Keys.token_type);
  }

  String getExpires_in () {
    return token.get(Keys.expires_in);
  }

  String getIssued () {
    return token.get(Keys.issued);
  }

  String getExpires () {
    return token.get(Keys.expires);
  }

  boolean isLoggedIn () {
    return getAccess_token()  != null;
  }

  @Override
  public String toString () {
    return "EmsAuth{" +
            "access_token='" + getAccess_token()+ '\'' +
            ", token_type='" + getToken_type() + '\'' +
            ", expires='" + getExpires()+ '\'' +
            ", expires_in='" + getExpires_in() + '\'' +

            ", issued='" + getIssued()+ '\'' +
            '}';
  }

  static enum Keys {
    access_token("access_token"),
    token_type("token_type"),
    expires(".expires"),
    expires_in("expires_in"),
    issued(".issued");

    final String kv;

    Keys (String key) {
      this.kv = key;
    }

    String getKV () {
      return this.kv;
    }

  }
}
