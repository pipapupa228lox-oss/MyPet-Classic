# MyPetClassic

Легендарный MyPet, возрождённый для Paper 1.21.5+.

## Статус
Форк MyPet 3 (End-of-Life). Активно развивается.

## Поддерживаемые версии

| Minecraft | Модуль NMS |
|---|---|
| 1.21.5 | `nms/v1_21_R4` |
| 1.21.6 – 1.21.8 | `nms/v1_21_R5` |
| 1.21.9 – 1.21.10 | `nms/v1_21_R6` |
| 1.21.11 | `nms/v1_21_R7` |

Поддерживаются Paper, его форки (Purpur, Pufferfish и т.п.) и Folia (`folia-supported: true`).
Forge-Bukkit гибриды (Arclight, Mohist) не поддерживаются: плагин выключится с понятным сообщением.

## Folia

Всё планирование идёт через `SchedulerManager` ([SchedulerManager.java](api/src/main/java/de/Keyle/MyPet/api/util/SchedulerManager.java)),
который сам выбирает нужный планировщик:

* глобальные задачи (загрузка конфигов, тик репозитория, регистрация сущностей) — `GlobalRegionScheduler`;
* работа с БД и сетью — `AsyncScheduler`;
* тик питомца и игрока — планировщик сущности владельца (`EntityScheduler`), поэтому каждое животное
  обрабатывается в своём регионе;
* спавн, деспавн, обновление экипировки, GUI, эффекты навыков — планировщики сущности/региона.

`BukkitRunnable` заменён на `CompatRunnable` ([CompatRunnable.java](api/src/main/java/de/Keyle/MyPet/api/util/CompatRunnable.java)):
на Paper он ведёт себя как обычный `BukkitRunnable`, на Folia — уходит в нужный регион.

Проверено на Folia 1.21.11: загрузка, `folia-supported`, регистрация NMS-сущностей, `/mypet`,
`/mypet reload all`, чистое выключение — без исключений в логе.

Известные ограничения Folia: сторонние хуки (Citizens, WorldEdit-подобные, региональные плагины)
могут обращаться к чужому региону; многомировые питомцы при телепортации между мирами требуют
перезагрузки чанков владельца.

Minecraft 1.21.4 и ниже пока не поддерживаются: для них нужен отдельный NMS-модуль `v1_21_R3`,
которого в этой сборке нет.

## Сообщения и MiniMessage

Все сообщения игроку (чат, action bar, названия питомцев, GUI) собираются в Adventure `Component` через
[MessageRenderer.java](api/src/main/java/de/Keyle/MyPet/api/util/message/MessageRenderer.java).
В одной строке можно смешивать:

* MiniMessage: `<gold>`, `<bold>`, `<gradient:#FFB6C1:#E75480>Мой питомец</gradient>`;
* коды `&`/`§`: `&6`, `§l`, `&#FFB6C1`;
* старые угловые теги: `<red>`, `<dark_red>`, `<a>`.

Пример: `/petname <gradient:#FFB6C1:#E75480>Мой питомец</gradient>`.

Если установлен PlaceholderAPI, плейсхолдеры подставляются в названия питомцев, nametag и сообщения:
`/petname %player_name%` (PlaceholderAPI указан в `softdepend` в [plugin.yml](src/main/resources/plugin.yml)).

## Установка

1. Нужна Java 21 и сервер Paper (или его форк: Purpur, Pufferfish, Folia) версии 1.21.5–1.21.11.
2. Положите `MyPetClassic-<версия>.jar` в папку `plugins/` и перезапустите сервер.
3. При первом запуске создаётся `plugins/MyPetClassic/` с конфигами: `config.yml`, `pet-config.yml`,
   `exp-config.yml`, `hooks-config.yml`, `worldgroups.yml`, `pet-shops.yml`.
4. Хранилище по умолчанию — SQLite (`MyPet.Repository.Type: SQLite`). MySQL и MongoDB настраиваются
   в том же разделе `config.yml`; данные в базу пишутся асинхронно.

Плагин собирает анонимную статистику через bStats (id `34874`); она отключается в `plugins/bStats/config.yml`.

## Перенос данных из MyPet 3

При первом запуске плагин сам копирует данные старой установки из `plugins/MyPet` в
`plugins/MyPetClassic`: `config.yml`, `pet-config.yml`, `exp-config.yml`, `hooks-config.yml`,
`worldgroups.yml`, `pet-shops.yml`, `exp.js`, `pets.db`, `My.Pets`, `My.Pets.old`, а также папки
`skilltrees/` и `locale/`.

Перенос идёт только в пустую установку: если в `plugins/MyPetClassic` уже есть `config.yml` или
`pets.db`, ничего не копируется. Старая папка не изменяется — её можно удалить после проверки, что
питомцы и настройки на месте. Если новая версия уже запускалась, скопируйте файлы вручную и
перезапустите сервер.

## Обновления

Источник обновлений — [Modrinth](https://modrinth.com/mod/mypet-classic). При старте плагин
спрашивает последнюю версию для вашей версии Minecraft; ответ кэшируется только на время запуска.

| Ключ | По умолчанию | Что делает |
|---|---|---|
| `MyPet.Update.Check` | `true` | проверять обновления при старте |
| `MyPet.Update.OP-Notification` | `true` | сообщать операторам при входе на сервер |
| `MyPet.Update.Download` | `false` | скачивать обновление автоматически |
| `MyPet.Update.ReplaceOld` | `false` | класть скачанный jar сразу в `update/` для замены |
| `MyPet.Update.In-Background` | `false` | качать в фоновом потоке |

Скачанный файл проверяется по SHA-512 из API Modrinth и попадает в
`plugins/MyPetClassic/update/`, поэтому сломанная загрузка не заменит рабочий плагин.
Версию и ссылку на страницу обновления можно посмотреть командой `/mypet update` (право
`MyPet.admin`). Сборки старой ветки 3.14.x не предлагаются установкам 1.x.

## Команды

| Команда | Алиасы | Назначение |
|---|---|---|
| `/mypet` | | информация и справка |
| `/mypet reload all` | | перезагрузка конфигов (право `MyPet.admin`) |
| `/mypet update` | | проверка обновлений (право `MyPet.admin`) |
| `/petcall` | `petc`, `pc` | позвать питомца |
| `/petsendaway` | `petsa`, `psa` | отправить питомца в хранилище |
| `/petrelease <имя>` | | отпустить питомца |
| `/petstop` | `pets`, `ps` | запретить питомцу атаковать |
| `/petpickup` | `petp`, `pp` | включить/выключить подбор предметов |
| `/petinventory` | `peti`, `pi` | инвентарь питомца |
| `/petbehavior` | `petb`, `pb` | режим поведения |
| `/petname <имя>` | | переименовать питомца |
| `/petinfo [игрок]` | | информация о питомце |
| `/petskill` | | навыки питомца |
| `/petchooseskilltree` | | выбрать дерево навыков |
| `/petbeacon` | | меню маяка питомца |
| `/petrespawn` | | возродить питомца |
| `/petcapturehelper` | | подсказки по приручению |
| `/petsettings` | | настройки питомца |
| `/petswitch` | | переключиться на другого питомца |
| `/petstore` | | убрать питомца в хранилище |
| `/petlist` | | список питомцев |
| `/pettrade` | | обмен питомцами |
| `/petshop` | | магазин питомцев |
| `/mypetadmin` | | админ-команды |

## Права

| Право | Назначение |
|---|---|
| `MyPet.*` | все обычные возможности плагина |
| `MyPet.admin` | админ-команды и обход ограничений |
| `MyPet.command.name`, `MyPet.command.name.color` | переименование и цветные имена |
| `MyPet.command.switch`, `MyPet.command.store`, `MyPet.command.list` | хранилище питомцев |
| `MyPet.extended.ride`, `MyPet.extended.ride.fly`, `MyPet.extended.control` | езда на питомце и управление |
| `MyPet.extended.feed`, `.equip`, `.inventory`, `.beacon`, `.pickup`, `.nametag` | расширенные действия |
| `MyPet.extended.behavior.*` | режимы поведения (`friendly`, `aggressive`, `farm`, `raid`, `duel`) |
| `MyPet.shop.storage` | покупка питомца при уже активном питомце |
| `MyPet.npc.shop`, `MyPet.npc.storage` | NPC-трейты Citizens |
| `MyPet.petstorage.limit.<N>` | лимит одновременно хранимых питомцев |
| `MyPet.experience.multiplier.<125…250>` | множитель опыта питомца |

Полный список с описаниями — в [plugin.yml](src/main/resources/plugin.yml).

## Тесты

```bash
./gradlew test
```

JUnit 5 покрывает сравнение версий для проверки обновлений (`api`) и перенос данных из MyPet 3
(`plugin`). Тесты входят в `./gradlew build`.

## Сборка
Требуется JDK 21 (модули `nms/v26_*` требуют JDK 25 и по умолчанию исключены из сборки — они остаются
на диске для будущих версий Minecraft).

```bash
./gradlew clean build
```

Готовый JAR: `build/libs/MyPetClassic-<версия>.jar`.

Во время сборки переводы скачиваются из репозитория
[MyPet-Translations](https://github.com/MyPetORG/MyPet-Translations) (нужны `git` и сеть).

## Тестовый сервер

```bash
mkdir -p test-server/plugins
curl -L -o test-server/paper-1.21.5.jar \
  "https://fill.papermc.io/v3/projects/paper/versions/1.21.5/builds/latest"
echo "eula=true" > test-server/eula.txt
cp build/libs/MyPetClassic-*.jar test-server/plugins/
cd test-server && java -Xmx2G -jar paper-1.21.5.jar --nogui
```

## Лицензия
LGPL-3.0 (как у оригинала).

## Оригинал
https://github.com/MyPetORG/MyPet
