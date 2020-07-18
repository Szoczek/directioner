package pl.inz.directioner.utils

import io.reactivex.Observable

inline fun <reified T> T.toObservable(): Observable<T> {
    return Observable.just(this)
}

