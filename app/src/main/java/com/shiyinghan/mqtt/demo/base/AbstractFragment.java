package com.shiyinghan.mqtt.demo.base;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.shiyinghan.mqtt.demo.R;
import com.shiyinghan.mqtt.demo.event.DummyEvent;
import com.shiyinghan.mqtt.demo.reactivex.RxUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;


/**
 * @author admin
 */
public abstract class AbstractFragment extends Fragment {

    /**
     * 管理所有的订阅
     */
    protected CompositeDisposable mCompositeDisposable;
    private ViewGroup mRootViewGroup;
    private View mFlLoading;

    /**
     * 初始化视图绑定的抽象函数
     *
     * @return fragment的根View
     */
    protected abstract View initBinding();

    /**
     * 这里处理业务逻辑
     * 在onCreateView中
     */
    protected abstract void processLogic();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(DummyEvent event) {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = initBinding();
        processLogic();

        setAutoCloseKeyBoard(rootView);

        return rootView;
    }

    protected <T> Disposable addSubscription(Observable<T> observable, DisposableObserver<T> observer) {
        return addSubscription(observable.compose(RxUtils.rxSchedulerHelper()).subscribeWith(observer));
    }

    protected <T> Disposable addSubscription(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
        return disposable;
    }

    @Override
    public void onDestroyView() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
        super.onDestroyView();
    }

    public void setLoadingVisible(boolean isShow) {
        getActivity().runOnUiThread(() -> {
            mFlLoading = getLoadingView();
            if (mFlLoading != null) {
                mFlLoading.setVisibility(isShow ? View.VISIBLE : View.GONE);
            } else {
                Log.e("Progress", "xml not find R.id.flLoading ");
            }
        });
    }

    protected View getLoadingView() {
        if (mFlLoading == null) {
            try {
                mFlLoading = getLayoutInflater().inflate(R.layout.loading_include, null);
                ViewGroup rootViewGroup = getRootViewGroup();
                rootViewGroup.addView(mFlLoading);
            } catch (Exception e) {
            }
        }
        return mFlLoading;
    }

    private ViewGroup getRootViewGroup() {
        ViewGroup viewGroup = null;
        if (mRootViewGroup != null) {
            viewGroup = mRootViewGroup;
        } else {
            mRootViewGroup = (ViewGroup) getActivity().getWindow().peekDecorView();
            if (mRootViewGroup == null) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                }
                mRootViewGroup = this.getRootViewGroup();
            }
            viewGroup = mRootViewGroup;
        }

        return viewGroup;
    }


    /**
     * 设置自动关闭软键盘
     */
    public void setAutoCloseKeyBoard(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnTouchListener(new AutoCloseKeyBoardListener());
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftInput() {
        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getActivity().getWindow().getDecorView()
                        .getWindowToken(), 0);
    }

    /**
     * 自动关闭软键盘监听
     */
    public class AutoCloseKeyBoardListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!(v instanceof EditText)) {
                hideSoftInput();
            }
            return false;
        }
    }
}
