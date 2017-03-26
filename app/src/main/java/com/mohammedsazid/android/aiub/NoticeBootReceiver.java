package com.mohammedsazid.android.aiub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NoticeBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CheckNoticeService.startActionCheckNotice(context);
    }
}
