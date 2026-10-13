# Drawmotion — aplicativo Android

**Editor nativo de desenho e animação 2D**, desenvolvido em Kotlin com Jetpack Compose. Este é um aplicativo Android; não precisa de uma página web para executar o editor.

> **Estado:** protótipo Android 0.1.1. O APK é uma versão de teste, não uma release estável.

## Funcionalidades implementadas

- Desenho por toque com pincel, borracha, linha, retângulo e elipse.
- Paleta rápida de cores e controlo do tamanho do pincel.
- Página inicial em modo vertical, com navegação para Projetos, Definições e Sobre.
- Editor em modo horizontal e imersivo, com a área de desenho central e ferramentas nas laterais.
- Palco 16:9 com zoom, deslocamento e rotação por gestos de dois dedos, além de controlos manuais.
- Timeline compacta com criação, duplicação e remoção de frames.
- Definições persistentes para FPS inicial, onion skin e ecrã imersivo.
- Onion skin para visualizar frames adjacentes.
- Pré-visualização e velocidade ajustável entre 1 e 30 FPS.
- Desfazer/refazer; mudanças de gestos para dois dedos não devem deixar marcas acidentais.
- Efeitos básicos: espelhar horizontalmente, inverter verticalmente, monocromático e limpar frame.
- Autosave local automático e botão para guardar imediatamente.
- Exportação do frame atual para PNG de 1920 × 1080.
- Workflow GitHub Actions para compilar e verificar a estrutura e assinatura do APK.

## Como descarregar e instalar o APK

1. Abre **Actions** neste repositório e escolhe a execução **Android APK** que terminou com sucesso.
2. Em **Artifacts**, descarrega `drawmotion-android-apk`. O download deste artefacto é um ZIP.
3. **Extrai o ZIP primeiro.** Dentro dele encontrarás `app-debug.apk`. Instala esse ficheiro APK — não tentes instalar o ZIP.
4. O Android poderá pedir que autorizes a instalação de aplicações a partir da aplicação utilizada para abrir o ficheiro.

O app agora suporta Android 6.0 (API 23) ou mais recente. Se continuar a aparecer “Ocorreu um erro ao analisar o pacote”, confirma que extraíste o APK e que o ficheiro termina em `.apk`, não `.zip`. A compilação verifica também a assinatura e a estrutura do APK.

## Compilar localmente

Requisitos: JDK 17, Android SDK Platform 35 e Gradle 8.9.

    gradle assembleDebug

O APK é criado em:

    app/build/outputs/apk/debug/app-debug.apk

## Próximas etapas

A base ainda precisa de testes em dispositivos reais e de mais funcionalidades: camadas independentes, seleção e transformação de objetos, importação de imagens, suporte avançado à pressão da caneta, exportação de animação para GIF/MP4, áudio e recuperação de projetos. Essas funções não devem ser consideradas concluídas até serem implementadas e verificadas.

## Identificação técnica

- Pacote Android: `com.nexauren.drawmotion`
- Linguagem: Kotlin
- UI e desenho: Jetpack Compose
- Versão: `0.1.1`
- `minSdk`: 23
- `targetSdk`: 34
