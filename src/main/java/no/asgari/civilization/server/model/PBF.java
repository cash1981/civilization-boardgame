/*
 * Copyright (c) 2015 Shervin Asgari
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

package no.asgari.civilization.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.common.collect.Lists;
import lombok.Data;
import no.asgari.civilization.server.jackson.TurnKeyDeserializer;
import no.asgari.civilization.server.jackson.TurnKeySerializer;
import org.hibernate.validator.constraints.NotBlank;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * PBF stands for Play By Forum
 */
@Data
@JsonRootName(value = "pbf")
@XmlRootElement
@JsonInclude
@JsonIgnoreProperties(ignoreUnknown = true)
//Perhaps use this when the object keep getting changed, otherwise jackson throws exception when it cannot map
public class PBF {
    @JsonIgnore
    public static final String COL_NAME = "pbf";
    @JsonIgnore
    public static final String NAME = "name";

    @ObjectId
    @Id
    private String id;

    @NotBlank
    private String name;
    private GameType type;
    private String mapLink;
    private String assetLink;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime created = LocalDateTime.now();

    private int numOfPlayers;
    private boolean active = true;
    private String winner; //username
    private List<Item> items = Lists.newArrayList();
    private List<Playerhand> players = Lists.newArrayList();
    private List<Tech> techs = Lists.newArrayList();
    private List<SocialPolicy> socialPolicies = new ArrayList<>(8);

    //@JsonSerialize(keyUsing=TurnKeySerializer.class)
    //@JsonDeserialize(keyUsing = TurnKeyDeserializer.class)
    private Map<String, PlayerTurn> publicTurns = new HashMap<>();

    //Will use these to reshuffle items which are discarded and can be drawn again
    private List<Item> discardedItems = new ArrayList<>();

    //If a player leaves, his items and stuff will be put here
    private List<Playerhand> withdrawnPlayers = new ArrayList<>();

    private boolean tournament = false;

    /**
     * Returns the username of the player who is start of turn
     */
    @JsonIgnore
    public String getNameOfUsersTurn() {
        Optional<Playerhand> optional = players.stream()
                .filter(Playerhand::isYourTurn)
                .findFirst();

        if (optional.isPresent()) {
            return optional.get().getUsername();
        }

        return "";
    }

}
