package org.corgi.consumer.sourcedownload.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.corgi.consumer.sourcedownload.model.enums.TaskStatusEnum;
import org.corgi.consumer.sourcedownload.model.repo.SourceInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.SourceZipPair;

import java.util.List;

/**
 * @author fxt
 * @version : TaskResult.java, v 0.1 2021/12/2 5:05 下午 fxt Exp $
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskResult {

    /**
     * 任务ID
     */
    String taskId;

    /**
     * 任务状态
     */
    TaskStatusEnum taskStatus;

    /**
     * 任务信息
     */
    String message;

    /**
     * 代码仓ID
     */
    String repoId;

    /**
     * 无法下载到源码包
     */
    List<SourceInfoDo> fileNotFoundSources;

    /**
     * MD5重复的源码包, 更新涉及字段如下
     * {@link SourceInfoDo#packageUrl}, {@link SourceInfoDo#packageMd5},
     * {@link SourceInfoDo#packageSize}, {@link SourceInfoDo#publishedAt}
     */
    List<SourceInfoDo> md5DuplicateSources;

    /**
     * 新增或更新的压缩包, 更新涉及字段如下
     * {@link ZipInfoDo#packageFlag}, {@link ZipInfoDo#size}
     * {@link ZipInfoDo#compressedSize}, {@link ZipInfoDo#num}
     * 完成下载的源码包, 更新涉及字段如下
     * {@link SourceInfoDo#packageUrl}, {@link SourceInfoDo#packageMd5},
     * {@link SourceInfoDo#packageSize}, {@link SourceInfoDo#publishedAt}
     */
    List<SourceZipPair>  sourceZipPairs;
}
