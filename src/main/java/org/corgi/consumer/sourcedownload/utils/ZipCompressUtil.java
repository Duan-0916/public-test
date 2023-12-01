package org.corgi.consumer.sourcedownload.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * @author fxt
 * @version : ZipCompressUtil.java, v 0.1 2021/11/27 11:11 下午 fxt Exp $
 */
public class ZipCompressUtil {

    /**
     * logger
     */
    private static final Logger logger = LogManager.getLogger(ZipCompressUtil.class);

    /**
     * 使用GBK编码可以避免压缩中文文件名乱码
     */
    private static final String CHINESE_CHARSET = "GBK";

    /**
     * 文件读取缓冲区大小
     */
    private static final int CACHE_SIZE = 1024;

    /**
     * 压缩文件
     *
     * @param sourceFolder 需压缩文件 或者 文件夹 路径
     * @param zipFilePath  压缩文件输出路径
     * @throws Exception
     */
    public static boolean zip(String sourceFolder, String zipFilePath) {
        boolean result = true;
        logger.info("ZipCompressUtil.zip 开始压缩 [" + sourceFolder + "] 到 [" + zipFilePath + "]");
        OutputStream out = null;
        try {
            out = new FileOutputStream(zipFilePath);
            BufferedOutputStream bos = new BufferedOutputStream(out);
            ZipOutputStream zos = new ZipOutputStream(bos);
            // 解决中文文件名乱码
            zos.setEncoding(CHINESE_CHARSET);
            File file = new File(sourceFolder);
            String basePath = null;
            if (file.isDirectory()) {
                basePath = file.getPath();
            } else {
                basePath = file.getParent();
            }
            zipFile(file, basePath, zos);
            zos.closeEntry();
            zos.close();
            bos.close();
            out.close();
        } catch (FileNotFoundException e) {
            result = false;
            logger.error("ZipCompressUtil.zip file not found exception : {}", e.getMessage());
        } catch (Exception e) {
            result = false;
            logger.error("ZipCompressUtil.zip exception : {}", e.getMessage());
        }

        logger.debug("ZipCompressUtil.zip 压缩 [" + sourceFolder + "] 完成！");
        return result;
    }

    /**
     * 递归压缩文件
     *
     * @param parentFile
     * @param basePath
     * @param zos
     * @throws Exception
     */
    private static void zipFile(File parentFile, String basePath, ZipOutputStream zos) {
        File[] files;
        if (parentFile.isDirectory()) {
            files = parentFile.listFiles();
        } else {
            files = new File[1];
            files[0] = parentFile;
        }
        String pathName;
        InputStream is;
        BufferedInputStream bis;
        byte[] cache = new byte[CACHE_SIZE];
        try {
            assert files != null;
            for (File file : files) {
                if (file.isDirectory()) {
                    basePath = basePath.replace('\\', '/');
                    if (basePath.substring(basePath.length() - 1).equals("/")) {
                        pathName = file.getPath().substring(basePath.length()) + "/";
                    } else {
                        pathName = file.getPath().substring(basePath.length() + 1) + "/";
                    }
                    zos.putNextEntry(new ZipEntry(pathName));
                    zipFile(file, basePath, zos);
                } else {
                    pathName = file.getPath().substring(basePath.length());
                    pathName = pathName.replace('\\', '/');
                    if (pathName.charAt(0) == '/') {
                        pathName = pathName.substring(1);
                    }
                    is = new FileInputStream(file);
                    bis = new BufferedInputStream(is);
                    zos.putNextEntry(new ZipEntry(pathName));
                    int nRead = 0;
                    while ((nRead = bis.read(cache, 0, CACHE_SIZE)) != -1) {
                        zos.write(cache, 0, nRead);
                    }
                    bis.close();
                    is.close();
                }
            }
        } catch (IOException e) {
            logger.error("ZipCompressUtil.zipFile io exception : {}", e.getMessage());
        }
    }

    public static boolean checkFileSuffix(String zipFilePath) {
        boolean result = false;
        List<String> zipFileSuffixList = Arrays.asList("zip", "rar");
        int lastDotIndex = zipFilePath.lastIndexOf('.');
        for (String suffix : zipFileSuffixList) {
            if (zipFilePath.substring(lastDotIndex + 1, zipFilePath.length()).equals(suffix)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 创建目录
     *
     * @param directory
     * @param subDirectory
     */
    private static void createDirectory(String directory, String subDirectory) {
        String[] dir;
        File fl = new File(directory);
        try {
            if (subDirectory.equals("") && !fl.exists()) {
                fl.mkdir();
            } else if (!subDirectory.equals("")) {
                dir = subDirectory.replace('\\', '/').split("/");
                StringBuilder directoryBuilder = new StringBuilder(directory);
                for (String s : dir) {
                    File subFile = new File(directoryBuilder + File.separator + s);
                    if (!subFile.exists()) {
                        subFile.mkdir();
                    }
                    directoryBuilder.append(File.separator).append(s);
                }
            }
        } catch (Exception e) {
            logger.error("ZipCompressUtil.createDirectory exception : {}", e.getMessage());
        }
    }

    /**
     * 解压zip文件
     *
     * @param zipFileName     待解压的zip文件路径
     * @param outputDirectory 解压目标文件夹
     */
    public static boolean unZip(String zipFileName, String outputDirectory) throws Exception {
        boolean result = true;
        if (!ZipCompressUtil.checkFileSuffix(zipFileName)) {
            logger.info("ZipCompressUtil.unZip wrong file suffix");
            return false;
        }

        ZipFile zipFile = new ZipFile(zipFileName);
        try {
            Enumeration<?> e = zipFile.getEntries();
            ZipEntry zipEntry;
            createDirectory(outputDirectory, "");
            while (e.hasMoreElements()) {
                zipEntry = (ZipEntry) e.nextElement();
                if (zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    name = name.substring(0, name.length() - 1);
                    File f = new File(outputDirectory + File.separator + name);
                    f.mkdir();
                } else {
                    String fileName = zipEntry.getName();
                    fileName = fileName.replace('\\', '/');
                    if (fileName.contains("/")) {
                        createDirectory(outputDirectory, fileName.substring(0, fileName.lastIndexOf("/")));
                    }
                    File f = new File(outputDirectory + File.separator + zipEntry.getName());
                    System.out.println(f.getAbsolutePath() + "   " + f.getAbsolutePath().length() + ".....");
                    f.createNewFile();
                    InputStream in = zipFile.getInputStream(zipEntry);
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] by = new byte[1024];
                    int c;
                    while ((c = in.read(by)) != -1) {
                        out.write(by, 0, c);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            result = false;
            logger.error("ZipCompressUtil.unZip exception : {}", e.getMessage());
        } finally {
            zipFile.close();
        }
        return result;
    }

    /**
     * gzip压缩字符串
     * @param str
     * @param charset
     * @return
     */
    public static byte[] gzipCompress(String str, Charset charset) {
        if (str == null || str.length() == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = null;

        try {
            gzipOutputStream = new GZIPOutputStream(out);
            gzipOutputStream.write(str.getBytes(charset.name()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (gzipOutputStream != null) {
                try {
                    gzipOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return out.toByteArray();
    }

    public static void gzipDepress(File gzipFile, File outputFile) {
        GZIPInputStream gzipInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        int bufSize = 4096;
        try {
            gzipInputStream = new GZIPInputStream(new FileInputStream(gzipFile));
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

            byte[] buf = new byte[bufSize];
            int len = -1;
            while ((len = gzipInputStream.read(buf)) != -1) {
                bufferedOutputStream.write(buf, 0, len);
            }
            bufferedOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (gzipInputStream != null) {
                try {
                    gzipInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
