# Matriz de compatibilidade — 2026-10-15

**Nenhuma combinação de jogo foi executada neste sandbox. Não há selo de
compatibilidade.** Não se deve inferir compatibilidade só porque usamos eventos.
A rede permite consultar GitHub, mas não artefatos de Minecraft/Fabric/Modrinth/
CurseForge. Builds listados abaixo são evidência de fontes, não JARs instalados.

| Combinação | Evidência consultada | Disponibilidade para teste aqui | Runtime |
|---|---|---|---|
| 26.3 Fabric sem mods de conteúdo, servidor | pins oficiais na auditoria | bloqueada por rede/JDK completo | não executado |
| 26.3 Fabric cliente / integrado | entrypoint comum, sem classe cliente | rede e sem GPU | não executado |
| Cliente sem mod → servidor com mod | somente comando/chat vanilla, sem packet custom | multiplayer bloqueado | não executado |
| Mekanism | branch padrão 26.3: `minecraft_version=26.3`, `neo_version=26.3.0.76`, mod 10.8.0; release publicada mais recente `v1.21.1-10.7.19.85` | **não disponível para teste**: não foi identificado artefato 26.3 **Fabric** | não executado |
| Create | upstream `mc1.21.1/dev`: 6.0.11 / NeoForge; release `mc1.21.1-6.0.10`; port Fabric também consultado, sem GitHub release/latest | **não disponível para teste**: artefato 26.3 Fabric não confirmado | não executado |
| Mowzie’s Mobs | fonte pública: MC 1.21.1, NeoForge 21.1.162, mod 1.8.2; sem release/latest GitHub | **não disponível para teste**: artefato 26.3 Fabric não confirmado | não executado |
| Ice and Fire | release `1.20.1-2.1.13-beta-5`, arquivo `iceandfire-2.1.13-1.20.1-beta-5.jar` | **não disponível para teste**: artefato 26.3 Fabric não confirmado | não executado |
| Applied Energistics 2 | main: MC 26.3, NeoForge 26.3.0.43-beta; release `v26.1.13-beta` | **não disponível para teste**: não foi identificado artefato 26.3 **Fabric** | não executado |
| Sodium/Lithium/outras otimizações | nenhum binário instalado | não verificada | não executado |

Pesquisa não exaustiva: falta de confirmação em GitHub **não prova inexistência**
em outros canais. Não instalamos um mod NeoForge/Forge para substituir Fabric,
nem tratamos uma branch/source tree como build publicado. Nenhum código desses
mods foi copiado, nem é necessário para inicializar este projeto.

## Fontes e revisões consultadas

- [Mekanism](https://github.com/mekanism/Mekanism/tree/8c5273c7da341eadd7d8a879dd631fb63aeef7e2), `gradle.properties`; release/latest via API.
- [Create upstream](https://github.com/Creators-of-Create/Create/tree/7ff2760e4924d35d4bc2e22c4b19923f0178cd16), `gradle.properties`.
- [Create Fabric](https://github.com/Fabricators-of-Create/Create/tree/4c3fe779d135d592548e4cb09da04d742d2637b7), árvore/build e API release/latest (404).
- [Mowzie's Mobs público](https://github.com/BobMowzie/MowziesMobs-Public/tree/7aa17309337cbd4102efc418cba12a4983b1540c), `gradle.properties`. Consulta inicial a `BobMowzie/MowziesMobs` retornou 404; corrigida para o repositório público.
- [Ice and Fire](https://github.com/AlexModGuy/Ice_and_Fire/releases/tag/1.20.1-2.1.13-beta-5); branch padrão consultada `02b2922003cbce3c67dad55dfea79acf1721e0db`.
- [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/b141e91764897d3844c9debd4210396b5f9b5238), `gradle.properties`.

## Como promover uma linha a “testada”

Arquivar URL/hash do JAR 26.3 Fabric e dependências; executar S0–S6/C0–C1
aplicáveis em `BENCHMARKS.md`, com o mod ausente/desligado/ligado. Além de boot:
criar/carregar mundo, conectar/sair, renderizar modelos/partículas/menus,
exercitar receitas/máquinas/redes/inventários/block entities sob carga,
salvar/reiniciar e verificar NBT e contagens. Examinar logs repetidos, mixins,
dessincronização e conflito com otimizações instaladas. Somente registrar
versões/cenários realmente executados. Sem esse registro, continuar “não validado”.
