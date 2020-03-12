/*
 * Copyright 2020 Babylon Partners Limited
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

package com.babylon.orbit.v2

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map


internal class TransformSuspend<S : Any, E : Any, E2 : Any>(val block: suspend Context<S, E>.() -> E2) :
    Operator<S, E2>

internal class TransformFlow<S : Any, E : Any>(val block: suspend Context<S, E>.() -> Flow<Any>) :
    Operator<S, E>

fun <S : Any, E : Any, E2 : Any> Builder<S, E>.transformSuspend(block: suspend Context<S, E>.() -> E2): Builder<S, E2> {
    return Builder(
        stack + TransformSuspend(
            block
        )
    )
}

fun <S : Any, E : Any, E2 : Any> Builder<S, E>.transformFlow(block: suspend Context<S, E>.() -> Flow<E2>): Builder<S, E2> {
    return Builder(
        stack + TransformFlow(
            block
        )
    )
}

internal class CoroutinePlugin<S : Any> : OrbitPlugin<S> {

    override fun <E : Any> apply(
        operator: Operator<S, E>,
        context: (event: E) -> Context<S, E>,
        flow: Flow<E>,
        setState: (suspend () -> S) -> Unit
    ): Flow<Any> {
        return when (operator) {
            is TransformSuspend<*, *, *> -> flow.map {
                @Suppress("UNCHECKED_CAST")
                with(operator as TransformSuspend<S, E, Any>) {
//                    withContext(Dispatchers.IO) {
                    context(it).block()
//                    }
                }
            }
            is TransformFlow -> flow.flatMapConcat {
                with(operator) {
                    context(it).block()//.flowOn(Dispatchers.IO)
                }
            }
            else -> flow
        }
    }
}
