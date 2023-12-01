package org.corgi.consumer.sourcedownload.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    public static String genShaFromStr(String str, String algorithmType, Charset charset) {
        return SHAForStr(str, algorithmType, charset);
    }

    public static String genShaFromFileForEncoding(String filePath, String algorithmType, Charset charset) throws Exception {
        String contentForCharset = FileUtils.convertContentToEncoding(filePath, charset);
        return SHAForStr(contentForCharset, algorithmType, StandardCharsets.UTF_8);
    }

    public static String genShaFromBytesForEncoding(byte[] contentBytes, String algorithmType, Charset charset) throws Exception {
        String contentForCharset = FileUtils.convertContentToEncoding(contentBytes, charset);
        return SHAForStr(contentForCharset, algorithmType, StandardCharsets.UTF_8);
    }

    /**
     * 字符串加密
     * @return
     */
    private static String SHAForStr(String str, String algorithmType, Charset charset) {
        // 返回值
        String strResult = null;

        // 是否是有效字符串
        if (str != null && str.length() > 0) {
            try {
                // SHA 加密开始
                // 创建加密对象 并傳入加密类型
                MessageDigest messageDigest = MessageDigest.getInstance(algorithmType);
                // 传入要加密的字符串
                messageDigest.update(str.getBytes(charset));
                // 得到 byte 类型结果
                byte[] byteBuffer = messageDigest.digest();

                // 將 byte 转为 string
                StringBuilder strHexString = new StringBuilder();
                // 遍历 byte buffer
                for (byte b : byteBuffer) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                // 得到返回结果
                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return strResult;
    }

    /**
     * byte数组加密
     * @param bytes
     * @param algorithmType
     * @return
     */
    public static String SHAForBytes(byte[] bytes, String algorithmType) {
        // 返回值
        String strResult = null;

        // 是否是有效字符串
        if (bytes != null && bytes.length > 0) {
            try {
                // SHA 加密开始
                // 创建加密对象 并傳入加密类型
                MessageDigest messageDigest = MessageDigest.getInstance(algorithmType);
                // 传入要加密的字符串
                messageDigest.update(bytes);
                // 得到 byte 类型结果
                byte[] byteBuffer = messageDigest.digest();

                // 將 byte 转为 string
                StringBuilder strHexString = new StringBuilder();
                // 遍历 byte buffer
                for (byte b : byteBuffer) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        strHexString.append('0');
                    }
                    strHexString.append(hex);
                }
                // 得到返回结果
                strResult = strHexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return strResult;
    }
}
