# Etapa 0 — Fundação — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ferramentas Android instaladas no Windows, repositório `Pedro3720/teladois-android` no GitHub com CI verde, e um app "Olá" em paisagem compilado localmente e instalado no celular do usuário via `adb install`.

**Architecture:** Projeto Gradle em `android/` com um único módulo `app` (Kotlin + Jetpack Compose). Build e testes rodam localmente com o Gradle Wrapper e o JDK 21; o GitHub Actions (Linux) repete os testes e gera o APK como artefato. A instalação é feita pelo cabo com o `adb` do Android SDK.

**Tech Stack:** JDK 21 (Temurin), Android SDK (command-line tools, platform-tools, `platforms;android-36`, `build-tools;36.0.0`), Gradle 8.14.3 (wrapper), Android Gradle Plugin 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00, activity-compose 1.11.0, JUnit 4.13.2, GitHub Actions.

Spec: `docs/superpowers/specs/2026-10-16-android-segunda-tela-descanso-design.md`

## Global Constraints

- Pacote / applicationId: `com.pedro3720.teladois`. Nome exibido: `TelaDois`.
- `minSdk 29` (Android 10), `targetSdk 36`, `compileSdk 36`.
- Kotlin + Jetpack Compose; Android Studio não é instalado (só linha de comando).
- Documentos para o usuário em português; código e nomes em inglês; mensagens de commit em português com prefixo convencional (`feat:`, `test:`, `ci:`, `docs:`, `chore:`), como no projeto do iPhone.
- `$SCRATCH` (Bash) / `$env:SCRATCH` (PowerShell) = pasta de scratchpad da sessão; defina a variável no início de cada comando que a usa.
- Repositório público `Pedro3720/teladois-android`; branch padrão `main`.
- O `java` do PATH é o Java 8 da Oracle — **nunca** use. Todo comando de build usa `JAVA_HOME` apontando para o JDK 21.
- Um único `adb`: o do SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`). Não instalar outro (winget/choco) — versões diferentes derrubam o servidor adb.
- Downloads ficam cada um numa pasta nova e vazia do scratchpad; checksums SHA-256 conferidos antes de extrair.
- Ações que exigem aprovação explícita do usuário no chat antes de executar: cada download/instalação, aceitar licenças (winget e Android SDK), criar o repositório público no GitHub e fazer push.
- Comandos com provável duração > 2 min (primeiro build do Gradle, sdkmanager) rodam em segundo plano.

---

## Mapa de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `.gitignore` | Ignorar saídas de build, `local.properties`, chaves de assinatura, `/builds/` |
| `.gitattributes` | Finais de linha (`gradlew` LF, `*.bat` CRLF) e binários |
| `README.md` | Apresentação curta do projeto |
| `CLAUDE.md` | Guia do projeto para o Claude (regras, comandos, armadilhas) |
| `android/settings.gradle.kts` | Repositórios e inclusão do módulo `app` |
| `android/build.gradle.kts` | Declaração dos plugins (sem aplicar) |
| `android/gradle.properties` | Opções do Gradle/AndroidX |
| `android/gradle/libs.versions.toml` | Catálogo de versões (única fonte de versões) |
| `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/*` | Gradle Wrapper 8.14.3 com SHA-256 fixado |
| `android/app/build.gradle.kts` | Configuração do app (SDKs, Compose, dependências) |
| `android/app/src/main/AndroidManifest.xml` | Activity principal em paisagem |
| `android/app/src/main/java/com/pedro3720/teladois/Greeting.kt` | Texto da saudação (lógica testável) |
| `android/app/src/main/java/com/pedro3720/teladois/MainActivity.kt` | Tela Compose "Olá" centralizada |
| `android/app/src/test/java/com/pedro3720/teladois/GreetingTest.kt` | Teste JVM da saudação |
| `.github/workflows/android.yml` | CI: testes + APK debug como artefato |
| `docs/INSTALACAO.md` | Guia do usuário: Depuração USB, instalar e abrir o app |

---

### Task 1: Ferramentas Android no PC

**Files:** nenhum no repositório (configuração da máquina).

**Interfaces:**
- Produces: variáveis de usuário `JAVA_HOME` (JDK 21) e `ANDROID_HOME` (`%LOCALAPPDATA%\Android\Sdk`); `%ANDROID_HOME%\platform-tools` no PATH do usuário; `sdkmanager` em `%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat`. Todas as tasks seguintes dependem disso.

- [ ] **Step 1: Pedir aprovação ao usuário**

Listar no chat o que será instalado e de onde, e esperar um "sim":
- JDK 21 Temurin via `winget` (pacote `EclipseAdoptium.Temurin.21.JDK`, ~190 MB) — aceita os termos do pacote.
- Android SDK command-line tools (zip do site oficial developer.android.com, ~150 MB).
- Pelo `sdkmanager`: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0` (~400 MB) — exige aceitar as licenças do Android SDK.
- Variáveis de usuário `JAVA_HOME`, `ANDROID_HOME` e `platform-tools` no PATH do usuário.

- [ ] **Step 2: Instalar o JDK 21 (PowerShell)**

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-package-agreements --accept-source-agreements
```

Expected: `Successfully installed`.

- [ ] **Step 3: Definir JAVA_HOME e ANDROID_HOME (PowerShell)**

```powershell
$jdk = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" | Sort-Object Name -Descending | Select-Object -First 1).FullName
$jdk
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdk, "User")
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force $sdk | Out-Null
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "User")
$p = [Environment]::GetEnvironmentVariable("Path", "User")
if ($p -notlike "*Android\Sdk\platform-tools*") { [Environment]::SetEnvironmentVariable("Path", "$p;$sdk\platform-tools", "User") }
```

Expected: imprime algo como `C:\Program Files\Eclipse Adoptium\jdk-21.0.x.y-hotspot`.

Observação: shells já abertos (inclusive os do Claude) não enxergam variáveis novas até o app ser reiniciado. Até lá, todo comando deve definir as variáveis no início — em Bash:

```bash
export JAVA_HOME="$(ls -d "/c/Program Files/Eclipse Adoptium/jdk-21"* | sort -r | head -1)"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

- [ ] **Step 4: Baixar as command-line tools**

Abrir `https://developer.android.com/studio#command-line-tools-only` (navegador embutido, `get_page_text`), copiar o link do zip **Windows** (`commandlinetools-win-<número>_latest.zip`) e o SHA-256 publicado ao lado. Baixar para uma pasta nova:

```bash
mkdir -p "$SCRATCH/dl-cmdline" && cd "$SCRATCH/dl-cmdline"
curl -fLO "<link copiado da página>"
sha256sum commandlinetools-win-*_latest.zip
```

Expected: o hash impresso é igual ao da página. Se diferente, apagar e parar.

- [ ] **Step 5: Extrair no layout esperado (`cmdline-tools/latest`)**

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$zip = Get-ChildItem "$env:SCRATCH\dl-cmdline\commandlinetools-win-*_latest.zip" | Select-Object -First 1
Expand-Archive $zip.FullName -DestinationPath "$env:SCRATCH\dl-cmdline\x" -Force
New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
Move-Item "$env:SCRATCH\dl-cmdline\x\cmdline-tools" "$sdk\cmdline-tools\latest"
Test-Path "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
```

(`$env:SCRATCH` = caminho do scratchpad da sessão.) Expected: `True`.

- [ ] **Step 6: Aceitar licenças (só depois do "sim" do Step 1) e instalar os pacotes (em segundo plano)**

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
$sm = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat"
(1..20 | ForEach-Object { "y" }) | & $sm --licenses
& $sm "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

Expected: `All SDK package licenses accepted` e instalação sem erro.

- [ ] **Step 7: Verificar**

```bash
"$JAVA_HOME/bin/java" -version
"$ANDROID_HOME/platform-tools/adb" version
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager.bat" --list_installed
```

Expected: `openjdk version "21...`; `Android Debug Bridge version 1.0.41`; lista contendo `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`.

Sem commit (nada no repositório).

---

### Task 2: Fundação do repositório

**Files:**
- Create: `.gitignore`, `.gitattributes`, `README.md`, `CLAUDE.md`

**Interfaces:**
- Consumes: repositório git local já existente (branch `main`, commit da spec).
- Produces: regras de ignore/EOL usadas por todas as tasks; `CLAUDE.md` com os comandos oficiais de build/instalação.

- [ ] **Step 1: Criar `.gitignore`**

```gitignore
# Gradle / Android
.gradle/
build/
/android/local.properties
/android/.kotlin/
captures/
.externalNativeBuild/
.cxx/
*.iml
.idea/

# Assinatura — nunca versionar
*.jks
*.keystore
keystore.properties

# Artefatos entregues ao usuário
/builds/

# Sistema
Thumbs.db
.DS_Store
```

- [ ] **Step 2: Criar `.gitattributes`**

```gitattributes
* text=auto
*.bat text eol=crlf
*.cmd text eol=crlf
gradlew text eol=lf
*.sh text eol=lf
*.kts text eol=lf
*.kt text eol=lf
*.xml text eol=lf
*.toml text eol=lf
*.yml text eol=lf
*.md text eol=lf
*.jar binary
*.png binary
*.apk binary
```

- [ ] **Step 3: Criar `README.md`**

```markdown
# TelaDois Android

Transforma o celular Android em **segunda tela do PC (via cabo USB)** e em um **Modo Descanso** estilo StandBy — relógio, clima, bateria, contadores de notificações e música — sem precisar do carregador.

- Especificação: [docs/superpowers/specs/2026-10-16-android-segunda-tela-descanso-design.md](docs/superpowers/specs/2026-10-16-android-segunda-tela-descanso-design.md)
- Como instalar no celular: [docs/INSTALACAO.md](docs/INSTALACAO.md)

O app é compilado no próprio Windows (Gradle + JDK 21) e instalado pelo cabo com `adb install`.
```

- [ ] **Step 4: Criar `CLAUDE.md`**

```markdown
# TelaDois Android — guia do projeto

App Android que (1) vira segunda tela do PC Windows via cabo USB (túnel ADB) e (2) tem um Modo Descanso estilo StandBy sem carregador.
Especificação: `docs/superpowers/specs/2026-10-16-android-segunda-tela-descanso-design.md`. Planos: `docs/superpowers/plans/`.

## Regras
- O usuário fala português; documentos para ele em português, código em inglês.
- O `java` do PATH é o Java 8 da Oracle — não serve. Use sempre o JDK 21 via `JAVA_HOME`. Se o shell não tiver as variáveis, defina no início do comando (Bash):
  `export JAVA_HOME="$(ls -d "/c/Program Files/Eclipse Adoptium/jdk-21"* | sort -r | head -1)"; export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"; export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"`
- Build e testes: `cd android && ./gradlew testDebugUnitTest assembleDebug` (o primeiro build demora — rode em segundo plano).
- Instalar no celular: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`; abrir: `adb shell am start -n com.pedro3720.teladois/.MainActivity`.
- Print do celular: use **Bash** (`adb exec-out screencap -p > print.png`). No PowerShell 5.1 o `>` corrompe o PNG.
- Um único `adb`: o do SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`). Não instale outro (winget/choco).
- Antes de entregar um APK ao usuário, inspecione com o REA (`inspect_android_package`): permissões, componentes exportados, `allowBackup`.
- Versões de dependências só em `android/gradle/libs.versions.toml`.
- A partir da Etapa 1, trabalhar em branch e só juntar ao `main` com CI verde.
- Quando houver código suficiente, use o `graphify` para navegar no repositório com menos tokens.

## Estrutura
- `android/` — app Kotlin + Jetpack Compose (Gradle)
- `windows/` — app de bandeja C#/.NET (a partir da Etapa 4)
- `protocol/` — protocolo PC ↔ celular e exemplos em bytes (a partir da Etapa 4)
- `prototype/` — protótipo HTML do Descanso + Playwright (Etapa 1)
- `.github/workflows/` — CI
```

- [ ] **Step 5: Commit**

```bash
git add .gitignore .gitattributes README.md CLAUDE.md
git commit -m "chore: fundação do repositório (ignore, atributos, guias)"
```

---

### Task 3: Projeto Gradle e `Greeting` com TDD

**Files:**
- Create: `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/gradle-wrapper.jar`, `android/gradle/wrapper/gradle-wrapper.properties` (gerados)
- Create: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/com/pedro3720/teladois/Greeting.kt`
- Test: `android/app/src/test/java/com/pedro3720/teladois/GreetingTest.kt`

**Interfaces:**
- Consumes: `JAVA_HOME`, `ANDROID_HOME` (Task 1).
- Produces: `object Greeting { fun text(): String }` retornando `"Olá, TelaDois!"`; comandos `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` funcionando.

- [ ] **Step 1: Gerar o Gradle Wrapper (com aprovação do download)**

O wrapper precisa ser gerado em `android/` **vazio**, antes dos arquivos de build (senão o Gradle tenta configurar o projeto).

```bash
mkdir -p "$SCRATCH/dl-gradle" && cd "$SCRATCH/dl-gradle"
curl -fLO https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
curl -fL https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256 -o expected.sha256
echo "$(cat expected.sha256)  gradle-8.14.3-bin.zip" | sha256sum -c -
unzip -q gradle-8.14.3-bin.zip -d x
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android" && mkdir -p android && cd android
"$SCRATCH/dl-gradle/x/gradle-8.14.3/bin/gradle" wrapper --gradle-version 8.14.3 --distribution-type bin --gradle-distribution-sha256-sum "$(cat "$SCRATCH/dl-gradle/expected.sha256")"
cat gradle/wrapper/gradle-wrapper.properties
```

Expected: `gradle-8.14.3-bin.zip: OK`; `gradle-wrapper.properties` contém `distributionUrl=...gradle-8.14.3-bin.zip` e `distributionSha256Sum=<hash>`. Apagar qualquer `settings.gradle(.kts)` vazio que o comando tenha criado antes do Step 2.

- [ ] **Step 2: Criar `android/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TelaDois"
include(":app")
```

- [ ] **Step 3: Criar `android/gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.13.0"
kotlin = "2.2.20"
composeBom = "2025.09.00"
activityCompose = "1.11.0"
junit = "4.13.2"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

Se alguma versão não resolver no build (Step 9), consultar `https://maven.google.com/web/index.html` (AndroidX/AGP) ou o Maven Central (Kotlin) e usar a versão estável mais recente compatível, registrando a troca no commit.

- [ ] **Step 4: Criar `android/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- [ ] **Step 5: Criar `android/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 6: Criar `android/app/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pedro3720.teladois"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pedro3720.teladois"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
```

- [ ] **Step 7: Criar o manifesto mínimo `android/app/src/main/AndroidManifest.xml`** (a Activity entra na Task 4)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="false"
        android:label="TelaDois"
        android:theme="@android:style/Theme.Material.NoActionBar" />

</manifest>
```

- [ ] **Step 8: Escrever o teste que falha — `android/app/src/test/java/com/pedro3720/teladois/GreetingTest.kt`**

```kotlin
package com.pedro3720.teladois

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun textGreetsInPortuguese() {
        assertEquals("Olá, TelaDois!", Greeting.text())
    }
}
```

- [ ] **Step 9: Rodar e ver falhar (primeira execução: segundo plano, baixa o Gradle e as dependências)**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android/android" && ./gradlew testDebugUnitTest
```

Expected: FAIL na compilação dos testes com `Unresolved reference 'Greeting'`. (Se falhar antes disso por versão de plugin/dependência, corrigir pelo procedimento do Step 3.)

- [ ] **Step 10: Implementar — `android/app/src/main/java/com/pedro3720/teladois/Greeting.kt`**

```kotlin
package com.pedro3720.teladois

object Greeting {
    fun text(): String = "Olá, TelaDois!"
}
```

- [ ] **Step 11: Rodar e ver passar**

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`; o relatório `app/build/test-results/testDebugUnitTest/TEST-com.pedro3720.teladois.GreetingTest.xml` mostra `tests="1" failures="0"`.

- [ ] **Step 12: Commit**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android"
git add android
git commit -m "test: projeto Gradle do app e Greeting com teste JVM"
git status --short
```

Expected: `git status` vazio (nada de `build/`, `.gradle/` ou `local.properties` pendente).

---

### Task 4: Tela "Olá" em paisagem e APK debug

**Files:**
- Create: `android/app/src/main/java/com/pedro3720/teladois/MainActivity.kt`
- Modify: `android/app/src/main/AndroidManifest.xml` (substituir inteiro)

**Interfaces:**
- Consumes: `Greeting.text()` (Task 3).
- Produces: `MainActivity` exportada como LAUNCHER (`com.pedro3720.teladois/.MainActivity`), usada pelo `am start` nas Tasks 7 e nas etapas seguintes; APK em `android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 1: Criar `MainActivity.kt`**

```kotlin
package com.pedro3720.teladois

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = Greeting.text(), color = Color.White, fontSize = 48.sp)
            }
        }
    }
}
```

- [ ] **Step 2: Substituir `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="false"
        android:label="TelaDois"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 3: Compilar, testar e checar lint**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android/android" && ./gradlew testDebugUnitTest assembleDebug lintDebug
ls -la app/build/outputs/apk/debug/app-debug.apk
```

Expected: `BUILD SUCCESSFUL`; o APK existe (alguns MB). Avisos de lint são aceitáveis; erros não.

- [ ] **Step 4: Commit**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android"
git add android/app/src/main
git commit -m "feat: tela inicial Olá em paisagem"
```

---

### Task 5: CI no GitHub Actions

**Files:**
- Create: `.github/workflows/android.yml`

**Interfaces:**
- Consumes: `./gradlew testDebugUnitTest assembleDebug` (Tasks 3–4).
- Produces: workflow `android` com artefatos `TelaDois-apk-debug` e `test-results`; usado como critério de "CI verde" em todas as etapas.

- [ ] **Step 1: Criar `.github/workflows/android.yml`**

```yaml
name: android

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: android-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    defaults:
      run:
        working-directory: android
    steps:
      - uses: actions/checkout@v5

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"

      - uses: gradle/actions/setup-gradle@v5

      - name: Testes e APK debug
        run: ./gradlew testDebugUnitTest assembleDebug --no-daemon

      - name: Resultados dos testes
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: android/app/build/test-results/
          if-no-files-found: warn

      - name: APK debug
        uses: actions/upload-artifact@v4
        with:
          name: TelaDois-apk-debug
          path: android/app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
```

(Se alguma `@v5` não existir no momento da execução, usar a maior versão principal publicada da action.)

- [ ] **Step 2: Garantir que `gradlew` é executável no git (Linux)**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android"
git update-index --chmod=+x android/gradlew
git ls-files -s android/gradlew
```

Expected: modo `100755`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/android.yml android/gradlew
git commit -m "ci: testes e APK debug no GitHub Actions"
```

---

### Task 6: Publicar no GitHub e CI verde

**Files:** nenhum novo.

**Interfaces:**
- Consumes: commits das Tasks 2–5.
- Produces: `https://github.com/Pedro3720/teladois-android` com `main` publicada e execução `android` verde.

- [ ] **Step 1: Pedir aprovação ao usuário** para criar o repositório **público** `Pedro3720/teladois-android` e enviar o histórico. Esperar "sim".

- [ ] **Step 2: Criar e enviar**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android"
gh auth status
gh repo create Pedro3720/teladois-android --public --source . --remote origin --push --description "Celular Android como segunda tela do PC (USB) + Modo Descanso estilo StandBy"
```

Expected: URL do repositório impressa; `git branch -vv` mostra `main` rastreando `origin/main`.

- [ ] **Step 3: Acompanhar o CI**

```bash
gh run list --workflow android.yml --limit 1
gh run watch "$(gh run list --workflow android.yml --limit 1 --json databaseId -q '.[0].databaseId')" --exit-status
```

Expected: conclusão `success`. Se falhar: `gh run view --log-failed`, corrigir com `superpowers:systematic-debugging`, commit e push, repetir.

---

### Task 7: Instalação no celular e guia do usuário

**Files:**
- Create: `docs/INSTALACAO.md`

**Interfaces:**
- Consumes: APK debug (Task 4), `adb` (Task 1), `MainActivity` (Task 4).
- Produces: app instalado no celular; guia reutilizado nas etapas seguintes.

- [ ] **Step 1: Criar `docs/INSTALACAO.md`**

```markdown
# Como instalar o TelaDois no celular Android

## 1. Ligar a Depuração USB (uma vez só)

1. **Configurações → Sobre o telefone** → toque **7 vezes** em **Número da versão** (em alguns aparelhos: *Informações do software → Número da versão*; na Xiaomi: *Versão do MIUI/HyperOS*). Aparece "Você agora é um desenvolvedor".
2. **Configurações → Sistema → Opções do desenvolvedor** (na Samsung fica no fim do menu principal; na Xiaomi em *Configurações adicionais*).
3. Ligue **Depuração USB**.
   - **Xiaomi/Redmi/POCO:** ligue também **Instalar via USB** (pode pedir login na conta Mi).

## 2. Conectar no PC

1. Use um **cabo de dados** (alguns cabos só carregam — se o celular não aparecer, troque o cabo).
2. Plugue no PC. No celular aparece **"Permitir depuração USB?"** → marque **"Sempre permitir deste computador"** → **Permitir**.
3. Se o celular perguntar o modo USB, pode deixar em "Somente carregar" — a depuração funciona assim mesmo.

## 3. Instalar e abrir o app

O Claude faz isso por você:

    adb install -r android/app/build/outputs/apk/debug/app-debug.apk
    adb shell am start -n com.pedro3720.teladois/.MainActivity

O app aparece na gaveta de apps como **TelaDois** e pode ser aberto normalmente depois. Diferente do iPhone, **ele não expira**.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `adb devices` não lista o celular | Troque o cabo/porta USB; confira se a Depuração USB está ligada |
| Aparece `unauthorized` | Desbloqueie o celular e aceite o aviso "Permitir depuração USB?" |
| `INSTALL_FAILED_USER_RESTRICTED` (Xiaomi) | Ligue "Instalar via USB" nas Opções do desenvolvedor |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | O app foi instalado com outra chave; desinstale (`adb uninstall com.pedro3720.teladois`) e instale de novo |
```

- [ ] **Step 2: Usuário liga a Depuração USB e pluga o celular** (pedir no chat, apontando para o guia). Verificar:

```bash
adb devices -l
```

Expected: uma linha com `device` (não `unauthorized`) e o modelo do aparelho. Anotar modelo e versão:

```bash
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell wm size
```

- [ ] **Step 3: Inspecionar o APK com o REA antes de instalar**

Usar `mcp__rea__inspect_android_package` em `android/app/build/outputs/apk/debug/app-debug.apk`.
Expected: pacote `com.pedro3720.teladois`, `minSdk 29`, `targetSdk 36`, `allowBackup=false`; Activity exportada só `MainActivity` (LAUNCHER). Esperados e aceitáveis por virem das bibliotecas AndroidX: a permissão própria `com.pedro3720.teladois.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (nível signature, do androidx.core) e o receiver `androidx.profileinstaller.ProfileInstallReceiver` exportado mas protegido por `android.permission.DUMP`. Qualquer outra permissão ou componente exportado é um problema a investigar.

- [ ] **Step 4: Instalar, abrir e tirar print (Bash)**

```bash
cd "/c/Pedro/Arquivos Pedro/Aplicativo Android"
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pedro3720.teladois/.MainActivity
mkdir -p "$SCRATCH/prints" && adb exec-out screencap -p > "$SCRATCH/prints/etapa0-ola.png"
```

Expected: `Success` na instalação; o print (abrir com Read) mostra fundo preto com "Olá, TelaDois!" centralizado, em paisagem. Enviar o print ao usuário (SendUserFile) e pedir confirmação de que viu o app no celular.

- [ ] **Step 5: Registrar o aparelho no CLAUDE.md e commitar**

Acrescentar ao fim da seção "Regras" do `CLAUDE.md` (com os valores reais do Step 2):

```markdown
- Celular de teste do usuário: <modelo> (Android <versão>, tela <largura>x<altura>).
```

```bash
git add docs/INSTALACAO.md CLAUDE.md
git commit -m "docs: guia de instalação via adb e aparelho de teste"
git push
```

Expected: push aceito; a execução `android` desse commit fica verde (`gh run watch ... --exit-status`).

---

## Critério de conclusão da Etapa 0

- [ ] `java -version` (via `JAVA_HOME`) = 21; `adb version` funciona; SDK 36 instalado.
- [ ] `./gradlew testDebugUnitTest assembleDebug` verde localmente.
- [ ] Repositório público no GitHub com a última execução `android` verde.
- [ ] App "Olá" instalado e aberto no celular do usuário, print confirmado.
- [ ] APK inspecionado pelo REA sem permissões nem componentes exportados inesperados.

---

## Roteiro das próximas etapas (cada uma ganha um plano próprio ao começar)

| Etapa | Plano a escrever | Pontos que o plano precisa decidir/conter |
|---|---|---|
| 1 — Design | `docs/superpowers/plans/AAAA-MM-DD-etapa-1-design.md` | Ícone no Canva; tokens (cores, tipografia, escala) com `ui-ux-pro-max:design-system`; protótipo HTML das páginas Relógio/Música/Escurecido/Noturno no tamanho real do aparelho de teste, verificado com Playwright; mockups como Artifact para aprovação |
| 2 — Descanso v1 | `...-etapa-2-descanso.md` | `Clock` injetável, `AppState`, `DimController` (TDD), `BatteryService`, `WeatherService` (Open-Meteo + `LocationManager`), tela imersiva, `showWhenLocked`, toque que só acorda; Roborazzi para prints; teste instrumentado no celular |
| 3 — Notificações e música | `...-etapa-3-notificacoes-musica.md` | `NotificationListenerService` + filtro dos contadores (TDD com dados simulados), `MediaWatcher`, página Música com gradiente da capa, onboarding da permissão |
| 4 — Túnel ADB | `...-etapa-4-tunel-adb.md` | `protocol/` com tipos numéricos e exemplos em bytes; `Protocol` em Kotlin e C#; `LinkServer`; solução `windows/` com `DeviceWatcher`/`AdbControl`/`StreamSender`; workflow `windows.yml` |
| 5 — Segunda Tela | `...-etapa-5-segunda-tela.md` | Inspeção do Virtual Display Driver com REA e instalação; `DisplayManager`; `CaptureEncoder` (FFmpeg); `StreamPlayer` (`MediaCodec`); medição de latência |
| 6 — Extras e acabamento | `...-etapa-6-acabamento.md` | `RestTile` + atalho, `NightMode`, Configurações, bandeja completa, iniciar com o Windows, chave de release + backup, `security-review`, `agent-skills:ship` |

As etapas 2–3 e 4–5 podem rodar em paralelo em sessões separadas (arquivos disjuntos: `android/` vs `windows/` + `protocol/`), desde que a Etapa 4 defina o `protocol/` antes de o `LinkServer` do celular ser escrito.
