package com.shiyinghan.mqtt.demo.base;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.shiyinghan.mqtt.demo.base.presenter.IBasePresenter;

/**
 * created at 2020/2/27
 *
 * @author admin
 */
public abstract class AbstractMvpActivity<T extends IBasePresenter> extends AbstractActivity {
    protected T mPresenter;

    /**
     * 初始化Presenter的抽象函数
     */
    protected abstract T initPresenter();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mPresenter = initPresenter();
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onDestroy() {
        if (mPresenter != null) {
            mPresenter.release();
        }
        super.onDestroy();
    }
}
