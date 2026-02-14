package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {
    private FrameLayout webViewLayout;
    private WebView webView;
    private ProgressBar progressBar;

    public static WebViewFragment newInstance(String url) {
        WebViewFragment fragment = new WebViewFragment();

        Bundle args = new Bundle();
        args.putString("url", url);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        webViewLayout = new FrameLayout(requireContext());
        webViewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webViewLayout.setBackgroundColor(Color.WHITE);

        return webViewLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(() -> {
            initAndLoad();
        }, 250);
    }

    private void initAndLoad() {
        if (!isAdded() || getContext() == null) return;

        if (webView == null) {
            progressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleLarge);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(120, 120);
            lp.gravity = android.view.Gravity.CENTER;
            progressBar.setLayoutParams(lp);

            webView = new WebView(requireContext());
            webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            webView.setBackgroundColor(Color.WHITE);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    progressBar.setVisibility(View.GONE);
                }
            });
            webViewLayout.addView(webView);
            webViewLayout.addView(progressBar);

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setCacheMode(WebSettings.LOAD_NO_CACHE);
            s.setSupportZoom(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        if (getArguments() != null) {
            String url = getArguments().getString("url");

            if (url != null) {
                if (url.endsWith(".pdf")) {
                    webView.loadUrl("https://docs.google.com/viewer?embedded=true&url=" + url);
                } else {
                    webView.loadUrl(url);
                }
            }
        }
    }

    public void refresh() {
        if (webView != null) {
            webView.reload();
        }
    }

    @Override
    public void onDestroyView() {
        if (webViewLayout != null && webView != null) {
            webViewLayout.removeView(webView);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
}