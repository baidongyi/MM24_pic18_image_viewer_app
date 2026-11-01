package com.example.my18pic;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_READ_STORAGE = 100;

    String strMyPicTopFolderPath;
    String[] arrSubFolderList;

    int intFolderIndex;
    String strFolderPath;
    String strFolderName;
    int intImageIndex;
    String strFilePath;
    int intImageTotal;

    // 标记是否已经初始化过数据，避免 onResume 重复初始化
    private boolean boolDataInited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setImageEvent();

        // 先检查权限，权限就绪后再初始化数据
        checkPermissionAndInit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 用户从"所有文件访问权限"设置页返回时，会走到这里
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Environment.isExternalStorageManager()) {
            if (!boolDataInited) {
                initData();
                goNextFolder(null);
                boolDataInited = true;
            }
        }
    }

    /**
     * 检查权限，就绪后初始化数据；否则申请权限
     */
    private void checkPermissionAndInit() {

        // Android 11+ (API 30+)：需要 MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                initData();
                goNextFolder(null);
                boolDataInited = true;
            } else {
                // 跳到系统设置页让用户开启"所有文件访问"
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    // 部分机型没有这个页面，回退到总入口
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                Toast.makeText(this,
                        "请开启“所有文件访问”权限后返回",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Android 6 ~ 10：需要 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                initData();
                goNextFolder(null);
                boolDataInited = true;
            } else {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_READ_STORAGE);
            }
            return;
        }

        // Android 6 以下：安装时已授权，直接初始化
        initData();
        goNextFolder(null);
        boolDataInited = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_READ_STORAGE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initData();
                goNextFolder(null);
                boolDataInited = true;
            } else {
                Toast.makeText(this,
                        "没有存储权限，无法读取图片",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initData() {
        File topFolder = new File(strGetPicFolder());
        this.arrSubFolderList = topFolder.list();
        if (this.arrSubFolderList == null) {
            this.arrSubFolderList = new String[0];
            System.out.println("initData: 顶层目录读取失败或为空，路径 = "
                    + strGetPicFolder());
        }
    }

    private void setImageEvent() {

        ImageView iv = findViewById(R.id.iv);
        iv.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            @Override
            public void onSwipeLeft() {
                System.out.println("left");
                gotoImage(1);
            }

            @Override
            public void onSwipeRight() {
                System.out.println("right");
                gotoImage(-1);
            }

            @Override
            public void onSwipeTop() {
                System.out.println("top");
                goNextFolder(null);
            }

            @Override
            public void onSwipeBottom() {
                System.out.println("bottom");
            }
        });
    }

    private int intGetRandomNumber(int intMax) {
        Random r = new Random();
        return r.nextInt(intMax);
    }

    private String strGetPicFolder() {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();

        // 第一优先：根目录 /pic18
        File f1 = new File(root, "pic18");
        if (f1.exists() && f1.isDirectory()) {
            System.out.println("strGetPicFolder: 使用 " + f1.getAbsolutePath());
            return f1.getAbsolutePath();
        }

        // 第二优先：/Pictures/pic18
        File f2 = new File(root, "Pictures/pic18");
        if (f2.exists() && f2.isDirectory()) {
            System.out.println("strGetPicFolder: 使用 " + f2.getAbsolutePath());
            return f2.getAbsolutePath();
        }

        // 第三：都不存在，返回 null 让调用方处理
        System.out.println("strGetPicFolder: 两个目录都不存在");
        return null;
    }

    public void goNextFolder(View v) {
        this.strMyPicTopFolderPath = strGetPicFolder();
        System.out.println("goNextFolder: strMyPicTopFolderPath = " + strMyPicTopFolderPath);

        EditText editText = findViewById(R.id.search);
        String strSearchWord = editText.getText().toString();

        List<String> lstSubFolder = new ArrayList<>();

        for (int i = 0; i < this.arrSubFolderList.length; i++) {
            String strOneFolderName = arrSubFolderList[i];
            System.out.println(i + ": check strOneFolderName = " + strOneFolderName);
            if (strOneFolderName.contains(strSearchWord)
                    || strSearchWord.trim().length() == 0) {
                lstSubFolder.add(strOneFolderName);
            }
        }

        if (lstSubFolder.size() > 0) {

            intFolderIndex = intGetRandomNumber(lstSubFolder.size());
            strFolderName = lstSubFolder.get(intFolderIndex);

            System.out.println("intFolderIndex = " + intFolderIndex
                    + ", strFolderName = " + strFolderName);

            this.strFolderPath = this.strMyPicTopFolderPath + "/" + this.strFolderName;

            System.out.println("strFolderPath = " + strFolderPath);

            this.intImageIndex = 0;

            gotoImage(0);

        } else {

            editText.setText("未找到");
        }
    }

    private void setTitle() {
        this.setTitle((intImageIndex + 1) + "/" + intImageTotal + ":"
                + strFolderName.replace("_", ""));
    }

    public void goNextImage(View v) {
        gotoImage(10);
    }

    private void gotoImage(int intNumber) {

        String[] arrFileList;

        File folder = new File(this.strFolderPath);
        arrFileList = folder.list();

        try {
            intImageTotal = arrFileList.length;
        } catch (Exception e) {
            goNextFolder(null);
            return;
        }

        if (intImageTotal == 0) {
            goNextFolder(null);
            return;
        }

        intImageIndex = intImageIndex + intNumber;
        if (intImageIndex > arrFileList.length - 1) {
            goNextFolder(null);

        } else {
            if (intImageIndex < 0) {
                intImageIndex = arrFileList.length - 1;
            }

            strFilePath = arrFileList[intImageIndex];
            System.out.println("intImageIndex = " + intImageIndex
                    + ", file = " + strFilePath);

            setImageSrcByFileName(strFolderPath + "/" + strFilePath);

            setTitle();
        }
    }

    private void setImageSrcByFileName(String strFileName) {
        ImageView imageView = findViewById(R.id.iv);
        imageView.setImageDrawable(Drawable.createFromPath(strFileName));
    }
}