package org.corgi.consumer.sourcedownload.utils;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.corgi.consumer.sourcedownload.utils.tar.TarEntry;
import org.corgi.consumer.sourcedownload.utils.tar.TarInputStream;
import org.corgi.consumer.sourcedownload.utils.tar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Stack;

/**
 * @author fxt
 * @version : TarUtil.java, v 0.1 2021/11/27 11:25 下午 fxt Exp $
 */
public class TarUtil {

    private static final Logger logger = LogManager.getLogger(TarUtil.class);

    public static boolean unTar(String tarPath, String unTarFolder) {
        if (!FileUtils.exists(tarPath)) {
            logger.error("TarUtil.unTar tar path not exists!");
            return false;
        }

        if (!FileUtils.mkdirs(unTarFolder, true)) {
            logger.error("TarUtil.unTar failed to create untar folder.");
            return false;
        }

        try {
            FileInputStream fis = new FileInputStream(tarPath);
            BufferedInputStream bis = new BufferedInputStream(fis);
            TarInputStream tis = new TarInputStream(bis);
            TarEntry te;

            while ((te = tis.getNextEntry()) != null) {
                String entryName = te.getName();
                logger.info("TarUtil.unTar untar entry " + entryName);

                String entryPath = unTarFolder + "/" + entryName;
                if (te.isDirectory()) {
                    FileUtils.mkdirs(entryPath);
                } else {
                    if (!FileUtils.create(entryPath, true)) {
                        logger.error("TarUtil.unTar failed to create file " + entryPath);
                        continue;
                    }

                    byte[] buffer = new byte[2048];
                    int count;
                    FileOutputStream fos = new FileOutputStream(entryPath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    while ((count = tis.read(buffer)) != -1) {
                        bos.write(buffer, 0, count);
                    }
                    bos.flush();
                    bos.close();
                }
            }
            tis.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean tar(String tarPath, String tarFolder) {
        if (!FileUtils.create(tarPath)) {
            logger.error("TarUtil.tar create tar file failed");
            return false;
        }
        if (!FileUtils.isFolder(tarFolder)) {
            logger.error("TarUtil.tar tar folder not exists!");
            return false;
        }

        if (FileUtils.childOf(tarPath, tarFolder)) {
            logger.error("TarUtil.tar can't create tar file under folder!");
            return false;
        }

        try {
            File tarFile = new File(tarPath);
            TarOutputStream tos = new TarOutputStream(tarFile);
            Stack<String> fileStack = new Stack<>();
            fileStack.push(tarFolder);
            String rootFolder = new File(tarFolder).getParent() + "/";
            while (!fileStack.isEmpty()) {
                String current = fileStack.pop();
                File currentFile = new File(current);

                if (FileUtils.isFolder(current)) {
                    File[] children = currentFile.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            fileStack.push(child.getAbsolutePath());
                        }
                    }
                } else {
                    String entryName = current.replace(rootFolder, "");
                    logger.error("TarUtil.tar tar entryName " + entryName);
                    TarEntry te = new TarEntry(currentFile, entryName);
                    tos.putNextEntry(te);

                    FileInputStream fis = new FileInputStream(currentFile);
                    BufferedInputStream origin = new BufferedInputStream(fis);
                    int count;
                    byte[] data = new byte[2048];
                    while ((count = origin.read(data)) != -1) {
                        tos.write(data, 0, count);
                    }
                    tos.flush();
                    origin.close();
                }
            }
            tos.close();
        } catch (Exception e) {
            logger.error("TarUtil.tar error e={}", new Gson().toJson(e));
        }

        return true;
    }
}
