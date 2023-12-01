package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fxt
 * @version : DownloadTask.java, v 0.1 2021/12/2 4:15 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadMeta {

    private String srcFileUrl;

    private String dstFilePath;

    private String ossCacheKey;
}
