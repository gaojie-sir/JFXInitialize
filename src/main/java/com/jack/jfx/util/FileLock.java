package com.jack.jfx.util;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 程序单例运行检查类
 *
 * @author gj
 */
public class FileLock {
    /**
     * 需要锁的文件
     */
    private File lockFile = new File(System.getProperties().get("user.home") + File.separator + "AppData" + File.separator + "Roaming" + File.separator + "HNF" + File.separator + new File(System.getProperty("user.dir")).getName());
    /**
     * 该文件的文件锁
     */
    private java.nio.channels.FileLock fileLock = null;

    private FileOutputStream fileOutputStream;

    private String lockFileName;

    private static FileLock instance;

    public static FileLock getInstance() {
        if (instance == null) {
            instance = new FileLock();
        }
        return instance;
    }

    public String getLockFileName() {
        return lockFileName;
    }

    public void setLockFileName(String lockFileName) {
        this.lockFileName = lockFileName;
    }

    public FileLock() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deleteLockFile();
        }));
    }

    /**
     * 检测程序是否已启动
     *
     * @return true 已启动 false 未启动
     */
    public boolean tryApplicationStarted() {
        if (!lockFile.exists()) {
            lockFile.mkdirs();
        }
        try {
            fileOutputStream = new FileOutputStream(lockFile.getAbsolutePath() + File.separator + (lockFileName == null ? "single.lock" : lockFileName));
            fileLock = fileOutputStream.getChannel().tryLock();
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, null, e);
            return true;
        }
        if (fileLock == null) {
            return true;
        }
        return false;
    }


    /**
     * 关闭文件锁 并 删除文件
     */
    public void deleteLockFile() {
        if (fileLock != null) {
            if (lockFile.exists()) {
                //删除文件
                try {
                    lockFile.delete();
                } catch (Exception e) {
                    Logger.getGlobal().log(Level.SEVERE, null, e);
                }
            }
        }
    }
}
