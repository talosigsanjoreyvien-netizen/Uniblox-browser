package fun.cybercode.uniblox.browser.util;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class AdBlocker {
    private static final List<String> AD_PATTERNS = new ArrayList<>();

    static {
        // Google Publisher Tag (GPT)
        AD_PATTERNS.add("googletagservices.com/tag/js/gpt.js");
        AD_PATTERNS.add("securepubads.g.doubleclick.net/tag/js/gpt.js");

        // Common Ad Servers and Networks
        AD_PATTERNS.add("doubleclick.net");
        AD_PATTERNS.add("googlesyndication.com");
        AD_PATTERNS.add("adservice.google");
        AD_PATTERNS.add("googleadservices.com");
        AD_PATTERNS.add("adnxs.com");
        AD_PATTERNS.add("amazon-adsystem.com");
        AD_PATTERNS.add("openx.net");
        AD_PATTERNS.add("pubmatic.com");
        AD_PATTERNS.add("rubiconproject.com");
        AD_PATTERNS.add("taboola.com");
        AD_PATTERNS.add("outbrain.com");
        AD_PATTERNS.add("criteo.com");
        AD_PATTERNS.add("advertising.com");
        AD_PATTERNS.add("adform.net");
        AD_PATTERNS.add("casalemedia.com");
        
        // Iframe and script patterns often used for ads
        AD_PATTERNS.add("/ads/");
        AD_PATTERNS.add("/adserver/");
        AD_PATTERNS.add("/adstream/");
        AD_PATTERNS.add("ads-host");
    }

    public static boolean isAd(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        for (String pattern : AD_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    public static WebResourceResponse createEmptyResource() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
    }
}
