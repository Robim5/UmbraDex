-- ============================================================================
-- UMBRADEX - DADOS DA LOJA (SHOP ITEMS)
-- ============================================================================
-- Este arquivo popula a tabela shop_items com todos os itens disponíveis
-- Organizado por: Themes, Skins, Badges, Name Colors
-- ============================================================================

-- ============================================================================
-- A. TEMAS (Themes)
-- ============================================================================

INSERT INTO public.shop_items (type, name, rarity, price, colors, min_level, description, sort_order) VALUES
-- Tema padrão (grátis)
('theme', 'Classic Purple', 'common', 0, '["#6366F1", "#8B5CF6"]', 0, 'The original UmbraDex theme', 1),

-- Temas Common (Level 2+)
('theme', 'Ocean Depths', 'common', 300, '["#0EA5E9", "#06B6D4"]', 2, 'Deep blue waters', 2),
('theme', 'Forest Green', 'common', 300, '["#10B981", "#059669"]', 2, 'Lush forest vibes', 3),
('theme', 'Sunset Orange', 'common', 300, '["#F97316", "#EA580C"]', 2, 'Warm sunset colors', 4),
('theme', 'Rose Pink', 'common', 300, '["#EC4899", "#DB2777"]', 2, 'Soft pink aesthetics', 5),

-- Temas Rare (Level 10+)
('theme', 'Midnight Sky', 'rare', 900, '["#1E293B", "#334155"]', 10, 'Dark and mysterious', 10),
('theme', 'Cherry Blossom', 'rare', 900, '["#FCA5A5", "#FDA4AF"]', 10, 'Spring blossoms', 11),
('theme', 'Electric Blue', 'rare', 900, '["#3B82F6", "#2563EB"]', 10, 'High voltage', 12),
('theme', 'Emerald Dream', 'rare', 900, '["#34D399", "#10B981"]', 10, 'Mystical greens', 13),

-- Temas Epic (Level 20+)
('theme', 'Ghost Shadow', 'epic', 1600, '["#6B21A8", "#7C3AED"]', 20, 'Haunting purples', 20),
('theme', 'Dragon Fire', 'epic', 1600, '["#DC2626", "#B91C1C"]', 20, 'Fierce dragon energy', 21),
('theme', 'Ice Crystal', 'epic', 1600, '["#7DD3FC", "#38BDF8"]', 20, 'Frozen beauty', 22),
('theme', 'Solar Flare', 'epic', 1600, '["#FBBF24", "#F59E0B"]', 20, 'Burning bright', 23),

-- Temas Legendary (Level 40+)
('theme', 'Rainbow Aura', 'legendary', 3500, '["#F472B6", "#FB923C", "#FBBF24", "#4ADE80", "#60A5FA", "#A78BFA"]', 40, 'All colors of power', 30),
('theme', 'Gold Luxe', 'legendary', 3500, '["#FCD34D", "#FBBF24"]', 40, 'Pure gold elegance', 31),
('theme', 'Shadow Umbra', 'legendary', 3500, '["#0F172A", "#1E1B4B"]', 40, 'Darkest shadows', 32),
('theme', 'Aurora Borealis', 'legendary', 3500, '["#34D399", "#3B82F6", "#A78BFA"]', 40, 'Northern lights', 33);

-- ============================================================================
-- B. AVATARES/SKINS
-- ============================================================================

-- Skins padrão masculinos (grátis, escolhidos no onboarding)
INSERT INTO public.shop_items (type, name, rarity, price, asset_url, min_level, description, sort_order) VALUES
('skin', 'Standard Male 1', 'common', 0, 'standard_male1.png', 0, 'Classic trainer look', 100),
('skin', 'Standard Male 2', 'common', 0, 'standard_male2.png', 0, 'Cool trainer style', 101),
('skin', 'Standard Male 3', 'common', 0, 'standard_male3.png', 0, 'Adventure ready', 102),
('skin', 'Standard Male 4', 'common', 0, 'standard_male4.png', 0, 'Urban explorer', 103),
('skin', 'Standard Male 5', 'common', 0, 'standard_male5.png', 0, 'Forest ranger', 104),

-- Skins padrão femininos (grátis, escolhidos no onboarding)
('skin', 'Standard Female 1', 'common', 0, 'standard_female1.png', 0, 'Classic trainer look', 105),
('skin', 'Standard Female 2', 'common', 0, 'standard_female2.png', 0, 'Elegant style', 106),
('skin', 'Standard Female 3', 'common', 0, 'standard_female3.png', 0, 'Sporty trainer', 107),
('skin', 'Standard Female 4', 'common', 0, 'standard_female4.png', 0, 'Modern explorer', 108),
('skin', 'Standard Female 5', 'common', 0, 'standard_female5.png', 0, 'Nature lover', 109),

-- Skins Common (Level 2+)
('skin', 'Green Hair Trainer', 'common', 300, 'shop_common_greenhair.png', 2, 'Fresh and unique', 110),
('skin', 'Dance Girl', 'common', 300, 'shop_common_girldance.png', 2, 'Rhythm and style', 111),
('skin', 'Fight Boy', 'common', 300, 'shop_common_fightboy.png', 2, 'Ready to battle', 112),

-- Skins Rare (Level 10+)
('skin', 'Artist Boy', 'rare', 900, 'shop_rare_artistboy.png', 10, 'Creative spirit', 120),

-- Skins Epic (Level 20+)
('skin', 'Water Master', 'epic', 1600, 'shop_epic_waterguy.png', 20, 'Aquatic specialist', 130),
('skin', 'Purple Mystic', 'epic', 1600, 'shop_epic_purplegirl.png', 20, 'Psychic powers', 131),

-- Skins Legendary (Level 40+)
('skin', 'Dream Walker', 'legendary', 3500, 'shop_legendary_dreamgirl.png', 40, 'Between worlds', 140),
('skin', 'Shadow Ninja', 'legendary', 3500, 'shop_legendary_ninjaboy.png', 40, 'Silent and swift', 141),
('skin', 'Ancient Master', 'legendary', 3500, 'shop_legendary_oldguy.png', 40, 'Wisdom of ages', 142);

-- ============================================================================
-- C. BADGES
-- ============================================================================

INSERT INTO public.shop_items (type, name, rarity, price, asset_url, min_level, description, sort_order) VALUES
-- Badge padrão (grátis)
('badge', 'Starter Badge', 'common', 0, 'start_badget.png', 0, 'Your first badge', 200),

-- Badges Common (Level 2+) - Elementos básicos
('badge', 'Balance Badge', 'common', 300, 'shop_common_balance.png', 2, 'Perfect harmony', 201),
('badge', 'Feather Badge', 'common', 300, 'shop_common_feather.png', 2, 'Light as air', 202),
('badge', 'Rain Badge', 'common', 300, 'shop_common_rain.png', 2, 'Water element', 203),
('badge', 'Rock Badge', 'common', 300, 'shop_common_rock.png', 2, 'Solid foundation', 204),

-- Badges Rare (Level 10+) - Elementos avançados
('badge', 'Fire Badge', 'rare', 900, 'shop_rare_fire.png', 10, 'Burning passion', 210),
('badge', 'Leaf Badge', 'rare', 900, 'shop_rare_leaf.png', 10, 'Nature''s power', 211),
('badge', 'Petal Badge', 'rare', 900, 'shop_rare_petal.png', 10, 'Grass mastery', 212),
('badge', 'Water Badge', 'rare', 900, 'shop_rare_water.png', 10, 'Aquatic force', 213),

-- Badges Epic (Level 20+) - Elementos especiais
('badge', 'Ghost Badge', 'epic', 1600, 'shop_epic_ghost.png', 20, 'Spirit realm', 220),
('badge', 'Ice Badge', 'epic', 1600, 'shop_epic_ice.png', 20, 'Frozen power', 221),
('badge', 'Sunflower Badge', 'epic', 1600, 'shop_epic_sunflower.png', 20, 'Solar energy', 222),

-- Badges Legendary (Level 40+) - Elementos míticos
('badge', 'Gold Badge', 'legendary', 3500, 'shop_legendary_gold.png', 40, 'Ultimate wealth', 230),
('badge', 'Heart Badge', 'legendary', 3500, 'shop_legendary_hearth.png', 40, 'Pure soul', 231),
('badge', 'Rainbow Badge', 'legendary', 3500, 'shop_legendary_rainbow.png', 40, 'Master of all', 232),
('badge', 'Demon Badge', 'legendary', 3500, 'shop_legendary_demon.png', 40, 'Dark power', 233);

-- ============================================================================
-- D. CORES DE NOME (Name Colors) - VERSÃO FINAL SEM ERROS
-- ============================================================================

INSERT INTO public.shop_items (type, name, rarity, price, colors, min_level, description, sort_order) VALUES
-- Cor padrão (grátis)
('name_color', 'Trainer White', 'common', 0, '["#FFFFFF"]', 0, 'Classic white', 300),

-- Cores Common (Level 2+) - Cores sólidas
('name_color', 'Classic Red', 'common', 300, '["#EF4444"]', 2, 'Bold red', 301),
('name_color', 'Ocean Blue', 'common', 300, '["#3B82F6"]', 2, 'Deep blue', 302),
('name_color', 'Leaf Green', 'common', 300, '["#10B981"]', 2, 'Fresh green', 303),
('name_color', 'Sunshine Yellow', 'common', 300, '["#FBBF24"]', 2, 'Bright yellow', 304),
('name_color', 'Shadow Grey', 'common', 300, '["#6B7280"]', 2, 'Neutral grey', 305),
('name_color', 'Royal Purple', 'common', 300, '["#8B5CF6"]', 2, 'Majestic purple', 306),

-- Cores Rare (Level 10+) - Gradientes duplos
('name_color', 'Fire Gradient', 'rare', 900, '["#F97316", "#FBBF24"]', 10, 'Flames of passion', 310),
('name_color', 'Water Gradient', 'rare', 900, '["#0EA5E9", "#3B82F6"]', 10, 'Ocean depths', 311),
('name_color', 'Grass Gradient', 'rare', 900, '["#84CC16", "#10B981"]', 10, 'Nature''s embrace', 312),
('name_color', 'Psychic Gradient', 'rare', 900, '["#EC4899", "#A855F7"]', 10, 'Mind powers', 313),
('name_color', 'Electric Spark', 'rare', 900, '["#FBBF24", "#F97316"]', 10, 'Lightning strike', 314),
('name_color', 'Fighting Spirit', 'rare', 900, '["#DC2626", "#EA580C"]', 10, 'Warrior energy', 315),

-- Cores Epic (Level 20+) - Gradientes triplos
('name_color', 'Frozen Crystal', 'epic', 1600, '["#67E8F9", "#22D3EE", "#06B6D4"]', 20, 'Frozen beauty', 320), -- Já estava corrigido
('name_color', 'Toxic Sludge', 'epic', 1600, '["#C084FC", "#A855F7", "#7C3AED"]', 20, 'Poison power', 321),
('name_color', 'Lava Flow', 'epic', 1600, '["#F97316", "#DC2626", "#B91C1C"]', 20, 'Volcanic fury', 322),
('name_color', 'Forest Spirit', 'epic', 1600, '["#D9F99D", "#84CC16", "#65A30D"]', 20, 'Ancient woods', 323),
('name_color', 'Steel Shine', 'epic', 1600, '["#CBD5E1", "#94A3B8", "#64748B"]', 20, 'Metallic gleam', 324),

-- Cores Legendary (Level 40+) - Gradientes complexos
('name_color', 'Galaxy', 'legendary', 3500, '["#1E1B4B", "#4C1D95", "#6366F1", "#818CF8"]', 40, 'Cosmic wonder', 330),
('name_color', 'Rainbow', 'legendary', 3500, '["#EF4444", "#F97316", "#FBBF24", "#10B981", "#3B82F6", "#8B5CF6"]', 40, 'All colors unite', 331),
('name_color', 'Golden Luxury', 'legendary', 3500, '["#FEF3C7", "#FCD34D", "#F59E0B", "#FCD34D", "#FEF3C7"]', 40, 'Golden majesty', 332), -- NOME MUDADO: De 'Gold Luxe' para 'Golden Luxury'
('name_color', 'Silver Chrome', 'legendary', 3500, '["#F3F4F6", "#D1D5DB", "#6B7280", "#D1D5DB", "#F3F4F6"]', 40, 'Silver elegance', 333),
('name_color', 'Umbra Shadow', 'legendary', 3500, '["#030712", "#1F2937", "#374151", "#1F2937", "#030712"]', 40, 'Ultimate darkness', 334),
('name_color', 'Dragon Flame', 'legendary', 3500, '["#450A0A", "#991B1B", "#DC2626", "#FCA5A5", "#DC2626"]', 40, 'Dragon''s breath', 335);

-- TÍTULOS (desbloqueados por nível)
INSERT INTO public.titles (title_text, min_level, rarity, colors) VALUES
('Rookie', 1, 'common', '["#FFFFFF"]'),
('Bug Catcher', 5, 'common', '["#8BC34A", "#33691E"]'),
('Student', 10, 'common', '["#90CAF9", "#1976D2"]'),
('Ace Trainer', 15, 'rare', '["#FF9800", "#F57C00"]'),
('Gym Leader', 20, 'rare', '["#FFEB3B", "#FBC02D"]'),
('Explorer', 25, 'rare', '["#009688", "#004D40"]'),
('Ranger', 30, 'rare', '["#4CAF50", "#1B5E20"]'),
('Veteran', 40, 'epic', '["#607D8B", "#263238"]'),
('Elite Four', 50, 'epic', '["#9C27B0", "#4A148C"]'),
('Champion', 60, 'epic', '["#FFD700", "#FF6F00", "#FFD700"]'),
('Professor', 70, 'legendary', '["#FFFFFF", "#E3F2FD", "#90CAF9"]'),
('Master', 80, 'legendary', '["#E040FB", "#AA00FF", "#4A148C"]'),
('Legend', 90, 'legendary', '["#FFD700", "#D50000", "#1A237E"]'),
('Mythical', 100, 'legendary', '["#F06292", "#C2185B", "#880E4F", "#AD1457"]');

-- ============================================================================
-- UMBRADEX - MEGA PACK DE MISSÕES (250+)
-- ============================================================================
-- ATENÇÃO: Isto limpa as missões atuais para recriar a estrutura completa!
TRUNCATE TABLE public.missions CASCADE;
ALTER SEQUENCE public.missions_id_seq RESTART WITH 1;

-- ============================================================================
-- 1. LIVING DEX MASTER (Progressão Global) - 25 Missões
-- ============================================================================
-- Focado em "Recolher X Pokémon" [cite: 180-189]

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 150, 200, 250, 300, 400, 500, 600, 700, 800, 900, 950, 1000, 1025];
    v_val int;
    v_rarity rarity_type;
    v_gold int;
    v_xp int;
BEGIN
    v_prev_id := NULL;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        -- Definir raridade e recompensas baseadas na dificuldade [cite: 289-304]
        IF v_val <= 20 THEN v_rarity := 'common'; v_gold := 20; v_xp := 50;
        ELSIF v_val <= 100 THEN v_rarity := 'rare'; v_gold := 50; v_xp := 100;
        ELSIF v_val <= 600 THEN v_rarity := 'epic'; v_gold := 120; v_xp := 200;
        ELSE v_rarity := 'legendary'; v_gold := 350; v_xp := 300; END IF;

        -- Recompensa especial para o final (1025)
        IF v_val = 1025 THEN v_gold := 5000; v_xp := 5000; END IF;

        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Collector ' || v_val, 
            'Collect ' || v_val || ' unique Pokémon', 
            v_rarity, 
            'collection', 
            'collect_pokemon', 
            v_val, 
            v_gold, 
            v_xp, 
            v_prev_id, 
            v_val
        ) RETURNING id INTO v_prev_id;
    END LOOP;
END $$;

-- ============================================================================
-- 2. TYPE MASTERY (Especialistas por Tipo) - 108 Missões
-- ============================================================================
-- 18 Tipos x 6 Tiers cada. Cria missões como "Fire Rookie", "Water Legend".
-- Baseado nos tipos do sistema[cite: 22, 97].

DO $$
DECLARE
    -- Lista dos 18 tipos oficiais
    v_types text[] := ARRAY['normal', 'fire', 'water', 'grass', 'electric', 'ice', 'fighting', 'poison', 'ground', 'flying', 'psychic', 'bug', 'rock', 'ghost', 'dragon', 'dark', 'steel', 'fairy'];
    v_type text;
    v_prev_id int;
    -- Tiers: Quantidade necessária para cada nível
    v_tiers int[] := ARRAY[5, 15, 30, 50, 75, 100]; 
    v_titles text[] := ARRAY['Rookie', 'Fan', 'Expert', 'Master', 'Grandmaster', 'Legend'];
    v_i int;
    v_rarity rarity_type;
    v_gold int;
    v_xp int;
BEGIN
    FOREACH v_type IN ARRAY v_types LOOP
        v_prev_id := NULL; -- Reseta o pré-requisito para cada novo tipo
        
        FOR v_i IN 1..6 LOOP
            -- Lógica de recompensas progressiva
            IF v_i <= 2 THEN v_rarity := 'common'; v_gold := 20; v_xp := 50;
            ELSIF v_i <= 4 THEN v_rarity := 'rare'; v_gold := 50; v_xp := 100;
            ELSIF v_i = 5 THEN v_rarity := 'epic'; v_gold := 120; v_xp := 200;
            ELSE v_rarity := 'legendary'; v_gold := 350; v_xp := 300; END IF;

            INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
            VALUES (
                INITCAP(v_type) || ' ' || v_titles[v_i], 
                'Collect ' || v_tiers[v_i] || ' ' || INITCAP(v_type) || '-type Pokémon', 
                v_rarity, 
                'type', 
                'collect_type_' || v_type, 
                v_tiers[v_i], 
                v_gold, 
                v_xp, 
                v_prev_id, 
                1000 + (array_position(v_types, v_type) * 10) + v_i
            ) RETURNING id INTO v_prev_id;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 3. GENERATION TIMELINE (Por Geração) - 45 Missões
-- ============================================================================
-- 9 Gerações x 5 Tiers cada[cite: 93, 102].
-- Tiers: 10, 30, 50, 80, All(progressivo).

DO $$
DECLARE
    v_gen int;
    v_prev_id int;
    v_tiers int[] := ARRAY[10, 25, 50, 75, 100]; -- Assumindo ~100+ por gen média
    v_tier_val int;
    v_i int;
    v_rarity rarity_type;
    v_gold int;
    v_xp int;
BEGIN
    FOR v_gen IN 1..9 LOOP
        v_prev_id := NULL;
        
        FOR v_i IN 1..5 LOOP
            v_tier_val := v_tiers[v_i];
            
            IF v_i <= 2 THEN v_rarity := 'common'; v_gold := 25; v_xp := 60;
            ELSIF v_i <= 4 THEN v_rarity := 'rare'; v_gold := 60; v_xp := 120;
            ELSE v_rarity := 'epic'; v_gold := 150; v_xp := 250; END IF;

            INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
            VALUES (
                'Gen ' || v_gen || ' Explorer ' || v_i, 
                'Collect ' || v_tier_val || ' Pokémon from Generation ' || v_gen, 
                v_rarity, 
                'generation', 
                'collect_gen_' || v_gen, 
                v_tier_val, 
                v_gold, 
                v_xp, 
                v_prev_id, 
                2000 + (v_gen * 10) + v_i
            ) RETURNING id INTO v_prev_id;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 4. GOLD TYCOON (Economia) - 15 Missões
-- ============================================================================
-- Focado em "Earn Gold"[cite: 55].

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[200, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000, 2000000, 5000000, 10000000];
    v_val int;
    v_i int;
    v_rarity rarity_type;
BEGIN
    v_prev_id := NULL;
    v_i := 1;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        IF v_i <= 5 THEN v_rarity := 'common';
        ELSIF v_i <= 10 THEN v_rarity := 'rare';
        ELSIF v_i <= 13 THEN v_rarity := 'epic';
        ELSE v_rarity := 'legendary'; END IF;

        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Millionaire ' || v_i, 
            'Earn a total of ' || v_val || ' Gold', 
            v_rarity, 
            'gold', 
            'earn_gold', 
            v_val, 
            v_i * 10, -- Ouro gera Ouro
            v_i * 20, 
            v_prev_id, 
            3000 + v_i
        ) RETURNING id INTO v_prev_id;
        v_i := v_i + 1;
    END LOOP;
END $$;

-- ============================================================================
-- 5. SHOPAHOLIC (Loja Geral) - 12 Missões
-- ============================================================================
-- Comprar itens na loja [cite: 135-141].

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[1, 5, 10, 25, 50, 100, 150, 200, 300, 400, 500, 1000];
    v_val int;
    v_i int;
BEGIN
    v_prev_id := NULL;
    v_i := 1;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Big Spender ' || v_i, 
            'Buy ' || v_val || ' items from the Shop', 
            CASE WHEN v_val < 25 THEN 'common'::rarity_type WHEN v_val < 150 THEN 'rare'::rarity_type ELSE 'epic'::rarity_type END,
            'shop', 
            'shop_buy', 
            v_val, 
            50, -- Cashback fixo
            100, 
            v_prev_id, 
            4000 + v_i
        ) RETURNING id INTO v_prev_id;
        v_i := v_i + 1;
    END LOOP;
END $$;

-- ============================================================================
-- 6. FASHION & STYLE (Categorias Específicas) - 20 Missões
-- ============================================================================
-- Focado em Skins, Badges, Temas e Cores [cite: 162-166].

DO $$
DECLARE
    v_cats text[] := ARRAY['own_skins', 'own_badges', 'own_themes', 'own_name_colors'];
    v_cat_names text[] := ARRAY['Fashionista', 'Badge Collector', 'Theme Artist', 'Color Master'];
    v_cat text;
    v_prev_id int;
    v_tiers int[] := ARRAY[1, 5, 10, 20, 50]; -- 5 tiers por categoria
    v_val int;
    v_i int;
    v_idx int;
BEGIN
    FOR v_idx IN 1..4 LOOP
        v_cat := v_cats[v_idx];
        v_prev_id := NULL;
        v_i := 1;
        
        FOREACH v_val IN ARRAY v_tiers LOOP
            INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
            VALUES (
                v_cat_names[v_idx] || ' ' || v_i, 
                'Own ' || v_val || ' items of this type', 
                CASE WHEN v_i <= 2 THEN 'common'::rarity_type WHEN v_i <= 4 THEN 'rare'::rarity_type ELSE 'epic'::rarity_type END,
                'customization', 
                v_cat, 
                v_val, 
                100 * v_i, 
                150 * v_i, 
                v_prev_id, 
                5000 + (v_idx * 100) + v_i
            ) RETURNING id INTO v_prev_id;
            v_i := v_i + 1;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 7. TEAM LEADER (Equipas) - 10 Missões
-- ============================================================================
-- Criar equipas [cite: 247-252].

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[1, 3, 5, 7, 10, 12, 15, 18, 20, 22]; -- Max 22 equipas [cite: 247]
    v_val int;
    v_i int;
BEGIN
    v_prev_id := NULL;
    v_i := 1;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Strategist ' || v_i, 
            'Create ' || v_val || ' Teams', 
            CASE WHEN v_i < 5 THEN 'common'::rarity_type ELSE 'rare'::rarity_type END, 
            'team', 
            'create_team', 
            v_val, 
            50, 
            100, 
            v_prev_id, 
            6000 + v_i
        ) RETURNING id INTO v_prev_id;
        v_i := v_i + 1;
    END LOOP;
END $$;

-- ============================================================================
-- 8. HEART COLLECTOR (Favoritos) - 15 Missões
-- ============================================================================
-- Adicionar favoritos[cite: 111, 119].

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[1, 5, 10, 20, 30, 40, 50, 75, 100, 150, 200, 250, 300, 400, 500];
    v_val int;
    v_i int;
BEGIN
    v_prev_id := NULL;
    v_i := 1;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Love is in the Air ' || v_i, 
            'Mark ' || v_val || ' Pokémon as Favorites', 
            CASE WHEN v_val < 50 THEN 'common'::rarity_type WHEN v_val < 200 THEN 'rare'::rarity_type ELSE 'epic'::rarity_type END,
            'collection', 
            'favorite_count', 
            v_val, 
            20, 
            50, 
            v_prev_id, 
            7000 + v_i
        ) RETURNING id INTO v_prev_id;
        v_i := v_i + 1;
    END LOOP;
END $$;

-- ============================================================================
-- 9. LEVEL CLIMBER (Níveis) - 20 Missões
-- ============================================================================
-- Alcançar níveis .

DO $$
DECLARE
    v_prev_id int;
    v_tiers int[] := ARRAY[5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100];
    v_val int;
    v_i int;
BEGIN
    v_prev_id := NULL;
    v_i := 1;
    
    FOREACH v_val IN ARRAY v_tiers LOOP
        INSERT INTO public.missions (title, description, rarity, category, requirement_type, requirement_value, gold_reward, xp_reward, prerequisite_mission_id, sort_order)
        VALUES (
            'Rising Star ' || v_val, 
            'Reach Level ' || v_val, 
            CASE WHEN v_val < 40 THEN 'common'::rarity_type WHEN v_val < 75 THEN 'rare'::rarity_type ELSE 'legendary'::rarity_type END,
            'level', 
            'reach_level', 
            v_val, 
            v_val * 10, -- Ouro escala com o nível
            0, -- Níveis não dão XP (senão seria ciclo infinito)
            v_prev_id, 
            8000 + v_i
        ) RETURNING id INTO v_prev_id;
        v_i := v_i + 1;
    END LOOP;
END $$;