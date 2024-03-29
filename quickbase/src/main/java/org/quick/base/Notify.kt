@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package org.quick.base

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BADGE_ICON_NONE
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.Serializable


/**
 * @describe 快速使用通知
 * @author ChrisZou
 * @date 2018/08/02-14:33
 * @from https://github.com/SpringSmell/quick.library
 * @email chrisSpringSmell@gmail.com
 */
object Notify {
    val actionShortcut = "com.android.launcher.ACTION.INSTALL_SHORTCUT"
    val shortcutName = "shortcutName"
    val shortcutIcon = "shortcutIcon"
    val shortcutPackageName = "shortcutPackageName"
    val shortcutTargetName = "shortcutTargetName"

    val ACTION = "ACTION"
    val ACTION_CANCEL = "ACTION_CANCEL"
    val ACTION_CLICK = "ACTION_CLICK"
    val NOTIFY_ID = "NOTIFY_ID"

    val clickListeners: SparseArray<(context: Context, intent: Intent) -> Unit> by lazy { return@lazy SparseArray<((context: Context, intent: Intent) -> Unit)>() }
    val cancelListeners: SparseArray<(context: Context, intent: Intent) -> Unit> by lazy { return@lazy SparseArray<((context: Context, intent: Intent) -> Unit)>() }
    val notificationManager: NotificationManager by lazy {
        return@lazy QuickAndroid.applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                QuickAndroid.applicationContext.packageName,
                "通知消息",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.lightColor = Color.RED
            notificationManager.createNotificationChannel(channel)
        }
        val intentFilter = IntentFilter(ACTION)
        intentFilter.addAction(actionShortcut)
        QuickAndroid.applicationContext.registerReceiver(NotificationReceiver(), intentFilter)
    }

    private fun notify(
        builder: Builder
    ) {
        builder.run {
            val notifyBuilder = NotificationCompat.Builder(
                QuickAndroid.applicationContext,
                QuickAndroid.applicationContext.packageName
            )
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setVisibility(VISIBILITY_PRIVATE)
                .setOngoing(ongoing)/*是否正在通知（是否不可以取消）*/
                .setAutoCancel(autoCancel)
                .setDefaults(default)
                .setBadgeIconType(badgeIconType)
            if (progress != -1)
                notifyBuilder.setProgress(progressMax, progress, indeterminate)
            if (largeIcon != null)
                notifyBuilder.setLargeIcon(largeIcon)

            onClickListener?.run {
                notifyBuilder.setContentIntent(
                    PendingIntent.getBroadcast(
                        QuickAndroid.applicationContext,
                        notificationId,
                        intent
                        , PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                clickListeners.put(notificationId, onClickListener)
            }

            onCancelListener?.run {
                notifyBuilder.setDeleteIntent(
                    PendingIntent.getBroadcast(
                        QuickAndroid.applicationContext,
                        notificationId,
                        (intent.clone() as Intent).setAction(ACTION_CANCEL)
                        , PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                cancelListeners.put(notificationId, onCancelListener)
            }

            notificationManager.notify(notificationId, notifyBuilder.build())
        }
    }

    class Builder(var notificationId: Int) {
        internal var title: String = ""
        internal var content: String = ""
        internal var largeIcon: Bitmap? = null
        internal var smallIcon: Int = -1
        internal var visibility: Int = VISIBILITY_PRIVATE
        internal var ongoing: Boolean = false
        internal var autoCancel: Boolean = true
        internal var default: Int = NotificationCompat.DEFAULT_ALL
        internal var progressMax = 100
        internal var progress = -1
        internal var indeterminate: Boolean = true
        internal var badgeIconType: Int = BADGE_ICON_NONE
        internal var remoteViews: RemoteViews? = null
        internal var onClickListener: ((context: Context, intent: Intent) -> Unit)? = null
        internal var onCancelListener: ((context: Context, intent: Intent) -> Unit)? = null

        internal val intent: Intent by lazy {
            return@lazy Intent(ACTION_CLICK)
                .putExtra(NOTIFY_ID, notificationId)
                .setClass(QuickAndroid.applicationContext, NotificationReceiver::class.java)
        }

        fun onClickListener(onClickListener: ((context: Context, intent: Intent) -> Unit)) =
            also { this.onClickListener = onClickListener }

        fun onCancelListener(onCancelListener: ((context: Context, intent: Intent) -> Unit)) =
            also { this.onCancelListener = onCancelListener }

        fun content(@DrawableRes icon: Int, title: String, content: String): Builder {
            this.smallIcon = icon
            this.title = title
            this.content = content
            return this
        }

        fun largeIcon(bitmap: Bitmap): Builder {
            this.largeIcon = bitmap
            return this
        }

        /**
         * 通知的可见性，默认为：在所有屏幕上显示通知，锁屏上面会隐藏一部分
         * @see VISIBILITY_PRIVATE
         */
        fun visibility(visibility: Int): Builder {
            this.visibility = visibility
            return this
        }

        /**
         * 是否正在通知
         * @param ongoing true:正通知用户无法删除
         */
        fun ongoing(ongoing: Boolean): Builder {
            this.ongoing = ongoing
            return this
        }

        /**
         * 点击是否取消
         */
        fun autoCancel(autoCancel: Boolean): Builder {
            this.autoCancel = autoCancel
            return this
        }

        fun progress(max: Int, progress: Int, indeterminate: Boolean): Builder {
            this.progressMax = max
            this.progress = progress
            this.indeterminate = indeterminate
            return this
        }

        /**
         * 通知选项，默认使用
         * @see NotificationCompat.DEFAULT_ALL
         */
        fun defaults(default: Int): Builder {
            this.default = default
            return this
        }

        fun badgeIconType(badgeIconType: Int): Builder {
            this.badgeIconType = badgeIconType
            return this
        }

        fun remoteViews(remoteViews: RemoteViews) = also { this.remoteViews = remoteViews }

        fun addParams(key: String, value: Intent): Builder {
            intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: String): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Float): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Int): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Double): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Byte): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: CharSequence): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Boolean): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Long): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, vararg value: Short): Builder {
            if (value.size == 1) intent.putExtra(key, value[0]) else intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, value: ArrayList<String>): Builder {
            intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, value: Bundle): Builder {
            intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, value: Serializable): Builder {
            intent.putExtra(key, value)
            return this
        }

        fun addParams(key: String, value: Parcelable): Builder {
            intent.putExtra(key, value)
            return this
        }

        fun action() {
            notify(this)
        }
    }

    /**
     * 自定义通知-不接管任务操作，只显示通知
     */
    fun notify(notificationId: Int, builder: NotificationCompat.Builder) {
        notify(notificationId, builder, null, null, null)
    }

    /**
     * 自定义通知-接管回调事件
     */
    fun notify(
        notificationId: Int,
        builder: NotificationCompat.Builder,
        onNotificationListener: ((context: Context, intent: Intent) -> Unit)
    ) {
        val intentCancel = Intent(ACTION)
        intentCancel.putExtra(ACTION, ACTION_CANCEL)
        intentCancel.putExtra(NOTIFY_ID, notificationId)

        val intentClick = Intent(ACTION)
        intentCancel.putExtra(ACTION, ACTION_CLICK)
        intentCancel.putExtra(NOTIFY_ID, notificationId)
        notify(notificationId, builder, intentClick, intentCancel, onNotificationListener)
    }

    /**
     * 自定义通知-不接管任何事件
     *
     * 注意：如果需要使用自带的点击回调，请将在intent的action指定为{@link #ACTION }例如：Intent(Notify.ACTION)
     * 并且将通知ID添加进intent{@link #NOTIFY_ID},例如：intentCancel.putExtra(NOTIFY_ID, notificationId)
     *
     * 这里提供一个自定义通知View的写法
     * for example:
     * 1、先实例一个RemoteViews
     * val customLayout = RemoteViews(packageName, R.layout.app_download_notification)
     *     customLayout.setTextViewText(R.id.titleTv, model.title)
     *     customLayout.setImageViewResource(R.id.coverIv, model.cover)
     *     customLayout.setOnClickPendingIntent(R.id.downloadStatusContainer, pendingIntentCancel)
     * 2、将实例好的RemoteViews安装进通知
     *     NotificationCompat.Builder(this, packageName).setCustomContentView(customLayout)
     * 3、安装好的Builder进行通知即可
     *
     * @param onNotificationListener 如果自行实现点击事件可不传
     */
    fun notify(
        notificationId: Int,
        builder: NotificationCompat.Builder,
        intentClick: Intent?,
        intentCancel: Intent?,
        onNotificationListener: ((context: Context, intent: Intent) -> Unit)?
    ) {
        if (onNotificationListener != null) {
            builder.setDeleteIntent(
                PendingIntent.getBroadcast(
                    QuickAndroid.applicationContext,
                    notificationId,
                    if (intentCancel != null) {
                        intentCancel
                            .setAction(ACTION)
                            .putExtra(ACTION, ACTION_CANCEL)
                            .putExtra(NOTIFY_ID, notificationId)
                    } else
                        Intent(ACTION)
                            .putExtra(ACTION, ACTION_CANCEL)
                            .putExtra(NOTIFY_ID, notificationId)
                    , PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            builder.setContentIntent(
                PendingIntent.getBroadcast(
                    QuickAndroid.applicationContext,
                    notificationId,
                    if (intentClick != null) {
                        intentClick
                            .setAction(ACTION)
                            .putExtra(ACTION, ACTION_CLICK)
                            .putExtra(NOTIFY_ID, notificationId)
                    } else
                        Intent(ACTION)
                            .putExtra(ACTION, ACTION_CLICK)
                            .putExtra(NOTIFY_ID, notificationId)
                    , PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            clickListeners.put(notificationId, onNotificationListener)
        }
        notificationManager.notify(notificationId, builder.build())
    }

    /**
     * 创建桌面通知（快捷方式）
     * @param builder 配置信息
     * @param onNotificationListener 创建成功回调
     */
    fun notifyDesktopShortcut(
        builder: ShortcutBuilder,
        onNotificationListener: (context: Context, intent: Intent) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setupShortcutOAfter(
            builder,
            onNotificationListener
        ) else setupShortcutOBefore(builder, onNotificationListener)
    }

    /**
     * Android O 以后兼容
     * @param builder
     * @param onSuccessListener 创建成功回调
     */
    private fun setupShortcutOAfter(
        builder: ShortcutBuilder,
        onSuccessListener: (context: Context, intent: Intent) -> Unit
    ) {
        clickListeners.put(builder.targetName.hashCode(), onSuccessListener)
        val callbackIntent = shortcutCallbackBuild(
            Intent(
                QuickAndroid.applicationContext,
                NotificationReceiver::class.java
            ), builder
        )
        val pendingIntentCancel = PendingIntent.getBroadcast(
            QuickAndroid.applicationContext,
            0x0,
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val targetIntent = shortcutCallbackBuild(Intent(Intent.ACTION_VIEW), builder).setComponent(
            ComponentName(
                builder.packageName,
                builder.packageName + "." + builder.targetName
            )
        )
        targetIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        targetIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        val shortcutManagerCompat =
            ShortcutInfoCompat.Builder(QuickAndroid.applicationContext, builder.shortcutId)
                .setIcon(IconCompat.createWithAdaptiveBitmap(builder.shortcutIcon))
                .setShortLabel(builder.shortcutName)
                .setIntent(targetIntent)
                .setAlwaysBadged()
                .build()

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(QuickAndroid.applicationContext))
            if (ShortcutManagerCompat.requestPinShortcut(
                    QuickAndroid.applicationContext,
                    shortcutManagerCompat,
                    pendingIntentCancel.intentSender
                )
            )
            else Toast.show("添加失败")
        else Toast.show("设备不支持")
    }

    /**
     * Android O 以前
     * @param builder 配置内容
     */
    private fun setupShortcutOBefore(
        builder: ShortcutBuilder,
        onSuccessListener: (context: Context, intent: Intent) -> Unit
    ) {
        clickListeners.put(builder.targetName.hashCode(), onSuccessListener)

        val targetIntent = shortcutCallbackBuild(Intent(Intent.ACTION_MAIN), builder).setComponent(
            ComponentName(
                builder.packageName,
                builder.packageName + "." + builder.targetName
            )
        )
        targetIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        targetIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        val shortcutIntent = shortcutCallbackBuild(Intent(actionShortcut), builder)
        shortcutIntent.putExtra("duplicate", false)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, targetIntent)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, builder.shortcutName)
        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, builder.shortcutIcon)
        QuickAndroid.applicationContext.sendBroadcast(shortcutIntent)
    }

    /**
     * 快捷方式回调数据构建
     */
    private fun shortcutCallbackBuild(intent: Intent, builder: ShortcutBuilder): Intent {
        intent.putExtra(NOTIFY_ID, builder.targetName.hashCode())
        intent.putExtra(shortcutName, builder.shortcutName)
//        intent.putExtra(shortcutIcon, builder.shortcutIcon)/*8.0无法协带*/
        intent.putExtra(shortcutPackageName, builder.packageName)
        intent.putExtra(shortcutTargetName, builder.targetName)
        intent.putExtras(builder.dataBundle)
        return intent
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * @param shortcutId
     */
    class ShortcutBuilder(var shortcutId: String = "") {
        var packageName: String = ""
        var targetName: String = ""
        var shortcutName: String = ""
        lateinit var dataBundle: Bundle
        lateinit var shortcutIcon: Bitmap

        fun setActivity(packageName: String, targetName: String): ShortcutBuilder {
            setActivity(packageName, targetName, Bundle())
            return this
        }

        /**
         * example: 包路径为org.quick.component 需要跳转到ui下面的MainActivity，那此时总路径应当为org.quick.component.ui.MainActivity
         *
         * @param packageName 包路径
         * @param targetName 目标Activity路径
         * @param dataBundle 传递到目标的数据集合
         */
        fun setActivity(
            packageName: String,
            targetName: String,
            dataBundle: Bundle
        ): ShortcutBuilder {
            this.packageName = packageName
            this.targetName = targetName
            this.dataBundle = dataBundle
            return this
        }

        fun setShortcut(shortcutName: String, shortcutIcon: Bitmap): ShortcutBuilder {
            this.shortcutName = shortcutName
            this.shortcutIcon = shortcutIcon
            return this
        }

        /**
         * 创建桌面通知（快捷方式）
         * @param builder 配置信息
         * @param onNotificationListener 创建成功回调
         */
        fun notifyDesktopShortcut(
            builder: ShortcutBuilder,
            onNotificationListener: (context: Context, intent: Intent) -> Unit
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setupShortcutOAfter(
                builder,
                onNotificationListener
            ) else setupShortcutOBefore(builder, onNotificationListener)
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_CLICK)
                clickListeners.get(intent.getIntExtra(NOTIFY_ID, 0))?.invoke(context, intent)
            else
                cancelListeners.get(intent.getIntExtra(NOTIFY_ID, 0))?.invoke(context, intent)
        }
    }
}