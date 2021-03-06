package com.wpf.wdownloadtool.Thread;

import android.os.Handler;

import com.wpf.wdownloadtool.Tools.Check;
import com.wpf.wdownloadtool.Tools.DownloadInfo;
import com.wpf.wdownloadtool.Tools.SendMessage;
import com.wpf.wdownloadtool.WDownloadTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Created by wazsj on 5-16-0016.
 * 下载线程
 */

public class DownloadThread extends Thread {

    private DownloadInfo.ThreadDownloadInfo threadDownloadInfo;
    private int threadID = 0;
    private String downloadUrl;
    private long fileSize = 0,downSize = 0;
    private String fileName,filePath;
    private Handler handler;
    private boolean stop;

    @Override
    public void run() {
        super.run();

        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(downloadUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(5000);
            long downloadStartPosition = downloadStartPosition(fileSize);
            long downloadSize = downloadSize(fileSize);
            httpURLConnection.setRequestProperty("range", "bytes="
                    + downloadStartPosition + "-" + (downloadStartPosition + downloadSize -1));
            httpURLConnection.connect();
            InputStream is = httpURLConnection.getInputStream();
            RandomAccessFile fos = new RandomAccessFile (filePath + fileName,"rw");
            fos.seek(downloadStartPosition);
            byte[] buf = new byte[1024];
            int len;
            while (!stop && (len = is.read(buf)) > 0) {
                downSize += len;
                threadDownloadInfo.downSize += len;
                threadDownloadInfo.curPosition = downloadStartPosition + downSize;
                SendMessage.send(handler,0x04,"",len,threadID);
                fos.write(buf, 0, len);
            }
            is.close();
            fos.close();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            SendMessage.send(handler,0x01,e.getMessage(),0,0);
        } catch (IOException e) {
            e.printStackTrace();
            SendMessage.send(handler,0x01,e.getMessage(),0,0);
        }
        assert httpURLConnection != null;
        httpURLConnection.disconnect();
    }

    private long downloadStartPosition(long fileSize) {
        long pos;
        if(threadDownloadInfo.curPosition != 0) {
            pos = threadDownloadInfo.curPosition;
        } else {
            long averageSize = fileSize / WDownloadTool.threadNum;
            pos = averageSize * threadID;
        }
        return pos;
    }

    private long downloadSize(long fileSize) {
        long shouldDownloadSize ;
        long averageSize = fileSize / WDownloadTool.threadNum;
        if(threadID < WDownloadTool.threadNum - 1)
            shouldDownloadSize = averageSize;
        else
            shouldDownloadSize = fileSize - (WDownloadTool.threadNum - 1) * averageSize;
        shouldDownloadSize -= threadDownloadInfo.downSize;
        return shouldDownloadSize;
    }

    public DownloadThread setThreadID(int threadID) {
        this.threadID = threadID;
        return this;
    }

    public DownloadThread setDownloadUrl(String url) {
        downloadUrl = url;
        return this;
    }

    public DownloadThread setHandler(Handler handler) {
        this.handler = handler;
        return this;
    }

    public DownloadThread setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public DownloadThread setFilePath(String filePath) {
        this.filePath = filePath;
        Check.CheckDir(filePath);
        return this;
    }

    public DownloadThread setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public DownloadThread setStop() {
        this.stop = true;
        return this;
    }

    public long getDownloadSize() {
        return downSize;
    }

    public void setThreadDownloadInfo(DownloadInfo.ThreadDownloadInfo threadDownloadInfo) {
        this.threadDownloadInfo = threadDownloadInfo;
    }
}
