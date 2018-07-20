package im.token.android.dappbrowser;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class JsInjectorClient {

  private static final String DEFAULT_CHARSET = "utf-8";
  private static final String DEFAULT_MIME_TYPE = "text/html";
  private final static String JS_TAG_TEMPLATE = "<script type=\"text/javascript\">%1$s</script>";

  private String mDAppSDK = "void(0);";
  private final OkHttpClient httpClient;

  JsInjectorClient() {
    this.httpClient = createHttpClient();
  }

  public void setDAppSDK(String DAppSDK) {
    mDAppSDK = DAppSDK;
  }

  JsInjectorResponse loadUrl(final String url, String userAgent) {
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", userAgent);
    return loadUrl(url, headers);
  }

  @Nullable
  JsInjectorResponse loadUrl(final String url, final Map<String, String> headers) {
    Request request = buildRequest(url, headers);
    JsInjectorResponse result = null;
    try {
      Response response = httpClient.newCall(request).execute();
      result = buildResponse(response);
    } catch (Exception ex) {
      Log.d("REQUEST_ERROR", "", ex);
    }
    return result;
  }

  String assembleJs(String template) {
    return String.format(template, mDAppSDK);
  }

  @Nullable
  private JsInjectorResponse buildResponse(Response response) {
    String body = null;
    int code = response.code();
    try {
      if (response.isSuccessful()) {
        body = response.body().string();
      }
    } catch (IOException ex) {
      Log.d("READ_BODY_ERROR", "Ex", ex);
    }
    Request request = response.request();
    Response prior = response.priorResponse();
    boolean isRedirect = prior != null && prior.isRedirect();
    String result = injectJS(body);
    String contentType = getContentTypeHeader(response);
    String charset = getCharset(contentType);
    String mime = getMimeType(contentType);
    String finalUrl = request.url().toString();
    return new JsInjectorResponse(result, code, finalUrl, mime, charset, isRedirect);
  }

  String injectJS(String html) {
    String js = assembleJs(JS_TAG_TEMPLATE);
    return injectJS(html, js);
  }

  private String injectJS(String html, String js) {
    if (TextUtils.isEmpty(html)) {
      return html;
    }
    int position = getInjectionPosition(html);
    if (position > 0) {
      String beforeTag = html.substring(0, position);
      String afterTab = html.substring(position);
      return beforeTag + js + afterTab;
    }
    return html;
  }

  private int getInjectionPosition(String body) {
    body = body.toLowerCase();
    int ieDetectTagIndex = body.indexOf("<!--[if");
    int scriptTagIndex = body.indexOf("<script");

    int index;
    if (ieDetectTagIndex < 0) {
      index = scriptTagIndex;
    } else {
      index = Math.min(scriptTagIndex, ieDetectTagIndex);
    }
    if (index < 0) {
      index = body.indexOf("</head");
    }
    return index;
  }

  @Nullable
  private Request buildRequest(String url, Map<String, String> headers) {
    HttpUrl httpUrl = HttpUrl.parse(url);
    if (httpUrl == null) {
      return null;
    }
    Request.Builder requestBuilder = new Request.Builder()
        .get()
        .url(httpUrl);
    Set<String> keys = headers.keySet();
    for (String key : keys) {
      requestBuilder.addHeader(key, headers.get(key));
    }
    return requestBuilder.build();
  }

  private String getMimeType(String contentType) {
    Matcher regexResult = Pattern.compile("^.*(?=;)").matcher(contentType);
    if (regexResult.find()) {
      return regexResult.group();
    }
    return DEFAULT_MIME_TYPE;
  }

  private String getCharset(String contentType) {
    Matcher regexResult = Pattern.compile("charset=([a-zA-Z0-9-]+)").matcher(contentType);
    if (regexResult.find()) {
      if (regexResult.groupCount() >= 2) {
        return regexResult.group(1);
      }
    }
    return DEFAULT_CHARSET;
  }

  @Nullable
  private String getContentTypeHeader(Response response) {
    Headers headers = response.headers();
    String contentType;
    if (TextUtils.isEmpty(headers.get("Content-Type"))) {
      if (TextUtils.isEmpty(headers.get("content-Type"))) {
        contentType = "text/data; charset=utf-8";
      } else {
        contentType = headers.get("content-Type");
      }
    } else {
      contentType = headers.get("Content-Type");
    }
    if (contentType != null) {
      contentType = contentType.trim();
    }
    return contentType;
  }

  private OkHttpClient createHttpClient() {
    return new OkHttpClient.Builder()
        .cookieJar(new WebViewCookieJar())
        .build();
  }
}
