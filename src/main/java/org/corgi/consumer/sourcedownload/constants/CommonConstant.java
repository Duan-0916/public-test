package org.corgi.consumer.sourcedownload.constants;

public class CommonConstant {
    /**
     * 工作目录
     */
    public static final String WORK_SPACE_DIR = "/tmp/codeinsightsearch/";
    /**
     * 外网下载源码默认保存路径
     */
    public static final String TMP_PACKAGE_DIR = WORK_SPACE_DIR + "package/";
    /**
     * 本地打包操作默认执行路径
     */
    public static final String TMP_REPACKAGE_DIR = "/tmp/codeinsightsearch/repackage/";
    public static final String ZIP_SUFFIX = ".zip";
    public static final String GIT_TAG_ZIP_DOWNLOAD_URL_FORMAT = "https://github.com/%s/archive/refs/tags/%s" + ZIP_SUFFIX;
    public static final String GIT_BRANCH_NAME_ZIP_DOWNLOAD_URL_FORMAT = "https://github.com/%s/archive/refs/heads/%s" + ZIP_SUFFIX;
    public static final String TGZ_SUFFIX = ".tar.gz";
    public static final String GIT_TAG_TGZ_DOWNLOAD_URL_FORMAT = "https://github.com/%s/archive/refs/tags/%s" + TGZ_SUFFIX;
    public static final String SEVEN_Z_SUFFIX = ".7z";
    public static final String SEVEN_Z_SUFFIX_AFTER_TAR = ".tar.7z";
    /**
     * 代码压缩包OSS存储格式: codeinsightsearch/source/{仓库来源}/{仓库名}/src_package_{压缩包编号}.7z
     */
    public static final String SOURCE_PACKAGE_OSS_ROOT = "codeinsightsearch/source/";
    public static final String SOURCE_PACKAGE_OSS_KEY_FORMAT = SOURCE_PACKAGE_OSS_ROOT + "%s/%s/src_package_%d" + SEVEN_Z_SUFFIX;
    public static final String SOURCE_PACKAGE_VERSION_OSS_KEY_FORMAT = SOURCE_PACKAGE_OSS_ROOT + "%s/%s/src_package_%s_%d" + SEVEN_Z_SUFFIX;
    /**
     * 代码压缩包缓存OSS存储格式: codeinsightsearch/cache/{仓库来源}/{仓库名}/{SOURCE_INFO_ID}.{SUFFIX}
     */
    public static final String SOURCE_PACKAGE_CACHE_OSS_ROOT = "codeinsightsearch/cache/";
    public static final String SOURCE_PACKAGE_CACHE_OSS_KEY_FORMAT = SOURCE_PACKAGE_CACHE_OSS_ROOT + "%s/%s/%s%s";
    /**
     * 每次定时任务搜索的待下载SOURCE_INFO任务数量
     */
    public static final int PRE_TIMER_SOURCE_INFO_TASK = 200;
    /**
     * 单个压缩包最大50M
     */
    public static final int MAX_PACKAGE_SIZE = 50 * 1024 * 1024;
    /**
     * 单文件最大50M
     */
    public static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    public static final String GITHUB_MAIN_BRANCH_NAME = "main";
    public static final String GITHUB_MASTER_BRANCH_NAME = "master";

    public static final int MAX_IRONMAN_TASK_INSTANCE = 1;


    public static final String SOURCE_CONTENT_HASH_URI_PREFIX = "sca/content_hash_uri";
}
