package org.corgi.consumer.sourcedownload.utils;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.corgi.consumer.sourcedownload.model.TaskResult;
import org.corgi.consumer.sourcedownload.model.WebHookResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * @author fxt
 * @version : CallbackUtil.java, v 0.1 2021/12/2 4:55 下午 fxt Exp $
 */
public class CallbackUtil {

    private static final Logger logger = LogManager.getLogger(SourceUtil.class);

    private static RestTemplate restTemplate = new RestTemplate();

    private static HttpHeaders requestHeaders;

    static {
        if (null == requestHeaders) {
            requestHeaders = new HttpHeaders();
        }
        requestHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    }

    public static void callback(String callbackUrl, TaskResult postBody) {
        logger.info("CodeInsightSearchClientImpl.callback callbackUrl={} postBody={}", callbackUrl,new Gson().toJson(postBody));
        int retry = 0;
        do {
            HttpEntity<TaskResult> request = new HttpEntity<>(postBody, requestHeaders);
            ResponseEntity<WebHookResponse> responseEntity = restTemplate.exchange(callbackUrl, HttpMethod.POST, request, WebHookResponse.class);
            if (Objects.nonNull(responseEntity) && Objects.nonNull(responseEntity.getBody())) {
                logger.info("CodeInsightSearchClientImpl.callback response={}", new Gson().toJson(responseEntity.getBody()));
                if (Objects.nonNull(responseEntity.getBody().getSuccess()) && responseEntity.getBody().getSuccess().equals(Boolean.TRUE)) {
                    logger.info("CodeInsightSearchClientImpl.callback response is success");
                    return;
                }
            }
            logger.info("CodeInsightSearchClientImpl.callback retry {}", ++retry);
        } while (retry < 10);
    }
}
