/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.app.Activity;
import android.app.KeyguardManager;
import mst.app.dialog.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.UpdateLock;
import android.os.UserManager;
import mst.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager; // MODIFIED by xijun.zhang, 2016-08-25,BUG-2796640
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.common.CallLogAsync;
import com.android.phone.settings.SettingsConstants;
import com.android.server.sip.SipService;
import com.android.services.telephony.activation.SimActivationManager;
import com.android.services.telephony.sip.SipUtil;

//[SOLUTION]-Add-BEGIN by TCTNB.(JiangLong Pan), 08/12/2016, SOLUTION-2476106
//Transfer call menu not available
import android.util.TctLog;

import com.android.internal.telephony.CallStateException;
//[SOLUTION]-Add-END by TCTNB.(JiangLong Pan)

//[FEATURE]-Add-BEGIN by TCTNB.Dong.Jiang,08/18/2016,Task-2655811, porting from Defect-667720
//Pause setup data call for 30s after airplane mode closed.
import com.android.internal.telephony.dataconnection.DcTracker;
//[FEATURE]-Add-END by TCTNB.Dong.Jiang

import android.content.SharedPreferences;
import com.mst.test.*;
import android.telecom.TelecomManager;
import android.telecom.PhoneAccountHandle;
import java.util.List;

/**
 * Global state for the telephony subsystem when running in the primary
 * phone process.
 */
public class PhoneGlobals extends ContextWrapper {
    public static final String LOG_TAG = "PhoneApp";

    /**
     * Phone app-wide debug level:
     *   0 - no debug logging
     *   1 - normal debug logging if ro.debuggable is set (which is true in
     *       "eng" and "userdebug" builds but not "user" builds)
     *   2 - ultra-verbose debug logging
     *
     * Most individual classes in the phone app have a local DBG constant,
     * typically set to
     *   (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1)
     * or else
     *   (PhoneApp.DBG_LEVEL >= 2)
     * depending on the desired verbosity.
     *
     * ***** DO NOT SUBMIT WITH DBG_LEVEL > 0 *************
     */
    public static final int DBG_LEVEL = 3;

    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);
    private static final String PROPERTY_AIRPLANE_MODE_ON = "persist.radio.airplane_mode_on";

    // Message codes; see mHandler below.
    private static final int EVENT_SIM_NETWORK_LOCKED = 3;
    private static final int EVENT_SIM_STATE_CHANGED = 8;
    private static final int EVENT_DATA_ROAMING_DISCONNECTED = 10;
    private static final int EVENT_DATA_ROAMING_OK = 11;
    private static final int EVENT_UNSOL_CDMA_INFO_RECORD = 12;
    private static final int EVENT_RESTART_SIP = 13;
    private static final int EVENT_UPDATE_STATS = 500; // MODIFIED by bo.chen, 2016-09-27,BUG-3000255

    // The MMI codes are also used by the InCallScreen.
    public static final int MMI_INITIATE = 51;
    public static final int MMI_COMPLETE = 52;
    public static final int MMI_CANCEL = 53;
    // Don't use message codes larger than 99 here; those are reserved for
    // the individual Activities of the Phone UI.
    private Phone[] mPhones = null; // MODIFIED by bo.chen, 2016-09-27,BUG-3000255
    public static final int AIRPLANE_ON = 1;
    public static final int AIRPLANE_OFF = 0;

    /**
     * Allowable values for the wake lock code.
     *   SLEEP means the device can be put to sleep.
     *   PARTIAL means wake the processor, but we display can be kept off.
     *   FULL means wake both the processor and the display.
     */
    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    private static PhoneGlobals sMe;

    // A few important fields we expose to the rest of the package
    // directly (rather than thru set/get methods) for efficiency.
    CallController callController;
    CallManager mCM;
    CallNotifier notifier;
    CallerInfoCache callerInfoCache;
    NotificationMgr notificationMgr;
    public PhoneInterfaceManager phoneMgr;
    public SimActivationManager simActivationManager;
    CarrierConfigLoader configLoader;

    private CallGatewayManager callGatewayManager;
    private Phone phoneInEcm;

    static boolean sVoiceCapable = true;

    // TODO: Remove, no longer used.
    CdmaPhoneCallState cdmaPhoneCallState;

    // The currently-active PUK entry activity and progress dialog.
    // Normally, these are the Emergency Dialer and the subsequent
    // progress dialog.  null if there is are no such objects in
    // the foreground.
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;

    private boolean mDataDisconnectedDueToRoaming = false;

    private WakeState mWakeState = WakeState.SLEEP;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mPartialWakeLock;
    private KeyguardManager mKeyguardManager;

    private UpdateLock mUpdateLock;
    private boolean mIsStopCount = false; // MODIFIED by bo.chen, 2016-09-27,BUG-3000255

    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new PhoneAppBroadcastReceiver();

    /**
     * The singleton OtaUtils instance used for OTASP calls.
     *
     * The OtaUtils instance is created lazily the first time we need to
     * make an OTASP call, regardless of whether it's an interactive or
     * non-interactive OTASP call.
     */
    public OtaUtils otaUtils;

    // Following are the CDMA OTA information Objects used during OTA Call.
    // cdmaOtaProvisionData object store static OTA information that needs
    // to be maintained even during Slider open/close scenarios.
    // cdmaOtaConfigData object stores configuration info to control visiblity
    // of each OTA Screens.
    // cdmaOtaScreenState object store OTA Screen State information.
    public OtaUtils.CdmaOtaProvisionData cdmaOtaProvisionData;
    public OtaUtils.CdmaOtaConfigData cdmaOtaConfigData;
    public OtaUtils.CdmaOtaScreenState cdmaOtaScreenState;
    public OtaUtils.CdmaOtaInCallScreenUiState cdmaOtaInCallScreenUiState;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PhoneConstants.State phoneState;
            switch (msg.what) {
                // TODO: This event should be handled by the lock screen, just
                // like the "SIM missing" and "Sim locked" cases (bug 1804111).
                case EVENT_SIM_NETWORK_LOCKED:
                    if (getCarrierConfig().getBoolean(
                            CarrierConfigManager.KEY_IGNORE_SIM_NETWORK_LOCKED_EVENTS_BOOL)) {
                        // Some products don't have the concept of a "SIM network lock"
                        Log.i(LOG_TAG, "Ignoring EVENT_SIM_NETWORK_LOCKED event; "
                              + "not showing 'SIM network unlock' PIN entry screen");
                    } else {
                        // Normal case: show the "SIM network unlock" PIN entry screen.
                        // The user won't be able to do anything else until
                        // they enter a valid SIM network PIN.
                        int subType = (Integer)((AsyncResult)msg.obj).result;
                        Log.i(LOG_TAG, "show sim depersonal panel");
                        IccNetworkDepersonalizationPanel.showDialog(subType);
                    }
                    break;

                case EVENT_DATA_ROAMING_DISCONNECTED:
                    notificationMgr.showDataDisconnectedRoaming();
                    break;

                case EVENT_DATA_ROAMING_OK:
                    notificationMgr.hideDataDisconnectedRoaming();
                    break;

                case MMI_COMPLETE:
                    onMMIComplete((AsyncResult) msg.obj);
                    break;

                case MMI_CANCEL:
                    PhoneUtils.cancelMmiCode(mCM.getFgPhone());
                    break;

                case EVENT_SIM_STATE_CHANGED:
                    // Marks the event where the SIM goes into ready state.
                    // Right now, this is only used for the PUK-unlocking
                    // process.
                    if (msg.obj.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                        // when the right event is triggered and there
                        // are UI objects in the foreground, we close
                        // them to display the lock panel.
                        if (mPUKEntryActivity != null) {
                            mPUKEntryActivity.finish();
                            mPUKEntryActivity = null;
                        }
                        if (mPUKEntryProgressDialog != null) {
                            mPUKEntryProgressDialog.dismiss();
                            mPUKEntryProgressDialog = null;
                        }
                    }
                    break;

                case EVENT_UNSOL_CDMA_INFO_RECORD:
                    //TODO: handle message here;
                    break;
                case EVENT_RESTART_SIP:
                    // This should only run if the Phone process crashed and was restarted. We do
                    // not want this running if the device is still in the FBE encrypted state.
                    // This is the same procedure that is triggered in the SipBroadcastReceiver
                    // upon BOOT_COMPLETED.
                    UserManager userManager = UserManager.get(sMe);
                    if (userManager != null && userManager.isUserUnlocked()) {
                        SipUtil.startSipService();
                    }
                    break;
                /* MODIFIED-BEGIN by bo.chen, 2016-09-27,BUG-3000255*/
                case EVENT_UPDATE_STATS:
                    getPowerOnTime();
                    if(!mIsStopCount) {
                       sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                      }
                     break;
                     /* MODIFIED-END by bo.chen,BUG-3000255*/
            }
        }
    };

    public PhoneGlobals(Context context) {
        super(context);
        sMe = this;
    }

    public void onCreate() {
        if (VDBG) Log.v(LOG_TAG, "onCreate()...");

        ContentResolver resolver = getContentResolver();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("test", false);
        editor.commit();

        // Cache the "voice capable" flag.
        // This flag currently comes from a resource (which is
        // overrideable on a per-product basis):
        sVoiceCapable =
                getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        // ...but this might eventually become a PackageManager "system
        // feature" instead, in which case we'd do something like:
        // sVoiceCapable =
        //   getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_VOICE_CALLS);

        if (mCM == null) {
            // Initialize the telephony framework
            PhoneFactory.makeDefaultPhones(this);

            /* MODIFIED-BEGIN by bo.chen, 2016-09-27,BUG-3000255*/
            int numPhones = TelephonyManager.getDefault().getPhoneCount();
            // Start TelephonyDebugService After the default phone is created.
            Intent intent = new Intent(this, TelephonyDebugService.class);
            startService(intent);

            mPhones = PhoneFactory.getPhones();
            /* MODIFIED-END by bo.chen,BUG-3000255*/
            mCM = CallManager.getInstance();
            for (Phone phone : PhoneFactory.getPhones()) {
                mCM.registerPhone(phone);
            }

            // Create the NotificationMgr singleton, which is used to display
            // status bar icons and control other status bar behavior.
            notificationMgr = NotificationMgr.init(this);

            // If PhoneGlobals has crashed and is being restarted, then restart.
            mHandler.sendEmptyMessage(EVENT_RESTART_SIP);
            /* MODIFIED-BEGIN by bo.chen, 2016-09-27,BUG-3000255*/
            if (getResources().getBoolean( R.bool.feature_phone_show_search_network_switch_on)){
                mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
                getPowerOnTime();
            }
            /* MODIFIED-END by bo.chen,BUG-3000255*/

            // Create an instance of CdmaPhoneCallState and initialize it to IDLE
            cdmaPhoneCallState = new CdmaPhoneCallState();
            cdmaPhoneCallState.CdmaPhoneCallStateInit();

            // before registering for phone state changes
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, LOG_TAG);
            // lock used to keep the processor awake, when we don't care for the display.
            mPartialWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);

            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

            // Get UpdateLock to suppress system-update related events (e.g. dialog show-up)
            // during phone calls.
            mUpdateLock = new UpdateLock("phone");

            if (DBG) Log.d(LOG_TAG, "onCreate: mUpdateLock: " + mUpdateLock);

            CallLogger callLogger = new CallLogger(this, new CallLogAsync());

            callGatewayManager = CallGatewayManager.getInstance();

            // Create the CallController singleton, which is the interface
            // to the telephony layer for user-initiated telephony functionality
            // (like making outgoing calls.)
            callController = CallController.init(this, callLogger, callGatewayManager);

            // Create the CallerInfoCache singleton, which remembers custom ring tone and
            // send-to-voicemail settings.
            //
            // The asynchronous caching will start just after this call.
            callerInfoCache = CallerInfoCache.init(this);

            phoneMgr = PhoneInterfaceManager.init(this, PhoneFactory.getDefaultPhone());

            configLoader = CarrierConfigLoader.init(this);

            // Create the CallNotifer singleton, which handles
            // asynchronous events from the telephony layer (like
            // launching the incoming-call UI when an incoming call comes
            // in.)
            notifier = CallNotifier.init(this);

            PhoneUtils.registerIccStatus(mHandler, EVENT_SIM_NETWORK_LOCKED);

            // register for MMI/USSD
            mCM.registerForMmiComplete(mHandler, MMI_COMPLETE, null);

            // register connection tracking to PhoneUtils
            PhoneUtils.initializeConnectionHandler(mCM);

            // Register for misc other intent broadcasts.
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
            registerReceiver(mReceiver, intentFilter);
            registerReceiver(mShutdownReceiver, new IntentFilter("begin.Shutdown.Sequence")); //[BUGFIX]-Mod-BEGIN by TCTNB.yubin.ying,09/01/2016,Solution-2828651,

            //[FEATURE]-Add-BEGIN by TCTNB.Xijun.Zhang,09/05/2016,Task-2830651,
            //Dual SIM logic development
//            if (getResources()
//                    .getBoolean(com.android.internal.R.bool.feature_tctfw_dds_auto_switch)) {
          if (getResources()
                  .getBoolean(R.bool.feature_tctfw_dds_auto_switch)) {
                IntentFilter ddsFilter = new IntentFilter(
                        TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
                registerReceiver(mDDSSwitchReceiver, ddsFilter);
            }
            //[FEATURE]-Add-END by TCTNB.Xijun.Zhang,09/05/2016,Task-2830651

            //set the default values for the preferences in the phone.
            PreferenceManager.setDefaultValues(this, R.xml.network_setting, false);

            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);

            // Make sure the audio mode (along with some
            // audio-mode-related state of our own) is initialized
            // correctly, given the current state of the phone.
            PhoneUtils.setAudioMode(mCM);
            //[FEATURE]-Add-BEGIN by TCTNB.Xijun.Zhang,09/06/2016,Task-2830651,
            //Dual SIM logic development
            Intent mIntent = new Intent("android.phone.PHONE_CREATE");
            sendBroadcast(mIntent);
            //[FEATURE]-Add-END by TCTNB.Xijun.Zhang,09/06/2016,Task-2830651
        }

        cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
        cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
        cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
        cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();

        simActivationManager = new SimActivationManager();

        // XXX pre-load the SimProvider so that it's ready
        resolver.getType(Uri.parse("content://icc/adn"));

        // TODO: Register for Cdma Information Records
        // phone.registerCdmaInformationRecord(mHandler, EVENT_UNSOL_CDMA_INFO_RECORD, null);

        // Read HAC settings and configure audio hardware
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            int hac = android.provider.Settings.System.getInt(
                    getContentResolver(),
                    android.provider.Settings.System.HEARING_AID,
                    0);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameter(SettingsConstants.HAC_KEY,
                    hac == SettingsConstants.HAC_ENABLED
                            ? SettingsConstants.HAC_VAL_ON : SettingsConstants.HAC_VAL_OFF);
        }

//[SOLUTION]-Add-BEGIN by TCTNB.(JiangLong Pan), 08/12/2016, SOLUTION-2476106
//Transfer call menu not available
        initExplicitCallTransferBroadcast();
//[SOLUTION]-Add-END by TCTNB.(JiangLong Pan)
        initMst();
    }

    //[BUGFIX]-Mod-BEGIN by TCTNB.yubin.ying,09/01/2016,Solution-2828651,
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
           String action = intent.getAction();
           if (action.equals("begin.Shutdown.Sequence")) {
               if (getPhone().getState() != PhoneConstants.State.IDLE) {
                   Log.i("PhoneGlobals", "receive shutdown broadcastReceiver, hangup all calls ");
                   PhoneUtils.hangupAllCalls(mCM);
                }
            }
        }
    };
    //[BUGFIX]-Mod-END by TCTNB.yubin.ying

    /**
     * Returns the singleton instance of the PhoneApp.
     */
    public static PhoneGlobals getInstance() {
        if (sMe == null) {
            throw new IllegalStateException("No PhoneGlobals here!");
        }
        return sMe;
    }

    /**
     * Returns the singleton instance of the PhoneApp if running as the
     * primary user, otherwise null.
     */
    static PhoneGlobals getInstanceIfPrimary() {
        return sMe;
    }

    /**
     * Returns the default phone.
     *
     * WARNING: This method should be used carefully, now that there may be multiple phones.
     */
    public static Phone getPhone() {
        return PhoneFactory.getDefaultPhone();
    }

    public static Phone getPhone(int subId) {
        return PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
    }

    /* package */ CallManager getCallManager() {
        return mCM;
    }

    public PersistableBundle getCarrierConfig() {
        return getCarrierConfigForSubId(SubscriptionManager.getDefaultSubscriptionId());
    }

    public PersistableBundle getCarrierConfigForSubId(int subId) {
        return configLoader.getConfigForSubId(subId);
    }

    /**
     * Handles OTASP-related events from the telephony layer.
     *
     * While an OTASP call is active, the CallNotifier forwards
     * OTASP-related telephony events to this method.
     */
    void handleOtaspEvent(Message msg) {
        if (DBG) Log.d(LOG_TAG, "handleOtaspEvent(message " + msg + ")...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaEvents: got an event but otaUtils is null! "
                  + "message = " + msg);
            return;
        }

        otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
    }

    /**
     * Similarly, handle the disconnect event of an OTASP call
     * by forwarding it to the OtaUtils instance.
     */
    /* package */ void handleOtaspDisconnect() {
        if (DBG) Log.d(LOG_TAG, "handleOtaspDisconnect()...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaspDisconnect: otaUtils is null!");
            return;
        }

        otaUtils.onOtaspDisconnect();
    }

    /**
     * Sets the activity responsible for un-PUK-blocking the device
     * so that we may close it when we receive a positive result.
     * mPUKEntryActivity is also used to indicate to the device that
     * we are trying to un-PUK-lock the phone. In other words, iff
     * it is NOT null, then we are trying to unlock and waiting for
     * the SIM to move to READY state.
     *
     * @param activity is the activity to close when PUK has
     * finished unlocking. Can be set to null to indicate the unlock
     * or SIM READYing process is over.
     */
    void setPukEntryActivity(Activity activity) {
        mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return mPUKEntryActivity;
    }

    /**
     * Sets the dialog responsible for notifying the user of un-PUK-
     * blocking - SIM READYing progress, so that we may dismiss it
     * when we receive a positive result.
     *
     * @param dialog indicates the progress dialog informing the user
     * of the state of the device.  Dismissed upon completion of
     * READYing process
     */
    void setPukEntryProgressDialog(ProgressDialog dialog) {
        mPUKEntryProgressDialog = dialog;
    }

    /**
     * Controls whether or not the screen is allowed to sleep.
     *
     * Once sleep is allowed (WakeState is SLEEP), it will rely on the
     * settings for the poke lock to determine when to timeout and let
     * the device sleep {@link PhoneGlobals#setScreenTimeout}.
     *
     * @param ws tells the device to how to wake.
     */
    /* package */ void requestWakeState(WakeState ws) {
        if (VDBG) Log.d(LOG_TAG, "requestWakeState(" + ws + ")...");
        synchronized (this) {
            if (mWakeState != ws) {
                switch (ws) {
                    case PARTIAL:
                        // acquire the processor wake lock, and release the FULL
                        // lock if it is being held.
                        mPartialWakeLock.acquire();
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                    case FULL:
                        // acquire the full wake lock, and release the PARTIAL
                        // lock if it is being held.
                        mWakeLock.acquire();
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                    case SLEEP:
                    default:
                        // release both the PARTIAL and FULL locks.
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                }
                mWakeState = ws;
            }
        }
    }

    /**
     * If we are not currently keeping the screen on, then poke the power
     * manager to wake up the screen for the user activity timeout duration.
     */
    /* package */ void wakeUpScreen() {
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP) {
                if (DBG) Log.d(LOG_TAG, "pulse screen lock");
                mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.phone:WAKE");
            }
        }
    }

    /**
     * Sets the wake state and screen timeout based on the current state
     * of the phone, and the current state of the in-call UI.
     *
     * This method is a "UI Policy" wrapper around
     * {@link PhoneGlobals#requestWakeState} and {@link PhoneGlobals#setScreenTimeout}.
     *
     * It's safe to call this method regardless of the state of the Phone
     * (e.g. whether or not it's idle), and regardless of the state of the
     * Phone UI (e.g. whether or not the InCallScreen is active.)
     */
    /* package */ void updateWakeState() {
        PhoneConstants.State state = mCM.getState();

        // True if the speakerphone is in use.  (If so, we *always* use
        // the default timeout.  Since the user is obviously not holding
        // the phone up to his/her face, we don't need to worry about
        // false touches, and thus don't need to turn the screen off so
        // aggressively.)
        // Note that we need to make a fresh call to this method any
        // time the speaker state changes.  (That happens in
        // PhoneUtils.turnOnSpeaker().)
        boolean isSpeakerInUse = (state == PhoneConstants.State.OFFHOOK) && PhoneUtils.isSpeakerOn(this);

        // TODO (bug 1440854): The screen timeout *might* also need to
        // depend on the bluetooth state, but this isn't as clear-cut as
        // the speaker state (since while using BT it's common for the
        // user to put the phone straight into a pocket, in which case the
        // timeout should probably still be short.)

        // Decide whether to force the screen on or not.
        //
        // Force the screen to be on if the phone is ringing or dialing,
        // or if we're displaying the "Call ended" UI for a connection in
        // the "disconnected" state.
        // However, if the phone is disconnected while the user is in the
        // middle of selecting a quick response message, we should not force
        // the screen to be on.
        //
        boolean isRinging = (state == PhoneConstants.State.RINGING);
        boolean isDialing = (mCM.getFgPhone().getForegroundCall().getState() == Call.State.DIALING);
        boolean keepScreenOn = isRinging || isDialing;
        // keepScreenOn == true means we'll hold a full wake lock:
        requestWakeState(keepScreenOn ? WakeState.FULL : WakeState.SLEEP);
    }

    KeyguardManager getKeyguardManager() {
        return mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult r) {
        if (VDBG) Log.d(LOG_TAG, "onMMIComplete()...");
        MmiCode mmiCode = (MmiCode) r.result;
        PhoneUtils.displayMMIComplete(mmiCode.getPhone(), getInstance(), mmiCode, null, null);
    }

    private void initForNewRadioTechnology(int phoneId) {
        if (DBG) Log.d(LOG_TAG, "initForNewRadioTechnology...");

        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || !TelephonyCapabilities.supportsOtasp(phone)) {
            // Clean up OTA for non-CDMA since it is only valid for CDMA.
            clearOtaState();
        }

        notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
    }

    private void handleAirplaneModeChange(int newMode) {
        if (newMode == AIRPLANE_ON) {
            // If we are trying to turn off the radio, make sure there are no active
            // emergency calls.  If there are, switch airplane mode back to off.
            if (PhoneUtils.isInEmergencyCall(mCM)) {
                // Switch airplane mode back to off.
                SystemProperties.set(PROPERTY_AIRPLANE_MODE_ON, "0");
                ConnectivityManager.from(this).setAirplaneMode(false);
                Toast.makeText(this, R.string.radio_off_during_emergency_call, Toast.LENGTH_LONG)
                        .show();
                Log.i(LOG_TAG, "Ignoring airplane mode: emergency call. Turning airplane off");
            } else {
                Log.i(LOG_TAG, "Turning radio off - airplane");

                Log.d(LOG_TAG, "Setting property " + PROPERTY_AIRPLANE_MODE_ON);
                SystemProperties.set(PROPERTY_AIRPLANE_MODE_ON, "1");
                PhoneUtils.setRadioPower(false);
            }
        } else {
            Log.i(LOG_TAG, "Turning radio on - airplane");
            SystemProperties.set(PROPERTY_AIRPLANE_MODE_ON, "0");
            PhoneUtils.setRadioPower(true);
        }
    }

    /**
     * Receiver for misc intent broadcasts the Phone app cares about.
     */
    private class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                int airplaneMode = Settings.Global.getInt(getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, AIRPLANE_OFF);
                // Treat any non-OFF values as ON.
                if (airplaneMode != AIRPLANE_OFF) {
                    airplaneMode = AIRPLANE_ON;
                }
                handleAirplaneModeChange(airplaneMode);
                //[FEATURE]-Add-BEGIN by TCTNB.Dong.Jiang,08/18/2016,Task-2655811, porting from Defect-667720
                //Pause setup data call for 30s after airplane mode closed.
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, 0);
                Phone phone = PhoneFactory.getPhone(phoneId);
                if ((airplaneMode == AIRPLANE_OFF) && (phone != null)) {
                    DcTracker dcTracker;
                    Log.d(LOG_TAG, "mReceiver: AIRPLANE_OFF");
                    if(phone.mDcTracker != null && phone.mDcTracker instanceof DcTracker) {
                        dcTracker = (DcTracker)phone.mDcTracker;
                        dcTracker.mAirplaneOff = true;
                    }
                }
                //[FEATURE]-Add-END by TCTNB.Dong.Jiang

            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED) ||
                    action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                int phoneId = SubscriptionManager.getPhoneId(subId);
                String state = intent.getStringExtra(PhoneConstants.STATE_KEY);
                if (VDBG) {
                    Log.d(LOG_TAG, "mReceiver: " + action);
                    Log.d(LOG_TAG, "- state: " + state);
                    Log.d(LOG_TAG, "- reason: "
                    + intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY));
                    Log.d(LOG_TAG, "- subId: " + subId);
                    Log.d(LOG_TAG, "- phoneId: " + phoneId);
                }
                Phone phone = SubscriptionManager.isValidPhoneId(phoneId) ?
                        PhoneFactory.getPhone(phoneId) : PhoneFactory.getDefaultPhone();

                // The "data disconnected due to roaming" notification is shown
                // if (a) you have the "data roaming" feature turned off, and
                // (b) you just lost data connectivity because you're roaming.

//[SOLUTION]-Add-BEGIN by TCTNB.(JiangLong Pan), 08/12/2016, SOLUTION-2504537
//[GPR][RU][NB PreTest][Roaming]Data roaming warning about mobile data closed should not disappear until mobile data restored
                //[BUGFIX]-Del-BEGIN by TCTNB.Danbin.Xu,11/04/2016,Defect-3276220,
                //Data roaming warning is not shown on device when entering in VPLMN area
                /*
                boolean disconnectedDueToRoaming;
                if(context.getResources().getBoolean(R.bool.feature_phone_russia_show_roaming_notification)){
                    disconnectedDueToRoaming =
                            !phone.getDataRoamingEnabled()
                            && PhoneConstants.DataState.DISCONNECTED.equals(state)
                            && phone.getServiceState().getRoaming();
                } else {
                    disconnectedDueToRoaming =
                            !phone.getDataRoamingEnabled()
                            && PhoneConstants.DataState.DISCONNECTED.equals(state)
                            && Phone.REASON_ROAMING_ON.equals(
                                intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY));
                }
                */
                //[BUGFIX]-Del-END by TCTNB.Danbin.Xu
//[SOLUTION]-Add-END by TCTNB.(JiangLong Pan)
                // (b) your registered to roaming network and
                // (c) you just lost data connectivity because you're roaming
                //       OR
                // (d) DDS was changed to a SIM card where (a) and (b) are true
                boolean disconnectReasonRoaming =
                        PhoneConstants.DataState.DISCONNECTED.name().equals(state)
                        && Phone.REASON_ROAMING_ON.equals(
                            intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY));
                Phone ddsPhone = getPhone(SubscriptionManager.getDefaultDataSubscriptionId());
                if (ddsPhone == null) ddsPhone = getPhone();
                boolean isRoaming = ddsPhone.getServiceState().getDataRoaming();
                boolean isRoamingDataEnabled = ddsPhone.getDataRoamingEnabled();
                boolean isDdsSwitch = action.equals(
                        TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);

                boolean disconnectedDueToRoaming = mDataDisconnectedDueToRoaming;
                if ((disconnectReasonRoaming || isDdsSwitch)
                        && !isRoamingDataEnabled && isRoaming) {
                    disconnectedDueToRoaming = true;
                } else if (!isRoaming || isRoamingDataEnabled) {
                    // Dismiss pop up only if phone is not roaming or dataonroaming is enabled
                    disconnectedDueToRoaming = false;
                }

                if (VDBG) Log.d(LOG_TAG, "isRoaming = " + isRoaming + " isRoamingDataEnabled = "
                        + isRoamingDataEnabled + "disconnectReasonRoaming = "
                        + disconnectReasonRoaming + " mDataDisconnectedDueToRoaming = "
                        + mDataDisconnectedDueToRoaming);

                if (mDataDisconnectedDueToRoaming != disconnectedDueToRoaming) {
                    Log.d(LOG_TAG, "disconnectedDueToRoaming = " + disconnectedDueToRoaming);
                    mDataDisconnectedDueToRoaming = disconnectedDueToRoaming;
                    mHandler.sendEmptyMessage(disconnectedDueToRoaming
                            ? EVENT_DATA_ROAMING_DISCONNECTED : EVENT_DATA_ROAMING_OK);
                }
            } else if ((action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) &&
                    (mPUKEntryActivity != null)) {
                // if an attempt to un-PUK-lock the device was made, while we're
                // receiving this state change notification, notify the handler.
                // NOTE: This is ONLY triggered if an attempt to un-PUK-lock has
                // been attempted.
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SIM_STATE_CHANGED,
                        intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE)));
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                String newPhone = intent.getStringExtra(PhoneConstants.PHONE_NAME_KEY);
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                        SubscriptionManager.INVALID_PHONE_INDEX);
                Log.d(LOG_TAG, "Radio technology switched. Now " + newPhone + " (" + phoneId
                        + ") is active.");
                initForNewRadioTechnology(phoneId);
            } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                handleServiceStateChanged(intent);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, 0);
                phoneInEcm = PhoneFactory.getPhone(phoneId);
                Log.d(LOG_TAG, "Emergency Callback Mode. phoneId:" + phoneId);
                if (phoneInEcm != null) {
                    if (TelephonyCapabilities.supportsEcm(phoneInEcm)) {
                        Log.d(LOG_TAG, "Emergency Callback Mode arrived in PhoneApp.");
                        // Start Emergency Callback Mode service
                        if (intent.getBooleanExtra("phoneinECMState", false)) {
                            context.startService(new Intent(context,
                                    EmergencyCallbackModeService.class));
                        } else {
                            phoneInEcm = null;
                        }
                    } else {
                        // It doesn't make sense to get ACTION_EMERGENCY_CALLBACK_MODE_CHANGED
                        // on a device that doesn't support ECM in the first place.
                        Log.e(LOG_TAG, "Got ACTION_EMERGENCY_CALLBACK_MODE_CHANGED, but "
                                + "ECM isn't supported for phone: " + phoneInEcm.getPhoneName());
                        phoneInEcm = null;
                    }
                } else {
                    Log.w(LOG_TAG, "phoneInEcm is null.");
                }
            }
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        /**
         * This used to handle updating EriTextWidgetProvider this routine
         * and and listening for ACTION_SERVICE_STATE_CHANGED intents could
         * be removed. But leaving just in case it might be needed in the near
         * future.
         */

        // If service just returned, start sending out the queued messages
        Bundle extras = intent.getExtras();
        if (extras != null) {
            ServiceState ss = ServiceState.newFromBundle(extras);
            if (ss != null) {
                int state = ss.getState();
                notificationMgr.updateNetworkSelection(state);
            }
        }
    }

    // it is safe to call clearOtaState() even if the InCallScreen isn't active
    public void clearOtaState() {
        if (DBG) Log.d(LOG_TAG, "- clearOtaState ...");
        if (otaUtils != null) {
            otaUtils.cleanOtaScreen(true);
            if (DBG) Log.d(LOG_TAG, "  - clearOtaState clears OTA screen");
        }
    }

    // it is safe to call dismissOtaDialogs() even if the InCallScreen isn't active
    public void dismissOtaDialogs() {
        if (DBG) Log.d(LOG_TAG, "- dismissOtaDialogs ...");
        if (otaUtils != null) {
            otaUtils.dismissAllOtaDialogs();
            if (DBG) Log.d(LOG_TAG, "  - dismissOtaDialogs clears OTA dialogs");
        }
    }

    public Phone getPhoneInEcm() {
        return phoneInEcm;
    }

    /**
     * Triggers a refresh of the message waiting (voicemail) indicator.
     *
     * @param subId the subscription id we should refresh the notification for.
     */
    public void refreshMwiIndicator(int subId) {
        notificationMgr.refreshMwi(subId);
    }

    /**
     * Dismisses the message waiting (voicemail) indicator.
     *
     * @param subId the subscription id we should dismiss the notification for.
     */
    public void clearMwiIndicator(int subId) {
        notificationMgr.updateMwi(subId, false);
    }

//[SOLUTION]-Add-BEGIN by TCTNB.(JiangLong Pan), 08/12/2016, SOLUTION-2476106
//Transfer call menu not available
    private final BroadcastReceiver mExplicitCallTransferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if ("com.tct.telephony.explicitCall".equals(action)) {
                explicitCallTransferFromPhoneGlobal();
            }
        }
    };
    private void initExplicitCallTransferBroadcast(){
        IntentFilter intentFilter = new IntentFilter("com.tct.telephony.explicitCall");
        registerReceiver(mExplicitCallTransferReceiver, intentFilter);
    }
    private void explicitCallTransferFromPhoneGlobal(){
        Call backgroundCall = mCM.getFirstActiveBgCall();
        try {
            TctLog.d(LOG_TAG,"explicitCallTransferFromPhoneGlobal");
            mCM.explicitCallTransfer(backgroundCall);
        } catch (CallStateException e) {
            TctLog.d(LOG_TAG, "CallStateException:", e);
        }
    }
//[SOLUTION]-Add-END by TCTNB.(JiangLong Pan)

    //[FEATURE]-Add-BEGIN by TCTNB.Xijun.Zhang,09/05/2016,Task-2830651,
    //Dual SIM logic development
    //Dual sim Default data subscription auto changed
    private BroadcastReceiver mDDSSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
                int currentDDSSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                if (currentDDSSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    TctLog.d(LOG_TAG, "The current DDSubId is INVALID.");
                    return;
                }

                int simCount = TelephonyManager.getDefault().getSimCount();
//                int dataStateSDM = context.getResources().getInteger(
//                            com.android.internal.R.integer.def_tctfw_data_service_state_after_dds);
                int dataStateSDM = context.getResources().getInteger(R.integer.def_tctfw_data_service_state_after_dds);
                Log.i(LOG_TAG, "show sim depersonal panel dataStateSDM = " + dataStateSDM);
                boolean dataEnabled = false;
                if (TelephonyManager.getDefault().isMultiSimEnabled() && simCount > 1) {
                    if (dataStateSDM == 2) {
                        dataEnabled = (Settings.System.getInt(getContentResolver(),
                                Settings.System.TCT_DUALSIM_CELLULAR_DATA_ENABLE, 1) > 0);
                    } else if (dataStateSDM == 1) {
                        dataEnabled = true;
                    } else {
                        dataEnabled = false;
                    }
                    for (Phone phone : PhoneFactory.getPhones()) {
                        int subId = phone.getSubId();
                        //[SOLUTION]-Add-BEGIN by TCTNB.(Chuanjun Chen), 09/02/2016, SOLUTION- 2473826 And TASk-2781430
                        //[ForTest][FDN] Mobile data connection and USSD should be disabled when FDN is enabled
                        int fdnFlag = 0;
                        if (subId == -1) {
                            fdnFlag = Settings.System.getInt(getContentResolver(), "FDN_DATA_CONNECTION_DISABLE_FLAG", -1);
                        } else {
                            fdnFlag = Settings.System.getInt(getContentResolver(), "FDN_DATA_CONNECTION_DISABLE_FLAG" + subId, -1);
                        }
                        int phoneId = SubscriptionManager.getPhoneId(subId);
                        boolean isFdnEnable = TelephonyManager.getDefault().getIccFdnEnabled(phoneId);
                        if (subId == currentDDSSubId && !(isFdnEnable && fdnFlag != -1)) {
                        //[SOLUTION]-Add-END by TCTNB.(Chuanjun Chen)
                            phone.setDataEnabled(dataEnabled);
                        } else {
                            phone.setDataEnabled(false);
                        }
                    }
                }
            }
        }
    };
    //[FEATURE]-Add-END by TCTNB.Xijun.Zhang,09/05/2016,Task-2830651
    /* MODIFIED-BEGIN by bo.chen, 2016-09-27,BUG-3000255*/
    private void getPowerOnTime(){
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }
        convert(ut);
        android.util.Log.e(LOG_TAG, "update Time:" + convert(ut));
    }
    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));
        android.util.Log.e(LOG_TAG, "mPhones.length:" + mPhones.length);
        for (int i = 0; i < mPhones.length; i++) {
            android.util.Log.e(LOG_TAG, "mPhones.ServiceState:" + mPhones[i].getServiceState().getState()); // MODIFIED by qili.zhang, 2016-09-04,BUG-2845388
            if((m >= 3 && mPhones[i].getServiceState().getState() == ServiceState.STATE_OUT_OF_SERVICE)){
                mIsStopCount = true;
                if(mPhones[i].getServiceState().getIsManualSelection()){
                    android.util.Log.e(LOG_TAG, "open the automic resigster the nw:");
                    mPhones[i].setNetworkSelectionModeAutomatic(null);
                }
            }else if(mPhones[i].getServiceState().getState() == ServiceState.STATE_IN_SERVICE){
                android.util.Log.e(LOG_TAG, "stop count,powner on time is more than 5:");
                mIsStopCount = true;
            /* MODIFIED-BEGIN by qili.zhang, 2016-09-04,BUG-2845388*/
            }else if(SystemProperties.get(PROPERTY_AIRPLANE_MODE_ON).equals("1")){
                android.util.Log.e(LOG_TAG, "PROPERTY_AIRPLANE_MODE_ON is true");
                mIsStopCount = true;
                /* MODIFIED-END by qili.zhang,BUG-2845388*/
            }
        }
        return h + ":" + pad(m) + ":" + pad(s);
    }
    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }
    /* MODIFIED-END by bo.chen,BUG-3000255*/

    static PhoneAccountHandle getPhoneAccountHandle(Context context, int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        String subId = String.valueOf(PhoneFactory.getPhone(phoneId).getSubId());
        TelecomManager telecomManager = TelecomManager.from(context);
        List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle account : accounts) {
            if (subId.equals(account.getId())) {
                return account;
            }
        }
        return null;
    }

    private ManageTest mManageTest;
    private void initMst() {
    	mManageTest = new ManageTest(this);    	    	
    	SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
    	subscriptionManager.setDefaultVoiceSubId(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    	//add by lgy for  3406149
    	Settings.System.putInt(getContentResolver(),
                Settings.System.TCT_TURN_OVER_TO_MUTE, 0);

    }
    
}
