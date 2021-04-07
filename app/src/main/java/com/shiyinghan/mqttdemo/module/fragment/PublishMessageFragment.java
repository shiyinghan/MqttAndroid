package com.shiyinghan.mqttdemo.module.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.base.AbstractFragment;
import com.shiyinghan.mqttdemo.databinding.FragmentPublishMessageBinding;
import com.shiyinghan.mqttdemo.mqtt.MqttClientFactory;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.utils.ToastUtil;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PublishMessageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PublishMessageFragment extends AbstractFragment implements View.OnClickListener {

    private static final String TAG = PublishMessageFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_CONNECTION = "connection";

    private FragmentPublishMessageBinding mBinding;
    private ConnectionEntity mConnection;
    private MqttAndroidClient mClient;

    public PublishMessageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param connection ConnectionEntity.
     * @return A new instance of fragment HistoryListFragment.
     */
    public static PublishMessageFragment newInstance(ConnectionEntity connection) {
        PublishMessageFragment fragment = new PublishMessageFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONNECTION, connection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View initBinding() {
        mBinding = FragmentPublishMessageBinding.inflate(getLayoutInflater());
        return mBinding.getRoot();
    }

    @Override
    protected void processLogic() {
        if (getArguments() != null) {
            mConnection = (ConnectionEntity) getArguments().getSerializable(ARG_CONNECTION);
            mClient = MqttClientFactory.getClient(getActivity(), mConnection);
        }

        initListeners();
    }

    private void initListeners() {
        mBinding.btnPublish.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnPublish) {
            publishMessage();
        }
    }

    private void publishMessage() {
        String topic = mBinding.etTopic.getText().toString();
        String message = mBinding.etMessage.getText().toString();
        int qos = Integer.parseInt(getResources().getStringArray(R.array.qos_options)[mBinding.spinnerQos.getSelectedItemPosition()]);
        boolean isRetain = mBinding.switchRetain.isChecked();

        if (TextUtils.isEmpty(topic)) {
            ToastUtil.show(getString(R.string.publish_message_topic_input_tip));
            return;
        }

        if (TextUtils.isEmpty(message)) {
            ToastUtil.show(getString(R.string.publish_message_message_input_tip));
            return;
        }

        setLoadingVisible(true);

        try {
            mClient.publish(topic, message.getBytes(), qos, isRetain, this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    setLoadingVisible(false);
                    ToastUtil.show(getString(R.string.toast_publish_success, message, topic));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    setLoadingVisible(false);
                    ToastUtil.show(getString(R.string.toast_publish_failed, message, topic));
                }
            });
        } catch (Exception e) {
            setLoadingVisible(false);
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }
}