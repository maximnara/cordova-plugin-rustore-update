# Cordova плагин для обновления приложения через RuStore

## Описание
Плагин для интеграции функционала обновления приложений через RuStore в Cordova/PhoneGap приложения.

## Установка

```bash
cordova plugin add ../cordova-plugin-rustore-update
```

## Основное использование

```javascript
// Проверка обновлений
RustoreUpdate.getAppUpdateInfo()
    .then(function(updateInfo) {
        console.log('Update info:', updateInfo);
    })
    .catch(function(error) {
        console.error('Error:', error);
    });

// Запуск процесса обновления
RustoreUpdate.startUpdateFlow({ updateType: 'FLEXIBLE' })
    .then(function(result) {
        console.log('Update completed:', result);
    })
    .catch(function(error) {
        console.error('Update failed:', error);
    });
```

## Методы API

### getAppUpdateInfo()
Проверяет наличие обновлений для приложения.

**Параметры:**
- Нет параметров

**Возвращает:** `Promise<UpdateInfo>`

**Структура ответа UpdateInfo:**
```typescript
interface UpdateInfo {
    // Сырые значения от SDK
    updateAvailability: number;        // Код доступности обновления (0-3)
    installStatus: number;              // Код статуса установки (0-11)
    availableVersionCode: number;       // Код доступной версии

    // Вспомогательные поля
    isUpdateAvailable: boolean;        // true если обновление доступно
    isUpdateReadyToInstall: boolean;   // true если обновление готово к установке

    // Текстовые представления
    updateAvailabilityText: string;    // Текстовое представление доступности
    installStatusText: string;         // Текстовое представление статуса
}
```

**Пример использования:**
```javascript
RustoreUpdate.getAppUpdateInfo()
    .then(function(updateInfo) {
        if (updateInfo.isUpdateAvailable) {
            console.log('Доступно обновление!');
            console.log('Код версии:', updateInfo.availableVersionCode);
        }

        // Проверка через константы
        if (updateInfo.updateAvailability === RustoreUpdate.UpdateAvailability.UPDATE_AVAILABLE) {
            console.log('Обновление доступно');
        }
    })
    .catch(function(error) {
        console.error('Ошибка проверки:', error);
    });
```

### startUpdateFlow(params)
Запускает процесс обновления приложения.

**Параметры:**
```typescript
interface StartUpdateFlowParams {
    updateType: 'FLEXIBLE' | 'IMMEDIATE';  // Тип обновления (по умолчанию 'FLEXIBLE')
}
```

**Возвращает:** `Promise<UpdateFlowResult>`

**Структура успешного ответа UpdateFlowResult:**
```typescript
interface UpdateFlowResult {
    resultCode: number;         // Код результата (-1 = успех, 0 = отменено, 1 = ошибка)
    resultMessage: string;      // Сообщение о результате
    updateType: string;         // Тип обновления который был запрошен
}
```

**Структура ошибки:**
```typescript
interface UpdateFlowError {
    error: string;              // Код ошибки
    message: string;            // Описание ошибки
    resultCode?: number;        // Код результата (если есть)
    installErrorCode?: number;  // Код ошибки установки (если есть)
}
```

**Пример использования:**
```javascript
// Гибкое обновление (пользователь может продолжать работу)
RustoreUpdate.startUpdateFlow({ updateType: 'FLEXIBLE' })
    .then(function(result) {
        if (result.resultCode === RustoreUpdate.ActivityResult.RESULT_OK) {
            console.log('Обновление запущено успешно');
        }
    })
    .catch(function(error) {
        if (error.error === RustoreUpdate.UpdateFlowError.UPDATE_CANCELED_BY_USER) {
            console.log('Пользователь отменил обновление');
        } else {
            console.error('Ошибка обновления:', error.message);
        }
    });

// Немедленное обновление (блокирует приложение)
RustoreUpdate.startUpdateFlow({ updateType: 'IMMEDIATE' })
    .then(function(result) {
        // Этот код может не выполниться, так как приложение перезапустится
        console.log('Обновление завершено');
    })
    .catch(function(error) {
        console.error('Ошибка:', error);
    });
```

## Константы

### UpdateAvailability
Коды доступности обновления:
```javascript
RustoreUpdate.UpdateAvailability = {
    UNKNOWN: 0,                                    // Неизвестно
    UPDATE_NOT_AVAILABLE: 1,                       // Обновление недоступно
    UPDATE_AVAILABLE: 2,                           // Обновление доступно
    DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS: 3      // Обновление в процессе
}
```

### InstallStatus
Коды статуса установки:
```javascript
RustoreUpdate.InstallStatus = {
    UNKNOWN: 0,         // Неизвестно
    PENDING: 1,         // Ожидание
    DOWNLOADING: 2,     // Загрузка
    INSTALLING: 3,      // Установка
    INSTALLED: 4,       // Установлено
    FAILED: 5,          // Ошибка
    CANCELED: 6,        // Отменено
    DOWNLOADED: 11      // Загружено
}
```

### UpdateType
Типы обновления:
```javascript
RustoreUpdate.UpdateType = {
    FLEXIBLE: 'FLEXIBLE',   // Гибкое (пользователь может продолжать работу)
    IMMEDIATE: 'IMMEDIATE'  // Немедленное (блокирует приложение)
}
```

### ActivityResult
Коды результата активности:
```javascript
RustoreUpdate.ActivityResult = {
    RESULT_OK: -1,                      // Успех
    RESULT_CANCELED: 0,                 // Отменено
    RESULT_IN_APP_UPDATE_FAILED: 1      // Ошибка обновления
}
```

### UpdateFlowResult
Сообщения о результатах:
```javascript
RustoreUpdate.UpdateFlowResult = {
    IMMEDIATE_UPDATE_SUCCESS: 'IMMEDIATE_UPDATE_SUCCESS',  // Успешное немедленное обновление
    UPDATE_CANCELED_BY_USER: 'UPDATE_CANCELED_BY_USER',    // Отменено пользователем
    UPDATE_DOWNLOAD_FAILED: 'UPDATE_DOWNLOAD_FAILED'       // Ошибка загрузки
}
```

### UpdateFlowError
Коды ошибок:
```javascript
RustoreUpdate.UpdateFlowError = {
    UPDATE_FLOW_FAILED: 'UPDATE_FLOW_FAILED',              // Общая ошибка процесса
    UPDATE_FLOW_START_ERROR: 'UPDATE_FLOW_START_ERROR',    // Ошибка запуска
    UPDATE_CHECK_FAILED: 'UPDATE_CHECK_FAILED',            // Ошибка проверки
    UPDATE_FLOW_ERROR: 'UPDATE_FLOW_ERROR',                // Общая ошибка
    MANAGER_CREATE_FAILED: 'MANAGER_CREATE_FAILED',        // Ошибка создания менеджера
    UPDATE_COMPLETE_FAILED: 'UPDATE_COMPLETE_FAILED',      // Ошибка завершения
    UPDATE_COMPLETE_ERROR: 'UPDATE_COMPLETE_ERROR'         // Ошибка при завершении
}
```

## События (Events)

Плагин отправляет следующие события через window:

### rustore-update-info
Отправляется при получении информации об обновлении.
```javascript
window.addEventListener('rustore-update-info', function(e) {
    console.log('Update info:', e.detail);
    // e.detail содержит те же данные что и getAppUpdateInfo()
});
```

### rustore-update-progress
Отправляется во время загрузки обновления.
```javascript
window.addEventListener('rustore-update-progress', function(e) {
    console.log('Progress:', e.detail.progress + '%');
    console.log('Downloaded:', e.detail.bytesDownloaded);
    console.log('Total:', e.detail.totalBytesToDownload);
});
```

**Структура события:**
```typescript
{
    status: 'downloading',          // Статус
    progress: number,               // Прогресс в процентах (0-100)
    bytesDownloaded: number,        // Загружено байт
    totalBytesToDownload: number    // Всего байт для загрузки
}
```

### rustore-update-downloading
Отправляется при начале загрузки обновления.
```javascript
window.addEventListener('rustore-update-downloading', function(e) {
    console.log('Download started');
    // e.detail = { status: 'update_downloading', message: 'Update download started' }
});
```

### rustore-update-installing
Отправляется при начале установки обновления.
```javascript
window.addEventListener('rustore-update-installing', function(e) {
    console.log('Installation started');
    // e.detail = { status: 'update_installing', message: 'Update installation started automatically' }
});
```

## Вспомогательные функции

### isUpdateAvailable(updateInfo)
Проверяет доступность обновления.
```javascript
const updateInfo = await RustoreUpdate.getAppUpdateInfo();
if (RustoreUpdate.isUpdateAvailable(updateInfo)) {
    console.log('Обновление доступно');
}
```

### isUpdateReadyToInstall(updateInfo)
Проверяет готовность обновления к установке.
```javascript
const updateInfo = await RustoreUpdate.getAppUpdateInfo();
if (RustoreUpdate.isUpdateReadyToInstall(updateInfo)) {
    console.log('Обновление готово к установке');
}
```

### isUpdateCanceled(result)
Проверяет, было ли обновление отменено пользователем.
```javascript
RustoreUpdate.startUpdateFlow({ updateType: 'FLEXIBLE' })
    .catch(function(error) {
        if (RustoreUpdate.isUpdateCanceled(error)) {
            console.log('Пользователь отменил обновление');
        }
    });
```

### isUpdateSuccessful(result)
Проверяет успешность обновления.
```javascript
RustoreUpdate.startUpdateFlow({ updateType: 'FLEXIBLE' })
    .then(function(result) {
        if (RustoreUpdate.isUpdateSuccessful(result)) {
            console.log('Обновление успешно');
        }
    });
```

### isUpdateFailed(error)
Проверяет, произошла ли ошибка обновления.
```javascript
RustoreUpdate.startUpdateFlow({ updateType: 'FLEXIBLE' })
    .catch(function(error) {
        if (RustoreUpdate.isUpdateFailed(error)) {
            console.log('Обновление не удалось');
        }
    });
```

## Полный пример использования

```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    // Подписка на события
    window.addEventListener('rustore-update-info', function(e) {
        console.log('Update info event:', e.detail);
    });

    window.addEventListener('rustore-update-progress', function(e) {
        updateProgressBar(e.detail.progress);
    });

    window.addEventListener('rustore-update-downloading', function(e) {
        showNotification('Загрузка обновления началась');
    });

    window.addEventListener('rustore-update-installing', function(e) {
        showNotification('Установка обновления');
    });

    // Проверка обновлений
    checkForUpdates();
}

async function checkForUpdates() {
    try {
        const updateInfo = await RustoreUpdate.getAppUpdateInfo();

        console.log('Update availability:', updateInfo.updateAvailability);
        console.log('Install status:', updateInfo.installStatus);
        console.log('Available version code:', updateInfo.availableVersionCode);

        if (RustoreUpdate.isUpdateAvailable(updateInfo)) {
            // Показать диалог пользователю
            const shouldUpdate = await showUpdateDialog();

            if (shouldUpdate) {
                // Определить тип обновления
                const updateType = isCriticalUpdate(updateInfo) ? 'IMMEDIATE' : 'FLEXIBLE';

                // Запустить обновление
                startUpdate(updateType);
            }
        } else {
            console.log('Обновлений нет');
        }
    } catch (error) {
        console.error('Ошибка проверки обновлений:', error);
    }
}

async function startUpdate(updateType) {
    try {
        const result = await RustoreUpdate.startUpdateFlow({
            updateType: updateType
        });

        if (RustoreUpdate.isUpdateSuccessful(result)) {
            console.log('Обновление успешно запущено');

            if (updateType === 'FLEXIBLE') {
                // Для гибкого обновления следим за прогрессом
                // События будут приходить автоматически
                console.log('Обновление загружается в фоне');
            }
        }
    } catch (error) {
        if (RustoreUpdate.isUpdateCanceled(error)) {
            console.log('Пользователь отменил обновление');
        } else if (error.error === RustoreUpdate.UpdateFlowError.MANAGER_CREATE_FAILED) {
            console.error('Не удалось создать менеджер обновлений');
        } else {
            console.error('Ошибка обновления:', error);
        }
    }
}

function updateProgressBar(progress) {
    document.getElementById('progress-bar').style.width = progress + '%';
    document.getElementById('progress-text').textContent = progress + '%';
}
```

## Требования

- Android 5.0 (API level 21) и выше
- RuStore SDK
- Cordova Android 9.0.0 или выше

## Поддерживаемые платформы

- Android

## Известные ограничения
1. Немедленное обновление (IMMEDIATE) блокирует приложение до завершения
2. После установки гибкого обновления (FLEXIBLE) требуется перезапуск приложения
