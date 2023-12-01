package org.corgi.consumer.sourcedownload.model.repo;

import lombok.Data;
import org.corgi.consumer.sourcedownload.model.BaseDo;

/**
 * @author fxt
 * @version : ZipInfoDo.java, v 0.1 2021/12/2 6:42 下午 fxt Exp $
 */
@Data
public class ZipInfoDo extends BaseDo {

    /**
     * OSS 存储路径
     */
    private String ossKey;

    /**
     * 超过原始文件100G不再继续打包
     */
    private Boolean packageFlag;

    /**
     * 压缩后体积 b
     */
    private Long zipSize;

    /**
     * 该仓库所有压缩文件数量
     */
    private Integer num;

    /**
     * 该压缩包序号 从0开始
     */
    private Integer idx;

    /**
     * 对应代码仓库编号
     */
    private String repoId;

    private String version;

    @Override
    public int hashCode() {
        return ossKey.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof ZipInfoDo) {
            result = this.getOssKey().equals(((ZipInfoDo) other).getOssKey());
        }
        return result;
    }
}
