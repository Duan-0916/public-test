package org.corgi.consumer.sourcedownload.utils;

import java.io.File;
import java.io.IOException;

/**
 * @author fxt
 * @version : RuntimeUtil.java, v 0.1 2021/12/8 6:30 下午 fxt Exp $
 */
public class RuntimeUtil {

    public static int exec(String[] commands, String[] envp, File dir) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(commands, envp, dir);

        return process.waitFor();
    }
}
