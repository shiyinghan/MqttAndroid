package com.shiyinghan.mqtt.demo.module.activity;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqtt.demo.R;
import com.shiyinghan.mqtt.demo.base.AbstractMvpActivity;
import com.shiyinghan.mqtt.demo.common.Constant;
import com.shiyinghan.mqtt.demo.databinding.ActivityConnectionPortalBinding;
import com.shiyinghan.mqtt.demo.event.ConnectionLostEvent;
import com.shiyinghan.mqtt.demo.module.contract.ConnectionPortalContract;
import com.shiyinghan.mqtt.demo.module.fragment.HistoryListFragment;
import com.shiyinghan.mqtt.demo.module.fragment.PublishMessageFragment;
import com.shiyinghan.mqtt.demo.module.fragment.SubscriptionListFragment;
import com.shiyinghan.mqtt.demo.module.presenter.ConnectionPortalPresenter;
import com.shiyinghan.mqtt.demo.mqtt.MqttClientFactory;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class ConnectionPortalActivity extends AbstractMvpActivity<ConnectionPortalPresenter>
        implements ConnectionPortalContract.View, View.OnClickListener {

    private static final String TAG = ConnectionPortalActivity.class.getSimpleName();

    private static final int[] TITLE_ARRAY = {
            R.string.connection_history_tab,
            R.string.connection_publish_tab,
            R.string.connection_subscribe_tab
    };

    private ActivityConnectionPortalBinding mBinding;

    private ConnectionEntity mConnection;

    private MqttAndroidClient mClient;
    private MqttConnectOptions mOptions;

    private FragmentStateAdapter mViewPage2Adapter;

    @Override
    protected void initBinding() {
        mBinding = ActivityConnectionPortalBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

    @Override
    protected void processLogic() {
        mConnection = (ConnectionEntity) getIntent().getSerializableExtra(Constant.DATA);
        if (mConnection == null) {
            finish();
        }

        setTitle(mConnection.getClientId());

        mViewPage2Adapter = new MyFragmentStateAdapter(this);
        mBinding.vpConnectionPortalContent.setAdapter(mViewPage2Adapter);
        new TabLayoutMediator(mBinding.tlConnectionPortalTabs, mBinding.vpConnectionPortalContent, (tab, position) -> {
            tab.setText(TITLE_ARRAY[position]);
        }).attach();

        initListeners();

        initMqttClient();
    }

    private void initListeners() {
        mBinding.flConnectionConnect.setOnClickListener(this);
    }

    private void initMqttClient() {
        mClient = MqttClientFactory.getClient(this, mConnection);
        mOptions = MqttClientFactory.getConnectOptions(mConnection);

        if (mClient.isConnected()) {
            showConnectionStatus();
        } else {
            connect();
        }
    }

    @Override
    protected ConnectionPortalPresenter initPresenter() {
        return new ConnectionPortalPresenter(this);
    }

    @Override
    protected void onDestroy() {
        if (mClient != null) {
            try {
                mClient.disconnect();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }

            if (mConnection.isCleanSession()) {
                mPresenter.deleteSubscriptionList(mClient.getClientHandle());
            }
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.flConnectionConnect) {
            if (mClient.isConnected()) {
                disconnect();
            } else {
                connect();
            }
        }
    }

    private void connect() {
        showConnectingView(true);
        try {
            mClient.connect(mOptions, this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (mConnection.isCleanSession()) {
                        mPresenter.deleteSubscriptionList(mClient.getClientHandle());
                    } else {
                        mPresenter.getSubscriptionList(mClient.getClientHandle());
                    }

                    showConnectingView(false);
                    showConnectionStatus();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    showConnectingView(false);
                    showConnectionStatus();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    private void disconnect() {
        showConnectingView(true);
        try {
            mClient.disconnect(this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (mConnection.isCleanSession()) {
                        mPresenter.deleteSubscriptionList(mClient.getClientHandle());
                    }

                    showConnectingView(false);
                    showConnectionStatus();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (mConnection.isCleanSession()) {
                        mPresenter.deleteSubscriptionList(mClient.getClientHandle());
                    }

                    showConnectingView(false);
                    showConnectionStatus();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void getSubscriptionListSuccess(List<SubscriptionEntity> list) {
        if (list.size() == 0) {
            return;
        }

        String[] topicArray = new String[list.size()];
        int[] qosArray = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            topicArray[i] = list.get(i).getTopic();
            qosArray[i] = list.get(i).getQos();
        }

        try {
            mClient.unsubscribe(topicArray, this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    subscribeTopics(topicArray, qosArray);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    subscribeTopics(topicArray, qosArray);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void subscribeTopics(String[] topicArray, int[] qosArray) {
        try {
            mClient.subscribe(topicArray, qosArray);
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void getSubscriptionListFail() {
    }

    @Override
    public void deleteSubscriptionListSuccess() {

    }

    @Override
    public void deleteSubscriptionListFail() {

    }

    private void showConnectingView(boolean isShow) {
        mBinding.vConnectionConnectingBackground.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
    }

    private void showConnectionStatus() {
        // wait 100 milliseconds for MqttAndroidClient to update connection state
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mClient.isConnected()) {
                    mBinding.vConnectionActiveBackground.setVisibility(View.VISIBLE);
                    mBinding.vConnectionInActiveBackground.setVisibility(View.INVISIBLE);
                } else {
                    mBinding.vConnectionActiveBackground.setVisibility(View.INVISIBLE);
                    mBinding.vConnectionInActiveBackground.setVisibility(View.VISIBLE);
                }
            }
        }, 100);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionLostEvent(ConnectionLostEvent event) {
        showConnectionStatus();
    }

    class MyFragmentStateAdapter extends FragmentStateAdapter {

        public MyFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = HistoryListFragment.newInstance(mConnection);
                    break;
                case 1:
                    fragment = PublishMessageFragment.newInstance(mConnection);
                    break;
                case 2:
                    fragment = SubscriptionListFragment.newInstance(mConnection);
                    break;
                default:
                    break;
            }
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}