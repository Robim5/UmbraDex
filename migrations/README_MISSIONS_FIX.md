# 🎯 UmbraDex - Correção do Sistema de Missões

## 📋 Problemas Resolvidos

### 1. ❌ Problema: Progresso não é contínuo entre missões
**Antes:** Se tinhas 30 Pokémon capturados e resgataste a missão de 10, a próxima missão de 25 começava em 0/25.

**Depois:** ✅ A próxima missão mostra 30/25 (já completa!) porque o progresso é GLOBAL e contínuo.

### 2. ❌ Problema: Missões de equipas não funcionavam
**Antes:** Criar equipas não atualizava o progresso das missões de `create_team`.

**Depois:** ✅ Um novo trigger v2 atualiza automaticamente o progresso quando crias equipas.

### 3. ❌ Problema: Missões de favoritos não funcionavam
**Antes:** Favoritar Pokémon não atualizava as missões corretamente.

**Depois:** ✅ O trigger v2 agora funciona corretamente.

### 4. ❌ Problema: Missões de tipos e gerações não funcionavam
**Antes:** Capturar Pokémon de tipo Fire não atualizava missões de `collect_type_fire`.

**Depois:** ✅ O novo trigger busca os tipos no cache e atualiza TODAS as missões relacionadas.

### 5. ❌ Problema: Missões de nível e gold não existiam
**Antes:** Subir de nível ou ganhar gold não atualizava nenhuma missão.

**Depois:** ✅ Novos triggers para `reach_level` e `earn_gold`.

---

## 🚀 Como Aplicar as Correções

### Passo 1: Executar o Script SQL no Supabase

1. Vai ao teu projeto no [Supabase Dashboard](https://app.supabase.com)
2. Abre o **SQL Editor**
3. Cola o conteúdo do ficheiro `migrations/002_fix_missions_system_complete.sql`
4. Clica em **Run**

### Passo 2: Verificar se funcionou

Execute esta query para verificar:

```sql
-- Ver stats globais de um usuário
SELECT * FROM user_global_stats LIMIT 5;

-- Ver progresso de missões ativas
SELECT m.title, mp.current_value, m.requirement_value, mp.status
FROM missions_progress mp
JOIN missions m ON m.id = mp.mission_id
WHERE mp.status = 'active'
LIMIT 20;
```

### Passo 3: Sincronizar usuário existente (se necessário)

Se o script não corrigiu automaticamente, executa:

```sql
SELECT initialize_global_stats_from_existing('SEU_USER_ID_AQUI');
```

---

## 📊 Nova Tabela: `user_global_stats`

Esta tabela armazena o progresso TOTAL do jogador, independente das missões:

| Coluna | Descrição |
|--------|-----------|
| `total_pokemon_collected` | Total de Pokémon na Living Dex |
| `total_favorites` | Total de Pokémon favoritos |
| `total_teams` | Total de equipas criadas |
| `type_fire`, `type_water`, etc. | Contagem por tipo |
| `gen_1`, `gen_2`, etc. | Contagem por geração |
| `total_skins`, `total_badges`, etc. | Itens comprados |

---

## 🔧 Novos Triggers

| Trigger | Tabela | O que faz |
|---------|--------|-----------|
| `trigger_pokemon_added` (v2) | `user_pokemons` | Atualiza missões de coleção, tipo e geração |
| `trigger_favorite_added` (v2) | `favorites` | Atualiza missões de favoritos |
| `trigger_team_created` (v2) | `teams` | Atualiza missões de equipas |
| `trigger_item_purchased` (v2) | `inventory` | Atualiza missões de loja e categorias |
| `trigger_level_changed` | `profiles` | Atualiza missões de nível |
| `trigger_gold_earned` | `profiles` | Atualiza missões de gold |

---

## 🎮 Como funciona agora

1. **Adicionar Pokémon à Living Dex:**
   - +10 XP
   - Atualiza `collect_pokemon` em TODAS as missões ativas
   - Atualiza `collect_type_X` para cada tipo do Pokémon
   - Atualiza `collect_gen_X` para a geração do Pokémon
   - Stats globais são incrementados

2. **Resgatar uma missão:**
   - Dá as recompensas (Gold + XP)
   - Marca como `completed`
   - Ativa a próxima missão da cadeia **COM O PROGRESSO GLOBAL JÁ APLICADO**

3. **Exemplo prático:**
   - Tens 50 Pokémon capturados
   - Resgatas a missão "Collector 25" (25 Pokémon)
   - A missão "Collector 50" é ativada mostrando **50/50** (já completa!)
   - Podes resgatar imediatamente

---

## 🔄 RPC disponível para o cliente

```kotlin
// Sincronizar missões manualmente (se necessário)
db.rpc("sync_user_missions", buildJsonObject {
    put("p_user_id", userId)
})
```

---

## ✅ Checklist de Verificação

Depois de aplicar as correções, verifica:

- [ ] As missões de equipas contam quando crias uma equipa
- [ ] As missões de favoritos contam quando favoritas um Pokémon
- [ ] As missões de tipo contam (ex: Fire Rookie ao capturar Charmander)
- [ ] As missões de geração contam
- [ ] O progresso não reseta quando resgatas uma missão
- [ ] A próxima missão na cadeia mostra o progresso correto

---

## 🐛 Problemas conhecidos

1. **Pokémon no cache (RESOLVIDO):** Anteriormente, as missões de tipo só funcionavam se o Pokémon estivesse no `pokemon_cache`. Agora, os tipos são passados diretamente pelo cliente Kotlin e atualizados via RPC `update_type_progress`.

2. **Usuários antigos:** Usuários criados antes desta migração precisam executar `initialize_global_stats_from_existing()` para sincronizar. O script de migração já faz isso automaticamente para todos os usuários existentes.

---

## 📝 Novas RPCs disponíveis

| RPC | Descrição |
|-----|-----------|
| `sync_user_missions(p_user_id)` | Sincroniza todas as missões com os stats globais |
| `update_type_progress(p_user_id, p_pokemon_types)` | Atualiza contadores de tipo quando um Pokémon é adicionado |
| `claim_mission_reward_v2(p_user_id, p_mission_id)` | Resgata missão mantendo progresso contínuo |

---

**Data:** Janeiro 2026
**Versão:** 2.1
