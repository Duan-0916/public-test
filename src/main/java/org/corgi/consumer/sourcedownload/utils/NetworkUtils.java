package org.corgi.consumer.sourcedownload.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.corgi.consumer.sourcedownload.service.download.impl.DownloadSourceServiceImpl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

public class NetworkUtils {

    private static final Logger logger = LogManager.getLogger(NetworkUtils.class);

    public static String parseHost(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (MalformedURLException e) {
            logger.error("malformed url: " + url);
            return null;
        }
    }


    public static String replaceHost(String url, String newHost) {
        try {
            URI uriObj = new URI(url);
            URI newUriObj = new URI(
                    uriObj.getScheme().toLowerCase(Locale.US),
                    newHost,
                    uriObj.getPath(),
                    uriObj.getQuery(),
                    uriObj.getFragment());

            return newUriObj.toString();
        } catch (URISyntaxException e) {
            logger.error("sync exception url: " + url);
            return null;
        }
    }
}
