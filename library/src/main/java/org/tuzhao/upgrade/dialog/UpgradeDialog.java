package org.tuzhao.upgrade.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import org.tuzhao.upgrade.R;
import org.tuzhao.upgrade.UpgradeBuildInfo;
import org.tuzhao.upgrade.bean.UpgradeInfoBean;
import org.tuzhao.upgrade.wiget.UpgradeBaseOnClickListener;
import org.tuzhao.upgrade.wiget.UpgradeWeakHandler;
import org.tuzhao.upgrade.wiget.UpgradeWeakRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

/**
 * 显示版本更新的dialog
 *
 * @author tuzhao
 */
public final class UpgradeDialog extends AppCompatDialog {

    private static final String TAG = "UpgradeDialog";

    private UpgradeInfoBean bean;
    private boolean isForce;
    private static WeakHashMap<String, WeakReference<UpgradeDialog>> map = new WeakHashMap<>();
    private static WeakReference<DownloadThread> th;
    private int count;
    private Activity activity;
    private String fileProvider;
    private Helper helper;

    /**
     * @param context Activity
     * @param bean    UpgradeBean
     * @return 根据返回的true或者false来判断当前dialog是否显示成功
     */
    public static boolean show(Activity context, UpgradeInfoBean bean, Helper helper) {
        if (null == bean || null == context) {
            return false;
        }

        if (context.isFinishing()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (context.isDestroyed()) {
                return false;
            }
        }

        /*
         * 清除已有的dialog
         */
        close(context);
        UpgradeDialog dialog = new UpgradeDialog(context, bean, helper);
        dialog.show();
        return true;
    }

    public static void close() {
        try {
            ArrayList<String> list = new ArrayList<>(map.keySet());
            for (int i = 0; i < list.size(); i++) {
                String key = list.get(i);
                WeakReference<UpgradeDialog> wr = map.get(key);
                if (null != wr) {
                    UpgradeDialog temp = wr.get();
                    if (null != temp && temp.isShowing()) {
                        temp.dismiss();
                    }
                    wr.clear();
                }
            }
            map.clear();
        } catch (Exception e) {
            if (UpgradeBuildInfo.DEBUG_MODE) {
                Log.w(TAG, "close upgrade dialog cause error", e);
            }
        }
    }

    public static void close(Activity activity) {
        try {
            if (null == activity) return;
            String key = activity.getClass().getName();
            WeakReference<UpgradeDialog> wr = map.get(key);
            if (null != wr) {
                UpgradeDialog temp = wr.get();
                if (null != temp && temp.isShowing()) {
                    temp.dismiss();
                }
                wr.clear();
            }
            map.remove(key);
        } catch (Exception e) {
            if (UpgradeBuildInfo.DEBUG_MODE) {
                Log.w(TAG, "close upgrade dialog cause error", e);
            }
        }
    }

    private static void clearThread() {
        if (null != th) {
            DownloadThread thread = th.get();
            if (null != thread) {
                thread.close();
            }
            th.clear();
            th = null;
        }
    }

    private UpgradeDialog(@NonNull Activity context, UpgradeInfoBean bean, Helper helper) {
        this(context, R.style.UGDialog);
        this.bean = bean;
        this.activity = context;
        this.isForce = bean.isForceUpgrade();
        this.helper = helper;
        this.fileProvider = bean.getUpgradeFileProvider();
    }

    private UpgradeDialog(@NonNull Activity context, int themeResId) {
        super(context, themeResId);
    }

    private TextView mBtCancel;
    private TextView mBtSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        setContentView(R.layout.ug_dialog_version_upgrade);
        count = 0;
        TextView mTitle = findViewById(R.id.dialog_update_title);
        TextView mMessage = findViewById(R.id.dialog_update_message);
        mBtCancel = findViewById(R.id.dialog_update_bt_cancel);
        mBtSubmit = findViewById(R.id.dialog_update_bt_submit);

        if (mTitle != null) {
            mTitle.setText(bean.getUpgradeTitle());
        }
        if (mMessage != null) {
            mMessage.setText(bean.getUpgradeDescription());
        }

        mBtCancel.setOnClickListener(new CancelClickListener());
        mBtSubmit.setOnClickListener(new SubmitClickListener());

        setCanceledOnTouchOutside(false);
        setCancelable(!isForce);
        if (isForce) {
            mBtCancel.setVisibility(View.GONE);
        }
        log("context: " + context.getClass().getName());
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (isForce) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                log("received a back event");
                count++;
                if (count == 1) {
                    showToast(R.string.ug_pressed_again_back);
                    new Handler().postDelayed(new ResetRunnable(this), 3000);
                }
                if (count == 2) {
                    if (isCanUseHelper()) {
                        helper.exitApp();
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private static class ResetRunnable extends UpgradeWeakRunnable<UpgradeDialog> {

        ResetRunnable(UpgradeDialog dialog) {
            super(dialog);
        }

        @Override
        public void weakRun(UpgradeDialog dialog) {
            dialog.resetCount();
        }
    }

    private void resetCount() {
        this.count = 0;
    }

    private static void log(String msg) {
        if (UpgradeBuildInfo.DEBUG_MODE) {
            Log.d("UpgradeDialog", msg);
        }
    }

    private class CancelClickListener extends UpgradeBaseOnClickListener {
        @Override
        public void click(View v) {
            log("click cancel");
            if (null != helper) {
                if (helper.onCancelClick(UpgradeDialog.this)) {
                    log("cancel click consumed by son class.");
                } else {
                    dealCancel();
                }
            } else {
                dealCancel();
            }
        }
    }

    private void dealCancel() {
        try {
            dismiss();
        } catch (Exception e) {
            //...ignore...
        }
    }

    private DownloadHandler handler;
    private String saveDir;
    private String saveName;

    private void dealSubmit() {
        mBtCancel.setVisibility(View.GONE);
        mBtSubmit.setOnClickListener(null);
        if (null == handler) {
            handler = new DownloadHandler(UpgradeDialog.this, activity.getMainLooper());
        }
        saveDir = getUseAppPath(getContext().getApplicationContext());
        saveName = "upgrade_" + bean.getUpgradeVersionName() + "_" + (System.currentTimeMillis() / 1000) + ".apk";
        String url = bean.getUpgradeDownloadUrl();
        DownloadThread thread = new DownloadThread(handler, url, saveDir, saveName, bean.getUpgradeFileSize());
        th = new WeakReference<>(thread);
        thread.start();
    }

    private class SubmitClickListener extends UpgradeBaseOnClickListener {
        @Override
        public void click(View v) {
            log("dialog update submit");
            if (null != helper) {
                if (helper.onSubmitClick(UpgradeDialog.this)) {
                    log("submit click consumed by son class.");
                } else {
                    dealSubmit();
                }
            } else {
                dealSubmit();
            }
        }
    }

    private String getUseAppPath(Context context) {
        if (null == context) return null;
        String path = null;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                path = context.getExternalCacheDir().getAbsolutePath() + "/" + "app";
            } else {
                path = context.getCacheDir().getAbsolutePath() + "/" + "app";
            }
            File file = new File(path);
            if (!file.exists()) {
                boolean mkdirs = file.mkdirs();
                log("create download dir result: " + mkdirs);
            }
        } catch (Exception e) {
            if (UpgradeBuildInfo.DEBUG_MODE) {
                Log.w(TAG, "get save path error!", e);
            }
        }
        return path;
    }

    private void showToast(int resId) {
        if (isCanUseHelper()) {
            helper.showToast(resId);
        }
    }

    private static class DownloadHandler extends UpgradeWeakHandler<UpgradeDialog> {

        static final int DOWNLOAD_START = 0x01;
        static final int DOWNLOAD_ERROR = 0x02;
        static final int DOWNLOAD_ING = 0x03;
        static final int DOWNLOAD_END = 0x04;

        private DownloadHandler(UpgradeDialog upgradeDialog, Looper looper) {
            super(upgradeDialog, looper);
        }

        @Override
        public void weakMessage(Message msg, UpgradeDialog dialog) {
            switch (msg.what) {
                case DOWNLOAD_START:
                    dialog.downloadStart();
                    break;
                case DOWNLOAD_ERROR:
                    dialog.downloadError();
                    break;
                case DOWNLOAD_ING:
                    try {
                        String p = String.valueOf(msg.obj);
                        int progress = Integer.parseInt(p);
                        dialog.downloadIng(progress);
                    } catch (Exception e) {
                        //...ignore...
                    }
                    break;
                case DOWNLOAD_END:
                    dialog.downloadEnd(String.valueOf(msg.obj));
                    break;
            }
        }
    }

    private void downloadStart() {
        try {
            if (null != mBtSubmit) {
                mBtSubmit.setText(R.string.ug_download_start);
                mBtSubmit.setEnabled(false);
            }
        } catch (Exception e) {
            //..ignore...
        }
    }

    private void downloadError() {
        try {
            if (null != mBtSubmit) {
                mBtSubmit.setText(R.string.ug_download_start);
                mBtSubmit.setEnabled(true);
                mBtSubmit.setOnClickListener(new SubmitClickListener());
            }
            showToast(R.string.ug_download_failure);
        } catch (Exception e) {
            //..ignore...
        }
    }

    private void downloadIng(int progress) {
        try {
            log("progress: " + progress);
            if (null != mBtSubmit) {
                mBtSubmit.setText(String.format(Locale.ENGLISH, "%d%%", progress));
            }
        } catch (Exception e) {
            //...ignore...
        }
    }

    /**
     * @param md5Cal 下载的apk文件计算出来的MD5
     */
    private void downloadEnd(String md5Cal) {
        try {
            File file = new File(saveDir + "/" + saveName);
            if (null != mBtSubmit) {
                mBtSubmit.setText(R.string.ug_download_start);
                mBtSubmit.setEnabled(true);
                mBtSubmit.setOnClickListener(new SubmitClickListener());
            }
            String md5Info = bean.getUpgradeApkMd5();
            log("download apk file md5: " + md5Cal);
            log("upgrade info file md5: " + md5Info);
            if (!TextUtils.isEmpty(md5Info) && !TextUtils.isEmpty(md5Cal)) {
                if (md5Cal.equals(md5Info)) {
                    install(file);
                } else {
                    showToast(R.string.ug_apk_verify_failure);
                }
            } else {
                install(file);
            }
            if (!isForce) {
                dismiss();
            }
        } catch (Exception e) {
            showToast(R.string.ug_apk_install_failure);
            if (UpgradeBuildInfo.DEBUG_MODE) {
                Log.w("install apk error", e);
            }
        }
    }

    @SuppressLint({"SetWorldReadable", "SetWorldWritable"})
    private void install(File file) {
        if (file.exists()) {
            try {
                boolean x = file.setExecutable(true, false);
                boolean r = file.setReadable(true, false);
                boolean w = file.setWritable(true, false);
                log("set x: " + x);
                log("set r: " + r);
                log("set w: " + w);
            } catch (Exception e) {
                Log.e("upgrade", "set permission error.", e);
            }
            Context app = activity.getApplicationContext();
            if (Build.VERSION.SDK_INT >= 24) {
                String auth = fileProvider;
                log("file provider auth is: " + auth);
                Uri uri = FileProvider.getUriForFile(app, auth, file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                getContext().startActivity(intent);
            } else {
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                getContext().startActivity(install);
            }
            clearThread();
        } else {
            log("where is your download apk file?");
        }
    }

    private boolean isCanUseHelper() {
        if (null == helper) {
            Log.w(TAG, "helper is null, you need check!");
            return false;
        }
        return true;
    }

    private static class DownloadThread extends Thread {

        private DownloadHandler handler;
        private String downUrl;
        private String saveDir;
        private String saveName;
        private long size;
        private volatile boolean isGiveUp;

        /**
         * @param size 这里是apk总的字节大小，要特别注意！
         */
        DownloadThread(DownloadHandler handler, String downUrl, String saveDir, String saveName, long size) {
            this.handler = handler;
            this.downUrl = downUrl;
            this.saveDir = saveDir;
            this.saveName = saveName;
            this.size = size;
        }

        private void close() {
            isGiveUp = true;
        }

        private static void log(String msg) {
            if (UpgradeBuildInfo.DEBUG_MODE) {
                Log.d("DownloadThread", msg);
            }
        }

        @Override
        public void run() {
            handler.sendEmptyMessage(DownloadHandler.DOWNLOAD_START);

            log("download url: " + downUrl);
            log("save dir: " + saveDir);
            if (TextUtils.isEmpty(downUrl) || TextUtils.isEmpty(saveDir)) {
                handler.sendEmptyMessage(DownloadHandler.DOWNLOAD_ERROR);
                return;
            }

            File dir = new File(saveDir);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
                log("create download dir result: " + mkdirs);
            }

            String nameRelease = saveName;
            String nameTemp = nameRelease + ".temp";
            log("save apk name: " + nameRelease);
            log("save apk temp name: " + nameTemp);

            String pathRelease = saveDir + "/" + nameRelease;
            String pathTemp = saveDir + "/" + nameTemp;
            log("path release: " + pathRelease);
            log("path temp: " + pathTemp);

            File f1 = new File(pathRelease);
            if (f1.exists()) {
                boolean result = f1.delete();
                log(nameRelease + " delete,result: " + result);
            }

            try {
                URL url = new URL(downUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                conn.setConnectTimeout(30000);
                long length;
                if (size < 0) {
                    length = conn.getContentLength();
                } else {
                    length = size;
                }
                log("use update info apk size: " + length);
                InputStream is = conn.getInputStream();

                File fileTemp = new File(pathTemp);
                FileOutputStream fos = new FileOutputStream(fileTemp);

                DecimalFormat df = new DecimalFormat("0.00");
                String apkFileSize = df.format((float) length / 1024 / 1024) + "MB";
                log("download app size: " + apkFileSize);

                long count = 0;
                byte buf[] = new byte[1024];
                int lastProgress = 0;
                do {
                    int numRead = is.read(buf);
                    count += numRead;

                    //tmpFileSize = df.format((float) count / 1024 / 1024) + "MB";
                    int progress = (int) (((float) count / length) * 100);
                    if (progress - lastProgress >= 1) {
                        log("progress: " + progress + " count: " + count + " length: " + length);
                        if (progress > 100) {
                            progress = 100;
                        }
                        Message message = Message.obtain();
                        message.obj = String.valueOf(progress);
                        message.what = DownloadHandler.DOWNLOAD_ING;
                        handler.sendMessage(message);
                        lastProgress = progress;
                    }

                    if (numRead <= 0) {
                        if (fileTemp.renameTo(f1)) {
                            log("rename file success");
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                //...ignore...
                            }
                            String md5Cal = "";
                            try {
                                md5Cal = getFileMD5(new File(pathRelease));
                            } catch (Exception e) {
                                //...ignore...
                            }
                            Message msg = Message.obtain();
                            msg.obj = md5Cal;
                            msg.what = DownloadHandler.DOWNLOAD_END;
                            handler.sendMessage(msg);
                        } else {
                            handler.sendEmptyMessage(DownloadHandler.DOWNLOAD_ERROR);
                        }
                        break;
                    }
                    fos.write(buf, 0, numRead);
                } while (!isGiveUp);

                try {
                    fos.flush();
                } catch (Exception e) {
                    //...ignore...
                }
                try {
                    fos.close();
                } catch (Exception e) {
                    //...ignore
                }
                try {
                    is.close();
                } catch (Exception e) {
                    //...ignore
                }
            } catch (Exception e) {
                if (UpgradeBuildInfo.DEBUG_MODE) {
                    Log.w(TAG, "download thread run error.", e);
                }
                handler.sendEmptyMessage(DownloadHandler.DOWNLOAD_ERROR);
            }
        }
    }

    private static String getFileMD5(File file) {
        if (null == file) return null;
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public interface Helper {

        void showToast(int resId);

        void exitApp();

        /**
         * 取消按键事件
         *
         * @param dialog UpgradeDialog
         * @return true代表由子类消费此次点击事件
         */
        boolean onCancelClick(UpgradeDialog dialog);

        /**
         * 立即更新按键事件
         *
         * @param dialog UpgradeDialog
         * @return true代表由子类消费此次点击事件
         */
        boolean onSubmitClick(UpgradeDialog dialog);
    }

}
