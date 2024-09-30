package client

import common.GameState
import common.Location
import server.World
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.Action
import common.Facing
import common.entities.Entity
import common.entities.EntityPlayer
import kotlinx.coroutines.flow.Flow
import common.tiles.Tile

class GameClient(
    private val world: World
){

    enum class SessionState{
        LOGGED_IN,
        LOGGED_OUT,
        EDIT_MODE
    }

    private var gameState: Flow<GameState>? by mutableStateOf(null)
    private var sessionState: SessionState by mutableStateOf(SessionState.LOGGED_OUT)

    companion object {
        const val DEBUG_VIEW = false
        const val CELL_SIZE = 20
        private val painterMap = mutableMapOf<String, Painter>()

        @Composable
        fun getPainter(name: String): Painter {
            return painterMap[name]?:painterResource(name).also { painterMap[name] = it }
        }
    }

    @Composable
    fun Display(){
        println("Rendering display")
        when(sessionState){
            SessionState.LOGGED_IN -> Game{
                sessionState = SessionState.LOGGED_OUT
            }
            SessionState.EDIT_MODE -> EditMode{
                sessionState = SessionState.LOGGED_OUT
            }
            else -> Login()
        }
    }

    @Composable
    fun Login(){
        var input by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        Column {
            TextField(
                value = input,
                onValueChange = {
                    error = null
                    input = it
                }
            )
            Button(
                onClick = {
                    input.toInt().let { playerId ->
                        try{
                            gameState = world.connect(playerId)
                            sessionState = SessionState.LOGGED_IN
                        }catch (e: Exception){
                            error = e.message
                        }
                    }
                },
                enabled = input.isNotEmpty() && input.toIntOrNull() != null && input.toInt() >= 0
            ){
                Text("Login")
            }
            Button(
                onClick = {
                    try{
                        gameState = world.connectInEditMode()
                        sessionState = SessionState.EDIT_MODE
                    }catch (e: Exception){
                        error = e.message
                    }
                }
            ){
                Text("Edit Mode")
            }
            error?.let { e ->
                Text(e)
            }
        }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun EditMode(
        onExit: () -> Unit
    ){
        var selectedEditTile by remember { mutableStateOf(Tile.values().first()) }
        val gameState by gameState?.collectAsState(GameState())?:return
        val keysDown = remember { mutableStateOf<MutableSet<Key>>(HashSet()) }
        val requester = remember { FocusRequester() }
        var showMapDropdown by remember { mutableStateOf(false) }
        var showTileSelection by remember { mutableStateOf(false) }
        val player = (gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?)?:return
        val maps = world.getMaps()

        Board(
            modifier = Modifier
                .focusRequester(requester)
                .focusable()
                .onKeyEvent {
                    when(it.type){
                        KeyEventType.KeyDown -> keysDown.value.add(it.key)
                        KeyEventType.KeyUp -> keysDown.value.remove(it.key)
                    }
                    true
                },
            location = player.location,
            tiles = gameState.tiles,
            entities = gameState.entities,
            onTap = { offset ->
                val x = (offset.x / CELL_SIZE.dp.toPx()).toInt()
                val y = (offset.y / CELL_SIZE.dp.toPx()).toInt()
                world.enqueueAction(
                    playerId = gameState.playerId,
                    action = Action.EditTile(
                        x = x + gameState.location.coords.x.toInt(),
                        y = y + gameState.location.coords.y.toInt(),
                        tile = selectedEditTile
                    )
                )
                requester.requestFocus()
            }
        )

        //Handle key presses
        var dX = 0
        var dY = 0
        keysDown.value.forEach { key ->
            when (key) {
                Key.W -> {
                    dY -= 1
                }

                Key.A -> {
                    dX -= 1
                }

                Key.S -> {
                    dY += 1
                }

                Key.D -> {
                    dX += 1
                }
            }
        }
        if (dX != 0) {
            world.enqueueAction(gameState.playerId, Action.MovePlayer(dX, 0))
        }
        if (dY != 0) {
            world.enqueueAction(gameState.playerId, Action.MovePlayer(0, dY))
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((CELL_SIZE * 2).dp)
                    .background(color = Color.Black.copy(alpha = 0.5f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TileSelection(selectedEditTile){
                    showTileSelection = true
                }

                Box(modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .fillMaxHeight()
                    .clickable { showMapDropdown = true }
                    .weight(1f),
                    contentAlignment = Alignment.CenterStart
                ){
                    Text(
                        text = gameState.location.map,
                        color = Color.White,
                    )
                }
                DropdownMenu(
                    expanded = showMapDropdown,
                    onDismissRequest = {
                        showMapDropdown = false
                        requester.requestFocus()
                    }
                ){
                    maps.forEach { map ->
                        DropdownMenuItem(onClick = {
                            world.enqueueAction(
                                playerId = gameState.playerId,
                                action = Action.GoToMap(map)
                            )
                            showMapDropdown = false
                            requester.requestFocus()
                        }) {
                            Text(map)
                        }
                    }
                }
                Icon(modifier = Modifier
                    .padding(5.dp)
                    .clickable {
                        world.disconnect(gameState.playerId)
                        onExit()
                    },
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ){
                DropdownMenu(
                    expanded = showTileSelection,
                    onDismissRequest = {
                        showTileSelection = false
                        requester.requestFocus()
                    }
                ){
                    Tile.values().forEach { tile ->
                        DropdownMenuItem(onClick = {
                            selectedEditTile = tile
                            showTileSelection = false
                            requester.requestFocus()
                        }) {
                            TileSelection(tile)
                            Text(tile.name)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TileSelection(
        tile: Tile,
        onClick: (() -> Unit)? = null
    ){
        Box(
            modifier = Modifier
                .clickable(
                    onClick = onClick?:{},
                    enabled = onClick != null
                )
        ) {
            Image(
                painter = getPainter("tiles/tile_${tile.sprite}.png"),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(CELL_SIZE.dp)
            )
            if(tile.isSolid){
                Icon(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size((CELL_SIZE / 4).dp),
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Red
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Game(
        onDisconnect: () -> Unit = {}
    ){
        val gameState by gameState?.collectAsState(GameState())?:return
        val keysDown = remember { mutableStateOf<MutableSet<Key>>(HashSet()) }
        val requester = remember { FocusRequester() }
        var showMenu by remember { mutableStateOf(false) }
        val player = gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?

        if (player != null){
            Board(
                modifier = Modifier
                    .focusRequester(requester)
                    .focusable()
                    .onKeyEvent {
                        when(it.type){
                            KeyEventType.KeyDown -> {
                                val inChat = player.state is EntityPlayer.State.TALKING
                                if(!inChat){
                                    keysDown.value.add(it.key)
                                }
                                when(it.key){
                                    Key.E -> {
                                        if(!inChat) {
                                            showMenu = !showMenu
                                        }
                                    }
                                    Key.Enter -> {
                                        if(!inChat) {
                                            world.enqueueAction(playerId = gameState.playerId, action = Action.Interact)
                                        }
                                    }
                                    Key.Escape -> {
                                        if(inChat){
                                            world.enqueueAction(
                                                playerId = gameState.playerId,
                                                action = Action.CloseConversation
                                            )
                                            requester.requestFocus()
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            KeyEventType.KeyUp -> keysDown.value.remove(it.key)
                        }
                        true
                    },
                location = player.location,
                tiles = gameState.tiles,
                entities = gameState.entities,
                decoration = {
                    //Lighting
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 1 - gameState.lightLevel))
                    )

                    //Chat
                    var isChatting by remember { mutableStateOf(false) }
                    if(player.state is EntityPlayer.State.TALKING){
                        isChatting = true
                        Chat(
                            modifier = Modifier
                                .align(Alignment.BottomEnd),
                            player = player,
                            gameState = gameState
                        )
                    }else if(isChatting){
                        isChatting = false
                        requester.requestFocus()
                    }

                    //Clock
                    DigitalClock(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        time = gameState.time
                    )
                }
            )

            //Handle key presses
            var dX = 0
            var dY = 0
            keysDown.value.forEach { key ->
                when (key) {
                    Key.W -> {
                        dY -= 1
                    }

                    Key.A -> {
                        dX -= 1
                    }

                    Key.S -> {
                        dY += 1
                    }

                    Key.D -> {
                        dX += 1
                    }
                }
            }
            if (dX != 0) {
                world.enqueueAction(gameState.playerId, Action.MovePlayer(dX, 0))
            }
            if (dY != 0) {
                world.enqueueAction(gameState.playerId, Action.MovePlayer(0, dY))
            }
            val facing = when {
                dX == 1 && dY == 0 -> Facing.RIGHT
                dX == 1 && dY == 1 -> Facing.RIGHT
                dX == 0 && dY == 1 -> Facing.DOWN
                dX == -1 && dY == 1 -> Facing.LEFT
                dX == -1 && dY == 0 -> Facing.LEFT
                dX == -1 && dY == -1 -> Facing.LEFT
                dX == 0 && dY == -1 -> Facing.UP
                dX == 1 && dY == -1 -> Facing.RIGHT
                else -> null
            }
            facing?.let {
                world.enqueueAction(
                    gameState.playerId, Action.RotateEntity(
                        id = gameState.playerId,
                        location = player.location,
                        facing = it
                    )
                )
            }

            if(showMenu){
                Menu(
                    gameState = gameState,
                    onDisconnect = {
                        world.disconnect(gameState.playerId)
                        onDisconnect()
                    }
                )
            }

            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
        }
    }

    @Composable
    private fun Board(
        modifier: Modifier = Modifier,
        location: Location,
        tiles: List<List<Tile>>,
        entities: List<Entity>,
        onTap: (PointerInputScope.(Offset) -> Unit)? = null,
        decoration: @Composable (BoxScope.() -> Unit)? = null
    ){
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            val (width, height) = world.getDisplaySize()
            val playerOffsetX = (width - 1) / 2
            val playerOffsetY = (height - 1) / 2

            val (playerX, playerY) = location.coords
            val displayXOffset = playerX % 1
            val displayYOffset = playerY % 1
            Box(
                modifier = Modifier
                    .size(width = ((width - 2) * CELL_SIZE).dp, height = ((height - 2) * CELL_SIZE).dp)
                    .clipToBounds()
                    .offset(
                        x = -CELL_SIZE.dp,
                        y = -CELL_SIZE.dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(width = (width * CELL_SIZE).dp, height = (height * CELL_SIZE).dp)
                        .align(Alignment.Center)
                        .offset(
                            x = (-displayXOffset * CELL_SIZE).dp,
                            y = (-displayYOffset * CELL_SIZE).dp
                        )
                        .pointerInput(Unit){
                            detectTapGestures{ offset ->
                                onTap?.invoke(this, offset)
                            }
                        }
                ) {
                    Tiles(
                        tiles = tiles
                    )
                }
                entities.sortedBy {
                    it.location.coords.y
                }.filter { it !is EntityPlayer || !it.isEditing }.forEach {
                    val (x, y) = it.location.coords
                    //player coords are in the center of the screen
                    //so we need to adjust the entity coords to be relative to the player
                    val adjustedX = playerOffsetX - (playerX - x)
                    val adjustedY = playerOffsetY - (playerY - y)
                    val cellX = adjustedX * CELL_SIZE
                    val cellY = adjustedY * CELL_SIZE
                    Entity(it, it.getSprite(), cellX, cellY)
                }
                Box(
                    modifier = Modifier
                        .size(width = (width * CELL_SIZE).dp, height = (height * CELL_SIZE).dp)
                        .align(Alignment.Center)
                        .offset(
                            x = (-displayXOffset * CELL_SIZE).dp,
                            y = (-displayYOffset * CELL_SIZE).dp
                        )
                ) {
                    Tiles(
                        tiles = tiles,
                        z = 1
                    )
                }
            }
            decoration?.invoke(this)
        }
    }

    @Composable
    private fun Chat(
        modifier: Modifier = Modifier,
        player: EntityPlayer,
        gameState: GameState
    ){
        val conversation = (player.state as EntityPlayer.State.TALKING).conversation
        val otherId = conversation.participants.find { it != gameState.playerId }?:-1
        val other = gameState.entities.find { it.id == otherId }
        val otherOnline = world.isPlayerOnline(otherId)
        var messageToSend by remember { mutableStateOf("") }
        val requester = remember { FocusRequester() }
        Column(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .padding(10.dp)
                .background(Color.White.copy(alpha = 0.5f))
        ) {
            if(!otherOnline){
                Text(modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.5f)),
                    text = "Offline",
                    textAlign = TextAlign.Center
                )
            }
            LazyColumn(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            ){
                items(conversation.messages.size){
                    val message = conversation.messages[it]
                    Box(modifier = Modifier.fillMaxWidth()){
                        Column(modifier = Modifier
                            .padding(5.dp)
                            .align(if (message.senderId == gameState.playerId) Alignment.TopEnd else Alignment.TopStart),
                            horizontalAlignment = if (message.senderId == gameState.playerId) Alignment.End else Alignment.Start
                        ){
                            Text(
                                text = message.time,
                                fontSize = 10.sp,
                                textAlign = if (message.senderId == gameState.playerId) TextAlign.End else TextAlign.Start
                            )
                            Text(message.message)
                        }
                    }
                }
            }
            TextField(modifier = Modifier
                .focusRequester(requester),
                value = messageToSend,
                onValueChange = {
                    messageToSend = it
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier.clickable(
                            onClick = {
                                world.enqueueAction(
                                    playerId = gameState.playerId,
                                    action = Action.SendMessage(
                                        message = messageToSend
                                    )
                                )
                                messageToSend = ""
                                requester.requestFocus()
                            },
                            enabled = messageToSend.isNotEmpty()
                        ) ,
                        imageVector = Icons.Default.Send,
                        contentDescription = null
                    )
                }
            )
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    }

    @Composable
    private fun Menu(
        gameState: GameState,
        onDisconnect: () -> Unit,
    ){
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Row {
                Text("Game logged in as player ${gameState.playerId}")
                Button(
                    onClick = onDisconnect
                ) {
                    Text("Logout")
                }
            }
        }
    }

    @Composable
    private fun DigitalClock(
        modifier: Modifier = Modifier,
        time: String,
    ){
        Text(modifier = modifier
            .background(Color.White.copy(alpha = 0.2f)),
            text = time
        )
    }

    @Composable
    private fun AnalogClock(
        tick: Int
    ){
        val size = 100
        val hour = 0//RpgIoTime.getHourOfDay()
        val minute = 0//RpgIoTime.getMinuteOfDay()
        Text("Time: $hour")
        Box(
            modifier = Modifier
                .size(size.dp)
                .border(width = 1.dp, color = Color.Black, shape = CircleShape)
        ){
            Box(modifier = Modifier
                .align(Alignment.Center)
                .size((size * 0.1).dp)
                .background(Color.Black, shape = CircleShape),
            )
            val hourAngle = (hour % 12f / 12f) * 360
            Box(modifier = Modifier
                .align(Alignment.Center)
                .height((size * 0.5).dp)
                .width((size * 0.1).dp)
                .rotate(hourAngle)
            ){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Color.Black, shape = CircleShape)
                )
            }
            val minuteAngle = (minute % 60f / 60f) * 360
            Box(modifier = Modifier
                .align(Alignment.Center)
                .height((size * 0.7).dp)
                .width((size * 0.1).dp)
                .rotate(minuteAngle)
            ){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Color.Black, shape = CircleShape)
                )
            }
        }
    }

    @Composable
    private fun Tiles(
        modifier: Modifier = Modifier,
        tiles: List<List<Tile>>,
        z: Int = 0
    ){
        Box(
            modifier = modifier
        ) {
            tiles.forEachIndexed { r, row ->
                val rowY = r * CELL_SIZE
                row.forEachIndexed { c, tile ->
                    val cellX = c * CELL_SIZE

                    if(z == 0 || tile.inFrontOf != null){
                        val sprite = when{
                            z == 0 -> tile.inFrontOf?.sprite?:tile.sprite
                            else -> tile.sprite
                        }
                        Image(
                            painter = getPainter("tiles/tile_$sprite.png"),
                            contentDescription = null,
                            modifier = Modifier
                                .size(CELL_SIZE.dp)
                                .offset(
                                    x = cellX.dp,
                                    y = rowY.dp
                                )
                        )
                    }
                }
            }

        }
    }

    @Composable
    fun Entity(entity: Entity, sprite: String, x: Float, y: Float){
        //val sprite = entity.getSprite()
        Image(
            painter = getPainter("$sprite ".trim()),
            contentDescription = null,
            modifier = Modifier
                .size(width = CELL_SIZE.dp, height = CELL_SIZE.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
        )
        if(DEBUG_VIEW){
            Box(modifier = Modifier
                .size(width = CELL_SIZE.dp, height = CELL_SIZE.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
                .border(1.dp, color = Color.Cyan)
                .padding(
                    start = (entity.hitBox.fromLeft * CELL_SIZE).dp,
                    top = (entity.hitBox.fromTop * CELL_SIZE).dp,
                    end = (entity.hitBox.fromRight * CELL_SIZE).dp,
                    bottom = (entity.hitBox.fromBottom * CELL_SIZE).dp
                )
                .border(1.dp, color = Color.Red)
            )
        }
        if (entity is EntityPlayer && entity.state is EntityPlayer.State.TALKING) {
            Image(
                painter = getPainter("interact.png"),
                contentDescription = null,
                modifier = Modifier
                    .size(width = CELL_SIZE.dp, height = CELL_SIZE.dp)
                    .offset(
                        x = x.dp,
                        y = y.dp - CELL_SIZE.dp
                    )
            )
        }
    }

}
