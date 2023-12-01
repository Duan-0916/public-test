package org.corgi.consumer.sourcedownload.service.download;

public interface DownloadService {
    /**
     * 从s3下载源码
     * @param ossKey
     * @return
     */
    public void execFromS3(String ossKey, String uuid);
}
