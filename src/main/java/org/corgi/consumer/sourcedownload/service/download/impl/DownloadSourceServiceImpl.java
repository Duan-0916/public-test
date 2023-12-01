package org.corgi.consumer.sourcedownload.service.download.impl;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.corgi.consumer.sourcedownload.model.DownloadMeta;
import org.corgi.consumer.sourcedownload.model.DownloadTaskRequest;
import org.corgi.consumer.sourcedownload.model.TaskResult;
import org.corgi.consumer.sourcedownload.model.enums.CommonStatusEnum;
import org.corgi.consumer.sourcedownload.model.enums.TaskStatusEnum;
import org.corgi.consumer.sourcedownload.model.enums.TenantEnum;
import org.corgi.consumer.sourcedownload.model.repo.RepoInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.SourceInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.ZipInfoDo;
import org.corgi.consumer.sourcedownload.service.download.DownloadService;
import org.corgi.consumer.sourcedownload.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.corgi.consumer.sourcedownload.constants.CommonConstant.*;
import static org.corgi.consumer.sourcedownload.utils.CommonConstant.COMMIT_FILES_URI_PREFIX;

@Service("sourceDownloadService")
public class DownloadSourceServiceImpl implements DownloadService {

    @Autowired
    private S3Util s3Util;

    private static final Logger logger = LogManager.getLogger(DownloadSourceServiceImpl.class);

    /**
     * 单个压缩包最大100G
     */
    // private static final long MAX_PACKAGE_SIZE = 100 * 1024 * 1024 * 1024L;
    /**
     * 单个文件最大50KB
     */
    private static final int MAX_FILE_SIZE = 50 * 1024;

    private static final String DOWNLOAD_TASK_RESULT_OSS_KEY_FORMAT = "codeinsightsearch/task/download/result/task_id_%s";

    private static final String UNUSED_ACCELERATE_GITHUB_SITE_HOST = "github.com.cnpmjs.org";
    private static final String GITHUB_SITE_HOST = "github.com";


    private List<String> getIdList(List<SourceInfoDo> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return new ArrayList<>();
        }

        return sources.stream().map(SourceInfoDo::getId).collect(Collectors.toList());
    }

    /**
     * 从s3下载源码
     * @param ossKey
     * @return
     */
    @Override
    public void execFromS3(String ossKey, String uuid) {
        logger.info("osskey = " + ossKey);
        if (!s3Util.exist(ossKey)) {
            logger.warn("DownloadSourceServiceImpl.execFromS3 ossKey not exists: " + ossKey);
            return;
        }

        DownloadTaskRequest param = null;
        try {
            logger.info("osskey = " + ossKey);
            String payload = s3Util.fetchContext(ossKey);
            logger.info("payload = " + payload);

            param = new GsonBuilder().registerTypeAdapter(
                    Date.class,
                    (JsonDeserializer<Date>) (json, typeOfT, context)
                            -> new Date(json.getAsJsonPrimitive().getAsLong())).create().fromJson(payload, DownloadTaskRequest.class);
            if (Objects.isNull(param)) {
                logger.warn("DownloadSourceServiceImpl.execFromS3, no extra data file");
                // param解析失败没有URL无法回调
                return;
            }

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateTime = df.format(new Date());
            TaskResult runningResult = TaskResult.builder().taskId(param.getTaskId())
                    .taskStatus(TaskStatusEnum.RUNNING).message(String.format("task = %s, gmtStart = %s, uuid = %s", param.getTaskId(), dateTime, uuid))
                    .build();
            //CallbackUtil.callback(param.getCallbackUrl(), runningResult);

            long startTime = System.currentTimeMillis();

            logger.info("DownloadSourceServiceImpl.execFromS3, with extra info: {}", new Gson().toJson(param));

            logger.info("DownloadSourceServiceImpl.execFromS3, download startTime {}", new Date());
            List<SourceInfoDo> fileNotFoundSources = new ArrayList<>();
            List<SourceInfoDo> md5DuplicateSources = new ArrayList<>();
            RepoInfoDo repo = param.getRepo();

            Set<String> md5Set = param
                    .getSources()
                    .stream()
                    .map(SourceInfoDo::getPackageMd5)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toSet());

            List<SourceInfoDo> sources = param.getSources();
            List<SourceInfoDo> downloadSources = new ArrayList<>();
            List<Pair<SourceInfoDo, File>> sfPairList = new ArrayList<>();
            int downSourceCnt = 0;
            for (Map.Entry<String, LinkedList<DownloadMeta>> entry : param.getTryDownloadMap().entrySet()) {
                String sourceId = entry.getKey();
                logger.info("DownloadSourceServiceImpl.execFromS3 try download source {}", sourceId);

                long endTime = System.currentTimeMillis();
                if (endTime - startTime > SourceUtil.TIMEOUT) {
                    logger.warn("DownloadSourceServiceImpl.execFromS3 startTime={} endTime={}", new Date(startTime), new Date(endTime));
                    break;
                }
                LinkedList<DownloadMeta> downloadMetas = entry.getValue();

                SourceInfoDo source = sources.stream().filter(s -> s.getId().equalsIgnoreCase(sourceId)).findFirst().get();
                boolean download = false;
                boolean md5Dup = false;
                File targetFile = null;
                for (DownloadMeta downloadMeta : downloadMetas) {
                    String downloadFileName = null;
                    if (s3Util.exist(downloadMeta.getOssCacheKey())) {
                        logger.info("DownloadSourceServiceImpl.execFromS3 source_info {} oss cache hit", source.getId());
                        String preSignedUrl = s3Util.genDownloadUrl(downloadMeta.getOssCacheKey());
                        download = SourceUtil.aria2cDownloadSourceFromUrl(preSignedUrl, downloadMeta.getDstFilePath(), startTime);
                        downloadFileName = downloadMeta.getDstFilePath();
                    } else {
                        String srcFileUrl = downloadMeta.getSrcFileUrl();
                        String srcHost = NetworkUtils.parseHost(srcFileUrl);
                        if (null == srcHost) {
                            logger.error("malformed url: " + srcFileUrl);
                            return;
                        }
                        String downloadSrcUrl = srcFileUrl;
                        if (UNUSED_ACCELERATE_GITHUB_SITE_HOST.equals(srcHost)) {
                            downloadSrcUrl = NetworkUtils.replaceHost(srcFileUrl, GITHUB_SITE_HOST);
                        }

                        download = SourceUtil.aria2cDownloadSourceFromUrl(downloadSrcUrl, downloadMeta.getDstFilePath(), startTime);
                        downloadFileName = downloadMeta.getDstFilePath();
                        if (download) {
                            ObjectMetadata metadata = new ObjectMetadata();
                            metadata.setContentLength(new File(downloadFileName).length());
                            s3Util.saveFileWithMetaData(downloadMeta.getOssCacheKey(), new FileInputStream(downloadFileName), metadata);
                            logger.info("DownloadSourceServiceImpl.execFromS3 source_info {} touch oss cache {}", source.getId(), downloadMeta.getOssCacheKey());
                        }
                    }

                    endTime = System.currentTimeMillis();
                    if (endTime - startTime > SourceUtil.TIMEOUT && !download) {
                        logger.warn("DownloadSourceServiceImpl.execFromS3 startTime={} endTime={}", new Date(startTime), new Date(endTime));
                        break;
                    }

                    if (download) {
                        logger.info("DownloadSourceServiceImpl.execFromS3 source {} download success", sourceId);
                        targetFile = new File(downloadFileName);
                        String md5 = Md5Util.getFileMD5String(targetFile);
                        String srcUrl = downloadMeta.getSrcFileUrl();

                        source.setPackageUrl(srcUrl);
                        source.setPackageMd5(md5);
                        source.setPackageSize(targetFile.length());
                        if (StringUtils.isBlank(source.getTagName()) && Objects.isNull(source.getPublishedAt())) {
                            source.setPublishedAt(new Date());
                        }
                        md5Set.clear();
                        if (md5Set.contains(md5)) {
                            md5DuplicateSources.add(source);
                            md5Dup = true;
                            break;
                        } else {
                            downSourceCnt++;
                            md5Set.add(md5);
                        }
                        break;
                    }
                }
                if (md5Dup) {
                    logger.info("DownloadSourceServiceImpl.execFromS3 md5 dup continue");
                    continue;
                }
                if (!download) {
                    if (endTime - startTime <= SourceUtil.TIMEOUT) {
                        logger.info("DownloadSourceServiceImpl.execFromS3 source {} download failed", sourceId);
                        fileNotFoundSources.add(source);
                    } else {
                        logger.info("DownloadSourceServiceImpl.execFromS3 source {} download timeout 10min", sourceId);
                    }
                    continue;
                }

                downloadSources.add(source);
                sfPairList.add(Pair.of(source, targetFile));
            }

            if (downloadSources.size() > 0) {
                // 说明有不满100G的压缩文件待上传
                unpackZipTarAndUpload(sfPairList, repo);
            }

            logger.info("DownloadSourceServiceImpl.execFromS3 success download {}/{} from candidate",
                    downSourceCnt, param.getTryDownloadMap().entrySet().size());
            TaskResult taskResult;
            if (fileNotFoundSources.size() > 0) {
                taskResult = TaskResult.builder().taskId(param.getTaskId())
                        .taskStatus(TaskStatusEnum.DONE).message(
                                String.format("candidate source=%d, " +
                                                "download source=%d%s, " +
                                                "md5 duplicate source=%d%s, " +
                                                "FileNotFoundException source=%d%s, " +
                                                "other source=%d",
                                        param.getTryDownloadMap().entrySet().size(),
                                        downSourceCnt, new Gson().toJson(getIdList(downloadSources)),
                                        md5DuplicateSources.size(), new Gson().toJson(getIdList(md5DuplicateSources)),
                                        fileNotFoundSources.size(), new Gson().toJson(getIdList(fileNotFoundSources)),
                                        param.getTryDownloadMap().entrySet().size() - downSourceCnt - md5DuplicateSources.size() - fileNotFoundSources.size()))
                        .repoId(param.getRepo().getId())
                        .fileNotFoundSources(fileNotFoundSources)
                        .md5DuplicateSources(md5DuplicateSources)
                        .build();
            } else {
                taskResult = TaskResult.builder()
                        .taskId(param.getTaskId())
                        .taskStatus(TaskStatusEnum.DONE).message(
                                String.format("candidate source = %d, " +
                                                "download source = %d %s, " +
                                                "md5 duplicate source = %d %s, " +
                                                "other source = %d",
                                        param.getTryDownloadMap().entrySet().size(),
                                        downSourceCnt, new Gson().toJson(getIdList(downloadSources)),
                                        md5DuplicateSources.size(), new Gson().toJson(getIdList(md5DuplicateSources)),
                                        param.getTryDownloadMap().entrySet().size() - downSourceCnt - md5DuplicateSources.size()))
                        .repoId(param.getRepo().getId())
                        .fileNotFoundSources(fileNotFoundSources)
                        .md5DuplicateSources(md5DuplicateSources)
                        .build();
            }

            String newOssKey = String.format(DOWNLOAD_TASK_RESULT_OSS_KEY_FORMAT, taskResult.getTaskId());
            ObjectMetadata taskResultMetadata = new ObjectMetadata();
            byte[] taskResultBytes = new Gson().toJson(taskResult).getBytes(StandardCharsets.UTF_8);
            taskResultMetadata.setContentLength(taskResultBytes.length);
            s3Util.saveFileWithMetaData(newOssKey, new ByteArrayInputStream(taskResultBytes), taskResultMetadata);

            CallbackUtil.callback(param.getCallbackUrl(), taskResult);

            logger.info("DownloadSourceServiceImpl.execFromS3, download end");
        } catch (Exception e) {
            logger.error("DownloadSourceServiceImpl.execFromS3 e={}", new Gson().toJson(e));
            TaskResult exceptionResult = TaskResult.builder()
                    .taskId(param.getTaskId())
                    .taskStatus(TaskStatusEnum.ERROR)
                    .message("e=" + new Gson().toJson(e))
                    .repoId(param.getRepo().getId())
                    .build();
            CallbackUtil.callback(param.getCallbackUrl(), exceptionResult);
            e.printStackTrace();
        } finally {
            // do sth
        }
    }


    /**
     * 打包前预处理
     *
     * @param workDir
     */
    void beforePackage(String workDir) {
        String newWorkDir;
        if (FileUtils.exists(workDir)) {
            newWorkDir = workDir;
        } else {
            logger.error("CodeInsightSearchDownload.beforePackage 工作目录不存在或者不是目录 {}", workDir);
            return;
        }
        logger.error("CodeInsightSearchDownload.beforePackage  工作目录 {}", newWorkDir);
        if (!FileUtils.isFolder(newWorkDir)) {
            logger.error("CodeInsightSearchDownload.beforePackage 工作目录不存在或者不是目录 {}", newWorkDir);
            return;
        }
        fileSuffixFilter(newWorkDir);
        fileSizeFilter(newWorkDir);
    }

    /**
     * 过滤超过50K的单文件
     *
     * @param workDir
     */
    void fileSizeFilter(String workDir) {
        Consumer<File> fileConsumer = file -> {
            if (file.length() > MAX_FILE_SIZE) {
                logger.debug("CodeInsightSearchDownload.fileSuffixFilter before repackage remove large file {} with size {}", file.getPath(), FileUtils.getSuitableSizeString(file));
                FileUtils.delete(file);
            }
        };

        fileVisitor(workDir, fileConsumer, this::removeTargetDir, this::removeEmptyDir);
    }

    /**
     * 删除特定目录
     *
     * @param dir
     */
    void removeTargetDir(File dir) {
        List<String> removeList = Arrays.asList("node_modules");
        if (removeList.contains(dir.getName())) {
            String[] files = dir.list();
            if (Objects.nonNull(files)) {
                for (String file : files) {
                    FileUtils.delete(dir + File.separator + file);
                }
            }
            logger.debug("CodeInsightSearchDownload.removeTargetDir before repackage remove target dir {}", dir.getPath());
        }
    }

    /**
     * 删除空目录
     *
     * @param dir
     */
    void removeEmptyDir(File dir) {
        String[] file = dir.list();
        if (Objects.isNull(file)) {
            logger.debug("CodeInsightSearchDownload.removeEmptyDir before repackage remove empty dir {}", dir.getPath());
            FileUtils.delete(dir);
        }
    }

    /**
     * 过滤非源码文件
     *
     * @param workDir
     */
    void fileSuffixFilter(String workDir) {
        List<String> blackList = Arrays.asList(".art", ".blp", ".bmp", ".bti", ".cd5", ".cit", ".clip", ".cpl", ".cpt",
                ".cr2", ".dds", ".dib", ".djvu", ".egt", ".exif", ".gif", ".grf", ".icb", ".icns", ".ico", ".iff", ".ilbm",
                ".jfif", ".jng", ".jp2", ".jpeg", ".jpg", ".jps", ".kra", ".lbm", ".log", ".max", ".miff", ".mng", ".msp",
                ".nitf", ".otb", ".pbm", ".pc1", ".pc2", ".pc3", ".pcf", ".pct", ".pcx", ".pdn", ".pgm", ".pi1", ".pi2",
                ".pi3", ".pict", ".pix", ".png", ".pnm", ".pns", ".ppm", ".psb", ".psd", ".psp", ".px", ".pxm", ".pxr",
                ".qfx", ".raw", ".rle", ".sct", ".sgi", ".targa", ".tga", ".tif", ".tiff", ".vda", ".vst", ".vtf", ".xbm",
                ".xcf", ".xpm", ".zif", ".svg", ".mp3", ".mp4", ".ogg", ".3gp", ".flv", ".m4v", ".m4a", ".webm", ".aaf",
                ".asf", ".avchd", ".avi", ".bik", ".braw", ".cam", ".collab", ".dat", ".dsh", ".noa", ".fla", ".flr",
                ".sol", ".str", ".mkv", ".wrap", ".mov", ".mpeg", ".thp", ".mxf", ".roq", ".nsv", ".rm", ".svi", ".smi",
                ".smk", ".swf", ".wmv", ".wtv", ".yuv", ".bin", ".dsk", ".wav",
                ".a", ".apk", ".app", ".aux", ".beam", ".bpi", ".bpl", ".bridgesupport", ".bs", ".cfg", ".chi", ".class",
                ".cmd", ".cover", ".ctxt", ".d", ".dsym", ".dcp", ".dcu", ".ddp", ".def", ".deployproj", ".dll", ".drc",
                ".dres", ".dsk", ".dylib", ".dyn_hi", ".dyn_o", ".ear", ".egg", ".elf", ".eventlog", ".exe", ".exp", ".gch",
                ".gem", ".gz", ".hex", ".hi", ".hie", ".hmap", ".hp", ".hpp", ".html", ".i*86", ".idb", ".identcache", ".ilk",
                ".ipa", ".jar", ".knit.md", ".ko", ".la", ".lai", ".lib", ".lo", ".local", ".log", ".manifest", ".map", ".mo",
                ".mod", ".mode1v3", ".mode2v3", ".moved-aside", ".nar", ".o", ".obj", ".ocx", ".os", ".out", ".pbxuser", ".pch",
                ".pdb", ".pdf", ".perspectivev3", ".plt", ".pm.tdy", ".png", ".pot", ".prof", ".projdata", ".py,cover", ".rar",
                ".rbc", ".res", ".rs.bk", ".rsm", ".sage.py", ".slo", ".smod", ".so", ".spec", ".src.rock", ".stat", ".su",
                ".tar.gz", ".tds", ".test", ".tlb", ".tvsconfig", ".utf8.md", ".vlb", ".war", ".x86_64", ".xccheckout", ".xcodeproj",
                ".xcscmblueprint", ".xcworkspace", ".zip", ".DS_Store");

        Consumer<File> fileConsumer = file -> {
            boolean sourceCodeFile = true;
            for (String suffix : blackList) {
                if (file.getName().toLowerCase(Locale.ROOT).endsWith(suffix)) {
                    sourceCodeFile = false;
                    break;
                }
            }
            if (!sourceCodeFile) {
                logger.debug("CodeInsightSearchDownload.fileSuffixFilter before repackage remove none source code file {}", file.getPath());
                FileUtils.delete(file);
            }
        };
        fileVisitor(workDir, fileConsumer, this::removeTargetDir, this::removeEmptyDir);
    }

    /**
     * 递归处理文件目录
     *
     * @param workDir
     * @param fileConsumer
     * @param dirBeforeConsumer
     * @param dirAfterConsumer
     */
    void fileVisitor(String workDir, Consumer<File> fileConsumer, Consumer<File> dirBeforeConsumer, Consumer<File> dirAfterConsumer) {
        try {
            File fileList = new File(workDir);
            String[] file = fileList.list();
            if (Objects.isNull(file)) {
                return;
            }
            File temp;
            for (String s : file) {
                if (workDir.endsWith(File.separator)) {
                    temp = new File(workDir + s);
                } else {
                    temp = new File(workDir + File.separator + s);
                }

                if (temp.isFile()) {
                    if (Objects.nonNull(fileConsumer)) {
                        fileConsumer.accept(temp);
                    }
                } else if (temp.isDirectory()) {
                    if (Objects.nonNull(dirBeforeConsumer)) {
                        dirBeforeConsumer.accept(temp);
                    }
                    fileVisitor(workDir + File.separator + s, fileConsumer, dirBeforeConsumer, dirAfterConsumer);
                    if (Objects.nonNull(dirAfterConsumer)) {
                        dirAfterConsumer.accept(temp);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("FileUtil.copyFolder 复制整个文件夹内容操作出错 e={}", new Gson().toJson(e));
        }
    }

    /**
     * 执行解压并按照内容hash上传到oss
     * @param sfPairList
     */
    private void unpackZipTarAndUpload(List<Pair<SourceInfoDo, File>> sfPairList, RepoInfoDo repo) {
        if (CollectionUtils.isEmpty(sfPairList)) {
            logger.error("DownloadSourceServiceImpl.unpackZipTarAndUpload, sfPairList is empty");
            return;
        }

        for (Pair<SourceInfoDo, File> sourceInfoDoFilePair : sfPairList) {
            File packageFile = sourceInfoDoFilePair.getRight();
            SourceInfoDo sourceInfoDo = sourceInfoDoFilePair.getLeft();
            if (packageFile.getName().endsWith(ZIP_SUFFIX)) {
                try {
                    // 解压zip并按文件内容hash压缩后上传到s3
                    boolean suc = unzipAndUploadHash(packageFile.getAbsolutePath(), sourceInfoDo);
                    if (!suc) {
                        logger.error("DownloadSourceServiceImpl.unpackZipTarAndUpload 失败");
                    }
                } catch (Exception e) {
                    logger.error("DownloadSourceServiceImpl.unpackZipTarAndUpload zip 源码压缩包解压上传失败 e={}", new Gson().toJson(e));
                }
            } else if (packageFile.getName().endsWith(TGZ_SUFFIX)) {
               // todo 解压tar并按文件hash上传到s3
                boolean suc = untarAndUploadHash(packageFile.getAbsolutePath(), sourceInfoDo);
                if (!suc) {
                    logger.error("DownloadSourceServiceImpl.untarAndUploadHash 失败");
                }
            } else {
                logger.error("DownloadSourceServiceImpl.unpackZipTarAndUpload tar 源码压缩包解压上传失败 {}", packageFile.getName());
            }
        }
    }


    private boolean unzipAndUploadHash(
            String zipFileName,
            SourceInfoDo sourceInfoDo) throws IOException {
        if (!ZipCompressUtil.checkFileSuffix(zipFileName)) {
            logger.info("unzipAndUploadHash wrong file suffix");
            return false;
        }

        ZipFile zipFile = new ZipFile(zipFileName);
        try {
            Enumeration<?> e = zipFile.getEntries();
            ZipEntry zipEntry;
            int batchUploadNum = 50;
            int curBatchNum = 0;
            Map<String, byte[]> batchUploadMap = new HashMap<>(batchUploadNum);
            List<Map<String, String>> commitFilesInfo = new ArrayList<>(100);
            String curRootDir = "";
            int minEntryDirLen = Integer.MAX_VALUE;
            while (e.hasMoreElements()) {
                zipEntry = (ZipEntry) e.nextElement();
                String zipEntryName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    if (zipEntryName.length() < minEntryDirLen) {
                        curRootDir = zipEntryName;
                        minEntryDirLen = zipEntryName.length();
                    }
                } else {
                    if (!FileUtils.isSourceCodeFile(zipEntryName)) {
                        logger.info("not source code file: " + zipEntryName);
                        continue;
                    }
                    InputStream in = zipFile.getInputStream(zipEntry);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(in, byteArrayOutputStream);
                    byte[] allContentBytes = byteArrayOutputStream.toByteArray();
                    if (0 == allContentBytes.length) {
                        logger.info("empty file: " + zipEntryName);
                        continue;
                    }

                    String utf8Content = FileUtils.convertContentToEncoding(allContentBytes, StandardCharsets.UTF_8);
                    byte[] compressedBytes = ZipCompressUtil.gzipCompress(utf8Content, StandardCharsets.UTF_8);
                    String hashStr = HashUtils.SHAForBytes(compressedBytes, "SHA-256");
                    batchUploadMap.put(hashStr, compressedBytes);

                    String fileUri = genFileUriFromContentSHA(hashStr);
                    Map<String, String> fileInfoMap = new HashMap<>(2);
                    fileInfoMap.put("fileUri", fileUri);
                    fileInfoMap.put("filePath", zipEntryName);
                    commitFilesInfo.add(fileInfoMap);

                    ++curBatchNum;
                    if (batchUploadNum == curBatchNum || !e.hasMoreElements()) {
                        // 执行上传
                        doBatchUpload(batchUploadMap);
                        batchUploadMap.clear();
                        curBatchNum = 0;
                    }
                }
            }

            if (!"".equals(curRootDir) && curRootDir.endsWith("/") && curRootDir.length() - 1 == curRootDir.indexOf("/")) {
                for(Map<String, String> cFile : commitFilesInfo) {
                    String oriFilePath = cFile.get("filePath");
                    cFile.put("filePath", oriFilePath.substring(curRootDir.length()));
                }
            } else {
                logger.error("cannot find zipped root dir: " + curRootDir);
                return false;
            }

            if (commitFilesInfo.size() > 0) {
                uploadCommitFilesList(commitFilesInfo, sourceInfoDo);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            zipFile.close();
        }
    }

    /**
     * 执行批量上传
     * @param md5ContentMap
     */
    private void doBatchUpload(Map<String, byte[]> md5ContentMap) {
        if (0 == md5ContentMap.size()) {
            return;
        }

        class UploadHash implements Runnable {
            private CountDownLatch latch;

            private String contentHash;

            private byte[] content;

            public UploadHash(CountDownLatch latch, String contentHash, byte[] content) {
                this.latch = latch;
                this.contentHash = contentHash;
                this.content = content;
            }

            @Override
            public void run() {
                try {
                    String fileUri = genFileUriFromContentSHA(contentHash);
                    if (s3Util.existContent(fileUri)) {
                        logger.info(fileUri + " exists, do not need upload");
                        return;
                    }
                    s3Util.saveContentByteArrays(fileUri, content);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("doBatchUpload completed, remaining task count: " + latch.getCount());
                    latch.countDown();
                }
            }
        }

        final CountDownLatch latch = new CountDownLatch(md5ContentMap.size());
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for(Map.Entry<String, byte[]> entry : md5ContentMap.entrySet()) {
            executorService.execute(new UploadHash(latch, entry.getKey(), entry.getValue()));
        }

        executorService.shutdown();

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private String genFileUriFromContentSHA(String contentSHA) {
        StringBuilder resUri = new StringBuilder("");
        int start = 0;
        int trunkLen = 3;
        while (start < contentSHA.length()) {
            int endIndex = start + trunkLen;
            if (endIndex > contentSHA.length()) {
                endIndex = contentSHA.length();
            }
            String curTrunk = contentSHA.substring(start, endIndex);
            resUri.append(curTrunk).append("/");

            start += trunkLen;
        }

        return resUri.substring(0, resUri.length() - 1);
    }


    private boolean untarAndUploadHash(String tarFileName,
                                       SourceInfoDo sourceInfoDo) {
        if (!FileUtils.exists(tarFileName)) {
            logger.error("TarUtil.unTar tar path not exists: " + tarFileName);
            return false;
        }

        try {
            //解压 .tar.gz文件
            int index = tarFileName.lastIndexOf('.');
            if (-1 == index || tarFileName.length() - 1 == index) {
                logger.error("invalid tar file name: " + tarFileName);
            }
            String tarFilePath = tarFileName.substring(0, index);
            File tarFile = new File(tarFilePath);
            ZipCompressUtil.gzipDepress(new File(tarFileName), tarFile);
            if (!tarFile.exists()) {
                logger.error("unzip tar file fail: " + tarFileName);
                return false;
            }

            ArchiveInputStream archiveInputStream =
                    new ArchiveStreamFactory()
                            .createArchiveInputStream("tar", new BufferedInputStream(new FileInputStream(tarFile)));
            int batchUploadNum = 50;
            int curBatchNum = 0;
            Map<String, byte[]> batchUploadMap = new HashMap<>(batchUploadNum);
            TarArchiveEntry tarEntry;
            List<Map<String, String>> commitFilesInfo = new ArrayList<>(100);
            String curRootDir = "";
            int minEntryDirLen = Integer.MAX_VALUE;
            while ((tarEntry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                String tarEntryName = tarEntry.getName();
                if (tarEntry.isDirectory()) {
                    if (tarEntryName.length() < minEntryDirLen) {
                        curRootDir = tarEntryName;
                        minEntryDirLen = tarEntryName.length();
                    }
                } else {
                    if (!FileUtils.isSourceCodeFile(tarEntryName)) {
                        logger.info("not source code file: " + tarEntryName);
                        continue;
                    }

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(archiveInputStream, byteArrayOutputStream);
                    byte[] allContentBytes = byteArrayOutputStream.toByteArray();
                    if (0 == allContentBytes.length) {
                        logger.info("empty file: " + tarEntryName);
                        continue;
                    }

                    String utf8Content = FileUtils.convertContentToEncoding(allContentBytes, StandardCharsets.UTF_8);
                    byte[] compressedBytes = ZipCompressUtil.gzipCompress(utf8Content, StandardCharsets.UTF_8);
                    String hashStr =
                            HashUtils.genShaFromBytesForEncoding(compressedBytes, "SHA-256", StandardCharsets.UTF_8);

                    String fileUri = genFileUriFromContentSHA(hashStr);
                    Map<String, String> fileInfoMap = new HashMap<>(2);
                    fileInfoMap.put("fileUri", fileUri);
                    fileInfoMap.put("filePath", tarEntryName);
                    commitFilesInfo.add(fileInfoMap);

                    batchUploadMap.put(hashStr, compressedBytes);
                    ++curBatchNum;
                    if (batchUploadNum == curBatchNum || null == archiveInputStream.getNextEntry()) {
                        // 执行上传
                        doBatchUpload(batchUploadMap);
                        batchUploadMap.clear();
                        curBatchNum = 0;
                    }
                }
            }

            if (!"".equals(curRootDir) && curRootDir.endsWith("/") && curRootDir.length() - 1 == curRootDir.indexOf("/")) {
                for (Map<String, String> cFile : commitFilesInfo) {
                    try {
                        String oriFilePath = cFile.get("filePath");
                        cFile.put("filePath", oriFilePath.substring(curRootDir.length()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                logger.error("cannot find zipped root entry: " + curRootDir);
                return false;
            }
            if (commitFilesInfo.size() > 0) {
                uploadCommitFilesList(commitFilesInfo, sourceInfoDo);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private void uploadCommitFilesList(List<Map<String, String>> commitFiles, SourceInfoDo sourceInfoDo) {
        String fileUri = COMMIT_FILES_URI_PREFIX + "/" + sourceInfoDo.getId();
        s3Util.saveStrContent(fileUri, new Gson().toJson(commitFiles));
    }


    private ZipInfoDo repackageZip(List<Pair<SourceInfoDo, File>> sfPairList, RepoInfoDo repo, List<ZipInfoDo> zips) throws IOException {
        if (CollectionUtils.isEmpty(sfPairList)) {
            logger.error("CodeInsightSearchDownload.repackage 源码压缩包重新打包失败 sfPairList is empty");
            return null;
        }

        SourceInfoDo sid = sfPairList.get(0).getLeft();
        String repoName = repo.nonAsciiRepoName();
        String repackageDir = TMP_REPACKAGE_DIR + repoName;
        String srcDir = repackageDir + File.separator + "src_package_00000001_0";
        File srcDirFile = new File(srcDir);
        if (!srcDirFile.exists()) {
            srcDirFile.mkdirs();
        }
        for (Pair<SourceInfoDo, File> sourceInfoDoFilePair : sfPairList) {
            // 下载待补充的压缩包并解压至目标目录
            SourceInfoDo source = sourceInfoDoFilePair.getLeft();
            File packageFile = sourceInfoDoFilePair.getRight();
            if (packageFile.getName().endsWith(ZIP_SUFFIX)) {
                try {
                    ZipCompressUtil.unZip(packageFile.getAbsolutePath(), srcDir + File.separator + String.format("source_%s", source.getId()));
                } catch (Exception e) {
                    logger.error("CodeInsightSearchDownload.repackage 源码压缩包重新打包失败 e={}", new Gson().toJson(e));
                }
            } else if (packageFile.getName().endsWith(TGZ_SUFFIX)) {
                TarUtil.unTar(packageFile.getAbsolutePath(), srcDir + File.separator + String.format("source_%s", source.getId()));
            } else {
                logger.error("CodeInsightSearchDownload.repackage 未知的打包格式 {}", packageFile.getName());
            }
        }
        if (CollectionUtils.isNotEmpty(zips)) {
            // 下载待更新的压缩包并解压至目标目录
            zips.forEach(zip -> {
                String sourceFilePath = repackageDir + File.separator + String.format("src_package_%d", zip.getIdx());
                String sourceFilePathZip = sourceFilePath + SEVEN_Z_SUFFIX;
                InputStream is = s3Util.fetchFile(zip.getOssKey());
                SourceUtil.downloadSourceFromOss(is, sourceFilePathZip);
                SevenZipUtil.p7zipDecompress(sourceFilePathZip, repackageDir);
                if (FileUtils.exists("/tools/cubesugar/" + repackageDir)) {
                    FileUtils.moveFolder("/tools/cubesugar/" + sourceFilePath, srcDir);
                    logger.warn("/tools/cubesugar/" + sourceFilePath + " exist");
                } else {
                    FileUtils.moveFolder(sourceFilePath, srcDir);
                }
                FileUtils.delete(sourceFilePath);
            });
        }

        // 重新对目标目录压缩并上传
        String srcDir7z = srcDir + SEVEN_Z_SUFFIX;
        beforePackage(srcDir);
        boolean compressRes = SevenZipUtil.p7zipCompress(srcDir, srcDir7z);
        if (!compressRes) {
            logger.info("SevenZipUtil.p7zipCompress failed, try to p7zip after tar cvf");
            srcDir7z = srcDir + SEVEN_Z_SUFFIX_AFTER_TAR;
            compressRes = SevenZipUtil.p7zipCompressAfterTarCompress(srcDir, srcDir7z);
            if (!compressRes) {
                throw new RuntimeException("SevenZipUtil.p7zipCompressAfterTarCompress still failed");
            } else {
                logger.info("SevenZipUtil.p7zipCompress failed, but SevenZipUtil.p7zipCompressAfterTarCompress success continue.....");
            }
        }
        FileUtils.delete(srcDir);
        String newSrcDir7z = srcDir7z;
        try {
            String ossKey = String.format(SOURCE_PACKAGE_VERSION_OSS_KEY_FORMAT, repo.getRepoType().name().toLowerCase(Locale.ROOT), repo.nonAsciiRepoName(), "00000001", 0);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(new File(newSrcDir7z).length());
            s3Util.saveFileWithMetaData(ossKey, new FileInputStream(newSrcDir7z), metadata);
            if (CollectionUtils.isNotEmpty(zips)) {
                for (ZipInfoDo unPackageZip : zips) {
                    if (unPackageZip.getIdx() == 0) {
                        // 更新SOURCE_ZIP压缩包相关信息
                        unPackageZip.setOssKey(ossKey);
                        unPackageZip.setPackageFlag(false);
                        unPackageZip.setZipSize(s3Util.fetchMetadata(ossKey).getContentLength());
                        logger.info("CodeInsightSearchDownload.repackage repo_info {} update source_zip of index 0 success", repo.getId());
                        return unPackageZip;
                    }
                }
                throw new RuntimeException("没有zip的idx==0");
            } else {
                ZipInfoDo szd = new ZipInfoDo();
                szd.setTenant(TenantEnum.ALIPAY);
                szd.setStatus(CommonStatusEnum.AVAILABLE);
                szd.setOssKey(ossKey);
                szd.setPackageFlag(false);
                szd.setZipSize(s3Util.fetchMetadata(ossKey).getContentLength());
                szd.setIdx(0);
                szd.setNum(1);
                szd.setRepoId(repo.getId());
                szd.setVersion("00000001");
                logger.info("CodeInsightSearchDownload.repackage repo_info {} insert source_zip of index 0 success", repo.getId());
                return szd;
            }
        } catch (FileNotFoundException e) {
            logger.error("CodeInsightSearchDownload.repackage FileNotFoundException e={}", new Gson().toJson(e));
            throw e;
        } catch (IOException e) {
            logger.error("CodeInsightSearchDownload.repackage IOException e={}", new Gson().toJson(e));
            throw e;
        }
    }
}
