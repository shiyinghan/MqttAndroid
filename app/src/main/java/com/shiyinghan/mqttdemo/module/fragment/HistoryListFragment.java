package com.shiyinghan.mqttdemo.module.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.View;

import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.adapter.HistoryListRecyclerViewAdapter;
import com.shiyinghan.mqttdemo.base.AbstractFragment;
import com.shiyinghan.mqttdemo.databinding.FragmentHistoryListBinding;
import com.shiyinghan.mqttdemo.event.MessageArrivedEvent;
import com.shiyinghan.mqttdemo.mqtt.MqttClientFactory;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HistoryListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryListFragment extends AbstractFragment implements View.OnClickListener {

    // the fragment initialization parameters
    private static final String ARG_CONNECTION = "connection";

    private FragmentHistoryListBinding mBinding;
    private ConnectionEntity mConnection;
    private MqttAndroidClient mClient;

    private HistoryListRecyclerViewAdapter mListAdapter;

    public HistoryListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param connection ConnectionEntity.
     * @return A new instance of fragment HistoryListFragment.
     */
    public static HistoryListFragment newInstance(ConnectionEntity connection) {
        HistoryListFragment fragment = new HistoryListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONNECTION, connection);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View initBinding() {
        mBinding = FragmentHistoryListBinding.inflate(getLayoutInflater());
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
    }

    private void initListeners() {
        mBinding.btnClear.setOnClickListener(this);
    }

    private void initRecyclerList() {
        mListAdapter = new HistoryListRecyclerViewAdapter();

        mBinding.rvHistoryList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.rvHistoryList.setAdapter(mListAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageArrivedEvent(MessageArrivedEvent event) {
        mListAdapter.addData(0, event.getReceivedMessage());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnClear) {
            mListAdapter.setNewInstance(null);
        }
    }
}