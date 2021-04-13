package com.shiyinghan.mqtt.demo.base.presenter;


import android.content.SharedPreferences;

import com.shiyinghan.mqtt.demo.reactivex.RxUtils;
import com.shiyinghan.mqtt.demo.utils.SharedPreferencesUtil;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;


/**
 * @author admin
 */
public abstract class BasePresenter<T> implements IBasePresenter {

    /**
     * 管理所有的订阅
     */
    protected CompositeDisposable mCompositeDisposable;
    protected T mView;
    protected SharedPreferences mSharedPreferences;

    public BasePresenter(T view) {
        mView = view;
        mSharedPreferences = SharedPreferencesUtil.getSharedPreferences();
        mCompositeDisposable = new CompositeDisposable();
    }

    /**
     * Presenter释放资源
     */
    @Override
    public void release() {
        clearSubscription();

        if (mView != null) {
            mView = null;
        }
    }

    protected <D> Disposable addSubscription(Observable<D> observable, DisposableObserver<D> Observer) {
        return addSubscription(observable.compose(RxUtils.rxSchedulerHelper()).subscribeWith(Observer));
    }

    protected <D> Disposable addSubscription(Maybe<D> maybe, DisposableMaybeObserver<D> Observer) {
        return addSubscription(maybe.compose(RxUtils.rxMaybeSchedulerHelper()).subscribeWith(Observer));
    }

    protected <D> Disposable addSubscription(Completable completable, DisposableCompletableObserver Observer) {
        return addSubscription(completable.compose(RxUtils.rxCompletableSchedulerHelper()).subscribeWith(Observer));
    }

    protected <D> Disposable addSubscription(Single<D> observable, DisposableSingleObserver<D> observer) {
        return addSubscription(observable.compose(RxUtils.rxSingleSchedulerHelper()).subscribeWith(observer));
    }

    protected Disposable addSubscription(Disposable disposable) {
        if (mCompositeDisposable == null) {
            mCompositeDisposable = new CompositeDisposable();
        }
        mCompositeDisposable.add(disposable);
        return disposable;
    }

    /**
     * 清空已添加的操作
     */
    public void clearSubscription() {
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
    }
}
