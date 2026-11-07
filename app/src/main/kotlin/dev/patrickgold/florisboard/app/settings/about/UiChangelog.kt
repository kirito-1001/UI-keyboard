/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import org.florisboard.lib.compose.stringRes

/**
 * One card of the in-app changelog. [version] is the commit (the sha shown at the end of the version string in
 * Settings > About) in which these changes were made.
 *
 * NEW ENTRIES GO AT THE TOP OF [UiChangelogEntries] (newest first).
 */
data class UiChangelogEntry(
    val version: String,
    val ar: List<String>,
    val en: List<String>,
)

val UiChangelogEntries = listOf(
    UiChangelogEntry(
        version = "846c5e5",
        ar = listOf(
            "الصفحة الرئيسية للإعدادات: كل خيار في بطاقة بيضاء منفصلة بحواف دائرية، بدون خطوط فاصلة، مع سطر تفاصيل تحت كل عنوان.",
        ),
        en = listOf(
            "Settings home: each option is a separate rounded white card with no divider lines, and a detail line under each title.",
        ),
    ),
    UiChangelogEntry(
        version = "7fd573d",
        ar = listOf("الصفحة الرئيسية للإعدادات: الخطوط الفاصلة بين الخيارات صارت أخف مرة أخرى بنسبة 50٪."),
        en = listOf("Settings home: the dividers between the options are faded by another 50%."),
    ),
    UiChangelogEntry(
        version = "c0ef210",
        ar = listOf(
            "تصحيح الكلمات أثناء الكتابة: الكلمة غير المعروفة (مثل «طعان» أو «tha») تظهر لها أقرب كلمة صحيحة في أول خانة، ثم الكلمة الأكثر استخدامًا لديك، ثم كلمة القاموس. وترتيب الاقتراحات من اليسار إلى اليمين.",
            "زر «إدخال صوتي» صار من أزرار الشريط العادية: يظهر في قائمة الشعار، ويمكن تغيير ترتيبه أو إخفاؤه من الإعدادات ← الشريط الذكي ← أزرار الشريط.",
            "الصفحة الرئيسية للإعدادات: الخطوط الفاصلة بين الخيارات صارت أخف بنسبة 50٪.",
        ),
        en = listOf(
            "Typo correction while typing: an unknown word (like «tha») gets the closest correct word in the first slot, then your most used word, then the word list suggestion. Suggestions run from left to right.",
            "«Voice input» is now a normal toolbar button: it shows in the logo menu and can be moved or hidden in Settings > Smartbar > Toolbar buttons.",
            "Settings home: the dividers between the options are 50% more transparent.",
        ),
    ),
    UiChangelogEntry(
        version = "05cea6d",
        ar = listOf(
            "تم اجراء مراجعه وتصحيح اخطاء هذه النسخه بواسطة السيد الكبير الرهيب وليد ابوطبنجه ابن عزبه زنكت البار.....واهي كلها بترزق",
        ),
        en = listOf(
            "This version was reviewed and debugged by the Great and Terrible Mr. Waleed Abu Tabanja, son of Ezbet Zankat al-Bar..... and it's all just making a living.",
        ),
    ),
    UiChangelogEntry(
        version = "97f9051",
        ar = listOf(
            "اللغتان الإنجليزية والعربية موجودتان في «اللغات والتخطيطات» من أول تشغيل، بدون الحاجة لإضافتهما.",
        ),
        en = listOf(
            "English and Arabic are in \"Languages & layouts\" from the first start, with no need to add them.",
        ),
    ),
    UiChangelogEntry(
        version = "8078825",
        ar = listOf(
            "قاموس أكبر للاقتراحات: نحو 150 ألف كلمة عربية و100 ألف إنجليزية بدل 50 ألفاً، مع حذف الكلمات النادرة جداً والمكتوبة بأخطاء.",
        ),
        en = listOf(
            "Bigger suggestion dictionary: about 150k Arabic and 100k English words instead of 50k, with very rare and misspelled words removed.",
        ),
    ),
    UiChangelogEntry(
        version = "9a2fe6f",
        ar = listOf(
            "تخطيط جديد للصفحة الرئيسية للإعدادات: خلفية فاتحة، وعنوان الترحيب في الأعلى، والخيارات داخل لوحة واحدة بحواف علوية دائرية يفصل بينها خطوط رفيعة.",
        ),
        en = listOf(
            "New settings home layout: a light page, the welcome title on top, and all options in one sheet with rounded top corners and thin dividers.",
        ),
    ),
    UiChangelogEntry(
        version = "c5822c9",
        ar = listOf(
            "القائمة المنبثقة عند الضغط المطوّل على الحروف: خطوط فاصلة رفيعة بين الحروف (وبين الصفوف إن كانت أكثر من صف).",
        ),
        en = listOf(
            "Long-press popup: thin divider lines between the characters (and between rows when there is more than one).",
        ),
    ),
    UiChangelogEntry(
        version = "d257984",
        ar = listOf(
            "تخطيط جديد للصفحة الرئيسية للإعدادات: بطاقة ترحيب أضيق في الأعلى («مرحبا بكم في» ثم «UI Keyboard») وتحتها بقية الخيارات في بطاقات نصف دائرية بلون سماوي فاتح.",
        ),
        en = listOf(
            "New settings home layout: a narrower welcome card on top (\"Welcome to\" then \"UI Keyboard\") with the other options below in light cyan pill cards.",
        ),
    ),
    UiChangelogEntry(
        version = "bcd96d5",
        ar = listOf(
            "أيقونة جديدة للإعدادات (ترس بحواف ناعمة) تحل محل القديمة في قائمة اللغة وقائمة الإجراءات وإعدادات أزرار الشريط.",
        ),
        en = listOf(
            "New settings icon (a soft-edged gear) replaces the old one in the language menu, the actions panel and the toolbar buttons settings.",
        ),
    ),
    UiChangelogEntry(
        version = "370a4b6",
        ar = listOf(
            "الصفحة الرئيسية للإعدادات: كل خيار داخل بطاقة بحواف نصف دائرية، وخط القائمة أكبر بنسبة 35%.",
        ),
        en = listOf(
            "Settings home page: every option is in a pill-shaped card, and the menu text is 35% larger.",
        ),
    ),
    UiChangelogEntry(
        version = "33544a8",
        ar = listOf(
            "زر الإدخال صار أخضر افتراضياً بنفس درجة مفتاح الاقتراحات (إذا لم تختر ثيماً).",
            "الأخضر الجديد أُضيف أول خيار في لوحة «اللون الثانوي للإعدادات» ولوحة ألوان تخصيص المفاتيح.",
            "إصلاح: مفتاح تشغيل الاقتراحات يظهر في كل اللغات، والاختيار من الاقتراحات يستبدل الكلمة كاملة.",
        ),
        en = listOf(
            "The enter key is now green by default, the same shade as the suggestions key (when no theme is chosen).",
            "The new green is the first choice in the settings accent colour and key customization palettes.",
            "Fix: the suggestions key shows in every language, and choosing a suggestion replaces the whole word.",
        ),
    ),
    UiChangelogEntry(
        version = "0d31700",
        ar = listOf(
            "توقّع الكلمة التالية صار يعتمد على آخر كلمتين وليس كلمة واحدة، مما يجعل الاقتراحات أدق.",
            "قوائم جاهزة (نحو 20 ألف ثلاثية عربية و15 ألف إنجليزية) والكيبورد يتعلّم الثلاثيات من كتابتك أيضاً.",
        ),
        en = listOf(
            "Next-word prediction now uses the last two words instead of one, so suggestions are more accurate.",
            "Bundled lists (about 20k Arabic and 15k English word triples), and the keyboard learns triples from your typing too.",
        ),
    ),
    UiChangelogEntry(
        version = "b98113d",
        ar = listOf(
            "زر جديد في الصف السفلي بدل زر الفاصلة (أيقونة الخط A): يشغّل الاقتراحات ويوقفها، ويصبح أخضر عندما تكون مشغّلة.",
            "الفاصلة انتقلت إلى زر النقطة: اضغط مطولاً على النقطة لكتابتها.",
        ),
        en = listOf(
            "New key in the bottom row replacing the comma key (the font \"A\" icon): turns suggestions on and off, and is green while they are on.",
            "The comma moved to the period key: long-press the period to type it.",
        ),
    ),
    UiChangelogEntry(
        version = "aff7b21",
        ar = listOf(
            "توقّع الكلمة التالية من أول استعمال: قائمة أزواج كلمات خفيفة (نحو 50 ألف زوج عربي و30 ألف إنجليزي) تُحسب وقت البناء من ويكيبيديا، فتقلّ اللهجات. ما تتعلمه من كتابتك يظهر قبلها.",
        ),
        en = listOf(
            "Next-word suggestions from the first use: a light word-pair list (about 50k Arabic and 30k English pairs) built from Wikipedia at build time, so dialects are mostly left out. What you type yourself shows first.",
        ),
    ),
    UiChangelogEntry(
        version = "d3224a7",
        ar = listOf(
            "إصلاح: تغيير اللغة وأنت في لوحة الأرقام أو الرموز يبقيك في نفس اللوحة بدل الرجوع إلى لوحة الأحرف.",
        ),
        en = listOf(
            "Fix: switching language while on the numeric or symbols panel now keeps you on that panel instead of going back to letters.",
        ),
    ),
    UiChangelogEntry(
        version = "2bc0424",
        ar = listOf(
            "عند الضغط على كلمة من الاقتراحات تُضاف مسافة بعدها تلقائياً، وتظهر مباشرة اقتراحات الكلمة التالية.",
        ),
        en = listOf(
            "Tapping a suggested word now adds a space after it automatically, and next-word suggestions appear right away.",
        ),
    ),
    UiChangelogEntry(
        version = "0cccaed",
        ar = listOf(
            "اقتراحات الكلمات (الدفعة الثانية): الكيبورد يتعلّم الكلمات التي تكتبها ويقترحها أولاً، ويتوقّع الكلمة التالية بعد المسافة.",
            "التعلّم يبقى على جوالك فقط، ويتوقف في الوضع المخفي وحقول كلمات المرور.",
            "زر جديد في إعدادات الكتابة «مسح ما تعلّمه الكيبورد».",
        ),
        en = listOf(
            "Word suggestions (batch 2): the keyboard learns the words you type, suggests them first, and predicts the next word after a space.",
            "Learning stays on your phone only, and is off in incognito mode and password fields.",
            "New button in Typing settings: \"Clear what the keyboard learned\".",
        ),
    ),
    UiChangelogEntry(
        version = "70da520",
        ar = listOf(
            "إصلاح: إعداد «حجم نص الاقتراحات» يغيّر الآن حجم الكلمات المعروضة في شريط الأدوات.",
        ),
        en = listOf(
            "Fix: the \"suggestion text size\" setting now changes the size of the words shown in the toolbar.",
        ),
    ),
    UiChangelogEntry(
        version = "0f689c3",
        ar = listOf(
            "اقتراحات الكلمات (الدفعة الأولى): قاموس عربي وإنجليزي، وأثناء الكتابة تظهر في شريط الاقتراحات كلمات تكمل ما كتبته (الأكثر شيوعاً أولاً). في العربية لا يهم التشكيل ولا شكل الألف أو الهمزة.",
            "قوائم الكلمات تُنزَّل وقت بناء التطبيق من مشروع FrequencyWords (ترخيص CC BY-SA 4.0)، ومصدرها مذكور في «حول».",
        ),
        en = listOf(
            "Word suggestions (batch 1): Arabic and English dictionaries; while typing, the suggestion bar completes the word you are typing (most frequent first). In Arabic, diacritics and alef/hamza forms are ignored.",
            "The word lists are downloaded when the app is built from the FrequencyWords project (CC BY-SA 4.0); the source is credited in About.",
        ),
    ),
    UiChangelogEntry(
        version = "b4c24a9",
        ar = listOf(
            "قائمة اللغة: أيقونة بجانب نص كل خيار (على يمينه): كرة أرضية للغات، لوحة أرقام للأرقام، # للرموز، لوحة ألوان للثيمات، وترس للإعدادات.",
        ),
        en = listOf(
            "Language menu: an icon right next to the text of each entry (on its right): globe for languages, dialpad for numbers, # for symbols, palette for themes and a gear for settings.",
        ),
    ),
    UiChangelogEntry(
        version = "57911db",
        ar = listOf(
            "خيار «الثيمات» انتقل من القائمة المنسدلة للزرين ١ و٢ إلى قائمة اللغة، بين «الرموز» و«الإعدادات».",
        ),
        en = listOf(
            "\"Themes\" moved from the dropdown of buttons 1 and 2 to the language menu, between \"Symbols\" and \"Settings\".",
        ),
    ),
    UiChangelogEntry(
        version = "016a2b8",
        ar = listOf(
            "أيقونات جديدة للقائمة المنسدلة من الزرين ١ و٢ (الإيموجي، تعديل النص، الحافظة، الإدخال الصوتي) وللزرين نفسيهما في الشريط، وأيقونة جديدة لزر إخفاء اللوحة.",
        ),
        en = listOf(
            "New icons for the dropdown of buttons 1 and 2 (emoji, edit text, clipboard, voice input) and for the two buttons themselves in the bar, and a new icon for the hide-keyboard button.",
        ),
    ),
    UiChangelogEntry(
        version = "48eaaba",
        ar = listOf(
            "إضافة «الثيمات» (بأيقونة لوحة الألوان) إلى القائمة المنسدلة للزرين ١ و٢، وإلى لوحة الأدوات، وإلى قائمة أزرار شريط الأدوات في الإعدادات للتحكم بموضعها. الضغط عليها يفتح الإعدادات على قسم المظهر.",
        ),
        en = listOf(
            "Added \"Themes\" (color-palette icon) to the dropdown of buttons 1 and 2, to the actions panel and to the toolbar buttons setting (so its position can be changed). Tapping it opens Settings on the Appearance section.",
        ),
    ),
    UiChangelogEntry(
        version = "9d227a5",
        ar = listOf("ظل متدرج تحت شريط الأدوات عند اختيار صورة كخلفية، يتلاشى في المنطقة الفاصلة بين الشريط والمفاتيح."),
        en = listOf("A soft gradient shadow under the toolbar when a background picture is used, fading out in the gap between the toolbar and the keys."),
    ),
    UiChangelogEntry(
        version = "e4b5dc7",
        ar = listOf(
            "عند اختيار صورة كخلفية: خلفية المفاتيح ضبابية (زجاج مصنفر) تظهر الجزء الذي خلف كل زر، في الحروف ولوحة تعديل النص ولوحة الأرقام.",
            "النوافذ المنبثقة للحروف صارت تتبع الثيم (ضبابية مع الصورة، وبلون الثيم مع الثيمات الجاهزة).",
            "إصلاح: مربع بلون مختلف حول الحرف داخل المفتاح عند اختيار صورة.",
        ),
        en = listOf(
            "With a background picture: key backgrounds are blurred (frosted glass) showing the part behind each key, on the letters keyboard, the edit-text panel and the numeric panel.",
            "Key popups now follow the theme (blurred over a picture, theme colors with ready-made themes).",
            "Fix: a square of a different color around the letter inside a key when a picture is chosen.",
        ),
    ),
    UiChangelogEntry(
        version = "cc3843b",
        ar = listOf(
            "قسم جديد «الثيمات» في الإعدادات > المظهر: اختيار صورة كخلفية للوحة، و18 ثيمًا جاهزًا (علامة ✓ على المستخدم)، وزر «إلغاء تطبيق الثيمات» للرجوع للافتراضي.",
        ),
        en = listOf(
            "New \"Themes\" section in Settings > Appearance: pick an image as keyboard background, 18 ready-made themes (✓ on the active one) and a button to cancel the theme and return to default.",
        ),
    ),
    UiChangelogEntry(
        version = "c1cdd76",
        ar = listOf("أيقونة الإيموجي في شريط الأدوات: حلقة سمكها 3dp مع العينين والفم والباقي فراغ."),
        en = listOf("Emoji icon in the toolbar: a 3dp ring with the eyes and mouth, the rest empty."),
    ),
    UiChangelogEntry(
        version = "e9c8c38",
        ar = listOf("إضافة زر «حواف المفاتيح» في إعدادات لوحة المفاتيح (تحت الإزاحة السفلية): نافذة بشريط يتحكم في انحناء حواف أزرار الحروف والأرقام والرموز ولوحتي تعديل النص والأرقام، مع إعادة الضبط حسب الثيم."),
        en = listOf("Added the «Key corners» button in Keyboard settings (below bottom offset): a dialog with a slider for the corner roundness of the letter, number and symbol keys and the edit-text and numeric panels, with a reset to the theme."),
    ),
    UiChangelogEntry(
        version = "340bab9",
        ar = listOf("القائمة المنسدلة من الزرين 1 و2: الخيار المحدد يكبر 110% مع حركة سلسة، وتبتعد الخيارات المجاورة للحفاظ على المسافات (الألوان كما هي)."),
        en = listOf("The menu opened from slot buttons 1 and 2: the selected option grows 110% with a smooth animation and neighbours move away to keep the gaps (colours unchanged)."),
    ),
    UiChangelogEntry(
        version = "a23832c",
        ar = listOf("خيار «حجم أيقونات شريط الأدوات» في إعدادات الشريط الذكي يتحكم الآن في حجم شعار لوحة المفاتيح وأيقونتي الزرين 1 و2 وزر الإخفاء (100% هو الحجم الحالي)."),
        en = listOf("The «Toolbar icon size» option in Smartbar settings now controls the keyboard logo, slot buttons 1 and 2 and the hide button (100% is the current size)."),
    ),
    UiChangelogEntry(
        version = "c63798e",
        ar = listOf("أيقونة التطبيق بدقة أعلى لتظهر أوضح في قائمة التطبيقات على الشاشات عالية الكثافة."),
        en = listOf("App icon at a higher resolution so it looks sharper in the app list on high-density screens."),
    ),
    UiChangelogEntry(
        version = "33c5e2a",
        ar = listOf("أيقونة التطبيق في قائمة التطبيقات: رُفعت دقتها إلى الضعف لإزالة الضبابية."),
        en = listOf("App icon in the launcher: resolution doubled to remove the blur."),
    ),
    UiChangelogEntry(
        version = "91cc23c",
        ar = listOf("لوحة تعديل النص: انقسم الزر الطويل إلى زر مسافة وزر إدخال، وزر الإدخال يُنفّذ إجراء الحقل (إرسال، بحث، سطر جديد…) ويتبع لون زر الإدخال في لوحة الأحرف."),
        en = listOf("Edit-text panel: the tall space key is now split into a space key and an enter key; enter runs the editor action and follows the enter key colour of the letters keyboard."),
    ),
    UiChangelogEntry(
        version = "fbd590f",
        ar = listOf("النافذة المنبثقة عند الضغط المطول على زر في لوحة الحروف أصبحت أكبر بنسبة 15%."),
        en = listOf("The popup shown when long-pressing a key on the letters keyboard is now 15% bigger."),
    ),
    UiChangelogEntry(
        version = "e5bef3d",
        ar = listOf("«سجل التغييرات» في «حول التطبيق» يفتح الآن نافذة داخل التطبيق بقائمة التعديلات لكل نسخة بدل فتح الموقع."),
        en = listOf("«Changelog» in About now opens an in-app dialog listing the changes per version instead of the website."),
    ),
    UiChangelogEntry(
        version = "d62894d",
        ar = listOf(
            "في ميزة «النص المنسوخ حديثًا»: تظهر أيقونة الهاتف عند نسخ رقم هاتف.",
            "أيقونة النسخ صارت بجوار النص (مجموعة واحدة في وسط الحبة) بدل طرف الحبة.",
        ),
        en = listOf(
            "Recently copied text: a phone icon is shown when a phone number is copied.",
            "The copy icon now sits next to the text as one centred group instead of at the edge of the chip.",
        ),
    ),
    UiChangelogEntry(
        version = "3d3fcf0",
        ar = listOf("شعار جديد (الدرع) في أقصى يسار الشريط، بلون واحد يتبع لون اللوحة (أسود أو أبيض)."),
        en = listOf("New shield logo at the far left of the toolbar, single colour following the panel colour."),
    ),
    UiChangelogEntry(
        version = "25c619c",
        ar = listOf("شاشة افتتاح جديدة: الشعار كبير وكامل في المنتصف، ثم اسم التطبيق، ثم «تم التطوير بواسطة» و«Khaled Yagami»."),
        en = listOf("New opening screen: large uncropped logo in the middle, app name, then the developer credit."),
    ),
    UiChangelogEntry(
        version = "0330b91",
        ar = listOf("أيقونة جديدة للتطبيق بخلفية شفافة وبلا قص."),
        en = listOf("New app icon with a transparent background and no cropping."),
    ),
    UiChangelogEntry(
        version = "6a96222",
        ar = listOf("إعدادات الحافظة: قسم «النص المنسوخ حديثًا» فيه مدة الضغط المطول لإخفاء الحبة (من 1 إلى 10 ثوانٍ، الافتراضي 3)."),
        en = listOf("Clipboard settings: «Recently copied text» group with the hold time to dismiss the chip (1-10 s, default 3)."),
    ),
    UiChangelogEntry(
        version = "5d463b2",
        ar = listOf(
            "لوحة تعديل النص: زر الحذف (حرف واحد) لا يظهر باهتًا أبدًا.",
            "الضغط المطول على حبة النص المنسوخ يخفيها دون لصق.",
            "اتجاه أيقونة الحبة يتبع اتجاه النص، والنص الطويل يُقصّ من آخره فقط.",
        ),
        en = listOf(
            "Edit-text panel: the backspace key is never dimmed.",
            "Holding the recently copied chip dismisses it without pasting.",
            "The chip icon follows the text direction; long text is cut only at its end.",
        ),
    ),
    UiChangelogEntry(
        version = "01ad58a",
        ar = listOf(
            "إضافة ميزة «النص المنسوخ حديثًا»: حبة في الشريط بين الفاصلين تلصق النص بضغطة، ولها أولوية على اقتراحات الكلمات.",
            "خيار «الخط المستخدم» في المظهر: اختيار ملف خط (ttf/otf) مع معاينة وتكبير وتصغير وإعادة ضبط.",
            "لوحة الإيموجي: قائمة عمودية واحدة لكل الفئات، والتبويب النشط يتبع التمرير، والضغط على التبويب ينقلك لفئته.",
        ),
        en = listOf(
            "Recently copied text: a chip in the toolbar between the dividers pastes the text with one tap and has priority over word suggestions.",
            "«Keyboard font» in Appearance: pick a ttf/otf font with preview, size control and reset.",
            "Emoji panel: one vertical list for all categories, the active tab follows scrolling, tapping a tab scrolls to its category.",
        ),
    ),
    UiChangelogEntry(
        version = "1715d31",
        ar = listOf(
            "تغيير اسم التطبيق الظاهر إلى «UI Keyboard» في كل اللغات.",
            "قائمة اللغة: عرض 190dp، والعنصر المحدد يكبر 110% مع توهج وحركة سلسة وتحرك الجيران للحفاظ على المسافات.",
            "القوائم المنبثقة: أزرار 40dp وزوايا 6dp، وقائمة الأيقونات تبدأ من مكان الزر وتأخذ لون الثيم.",
            "خيار «تكرار المسافة» في الضغط المطول على المسافة (لوحة الحروف ولوحة تعديل النص).",
        ),
        en = listOf(
            "Visible app name changed to «UI Keyboard» in all languages.",
            "Language menu: 190dp wide, the selected item grows 110% with a glow and smooth animation, neighbours move to keep equal gaps.",
            "Popup menus: 40dp buttons and 6dp corners; the icon menu starts at the button and uses the theme colour.",
            "«Repeat space» option for long-pressing the space bar (letters keyboard and edit-text panel).",
        ),
    ),
    UiChangelogEntry(
        version = "7ac2ea2",
        ar = listOf(
            "تخصيص الأزرار: تظليل الزر المحدد، وملاءمة الأزرار للشاشة، وأدوات تعديل مضمّنة.",
            "لوحة تعديل النص: تعتيم المفاتيح التي لا يمكنها العمل (بلا تحديد أو بلا نص أو حافظة فارغة)، والأسهم وزر التحديد لا تُعتَّم.",
        ),
        en = listOf(
            "Key customization: selected key shading, keys fit the screen, inline editing tools.",
            "Edit-text panel: keys which cannot act are dimmed (no selection, no text, empty clipboard); arrows and select are never dimmed.",
        ),
    ),
    UiChangelogEntry(
        version = "1b2f9f5",
        ar = listOf(
            "زر الشعار يفتح لوحة قابلة للتمرير بكل إجراءات الشريط، وتتبع إعدادات أزرار الشريط (الترتيب والمخفي).",
            "الشريط: مقاس 140dp لصندوق اللغة، وفواصل، وأيقونات أكبر، وحجم الشريط لا يتبع إعداد ارتفاع الكيبورد.",
            "قائمة لأيقونات الميزات فوق الزر المضغوط، وأيقونة ميكروفون ولوحة تعديل نص جديدتان.",
        ),
        en = listOf(
            "The logo button opens a scrollable panel with all toolbar actions, following the toolbar button settings (order and hidden).",
            "Toolbar: 140dp language box, dividers, larger icons; the toolbar size no longer follows the keyboard height setting.",
            "Icon-only feature menu above the pressed button, new microphone and edit-text icons.",
        ),
    ),
    UiChangelogEntry(
        version = "5571516",
        ar = listOf(
            "أزرار الشريط قابلة للتخصيص (ضغطة وضغطة مطولة بقائمة).",
            "زر الإيموجي يفتح شاشة الإيموجي الأصلية، وإعادة فتح الكيبورد على الحروف بعد إغلاقه.",
        ),
        en = listOf(
            "Customizable toolbar slots (tap / long-press menu).",
            "The emoji button opens the original emoji screen; the keyboard reopens on letters after closing.",
        ),
    ),
    UiChangelogEntry(
        version = "63f8cc3",
        ar = listOf(
            "شريط أدوات جديد، وقائمة اللغة المنبثقة (على اليسار، ممتدة فوق الشريط وتحته).",
            "إضافة لوحة تعديل النص: تتحكم في مؤشر الكتابة ليتحرك في أي اتجاه، وتختار النص الذي تريد تحديده، مع نسخ وقص ولصق وبحث.",
            "إضافة لوحة الأرقام.",
        ),
        en = listOf(
            "New toolbar and the language popup menu (on the left, spanning above and below the toolbar).",
            "Added the edit-text panel: move the cursor in any direction and select the text you want, with copy, cut, paste and search.",
            "Added the numeric pad.",
        ),
    ),
    UiChangelogEntry(
        version = "dcc093f",
        ar = listOf(
            "الاسم الجديد وأيقونة التطبيق وقسم «حول المطور».",
            "إزالة أدوات المطور، وخيار حجم نص الاقتراحات.",
        ),
        en = listOf(
            "New app name and icon, and the «About the developer» section.",
            "Developer tools removed; suggestion text size option.",
        ),
    ),
    UiChangelogEntry(
        version = "7928141",
        ar = listOf("نقطة البداية: نسخة FlorisBoard v0.5.2 المستقرة مع تخصيص الشريط والأزرار."),
        en = listOf("Starting point: stable FlorisBoard v0.5.2 with toolbar and key customization."),
    ),
)

/** The changelog dialog opened from Settings > About > Changelog. */
@Composable
fun UiChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isArabic = context.resources.configuration.locales[0].language == "ar"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringRes(R.string.about__changelog__title)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(UiChangelogEntries) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringRes(R.string.ui__changelog__version, "v" to entry.version),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            for (line in if (isArabic) entry.ar else entry.en) {
                                Text(text = "• $line", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringRes(R.string.ui__changelog__close)) }
        },
    )
}
