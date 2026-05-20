# phantom_agent

Projeto Android (Kotlin + Jetpack Compose + UiAutomator) com um servidor TCP instrumentado para automação de UI no dispositivo.

## Requisitos

- JDK 17
- Android SDK configurado (`ANDROID_HOME`/`ANDROID_SDK_ROOT`)
- Dispositivo ou emulador Android (API 24+)
- `adb` disponível no PATH

## Estrutura principal

- App Android em `:app` (Compose + Navigation 3)
- Testes instrumentados em `app/src/androidTest`
- Servidor de automação: `app/src/androidTest/java/com/example/phantom_agent/PhantomServer.kt`

## Build, lint e testes

Da raiz do repositório:

```bash
./gradlew :app:assembleDebug
./gradlew :app:lint
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Executar um único teste:

```bash
# Classe de teste unitário
./gradlew :app:testDebugUnitTest --tests "com.example.phantom_agent.ui.main.MainScreenViewModelTest"

# Método de teste unitário
./gradlew :app:testDebugUnitTest --tests "com.example.phantom_agent.ui.main.MainScreenViewModelTest.uiState_initiallyLoading"

# Classe de teste instrumentado
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.phantom_agent.ui.main.MainScreenTest

# Método de teste instrumentado
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.phantom_agent.ui.main.MainScreenTest#firstItem_exists
```

## Como usar o PhantomServer (TCP + JSON)

O `PhantomServer` é um teste instrumentado que abre `ServerSocket`s em portas dinâmicas (porta `0`) dentro do dispositivo e processa comandos JSON linha a linha.
As portas reais são publicadas no Logcat com:
- `COMMAND_PORT_ALLOCATED: <porta>`
- `VIDEO_PORT_ALLOCATED: <porta>`

### 1. Preparar dispositivo

```bash
adb devices
# obter as portas no logcat e aplicar reverse para a porta de comando
adb logcat -s PhantomServer:I
adb reverse tcp:<command_port> tcp:<command_port>
```

### 2. Iniciar o servidor no dispositivo

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.phantom_agent.PhantomServer
```

> O teste entra em loop contínuo (`while (true)`), então ele funciona como processo de fundo enquanto a sessão de teste estiver ativa.

### 3. Enviar comandos via TCP puro

Cada requisição deve ser um JSON válido terminado por `\n`.

Exemplo com `nc`:

```bash
echo '{"action":"dumpWindow"}' | nc 127.0.0.1 <command_port>
echo '{"action":"clickByText","text":"OK"}' | nc 127.0.0.1 <command_port>
```

## Protocolo JSON suportado

### `dumpWindow`

Request:

```json
{"action":"dumpWindow"}
```

Success:

```json
{"status":"success","xml":"<hierarchy>...</hierarchy>"}
```

### `clickByText`

Request:

```json
{"action":"clickByText","text":"Texto do elemento"}
```

Success:

```json
{"status":"success"}
```

Erro (elemento não encontrado):

```json
{"status":"error","message":"Element not found"}
```

## Tratamento de erros

- Qualquer exceção durante o processamento do cliente retorna:

```json
{"status":"error","message":"<detalhe_do_erro>"}
```

- O socket do cliente é sempre fechado ao final de cada requisição.
