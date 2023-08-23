package com.r3.developers.samples.fx

import org.slf4j.LoggerFactory

interface WithLogger {
    val logger get() = LoggerFactory.getLogger(this::class.java.enclosingClass)
}