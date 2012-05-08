/**
 * This file is part of Picasa Photo Uploader.
 *
 * Picasa Photo Uploader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Picasa Photo Uploader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Picasa Photo Uploader. If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.picasaphotouploader;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Try to authenticate at google with user email and password to
 * get authentication string for uploading images
 *
 * @author Jan Peter Hooiveld
 */
public class GoogleAuthentication
{
  /**
   * User google e-mail
   */
  String email;

  /**
   * User google password
   */
  String password;

  /**
   * Google authentication string
   */
  String auth = null;
  
  Context context;

  /**
   * Constructor
   *
   * @param prefs User preferences
   */
  public GoogleAuthentication(SharedPreferences prefs, Context context)
  {
    email    = prefs.getString("email", "");
    password = prefs.getString("password", "");
    this.context = context;
  }

  public GoogleAuthentication(String email, String pass, Context context)
  {
    this.email    = email;
    password = pass;
    this.context = context;
  }
  
  /**
   * Try to get google authentication string for user e-mail and password.
   * We need authentication string to authenticate uploads for images
   *
   * @return Google authentication string
   */
  public String getAuthenticationString()
  {
    try {
      // check if user set email and password in preferences
     /* if (email.trim().length() == 0 || password.trim().length() == 0) {
    	  Toast.makeText(context, email.trim() + " + " + password.trim(), Toast.LENGTH_SHORT).show();
        return null;
      }*/

      // url to authenticate on
      String authUrl  = "https://www.google.com/accounts/ClientLogin?accountType=GOOGLE&Email="+email+"&Passwd="+password+"&service=lh2&source=PicasaUploader";

      // create ssl context
      SSLContext sc = SSLContext.getInstance("TLS");

      // override ssl context with our own class, otherwise ssl will fail
      sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());

      // create url connection
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());
      HttpsURLConnection con = (HttpsURLConnection) new URL(authUrl).openConnection();

      // set timeout and that we do  output
      con.setReadTimeout(15000);
      con.setDoOutput(true);
      con.connect();

      // read response from url and accumulate body
      BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
      StringBuffer sb   = new StringBuffer();
      String line;
      
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

      // create body and check if it containts auth string
      String body   = sb.toString();
      Integer index = body.indexOf("Auth=");
      // if body contains auth string save it
      if (index != -1) {
        auth = body.substring(index + 5);
      }
    } catch (Exception e) {
    	e.printStackTrace();
    }

     // return auth for further use in the application
    return auth;
  }

  /**
   * Override class so SSL will work
   */
  private class MyHostnameVerifier implements HostnameVerifier
  {
    @Override
    public boolean verify(String hostname, SSLSession session)
    {
      return true;
    }
  }

  /**
   * Override class so SSL will work
   */
  private class MyTrustManager implements X509TrustManager
  {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
    {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
    {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
      return null;
    }
  }
}
