# UmbraDex - Documentação Completa

## Tecnologias Utilizadas

- **Android Studio** com Kotlin
- **Supabase** como backend

A aplicação é gamificada, com forte foco visual. O tema por defeito utiliza tons de roxo.

---

## Autenticação

Ao abrir a aplicação, o utilizador pode escolher entre:

### Login
- Email
- Palavra-passe

O utilizador entra na aplicação com todos os dados já existentes carregados.

### Signup (Registo)

Durante o registo, o utilizador passa por um onboarding em formato de questionário.

---

## Onboarding

O utilizador responde às seguintes etapas:

### 1. Data de Nascimento

### 2. Nível de Conhecimento sobre Pokémon
- Gosto muito de Pokémon
- Conheço algumas coisas
- Conheço bem pouco

### 3. Tipo de Pokémon Favorito (type)

### 4. Escolha de Avatar
- 5 avatares masculinos
- 5 avatares femininos
- Imagens em formato PNG

### 5. Escolha de Nome do Utilizador

### 6. Escolha de um Pokémon Inicial
- Fica equipado no perfil como Pokémon principal (Partner)
- Escolhe uma geração (possível puxar para a direita ou esquerda com uma transição bonita)
- Quando a geração é mudada, mudam os starters
- O jogador escolhe e confirma

**Ao completar o registo, o Starter escolhido é automaticamente:**
- Adicionado à Living Dex (Pokédex aparece colorido)
- Adicionado aos Favoritos
- Definido como Partner equipado
- Contabilizado para missões (ex: escolher Charmander conta para missões de tipo Fire)

**Após concluir o registo, a aplicação abre na Start Page.**

---

## Start Page (Página Inicial)

Funciona como a página principal do perfil do utilizador.

### Elementos Apresentados

- **Título do jogador** centrado
- **Avatar do utilizador**
- **Pokémon equipado** (pet)
- **Nível** (barra de progresso até ao próximo nível)

### Sessão Gráficos de Estatísticas

Possível fechar para não ser necessário scroll no telemóvel.

#### Nota em Cima (quando fechado)
As notas mostram a classificação do jogador:
- **Imperador Pokémon**: muito bom
- **Campeão**: bom
- **Gym King**: suficiente
- **Sonhador**: insuficiente
- **Explorador do Novo Mundo**: muito insuficiente

*Exemplo: muito bom é se tiver colecionado mais de 85% dos pokémons e feito mais de 85% das missões*

#### Gráficos Disponíveis
- Gráfico circular de missões completas (ex: 1/250)
- Gráfico circular de Pokémons obtidos
- Tempo online na app (horas)
- Partners já equipados
- Itens obtidos
- Ouro total ganho

### Interação com o Pokémon

- Ao clicar no Pokémon, é reproduzido um som
- É apresentada uma mensagem relacionada com o Pokémon
- Ao clicar 3 vezes no pet, ele dá um 360°
- Fechar e abrir secção dos gráficos como um pergaminho

---

## Navegação Inferior

Barra de navegação fixa com os seguintes acessos:

1. Start Page
2. Pokédex
3. Poké Live
4. Loja
5. Missões
6. Inventário
7. Equipas

---

## Topo da Aplicação

No topo são apresentados:

- **Nome da aplicação**: UmbraDex
- **Ícone de definições** no canto lado direito
- **Dinheiro (gold)** à esquerda do ícone de definições
- **Número do nível atual** à esquerda do gold

---

## Definições

O menu de definições inclui:

### Ligações Externas
- Ligação para o canal de YouTube
- Ligação para o Discord
- Conexão com Facebook (popup de Coming Soon)

### Configurações de Conta
- Alterar email
- Alterar palavra-passe
- Alterar nome
- Apagar conta

### Configurações de App
- Tirar os sons
- **Pet móvel**: o pet fica na tela, é possível interagir, atirar para cima e ele cair, meter em um canto. Ele automaticamente anda para o lado e para o outro sozinho

### Logout

---

## Pokédex Page

Apresenta todos os Pokémon da geração 1 à 9.

### Funcionalidades de Pesquisa e Filtros

#### Pesquisa
- Pesquisa por nome

#### Filtros
- Tipo
- Ordem alfabética (A–Z / Z–A)
- Número na Pokédex
- Geração
- Apenas favoritos

### Cartão de Pokémon

Cada Pokémon é apresentado num cartão com:

- Imagem do Pokémon
- Nome
- Tipo(s)
- Cor do cartão relacionada ao tipo (colorido se capturado, acinzentado se não)
- Ícone de coração para adicionar aos favoritos

### Sistema de Favoritos

- O utilizador pode favoritar **múltiplos Pokémon**
- O **último Pokémon favoritado** torna-se automaticamente o **Partner equipado** no perfil
- Os Pokémon favoritos aparecem com coração preenchido na Pokédex
- Pokémon capturados (na Living Dex) aparecem com cores; não capturados aparecem acinzentados
- Ao criar conta, o **starter escolhido** é automaticamente:
  - Adicionado à Living Dex
  - Adicionado aos Favoritos
  - Definido como Partner equipado
  - Contabilizado para missões do tipo correspondente

### Detalhes do Pokémon

Ao clicar num Pokémon abre uma janela/modal com:

- Informações gerais
- Estatísticas
- Evoluções

#### Opções Disponíveis
- Voltar à Pokédex
- Adicionar ou remover dos favoritos (toggle)
- Adicionar à Living Dex
- O último favorito torna-se automaticamente o Partner

---

## Poké Live Page

Funciona como um tracker de coleção, semelhante ao Pokémon Home.

### Apresentação

- Todos os Pokémon que o utilizador possui em uma box
- 30 Pokémon por box
- Possível ordenar por número e tipo nas boxes

### Estatísticas

- Número total de Pokémon obtidos
- Número de Pokémon em falta
- Top 3 tipos mais frequentes
- Outras estatísticas gerais da coleção
- Possível fechar em modo pergaminho como na Start Page

---

## Shop Page (Loja)

A loja permite a compra de itens utilizando gold.

### Itens Disponíveis

- **Temas da aplicação**
- **Avatares adicionais** (skins)
- **Badges** (ficam à frente do nome do jogador)
- **Cores para o nome do utilizador** (afeta no Header o "UmbraDex")
- **Títulos** dependendo do nível (a cada 10 níveis um novo título)

### Carregamento de Itens

Os itens são carregados a partir de:
- Pasta pública de res/drawable do projeto
- URLs internas

### Requerimentos de Nível

- **Itens comuns**: nível 2+
- **Itens raros**: nível 10+
- **Itens épicos**: nível 20+
- **Itens lendários**: nível 40+

**Após a compra, os itens são enviados para o Inventário.**

---

## Inventory Page (Inventário)

### Itens Iniciais

O utilizador começa com:

- Pokémon inicial equipado
- Skin inicial
- Tema por defeito
- Badge por defeito
- Cor do nome por defeito (branco)
- Título inicial

### Estrutura da Página

- **Secção superior**: itens equipados
- **Secção inferior**: todos os itens desbloqueados

### Filtros Disponíveis

- Skins
- Temas
- Badges
- Cores de nome

### Notas Importantes

- Pokémon só podem ser equipados através da Pokédex
- No Inventário apenas é apresentado o Pokémon atualmente equipado
- Os títulos são automaticamente mudados ao subir de nível, não é possível equipar um anterior
- No inventário aparece somente o título atual

---

## Missions Page (Missões)

As missões funcionam como um sistema de achievements progressivos.

### Estrutura Geral

- Existem cerca de **200 achievements** no total
- Os achievements estão organizados por categorias e níveis de progressão
- Cada achievement desbloqueia o seguinte dentro da mesma linha de progressão

### Sistema de Progressão

As missões seguem uma lógica sequencial e evolutiva.

#### Exemplo: Coleção de Pokémon

1. Recolher 1 Pokémon (concluído automaticamente após o onboarding)
2. Recolher 10 Pokémon
3. Recolher 25 Pokémon
4. Recolher 50 Pokémon
5. Recolher 100 Pokémon
6. Recolher 250 Pokémon
7. ...
8. Recolher todos os Pokémon

#### Após Concluir uma Missão

- A missão seguinte da mesma categoria é automaticamente ativada
- Apenas a próxima missão relevante fica visível

### Categorias de Achievements

Exemplos de categorias:

- Coleção de Pokémon
- Favoritos
- Pokédex
- Equipas
- Loja
- Personalização
- Missões especiais
- Exploração geral

Cada categoria possui várias linhas de progressão independentes.

### Apresentação na Interface

#### Vista Principal

- Mostra apenas as missões ativas atuais
- Normalmente entre 3 a 5 missões visíveis
- Cada missão apresenta:
  - Título
  - Descrição
  - Barra de progresso
  - Recompensas

#### Vista Completa

- Botão "Ver todas as missões"
- Abre um modal com:
  - Todas as categorias
  - Todas as linhas de achievements
  - Estado de cada missão:
    - Bloqueada
    - Ativa
    - Concluída
- Opção para voltar à vista reduzida

### Recompensas

Cada achievement concede:

- Gold
- Experiência (XP)

### Raridade das Missões

Cada achievement possui um nível de raridade:

- Common
- Rare
- Epic
- Legendary

A raridade influencia:

- A dificuldade
- O valor das recompensas
- A visibilidade dentro da aplicação

### Comportamento Automático

- Missões concluídas são marcadas automaticamente
- A missão seguinte da mesma progressão é ativada sem intervenção do utilizador
- O progresso é atualizado em tempo real

---

## Team Page (Equipas)

Permite a criação e gestão de equipas Pokémon.

### Regras

- **Máximo de 22 equipas**
- Cada equipa pode ter:
  - Máximo de 6 Pokémon
  - Mínimo de 0 Pokémon

### Criação de Equipa

Ao criar uma equipa, o utilizador define:

- Nome da equipa
- Região

#### Após a Criação

- O cartão da equipa recebe uma cor em gradiente aleatória

### Gestão de Equipa

- Cada slot vazio é representado por um botão "+"
- Ao clicar, é apresentada uma lista de Pokémon
- Possível pesquisar por Pokémon escrevendo o seu nome
- O Pokémon selecionado substitui o espaço vazio

### Funcionalidades Adicionais

- Download do cartão da equipa em formato PNG
- Eliminar equipa
- Alterar Pokémon selecionados

### Limitações

- Não é possível alterar o nome da equipa
- Não é possível alterar a região ou geração

---

## Sistema de Raridades

Os seguintes elementos possuem sistema de raridade:

- Skins
- Cores de nome
- Temas
- Missões
- Badges

### Tipos de Raridade

- Common
- Rare
- Epic
- Legendary

---

## Economia do Projeto

### Preços na Loja

| Raridade | Preço |
|----------|-------|
| Comum | 300 coins |
| Raro | 900 coins |
| Épico | 1600 coins |
| Lendário | 3500 coins |

### Ganho de Ouro (Missões)

| Raridade | Recompensa |
|----------|------------|
| Comum | 20 coins |
| Raro | 50 coins |
| Épico | 120 coins |
| Lendário | 350 coins |

### Ganho em Nível

- **Default** (cada 1 nível): 5 coins
- **Cada 5 níveis**: 50 coins
- **Cada 10 níveis**: 150 coins

---

## Sistema de Experiência

### Ganho de XP (Missões)

| Raridade | XP |
|----------|-----|
| Comum | 50 exp |
| Raro | 100 exp |
| Épico | 200 exp |
| Lendário | 300 exp |

### Ganho por Pokémon Adicionado na Living Box

- **10 XP** por cada Pokémon adicionado

---

## Tabela de Níveis

Lista completa de níveis e XP necessária para progressão:

| Nível | XP Necessária | | Nível | XP Necessária | | Nível | XP Necessária |
|-------|---------------|---|-------|---------------|---|-------|---------------|
| 1 → 2 | 60 XP | | 35 → 36 | 400 XP | | 69 → 70 | 740 XP |
| 2 → 3 | 70 XP | | 36 → 37 | 410 XP | | 70 → 71 | 750 XP |
| 3 → 4 | 80 XP | | 37 → 38 | 420 XP | | 71 → 72 | 760 XP |
| 4 → 5 | 90 XP | | 38 → 39 | 430 XP | | 72 → 73 | 770 XP |
| 5 → 6 | 100 XP | | 39 → 40 | 440 XP | | 73 → 74 | 780 XP |
| 6 → 7 | 110 XP | | 40 → 41 | 450 XP | | 74 → 75 | 790 XP |
| 7 → 8 | 120 XP | | 41 → 42 | 460 XP | | 75 → 76 | 800 XP |
| 8 → 9 | 130 XP | | 42 → 43 | 470 XP | | 76 → 77 | 810 XP |
| 9 → 10 | 140 XP | | 43 → 44 | 480 XP | | 77 → 78 | 820 XP |
| 10 → 11 | 150 XP | | 44 → 45 | 490 XP | | 78 → 79 | 830 XP |
| 11 → 12 | 160 XP | | 45 → 46 | 500 XP | | 79 → 80 | 840 XP |
| 12 → 13 | 170 XP | | 46 → 47 | 510 XP | | 80 → 81 | 850 XP |
| 13 → 14 | 180 XP | | 47 → 48 | 520 XP | | 81 → 82 | 860 XP |
| 14 → 15 | 190 XP | | 48 → 49 | 530 XP | | 82 → 83 | 870 XP |
| 15 → 16 | 200 XP | | 49 → 50 | 540 XP | | 83 → 84 | 880 XP |
| 16 → 17 | 210 XP | | 50 → 51 | 550 XP | | 84 → 85 | 890 XP |
| 17 → 18 | 220 XP | | 51 → 52 | 560 XP | | 85 → 86 | 900 XP |
| 18 → 19 | 230 XP | | 52 → 53 | 570 XP | | 86 → 87 | 910 XP |
| 19 → 20 | 240 XP | | 53 → 54 | 580 XP | | 87 → 88 | 920 XP |
| 20 → 21 | 250 XP | | 54 → 55 | 590 XP | | 88 → 89 | 930 XP |
| 21 → 22 | 260 XP | | 55 → 56 | 600 XP | | 89 → 90 | 940 XP |
| 22 → 23 | 270 XP | | 56 → 57 | 610 XP | | 90 → 91 | 950 XP |
| 23 → 24 | 280 XP | | 57 → 58 | 620 XP | | 91 → 92 | 960 XP |
| 24 → 25 | 290 XP | | 58 → 59 | 630 XP | | 92 → 93 | 970 XP |
| 25 → 26 | 300 XP | | 59 → 60 | 640 XP | | 93 → 94 | 980 XP |
| 26 → 27 | 310 XP | | 60 → 61 | 650 XP | | 94 → 95 | 990 XP |
| 27 → 28 | 320 XP | | 61 → 62 | 660 XP | | 95 → 96 | 1000 XP |
| 28 → 29 | 330 XP | | 62 → 63 | 670 XP | | 96 → 97 | 1010 XP |
| 29 → 30 | 340 XP | | 63 → 64 | 680 XP | | 97 → 98 | 1020 XP |
| 30 → 31 | 350 XP | | 64 → 65 | 690 XP | | 98 → 99 | 1030 XP |
| 31 → 32 | 360 XP | | 65 → 66 | 700 XP | | 99 → 100 | 1040 XP |
| 32 → 33 | 370 XP | | 66 → 67 | 710 XP | | | |
| 33 → 34 | 380 XP | | 67 → 68 | 720 XP | | | |
| 34 → 35 | 390 XP | | 68 → 69 | 730 XP | | | |

---

## Resumo das Funcionalidades

### Páginas Principais
1. **Start Page** - Perfil do utilizador com estatísticas
2. **Pokédex** - Catálogo completo de Pokémon
3. **Poké Live** - Sistema de coleção tipo Living Dex
4. **Loja** - Compra de itens cosméticos e personalizações
5. **Missões** - Sistema de achievements progressivos
6. **Inventário** - Gestão de itens desbloqueados
7. **Equipas** - Criação e gestão de equipas Pokémon

### Características Principais
- Sistema de progressão por níveis (1-100)
- Economia baseada em gold e XP
- Sistema de raridades (Common, Rare, Epic, Legendary)
- Personalização completa (temas, avatares, badges, cores)
- Pet interativo na tela
- Gamificação com missões e recompensas
- Interface visualmente rica em tons de roxo

---

**Documento criado com base nas especificações do projeto UmbraDex**