package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fxt
 * @version : ZipRecord.java, v 0.1 2021/12/2 6:00 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ZipRecord {

    private String zipId;

    private String idx;

    private String ossKey;
}
