package com.shiyinghan.mqttdemo.adapter;

import android.content.res.Resources;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

import org.jetbrains.annotations.NotNull;

public class SubscriptionListRecyclerViewAdapter extends BaseQuickAdapter<SubscriptionEntity, BaseViewHolder> {

    private SubscriptionListClickListener mListener;

    public void setListener(SubscriptionListClickListener mListener) {
        this.mListener = mListener;
    }

    public SubscriptionListRecyclerViewAdapter() {
        super(R.layout.list_item_subscription);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, SubscriptionEntity entity) {
        Resources resources = getRecyclerView().getResources();

        TextView tvSubscriptionItemTopic = baseViewHolder.getView(R.id.tvSubscriptionItemTopic);
        tvSubscriptionItemTopic.setText(resources.getString(R.string.subscription_list_topic_text, entity.getTopic()));

        TextView tvSubscriptionItemQos = baseViewHolder.getView(R.id.tvSubscriptionItemQos);
        tvSubscriptionItemQos.setText(resources.getString(R.string.subscription_list_qos_text, entity.getQos()));

        TextView tvSubscriptionItemShowNotification = baseViewHolder.getView(R.id.tvSubscriptionItemShowNotification);
        tvSubscriptionItemShowNotification.setText(
                resources.getString(
                        R.string.subscription_list_notify_text,
                        entity.isEnableNotification() ? resources.getString(R.string.enabled) : resources.getString(R.string.disabled)));

        ImageView ivSubscriptionItemDelete = baseViewHolder.getView(R.id.ivSubscriptionItemDelete);
        ivSubscriptionItemDelete.setOnClickListener(v -> {
            mListener.onClickDeleteIcon(entity);
        });
    }

    public interface SubscriptionListClickListener {

        void onClickDeleteIcon(SubscriptionEntity entity);
    }
}
