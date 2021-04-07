package com.shiyinghan.mqttdemo.adapter;

import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;

import org.jetbrains.annotations.NotNull;

public class ConnectionListRecyclerViewAdapter extends BaseQuickAdapter<ConnectionEntity, BaseViewHolder> {

    private ConnectionListClickListener mListener;

    public void setListener(ConnectionListClickListener mListener) {
        this.mListener = mListener;
    }

    public ConnectionListRecyclerViewAdapter() {
        super(R.layout.list_item_connection);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, ConnectionEntity connectionEntity) {
        TextView tvConnectionRowTitle = baseViewHolder.getView(R.id.tvConnectionRowTitle);
        tvConnectionRowTitle.setText(connectionEntity.getClientId());

        ImageView ivConnectionRowDeleteIcon = baseViewHolder.getView(R.id.ivConnectionRowDeleteIcon);
        ImageView ivConnectionRowEditIcon = baseViewHolder.getView(R.id.ivConnectionRowEditIcon);

        baseViewHolder.itemView.setOnClickListener(v -> mListener.onClickItem(connectionEntity));
        ivConnectionRowDeleteIcon.setOnClickListener(v -> mListener.onClickDeleteIcon(connectionEntity));
        ivConnectionRowEditIcon.setOnClickListener(v -> mListener.onClickEditIcon(connectionEntity));
    }

    public interface ConnectionListClickListener {
        void onClickItem(ConnectionEntity entity);

        void onClickEditIcon(ConnectionEntity entity);

        void onClickDeleteIcon(ConnectionEntity entity);
    }
}
