/*
 * Copyright 2019 Babylon Partners Limited
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.babylon.orbit

import androidx.lifecycle.SavedStateHandle
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class OrbitViewModelStateSavingTest {

    @Before
    fun before() {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler {
            RxJavaPlugins.createNewThreadScheduler { Thread(it, "main") }
        }
        RxAndroidPlugins.setMainThreadSchedulerHandler {
            RxJavaPlugins.createNewThreadScheduler { Thread(it, "main") }
        }
    }

    @After
    fun after() {
        RxJavaPlugins.reset()
    }

    @Test
    fun `If I provide a saved state handle for a middleware initialised ViewModel my state gets saved`() {
        val savedStateHandle = SavedStateHandle()

        // Given A simple middleware with a reducer
        val middleware = middleware<Int, Unit>(0) {
            perform("Increment id")
                .on<Unit>()
                .reduce { currentState + 42 }
        }

        testSavedState(OrbitViewModel(middleware, savedStateHandle), savedStateHandle)
    }

    @Test
    fun `If I provide a saved state handle for a container initialised ViewModel my state gets saved`() {
        val savedStateHandle = SavedStateHandle()

        // Given A simple middleware with a reducer
        val container = BaseOrbitContainer(
            middleware<Int, Unit>(0) {
                perform("Increment id")
                    .on<Unit>()
                    .reduce { currentState + 42 }
            }
        )

        testSavedState(OrbitViewModel(container, savedStateHandle), savedStateHandle)
    }

    @Test
    fun `If I provide a saved state handle for a self initialised ViewModel my state gets saved`() {
        val savedStateHandle = SavedStateHandle()

        // Given A simple middleware with a reducer
        val orbitViewModel = OrbitViewModel<Int, Unit>(0, savedStateHandle) {
            perform("Increment id")
                .on<Unit>()
                .reduce { currentState + 42 }
        }

        testSavedState(orbitViewModel, savedStateHandle)
    }

    private fun testSavedState(
        orbitViewModel: OrbitViewModel<Int, *>,
        savedStateHandle: SavedStateHandle
    ) {
        val lifecycleOwner = MockLifecycleOwner()
        val stateSubject = PublishSubject.create<Int>()
        val stateObserver = stateSubject.test()

        // Given I am connected to the view model
        orbitViewModel.connect(lifecycleOwner) { stateSubject.onNext(it) }

        // When I post an action to the viewModel
        orbitViewModel.sendAction(Unit)

        // Then My new state is saved
        stateObserver.awaitCount(2)
        assertThat(savedStateHandle.get<Int?>("state"))
            .isEqualTo(42)
    }
}
