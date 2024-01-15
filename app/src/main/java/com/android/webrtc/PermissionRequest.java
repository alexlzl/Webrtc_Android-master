package com.android.webrtc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装权限申请
 */
public class PermissionRequest extends Activity {

    private static final String TAG = "PermissionRequest";
    public static final int REQUEST_PERMISSION_CODE = 1;  //默认请求权限的requestCode为1
    private PermissionListener mListener;

    /**
     * 请求申请权限
     * 默认请求权限的requestCode为1
     *
     * @param permissions        要申请的权限数组
     * @param permissionListener 权限申请结果监听者
     */
    public void requestRuntimePermission(Context context, String[] permissions, PermissionListener permissionListener) {
        mListener = permissionListener;
        //存放permissions中当前未被授予的权限
        List<String> permissionList = new ArrayList<>();
        //遍历权限数组，检测所需权限是否已被授予，若该权限尚未授予，添加到permissionList中
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if (!permissionList.contains(permission)) {
                    permissionList.add(permission);
                }
            }
        }

        if (!permissionList.isEmpty()) {
            //有权限尚未授予，去授予权限
            ActivityCompat.requestPermissions((Activity) context, permissionList.toArray(new String[permissionList.size()]), REQUEST_PERMISSION_CODE);
        } else {
            //权限都被授予了
            if (mListener != null) {
                mListener.onGranted();  //权限都被授予了回调
                Log.d(TAG, "权限都授予了");
            }
        }
    }

    /**
     * 申请权限结果返回
     *
     * @param requestCode  请求码
     * @param permissions  所有申请的权限集合
     * @param grantResults 权限申请的结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0) { //有权限申请
                    //存储被用户拒绝的权限
                    List<String> deniedPermissionList = new ArrayList<>();
                    //有权限被拒绝，分类出被拒绝的权限
                    for (int i = 0; i < grantResults.length; i++) {
                        String permission = permissions[i];
                        int grantResult = grantResults[i];
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            if (!deniedPermissionList.contains(permission)) {
                                deniedPermissionList.add(permission);
                            }
                        }
                    }

                    if (deniedPermissionList.isEmpty()) {
                        //没有被拒绝的权限
                        if (mListener != null) {
                            mListener.onGranted();
                            Log.d(TAG, "权限都授予了");
                        }
                    } else {
                        //有被拒绝的权限
                        if (mListener != null) {
                            mListener.onDenied(deniedPermissionList);
                            Log.e(TAG, "有权限被拒绝了");
                        }
                    }
                }
                break;
        }
    }


    //动态申请权限
    public static String[] requestPermissionArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static void requestLocationPermission(Activity context, RequestCallBackListener requestCallBackListener) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionList = new ArrayList<>();
            //遍历权限数组，检测所需权限是否已被授予，若该权限尚未授予，添加到permissionList中
            for (String permission : requestPermissionArray) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (!permissionList.contains(permission)) {
                        permissionList.add(permission);
                    }
                }
            }

            String titleStart = "需要获取手机的";
            String middle = "【位置信息】";
            String endStr = "权限，用于连接动康设备";

            SpannableStringBuilder builder = new SpannableStringBuilder();
            SpannableString spannableString1 = new SpannableString(titleStart + middle);

            ClickableSpan clickableSpan1 = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
                    ds.setUnderlineText(false);
                    ds.setTypeface(Typeface.DEFAULT_BOLD);
                }
            };
            spannableString1.setSpan(clickableSpan1, titleStart.length()
                    , titleStart.length() + middle.length()
                    , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(spannableString1);
            builder.append(endStr);

            if (!permissionList.isEmpty()) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            } else {
                //所有权限都被授予了
                requestCallBackListener.onSuccess();
            }
        }
    }

    /**
     * 引导开始定位权限
     *
     * @param context
     */
    public static void requestLocationManager(Activity context, RequestCallBackListener requestCallBackListener) {
        requestLocationPermission(context, new RequestCallBackListener() {
            @Override
            public void onSuccess() {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    //如果用户已经打开定位服务逻辑
                    requestCallBackListener.onSuccess();
                } else {


                    String titleStart = "需要开启手机的";
                    String middle = "【定位】";
                    String endStr = "权限，用于连接动康设备";

                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    SpannableString spannableString1 = new SpannableString(titleStart + middle);

                    ClickableSpan clickableSpan1 = new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                        }

                        @Override
                        public void updateDrawState(TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setColor(ContextCompat.getColor(context, R.color.colorAccent));
                            ds.setUnderlineText(false);
                            ds.setTypeface(Typeface.DEFAULT_BOLD);
                        }
                    };
                    spannableString1.setSpan(clickableSpan1, titleStart.length()
                            , titleStart.length() + middle.length()
                            , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.append(spannableString1);
                    builder.append(endStr);


                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivityForResult(intent, 1);
                    }
                }
            }

            @Override
            public void onFaile() {

            }
        });
    }

    /**
     * 权限成功失败监听
     */
    public interface RequestCallBackListener {

        void onSuccess();

        void onFaile();

    }
}
