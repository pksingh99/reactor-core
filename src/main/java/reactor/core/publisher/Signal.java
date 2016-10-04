/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.publisher;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A domain representation of a Reactive Stream signal.
 * There are 4 distinct signals and their possible sequence is defined as such:
 * onError | (onSubscribe onNext* (onError | onComplete)?)
 *
 * @author Stephane Maldini
 * 
 * @param <T> the value type
 */
public final class Signal<T> implements Supplier<T>, Consumer<Subscriber<? super T>>, Serializable {

	private static final long serialVersionUID = 8430680363917273272L;

	private static final Signal<Void> ON_COMPLETE =
			new Signal<>(SignalType.ON_COMPLETE, null, null, null);

	private final SignalType type;
	private final Throwable  throwable;

	private final T value;

	private transient final Subscription subscription;

	/**
	 * Creates and returns a {@code Signal} of variety {@code Type.COMPLETE}.
	 * @param <T> the value type
	 *
	 * @return an {@code OnCompleted} variety of {@code Signal}
	 */
	@SuppressWarnings("unchecked")
	public static <T> Signal<T> complete() {
		return (Signal<T>) ON_COMPLETE;
	}

	/**
	 * Creates and returns a {@code Signal} of variety {@code Type.FAILED}, and assigns it an error.
	 *
	 * @param <T> the value type
	 * @param e the error to assign to the signal
	 * @return an {@code OnError} variety of {@code Signal}
	 */
	public static <T> Signal<T> error(Throwable e) {
		return new Signal<>(SignalType.ON_ERROR, null, e, null);
	}

	/**
	 * Creates and returns a {@code Signal} of variety {@code Type.NEXT}, and assigns it a value.
	 *
	 * @param <T> the value type
	 * @param t the item to assign to the signal as its value
	 * @return an {@code OnNext} variety of {@code Signal}
	 */
	public static <T> Signal<T> next(T t) {
		return new Signal<>(SignalType.ON_NEXT, t, null, null);
	}

	/**
	 * @param o is the given object a complete {@link Signal}
	 * @return true if completition signal
	 */
	public static boolean isComplete(Object o){
		return o == ON_COMPLETE;
	}

	/**
	 * @param o is the given object a complete {@link Signal}
	 * @return true if completition signal
	 */
	public static boolean isError(Object o){
		return o instanceof Signal && ((Signal)o).type == SignalType.ON_ERROR;
	}

	/**
	 * Creates and returns a {@code Signal} of variety {@code Type.ON_SUBSCRIBE}.
	 *
	 * @param <T> the value type
	 * @param subscription the subscription
	 * @return an {@code OnCompleted} variety of {@code Signal}
	 */
	public static <T> Signal<T> subscribe(Subscription subscription) {
		return new Signal<>(SignalType.ON_SUBSCRIBE, null, null, subscription);
	}

	private Signal(SignalType type, T value, Throwable e, Subscription subscription) {
		this.value = value;
		this.subscription = subscription;
		this.throwable = e;
		this.type = type;
	}

	/**
	 * Read the error associated with this (onError) signal.
	 *
	 * @return the Throwable associated with this (onError) signal
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * Read the subscription associated with this (onSubscribe) signal.
	 *
	 * @return the Subscription associated with this (onSubscribe) signal
	 */
	public Subscription getSubscription() {
		return subscription;
	}

	/**
	 * Retrieves the item associated with this (onNext) signal.
	 *
	 * @return the item associated with this (onNext) signal
	 */
	@Override
	public T get() {
		return value;
	}

	/**
	 * Has this signal an item associated with it ?
	 *
	 * @return a boolean indicating whether or not this signal has an item associated with it
	 */
	public boolean hasValue() {
		return isOnNext() && value != null;
	}

	/**
	 * Read whether this signal is on error and carries the cause.
	 *
	 * @return a boolean indicating whether this signal has an error
	 */
	public boolean hasError() {
		return isOnError() && throwable != null;
	}

	/**
	 * Read the type of this signal: {@code Subscribe}, {@code Next}, {@code Error}, or {@code Complete}
	 *
	 * @return the type of the signal
	 */
	public SignalType getType() {
		return type;
	}

	/**
	 * Indicates whether this signal represents an {@code onError} event.
	 *
	 * @return a boolean indicating whether this signal represents an {@code onError} event
	 */
	public boolean isOnError() {
		return getType() == SignalType.ON_ERROR;
	}

	/**
	 * Indicates whether this signal represents an {@code onComplete} event.
	 *
	 * @return a boolean indicating whether this signal represents an {@code onSubscribe} event
	 */
	public boolean isOnComplete() {
		return getType() == SignalType.ON_COMPLETE;
	}

	/**
	 * Indicates whether this signal represents an {@code onSubscribe} event.
	 *
	 * @return a boolean indicating whether this signal represents an {@code onSubscribe} event
	 */
	public boolean isOnSubscribe() {
		return getType() == SignalType.ON_SUBSCRIBE;
	}

	/**
	 * Indicates whether this signal represents an {@code onNext} event.
	 *
	 * @return a boolean indicating whether this signal represents an {@code onNext} event
	 */
	public boolean isOnNext() {
		return getType() == SignalType.ON_NEXT;
	}

	@Override
	public void accept(Subscriber<? super T> observer) {
		if (isOnNext()) {
			observer.onNext(get());
		} else if (isOnComplete()) {
			observer.onComplete();
		} else if (isOnError()) {
			observer.onError(getThrowable());
		} else if (isOnSubscribe()) {
			observer.onSubscribe(subscription);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Signal<?> signal = (Signal<?>) o;

		if (type != signal.type) {
			return false;
		}
		if (isOnComplete()) {
			return true;
		}
		if (isOnSubscribe()) {
			return Objects.equals(this.subscription, signal.subscription);
		}
		else if (isOnError()) {
			return Objects.equals(this.throwable, signal.throwable);
		}
		else if (isOnNext()) {
			return Objects.equals(this.value, signal.value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		if (isOnError())
			result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
		if (isOnNext())
			result = 31 * result + (value != null ? value.hashCode() : 0);
		if (isOnComplete())
			result = 31 * result + (subscription != null ? subscription.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		switch (this.type) {
			case ON_SUBSCRIBE:
				return String.format("onSubscribe(%s)", this.subscription);
			case ON_NEXT:
				return String.format("onNext(%s)", this.value);
			case ON_ERROR:
				return String.format("onError(%s)", this.throwable);
			case ON_COMPLETE:
				return "onComplete()";
			default:
				return String.format("Signal type=%s", this.type);
		}
	}
}
