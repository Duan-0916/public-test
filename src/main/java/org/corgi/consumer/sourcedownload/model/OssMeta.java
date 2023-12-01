package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fxt
 * @version : OssMeta.java, v 0.1 2021/12/2 7:15 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OssMeta {

    private String ossEndPoint;

    private String bucketName;

    private String accessKeyId;

    private String accessKeySecret;
}
