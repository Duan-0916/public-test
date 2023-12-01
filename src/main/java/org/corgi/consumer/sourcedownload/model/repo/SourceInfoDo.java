package org.corgi.consumer.sourcedownload.model.repo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.corgi.consumer.sourcedownload.model.BaseDo;

import java.util.Date;

/**
 * @author fxt
 * @version : SourceInfoDo.java, v 0.1 2021/12/2 6:42 下午 fxt Exp $
 */
@Data
public class SourceInfoDo extends BaseDo {

    private static final String UNIQUE_KEY_TAG_FORMAT = "repoId_%s_tagName_%s";
    private static final String UNIQUE_KEY_COMMIT_FORMAT = "repoId_%s_commitId_%s";
    private static final String UNIQUE_KEY_BRANCH_FORMAT = "repoId_%s_branchName_%s_gmtCreate_%d";

    private String repoId;

    @SerializedName(value = "commit", alternate = {"commitId"})
    private String commitId;

    private String branchName;

    private String releaseId;

    private String title;

    @SerializedName(value = "tag_name", alternate = {"tagName"})
    private String tagName;

    @SerializedName(value = "target_commitish", alternate = {"targetCommitish"})
    private String targetCommitish;

    @SerializedName(value = "published_at", alternate = {"publishedAt"})
    private Date publishedAt;

    @SerializedName(value = "author_name", alternate = {"authorName"})
    private String authorName;

    @SerializedName(value = "author_email", alternate = {"authorEmail"})
    private String authorEmail;

    private String zipId;

    private String packageUrl;

    private String packageMd5;

    private Long packageSize;

    private String version;

    private String hashKey;

    public String uniqueKey() {
        if (StringUtils.isNotBlank(this.tagName)) {
            return String.format(UNIQUE_KEY_TAG_FORMAT, this.repoId, this.tagName);
        } else if (StringUtils.isNotBlank(commitId)) {
            return String.format(UNIQUE_KEY_COMMIT_FORMAT, this.repoId, this.commitId);
        } else {
            return String.format(UNIQUE_KEY_BRANCH_FORMAT, this.repoId, this.branchName, this.getGmtCreate().getTime());
        }
    }

    @Override
    public String toString() {
        return null;
    }
}
