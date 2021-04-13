package com.shiyinghan.mqtt.demo.base;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shiyinghan.mqtt.demo.base.presenter.IBasePresenter;


/**
 * @author admin
 */
public abstract class AbstractMvpFragment<T extends IBasePresenter> extends AbstractFragment {
    protected T mPresenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mPresenter = initPresenter();

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        mPresenter.release();
        mPresenter = null;
        super.onDestroyView();
    }

    protected abstract T initPresenter();

}
