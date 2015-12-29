package com.uracer.racer.service;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Khoi Le
 */
@Component
public class BaseService {

    @Autowired
    private Environment env;
    @Autowired
    private FTPClient ftpClient;

    public synchronized OutputStream downloadFile(String fileName) throws Exception {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        executeFTPTask(new TaskExecutor<InputStream>() {
            @Override
            public void execute(InputStream... targets) throws Exception {
                ftpClient.retrieveFile(fileName, outputStream);
            }
        });
        return outputStream;
    }

    public synchronized void uploadFile(InputStream fileData, String fileName) throws Exception {

        executeFTPTask(new TaskExecutor<InputStream>() {
            @Override
            public void execute(InputStream... targets) throws Exception {
                if (!ArrayUtils.isEmpty(targets)) {
                    InputStream is = targets[0];
                    ftpClient.storeFile(fileName, is);
                } else {
                    ftpClient.storeFile(fileName, fileData);
                }
            }
        });
    }

    protected void executeFTPTask(TaskExecutor taskExecutor) throws Exception {

        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
        ftpClient.connect(env.getProperty("ftp.url"));
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())
                && ftpClient.login(env.getProperty("ftp.username"), env.getProperty("ftp.password"))) {
            taskExecutor.execute();
        }

    }
}
