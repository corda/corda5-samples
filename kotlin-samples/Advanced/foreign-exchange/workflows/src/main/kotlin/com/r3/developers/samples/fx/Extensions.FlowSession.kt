package com.r3.developers.samples.fx

import net.corda.v5.application.messaging.FlowSession

internal inline fun <reified T : Any> FlowSession.receive(): T {
    return receive(T::class.java)
}

internal inline fun <reified T : Any> FlowSession.sendAndReceive(payload: Any): T {
    return sendAndReceive(T::class.java, payload)
}