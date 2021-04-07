package com.shiyinghan.mqttdemo.base;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.shiyinghan.mqttdemo.R;
import com.shiyinghan.mqttdemo.event.DummyEvent;
import com.shiyinghan.mqttdemo.reactivex.RxUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;


/**
 * @author admin
 */
public abstract class AbstractActivity extends AppCompatActivity {

    protected CompositeDisposable mCompositeDisposable;
    private ViewGroup mRootViewGroup;
    private View mFlLoading;

    protected Handler mHandler;

    protected ImageView ivTitleBack;
    protected TextView tvTitleText;

    /**
     * 初始化视图绑定的抽象函数
     */
    protected abstract void initBinding();

    /**
     * 这里处理业务逻辑
     * 在onCreateView中
     */
    protected abstract void processLogic();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler(getMainLooper());

        initBinding();
        processLogic();
        initCommonViews();

        setAutoCloseKeyBoard();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(DummyEvent event) {
    }

    protected void initCommonViews() {
        ivTitleBack = findViewById(R.id.ivTitleBack);
        tvTitleText = findViewById(R.id.tvTitleText);
        if (ivTitleBack != null) {
            ivTitleBack.setOnClickListener(v -> {
                onBack();
            });
        }
        String title = getTitle().toString();
        if (tvTitleText != null && !TextUtils.isEmpty(title)) {
            tvTitleText.setText(title);
        }
    }

    protected void onBack() {
        finish();
    }

    public void setLoadingVisible(boolean isShow) {
        runOnUiThread(() -> {
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
            mRootViewGroup = (ViewGroup) this.getWindow().peekDecorView();
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

    /**
     * 获取根View
     *
     * @return
     */
    public View getRootView() {
        return ((ViewGroup) this.findViewById(android.R.id.content))
                .getChildAt(0);
    }

    /**
     * 设置自动关闭软键盘
     */
    protected void setAutoCloseKeyBoard() {
        View rootView = getRootView();
        if (rootView != null) {
            setAutoCloseKeyBoard(rootView);
        }
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
        ((InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(this.getWindow().getDecorView()
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
