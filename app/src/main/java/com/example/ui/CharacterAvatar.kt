package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.R
import com.example.domain.JourneyCharacter

/** The approved avatar art for each character (from docs/journey-ui-v2.html), keyed by character id. */
internal val CHARACTER_AVATARS = mapOf(
    "max" to R.drawable.char_max_avatar,
    "ivy" to R.drawable.char_ivy_avatar,
    "lee" to R.drawable.char_lee_avatar,
    "emma" to R.drawable.char_emma_avatar,
    "charles" to R.drawable.char_charles_avatar,
    "georgia" to R.drawable.char_georgia_avatar
)

/** The character's approved head-and-shoulders avatar, in a circle. */
@Composable
fun CharacterAvatar(character: JourneyCharacter, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(CHARACTER_AVATARS[character.id] ?: R.drawable.char_max_avatar),
        contentDescription = character.name,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(CircleShape)
    )
}
