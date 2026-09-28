# Restoring DORJA AI Features

All AI code was **kept in the repo** — only the entry points and dependencies were
removed/disabled. Follow the checklist below to bring everything back. The removal
happened in a single commit; find it with:

```bash
git log --oneline -S "HeyDorjaPill" -- dorja/app/src/main/java/com/example/ui/detail/PropertyDetailScreen.kt
```

Then `git show <commit>` to see the exact removed code (every snippet below is copied
from there verbatim).

## What still exists (untouched)

| Path | Contents |
|---|---|
| `dorja/app/src/main/java/com/example/ai/` | `DorjaAiEngine.kt` (rule-based engine), `DorjaAssistant.kt` (Cactus on-device model, 4 tools), `PropertyAiContext.kt`, `VoiceAssistantHelper.kt` (speech recognizer) |
| `dorja/app/src/main/java/com/example/ui/ai/` | `HeyDorjaBottomSheet.kt` — the "Hey Dorja" property-context sheet |
| `dorja/app/src/main/java/com/example/ui/assistant/` | `AssistantScreen.kt` — full-screen assistant (route `Screen.Assistant` still exists in `DorjaNavHost.kt`) |
| `dorja/ai-finetune/` | Needle Phase 2 fine-tuning kit (tools.json, data, README) |

## Restore checklist

### 0. Re-include the AI sources (`dorja/app/build.gradle.kts`)

The `sourceSets` block in `android { }` currently excludes `com/example/ai/**`,
`com/example/ui/ai/**` and `com/example/ui/assistant/**` from compilation (otherwise
the kept files can't compile without the cactus dependency). Delete that whole
`sourceSets { ... }` block — it is marked with an "AI PARKED" comment.

### 1. Dependency (`dorja/app/build.gradle.kts`)

```kotlin
// uncomment:
implementation("com.cactuscompute:cactus:1.4.1-beta")
```

Skip this step if you are wiring a **Gemini API** instead — then add e.g.
`implementation("com.google.ai.client.generativeai:generativeai:0.9.0")` and swap the
engine internals; the UI layers below don't change.

### 2. SDK init (`MainActivity.kt`, in `onCreate` before `setContent`)

```kotlin
// uncomment:
com.cactus.CactusContextInitializer.initialize(this)
```

### 3. Application hook (`DorjaApp.kt`)

```kotlin
// uncomment:
val aiEngine by lazy { com.example.ai.DorjaAiEngine.getInstance(this) }
```

### 4. Manifest (`app/src/main/AndroidManifest.xml`) — only needed for voice wake-word

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />

<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```

### 5. Explore sparkle button (`ui/explore/ExploreScreen.kt`)

Re-add the parameter on the composable signature:

```kotlin
onOpenAssistant: () -> Unit = {}
```

Re-add the sparkle icon import (`androidx.compose.material.icons.filled.AutoAwesome`)
and put this back as the first child of the search field's `trailingIcon = { ... }`:

```kotlin
IconButton(
    onClick = onOpenAssistant,
    modifier = Modifier.testTag("explore_open_assistant")
) {
    Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = L("assistant_title"),
        tint = DorjaColors.Jol600
    )
}
```

(If the search field's trailing block was simplified, wrap both buttons in
`Row { ... }` again.)

### 6. Property detail voice + pill (`ui/detail/PropertyDetailScreen.kt`)

Restore from `git show <removal-commit>`:

1. imports: `android.Manifest`, `android.content.pm.PackageManager`,
   `androidx.core.content.ContextCompat`, `androidx.compose.runtime.DisposableEffect`,
   `com.example.ai.PropertyAiContext`, `com.example.ai.VoiceAssistantHelper`,
   `com.example.ui.ai.HeyDorjaAssistantSheet`, `com.example.ui.components.DorjaLogo`
2. the `showHeyDorjaSheet` state + `propertyAiContext` remember block
3. the `VoiceAssistantHelper` permission launcher + `DisposableEffect` hotword listener
4. both `HeyDorjaPill(onClick = { ... })` entries in the floating action bar
5. the `if (showHeyDorjaSheet) { HeyDorjaAssistantSheet(...) }` invocation at the end
6. the `HeyDorjaPill` private composable + `matchesWakeWord()` helper at file bottom

### 7. Navigation (`ui/navigation/DorjaNavHost.kt`)

```kotlin
// MainContainer params — re-add:
onNavigateToAssistant: () -> Unit = {},

// call site inside Main tab:
onNavigateToAssistant = { navController.navigate(Screen.Assistant.route) },

// Explore wiring:
BuyerTab.EXPLORE -> ExploreScreen(
    onSelectListing = onNavigateToDetail,
    onOpenAssistant = onNavigateToAssistant
)
```

(The `composable(Screen.Assistant.route) { AssistantScreen(...) }` destination and the
`import com.example.ui.assistant.AssistantScreen` were removed too — restore them.)

### 8. Verify

Push → wait for Build APK → install → check: sparkle button in Explore search,
"Dorja" pill on PropertyDetail, voice hotword "Hey Dorja" opens the sheet.
