# Drawmotion — estúdio Android de desenho e animação 2D

Drawmotion é uma aplicação Android nativa, escrita em Kotlin com Jetpack Compose. O objetivo desta versão é organizar o editor como um estúdio com biblioteca de projetos, ferramentas de desenho e exportação de animações.

> **Versão:** 0.2.0-preview.1. Esta é uma pré-versão para testes; ainda exige validação em dispositivos reais antes de ser considerada estável.

## Funcionalidades

- **Biblioteca de projetos locais:** criar, abrir, guardar e duplicar projetos separados.
- **Detalhes dos projetos:** nome, número de frames, quantidade de traços, FPS, material de palco e data da última alteração.
- **Autosave e migração:** projetos gravados em ficheiros individuais; os dados da versão antiga são migrados automaticamente na primeira abertura.
- **Oito pincéis:** tinta, lápis, marcador, aerógrafo, giz de cera, marca-texto, pastel e pixel.
- **Entrada por toque ou caneta digital:** modo combinado, apenas dedo ou apenas stylus, quando o dispositivo comunica corretamente o tipo de ponteiro.
- **Ferramentas de desenho:** borracha, linha, régua com marcas, retângulo e elipse.
- **Materiais de palco:** papel branco, grelha, creme, azul técnico e fundo noturno.
- **Estúdio horizontal imersivo:** a área de desenho fica no centro, com ferramentas e cores nas laterais.
- **Transformação do palco:** zoom, deslocamento e rotação por gestos de dois dedos.
- **Animação:** timeline, criar/duplicar/apagar frames, onion skin, reprodução e velocidade de 1 a 30 FPS.
- **Efeitos:** espelhamento horizontal, inversão vertical, monocromático e limpeza do frame.
- **Exportação de imagem:** PNG e JPEG em alta resolução.
- **Exportação de animação:** MP4/H.264, GIF e sequência de frames PNG num ZIP.
- **Compilação verificável:** GitHub Actions compila o APK e verifica a estrutura e assinatura do pacote.

## Descarregar a pré-versão

1. Abre a secção [Releases](https://github.com/nexauren1/Draw-motion/releases).
2. Escolhe **Drawmotion v0.2.0-preview.1** e descarrega o ficheiro `Drawmotion-v0.2.0-preview.1-android.apk`.
3. Abre o APK no Android e autoriza a instalação a partir dessa origem, caso o sistema o solicite.

O APK gerado pelo workflow é assinado com a chave de debug e serve para testes. Não é uma build de produção assinada para distribuição comercial.

Também podes abrir [GitHub Actions](https://github.com/nexauren1/Draw-motion/actions), escolher uma execução **Android APK** bem-sucedida e descarregar o artefacto `drawmotion-android-apk`. Esse download é um ZIP; extrai-o para encontrar o APK.

## Compilar localmente

Requisitos: JDK 17, Android SDK Platform 35 e Gradle 8.9.

```bash
gradle --no-daemon assembleDebug
```

Ficheiro gerado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Processo de publicação

O workflow `.github/workflows/android-build.yml` compila a aplicação em alterações relevantes. Ao publicar um tag `v*`, cria uma GitHub Release e anexa o APK; tags que incluem `preview` são marcadas como pré-lançamentos.

Antes de promover para versão estável, é necessário validar em hardware real: toque e caneta digital, criação/abertura de vários projetos, autosave e recuperação, além de reproduzir os MP4 exportados em diferentes dispositivos. A codificação MP4 depende do codificador H.264 disponível no Android.

## Identificação técnica

- **Application ID:** `com.nexauren.drawmotion`
- **Linguagem:** Kotlin
- **UI e desenho:** Jetpack Compose
- **Versão:** `0.2.0-preview.1`
- **versionCode:** `6`
- **minSdk:** 23 (Android 6.0 ou superior)
- **targetSdk:** 34
- **compileSdk:** 35

## Próximas melhorias

Camadas independentes, seleção e transformação de objetos, importação de imagens, controlo de pressão da caneta, áudio, testes automatizados de interface e assinatura de produção protegida por secrets do CI.
