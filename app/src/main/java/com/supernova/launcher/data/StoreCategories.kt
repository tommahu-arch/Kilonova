package com.supernova.launcher.data

// The app-store categories a drawer group can take apps from, and the
// groups a first run can make for them. A category is a key: "play:" and a
// Play Store category id (MUSIC_AND_AUDIO), "fdroid:" and an F-Droid
// category name (Music Practice Tool), or one of the two "special:" buckets
// for what no store sorted. A key belongs to at most one group.
//
// Added for Kilonova; not part of upstream Supernova.
object StoreCategories {
    const val PLAY = "play:"
    const val FDROID = "fdroid:"
    const val SPECIAL = "special:"
    val keyPrefixes = listOf(PLAY, FDROID, SPECIAL)

    // com.samsung.* apps no store category placed, and apps nothing placed.
    const val SAMSUNG = SPECIAL + "samsung"
    const val UNSORTED = SPECIAL + "unsorted"

    fun play(id: String) = PLAY + id
    fun fdroid(name: String) = FDROID + name

    // Keys sit in a comma-separated list inside a '|'-separated entry.
    fun escape(key: String): String =
        key.replace("%", "%25").replace(",", "%2C").replace("|", "%7C")

    fun unescape(text: String): String =
        text.replace("%7C", "|").replace("%2C", ",").replace("%25", "%")

    class Play(val id: String, val label: String, val game: Boolean = false)

    // Google Play's categories (apps, then games), with the names the Play
    // Store shows. Ids Play adds later still work: the page reads any id it
    // finds; only the picker's list is fixed.
    val playCategories: List<Play> = listOf(
        Play("ART_AND_DESIGN", "Art & Design"),
        Play("AUTO_AND_VEHICLES", "Auto & Vehicles"),
        Play("BEAUTY", "Beauty"),
        Play("BOOKS_AND_REFERENCE", "Books & Reference"),
        Play("BUSINESS", "Business"),
        Play("COMICS", "Comics"),
        Play("COMMUNICATION", "Communication"),
        Play("DATING", "Dating"),
        Play("EDUCATION", "Education"),
        Play("ENTERTAINMENT", "Entertainment"),
        Play("EVENTS", "Events"),
        Play("FINANCE", "Finance"),
        Play("FOOD_AND_DRINK", "Food & Drink"),
        Play("HEALTH_AND_FITNESS", "Health & Fitness"),
        Play("HOUSE_AND_HOME", "House & Home"),
        Play("LIBRARIES_AND_DEMO", "Libraries & Demo"),
        Play("LIFESTYLE", "Lifestyle"),
        Play("MAPS_AND_NAVIGATION", "Maps & Navigation"),
        Play("MEDICAL", "Medical"),
        Play("MUSIC_AND_AUDIO", "Music & Audio"),
        Play("NEWS_AND_MAGAZINES", "News & Magazines"),
        Play("PARENTING", "Parenting"),
        Play("PERSONALIZATION", "Personalization"),
        Play("PHOTOGRAPHY", "Photography"),
        Play("PRODUCTIVITY", "Productivity"),
        Play("SHOPPING", "Shopping"),
        Play("SOCIAL", "Social"),
        Play("SPORTS", "Sports"),
        Play("TOOLS", "Tools"),
        Play("TRAVEL_AND_LOCAL", "Travel & Local"),
        Play("VIDEO_PLAYERS", "Video Players & Editors"),
        Play("WEATHER", "Weather"),
        Play("GAME", "Games (general)", game = true),
        Play("GAME_ACTION", "Action", game = true),
        Play("GAME_ADVENTURE", "Adventure", game = true),
        Play("GAME_ARCADE", "Arcade", game = true),
        Play("GAME_BOARD", "Board", game = true),
        Play("GAME_CARD", "Card", game = true),
        Play("GAME_CASINO", "Casino", game = true),
        Play("GAME_CASUAL", "Casual", game = true),
        Play("GAME_EDUCATIONAL", "Educational", game = true),
        Play("GAME_MUSIC", "Music", game = true),
        Play("GAME_PUZZLE", "Puzzle", game = true),
        Play("GAME_RACING", "Racing", game = true),
        Play("GAME_ROLE_PLAYING", "Role playing", game = true),
        Play("GAME_SIMULATION", "Simulation", game = true),
        Play("GAME_SPORTS", "Sports", game = true),
        Play("GAME_STRATEGY", "Strategy", game = true),
        Play("GAME_TRIVIA", "Trivia", game = true),
        Play("GAME_WORD", "Word", game = true)
    )

    // F-Droid's own categories. Broad ones are catch-alls that say little
    // about what an app is (System, Internet, ...); the rest are narrow.
    // Names are as the F-Droid index spells them.
    val fdroidBroad: Set<String> = setOf(
        "System", "Internet", "Multimedia", "Science & Education", "Connectivity", "Security",
        "Sports & Health", "Reading", "Writing", "Development", "Graphics", "Theming"
    )

    val fdroidNarrow: List<String> = listOf(
        "Action Game", "AI Chat", "Alarm Clock", "Ambient Sound", "App Manager", "App Store & Updater",
        "Audiobook", "Battery", "Board Game", "Bookmark", "Browser", "Calculator", "Calendar & Agenda",
        "Camera", "Card Game", "Cast", "Casual Game", "Clock", "Cloud Storage & File Sync", "Code & Forge",
        "Contact", "Dice", "Diet", "Digital Wellbeing", "DNS & Hosts", "Download", "Draw", "Ebook Reader",
        "Educational Game", "Email", "Emergency Action", "Emulator", "File Encryption & Vault",
        "File Manager", "File Transfer", "Finance Manager", "Firewall", "Flashlight", "Forum", "Gallery",
        "Game Helper", "Habit Tracker", "Health Manager", "Icon Pack", "Inventory", "Keyboard & IME",
        "Launcher", "Local Media Player", "Location Tracker & Sharer", "Lyrics", "Market & Price",
        "Medication", "Meditation", "Mental Health", "Messaging", "Music Practice Tool", "Navigation",
        "Network Analyzer", "News", "Note", "Notification", "OCR", "Online Media Player", "Party Game",
        "Pass Wallet", "Password & 2FA", "Phone & SMS", "Platformer Game", "Podcast", "Public Transport",
        "Push", "Puzzle Game", "Radio", "Recipe Manager", "Recorder", "Religion", "Remote Access",
        "Remote Controller", "Role-Playing Game", "Schedule", "Shooter Game", "Shopping List",
        "Social Network", "Speech Recognizer", "Sport Game", "Stopwatch", "Strategy Game", "Task",
        "Text Editor", "Text Encryption", "Text to Speech", "Time Tracker", "Timer",
        "Translation & Dictionary", "Unit Convertor", "Visual Novel", "Voice & Video Chat", "Volume",
        "VPN & Proxy", "Wallet", "Wallpaper", "Weather", "Word Game", "Workout"
    )

    fun isBroad(fdroidName: String): Boolean = fdroidName in fdroidBroad

    // What the picker and the rows call a key.
    fun label(key: String): String = when {
        key == SAMSUNG -> "Samsung built-in apps"
        key == UNSORTED -> "Unsorted apps"
        key.startsWith(PLAY) -> key.removePrefix(PLAY).let { id ->
            playCategories.firstOrNull { it.id == id }?.label ?: id
        }
        key.startsWith(FDROID) -> key.removePrefix(FDROID)
        else -> key
    }

    // A group a first run makes: the Play category it is named for, and the
    // F-Droid categories mapped to it.
    class Preset(val name: String, val play: List<String>, val fdroid: List<String>)

    private val gameIds = playCategories.filter { it.game }.map { it.id }

    // Names and ids follow Google Play; the F-Droid column is Kilonova's own
    // mapping, an arbitrary choice the user can edit group by group. F-Droid
    // categories left out (Game Helper) fall through to the next source.
    val presets: List<Preset> = listOf(
        Preset("Art & Design", listOf("ART_AND_DESIGN"), listOf("Draw", "Graphics")),
        Preset("Auto & Vehicles", listOf("AUTO_AND_VEHICLES"), emptyList()),
        Preset("Beauty", listOf("BEAUTY"), emptyList()),
        Preset(
            "Books & Reference", listOf("BOOKS_AND_REFERENCE"),
            listOf("Ebook Reader", "Reading", "Translation & Dictionary")
        ),
        Preset("Business", listOf("BUSINESS"), listOf("Inventory", "Time Tracker")),
        Preset("Comics", listOf("COMICS"), emptyList()),
        Preset(
            "Communication", listOf("COMMUNICATION"),
            listOf(
                "Messaging", "Phone & SMS", "Email", "Voice & Video Chat", "Contact", "Forum", "Push",
                "Browser", "Bookmark", "Internet"
            )
        ),
        Preset("Dating", listOf("DATING"), emptyList()),
        Preset("Education", listOf("EDUCATION"), listOf("Science & Education")),
        Preset("Entertainment", listOf("ENTERTAINMENT"), listOf("Multimedia")),
        Preset("Events", listOf("EVENTS"), emptyList()),
        Preset(
            "Finance", listOf("FINANCE"),
            listOf("Finance Manager", "Market & Price", "Wallet", "Pass Wallet")
        ),
        Preset("Food & Drink", listOf("FOOD_AND_DRINK"), listOf("Recipe Manager", "Diet")),
        Preset(
            "Health & Fitness", listOf("HEALTH_AND_FITNESS"),
            listOf(
                "Health Manager", "Workout", "Sports & Health", "Meditation", "Mental Health",
                "Digital Wellbeing"
            )
        ),
        Preset("House & Home", listOf("HOUSE_AND_HOME"), emptyList()),
        Preset("Libraries & Demo", listOf("LIBRARIES_AND_DEMO"), emptyList()),
        Preset("Lifestyle", listOf("LIFESTYLE"), listOf("Religion", "Emergency Action")),
        Preset(
            "Maps & Navigation", listOf("MAPS_AND_NAVIGATION"),
            listOf("Navigation", "Public Transport", "Location Tracker & Sharer")
        ),
        Preset("Medical", listOf("MEDICAL"), listOf("Medication")),
        Preset(
            "Music & Audio", listOf("MUSIC_AND_AUDIO"),
            listOf("Podcast", "Radio", "Audiobook", "Lyrics", "Ambient Sound", "Music Practice Tool", "Recorder")
        ),
        Preset("News & Magazines", listOf("NEWS_AND_MAGAZINES"), listOf("News")),
        Preset("Parenting", listOf("PARENTING"), emptyList()),
        Preset(
            "Personalization", listOf("PERSONALIZATION"),
            listOf("Launcher", "Icon Pack", "Theming", "Wallpaper")
        ),
        Preset("Photography", listOf("PHOTOGRAPHY"), listOf("Camera", "Gallery")),
        Preset(
            "Productivity", listOf("PRODUCTIVITY"),
            listOf(
                "Note", "Task", "Schedule", "Calendar & Agenda", "Text Editor", "Writing", "Habit Tracker",
                "Development", "Code & Forge", "OCR"
            )
        ),
        Preset("Shopping", listOf("SHOPPING"), listOf("Shopping List")),
        Preset("Social", listOf("SOCIAL"), listOf("Social Network")),
        Preset("Sports", listOf("SPORTS"), emptyList()),
        Preset(
            "Tools", listOf("TOOLS"),
            listOf(
                "System", "Connectivity", "Security", "Password & 2FA", "VPN & Proxy", "Firewall", "DNS & Hosts",
                "File Manager", "File Transfer", "Cloud Storage & File Sync", "Keyboard & IME", "Calculator",
                "Unit Convertor", "Flashlight", "Clock", "Alarm Clock", "Stopwatch", "Timer", "Battery",
                "Volume", "Notification", "Network Analyzer", "App Manager", "App Store & Updater",
                "Download", "Text Encryption", "File Encryption & Vault", "Speech Recognizer",
                "Text to Speech", "Remote Controller", "Remote Access", "AI Chat"
            )
        ),
        Preset("Travel & Local", listOf("TRAVEL_AND_LOCAL"), emptyList()),
        Preset(
            "Video Players & Editors", listOf("VIDEO_PLAYERS"),
            listOf("Local Media Player", "Online Media Player", "Cast")
        ),
        Preset("Weather", listOf("WEATHER"), listOf("Weather")),
        Preset(
            "Games", gameIds,
            listOf(
                "Action Game", "Shooter Game", "Board Game", "Dice", "Card Game", "Casual Game", "Party Game",
                "Educational Game", "Platformer Game", "Emulator", "Puzzle Game", "Role-Playing Game",
                "Sport Game", "Strategy Game", "Visual Novel", "Word Game"
            )
        )
    )

    const val SAMSUNG_NAME = "Samsung built-in apps"
    const val UNSORTED_NAME = "Unsorted apps"
}
