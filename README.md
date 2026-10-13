# Drawmotion Studio

**Um estúdio web de desenho e animação quadro a quadro**, com foco em uso por toque, telemóvel, tablet e computador.

> Estado atual: **pré-lançamento 0.1.0**. Esta é uma primeira versão funcional e uma base para testes. Ainda não substitui um editor de animação profissional e deve ser validada em vários dispositivos antes de ser anunciada como estável.

## Experimentar

A app é estática e não precisa de servidor próprio. Quando o workflow de GitHub Pages terminar com sucesso, estará disponível em:

**https://nexauren1.github.io/Draw-motion/**

Se a página ainda não abrir, consulta o separador **Actions** no repositório e confirma se o workflow **Deploy Drawmotion Studio to GitHub Pages** terminou. A primeira publicação pode exigir alguns segundos/minutos e a ativação de Pages nas definições do repositório.

Para testar localmente, abre o ficheiro **index.html** num navegador moderno. A instalação PWA e o modo offline requerem um contexto HTTPS ou localhost.

## Funcionalidades implementadas nesta versão

- Lápis, borracha, linha, retângulo, elipse, texto e balde de tinta.
- Paleta de cores, conta-gotas, tamanho e opacidade do pincel.
- Palco 960×540 com zoom, deslocamento, rotação manual e gestos de pinça/rotação com dois dedos.
- Timeline com seleção, criação, duplicação e remoção de frames, FPS e duração de exposição.
- Onion skin com frames anteriores e seguintes, em tons auxiliares.
- Camadas por frame, visibilidade, opacidade, renomeação e importação de imagens como camadas.
- Efeitos rápidos para a camada ativa; podem ser revertidos com Desfazer.
- Desfazer/refazer para operações de desenho.
- Exportação do frame em PNG, sprite sheet PNG, projeto editável **.drawmotion** e vídeo WebM quando o navegador suporta **MediaRecorder**.
- Salvamento local com IndexedDB e fallback para armazenamento do navegador.
- PWA instalável com shell offline e layout responsivo.

## Limitações conhecidas da versão 0.1.0

- A exportação de vídeo depende do suporte do navegador a MediaRecorder; o formato aqui gerado é WebM. GIF, MP4, sequência PNG em ZIP e áudio sincronizado ainda não estão implementados.
- O projeto é guardado localmente no dispositivo/navegador. Não existe conta de utilizador nem sincronização na nuvem.
- O editor ainda precisa de ensaios de desempenho com animações longas, de teste de pressão de caneta e de validação em Android/iOS.
- A área de desenho tem tamanho de projeto fixo (960×540) nesta versão.
- A exportação de vídeo, efeitos, preenchimento e muitos frames devem ser verificados em equipamentos reais antes de uma versão estável.

## Desenvolvimento e verificações

Não usa dependências externas em runtime. O código da aplicação está concentrado em index.html para facilitar a primeira distribuição. O GitHub Actions faz uma verificação de sintaxe do JavaScript e publica a aplicação via GitHub Pages.

1. Abre o separador **Actions** e verifica os workflows.
2. Testa desenho, desfazer/refazer, frames, camadas, onion skin, salvar/reabrir e as quatro exportações.
3. Regista erros como Issues com passos para reproduzir e dispositivo/navegador.
4. Só depois dos testes, publica uma versão estável.

## Plano de evolução

- **0.1.x — Robustez:** corrigir erros reportados, testes de gestos e recuperação de projetos.
- **0.2 — Animação:** áudio na timeline, importação de vídeo/rotoscopia e melhor controlo de exposição.
- **0.3 — Exportação:** sequência PNG em ZIP, GIF e MP4 através de pipeline suportado.
- **0.4 — Desenho:** pressão de caneta, estabilização de traço, ferramentas de seleção/transformação e simetria.
- **1.0 — Estável:** testes automatizados, desempenho e acessibilidade, documentação e processo de release validado.

## Referências de produto

O conjunto de funcionalidades foi planeado a partir de padrões comuns de apps de animação: timeline por frames, onion skin, camadas, controlo de FPS e exportação. Referências consultadas: [FlipaClip — nova timeline e layouts](https://support.flipaclip.com/article/113-newversion), [FlipaClip — onion skin](https://support.flipaclip.com/article/17-onion-skinning), [Krita — funcionalidades de animação](https://krita.org/en/features/) e [RoughAnimator — funcionalidades](https://roughanimator.com/).

Drawmotion é um projeto independente; não é afiliado nem patrocinado por estas aplicações.
