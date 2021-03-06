package com.hfut.buaa.data.manager.repository.impl;

import com.hfut.buaa.data.manager.exception.FileAlreadyExistsException;
import com.hfut.buaa.data.manager.model.BucketInst;
import com.hfut.buaa.data.manager.model.DataInst;
import com.hfut.buaa.data.manager.model.User;
import com.hfut.buaa.data.manager.repository.BucketInstDao;
import com.hfut.buaa.data.manager.repository.DaoInst;
import com.hfut.buaa.data.manager.repository.DataInstDao;
import com.hfut.buaa.data.manager.utils.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BufferedFSInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by tanweihan on 16/11/12.
 */
@Repository
public class DataInstDaoImpl extends DaoInst implements DataInstDao {
    @Autowired(required = true)
    private BucketInstDao bucketInstDao;

    /**
     * @param path
     * @return
     */
    @Override
    public String getFileString(String path) {
        Configuration conf = FileUtils.configuration;
        FileSystem fs = null;
        StringBuffer stringBuffer = new StringBuffer();
        InputStream hdfsInStream = null;
        BufferedInputStream in = null;
        try {
            byte[] bytes = new byte[1024];
            fs = FileSystem.get(URI.create(path), conf);
            hdfsInStream = fs.open(new Path(path));
            in = new BufferedInputStream(hdfsInStream);
            int len;
            while ((len = in.read(bytes)) != -1) {
                stringBuffer.append(FileUtils.bytesToString(bytes));
            }
            clossAll(fs, hdfsInStream, in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    /**
     * @param url
     */
    @Override
    public void deleteFileString(String url) {
        try {
            FileSystem hdfs = FileSystem.get(new java.net.URI(url),
                    new org.apache.hadoop.conf.Configuration());
            Path path = new Path(url);
            if (hdfs.exists(path)) {
                hdfs.delete(path, true);
            } else {
                throw new FileNotFoundException("hdfs path " + url + " have not found file!");
            }
            hdfs.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param url
     * @param fileString
     */
    @Override
    public void saveFileString(String url, String fileString) {
        byte[] bytes = new byte[1024];
        OutputStream outputStream = null;
        try {
            FileSystem hdfs = FileSystem.get(new java.net.URI(url),
                    new org.apache.hadoop.conf.Configuration());
            Path path = new Path(url);
            if (!hdfs.exists(path)) {
                outputStream = hdfs.create(path);
                outputStream.write(FileUtils.stringToBytes(fileString));
                outputStream.flush();
                outputStream.close();
                hdfs.close();
            } else {
                throw new FileAlreadyExistsException("hdfs file " + url + " is alread exists!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新文件
     *
     * @param userId
     * @param bucketId
     * @param dataInstId
     * @param fileString
     * @param isDelete
     */
    @Override
    public void updateFileString(long userId, long bucketId,
                                 long dataInstId, String fileString, boolean isDelete) {
        // 判断是否存在，id是否能对应上，权限是否为2
        DataInst dataInst = bucketInstDao.getDataInst(userId, bucketId, dataInstId);
        String string = dataInst.getFileString();
        String path = dataInst.getFilePath();
        if (!isDelete && string.length() > 0) {
            // 添加
            // 抛出文件已存在异常
            throw new FileAlreadyExistsException("file is already exists that userId is " + userId + " and " +
                    "bucketId is " + bucketId + " and dataInstId is " + dataInstId);
            // 更新
        } else if (string.length() > 0 && path.length() > 0) {
            // delete
            deleteFileString(path);
        }
        saveFileString(path, fileString);
    }


}
