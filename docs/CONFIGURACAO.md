# Configuração e instalação (preview não validada no jogo)

Alvo único: Minecraft **26.3**, Fabric Loader **0.20.0**, Fabric API
**0.163.0+26.3**, Java **25**. Pins intencionalmente exatos no metadata;
não prometemos funcionamento com outras releases. Java tem requisito mínimo
25, mas os únicos ensaios locais usaram runtime 25.0.2+10.

Após um build completo bem-sucedido (`./gradlew clean check build`), usar
`build/libs/otimization-minecraft-mod-0.1.0-dev.jar` — não o `-sources.jar`.
Instalar junto da API em `mods/` do servidor dedicado ou da instalação cliente
para diagnóstico do servidor integrado de singleplayer. **Clientes remotos
não precisam deste mod** para consultar o servidor: só comando/chat vanilla.
Instalado apenas no cliente, não mede nem otimiza servidor remoto.
Sem renderer, tela ou entrypoint cliente; sem dependências de mods de conteúdo.
Não há requisito de GPU no diagnóstico do servidor.

## Opções

Criar manualmente `config/omperf.properties` a partir de
`config/omperf.properties.example`. Arquivo ausente não é criado automaticamente.
Reiniciar o processo após editar; sem hot reload de estado durante tick.

| Chave | Padrão | Limites / custo |
|---|---|---|
| `diagnostics.enabled` | `false` | somente `true`/`false`; desligado não registra callbacks de tick nem cria ring |
| `diagnostics.windowSamples` | `4096` | inteiro 128..65536; retenção fixa de 8×N bytes + objeto; 32768 bytes de primitivos no padrão |

Configuração inválida, ilegível, grande (>8192 bytes UTF-8) ou com chave
inexistente: desativa **todo o diagnóstico** e avisa uma vez, sem imprimir
conteúdo/caminhos do arquivo. Campos ausentes recebem padrão. Não reescreve
arquivo incorreto. Sintaxe `.properties` Java: comentários `#`/`!`, última
ocorrência de uma chave vence; **não usar chaves repetidas**. Sem JSON permissivo,
coerção de booleano ou aplicação parcial de config inválida.

Com diagnóstico ativo, executar `/omperf` como administrador (permissão vanilla
`COMMANDS_ADMIN`, normalmente nível 3), console ou RCON. Somente o solicitante
recebe a resposta; `sendSuccess(..., false)` não transmite aos demais operadores.
Consulta a cada ≥5 segundos **por sessão de servidor**, compartilhada entre
operadores; não há fila se chegarem várias solicitações. Resposta de cooldown
não ordena/copia o histórico. O comando continua disponível para mostrar estado
quando diagnóstico está desligado.

## Significado e limites da medida

- Duração monotônica `START_SERVER_TICK` → `END_SERVER_TICK`, sem espera para
  o próximo tick. Ordem de callbacks de terceiros pode afetar o intervalo;
  não é medição de todo o loop nem TPS real.
- Janela dos últimos N ticks concluídos (4096 ≈205 s a 20 ticks/s), não histórico
  completo de sessão; ticks lentos não são descartados seletivamente. Eventos
  ausentes durante pausa não criam amostras fictícias.
- p50/p95/p99 pelo método nearest-rank (`ceil(p*N)`), min/max/média no núcleo;
  comando mostra p50/p95/p99/max, contagem >50 ms e duração total desses ticks.
  Exatamente 50 ms não é contado como >50 ms.
- Inserção O(1), sem alocação por amostra; duas chamadas `nanoTime` por tick.
  Relatório sob demanda copia e ordena O(N log N) no thread servidor, com
  alocação temporária O(N). Janela máxima pode aumentar a pausa da consulta:
  não é grátis e não é recomendada sem medição. Não há ordenação por tick.
- Nenhum envio automático, arquivo de histórico, coleta de nomes, IPs, chat,
  coordenadas, inventários ou upload. Sem logging por tick.
- Parada limpa libera referência ao servidor e histórico; nova sessão não herda
  amostras ou cooldown. Não há worker, estado persistido nem escrita em mundos.
- Defaults conservadores: nenhuma opção de distância, simulação, render, rede
  ou inventário. Tais opções **não foram implementadas** sem evidência/testes.

Desinstalação: remover o JAR e opcionalmente a configuração. Este módulo não
possui dados de mundo para migrar. Isso é propriedade do código, não comprovação
empírica de integridade em runtime: os testes de salvamento ainda estão pendentes.
