# TelaDois Android — Segunda Tela + Modo Descanso — Especificação de Design

- **Data:** 2026-10-16
- **Status:** design aprovado em conversa; aguardando revisão do documento
- **Nome do app:** TelaDois Android (pacote `com.pedro3720.teladois`)
- **Repositório:** `Pedro3720/teladois-android` (público), separado do projeto do iPhone
- **Referência:** a spec do app de iPhone (`Aplicativo celular/docs/superpowers/specs/2026-10-16-iphone-segunda-tela-descanso-design.md`) — este projeto reaproveita a arquitetura do PC e o protocolo; o transporte troca usbmuxd por ADB

## 1. Objetivo

Um app Android com duas funções:

1. **Segunda Tela via cabo USB** — ao plugar o celular no PC Windows, ele vira um monitor extra do Windows (só exibe; o mouse do PC é usado para mexer nele). Ao desplugar, o monitor extra desaparece.
2. **Modo Descanso** — imitação do StandBy do iOS, sem precisar estar no carregador, na horizontal: relógio grande, data, temperatura, bateria, contadores de notificações por app e a música que está tocando (qualquer player) com capa e controles. Após 60 s sem toque a tela fica totalmente preta; 1 toque acende de novo (esse toque só acorda, não aciona botões).

Extras da v1: bloco "Descanso" nas Configurações rápidas (e atalho na tela inicial) e modo noturno (relógio vermelho-escuro em horário configurável).

### Fora do escopo da v1 (ideias futuras)

- Toque na Segunda Tela virando toque/mouse do Windows.
- Segunda Tela por Wi-Fi (ADB sem fio).
- Clima completo (ícone, máxima/mínima), próximo compromisso da agenda, página de fotos.
- Prévia do conteúdo das notificações; notificação acender a tela escurecida.
- Alarme, controle de volume, publicação na Play Store.

## 2. Restrições do ambiente

| Restrição | Consequência |
|---|---|
| Usuário só tem Windows 11 | Tudo compila e instala no próprio Windows: JDK 21 + Android SDK command-line tools + platform-tools; `gradlew assembleDebug` e `adb install -r`. Sem Mac, sem Sideloadly, o app não expira |
| Sem Android Studio (por escolha) | Linha de comando apenas; emulador só se fizer falta. Telas verificadas por Roborazzi (JVM) e no celular real |
| Qualquer Android 10+ | `minSdk 29`, `targetSdk 36`, `compileSdk 36`. Resolução lida do aparelho em tempo de execução. Tela pode ser OLED ou LCD |
| Fabricantes com economia de bateria agressiva | Onboarding pede para ignorar a otimização de bateria |
| Sem cadastro em serviços | Clima via **Open-Meteo** (sem chave); música via `MediaSession` do sistema (sem SDK do Spotify) |
| Repositório público | CI do GitHub (Linux e Windows) grátis e sem limite |

## 3. Arquitetura

```
┌──────────────── PC (Windows) ────────────────┐          ┌──────────── Celular Android ────────────┐
│ App de bandeja (C#/.NET)                     │          │ App TelaDois (Kotlin/Compose)           │
│ DeviceWatcher ─ "adb track-devices"          │          │ LinkServer ── escuta 127.0.0.1:7100     │
│ AdbControl ── am start + adb forward 7100    │  cabo    │ StreamPlayer ─ H.264 via MediaCodec     │
│ DisplayManager ─ monitor virtual (VDD)       │  USB     │ AppState ── Descanso ⇄ escuro ⇄ 2ª tela │
│ CaptureEncoder ─ FFmpeg ddagrab + H.264 GPU  │  (ADB)   │ RestScreen ─ Relógio | Música           │
│ StreamSender ── envia o vídeo pelo túnel     │ ───────► │ DimController ─ 60 s → preto            │
│ Ícone de bandeja ─ status, fps, latência     │          │ NotificationHub / MediaWatcher          │
└──────────────────────────────────────────────┘          │ Weather / Battery / NightMode / RestTile│
                                                          └─────────────────────────────────────────┘
```

### 3.1 Transporte USB (ADB)

- O PC usa o `adb.exe` das **platform-tools** (encontrado no PATH ou configurado na bandeja).
- `DeviceWatcher` acompanha `adb track-devices` e distingue `device` (pronto), `unauthorized` (aguardando o usuário aceitar a depuração no celular) e ausente.
- Quando o celular fica `device`: `AdbControl` executa `adb shell am start -n com.pedro3720.teladois/.MainActivity`, depois `adb forward tcp:7100 tcp:7100`, e o `StreamSender` conecta em `127.0.0.1:7100` (com novas tentativas até o app estar escutando).
- O app escuta só em `127.0.0.1:7100` (loopback — inacessível pela rede), apenas enquanto está em primeiro plano.
- O transporte fica atrás de uma interface no PC (`IDeviceTransport`) para que, no futuro, o túnel usbmuxd do iPhone possa ser plugado sem alterar o resto. Na v1 só existe a implementação ADB.
- Pré-requisito no celular: **Depuração USB** ligada nas Opções do desenvolvedor e o PC autorizado uma vez; cabo de dados.

### 3.2 Componentes do celular (Kotlin + Jetpack Compose)

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `Protocol` | Codifica/decodifica as mensagens `[tipo][tamanho][dados]` | nada (Kotlin puro) |
| `LinkServer` | Escuta `127.0.0.1:7100`, enquadra as mensagens, responde `PING`, detecta timeout | `Protocol` |
| `StreamPlayer` | Decodifica H.264 com `MediaCodec` (modo baixa latência quando disponível, Android 11+) e desenha em `SurfaceView` em tela cheia | `MediaCodec` |
| `AppState` | Máquina de estados: `Rest`, `Dimmed`, `SecondScreen` | `Clock` injetável |
| `DimController` | Timer de inatividade de 60 s; aplica tela preta + brilho mínimo da janela; restaura no toque | `Clock` |
| `NotificationHub` | `NotificationListenerService`: contadores de notificações por app | permissão "Acesso a notificações" |
| `MediaWatcher` | `MediaSessionManager.getActiveSessions` + `MediaController`: capa, título, artista, progresso, ⏮ ⏯ ⏭ de qualquer player | mesma permissão (componente do listener) |
| `WeatherService` | Temperatura atual via Open-Meteo a cada 30 min | `LocationManager` (localização aproximada, sem Google Play Services) |
| `BatteryService` | Nível e estado de carga | broadcast `ACTION_BATTERY_CHANGED` |
| `NightMode` | Decide se o horário atual está na faixa noturna (padrão 22:00–07:00) | `Clock` |
| `RestTile` | Bloco "Descanso" nas Configurações rápidas; atalho na tela inicial | `TileService`, `ShortcutManager` |
| `Onboarding` | Explica e pede: acesso a notificações, localização, ignorar otimização de bateria | — |

Uma única `MainActivity` hospeda o Descanso e a Segunda Tela.

### 3.3 Componentes do PC (C#/.NET, app de bandeja)

| Módulo | Responsabilidade |
|---|---|
| `DeviceWatcher` | Lê `adb track-devices`; emite eventos conectado/autorizado/desconectado |
| `AdbControl` | `am start`, `adb forward`/remoção do forward, verificação de que o app está instalado |
| `DisplayManager` | Controla o **Virtual Display Driver** (open-source, assinado): cria/remove o monitor na resolução do `CONFIG`; ao iniciar, remove monitores que sobraram |
| `CaptureEncoder` | FFmpeg com `ddagrab` apontado para o monitor virtual + H.264 por hardware (`h264_nvenc` / `h264_qsv` / `h264_amf`; reserva `libx264 -preset ultrafast -tune zerolatency`), saída Annex-B no stdout |
| `StreamSender` | Conecta no túnel, envia as mensagens, mede a latência via `PING` |
| Ícone de bandeja | Status (sem celular / aceite no celular / conectado), resolução, fps, latência, "iniciar com o Windows", caminhos de `adb.exe` e `ffmpeg.exe` |

### 3.4 Protocolo PC ↔ celular

Cada mensagem: `[tipo: 1 byte][tamanho: 4 bytes, big-endian][dados]`.

| Tipo | Direção | Conteúdo |
|---|---|---|
| `HELLO` | celular → PC | versão do protocolo; largura × altura da tela em paisagem (pixels reais, incluindo a área do recorte da câmera); maior largura × altura que o decodificador H.264 aceita |
| `CONFIG` | PC → celular | largura, altura, fps escolhidos |
| `VIDEO` | PC → celular | unidades NAL H.264 (Annex-B) |
| `KEYFRAME_REQ` | celular → PC | pede um quadro completo (após erro de decodificação) |
| `PING` / `PONG` | ambos | timestamp; mede latência e detecta conexão travada (timeout: 3 s) |
| `BYE` | ambos | encerramento limpo |

Regra de resolução: a resolução da tela do celular; se ela passar do limite do decodificador, reduz mantendo a proporção. 60 fps; o PC reduz para 30 fps se a latência passar de 100 ms. Os valores numéricos dos tipos e o layout exato dos campos ficam definidos em `protocol/` (com exemplos em bytes usados pelos testes dos dois lados).

## 4. Comportamento do app no celular

### 4.1 Estados

1. Abrir o app (ícone, bloco rápido, atalho ou PC) → **Descanso**: tela cheia imersiva (barras do sistema escondidas), orientação paisagem nos dois sentidos, tela mantida ligada enquanto o app está em primeiro plano.
2. **Descanso ativo**: conteúdo visível por **60 s** desde o último toque.
3. Após 60 s sem toque → **Escurecido**: tela 100% preta e brilho da janela no mínimo. O brilho do sistema não é alterado.
4. **1 toque** em qualquer lugar → volta ao Descanso ativo na hora; o timer reinicia. Esse toque é consumido — não aciona nenhum botão.
5. PC conectado (`HELLO`/`CONFIG` concluídos), em qualquer estado → **Segunda Tela**; escurecimento desligado; brilho da janela volta ao normal.
6. Conexão encerrada (`BYE`, erro de socket ou timeout do `PING`) → **Descanso ativo**.
7. App sai do primeiro plano → a conexão e o `LinkServer` são encerrados (o PC remove o monitor).

### 4.2 Tela de bloqueio

O app aparece **por cima da tela de bloqueio** (`showWhenLocked`; `turnScreenOn` quando aberto pelo PC). Com o celular bloqueado, o Descanso e a Segunda Tela funcionam sem desbloquear; sair do app exige desbloqueio normalmente. Os contadores mostram só ícones (nenhum conteúdo de mensagem).

### 4.3 Telas do Descanso (deslizar horizontalmente)

- **Página 1 — Relógio**
  - Esquerda: horas e minutos gigantes.
  - Direita: dia da semana + data, temperatura, anel de bateria (verde, com raio quando carregando).
  - Faixa de contadores: até 5 ícones de apps com o número de notificações ativas, do mais recente para o mais antigo; "+N" se houver mais apps.
  - Rodapé: "música — artista" pequeno, só quando algo está tocando.
  - Engrenagem discreta (só com a tela acesa) → Configurações.
- **Página 2 — Música**
  - Esquerda: capa. Direita: título, artista, barra de progresso com tempo decorrido/restante, ⏮ ⏯ ⏭.
  - Fundo: gradiente escuro a partir das cores da capa.
  - Nenhum player ativo: "Nada tocando". Sem permissão: botão "Liberar acesso a notificações".

### 4.4 Regras dos contadores

- Conta as notificações ativas de cada app (resumos de grupo não contam em dobro).
- Ignora: notificações fixas/contínuas (serviços em primeiro plano, downloads), notificações de mídia, o próprio TelaDois e os pacotes do sistema (`android`, `com.android.systemui`).
- O número diminui/some quando a notificação é removida no celular.

### 4.5 Modo noturno

Dentro do horário configurado (padrão 22:00–07:00): textos e anel em vermelho-escuro, brilho da janela reduzido. O escurecimento de 60 s continua valendo. Configurável em Configurações (ligar/desligar, início, fim).

### 4.6 Configurações

Horário do modo noturno; status de cada permissão com botão para liberar; aviso de otimização de bateria.

## 5. Tratamento de erros

| Situação | Comportamento |
|---|---|
| Cabo removido durante o uso | `DeviceWatcher` vê o celular sumir → monitor removido em < 1 s; o Windows devolve as janelas ao monitor principal; o celular volta ao Descanso |
| Celular `unauthorized` | Bandeja mostra "Aceite a depuração USB no celular"; continua automaticamente após aceitar |
| Depuração USB desligada / celular não aparece | Bandeja mostra o passo a passo para ligar |
| App não instalado no celular | Bandeja avisa |
| Usuário sai do app | O socket fecha → o PC remove o monitor |
| App do PC trava ou fecha | Na próxima abertura, `DisplayManager` remove monitores virtuais remanescentes e o forward antigo |
| Sem codificador de hardware | Reserva `libx264 ultrafast zerolatency` |
| Erro de decodificação no celular | Descarta até o próximo keyframe e envia `KEYFRAME_REQ` |
| Resolução acima do limite do decodificador | PC reduz a resolução mantendo a proporção (pelo campo do `HELLO`) |
| Sem permissão de notificações | Contadores e página Música mostram o botão "Liberar"; o resto funciona |
| Nenhum player ativo | "Nada tocando" |
| Localização negada ou Open-Meteo fora do ar | Temperatura escondida; o resto funciona |
| Sistema fecha o app escurecido | Mitigado pelo pedido de ignorar otimização de bateria |

## 6. Estrutura do repositório

```
/
├── android/        projeto Gradle (Kotlin + Compose): app, testes JVM, Roborazzi, testes instrumentados
├── windows/        solução C#/.NET (app de bandeja + testes xUnit)
├── protocol/       definição do protocolo e exemplos em bytes usados pelos testes dos dois lados
├── prototype/      protótipo HTML do Descanso + testes Playwright
├── docs/           especificações, planos, ADRs, guia de instalação
└── .github/workflows/
    ├── android.yml   (ubuntu) testes JVM + prints Roborazzi + APK debug como artefatos
    └── windows.yml   (windows) dotnet build + dotnet test
```

## 7. Build e instalação

- **Ferramentas (etapa 0):** JDK 21 (Temurin), Android SDK command-line tools, platform-tools, `platforms;android-36`, `build-tools` correspondente. Gradle via wrapper.
- **Ciclo:** `gradlew testDebugUnitTest` → `gradlew assembleDebug` → `adb install -r app-debug.apk`.
- **Assinatura:** debug automática no desenvolvimento; chave de release gerada na etapa 6, guardada fora do git, com backup feito pelo usuário (sem ela, atualizar exige desinstalar).
- **Git:** a partir da etapa 1, trabalho em branch; junta ao `main` só com CI verde.

## 8. Testes

| Alvo | Como |
|---|---|
| Lógica do celular | Testes JVM: `Protocol`, `AppState`, `DimController` com relógio simulado, `NightMode`, filtro do `NotificationHub`, mapeamento do `MediaWatcher` |
| Telas do celular | Roborazzi (JVM, sem aparelho): Relógio, Música, Escurecido, Noturno, estados sem permissão; PNGs como artefato do CI, revisados |
| Comportamento real | Testes instrumentados no celular pelo cabo: escurece (timer encurtado por parâmetro), 1 toque acende e não aciona botão |
| Protocolo nos dois lados | Mesmos exemplos de `protocol/` verificados em Kotlin e em C# |
| PC | xUnit: protocolo, leitura da saída do `adb`, estados do `DisplayManager` |
| Ponta a ponta | Manual com o celular real; latência do `PING` exibida na bandeja. Meta: < 100 ms a 60 fps |
| Segurança | REA inspeciona cada APK antes da entrega (manifesto, permissões, componentes exportados) e o binário do Virtual Display Driver antes da instalação |

## 9. Etapas de entrega

Cada etapa termina em algo funcionando e verificado.

| # | Etapa | Entrega verificável |
|---|---|---|
| 0 | Fundação | Ferramentas instaladas, repositório no GitHub, CI verde, app "Olá" instalado no celular via `adb install` |
| 1 | Design | Ícone (Canva), tokens de cor/tipografia, protótipo HTML do Descanso aprovado |
| 2 | Descanso v1 | Relógio, data, bateria, clima, escurecer em 60 s e acender com 1 toque, sobre a tela de bloqueio — Roborazzi + teste no celular |
| 3 | Notificações e música | Onboarding da permissão, contadores por app, página Música com capa, progresso e controles |
| 4 | Túnel ADB | Bandeja detecta o celular, abre o app e troca `HELLO`/`PING` pelo cabo |
| 5 | Segunda Tela | Monitor virtual + transmissão + remoção automática ao desplugar |
| 6 | Extras e acabamento | Bloco rápido e atalho, modo noturno, Configurações, bandeja completa, iniciar com o Windows, chave de release, revisão de segurança |

As etapas 2–3 (celular) e 4–5 (PC) são quase independentes e podem andar em paralelo em sessões separadas.

## 10. Ferramentas do Claude por etapa

- **Todas as etapas:** `superpowers:writing-plans`, `superpowers:subagent-driven-development`, `superpowers:test-driven-development`, `superpowers:systematic-debugging`, `superpowers:verification-before-completion`, `superpowers:requesting-code-review`, `agent-skills:incremental-implementation`, `agent-skills:git-workflow-and-versioning`, `agent-skills:documentation-and-adrs`.
- **Etapa 0:** `init` (CLAUDE.md do projeto), `agent-skills:constraints`, `agent-skills:ci-cd-and-automation`.
- **Etapa 1:** Canva (ícone), `ui-ux-pro-max`, `ui-ux-pro-max:design-system`, `frontend-design`, Artifacts (mockups), Playwright (protótipo em viewport de celular em paisagem).
- **Etapas 2–3:** `agent-skills:source-driven-development`, `agent-skills:frontend-ui-engineering`, `run`, REA (`list_adb_devices`, `capture_adb_screen`, `read_adb_logcat`, `inspect_android_package`).
- **Etapas 4–5:** `agent-skills:api-and-interface-design`, `agent-skills:doubt-driven-development`, `agent-skills:performance-optimization`, `agent-skills:observability-and-instrumentation`, `agent-skills:security-and-hardening`, agente `security-auditor`, REA (Virtual Display Driver).
- **Etapa 6:** `code-review`, `security-review`, `agent-skills:ship`, `simplify`, `graphify` (quando houver código suficiente), `hyperframes` (vídeo demo, opcional).

## 11. Pré-requisitos do usuário

- Etapa 0: autorizar a instalação de JDK 21, Android SDK e platform-tools; ligar a Depuração USB no celular e aceitar o PC; usar um cabo de dados (não só de carga). Conta no GitHub (já existe).
- Etapa 3: liberar "Acesso a notificações" e localização no celular.
- Etapa 5: aprovar a instalação do Virtual Display Driver (requer administrador).
- Etapa 6: fazer backup da chave de assinatura de release.
