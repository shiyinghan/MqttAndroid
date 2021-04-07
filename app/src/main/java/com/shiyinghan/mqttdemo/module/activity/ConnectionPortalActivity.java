package com.shiyinghan.mqttdemo.module.activity;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.base.AbstractMvpActivity;
import com.shiyinghan.mqttdemo.common.Constant;
import com.shiyinghan.mqttdemo.databinding.ActivityConnectionPortalBinding;
import com.shiyinghan.mqttdemo.event.ConnectionLostEvent;
import com.shiyinghan.mqttdemo.module.contract.ConnectionPortalContract;
import com.shiyinghan.mqttdemo.module.fragment.HistoryListFragment;
import com.shiyinghan.mqttdemo.module.fragment.PublishMessageFragment;
import com.shiyinghan.mqttdemo.module.fragment.SubscriptionListFragment;
import com.shiyinghan.mqttdemo.module.presenter.ConnectionPortalPresenter;
import com.shiyinghan.mqttdemo.mqtt.MqttClientFactory;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

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
        super.onDestroy();
        if (mClient != null) {
            try {
                mClient.disconnect();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
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
                    mPresenter.getSubscriptionList(mClient.getClientHandle());

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

    @Override
    public void getSubscriptionListSuccess(List<SubscriptionEntity> list) {
        try {
            for (SubscriptionEntity entity : list) {
                mClient.subscribe(entity.getTopic(), entity.getQos());
            }
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void getSubscriptionListFail() {
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