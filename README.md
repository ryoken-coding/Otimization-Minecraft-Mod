# Otimization Minecraft Mod — 26.3 Fabric

**Preview de diagnóstico, não um mod de otimização concluído.** Este repositório
começou sem código. A primeira etapa fornece build direcionado ao alvo exato,
telemetria opcional e limitada de ticks e testes independentes. **Nenhum ganho de
FPS/MSPT no Minecraft foi demonstrado; nenhum JAR Fabric foi compilado aqui.**
O sandbox bloqueia downloads oficiais de Gradle/Fabric/Minecraft. Não houve
substituição por outra versão ou loader.

## Alvo fixado

Minecraft **26.3** · Fabric Loader **0.20.0** · Fabric API **0.163.0+26.3** ·
Java **25** · Loom **1.18** · Gradle **9.7.1**.

Fontes oficiais e ambiente: [auditoria e plano priorizado](docs/AUDITORIA.md).
Essas são versões selecionadas; integração real ainda precisa de build/runtime.

## Implementado

- Diagnóstico de servidor dedicado e integrado via eventos públicos Fabric,
  **desligado por padrão**. Sem tick listener ou buffer quando desligado.
- Ring de primitivos limitado, inserção O(1), sem alocação por amostra;
  p50/p95/p99/max e ticks >50 ms em consulta administrativa `/omperf`.
- Configuração validada, relatório com cooldown global, liberação por sessão;
  sem coleta sensível, uploads, histórico em disco, worker ou pacote customizado.
- Testes determinísticos do núcleo e microbenchmark sintético reproduzível.
- Nenhum item, bloco, receita, entidade, dimensão ou mecânica adicionada/alterada.
  Nenhum mixin, alteração de simulação/distância/render, inventário persistente
  ou dependência de mod de conteúdo.

## Build e testes

Com **JDK 25 completo** e acesso aos repositórios oficiais:

```sh
./gradlew --no-daemon clean check build
# Núcleo sem resolver dependências de Minecraft (Bash >=4):
scripts/test-core.sh
scripts/benchmark-core.sh results.csv
```

No Windows nativo: `gradlew.bat check build`. Artefato esperado após sucesso:
`build/libs/otimization-minecraft-mod-0.1.0-dev.jar` (não `-sources.jar`).
CI de build foi configurada, mas não executada nesta sessão.

[Instalação, comando, config e custos](docs/CONFIGURACAO.md). Instalar no
**servidor** a diagnosticar ou no cliente para seu **servidor integrado**;
clientes remotos não precisam do mod. Copiar
[`config/omperf.properties.example`](config/omperf.properties.example) para
`config/omperf.properties`, habilitar explicitamente e reiniciar.

## Evidências e pendências

- [Testes realmente executados, reprodução e gates pendentes](docs/TESTES.md):
  **155315 verificações do núcleo passaram**, compiladas como Java 25 com ECJ.
- [Protocolo A/B e cenários de benchmark](docs/BENCHMARKS.md).
- [Resultados brutos e limitações](docs/benchmarks/RESULTADOS.md): somente
  microbenchmark de telemetria, sem medição de Minecraft.
- [Decisões arquiteturais](docs/ARQUITETURA.md).
- [Matriz de compatibilidade](docs/COMPATIBILIDADE.md): **nenhuma combinação de
  jogo testada**; builds 26.3 Fabric dos mods de conteúdo não confirmados para teste.

Faltam compilação real do adaptador, execução cliente/servidor, profiling de
Minecraft, testes multiplayer/inventários/salvamento e sessões longas. Mudanças
invasivas e QoL de persistência foram adiadas por falta dessa evidência.
Não instalar este preview em mundo de produção sem validar e fazer backup.

## Contribuir

Manter o alvo 26.3 Fabric; reproduzir o núcleo e o build antes de enviar mudanças.
Nova otimização precisa de perfil, A/B repetido, preservação de semântica,
config independente e reversão sem ganho. Documentar resultados negativos.
Testes destrutivos somente em cópias descartáveis. Ver [licença](LICENSE) e
[atribuições](THIRD_PARTY_NOTICES.md).
