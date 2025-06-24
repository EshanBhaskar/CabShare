package com.project.cabshare.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.cabshare.R

/**
 * A composable function that displays the CabShare logo in a rounded frame
 * with consistent spacing and alignment
 */
@Composable
fun CabShareLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "CabShare Logo",
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFF00ADB5), // App primary color
    borderWidth: Int = 2,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderWidth.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(8.dp), // Added consistent padding
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.cabshare_logo),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit // Changed to Fit for better scaling
        )
    }
}

/**
 * A smaller version of the CabShare logo in a rounded frame for app bars
 * with consistent spacing and alignment
 */
@Composable
fun CabShareLogoSmall(
    modifier: Modifier = Modifier.size(40.dp),
    contentDescription: String? = "CabShare Logo",
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFF00ADB5), // App primary color
    borderWidth: Int = 1,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp)) // Slightly smaller corner radius for small size
            .background(backgroundColor)
            .border(borderWidth.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(4.dp), // Smaller padding for small size
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.cabshare_logo),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit // Changed to Fit for better scaling
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogoPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CabShareLogo(
            modifier = Modifier.size(180.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CabShareLogoSmall(
            modifier = Modifier.size(60.dp)
        )
    }
} 