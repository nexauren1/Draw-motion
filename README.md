# Drawmotion — estúdio Android de desenho e animação 2D

Drawmotion é um editor nativo para Android, desenvolvido em Kotlin e Jetpack Compose. O projeto está a evoluir de protótipo para um estúdio com projetos organizados, ferramentas de desenho e exportação de animações.

> **Versão atual:** 0.2.0-preview.1. É uma pré-versão de testes: ainda precisa de validação em dispositivos Android reais antes de ser considerada estável.

## Funcionalidades desta pré-versão

- **Projetos locais:** criar, abrir, guardar e duplicar projetos com nome próprio.
- **Detalhes do projeto:** mostra número de frames, traços, FPS e data da última alteração.
- **Autosave:** cada projeto é guardado em ficheiro separado no armazenamento privado da aplicação. Os dados antigos do protótipo são migrados na primeira abertura.
- **Pincéis e formas:** caneta fina, pincel, lápis, marcador, realçador, aerógrafo, caligrafia, borracha, linha, retângulo, elipse e régua com marcas de medição.
- **Entrada controlada:** escolher entre dedo e caneta digital, só dedo ou só caneta.
- **Palco:** grelha opcional, zoom, deslocamento e rotação por gestos de dois dedos.
- **Animação:** timeline, criar/duplicar/apagar frames, onion skin, pré-visualização e velocidade de 1 a 30 FPS.
- **Edição:** desfazer/refazer e efeitos de espelhamento, inversão, monocromático e limpeza de frame.
- **Exportação PNG:** frame atual a 1920 × 1080.
- **Exportação MP4:** sequência de frames codificada em H.264 a 960 × 540, usando o FPS selecionado e o codificador de vídeo disponível no dispositivo.
- **Compilação automática:** GitHub Actions compila o APK, verifica a estrutura e a assinatura, e prepara um artefacto descarregável.

## Descarregar a versão de testes

1. Abre a secção **Releases** deste repositório.
2. Escolhe a versão **0.2.0-preview.1** e descarrega `Drawmotion-v0.2.0-preview.1-android.apk`.
3. Autoriza a instalação a partir da aplicação que abriu o ficheiro, caso o Android peça essa permissão.

O APK desta pré-versão é assinado com a chave de debug do Android e destina-se a avaliação. Não é uma compilação de produção assinada para distribuição comercial.

Também podes abrir **Actions**, escolher uma execução bem-sucedida de **Android APK** e descarregar o artefacto `drawmotion-android-apk`. Esse artefacto é um ZIP; extrai-o para encontrar o APK.

## Compilar localmente

Requisitos: JDK 17, Android SDK Platform 35 e Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

O APK será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Publicação de versões

O workflow `.github/workflows/android-build.yml` compila a aplicação em cada alteração relevante e publica um APK numa Release quando é enviado um tag `v*`. As pré-versões cujo tag contém `preview` ficam assinaladas como pré-lançamentos.

Antes de promover uma versão a estável, é necessário testar toque e caneta em hardware real, a gravação e recuperação de vários projetos, e a reprodução dos MP4 exportados em diferentes dispositivos.

## Identificação técnica

- **Pacote Android:** `com.nexauren.drawmotion`
- **Linguagem:** Kotlin
- **Interface/desenho:** Jetpack Compose
- **Versão:** `0.2.0-preview.1`
- **versionCode:** `3`
- **minSdk:** 23 (Android 6.0 ou superior)
- **targetSdk:** 34
- **compileSdk:** 35

## Próximas melhorias

Camadas independentes, seleção e transformação de objetos, importação de imagens, suporte avançado à pressão da caneta, áudio, exportação GIF, testes automatizados de interface e uma assinatura de produção protegida por secrets de CI.
