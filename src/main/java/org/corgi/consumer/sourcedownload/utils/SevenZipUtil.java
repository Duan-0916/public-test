package org.corgi.consumer.sourcedownload.utils;

import com.google.gson.Gson;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tukaani.xz.LZMA2Options;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;

/**
 * @author fxt
 * @version : SevenZipUtil.java, v 0.1 2021/11/26 3:46 下午 fxt Exp $
 */
public class SevenZipUtil {

    private static final Logger logger = LogManager.getLogger(SevenZipUtil.class);

    public static boolean p7zipCompress(String inputFile, String outputFile) {
        //String[] commands = new String[3];
        //commands[0] = "/bin/sh";
        //commands[1] = "-c";
        //commands[2] = "7z a \"" + outputFile + "\" -mx9 \"" + inputFile + "\"";

        String[] commands = new String[] {
                "/opt/homebrew/bin/7z", "a", outputFile, "-mx9", inputFile
        };
        logger.info("command: {}", commands[2]);

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(commands);
        } catch (FileNotFoundException e) {
            logger.error("7z文件压缩出错：filenotfound {}", e);
            return false;
        } catch (IOException e) {
            logger.error("7z文件压缩出错：{}", e);
            return false;
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
        return exitVal == 0;
    }


    public static boolean p7zipCompressAfterTarCompress(String inputFile, String outputFile) {
        String[] commands = new String[3];
        commands[0] = "/bin/sh";
        commands[1] = "-c";
        commands[2] = "cd " + new File(inputFile).getParent() + " && tar cvf - " + new File(inputFile).getName() + " | 7za a -si " + outputFile;
        logger.info("command: {}", commands[2]);

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(commands);
        } catch (FileNotFoundException e) {
            logger.error("7z文件压缩tar文件出错：filenotfound {}", e);
            return false;
        } catch (IOException e) {
            logger.error("7z文件压缩tar文件出错：{}", e);
            return false;
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
        return exitVal == 0;
    }


    public static boolean p7DepressAfterTarCompress(String inputFile) {
        String[] commands = new String[3];
        commands[0] = "/bin/sh";
        commands[1] = "-c";
        commands[2] = "7za x -so " + inputFile + " | tar xf - ";
        logger.info("command: {}", commands[2]);

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(commands);
        } catch (FileNotFoundException e) {
            logger.error("7z文件压缩tar文件出错：filenotfound {}", e);
            return false;
        } catch (IOException e) {
            logger.error("7z文件压缩tar文件出错：{}", e);
            return false;
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
        return exitVal == 0;
    }

    public static boolean p7zipDecompress(String sourceFilePath, String outDir) {
        String[] commands = new String[3];
        commands[0] = "/bin/sh";
        commands[1] = "-c";
        commands[2] = "7z x \"" + sourceFilePath + "\" -o\"" + outDir + "\"";

        try {
            Process process = Runtime.getRuntime().exec(commands);
            int exitVal = process.waitFor();
            return exitVal == 0;
        } catch (InterruptedException | IOException e) {
            logger.error("SevenZUtil.p7zipDecompress 7z文件解压出错 e={}", new Gson().toJson(e));
        }

        return false;
    }

    /**
     * 7z文件压缩
     *
     * @param inputFile  待压缩文件夹/文件名
     * @param outputFile 生成的压缩包名字
     */
    public static void compress7z(String inputFile, String outputFile) {
        try {
            File input = new File(inputFile);
            if (!input.exists()) {
                logger.error("SevenZUtil.Compress7z 待压缩文件{}不存在", input.getPath());
                return;
            }
            SevenZOutputFile out = new SevenZOutputFile(new File(outputFile));
            LZMA2Options lzma2Options = new LZMA2Options(LZMA2Options.PRESET_MIN);
            SevenZMethodConfiguration configuration = new SevenZMethodConfiguration(SevenZMethod.LZMA2, lzma2Options);
            out.setContentMethods(Collections.singleton(configuration));

            compress(out, input, null);
            out.close();
        } catch (IOException e) {
            logger.error("SevenZUtil.Compress7z 7z文件压缩出错 e={}", new Gson().toJson(e));
        }
    }

    /**
     * 递归7z压缩
     *
     * @param name 压缩文件名
     */
    public static void compress(SevenZOutputFile out, File input, String name) {
        if (StringUtils.isBlank(name)) {
            name = input.getName();
        }

        try {
            SevenZArchiveEntry entry;
            if (input.isDirectory()) {
                // 如果路径为目录（文件夹）取出文件夹中的文件（或子文件夹）
                File[] fList = input.listFiles();

                assert fList != null;
                if (fList.length == 0) {
                    // 如果文件夹为空，则只需在目的地.7z文件中写入一个目录进入
                    entry = out.createArchiveEntry(input, name + File.separator);
                    out.putArchiveEntry(entry);
                } else {
                    // 如果文件夹不为空，则递归调用compress，文件夹中的每一个文件（或文件夹）进行压缩
                    for (File file : fList) {
                        compress(out, file, name + File.separator + file.getName());
                    }
                }
            } else {
                // 如果不是目录（文件夹），即为文件，则先写入目录进入点，之后将文件写入7z文件中
                FileInputStream fos = new FileInputStream(input);
                BufferedInputStream bis = new BufferedInputStream(fos);
                entry = out.createArchiveEntry(input, name);
                out.putArchiveEntry(entry);
                int len;
                // 将源文件写入到7z文件中
                byte[] buf = new byte[1024];
                while ((len = bis.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                bis.close();
                fos.close();
                out.closeArchiveEntry();
            }
        } catch (IOException e) {
            logger.error("SevenZUtil.compress 文件压缩出错 e={}", new Gson().toJson(e));
        }
    }

    public static void decompress(File file, String outDir) {
        try (SevenZFile sevenZipFile = new SevenZFile(file)) {
            byte[] buffer = new byte[4096];
            SevenZArchiveEntry entry;
            while ((entry = sevenZipFile.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File outputFile = new File(outDir + entry.getName());

                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    while (sevenZipFile.read(buffer) > 0) {
                        fos.write(buffer);
                    }
                } catch (IOException e) {
                    logger.error("SevenZUtil.decompress IOException e={}", new Gson().toJson(e));
                }
            }
        } catch (IOException e) {
            logger.error("SevenZUtil.decompress 文件解压出错 e={}", new Gson().toJson(e));
        }
    }
}
