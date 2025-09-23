package com.company.product;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // if your website starts with www, exclude it
    private static final String myWebSite = "mariam-241.vercel.app";

    WebView webView;
    ImageView imageView;
    ProgressBar progressBar;

    // for handling file upload, set a static value, any number you like
    // this value will be used by WebChromeClient during file upload
    private static final int file_chooser_activity_code = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static ValueCallback<Uri[]> mUploadMessageArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check for storage permissions
        checkPermissions();

        // get the web-view, image-view and progress-bar from the layout
        webView = findViewById(R.id.webView);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);


        // for handling Android Device [Back] key press
        webView.canGoBackOrForward(99);

        // handling web page browsing mechanism
        webView.setWebViewClient(new myWebViewClient());

        // handling file upload mechanism
        webView.setWebChromeClient(new myWebChromeClient());

        // some other settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString(new WebView(this).getSettings().getUserAgentString());
        settings.setDomStorageEnabled(true);


        // session management
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);


        // set the download listener
        webView.setDownloadListener(downloadListener);

        // load the website
        webView.loadUrl("https://" + myWebSite);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // flush cookies to disk
        CookieManager.getInstance().flush();
    }

    // after the file chosen handled, variables are returned back to MainActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the chrome activity is a file choosing session
        if (requestCode == file_chooser_activity_code) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri[] results = null;

                // Check if response is a multiple choice selection containing the results
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    // Response is a single choice selection
                    results = new Uri[]{data.getData()};
                }

                mUploadMessageArr.onReceiveValue(results);
                mUploadMessageArr = null;
            } else {
                if (mUploadMessageArr != null) {
                    mUploadMessageArr.onReceiveValue(null);
                    mUploadMessageArr = null;
                }
                Toast.makeText(MainActivity.this, "Error getting file", Toast.LENGTH_LONG).show();
            }
        }
    }

    class myWebViewClient extends android.webkit.WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // show the splash screen
            imageView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            webView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // hide the splash screen
            imageView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            // show the error message = no internet access
            webView.loadUrl("file:///android_asset/no_internet.html");
            // hide the splash screen on error in loading
            imageView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(), "Internet issue", Toast.LENGTH_SHORT).show();
        }
    }

    // Calling WebChromeClient to select files from the device
    public class myWebChromeClient extends WebChromeClient {
        @SuppressLint("NewApi")
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            Intent chooserIntent = Intent.createChooser(intent, "Choose file");
            startActivityForResult(chooserIntent, file_chooser_activity_code);

            // Save the callback for handling the selected file
            mUploadMessageArr = valueCallback;

            return true;
        }
    }

    DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    };

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        } else {
            finish();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
