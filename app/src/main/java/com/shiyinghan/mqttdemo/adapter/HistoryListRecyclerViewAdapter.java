package com.shiyinghan.mqttdemo.adapter;

import android.content.res.Resources;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.mqtt.entity.ReceivedMessageEntity;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class HistoryListRecyclerViewAdapter extends BaseQuickAdapter<ReceivedMessageEntity, BaseViewHolder> {

    private DateFormat mDateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    public HistoryListRecyclerViewAdapter() {
        super(R.layout.list_item_history);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, ReceivedMessageEntity entity) {
        Resources resources = getRecyclerView().getResources();

        TextView tvHistoryItemTopic = baseViewHolder.getView(R.id.tvHistoryItemTopic);
        tvHistoryItemTopic.setText(resources.getString(R.string.history_list_topic_fmt, entity.getTopic()));

        TextView tvHistoryItemMessage = baseViewHolder.getView(R.id.tvHistoryItemMessage);
        tvHistoryItemMessage.setText(new String(entity.getMessage().getPayload()));

        TextView tvHistoryItemTime = baseViewHolder.getView(R.id.tvHistoryItemTime);
        tvHistoryItemTime.setText(resources.getString(R.string.history_list_time_fmt, mDateFormat.format(entity.getTimestamp())));
    }
}
