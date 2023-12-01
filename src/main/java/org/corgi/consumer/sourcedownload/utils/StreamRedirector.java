package org.corgi.consumer.sourcedownload.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author fxt
 * @version : StreamRedirector.java, v 0.1 2021/12/7 1:17 下午 fxt Exp $
 */
public class StreamRedirector extends Thread {
    private static final Logger logger = LogManager.getLogger(StreamRedirector.class);
    InputStream is;

    StreamRedirector(InputStream is) {
        this.is = is;
    }

    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                logger.info("执行命令线程返回的结果：" + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
