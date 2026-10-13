# Drawmotion — aplicativo Android

**Editor nativo de desenho e animação 2D**, desenvolvido em Kotlin com Jetpack Compose. O projeto principal é Android; não depende de uma página web para abrir o editor.

> **Estado:** protótipo Android 0.1.0. A primeira base está a ser compilada e validada. Ainda não é uma substituição completa do FlipaClip.

## Funcionalidades nesta primeira versão

- Desenho por toque com pincel, borracha, linha e retângulo.
- Paleta rápida de cores e controlo do tamanho do pincel.
- Palco 16:9 com zoom, deslocamento e rotação por gestos de dois dedos, além de controlos manuais.
- Timeline com criação, duplicação e remoção de frames.
- Onion skin para visualizar o frame anterior e o seguinte.
- Pré-visualização da animação e velocidade ajustável entre 1 e 30 FPS.
- Desfazer/refazer traços.
- Efeitos básicos: espelhar horizontalmente, inverter verticalmente, monocromático e limpar frame.
- Gravação local do projeto no dispositivo Android.
- Exportação do frame atual para PNG de 1920 × 1080.
- Workflow GitHub Actions para compilar e disponibilizar um APK de teste.

## Compilar localmente

Requisitos: Android Studio ou JDK 17 + Android SDK 35 + Gradle 8.9.

Na raiz do repositório, executa:

    gradle assembleDebug

O APK de teste é criado em:

    app/build/outputs/apk/debug/app-debug.apk

Também podes abrir o projeto no Android Studio e executar a configuração app.

## Obter o APK pelo GitHub

1. Abre **Actions** neste repositório.
2. Seleciona **Android APK** e abre a execução mais recente.
3. Quando a compilação terminar com sucesso, transfere o artefacto **drawmotion-debug-apk**.

O APK de debug é destinado a teste e pode ser instalado manualmente num dispositivo Android. **Não é um pacote assinado para publicação na Google Play.** Uma distribuição pública exige testes adicionais e uma configuração de assinatura.

## Próximas etapas

Esta base ainda precisa de testes em dispositivos reais e de mais funcionalidades: camadas independentes, seleção e transformação de objetos, suporte melhorado a canetas com pressão, importação de imagens, exportação de animação para GIF/MP4, áudio e recuperação de projetos. Essas funções não devem ser consideradas concluídas até serem implementadas e verificadas.

## Identificação técnica

- Pacote Android: com.nexauren.drawmotion
- Linguagem: Kotlin
- UI e desenho: Jetpack Compose
- Versão inicial: 0.1.0
