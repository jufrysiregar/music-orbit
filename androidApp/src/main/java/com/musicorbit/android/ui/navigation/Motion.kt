package com.musicorbit.android.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-transition plumbing so the album art can "expand" from the mini player on Home
 * into the full player. The scopes are provided once at the NavHost level and consumed by
 * [orbitSharedElement] anywhere in the tree. They degrade to a no-op when absent.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope      = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Applies a shared-element transition for [key] if both the shared-transition scope and the
 * current animated-visibility scope are available. Safe to call on any composable, on any
 * route - it simply returns an unmodified Modifier when the scopes aren't present.
 */
@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun Modifier.orbitSharedElement(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animScope   = LocalNavAnimatedScope.current ?: return this
    return with(sharedScope) {
        this@orbitSharedElement.sharedElement(
            state                   = rememberSharedContentState(key = key),
            animatedVisibilityScope = animScope
        )
    }
}
package com.musicorbit.android.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-transition plumbing so the album art can "expand" from the mini player on Home
 * into the full player. The scopes are provided once at the NavHost level and consumed by
 * [orbitSharedElement] anywhere in the tree. They degrade to a no-op when absent.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope      = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Standard eased bounds morph for shared elements. */
private val orbitBoundsTransform = androidx.compose.animation.BoundsTransform {
    tween(durationMillis = 380)
}

/**
 * Applies a shared-element transition for [key] if both the shared-transition scope and the
 * current animated-visibility scope are available. Safe to call on any composable, on any
 * route - it simply returns an unmodified Modifier when the scopes aren't present.
 */
@Composable
fun Modifier.orbitSharedElement(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animScope   = LocalNavAnimatedScope.current ?: return this
    return with(sharedScope) {
        this@orbitSharedElement.sharedElement(
            state                 = rememberSharedContentState(key = key),
            animatedVisibilityScope = animScope,
            boundsTransform         = orbitBoundsTransform
        )
    }
}
