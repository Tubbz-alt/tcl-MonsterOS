/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


//import android.app.AlertDialog;
import mst.app.dialog.AlertDialog;
import mst.app.dialog.ProgressDialog;
import android.app.Dialog; // MODIFIED by caihong.gu-nb, 2016-04-20,BUG-1963058
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
/* MODIFIED-BEGIN by lina.tang, 2016-08-12,BUG-2268661*/
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
/* MODIFIED-END by lina.tang,BUG-2268661*/
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.print.PrintHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
/* MODIFIED-BEGIN by dongliang.feng, 2016-03-22,BUG-1850791 */
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.ImageCacheService;
/* MODIFIED-END by dongliang.feng,BUG-1850791 */
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.category.Action;
import com.android.gallery3d.filtershow.category.CategoryAdapter;
import com.android.gallery3d.filtershow.category.CategorySelected;
import com.android.gallery3d.filtershow.category.CategoryUtil;
import com.android.gallery3d.filtershow.category.MainPanel;
import com.android.gallery3d.filtershow.data.UserPresetsManager;
import com.android.gallery3d.filtershow.editors.BasicEditor;
import com.android.gallery3d.filtershow.editors.Editor;
import com.android.gallery3d.filtershow.editors.EditorChanSat;
import com.android.gallery3d.filtershow.editors.EditorColorBorder;
import com.android.gallery3d.filtershow.editors.EditorCrop;
import com.android.gallery3d.filtershow.editors.EditorDraw;
import com.android.gallery3d.filtershow.editors.EditorGrad;
import com.android.gallery3d.filtershow.editors.EditorManager;
import com.android.gallery3d.filtershow.editors.EditorMirror;
import com.android.gallery3d.filtershow.editors.EditorPanel;
import com.android.gallery3d.filtershow.editors.EditorRedEye;
import com.android.gallery3d.filtershow.editors.EditorRotate;
import com.android.gallery3d.filtershow.editors.EditorStraighten;
import com.android.gallery3d.filtershow.editors.EditorTinyPlanet;
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterChanSatRepresentation;
import com.android.gallery3d.filtershow.filters.FilterColorBorderRepresentation;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterDirectRepresentation;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.filters.ImageFilterWBalance;
import com.android.gallery3d.filtershow.history.HistoryItem;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.imageshow.Spline;
import com.android.gallery3d.filtershow.info.InfoPanel;
import com.android.gallery3d.filtershow.pipeline.CachingPipeline;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.ProcessingService;
import com.android.gallery3d.filtershow.presets.PresetManagementDialog;
import com.android.gallery3d.filtershow.presets.UserPresetsAdapter;
import com.android.gallery3d.filtershow.provider.SharedImageProvider;
import com.android.gallery3d.filtershow.state.StateAdapter;
import com.android.gallery3d.filtershow.tools.AutoEditHelper;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.android.gallery3d.filtershow.tools.XmpPresets;
import com.android.gallery3d.filtershow.tools.XmpPresets.XMresults;
import com.android.gallery3d.filtershow.ui.ExportDialog;
import com.android.gallery3d.filtershow.ui.FilterShowSaveDialog;
import com.android.gallery3d.filtershow.ui.FramedTextButton;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.LogUtil;
import com.android.gallery3d.util.PermissionUtil;
import com.android.gallery3d.util.ScreenManager;
import com.android.photos.data.GalleryBitmapPool;

public class FilterShowActivity extends FragmentActivity implements OnItemClickListener,
        OnShareTargetSelectedListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener, PopupMenu.OnDismissListener, View.OnClickListener {
    
    public static final String TAG = "FilterShowActivity";
    //modify begin by liaoanhua
    public static final int mWholeCorpId = -1;
    private boolean mServicesLoadFinish = false;
    //modify end
    private String mAction = "";
    MasterImage mMasterImage = null;

    private static final long LIMIT_SUPPORTS_HIGHRES = 134217728; // 128Mb

    public static final String TINY_PLANET_ACTION = "com.android.camera.action.TINY_PLANET";
    public static final String LAUNCH_FULLSCREEN = "launch-fullscreen";
    public static final boolean RESET_TO_LOADED = false;
    // [ALM][BUGFIX]-Add by TCTNJ,jian.pan1, 2016-03-05,Defect:1533434 begin
    public static final int MAX_VERSION_COUNT = 5;
    // [ALM][BUGFIX]-Add by TCTNJ,jian.pan1, 2016-03-05,Defect:1533434 end
    private ImageShow mImageShow = null;

    // private View mBackButton = null;
    private MenuItem mSaveButton = null;
    // private MenuItem mAutoEditButton = null;
    // private MenuItem mDrawEditButton = null;
    // private MenuItem mDrawUButton = null;
    // private ImageView mFiltershowBack;

    private EditorPlaceHolder mEditorPlaceHolder = new EditorPlaceHolder(this);
    private Editor mCurrentEditor = null;

    private static final int SELECT_PICTURE = 1;
    private static final String LOGTAG = "FilterShowActivity";

    private boolean mShowingTinyPlanet = true;
    private boolean mShowingImageStatePanel = false;
    private boolean mShowingVersionsPanel = false;

    private final Vector<ImageShow> mImageViews = new Vector<ImageShow>();

    private ShareActionProvider mShareActionProvider;
    private File mSharedOutputFile = null;

    private boolean mSharingImage = false;

    private WeakReference<ProgressDialog> mSavingProgressDialog;

    private LoadBitmapTask mLoadBitmapTask;

    private Uri mOriginalImageUri = null;
    private ImagePreset mOriginalPreset = null;

    private Uri mSelectedImageUri = null;

    private ArrayList<Action> mActions = new ArrayList<Action>();
    private UserPresetsManager mUserPresetsManager = null;
    private UserPresetsAdapter mUserPresetsAdapter = null;

    // TCL ShenQianfeng Begin on 2016.08.19
    private CategoryAdapter mCategoryCropAdapter = null;
    private CategoryAdapter mCategoryRotateAdapter = null;
    // TCL ShenQianfeng End on 2016.08.19
    private CategoryAdapter mCategoryLooksAdapter = null;
    // TCL ShenQianfeng Begin on 2016.09.01
    // Annotated Below:
    /*
    private CategoryAdapter mCategoryBordersAdapter = null;
    private CategoryAdapter mCategoryGeometryAdapter = null;
    */
    // TCL ShenQianfeng End on 2016.09.01
    private CategoryAdapter mCategoryFiltersAdapter = null;
    private CategoryAdapter mCategoryVersionsAdapter = null;
    // TCL ShenQianfeng Begin on 2016.08.22
    // Original:
    //private int mCurrentPanel = MainPanel.LOOKS;
    // Modify To:
    private int mCurrentPanel = MainPanel.CROP;
    // TCL ShenQianfeng End on 2016.08.22
    
    private Vector<FilterUserPresetRepresentation> mVersions =
            new Vector<FilterUserPresetRepresentation>();
    private int mVersionsCounter = 0;
    private FilterDrawRepresentation mDrawRep = null;

    private boolean mHandlingSwipeButton = false;
    private View mHandledSwipeView = null;
    private float mHandledSwipeViewLastDelta = 0;
    private float mSwipeStartX = 0;
    private float mSwipeStartY = 0;

    private ProcessingService mBoundService;
    private boolean mIsBound = false;
    private Menu mMenu;
    private DialogInterface mCurrentDialog = null;
    private PopupMenu mCurrentMenu = null;
    private boolean mLoadingVisible = true;
//    private boolean mIsOnDrawMode = false;
    
    // TCL ShenQianfeng Begin on 2016.08.23
    private TopBarManager mTopBarManager;
    // TCL ShenQianfeng End on 2016.08.23
    
    

    private boolean mHasAutoEditAdd = false;
    private AutoEditHelper mAutoEditHelper = null;
    /* MODIFIED-BEGIN by wei.song, 2016-05-11,BUG-2114749*/
    private boolean isAutoPressed = true;
    private MenuItem autoeditorItem;
    /* MODIFIED-END by wei.song,BUG-2114749*/
    private Dialog mDialog; // MODIFIED by caihong.gu-nb, 2016-04-20,BUG-1963058
    //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 begin
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case FilterShowSaveDialog.MSG_SAVE_COPY:
                saveAndCopy();
                break;
            case FilterShowSaveDialog.MSG_SAVE_OVERWRITE:
                saveImage();
                break;
            case FilterShowSaveDialog.MSG_SAVE_DONE:
                Uri saveUri = (Uri) msg.obj;
                if (saveUri != null) {
                    /* MODIFIED-BEGIN by dongliang.feng, 2016-03-22,BUG-1850791 */
                    if (mSelectedImageUri.equals(saveUri)) {
                        long id = ImageLoader.getIdFromUri(getBaseContext(), mSelectedImageUri);
                        if (id != -1) {
                            ImageCacheService cache = ((GalleryApp)getApplication()).getImageCacheService();
                            cache.removeBitmapRamCache(id);
                        }
                    }
                    /* MODIFIED-END by dongliang.feng,BUG-1850791 */
                    setResult(RESULT_OK, new Intent().setData(saveUri));
                }
                hideSavingProgress();
                finish();
                break;
            default:
                break;
            }
        }
    };
    
    // TCL ShenQianfeng Begin on 2016.08.29
    public TopBarManager getTopBarManager() {
        return mTopBarManager;
    }
    // TCL ShenQianfeng End on 2016.08.29

    /* MODIFIED-BEGIN by lina.tang, 2016-08-12,BUG-2268661*/
    private static final class UpdateScannerClient implements MediaScannerConnectionClient {
        MediaScannerConnection mScannerConnection;
        String mFile;

        public UpdateScannerClient(Context context, String file) {
            mScannerConnection = new MediaScannerConnection(context, this);
            mFile = file;
            if (mFile == null) return;
            mScannerConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            if(mFile == null) return;
            mScannerConnection.scanFile(mFile, null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (mFile == null) return;
            mScannerConnection.disconnect();
        }
    }
    /* MODIFIED-END by lina.tang,BUG-2268661*/
    /**
     * save and copy picture
     */
    private void saveAndCopy() {
      //[BUGFIX]-Add-BEGIN by TCTNJ.ye.chen, 2015/03/21 PR-956368.
        if(ImageLoader.getLocalPathFromUri(getBaseContext(), mSelectedImageUri) == null) {
            cannotLoadImage();
            return;
        }
      //[BUGFIX]-Add-BEGIN by TCTNJ.ye.chen, 2015/03/21 PR-956368.
        File saveDir = SaveImage.getFinalSaveDirectory(FilterShowActivity.this, mSelectedImageUri);
        int bucketId = GalleryUtils.getBucketId(saveDir.getPath());
        String albumName = LocalAlbum.getLocalizedName(getResources(), bucketId, null);
        showSavingProgress(albumName);
        Uri sourceUri = MasterImage.getImage().getUri();
        File dest = SaveImage.getNewFile(FilterShowActivity.this,  getSelectedImageUri());
        Intent processIntent = ProcessingService.getSaveIntent(FilterShowActivity.this, MasterImage
                .getImage().getPreset(), dest, getSelectedImageUri(), sourceUri,
                true, 100, 1f, true);
        startService(processIntent);
        saveExtraWork();
    }
    //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 end

    private void saveExtraWork() {
        // TODO Diagnostis
        int userSelectedLookPos = mCategoryLooksAdapter.getUserSelectedPos();
        
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        //int userSelectedBorderPos = mCategoryBordersAdapter.getUserSelectedPos();
        // TCL ShenQianfeng End on 2016.09.01
        
//        int userSelectedGeometryPos = mCategoryGeometryAdapter.getUserSelectedPos();
//        int userSelectedFilterPos = mCategoryFiltersAdapter.getUserSelectedPos();
        Map<String, String> map = new HashMap<String, String>();
        // Amount of times the user saved a photo after editing
        map.put("COUNTSAVEDAFTEREDITING", "1");
        if (userSelectedLookPos >= 0) {
            // Amount of times user saved a photo edit with a filter applied
            map.put("COUNTSAVEDWITHFILTER", "1");
            // Amount of times the user saved a photo edit with filter 0~9,including preset filter
            if (userSelectedLookPos <= 9) {
                map.put("COUNTSAVEDWITHFILTER" + userSelectedLookPos, "1");
            }
        }
        
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        /*
        if (userSelectedBorderPos >= 0) {
            // Amount of times user saved a photo edit with a frame applied
            map.put("COUNTSAVEDWITHFRAME", "1");
            // Amount of times the user saved a photo edit with frame 0~11 applied
            if (userSelectedBorderPos <= 11) {
                map.put("COUNTSAVEDWITHFRAME" + userSelectedBorderPos, "1");
            }
        }
        */
        // TCL ShenQianfeng End on 2016.09.01
        
        // Amount of times user saved a photo edit with autoeffect applied
        if (!isAutoPressed) {
            map.put("COUNTSAVEDWITHAUTOEFFECT", "1");
        }
    }

    public ProcessingService getProcessingService() {
        return mBoundService;
    }

    public boolean isSimpleEditAction() {
        return !PhotoPage.ACTION_NEXTGEN_EDIT.equalsIgnoreCase(mAction);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            /*
             * This is called when the connection with the service has been
             * established, giving us the service object we can use to
             * interact with the service.  Because we have bound to a explicit
             * service that we know is running in our own process, we can
             * cast its IBinder to a concrete class and directly access it.
             */
            ///[BUGFIX]-ADD-BEGIN BY TSNJ.LIUDEKUAN ON 2016/01/06 FOR PR1192698
            boolean checkResult = PermissionUtil.checkPermissions(FilterShowActivity.this, AbstractGalleryActivity.class.getName());
            if (!checkResult) {
                Log.d("FilterShowActivity", "error: checkPermissions failed");
                FilterShowActivity.this.finish();
                return;
            }
            ///[BUGFIX]-ADD-END BY TSNJ.LIUDEKUAN
            mBoundService = ((ProcessingService.LocalBinder)service).getService();
            mBoundService.setFiltershowActivity(FilterShowActivity.this);
            mBoundService.onStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            /*
             * This is called when the connection with the service has been
             * unexpectedly disconnected -- that is, its process crashed.
             * Because it is running in our same process, we should never
             * see this happen.
             */
            mBoundService = null;
        }
    };

    void doBindService() {
        /*
         * Establish a connection with the service.  We use an explicit
         * class name because we want a specific service implementation that
         * we know will be running in our own process (and thus won't be
         * supporting component replacement by other applications).
         */
        bindService(new Intent(FilterShowActivity.this, ProcessingService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void updateUIAfterServiceStarted() {
        MasterImage.setMaster(mMasterImage);
        ImageFilter.setActivityForMemoryToasts(this);
        mUserPresetsManager = new UserPresetsManager(this);
        mUserPresetsAdapter = new UserPresetsAdapter(this);

        setupMasterImage();
        //setupMenu();
        setDefaultValues();
        fillEditors();
        //getWindow().setBackgroundDrawable(new ColorDrawable(0));
        loadXML();

        fillCategories();
        loadMainPanel();
//        extractXMPData(); //MODIFIED by jian.pan1, 2016-04-16,BUG-1940322
        processIntent();
        //modify by liaoah
        mServicesLoadFinish = true;
        //modify end
    }
    
    // TCL ShenQianfeng Begin on 2016.08.29
    public void hideNavigationBar() {
        final View decorView = getWindow().getDecorView();
        int uiFlags = decorView.getSystemUiVisibility();
        uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        decorView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) == 0) {
                    visibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                    decorView.setSystemUiVisibility(visibility);
                }
            }
        });
        decorView.setSystemUiVisibility(uiFlags);
       }
    // TCL ShenQianfeng End on 2016.08.29

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // TCL ShenQianfeng Begin on 2016.08.29
        /*
        hideNavigationBar();
        
        if (Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.KITKAT) {
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        */
        
        // TCL ShenQianfeng End on 2016.08.29
        
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 begin
        ScreenManager.getScreenManager().resetCurrentActivity(this);
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 end
        // TCL BaiYuan Begin on 2016.10.18
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(winParams);
        // TCL BaiYuan End on 2016.10.18
        
        boolean onlyUsePortrait = getResources().getBoolean(R.bool.only_use_portrait);
        if (onlyUsePortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        clearGalleryBitmapPool();
        doBindService();
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));//[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666
        setContentView(R.layout.filtershow_splashscreen);
        //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 begin
        if(ApiHelper.CAN_SET_STATUS_BAR_COLOR) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
        // TCL ShenQianfeng Begin on 2016.08.23
        // Annotated Below:
        /*
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        */
        // TCL ShenQianfeng End on 2016.08.23

        //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 end
    }

    public boolean isShowingImageStatePanel() {
        return mShowingImageStatePanel;
    }

    public void loadMainPanel() {
        if (findViewById(R.id.main_panel_container) == null) {
            return;
        }
        MainPanel panel = new MainPanel();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_panel_container, panel, MainPanel.FRAGMENT_TAG);
        transaction.commitAllowingStateLoss();
    }

    public void loadEditorPanel(FilterRepresentation representation,
                                final Editor currentEditor) {
        if (representation.getEditorId() == ImageOnlyEditor.ID) {
            currentEditor.reflectCurrentFilter();
            return;
        }
        final int currentId = currentEditor.getID();
        Runnable showEditor = new Runnable() {
            @Override
            public void run() {
                EditorPanel panel = new EditorPanel();
                panel.setEditor(currentId);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG));
                transaction.replace(R.id.main_panel_container, panel, MainPanel.FRAGMENT_TAG);
                transaction.commit();
            }
        };
        Fragment main = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        boolean doAnimation = false;
        if (mShowingImageStatePanel
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            doAnimation = true;
        }
        if (doAnimation && main != null && main instanceof MainPanel) {
            MainPanel mainPanel = (MainPanel) main;
            View container = mainPanel.getView().findViewById(R.id.category_panel_container);
            View bottom = mainPanel.getView().findViewById(R.id.bottom_panel);
            int panelHeight = container.getHeight() + bottom.getHeight();
            ViewPropertyAnimator anim = mainPanel.getView().animate();
            anim.translationY(panelHeight).start();
            final Handler handler = new Handler();
            handler.postDelayed(showEditor, anim.getDuration());
        } else {
            showEditor.run();
        }
    }

    public void toggleInformationPanel() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
        InfoPanel panel = new InfoPanel();
        panel.show(transaction, InfoPanel.FRAGMENT_TAG);
    }

    private void loadXML() {
        setContentView(R.layout.filtershow_activity);
        // TCL ShenQianfeng Begin on 2016.08.23
        // Annotated Below:
        /*
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        actionBar.setCustomView(LayoutInflater.from(this).inflate(R.layout.custom_view, null));
        mBackButton = actionBar.getCustomView();
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterShowActivity.this.onBackPressed();
            }
        });
        */
        // TCL ShenQianfeng End on 2016.08.23
        
        // TCL ShenQianfeng Begin on 2016.08.23
        View topBarRootView = findViewById(R.id.custom_top_bar);
        mTopBarManager = new TopBarManager(topBarRootView);
        mTopBarManager.setOnClickListener(this);
        //mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        //modify by liaoah begin
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_APPLY);
        //modify end
        mTopBarManager.enableSaveButton(false);
        // TCL ShenQianfeng End on 2016.08.23
        
        mAutoEditHelper = new AutoEditHelper();
        mImageShow = (ImageShow) findViewById(R.id.imageShow);
        mImageViews.add(mImageShow);

        setupEditors();

        mEditorPlaceHolder.hide();
        mImageShow.attach();

        setupStatePanel();
        //[BUGFIX]-Add by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 begin
        if(ApiHelper.CAN_SET_STATUS_BAR_COLOR) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
        //[BUGFIX]-Add by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 end
    }

    public void fillCategories() {
        // TCL ShenQianfeng Begin on 2016.08.22
        fillCrops();
        fillRotate();
        // TCL ShenQianfeng End on 2016.08.22
        fillLooks();
        loadUserPresets();
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        /*
        fillBorders();
        fillTools();
        */
        // TCL ShenQianfeng End on 2016.09.01
        fillEffects();
        fillVersions();
    }

    public void setupStatePanel() {
        MasterImage.getImage().setHistoryManager(mMasterImage.getHistory());
    }

    private void fillVersions() {
        if (mCategoryVersionsAdapter != null) {
            mCategoryVersionsAdapter.clear();
        }
        mCategoryVersionsAdapter = new CategoryAdapter(this);
        mCategoryVersionsAdapter.setShowAddButton(true);
    }

    public void registerAction(Action action) {
        if (mActions.contains(action)) {
            return;
        }
        mActions.add(action);
    }

    private void loadActions() {
        for (int i = 0; i < mActions.size(); i++) {
            Action action = mActions.get(i);
            action.setImageFrame(new Rect(0, 0, 96, 96), 0);
        }
    }

    public void updateVersions() {
        mCategoryVersionsAdapter.clear();
        FilterUserPresetRepresentation originalRep = new FilterUserPresetRepresentation(
                getString(R.string.filtershow_version_original), new ImagePreset(), -1);
        mCategoryVersionsAdapter.add(
                new Action(this, originalRep, Action.FULL_VIEW));
        ImagePreset current = new ImagePreset(MasterImage.getImage().getPreset());
        FilterUserPresetRepresentation currentRep = new FilterUserPresetRepresentation(
                getString(R.string.filtershow_version_current), current, -1);
        mCategoryVersionsAdapter.add(
                new Action(this, currentRep, Action.FULL_VIEW));
        if (mVersions.size() > 0) {
            mCategoryVersionsAdapter.add(new Action(this, Action.SPACER));
        }
        for (FilterUserPresetRepresentation rep : mVersions) {
            mCategoryVersionsAdapter.add(
                    new Action(this, rep, Action.FULL_VIEW, true));
        }
        mCategoryVersionsAdapter.notifyDataSetInvalidated();
    }

    public void addCurrentVersion() {
        ImagePreset current = new ImagePreset(MasterImage.getImage().getPreset());
        mVersionsCounter++;
        FilterUserPresetRepresentation rep = new FilterUserPresetRepresentation(
                "" + mVersionsCounter, current, -1);
        // [ALM][BUGFIX]-Add by TCTNJ,jian.pan1, 2016-03-05,Defect:1533434 begin
        if (mVersions.size() == MAX_VERSION_COUNT) {
            mVersions.remove(0);
        }
        // [ALM][BUGFIX]-Add by TCTNJ,jian.pan1, 2016-03-05,Defect:1533434 end
        mVersions.add(rep);
        updateVersions();
    }

    public void removeVersion(Action action) {
        mVersions.remove(action.getRepresentation());
        updateVersions();
    }

    public void removeLook(Action action) {
        FilterUserPresetRepresentation rep =
                (FilterUserPresetRepresentation) action.getRepresentation();
        if (rep == null) {
            return;
        }
        mUserPresetsManager.delete(rep.getId());
        updateUserPresetsFromManager();
    }

    private void fillEffects() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getEffects();
        //[BUGFIX]-Add by TCTNJ,jialiang.ren, 2015-02-05,PR910210 begin
        // TCL ShenQianfeng Begin on 2016.09.05
        // Annotated Below:
        /*
        ArrayList<Integer> filtersNormalIconId = filtersManager.getFiltersNormalIconId();
        ArrayList<Integer> filtersSelectedIconId = filtersManager.getFiltersSelectedIconId();
        */
        // TCL ShenQianfeng End on 2016.09.05
        //[BUGFIX]-Add by TCTNJ,jialiang.ren, 2015-02-05,PR910210 end
        if (mCategoryFiltersAdapter != null) {
            mCategoryFiltersAdapter.clear();
        }
        mCategoryFiltersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation representation : filtersRepresentations) {
            /*
            if (representation.getTextId() != 0) {
                representation.setName(getString(representation.getTextId()));
            }
            */
            mCategoryFiltersAdapter.add(new Action(this, representation));
        }
        //[BUGFIX]-Add by TCTNJ,jialiang.ren, 2015-02-05,PR910210 begin
        // TCL ShenQianfeng Begin on 2016.09.05
        // Annotated Below:
        /*
        for (Integer id : filtersNormalIconId) {
            mCategoryFiltersAdapter.addNormalIcon(BitmapFactory.decodeResource(getResources(), id));
        }
        for (Integer id : filtersSelectedIconId) {
            mCategoryFiltersAdapter.addSelectedIcon(BitmapFactory.decodeResource(getResources(), id));
        }
        */
        // TCL ShenQianfeng End on 2016.09.05
        //[BUGFIX]-Add by TCTNJ,jialiang.ren, 2015-02-05,PR910210 end
    }

//    private void fillFaceBeauty() {
//        FiltersManager filtersManager = FiltersManager.getManager();
//        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getFaceBeauty();
//
//        if (mCategoryFaceBeautyAdapter != null) {
//            mCategoryFaceBeautyAdapter.clear();
//        }
//        mCategoryFaceBeautyAdapter = new CategoryAdapter(this);
//        int verticalItemHeight = (int) getResources().getDimension(R.dimen.action_item_height);
//        mCategoryFaceBeautyAdapter.setItemHeight(verticalItemHeight);
//        for (FilterRepresentation representation : filtersRepresentations) {
//            mCategoryFaceBeautyAdapter.add(new Action(this, representation, Action.FULL_VIEW));
//        }
//        if (mUserPresetsManager.getRepresentations() == null
//            || mUserPresetsManager.getRepresentations().size() == 0) {
//            mCategoryFaceBeautyAdapter.add(new Action(this, Action.ADD_ACTION));
//        }
//
//        Fragment panel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
//        if (panel != null) {
//            if (panel instanceof MainPanel) {
//                MainPanel mainPanel = (MainPanel) panel;
//                mainPanel.loadFaceBeautyPanel(true);
//            }
//        }
//    }

    // TCL ShenQianfeng Begin on 2016.09.01
    // Annotated Below:
    /*
    private void fillTools() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getTools();
        if (mCategoryGeometryAdapter != null) {
            mCategoryGeometryAdapter.clear();
        }
        mCategoryGeometryAdapter = new CategoryAdapter(this);
        boolean found = false;
        for (FilterRepresentation representation : filtersRepresentations) {
            mCategoryGeometryAdapter.add(new Action(this, representation));
            if (representation instanceof FilterDrawRepresentation) {
                found = true;
            }
        }
        if (!found) {
            FilterRepresentation representation = new FilterDrawRepresentation();
            Action action = new Action(this, representation);
            action.setIsDoubleAction(true);
            mCategoryGeometryAdapter.add(action);
        }
    }
    */
    // TCL ShenQianfeng End on 2016.09.01

    private void processIntent() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(LAUNCH_FULLSCREEN, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mAction = intent.getAction();
        mSelectedImageUri = intent.getData();
        Uri loadUri = mSelectedImageUri;
        if (mOriginalImageUri != null) {
            loadUri = mOriginalImageUri;
        }
        if (loadUri != null) {
            startLoadBitmap(loadUri);
        } else {
            pickImage();
        }
    }

    private void setupEditors() {
        mEditorPlaceHolder.setContainer((FrameLayout) findViewById(R.id.editorContainer));
        EditorManager.addEditors(mEditorPlaceHolder);
        mEditorPlaceHolder.setOldViews(mImageViews);
    }

    private void fillEditors() {
        mEditorPlaceHolder.addEditor(new EditorChanSat());
        mEditorPlaceHolder.addEditor(new EditorGrad());
        mEditorPlaceHolder.addEditor(new EditorDraw());
        mEditorPlaceHolder.addEditor(new EditorColorBorder());
        mEditorPlaceHolder.addEditor(new BasicEditor());
        mEditorPlaceHolder.addEditor(new ImageOnlyEditor());
        mEditorPlaceHolder.addEditor(new EditorTinyPlanet());
        mEditorPlaceHolder.addEditor(new EditorRedEye());
        mEditorPlaceHolder.addEditor(new EditorCrop());
        mEditorPlaceHolder.addEditor(new EditorMirror());
        mEditorPlaceHolder.addEditor(new EditorRotate());
        mEditorPlaceHolder.addEditor(new EditorStraighten());
    }

    private void setDefaultValues() {
        Resources res = getResources();

        // TODO: get those values from XML.
        FramedTextButton.setTextSize((int) getPixelsFromDip(14));
        FramedTextButton.setTrianglePadding((int) getPixelsFromDip(4));
        FramedTextButton.setTriangleSize((int) getPixelsFromDip(10));

        Drawable curveHandle = res.getDrawable(R.drawable.camera_crop);
        int curveHandleSize = (int) res.getDimension(R.dimen.crop_indicator_size);
        Spline.setCurveHandle(curveHandle, curveHandleSize);
        Spline.setCurveWidth((int) getPixelsFromDip(3));

        mOriginalImageUri = null;
    }

    private void startLoadBitmap(Uri uri) {
        final View imageShow = findViewById(R.id.imageShow);
        imageShow.setVisibility(View.INVISIBLE);
        startLoadingIndicator();
        mShowingTinyPlanet = false;
        mLoadBitmapTask = new LoadBitmapTask();
        mLoadBitmapTask.execute(uri);
    }

    // TCL ShenQianfeng Begin on 2016.09.01
    // Annotated Below:
    /*
    private void fillBorders() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> borders = filtersManager.getBorders();

        for (int i = 0; i < borders.size(); i++) {
            FilterRepresentation filter = borders.get(i);
            filter.setName(getString(R.string.borders));
            if (i == 0) {
                filter.setName(getString(R.string.none));
            }
        }

        if (mCategoryBordersAdapter != null) {
            mCategoryBordersAdapter.clear();
        }
        mCategoryBordersAdapter = new CategoryAdapter(this);
        for (FilterRepresentation representation : borders) {
            // [FEATURE]-Add-BEGIN by NJHR.ye.chen For PR838295 2014.11.12
            int i = borders.indexOf(representation);
            if (representation.getTextId() != 0 && i != 0) {
                representation.setName(getString(representation.getTextId()) + i);
            }
            // [FEATURE]-Add-end by NJHR.ye.chen For PR838295 2014.11.12
            mCategoryBordersAdapter.add(new Action(this, representation, Action.FULL_VIEW));
        }
    }
    */
    // TCL ShenQianfeng End on 2016.09.01
    
    public UserPresetsAdapter getUserPresetsAdapter() {
        return mUserPresetsAdapter;
    }
    
    // TCL ShenQianfeng Begin on 2016.08.19
    public CategoryAdapter getCategoryCropAdapter() {
        return mCategoryCropAdapter;
    }
    
    public CategoryAdapter getCategoryRotateAdapter() {
        return mCategoryRotateAdapter;
    }
    // TCL ShenQianfeng End on 2016.08.19

    public CategoryAdapter getCategoryLooksAdapter() {
        return mCategoryLooksAdapter;
    }

    // TCL ShenQianfeng Begin on 2016.09.01
    // Annotated Below:
    /*
    public CategoryAdapter getCategoryBordersAdapter() {
        return mCategoryBordersAdapter;
    }

    public CategoryAdapter getCategoryGeometryAdapter() {
        return mCategoryGeometryAdapter;
    }
    */
    // TCL ShenQianfeng End on 2016.09.01

//    public CategoryAdapter getCategoryCropEditAdapter() {
//        return mCategoryCropEditAdapter;
//    }
//
//    public CategoryAdapter getCategoryFaceBeautyAdapter() {
//        return mCategoryFaceBeautyAdapter;
//    }

    public CategoryAdapter getCategoryFiltersAdapter() {
        return mCategoryFiltersAdapter;
    }

    public CategoryAdapter getCategoryVersionsAdapter() {
        return mCategoryVersionsAdapter;
    }

    public void removeFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == null) {
            return;
        }
        ImagePreset oldPreset = MasterImage.getImage().getPreset();
        ImagePreset copy = new ImagePreset(oldPreset);
        copy.removeFilter(filterRepresentation);
        MasterImage.getImage().setPreset(copy, copy.getLastRepresentation(), true);
        if (MasterImage.getImage().getCurrentFilterRepresentation() == filterRepresentation) {
            FilterRepresentation lastRepresentation = copy.getLastRepresentation();
            MasterImage.getImage().setCurrentFilterRepresentation(lastRepresentation);
        }
    }

    public void useFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == null) {
            return;
        }
        if (!(filterRepresentation instanceof FilterRotateRepresentation)
            && !(filterRepresentation instanceof FilterMirrorRepresentation)
            && MasterImage.getImage().getCurrentFilterRepresentation() == filterRepresentation) {
            return;
        }
        if (filterRepresentation instanceof FilterUserPresetRepresentation
                || filterRepresentation instanceof FilterRotateRepresentation
                || filterRepresentation instanceof FilterMirrorRepresentation) {
            MasterImage.getImage().onNewLook(filterRepresentation);
        }
        ImagePreset oldPreset = MasterImage.getImage().getPreset();
        if (oldPreset == null) return;
        ImagePreset copy = new ImagePreset(oldPreset);
        FilterRepresentation representation = copy.getRepresentation(filterRepresentation);
        if (representation == null) {
            filterRepresentation = filterRepresentation.copy();
            copy.addFilter(filterRepresentation);
        } else {
            if (filterRepresentation.allowsSingleInstanceOnly()) {
                // Don't just update the filter representation. Centralize the
                // logic in the addFilter(), such that we can keep "None" as
                // null.
                if (!representation.equals(filterRepresentation)) {
                    // Only do this if the filter isn't the same
                    // (state panel clicks can lead us here)
                    // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-06-09,PR1003186 begin
                    if (!(filterRepresentation instanceof FilterColorBorderRepresentation)) {
                        copy.removeFilter(representation);
                        copy.addFilter(filterRepresentation);
                    }
                    // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-06-09,PR1003186 end
                }
            }
        }
        MasterImage.getImage().setPreset(copy, filterRepresentation, true);
        MasterImage.getImage().setCurrentFilterRepresentation(filterRepresentation);
    }

    /**
     * 
     * @param representation
     * @return true indicates representation is applied , false indicates cancel previous same representation.
     */
    public boolean showRepresentation(FilterRepresentation representation) {
        if (representation == null) {
            return false;
        }

        if (representation instanceof FilterRotateRepresentation) {
            FilterRotateRepresentation r = (FilterRotateRepresentation) representation;
            // TCL ShenQianfeng Begin on 2016.08.23
            // Original:
            //r.rotateCW();
            // Modify To:
            r.rotate();
            // TCL ShenQianfeng End on 2016.08.23
        }
        if (representation instanceof FilterMirrorRepresentation) {
            FilterMirrorRepresentation r = (FilterMirrorRepresentation) representation;
            // TCL ShenQianfeng Begin on 2016.08.25
            // Original:
            // r.cycle();
            // Modify To:
            r.transform();
            // TCL ShenQianfeng End on 2016.08.25
            
        }
        if (representation.isBooleanFilter()) {
            ImagePreset preset = MasterImage.getImage().getPreset();
            if (preset.getRepresentation(representation) != null) {
                // remove
                ImagePreset copy = new ImagePreset(preset);
                copy.removeFilter(representation);
                FilterRepresentation filterRepresentation = representation.copy();
                MasterImage.getImage().setPreset(copy, filterRepresentation, true);
                MasterImage.getImage().setCurrentFilterRepresentation(null);
                // TCL ShenQianfeng Begin on 2016.09.01
                if(representation instanceof FilterDirectRepresentation && 
                        representation.getFilterClass().equals(ImageFilterWBalance.class)) {
                        mTopBarManager.enableApplyButton(false);
                        if(getCurrentPanel() == MainPanel.FILTERS) {
                            getCategoryFiltersAdapter().setSelected(0, false);
                        }
                }
                // TCL ShenQianfeng End on 2016.09.01
                return false;
            }
        }
        // TCL ShenQianfeng Begin on 2016.09.01
        mTopBarManager.enableApplyButton(true);
        // TCL ShenQianfeng End on 2016.09.01
        useFilterRepresentation(representation);
        // show representation
        if (mCurrentEditor != null) {
            mCurrentEditor.detach();
        }
        mCurrentEditor = mEditorPlaceHolder.showEditor(representation.getEditorId());
        // TCL ShenQianfeng Begin on 2016.08.22
        if(changeCropAspectWhenEditing(representation)) {
            return true;
        }
        if(changeToColorsEffect(representation)) {
            
        }
        // TCL ShenQianfeng End on 2016.08.22
        loadEditorPanel(representation, mCurrentEditor);
        // TCL ShenQianfeng Begin on 2016.09.05
        if(hasModifications()) {
            mTopBarManager.enableSaveButton(true);
        }
        return true;
        // TCL ShenQianfeng End on 2016.09.05
    }
    
    private synchronized boolean changeCropAspectWhenEditing(FilterRepresentation representation) {
        if(representation.getEditorId() == EditorCrop.ID) {
            EditorCrop cropEditor = ((EditorCrop)mCurrentEditor);
            cropEditor.attach();
            //cropEditor.setUpEditorUI(actionControl, editControl, editTitle, toggleState);
            cropEditor.reflectCurrentFilter();
            mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_APPLY);
            mTopBarManager.enableResetButton(false);
            switch(representation.getTextId()) {
            case R.string.crop_7_5:
                cropEditor.changeCropAspect(R.id.crop_menu_7to5);
                break;
            case R.string.crop_4_3:
                cropEditor.changeCropAspect(R.id.crop_menu_4to3);
                break;
            case R.string.crop_1_1:
                cropEditor.changeCropAspect(R.id.crop_menu_1to1);
                break;
            case R.string.crop_9_16:
                cropEditor.changeCropAspect(R.id.crop_menu_9to16);
                break;
            case R.string.crop_5_7:
                cropEditor.changeCropAspect(R.id.crop_menu_5to7);
                break;
            //modify begin by liaoanhua
            default:
                cropEditor.changeCropAspect(R.id.crop_menu_custom);
                break;
            //modify end
            }
            return true;
        }
        return false;
    }
    
    private boolean changeToColorsEffect(FilterRepresentation representation) {
       if( ! isColorEffects(representation)) {
           return false;
       }
       mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_APPLY);
       if((representation instanceof FilterDirectRepresentation)) {
           //FilterDirectRepresentation directRep = (FilterDirectRepresentation)representation;
           mTopBarManager.enableResetButton(false);
           mTopBarManager.showResetButton(false);
       } else if(representation instanceof FilterBasicRepresentation) {
           FilterBasicRepresentation basicRep = (FilterBasicRepresentation)representation;
           int value = basicRep.getValue();
           mTopBarManager.showResetButton(true);
           mTopBarManager.enableResetButton(value != 0);
       } else if(representation instanceof FilterChanSatRepresentation) {
           FilterChanSatRepresentation chansatRep = (FilterChanSatRepresentation) representation;
           int value = chansatRep.getValue(FilterChanSatRepresentation.MODE_MASTER);
           mTopBarManager.showResetButton(true);
           mTopBarManager.enableResetButton(value != 0);
       }
        return false;
    }
    
    private boolean isColorEffects(FilterRepresentation representation) {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> reps = filtersManager.getEffects();
        for(int i = 0; i< reps.size(); i ++) {
            if(representation.equals(reps.get(i))) {
                return true;
            }
        }
        return false;
    }

    public Editor getEditor(int editorID) {
        return mEditorPlaceHolder.getEditor(editorID);
    }

    public void setCurrentPanel(int currentPanel) {
        mCurrentPanel = currentPanel;
    }

    public int getCurrentPanel() {
        return mCurrentPanel;
    }

    public void updateCategories() {
        if (mMasterImage == null) {
            return;
        }
        ImagePreset preset = mMasterImage.getPreset();
        mCategoryLooksAdapter.reflectImagePreset(preset);
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        // mCategoryBordersAdapter.reflectImagePreset(preset);
        // TCL ShenQianfeng End on 2016.09.01
    }

    public View getMainStatePanelContainer(int id) {
        return findViewById(id);
    }

    public void onShowMenu(PopupMenu menu) {
        mCurrentMenu = menu;
        menu.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(PopupMenu popupMenu){
        if (mCurrentMenu == null) {
            return;
        }
        mCurrentMenu.setOnDismissListener(null);
        mCurrentMenu = null;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mCurrentDialog = dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        mCurrentDialog = null;
    }

    private class LoadHighresBitmapTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            MasterImage master = MasterImage.getImage();
            Rect originalBounds = master.getOriginalBounds();
            if (master.supportsHighRes()) {
                int highresPreviewSize = master.getOriginalBitmapLarge().getWidth() * 2;
                if (highresPreviewSize > originalBounds.width()) {
                    highresPreviewSize = originalBounds.width();
                }
                Rect bounds = new Rect();
                Bitmap originalHires = ImageLoader.loadOrientedConstrainedBitmap(master.getUri(),
                        master.getActivity(), highresPreviewSize,
                        master.getOrientation(), bounds);
                master.setOriginalBounds(bounds);
                master.setOriginalBitmapHighres(originalHires);
                mBoundService.setOriginalBitmapHighres(originalHires);
                master.warnListeners();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Bitmap highresBitmap = MasterImage.getImage().getOriginalBitmapHighres();
            if (highresBitmap != null) {
                float highResPreviewScale = (float) highresBitmap.getWidth()
                        / (float) MasterImage.getImage().getOriginalBounds().width();
                mBoundService.setHighresPreviewScaleFactor(highResPreviewScale);
            }
            MasterImage.getImage().warnListeners();
            //modify begin by liaoanhua
            setSelectWholeBitmap();
            //modify end
        }
    }
   //modify begin by liaoanhua
   public void setSelectWholeBitmap() {
        FilterRepresentation WholeCropFilters = new FilterCropRepresentation();
        WholeCropFilters.setTextId(mWholeCorpId);
        showRepresentation(WholeCropFilters);
   }
   //modify end

    public boolean isLoadingVisible() {
        return mLoadingVisible;
    }

    public void startLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        mLoadingVisible = true;
        loading.setVisibility(View.VISIBLE);
    }

    public void stopLoadingIndicator() {
        final View loading = findViewById(R.id.loading);
        loading.setVisibility(View.GONE);
        mLoadingVisible = false;
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Boolean, Boolean> {
        int mBitmapSize;

        public LoadBitmapTask() {
            mBitmapSize = getScreenImageSize();
        }

        @Override
        protected Boolean doInBackground(Uri... params) {
            if (!MasterImage.getImage().loadBitmap(params[0], mBitmapSize)) {
                return false;
            }
            publishProgress(ImageLoader.queryLightCycle360(MasterImage.getImage().getActivity()));
            return true;
        }

        @Override
        protected void onProgressUpdate(Boolean... values) {
            super.onProgressUpdate(values);
            if (isCancelled()) {
                return;
            }
            if (values[0]) {
                mShowingTinyPlanet = true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            MasterImage.setMaster(mMasterImage);
            if (isCancelled()) {
                return;
            }

            if (!result) {
                if (mOriginalImageUri != null
                        && !mOriginalImageUri.equals(mSelectedImageUri)) {
                    mOriginalImageUri = mSelectedImageUri;
                    mOriginalPreset = null;
                    Toast.makeText(FilterShowActivity.this,
                            R.string.cannot_edit_original, Toast.LENGTH_SHORT).show();
                    startLoadBitmap(mOriginalImageUri);
                } else {
                    cannotLoadImage();
                }
                return;
            }

            if (null == CachingPipeline.getRenderScriptContext()){
                Log.v(LOGTAG,"RenderScript context destroyed during load");
                return;
            }
            final View imageShow = findViewById(R.id.imageShow);
            imageShow.setVisibility(View.VISIBLE);


            Bitmap largeBitmap = MasterImage.getImage().getOriginalBitmapLarge();
            mBoundService.setOriginalBitmap(largeBitmap);

            float previewScale = (float) largeBitmap.getWidth()
                    / (float) MasterImage.getImage().getOriginalBounds().width();
            mBoundService.setPreviewScaleFactor(previewScale);
            // TCL ShenQianfeng Begin on 2016.08.29
            // Annotated Below:
            // we have removed tiny planet from addEffects in BaseFilterManager
            /*
            if (!mShowingTinyPlanet) {
                mCategoryFiltersAdapter.removeTinyPlanet();
            }
            */
            // TCL ShenQianfeng End on 2016.08.29
            // TCL ShenQianfeng Begin on 2016.08.22
            mCategoryCropAdapter.imageLoaded();
            mCategoryRotateAdapter.imageLoaded();
            // TCL ShenQianfeng End on 2016.08.22
            mCategoryLooksAdapter.imageLoaded();
            // TCL ShenQianfeng Begin on 2016.09.01
            // Annotated Below:
            /*
            mCategoryBordersAdapter.imageLoaded();
            mCategoryGeometryAdapter.imageLoaded();
            */
            // TCL ShenQianfeng End on 2016.09.01
            mCategoryFiltersAdapter.imageLoaded();
            mLoadBitmapTask = null;

            MasterImage.getImage().warnListeners();
            loadActions();

            if (mOriginalPreset != null) {
                MasterImage.getImage().setLoadedPreset(mOriginalPreset);
                MasterImage.getImage().setPreset(mOriginalPreset,
                        mOriginalPreset.getLastRepresentation(), true);
                mOriginalPreset = null;
            } else {
                setDefaultPreset();
            }

            MasterImage.getImage().resetGeometryImages(true);

            if (mAction == TINY_PLANET_ACTION) {
                showRepresentation(mCategoryFiltersAdapter.getTinyPlanet());
            }
            LoadHighresBitmapTask highresLoad = new LoadHighresBitmapTask();
            highresLoad.execute();
            MasterImage.getImage().warnListeners();
            super.onPostExecute(result);
        }

    }

    private void clearGalleryBitmapPool() {
        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Free memory held in Gallery's Bitmap pool.  May be O(n) for n bitmaps.
                GalleryBitmapPool.getInstance().clear();
                return null;
            }
        }).execute();
    }

    //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-26,PR954582 begin
    private void clearAdapter(CategoryAdapter adapter) {
        if (adapter != null) {
            adapter.clear();
        }
    }

    private void clearCategoryAdapter() {
        // TCL ShenQianfeng Begin on 2016.08.19
        clearAdapter(mCategoryCropAdapter);
        clearAdapter(mCategoryRotateAdapter);
        // TCL ShenQianfeng End on 2016.08.19
        clearAdapter(mCategoryLooksAdapter);
//        clearAdapter(mCategoryCropEditAdapter);
//        clearAdapter(mCategoryFaceBeautyAdapter);
        
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        /*
        clearAdapter(mCategoryBordersAdapter);
        clearAdapter(mCategoryGeometryAdapter);
        */
        // TCL ShenQianfeng End on 2016.09.01

        clearAdapter(mCategoryFiltersAdapter);
        clearAdapter(mCategoryVersionsAdapter);
    }

    private void clearActions() {
        if (mActions == null || mActions.isEmpty())
            return;

        for (int i = 0; i < mActions.size(); i++) {
            Action action = mActions.get(i);
            action.clearBitmap();
        }
    }
    //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-26,PR954582 end

    @Override
    protected void onDestroy() {
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
        }
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 begin
        Log.i(LOGTAG, "FilterShowActivity onDestory in");
        if (mUserPresetsManager != null) {
            mUserPresetsManager.close();
        }
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 end
        doUnbindService();
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-26,PR954582 begin
        clearGalleryBitmapPool();
        clearCategoryAdapter();
        clearActions();
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-26,PR954582 end
        // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-06-12,PR1003170 begin
        CategoryUtil.clearArray();
        // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-06-12,PR1003170 end
        ///[BUGFIX]-MOD-BEGIN BY TSNJ.LIUDEKUAN ON 2016/01/06 FOR PR1192698
        if (mAutoEditHelper != null) {
            mAutoEditHelper.resetAutoEditStatus();
        }
        ///[BUGFIX]-MOD-END BY TSNJ.LIUDEKUAN
        super.onDestroy();
    }

    // TODO: find a more robust way of handling image size selection
    // for high screen densities.
    private int getScreenImageSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
    }

    private void showSavingProgress(String albumName) {
        ProgressDialog progress;
        if (mSavingProgressDialog != null) {
            progress = mSavingProgressDialog.get();
            if (progress != null) {
                progress.show();
                return;
            }
        }
        // TODO: Allow cancellation of the saving process
        String progressText;
        if (albumName == null) {
            progressText = getString(R.string.saving_image);
        } else {
            progressText = getString(R.string.filtershow_saving_image, albumName);
        }
        progress = ProgressDialog.show(this, "", progressText, true, false);
        mSavingProgressDialog = new WeakReference<ProgressDialog>(progress);
    }

    private void hideSavingProgress() {
        if (mSavingProgressDialog != null) {
            ProgressDialog progress = mSavingProgressDialog.get();
            if (progress != null)
                progress.dismiss();
        }
    }

    public void completeSaveImage(Uri saveUri) {
        if (mSharingImage && mSharedOutputFile != null) {
            // Image saved, we unblock the content provider
            Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI,
                    Uri.encode(mSharedOutputFile.getAbsolutePath()));
            ContentValues values = new ContentValues();
            values.put(SharedImageProvider.PREPARE, false);
            getContentResolver().insert(uri, values);
        }

        /* MODIFIED-BEGIN by lina.tang, 2016-08-16,BUG-2255033*/
        new UpdateScannerClient(getBaseContext(), ImageLoader.getLocalPathFromUri(getBaseContext(), saveUri));

        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 begin
        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                FilterShowSaveDialog.MSG_SAVE_DONE, saveUri), 1000);
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 end
    }

    public void SaveImagePreview(Uri saveUri) {
        if (mSharingImage && mSharedOutputFile != null) {
            // Image saved, we unblock the content provider
            Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI,
                    Uri.encode(mSharedOutputFile.getAbsolutePath()));
            ContentValues values = new ContentValues();
            values.put(SharedImageProvider.PREPARE, false);
            getContentResolver().insert(uri, values);
        }
    }
    /* MODIFIED-END by lina.tang,BUG-2255033*/


    @Override
    public boolean onShareTargetSelected(ShareActionProvider arg0, Intent arg1) {
        // First, let's tell the SharedImageProvider that it will need to wait
        // for the image
        Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI,
                Uri.encode(mSharedOutputFile.getAbsolutePath()));
        ContentValues values = new ContentValues();
        values.put(SharedImageProvider.PREPARE, true);
        getContentResolver().insert(uri, values);
        mSharingImage = true;

        // Process and save the image in the background.
        showSavingProgress(null);
        mImageShow.saveImage(this, mSharedOutputFile);
        return true;
    }

    private Intent getDefaultShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType(SharedImageProvider.MIME_TYPE);
        mSharedOutputFile = SaveImage.getNewFile(this, MasterImage.getImage().getUri());
        Uri uri = Uri.withAppendedPath(SharedImageProvider.CONTENT_URI,
                Uri.encode(mSharedOutputFile.getAbsolutePath()));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
        getMenuInflater().inflate(R.menu.filtershow_activity_menu, menu);
        MenuItem showState = menu.findItem(R.id.showImageStateButton);
        mSaveButton = menu.findItem(R.id.saveButton);
//        mAutoEditButton = menu.findItem(R.id.autoEdit);
//        mDrawEditButton = menu.findItem(R.id.drawEdit);
//        mDrawUButton = menu.findItem(R.id.undoDraw);
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-17,PR951972 begin
        enableSave(false);
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-17,PR951972 end
        if (mShowingImageStatePanel) {
            showState.setTitle(R.string.hide_imagestate_panel);
        } else {
            showState.setTitle(R.string.show_imagestate_panel);
        }
        mShareActionProvider = (ShareActionProvider) menu.findItem(R.id.menu_share)
                .getActionProvider();
        mShareActionProvider.setShareIntent(getDefaultShareIntent());
        mShareActionProvider.setOnShareTargetSelectedListener(this);
        mMenu = menu;
        setupMenu();

        return true;
        */
        return false;
    }
    private void setupMenu(){
        if (mMenu == null || mMasterImage == null) {
            return;
        }
        MenuItem undoItem = mMenu.findItem(R.id.undoButton);
        MenuItem redoItem = mMenu.findItem(R.id.redoButton);
        MenuItem resetItem = mMenu.findItem(R.id.resetHistoryButton);
        MenuItem printItem = mMenu.findItem(R.id.printButton);
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 begin
        MenuItem infoItem = mMenu.findItem(R.id.showInfoPanel);
        MenuItem stateItem = mMenu.findItem(R.id.showImageStateButton);
        MenuItem exportItem = mMenu.findItem(R.id.exportFlattenButton);
        autoeditorItem = mMenu.findItem(R.id.autoEdit); // MODIFIED by wei.song, 2016-05-11,BUG-2114749
        if (!PrintHelper.systemSupportsPrint()) {
            printItem.setVisible(false);
        }
        //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 begin
        mMasterImage.getHistory().setMenuItems(undoItem, redoItem, resetItem,
                infoItem, stateItem, exportItem, printItem,autoeditorItem);
        //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 end
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 end
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mShareActionProvider != null) {
            mShareActionProvider.setOnShareTargetSelectedListener(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mShareActionProvider != null) {
            mShareActionProvider.setOnShareTargetSelectedListener(this);
        }
        // TCL ShenQianfeng Begin on 2016.09.01
        hideNavigationBar();
        // TCL ShenQianfeng End on 2016.09.01
        
    }
    
    
    // TCL ShenQianfeng Begin on 2016.09.01
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            hideNavigationBar();
        }
            
    }
    // TCL ShenQianfeng End on 2016.09.01

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undoButton: {
                HistoryManager adapter = mMasterImage.getHistory();
                int position = adapter.undo();
                mMasterImage.onHistoryItemClick(position);
                backToMain();
                invalidateViews();
                return true;
            }
            case R.id.redoButton: {
                HistoryManager adapter = mMasterImage.getHistory();
                int position = adapter.redo();
                mMasterImage.onHistoryItemClick(position);
                invalidateViews();
                return true;
            }
            case R.id.resetHistoryButton: {
                resetHistory();
                return true;
            }
            case R.id.showImageStateButton: {
                toggleImageStatePanel();
                return true;
            }
            case R.id.exportFlattenButton: {
                showExportOptionsDialog();
                return true;
            }
            case android.R.id.home: {
//                saveImage();
                this.onBackPressed();
                return true;
            }
            case R.id.manageUserPresets: {
                manageUserPresets();
                return true;
            }
            case R.id.showInfoPanel: {
                toggleInformationPanel();
                return true;
            }
            case R.id.printButton: {
                print();
                return true;
            }
            case R.id.saveButton: {
                //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 begin
                if (mImageShow.hasModifications()) {
                    FilterShowSaveDialog dialog = new FilterShowSaveDialog(FilterShowActivity.this, mHandler);
                    dialog.show();
                }
                //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-02-11,CR915235 end
                return true;
            }
            case R.id.autoEdit: {
                /* MODIFIED-BEGIN by wei.song, 2016-05-11,BUG-2114749*/
                /* MODIFIED-BEGIN wencan.wu1, 2016-07-12,BUG-2475956 start*/
                if(null != autoeditorItem)
                {
                    if(isAutoPressed){
                        autoeditorItem.setIcon(R.drawable.ic_auto_on);
                        isAutoPressed = false;
                    }else {
                        autoeditorItem.setIcon(R.drawable.ic_auto);
                        isAutoPressed = true;
                    }
                }
                /* MODIFIED-BEGIN wencan.wu1, 2016-07-12,BUG-2475956 end*/
                /* MODIFIED-END by wei.song,BUG-2114749*/
                //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 begin
                if (mAutoEditHelper != null) {
                    if (mHasAutoEditAdd) {
                        /*MODIFIED-BEGIN by caihong.gu-nb, 2016-04-13,BUG-1930098*/
                        mAutoEditHelper.showCurrentRepresentation(false, FilterShowActivity.this);
                        mHasAutoEditAdd = false;
                    } else {
                        mAutoEditHelper.showCurrentRepresentation(true, FilterShowActivity.this);
                        /*MODIFIED-END by caihong.gu-nb,BUG-1930098*/
                        mHasAutoEditAdd = true;
                    }
                }
                //[BUGFIX]-Modify by TCTNJ,caihong.gu-nb, 2016/01/21,PR1238666 end
                return true;
            }
//            case R.id.drawEdit: {
//                if (mDrawRep == null) {
//                    mDrawRep = new FilterDrawRepresentation();
//                }
//                showRepresentation(mDrawRep);
//                reloadDrawMenuItem(true);
//                return true;
//            }
            case R.id.undoDraw: {
                if (mDrawRep != null) {
                    mEditorPlaceHolder.undoWithDraw(mDrawRep.getEditorId());
                }
                return true;
            }
        }
        return false;
    }

    public void print() {
        Bitmap bitmap = MasterImage.getImage().getHighresImage();
        PrintHelper printer = new PrintHelper(this);
        printer.printBitmap("ImagePrint", bitmap);
    }

    public void addNewPreset() {
        DialogFragment dialog = new PresetManagementDialog();
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void manageUserPresets() {
        DialogFragment dialog = new PresetManagementDialog();
        dialog.show(getSupportFragmentManager(), "NoticeDialogFragment");
    }

    private void showExportOptionsDialog() {
        //[ALM][BUGFIX]-Add by TCTNJ,jun.xie-nb, 2015-4-12,ALM-1941392 begin
        Rect mOriginalBounds = MasterImage.getImage().getOriginalBounds();
        ImagePreset preset = MasterImage.getImage().getPreset();
        if (mOriginalBounds == null || preset == null) {
            return;
        }

        DialogFragment dialog = new ExportDialog(mOriginalBounds, preset);
        //[ALM][BUGFIX]-Add by TCTNJ,jun.xie-nb, 2015-4-12,ALM-1941392 end
        dialog.show(getSupportFragmentManager(), "ExportDialogFragment");
    }

    public void updateUserPresetsFromAdapter(UserPresetsAdapter adapter) {
        ArrayList<FilterUserPresetRepresentation> representations =
                adapter.getDeletedRepresentations();
        for (FilterUserPresetRepresentation representation : representations) {
            deletePreset(representation.getId());
        }
        ArrayList<FilterUserPresetRepresentation> changedRepresentations =
                adapter.getChangedRepresentations();
        for (FilterUserPresetRepresentation representation : changedRepresentations) {
            updatePreset(representation);
        }
        adapter.clearDeletedRepresentations();
        adapter.clearChangedRepresentations();
        loadUserPresets();
    }

    public void loadUserPresets() {
        mUserPresetsManager.load();
        updateUserPresetsFromManager();
    }

    public void updateUserPresetsFromManager() {
        ArrayList<FilterUserPresetRepresentation> presets = mUserPresetsManager.getRepresentations();
        if (presets == null) {
            return;
        }
        if (mCategoryLooksAdapter != null) {
            fillLooks();
        }
        if (presets.size() > 0) {
            mCategoryLooksAdapter.add(new Action(this, Action.SPACER));
        }
        mUserPresetsAdapter.clear();
        for (int i = 0; i < presets.size(); i++) {
            FilterUserPresetRepresentation representation = presets.get(i);
            mCategoryLooksAdapter.add(
                    new Action(this, representation, Action.FULL_VIEW, true));
            mUserPresetsAdapter.add(new Action(this, representation, Action.FULL_VIEW));
        }
        if (presets.size() > 0) {
            mCategoryLooksAdapter.add(new Action(this, Action.ADD_ACTION));
        }
        mCategoryLooksAdapter.notifyDataSetChanged();
        mCategoryLooksAdapter.notifyDataSetInvalidated();
    }

    public void saveCurrentImagePreset(String name) {
        mUserPresetsManager.save(MasterImage.getImage().getPreset(), name);
    }

    private void deletePreset(int id) {
        mUserPresetsManager.delete(id);
    }

    private void updatePreset(FilterUserPresetRepresentation representation) {
        mUserPresetsManager.update(representation);
    }

    public void enableSave(boolean enable) {
        // TCL ShenQianfeng Begin on 2016.09.05
        // Original:
        /*
        if (mSaveButton != null
                && enable != mSaveButton.isEnabled()) { //[BUGFIX]-Modify by TCTNJ, dongliang.feng, 2015-07-14, PR1042208
            mSaveButton.setEnabled(enable);
            //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-17,PR951972 begin
            Drawable drawable = mSaveButton.getIcon();
            if (drawable != null) {
                drawable.setAlpha(enable ? 255 : 0);
            }
            //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-03-17,PR951972 end
        }
        */
        // Modify To:
        mTopBarManager.enableSaveButton(enable);
        // TCL ShenQianfeng End on 2016.09.05
    }

    // TCL ShenQianfeng Begin on 2016.08.22
    private void fillCrops() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> crops = filtersManager.getCrops();
        if (mCategoryCropAdapter != null) {
            mCategoryCropAdapter.clear();
        }
        mCategoryCropAdapter = new CategoryAdapter(this);
        for (FilterRepresentation representation : crops) {
            mCategoryCropAdapter.add(new Action(this, representation, Action.CROP_VIEW));
        }
    }
    
    private void fillRotate() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> rotates = filtersManager.getRotate();
        if (mCategoryRotateAdapter != null) {
            mCategoryRotateAdapter.clear();
        }
        mCategoryRotateAdapter = new CategoryAdapter(this);
        int pos = 0;
        for (FilterRepresentation representation : rotates) {
            mCategoryRotateAdapter.add(new Action(this, representation, Action.CROP_VIEW, pos));
            pos++;
        }
    }
    
    // TCL ShenQianfeng End on 2016.08.22
    
    private void fillLooks() {
        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getLooks();

        int savedUserSelectedPos = -1;

        if (mCategoryLooksAdapter != null) {
            savedUserSelectedPos = mCategoryLooksAdapter.getUserSelectedPos();
            mCategoryLooksAdapter.clear();
        }
        mCategoryLooksAdapter = new CategoryAdapter(this);
        // TCL ShenQianfeng Begin on 2016.09.01
        // Annotated Below:
        /*
        int verticalItemHeight = (int) getResources().getDimension(R.dimen.action_item_height);
        mCategoryLooksAdapter.setItemHeight(verticalItemHeight);
        */
        // TCL ShenQianfeng End on 2016.09.01
        for (FilterRepresentation representation : filtersRepresentations) {
            mCategoryLooksAdapter.add(new Action(this, representation, Action.FULL_VIEW));
        }
        mCategoryLooksAdapter.setDefaultUserSelectedPos(savedUserSelectedPos); // MODIFIED by Hongbin.Chen, 2016-06-29,BUG-2432573
        
        // TCL ShenQianfeng Begin on 2016.08.29
        // Annotated Below:
        // we do not need the "ADD" button
        /*
        if (mUserPresetsManager.getRepresentations() == null
            || mUserPresetsManager.getRepresentations().size() == 0) {
            mCategoryLooksAdapter.add(new Action(this, Action.ADD_ACTION));
        }
        */
        // TCL ShenQianfeng End on 2016.08.29
        
        // TCL ShenQianfeng Begin on 2016.09.05
        // Annotated Below:
        /*
        Fragment panel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        if (panel != null) {
            if (panel instanceof MainPanel) {
                MainPanel mainPanel = (MainPanel) panel;
                mainPanel.loadCategoryLookPanel(true);
            }
        }
        // TCL ShenQianfeng End on 2016.09.05
        */
    }

    public void setDefaultPreset() {
        // Default preset (original)
        ImagePreset preset = new ImagePreset(); // empty
        mMasterImage.setPreset(preset, preset.getLastRepresentation(), true);
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Some utility functions
    // TODO: finish the cleanup.

    public void invalidateViews() {
        for (ImageShow views : mImageViews) {
            views.updateImage();
        }
    }

    public void hideImageViews() {
        for (View view : mImageViews) {
            view.setVisibility(View.GONE);
        }
        mEditorPlaceHolder.hide();
    }

    // //////////////////////////////////////////////////////////////////////////////
    // imageState panel...

    public void toggleImageStatePanel() {
        invalidateOptionsMenu();
        mShowingImageStatePanel = !mShowingImageStatePanel;
        Fragment panel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        if (panel != null) {
            if (panel instanceof EditorPanel) {
                EditorPanel editorPanel = (EditorPanel) panel;
                editorPanel.showImageStatePanel(mShowingImageStatePanel);
            } else if (panel instanceof MainPanel) {
                MainPanel mainPanel = (MainPanel) panel;
                mainPanel.showImageStatePanel(mShowingImageStatePanel);
            }
        }
    }

    public void toggleVersionsPanel() {
        mShowingVersionsPanel = !mShowingVersionsPanel;
        Fragment panel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        if (panel != null && panel instanceof MainPanel) {
            MainPanel mainPanel = (MainPanel) panel;
            mainPanel.loadCategoryVersionsPanel();
        }
    }

    //[BUGFIX]-Modify by TCTNJ, dongliang.feng, 2015-08-25, PR525345 begin
//    @Override
//    public void onConfigurationChanged(Configuration newConfig)
//    {
//        super.onConfigurationChanged(newConfig);
//
//        setDefaultValues();
//        if (mMasterImage == null) {
//            return;
//        }
//        loadXML();
//        fillCategories();
//        loadMainPanel();
//
//        if (mCurrentMenu != null) {
//            mCurrentMenu.dismiss();
//            mCurrentMenu = null;
//        }
//        if (mCurrentDialog != null) {
//            mCurrentDialog.dismiss();
//            mCurrentDialog = null;
//        }
//        // mLoadBitmapTask==null implies you have looked at the intent
//        if (!mShowingTinyPlanet && (mLoadBitmapTask == null)) {
//            mCategoryFiltersAdapter.removeTinyPlanet();
//        }
//        stopLoadingIndicator();
//    }
    //[BUGFIX]-Modify by TCTNJ, dongliang.feng, 2015-08-25, PR525345 end

    public void setupMasterImage() {

        HistoryManager historyManager = new HistoryManager();
        StateAdapter imageStateAdapter = new StateAdapter(this, 0);
        MasterImage.reset();
        mMasterImage = MasterImage.getImage();
        mMasterImage.setHistoryManager(historyManager);
        mMasterImage.setStateAdapter(imageStateAdapter);
        mMasterImage.setActivity(this);

        if (Runtime.getRuntime().maxMemory() > LIMIT_SUPPORTS_HIGHRES) {
            mMasterImage.setSupportsHighRes(true);
        } else {
            mMasterImage.setSupportsHighRes(false);
        }
    }

    void resetHistory() {
        HistoryManager adapter = mMasterImage.getHistory();
        adapter.reset();

        FiltersManager filtersManager = FiltersManager.getManager();
        ArrayList<FilterRepresentation> filtersRepresentations = filtersManager.getTools();
        for (FilterRepresentation representation : filtersRepresentations) {
            if (representation instanceof FilterRotateRepresentation) {
                ((FilterRotateRepresentation) representation).setRotation(FilterRotateRepresentation.getNil());
            }
            if (representation instanceof FilterMirrorRepresentation) {
                ((FilterMirrorRepresentation) representation).setMirror(FilterMirrorRepresentation.getNil());
            }
        }

        HistoryItem historyItem = adapter.getItem(0);
        ImagePreset original = null;
        if (RESET_TO_LOADED) {
            original = new ImagePreset(historyItem.getImagePreset());
        } else {
            original = new ImagePreset();
        }
        FilterRepresentation rep = null;
        if (historyItem != null) {
            rep = historyItem.getFilterRepresentation();
        }
        mMasterImage.setPreset(original, rep, true);
        invalidateViews();
        backToMain();
    }

    public void showDefaultImageView() {
         //modify by liaoah
        if (mServicesLoadFinish == false) return;
        //modify end
        mEditorPlaceHolder.hide();
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 begin
        if (mImageShow != null) {
            mImageShow.setVisibility(View.VISIBLE);
        }
        //[BUGFIX]-Add by TCTNJ,jian.pan1, 2015-04-27,PR950449 end
        MasterImage.getImage().setCurrentFilter(null);
        // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-07-17,PR1044901 begin
        MasterImage.getImage().setFilterSharpen(false);
        // [BUGFIX]-Add by TCTNJ,jian.pan1, 2015-07-17,PR1044901 end
        MasterImage.getImage().setCurrentFilterRepresentation(null);
        
        // TCL ShenQianfeng Begin on 2016.08.30
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        // TCL ShenQianfeng End on 2016.08.30
    }

    public void backToMain() {
        Fragment currentPanel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        if (currentPanel instanceof MainPanel) {
            // TCL ShenQianfeng Begin on 2016.08.23
            showDefaultImageView();
            // TCL ShenQianfeng End on 2016.08.23
            return;
        }
        loadMainPanel();
        showDefaultImageView();
    }

    @Override
    public void onBackPressed() {
        Fragment currentPanel = getSupportFragmentManager().findFragmentByTag(MainPanel.FRAGMENT_TAG);
        if (currentPanel instanceof MainPanel) {
            if (!mImageShow.hasModifications()) {
                done();
            } else {
                /* MODIFIED-BEGIN by caihong.gu-nb, 2016-04-20,BUG-1963058*/
                String cancelMessage = getString(R.string.review_cancel).toUpperCase();
                String discardMessage = getString(R.string.trim_discard).toUpperCase();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.trim_message)/*.setTitle(R.string.trim_title)*/;
                builder.setNegativeButton(cancelMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if(mDialog != null){
                            mDialog.dismiss();
                        }
                    }
                });
                builder.setPositiveButton(discardMessage, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        done();
                    }
                });
                mDialog = builder.show();
                /* MODIFIED-END by caihong.gu-nb,BUG-1963058*/
            }
        } else {
            /*MODIFIED-BEGIN by jian.pan1, 2016-04-08,BUG-1864110*/
            if (currentPanel instanceof EditorPanel) {
                Editor editor = ((EditorPanel) currentPanel).getEditor();
                if (editor != null && editor.getID() == R.id.editorCrop) {
                    ((EditorPanel) currentPanel).cancelCurrentFilter();
                }
            }
            /*MODIFIED-END by jian.pan1,BUG-1864110*/
            backToMain();
        }
    }

    public void cannotLoadImage() {
        Toast.makeText(this, R.string.cannot_load_image, Toast.LENGTH_SHORT).show();
        finish();
    }

    // //////////////////////////////////////////////////////////////////////////////

    public float getPixelsFromDip(float value) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                r.getDisplayMetrics());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        mMasterImage.onHistoryItemClick(position);
        invalidateViews();
    }

    public void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_image)),
                SELECT_PICTURE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                startLoadBitmap(selectedImageUri);
            }
        }
    }


    public void saveImage() {
      //[BUGFIX]-Add-BEGIN by TCTNJ.ye.chen, 2015/03/21 PR-956368.
        if(ImageLoader.getLocalPathFromUri(getBaseContext(), mSelectedImageUri) == null) {
            cannotLoadImage();
            return;
        }
      //[BUGFIX]-Add-BEGIN by TCTNJ.ye.chen, 2015/03/21 PR-956368.
        if (mImageShow.hasModifications()) {
            // Get the name of the album, to which the image will be saved
            File saveDir = SaveImage.getFinalSaveDirectory(this, mSelectedImageUri);
            int bucketId = GalleryUtils.getBucketId(saveDir.getPath());
            String albumName = LocalAlbum.getLocalizedName(getResources(), bucketId, null);
            showSavingProgress(albumName);
            mImageShow.saveImage(this, null);
            saveExtraWork(); // MODIFIED by Hongbin.Chen, 2016-06-29,BUG-2432573
        } else {
            done();
        }
    }


    public void done() {
        hideSavingProgress();
        if (mLoadBitmapTask != null) {
            mLoadBitmapTask.cancel(false);
        }
        finish();
    }

    private void extractXMPData() {
        XMresults res = XmpPresets.extractXMPData(
                getBaseContext(), mMasterImage, getIntent().getData());
        if (res == null)
            return;

        mOriginalImageUri = res.originalimage;
        mOriginalPreset = res.preset;
    }

    public Uri getSelectedImageUri() {
        return mSelectedImageUri;
    }

    public void setHandlesSwipeForView(View view, float startX, float startY) {
        if (view != null) {
            mHandlingSwipeButton = true;
        } else {
            mHandlingSwipeButton = false;
        }
        mHandledSwipeView = view;
        int[] location = new int[2];
        view.getLocationInWindow(location);
        mSwipeStartX = location[0] + startX;
        mSwipeStartY = location[1] + startY;
    }

    // TCL ShenQianfeng Begin on 2016.09.05
    // Annotated Below:
    /*
    public boolean dispatchTouchEvent (MotionEvent ev) {
        if (mHandlingSwipeButton) {
            int direction = CategoryView.HORIZONTAL;
            if (mHandledSwipeView instanceof CategoryView) {
                direction = ((CategoryView) mHandledSwipeView).getOrientation();
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float delta = ev.getY() - mSwipeStartY;
                float distance = mHandledSwipeView.getHeight();
                if (direction == CategoryView.VERTICAL) {
                    delta = ev.getX() - mSwipeStartX;
                    mHandledSwipeView.setTranslationX(delta);
                    distance = mHandledSwipeView.getWidth();
                } else {
                    mHandledSwipeView.setTranslationY(delta);
                }
                delta = Math.abs(delta);
                float transparency = Math.min(1, delta / distance);
                mHandledSwipeView.setAlpha(1.f - transparency);
                mHandledSwipeViewLastDelta = delta;
            }
            if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL
                    || ev.getActionMasked() == MotionEvent.ACTION_UP) {
                mHandledSwipeView.setTranslationX(0);
                mHandledSwipeView.setTranslationY(0);
                mHandledSwipeView.setAlpha(1.f);
                mHandlingSwipeButton = false;
                float distance = mHandledSwipeView.getHeight();
                if (direction == CategoryView.VERTICAL) {
                    distance = mHandledSwipeView.getWidth();
                }
                if (mHandledSwipeViewLastDelta > distance) {
                    ((SwipableView) mHandledSwipeView).delete();
                }
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
    */
    // TCL ShenQianfeng End on 2016.09.05

    public Point mHintTouchPoint = new Point();

    public Point hintTouchPoint(View view) {
        int location[] = new int[2];
        view.getLocationOnScreen(location);
        int x = mHintTouchPoint.x - location[0];
        int y = mHintTouchPoint.y - location[1];
        return new Point(x, y);
    }

    public void startTouchAnimation(View target, float x, float y) {
        final CategorySelected hint =
                (CategorySelected) findViewById(R.id.categorySelectedIndicator);
        int location[] = new int[2];
        target.getLocationOnScreen(location);
        mHintTouchPoint.x = (int) (location[0] + x);
        mHintTouchPoint.y = (int) (location[1] + y);
        int locationHint[] = new int[2];
        ((View)hint.getParent()).getLocationOnScreen(locationHint);
        int dx = (int) (x - (hint.getWidth())/2);
        int dy = (int) (y - (hint.getHeight())/2);
        hint.setTranslationX(location[0] - locationHint[0] + dx);
        hint.setTranslationY(location[1] - locationHint[1] + dy);
        hint.setVisibility(View.VISIBLE);
        hint.animate().scaleX(2).scaleY(2).alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                hint.setVisibility(View.INVISIBLE);
                hint.setScaleX(1);
                hint.setScaleY(1);
                hint.setAlpha(1);
            }
        });
    }
    
    // TCL ShenQianfeng Begin on 2016.08.23
    public void cancelCurrentFilter() {
        //LogUtil.i2(TAG, "cancelCurrentFilter ---------- ");
        MasterImage masterImage = MasterImage.getImage();
        HistoryManager adapter = masterImage.getHistory();
        int position = adapter.undo();
        masterImage.onHistoryItemClick(position);
        invalidateViews();
    }
    
    private boolean hasModifications() {
        return mImageShow != null && mImageShow.hasModifications();
    }
    
    public void cancelApplyCrop(boolean needCancelCurrentFilter) {
        if(needCancelCurrentFilter) {
            cancelCurrentFilter();
        }
        backToMain();
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        mTopBarManager.enableSaveButton(hasModifications());
    }
    
    private void applyCrop() {
        if (mCurrentEditor == null) return;
        mCurrentEditor.finalApplyCalled();
        backToMain();
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        if(hasModifications()) {
            mTopBarManager.enableSaveButton(true);
        }
    }
    
    private void cancelApplyColor() {
        cancelCurrentFilter();
        backToMain();
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        mTopBarManager.enableSaveButton(hasModifications());
    }
    
    private void applyColor() {
        if (mCurrentEditor == null) return;
        mCurrentEditor.finalApplyCalled();
        backToMain();
        mTopBarManager.switchMode(TopBarManager.MODE_CANCEL_SAVE);
        if(hasModifications()) {
            mTopBarManager.enableSaveButton(true);
        }
    }
    
    private void showSaveDialog() {
        String cancelMessage = getString(android.R.string.cancel);
        String okMessage = getString(android.R.string.ok);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.edit_save_message).setTitle(R.string.save);
        builder.setNegativeButton(cancelMessage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if(mDialog != null){
                    mDialog.dismiss();
                }
            }
        });
        builder.setPositiveButton(okMessage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mDialog.dismiss();
                mHandler.sendMessage(mHandler.obtainMessage(FilterShowSaveDialog.MSG_SAVE_COPY));
            }
        });
        mDialog = builder.show();
    }

    @Override
    public void onClick(View v) {
        //modify by liaoah begin
        synchronized(this) {
            int id = v.getId();
            switch(id) {
                case R.id.cancel_btn: {
                    onBackPressed();
                    break;
              }
             case R.id.cancel_apply_btn: {
                if(mCurrentPanel == MainPanel.CROP) {
                    cancelApplyCrop(true);
                 } else if(mCurrentPanel == MainPanel.FILTERS) {
                     cancelApplyColor();
                 }
                break;
             }
             case R.id.save_btn: {
                if (mImageShow.hasModifications()) {
                    showSaveDialog();
                    /*
                    FilterShowSaveDialog dialog = new FilterShowSaveDialog(FilterShowActivity.this, mHandler);
                    dialog.show();
                    */
                }
                break;
            }
            case R.id.apply_btn: {
                if(mCurrentPanel == MainPanel.CROP) {
                     applyCrop();
                } else if(mCurrentPanel == MainPanel.FILTERS) {
                     applyColor();
                }
                break;
            }
            case R.id.reset_btn: {
                if(null == mCurrentEditor) return;
                FilterRepresentation rep = mCurrentEditor.getLocalRepresentation();
                if(mCurrentEditor instanceof BasicEditor) {
                    BasicEditor basicEditor = (BasicEditor)mCurrentEditor;
                    basicEditor.resetValueAndUI();
                    mTopBarManager.enableResetButton(false);
                } else if((mCurrentEditor instanceof EditorChanSat) && 
                        (rep instanceof FilterChanSatRepresentation)) {
                        EditorChanSat chanSatEditor = (EditorChanSat)mCurrentEditor;
                        //FilterChanSatRepresentation chanSatRep = (FilterChanSatRepresentation)rep;
                        //chanSatRep.setCurrentParameter(0);
                        chanSatEditor.resetValueAndUI();
                        mTopBarManager.enableResetButton(false);
                }
                break;
            }
          }
        }
        //modify end
    }
    
    public void enableResetButton(boolean enabled) {
        mTopBarManager.enableResetButton(enabled);
    }

    // TCL ShenQianfeng End on 2016.08.23
}
