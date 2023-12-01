package org.corgi.consumer.sourcedownload.utils;

import com.google.gson.Gson;
import info.monitorenter.cpdetector.io.*;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.corgi.consumer.sourcedownload.model.repo.RepoInfoDo;
import org.corgi.consumer.sourcedownload.model.repo.SourceInfoDo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author fxt
 * @version : FileUtil.java, v 0.1 2021/11/27 10:49 下午 fxt Exp $
 */
public class FileUtils {

    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    public static final long BYTE_BITS = 8L;
    public static final long KB_BITS = BYTE_BITS * 1024;
    public static final long MB_BITS = KB_BITS * 1024;
    public static final long GB_BITS = MB_BITS * 1024;
    public static final long TB_BITS = GB_BITS * 1024;
    public static final long PB_BITS = TB_BITS * 1024;

    public static final long MBPS_KBPS = 1024L;

    private static final List<String> SUFFIX_BLACKLIST = Arrays.asList(".art", ".blp", ".bmp", ".bti", ".cd5", ".cit", ".clip", ".cpl", ".cpt",
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

    public static boolean isSourceCodeFile(String filePath) {
        for(String fSuffix : SUFFIX_BLACKLIST) {
            if (filePath.endsWith(fSuffix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 删除目录及目录下的所有子目录和文件
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (Objects.isNull(dir)) {
            return false;
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            // 递归删除目录中的子目录下
            assert children != null;
            for (String child : children) {
                boolean isDelete = deleteDir(new File(dir, child));
                if (!isDelete) {
                    return false;
                }
            }
        }

        // 读到的是一个文件或者是一个空目录，则可以直接删除
        return dir.delete();
    }

    /**
     * 复制某个目录及目录下的所有子目录和文件到新文件夹
     *
     * @param oldPath
     * @param newPath
     */
    public static void copyFolder(String oldPath, String newPath) {
        if (oldPath.equalsIgnoreCase(newPath)) {
            return;
        }

        try {
            // 如果文件夹不存在，则建立新文件夹
            File newPathFile = new File(newPath);
            if (!newPathFile.exists()) {
                newPathFile.mkdirs();
            }
            // 读取整个文件夹的内容到file字符串数组，下面设置一个游标i，不停地向下移开始读这个数组
            File fileList = new File(oldPath);
            String[] file = fileList.list();
            if (Objects.isNull(file)) {
                return;
            }
            // 要注意，这个temp仅仅是一个临时文件指针
            // 整个程序并没有创建临时文件
            File temp;
            for (String s : file) {
                // 如果oldPath以路径分隔符/或者\结尾，那么则oldPath/文件名就可以了
                // 否则要自己oldPath后面补个路径分隔符再加文件名
                // 谁知道你传递过来的参数是f:/a还是f:/a/啊？
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + s);
                } else {
                    temp = new File(oldPath + File.separator + s);
                }

                // 如果游标遇到文件
                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    // 复制并且改名
                    FileOutputStream output = new FileOutputStream(newPath
                        + File.separator + temp.getName());
                    byte[] bufferArray = new byte[1024];
                    int preReadLength;
                    while ((preReadLength = input.read(bufferArray)) != -1) {
                        output.write(bufferArray, 0, preReadLength);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                // 如果游标遇到文件夹
                if (temp.isDirectory()) {
                    copyFolder(oldPath + File.separator + s, newPath + File.separator + s);
                }
            }
        } catch (Exception e) {
            logger.error("FileUtil.copyFolder 复制整个文件夹内容操作出错 e={}", new Gson().toJson(e));
        }
    }

    /**
     * 移动目录 先复制后删除
     *
     * @param oldPath
     * @param newPath
     */
    public static void moveFolder(String oldPath, String newPath) {
        // 先复制文件
        copyFolder(oldPath, newPath);
        // 则删除源文件，以免复制的时候错乱
        deleteDir(new File(oldPath));
    }

    public static boolean create(String absPath) {
        return create(absPath, false);
    }

    public static boolean create(String absPath, boolean force) {
        if (StringUtils.isBlank(absPath)) {
            return false;
        }

        if (exists(absPath)) {
            return true;
        }

        String parentPath = getParent(absPath);
        mkdirs(parentPath, force);

        try {
            File file = new File(absPath);
            return file.createNewFile();
        } catch (Exception e) {
            //PMD
        }
        return false;
    }

    public static boolean exists(String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return false;
        }
        File file = new File(absPath);
        return exists(file);
    }

    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    public static String getParent(String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return null;
        }
        absPath = cleanPath(absPath);
        File file = new File(absPath);
        return getParent(file);
    }

    public static String getParent(File file) {
        if (file == null) {
            return null;
        } else {
            return file.getParent();
        }
    }

    public static boolean childOf(String childPath, String parentPath) {
        if (StringUtils.isBlank(childPath) || StringUtils.isBlank(parentPath)) {
            return false;
        }
        childPath = cleanPath(childPath);
        parentPath = cleanPath(parentPath);
        return childPath.startsWith(parentPath + File.separator);
    }

    public static String cleanPath(String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return absPath;
        }
        try {
            File file = new File(absPath);
            absPath = file.getCanonicalPath();
        } catch (Exception e) {
            //PMD
        }
        return absPath;
    }

    public static boolean mkdirs(String absPath) {
        return mkdirs(absPath, false);
    }

    public static boolean mkdirs(String absPath, boolean force) {
        File file = new File(absPath);
        if (exists(absPath) && !isFolder(absPath)) {
            if (!force) {
                return false;
            }

            delete(file);
        }
        try {
            file.mkdirs();
        } catch (Exception e) {
            //PMD
        }
        return exists(file);
    }

    public static boolean isFolder(String absPath) {
        boolean exists = exists(absPath);
        if (!exists) {
            return false;
        }

        File file = new File(absPath);
        return file.isDirectory();
    }

    public static boolean delete(String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return false;
        }

        File file = new File(absPath);
        return delete(file);
    }

    public static boolean delete(File file) {
        if (!exists(file)) {
            return true;
        }

        if (file.isFile()) {
            return file.delete();
        }

        boolean result = true;
        File[] files = file.listFiles();
        assert files != null;
        for (File value : files) {
            result &= delete(value);
        }
        result &= file.delete();

        return result;
    }

    public static String getSuitableSizeString(String absPath) {
        if (StringUtils.isBlank(absPath)) {
            return null;
        }

        File file = new File(absPath);
        return getSuitableSizeString(file);
    }

    public static String getSuitableSizeString(File file) {
        if (!exists(file)) {
            return null;
        }
        return getSuitableSizeString(file.length() * BYTE_BITS);
    }

    public static String getSuitableSizeString(long bits) {
        if (bits < BYTE_BITS) {
            return bits + " bit";
        } else if (bits < KB_BITS) {
            return bits / BYTE_BITS + " byte";
        } else if (bits < MB_BITS) {
            return bits / (KB_BITS) + " KB";
        } else if (bits < GB_BITS) {
            return bits / MB_BITS + " MB";
        } else if (bits < TB_BITS) {
            return bits / GB_BITS + " GB";
        } else if (bits < PB_BITS) {
            return bits / TB_BITS + " TB";
        } else {
            return bits / PB_BITS + " PB";
        }
    }

    public static String getSuitableKbpsString(double kbps) {
        if (kbps < MBPS_KBPS) {
            return new DecimalFormat("#.00").format(kbps) + " kbps";
        } else {
            return new DecimalFormat("#.00").format(kbps / MBPS_KBPS) + " mbps";
        }
    }

    public static Long getLength(String filePath) {
        File fileObj = new File(filePath);
        if (!fileObj.exists()) {
            return 0L;
        }

        return fileObj.length();
    }


    public static String convertContentToEncoding(byte[] contentBytes, Charset targetEncoding) throws Exception {
        Charset oriEncoding = checkEncodingByInputStream(new ByteArrayInputStream(contentBytes));
        if (null == oriEncoding) {
            throw new Exception("unknown encoding");
        }

        try {
            String oriContentStr = new String(contentBytes, oriEncoding);

            return new String(oriContentStr.getBytes(targetEncoding), targetEncoding);
        } catch (Exception e) {
            throw e;
        }
    }

    public static String convertContentToEncoding(String filePath, Charset targetEncoding) throws Exception {
        byte[] contentBytes = getContent(filePath);
        if (0 == contentBytes.length) {
            throw new Exception("file content empty: " + filePath);
        }

        Charset oriEncoding = checkEncodingByFile(filePath);
        if (null == oriEncoding) {
            throw new Exception("unknown encoding");
        }

        String oriContentStr = new String(contentBytes, oriEncoding);

        return new String(oriContentStr.getBytes(targetEncoding), targetEncoding);
    }

    public static byte[] getContent(String filePath) throws Exception {
        File fileObj = new File(filePath);
        if (!fileObj.exists()) {
            return new byte[0];
        }

        FileInputStream fileInputStream = new FileInputStream(fileObj);
        FileChannel fileChannel = fileInputStream.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
        while (fileChannel.read(byteBuffer) > 0) {
            byteBuffer.clear();
        }

        return byteBuffer.array();
    }

    public static Charset checkEncodingByFile(String filePath) throws Exception {
        File fileObj = new File(filePath);
        if (!fileObj.exists()) {
            throw new Exception("file not exists: " + filePath);
        }

        // 获取 CodepageDetectorProxy 实例
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(new ParsingDetector(false));
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        detector.add(UnicodeDetector.getInstance());

        return detector.detectCodepage(fileObj.toURI().toURL());
    }


    public static Charset checkEncodingByInputStream(InputStream inputStream) throws Exception {
        // 获取 CodepageDetectorProxy 实例
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(new ParsingDetector(false));
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        detector.add(UnicodeDetector.getInstance());

        try {
            return detector.detectCodepage(inputStream, inputStream.available() - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static boolean unzipTest(
            String zipFileName) throws IOException {

        ZipFile zipFile = new ZipFile(zipFileName);
        try {
            Enumeration<?> e = zipFile.getEntries();
            ZipEntry zipEntry;
            while (e.hasMoreElements()) {
                zipEntry = (ZipEntry) e.nextElement();
                if (!zipEntry.isDirectory()) {
                    String zipEntryName = zipEntry.getName();
                    String a = "b";
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            zipFile.close();
        }
    }


    public static boolean untarTest(String tarFileName) {
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
            TarArchiveEntry tarEntry;
            while ((tarEntry = (TarArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                if (!tarEntry.isDirectory()) {
                    String tarEntryName = tarEntry.getName();
                    String a = "b";
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
