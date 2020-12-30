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

package no.asgari.civilization.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import no.asgari.civilization.server.model.GameType;
import no.asgari.civilization.server.model.Item;
import no.asgari.civilization.server.model.Playerhand;

import java.util.List;

/**
 * DTO for inside a specific game
 */
@Data
@JsonRootName("gameDTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDTO {

    private String id;
    private GameType type;
    private String name;
    private long created;
    private String whosTurnIsIt; //username of the players turn
    private boolean active;
    private String mapLink;
    private String assetLink;

    private List<GameLogDTO> publicLogs;
    private List<GameLogDTO> privateLogs;
    private Playerhand player;
    private List<Item> revealedItems;

}
