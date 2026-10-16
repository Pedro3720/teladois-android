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
