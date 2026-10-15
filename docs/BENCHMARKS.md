# Protocolo reproduzível (definido antes da implementação)

## Controles e linha de base

Nunca comparar versões/loaders diferentes. Usar 26.3, Java 25 com patch exato,
mesmo hardware, driver, mundo, modlist, resource packs e configurações.
Preservar cópias externas; nunca testar falhas em mundo de produção.

Braços: A = Minecraft 26.3 sem Fabric (referência opcional); B = Fabric com API,
sem este mod; C = B + mod com diagnóstico desabilitado; D = B + diagnóstico
habilitado. A/B não isola este mod; **B/C/D** isolam o custo da implementação.
Não comparar snapshot com release. Não usar o diagnóstico como única medida de si mesmo.

Por braço: aquecer 5 min, medir 15 min, pelo menos 5 repetições em ordem
alternada B-D-C-C-D-B, restaurando a mesma cópia de mundo a cada ensaio.
Sincronizar ações por tick/roteiro gravado, fixar seed `263015`, dificuldade
normal, gamerules originais, horário inicial, clima e posição inicial iguais.
Registrar pausas quando vazio; não interpretar servidor pausado como TPS zero.
Não alterar flags JVM entre braços. Heap inicial para **ensaio**, não recomendação:
servidor `-Xms1G -Xmx2G`, GC padrão Java 25; registrar falhas por memória.
Repetir com heap 1G como estresse. Em hardware de referência ajustar antes de
iniciar todo o conjunto, registrando o valor; não ajustar apenas um braço.

## Cenários pendentes no runtime 26.3

| ID | Montagem e ação | Verificações |
|---|---|---|
| S0 | mundo novo, 1 jogador parado, view=10, simulation=6, 15 min | custo base / tick, pausas, startup/load |
| S1 | fixture salva: 1000 entidades agrupadas, 1000 hoppers, clock redstone e fluidos; 1 jogador, mesmas posições e NBT por braço | tick p50/p95/p99/max, ticks >50 ms, inventário/NBT, CPU por thread |
| S2 | mundo não pré-gerado; rota 10.000 blocos no eixo X a 20 blocos/s por teleporte agendado; view 10/16 (ensaios separados), simulation=6 | chunks/s, tempo de entrada, IO, filas, pico heap, salvamento |
| S3 | 2 jogadores: 1000 ciclos abrir/fechar, shift-click, disputa por slots, desconectar durante transferência | inventário agregado antes/depois, sem perda/duplicação; não automatizado ainda |
| S4 | 100 ciclos entrar/sair/trocar dimensão/reload; sessão 4h | heap retido após aquecimento, GC, listeners/estado não retidos |
| S5 | cópia descartável de S2: save durante carga, kill -9 em 30/60/90 s, reinício em cada caso, backup por tentativa | logs, regiões/NBT legíveis, contagem de itens; distinguir dano vanilla do mod |
| S6 | duas máquinas/VMs, netem RTT +50/+150 ms, jitter 10 ms, perda 1%, disco com limite IO declarado | RTT real, bytes/s, pacotes/s, filas e resposta de comando; sem alegar vencer latência física |
| C0 | cliente, rota/câmera gravadas, resolução 1920x1080, VSync off, limite FPS igual, view=10, pack padrão | frametimes brutos, mediana/p95/p99, FPS, 1% low definido como 1000/média dos 1% frames mais lentos |
| C1 | C0 com pack pesado identificado por SHA-256 | visuais, VRAM, GPU e recursos; não presumir ganho |

S1 exige fixture/datapack com posições/estados exatos e hash arquivado **antes**
de alegar reprodutibilidade completa; estes são planos, não mundos já fornecidos
ou ensaios executados. Mods de conteúdo só quando existir artefato 26.3 Fabric
verificado. Registrar resultados separados, não substituir por NeoForge.

## Coleta

No servidor/cliente: usar JFR do próprio Java (`-XX:StartFlightRecording=filename=trial.jfr,settings=profile`)
e GC log (`-Xlog:gc*:file=gc.log:time,uptime,level,tags`) nos braços equivalentes.
Guardar logs, build SHA, hashes de configs/mods/mundo inicial, runtime Java,
SO, CPU física/vCPUs, GPU/driver, heap, armazenamento e ações exatas.
Coletar ticks completos via profiler externo à janela do mod; comparar o
intervalo START→END do mod sem apresentá-lo como duração total do loop.
O relatório do mod exclui espera para o próximo tick e callbacks fora do seu
intervalo. **Não calcula TPS real**; não inferir TPS de `1000/MSPT`.

Publicar CSV: `trial,arm,scenario,metric,unit,value` e dados brutos. Medir
MSPT p50/p95/p99/max, contagem/duração >50ms, TPS observado pelo profiler,
startup/load/chunks, heap aquecido/pico/nativo se disponível, GC, CPU por
thread, GPU (cliente), disco e rede. Reportar mediana por ensaio e mínimo/
máximo entre repetições; não misturar milhões de amostras como ensaios independentes.
Aceitar ganho só além da variação observada, sem regressão significativa ou
alteração de resultado. Publicar resultados negativos. Campos não medidos = N/D.

## Microbenchmark independente executável

`scripts/test-core.sh` executa testes determinísticos sem bibliotecas externas.
`scripts/benchmark-core.sh results.csv` compara exclusivamente duas implementações
de **coleta de telemetria**, usando as mesmas amostras e verificando relatórios:

- referência sintética: array cronológico limitado com shift a cada amostra;
- implementação: ring de primitivos O(1); sort somente na consulta.

A referência não veio do Minecraft nem existia no repositório. O ensaio mostra
por que evitar essa arquitetura na nova telemetria; não prova ganho de MSPT/FPS
ou overhead total de eventos/relógios no Minecraft. Registrar heap 128 MiB,
mesmo JDK, 5 repetições, aquecimento e ordem alternada. Separar inserção e
relatório. Tempo é informativo, nunca teste CI com limiar instável de ns/op.
