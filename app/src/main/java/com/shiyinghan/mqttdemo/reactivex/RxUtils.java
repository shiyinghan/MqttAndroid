package com.shiyinghan.mqttdemo.reactivex;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author admin
 */
public class RxUtils {

    /**
     * 管理所有的订阅
     */
    protected static CompositeDisposable sCompositeDisposable;

    /**
     * 释放资源
     */
    public static void release() {
        if (sCompositeDisposable != null) {
            sCompositeDisposable.clear();
        }
    }

    public static void addSubscription(Disposable disposable) {
        if (sCompositeDisposable == null) {
            sCompositeDisposable = new CompositeDisposable();
        }
        sCompositeDisposable.add(disposable);
    }

    public static <D> void addSubscription(Observable<D> observable, DisposableObserver<D> observer) {
        addSubscription(observable.compose(rxSchedulerHelper()).subscribeWith(observer));
    }

    public static <D> void addSubscription(Completable completable, DisposableCompletableObserver observer) {
        addSubscription(completable.compose(rxCompletableSchedulerHelper()).subscribeWith(observer));
    }

    public static <D> void addSubscription(Maybe<D> observable, DisposableMaybeObserver<D> observer) {
        addSubscription(observable.compose(rxMaybeSchedulerHelper()).subscribeWith(observer));
    }

    public static <D> void addSubscription(Single<D> observable, DisposableSingleObserver<D> observer) {
        addSubscription(observable.compose(rxSingleSchedulerHelper()).subscribeWith(observer));
    }

    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return ObservableTransformer
     */
    public static <T> ObservableTransformer<T, T> rxSchedulerHelper() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return FlowableTransformer
     */
    public static <T> FlowableTransformer<T, T> rxFlowableSchedulerHelper() {
        return flowable -> flowable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return MaybeTransformer
     */
    public static <T> MaybeTransformer<T, T> rxMaybeSchedulerHelper() {
        return maybe -> maybe.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return SingleTransformer
     */
    public static <T> SingleTransformer<T, T> rxSingleSchedulerHelper() {
        return single -> single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 统一线程处理
     *
     * @param <T> 指定的泛型类型
     * @return CompletableTransformer
     */
    public static <T> CompletableTransformer rxCompletableSchedulerHelper() {
        return completable -> completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
