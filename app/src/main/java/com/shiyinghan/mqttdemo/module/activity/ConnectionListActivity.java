package com.shiyinghan.mqttdemo.module.activity;

import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.adapter.ConnectionListRecyclerViewAdapter;
import com.shiyinghan.mqttdemo.base.AbstractMvpActivity;
import com.shiyinghan.mqttdemo.common.Constant;
import com.shiyinghan.mqttdemo.databinding.ActivityConnectionListBinding;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.event.ConnectionEditEvent;
import com.shiyinghan.mqttdemo.module.contract.ConnectionListContract;
import com.shiyinghan.mqttdemo.module.presenter.ConnectionListPresenter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class ConnectionListActivity extends AbstractMvpActivity<ConnectionListPresenter>
        implements ConnectionListContract.View, android.view.View.OnClickListener {

    private ActivityConnectionListBinding mBinding;

    private ConnectionListRecyclerViewAdapter mAdapter;

    @Override
    protected ConnectionListPresenter initPresenter() {
        return new ConnectionListPresenter(this);
    }

    @Override
    protected void initBinding() {
        mBinding = ActivityConnectionListBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void processLogic() {
        setTitle(R.string.connection_list_title);
        mBinding.layoutHeader.ivTitleBack.setVisibility(android.view.View.INVISIBLE);

        refreshListData();

        initListeners();
    }

    private void refreshListData() {
        if (mAdapter == null) {
            initRecyclerView();
        }

        setLoadingVisible(true);
        mPresenter.getConnectionList();
    }

    private void initListeners() {
        mBinding.btnAddConnection.setOnClickListener(this);
    }

    private void initRecyclerView() {
        mAdapter = new ConnectionListRecyclerViewAdapter();
        mAdapter.setListener(new ConnectionListRecyclerViewAdapter.ConnectionListClickListener() {
            @Override
            public void onClickItem(ConnectionEntity entity) {
                Intent intent = new Intent(ConnectionListActivity.this, ConnectionPortalActivity.class);
                intent.putExtra(Constant.DATA, entity);
                startActivity(intent);
            }

            @Override
            public void onClickEditIcon(ConnectionEntity entity) {
                Intent intent = new Intent(ConnectionListActivity.this, ConnectionEditActivity.class);
                intent.putExtra(Constant.DATA, entity);
                startActivity(intent);
            }

            @Override
            public void onClickDeleteIcon(ConnectionEntity entity) {
                showDeleteConnectionConfirmDialog(entity);
            }
        });

        RecyclerView recyclerView = mBinding.rvConnectionList;
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showDeleteConnectionConfirmDialog(ConnectionEntity connectionEntity) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_connection_tip)
                .setPositiveButton(R.string.dialog_positive_button, (dialog, which) -> {
                    setLoadingVisible(true);
                    mPresenter.deleteConnection(connectionEntity);
                }).setNegativeButton(R.string.dialog_negative_button, (dialog, which) -> {
        }).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ConnectionEditEvent event) {
        refreshListData();
    }

    @Override
    public void getConnectionListSuccess(List<ConnectionEntity> connections) {
        setLoadingVisible(false);
        mAdapter.setNewInstance(connections);
    }

    @Override
    public void getConnectionListFail() {
        setLoadingVisible(false);
    }

    @Override
    public void deleteConnectionSuccess() {
        setLoadingVisible(false);
        refreshListData();
    }

    @Override
    public void deleteConnectionFail() {
        setLoadingVisible(false);
        refreshListData();
    }

    @Override
    public void onClick(android.view.View v) {
        if (v.getId() == R.id.btnAddConnection) {
            Intent intent = new Intent(this, ConnectionEditActivity.class);
            startActivity(intent);
        }
    }

}