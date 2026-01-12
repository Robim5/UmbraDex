# 🗄️ UmbraDex - Documentação Completa da Base de Dados (v3.0 Final)

**Estado do Sistema:** Produção (Stable & Production-Ready)  
**Motor de Base de Dados:** PostgreSQL 15 (via Supabase)  
**Última Atualização:** Janeiro 2026 - Sistema de Missões v2.0 + Correções de Signup  
**Status:** ✅ Totalmente Funcional

---

## 📋 Índice

1. [Visão Geral](#1-visão-geral)
2. [Configurações Globais](#2-configurações-globais)
3. [Esquema Completo das Tabelas](#3-esquema-completo-das-tabelas)
4. [Sistema de Missões v2.0](#4-sistema-de-missões-v20)
5. [Lógica de Negócio (Stored Procedures)](#5-lógica-de-negócio-stored-procedures)
6. [Automação (Triggers)](#6-automação-triggers)
7. [RPCs (Funções Cliente)](#7-rpcs-funções-cliente)
8. [Performance e Segurança](#8-performance-e-segurança)
9. [Histórico de Correções e Migrações](#9-histórico-de-correções-e-migrações)
10. [Diagrama de Relacionamentos](#10-diagrama-de-relacionamentos)

---

## 1. Visão Geral

### 1.1. Propósito
UmbraDex é uma aplicação gamificada de Pokédex que permite aos utilizadores:
- Construir uma Living Dex completa (coleção de todos os Pokémon)
- Criar e gerir equipas de Pokémon
- Progredir através de um sistema de níveis e missões
- Personalizar o perfil com skins, temas, badges e títulos
- Competir através de um sistema de economia (Gold/XP)

### 1.2. Arquitetura
- **Backend:** Supabase (PostgreSQL + Auth + Row Level Security)
- **Cliente:** Android (Kotlin)
- **API Externa:** PokéAPI (cache local)

---

## 2. Configurações Globais

### 2.1. Extensões Ativas

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- Geração de UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- Funções de criptografia
```

### 2.2. Tipos Personalizados (Enums)

```sql
-- Categorias de itens na loja
CREATE TYPE item_category AS ENUM (
    'skin',        -- Avatares visuais
    'theme',       -- Temas da aplicação
    'badge',       -- Insígnias/Emblemas
    'name_color',  -- Cores do nome
    'title'        -- Títulos desbloqueáveis
);

-- Estados das missões
CREATE TYPE mission_status AS ENUM (
    'locked',      -- Ainda não disponível
    'active',      -- Em progresso
    'completed'    -- Concluída
);

-- Sistema de raridade
CREATE TYPE rarity_type AS ENUM (
    'common',      -- Comum
    'rare',        -- Raro
    'epic',        -- Épico
    'legendary'    -- Lendário
);
```

---

## 3. Esquema Completo das Tabelas

### 3.1. `profiles` - Perfil do Utilizador

**Descrição:** Tabela central que estende `auth.users` do Supabase com dados do jogo.

```sql
CREATE TABLE public.profiles (
    -- Identificação
    id uuid PRIMARY KEY REFERENCES auth.users ON DELETE CASCADE,
    email text NOT NULL,
    username text UNIQUE NOT NULL,
    
    -- Recursos do Jogo
    gold bigint DEFAULT 0 CHECK (gold >= 0),
    xp bigint DEFAULT 0 CHECK (xp >= 0),
    level int DEFAULT 1 CHECK (level BETWEEN 1 AND 200),
    xp_for_next_level bigint DEFAULT 60,
    
    -- Equipamento Atual
    equipped_pokemon_id int,  -- Pokedex ID do Partner
    equipped_skin text DEFAULT 'standard_male1',
    equipped_theme text DEFAULT 'theme_default',
    equipped_badge text DEFAULT 'start_badget',
    equipped_title text DEFAULT 'Rookie',
    equipped_name_color jsonb DEFAULT '["#FFFFFF"]',
    
    -- Dados Pessoais
    birth_date date,
    pokemon_knowledge text CHECK (pokemon_knowledge IN ('expert', 'intermediate', 'beginner')),
    favorite_type text,
    
    -- Estatísticas Acumuladas
    total_time_seconds bigint DEFAULT 0,
    total_gold_earned bigint DEFAULT 0,
    total_xp_earned bigint DEFAULT 0,
    pet_clicks int DEFAULT 0,
    
    -- Timestamps
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now(),
    last_login timestamptz DEFAULT now()
);
```

**Índices:**
```sql
CREATE INDEX idx_profiles_level ON public.profiles(level);
CREATE INDEX idx_profiles_username ON public.profiles(username);
```

---

### 3.2. `user_global_stats` - Estatísticas Globais (Missões v2.0)

**Descrição:** Tracking global de progresso independente das missões ativas. Permite que novas missões sejam ativadas com o progresso já acumulado.

```sql
CREATE TABLE public.user_global_stats (
    user_id uuid PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    
    -- Contadores de Coleção
    total_pokemon_collected int DEFAULT 0,
    total_favorites int DEFAULT 0,
    total_teams int DEFAULT 0,
    
    -- Contadores por Tipo Pokémon (18 tipos)
    type_normal int DEFAULT 0,
    type_fire int DEFAULT 0,
    type_water int DEFAULT 0,
    type_grass int DEFAULT 0,
    type_electric int DEFAULT 0,
    type_ice int DEFAULT 0,
    type_fighting int DEFAULT 0,
    type_poison int DEFAULT 0,
    type_ground int DEFAULT 0,
    type_flying int DEFAULT 0,
    type_psychic int DEFAULT 0,
    type_bug int DEFAULT 0,
    type_rock int DEFAULT 0,
    type_ghost int DEFAULT 0,
    type_dragon int DEFAULT 0,
    type_dark int DEFAULT 0,
    type_steel int DEFAULT 0,
    type_fairy int DEFAULT 0,
    
    -- Contadores por Geração (Gen 1-9)
    gen_1 int DEFAULT 0,  -- Pokémon 1-151
    gen_2 int DEFAULT 0,  -- Pokémon 152-251
    gen_3 int DEFAULT 0,  -- Pokémon 252-386
    gen_4 int DEFAULT 0,  -- Pokémon 387-493
    gen_5 int DEFAULT 0,  -- Pokémon 494-649
    gen_6 int DEFAULT 0,  -- Pokémon 650-721
    gen_7 int DEFAULT 0,  -- Pokémon 722-809
    gen_8 int DEFAULT 0,  -- Pokémon 810-905
    gen_9 int DEFAULT 0,  -- Pokémon 906-1025
    
    -- Contadores de Loja/Personalização
    total_shop_purchases int DEFAULT 0,
    total_skins int DEFAULT 0,
    total_badges int DEFAULT 0,
    total_themes int DEFAULT 0,
    total_name_colors int DEFAULT 0,
    total_titles int DEFAULT 0,  -- NOVO: contador de títulos
    
    -- Metadata
    updated_at timestamptz DEFAULT now()
);
```

**Índices:**
```sql
CREATE INDEX idx_user_global_stats_user ON public.user_global_stats(user_id);
```

---

### 3.3. `shop_items` - Catálogo da Loja

**Descrição:** Todos os itens disponíveis para compra.

```sql
CREATE TABLE public.shop_items (
    id serial PRIMARY KEY,
    type item_category NOT NULL,
    name text UNIQUE NOT NULL,  -- Slug identificador
    rarity rarity_type NOT NULL,
    price int DEFAULT 0,
    min_level int DEFAULT 0,
    asset_url text,
    is_available boolean DEFAULT true,
    created_at timestamptz DEFAULT now()
);
```

**Índices:**
```sql
CREATE INDEX idx_shop_items_type ON public.shop_items(type);
CREATE INDEX idx_shop_items_rarity ON public.shop_items(rarity);
```

---

### 3.4. `missions` - Definições de Missões

**Descrição:** Configuração estática das missões do jogo.

```sql
CREATE TABLE public.missions (
    id serial PRIMARY KEY,
    title text NOT NULL,
    description text,
    category text NOT NULL,  -- 'collection', 'shop', 'level', etc.
    requirement_type text NOT NULL,  -- Código para triggers
    requirement_value int NOT NULL,
    gold_reward int DEFAULT 0,
    xp_reward int DEFAULT 0,
    prerequisite_mission_id int REFERENCES public.missions(id),
    sort_order int DEFAULT 0,
    created_at timestamptz DEFAULT now()
);
```

**Tipos de Requisitos (`requirement_type`):**
- `collect_pokemon` - Capturar X Pokémon
- `collect_type_X` - Capturar X Pokémon do tipo X (fire, water, etc.)
- `collect_gen_X` - Capturar X Pokémon da geração X
- `favorite_count` - Ter X favoritos
- `create_team` - Criar X equipas
- `shop_buy` - Comprar X itens
- `own_skins`, `own_badges`, `own_themes`, `own_name_colors` - Possuir X itens de cada categoria
- `reach_level` - Atingir nível X
- `earn_gold` - Ganhar X gold total

**Índices:**
```sql
CREATE INDEX idx_missions_category ON public.missions(category);
CREATE INDEX idx_missions_prerequisite ON public.missions(prerequisite_mission_id);
```

---

### 3.5. `missions_progress` - Progresso das Missões

**Descrição:** Estado dinâmico de cada missão por utilizador.

```sql
CREATE TABLE public.missions_progress (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    mission_id int NOT NULL REFERENCES public.missions(id) ON DELETE CASCADE,
    status mission_status DEFAULT 'locked',
    current_value int DEFAULT 0,
    completed_at timestamptz,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now(),
    
    UNIQUE(user_id, mission_id)
);
```

**Índices:**
```sql
CREATE INDEX idx_missions_progress_user ON public.missions_progress(user_id);
CREATE INDEX idx_missions_progress_status ON public.missions_progress(status);
```

---

### 3.6. `inventory` - Inventário do Utilizador

**Descrição:** Itens que o utilizador possui.

```sql
CREATE TABLE public.inventory (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    item_id text NOT NULL,  -- Nome do item (corresponde a shop_items.name)
    category item_category NOT NULL,
    obtained_at timestamptz DEFAULT now(),
    
    UNIQUE(user_id, item_id, category)  -- Não pode ter duplicados
);
```

**Índices:**
```sql
CREATE INDEX idx_inventory_user ON public.inventory(user_id);
CREATE INDEX idx_inventory_category ON public.inventory(category);
```

---

### 3.7. `user_pokemons` - Living Dex

**Descrição:** Pokémon capturados pelo utilizador.

```sql
CREATE TABLE public.user_pokemons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    pokedex_id int NOT NULL CHECK (pokedex_id > 0),
    obtained_at timestamptz DEFAULT now(),
    
    UNIQUE(user_id, pokedex_id)  -- Não pode capturar o mesmo Pokémon duas vezes
);
```

**Índices:**
```sql
CREATE INDEX idx_user_pokemons_user ON public.user_pokemons(user_id);
CREATE INDEX idx_user_pokemons_pokedex ON public.user_pokemons(pokedex_id);
```

---

### 3.8. `favorites` - Pokémon Favoritos

**Descrição:** Pokémon marcados como favoritos. O último favorito torna-se automaticamente o Partner equipado.

```sql
CREATE TABLE public.favorites (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    pokedex_id int NOT NULL,
    created_at timestamptz DEFAULT now(),
    
    UNIQUE(user_id, pokedex_id)  -- Não pode favoritar o mesmo Pokémon duas vezes
);
```

**Nota Importante:** O Pokémon com o `created_at` mais recente é automaticamente definido como `equipped_pokemon_id` no perfil.

**Índices:**
```sql
CREATE INDEX idx_favorites_user ON public.favorites(user_id);
CREATE INDEX idx_favorites_created ON public.favorites(created_at DESC);
```

---

### 3.9. `teams` - Equipas de Pokémon

**Descrição:** Equipas criadas pelo utilizador.

```sql
CREATE TABLE public.teams (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 50),
    region text,
    gradient_colors jsonb,  -- Array de cores para UI
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);
```

**Índices:**
```sql
CREATE INDEX idx_teams_user ON public.teams(user_id);
```

---

### 3.10. `team_slots` - Pokémon nas Equipas

**Descrição:** Composição de cada equipa (máximo 6 Pokémon).

```sql
CREATE TABLE public.team_slots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id uuid NOT NULL REFERENCES public.teams(id) ON DELETE CASCADE,
    slot_position int NOT NULL CHECK (slot_position BETWEEN 1 AND 6),
    pokedex_id int NOT NULL,
    level int DEFAULT 50 CHECK (level BETWEEN 1 AND 100),
    
    UNIQUE(team_id, slot_position)
);
```

**Índices:**
```sql
CREATE INDEX idx_team_slots_team ON public.team_slots(team_id);
```

---

### 3.11. `titles` - Títulos Desbloqueáveis

**Descrição:** Títulos que podem ser equipados baseados no nível do jogador.

```sql
CREATE TABLE public.titles (
    id serial PRIMARY KEY,
    name text UNIQUE NOT NULL,
    required_level int NOT NULL,
    rarity rarity_type DEFAULT 'common',
    created_at timestamptz DEFAULT now()
);
```

---

### 3.12. `pokemon_cache` - Cache da PokéAPI

**Descrição:** Cache local dos dados da PokéAPI para melhorar performance.

```sql
CREATE TABLE public.pokemon_cache (
    pokedex_id int PRIMARY KEY,
    name text NOT NULL,
    types jsonb,  -- Array de tipos: ["fire", "flying"]
    sprite_url text,
    data jsonb,  -- Dados completos da API
    cached_at timestamptz DEFAULT now()
);
```

---

### 3.13. `user_sessions` - Histórico de Sessões

**Descrição:** Tracking de tempo online do utilizador.

```sql
CREATE TABLE public.user_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    started_at timestamptz DEFAULT now(),
    ended_at timestamptz,
    duration_seconds int
);
```

**Índices:**
```sql
CREATE INDEX idx_user_sessions_user ON public.user_sessions(user_id);
```

---

## 4. Sistema de Missões v2.0

### 4.1. Características Principais

**✅ Progresso Contínuo**
- Quando uma missão é completada e a próxima é ativada, o progresso já acumulado é MANTIDO
- Exemplo: Se tens 30 Pokémon capturados e resgataste a missão de 10, a próxima de 25 já mostra 30/25 (completa imediatamente)

**✅ Tracking Global**
- O progresso é trackeado mesmo para missões ainda não ativas
- Usa a tabela `user_global_stats` como fonte única de verdade
- Quando missões são ativadas, já mostram o progresso real

**✅ Todos os Tipos de Missões**
- Coleção geral de Pokémon
- Coleção por tipo (18 tipos)
- Coleção por geração (Gen 1-9)
- Favoritos
- Equipas
- Compras na loja (geral e por categoria)
- Nível alcançado
- Gold ganho total

### 4.2. Fluxo de Funcionamento

```
1. Utilizador realiza ação (ex: captura Pokémon)
   ↓
2. Trigger atualiza user_global_stats (incrementa contador)
   ↓
3. Função update_mission_progress_v2() é chamada
   ↓
4. TODAS as missões desse tipo são atualizadas (ativas e futuras)
   ↓
5. Missões com progresso >= requirement_value ficam prontas para claim
   ↓
6. Utilizador clica em "Claim Reward"
   ↓
7. claim_mission_reward_v2() valida, dá recompensas e ativa próxima missão
   ↓
8. Próxima missão é ativada JÁ com o progresso global sincronizado
```

---

## 5. Lógica de Negócio (Stored Procedures)

### 5.1. `add_xp_and_level_up(user_id, xp_amount)` - Sistema de Níveis

**Descrição:** Adiciona XP ao jogador e gere level-ups automáticos.

```sql
CREATE OR REPLACE FUNCTION add_xp_and_level_up(p_user_id uuid, p_xp_amount int)
RETURNS void AS $$
DECLARE
    v_current_xp bigint;
    v_current_level int;
    v_xp_needed bigint;
    v_gold_bonus int;
BEGIN
    -- Buscar estado atual
    SELECT xp, level, xp_for_next_level
    INTO v_current_xp, v_current_level, v_xp_needed
    FROM public.profiles
    WHERE id = p_user_id;
    
    -- Adicionar XP
    v_current_xp := v_current_xp + p_xp_amount;
    
    -- Loop para subir múltiplos níveis se necessário
    WHILE v_current_xp >= v_xp_needed AND v_current_level < 200 LOOP
        v_current_level := v_current_level + 1;
        v_current_xp := v_current_xp - v_xp_needed;
        
        -- Calcular próximo XP needed (Fórmula: 60 + (level-1) * 10)
        v_xp_needed := 60 + ((v_current_level - 1) * 10);
        
        -- Bônus de Gold
        v_gold_bonus := 5;
        IF v_current_level % 10 = 0 THEN
            v_gold_bonus := v_gold_bonus + 150;
        ELSIF v_current_level % 5 = 0 THEN
            v_gold_bonus := v_gold_bonus + 50;
        END IF;
        
        -- Dar gold
        UPDATE public.profiles
        SET gold = gold + v_gold_bonus,
            total_gold_earned = total_gold_earned + v_gold_bonus
        WHERE id = p_user_id;
    END LOOP;
    
    -- Atualizar perfil
    UPDATE public.profiles
    SET xp = v_current_xp,
        level = v_current_level,
        xp_for_next_level = v_xp_needed,
        total_xp_earned = total_xp_earned + p_xp_amount
    WHERE id = p_user_id;
    
    -- Atualizar título se necessário
    PERFORM update_user_title(p_user_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Fórmula de XP:**
- Nível 1→2: 60 XP
- Nível 2→3: 70 XP
- Nível 3→4: 80 XP
- ...
- Nível N→N+1: `60 + ((N - 1) * 10)` XP

**Recompensas de Gold:**
- Base: +5 Gold por nível
- Nível múltiplo de 5: +50 Gold extra
- Nível múltiplo de 10: +150 Gold extra

---

### 5.2. `handle_new_user()` - Registo de Utilizador

**Descrição:** Trigger "fail-safe" executado no signup. NUNCA falha - cada operação está num bloco TRY separado.

```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_username text;
    v_starter_id int;
    v_birth_date date;
    v_knowledge text;
    v_fav_type text;
    v_avatar text;
BEGIN
    -- Extrair dados com fallbacks ultra-seguros
    v_username := COALESCE(
        NEW.raw_user_meta_data->>'username',
        split_part(NEW.email, '@', 1),
        'trainer'
    );
    
    v_avatar := COALESCE(NEW.raw_user_meta_data->>'avatar', 'standard_male1');
    v_knowledge := COALESCE(NEW.raw_user_meta_data->>'pokemon_knowledge', 'beginner');
    v_fav_type := NEW.raw_user_meta_data->>'favorite_type';
    
    -- Starter ID com fallback
    BEGIN
        v_starter_id := (NEW.raw_user_meta_data->>'starter_id')::int;
        IF v_starter_id IS NULL OR v_starter_id < 1 THEN 
            v_starter_id := 1;  -- Default: Bulbasaur
        END IF;
    EXCEPTION WHEN OTHERS THEN
        v_starter_id := 1;
    END;
    
    -- Birth date
    BEGIN
        v_birth_date := (NEW.raw_user_meta_data->>'birth_date')::date;
    EXCEPTION WHEN OTHERS THEN
        v_birth_date := NULL;
    END;
    
    -- OPERAÇÃO 1: Criar perfil (CRÍTICO)
    BEGIN
        INSERT INTO public.profiles (
            id, email, username, birth_date, pokemon_knowledge, favorite_type,
            equipped_pokemon_id, equipped_skin, equipped_theme, equipped_badge,
            equipped_title, gold, xp, level, xp_for_next_level
        ) VALUES (
            NEW.id, NEW.email, v_username, v_birth_date, v_knowledge, v_fav_type,
            v_starter_id, v_avatar, 'theme_default', 'start_badget',
            'Rookie', 0, 0, 1, 60
        );
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'handle_new_user ERROR creating profile: %', SQLERRM;
        -- Tentar versão mínima
        INSERT INTO public.profiles (id, email, username)
        VALUES (NEW.id, NEW.email, v_username);
    END;
    
    -- OPERAÇÃO 2: Itens iniciais (cada item num bloco separado)
    BEGIN
        INSERT INTO public.inventory (user_id, item_id, category)
        VALUES (NEW.id, 'theme_default', 'theme');
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding theme: %', SQLERRM;
    END;
    
    BEGIN
        INSERT INTO public.inventory (user_id, item_id, category)
        VALUES (NEW.id, v_avatar, 'skin');
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding skin: %', SQLERRM;
    END;
    
    BEGIN
        INSERT INTO public.inventory (user_id, item_id, category)
        VALUES (NEW.id, 'start_badget', 'badge');
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding badge: %', SQLERRM;
    END;
    
    BEGIN
        INSERT INTO public.inventory (user_id, item_id, category)
        VALUES (NEW.id, 'Rookie', 'title');
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding title: %', SQLERRM;
    END;
    
    -- OPERAÇÃO 3: Adicionar starter à Living Dex
    BEGIN
        INSERT INTO public.user_pokemons (user_id, pokedex_id)
        VALUES (NEW.id, v_starter_id);
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding pokemon: %', SQLERRM;
    END;
    
    -- OPERAÇÃO 4: Adicionar starter aos favoritos
    BEGIN
        INSERT INTO public.favorites (user_id, pokedex_id)
        VALUES (NEW.id, v_starter_id)
        ON CONFLICT DO NOTHING;
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error adding favorite: %', SQLERRM;
    END;
    
    -- OPERAÇÃO 5: Ativar primeiras missões
    BEGIN
        INSERT INTO public.missions_progress (user_id, mission_id, status)
        SELECT NEW.id, m.id, 'active'
        FROM public.missions m
        WHERE m.prerequisite_mission_id IS NULL
        LIMIT 10;
    EXCEPTION WHEN OTHERS THEN
        RAISE LOG 'Error activating missions: %', SQLERRM;
    END;
    
    -- SEMPRE retorna NEW - NUNCA falha o signup
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Setup Inicial:**
- ✅ Perfil criado com starter como Partner equipado
- ✅ Itens iniciais: Avatar, Tema, Badge, Título
- ✅ Starter adicionado à Living Dex
- ✅ Starter adicionado aos Favoritos (aparece com ❤️ na Pokédex)
- ✅ Primeiras 10 missões ativadas

---

### 5.3. `update_mission_progress_v2(user_id, requirement_type, new_total)` - Atualizar Progresso

**Descrição:** Versão 2.0 que atualiza TODAS as missões do tipo (ativas E futuras) com o valor total.

```sql
CREATE OR REPLACE FUNCTION update_mission_progress_v2(
    p_user_id uuid,
    p_requirement_type text,
    p_new_total int  -- Valor TOTAL atual (não incremento)
)
RETURNS void AS $$
DECLARE
    v_mission record;
BEGIN
    -- Buscar TODAS as missões deste tipo
    FOR v_mission IN
        SELECT m.*, mp.status
        FROM public.missions m
        LEFT JOIN public.missions_progress mp 
            ON m.id = mp.mission_id AND mp.user_id = p_user_id
        WHERE m.requirement_type = p_requirement_type
        ORDER BY m.requirement_value ASC
    LOOP
        -- Se missão está ativa
        IF v_mission.status = 'active' THEN
            UPDATE public.missions_progress
            SET current_value = LEAST(p_new_total, v_mission.requirement_value),
                updated_at = now()
            WHERE user_id = p_user_id AND mission_id = v_mission.id;
        
        -- Se missão não tem progresso mas deveria ter
        ELSIF v_mission.status IS NULL THEN
            -- Verificar se é raiz ou pré-requisito completo
            IF v_mission.prerequisite_mission_id IS NULL OR EXISTS (
                SELECT 1 FROM public.missions_progress
                WHERE user_id = p_user_id
                AND mission_id = v_mission.prerequisite_mission_id
                AND status = 'completed'
            ) THEN
                -- Criar progresso com valor atual
                INSERT INTO public.missions_progress (user_id, mission_id, current_value, status)
                VALUES (p_user_id, v_mission.id, LEAST(p_new_total, v_mission.requirement_value), 'active')
                ON CONFLICT (user_id, mission_id) DO UPDATE
                SET current_value = LEAST(p_new_total, v_mission.requirement_value),
                    status = 'active',
                    updated_at = now();
            END IF;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 5.4. `claim_mission_reward_v2(user_id, mission_id)` - Resgatar Recompensa

**Descrição:** Valida se missão está completa, dá recompensas, marca como concluída e ativa a próxima COM progresso global.

```sql
CREATE OR REPLACE FUNCTION claim_mission_reward_v2(p_user_id uuid, p_mission_id int)
RETURNS jsonb AS $$
DECLARE
    v_mission record;
    v_next_mission record;
    v_global_value int;
    v_result jsonb;
BEGIN
    -- Buscar missão e progresso
    SELECT m.*, mp.status, mp.current_value
    INTO v_mission
    FROM public.missions m
    JOIN public.missions_progress mp 
        ON m.id = mp.mission_id AND mp.user_id = p_user_id
    WHERE m.id = p_mission_id;
    
    -- Validações
    IF v_mission IS NULL THEN
        RAISE EXCEPTION 'Mission not found';
    END IF;
    
    IF v_mission.status != 'active' THEN
        RAISE EXCEPTION 'Mission not active';
    END IF;
    
    IF v_mission.current_value < v_mission.requirement_value THEN
        RAISE EXCEPTION 'Mission not completed';
    END IF;
    
    -- Dar recompensas
    PERFORM add_gold(p_user_id, v_mission.gold_reward);
    PERFORM add_xp_and_level_up(p_user_id, v_mission.xp_reward);
    
    -- Marcar como completada
    UPDATE public.missions_progress
    SET status = 'completed',
        completed_at = now(),
        updated_at = now()
    WHERE user_id = p_user_id AND mission_id = p_mission_id;
    
    -- Ativar próxima missão COM PROGRESSO GLOBAL
    SELECT m.* INTO v_next_mission
    FROM public.missions m
    WHERE m.prerequisite_mission_id = p_mission_id;
    
    IF v_next_mission IS NOT NULL THEN
        -- Buscar valor global para este tipo
        v_global_value := COALESCE((
            SELECT 
                CASE v_next_mission.requirement_type
                    WHEN 'collect_pokemon' THEN ugs.total_pokemon_collected
                    WHEN 'favorite_count' THEN ugs.total_favorites
                    WHEN 'create_team' THEN ugs.total_teams
                    WHEN 'shop_buy' THEN ugs.total_shop_purchases
                    -- ... (todos os outros casos)
                    WHEN 'reach_level' THEN (SELECT level FROM public.profiles WHERE id = p_user_id)
                    WHEN 'earn_gold' THEN (SELECT total_gold_earned::int FROM public.profiles WHERE id = p_user_id)
                    ELSE 0
                END
            FROM public.user_global_stats ugs
            WHERE ugs.user_id = p_user_id
        ), 0);
        
        -- Ativar com progresso sincronizado
        INSERT INTO public.missions_progress (user_id, mission_id, current_value, status)
        VALUES (p_user_id, v_next_mission.id, LEAST(v_global_value, v_next_mission.requirement_value), 'active')
        ON CONFLICT (user_id, mission_id) DO UPDATE
        SET status = 'active',
            current_value = LEAST(v_global_value, v_next_mission.requirement_value),
            updated_at = now();
    END IF;
    
    -- Retornar resultado
    RETURN jsonb_build_object(
        'success', true,
        'gold_reward', v_mission.gold_reward,
        'xp_reward', v_mission.xp_reward,
        'next_mission_id', v_next_mission.id
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 5.5. `get_pokemon_generation(pokedex_id)` - Determinar Geração

```sql
CREATE OR REPLACE FUNCTION get_pokemon_generation(p_pokedex_id int)
RETURNS int AS $$
BEGIN
    RETURN CASE
        WHEN p_pokedex_id BETWEEN 1 AND 151 THEN 1
        WHEN p_pokedex_id BETWEEN 152 AND 251 THEN 2
        WHEN p_pokedex_id BETWEEN 252 AND 386 THEN 3
        WHEN p_pokedex_id BETWEEN 387 AND 493 THEN 4
        WHEN p_pokedex_id BETWEEN 494 AND 649 THEN 5
        WHEN p_pokedex_id BETWEEN 650 AND 721 THEN 6
        WHEN p_pokedex_id BETWEEN 722 AND 809 THEN 7
        WHEN p_pokedex_id BETWEEN 810 AND 905 THEN 8
        WHEN p_pokedex_id BETWEEN 906 AND 1025 THEN 9
        ELSE 0
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;
```

---

## 6. Automação (Triggers)

### 6.1. Trigger de Signup

```sql
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();
```

---

### 6.2. Trigger: Pokémon Adicionado (v2.0)

**Função:**
```sql
CREATE OR REPLACE FUNCTION on_pokemon_added_safe()
RETURNS TRIGGER AS $$
DECLARE
    v_gen int;
    v_new_total int;
BEGIN
    -- Bloco 1: Dar XP
    BEGIN
        PERFORM add_xp_and_level_up(NEW.user_id, 10);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    -- Bloco 2: Atualizar stats globais
    BEGIN
        -- Criar ou buscar stats
        INSERT INTO public.user_global_stats (user_id)
        VALUES (NEW.user_id)
        ON CONFLICT (user_id) DO NOTHING;
        
        -- Incrementar contador total
        UPDATE public.user_global_stats
        SET total_pokemon_collected = total_pokemon_collected + 1,
            updated_at = now()
        WHERE user_id = NEW.user_id
        RETURNING total_pokemon_collected INTO v_new_total;
        
        -- Atualizar missões de coleção
        PERFORM update_mission_progress_v2(NEW.user_id, 'collect_pokemon', v_new_total);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    -- Bloco 3: Atualizar geração
    BEGIN
        v_gen := get_pokemon_generation(NEW.pokedex_id);
        
        IF v_gen > 0 THEN
            EXECUTE format(
                'UPDATE public.user_global_stats SET gen_%s = gen_%s + 1, updated_at = now() WHERE user_id = $1 RETURNING gen_%s',
                v_gen, v_gen, v_gen
            ) INTO v_new_total USING NEW.user_id;
            
            PERFORM update_mission_progress_v2(NEW.user_id, 'collect_gen_' || v_gen, v_new_total);
        END IF;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    -- NOTA: Atualização de tipos (collect_type_X) é feita pelo cliente Kotlin
    -- via RPC update_type_progress() porque o pokemon_cache pode não estar populado
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Trigger:**
```sql
CREATE TRIGGER trigger_pokemon_added
    AFTER INSERT ON public.user_pokemons
    FOR EACH ROW
    EXECUTE FUNCTION on_pokemon_added_safe();
```

**Recompensas:**
- +10 XP
- Atualiza missões de coleção geral
- Atualiza missões de geração

---

### 6.3. Trigger: Favorito Adicionado

```sql
CREATE OR REPLACE FUNCTION on_favorite_added_safe()
RETURNS TRIGGER AS $$
DECLARE
    v_new_total int;
BEGIN
    BEGIN
        -- Atualizar stats
        INSERT INTO public.user_global_stats (user_id)
        VALUES (NEW.user_id)
        ON CONFLICT (user_id) DO NOTHING;
        
        UPDATE public.user_global_stats
        SET total_favorites = total_favorites + 1,
            updated_at = now()
        WHERE user_id = NEW.user_id
        RETURNING total_favorites INTO v_new_total;
        
        -- Atualizar missões
        PERFORM update_mission_progress_v2(NEW.user_id, 'favorite_count', v_new_total);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_favorite_added
    AFTER INSERT ON public.favorites
    FOR EACH ROW
    EXECUTE FUNCTION on_favorite_added_safe();
```

---

### 6.4. Trigger: Equipa Criada

```sql
CREATE OR REPLACE FUNCTION on_team_created_safe()
RETURNS TRIGGER AS $$
DECLARE
    v_new_total int;
BEGIN
    BEGIN
        INSERT INTO public.user_global_stats (user_id)
        VALUES (NEW.user_id)
        ON CONFLICT (user_id) DO NOTHING;
        
        UPDATE public.user_global_stats
        SET total_teams = total_teams + 1,
            updated_at = now()
        WHERE user_id = NEW.user_id
        RETURNING total_teams INTO v_new_total;
        
        PERFORM update_mission_progress_v2(NEW.user_id, 'create_team', v_new_total);
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_team_created
    AFTER INSERT ON public.teams
    FOR EACH ROW
    EXECUTE FUNCTION on_team_created_safe();
```

---

### 6.5. Trigger: Item Comprado

```sql
CREATE OR REPLACE FUNCTION on_item_purchased_safe()
RETURNS TRIGGER AS $$
DECLARE
    v_new_total int;
BEGIN
    BEGIN
        -- Stats globais
        INSERT INTO public.user_global_stats (user_id)
        VALUES (NEW.user_id)
        ON CONFLICT (user_id) DO NOTHING;
        
        -- Incrementar compras totais
        UPDATE public.user_global_stats
        SET total_shop_purchases = total_shop_purchases + 1,
            updated_at = now()
        WHERE user_id = NEW.user_id
        RETURNING total_shop_purchases INTO v_new_total;
        
        PERFORM update_mission_progress_v2(NEW.user_id, 'shop_buy', v_new_total);
        
        -- Incrementar por categoria
        IF NEW.category = 'skin' THEN
            UPDATE public.user_global_stats
            SET total_skins = total_skins + 1, updated_at = now()
            WHERE user_id = NEW.user_id
            RETURNING total_skins INTO v_new_total;
            PERFORM update_mission_progress_v2(NEW.user_id, 'own_skins', v_new_total);
            
        ELSIF NEW.category = 'badge' THEN
            UPDATE public.user_global_stats
            SET total_badges = total_badges + 1, updated_at = now()
            WHERE user_id = NEW.user_id
            RETURNING total_badges INTO v_new_total;
            PERFORM update_mission_progress_v2(NEW.user_id, 'own_badges', v_new_total);
            
        ELSIF NEW.category = 'theme' THEN
            UPDATE public.user_global_stats
            SET total_themes = total_themes + 1, updated_at = now()
            WHERE user_id = NEW.user_id
            RETURNING total_themes INTO v_new_total;
            PERFORM update_mission_progress_v2(NEW.user_id, 'own_themes', v_new_total);
            
        ELSIF NEW.category = 'name_color' THEN
            UPDATE public.user_global_stats
            SET total_name_colors = total_name_colors + 1, updated_at = now()
            WHERE user_id = NEW.user_id
            RETURNING total_name_colors INTO v_new_total;
            PERFORM update_mission_progress_v2(NEW.user_id, 'own_name_colors', v_new_total);
        END IF;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_item_purchased
    AFTER INSERT ON public.inventory
    FOR EACH ROW
    EXECUTE FUNCTION on_item_purchased_safe();
```

---

### 6.6. Trigger: Nível Mudou

```sql
CREATE OR REPLACE FUNCTION on_level_changed()
RETURNS TRIGGER AS $$
DECLARE
    v_mission record;
BEGIN
    IF NEW.level > OLD.level THEN
        FOR v_mission IN
            SELECT m.*, mp.status
            FROM public.missions m
            LEFT JOIN public.missions_progress mp 
                ON m.id = mp.mission_id AND mp.user_id = NEW.id
            WHERE m.requirement_type = 'reach_level'
            ORDER BY m.requirement_value ASC
        LOOP
            IF v_mission.status = 'active' OR v_mission.status IS NULL 
               OR v_mission.prerequisite_mission_id IS NULL THEN
                INSERT INTO public.missions_progress (user_id, mission_id, current_value, status)
                VALUES (NEW.id, v_mission.id, NEW.level, COALESCE(v_mission.status, 'active'))
                ON CONFLICT (user_id, mission_id) DO UPDATE
                SET current_value = NEW.level, updated_at = now();
            END IF;
        END LOOP;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_level_changed
    AFTER UPDATE OF level ON public.profiles
    FOR EACH ROW
    WHEN (NEW.level > OLD.level)
    EXECUTE FUNCTION on_level_changed();
```

---

### 6.7. Trigger: Gold Ganho

```sql
CREATE OR REPLACE FUNCTION on_gold_earned()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.total_gold_earned > OLD.total_gold_earned THEN
        PERFORM update_mission_progress_v2(NEW.id, 'earn_gold', NEW.total_gold_earned::int);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_gold_earned
    AFTER UPDATE OF total_gold_earned ON public.profiles
    FOR EACH ROW
    WHEN (NEW.total_gold_earned > OLD.total_gold_earned)
    EXECUTE FUNCTION on_gold_earned();
```

---

### 6.8. Trigger: Atualizar `updated_at`

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

---

## 7. RPCs (Funções Cliente)

### 7.1. `get_user_stats(user_id)` - Estatísticas do Utilizador

```sql
CREATE OR REPLACE FUNCTION get_user_stats(p_user_id uuid)
RETURNS jsonb AS $$
DECLARE
    v_result jsonb;
BEGIN
    SELECT jsonb_build_object(
        'total_pokemon', (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id),
        'total_favorites', (SELECT COUNT(*) FROM public.favorites WHERE user_id = p_user_id),
        'total_teams', (SELECT COUNT(*) FROM public.teams WHERE user_id = p_user_id),
        'completion_percentage', (
            (SELECT COUNT(*)::float FROM public.user_pokemons WHERE user_id = p_user_id) / 1025.0 * 100
        )
    ) INTO v_result;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.2. `add_gold(user_id, amount)` - Adicionar Gold

```sql
CREATE OR REPLACE FUNCTION add_gold(p_user_id uuid, p_amount int)
RETURNS void AS $$
BEGIN
    UPDATE public.profiles
    SET gold = gold + p_amount,
        total_gold_earned = total_gold_earned + p_amount
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.3. `spend_gold(user_id, amount)` - Gastar Gold

```sql
CREATE OR REPLACE FUNCTION spend_gold(p_user_id uuid, p_amount int)
RETURNS boolean AS $$
DECLARE
    v_current_gold bigint;
BEGIN
    SELECT gold INTO v_current_gold
    FROM public.profiles
    WHERE id = p_user_id;
    
    IF v_current_gold >= p_amount THEN
        UPDATE public.profiles
        SET gold = gold - p_amount
        WHERE id = p_user_id;
        
        RETURN true;
    ELSE
        RETURN false;  -- Saldo insuficiente
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.4. `increment_pet_clicks(user_id)` - Cliques no Pet

```sql
CREATE OR REPLACE FUNCTION increment_pet_clicks(p_user_id uuid)
RETURNS void AS $$
BEGIN
    UPDATE public.profiles
    SET pet_clicks = pet_clicks + 1
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.5. `end_session(user_id, session_id, duration)` - Finalizar Sessão

```sql
CREATE OR REPLACE FUNCTION end_session(
    p_user_id uuid,
    p_session_id uuid,
    p_duration_seconds int
)
RETURNS void AS $$
BEGIN
    -- Atualizar sessão
    UPDATE public.user_sessions
    SET ended_at = now(),
        duration_seconds = p_duration_seconds
    WHERE id = p_session_id;
    
    -- Atualizar tempo total
    UPDATE public.profiles
    SET total_time_seconds = total_time_seconds + p_duration_seconds
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.6. `sync_user_missions(user_id)` - Sincronizar Missões

**Descrição:** Permite sincronização manual de missões pelo cliente.

```sql
CREATE OR REPLACE FUNCTION sync_user_missions(p_user_id uuid)
RETURNS void AS $$
BEGIN
    IF auth.uid() != p_user_id THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;
    
    PERFORM initialize_global_stats_from_existing(p_user_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### 7.7. `update_type_progress(user_id, types[], pokemon_id)` - Atualizar Progresso de Tipo

**Descrição:** Chamado pelo cliente Kotlin quando adiciona um Pokémon e conhece os tipos.

```sql
CREATE OR REPLACE FUNCTION update_type_progress(
    p_user_id uuid,
    p_pokemon_types text[],
    p_pokemon_id int DEFAULT NULL  -- NOVO: opcional para verificar geração
)
RETURNS void AS $$
DECLARE
    v_type text;
    v_new_total int;
    v_column_name text;
    v_gen int;
BEGIN
    IF auth.uid() != p_user_id THEN
        RAISE EXCEPTION 'Unauthorized';
    END IF;
    
    -- Garantir stats globais
    INSERT INTO public.user_global_stats (user_id)
    VALUES (p_user_id)
    ON CONFLICT (user_id) DO NOTHING;
    
    -- Para cada tipo
    FOREACH v_type IN ARRAY p_pokemon_types
    LOOP
        v_column_name := 'type_' || lower(v_type);
        
        BEGIN
            -- Incrementar contador
            EXECUTE format(
                'UPDATE public.user_global_stats SET %I = %I + 1 WHERE user_id = $1 RETURNING %I',
                v_column_name, v_column_name, v_column_name
            ) INTO v_new_total USING p_user_id;
            
            -- Atualizar missões
            PERFORM update_mission_progress_v2(p_user_id, 'collect_type_' || lower(v_type), v_new_total);
        EXCEPTION WHEN OTHERS THEN
            RAISE LOG 'update_type_progress ERROR for type %: %', v_type, SQLERRM;
        END;
    END LOOP;
    
    -- Se pokemon_id fornecido, verificar geração (já tratado pelo trigger)
    IF p_pokemon_id IS NOT NULL THEN
        v_gen := get_pokemon_generation(p_pokemon_id);
        -- Nota: geração já é atualizada pelo trigger on_pokemon_added()
        -- Este parâmetro é opcional para validação
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Parâmetros:**
- `p_user_id`: UUID do utilizador
- `p_pokemon_types`: Array de tipos (ex: `['fire', 'flying']`)
- `p_pokemon_id`: *(Opcional)* Pokedex ID para validação de geração

**Uso no Cliente Kotlin:**
```kotlin
supabase.rpc("update_type_progress", mapOf(
    "p_user_id" to userId,
    "p_pokemon_types" to arrayOf("fire", "flying"),
    "p_pokemon_id" to 6  // Opcional: Charizard
))
```

---

### 7.9. `sync_user_stats_and_missions(user_id)` - Sincronizar Stats e Missões

**Descrição:** Sincroniza completamente `user_global_stats` e todas as missões com dados reais. Útil para migração de utilizadores existentes ou correção de inconsistências.

```sql
CREATE OR REPLACE FUNCTION sync_user_stats_and_missions(p_user_id uuid)
RETURNS void AS $$
DECLARE
    v_pokemon_count int;
    v_favorite_count int;
    v_team_count int;
    v_inventory_count int;
    v_skins_count int;
    v_badges_count int;
    v_themes_count int;
    v_colors_count int;
    v_titles_count int;
    v_i int;
BEGIN
    -- Contar todos os dados reais
    SELECT COUNT(*) INTO v_pokemon_count FROM public.user_pokemons WHERE user_id = p_user_id;
    SELECT COUNT(*) INTO v_favorite_count FROM public.favorites WHERE user_id = p_user_id;
    SELECT COUNT(*) INTO v_team_count FROM public.teams WHERE user_id = p_user_id;
    SELECT COUNT(*) INTO v_inventory_count FROM public.inventory WHERE user_id = p_user_id;
    SELECT COUNT(*) INTO v_skins_count FROM public.inventory WHERE user_id = p_user_id AND category = 'skin';
    SELECT COUNT(*) INTO v_badges_count FROM public.inventory WHERE user_id = p_user_id AND category = 'badge';
    SELECT COUNT(*) INTO v_themes_count FROM public.inventory WHERE user_id = p_user_id AND category = 'theme';
    SELECT COUNT(*) INTO v_colors_count FROM public.inventory WHERE user_id = p_user_id AND category = 'name_color';
    SELECT COUNT(*) INTO v_titles_count FROM public.inventory WHERE user_id = p_user_id AND category = 'title';
    
    -- Criar ou atualizar user_global_stats
    INSERT INTO public.user_global_stats (
        user_id, 
        total_pokemon_collected, 
        total_favorites, 
        total_teams,
        total_shop_purchases,
        total_skins,
        total_badges,
        total_themes,
        total_name_colors,
        total_titles
    ) VALUES (
        p_user_id,
        v_pokemon_count,
        v_favorite_count,
        v_team_count,
        v_inventory_count,
        v_skins_count,
        v_badges_count,
        v_themes_count,
        v_colors_count,
        v_titles_count
    )
    ON CONFLICT (user_id) DO UPDATE SET
        total_pokemon_collected = v_pokemon_count,
        total_favorites = v_favorite_count,
        total_teams = v_team_count,
        total_shop_purchases = v_inventory_count,
        total_skins = v_skins_count,
        total_badges = v_badges_count,
        total_themes = v_themes_count,
        total_name_colors = v_colors_count,
        total_titles = v_titles_count,
        updated_at = now();
    
    -- Contar por geração
    FOR v_i IN 1..9 LOOP
        EXECUTE format(
            'UPDATE public.user_global_stats SET gen_%s = (
                SELECT COUNT(*) FROM public.user_pokemons 
                WHERE user_id = $1 AND get_pokemon_generation(pokedex_id) = %s
            ) WHERE user_id = $1',
            v_i, v_i
        ) USING p_user_id;
    END LOOP;
    
    -- Sincronizar TODAS as missões com valores globais
    PERFORM update_mission_progress_v2(p_user_id, 'collect_pokemon', v_pokemon_count);
    PERFORM update_mission_progress_v2(p_user_id, 'favorite_count', v_favorite_count);
    PERFORM update_mission_progress_v2(p_user_id, 'create_team', v_team_count);
    PERFORM update_mission_progress_v2(p_user_id, 'shop_buy', v_inventory_count);
    PERFORM update_mission_progress_v2(p_user_id, 'own_skins', v_skins_count);
    PERFORM update_mission_progress_v2(p_user_id, 'own_badges', v_badges_count);
    PERFORM update_mission_progress_v2(p_user_id, 'own_themes', v_themes_count);
    PERFORM update_mission_progress_v2(p_user_id, 'own_name_colors', v_colors_count);
    PERFORM update_mission_progress_v2(p_user_id, 'own_titles', v_titles_count);
    
    -- Sincronizar gerações
    FOR v_i IN 1..9 LOOP
        EXECUTE format(
            'SELECT gen_%s FROM public.user_global_stats WHERE user_id = $1',
            v_i
        ) INTO v_pokemon_count USING p_user_id;
        
        PERFORM update_mission_progress_v2(p_user_id, 'collect_gen_' || v_i, v_pokemon_count);
    END LOOP;
    
    -- Sincronizar nível
    SELECT level INTO v_pokemon_count FROM public.profiles WHERE id = p_user_id;
    IF v_pokemon_count IS NOT NULL THEN
        PERFORM update_mission_progress_v2(p_user_id, 'reach_level', v_pokemon_count);
    END IF;
    
    -- Sincronizar gold
    SELECT total_gold_earned INTO v_pokemon_count FROM public.profiles WHERE id = p_user_id;
    IF v_pokemon_count IS NOT NULL THEN
        PERFORM update_mission_progress_v2(p_user_id, 'earn_gold', v_pokemon_count);
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

**Uso:**
```sql
-- Sincronizar um utilizador específico
SELECT sync_user_stats_and_missions('user-uuid-here'::uuid);

-- Sincronizar TODOS os utilizadores (script de migração)
DO $$
DECLARE
    v_user record;
BEGIN
    FOR v_user IN SELECT id FROM public.profiles LOOP
        PERFORM sync_user_stats_and_missions(v_user.id);
    END LOOP;
END $$;
```

---

### 7.8. `initialize_global_stats_from_existing(user_id)` - Inicializar Stats

**Descrição:** Sincroniza `user_global_stats` com dados existentes. Útil para migrar utilizadores antigos.

```sql
CREATE OR REPLACE FUNCTION initialize_global_stats_from_existing(p_user_id uuid)
RETURNS void AS $$
DECLARE
    v_pokemon record;
    v_type text;
BEGIN
    -- Criar registo
    INSERT INTO public.user_global_stats (user_id)
    VALUES (p_user_id)
    ON CONFLICT (user_id) DO NOTHING;
    
    -- Contar Pokémon totais
    UPDATE public.user_global_stats
    SET total_pokemon_collected = (
        SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id
    )
    WHERE user_id = p_user_id;
    
    -- Contar favoritos
    UPDATE public.user_global_stats
    SET total_favorites = (
        SELECT COUNT(*) FROM public.favorites WHERE user_id = p_user_id
    )
    WHERE user_id = p_user_id;
    
    -- Contar equipas
    UPDATE public.user_global_stats
    SET total_teams = (
        SELECT COUNT(*) FROM public.teams WHERE user_id = p_user_id
    )
    WHERE user_id = p_user_id;
    
    -- Contar itens por categoria
    UPDATE public.user_global_stats
    SET 
        total_shop_purchases = (SELECT COUNT(*) FROM public.inventory WHERE user_id = p_user_id),
        total_skins = (SELECT COUNT(*) FROM public.inventory WHERE user_id = p_user_id AND category = 'skin'),
        total_badges = (SELECT COUNT(*) FROM public.inventory WHERE user_id = p_user_id AND category = 'badge'),
        total_themes = (SELECT COUNT(*) FROM public.inventory WHERE user_id = p_user_id AND category = 'theme'),
        total_name_colors = (SELECT COUNT(*) FROM public.inventory WHERE user_id = p_user_id AND category = 'name_color')
    WHERE user_id = p_user_id;
    
    -- Contar por geração
    UPDATE public.user_global_stats
    SET 
        gen_1 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 1 AND 151),
        gen_2 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 152 AND 251),
        gen_3 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 252 AND 386),
        gen_4 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 387 AND 493),
        gen_5 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 494 AND 649),
        gen_6 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 650 AND 721),
        gen_7 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 722 AND 809),
        gen_8 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 810 AND 905),
        gen_9 = (SELECT COUNT(*) FROM public.user_pokemons WHERE user_id = p_user_id AND pokedex_id BETWEEN 906 AND 1025)
    WHERE user_id = p_user_id;
    
    -- Contar por tipo (usando cache)
    UPDATE public.user_global_stats
    SET 
        type_normal = 0, type_fire = 0, type_water = 0, type_grass = 0,
        type_electric = 0, type_ice = 0, type_fighting = 0, type_poison = 0,
        type_ground = 0, type_flying = 0, type_psychic = 0, type_bug = 0,
        type_rock = 0, type_ghost = 0, type_dragon = 0, type_dark = 0,
        type_steel = 0, type_fairy = 0
    WHERE user_id = p_user_id;
    
    FOR v_pokemon IN
        SELECT up.pokedex_id, pc.types
        FROM public.user_pokemons up
        LEFT JOIN public.pokemon_cache pc ON up.pokedex_id = pc.pokedex_id
        WHERE up.user_id = p_user_id AND pc.types IS NOT NULL
    LOOP
        FOR v_type IN SELECT jsonb_array_elements_text(v_pokemon.types)
        LOOP
            v_type := lower(v_type);
            EXECUTE format(
                'UPDATE public.user_global_stats SET type_%s = type_%s + 1 WHERE user_id = $1',
                v_type, v_type
            ) USING p_user_id;
        END LOOP;
    END LOOP;
    
    UPDATE public.user_global_stats SET updated_at = now() WHERE user_id = p_user_id;
    
    -- Sincronizar missões
    PERFORM sync_mission_progress_with_global(p_user_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

## 8. Performance e Segurança

### 8.1. Índices Criados

**Profiles:**
```sql
CREATE INDEX idx_profiles_level ON public.profiles(level);
CREATE INDEX idx_profiles_username ON public.profiles(username);
```

**Shop Items:**
```sql
CREATE INDEX idx_shop_items_type ON public.shop_items(type);
CREATE INDEX idx_shop_items_rarity ON public.shop_items(rarity);
```

**Missions:**
```sql
CREATE INDEX idx_missions_category ON public.missions(category);
CREATE INDEX idx_missions_prerequisite ON public.missions(prerequisite_mission_id);
```

**Missions Progress:**
```sql
CREATE INDEX idx_missions_progress_user ON public.missions_progress(user_id);
CREATE INDEX idx_missions_progress_status ON public.missions_progress(status);
```

**Inventory:**
```sql
CREATE INDEX idx_inventory_user ON public.inventory(user_id);
CREATE INDEX idx_inventory_category ON public.inventory(category);
```

**User Pokemons:**
```sql
CREATE INDEX idx_user_pokemons_user ON public.user_pokemons(user_id);
CREATE INDEX idx_user_pokemons_pokedex ON public.user_pokemons(pokedex_id);
```

**Favorites:**
```sql
CREATE INDEX idx_favorites_user ON public.favorites(user_id);
CREATE INDEX idx_favorites_created ON public.favorites(created_at DESC);
```

**Teams:**
```sql
CREATE INDEX idx_teams_user ON public.teams(user_id);
```

**Team Slots:**
```sql
CREATE INDEX idx_team_slots_team ON public.team_slots(team_id);
```

**User Sessions:**
```sql
CREATE INDEX idx_user_sessions_user ON public.user_sessions(user_id);
```

**User Global Stats:**
```sql
CREATE INDEX idx_user_global_stats_user ON public.user_global_stats(user_id);
```

---

### 8.2. Row Level Security (RLS)

**Princípio:** Todas as tabelas têm RLS ativo. Apenas o dono pode ver/editar os seus dados.

```sql
-- Ativar RLS em todas as tabelas
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.inventory ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_pokemons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.team_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.missions_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_global_stats ENABLE ROW LEVEL SECURITY;
```

**Políticas Públicas (Leitura):**
```sql
-- Qualquer utilizador autenticado pode ler configurações
CREATE POLICY "Leitura pública" ON public.shop_items FOR SELECT USING (true);
CREATE POLICY "Leitura pública" ON public.missions FOR SELECT USING (true);
CREATE POLICY "Leitura pública" ON public.titles FOR SELECT USING (true);
CREATE POLICY "Leitura pública" ON public.pokemon_cache FOR SELECT USING (true);
CREATE POLICY "Leitura pública" ON public.profiles FOR SELECT USING (true);
```

**Políticas Privadas (Gestão):**
```sql
-- Apenas o dono pode gerir os seus dados
CREATE POLICY "Dono gere inventory" ON public.inventory
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere pokemons" ON public.user_pokemons
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere favoritos" ON public.favorites
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere equipas" ON public.teams
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere team_slots" ON public.team_slots
    FOR ALL USING (auth.uid() = (SELECT user_id FROM public.teams WHERE id = team_id));

CREATE POLICY "Dono gere progresso" ON public.missions_progress
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere sessões" ON public.user_sessions
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere stats" ON public.user_global_stats
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Dono gere perfil" ON public.profiles
    FOR ALL USING (auth.uid() = id);
```

---

## 9. Histórico de Correções e Migrações

### 9.1. Migration 1: Starter nos Favoritos (Inicial)

**Data:** Dezembro 2024  
**Problema:** Starter não aparecia favoritado na Pokédex desde o início  
**Solução:** Adicionar starter à tabela `favorites` no signup

```sql
-- Atualização do handle_new_user para adicionar starter aos favoritos
INSERT INTO public.favorites (user_id, pokedex_id)
VALUES (NEW.id, v_starter_id);
```

---

### 9.2. Migration 2: Sistema de Missões v2.0 (Completo)

**Data:** Janeiro 2025  
**Problemas:**
1. Progresso resetava ao ativar nova missão
2. Missões não trackeavam progresso antes de serem ativadas
3. Faltavam triggers para alguns tipos de missões

**Soluções:**
1. Criada tabela `user_global_stats` para tracking global
2. Nova função `update_mission_progress_v2()` que atualiza todas as missões (ativas e futuras)
3. Função `claim_mission_reward_v2()` ativa próxima missão com progresso sincronizado
4. Triggers criados para TODOS os tipos de missões

**Arquivos do Script:**
- `bddados.txt` - Script completo do sistema de missões v2.0

---

### 9.3. Migration 3: Correção de Signup (Emergência)

**Data:** Janeiro 2026  
**Problema:** Signup falhava devido a dependências do sistema de missões v2.0  
**Solução:** Função `handle_new_user()` totalmente reescrita com blocos TRY-CATCH independentes

**Características:**
- ✅ Cada operação num bloco TRY separado
- ✅ NUNCA falha o signup - sempre retorna NEW
- ✅ Logs detalhados de cada erro
- ✅ Fallbacks para todos os campos opcionais

---

### 9.4. Migration 4: Triggers Fail-Safe (Estabilização)

**Data:** Janeiro 2026  
**Problema:** Triggers podiam falhar e bloquear operações normais  
**Solução:** Todos os triggers reescritos com blocos TRY-CATCH

**Triggers Atualizados:**
- `on_pokemon_added_safe()`
- `on_favorite_added_safe()`
- `on_team_created_safe()`
- `on_item_purchased_safe()`

---

### 9.5. Migration 5: Sistema de Missões 100% Funcional (Final)

**Data:** Janeiro 2026  
**Arquivo:** Script completo de reconstrução do sistema de missões  
**Status:** ✅ **VERSÃO FINAL ESTÁVEL**

**Problemas Resolvidos:**
1. ❌ Sistema de missões não atualizava progresso corretamente
2. ❌ Missões de tipo (fire, water, etc.) não funcionavam
3. ❌ Missões de geração perdiam sincronização
4. ❌ Claim de reward não ativava próxima missão com progresso correto
5. ❌ Faltava coluna `total_titles` em `user_global_stats`

**Soluções Implementadas:**

**1. Tabela `user_global_stats` Completa:**
```sql
-- Adicionada coluna total_titles
total_titles int DEFAULT 0
```

**2. Função `update_mission_progress_v2()` Otimizada:**
- Atualiza apenas missões **ativas** (não cria progresso para missões futuras)
- Usa valor **total absoluto** em vez de incremento
- Performance melhorada

**3. RPC `update_type_progress()` Melhorado:**
```sql
CREATE OR REPLACE FUNCTION update_type_progress(
    p_user_id uuid,
    p_pokemon_types text[],
    p_pokemon_id int DEFAULT NULL  -- NOVO: permite atualizar geração também
)
```
- Aceita array de tipos: `['fire', 'flying']`
- Atualiza contadores em `user_global_stats`
- Atualiza missões `collect_type_X` automaticamente
- Parâmetro opcional para pokemon_id (geração já é tratada pelo trigger)

**4. RPC `sync_user_stats_and_missions()` Completo:**
- Sincroniza **TODOS** os contadores de `user_global_stats` com dados reais
- Sincroniza **TODAS** as missões com progresso global
- Útil para:
  - Migrar utilizadores existentes
  - Corrigir inconsistências
  - Reset após bugs

**5. Função `claim_mission_reward_v2()` Corrigida:**
- Busca progresso global de **TODAS** as categorias:
  - Coleção (pokemon, favoritos, equipas)
  - Tipos (18 tipos Pokémon)
  - Gerações (Gen 1-9)
  - Loja (geral + por categoria)
  - Nível e Gold
- Ativa próxima missão com progresso **já sincronizado**
- Retorna JSON com informações da recompensa

**6. Triggers Simplificados e Estáveis:**

```sql
-- Pokémon Adicionado
on_pokemon_added()
  ├─ Incrementa total_pokemon_collected
  ├─ Incrementa gen_X (baseado no pokedex_id)
  ├─ Atualiza missões collect_pokemon
  ├─ Atualiza missões collect_gen_X
  └─ Dá +10 XP

-- Favorito Adicionado
on_favorite_added()
  ├─ Incrementa total_favorites
  └─ Atualiza missões favorite_count

-- Equipa Criada
on_team_created()
  ├─ Incrementa total_teams
  └─ Atualiza missões create_team

-- Item Comprado
on_item_purchased()
  ├─ Incrementa total_shop_purchases
  ├─ Incrementa total_[categoria] (skins, badges, etc.)
  ├─ Atualiza missões shop_buy
  └─ Atualiza missões own_[categoria]

-- Nível Mudou
on_level_changed()
  └─ Atualiza missões reach_level

-- Gold Ganho
on_gold_earned()
  └─ Atualiza missões earn_gold
```

**7. Script de Migração Automática:**
```sql
-- Sincroniza TODOS os utilizadores existentes automaticamente
DO $$
DECLARE
    v_user record;
BEGIN
    FOR v_user IN SELECT id FROM public.profiles LOOP
        PERFORM sync_user_stats_and_missions(v_user.id);
    END LOOP;
END $$;
```

**Fluxo Completo do Sistema de Missões v2.0:**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. UTILIZADOR FAZ AÇÃO (ex: captura Pokémon)               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. TRIGGER on_pokemon_added() EXECUTA                       │
│    ├─ INSERT em user_pokemons                               │
│    ├─ UPDATE user_global_stats (total_pokemon_collected++)  │
│    ├─ UPDATE user_global_stats (gen_X++)                    │
│    └─ PERFORM update_mission_progress_v2()                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. CLIENTE KOTLIN CHAMA update_type_progress()              │
│    (com tipos do Pokémon: ['fire', 'flying'])               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. update_type_progress() EXECUTA                           │
│    ├─ UPDATE user_global_stats (type_fire++)                │
│    ├─ UPDATE user_global_stats (type_flying++)              │
│    ├─ PERFORM update_mission_progress_v2('collect_type_fire')│
│    └─ PERFORM update_mission_progress_v2('collect_type_flying')│
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. update_mission_progress_v2() ATUALIZA MISSÕES           │
│    └─ UPDATE missions_progress SET current_value = [total]  │
│       (apenas para missões ATIVAS)                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. MISSÃO FICA PRONTA (current_value >= requirement_value)  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. UTILIZADOR CLICA "CLAIM REWARD"                          │
│    └─ Cliente chama claim_mission_reward_v2()               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. claim_mission_reward_v2() EXECUTA                        │
│    ├─ Valida missão (ativa? completa?)                      │
│    ├─ UPDATE missions_progress SET status='completed'       │
│    ├─ UPDATE profiles (dá gold + xp)                        │
│    ├─ PERFORM add_xp_and_level_up()                         │
│    ├─ Busca próxima missão (prerequisite_mission_id)        │
│    ├─ Busca progresso global para o requirement_type        │
│    └─ INSERT próxima missão com current_value = [global]    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. PRÓXIMA MISSÃO ATIVADA JÁ COM PROGRESSO CORRETO         │
│    (se user tem 30 pokémon e próxima missão pede 25,        │
│     ela já aparece 30/25 = completa!)                       │
└─────────────────────────────────────────────────────────────┘
```

**Características Principais:**

✅ **Progresso Nunca Se Perde:**
- Mesmo que uma missão não esteja ativa, o progresso continua sendo trackeado em `user_global_stats`
- Quando a missão é ativada, já mostra o progresso real

✅ **Sincronização Perfeita:**
- `user_global_stats` é a fonte única de verdade
- Todas as missões consultam esta tabela
- Impossível ter progresso inconsistente

✅ **Performance Otimizada:**
- Triggers apenas atualizam contadores (operações simples)
- `update_mission_progress_v2()` só atualiza missões ativas
- Sem queries pesadas ou loops desnecessários

✅ **Fail-Safe Total:**
- Todos os blocos críticos têm TRY-CATCH
- Erros nunca bloqueiam operações do utilizador
- Logging detalhado para debug

✅ **Fácil de Debugar:**
```sql
-- Ver stats globais
SELECT * FROM user_global_stats WHERE user_id = 'xxx';

-- Ver progresso de missões
SELECT m.title, mp.status, mp.current_value, m.requirement_value
FROM missions_progress mp
JOIN missions m ON m.id = mp.mission_id
WHERE mp.user_id = 'xxx' AND mp.status = 'active';

-- Sincronizar manualmente
SELECT sync_user_stats_and_missions('xxx'::uuid);
```

**Arquivo do Script:**
- `UMBRADEX - SISTEMA DE MISSÕES 100% FUNCIONAL.sql`
- Pode ser executado múltiplas vezes (é idempotente)
- Sincroniza automaticamente todos os utilizadores existentes

---

## 10. Diagrama de Relacionamentos

```
auth.users (Supabase Auth)
    ↓
    ├─→ profiles (1:1)
    │      ↓
    │      ├─→ user_global_stats (1:1)
    │      ├─→ inventory (1:N)
    │      ├─→ user_pokemons (1:N)
    │      ├─→ favorites (1:N)
    │      ├─→ teams (1:N)
    │      │      ↓
    │      │      └─→ team_slots (1:6)
    │      ├─→ missions_progress (1:N)
    │      └─→ user_sessions (1:N)
    │
    ├─→ missions (Static Config)
    │      ↓
    │      └─→ missions_progress (1:N)
    │
    ├─→ shop_items (Static Catalog)
    │
    ├─→ titles (Static Config)
    │
    └─→ pokemon_cache (API Cache)
```

---

## 11. Notas de Integração Cliente (Kotlin)

### 11.1. Fluxo de Signup

```kotlin
// 1. Recolher dados do utilizador
val signupData = mapOf(
    "username" to username,
    "starter_id" to starterId,  // 1-1025
    "birth_date" to birthDate,  // "YYYY-MM-DD"
    "pokemon_knowledge" to knowledge,  // "beginner" | "intermediate" | "expert"
    "favorite_type" to favoriteType,  // "fire", "water", etc.
    "avatar" to selectedAvatar  // "standard_male1", etc.
)

// 2. Criar utilizador no Supabase Auth
val response = supabase.auth.signUp {
    email = email
    password = password
    data = signupData  // Metadata
}

// 3. Trigger handle_new_user() executa automaticamente
// 4. Utilizador está pronto para usar a app
```

### 11.2. Fluxo de Captura de Pokémon

```kotlin
// 1. Adicionar à Living Dex
supabase.from("user_pokemons").insert(
    mapOf(
        "user_id" to userId,
        "pokedex_id" to pokemonId
    )
)

// 2. Trigger on_pokemon_added_safe() executa:
//    - Dá +10 XP
//    - Atualiza user_global_stats.total_pokemon_collected
//    - Atualiza user_global_stats.gen_X
//    - Atualiza missões de coleção

// 3. Cliente chama RPC para atualizar tipos (importante!)
supabase.rpc("update_type_progress", mapOf(
    "p_user_id" to userId,
    "p_pokemon_types" to arrayOf("fire", "flying")
))

// 4. Atualiza missões de tipo
```

### 11.3. Fluxo de Resgate de Missão

```kotlin
// 1. Verificar se missão está completa (current_value >= requirement_value)
val mission = supabase.from("missions_progress")
    .select()
    .eq("user_id", userId)
    .eq("mission_id", missionId)
    .eq("status", "active")
    .single()

if (mission.current_value >= mission.requirement_value) {
    // 2. Chamar RPC para resgatar
    val result = supabase.rpc("claim_mission_reward_v2", mapOf(
        "p_user_id" to userId,
        "p_mission_id" to missionId
    ))
    
    // 3. Resultado contém:
    // {
    //   "success": true,
    //   "gold_reward": 100,
    //   "xp_reward": 50,
    //   "next_mission_id": 42
    // }
    
    // 4. UI mostra animação de recompensa
    // 5. Próxima missão já está ativa com progresso sincronizado
}
```

---

## 12. Resumo Executivo

### Estado Atual
✅ **Sistema 100% Funcional e Testado em Produção**
- ✅ Signup estável com fallbacks múltiplos
- ✅ Sistema de missões v2.0 com progresso contínuo VERIFICADO
- ✅ Todos os triggers fail-safe e testados
- ✅ RLS configurado e seguro
- ✅ Performance otimizada com índices estratégicos
- ✅ Script de sincronização completo para utilizadores existentes

### Principais Componentes
1. **Sistema de Autenticação:** Supabase Auth + RLS robusto
2. **Sistema de Progressão:** XP/Níveis/Títulos com fórmula balanceada
3. **Sistema de Economia:** Gold + Loja + Recompensas
4. **Sistema de Missões v2.0:** 
   - ✅ Tracking global via `user_global_stats`
   - ✅ Progresso contínuo (nunca reseta)
   - ✅ Suporte para TODOS os tipos de missões
   - ✅ Sincronização perfeita entre claim de recompensas
5. **Living Dex:** Coleção completa com tracking por tipo e geração
6. **Equipas:** Construção e gestão de equipas de 6 Pokémon
7. **Personalização:** Skins, Temas, Badges, Cores, Títulos completo

### Estatísticas do Sistema
- **Tabelas:** 13 principais + 1 auxiliar (user_global_stats)
- **Funções Stored:** 18+ (incluindo helpers)
- **Triggers:** 8 principais (todos fail-safe)
- **RPCs Públicos:** 10+ (incluindo sync)
- **Índices:** 22+ estratégicos
- **Políticas RLS:** 15+ granulares

### Tipos de Missões Suportados
✅ **Coleção:**
- `collect_pokemon` - Capturar X Pokémon (geral)
- `collect_gen_X` - Capturar X Pokémon da geração X (9 gerações)
- `collect_type_X` - Capturar X Pokémon do tipo X (18 tipos)

✅ **Social:**
- `favorite_count` - Ter X Pokémon favoritos
- `create_team` - Criar X equipas

✅ **Loja:**
- `shop_buy` - Comprar X itens (geral)
- `own_skins` - Possuir X skins
- `own_badges` - Possuir X badges
- `own_themes` - Possuir X temas
- `own_name_colors` - Possuir X cores de nome
- `own_titles` - Possuir X títulos

✅ **Progressão:**
- `reach_level` - Atingir nível X
- `earn_gold` - Ganhar X gold total

### Próximos Passos Recomendados
1. ✅ Sistema implementado e em produção
2. ✅ Migrações testadas e funcionais
3. 🔄 Monitorizar logs do Supabase (especialmente triggers)
4. 📊 Implementar analytics de progresso de missões
5. 🎨 Expandir catálogo da loja com novos itens
6. 🏆 Considerar sistema de conquistas/achievements adicional
7. 📱 Otimizar queries do cliente (batch operations)
8. 🔐 Auditoria de segurança RLS completa

### Scripts de Manutenção Disponíveis

**Sincronizar Utilizador Individual:**
```sql
SELECT sync_user_stats_and_missions('user-uuid'::uuid);
```

**Sincronizar Todos os Utilizadores (Migração):**
```sql
DO $$
DECLARE v_user record;
BEGIN
    FOR v_user IN SELECT id FROM public.profiles LOOP
        PERFORM sync_user_stats_and_missions(v_user.id);
    END LOOP;
END $$;
```

**Verificar Inconsistências:**
```sql
-- Ver utilizadores com progresso desincronizado
SELECT 
    p.username,
    ugs.total_pokemon_collected as global_count,
    (SELECT COUNT(*) FROM user_pokemons WHERE user_id = p.id) as real_count,
    ugs.total_pokemon_collected - (SELECT COUNT(*) FROM user_pokemons WHERE user_id = p.id) as diff
FROM profiles p
LEFT JOIN user_global_stats ugs ON ugs.user_id = p.id
WHERE ugs.total_pokemon_collected != (SELECT COUNT(*) FROM user_pokemons WHERE user_id = p.id);
```
