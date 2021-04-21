package com.shiyinghan.mqtt.demo.module.fragment;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqtt.demo.R;
import com.shiyinghan.mqtt.demo.adapter.SubscriptionListRecyclerViewAdapter;
import com.shiyinghan.mqtt.demo.base.AbstractMvpFragment;
import com.shiyinghan.mqtt.demo.databinding.DialogSubscriptionAddBinding;
import com.shiyinghan.mqtt.demo.databinding.FragmentSubscriptionListBinding;
import com.shiyinghan.mqtt.demo.module.contract.SubscriptionListContract;
import com.shiyinghan.mqtt.demo.module.presenter.SubscriptionListPresenter;
import com.shiyinghan.mqtt.demo.mqtt.MqttClientFactory;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;
import com.shiyinghan.mqtt.demo.utils.ToastUtil;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SubscriptionListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SubscriptionListFragment extends AbstractMvpFragment<SubscriptionListPresenter>
        implements SubscriptionListContract.View, android.view.View.OnClickListener {
    private static final String TAG = SubscriptionListFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_CONNECTION = "connection";

    private FragmentSubscriptionListBinding mBinding;
    private ConnectionEntity mConnection;
    private MqttAndroidClient mClient;

    private SubscriptionListRecyclerViewAdapter mListAdapter;

    private DialogSubscriptionAddBinding mDialogBinding;

    public SubscriptionListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param connection ConnectionEntity.
     * @return A new instance of fragment HistoryListFragment.
     */
    public static SubscriptionListFragment newInstance(ConnectionEntity connection) {
        SubscriptionListFragment fragment = new SubscriptionListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONNECTION, connection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected SubscriptionListPresenter initPresenter() {
        return new SubscriptionListPresenter(this);
    }

    @Override
    protected android.view.View initBinding() {
        mBinding = FragmentSubscriptionListBinding.inflate(getLayoutInflater());
        return mBinding.getRoot();
    }

    @Override
    protected void processLogic() {
        if (getArguments() != null) {
            mConnection = (ConnectionEntity) getArguments().getSerializable(ARG_CONNECTION);
            mClient = MqttClientFactory.getClient(getActivity(), mConnection);
        }
        initListeners();

        initRecyclerList();

        setLoadingVisible(true);
        mPresenter.getSubscriptionListObservable(mClient.getClientHandle());
    }

    private void initListeners() {
        mBinding.btnNewSubscription.setOnClickListener(this);
    }

    private void initRecyclerList() {
        mListAdapter = new SubscriptionListRecyclerViewAdapter();
        mListAdapter.setListener(entity -> {
            setLoadingVisible(true);
            mPresenter.deleteSubscription(entity);
        });

        mBinding.rvSubscriptionList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.rvSubscriptionList.setAdapter(mListAdapter);
    }

    @Override
    public void onClick(android.view.View v) {
        if (v.getId() == R.id.btnNewSubscription) {
            showSubscriptionNewDialog();
        }
    }

    private void showSubscriptionNewDialog() {
        mDialogBinding = DialogSubscriptionAddBinding.inflate(getLayoutInflater());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setView(mDialogBinding.getRoot())
                .setCancelable(true)
                .setPositiveButton(R.string.subscription_ok_button, (dialog, which) -> {
                    saveSubscription();
                })
                .setNegativeButton(R.string.subscription_cancel_button, (dialog, which) -> {
                    dialog.cancel();
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void saveSubscription() {
        String topic = mDialogBinding.etSubscriptionTopic.getText().toString();
        int qos = Integer.parseInt(getResources().getStringArray(R.array.qos_options)[mDialogBinding.spinnerSubscriptionQos.getSelectedItemPosition()]);
        boolean enableNotification = mDialogBinding.switchSubscriptionShowNotification.isChecked();

        if (TextUtils.isEmpty(topic)) {
            ToastUtil.show(getString(R.string.subscription_topic_input_tip));
            return;
        }

        setLoadingVisible(true);

        SubscriptionEntity entity = new SubscriptionEntity(mClient.getClientHandle(), topic, qos, enableNotification);
        mPresenter.saveSubscription(entity);
    }

    @Override
    public void getSubscriptionListObservableSuccess(List<SubscriptionEntity> list) {
        setLoadingVisible(false);
        mListAdapter.setNewInstance(list);
    }

    @Override
    public void getSubscriptionListObservableFail() {
        setLoadingVisible(false);
    }

    @Override
    public void saveSubscriptionSuccess(SubscriptionEntity entity) {
        try {
            mClient.subscribe(entity.getTopic(), entity.getQos(), this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ToastUtil.show(getString(R.string.toast_subscribe_success, entity.getTopic()));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    ToastUtil.show(getString(R.string.toast_subscribe_failed, entity.getTopic()));
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void saveSubscriptionFail() {
        setLoadingVisible(false);
    }

    @Override
    public void deleteSubscriptionSuccess(SubscriptionEntity entity) {
        try {
            mClient.unsubscribe(entity.getTopic(), this, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    ToastUtil.show(getString(R.string.toast_unsubscribe_success, entity.getTopic()));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, exception.getLocalizedMessage(), exception);
                    ToastUtil.show(getString(R.string.toast_unsubscribe_failed, entity.getTopic()));
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void deleteSubscriptionFail() {
        mPresenter.getSubscriptionListObservable(mClient.getClientHandle());
    }
}