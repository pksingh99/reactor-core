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

import org.junit.Test;
import reactor.test.TestSubscriber;

public class FluxSkipWhileTest {

	@Test(expected = NullPointerException.class)
	public void sourceNull() {
		new FluxSkipWhile<>(null, v -> true);
	}

	@Test(expected = NullPointerException.class)
	public void predicateNull() {
		Flux.never()
		    .skipWhile(null);
	}

	@Test
	public void skipNone() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 5)
		    .skipWhile(v -> false)
		    .subscribe(ts);

		ts.assertValues(1, 2, 3, 4, 5)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void skipNoneBackpressured() {
		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		Flux.range(1, 5)
		    .skipWhile(v -> false)
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		ts.request(2);

		ts.assertValues(1, 2)
		  .assertNoError()
		  .assertNotComplete();

		ts.request(10);

		ts.assertValues(1, 2, 3, 4, 5)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void skipSome() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 5)
		    .skipWhile(v -> v < 3)
		    .subscribe(ts);

		ts.assertValues(3, 4, 5)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void skipSomeBackpressured() {
		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		Flux.range(1, 5)
		    .skipWhile(v -> v < 3)
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		ts.request(2);

		ts.assertValues(3, 4)
		  .assertNoError()
		  .assertNotComplete();

		ts.request(10);

		ts.assertValues(3, 4, 5)
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void skipAll() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 5)
		    .skipWhile(v -> true)
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void skipAllBackpressured() {
		TestSubscriber<Integer> ts = TestSubscriber.create(0);

		Flux.range(1, 5)
		    .skipWhile(v -> true)
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNoError()
		  .assertNotComplete();

		ts.request(2);

		ts.assertNoValues()
		  .assertComplete()
		  .assertNoError();
	}

	@Test
	public void predicateThrows() {
		TestSubscriber<Integer> ts = TestSubscriber.create();

		Flux.range(1, 5)
		    .skipWhile(v -> {
			    throw new RuntimeException("forced failure");
		    })
		    .subscribe(ts);

		ts.assertNoValues()
		  .assertNotComplete()
		  .assertError(RuntimeException.class)
		  .assertErrorMessage("forced failure");

	}

}
