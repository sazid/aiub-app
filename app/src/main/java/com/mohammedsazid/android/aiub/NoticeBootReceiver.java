package com.mohammedsazid.android.aiub;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class NoticeBootReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        CheckNoticeService.startActionCheckNotice(context);
    }
}
