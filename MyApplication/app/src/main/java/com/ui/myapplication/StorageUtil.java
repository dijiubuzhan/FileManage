package com.ui.myapplication;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by wilson on 2017/4/27.
 */

public class StorageUtil {
    private static final String LOG_TAG ="StorageUtil:" ;

    public static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getExternalStorageStateExt(Context context, String path) {
        Log.d(LOG_TAG, "getExternalStorageStateExt: path = " + path);
        try {
            File sdcardSystem = new File(path);
            StatFs statfs = new StatFs(sdcardSystem.getPath());
            long blockSize = statfs.getBlockSize();
            long totalBlock = statfs.getBlockCount();
            if (totalBlock * blockSize < 10 * 1024 * 1024) {
                return Environment.MEDIA_REMOVED;
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, " = " + e.toString());
            if (Build.VERSION.SDK_INT < 23) {   //6.0以下就直接返回removed 状态  2015年12月22日9:47:51
                return Environment.MEDIA_REMOVED;
            }
        }

        if (path.contains(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            Log.d(LOG_TAG, "getExternalStorageStateExt: Environment.getExternalStorageState = " + Environment.getExternalStorageState());
            return Environment.getExternalStorageState();
        }
        //if /data/data开头的地址，就直接返回
        if (path.startsWith("/data/data"))
            return Environment.MEDIA_MOUNTED;

        if (isCanBulidFile(context, path, true) != null)
            return Environment.MEDIA_MOUNTED;

        return Environment.MEDIA_MOUNTED;
    }

    public static String isCanBulidFile(Context context, String sdcard, boolean isRoot) {
        Log.d(LOG_TAG, "isCanBulidFile: = sdcard " + sdcard);
        try {
            if (TextUtils.isEmpty(sdcard)) {
                Log.d(LOG_TAG, "invalid path " + sdcard);
                return null;
            }
        } catch (Exception e) {

        }
        try {
            if (context != null && !isRoot) {
                sdcard = sdcard + "/Android/data/" + context.getPackageName() + "/files";
            }
            new File(sdcard).mkdirs();
            Log.d(LOG_TAG, "isCanBulidFile: = path " + sdcard);
            //判断是否有权限生成文件做为判断 2016年3月14日20:21:57
            File file = new File(sdcard + "/test.txt");
            if (file.exists()) {
                file.delete();
            }
            File fileDir = new File(sdcard + "/test");
            if (fileDir.exists()) {
                fileDir.delete();
            }
            boolean isBulidDir = fileDir.mkdir();
            Log.d(LOG_TAG, "isCanBulidFileDir: = " + isBulidDir);
            if (file.createNewFile() && isBulidDir) {
                // if (file.createNewFile()) {
                fileDir.delete();
                file.delete();
                Log.d(LOG_TAG, "isCanBulidFile: = true");
                return sdcard;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<StorageInfo> getStorageList() {
        List<StorageInfo> list = new ArrayList<StorageInfo>();
        String def_path = Environment.getExternalStorageDirectory().getPath();
        boolean def_path_internal = !Environment.isExternalStorageRemovable();
        String def_path_state = Environment.getExternalStorageState();
        boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED) ||
                def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean def_path_readonly =
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        BufferedReader buf_reader = null;
        try {
            HashSet<String> paths = new HashSet<String>();
            buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            int cur_display_number = 1;
            Log.d(LOG_TAG, "/proc/mounts");
            while ((line = buf_reader.readLine()) != null) {
                Log.d(LOG_TAG, line);
                if (line.contains("vfat") || line.contains("/mnt")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    tokens.nextToken(); //device
                    String mount_point = tokens.nextToken(); //mount point
                    if (paths.contains(mount_point)) {
                        continue;
                    }
                    List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
                    boolean readonly = flags.contains("ro");

                    if (mount_point.equals(def_path)) {
                        paths.add(def_path);
                        list.add(0, new StorageInfo(def_path, def_path_internal, readonly, -1));
                    } else if (line.contains("/dev/block/vold")) {
                        if (!line.contains("/mnt/secure") && !line.contains("/mnt/asec") &&
                                !line.contains("/mnt/obb") && !line.contains("/dev/mapper") &&
                                !line.contains("tmpfs")) {
                            paths.add(mount_point);
                            list.add(new StorageInfo(mount_point, false, readonly, cur_display_number++));
                        }
                    }
                }
            }

            if (!paths.contains(def_path) && def_path_available) {
                list.add(0, new StorageInfo(def_path, def_path_internal, def_path_readonly, -1));
            }

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (buf_reader != null) {
                try {
                    buf_reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return list;
    }


    /*
    * 判断是路径是否可写
    * **/
    public static boolean isMount(Context context, String sdcard) {
        Log.d(LOG_TAG, "isMount: = sdcard " + sdcard);
        try {
            if (TextUtils.isEmpty(sdcard)) {
                Log.d(LOG_TAG, "invalid path " + sdcard);
                return false;
            }
        } catch (Exception e) {

        }

        if (Environment.MEDIA_MOUNTED.equals(getExternalStorageStateExt(context, sdcard))) {
            return true;
        }

        return false;
    }


    public static class StorageInfo {
        public final String path;
        public final boolean internal;
        public final boolean readonly;
        public final int display_number;

        StorageInfo(String path, boolean internal, boolean readonly, int display_number) {
            this.path = path;
            this.internal = internal;
            this.readonly = readonly;
            this.display_number = display_number;
        }

    }
}
