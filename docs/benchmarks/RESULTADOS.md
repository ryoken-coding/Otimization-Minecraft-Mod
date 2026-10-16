# Resultados medidos — 2026-10-15

**Estes não são resultados de Minecraft.** Fonte bruta:
[`telemetry-2026-10-15.csv`](telemetry-2026-10-15.csv).

## Microbenchmark de arquitetura da coleta

Java runtime Temurin 25.0.2+10, ECJ 3.45.0, release 25; Linux / Xeon 2 vCPU
compartilhadas / RAM limitada ~3.7 GiB / sem GPU. Heap do processo 128 MiB,
GC padrão, sem flags extras. Ver detalhes em `../AUDITORIA.md`.

Janela 4096, entradas determinísticas seed 263015 (0..100000000 ns), 3 rodadas
de aquecimento por braço, 200000 inserções e 200 relatórios por rodada, 5
repetições medidas alternando ordem. Mesma JVM, **não forks independentes**.

| Implementação / operação | Mediana ns/op | Min..max ns/op | Bytes alocados/op |
|---|---:|---:|---:|
| Referência sintética com shift / inserção | 234.136 | 233.374..258.402 | 0 |
| Ring primitivo / inserção | 5.161 | 1.421..39.543 | 0 |
| Referência sintética com shift / relatório | 35374.600 | 34775.550..49887.950 | 34056..34136 |
| Ring primitivo / relatório | 35404.015 | 35134.630..44809.710 | 33992 |

Ambos retêm somente os últimos 4096 valores e produzem estatísticas iguais.
Inserir no ring evita copiar 4095 longs a cada tick. **A referência foi criada
para este experimento, não existia no Minecraft/repositório.** Não há comparação
“Minecraft antes/depois” nem ganho atribuído à simulação. A referência também
não aloca por inserção; não afirmamos redução de GC nela.

Resultado negativo/limitação: relatório do ring não tem redução consistente de
tempo e sua mediana foi ligeiramente maior; ambos copiam/ordenam. Grande variação
na inserção do ring sugere JIT/escalonamento; não usar uma razão multiplicativa
como promessa de ganho. Contador de alocações mede só thread atual, não memória
nativa ou todo o processo. O benchmark não inclui relógios, eventos Fabric,
permissões nem envio de comando. O ring é preferido por custo O(1)/retenção
limitada, não por promessa baseada em ns/op de um único hardware.

## Métricas do jogo (nenhum ensaio executado)

| Métrica pedida | Sem mod | Com mod | Motivo |
|---|---|---|---|
| FPS / 1% lows / frametime p50/p95/p99 | N/D | N/D | sem cliente ou GPU |
| MSPT p50/p95/p99/max / TPS / ticks lentos | N/D | N/D | runtime/artefatos inacessíveis |
| Startup / load mundo / chunks novos | N/D | N/D | runtime/artefatos inacessíveis |
| Heap aquecido / pico / nativo / GC | N/D | N/D | nenhum processo de jogo |
| CPU por thread / GPU / IO | N/D | N/D | nenhum processo de jogo |
| Bytes/s / pacotes/s / RTT / filas / resposta | N/D | N/D | sem servidor/multiplayer controlado |
| Salvamento / reinício / inventários | não testado | não testado | sem mundo/servidor |

Não há resultado que permita afirmar melhoria de FPS, MSPT, memória de mundo,
carregamento, rede ou ping. Meta de 20 TPS / orçamento 50 ms ainda não avaliada.
Próximo passo é executar o protocolo em `../BENCHMARKS.md` após liberar runtime.
