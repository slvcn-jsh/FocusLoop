# Native UI screenshot gallery

Captured by the real Compose UI on the connected Fold5 with **synthetic injected data/actions**, never the user's usage records or session. Yellow banners identify test app captures. The native calendar dialog is captured as a separate window, without the underlying banner; it contains only calendar dates. The running app has no synthetic metrics or test banner.

Final fixtures: 360 dp compact, 680 dp unfolded (explicit measured-width assertion), and 320 dp at 2× Compose font scale, rendered at 420 dpi on the unlocked unfolded device. Earlier runs also exercised the physical cover window (904×2316 pixels). These are viewport captures: scrollable content continues below the screen. They do not establish TalkBack, actual continuous fold transitions, Android nonlinear system font scaling, or exact font/motion parity with Canva.

| Main screen | Compact | Unfolded |
|---|---|---|
| Home | [Home](compact-home.png) | [Paired Home panels](unfolded-home.png) |
| Usage | [Usage](compact-usage.png) | [Usage](unfolded-usage.png) |
| Focus | [Focus](compact-focus.png) | [Focus](unfolded-focus.png) |
| Settings | [Settings](compact-settings.png) | [Settings](unfolded-settings.png) |

| Flow/state | Screenshot |
|---|---|
| Welcome | [Welcome](compact-welcome.png) |
| App detail | [App detail](compact-app-detail.png) |
| Intention editor, optional controls | [Editor viewport](compact-editor.png) |
| Active session | [Active](compact-active.png) |
| Optional reflection | [Reflection](compact-reflection.png) |
| Session summary | [Summary](compact-summary.png) |
| Data/privacy confirmation result | [Privacy](compact-privacy.png) |
| No available usage records | [Empty usage](compact-empty-usage.png) |
| Native custom-date picker | [Calendar dialog](unfolded-custom-dates.png) |
| Large text on 320 dp Home | [Large-text Home](large-font-home.png) |
| Large text on 320 dp Settings | [Large-text Settings](large-font-settings.png) |

Review found and corrected compact Home's action ordering and the default dialog tint. The palette/card hierarchy follows the supplied Canva exports; standard native controls, visible data-quality caveats and adaptive text wrapping remain intentional. See [verification metadata](../native-ui-verification.json) for actual image dimensions and [results](../../../engineering/NATIVE_UI_RESULTS.md) for limits and remaining manual checks.
