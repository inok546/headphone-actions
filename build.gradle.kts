// SPDX-License-Identifier: AGPL-3.0-only

plugins {
    alias(libs.plugins.android.application) apply false
    // Also puts the matching Kotlin Gradle Plugin on the classpath, overriding
    // the older version AGP's built-in Kotlin support would otherwise use.
    alias(libs.plugins.kotlin.compose) apply false
}
