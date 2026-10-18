package com.mynote.android.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.mynote.android.R
import com.mynote.android.ui.main.MainActivity

/**
 * 图标样式入口小组件（对齐 MKB 项目的 AppIconWidget）：
 * 只作为打开 App 的快捷入口 —— 整块点击启动主应用，交互与桌面快捷方式图标一致。
 *
 * 所有支持尺寸（1×1 ~ 4×5+）都渲染同一套图标资源（app_icon_snapshot 玻璃书），
 * 由 ImageView fitCenter 按比例居中缩放：不切换布局、不变形、不裁切、不增减元素。
 *
 * 桌面小组件列表里的三个条目分别由本类 / [MyNoteIconWidgetLarge] / [MyNoteIconWidgetTall]
 * 注册，仅默认放置尺寸不同（2×2 / 4×4 / 4×5），外观与行为完全一致。
 */
open class MyNoteIconWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(ctx, mgr, id)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(ctx, mgr, id, newOptions)
        // 拉大/缩小后仍用同一布局重绘，图标自动等比缩放
        updateWidget(ctx, mgr, id)
    }

    companion object {
        /** 数据变化后由 EditActivity / MainActivity 联动刷新（三个预设条目都刷） */
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            for (cls in listOf(
                MyNoteIconWidget::class.java,
                MyNoteIconWidgetLarge::class.java,
                MyNoteIconWidgetTall::class.java
            )) {
                try {
                    for (id in mgr.getAppWidgetIds(ComponentName(ctx, cls))) {
                        updateWidget(ctx, mgr, id)
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun updateWidget(ctx: Context, mgr: AppWidgetManager, id: Int) {
            // 唯一布局：任何尺寸都不换
            val views = RemoteViews(ctx.packageName, R.layout.widget_icon_unified)

            // ── 整卡点击 → 打开主应用（自动走应用锁），无任何按钮/附加交互 ──
            val openPi = PendingIntent.getActivity(
                ctx, 2000,
                Intent(ctx, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_icon_unified_root, openPi)

            mgr.updateAppWidget(id, views)
        }
    }
}

/** 4×4 大图标预设条目（放置后仍可任意拉伸，外观与 2×2 完全一致） */
class MyNoteIconWidgetLarge : MyNoteIconWidget()

/** 4×5 高图标预设条目（放置后仍可任意拉伸，外观与 2×2 完全一致） */
class MyNoteIconWidgetTall : MyNoteIconWidget()
