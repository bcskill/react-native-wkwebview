package im.token.android.dappbrowser;

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.webview.ReactWebViewManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import okhttp3.HttpUrl;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

/**
 * Extends the ReactWebViewManager to provide a initialJavaScript which run at document starting
 * ref documents:
 * https://github.com/facebook/react-native/blob/master/Libraries/Components/WebView/WebView.android.js
 * https://facebook.github.io/react-native/docs/native-components-android.html
 * https://github.com/magicismight/react-native-advanced-webview/blob/master/WebView.android.js
 */
@ReactModule(name = "RCTDAppBrowser")
public class DAppBrowser extends ReactWebViewManager {
  private static final String REACT_CLASS = "RCTDAppBrowser";

  private static Map<Integer, JsInjectorClient> sJsInjectorClientMap = new WeakHashMap<>();
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected ReactWebViewManager.ReactWebView createReactWebViewInstance(ThemedReactContext reactContext) {
    return super.createReactWebViewInstance(reactContext);
  }

  @Override
  protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
    // Do not register default touch emitter and let WebView implementation handle touches
    JsInjectorClient client = new JsInjectorClient();
    sJsInjectorClientMap.put(view.hashCode(), client);
    view.setWebViewClient(new DAppWebViewClient(client, new UrlHandlerManager()));
  }

  @ReactProp(name = "initialJavaScript")
  public void setInitialJavaScript(WebView view, String initialJavaScript) {
    JsInjectorClient client = sJsInjectorClientMap.get(view.hashCode());
    if (client != null) {
      client.setDAppSDK(initialJavaScript);
    }
  }

  /**
   * Extend the ReactWebViewClient, Intercept the request for injecting the initialJavaScript
   */
  public class DAppWebViewClient extends ReactWebViewManager.ReactWebViewClient {

    private final Object lock = new Object();

    private final JsInjectorClient jsInjectorClient;
    private final UrlHandlerManager urlHandlerManager;

    private boolean isInjected;

    public DAppWebViewClient(JsInjectorClient jsInjectorClient, UrlHandlerManager urlHandlerManager) {
      this.jsInjectorClient = jsInjectorClient;
      this.urlHandlerManager = urlHandlerManager;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      return super.shouldOverrideUrlLoading(view, url) || this.shouldOverrideUrlLoading(view, url, false, false);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      if (super.shouldOverrideUrlLoading(view, request)) return true;
      if (request == null || view == null) {
        return false;
      }
      String url = request.getUrl().toString();
      boolean isMainFrame = request.isForMainFrame();
      boolean isRedirect = SDK_INT >= N && request.isRedirect();
      return shouldOverrideUrlLoading(view, url, isMainFrame, isRedirect);
    }


    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      WebResourceResponse response = super.shouldInterceptRequest(view, request);
      if (response != null) {
        try {
          InputStream in = response.getData();
          int len = in.available();
          byte[] data = new byte[len];
          int readLen = in.read(data);
          if (readLen == 0) {
            throw new IOException("Nothing is read.");
          }
          String injectedHtml = jsInjectorClient.injectJS(new String(data));
          response.setData(new ByteArrayInputStream(injectedHtml.getBytes()));
          return response;
        } catch (IOException ex) {
          Log.d("INJECT AFTER_EXTRNAL", "", ex);
        }
      } else {
        if (request == null) {
          return null;
        }
        if (!request.getMethod().equalsIgnoreCase("GET") || !request.isForMainFrame()) {
          if (request.getMethod().equalsIgnoreCase("GET")
              && (request.getUrl().toString().contains(".js")
              || request.getUrl().toString().contains("json")
              || request.getUrl().toString().contains("css"))) {
            synchronized (lock) {
              if (!isInjected) {
                injectScriptFile(view);
                isInjected = true;
              }
            }
          }
          super.shouldInterceptRequest(view, request);
          return null;
        }

        HttpUrl httpUrl = HttpUrl.parse(request.getUrl().toString());
        if (httpUrl == null) {
          return null;
        }
        Map<String, String> headers = request.getRequestHeaders();
        JsInjectorResponse jsInjectorResponse;
        try {
          jsInjectorResponse = jsInjectorClient.loadUrl(httpUrl.toString(), headers);
        } catch (Exception ex) {
          return null;
        }
        if (jsInjectorResponse == null || jsInjectorResponse.isRedirect) {
          return null;
        } else {
          ByteArrayInputStream inputStream = new ByteArrayInputStream(jsInjectorResponse.data.getBytes());
          WebResourceResponse webResourceResponse = new WebResourceResponse(
              jsInjectorResponse.mime, jsInjectorResponse.charset, inputStream);
          synchronized (lock) {
            isInjected = true;
          }
          return webResourceResponse;
        }
      }
      return null;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
      handler.proceed();
    }

    public void onReload() {
      synchronized (lock) {
        isInjected = false;
      }
    }

    private void injectScriptFile(final WebView view) {
      String js = jsInjectorClient.assembleJs("%1$s");
      byte[] buffer = js.getBytes();
      final String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);

      view.post(new Runnable() {
        @Override
        public void run() {
          view.loadUrl("javascript:(function() {" +
              "var parent = document.getElementsByTagName('head').item(0);" +
              "var script = document.createElement('script');" +
              "script.type = 'text/javascript';" +
              // Tell the browser to BASE64-decode the string into your script !!!
              "script.innerHTML = window.atob('" + encoded + "');" +
              "parent.appendChild(script)" +
              "})()");
        }
      });
//      view.post(() -> view.loadUrl("javascript:(function() {" +
//          "var parent = document.getElementsByTagName('head').item(0);" +
//          "var script = document.createElement('script');" +
//          "script.type = 'text/javascript';" +
//          // Tell the browser to BASE64-decode the string into your script !!!
//          "script.innerHTML = window.atob('" + encoded + "');" +
//          "parent.appendChild(script)" +
//          "})()"));
    }

    private boolean shouldOverrideUrlLoading(WebView webView, String url, boolean isMainFrame, boolean isRedirect) {
      boolean result = false;
      synchronized (lock) {
        isInjected = false;
      }
      String urlToOpen = urlHandlerManager.handle(url);
      if (!url.startsWith("http")) {
        result = true;
      }
      if (isMainFrame && isRedirect) {
        urlToOpen = url;
        result = true;
      }

      if (result && !TextUtils.isEmpty(urlToOpen)) {
        webView.loadUrl(urlToOpen);
      }
      return result;
    }

  }
}
