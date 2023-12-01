package org.corgi.consumer.sourcedownload.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author fxt
 * @version : OssUtil.java, v 0.1 2021/12/2 7:00 下午 fxt Exp $
 */
@Component(value = "s3Util")
public class S3Util {

    private final static  Logger logger = LogManager.getLogger(S3Util.class);

    private final static String BUCKET_NAME = "test-liang244";

    private final static String CONTENT_BUCKET_NAME = "file-content-sha";

    private final static Integer DEFAULT_URL_EXPITETIME_IN_HOUR = 2;

    @Autowired
    private AmazonS3 s3Client;

    /**
     * 文件是否存在
     *
     * @param fileUri
     * @return
     */
    public boolean exist(String fileUri) {
        return s3Client.doesObjectExist(BUCKET_NAME, fileUri);
    }

    /**
     * 文件是否存在
     *
     * @param fileUri
     * @return
     */
    public boolean existContent(String fileUri) {
        return s3Client.doesObjectExist(CONTENT_BUCKET_NAME, fileUri);
    }

    /**
     * 保存文件
     *
     * @param fileUri
     * @param inputStream
     */
    public void saveFile(String fileUri, InputStream inputStream) throws IOException {
        s3Client.putObject(BUCKET_NAME, fileUri, inputStream, new ObjectMetadata());
    }


    /**
     * 上传字符串内容
     * @param fileUri
     * @param strContent
     * @throws IOException
     */
    public void saveStrContent(String fileUri, String strContent) {
        s3Client.putObject(BUCKET_NAME, fileUri, strContent);
    }


    /**
     * 上传byte数组
     * @param fileUri
     * @param byteArrs
     */
    public void saveByteArrays(String fileUri, byte[] byteArrs) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrs);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(byteArrs.length);
        s3Client.putObject(BUCKET_NAME, fileUri, byteArrayInputStream, objectMetadata);
    }


    /**
     * 上传byte数组
     * @param fileUri
     * @param byteArrs
     */
    public void saveContentByteArrays(String fileUri, byte[] byteArrs) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrs);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(byteArrs.length);
        s3Client.putObject(CONTENT_BUCKET_NAME, fileUri, byteArrayInputStream, objectMetadata);
    }

    /**
     * 保存文件
     *
     * @param fileUri
     * @param inputStream
     */
    public void saveFileWithMetaData(String fileUri, InputStream inputStream, ObjectMetadata metadata) throws IOException {
        ObjectMetadata objectMetadata = null == metadata ? new ObjectMetadata() : metadata;
        s3Client.putObject(BUCKET_NAME, fileUri, inputStream, objectMetadata);
    }

    /**
     * 获得文件元数据
     *
     * @param fileUri
     * @return
     */
    public ObjectMetadata fetchMetadata(String fileUri) {
        return s3Client.getObjectMetadata(BUCKET_NAME, fileUri);
    }

    /**
     * 获得文件
     *
     * @param fileUri
     * @return
     */
    public InputStream fetchFile(String fileUri) {
        S3Object s3Object = s3Client.getObject(BUCKET_NAME, fileUri);
        return s3Object.getObjectContent();
    }

    public String genDownloadUrl(String fileUri) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, DEFAULT_URL_EXPITETIME_IN_HOUR);
        URL preSignedUrl = s3Client.generatePresignedUrl(BUCKET_NAME, fileUri, calendar.getTime());
        return preSignedUrl.toString();
    }

    /**
     * 获得内容
     *
     * @param fileUri
     * @return
     */
    public String fetchContext(String fileUri) {
        InputStream inputStream = s3Client.getObject(BUCKET_NAME, fileUri).getObjectContent();

        Integer BUF_LENGTH = 1024;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUF_LENGTH];
            int length;
            while ((length = inputStream.read(buffer, 0, 1024)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            logger.error("S3Util.fetchContext IOException={}", new Gson().toJson(e));
            return null;
        }
    }
}
