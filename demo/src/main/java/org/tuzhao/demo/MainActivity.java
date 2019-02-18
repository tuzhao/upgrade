package org.tuzhao.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.tuzhao.upgrade.BuildInfo;
import org.tuzhao.upgrade.bean.UpgradeInfoBean;
import org.tuzhao.upgrade.dialog.UpgradeDialog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public Activity getActivity() {
        return this;
    }

    public void checkUpdate(View view) {
        BuildInfo.setDebugMode(true);

        UpgradeInfoBean info = new UpgradeInfoBean() {

            @Override
            public String getUpgradeTitle() {
                return "实简FTP v1.2.01";
            }

            @Override
            public String getUpgradeDescription() {
                return "1.fix在部分手机上权限检查不通过的bug；\n2.增加android 9兼容性适配；\n3.其它细节优化；";
            }

            @Override
            public String getUpgradeDownloadUrl() {
                return "https://rsftp.cn/static/rsftp_v1.2.01_normal.apk";
            }

            @Override
            public String getUpgradeVersionName() {
                return "1.2.01";
            }

            @Override
            public long getUpgradeFileSize() {
                return -1;
            }

            @Override
            public String getUpgradeFileProvider() {
                return getActivity().getPackageName() + ".fileProvider";
            }

            @Override
            public boolean isForceUpgrade() {
                return false;
            }

            @Override
            public String getUpgradeApkMd5() {
                return "be5cc8e459f9decdbc80176bfd792959";
            }
        };

        UpgradeDialog.Helper helper = new UpgradeDialog.Helper() {

            @Override
            public void showToast(int resId) {
                Toast.makeText(MainActivity.this, resId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void exitApp() {

            }
        };

        boolean show = UpgradeDialog.show(this, info, helper);
        Log.d("upgrade", "show upgrade dialog result: " + show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UpgradeDialog.close();
    }
}
