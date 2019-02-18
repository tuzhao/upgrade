package org.tuzhao.upgrade.bean;

public interface UpgradeInfoBean {

    /**
     * 获取升级提示框顶部的title
     */
    String getUpgradeTitle();

    /**
     * 获取升级提示框里面的升级描述
     */
    String getUpgradeDescription();

    /**
     * 获取新版本apk下载的链接
     */
    String getUpgradeDownloadUrl();

    /**
     * 获取新版本apk的version name
     */
    String getUpgradeVersionName();

    /**
     * 获取新版本apk文件的大小 单位必须为字节 byte
     * 当这个方法的返回为-1那么就认为不需要使用这个方法返回的文件大小值，直接使用http IO返回的文件大小即可。
     */
    long getUpgradeFileSize();

    /**
     * 获取file provider的名称,主要是用来安装apk使用
     */
    String getUpgradeFileProvider();

    /**
     * 是否是强制更新
     *
     * @return true 当前的升级框为强制更新 false 当前的升级框为非强制更新
     */
    boolean isForceUpgrade();

    /**
     * 获取下载下来后apk的md5值
     * 如果返回为空那么就不需要进行MD5相关检查匹配
     */
    String getUpgradeApkMd5();

}
