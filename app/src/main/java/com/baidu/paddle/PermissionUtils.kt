/*
 * copyright: Copyright © 2016 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.paddle

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import java.util.*

/**
 * 权限工具类
 **/
object PermissionUtils {

    interface PermissionCallbacks : ActivityCompat.OnRequestPermissionsResultCallback {

        fun onPermissionsGranted(requestCode: Int, perms: List<String>)

        fun onPermissionsDenied(requestCode: Int, perms: List<String>)

    }

    fun checkAllPermissions(activity: Activity?): Boolean {
        if (activity == null) {
            return false
        }

        val permisstions = HashSet<String>()

        if (!checkPermissions(activity, Manifest.permission.CAMERA)) {
            permisstions.add(Manifest.permission.CAMERA)
        }
        if (!checkPermissions(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permisstions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permisstions.size > 0) {
            val permisstionArray = permisstions.toTypedArray()
            if (Build.VERSION.SDK_INT >= 23) {
                activity.requestPermissions(permisstionArray, 9999)
            }
        }

        return permisstions.size == 0
    }

    /**
     * 检查是否有应用权限
     *
     * @param permission 权限名称
     *
     * @return 是否有应用权限
     */
    fun checkPermissions(context: Context?, permission: String): Boolean {
        var flag = false
        if (!TextUtils.isEmpty(permission) && context != null) {
            try {
                // SDK版本判断
                if (Build.VERSION.SDK_INT >= 23) {
                    // android 6.0使用新检查方法
                    val hasPer = ContextCompat.checkSelfPermission(context, permission)
                    if (hasPer == PackageManager.PERMISSION_GRANTED) {
                        flag = true
                    }
                } else {
                    val hasPer = context.checkCallingOrSelfPermission(permission)
                    if (hasPer == PackageManager.PERMISSION_GRANTED) {
                        flag = true
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
        return flag
    }

    /**
     * 请求应用权限
     *
     * @param requestCode 请求标识
     * @param permission  权限名称
     */
    fun requestPermissions(activity: Activity?, requestCode: Int, permission: String) {
        if (!TextUtils.isEmpty(permission) && activity != null) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    // 检查是否有权限
                    val hasPer = activity.checkSelfPermission(permission)
                    if (hasPer != PackageManager.PERMISSION_GRANTED) {
                        // 是否应该显示权限请求
                        // boolean isShould = getActivity().shouldShowRequestPermissionRationale(permission);
                        activity.requestPermissions(arrayOf(permission), requestCode)

                    }
                } else {
                    // android 6.0以下版本之间返回请求失败结果
                    // 不进行权限检查
                    onSelfRequestPermissionsResult(requestCode, arrayOf(permission),
                            intArrayOf(PackageManager.PERMISSION_DENIED))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    /**
     * 请求应用权限
     *
     * @param requestCode 请求标识
     * @param permission  权限名称
     */
    fun requestPermissions(fragment: Fragment?, requestCode: Int, permission: String) {
        if (!TextUtils.isEmpty(permission) && fragment != null) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    // 检查是否有权限
                    val hasPer = fragment.activity!!.checkSelfPermission(permission)
                    if (hasPer != PackageManager.PERMISSION_GRANTED) {
                        // 是否应该显示权限请求
                        // boolean isShould = getActivity().shouldShowRequestPermissionRationale(permission);
                        fragment.requestPermissions(arrayOf(permission), requestCode)

                    }
                } else {
                    // android 6.0以下版本之间返回请求失败结果
                    // 不进行权限检查
                    onSelfRequestPermissionsResult(requestCode, arrayOf(permission),
                            intArrayOf(PackageManager.PERMISSION_DENIED))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }


    /**
     * 请求应用权限数组
     *
     * @param requestCode 请求标识
     * @param permission  权限名称
     */
    fun requestArrayPermissions(activity: Activity?, requestCode: Int, permission: Array<String>?) {
        if (permission != null && permission.size > 0 && activity != null) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    // 检查是否有权限
                    for (i in permission.indices) {
                        if (permission[i] == null) {
                            break
                        }
                        val hasPer = activity.checkSelfPermission(permission[i])
                        if (hasPer != PackageManager.PERMISSION_GRANTED) {
                            activity.requestPermissions(permission, requestCode)
                            break
                        }
                    }
                } else {
                    // android 6.0以下版本之间返回请求失败结果
                    // 不进行权限检查
                    onSelfRequestPermissionsResult(requestCode, permission,
                            intArrayOf(PackageManager.PERMISSION_DENIED))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    @TargetApi(23)
    internal fun executePermissionsRequest(`object`: Any, perms: Array<String>, requestCode: Int) {
        checkCallingObjectSuitability(`object`)
        if (`object` is android.app.Activity) {
            ActivityCompat.requestPermissions(`object`, perms, requestCode)
        } else if (`object` is android.support.v4.app.Fragment) {
            `object`.requestPermissions(perms, requestCode)
        } else if (`object` is android.app.Fragment) {
            `object`.requestPermissions(perms, requestCode)
        }
    }

    private fun checkCallingObjectSuitability(`object`: Any?) {
        if (`object` == null) {
            throw NullPointerException("Activity or Fragment should not be null")
        }
        // Make sure Object is an Activity or Fragment
        val isActivity = `object` is android.app.Activity
        val isSupportFragment = `object` is android.support.v4.app.Fragment
        val isAppFragment = `object` is android.app.Fragment
        val isMinSdkM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        if (!(isSupportFragment || isActivity || isAppFragment && isMinSdkM)) {
            if (isAppFragment) {
                throw IllegalArgumentException(
                        "Target SDK needs to be greater than 23 if caller is android.app.Fragment")
            } else {
                throw IllegalArgumentException("Caller must be an Activity or a Fragment.")
            }
        }
    }

    /**
     * 子界面权限请求授权结果回调
     *
     * @param rc     请求Code
     * @param pers   权限数组
     * @param result 授权结果
     *
     * @return 是否调用父类方法
     */
    fun onSelfRequestPermissionsResult(rc: Int, pers: Array<String>, result: IntArray): Boolean {
        return false
    }

    /**
     * Handle the result of a permission request, should be called from the calling Activity's [ .OnRequestPermissionsResultCallback#onRequestPermissionsResult(int, String[], int[])][android.support.v4.app.ActivityCompat] method.
     *
     *
     * If any permissions were granted or denied, the `object` will receive the appropriate callbacks through [PermissionCallbacks] and
     * methods annotated with [AfterPermissionGranted] will be run if appropriate.
     *
     * @param requestCode
     * requestCode argument to permission result callback.
     * @param permissions
     * permissions argument to permission result callback.
     * @param grantResults
     * grantResults argument to permission result callback.
     * @param receivers
     * an array of objects that have a method annotated with [AfterPermissionGranted] or implement [PermissionCallbacks].
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                   grantResults: IntArray,
                                   vararg receivers: Any) {

        // Make a collection of granted and denied permissions from the request.
        val granted = ArrayList<String>()
        val denied = ArrayList<String>()
        for (i in permissions.indices) {
            val perm = permissions[i]
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm)
            } else {
                denied.add(perm)
            }
        }

        // iterate through all receivers
        for (`object` in receivers) {
            // Report granted permissions, if any.
            if (!granted.isEmpty()) {
                if (`object` is PermissionCallbacks) {
                    `object`.onPermissionsGranted(requestCode, granted)
                }
            }

            // Report denied permissions, if any.
            if (!denied.isEmpty()) {
                if (`object` is PermissionCallbacks) {
                    `object`.onPermissionsDenied(requestCode, denied)
                }
            }

        }

    }

}
