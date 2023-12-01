package org.corgi.consumer.sourcedownload.utils;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * @author fxt
 * @version : SourceService.java, v 0.1 2021/12/2 4:51 下午 fxt Exp $
 */
public class SourceUtil {

    private static final Logger logger = LogManager.getLogger(SourceUtil.class);

    private static final int RETRY_MAX = 4;
    private static final int REPORT_REPORT_FREQUENCY = 10;

    public static final long TIMEOUT = 30 * 60 * 1000L;

    public static boolean aria2cDownloadSourceFromUrl(String srcFileUrl, String dstFilePath, long startTime) {
        int count = 0;
        do {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime > TIMEOUT) {
                break;
            }
            //根据api，其实exec方法可以直接执行命令，但是推荐用数组。
            //因为如果一些命令中包含特殊符号，命令会失效。经过测试如果直接执行复制命令，
            //目录可以复制过来，但是目录下的文件会丢失
            //String[] commands = new String[3];
            //commands[0] = new String("/bin/sh");
            //commands[1] = new String("-c");
            //aria2c -x 16 "srcUrl" -o dstFile

            File destFile = new File(dstFilePath);
            String resDir = destFile.getParent();
            String resFileName = destFile.getName();
            String[] commands = new String[] {
                    "/opt/homebrew/bin/aria2c", "-x", "16", srcFileUrl, "--dir", resDir, "-o", resFileName
            };
            Runtime rt = Runtime.getRuntime();
            Process proc;
            try {
                proc = rt.exec(commands);
            } catch (FileNotFoundException e) {
                logger.error("执行aria2c命令出错：filenotfound {}", e);
                break;
            } catch (IOException e) {
                logger.error("执行aria2c命令出错：{}", e);
                count++;
                continue;
            }
            //exec执行Linux命令其实会在系统环境下启动一个子进程
            //外部程序的输出流、错误流会输入到虚拟机进程中，而默认的缓冲区大小有限，
            //当输出、错误打印量较大时，会阻塞进程的执行，虚拟机也可能会死锁。
            //看文档找资料并没有找到很好的处理方式，目前只能这样处理
            StreamRedirector errors = new StreamRedirector(proc.getErrorStream());
            StreamRedirector outputs = new StreamRedirector(proc.getInputStream());
            errors.start();
            outputs.start();
            int exitVal = -1;    //如果返回的exitVal为0，则为进程正确结束，从而判断复制是否成功
            try {
                exitVal = proc.waitFor();
                logger.info("exitVal = {}", exitVal);
            } catch (InterruptedException e) {
                logger.error("执行命令的子进程没有正确结束：{}", e);
            }
            if (exitVal == 0) {
                return true;
            } else if (exitVal == 3) {
                //errorCode=3 Resource not found
                return false;
            }
            count++;
        } while (count <= RETRY_MAX - 1);

        // 没有下载成功 删除空文件
        logger.warn("SourceServiceImpl.downloadSourceFromUrl download target file {} to local file {} failed, retry {}", srcFileUrl, dstFilePath, count);
        FileUtils.delete(dstFilePath);
        return false;
    }

    /**
     * 从WEB下载源码
     *
     * @param sourceUrl
     * @param targetFile
     * @param start
     * @return
     */
    public static boolean downloadSourceFromUrl(String sourceUrl, String targetFile, long start) {
        URL httpUrl;
        try {
            httpUrl = new URL(sourceUrl);
        } catch (MalformedURLException e) {
            logger.error("SourceServiceImpl.downloadSourceFromUrl invalid download url {}, e.msg={}", sourceUrl, e.getMessage());
            return false;
        }

        File file = new File(targetFile);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                boolean create = file.createNewFile();
                if (!create) {
                    return false;
                }
            } catch (IOException e) {
                logger.error("SourceServiceImpl.downloadSourceFromUrl create target file {} failed, e={}", targetFile, new Gson().toJson(e));
                return false;
            }
        }

        int count = 0;
        do {
            long end = System.currentTimeMillis();
            if (end - start > TIMEOUT) {
                logger.warn("SourceInfoServiceImpl.downloadSourceFromUrl startTime={} endTime={}", new Date(start), new Date(end));
                break;
            }
            try {
                long startTime = System.currentTimeMillis();
                org.apache.commons.io.FileUtils.copyURLToFile(httpUrl, file);
                long endTime = System.currentTimeMillis();
                double second = (endTime - startTime) / 1000d;
                long bit = new File(targetFile).length();
                double kbps = bit / (1024d * 8) / second;
                logger.info("SourceInfoServiceImpl.downloadSourceFromUrl download {}({}) from web {}",
                    targetFile, FileUtils.getSuitableSizeString(targetFile), FileUtils.getSuitableKbpsString(kbps));
                return true;
            } catch (FileNotFoundException e) {
                logger.error("SourceInfoServiceImpl.downloadSourceFromUrl FileNotFoundException {} , e.msg={}", targetFile, e.getMessage());
                break;
            } catch (IOException e) {
                logger.warn("SourceInfoServiceImpl.downloadSourceFromUrl download {} from web, e.msg={}", targetFile, e.getMessage());
            }
            if (++count % REPORT_REPORT_FREQUENCY == 0) {
                logger.warn("SourceInfoServiceImpl.downloadSourceFromUrl download {} from web, retry {}", targetFile, count);
            }
        } while (count <= RETRY_MAX - 1);

        // 没有下载成功 删除空文件
        logger.warn("SourceServiceImpl.downloadSourceFromUrl download target file {} to local file {} failed, retry {}", sourceUrl, targetFile, count);
        FileUtils.delete(file);
        return false;
    }

    /**
     * 从OSS下载源码
     *
     * @param is
     * @param targetFile
     * @return
     */
    public static boolean downloadSourceFromOss(InputStream is, String targetFile) {
        try {
            File file = new File(targetFile);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                try {
                    boolean create = file.createNewFile();
                    if (!create) {
                        return false;
                    }
                } catch (IOException e) {
                    logger.error("SourceServiceImpl.downloadSourceFromOss create target file {} failed, e={}", targetFile, new Gson().toJson(e));
                    return false;
                }
            }

            long startTime = System.currentTimeMillis();
            FileOutputStream fos = new FileOutputStream(targetFile);
            byte[] b = new byte[1024 * 4];
            int length;
            while ((length = is.read(b)) > 0) {
                fos.write(b, 0, length);
            }
            is.close();
            fos.close();

            long endTime = System.currentTimeMillis();
            double second = (endTime - startTime) / 1000d;
            long bit = new File(targetFile).length();
            double kbps = bit / (1024d * 8) / second;
            logger.info("SourceInfoServiceImpl.downloadSourceFromOss download {}({}) from oss {}",
                targetFile, FileUtils.getSuitableSizeString(targetFile), FileUtils.getSuitableKbpsString(kbps));

            return true;
        } catch (FileNotFoundException e) {
            logger.error("SourceServiceImpl.downloadSourceFromOss FileNotFoundException e={}", new Gson().toJson(e));
        } catch (IOException e) {
            logger.error("SourceServiceImpl.downloadSourceFromOss IOException e={}", new Gson().toJson(e));
        }

        return false;
    }
}
