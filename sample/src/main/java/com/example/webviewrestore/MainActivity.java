package com.example.webviewrestore;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.tu.webview.ContextDelegate;
import com.tu.webview.WebViewDelegate;
import com.tu.webview.WebViewRestore;

public class MainActivity extends Activity implements ContextDelegate {
  private static final String URL_LOCAL = "file:///android_asset/index.htm";
  private static final int REQUEST_CODE_PICK_CONTACT = 100;
  WebViewRestore webView;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    webView = findViewById(R.id.webview);
    webView.getSettings().setJavaScriptEnabled(true);

    webView.getSettings().setDatabaseEnabled(true);
    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
    webView.getSettings().setDomStorageEnabled(true);

    webView.getSettings().setAppCachePath(getCacheDir().getPath());

    webView.getSettings().setAppCacheEnabled(true);
    webView.setWebViewClient(new WebViewClient() {

      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        Log.e("xxx", "onPageFinished");
        if (view instanceof WebViewDelegate) {
          ((WebViewDelegate) view).onPageFinished();
        }
      }
    });
    webView.addJavascriptInterface(new JSInteraction(), "contact");

    if (savedInstanceState != null) {
      webView.restoreState(savedInstanceState);
    } else {
      webView.loadUrl(URL_LOCAL);
    }
  }

  public class JSInteraction {
    @JavascriptInterface public void getContact() {
      Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
      startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    webView.saveState(outState);
  }

  @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_CODE_PICK_CONTACT) {
      if (Activity.RESULT_OK == resultCode && data != null) {
        if (webView instanceof WebViewDelegate) {
          webView.onActivityResult(requestCode, resultCode, data);
        }
      }
    }
  }

  @Override public void setActivityResult(int requestCode, int resultCode, Intent data) {
    String contact = readContactFormResult(this, data);
    webView.loadUrl("javascript:setContact(\"" + contact + "\")");
  }

  public static String readContactFormResult(Context context, Intent data) {
    StringBuilder contact = new StringBuilder();
    if (data != null) {
      try {
        Uri uri = data.getData();
        ContentResolver contentResolver = context.getContentResolver();

        String contactId = uri.getLastPathSegment();

        Cursor cursor =
            contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

        if (cursor != null) {
          String contactName = "";
          String phoneNumber = "";

          while (cursor.moveToNext()) {
            String name = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            contactName = !TextUtils.isEmpty(name) ? name : contactName;
            if (!TextUtils.isEmpty(number)) {
              phoneNumber = number.replaceAll(" |-", "");
              contact.append(contactName + "  " + phoneNumber + "\n");
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return contact.toString();
  }
}