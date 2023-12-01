package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fxt
 * @version : WebHookResponse.java, v 0.1 2021/12/2 5:17 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebHookResponse {
    Boolean success;
}
