# Developer & Agent Guidelines — MrMouseNavigation (1.21.8)

## 1. Project Overview & Identity
- **Mod Name:** MrMouseNavigation
- **Display Name in Mod Loaders:** `[MR] Mouse Navigation`
- **Mod ID:** `mrmousenavigation`
- **Current Version:** `1.0` (внутри мода и манифестах строго `1.0` без префикса `v`; в именах файлов `-v1.0.jar`)
- **Supported Minecraft Versions:** 1.21 through 1.21.11, 26.1 through 26.3 (Branches: `1.21`, ..., `26.3`)
- **Default / Current Branch:** `1.21.8`
- **Supported Loaders:** Pure Fabric Loader (>=0.19.5) and NeoForge (>=1.0.0) — **Zero extra dependencies required** (No Fabric API or NeoForge library dependencies needed).
- **Java Requirement:** Java 21 (сборка на JDK 25 с release = 21)
- **Build Output Artifacts:**
  - `MrMouseNavigation-Fabric-1.21.8-byMr712-v1.0.jar`
  - `MrMouseNavigation-NeoForge-1.21.8-byMr712-v1.0.jar`
- **Repository:** https://github.com/byMr712/MrMouseNavigation-MinecraftMod
- **Author:** byMr712

---

## 2. Multi-Loader Architecture: Fabric & NeoForge

Мод полностью кросс-платформенен и поддерживает автономную сборку под **Fabric** и **NeoForge** из единой кодовой базы:

### 2.1 Точки входа (Entrypoints)
1. **Fabric Loader:**
   - Реализует `net.fabricmc.api.ClientModInitializer` в классе `com.mr712.mrmousenavigation.MouseNavigationClient`.
   - Зарегистрирован в `fabric.mod.json` в секции `entrypoints.client`.
2. **NeoForge:**
   - Главный класс `com.mr712.mrmousenavigation.MouseNavigationNeoForge` аннотирован `@Mod("mrmousenavigation")`.
   - В конструкторе регистрирует расширение экрана настроек: `modContainer.registerExtensionPoint(IConfigScreenFactory.class, ...)`.
   - Для сборки через Fabric Loom используются легковесные стабы `net.neoforged.fml.common.Mod`, `ModContainer`, `IExtensionPoint` и `IConfigScreenFactory` (`src/neoforge/java/net/neoforged/`), которые при упаковке в JAR исключаются (`exclude "net/**"`), предотвращая конфликты с рантаймом NeoForge.
   - Метаданные мода объявлены в `src/neoforge/resources/META-INF/neoforge.mods.toml`.

### 2.2 Loader-Agnostic Конфигурация (`MouseNavigationConfig`)
- Путь к файлу конфигурации `mrmousenavigation.json` определяется динамически:
  - При наличии Fabric на лету запрашивается каталог через `FabricLoader.getInstance().getConfigDir()`.
  - При запуске под NeoForge или вне Fabric путь автоматически откатывается на стандартный `config/mrmousenavigation.json`.
- Жёсткие зависимости и импорты пакетов `net.fabricmc.loader` устранены, что предотвращает `NoClassDefFoundError` в среде NeoForge.

### 2.3 Универсальная система миксинов
- Обе платформы (Fabric и NeoForge) поддерживают **SpongePowered Mixin**.
- Конфигурация `mrmousenavigation.mixins.json` зарегистрирована:
  - В Fabric: через `"mixins": ["mrmousenavigation.mixins.json"]` в `fabric.mod.json`.
  - В NeoForge: через `[[mixins]] config = "mrmousenavigation.mixins.json"` в `neoforge.mods.toml`.
- Миксины инжектируются напрямую в ванильный класс обработки мыши Minecraft и работают без сторонних API.

### 2.4 Раздельная сборка в Gradle (Dual-Target Build)
- `src/main/` — общие классы мода в официальных маппингах Mojang (хэндлеры мыши, миксины, аксессоры, UI настроек, ресурсы, переводы, иконка).
- `src/neoforge/` — NeoForge точка входа (`MouseNavigationNeoForge`), метаданные `neoforge.mods.toml` и стабы FML/NeoForge.
- Задача `jar` / `remapJar` компилирует чистый Fabric JAR (`fabric.mod.json`, исключая NeoForge классы), автоматически ремапя код в Intermediary.
- Задача `jarNeoForge` компилирует чистый NeoForge JAR (`neoforge.mods.toml`, исключая `MouseNavigationClient.class`, `ModMenuIntegration`, стабы `com/terraformersmc/**`, `fabric.mod.json` и пакет стабов `net/**`).
- `build.dependsOn(jarNeoForge)` обеспечивает параллельную компиляцию обоих артефактов при запуске `./gradlew build`.
- Задачи `copyToAllBuildsFabric` и `copyToAllBuildsNeoForge` автоматически копируют готовые JAR в:
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\Fabric\`
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\NeoForge\`
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\` (корень)

### 2.5 Формат версий внутри мода: строго X.X (без префикса v)
- **Требование к версии внутри мода:** В обоих загрузчиках (как в NeoForge FML, так и в Fabric Loader) внутренняя версия мода должна быть строго в формате `X.X` (или SemVer `X.Y.Z`, например `1.0`), а не `vX.X`. Префикс `v` внутри манифестов приводит к ошибкам валидации версий загрузчиком NeoForge (`ArtifactVersion`) и некорректному отображению версий в меню модов.
- **Конфигурация:** В `gradle.properties` версия мода объявляется как `mod_version=1.0`.
- **Защита в сборщике (`build.gradle`):**
  - `version = project.mod_version.toString().replaceFirst(/^v/, '')`
  - Задача `processResources` для Fabric: `def version = project.version.toString().replaceFirst(/^v/, '')`
  - Задача `processNeoforgeResources` для NeoForge: `def neoforgeVersion = project.version.toString().replaceFirst(/^v/, '')`
  Это гарантирует, что даже если в свойствах появится префикс `v`, внутри `fabric.mod.json` и `neoforge.mods.toml` всегда попадёт чистая версия `X.X`.
- **Имена файлов:** Префикс `v` сохраняется только в имени результирующих JAR-файлов (`archives_base_name=...-v1.0`), например `MrMouseNavigation-Fabric-1.21.8-byMr712-v1.0.jar` и `MrMouseNavigation-NeoForge-1.21.8-byMr712-v1.0.jar`.

### 2.6 Меню настроек, иконка и домашняя страница в NeoForge
- **Интеграция экрана настроек (`IConfigScreenFactory`):** В отличие от Fabric, где экран конфигурации подключается через `ModMenuApi`, в NeoForge используется встроенная система расширений `IConfigScreenFactory`. Регистрация фабрики в `ModContainer` обеспечивает открытие `MouseNavigationConfigScreen` по нажатию кнопки **«Config»** в ванильном интерфейсе списка модов NeoForge.
- **Иконка мода (`logoFile`):** По спецификации Forge/NeoForge загрузчик и модификации каталогов (Catalogue) ищут файл иконки относительно корня JAR-файла. В манифесте задано `logoFile="icon.png"`, а при сборке `icon.png` копируется в корень архива.
- **Домашняя страница / Website (`displayURL`):** Для активности кнопки **«Homepage» / «Website»** во встроенном списке модов NeoForge и сторонних меню (Catalogue) в манифесте `neoforge.mods.toml` обязательно задаётся параметр `displayURL="https://github.com/byMr712/MrMouseNavigation-MinecraftMod"`.

### 2.7 Стратегия маппингов (Mojang Official Mappings vs Intermediary)
- **Принцип работы NeoForge:** Среда выполнения NeoForge 1.21+ работает исключительно на официальных маппингах Mojang (`net.minecraft.client.gui.screens.Screen`, `net.minecraft.client.MouseHandler` и т.д.). Использование Yarn-маппингов приводит к несовместимости классов и `NoClassDefFoundError`.
- **Конфигурация сборщика:**
  - В ветках 1.21.x используется `mappings loom.officialMojangMappings()`. Весь исходный код пишется на официальных маппингах Mojang.
  - Задача `jarNeoForge` упаковывает нативные классы в маппингах Mojang напрямую.
  - Fabric Loom через стандартную задачу `remapJar` автоматически ремапит те же самые скомпилированные классы в Intermediary для Fabric.
  - В ветках 26.x используется `loom { noIntermediateMappings() }` с локальным `empty-mappings.jar`.

---

## 3. Версия и стек в данной ветке
- **Minecraft:** 1.21.8
- **Маппинги:** Mojang Official (`loom.officialMojangMappings()`)
- **Fabric Loader:** >=0.19.5
- **NeoForge:** Поддерживается (отдельный автономный JAR)
- **Fabric Loom:** 1.18-SNAPSHOT
- **Fabric API / Сторонние API:** Не требуются (мод полностью автономен)
- **Java:** 21 (сборка на JDK 25 с release = 21)

---

## 4. Особенности реализации в данной версии
В Minecraft 1.21.8 используются официальные Mojang-маппинги и обновлённая архитектура книги рецептов:
- **Перехват кликов мыши:** `@Mixin(net.minecraft.client.MouseHandler.class)` -> инжекция в метод `onPress(long window, int button, int action, int mods)`.
- **Книга рецептов:** Начиная с 1.21.2 интерфейс `RecipeBookProvider`/`RecipeUpdateListener` для получения книги рецептов заменён на базовый класс экранов `net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen<?>`. Виджет книги рецептов получается через аксессор `AbstractRecipeBookScreenAccessor.mrmousenavigation$getRecipeBookComponent()`, а переключение страниц — через `RecipeBookComponentAccessor` и `RecipeBookPageAccessor`.
- **Творческий инвентарь:** `CreativeModeInventoryScreenAccessor.mrmousenavigation$getSelectedTab()`, выбор вкладки через `mrmousenavigation$selectTab(targetTab)` из списка `CreativeModeTabs.tabs()`.
- **Достижения:** `AdvancementsScreenAccessor.mrmousenavigation$getTabs()`, переключение вкладки через `advScreen.onSelectedTabChanged(holder)` и `clientAdvancements.setSelectedTab(holder, true)`.
- **Клавиши:** Нажатие клавиш вызывается через `currentScreen.keyPressed(int keyCode, int scanCode, int modifiers)`.
- **Звук клика:** `SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)`.

---

## 5. Логика навигации
1. **Кнопка «Назад» (Mouse Button 4 / XBUTTON1 / нижняя боковая):**
   - Закрытие экранов и меню (эквивалент клавиши `Esc`).
   - Книги и кафедры: перелистывание назад.
   - Книга рецептов: переключение страниц рецептов назад.
   - Творческий инвентарь: переключение вкладок категорий назад.
   - Экран достижений: переключение между вкладками достижений (ванильные вкладки или мод Better Advancements).
   - Чат (`ChatScreen`): история отправленных сообщений назад (стрелка `↑`).
2. **Кнопка «Вперёд» (Mouse Button 5 / XBUTTON2 / верхняя боковая):**
   - Перелистывание страниц книг и кафедр вперёд.
   - Переключение страниц книги рецептов вперёд.
   - Переключение вкладок творческого инвентаря вперёд.
   - Экран достижений: переключение между вкладками достижений.
   - Чат: история сообщений вперёд (стрелка `↓`).
   - История экранов: возвращение во вложенный экран, если вы вышли из него назад.
3. **Средняя кнопка мыши (СКМ / Mouse Button 3 / колёсико):**
   - В чате (`ChatScreen`): мгновенная отправка сообщения (эквивалент клавиши `Enter`).
4. **Конфигурация:**
   - Интеграция с ModMenu (Fabric) и встроенным меню модов NeoForge (кнопка «Config»).
   - Экран настроек: `MouseNavigationConfigScreen`.
   - Конфигурационный файл: `config/mrmousenavigation.json`.

---

## 6. Структура проекта

```
MrMouseNavigation-MinecraftMod/
├── src/main/java/com/mr712/mrmousenavigation/
│   ├── MouseNavigationClient.java                 # Fabric ClientModInitializer точка входа
│   ├── config/
│   │   └── MouseNavigationConfig.java             # Загрузчик конфигурации (Loader-agnostic)
│   ├── gui/
│   │   └── MouseNavigationConfigScreen.java       # Графический интерфейс настроек (Mojang Screen)
│   ├── handler/
│   │   └── MouseNavigationHandler.java            # Центральная логика обработки мыши
│   ├── integration/
│   │   └── ModMenuIntegration.java                # Интеграция с ModMenu (только для Fabric)
│   └── mixin/                                     # Миксины и аксессоры
│       ├── AbstractRecipeBookScreenAccessor.java
│       ├── AdvancementsScreenAccessor.java
│       ├── CreativeModeInventoryScreenAccessor.java
│       ├── MouseMixin.java
│       ├── RecipeBookComponentAccessor.java
│       └── RecipeBookPageAccessor.java
├── src/main/java/com/terraformersmc/modmenu/api/   # Стабы ModMenu API для компиляции
│   ├── ConfigScreenFactory.java
│   └── ModMenuApi.java
├── src/main/resources/
│   ├── fabric.mod.json                            # Манифест мода для Fabric
│   ├── mrmousenavigation.mixins.json              # Конфигурация Mixin (общая для Fabric и NeoForge)
│   └── assets/mrmousenavigation/                  # Иконка и локализации (ru_ru, en_us)
├── src/neoforge/java/
│   ├── com/mr712/mrmousenavigation/
│   │   └── MouseNavigationNeoForge.java           # NeoForge точка входа с @Mod("mrmousenavigation")
│   └── net/neoforged/                             # Стабы FML/NeoForge для компиляции
│       ├── fml/IExtensionPoint.java
│       ├── fml/ModContainer.java
│       ├── fml/common/Mod.java
│       └── neoforge/client/gui/IConfigScreenFactory.java
├── src/neoforge/resources/
│   └── META-INF/neoforge.mods.toml                # Манифест мода для NeoForge (displayURL, logoFile, mixins)
├── LICENSE                                        # Apache-2.0 License
├── gradle.properties                              # Свойства версии, Fabric Loader (0.19.5), Java 21
└── build.gradle                                   # Двойная сборка Fabric и NeoForge
```

---

## 7. Команды сборки

- **Полная сборка обоих загрузчиков:**
  ```bash
  ./gradlew build
  ```
- **Выходные артефакты в `build/libs/`:**
  - `MrMouseNavigation-Fabric-1.21.8-byMr712-v1.0.jar`
  - `MrMouseNavigation-NeoForge-1.21.8-byMr712-v1.0.jar`
- **Автоматическая доставка в архив всех сборок:**
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\Fabric\`
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\NeoForge\`
  - `C:\Users\Mr712\Documents\MrMouseNavigation_all_builds\`
