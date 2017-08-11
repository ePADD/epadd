/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.email;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.security.cert.X509Certificate;

public class DummySSLSocketFactory extends SSLSocketFactory {
  private SSLSocketFactory factory;
  public DummySSLSocketFactory() {
    System.out.println( "DummySocketFactory instantiated");
    try {
    	/*
      SSLContext sslcontext = SSLContext.getInstance( "TLS");
      sslcontext.init( null, // No KeyManager required
          new TrustManager[] { new DummyTrustManager()},
          new java.security.SecureRandom());
      factory = ( SSLSocketFactory) sslcontext.getSocketFactory();
      */
    } catch( Exception ex) {
      ex.printStackTrace();
    }
  }
  public static SocketFactory getDefault() {
    return new DummySSLSocketFactory();
  }
  public Socket createSocket( Socket socket, String s, int i, boolean
flag)
      throws IOException {
    return factory.createSocket( socket, s, i, flag);
  }
  public Socket createSocket( InetAddress inaddr, int i,
      InetAddress inaddr1, int j) throws IOException {
    return factory.createSocket( inaddr, i, inaddr1, j);
  }
  public Socket createSocket( InetAddress inaddr, int i) throws
IOException {
    return factory.createSocket( inaddr, i);
  }
  public Socket createSocket( String s, int i, InetAddress inaddr, int j)
      throws IOException {
    return factory.createSocket( s, i, inaddr, j);
  }
  public Socket createSocket( String s, int i) throws IOException {
    return factory.createSocket( s, i);
  }
  public String[] getDefaultCipherSuites() {
    return factory.getSupportedCipherSuites();
  }
  public String[] getSupportedCipherSuites() {
    return factory.getSupportedCipherSuites();
  }
}

class DummyTrustManager //implements X509TrustManager {

{public boolean isClientTrusted( X509Certificate[] cert) {
    return true;
  }
  public boolean isServerTrusted( X509Certificate[] cert) {
    return true;
  }
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[ 0];
  }
}
