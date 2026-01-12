package com.umbra.umbradex.ui.teams

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.umbra.umbradex.data.model.Pokemon
import com.umbra.umbradex.data.repository.PokemonRepository
import com.umbra.umbradex.ui.theme.*
import com.umbra.umbradex.utils.Resource
import com.umbra.umbradex.utils.createTypeGradientBrush
import com.umbra.umbradex.utils.getTypeColor
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.flow.collect

/**
 * Dados simplificados de Pokémon para carregamento rápido.
 * Carrega apenas o necessário: id, nome e tipos (para o gradiente).
 */
data class SimplePokemon(
    val id: Int,
    val name: String,
    val types: List<String> = emptyList()
) {
    val imageUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
    
    fun toPokemon(): Pokemon = Pokemon(
        id = id,
        name = name,
        imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png",
        types = types,
        height = 0.0,
        weight = 0.0
    )
}

// Cache global para lista simplificada de Pokémon (carrega uma vez, usa sempre)
private var cachedSimplePokemonList: List<SimplePokemon>? = null

@Composable
fun PokemonSelectorDialog(
    onDismiss: () -> Unit,
    onPokemonSelected: (Pokemon) -> Unit,
    excludedPokemonIds: Set<Int> = emptySet()
) {
    // Estado local para a pesquisa e dados
    var searchQuery by remember { mutableStateOf("") }
    var allPokemon by remember { mutableStateOf<List<SimplePokemon>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Pokémon filtrados baseados na pesquisa e exclusões
    val filteredPokemon = remember(searchQuery, allPokemon, excludedPokemonIds) {
        val query = searchQuery.lowercase().trim()
        allPokemon
            .filter { pokemon ->
                if (query.isEmpty()) true
                else pokemon.name.lowercase().contains(query) ||
                        pokemon.id.toString().contains(query) ||
                        pokemon.id.toString().padStart(3, '0').contains(query)
            }
            .filterNot { it.id in excludedPokemonIds }
    }

    // Carregar pokémon quando o diálogo abre - usa cache se disponível
    LaunchedEffect(Unit) {
        isLoading = true
        
        // Verifica se temos cache
        cachedSimplePokemonList?.let { cached ->
            allPokemon = cached
            isLoading = false
            return@LaunchedEffect
        }
        
        // Se não há cache, criar lista simplificada (muito mais rápido)
        try {
            // Lista pré-definida com todos os Pokémon (1-1025)
            // Carrega apenas ID e nome, sem chamar API para cada um
            val simplePokemonList = (1..1025).map { id ->
                SimplePokemon(
                    id = id,
                    name = getPokemonName(id),
                    types = emptyList() // Tipos serão carregados quando selecionar
                )
            }
            
            cachedSimplePokemonList = simplePokemonList
            allPokemon = simplePokemonList
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Falha ao carregar lista de Pokémon"
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selecionar Pokémon",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Campo de pesquisa melhorado
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        // Scroll para o topo quando pesquisa muda
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pesquisar por nome ou número...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Pesquisar")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Counter de resultados
                Text(
                    text = "${filteredPokemon.size} Pokémon encontrados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Conteúdo baseado no estado
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "A carregar Pokémon...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    filteredPokemon.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum Pokémon encontrado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tenta um termo de pesquisa diferente",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredPokemon,
                                key = { it.id }
                            ) { pokemon ->
                                PokemonListItemCard(
                                    pokemon = pokemon,
                                    onClick = {
                                        onPokemonSelected(pokemon.toPokemon())
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PokemonListItemCard(
    pokemon: SimplePokemon,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem do Pokémon com fundo circular
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informações do Pokémon
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${pokemon.id.toString().padStart(3, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pokemon.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Ícone de seta
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Função que retorna o nome do Pokémon baseado no ID.
 * Lista completa de todos os 1025 Pokémon para carregamento instantâneo.
 */
private fun getPokemonName(id: Int): String {
    // Mapa de IDs para nomes (lista completa Geração 1-9)
    return pokemonNames.getOrElse(id) { "Pokemon-$id" }
}

// Lista de nomes de todos os Pokémon (1-1025)
private val pokemonNames = mapOf(
    // Geração 1 (1-151)
    1 to "bulbasaur", 2 to "ivysaur", 3 to "venusaur", 4 to "charmander", 5 to "charmeleon",
    6 to "charizard", 7 to "squirtle", 8 to "wartortle", 9 to "blastoise", 10 to "caterpie",
    11 to "metapod", 12 to "butterfree", 13 to "weedle", 14 to "kakuna", 15 to "beedrill",
    16 to "pidgey", 17 to "pidgeotto", 18 to "pidgeot", 19 to "rattata", 20 to "raticate",
    21 to "spearow", 22 to "fearow", 23 to "ekans", 24 to "arbok", 25 to "pikachu",
    26 to "raichu", 27 to "sandshrew", 28 to "sandslash", 29 to "nidoran-f", 30 to "nidorina",
    31 to "nidoqueen", 32 to "nidoran-m", 33 to "nidorino", 34 to "nidoking", 35 to "clefairy",
    36 to "clefable", 37 to "vulpix", 38 to "ninetales", 39 to "jigglypuff", 40 to "wigglytuff",
    41 to "zubat", 42 to "golbat", 43 to "oddish", 44 to "gloom", 45 to "vileplume",
    46 to "paras", 47 to "parasect", 48 to "venonat", 49 to "venomoth", 50 to "diglett",
    51 to "dugtrio", 52 to "meowth", 53 to "persian", 54 to "psyduck", 55 to "golduck",
    56 to "mankey", 57 to "primeape", 58 to "growlithe", 59 to "arcanine", 60 to "poliwag",
    61 to "poliwhirl", 62 to "poliwrath", 63 to "abra", 64 to "kadabra", 65 to "alakazam",
    66 to "machop", 67 to "machoke", 68 to "machamp", 69 to "bellsprout", 70 to "weepinbell",
    71 to "victreebel", 72 to "tentacool", 73 to "tentacruel", 74 to "geodude", 75 to "graveler",
    76 to "golem", 77 to "ponyta", 78 to "rapidash", 79 to "slowpoke", 80 to "slowbro",
    81 to "magnemite", 82 to "magneton", 83 to "farfetchd", 84 to "doduo", 85 to "dodrio",
    86 to "seel", 87 to "dewgong", 88 to "grimer", 89 to "muk", 90 to "shellder",
    91 to "cloyster", 92 to "gastly", 93 to "haunter", 94 to "gengar", 95 to "onix",
    96 to "drowzee", 97 to "hypno", 98 to "krabby", 99 to "kingler", 100 to "voltorb",
    101 to "electrode", 102 to "exeggcute", 103 to "exeggutor", 104 to "cubone", 105 to "marowak",
    106 to "hitmonlee", 107 to "hitmonchan", 108 to "lickitung", 109 to "koffing", 110 to "weezing",
    111 to "rhyhorn", 112 to "rhydon", 113 to "chansey", 114 to "tangela", 115 to "kangaskhan",
    116 to "horsea", 117 to "seadra", 118 to "goldeen", 119 to "seaking", 120 to "staryu",
    121 to "starmie", 122 to "mr-mime", 123 to "scyther", 124 to "jynx", 125 to "electabuzz",
    126 to "magmar", 127 to "pinsir", 128 to "tauros", 129 to "magikarp", 130 to "gyarados",
    131 to "lapras", 132 to "ditto", 133 to "eevee", 134 to "vaporeon", 135 to "jolteon",
    136 to "flareon", 137 to "porygon", 138 to "omanyte", 139 to "omastar", 140 to "kabuto",
    141 to "kabutops", 142 to "aerodactyl", 143 to "snorlax", 144 to "articuno", 145 to "zapdos",
    146 to "moltres", 147 to "dratini", 148 to "dragonair", 149 to "dragonite", 150 to "mewtwo",
    151 to "mew",
    // Geração 2 (152-251)
    152 to "chikorita", 153 to "bayleef", 154 to "meganium", 155 to "cyndaquil", 156 to "quilava",
    157 to "typhlosion", 158 to "totodile", 159 to "croconaw", 160 to "feraligatr", 161 to "sentret",
    162 to "furret", 163 to "hoothoot", 164 to "noctowl", 165 to "ledyba", 166 to "ledian",
    167 to "spinarak", 168 to "ariados", 169 to "crobat", 170 to "chinchou", 171 to "lanturn",
    172 to "pichu", 173 to "cleffa", 174 to "igglybuff", 175 to "togepi", 176 to "togetic",
    177 to "natu", 178 to "xatu", 179 to "mareep", 180 to "flaaffy", 181 to "ampharos",
    182 to "bellossom", 183 to "marill", 184 to "azumarill", 185 to "sudowoodo", 186 to "politoed",
    187 to "hoppip", 188 to "skiploom", 189 to "jumpluff", 190 to "aipom", 191 to "sunkern",
    192 to "sunflora", 193 to "yanma", 194 to "wooper", 195 to "quagsire", 196 to "espeon",
    197 to "umbreon", 198 to "murkrow", 199 to "slowking", 200 to "misdreavus", 201 to "unown",
    202 to "wobbuffet", 203 to "girafarig", 204 to "pineco", 205 to "forretress", 206 to "dunsparce",
    207 to "gligar", 208 to "steelix", 209 to "snubbull", 210 to "granbull", 211 to "qwilfish",
    212 to "scizor", 213 to "shuckle", 214 to "heracross", 215 to "sneasel", 216 to "teddiursa",
    217 to "ursaring", 218 to "slugma", 219 to "magcargo", 220 to "swinub", 221 to "piloswine",
    222 to "corsola", 223 to "remoraid", 224 to "octillery", 225 to "delibird", 226 to "mantine",
    227 to "skarmory", 228 to "houndour", 229 to "houndoom", 230 to "kingdra", 231 to "phanpy",
    232 to "donphan", 233 to "porygon2", 234 to "stantler", 235 to "smeargle", 236 to "tyrogue",
    237 to "hitmontop", 238 to "smoochum", 239 to "elekid", 240 to "magby", 241 to "miltank",
    242 to "blissey", 243 to "raikou", 244 to "entei", 245 to "suicune", 246 to "larvitar",
    247 to "pupitar", 248 to "tyranitar", 249 to "lugia", 250 to "ho-oh", 251 to "celebi",
    // Geração 3 (252-386)
    252 to "treecko", 253 to "grovyle", 254 to "sceptile", 255 to "torchic", 256 to "combusken",
    257 to "blaziken", 258 to "mudkip", 259 to "marshtomp", 260 to "swampert", 261 to "poochyena",
    262 to "mightyena", 263 to "zigzagoon", 264 to "linoone", 265 to "wurmple", 266 to "silcoon",
    267 to "beautifly", 268 to "cascoon", 269 to "dustox", 270 to "lotad", 271 to "lombre",
    272 to "ludicolo", 273 to "seedot", 274 to "nuzleaf", 275 to "shiftry", 276 to "taillow",
    277 to "swellow", 278 to "wingull", 279 to "pelipper", 280 to "ralts", 281 to "kirlia",
    282 to "gardevoir", 283 to "surskit", 284 to "masquerain", 285 to "shroomish", 286 to "breloom",
    287 to "slakoth", 288 to "vigoroth", 289 to "slaking", 290 to "nincada", 291 to "ninjask",
    292 to "shedinja", 293 to "whismur", 294 to "loudred", 295 to "exploud", 296 to "makuhita",
    297 to "hariyama", 298 to "azurill", 299 to "nosepass", 300 to "skitty", 301 to "delcatty",
    302 to "sableye", 303 to "mawile", 304 to "aron", 305 to "lairon", 306 to "aggron",
    307 to "meditite", 308 to "medicham", 309 to "electrike", 310 to "manectric", 311 to "plusle",
    312 to "minun", 313 to "volbeat", 314 to "illumise", 315 to "roselia", 316 to "gulpin",
    317 to "swalot", 318 to "carvanha", 319 to "sharpedo", 320 to "wailmer", 321 to "wailord",
    322 to "numel", 323 to "camerupt", 324 to "torkoal", 325 to "spoink", 326 to "grumpig",
    327 to "spinda", 328 to "trapinch", 329 to "vibrava", 330 to "flygon", 331 to "cacnea",
    332 to "cacturne", 333 to "swablu", 334 to "altaria", 335 to "zangoose", 336 to "seviper",
    337 to "lunatone", 338 to "solrock", 339 to "barboach", 340 to "whiscash", 341 to "corphish",
    342 to "crawdaunt", 343 to "baltoy", 344 to "claydol", 345 to "lileep", 346 to "cradily",
    347 to "anorith", 348 to "armaldo", 349 to "feebas", 350 to "milotic", 351 to "castform",
    352 to "kecleon", 353 to "shuppet", 354 to "banette", 355 to "duskull", 356 to "dusclops",
    357 to "tropius", 358 to "chimecho", 359 to "absol", 360 to "wynaut", 361 to "snorunt",
    362 to "glalie", 363 to "spheal", 364 to "sealeo", 365 to "walrein", 366 to "clamperl",
    367 to "huntail", 368 to "gorebyss", 369 to "relicanth", 370 to "luvdisc", 371 to "bagon",
    372 to "shelgon", 373 to "salamence", 374 to "beldum", 375 to "metang", 376 to "metagross",
    377 to "regirock", 378 to "regice", 379 to "registeel", 380 to "latias", 381 to "latios",
    382 to "kyogre", 383 to "groudon", 384 to "rayquaza", 385 to "jirachi", 386 to "deoxys",
    // Geração 4 (387-493)
    387 to "turtwig", 388 to "grotle", 389 to "torterra", 390 to "chimchar", 391 to "monferno",
    392 to "infernape", 393 to "piplup", 394 to "prinplup", 395 to "empoleon", 396 to "starly",
    397 to "staravia", 398 to "staraptor", 399 to "bidoof", 400 to "bibarel", 401 to "kricketot",
    402 to "kricketune", 403 to "shinx", 404 to "luxio", 405 to "luxray", 406 to "budew",
    407 to "roserade", 408 to "cranidos", 409 to "rampardos", 410 to "shieldon", 411 to "bastiodon",
    412 to "burmy", 413 to "wormadam", 414 to "mothim", 415 to "combee", 416 to "vespiquen",
    417 to "pachirisu", 418 to "buizel", 419 to "floatzel", 420 to "cherubi", 421 to "cherrim",
    422 to "shellos", 423 to "gastrodon", 424 to "ambipom", 425 to "drifloon", 426 to "drifblim",
    427 to "buneary", 428 to "lopunny", 429 to "mismagius", 430 to "honchkrow", 431 to "glameow",
    432 to "purugly", 433 to "chingling", 434 to "stunky", 435 to "skuntank", 436 to "bronzor",
    437 to "bronzong", 438 to "bonsly", 439 to "mime-jr", 440 to "happiny", 441 to "chatot",
    442 to "spiritomb", 443 to "gible", 444 to "gabite", 445 to "garchomp", 446 to "munchlax",
    447 to "riolu", 448 to "lucario", 449 to "hippopotas", 450 to "hippowdon", 451 to "skorupi",
    452 to "drapion", 453 to "croagunk", 454 to "toxicroak", 455 to "carnivine", 456 to "finneon",
    457 to "lumineon", 458 to "mantyke", 459 to "snover", 460 to "abomasnow", 461 to "weavile",
    462 to "magnezone", 463 to "lickilicky", 464 to "rhyperior", 465 to "tangrowth", 466 to "electivire",
    467 to "magmortar", 468 to "togekiss", 469 to "yanmega", 470 to "leafeon", 471 to "glaceon",
    472 to "gliscor", 473 to "mamoswine", 474 to "porygon-z", 475 to "gallade", 476 to "probopass",
    477 to "dusknoir", 478 to "froslass", 479 to "rotom", 480 to "uxie", 481 to "mesprit",
    482 to "azelf", 483 to "dialga", 484 to "palkia", 485 to "heatran", 486 to "regigigas",
    487 to "giratina", 488 to "cresselia", 489 to "phione", 490 to "manaphy", 491 to "darkrai",
    492 to "shaymin", 493 to "arceus",
    // Geração 5 (494-649)
    494 to "victini", 495 to "snivy", 496 to "servine", 497 to "serperior", 498 to "tepig",
    499 to "pignite", 500 to "emboar", 501 to "oshawott", 502 to "dewott", 503 to "samurott",
    504 to "patrat", 505 to "watchog", 506 to "lillipup", 507 to "herdier", 508 to "stoutland",
    509 to "purrloin", 510 to "liepard", 511 to "pansage", 512 to "simisage", 513 to "pansear",
    514 to "simisear", 515 to "panpour", 516 to "simipour", 517 to "munna", 518 to "musharna",
    519 to "pidove", 520 to "tranquill", 521 to "unfezant", 522 to "blitzle", 523 to "zebstrika",
    524 to "roggenrola", 525 to "boldore", 526 to "gigalith", 527 to "woobat", 528 to "swoobat",
    529 to "drilbur", 530 to "excadrill", 531 to "audino", 532 to "timburr", 533 to "gurdurr",
    534 to "conkeldurr", 535 to "tympole", 536 to "palpitoad", 537 to "seismitoad", 538 to "throh",
    539 to "sawk", 540 to "sewaddle", 541 to "swadloon", 542 to "leavanny", 543 to "venipede",
    544 to "whirlipede", 545 to "scolipede", 546 to "cottonee", 547 to "whimsicott", 548 to "petilil",
    549 to "lilligant", 550 to "basculin", 551 to "sandile", 552 to "krokorok", 553 to "krookodile",
    554 to "darumaka", 555 to "darmanitan", 556 to "maractus", 557 to "dwebble", 558 to "crustle",
    559 to "scraggy", 560 to "scrafty", 561 to "sigilyph", 562 to "yamask", 563 to "cofagrigus",
    564 to "tirtouga", 565 to "carracosta", 566 to "archen", 567 to "archeops", 568 to "trubbish",
    569 to "garbodor", 570 to "zorua", 571 to "zoroark", 572 to "minccino", 573 to "cinccino",
    574 to "gothita", 575 to "gothorita", 576 to "gothitelle", 577 to "solosis", 578 to "duosion",
    579 to "reuniclus", 580 to "ducklett", 581 to "swanna", 582 to "vanillite", 583 to "vanillish",
    584 to "vanilluxe", 585 to "deerling", 586 to "sawsbuck", 587 to "emolga", 588 to "karrablast",
    589 to "escavalier", 590 to "foongus", 591 to "amoonguss", 592 to "frillish", 593 to "jellicent",
    594 to "alomomola", 595 to "joltik", 596 to "galvantula", 597 to "ferroseed", 598 to "ferrothorn",
    599 to "klink", 600 to "klang", 601 to "klinklang", 602 to "tynamo", 603 to "eelektrik",
    604 to "eelektross", 605 to "elgyem", 606 to "beheeyem", 607 to "litwick", 608 to "lampent",
    609 to "chandelure", 610 to "axew", 611 to "fraxure", 612 to "haxorus", 613 to "cubchoo",
    614 to "beartic", 615 to "cryogonal", 616 to "shelmet", 617 to "accelgor", 618 to "stunfisk",
    619 to "mienfoo", 620 to "mienshao", 621 to "druddigon", 622 to "golett", 623 to "golurk",
    624 to "pawniard", 625 to "bisharp", 626 to "bouffalant", 627 to "rufflet", 628 to "braviary",
    629 to "vullaby", 630 to "mandibuzz", 631 to "heatmor", 632 to "durant", 633 to "deino",
    634 to "zweilous", 635 to "hydreigon", 636 to "larvesta", 637 to "volcarona", 638 to "cobalion",
    639 to "terrakion", 640 to "virizion", 641 to "tornadus", 642 to "thundurus", 643 to "reshiram",
    644 to "zekrom", 645 to "landorus", 646 to "kyurem", 647 to "keldeo", 648 to "meloetta",
    649 to "genesect",
    // Geração 6 (650-721)
    650 to "chespin", 651 to "quilladin", 652 to "chesnaught", 653 to "fennekin", 654 to "braixen",
    655 to "delphox", 656 to "froakie", 657 to "frogadier", 658 to "greninja", 659 to "bunnelby",
    660 to "diggersby", 661 to "fletchling", 662 to "fletchinder", 663 to "talonflame", 664 to "scatterbug",
    665 to "spewpa", 666 to "vivillon", 667 to "litleo", 668 to "pyroar", 669 to "flabebe",
    670 to "floette", 671 to "florges", 672 to "skiddo", 673 to "gogoat", 674 to "pancham",
    675 to "pangoro", 676 to "furfrou", 677 to "espurr", 678 to "meowstic", 679 to "honedge",
    680 to "doublade", 681 to "aegislash", 682 to "spritzee", 683 to "aromatisse", 684 to "swirlix",
    685 to "slurpuff", 686 to "inkay", 687 to "malamar", 688 to "binacle", 689 to "barbaracle",
    690 to "skrelp", 691 to "dragalge", 692 to "clauncher", 693 to "clawitzer", 694 to "helioptile",
    695 to "heliolisk", 696 to "tyrunt", 697 to "tyrantrum", 698 to "amaura", 699 to "aurorus",
    700 to "sylveon", 701 to "hawlucha", 702 to "dedenne", 703 to "carbink", 704 to "goomy",
    705 to "sliggoo", 706 to "goodra", 707 to "klefki", 708 to "phantump", 709 to "trevenant",
    710 to "pumpkaboo", 711 to "gourgeist", 712 to "bergmite", 713 to "avalugg", 714 to "noibat",
    715 to "noivern", 716 to "xerneas", 717 to "yveltal", 718 to "zygarde", 719 to "diancie",
    720 to "hoopa", 721 to "volcanion",
    // Geração 7 (722-809)
    722 to "rowlet", 723 to "dartrix", 724 to "decidueye", 725 to "litten", 726 to "torracat",
    727 to "incineroar", 728 to "popplio", 729 to "brionne", 730 to "primarina", 731 to "pikipek",
    732 to "trumbeak", 733 to "toucannon", 734 to "yungoos", 735 to "gumshoos", 736 to "grubbin",
    737 to "charjabug", 738 to "vikavolt", 739 to "crabrawler", 740 to "crabominable", 741 to "oricorio",
    742 to "cutiefly", 743 to "ribombee", 744 to "rockruff", 745 to "lycanroc", 746 to "wishiwashi",
    747 to "mareanie", 748 to "toxapex", 749 to "mudbray", 750 to "mudsdale", 751 to "dewpider",
    752 to "araquanid", 753 to "fomantis", 754 to "lurantis", 755 to "morelull", 756 to "shiinotic",
    757 to "salandit", 758 to "salazzle", 759 to "stufful", 760 to "bewear", 761 to "bounsweet",
    762 to "steenee", 763 to "tsareena", 764 to "comfey", 765 to "oranguru", 766 to "passimian",
    767 to "wimpod", 768 to "golisopod", 769 to "sandygast", 770 to "palossand", 771 to "pyukumuku",
    772 to "type-null", 773 to "silvally", 774 to "minior", 775 to "komala", 776 to "turtonator",
    777 to "togedemaru", 778 to "mimikyu", 779 to "bruxish", 780 to "drampa", 781 to "dhelmise",
    782 to "jangmo-o", 783 to "hakamo-o", 784 to "kommo-o", 785 to "tapu-koko", 786 to "tapu-lele",
    787 to "tapu-bulu", 788 to "tapu-fini", 789 to "cosmog", 790 to "cosmoem", 791 to "solgaleo",
    792 to "lunala", 793 to "nihilego", 794 to "buzzwole", 795 to "pheromosa", 796 to "xurkitree",
    797 to "celesteela", 798 to "kartana", 799 to "guzzlord", 800 to "necrozma", 801 to "magearna",
    802 to "marshadow", 803 to "poipole", 804 to "naganadel", 805 to "stakataka", 806 to "blacephalon",
    807 to "zeraora", 808 to "meltan", 809 to "melmetal",
    // Geração 8 (810-905)
    810 to "grookey", 811 to "thwackey", 812 to "rillaboom", 813 to "scorbunny", 814 to "raboot",
    815 to "cinderace", 816 to "sobble", 817 to "drizzile", 818 to "inteleon", 819 to "skwovet",
    820 to "greedent", 821 to "rookidee", 822 to "corvisquire", 823 to "corviknight", 824 to "blipbug",
    825 to "dottler", 826 to "orbeetle", 827 to "nickit", 828 to "thievul", 829 to "gossifleur",
    830 to "eldegoss", 831 to "wooloo", 832 to "dubwool", 833 to "chewtle", 834 to "drednaw",
    835 to "yamper", 836 to "boltund", 837 to "rolycoly", 838 to "carkol", 839 to "coalossal",
    840 to "applin", 841 to "flapple", 842 to "appletun", 843 to "silicobra", 844 to "sandaconda",
    845 to "cramorant", 846 to "arrokuda", 847 to "barraskewda", 848 to "toxel", 849 to "toxtricity",
    850 to "sizzlipede", 851 to "centiskorch", 852 to "clobbopus", 853 to "grapploct", 854 to "sinistea",
    855 to "polteageist", 856 to "hatenna", 857 to "hattrem", 858 to "hatterene", 859 to "impidimp",
    860 to "morgrem", 861 to "grimmsnarl", 862 to "obstagoon", 863 to "perrserker", 864 to "cursola",
    865 to "sirfetchd", 866 to "mr-rime", 867 to "runerigus", 868 to "milcery", 869 to "alcremie",
    870 to "falinks", 871 to "pincurchin", 872 to "snom", 873 to "frosmoth", 874 to "stonjourner",
    875 to "eiscue", 876 to "indeedee", 877 to "morpeko", 878 to "cufant", 879 to "copperajah",
    880 to "dracozolt", 881 to "arctozolt", 882 to "dracovish", 883 to "arctovish", 884 to "duraludon",
    885 to "dreepy", 886 to "drakloak", 887 to "dragapult", 888 to "zacian", 889 to "zamazenta",
    890 to "eternatus", 891 to "kubfu", 892 to "urshifu", 893 to "zarude", 894 to "regieleki",
    895 to "regidrago", 896 to "glastrier", 897 to "spectrier", 898 to "calyrex", 899 to "wyrdeer",
    900 to "kleavor", 901 to "ursaluna", 902 to "basculegion", 903 to "sneasler", 904 to "overqwil",
    905 to "enamorus",
    // Geração 9 (906-1025)
    906 to "sprigatito", 907 to "floragato", 908 to "meowscarada", 909 to "fuecoco", 910 to "crocalor",
    911 to "skeledirge", 912 to "quaxly", 913 to "quaxwell", 914 to "quaquaval", 915 to "lechonk",
    916 to "oinkologne", 917 to "tarountula", 918 to "spidops", 919 to "nymble", 920 to "lokix",
    921 to "pawmi", 922 to "pawmo", 923 to "pawmot", 924 to "tandemaus", 925 to "maushold",
    926 to "fidough", 927 to "dachsbun", 928 to "smoliv", 929 to "dolliv", 930 to "arboliva",
    931 to "squawkabilly", 932 to "nacli", 933 to "naclstack", 934 to "garganacl", 935 to "charcadet",
    936 to "armarouge", 937 to "ceruledge", 938 to "tadbulb", 939 to "bellibolt", 940 to "wattrel",
    941 to "kilowattrel", 942 to "maschiff", 943 to "mabosstiff", 944 to "shroodle", 945 to "grafaiai",
    946 to "bramblin", 947 to "brambleghast", 948 to "toedscool", 949 to "toedscruel", 950 to "klawf",
    951 to "capsakid", 952 to "scovillain", 953 to "rellor", 954 to "rabsca", 955 to "flittle",
    956 to "espathra", 957 to "tinkatink", 958 to "tinkatuff", 959 to "tinkaton", 960 to "wiglett",
    961 to "wugtrio", 962 to "bombirdier", 963 to "finizen", 964 to "palafin", 965 to "varoom",
    966 to "revavroom", 967 to "cyclizar", 968 to "orthworm", 969 to "glimmet", 970 to "glimmora",
    971 to "greavard", 972 to "houndstone", 973 to "flamigo", 974 to "cetoddle", 975 to "cetitan",
    976 to "veluza", 977 to "dondozo", 978 to "tatsugiri", 979 to "annihilape", 980 to "clodsire",
    981 to "farigiraf", 982 to "dudunsparce", 983 to "kingambit", 984 to "great-tusk", 985 to "scream-tail",
    986 to "brute-bonnet", 987 to "flutter-mane", 988 to "slither-wing", 989 to "sandy-shocks",
    990 to "iron-treads", 991 to "iron-bundle", 992 to "iron-hands", 993 to "iron-jugulis",
    994 to "iron-moth", 995 to "iron-thorns", 996 to "frigibax", 997 to "arctibax", 998 to "baxcalibur",
    999 to "gimmighoul", 1000 to "gholdengo", 1001 to "wo-chien", 1002 to "chien-pao", 1003 to "ting-lu",
    1004 to "chi-yu", 1005 to "roaring-moon", 1006 to "iron-valiant", 1007 to "koraidon", 1008 to "miraidon",
    1009 to "walking-wake", 1010 to "iron-leaves", 1011 to "dipplin", 1012 to "poltchageist",
    1013 to "sinistcha", 1014 to "okidogi", 1015 to "munkidori", 1016 to "fezandipiti", 1017 to "ogerpon",
    1018 to "archaludon", 1019 to "hydrapple", 1020 to "gouging-fire", 1021 to "raging-bolt",
    1022 to "iron-boulder", 1023 to "iron-crown", 1024 to "terapagos", 1025 to "pecharunt"
)