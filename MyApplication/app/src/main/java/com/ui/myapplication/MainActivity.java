package com.ui.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private ListView mListView;
    private List<FileItem> mData;
    private String mDir;
    private String outSdcard;
    private String innerSdcard;
    private static final String SDCARD_ROOT_DEFAULT = "/storage";
    private static int mPosition;
    private static Stack<Integer> mPositionStack = new Stack<Integer>();
    boolean isroot = false;
    RecyclerView m_recycleView;
    RecycleAdapter m_RecycleAdapter;
    List<String> mRData = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        initViews();

        permission();
    }


    private void initViews() {


        outSdcard = StorageUtil.getStoragePath(this, true);
        innerSdcard = StorageUtil.getStoragePath(this, false);


        mData = new ArrayList<FileItem>();
        FileItem fileItem = null;
        boolean isMount = StorageUtil.isMount(this, outSdcard);
        if (!TextUtils.isEmpty(outSdcard) && isMount) {
            fileItem = new FileItem(getString(R.string.out_sdcard), outSdcard, false);
            mData.add(fileItem);
        }

        if (!TextUtils.isEmpty(innerSdcard)) {
            fileItem = new FileItem(getString(R.string.inner_sdcard), innerSdcard, false);
            mData.add(fileItem);
        }

        mListView = (ListView) findViewById(R.id.file_list_view);

        m_recycleView = (RecyclerView) findViewById(R.id.recycle);
        m_recycleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        m_recycleView.setItemAnimator(new DefaultItemAnimator());

        initData();
    }


    private void initData() {

        FileAdapter adapter = new FileAdapter(this);

        mListView.setAdapter(adapter);


        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                // TODO Auto-generated method stub
                mPositionStack.push(mPosition);
                Log.d(LOG_TAG, "initListViews, " + mData.get(arg2).filePath);
                if (!mData.get(arg2).isZip) {
                    mDir = mData.get(arg2).filePath;
                    mData = getData();
                    FileAdapter adapter = new FileAdapter(MainActivity.this);
                    mListView.setAdapter(adapter);
                }
            }

        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mPosition = mListView.getFirstVisiblePosition();
                }
            }

        });

    }


    @Override
    public void onBackPressed() {
        if (isroot || TextUtils.isEmpty(mDir)) {
            mPositionStack.clear();
            super.onBackPressed();
        } else if (!TextUtils.isEmpty(innerSdcard) && mDir.equals(innerSdcard)) {
            isroot = true;
            mDir = new File(innerSdcard).getParent();
            initViews();
        } else if (!TextUtils.isEmpty(outSdcard) && mDir.equals(outSdcard)) {
            isroot = true;
            mDir = new File(outSdcard).getParent();
            initViews();
        } else {
            isroot = false;
            File f = new File(mDir);
            mDir = f.getParent();
            mData = getData();
            FileAdapter adapter = new FileAdapter(MainActivity.this);
            mListView.setAdapter(adapter);
            if (mPositionStack.size() > 0) {
                int first = mPositionStack.pop();
                mListView.setSelection(first);
            }
        }
    }


    private List<FileItem> getData() {
        List<FileItem> list = new ArrayList<>();
        File f = new File(mDir);
        File[] files = f.listFiles();

        files = getSdcardList();

        if (!TextUtils.isEmpty(innerSdcard) && mDir.contains(innerSdcard)) {
            mRData = new ArrayList<String>(Arrays.asList(mDir.replace(innerSdcard, getString(R.string.inner_sdcard)).split("/")));
        } else if (!TextUtils.isEmpty(outSdcard) && mDir.contains(outSdcard)) {
            mRData = new ArrayList<String>(Arrays.asList(mDir.replace(outSdcard, getString(R.string.out_sdcard)).split("/")));
        }


        m_RecycleAdapter = new RecycleAdapter(this, mRData);
        m_RecycleAdapter.setOnItemClickListener(new RecycleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                for (int i = mRData.size() - 1; i > position; i--) {
                    mRData.remove(i);
                    onBackPressed();
                }

                m_recycleView.setAdapter(m_RecycleAdapter);
                m_RecycleAdapter.notifyDataSetChanged();
                m_recycleView.smoothScrollToPosition(mRData.size() - 1);
            }
        });
        m_recycleView.setAdapter(m_RecycleAdapter);
        m_RecycleAdapter.notifyDataSetChanged();
        m_recycleView.smoothScrollToPosition(mRData.size() - 1);


        if (files != null) {
            for (int i = 0; i < files.length; i++) {

                if (files[i].isDirectory()) {

                    list.add(new FileItem(files[i].getName(), files[i].getPath(), false));
                } else {
                    if (files[i].getName().toLowerCase(Locale.US).endsWith(".zip")) {
                        list.add(new FileItem(files[i].getName(), files[i].getPath(), true));
                    }

                }
            }
        }
        return list;
    }


    //过滤 SD卡
    private File[] getSdcardList() {

        File[] files = new File(mDir).listFiles();
        ArrayList<String> newFile = new ArrayList<>();

        if (mDir.equals(SDCARD_ROOT_DEFAULT)) {


            List<StorageUtil.StorageInfo> infos = StorageUtil.getStorageList();
            if (infos != null) {
                for (int i = 0; i < infos.size(); i++) {
                    String infoPath = infos.get(i).path;
                    if (infoPath != null) {
                        newFile.add(infoPath);
                    }
                }

                if (newFile.size() > 0) {
                    File[] newFiles = new File[newFile.size()];
                    for (int i = 0; i < newFiles.length; i++) {
                        newFiles[i] = new File(newFile.get(i));
                    }
                    return newFiles;
                } else {
                    File[] newFiles2 = new File[1];
                    newFiles2[0] = Environment.getExternalStorageDirectory();
                    return newFiles2;
                }
            } else {
                File[] newFiles3 = new File[1];
                newFiles3[0] = Environment.getExternalStorageDirectory();
                return newFiles3;
            }

        }
        return files;
    }


    public class FileItem {
        String fileName;
        String filePath;
        boolean isZip;

        public FileItem(String fileName, String filePath, boolean isZip) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.isZip = isZip;
        }
    }


    public class FileAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FileAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mData.size();
        }

        public Object getItem(int arg0) {
            return mData.get(arg0);
        }

        public long getItemId(int arg0) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.browser_file_list_item, null);
                holder.img = (ImageView) convertView.findViewById(R.id.img);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FileItem item = (FileItem) getItem(position);

            if (item.isZip) {
                holder.img.setImageResource(R.mipmap.ex_doc);
            } else {
                holder.img.setImageResource(R.mipmap.ex_folder);
            }

            holder.title.setText(item.fileName);

            return convertView;
        }


        public final class ViewHolder {
            public ImageView img;
            public TextView title;

        }

    }


    private void permission() {

        if (Build.VERSION.SDK_INT == 23) {
            if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else if (Build.VERSION.SDK_INT >= 24) {
            if (!(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
            if (!(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
