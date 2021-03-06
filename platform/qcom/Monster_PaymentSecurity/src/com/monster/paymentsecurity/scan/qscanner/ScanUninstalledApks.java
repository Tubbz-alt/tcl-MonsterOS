package com.monster.paymentsecurity.scan.qscanner;

import android.content.Context;

import com.monster.paymentsecurity.scan.Result;
import com.monster.paymentsecurity.util.SettingUtil;
import com.monster.paymentsecurity.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tmsdk.common.module.qscanner.QScanConstants;
import tmsdk.common.module.qscanner.QScanResultEntity;
import tmsdk.fg.module.qscanner.QScanListenerV2;

/**
 * Created by logic on 16-12-5.
 */
public class ScanUninstalledApks extends QScannerTask {

    public ScanUninstalledApks(Context context) {
        super(context);
    }

    @Override
    protected List<QScanResultEntity> onStart() {
        Context context = weakContext.get();
        boolean isCloudScan;
        isCloudScan = context != null && Utils.isWifiOk(context) && SettingUtil.isScanCloudEnable(context);
        result.setErrCode(SCAN_ERRCODE_DEFAULT);
        //noinspection unchecked
        mQScannerMananger.scanUninstalledApks(new ScanUninstalledApks.MyQScanListener(result), isCloudScan);
        //noinspection unchecked
        return (List<QScanResultEntity>) result.getRawData();
    }

    @Override
    protected int getScanType() {
        return SCAN_TYPE_QSCANER_UNINSTALLED_APKS;
    }

    private static final class MyQScanListener extends QScanListenerV2 {
        private final Result taskResult;
        private final List<QScanResultEntity> entities;

        private MyQScanListener(Result<QScanResultEntity> result){
            this.taskResult = result;
            this.entities = new ArrayList<>();
        }

        @Override
        public void onScanStarted(int scanType) {}

        /**
         * 搜索到不扫描的文件的回调
         */
        @Override
        public void onFoundElseFile(int i, File file) {
            taskResult.setErrCode(SCAN_ERRCODE_QSCANNER_APK_FILE_NOT_FOUND);
            //noinspection unchecked
            taskResult.setRawData(file);
        }

        /**
         * 云扫描出现网络错误
         *
         * @param scanType 扫描类型，具体参考{@link QScanConstants#SCAN_INSTALLEDPKGS} ~
         *                 {@link QScanConstants#SCAN_SPECIALS}
         * @param errCode  错误码
         */
        @Override
        public void onScanError(int scanType, int errCode) {
            taskResult.setErrCode(SCAN_ERRCODE_QSCANNER_SCAN_ERROR);
        }

        public void onScanProgress(int i, int i1, QScanResultEntity qScanResultEntity){
            entities.add(qScanResultEntity);
        }

        /**
         * 扫描结束
         *
         * @param scanType 扫描类型，具体参考{@link QScanConstants#SCAN_INSTALLEDPKGS} ~
         *                 {@link QScanConstants#SCAN_SPECIALS}
         * @param qScanresults  扫描的所有结果
         */
        @Override
        public void onScanFinished(int scanType, List<QScanResultEntity> qScanresults) {
            //noinspection unchecked
            taskResult.setRawData(entities);
        }
    }
}
