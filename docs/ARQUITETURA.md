# Decisões arquiteturais

## ADR-001 — Medir antes de alterar a simulação

Sem código/perfil inicial e sem runtime acessível, implementar um cache de mundo,
culling, deduplicação de pacotes ou ticks assíncronos seria especulativo. Não
fazemos isso. O preview fornece uma ferramenta opt-in de baixo alcance para a
próxima fase de profiling. Não é entregue como mod de otimização pronto.

## ADR-002 — Separar o núcleo testável do adaptador Fabric

- `config/`: leitura limitada a 8193 bytes, validação e defaults fail-closed.
- `telemetry/`: primitivos, ring limitado, lifecycle temporal e formatação; sem
  referência Fabric, Minecraft, jogador ou mundo.
- `server/`: único adaptador, APIs de eventos públicas, comando administrativo,
  estado por servidor ativo; servidor dedicado e integrado. Sem classes cliente.
- `src/test/`: testes independentes com oracle determinístico e adversariais.
- `src/benchmark/`: comparação sintética não empacotada no mod.

Configuração é carregada uma vez. Com módulo desligado, só se registra o comando
que informa o estado, sem coleta. Sem dependência de mods opcionais, reflexão,
classe condicional, mixins, `@Overwrite` ou redirects. Não há integração com
inventário nem novas definições de conteúdo/receitas. A única mensagem é uma
resposta administrativa explícita sobre infraestrutura, não QoL de gameplay.

## ADR-003 — Thread confinement, não paralelismo especulativo

Callbacks de servidor/command executam no thread servidor. START marca tempo;
END grava duração não negativa. END órfão é ignorado; dois START consecutivos
invalidam o par. Clock `nanoTime` é usado por diferença, inclusive overflow
assinado (intervalos menores que 2^63 ns). STOP limpa e libera a referência.
Não usamos locks, filas, executores, referências globais a entidades, mundo ou
cache compartilhado. Thread safety depende desse contrato; núcleo não é uma
API concorrente. Não movemos estado mutável do mundo para workers.

## ADR-004 — Janela limitada e consultas raras

Ring preserva exatamente os últimos N tempos sem shift de array. Inserções
não calculam percentis e não alocam. Consulta cria um snapshot imutável ordenado,
sem expor o array original. Contagem >50 ms e percentis são somente da janela,
não totais desde o início; isso evita interpretação enganosa de retenção.
Média usa soma double para não transbordar um long com durações adversariais.

Não usamos um histograma aproximado porque aqui podemos obter percentis exatos
em consultas raras; se a consulta impactar ticks, manter desativado ou reduzir
janela, e medir antes de substituir. Cooldown global de 5 s impede uma fila de
sorts gerada por vários operadores. Não há relatório periódico a esconder custo.
Limite de 65536 evita arrays ilimitados, mas ainda exige medir pausas reais.

## ADR-005 — QoL persistente adiada

Persistir ingredientes muda o fechamento vanilla e cria propriedade/transferência
transacional. Sem multiplayer, crash/recovery e teste de menus customizados,
não é uma primeira mudança de baixo risco. Nenhum código de persistência foi
adicionado; desligar diagnósticos não afeta qualquer simulação. QoL futura terá
módulo/config separados, testes autoritativos e nenhuma dependência obrigatória
de mods de conteúdo.
