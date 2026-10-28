package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tuvin.pixelautomations.MainActivity
import com.tuvin.pixelautomations.R
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation

object Notifications {
    const val SERVICE = "service"
    const val CONFIRM = "confirm"
    const val RAN = "ran"
    const val FROM_ACTIONS = "actions"

    const val SERVICE_ID = 1
    const val CAPTURE_ID = 2

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(SERVICE, t("Watching for triggers"), NotificationManager.IMPORTANCE_MIN),
                NotificationChannel(CONFIRM, t("Run automation?"), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(RAN, t("Automation ran"), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(FROM_ACTIONS, t("Notifications from automations"), NotificationManager.IMPORTANCE_HIGH),
            )
        )
    }

    /** Shown while CaptureService grabs location/photo/audio (a foreground service must show something). */
    fun capture(ctx: Context): Notification =
        Notification.Builder(ctx, SERVICE)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(t("Automation running"))
            .build()

    fun service(ctx: Context): Notification =
        Notification.Builder(ctx, SERVICE)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(t("Automations are on"))
            .setContentIntent(openApp(ctx))
            .setOngoing(true)
            .build()

    /** iOS "Run After Confirmation": tap the notification (or Run) to run. */
    fun askToRun(ctx: Context, a: Automation, why: String) {
        val run = PendingIntent.getBroadcast(
            ctx, a.id.toInt(),
            Intent(ctx, ConfirmReceiver::class.java).putExtra(ConfirmReceiver.EXTRA_ID, a.id).putExtra(ConfirmReceiver.EXTRA_WHY, why),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = Notification.Builder(ctx, CONFIRM)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(t("Run “%s”?", a.title))
            .setContentText(why)
            .setContentIntent(run)
            .addAction(Notification.Action.Builder(null, t("Run"), run).build())
            .setAutoCancel(true)
            .build()
        nm(ctx).notify(confirmId(a.id), n)
    }

    fun cancelAsk(ctx: Context, id: Long) = nm(ctx).cancel(confirmId(id))

    /** iOS "Choose from Menu": one button per option (Android shows up to 3). */
    fun menu(ctx: Context, a: Automation, index: Int, menu: Action.ChooseFromMenu, token: Int) {
        val b = Notification.Builder(ctx, CONFIRM)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(menu.prompt.ifBlank { a.title })
            .setContentText(a.title)
            .setAutoCancel(true)
        menu.options.take(3).forEachIndexed { option, o ->
            val pi = PendingIntent.getBroadcast(
                ctx, token * 10 + option,
                Intent(ctx, MenuReceiver::class.java)
                    .putExtra(MenuReceiver.EXTRA_TOKEN, token)
                    .putExtra(MenuReceiver.EXTRA_OPTION, option),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            b.addAction(Notification.Action.Builder(null, o.label, pi).build())
        }
        nm(ctx).notify(menuId(token), b.build())
    }

    fun cancelMenu(ctx: Context, token: Int) = nm(ctx).cancel(menuId(token))

    private fun menuId(token: Int) = 400_000 + token

    /** iOS "Notify When Run". */
    fun ran(ctx: Context, a: Automation, result: String) {
        val n = Notification.Builder(ctx, RAN)
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle(t("Ran “%s”", a.title))
            .setContentText(result)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        nm(ctx).notify(ranId(a.id), n)
    }

    private fun confirmId(id: Long) = 100_000 + id.toInt()
    private fun ranId(id: Long) = 200_000 + id.toInt()

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    private fun openApp(ctx: Context) = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
}
