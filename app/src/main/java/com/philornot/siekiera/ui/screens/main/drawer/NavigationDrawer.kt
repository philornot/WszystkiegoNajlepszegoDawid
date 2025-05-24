package com.philornot.siekiera.ui.screens.main.drawer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Navigation drawer for the app that slides in from the left. Contains
 * options for gift, timer, and countdown to next birthday. Only accessible
 * after the gift has been opened at least once.
 *
 * Updated with improved hamburger menu positioning to avoid text
 * collision.
 */
@Composable
fun NavigationDrawer(
    isOpen: Boolean,
    onOpenStateChange: (Boolean) -> Unit,
    currentSection: NavigationSection,
    onSectionSelected: (NavigationSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerWidth = 280.dp
    val animatedWidth by animateDpAsState(
        targetValue = if (isOpen) drawerWidth else 0.dp, label = "drawerWidth"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f, label = "drawerAlpha"
    )
    val hamburgerRotation by animateFloatAsState(
        targetValue = if (isOpen) 90f else 0f, label = "hamburgerRotation"
    )

    // Drawer system with overlay
    Box(modifier = modifier
        .fillMaxSize()
        .zIndex(10f)) {
        // Scrim (dimming overlay when drawer is open)
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onOpenStateChange(false) })
        }

        // Drawer content
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        // Close drawer on swipe left
                        if (dragAmount < -10) {
                            onOpenStateChange(false)
                        }
                    }
                }) {
            // Drawer items
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Respect system status bar
                    .padding(16.dp)
                    .alpha(animatedAlpha)
            ) {
                // Header with extra top spacing
                Spacer(modifier = Modifier.padding(top = 26.dp))

                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                // Navigation items
                DrawerItem(
                    icon = Icons.Default.CardGiftcard,
                    title = "Prezent",
                    isSelected = currentSection == NavigationSection.GIFT,
                    onClick = {
                        onSectionSelected(NavigationSection.GIFT)
                        onOpenStateChange(false)
                    })

                DrawerItem(
                    icon = Icons.Default.AccessTime,
                    title = "Timer",
                    isSelected = currentSection == NavigationSection.TIMER,
                    onClick = {
                        onSectionSelected(NavigationSection.TIMER)
                        onOpenStateChange(false)
                    })

                DrawerItem(
                    icon = Icons.Default.Cake,
                    title = "Odliczanie urodzin",
                    isSelected = currentSection == NavigationSection.BIRTHDAY_COUNTDOWN,
                    onClick = {
                        onSectionSelected(NavigationSection.BIRTHDAY_COUNTDOWN)
                        onOpenStateChange(false)
                    })
            }
        }

        // Improved hamburger menu icon positioning
        Surface(
            modifier = Modifier
                .statusBarsPadding() // Respect system status bar
                .padding(12.dp) // Reduced padding for better positioning
                .align(Alignment.TopStart),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // Semi-transparent background
            shadowElevation = 4.dp // Subtle shadow for better visibility
        ) {
            IconButton(
                onClick = { onOpenStateChange(!isOpen) },
                modifier = Modifier.size(48.dp) // Consistent size
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(hamburgerRotation)
                )
            }
        }
    }
}
