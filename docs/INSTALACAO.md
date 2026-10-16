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
