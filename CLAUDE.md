# TelaDois Android — guia do projeto

> **Ao começar uma sessão (em qualquer PC):** `git pull`, leia este arquivo inteiro e confira a seção **Estado atual** antes de mexer em qualquer coisa.

## Visão geral

**Objetivo.** App Android (qualquer celular Android 10+) com duas funções:
1. **Segunda tela via cabo USB** — plugar no PC Windows faz o celular virar um monitor extra; desplugar faz o monitor sumir. Só exibe (o mouse do PC é quem mexe).
2. **Modo Descanso** estilo StandBy, sem carregador, na horizontal — relógio, data, temperatura, bateria, contadores de notificações por app e música de qualquer player com capa e controles. Após 60 s sem toque, a tela fica totalmente preta; 1 toque acende (esse toque não aciona botões).

Projeto irmão (pausado): app de iPhone em `C:\Pedro\Arquivos Pedro\Aplicativo celular` (repo `Pedro3720/teladois`). A arquitetura do PC e o protocolo vêm de lá; aqui o túnel é ADB em vez de usbmuxd.

- Especificação (fonte da verdade do design): `docs/superpowers/specs/2026-10-16-android-segunda-tela-descanso-design.md`
- Planos por etapa: `docs/superpowers/plans/`
- Guia do usuário para instalar no celular: `docs/INSTALACAO.md`
- Repositório: https://github.com/Pedro3720/teladois-android (público)

**Stack.**
- Celular: Kotlin + Jetpack Compose, Gradle (wrapper 8.14.3), AGP 8.13.0, Kotlin 2.2.20, Compose BOM 2025.09.00; `minSdk 29`, `targetSdk 36`, `compileSdk 36`; pacote `com.pedro3720.teladois`.
- PC (a partir da Etapa 4): app de bandeja em C#/.NET, `adb.exe`, Virtual Display Driver, FFmpeg (`ddagrab` + H.264 pela GPU).
- CI: GitHub Actions (`.github/workflows/android.yml`, Ubuntu): testes JVM + APK debug como artefato.

**Estrutura de pastas.**
- `android/` — app Kotlin + Compose (módulo único `app`)
- `windows/` — app de bandeja C#/.NET (a partir da Etapa 4)
- `protocol/` — protocolo PC ↔ celular e exemplos em bytes, testados pelos dois lados (a partir da Etapa 4)
- `prototype/` — protótipo HTML do Descanso + Playwright (Etapa 1)
- `docs/` — spec, planos, guia de instalação
- `.github/workflows/` — CI
- `.superpowers/` — rascunho local do fluxo de subagentes (ignorado pelo git, **não vai para o outro PC**)

**Como rodar localmente** (Git Bash; ver "Variáveis de ambiente" para o prelúdio):
```bash
cd android && ./gradlew testDebugUnitTest assembleDebug     # testes + APK (1º build demora: rode em segundo plano)
adb install -r app/build/outputs/apk/debug/app-debug.apk    # instala no celular plugado
adb shell am start -n com.pedro3720.teladois/.MainActivity  # abre o app
adb exec-out screencap -p > print.png                       # print (só no Bash)
```

## Convenções e decisões técnicas (com o motivo)

**Convenções**
- O usuário fala português: documentos e mensagens para ele em português; código e identificadores em inglês.
- Commits em português com prefixo convencional (`feat:`, `fix:`, `test:`, `ci:`, `docs:`, `chore:`), como no projeto do iPhone.
- Versões de dependências **só** em `android/gradle/libs.versions.toml` (uma fonte da verdade).
- TDD: teste falhando → código mínimo → teste passando → commit.
- A partir da Etapa 1: trabalhar em branch e só juntar ao `main` com CI verde. A Etapa 0 foi feita direto no `main` (fundação).
- Antes de entregar um APK ao usuário, inspecionar manifesto/permissões/componentes exportados. O REA (`inspect_android_package`) precisa de jadx/apktool, que **não** estão instalados aqui; o substituto que funciona é o `aapt2` do SDK:
  `"$(cygpath -u "$LOCALAPPDATA")/Android/Sdk/build-tools/36.0.0/aapt2.exe" dump badging <apk>` e `... dump xmltree --file AndroidManifest.xml <apk>`.
  Aceitáveis (vêm do AndroidX): permissão `com.pedro3720.teladois.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` e `ProfileInstallReceiver` protegido por `android.permission.DUMP`.

**Decisões (detalhes na spec)**

| Decisão | Motivo |
|---|---|
| Kotlin + Compose nativo (não MAUI/Flutter) | Vídeo (`MediaCodec`), notificações e música são recursos profundos do Android; melhor documentação e APK menor |
| Linha de comando, sem Android Studio | Leve (~1,5 GB); o Claude compila/testa/instala pelo terminal. Emulador só se fizer falta |
| Música via `MediaSession` do sistema (não SDK do Spotify) | Funciona com qualquer player, sem cadastro no Spotify Developer; usa a mesma permissão das notificações |
| Notificações: só ícone + contador, sem conteúdo | Privacidade (a tela fica exposta na mesa) |
| Só o toque acorda a tela preta | Economia de bateria |
| App aparece sobre a tela de bloqueio | Comportamento de StandBy; contadores não mostram conteúdo |
| Túnel ADB: `adb track-devices` + `am start` + `adb forward tcp:7100`; app escuta só em `127.0.0.1:7100` | O PC abre o app sozinho ao plugar; loopback não fica exposto na rede |
| Segunda tela só exibe (sem toque) na v1 | Escopo; toque fica como ideia futura |
| Extras da v1: bloco nas Configurações rápidas + modo noturno | Escolha do usuário; clima completo/agenda/fotos/Wi-Fi ficaram para depois |
| Repositório público | CI do GitHub grátis e ilimitado |
| Um único `adb` (o do SDK) | Dois `adb` de versões diferentes derrubam o servidor adb um do outro |

**Armadilhas desta máquina (Windows)**
- O `java` do PATH é o **Java 8 da Oracle** — não serve. Use sempre o JDK 21 via `JAVA_HOME`.
- O TEMP do usuário é um caminho curto 8.3 (`CONTAB~1`), que quebra sockets AF_UNIX do JDK 21 ("Unable to establish loopback connection"). Solução: `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Temp\jt` (a pasta `C:\Temp\jt` precisa existir).
- `adb exec-out screencap -p > arquivo` corrompe o PNG no PowerShell 5.1 — use Bash.
- `sdkmanager --licenses`: no PowerShell o pipe de "y" não funciona; use Bash: `yes | sdkmanager.bat --licenses`.
- `python3` no PATH pode ser o atalho da Microsoft Store; use `py -3`.
- Variáveis de usuário recém-criadas só aparecem em shells novos (reinicie o app do Claude ou use o prelúdio abaixo).

## Variáveis de ambiente (só nomes)

| Nome | Para quê |
|---|---|
| `JAVA_HOME` | JDK 21 (Temurin) usado pelo Gradle e pelo sdkmanager |
| `ANDROID_HOME` | Raiz do Android SDK (`%LOCALAPPDATA%\Android\Sdk`) |
| `PATH` (acréscimo) | `%ANDROID_HOME%\platform-tools` (para o `adb`) |
| `JAVA_TOOL_OPTIONS` | Contorno do TEMP 8.3 (ver armadilhas) — só no PC que tiver esse problema |

Não há segredos no projeto por enquanto. A chave de assinatura de release (Etapa 6) e qualquer `keystore.properties`/`.env` ficam **fora do git** (já estão no `.gitignore`).

Prelúdio para shells que ainda não enxergam as variáveis (Git Bash):
```bash
export JAVA_HOME="$(ls -d "/c/Program Files/Eclipse Adoptium/jdk-21"* | sort -r | head -1)"; export ANDROID_HOME="$(cygpath -w "$(cygpath -u "$LOCALAPPDATA")/Android/Sdk")"; export PATH="$JAVA_HOME/bin:$(cygpath -u "$LOCALAPPDATA")/Android/Sdk/platform-tools:$PATH"; export JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\Temp\jt'
```

## Estado atual

*Atualizado em 2026-10-16.*

**Pronto — Etapa 0 (fundação), Tasks 1–6 do plano `docs/superpowers/plans/2026-10-16-etapa-0-fundacao.md`:**
- Ferramentas no PC do trabalho: JDK 21.0.12, Android SDK (platform-tools 37.0.1, `platforms;android-36`, `build-tools;36.0.0`), variáveis de usuário.
- Repositório com `.gitignore`/`.gitattributes`, projeto Gradle, `Greeting` com teste JVM, tela "Olá, TelaDois!" em paisagem.
- CI `android` verde no GitHub (run 38950216737), artefatos `test-results` e `TelaDois-apk-debug`.
- `docs/INSTALACAO.md` escrito; APK debug inspecionado (aapt2) sem permissões/componentes inesperados.

**Em andamento — Task 7 (instalar no celular):** aguardando o usuário ligar a Depuração USB e plugar o celular. Faltam: `adb devices -l` mostrar `device`, anotar modelo/Android/resolução (`getprop ro.product.model`, `getprop ro.build.version.release`, `wm size`), instalar, abrir, print, confirmar com o usuário, acrescentar aqui a linha "Celular de teste do usuário: …" e dar push.

**Pendências pequenas da Etapa 0** (achadas nas revisões; resolver numa leva só antes de fechar a etapa):
- `.github/workflows/android.yml`: `actions/upload-artifact@v4` roda em Node 20 (descontinuado) → subir para `@v7`; incluir `android/app/build/reports/tests/` no artefato de testes; `cancel-in-progress` só fora do `main`; `retention-days` curto; avaliar fixar `ubuntu-24.04` (o `ubuntu-latest` muda para Ubuntu 26 em 19/10/2026).
- `android/app/build.gradle.kts`: fixar `buildToolsVersion = "36.0.0"` (o AGP baixou a 35.0.0 sozinho).
- `.gitattributes`: `eol=lf` para `.gitignore`, `.gitattributes`, `*.properties`, `*.json`.
- `.gitignore`: acrescentar `node_modules/`, `playwright-report/`, `test-results/` (Etapa 1) e `bin/`, `obj/` (Etapa 4) quando chegar a hora.
- Depois: revisão final da Etapa 0 (whole-branch review, `superpowers:requesting-code-review`).

**Próximos passos:** concluir a Task 7 → leva de pendências + revisão final → escrever o plano da **Etapa 1 — Design** (ícone no Canva, tokens com `ui-ux-pro-max:design-system`, protótipo HTML do Descanso no tamanho real do celular verificado com Playwright). Roteiro das etapas 1–6 no fim do plano da Etapa 0.

## Ambiente do Claude Code

Levantado em 2026-10-16 a partir de `~/.claude/settings.json`, `~/.claude/plugins/installed_plugins.json`, `~/.claude/plugins/known_marketplaces.json`, `~/.claude/skills`, `~/.claude.json` (só nomes dos MCPs), `claude_desktop_config.json` do app Claude e da pasta de plugins da conta claude.ai. O projeto não tem `.mcp.json` nem `.claude/`.

### Plugins

**A. Plugins da conta claude.ai** — chegam sozinhos no app Claude (Desktop) de qualquer PC logado na mesma conta; **nenhuma ação manual**:

| Plugin | Versão | Uso aqui |
|---|---|---|
| `superpowers` | 6.1.0 | brainstorming, writing-plans, subagent-driven-development, TDD, debugging, verificação |
| `ui-ux-pro-max` | 2.6.2 | design system, tokens, ícones (Etapa 1) |
| `productivity` | 1.3.1 | não usado neste projeto |

Se for usar o Claude Code **no terminal** (fora do app) e eles não aparecerem, instale pelo marketplace:
```
/plugin marketplace add obra/superpowers
/plugin install superpowers@superpowers-dev
/plugin marketplace add nextlevelbuilder/ui-ux-pro-max-skill
/plugin install ui-ux-pro-max@ui-ux-pro-max-skill
```

**B. Plugins instalados pelo marketplace (escopo usuário)** — precisam ser instalados no outro PC:
```
/plugin marketplace add anthropics/claude-plugins-official
/plugin install frontend-design@claude-plugins-official
/plugin install supabase@claude-plugins-official
/plugin marketplace add heygen-com/hyperframes
/plugin install hyperframes@hyperframes
/plugin marketplace add addyosmani/agent-skills
/plugin install agent-skills@addy-agent-skills
```

| Plugin | Marketplace | Uso aqui |
|---|---|---|
| `agent-skills` | `addy-agent-skills` (github.com/addyosmani/agent-skills) | spec, plan, incremental-implementation, ci-cd, security, code-reviewer etc. |
| `frontend-design` | `claude-plugins-official` (github.com/anthropics/claude-plugins-official) | protótipo HTML (Etapa 1) |
| `hyperframes` | `hyperframes` (github.com/heygen-com/hyperframes) | vídeo demo opcional (Etapa 6) |
| `supabase` | `claude-plugins-official` | não usado neste projeto (pode pular) |

`~/.claude/settings.json` também tem `"enableWorkflows": true`, `"autoUpdatesChannel": "latest"` e `"theme": "dark"` — opcional replicar.

### Skills

- **De plugins:** todas as `superpowers:*`, `agent-skills:*`, `ui-ux-pro-max:*`, `frontend-design:*`, `hyperframes:*` (vêm com os plugins acima).
- **Pessoais (`~/.claude/skills`):**
  - `reverse-engineer-anything` — skill do REA. **Manter pessoal**: depende do REA instalado na máquina e acompanha a versão dele (vem junto ao instalar o `rea-agents`; se não vier, copie a pasta `~/.claude/skills/reverse-engineer-anything`).
  - `graphify` — o `~/.claude/CLAUDE.md` global manda usar `~/.claude/skills/graphify/SKILL.md`, mas essa skill **não está instalada** neste PC (só a ferramenta `graphify` 0.9.28 via `uv`). **Sugestão:** quando houver código suficiente, rodar `graphify install --project` neste repositório — a skill vai para `.claude/skills/graphify/` e passa a ir junto pelo git para os dois PCs.
  - `playwright-cli` está em `~/.claude/.claude/skills/` (local errado, só vale com a pasta `~/.claude` aberta como projeto) — não é usada aqui; o Playwright MCP cobre o protótipo.
- **Do projeto (`.claude/skills`):** nenhuma ainda.

### MCP servers

| Nome | Para quê | Onde está configurado | Como configurar no outro PC |
|---|---|---|---|
| `rea` | Engenharia reversa/inspeção de APKs, binários (ex.: Virtual Display Driver), adb | `~/.claude.json` (usuário), stdio | `npm install -g rea-agents` (v6.5.0 aqui) e depois `claude mcp add --scope user rea -- node "$(npm root -g)/rea-agents/scripts/rea.mjs" mcp`. Para inspecionar APKs ele ainda precisa de jadx/apktool (não instalados aqui) |
| `apify` | Raspagem/busca na web (pouco usado aqui) | `~/.claude.json` (usuário), HTTP `https://mcp.apify.com/` | `claude mcp add --scope user --transport http apify https://mcp.apify.com/` — autentica por OAuth no primeiro uso (nenhum token no arquivo) |
| `playwright` | Verificar o protótipo HTML (Etapa 1) | `claude_desktop_config.json` do app Claude | No app: Configurações → Desenvolvedor → editar config e adicionar `"playwright": {"command": "npx", "args": ["@playwright/mcp@latest"]}`; ou no CLI: `claude mcp add --scope user playwright -- npx @playwright/mcp@latest` |
| Conectores da conta claude.ai (Canva, Gmail, Google Calendar, Google Drive, Supabase etc.) | Canva para o ícone (Etapa 1) | conta claude.ai | Automático com o mesmo login; se pedir, reautorize em claude.ai → Configurações → Conectores |
| Claude in Chrome | Navegar no Chrome real | extensão do Chrome | Instalar a extensão "Claude" no Chrome e logar |

### Ferramentas de linha de comando (fora do Claude)

| Ferramenta | Como instalar | Etapa |
|---|---|---|
| Git, GitHub CLI (`gh auth login`), Node.js, Python 3 (`py`) | winget | já |
| JDK 21 Temurin | `winget install --id EclipseAdoptium.Temurin.21.JDK -e` + `JAVA_HOME` | 0 |
| Android SDK command-line tools + `platform-tools`, `platforms;android-36`, `build-tools;36.0.0` | zip oficial em developer.android.com → `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`; depois `sdkmanager` (ver plano da Etapa 0, Task 1) | 0 |
| `uv` + `graphifyy` | `winget install astral-sh.uv` e `uv tool install graphifyy` | quando usar graphify |
| .NET SDK | winget | 4 |
| FFmpeg | `winget install Gyan.FFmpeg` | 5 |

### Memória do Claude

A memória automática (`~/.claude/projects/<pasta>/memory/`) é **local de cada PC**. Tudo o que importa para continuar o trabalho deve estar **neste CLAUDE.md** (principalmente "Estado atual"), na spec e nos planos.

## Handoff entre computadores

**Antes de sair:**
- [ ] Atualizar a seção **Estado atual** (pronto / em andamento com o próximo passo exato / pendências). O ledger em `.superpowers/sdd/progress.md` é local e ignorado pelo git — copie para cá o que importar.
- [ ] `git status` limpo: commit de tudo (nunca segredos).
- [ ] `git push` (e confirmar que o CI ficou verde, se mexeu em código).

**Ao chegar no outro PC:**
- [ ] `git pull`.
- [ ] Ler este CLAUDE.md antes de começar (o Claude Code carrega sozinho; peça "leia o CLAUDE.md e me diga o estado atual").
- [ ] Se for o primeiro uso nesse PC: seguir **Ambiente do Claude Code** e **Ferramentas de linha de comando** acima, e conferir o build (`./gradlew testDebugUnitTest`).
