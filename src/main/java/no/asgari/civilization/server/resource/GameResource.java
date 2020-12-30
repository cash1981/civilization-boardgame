/*
 * Copyright (c) 2015-2021 Shervin Asgari
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package no.asgari.civilization.server.resource;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mongodb.DB;
import io.dropwizard.auth.Auth;
import lombok.extern.log4j.Log4j;
import no.asgari.civilization.server.action.GameAction;
import no.asgari.civilization.server.action.GameLogAction;
import no.asgari.civilization.server.action.PlayerAction;
import no.asgari.civilization.server.action.TurnAction;
import no.asgari.civilization.server.action.UndoAction;
import no.asgari.civilization.server.dto.ChatDTO;
import no.asgari.civilization.server.dto.CivHighscoreDTO;
import no.asgari.civilization.server.dto.CreateNewGameDTO;
import no.asgari.civilization.server.dto.DrawDTO;
import no.asgari.civilization.server.dto.GameDTO;
import no.asgari.civilization.server.dto.GameLogDTO;
import no.asgari.civilization.server.dto.MessageDTO;
import no.asgari.civilization.server.dto.PbfDTO;
import no.asgari.civilization.server.dto.PlayerDTO;
import no.asgari.civilization.server.dto.PlayerHighscoreDTO;
import no.asgari.civilization.server.model.Chat;
import no.asgari.civilization.server.model.GameLog;
import no.asgari.civilization.server.model.PBF;
import no.asgari.civilization.server.model.Player;
import no.asgari.civilization.server.model.PlayerTurn;
import no.asgari.civilization.server.model.Tech;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Path("game")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Log4j
public class GameResource {
    private final DB db;
    private final GameAction gameAction;
    private final GameLogAction gameLogAction;

    @Context
    private UriInfo uriInfo;

    public GameResource(DB db) {
        this.db = db;
        this.gameAction = new GameAction(db);
        this.gameLogAction = new GameLogAction(db);
    }

    /**
     * This is the default method for this resource.
     * It will return all active games
     *
     * @return
     */
    @GET
    @Timed
    public Response getAllGames() {
        List<PbfDTO> games = gameAction.getAllGames();
        return Response.ok()
                .entity(games)
                .build();
    }

    /**
     * Returns a specific game
     *
     * @return
     */
    @Path("/{pbfId}")
    @GET
    @Timed
    public Response getGame(@Auth(required = false) Player player, @PathParam("pbfId") String pbfId) {
        if (Strings.isNullOrEmpty(pbfId)) {
            log.error("pbfId is missing");
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        PBF pbf = gameAction.findPBFById(pbfId);
        GameDTO gameDTO = gameAction.mapGameDTO(pbf, player);

        return Response.ok()
                .entity(gameDTO)
                .build();
    }

    /**
     * Will return a collection of all pbf ids
     */
    @Path("/player")
    @GET
    @Timed
    public Response getGamesByPlayer(@Auth Player player) {
        PlayerAction playerAction = new PlayerAction(db);
        Set<String> games = playerAction.getGames(player);
        return Response.ok().entity(games).build();
    }

    /**
     * Will return a list of all the players of this PBF.
     * Handy for selecting players whom to trade with. Will remove the current user from the list
     */
    @GET
    @Path("/{pbfId}/players")
    public Response getAllPlayersForPBF(@NotEmpty @PathParam("pbfId") String pbfId, @Auth(required = false) Player player) {
        List<PlayerDTO> players = gameAction.getAllPlayers(pbfId);
        if (player != null) {
            return Response.ok()
                    .entity(players.stream().filter(p -> !p.getPlayerId().equals(player.getId())).collect(toList()))
                    .build();
        }
        return Response.ok()
                .entity(players)
                .build();
    }

    /**
     * Will return a list of all the players of this PBF.
     */
    @GET
    @Path("/{pbfId}/players/all")
    public Response getAllPlayersForPBF(@NotEmpty @PathParam("pbfId") String pbfId) {
        List<PlayerDTO> players = gameAction.getAllPlayers(pbfId);
        return Response.ok()
                .entity(players)
                .build();
    }

    @POST
    @Timed
    public Response createGame(@Valid CreateNewGameDTO dto, @Auth Player player) {
        Preconditions.checkNotNull(dto);
        Preconditions.checkNotNull(player);

        log.info("Creating game " + dto);

        String id = gameAction.createNewGame(dto, player.getId());
        URI location = URI.create("/" + id);
        log.debug("location for new game is " + location);
        return Response.status(Response.Status.CREATED)
                .location(location)
                .build();
    }

    @DELETE
    @Timed
    @Path("/{pbfId}/end")
    public void endGame(@PathParam("pbfId") String pbfId, @Auth Player player, @QueryParam("winner") String winner) {
        Preconditions.checkNotNull(pbfId);

        log.info("Ending game with id " + pbfId);
        gameAction.endGame(pbfId, player, winner);
    }

    @POST
    @Timed
    @Path("/{pbfId}/join")
    public Response joinGame(@NotEmpty @PathParam("pbfId") String pbfId, @Auth Player player) {
        Preconditions.checkNotNull(pbfId);
        Preconditions.checkNotNull(player);

        gameAction.joinGame(pbfId, player, Optional.empty());

        URI location = URI.create("/" + pbfId);
        log.debug("location for new game is " + location);
        return Response.status(Response.Status.OK)
                .location(location)
                .build();
    }

    /**
     * Withdraw from existing game.
     * Game must not have started
     */
    @POST
    @Timed
    @Path("/{pbfId}/withdraw")
    public Response withdrawFromGame(@NotEmpty @PathParam("pbfId") String pbfId, @Auth Player player) {
        Preconditions.checkNotNull(pbfId);
        Preconditions.checkNotNull(player);

        boolean ok = gameAction.withdrawFromGame(pbfId, player.getId());
        if (ok) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.NOT_ACCEPTABLE).build();
    }

    /**
     * Gets all the available techs. Will remove the techs that player already have chosen
     *
     * @param pbfId  - The PBF
     * @param player - The Authenticated player
     * @return - Response ok with a list of techs
     */
    @GET
    @Timed
    @Path("/{pbfId}/techs")
    public List<Tech> getAvailableTechs(@NotEmpty @PathParam("pbfId") String pbfId, @Auth Player player) {
        return new PlayerAction(db).getRemaingTechsForPlayer(player.getId(), pbfId);
    }

    @GET
    @Timed
    @Path("/{pbfId}/publiclog")
    public List<GameLogDTO> getPublicLog(@NotEmpty @PathParam("pbfId") String pbfId) {
        List<GameLog> allPublicLogs = gameLogAction.getGameLogs(pbfId);
        List<GameLogDTO> gameLogDTOs = new ArrayList<>();
        if (!allPublicLogs.isEmpty()) {
            gameLogDTOs = allPublicLogs.stream()
                    .filter(log -> !Strings.isNullOrEmpty(log.getPublicLog()))
                    .map(gl -> new GameLogDTO(gl.getId(), gl.getPublicLog(), gl.getCreatedInMillis(), new DrawDTO(gl.getDraw())))
                    .collect(toList());
        }
        return gameLogDTOs;
    }

    @GET
    @Timed
    @Path("/{pbfId}/privatelog")
    public List<GameLogDTO> getPrivateLog(@NotEmpty @PathParam("pbfId") String pbfId, @Auth Player player) {
        List<GameLog> allPrivateLogs = gameLogAction.getGameLogsBelongingToPlayer(pbfId, player.getUsername());
        List<GameLogDTO> gameLogDTOs = new ArrayList<>();
        if (!allPrivateLogs.isEmpty()) {
            gameLogDTOs = allPrivateLogs.stream()
                    .filter(log -> !Strings.isNullOrEmpty(log.getPrivateLog()))
                    .map(gl -> new GameLogDTO(gl.getId(), gl.getPrivateLog(), gl.getCreatedInMillis(), new DrawDTO(gl.getDraw())))
                    .collect(toList());
        }
        return gameLogDTOs;
    }

    /**
     * Returns a list of all undoes that are currently initiated and still not finished
     *
     * @param pbfId
     * @return
     */
    @GET
    @Path("/{pbfId}/undo/active")
    @Timed
    public Response getAllActiveUndosCurrentlyInProgress(@NotEmpty @PathParam("pbfId") String pbfId) {
        UndoAction undoAction = new UndoAction(db);
        List<GameLog> gamelogs = undoAction.getAllActiveUndos(pbfId);
        return Response.ok().entity(gamelogs).build();
    }

    /**
     * Returns a list of all undoes that are finished voted
     *
     * @param pbfId
     * @return
     */
    @GET
    @Path("/{pbfId}/undo/finished")
    @Timed
    public Response getAllFinishedUndos(@NotEmpty @PathParam("pbfId") String pbfId) {
        UndoAction undoAction = new UndoAction(db);
        List<GameLog> gamelogs = undoAction.getAllFinishedUndos(pbfId);
        return Response.ok().entity(gamelogs).build();
    }

    /**
     * Initiates undo for an item.
     * <p>
     * Will throw BAD_REQUEST if undo has already been performed
     *
     * @param player
     * @param pbfId
     * @param gameLogId
     * @return 200 ok
     */
    @PUT
    @Path("/{pbfId}/undo/{gameLogId}")
    @Timed
    public Response undoItem(@Auth Player player, @NotEmpty @PathParam("pbfId") String pbfId, @NotEmpty @PathParam("gameLogId") String gameLogId) {
        GameLog gameLog = gameLogAction.findGameLogById(gameLogId);
        UndoAction undoAction = new UndoAction(db);
        undoAction.initiateUndo(gameLog, player.getId());
        return Response.ok().build();
    }

    /**
     * Performs yes vote on an undo
     * <p>
     * Returns error if no undo is found
     *
     * @param player
     * @param pbfId
     * @param gameLogId
     * @return 200 ok
     */
    @PUT
    @Path("/{pbfId}/vote/{gameLogId}/yes")
    @Timed
    public Response voteYes(@Auth Player player, @NotEmpty @PathParam("pbfId") String pbfId, @NotEmpty @PathParam("gameLogId") String gameLogId) {
        GameLog gameLog = gameLogAction.findGameLogById(gameLogId);
        if (gameLog.getDraw() == null || gameLog.getDraw().getUndo() == null) {
            log.error("There is no undo to vote on");
            return Response.status(Response.Status.PRECONDITION_FAILED)
                    .build();
        }
        UndoAction undoAction = new UndoAction(db);
        undoAction.vote(gameLog, player.getId(), true);
        return Response.ok().build();
    }

    /**
     * Performs no vote on an undo
     * <p>
     * Returns "412 Precondition failed" if no undo is found
     *
     * @param player
     * @param pbfId
     * @param gameLogId
     * @return 200 ok
     */
    @PUT
    @Path("/{pbfId}/vote/{gameLogId}/no")
    @Timed
    public Response voteNo(@Auth Player player, @NotEmpty @PathParam("pbfId") String pbfId, @NotEmpty @PathParam("gameLogId") String gameLogId) {
        GameLog gameLog = gameLogAction.findGameLogById(gameLogId);
        if (gameLog.getDraw() == null || gameLog.getDraw().getUndo() == null) {
            log.error("There is no undo to vote on");
            return Response.status(Response.Status.PRECONDITION_FAILED)
                    .build();
        }
        UndoAction undoAction = new UndoAction(db);
        undoAction.vote(gameLog, player.getId(), false);
        return Response.ok().build();
    }

    @POST
    @Timed
    @Path("/{pbfId}/chat")
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response chat(@Auth Player player, @FormParam("message") String message, @PathParam("pbfId") String pbfId) {
        Preconditions.checkNotNull(message);
        Chat chat = gameAction.chat(pbfId, message, player.getUsername());
        return Response.created(URI.create(chat.getId())).entity(gameAction.getChat(pbfId)).build();
    }

    @POST
    @Timed
    @Path("/publicchat")
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response publicChat(@Auth Player player, @FormParam("message") String message) {
        Preconditions.checkNotNull(message);
        Chat chat = gameAction.chat(null, message, player.getUsername());
        return Response.created(URI.create(chat.getId())).entity(gameAction.getPublicChat()).build();
    }

    @GET
    @Timed
    @Path("/{pbfId}/chat")
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response getChatList(@PathParam("pbfId") String pbfId) {
        List<ChatDTO> chats = gameAction.getChat(pbfId);
        return Response.ok().entity(chats).build();
    }

    /**
     * Gets public chat which is 1 week old and maximum 50 entries, sorted on created
     */
    @GET
    @Timed
    @Path("/publicchat")
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response getPublicChatList() {

        List<ChatDTO> chats = gameAction.getPublicChat();
        return Response.ok().entity(chats).build();
    }

    @POST
    @Timed
    @Path("/{pbfId}/map")
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response updateMap(@Auth Player player, @FormParam("link") String link, @PathParam("pbfId") String pbfId) {
        Preconditions.checkNotNull(link);
        String linkId = gameAction.addMapLink(pbfId, link, player.getId());
        return Response.ok().entity(new MessageDTO(linkId)).build();
    }

    @POST
    @Timed
    @Path("/{pbfId}/asset")
    @Consumes(value = MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = MediaType.APPLICATION_JSON)
    public Response updateAsset(@Auth Player player, @FormParam("link") String link, @PathParam("pbfId") String pbfId) {
        Preconditions.checkNotNull(link);


        String linkId = gameAction.addAssetLink(pbfId, link, player.getId());
        return Response.ok().entity(new MessageDTO(linkId)).build();
    }

    @GET
    @Path("playerhighscore")
    @Produces(value = MediaType.APPLICATION_JSON)
    public PlayerHighscoreDTO getPlayerHighscores() {
        return gameAction.getPlayerHighScore();
    }

    @GET
    @Path("civhighscore")
    @Produces(value = MediaType.APPLICATION_JSON)
    public CivHighscoreDTO getCivHighscore() {
        return gameAction.getCivHighscore();
    }

    @GET
    @Path("/{pbfId}/turns")
    @Produces(value = MediaType.APPLICATION_JSON)
    public List<PlayerTurn> getAllPublicTurns(@PathParam("pbfId") String pbfId) {
        TurnAction turnAction = new TurnAction(db);
        return turnAction.getAllPublicTurns(pbfId);
    }
}
