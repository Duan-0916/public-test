package org.corgi.consumer.sourcedownload.model.repo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author fxt
 * @version : SourceZipPair.java, v 0.1 2021/12/4 5:48 下午 fxt Exp $
 */
@Data
@AllArgsConstructor
public class SourceZipPair {

    ZipInfoDo zip;

    List<SourceInfoDo> sources;
}
