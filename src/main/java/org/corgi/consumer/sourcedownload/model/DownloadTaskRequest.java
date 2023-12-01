package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.corgi.consumer.sourcedownload.model.repo.RepoInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.SourceInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.ZipInfoDo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author fxt
 * @version : DownloadTaskModel.java, v 0.1 2021/12/2 4:06 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownloadTaskRequest {

    /**
     * 任务ID
     */
    private String taskId;

    private RepoInfoDo repo;

    private List<SourceInfoDo> sources;

    private List<ZipInfoDo> zips;

    /**
     * 待下载任务列表
     */
    private Map<String, LinkedList<DownloadMeta>> tryDownloadMap;

    private OssMeta ossMeta;

    private String callbackUrl;
}
