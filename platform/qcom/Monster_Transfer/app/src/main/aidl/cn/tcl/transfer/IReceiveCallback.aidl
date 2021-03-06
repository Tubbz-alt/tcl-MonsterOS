/*Copyright (C) 2016 Tcl Corporation Limited */
package cn.tcl.transfer;


interface IReceiveCallback {
    void onConnected();
    void onStart(int type);
    void onProgress(int type, long size, long speed);
    void onFileBeginRecv(int type, String fileName);
    void onComplete(int type);
    void onError(int type, String reason);
    void onAllComplete();
    void onCancel();
    void onReceiveDataInfo(String info);
    void onReceiveDataSize(String info);
}

