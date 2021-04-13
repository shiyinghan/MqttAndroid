package com.shiyinghan.mqtt.demo.module.activity;

import android.text.TextUtils;

import com.shiyinghan.mqtt.demo.R;
import com.shiyinghan.mqtt.demo.base.AbstractMvpActivity;
import com.shiyinghan.mqtt.demo.common.Constant;
import com.shiyinghan.mqtt.demo.databinding.ActivityConnectionEditBinding;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqtt.demo.event.ConnectionEditEvent;
import com.shiyinghan.mqtt.demo.module.contract.ConnectionEditContract;
import com.shiyinghan.mqtt.demo.module.presenter.ConnectionEditPresenter;
import com.shiyinghan.mqtt.demo.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

public class ConnectionEditActivity extends AbstractMvpActivity<ConnectionEditPresenter>
        implements ConnectionEditContract.View, android.view.View.OnClickListener {

    private ActivityConnectionEditBinding mBinding;

    private ConnectionEntity mConnectionEntity;

    @Override
    protected ConnectionEditPresenter initPresenter() {
        return new ConnectionEditPresenter(this);
    }

    @Override
    protected void initBinding() {
        mBinding = ActivityConnectionEditBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void processLogic() {
        if (getIntent().hasExtra(Constant.DATA)) {
            setTitle(R.string.edit_connection_title);
            mConnectionEntity = (ConnectionEntity) getIntent().getSerializableExtra(Constant.DATA);
            initConnectionInfo();
        } else {
            setTitle(R.string.add_connection_title);
            mConnectionEntity = new ConnectionEntity();
        }

        initListeners();
    }

    private void initConnectionInfo() {
        mBinding.etClientId.setText(mConnectionEntity.getClientId());
        mBinding.etServer.setText(mConnectionEntity.getServer());
        mBinding.etPort.setText(String.valueOf(mConnectionEntity.getPort()));
        mBinding.switchCleanSession.setChecked(mConnectionEntity.isCleanSession());
        mBinding.etUsername.setText(mConnectionEntity.getUsername());
        mBinding.etPassword.setText(mConnectionEntity.getPassword());
        mBinding.etTimeout.setText(String.valueOf(mConnectionEntity.getTimeout()));
        mBinding.etKeepAlive.setText(String.valueOf(mConnectionEntity.getKeepAlive()));
        mBinding.etLwtTopic.setText(mConnectionEntity.getLwtTopic());
        mBinding.etLwtMessage.setText(mConnectionEntity.getLwtMessage());
        mBinding.spinnerLwtQos.setSelection(Arrays.asList(getResources().getStringArray(R.array.qos_options))
                .indexOf(String.valueOf(mConnectionEntity.getLwtQos())));
        mBinding.switchLwtRetain.setChecked(mConnectionEntity.isLwtRetain());
    }

    private void initListeners() {
        mBinding.btnConfirm.setOnClickListener(this);
    }

    @Override
    public void saveConnectionSuccess() {
        setLoadingVisible(false);
        EventBus.getDefault().post(new ConnectionEditEvent());
        finish();
    }

    @Override
    public void saveConnectionFail() {
        setLoadingVisible(false);
        EventBus.getDefault().post(new ConnectionEditEvent());
        finish();
    }

    @Override
    public void onClick(android.view.View v) {
        if (v.getId() == R.id.btnConfirm) {
            saveConnection();
        }
    }

    private void saveConnection() {
        if (TextUtils.isEmpty(mBinding.etClientId.getText().toString())) {
            ToastUtil.show(getString(R.string.connection_client_id_input_tip));
            mBinding.etClientId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mBinding.etServer.getText().toString())) {
            ToastUtil.show(getString(R.string.connection_server_input_tip));
            mBinding.etServer.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mBinding.etPort.getText().toString())) {
            ToastUtil.show(getString(R.string.connection_port_input_tip));
            mBinding.etPort.requestFocus();
            return;
        }

        mConnectionEntity.setClientId(mBinding.etClientId.getText().toString());
        mConnectionEntity.setServer(mBinding.etServer.getText().toString());
        mConnectionEntity.setPort(Integer.parseInt(mBinding.etPort.getText().toString()));
        mConnectionEntity.setCleanSession(mBinding.switchCleanSession.isChecked());
        mConnectionEntity.setUsername(mBinding.etUsername.getText().toString());
        mConnectionEntity.setPassword(mBinding.etPassword.getText().toString());
        mConnectionEntity.setTimeout(Integer.parseInt(mBinding.etTimeout.getText().toString()));
        mConnectionEntity.setKeepAlive(Integer.parseInt(mBinding.etKeepAlive.getText().toString()));
        mConnectionEntity.setLwtTopic(mBinding.etLwtTopic.getText().toString());
        mConnectionEntity.setLwtMessage(mBinding.etLwtMessage.getText().toString());
        mConnectionEntity.setLwtQos(Integer.parseInt(getResources().getStringArray(R.array.qos_options)[mBinding.spinnerLwtQos.getSelectedItemPosition()]));
        mConnectionEntity.setLwtRetain(mBinding.switchLwtRetain.isChecked());

        setLoadingVisible(true);
        mPresenter.saveConnection(mConnectionEntity);
    }
}